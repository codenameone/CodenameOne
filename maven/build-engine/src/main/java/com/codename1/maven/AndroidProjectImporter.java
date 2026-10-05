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
package com.codename1.maven;

import com.codename1.build.Log;
import com.codename1.builders.BuildException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Imports an Android Studio module into a Codename One application: copies
/// its `src/main` into `src/main/android`, carries the namespace into the
/// manifest when the Gradle build declares it there instead, points the
/// Codename One entry point at the generated Android application, and reports
/// which Gradle dependencies the compatibility runtime covers.
public final class AndroidProjectImporter {

    /// Gradle coordinates the runtime implements, by group:artifact prefix.
    static final Map<String, String> COVERED = new LinkedHashMap<String, String>();

    static {
        COVERED.put("androidx.appcompat:appcompat", "AppCompat");
        COVERED.put("com.google.android.material:material", "Material Components");
        COVERED.put("androidx.recyclerview:recyclerview", "RecyclerView");
        COVERED.put("androidx.viewpager2:viewpager2", "ViewPager2");
        COVERED.put("androidx.constraintlayout:constraintlayout", "ConstraintLayout");
        COVERED.put("androidx.constraintlayout:constraintlayout-core", "ConstraintLayout");
        COVERED.put("androidx.fragment:fragment", "Fragments");
        COVERED.put("androidx.fragment:fragment-ktx", "Fragments; its Kotlin extensions are not provided (no by viewModels(), commit { })");
        COVERED.put("androidx.core:core", "AndroidX core");
        COVERED.put("androidx.core:core-ktx", "AndroidX core; its Kotlin extensions are not provided");
        COVERED.put("androidx.activity:activity", "AndroidX activity");
        COVERED.put("androidx.activity:activity-ktx", "AndroidX activity; its Kotlin extensions are not provided");
        COVERED.put("androidx.lifecycle:lifecycle-common", "Lifecycle");
        COVERED.put("androidx.lifecycle:lifecycle-runtime", "Lifecycle");
        COVERED.put("androidx.lifecycle:lifecycle-runtime-ktx", "Lifecycle; its Kotlin extensions are not provided (no lifecycleScope)");
        COVERED.put("androidx.lifecycle:lifecycle-viewmodel", "ViewModel");
        COVERED.put("androidx.lifecycle:lifecycle-viewmodel-ktx", "ViewModel; its Kotlin extensions are not provided (no viewModelScope)");
        COVERED.put("androidx.lifecycle:lifecycle-livedata", "LiveData");
        COVERED.put("androidx.lifecycle:lifecycle-livedata-ktx", "LiveData; its Kotlin extensions are not provided");
        COVERED.put("androidx.lifecycle:lifecycle-livedata-core", "LiveData");
        COVERED.put("androidx.lifecycle:lifecycle-viewmodel-savedstate", "ViewModel");
        COVERED.put("androidx.lifecycle:lifecycle-process", "Lifecycle");
        COVERED.put("androidx.savedstate:savedstate", "Saved state");
        COVERED.put("androidx.annotation:annotation", "annotations");
        COVERED.put("androidx.cardview:cardview", "CardView");
        COVERED.put("org.jetbrains.kotlin:kotlin-stdlib", "Kotlin standard library");
        COVERED.put("org.jetbrains.kotlin:kotlin-stdlib-jdk7", "Kotlin standard library");
        COVERED.put("org.jetbrains.kotlin:kotlin-stdlib-jdk8", "Kotlin standard library");
    }

    /// Dependencies that only matter to Android tooling and tests.
    static final String[] IGNORED = {"junit:", "androidx.test", "org.robolectric", "org.mockito",
        "androidx.compose.ui:ui-tooling", "com.android.tools"};

    public static final class Result {
        public final List<String> covered = new ArrayList<String>();
        public final List<String> uncovered = new ArrayList<String>();
        public String namespace;
        public String applicationId;
        public String versionName;
        public String versionCode;
        public boolean kotlin;
        public int copiedFiles;
    }

    private final Log log;

