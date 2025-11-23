package com.authreg.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.LogEvent;
import org.bukkit.plugin.Plugin;

import java.util.Locale;
import java.util.Set;

public class CommandLogFilter extends AbstractFilter {
    private static final Set<String> SENSITIVE = Set.of("/login", "/l", "/register", "/reg");

    @Override
    public Result filter(LogEvent event) {
        String msg = event.getMessage() == null ? null : event.getMessage().getFormattedMessage();
        if (msg == null) return Result.NEUTRAL;
        return shouldBlock(msg);
    }

    private Result shouldBlock(String msg) {
        String lower = msg.toLowerCase(Locale.ROOT);
        if (!lower.contains("issued server command:")) {
            return Result.NEUTRAL;
        }
        for (String cmd : SENSITIVE) {
            if (lower.contains("issued server command: " + cmd)) {
                return Result.DENY;
            }
        }
        return Result.NEUTRAL;
    }

    public static void register(Plugin plugin) {
        try {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            CommandLogFilter filter = new CommandLogFilter();
            config.addFilter(filter);
            ctx.updateLoggers();
            plugin.getLogger().info("Sensitive command log filter enabled.");
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to register command log filter: " + t.getMessage());
        }
    }
}
