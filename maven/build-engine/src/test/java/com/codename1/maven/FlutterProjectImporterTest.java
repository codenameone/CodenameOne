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

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FlutterProjectImporterTest {

    private static final String PUBSPEC = "name: demo_app\n"
            + "description: A demo.\n"
            + "version: 1.2.3+4\n"
            + "\n"
            + "environment:\n"
            + "  sdk: ^3.0.0\n"
            + "\n"
            + "dependencies:\n"
            + "  flutter:\n"
            + "    sdk: flutter\n"
            + "  cupertino_icons: ^1.0.2\n"
            + "  some_uncovered_pkg: ^1.0.0\n"
            + "\n"
            + "dev_dependencies:\n"
            + "  flutter_test:\n"
            + "    sdk: flutter\n"
            + "\n"
            + "flutter:\n"
            + "  uses-material-design: true\n"
            + "  assets:\n"
            + "    - assets/images/logo.png\n"
            + "    - assets/icons/\n"
            + "  fonts:\n"
            + "    - family: Custom\n"
            + "      fonts:\n"
            + "        - asset: assets/fonts/Custom-Regular.ttf\n";

    @Test
    public void parsesNameVersionDependenciesAssetsAndFonts() {
        FlutterProjectImporter.Pubspec p = FlutterProjectImporter.Pubspec.parse(PUBSPEC);
        assertEquals("demo_app", p.name);
        assertEquals("1.2.3+4", p.version);
        assertEquals(java.util.Arrays.asList("flutter", "cupertino_icons", "some_uncovered_pkg"), p.dependencies);
        assertFalse("dev_dependencies must not be reported as app dependencies",
                p.dependencies.contains("flutter_test"));
        assertEquals(java.util.Arrays.asList("assets/images/logo.png", "assets/icons/"), p.assets);
        assertEquals(java.util.Arrays.asList("assets/fonts/Custom-Regular.ttf"), p.fontAssets);
    }

    @Test
    public void setPropertyReplacesOnlyAnExistingKey() {
        String props = "codename1.version=1.0\ncodename1.displayName=Hi World\n";
        String updated = FlutterProjectImporter.setProperty(props, "codename1.version", "2.0");
        assertTrue(updated, updated.contains("codename1.version=2.0"));
        assertTrue(updated, updated.contains("codename1.displayName=Hi World"));
        // A key the file never declared is left alone, not appended.
        assertEquals(props, FlutterProjectImporter.setProperty(props, "codename1.secondaryTitle", "x"));
    }

    @Test
    public void importsLibTreeAssetsAndFontsAndReportsDependencies() throws Exception {
        File root = Files.createTempDirectory("flutter-import").toFile();
        File flutterProject = new File(root, "demo");
        write(new File(flutterProject, "pubspec.yaml"), PUBSPEC);
        write(new File(flutterProject, "lib/main.dart"), "void main() {}\n");
        write(new File(flutterProject, "lib/pages/home.dart"), "class Home {}\n");
        write(new File(flutterProject, "assets/images/logo.png"), "PNGDATA");
        write(new File(flutterProject, "assets/icons/a.png"), "ICONA");
        write(new File(flutterProject, "assets/icons/b.png"), "ICONB");
        write(new File(flutterProject, "assets/fonts/Custom-Regular.ttf"), "FONTDATA");

        File common = new File(root, "common");
        FlutterProjectImporter importer = new FlutterProjectImporter(new com.codename1.build.SystemStreamLog());
        FlutterProjectImporter.Result r = importer.importProject(flutterProject, common, "com.example", "MyApp");

        assertEquals("demo_app", r.appName);
        assertEquals("1.2.3+4", r.appVersion);
        assertTrue(r.covered.toString(), r.covered.get(0).startsWith("flutter ("));
        assertTrue(r.covered.toString(), r.covered.get(1).startsWith("cupertino_icons ("));
        assertEquals(java.util.Collections.singletonList("some_uncovered_pkg"), r.uncovered);
        assertTrue(r.missingAssets.toString(), r.missingAssets.isEmpty());

        File flutterDir = new File(common, "src/main/flutter");
        assertTrue(new File(flutterDir, "main.dart").isFile());
        assertTrue(new File(flutterDir, "pages/home.dart").isFile());
        assertTrue(new File(flutterDir, "assets/images/logo.png").isFile());
        assertTrue(new File(flutterDir, "assets/icons/a.png").isFile());
        assertTrue(new File(flutterDir, "assets/icons/b.png").isFile());
        assertTrue(new File(flutterDir, "assets/fonts/Custom-Regular.ttf").isFile());

        File entry = new File(common, "src/main/java/com/example/MyApp.java");
        assertTrue(entry.isFile());
        assertTrue(read(entry).contains("FlutterRegistry.invokeMain()"));
    }

    @Test
    public void missingAssetIsReportedNotFatal() throws Exception {
        File root = Files.createTempDirectory("flutter-import-missing").toFile();
        File flutterProject = new File(root, "demo");
        write(new File(flutterProject, "pubspec.yaml"), "name: demo\nversion: 1.0.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\n"
                + "flutter:\n  assets:\n    - packages/some_pkg/logo.png\n");
        write(new File(flutterProject, "lib/main.dart"), "void main() {}\n");
        File common = new File(root, "common");
        FlutterProjectImporter.Result r = new FlutterProjectImporter(new com.codename1.build.SystemStreamLog())
                .importProject(flutterProject, common, null, null);
        assertEquals(java.util.Collections.singletonList("packages/some_pkg/logo.png"), r.missingAssets);
        // Neither .dart_tool/package_config.json nor pubspec.lock exist here, so the
        // package itself -- not just the one asset key -- could not be resolved at all.
        assertEquals(java.util.Collections.singletonList("some_pkg"), r.unresolvedPackages);
    }

    /// `packages/<name>/...` asset keys are the common case in a real app
    /// (flutter_gallery_assets, cupertino_icons' own font, ...): the bytes
    /// live in the dependency's own checkout, found via
    /// `.dart_tool/package_config.json` exactly as `flutter pub get` left it,
    /// never a manual step.
    @Test
    public void resolvesPackageAssetsViaPackageConfigJson() throws Exception {
        File root = Files.createTempDirectory("flutter-pkg-assets").toFile();
        File flutterProject = new File(root, "demo");
        write(new File(flutterProject, "pubspec.yaml"), "name: demo\nversion: 1.0.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\n  my_pkg: 1.0.0\n"
                + "flutter:\n  assets:\n    - packages/my_pkg/logo.png\n    - packages/my_pkg/images/\n");
        write(new File(flutterProject, "lib/main.dart"), "void main() {}\n");

        // Stands in for the pub cache: a package checkout with no .dart source,
        // pure artwork, the same shape as flutter_gallery_assets.
        File pkgRoot = new File(root, "fake-pub-cache/my_pkg-1.0.0");
        write(new File(pkgRoot, "lib/logo.png"), "LOGO");
        write(new File(pkgRoot, "lib/images/a.png"), "A");
        write(new File(pkgRoot, "lib/images/b.png"), "B");

        File dartTool = new File(flutterProject, ".dart_tool");
        write(new File(dartTool, "package_config.json"), "{\n  \"configVersion\": 2,\n  \"packages\": [\n"
                + "    {\n      \"name\": \"my_pkg\",\n      \"rootUri\": \"" + pkgRoot.toURI()
                + "\",\n      \"packageUri\": \"lib/\"\n    }\n  ]\n}\n");

        File common = new File(root, "common");
        FlutterProjectImporter.Result r = new FlutterProjectImporter(new com.codename1.build.SystemStreamLog())
                .importProject(flutterProject, common, null, null);

        assertTrue(r.missingAssets.toString(), r.missingAssets.isEmpty());
        assertTrue(r.unresolvedPackages.toString(), r.unresolvedPackages.isEmpty());
        File flutterDir = new File(common, "src/main/flutter");
        assertTrue(new File(flutterDir, "packages/my_pkg/logo.png").isFile());
        assertTrue(new File(flutterDir, "packages/my_pkg/images/a.png").isFile());
        assertTrue(new File(flutterDir, "packages/my_pkg/images/b.png").isFile());
        // A package with no Dart source at all (pure artwork) is reported as
        // covered once its declared assets are actually copied.
        assertEquals(2, r.covered.size());
        assertTrue(r.covered.toString(), r.covered.get(1).startsWith("my_pkg (asset"));
        assertTrue(r.uncovered.toString(), r.uncovered.isEmpty());
    }

    @Test
    public void reimportKeepsACustomizedEntryPointAndRemovesStaleAssets() throws Exception {
        File root = Files.createTempDirectory("flutter-reimport").toFile();
        File flutterProject = new File(root, "demo");
        write(new File(flutterProject, "pubspec.yaml"), "name: demo\nversion: 1.0.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\n");
        write(new File(flutterProject, "lib/main.dart"), "void main() {}\n");
        File oldAsset = new File(flutterProject, "assets/old.png");
        // The asset is declared now and dropped before the second import.
        write(new File(flutterProject, "pubspec.yaml"), "name: demo\nversion: 1.0.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\n"
                + "flutter:\n  assets:\n    - assets/old.png\n");
        write(oldAsset, "OLD");
        File common = new File(root, "common");
        // A project generated from the archetype already has its main class; the
        // first import saves it as the backup, as the Android importer does.
        File entry = new File(common, "src/main/java/com/example/MyApp.java");
        write(entry, "package com.example; public class MyApp {}");
        FlutterProjectImporter importer = new FlutterProjectImporter(new com.codename1.build.SystemStreamLog());
        importer.importProject(flutterProject, common, "com.example", "MyApp");

        String generated = read(entry);
        String customized = generated.replace("invokeMain();", "invokeMain(); // customized");
        write(entry, customized);

        // The asset is dropped from pubspec and the import re-run.
        write(new File(flutterProject, "pubspec.yaml"), "name: demo\nversion: 1.0.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\n");
        importer.importProject(flutterProject, common, "com.example", "MyApp");

        assertEquals("the customized entry point was overwritten", customized, read(entry));
        File backup = new File(entry.getPath() + ".pre-flutter-import");
        assertEquals("package com.example; public class MyApp {}", read(backup));
        assertFalse("a stale asset dropped from pubspec.yaml stayed behind",
                new File(common, "src/main/flutter/assets/old.png").exists());
    }

    @Test
    public void aTrailingCommentIsNotPartOfAPubspecValue() {
        assertEquals("2.10.2+021002", FlutterProjectImporter.Pubspec.unquote("2.10.2+021002 # See README.md"));
        assertEquals("a#b", FlutterProjectImporter.Pubspec.unquote("a#b"));
        assertEquals("x # y", FlutterProjectImporter.Pubspec.unquote("\"x # y\" # z"));
        assertEquals("plain", FlutterProjectImporter.Pubspec.unquote("'plain'"));
    }

    private static void write(File f, String content) throws IOException {
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), content.getBytes("UTF-8"));
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), "UTF-8");
    }
}
