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
import net.mcblueice.bluelib.utils.TextUtil;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil.PlayerData;

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
        PlayerData playerData = databaseUtil.getPlayerData(uuid);
        String[] parts = rawParams.split("_", 2);
        String key = parts[0].toLowerCase();

        switch (key) {
            case "armorhidden":
                return playerData.hiddenArmor() ? "true" : "false";
            case "phantomspawn":
                return playerData.phantomSpawn() ? "true" : "false";
            case "hasnickname":
                return playerData.nickname().isEmpty() ? "false" : "true";
            case "nickname":
                return playerData.nickname();
            case "displayname":
                return playerData.getDisplayName();
            case "ip":
                return playerData.ip();
            case "hostname":
                return playerData.hostname();
            case "absorption":
                return String.valueOf(AbsorptionScale.getAbsorption(uuid));
            case "maxabsorption":
                return String.valueOf(AbsorptionScale.getMaxAbsorption(uuid));
            case "minimessage":
                if (parts.length < 2) return "";
                String content = PlaceholderAPI.setBracketPlaceholders(player, parts[1]);
                return TextUtil.parseToString(content);
            case "tps":
                double tps = PlayerDataListener.playerTPSCache.getOrDefault(uuid, 0.0);
                tps = Math.min(20.0, tps);
                String tpsStr = new java.text.DecimalFormat("#.##").format(tps);
                if (tps >= 19.8) return "§a" + tpsStr;
                if (tps >= 18.0) return "§2" + tpsStr;
                if (tps >= 15.0) return "§e" + tpsStr;
                if (tps >= 10.0) return "§c" + tpsStr;
                return "§4" + tpsStr;
            default:
                 return "";
        }
    }
}
