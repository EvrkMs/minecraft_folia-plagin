package com.authreg.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class Messages {
    private final JavaPlugin plugin;
    private FileConfiguration messagesCfg;
    private String prefix;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messagesCfg = YamlConfiguration.loadConfiguration(file);
        this.prefix = color(messagesCfg.getString("prefix", "&7[Auth]"));
    }

    public String get(String path) {
        String raw = messagesCfg.getString(path, path);
        return prefix + " " + color(raw);
    }

    public String raw(String path) {
        String raw = messagesCfg.getString(path, path);
        return color(raw);
    }

    public String format(String path, Map<String, String> placeholders) {
        String msg = get(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return msg;
    }

    public String formatRaw(String path, Map<String, String> placeholders) {
        String msg = raw(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return msg;
    }

    public void saveDefaults() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message == null ? "" : message);
    }
}
