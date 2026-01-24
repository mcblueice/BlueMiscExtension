package net.mcblueice.bluemiscextension.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import net.mcblueice.bluemiscextension.features.VirtualWorkbench.VirtualWorkbench;
import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.FeatureManager;
import net.mcblueice.bluemiscextension.features.ArmorHide.ArmorHide;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;
import net.mcblueice.bluemiscextension.utils.ServerUtil;
import net.mcblueice.bluemiscextension.utils.TaskScheduler;

public class Commands implements CommandExecutor, TabCompleter {
    private final BlueMiscExtension plugin;
    private final ConfigManager lang;
    private final FeatureManager featureManager;
    private final DatabaseUtil databaseUtil;

    public Commands(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.featureManager = plugin.getFeatureManager();
        this.databaseUtil = plugin.getDatabaseUtil();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            Player player;
            switch (args[0].toUpperCase()) {
                case "RELOAD":
                    if (!sender.hasPermission("bluemiscextension.reload")) return NoPermission(sender);
                    plugin.reloadConfig();
                    lang.reload();
                    featureManager.reload();
                    sender.sendMessage(lang.get("Prefix.Default") + lang.get("ReloadSuccess"));
                    return true;
                case "DEBUG":
                    if (!sender.hasPermission("bluemiscextension.debug")) return NoPermission(sender);
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
                    if (!sender.hasPermission("bluemiscextension.status")) return NoPermission(sender);
                    TaskScheduler.runGlobalTask(plugin, () -> {
                        sender.sendMessage(ServerUtil.getServerStatus(sender));
                    });
                    TaskScheduler.runAsync(plugin, () -> {
                        sender.sendMessage(databaseUtil.getDatabaseStatus());
                    });
                    return true;
                case "ARMORHIDE":
                    if (!sender.hasPermission("bluemiscextension.armorhide")) return NoPermission(sender);
                    ArmorHide armorHide = featureManager.getFeature(ArmorHide.class);
                    if (armorHide == null) return NotEnabled(sender, "ArmorHideFeature");
                    if (args.length == 1) {
                        if (!(sender instanceof Player)) return OnlyPlayer(sender);
                        player = (Player) sender;
                        boolean newState = !databaseUtil.getArmorHiddenState(player.getUniqueId());
                        databaseUtil.setArmorHiddenState(player.getUniqueId(), newState);
                        armorHide.updatePlayer(player);
                        player.sendMessage(lang.get("Prefix.Default") + lang.get(newState ? "ArmorHideFeature.ToggleOn" : "ArmorHideFeature.ToggleOff"));
                        return true;
                    }
                    if (args.length == 2) {
                        if (!sender.hasPermission("bluemiscextension.armorhide.other")) return NoPermission(sender);
                        Player target = plugin.getServer().getPlayerExact(args[1]);
                        if (target == null) return PlayerNotFound(sender, args[1]);
                        boolean newState = !databaseUtil.getArmorHiddenState(target.getUniqueId());
                        databaseUtil.setArmorHiddenState(target.getUniqueId(), newState);
                        armorHide.updatePlayer(target);
                        sender.sendMessage(lang.get("Prefix.Default") + lang.get(newState ? "ArmorHideFeature.ToggleOnOther" : "ArmorHideFeature.ToggleOffOther", target.getName()));
                        target.sendMessage(lang.get("Prefix.Default") + lang.get(newState ? "ArmorHideFeature.ToggleOn" : "ArmorHideFeature.ToggleOff", target.getName()));
                        return true;
                    }
                    return UsageError(sender);
                case "WORKBENCH":
                    if (!sender.hasPermission("bluemiscextension.workbench")) return NoPermission(sender);
                    String workbenchType = args[1].toLowerCase();
                    if (!sender.hasPermission("bluemiscextension.workbench." + workbenchType)) return NoPermission(sender);
                    VirtualWorkbench workbench = featureManager.getFeature(VirtualWorkbench.class);
                    if (workbench == null) return NotEnabled(sender, "VirtualWorkbenchFeature");
                    if (args.length == 1) return UsageError(sender);
                    if (args.length == 2) {
                        if (!(sender instanceof Player)) return OnlyPlayer(sender);
                        player = (Player) sender;
                        workbench.open(player, workbenchType);
                        return true;
                    }
                    if (args.length == 3) {
                        if (!sender.hasPermission("bluemiscextension.workbench.other")) return NoPermission(sender);
                        Player target = plugin.getServer().getPlayerExact(args[2]);
                        if (target == null) return PlayerNotFound(sender, args[2]);
                        workbench.open(target, workbenchType);
                        sender.sendMessage(lang.get("Prefix.Default") + lang.get("VirtualWorkbench.OpenedOther", target.getName(), workbenchType));
                        return true;
                    }
                    return UsageError(sender);
                case "UNLOCKDATA":
                    if (!sender.hasPermission("bluemiscextension.workbench")) return NoPermission(sender);
                    Player target = plugin.getServer().getPlayerExact(args[1]);
                    if (target == null) return PlayerNotFound(sender, args[1]);
                    databaseUtil.updateDatabaseField(target.getUniqueId(), "is_data_saved", true).thenRun(() -> {
                        sender.sendMessage(lang.get("Prefix.Default") + lang.get("ForceUnlockData", target.getName()));
                    });
                default:
                    return UsageError(sender);
            }
        }
        return UsageError(sender);
    }

    private boolean UsageError(CommandSender sender) {
        sender.sendMessage(lang.get("Prefix.Default") + lang.get("UsageError"));
        return true;
    }
    private boolean OnlyPlayer(CommandSender sender) {
        sender.sendMessage(lang.get("Prefix.Default") + lang.get("OnlyPlayer"));
        return true;
    }
    private boolean PlayerNotFound(CommandSender sender, String playerName) {
        sender.sendMessage(lang.get("Prefix.Default") + lang.get("PlayerNotFound", playerName));
        return true;
    }
    private boolean NoPermission(CommandSender sender) {
        sender.sendMessage(lang.get("Prefix.Default") + lang.get("NoPermission"));
        return true;
    }
    private boolean NotEnabled(CommandSender sender, String featureName) {
        sender.sendMessage(lang.get("Prefix.Default") + lang.get(featureName + ".NotEnabled"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (sender.hasPermission("bluemiscextension.reload")) subs.add("reload");
            if (sender.hasPermission("bluemiscextension.debug")) subs.add("debug");
            if (sender.hasPermission("bluemiscextension.status")) subs.add("status");
            if (sender.hasPermission("bluemiscextension.armor")) subs.add("armor");
            if (sender.hasPermission("bluemiscextension.unlockdata")) subs.add("unlockdata");
            if (sender.hasPermission("bluemiscextension.workbench") && featureManager.isFeatureEnabled(VirtualWorkbench.class)) subs.add("workbench");
            if (sender.hasPermission("bluemiscextension.armorhide") && featureManager.isFeatureEnabled(ArmorHide.class)) subs.add("armorhide");
            StringUtil.copyPartialMatches(args[0], subs, completions);
            Collections.sort(completions);
            return completions;
        }
        if (args.length == 2) {
            List<String> subs = new ArrayList<>();
            if (args[0].equalsIgnoreCase("workbench")) {
                if (sender.hasPermission("bluemiscextension.workbench.workbench")) subs.add("WORKBENCH");
                if (sender.hasPermission("bluemiscextension.workbench.anvil")) subs.add("ANVIL");
                if (sender.hasPermission("bluemiscextension.workbench.grindstone")) subs.add("GRINDSTONE");
                if (sender.hasPermission("bluemiscextension.workbench.smithing")) subs.add("SMITHING");
                if (sender.hasPermission("bluemiscextension.workbench.cartography")) subs.add("CARTOGRAPHY");
                if (sender.hasPermission("bluemiscextension.workbench.loom")) subs.add("LOOM");
                if (sender.hasPermission("bluemiscextension.workbench.enderchest")) subs.add("ENDERCHEST");
                StringUtil.copyPartialMatches(args[1], subs, completions);
                Collections.sort(completions);
                return completions;
            }
            if (args[0].equalsIgnoreCase("armorhide")) {
                if (!sender.hasPermission("bluemiscextension.armorhide.other")) return Collections.emptyList();
                for (Player online : plugin.getServer().getOnlinePlayers()) {
                    subs.add(online.getName());
                }
                StringUtil.copyPartialMatches(args[1], subs, completions);
                Collections.sort(completions);
                return completions;
            }
            if (args[0].equalsIgnoreCase("unlockdata")) {
                if (!sender.hasPermission("bluemiscextension.unlockdata")) return Collections.emptyList();
                for (Player online : plugin.getServer().getOnlinePlayers()) {
                    subs.add(online.getName());
                }
                StringUtil.copyPartialMatches(args[1], subs, completions);
                Collections.sort(completions);
                return completions;
            }
        }
        if (args.length == 3) {
            List<String> subs = new ArrayList<>();
            // workbench
            final Set<String> workbenchTypes = Set.of("workbench", "anvil", "grindstone", "smithing", "cartography", "loom", "enderchest");
            if (workbenchTypes.contains(args[1].toLowerCase())) {
                if (!sender.hasPermission("bluemiscextension.workbench.other")) return Collections.emptyList();
                for (Player online : plugin.getServer().getOnlinePlayers()) {
                    subs.add(online.getName());
                }
                StringUtil.copyPartialMatches(args[2], subs, completions);
                Collections.sort(completions);
                return completions;
            }
        }
        return Collections.emptyList();
    }
}
