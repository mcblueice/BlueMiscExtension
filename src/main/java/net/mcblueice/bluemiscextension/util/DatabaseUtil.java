package net.mcblueice.bluemiscextension.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;

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
            String host = config.getString("Database.host", "localhost");
            int port = config.getInt("Database.port", 3306);
            String database = config.getString("Database.database", "database");
            String user = config.getString("Database.user", "user");
            String password = config.getString("Database.password", "password");
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
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "player_name VARCHAR(32) NOT NULL, " +
                "hidden_armor BOOLEAN NOT NULL DEFAULT 0" +
                ")";
        } else {
            createTable = "CREATE TABLE IF NOT EXISTS player_data (" +
                "uuid TEXT PRIMARY KEY, " +
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
