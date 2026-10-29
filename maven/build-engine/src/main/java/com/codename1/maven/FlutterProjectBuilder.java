/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
import com.codename1.dart.transpiler.api.DartTranspiler;
import com.codename1.dart.transpiler.api.Diagnostic;
import com.codename1.dart.transpiler.api.TranspileRequest;
import com.codename1.dart.transpiler.api.TranspileResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Builds `src/main/flutter` -- Flutter/Dart sources and their assets -- into
/// Java source on the Codename One Flutter runtime
/// (`codenameone-flutter-runtime`) and flat resources. The Maven plugin's
/// `transcode-flutter` goal and the Gradle plugin's `transcodeFlutter` task
/// both run it, so the two builds cannot drift.
///
/// - `src/main/flutter/**.dart` are transpiled as one program, with a
///   digest-based fast skip; unchanged outputs are not rewritten, keeping
///   incremental javac warm. The generated code is Java 17 source.
/// - `src/main/flutter/assets/**` and `src/main/flutter/packages/**`, and any
///   other pubspec `assets:`/`fonts:` key `cn1:import-flutter-project`
///   recorded (`FlutterProjectImporter.ASSET_KEYS_RECORD`), are flattened into
///   the assets directory, because Codename One resources are flat on every
///   port.
///
/// Without the source directory, [#build()] removes whatever an earlier build
/// of one generated.
public final class FlutterProjectBuilder {
    /// The runtime the generated code runs on.
    public static final String RUNTIME_ARTIFACT = "codenameone-flutter-runtime";
    /// The package generated code goes into unless `cn1.flutter.package` says otherwise.
    public static final String DEFAULT_PACKAGE = "com.codename1.generated.flutter";

    private final File sourceDir;
    private final File outputDir;
    private final File assetsDir;
    private final File assetManifest;
    private final File stateFile;
    private final String packageName;
    private final Log log;
    private final List<File> stubClasspath = new ArrayList<File>();
    private final List<File> javaSourceRoots = new ArrayList<File>();
    private File classesDir;

    /// @param sourceDir `src/main/flutter`
    /// @param outputDir where the generated Java goes; owned by this builder
    /// @param assetsDir where flattened assets go -- a resources root of the build
    /// @param assetManifest the record of the asset names written, so the next
    ///        build can remove the ones it no longer writes; outside `outputDir`
    /// @param stateFile the transpiler's incremental state
    public FlutterProjectBuilder(File sourceDir, File outputDir, File assetsDir, File assetManifest,
            File stateFile, String packageName, Log log) {
        this.sourceDir = sourceDir;
        this.outputDir = outputDir;
        this.assetsDir = assetsDir;
        this.assetManifest = assetManifest;
        this.stateFile = stateFile;
        this.packageName = packageName == null || packageName.isEmpty() ? DEFAULT_PACKAGE : packageName;
        this.log = log;
    }

    /// True when `dir` is a Flutter source tree to build.
    public static boolean isFlutterProject(File dir) {
        return dir != null && dir.isDirectory();
    }

    /// A jar or directory whose `META-INF/dart/*.dart` stubs give the Dart
    /// API the program compiles against: the compile class path.
    public FlutterProjectBuilder stubClasspath(Iterable<File> entries) {
        for (File f : entries) {
            if (f != null) {
                stubClasspath.add(f);
            }
        }
        return this;
    }

    /// The application's own Java roots. Its Java can name a plugin pack too
    /// (the import-generated entry point installs the firebase_messaging and
    /// app_links hooks): the transpiler copies exactly the packs that are
    /// named, so it must see that Java as well.
    public FlutterProjectBuilder javaSourceRoots(Iterable<File> roots) {
        for (File dir : roots) {
            if (dir != null && dir.isDirectory() && !dir.getAbsoluteFile().equals(outputDir.getAbsoluteFile())) {
                javaSourceRoots.add(dir);
            }
        }
        return this;
    }

    /// The compiled classes, when the build compiles into a directory it
    /// does not empty (Maven's `target/classes`). The transpiler deletes the
    /// source of a plugin pack the app stopped naming, but javac does not
    /// reliably delete the class compiled from it on an earlier build. Left
    /// there, those classes would still ship -- and still imply the
    /// permissions (location, push, ...) the builders derive from the classes
    /// they find.
    public FlutterProjectBuilder classesDir(File dir) {
        this.classesDir = dir;
        return this;
    }

