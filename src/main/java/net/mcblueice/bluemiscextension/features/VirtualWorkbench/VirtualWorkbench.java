package net.mcblueice.bluemiscextension.features.VirtualWorkbench;

import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;

import net.kyori.adventure.text.Component;
import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluemiscextension.utils.TaskScheduler;
import net.mcblueice.bluemiscextension.features.Feature;

public class VirtualWorkbench implements Feature {
    private final BlueMiscExtension plugin;
    private final ConfigManager lang;
    private final boolean debug;

    public VirtualWorkbench(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.debug = plugin.getConfig().getBoolean("Features.VirtualWorkbench.debug", false);
    }

    @Override
    public void register() {}

    @Override
    public void unregister() {}

    public void open(Player player, String station) {
        if (player == null || station == null) return;
        TaskScheduler.runTask(player, plugin, () -> {
            Component title;
            switch (station.toUpperCase()) {
                case "WORKBENCH":
                    title = lang.get("VirtualWorkbench.Title.WorkbenchTitle").equals("none") ?
                        Component.translatable("block.minecraft.crafting_table") :
                        Component.text(lang.get("VirtualWorkbench.Title.WorkbenchTitle"));
                    MenuType.CRAFTER_3X3.builder().title(title).build(player).open();
                    if (debug) plugin.sendDebug("已為玩家開啟工作台: " + player.getName());
                    break;
                case "ANVIL":
                    title = lang.get("VirtualWorkbench.Title.AnvilTitle").equals("none") ?
                        Component.translatable("block.minecraft.anvil") :
                        Component.text(lang.get("VirtualWorkbench.Title.AnvilTitle"));
                    MenuType.ANVIL.builder().title(title).build(player).open();
                    if (debug) plugin.sendDebug("已為玩家開啟鐵砧: " + player.getName());
                    break;
                case "GRINDSTONE":
                    title = lang.get("VirtualWorkbench.Title.GrindstoneTitle").equals("none") ?
                        Component.translatable("block.minecraft.grindstone") :
                        Component.text(lang.get("VirtualWorkbench.Title.GrindstoneTitle"));
                    MenuType.GRINDSTONE.builder().title(title).build(player).open();
                    if (debug) plugin.sendDebug("已為玩家開啟砂輪: " + player.getName());
                    break;
                case "SMITHING":
                    title = lang.get("VirtualWorkbench.Title.SmithingTitle").equals("none") ?
                        Component.translatable("block.minecraft.smithing_table") :
                        Component.text(lang.get("VirtualWorkbench.Title.SmithingTitle"));
                    MenuType.SMITHING.builder().title(title).build(player).open();
                    if (debug) plugin.sendDebug("已為玩家開啟鍛造台: " + player.getName());
                    break;
                case "CARTOGRAPHY":
                    title = lang.get("VirtualWorkbench.Title.CartographyTitle").equals("none") ?
                        Component.translatable("block.minecraft.cartography_table") :
                        Component.text(lang.get("VirtualWorkbench.Title.CartographyTitle"));
                    MenuType.CARTOGRAPHY_TABLE.builder().title(title).build(player).open();
                    if (debug) plugin.sendDebug("已為玩家開啟製圖台: " + player.getName());
                    break;
                case "LOOM":
                    title = lang.get("VirtualWorkbench.Title.LoomTitle").equals("none") ?
                        Component.translatable("block.minecraft.loom") :
                        Component.text(lang.get("VirtualWorkbench.Title.LoomTitle"));
                    MenuType.LOOM.builder().title(title).build(player).open();
                    if (debug) plugin.sendDebug("已為玩家開啟紡織機: " + player.getName());
                    break;
                case "ENDERCHEST":
                    player.openInventory(player.getEnderChest());
                    if (debug) plugin.sendDebug("已為玩家開啟終界箱: " + player.getName());
                    break;
                default:
                    player.sendMessage(lang.get("VirtualWorkbench.UnknownStation", station));
                    break;
            }
        });
    }
}
