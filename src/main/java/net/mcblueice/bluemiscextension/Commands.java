package net.mcblueice.bluemiscextension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import net.mcblueice.bluemiscextension.features.VirtualWorkbench.VirtualWorkbench;
import net.mcblueice.bluemiscextension.features.FeatureManager;
import net.mcblueice.bluemiscextension.features.ArmorHide.ArmorHide;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;

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
                    if (!sender.hasPermission("bluemiscextension.reload")) {
                        sender.sendMessage(lang.get("Prefix") + lang.get("NoPermission"));
                        return true;
                    }
                    plugin.reloadConfig();
                    lang.reload();
                    featureManager.reload();
                    sender.sendMessage(lang.get("Prefix") + lang.get("ReloadSuccess"));
                    return true;
                case "DEBUG":
                    if (!sender.hasPermission("bluemiscextension.debug")) {
                        sender.sendMessage(lang.get("Prefix") + lang.get("NoPermission"));
                        return true;
                    }
                    if (sender instanceof Player) {
                        player = (Player) sender;
                        boolean stat = plugin.toggleDebugMode(player.getUniqueId());
                        sender.sendMessage(lang.get("Prefix") + lang.get(stat ? "DebugEnabled" : "DebugDisabled"));
                    } else {
                        boolean stat = plugin.toggleDebugMode(new UUID(0L, 0L));
                        sender.sendMessage(lang.get("Prefix") + lang.get(stat ? "DebugEnabled" : "DebugDisabled"));
                    }
                    return true;
                case "ARMORHIDE":
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(lang.get("Prefix") + lang.get("OnlyPlayer"));
                        return true;
                    }
                    player = (Player) sender;
                    if (!player.hasPermission("bluemiscextension.armorhide")) {
                        player.sendMessage(lang.get("Prefix") + lang.get("NoPermission"));
                        return true;
                    }

                    ArmorHide armorHide = featureManager.getFeature(ArmorHide.class);
                    if (armorHide == null) {
                        player.sendMessage(lang.get("Prefix") + lang.get("ArmorHideFeature.NotEnabled"));
                        return true;
                    }
                    boolean newState = !databaseUtil.getArmorHiddenState(player.getUniqueId());
                    databaseUtil.setArmorHiddenState(player.getUniqueId(), newState);
                    armorHide.updatePlayer(player);
                    player.sendMessage(lang.get("Prefix") + lang.get(newState ? "ArmorHideFeature.ToggleOn" : "ArmorHideFeature.ToggleOff"));
                    return true;
                case "WORKBENCH":
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(lang.get("Prefix") + lang.get("OnlyPlayer"));
                        return true;
                    }
                    player = (Player) sender;
                    if (args.length < 2) {
                        sender.sendMessage(lang.get("Prefix") + lang.get("UsageError"));
                        return true;
                    }

                    VirtualWorkbench workbench = featureManager.getFeature(VirtualWorkbench.class);
                    if (workbench == null) {
                        player.sendMessage(lang.get("Prefix") + lang.get("VirtualWorkbench.NotEnabled"));
                        return true;
                    }
                    String workbenchType = args[1].toLowerCase();
                    if (!player.hasPermission("bluemiscextension.workbench." + workbenchType)) {
                        player.sendMessage(lang.get("Prefix") + lang.get("NoPermission"));
                        return true;
                    } else {
                        workbench.open(player, workbenchType);
                        return true;
                    }
                default:
                    sender.sendMessage(lang.get("Prefix") + lang.get("UsageError"));
                    return true;
            }
        }
        sender.sendMessage(lang.get("Prefix") + lang.get("UsageError"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (sender.hasPermission("bluemiscextension.reload")) subs.add("reload");
            if (sender.hasPermission("bluemiscextension.debug")) subs.add("debug");
            if (sender.hasPermission("bluemiscextension.workbench") && featureManager.isFeatureEnabled(VirtualWorkbench.class)) subs.add("workbench");
            if (sender.hasPermission("bluemiscextension.armorhide") && featureManager.isFeatureEnabled(ArmorHide.class)) subs.add("armorhide");
            StringUtil.copyPartialMatches(args[0], subs, completions);
            Collections.sort(completions);
            return completions;
        }
        if (args.length == 2) {
            if (!args[0].equalsIgnoreCase("workbench")) return Collections.emptyList();
            List<String> subs = new ArrayList<>();
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
        return Collections.emptyList();
    }
}
