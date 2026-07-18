package net.mcblueice.bluemiscextension.features.ElytraArmor;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.Feature;
import net.mcblueice.bluemiscextension.utils.ConfigManager;

public class ElytraArmor implements Feature, Listener {

    private final BlueMiscExtension plugin;
    private final ConfigManager lang;

    public ElytraArmor(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }

    @Override
    public void register() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void unregister() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack slot1 = event.getInventory().getItem(0);
        ItemStack slot2 = event.getInventory().getItem(1);
        if (slot1 == null || slot2 == null) return;

        ItemStack chestplate;
        ItemStack elytra;
        if (slot2.getType() == Material.ELYTRA && Tag.ITEMS_CHEST_ARMOR.isTagged(slot1.getType())) {
            chestplate = slot1;
            elytra = slot2;
        } else if (slot1.getType() == Material.ELYTRA && Tag.ITEMS_CHEST_ARMOR.isTagged(slot2.getType())) {
            chestplate = slot2;
            elytra = slot1;
        } else {
            return;
        }

        ItemStack result = chestplate.clone();
        ItemMeta resultMeta = result.getItemMeta();
        if (resultMeta == null) return;
        if (resultMeta.isGlider()) return;

        ItemMeta elytraMeta = elytra.getItemMeta();
        if (elytraMeta != null) {
            if (elytraMeta.isUnbreakable()) resultMeta.setUnbreakable(true);
            if (elytraMeta.hasEnchants()) {
                for (Map.Entry<Enchantment, Integer> entry : elytraMeta.getEnchants().entrySet()) {
                    Enchantment ench = entry.getKey();
                    int elytraLevel = entry.getValue();
                    int chestLevel = resultMeta.getEnchantLevel(ench);
                    int newLevel = (elytraLevel == chestLevel) ? (elytraLevel + 1) : Math.max(elytraLevel, chestLevel);
                    newLevel = Math.min(newLevel, ench.getMaxLevel());
                    resultMeta.addEnchant(ench, newLevel, true);
                }
            }
        }

        AnvilView anvilView = (AnvilView) event.getView();
        String renameText = anvilView.getRenameText();

        if (!resultMeta.hasDisplayName()) {
            if (renameText != null && !renameText.isEmpty()) {
                resultMeta.displayName(Component.text(renameText));
            } else {
                String matName = chestplate.getType().name();
                String armorType = matName.endsWith("_CHESTPLATE") ? matName.substring(0, matName.indexOf("_CHESTPLATE"))  : "GENERIC";
                Component defaultName = lang.has("ElytraArmor.ArmorName." + armorType) ? lang.getComponent("ElytraArmor.ArmorName." + armorType) : lang.getComponent("ElytraArmor.ArmorName.GENERIC");
                resultMeta.displayName(defaultName.decoration(TextDecoration.ITALIC, false));
            }
        }

        resultMeta.setGlider(true);
        result.setItemMeta(resultMeta);
        event.setResult(result);
        anvilView.setRepairCost(10);
    }
}