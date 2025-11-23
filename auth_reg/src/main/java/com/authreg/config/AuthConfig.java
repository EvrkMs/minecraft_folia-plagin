package com.authreg.config;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class AuthConfig {
    private final JavaPlugin plugin;
    private int maxAttempts;
    private int timeoutSeconds;
    private int ipCooldownSeconds;
    private int reminderSeconds;
    private TitleSettings titleSettings;
    private ProtectionSettings protectionSettings;
    private TeleportSettings teleportSettings;
    private String databaseFile;
    private boolean consoleOnlyAdmin;

    public AuthConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        FileConfiguration cfg = plugin.getConfig();
        this.maxAttempts = cfg.getInt("login.max-attempts", 5);
        this.timeoutSeconds = cfg.getInt("login.timeout-seconds", 60);
        this.ipCooldownSeconds = cfg.getInt("login.ip-cooldown-seconds", 120);
        this.reminderSeconds = Math.max(1, cfg.getInt("login.reminder-seconds", 10));
        this.titleSettings = new TitleSettings(cfg);
        this.protectionSettings = new ProtectionSettings(cfg);
        this.teleportSettings = new TeleportSettings(plugin, cfg);
        this.databaseFile = cfg.getString("database.file", "plugins/auth_reg/data/auth.db");
        this.consoleOnlyAdmin = cfg.getBoolean("admin.console-only", true);
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getIpCooldownSeconds() {
        return ipCooldownSeconds;
    }

    public int getReminderSeconds() {
        return reminderSeconds;
    }

    public TitleSettings getTitleSettings() {
        return titleSettings;
    }

    public ProtectionSettings getProtectionSettings() {
        return protectionSettings;
    }

    public TeleportSettings getTeleportSettings() {
        return teleportSettings;
    }

    public String getDatabaseFile() {
        return databaseFile;
    }

    public boolean isConsoleOnlyAdmin() {
        return consoleOnlyAdmin;
    }

    public static class TitleSettings {
        private final boolean enabled;
        private final String title;
        private final String subtitle;
        private final int fadeIn;
        private final int stay;
        private final int fadeOut;

        TitleSettings(FileConfiguration cfg) {
            this.enabled = cfg.getBoolean("login.title.enabled", true);
            this.title = cfg.getString("login.title.title", "&cАвторизация");
            this.subtitle = cfg.getString("login.title.subtitle", "&fВведите &b/login <пароль> &fили &b/register <пароль>");
            this.fadeIn = cfg.getInt("login.title.fade-in", 5);
            this.stay = cfg.getInt("login.title.stay", 40);
            this.fadeOut = cfg.getInt("login.title.fade-out", 5);
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getTitle() {
            return title;
        }

        public String getSubtitle() {
            return subtitle;
        }

        public int getFadeIn() {
            return fadeIn;
        }

        public int getStay() {
            return stay;
        }

        public int getFadeOut() {
            return fadeOut;
        }
    }

    public static class ProtectionSettings {
        private final boolean lockMovement;
        private final boolean lockCommands;
        private final boolean lockDamage;
        private final boolean lockInteract;
        private final boolean lockInventory;
        private final boolean lockItemSwitch;
        private final List<String> allowedCommands;

        ProtectionSettings(FileConfiguration cfg) {
            this.lockMovement = cfg.getBoolean("protection.lock-movement", true);
            this.lockCommands = cfg.getBoolean("protection.lock-commands", true);
            this.lockDamage = cfg.getBoolean("protection.lock-damage", true);
            this.lockInteract = cfg.getBoolean("protection.lock-interact", true);
            this.lockInventory = cfg.getBoolean("protection.lock-inventory", true);
            this.lockItemSwitch = cfg.getBoolean("protection.lock-item-switch", true);
            List<String> cmds = cfg.getStringList("protection.allowed-commands");
            this.allowedCommands = cmds == null ? Collections.emptyList() : cmds;
        }

        public boolean isLockMovement() {
            return lockMovement;
        }

        public boolean isLockCommands() {
            return lockCommands;
        }

        public boolean isLockDamage() {
            return lockDamage;
        }

        public boolean isLockInteract() {
            return lockInteract;
        }

        public boolean isLockInventory() {
            return lockInventory;
        }

        public boolean isLockItemSwitch() {
            return lockItemSwitch;
        }

        public List<String> getAllowedCommands() {
            return allowedCommands;
        }
    }

    public static class TeleportSettings {
        private final boolean enabled;
        private final String mode;
        private final boolean returnToPrevious;
        private final Location fixedLocation;

        TeleportSettings(JavaPlugin plugin, FileConfiguration cfg) {
            this.enabled = cfg.getBoolean("teleport.enabled", true);
            this.mode = cfg.getString("teleport.mode", "spawn").toLowerCase();
            this.returnToPrevious = cfg.getBoolean("teleport.return-to-previous", true);
            Location fixed = null;
            try {
                String worldName = cfg.getString("teleport.fixed.world", "world");
                World world = plugin.getServer().getWorld(worldName);
                if (world != null) {
                    double x = cfg.getDouble("teleport.fixed.x", 0.0);
                    double y = cfg.getDouble("teleport.fixed.y", 200.0);
                    double z = cfg.getDouble("teleport.fixed.z", 0.0);
                    float yaw = (float) cfg.getDouble("teleport.fixed.yaw", 0.0);
                    float pitch = (float) cfg.getDouble("teleport.fixed.pitch", 0.0);
                    fixed = new Location(world, x, y, z, yaw, pitch);
                }
            } catch (Exception ignored) {
                fixed = null;
            }
            this.fixedLocation = fixed;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public String getMode() {
            return mode;
        }

        public boolean isReturnToPrevious() {
            return returnToPrevious;
        }

        public Location getFixedLocation() {
            return fixedLocation;
        }
    }
}
