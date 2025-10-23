package net.mcblueice.bluemiscextension.features;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataValue;

import net.mcblueice.bluemiscextension.BlueMiscExtension;

public class AbsorptionScale implements Listener {

	private final BlueMiscExtension plugin;
	private final Map<UUID, Float> peakAbsorption = new ConcurrentHashMap<>();
	private final Map<UUID, Float> lastDisplayed = new ConcurrentHashMap<>();

	public AbsorptionScale(BlueMiscExtension plugin) {
		this.plugin = plugin;
	}

	public void register() {
		ProtocolManager manager = ProtocolLibrary.getProtocolManager();

		manager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_METADATA) {
			float maxAbsorption = (float) plugin.getConfig().getDouble("Features.AbsorptionScale.max_amount", 20D);

			@Override
			public void onPacketSending(PacketEvent event) {
				PacketContainer packet = event.getPacket().deepClone();
				Player viewer = event.getPlayer();
				Integer entityId = packet.getIntegers().readSafely(0);
				if (entityId == null || entityId != viewer.getEntityId()) return;
				Player player = viewer;
				UUID uuid = player.getUniqueId();

				List<WrappedDataValue> dataList = packet.getDataValueCollectionModifier().readSafely(0);
				if (dataList == null || dataList.isEmpty()) return;
				WrappedDataValue packetData = null;
				for (WrappedDataValue data : dataList) {
					// index 15 在 1.20, 1.21 是吸收量 其他我不確定
					if (data.getIndex() == 15) {
						packetData = data;
						break;
					}
				}
				if (packetData == null) return;
				float absorptionValue = ((Number) packetData.getValue()).floatValue();

				if (absorptionValue <= 0F) {
					AbsorptionScale.this.plugin.sendDebug("§e已重置 §b" + player.getName() + " §e的吸收血量縮放資料");
					peakAbsorption.remove(uuid);
					lastDisplayed.remove(uuid);
					return;
				}

				float peak = peakAbsorption.getOrDefault(uuid, absorptionValue);
				if (absorptionValue > peak) {
					peak = absorptionValue;
					AbsorptionScale.this.plugin.sendDebug("§e更新 §b" + player.getName() + " §e最高吸收血量至 §6" + peak);
				}
				peakAbsorption.putIfAbsent(uuid, peak);

				float displayValue = absorptionValue;
				if (peak > maxAbsorption) {
					float ratio = maxAbsorption / peak;
					displayValue = absorptionValue * ratio;
				}

				Float prevDisplayed = lastDisplayed.get(uuid);
				if (prevDisplayed != null && Math.abs(prevDisplayed - displayValue) < 1.0f) {
					event.setCancelled(true);
					return;
				}
				lastDisplayed.put(uuid, displayValue);

				packetData.setValue(displayValue);
				packet.getDataValueCollectionModifier().writeSafely(0, dataList);
				event.setPacket(packet);
				AbsorptionScale.this.plugin.sendDebug("§e更新 §b" + player.getName() + " §e吸收血量至 §6" + displayValue + "§7(§6" + absorptionValue + "/" + peak + "§7)");
			}
        });
	}

    public void unregister() { ProtocolLibrary.getProtocolManager().removePacketListeners(plugin); }
}
