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
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Imports a Flutter project into a Codename One application: copies its
/// `lib` tree into `src/main/flutter` (where `cn1:transcode-flutter` picks it
/// up), copies the assets and fonts its `pubspec.yaml` declares, points the
/// Codename One entry point at the transpiled Dart `main()`, and reports
/// which `pubspec.yaml` dependencies the Flutter runtime's stubs cover.
public final class FlutterProjectImporter {

    /// Pub package names the transpiler's stub set gives Dart API for, by
    /// package name. Unlike [AndroidProjectImporter#COVERED] (a stable,
    /// curated Gradle coordinate list) this can only ever be the handful of
    /// pub packages the runtime actually implements signatures for -- most
    /// pub packages have no Codename One counterpart at all.
    static final Map<String, String> COVERED = new LinkedHashMap<String, String>();

    static {
        COVERED.put("flutter", "Material and Cupertino widget stubs (codenameone-flutter-runtime)");
        COVERED.put("flutter_localizations", "localization delegate scaffolding only; strings are not translated");
        COVERED.put("cupertino_icons", "CupertinoIcons constants (flutter-runtime stub); "
                + "the font file itself still needs its own pubspec.yaml fonts: entry to be copied");
        // The rest are verified against the actual stub sources (maven/flutter-runtime's
        // META-INF/dart/*.dart and the transpiler's embedded stubs), not guessed at: each
        // entry names the real stub class/function backing it, so a pub package that is
        // merely MENTIONED in a code comment (package:collection's case) does not count.
        COVERED.put("provider", "Provider/ChangeNotifierProvider/Consumer stubs (flutter-runtime)");
        COVERED.put("animations", "OpenContainer/PageTransitionSwitcher/SharedAxisTransition stubs");
        COVERED.put("intl", "DateFormat/NumberFormat/Intl stubs");
        COVERED.put("google_fonts", "GoogleFonts stub (bundled/asset fonts only, no network fetch)");
        COVERED.put("url_launcher", "launchUrl/launchUrlString stubs");
        COVERED.put("scoped_model", "ScopedModel/ScopedModelDescendant stubs");
        COVERED.put("vector_math", "Matrix4/Vector3 stubs");
        COVERED.put("flutter_localized_locales", "LocaleNames/LocaleNamesLocalizationsDelegate stubs");
        COVERED.put("adaptive_breakpoints", "AdaptiveBreakpoints.getWindowType only; "
                + "isDisplayDesktop/isDisplaySmallDesktop/isDisplayMobile are not stubbed");
        // NOT covered, checked and confirmed absent: "collection" (no ListEquality/MapEquality/
        // SetEquality stub -- the one hit on that name is a comment, not an implementation),
        // "flutter_staggered_grid_view" and "nested" (provider's internal composition helper,
        // never imported directly by application code).
    }

    /// Pub dependencies that only matter to the Dart tooling and tests, never
    /// reached by a transpiled build.
    static final String[] IGNORED = {"flutter_test", "flutter_driver", "integration_test", "test", "build_runner"};

    /// The package `cn1:transcode-flutter` (`TranscodeFlutterMojo`) generates
    /// the transpiled Dart sources into when a project does not override
    /// `cn1.flutter.package` -- the same default that Mojo's own `@Parameter`
    /// declares. The generated entry point's `FlutterRegistry.invokeMain()`
    /// call must agree with whichever package the project actually configured,
    /// never this constant by itself: see [#importProject].
    static final String DEFAULT_FLUTTER_PACKAGE = "com.codename1.generated.flutter";

    /// flutter_tools' own resolution-variant directory name, matched exactly
    /// (`asset.dart`'s `_assetVariantDirectoryRegExp`): one or more digits,
    /// optionally a `.` and zero or more further digits, then `x` --
    /// `"2x"`, `"3.0x"`, `"1.5x"`, even the edge case `"2.x"` that regex also
    /// accepts. A pubspec asset key naming a single file never lists these
    /// directories itself; Flutter still bundles whichever of them hold a
    /// same-named file beside it, and [#copyAssetVariants] replicates that.
    private static final Pattern VARIANT_DIR = Pattern.compile("\\d+(\\.\\d*)?x");

    public static final class Result {
        public final List<String> covered = new ArrayList<String>();
        public final List<String> uncovered = new ArrayList<String>();
        /// A pubspec `assets:`/`fonts:` entry that does not exist under the
        /// Flutter project -- typically a `packages/<pub-package>/...` key
        /// whose bytes live in that package's own checkout or pub cache, not
        /// in this project.
        public final List<String> missingAssets = new ArrayList<String>();
        /// A pub package name a `packages/<name>/...` asset key needed, that
        /// neither `.dart_tool/package_config.json` nor the pub cache could
        /// resolve a directory for.
        public final List<String> unresolvedPackages = new ArrayList<String>();
        public String appName;
        public String appVersion;
        public int copiedFiles;
        public int copiedAssets;
    }

    private final Log log;

    public FlutterProjectImporter(Log log) {
        this.log = log;
    }

