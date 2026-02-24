package net.mcblueice.bluemiscextension.features.BedrockGlideElytra.Listener.Packets;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.BedrockGlideElytra.BedrockGlideElytra;
import net.mcblueice.bluemiscextension.utils.FloodgateUtil;

public class WindowItemsListener extends PacketAdapter {
    private static final int PLAYER_INVENTORY_WINDOW_ID = 0;
    private static final int CHEST_SLOT = 6;

    private final BedrockGlideElytra bedrockGlideElytra;

    public WindowItemsListener(BedrockGlideElytra bedrockGlideElytra) {
        super(BlueMiscExtension.getInstance(), ListenerPriority.NORMAL, PacketType.Play.Server.WINDOW_ITEMS);
        this.bedrockGlideElytra = bedrockGlideElytra;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        if (!FloodgateUtil.isFloodgatePlayer(player)) return;

        PacketContainer originalPacket = event.getPacket();

        Integer windowId = originalPacket.getIntegers().readSafely(0);
        if (windowId == null || windowId != PLAYER_INVENTORY_WINDOW_ID) return;

        List<ItemStack> items = originalPacket.getItemListModifier().readSafely(0);
        if (items == null || items.isEmpty() || CHEST_SLOT >= items.size()) return;

        ItemStack virtualChestplate = bedrockGlideElytra.getVirtualElytraForPlayer(player);
        if (virtualChestplate == null) return;

        List<ItemStack> modifiedItems = new ArrayList<>(items);
        modifiedItems.set(CHEST_SLOT, virtualChestplate);

        PacketContainer clonedPacket = originalPacket.deepClone();
        clonedPacket.getItemListModifier().writeSafely(0, modifiedItems);
        event.setPacket(clonedPacket);
    }
}
