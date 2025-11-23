package com.authreg.command;

import com.authreg.AuthRegPlugin;
import com.authreg.user.AuthManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegisterCommand implements CommandExecutor {
    private final AuthRegPlugin plugin;

    public RegisterCommand(AuthRegPlugin plugin) {
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
        String password = args[0];
        AuthManager manager = plugin.getAuthManager();
        manager.handleRegister(player, password);
        return true;
    }
}
