package eu.cloudnetservice.cloudnet.repository.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.cloudnetservice.cloudnet.repository.faq.FAQEntry;
import eu.cloudnetservice.cloudnet.repository.util.StringUtils;
import eu.cloudnetservice.cloudnet.repository.util.ThrowingConsumer;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;
import eu.cloudnetservice.cloudnet.repository.web.WebPermissionRole;
import eu.cloudnetservice.cloudnet.repository.web.WebUser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

public class H2Database implements Database {

    private final Gson gson = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .create();

    private Path mvStorePath;

    private Connection connection;

    private CloudNetVersion[] cachedVersions;
    private FAQEntry[] cachedFAQEntries;
    private WebUser[] cachedUsers;

    public H2Database(Path mvStorePath) {
        this.mvStorePath = mvStorePath;
    }

    @Override
    public boolean init() {

        try {
            this.connection = DriverManager.getConnection("jdbc:h2:" + this.mvStorePath.toAbsolutePath());

            this.executeUpdate("CREATE TABLE IF NOT EXISTS versions (name VARCHAR(128), content TEXT)");
            this.executeUpdate("CREATE TABLE IF NOT EXISTS faq (uniqueId BINARY(16), content TEXT)");
            this.executeUpdate("CREATE TABLE IF NOT EXISTS users (username VARCHAR(128), password TEXT, role VARCHAR(32))");
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        this.cacheVersions();
        this.cacheFAQEntries();
        this.cacheUsers();

        return true;
    }

    private void executeUpdate(String sql) {
        this.executeUpdate(sql, preparedStatement -> {});
    }

    private void executeUpdate(String sql, ThrowingConsumer<PreparedStatement, SQLException> preUpdateHandler) {
        try (PreparedStatement statement = this.connection.prepareStatement(sql)) {
            preUpdateHandler.accept(statement);
            statement.executeUpdate();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void registerVersion(CloudNetVersion version) {
        if (Arrays.stream(this.cachedVersions).anyMatch(cloudNetVersion -> cloudNetVersion.getName().equals(version.getName()))) {
            this.executeUpdate(
                    "DELETE FROM versions WHERE `name` = ?",
                    preparedStatement -> preparedStatement.setString(1, version.getName())
            );
        }

        this.executeUpdate(
                "INSERT INTO versions (name, content) VALUES (?, ?)",
                preparedStatement -> {
                    preparedStatement.setString(1, version.getName());
                    preparedStatement.setString(2, this.gson.toJson(version));
                }
        );

        this.cacheVersions();
    }

    @Override
    public void updateVersion(CloudNetVersion version) {
        this.executeUpdate(
                "UPDATE versions SET content = ? WHERE name = ?",
                preparedStatement -> {
                    preparedStatement.setString(1, this.gson.toJson(version));
                    preparedStatement.setString(2, version.getName());
                }
        );
    }

    @Override
    public CloudNetVersion getVersion(String parentVersionName, String name) {
        return Arrays.stream(this.cachedVersions)
                .filter(version -> version.getParentVersionName().equalsIgnoreCase(parentVersionName))
                .filter(version -> version.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public CloudNetVersion getLatestVersion(String parentVersionName) {
        return Arrays.stream(this.cachedVersions)
                .filter(Objects::nonNull)
                .filter(version -> version.getParentVersionName().equalsIgnoreCase(parentVersionName))
                .max(Comparator.comparing(CloudNetVersion::getReleaseDate)).orElse(null);
    }

    @Override
    public CloudNetVersion[] getAllVersions() {
        return this.cachedVersions;
    }

    @Override
    public CloudNetVersion[] getAllVersions(String parentVersionName) {
        return Arrays.stream(this.cachedVersions)
                .filter(Objects::nonNull)
                .filter(version -> version.getParentVersionName().equalsIgnoreCase(parentVersionName))
                .toArray(CloudNetVersion[]::new);
    }

    @Override
    public FAQEntry[] getFAQEntries(String parentVersionName) {
        return Arrays.stream(this.cachedFAQEntries)
                .filter(entry -> entry.getParentVersionName().equalsIgnoreCase(parentVersionName))
                .toArray(FAQEntry[]::new);
    }

    @Override
    public void insertFAQEntry(FAQEntry entry) {
        this.executeUpdate(
                "INSERT INTO faq (uniqueId, content) VALUES (?, ?)",
                preparedStatement -> {
                    preparedStatement.setBytes(1, this.uuidToBytes(entry.getUniqueId()));
                    preparedStatement.setString(2, this.gson.toJson(entry));
                }
        );
        this.cacheFAQEntries();
    }

    @Override
    public void updateFAQEntry(FAQEntry entry) {
        this.executeUpdate(
                "UPDATE faq SET content = ? WHERE uniqueId = ?",
                preparedStatement -> {
                    preparedStatement.setString(1, this.gson.toJson(entry));
                    preparedStatement.setBytes(2, this.uuidToBytes(entry.getUniqueId()));
                }
        );
        this.cacheFAQEntries();
    }

    @Override
    public FAQEntry getFAQEntry(UUID uniqueId) {
        return Arrays.stream(this.cachedFAQEntries)
                .filter(entry -> entry.getUniqueId().equals(uniqueId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void deleteFAQEntry(UUID uniqueId) {
        this.executeUpdate(
                "DELETE FROM faq WHERE uniqueId = ?",
                preparedStatement -> preparedStatement.setBytes(1, this.uuidToBytes(uniqueId))
        );
        this.cacheFAQEntries();
    }

    @Override
    public boolean checkUserPassword(String username, String password) {
        String hashedPassword = StringUtils.hashString(password);
        return Arrays.stream(this.cachedUsers)
                .filter(webUser -> webUser.getUsername().equals(username))
                .anyMatch(webUser -> webUser.getHashedPassword().equals(hashedPassword));
    }

    @Override
    public boolean hasPassword(String username) {
        return Arrays.stream(this.cachedUsers)
                .filter(webUser -> webUser.getUsername().equals(username))
                .anyMatch(webUser -> webUser.getHashedPassword().equals(StringUtils.EMPTY_HASH_STRING));
    }

    @Override
    public boolean containsUser(String username) {
        return Arrays.stream(this.cachedUsers).anyMatch(webUser -> webUser.getUsername().equals(username));
    }

    @Override
    public WebPermissionRole getRole(String username) {
        return Arrays.stream(this.cachedUsers)
                .filter(webUser -> webUser.getUsername().equals(username))
                .findFirst()
                .map(WebUser::getPermissionRole)
                .orElse(null);
    }

    @Override
    public void insertUser(String username, String password) {
        if (username.length() > 128) {
            throw new IllegalArgumentException("Username may not be longer than 128 characters!");
        }
        if (this.containsUser(username)) {
            throw new IllegalArgumentException("Username is already taken!");
        }
        this.executeUpdate(
                "INSERT INTO users (username, password, role) VALUES (?, ?, ?)",
                preparedStatement -> {
                    preparedStatement.setString(1, username);
                    preparedStatement.setString(2, StringUtils.hashString(password));
                    preparedStatement.setString(3, WebPermissionRole.MEMBER.name());
                }
        );
        this.cacheUsers();
    }

    @Override
    public void insertUser(String username) {
        this.insertUser(username, "");
    }

    @Override
    public void deleteUser(String username) {
        this.executeUpdate(
                "DELETE FROM users WHERE username = ?",
                preparedStatement -> preparedStatement.setString(1, username)
        );
        this.cacheUsers();
    }

    @Override
    public void updateUserRole(String username, WebPermissionRole newPermissionRole) {
        this.executeUpdate(
                "UPDATE users SET role = ? WHERE username = ?",
                preparedStatement -> {
                    preparedStatement.setString(1, newPermissionRole.name());
                    preparedStatement.setString(2, username);
                }
        );
        this.cacheUsers();
    }

    @Override
    public void updateUserPassword(String username, String newPassword) {
        this.executeUpdate(
                "UPDATE users SET password = ? WHERE username = ?",
                preparedStatement -> {
                    preparedStatement.setString(1, StringUtils.hashString(newPassword));
                    preparedStatement.setString(2, username);
                }
        );
        this.cacheUsers();
    }

    private byte[] uuidToBytes(UUID uuid) {
        return ByteBuffer.allocate(16).putLong(uuid.getMostSignificantBits()).putLong(uuid.getLeastSignificantBits()).array();
    }

    private UUID bytesToUUID(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    private void cacheVersions() {
        Collection<CloudNetVersion> versions = new ArrayList<>();

        try (PreparedStatement statement = this.connection.prepareStatement("SELECT content FROM versions");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                versions.add(this.gson.fromJson(resultSet.getString("content"), CloudNetVersion.class));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        this.cachedVersions = versions.toArray(CloudNetVersion[]::new);
    }

    private void cacheFAQEntries() {
        Collection<FAQEntry> entries = new ArrayList<>();
        try (PreparedStatement statement = this.connection.prepareStatement("SELECT content FROM faq");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                entries.add(this.gson.fromJson(resultSet.getString("content"), FAQEntry.class));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        this.cachedFAQEntries = entries.toArray(FAQEntry[]::new);
    }

    private void cacheUsers() {
        Collection<WebUser> users = new ArrayList<>();
        try (PreparedStatement statement = this.connection.prepareStatement("SELECT * FROM users");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                users.add(new WebUser(
                        resultSet.getString("username"),
                        resultSet.getString("password"),
                        WebPermissionRole.valueOf(resultSet.getString("role"))
                ));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
        this.cachedUsers = users.toArray(WebUser[]::new);
    }

    @Override
    public void close() throws IOException {
        try {
            this.connection.close();
        } catch (SQLException exception) {
            exception.printStackTrace();
        }
    }
}
