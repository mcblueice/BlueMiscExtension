package net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

public class ArmorEnergyManager {

    private ArmorEnergyManager() {}

    public static long getEnergy(Player player, NamespacedKey energyKey) {
        return player.getPersistentDataContainer().getOrDefault(energyKey, PersistentDataType.LONG, 0L);
    }

    public static void setEnergy(Player player, NamespacedKey energyKey, long amount) {
        if (amount < 0) amount = 0;
        player.getPersistentDataContainer().set(energyKey, PersistentDataType.LONG, amount);
    }
    
    public static void addEnergy(Player player, NamespacedKey energyKey, long amount) {
        setEnergy(player, energyKey, getEnergy(player, energyKey) + amount);
    }

    public static void removeEnergy(Player player, NamespacedKey energyKey, long amount) {
        setEnergy(player, energyKey, getEnergy(player, energyKey) - amount);
    }
}
