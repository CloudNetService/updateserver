package eu.cloudnetservice.cloudnet.repository.loader;

import java.net.URL;

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
