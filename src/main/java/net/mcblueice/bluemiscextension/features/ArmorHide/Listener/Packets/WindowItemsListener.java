package net.mcblueice.bluemiscextension.features.ArmorHide.Listener.Packets;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.ArmorHide.ArmorHide;
import net.mcblueice.bluemiscextension.features.ArmorHide.ArmorHideUtil;
import net.mcblueice.bluemiscextension.utils.ConfigManager;

public class WindowItemsListener extends PacketListenerAbstract {
    private static final int PLAYER_INVENTORY_WINDOW_ID = 0;
    private static final int ARMOR_SLOT_START = 5;
    private static final int ARMOR_SLOT_END = 8;

    private final ConfigManager lang;
    private final ArmorHide armorHide;

    public WindowItemsListener(BlueMiscExtension plugin, ArmorHide armorHide) {
        super(PacketListenerPriority.NORMAL);
        this.lang = plugin.getLanguageManager();
        this.armorHide = armorHide;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.WINDOW_ITEMS) return;

        if (!(event.getPlayer() instanceof Player player)) return;
        if (!armorHide.isArmorHidden(player)) return;
        if (player.getGameMode() == GameMode.CREATIVE) return;

        WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(event);

        if (packet.getWindowId() != PLAYER_INVENTORY_WINDOW_ID) return;

        List<com.github.retrooper.packetevents.protocol.item.ItemStack> items = packet.getItems();
        if (items == null || items.isEmpty()) return;

        List<com.github.retrooper.packetevents.protocol.item.ItemStack> modifiedItems = new ArrayList<>(items);
        for (int slot = ARMOR_SLOT_START; slot <= ARMOR_SLOT_END && slot < modifiedItems.size(); slot++) {
            com.github.retrooper.packetevents.protocol.item.ItemStack peArmor = modifiedItems.get(slot);
            ItemStack bukkitArmor = SpigotConversionUtil.toBukkitItemStack(peArmor);
            if (bukkitArmor == null || bukkitArmor.getType().isAir()) continue;

            ItemStack placeholder = ArmorHideUtil.armorConvert(bukkitArmor, lang);
            modifiedItems.set(slot, SpigotConversionUtil.fromBukkitItemStack(placeholder));
        }

        packet.setItems(modifiedItems);
    }
}
