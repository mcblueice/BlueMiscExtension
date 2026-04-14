package net.mcblueice.bluemiscextension.features.ArmorHide;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.commands.CommandManager;
import net.mcblueice.bluemiscextension.commands.SubCommand;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;

public class ArmorHideCommand implements SubCommand {
    private final BlueMiscExtension plugin;
    private final CommandManager commandManager;
    private final ArmorHide feature;
    private final ConfigManager lang;
    private final DatabaseUtil databaseUtil;

    public ArmorHideCommand(BlueMiscExtension plugin, ArmorHide feature) {
        this.plugin = plugin;
        this.commandManager = plugin.getCommandManager();
        this.feature = feature;
        this.lang = plugin.getLanguageManager();
        this.databaseUtil = plugin.getDatabaseUtil();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bluemiscextension.armorhide")) return commandManager.NoPermission(sender);

        // self toggle
        if (args.length == 1) { 
            if (!(sender instanceof Player player)) return commandManager.OnlyPlayer(sender);

            boolean newState = !databaseUtil.getPlayerData(player.getUniqueId()).hiddenArmor();
            databaseUtil.setArmorHiddenState(player.getUniqueId(), newState);
            feature.updatePlayer(player);

            player.sendMessage(lang.get("Prefix.Default") + lang.get(newState ? "ArmorHideFeature.ToggleOn" : "ArmorHideFeature.ToggleOff"));
            return true;
        }

        // toggle for others
        if (args.length == 2) {
            if (!sender.hasPermission("bluemiscextension.armorhide.other")) return commandManager.NoPermission(sender);
            Player target = plugin.getServer().getPlayerExact(args[1]);
            if (target == null) return commandManager.PlayerNotFound(sender, args[1]);

            boolean newState = !databaseUtil.getPlayerData(target.getUniqueId()).hiddenArmor();
            databaseUtil.setArmorHiddenState(target.getUniqueId(), newState);
            feature.updatePlayer(target);

            sender.sendMessage(lang.get("Prefix.Default") + lang.get(newState ? "ArmorHideFeature.ToggleOnOther" : "ArmorHideFeature.ToggleOffOther", target.getName()));
            target.sendMessage(lang.get("Prefix.Default") + lang.get(newState ? "ArmorHideFeature.ToggleOn" : "ArmorHideFeature.ToggleOff", target.getName()));
            return true;
        }

        return commandManager.UsageError(sender);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        // player names
        if (args.length == 2) {
            if (!sender.hasPermission("bluemiscextension.armorhide.other")) return Collections.emptyList();
            return null;
        }
        return Collections.emptyList();
    }
}
