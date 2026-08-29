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
package com.codename1.builders;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Builds a native macOS (AppKit) application.
 *
 * <p>This is a peer of {@link IPhoneBuilder} rather than a delegate of it. Mac
 * Catalyst is a slice of the iOS Xcode project and is built by handing
 * {@code IPhoneBuilder} one extra hint; the AppKit port is a different SDK, a
 * different UI framework and a different project shape, and it wants none of the
 * iOS project geometry -- no app extensions, no {@code SUPPORTS_MACCATALYST}, no
 * multi-scene manifest, no Ruby {@code xcodeproj} overlay.</p>
 *
 * <p>What it does share is the Apple toolchain: the translator, xcodebuild,
 * codesign and notarization. Those live on {@link Executor} and are used here
 * directly.</p>
 *
 * <p>The project itself is emitted by the translator's {@code macos} output
 * type, which is the same pipeline as iOS parameterised by four things -- the
 * pbxproj template, the Info.plist, the framework list and the asset catalog.
 * The plists and entitlements this builder writes over the top come from
 * {@link MacOSXcodeProject}, which is deliberately free of any Xcode dependency
 * so the generation is unit-testable on a machine with no developer tools.</p>
 *
 * @author Shai Almog
 */
public class MacOSNativeBuilder extends Executor {

    private File resultDir;
    private File appBundle;
    private File xcodeProjectDir;
    private final List<File> artifacts = new ArrayList<File>();
    /// The scan result and the dlopen flag, kept because the entitlements are
    /// written once per signing channel while the scan is worth doing once.
    /// Whether the class scan found a local-calendar entry point, carried from
    /// the native-feature scan to the permission generation that runs later.
    private boolean calendarDetected;
    /// Bluetooth, carried the same way and for the same reason. The
    /// native-feature scan sees Display.getBluetooth(), which names
    /// com.codename1.bluetooth only in its RETURN type; the capability scan
    /// reads the invoked owner and cannot. An application that reaches
    /// Bluetooth only through Display therefore linked CoreBluetooth and
    /// shipped without the entitlement or the usage description, and was
    /// denied at first use in any sandboxed or hardened build.
    private boolean bluetoothDetected;

    /// Whether the application registers for push, taken from the
    /// native-feature scan rather than the capability scan -- one detection,
    /// one result, the way calendarDetected works. It has to be known EARLY:
    /// it decides whether CN1_INCLUDE_NOTIFICATIONS2 is enabled while the
    /// native sources are still being staged, long before the capability scan.
    private boolean pushDetected;

    private MacOSXcodeProject.MacOSCapabilities capabilities;
    private boolean loadsExternalCode;

    /** The produced {@code .app} bundle, or {@code null} if the build failed. */
    public File getAppBundle() {
        return appBundle;
    }

    /**
     * Everything the build produced, in the order it was produced: each channel's
     * {@code .app} followed by the dmg and/or pkg built from it.
     */
    public List<File> getArtifacts() {
        return artifacts;
    }

    /** The directory holding the build output. */
    public File getResultDir() {
        return resultDir;
    }

    /**
     * The directory holding the generated Xcode project, for the
     * {@code mac-source} target which stops after generating it.
     *
     * <p>The directory rather than the {@code .xcodeproj} bundle: the sources
     * the project references sit beside it, so handing back the bundle alone
     * gives a project that opens and cannot build.</p>
     */
    public File getXcodeProjectDir() {
        return xcodeProjectDir;
    }

    @Override
    protected String getDeviceIdCode() {
        return "\"\"";
    }

    @Override
    protected String hardeningPlatform(BuildRequest request) {
        return "mac";
    }

    // The generated XxxImplCodenameOne carries the native methods; a
    // PeerComponent-returning native interface method is bridged through the
    // native view pointer, exactly as the iOS builder does, because the peer
    // machinery on this port is the shared Apple one.
    // Same suffix the iOS builder uses, so a project's Objective-C native
    // interface implementation is found under the same name on both.
    @Override
    protected String getImplSuffix() {
        return "ImplCodenameOne";
    }

    @Override
    protected String nativeInterfaceFrameworkImports() {
        return "#import \"CodenameOne_GLViewController.h\"\n"
                + "#import <AppKit/AppKit.h>\n";
    }

    @Override
    protected String generatePeerComponentCreationCode(String methodCallString) {
        return "PeerComponent.create(new long[] {" + methodCallString + "})";
    }

    @Override
    protected String convertPeerComponentToNative(String param) {
        return "((long[])" + param + ".getNativePeer())[0]";
    }

