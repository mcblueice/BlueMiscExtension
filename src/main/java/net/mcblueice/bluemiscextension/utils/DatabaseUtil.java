package net.mcblueice.bluemiscextension.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import net.mcblueice.bluemiscextension.BlueMiscExtension;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import net.kyori.adventure.text.Component;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseUtil {
    private final BlueMiscExtension plugin;
    private HikariDataSource dataSource;
    private String dbType = "sqlite";
    private static final int MaxRetry = 39;
    private static final long RetryDelay = 10L;
    private final Set<UUID> dataLoadedPlayers = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Boolean> armorHiddenCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> hostnameCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> ipCache = new ConcurrentHashMap<>();

    public DatabaseUtil(BlueMiscExtension plugin) {
        this.plugin = plugin;
    }

    public void connectAndInitPlayerTable() throws SQLException {
        FileConfiguration config = plugin.getConfig();
        dbType = config.getString("Database.type", "sqlite").toLowerCase();
        
        HikariConfig hikariConfig = new HikariConfig();

        switch (dbType) {
            case "mysql":
                String host = config.getString("Database.MySQL.host", "localhost");
                int port = config.getInt("Database.MySQL.port", 3306);
                String database = config.getString("Database.MySQL.database", "database");
                String user = config.getString("Database.MySQL.user", "user");
                String password = config.getString("Database.MySQL.password", "password");
                
                hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
                hikariConfig.setUsername(user);
                hikariConfig.setPassword(password);
                hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
                hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
                hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
                hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
                hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
                hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
                hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
                hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
                hikariConfig.addDataSourceProperty("maintainTimeStats", "false");
                break;
            case "sqlite":
                File dbFile = new File(plugin.getDataFolder(), "database.db");
                hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
                hikariConfig.setDriverClassName("org.sqlite.JDBC");
                hikariConfig.setMaximumPoolSize(1);
                hikariConfig.addDataSourceProperty("journal_mode", "WAL");
                hikariConfig.addDataSourceProperty("synchronous", "NORMAL");
                break;
            default:
                throw new SQLException("Unknown database type: " + dbType);
        }

        dataSource = new HikariDataSource(hikariConfig);
        checkAndCreatePlayerTable();
    }

    private void checkAndCreatePlayerTable() throws SQLException {
        String createTable;
        switch (dbType) {
            case "mysql":
                createTable = "CREATE TABLE IF NOT EXISTS player_data (" +
                    "uuid CHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(32) NOT NULL, " +
                    "hidden_armor BOOLEAN NOT NULL DEFAULT 0, " +
                    "hostname VARCHAR(255), " +
                    "ip_address VARCHAR(45)" +
                    ")";
                break;
            case "sqlite":
                createTable = "CREATE TABLE IF NOT EXISTS player_data (" +
                    "uuid CHAR(36) PRIMARY KEY, " +
                    "player_name TEXT NOT NULL, " +
                    "hidden_armor BOOLEAN NOT NULL DEFAULT 0, " +
                    "hostname TEXT, " +
                    "ip_address TEXT" +
                    ")";
                break;
            default:
                throw new SQLException("Unknown database type: " + dbType);
        }
        
        try (Connection connection = dataSource.getConnection(); Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(createTable);
        }

        switch (dbType) {
            case "mysql":
                ensureColumnExists("player_data", "player_name", "VARCHAR(32) NOT NULL");
                ensureColumnExists("player_data", "hidden_armor", "BOOLEAN NOT NULL DEFAULT 0");
                ensureColumnExists("player_data", "hostname", "VARCHAR(255)");
                ensureColumnExists("player_data", "ip_address", "VARCHAR(45)");
                ensureColumnExists("player_data", "is_data_saved", "BOOLEAN NOT NULL DEFAULT 1");
                break;
            case "sqlite":
                ensureColumnExists("player_data", "player_name", "TEXT NOT NULL");
                ensureColumnExists("player_data", "hidden_armor", "BOOLEAN NOT NULL DEFAULT 0");
                ensureColumnExists("player_data", "hostname", "TEXT");
                ensureColumnExists("player_data", "ip_address", "TEXT");
                ensureColumnExists("player_data", "is_data_saved", "BOOLEAN NOT NULL DEFAULT 1");
                break;
            default:
                throw new SQLException("Unknown database type: " + dbType);
        }
    }

    private void ensureColumnExists(String table, String column, String definition) throws SQLException {
        boolean exists = false;
        try (Connection connection = dataSource.getConnection(); Statement stmt = connection.createStatement()) {
            switch (dbType) {
                case "mysql":
                    try (ResultSet rs = stmt.executeQuery("SHOW COLUMNS FROM " + table + " LIKE '" + column + "'")) {
                        if (rs.next()) exists = true;
                    }
                    break;
                case "sqlite":
                    try (ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + table + ")")) {
                        while (rs.next()) {
                            if (rs.getString("name").equalsIgnoreCase(column)) {
                                exists = true;
                                break;
                            }
                        }
                    }
                    break;
                default:
                    throw new SQLException("Unknown database type: " + dbType);
            }
            
            if (!exists) stmt.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }

    public synchronized void upsertPlayerData(UUID uuid, String playerName) throws SQLException {
        if (uuid == null || playerName == null || playerName.isEmpty()) return;
        if (dataSource == null || dataSource.isClosed()) connectAndInitPlayerTable();

        String trimmedName = playerName.length() > 32 ? playerName.substring(0, 32) : playerName;
        String sql;
        switch (dbType) {
            case "mysql":
                sql = "INSERT INTO player_data (uuid, player_name, hidden_armor, is_data_saved) VALUES (?, ?, 0, 1) " +
                    "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name)";
                break;
            case "sqlite":
                sql = "INSERT INTO player_data (uuid, player_name, hidden_armor, is_data_saved) VALUES (?, ?, 0, 1) " +
                    "ON CONFLICT(uuid) DO UPDATE SET player_name = excluded.player_name";
                break;
            default:
                throw new SQLException("Unknown database type: " + dbType);
        }

        try (Connection connection = dataSource.getConnection(); PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, trimmedName);
            ps.executeUpdate();
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    public CompletableFuture<Void> loadAndCreateCache(UUID uuid) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        attemptLoad(uuid, 0, future);
        return future;
    }

    private void attemptLoad(UUID uuid, int retryCount, CompletableFuture<Void> future) {
        TaskScheduler.runAsync(plugin, () -> {
            try {
                if (uuid == null || dataSource == null || dataSource.isClosed()) {
                    future.complete(null);
                    return;
                }

                // Check is_data_saved true
                try (Connection connection = dataSource.getConnection()) {
                    boolean isDataLocked = false;

                    String lockSql = "UPDATE player_data SET is_data_saved = 0 WHERE uuid = ? AND is_data_saved = 1";
                    try (PreparedStatement ps = connection.prepareStatement(lockSql)) {
                        ps.setString(1, uuid.toString());
                        int rowsAffected = ps.executeUpdate();
                        isDataLocked = rowsAffected > 0;
                    }

                    if (isDataLocked) {
                        String sql = "SELECT hidden_armor, hostname, ip_address FROM player_data WHERE uuid = ?";
                        try (PreparedStatement ps = connection.prepareStatement(sql)) {
                            ps.setString(1, uuid.toString());
                            try (ResultSet rs = ps.executeQuery()) {
                                if (rs.next()) {
                                    boolean dataHiddenArmor = rs.getBoolean("hidden_armor");
                                    armorHiddenCache.put(uuid, dataHiddenArmor);

                                    String dataHostname = rs.getString("hostname");
                                    hostnameCache.put(uuid, (dataHostname != null) ? dataHostname : "UnknownHostname");
            
                                    String dataIp = rs.getString("ip_address");
                                    ipCache.put(uuid, (dataIp != null) ? dataIp : "UnknownIp");
                                } else {
                                    armorHiddenCache.put(uuid, false);
                                    hostnameCache.put(uuid, "UnknownHostname");
                                    ipCache.put(uuid, "UnknownIp");
                                }
                            }
                        }

                        dataLoadedPlayers.add(uuid);
                        plugin.sendDebug("資料已載入並鎖定: " + uuid);

                        future.complete(null);
                    // Retry
                    } else {
                        plugin.sendDebug("資料尚未儲存: " + uuid + "，正在重試... (" + (retryCount+1) + ")");
                        if (retryCount+1 >= MaxRetry) {
                            TaskScheduler.runTask(plugin, () -> {
                                Player player = Bukkit.getPlayer(uuid);
                                if (player != null) player.kick(Component.text("§c無法同步玩家資料 請找管理員協助"));
                            });
                            plugin.getLogger().warning("Data sync timeout for " + uuid);
                            future.completeExceptionally(new RuntimeException("Data sync timeout"));
                        } else {
                            TaskScheduler.runTaskLater(plugin, () -> {
                                attemptLoad(uuid, retryCount + 1, future);
                            }, RetryDelay);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to load player data for " + uuid + ": " + e.getMessage());
                future.completeExceptionally(e);
            }
        });
    }

    public CompletableFuture<Void> saveAndRemoveCache(UUID uuid) {
        return CompletableFuture.runAsync(() -> saveAndRemoveCacheSync(uuid), runnable -> TaskScheduler.runAsync(plugin, runnable));
    }
    public void saveAndRemoveCacheSync(UUID uuid) {
        try {
            if (uuid == null || dataSource == null || dataSource.isClosed()) return;

            if (!dataLoadedPlayers.contains(uuid)) {
                plugin.sendDebug("玩家 " + uuid + " 資料未完全載入或已離線 跳過資料庫儲存並清除快取");
                armorHiddenCache.remove(uuid);
                hostnameCache.remove(uuid);
                ipCache.remove(uuid);
                return;
            }

            try (Connection connection = dataSource.getConnection()) {
                // 開啟事務
                connection.setAutoCommit(false);
                try {
                    //ArmorHidden
                    Boolean cachedArmor = armorHiddenCache.get(uuid);
                    if (cachedArmor != null) executeUpdate(connection, uuid, "hidden_armor", cachedArmor);

                    //Hostname
                    String cachedHostname = hostnameCache.get(uuid);
                    if (cachedHostname != null) executeUpdate(connection, uuid, "hostname", cachedHostname);
                    
                    //IP
                    String cachedIp = ipCache.get(uuid);
                    if (cachedIp != null) executeUpdate(connection, uuid, "ip_address", cachedIp);

                    // Set is_data_saved true
                    executeUpdate(connection, uuid, "is_data_saved", true);
                    
                    // 提交事務
                    connection.commit();

                    dataLoadedPlayers.remove(uuid);
                    armorHiddenCache.remove(uuid);
                    hostnameCache.remove(uuid);
                    ipCache.remove(uuid);

                    plugin.sendDebug("資料已儲存並解鎖: " + uuid);
                } catch (SQLException e) {
                    connection.rollback();
                    plugin.sendDebug("資料儲存失敗: " + e.getMessage());
                    throw e;
                } finally {
                    connection.setAutoCommit(true);
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().warning("Failed to save and remove cache for " + uuid + ": " + e.getMessage());
        }
    }

    private CompletableFuture<Void> updateDatabaseField(UUID uuid, String columnName, Object value) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (uuid == null) return;
                if (dataSource == null || dataSource.isClosed()) connectAndInitPlayerTable();

                try (Connection connection = dataSource.getConnection()) {
                    executeUpdate(connection, uuid, columnName, value);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to update " + columnName + " for " + uuid + ": " + e.getMessage());
            }
        }, runnable -> TaskScheduler.runAsync(plugin, runnable));
    }

    private void executeUpdate(Connection connection, UUID uuid, String columnName, Object value) throws SQLException {
        String sql = "UPDATE player_data SET " + columnName + " = ? WHERE uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, value);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

// #region ArmorHidden處理
    public boolean getArmorHiddenState(UUID uuid) {
        if (uuid == null) return false;
        return armorHiddenCache.getOrDefault(uuid, false);
    }

    public CompletableFuture<Void> setArmorHiddenState(UUID uuid, boolean state) {
        armorHiddenCache.put(uuid, state);
        return updateDatabaseField(uuid, "hidden_armor", state);
    }
// #endregion

// #region Hostname/IP處理
    public String getHostname(UUID uuid) {
        if (uuid == null) return null;
        return hostnameCache.getOrDefault(uuid, "UnknownHostname");
    }

    public CompletableFuture<Void> setHostname(UUID uuid, String hostname) {
        hostnameCache.put(uuid, hostname);
        return updateDatabaseField(uuid, "hostname", hostname);
    }

    public String getIp(UUID uuid) {
        if (uuid == null) return null;
        return ipCache.getOrDefault(uuid, "UnknownIp");
    }

    public CompletableFuture<Void> setIpAddress(UUID uuid, String ip) {
        ipCache.put(uuid, ip);
        return updateDatabaseField(uuid, "ip_address", ip);
    }
// #endregion
}
