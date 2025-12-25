package net.mcblueice.bluemiscextension.features.VirtualWorkbench;

import org.bukkit.entity.Player;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluemiscextension.utils.TaskScheduler;
import net.mcblueice.bluemiscextension.features.Feature;

public class VirtualWorkbench implements Feature {
    private final BlueMiscExtension plugin;
    private final ConfigManager lang;

    public VirtualWorkbench(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }

    @Override
    public void register() {}

    @Override
    public void unregister() {}

    public void open(Player player, String station) {
        if (player == null || station == null) return;
        TaskScheduler.runTask(player, plugin, () -> {
            switch (station.toUpperCase()) {
                case "WORKBENCH":
                    player.openWorkbench(player.getLocation(), true);
                    plugin.sendDebug("已為玩家開啟工作台: " + player.getName());
                    break;
                case "ANVIL":
                    player.openAnvil(player.getLocation(), true);
                    plugin.sendDebug("已為玩家開啟鐵砧: " + player.getName());
                    break;
                case "GRINDSTONE":
                    player.openGrindstone(player.getLocation(), true);
                    plugin.sendDebug("已為玩家開啟砂輪: " + player.getName());
                    break;
                case "SMITHING":
                    player.openSmithingTable(player.getLocation(), true);
                    plugin.sendDebug("已為玩家開啟鍛造台: " + player.getName());
                    break;
                case "CARTOGRAPHY":
                    player.openCartographyTable(player.getLocation(), true);
                    plugin.sendDebug("已為玩家開啟製圖台: " + player.getName());
                    break;
                case "LOOM":
                    player.openLoom(player.getLocation(), true);
                    plugin.sendDebug("已為玩家開啟紡織機: " + player.getName());
                    break;
                case "ENDERCHEST":
                    player.openInventory(player.getEnderChest());
                    plugin.sendDebug("已為玩家開啟終界箱: " + player.getName());
                    break;
                default:
                    player.sendMessage(lang.get("VirtualWorkbench.UnknownStation", station));
                    break;
            }
        });
    }
}
