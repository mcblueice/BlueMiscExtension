package net.mcblueice.bluemiscextension.features.BedrockGlideElytra.Listener.Packets;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.geysermc.floodgate.api.FloodgateApi;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import net.mcblueice.bluemiscextension.features.BedrockGlideElytra.BedrockGlideElytra;

public class WindowItemsListener extends PacketListenerAbstract {
    private static final int PLAYER_INVENTORY_WINDOW_ID = 0;
    private static final int CHEST_SLOT = 6;

    private final BedrockGlideElytra bedrockGlideElytra;

    public WindowItemsListener(BedrockGlideElytra bedrockGlideElytra) {
        super(PacketListenerPriority.NORMAL);
        this.bedrockGlideElytra = bedrockGlideElytra;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.WINDOW_ITEMS) return;

        if (!(event.getPlayer() instanceof Player player)) return;
        if (!FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) return;

        WrapperPlayServerWindowItems windowItems = new WrapperPlayServerWindowItems(event);

        if (windowItems.getWindowId() != PLAYER_INVENTORY_WINDOW_ID) return;

        List<com.github.retrooper.packetevents.protocol.item.ItemStack> items = windowItems.getItems();
        if (items == null || items.isEmpty() || CHEST_SLOT >= items.size()) return;

        ItemStack virtualChestplate = bedrockGlideElytra.getVirtualElytraForPlayer(player);
        if (virtualChestplate == null) return;

        List<com.github.retrooper.packetevents.protocol.item.ItemStack> modifiedItems = new ArrayList<>(items);
        modifiedItems.set(CHEST_SLOT, SpigotConversionUtil.fromBukkitItemStack(virtualChestplate));

        windowItems.setItems(modifiedItems);
    }
}
