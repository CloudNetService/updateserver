package eu.cloudnetservice.cloudnet.repository.config;

import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersionFile;

public class CloudNetVersionFileInfo {
    private String path;
    private String name;
    private CloudNetVersionFile.FileType fileType;

    public CloudNetVersionFileInfo(String path, String name, CloudNetVersionFile.FileType fileType) {
        this.path = path;
        this.name = name;
        this.fileType = fileType;
    }

    public String getPath() {
        return this.path;
    }

    public String getName() {
        return this.name;
    }

    public CloudNetVersionFile.FileType getFileType() {
        return this.fileType;
    }

}
