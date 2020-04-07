package eu.cloudnetservice.cloudnet.repository.version;

import eu.cloudnetservice.cloudnet.repository.config.CloudNetVersionFileInfo;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CloudNetParentVersion {

    private String name;
    private String mavenGroupId;

    private String updateRepositoryPath;

    private String jenkinsBaseURL;

    private String gitHubApiURL;
    private String gitHubSecret;
    private String gitHubWebHookPath;

    private Collection<CloudNetVersionFileInfo> additionalVersionFiles;

    private String moduleFileName;

    private Map<String, Object> properties;
    private VersionFileMappings defaultVersionFileMappings;

    public CloudNetParentVersion(String name, String mavenGroupId, String updateRepositoryPath, String jenkinsBaseURL, String gitHubApiURL,
                                 String gitHubSecret, String gitHubWebHookPath, Collection<CloudNetVersionFileInfo> additionalVersionFiles,
                                 String moduleFileName, VersionFileMappings defaultVersionFileMappings) {
        this.name = name;
        this.mavenGroupId = mavenGroupId;
        this.updateRepositoryPath = updateRepositoryPath;
        this.jenkinsBaseURL = jenkinsBaseURL;
        this.gitHubApiURL = gitHubApiURL;
        this.gitHubSecret = gitHubSecret;
        this.gitHubWebHookPath = gitHubWebHookPath;
        this.additionalVersionFiles = additionalVersionFiles;
        this.moduleFileName = moduleFileName;
        this.defaultVersionFileMappings = defaultVersionFileMappings;
        this.properties = new HashMap<>();
    }

    public String getName() {
        return this.name;
    }

    public String getMavenGroupId() {
        return this.mavenGroupId;
    }

    public String getUpdateRepositoryPath() {
        return this.updateRepositoryPath;
    }

    public String getJenkinsBaseURL() {
        return this.jenkinsBaseURL;
    }

    public String getGitHubApiURL() {
        return this.gitHubApiURL;
    }

    public String getGitHubSecret() {
        return this.gitHubSecret;
    }

    public String getGitHubWebHookPath() {
        return this.gitHubWebHookPath;
    }

    public Collection<CloudNetVersionFileInfo> getAdditionalVersionFiles() {
        return this.additionalVersionFiles;
    }

    public String getModuleFileName() {
        return this.moduleFileName;
    }

    public Map<String, Object> getProperties() {
        return this.properties != null ? this.properties : (this.properties = new HashMap<>());
    }

    public VersionFileMappings getDefaultVersionFileMappings() {
        return this.defaultVersionFileMappings;
    }

}
