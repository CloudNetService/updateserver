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
package eu.cloudnetservice.updateserver.launcher;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarFile;

/**
 * @author derklaro
 * @version 1.0
 * @since 2. October 2020
 */
public final class UpdateServerApplicationLauncher {

    private static final Path APPLICATION_FILE = Path.of("dependencies", "server.jar");

    public static synchronized void main(String[] args) {
        try (var serverJarStream = UpdateServerApplicationLauncher.class.getClassLoader().getResourceAsStream("server.jar")) {
            if (serverJarStream == null) {
                System.err.println("Missing compiled server.jar file! Please recheck your build lifecycle and retry");
                return;
            }

            var parentPath = APPLICATION_FILE.getParent();
            if (parentPath != null && Files.notExists(parentPath)) {
                Files.createDirectories(parentPath);
            }

            Files.copy(serverJarStream, APPLICATION_FILE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new RuntimeException("Unable to copy server jar file!", exception);
        }

        try (var jarfile = new JarFile(APPLICATION_FILE.toFile())) {
            runApplication(jarfile, args);
        } catch (IOException exception) {
            throw new RuntimeException("Unable to open application file located at " + APPLICATION_FILE.toString() + " for reading", exception);
        }
    }

    private static void runApplication(@NotNull JarFile jarFile, @NotNull String[] args) {
        final ClassLoader classLoader;
        try {
            classLoader = new ServerClassLoader(new URL[]{APPLICATION_FILE.toUri().toURL()});
            Thread.currentThread().setContextClassLoader(classLoader);
        } catch (IOException exception) {
            throw new RuntimeException("Unable to create launcher class loader", exception);
        }

        final String mainClass;
        try {
            mainClass = jarFile.getManifest().getMainAttributes().getValue("Main-Class");
        } catch (IOException exception) {
            throw new RuntimeException("Unable to read main class of application file " + APPLICATION_FILE.toString(), exception);
        }

        final Method mainMethod;
        try {
            mainMethod = classLoader.loadClass(mainClass).getMethod("main", String[].class);
        } catch (ClassNotFoundException exception) {
            throw new RuntimeException("Unable to locate main class " + mainClass, exception);
        } catch (NoSuchMethodException exception) {
            throw new RuntimeException("Class " + mainClass + " and superclasses does not contain a main method");
        }

        if (!Modifier.isStatic(mainMethod.getModifiers())) {
            throw new RuntimeException("Found main method " + mainMethod.getName() + " in " + mainClass + " but method is not static");
        }

        if (!mainMethod.getReturnType().equals(Void.TYPE)) {
            throw new RuntimeException("Found main method " + mainMethod.getName() + " in " + mainClass + " but method does not return void");
        }

        try {
            mainMethod.invoke(null, (Object) args);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new RuntimeException("Unable to start server application", exception);
        }
    }
}
