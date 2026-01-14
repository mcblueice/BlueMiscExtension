package net.mcblueice.bluemiscextension.features.PlaceholderAPI.Placeholder;

import java.util.UUID;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.AbsorptionScale.AbsorptionScale;
import net.mcblueice.bluemiscextension.listeners.PlayerDataListener;
//import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;
import net.mcblueice.bluemiscextension.utils.MessageUtil;
import net.mcblueice.bluemiscextension.utils.ServerUtil;

public final class BlueMiscPlaceholder extends PlaceholderExpansion {
    private final BlueMiscExtension plugin;
    //private final ConfigManager lang;
    private final DatabaseUtil databaseUtil;

    public BlueMiscPlaceholder(BlueMiscExtension plugin) {
        this.plugin = plugin;
        //this.lang = plugin.getLanguageManager();
        this.databaseUtil = plugin.getDatabaseUtil();
    }

    @Override
    public String getIdentifier() { return "bluemiscextension"; }

    @Override
    public String getAuthor() { return String.join(", ", plugin.getPluginMeta().getAuthors()); }

    @Override
    public String getVersion() { return plugin.getPluginMeta().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public boolean canRegister() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, String rawParams) {
        if (player == null) return "";
        if (rawParams == null || rawParams.isEmpty()) return "";

        UUID uuid = player.getUniqueId();
        String[] parts = rawParams.split("_", 2);
        String key = parts[0].toLowerCase();

        switch (key) {
            case "armorhidden":
                return databaseUtil.getArmorHiddenState(uuid) ? "true" : "false";
            case "ip":
                return databaseUtil.getIp(uuid);
            case "hostname":
                return databaseUtil.getHostname(uuid);
            case "absorption":
                return String.valueOf(AbsorptionScale.getAbsorption(uuid));
            case "maxabsorption":
                return String.valueOf(AbsorptionScale.getMaxAbsorption(uuid));
            case "minimessage":
                if (parts.length < 2) return "";
                String content = PlaceholderAPI.setBracketPlaceholders(player, parts[1]);
                return MessageUtil.legacyToMiniMessage(content);
            case "tps":
                double tps = PlayerDataListener.playerTPSCache.getOrDefault(uuid, 0.0);
                return ServerUtil.formatTPS(tps);
            default:
                 return "";
        }
    }
}
