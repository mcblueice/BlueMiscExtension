package net.mcblueice.bluemiscextension.features.VirtualWorkbench;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.commands.CommandManager;
import net.mcblueice.bluemiscextension.commands.SubCommand;
import net.mcblueice.bluemiscextension.utils.ConfigManager;

public class VirtualWorkbenchCommand implements SubCommand {
    private final BlueMiscExtension plugin;
    private final CommandManager commandManager;
    private final VirtualWorkbench virtualWorkbench;
    private final ConfigManager lang;

    public VirtualWorkbenchCommand(BlueMiscExtension plugin, VirtualWorkbench virtualWorkbench) {
        this.plugin = plugin;
        this.commandManager = plugin.getCommandManager();
        this.virtualWorkbench = virtualWorkbench;
        this.lang = plugin.getLanguageManager();
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bluemiscextension.workbench")) return commandManager.NoPermission(sender);
        if (args.length < 2) return commandManager.UsageError(sender);

        String workbenchType = args[1].toLowerCase();
        if (!sender.hasPermission("bluemiscextension.workbench." + workbenchType)) return commandManager.NoPermission(sender);

        // self open
        if (args.length == 2) { 
            if (!(sender instanceof Player player)) return commandManager.OnlyPlayer(sender);
            virtualWorkbench.open(player, workbenchType);
            return true;
        }

        // open for others
        if (args.length == 3) {
            if (!sender.hasPermission("bluemiscextension.workbench.other")) return commandManager.NoPermission(sender);
            Player target = plugin.getServer().getPlayerExact(args[2]);
            if (target == null) return commandManager.PlayerNotFound(sender, args[2]);

            virtualWorkbench.open(target, workbenchType);
            sender.sendMessage(lang.get("Prefix.Default") + lang.get("VirtualWorkbench.OpenedOther", target.getName(), workbenchType));
            return true;
        }

        return commandManager.UsageError(sender);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        // workbench types
        if (args.length == 2) {
            List<String> subs = new ArrayList<>();
            for (String workbenchType : VirtualWorkbench.WORKBENCH_TYPES) {
                if (sender.hasPermission("bluemiscextension.workbench." + workbenchType.toLowerCase())) subs.add(workbenchType);
            }
            StringUtil.copyPartialMatches(args[1], subs, completions);
            Collections.sort(completions);
            return completions;
        }

        // player names
        if (args.length == 3) {
            if (VirtualWorkbench.WORKBENCH_TYPES.contains(args[1].toUpperCase())) {
                if (!sender.hasPermission("bluemiscextension.workbench.other")) return Collections.emptyList();
                return null;
            }
        }
        return Collections.emptyList();
    }
}
