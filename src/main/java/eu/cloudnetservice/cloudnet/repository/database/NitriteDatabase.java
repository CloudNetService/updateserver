package eu.cloudnetservice.cloudnet.repository.database;

import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;
import org.dizitart.no2.*;

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
        Document document = Document.createDocument("name", version.getName());
        document.put("version", version);

        this.versionsCollection.insert(document);

        this.cacheVersions();
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