    /// Imports `source` (a Flutter project: `pubspec.yaml` beside a `lib`
    /// directory) into `commonDir`, with the generated entry point calling
    /// `FlutterRegistry.invokeMain()` in [#DEFAULT_FLUTTER_PACKAGE] -- correct
    /// only when the project has not overridden `cn1.flutter.package`. A
    /// caller that can see that configuration (`ImportFlutterProjectMojo`)
    /// must use the five-argument overload instead.
    public Result importProject(File source, File commonDir, String mainPackage, String mainClass)
            throws BuildException {
        return importProject(source, commonDir, mainPackage, mainClass, DEFAULT_FLUTTER_PACKAGE);
    }

    /// Like the four-argument [#importProject], but generates the entry point
    /// against `flutterPackage` instead of assuming [#DEFAULT_FLUTTER_PACKAGE]
    /// -- `cn1:transcode-flutter` emits `FlutterRegistry` into whichever
    /// package `cn1.flutter.package` configures (`TranscodeFlutterMojo`'s own
    /// `@Parameter`, same default), and the generated entry point's
    /// `invokeMain()` call must name that same package or the project fails
    /// to compile with "cannot find symbol" the moment the override differs
    /// from the hardcoded default.
    public Result importProject(File source, File commonDir, String mainPackage, String mainClass,
            String flutterPackage) throws BuildException {
        String entryPointPackage = flutterPackage == null ? DEFAULT_FLUTTER_PACKAGE : flutterPackage;
        File pubspecFile = new File(source, "pubspec.yaml");
        File libDir = new File(source, "lib");
        if (!pubspecFile.isFile()) {
            throw new BuildException("No pubspec.yaml under " + source + " -- point cn1.flutter.import at a "
                    + "Flutter project's root");
        }
        if (!libDir.isDirectory()) {
            throw new BuildException("No lib/ under " + source + " -- not a Flutter project");
        }
        Result r = new Result();
        File target = new File(commonDir, "src/main/flutter");
        try {
            Pubspec pubspec = Pubspec.parse(new String(Files.readAllBytes(pubspecFile.toPath()),
                    Charset.forName("UTF-8")));
            r.appName = pubspec.name;
            r.appVersion = pubspec.version;

            Map<String, String> previous = readImportRecord(target);
            Map<String, String> imported = new TreeMap<String, String>();

            File[] libChildren = libDir.listFiles();
            if (libChildren != null) {
                for (File child : libChildren) {
                    r.copiedFiles += copy(child, new File(target, child.getName()), child.getName(), previous,
                            imported);
                }
            }

            // A `packages/<name>/...` asset key points into a pub dependency's own
            // checkout, not this project: resolve it the same way `flutter pub get`
            // left it resolvable, via .dart_tool/package_config.json first and the
            // pub cache (by the version pubspec.lock locked) otherwise.
            Map<String, File> packageConfig = readPackageConfig(source);
            Map<String, String> lockVersions = readLockVersions(source);
            Map<String, File> resolvedPackageLibDirs = new HashMap<String, File>();
            Set<String> packagesWithCopiedAssets = new LinkedHashSet<String>();
            // Every declared key that resolved to real content, recorded for
            // TranscodeFlutterMojo -- see ASSET_KEYS_RECORD.
            Set<String> declaredAssetKeys = new TreeSet<String>();

            Set<String> assetKeys = new LinkedHashSet<String>();
            assetKeys.addAll(pubspec.assets);
            assetKeys.addAll(pubspec.fontAssets);
            for (String key : assetKeys) {
                String clean = key.endsWith("/") ? key.substring(0, key.length() - 1) : key;
                if (clean.length() == 0 || clean.contains("..")) {
                    continue;
                }
                File assetSrc = new File(source, clean.replace('/', File.separatorChar));
                String pkgName = null;
                if (!assetSrc.exists() && clean.startsWith("packages/")) {
                    int nameEnd = clean.indexOf('/', "packages/".length());
                    pkgName = nameEnd > 0 ? clean.substring("packages/".length(), nameEnd) : null;
                    String rest = nameEnd > 0 ? clean.substring(nameEnd + 1) : "";
                    File pkgLibDir;
                    if (resolvedPackageLibDirs.containsKey(pkgName)) {
                        pkgLibDir = resolvedPackageLibDirs.get(pkgName);
                    } else {
                        pkgLibDir = pkgName == null ? null
                                : resolvePackageLibDir(pkgName, packageConfig, lockVersions);
                        resolvedPackageLibDirs.put(pkgName, pkgLibDir);
                    }
                    if (pkgLibDir == null) {
                        if (pkgName != null && !r.unresolvedPackages.contains(pkgName)) {
                            r.unresolvedPackages.add(pkgName);
                            log.warn("Flutter package '" + pkgName + "' is not resolvable from "
                                    + ".dart_tool/package_config.json or the pub cache; run `flutter pub get` "
                                    + "once in " + source + ", or set PUB_CACHE if it is not at ~/.pub-cache");
                        }
                        r.missingAssets.add(key);
                        continue;
                    }
                    assetSrc = new File(pkgLibDir, rest.replace('/', File.separatorChar));
                }
                if (!assetSrc.exists()) {
                    // flutter_tools' own asset.dart (_AssetBundle.build) only reports a
                    // single-file key missing when NEITHER the nominal file NOR any of
                    // its resolution variants exist: "[the main entry] need not exist for
                    // an image if resolution-specific variant files exist." #copyAssetVariants
                    // needs only assetSrc's parent directory and name, not assetSrc itself,
                    // so it can be tried here even though the nominal file is absent.
                    int before = r.copiedAssets;
                    r.copiedAssets += copyAssetVariants(assetSrc, clean, target, previous, imported,
                            declaredAssetKeys);
                    if (r.copiedAssets == before) {
                        r.missingAssets.add(key);
                    } else if (pkgName != null) {
                        packagesWithCopiedAssets.add(pkgName);
                    }
                    continue;
                }
                declaredAssetKeys.add(clean);
                int before = r.copiedAssets;
                boolean singleFile = assetSrc.isFile();
                if (singleFile) {
                    r.copiedAssets += copy(assetSrc, new File(target, clean.replace('/', File.separatorChar)),
                            clean, previous, imported);
                    // A single-file key ("assets/logo.png") needs its resolution
                    // variants found separately, because Flutter bundles them
                    // without a second pubspec entry.
                    r.copiedAssets += copyAssetVariants(assetSrc, clean, target, previous, imported,
                            declaredAssetKeys);
                } else {
                    // A directory key ("assets/icons/") is NOT the same as
                    // recursing #copy through the whole subtree -- that copied
                    // every nested subdirectory unconditionally, but Flutter's
                    // own asset resolution for a declared directory bundles only
                    // the files directly inside it, plus the resolution-variant
                    // siblings (VARIANT_DIR) of those SAME files; any other
                    // subdirectory needs its own pubspec assets:/fonts: entry to
                    // be bundled at all. Copying it anyway shipped files no real
                    // `flutter build` would have included.
                    r.copiedAssets += copyAssetDirectory(assetSrc,
                            new File(target, clean.replace('/', File.separatorChar)), clean, previous, imported);
                }
                if (r.copiedAssets == before) {
                    // A declared directory with nothing in it yet; not an error.
                    log.debug("No files found for Flutter asset entry " + key);
                } else if (pkgName != null) {
                    packagesWithCopiedAssets.add(pkgName);
                }
            }

            removeStaleImports(target, previous, imported);
            writeImportRecord(target, imported);
            writeDeclaredAssetKeys(target, declaredAssetKeys);
            writePackageName(target, pubspec.name);

            if (mainPackage != null && mainClass != null) {
                writeEntryPoint(new File(commonDir, "src/main/java"), mainPackage, mainClass, entryPointPackage);
            }
            updateSettings(commonDir, pubspec);

            // A package with no Dart sources at all is pure artwork/fonts (flutter_gallery_assets,
            // rally_assets, shrine_images are exactly this for the gallery): once its declared
            // assets copied, there is nothing further for the runtime to "support" -- unlike an
            // API package, which is only covered when flutter-runtime actually stubs its classes.
            Map<String, String> assetOnlyCovered = new LinkedHashMap<String, String>();
            for (String pkgName : packagesWithCopiedAssets) {
                if (COVERED.containsKey(pkgName)) {
                    continue;
                }
                File pkgLibDir = resolvedPackageLibDirs.get(pkgName);
                if (pkgLibDir != null && !containsDartSource(pkgLibDir)) {
                    assetOnlyCovered.put(pkgName, "asset/font package; files copied from " + pkgLibDir);
                }
            }

            for (String dep : pubspec.dependencies) {
                boolean ignored = false;
                for (String i : IGNORED) {
                    if (i.equals(dep)) {
                        ignored = true;
                        break;
                    }
                }
                if (ignored) {
                    continue;
                }
                String note = COVERED.get(dep);
                if (note == null) {
                    note = assetOnlyCovered.get(dep);
                }
                if (note != null) {
                    r.covered.add(dep + " (" + note + ")");
                } else {
                    r.uncovered.add(dep);
                }
            }
        } catch (IOException e) {
            throw new BuildException("Import failed: " + e.getMessage(), e);
        }
        return r;
    }

