package net.mcblueice.bluemiscextension.features.ArmorHide.Listener.Packets;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.ArmorHide.ArmorHide;
import net.mcblueice.bluemiscextension.listeners.PlayerDataListener;

public class EntityEquipmentListener extends PacketListenerAbstract {
    private final ArmorHide armorHide;

    public EntityEquipmentListener(BlueMiscExtension plugin, ArmorHide armorHide) {
        super(PacketListenerPriority.NORMAL);
        this.armorHide = armorHide;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_EQUIPMENT) return;

        WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment(event);

        Entity trackedEntity = PlayerDataListener.playerIDCache.get(packet.getEntityId());
        if (!(trackedEntity instanceof Player targetPlayer)) return;
        if (!armorHide.isArmorHidden(targetPlayer)) return;

        List<Equipment> equipmentList = packet.getEquipment();
        if (equipmentList == null || equipmentList.isEmpty()) return;

        List<Equipment> modifiedList = new ArrayList<>(equipmentList.size());
        boolean modified = false;

        for (Equipment eq : equipmentList) {
            EquipmentSlot slot = eq.getSlot();
            if (slot == EquipmentSlot.HELMET 
                    || slot == EquipmentSlot.CHEST_PLATE 
                    || slot == EquipmentSlot.LEGGINGS 
                    || slot == EquipmentSlot.BOOTS) {
                // 將四大盔甲欄位替換為空物品
                modifiedList.add(new Equipment(slot, ItemStack.EMPTY));
                modified = true;
            } else {
                modifiedList.add(eq);
            }
        }

        if (modified) {
            packet.setEquipment(modifiedList);
        }
    }
}