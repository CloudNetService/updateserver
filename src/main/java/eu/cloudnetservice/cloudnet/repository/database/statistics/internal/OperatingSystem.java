package eu.cloudnetservice.cloudnet.repository.database.statistics.internal;

public enum OperatingSystem {

    DEBIAN_8, DEBIAN_9, DEBIAN_10,
    WINDOWS_8, WINDOWS_10,
    OSX,
    WINDOWS_SERVER_2019, WINDOWS_SERVER_2016, WINDOWS_SERVER_2010,
    UBUNTU_18_04, UBUNTU_19_10;

    public static OperatingSystem parseOperatingSystem(String raw) {
        return DEBIAN_10; // todo
    }

}
