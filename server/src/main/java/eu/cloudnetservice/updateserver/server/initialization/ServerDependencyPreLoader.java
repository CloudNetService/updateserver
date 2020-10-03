/*
 * Licensed to CloudNetService under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.cloudnetservice.updateserver.server.initialization;

import eu.cloudnetservice.updateserver.launcher.ServerClassLoader;
import eu.cloudnetservice.updateserver.server.UpdateServerApplication;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author derklaro
 * @version 1.0
 * @since 2. October 2020
 */
public final class ServerDependencyPreLoader {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("##.###");
    private static final String DEPENDENCY_DOWNLOAD_DONE_FORMAT = "Successfully completed download of dependency \"%s\" after %ss %n";
    private static final String DEPENDENCY_LOAD_FORMAT = "Loaded dependency %s:%s version %s %n";
    private static final String DEPENDENCY_DOWNLOAD_FORMAT = "Trying to download non-existing dependency \"%s\" version %s from %s (%s)... %n";

    public static void main(String[] args) {
        if (!(Thread.currentThread().getContextClassLoader() instanceof ServerClassLoader)) {
            System.err.println("System class loader is not the launcher class loader. Ensure the server is launched using the launcher.");
            return;
        }

        var dependencyFileInputStream = ServerDependencyPreLoader.class.getClassLoader().getResourceAsStream("dependencies.txt");
        if (dependencyFileInputStream == null) {
            System.err.println("Unable to locate the dependencies.txt file. This file is required, please check your build lifecycle.");
            return;
        }

        final List<String> dependencyLines;
        try (var reader = new BufferedReader(new InputStreamReader(dependencyFileInputStream, StandardCharsets.UTF_8))) {
            dependencyLines = reader.lines().collect(Collectors.toList());
        } catch (IOException exception) {
            throw new RuntimeException("Unable to read dependencies file!", exception);
        }

        final List<Dependency> dependencies;
        try {
            dependencies = collectDependenciesFromFileLines(dependencyLines);
        } catch (Exception exception) {
            throw new RuntimeException("Unable to collect dependencies from file lines " + String.join(";", dependencyLines), exception);
        }

        for (Dependency dependency : dependencies) {
            if (dependency.notExists()) {
                try {
                    Path parent = dependency.artifactPath.getParent();
                    if (parent != null && Files.notExists(parent)) {
                        Files.createDirectories(parent);
                    }

                    System.out.printf(DEPENDENCY_DOWNLOAD_FORMAT, dependency.dependencyArtifactId, dependency.dependencyVersion, dependency.repositoryId, dependency.repositoryUrl);
                    long start = System.currentTimeMillis();
                    downloadDependency(dependency);
                    System.out.printf(DEPENDENCY_DOWNLOAD_DONE_FORMAT, dependency.dependencyArtifactId, DECIMAL_FORMAT.format((System.currentTimeMillis() - start) / 1000D));
                } catch (IOException exception) {
                    throw new RuntimeException("Unable to download dependency " + dependency.dependencyArtifactId + " from " + dependency.repositoryUrl, exception);
                }
            }
        }

        ServerClassLoader classLoader = (ServerClassLoader) Thread.currentThread().getContextClassLoader();
        for (Dependency dependency : dependencies) {
            try {
                classLoader.addURL(dependency.artifactPath.toUri().toURL());
                System.out.printf(DEPENDENCY_LOAD_FORMAT, dependency.dependencyGroupId, dependency.dependencyArtifactId, dependency.dependencyVersion);
            } catch (IOException exception) {
                throw new RuntimeException("Unable to add dependency " + dependency.dependencyArtifactId, exception);
            }
        }

        System.out.println();
        System.out.println("All dependencies were loaded successfully - Booting server...");
        UpdateServerApplication.main(args);
    }

    @NotNull
    private static List<Dependency> collectDependenciesFromFileLines(@NotNull List<String> lines) {
        return lines.stream().filter(line -> !line.trim().isBlank()).map(Dependency::parseFromString).collect(Collectors.toList());
    }

    private static void downloadDependency(@NotNull Dependency dependency) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(dependency.downloadUrl).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11"
        );
        connection.connect();

        try (var stream = connection.getInputStream()) {
            Files.copy(stream, dependency.artifactPath, StandardCopyOption.REPLACE_EXISTING); // replace existing to prevent bugs (which should never appear!)
        }

        connection.disconnect();
    }

    private static final class Dependency {

        @NotNull
        private static Dependency parseFromString(@NotNull String line) {
            String[] parts = line.split(" ");
            String[] repositoryParts = parts[0].split("=>");
            String[] artifactInformationParts = parts[1].split(":");

            return new Dependency(repositoryParts[0], repositoryParts[1], artifactInformationParts[0], artifactInformationParts[1], artifactInformationParts[2]);
        }

        private final String repositoryId;
        private final String repositoryUrl;

        private final String dependencyGroupId;
        private final String dependencyArtifactId;
        private final String dependencyVersion;

        // generated information
        private final Path artifactPath;
        private final String downloadUrl;

        public Dependency(String repositoryId, String repositoryUrl, String dependencyGroupId, String dependencyArtifactId, String dependencyVersion) {
            this.repositoryId = repositoryId;
            this.repositoryUrl = repositoryUrl;
            this.dependencyGroupId = dependencyGroupId;
            this.dependencyArtifactId = dependencyArtifactId;
            this.dependencyVersion = dependencyVersion;

            this.artifactPath = Path.of(
                // head directory in which all dependencies are located
                "dependencies",
                // dependency information
                dependencyGroupId.replace(".", "/"),
                dependencyArtifactId,
                dependencyVersion,
                // formatted jar file name
                dependencyArtifactId + '-' + dependencyVersion + ".jar"
            );
            this.downloadUrl = repositoryUrl + "/" // Will never end with '/' as the dependency list plugin removes it
                + dependencyGroupId.replace(".", "/") + "/"
                + dependencyArtifactId + "/"
                + dependencyVersion + "/"
                + dependencyArtifactId + "-" + dependencyVersion + ".jar";
        }

        private boolean notExists() {
            return Files.notExists(this.artifactPath);
        }
    }
}
