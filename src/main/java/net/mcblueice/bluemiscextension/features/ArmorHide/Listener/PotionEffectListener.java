package net.mcblueice.bluemiscextension.features.ArmorHide.Listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.ArmorHide.ArmorHide;
import net.mcblueice.bluemiscextension.utils.TaskScheduler;

public class PotionEffectListener implements Listener {
    private final BlueMiscExtension plugin;
    private final ArmorHide armorHide;

    public PotionEffectListener(BlueMiscExtension plugin, ArmorHide armorHide) {
        this.plugin = plugin;
        this.armorHide = armorHide;
    }

    @EventHandler
    public void onPlayerPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (!armorHide.isArmorHidden(player)) return;
        TaskScheduler.runTaskLater(player, plugin, () -> armorHide.updatePlayer(player), 1L);
    }
}
