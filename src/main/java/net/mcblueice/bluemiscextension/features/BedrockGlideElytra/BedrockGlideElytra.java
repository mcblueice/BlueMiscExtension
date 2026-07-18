package net.mcblueice.bluemiscextension.features.BedrockGlideElytra;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.BedrockGlideElytra.Listener.Packets.SetSlotListener;
import net.mcblueice.bluemiscextension.features.BedrockGlideElytra.Listener.Packets.WindowItemsListener;
import net.mcblueice.bluemiscextension.features.Feature;

public class BedrockGlideElytra implements Feature {
    private final ProtocolManager protocolManager;
    private SetSlotListener setSlotListener;
    private WindowItemsListener windowItemsListener;

    public BedrockGlideElytra(BlueMiscExtension plugin) {
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    @Override
    public void register() {
        this.setSlotListener = new SetSlotListener(this);
        this.windowItemsListener = new WindowItemsListener(this);

        protocolManager.addPacketListener(setSlotListener);
        protocolManager.addPacketListener(windowItemsListener);
    }

    @Override
    public void unregister() {
        if (setSlotListener != null) protocolManager.removePacketListener(setSlotListener);
        if (windowItemsListener != null) protocolManager.removePacketListener(windowItemsListener);
    }

    public ItemStack getVirtualElytraForPlayer(Player player) {
        GlideSource source = findGlideSource(player);
        if (source == null) return null;
        return createVirtualElytra(source);
    }

    private ItemStack createVirtualElytra(GlideSource source) {
        ItemStack sourceItem = source.item();
        if (sourceItem == null || sourceItem.getType() == Material.AIR) return null;

        ItemStack virtualElytra = new ItemStack(Material.ELYTRA, 1);
        if (sourceItem.hasData(DataComponentTypes.GLIDER)) virtualElytra.setData(DataComponentTypes.GLIDER);

        ItemMeta virtualMeta = virtualElytra.getItemMeta();
        ItemMeta sourceMeta = sourceItem.getItemMeta();
        if (virtualMeta == null) return virtualElytra;

        if (sourceMeta != null) {
            virtualMeta.setUnbreakable(sourceMeta.isUnbreakable());
            virtualMeta.addItemFlags(sourceMeta.getItemFlags().toArray(new ItemFlag[0]));
        }

        if (sourceMeta instanceof Damageable sourceDamageable && virtualMeta instanceof Damageable virtualDamageable) {
            virtualDamageable.setDamage(sourceDamageable.getDamage());
        }

        virtualMeta.displayName(Component.text("虛擬鞘翅").color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        virtualMeta.lore(buildLore(source, sourceMeta));
        virtualElytra.setItemMeta(virtualMeta);
        return virtualElytra;
    }

    private List<Component> buildLore(GlideSource source, ItemMeta sourceMeta) {
        List<Component> lore = (sourceMeta != null && sourceMeta.lore() != null)
                ? new ArrayList<>(sourceMeta.lore())
                : new ArrayList<>();

        if (!lore.isEmpty()) lore.add(Component.empty());
        lore.add(Component.text()
            .append(Component.text("由 "))
            .append(Component.translatable(source.slotTranslationKey()))
            .append(Component.text(" 上的鞘翅效果觸發"))
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)
            .build());
        lore.add(Component.text()
            .append(Component.text("脫下 "))
            .append(Component.translatable(source.slotTranslationKey()))
            .append(Component.text(" 即可取下此虛擬鞘翅"))
            .color(NamedTextColor.GRAY)
            .decoration(TextDecoration.ITALIC, false)
            .build());

        if (sourceMeta instanceof Damageable damageable) {
            int max = source.item().getType().getMaxDurability();
            if (max > 0) {
                int remain = Math.max(0, max - damageable.getDamage());
                lore.add(Component.translatable("item.durability", Component.text(remain), Component.text(max))
                        .color(NamedTextColor.WHITE)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
        return lore;
    }

    private GlideSource findGlideSource(Player player) {
        if (player == null) return null;
        ItemStack chest = player.getInventory().getChestplate();
        if (hasGliderComponent(chest)) return new GlideSource(chest, "item.modifiers.chest");

        ItemStack leggings = player.getInventory().getLeggings();
        if (hasGliderComponent(leggings)) return new GlideSource(leggings, "item.modifiers.legs");

        ItemStack boots = player.getInventory().getBoots();
        if (hasGliderComponent(boots)) return new GlideSource(boots, "item.modifiers.feet");

        ItemStack helmet = player.getInventory().getHelmet();
        if (hasGliderComponent(helmet)) return new GlideSource(helmet, "item.modifiers.head");

        return null;
    }

    private boolean hasGliderComponent(ItemStack item) {
        return item != null && !item.getType().isAir() && item.hasData(DataComponentTypes.GLIDER) && item.getType() != Material.ELYTRA;
    }

    private record GlideSource(ItemStack item, String slotTranslationKey) {}
}
