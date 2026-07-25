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

/**
 * Transpiles Flutter/Dart sources under {@code src/main/flutter} into Java
 * source targeting the Codename One Flutter runtime
 * ({@code codenameone-flutter-runtime}), so Flutter UI code runs as plain
 * Codename One components at native speed — no Dart VM or Flutter engine.
 *
 * <h3>Source layout</h3>
 * <ul>
 *   <li>{@code src/main/flutter/**&#47;*.dart} — Dart sources (whole-program
 *       transpile; subdirectories allowed)</li>
 *   <li>{@code src/main/flutter/assets/**} — bundled assets, flattened into the
 *       build output (Codename One resources are flat on every port) so
 *       {@code Image.asset(...)} resolves</li>
 * </ul>
 *
 * <p>When the directory does not exist the goal is a silent no-op. When it
 * exists, generated sources land in {@code target/generated-sources/flutter}
 * (registered as a compile source root). Generated code is Java 17 source,
 * so the build must run on JDK 17+ — checked here with a friendly error.
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
                    + "Confirm the Dart files pass `dart analyze` — constructs outside the "
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
     * <p>Codename One resources are flat on every port — {@code getResourceAsStream}
     * rejects a name containing a {@code '/'} past the leading one — so the asset
     * tree cannot be mirrored. Each asset is written to the output root under the
     * name produced by {@link #flatAssetName}, which the Flutter runtime's
     * {@code FlutterAssets} recomputes when resolving {@code Image.asset(...)}.</p>
     */
    private void copyAssets() throws MojoExecutionException {
        File assets = new File(flutterSourceDir, "assets");
        if (!assets.isDirectory()) {
            return;
        }
        File outDir = new File(project.getBuild().getOutputDirectory());
        try {
            Files.createDirectories(outDir.toPath());
            // "assets/" stays in the Flutter asset key, matching pubspec paths
            int count = flattenInto(assets, "assets", outDir);
            getLog().info("Flattened " + count + " Flutter asset(s) into the build output");
        } catch (IOException e) {
            throw new MojoExecutionException("Failed copying Flutter assets", e);
        }
    }

    private int flattenInto(File dir, String assetPrefix, File outDir) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return 0;
        }
        int count = 0;
        for (File child : children) {
            String key = assetPrefix + "/" + child.getName();
            if (child.isDirectory()) {
                count += flattenInto(child, key, outDir);
            } else {
                Files.copy(child.toPath(), new File(outDir, flatAssetName(key)).toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
                count++;
            }
        }
        return count;
    }

    /**
     * The flat resource name for a Flutter asset key. Doubles every {@code '_'}
     * then uses {@code '_'} as the path separator, so the encoding is
     * unambiguous while introducing no characters that were not already legal
     * in the source path (extensions survive for native bundlers).
     *
     * <p><b>Keep in sync</b> with {@code com.codename1.flutter.FlutterAssets} in
     * the Flutter runtime — deliberately duplicated rather than shared, because
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
                sb.append('_');
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