    /// Transpiles and copies the assets, or cleans up when there is nothing to build.
    public void build() throws BuildException {
        if (!isFlutterProject(sourceDir)) {
            deleteRecursive(outputDir);
            removeDeselectedPackClasses();
            // Flutter was removed from the project: its assets go with it.
            removeStaleAssets(new HashSet<String>());
            return;
        }
        checkJdk();
        TranspileRequest req = new TranspileRequest()
                .sourceRoot(sourceDir)
                .outputDir(outputDir)
                .packageName(packageName)
                .stateFile(stateFile);
        for (File f : stubClasspath) {
            req.stubClasspathEntry(f);
        }
        for (File dir : javaSourceRoots) {
            req.javaSourceRoot(dir);
        }
        TranspileResult result = new DartTranspiler().transpile(req);
        for (Diagnostic d : result.getDiagnostics()) {
            String msg = "src/main/flutter/" + d.file + ":[" + d.line + "," + d.col + "] "
                    + d.message + " (dart2java:" + d.code + ")";
            if (d.severity == Diagnostic.Severity.ERROR) {
                log.error(msg);
            } else if (d.severity == Diagnostic.Severity.WARNING) {
                log.warn(msg);
            } else {
                log.info(msg);
            }
        }
        if (result.hasErrors()) {
            throw new BuildException("Flutter transpilation failed with "
                    + result.errors().size() + " error(s); see log above. "
                    + "Confirm the Dart files pass `dart analyze` -- constructs outside the "
                    + "currently supported subset are reported with a milestone code.");
        }
        if (result.isUpToDate()) {
            log.info("Flutter sources are up to date");
        } else {
            log.info("Transpiled Flutter sources to " + outputDir);
        }
        removeDeselectedPackClasses();
        copyAssets();
    }

    private void removeDeselectedPackClasses() {
        if (classesDir == null) {
            return;
        }
        String packs = "com/codename1/flutter/plugins".replace('/', File.separatorChar);
        File[] compiled = new File(classesDir, packs).listFiles();
        if (compiled == null) {
            return;
        }
        File selected = new File(outputDir, packs);
        for (File pack : compiled) {
            if (pack.isDirectory() && !new File(selected, pack.getName()).isDirectory()) {
                deleteRecursive(pack);
                log.debug("Removed classes of unused Flutter plugin pack " + pack.getName());
            }
        }
    }

    private static void checkJdk() throws BuildException {
        String spec = System.getProperty("java.specification.version", "1.8");
        int major;
        try {
            major = Integer.parseInt(spec.startsWith("1.") ? spec.substring(2) : spec);
        } catch (NumberFormatException e) {
            major = 8;
        }
        if (major < 17) {
            throw new BuildException("Flutter support generates Java 17 source, but this build "
                    + "is running on JDK " + spec + ". Run the build with JDK 17 or newer "
                    + "(e.g. from https://adoptium.net) to use src/main/flutter.");
        }
    }

