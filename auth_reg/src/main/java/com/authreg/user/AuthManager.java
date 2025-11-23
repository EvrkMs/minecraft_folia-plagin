package com.authreg.user;

import com.authreg.AuthRegPlugin;
import com.authreg.config.AuthConfig;
import com.authreg.config.Messages;
import com.authreg.storage.Account;
import com.authreg.storage.AccountStorage;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AuthManager {
    private final AuthRegPlugin plugin;
    private final AccountStorage storage;
    private final AuthConfig config;
    private final Messages messages;

    private final Set<UUID> authenticated = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> attempts = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> reminders = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> timeouts = new ConcurrentHashMap<>();
    private final Map<UUID, Location> previousLocations = new ConcurrentHashMap<>();
    private final Map<String, IpStamp> ipUsage = new ConcurrentHashMap<>();

    public AuthManager(AuthRegPlugin plugin, AccountStorage storage, AuthConfig config, Messages messages) {
        this.plugin = plugin;
        this.storage = storage;
        this.config = config;
        this.messages = messages;
    }

    public void handleJoin(Player player) {
        authenticated.remove(player.getUniqueId());
        attempts.put(player.getUniqueId(), 0);
        previousLocations.put(player.getUniqueId(), player.getLocation());
        cleanupIpUsage();
        sendPrompt(player);
        loadLastLocation(player);
        teleportToLobby(player);
        startReminder(player);
        startTimeout(player);
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        cancelTask(reminders.remove(uuid));
        cancelTask(timeouts.remove(uuid));
        if (isAuthenticated(uuid)) {
            storage.updateLastLocationAsync(uuid, player.getLocation());
        }
        authenticated.remove(uuid);
        attempts.remove(uuid);
        previousLocations.remove(uuid);
    }

    public boolean isAuthenticated(UUID uuid) {
        return authenticated.contains(uuid);
    }

    public boolean isIpBlocked(String playerName, String ip) {
        if (ip == null) return false;
        IpStamp stamp = ipUsage.get(ip);
        if (stamp == null) return false;
        if (stamp.playerName.equalsIgnoreCase(playerName)) return false;
        long now = System.currentTimeMillis();
        return now - stamp.timestamp < config.getIpCooldownSeconds() * 1000L;
    }

    public long ipSecondsLeft(String ip) {
        IpStamp stamp = ipUsage.get(ip);
        if (stamp == null) return 0;
        long now = System.currentTimeMillis();
        long diff = config.getIpCooldownSeconds() * 1000L - (now - stamp.timestamp);
        return Math.max(0, diff / 1000);
    }

    public void recordIpUse(String playerName, String ip) {
        if (ip == null) return;
        ipUsage.put(ip, new IpStamp(playerName, System.currentTimeMillis()));
    }

    public void handleLogin(Player player, String password) {
        String ip = getPlayerIp(player);
        if (isIpBlocked(player.getName(), ip)) {
            long left = ipSecondsLeft(ip);
            player.sendMessage(messages.format("ip.cooldown", Map.of("seconds_left", String.valueOf(left))));
            return;
        }

        storage.findByUUIDAsync(player.getUniqueId()).thenAccept(accountOpt -> {
            if (accountOpt.isEmpty()) {
                runOnPlayer(player, () -> player.sendMessage(messages.get("login.not_registered")));
                return;
            }
            if (isAuthenticated(player.getUniqueId())) {
                runOnPlayer(player, () -> player.sendMessage(messages.get("login.already")));
                return;
            }

            boolean ok = org.mindrot.jbcrypt.BCrypt.checkpw(password, accountOpt.get().getPasswordHash());
            if (!ok) {
                int count = attempts.merge(player.getUniqueId(), 1, Integer::sum);
                int left = Math.max(0, config.getMaxAttempts() - count);
                if (count >= config.getMaxAttempts()) {
                    runOnPlayer(player, () -> player.kick(component(messages.raw("login.max_attempts"))));
                } else {
                    runOnPlayer(player, () -> player.sendMessage(messages.format("login.wrong_password", Map.of("attempts_left", String.valueOf(left)))));
                }
                return;
            }

            storage.updateLastIpAsync(player.getUniqueId(), ip);
            recordIpUse(player.getName(), ip);
            runOnPlayer(player, () -> {
                authenticate(player);
                player.sendMessage(messages.get("login.success"));
            });
        });
    }

    public void handleRegister(Player player, String password) {
        String ip = getPlayerIp(player);
        if (isIpBlocked(player.getName(), ip)) {
            long left = ipSecondsLeft(ip);
            player.sendMessage(messages.format("ip.cooldown", Map.of("seconds_left", String.valueOf(left))));
            return;
        }

        storage.findByUUIDAsync(player.getUniqueId()).thenAccept(accountOpt -> {
            if (accountOpt.isPresent()) {
                runOnPlayer(player, () -> player.sendMessage(messages.get("register.already")));
                return;
            }
            String hash = org.mindrot.jbcrypt.BCrypt.hashpw(password, org.mindrot.jbcrypt.BCrypt.gensalt());
            Location loc = previousLocations.getOrDefault(player.getUniqueId(), player.getLocation());
            storage.createAccountAsync(player.getUniqueId(), player.getName(), hash, ip, loc).thenRun(() -> {
                recordIpUse(player.getName(), ip);
                runOnPlayer(player, () -> player.sendMessage(messages.get("register.success")));
            });
        });
    }

    public void authenticate(Player player) {
        UUID uuid = player.getUniqueId();
        authenticated.add(uuid);
        attempts.remove(uuid);
        cancelTask(reminders.remove(uuid));
        cancelTask(timeouts.remove(uuid));
        returnPlayer(player);
    }

    public void sendPrompt(Player player) {
        storage.findByUUIDAsync(player.getUniqueId()).thenAccept(opt ->
                runOnPlayer(player, () -> {
                    if (opt.isEmpty()) {
                        player.sendMessage(messages.get("register.prompt"));
                    } else {
                        player.sendMessage(messages.get("login.prompt"));
                    }
                    sendTitle(player);
                })
        );
    }

    private void sendTitle(Player player) {
        AuthConfig.TitleSettings t = config.getTitleSettings();
        if (!t.isEnabled()) return;
        player.sendTitle(color(t.getTitle()), color(t.getSubtitle()), t.getFadeIn(), t.getStay(), t.getFadeOut());
    }

    private void startReminder(Player player) {
        long period = config.getReminderSeconds() * 20L;
        long initialDelay = Math.max(1L, period);
        ScheduledTask task = player.getScheduler().runAtFixedRate(plugin, scheduledTask -> {
            if (isAuthenticated(player.getUniqueId())) {
                scheduledTask.cancel();
                return;
            }
            sendTitle(player);
        }, null, initialDelay, period);
        reminders.put(player.getUniqueId(), task);
    }

    private void startTimeout(Player player) {
        long delay = config.getTimeoutSeconds() * 20L;
        ScheduledTask task = player.getScheduler().runDelayed(plugin, scheduledTask -> {
            if (!isAuthenticated(player.getUniqueId())) {
                player.kick(component(messages.raw("timeout.kick")));
            }
        }, null, delay);
        timeouts.put(player.getUniqueId(), task);
    }

    private void teleportToLobby(Player player) {
        if (!config.getTeleportSettings().isEnabled()) {
            return;
        }
        String mode = config.getTeleportSettings().getMode();
        Location target = null;
        switch (mode) {
            case "fixed":
                target = config.getTeleportSettings().getFixedLocation();
                break;
            case "spawn":
                target = player.getWorld().getSpawnLocation();
                break;
            case "previous":
                target = previousLocations.get(player.getUniqueId());
                break;
            default:
                target = player.getWorld().getSpawnLocation();
        }

        if (target != null) {
            player.teleportAsync(target);
        }
    }

    private void returnPlayer(Player player) {
        Location back = null;
        if (config.getTeleportSettings().isReturnToPrevious()) {
            back = previousLocations.get(player.getUniqueId());
        }
        if (back == null) {
            back = player.getWorld().getSpawnLocation();
        }
        player.teleportAsync(back);
    }

    private void cancelTask(ScheduledTask task) {
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    private String getPlayerIp(Player player) {
        InetSocketAddress addr = player.getAddress();
        if (addr == null || addr.getAddress() == null) return null;
        return addr.getAddress().getHostAddress();
    }

    public boolean isAllowedCommand(String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        for (String allowed : config.getProtectionSettings().getAllowedCommands()) {
            if (lower.startsWith("/")) {
                if (lower.startsWith("/" + allowed.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            } else if (lower.startsWith(allowed.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String color(String text) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private Component component(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message == null ? "" : message);
    }

    private void cleanupIpUsage() {
        long cutoff = System.currentTimeMillis() - config.getIpCooldownSeconds() * 1000L;
        ipUsage.entrySet().removeIf(e -> e.getValue().timestamp < cutoff);
    }

    private void runOnPlayer(Player player, Runnable runnable) {
        player.getScheduler().run(plugin, scheduledTask -> {
            if (!player.isOnline()) {
                return;
            }
            runnable.run();
        }, null);
    }

    public void saveOnlineLocationsNow() {
        plugin.getServer().getOnlinePlayers().forEach(p -> {
            storage.updateLastLocation(p.getUniqueId(), p.getLocation());
        });
    }

    private void loadLastLocation(Player player) {
        storage.findByUUIDAsync(player.getUniqueId()).thenAccept(opt -> opt.ifPresent(account -> {
            Location loc = accountToLocation(account);
            if (loc != null) {
                previousLocations.put(player.getUniqueId(), loc);
                if (config.getTeleportSettings().isEnabled() &&
                        "previous".equalsIgnoreCase(config.getTeleportSettings().getMode())) {
                    runOnPlayer(player, () -> player.teleportAsync(loc));
                }
            }
        }));
    }

    private Location accountToLocation(Account account) {
        if (account.getLastWorld() == null || account.getLastX() == null || account.getLastY() == null || account.getLastZ() == null) {
            return null;
        }
        var world = plugin.getServer().getWorld(account.getLastWorld());
        if (world == null) return null;
        float yaw = account.getLastYaw() == null ? 0f : account.getLastYaw();
        float pitch = account.getLastPitch() == null ? 0f : account.getLastPitch();
        return new Location(world, account.getLastX(), account.getLastY(), account.getLastZ(), yaw, pitch);
    }

    private static final class IpStamp {
        private final String playerName;
        private final long timestamp;

        private IpStamp(String playerName, long timestamp) {
            this.playerName = playerName;
            this.timestamp = timestamp;
        }
    }

    public void shutdown() {
        reminders.values().forEach(this::cancelTask);
        timeouts.values().forEach(this::cancelTask);
        reminders.clear();
        timeouts.clear();
        authenticated.clear();
        attempts.clear();
        previousLocations.clear();
        ipUsage.clear();
    }
}
