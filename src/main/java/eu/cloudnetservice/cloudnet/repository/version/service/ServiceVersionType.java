package eu.cloudnetservice.cloudnet.repository.version.service;

import java.util.Collection;
import java.util.Optional;

public class ServiceVersionType {

    private String parentVersionName;
    private final String name;
    private final ServiceEnvironment targetEnvironment;
    private final InstallerType installerType;
    private final Collection<ServiceVersion> versions;

    public ServiceVersionType(String parentVersionName, String name, ServiceEnvironment targetEnvironment, InstallerType installerType, Collection<ServiceVersion> versions) {
        this.parentVersionName = parentVersionName;
        this.name = name;
        this.targetEnvironment = targetEnvironment;
        this.installerType = installerType;
        this.versions = versions;
    }

    public Optional<ServiceVersion> getVersion(String name) {
        return this.versions.stream()
                .filter(serviceVersion -> serviceVersion.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    public String getParentVersionName() {
        return this.parentVersionName;
    }

    public void setParentVersionName(String parentVersionName) {
        this.parentVersionName = parentVersionName;
    }

    public String getName() {
        return this.name;
    }

    public ServiceEnvironment getTargetEnvironment() {
        return this.targetEnvironment;
    }

    public InstallerType getInstallerType() {
        return this.installerType;
    }

    public Collection<ServiceVersion> getVersions() {
        return this.versions;
    }

    public enum InstallerType {
        DOWNLOAD(false),
        BUILD(true);

        private final boolean requiresSpecificJavaVersionToExecute;

        InstallerType(boolean requiresSpecificJavaVersionToExecute) {
            this.requiresSpecificJavaVersionToExecute = requiresSpecificJavaVersionToExecute;
        }

        public boolean canInstall(ServiceVersion serviceVersion) {
            return !this.requiresSpecificJavaVersionToExecute || serviceVersion.canRun();
        }

    }

}
