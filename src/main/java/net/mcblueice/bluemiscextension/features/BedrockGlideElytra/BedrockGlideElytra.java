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

        virtualMeta.displayName(Component.text("虛擬鞘翅")
                .color(NamedTextColor.GOLD)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        virtualMeta.lore(buildLore(source, sourceMeta));
        virtualElytra.setItemMeta(virtualMeta);
        return virtualElytra;
    }

    private List<Component> buildLore(GlideSource source, ItemMeta sourceMeta) {
        List<Component> lore = (sourceMeta != null && sourceMeta.lore() != null)
                ? new ArrayList<>(sourceMeta.lore())
                : new ArrayList<>();

        if (!lore.isEmpty()) lore.add(Component.empty());
        lore.add(Component.text("由「" + source.slotDisplayName() + "」上的 glide 組件觸發", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("脫下該裝備即可取下此虛擬鞘翅", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

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
        if (hasGliderComponent(chest)) return new GlideSource(chest, "胸甲");

        ItemStack leggings = player.getInventory().getLeggings();
        if (hasGliderComponent(leggings)) return new GlideSource(leggings, "護腿");

        ItemStack boots = player.getInventory().getBoots();
        if (hasGliderComponent(boots)) return new GlideSource(boots, "靴子");

        ItemStack helmet = player.getInventory().getHelmet();
        if (hasGliderComponent(helmet)) return new GlideSource(helmet, "頭盔");

        return null;
    }

    private boolean hasGliderComponent(ItemStack item) {
        return item != null && item.getType() != Material.AIR && item.hasData(DataComponentTypes.GLIDER);
    }

    private record GlideSource(ItemStack item, String slotDisplayName) {}
}
