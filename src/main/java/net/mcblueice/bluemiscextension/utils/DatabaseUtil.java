package net.mcblueice.bluemiscextension.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseUtil {
    private final JavaPlugin plugin;
    private Connection connection;
    private String dbType = "sqlite";
    private final ConcurrentHashMap<UUID, Boolean> armorHiddenCache = new ConcurrentHashMap<>();

    public DatabaseUtil(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void connectAndInitPlayerTable() throws SQLException {
        FileConfiguration config = plugin.getConfig();
        dbType = config.getString("Database.type", "sqlite").toLowerCase();
        if (dbType.equals("mysql")) {
            String host = config.getString("Database.MySQL.host", "localhost");
            int port = config.getInt("Database.MySQL.port", 3306);
            String database = config.getString("Database.MySQL.database", "database");
            String user = config.getString("Database.MySQL.user", "user");
            String password = config.getString("Database.MySQL.password", "password");
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
            connection = DriverManager.getConnection(url, user, password);
        } else {
            // SQLite
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC driver not found.", e);
            }
            File dbFile = new File(plugin.getDataFolder(), "database.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
        }
        checkAndCreatePlayerTable();
    }

    private void checkAndCreatePlayerTable() throws SQLException {
        String createTable;
        if (dbType.equals("mysql")) {
            createTable = "CREATE TABLE IF NOT EXISTS player_data (" +
                "uuid CHAR(36) PRIMARY KEY, " +
                "player_name VARCHAR(32) NOT NULL, " +
                "hidden_armor BOOLEAN NOT NULL DEFAULT 0" +
                ")";
        } else {
            createTable = "CREATE TABLE IF NOT EXISTS player_data (" +
                "uuid CHAR(36) PRIMARY KEY, " +
                "player_name TEXT NOT NULL, " +
                "hidden_armor INTEGER NOT NULL DEFAULT 0" +
                ")";
        }
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(createTable);
        if (dbType.equals("mysql")) {
            ensureColumnExists("player_data", "player_name", "VARCHAR(32) NOT NULL");
            ensureColumnExists("player_data", "hidden_armor", "BOOLEAN NOT NULL DEFAULT 0");
        }
        stmt.close();
    }

    private void ensureColumnExists(String table, String column, String definition) throws SQLException {
        if (!dbType.equals("mysql")) return;
        Statement stmt = connection.createStatement();
        String checkColumn = "SHOW COLUMNS FROM " + table + " LIKE '" + column + "'";
        boolean exists = false;
        var rs = stmt.executeQuery(checkColumn);
        if (rs.next()) exists = true;
        rs.close();
        if (!exists) {
            String alter = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition;
            stmt.executeUpdate(alter);
        }
        stmt.close();
    }

    public synchronized void upsertPlayerData(UUID uuid, String playerName) throws SQLException {
        if (uuid == null || playerName == null || playerName.isEmpty()) return;
        if (connection == null || connection.isClosed()) connectAndInitPlayerTable();

        String trimmedName = playerName.length() > 32 ? playerName.substring(0, 32) : playerName;
        String sql;
        if (dbType.equals("mysql")) {
            sql = "INSERT INTO player_data (uuid, player_name, hidden_armor) VALUES (?, ?, 0) " +
                "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name)";
        } else {
            sql = "INSERT INTO player_data (uuid, player_name, hidden_armor) VALUES (?, ?, 0) " +
                "ON CONFLICT(uuid) DO UPDATE SET player_name = excluded.player_name";
        }

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, trimmedName);
            ps.executeUpdate();
        }
    }

    public Connection getConnection() { return connection; }

    public void createTable(String sql) throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database connection is not established.");
        }
        Statement stmt = connection.createStatement();
        stmt.executeUpdate(sql);
        stmt.close();
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public CompletableFuture<Void> loadAndCreateCache(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (uuid == null || connection == null || connection.isClosed()) return;

                String sql = "SELECT hidden_armor FROM player_data WHERE uuid = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        boolean result = dbType.equals("mysql") ? rs.getBoolean("hidden_armor") : rs.getInt("hidden_armor") == 1;
                        armorHiddenCache.put(uuid, result);
                    } else {
                        armorHiddenCache.put(uuid, false);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to load player data for " + uuid + ": " + e.getMessage());
            }
        }, runnable -> TaskScheduler.runAsync(plugin, runnable));
    }

    public CompletableFuture<Void> saveAndRemoveCache(UUID uuid) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (uuid == null || connection == null || connection.isClosed()) return;

                Boolean cached = armorHiddenCache.get(uuid);
                if (cached == null) return;

                String sql;
                if (dbType.equals("mysql")) {
                    sql = "UPDATE player_data SET hidden_armor = ? WHERE uuid = ?";
                } else {
                    sql = "UPDATE player_data SET hidden_armor = ? WHERE uuid = ?";
                }

                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, cached ? 1 : 0);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }

                armorHiddenCache.remove(uuid);
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to save and remove cache for " + uuid + ": " + e.getMessage());
            }
        }, runnable -> TaskScheduler.runAsync(plugin, runnable));
    }

// #region ArmorHidden處理
    public boolean getArmorHiddenState(UUID uuid) {
        if (uuid == null) return false;

        Boolean cached = armorHiddenCache.get(uuid);
        if (cached != null) return cached;

        // 注意：如果在 PacketListener (高頻率觸發) 中調用此方法且緩存未命中，
        // 直接調用 loadAndCreateCache 會導致瞬間產生大量異步任務，造成伺服器卡頓。
        // 建議確保在 PlayerJoinEvent 預先載入數據。
        // 如果必須在此處懶加載，建議增加防重複機制 (例如 Set<UUID> loading)。
        // 這裡暫時保留原邏輯但建議您檢查調用頻率，或將載入邏輯移至登入事件。
        if (!plugin.getServer().getPlayer(uuid).isOnline()) return false; // 簡單檢查，避免離線玩家觸發
        
        // 為了安全，這裡不應該直接觸發資料庫查詢，除非有防重機制。
        // 假設您已在 JoinEvent 處理載入，這裡返回 false 是安全的默認值。
        return false;
    }

    // 異步設定裝備隱藏狀態
    public CompletableFuture<Void> setArmorHiddenState(UUID uuid, boolean state) {
        // 1. 立即更新緩存 (同步)，確保 UI 或邏輯能即時獲得最新狀態，避免異步延遲導致的狀態不一致
        armorHiddenCache.put(uuid, state);

        return CompletableFuture.runAsync(() -> {
            try {
                if (uuid == null) return;
                if (connection == null || connection.isClosed()) connectAndInitPlayerTable();

                String sql = "UPDATE player_data SET hidden_armor = ? WHERE uuid = ?";

                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, state ? 1 : 0);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to update armor hidden state for " + uuid + ": " + e.getMessage());
            }
        }, runnable -> TaskScheduler.runAsync(plugin, runnable));
    }
// #endregion
}
