package net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.effects;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FireResistanceEffect implements ArmorEffect {
    private static final String EffectKey = "fire_resistance";
    private static final PotionEffectType EffectType = PotionEffectType.FIRE_RESISTANCE;
    private static final int baseCost = 5;

    @Override
    public String getId() {
        return EffectKey;
    }

    @Override
    public int calculateCost(Player player, float value) {
        return baseCost;
    }

    @Override
    public void apply(Player player, float value) {
        player.addPotionEffect(new PotionEffect(EffectType, 200, (int) value, false, false, false));
    }

    @Override
    public void remove(Player player) {
        player.removePotionEffect(EffectType);
    }
}