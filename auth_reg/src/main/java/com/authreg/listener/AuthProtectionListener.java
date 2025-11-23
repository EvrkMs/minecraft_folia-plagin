package com.authreg.listener;

import com.authreg.config.AuthConfig;
import com.authreg.config.Messages;
import com.authreg.user.AuthManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.entity.Player;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuthProtectionListener implements Listener {
    private final AuthManager authManager;
    private final Messages messages;
    private final AuthConfig config;
    private final Map<UUID, Long> warnCooldown = new HashMap<>();

    public AuthProtectionListener(AuthManager authManager, Messages messages, AuthConfig config) {
        this.authManager = authManager;
        this.messages = messages;
        this.config = config;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        authManager.handleJoin(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        authManager.handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!config.getProtectionSettings().isLockMovement()) return;
        Player player = event.getPlayer();
        if (!authManager.isAuthenticated(player.getUniqueId())) {
            if (event.getTo() != null && !event.getFrom().toVector().equals(event.getTo().toVector())) {
                event.setTo(event.getFrom());
            }
            warn(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!config.getProtectionSettings().isLockCommands()) return;
        Player player = event.getPlayer();
        if (authManager.isAuthenticated(player.getUniqueId())) return;
        String msg = event.getMessage().toLowerCase();
        if (!authManager.isAllowedCommand(msg.startsWith("/") ? msg.substring(1) : msg)) {
            event.setCancelled(true);
            warn(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!config.getProtectionSettings().isLockDamage()) return;
        if (!authManager.isAuthenticated(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!config.getProtectionSettings().isLockInteract()) return;
        Player player = event.getPlayer();
        if (!authManager.isAuthenticated(player.getUniqueId())) {
            event.setCancelled(true);
            warn(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!config.getProtectionSettings().isLockInteract()) return;
        Player player = event.getPlayer();
        if (!authManager.isAuthenticated(player.getUniqueId())) {
            event.setCancelled(true);
            warn(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventory(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!config.getProtectionSettings().isLockInventory()) return;
        if (!authManager.isAuthenticated(player.getUniqueId())) {
            event.setCancelled(true);
            warn(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (!config.getProtectionSettings().isLockItemSwitch()) return;
        Player player = event.getPlayer();
        if (!authManager.isAuthenticated(player.getUniqueId())) {
            event.setCancelled(true);
            warn(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) {
        if (!config.getProtectionSettings().isLockItemSwitch()) return;
        Player player = event.getPlayer();
        if (!authManager.isAuthenticated(player.getUniqueId())) {
            event.setCancelled(true);
            warn(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!authManager.isAuthenticated(player.getUniqueId())) {
            event.setCancelled(true);
            warn(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!authManager.isAuthenticated(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!authManager.isAuthenticated(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!config.getProtectionSettings().isLockInteract()) return;
        Player player = event.getPlayer();
        if (!authManager.isAuthenticated(player.getUniqueId())) {
            event.setCancelled(true);
            warn(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!config.getProtectionSettings().isLockInteract()) return;
        Player player = event.getPlayer();
        if (!authManager.isAuthenticated(player.getUniqueId())) {
            event.setCancelled(true);
            warn(player);
        }
    }

    private void warn(Player player) {
        long now = System.currentTimeMillis();
        long last = warnCooldown.getOrDefault(player.getUniqueId(), 0L);
        if (now - last > 1500) {
            player.sendMessage(messages.get("protections.blocked"));
            warnCooldown.put(player.getUniqueId(), now);
        }
    }
}
