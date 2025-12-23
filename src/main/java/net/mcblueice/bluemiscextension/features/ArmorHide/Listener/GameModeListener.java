package net.mcblueice.bluemiscextension.features.ArmorHide.Listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.ArmorHide.ArmorHide;
import net.mcblueice.bluemiscextension.utils.TaskScheduler;

public class GameModeListener implements Listener {
    private final BlueMiscExtension plugin;
    private final ArmorHide armorHide;

    public GameModeListener(BlueMiscExtension plugin, ArmorHide armorHide){
        this.plugin = plugin;
        this.armorHide = armorHide;
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event){
        Player player = event.getPlayer();
        if(!armorHide.isArmorHidden(player)) return;
        TaskScheduler.runTaskLater(player, plugin, () -> armorHide.updateSelf(player), 1L);
    }
}
