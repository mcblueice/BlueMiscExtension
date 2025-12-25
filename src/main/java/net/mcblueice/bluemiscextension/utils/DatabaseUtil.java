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
    private final ConcurrentHashMap<UUID, String> hostnameCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, String> ipCache = new ConcurrentHashMap<>();

    public DatabaseUtil(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void connectAndInitPlayerTable() throws SQLException {
        FileConfiguration config = plugin.getConfig();
        dbType = config.getString("Database.type", "sqlite").toLowerCase();
        
        switch (dbType) {
            case "mysql":
                String host = config.getString("Database.MySQL.host", "localhost");
                int port = config.getInt("Database.MySQL.port", 3306);
                String database = config.getString("Database.MySQL.database", "database");
                String user = config.getString("Database.MySQL.user", "user");
                String password = config.getString("Database.MySQL.password", "password");
                String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
                connection = DriverManager.getConnection(url, user, password);
                break;
            case "sqlite":
                try {
                    Class.forName("org.sqlite.JDBC");
                } catch (ClassNotFoundException e) {
                    throw new SQLException("SQLite JDBC driver not found.", e);
                }
                File dbFile = new File(plugin.getDataFolder(), "database.db");
                String sqliteUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
                connection = DriverManager.getConnection(sqliteUrl);
                break;
            default:
                throw new SQLException("Unknown database type: " + dbType);
        }
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
                    "hidden_armor INTEGER NOT NULL DEFAULT 0, " +
                    "hostname TEXT, " +
                    "ip_address TEXT" +
                    ")";
                break;
            default:
                throw new SQLException("Unknown database type: " + dbType);
        }
        
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(createTable);
        }

        switch (dbType) {
            case "mysql":
                ensureColumnExists("player_data", "player_name", "VARCHAR(32) NOT NULL");
                ensureColumnExists("player_data", "hidden_armor", "BOOLEAN NOT NULL DEFAULT 0");
                ensureColumnExists("player_data", "hostname", "VARCHAR(255)");
                ensureColumnExists("player_data", "ip_address", "VARCHAR(45)");
                break;
            case "sqlite":
                ensureColumnExists("player_data", "player_name", "TEXT NOT NULL");
                ensureColumnExists("player_data", "hidden_armor", "INTEGER NOT NULL DEFAULT 0");
                ensureColumnExists("player_data", "hostname", "TEXT");
                ensureColumnExists("player_data", "ip_address", "TEXT");
                break;
            default:
                throw new SQLException("Unknown database type: " + dbType);
        }
    }

    private void ensureColumnExists(String table, String column, String definition) throws SQLException {
        boolean exists = false;
        try (Statement stmt = connection.createStatement()) {
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
        if (connection == null || connection.isClosed()) connectAndInitPlayerTable();

        String trimmedName = playerName.length() > 32 ? playerName.substring(0, 32) : playerName;
        String sql;
        switch (dbType) {
            case "mysql":
                sql = "INSERT INTO player_data (uuid, player_name, hidden_armor) VALUES (?, ?, 0) " +
                    "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name)";
                break;
            case "sqlite":
                sql = "INSERT INTO player_data (uuid, player_name, hidden_armor) VALUES (?, ?, 0) " +
                    "ON CONFLICT(uuid) DO UPDATE SET player_name = excluded.player_name";
                break;
            default:
                throw new SQLException("Unknown database type: " + dbType);
        }

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, trimmedName);
            ps.executeUpdate();
        }
    }

    public Connection getConnection() { return connection; }

    public void createTable(String sql) throws SQLException {
        if (connection == null || connection.isClosed()) throw new SQLException("Database connection is not established.");

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

                String sql = "SELECT hidden_armor, hostname, ip_address FROM player_data WHERE uuid = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        boolean result = false;
                        switch (dbType) {
                            case "mysql":
                                result = rs.getBoolean("hidden_armor");
                                break;
                            case "sqlite":
                                result = rs.getInt("hidden_armor") == 1;
                                break;
                            default:
                                throw new SQLException("Unknown database type: " + dbType);
                        }
                        armorHiddenCache.put(uuid, result);

                        String hostname = rs.getString("hostname");
                        hostnameCache.put(uuid, (hostname != null) ? hostname : "UnknownHostname");

                        String ip = rs.getString("ip_address");
                        ipCache.put(uuid, (ip != null) ? ip : "UnknownIp");
                    } else {
                        armorHiddenCache.put(uuid, false);
                        hostnameCache.put(uuid, "UnknownHostname");
                        ipCache.put(uuid, "UnknownIp");
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

                //ArmorHidden
                Boolean cachedArmor = armorHiddenCache.get(uuid);
                if (cachedArmor != null) {
                    String sql = "UPDATE player_data SET hidden_armor = ? WHERE uuid = ?";

                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        ps.setInt(1, cachedArmor ? 1 : 0);
                        ps.setString(2, uuid.toString());
                        ps.executeUpdate();
                    }
                    armorHiddenCache.remove(uuid);
                }

                //Hostname
                String cachedHostname = hostnameCache.get(uuid);
                if (cachedHostname != null) {
                    String sql = "UPDATE player_data SET hostname = ? WHERE uuid = ?";

                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        ps.setString(1, cachedHostname);
                        ps.setString(2, uuid.toString());
                        ps.executeUpdate();
                    }
                    hostnameCache.remove(uuid);
                }

                //IP
                String cachedIp = ipCache.get(uuid);
                if (cachedIp != null) {
                    String sql = "UPDATE player_data SET ip_address = ? WHERE uuid = ?";

                    try (PreparedStatement ps = connection.prepareStatement(sql)) {
                        ps.setString(1, cachedIp);
                        ps.setString(2, uuid.toString());
                        ps.executeUpdate();
                    }
                    ipCache.remove(uuid);
                }

            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to save and remove cache for " + uuid + ": " + e.getMessage());
            }
        }, runnable -> TaskScheduler.runAsync(plugin, runnable));
    }

