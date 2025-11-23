package com.authreg;

import com.authreg.command.AdminCommand;
import com.authreg.command.LoginCommand;
import com.authreg.command.RegisterCommand;
import com.authreg.config.AuthConfig;
import com.authreg.config.Messages;
import com.authreg.logging.CommandLogFilter;
import com.authreg.listener.AuthProtectionListener;
import com.authreg.storage.AccountStorage;
import com.authreg.user.AuthManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuthRegPlugin extends JavaPlugin {

    private AuthConfig authConfig;
    private Messages messages;
    private AccountStorage accountStorage;
    private AuthManager authManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.authConfig = new AuthConfig(this);
        this.messages = new Messages(this);
        this.accountStorage = new AccountStorage(this);
        this.authManager = new AuthManager(this, accountStorage, authConfig, messages);

        accountStorage.init(authConfig.getDatabaseFile());
        CommandLogFilter.register(this);

        registerCommands();
        getServer().getPluginManager().registerEvents(new AuthProtectionListener(authManager, messages, authConfig), this);
    }

    @Override
    public void onDisable() {
        if (authManager != null) {
            authManager.saveOnlineLocationsNow();
            authManager.shutdown();
        }
        if (accountStorage != null) {
            accountStorage.close();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        messages.reload();
        authConfig.reload();
        accountStorage.init(authConfig.getDatabaseFile());
    }

    public AuthConfig getAuthConfig() {
        return authConfig;
    }

    public Messages getMessages() {
        return messages;
    }

    public AccountStorage getAccountStorage() {
        return accountStorage;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    private void registerCommands() {
        PluginCommand register = getCommand("register");
        if (register != null) {
            register.setExecutor(new RegisterCommand(this));
            register.setTabCompleter((sender, command, alias, args) -> java.util.Collections.emptyList());
        }

        PluginCommand login = getCommand("login");
        if (login != null) {
            login.setExecutor(new LoginCommand(this));
            login.setTabCompleter((sender, command, alias, args) -> java.util.Collections.emptyList());
        }

        PluginCommand admin = getCommand("authreg");
        if (admin != null) {
            AdminCommand adminCommand = new AdminCommand(this);
            admin.setExecutor(adminCommand);
            admin.setTabCompleter(adminCommand);
        }
    }
}
