/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.gradle;

import com.codename1.build.BuildArtifact;
import com.codename1.build.Log;
import com.codename1.build.ProjectHost;
import com.codename1.project.ProjectLayout;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/// The build engine's [ProjectHost] for a Gradle project.
///
/// Built inside a task action from the task's own inputs -- plain strings and
/// files -- never from the `Project`, so every task that uses it stays
/// compatible with the configuration cache.
final class GradleProjectHost implements ProjectHost {
    private final Log log;
    private final ProjectLayout layout;
    private final String finalName;
    private final String groupId;
    private final Properties projectProperties;
    private final Properties userProperties;
    private final List<String> classpath;
    private final List<String> sourceRoots;
    private final List<BuildArtifact> artifacts;
    private final Map<String, File> frameworkJars;
    private final String codenameOneVersion;

    GradleProjectHost(Log log, ProjectLayout layout, String finalName, String groupId,
                      Map<String, String> projectProperties, Map<String, String> userProperties,
                      List<String> classpath, List<String> sourceRoots, List<String> encodedArtifacts,
                      Map<String, File> frameworkJars, String codenameOneVersion) {
        this.log = log;
        this.layout = layout;
        this.finalName = finalName;
        this.groupId = groupId;
        this.projectProperties = toProperties(projectProperties);
        this.userProperties = toProperties(userProperties);
        this.classpath = classpath == null ? Collections.<String>emptyList() : classpath;
        this.sourceRoots = sourceRoots == null ? Collections.<String>emptyList() : sourceRoots;
        this.artifacts = new ArrayList<BuildArtifact>();
        if (encodedArtifacts != null) {
            for (String encoded : encodedArtifacts) {
                BuildArtifact a = decode(encoded);
                if (a != null) {
                    this.artifacts.add(a);
                }
            }
        }
        this.frameworkJars = frameworkJars == null ? Collections.<String, File>emptyMap() : frameworkJars;
        this.codenameOneVersion = codenameOneVersion;
    }