// #region ArmorHidden處理
    public boolean getArmorHiddenState(UUID uuid) {
        if (uuid == null) return false;
        return armorHiddenCache.getOrDefault(uuid, false);
    }

    public CompletableFuture<Void> setArmorHiddenState(UUID uuid, boolean state) {
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

// #region Hostname/IP處理
    public String getHostname(UUID uuid) {
        if (uuid == null) return null;
        return hostnameCache.getOrDefault(uuid, "UnknownHostname");
    }

    public String getIp(UUID uuid) {
        if (uuid == null) return null;
        return ipCache.getOrDefault(uuid, "UnknownIp");
    }

    public CompletableFuture<Void> setHostname(UUID uuid, String hostname) {
        hostnameCache.put(uuid, hostname);

        return CompletableFuture.runAsync(() -> {
            try {
                if (uuid == null) return;
                if (connection == null || connection.isClosed()) connectAndInitPlayerTable();

                String sql = "UPDATE player_data SET hostname = ? WHERE uuid = ?";

                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, hostname);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to update hostname for " + uuid + ": " + e.getMessage());
            }
        }, runnable -> TaskScheduler.runAsync(plugin, runnable));
    }

    public CompletableFuture<Void> setIpAddress(UUID uuid, String ip) {
        if (ip != null) ipCache.put(uuid, ip);

        return CompletableFuture.runAsync(() -> {
            try {
                if (uuid == null) return;
                if (connection == null || connection.isClosed()) connectAndInitPlayerTable();

                String sql = "UPDATE player_data SET ip_address = ? WHERE uuid = ?";

                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, ip);
                    ps.setString(2, uuid.toString());
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to update IP address for " + uuid + ": " + e.getMessage());
            }
        }, runnable -> TaskScheduler.runAsync(plugin, runnable));
    }
// #endregion
}
