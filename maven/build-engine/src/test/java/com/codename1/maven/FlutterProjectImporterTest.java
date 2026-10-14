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
    public void setOrAppendPropertyAddsAMissingKeyButUpdatesAnExistingOneInPlace() {
        String props = "codename1.version=1.0\n";
        // Not declared yet (the archetype's settings file never ships either build-number
        // hint by default) -- so it is appended, unlike setProperty's "leave it alone".
        String withNew = FlutterProjectImporter.setOrAppendProperty(props, "codename1.arg.android.versionCode", "4");
        assertTrue(withNew, withNew.contains("codename1.arg.android.versionCode=4"));
        assertTrue(withNew, withNew.contains("codename1.version=1.0"));

        // Declared now: re-running (as a second import would) updates it in place,
        // never appending a second line for the same key.
        String updated = FlutterProjectImporter.setOrAppendProperty(withNew, "codename1.arg.android.versionCode",
                "9");
        assertEquals(2, updated.split("codename1\\.arg\\.android\\.versionCode=", -1).length);
        assertTrue(updated, updated.contains("codename1.arg.android.versionCode=9"));
    }

    @Test
    public void importSplitsPubspecVersionIntoMarketingAndBuildNumberHints() throws Exception {
        // Flutter's own template ships "1.0.0+1": PUBSPEC's "1.2.3+4" is the same
        // marketing+buildNumber shape. Copied in whole, "+4" is not
        // Float.parseFloat-compatible (AndroidGradleBuilder's default versionCode)
        // and is invalid inside CFBundleShortVersionString (IPhoneBuilder).
        File root = Files.createTempDirectory("flutter-version-split").toFile();
        File flutterProject = new File(root, "demo");
        write(new File(flutterProject, "pubspec.yaml"), PUBSPEC);
        write(new File(flutterProject, "lib/main.dart"), "void main() {}\n");
        write(new File(flutterProject, "assets/images/logo.png"), "PNGDATA");
        write(new File(flutterProject, "assets/icons/a.png"), "ICONA");
        write(new File(flutterProject, "assets/fonts/Custom-Regular.ttf"), "FONTDATA");

        File common = new File(root, "common");
        write(new File(common, "codenameone_settings.properties"),
                "codename1.version=1.0\ncodename1.displayName=Old Name\n");

        new FlutterProjectImporter(new com.codename1.build.SystemStreamLog())
                .importProject(flutterProject, common, null, null);

        String settings = read(new File(common, "codenameone_settings.properties"));
        assertTrue(settings, settings.contains("codename1.version=1.2.3"));
        assertFalse("the marketing version must not carry the build number",
                settings.contains("codename1.version=1.2.3+4"));
        assertTrue(settings, settings.contains("codename1.arg.android.versionCode=4"));
        assertTrue(settings, settings.contains("codename1.arg.ios.bundleVersion=4"));
        assertTrue(settings, settings.contains("codename1.displayName=demo_app"));
    }

    @Test
    public void importWithNoBuildNumberLeavesTheVersionHintsUnset() throws Exception {
        File root = Files.createTempDirectory("flutter-version-no-build-number").toFile();
        File flutterProject = new File(root, "demo");
        write(new File(flutterProject, "pubspec.yaml"), "name: demo\nversion: 1.0.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\n");
        write(new File(flutterProject, "lib/main.dart"), "void main() {}\n");

        File common = new File(root, "common");
        write(new File(common, "codenameone_settings.properties"), "codename1.version=1.0\n");

        new FlutterProjectImporter(new com.codename1.build.SystemStreamLog())
                .importProject(flutterProject, common, null, null);

        String settings = read(new File(common, "codenameone_settings.properties"));
        assertTrue(settings, settings.contains("codename1.version=1.0.0"));
        assertFalse(settings, settings.contains("android.versionCode"));
        assertFalse(settings, settings.contains("ios.bundleVersion"));
    }

    /// Flutter allows an asset or font declared anywhere project-relative, not
    /// only under `assets/` or `packages/` -- `fonts/MyFont.ttf` is exactly the
    /// shape a font-only pubspec entry takes. The importer must both copy the
    /// file (already true) and record the key in ASSET_KEYS_RECORD, which is
    /// what lets TranscodeFlutterMojo find it outside the two roots it scans.
    @Test
    public void recordsDeclaredAssetKeysOutsideTheLegacyRootsForTheMojoToFlatten() throws Exception {
        File root = Files.createTempDirectory("flutter-declared-asset-keys").toFile();
        File flutterProject = new File(root, "demo");
        write(new File(flutterProject, "pubspec.yaml"), "name: demo\nversion: 1.0.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\n"
                + "flutter:\n  assets:\n    - images/logo.png\n"
                + "  fonts:\n    - family: Custom\n      fonts:\n        - asset: fonts/Custom-Regular.ttf\n");
        write(new File(flutterProject, "lib/main.dart"), "void main() {}\n");
        write(new File(flutterProject, "images/logo.png"), "PNGDATA");
        write(new File(flutterProject, "fonts/Custom-Regular.ttf"), "FONTDATA");

        File common = new File(root, "common");
        new FlutterProjectImporter(new com.codename1.build.SystemStreamLog())
                .importProject(flutterProject, common, null, null);

        File flutterDir = new File(common, "src/main/flutter");
        assertTrue(new File(flutterDir, "images/logo.png").isFile());
        assertTrue(new File(flutterDir, "fonts/Custom-Regular.ttf").isFile());

        File record = new File(flutterDir, FlutterProjectImporter.ASSET_KEYS_RECORD);
        assertTrue(record.isFile());
        assertEquals(java.util.Arrays.asList("fonts/Custom-Regular.ttf", "images/logo.png"),
                Files.readAllLines(record.toPath(), java.nio.charset.StandardCharsets.UTF_8));
    }

    /// Flutter bundles a single-file asset's resolution-aware siblings --
    /// `2.0x/logo.png`, `3.0x/logo.png` -- without a separate pubspec entry
    /// for each. The importer must find and copy them beside the declared
    /// file, and record each one's own key so `TranscodeFlutterMojo` packages
    /// it; a sibling directory that is not a `<N>x` variant, or a same-named
    /// file in a directory belonging to some other asset, must not be swept
    /// in by mistake.
    @Test
    public void copiesResolutionVariantSiblingsOfASingleFileAssetKeyAndRecordsThem() throws Exception {
        File root = Files.createTempDirectory("flutter-asset-variants").toFile();
        File flutterProject = new File(root, "demo");
        write(new File(flutterProject, "pubspec.yaml"), "name: demo\nversion: 1.0.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\n"
                + "flutter:\n  assets:\n    - assets/logo.png\n");
        write(new File(flutterProject, "lib/main.dart"), "void main() {}\n");
        write(new File(flutterProject, "assets/logo.png"), "LOGO-1X");
        write(new File(flutterProject, "assets/2.0x/logo.png"), "LOGO-2X");
        write(new File(flutterProject, "assets/3.0x/logo.png"), "LOGO-3X");
        // A same-named file sitting in a directory that is not a "<N>x" variant
        // name must not be treated as a resolution variant.
        write(new File(flutterProject, "assets/notaratio/logo.png"), "LOGO-NOT-A-VARIANT");
        // A different file inside a real variant directory belongs to some
        // other (undeclared) asset here, not to "logo.png".
        write(new File(flutterProject, "assets/2.0x/other.png"), "OTHER-2X");

        File common = new File(root, "common");
        new FlutterProjectImporter(new com.codename1.build.SystemStreamLog())
                .importProject(flutterProject, common, null, null);

        File flutterDir = new File(common, "src/main/flutter");
        assertEquals("LOGO-1X", read(new File(flutterDir, "assets/logo.png")));
        assertEquals("LOGO-2X", read(new File(flutterDir, "assets/2.0x/logo.png")));
        assertEquals("LOGO-3X", read(new File(flutterDir, "assets/3.0x/logo.png")));
        assertFalse("a sibling directory that is not a resolution-variant name must be ignored",
                new File(flutterDir, "assets/notaratio/logo.png").exists());
        assertFalse("a different file in a variant directory belongs to another asset",
                new File(flutterDir, "assets/2.0x/other.png").exists());

        File record = new File(flutterDir, FlutterProjectImporter.ASSET_KEYS_RECORD);
        assertEquals(java.util.Arrays.asList("assets/2.0x/logo.png", "assets/3.0x/logo.png", "assets/logo.png"),
                Files.readAllLines(record.toPath(), java.nio.charset.StandardCharsets.UTF_8));
    }

    /// The same bundling rule applies to a package asset resolved through
    /// `.dart_tool/package_config.json`: the variant lives beside the file in
    /// the package's own `lib/`, not in the importing app's tree.
    @Test
    public void copiesResolutionVariantSiblingsOfAPackageAsset() throws Exception {
        File root = Files.createTempDirectory("flutter-pkg-asset-variants").toFile();
        File flutterProject = new File(root, "demo");
        write(new File(flutterProject, "pubspec.yaml"), "name: demo\nversion: 1.0.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\n  my_pkg: 1.0.0\n"
                + "flutter:\n  assets:\n    - packages/my_pkg/logo.png\n");
        write(new File(flutterProject, "lib/main.dart"), "void main() {}\n");

        File pkgRoot = new File(root, "fake-pub-cache/my_pkg-1.0.0");
        write(new File(pkgRoot, "lib/logo.png"), "PKG-LOGO-1X");
        write(new File(pkgRoot, "lib/2.0x/logo.png"), "PKG-LOGO-2X");

        File dartTool = new File(flutterProject, ".dart_tool");
        write(new File(dartTool, "package_config.json"), "{\n  \"configVersion\": 2,\n  \"packages\": [\n"
                + "    {\n      \"name\": \"my_pkg\",\n      \"rootUri\": \"" + pkgRoot.toURI()
                + "\",\n      \"packageUri\": \"lib/\"\n    }\n  ]\n}\n");

        File common = new File(root, "common");
        new FlutterProjectImporter(new com.codename1.build.SystemStreamLog())
                .importProject(flutterProject, common, null, null);

        File flutterDir = new File(common, "src/main/flutter");
        assertEquals("PKG-LOGO-1X", read(new File(flutterDir, "packages/my_pkg/logo.png")));
        assertEquals("PKG-LOGO-2X", read(new File(flutterDir, "packages/my_pkg/2.0x/logo.png")));

        File record = new File(flutterDir, FlutterProjectImporter.ASSET_KEYS_RECORD);
        assertEquals(java.util.Arrays.asList("packages/my_pkg/2.0x/logo.png", "packages/my_pkg/logo.png"),
                Files.readAllLines(record.toPath(), java.nio.charset.StandardCharsets.UTF_8));
    }

    /// A variant dropped from the Flutter project (not just the main asset)
    /// must stop being bundled on the next import, the same way a dropped
    /// main asset already does.
    @Test
    public void reimportRemovesAStaleResolutionVariantNoLongerPresent() throws Exception {
        File root = Files.createTempDirectory("flutter-reimport-variant").toFile();
        File flutterProject = new File(root, "demo");
        write(new File(flutterProject, "pubspec.yaml"), "name: demo\nversion: 1.0.0\n"
                + "dependencies:\n  flutter:\n    sdk: flutter\n"
                + "flutter:\n  assets:\n    - assets/logo.png\n");
        write(new File(flutterProject, "lib/main.dart"), "void main() {}\n");
        write(new File(flutterProject, "assets/logo.png"), "LOGO-1X");
        File variant = new File(flutterProject, "assets/2.0x/logo.png");
        write(variant, "LOGO-2X");

        File common = new File(root, "common");
        FlutterProjectImporter importer = new FlutterProjectImporter(new com.codename1.build.SystemStreamLog());
        importer.importProject(flutterProject, common, null, null);

        File flutterDir = new File(common, "src/main/flutter");
        assertTrue(new File(flutterDir, "assets/2.0x/logo.png").isFile());

        assertTrue(variant.delete());
        importer.importProject(flutterProject, common, null, null);

        assertFalse("the dropped variant must not stay behind",
                new File(flutterDir, "assets/2.0x/logo.png").exists());
        File record = new File(flutterDir, FlutterProjectImporter.ASSET_KEYS_RECORD);
        assertEquals(java.util.Collections.singletonList("assets/logo.png"),
                Files.readAllLines(record.toPath(), java.nio.charset.StandardCharsets.UTF_8));
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
