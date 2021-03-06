package eu.cloudnetservice.cloudnet.repository.database;

import eu.cloudnetservice.cloudnet.repository.database.statistics.internal.InternalStatistics;
import eu.cloudnetservice.cloudnet.repository.faq.FAQEntry;
import eu.cloudnetservice.cloudnet.repository.github.GitHubReleaseInfo;
import eu.cloudnetservice.cloudnet.repository.module.ModuleId;
import eu.cloudnetservice.cloudnet.repository.module.RepositoryModuleInfo;
import eu.cloudnetservice.cloudnet.repository.version.CloudNetVersion;
import eu.cloudnetservice.cloudnet.repository.version.service.ServiceVersionType;
import eu.cloudnetservice.cloudnet.repository.web.WebPermissionRole;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

public interface Database extends Closeable {

    boolean init();

    void registerVersion(CloudNetVersion version);

    void updateVersion(CloudNetVersion version);

    CloudNetVersion getVersion(String parentVersionName, String name);

    default CloudNetVersion getVersion(String parentVersionName, GitHubReleaseInfo release) {
        return Arrays.stream(this.getAllVersions())
                .filter(Objects::nonNull)
                .filter(version -> version.getParentVersionName().equalsIgnoreCase(parentVersionName))
                .filter(version -> version.getRelease().getId() == release.getId())
                .findFirst()
                .orElse(null);
    }

    CloudNetVersion getLatestVersion(String parentVersionName);

    CloudNetVersion[] getAllVersions();

    CloudNetVersion[] getAllVersions(String parentVersionName);

    FAQEntry[] getFAQEntries(String parentVersionName);

    void insertFAQEntry(FAQEntry entry);

    void updateFAQEntry(FAQEntry entry);

    FAQEntry getFAQEntry(UUID uniqueId);

    void deleteFAQEntry(UUID uniqueId);

    Collection<String> getUserNames();

    boolean checkUserPassword(String username, String password);

    boolean hasPassword(String username);

    boolean containsUser(String username);

    WebPermissionRole getRole(String username);

    void insertUser(String username, String password);

    void insertUser(String username);

    void deleteUser(String username);

    void updateUserRole(String username, WebPermissionRole newPermissionRole);

    void updateUserPassword(String username, String newPassword);

    void insertModuleInfo(RepositoryModuleInfo moduleInfo);

    void updateModuleInfo(RepositoryModuleInfo moduleInfo);

    void removeModuleInfo(RepositoryModuleInfo moduleInfo);

    void removeModuleInfo(String parentVersionName, ModuleId moduleId);

    RepositoryModuleInfo[] getModuleInfos();

    InternalStatistics getStatistics();

    void updateStatistics(InternalStatistics statistics);

    ServiceVersionType[] getServiceVersionTypes(String parentVersionName);

    boolean containsServiceVersionType(String parentVersionName, String name);

    void updateServiceVersionType(ServiceVersionType versionType);

    void insertServiceVersionType(ServiceVersionType versionType);

}