    @Override
    public boolean build(File sourceZip, BuildRequest request) throws BuildException {
        if (!System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            throw new BuildException("A native macOS application can only be built on a Mac: "
                    + "the build runs xcodebuild against the macosx SDK.");
        }

        final MacOSBuildHints hints = new MacOSBuildHints();
        hints.parse(new MacOSBuildHints.HintSource() {
            @Override
            public String get(String key, String defaultValue) {
                return request.getArg(key, defaultValue);
            }
        }, request.getPackageName());
        for (String warning : hints.getWarnings()) {
            log(warning);
        }

        File tmpFile = getBuildDirectory();
        tmpFile.mkdirs();
        File classesDir = new File(tmpFile, "classes");
        File resDir = new File(tmpFile, "res");
        File buildinRes = new File(tmpFile, "btres");
        // Emptied, not merely created. These paths are stable across builds and
        // unzip() only overwrites the entries the archive carries, so on a
        // rebuild without `mvn clean` a class or resource deleted since the last
        // build stays here -- and is then scanned for permissions and crypto
        // usage, and translated into the app. Removed code that keeps shipping,
        // and an entitlement the application no longer justifies, are both worse
        // than a slower build.
        try {
            deleteRecursively(classesDir);
            deleteRecursively(resDir);
            deleteRecursively(buildinRes);
        } catch (IOException ex) {
            throw new BuildException("Failed to clear the staged build inputs", ex);
        }
        classesDir.mkdirs();
        resDir.mkdirs();
        buildinRes.mkdirs();

        try {
            unzip(sourceZip, classesDir, resDir, resDir, buildinRes);
        } catch (Exception ex) {
            throw new BuildException("Failed to unzip the application sources", ex);
        }

        // The JDK class set the translator emits as java_lang_*.m/.h. Shipped
        // separately and unzipped into a translator source root; without it
        // cn1_globals.c references java_lang_Class.h and no such header is
        // produced. Same wire-up as every other ParparVM target.
        try {
            unzip(getResourceAsStream("/parparvm-java-api.jar"), classesDir, classesDir, classesDir);
        } catch (Exception ex) {
            throw new BuildException("Failed to load JavaAPI.jar", ex);
        }

        File parparVMCompilerJar;
        File portClasses;
        File nativeSources;
        try {
            parparVMCompilerJar = getResourceAsFile("/parparvm-compiler.jar", ".jar");
            File portDir = new File(tmpFile, "macPort");
            portClasses = new File(portDir, "classes");
            nativeSources = new File(portDir, "nativeSources");
            // Emptied, not merely created. extractJarResource overwrites the
            // entries the bundle carries and removes nothing, so on a rebuild in
            // the same directory a class or native source dropped from
            // MacPort.jar or nativemac.jar stayed in the translator's input --
            // still compiled, and a duplicate symbol once a replacement lands
            // under a different name.
            deleteRecursively(portDir);
            portClasses.mkdirs();
            nativeSources.mkdirs();
            // Provided by the codenameone-mac 'bundle' artifact on the plugin
            // classpath. nativemac.jar is the materialised native set -- the
            // MacPort sources plus the shared iOSPort ones the exclusion
            // manifest keeps -- so what is staged here is exactly what clang
            // sees, and exactly what the offline signature gate verifies.
            extractJarResource("/MacPort.jar", portClasses);
            extractJarResource("/nativemac.jar", nativeSources);
            // The themes out of that jar are APPLICATION resources, not native
            // sources, and have to be where getResourceAsStream() can find them
            // at run time. The iOS builder gets this for free -- it unzips
            // nativeios.jar with buildinRes as the resource destination -- while
            // this one stages the same jar for clang alone.
            //
            // The failure was silent and expensive to find. installNativeTheme()
            // asks for /iOSModernTheme.res, gets null, and falls through to
            // iOS7Theme.res without a word; iOS7Theme declares no
            // @darkModeBool, which is the constant UIManager gates the entire
            // dark palette on, so every dark-appearance screen rendered light
            // however carefully the application asked for dark. Nothing failed
            // to build and nothing was logged.
            stageThemeResources(nativeSources, buildinRes);
        } catch (Exception ex) {
            throw new BuildException("Failed to stage the MacPort native layer. The codenameone "
                    + "maven plugin must provide the codenameone-mac 'bundle' artifact "
                    + "(MacPort.jar + nativemac.jar) on its classpath.", ex);
        }

        DatabaseUsage databaseUsage;
        try {
            databaseUsage = scanForDatabaseUsage(classesDir).merge(scanForDatabaseUsage(buildinRes));
        } catch (IOException ex) {
            throw new BuildException("Failed to scan for database usage", ex);
        }

        // The crypto primitives are compiled in only for an application that
        // references them, as on iOS -- an application that does not never
        // references a CommonCrypto or Security symbol.
        //
        // This is not an optimisation to skip. The stubs the toggle leaves in
        // place return unsupported for the ciphers, but secureRandomBytes just
        // leaves the caller's buffer alone: two calls then agree, because both
        // read back the zeroes that were already there. A random source that
        // silently returns a constant is the worst possible failure, and it is
        // what an unconfigured build ships.
        final boolean[] usesCrypto = {false};
        final boolean[] usesLocalNotifications = {false};
        final boolean[] usesMicrophone = {false};
        final boolean[] usesBluetooth = {false};
        final boolean[] usesCalendar = {false};
        final boolean[] usesPush = {false};
        final boolean[] usesLocation = {false};
        final boolean[] usesAppReview = {false};
        try {
            scanClassesForPermissions(classesDir, new NativeFeatureScanner(usesCrypto,
                    usesLocalNotifications, usesMicrophone, usesBluetooth, usesCalendar, usesPush,
                    usesLocation, usesAppReview));
            // btres too, for the reason the capability scan reads it: unzip routes a
            // submitted cn1lib's jar there rather than unpacking it beside the loose
            // classes. A library that is the only thing calling SecureRandom left
            // CN1_INCLUDE_CRYPTO off, and the stub bridge then returns the caller's
            // buffer untouched -- commonly all zeroes. A random source that silently
            // returns a constant is the worst failure in this file.
            scanClassesForPermissions(buildinRes, new NativeFeatureScanner(usesCrypto,
                    usesLocalNotifications, usesMicrophone, usesBluetooth, usesCalendar, usesPush,
                    usesLocation, usesAppReview));
        } catch (IOException ex) {
            throw new BuildException("Failed to scan the application for native feature usage", ex);
        }
        if (usesCrypto[0]) {
            // In the staged native tree, not in the resources: that tree is what
            // the translator copies into the Xcode project, and it is the copy
            // clang reads. The iOS builder edits its resources directory because
            // that is where it unzips the port's natives; here they are their own
            // translator source root.
            File cn1Crypto = new File(nativeSources, "CN1Crypto.h");
            if (!cn1Crypto.exists()) {
                // Not skipped quietly. The stub secureRandomBytes leaves the
                // caller's buffer untouched, so the failure is a random source
                // that returns a constant -- which nothing downstream can detect.
                throw new BuildException("The application uses com.codename1.security but "
                        + "CN1Crypto.h is missing from the staged native sources at "
                        + cn1Crypto.getAbsolutePath());
            }
            try {
                // CN1_INCLUDE_CRYPTO is a strict PREFIX of CN1_INCLUDE_CRYPTO_GCM
                // and they sit on consecutive lines, while replaceInFile is an
                // unrestricted String.replace -- so enabling the base switch also
                // uncommented the GCM line, and macos.crypto.gcm was ignored for
                // any application that used crypto at all. The conditional below
                // it then had nothing left to match, which is why the opt-in
                // looked wired up.
                //
                // The GCM directive is parked under a placeholder first so the
                // base replacement cannot see it, then restored to what the hint
                // actually asked for. Parked rather than anchored to a newline
                // because that anchor would quietly depend on the file staying LF.
                boolean wantsGcm = "true".equalsIgnoreCase(hints.getCryptoGcm());
                replaceInFile(cn1Crypto, "//#define CN1_INCLUDE_CRYPTO_GCM",
                        "//@CN1_CRYPTO_GCM_PLACEHOLDER@");
                replaceInFile(cn1Crypto, "//#define CN1_INCLUDE_CRYPTO",
                        "#define CN1_INCLUDE_CRYPTO");
                replaceInFile(cn1Crypto, "//@CN1_CRYPTO_GCM_PLACEHOLDER@",
                        wantsGcm ? "#define CN1_INCLUDE_CRYPTO_GCM"
                                 : "//#define CN1_INCLUDE_CRYPTO_GCM");
            } catch (Exception ex) {
                throw new BuildException("Failed to configure CN1Crypto.h", ex);
            }
        }
        log("Crypto API " + (usesCrypto[0] ? "enabled" : "disabled"));

        // UserNotifications is the same framework on macOS, so the only thing
        // standing between an app and a working LocalNotification is this
        // define -- and with it off every sendLocalNotification body compiles
        // away while requestNotificationPermission still reports granted. The
        // app schedules notifications that never arrive and has no way to tell.
        //
        // The app delegate's CN1_INCLUDE_NOTIFICATIONS is deliberately NOT set:
        // that one lives in CodenameOne_GLAppDelegate.h, which this port
        // excludes. CN1MacAppDelegate handles delivery instead.
        // Push as well as local notifications, which is what IPhoneBuilder does:
        // it enables this define for a push certificate, ios.includePush OR
        // local notifications. Gating on local notifications alone gave a
        // push-only application a bundle carrying the APNs entitlement while
        // the UserNotifications import that registerPush depends on was left
        // to clang's implicit module auto-import -- which the comment beside
        // that import in IOSNative.m already records as the fragile
        // arrangement it exists to replace.
        // The override counts as well as the scan. macos.entitlements.apsEnvironment
        // turns push on for an application whose registration the scanner cannot
        // see -- a reflective call, most obviously -- and it is the same hint that
        // writes the APNs entitlement. Reading only the scan flags here shipped a
        // bundle advertising APNs with the push natives compiled out of it, which
        // is the precise mismatch this define exists to prevent. Any channel that
        // resolves to push is enough: the define is written once for the build,
        // while the entitlements are written per channel.
        // Every capability that gates a native define is resolved the same way:
        // the class scan OR an explicit entitlement override. The override exists
        // for exactly the application the scanner cannot read -- a reflective
        // call, most obviously -- and it is the same hint that writes the
        // permission into the bundle. Deciding the define from the scan alone
        // therefore ships a bundle asking for a permission whose implementation
        // was compiled out of it, which is the one mismatch these defines exist
        // to prevent. Any signing channel is enough: the defines are written once
        // per build, the entitlements once per channel.
        java.util.List<MacOSBuildHints.EntitlementOverrides> channelOverrides =
                new java.util.ArrayList<MacOSBuildHints.EntitlementOverrides>();
        for (String channel : hints.getChannels()) {
            channelOverrides.add(hints.entitlementsFor(channel));
        }
        boolean pushEnabled = usesPush[0];
        boolean microphoneEnabled = usesMicrophone[0];
        boolean bluetoothEnabled = usesBluetooth[0];
        boolean calendarEnabled = usesCalendar[0];
        boolean locationEnabled = usesLocation[0];
        boolean appReviewEnabled = usesAppReview[0];
        // Unconditional, for the reason the capability scan above explains at
        // length: com/codename1/camera/Camera#open is invoked by the framework's
        // own Camera class, so no scan can tell an application using the low
        // level API from the framework merely being present. The bridge is
        // therefore compiled into every macOS binary -- CN1Camera.m plus the
        // AVFoundation link -- rather than gated on a test that always answers
        // yes. The PERMISSION is not: that stays on the Capture/Display signal,
        // so an application that never opens a camera still does not ask for
        // one.
        boolean cn1CameraEnabled = true;
        for (MacOSBuildHints.EntitlementOverrides o : channelOverrides) {
            pushEnabled = pushEnabled || o.push(false);
            microphoneEnabled = microphoneEnabled || o.microphone(false);
            bluetoothEnabled = bluetoothEnabled || o.bluetooth(false);
            calendarEnabled = calendarEnabled || o.calendars(false);
            locationEnabled = locationEnabled || o.location(false);
        }
        if (usesLocalNotifications[0] || pushEnabled) {
            File iosNative = new File(nativeSources, "IOSNative.m");
            if (!iosNative.exists()) {
                throw new BuildException("The application uses com.codename1.notifications but "
                        + "IOSNative.m is missing from the staged native sources at "
                        + iosNative.getAbsolutePath());
            }
            try {
                replaceInFile(iosNative, "//#define CN1_INCLUDE_NOTIFICATIONS2",
                        "#define CN1_INCLUDE_NOTIFICATIONS2");
            } catch (Exception ex) {
                throw new BuildException("Failed to enable local notifications in IOSNative.m", ex);
            }
        }
        log("Local Notifications " + (usesLocalNotifications[0] ? "enabled" : "disabled"));

        // AVAudioRecorder is the same API on macOS and the recorder natives carry
        // no UIKit, so the only thing between an application and a working
        // recording is this define. With it off, checkMicrophoneUsage() answers
        // false and the inherited implementation throws the iOS build-hint
        // exception before recording starts, while the recorder bodies are
        // compiled away -- so the entitlement and the usage description were
        // written for a feature that could not run.
        //
        // Location is wired up the same way, and for the same reason: Core
        // Location is one framework across both platforms and the natives behind
        // it carry no UIKit, so the define is the only thing between an
        // application and a working fix. The delegate callbacks that deliver the
        // fix are compiled here too -- they sit outside the !TARGET_OS_OSX
        // region of CodenameOne_GLViewController.m, which an earlier version of
        // this comment had wrong.
        //
        // Camera is wired through INCLUDE_CN1_CAMERA, the AVFoundation bridge,
        // rather than INCLUDE_CAMERA_USAGE. The latter compiles the UIKit modal
        // capture UI, which macOS has no class for; the former is CN1Camera.m,
        // which is AVFoundation on both platforms. MacImplementation builds the
        // modal Capture API on top of that bridge in Java, so an application
        // using either com.codename1.camera or com.codename1.capture gets a
        // working camera here.
        if (cn1CameraEnabled) {
            File controllerHeader = new File(nativeSources, "CodenameOne_GLViewController.h");
            if (!controllerHeader.exists()) {
                throw new BuildException("The application uses the camera but "
                        + "CodenameOne_GLViewController.h is missing from the staged native "
                        + "sources at " + controllerHeader.getAbsolutePath());
            }
            try {
                replaceInFile(controllerHeader, "//#define INCLUDE_CN1_CAMERA",
                        "#define INCLUDE_CN1_CAMERA");
            } catch (Exception ex) {
                throw new BuildException("Failed to enable the camera in CodenameOne_GLViewController.h", ex);
            }
        }
        log("Camera " + (cn1CameraEnabled ? "enabled" : "disabled"));

        if (appReviewEnabled) {
            File controllerHeader = new File(nativeSources, "CodenameOne_GLViewController.h");
            if (!controllerHeader.exists()) {
                throw new BuildException("The application asks for an app store review but "
                        + "CodenameOne_GLViewController.h is missing from the staged native "
                        + "sources at " + controllerHeader.getAbsolutePath());
            }
            try {
                replaceInFile(controllerHeader, "//#define CN1_USE_APPREVIEW",
                        "#define CN1_USE_APPREVIEW");
            } catch (Exception ex) {
                throw new BuildException("Failed to enable the app review prompt in "
                        + "CodenameOne_GLViewController.h", ex);
            }
        }
        log("App review " + (appReviewEnabled ? "enabled" : "disabled"));

        if (locationEnabled) {
            File controllerHeader = new File(nativeSources, "CodenameOne_GLViewController.h");
            if (!controllerHeader.exists()) {
                throw new BuildException("The application uses location but "
                        + "CodenameOne_GLViewController.h is missing from the staged native "
                        + "sources at " + controllerHeader.getAbsolutePath());
            }
            try {
                replaceInFile(controllerHeader, "//#define INCLUDE_LOCATION_USAGE",
                        "#define INCLUDE_LOCATION_USAGE");
            } catch (Exception ex) {
                throw new BuildException("Failed to enable location in CodenameOne_GLViewController.h", ex);
            }
        }
        log("Location " + (locationEnabled ? "enabled" : "disabled"));

        if (microphoneEnabled) {
            File controllerHeader = new File(nativeSources, "CodenameOne_GLViewController.h");
            if (!controllerHeader.exists()) {
                throw new BuildException("The application records audio but "
                        + "CodenameOne_GLViewController.h is missing from the staged native "
                        + "sources at " + controllerHeader.getAbsolutePath());
            }
            try {
                replaceInFile(controllerHeader, "//#define INCLUDE_MICROPHONE_USAGE",
                        "#define INCLUDE_MICROPHONE_USAGE");
            } catch (Exception ex) {
                throw new BuildException("Failed to enable the microphone backend in "
                        + "CodenameOne_GLViewController.h", ex);
            }
        }
        log("Microphone " + (microphoneEnabled ? "enabled" : "disabled"));

        // CoreBluetooth and EventKit are the same frameworks on macOS, and their
        // natives carry no UIKit -- but each is behind a define AND a framework,
        // and neither was being set. CN1Bluetooth.m compiled its stub branch, so
        // every capability check answered false and every operation was inert;
        // calendarSupported() answered false the same way. Both features were
        // documented as working and were not present in any build.
        //
        // The frameworks travel in the translator's addLibs argument, which this
        // target was passing as "none".
        List<String> extraFrameworks = new ArrayList<String>();
        // Whatever the project asked for, before anything detected is appended.
        // A migrated project carries the iOS spelling, because the target name
        // did not change under it, and a submitted .m that needs a framework
        // still compiles without one and then fails at the link.
        String configuredLibs = hints.getAddLibs();
        if (configuredLibs != null) {
            // Semicolon, comma OR colon. IPhoneBuilder normalises all three
            // before it splits, so those are the separators a project has been
            // free to use -- and a migrated one carries whichever it wrote.
            // Splitting on semicolons alone handed the translator
            // "Foo.framework,Bar.framework" as one framework name, which links
            // neither. An empty token, from a leading or doubled separator, is
            // dropped by the length check rather than by a special case.
            for (String lib : configuredLibs.split("[;,:]")) {
                String trimmed = lib.trim();
                if (trimmed.length() > 0 && !extraFrameworks.contains(trimmed)) {
                    extraFrameworks.add(trimmed);
                }
            }
        }
        // Unconditional, because the LAContext code in IOSNative.m is compiled
        // on every macOS build -- it is excluded only on tvOS -- so there is no
        // usage to detect.
        //
        // Raised in review as a link failure for biometric builds, and it is
        // not one today: otool -L on the built binary shows
        // LocalAuthentication already linked, because clang auto-links a
        // framework whose module is imported. Named here anyway, so the link
        // does not depend on modules and auto-linking staying enabled -- which
        // is a build setting, not a property of this code.
        extraFrameworks.add("LocalAuthentication.framework");
        if (bluetoothEnabled) {
            File controllerHeader = new File(nativeSources, "CodenameOne_GLViewController.h");
            if (!controllerHeader.exists()) {
                throw new BuildException("The application uses com.codename1.bluetooth but "
                        + "CodenameOne_GLViewController.h is missing from the staged native "
                        + "sources at " + controllerHeader.getAbsolutePath());
            }
            try {
                replaceInFile(controllerHeader, "//#define CN1_INCLUDE_BLUETOOTH",
                        "#define CN1_INCLUDE_BLUETOOTH");
            } catch (Exception ex) {
                throw new BuildException("Failed to enable the Bluetooth backend", ex);
            }
            extraFrameworks.add("CoreBluetooth.framework");
        }
        log("Bluetooth " + (bluetoothEnabled ? "enabled" : "disabled"));
        if (calendarEnabled) {
            File iosNative = new File(nativeSources, "IOSNative.m");
            if (!iosNative.exists()) {
                throw new BuildException("The application uses com.codename1.calendar but "
                        + "IOSNative.m is missing from the staged native sources at "
                        + iosNative.getAbsolutePath());
            }
            try {
                replaceInFile(iosNative, "//#define CN1_USE_CALENDAR", "#define CN1_USE_CALENDAR");
            } catch (Exception ex) {
                throw new BuildException("Failed to enable the calendar backend", ex);
            }
            extraFrameworks.add("EventKit.framework");
        }
        log("Calendar " + (calendarEnabled ? "enabled" : "disabled"));
        // The same answer that enabled EventKit above has to reach the
        // entitlement and the usage strings, or a sandboxed build links the
        // framework and is refused access to it at first use, and an unsandboxed
        // one is terminated for a missing privacy string.
        calendarDetected = usesCalendar[0];
        bluetoothDetected = usesBluetooth[0];
        pushDetected = usesPush[0];

        // The application's entry point. A Codename One main class is a
        // Lifecycle subclass with no main(String[]), and the translator refuses
        // a class set with no main at all -- so the stub is what makes the
        // application translatable, not a convenience.
        File stubSource = new File(tmpFile, "stubSource");
        // Emptied rather than merged into: the build directory survives between
        // runs, and a stub left over from a previous one is compiled alongside
        // the new one. Two classes with the same name is a compile error at
        // best, and at worst the translator finds two entry points.
        deleteRecursive(stubSource);
        stubSource.mkdirs();
        try {
            writeStub(request, stubSource, classesDir, hints);
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Failed to generate the application entry point", ex);
        }
        // The native-interface bridges, written beside the stub so the one
        // javac pass below compiles them together with it.
        generateNativeInterfaceBindings(stubSource, resDir);
        if (!compileStub(stubSource, classesDir, portClasses)) {
            return false;
        }

        String version = request.getVersion() != null ? request.getVersion() : "1.0";
        File translatedOut = new File(tmpFile, "translated");
        // Emptied for the same reason the stub source is: the build directory
        // survives between runs, and a project generated under a previous app
        // name would be collected alongside the current one.
        deleteRecursive(translatedOut);
        translatedOut.mkdirs();

        List<String> parparCmd = new ArrayList<String>();
        parparCmd.add("java");
        List<String> translatorOpts = TranslatorHeap.extraJvmOptions();
        parparCmd.addAll(translatorOpts);
        boolean heapOverridden = TranslatorHeap.specifiesHeap(translatorOpts);
        int heapMB = TranslatorHeap.maxHeapMB(2048);
        if (!heapOverridden) {
            parparCmd.add("-Xmx" + heapMB + "m");
        }
        // Without this the translated java.io.File overwrites the port's native
        // java_io_File.m, because both want the same file name in srcRoot. Every
        // Apple target sets it.
        parparCmd.add("-DconcatenateFiles=true");
        parparCmd.add("-Dcn1.sqlite=" + databaseUsage.usesDatabase());
        parparCmd.add("-Dcn1.sqlcipher=" + databaseUsage.usesDatabaseCipher());
        NativeVerifyOption.addTo(parparCmd, request, "mac");
        parparCmd.add("-jar");
        parparCmd.add(parparVMCompilerJar.getAbsolutePath());
        // The macos output type: Objective-C plus an AppKit Xcode project, and
        // the "mac" app type below binds CodenameOneImplementation to its
        // @Concrete mac() target (MacImplementation) during translation. Without
        // that binding every devirtualized call lands on IOSImplementation and
        // the port is inert -- a green build with nothing of it running.
        parparCmd.add("macos");
        parparCmd.add(join(";", classesDir, portClasses, resDir, buildinRes, nativeSources));
        parparCmd.add(translatedOut.getAbsolutePath());
        // The application's name, not the stub's: it names the Xcode project and
        // the bundle. The stub is found as the entry point because it is the one
        // class with a main, which is how every ParparVM target works.
        parparCmd.add(request.getMainClass());
        parparCmd.add(request.getPackageName());
        parparCmd.add(request.getDisplayName());
        parparCmd.add(version);
        parparCmd.add("mac");
        // The addLibs slot. "none" when nothing extra is needed, which is what
        // this target always passed -- so a detected framework had no way to
        // reach the generated project.
        parparCmd.add(extraFrameworks.isEmpty() ? "none" : join(";", extraFrameworks));
        try {
            int outputMark = message.length();
            if (!exec(tmpFile, 900000, parparCmd.toArray(new String[0]))) {
                if (!heapOverridden && TranslatorHeap.looksOutOfMemory(message.substring(outputMark))) {
                    error(TranslatorHeap.outOfMemoryAdvice(heapMB, true), null);
                }
                return false;
            }
        } catch (Exception ex) {
            throw new BuildException("Failure while running the ParparVM translator (macos target)", ex);
        }

        File distDir = new File(translatedOut, "dist");
        String appName = request.getMainClass();
        File xcodeproj = new File(distDir, appName + ".xcodeproj");
        File srcRoot = new File(distDir, appName + "-src");
        if (!xcodeproj.exists()) {
            throw new BuildException("Translator did not emit an Xcode project at "
                    + xcodeproj.getAbsolutePath());
        }
        xcodeProjectDir = distDir;

        String bundleVersion = hints.getBundleVersion(version);
        try {
            writeGeneratedPlists(request, hints, srcRoot, appName, version, bundleVersion,
                classesDir, buildinRes);
        } catch (IOException ex) {
            throw new BuildException("Failed to write the macOS bundle metadata", ex);
        }

        attachEntitlementsToProject(distDir, appName, hints);
        applyDeploymentTargetToProject(distDir, appName, hints);

        resultDir = new File(tmpFile, "result");
        resultDir.mkdirs();

        if (hints.isSourceOnly()) {
            // mac-source: the deliverable is the project, so stop before
            // xcodebuild. Reported rather than silently skipped, because a build
            // that produces no .app and says nothing reads as a failure.
            log("macos.sourceOnly is set; the Xcode project has been generated and will not be built.");
            return true;
        }

        return buildAndPackage(request, hints, distDir, appName);
    }

