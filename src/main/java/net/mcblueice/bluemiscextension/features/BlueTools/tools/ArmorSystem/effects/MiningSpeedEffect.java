package net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.effects;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

public class MiningSpeedEffect implements ArmorEffect {
    private static final String EffectKey = "mining_speed";
    private static final Attribute attribute = Attribute.BLOCK_BREAK_SPEED;
    private static final NamespacedKey attributeKey = new NamespacedKey("bluetools", EffectKey);
    private static final Operation attributeOperation = Operation.ADD_NUMBER;
    private static final float baseCost = 20;

    @Override
    public String getId() {
        return EffectKey;
    }

    @Override
    public int calculateCost(Player player, float value) {
        int cost = Math.round(baseCost * value / 0.2f);
        return cost;
    }

    @Override
    public void apply(Player player, float value) {
        AttributeInstance attributes = player.getAttribute(attribute);
        if (attributes == null) return;

        if (value <= 0) value = 0.2f;
        AttributeModifier existing = attributes.getModifier(attributeKey);
        if (existing != null) {
            if (existing.getAmount() == value) return;
            attributes.removeModifier(existing);
        }
        attributes.addModifier(new AttributeModifier(attributeKey, value, attributeOperation, EquipmentSlotGroup.ANY));
    }

    @Override
    public void remove(Player player) {
        AttributeInstance attributes = player.getAttribute(attribute);
        if (attributes == null) return;

        if (attributes.getModifier(attributeKey) != null) attributes.removeModifier(attributeKey);
    }
}