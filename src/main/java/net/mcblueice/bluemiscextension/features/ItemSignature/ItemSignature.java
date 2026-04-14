package net.mcblueice.bluemiscextension.features.ItemSignature;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.commands.CommandManager;
import net.mcblueice.bluemiscextension.features.Feature;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluelib.utils.TextUtil;

public class ItemSignature implements Feature {
    private final BlueMiscExtension plugin;
    private final CommandManager commandManager;
    private final ConfigManager lang;
    private final boolean debug;

    private final NamespacedKey signerUuidKey;
    private final NamespacedKey signerNameKey;
    private ItemSignatureListener mapProtectionListener;

    public ItemSignature(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.commandManager = plugin.getCommandManager();
        this.lang = plugin.getLanguageManager();
        this.debug = plugin.getConfig().getBoolean("Features.ItemSignature.debug", false);
        this.signerUuidKey = new NamespacedKey(plugin, "itemsign_signer_uuid");
        this.signerNameKey = new NamespacedKey(plugin, "itemsign_signer_name");
    }

    @Override
    public void register() {
        commandManager.register(new ItemSignatureCommand(plugin, this), "bluemiscextension.itemsign", new String[]{"sign"});

        mapProtectionListener = new ItemSignatureListener(plugin, this);
        Bukkit.getPluginManager().registerEvents(mapProtectionListener, plugin);
    }

    @Override
    public void unregister() {
        commandManager.unregister("sign");

        if (mapProtectionListener != null) {
            HandlerList.unregisterAll(mapProtectionListener);
            mapProtectionListener = null;
        }
    }

    public void signMainHandItem(Player player) {
        if (player == null) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            player.sendMessage(lang.get("Prefix.Default") + lang.get("ItemSignature.NoItemInHand"));
            return;
        }

        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) {
            player.sendMessage(lang.get("Prefix.Default") + lang.get("ItemSignature.UnsupportedItem"));
            return;
        }

        PersistentDataContainer pdc = itemMeta.getPersistentDataContainer();
        List<Component> lore = itemMeta.lore() != null ? new ArrayList<>(itemMeta.lore()) : new ArrayList<>();
        String oldSignerName = pdc.getOrDefault(signerNameKey, PersistentDataType.STRING, "");

        UUID ownerUuid = getSignerUuid(item);
        if (ownerUuid != null) {
            if (!ownerUuid.equals(player.getUniqueId()) && !player.hasPermission("bluemiscextension.itemsign.override")) {
                player.sendMessage(lang.get("Prefix.Default") + lang.get("ItemSignature.NotOwner", getSignerName(item)));
                return;
            }

            String signerNameForLore = oldSignerName.isBlank() ? player.getName() : oldSignerName;
            Component signedLore = TextUtil.parse(lang.get("ItemSignature.SignedLore", signerNameForLore)).decoration(TextDecoration.ITALIC, false);
            lore.removeIf(signedLore::equals);

            pdc.remove(signerUuidKey);
            pdc.remove(signerNameKey);

            itemMeta.lore(lore.isEmpty() ? null : lore);
            item.setItemMeta(itemMeta);
            player.getInventory().setItemInMainHand(item);
            player.updateInventory();

            player.sendMessage(lang.get("Prefix.Default") + lang.get("ItemSignature.UnsignedSuccess"));
            if (debug) plugin.sendDebug("玩家 " + player.getName() + " 已解除主手物品署名 @" + player.getWorld().getName() + "," + player.getLocation().getBlockX() + " " + player.getLocation().getBlockY() + " " + player.getLocation().getBlockZ());
            return;
        }

        if (!oldSignerName.isBlank()) {
            Component oldSignedLore = TextUtil.parse(lang.get("ItemSignature.SignedLore", oldSignerName)).decoration(TextDecoration.ITALIC, false);
            lore.removeIf(oldSignedLore::equals);
        }

        String signerName = player.getName();
        pdc.set(signerUuidKey, PersistentDataType.STRING, player.getUniqueId().toString());
        pdc.set(signerNameKey, PersistentDataType.STRING, signerName);

        lore.add(TextUtil.parse(lang.get("ItemSignature.SignedLore", signerName)).decoration(TextDecoration.ITALIC, false));
        itemMeta.lore(lore);
        item.setItemMeta(itemMeta);
        player.getInventory().setItemInMainHand(item);
        player.updateInventory();

        player.sendMessage(lang.get("Prefix.Default") + lang.get("ItemSignature.SignedSuccess"));
        if (debug) plugin.sendDebug("玩家 " + signerName + " 已為主手物品署名 @" + player.getWorld().getName() + "," + player.getLocation().getBlockX() + " " + player.getLocation().getBlockY() + " " + player.getLocation().getBlockZ());
    }

    public UUID getSignerUuid(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return null;

        String raw = itemMeta.getPersistentDataContainer().getOrDefault(signerUuidKey, PersistentDataType.STRING, "").trim();
        if (raw.isBlank()) return null;

        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public String getSignerName(ItemStack item) {
        if (item == null || item.getType().isAir()) return "UNKNOWN";
        ItemMeta itemMeta = item.getItemMeta();
        if (itemMeta == null) return "UNKNOWN";
        return itemMeta.getPersistentDataContainer().getOrDefault(signerNameKey, PersistentDataType.STRING, "UNKNOWN");
    }

    public boolean isSignedMap(ItemStack item) {
        return item != null && item.getType() == Material.FILLED_MAP && getSignerUuid(item) != null;
    }
}