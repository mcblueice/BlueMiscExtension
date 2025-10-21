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

import net.mcblueice.bluemiscextension.features.VirtualWorkbench;
import net.mcblueice.bluemiscextension.utils.ConfigManager;

public class Commands implements CommandExecutor, TabCompleter {
    private final BlueMiscExtension plugin;
    private final ConfigManager lang;

    public Commands(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            Player player;
            switch (args[0].toUpperCase()) {
                case "RELOAD":
                    if (!sender.hasPermission("bluemiscextension.reload")) {
                        sender.sendMessage(lang.get("Prefix") + "§c你沒有權限使用此指令!");
                        return true;
                    }
                    plugin.reloadConfig();
                    plugin.getLanguageManager().reload();
                    plugin.refreshFeatures();
                    sender.sendMessage(lang.get("Prefix") + "§aConfig已重新加載");
                    return true;
                case "DEBUG":
                    if (!sender.hasPermission("bluemiscextension.debug")) {
                        sender.sendMessage(lang.get("Prefix") + "§c你沒有權限使用此指令!");
                        return true;
                    }
                    if (sender instanceof Player) {
                        player = (Player) sender;
                        boolean stat = plugin.toggleDebugMode(player.getUniqueId());
                        sender.sendMessage(lang.get("Prefix") + "§6Debug模式" + (stat ? "§a已開啟" : "§c已關閉"));
                    } else {
                        boolean stat = plugin.toggleDebugMode(new UUID(0L, 0L));
                        sender.sendMessage(lang.get("Prefix") + "§6Debug模式" + (stat ? "§a已開啟" : "§c已關閉"));
                    }
                    return true;
                case "WORKBENCH":
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(lang.get("Prefix") + "§c此指令僅限玩家使用!");
                        return true;
                    }
                    player = (Player) sender;
                    if (args.length < 2) {
                        sender.sendMessage(lang.get("Prefix") + "§c用法錯誤!");
                        return true;
                    }
                    switch (args[1].toUpperCase()) {
                        case "WORKBENCH":
                            if (!player.hasPermission("bluemiscextension.workbench.workbench")) {
                                player.sendMessage(lang.get("Prefix") + "§c你沒有權限使用此指令!");
                                return true;
                            }
                            new VirtualWorkbench(plugin).open(player, "WORKBENCH");
                            return true;
                        case "ANVIL":
                            if (!player.hasPermission("bluemiscextension.workbench.anvil")) {
                                player.sendMessage(lang.get("Prefix") + "§c你沒有權限使用此指令!");
                                return true;
                            }
                            new VirtualWorkbench(plugin).open(player, "ANVIL");
                            return true;
                        case "GRINDSTONE":
                            if (!player.hasPermission("bluemiscextension.workbench.grindstone")) {
                                player.sendMessage(lang.get("Prefix") + "§c你沒有權限使用此指令!");
                                return true;
                            }
                            new VirtualWorkbench(plugin).open(player, "GRINDSTONE");
                            return true;
                        case "SMITHING":
                            if (!player.hasPermission("bluemiscextension.workbench.smithing")) {
                                player.sendMessage(lang.get("Prefix") + "§c你沒有權限使用此指令!");
                                return true;
                            }
                            new VirtualWorkbench(plugin).open(player, "SMITHING");
                            return true;
                        case "CARTOGRAPHY":
                            if (!player.hasPermission("bluemiscextension.workbench.cartography")) {
                                player.sendMessage(lang.get("Prefix") + "§c你沒有權限使用此指令!");
                                return true;
                            }
                            new VirtualWorkbench(plugin).open(player, "CARTOGRAPHY");
                            return true;
                        case "LOOM":
                            if (!player.hasPermission("bluemiscextension.workbench.loom")) {
                                player.sendMessage(lang.get("Prefix") + "§c你沒有權限使用此指令!");
                                return true;
                            }
                            new VirtualWorkbench(plugin).open(player, "LOOM");
                            return true;
                        case "ENDERCHEST":
                            if (!player.hasPermission("bluemiscextension.workbench.enderchest")) {
                                player.sendMessage(lang.get("Prefix") + "§c你沒有權限使用此指令!");
                                return true;
                            }
                            new VirtualWorkbench(plugin).open(player, "ENDERCHEST");
                            return true;
                        default:
                            player.sendMessage(lang.get("Prefix") + "§c用法錯誤!");
                            return true;
                    }
                default:
                    sender.sendMessage(lang.get("Prefix") + "§c用法錯誤!");
                    return true;
            }
        }
        sender.sendMessage(lang.get("Prefix") + "§c用法錯誤!");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (sender.hasPermission("bluemiscextension.reload")) subs.add("reload");
            if (sender.hasPermission("bluemiscextension.debug")) subs.add("debug");
            if (sender.hasPermission("bluemiscextension.workbench")) subs.add("workbench");
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
