package net.mcblueice.bluemiscextension.features.ArmorHide.Listener.Packets;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.ArmorHide.ArmorHide;
import net.mcblueice.bluemiscextension.features.ArmorHide.ArmorHideUtil;
import net.mcblueice.bluemiscextension.utils.ConfigManager;

public class SetSlotListener extends PacketListenerAbstract {
    private final ConfigManager lang;
    private final ArmorHide armorHide;

    public SetSlotListener(BlueMiscExtension plugin, ArmorHide armorHide) {
        super(PacketListenerPriority.NORMAL);
        this.lang = plugin.getLanguageManager();
        this.armorHide = armorHide;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.SET_SLOT) return;

        if (!(event.getPlayer() instanceof Player player)) return;
        if (!armorHide.isArmorHidden(player)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(event);

        if (packet.getWindowId() != 0) return;

        int slot = packet.getSlot();
        if (slot < 5 || slot > 8) return;

        com.github.retrooper.packetevents.protocol.item.ItemStack peItem = packet.getItem();
        ItemStack originalItem = SpigotConversionUtil.toBukkitItemStack(peItem);

        if (originalItem != null && !originalItem.getType().isAir()) {
            ItemStack newItem = ArmorHideUtil.armorConvert(originalItem, this.lang);
            packet.setItem(SpigotConversionUtil.fromBukkitItemStack(newItem));
        }
    }
}
