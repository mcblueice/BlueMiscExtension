package net.mcblueice.bluemiscextension.listeners;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;
import net.mcblueice.bluemiscextension.utils.TaskScheduler;

public class PlayerDataListener implements Listener {
    private final BlueMiscExtension plugin;
    private final Map<UUID, String[]> loginData = new ConcurrentHashMap<>();

    public PlayerDataListener(BlueMiscExtension plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        String playerName = event.getPlayer().getName();
        String hostname = event.getHostname();
        String ip = event.getAddress().getHostAddress();
        
        loginData.put(event.getPlayer().getUniqueId(), new String[]{hostname, ip});
        Bukkit.getConsoleSender().sendMessage("§7[§b登入§7]§e玩家 §b" + playerName + " §e從 §a" + hostname + " §e(IP: §9" + ip + "§e) 登入伺服器");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        DatabaseUtil databaseUtil = plugin.getDatabaseUtil();
        if (databaseUtil == null) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        TaskScheduler.runAsync(plugin, () -> {
            try {
                databaseUtil.upsertPlayerData(uuid, player.getName());
                databaseUtil.loadAndCreateCache(uuid).thenRun(() -> {
                    String[] data = loginData.getOrDefault(uuid, new String[]{"UnknownHostname", "UnknownIp"});
                    databaseUtil.setHostname(uuid, (data[0] != null) ? data[0] : "UnknownHostname");
                    databaseUtil.setIpAddress(uuid, (data[1] != null) ? data[1] : "UnknownIp");
                });
            } catch (SQLException e) {
                plugin.getLogger().severe("同步玩家資料至資料庫時發生錯誤：" + e.getMessage());
                plugin.sendDebug("同步玩家資料至資料庫時發生錯誤：" + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        DatabaseUtil databaseUtil = plugin.getDatabaseUtil();
        if (databaseUtil == null) return;

        Player player = event.getPlayer();
        databaseUtil.savePlayerData(player.getUniqueId(), true);
        loginData.remove(player.getUniqueId());
    }
}
