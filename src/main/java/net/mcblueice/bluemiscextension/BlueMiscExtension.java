package net.mcblueice.bluemiscextension;

import java.sql.SQLException;
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
import net.mcblueice.bluemiscextension.utils.TaskScheduler;
import net.mcblueice.bluemiscextension.features.AbsorptionScale;
import net.mcblueice.bluemiscextension.features.DamageIndicatorLimiter;
import net.mcblueice.bluemiscextension.features.ShulkerBox.ShulkerBox;

public class BlueMiscExtension extends JavaPlugin {
    private static BlueMiscExtension instance;
    private Logger logger;
    private boolean enableDamageIndicatorLimiter;
    private boolean enableAbsorptionScale;
    private boolean enableShulkerBox;
    private DamageIndicatorLimiter damageIndicatorLimiter;
    private AbsorptionScale absorptionScale;
    private ShulkerBox shulkerBox;
    private DatabaseUtil databaseUtil;
    private ConfigManager lang;
    public final Set<UUID> debugModePlayers = ConcurrentHashMap.newKeySet();

    public BlueMiscExtension() {
    }

    public static BlueMiscExtension getInstance() { return instance; }

    @Override
    public void onEnable() {
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

        refreshFeatures();

        getCommand("bluemiscextension").setExecutor(new Commands(this));

        getServer().getPluginManager().registerEvents(new PlayerDataListener(this), this);

        logger.info("BlueMiscExtension 已啟動");
    }

    @Override
    public void onDisable() {
        logger.info("BlueMiscExtension 已卸載");
        unregisterFeatures();
        if (databaseUtil != null) { databaseUtil.close(); }
    }

    public void refreshFeatures() {
        unregisterFeatures();

        enableDamageIndicatorLimiter = getConfig().getBoolean("Features.DamageIndicatorLimiter.enable", false);
        enableAbsorptionScale = getConfig().getBoolean("Features.AbsorptionScale.enable", false);
        enableShulkerBox = getConfig().getBoolean("Features.ShulkerBox.enable", false);

        boolean hasProtocolLib = getServer().getPluginManager().getPlugin("ProtocolLib") != null;

        if (enableDamageIndicatorLimiter) {
            if (hasProtocolLib) {
                getServer().getConsoleSender().sendMessage("§r[BlueMiscExtension] §aProtocolLib 已啟用 已開啟 移除受傷愛心 功能!");
                damageIndicatorLimiter = new DamageIndicatorLimiter(this);
                damageIndicatorLimiter.register();
            } else {
                getServer().getConsoleSender().sendMessage("§r[BlueMiscExtension] §cProtocolLib 未啟用 已關閉 移除受傷愛心 功能!");
            }
        } else {
            getServer().getConsoleSender().sendMessage("§r[BlueMiscExtension] §c移除受傷愛心 功能已關閉");
        }

        if (enableAbsorptionScale) {
            if (hasProtocolLib) {
                getServer().getConsoleSender().sendMessage("§r[BlueMiscExtension] §aProtocolLib 已啟用 已開啟 吸收血量縮放 功能!");
                absorptionScale = new AbsorptionScale(this);
                absorptionScale.register();
            } else {
                getServer().getConsoleSender().sendMessage("§r[BlueMiscExtension] §cProtocolLib 未啟用 已關閉 吸收血量縮放 功能!");
            }
        } else {
            getServer().getConsoleSender().sendMessage("§r[BlueMiscExtension] §c吸收血量縮放 功能已關閉");
        }

        if (enableShulkerBox) {
            getServer().getConsoleSender().sendMessage("§r[BlueMiscExtension] §a已開啟 界伏盒 功能!");
            shulkerBox = new ShulkerBox(this);
            shulkerBox.register();
        } else {
            getServer().getConsoleSender().sendMessage("§r[BlueMiscExtension] §c界伏盒 功能已關閉");
        }
    }

    private void unregisterFeatures() {
        if (damageIndicatorLimiter != null) {
            damageIndicatorLimiter.unregister();
            damageIndicatorLimiter = null;
        }
        if (absorptionScale != null) {
            absorptionScale.unregister();
            absorptionScale = null;
        }
        if (shulkerBox != null) {
            shulkerBox.unregister();
            shulkerBox = null;
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
        if (debugModePlayers.contains(new UUID(0L, 0L))) sendMessage("§eDEBUG: §7" + message);
        // player
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (debugModePlayers.contains(player.getUniqueId())) sendMessage(player, "§eDEBUG: §7" + message);
        }
    }

    public void sendMessage(String message) {
        if (message == null) return;
        TaskScheduler.runTask(this, () -> Bukkit.getConsoleSender().sendMessage(lang.get("Prefix") + message));
    }

    public void sendMessage(Player player, String message) {
        if (player == null || message == null) return;
        TaskScheduler.runTask(player, this, () -> player.sendMessage(lang.get("Prefix") + message));
    }

    public DatabaseUtil getDatabaseUtil() { return databaseUtil; }
    public ConfigManager getLanguageManager() { return lang; }
}
