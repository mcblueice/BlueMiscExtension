package net.mcblueice.bluemiscextension.features.ArmorHide;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.attribute.AttributeModifier.Operation;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.mcblueice.bluemiscextension.utils.ConfigManager;

public final class ArmorHideUtil {
    private ArmorHideUtil() {}

    public static ItemStack armorConvert(ItemStack item, ConfigManager lang) {
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
            if (prefix.equals("CHAINMAIL")) displayComponent = Component.text("§e").append(displayComponent);
            newMeta.displayName(displayComponent);
        } else {
            String armorKey = "ArmorHide.ArmorNames." + item.getType().name();
            String armorName = lang.get(armorKey);
            if (armorName == null || armorName.isEmpty() || armorName.equals(armorKey)) armorName = item.getType().name().replace("_", " ");
            if (prefix.equals("CHAINMAIL")) armorName = "§e" + armorName;
            newMeta.displayName(Component.text(armorName).decoration(TextDecoration.ITALIC, false));
        }

        // lore
        List<Component> loreComponent = oldMeta.lore() != null ? new ArrayList<>(oldMeta.lore()) : new ArrayList<>();
        loreComponent.add(Component.text(""));

        // enchants
        for (Enchantment ench : oldMeta.getEnchants().keySet()) {
            int level = oldMeta.getEnchantLevel(ench);
            newMeta.addEnchant(ench, level, true);
        }

        // attributes
        if (oldMeta.getAttributeModifiers() != null && !oldMeta.getAttributeModifiers().isEmpty()) {
            loreComponent.add(Component.text(lang.get("ArmorHide.Attributes.EQUIPMENT")));
            oldMeta.getAttributeModifiers().forEach((attribute, modifier) -> {
                String attributeKey = lang.get("ArmorHide.Attributes." + attribute.getKey().getKey());

                double value = modifier.getAmount();
                String text;
                if (modifier.getOperation() == Operation.ADD_NUMBER) {
                    text = lang.get(attributeKey, String.valueOf((int) value));
                } else if (modifier.getOperation() == Operation.ADD_SCALAR) {
                    text = lang.get(attributeKey, String.valueOf((int) (value * 100)) + "%");
                } else if (modifier.getOperation() == Operation.MULTIPLY_SCALAR_1) {
                    text = lang.get(attributeKey, String.valueOf((int) ((value + 1) * 100)) + "%");
                } else {
                    text = attributeKey + ": " + value;
                }

                loreComponent.add(Component.text(text));
            });
        } else {
            int slotIndex = -1;
            if (equipmentType.equals("HELMET")) {
                slotIndex = 0;
                loreComponent.add(Component.text(lang.get("ArmorHide.Attributes.HELMET")));
            }
            if (equipmentType.equals("CHESTPLATE")) {
                slotIndex = 1;
                loreComponent.add(Component.text(lang.get("ArmorHide.Attributes.CHESTPLATE")));
            }
            if (equipmentType.equals("LEGGINGS")) {
                slotIndex = 2;
                loreComponent.add(Component.text(lang.get("ArmorHide.Attributes.LEGGINGS")));
            }
            if (equipmentType.equals("BOOTS")) {
                slotIndex = 3;
                loreComponent.add(Component.text(lang.get("ArmorHide.Attributes.BOOTS")));
            }

            int[] armorList = getArmor(prefix);
            int[] toughnessList = getToughness(prefix);
            int[] knockbackList = getKnockback(prefix);
            if (armorList != null && armorList.length > slotIndex) {
                int armor = armorList[slotIndex];
                if (armor > 0) loreComponent.add(Component.text(lang.get("ArmorHide.Attributes.armor", armor)));
            }
            if (toughnessList != null && toughnessList.length > slotIndex) {
                int toughness = toughnessList[slotIndex];
                if (toughness > 0) loreComponent.add(Component.text(lang.get("ArmorHide.Attributes.armor_toughness", toughness)));
            }
            if (knockbackList != null && knockbackList.length > slotIndex) {
                int knockback = knockbackList[slotIndex];
                if (knockback > 0) loreComponent.add(Component.text(lang.get("ArmorHide.Attributes.knockback_resistance", knockback)));
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
                if (damage > 0) loreComponent.add(Component.text(lang.get("ArmorHide.Durability", remaining, maxDurability)));
            }
        }

        newMeta.addItemFlags(oldMeta.getItemFlags().toArray(new ItemFlag[0]));
        newMeta.lore(loreComponent);

        newItem.setItemMeta(newMeta);
        return newItem;
    }

    private static int[] getArmor(String prefix) {
        switch (prefix) {
            case "NETHERITE": return new int[] {3,8,6,3};
            case "GOLDEN": return new int[] {2,5,3,1};
            case "DIAMOND": return new int[] {3,8,6,3};
            case "IRON": return new int[] {2,6,5,2};
            case "COPPER": return new int[] {2,4,3,1};
            case "CHAINMAIL": return new int[] {2,5,4,1};
            case "LEATHER": return new int[] {1,3,2,1};
            case "TURTLE": return new int[] {2,0,0,0};
            default: return new int[] {0,0,0,0};
        }
    }
    private static int[] getToughness(String prefix) {
        switch (prefix) {
            case "NETHERITE": return new int[] {3,3,3,3};
            case "DIAMOND": return new int[] {2,2,2,2};
            default: return new int[] {0,0,0,0};
        }
    }
    private static int[] getKnockback(String prefix) {
        switch (prefix) {
            case "NETHERITE": return new int[] {1,1,1,1};
            default: return new int[] {0,0,0,0};
        }
    }
}
