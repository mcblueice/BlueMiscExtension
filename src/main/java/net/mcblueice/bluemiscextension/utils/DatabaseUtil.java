package net.mcblueice.bluemiscextension.utils;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent.Builder;
import net.mcblueice.bluelib.utils.TaskScheduler;
import net.mcblueice.bluemiscextension.BlueMiscExtension;

public class DatabaseUtil {
    private final BlueMiscExtension plugin;
    private final boolean debug;
    private HikariDataSource dataSource;
    private String dbType = "sqlite";
    private static final int MaxRetry = 39;
    private static final long RetryDelay = 10L;
    private final ConcurrentHashMap<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();
    private TaskScheduler.RepeatingTaskHandler autoSaveTask;

    public DatabaseUtil(BlueMiscExtension plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("Database.debug", false);
    }

    public record PlayerData(
        UUID uuid,
        String playerName,
        boolean hiddenArmor,
        boolean phantomSpawn,
        String nickname,
        String hostname,
        String ip
    ) {
        public PlayerData {
            if (playerName == null) playerName = "Unknown";
            if (nickname == null) nickname = "";
            if (hostname == null) hostname = "UnknownHostname";
            if (ip == null) ip = "UnknownIp";
        }

        public String getDisplayName() {
            return (nickname == null || nickname.isEmpty()) ? playerName : nickname;
        }

        public static PlayerData defaultData(UUID uuid, String playerName) {
            return new PlayerData(uuid, playerName, false, true, "", "UnknownHostname", "UnknownIp");
        }

        public PlayerData withPlayerName(String newPlayerName) {
            return new PlayerData(this.uuid, newPlayerName, this.hiddenArmor, this.phantomSpawn, this.nickname, this.hostname, this.ip);
        }

        public PlayerData withHiddenArmor(boolean newHiddenArmor) {
            return new PlayerData(this.uuid, this.playerName, newHiddenArmor, this.phantomSpawn, this.nickname, this.hostname, this.ip);
        }

        public PlayerData withPhantomSpawn(boolean newPhantomSpawn) {
            return new PlayerData(this.uuid, this.playerName, this.hiddenArmor, newPhantomSpawn, this.nickname, this.hostname, this.ip);
        }

        public PlayerData withNickname(String newNickname) {
            return new PlayerData(this.uuid, this.playerName, this.hiddenArmor, this.phantomSpawn, newNickname, this.hostname, this.ip);
        }

        public PlayerData withHostname(String newHostname) {
            return new PlayerData(this.uuid, this.playerName, this.hiddenArmor, this.phantomSpawn, this.nickname, newHostname, this.ip);
        }

        public PlayerData withIp(String newIp) {
            return new PlayerData(this.uuid, this.playerName, this.hiddenArmor, this.phantomSpawn, this.nickname, this.hostname, newIp);
        }
    }

