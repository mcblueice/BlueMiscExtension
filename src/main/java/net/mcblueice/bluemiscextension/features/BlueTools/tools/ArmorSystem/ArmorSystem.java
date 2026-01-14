package net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.effects.FlightEffect;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.effects.JumpEffect;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.effects.MiningSpeedEffect;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.effects.BlockReachEffect;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.effects.FireResistanceEffect;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.effects.SpeedEffect;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.effects.StepAssistEffect;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.effects.ArmorEffect;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.effects.NightVisionEffect;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.effects.WaterBreathingEffect;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.utils.ArmorEnergyManager;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.gui.ArmorAttributeGUI;
import net.mcblueice.bluemiscextension.features.BlueTools.tools.ArmorSystem.gui.ArmorSettingsGUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ArmorSystem implements Listener {
    private final BlueMiscExtension plugin;
    private final ConfigManager lang;
    private final boolean debug;

    public final NamespacedKey effectList;
    public final NamespacedKey enabledEffectsKey;
    public final NamespacedKey armorSetKey;
    public final NamespacedKey energyKey;
    
    // Attribute Value Keys
    public final NamespacedKey speedValueKey;
    public final NamespacedKey jumpValueKey;
    public final NamespacedKey flySpeedValueKey;

    public final Map<String, ArmorEffect> effectRegistry;
    private final ArmorSettingsGUI settingsGUI;
    private final ArmorAttributeGUI attributeGUI;
    private final ArmorSystemBossbar bossBarManager;

    // 預設啟用的功能列表
    public final List<String> defaultEnabledEffects = List.of(
        "flight", "night_vision", "water_breathing", 
        "speed", "jump", "step_assist", "block_reach_increase", "mining_speed"
    );

    public final List<String> armorSetList = List.of("power_armor_mk1", "power_armor_mk2");

    public ArmorSystem(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
        this.debug = plugin.getConfig().getBoolean("Features.BlueTools.debug", false);

        this.armorSetKey = new NamespacedKey("bluetools", "armor_set_id"); // Item String
        this.energyKey = new NamespacedKey("bluetools", "energy_point"); // Player Integer
        this.effectList = new NamespacedKey("bluetools", "effectlist"); //Player List<String>
        this.enabledEffectsKey = new NamespacedKey("bluetools", "enabled_effects"); // Player List<String>
        
        this.speedValueKey = new NamespacedKey("bluetools", "attr_speed_val"); // Player Float
        this.jumpValueKey = new NamespacedKey("bluetools", "attr_jump_val"); // Player Float
        this.flySpeedValueKey = new NamespacedKey("bluetools", "attr_fly_speed_val"); // Player Float

        // 註冊效果
        this.effectRegistry = new HashMap<>();
        this.effectRegistry.put("flight", new FlightEffect());
        this.effectRegistry.put("night_vision", new NightVisionEffect());
        this.effectRegistry.put("water_breathing", new WaterBreathingEffect());
        this.effectRegistry.put("fire_resistance", new FireResistanceEffect());
        this.effectRegistry.put("speed", new SpeedEffect());
        this.effectRegistry.put("jump", new JumpEffect());
        this.effectRegistry.put("step_assist", new StepAssistEffect());
        this.effectRegistry.put("block_reach_increase", new BlockReachEffect());
        this.effectRegistry.put("mining_speed", new MiningSpeedEffect());

        this.bossBarManager = new ArmorSystemBossbar(this);
        this.settingsGUI = new ArmorSettingsGUI(this);
        this.attributeGUI = new ArmorAttributeGUI(this);
        
        plugin.getServer().getPluginManager().registerEvents(this.settingsGUI, plugin);
        plugin.getServer().getPluginManager().registerEvents(this.attributeGUI, plugin);
    }
    
    public ArmorSystemBossbar getBossBarManager() {
        return bossBarManager;
    }

    public ArmorSettingsGUI getSettingsGUI() {
        return settingsGUI;
    }
    
    public ArmorAttributeGUI getAttributeGUI() {
        return attributeGUI;
    }

    private List<String> getPlayerEffectsKey(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        List<String> list = pdc.getOrDefault(effectList, PersistentDataType.LIST.strings(), null);
        return list != null ? list : new ArrayList<>();
    }

    public Map<String, float[]> getDesiredArmorEffects(Player player) {
        Map<String, float[]> desiredEffects = new HashMap<>();

        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack chestplate = player.getInventory().getChestplate();
        ItemStack leggings = player.getInventory().getLeggings();
        ItemStack boots = player.getInventory().getBoots();
        
        for (String targetSetId : armorSetList) {
            boolean hasHelmet = isArmorSet(helmet, targetSetId);
            boolean hasChest = isArmorSet(chestplate, targetSetId);
            boolean hasLegs = isArmorSet(leggings, targetSetId);
            boolean hasBoots = isArmorSet(boots, targetSetId);
            boolean isFullSet = hasHelmet && hasChest && hasLegs && hasBoots;

            switch (targetSetId) {
                case "power_armor_mk1":
                    // float[]{Default, Min, Max, AllowCustom}
                    if (hasHelmet) {
                        desiredEffects.put("night_vision", new float[]{0, 0, 0, 0});
                        desiredEffects.put("water_breathing", new float[]{0, 0, 0, 0});
                    }
                    if (hasLegs) {
                        desiredEffects.put("speed", new float[]{0.05f, 0.05f, 0.05f, 0});
                    }
                    if (hasBoots) {
                        desiredEffects.put("jump", new float[]{0.2f, 0.2f, 0.2f, 0});
                    }
                    if (isFullSet) {
                        desiredEffects.put("fire_resistance", new float[]{0, 0, 0, 0});
                        desiredEffects.put("flight", new float[]{0.1f, 0.1f, 0.1f, 0});
                    }
                    break;
                case "power_armor_mk2":
                    if (hasHelmet) {
                        desiredEffects.put("night_vision", new float[]{0, 0, 0, 0});
                        desiredEffects.put("water_breathing", new float[]{0, 0, 0, 0});
                    }
                    if (hasChest) {
                        desiredEffects.put("block_reach_increase", new float[]{2.0f, 2.0f, 2.0f, 0});
                        desiredEffects.put("mining_speed", new float[]{0.2f, 0.2f, 0.2f, 0});
                    }
                    if (hasLegs) {
                        desiredEffects.put("speed", new float[]{0.05f, 0.01f, 0.1f, 1.0f});
                    }
                    if (hasBoots) {
                        desiredEffects.put("jump", new float[]{0.2f, 0.01f, 0.6f, 1.0f});
                    }
                    if (hasLegs && hasBoots) {
                        desiredEffects.put("step_assist", new float[]{1.0f, 1.0f, 1.0f, 0});
                    }
                    if (isFullSet) {
                        desiredEffects.put("fire_resistance", new float[]{0, 0, 0, 0});
                        desiredEffects.put("flight", new float[]{0.1f, 0.01f, 0.2f, 1.0f});
                    }
                    break;
                default:
                    break;
            }
        }
        return desiredEffects;
    }

    public boolean isArmorSet(ItemStack item, String targetId) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(armorSetKey, PersistentDataType.STRING)) return false;
        String id = pdc.get(armorSetKey, PersistentDataType.STRING);
        return targetId.equals(id);
    }

    // 取得玩家啟用的效果列表
    public List<String> getEnabledEffects(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        List<String> list = pdc.getOrDefault(enabledEffectsKey, PersistentDataType.LIST.strings(), null);
        if (list == null) {
            list = new ArrayList<>(defaultEnabledEffects);
            pdc.set(enabledEffectsKey, PersistentDataType.LIST.strings(), list);
        }
        return list;
    }

    // 啟用/禁用特定效果
    public boolean toggleEffect(Player player, String effectId, boolean state) {
        if (!effectRegistry.containsKey(effectId)) return false;

        List<String> list = new ArrayList<>(getEnabledEffects(player));
        boolean isEnabled = list.contains(effectId);

        if (state) {
            if (!isEnabled) list.add(effectId);
        } else {
            list.remove(effectId);
        }
        
        player.getPersistentDataContainer().set(enabledEffectsKey, PersistentDataType.LIST.strings(), list);
        return state; 
    }

    public void tickPlayer(Player player) {
        if (!isWearingAnyPowerArmor(player)) {
            bossBarManager.removeBossBar(player);
            return;
        }
        Map<String, float[]> desiredEffects = getDesiredArmorEffects(player);
        List<String> enabledList = getEnabledEffects(player);
        List<String> finalEffectsKey = new ArrayList<>();
        
        // 篩選出同時存在於「期望效果」和「啟用列表」中的效果
        for (String effect : desiredEffects.keySet()) {
            if (enabledList.contains(effect)) finalEffectsKey.add(effect);
        }

        if (debug) plugin.sendDebug("§e玩家: " + player.getName() + " §b期望效果: " + finalEffectsKey);

        int totalCost = 0;
        for (String effectId : finalEffectsKey) {
            ArmorEffect effect = effectRegistry.get(effectId);
            if (effect != null && desiredEffects.containsKey(effectId)) {
                float[] specs = desiredEffects.get(effectId);
                float finalValue = calculateFinalValue(player, effectId, specs);
                totalCost += effect.calculateCost(player, finalValue);
            }
        }
        if (debug) plugin.sendDebug("§c總消耗: " + totalCost);

        long currentEnergy = ArmorEnergyManager.getEnergy(player, energyKey);
        if (debug) plugin.sendDebug("§a當前能量: " + currentEnergy);
        List<String> playerEffectsKey = getPlayerEffectsKey(player);
        if (debug) plugin.sendDebug("§d玩家當前效果: " + playerEffectsKey);

        if (currentEnergy >= totalCost) {
            if (!playerEffectsKey.equals(finalEffectsKey)) {
                if (debug) plugin.sendDebug("§6效果不匹配 正在同步...");
                List<String> toRemove = new ArrayList<>(playerEffectsKey);
                toRemove.removeAll(finalEffectsKey);
                List<String> toAdd = new ArrayList<>(finalEffectsKey);
                toAdd.removeAll(playerEffectsKey);

                for (String effectKey : toRemove) {
                    ArmorEffect effect = effectRegistry.get(effectKey);
                    if (effect != null) {
                        effect.remove(player);
                        if (debug) plugin.sendDebug("§c移除效果: " + effectKey);
                    }
                }

                if (debug) {
                    for (String effectKey : toAdd) {
                        String effectName = lang.get("ArmorSystem.Effects." + effectKey);
                        String message = lang.get("ArmorSystem.Message.EnabledFeature", effectName);
                        plugin.sendMessage(player, "Armor", message);
                    }
                }

                player.getPersistentDataContainer().set(effectList, PersistentDataType.LIST.strings(), finalEffectsKey);
                playerEffectsKey = finalEffectsKey;
                if (debug) plugin.sendDebug("§a已同步效果: " + playerEffectsKey);
            }
            if (totalCost > 0) {
                ArmorEnergyManager.removeEnergy(player, energyKey, totalCost);
                if (debug) plugin.sendDebug("§c消耗能量: " + totalCost);
            }
        } else {
            if (debug) plugin.sendDebug("§4能量不足!");
            if (!playerEffectsKey.isEmpty()) {
                if (debug) plugin.sendDebug("§c清空所有效果...");
                
                String message = lang.get("ArmorSystem.Message.LowEnergy");
                plugin.sendMessage(player, "Armor", message);

                for (String effectKey : playerEffectsKey) {
                    ArmorEffect effect = effectRegistry.get(effectKey);
                    if (effect != null) {
                        effect.remove(player);
                        if (debug) plugin.sendDebug("§c移除效果: " + effectKey);
                    }
                }

                playerEffectsKey = new ArrayList<>();
                player.getPersistentDataContainer().set(effectList, PersistentDataType.LIST.strings(), playerEffectsKey);
                if (debug) plugin.sendDebug("§c所有效果已清空");
            }
        }
        
        // 應用效果循環
        for (String effectKey : playerEffectsKey) {
            ArmorEffect effect = effectRegistry.get(effectKey);
            if (effect != null && desiredEffects.containsKey(effectKey)) {
                float[] specs = desiredEffects.get(effectKey);
                float finalValue = calculateFinalValue(player, effectKey, specs);
                
                effect.apply(player, finalValue);
                if (debug) plugin.sendDebug("§a應用效果: " + effectKey); 
            }
        }

        bossBarManager.update(player, currentEnergy, totalCost);
    }

    public boolean isWearingAnyPowerArmor(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ItemStack item : armor) {
            for (String setId : armorSetList) {
                if (isArmorSet(item, setId)) return true;
            }
        }
        return false;
    }

    private float calculateFinalValue(Player player, String effectId, float[] specs) {
        float defaultVal = specs[0];
        float min = specs[1];
        float max = specs[2];
        boolean allowCustom = specs[3] == 1.0;
        
        float finalValue = defaultVal;

        if (allowCustom) {
            PersistentDataContainer pdc = player.getPersistentDataContainer();
            // 根據 effectId 決定讀取哪個 Key
            if ("flight".equals(effectId) && pdc.has(flySpeedValueKey, PersistentDataType.FLOAT)) {
                finalValue = pdc.get(flySpeedValueKey, PersistentDataType.FLOAT);
            } else if ("speed".equals(effectId) && pdc.has(speedValueKey, PersistentDataType.FLOAT)) {
                finalValue = pdc.get(speedValueKey, PersistentDataType.FLOAT);
            } else if ("jump".equals(effectId) && pdc.has(jumpValueKey, PersistentDataType.FLOAT)) {
                finalValue = pdc.get(jumpValueKey, PersistentDataType.FLOAT);
            }
        }

        // 區間限制 (Clamp)
        if (finalValue < min) finalValue = min;
        if (finalValue > max) finalValue = max;

        return finalValue;
    }
    
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        bossBarManager.removeBossBar(event.getPlayer());
    }

}