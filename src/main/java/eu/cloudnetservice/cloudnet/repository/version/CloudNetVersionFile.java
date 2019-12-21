package eu.cloudnetservice.cloudnet.repository.version;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.net.URL;

@EqualsAndHashCode
@ToString
public class CloudNetVersionFile {

    private URL downloadURL;
    private String name;
    private FileType fileType;

    public CloudNetVersionFile(URL downloadURL, String name, FileType fileType) {
        this.downloadURL = downloadURL;
        this.name = name;
        this.fileType = fileType;
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

    public enum FileType {
        CLOUDNET_JAR, CLOUDNET_CNL, MODULE, JAVA_DOCS
    }

}
