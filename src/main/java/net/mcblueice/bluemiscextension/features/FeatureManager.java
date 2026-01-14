package net.mcblueice.bluemiscextension.features;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.bukkit.Bukkit;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.ArmorHide.ArmorHide;
import net.mcblueice.bluemiscextension.features.ShulkerBox.ShulkerBox;
import net.mcblueice.bluemiscextension.features.DamageIndicatorLimiter.DamageIndicatorLimiter;
import net.mcblueice.bluemiscextension.features.AbsorptionScale.AbsorptionScale;
import net.mcblueice.bluemiscextension.features.Elevator.Elevator;
import net.mcblueice.bluemiscextension.features.LightBlock.LightBlock;
import net.mcblueice.bluemiscextension.features.PlaceholderAPI.PlaceholderFeature;
import net.mcblueice.bluemiscextension.features.VirtualWorkbench.VirtualWorkbench;
import net.mcblueice.bluemiscextension.features.BlueTools.BlueTools;
import net.mcblueice.bluemiscextension.utils.ConfigManager;

public class FeatureManager {
    private final BlueMiscExtension plugin;
    private final ConfigManager lang;
    private final List<Feature> activeFeatures = new ArrayList<>();

    public FeatureManager(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }

    public void reload() {
        unloadAll();

        loadFeature("DamageIndicatorLimiter", "ProtocolLib", DamageIndicatorLimiter::new);
        loadFeature("AbsorptionScale", "ProtocolLib", AbsorptionScale::new);
        loadFeature("ArmorHide", "ProtocolLib", ArmorHide::new);
        loadFeature("ShulkerBox", ShulkerBox::new);
        loadFeature("Elevator", Elevator::new);
        loadFeature("LightBlock", LightBlock::new);
        loadFeature("VirtualWorkbench", VirtualWorkbench::new);
        loadFeature("PlaceholderAPI", "PlaceholderAPI", PlaceholderFeature::new);
        loadFeature("BlueTools", BlueTools::new);
    }

    public void unloadAll() {
        for (Feature feature : activeFeatures) {
            feature.unregister();
        }
        activeFeatures.clear();
    }

    public boolean isFeatureEnabled(Class<? extends Feature> featureClass) {
        return getFeature(featureClass) != null;
    }

    public <T extends Feature> T getFeature(Class<T> featureClass) {
        for (Feature feature : activeFeatures) {
            if (featureClass.isInstance(feature)) return featureClass.cast(feature);
        }
        return null;
    }

    private <T extends Feature> T loadFeature(String name, Function<BlueMiscExtension, T> factory) {
        return loadFeature(name, (String[]) null, factory);
    }

    private <T extends Feature> T loadFeature(String name, String dependency, Function<BlueMiscExtension, T> factory) {
        return loadFeature(name, new String[]{dependency}, factory);
    }

    private <T extends Feature> T loadFeature(String name, String[] dependencies, Function<BlueMiscExtension, T> factory) {
        boolean enabled = plugin.getConfig().getBoolean("Features." + name + ".enable", false);
        String displayName = lang.get("FeatureName." + name);

        if (enabled) {
            List<String> missingDeps = new ArrayList<>();
            if (dependencies != null) {
                for (String dep : dependencies) {
                    if (Bukkit.getPluginManager().getPlugin(dep) == null) missingDeps.add(dep);
                }
            }

            if (missingDeps.isEmpty()) {
                String depMsg = "§a";
                if (dependencies != null && dependencies.length > 0) {
                    depMsg += lang.get("FeatureManager.Dependency.Enabled", String.join(", ", dependencies));
                }
                Bukkit.getConsoleSender().sendMessage("[BlueMiscExtension]" + lang.get("FeatureManager.Enabled", depMsg, displayName));

                T feature = factory.apply(plugin);
                feature.register();
                activeFeatures.add(feature);
                return feature;
            } else {
                String depMsg = "§c" + lang.get("FeatureManager.Dependency.Missing", String.join(", ", missingDeps));
                Bukkit.getConsoleSender().sendMessage("[BlueMiscExtension]" + lang.get("FeatureManager.Disabled", depMsg, displayName));
            }
        } else {
            Bukkit.getConsoleSender().sendMessage("[BlueMiscExtension]" + lang.get("FeatureManager.Closed", displayName));
        }
        return null;
    }
}
