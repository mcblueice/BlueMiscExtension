package net.mcblueice.bluemiscextension.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ConfigManager {
    private final JavaPlugin plugin;
    private Map<String, Object> langData = new HashMap<>();
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
            langData.put(key, langYml.get(key));
        }
    }

    public void reload() {
        load();
    }

    public String get(String key) {
        Object value = langData.get(key);
        String text = value != null ? value.toString() : key;
        return text.replace('&', '§');
    }

    public String get(String key, Object... args) {
        String text = get(key);
        if (text == null) return "";

        Pattern pattern = Pattern.compile("%\\{(\\d+)}");
        Matcher matcher = pattern.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1)) - 1;
            String replacement = (index >= 0 && index < args.length)
                    ? String.valueOf(args[index])
                    : matcher.group();

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public Component getComponent(String key) {
        String text = get(key);
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }
}