    public AndroidProjectImporter(Log log) {
        this.log = log;
    }

    /// Finds the module's `src/main`: `source` itself, `source/src/main`, or
    /// `source/<module>/src/main`; null when none has a manifest.
    public static File mainDir(File source, String module) {
        if (new File(source, "AndroidManifest.xml").isFile()) {
            return source;
        }
        File m = new File(source, "src/main");
        if (new File(m, "AndroidManifest.xml").isFile()) {
            return m;
        }
        m = new File(source, (module == null ? "app" : module) + "/src/main");
        if (new File(m, "AndroidManifest.xml").isFile()) {
            return m;
        }
        return null;
    }

    public Result importProject(File source, String module, File commonDir, String mainPackage, String mainClass)
            throws BuildException {
        File main = mainDir(source, module);
        if (main == null) {
            throw new BuildException("No AndroidManifest.xml under " + source + " (looked in ., src/main and "
                    + (module == null ? "app" : module) + "/src/main)");
        }
        Result r = new Result();
        File target = new File(commonDir, "src/main/android");
        try {
            for (String name : new String[] {"AndroidManifest.xml", "res", "assets", "java", "kotlin"}) {
                File src = new File(main, name);
                if (src.exists()) {
                    r.copiedFiles += copy(src, new File(target, name));
                }
            }
            File moduleDir = main.getParentFile().getParentFile();
            File gradle = new File(moduleDir, "build.gradle.kts");
            if (!gradle.isFile()) {
                gradle = new File(moduleDir, "build.gradle");
            }
            if (gradle.isFile()) {
                readGradle(new String(Files.readAllBytes(gradle.toPath()), Charset.forName("UTF-8")), r);
            }
            File manifest = new File(target, "AndroidManifest.xml");
            String m = new String(Files.readAllBytes(manifest.toPath()), Charset.forName("UTF-8"));
            // The Android Gradle plugin merges these from the build script into
            // the manifest it packages; the copy gets the same, unless the
            // manifest already says otherwise.
            String merged = m;
            if (r.namespace != null) {
                merged = addManifestAttribute(merged, "package", r.namespace);
            }
            if (r.versionCode != null) {
                merged = addManifestAttribute(merged, "android:versionCode", r.versionCode);
            }
            if (r.versionName != null) {
                merged = addManifestAttribute(merged, "android:versionName", r.versionName);
            }
            if (!merged.equals(m)) {
                Files.write(manifest.toPath(), merged.getBytes(Charset.forName("UTF-8")));
                log.info("Added the Gradle namespace and version to the copied manifest");
            }
            if (mainPackage != null && mainClass != null) {
                writeEntryPoint(new File(commonDir, "src/main/java"), mainPackage, mainClass);
            }
            if (hasKotlin(target)) {
                // The application's Kotlin build (its "kotlin" profile, or the
                // Gradle conversion's Kotlin plugin) switches on when
                // src/main/kotlin exists; it compiles the Android sources too.
                File kotlinDir = new File(commonDir, "src/main/kotlin");
                if (!kotlinDir.isDirectory()) {
                    if (!kotlinDir.mkdirs()) {
                        throw new IOException("Cannot create " + kotlinDir);
                    }
                    Files.write(new File(kotlinDir, ".keep").toPath(), new byte[0]);
                    log.info("The Android sources include Kotlin: created " + kotlinDir
                            + " so the application compiles Kotlin");
                }
                r.kotlin = true;
            }
        } catch (IOException e) {
            throw new BuildException("Import failed: " + e.getMessage(), e);
        }
        return r;
    }

