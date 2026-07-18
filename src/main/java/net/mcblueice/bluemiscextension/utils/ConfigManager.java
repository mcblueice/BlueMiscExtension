package net.mcblueice.bluemiscextension.utils;

import java.io.File;
import java.util.*;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.mcblueice.bluelib.utils.TextUtil;

public class ConfigManager {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private final JavaPlugin plugin;
    private final Map<String, String> langData = new HashMap<>();
    private File langFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        langFile = new File(plugin.getDataFolder(), "lang.yml");
        if (!langFile.exists()) plugin.saveResource("lang.yml", false);
        YamlConfiguration langYml = YamlConfiguration.loadConfiguration(langFile);
        langData.clear();
        for (String key : langYml.getKeys(true)) {
            String value = langYml.getString(key);
            if (value != null) langData.put(key, value);
        }
    }

    public void reload() {
        load();
    }

    public boolean has(String key) {
        return langData.containsKey(key);
    }

    public List<String> getSectionKeys(String sectionPath) {
        String prefix = sectionPath.endsWith(".") ? sectionPath : sectionPath + ".";
        Set<String> keys = new LinkedHashSet<>();

        for (String key : langData.keySet()) {
            if (!key.startsWith(prefix)) continue;
            String remaining = key.substring(prefix.length());
            if (remaining.isBlank()) continue;
            int dot = remaining.indexOf('.');
            keys.add(dot == -1 ? remaining : remaining.substring(0, dot));
        }

        return new ArrayList<>(keys);
    }

    public String get(String key) {
        return LEGACY_SERIALIZER.serialize(getComponent(key));
    }
    
    public String get(String key, Object... args) {
        return LEGACY_SERIALIZER.serialize(getComponent(key, args));
    }
    
    public List<String> getList(String key, Object... args) {
        List<String> result = new ArrayList<>();
        for (Component component : getComponentList(key, args)) {
            if (component != null) result.add(LEGACY_SERIALIZER.serialize(component));
        }
        return result;
    }

    public Component getComponent(String key) {
        return TextUtil.parse(getRawText(key), true, true);
    }
    
    public Component getComponent(String key, Object... args) {
        Component result = getComponent(key);
        return applyComponentPlaceholders(result, args);
    }
    
    public List<Component> getComponentList(String key, Object... args) {
        String text = getRawText(key);
        List<Component> list = new ArrayList<>();
        if (text.equals(key)) {
            list.add(Component.text(key));
            return list;
        }

        String normalized = text.replace("\\n", "\n");
        for (String line : normalized.split("\\n", -1)) {
            Component comp = TextUtil.parse(line, true, true).decoration(TextDecoration.ITALIC, false);
            list.add(applyComponentPlaceholders(comp, args));
        }
        return list;
    }

    private String getRawText(String key) {
        String text = langData.get(key);
        if (text == null) {
            plugin.getLogger().warning("Language key '" + key + "' not found in lang.yml");
            return key;
        }
        return text;
    }

    private Component applyComponentPlaceholders(Component target, Object... args) {
        if (args == null || args.length == 0) return target;

        for (int i = 0; i < args.length; i++) {
            String placeholder = "%{" + (i + 1) + "}";
            Object arg = args[i];
            Component replacementComp;

            if (arg instanceof Component) {
                replacementComp = (Component) arg;
            } else {
                replacementComp = TextUtil.parse(String.valueOf(arg), true, false);
            }

            target = target.replaceText(TextReplacementConfig.builder().matchLiteral(placeholder).replacement(replacementComp).build());
        }
        return target;
    }
}