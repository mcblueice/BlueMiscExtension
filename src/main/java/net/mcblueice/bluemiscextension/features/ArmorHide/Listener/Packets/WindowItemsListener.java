package net.mcblueice.bluemiscextension.features.ArmorHide.Listener.Packets;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.GameMode;
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

public class WindowItemsListener extends PacketAdapter {
	private static final int PLAYER_INVENTORY_WINDOW_ID = 0;
	private static final int ARMOR_SLOT_START = 5;
	private static final int ARMOR_SLOT_END = 8;

    private final ConfigManager lang;
	private final ArmorHide armorHide;

	public WindowItemsListener(BlueMiscExtension plugin, ArmorHide armorHide) {
		super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.WINDOW_ITEMS);
		this.lang = plugin.getLanguageManager();
		this.armorHide = armorHide;
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		Player player = event.getPlayer();

		if (!armorHide.isArmorHidden(player)) return;
		if (player.getGameMode() == GameMode.CREATIVE) return;

		PacketContainer originalPacket = event.getPacket();
		Integer windowId = originalPacket.getIntegers().readSafely(0);
		if (windowId == null || windowId != PLAYER_INVENTORY_WINDOW_ID) return;

		List<ItemStack> items = originalPacket.getItemListModifier().readSafely(0);
		if (items == null || items.isEmpty()) return;

		List<ItemStack> modifiedItems = new ArrayList<>(items);
		for (int slot = ARMOR_SLOT_START; slot <= ARMOR_SLOT_END && slot < modifiedItems.size(); slot++) {
			ItemStack armor = modifiedItems.get(slot);
			if (armor == null) continue;
			ItemStack placeholder = ArmorHideUtil.armorConvert(armor, lang);
			modifiedItems.set(slot, placeholder);
		}

		PacketContainer clonedPacket = originalPacket.deepClone();
		clonedPacket.getItemListModifier().writeSafely(0, modifiedItems);
		event.setPacket(clonedPacket);
	}
}
