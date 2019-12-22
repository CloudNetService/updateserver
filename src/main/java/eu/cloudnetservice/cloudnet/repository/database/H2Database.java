package eu.cloudnetservice.cloudnet.repository.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import eu.cloudnetservice.cloudnet.repository.util.ThrowingConsumer;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;

import java.io.IOException;
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
    private CloudNetVersion cachedLatestVersion;

    public H2Database(Path mvStorePath) {
        this.mvStorePath = mvStorePath;
    }

    @Override
    public boolean init() {

        try {
            this.connection = DriverManager.getConnection("jdbc:h2:" + this.mvStorePath.toAbsolutePath());

            this.executeUpdate("CREATE TABLE IF NOT EXISTS versions (name VARCHAR(128), content TEXT)");
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        this.cacheVersions();

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
    public CloudNetVersion getVersion(String name) {
        return Arrays.stream(this.cachedVersions)
                .filter(version -> version.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public CloudNetVersion getLatestVersion() {
        return this.cachedLatestVersion;
    }

    @Override
    public CloudNetVersion[] getAllVersions() {
        return this.cachedVersions;
    }

    private void cacheVersions() {
        this.cachedVersions = this.loadVersions();
        this.cachedLatestVersion = Arrays.stream(this.cachedVersions).filter(Objects::nonNull).max(Comparator.comparing(CloudNetVersion::getReleaseDate)).orElse(null);
    }

    private CloudNetVersion[] loadVersions() {
        Collection<CloudNetVersion> versions = new ArrayList<>();

        try (PreparedStatement statement = this.connection.prepareStatement("SELECT content FROM versions");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                versions.add(this.gson.fromJson(resultSet.getString("content"), CloudNetVersion.class));
            }
        } catch (SQLException exception) {
            exception.printStackTrace();
        }

        return versions.toArray(CloudNetVersion[]::new);
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
