package net.mcblueice.bluemiscextension.features.PlayerNick;

import org.bukkit.entity.Player;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.features.Feature;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;
import net.mcblueice.bluelib.utils.TextUtil;

public class PlayerNick implements Feature {
    private final BlueMiscExtension plugin;
    private final DatabaseUtil databaseUtil;

    public PlayerNick(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.databaseUtil = plugin.getDatabaseUtil();
    }

    @Override
    public void register() {
        plugin.getCommandManager().register(new PlayerNickCommand(plugin, this), "bluemiscextension.nick", new String[]{"nick"});
    }

    @Override
    public void unregister() {
        plugin.getCommandManager().unregister("nick");
    }

    @Override
    public void onPlayerDataLoaded(Player player) {
        if (!player.hasPermission("bluemiscextension.nick")) {
            setPlayerNick(player, "");
        } else {
            String displayName = databaseUtil.getPlayerData(player.getUniqueId()).getDisplayName();
            player.displayName(TextUtil.parse(displayName));
        }
    }

    @Override
    public void onPlayerDataUnload(Player player) {
        if (!player.hasPermission("bluemiscextension.nick")) databaseUtil.setNickname(player.getUniqueId(), "");
    }

    public void setPlayerNick(Player player, String nick) {
        if (nick == null) nick = "";
        databaseUtil.setNickname(player.getUniqueId(), nick);
        String displayName = databaseUtil.getPlayerData(player.getUniqueId()).getDisplayName();
        player.displayName(TextUtil.parse(displayName));
    }
}
