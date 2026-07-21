package net.mcblueice.bluemiscextension.features.ArmorHide;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffectType;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.ArmorHide.Listener.GameModeListener;
import net.mcblueice.bluemiscextension.features.ArmorHide.Listener.InventoryClickListener;
import net.mcblueice.bluemiscextension.features.ArmorHide.Listener.PotionEffectListener;
import net.mcblueice.bluemiscextension.features.ArmorHide.Listener.Packets.EntityEquipmentListener;
import net.mcblueice.bluemiscextension.features.ArmorHide.Listener.Packets.SetSlotListener;
import net.mcblueice.bluemiscextension.features.ArmorHide.Listener.Packets.WindowItemsListener;
import net.mcblueice.bluemiscextension.features.Feature;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;

public class ArmorHide implements Feature {
    private final BlueMiscExtension plugin;
    private final PluginManager pluginManager;
    private final DatabaseUtil databaseUtil;
    private SetSlotListener setSlotListener;
    private WindowItemsListener windowItemsListener;
    private EntityEquipmentListener entityEquipmentListener;
    private GameModeListener gameModeListener;
    private InventoryClickListener inventoryClickListener;
    private PotionEffectListener potionEffectListener;

    public ArmorHide(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.pluginManager = Bukkit.getPluginManager();
        this.databaseUtil = plugin.getDatabaseUtil();
    }

    @Override
    public void register() {
        this.setSlotListener = new SetSlotListener(plugin, this);
        this.windowItemsListener = new WindowItemsListener(plugin, this);
        this.entityEquipmentListener = new EntityEquipmentListener(plugin, this);
        this.gameModeListener = new GameModeListener(plugin, this);
        this.inventoryClickListener = new InventoryClickListener(plugin, this);
        this.potionEffectListener = new PotionEffectListener(plugin, this);

        PacketEvents.getAPI().getEventManager().registerListener(setSlotListener);
        PacketEvents.getAPI().getEventManager().registerListener(windowItemsListener);
        PacketEvents.getAPI().getEventManager().registerListener(entityEquipmentListener);

        pluginManager.registerEvents(gameModeListener, plugin);
        pluginManager.registerEvents(inventoryClickListener, plugin);
        pluginManager.registerEvents(potionEffectListener, plugin);
        plugin.getCommandManager().register(new ArmorHideCommand(plugin, this), "bluemiscextension.armorhide", new String[]{"armorhide"});
    }

    @Override
    public void unregister() { 
        PacketEvents.getAPI().getEventManager().unregisterListener(setSlotListener);
        PacketEvents.getAPI().getEventManager().unregisterListener(windowItemsListener);
        PacketEvents.getAPI().getEventManager().unregisterListener(entityEquipmentListener);

        HandlerList.unregisterAll(gameModeListener);
        HandlerList.unregisterAll(inventoryClickListener);
        HandlerList.unregisterAll(potionEffectListener);
        plugin.getCommandManager().unregister("armorhide");
    }

    public boolean isArmorHidden(Player player) {
        if (databaseUtil.getPlayerData(player.getUniqueId()).hiddenArmor()) return true;
        return player.hasPotionEffect(PotionEffectType.INVISIBILITY);
    }

    public void updatePlayer(Player player) {
        updateSelf(player);
        updateToOthers(player);
    }

    public void updateSelf(Player player) {
        PlayerInventory inv = player.getInventory();
        // 5: 頭盔, 6: 胸甲, 7: 護腿, 8: 靴子
        for (int i = 5; i <= 8; i++) {
            ItemStack item = switch (i) {
                case 5 -> inv.getHelmet();
                case 6 -> inv.getChestplate();
                case 7 -> inv.getLeggings();
                case 8 -> inv.getBoots();
                default -> null;
            };

            com.github.retrooper.packetevents.protocol.item.ItemStack peItem = SpigotConversionUtil.fromBukkitItemStack(item);

            // windowId=0, stateId=0, slot=i
            WrapperPlayServerSetSlot packet = new WrapperPlayServerSetSlot(0, 0, i, peItem);
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        }
    }

    public void updateToOthers(Player player) {
        PlayerInventory inv = player.getInventory();
        List<Equipment> equipmentList = new ArrayList<>();

        equipmentList.add(new Equipment(EquipmentSlot.HELMET, SpigotConversionUtil.fromBukkitItemStack(inv.getHelmet())));
        equipmentList.add(new Equipment(EquipmentSlot.CHEST_PLATE, SpigotConversionUtil.fromBukkitItemStack(inv.getChestplate())));
        equipmentList.add(new Equipment(EquipmentSlot.LEGGINGS, SpigotConversionUtil.fromBukkitItemStack(inv.getLeggings())));
        equipmentList.add(new Equipment(EquipmentSlot.BOOTS, SpigotConversionUtil.fromBukkitItemStack(inv.getBoots())));

        WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(player.getEntityId(), equipmentList);

        for (Player p : player.getWorld().getPlayers()) {
            if (p.getEntityId() != player.getEntityId()) PacketEvents.getAPI().getPlayerManager().sendPacket(p, packet);
        }
    }
}
