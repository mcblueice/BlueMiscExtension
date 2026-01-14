package net.mcblueice.bluemiscextension.features.BlueTools.tools;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class AutoSmeltTool implements Listener {

    private final BlueMiscExtension plugin;

    public AutoSmeltTool(BlueMiscExtension plugin) {
        this.plugin = plugin;
    }

    public void register() {
         plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // TODO: 自動熔煉
    }
}