    // region 初始化與關閉
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
                hikariConfig.setConnectionTimeout(5000);
                hikariConfig.addDataSourceProperty("journal_mode", "WAL");
                hikariConfig.addDataSourceProperty("synchronous", "NORMAL");
                break;
            default:
                throw new SQLException("Unknown database type: " + dbType);
        }

        dataSource = new HikariDataSource(hikariConfig);
        checkAndCreatePlayerTable();
        startAutoSaveTask();
    }

    public void close() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    private void checkAndCreatePlayerTable() throws SQLException {
        String createTable;
        switch (dbType) {
            case "mysql":
                createTable = "CREATE TABLE IF NOT EXISTS player_data (" +
                    "uuid CHAR(36) PRIMARY KEY, " +
                    "player_name VARCHAR(32) NOT NULL, " +
                    "hidden_armor BOOLEAN NOT NULL DEFAULT 0, " +
                    "phantom_spawn BOOLEAN NOT NULL DEFAULT 1, " +
                    "nickname VARCHAR(255) NOT NULL DEFAULT '', " +
                    "hostname VARCHAR(255) NOT NULL DEFAULT '', " +
                    "ip_address VARCHAR(45) NOT NULL DEFAULT '', " +
                    "is_data_saved BOOLEAN NOT NULL DEFAULT 1" +
                    ")";
                break;
            case "sqlite":
                createTable = "CREATE TABLE IF NOT EXISTS player_data (" +
                    "uuid CHAR(36) PRIMARY KEY, " +
                    "player_name TEXT NOT NULL, " +
                    "hidden_armor BOOLEAN NOT NULL DEFAULT 0, " +
                    "phantom_spawn BOOLEAN NOT NULL DEFAULT 1, " +
                    "nickname TEXT NOT NULL DEFAULT '', " +
                    "hostname TEXT NOT NULL DEFAULT '', " +
                    "ip_address TEXT NOT NULL DEFAULT '', " +
                    "is_data_saved BOOLEAN NOT NULL DEFAULT 1" +
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
                ensureColumnExists("player_data", "phantom_spawn", "BOOLEAN NOT NULL DEFAULT 1");
                ensureColumnExists("player_data", "nickname", "VARCHAR(255) NOT NULL DEFAULT ''");
                ensureColumnExists("player_data", "hostname", "VARCHAR(255) NOT NULL DEFAULT ''");
                ensureColumnExists("player_data", "ip_address", "VARCHAR(45) NOT NULL DEFAULT ''");
                ensureColumnExists("player_data", "is_data_saved", "BOOLEAN NOT NULL DEFAULT 1");
                break;
            case "sqlite":
                ensureColumnExists("player_data", "player_name", "TEXT NOT NULL");
                ensureColumnExists("player_data", "hidden_armor", "BOOLEAN NOT NULL DEFAULT 0");
                ensureColumnExists("player_data", "phantom_spawn", "BOOLEAN NOT NULL DEFAULT 1");
                ensureColumnExists("player_data", "nickname", "TEXT NOT NULL DEFAULT ''");
                ensureColumnExists("player_data", "hostname", "TEXT NOT NULL DEFAULT ''");
                ensureColumnExists("player_data", "ip_address", "TEXT NOT NULL DEFAULT ''");
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
    // endregion 初始化與關閉

    // region 資料保存與載入
    private void startAutoSaveTask() {
        long interval = plugin.getConfig().getLong("Database.AutosaveInterval", 12000L);
        if (interval <= 0) return;
        if (autoSaveTask != null) autoSaveTask.cancel();
        autoSaveTask = TaskScheduler.runAsyncRepeatingTask(plugin, this::saveAllCachedData, interval, interval);
    }

    public void saveAllCachedData() {
        if (dataSource == null || dataSource.isClosed()) return;
        if (playerDataCache.isEmpty()) return;

        if (debug) plugin.sendDebug("執行資料庫自動保存任務...");
        // new一個新的Set避免被修改
        savePlayerData(new HashSet<>(playerDataCache.keySet()), false);
    }

    public void savePlayerData(UUID uuid, boolean removeCacheAndUnlock) {
        if (dataSource == null || dataSource.isClosed()) return;
        
        try (Connection connection = dataSource.getConnection()) {
            savePlayerDataInternal(connection, uuid, removeCacheAndUnlock);
        } catch (SQLException e) {
            plugin.getLogger().severe("Error saving player " + uuid + ": " + e.getMessage());
        }
    }

    public void savePlayerData(Collection<UUID> uuids, boolean removeCacheAndUnlock) {
        if (dataSource == null || dataSource.isClosed()) return;
        if (uuids == null || uuids.isEmpty()) return;

        try (Connection connection = dataSource.getConnection()) {
            for (UUID uuid : uuids) {
                if (debug) plugin.sendDebug("§7儲存玩家 " + uuid + " 的資料...");
                savePlayerDataInternal(connection, uuid, removeCacheAndUnlock);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not open database connection for batch save!");
        }
    }

    private void savePlayerDataInternal(Connection connection, UUID uuid, boolean removeCacheAndUnlock) throws SQLException {
        if (!playerDataCache.containsKey(uuid)) return;
        Map<String, Object> updates = new HashMap<>();
        try {
            PlayerData playerData = playerDataCache.get(uuid);
            if (playerData != null) {
                updates.put("hidden_armor", playerData.hiddenArmor());
                updates.put("phantom_spawn", playerData.phantomSpawn());
                updates.put("nickname", playerData.nickname());
                updates.put("hostname", playerData.hostname());
                updates.put("ip_address", playerData.ip());
            }
            if (removeCacheAndUnlock) updates.put("is_data_saved", true);
            if (updates.isEmpty()) return;

            executeUpdate(connection, uuid, updates);

            if (removeCacheAndUnlock) {
                clearCache(uuid);
                if (debug) plugin.sendDebug("資料已儲存並解鎖: " + uuid);
            }
        } catch (SQLException e) {
            throw e;
        }
    }

    public CompletableFuture<Void> savePlayerDataAsync(UUID uuid, boolean removeCacheAndUnlock) {
        return CompletableFuture.runAsync(() -> savePlayerData(uuid, removeCacheAndUnlock), runnable -> TaskScheduler.runAsync(plugin, runnable));
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

    public CompletableFuture<Void> loadAndCreateCache(UUID uuid) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        attemptLoad(uuid, future);
        return future;
    }

    private void attemptLoad(UUID uuid, CompletableFuture<Void> future) {
        TaskScheduler.runAsync(plugin, () -> {
            int retryCount = 0;
            while (retryCount < MaxRetry) {
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
                            String sql = "SELECT player_name, hidden_armor, phantom_spawn, nickname, hostname, ip_address FROM player_data WHERE uuid = ?";
                            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                                ps.setString(1, uuid.toString());
                                try (ResultSet rs = ps.executeQuery()) {
                                    if (rs.next()) {
                                        PlayerData playerData = new PlayerData(
                                            uuid,
                                            rs.getString("player_name"),
                                            rs.getBoolean("hidden_armor"),
                                            rs.getBoolean("phantom_spawn"),
                                            rs.getString("nickname"),
                                            rs.getString("hostname"),
                                            rs.getString("ip_address")
                                        );
                                        playerDataCache.put(uuid, playerData);
                                    } else {
                                        Player player = Bukkit.getPlayer(uuid);
                                        String fallbackName = (player != null) ? player.getName() : "Unknown";
                                        playerDataCache.put(uuid, PlayerData.defaultData(uuid, fallbackName));
                                    }
                                }
                            }
                            if (debug) plugin.sendDebug("資料已載入並鎖定: " + uuid);
                            future.complete(null);
                            return;
                        } else {
                            if (debug) plugin.sendDebug("資料尚未儲存: " + uuid + "，正在重試... (" + (retryCount+1) + ")");
                            retryCount++;
                            try {
                                Thread.sleep(RetryDelay * 50);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                future.completeExceptionally(e);
                                return;
                            }
                        }
                    }
                } catch (SQLException e) {
                    plugin.getLogger().warning("Failed to load player data for " + uuid + ": " + e.getMessage());
                    future.completeExceptionally(e);
                    return;
                }
            }

            TaskScheduler.runTask(plugin, () -> {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) player.kick(Component.text("§c無法同步玩家資料 請找管理員協助"));
            });
            plugin.getLogger().warning("Data sync timeout for " + uuid);
            future.completeExceptionally(new RuntimeException("Data sync timeout"));
        });
    }

    private void clearCache(UUID uuid) {
        playerDataCache.remove(uuid);
    }
    // endregion 資料保存與載入

    // region 資料庫操作
    public CompletableFuture<Void> updateDatabaseField(UUID uuid, String columnName, Object value) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (uuid == null) return;
                if (dataSource == null || dataSource.isClosed()) connectAndInitPlayerTable();

                try (Connection connection = dataSource.getConnection()) {
                    Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put(columnName, value);
                    executeUpdate(connection, uuid, updates);
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to update " + columnName + " for " + uuid + ": " + e.getMessage());
            }
        }, runnable -> TaskScheduler.runAsync(plugin, runnable));
    }

    private void executeUpdate(Connection connection, UUID uuid, Map<String, Object> updates) throws SQLException {
        if (updates == null || updates.isEmpty()) return;

        StringBuilder sql = new StringBuilder("UPDATE player_data SET ");
        int i = 0;
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            sql.append(entry.getKey()).append(" = ?");
            if (i < updates.size() - 1) sql.append(", ");
            i++;
        }
        sql.append(" WHERE uuid = ? AND is_data_saved = 0");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            for (Map.Entry<String, Object> entry : updates.entrySet()) {
                ps.setObject(paramIndex++, entry.getValue());
            }
            ps.setString(paramIndex, uuid.toString());
            ps.executeUpdate();
        }
    }

    public Component getDatabaseStatus() {
        Builder builder = Component.text()
            .append(Component.text("§8§m---------§r§b資料庫狀態§8§m---------\n"))
            .append(Component.text("§6類型: §e" + dbType + "\n"))
            .append(Component.text("§c無法取得資料庫狀態!\n"))
            .append(Component.text("§8§m--------------------------"));

        if (dataSource == null || dataSource.isClosed()) return builder.build();

        try {
            HikariPoolMXBean poolProxy = dataSource.getHikariPoolMXBean();
            if (poolProxy == null) return builder.build();

            int active = poolProxy.getActiveConnections();
            int idle = poolProxy.getIdleConnections();
            int total = poolProxy.getTotalConnections();
            int threadsAwaiting = poolProxy.getThreadsAwaitingConnection();
            int maxPoolSize = dataSource.getMaximumPoolSize();

            builder = Component.text()
                .append(Component.text("§8§m---------§r§b資料庫狀態§8§m---------\n"))
                .append(Component.text("§6類型: §e" + dbType + "\n"))
                .append(Component.text("§6工作連線 (Active): §e" + active + "\n"))
                .append(Component.text("§6閒置連線 (Idle): §e" + idle + "\n"))
                .append(Component.text("§6總連線數 (Total): §e" + total + " / " + maxPoolSize + "\n"))
                .append(Component.text(((threadsAwaiting > 0) ? "§c⚠" : "§6") + "等待連線 (Awaiting): §e" + threadsAwaiting + "\n"))
                .append(Component.text("§8§m--------------------------"));

            return builder.build();
        } catch (Exception e) {
            return builder.build();
        }
    }
    // endregion 資料庫操作

    // region 其他查詢
    public CompletableFuture<UUID> getUUID(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            if (playerName == null || playerName.isEmpty()) return null;
            if (dataSource == null || dataSource.isClosed()) return null;

            String sql = "SELECT uuid FROM player_data WHERE player_name = ? LIMIT 1";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                ps.setString(1, playerName);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String uuidStr = rs.getString("uuid");
                        if (uuidStr != null && !uuidStr.isEmpty()) {
                            try {
                                return UUID.fromString(uuidStr);
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Database corruption: Invalid UUID format for player " + playerName + ": " + uuidStr);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to lookup UUID by name [" + playerName + "]: " + e.getMessage());
            }
            return null;
        }, runnable -> TaskScheduler.runAsync(plugin, runnable));
    }

    public CompletableFuture<String> getName(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (uuid == null) return null;
            if (dataSource == null || dataSource.isClosed()) return null;

            String sql = "SELECT player_name FROM player_data WHERE uuid = ?";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                ps.setString(1, uuid.toString());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("player_name");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to lookup name by UUID [" + uuid + "]: " + e.getMessage());
            }
            return null;
        }, runnable -> TaskScheduler.runAsync(plugin, runnable));
    }
    // endregion 其他查詢

    // region PlayerData 操作
    public boolean isPlayerDataLoaded(UUID uuid) {
        return uuid != null && playerDataCache.containsKey(uuid);
    }

    public PlayerData getPlayerData(UUID uuid) {
        if (uuid == null) return PlayerData.defaultData(null, "Unknown");

        PlayerData playerData = playerDataCache.get(uuid);
        if (playerData != null) return playerData;

        Player player = Bukkit.getPlayer(uuid);
        String fallbackName = (player != null) ? player.getName() : "Unknown";
        return PlayerData.defaultData(uuid, fallbackName);
    }

    public CompletableFuture<PlayerData> getOfflinePlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (uuid == null) return null;
            if (dataSource == null || dataSource.isClosed()) return null;

            String sql = "SELECT player_name, hidden_armor, phantom_spawn, nickname, hostname, ip_address FROM player_data WHERE uuid = ?";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                ps.setString(1, uuid.toString());

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String playerName = rs.getString("player_name");
                        Boolean hiddenArmor = rs.getBoolean("hidden_armor");
                        Boolean phantomSpawn = rs.getBoolean("phantom_spawn");
                        String nickname = rs.getString("nickname");
                        String hostname = rs.getString("hostname");
                        String ip_address = rs.getString("ip_address");
                        return new PlayerData(uuid, playerName, hiddenArmor, phantomSpawn, nickname, hostname, ip_address);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to lookup player data by UUID [" + uuid + "]: " + e.getMessage());
            }
            return null;
        }, runnable -> TaskScheduler.runAsync(plugin, runnable));
    }
    public CompletableFuture<PlayerData> getOfflinePlayerData(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            if (playerName == null || playerName.isEmpty()) return null;
            if (dataSource == null || dataSource.isClosed()) return null;

            String sql = "SELECT uuid, hidden_armor, phantom_spawn, nickname, hostname, ip_address FROM player_data WHERE player_name = ?";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement(sql)) {

                ps.setString(1, playerName);

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String uuidStr = rs.getString("uuid");
                        Boolean hiddenArmor = rs.getBoolean("hidden_armor");
                        Boolean phantomSpawn = rs.getBoolean("phantom_spawn");
                        String nickname = rs.getString("nickname");
                        String hostname = rs.getString("hostname");
                        String ip_address = rs.getString("ip_address");
                        if (uuidStr != null && !uuidStr.isEmpty()) {
                            try {
                                return new PlayerData(UUID.fromString(uuidStr), playerName, hiddenArmor, phantomSpawn, nickname, hostname, ip_address);
                            } catch (IllegalArgumentException e) {
                                plugin.getLogger().warning("Database corruption: Invalid UUID format for player " + playerName + ": " + uuidStr);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to lookup UUID by name [" + playerName + "]: " + e.getMessage());
            }
            return null;
        }, runnable -> TaskScheduler.runAsync(plugin, runnable));
    }

    public CompletableFuture<Void> setArmorHiddenState(UUID uuid, boolean state) {
        if (!playerDataCache.containsKey(uuid)) return CompletableFuture.completedFuture(null);
        playerDataCache.computeIfPresent(uuid, (k, data) -> data.withHiddenArmor(state));
        if (plugin.getConfig().getBoolean("Database.SyncWrite", true)) return updateDatabaseField(uuid, "hidden_armor", state);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> setPhantomSpawnState(UUID uuid, boolean state) {
        if (!playerDataCache.containsKey(uuid)) return CompletableFuture.completedFuture(null);
        playerDataCache.computeIfPresent(uuid, (k, data) -> data.withPhantomSpawn(state));
        if (plugin.getConfig().getBoolean("Database.SyncWrite", true)) return updateDatabaseField(uuid, "phantom_spawn", state);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> setNickname(UUID uuid, String nickname) {
        if (!playerDataCache.containsKey(uuid)) return CompletableFuture.completedFuture(null);
        playerDataCache.computeIfPresent(uuid, (k, data) -> data.withNickname(nickname));
        if (plugin.getConfig().getBoolean("Database.SyncWrite", true)) return updateDatabaseField(uuid, "nickname", nickname);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> setHostname(UUID uuid, String hostname) {
        if (!playerDataCache.containsKey(uuid)) return CompletableFuture.completedFuture(null);
        playerDataCache.computeIfPresent(uuid, (k, data) -> data.withHostname(hostname));
        if (plugin.getConfig().getBoolean("Database.SyncWrite", true)) return updateDatabaseField(uuid, "hostname", hostname);
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> setIpAddress(UUID uuid, String ip) {
        if (!playerDataCache.containsKey(uuid)) return CompletableFuture.completedFuture(null);
        playerDataCache.computeIfPresent(uuid, (k, data) -> data.withIp(ip));
        if (plugin.getConfig().getBoolean("Database.SyncWrite", true)) return updateDatabaseField(uuid, "ip_address", ip);
        return CompletableFuture.completedFuture(null);
    }
    // endregion PlayerData 操作
}
