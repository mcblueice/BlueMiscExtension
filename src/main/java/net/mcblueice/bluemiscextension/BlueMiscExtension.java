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
import net.mcblueice.bluelib.utils.TextUtil;
import net.mcblueice.bluemiscextension.commands.AliasCommand;
import net.mcblueice.bluemiscextension.commands.CommandManager;
import net.mcblueice.bluemiscextension.features.FeatureManager;

public class BlueMiscExtension extends JavaPlugin {
    private static BlueMiscExtension instance;
    private Logger logger;
    private FeatureManager featureManager;
    private DatabaseUtil databaseUtil;
    private ConfigManager lang;
    private CommandManager commandManager;
    private AliasCommand aliasCommand;
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

        databaseUtil = new DatabaseUtil(this);
        try {
            databaseUtil.connectAndInitPlayerTable();
        } catch (SQLException e) {
            logger.severe("初始化資料庫時發生錯誤：" + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        featureManager = new FeatureManager(this);
        commandManager = new CommandManager(this);
        getCommand("bluemiscextension").setExecutor(commandManager);

        featureManager.reload();
        aliasCommand = new AliasCommand(this);
        aliasCommand.registerAll();
        getServer().getPluginManager().registerEvents(new PlayerDataListener(this), this);

        logger.info("BlueMiscExtension 已啟動");
    }

    @Override
    public void onDisable() {
        logger.info("BlueMiscExtension 已卸載");

        if (featureManager != null) featureManager.unloadAll();
        if (aliasCommand != null) aliasCommand.unregisterAll();

        if (databaseUtil != null) {
            sendMessage("伺服器關閉 開始保存玩家資料");
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
        sendDebug("Default", message);
    }
    public void sendDebug(String prefixNode, String message) {
        if (debugModePlayers.isEmpty()) return;
        String debugPrefix = "&eDEBUG: &7"; 

        // console
        if (debugModePlayers.contains(CONSOLE_UUID)) sendMessage(prefixNode, debugPrefix + message);

        // player
        for (UUID uuid : debugModePlayers) {
            if (uuid.equals(CONSOLE_UUID)) continue;
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) sendMessage(player, prefixNode, debugPrefix + message);
        }
    }

    public void sendMessage(String message) {
        sendMessage("Default", message);
    }

    public void sendMessage(String prefixNode, String message) {
        if (message == null) return;

        String prefix = prefixNode.equals("none") ? "" : lang.get("Prefix." + prefixNode);
        Bukkit.getConsoleSender().sendMessage(TextUtil.parse(prefix + message));
    }

    public void sendMessage(Player player, String message) {
        sendMessage(player, "Default", message);
    }
    
    public void sendMessage(Player player, String prefixNode, String message) {
        if (player == null || message == null) return;

        String prefix = prefixNode.equals("none") ? "" : lang.get("Prefix." + prefixNode);
        player.sendMessage(TextUtil.parse(prefix + message));
    }

    public DatabaseUtil getDatabaseUtil() { return databaseUtil; }
    public ConfigManager getLanguageManager() { return lang; }
    public FeatureManager getFeatureManager() { return featureManager; }
    public CommandManager getCommandManager() { return commandManager; }
    public AliasCommand getAliasCommand() { return aliasCommand; }
}
