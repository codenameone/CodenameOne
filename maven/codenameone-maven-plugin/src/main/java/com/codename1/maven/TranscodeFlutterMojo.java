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

import com.codename1.dart.transpiler.api.DartTranspiler;
import com.codename1.dart.transpiler.api.Diagnostic;
import com.codename1.dart.transpiler.api.TranspileRequest;
import com.codename1.dart.transpiler.api.TranspileResult;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Transpiles Flutter/Dart sources under {@code src/main/flutter} into Java
 * source targeting the Codename One Flutter runtime
 * ({@code codenameone-flutter-runtime}), so Flutter UI code runs as plain
 * Codename One components at native speed -- no Dart VM or Flutter engine.
 *
 * <h3>Source layout</h3>
 * <ul>
 *   <li>{@code src/main/flutter/**&#47;*.dart} -- Dart sources (whole-program
 *       transpile; subdirectories allowed)</li>
 *   <li>{@code src/main/flutter/assets/**} and {@code src/main/flutter/packages/**} --
 *       bundled assets, flattened into the
 *       build output (Codename One resources are flat on every port) so
 *       {@code Image.asset(...)} resolves</li>
 * </ul>
 *
 * <p>When the directory does not exist the goal is a silent no-op. When it
 * exists, generated sources land in {@code target/generated-sources/flutter}
 * (registered as a compile source root). Generated code is Java 17 source,
 * so the build must run on JDK 17+ -- checked here with a friendly error.
 * The transpile is whole-program with a digest-based fast skip; unchanged
 * outputs are not rewritten, keeping incremental javac warm.</p>
 */
@Mojo(name = "transcode-flutter", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class TranscodeFlutterMojo extends AbstractCN1Mojo {

    private static final String RUNTIME_ARTIFACT = "codenameone-flutter-runtime";

    @Parameter(property = "cn1.flutter.sourceDir", defaultValue = "${project.basedir}/src/main/flutter")
    private File flutterSourceDir;

    @Parameter(property = "cn1.flutter.outputDir", defaultValue = "${project.build.directory}/generated-sources/flutter")
    private File flutterOutputDir;

    @Parameter(property = "cn1.flutter.package", defaultValue = "com.codename1.generated.flutter")
    private String flutterPackage;

    @Parameter(property = "cn1.flutter.stateFile", defaultValue = "${project.build.directory}/flutter-transpiler/state.txt")
    private File stateFile;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (flutterSourceDir == null || !flutterSourceDir.isDirectory()) {
            sweepStaleOutput();
            // Flutter was removed from the project: its assets go with it.
            removeStaleAssets(new HashSet<String>());
            return;
        }
        checkJdk();
        checkRuntimeDependency();

        TranspileRequest req = new TranspileRequest()
                .sourceRoot(flutterSourceDir)
                .outputDir(flutterOutputDir)
                .packageName(flutterPackage)
                .stateFile(stateFile);
        // runtime dependencies contribute API stubs via META-INF/dart/*.dart
        for (Object o : project.getArtifacts()) {
            org.apache.maven.artifact.Artifact a = (org.apache.maven.artifact.Artifact) o;
            if (a.getFile() != null) {
                req.stubClasspathEntry(a.getFile());
            }
        }
        TranspileResult result = new DartTranspiler().transpile(req);
        for (Diagnostic d : result.getDiagnostics()) {
            String msg = "src/main/flutter/" + d.file + ":[" + d.line + "," + d.col + "] "
                    + d.message + " (dart2java:" + d.code + ")";
            if (d.severity == Diagnostic.Severity.ERROR) {
                getLog().error(msg);
            } else if (d.severity == Diagnostic.Severity.WARNING) {
                getLog().warn(msg);
            } else {
                getLog().info(msg);
            }
        }
        if (result.hasErrors()) {
            throw new MojoFailureException("Flutter transpilation failed with "
                    + result.errors().size() + " error(s); see log above. "
                    + "Confirm the Dart files pass `dart analyze` -- constructs outside the "
                    + "currently supported subset are reported with a milestone code.");
        }
        if (result.isUpToDate()) {
            getLog().info("Flutter sources are up to date");
        } else {
            getLog().info("Transpiled Flutter sources to " + flutterOutputDir);
        }
        copyAssets();
        registerSourceRoot();
    }

    private void checkJdk() throws MojoFailureException {
        String spec = System.getProperty("java.specification.version", "1.8");
        int major;
        try {
            major = Integer.parseInt(spec.startsWith("1.") ? spec.substring(2) : spec);
        } catch (NumberFormatException e) {
            major = 8;
        }
        if (major < 17) {
            throw new MojoFailureException("Flutter support generates Java 17 source, but this build "
                    + "is running on JDK " + spec + ". Run Maven with JDK 17 or newer "
                    + "(e.g. from https://adoptium.net) to use src/main/flutter.");
        }
    }

    private void checkRuntimeDependency() throws MojoFailureException {
        for (Object o : project.getDependencies()) {
            Dependency d = (Dependency) o;
            if (RUNTIME_ARTIFACT.equals(d.getArtifactId())) {
                return;
            }
        }
        throw new MojoFailureException("src/main/flutter contains Dart sources but the project "
                + "does not declare the Flutter runtime dependency. Add this to common/pom.xml:\n\n"
                + "    <dependency>\n"
                + "        <groupId>com.codenameone</groupId>\n"
                + "        <artifactId>" + RUNTIME_ARTIFACT + "</artifactId>\n"
                + "        <version>${cn1.version}</version>\n"
                + "    </dependency>\n");
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
    void copyAssets() throws MojoExecutionException {
        File outDir = new File(project.getBuild().getOutputDirectory());
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
                File dir = new File(flutterSourceDir, root);
                if (!dir.isDirectory()) {
                    continue;
                }
                Files.createDirectories(outDir.toPath());
                // the root stays in the Flutter asset key, matching pubspec paths
                count += flattenInto(dir, root, outDir, written, keys);
            }
            if (count > 0) {
                writeAssetManifest(outDir, keys, written);
                getLog().info("Flattened " + count + " Flutter asset(s) into the build output");
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed copying Flutter assets", e);
        }
        removeStaleAssets(written);
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
    void removeStaleAssets(Set<String> written) throws MojoExecutionException {
        File outDir = new File(project.getBuild().getOutputDirectory());
        File manifest = assetManifest();
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
            throw new MojoExecutionException("Failed removing stale Flutter assets", e);
        }
    }

    /**
     * The record of flattened asset names. Beside the output rather than inside
     * the generated-sources directory, which {@link #sweepStaleOutput} deletes.
     */
    private File assetManifest() {
        return new File(project.getBuild().getDirectory(), "cn1-flutter-assets.lst");
    }

    /** The directory names under {@code src/main/flutter} that hold bundled assets. */
    private static final String[] ASSET_ROOTS = {"assets", "packages"};

    /**
     * The bundled list of asset keys, one per line, which the runtime's
     * {@code FlutterAssets} reads to learn which resolution variants exist. Flutter
     * builds the same thing (AssetManifest) for the same reason: a variant can sit
     * in any positive scale directory -- {@code 2.5x/}, or {@code 2x/} as well as
     * {@code 2.0x/} -- so no fixed probe list finds them all, and one outside it was
     * bundled and never used. Its name cannot collide with an asset: every asset key
     * starts with {@code assets/} or {@code packages/}, so every asset's flat name
     * carries a {@code _s} after that root.
     */
    static final String ASSET_MANIFEST = "cn1f_AssetManifest.txt";

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

    private void sweepStaleOutput() {
        if (flutterOutputDir != null && flutterOutputDir.isDirectory()) {
            deleteRecursive(flutterOutputDir);
            getLog().debug("Removed stale Flutter generated sources");
        }
    }

    private void deleteRecursive(File dir) {
        File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    deleteRecursive(child);
                } else {
                    child.delete();
                }
            }
        }
        dir.delete();
    }

    private void registerSourceRoot() {
        String path = flutterOutputDir.getAbsolutePath();
        if (!project.getCompileSourceRoots().contains(path)) {
            project.addCompileSourceRoot(path);
            getLog().debug("Added compile source root " + path);
        }
    }
}