    static boolean hasKotlin(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return false;
        }
        for (File f : files) {
            if (f.isDirectory() ? hasKotlin(f) : f.getName().endsWith(".kt")) {
                return true;
            }
        }
        return false;
    }

    /// Adds `name="value"` to the `<manifest>` element when it has no such
    /// attribute.
    static String addManifestAttribute(String manifest, String name, String value) {
        Matcher open = Pattern.compile("<manifest\\b[^>]*>").matcher(manifest);
        if (!open.find()) {
            return manifest;
        }
        String tag = open.group();
        if (Pattern.compile("\\s" + Pattern.quote(name) + "\\s*=").matcher(tag).find()) {
            return manifest;
        }
        return manifest.substring(0, open.start()) + "<manifest " + name + "=\"" + value + "\""
                + tag.substring("<manifest".length()) + manifest.substring(open.end());
    }

    private void writeEntryPoint(File javaDir, String pkg, String cls) throws IOException {
        File f = new File(javaDir, pkg.replace('.', File.separatorChar) + File.separator + cls + ".java");
        f.getParentFile().mkdirs();
        String src = "package " + pkg + ";\n\n"
                + "/// The Codename One entry point: starts the imported Android application's launcher activity.\n"
                + "public class " + cls + " extends com.codename1.androidcompat.runtime.AndroidLifecycle {\n"
                + "    public " + cls + "() {\n"
                + "        super(new com.codename1.generated.android.AndroidAppImpl());\n"
                + "    }\n"
                + "}\n";
        byte[] bytes = src.getBytes(Charset.forName("UTF-8"));
        if (f.isFile()) {
            File backup = new File(f.getPath() + ".pre-android-import");
            if (backup.exists()) {
                // A repeated import: the backup already holds the application's
                // original class and the file is what an earlier import wrote.
                // Overwriting the backup would lose the only copy of the original.
                log.info("Kept the existing " + backup.getName() + " from an earlier import");
            } else if (!java.util.Arrays.equals(bytes, Files.readAllBytes(f.toPath()))) {
                Files.copy(f.toPath(), backup.toPath());
                log.info("Saved the previous " + f.getName() + " as " + backup.getName());
            }
        }
        Files.write(f.toPath(), bytes);
    }

    static void readGradle(String gradle, Result r) {
        Matcher ns = Pattern.compile("namespace\\s*=?\\s*[\"']([^\"']+)[\"']").matcher(gradle);
        if (ns.find()) {
            r.namespace = ns.group(1);
        }
        Matcher app = Pattern.compile("applicationId\\s*=?\\s*[\"']([^\"']+)[\"']").matcher(gradle);
        if (app.find()) {
            r.applicationId = app.group(1);
        }
        Matcher ver = Pattern.compile("versionName\\s*=?\\s*[\"']([^\"']+)[\"']").matcher(gradle);
        if (ver.find()) {
            r.versionName = ver.group(1);
        }
        Matcher code = Pattern.compile("versionCode\\s*=?\\s*(\\d+)").matcher(gradle);
        if (code.find()) {
            r.versionCode = code.group(1);
        }
        Matcher dep = Pattern.compile("(?:implementation|api|compileOnly)\\s*\\(?\\s*[\"']([^\"':]+:[^\"':]+)(?::[^\"']*)?[\"']")
                .matcher(gradle);
        while (dep.find()) {
            String coord = dep.group(1);
            boolean ignored = false;
            for (String i : IGNORED) {
                if (coord.startsWith(i)) {
                    ignored = true;
                }
            }
            if (ignored) {
                continue;
            }
            // The pattern captured the whole group:artifact, so the lookup is exact:
            // a prefix match reported androidx.core:core-splashscreen as covered by
            // androidx.core:core and suppressed the warning for a library the
            // runtime does not implement.
            String covered = COVERED.get(coord);
            if (covered != null) {
                r.covered.add(coord + " (" + covered + ")");
            } else {
                r.uncovered.add(coord);
            }
        }
        if (gradle.contains("libs.")) {
            r.uncovered.add("dependencies declared through a version catalog (libs.*) were not resolved; "
                    + "check them against the supported list by hand");
        }
    }

    private static int copy(File src, File dest) throws IOException {
        if (src.isDirectory()) {
            int n = 0;
            File[] files = src.listFiles();
            if (files != null) {
                for (File f : files) {
                    n += copy(f, new File(dest, f.getName()));
                }
            }
            return n;
        }
        dest.getParentFile().mkdirs();
        Files.copy(src.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return 1;
    }
}
