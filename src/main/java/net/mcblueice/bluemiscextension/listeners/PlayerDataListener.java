package net.mcblueice.bluemiscextension.listeners;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.mcblueice.bluemiscextension.BlueMiscExtension;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;
import net.mcblueice.bluemiscextension.utils.ServerUtil;
import net.mcblueice.bluemiscextension.utils.TaskScheduler;

public class PlayerDataListener implements Listener {
    private final BlueMiscExtension plugin;
    private final DatabaseUtil databaseUtil;
    private final boolean debug;

    private final Map<UUID, TaskScheduler.RepeatingTaskHandler> playerTasks = new ConcurrentHashMap<>();
    private final Map<UUID, String[]> loginData = new ConcurrentHashMap<>();
    public static final Map<UUID, Double> playerTPSCache = new ConcurrentHashMap<>();
    public static final Map<Integer, Player> playerIDCache = new ConcurrentHashMap<>();

    public PlayerDataListener(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.databaseUtil = plugin.getDatabaseUtil();
        this.debug = plugin.getConfig().getBoolean("Database.debug", false);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            playerIDCache.put(player.getEntityId(), player);
        }
    }

    @EventHandler
    public void onPlayerLogin(AsyncPlayerPreLoginEvent event) {
        String playerName = event.getName();
        UUID playerUUID = event.getUniqueId();
        String hostname = event.getHostname();
        String ip = event.getAddress().getHostAddress();
        
        loginData.put(playerUUID, new String[]{hostname, ip});
        plugin.sendMessage("§e玩家 §b" + playerName + " §e從 §a" + hostname + " §e(IP: §9" + ip + "§e) 登入伺服器");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        TaskScheduler.runAsync(plugin, () -> {
            try {
                databaseUtil.upsertPlayerData(uuid, player.getName());
                databaseUtil.loadAndCreateCache(uuid).thenRun(() -> {
                    String[] data = loginData.remove(uuid);
                    if (data != null) {
                        databaseUtil.setHostname(uuid, (data[0] != null) ? data[0] : "UnknownHostname");
                        databaseUtil.setIpAddress(uuid, (data[1] != null) ? data[1] : "UnknownIp");
                    }
                });
            } catch (SQLException e) {
                plugin.getLogger().warning("同步玩家資料至資料庫時發生錯誤：" + e.getMessage());
                if (debug) plugin.sendDebug("同步玩家資料至資料庫時發生錯誤：" + e.getMessage());
            }
        });

        if (plugin.getConfig().getBoolean("Database.CleanPlayerAttributes", true)) cleanAttributes(player);
        playerIDCache.put(event.getPlayer().getEntityId(), event.getPlayer());
        playerTasks.put(uuid,
                        TaskScheduler.runPlayerRepeatingTask(player, plugin, () -> {
                            Double tps = ServerUtil.isFolia ? ServerUtil.getRegionTPS(player.getLocation()) : ServerUtil.getTPS();
                            if (debug) plugin.sendDebug("更新cache中的TPS數據 for 玩家: " + player.getName() + "TPS: " + tps);
                            playerTPSCache.put(uuid, tps);
                        }, 20L, 20L));

    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        DatabaseUtil databaseUtil = plugin.getDatabaseUtil();
        if (databaseUtil == null) return;

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        databaseUtil.savePlayerDataAsync(uuid, true);

        TaskScheduler.RepeatingTaskHandler task = playerTasks.remove(uuid);
        if (task != null) task.cancel();
        playerIDCache.remove(player.getEntityId());
        playerTPSCache.remove(uuid);
    }

    private void cleanAttributes(Player player) {
        if (debug) plugin.sendDebug("§e清理玩家 §6" + player.getName() + " §e的屬性基礎值");
        for (Attribute att : Registry.ATTRIBUTE) {
            AttributeInstance instance = player.getAttribute(att);
            if (instance == null) continue;
            double baseValue = instance.getBaseValue();
            double roundedValue = Math.round(baseValue * 1000000d) / 1000000d;
            if (Double.compare(baseValue, roundedValue) != 0) {
                try {
                    instance.setBaseValue(roundedValue);
                } catch (IllegalArgumentException e) {
                    if (debug) plugin.sendDebug("§c無法設置屬性 " + att.getKey().getKey());
                }
            }
        }
    }
}
