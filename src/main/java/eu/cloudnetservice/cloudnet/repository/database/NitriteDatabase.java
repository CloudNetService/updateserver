package eu.cloudnetservice.cloudnet.repository.database;

import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;
import org.dizitart.no2.*;
import org.dizitart.no2.filters.Filters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class NitriteDatabase implements Database {

    private Nitrite nitrite;
    private NitriteCollection versionsCollection;
    private Path mvStorePath;

    private CloudNetVersion[] cachedVersions;
    private CloudNetVersion cachedLatestVersion;

    public NitriteDatabase(Path mvStorePath) {
        this.mvStorePath = mvStorePath;
    }

    @Override
    public boolean init() {

        this.nitrite = Nitrite.builder()
                .compressed()
                .filePath(this.mvStorePath.toFile())
                .openOrCreate();

        this.versionsCollection = this.nitrite.getCollection("versions");

        this.cacheVersions();

        return true;
    }

    @Override
    public void registerVersion(CloudNetVersion version) {
        if (Arrays.stream(this.cachedVersions).anyMatch(cloudNetVersion -> cloudNetVersion.getName().equals(version.getName()))) {
            this.versionsCollection.remove(Filters.eq("name", version.getName()));
        }

        Document document = Document.createDocument("name", version.getName());

        document.put("version", version);

        this.versionsCollection.insert(document);

        this.cacheVersions();
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
        this.versionsCollection.find().forEach(document -> versions.add((CloudNetVersion) document.get("version")));
        return versions.toArray(CloudNetVersion[]::new);
    }

    @Override
    public void close() throws IOException {
        this.nitrite.close();
    }
}
