package eu.cloudnetservice.cloudnet.repository.module;

public class RepositoryModuleInfo {

    private ModuleId moduleId;
    private String[] authors;
    private ModuleId[] depends;
    private ModuleId[] conflicts;
    private String parentVersionName;
    private String requiredCloudNetVersion;
    private String description;
    private String website;
    private String sourceUrl;
    private String supportUrl;

    public RepositoryModuleInfo(ModuleId moduleId, String[] authors, ModuleId[] depends, ModuleId[] conflicts,
                                String parentVersionName, String requiredCloudNetVersion, String description,
                                String website, String sourceUrl, String supportUrl) {
        this.moduleId = moduleId;
        this.authors = authors;
        this.depends = depends;
        this.conflicts = conflicts;
        this.parentVersionName = parentVersionName;
        this.requiredCloudNetVersion = requiredCloudNetVersion;
        this.description = description;
        this.website = website;
        this.sourceUrl = sourceUrl;
        this.supportUrl = supportUrl;
    }

    public void setModuleId(ModuleId moduleId) {
        this.moduleId = moduleId;
    }

    public ModuleId getModuleId() {
        return this.moduleId;
    }

    public String getParentVersionName() {
        return this.parentVersionName;
    }
    
    public String getRequiredCloudNetVersion() {
        return this.requiredCloudNetVersion;
    }

    public ModuleId[] getConflicts() {
        return this.conflicts;
    }

    public String[] getAuthors() {
        return this.authors;
    }

    public ModuleId[] getDepends() {
        return this.depends;
    }

    public String getWebsite() {
        return this.website;
    }

    public String getSourceUrl() {
        return this.sourceUrl;
    }

    public String getSupportUrl() {
        return this.supportUrl;
    }

    public String getDescription() {
        return this.description;
    }

}
