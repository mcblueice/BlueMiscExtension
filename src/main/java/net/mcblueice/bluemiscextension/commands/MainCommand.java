package net.mcblueice.bluemiscextension.commands;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.FeatureManager;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;
import net.mcblueice.bluemiscextension.utils.StatusUtil;
import net.mcblueice.bluelib.utils.TaskScheduler;

public class MainCommand implements SubCommand {

    private final BlueMiscExtension plugin;
    private final CommandManager commandManager;
    private final ConfigManager lang;
    private final FeatureManager featureManager;
    private final DatabaseUtil databaseUtil;

    public MainCommand(BlueMiscExtension plugin, CommandManager commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.lang = plugin.getLanguageManager();
        this.featureManager = plugin.getFeatureManager();
        this.databaseUtil = plugin.getDatabaseUtil();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0) return commandManager.UsageError(sender);

        Player player;
        switch (args[0].toUpperCase()) {
            case "RELOAD":
                if (!sender.hasPermission("bluemiscextension.reload")) return commandManager.NoPermission(sender);
                TaskScheduler.runGlobalTask(plugin, () -> {
                    plugin.reloadConfig();
                    lang.reload();
                    featureManager.reload();
                    if (plugin.getAliasCommand() != null) plugin.getAliasCommand().reload();
                    sender.sendMessage(lang.get("Prefix.Default") + lang.get("ReloadSuccess"));
                });
                return true;
            case "DEBUG":
                if (!sender.hasPermission("bluemiscextension.debug")) return commandManager.NoPermission(sender);
                if (sender instanceof Player) {
                    player = (Player) sender;
                    boolean stat = plugin.toggleDebugMode(player.getUniqueId());
                    sender.sendMessage(lang.get("Prefix.Default") + lang.get(stat ? "DebugEnabled" : "DebugDisabled"));
                } else {
                    boolean stat = plugin.toggleDebugMode(plugin.CONSOLE_UUID);
                    sender.sendMessage(lang.get("Prefix.Default") + lang.get(stat ? "DebugEnabled" : "DebugDisabled"));
                }
                return true;
            case "STATUS":
                if (!sender.hasPermission("bluemiscextension.status")) return commandManager.NoPermission(sender);
                TaskScheduler.runGlobalTask(plugin, () -> {
                    sender.sendMessage(StatusUtil.getServerStatus(sender));
                });
                TaskScheduler.runAsync(plugin, () -> {
                    sender.sendMessage(databaseUtil.getDatabaseStatus());
                });
                return true;
            case "UNLOCKDATA":
                if (!sender.hasPermission("bluemiscextension.unlockdata")) return commandManager.NoPermission(sender);
                if (args.length < 2) return commandManager.UsageError(sender);
                String name = args[1];
                if (plugin.getServer().getPlayerExact(name) instanceof Player target) {
                        UUID uuid = target.getUniqueId();
                        target.kick(Component.text("§6管理員正在修復你的資料 請稍後重新登入"));
                        databaseUtil.updateDatabaseField(uuid, "is_data_saved", true).thenRun(() -> {
                            sender.sendMessage(lang.get("Prefix.Default") + lang.get("ForceUnlockData", name));
                        });
                } else {
                    sender.sendMessage(lang.get("Prefix.Default") + lang.get("SearchingPlayer", name));
                    databaseUtil.getUUID(name).thenAccept(uuid -> {
                        if (uuid == null) {
                            sender.sendMessage(lang.get("Prefix.Default") + lang.get("PlayerNotFound", name));
                            return;
                        }
                        databaseUtil.updateDatabaseField(uuid, "is_data_saved", true).thenRun(() -> {
                            sender.sendMessage(lang.get("Prefix.Default") + lang.get("ForceUnlockData", name));
                        });
                    });
                }
                return true;
            default:
                return commandManager.UsageError(sender);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("unlockdata")) {
            if (!sender.hasPermission("bluemiscextension.unlockdata")) return Collections.emptyList();
            return null;
        }

        return Collections.emptyList();
    }
}
