package com.authreg.storage;

import com.authreg.AuthRegPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class AccountStorage {
    private final AuthRegPlugin plugin;
    private Connection connection;
    private final ExecutorService dbExecutor;

    public AccountStorage(AuthRegPlugin plugin) {
        this.plugin = plugin;
        this.dbExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "auth_reg-sqlite");
                t.setDaemon(true);
                return t;
            }
        });
    }

    public synchronized void init(String path) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            File dbFile = new File(path);
            File parent = dbFile.getParentFile();
            if (parent != null && !parent.exists()) {
                //noinspection ResultOfMethodCallIgnored
                parent.mkdirs();
            }
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            try (Statement st = connection.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS accounts (" +
                        "uuid TEXT PRIMARY KEY," +
                        "name TEXT UNIQUE COLLATE NOCASE," +
                        "password_hash TEXT NOT NULL," +
                        "last_ip TEXT," +
                        "last_world TEXT," +
                        "last_x REAL," +
                        "last_y REAL," +
                        "last_z REAL," +
                        "last_yaw REAL," +
                        "last_pitch REAL," +
                        "created_at INTEGER," +
                        "updated_at INTEGER" +
                        ");");
                addColumnIfMissing(st, "ALTER TABLE accounts ADD COLUMN last_world TEXT;");
                addColumnIfMissing(st, "ALTER TABLE accounts ADD COLUMN last_x REAL;");
                addColumnIfMissing(st, "ALTER TABLE accounts ADD COLUMN last_y REAL;");
                addColumnIfMissing(st, "ALTER TABLE accounts ADD COLUMN last_z REAL;");
                addColumnIfMissing(st, "ALTER TABLE accounts ADD COLUMN last_yaw REAL;");
                addColumnIfMissing(st, "ALTER TABLE accounts ADD COLUMN last_pitch REAL;");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[auth_reg] Не удалось инициализировать SQLite: " + e.getMessage());
        }
    }

    public CompletableFuture<Optional<Account>> findByUUIDAsync(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> findByUUID(uuid), dbExecutor);
    }

    public synchronized Optional<Account> findByUUID(UUID uuid) {
        if (connection == null) return Optional.empty();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM accounts WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("findByUUID error: " + e.getMessage());
        }
        return Optional.empty();
    }

    public CompletableFuture<Optional<Account>> findByNameAsync(String name) {
        return CompletableFuture.supplyAsync(() -> findByName(name), dbExecutor);
    }

    public synchronized Optional<Account> findByName(String name) {
        if (connection == null) return Optional.empty();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM accounts WHERE LOWER(name) = LOWER(?)")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("findByName error: " + e.getMessage());
        }
        return Optional.empty();
    }

    public CompletableFuture<Void> createAccountAsync(UUID uuid, String name, String passwordHash, String ip, Location location) {
        return CompletableFuture.runAsync(() -> createAccount(uuid, name, passwordHash, ip, location), dbExecutor);
    }

    public synchronized void createAccount(UUID uuid, String name, String passwordHash, String ip, Location location) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement("INSERT INTO accounts(uuid, name, password_hash, last_ip, last_world, last_x, last_y, last_z, last_yaw, last_pitch, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            long now = Instant.now().getEpochSecond();
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setString(3, passwordHash);
            ps.setString(4, ip);
            if (location != null && location.getWorld() != null) {
                ps.setString(5, location.getWorld().getName());
                ps.setDouble(6, location.getX());
                ps.setDouble(7, location.getY());
                ps.setDouble(8, location.getZ());
                ps.setFloat(9, location.getYaw());
                ps.setFloat(10, location.getPitch());
            } else {
                ps.setNull(5, Types.VARCHAR);
                ps.setNull(6, Types.REAL);
                ps.setNull(7, Types.REAL);
                ps.setNull(8, Types.REAL);
                ps.setNull(9, Types.REAL);
                ps.setNull(10, Types.REAL);
            }
            ps.setLong(11, now);
            ps.setLong(12, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("createAccount error: " + e.getMessage());
        }
    }

    public CompletableFuture<Void> updatePasswordAsync(UUID uuid, String passwordHash) {
        return CompletableFuture.runAsync(() -> updatePassword(uuid, passwordHash), dbExecutor);
    }

    public synchronized void updatePassword(UUID uuid, String passwordHash) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement("UPDATE accounts SET password_hash = ?, updated_at = ? WHERE uuid = ?")) {
            ps.setString(1, passwordHash);
            ps.setLong(2, Instant.now().getEpochSecond());
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("updatePassword error: " + e.getMessage());
        }
    }

    public CompletableFuture<Void> updateLastIpAsync(UUID uuid, String ip) {
        return CompletableFuture.runAsync(() -> updateLastIp(uuid, ip), dbExecutor);
    }

    public synchronized void updateLastIp(UUID uuid, String ip) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement("UPDATE accounts SET last_ip = ?, updated_at = ? WHERE uuid = ?")) {
            ps.setString(1, ip);
            ps.setLong(2, Instant.now().getEpochSecond());
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("updateLastIp error: " + e.getMessage());
        }
    }

    public CompletableFuture<Void> updateLastLocationAsync(UUID uuid, Location location) {
        return CompletableFuture.runAsync(() -> updateLastLocation(uuid, location), dbExecutor);
    }

    public synchronized void updateLastLocation(UUID uuid, Location location) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement("UPDATE accounts SET last_world = ?, last_x = ?, last_y = ?, last_z = ?, last_yaw = ?, last_pitch = ?, updated_at = ? WHERE uuid = ?")) {
            if (location != null && location.getWorld() != null) {
                ps.setString(1, location.getWorld().getName());
                ps.setDouble(2, location.getX());
                ps.setDouble(3, location.getY());
                ps.setDouble(4, location.getZ());
                ps.setFloat(5, location.getYaw());
                ps.setFloat(6, location.getPitch());
            } else {
                ps.setNull(1, Types.VARCHAR);
                ps.setNull(2, Types.REAL);
                ps.setNull(3, Types.REAL);
                ps.setNull(4, Types.REAL);
                ps.setNull(5, Types.REAL);
                ps.setNull(6, Types.REAL);
            }
            ps.setLong(7, Instant.now().getEpochSecond());
            ps.setString(8, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("updateLastLocation error: " + e.getMessage());
        }
    }

    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
        dbExecutor.shutdownNow();
    }

    private Account map(ResultSet rs) throws SQLException {
        UUID uuid = UUID.fromString(rs.getString("uuid"));
        String name = rs.getString("name");
        String password = rs.getString("password_hash");
        String ip = rs.getString("last_ip");
        long created = rs.getLong("created_at");
        long updated = rs.getLong("updated_at");
        String world = rs.getString("last_world");
        Double x = (Double) rs.getObject("last_x");
        Double y = (Double) rs.getObject("last_y");
        Double z = (Double) rs.getObject("last_z");
        Float yaw = rs.getObject("last_yaw") == null ? null : rs.getFloat("last_yaw");
        Float pitch = rs.getObject("last_pitch") == null ? null : rs.getFloat("last_pitch");
        return new Account(uuid, name, password, ip, created, updated, world, x, y, z, yaw, pitch);
    }

    private void addColumnIfMissing(Statement st, String sql) {
        try {
            st.executeUpdate(sql);
        } catch (SQLException ignored) {
        }
    }
}
