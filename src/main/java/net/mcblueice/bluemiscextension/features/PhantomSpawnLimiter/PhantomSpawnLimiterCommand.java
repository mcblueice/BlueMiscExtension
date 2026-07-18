package net.mcblueice.bluemiscextension.features.PhantomSpawnLimiter;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.commands.CommandManager;
import net.mcblueice.bluemiscextension.commands.SubCommand;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;

public class PhantomSpawnLimiterCommand implements SubCommand {
    private final BlueMiscExtension plugin;
    private final CommandManager commandManager;
    private final ConfigManager lang;
    private final DatabaseUtil databaseUtil;

    public PhantomSpawnLimiterCommand(BlueMiscExtension plugin, PhantomSpawnLimiter feature) {
        this.plugin = plugin;
        this.commandManager = plugin.getCommandManager();
        this.lang = plugin.getLanguageManager();
        this.databaseUtil = plugin.getDatabaseUtil();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bluemiscextension.phantom")) return commandManager.NoPermission(sender);

        // self toggle
        if (args.length == 1) {
            if (!(sender instanceof Player player)) return commandManager.OnlyPlayer(sender);

            boolean currentState = databaseUtil.getPlayerData(player.getUniqueId()).phantomSpawn();
            boolean newState = !currentState;

            databaseUtil.setPhantomSpawnState(player.getUniqueId(), newState);

            player.sendMessage(lang.get("Prefix.Default") + lang.get(!newState ? "PhantomSpawnLimiter.ToggleOn" : "PhantomSpawnLimiter.ToggleOff"));
            return true;
        }

        // toggle for others
        if (args.length == 2) {
            if (!sender.hasPermission("bluemiscextension.phantom.other")) return commandManager.NoPermission(sender);
            Player target = plugin.getServer().getPlayerExact(args[1]);
            if (target == null) return commandManager.PlayerNotFound(sender, args[1]);

            boolean currentState = databaseUtil.getPlayerData(target.getUniqueId()).phantomSpawn();
            boolean newState = !currentState;

            databaseUtil.setPhantomSpawnState(target.getUniqueId(), newState);

            sender.sendMessage(lang.get("Prefix.Default") + lang.get(!newState ? "PhantomSpawnLimiter.ToggleOnOther" : "PhantomSpawnLimiter.ToggleOffOther", target.getName()));
            target.sendMessage(lang.get("Prefix.Default") + lang.get(!newState ? "PhantomSpawnLimiter.ToggleOn" : "PhantomSpawnLimiter.ToggleOff"));
            return true;
        }
        return commandManager.UsageError(sender);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // player names
        if (args.length == 2) {
            if (!sender.hasPermission("bluemiscextension.phantom.other")) return Collections.emptyList();
            return null;
        }
        return Collections.emptyList();
    }
}