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

import com.codename1.android.rescompiler.Diagnostic;
import com.codename1.android.rescompiler.ResourceCompiler;
import com.codename1.build.Log;
import com.codename1.builders.BuildException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Compiles an application's Android sources (`src/main/android`): the res
/// tree and manifest into R classes, the resource table and the generated
/// factory, and the assets into flat resources. Shared by the Maven goal
/// `compile-android-res` and the Gradle task.
///
/// `src/main/android` mirrors an Android Studio module's `src/main`:
///
/// ```
/// AndroidManifest.xml   res/**   assets/**   java/**
/// ```
public class AndroidResourceRunner {

    /// The runtime artifact that carries the framework symbols.
    public static final String COMPAT_ARTIFACT = "codenameone-android-compat";

    private final File androidDir;
    private final File javaOut;
    private final File resourcesOut;
    private final File buildDir;
    private final File compatJar;
    private final String namespace;
    private final String mainPackage;
    private final String mainClass;
    private final List<File> sourceRoots;
    private final Log log;

    /// @param androidDir    `src/main/android`
    /// @param javaOut       generated sources (`target/generated-sources/android`)
    /// @param resourcesOut  where the table and flat files go (the classes directory)
    /// @param buildDir      `target`, for the state and onClick files
    /// @param compatJar     the `codenameone-android-compat` jar
    /// @param namespace     overrides the manifest package, or null
    /// @param mainPackage   `codename1.packageName`, or null
    /// @param mainClass     `codename1.mainName`, or null
    /// @param sourceRoots   the project's own source roots, to see whether the main class exists
    public AndroidResourceRunner(File androidDir, File javaOut, File resourcesOut, File buildDir, File compatJar,
                                 String namespace, String mainPackage, String mainClass, List<File> sourceRoots,
                                 Log log) {
        this.androidDir = androidDir;
        this.javaOut = javaOut;
        this.resourcesOut = resourcesOut;
        this.buildDir = buildDir;
        this.compatJar = compatJar;
        this.namespace = namespace != null && namespace.length() > 0 ? namespace : gradleNamespace(androidDir);
        this.mainPackage = mainPackage;
        this.mainClass = mainClass;
        this.sourceRoots = sourceRoots;
        this.log = log;
    }

    /// The `namespace` of the Android Gradle module the sources belong to,
    /// when they are an Android Studio module's own `src/main` (a project
    /// building a module where it stands rather than an imported copy):
    /// current modules declare their package there, not in the manifest.
    static String gradleNamespace(File androidDir) {
        File dir = androidDir.getAbsoluteFile();
        File src = dir.getParentFile();
        if (!"main".equals(dir.getName()) || src == null || !"src".equals(src.getName())) {
            return null;
        }
        File module = src.getParentFile();
        if (module == null) {
            return null;
        }
        for (String name : new String[] {"build.gradle.kts", "build.gradle"}) {
            File script = new File(module, name);
            if (script.isFile()) {
                try {
                    AndroidProjectImporter.Result r = new AndroidProjectImporter.Result();
                    AndroidProjectImporter.readGradle(new String(java.nio.file.Files.readAllBytes(script.toPath()),
                            java.nio.charset.StandardCharsets.UTF_8), r);
                    return r.namespace;
                } catch (IOException e) {
                    return null;
                }
            }
        }
        return null;
    }

    public static boolean isAndroidProject(File androidDir) {
        return new File(androidDir, "AndroidManifest.xml").isFile() || new File(androidDir, "res").isDirectory();
    }

    public File onClickNamesFile() {
        return onClickNamesFile(buildDir);
    }

    /// Where a runner given `buildDir` writes the `android:onClick` names the
    /// remap step reads. Both build plugins locate the file through this, so the
    /// writer and the reader cannot disagree on its path.
    public static File onClickNamesFile(File buildDir) {
        return new File(buildDir, "android-res/onclick.txt");
    }