    /// Rewrites `codename1.version`/`codename1.displayName` in
    /// `commonDir/codenameone_settings.properties` from the pubspec, but only
    /// when the project already declares that key -- this never introduces a
    /// setting the developer did not already have, the same caution
    /// [AndroidProjectImporter] applies to `codename1.packageName`.
    ///
    /// Flutter's `version:` is `marketing[+buildNumber]` (its own template
    /// ships `1.0.0+1`), and the two halves mean different things to the two
    /// native builders: AndroidGradleBuilder parses `codename1.version` with
    /// `Float.parseFloat` for its default `versionCode`, and IPhoneBuilder
    /// writes it straight into `CFBundleShortVersionString` (the marketing
    /// string App Review shows). Copying `"1.0.0+1"` there whole is wrong
    /// twice over: the `+1` is not `Float.parseFloat`-compatible --
    /// `AndroidGradleBuilder.computeVersionCode` falls back to a hardcoded
    /// `1` -- and `CFBundleShortVersionString` must not contain a `+` at
    /// all. The marketing half (before `+`) goes to `codename1.version`
    /// alone; the build number (after `+`) goes to the hints the builders
    /// actually read for their OWN build-number concept --
    /// `android.versionCode` (`BuildHintsAndroid`) and `ios.bundleVersion`
    /// (`CodenameOne/src/.../buildhints/Ios.java`, which backs
    /// `CFBundleVersion`) -- added rather than updated-if-present, since the
    /// archetype's settings file never declares either by default.
    private void updateSettings(File commonDir, Pubspec pubspec) throws IOException {
        File settings = new File(commonDir, "codenameone_settings.properties");
        if (!settings.isFile()) {
            return;
        }
        String s = new String(Files.readAllBytes(settings.toPath()), Charset.forName("UTF-8"));
        String updated = s;
        if (pubspec.version != null) {
            int plus = pubspec.version.indexOf('+');
            String marketing = plus < 0 ? pubspec.version : pubspec.version.substring(0, plus);
            updated = setProperty(updated, "codename1.version", marketing);
            if (plus >= 0 && plus < pubspec.version.length() - 1) {
                String buildNumber = pubspec.version.substring(plus + 1);
                updated = setOrAppendProperty(updated, "codename1.arg.android.versionCode", buildNumber);
                updated = setOrAppendProperty(updated, "codename1.arg.ios.bundleVersion", buildNumber);
            }
        }
        if (pubspec.name != null) {
            updated = setProperty(updated, "codename1.displayName", pubspec.name);
        }
        if (!updated.equals(s)) {
            Files.write(settings.toPath(), updated.getBytes(Charset.forName("UTF-8")));
            log.info("Set codename1.version/codename1.displayName from pubspec.yaml");
        }
    }

