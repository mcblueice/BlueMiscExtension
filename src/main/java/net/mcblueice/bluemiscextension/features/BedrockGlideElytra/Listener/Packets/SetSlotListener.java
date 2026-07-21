package net.mcblueice.bluemiscextension.features.BedrockGlideElytra.Listener.Packets;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.floodgate.api.FloodgateApi;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import net.mcblueice.bluemiscextension.features.BedrockGlideElytra.BedrockGlideElytra;

public class SetSlotListener extends PacketListenerAbstract {
    private static final int PLAYER_INVENTORY_WINDOW_ID = 0;
    private static final int CHEST_SLOT = 6;

    private final BedrockGlideElytra bedrockGlideElytra;

    public SetSlotListener(BedrockGlideElytra bedrockGlideElytra) {
        super(PacketListenerPriority.NORMAL);
        this.bedrockGlideElytra = bedrockGlideElytra;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.SET_SLOT) return;

        if (!(event.getPlayer() instanceof Player player)) return;
        if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) return;

        WrapperPlayServerSetSlot setSlot = new WrapperPlayServerSetSlot(event);
        if (setSlot.getWindowId() != PLAYER_INVENTORY_WINDOW_ID) return;
        if (setSlot.getSlot() != CHEST_SLOT) return;

        ItemStack virtualItem = bedrockGlideElytra.getVirtualElytraForPlayer(player);
        if (virtualItem == null) return;

        setSlot.setItem(SpigotConversionUtil.fromBukkitItemStack(virtualItem));
    }
}
