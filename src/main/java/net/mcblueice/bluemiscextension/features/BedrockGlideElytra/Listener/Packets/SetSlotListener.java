package net.mcblueice.bluemiscextension.features.BedrockGlideElytra.Listener.Packets;

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

public class SetSlotListener extends PacketAdapter {
    private static final int PLAYER_INVENTORY_WINDOW_ID = 0;
    private static final int CHEST_SLOT = 6;

    private final BedrockGlideElytra bedrockGlideElytra;

    public SetSlotListener(BedrockGlideElytra bedrockGlideElytra) {
        super(BlueMiscExtension.getInstance(), ListenerPriority.NORMAL, PacketType.Play.Server.SET_SLOT);
        this.bedrockGlideElytra = bedrockGlideElytra;
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        Player player = event.getPlayer();
        if (!FloodgateUtil.isFloodgatePlayer(player)) return;

        PacketContainer originalPacket = event.getPacket();

        Integer windowId = originalPacket.getIntegers().readSafely(0);
        if (windowId == null || windowId != PLAYER_INVENTORY_WINDOW_ID) return;

        Integer slot = originalPacket.getIntegers().readSafely(2);
        if (slot == null || slot != CHEST_SLOT) return;

        ItemStack virtualItem = bedrockGlideElytra.getVirtualElytraForPlayer(player);
        if (virtualItem == null) return;

        PacketContainer clonedPacket = originalPacket.deepClone();
        clonedPacket.getItemModifier().writeSafely(0, virtualItem);
        event.setPacket(clonedPacket);
    }
}
