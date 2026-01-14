package net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.effects;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public class FlightEffect implements ArmorEffect {
    private static final String EffectKey = "flight";
    private static final int baseCost = 10;

    public String getId() {
        return EffectKey;
    }

    @Override
    public int calculateCost(Player player, float value) {
        int cost = Math.round(baseCost * value / 0.1f);
        return player.isFlying() ? (cost * 10) : cost; 
    }

    @Override
    public void apply(Player player, float value) {
        if (!player.getAllowFlight()) player.setAllowFlight(true);

        if (value <= 0) value = 0.1f;
        if (player.getFlySpeed() != value) player.setFlySpeed(value);
    }

    @Override
    public void remove(Player player) {
        if (!player.getGameMode().equals(GameMode.CREATIVE) && !player.getGameMode().equals(GameMode.SPECTATOR)) {
            player.setAllowFlight(false);
            player.setFlying(false);
            player.setFlySpeed(0.1f);
        }
    }
}