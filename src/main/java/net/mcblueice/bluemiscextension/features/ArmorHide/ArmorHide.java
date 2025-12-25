package net.mcblueice.bluemiscextension.features.ArmorHide;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import org.bukkit.potion.PotionEffectType;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.ArmorHide.Listener.Packets.EntityEquipmentListener;
import net.mcblueice.bluemiscextension.features.ArmorHide.Listener.Packets.SetSlotListener;
import net.mcblueice.bluemiscextension.features.ArmorHide.Listener.Packets.WindowItemsListener;
import net.mcblueice.bluemiscextension.features.ArmorHide.Listener.GameModeListener;
import net.mcblueice.bluemiscextension.features.ArmorHide.Listener.InventoryClickListener;
import net.mcblueice.bluemiscextension.features.ArmorHide.Listener.PotionEffectListener;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;
import net.mcblueice.bluemiscextension.features.Feature;


public class ArmorHide implements Feature {
	private final BlueMiscExtension plugin;
    private final ProtocolManager protocolManager;
    private final DatabaseUtil databaseUtil;

    public ArmorHide(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.databaseUtil = plugin.getDatabaseUtil();
    }

	@Override
	public void register() {
		ProtocolManager manager = ProtocolLibrary.getProtocolManager();

		manager.addPacketListener(new SetSlotListener(plugin, this));
        manager.addPacketListener(new WindowItemsListener(plugin, this));
        manager.addPacketListener(new EntityEquipmentListener(plugin, this));
        
        Bukkit.getPluginManager().registerEvents(new GameModeListener(plugin, this), plugin);
        Bukkit.getPluginManager().registerEvents(new InventoryClickListener(plugin, this), plugin);
        Bukkit.getPluginManager().registerEvents(new PotionEffectListener(plugin, this), plugin);
	}

    @Override
    public void unregister() { ProtocolLibrary.getProtocolManager().removePacketListeners(plugin); }

    public boolean isArmorHidden(Player player) {
        if (databaseUtil.getArmorHiddenState(player.getUniqueId())) return true;
        if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) return true;
        return false;
    }

    public void updatePlayer(Player player) {
        updateSelf(player);
        updateToOthers(player);
    }

    public void updateSelf(Player player) {
        PlayerInventory inv = player.getInventory();
        // 5: 頭盔, 6: 胸甲, 7: 護腿, 8: 靴子
        for (int i = 5; i <= 8; i++) {
            PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.SET_SLOT);
            packet.getIntegers().write(0, 0);
            packet.getIntegers().write(2, i);

            ItemStack item = null;
            switch (i) {
                case 5: item = inv.getHelmet(); break;
                case 6: item = inv.getChestplate(); break;
                case 7: item = inv.getLeggings(); break;
                case 8: item = inv.getBoots(); break;
            }
            
            packet.getItemModifier().write(0, item);
            
            try {
                protocolManager.sendServerPacket(player, packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updateToOthers(Player player) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
        packet.getIntegers().write(0, player.getEntityId());

        List<Pair<EnumWrappers.ItemSlot, ItemStack>> pairs = new ArrayList<>();
        PlayerInventory inv = player.getInventory();

        pairs.add(new Pair<>(EnumWrappers.ItemSlot.HEAD, inv.getHelmet()));
        pairs.add(new Pair<>(EnumWrappers.ItemSlot.CHEST, inv.getChestplate()));
        pairs.add(new Pair<>(EnumWrappers.ItemSlot.LEGS, inv.getLeggings()));
        pairs.add(new Pair<>(EnumWrappers.ItemSlot.FEET, inv.getBoots()));

        packet.getSlotStackPairLists().write(0, pairs);

        try {
            for (Player p : player.getWorld().getPlayers()) {
                if (p.getEntityId() != player.getEntityId()) {
                    protocolManager.sendServerPacket(p, packet);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}