    private static Properties toProperties(Map<String, String> map) {
        Properties p = new Properties();
        if (map != null) {
            for (Map.Entry<String, String> e : map.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    p.setProperty(e.getKey(), e.getValue());
                }
            }
        }
        return p;
    }

    /// `group|artifact|version|classifier|type|scope|path`, the form a task
    /// input carries a resolved dependency in.
    static String encode(ResolvedArtifactResult result, String scope) {
        ComponentIdentifier id = result.getId().getComponentIdentifier();
        File file = result.getFile();
        if (!(id instanceof ModuleComponentIdentifier)) {
            return null;
        }
        ModuleComponentIdentifier m = (ModuleComponentIdentifier) id;
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        String type = dot < 0 ? "jar" : name.substring(dot + 1);
        String base = dot < 0 ? name : name.substring(0, dot);
        String prefix = m.getModule() + "-" + m.getVersion();
        String classifier = base.startsWith(prefix + "-") ? base.substring(prefix.length() + 1) : "";
        return m.getGroup() + "|" + m.getModule() + "|" + m.getVersion() + "|" + classifier + "|" + type + "|"
                + scope + "|" + file.getAbsolutePath();
    }

    static BuildArtifact decode(String encoded) {
        if (encoded == null) {
            return null;
        }
        String[] p = encoded.split("\\|", 7);
        if (p.length < 7) {
            return null;
        }
        return new BuildArtifact(p[0], p[1], p[2], p[3].isEmpty() ? null : p[3], p[4], p[5], new File(p[6]),
                null);
    }

    @Override
    public Log log() {
        return log;
    }

    @Override
    public ProjectLayout layout() {
        return layout;
    }

    @Override
    public File cn1ProjectDir() {
        return layout.projectDir();
    }

    @Override
    public File baseDir() {
        return layout.projectDir();
    }

    @Override
    public File buildDirectory() {
        return layout.buildDir();
    }

    @Override
    public File outputDirectory() {
        return layout.classesDir();
    }

    @Override
    public String finalName() {
        return finalName;
    }

    @Override
    public String groupId() {
        return groupId;
    }

    @Override
    public Properties projectProperties() {
        return projectProperties;
    }

    @Override
    public Properties userProperties() {
        return userProperties;
    }

    @Override
    public List<String> compileClasspathElements() {
        return new ArrayList<String>(classpath);
    }

    @Override
    public List<String> runtimeClasspathElements() {
        return new ArrayList<String>(classpath);
    }

    @Override
    public List<String> compileSourceRoots() {
        return sourceRoots;
    }

    @Override
    public Collection<BuildArtifact> artifacts() {
        return artifacts;
    }

    /// Gradle keeps the desktop runtime binaries off the application's
    /// classpaths entirely (only the simulator gets them), so nothing reached
    /// through that aggregator is ever staged and there is nothing to decide.
    @Override
    public Set<String> neededWithoutDesktopRuntime() {
        return null;
    }

    @Override
    public File getJar(BuildArtifact artifact) {
        return artifact.getFile();
    }

    @Override
    public File getJar(String groupId, String artifactId, String classifier) {
        File f = frameworkJars.get(groupId + ":" + artifactId + (classifier == null ? "" : ":" + classifier));
        if (f != null) {
            return f;
        }
        for (BuildArtifact a : artifacts) {
            if (a.getGroupId().equals(groupId) && a.getArtifactId().equals(artifactId)
                    && (classifier == null ? a.getClassifier() == null : classifier.equals(a.getClassifier()))) {
                return a.getFile();
            }
        }
        return null;
    }

    @Override
    public long sessionStartTime() {
        return Long.MAX_VALUE;
    }

    /// The newest input a cached native output (an APK, a generated Xcode or
    /// Android Studio project) was built from: the sources, the build files, every
    /// file on the upload classpath, and the build hints. Files are compared by timestamp;
    /// the build hints are not files -- they arrive as `codename1.*` Gradle
    /// properties, from `gradle.properties`, `-P` or the build script -- so
    /// their fingerprint is recorded beside the build output, and a change in it
    /// counts as a change now. Without it a hint edited in gradle.properties
    /// returned the artifact built with the old one.
    @Override
    public long sourcesModificationTime() {
        long t = lastModified(new File(layout.projectDir(), "src"));
        t = Math.max(t, layout.settingsFile().lastModified());
        t = Math.max(t, layout.dependencyFile().lastModified());
        t = Math.max(t, layout.rootBuildFile().lastModified());
        // The dependencies too: a jar replaced in place (a converted Ant project's
        // libs/foo.jar) edits no build file, and the cached APK or generated
        // project still held the old code.
        for (String element : classpath) {
            t = Math.max(t, lastModified(new File(element)));
        }
        return Math.max(t, hintsChangedAt());
    }

    /// When the build hints last changed, as the fingerprint file's timestamp.
    private long hintsChangedAt() {
        File fingerprint = new File(layout.buildDir(), "codenameone" + File.separator + "build-hints.fingerprint");
        String current = hintsFingerprint(userProperties);
        try {
            String previous = fingerprint.isFile()
                    ? new String(java.nio.file.Files.readAllBytes(fingerprint.toPath()), StandardCharsets.UTF_8)
                    : null;
            if (!current.equals(previous)) {
                File dir = fingerprint.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    throw new IOException("Could not create " + dir);
                }
                java.nio.file.Files.write(fingerprint.toPath(), current.getBytes(StandardCharsets.UTF_8));
            }
            return fingerprint.lastModified();
        } catch (IOException ex) {
            // Unknown, so treat the hints as just changed: rebuilding is safe,
            // reusing a stale artifact is not.
            log.warn("Could not record the build hint fingerprint in " + fingerprint + ": " + ex.getMessage());
            return System.currentTimeMillis();
        }
    }

    /// A SHA-256 of `properties`, sorted, so equal hints give equal digests. A
    /// digest, not the text: hints carry signing passwords and tokens, which
    /// have no business in the build directory.
    static String hintsFingerprint(Properties properties) {
        StringBuilder sb = new StringBuilder();
        for (String key : new java.util.TreeSet<String>(properties.stringPropertyNames())) {
            sb.append(key).append('=').append(properties.getProperty(key)).append('\n');
        }
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required of every JVM", ex);
        }
    }

    private static long lastModified(File f) {
        if (!f.exists()) {
            return 0L;
        }
        if (!f.isDirectory()) {
            return f.lastModified();
        }
        long t = f.lastModified();
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                t = Math.max(t, lastModified(c));
            }
        }
        return t;
    }

    /// Gradle has no attached-artifact concept; the file stays where the build
    /// wrote it, under the build directory, and is reported.
    @Override
    public void attachArtifact(String type, String classifier, File file) {
        log.info("Build output: " + file.getAbsolutePath());
    }

    @Override
    public String codenameOneVersion() {
        return codenameOneVersion;
    }

    @Override
    public String pluginVersion() {
        return PluginInfo.version();
    }
}
