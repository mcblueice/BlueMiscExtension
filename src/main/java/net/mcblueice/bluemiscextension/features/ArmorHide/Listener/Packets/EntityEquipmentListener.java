package net.mcblueice.bluemiscextension.features.ArmorHide.Listener.Packets;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.ArmorHide.ArmorHide;
import net.mcblueice.bluemiscextension.listeners.PlayerDataListener;

public class EntityEquipmentListener extends PacketAdapter {
	private final ArmorHide armorHide;

	public EntityEquipmentListener(BlueMiscExtension plugin, ArmorHide armorHide) {
		super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_EQUIPMENT);
		this.armorHide = armorHide;
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		PacketContainer originalPacket = event.getPacket();

		Integer entityId = originalPacket.getIntegers().readSafely(0);
		if (entityId == null) return;

		Entity trackedEntity = PlayerDataListener.playerIDCache.get(entityId);
		if (!(trackedEntity instanceof Player targetPlayer)) return;
		if (!armorHide.isArmorHidden(targetPlayer)) return;

		List<Pair<EnumWrappers.ItemSlot, ItemStack>> pairs = originalPacket.getSlotStackPairLists().readSafely(0);
		if (pairs == null || pairs.isEmpty()) return;

		List<Pair<EnumWrappers.ItemSlot, ItemStack>> modifiedPairs = new ArrayList<>(pairs.size());
		boolean modified = false;

		for (Pair<EnumWrappers.ItemSlot, ItemStack> pair : pairs) {
			EnumWrappers.ItemSlot slot = pair.getFirst();
			if (slot == EnumWrappers.ItemSlot.HEAD
					|| slot == EnumWrappers.ItemSlot.CHEST
					|| slot == EnumWrappers.ItemSlot.LEGS
					|| slot == EnumWrappers.ItemSlot.FEET) {
				modifiedPairs.add(new Pair<>(slot, new ItemStack(Material.AIR)));
				modified = true;
			} else {
				modifiedPairs.add(pair);
			}
		}

		if (!modified) return;

		PacketContainer clonedPacket = originalPacket.deepClone();
		clonedPacket.getSlotStackPairLists().writeSafely(0, modifiedPairs);
		event.setPacket(clonedPacket);
	}
}
