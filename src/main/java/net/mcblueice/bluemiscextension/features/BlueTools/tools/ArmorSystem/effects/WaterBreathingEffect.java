package net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.effects;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WaterBreathingEffect implements ArmorEffect {
    private static final String EffectKey = "water_breathing";
    private static final PotionEffectType EffectType = PotionEffectType.WATER_BREATHING;
    private static final int baseCost = 20;

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