    /**
     * Writes the {@code <MainClass>Stub} that boots the application.
     *
     * <p>Deliberately a fraction of the iOS one. That stub carries Facebook,
     * Google Sign-In, Apple Sign-In, WebAuthn, maps, push, ads, Firebase and a
     * watch entry point, because those integrations are wired in at the entry
     * point on iOS. None of them is part of what makes an application start, and
     * the ones that apply to macOS reach the same shared implementation without
     * the stub's help.</p>
     *
     * <p>It extends the shared {@code com.codename1.impl.ios.Lifecycle} rather
     * than a macOS one of its own: that class is pure Java with no natives, it
     * ships in both ports, and the transitions it declares are the ones
     * CN1MacAppDelegate reports.</p>
     */
    // Package-visible so a test can read the generated source. The theme mode
    // it emits decides whether the application has a dark mode at all, and
    // that is invisible until a screenshot suite runs an hour later.
    void writeStub(BuildRequest request, File stubSource, File classesDir,
            MacOSBuildHints hints) throws Exception {
        String themeMode = hints.getThemeMode();
        String svgRegistryInstall = new File(classesDir,
                "com/codename1/generated/svg/SVGRegistry.class").isFile()
                ? "            com.codename1.generated.svg.SVGRegistry.installGlobal();\n"
                : "";
        // Discovery reads classesDir, which is where the classes the customer
        // submitted land -- including a dependency's, because the Maven plugin
        // uploads a jar-with-dependencies and everything on the app's classpath is
        // merged into it. A cn1lib's NativeInterface therefore arrives here, and
        // findNativeClassesInDir also has a branch for a .jar sitting beside them,
        // but do not rely on it: that branch computes the class name as
        // entryName.substring(baseDir.length() + 1), where entryName is a
        // zip-internal path like com/foo/Bar.class and baseDir is a long absolute
        // filesystem path, so it cannot produce a loadable name. Jar-borne
        // interfaces are therefore not discovered on ANY platform today.
        //
        // Not buildinRes as well, which is where unzip routes a jar submitted as a
        // separate artifact. Reaching those would mean changing findNativeInterfaces,
        // which scans only its first root and is shared by every builder --
        // IPhoneBuilder passes exactly classesDir here too, and has for years.
        // Widening it would newly generate stubs on every platform, which is not a
        // change that belongs beside a macOS port.
        String registerNatives = registerNativeImplementationsAndCreateStubs(
                new java.net.URLClassLoader(new java.net.URL[]{getCodenameOneJar().toURI().toURL()}),
                stubSource, classesDir);
        String stubName = request.getMainClass() + "Stub";
        String src = "package " + request.getPackageName() + ";\n\n"
                + "import com.codename1.ui.*;\n"
                + "import com.codename1.system.*;\n\n"
                + "public class " + stubName + " extends com.codename1.impl.ios.Lifecycle implements Runnable {\n"
                + "    public static final String PACKAGE_NAME = \"" + request.getPackageName() + "\";\n"
                + "    public static final String APPLICATION_VERSION = \""
                + (request.getVersion() != null ? request.getVersion() : "1.0") + "\";\n"
                + "    private boolean initialized;\n"
                + "    private boolean stopped;\n"
                + "    private " + request.getMainClass() + " i = new " + request.getMainClass() + "();\n\n"
                + "    public void run() {\n"
                + "        if(!initialized) {\n"
                + "            initialized = true;\n"
                // Stamped before anything else runs, so Hardening.isHardened()
                // and the crash report's mapping id are right from the first
                // line of application code. Without it a hardened build reports
                // itself unhardened and its crash reports name no mapping, which
                // makes the uploaded mapping unreachable -- the failure is a
                // stack trace nobody can retrace, months later.
                + hardeningRuntimeProperties(request)
                + svgRegistryInstall
                + "            i.init(this);\n"
                + createStartInvocation(request, "i")
                + "        } else {\n"
                + createStartInvocation(request, "i")
                + "        }\n"
                + "    }\n\n"
                // Hide and unhide are what CN1MacAppDelegate reports as the
                // background transitions; a Mac has no suspended state.
                + "    public void applicationDidEnterBackground() {\n"
                + "        stopped = true;\n"
                + "        Display.getInstance().callSerially(new Runnable() {\n"
                + "            public void run() {\n"
                + "                i.stop();\n"
                + "            }\n"
                + "        });\n"
                + "    }\n\n"
                + "    public void applicationWillEnterForeground() {\n"
                + "        if(stopped) {\n"
                + "            stopped = false;\n"
                + "            Display.getInstance().callSerially(this);\n"
                + "        }\n"
                + "    }\n\n"
                + "    public boolean shouldApplicationHandleURL(String url, String caller) {\n"
                + "        if(i instanceof com.codename1.system.URLCallback) {\n"
                + "            return ((com.codename1.system.URLCallback)i).shouldApplicationHandleURL(url, caller);\n"
                + "        }\n"
                + "        return true;\n"
                + "    }\n\n"
                + "    public void applicationWillTerminate() {\n"
                + "        if(!stopped) {\n"
                + "            i.stop();\n"
                + "            stopped = true;\n"
                + "        }\n"
                + "        i.destroy();\n"
                + "    }\n\n"
                + "    public static void main(String[] argv) {\n"
                + "        if(!(argv != null && argv.length > 0 && argv[0].equals(\"ignoreNative\"))) {\n"
                + registerNatives
                + "        }\n"
                + "        " + stubName + " stub = new " + stubName + "();\n"
                + "        com.codename1.impl.ios.IOSImplementation.setMainClass(stub.i);\n"
                // Before Display.init, which is what triggers
                // installNativeTheme(). Without it the mode stays at the
                // runtime default of "auto" and the port loads
                // iOS7Theme.res -- which carries no $Dark styles at all, so
                // UIManager.shouldUseDarkStyle() can never answer true and
                // every dark-mode screen renders light no matter what the
                // application asks for. IPhoneBuilder has always emitted
                // this call; this builder was written without it.
                + "        com.codename1.impl.ios.IOSImplementation.setIosMode(\""
                + themeMode + "\");\n"
                + routeDispatcherInstallSource(null, "        ")
                + "        Display.init(stub);\n"
                + "    }\n"
                + "}\n";
        File pkgDir = new File(stubSource,
                request.getPackageName().replace('.', File.separatorChar));
        pkgDir.mkdirs();
        FileOutputStream out = new FileOutputStream(new File(pkgDir, stubName + ".java"));
        try {
            out.write(src.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    private static void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        f.delete();
    }

    private boolean compileStub(File stubSource, File classesDir, File portClasses) throws BuildException {
        String javacPath = System.getProperty("java.home") + "/../bin/javac";
        if (!new File(javacPath).exists()) {
            javacPath = System.getProperty("java.home") + "/bin/javac";
        }
        if (!new File(javacPath).exists()) {
            javacPath = "javac";
        }
        String[] sourceTarget = getStubCompileSourceTarget(javacPath);
        try {
            return execWithFiles(stubSource, stubSource, ".java", javacPath,
                    "-source", sourceTarget[0], "-target", sourceTarget[1],
                    // The port classes as well as the application's: the stub
                    // extends the shared Lifecycle, which ships in MacPort.jar
                    // and is not unzipped into the application's own classes.
                    "-classpath", classesDir.getAbsolutePath() + File.pathSeparator
                            + portClasses.getAbsolutePath(),
                    "-d", classesDir.getAbsolutePath());
        } catch (Exception ex) {
            throw new BuildException("Failed to compile the application entry point", ex);
        }
    }

    /**
     * Emits the Info.plist and, when the build is signed, the entitlements.
     *
     * <p>The plist is written from a map rather than patched into a template.
     * That is what removes the injection-conflict problem the Catalyst path has
     * to guard against: a key the application supplies either merges or is
     * refused, and there is no textual substitution that can half-apply.</p>
     */
    private void writeGeneratedPlists(BuildRequest request, MacOSBuildHints hints, File srcRoot,
            String appName, String version, String bundleVersion, File classesDir,
            File buildinRes) throws IOException {
        // Scanned once, up front. The same answer decides two things that have to
        // agree: which sandbox entitlements the signature carries, and which
        // usage descriptions the bundle carries. An entitlement without its
        // description is a capability the app is allowed to ask for and is
        // killed for using.
        MacOSXcodeProject.MacOSCapabilities caps = new MacOSXcodeProject.MacOSCapabilities();
        try {
            scanClassesForPermissions(classesDir, new CapabilityScanner(caps));
            // btres too: unzip routes a submitted library's jar there rather
            // than unpacking it beside the loose classes, so a capability that
            // only a cn1lib reaches is invisible in classesDir alone.
            scanClassesForPermissions(buildinRes, new CapabilityScanner(caps));
        } catch (IOException ex) {
            // A failed scan must not silently produce an entitlement set that
            // omits a capability the application uses, because the failure then
            // shows up as a permission denial at runtime with no explanation.
            throw new IOException("Failed to scan the application for macOS capabilities", ex);
        }
        // The calendar answer comes from the native-feature scan rather than
        // this one, because it is the same answer that decides whether EventKit
        // is linked at all -- one detection, one result.
        // Bluetooth joins them: one detection, one result. Re-deriving it here
        // from the invoked class is exactly how the entitlement and the
        // compiled-in code came to disagree.
        caps.usesBluetooth |= bluetoothDetected;
        caps.usesCalendar = calendarDetected;
        // Push comes from the native-feature scan for the same reason the
        // calendar does: it is the same answer that decided whether
        // CN1_INCLUDE_NOTIFICATIONS2 was enabled, and detecting it twice is
        // how the entitlement and the compiled-in code come to disagree.
        caps.usesPush = pushDetected;
        capabilities = caps;
        // loadsExternalCode: a hardened-runtime bundle that dlopens anything --
        // which a Codename One application does not, but a cn1lib shipping a
        // dylib might -- needs the library-validation exception or the load is
        // refused at runtime with nothing in the application's own logs.
        loadsExternalCode = hints.isLoadsExternalCode();

        Map<String, Object> plist = MacOSXcodeProject.infoPlist(request.getDisplayName(),
                hints.getBundleId(), version, bundleVersion, hints);

        int[] fixedSize = MacOSXcodeProject.parseFixedWindowSize(hints.getFixedWindowSize());
        if (fixedSize != null) {
            // Read back by CN1MacHost to pin the content size. The screenshot
            // suite's only lever for a deterministic pixel comparison, so it is
            // in the bundle rather than in a build-time define -- a test run has
            // to be able to set it without recompiling the port.
            plist.put("CN1FixedWindowWidth", Integer.valueOf(fixedSize[0]));
            plist.put("CN1FixedWindowHeight", Integer.valueOf(fixedSize[1]));
        }

        // Written before the inject merge so an application can still override one
        // of them, and after the generated keys so a capability the scan found
        // cannot ship without its sentence. macOS kills a process that touches a
        // TCC-gated API with no usage description -- no prompt, no catchable
        // error -- so a camera app built without this crashes on first use.
        // Calendars travels separately from the scanned capabilities: it is not
        // something the class scan detects, it is inherited from the iOS
        // usage-description hints -- and whichever channel grants the
        // entitlement needs the descriptions in the one shared plist.
        boolean calendarsGranted = calendarDetected;
        for (String channel : hints.getChannels()) {
            calendarsGranted |= hints.entitlementsFor(channel).calendars(false);
        }
        plist.putAll(MacOSXcodeProject.privacyUsageDescriptions(
                effectiveCapabilities(hints, caps), calendarsGranted,
                new MacOSXcodeProject.UsageDescriptionResolver() {
                    @Override
                    public String get(String key) {
                        return hints.getUsageDescription(key);
                    }
                }));

        List<Object> urlTypes = MacOSXcodeProject.urlTypes(hints.getBundleId(),
                hints.getUrlSchemes());
        if (urlTypes != null) {
            plist.put("CFBundleURLTypes", urlTypes);
        }

        // macos.plistInject, then the legacy macNative. spelling, then
        // ios.plistInject -- the resolver's order, like every other hint this
        // builder reads. A project that was building mac-os-x-native before this
        // port existed carries the iOS spelling, and the target name did not
        // change under it, so ignoring it would make its document types, ATS
        // exceptions and application services vanish from the bundle the first
        // time it builds here, with nothing said.
        String inject = hints.getPlistInject();
        String rawInject = null;
        if (inject != null && inject.trim().length() > 0) {
            if (MacOSXcodeProject.isRawPlistFragment(inject)) {
                // Raw <key>/<value> members, which is what the documented
                // ios.plistInject and desktop.mac.plistInject both carry, and
                // the only form that can express a dict or an array. Written
                // verbatim for the reason macos.entitlements.extra is: this hint
                // exists precisely for keys the builder does not model, so
                // parsing it would defeat it.
                rawInject = inject;
                for (String key : MacOSXcodeProject.injectedPlistKeys(inject)) {
                    if (plist.remove(key) != null) {
                        // Removed rather than left to duplicate: two entries for
                        // one key in a dict is not a valid plist, and which one
                        // wins is up to whichever parser reads it.
                        log("plistInject overrides the generated " + key
                                + "; the port depends on the generated value, so check this "
                                + "deliberately.");
                    }
                }
            } else {
                Map<String, Object> extra = new LinkedHashMap<String, Object>();
                for (String line : inject.split("\n")) {
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        extra.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                    }
                }
                List<String> collisions = MacOSXcodeProject.mergePlist(plist, extra);
                for (String key : collisions) {
                    log("plistInject overrides the generated " + key
                            + "; the port depends on the generated value, so check this "
                            + "deliberately.");
                }
            }
        }

        MacOSXcodeProject.writePlist(plist, rawInject, new File(srcRoot, appName + "-Info.plist"));
        writeAppIcon(request, srcRoot);

        // Written here rather than only at signing time, so mac-source hands the
        // developer a project that is complete: they open it in Xcode, sign it
        // themselves, and the entitlements are already sitting beside the plist
        // under the channel name. buildChannel points xcodebuild at the same
        // files rather than writing its own.
        for (String channel : hints.getChannels()) {
            writeEntitlements(hints, srcRoot, appName, channel, channelSuffix(channel));
        }
    }

    /**
     * Renders the application icon into the generated asset catalog.
     *
     * <p>The translator emits {@code AppIcon.appiconset/Contents.json} naming ten
     * PNGs and none of them, because the icon does not travel in the source
     * archive -- it rides the {@code BuildRequest} separately, which is why the
     * iOS and Windows builders read it explicitly too. Left unwritten, the
     * catalog references files that are not there: the bundle shows a generic
     * icon in the Dock and App Store validation refuses it for having none.</p>
     *
     * <p>The sizes are the ten the generated Contents.json asks for. They are
     * listed here rather than derived from it because the catalog is the
     * contract: a size the plist names and the build does not produce is the
     * failure this method exists to prevent, and it should be visible in one
     * place.</p>
     */
    private void writeAppIcon(BuildRequest request, File srcRoot) throws IOException {
        byte[] iconBytes = request.getIcon();
        File iconSet = new File(new File(srcRoot, "Images.xcassets"), "AppIcon.appiconset");
        if (iconBytes == null || iconBytes.length == 0 || !iconSet.isDirectory()) {
            if (iconSet.isDirectory()) {
                log("The build request carries no icon, so the generated asset catalog stays "
                        + "empty and the application ships with the system's generic icon.");
            }
            return;
        }
        BufferedImage icon = ImageIO.read(new ByteArrayInputStream(iconBytes));
        if (icon == null) {
            log("The build request's icon could not be decoded; the application ships with "
                    + "the system's generic icon.");
            return;
        }
        int[][] sizes = {
            {16, 1}, {16, 2}, {32, 1}, {32, 2}, {128, 1},
            {128, 2}, {256, 1}, {256, 2}, {512, 1}, {512, 2},
        };
        for (int[] size : sizes) {
            String name = "mac_" + size[0] + "x" + size[0]
                    + (size[1] == 2 ? "@2x" : "") + ".png";
            int px = size[0] * size[1];
            createIconFile(new File(iconSet, name), icon, px, px);
        }
    }

    /**
     * The capabilities the bundle has to describe: what the scan found, plus
     * anything a {@code macos.entitlements.device.*} override turned on.
     *
     * <p>The union rather than the scan, because the two have to agree. An
     * override exists precisely for access the scanner cannot see -- through a
     * cn1lib, or native code -- and granting the entitlement while omitting the
     * usage description is the worst of both: the app is allowed to ask, and
     * macOS kills it the moment it does.</p>
     *
     * <p>Across every channel, since the plist is one file and a capability
     * enabled for either channel ships in it.</p>
     */
    private MacOSXcodeProject.MacOSCapabilities effectiveCapabilities(MacOSBuildHints hints,
            MacOSXcodeProject.MacOSCapabilities scanned) {
        MacOSXcodeProject.MacOSCapabilities out = new MacOSXcodeProject.MacOSCapabilities();
        out.usesCamera = scanned.usesCamera;
        out.usesMicrophone = scanned.usesMicrophone;
        out.usesBluetooth = scanned.usesBluetooth;
        out.usesLocation = scanned.usesLocation;
        out.usesServerSockets = scanned.usesServerSockets;
        for (String channel : hints.getChannels()) {
            MacOSBuildHints.EntitlementOverrides o = hints.entitlementsFor(channel);
            out.usesCamera |= o.camera(scanned.usesCamera);
            out.usesMicrophone |= o.microphone(scanned.usesMicrophone);
            out.usesBluetooth |= o.bluetooth(scanned.usesBluetooth);
            out.usesLocation |= o.location(scanned.usesLocation);
            out.usesServerSockets |= o.networkServer(scanned.usesServerSockets);
        }
        return out;
    }

    /**
     * Names the generated entitlements in the Xcode project itself.
     *
     * <p>buildChannel passes CODE_SIGN_ENTITLEMENTS on the xcodebuild command
     * line, which covers a build this builder runs -- but not mac-source, where
     * the deliverable IS the project and the developer signs it in Xcode. The
     * files were written beside the sources and nothing referenced them, so an
     * App Store archive made from the generated project came out with no
     * sandbox entitlement at all.</p>
     *
     * <p>The first channel's file, which is the one a single-channel build has
     * and the store's when a project ships both -- and the store is the side
     * that cannot be signed without it. A developer targeting the other channel
     * changes one line in the project, which is visible and editable; an absent
     * setting is neither.</p>
     */
    private void attachEntitlementsToProject(File distDir, String appName, MacOSBuildHints hints) {
        java.util.List<String> channels = hints.getChannels();
        if (channels.isEmpty()) {
            return;
        }
        File pbxproj = new File(new File(distDir, appName + ".xcodeproj"), "project.pbxproj");
        if (!pbxproj.isFile()) {
            return;
        }
        String entitlements = appName + "-src/" + appName + "-"
                + channelSuffix(channels.get(0)) + ".entitlements";
        String infoPlist = "INFOPLIST_FILE = \"" + appName + "-src/" + appName + "-Info.plist\";";
        // The bundle identifier goes in alongside the entitlements, in the SAME
        // replacement. Naming it on the xcodebuild command line settles this
        // build and nothing else: the generated project carries no
        // PRODUCT_BUNDLE_IDENTIFIER at all, so the .xcodeproj a customer opens --
        // from an includeSource export, or from a mac-source build locally --
        // shows an empty identifier in Signing & Capabilities and cannot match a
        // provisioning profile, even though its Info.plist declares the right
        // one. One replacement rather than two because both settings anchor on
        // the same line and a second pass would match it again.
        String bundleId = hints.getBundleId() == null ? "" : hints.getBundleId().trim();
        String settings = infoPlist;
        if (bundleId.length() > 0) {
            settings += "\n\t\t\t\tPRODUCT_BUNDLE_IDENTIFIER = \"" + bundleId + "\";";
        }
        try {
            replaceInFile(pbxproj, infoPlist,
                    settings + "\n\t\t\t\tCODE_SIGN_ENTITLEMENTS = \"" + entitlements + "\";");
        } catch (Exception ex) {
            // Not fatal: a project without the setting still builds, it just
            // signs without entitlements, and saying so is more use than failing
            // a build over a cosmetic patch that did not apply.
            log("Could not name the entitlements in the generated Xcode project ("
                    + ex + "); sign with CODE_SIGN_ENTITLEMENTS set by hand.");
        }
    }

    /// Writes the configured deployment target into the generated project.
    ///
    /// The template pins `MACOSX_DEPLOYMENT_TARGET` in every build configuration,
    /// and naming it on the xcodebuild command line settles only the build this
    /// run performs. Any project handed to a customer -- a source-only
    /// deliverable, or an `includeSource` export -- is then a project whose
    /// command line never ran, so someone who raised the floor to reach an API
    /// that needs it opens one still compiling against the template value and
    /// gets exactly the availability errors the hint exists to prevent. Applied
    /// unconditionally so the project left behind agrees with the binary built.
    private void applyDeploymentTargetToProject(File distDir, String appName, MacOSBuildHints hints) {
        String target = hints.getMinDeploymentTarget();
        if (target == null || target.trim().length() == 0) {
            return;
        }
        File pbxproj = new File(new File(distDir, appName + ".xcodeproj"), "project.pbxproj");
        if (!pbxproj.isFile()) {
            return;
        }
        String replacement = "MACOSX_DEPLOYMENT_TARGET = " + target.trim() + ";";
        try {
            java.util.Set<String> found = deploymentTargetAssignments(readFileToString(pbxproj));
            if (found.isEmpty()) {
                // Matched nothing, so the template changed shape. Reported rather
                // than passed over: the symptom is otherwise invisible until the
                // customer opens the project and hits an availability error.
                log("Could not set MACOSX_DEPLOYMENT_TARGET in the generated Xcode "
                        + "project; it keeps the template default.");
                return;
            }
            for (String literal : found) {
                if (!literal.equals(replacement)) {
                    replaceInFile(pbxproj, literal, replacement);
                }
            }
        } catch (Exception ex) {
            // Not fatal, matching attachEntitlementsToProject: the project still
            // builds, it just carries the template floor.
            log("Could not set the deployment target in the generated Xcode project ("
                    + ex + "); set MACOSX_DEPLOYMENT_TARGET by hand.");
        }
    }

    /// The distinct `MACOSX_DEPLOYMENT_TARGET` assignments in a pbxproj body.
    ///
    /// Separated from the file handling so the matching can be tested without
    /// Xcode, which is the half that silently stops working when the template
    /// changes shape. Distinct rather than a list because the template carries
    /// the setting once per build configuration and both get the same value;
    /// `replaceInFile` then rewrites every occurrence of each literal.
    static java.util.Set<String> deploymentTargetAssignments(String pbxprojBody) {
        java.util.Set<String> found = new java.util.LinkedHashSet<String>();
        if (pbxprojBody == null) {
            return found;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("MACOSX_DEPLOYMENT_TARGET = [^;\\n]*;").matcher(pbxprojBody);
        while (m.find()) {
            found.add(m.group());
        }
        return found;
    }

    /// The file-name suffix for one signing channel. One place, because the
    /// entitlements are written under it and named to xcodebuild under it, and
    /// the two disagreeing would sign against a file nobody generated.
    private static String channelSuffix(String channel) {
        return MacOSBuildHints.DISTRIBUTION_APP_STORE.equals(channel) ? "appstore" : "developerid";
    }

    /**
     * Writes the entitlements for one signing channel and returns the file.
     *
     * <p>Per channel rather than once, because the two channels do not want the
     * same set: the App Store requires the sandbox and a direct build usually
     * does not. That is also why {@code distribution=both} cannot be one
     * xcodebuild run -- the entitlements are baked into the signature.</p>
     */
    private File writeEntitlements(MacOSBuildHints hints, File srcRoot, String appName,
            String channel, String suffix) throws IOException {
        boolean appStore = MacOSBuildHints.DISTRIBUTION_APP_STORE.equals(channel);
        MacOSBuildHints.EntitlementOverrides overrides = hints.entitlementsFor(channel);
        Map<String, Object> ent = MacOSXcodeProject.entitlements(appStore, overrides,
                capabilities, loadsExternalCode);
        File out = new File(srcRoot, appName + "-" + suffix + ".entitlements");
        // macos.entitlements.extra rides along as raw XML rather than through the
        // map: it exists precisely for keys this builder does not model, so
        // parsing it would defeat it.
        MacOSXcodeProject.writePlist(ent, overrides.getExtra(), out);
        return out;
    }

    /**
     * Notices whether the application reaches the crypto primitives.
     *
     * <p>Biometrics and secure storage live in the same package and are
     * deliberately not counted: they need LocalAuthentication rather than the
     * cipher implementations, and this port links the former unconditionally.</p>
     */
    /// The compile-time toggles the staged native sources have to be told about
    /// before translation, gathered in one pass.
    ///
    /// Both fail the same silent way if they are missed: the code they guard is
    /// compiled to empty bodies, so the feature is simply absent from an
    /// otherwise green build.
    private static final class NativeFeatureScanner implements Executor.ClassScanner {
        private final boolean[] usesCrypto;
        private final boolean[] usesLocalNotifications;

        private final boolean[] usesMicrophone;
        private final boolean[] usesBluetooth;
        private final boolean[] usesCalendar;
        private final boolean[] usesPush;
        private final boolean[] usesLocation;
        private final boolean[] usesAppReview;

        NativeFeatureScanner(boolean[] usesCrypto, boolean[] usesLocalNotifications,
                boolean[] usesMicrophone, boolean[] usesBluetooth, boolean[] usesCalendar,
                boolean[] usesPush, boolean[] usesLocation, boolean[] usesAppReview) {
            this.usesCrypto = usesCrypto;
            this.usesLocalNotifications = usesLocalNotifications;
            this.usesMicrophone = usesMicrophone;
            this.usesBluetooth = usesBluetooth;
            this.usesCalendar = usesCalendar;
            this.usesPush = usesPush;
            this.usesLocation = usesLocation;
            this.usesAppReview = usesAppReview;
        }

        @Override
        public void implementsInterface(String cls, String iface) {
        }

        @Override
        public void usesClass(String cls) {
            if (cls == null) {
                return;
            }
            // The whole com.codename1.notifications package, not just
            // LocalNotification. That package exists for nothing else, and the
            // define gates one thing -- whether the notification code is
            // compiled at all -- so anything in it is evidence enough.
            //
            // Narrower than this leaves a real hole: an application that only
            // calls Display.requestNotificationPermission() references
            // NotificationPermissionRequest and NotificationPermissionCallback
            // and never names LocalNotification, so the define stayed off and
            // requestNotificationPermission() returned a synthetic grant
            // without ever asking macOS. IPhoneBuilder matches only
            // LocalNotification and has the same hole; it is left alone here
            // because widening it changes what an existing iOS build compiles.
            if (cls.startsWith("com/codename1/notifications/")) {
                usesLocalNotifications[0] = true;
            }
            if (cls.startsWith("com/codename1/bluetooth/")) {
                usesBluetooth[0] = true;
            }
            // The whole package: every analyzer goes through the same native
            // bridge, and CN1Vision.m compiles to a fallback returning 0 unless
            // the define is on -- so an application referencing any of them
            // reported "vision unsupported" at run time on a build that had
            // linked nothing to support it.
                // No vision detection here. INCLUDE_CN1_VISION cannot simply be
                // flipped on this target: CN1Vision.m declares #error unless it
                // is compiled with -fobjc-arc, and this port compiles without
                // ARC, and it reads UIImage.CGImage, which NSImage does not
                // have. Enabling it produced a project that does not build.
                // Porting the file -- a per-file ARC flag and an NSImage to
                // CGImage bridge -- is its own change.

            if (cls.startsWith("com/codename1/calendar/LocalCalendarSource")) {
                usesCalendar[0] = true;
            }
            // The same test the capability scan uses, and for the same reason:
            // MapComponent is the one maps class that reads the LocationManager,
            // and an application using it never names the location API itself.
            if (cls.startsWith("com/codename1/location/")
                    || cls.equals("com/codename1/maps/MapComponent")) {
                usesLocation[0] = true;
            }
            // By package, which is safe here in a way it was not for the camera:
            // nothing in this port references com.codename1.appreview, so the
            // scan sees only the application.
            if (cls.startsWith("com/codename1/appreview")) {
                usesAppReview[0] = true;
            }
            if (!cls.startsWith("com/codename1/security/")) {
                return;
            }
            // Biometrics and secure storage live in the same package and are
            // LocalAuthentication and Keychain rather than CommonCrypto, so they
            // must not drag the cipher suite in.
            String shortName = cls.substring("com/codename1/security/".length());
            boolean isBiometric = shortName.startsWith("Biometric")
                    || shortName.equals("SecureStorage")
                    || shortName.equals("AuthenticationOptions");
            if (!isBiometric) {
                usesCrypto[0] = true;
            }
        }

        @Override
        public void usesClassMethod(String cls, String method) {
            if (opensMicrophone(cls, method)) {
                usesMicrophone[0] = true;
            }
            if (usesNotifications(cls, method)) {
                usesLocalNotifications[0] = true;
            }
            // Push registration, detected here rather than only in the later
            // capability scan: that one runs long after the native sources are
            // staged, and CN1_INCLUDE_NOTIFICATIONS2 has to be decided while
            // they can still be edited.
            if (usesPushRegistration(cls, method)) {
                usesPush[0] = true;
            }
            if (usesLocalCalendar(cls, method)) {
                usesCalendar[0] = true;
            }
            if (requestsNativeReview(cls, method)) {
                usesAppReview[0] = true;
            }
            if (usesUtilCrypto(cls, method)) {
                usesCrypto[0] = true;
            }
            if (reachesBluetoothViaDisplay(cls, method)) {
                // Display.getBluetooth() names com.codename1.bluetooth only in
                // its return type, which this scan does not read. An application
                // that goes on to call something on the result is caught by the
                // owner of THAT call; one that only obtains it is not.
                usesBluetooth[0] = true;
            }
        }
    }

    /**
     * Whether an invoked method is the low-level in-app review entry point.
     *
     * <p>The package test in {@code usesClass} catches the
     * {@code com.codename1.appreview} facade, which is how an application is
     * expected to ask. It does not catch one that calls
     * {@code CN.requestNativeInAppReview} or the identical method on
     * {@code Display} directly, and those are public documented API. Missing
     * them leaves {@code CN1_USE_APPREVIEW} undefined, so the native request
     * is compiled out and the callback still reports success -- a review
     * prompt that silently never appears.</p>
     */
    static boolean requestsNativeReview(String cls, String method) {
        if (cls == null || method == null) {
            return false;
        }
        return (cls.equals("com/codename1/ui/CN") || cls.equals("com/codename1/ui/Display"))
                && method.equals("requestNativeInAppReview");
    }

    /// Whether an invoked method is one of com.codename1.io.Util's crypto
    /// delegates, which reach the same natives com.codename1.security does.
    ///
    /// The package test alone misses them, and the consequence is not a
    /// disabled feature. Every other stub answers CN1_CRYPTO_E_UNSUPPORTED,
    /// which surfaces as an exception; secureRandomBytes returns void and its
    /// stub simply leaves the caller's buffer alone, so an application that
    /// asks for random bytes with the crypto suite compiled out receives the
    /// zero-filled array it passed in and has no way to tell. Predictable
    /// "random" data is worse than an error.
    ///
    /// The whole surface at once rather than the one method that was reported:
    /// these nine are Util's complete set of crypto delegates.
    static boolean usesUtilCrypto(String cls, String method) {
        if (cls == null || method == null) {
            return false;
        }
        if (!cls.equals("com/codename1/io/Util")) {
            return false;
        }
        return method.equals("secureRandomBytes")
                || method.equals("aesEncrypt")
                || method.equals("aesDecrypt")
                || method.equals("rsaEncrypt")
                || method.equals("rsaDecrypt")
                || method.equals("cryptoSign")
                || method.equals("cryptoVerify")
                || method.equals("generateRsaKeyPair")
                || method.equals("generateSymmetricKey");
    }

    /**
     * Whether an invoked method means the application uses notifications.
     *
     * <p>The package test in {@code usesClass} catches an application that
     * names any of these types, which is most of them. It does not catch a
     * lambda: {@code requestNotificationPermission(r -> ...)} compiles to an
     * invokedynamic whose reported owner is the app class holding the lambda
     * body, and the functional interface appears only in a descriptor the scan
     * does not read. So the entry point is matched by name as well.</p>
     */
    static boolean usesNotifications(String cls, String method) {
        if (cls == null || method == null) {
            return false;
        }
        // cancelLocalNotification as well as the permission request. An update
        // whose only notification code REMOVES one scheduled by an earlier
        // version names nothing in com.codename1.notifications, so without this
        // CN1_INCLUDE_NOTIFICATIONS2 stays off, cn1CancelScheduledLocalNotificationById
        // compiles away, and the notification the application asked to withdraw
        // still fires.
        return cls.equals("com/codename1/ui/Display")
                && (method.indexOf("requestNotificationPermission") > -1
                    || method.indexOf("cancelLocalNotification") > -1);
    }

    /**
     * Whether an invoked method registers for push.
     *
     * <p>Matched on the entry points an application actually calls --
     * {@code Push.registerPush} and {@code Display.registerPush} -- rather than
     * on the push package, because naming {@code com.codename1.push.PushContent}
     * in a callback is what a RECEIVER does and a receiver needs no entitlement
     * of its own; it is registration that macOS refuses without one.</p>
     */
    static boolean usesPushRegistration(String cls, String method) {
        if (cls == null || method == null) {
            return false;
        }
        // PushClient.register() is the DOCUMENTED entry point, and matching it is
        // not optional: it reaches Display.registerPush() from inside
        // PushClient, which is framework bytecode this scan never opens -- it
        // reads the application's own classes. So an application following the
        // recommended PushClient.builder(...).build().register() path invokes
        // nothing this predicate could otherwise see, and shipped with no APNs
        // entitlement at all. Matched by equality rather than by substring so
        // unregister() does not count as registering.
        if (cls.equals("com/codename1/push/PushClient")) {
            return "register".equals(method);
        }
        if (method.indexOf("registerPush") < 0) {
            return false;
        }
        return cls.equals("com/codename1/push/Push") || isDisplay(cls);
    }

    /**
     * Whether an invoked method reaches the local calendar.
     *
     * <p>The same entry points IPhoneBuilder matches, so a project that gets
     * EventKit on iOS gets it here. EventKit is the same framework on macOS.</p>
     */
    static boolean usesLocalCalendar(String cls, String method) {
        if (cls == null || method == null) {
            return false;
        }
        if (cls.startsWith("com/codename1/calendar/LocalCalendarSource")) {
            return true;
        }
        if (cls.startsWith("com/codename1/calendar/CalendarManager")) {
            return method.indexOf("getLocalSource") >= 0 || method.indexOf("getSources") >= 0;
        }
        return cls.equals("com/codename1/ui/Display")
                && method.indexOf("getLocalCalendarSource") >= 0;
    }

    /**
     * Whether an invoked method opens the microphone.
     *
     * <p>One rule, two callers: the entitlement scan decides what the signature
     * asks for, and the feature scan decides whether the recorder is compiled at
     * all. Written twice they would drift, and the failure of the drift is an
     * application that holds the entitlement and has no recorder behind it.</p>
     */
    static boolean opensMicrophone(String cls, String method) {
        if (cls == null || method == null) {
            return false;
        }
        // The recorder is the one thing in com.codename1.media that opens a
        // microphone; createMedia and the player APIs only read a file or a
        // stream. Matched by method for the same reason the Android builder
        // matches createMediaRecorder before adding RECORD_AUDIO.
        if (cls.startsWith("com/codename1/media/")) {
            return method.indexOf("createMediaRecorder") > -1;
        }
        // Audio capture, and video capture, which records sound with the picture.
        if (cls.startsWith("com/codename1/capture/")) {
            return method.indexOf("captureAudio") > -1 || method.indexOf("captureVideo") > -1;
        }
        // Display carries its own overloads of all three, and an application
        // that calls those names them on Display rather than on the feature
        // class -- the invocation's owner is what the scan sees. Missing them
        // left a recording application with no define, no entitlement and no
        // usage description, and an implementation that then refused to record.
        return isDisplay(cls)
                && (method.indexOf("createMediaRecorder") > -1
                    || method.indexOf("captureAudio") > -1
                    || method.indexOf("captureVideo") > -1);
    }

    /// Whether an invoked method opens a camera through the low level
    /// com.codename1.camera API, which the modal Capture API above does not
    /// name. Opening one is what needs the native compiled in; asking whether a
    /// camera exists is not, and `Camera.isSupported()` is deliberately absent
    /// so that a hasCamera() check does not compile in a bridge nothing uses.
    /// The whole com.codename1.camera.Camera surface, classified once rather
    /// than one entry point per review round:
    ///
    /// <ul>
    /// <li>{@code open} -- starts an AVCaptureSession. Needs the key.</li>
    /// <li>{@code getCameras} / {@code getDefault} -- run a discovery session.
    ///     They neither prompt nor terminate without the key, but an
    ///     application enumerating cameras is an application about to open one,
    ///     so they count.</li>
    /// <li>{@code requestPermissions} -- calls requestAccessForMediaType, which
    ///     PROMPTS. Without the key macOS terminates the process rather than
    ///     failing the call, so this is the one that must not be missed.</li>
    /// <li>{@code isSupported} -- deliberately absent. It only asks whether a
    ///     backend object exists and touches no AVFoundation authorization at
    ///     all, so counting it would put a camera privacy string in every
    ///     application that merely checks.</li>
    /// </ul>
    static boolean opensCameraSession(String cls, String method) {
        if (cls == null || method == null) {
            return false;
        }
        return "com/codename1/camera/Camera".equals(cls)
                && (method.indexOf("open") > -1
                    || method.indexOf("getCameras") > -1
                    || method.indexOf("getDefault") > -1
                    // requestPermissions counts as camera use, not only as
                    // microphone use. MacCameraImpl answers it by asking
                    // AVFoundation for video authorization, and asking for that
                    // without NSCameraUsageDescription in the bundle does not
                    // fail the request -- macOS terminates the process. So the
                    // one documented call an application makes BEFORE touching
                    // a camera was the one call that did not earn it the key.
                    || method.indexOf("requestPermissions") > -1);
    }

    /// The framework's own classes that invoke the low level camera entry
    /// points. Their references sit in every build, because the tree this scan
    /// reads is the application merged with the framework -- so counting them
    /// would grant the camera entitlement and the camera privacy string to every
    /// macOS application ever built.
    ///
    /// Derived by disassembling the built framework, not guessed, and pinned by
    /// a test that fails when the set changes. That matters more than the list
    /// being short: a new framework class that opens a camera would otherwise
    /// silently put the entitlement back into every app, which is the failure
    /// this exclusion exists to prevent.
    static final String[] FRAMEWORK_CAMERA_CALLERS = {
        // Camera.getDefault() calls getCameras(), which calls open(), so the
        // class reaches its own entry points and appears in every build. This
        // one was missing from a hand-disassembled first draft of the list and
        // the test below found it immediately, which is the whole argument for
        // that test existing.
        "com/codename1/camera/Camera",
        "com/codename1/ai/vision/CodeScanner",
        "com/codename1/ai/vision/VisionCameraView",
        "com/codename1/impl/mac/MacCameraCapture",
    };

    /// Whether {@code caller} is one of those, nested classes included -- the
    /// scanner reports {@code Foo$Session} as its own type.
    static boolean isFrameworkCameraCaller(String caller) {
        if (caller == null) {
            return false;
        }
        for (int iter = 0; iter < FRAMEWORK_CAMERA_CALLERS.length; iter++) {
            String framework = FRAMEWORK_CAMERA_CALLERS[iter];
            if (caller.equals(framework) || caller.startsWith(framework + "$")) {
                return true;
            }
        }
        return false;
    }

    /// The camera-backed vision entry points an application names directly.
    ///
    /// CodeScanner and VisionCameraView open an AVFoundation session through
    /// Camera.open(), from inside the framework -- which is why they are in
    /// FRAMEWORK_CAMERA_CALLERS, so that their presence in every build does not
    /// grant every build the camera. The other half of that decision is this
    /// one: an application that REFERENCES either of them is using a camera and
    /// must get the entitlement and the usage description, or it is denied at
    /// first use.
    ///
    /// The caller is what separates the two. Inside the vision package these
    /// references are the framework talking to itself and are present in every
    /// build; from anywhere else they are the application's own.
    static boolean applicationUsesCameraBackedVision(String caller, String cls) {
        if (cls == null || caller == null
                || caller.startsWith("com/codename1/ai/vision/")) {
            return false;
        }
        return cls.equals("com/codename1/ai/vision/CodeScanner")
                || cls.startsWith("com/codename1/ai/vision/CodeScanner$")
                || cls.equals("com/codename1/ai/vision/VisionCameraView");
    }

    /// Whether an APPLICATION opening a low level camera session also opens a
    /// microphone.
    ///
    /// It does, by default: CameraSessionOptions.captureAudio is initialised to
    /// true, and CN1Camera adds an AVMediaTypeAudio input whenever it is set. So
    /// a plain Camera.open() takes the microphone, and without this the bundle
    /// carried camera metadata and no NSMicrophoneUsageDescription -- macOS then
    /// terminates the process the moment the session starts.
    /// Camera.requestPermissions(audio, cb) is the same path: it opens a probe
    /// session with exactly that option.
    ///
    /// The captureAudio(false) argument is deliberately NOT consulted, even
    /// though the scanner could see it. An application that disables audio on
    /// one session and leaves another on its default would then be reported as
    /// using no microphone and be terminated on the second one. Over-reporting
    /// is the recoverable direction and it has a documented way out --
    /// macos.entitlements.device.microphone=false turns it off for an
    /// application that knows every one of its sessions is silent.
    static boolean applicationOpensCameraMicrophone(String caller, String cls, String method) {
        // requestPermissions is part of applicationOpensCameraSession now, so
        // this is simply that test: the probe session it opens carries
        // CameraSessionOptions' default captureAudio, which is true.
        return applicationOpensCameraSession(caller, cls, method);
    }

    /// Whether an APPLICATION class opens a camera through the low level
    /// com.codename1.camera API.
    ///
    /// The modal Capture API is named by `#opensCamera` and needs no such care,
    /// because nothing inside the framework calls Capture.capturePhoto. The low
    /// level entry points do have internal callers, and the caller is the only
    /// thing that separates "this application opens a camera" from "the
    /// framework contains a code scanner".
    static boolean applicationOpensCameraSession(String caller, String cls, String method) {
        return opensCameraSession(cls, method) && !isFrameworkCameraCaller(caller);
    }

    /** Whether an invoked method opens the camera. */
    static boolean opensCamera(String cls, String method) {
        if (cls == null || method == null) {
            return false;
        }
        // Video records a picture as well as sound, so it counts for both.
        boolean captures = method.indexOf("capturePhoto") > -1
                || method.indexOf("captureVideo") > -1;
        return captures && (cls.startsWith("com/codename1/capture/") || isDisplay(cls));
    }

    /// Whether an invoked method obtains the Bluetooth API through Display.
    ///
    /// Named and package-visible so a test can drive the real rule: the call
    /// names com.codename1.bluetooth in its RETURN type alone, which no scan
    /// here reads, so this explicit test is the only thing that sees it.
    static boolean reachesBluetoothViaDisplay(String cls, String method) {
        return cls != null && method != null
                && isDisplay(cls) && method.indexOf("getBluetooth") > -1;
    }

    private static boolean isDisplay(String cls) {
        return "com/codename1/ui/Display".equals(cls) || "com/codename1/ui/CN".equals(cls);
    }

    /** Maps class references onto the entitlements they require. */
    private static final class CapabilityScanner implements Executor.ClassScanner {
        private final MacOSXcodeProject.MacOSCapabilities caps;
        /// The class currently being read. Needed because the low level camera
        /// entry points have framework callers whose references are in every
        /// build; see applicationOpensCameraSession.
        private String scanning;

        CapabilityScanner(MacOSXcodeProject.MacOSCapabilities caps) {
            this.caps = caps;
        }

        @Override
        public void scanningType(String cls) {
            scanning = cls;
        }

        @Override
        public void implementsInterface(String cls, String iface) {
        }

        @Override
        public void usesClass(String cls) {
            if (cls == null) {
                return;
            }
            if (cls.startsWith("com/codename1/bluetooth/")) {
                caps.usesBluetooth = true;
            }
            // A camera-backed vision component is a camera user. Excluding these
            // classes as framework CALLERS keeps their internal Camera.open()
            // from granting every build the camera; recognising an application's
            // reference to them is the other half of that, without which an
            // application whose only camera use is a code scanner shipped with
            // no entitlement and was denied at first use.
            if (applicationUsesCameraBackedVision(scanning, cls)) {
                caps.usesCamera = true;
                // The scanner session takes the microphone with it for the same
                // reason a plain Camera.open() does: captureAudio defaults on.
                caps.usesMicrophone = true;
            }
            // MapComponent by name rather than the whole maps package. Only that
            // class reads the LocationManager; LatLng, Coord, WebMercator and the
            // tile renderers are geometry and drawing. Scanning the app's own
            // classes cannot see the call itself -- an app that uses MapComponent
            // never names LocationManager, so the reference lives in a framework
            // class this scan does not read -- which is why the CLASS is the test
            // rather than the location API.
            if (cls.startsWith("com/codename1/location/")
                    || cls.equals("com/codename1/maps/MapComponent")) {
                caps.usesLocation = true;
            }
            // ServerSocket alone. com.apple.security.network.server is the
            // entitlement to LISTEN, and a sandboxed application that asks for
            // inbound authority it never uses has to justify it at review.
            //
            // The websocket prefix that used to sit here matched nothing: the
            // client is com.codename1.io.WebSocket, a class rather than a
            // package, and it dials out -- com.apple.security.network.client
            // already covers it. So the test granted no entitlement in practice
            // and would have granted the wrong one had the package ever existed.
            if (cls.equals("com/codename1/io/ServerSocket")) {
                caps.usesServerSockets = true;
            }
        }

        @Override
        public void usesClassMethod(String cls, String method) {
            if (cls == null || method == null) {
                return;
            }
            // A low level camera session opens the microphone unless the
            // application turns it off: CameraSessionOptions.captureAudio starts
            // true and CN1Camera adds an audio input whenever it is set. This is
            // deliberately NOT added to the native-define scanner, which shares
            // opensMicrophone() for the recorder: the camera's audio input is
            // compiled under the camera define, not under
            // INCLUDE_MICROPHONE_USAGE, so switching the recorder on here would
            // compile in a backend nothing calls. The two decisions were one
            // rule and are now genuinely two -- an entitlement with no recorder
            // behind it is exactly right when the microphone belongs to a camera
            // session.
            if (opensMicrophone(cls, method)
                    || applicationOpensCameraMicrophone(scanning, cls, method)) {
                caps.usesMicrophone = true;
            }
            // Matched by method one step finer than by package: the capture
            // package is entirely capture, but each entry point opens a
            // different device. Granting both for any reference to it declared
            // the camera for an audio-only recorder and the microphone for a
            // photo app -- and merely naming VideoCaptureConstraints, or asking
            // hasCamera(), declared both while opening nothing.
            // Capture/Display only, and NOT the low level com.codename1.camera
            // entry points, however much they deserve the same treatment.
            //
            // They cannot be told apart from the framework's own internals. This
            // scan reports the class and method being INVOKED, never the class
            // doing the invoking, and Camera.getDefault() calls getCameras(),
            // which calls open() -- so com/codename1/camera/Camera#open appears
            // in every build that carries the framework, which is every build.
            // Matching it granted the camera entitlement and the camera privacy
            // string to applications with no camera code at all, which is a
            // worse failure than the one it was trying to fix: a privacy
            // declaration nobody can justify, in every macOS app.
            //
            // Display.capturePhoto and Capture.capturePhoto have no such
            // internal callers, which is why the test below stays honest.
            //
            // An application driving com.codename1.camera directly is caught by
            // the caller-aware test instead: the framework classes that reach
            // those entry points are known and excluded by name, so a reference
            // from anywhere else is the application's own. Without it, the low
            // level API -- which is documented, and is what the modal Capture
            // API is built on -- produced no usage description and no
            // entitlement, and the application was denied the first time it
            // opened a camera.
            if (opensCamera(cls, method)
                    || applicationOpensCameraSession(scanning, cls, method)) {
                caps.usesCamera = true;
            }
        }
    }

    /**
     * Runs xcodebuild once per requested signing channel, then packages,
     * signs and notarizes each channel's output.
     *
     * <p>{@code distribution=both} is genuinely two builds. The channels differ
     * in the signing certificate AND in the entitlements the signature carries --
     * the App Store one has to be sandboxed -- so a single binary cannot be
     * relabelled into the other channel afterwards.</p>
     */
    private boolean buildAndPackage(BuildRequest request, MacOSBuildHints hints, File distDir,
            String appName) throws BuildException {
        File srcRoot = new File(distDir, appName + "-src");
        List<String> channels = hints.getChannels();
        // An unsigned App Store package is never a valid one. signingIdentity
        // "none" turns application signing off, but packaging still signs the
        // outer .pkg with the installer identity and reports a successful
        // artifact -- so the build looks fine and App Store Connect rejects it
        // for an app with no signature and none of the sandbox entitlements it
        // is required to carry. Refused here, where the customer is told which
        // hint to change, rather than by Apple hours later.
        //
        // Only the store channel: "none" is a real escape hatch for a Developer
        // ID or local build a developer wants unsigned.
        for (String channel : channels) {
            if (MacOSBuildHints.DISTRIBUTION_APP_STORE.equals(channel)
                    && hints.getSigningIdentityFor(channel) == null) {
                throw new BuildException("macos.signingIdentity.appStore=none cannot be "
                        + "combined with an App Store build: the package would carry an "
                        + "unsigned application with no entitlements and App Store Connect "
                        + "rejects it. Name a signing identity, or build only the "
                        + "developerID channel.");
            }
        }
        for (String channel : channels) {
            if (!buildChannel(request, hints, distDir, srcRoot, appName, channel,
                    channels.size() > 1)) {
                return false;
            }
        }
        return true;
    }

    private boolean buildChannel(BuildRequest request, MacOSBuildHints hints, File distDir,
            File srcRoot, String appName, String channel, boolean multiChannel)
            throws BuildException {
        String suffix = channelSuffix(channel);
        // A derived-data directory per channel: the two builds differ only in
        // signing settings, and sharing one would let xcodebuild hand the second
        // channel the first channel's signature out of the build cache.
        File derived = new File(distDir, multiChannel ? "DerivedData-" + suffix : "DerivedData");
        derived.mkdirs();

        String signingIdentity = hints.getSigningIdentityFor(channel);
        boolean automatic = hints.usesAutomaticSigning();
        List<String> cmd = new ArrayList<String>();
        cmd.add("xcodebuild");
        cmd.add("-project");
        cmd.add(new File(distDir, appName + ".xcodeproj").getAbsolutePath());
        cmd.add("-target");
        cmd.add(appName);
        cmd.add("-configuration");
        cmd.add(hints.getConfiguration());
        // SYMROOT/OBJROOT rather than -derivedDataPath. xcodebuild refuses
        // -derivedDataPath unless it is also given -scheme ("The flag -scheme,
        // -testProductsPath, or -xctestrun is required when specifying
        // -derivedDataPath"), and the generated project has no shared scheme --
        // the screenshot script writes one itself precisely because the
        // translator emits none. These two settings are target-compatible and
        // put the products in the same place under `derived`, which is where
        // findAppBundle looks.
        cmd.add("SYMROOT=" + new File(derived, "Build/Products").getAbsolutePath());
        cmd.add("OBJROOT=" + new File(derived, "Build/Intermediates.noindex").getAbsolutePath());
        // Universal by default. A Mac application is expected to run on both
        // architectures, and a single-architecture build is the kind of thing
        // nobody notices until an Intel user reports it.
        cmd.add("ARCHS=" + hints.getArch());
        cmd.add("ONLY_ACTIVE_ARCH=NO");
        cmd.add("MACOSX_DEPLOYMENT_TARGET=" + hints.getMinDeploymentTarget());
        // The identifier signing and provisioning read. Checked against a
        // generated project rather than assumed: neither the macOS template nor
        // the emitted pbxproj sets PRODUCT_BUNDLE_IDENTIFIER at all, so the
        // resolved id -- <package>.mac by default, macos.bundleId when set --
        // reached the app through the Info.plist alone and the build setting
        // stayed empty. That is enough for the product to declare the right
        // identifier and not enough for the rest: automatic signing matches a
        // provisioning profile on the build setting, and an empty one leaves it
        // matching against nothing while the bundle declares something else.
        // Stating it here makes the same value authoritative in both places.
        // Guarded only against the empty string, which xcodebuild accepts and
        // turns into a bundle nothing can install; the id itself is always
        // derived from the package when no hint sets it, so this cannot
        // silently fall back to the translator value in a normal build.
        if (hints.getBundleId() != null && hints.getBundleId().trim().length() > 0) {
            cmd.add("PRODUCT_BUNDLE_IDENTIFIER=" + hints.getBundleId().trim());
        }
        // The template hard-codes ENABLE_HARDENED_RUNTIME = YES in both
        // configurations, so the default was already right and only the opt-out
        // was broken. Passed explicitly either way, because a setting the build
        // reads from two places is a setting that disagrees with itself the next
        // time the template is edited.
        cmd.add("ENABLE_HARDENED_RUNTIME=" + (hints.isHardenedRuntime() ? "YES" : "NO"));
        if (signingIdentity == null) {
            cmd.add("CODE_SIGNING_ALLOWED=NO");
            cmd.add("CODE_SIGN_IDENTITY=");
        } else {
            File entitlements;
            try {
                entitlements = writeEntitlements(hints, srcRoot, appName, channel, suffix);
            } catch (IOException ex) {
                throw new BuildException("Failed to write the " + channel + " entitlements", ex);
            }
            if (automatic) {
                // No CODE_SIGN_IDENTITY under automatic signing: Xcode resolves
                // the certificate from the team and the provisioning profile, and
                // naming one here while it picks another is how a build ends up
                // signed by a certificate nobody chose.
                cmd.add("CODE_SIGN_STYLE=Automatic");
            } else {
                cmd.add("CODE_SIGN_STYLE=Manual");
                cmd.add("CODE_SIGN_IDENTITY=" + signingIdentity);
                // Manual signing is the only mode that takes a profile: under
                // automatic, Xcode chooses one from the team and naming a second
                // here is how a build ends up signed against a profile nobody
                // picked. Without this the hint was accepted, documented and
                // read by nothing -- getProvisioningProfileFor had no caller at
                // all -- so a configuration that requires one specific installed
                // profile either failed to sign or silently matched a different
                // one.
                String profile = hints.getProvisioningProfileFor(channel);
                if (profile != null && profile.length() > 0) {
                    cmd.add("PROVISIONING_PROFILE_SPECIFIER=" + profile);
                }
            }
            // The generated entitlements have to be named here. Neither the
            // project template nor codesign picks the file up by convention, so
            // without this the signature carries none of them: an App Store build
            // is signed without the sandbox entitlement it is required to have,
            // and the scanned capabilities -- camera, Bluetooth, location,
            // network server -- are absent, which shows up as a submission
            // rejection or a permission denial rather than as a build failure.
            cmd.add("CODE_SIGN_ENTITLEMENTS=" + entitlements.getAbsolutePath());
            if (hints.getTeamId() != null && hints.getTeamId().length() > 0) {
                cmd.add("DEVELOPMENT_TEAM=" + hints.getTeamId());
            }
        }
        cmd.add("build");
        try {
            if (!exec(distDir, 3600000, cmd.toArray(new String[0]))) {
                return false;
            }
        } catch (Exception ex) {
            throw new BuildException("xcodebuild failed for the macOS target", ex);
        }

        File built = findAppBundle(derived, appName);
        if (built == null) {
            throw new BuildException("xcodebuild reported success but no " + appName
                    + ".app was produced under " + derived.getAbsolutePath());
        }
        String base = multiChannel ? appName + "-" + suffix : appName;
        File bundle = new File(resultDir, base + ".app");
        collectBundle(built, bundle);
        if (appBundle == null) {
            appBundle = bundle;
        }
        artifacts.add(bundle);

        List<File> containers = packageChannel(hints, channel, bundle, base);
        artifacts.addAll(containers);

        // Notarization needs a signed bundle, so an unsigned build says so
        // rather than running a notarization that would be rejected.
        if (hints.isNotarize()) {
            if (MacOSBuildHints.DISTRIBUTION_APP_STORE.equals(channel)) {
                // Notarization requires Developer ID signing, so submitting the
                // store-signed artifact is rejected -- and with distribution=both
                // that rejection would fail the whole build after BOTH artifacts
                // were successfully produced. The store notarizes its own uploads.
                log("Skipping notarization of the App Store package: notarization applies to "
                        + "Developer ID distribution, and App Store submissions are notarized by "
                        + "the store.");
            } else if (signingIdentity == null) {
                log("macos.notarize is set but the " + channel + " build is unsigned, so there is "
                        + "nothing to notarize. Configure a Developer ID signing identity.");
            } else if (containers.isEmpty()) {
                notarize(request, hints, bundle);
            } else {
                // The container carries the ticket. Stapling to the .app inside a
                // dmg is invisible to the person who downloads the dmg, so it is
                // the dmg or the pkg that gets submitted and stapled.
                for (File container : containers) {
                    notarize(request, hints, container);
                }
            }
        }
        return true;
    }

    /**
     * Produces the artifacts {@code macos.packaging} asks for, beside the
     * {@code .app}, and returns them. Without this the build hands back a bundle
     * directory: nothing to upload to the App Store, and on the cloud builder
     * nothing that can even be transferred, since a result is read as a file.
     */
    private List<File> packageChannel(MacOSBuildHints hints, String channel, File bundle,
            String base) throws BuildException {
        String packaging = hints.getPackagingFor(channel);
        boolean wantsDmg = "dmg".equalsIgnoreCase(packaging) || "both".equalsIgnoreCase(packaging);
        boolean wantsPkg = "pkg".equalsIgnoreCase(packaging) || "both".equalsIgnoreCase(packaging);
        if (!wantsDmg && !wantsPkg && !"app".equalsIgnoreCase(packaging)) {
            log("Unrecognized macos.packaging value \"" + packaging
                    + "\"; producing the .app only. Valid values are app, dmg, pkg and both.");
        }
        List<File> containers = new ArrayList<File>();
        if (wantsDmg) {
            containers.add(buildDmg(hints, channel, bundle, base));
        }
        if (wantsPkg) {
            containers.add(buildPkg(hints, channel, bundle, base));
        }
        // packaging=app deliberately adds nothing here. This builder is the
        // LOCAL one, and locally the .app beside the project is the thing you
        // double-click -- wrapping it in a zip would only make the developer
        // unpack it again.
        //
        // The zip the Mac.packaging() javadoc promises belongs to the cloud,
        // and the cloud runs a different program: BuildDaemon's MacOSBuilder,
        // whose own packageChannel() ends with
        // "if (out.isEmpty()) out.add(archiveBundle(...))" for exactly the
        // reason given there -- a result is uploaded through a FileInputStream,
        // so handing back a directory fails with "Is a directory" after an
        // otherwise successful build. Both halves are already correct; they
        // just live in different repositories.
        return containers;
    }

    private File buildDmg(MacOSBuildHints hints, String channel, File bundle, String base)
            throws BuildException {
        File dmg = new File(resultDir, base + ".dmg");
        dmg.delete();
        try {
            if (!exec(resultDir, 1800000, "hdiutil", "create", "-volname", base,
                    "-srcfolder", bundle.getAbsolutePath(), "-ov", "-format", "UDZO",
                    dmg.getAbsolutePath())) {
                throw new BuildException("hdiutil failed to create " + dmg.getName());
            }
            String identity = hints.getSigningIdentityFor(channel);
            if (identity != null) {
                // Gatekeeper checks the container on first open, so an unsigned
                // dmg around a signed app still warns.
                if (!exec(resultDir, 600000, "codesign", "--force", "--sign", identity,
                        "--timestamp", dmg.getAbsolutePath())) {
                    throw new BuildException("Failed to sign " + dmg.getName());
                }
            }
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Failed to build " + dmg.getName(), ex);
        }
        return dmg;
    }

    private File buildPkg(MacOSBuildHints hints, String channel, File bundle, String base)
            throws BuildException {
        File pkg = new File(resultDir, base + ".pkg");
        pkg.delete();
        List<String> cmd = new ArrayList<String>();
        cmd.add("productbuild");
        cmd.add("--component");
        cmd.add(bundle.getAbsolutePath());
        cmd.add("/Applications");
        String installer = emptyToNull(hints.getInstallerIdentityFor(channel));
        if (installer != null) {
            cmd.add("--sign");
            cmd.add(installer);
        } else {
            // Fatal for either channel, not a warning. productbuild accepts an
            // unsigned package happily and it is unusable at the other end:
            // App Store Connect refuses it, and Gatekeeper cannot treat an
            // unsigned installer as Developer ID distribution however well the
            // app inside it is signed -- signing the app does not sign the
            // container. Warning meant charging for an artifact nobody can ship.
            boolean appStore = MacOSBuildHints.DISTRIBUTION_APP_STORE.equals(channel);
            throw new BuildException("macos.signingIdentity.installer is not set, so the "
                    + (appStore ? "App Store" : "Developer ID")
                    + " package would be unsigned and "
                    + (appStore ? "App Store Connect would refuse it"
                                : "Gatekeeper would not accept it as Developer ID distribution")
                    + ". It wants a \""
                    + (appStore ? "3rd Party Mac Developer Installer" : "Developer ID Installer")
                    + "\" certificate, which is a different certificate from the application "
                    + "signing identity.");
        }
        cmd.add(pkg.getAbsolutePath());
        try {
            if (!exec(resultDir, 1800000, cmd.toArray(new String[0]))) {
                throw new BuildException("productbuild failed to create " + pkg.getName());
            }
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Failed to build " + pkg.getName(), ex);
        }
        return pkg;
    }

    private static String emptyToNull(String s) {
        return s == null || s.trim().length() == 0 ? null : s;
    }

    /**
     * Submits one artifact to notarytool and staples the ticket to it.
     *
     * <p>Takes a {@code .app}, a {@code .dmg} or a {@code .pkg}. Only the bundle
     * needs archiving first; notarytool accepts a dmg or a pkg directly, and
     * wrapping one in a zip would staple the ticket to the zip instead of to the
     * thing the user opens.</p>
     */
    private void notarize(BuildRequest request, MacOSBuildHints hints, File artifact)
            throws BuildException {
        File zip = null;
        try {
            File submission = artifact;
            if (artifact.isDirectory()) {
                zip = new File(resultDir, artifact.getName() + ".zip");
                // ditto rather than zip: notarytool rejects an archive that does
                // not preserve the bundle's symlinks and extended attributes, and
                // a plain zip does not.
                if (!exec(resultDir, 600000, "ditto", "-c", "-k", "--keepParent",
                        artifact.getAbsolutePath(), zip.getAbsolutePath())) {
                    throw new BuildException("Failed to archive the application for notarization");
                }
                submission = zip;
            }
            String notarizePassword = null;
            List<String> submit = new ArrayList<String>();
            submit.add("xcrun");
            submit.add("notarytool");
            submit.add("submit");
            submit.add(submission.getAbsolutePath());
            if (hints.getNotarizeKeychainProfile() != null
                    && hints.getNotarizeKeychainProfile().length() > 0) {
                submit.add("--keychain-profile");
                submit.add(hints.getNotarizeKeychainProfile());
            } else {
                submit.add("--apple-id");
                submit.add(hints.getNotarizeAppleId());
                submit.add("--team-id");
                submit.add(hints.getNotarizeTeamId());
                submit.add("--password");
                // Through the hints, not straight off the request: the legacy
                // macNative.notarize.password spelling is still supported and
                // only the hint parser knows about it. Reading the modern key
                // directly submits an empty password for a migrated project.
                notarizePassword = hints.getNotarizePassword() == null
                        ? "" : hints.getNotarizePassword();
                submit.add(notarizePassword);
            }
            submit.add("--wait");
            // The app-specific password must not reach the build log: exec
            // appends every argument to the message this builder prints. Redacted
            // by value rather than by position, because a miscounted index
            // redacts the flag and prints the secret next to it.
            if (!execRedacted(resultDir, 3600000, new String[]{notarizePassword},
                    submit.toArray(new String[0]))) {
                throw new BuildException("Notarization was rejected");
            }
            // Stapling the ticket is what makes the application launch without a
            // network round trip on the user's machine.
            if (!exec(resultDir, 600000, "xcrun", "stapler", "staple",
                    artifact.getAbsolutePath())) {
                throw new BuildException("Failed to staple the notarization ticket");
            }
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Notarization failed", ex);
        } finally {
            if (zip != null) {
                zip.delete();
            }
        }
    }

    private File findAppBundle(File dir, String appName) {
        File[] children = dir.listFiles();
        if (children == null) {
            return null;
        }
        for (File f : children) {
            if (f.isDirectory()) {
                if (f.getName().equals(appName + ".app")) {
                    return f;
                }
                File found = findAppBundle(f, appName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Copies the built {@code .app} into the result directory.
     *
     * <p>ditto, not a recursive file copy. A bundle that embeds a framework
     * carries symlinks -- {@code Versions/Current} and the top-level binary and
     * resource entries -- and {@code File.isDirectory()} and
     * {@code FileInputStream} both follow them, so a hand-rolled copy
     * materializes each link as a real file. The copy then no longer matches the
     * signature Xcode just produced, and codesign verification and notarization
     * reject it; the original bundle passes, so it looks like the packaging step
     * broke the app. ditto also preserves the mode bits and extended attributes
     * the signature covers.</p>
     *
     * <p>The destination is removed first. Without that, a rebuild without
     * {@code mvn clean} merges the new bundle over the old one: a resource or a
     * nested binary deleted since the last build stays in what ships, and for a
     * signed build those unsealed files fail verification.</p>
     */
    private void collectBundle(File src, File dest) throws BuildException {
        try {
            if (dest.exists()) {
                deleteRecursively(dest);
            }
            dest.getParentFile().mkdirs();
            if (!exec(resultDir, 600000, "ditto", src.getAbsolutePath(), dest.getAbsolutePath())) {
                throw new BuildException("Failed to collect " + src.getName()
                        + " into the result directory");
            }
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Failed to collect the built application bundle", ex);
        }
    }

    /// Copies the theme resources staged for clang into the application's
    /// resource directory, which is where the runtime looks for them.
    ///
    /// A copy rather than a move: the native staging directory is what the
    /// offline signature gate verifies, and quietly removing files from it would
    /// make that gate answer about a set the build never assembled.
    // Package-visible so a test can drive it; the failure it prevents is
    // invisible until a screenshot suite runs on a macOS runner.
    static void stageThemeResources(File nativeSources, File buildinRes)
            throws IOException {
        File[] staged = nativeSources.listFiles();
        if (staged == null) {
            return;
        }
        for (File f : staged) {
            if (!f.isFile() || !f.getName().endsWith(".res")) {
                continue;
            }
            File target = new File(buildinRes, f.getName());
            if (target.exists()) {
                // The application's own resource of the same name wins. unzip()
                // put it there from the submitted archive, and a theme the
                // developer shipped is not ours to overwrite.
                continue;
            }
            java.nio.file.Files.copy(f.toPath(), target.toPath());
        }
    }

    /// Removes a tree, tolerating one that is not there.
    ///
    /// No symlink following: descending through a link would delete outside the
    /// tree being removed, and a signed bundle is full of them.
    // Package-visible so a test can drive the real recursion: asserting that
    // isSymlink answers correctly is worth little unless something also asserts
    // that this loop acts on the answer.
    static void deleteRecursively(File f) throws IOException {
        if (!f.exists()) {
            return;
        }
        if (f.isDirectory() && !isSymlink(f)) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (!f.delete() && f.exists()) {
            throw new IOException("Failed to delete " + f.getAbsolutePath());
        }
    }

    /// Whether this path is itself a symbolic link.
    ///
    /// java.nio answers this about the LAST component only, which is the whole
    /// question. Comparing the canonical path against the absolute one answers
    /// a different one -- "is any component of this path a link" -- and on macOS
    /// that is true of nearly every path a build touches: /var and /tmp are
    /// themselves links into /private, so a staging directory under
    /// /var/folders/... canonicalises somewhere else and every ordinary
    /// directory in it looked like a symlink. deleteRecursively then declined to
    /// descend, and deleting a directory it had just refused to empty failed and
    /// aborted the build -- on any build that reused a non-empty output
    /// directory, which is every retried or incremental one.
    private static boolean isSymlink(File f) {
        return java.nio.file.Files.isSymbolicLink(f.toPath());
    }

    private void extractJarResource(String resource, File destDir) throws Exception {
        InputStream is = getResourceAsStream(resource);
        if (is == null) {
            throw new BuildException("Required bundled resource missing: " + resource);
        }
        java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(is);
        try {
            java.util.zip.ZipEntry entry;
            byte[] buf = new byte[8192];
            String destCanon = destDir.getCanonicalPath() + File.separator;
            while ((entry = zis.getNextEntry()) != null) {
                File out = new File(destDir, entry.getName());
                // Zip slip: reject an entry that escapes destDir.
                if (!out.getCanonicalPath().startsWith(destCanon)) {
                    throw new BuildException("Refusing to extract entry outside the target "
                            + "directory (zip slip): " + entry.getName());
                }
                if (entry.isDirectory()) {
                    out.mkdirs();
                    continue;
                }
                out.getParentFile().mkdirs();
                FileOutputStream fos = new FileOutputStream(out);
                try {
                    int n;
                    while ((n = zis.read(buf)) != -1) {
                        fos.write(buf, 0, n);
                    }
                } finally {
                    fos.close();
                }
            }
        } finally {
            zis.close();
        }
    }

    private static String join(String sep, List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (String v : values) {
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(v);
        }
        return sb.toString();
    }

    private static String join(String sep, File... files) {
        StringBuilder sb = new StringBuilder();
        for (File f : files) {
            if (sb.length() > 0) {
                sb.append(sep);
            }
            sb.append(f.getAbsolutePath());
        }
        return sb.toString();
    }
}
