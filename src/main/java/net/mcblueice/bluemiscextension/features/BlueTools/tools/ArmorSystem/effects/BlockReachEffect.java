package net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.effects;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

public class BlockReachEffect implements ArmorEffect {
    private static final String EffectKey = "block_reach_increase";
    private static final Attribute attribute = Attribute.BLOCK_INTERACTION_RANGE;
    private static final NamespacedKey attributeKey = new NamespacedKey("bluetools", EffectKey);
    private static final Operation attributeOperation = Operation.ADD_NUMBER;
    private static final int baseCost = 5;

    @Override
    public String getId() {
        return EffectKey;
    }

    @Override
    public int calculateCost(Player player, float value) {
        return baseCost;
    }

    @Override
    public void apply(Player player, float value) {
        AttributeInstance attributes = player.getAttribute(attribute);
        if (attributes == null) return;
        if (attributes.getModifier(attributeKey) != null) return;
        attributes.addModifier(new AttributeModifier(attributeKey, value, attributeOperation, EquipmentSlotGroup.ANY));
    }

    @Override
    public void remove(Player player) {
        AttributeInstance attributes = player.getAttribute(attribute);
        if (attributes == null) return;
        attributes.removeModifier(attributeKey);
    }
}