    /// Replaces the value of a `key=value` line in a `.properties` file,
    /// leaving every other line untouched; a `key` the file does not already
    /// declare is left alone rather than appended.
    static String setProperty(String properties, String key, String value) {
        Matcher m = Pattern.compile("(?m)^" + Pattern.quote(key) + "\\s*=.*$").matcher(properties);
        if (!m.find()) {
            return properties;
        }
        return m.replaceFirst(Matcher.quoteReplacement(key + "=" + value));
    }

    /// Like [#setProperty], but appends `key=value` as a new line when the
    /// file does not already declare `key`, instead of leaving it alone --
    /// used for the `android.versionCode`/`ios.bundleVersion` build hints,
    /// which (unlike `codename1.version`/`codename1.displayName`) the
    /// archetype's settings file never declares by default, so "update only
    /// if present" would silently drop the pubspec build number every time.
    static String setOrAppendProperty(String properties, String key, String value) {
        Matcher m = Pattern.compile("(?m)^" + Pattern.quote(key) + "\\s*=.*$").matcher(properties);
        if (m.find()) {
            return m.replaceFirst(Matcher.quoteReplacement(key + "=" + value));
        }
        String separator = properties.length() == 0 || properties.endsWith("\n") ? "" : "\n";
        return properties + separator + key + "=" + value + "\n";
    }

    private void writeEntryPoint(File javaDir, String pkg, String cls, String flutterPackage) throws IOException {
        File f = new File(javaDir, pkg.replace('.', File.separatorChar) + File.separator + cls + ".java");
        f.getParentFile().mkdirs();
        String src = "package " + pkg + ";\n\n"
                + "/// The Codename One entry point: starts the imported Flutter application's Dart `main()`.\n"
                + "public class " + cls + " extends com.codename1.system.Lifecycle {\n"
                + "    @Override\n"
                + "    public void runApp() {\n"
                + "        " + flutterPackage + ".FlutterRegistry.invokeMain();\n"
                + "    }\n"
                + "}\n";
        byte[] bytes = src.getBytes(Charset.forName("UTF-8"));
        if (f.isFile()) {
            File backup = new File(f.getPath() + ".pre-flutter-import");
            boolean unchanged = java.util.Arrays.equals(bytes, Files.readAllBytes(f.toPath()));
            if (backup.exists()) {
                // A repeated import: the backup already holds the application's
                // original class, and overwriting it would lose the only copy.
                if (!unchanged) {
                    log.warn(f.getName() + " was changed after an earlier import and was left as it is; "
                            + "delete it and import again to regenerate it");
                    return;
                }
            } else if (!unchanged) {
                Files.copy(f.toPath(), backup.toPath());
                log.info("Saved the previous " + f.getName() + " as " + backup.getName());
            }
        }
        Files.write(f.toPath(), bytes);
    }

    /// The manifest-free minimal record: one `<sha-256> <path>` line per file
    /// this import copied, `path` relative to `src/main/flutter`, `/`
    /// separated. Same semantics as [AndroidProjectImporter#IMPORT_RECORD]:
    /// a file the developer edited since the earlier import is kept with its
    /// earlier hash so the next import still recognizes the edit, and a file
    /// the earlier import copied that is no longer declared is removed.
    static final String IMPORT_RECORD = ".flutter-import-files";

    private static Map<String, String> readImportRecord(File target) throws IOException {
        Map<String, String> out = new TreeMap<String, String>();
        File f = new File(target, IMPORT_RECORD);
        if (!f.isFile()) {
            return out;
        }
        for (String line : Files.readAllLines(f.toPath(), Charset.forName("UTF-8"))) {
            int sp = line.indexOf(' ');
            if (sp > 0 && sp < line.length() - 1) {
                out.put(line.substring(sp + 1), line.substring(0, sp));
            }
        }
        return out;
    }

