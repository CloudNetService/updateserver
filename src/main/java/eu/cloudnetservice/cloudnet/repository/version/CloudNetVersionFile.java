package eu.cloudnetservice.cloudnet.repository.version;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.io.Serializable;
import java.net.URL;

@EqualsAndHashCode
@ToString
public class CloudNetVersionFile implements Serializable {

    private transient URL downloadURL;
    private String name;
    private FileType fileType;
    private MavenVersionInfo versionInfo;

    public CloudNetVersionFile(URL downloadURL, String name, FileType fileType, MavenVersionInfo versionInfo) {
        this.downloadURL = downloadURL;
        this.name = name;
        this.fileType = fileType;
        this.versionInfo = versionInfo;
    }

    public URL getDownloadURL() {
        return this.downloadURL;
    }

    public String getName() {
        return this.name;
    }

    public FileType getFileType() {
        return this.fileType;
    }

    public MavenVersionInfo getVersionInfo() {
        return this.versionInfo;
    }

    public enum FileType {
        CLOUDNET_ZIP, CLOUDNET_JAR, CLOUDNET_CNL, MODULE, JAVA_DOCS
    }

}
