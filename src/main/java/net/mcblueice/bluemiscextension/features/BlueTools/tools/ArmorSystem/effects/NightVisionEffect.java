package net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.effects;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class NightVisionEffect implements ArmorEffect {
    private static final String EffectKey = "night_vision";
    private static final PotionEffectType EffectType = PotionEffectType.NIGHT_VISION;
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
        player.addPotionEffect(new PotionEffect(EffectType, 400, (int) value, false, false, false));
    }

    @Override
    public void remove(Player player) {
        player.removePotionEffect(EffectType);
    }
}