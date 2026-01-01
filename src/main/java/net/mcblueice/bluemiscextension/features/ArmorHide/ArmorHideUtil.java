package net.mcblueice.bluemiscextension.features.ArmorHide;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import com.google.common.collect.Multimap;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluemiscextension.utils.ServerUtil;

public final class ArmorHideUtil {
    private ArmorHideUtil() {}

    public static ItemStack armorConvert(ItemStack item, ConfigManager lang) {
        DecimalFormat df = new DecimalFormat("#.##");
        if (item == null) return null;

        // material
        String materialName = item.getType().toString();
        String equipmentType = materialName.contains("_") ? materialName.substring(materialName.lastIndexOf('_') + 1) : null;
        if (equipmentType == null) return item;

        String prefix;
        int idx = materialName.indexOf('_');
        prefix = (idx > 0) ? materialName.substring(0, idx) : materialName;

        Material newMaterial;
        switch (prefix) {
            case "NETHERITE":
                newMaterial = Material.POLISHED_BLACKSTONE_BUTTON;
                break;
            case "GOLDEN":
                newMaterial = Material.BAMBOO_BUTTON;
                break;
            case "DIAMOND":
                newMaterial = Material.WARPED_BUTTON;
                break;
            case "IRON":
                newMaterial = Material.STONE_BUTTON;
                break;
            case "COPPER":
                newMaterial = Material.ACACIA_BUTTON;
                break;
            case "CHAINMAIL":
                newMaterial = Material.BIRCH_BUTTON;
                break;
            case "LEATHER":
                newMaterial = Material.JUNGLE_BUTTON;
                break;
            case "TURTLE":
                newMaterial = Material.CRIMSON_BUTTON;
                break;
            default:
                newMaterial = Material.OAK_BUTTON;
                break;
        }
        ItemStack newItem = new ItemStack(newMaterial);
        ItemMeta newMeta = newItem.getItemMeta();
        ItemMeta oldMeta = item.getItemMeta();
        if (oldMeta == null) {
            newItem.setItemMeta(newMeta);
            return newItem;
        }

        // displayname
        Component displayComponent = oldMeta.displayName();
        if (displayComponent != null) {
            if (prefix.equals("CHAINMAIL")) displayComponent = displayComponent.color(NamedTextColor.YELLOW);
        } else {
            displayComponent = Component.translatable(item.getType().getItemTranslationKey());
            if (prefix.equals("CHAINMAIL")) displayComponent = displayComponent.color(NamedTextColor.YELLOW);
        }
        newMeta.displayName(displayComponent.decoration(TextDecoration.ITALIC, false));

        // lore
        List<Component> loreComponent = oldMeta.lore() != null ? new ArrayList<>(oldMeta.lore()) : new ArrayList<>();

        // enchants
        for (Enchantment ench : oldMeta.getEnchants().keySet()) {
            int level = oldMeta.getEnchantLevel(ench);
            newMeta.addEnchant(ench, level, true);
        }

        // attributes
        newMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (!oldMeta.hasItemFlag(ItemFlag.HIDE_ATTRIBUTES)) {
            if (oldMeta.getAttributeModifiers() != null && !oldMeta.getAttributeModifiers().isEmpty()) {
                EquipmentSlotGroup[] slots = {
                    EquipmentSlotGroup.ANY,
                    EquipmentSlotGroup.ARMOR,
                    EquipmentSlotGroup.HAND,
                    EquipmentSlotGroup.MAINHAND,
                    EquipmentSlotGroup.OFFHAND,
                    EquipmentSlotGroup.FEET,
                    EquipmentSlotGroup.LEGS,
                    EquipmentSlotGroup.CHEST,
                    EquipmentSlotGroup.HEAD
                };

                for (EquipmentSlotGroup slot : slots) {
                    List<Map.Entry<Attribute, AttributeModifier>> activeModifiers = new ArrayList<>();
                    for (Map.Entry<Attribute, AttributeModifier> entry : oldMeta.getAttributeModifiers().entries()) {
                        AttributeModifier modifier = entry.getValue();
                        if (modifier.getSlotGroup().equals(slot)) activeModifiers.add(entry);
                    }

                    if (!activeModifiers.isEmpty()) {
                        loreComponent.add(Component.empty());
                        loreComponent.add(Component.translatable(getSlotKey(slot)).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

                        for (Map.Entry<Attribute, AttributeModifier> entry : activeModifiers) {
                            addAttributeLore(loreComponent, entry.getKey(), entry.getValue(), df);
                        }
                    }
                }
            } else {
                EquipmentSlot slot = null;
                String slotKey = null;
                if (equipmentType.equals("HELMET")) {
                    slot = EquipmentSlot.HEAD;
                    slotKey = "head";
                }
                if (equipmentType.equals("CHESTPLATE")) {
                    slot = EquipmentSlot.CHEST;
                    slotKey = "chest";
                }
                if (equipmentType.equals("LEGGINGS")) {
                    slot = EquipmentSlot.LEGS;
                    slotKey = "legs";
                }
                if (equipmentType.equals("BOOTS")) {
                    slot = EquipmentSlot.FEET;
                    slotKey = "feet";
                }

                if (slot != null) {
                    loreComponent.add(Component.empty());
                    loreComponent.add(Component.translatable("item.modifiers." + slotKey)
                            .color(NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false));

                    Multimap<Attribute, AttributeModifier> defaults = item.getType().getDefaultAttributeModifiers(slot);

                    for (Map.Entry<Attribute, AttributeModifier> entry : defaults.entries()) {
                        if (entry.getValue().getAmount() == 0) continue;
                        addAttributeLore(loreComponent, entry.getKey(), entry.getValue(), df);
                    }
                    addEnchantmentLore(loreComponent, item, df, slot);
                }
            }
        }

        // durability
        if (oldMeta.isUnbreakable()) {
            newMeta.setUnbreakable(true);
        } else {
            if (oldMeta instanceof Damageable) {
                int damage = ((Damageable) oldMeta).getDamage();
                int maxDurability = item.getType().getMaxDurability();
                int remaining = maxDurability - damage;
                if (damage > 0) {
                    loreComponent.add(Component.translatable("item.durability", Component.text(remaining), Component.text(maxDurability))
                            .color(NamedTextColor.WHITE)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
        }

        newMeta.addItemFlags(oldMeta.getItemFlags().toArray(new ItemFlag[0]));
        newMeta.lore(loreComponent);

        newItem.setItemMeta(newMeta);
        return newItem;
    }

    private static void addEnchantmentLore(List<Component> loreComponent, ItemStack item, DecimalFormat df, EquipmentSlot slot) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            Enchantment ench = entry.getKey();
            int level = entry.getValue();
            String key = ench.getKey().getKey();

            // 迅捷潛行 - 護腿
            if (key.equals("swift_sneak") && slot == EquipmentSlot.LEGS) {
                double amount = level * 0.15;
                loreComponent.add(Component.text()
                        .append(Component.text("+" + df.format(amount) + " "))
                        .append(Component.translatable(getAttributeKey(Attribute.PLAYER_SNEAKING_SPEED)))
                        .color(NamedTextColor.BLUE)
                        .decoration(TextDecoration.ITALIC, false)
                        .build());
            }

            // 深海探索者 - 靴子
            if (key.equals("depth_strider") && slot == EquipmentSlot.FEET) {
                double amount = level * 0.33;
                loreComponent.add(Component.text()
                        .append(Component.text("+" + df.format(amount) + " "))
                        .append(Component.translatable(getAttributeKey(Attribute.GENERIC_WATER_MOVEMENT_EFFICIENCY)))
                        .color(NamedTextColor.BLUE)
                        .decoration(TextDecoration.ITALIC, false)
                        .build());
            }

            // 水下呼吸 - 頭盔
            if (key.equals("respiration") && slot == EquipmentSlot.HEAD) {
                double amount = level * 1.0;
                loreComponent.add(Component.text()
                        .append(Component.text("+" + df.format(amount) + " "))
                        .append(Component.translatable(getAttributeKey(Attribute.GENERIC_OXYGEN_BONUS)))
                        .color(NamedTextColor.BLUE)
                        .decoration(TextDecoration.ITALIC, false)
                        .build());
            }

            // 水下挖掘 - 頭盔
            if (key.equals("aqua_affinity") && slot == EquipmentSlot.HEAD) {
                loreComponent.add(Component.text()
                        .append(Component.text("+400% "))
                        .append(Component.translatable(getAttributeKey(Attribute.PLAYER_SUBMERGED_MINING_SPEED)))
                        .color(NamedTextColor.BLUE)
                        .decoration(TextDecoration.ITALIC, false)
                        .build());
            }
        }
    }

    private static String getSlotKey(EquipmentSlotGroup slot) {
        if (slot.equals(EquipmentSlotGroup.ARMOR)) return "item.modifiers.armor";
        if (slot.equals(EquipmentSlotGroup.HAND)) return "item.modifiers.hand";
        if (slot.equals(EquipmentSlotGroup.MAINHAND)) return "item.modifiers.mainhand";
        if (slot.equals(EquipmentSlotGroup.OFFHAND)) return "item.modifiers.offhand";
        if (slot.equals(EquipmentSlotGroup.FEET)) return "item.modifiers.feet";
        if (slot.equals(EquipmentSlotGroup.LEGS)) return "item.modifiers.legs";
        if (slot.equals(EquipmentSlotGroup.CHEST)) return "item.modifiers.chest";
        if (slot.equals(EquipmentSlotGroup.HEAD)) return "item.modifiers.head";
        return "item.modifiers.any";
    }
    private static String getAttributeKey(Attribute attribute) {
        String attributeBaseKey = (ServerUtil.isNewAttributeKey()) ? "attribute.name." : "attribute.name.generic.";
        return attributeBaseKey + attribute.getKey().getKey();
    }

    private static void addAttributeLore(List<Component> loreComponent, Attribute attribute, AttributeModifier modifier, DecimalFormat df) {
        double amount = modifier.getAmount();
        double displayValue;
        String displaySuffix = "";

        switch (modifier.getOperation()) {
            case ADD_SCALAR:
                displayValue = amount * 100;
                displaySuffix = "%";
                break;
            case MULTIPLY_SCALAR_1:
                displayValue = (amount + 1) * 100;
                displaySuffix = "%";
                break;
            default:
                if (attribute == Attribute.GENERIC_KNOCKBACK_RESISTANCE) {
                    displayValue = amount * 10;
                } else {
                    displayValue = amount;
                }
                break;
        }

        NamedTextColor displayColor = amount > 0 ? NamedTextColor.BLUE : NamedTextColor.RED;
        String prefixSign = amount > 0 ? "+" : "";

        loreComponent.add(Component.text()
                .append(Component.text(prefixSign + df.format(displayValue) + displaySuffix + " "))
                .append(Component.translatable(getAttributeKey(attribute)))
                .color(displayColor)
                .decoration(TextDecoration.ITALIC, false)
                .build());
    }
}
