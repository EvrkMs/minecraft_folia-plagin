package com.authreg.command;

import com.authreg.AuthRegPlugin;
import com.authreg.storage.Account;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.ConsoleCommandSender;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class AdminCommand implements CommandExecutor, TabCompleter {
    private final AuthRegPlugin plugin;

    public AdminCommand(AuthRegPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean consoleOnly = plugin.getAuthConfig().isConsoleOnlyAdmin();
        if (consoleOnly && !(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(plugin.getMessages().get("admin.console_only"));
            return true;
        }

        if (!consoleOnly && !sender.hasPermission("authreg.admin")) {
            sender.sendMessage(plugin.getMessages().get("admin.console_only"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("/authreg reload");
            sender.sendMessage("/authreg reset <ник> <новый_пароль>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadPlugin();
                sender.sendMessage(plugin.getMessages().get("admin.reload"));
                return true;
            case "reset":
                if (args.length < 3) {
                    sender.sendMessage("/authreg reset <ник> <новый_пароль>");
                    return true;
                }
                String target = args[1];
                String newPass = args[2];
                CompletableFuture<Optional<Account>> future = plugin.getAccountStorage().findByNameAsync(target);
                future.thenAccept(opt -> {
                    if (opt.isEmpty()) {
                        sendLater(sender, plugin.getMessages().format("admin.reset_not_found", Map.of("player", target)));
                        return;
                    }
                    String hash = org.mindrot.jbcrypt.BCrypt.hashpw(newPass, org.mindrot.jbcrypt.BCrypt.gensalt());
                    plugin.getAccountStorage().updatePasswordAsync(opt.get().getUuid(), hash).thenRun(() ->
                            sendLater(sender, plugin.getMessages().format("admin.reset_success", Map.of("player", opt.get().getName())))
                    );
                });
                return true;
            default:
                sender.sendMessage("/authreg reload");
                sender.sendMessage("/authreg reset <ник> <новый_пароль>");
                return true;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean consoleOnly = plugin.getAuthConfig().isConsoleOnlyAdmin();
        if (consoleOnly && !(sender instanceof ConsoleCommandSender)) {
            return Collections.emptyList();
        }
        if (!consoleOnly && !sender.hasPermission("authreg.admin")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return Arrays.asList("reload", "reset");
        }
        return Collections.emptyList();
    }

    private void sendLater(CommandSender sender, String message) {
        plugin.getServer().getGlobalRegionScheduler().execute(plugin, () -> sender.sendMessage(message));
    }
}