    /**
     * Copies {@code src/main/flutter/assets} into the build output, <em>flattened</em>.
     *
     * <p>Codename One resources are flat on every port -- {@code getResourceAsStream}
     * rejects a name containing a {@code '/'} past the leading one -- so the asset
     * tree cannot be mirrored. Each asset is written to the output root under the
     * name produced by {@link #flatAssetName}, which the Flutter runtime's
     * {@code FlutterAssets} recomputes when resolving {@code Image.asset(...)}.</p>
     */
    private void copyAssets() throws BuildException {
        File outDir = assetsDir;
        Set<String> written = new HashSet<String>();
        List<String> keys = new ArrayList<String>();
        int count = 0;
        try {
            // Both roots a pubspec asset key can start with. "assets/" is an app's own
            // bundle; "packages/" is one it pulls from a dependency, which is how the
            // gallery ships its artwork (packages/flutter_gallery_assets/assets/...).
            // Only "assets/" was copied before, so every packaged asset key resolved to
            // nothing on every port - the images were simply absent.
            for (String root : ASSET_ROOTS) {
                File dir = new File(sourceDir, root);
                if (!dir.isDirectory()) {
                    continue;
                }
                Files.createDirectories(outDir.toPath());
                // the root stays in the Flutter asset key, matching pubspec paths
                count += flattenInto(dir, root, outDir, written, keys);
            }
            // Flutter allows an asset or font declared anywhere project-relative --
            // fonts/MyFont.ttf, images/logo.png -- not only under assets/ or
            // packages/, so the scan above missed every one of those: the key
            // resolved to nothing on every port. cn1:import-flutter-project records
            // exactly which keys it copied, in FlutterProjectImporter.ASSET_KEYS_RECORD;
            // a hand-wired project (no import, so no such record) is unaffected --
            // it keeps relying on the scan above alone, as before.
            for (String key : readDeclaredAssetKeys()) {
                if (ASSET_ROOTS_SET.contains(topSegment(key))) {
                    continue; // already copied by the scan above
                }
                Files.createDirectories(outDir.toPath());
                count += flattenDeclaredAsset(key, outDir, written, keys);
            }
            if (count > 0) {
                writeAssetManifest(outDir, keys, written);
                log.info("Flattened " + count + " Flutter asset(s) into the build output");
            }
        } catch (IOException e) {
            throw new BuildException("Failed copying Flutter assets", e);
        }
        removeStaleAssets(written);
    }

