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
import net.mcblueice.bluemiscextension.utils.HuskSyncAPIHook;
import net.kyori.adventure.text.Component;
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
    private HuskSyncAPIHook huskSyncAPIHook;
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

        if (Bukkit.getPluginManager().getPlugin("HuskSync") != null) huskSyncAPIHook = new HuskSyncAPIHook();
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
        sendDebug("Default", TextUtil.parse(message, true, true));
    }
    public void sendDebug(String prefixNode, String message) {
        sendDebug(prefixNode, TextUtil.parse(message, true, true));
    }
    public void sendDebug(Component message) {
        sendDebug("Default", message);
    }
    public void sendDebug(String prefixNode, Component message) {
        if (debugModePlayers.isEmpty()) return;

        Component debugPrefix = TextUtil.parse("&eDEBUG: &7", true, true);
        Component debugMessage = debugPrefix.append(message);

        // console
        if (debugModePlayers.contains(CONSOLE_UUID)) sendMessage(prefixNode, debugMessage);

        // player
        for (UUID uuid : debugModePlayers) {
            if (uuid.equals(CONSOLE_UUID)) continue;
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) sendMessage(player, prefixNode, debugMessage);
        }
    }

    public void sendMessage(String message) {
        sendMessage("Default", TextUtil.parse(message, true, true));
    }
    public void sendMessage(String prefixNode, String message) {
        sendMessage(prefixNode, TextUtil.parse(message, true, true));
    }
    public void sendMessage(Player player, String message) {
        sendMessage(player, "Default", TextUtil.parse(message, true, true));
    }
    public void sendMessage(Player player, String prefixNode, String message) {
        sendMessage(player, prefixNode, TextUtil.parse(message, true, true));
    }

    public void sendMessage(Component message) {
        sendMessage("Default", message);
    }
    public void sendMessage(String prefixNode, Component message) {
        if (message == null) return;
        Component prefixComp = prefixNode.equals("none") ? Component.empty() : TextUtil.parse(lang.get("Prefix." + prefixNode), true, true);  
        Bukkit.getConsoleSender().sendMessage(prefixComp.append(message));
    }
    public void sendMessage(Player player, Component message) {
        sendMessage(player, "Default", message);
    }
    public void sendMessage(Player player, String prefixNode, Component message) {
        if (player == null || message == null) return;
        Component prefixComp = prefixNode.equals("none") ? Component.empty() : TextUtil.parse(lang.get("Prefix." + prefixNode), true, true);
        player.sendMessage(prefixComp.append(message));
    }

    public DatabaseUtil getDatabaseUtil() { return databaseUtil; }
    public ConfigManager getLanguageManager() { return lang; }
    public FeatureManager getFeatureManager() { return featureManager; }
    public CommandManager getCommandManager() { return commandManager; }
    public HuskSyncAPIHook getHuskSyncAPIHook() { return huskSyncAPIHook; }
    public AliasCommand getAliasCommand() { return aliasCommand; }
}
