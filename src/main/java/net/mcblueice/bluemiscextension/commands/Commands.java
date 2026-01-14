package net.mcblueice.bluemiscextension.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.StringUtil;

import net.mcblueice.bluemiscextension.features.VirtualWorkbench.VirtualWorkbench;
import net.mcblueice.bluemiscextension.features.BlueTools.BlueTools;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.ArmorSystem;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.utils.ArmorEnergyManager;
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
                case "ARMOR":
                    if (!sender.hasPermission("bluemiscextension.armor")) return NoPermission(sender);
                    BlueTools blueTools = featureManager.getFeature(BlueTools.class);
                    if (blueTools == null) return NotEnabled(sender, "BlueToolsFeature");
                    ArmorSystem armorSystem = blueTools.getArmorSystem();
                    if (armorSystem == null) {
                        sender.sendMessage(lang.get("Prefix.Default") + "ArmorSystem is not active.");
                        return true;
                    }

                    if (args.length < 2) return UsageError(sender);

                    switch (args[1].toUpperCase()) {
                        case "SETID":
                            if (!sender.hasPermission("bluemiscextension.armor.setid")) return NoPermission(sender);
                            if (!(sender instanceof Player)) return OnlyPlayer(sender);
                            if (args.length < 3) return UsageError(sender);

                            player = (Player) sender;
                            ItemStack item = player.getInventory().getItemInMainHand();
                            if (item == null || item.getType().isAir()) {
                                player.sendMessage(lang.get("Prefix.Default") + "Please hold an item in your main hand.");
                                return true;
                            }
                            ItemMeta meta = item.getItemMeta();
                            if (meta != null) {
                                meta.getPersistentDataContainer().set(new NamespacedKey("bluetools", "armor_set_id"), PersistentDataType.STRING, args[2]);
                                item.setItemMeta(meta);
                                player.sendMessage(lang.get("Prefix.Default") + "Set armor ID to: " + args[2]);
                            }
                            return true;

                        case "ADDENERGY":
                            if (!sender.hasPermission("bluemiscextension.armor.addenergy")) return NoPermission(sender);
                            if (args.length < 3) return UsageError(sender);
                            
                            int amount;
                            try {
                                amount = Integer.parseInt(args[2]);
                            } catch (NumberFormatException e) {
                                sender.sendMessage(lang.get("Prefix.Default") + "Invalid amount: " + args[2]);
                                return true;
                            }
                            
                            Player targetEnergy = null;
                            if (args.length > 3) {
                                targetEnergy = plugin.getServer().getPlayerExact(args[3]);
                                if (targetEnergy == null) return PlayerNotFound(sender, args[3]);
                            } else {
                                if (sender instanceof Player) {
                                    targetEnergy = (Player) sender;
                                } else {
                                    sender.sendMessage(lang.get("Prefix.Default") + "Console usage: /bme armor addenergy <amount> <player>");
                                    return true;
                                }
                            }
                            ArmorEnergyManager.addEnergy(targetEnergy, new NamespacedKey("bluetools", "energy_point"), amount);
                            sender.sendMessage(lang.get("Prefix.Default") + "Added " + amount + " energy to " + targetEnergy.getName());
                            return true;

                        case "TOGGLE":
                            if (!sender.hasPermission("bluemiscextension.armor.toggle")) return NoPermission(sender);
                            if (!(sender instanceof Player)) return OnlyPlayer(sender);
                            if (args.length < 3) return UsageError(sender);
                            
                            player = (Player) sender;
                            String effectId = args[2].toLowerCase();
                            
                            if (!armorSystem.effectRegistry.containsKey(effectId)) {
                                player.sendMessage(lang.get("Prefix.Default") + "Invalid effect ID: " + effectId);
                                return true;
                            }

                            boolean currentStatus = armorSystem.getEnabledEffects(player).contains(effectId);
                            armorSystem.toggleEffect(player, effectId, !currentStatus);
                            player.sendMessage(lang.get("Prefix.Default") + "Effect " + effectId + " is now " + (!currentStatus ? "§aEnabled" : "§cDisabled"));
                            return true;

                        case "SETTINGS":
                            if (!sender.hasPermission("bluemiscextension.armor.settings")) return NoPermission(sender);
                            if (!(sender instanceof Player)) return OnlyPlayer(sender);
                            player = (Player) sender;
                            
                            armorSystem.getSettingsGUI().open(player);
                            return true;

                        default:
                            return UsageError(sender);
                    }
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
            if (sender.hasPermission("bluemiscextension.workbench") && featureManager.isFeatureEnabled(VirtualWorkbench.class)) subs.add("workbench");
            if (sender.hasPermission("bluemiscextension.armorhide") && featureManager.isFeatureEnabled(ArmorHide.class)) subs.add("armorhide");
            StringUtil.copyPartialMatches(args[0], subs, completions);
            Collections.sort(completions);
            return completions;
        }
        if (args.length == 2) {
            List<String> subs = new ArrayList<>();
            if (args[0].equalsIgnoreCase("armor")) {
                if (sender.hasPermission("bluemiscextension.armor.setid")) subs.add("setid");
                if (sender.hasPermission("bluemiscextension.armor.addenergy")) subs.add("addenergy");
                if (sender.hasPermission("bluemiscextension.armor.toggle")) subs.add("toggle");
                if (sender.hasPermission("bluemiscextension.armor.settings")) subs.add("settings");
                StringUtil.copyPartialMatches(args[1], subs, completions);
                Collections.sort(completions);
                return completions;
            }
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
        }
        if (args.length == 3) {
            List<String> subs = new ArrayList<>();
            // armor
            if (args[0].equalsIgnoreCase("armor")) {
                if (args[1].equalsIgnoreCase("setid")) {
                    if (sender.hasPermission("bluemiscextension.armor.setid")) {
                        subs.add("power_armor_mk1");
                        subs.add("power_armor_mk2");
                    }
                }
                if (args[1].equalsIgnoreCase("toggle")) {
                    if (sender.hasPermission("bluemiscextension.armor.toggle")) {
                        try {
                           ArmorSystem armorSystem = featureManager.getFeature(BlueTools.class).getArmorSystem();
                           if (armorSystem != null) subs.addAll(armorSystem.effectRegistry.keySet());
                        } catch (Exception e) {}
                    }
                }
                // addenergy 第3個參數是數量，沒有補全
                StringUtil.copyPartialMatches(args[2], subs, completions);
                Collections.sort(completions);
                return completions;
            }
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
        if (args.length == 4) {
            List<String> subs = new ArrayList<>();
            if (args[0].equalsIgnoreCase("armor") && args[1].equalsIgnoreCase("addenergy")) {
                 if (sender.hasPermission("bluemiscextension.armor.addenergy")) {
                     for (Player online : plugin.getServer().getOnlinePlayers()) {
                         subs.add(online.getName());
                     }
                 }
                StringUtil.copyPartialMatches(args[3], subs, completions);
                Collections.sort(completions);
                return completions;
            }
        }
        return Collections.emptyList();
    }
}
