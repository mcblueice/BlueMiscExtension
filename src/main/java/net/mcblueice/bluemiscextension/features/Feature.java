package net.mcblueice.bluemiscextension.features;

import org.bukkit.entity.Player;

public interface Feature {
    void register();
    void unregister();
    default void onPlayerDataLoaded(Player player) {}
    default void onPlayerDataUnload(Player player) {}
}