    /// Runs the compiler unless nothing changed since the last run. Returns
    /// false when the project has no Android sources.
    public boolean run() throws BuildException {
        if (!isAndroidProject(androidDir)) {
            return false;
        }
        if (compatJar == null || !compatJar.isFile()) {
            throw new BuildException("src/main/android exists but the " + COMPAT_ARTIFACT
                    + " dependency is missing; add it to the common module (scope provided).");
        }
        File state = new File(buildDir, "android-res/state.txt");
        String digest = digest();
        try {
            if (state.isFile() && digest.equals(new String(Files.readAllBytes(state.toPath()), "UTF-8"))
                    && new File(resourcesOut, ResourceCompiler.APP_TABLE).isFile() && javaOut.isDirectory()) {
                // The digest covers the Android sources, not the
                // application's own: a main class written since the last
                // build must still retire the generated one, and one deleted
                // since must bring it back. The latter falls through to a
                // full run, which is what writes the generated class.
                if (removeGeneratedMainClassIfWritten() || !entryPointMissing()) {
                    log.debug("Android resources unchanged; skipping");
                    return true;
                }
            }
        } catch (IOException e) {
            log.debug("Cannot read " + state + ": " + e);
        }
        ResourceCompiler.Request req = new ResourceCompiler.Request();
        File res = new File(androidDir, "res");
        if (res.isDirectory()) {
            req.res.add(new ResourceCompiler.ResSource(res, null));
        }
        req.manifest = new File(androidDir, "AndroidManifest.xml");
        req.namespace = namespace;
        req.javaOut = javaOut;
        req.resourcesOut = resourcesOut;
        req.assetsDir = new File(androidDir, "assets");
        req.onClickNamesOut = onClickNamesFile();
        ResourceCompiler.Result result;
        try {
            ZipFile zip = new ZipFile(compatJar);
            try {
                ZipEntry symbols = zip.getEntry(ResourceCompiler.FRAMEWORK_SYMBOLS_RESOURCE);
                if (symbols == null) {
                    throw new BuildException(compatJar + " has no " + ResourceCompiler.FRAMEWORK_SYMBOLS_RESOURCE);
                }
                InputStream in = zip.getInputStream(symbols);
                // The AndroidX and Material resources the runtime ships join
                // the application's namespace. Their symbols cover every
                // shipped library: like the Android Gradle plugin merging what
                // the build declares, but without per-dependency selection,
                // because the runtime's library classes were compiled against
                // these exact ids and an R field costs a few bytes.
                ZipEntry libSymbols = zip.getEntry(ResourceCompiler.LIBRARY_SYMBOLS_RESOURCE);
                InputStream lib = libSymbols == null ? null : zip.getInputStream(libSymbols);
                try {
                    req.frameworkSymbols = in;
                    req.librarySymbols = lib;
                    javaOut.mkdirs();
                    resourcesOut.mkdirs();
                    result = new ResourceCompiler().compile(req);
                } finally {
                    in.close();
                    if (lib != null) {
                        lib.close();
                    }
                }
            } finally {
                zip.close();
            }
        } catch (IOException e) {
            throw new BuildException("Failed to compile Android resources: " + e.getMessage(), e);
        }
        int errors = 0;
        for (Diagnostic d : result.diagnostics) {
            String msg = "src/main/android/" + d;
            if (d.severity == Diagnostic.Severity.ERROR) {
                log.error(msg);
                errors++;
            } else {
                log.warn(msg);
            }
        }
        if (errors > 0) {
            throw new BuildException(errors + " error(s) compiling Android resources; see above");
        }
        if (result.manifest != null) {
            writeMainClass(result.manifest.packageName);
        }
        log.info("Compiled " + result.resourceCount + " Android resources");
        try {
            state.getParentFile().mkdirs();
            Files.write(state.toPath(), digest.getBytes("UTF-8"));
        } catch (IOException e) {
            log.debug("Cannot write " + state + ": " + e);
        }
        return true;
    }

    private File generatedMainClass() {
        return new File(javaOut, mainPackage.replace('.', File.separatorChar) + File.separator + mainClass + ".java");
    }

    /// True when the application has its own main class, in which case the
    /// generated one is deleted: it lives in the persistent generated-sources
    /// tree, and left there next to the developer's class javac would see two
    /// definitions until a clean build.
    private boolean removeGeneratedMainClassIfWritten() {
        if (mainPackage == null || mainClass == null) {
            return false;
        }
        String rel = mainPackage.replace('.', File.separatorChar) + File.separator + mainClass;
        for (File root : sourceRoots) {
            if (root.equals(javaOut)) {
                continue;
            }
            if (new File(root, rel + ".java").isFile() || new File(root, rel + ".kt").isFile()) {
                File generated = generatedMainClass();
                if (generated.isFile() && !generated.delete()) {
                    log.warn("Cannot delete the generated " + generated + "; the application now has its own");
                }
                return true;
            }
        }
        return false;
    }

    /// True when a full run would generate the main class but it is absent:
    /// the application deleted its own after an earlier run retired the
    /// generated one. Only a manifest makes a run generate it.
    private boolean entryPointMissing() {
        return mainPackage != null && mainClass != null && new File(androidDir, "AndroidManifest.xml").isFile()
                && !generatedMainClass().isFile();
    }

    /// Generates the Codename One main class when the project does not have
    /// one, so an imported Android application builds without any Codename
    /// One code of its own.
    private void writeMainClass(String appPackage) throws BuildException {
        if (mainPackage == null || mainClass == null) {
            return;
        }
        if (removeGeneratedMainClassIfWritten()) {
            return;
        }
        File out = generatedMainClass();
        out.getParentFile().mkdirs();
        String src = "// Generated by the Codename One Android resource compiler: the application's\n"
                + "// Codename One entry point. Write this class yourself to customize startup.\n"
                + "package " + mainPackage + ";\n\n"
                + "public class " + mainClass + " extends com.codename1.androidcompat.runtime.AndroidLifecycle {\n"
                + "    public " + mainClass + "() {\n"
                + "        super(new " + ResourceCompiler.DEFAULT_GENERATED_PACKAGE + "."
                + ResourceCompiler.APP_IMPL_CLASS + "());\n"
                + "    }\n"
                + "}\n";
        try {
            Writer w = new OutputStreamWriter(new FileOutputStream(out), Charset.forName("UTF-8"));
            try {
                w.write(src);
            } finally {
                w.close();
            }
        } catch (IOException e) {
            throw new BuildException("Cannot write " + out + ": " + e.getMessage(), e);
        }
    }

    private String digest() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            List<String> entries = new ArrayList<String>();
            collect(androidDir, "", entries);
            Collections.sort(entries);
            for (String e : entries) {
                md.update(e.getBytes("UTF-8"));
            }
            md.update(("" + compatJar.length() + "/" + compatJar.lastModified() + "/" + namespace + "/" + mainPackage
                    + "/" + mainClass).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest()) {
                sb.append(Integer.toHexString((b & 0xff) | 0x100).substring(1));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        } catch (IOException e) {
            return "";
        }
    }

    private static void collect(File dir, String prefix, List<String> out) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            String rel = prefix + f.getName();
            if (f.isDirectory()) {
                if (prefix.length() == 0 && (f.getName().equals("java") || f.getName().equals("kotlin"))) {
                    continue;
                }
                collect(f, rel + "/", out);
            } else {
                out.add(rel + ":" + f.length() + ":" + f.lastModified());
            }
        }
    }
}