    /// The `FlutterProjectImporter.ASSET_KEYS_RECORD` keys a previous
    /// `cn1:import-flutter-project` wrote beside `src/main/flutter`, or
    /// empty when the project was hand-wired (never imported) or predates
    /// the record. The record's file name is the importer's own constant.
    private List<String> readDeclaredAssetKeys() throws IOException {
        File f = new File(sourceDir, FlutterProjectImporter.ASSET_KEYS_RECORD);
        if (!f.isFile()) {
            return java.util.Collections.emptyList();
        }
        List<String> out = new ArrayList<String>();
        for (String line : Files.readAllLines(f.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
            line = line.trim();
            if (line.length() > 0) {
                out.add(line);
            }
        }
        return out;
    }

    private static String topSegment(String key) {
        int slash = key.indexOf('/');
        return slash < 0 ? key : key.substring(0, slash);
    }

    /// Flattens one declared asset key outside {@link #ASSET_ROOTS}: a file,
    /// or a directory (pubspec allows declaring a whole directory), rooted
    /// at {@code sourceDir}. Missing on disk is not an error here --
    /// {@code FlutterProjectImporter} already warned about that key when it
    /// could not resolve it.
    private int flattenDeclaredAsset(String key, File outDir, Set<String> written, List<String> keys)
            throws IOException {
        File node = new File(sourceDir, key.replace('/', File.separatorChar));
        if (node.isDirectory()) {
            return flattenInto(node, key, outDir, written, keys);
        }
        if (!node.isFile()) {
            return 0;
        }
        String flat = flatAssetName(key);
        Files.copy(node.toPath(), new File(outDir, flat).toPath(), StandardCopyOption.REPLACE_EXISTING);
        written.add(flat);
        keys.add(key);
        return 1;
    }

    /**
     * Deletes the flattened assets an earlier build wrote that this one did not,
     * then records what this build wrote.
     *
     * <p>Flattening only ever ADDS to {@code target/classes}. An asset renamed or
     * deleted in {@code src/main/flutter} -- or the whole Flutter tree removed --
     * therefore kept being packaged by every incremental build until a clean one:
     * a bigger application, and a stale {@code Image.asset} lookup that still
     * succeeded against a file the project no longer has.</p>
     *
     * <p>Only names from this plugin's own record are ever deleted. Sweeping every
     * {@code cn1f_*} file would be simpler, but the output directory is shared with
     * the application's own resources, and a name this plugin did not write is not
     * one it should remove.</p>
     */
    private void removeStaleAssets(Set<String> written) throws BuildException {
        File outDir = assetsDir;
        File manifest = assetManifest;
        try {
            if (manifest.isFile()) {
                for (String name : Files.readAllLines(manifest.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
                    name = name.trim();
                    // Defensive: a record entry must name a flat asset in the output
                    // root, never a path, whatever ended up in the file.
                    if (name.startsWith("cn1f_") && name.indexOf('/') < 0 && name.indexOf('\\') < 0
                            && !written.contains(name)) {
                        Files.deleteIfExists(new File(outDir, name).toPath());
                    }
                }
            }
            if (written.isEmpty()) {
                Files.deleteIfExists(manifest.toPath());
            } else {
                List<String> sorted = new ArrayList<String>(written);
                java.util.Collections.sort(sorted);
                Files.createDirectories(manifest.getParentFile().toPath());
                Files.write(manifest.toPath(), sorted, java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new BuildException("Failed removing stale Flutter assets", e);
        }
    }

    /** The directory names under {@code src/main/flutter} that hold bundled assets. */
    private static final String[] ASSET_ROOTS = {"assets", "packages"};
    private static final Set<String> ASSET_ROOTS_SET =
            new HashSet<String>(java.util.Arrays.asList(ASSET_ROOTS));

    /**
     * The bundled list of asset keys, one per line, which the runtime's
     * {@code FlutterAssets} reads to learn which resolution variants exist. Flutter
     * builds the same thing (AssetManifest) for the same reason: a variant can sit
     * in any positive scale directory -- {@code 2.5x/}, or {@code 2x/} as well as
     * {@code 2.0x/} -- so no fixed probe list finds them all, and one outside it was
     * bundled and never used.
     *
     * <p>The name is outside the namespace {@link #flatAssetName} can produce: the
     * encoder writes every {@code '_'} of a key as {@code "__"} and every {@code '/'}
     * as {@code "_s"}, so after the {@code cn1f_} prefix a single underscore is always
     * followed by another underscore or an {@code s}. {@code "cn1f__A..."} -- an
     * underscore followed by {@code A} -- is therefore no asset's flat name. The
     * plain {@code cn1f_AssetManifest.txt} used to be, once {@link #readDeclaredAssetKeys}
     * began flattening project-relative keys outside {@code assets/} and
     * {@code packages/}: a declared {@code AssetManifest.txt} flattened to it and
     * {@link #writeAssetManifest} overwrote the developer's asset. The runtime reads
     * the same literal ({@code FlutterAssets.MANIFEST}); the two must change
     * together.</p>
     */
    static final String ASSET_MANIFEST = "cn1f__AssetManifest.txt";


    private void writeAssetManifest(File outDir, List<String> keys, Set<String> written) throws IOException {
        List<String> sorted = new ArrayList<String>(keys);
        java.util.Collections.sort(sorted);
        Files.write(new File(outDir, ASSET_MANIFEST).toPath(), sorted, java.nio.charset.StandardCharsets.UTF_8);
        written.add(ASSET_MANIFEST);
    }

    private int flattenInto(File dir, String assetPrefix, File outDir, Set<String> written,
                            List<String> keys) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return 0;
        }
        int count = 0;
        for (File child : children) {
            String key = assetPrefix + "/" + child.getName();
            if (child.isDirectory()) {
                count += flattenInto(child, key, outDir, written, keys);
            } else {
                String flat = flatAssetName(key);
                Files.copy(child.toPath(), new File(outDir, flat).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                written.add(flat);
                keys.add(key);
                count++;
            }
        }
        return count;
    }

    /**
     * The flat resource name for a Flutter asset key. {@code '_'} is an escape
     * always followed by one character -- {@code "__"} for an underscore,
     * {@code "_s"} for the separator -- which is prefix-free, so no two keys
     * share a name. Doubling underscores and using a single one as the
     * separator was not: {@code a_/b} and {@code a/_b} both became
     * {@code a___b}, and one asset was silently written over the other.
     * Extensions survive for native bundlers.
     *
     * <p><b>Keep in sync</b> with {@code com.codename1.flutter.FlutterAssets} in
     * the Flutter runtime -- deliberately duplicated rather than shared, because
     * this plugin must not depend on the runtime it builds against.</p>
     */
    static String flatAssetName(String assetKey) {
        String p = assetKey;
        while (p.startsWith("/")) {
            p = p.substring(1);
        }
        StringBuilder sb = new StringBuilder("cn1f_");
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            if (c == '_') {
                sb.append("__");
            } else if (c == '/') {
                sb.append("_s");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static void deleteRecursive(File dir) {
        File[] children = dir == null ? null : dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleteRecursive(child);
                } else {
                    child.delete();
                }
            }
        }
        if (dir != null) {
            dir.delete();
        }
    }
}
