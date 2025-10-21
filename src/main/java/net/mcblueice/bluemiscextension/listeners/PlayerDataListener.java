package net.mcblueice.bluemiscextension.listeners;

import java.sql.SQLException;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;
import net.mcblueice.bluemiscextension.utils.TaskScheduler;

public class PlayerDataListener implements Listener {
    private final BlueMiscExtension plugin;

    public PlayerDataListener(BlueMiscExtension plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        DatabaseUtil databaseUtil = plugin.getDatabaseUtil();
        if (databaseUtil == null) return;

        Player player = event.getPlayer();
        TaskScheduler.runAsync(plugin, () -> {
            try {
                databaseUtil.upsertPlayerData(player.getUniqueId(), player.getName());
            } catch (SQLException e) {
                plugin.getLogger().severe("同步玩家資料至資料庫時發生錯誤：" + e.getMessage());
            }
        });
    }
}
