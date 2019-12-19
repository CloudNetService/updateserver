package eu.cloudnetservice.cloudnet.repository.module;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import io.javalin.Javalin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class ModuleRepositoryProvider {

    private Collection<RepositoryModuleInfo> availableModules;

    private String availableModulesJson;

    public ModuleRepositoryProvider(Javalin webServer) {
        if (webServer != null) {
            webServer.get("/api/modules/list", context -> context.result(this.availableModulesJson));
            webServer.get("/api/modules/list/:group",
                    context -> context.result(
                            JsonDocument.newDocument()
                                    .append("modules", this.getModuleInfos(context.pathParam("group")))
                                    .toJson()
                    )
            );
            webServer.get("/api/modules/list/:group/:name",
                    context -> context.result(
                            JsonDocument.newDocument()
                                    .append("modules", this.getModuleInfos(context.pathParam("group"), context.pathParam("name")))
                                    .toJson()
                    )
            );
            webServer.get("/api/modules/file/:group/:name",
                    context -> {/*todo*/}
            );
        }

        this.availableModules = new ArrayList<>(); //todo load from database

        this.availableModules.add(
                new RepositoryModuleInfo(
                        new ModuleId(
                                "eu.cloudnetservice.cloudnet",
                                "CloudNet-Bridge",
                                "3.2.0-SNAPSHOT"
                        ),
                        new String[]{"CloudNetService"},
                        new ModuleId[0],
                        new ModuleId[0],
                        "3.2.0-SNAPSHOT",
                        "...",
                        "https://cloudnetservice.eu",
                        "https://github.com/CloudNetService/CloudNet-v3",
                        "https://discord.cloudnetservice.eu",
                        "https://cloudnetservice.eu/versions/latest"
                )
        );

        this.updateAvailableModules();
    }

    private void updateAvailableModules() {
        this.availableModulesJson = JsonDocument.newDocument()
                .append("modules", this.availableModules)
                .toJson();
    }

    public ModuleRepositoryProvider() {
    }

    public Collection<RepositoryModuleInfo> getModuleInfos(String group) {
        return this.availableModules.stream().filter(moduleInfo -> moduleInfo.getModuleId().getGroup().equalsIgnoreCase(group)).collect(Collectors.toList());
    }

    public RepositoryModuleInfo getModuleInfos(String group, String name) {
        return this.availableModules.stream()
                .filter(moduleInfo -> moduleInfo.getModuleId().getGroup().equalsIgnoreCase(group))
                .filter(moduleInfo -> moduleInfo.getModuleId().getName().equalsIgnoreCase(name))
                .findFirst().orElse(null);
    }

    public RepositoryModuleInfo getModuleInfo(ModuleId moduleId) {
        return this.availableModules.stream()
                .filter(moduleInfo -> moduleId.equals(moduleInfo.getModuleId()))
                .findFirst().orElse(null);
    }

    public void addModule(RepositoryModuleInfo moduleInfo) {
        this.availableModules.add(moduleInfo);
        this.updateAvailableModules();
        //Todo write to database
    }

    public void removeModule(ModuleId moduleId) {
        this.availableModules.stream()
                .filter(moduleInfo -> moduleId.equals(moduleInfo.getModuleId()))
                .findFirst()
                .ifPresent(moduleInfo -> {
                    this.availableModules.remove(moduleInfo);
                    this.updateAvailableModules();
                });
    }

}
