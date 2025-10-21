package net.mcblueice.bluemiscextension.features.ShulkerBox;

import java.util.UUID;

import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Openable;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import net.mcblueice.bluemiscextension.BlueMiscExtension;

public final class ShulkerBoxUtil {

    private ShulkerBoxUtil() {}

// #region UUID 處理
    private static NamespacedKey key() {
        return new NamespacedKey(BlueMiscExtension.getInstance(), "shulkerbox_uuid");
    }

    public static UUID getUUID(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String uuid = meta.getPersistentDataContainer().get(key(), PersistentDataType.STRING);
        if (uuid == null || uuid.isEmpty()) return null;
        return UUID.fromString(uuid);
    }

    public static UUID addUUID(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        UUID uuid = UUID.randomUUID();
        meta.getPersistentDataContainer().set(key(), PersistentDataType.STRING, uuid.toString());
        item.setItemMeta(meta);
        return uuid;
    }

    public static void removeUUID(ItemStack item) {
        if (item == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().remove(key());
        item.setItemMeta(meta); 
    }
// #endregion

    public static boolean isInteractableBlock(Block block) {
        if (block == null) return false;
        BlockState state = block.getState();
        if (state instanceof InventoryHolder) return true;
        if (block.getBlockData() instanceof Openable) return true;

        String name = block.getType().name();
        if (name.contains("BUTTON")
            || name.contains("PRESSURE_PLATE")
            || name.contains("LEVER")
            || name.contains("DOOR")
            || name.contains("TRAPDOOR")
            || name.contains("LECTERN")
            || name.contains("CRAFTING_TABLE")
            || name.contains("ENCHANTING_TABLE")
            || name.contains("ANVIL")
            || name.contains("STONECUTTER")
            || name.contains("LOOM")
            || name.contains("JUKEBOX")
            || name.contains("NOTE_BLOCK")
            || name.contains("BELL")
            ) return true;
        return false;
    }
}
