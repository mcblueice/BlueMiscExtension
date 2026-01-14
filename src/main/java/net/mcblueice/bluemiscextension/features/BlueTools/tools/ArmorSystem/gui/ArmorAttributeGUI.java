package net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.ArmorSystem;
import net.mcblueice.bluemiscextension.utils.ConfigManager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ArmorAttributeGUI implements Listener {

    private final BlueMiscExtension plugin;
    private final ConfigManager lang;
    private final ArmorSystem armorSystem;
    private final Component GUI_TITLE;
    private final DecimalFormat df = new DecimalFormat("#.##");

    public ArmorAttributeGUI(ArmorSystem armorSystem) {
        this.plugin = BlueMiscExtension.getInstance();
        this.lang = plugin.getLanguageManager();
        this.armorSystem = armorSystem;
        this.GUI_TITLE = lang.getComponent("ArmorSystem.GUI.Attribute.Title");
    }

    public static class AttributeGUIHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() { return null; }
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(new AttributeGUIHolder(), 27, GUI_TITLE);

        renderBackground(inv);

        // 參數讀取 (全部統一使用 Float)
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        
        // 從 ArmorSystem 獲取當前裝備的限制
        Map<String, float[]> specsMap = armorSystem.getDesiredArmorEffects(player);

        // 定義默認規格 (Fallback): {Default, Min, Max, AllowCustom}
        float[] speedSpecs = specsMap.getOrDefault("speed", new float[]{0.05f, 0.05f, 0.05f, 0f});
        float[] flySpecs = specsMap.getOrDefault("flight", new float[]{0.1f, 0.1f, 0.1f, 0f});
        float[] jumpSpecs = specsMap.getOrDefault("jump", new float[]{0.25f, 0.25f, 0.25f, 0f});

        // 決定顯示數值: 如果允許自定義，讀取 PDC；否則顯示預設值
        float speedVal = (speedSpecs[3] == 1.0f) ? pdc.getOrDefault(armorSystem.speedValueKey, PersistentDataType.FLOAT, speedSpecs[0]) : speedSpecs[0];
        float flyVal = (flySpecs[3] == 1.0f) ? pdc.getOrDefault(armorSystem.flySpeedValueKey, PersistentDataType.FLOAT, flySpecs[0]) : flySpecs[0];
        float jumpVal = (jumpSpecs[3] == 1.0f) ? pdc.getOrDefault(armorSystem.jumpValueKey, PersistentDataType.FLOAT, jumpSpecs[0]) : jumpSpecs[0];

        // Speed Column (2, 11, 20)
        inv.setItem(2, createButton(true, speedSpecs[3] == 1.0f));
        inv.setItem(11, createDisplayItem(Material.SUGAR, "ArmorSystem.GUI.Attribute.Speed.Name", speedVal, speedSpecs));
        inv.setItem(20, createButton(false, speedSpecs[3] == 1.0f));

        // Fly Column (4, 13, 22)
        inv.setItem(4, createButton(true, flySpecs[3] == 1.0f));
        inv.setItem(13, createDisplayItem(Material.FEATHER, "ArmorSystem.GUI.Attribute.Flight.Name", flyVal, flySpecs));
        inv.setItem(22, createButton(false, flySpecs[3] == 1.0f));

        // Jump Column (6, 15, 24)
        inv.setItem(6, createButton(true, jumpSpecs[3] == 1.0f));
        inv.setItem(15, createDisplayItem(Material.RABBIT_FOOT, "ArmorSystem.GUI.Attribute.Jump.Name", jumpVal, jumpSpecs));
        inv.setItem(24, createButton(false, jumpSpecs[3] == 1.0f));

        // 返回按鈕
        inv.setItem(26, createBackItem());

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1f, 1f);
    }

    private void renderBackground(Inventory inv) {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.empty());
            border.setItemMeta(meta);
        }
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, border);
        }
    }

    private ItemStack createDisplayItem(Material material, String nameKey, float value, float[] specs) {
        float defaultVal = specs[0];
        float min = specs[1];
        float max = specs[2];
        boolean canEdit = specs[3] == 1.0f;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            Component nameComp = lang.getComponent(nameKey).color(canEdit ? NamedTextColor.GOLD : NamedTextColor.GRAY);
            meta.displayName(nameComp);
            List<Component> lore = new ArrayList<>();
            lore.add(lang.getComponent("ArmorSystem.GUI.Attribute.Lore.CurrentValue").append(Component.text(df.format(value))));
            lore.add(lang.getComponent("ArmorSystem.GUI.Attribute.Lore.DefaultValue").append(Component.text(df.format(defaultVal))));
            
            if (canEdit) {
                lore.add(lang.getComponent("ArmorSystem.GUI.Attribute.Lore.Range").append(Component.text(df.format(min) + " ~ " + df.format(max))));
            } else {
                 lore.add(Component.empty());
                 lore.add(lang.getComponent("ArmorSystem.GUI.Attribute.Lore.Locked"));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createButton(boolean isIncrease, boolean isEnabled) {
        Material mat;
        Component nameComp;
        
        if (!isEnabled) {
            mat = Material.GRAY_STAINED_GLASS_PANE;
            nameComp = lang.getComponent("ArmorSystem.GUI.Attribute.Button.Unavailable").color(NamedTextColor.GRAY);
        } else {
            mat = isIncrease ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
            nameComp = lang.getComponent(isIncrease ? "ArmorSystem.GUI.Attribute.Button.Increase" : "ArmorSystem.GUI.Attribute.Button.Decrease")
                    .color(isIncrease ? NamedTextColor.GREEN : NamedTextColor.RED);
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(nameComp);
            if (isEnabled) {
                List<Component> lore = new ArrayList<>();
                lore.add(lang.getComponent(isIncrease ? "ArmorSystem.GUI.Attribute.Button.Lore.ClickIncrease" : "ArmorSystem.GUI.Attribute.Button.Lore.ClickDecrease"));
                lore.add(lang.getComponent(isIncrease ? "ArmorSystem.GUI.Attribute.Button.Lore.ShiftClickIncrease" : "ArmorSystem.GUI.Attribute.Button.Lore.ShiftClickDecrease"));
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(lang.getComponent("ArmorSystem.GUI.Common.Back").color(NamedTextColor.RED));
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof AttributeGUIHolder) {
            event.setCancelled(true);
            if (!(event.getWhoClicked() instanceof Player)) return;
            Player player = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            // 返回按鈕
            if (clicked.getType() == Material.ARROW) {
                armorSystem.getSettingsGUI().open(player);
                return;
            }

            int slot = event.getSlot();

            // 獲取當前規格
            Map<String, float[]> specsMap = armorSystem.getDesiredArmorEffects(player);
            float[] speedSpecs = specsMap.getOrDefault("speed", new float[]{0.05f, 0.05f, 0.05f, 0f});
            float[] flySpecs = specsMap.getOrDefault("flight", new float[]{0.1f, 0.1f, 0.1f, 0f});
            float[] jumpSpecs = specsMap.getOrDefault("jump", new float[]{0.25f, 0.25f, 0.25f, 0f});

            // Speed
            if (slot == 2) adjustValue(player, armorSystem.speedValueKey, event.getClick(), speedSpecs, 1);
            if (slot == 20) adjustValue(player, armorSystem.speedValueKey, event.getClick(), speedSpecs, -1);
            
            // Fly
            if (slot == 4) adjustValue(player, armorSystem.flySpeedValueKey, event.getClick(), flySpecs, 1);
            if (slot == 22) adjustValue(player, armorSystem.flySpeedValueKey, event.getClick(), flySpecs, -1);

            // Jump
            if (slot == 6) adjustValue(player, armorSystem.jumpValueKey, event.getClick(), jumpSpecs, 1);
            if (slot == 24) adjustValue(player, armorSystem.jumpValueKey, event.getClick(), jumpSpecs, -1);
        }
    }

    private void adjustValue(Player player, NamespacedKey key, ClickType click, float[] specs, int direction) {
        boolean canEdit = specs[3] == 1.0f;
        if (!canEdit) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            plugin.sendMessage(player, "Armor", lang.get("ArmorSystem.Message.LockedAttribute"));
            return;
        }

        float defaultVal = specs[0];
        float min = specs[1];
        float max = specs[2];

        PersistentDataContainer pdc = player.getPersistentDataContainer();
        float current = pdc.getOrDefault(key, PersistentDataType.FLOAT, defaultVal);

        float change = click.isShiftClick() ? 0.05f : 0.01f;
        change *= direction;

        float newVal = Math.max(min, Math.min(max, current + change));
        
        // 統一儲存為 FLOAT
        pdc.set(key, PersistentDataType.FLOAT, newVal);

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
        open(player); // 刷新
    }
}
