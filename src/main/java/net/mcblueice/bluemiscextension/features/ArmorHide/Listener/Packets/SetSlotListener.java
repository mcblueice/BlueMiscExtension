package net.mcblueice.bluemiscextension.features.ArmorHide.Listener.Packets;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.ArmorHide.ArmorHide;
import net.mcblueice.bluemiscextension.features.ArmorHide.ArmorHideUtil;
import net.mcblueice.bluemiscextension.utils.ConfigManager;

public class SetSlotListener extends PacketAdapter {
    private final ConfigManager lang;
    private final ArmorHide armorHide;

    public SetSlotListener(BlueMiscExtension plugin, ArmorHide armorHide) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.SET_SLOT);
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.armorHide = armorHide;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket().deepClone();
        Player player = event.getPlayer();

        if (!armorHide.isArmorHidden(player)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        int windowId = packet.getIntegers().readSafely(0);
        if (windowId != 0) return;

        int slot = packet.getIntegers().readSafely(2);
        if (slot < 5 || slot > 8) return;
        ItemStack originalItem = packet.getItemModifier().readSafely(0);
        if (originalItem != null && originalItem.getType() != Material.AIR) {
            ItemStack newItem = ArmorHideUtil.armorConvert(originalItem, this.lang);
            packet.getItemModifier().writeSafely(0, newItem);
            event.setPacket(packet);
        }
    }
}
