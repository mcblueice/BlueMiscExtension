package net.mcblueice.bluemiscextension.features.BlueTools;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.Feature;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.ArmorSystem;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.AreaMiningTool;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.AutoSmeltTool;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BlueTools implements Feature {
    private final BlueMiscExtension plugin;
    private final PluginManager pluginManager;
    
    private AutoSmeltTool autoSmeltTool;
    private AreaMiningTool areaMiningTool;
    private ArmorSystem armorSystem;

    public BlueTools(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.pluginManager = Bukkit.getPluginManager();
    }

    @Override
    public void register() {
        this.autoSmeltTool = new AutoSmeltTool(plugin);
        this.areaMiningTool = new AreaMiningTool(plugin);
        this.armorSystem = new ArmorSystem(plugin);

        pluginManager.registerEvents(autoSmeltTool, plugin);
        pluginManager.registerEvents(areaMiningTool, plugin);
        pluginManager.registerEvents(armorSystem, plugin);
    }

    @Override
    public void unregister() {
        for (Player getPlayer : Bukkit.getOnlinePlayers()) {
            armorSystem.getBossBarManager().removeBossBar(getPlayer);
        }

        HandlerList.unregisterAll(autoSmeltTool);
        HandlerList.unregisterAll(areaMiningTool);
        HandlerList.unregisterAll(armorSystem);
    }

    public ArmorSystem getArmorSystem() { return armorSystem; }
}