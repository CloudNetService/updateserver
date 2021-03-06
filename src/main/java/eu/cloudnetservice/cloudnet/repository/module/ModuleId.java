package eu.cloudnetservice.cloudnet.repository.module;

import java.util.Objects;

public class ModuleId {

    private String group;
    private String name;
    private String version;

    public ModuleId(String group, String name, String version) {
        this.group = group;
        this.name = name;
        this.version = version != null ? version : "latest";
    }

    public ModuleId(String group, String name) {
        this(group, name, null);
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public ModuleId ignoreVersion() {
        return new ModuleId(this.group, this.name);
    }

    @Override
    public String toString() {
        return this.version != null ?
                this.group + ":" + this.name + ":" + this.version :
                this.group + ":" + this.name;
    }

    public boolean equalsIgnoreVersion(ModuleId moduleId) {
        return group.equals(moduleId.group) && name.equals(moduleId.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleId moduleId = (ModuleId) o;
        return group.equals(moduleId.group) &&
                name.equals(moduleId.name) &&
                Objects.equals(version, moduleId.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, name, version);
    }

    public static ModuleId parse(String rawModuleId) {
        String[] split = rawModuleId.split(":");
        if (split.length != 2 && split.length != 3) {
            return null;
        }
        return new ModuleId(split[0], split[1], split.length == 3 ? split[2] : null);
    }

}
