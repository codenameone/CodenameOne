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
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
        COVERED.put("url_launcher", "launchUrl/launchUrlString/canLaunchUrl; LaunchMode.inAppWebView and "
                + "inAppBrowserView open an in-app BrowserComponent, the others the system handler");
        COVERED.put("scoped_model", "ScopedModel/ScopedModelDescendant stubs");
        COVERED.put("vector_math", "Matrix4/Vector3 stubs");
        COVERED.put("flutter_localized_locales", "LocaleNames/LocaleNamesLocalizationsDelegate stubs");
        COVERED.put("adaptive_breakpoints", "AdaptiveBreakpoints.getWindowType only; "
                + "isDisplayDesktop/isDisplaySmallDesktop/isDisplayMobile are not stubbed");
        // Plugin and state-management packs (flutter-runtime com.codename1.flutter.plugins.*,
        // META-INF/dart/plugin_*.dart). Stubbed rather than transpiled from pub: the
        // plugins call platform channels, and bloc/go_router depend on reified generics
        // and the Router page API, neither of which a transpile can reproduce.
        COVERED.put("bloc", "Bloc/Cubit/Emitter/BlocObserver (plugins.bloc)");
        COVERED.put("flutter_bloc", "BlocProvider/BlocBuilder/BlocListener/BlocConsumer/BlocSelector/"
                + "RepositoryProvider and their Multi* forms (plugins.bloc)");
        COVERED.put("equatable", "Equatable with deep props equality (plugins.equatable)");
        COVERED.put("json_annotation", "@JsonSerializable/@JsonKey/@JsonValue and $enumDecode "
                + "for committed *.g.dart sources (plugins.json)");
        COVERED.put("shared_preferences", "SharedPreferences over Storage (plugins.sharedpreferences)");
        COVERED.put("path_provider", "application/support/temporary/cache directories (plugins.pathprovider)");
        COVERED.put("dio", "Dio/BaseOptions/Options/Response/DioException/interceptors/FormData "
                + "over ConnectionRequest (plugins.dio)");
        COVERED.put("go_router", "GoRouter/GoRoute/ShellRoute/GoRouterState and the context.go/push/pop "
                + "extension; MaterialApp.router (plugins.gorouter)");
        COVERED.put("cached_network_image", "CachedNetworkImage/CachedNetworkImageProvider over "
                + "ConnectionRequest with placeholder, progress and error widgets (plugins.cachednetworkimage)");
        COVERED.put("cross_file", "XFile over FileSystemStorage or in-memory bytes (plugins.crossfile)");
        COVERED.put("share_plus", "SharePlus.instance.share and the static Share API over Display.share "
                + "(plugins.share)");
        COVERED.put("package_info_plus", "PackageInfo.fromPlatform from the build properties (plugins.packageinfo)");
        COVERED.put("device_info_plus", "DeviceInfoPlugin android/ios/deviceInfo from Display properties "
                + "(plugins.deviceinfo)");
        COVERED.put("connectivity_plus", "Connectivity.checkConnectivity/onConnectivityChanged over "
                + "NetworkManager (plugins.connectivity)");
        COVERED.put("geolocator", "Geolocator positions, permissions and distance/bearing over "
                + "LocationManager (plugins.geolocator)");
        COVERED.put("image_picker", "ImagePicker pickImage/pickVideo/pickMedia/pickMultiImage over Capture and "
                + "the gallery, with maxWidth/maxHeight resizing and imageQuality (plugins.imagepicker)");
        COVERED.put("app_links", "AppLinks initial link and link streams from the AppArg launch property "
                + "(plugins.applinks)");
        COVERED.put("flutter_local_notifications", "show/periodicallyShow/zonedSchedule/cancel and tap "
                + "callbacks over LocalNotification, background tap callback, Android channels and raw "
                + "sounds (plugins.notifications)");
        COVERED.put("firebase_core", "Firebase.initializeApp/FirebaseOptions (plugins.firebase)");
        COVERED.put("firebase_auth", "FirebaseAuth email/password, anonymous and federated credentials "
                + "(Google, Apple, OAuthProvider tokens through signInWithIdp) over the Identity Toolkit REST "
                + "API; signInWithProvider for Google and Apple (plugins.firebase, firebasegoogle, firebaseapple)");
        COVERED.put("google_sign_in", "GoogleSignIn signIn/signOut/currentUser/onCurrentUserChanged over "
                + "GoogleConnect's OIDC sign-in; signInSilently answers this run's account only (plugins.googlesignin)");
        COVERED.put("firebase_analytics", "FirebaseAnalytics events logged, screen views to AnalyticsService "
                + "(plugins.firebase)");
        COVERED.put("firebase_messaging", "FirebaseMessaging token, onMessage, onBackgroundMessage and "
                + "provisional permission over Codename One push (plugins.firebase)");
        COVERED.put("firebase_ai", "FirebaseAI googleAI/vertexAI, generateContent, countTokens and chat over "
                + "the Firebase AI Logic REST API with App Check and auth headers; streams are one-shot and "
                + "function calling is not run (plugins.firebaseai)");
        COVERED.put("firebase_app_check", "FirebaseAppCheck activate/getToken; only the debug provider yields "
                + "a token, the attestation providers are accepted (plugins.firebase)");
        COVERED.put("firebase_ui_auth", "SignInScreen, ProfileScreen, auth state actions and "
                + "FirebaseUIAuth.configureProviders over firebase_auth (plugins.firebaseui)");
        COVERED.put("firebase_ui_oauth", "OAuthProviderButton over the configured providers (plugins.firebaseui)");
        COVERED.put("firebase_ui_oauth_google", "GoogleProvider over GoogleConnect (plugins.firebaseuigoogle)");
        COVERED.put("firebase_ui_oauth_apple", "AppleProvider over AppleSignIn (plugins.firebaseuiapple)");
        COVERED.put("google_maps_flutter", "GoogleMap widget with markers, polylines, circles, hued and byte marker icons, camera "
                + "and taps over MapView "
                + "(plugins.googlemaps)");
        COVERED.put("flutter_timezone", "local zone id and available zones from java.util.TimeZone "
                + "(plugins.timezone)");
        COVERED.put("app_settings", "openAppSettings/openAppSettingsPanel through Display.execute "
                + "(plugins.appsettings)");
        COVERED.put("android_play_install_referrer", "API only; installReferrer always fails with "
                + "UnsupportedError (plugins.installreferrer)");
        COVERED.put("geocoding", "placemarkFromCoordinates/locationFromAddress over the Google Geocoding REST "
                + "API, key in the google.geocoding.key Display property (plugins.geocoding)");
        COVERED.put("file_picker", "FilePicker.platform pickFiles/saveFile/getDirectoryPath over the native "
                + "file dialogs (plugins.filepicker)");
        COVERED.put("audioplayers", "AudioPlayer with asset/url/file/bytes sources and its streams over "
                + "MediaManager (plugins.audioplayers)");
        COVERED.put("mixpanel_flutter", "track/identify/alias/super properties/People/flush over the Mixpanel "
                + "HTTP API, setServerURL for EU residency (plugins.mixpanel)");
        COVERED.put("flutter_web_auth_2", "authenticate through an in-app browser; cancel is a CANCELED "
                + "PlatformException (plugins.webauth)");
        COVERED.put("flutter_image_compress", "compressWithFile/List/AndGetFile/AssetImage over Image and "
                + "ImageIO, JPEG and PNG (plugins.imagecompress)");
        COVERED.put("image_picker_platform_interface", "ImagePickerPlatform.instance and getImageFromSource "
                + "(plugins.imagepicker)");
        COVERED.put("image_picker_android", "ImagePickerAndroid.registerWith; useAndroidPhotoPicker accepted "
                + "with no effect (plugins.imagepicker)");
        COVERED.put("webview_flutter", "WebViewController/NavigationDelegate/WebViewWidget over BrowserComponent; "
                + "a POST is sent as a GET (plugins.webview)");
        COVERED.put("video_player", "VideoPlayerController/VideoPlayer/VideoProgressIndicator over MediaManager; "
                + "no captions, speed or headers (plugins.videoplayer)");
        COVERED.put("video_thumbnail", "API only; every call fails with UnsupportedError, core extracts no "
                + "frames (plugins.videothumbnail)");
        COVERED.put("video_compress", "API only; compressVideo answers the ORIGINAL file uncompressed with a "
                + "warning (plugins.videocompress)");
        COVERED.put("camera", "availableCameras/CameraController/CameraPreview over com.codename1.camera "
                + "(plugins.camera)");
        COVERED.put("emoji_picker_flutter", "EmojiPicker as a tabbed grid of built-in emoji with recents and "
                + "search; Config ignored except height (plugins.emojipicker)");
        COVERED.put("flutter_svg", "SvgPicture.asset/network/string/file/memory through Image.createSVG where "
                + "the port renders SVG, otherwise a sized empty box (plugins.svg)");
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
        /// An asset whose path is also a file copied from `lib/` (`lib/assets/data.dart`
        /// beside a declared `assets/data.dart`): lib/ is copied flat into the same
        /// directory, so the two cannot both be there. The source is kept, every time,
        /// and the asset is not copied.
        public final List<String> assetCollisions = new ArrayList<String>();
        /// Pure-Dart pub packages whose sources were copied to be transpiled with the
        /// app, each with the number of its files the app reaches.
        public final Map<String, Integer> transpiledPackages = new TreeMap<String, Integer>();
        /// A package the app imports that cannot be transpiled and has no stub, with
        /// the reason: a platform plugin, or a library that needs dart:ffi, dart:html
        /// or dart:mirrors. These are what an import of this app cannot provide.
        public final Map<String, String> unsupportedPackages = new TreeMap<String, String>();
        /// A plugin with no Android or iOS implementation, transpiled as Dart (see
        /// [#isForeignPlatformPlugin]), with the platforms it does implement.
        public final Map<String, String> foreignPlugins = new TreeMap<String, String>();
        /// For an unsupported package, the packages its verdict came through, ending with
        /// the one that is unsupported itself: `[firebase_ui_oauth, flutter_svg,
        /// vector_graphics_compiler]` when vector_graphics_compiler needs dart:ffi.
        public final Map<String, List<String>> unsupportedChains = new TreeMap<String, List<String>>();
        /// Every package the import reached, with the app's own `lib/` files that reach
        /// it, directly or through other packages.
        public final Map<String, Set<String>> reachedFrom = new TreeMap<String, Set<String>>();
        /// The Google Fonts families the app's own files name (`GoogleFonts.openSans(...)`,
        /// `GoogleFonts.latoTextTheme()`, `GoogleFonts.getFont('Rubik')`). The stub draws
        /// only fonts bundled with the app, so these are the ones to add as assets.
        public final Set<String> googleFonts = new java.util.TreeSet<String>();
        public String appName;
        public String appVersion;
        public int copiedFiles;
        public int copiedAssets;

        /// The report as JSON, for a tool or a CI step to read: every package with its
        /// status (`covered`, `unsupported`), the note or reason, the chain an
        /// unsupported verdict came through, and the app files that reach it.
        public String toJson() {
            StringBuilder b = new StringBuilder("{\n");
            b.append("  \"app\": ").append(jsonString(appName)).append(",\n");
            b.append("  \"version\": ").append(jsonString(appVersion)).append(",\n");
            b.append("  \"packages\": [");
            boolean first = true;
            for (int pass = 0; pass < 2; pass++) {
                for (String entry : pass == 0 ? covered : uncovered) {
                    int paren = entry.indexOf(" (");
                    String name = paren < 0 ? entry : entry.substring(0, paren);
                    String note = paren < 0 ? null : entry.substring(paren + 2, entry.length() - 1);
                    b.append(first ? "\n" : ",\n");
                    first = false;
                    b.append("    {\"package\": ").append(jsonString(name));
                    b.append(", \"status\": \"").append(pass == 0 ? "covered" : "unsupported").append('"');
                    if (note != null) {
                        b.append(", \"note\": ").append(jsonString(note));
                    }
                    List<String> chain = unsupportedChains.get(name);
                    if (pass == 1 && chain != null) {
                        b.append(", \"cause\": ").append(jsonString(unsupportedPackages.get(chain.get(chain.size() - 1))));
                        b.append(", \"chain\": ").append(jsonArray(chain));
                    }
                    Set<String> from = reachedFrom.get(name);
                    if (from != null) {
                        b.append(", \"reachedFrom\": ").append(jsonArray(from));
                    }
                    b.append('}');
                }
            }
            b.append("\n  ],\n");
            b.append("  \"missingAssets\": ").append(jsonArray(missingAssets)).append(",\n");
            b.append("  \"unresolvedPackages\": ").append(jsonArray(unresolvedPackages)).append(",\n");
            b.append("  \"assetCollisions\": ").append(jsonArray(assetCollisions)).append(",\n");
            b.append("  \"googleFonts\": ").append(jsonArray(googleFonts)).append("\n");
            return b.append("}\n").toString();
        }

        private static String jsonArray(java.util.Collection<String> items) {
            StringBuilder b = new StringBuilder("[");
            for (String i : items) {
                if (b.length() > 1) {
                    b.append(", ");
                }
                b.append(jsonString(i));
            }
            return b.append(']').toString();
        }

        private static String jsonString(String v) {
            if (v == null) {
                return "null";
            }
            StringBuilder b = new StringBuilder("\"");
            for (int i = 0; i < v.length(); i++) {
                char c = v.charAt(i);
                if (c == '"' || c == '\\') {
                    b.append('\\').append(c);
                } else if (c < 0x20) {
                    b.append(String.format("\\u%04x", (int) c));
                } else {
                    b.append(c);
                }
            }
            return b.append('"').toString();
        }
    }

    private final Log log;
    /// The paths the current import copied from lib/; an asset never overwrites one.
    private Set<String> sourcePaths;
    private final List<String> collisions = new ArrayList<String>();

    public FlutterProjectImporter(Log log) {
        this.log = log;
    }

    /// Imports `source` (a Flutter project: `pubspec.yaml` beside a `lib`
    /// directory) into `commonDir`, with the generated entry point calling
    /// `FlutterRegistry.invokeMain()` in [#DEFAULT_FLUTTER_PACKAGE] -- correct
    /// only when the project has not overridden `cn1.flutter.package`. A
    /// caller that can see that configuration (`ImportFlutterProjectMojo`)
    /// must use the five-argument overload instead.
    /// [#COVERED] plus what the stub packs of this import declare.
    private final Map<String, String> coverage = new LinkedHashMap<String, String>();
    private final List<File> stubPacks = new ArrayList<File>();

    /// Stub packs beyond the Flutter runtime's own -- jars, or class/resource directories,
    /// with `META-INF/dart/*.dart` stubs -- whose `// cn1:covers` lines (see
    /// [#declaredCoverage]) count as covered. The application's own
    /// `common/src/main/resources` is always read.
    public FlutterProjectImporter withStubPacks(java.util.Collection<File> packs) {
        stubPacks.addAll(packs);
        return this;
    }

    private static final Pattern COVERS = Pattern.compile("(?m)^//\\s*cn1:covers\\s+([A-Za-z0-9_]+)\\s*(.*)$");

    /// A stub file declares the pub packages it stands in for, one line each, so a third-party
    /// pack is reported -- and its package not followed into -- like a built-in one:
    ///
    /// ```
    /// // cn1:covers lottie Lottie.asset/network over a Codename One animation
    /// ```
    static void declaredCoverage(List<File> packs, Map<String, String> out) {
        for (File pack : packs) {
            try {
                if (pack.isDirectory()) {
                    File[] stubs = new File(pack, "META-INF/dart").listFiles();
                    if (stubs != null) {
                        for (File f : stubs) {
                            if (f.isFile() && f.getName().endsWith(".dart")) {
                                covers(new String(Files.readAllBytes(f.toPath()), Charset.forName("UTF-8")), out);
                            }
                        }
                    }
                } else if (pack.isFile()) {
                    java.util.zip.ZipFile z = new java.util.zip.ZipFile(pack);
                    try {
                        java.util.Enumeration<? extends java.util.zip.ZipEntry> e = z.entries();
                        while (e.hasMoreElements()) {
                            java.util.zip.ZipEntry ze = e.nextElement();
                            String n = ze.getName();
                            if (!ze.isDirectory() && n.startsWith("META-INF/dart/") && n.endsWith(".dart")
                                    && n.indexOf('/', "META-INF/dart/".length()) < 0) {
                                InputStream in = z.getInputStream(ze);
                                try {
                                    covers(new String(readAll(in), Charset.forName("UTF-8")), out);
                                } finally {
                                    in.close();
                                }
                            }
                        }
                    } finally {
                        z.close();
                    }
                }
            } catch (IOException ex) {
                // An unreadable pack declares nothing; the import reports its packages as it would without it.
            }
        }
    }

    private static void covers(String stub, Map<String, String> out) {
        Matcher m = COVERS.matcher(stub);
        while (m.find()) {
            String note = m.group(2).trim();
            out.put(m.group(1), note.isEmpty() ? "a stub pack on the class path" : note);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) {
            b.write(buf, 0, n);
        }
        return b.toByteArray();
    }

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
        coverage.clear();
        coverage.putAll(COVERED);
        List<File> packs = new ArrayList<File>(stubPacks);
        packs.add(new File(commonDir, "src/main/resources"));
        declaredCoverage(packs, coverage);
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

            sourcePaths = null;
            File[] libChildren = libDir.listFiles();
            if (libChildren != null) {
                for (File child : libChildren) {
                    r.copiedFiles += copy(child, new File(target, child.getName()), child.getName(), previous,
                            imported);
                }
            }
            // The pub packages the app imports: pure-Dart ones are transpiled from their
            // own sources, stubbed ones come from the runtime, anything else is reported.
            Map<String, File> packageConfig = readPackageConfig(source);
            Map<String, String> lockVersions = readLockVersions(source);
            List<String> packageSources = new ArrayList<String>();
            r.copiedFiles += importPackageSources(libDir, pubspec.name, packageConfig, lockVersions, target,
                    previous, imported, packageSources, r);
            // Everything copied so far came from lib/. An asset landing on one of those
            // paths silently replaced the source on a first import, and a later import's
            // kept-local-edit check could keep either -- the result depended on how many
            // times import had run. The source wins, always, and the clash is reported.
            sourcePaths = new java.util.HashSet<String>(imported.keySet());
            collisions.clear();

            // A `packages/<name>/...` asset key points into a pub dependency's own
            // checkout, not this project: resolve it the same way `flutter pub get`
            // left it resolvable, via .dart_tool/package_config.json first and the
            // pub cache (by the version pubspec.lock locked) otherwise.
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
                if (clean.length() == 0 || hasParentSegment(clean)) {
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
            r.assetCollisions.addAll(collisions);
            sourcePaths = null;
            writeImportRecord(target, imported);
            writeLibSources(target, libDir, packageSources);
            writeDeclaredAssetKeys(target, declaredAssetKeys);
            writePackageName(target, pubspec.name);

            if (mainPackage != null && mainClass != null) {
                writeEntryPoint(new File(commonDir, "src/main/java"), mainPackage, mainClass, entryPointPackage,
                        pubspec.dependencies);
            }
            updateSettings(commonDir, pubspec);

            // A package with no Dart sources at all is pure artwork/fonts (flutter_gallery_assets,
            // rally_assets, shrine_images are exactly this for the gallery): once its declared
            // assets copied, there is nothing further for the runtime to "support" -- unlike an
            // API package, which is only covered when flutter-runtime actually stubs its classes.
            Map<String, String> assetOnlyCovered = new LinkedHashMap<String, String>();
            for (String pkgName : packagesWithCopiedAssets) {
                if (coverage.containsKey(pkgName)) {
                    continue;
                }
                File pkgLibDir = resolvedPackageLibDirs.get(pkgName);
                if (pkgLibDir != null && !containsDartSource(pkgLibDir)) {
                    assetOnlyCovered.put(pkgName, "asset/font package; files copied from " + pkgLibDir);
                }
            }

            Set<String> reachedPackages = new HashSet<String>(lastReachedPackages);
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
                String note = coverage.get(dep);
                if (note == null) {
                    note = assetOnlyCovered.get(dep);
                }
                if ("google_fonts".equals(dep) && note != null && !r.googleFonts.isEmpty()) {
                    note += "; bundle these families as assets: " + String.join(", ", r.googleFonts);
                }
                if (note == null && r.foreignPlugins.containsKey(dep) && r.transpiledPackages.containsKey(dep)) {
                    note = "a " + r.foreignPlugins.get(dep) + "-only plugin, transpiled as Dart; its platform "
                            + "calls throw MissingPluginException";
                }
                if (note == null && r.transpiledPackages.containsKey(dep)) {
                    note = "transpiled from its sources, " + r.transpiledPackages.get(dep) + " file(s)";
                }
                if (note == null && !r.unsupportedPackages.containsKey(dep) && !reachedPackages.contains(dep)) {
                    note = "not imported by lib/";
                }
                if (note != null) {
                    r.covered.add(dep + " (" + note + ")");
                } else {
                    String why = r.unsupportedPackages.get(dep);
                    r.uncovered.add(why == null ? dep : dep + " (" + why + ")");
                }
            }
            // A package reached only through another one (a transitive dependency) is not
            // in pubspec.yaml's list, but the app cannot build without it either.
            for (Map.Entry<String, String> u : r.unsupportedPackages.entrySet()) {
                if (!pubspec.dependencies.contains(u.getKey())) {
                    r.uncovered.add(u.getKey() + " (" + u.getValue() + ", needed by a dependency)");
                }
            }
        } catch (IOException e) {
            throw new BuildException("Import failed: " + e.getMessage(), e);
        }
        return r;
    }

    /// SDK packages the runtime itself provides; never copied, never reported.
    static final Set<String> SDK_PACKAGES = new HashSet<String>(java.util.Arrays.asList(
            "flutter", "flutter_test", "flutter_driver", "flutter_web_plugins", "flutter_localizations",
            "sky_engine", "integration_test"));

    /// The `dart:` libraries the transpiler implements -- its configurable-import
    /// test answers true for exactly these, and the walk below follows the same
    /// alternative it will pick.
    static final Set<String> SUPPORTED_DART_LIBRARIES = new HashSet<String>(java.util.Arrays.asList(
            "core", "async", "collection", "convert", "developer", "io", "math", "typed_data", "ui"));

    private static final Pattern GOOGLE_FONT = Pattern.compile(
            "GoogleFonts\\s*\\.\\s*([a-z][A-Za-z0-9]*)\\s*\\(\\s*(?:['\"]([^'\"]+)['\"])?");

    /// google_fonts' members that are not a family.
    private static final Set<String> GOOGLE_FONTS_API = new java.util.HashSet<String>(java.util.Arrays.asList(
            "getFont", "getTextTheme", "asMap", "pendingFonts", "config"));

    /// Adds the families `source` names through google_fonts: a family method is the
    /// family in lower camel case (`notoSansHebrew` is Noto Sans Hebrew, `baloo2` is
    /// Baloo 2), and its `TextTheme` variant names the same one.
    static void googleFontFamilies(String source, Set<String> out) {
        Matcher m = GOOGLE_FONT.matcher(source);
        while (m.find()) {
            String member = m.group(1);
            if ("getFont".equals(member) || "getTextTheme".equals(member)) {
                if (m.group(2) != null) {
                    out.add(m.group(2));
                }
                continue;
            }
            if (GOOGLE_FONTS_API.contains(member)) {
                continue;
            }
            if (member.endsWith("TextTheme") && member.length() > "TextTheme".length()) {
                member = member.substring(0, member.length() - "TextTheme".length());
            }
            StringBuilder family = new StringBuilder();
            for (int i = 0; i < member.length(); i++) {
                char c = member.charAt(i);
                boolean digit = c >= '0' && c <= '9';
                boolean upper = c >= 'A' && c <= 'Z';
                char prev = i == 0 ? ' ' : member.charAt(i - 1);
                boolean prevDigit = prev >= '0' && prev <= '9';
                if (i > 0 && (upper || (digit && !prevDigit))) {
                    family.append(' ');
                }
                family.append(i == 0 && c >= 'a' && c <= 'z' ? (char) (c - 'a' + 'A') : c);
            }
            out.add(family.toString());
        }
    }

    private static final Pattern DIRECTIVE = Pattern.compile(
            "(?m)^\\s*(import|export|part)\\s+(?!of\\b)(['\"])([^'\"]+)\\2([^;]*);");
    private static final Pattern CONDITIONAL = Pattern.compile(
            "if\\s*\\(\\s*dart\\.library\\.([A-Za-z_]+)\\s*\\)\\s*(['\"])([^'\"]+)\\2");

    /// The packages the last [#importPackageSources] reached, stubbed ones included.
    private final Set<String> lastReachedPackages = new HashSet<String>();

    /// Follows the app's imports from every file in `lib/`, file by file, into its pub
    /// packages. A package the runtime stubs (COVERED) or the SDK provides is not
    /// followed. A pure-Dart package has every file the app reaches copied to
    /// `packages/<name>/` -- where Flutter's `packages/<name>/...` asset keys already
    /// put that package's files -- for the transpiler to compile with the app. A
    /// platform plugin, or a file that needs a `dart:` library with no counterpart, is
    /// recorded in [Result#unsupportedPackages] and not followed further.
    ///
    /// Only reachable files are copied: a package's tests, tools and web-only
    /// libraries never reach the transpiler.
    private int importPackageSources(File libDir, String ownName, Map<String, File> packageConfig,
            Map<String, String> lockVersions, File target, Map<String, String> previous,
            Map<String, String> imported, List<String> packageSources, Result r) throws IOException {
        lastReachedPackages.clear();
        // (package, path inside its lib/) of every file visited; own package is null.
        Set<String> seen = new HashSet<String>();
        java.util.ArrayDeque<String[]> work = new java.util.ArrayDeque<String[]>();
        List<String> own = new ArrayList<String>();
        collectDartSources(libDir, "", own);
        // Seeded from EVERY lib/ file, not just the entry point's import graph: all of
        // lib/ is copied and the transpiler compiles the whole program, so a file nothing
        // imports is still compiled, and the packages it imports must resolve too.
        // Reporting them here names the problem; skipping them would only move it to a
        // compile error in the transpiler.
        for (String f : own) {
            work.add(new String[] {null, f});
            seen.add("\u0000" + f);
        }
        Map<String, File> libDirs = new HashMap<String, File>();
        Map<String, Integer> counts = new TreeMap<String, Integer>();
        Map<String, Set<String>> importers = new HashMap<String, Set<String>>();
        // package -> the app's own lib/ files that import it directly
        Map<String, Set<String>> ownImporters = new HashMap<String, Set<String>>();
        int copied = 0;
        while (!work.isEmpty()) {
            String[] item = work.poll();
            String pkg = item[0];
            String rel = item[1];
            File base = pkg == null ? libDir : libDirs.get(pkg);
            File file = new File(base, rel.replace('/', File.separatorChar));
            if (!file.isFile()) {
                continue;
            }
            String text = new String(Files.readAllBytes(file.toPath()), Charset.forName("UTF-8"));
            if (pkg != null) {
                String dest = "packages/" + pkg + "/" + rel;
                copied += copy(file, new File(target, dest.replace('/', File.separatorChar)), dest, previous,
                        imported);
                packageSources.add(dest);
                Integer c = counts.get(pkg);
                counts.put(pkg, Integer.valueOf(c == null ? 1 : c.intValue() + 1));
            }
            if (pkg == null) {
                googleFontFamilies(stripComments(text), r.googleFonts);
            }
            Matcher m = DIRECTIVE.matcher(stripComments(text));
            while (m.find()) {
                String uri = selectUri(m.group(3), m.group(4));
                if (uri.startsWith("dart:")) {
                    String lib = uri.substring(5);
                    // Anything the transpiler does not implement is unsupported, not only
                    // the libraries listed as such: dart:developer copied through would
                    // only fail later, in the transpiler, instead of in this report.
                    if (pkg != null && !SUPPORTED_DART_LIBRARIES.contains(lib)
                            && !r.unsupportedPackages.containsKey(pkg)) {
                        r.unsupportedPackages.put(pkg, "needs dart:" + lib + ", which has no Codename One "
                                + "counterpart (" + rel + ")");
                    }
                    continue;
                }
                String nextPkg;
                String nextRel;
                if (uri.startsWith("package:")) {
                    int slash = uri.indexOf('/');
                    if (slash < 0) {
                        continue;
                    }
                    nextPkg = uri.substring("package:".length(), slash);
                    nextRel = uri.substring(slash + 1);
                    if (nextPkg.equals(ownName)) {
                        nextPkg = null;
                    }
                } else if (uri.indexOf(':') >= 0) {
                    continue;
                } else {
                    nextPkg = pkg;
                    int cut = rel.lastIndexOf('/');
                    nextRel = normalize((cut < 0 ? "" : rel.substring(0, cut + 1)) + uri);
                }
                if (nextPkg != null) {
                    lastReachedPackages.add(nextPkg);
                    if (pkg == null) {
                        Set<String> files = ownImporters.get(nextPkg);
                        if (files == null) {
                            files = new TreeSet<String>();
                            ownImporters.put(nextPkg, files);
                        }
                        files.add(rel);
                    }
                    if (pkg != null && !pkg.equals(nextPkg)) {
                        Set<String> by = importers.get(nextPkg);
                        if (by == null) {
                            by = new TreeSet<String>();
                            importers.put(nextPkg, by);
                        }
                        by.add(pkg);
                    }
                    if (SDK_PACKAGES.contains(nextPkg) || coverage.containsKey(nextPkg)
                            || r.unsupportedPackages.containsKey(nextPkg)) {
                        continue;
                    }
                    if (!libDirs.containsKey(nextPkg)) {
                        File dir = resolvePackageLibDir(nextPkg, packageConfig, lockVersions);
                        if (dir == null) {
                            if (!r.unresolvedPackages.contains(nextPkg)) {
                                r.unresolvedPackages.add(nextPkg);
                                log.warn("Flutter package '" + nextPkg + "' is imported but not resolvable from "
                                        + ".dart_tool/package_config.json or the pub cache; run `flutter pub get` "
                                        + "once in the Flutter project");
                            }
                            r.unsupportedPackages.put(nextPkg, "sources not found; run `flutter pub get`");
                            continue;
                        }
                        Set<String> platforms = pluginPlatforms(dir);
                        if (isForeignPlatformPlugin(platforms)) {
                            r.foreignPlugins.put(nextPkg, platforms.isEmpty() ? "no platform"
                                    : String.join("/", platforms));
                        } else if (platforms != null) {
                            r.unsupportedPackages.put(nextPkg, "a platform plugin with no Codename One stub");
                            continue;
                        }
                        libDirs.put(nextPkg, dir);
                    }
                }
                if (seen.add((nextPkg == null ? "\u0000" : nextPkg + "\u0000") + nextRel)) {
                    work.add(new String[] {nextPkg, nextRel});
                }
            }
        }
        // A package that imports an unsupported one cannot compile either (its symbols
        // were never imported), so the verdict travels up to every importer -- else the
        // build failed in the importer's sources while this report blamed only the leaf.
        java.util.ArrayDeque<String> verdicts = new java.util.ArrayDeque<String>(r.unsupportedPackages.keySet());
        while (!verdicts.isEmpty()) {
            String dep = verdicts.poll();
            Set<String> by = importers.get(dep);
            if (by == null) {
                continue;
            }
            for (String p : by) {
                if (!r.unsupportedPackages.containsKey(p)) {
                    r.unsupportedPackages.put(p, "depends on " + dep + ", which is unsupported");
                    List<String> chain = new ArrayList<String>();
                    chain.add(p);
                    List<String> rest = r.unsupportedChains.get(dep);
                    chain.addAll(rest == null ? java.util.Collections.singletonList(dep) : rest);
                    r.unsupportedChains.put(p, chain);
                    verdicts.add(p);
                }
            }
        }
        for (String u : r.unsupportedPackages.keySet()) {
            if (!r.unsupportedChains.containsKey(u)) {
                r.unsupportedChains.put(u, java.util.Collections.singletonList(u));
            }
        }
        // The app files behind each package: its own importers, and those of every
        // package that imports it, transitively.
        for (String reached : lastReachedPackages) {
            Set<String> files = new TreeSet<String>();
            Set<String> visited = new HashSet<String>();
            java.util.ArrayDeque<String> up = new java.util.ArrayDeque<String>();
            up.add(reached);
            while (!up.isEmpty()) {
                String q = up.poll();
                if (!visited.add(q)) {
                    continue;
                }
                Set<String> direct = ownImporters.get(q);
                if (direct != null) {
                    files.addAll(direct);
                }
                Set<String> by = importers.get(q);
                if (by != null) {
                    up.addAll(by);
                }
            }
            r.reachedFrom.put(reached, files);
        }
        // A package found unsupported after some of its files were copied is not
        // transpiled: drop what was copied, so the build does not compile half of it.
        for (String bad : r.unsupportedPackages.keySet()) {
            if (counts.remove(bad) != null) {
                String prefix = "packages/" + bad + "/";
                for (java.util.Iterator<String> it = packageSources.iterator(); it.hasNext();) {
                    String ps = it.next();
                    if (ps.startsWith(prefix)) {
                        it.remove();
                        String written = imported.remove(ps);
                        File f = new File(target, ps.replace('/', File.separatorChar));
                        // Only a file that is still exactly the importer's copy goes. One
                        // edited locally (copy() kept it, recording the earlier hash) is
                        // left for removeStaleImports, which keeps it with a warning.
                        if (f.isFile() && (written == null || !written.equals(sha256(f)))) {
                            continue;
                        }
                        if (f.isFile() && !f.delete()) {
                            log.warn("Could not remove " + f);
                        }
                    }
                }
            }
        }
        r.transpiledPackages.putAll(counts);
        return copied;
    }

    /// The uri a configurable import names here: the first `if (dart.library.x)`
    /// alternative whose library the transpiler implements, else the default.
    static String selectUri(String defaultUri, String conditions) {
        Matcher c = CONDITIONAL.matcher(conditions);
        while (c.find()) {
            if (SUPPORTED_DART_LIBRARIES.contains(c.group(1))) {
                return c.group(3);
            }
        }
        return defaultUri;
    }

    /// Whether a package's pubspec declares `flutter: plugin:` -- a federated or
    /// classic platform plugin, whose Dart side calls into native code.
    static boolean isPlatformPlugin(File packageLibDir) {
        return pluginPlatforms(packageLibDir) != null;
    }

    /// The platforms a plugin's pubspec lists under `flutter: plugin: platforms:`,
    /// null for a package that is not a plugin. A plugin in the format that predates
    /// `platforms:` (`androidPackage`/`pluginClass` directly under `plugin:`) served
    /// Android and iOS, and answers those two.
    static Set<String> pluginPlatforms(File packageLibDir) {
        File pubspec = new File(packageLibDir.getParentFile(), "pubspec.yaml");
        if (!pubspec.isFile()) {
            return null;
        }
        try {
            boolean inFlutter = false;
            int pluginIndent = -1;
            int platformsIndent = -1;
            int keyIndent = -1;
            Set<String> platforms = null;
            for (String line : Files.readAllLines(pubspec.toPath(), Charset.forName("UTF-8"))) {
                String t = line.trim();
                if (t.isEmpty() || t.startsWith("#")) {
                    continue;
                }
                int indent = line.length() - line.replaceAll("^\\s+", "").length();
                if (indent == 0) {
                    if (pluginIndent >= 0) {
                        break;
                    }
                    inFlutter = line.startsWith("flutter:");
                    continue;
                }
                if (!inFlutter) {
                    continue;
                }
                if (pluginIndent < 0) {
                    if (t.startsWith("plugin:")) {
                        pluginIndent = indent;
                    }
                    continue;
                }
                if (indent <= pluginIndent) {
                    break;
                }
                if (platformsIndent < 0) {
                    if (t.startsWith("platforms:")) {
                        platformsIndent = indent;
                        platforms = new TreeSet<String>();
                    }
                    continue;
                }
                if (indent <= platformsIndent) {
                    break;
                }
                if (keyIndent < 0) {
                    keyIndent = indent;
                }
                int colon = t.indexOf(':');
                if (indent == keyIndent && colon > 0) {
                    platforms.add(t.substring(0, colon).trim());
                }
            }
            if (pluginIndent < 0) {
                return null;
            }
            if (platforms == null) {
                return new TreeSet<String>(java.util.Arrays.asList("android", "ios"));
            }
            return platforms;
        } catch (IOException e) {
            return null;
        }
    }

    /// A plugin with no Android or iOS implementation: what it does natively happens only
    /// on a desktop or the web, platforms Codename One does not build for. A mobile Flutter
    /// app reaches one only behind a platform check (`firebase_ui_oauth` depends on
    /// `desktop_webview_auth` for its desktop sign-in), so the plugin's Dart is
    /// transpiled like any pure-Dart package and its method-channel calls throw
    /// MissingPluginException, as they do on a platform the plugin does not implement.
    static boolean isForeignPlatformPlugin(Set<String> platforms) {
        return platforms != null && !platforms.contains("android") && !platforms.contains("ios");
    }

    /// Removes `//` and `/* */` comments, keeping string literals intact enough for
    /// directive matching (a directive is never inside a string). A triple-quoted
    /// string's contents are blanked, newlines kept: it spans lines, and a line of it
    /// that reads like `import 'package:x/x.dart';` is text, not a dependency. A raw
    /// string (`r'...'`) has no escapes.
    static String stripComments(String s) {
        StringBuilder out = new StringBuilder(s.length());
        int i = 0;
        int n = s.length();
        char quote = 0;
        boolean rawQuote = false;
        while (i < n) {
            char ch = s.charAt(i);
            if (quote == 0 && (ch == '\'' || ch == '"') && i + 2 < n
                    && s.charAt(i + 1) == ch && s.charAt(i + 2) == ch) {
                boolean raw = i > 0 && (s.charAt(i - 1) == 'r' || s.charAt(i - 1) == 'R');
                String close = new String(new char[] {ch, ch, ch});
                out.append(close);
                i += 3;
                while (i < n && !s.startsWith(close, i)) {
                    char c = s.charAt(i);
                    if (c == '\\' && !raw && i + 1 < n) {
                        out.append(s.charAt(i + 1) == '\n' ? "\n " : "  ");
                        i += 2;
                        continue;
                    }
                    out.append(c == '\n' ? '\n' : ' ');
                    i++;
                }
                if (i < n) {
                    out.append(close);
                    i += 3;
                }
                continue;
            }
            if (quote != 0) {
                out.append(ch);
                if (ch == '\\' && i + 1 < n && !rawQuote) {
                    out.append(s.charAt(i + 1));
                    i += 2;
                    continue;
                }
                if (ch == quote || ch == '\n') {
                    quote = 0;
                }
                i++;
                continue;
            }
            if (ch == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
                while (i < n && s.charAt(i) != '\n') {
                    i++;
                }
                continue;
            }
            if (ch == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                int depth = 0;
                while (i < n) {
                    if (s.startsWith("/*", i)) {
                        depth++;
                        i += 2;
                    } else if (s.startsWith("*/", i)) {
                        depth--;
                        i += 2;
                        if (depth == 0) {
                            break;
                        }
                    } else {
                        if (s.charAt(i) == '\n') {
                            out.append('\n');
                        }
                        i++;
                    }
                }
                continue;
            }
            if (ch == '\'' || ch == '"') {
                quote = ch;
                rawQuote = i > 0 && (s.charAt(i - 1) == 'r' || s.charAt(i - 1) == 'R');
            }
            out.append(ch);
            i++;
        }
        return out.toString();
    }

    private static String normalize(String path) {
        List<String> out = new ArrayList<String>();
        for (String p : path.split("/")) {
            if (p.isEmpty() || p.equals(".")) {
                continue;
            }
            if (p.equals("..")) {
                if (!out.isEmpty()) {
                    out.remove(out.size() - 1);
                }
            } else {
                out.add(p);
            }
        }
        StringBuilder b = new StringBuilder();
        for (String p : out) {
            if (b.length() > 0) {
                b.append('/');
            }
            b.append(p);
        }
        return b.toString();
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
    /// Must match com.codename1.flutter.plugins.packageinfo.PackageInfo.
    static final String BUILD_NUMBER_RESOURCE = "cn1-flutter-build-number.txt";

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
                // No device property reports the native build number, so package_info_plus
                // reads it from here (PackageInfo.BUILD_NUMBER_RESOURCE).
                File res = new File(commonDir, "src/main/resources/" + BUILD_NUMBER_RESOURCE);
                res.getParentFile().mkdirs();
                Files.write(res.toPath(), buildNumber.getBytes(Charset.forName("UTF-8")));
            } else {
                // A reimport after the pubspec dropped its +build part: the resource an
                // earlier import wrote would keep reporting the old build number, and the
                // native hints it appended would keep building with it. A hint whose value
                // is not the one this importer wrote was set by hand and is kept.
                File res = new File(commonDir, "src/main/resources/" + BUILD_NUMBER_RESOURCE);
                if (res.isFile()) {
                    String old = new String(Files.readAllBytes(res.toPath()), Charset.forName("UTF-8")).trim();
                    updated = removePropertyIfValue(updated, "codename1.arg.android.versionCode", old);
                    updated = removePropertyIfValue(updated, "codename1.arg.ios.bundleVersion", old);
                    Files.delete(res.toPath());
                }
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

    /// Removes the `key=value` line whose value is exactly `value`; any other
    /// value (or no line) leaves the file untouched.
    static String removePropertyIfValue(String properties, String key, String value) {
        return properties.replaceAll("(?m)^" + Pattern.quote(key) + "\\s*=\\s*" + Pattern.quote(value)
                + "[ \\t]*(\\r?\\n|$)", "");
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

    private void writeEntryPoint(File javaDir, String pkg, String cls, String flutterPackage,
            java.util.Collection<String> dependencies) throws IOException {
        File f = new File(javaDir, pkg.replace('.', File.separatorChar) + File.separator + cls + ".java");
        f.getParentFile().mkdirs();
        String src = entryPointSource(pkg, cls, flutterPackage, dependencies);
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

    /// The entry point's source. The plugin packs that need the platform to call the
    /// app -- a deep link (app_links), a notification tap (flutter_local_notifications), a
    /// push (firebase_messaging) -- get their callbacks forwarded here, and only when the
    /// app depends on them: implementing PushCallback is what turns push on in a build.
    static String entryPointSource(String pkg, String cls, String flutterPackage,
            java.util.Collection<String> dependencies) {
        boolean links = dependencies.contains("app_links");
        boolean notifications = dependencies.contains("flutter_local_notifications");
        boolean push = dependencies.contains("firebase_messaging");
        StringBuilder implementsList = new StringBuilder();
        if (notifications) {
            implementsList.append("com.codename1.notifications.LocalNotificationCallback");
        }
        if (push) {
            implementsList.append(implementsList.length() > 0 ? ", " : "").append("com.codename1.push.PushCallback");
        }
        StringBuilder b = new StringBuilder();
        b.append("package ").append(pkg).append(";\n\n")
                .append("/// The Codename One entry point: starts the imported Flutter application's Dart `main()`.\n")
                .append("public class ").append(cls).append(" extends com.codename1.system.Lifecycle");
        if (implementsList.length() > 0) {
            b.append("\n        implements ").append(implementsList);
        }
        b.append(" {\n");
        if (links) {
            b.append("    @Override\n")
                    .append("    public void start() {\n")
                    .append("        com.codename1.flutter.plugins.applinks.AppLinks.appStarted();\n")
                    .append("        super.start();\n")
                    .append("    }\n\n");
        }
        b.append("    @Override\n")
                .append("    public void runApp() {\n");
        if (push) {
            // the runtime reads the key through this, so only an app using push names Push
            b.append("        com.codename1.flutter.plugins.firebase.FirebaseMessaging.tokenSource(\n")
                    .append("                new dart.runtime.Funcs.Func0<String>() {\n")
                    .append("                    @Override\n")
                    .append("                    public String call() {\n")
                    .append("                        return com.codename1.push.Push.getPushKey();\n")
                    .append("                    }\n")
                    .append("                });\n");
            b.append("        com.codename1.flutter.plugins.firebase.FirebaseMessaging.topicSink(\n")
                    .append("                new dart.runtime.Funcs.VoidFunc2<String, Boolean>() {\n")
                    .append("                    @Override\n")
                    .append("                    public void call(String topic, Boolean subscribe) {\n")
                    .append("                        if (subscribe.booleanValue()) {\n")
                    .append("                            com.codename1.push.Push.subscribeToTopic(topic);\n")
                    .append("                        } else {\n")
                    .append("                            com.codename1.push.Push.unsubscribeFromTopic(topic);\n")
                    .append("                        }\n")
                    .append("                    }\n")
                    .append("                });\n");
        }
        b.append("        ").append(flutterPackage).append(".FlutterRegistry.invokeMain();\n")
                .append("    }\n");
        if (notifications) {
            b.append("\n    @Override\n")
                    .append("    public void localNotificationReceived(String notificationId) {\n")
                    .append("        com.codename1.flutter.plugins.notifications.FlutterLocalNotificationsPlugin")
                    .append(".notificationTapped(notificationId);\n")
                    .append("    }\n");
        }
        if (push) {
            b.append("\n    @Override\n")
                    .append("    public void push(String value) {\n")
                    .append("        com.codename1.flutter.plugins.firebase.FirebaseMessaging.pushReceived(value);\n")
                    .append("    }\n\n")
                    .append("    @Override\n")
                    .append("    public void registeredForPush(String deviceId) {\n")
                    .append("        com.codename1.flutter.plugins.firebase.FirebaseMessaging.tokenRegistered(deviceId);\n")
                    .append("    }\n\n")
                    .append("    @Override\n")
                    .append("    public void pushRegistrationError(String error, int errorCode) {\n")
                    .append("        com.codename1.io.Log.p(\"Push registration failed: \" + error);\n")
                    .append("        com.codename1.flutter.plugins.firebase.FirebaseMessaging.registrationFailed(error);\n")
                    .append("    }\n");
        }
        return b.append("}\n").toString();
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

    /// The `.dart` files copied from `lib/`, relative to `src/main/flutter`. lib/ is
    /// copied flat, so `lib/assets/generated.dart` lands in `assets/` beside the
    /// project's bundled assets; this record is how the transpiler still parses it
    /// as source there. Read by `com.codename1.dart.transpiler.api.DartTranspiler`.
    static final String LIB_SOURCES_RECORD = ".flutter-lib-sources";

    private static void writeLibSources(File target, File libDir, List<String> packageSources) throws IOException {
        List<String> sources = new ArrayList<String>();
        collectDartSources(libDir, "", sources);
        sources.addAll(packageSources);
        java.util.Collections.sort(sources);
        StringBuilder b = new StringBuilder();
        for (String s : sources) {
            b.append(s).append('\n');
        }
        target.mkdirs();
        Files.write(new File(target, LIB_SOURCES_RECORD).toPath(), b.toString().getBytes(Charset.forName("UTF-8")));
    }

    private static void collectDartSources(File dir, String prefix, List<String> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File f : children) {
            if (f.isDirectory()) {
                collectDartSources(f, prefix + f.getName() + "/", out);
            } else if (f.getName().endsWith(".dart")) {
                out.add(prefix + f.getName());
            }
        }
    }

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
        if (sourcePaths != null && sourcePaths.contains(path)) {
            if (!collisions.contains(path)) {
                collisions.add(path);
                log.warn("Not imported: the asset " + path + " has the same path as a file copied from lib/ "
                        + "(lib/ is copied flat into the same directory); the Dart source was kept");
            }
            return 0;
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

    /// Whether `path` climbs out of its root: a `..` SEGMENT. A substring test also
    /// rejected `assets/logo..png` and `images/v1..2/icon.png`, valid names that were
    /// then neither copied nor reported missing.
    static boolean hasParentSegment(String path) {
        for (String segment : path.split("[/\\\\]")) {
            if (segment.equals("..")) {
                return true;
            }
        }
        return false;
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
            if (imported.containsKey(path) || hasParentSegment(path)) {
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
