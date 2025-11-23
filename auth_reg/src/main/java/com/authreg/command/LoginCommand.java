package com.authreg.command;

import com.authreg.AuthRegPlugin;
import com.authreg.user.AuthManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LoginCommand implements CommandExecutor {
    private final AuthRegPlugin plugin;

    public LoginCommand(AuthRegPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только игрок может выполнять эту команду.");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(command.getUsage());
            return true;
        }
        AuthManager manager = plugin.getAuthManager();
        manager.handleLogin(player, args[0]);
        return true;
    }
}
