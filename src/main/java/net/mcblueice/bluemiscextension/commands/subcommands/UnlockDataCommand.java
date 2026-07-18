package net.mcblueice.bluemiscextension.commands.subcommands;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.commands.CommandManager;
import net.mcblueice.bluemiscextension.commands.SubCommand;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;
import net.kyori.adventure.text.Component;

public class UnlockDataCommand implements SubCommand {
    private final BlueMiscExtension plugin;
    private final CommandManager commandManager;
    private final ConfigManager lang;
    private final DatabaseUtil databaseUtil;

    public UnlockDataCommand(BlueMiscExtension plugin, CommandManager commandManager) {
        this.plugin = plugin;
        this.commandManager = commandManager;
        this.lang = plugin.getLanguageManager();
        this.databaseUtil = plugin.getDatabaseUtil();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bluemiscextension.unlockdata")) return commandManager.NoPermission(sender);
        if (args.length < 2) return commandManager.UsageError(sender);

        String targetName = args[1];
        if (plugin.getServer().getPlayerExact(targetName) instanceof Player target) {
            UUID uuid = target.getUniqueId();
            target.kick(Component.text(lang.get("UnlockData.KickMessage")));
            databaseUtil.updateDatabaseField(uuid, "is_data_saved", true).thenRun(() ->
                sender.sendMessage(lang.get("Prefix.Default") + lang.get("ForceUnlockData", targetName))
            );
        } else {
            sender.sendMessage(lang.get("Prefix.Default") + lang.get("SearchingPlayer", targetName));
            databaseUtil.getUUID(targetName).thenAccept(uuid -> {
                if (uuid == null) {
                    sender.sendMessage(lang.get("Prefix.Default") + lang.get("PlayerNotFound", targetName));
                    return;
                }
                databaseUtil.updateDatabaseField(uuid, "is_data_saved", true).thenRun(() ->
                    sender.sendMessage(lang.get("Prefix.Default") + lang.get("ForceUnlockData", targetName))
                );
            });
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            if (!sender.hasPermission("bluemiscextension.unlockdata.other")) return Collections.emptyList();
            return null;
        }
        return Collections.emptyList();
    }
}