    private static void writeImportRecord(File target, Map<String, String> imported) throws IOException {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<String, String> e : imported.entrySet()) {
            b.append(e.getValue()).append(' ').append(e.getKey()).append('\n');
        }
        target.mkdirs();
        Files.write(new File(target, IMPORT_RECORD).toPath(), b.toString().getBytes(Charset.forName("UTF-8")));
    }

    /// File (beside [#IMPORT_RECORD], directly under `src/main/flutter`)
    /// listing every pubspec `assets:`/`fonts:` key this import resolved to
    /// real content, one per line, `/` separated -- exactly the path each
    /// key was copied to under `src/main/flutter`, whether its source was
    /// the Flutter project itself or a resolved pub package.
    ///
    /// `TranscodeFlutterMojo.copyAssets()` reads this to flatten keys
    /// outside the two roots it otherwise scans (`assets/`, `packages/`):
    /// Flutter allows an asset or font declared anywhere project-relative
    /// (`fonts/MyFont.ttf`, `images/logo.png`), and without this record the
    /// mojo had no way to learn such a key exists, so it silently packaged
    /// nothing for it on every port. Rewritten whole on every import, even
    /// to empty, so a key removed from pubspec.yaml stops being flattened
    /// too. A hand-wired project (no import, so no such file) is unaffected
    /// -- the mojo falls back to its existing assets/+packages scan alone.
    static final String ASSET_KEYS_RECORD = ".flutter-asset-keys";

    /// The pubspec `name:`, recorded beside the copied `lib/` so the transpiler
    /// can resolve the project's own `package:<name>/...` imports to those files
    /// (lib/ is copied flat, and pubspec.yaml is not copied). Read by
    /// `com.codename1.dart.transpiler.api.DartTranspiler`, which owns the name.
    static final String PACKAGE_NAME_RECORD = ".flutter-package";

    private static void writePackageName(File target, String name) throws IOException {
        File record = new File(target, PACKAGE_NAME_RECORD);
        if (name == null || name.trim().isEmpty()) {
            if (record.exists() && !record.delete()) {
                throw new IOException("Cannot remove " + record);
            }
            return;
        }
        target.mkdirs();
        Files.write(record.toPath(), (name.trim() + "\n").getBytes(Charset.forName("UTF-8")));
    }

    private static void writeDeclaredAssetKeys(File target, Set<String> keys) throws IOException {
        StringBuilder b = new StringBuilder();
        for (String k : keys) {
            b.append(k).append('\n');
        }
        target.mkdirs();
        Files.write(new File(target, ASSET_KEYS_RECORD).toPath(), b.toString().getBytes(Charset.forName("UTF-8")));
    }

    /// Copies `src` to `dest` (recursing through EVERY subdirectory
    /// unconditionally when `src` is a directory), recording each copied
    /// file's path (relative to `src/main/flutter`, `/` separated) and
    /// content hash in `imported`. Correct for `lib/`, whose Dart sources
    /// have no resolution-variant concept; a directory asset key goes
    /// through [#copyAssetDirectory] instead, which recurses selectively.
    private int copy(File src, File dest, String path, Map<String, String> previous, Map<String, String> imported)
            throws IOException {
        if (src.isDirectory()) {
            int n = 0;
            File[] files = src.listFiles();
            if (files != null) {
                for (File f : files) {
                    n += copy(f, new File(dest, f.getName()), path + "/" + f.getName(), previous, imported);
                }
            }
            return n;
        }
        String recorded = previous.get(path);
        if (recorded != null && dest.isFile() && !recorded.equals(sha256(dest))
                && !java.util.Arrays.equals(Files.readAllBytes(src.toPath()), Files.readAllBytes(dest.toPath()))) {
            log.warn("Kept " + dest + ": it was changed after the earlier import, so the Flutter project's copy "
                    + "was not imported; delete it and import again to take that copy");
            imported.put(path, recorded);
            return 0;
        }
        dest.getParentFile().mkdirs();
        Files.copy(src.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        imported.put(path, sha256(dest));
        return 1;
    }

    /// Copies every resolution-specific sibling of a single-file asset --
    /// `logo.png`'s `2.0x/logo.png`, `3.0x/logo.png` and so on -- that Flutter
    /// bundles without a second pubspec entry (flutter_tools' own rule,
    /// `asset.dart`'s `_AssetDirectoryCache.variantsFor`: any sibling directory
    /// matching [#VARIANT_DIR] holding a same-named file). `assetSrc` is the
    /// resolved main file's path, from either the Flutter project itself or a
    /// pub package's `lib/`; `cleanKey` is the asset key it was declared under
    /// (`assets/logo.png`, or `packages/<pkg>/assets/logo.png`), and each
    /// variant's own key is built from it the way flutter_tools names the
    /// variant file -- `assets/2.0x/logo.png`.
    ///
    /// `assetSrc` itself need not exist: only its parent directory and name are
    /// read, so [#importProject] also calls this for a single-file key whose
    /// nominal file is missing, to find any resolution variants before giving
    /// up on the key as a whole -- `asset.dart`'s own rule for that case.
    ///
    /// Every variant's key is added to `declaredAssetKeys` too, unconditionally
    /// once the variant file is found to exist -- the same rule [#importProject]
    /// applies to the main key itself. A key rooted at `assets/`/`packages/`
    /// does not strictly need that (`TranscodeFlutterMojo` already rescans those
    /// two roots whole, so a variant copied under them is found regardless), but
    /// any other project-relative key does: the mojo flattens such a key one
    /// file at a time from exactly this record, with no scan of its own to find
    /// the variant otherwise.
    private int copyAssetVariants(File assetSrc, String cleanKey, File target, Map<String, String> previous,
            Map<String, String> imported, Set<String> declaredAssetKeys) throws IOException {
        File dir = assetSrc.getParentFile();
        File[] siblings = dir == null ? null : dir.listFiles();
        if (siblings == null) {
            return 0;
        }
        String fileName = assetSrc.getName();
        int slash = cleanKey.lastIndexOf('/');
        String keyDir = slash < 0 ? "" : cleanKey.substring(0, slash + 1);
        int copied = 0;
        for (File sibling : siblings) {
            if (!sibling.isDirectory() || !VARIANT_DIR.matcher(sibling.getName()).matches()) {
                continue;
            }
            File variantFile = new File(sibling, fileName);
            if (!variantFile.isFile()) {
                continue;
            }
            String variantKey = keyDir + sibling.getName() + "/" + fileName;
            declaredAssetKeys.add(variantKey);
            copied += copy(variantFile, new File(target, variantKey.replace('/', File.separatorChar)), variantKey,
                    previous, imported);
        }
        return copied;
    }

    /// Copies a directory asset key (`"assets/icons/"`) the way Flutter's own
    /// asset resolution limits it, rather than the unconditional recursion
    /// [#copy] does for `lib/`: the files directly inside `srcDir`, plus --
    /// for each sibling subdirectory matching [#VARIANT_DIR] -- only the
    /// files inside it that share a name with one of those direct files
    /// (the same per-file rule [#copyAssetVariants] applies to a single-file
    /// key, applied here to every file the directory declares at once). Any
    /// OTHER subdirectory is left alone: Flutter does not bundle it either
    /// unless the pubspec declares it as its own `assets:`/`fonts:` entry.
    private int copyAssetDirectory(File srcDir, File destDir, String path, Map<String, String> previous,
            Map<String, String> imported) throws IOException {
        File[] children = srcDir.listFiles();
        if (children == null) {
            return 0;
        }
        Set<String> directFileNames = new LinkedHashSet<String>();
        for (File f : children) {
            if (f.isFile()) {
                directFileNames.add(f.getName());
            }
        }
        int n = 0;
        for (File f : children) {
            if (f.isFile()) {
                n += copy(f, new File(destDir, f.getName()), path + "/" + f.getName(), previous, imported);
            } else if (f.isDirectory() && VARIANT_DIR.matcher(f.getName()).matches()) {
                File[] variantFiles = f.listFiles();
                if (variantFiles == null) {
                    continue;
                }
                for (File vf : variantFiles) {
                    if (vf.isFile() && directFileNames.contains(vf.getName())) {
                        n += copy(vf, new File(destDir, f.getName() + File.separator + vf.getName()),
                                path + "/" + f.getName() + "/" + vf.getName(), previous, imported);
                    }
                }
            }
        }
        return n;
    }

    /// Removes what an earlier import copied and this one did not -- a Dart
    /// file or asset renamed or removed upstream -- so it does not stay
    /// behind to be transpiled or packaged. Only a file listed in the earlier
    /// import's record is a candidate, and only while it still holds exactly
    /// the bytes that import wrote; a file the developer added, or edited
    /// since, is kept. Directories the removal empties go too.
    private void removeStaleImports(File target, Map<String, String> previous, Map<String, String> imported)
            throws IOException {
        for (Map.Entry<String, String> e : previous.entrySet()) {
            String path = e.getKey();
            if (imported.containsKey(path) || path.contains("..")) {
                continue;
            }
            File f = new File(target, path.replace('/', File.separatorChar));
            if (!f.isFile()) {
                continue;
            }
            if (!e.getValue().equals(sha256(f))) {
                log.warn("Kept " + f + ": it is no longer in the Flutter project, but it was changed after "
                        + "the earlier import");
                continue;
            }
            Files.delete(f.toPath());
            log.info("Removed " + f + ": it is no longer in the Flutter project");
            File dir = f.getParentFile();
            while (dir != null && !dir.equals(target)) {
                String[] left = dir.list();
                if (left == null || left.length > 0 || !dir.delete()) {
                    break;
                }
                dir = dir.getParentFile();
            }
        }
    }

    private static String sha256(File f) throws IOException {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(f.toPath()));
            StringBuilder b = new StringBuilder(d.length * 2);
            for (byte x : d) {
                b.append(Character.forDigit((x >> 4) & 0xf, 16)).append(Character.forDigit(x & 0xf, 16));
            }
            return b.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 is unavailable", e);
        }
    }

    /// A `"key": "value"` line inside `.dart_tool/package_config.json`, which
    /// `flutter pub get` always writes one field per line.
    private static final Pattern JSON_FIELD = Pattern.compile("\"([A-Za-z]+)\"\\s*:\\s*\"([^\"]*)\"\\s*,?");

    /// Maps each pub dependency `flutter pub get` resolved to its `lib/`
    /// directory, read from the Flutter project's
    /// `.dart_tool/package_config.json`. Empty when the file is absent or
    /// unreadable -- the caller falls back to the pub cache.
    private static Map<String, File> readPackageConfig(File flutterProjectDir) {
        Map<String, File> out = new HashMap<String, File>();
        File f = new File(flutterProjectDir, ".dart_tool" + File.separator + "package_config.json");
        if (!f.isFile()) {
            return out;
        }
        try {
            String json = new String(Files.readAllBytes(f.toPath()), Charset.forName("UTF-8"));
            URI base = new File(flutterProjectDir, ".dart_tool").toURI();
            boolean inPackages = false;
            String name = null;
            String rootUri = null;
            String packageUri = "lib/";
            for (String rawLine : json.split("\n", -1)) {
                String line = rawLine.trim();
                if (!inPackages) {
                    if (line.startsWith("\"packages\"")) {
                        inPackages = true;
                    }
                    continue;
                }
                if (line.startsWith("]")) {
                    break;
                }
                if (line.equals("{")) {
                    name = null;
                    rootUri = null;
                    packageUri = "lib/";
                    continue;
                }
                if (line.startsWith("}")) {
                    if (name != null && rootUri != null) {
                        putPackageLibDir(out, base, name, rootUri, packageUri);
                    }
                    continue;
                }
                Matcher m = JSON_FIELD.matcher(line);
                if (!m.matches()) {
                    continue;
                }
                String key = m.group(1);
                if ("name".equals(key)) {
                    name = m.group(2);
                } else if ("rootUri".equals(key)) {
                    rootUri = m.group(2);
                } else if ("packageUri".equals(key)) {
                    packageUri = m.group(2);
                }
            }
        } catch (IOException e) {
            // Unreadable package_config.json: resolved via the pub cache instead.
        }
        return out;
    }

    /// Resolves one `package_config.json` entry's `lib/` directory and adds
    /// it to `out`. `rootUri` is a `file:` URI, absolute or (for a path
    /// dependency) relative to `.dart_tool/`; `packageUri` is conventionally
    /// `lib/` but is read rather than assumed. A malformed URI for one
    /// package is skipped rather than failing every other one.
    private static void putPackageLibDir(Map<String, File> out, URI dartToolDir, String name, String rootUri,
            String packageUri) {
        try {
            URI root = dartToolDir.resolve(rootUri);
            String rootStr = root.toString();
            if (!rootStr.endsWith("/")) {
                // A relative reference resolved against a URI with no trailing
                // slash replaces its last segment instead of extending it --
                // the package's own directory name, here -- so packageUri must
                // be resolved against a URI that unambiguously names a directory.
                rootStr = rootStr + "/";
            }
            URI rootDir = URI.create(rootStr);
            URI lib = packageUri.length() == 0 ? rootDir : rootDir.resolve(packageUri);
            out.put(name, new File(lib));
        } catch (IllegalArgumentException e) {
            // Skip just this entry.
        }
    }

    /// A pub package's locked version, read from `pubspec.lock`'s own small
    /// line-based scan (see [Pubspec]). Used only when
    /// `.dart_tool/package_config.json` does not resolve a package -- a
    /// project that has never run `flutter pub get` has neither.
    private static Map<String, String> readLockVersions(File flutterProjectDir) {
        Map<String, String> out = new HashMap<String, String>();
        File f = new File(flutterProjectDir, "pubspec.lock");
        if (!f.isFile()) {
            return out;
        }
        try {
            String yaml = new String(Files.readAllBytes(f.toPath()), Charset.forName("UTF-8"));
            boolean inPackages = false;
            String currentPkg = null;
            for (String rawLine : yaml.split("\n", -1)) {
                String trimmed = rawLine.trim();
                if (trimmed.length() == 0 || trimmed.charAt(0) == '#') {
                    continue;
                }
                int indent = 0;
                while (indent < rawLine.length() && rawLine.charAt(indent) == ' ') {
                    indent++;
                }
                if (indent == 0) {
                    inPackages = "packages:".equals(trimmed);
                    currentPkg = null;
                    continue;
                }
                if (!inPackages) {
                    continue;
                }
                if (indent == 2) {
                    Matcher m = Pattern.compile("^([A-Za-z0-9_.-]+):\\s*$").matcher(trimmed);
                    currentPkg = m.matches() ? m.group(1) : null;
                    continue;
                }
                if (currentPkg != null && indent == 4 && trimmed.startsWith("version:")) {
                    Matcher v = Pattern.compile("^version:\\s*(.+)$").matcher(trimmed);
                    if (v.matches()) {
                        out.put(currentPkg, Pubspec.unquote(v.group(1).trim()));
                    }
                }
            }
        } catch (IOException e) {
            // Unreadable pubspec.lock: the package stays unresolved.
        }
        return out;
    }

    /// `name`'s `lib/` directory: from `package_config`, or failing that
    /// from the pub cache at the version `lockVersions` locked
    /// (`$PUB_CACHE`, or `~/.pub-cache`, under `hosted/pub.dev/<name>-<version>/lib`).
    /// Null when neither resolves it.
    private static File resolvePackageLibDir(String name, Map<String, File> packageConfig,
            Map<String, String> lockVersions) {
        File fromConfig = packageConfig.get(name);
        if (fromConfig != null) {
            return fromConfig;
        }
        String version = lockVersions.get(name);
        if (version == null) {
            return null;
        }
        String pubCacheEnv = System.getenv("PUB_CACHE");
        File cacheRoot = pubCacheEnv != null && pubCacheEnv.length() > 0 ? new File(pubCacheEnv)
                : new File(System.getProperty("user.home"), ".pub-cache");
        File lib = new File(new File(new File(cacheRoot, "hosted" + File.separator + "pub.dev"),
                name + "-" + version), "lib");
        return lib.isDirectory() ? lib : null;
    }

    /// True when `dir` (or a subdirectory) holds at least one `.dart` file --
    /// the test for "this pub package is Dart API, not just artwork". Stops
    /// at the first one found.
    private static boolean containsDartSource(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return false;
        }
        for (File f : files) {
            if (f.isDirectory() ? containsDartSource(f) : f.getName().endsWith(".dart")) {
                return true;
            }
        }
        return false;
    }

    /// The handful of `pubspec.yaml` keys the importer needs, read with a
    /// small line-based scanner rather than a YAML library: build-engine has
    /// none, and `name:`/`version:`/`dependencies:`/`flutter: assets:` and
    /// `flutter: fonts:` are all that matters here. It is not a general YAML
    /// reader -- block scalars, anchors and flow collections are not
    /// recognized, and a pubspec using them for these particular keys (rare
    /// in practice) will simply not contribute to the list it misses.
    static final class Pubspec {
        String name;
        String version;
        final List<String> assets = new ArrayList<String>();
        final List<String> fontAssets = new ArrayList<String>();
        final List<String> dependencies = new ArrayList<String>();

        private static final Pattern TOP_KEY = Pattern.compile("^([A-Za-z0-9_.-]+):\\s*(.*)$");
        private static final Pattern DEP_KEY = Pattern.compile("^([A-Za-z0-9_.-]+):.*$");
        private static final Pattern FONT_ASSET = Pattern.compile("^-?\\s*asset:\\s*(.+)$");

        static Pubspec parse(String yaml) {
            Pubspec p = new Pubspec();
            String currentTop = null;
            int depItemIndent = -1;
            boolean inAssetsList = false;
            int assetsBulletIndent = -1;
            for (String rawLine : yaml.split("\n", -1)) {
                String line = rawLine.length() > 0 && rawLine.charAt(rawLine.length() - 1) == '\r'
                        ? rawLine.substring(0, rawLine.length() - 1) : rawLine;
                String trimmed = line.trim();
                if (trimmed.length() == 0 || trimmed.charAt(0) == '#') {
                    continue;
                }
                int indent = 0;
                while (indent < line.length() && line.charAt(indent) == ' ') {
                    indent++;
                }
                if (indent == 0) {
                    Matcher top = TOP_KEY.matcher(trimmed);
                    if (top.matches()) {
                        currentTop = top.group(1);
                        if ("name".equals(currentTop)) {
                            p.name = unquote(top.group(2));
                        } else if ("version".equals(currentTop)) {
                            p.version = unquote(top.group(2));
                        }
                    } else {
                        currentTop = null;
                    }
                    depItemIndent = -1;
                    inAssetsList = false;
                    assetsBulletIndent = -1;
                    continue;
                }
                if ("dependencies".equals(currentTop)) {
                    Matcher dep = DEP_KEY.matcher(trimmed);
                    if (dep.matches()) {
                        if (depItemIndent == -1) {
                            depItemIndent = indent;
                        }
                        if (indent == depItemIndent) {
                            p.dependencies.add(dep.group(1));
                        }
                    }
                    continue;
                }
                if (!"flutter".equals(currentTop)) {
                    continue;
                }
                if ("assets:".equals(trimmed)) {
                    inAssetsList = true;
                    assetsBulletIndent = -1;
                    continue;
                }
                if (inAssetsList && trimmed.startsWith("- ")) {
                    if (assetsBulletIndent == -1) {
                        assetsBulletIndent = indent;
                    }
                    if (indent == assetsBulletIndent) {
                        p.assets.add(unquote(trimmed.substring(2).trim()));
                        continue;
                    }
                }
                inAssetsList = false;
                Matcher font = FONT_ASSET.matcher(trimmed);
                if (font.matches()) {
                    p.fontAssets.add(unquote(font.group(1).trim()));
                }
            }
            return p;
        }

        /// A plain scalar: a trailing ` # comment` dropped (YAML starts a comment
        /// only at a `#` preceded by whitespace, so `a#b` keeps its `#`), then one
        /// pair of matching quotes removed. A quoted value keeps a `#` inside the
        /// quotes. The gallery's `version: 2.10.2+021002 # See README.md` came
        /// through whole before this.
        static String unquote(String s) {
            String v = s.trim();
            if (!v.startsWith("\"") && !v.startsWith("'")) {
                for (int i = 1; i < v.length(); i++) {
                    if (v.charAt(i) == '#' && Character.isWhitespace(v.charAt(i - 1))) {
                        v = v.substring(0, i).trim();
                        break;
                    }
                }
            } else {
                char q = v.charAt(0);
                int close = v.indexOf(q, 1);
                if (close > 0) {
                    v = v.substring(0, close + 1);
                }
            }
            if (v.length() >= 2 && ((v.charAt(0) == '"' && v.charAt(v.length() - 1) == '"')
                    || (v.charAt(0) == '\'' && v.charAt(v.length() - 1) == '\''))) {
                v = v.substring(1, v.length() - 1);
            }
            return v;
        }
    }
}
