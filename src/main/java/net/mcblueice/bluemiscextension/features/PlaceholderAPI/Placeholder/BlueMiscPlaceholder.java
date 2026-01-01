package net.mcblueice.bluemiscextension.features.PlaceholderAPI.Placeholder;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.AbsorptionScale.AbsorptionScale;
//import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;

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

        switch (rawParams.toLowerCase()) {
            case "armorhidden":
                return databaseUtil.getArmorHiddenState(player.getUniqueId()) ? "true" : "false";
            case "ip":
                return databaseUtil.getIp(player.getUniqueId());
            case "hostname":
                return databaseUtil.getHostname(player.getUniqueId());
            case "absorption":
                return String.valueOf(AbsorptionScale.getAbsorption(player.getUniqueId()));
            case "maxabsorption":
                return String.valueOf(AbsorptionScale.getMaxAbsorption(player.getUniqueId()));
        }
        return "";
    }
}
