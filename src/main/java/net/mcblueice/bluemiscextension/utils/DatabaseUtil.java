package net.mcblueice.bluemiscextension.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

public class DatabaseUtil {
    private final JavaPlugin plugin;
    private Connection connection;
    private String dbType = "sqlite";

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

    public Connection getConnection() {
        return connection;
    }

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
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
