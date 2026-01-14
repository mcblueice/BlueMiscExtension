package net.mcblueice.bluemiscextension.features.BlueTools.tools;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class AreaMiningTool implements Listener {

    private final BlueMiscExtension plugin;

    public AreaMiningTool(BlueMiscExtension plugin) {
        this.plugin = plugin;
    }

    public void register() {
         plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // TODO: 範圍挖掘
    }
}
