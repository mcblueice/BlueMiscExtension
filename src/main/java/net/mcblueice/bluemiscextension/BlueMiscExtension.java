package net.mcblueice.bluemiscextension;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.mcblueice.bluemiscextension.listeners.PlayerDataListener;
import net.mcblueice.bluemiscextension.utils.ConfigManager;
import net.mcblueice.bluemiscextension.utils.DatabaseUtil;
import net.mcblueice.bluemiscextension.utils.ServerUtil;
import net.mcblueice.bluemiscextension.utils.TaskScheduler;
import net.mcblueice.bluemiscextension.commands.Commands;
import net.mcblueice.bluemiscextension.features.FeatureManager;

public class BlueMiscExtension extends JavaPlugin {
    private static BlueMiscExtension instance;
    private Logger logger;
    private FeatureManager featureManager;
    private DatabaseUtil databaseUtil;
    private ServerUtil serverUtil;
    private ConfigManager lang;
    public final UUID CONSOLE_UUID = new UUID(0L, 0L);
    public final Set<UUID> debugModePlayers = ConcurrentHashMap.newKeySet();

    public BlueMiscExtension() {
    }

    public static BlueMiscExtension getInstance() { return instance; }

    @Override
    public void onEnable() {
        if (instance != null) throw new IllegalStateException("Plugin is already initialized!");
        instance = this;
        logger = getLogger();
        saveDefaultConfig();
        this.lang = new ConfigManager(this);

        serverUtil = new ServerUtil();

        databaseUtil = new DatabaseUtil(this);
        try {
            databaseUtil.connectAndInitPlayerTable();
        } catch (SQLException e) {
            logger.severe("初始化資料庫時發生錯誤：" + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        featureManager = new FeatureManager(this);
        featureManager.reload();

        getCommand("bluemiscextension").setExecutor(new Commands(this));

        getServer().getPluginManager().registerEvents(new PlayerDataListener(this), this);


        logger.info("BlueMiscExtension 已啟動");
    }

    @Override
    public void onDisable() {
        logger.info("BlueMiscExtension 已卸載");

        if (featureManager != null) featureManager.unloadAll();

        if (databaseUtil != null) {
            Bukkit.getConsoleSender().sendMessage(lang.get("Prefix") + "§eDEBUG: §7" + "伺服器關閉 開始保存玩家資料");
            HashSet<UUID> playerUUIDs = new HashSet<>();
            for (Player player : getServer().getOnlinePlayers()) {
                playerUUIDs.add(player.getUniqueId());
            }
            databaseUtil.savePlayerData(playerUUIDs, true);
            databaseUtil.close();
        }
    }

    public boolean toggleDebugMode(UUID uuid) {
        if (uuid == null) return false;
        if (debugModePlayers.contains(uuid)) {
            debugModePlayers.remove(uuid);
            return false;
        } else {
            debugModePlayers.add(uuid);
            return true;
        }
    }

    public void sendDebug(String message) {
        if (debugModePlayers.isEmpty()) return;

        // console
        if (debugModePlayers.contains(CONSOLE_UUID)) sendMessage("§eDEBUG: §7" + message);
        // player
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (debugModePlayers.contains(player.getUniqueId())) sendMessage(player, "§eDEBUG: §7" + message);
        }
    }

    public void sendMessage(String message) {
        if (message == null) return;
        Bukkit.getConsoleSender().sendMessage(lang.get("Prefix") + message);
    }

    public void sendMessage(Player player, String message) {
        if (player == null || message == null) return;
        TaskScheduler.runTask(player, this, () -> player.sendMessage(lang.get("Prefix") + message));
    }

    public DatabaseUtil getDatabaseUtil() { return databaseUtil; }
    public ConfigManager getLanguageManager() { return lang; }
    public FeatureManager getFeatureManager() { return featureManager; }
    public ServerUtil getServerUtil() { return serverUtil; }
}
