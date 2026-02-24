package net.mcblueice.bluemiscextension.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import java.util.UUID;

public class FloodgateUtil {
    private FloodgateUtil() {
        throw new IllegalStateException("Utility class should not be instantiated");
    }

    public static boolean isFloodgateEnabled() {
        return Bukkit.getPluginManager().getPlugin("floodgate") != null;
    }

    public static boolean isFloodgatePlayer(UUID uuid) {
        if (!isFloodgateEnabled()) return false;
        return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
    }

    public static boolean isFloodgatePlayer(Player player) {
        if (!isFloodgateEnabled() || player == null) return false;
        return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
    }
}
