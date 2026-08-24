/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.build.shared.PlatformFeatureCatalog;
import com.codename1.util.IOSAppIntentsBuilder;
import com.codename1.util.IOSWalletExtensionBuilder;
import com.codename1.util.MatterExtensionBuilder;
import com.codename1.util.IOSWidgetExtensionBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.Comparator;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.Set;
import java.io.DataInputStream;
import java.util.regex.Pattern;

/**
 *
 * @author Shai Almog
 * @author Steve Hannah
 */
public class IPhoneBuilder extends Executor {
    private boolean useMetal;

    // macNative.enabled=true switches this iOS build to also emit a native Mac
    // variant of the same app. All Mac-specific code lives in MacNativeBuilder
    // (same package). The underlying Apple technology is Mac Catalyst, but
    // that is an implementation detail -- never surfaced in hint names.
    private final MacNativeBuilder macNativeBuilder = new MacNativeBuilder(this);

    // Watch delegate: adds an Apple Watch (watchOS) target rendered via the Core
    // Graphics backend. Like macNativeBuilder this is inert unless the project
    // declares a codename1.watchMain, keeping the iOS build unchanged.
    private final WatchNativeBuilder watchNativeBuilder = new WatchNativeBuilder(this);

    /// Where each entry-point stub lives once they have been separated, or null when there is one
    /// translation and the classpath is untouched. See WatchNativeBuilder.isolateStub.
    /// The lifecycle class the project declared, before a unit-test build swaps it out.
    private String origMainClass;

    private File phoneStubDir;

    private File watchStubDir;

    // tvNative.* delegate: adds an Apple TV (tvOS) target. tvOS is handled like
    // the Mac Catalyst slice (Metal + GL stub headers + GL-only sources excluded)
    // but as a separate appletvos target. Inert unless tvNative.enabled (or
    // codename1.tvMain) is set, keeping the iOS build unchanged.
    private final TvNativeBuilder tvNativeBuilder = new TvNativeBuilder(this);

    private boolean enableGalleryMultiselect;
    private boolean usePhotoKitForMultigallery;
    private boolean enableWKWebView, disableUIWebView;
    private String pod = "/usr/local/bin/pod";
    private int podTimeout = 300000; // 5 minutes
    private int xcodeVersion;
    private static final String GOOGLE_SIGNIN_TUTORIAL_URL = "http://www.codenameone.com/...";

    /**
     * The CocoaPods requirement injected when an app enables Google sign-in.
     * Held here rather than inline so the arm64-simulator floor it encodes is
     * assertable -- see the comment at the injection site.
     */
    static final String GOOGLE_SIGNIN_POD = "GoogleSignIn ~>7.1";

    /** Deployment target floor that {@link #GOOGLE_SIGNIN_POD} requires. */
    static final String GOOGLE_SIGNIN_MIN_IOS = "12.0";

    private File resultDir;
    private boolean includePush;
    private File tmpFile;
    private File icon57;
    private File icon512;
    // Bumped from 12.0 → 13.0 to enable NSURLSessionWebSocketTask
    // (iOS 13+) used by com.codename1.io.WebSocket's iOS implementation.
    // BuildDaemon's iOS lane needs the same bump.
    private static final String DEFAULT_MIN_DEPLOYMENT_VERSION = "13.0";

    // StringBuilder used for constructing ruby script with xcodeproj
    // which adds localized strings files to the project.
    private StringBuilder installLocalizedStringsScript = new StringBuilder();

    // Populated by processLocalizedIcons from cn1_icon_LANG[_COUNTRY].png files
    // found in common/src/resources. Keys are alternate icon names embedded in the
    // Info.plist (e.g. "AppIcon_en_GB"); values are the normalized locale match key
    // (e.g. "en_GB" or just "en" when no country is supplied).
    private Map<String, String> localizedIcons = new LinkedHashMap<String, String>();

    private boolean detectJailbreak;
    private boolean appAttest;

    private boolean runPods=false;
    private boolean runSpm=false;
    private boolean photoLibraryUsage;
    private String buildVersion;
    // Where the .ios.appext archives are parked between being taken out of the resources
    // directory and being unpacked into dist/. Null when the app brought none.
    private File appExtensionArchiveDir;
    private boolean usesLocalNotifications;
    private boolean usesPurchaseAPI;
    private boolean usesAppReview;
    private boolean usesWalletApi;
    private boolean usesCryptoAPI;
    private boolean usesCryptoGcm;
    private boolean usesBiometrics;
    private boolean usesNfc;
    private boolean usesDatabase;
    private boolean usesDatabaseCipher;
    private boolean usesBluetooth;
    private boolean usesBluetoothPeripheral;

    // See AndroidGradleBuilder for why the store flag is separate from the
    // umbrella one: com.codename1.health.sensors is pure BLE and must not
    // pull in HealthKit or its entitlement.
    private boolean usesHealth;
    private boolean usesHealthStore;
    /// The URL schemes a jailbreak's package managers and file browsers register.
    /// Probed by IOSImplementation.getCompromiseReasons(), and dead weight without the
    /// declaration below: since iOS 9, canOpenURL: answers false for any scheme the app
    /// has not listed, whatever is installed. Cydia was the only one the runtime asked
    /// about, and Cydia belongs to a rootful jailbreak nobody ships for current iOS --
    /// a rootless device runs Sileo, so the probe looked for the one front end certain
    /// not to be there.
    ///
    /// Four, not the obvious longer list. These entries are spent out of the app's
    /// budget, not ours -- iOS caps the array, at 25 entries for an app linked against
    /// the iOS 27 SDK -- so a secondary probe taking a quarter of it is not a reasonable
    /// trade. `undecimus` and `activator` were dropped on that basis: unc0ver is the
    /// iOS 11-14 era and Activator is a tweak rather than evidence of one. What is left
    /// covers rootless (sileo), the file browser almost every jailbroken device carries
    /// (filza), the alternative package manager (zbra) and rootful (cydia).
    ///
    /// THE ORDER IS LOAD-BEARING, most valuable first. When the cap below leaves room for
    /// only some of these, the ones at the front are the ones declared. Cydia was first
    /// and it is the worst of the four: it belongs to a rootful jailbreak nobody ships
    /// for current iOS, so an app with one slot left spent it on the obsolete probe and
    /// dropped Sileo -- silently reproducing, in the fix, the exact rootless blind spot
    /// the fix is for.
    static final String[] JAILBREAK_QUERY_SCHEMES = {
        "sileo", "filza", "zbra", "cydia"
    };

    /// How many LSApplicationQueriesSchemes entries iOS honours.
    ///
    /// The cap keys off the SDK the app was LINKED against, not the OS it runs on: 50 for
    /// iOS 15 and later, 25 for iOS 27 and later. Going over does not fail the build --
    /// canOpenURL: simply answers false for the entries past the cap, which is the same
    /// silent wrong answer this declaration exists to prevent, except now it is the app's
    /// own schemes that quietly stop resolving.
    private int applicationQueriesSchemeCap() {
        return xcodeVersion >= 27 ? 25 : 50;
    }

    /// Entries the Info.plist renderer appends to this array on its own, after every
    /// caller here has had its say.
    ///
    /// Counted rather than ignored because a ceiling that the renderer then walks past is
    /// not a ceiling. The renderer adds `fbauth2` and `gplus` off exactly these two hints,
    /// so an app within two of the cap and using Facebook or Google sign-in would have
    /// been told its schemes fit and then shipped a plist where they did not.
    private int reservedApplicationQueriesSchemes(BuildRequest request) {
        int reserved = 0;
        if (request.getArg("facebook.appId", null) != null) {
            reserved++;
        }
        if (request.getArg("ios.gplus.clientId", null) != null) {
            reserved++;
        }
        return reserved;
    }

    /// Adds `schemes` to ios.applicationQueriesSchemes, entry by entry.
    ///
    /// Entry by entry rather than as a substring, because a project that already queries
    /// `sileo-installer` contains "sileo", and skipping on that basis leaves the exact
    /// scheme canOpenURL: is asked about undeclared -- which is the failure this exists
    /// to prevent, and it fails silently.
    ///
    /// A project that declares the array through ios.plistInject is left alone and told
    /// so. The plist renderer emits its own LSApplicationQueriesSchemes key for this hint
    /// without looking at the injected fragment, so writing the hint as well would put
    /// the key in the plist twice -- and a plist with a duplicate key is not a plist that
    /// reliably keeps either value.
    private void declareApplicationQueriesSchemes(BuildRequest request,
            String[] schemes, String why) {
        java.util.List<String> alreadyInjected =
                WatchNativeBuilder.injectedPlistKeys(request)
                        .contains("LSApplicationQueriesSchemes")
                        ? WatchNativeBuilder.injectedPlistStringArray(request,
                                "LSApplicationQueriesSchemes")
                        : null;
        String queries = request.getArg("ios.applicationQueriesSchemes", "");
        java.util.List<String> declared = new ArrayList<String>();
        for (String entry : queries.split(",")) {
            String trimmed = entry.trim();
            if (trimmed.length() > 0) {
                declared.add(trimmed);
            }
        }
        java.util.List<String> missing = new ArrayList<String>();
        java.util.List<String> overCap = new ArrayList<String>();
        int cap = applicationQueriesSchemeCap() - reservedApplicationQueriesSchemes(request);
        for (String scheme : schemes) {
            if (declared.contains(scheme)) {
                continue;
            }
            if (alreadyInjected != null) {
                if (!alreadyInjected.contains(scheme)) {
                    missing.add(scheme);
                }
                continue;
            }
            // Ours are the ones that give way. A project near the cap is already
            // spending it on schemes its features need, and appending past the cap
            // would not buy this probe anything anyway -- iOS ignores the overflow --
            // while pushing the app's own entries into the ignored region.
            if (declared.size() >= cap) {
                overCap.add(scheme);
                continue;
            }
            declared.add(scheme);
        }
        if (!overCap.isEmpty()) {
            log("ios.applicationQueriesSchemes already holds " + declared.size()
                    + " entries and this build has room for " + cap
                    + " (iOS honours at most " + applicationQueriesSchemeCap()
                    + " for this SDK), so " + overCap + " was not added. " + why);
        }
        if (alreadyInjected != null) {
            if (!missing.isEmpty()) {
                log("ios.plistInject already declares LSApplicationQueriesSchemes, so "
                        + missing + " was not added for you. Add those entries to that "
                        + "array or " + why);
            }
            return;
        }
        StringBuilder joined = new StringBuilder();
        for (String entry : declared) {
            if (joined.length() > 0) {
                joined.append(",");
            }
            joined.append(entry);
        }
        request.putArgument("ios.applicationQueriesSchemes", joined.toString());
    }

    /// Treats a blank hint as missing.
    private static String trimToNull(String v) {
        if (v == null) {
            return null;
        }
        String t = v.trim();
        return t.length() == 0 ? null : t;
    }

    /// What the scan saw about health background listeners, and which of
    /// them the generated factory can actually construct.
    private final HealthListenerScan healthScan = new HealthListenerScan();

    /// The listener bindings each root gets, resolved once where the stubs are written and read
    /// again where the factory sources are generated.
    private java.util.Map<String, String> phoneHealthListeners =
            java.util.Collections.emptyMap();

    private java.util.Map<String, String> watchHealthListeners =
            java.util.Collections.emptyMap();

    /// Writes one root's generated factory, if that root binds anything.
    private void writeHealthBindings(File stubSource,
            java.util.Map<String, String> listeners, String classSuffix)
            throws BuildException {
        String source = HealthListenerBindings.generate(listeners, classSuffix);
        if (source == null) {
            return;
        }
        File healthBindingsFile = new File(stubSource,
                HealthListenerBindings.sourcePath(classSuffix));
        healthBindingsFile.getParentFile().mkdirs();
        try (OutputStream bindings = new FileOutputStream(healthBindingsFile)) {
            bindings.write(source.getBytes("UTF-8"));
        } catch (Exception ex) {
            throw new BuildException(
                    "Failed to write the health listener bindings", ex);
        }
        log("Generated health background-listener bindings for "
                + listeners.keySet());
    }

    /// The indented install statement for one root's factory, or "" when that root has no
    /// listeners to bind.
    private String healthBindingsInstall(java.util.Map<String, String> listeners,
            String classSuffix) {
        String statement = HealthListenerBindings.installStatement(listeners, classSuffix);
        return statement == null ? "" : "            " + statement;
    }


    boolean phoneUsesHealthData(BuildRequest request) {
        // App-wide, which is the grain the API scan works at and the grain this answer is
        // reported at. A per-root reachability walk used to narrow it per target; it was deleted
        // because the thing it protected against does not exist. ParparVM copies every non-class
        // file on the classpath into its output verbatim, so a native .m is staged and compiled
        // for whichever target lists it whether or not any Java stub references it -- the walk
        // never prevented the build failure it was written for. A native that cannot compile for
        // watchOS is guarded with TARGET_OS_WATCH in its own source, as the port guards its own in
        // 71 files, and watchNative.health overrides this answer when the scan is wrong.
        return usesHealthRead || usesHealthWrite || usesHealthWorkout
                || healthCapabilityDeclared(request);
    }


    /// Whether the detected usage READS from the store, per root.
    ///
    /// Collapsing the direction to "uses HealthKit" was enough to decide the entitlement, and not
    /// enough to decide the purpose string. Apple wants the one matching the operation: a bundle
    /// that only reads and declares only NSHealthUpdateUsageDescription is refused at
    /// authorization exactly as if it had declared nothing. The phone plist pass already keeps them
    /// apart; the watch pass had one boolean and accepted either string.
    boolean phoneReadsHealthData() {
        return usesHealthRead;
    }

    /// Whether the detected usage WRITES to the store, per root. A workout writes: it saves the
    /// session and the samples it was fed.
    boolean phoneWritesHealthData() {
        return usesHealthWrite || usesHealthWorkout;
    }


    /// HealthKit asked for explicitly, by any of its spellings.
    private boolean healthCapabilityDeclared(BuildRequest request) {
        return
                // The parent entitlement asked for outright. Enumerating only the two
                // sub-capabilities missed the plainest declaration of all: a project with native
                // health code that says com.apple.developer.healthkit=true and supplies its purpose
                // string. The phone kept the entitlement it was handed and the watch, signed
                // independently, went without it.
                "true".equalsIgnoreCase(request.getArg(
                        "ios.entitlements.com.apple.developer.healthkit", "false"))
                || healthCapabilityRequested(request, "ios.health.backgroundDelivery",
                        "background-delivery")
                || healthCapabilityRequested(request, "ios.health.recalibrateEstimates",
                        "recalibrate-estimates");
    }

    /// A HealthKit sub-capability requested under either spelling: the short alias, or the
    /// canonical entitlement key written out in full. The generic renderer emits whatever is in the
    /// `ios.entitlements.*` namespace, so a project that used the long spelling got its
    /// sub-capability emitted while a gate reading only the aliases left the parent entitlement
    /// off -- the unsignable set the aliases exist to avoid, reached by the other spelling.
    private static boolean healthCapabilityRequested(BuildRequest request, String alias,
            String suffix) {
        return "true".equalsIgnoreCase(request.getArg(alias, "false"))
                || "true".equalsIgnoreCase(request.getArg(
                        "ios.entitlements.com.apple.developer.healthkit." + suffix, "false"));
    }

    private boolean usesHealthRead;
    private boolean usesHealthWrite;
    private boolean usesHealthWorkout;

    /// Whether a sensor session was seen asking to write its samples through to the store.
    ///
    /// Kept apart from usesHealthWrite, which write-through also sets: the two are the same
    /// permission but not the same evidence, and only this one says the sensors package is a route
    /// into HealthKit for the root that reaches it.
    boolean sensorWriteThrough;

    private boolean usesCn1Camera;
    private boolean usesCn1Ar;
    private boolean usesCn1Vision;
    private boolean usesCn1Language;
    private boolean usesCn1Inference;
    // Set when the app references com.codename1.car.* (Apple CarPlay support). Gates the
    // CN1_USE_CARPLAY native define, CarPlay.framework linkage, the carplay entitlement and the
    // CarPlay scene in the Info.plist scene manifest. Apps that never touch the API see no change.
    private boolean usesCar;
    // Set when the app references com.codename1.surfaces.* (home-screen widgets + live
    // activities). Gates the CN1_USE_WIDGETS native define, the CN1Widgets WidgetKit extension
    // target, the app group / CN1SurfacesAppGroup + NSSupportsLiveActivities plist injection and
    // the cn1surface URL scheme. Apps that never touch the API see no change.
    private boolean usesSurfaces;
    // True when the app references com.codename1.intents. Gates the CN1_USE_INTENTS native
    // define, CoreSpotlight.framework, the generated Swift App Intents declarations and, only
    // when an @AppIntent is actually declared, the App Intents deployment floor.
    private boolean usesIntents;
    // True when intents.json declares at least one @AppIntent, as opposed to an app that only
    // indexes content. The distinction is what keeps an indexing-only app off the newer floor.
    private boolean declaresAppIntents;
    /// True when the app declares intents but ios.intents.appIntents=false asked for the App
    /// Intents declarations to be left out. Donation still needs its Swift bridge.
    private boolean appIntentsSuppressed;
    private java.util.List<Map<String, Object>> intentsManifest = new ArrayList<Map<String, Object>>();
    private java.util.List<Map<String, Object>> entitiesManifest = new ArrayList<Map<String, Object>>();
    // usesSurfaces && ios.surfaces.extension != false. When the developer opts out with
    // ios.surfaces.extension=false the whole iOS lowering is skipped (no define flip, no
    // extension, no Swift glue): the surfaces API compiles but answers unsupported at runtime.
    private boolean surfacesExtensionEnabled;
    /// True when a watch target exists and at least one kind declares a complication family, so
    /// the watch app gets a CN1WatchWidgets extension of its own.
    ///
    /// Deliberately INDEPENDENT of surfacesExtensionEnabled. A manifest whose every kind is a
    /// complication produces no iOS extension and no phone app-group entitlement, and must still
    /// produce a watch one -- that is the whole case the surfaces watch families exist for.
    private boolean surfacesWatchEnabled;
    // Resolved app group: surfaces.json appGroup > ios.surfaces.appGroup hint > group.<package>.
    private String surfacesAppGroup;
    private boolean surfacesLiveActivities;
    private final List<IOSWidgetExtensionBuilder.Kind> surfacesKinds =
            new ArrayList<IOSWidgetExtensionBuilder.Kind>();
    // Set when the app references com.codename1.wearable.* (the phone-to-watch link). Gates the
    // CN1_USE_WATCHCONNECTIVITY native define and WatchConnectivity.framework linkage on both the
    // phone target and the watch target -- WCSession is symmetric, so both halves of a pair need
    // it. Apps that never touch the API see no change.
    private boolean usesWearable;

    // Smart home (com.codename1.home.*). Three flags rather than one, because
    // the three things they gate cost very different amounts.
    //
    // usesSmartHome links HomeKit and compiles the natives -- cheap, and
    // needed even by an app that only asks whether HomeKit exists.
    //
    // usesHomeAccessoryData is what earns the com.apple.developer.homekit
    // ENTITLEMENT, and it is deliberately narrower. That entitlement has to be
    // granted on the App ID, so handing it to an app that merely called
    // getAvailability() would fail its codesigning for a capability it never
    // asked for -- the same trap the HealthKit block below documents at
    // length.
    //
    // usesHomeCommissioning is the expensive one: MatterSupport, a second
    // entitlement, an app group, a background mode, Bonjour services and a
    // whole generated app-extension target.
    private boolean usesSmartHome;
    private boolean usesHomeAccessoryData;
    private boolean usesHomeCommissioning;
    // Whether the app asked for the accessory to join a fabric of its own.
    // Set by CommissioningRequest.setCommissionToThisApp(true) -- a call the
    // scanner can see, which is why the API takes a boolean rather than being
    // a mode the developer configures somewhere the build cannot read -- or
    // by ios.home.commissioning.fabric for a call behind reflection.
    private boolean usesHomeOwnFabric;
    // A setCommissionToThisApp call whose argument the scanner could not
    // read, or one that says false while another says true. Either way the
    // build cannot honour what the app asked for -- the extension is one
    // generated file with one behaviour -- so it refuses instead of picking.
    private boolean homeFabricAmbiguous;
    private boolean usesHomeOwnFabricDeclined;

    /// Whether the generated Xcode project gets the MatterAddDeviceExtension
    /// target. True only when the app referenced
    /// com.codename1.home.commissioning and did not opt out with
    /// ios.home.commissioning=false.
    private boolean matterExtensionEnabled;

    /// The app group the Matter extension and its host share. Resolved
    /// alongside the extension and read again when the target is written.
    private String matterAppGroup;

    /**
     * Whether the API scan saw {@code com.codename1.wearable}.
     *
     * <p>Package-private for {@link WatchNativeBuilder}, which links WatchConnectivity onto the
     * watch target it generates. The phone target gets the framework through {@code addLibs}, but
     * that list is consumed before the watch target exists, so the watch half has to ask.</p>
     */
    boolean usesWearable() {
        return usesWearable;
    }

    private boolean usesOidc;
    private boolean usesAppleSignIn;
    private boolean usesWebauthn;
    private boolean usesNfcHce;

    // Deeper-network connectivity flags. Set by the classpath scanner when
    // the app references com.codename1.io.wifi.* / com.codename1.io.bonjour.*
    // The build pipeline injects the matching entitlements / Info.plist
    // strings further down. Apps that never touch the APIs see no change.
    private boolean usesWifiInfo;
    private boolean usesWifiHotspotConfig;
    private boolean usesBonjour;
    private boolean usesCalendarApi;
    private boolean usesCalendarEventApi;
    private boolean usesCalendarTaskApi;
    private String firstBonjourType;
                                  // so we need to store the main class name for later here.
    // Map will be used for Xcode 8 privacy usage descriptions.  Don't need it yet
    // so leaving it commented out.
    private Map<String,String> privacyUsageDescriptions = new HashMap<String,String>();
    
    final static int majorOSVersion;
    final static int minorOSVersion;
    final static String osVersion;
    static {
        osVersion = System.getProperty("os.version");
        StringTokenizer versionTok = new StringTokenizer(osVersion, ".");
        majorOSVersion = Integer.parseInt(versionTok.nextToken());
        minorOSVersion = Integer.parseInt(versionTok.nextToken());
    }
    

    
    public void cleanup() {
        super.cleanup();
    }

    /// Records a boolean CarPlay entitlement (e.g. com.apple.developer.carplay-audio) unless the
    /// project already set it explicitly, mirroring how the App Attest / Apple Sign-In entitlements
    /// are injected. The downstream entitlements generator emits these as &lt;true/&gt;.
    private void putCarPlayEntitlement(BuildRequest request, String key) {
        if (request.getArg("ios.entitlements." + key, null) == null) {
            request.putArgument("ios.entitlements." + key, "true");
        }
    }

    private static String maxVersionString(String commaDelimitedVersions) {
        String[] versions = commaDelimitedVersions.split(",");
        String currMax = "0.0";
        for (String version : versions) {
            version = version.trim();
            if (version.length() == 0) {
                continue;
            }
            if (compareVersionStrings(version, currMax) > 0) {
                currMax = version;
            }
        }
        
        return currMax;
    }
    
    private static int compareVersionStrings(String v1, String v2) {
        String[] p1 = v1.split("\\.");
        String[] p2 = v2.split("\\.");
        int len = Math.max(p1.length, p2.length);
        for (int i=0; i<len; i++) {
            int iPart1 = p1.length > i ? Integer.parseInt(p1[i]) : 0;
            int iPart2 = p2.length > i ? Integer.parseInt(p2[i]) : 0;
            if (iPart1 != iPart2) {
                return iPart1 < iPart2 ? -1 : 1;
            }
        }
        return 0;
    }

    private void ensurePodsInstalled() throws BuildException {
        if(!new File(pod).exists()) {
            pod = "/usr/bin/pod";
            if(!new File(pod).exists()) {
                pod = "/opt/homebrew/bin/pod";
                if(!new File(pod).exists()) {
                    log("You need to install cocoapods to proceed, to install cocoapods on your mac issue this command in the terminal: sudo gem install cocoapods --pre\n"
                            + "followed by: sudo gem install xcodeproj");
                    throw new BuildException("Please install Cocoapods in order to use ios.dependencyManager=cocoapods or ios.pods");
                }
            }
        }
        try {
            log("Pods version: " + execString(new File("."), pod, "--version"));
        } catch (Exception ex) {
            error("Please install Cocoapods in order to build iOS projects with CocoaPods.  E.g. 'sudo gem install cocoapods'.  See https://cocoapods.org/", ex);
            throw new BuildException("Please install Cocoapods in order to build iOS projects with CocoaPods.  E.g. 'sudo gem install cocoapods'.  See https://cocoapods.org/");
        }
    }

    private void ensureXcodeprojInstalled() throws BuildException {
        try {
            execString(new File("."), "ruby", "-e", "require 'xcodeproj'; puts Xcodeproj::VERSION");
        } catch (Exception ex) {
            throw new BuildException("Please install the xcodeproj Ruby gem to configure iOS Swift packages. E.g. 'sudo gem install xcodeproj'", ex);
        }
    }

    private static String escapeRuby(String input) {
        return input.replace("\\", "\\\\").replace("'", "\\'");
    }

    /** Package-private accessor so {@link MacNativeBuilder} (separate file in
     *  the same package) can use the same escaping helper. */
    static String escapeRubyStr(String input) {
        return escapeRuby(input);
    }

    static String createLldbSchemeSetupScript() {
        return "lldb_init_file = File.join(File.dirname(project_file), 'cn1.lldbinit')\n"
                + "File.write(lldb_init_file, \"# Codename One LLDB settings\\nprocess handle -s false -n false -p true SIGUSR2\\n\")\n"
                + "configure_cn1_lldb = lambda do |scheme|\n"
                + "  scheme.launch_action.xml_element.attributes['customLLDBInitFile'] = '$(SRCROOT)/cn1.lldbinit'\n"
                + "  scheme.test_action.xml_element.attributes['customLLDBInitFile'] = '$(SRCROOT)/cn1.lldbinit'\n"
                + "end\n";
    }
    
    @Override
    protected String getDeviceIdCode() {
        return "\"\"";
    }
    
    /**
     * Static libs that don't include the LC_VERSION_MIN_XXX run instructions seem
     * to cause IPATool to crash.  This occurs for .a archives compiled with Xcode before version 7.
     * We should validate it here so that the error message is sensical. (It will fail in ipatool in the
     * export step but the error won't make any sense..
     * @param file
     * @return 
     */
    private boolean validateLC_MIN_VERSION(File file) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("otool", "-lv", file.getAbsolutePath());
        Process p = pb.start();
        InputStream is = p.getInputStream();
        Scanner scanner = new Scanner(is, "UTF-8");
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.contains("LC_VERSION_MIN_")) {
                return true;
            }
        }
        try {
            p.waitFor();
        } catch (InterruptedException ex) {
            Logger.getLogger(IPhoneBuilder.class.getName()).log(Level.SEVERE, null, ex);
            log(ex.getMessage());
            
        }
        return false;
    }



    
    private File getResDir() {
        return new File(tmpFile, "res");
    }
    
    private File getBuildinRes() {
        return new File(tmpFile, "btres");
    }
    
    private String minDeploymentTargets = "12.0";
    private void addMinDeploymentTarget(String target) {
        minDeploymentTargets += ","+target;
    }
    
    private String getDeploymentTarget(BuildRequest request){
        StringBuilder sb = new StringBuilder();
        sb.append(minDeploymentTargets);
        if (request.getArg("ios.pods.platform", null) != null) {
            sb.append(",");
            sb.append(request.getArg("ios.pods.platform", ""));
        }
        if (request.getArg("ios.deployment_target", null) != null) {
            sb.append(",");
            sb.append(request.getArg("ios.deployment_target", ""));
        }
        if (request.getArg("ios.minDeploymentTarget", null) != null) {
            sb.append(",");
            sb.append(request.getArg("ios.minDeploymentTarget", ""));
        }
        return maxVersionString(sb.toString());
        
               
    }
    
    private static String append(String str, String separator, String append) {
        if (!str.trim().endsWith(separator)) {
            str += separator;
        }
        return str + append;
    }

    private static String appendFrameworks(String libraries,
                                           String... frameworks) {
        String out = libraries == null ? "" : libraries;
        for (String framework : frameworks) {
            boolean present = false;
            for (String item : out.split(";")) {
                if (framework.equalsIgnoreCase(item.trim())) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                out = out.length() == 0 ? framework : out + ";" + framework;
            }
        }
        return out;
    }

    static String appendPodSpecIfAbsent(String pods, String candidate) {
        String out = pods == null ? "" : pods;
        String candidateName = podName(candidate);
        for (String existing : out.split("[,;]")) {
            if (candidateName.equalsIgnoreCase(podName(existing))) {
                return out;
            }
        }
        return out.length() == 0 ? candidate : out + "," + candidate;
    }

    static String deduplicatePodSpecs(String pods) {
        String out = "";
        if (pods == null) {
            return out;
        }
        for (String podSpec : pods.split("[,;]")) {
            podSpec = podSpec.trim();
            if (podSpec.length() > 0) {
                out = appendPodSpecIfAbsent(out, podSpec);
            }
        }
        return out;
    }

    private static String podName(String podSpec) {
        String value = podSpec == null ? "" : podSpec.trim();
        int separator = value.indexOf(' ');
        return separator < 0 ? value : value.substring(0, separator).trim();
    }

    private void applyCatalogPlistEntry(BuildRequest request,
                                        String[] plistEntry) {
        String privacyKey = plistEntry[0];
        String requestKey = "ios." + privacyKey;
        String value = request.getArg(requestKey, null);
        if (value == null) {
            value = plistEntry[1];
            request.putArgument(requestKey, value);
        }
        if (value != null
                && !privacyUsageDescriptions.containsKey(privacyKey)) {
            privacyUsageDescriptions.put(privacyKey, value);
        }
    }

    private int getDeploymentTargetInt(BuildRequest request) {
        String target = getDeploymentTarget(request);
        if (target.indexOf(".") > 0) {
            target = target.substring(0, target.indexOf("."));
        }
        return Integer.parseInt(target);
    }


    /**
     * The Facebook SDK pods, at whatever version the request asked for.
     *
     * <p>Its own method so the validation below has a caller a test can reach.</p>
     */
    String facebookPods(BuildRequest request) {
        String v = podVersionRequirement(
                request.getArg("ios.facebook.version", "~>5.6.0"), "~>5.6.0");
        return "FBSDKCoreKit " + v + ",FBSDKLoginKit " + v + ",FBSDKShareKit " + v;
    }

    /**
     * A CocoaPods version requirement, or the default when the hint is not one.
     *
     * <p>This value is appended to the pod list and interpolated into the generated
     * Podfile unescaped, and a Podfile is Ruby that {@code pod install} executes -- so a
     * hint carrying a quote and a newline is code running in the build workspace, which
     * holds signing material. Version requirements are a tiny language ("~> 5.6.0",
     * "&gt;= 5.0", "5.6.0"), so anything outside it is refused rather than escaped:
     * escaping invites the next value that needs a different escape.</p>
     */
    private String podVersionRequirement(String hint, String fallback) {
        if (hint == null || hint.length() == 0) {
            return fallback;
        }
        for (int i = 0; i < hint.length(); i++) {
            char c = hint.charAt(i);
            boolean ok = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || c == '.' || c == '-' || c == '+'
                    || c == '~' || c == '>' || c == '<' || c == '=' || c == ' ';
            if (!ok) {
                log("ios.facebook.version '" + hint + "' is not a CocoaPods version "
                        + "requirement; using " + fallback);
                return fallback;
            }
        }
        return hint;
    }



    @Override
    protected String hardeningPlatform(BuildRequest request) {
        // The native-Mac target sets macNative.enabled=true (BuildMacNativeMojo / CN1BuildMojo), so
        // a build producing a Mac slice reports "mac" and honors harden.mac.enabled. The shared
        // application jar is hardened once, so a combined build hardens the Mac output under "mac".
        if ("true".equals(request.getArg("macNative.enabled", "false"))) {
            return "mac";
        }
        return "ios";
    }

    /**
     * Every Apple slice this one build ships from the SAME hardened jar: the iOS app plus any native-Mac,
     * watchOS or tvOS target. hardeningPlatform() reports one tag ("mac" for a combined build), so the
     * engine reads a single harden.<tag>.enabled; listing every slice here lets each target's
     * harden.<platform>.enabled participate in the combined opt-out (Executor.writeHardeningConfig hardens
     * unless EVERY slice is opted out). The slices cannot be hardened independently -- there is one binary.
     * Reads the RAW hints, because this runs in the shared Executor before the slice builders' parseHints.
     */
    @Override
    protected java.util.List<String> effectiveHardeningPlatforms(BuildRequest request) {
        return appleHardeningSlices(request);
    }

    /** The Apple slices a build ships from the shared hardened jar: ios plus any mac/watch/tv target. */
    static java.util.List<String> appleHardeningSlices(BuildRequest request) {
        java.util.List<String> platforms = new java.util.ArrayList<String>();
        platforms.add("ios");
        if ("true".equals(request.getArg("macNative.enabled", "false"))) {
            platforms.add("mac");
        }
        if (watchTargetEnabled(request)) {
            platforms.add("watch");
        }
        if (tvTargetEnabled(request)) {
            platforms.add("tv");
        }
        return platforms;
    }

    /** True when this build ships a tvOS slice (tvNative.enabled or a tvMain entry point). */
    static boolean tvTargetEnabled(BuildRequest request) {
        return "true".equals(request.getArg("tvNative.enabled", "false"))
                || request.getArg("tvMain",
                        request.getArg("tvNative.mainClass", "")).trim().length() > 0;
    }

    /**
     * The watch lifecycle entry class is resolved by its ORIGINAL fully-qualified name at run time --
     * {@code CN1WatchBootstrap} embeds it in {@code cn1_watch_runtime_start("<watchMain>")} -- and that
     * request-only string is not a reference the input-jar scanner can discover. When the watch ships a
     * distinct entry class, keep it so the rename doesn't leave the watch runtime looking up a name that
     * no longer exists. (A shared entry is the phone main class, already kept as {@code cn1.mainClass};
     * the tvOS target boots through the translated main-class symbol, not a by-name lookup.)
     */
    @Override
    protected java.util.List<String> extraKeepClasses(BuildRequest request) {
        return watchEntryKeepClasses(request);
    }

    /** True when this build ships a watchOS slice (watchNative.enabled or a watchMain entry point). */
    static boolean watchTargetEnabled(BuildRequest request) {
        return "true".equals(request.getArg("watchNative.enabled", "false"))
                || request.getArg("watchMain",
                        request.getArg("watchNative.mainClass", "")).trim().length() > 0;
    }

    /** The distinct watch entry class to keep (fully qualified), or empty when it shares the main class. */
    static java.util.List<String> watchEntryKeepClasses(BuildRequest request) {
        if (!watchTargetEnabled(request)) {
            return java.util.Collections.emptyList();
        }
        String watchMain = request.getArg("watchMain",
                request.getArg("watchNative.mainClass", "")).trim();
        String main = request.getMainClass() == null ? "" : request.getMainClass().trim();
        if (watchMain.length() == 0 || watchMain.equals(main)) {
            return java.util.Collections.emptyList();
        }
        if (watchMain.indexOf('.') < 0) {
            String pkg = request.getPackageName();
            if (pkg != null && pkg.trim().length() > 0) {
                watchMain = pkg.trim() + "." + watchMain;
            }
        }
        return java.util.Collections.singletonList(watchMain);
    }

    @Override
    public boolean build(File sourceZip, BuildRequest request) throws BuildException {
        // Builder instances are normally single-use, but keep scan-derived
        // native feature state deterministic if an instance is reused.
        usesCn1Vision = false;
        usesCn1Language = false;
        usesCn1Inference = false;
        Stopwatch stopwatch = new Stopwatch();
        addMinDeploymentTarget(DEFAULT_MIN_DEPLOYMENT_VERSION);
        if (request.getArg("ios.deployment_target", null) == null) {
            // No explicit deployment target. Default to iOS 14 so the
            // UILaunchScreen-based launch screen injected for UIScene builds
            // satisfies App Store validation: apps supporting iPad
            // multitasking must provide a launch storyboard, or UILaunchScreen
            // when MinimumOSVersion is 14 or higher.
            addMinDeploymentTarget("14.0");
        }
        detectJailbreak = request.getArg("ios.detectJailbreak", "false").equals("true");
        appAttest = request.getArg("ios.appAttest", "false").equals("true");
        defaultEnvironment.put("LANG", "en_US.UTF-8");
        tmpFile = tmpDir = getBuildDirectory();
        useMetal = "true".equals(request.getArg("ios.metal", "true"));

        // macNative: extend this iOS build to also produce a native Mac slice.
        // All Mac-specific work is delegated to MacNativeBuilder; this builder
        // only flips a few iOS-side knobs (Metal forced on, minimum deployment
        // target floor, Ruby xcodeproj gem required) when Mac is enabled.
        macNativeBuilder.parseHints(request);
        if (macNativeBuilder.isEnabled()) {
            // The Mac slice cannot link OpenGL ES; force Metal on regardless of
            // the ios.metal hint. (Already on by default now, but defensive.)
            useMetal = true;
            // Catalyst requires iOS 13.1+ -> macOS 10.15+.
            addMinDeploymentTarget(macNativeBuilder.getIosMinDeploymentTarget());
            // Mac requires the iPad device family. iphone-only is incompatible.
            macNativeBuilder.validateProjectType(request);
            // Ruby + xcodeproj gem is unconditionally required for the Mac slice.
            ensureXcodeprojInstalled();
        }

        // watchNative: parse + prep. The watch app is a separate target (not a
        // slice), built from the shared sources for arm64_32 and rendered via
        // Core Graphics, so no iOS-side renderer knobs change here -- we only
        // need the xcodeproj gem to add and wire the target post-generate.
        watchNativeBuilder.parseHints(request);
        if (watchNativeBuilder.isEnabled()) {
            ensureXcodeprojInstalled();
        }

        // tvNative: parse + prep. The tvOS app is a SEPARATE appletvos target
        // (like the watch target, not a Catalyst-style slice of the iOS app), so
        // we must NOT touch the iOS app's renderer here -- forcing useMetal=true
        // would override an explicit ios.metal=false and make the GL screenshot
        // job actually render with Metal. tvOS itself has no OpenGL ES and runs
        // on Metal via the project's default ios.metal=true; the tvOS target's
        // own Xcode settings are written by tvNativeBuilder.applyXcodeSettings.
        // We only need the xcodeproj gem to add and wire the target.
        tvNativeBuilder.parseHints(request);
        if (tvNativeBuilder.isEnabled()) {
            ensureXcodeprojInstalled();
        }

        log("Request Args: ");
        log("-----------------");
        for (String arg : request.getArgs()) {
            log(arg+"="+request.getArg(arg, null));
        }
        log("-------------------");


        buildVersion = request.getVersion();
        if(request.getArg("ios.twoDigitVersion", "false").equals("true")) {
            try {
                float version = Float.parseFloat(buildVersion);
                int intVersion = Math.round(100 * version);
                int lsb = intVersion % 100;
                buildVersion = "" + (intVersion / 100) + ".";
                if(lsb == 0) {
                    buildVersion += "00";
                } else {
                    if(lsb < 10) {
                        buildVersion += "0" + lsb;
                    } else {
                        buildVersion += lsb;
                    }
                }
            } catch(Exception err) {
            }
        }
        
        for (String arg : request.getArgs()) {
            if (arg.startsWith("ios.NS") && arg.endsWith("UsageDescription")) {
                if (arg.toUpperCase().contains("PHOTOLIBRARY")) {
                    photoLibraryUsage = true;
                }
                privacyUsageDescriptions.put(arg.substring(arg.lastIndexOf(".")+1), request.getArg(arg, null));
            }
        }

        String xcodebuild;
        String iosPods = request.getArg("ios.pods", "");
        enableGalleryMultiselect = "true".equals(request.getArg("ios.enableGalleryMultiselect", "false"));
        if (enableGalleryMultiselect) {
            if (!iosPods.contains("QBImagePickerController") && photoLibraryUsage) {
                if (!iosPods.endsWith(",")) {
                    iosPods += ",";
                }
                iosPods += "QBImagePickerController ~> 3.4";
                addMinDeploymentTarget("8.0");
            }
        }
        usePhotoKitForMultigallery = "true".equals(request.getArg("ios.usePhotoKitForMultigallery", "false"));

        enableWKWebView = "true".equals(request.getArg("ios.useWKWebView", "true"));
        if (enableWKWebView) {
            addMinDeploymentTarget("8.0");
        }
        disableUIWebView = enableWKWebView && "true".equals(request.getArg("ios.noUIWebView", "true"));

        boolean bicodeHandle = true;
        xcodebuild = resolveXcodebuild();
        xcodeVersion = getXcodeVersion(xcodebuild);
        if (xcodeVersion <= 0) {
            xcodeVersion = 10;
        }

        String facebookAppId = request.getArg("facebook.appId", null);
        boolean usePodsForFacebook = !request.getArg("ios.facebook.usePods", "true").equals("false") && facebookAppId != null && facebookAppId.length() > 0;
        if (usePodsForFacebook) {
            addMinDeploymentTarget("10.0");
            iosPods += (((iosPods.length() > 0) ? ",":"") + facebookPods(request));
        }

        String googleAdUnitId = request.getArg("ios.googleAdUnitId", request.getArg("google.adUnitId", null));
        boolean usePodsForGoogleAds = googleAdUnitId != null && googleAdUnitId.length() > 0;
        if (usePodsForGoogleAds) {
            iosPods += (((iosPods.length() > 0) ? ",":"") + "Firebase/Core,Firebase/AdMob");
            addMinDeploymentTarget("7.0");
        }

        // Firebase Analytics (com.codename1.analytics.FirebaseAnalyticsProvider
        // delegates to a generated FirebaseAnalyticsProvider.Bridge). Enabled
        // with the build hint ios.firebaseAnalytics=true; requires a
        // GoogleService-Info.plist in the project resources. Adds the
        // Firebase/Analytics pod (skipped if Firebase/Core was already pulled
        // in by AdMob, which carries Analytics transitively).
        boolean useFirebaseAnalytics = "true".equals(request.getArg("ios.firebaseAnalytics", "false"));
        if (useFirebaseAnalytics && !iosPods.contains("Firebase/")) {
            String fbAnalyticsVersion = request.getArg("ios.firebaseAnalyticsVersion", "");
            iosPods += (((iosPods.length() > 0) ? ",":"") + "Firebase/Analytics"
                    + (fbAnalyticsVersion.length() > 0 ? " " + fbAnalyticsVersion : ""));
            addMinDeploymentTarget("10.0");
        }
        if (enableGalleryMultiselect && photoLibraryUsage) {
            addMinDeploymentTarget("8.0");
        }
        if (enableWKWebView) {
            addMinDeploymentTarget("8.0");
        }

        IOSDependencyConfig dependencyConfig = IOSDependencyManager.resolve(request, iosPods);
        iosPods = dependencyConfig.iosPods;
        runPods = dependencyConfig.usesCocoaPods();
        runSpm = dependencyConfig.usesSwiftPackages();
        if (runPods) {
            ensurePodsInstalled();
        }
        if (runSpm) {
            ensureXcodeprojInstalled();
        }

        debug("Xcode version is "+xcodeVersion);
        // ios.themeMode stays the platform-specific knob; nativeTheme is
        // the cross-platform meta hint. modern / legacy on the meta hint
        // translate to the equivalent iOS values when ios.themeMode is unset.
        // cn1.nativeTheme is honored as a deprecated alias for nativeTheme.
        String iosMode = request.getArg("ios.themeMode", null);
        if (iosMode == null) {
            String sharedMode = request.getArg("nativeTheme",
                    request.getArg("cn1.nativeTheme", null));
            if ("legacy".equalsIgnoreCase(sharedMode)) {
                iosMode = "ios7";
            } else if ("modern".equalsIgnoreCase(sharedMode)) {
                iosMode = "modern";
            } else {
                iosMode = "auto";
            }
        }
        
        tmpFile = getBuildDirectory();
        if (tmpFile == null) {
            throw new IllegalStateException("Build directory must be set before running build.");
        }
        if (tmpFile.exists()) {
            delTree(tmpFile);
        }
        tmpFile.mkdirs();

        File classesDir = new File(tmpFile, "classes");
        classesDir.mkdirs();
        File resDir = new File(tmpFile, "res");
        resDir.mkdirs();
        File buildinRes = new File(tmpFile, "btres");
        buildinRes.mkdirs();

        // fill classes dir from JAR and proper ports
        try {
            unzip(sourceZip, classesDir, resDir, resDir, buildinRes);
        } catch (IOException ex) {
            throw new BuildException("Failed to unzip source Zip file.", ex);
        }
        stopwatch.split("Setup & Unzip");
        
        
        // We allow devs to add local podspecs inside a folder called "podspecs".  This will
        // be tarred by unzip() into a file named podspecs.tar so that folder hierarchies can be preserved
        // We must now go through and extract this tar file into a separate directory so that we can copy them
        // into the project folder after ByteCodeTranslator has created the Xcode project.
        
        // Before anything walks the resources: an .ios.appext is unpacked much later, once the
        // Xcode project exists, but it has to leave resDir now. See stageAppExtensionArchives.
        try {
            appExtensionArchiveDir = stageAppExtensionArchives(resDir, new File(tmpFile, "appext"));
        } catch (IOException ex) {
            throw new BuildException("Failed to stage the app extension archives out of the "
                    + "resources directory", ex);
        }

        // Look for frameworks and localized strings
        Set<String> variantGroups = new HashSet<String>();
        for (File child : resDir.listFiles()) {
            if (child.getName().endsWith(".lproj.zip")) {
                // This is a zipped lproj directory that contains localized strings.
                // We need to extract this and add the localized files to the project.

                String languageBase = child.getName().substring(0, child.getName().lastIndexOf(".lproj.zip"));
                File languageDir = new File(new File(tmpDir, "dist"), languageBase+".lproj");
                if (languageDir.exists()) {
                    delTree(languageDir, true);

                }
                languageDir.mkdirs();

                log("Found native strings directory "+child+". Attempting extract it and add it to the project");
                try {
                    if (!exec(resDir, "unzip", child.getName(), "-d", languageDir.getAbsolutePath())) {
                        log("Failed to unzip " + child.getName());
                        return false;
                    }
                } catch (Exception ex) {
                    throw new BuildException("Failed to extract bundled strings directory "+child, ex);
                }

                if (languageDir.exists()) {
                    // Sometmes files are zipped with the language dir as a directory within the root.  We need to detect this
                    // case and fix it.
                    File nestedLanguageDir = new File(languageDir, languageDir.getName());
                    if (nestedLanguageDir.exists() && nestedLanguageDir.isDirectory()) {
                        for (File nestedStringsFile : nestedLanguageDir.listFiles()) {
                            if (!nestedStringsFile.getName().endsWith(".strings")) {
                                continue;
                            }
                            File destStringsFile = new File(languageDir, nestedStringsFile.getName());
                            try {

                                Files.copy(nestedStringsFile.toPath(), destStringsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException ex) {
                                log("Failed to reparent nested strings file "+nestedStringsFile+" to "+destStringsFile);
                            }
                        }
                        delTree(nestedLanguageDir, true);
                    }

                }

                if (!languageDir.exists()) {
                    log("Cannot find localization directory "+languageDir+" after extracting "+child+".  Please ensure that the localization file is located in the top level of the zip file.");
                    return false;
                }

                // Create the Ruby xcodeproj script that will add the strings files to the project
                for (File stringsFile : languageDir.listFiles()) {
                    if (!stringsFile.getName().endsWith(".strings")) {
                        // We only care about strings files.
                        continue;
                    }
                    if (installLocalizedStringsScript.length() == 0) {
                        installLocalizedStringsScript.append("variant_groups={}\n");
                    }
                    if (!variantGroups.contains(stringsFile.getName())) {
                        variantGroups.add(stringsFile.getName());
                        installLocalizedStringsScript.append("variant_group = xcproj.main_group.new_variant_group('").append(stringsFile.getName()).append("')\n");
                        installLocalizedStringsScript.append("variant_groups['").append(stringsFile.getName()).append("'] = variant_group\n");
                        //installLocalizedStringsScript.append("xcproj.targets.find{|e|e.name=='").append(request.getMainClass()).append("'}.add_file_reference(variant_group)\n");
                    }
                    installLocalizedStringsScript.append("fileref = variant_groups['").append(stringsFile.getName()).append("'].new_file('").append(languageDir.getName()).append("/").append(stringsFile.getName()).append("')\n");
                    installLocalizedStringsScript.append("xcproj.targets.each{|e| e.add_resources([fileref])}\n");
                }


                child.delete();


            }
            if (child.getName().endsWith(".framework.zip")) {
                log("Found framework "+child+". Attempting extract it and generate podspec for it");
                try {
                    if (!exec(resDir, "ditto", "-x", "-k", child.getAbsolutePath(), new File(tmpDir, "dist").getAbsolutePath())) {
                        log("Failed to unzip " + child.getName());
                        return false;
                    }
                } catch (Exception ex) {
                    throw new BuildException("Failed to extract bundled framework "+child, ex);
                }
                String frameworkBase = child.getName().substring(0, child.getName().lastIndexOf(".framework.zip"));
                File frameworkFile = new File(new File(tmpDir, "dist"), frameworkBase+".framework");
                if (!frameworkFile.exists()) {
                    log("Cannot find framework file "+frameworkFile+" after extracting "+child+".  Please ensure that the framework is located in the top level of the zip file.");
                    return false;
                }
                
                File podspecFile = new File(resDir, frameworkBase+".podspec");
                StringBuilder podspecContents = new StringBuilder()
                        .append("Pod::Spec.new do |s|\n" +
                        "  s.name                    = \""+frameworkBase+"\"\n" +
                        "  s.version                 = \"1.0.0\"\n" +
                        "  s.summary                 = \""+frameworkBase+" framework\"\n" +
                        "  s.description             = \"This spec specifies a vendored framework.\"\n" +
                        "  s.platform                = :ios\n" +
                        "  s.homepage                = \"https://www.codenameone.com\"\n" +
                        "  s.source                  = {:path => \".\"}\n" +
                        "  s.author                  = \"Codename One\"\n" +
                        "  s.vendored_frameworks     = \""+frameworkBase+".framework\"\n" +
                        "end");
                log("Writing podspec "+podspecFile+" with contents:\n"+podspecContents.toString());
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(podspecFile);
                    fos.write(podspecContents.toString().getBytes("UTF-8"));
                } catch (IOException ex) {
                    throw new BuildException("Failed to write th podxspec file for bundled framework "+child, ex);
                } finally {
                    if (fos != null) {
                        try {fos.close();} catch (Throwable t){}
                    }
                }
                child.delete();
                
                iosPods = append(iosPods, ",", frameworkBase);
                
                
            }
        }
        stopwatch.split("Extract Extensions");
        
        File podSpecs = new File(tmpFile, "podspecs");
        podSpecs.mkdirs();
        try {
            for (File dir : new File[]{classesDir, resDir, buildinRes}) {
                for (File child : dir.listFiles()) {
                    if (child.getName().endsWith(".podspec")) {
                        Files.move(child.toPath(), new File(podSpecs, child.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                    if ("podspecs.tar".equals(child.getName())) {
                        if (!exec(tmpFile, "tar", "xvf", child.getAbsolutePath(), "-C", podSpecs.getAbsolutePath())) {
                            log("Failed to extract podspecs tar file " + child.getAbsolutePath() + " to podspecs dir " + podSpecs.getAbsolutePath());
                            return false;
                        }

                        child.delete();
                    }

                }
            }
        } catch (Exception ex) {
            throw new BuildException("An error occurred while attempting to install bundled podspecs", ex);
        }
        
        File googleServicePlistFile = new File(resDir, "GoogleService-Info.plist");
        String googleClientId = null;
        boolean useGoogleSignIn = false;
        if (googleServicePlistFile.exists()) {
            googleServicePlist = new GoogleServicePlist();
            
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            Document doc;
            try {
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                doc = dBuilder.parse(googleServicePlistFile);
            } catch (Exception ex) {
                throw new BuildException("Failed to parse google services Plist File", ex);
            }
            Element dict = (Element)doc.getElementsByTagName("dict").item(0);
            NodeList childNodes = dict.getChildNodes();
            int len = childNodes.getLength();
            for (int i=0; i<len; i++) {
                Node n = childNodes.item(i);
                if (n instanceof Element) {
                    Element e = (Element)n;
                    if ("key".equals(e.getTagName()) && "CLIENT_ID".equals(e.getTextContent().trim())) {
                        Element nextEl = getNextElement(childNodes, i);
                        
                        if (nextEl != null && "string".equals(nextEl.getTagName())) {
                            googleClientId = nextEl.getTextContent().trim();
                            googleServicePlist.clientId = googleClientId;
                        }
                    } else if ("key".equals(e.getTagName()) && "BUNDLE_ID".equals(e.getTextContent().trim())) {
                        Element nextEl = getNextElement(childNodes, i);
                        if (nextEl != null && "string".equals(nextEl.getTagName())) {
                            String bid = nextEl.getTextContent().trim();
                            if (bid == null || !bid.equals(request.getPackageName())) {
                                debug("Bundle ID="+request.getPackageName()+"; GoogleService BUNDLE_ID="+bid);
                                debug("GoogleService-Info.plist file bundle ID does not match the App ID.  See "+GOOGLE_SIGNIN_TUTORIAL_URL+" for instructions on setting up GoogleSignIn");
                                log("GoogleService-Info.plist file bundle ID does not match the App ID.  See "+GOOGLE_SIGNIN_TUTORIAL_URL+" for instructions on setting up GoogleSignIn");
                                
                                return false;
                            }
                        }
                    } else if ("key".equals(e.getTagName()) && "IS_SIGNIN_ENABLED".equals(e.getTextContent().trim())) {
                        Element nextEl = getNextElement(childNodes, i);
                        if ("true".equals(nextEl.getTagName())) {
                            useGoogleSignIn = true;
                            googleServicePlist.useSignIn = true;
                        }
                    } else if ("key".equals(e.getTagName()) && "REVERSED_CLIENT_ID".equals(e.getTextContent().trim())) {
                        Element nextEl = getNextElement(childNodes, i);
                        
                        if (nextEl != null && "string".equals(nextEl.getTagName())) {
                            //googleClientId = nextEl.getTextContent().trim();
                            googleServicePlist.reverseClientId = nextEl.getTextContent().trim();
                        }
                    }
                }
            }
            
            
            
        }
        stopwatch.split("Google Services Setup");
        
        if (googleClientId == null && useGoogleSignIn) {
            log("GoogleService-Info.plist file specifies that GoogleSignIn should be used but it doesn't provide a client ID.  Likely the GoogleService-Info.plist file is not valid.  See "+GOOGLE_SIGNIN_TUTORIAL_URL+" for instructions on setting up GoogleSignIn");
            error("Fail 2", new RuntimeException("Need to provide GoogleService-Info.plist file"));
            return false;
        }
        if (googleClientId == null) {
            googleClientId = request.getArg("ios.gplus.clientId", null);
            if (googleClientId != null) {
                useGoogleSignIn = true;
            }
        }
        
                
        if (useGoogleSignIn) {
            // 5.0.x shipped a vendored fat framework whose only arm64 slice is
            // built for the device (LC_VERSION_MIN_IPHONEOS); its simulator
            // slices are i386 and x86_64. On an Apple Silicon Mac the linker
            // therefore picked the device slice for an arm64 simulator build
            // and refused it, so no app that enables Google sign-in could run
            // in the simulator at all. 7.x is distributed as source, so
            // CocoaPods compiles a slice for whichever platform is being built.
            //
            // 8.x and 9.x add an AppCheckCore dependency, which is Swift and
            // depends in turn on two pods that define no module map. Adopting
            // either would require use_modular_headers! in every generated
            // Podfile, changing pod resolution for projects that have nothing
            // to do with Google sign-in. 7.1 is the newest release that drops
            // in without that.
            iosPods += (((iosPods.length() > 0) ? ",":"") + GOOGLE_SIGNIN_POD);
            // GoogleSignIn 7.1's own floor is iOS 10, but Xcode 26 accepts
            // deployment targets no lower than 12.0.
            addMinDeploymentTarget(GOOGLE_SIGNIN_MIN_IOS);
        }
        
        // Accumulator for AI/ML class hits. After the scan we apply
        // every matched PlatformFeatureCatalog.Entry -- appending pods,
        // SPM specs, plist defaults and Android perms -- so the user
        // doesn't have to declare them by hand.
        final PlatformFeatureCatalog.Accumulator aiAcc = new PlatformFeatureCatalog.Accumulator();
        boolean excludeArm64Simulator = false;

        // Attributed to the class that makes the reference, so the framework's own use of the
        // database does not answer for the application's. Both payloads this gates are large --
        // the cipher amalgamation replaces the system libsqlite3 -- so an application that never
        // touches com.codename1.db must not carry either.
        try {
            // The libraries as well as the loose classes. unzip() routes a submitted .jar to the
            // libs directory, which is btres here, and ParparVM translates it alongside the
            // application -- so an application whose dependency is the only thing that calls
            // DatabaseConfig scanned as using no database at all, and the build then linked the
            // system SQLite and left the library's encrypted open failing as unsupported.
            //
            // Read before the port's own jars are unzipped into btres further down, which is what
            // keeps the framework's use of the database from answering for the application's. The
            // scan filters the framework's classes by name as well, in both trees.
            DatabaseUsage databaseUsage = scanForDatabaseUsage(classesDir)
                    .merge(scanForDatabaseUsage(buildinRes));
            usesDatabase = databaseUsage.usesDatabase();
            usesDatabaseCipher = databaseUsage.usesDatabaseCipher();
        } catch (IOException ex) {
            throw new BuildException("Failed to scan for database usage", ex);
        }

        try {
            scanClassesForPermissions(classesDir, new Executor.ClassScanner() {
                // iOS has no OS-relaunch delivery, but it does have cold
                // launches, and that is enough to need these. A restored
                // subscription carries only the listener's class *name*;
                // without generated bindings the runtime cannot turn that
                // back into an instance, so even a manual drainChanges()
                // after a restart delivered nothing.
                @Override
                public void implementsInterface(String cls, String iface) {
                    healthScan.implementsInterface(cls, iface);
                }

                @Override
                public void declaresEnclosedBy(String cls, String outer) {
                    healthScan.declaresEnclosedBy(cls, outer);
                }

                @Override
                public void declaresPublicType(String cls) {
                    healthScan.declaresPublicType(cls);
                }

                @Override
                public void declaresConcreteType(String cls) {
                    healthScan.declaresConcreteType(cls);
                }

                @Override
                public void declaresType(String cls, String superName,
                        boolean isConcrete) {
                    healthScan.declaresType(cls, superName, isConcrete);
                }

                @Override
                public void usesClass(String cls) {
                    if (cls == null) return;
                    aiAcc.consume(cls);
                    if (cls.indexOf("com/codename1/calendar/LocalCalendarSource") == 0) {
                        usesCalendarApi = true;
                    }
                    if (!usesLocalNotifications && cls.indexOf("com/codename1/notifications/LocalNotification") == 0) {
                        usesLocalNotifications = true;
                    }
                    if (!usesPurchaseAPI && cls.indexOf("com/codename1/payment") == 0) {
                        usesPurchaseAPI = true;
                    }
                    // App review API (SKStoreReviewController). Gated on actual
                    // usage so StoreKit.framework + the CN1_USE_APPREVIEW native
                    // bridge are only linked when the app references the API.
                    if (!usesAppReview && cls.indexOf("com/codename1/appreview") == 0) {
                        usesAppReview = true;
                    }
                    // Wallet issuer-provisioning natives are only compiled in when
                    // the app actually references the API (or enables the extension
                    // via the ios.wallet.extension hint) - see CN1_INCLUDE_WALLET.
                    if (!usesWalletApi && cls.indexOf("com/codename1/payment/Wallet") == 0) {
                        usesWalletApi = true;
                    }
                    if (cls.indexOf("com/codename1/security/") == 0) {
                        // com.codename1.security contains two distinct API
                        // families that toggle different bits of the iOS
                        // build. Biometrics + SecureStorage need the
                        // LocalAuthentication.framework linkage; the crypto
                        // primitives need the CN1Crypto.{h,m} #defines and
                        // an Info.plist export-compliance entry.
                        String shortName = cls.substring("com/codename1/security/".length());
                        boolean isBiometric =
                                shortName.startsWith("Biometric")
                             || shortName.equals("SecureStorage")
                             || shortName.equals("AuthenticationOptions");
                        if (isBiometric) {
                            usesBiometrics = true;
                        } else {
                            usesCryptoAPI = true;
                        }
                    }
                    // Deliberately not scanned here. This callback cannot say which class made
                    // the reference, and the tree it walks is the application merged with the
                    // framework, where Display alone carries openOrCreate(String, DatabaseConfig)
                    // -- so both database gates would answer yes for every application ever built.
                    // scanForDatabaseUsage below attributes the reference instead.
                    if (!usesNfc && cls.indexOf("com/codename1/nfc/") == 0) {
                        usesNfc = true;
                        if (cls.equals("com/codename1/nfc/HostCardEmulationService")) {
                            usesNfcHce = true;
                        }
                    }
                    // First-class Bluetooth (com.codename1.bluetooth.*).
                    // Gated on actual usage so CoreBluetooth and the
                    // CN1Bluetooth natives are only linked/compiled for apps
                    // that reference the API. The peripheral flag keys on
                    // the permission-aligned le/server/ package so the
                    // bluetooth-peripheral background mode is only ever
                    // offered to apps that actually advertise.
                    if (cls.indexOf("com/codename1/bluetooth/") == 0) {
                        usesBluetooth = true;
                        if (cls.indexOf("com/codename1/bluetooth/le/server/") == 0) {
                            usesBluetoothPeripheral = true;
                        }
                    }
                    // First-class health (com.codename1.health.*). The
                    // store flag is what gates HealthKit, its entitlement
                    // and the privacy-string requirement; the sensors
                    // subpackage is ordinary BLE and must not trigger any
                    // of that.
                    if (cls.indexOf("com/codename1/health/") == 0) {
                        usesHealth = true;
                        // The facade itself is not evidence of the store:
                        // the documented sensor-only flow is
                        // Health.getInstance().getSensors(), which the
                        // scanner reports with com/codename1/health/Health
                        // as the owner. Treating that as store usage failed
                        // the build for BLE-only apps over Health Connect
                        // hints they have no use for. The usesClassMethod
                        // hook below decides for the facade.
                        // The value types are exempt for the same reason the
                        // sensors package is: a sensor callback is handed a
                        // HealthSample and reading a number off it names
                        // QuantitySample, so counting those as store use linked
                        // HealthKit into a BLE-only binary and put it through
                        // health processing it never asked for.
                        if (cls.indexOf("com/codename1/health/sensors/") != 0
                                && !"com/codename1/health/Health".equals(cls)
                                && !isSharedHealthModel(cls)) {
                            usesHealthStore = true;
                        }
                        if (cls.indexOf("com/codename1/health/workout/") == 0) {
                            usesHealthWorkout = true;
                            // The update string, matching the getWorkouts()
                            // hook. usesHealthWorkout alone drove nothing,
                            // and a workout-only app shipped without any
                            // purpose string and was refused when it asked
                            // HealthKit for access.
                            //
                            // Not the share string: naming
                            // WorkoutConfiguration says no more about
                            // reading than calling getWorkouts() does, and
                            // nothing in the package reads.
                            usesHealthWrite = true;
                        }
                    }
                    // Low-level camera API (com.codename1.camera.*). Gated on
                    // actual usage -- NOT on the camera privacy description --
                    // so the old modal Capture API (which only sets
                    // INCLUDE_CAMERA_USAGE) does not pull in the new
                    // AVFoundation-based CN1Camera natives.
                    if (!usesCn1Camera
                            && (cls.indexOf("com/codename1/camera/") == 0
                                || isCameraBackedVisionClass(cls))) {
                        usesCn1Camera = true;
                    }
                    // Augmented reality (com.codename1.ar.*). Gated on actual
                    // usage so ARKit/SceneKit and the CN1AR natives are only
                    // built for apps that reference the AR API.
                    if (!usesCn1Ar && cls.indexOf("com/codename1/ar/") == 0) {
                        usesCn1Ar = true;
                    }
                    if (!usesCn1Vision && isVisionAnalyzerClass(cls)) {
                        usesCn1Vision = true;
                    }
                    if (!usesCn1Language && isLanguageFeatureClass(cls)) {
                        usesCn1Language = true;
                    }
                    if (!usesCn1Inference
                            && "com/codename1/ai/inference/InferenceSession".equals(cls)) {
                        usesCn1Inference = true;
                    }
                    // Apple CarPlay (com.codename1.car.*). Gated on actual usage so the
                    // CarPlay scene/entitlement/framework are only added for apps that
                    // build an in-car experience.
                    if (!usesCar && cls.indexOf("com/codename1/car/") == 0) {
                        usesCar = true;
                    }
                    // Smart home (com.codename1.home.*). Gated on actual usage so
                    // HomeKit, its entitlement and the CN1SmartHome natives are only
                    // added for apps that reference the API.
                    if (cls.indexOf("com/codename1/home/") == 0
                            && !isSmartHomeSetupPayload(cls)) {
                        usesSmartHome = true;
                        if (cls.indexOf("com/codename1/home/commissioning/") == 0
                                && !SmartHomeManifestFragments
                                        .isCommissioningCapabilityType(cls)) {
                            // Commissioner and CommissioningStyle are left out: an
                            // app asking whether it COULD add an accessory names
                            // both and may never add one, and the answer used to
                            // cost it MatterSupport, a restricted entitlement, an
                            // app group, an extension target and a raised floor.
                            // The Commissioner call decides it instead, below.
                            usesHomeCommissioning = true;
                            // The entitlement commissioning earns is applied
                            // below rather than here, because it is only
                            // earned when commissioning is actually built:
                            // ios.home.commissioning=false is not known yet
                            // at scan time, and an app that set it would
                            // otherwise be made to declare a HomeKit purpose
                            // string and carry a restricted entitlement for
                            // machinery it explicitly turned off.
                        } else if (!"com/codename1/home/SmartHome".equals(cls)
                                && !isSmartHomeAvailabilityType(cls)
                                && !SmartHomeManifestFragments
                                        .isCommissioningCapabilityType(cls)) {
                            // Naming any type beyond the facade and the
                            // capability enums means the app is working with
                            // accessories. The facade alone is decided by the
                            // usesClassMethod hook below, because
                            // SmartHome.getAvailability() and SmartHome.read()
                            // are the same class reference and only one of
                            // them needs an entitlement.
                            usesHomeAccessoryData = true;
                        }
                    }
                    // External surfaces (com.codename1.surfaces.*): home-screen widgets
                    // and live activities. Gated on actual usage so the CN1Widgets
                    // extension / app group / CN1_USE_WIDGETS natives are only added for
                    // apps that publish external surfaces.
                    if (!usesSurfaces && cls.indexOf("com/codename1/surfaces/") == 0) {
                        usesSurfaces = true;
                    }
                    // Phone-to-watch link (com.codename1.wearable.*). Gated on actual usage
                    // so WatchConnectivity.framework and the CN1_USE_WATCHCONNECTIVITY
                    // natives are only added for apps that talk to their watch app.
                    if (!usesWearable && cls.indexOf("com/codename1/wearable/") == 0) {
                        usesWearable = true;
                    }
                    // App intents (com.codename1.intents.*). Gated on actual usage so
                    // CoreSpotlight.framework, the CN1_USE_INTENTS natives and the generated
                    // Swift declarations are only added for apps that expose something to
                    // Siri or device search.
                    if (!usesIntents && cls.indexOf("com/codename1/intents/") == 0) {
                        usesIntents = true;
                    }
                    // OidcClient + SystemBrowser rely on
                    // ASWebAuthenticationSession (AuthenticationServices.framework,
                    // iOS 12+).
                    if (!usesOidc && cls.indexOf("com/codename1/io/oidc/") == 0) {
                        usesOidc = true;
                    }
                    // Sign in with Apple (ASAuthorizationAppleIDProvider) lives
                    // in the same framework and only matters when the user
                    // actually references AppleSignIn.
                    if (!usesAppleSignIn
                            && cls.indexOf("com/codename1/social/AppleSignIn") == 0) {
                        usesAppleSignIn = true;
                    }
                    // WebAuthn / passkeys (ASAuthorizationPlatformPublicKeyCredentialProvider)
                    // also lives in AuthenticationServices.framework. Same gate
                    // strategy: only enable the native bridge when the app
                    // references com.codename1.io.webauthn.*
                    if (!usesWebauthn
                            && cls.indexOf("com/codename1/io/webauthn/") == 0) {
                        usesWebauthn = true;
                    }
                    if (cls.indexOf("com/codename1/io/wifi/WiFi") == 0
                            && !cls.equals("com/codename1/io/wifi/WiFiDirect")) {
                        // WiFi info or scan/connect. iOS has no scan API so
                        // the WiFi entitlement we inject is hotspot config +
                        // wifi-info; we treat any use as info-capable and
                        // upgrade to hotspot config only when scan/connect
                        // is referenced (detected via method scan below).
                        usesWifiInfo = true;
                    }
                    if (cls.indexOf("com/codename1/io/bonjour/") == 0) {
                        usesBonjour = true;
                    }
                    // WiFi Direct / USB on iOS: not supported. We
                    // intentionally do not inject entitlements -- the runtime
                    // stub returns "unsupported" at call time.
                }

                @Override
                public void usesClassMethodWithBooleanArgument(String cls,
                        String method, Boolean value) {
                    // Sensor write-through is HealthKit use. An app enables
                    // it with SensorSessionOptions.setWriteToStore(true)
                    // and need never name HealthStore, so the
                    // sensors-package exemption -- there so a BLE-only app
                    // is not linked against HealthKit -- hid the one call
                    // in that package that genuinely needs it, and the
                    // build omitted the framework, the entitlement and the
                    // purpose-string check.
                    //
                    // The argument decides it, which is why this is not in
                    // usesClassMethod: an explicit setWriteToStore(false)
                    // is a BLE-only app switching the store off, and
                    // reading it as store use linked that app against
                    // HealthKit and demanded purpose strings for data it
                    // had just declined to touch.
                    // Asking for a fabric of this app's is what turns the
                    // generated extension's commissioning implementation from
                    // commented-out scaffolding into live code, and it ships
                    // an operating-system Matter controller with it. The
                    // argument decides it for the same reason it does above:
                    // an explicit setCommissionToThisApp(false) is an app
                    // saying it does NOT want that, and reading it as a
                    // request would ship the controller to a build that had
                    // just declined it.
                    if ("com/codename1/home/commissioning/CommissioningRequest"
                            .equals(cls)
                            && "setCommissionToThisApp".equals(method)) {
                        if (value == null) {
                            // The argument is computed, so the build cannot
                            // tell what it will be. Collapsing that to "off"
                            // ships an app whose request is ignored; to "on"
                            // ships a Matter controller nobody asked for.
                            homeFabricAmbiguous = true;
                        } else if (value.booleanValue()) {
                            usesHomeOwnFabric = true;
                        } else {
                            usesHomeOwnFabricDeclined = true;
                        }
                    }
                    if (HealthManifestFragments.enablesSensorWriteThrough(
                            cls, method, value)) {
                        usesHealth = true;
                        usesHealthStore = true;
                        usesHealthWrite = true;
                        sensorWriteThrough = true;
                    }
                }


                @Override
                public void usesClassMethod(String cls, String method) {
                    // The catalog first: it decides frameworks and plist
                    // entries for every feature, health included, and is
                    // indifferent to what follows.
                    aiAcc.consumeMethod(cls, method);
                    // Health.getStore()/getWorkouts() mean a real platform
                    // store; Health.getSensors() means BLE only. The class
                    // reference alone cannot tell them apart, so the facade
                    // is decided here.
                    // SmartHome.read()/write()/refresh() touch the home;
                    // SmartHome.getAvailability() asks whether HomeKit exists and
                    // reads nothing. Both are the same class reference, so the
                    // entitlement decision has to be made here.
                    if ("com/codename1/home/commissioning/Commissioner"
                            .equals(cls)) {
                        // isSupported() and getStyle() ask whether this
                        // platform can add an accessory; anything else adds
                        // one. The class reference alone cannot tell them
                        // apart, and the difference is an entire generated
                        // extension target.
                        if (SmartHomeManifestFragments
                                .isCommissioningCall(method)) {
                            usesHomeCommissioning = true;
                        }
                    }
                    if ("com/codename1/home/SmartHome".equals(cls)) {
                        usesSmartHome = true;
                        if (SmartHomeManifestFragments.isAccessoryDataCall(method)) {
                            usesHomeAccessoryData = true;
                        }
                    }
                    if ("com/codename1/health/Health".equals(cls)) {
                        usesHealth = true;
                        if (method.startsWith("getStore")
                                || method.startsWith("getWorkouts")
                                || method.startsWith("openHealthSettings")
                                || method.startsWith("openProviderSetup")
                                || method.startsWith("getAvailability")
                                // The probes need the backend as much as a read
                                // does: isSupported() asks the Health Connect
                                // delegate whether it is there and hkIsAvailable()
                                // asks the native, and neither exists unless the
                                // build bundles them -- so an app whose only health
                                // call was "is this supported?" was told no on every
                                // device, for ever, because it had asked.
                                || method.startsWith("isSupported")
                                || method.startsWith("getConfigurationProblems")) {
                            usesHealthStore = true;
                        }
                        if (method.startsWith("getWorkouts")) {
                            usesHealthWorkout = true;
                            // The update string, as the Android hook demands the
                            // write direction. The class-reference branch sets this
                            // when a workout type is named, but an app that calls
                            // getWorkouts() and passes the facade around as Object
                            // never names one -- and was entitled for HealthKit with
                            // no string at all, so its first authorization request
                            // was refused.
                            //
                            // Not the share string: nothing in the workout package
                            // reads. The rollup is computed from the samples the app
                            // fed in, and end() writes them. Asking for a read
                            // purpose string an app cannot justify is the kind of
                            // over-declaration App Review pushes back on, and the
                            // build refused to proceed without it.
                            usesHealthWrite = true;
                        }
                        if (method.startsWith("getSensors")) {
                            // Same reason as Android: the sensor layer is
                            // built on the public bluetooth API, so an app
                            // that only calls getSensors() would otherwise
                            // ship without CoreBluetooth linked or
                            // CN1_INCLUDE_BLUETOOTH set.
                            usesBluetooth = true;
                        }
                    }
                    if (cls.indexOf("com/codename1/health/HealthStore") == 0) {
                        // Writing needs a separate privacy string from
                        // reading, and observers need a separate
                        // entitlement, so both are detected rather than
                        // assumed from mere health usage.
                        if (method.startsWith("write")
                                || method.startsWith("delete")) {
                            usesHealthWrite = true;
                        }
                        // Reading needs NSHealthShareUsageDescription and
                        // writing needs NSHealthUpdateUsageDescription.
                        // They are not interchangeable: iOS kills the app
                        // when it reads without the share string, so a
                        // read-only app that declared only the update
                        // string must not be waved through.
                        if (method.startsWith("read")
                                || method.startsWith("aggregate")
                                || method.startsWith("subscribe")
                                || method.startsWith("drainChanges")
                                || method.startsWith("hasAnyData")
                                || method.startsWith("requestAuthorization")) {
                            usesHealthRead = true;
                        }
                        // Deliberately not set from subscribe() or
                        // drainChanges(). Neither registers an
                        // HKObserverQuery -- IOSHealthStore.doDrainChanges
                        // polls with sample queries -- so the
                        // background-delivery entitlement they used to
                        // trigger bought nothing, while demanding a
                        // provisioning-profile capability that a
                        // polling-only app has no reason to hold. Getting
                        // that wrong fails codesign with an opaque
                        // message. The explicit build hint still turns it
                        // on, for an app that knows it wants it.
                        // requestAuthorization takes a HealthAccess list whose
                        // contents the scanner cannot see, and asking for any
                        // write access needs the update string. Requiring both
                        // descriptions is the conservative reading; the
                        // alternative is a build that passes and an app that
                        // iOS kills the moment it requests share access.
                        if (method.startsWith("requestAuthorization")) {
                            usesHealthWrite = true;
                        }
                    }
                    if (cls.indexOf("com/codename1/calendar/LocalCalendarSource") == 0
                            || (cls.indexOf("com/codename1/calendar/CalendarManager") == 0
                            && (method.indexOf("getLocalSource") >= 0
                            || method.indexOf("getSources") >= 0))
                            || (cls.indexOf("com/codename1/ui/Display") == 0
                            && method.indexOf("getLocalCalendarSource") >= 0)) {
                        usesCalendarApi = true;
                    }
                    if (cls.indexOf("com/codename1/calendar/CalendarSource") == 0
                            || cls.indexOf("com/codename1/calendar/LocalCalendarSource") == 0) {
                        if (method.indexOf("Task") >= 0 || method.indexOf("Tasks") >= 0) {
                            usesCalendarTaskApi = true;
                        }
                        if (method.indexOf("Event") >= 0 || method.indexOf("Events") >= 0
                                || method.indexOf("FreeBusy") >= 0
                                || method.indexOf("Invitation") >= 0) {
                            usesCalendarEventApi = true;
                        }
                    }
                    if (cls.equals("com/codename1/io/wifi/WiFi")
                            && (method.indexOf("connect") > -1
                                || method.indexOf("disconnect") > -1)) {
                        usesWifiHotspotConfig = true;
                    }
                    // Apps that call the low-level CN/Display review entry point
                    // directly (without the com.codename1.appreview facade) still
                    // need StoreKit + the native bridge.
                    if (!usesAppReview
                            && (cls.equals("com/codename1/ui/CN") || cls.equals("com/codename1/ui/Display"))
                            && method.indexOf("equestNativeInAppReview") > -1) {
                        usesAppReview = true;
                    }
                }
            });
        } catch (Exception ex) {
            throw new BuildException("Failed to scan project classes for permissions.", ex);
        }
        stopwatch.split("Scan Classes");

        if (usesCalendarApi) {
            // An unqualified local-source lookup defaults to event access. Task
            // methods, or an explicit reminder privacy hint, opt into reminders.
            boolean includeTasks = usesCalendarTaskApi
                    || request.getArg("ios.NSRemindersFullAccessUsageDescription", null) != null
                    || request.getArg("ios.NSRemindersUsageDescription", null) != null;
            boolean includeEvents = usesCalendarEventApi || !includeTasks;
            if (includeEvents) {
                String calendarDescription = request.getArg("ios.NSCalendarsFullAccessUsageDescription",
                        "This app uses your calendars to read and schedule events.");
                String calendarWriteDescription = request.getArg("ios.NSCalendarsWriteOnlyAccessUsageDescription",
                        "This app uses your calendar to schedule events.");
                privacyUsageDescriptions.put("NSCalendarsFullAccessUsageDescription", calendarDescription);
                privacyUsageDescriptions.put("NSCalendarsWriteOnlyAccessUsageDescription", calendarWriteDescription);
                // Retain the pre-iOS-17 key when an app supports older releases.
                privacyUsageDescriptions.put("NSCalendarsUsageDescription",
                        request.getArg("ios.NSCalendarsUsageDescription", calendarDescription));
                request.putArgument("ios.NSCalendarsFullAccessUsageDescription", calendarDescription);
                request.putArgument("ios.NSCalendarsWriteOnlyAccessUsageDescription", calendarWriteDescription);
                request.putArgument("ios.NSCalendarsUsageDescription",
                        request.getArg("ios.NSCalendarsUsageDescription", calendarDescription));
            }
            if (includeTasks) {
                String remindersDescription = request.getArg("ios.NSRemindersFullAccessUsageDescription",
                        "This app uses your reminders to read and schedule tasks.");
                privacyUsageDescriptions.put("NSRemindersFullAccessUsageDescription", remindersDescription);
                privacyUsageDescriptions.put("NSRemindersUsageDescription",
                        request.getArg("ios.NSRemindersUsageDescription", remindersDescription));
                request.putArgument("ios.NSRemindersFullAccessUsageDescription", remindersDescription);
                request.putArgument("ios.NSRemindersUsageDescription",
                        request.getArg("ios.NSRemindersUsageDescription", remindersDescription));
            }
        }

        // External surfaces: parse the build-time kinds manifest (surfaces.json in the project
        // resources, delivered alongside .ios.appext archives in resDir) and resolve the app
        // group. Widget kinds must be known at build time -- the Swift WidgetBundle is static.
        parseSurfacesManifest(resDir, request);

        // App intents: read the build-time manifest the annotation processor emitted into the
        // project jar. Deliberately softer than surfaces, where a missing manifest fails the
        // build: indexing content and donating shortcuts are perfectly legitimate with no
        // @AppIntent declared at all, so an absent manifest is simply "no declarations".
        parseIntentsManifest(resDir, request);

        // Apply AI/ML dependency table hits accumulated during the
        // scan. We route iOS pods through the existing
        // iosPods string and SPM entries through the request build
        // hints, so the IOSDependencyManager.resolve() call below can
        // pick them up consistently with manually-declared deps.
        Set<PlatformFeatureCatalog.Entry> platformFeatureHits = aiAcc.hits();
        if (!platformFeatureHits.isEmpty()) {
            // Prefer SPM when the project already uses SPM and the
            // entry exposes an SPM spec; otherwise pods. A handful
            // of ML Kit libs are pods-only -- those force pods on
            // regardless of project preference (the resolver will
            // upgrade the effective mode to BOTH below).
            boolean projectPrefersSpm = dependencyConfig.usesSwiftPackages() && !dependencyConfig.usesCocoaPods();
            StringBuilder spmPackages = new StringBuilder(request.getArg("ios.spm.packages", ""));
            for (PlatformFeatureCatalog.Entry entry : platformFeatureHits) {
                // Third-party AI packages may omit Catalyst or arm64
                // simulator slices. Keep framework-only Apple Vision enabled
                // for macNative and record the simulator constraint for the
                // generated Xcode and Pods projects.
                boolean includeApplePackageDependencies =
                        !macNativeBuilder.isEnabled()
                        || entry.iosDependenciesSupportMacCatalyst();
                if (includeApplePackageDependencies
                        && entry.iosMinimumDeploymentTarget() != null) {
                    addMinDeploymentTarget(
                            entry.iosMinimumDeploymentTarget());
                }
                if (includeApplePackageDependencies
                        && !entry.iosDependenciesSupportArm64Simulator()
                        && (!entry.iosPods().isEmpty()
                        || !entry.iosSpmSpecs().isEmpty())) {
                    excludeArm64Simulator = true;
                    log("Catalog-selected iOS dependency \""
                            + entry.description()
                            + "\" has no arm64 simulator slice. The generated "
                            + "project will use the x86_64 simulator architecture.");
                }
                boolean handledViaSpm = false;
                if (includeApplePackageDependencies
                        && projectPrefersSpm
                        && !entry.iosSpmSpecs().isEmpty()) {
                    for (PlatformFeatureCatalog.IosSpm spm : entry.iosSpmSpecs()) {
                        if (spmPackages.length() > 0) spmPackages.append(';');
                        spmPackages.append(spm.identity).append('|')
                                .append(spm.url).append('|')
                                .append(spm.requirement);
                        StringBuilder products = new StringBuilder();
                        for (int i = 0; i < spm.products.size(); i++) {
                            if (i > 0) products.append(',');
                            products.append(spm.products.get(i));
                        }
                        // Honor any user-declared products -- append, don't overwrite.
                        String existingProducts = request.getArg("ios.spm.products." + spm.identity, "");
                        if (existingProducts != null && existingProducts.length() > 0) {
                            products.insert(0, existingProducts + ",");
                        }
                        request.putArgument("ios.spm.products." + spm.identity, products.toString());
                    }
                    handledViaSpm = true;
                }
                if (includeApplePackageDependencies && !handledViaSpm) {
                    for (String pod : entry.iosPods()) {
                        // User-declared specs are already first in iosPods, so
                        // they win when the catalog requests the same pod.
                        iosPods = appendPodSpecIfAbsent(iosPods, pod);
                    }
                }
                for (String[] plistEntry : entry.iosPlistEntries()) {
                    applyCatalogPlistEntry(request, plistEntry);
                }
            }
            iosPods = deduplicatePodSpecs(iosPods);
            if (spmPackages.length() > 0) {
                request.putArgument("ios.spm.packages", spmPackages.toString());
            }
            // Surface the upload-size flag for the cloud build server
            // so it can abort early with a friendly message.
            if (aiAcc.anyRequiresBigUpload()) {
                request.putArgument("cn1.ai.requiresBigUpload", "true");
            }
            // Re-resolve in case AI deps pushed us into a different
            // mode (e.g. pods-only-when-the-project-was-SPM-only).
            dependencyConfig = IOSDependencyManager.resolve(request, iosPods);
            iosPods = dependencyConfig.iosPods;
            boolean newRunPods = dependencyConfig.usesCocoaPods();
            boolean newRunSpm = dependencyConfig.usesSwiftPackages();
            if (newRunPods && !runPods) {
                ensurePodsInstalled();
            }
            if (newRunSpm && !runSpm) {
                ensureXcodeprojInstalled();
            }
            runPods = newRunPods;
            runSpm = newRunSpm;
        }

        debug("Local Notifications "+(usesLocalNotifications?"enabled":"disabled"));
        try {
            unzip(getResourceAsStream("/iOSPort.jar"), classesDir, buildinRes, buildinRes);
        } catch (IOException ex) {
            throw new BuildException("Failed to extract the iOSPort jar", ex);
        }

        
        // Check to make sure that static libraries include the LC_VERSION_MIN_XXX run commands
        // so that ipatool doesn't choke. 
        // See https://stackoverflow.com/questions/47816371/getting-ios-development-build-error#
        // And http://thomask.sdf.org/blog/2015/09/15/xcode-7s-new-linker-rules.html
        boolean foundImproperStaticLibs = false;
        try {
            for (File f : buildinRes.listFiles()) {
                if (f.getName().endsWith(".a")) {
                    if (!validateLC_MIN_VERSION(f)) {
                        log("WARNING: The static library " + f.getName() + " is missing the LC_MIN_VERSION_IPHONEOS run command which is required by the Xcode build tools.  This generally means that it was compiled with an older version of Xcode which didn't include this command.  Unfortunately, Xcode 7 now requires this command to be embedded into all static libraries.  Please recompile this library with Xcode 7 or higher.  If this library has been embedded as part of a cn1lib, you will need to update the cn1lib with the newly compiled static library.  You may also want to look at changing the library to use Cocoapods instead of embedding the static lib directly.");
                        foundImproperStaticLibs = true;

                    }
                }
            }
        } catch (Exception ex) {
            throw new BuildException("Exception while trying to verify static libraries", ex);
        }
        if (foundImproperStaticLibs && "true".equals(request.getArg("ios.failOnWarning", "false"))) {
            // For now, we'll make the default behaviour such that we don't automatically fail when a static
            // lib doesn't have LC_VERSION_MIN because it is possible that compilation will still work.  E.g.
            // libzbar.a in the cn1-codescan library and little monkey QR reader doesn't include this 
            // and it doesn't seem to cause export to fail (need to test this).
            log("Cancelling build due to static library warnings.  Set ios.failOnWarning build hint to 'false' to ignore these warnings.");
            return false;
        }
        

        try {
            unzip(getResourceAsStream("/nativeios.jar"), classesDir, buildinRes, buildinRes);
        } catch (IOException ex) {
            throw new BuildException("Failed to extract nativeios.jar",ex);
        }
        stopwatch.split("Extract Libs");

        if(request.getArg("noExtraResources", "false").equals("true")) {
            new File(buildinRes, "CN1Resource.res").delete();
            new File(buildinRes, "IPhoneTheme.res").delete();
            new File(buildinRes, "iOS7Theme.res").delete();
        } 


        // Flip the crypto build toggles in CN1Crypto.h based on what the
        // user's bytecode references. Apps that don't touch
        // com.codename1.security.* get stub-only versions of the iOS
        // crypto bridge -- no CommonCrypto / Security framework symbols
        // referenced -- which keeps Apple's static-symbol scanner happy.
        usesCryptoGcm = usesCryptoAPI && "true".equals(request.getArg("ios.crypto.gcm", "false"));
        try {
            File cn1Crypto = new File(buildinRes, "CN1Crypto.h");
            if (cn1Crypto.exists()) {
                if (usesCryptoAPI) {
                    replaceInFile(cn1Crypto, "//#define CN1_INCLUDE_CRYPTO", "#define CN1_INCLUDE_CRYPTO");
                }
                if (usesCryptoGcm) {
                    replaceInFile(cn1Crypto, "//#define CN1_INCLUDE_CRYPTO_GCM", "#define CN1_INCLUDE_CRYPTO_GCM");
                }
            }
        } catch (Exception ex) {
            throw new BuildException("Failed to configure CN1Crypto.h", ex);
        }
        debug("Crypto API "+(usesCryptoAPI?"enabled":"disabled")
              +", AES-GCM "+(usesCryptoGcm?"enabled":"disabled"));

        if (useMetal) {
            try {
                File CN1ES2compat = new File(buildinRes, "CN1ES2compat.h");
                replaceInFile(CN1ES2compat, "//#define CN1_USE_METAL", "#define CN1_USE_METAL");
                String colorSpaceDefine = resolveMetalColorSpaceDefine(request.getArg("ios.metal.colorSpace", "sRGB"));
                replaceInFile(CN1ES2compat, "//#define CN1_METAL_COLORSPACE_PLACEHOLDER", colorSpaceDefine);
                copy(new File(buildinRes, "MainWindowMETAL.xib"), new File(buildinRes, "MainWindow.xib"));
                copy(new File(buildinRes, "CodenameOne_METALViewController.xib"), new File(buildinRes, "CodenameOne_GLViewController.xib"));
            } catch (Exception ex) {
                throw new BuildException("Failed to inject Metal controllers", ex);
            }
        } else {
            new File(buildinRes, "MainWindowMETAL.xib").delete();
            new File(buildinRes, "CodenameOne_METALViewController.xib").delete();
            // The .metal shader file isn't guarded by an #ifdef like the
            // companion .m files, so leaving it in the project forces Xcode
            // to invoke the Metal toolchain -- which Xcode 26 ships as a
            // separately-downloaded component that build servers don't have.
            new File(buildinRes, "CN1MetalShaders.metal").delete();
        }


        final String moPubAdUnitId = request.getArg("ios.mopubId", null);
        final String moPubTabletAdUnitId = request.getArg("ios.mopubTabletId", moPubAdUnitId);
        if(moPubAdUnitId != null && moPubAdUnitId.length() > 0) {
            try {
                File CodenameOne_GLViewController = new File(buildinRes, "CodenameOne_GLViewController.h");
                unzip(getResourceAsStream("/MoPubSDK_ios.zip"), classesDir, buildinRes, buildinRes);
                replaceInFile(CodenameOne_GLViewController, "//#define INCLUDE_MOPUB", "#define INCLUDE_MOPUB");
                replaceInFile(CodenameOne_GLViewController, "#define MOPUB_AD_UNIT", "#define MOPUB_AD_UNIT @\"" + moPubAdUnitId + "\"");
                replaceInFile(CodenameOne_GLViewController, "#define MOPUB_AD_SIZE", "#define MOPUB_AD_SIZE " + request.getArg("ios.mopubAdSize", "MOPUB_BANNER_SIZE"));
                replaceInFile(CodenameOne_GLViewController, "#define MOPUB_TABLET_AD_UNIT", "#define MOPUB_TABLET_AD_UNIT @\"" + moPubTabletAdUnitId + "\"");
                replaceInFile(CodenameOne_GLViewController, "#define MOPUB_TABLET_AD_SIZE", "#define MOPUB_TABLET_AD_SIZE " + request.getArg("ios.mopubTabletAdSize", "MOPUB_LEADERBOARD_SIZE"));

            } catch (Exception ex) {
                throw new BuildException("Failed to inject MoPubSDK");
            }
        }
        
        String microphoneCallback = "";
        if(request.getArg("ios.headphoneCallback", "false").equals("true")) {
            try {
                File headphoneDetectorM = new File(buildinRes, "HeadphonesDetector.m");
                File headphoneDetectorH = new File(buildinRes, "HeadphonesDetector.h");
                replaceInFile(headphoneDetectorM, "//#define DETECT_HEADPHONE", "#define DETECT_HEADPHONE");
                replaceInFile(headphoneDetectorH, "//#define DETECT_HEADPHONE2", "#define DETECT_HEADPHONE2");
                microphoneCallback =
                        "    public void headphonesDisconnected() {\n"
                                + "        i.headphonesDisconnected();\n"
                                + "    }\n\n"
                                + "    public void headphonesConnected() {\n"
                                + "        i.headphonesConnected();\n"
                                + "    }\n\n";
            } catch (Exception ex) {
                throw new BuildException("Failed to add microphone callbacks", ex);
            }
        }
        
        File launchStoryboard = new File(buildinRes, "LaunchScreen-Default.storyboard");
        if (xcodeVersion < 9) {
            launchStoryboard.delete();
        }
        
        File glAppDelegate = new File(buildinRes, "CodenameOne_GLAppDelegate.m");
        boolean useUIScene = "true".equalsIgnoreCase(request.getArg("ios.uiscene", "true"));
        String integrateFacebook = "";
        

            
        if(facebookAppId != null && facebookAppId.length() > 0) {
            try {
                if (usePodsForFacebook) {

                } else {
                    String facebookFile = "/facebook-ios-sdk-4.12.zip";

                    unzip(getResourceAsStream(facebookFile), classesDir, buildinRes, buildinRes);
                }
                integrateFacebook = "        com.codename1.social.FacebookImpl.init(com.codename1.impl.ios.IOSImplementation.nativeInstance);\n"
                        + "        Display.getInstance().setProperty(\"facebook_app_id\", \"" + facebookAppId + "\");\n";
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define INCLUDE_FACEBOOK_CONNECT", "#define INCLUDE_FACEBOOK_CONNECT");
                if (usePodsForFacebook) {
                    replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define USE_FACEBOOK_CONNECT_PODS", "#define USE_FACEBOOK_CONNECT_PODS");
                }

                String defaultPermissions = "\"public_profile\", \"email\", \"user_friends\"";
                String permissions = request.getArg("ios.facebook_permissions", request.getArg("and.facebook_permissions", defaultPermissions));
                StringTokenizer t = new StringTokenizer(permissions, " ,\n\r\t");
                permissions = "";
                permissions += "@" + t.nextToken();
                while (t.hasMoreTokens()) {
                    permissions += ", @" + t.nextToken();
                }

                replaceInFile(new File(buildinRes, "FacebookImpl.m"), "@\"basic_info\"", permissions);
            } catch (Exception ex) {
                throw new BuildException("Failed to add facebook api", ex);
            }


        }

        
        // OidcClient + SystemBrowser bootstrap: when the scanner saw any
        // com.codename1.io.oidc.* reference, the port's
        // OidcBrowserNativeImpl.init() must run before the app starts so
        // SystemBrowser.getProvider() returns the iOS native bridge.
        String integrateOidcBrowser = "";
        if (usesOidc) {
            integrateOidcBrowser =
                "        com.codename1.io.oidc.OidcBrowserNativeImpl.init();\n";
        }
        // AppleSignIn bootstrap -- same mechanism, separate gate.
        String integrateAppleSignIn = "";
        if (usesAppleSignIn) {
            integrateAppleSignIn =
                "        com.codename1.social.AppleSignInNativeImpl.init();\n";
        }
        // WebAuthn bootstrap -- same mechanism, separate gate.
        String integrateWebauthn = "";
        if (usesWebauthn) {
            integrateWebauthn =
                "        com.codename1.io.webauthn.WebAuthnNativeImpl.init();\n";
        }

        String integrateGoogleConnect = "";
        if (useGoogleSignIn) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define INCLUDE_GOOGLE_CONNECT", "#define INCLUDE_GOOGLE_CONNECT");
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define GOOGLE_SIGNIN", "#define GOOGLE_SIGNIN");

                integrateGoogleConnect = "        com.codename1.social.GoogleImpl.init(com.codename1.impl.ios.IOSImplementation.nativeInstance);\n"
                        + "        Display.getInstance().setProperty(\"ios.gplus.clientId\", \"" + googleClientId + "\");\n";
            } catch (IOException ex) {
                throw new BuildException("Failed to inject google signin support", ex);
            }
        } 

        
        boolean enableBackgroundFetch = request.getArg("ios.background_modes", "").contains("fetch");
        if (enableBackgroundFetch) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define INCLUDE_CN1_BACKGROUND_FETCH", "#define INCLUDE_CN1_BACKGROUND_FETCH");
            } catch (IOException ex) {
                throw new BuildException("Failed to add background fetch support", ex);
            }

        }
        
        if(request.getArg("ios.usePrintf","false").equals("true")) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "#define CN1Log(str,...) NSLog(str,##__VA_ARGS__)", "#define CN1Log(str,...) printf([[NSString stringWithFormat:str,##__VA_ARGS__] UTF8String])");
            } catch (IOException ex) {
                throw new BuildException("Failed to process ios.usePrintf build hint");
            }
        }
        
        boolean disableSignalHandler = request.getArg("ios.convertSignalsToExceptions", "true").equals("false");
        if (disableSignalHandler) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLAppDelegate.m"), "installSignalHandlers();", "//installSignalHandlers();");
            } catch (IOException ex) {
                throw new BuildException("Failed to process ios.convertSignalsToExceptions build hint", ex);
            }
        }
        
        
        boolean enableBackgroundLocation = request.getArg("ios.background_modes", "").contains("location");
        if (enableBackgroundLocation) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define CN1_ENABLE_BACKGROUND_LOCATION", "#define CN1_ENABLE_BACKGROUND_LOCATION");
            } catch (IOException ex) {
                throw new BuildException("Failed to process ios.background_modes location build hint", ex);
            }
        }
        
        if (enableGalleryMultiselect) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define ENABLE_GALLERY_MULTISELECT", "#define ENABLE_GALLERY_MULTISELECT");
            } catch (IOException ex) {
                throw new BuildException("Failed to enabled gallery multiselect support", ex);
            }
        }
        if (usePhotoKitForMultigallery) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define USE_PHOTOKIT_FOR_MULTIGALLERY", "#define USE_PHOTOKIT_FOR_MULTIGALLERY");
            } catch (IOException ex) {
                throw new BuildException("Failed to enabled gallery multiselect support", ex);
            }
        }
        if (enableWKWebView) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define ENABLE_WKWEBVIEW", "#define ENABLE_WKWEBVIEW");
            } catch (IOException ex) {
                throw new BuildException("Failure while enabing WKWebView support", ex);
            }
        }
        if (disableUIWebView) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define NO_UIWEBVIEW", "#define NO_UIWEBVIEW");
            } catch (IOException ex) {
                throw new BuildException("Failure while disabling UIWebView support", ex);
            }
        }
        
        if (xcodeVersion >= 9) {
            try {
                for (String privacyKey : privacyUsageDescriptions.keySet()) {
                    String defKey = "INCLUDE_" + privacyKey.replace("UsageDescription", "_USAGE").substring(2).toUpperCase();
                    replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define " + defKey, "#define " + defKey);

                }
                if (request.getArg("ios.locationUsageDescription", null) != null) {
                    replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define INCLUDE_LOCATION_USAGE", "#define INCLUDE_LOCATION_USAGE");
                }
            } catch (IOException ex) {
                throw new BuildException("Failed to add privacy usage descriptions", ex);
            }
        } else {

            photoLibraryUsage = true;
            String[] defines = {"INCLUDE_CONTACTS_USAGE", "INCLUDE_CALENDARS_USAGE", "INCLUDE_CAMERA_USAGE",
                "INCLUDE_FACEID_USAGE", "INCLUDE_LOCATION_USAGE", "INCLUDE_MICROPHONE_USAGE", "INCLUDE_MOTION_USAGE",
                "INCLUDE_PHOTOLIBRARYADD_USAGE", "INCLUDE_PHOTOLIBRARY_USAGE", "INCLUDE_REMINDERS_USAGE", 
                "INCLUDE_SIRI_USAGE", "INCLUDE_SPEECHRECOGNITION_USAGE", "INCLUDE_NFCREADER_USAGE"
            };
            try {
                for (String defKey : defines) {
                    replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define " + defKey, "#define " + defKey);
                }
            } catch (IOException ex) {
                throw new BuildException("Failed to process usage descriptions", ex);
            }
        }
        
        if ("true".equals(request.getArg("ios.blockScreenshotsOnEnterBackground", "false"))) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define CN1_BLOCK_SCREENSHOTS_ON_ENTER_BACKGROUND", "#define CN1_BLOCK_SCREENSHOTS_ON_ENTER_BACKGROUND");
            } catch (IOException ex) {
                throw new BuildException("Failure while processing ios.blockScreenshotsOnEnterBackground build hint", ex);
            }
        }

        if (useUIScene) {
            try {
                replaceInFile(new File(buildinRes, "CodenameOne_GLAppDelegate.h"), "#ifdef CN1_USE_UI_SCENE", "#define CN1_USE_UI_SCENE\n#ifdef CN1_USE_UI_SCENE");
            } catch (IOException ex) {
                throw new BuildException("Failure while processing ios.uiscene build hint", ex);
            }
        }
        
        String applicationDidEnterBackground = request.getArg("ios.applicationDidEnterBackground", null);
        if(applicationDidEnterBackground != null) {
            try {
                replaceInFile(glAppDelegate, "//----application_will_resign_active", applicationDidEnterBackground);
            } catch (IOException ex) {
                throw new BuildException("Failure while processing ios.applicationDidEnterBackground build hint", ex);
            }
        }
        


        try {
            if (request.getArg("ios.lowMemCamera", "false").equals("true")) {
                File CodenameOne_GLViewController = new File(buildinRes, "CodenameOne_GLViewController.m");
                replaceInFile(CodenameOne_GLViewController, "//#define LOW_MEM_CAMERA", "#define LOW_MEM_CAMERA");
            }

            if (request.getArg("ios.launchPlaceholder", "true").equals("false")) {
                File glViewControllerM = new File(buildinRes, "CodenameOne_GLViewController.m");
                replaceInFile(glViewControllerM, "//#define CN1_DISABLE_LAUNCH_PLACEHOLDER", "#define CN1_DISABLE_LAUNCH_PLACEHOLDER");
            }

            if (request.getArg("ios.enableStatusBar7", "true").equals("false")) {
                File CodenameOne_GLViewController = new File(buildinRes, "CodenameOne_GLViewController.m");
                replaceInFile(CodenameOne_GLViewController, "int statusbarHeight = 20;", "int statusbarHeight = 0;");
            }

            if (request.getArg("ios.enableAutoplayVideo", "false").equals("false")) {
                File iosNative = new File(buildinRes, "IOSNative.m");
                replaceInFile(iosNative, "#define AUTO_PLAY_VIDEO", "//#define AUTO_PLAY_VIDEO");
            }

            if (request.getArg("ios.background_modes", "").contains("fetch")) {
                replaceInFile(new File(buildinRes, "CodenameOne_GLAppDelegate.m"), "//#define INCLUDE_CN1_BACKGROUND_FETCH", "#define INCLUDE_CN1_BACKGROUND_FETCH");
            }
        } catch (IOException ex) {
            throw new BuildException("Failure while trying to inject build hints into sources.", ex);
        }
        
        String viewDidLoad = request.getArg("ios.viewDidLoad", null);
        String adPadding = request.getArg("ios.googleAdUnitIdPadding", "");
        if(googleAdUnitId != null && googleAdUnitId.length() > 0) {
            if(adPadding.length() == 0) {
                adPadding = "        Display.getInstance().setProperty(\"adPaddingBottom\", \"9\");\n"; 
            } else {
                adPadding = "        Display.getInstance().setProperty(\"adPaddingBottom\", \"" + adPadding + "\");\n"; 
            }
        }
        stopwatch.split("Inject Build Hints");
        
        File stubSource = new File(tmpFile, "stub");
        stubSource.mkdirs();
        // Native map provider injection (no-op unless maps.provider=apple):
        // writes the MapKit provider's Java into the stub source (compiled by
        // javac + translated by ParparVM) and its Objective-C into the native
        // sources, returning the startup snippet that registers it. Keeps the
        // core framework free of any map SDK.
        String integrateMaps = MapsProviderInjector.injectIos(this, request, stubSource, buildinRes);
        if (integrateMaps.length() > 0) {
            StringBuilder libs = new StringBuilder(request.getArg("ios.add_libs", ""));
            String[] fw = MapsProviderInjector.iosFrameworks(request);
            for (int fwi = 0; fwi < fw.length; fwi++) {
                if (libs.length() > 0) {
                    libs.append(';');
                }
                libs.append(fw[fwi]);
            }
            request.putArgument("ios.add_libs", libs.toString());
            if (request.getArg("ios.NSLocationWhenInUseUsageDescription", null) == null) {
                request.putArgument("ios.NSLocationWhenInUseUsageDescription",
                        "Shows your location on the map.");
            }
        }
        // Captured BEFORE generateUnitTestFiles, which replaces the main class with
        // CodenameOneUnitTestExecutor. Anything downstream that needs the lifecycle the developer
        // actually wrote -- the health reachability walk below is one -- cannot get it from the
        // request afterwards, and the executor class is not compiled into classesDir yet either.
        origMainClass = request.getMainClass();
        try {
            generateUnitTestFiles(request, stubSource);
        } catch (Exception ex) {
            throw new BuildException("Failed to generate Unit Test Files", ex);
        }
        stopwatch.split("Generate Unit Tests");

        String newStorage = "";
        if(request.getArg("ios.newStorageLocation", "true").equals("true")) {
            newStorage = "        Display.getInstance().setProperty(\"iosNewStorage\", \"true\");\n";
        }
        String disableScreenshots = "";
        if (request.getArg("ios.disableScreenshots", "false").equalsIgnoreCase("true")) {
            disableScreenshots = "        Display.getInstance().setProperty(\"DisableScreenshots\", \"true\");\n";
        }
        String dbLegacy = databaseLegacyStubProperty(request, usesDatabase);

        // If the build-time SVG transcoder produced a registry class, weave
        // its installGlobal() call into the Stub right before the first
        // init(Object) so theme.getImage("foo.svg") returns the transcoded
        // SVG immediately. Skipped silently for apps that have no SVGs.
        // Before the stubs are written, because what goes INTO each stub depends on the answer:
        // installing the health bindings in a stub whose lifecycle never touches health would both
        // entitle that target and make it reach health code, which is the question being asked.
        // The same registry for both targets, from the app-wide scan.
        //
        // This used to be filtered per translation root by a class walk, so each stub named only
        // the listeners its own root reached. The walk is gone: the compile failure it was written
        // to prevent happens regardless of what the stub names, because ParparVM copies every
        // non-class file on the classpath into its output verbatim. What is left is a size
        // difference in the watch binary, which is not worth a bytecode walk to own -- and
        // watchNative.health turns the whole thing off for a watch that should not have it.
        phoneHealthListeners = healthScan.resolve();
        watchHealthListeners = healthScan.resolve();
        String phoneHealthBindingsInstall = healthBindingsInstall(phoneHealthListeners, "");
        String watchHealthBindingsInstall = watchNativeBuilder.needsOwnTranslation()
                ? healthBindingsInstall(watchHealthListeners,
                        HealthListenerBindings.WATCH_SUFFIX)
                : "";

        String svgRegistryInstall = "";
        File svgRegistryClassFile = new File(classesDir,
                "com/codename1/generated/svg/SVGRegistry.class");
        if (svgRegistryClassFile.isFile()) {
            svgRegistryInstall = "            com.codename1.generated.svg.SVGRegistry.installGlobal();\n";
        }

        // Firebase Analytics bridge (ios.firebaseAnalytics=true): generate a
        // FirebaseAnalyticsProvider.Bridge whose methods are native and link to
        // the generated .m below, then register it in the Stub before init().
        // FIRAnalytics is invoked dynamically (NSClassFromString / performSelector)
        // so the .m compiles even without the Firebase pod; when the pod is absent
        // isSupported() returns false and FirebaseAnalyticsProvider is a no-op.
        String firebaseRegisterInstall = "";
        if (useFirebaseAnalytics) {
            String fbPkg = request.getPackageName();
            String fbBridgeJava = "package " + fbPkg + ";\n\n"
                    + "import com.codename1.analytics.FirebaseAnalyticsProvider;\n\n"
                    + "/** Generated by the Codename One build (ios.firebaseAnalytics=true). */\n"
                    + "public class FirebaseAnalyticsBridgeImpl implements FirebaseAnalyticsProvider.Bridge {\n"
                    + "    public native boolean isSupported();\n"
                    + "    public native void logEvent(String name, String paramsJson);\n"
                    + "    public native void logScreen(String screenName);\n"
                    + "    public native void setUserId(String id);\n"
                    + "    public native void setUserProperty(String key, String value);\n"
                    + "}\n";
            try (OutputStream fbJavaStream = new FileOutputStream(new File(stubSource, "FirebaseAnalyticsBridgeImpl.java"))) {
                fbJavaStream.write(fbBridgeJava.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ex) {
                throw new RuntimeException("Failed to generate FirebaseAnalyticsBridgeImpl.java", ex);
            }
            String pfx = fbPkg.replace('.', '_') + "_FirebaseAnalyticsBridgeImpl";
            String fbM = "// Generated by the Codename One build (ios.firebaseAnalytics=true).\n"
                    + "// Native implementation of " + fbPkg + ".FirebaseAnalyticsBridgeImpl.\n"
                    + "// FIRAnalytics is invoked dynamically so this compiles without the\n"
                    + "// Firebase pod; isSupported() returns NO when the SDK is absent.\n"
                    + "#include \"xmlvm.h\"\n"
                    + "#include \"java_lang_String.h\"\n"
                    + "#import <Foundation/Foundation.h>\n\n"
                    + "#pragma clang diagnostic ignored \"-Wundeclared-selector\"\n\n"
                    + "static Class cn1FirebaseAnalyticsClass(void) {\n"
                    + "    return NSClassFromString(@\"FIRAnalytics\");\n"
                    + "}\n\n"
                    + "JAVA_BOOLEAN " + pfx + "_isSupported__(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject) {\n"
                    + "    return cn1FirebaseAnalyticsClass() != nil ? JAVA_TRUE : JAVA_FALSE;\n"
                    + "}\n\n"
                    + "void " + pfx + "_logEvent___java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT name, JAVA_OBJECT paramsJson) {\n"
                    + "    POOL_BEGIN();\n"
                    + "    Class fir = cn1FirebaseAnalyticsClass();\n"
                    + "    if (fir != nil) {\n"
                    + "        NSString* n = toNSString(CN1_THREAD_STATE_PASS_ARG name);\n"
                    + "        NSString* pj = toNSString(CN1_THREAD_STATE_PASS_ARG paramsJson);\n"
                    + "        NSDictionary* params = nil;\n"
                    + "        if (pj != nil && pj.length > 0) {\n"
                    + "            NSData* data = [pj dataUsingEncoding:NSUTF8StringEncoding];\n"
                    + "            id parsed = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];\n"
                    + "            if ([parsed isKindOfClass:[NSDictionary class]]) { params = (NSDictionary*) parsed; }\n"
                    + "        }\n"
                    + "        SEL sel = @selector(logEventWithName:parameters:);\n"
                    + "        if ([fir respondsToSelector:sel]) {\n"
                    + "#pragma clang diagnostic push\n"
                    + "#pragma clang diagnostic ignored \"-Warc-performSelector-leaks\"\n"
                    + "            [fir performSelector:sel withObject:n withObject:params];\n"
                    + "#pragma clang diagnostic pop\n"
                    + "        }\n"
                    + "    }\n"
                    + "    POOL_END();\n"
                    + "}\n\n"
                    + "void " + pfx + "_logScreen___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT screenName) {\n"
                    + "    POOL_BEGIN();\n"
                    + "    Class fir = cn1FirebaseAnalyticsClass();\n"
                    + "    if (fir != nil) {\n"
                    + "        NSString* n = toNSString(CN1_THREAD_STATE_PASS_ARG screenName);\n"
                    + "        NSDictionary* params = n != nil ? @{ @\"screen_name\": n } : @{};\n"
                    + "        SEL sel = @selector(logEventWithName:parameters:);\n"
                    + "        if ([fir respondsToSelector:sel]) {\n"
                    + "#pragma clang diagnostic push\n"
                    + "#pragma clang diagnostic ignored \"-Warc-performSelector-leaks\"\n"
                    + "            [fir performSelector:sel withObject:@\"screen_view\" withObject:params];\n"
                    + "#pragma clang diagnostic pop\n"
                    + "        }\n"
                    + "    }\n"
                    + "    POOL_END();\n"
                    + "}\n\n"
                    + "void " + pfx + "_setUserId___java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT idStr) {\n"
                    + "    POOL_BEGIN();\n"
                    + "    Class fir = cn1FirebaseAnalyticsClass();\n"
                    + "    if (fir != nil) {\n"
                    + "        NSString* v = toNSString(CN1_THREAD_STATE_PASS_ARG idStr);\n"
                    + "        SEL sel = @selector(setUserID:);\n"
                    + "        if ([fir respondsToSelector:sel]) {\n"
                    + "#pragma clang diagnostic push\n"
                    + "#pragma clang diagnostic ignored \"-Warc-performSelector-leaks\"\n"
                    + "            [fir performSelector:sel withObject:v];\n"
                    + "#pragma clang diagnostic pop\n"
                    + "        }\n"
                    + "    }\n"
                    + "    POOL_END();\n"
                    + "}\n\n"
                    + "void " + pfx + "_setUserProperty___java_lang_String_java_lang_String(CN1_THREAD_STATE_MULTI_ARG JAVA_OBJECT instanceObject, JAVA_OBJECT key, JAVA_OBJECT value) {\n"
                    + "    POOL_BEGIN();\n"
                    + "    Class fir = cn1FirebaseAnalyticsClass();\n"
                    + "    if (fir != nil) {\n"
                    + "        NSString* k = toNSString(CN1_THREAD_STATE_PASS_ARG key);\n"
                    + "        NSString* v = toNSString(CN1_THREAD_STATE_PASS_ARG value);\n"
                    + "        SEL sel = @selector(setUserPropertyString:forName:);\n"
                    + "        if ([fir respondsToSelector:sel]) {\n"
                    + "#pragma clang diagnostic push\n"
                    + "#pragma clang diagnostic ignored \"-Warc-performSelector-leaks\"\n"
                    + "            [fir performSelector:sel withObject:v withObject:k];\n"
                    + "#pragma clang diagnostic pop\n"
                    + "        }\n"
                    + "    }\n"
                    + "    POOL_END();\n"
                    + "}\n";
            try (OutputStream fbMStream = new FileOutputStream(new File(buildinRes, "cn1_firebase_analytics_bridge.m"))) {
                fbMStream.write(fbM.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ex) {
                throw new RuntimeException("Failed to generate cn1_firebase_analytics_bridge.m", ex);
            }
            firebaseRegisterInstall = "            com.codename1.analytics.FirebaseAnalyticsProvider.registerBridge(new "
                    + fbPkg + ".FirebaseAnalyticsBridgeImpl());\n";
        }

        String didEnterBackground =  "        stopped = true;\n"
                + "        final long bgTask = com.codename1.impl.ios.IOSImplementation.beginBackgroundTask();\n"
                + "        Display.getInstance().callSerially(new Runnable() { \n"
                + "            public void run(){ \n"
                + "                i.stop();\n"
                + "                com.codename1.impl.ios.IOSImplementation.endBackgroundTask(bgTask);"
                + "            }\n"
                + "        });\n";

        try (OutputStream stubSourceStream = new FileOutputStream(new File(stubSource, request.getMainClass() + "Stub.java"))) {
            String stubSourceCode = "package " + request.getPackageName() + ";\n\n"
                    + "import com.codename1.ui.*;\n"
                    + "import com.codename1.push.PushCallback;\n\n"
                    + "import com.codename1.system.*;\n\n"
                    + "public class " + request.getMainClass() + "Stub extends com.codename1.impl.ios.Lifecycle implements Runnable {\n"
                    + "    public static final String PACKAGE_NAME = \"" + request.getPackageName() + "\";\n"
                    + "    public static final String APPLICATION_VERSION = \"" + buildVersion + "\";\n"
                    + "    public static final String APPLICATION_NAME = \"" + request.getDisplayName()+ "\";\n"
                    + "    private " + request.getMainClass() + " i = new "+request.getMainClass()+"();\n"
                    + "    private boolean initialized = false;\n"
                    + "    private boolean stopped = false;\n";

                stubSourceCode += decodeFunction();
                String delayPushCompletion = "";
                if ("true".equals(request.getArg("ios.delayPushCompletion", "false")) ||
                    "true".equals(request.getArg("delayPushCompletion", "false"))) {
                    delayPushCompletion = "        Display.getInstance().setProperty(\"ios.delayPushCompletion\", \"true\");\n";
                }
                stubSourceCode += "    public void run() {\n"
                    + "        Display.getInstance().setProperty(\"package_name\", PACKAGE_NAME);\n"
                    + delayPushCompletion
                    + "        Display.getInstance().setProperty(\"AppVersion\", APPLICATION_VERSION);\n"
                    + "        Display.getInstance().setProperty(\"AppName\", APPLICATION_NAME);\n"
                    + hardeningRuntimeProperties(request)
                    + newStorage
                    + disableScreenshots
                    + dbLegacy
                    + adPadding
                    + integrateFacebook
                    + integrateGoogleConnect
                    + integrateOidcBrowser
                    + integrateAppleSignIn
                    + integrateWebauthn
                    + integrateMaps

                    + "        if(!initialized) {\n"
                    + "            initialized = true;\n"
                    + firebaseRegisterInstall
                    + svgRegistryInstall
                    + phoneHealthBindingsInstall
                    + "            i.init(this);\n"
                    + createStartInvocation(request, "i")
                    + "        } else {\n"
                    + createStartInvocation(request, "i")
                    + "        }\n"
                    + "    }\n\n"
                    + "    public void applicationDidEnterBackground() {\n"
                    + didEnterBackground
                    + "    }\n\n"
                    + "    public void applicationWillEnterForeground() {\n"
                    + "         if(stopped) {\n"
                    + "             stopped = false;\n"
                    + "             Display.getInstance().callSerially(this);"
                    + "         }\n"
                    + "    }\n\n"
                    + "    public void applicationDidBecomeActive() {\n"
                    + "    }\n\n"
                    + microphoneCallback
                    + "    public boolean shouldApplicationHandleURL(String url, String caller) {\n"
                    + "        if(i instanceof com.codename1.system.URLCallback) {"
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
                    + registerNativeImplementationsAndCreateStubs(
                              new URLClassLoader(new URL[]{codenameOneJar.toURI().toURL()}),
                              stubSource, classesDir)
                    + "        }\n"
                    + "        " + request.getMainClass() + "Stub stub = new " + request.getMainClass() + "Stub();\n"
                    + "        com.codename1.impl.ios.IOSImplementation.setMainClass(stub.i);\n"
                    + "        com.codename1.impl.ios.IOSImplementation.setIosMode(\"" + iosMode + "\");\n"
                    + routeDispatcherInstallSource(sourceZip, "        ")
                    + annotationFrameworksInstallSource(sourceZip, "        ")
                    + "        Display.init(stub);\n"
                    + "    }\n"
                    + "}\n";

            stubSourceStream.write(stubSourceCode.getBytes(StandardCharsets.UTF_8));
            // The watch gets a stub of its OWN when it boots a different class, written into the
            // same source folder so the one javac pass below compiles both. Its translation is
            // rooted here rather than at the phone stub, which is what finally makes watchMain the
            // watch's entry point and lets the watch binary be shaken down to what that entry
            // point actually reaches.
            // No separate "reflective keep roots" channel is passed to the watch pass, and this
            // has been raised: the stub IS that channel. svgRegistryInstall and the route and
            // annotation install snippets below are the same ones the phone stub gets, so the
            // watch stub calls SVGRegistry.installGlobal() too and the generated classes it names
            // are hard references the translator follows out of the watch root. Verified on the
            // watch suite, which builds a distinct watchMain: SVGStatic and SVGAnimated render
            // real transcoded SVG and match their goldens every run. (Lottie is blank there, and
            // equally blank in the PHONE golden from the single-translation build, so the rooting
            // is not what produces it.)
            if (watchNativeBuilder.needsOwnTranslation()) {
                watchNativeBuilder.writeWatchStubSource(request, stubSource, buildVersion,
                        registerNativeImplementationsAndCreateStubs(
                                        new URLClassLoader(new URL[]{codenameOneJar.toURI().toURL()}),
                                        stubSource, classesDir),
                        iosMode, svgRegistryInstall, watchHealthBindingsInstall,
                        routeDispatcherInstallSource(sourceZip, "        "),
                        annotationFrameworksInstallSource(sourceZip, "        "));
            }
        } catch (IOException ex) {
            throw new BuildException("Failed to write stub source", ex);
        }
        stopwatch.split("Generate Stubs");
        
        Class[] nativeInterfaces = getNativeInterfaces();
        if(nativeInterfaces != null && nativeInterfaces.length > 0) {
            for(Class currentNative : nativeInterfaces) {
                File folder = new File(stubSource, currentNative.getPackage().getName().replace('.', File.separatorChar));
                folder.mkdirs();
                File javaFile = new File(folder, currentNative.getSimpleName() + "ImplCodenameOne.java");
                
                String javaImplSourceFile = "package " + currentNative.getPackage().getName() + ";\n\n"
                        + "import com.codename1.ui.PeerComponent;\n\n"
                        + "public class " + currentNative.getSimpleName() + "ImplCodenameOne {\n"
                        + "    private long nativePeer;\n\n"
                        + "    public " + currentNative.getSimpleName() + "ImplCodenameOne() {\n"
                        + "        nativePeer = initializeNativePeer();\n"
                        + "    }\n\n"
                        + "    public void finalize() {\n"
                        + "        releaseNativePeerInstance(nativePeer);\n"
                        + "    }\n\n"
                        + "    private static native long initializeNativePeer();\n\n"
                        + "    private static native void releaseNativePeerInstance(long peer);\n\n";
                
                String prefixForNewVM = "";
                String postfixForNewVM = "";
                String prefix2ForNewVM = "";
                String newVMEnterNativeCode = "";
                String newVMExitNativeCode = "";
                String newVMInclude = "";

                newVMInclude = "\n#include \"cn1_globals.h\"\n";
                newVMEnterNativeCode = "    POOL_BEGIN();\n    enteringNativeAllocations();\n";
                newVMExitNativeCode = "    finishedNativeAllocations();\n    POOL_END();\n";
                prefixForNewVM = "CODENAME_ONE_THREAD_STATE";
                prefix2ForNewVM = "CODENAME_ONE_THREAD_STATE, ";
                postfixForNewVM = "_R_long";

                String classNameWithUnderscores = currentNative.getName().replace('.', '_');
                String mSourceFile = "#include \"xmlvm.h\"\n"
                        + "#include \"java_lang_String.h\"\n"
                        + "#include <stdlib.h>\n"
                        + "#import \"CodenameOne_GLViewController.h\"\n"
                        + "#import <UIKit/UIKit.h>\n"
                        + "#import <objc/runtime.h>\n"
                        + "#import \"" + classNameWithUnderscores + "Impl.h\"\n"
                        + newVMInclude
                        + "#include \"" + classNameWithUnderscores + "ImplCodenameOne.h\"\n\n"
                        + "static id cn1_createNativeInterfacePeer(NSString* className) {\n"
                        + "    NSMutableArray* candidates = [NSMutableArray arrayWithObject:className];\n"
                        + "    NSString* executableName = [[NSBundle mainBundle] objectForInfoDictionaryKey:@\"CFBundleExecutable\"];\n"
                        + "    NSString* bundleName = [[NSBundle mainBundle] objectForInfoDictionaryKey:@\"CFBundleName\"];\n"
                        + "    NSArray* moduleNames = @[executableName ?: @\"\", bundleName ?: @\"\"];\n"
                        + "    for(NSString* moduleName in moduleNames) {\n"
                        + "        if(moduleName.length == 0) {\n"
                        + "            continue;\n"
                        + "        }\n"
                        + "        NSString* sanitized = [[moduleName stringByReplacingOccurrencesOfString:@\"-\" withString:@\"_\"] stringByReplacingOccurrencesOfString:@\" \" withString:@\"_\"];\n"
                        + "        [candidates addObject:[sanitized stringByAppendingFormat:@\".%@\", className]];\n"
                        + "        if(![sanitized isEqualToString:moduleName]) {\n"
                        + "            [candidates addObject:[moduleName stringByAppendingFormat:@\".%@\", className]];\n"
                        + "        }\n"
                        + "    }\n"
                        + "    Class cls = Nil;\n"
                        + "    for(NSString* candidate in candidates) {\n"
                        + "        cls = NSClassFromString(candidate);\n"
                        + "        if(cls != Nil) {\n"
                        + "            break;\n"
                        + "        }\n"
                        + "    }\n"
                        + "    if(cls == Nil) {\n"
                        + "        unsigned int classCount = 0;\n"
                        + "        Class *classList = objc_copyClassList(&classCount);\n"
                        + "        NSString* dottedSuffix = [@\".\" stringByAppendingString:className];\n"
                        + "        for(unsigned int i = 0; i < classCount; i++) {\n"
                        + "            NSString* runtimeName = [NSString stringWithUTF8String:class_getName(classList[i])];\n"
                        + "            if([runtimeName isEqualToString:className] || [runtimeName hasSuffix:dottedSuffix] || [runtimeName hasSuffix:className]) {\n"
                        + "                cls = classList[i];\n"
                        + "                NSLog(@\"[CN1] Resolved native interface class %@ via runtime scan as %@\", className, runtimeName);\n"
                        + "                break;\n"
                        + "            }\n"
                        + "        }\n"
                        + "        if(classList != NULL) {\n"
                        + "            free(classList);\n"
                        + "        }\n"
                        + "    }\n"
                        + "    if(cls == Nil) {\n"
                        + "        NSLog(@\"[CN1] Failed to find native interface class %@. Tried: %@\", className, candidates);\n"
                        + "        return nil;\n"
                        + "    }\n"
                        + "    return [[cls alloc] init];\n"
                        + "}\n\n"
                        + "JAVA_LONG " + classNameWithUnderscores + "ImplCodenameOne_initializeNativePeer__" + postfixForNewVM + "(" + prefixForNewVM + ") {\n"
                        + "    id i = cn1_createNativeInterfacePeer(@\"" + classNameWithUnderscores + "Impl\");\n"
                        + "    return i;\n"
                        + "}\n\n"
                        + "void " + classNameWithUnderscores + "ImplCodenameOne_releaseNativePeerInstance___long(" + prefix2ForNewVM + "JAVA_LONG l) {\n"
                        + "    id i = (id)l;\n"
                        + "    [i release];\n"
                        + "}\n\n"
                        + "extern NSData* arrayToData(JAVA_OBJECT arr);\n"
                        + "extern NSString* toNSString(" + prefix2ForNewVM + "JAVA_OBJECT str);\n"
                        + "extern JAVA_OBJECT nsDataToByteArr(NSData *data);\n"
                        + "extern JAVA_OBJECT nsDataToBooleanArray(NSData *data);\n"
                        + "extern JAVA_OBJECT nsDataToCharArray(NSData *data);\n"
                        + "extern JAVA_OBJECT nsDataToShortArray(NSData *data);\n"
                        + "extern JAVA_OBJECT nsDataToIntArray(NSData *data);\n"
                        + "extern JAVA_OBJECT nsDataToLongArray(NSData *data);\n"
                        + "extern JAVA_OBJECT nsDataToFloatArray(NSData *data);\n"
                        + "extern JAVA_OBJECT nsDataToDoubleArray(NSData *data);\n\n"
                        + "void xmlvm_init_native_"+ classNameWithUnderscores + "ImplCodenameOne() {}\n\n";

                for(Method m : currentNative.getMethods()) {
                    String name = m.getName();
                    if(name.equals("hashCode") || name.equals("equals") || name.equals("toString")) {
                        continue;
                    }
                    
                    Class returnType = m.getReturnType();
                    
                    mSourceFile += typeToXMLVMName(returnType) + " " + currentNative.getName().replace('.', '_') + "ImplCodenameOne_" + 
                            name + "__";
                    String mFileArgs;
                    String mFileBody;

                    mFileArgs = "(CODENAME_ONE_THREAD_STATE, JAVA_OBJECT me";
                    mFileBody = "    id ptr = (id)get_field_" + classNameWithUnderscores + "ImplCodenameOne_nativePeer(me);\n";

                    
                    if(!(returnType.equals(Void.class) || returnType.equals(Void.TYPE))) {
                        mFileBody += "    " + typeToXMLVMName(returnType) + " returnValue = " + convertToJavaMethod(returnType);
                    }
                    mFileBody += "[((" + classNameWithUnderscores + "Impl*)ptr) " + name;
                    
                    if(returnType.getName().equals("com.codename1.ui.PeerComponent")) {
                        javaImplSourceFile += "    public native long " + name + "(";
                    } else {
                        javaImplSourceFile += "    public native " + getSimpleNameWithJavaLang(returnType) + " " + name + "(";
                    }
                    Class[] params = m.getParameterTypes();
                    if(params != null && params.length > 0) {
                        for(int iter = 0 ; iter < params.length ; iter++) {
                            if(params[iter].getName().equals("com.codename1.ui.PeerComponent")) {
                                params[iter] = Long.TYPE;
                            }
                        }
                        javaImplSourceFile += getSimpleNameWithJavaLang(params[0]) + " param0";
                        for(int iter = 1 ; iter < params.length ; iter++) {
                            javaImplSourceFile += ", " + getSimpleNameWithJavaLang(params[iter]) + " param" + iter;
                        }
                                                
                        for(int iter = 0 ; iter < params.length ; iter++) {
                            mSourceFile += "_" + typeToXMLVMJavaName(params[iter]);
                            mFileArgs += ", " + typeToXMLVMName(params[iter]) + " param" + iter;
                            if(iter == 0) {
                                mFileBody += ":" + convertToObjectiveCMethod(params[iter]) + "param0" + convertToClosing(params[iter]); 
                            } else {
                                mFileBody += " param" + iter + ":" + convertToObjectiveCMethod(params[iter]) + "param" + iter + convertToClosing(params[iter]); 
                            }
                        }
                    }

                    if(!(returnType.equals(Void.class) || returnType.equals(Void.TYPE))) {
                        if(returnType.getName().endsWith("PeerComponent")) {
                            mSourceFile += "_R_long";
                        } else {
                            mSourceFile += "_R_" + typeToXMLVMJavaName(returnType);
                        }
                    }

                    if(!(returnType.equals(Void.class) || returnType.equals(Void.TYPE))) {
                        mSourceFile += mFileArgs + ") {\n" + newVMEnterNativeCode +
                                mFileBody + "]" + convertToClosing(returnType) + ";\n" + newVMExitNativeCode 
                                + "    return returnValue;\n}\n\n";                        
                    } else {
                        mSourceFile += mFileArgs + ") {\n" + newVMEnterNativeCode +
                                mFileBody + "]" + convertToClosing(returnType) + ";\n" + newVMExitNativeCode 
                                + "}\n\n";                        
                    }
                    javaImplSourceFile += ");\n";
                }
                
                javaImplSourceFile += "}\n";
                
                
                try (FileOutputStream out = new FileOutputStream(javaFile)) {
                    out.write(javaImplSourceFile.getBytes(StandardCharsets.UTF_8));
                } catch (IOException ex) {
                    throw new BuildException("Error while generating native interface stub for "+currentNative, ex);
                }
                File mFile = new File(resDir, "native_" + currentNative.getName().replace('.', '_') + "ImplCodenameOne.m");

                try (FileOutputStream out = new FileOutputStream(mFile)) {
                    out.write(mSourceFile.getBytes(StandardCharsets.UTF_8));
                } catch (IOException ex) {
                    throw new BuildException("Error while generating native interface stub for "+currentNative, ex);
                }

                // The generated .m imports "<X>Impl.h" -- the Objective-C
                // class the user is expected to provide as their native
                // implementation. When no such class exists for this app
                // (native interfaces pulled in transitively from a CN1
                // library, the app never instantiates them), the build
                // still needs an @interface in scope so the .m compiles.
                // Generate a tiny placeholder iff the user hasn't dropped
                // their own copy alongside the project sources. The peer
                // class itself stays absent at runtime, which is fine: any
                // call into this native interface from Java would have
                // failed to resolve a peer regardless.
                File implHeader = new File(resDir, classNameWithUnderscores + "Impl.h");
                if (!implHeader.exists()) {
                    String guard = classNameWithUnderscores.toUpperCase() + "_IMPL_H";
                    String hStub = "#ifndef " + guard + "\n"
                            + "#define " + guard + "\n"
                            + "// Auto-generated placeholder: the native interface "
                            + currentNative.getName() + " has no user-provided\n"
                            + "// Objective-C implementation in this project. The CN1\n"
                            + "// runtime returns nil from cn1_createNativeInterfacePeer\n"
                            + "// in that case; calls into the peer no-op silently.\n"
                            + "#import <Foundation/Foundation.h>\n"
                            + "@interface " + classNameWithUnderscores + "Impl : NSObject\n"
                            + "@end\n"
                            + "#endif\n";
                    try (FileOutputStream out = new FileOutputStream(implHeader)) {
                        out.write(hStub.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException ex) {
                        throw new BuildException("Error while generating placeholder header for "+currentNative, ex);
                    }
                }
            }
        }
        // Health background-listener bindings, written where the stub
        // sources are compiled from so javac picks them up and ParparVM
        // translates the result. Generated rather than resolved
        // reflectively: a direct constructor call is a reference the
        // translator follows, and a name passed to Class.forName is not.
        for (String warning : healthScan.warnings()) {
            log("WARNING: " + warning);
        }
        // One factory per translation root, each holding only the listeners that root reaches --
        // see healthListenersReachableFrom. The two are written into the same source folder because
        // one javac pass compiles both stubs; it is the TRANSLATION that separates them, and each
        // stub installs only its own.
        writeHealthBindings(stubSource, phoneHealthListeners, "");
        if (watchNativeBuilder.needsOwnTranslation()) {
            writeHealthBindings(stubSource, watchHealthListeners,
                    HealthListenerBindings.WATCH_SUFFIX);
        }
        String javacPath = System.getProperty("java.home") + "/../bin/javac";
        if (!new File(javacPath).exists()) {
            javacPath = System.getProperty("java.home") + "/bin/javac";
        }
        if (!new File(javacPath).exists()) {
            javacPath = "javac";
        }
        String[] stubSourceTarget = getStubCompileSourceTarget(javacPath);
        try {
            if (!execWithFiles(stubSource, stubSource, ".java", javacPath, "-source", stubSourceTarget[0], "-target", stubSourceTarget[1], "-classpath",
                    classesDir.getAbsolutePath(),
                    "-d", classesDir.getAbsolutePath())) {
                return false;
            }
        } catch (Exception ex) {
            throw new BuildException("Failure occurred while compiling native interface stubs", ex);
        }
        // Two entry points cannot share one classpath: the translator parses everything it is
        // given and refuses two mains. Each stub moves into a directory of its own so each pass
        // can be handed exactly one; the application classes stay shared.
        if (watchNativeBuilder.needsOwnTranslation()) {
            try {
                phoneStubDir = watchNativeBuilder.isolateStub(request, classesDir, tmpFile, false);
                watchStubDir = watchNativeBuilder.isolateStub(request, classesDir, tmpFile, true);
            } catch (IOException ex) {
                throw new BuildException("Failed to separate the phone and watch entry points", ex);
            }
        }
        stopwatch.split("Compile Stubs");

        
        try {
            if (!generateIcons(request)) {
                return false;
            }
        } catch (Exception ex) {
            throw new BuildException("Failed to generate icons", ex);
        }

        try {
            if (!generateLaunchScreen(request)) {
                return false;
            }
        } catch (Exception ex) {
            throw new BuildException("Failed to generate launch screen");
        }
        stopwatch.split("Generate Icons");

        resultDir = new File(tmpFile, "result");
        resultDir.mkdirs();




        includePush = request.getArg("ios.includePush", "false").equalsIgnoreCase("true");

        if ((request.getPushCertificate() != null || includePush) || usesLocalNotifications) {
            try {
                File appDelH = new File(buildinRes, "CodenameOne_GLAppDelegate.h");
                DataInputStream dis = new DataInputStream(new FileInputStream(appDelH));
                byte[] data = new byte[(int) appDelH.length()];
                dis.readFully(data);
                dis.close();
                try(Writer fios = new OutputStreamWriter(Files.newOutputStream(appDelH.toPath()), StandardCharsets.UTF_8)) {
                    String str = new String(data, StandardCharsets.UTF_8);
                    str = str.replace("//#define CN1_INCLUDE_NOTIFICATIONS", "#define CN1_INCLUDE_NOTIFICATIONS");
                    if (request.getArg("ios.notificationPermissionAtLaunch", "false").equalsIgnoreCase("true")) {
                        // Restore pre-#4876 behavior: prompt for notification permission
                        // in didFinishLaunchingWithOptions instead of on first registerPush /
                        // sendLocalNotification call.
                        str = str.replace("//#define CN1_NOTIFICATION_PERMISSION_AT_LAUNCH", "#define CN1_NOTIFICATION_PERMISSION_AT_LAUNCH");
                    }
                    fios.write(str);
                }

                File iosNative = new File(buildinRes, "IOSNative.m");
                dis = new DataInputStream(Files.newInputStream(iosNative.toPath()));
                data = new byte[(int) iosNative.length()];
                dis.readFully(data);
                dis.close();
                try (Writer fios = new OutputStreamWriter(Files.newOutputStream(iosNative.toPath()), StandardCharsets.UTF_8)) {
                    String str = new String(data, StandardCharsets.UTF_8);
                    str = str.replace("//#define CN1_INCLUDE_NOTIFICATIONS2", "#define CN1_INCLUDE_NOTIFICATIONS2");
                    fios.write(str);
                }
            } catch (IOException ex) {
                log("Failed to Update Objective-C source files to activate notifications flag");
                throw new BuildException("Failed to update Objective-C source files to activate notifications flag", ex);
            }
        }

        // The Wallet issuer-provisioning natives in IOSNative.m stay dormant
        // (#else stubs) unless the app needs them: unused wallet-looking code
        // in the binary can trigger questions during Apple review.
        if (usesWalletApi || "true".equals(request.getArg("ios.wallet.extension", "false"))) {
            try {
                replaceInFile(new File(buildinRes, "IOSNative.m"), "//#define CN1_INCLUDE_WALLET", "#define CN1_INCLUDE_WALLET");
            } catch (IOException ex) {
                throw new BuildException("Failed to update Objective-C source files to activate the wallet flag", ex);
            }
        }

        if(!(request.getPushCertificate() != null || includePush)) {
            try {
                // special workaround for issue Apple is having with push notification missing from
                // the entitlements
                byte[] data = new byte[(int) glAppDelegate.length()];
                try(DataInputStream dis = new DataInputStream(Files.newInputStream(glAppDelegate.toPath()))) {
                    dis.readFully(data);
                }

                try(Writer fios = new OutputStreamWriter(Files.newOutputStream(glAppDelegate.toPath()), StandardCharsets.UTF_8)) {
                    String str = new String(data, StandardCharsets.UTF_8);
                    str = str.replace("#define INCLUDE_CN1_PUSH", "");
                    fios.write(str);
                }

                File iosNative = new File(buildinRes, "IOSNative.m");
                try(DataInputStream dis = new DataInputStream(Files.newInputStream(iosNative.toPath()))) {
                    data = new byte[(int) iosNative.length()];
                    dis.readFully(data);
                }
                try (Writer fios = new OutputStreamWriter(Files.newOutputStream(iosNative.toPath()), StandardCharsets.UTF_8)) {
                    String str = new String(data, StandardCharsets.UTF_8);
                    str = str.replace("#define INCLUDE_CN1_PUSH2", "//#define INCLUDE_CN1_PUSH2");
                    fios.write(str);
                }
            } catch (IOException ex) {
                throw new BuildException("Failed to update Objective-C source files to activate push notification flag", ex);
            }

        } else {
            if(request.getArg("ios.enableBadgeClear", "true").equals("false")) {
                try {
                    replaceInFile(glAppDelegate, "[UIApplication sharedApplication].applicationIconBadgeNumber = 0;", "//[UIApplication sharedApplication].applicationIconBadgeNumber = 0;");
                    replaceInFile(glAppDelegate, "[[UIApplication sharedApplication] cancelAllLocalNotifications];", "//[[UIApplication sharedApplication] cancelAllLocalNotifications];");
                } catch (IOException ex) {
                    throw new BuildException("Failed to remove badge notifications from objective-c soruce files", ex);
                }
            }
        }

        try {
            File iosNative = new File(buildinRes, "IOSNative.m");
            String glAppDelegeateHeader = request.getArg("ios.glAppDelegateHeader", null);
            if (glAppDelegeateHeader != null && glAppDelegeateHeader.length() > 0) {
                replaceInFile(glAppDelegate, "//GL_APP_DELEGATE_INCLUDE", glAppDelegeateHeader);
            }

            File jailbreakH = new File(buildinRes, "CN1JailbreakDetector.h");
            if (jailbreakH.exists() && detectJailbreak) {
                replaceInFile(jailbreakH, "//#define CN1_DETECT_JAILBREAK", "#define CN1_DETECT_JAILBREAK");
                // The native probes in that header need nothing from the plist. The
                // URL-scheme probe on the Java side does, and its declaration is made
                // further down, just before the plist is rendered -- see the call beside
                // injectToPlist().
            }

            // ios.appAttest compiles the DeviceCheck-backed App Attest native code
            // (gated by CN1_USE_APP_ATTEST so non-attest builds neither import nor
            // link DeviceCheck) and links DeviceCheck.framework + injects the
            // appattest-environment entitlement below.
            if (appAttest) {
                replaceInFile(new File(buildinRes, "IOSNative.m"), "//#define CN1_USE_APP_ATTEST", "#define CN1_USE_APP_ATTEST");
            }

            // com.codename1.car usage compiles the CarPlay native code (gated by CN1_USE_CARPLAY so
            // non-car builds neither import nor link CarPlay.framework) and links the framework +
            // injects the carplay entitlement and CarPlay scene below. The define lives in the shared
            // CodenameOne_GLViewController.h so it is visible to every CarPlay translation unit
            // (IOSNative.m and CodenameOne_CarPlaySceneDelegate.m), mirroring CN1_INCLUDE_NFC.
            if (usesCar) {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define CN1_USE_CARPLAY", "#define CN1_USE_CARPLAY");
            }

            // com.codename1.surfaces usage compiles the WidgetKit/ActivityKit native glue (gated
            // by CN1_USE_WIDGETS so other builds carry no surfaces symbols). The define lives in
            // the shared CodenameOne_GLViewController.h so it is visible to every surfaces
            // translation unit (IOSNative.m and CodenameOne_GLAppDelegate.m), mirroring
            // CN1_USE_CARPLAY. Skipped when ios.surfaces.extension=false.
            // The watch half counts too, and independently: a manifest whose every kind is a
            // complication produces no iOS extension, and the WATCH slice still needs these
            // natives. Without the define its own Surfaces.publish() is a no-op AND
            // cn1_watch_apply_mirrored_surface -- which the WatchConnectivity delegate calls -- is
            // compiled out, so the watch slice fails to link rather than merely doing nothing.
            if (surfacesExtensionEnabled || surfacesWatchEnabled) {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define CN1_USE_WIDGETS", "#define CN1_USE_WIDGETS");
            }

            // com.codename1.wearable usage compiles the WatchConnectivity glue (gated by
            // CN1_USE_WATCHCONNECTIVITY so other builds carry no WCSession symbols). The define
            // lives in the shared CodenameOne_GLViewController.h so it reaches every wearable
            // translation unit, and unlike the widgets define it deliberately survives on the watch
            // slice: both halves of a pair run the same symmetric code.
            // surfacesWatchEnabled counts too. Mirroring a phone-side publish to the watch
            // rides WCSession, and the app that publishes need never have written a line of
            // com.codename1.wearable -- so the scan alone would leave the mirror with no
            // transport in exactly the apps that want one.
            if (usesWearable || surfacesWatchEnabled) {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define CN1_USE_WATCHCONNECTIVITY", "#define CN1_USE_WATCHCONNECTIVITY");
            }

            // com.codename1.intents usage compiles the Core Spotlight / App Intents glue (gated
            // by CN1_USE_INTENTS so other builds carry no such symbols), and opens the
            // non-browsing NSUserActivity path in the app delegate. The define lives in the
            // shared CodenameOne_GLViewController.h so it reaches every intents translation
            // unit, mirroring CN1_USE_WIDGETS.
            if (usesIntents) {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define CN1_USE_INTENTS", "#define CN1_USE_INTENTS");
            }
            // Narrower than CN1_USE_INTENTS: this says declarations were actually generated, so
            // the runtime can answer isVoiceInvocationSupported() honestly. An app that only
            // indexes content, or that set ios.intents.appIntents=false, still gets the bridge
            // Swift for donation and queries -- so the class being present proves nothing about
            // whether an App Intent can run.
            if (declaresAppIntents) {
                replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"),
                        "//#define CN1_APP_INTENTS_DECLARED", "#define CN1_APP_INTENTS_DECLARED");
            }

            String glAppDelegeateBody = request.getArg("ios.glAppDelegateBody", null);
            if (glAppDelegeateBody != null && glAppDelegeateBody.length() > 0) {
                replaceInFile(glAppDelegate, "//GL_APP_DELEGATE_BODY", glAppDelegeateBody);
            }

            String openURLInject = request.getArg("ios.openURLInject", null);
            if (openURLInject != null && openURLInject.length() > 0) {
                replaceInFile(glAppDelegate, "//openURLMarkerEntry", openURLInject);
            }


            String beforeFinishLaunching = request.getArg("ios.beforeFinishLaunching", null);
            if (beforeFinishLaunching != null) {
                replaceInFile(glAppDelegate, "//beforeDidFinishLaunchingWithOptionsMarkerEntry", beforeFinishLaunching);
            }

            if (!localizedIcons.isEmpty()) {
                // Runs before the user's ios.afterFinishLaunching injection so the marker
                // is still present for that replacement. Preserve the marker for future hooks.
                String selector = buildLocalizedIconSelectorObjC();
                replaceInFile(glAppDelegate, "//afterDidFinishLaunchingWithOptionsMarkerEntry",
                        selector + "    //afterDidFinishLaunchingWithOptionsMarkerEntry");
            }

            String afterFinishLaunching = request.getArg("ios.afterFinishLaunching", null);
            if (afterFinishLaunching != null) {
                replaceInFile(glAppDelegate, "//afterDidFinishLaunchingWithOptionsMarkerEntry", afterFinishLaunching);
            }

            // one of: UIActionSheetStyleAutomatic, UIActionSheetStyleDefault, UIActionSheetStyleBlackTranslucent (default), UIActionSheetStyleBlackOpaque
            String actionSheetStyle = request.getArg("ios.actionSheetStyle", null);
            if (actionSheetStyle != null) {
                replaceInFile(iosNative, "[actionSheet setActionSheetStyle:UIActionSheetStyleBlackTranslucent", "[actionSheet setActionSheetStyle:" + actionSheetStyle);

            }

            String zbarFlash = request.getArg("ios.zbar_flash", "true");
            if (zbarFlash.equals("false")) {
                // remove the flash behavior from zbar
                replaceAllInFile(iosNative, "//ZBAR_CONFIGURATIONS", "reader.readerView.torchMode = AVCaptureTorchModeOff;");
            }

            if (request.getArg("ios.keyboardOpen", "true").equals("true")) {
                File CodenameOne_GLViewController_m = new File(buildinRes, "CodenameOne_GLViewController.m");
                replaceInFile(CodenameOne_GLViewController_m, "BOOL vkbAlwaysOpen = NO;", "BOOL vkbAlwaysOpen = YES;");
            }
            if (request.getArg("ios.associatedDomains", null) != null) {
                // If the user has provided the ios.associatedDomains build hint, then we will need to
                // enable handling for these events.
                // We keep it off by default in case it interferes.
                File CodenameOne_GLViewController_h = new File(buildinRes, "CodenameOne_GLViewController.h");
                replaceInFile(CodenameOne_GLViewController_h, "//#define CN1_HANDLE_UNIVERSAL_LINKS", "#define CN1_HANDLE_UNIVERSAL_LINKS");
            }


            if (request.getArg("ios.locationUsageDescription", null) != null) {
                // Remove location warning message for iOS8...  This is sort of developer documentation
                // so that they know what to do when location fails silently on iOS 8
                File CodenameOne_GLViewController_h = new File(buildinRes, "CodenameOne_GLViewController.h");
                replaceInFile(CodenameOne_GLViewController_h, "#define IOS8_LOCATION_WARNING", "//#define IOS8_LOCATION_WARNING");
            }

            if (request.getArg("ios.background_modes", "").contains("location")) {
                File CodenameOne_GLViewController_h = new File(buildinRes, "CodenameOne_GLViewController.h");
                replaceInFile(CodenameOne_GLViewController_h, "#define CN1_REQUEST_LOCATION_AUTH requestWhenInUseAuthorization", "#define CN1_REQUEST_LOCATION_AUTH requestAlwaysAuthorization");

            }
            if (usesPurchaseAPI) {
                File CodenameOne_GLViewController_h = new File(buildinRes, "CodenameOne_GLViewController.h");
                replaceInFile(CodenameOne_GLViewController_h, "//#define CN1_USE_STOREKIT", "#define CN1_USE_STOREKIT");

            }
            if (usesAppReview) {
                File CodenameOne_GLViewController_h = new File(buildinRes, "CodenameOne_GLViewController.h");
                replaceInFile(CodenameOne_GLViewController_h, "//#define CN1_USE_APPREVIEW", "#define CN1_USE_APPREVIEW");

            }
        } catch (Exception ex) {
            throw new BuildException("Failure while injecting code from build hints", ex);
        }
        if(googleAdUnitId != null && googleAdUnitId.length() > 0) {

            try {
                File CodenameOne_GLViewController_h = new File(buildinRes, "CodenameOne_GLViewController.h");
                replaceAllInFile(CodenameOne_GLViewController_h, "//ADD_VARIABLES", "@public\n    GADBannerView *googleBannerView;\n");
                if (usePodsForGoogleAds) {
                    replaceAllInFile(CodenameOne_GLViewController_h, "//ADD_INCLUDE", "#import <GoogleMobileAds/GoogleMobileAds.h>\n");
                } else {
                    replaceAllInFile(CodenameOne_GLViewController_h, "//ADD_INCLUDE", "#import \"GADBannerView.h\"\n");
                }

                File CodenameOne_GLViewController_m = new File(buildinRes, "CodenameOne_GLViewController.m");
                replaceAllInFile(CodenameOne_GLViewController_m, "//replaceViewDidAppear", "[self addGoogleAds];\n");

                replaceAllInFile(CodenameOne_GLViewController_m, "//WILL_ROTATE_TO_INTERFACE_MARKER", "if(googleBannerView != nil) {\n" +
                        "        [googleBannerView removeFromSuperview];\n" +
                        "        [googleBannerView release];\n" +
                        "        googleBannerView = nil;\n" +
                        "    }\n");
                replaceAllInFile(CodenameOne_GLViewController_m, "//DID_ROTATE_FROM_INTERFACE_MARKER", "[self addGoogleAds];\n");
                replaceAllInFile(CodenameOne_GLViewController_m, "//INJECT_METHODS_MARKER", "-(void) addGoogleAds {\n" +
                        "    if(googleBannerView != nil) {\n" +
                        "        [googleBannerView removeFromSuperview];\n" +
                        "        [googleBannerView release];\n" +
                        "        googleBannerView = nil;\n" +
                        "    }\n" +
                        "    UIInterfaceOrientation orientation = [[UIApplication sharedApplication] statusBarOrientation];\n" +
                        "    bool isPortrait = (orientation == UIInterfaceOrientationPortrait || orientation == UIInterfaceOrientationPortraitUpsideDown);\n" +
                        "    GADAdSize adSize = kGADAdSizeSmartBannerPortrait;\n" +
                        "    if(!isPortrait) {\n" +
                        "        adSize = kGADAdSizeSmartBannerLandscape;\n" +
                        "    }\n" +
                        "    googleBannerView = [[GADBannerView alloc] initWithAdSize:adSize];\n" +
                        "    googleBannerView.adUnitID = @\"" + googleAdUnitId + "\";\n" +
                        "    googleBannerView.rootViewController = self;\n" +
                        "    [self.view addSubview:googleBannerView];\n" +
                        "    GADRequest *request = [GADRequest request];\n" +
                        "    request.testDevices = [NSArray arrayWithObjects:@\"" +
                        request.getArg("ios.googleAdUnitTestDevice", "97cfc76e5efbc6dfa7eb2e6857b613a0") + "\", nil];\n" +
                        "    [googleBannerView loadRequest:request];\n" +
                        "    CGRect r =CGRectMake([CodenameOne_GLViewController instance].view.bounds.size.width / 2 - googleBannerView.bounds.size.width / 2,\n" +
                        "                         [CodenameOne_GLViewController instance].view.bounds.size.height - googleBannerView.bounds.size.height,\n" +
                        "                         CGSizeFromGADAdSize(adSize).width, CGSizeFromGADAdSize(adSize).height);\n" +
                        "    [googleBannerView setFrame:r];\n" +
                        "}");
            } catch (Exception ex) {
                throw new BuildException("Failed to inject google ads", ex);
            }
        }
        try {
            if (viewDidLoad != null) {
                File CodenameOne_GLViewController = new File(buildinRes, "CodenameOne_GLViewController.m");
                replaceAllInFile(CodenameOne_GLViewController, "//replaceViewDidLoad", viewDidLoad);
            }

            if (request.getArg("ios.viewDidLoadInclude", null) != null) {
                File CodenameOne_GLViewController = new File(buildinRes, "CodenameOne_GLViewController.m");
                replaceAllInFile(CodenameOne_GLViewController, "#import \"CodenameOne_GLViewController.h\"", "#import \"CodenameOne_GLViewController.h\"\n" + request.getArg("ios.viewDidLoadInclude", ""));
            }
        } catch (Exception ex) {
            throw new BuildException("Failed to inject indo vidwDidLoad", ex);
        }
        
         {

            String addLibs = request.getArg("ios.add_libs", null);
            if(addLibs != null) {
                addLibs = addLibs.replace(',', ';').replace(':', ';');
                if (addLibs.startsWith(";")) {
                    addLibs = addLibs.substring(1);
                }
            }

            // LocalAuthentication is required only when the app actually uses
            // com.codename1.security.Biometrics / SecureStorage. The scanner
            // above sets usesBiometrics if any com/codename1/security/ class
            // is referenced; apps that don't touch the API pay nothing.
            if (usesBiometrics) {
                if (addLibs == null || addLibs.length() == 0) {
                    addLibs = "LocalAuthentication.framework";
                } else if (!addLibs.toLowerCase().contains("localauthentication")) {
                    addLibs = addLibs + ";LocalAuthentication.framework";
                }
            }
            if (usesCalendarApi) {
                try {
                    replaceInFile(new File(buildinRes, "IOSNative.m"),
                            "//#define CN1_USE_CALENDAR", "#define CN1_USE_CALENDAR");
                } catch (IOException ex) {
                    throw new BuildException(
                            "Failed to enable CN1_USE_CALENDAR", ex);
                }
                if (addLibs == null || addLibs.length() == 0) {
                    addLibs = "EventKit.framework";
                } else if (!addLibs.toLowerCase().contains("eventkit")) {
                    addLibs = addLibs + ";EventKit.framework";
                }
            }

            // DeviceCheck.framework backs App Attest (com.codename1.security.
            // DeviceIntegrity.requestIntegrityToken on iOS). Only linked when the
            // ios.appAttest build hint enabled the CN1_USE_APP_ATTEST native code.
            if (appAttest) {
                if (addLibs == null || addLibs.length() == 0) {
                    addLibs = "DeviceCheck.framework";
                } else if (!addLibs.toLowerCase().contains("devicecheck")) {
                    addLibs = addLibs + ";DeviceCheck.framework";
                }
            }

            // AuthenticationServices.framework hosts both
            // ASWebAuthenticationSession (used by SystemBrowser) and
            // ASAuthorizationAppleIDProvider (used by AppleSignIn). Linking
            // it always when the user references either API is the simplest
            // policy; iOS 12 is the deployment-target floor for both classes.
            //
            // We also flip the matching CN1_INCLUDE_OIDC / CN1_INCLUDE_APPLESIGNIN
            // preprocessor defines so the .m source bodies in
            // nativeSources/CN1OidcBrowser.m and CN1AppleSignIn.m compile
            // in -- otherwise the .m files would reference framework symbols
            // without the framework being linked, breaking the link step
            // for apps that never use the API.
            if (usesOidc || usesAppleSignIn || usesWebauthn) {
                String authSvc = "AuthenticationServices.framework";
                if (addLibs == null || addLibs.length() == 0) {
                    addLibs = authSvc;
                } else if (!addLibs.toLowerCase().contains("authenticationservices")) {
                    addLibs = addLibs + ";" + authSvc;
                }
            }
            if (usesOidc) {
                try {
                    replaceInFile(new File(buildinRes,
                            "CodenameOne_GLViewController.h"),
                            "//#define CN1_INCLUDE_OIDC",
                            "#define CN1_INCLUDE_OIDC");
                } catch (IOException ex) {
                    throw new BuildException(
                            "Failed to enable CN1_INCLUDE_OIDC", ex);
                }
            }
            if (usesAppleSignIn) {
                try {
                    replaceInFile(new File(buildinRes,
                            "CodenameOne_GLViewController.h"),
                            "//#define CN1_INCLUDE_APPLESIGNIN",
                            "#define CN1_INCLUDE_APPLESIGNIN");
                } catch (IOException ex) {
                    throw new BuildException(
                            "Failed to enable CN1_INCLUDE_APPLESIGNIN", ex);
                }
            }
            if (usesWebauthn) {
                try {
                    replaceInFile(new File(buildinRes,
                            "CodenameOne_GLViewController.h"),
                            "//#define CN1_INCLUDE_WEBAUTHN",
                            "#define CN1_INCLUDE_WEBAUTHN");
                } catch (IOException ex) {
                    throw new BuildException(
                            "Failed to enable CN1_INCLUDE_WEBAUTHN", ex);
                }
            }

            // CoreNFC is required only when the app actually uses
            // com.codename1.nfc. We weak-link it so older deployment targets
            // still load on iOS 10 (Core NFC was introduced in iOS 11).
            if (usesNfc) {
                String coreNfc = "CoreNFC.framework";
                if (addLibs == null || addLibs.length() == 0) {
                    addLibs = coreNfc;
                } else if (!addLibs.toLowerCase().contains("corenfc")) {
                    addLibs = addLibs + ";" + coreNfc;
                }
                // Default the NFC reader usage description if the developer
                // forgot the plist hint; Apple rejects builds that present
                // an NFCNDEFReaderSession without one.
                if (request.getArg("ios.NFCReaderUsageDescription", null) == null) {
                    request.putArgument("ios.NFCReaderUsageDescription",
                            "Hold near an NFC tag to continue");
                }
                // Inject the canonical NFC entitlement keys. The developer
                // can override either via build hints.
                String formats = request.getArg(
                        "ios.entitlements.com.apple.developer.nfc.readersession.formats",
                        null);
                if (formats == null) {
                    request.putArgument(
                            "ios.entitlements.com.apple.developer.nfc.readersession.formats",
                            "TAG\nNDEF");
                }
                // Uncomment CN1_INCLUDE_NFC in CodenameOne_GLViewController.h
                // so the NFC native block in IOSNative.m compiles in. Apps
                // that do NOT reference com.codename1.nfc leave the define
                // commented out, which means CoreNFC.framework symbols are
                // never linked --- this is required to pass Apple's API-
                // usage scan without a CoreNFC privacy manifest.
                try {
                    replaceInFile(new File(buildinRes,
                            "CodenameOne_GLViewController.h"),
                            "//#define CN1_INCLUDE_NFC",
                            "#define CN1_INCLUDE_NFC");
                } catch (IOException ex) {
                    throw new BuildException(
                            "Failed to enable CN1_INCLUDE_NFC", ex);
                }
            }

            // First-class Bluetooth: weak-link CoreBluetooth and compile in
            // the CN1Bluetooth natives only when the app references
            // com.codename1.bluetooth.*. The NSBluetooth* privacy strings
            // are defaulted (only-if-unset) by the PlatformFeatureCatalog entry
            // through the standard plist application above. Background
            // operation is opt-in through the ios.bluetooth.background hint
            // ("central", "peripheral" or "central,peripheral"), merged into
            // ios.background_modes so the standard UIBackgroundModes
            // assembly (and its plistInject-conflict check) applies.
            if (usesBluetooth) {
                String coreBt = "CoreBluetooth.framework";
                if (addLibs == null || addLibs.length() == 0) {
                    addLibs = coreBt;
                } else if (!addLibs.toLowerCase().contains("corebluetooth")) {
                    addLibs = addLibs + ";" + coreBt;
                }
                try {
                    replaceInFile(new File(buildinRes,
                            "CodenameOne_GLViewController.h"),
                            "//#define CN1_INCLUDE_BLUETOOTH",
                            "#define CN1_INCLUDE_BLUETOOTH");
                } catch (IOException ex) {
                    throw new BuildException(
                            "Failed to enable CN1_INCLUDE_BLUETOOTH", ex);
                }
                String btBackground = request.getArg("ios.bluetooth.background", "");
                if (btBackground.length() > 0) {
                    String modes = request.getArg("ios.background_modes", "");
                    if (btBackground.contains("central")
                            && !modes.contains("bluetooth-central")) {
                        modes = modes.length() == 0 ? "bluetooth-central"
                                : modes + ",bluetooth-central";
                    }
                    if (btBackground.contains("peripheral")
                            && !modes.contains("bluetooth-peripheral")) {
                        if (!usesBluetoothPeripheral) {
                            log("Warning: ios.bluetooth.background requests the "
                                    + "bluetooth-peripheral mode but the app never "
                                    + "references com.codename1.bluetooth.le.server; "
                                    + "adding it anyway.");
                        }
                        modes = modes.length() == 0 ? "bluetooth-peripheral"
                                : modes + ",bluetooth-peripheral";
                    }
                    request.putArgument("ios.background_modes", modes);
                }
            }

            // Smart home (com.codename1.home.*).
            //
            // The commissioning opt-out is read first: everything below that
            // asks whether the app touches the accessory graph has to see the
            // same answer, and adding an accessory does touch it -- the
            // commissioning sheet puts the device into the user's HomeKit
            // home -- but only when the sheet is actually built.
            matterExtensionEnabled = usesHomeCommissioning
                    && !"false".equals(request.getArg(
                            "ios.home.commissioning", "true"));
            if (matterExtensionEnabled) {
                usesHomeAccessoryData = true;
            }
            if (usesSmartHome) {
                String hk = "HomeKit.framework";
                if (addLibs == null || addLibs.length() == 0) {
                    addLibs = hk;
                } else if (!addLibs.toLowerCase().contains("homekit")) {
                    addLibs = addLibs + ";" + hk;
                }
                try {
                    replaceInFile(new File(buildinRes,
                            "CodenameOne_GLViewController.h"),
                            "//#define CN1_INCLUDE_HOMEKIT",
                            "#define CN1_INCLUDE_HOMEKIT");
                } catch (Exception ex) {
                    throw new BuildException(
                            "Failed to enable CN1_INCLUDE_HOMEKIT", ex);
                }

                // openEcosystemApp() asks canOpenURL: whether the Apple Home
                // app is there, and iOS answers false for any scheme the app
                // has not declared -- so without this the fallback every
                // unsupported platform points at reports Home missing on a
                // device that has it.
                // Entry by entry, not as a substring: a project that already
                // queries com.apple.HomePreview contains "com.apple.Home",
                // and skipping on that basis leaves the exact scheme
                // openEcosystemApp() asks about undeclared -- so canOpenURL:
                // reports Apple Home missing on a device that has it, which
                // is the failure this block exists to prevent.
                String queries = request.getArg(
                        "ios.applicationQueriesSchemes", "");
                boolean homeSchemeDeclared = false;
                for (String scheme : queries.split(",")) {
                    if ("com.apple.Home".equals(scheme.trim())) {
                        homeSchemeDeclared = true;
                        break;
                    }
                }
                // A project that declares the array through ios.plistInject
                // is left alone. The plist renderer emits its own
                // LSApplicationQueriesSchemes key for this hint without
                // looking at the injected fragment, so setting the hint as
                // well would put the key in the plist twice -- and a plist
                // with a duplicate key is not a plist that reliably keeps
                // either value. Said out loud, because the consequence
                // (openEcosystemApp finding nothing) is otherwise a mystery.
                if (!homeSchemeDeclared && WatchNativeBuilder
                        .injectedPlistKeys(request)
                        .contains("LSApplicationQueriesSchemes")) {
                    // The KEY, and then what its array actually lists. A
                    // fragment that merely mentions the name -- in a comment,
                    // or inside an unrelated string -- declares nothing, and
                    // reading that as a declaration skipped the hint and
                    // shipped a plist with no com.apple.Home entry at all,
                    // which is the exact outcome this block exists to
                    // prevent. A fragment that does list the scheme needs no
                    // warning either.
                    boolean listed = false;
                    for (String entry : WatchNativeBuilder
                            .injectedPlistStringArray(request,
                                    "LSApplicationQueriesSchemes")) {
                        if ("com.apple.Home".equals(entry)) {
                            listed = true;
                            break;
                        }
                    }
                    if (!listed) {
                        log("ios.plistInject already declares "
                                + "LSApplicationQueriesSchemes, so "
                                + "com.apple.Home was not added for you. Add "
                                + "it to that array or "
                                + "SmartHome.openEcosystemApp() will report "
                                + "the Apple Home app missing on devices that "
                                + "have it.");
                    }
                    homeSchemeDeclared = true;
                }
                if (!homeSchemeDeclared) {
                    request.putArgument("ios.applicationQueriesSchemes",
                            queries.trim().length() == 0 ? "com.apple.Home"
                                    : queries.trim() + ",com.apple.Home");
                }

                // iOS TERMINATES an app that creates HMHomeManager without a
                // usage description -- it does not fail gracefully, it kills
                // the process on launch. So this is a hard failure rather
                // than a defaulted placeholder, and for the same second
                // reason the health strings are: Apple reviews this text
                // against what the app actually does, so a generic string is
                // what gets it rejected, and injecting one would be a privacy
                // claim made in the developer's name.
                //
                // Trimmed, and blank counts as absent -- a hint present but
                // empty produces exactly the empty string iOS refuses.
                // "false" is this builder's way of suppressing a privacy
                // string -- the plist renderer skips any value equal to it --
                // so it means absent here, not present. Read as present, it
                // satisfied the requirement below and produced exactly the
                // app that requirement exists to prevent: linked, entitled,
                // and terminated on launch for a missing purpose string.
                // Either source: ios.plistInject is a supported way to
                // supply a purpose string, and reading only the direct hint
                // failed a build whose generated plist carries a perfectly
                // good disclosure. WatchNativeBuilder resolves the same two
                // sources for the watch.
                // Whichever of the two the RENDERER will use, not whichever
                // is set. ios.plistInject wins there, so a fragment carrying
                // this key as <false/> beside a perfectly good hint used to
                // pass -- and the plist shipped the false.
                String homeUsage = trimToNull(effectivePurposeString(request,
                        "ios.NSHomeKitUsageDescription"));
                if ("false".equalsIgnoreCase(homeUsage)) {
                    homeUsage = null;
                }
                if (homeUsage != null) {
                    request.putArgument("ios.NSHomeKitUsageDescription",
                            homeUsage);
                }
                if (usesHomeAccessoryData && homeUsage == null) {
                    throw new BuildException(
                        "This app uses com.codename1.home but declares no "
                      + "HomeKit privacy string.\n"
                      + "  Add ios.NSHomeKitUsageDescription=<why your app "
                      + "needs the user's home>\n"
                      + "to codenameone_settings.properties. If ios.plistInject "
                      + "declares the key, ITS value is the one that ships -- "
                      + "make that a nonblank string.\n"
                      + "iOS terminates "
                      + "an app that reaches HomeKit without it, and "
                      + "Codename One does not inject a placeholder: Apple "
                      + "reviews this text against your app's behaviour and "
                      + "rejects generic copy.");
                }

                // The entitlement, and ONLY for an app that touches the home.
                //
                // com.apple.developer.homekit has to be enabled on the App ID
                // and present in the provisioning profile, so entitling an
                // app that merely rendered "not supported on this device"
                // would fail its codesigning for a capability it never
                // wanted -- and the failure surfaces as an opaque codesign
                // error minutes into a cloud build. Exactly the trap the
                // HealthKit block below spells out.
                String homeEntitlement = request.getArg(
                        "ios.entitlements.com.apple.developer.homekit", null);
                if (usesHomeAccessoryData && homeEntitlement != null
                        && !"true".equalsIgnoreCase(homeEntitlement)) {
                    // Refused rather than overridden, because neither reading
                    // wins on its own: forcing it on contradicts an explicit
                    // instruction, and honouring it signs the app without the
                    // capability so every HomeKit call fails at runtime while
                    // the build looked perfectly healthy.
                    // Thrown, not logged. Executor.error only writes to the
                    // log and returns, so the build carried on and shipped
                    // the app the paragraph above says it must not -- the
                    // refusal has to be a refusal, like the missing
                    // usage-description path below.
                    throw new BuildException(
                            "This app uses com.codename1.home but sets "
                            + "ios.entitlements.com.apple.developer.homekit="
                            + homeEntitlement + ". HomeKit cannot be used "
                            + "without that entitlement: the app would be "
                            + "signed without the capability and every "
                            + "accessory call would fail at runtime. Remove "
                            + "the hint to have it added for you, set it to "
                            + "true, or stop touching the home.");
                }
                if (usesHomeAccessoryData && homeEntitlement == null) {
                    request.putArgument(
                        "ios.entitlements.com.apple.developer.homekit",
                        "true");
                }
                if ("true".equalsIgnoreCase(
                        request.getArg("ios.home.required", "false"))) {
                    String caps = request.getArg(
                            "ios.UIRequiredDeviceCapabilities", "");
                    if (!caps.contains("homekit")) {
                        request.putArgument("ios.UIRequiredDeviceCapabilities",
                                caps.length() == 0 ? "homekit"
                                        : caps + "," + "homekit");
                    }
                }
            }

            // Adding a Matter accessory. Everything here is skipped for an app
            // that only reads its lights, which is why commissioning lives in
            // a package of its own -- the scanner matches on a prefix and
            // cannot express an exclusion. matterExtensionEnabled is decided
            // above, before anything reads usesHomeAccessoryData.
            if (matterExtensionEnabled) {
                // The floor MatterSupport needs, raised here rather than in
                // the feature catalog: the catalog matches on a package
                // prefix and cannot see ios.home.commissioning=false, so an
                // app that deliberately excludes the framework would have
                // lost every iOS below 16.1 for nothing.
                // The APP's floor stays where MatterSupport put it. Only
                // the extension needs 16.4 for a fabric of its own, and an
                // extension whose target is above its host's is the ordinary
                // arrangement -- a widget extension does the same thing.
                // Raising the app to 16.4 would cost every user on 16.1
                // through 16.3 the whole application over an opt-in they
                // never asked for.
                addMinDeploymentTarget(MatterExtensionBuilder.DEPLOYMENT_TARGET);
                String ms = "MatterSupport.framework";
                if (addLibs == null || addLibs.length() == 0) {
                    addLibs = ms;
                } else if (!addLibs.toLowerCase().contains("mattersupport")) {
                    addLibs = addLibs + ";" + ms;
                }
                try {
                    replaceInFile(new File(buildinRes,
                            "CodenameOne_GLViewController.h"),
                            "//#define CN1_INCLUDE_MATTER_SETUP",
                            "#define CN1_INCLUDE_MATTER_SETUP");
                } catch (Exception ex) {
                    throw new BuildException(
                            "Failed to enable CN1_INCLUDE_MATTER_SETUP", ex);
                }
                if (matterOwnFabric(request)) {
                    // A fabric of this app's own needs Apple's Matter
                    // framework, which starts at iOS 16.4 -- so the extension
                    // is built for 16.4 while the app keeps the 16.1 floor
                    // above. What 16.1 through 16.3 must not get is a
                    // commissioning button that opens a sheet backed by an
                    // extension they cannot load: CN1SmartHome.m answers
                    // APP_HANDOFF rather than OS_UI there in an own-fabric
                    // build, and refuses the call itself, so those releases
                    // hand off to the Home app exactly as a build with no
                    // extension does.
                    //
                    // Same flip, for the half that decides what a successful
                    // flow means: with a fabric of this app's, the extension
                    // commissioned the accessory onto it or threw, and a
                    // throw is what would have failed the flow.
                    try {
                        replaceInFile(new File(buildinRes,
                                "CodenameOne_GLViewController.h"),
                                "//#define CN1_MATTER_OWN_FABRIC",
                                "#define CN1_MATTER_OWN_FABRIC");
                    } catch (Exception ex) {
                        throw new BuildException(
                                "Failed to enable CN1_MATTER_OWN_FABRIC", ex);
                    }
                }
                String setupPayloadEntitlement = request.getArg(
                        "ios.entitlements.com.apple.developer"
                        + ".matter.allow-setup-payload", null);
                if (setupPayloadEntitlement == null) {
                    request.putArgument("ios.entitlements.com.apple.developer"
                            + ".matter.allow-setup-payload", "true");
                } else if (!"true".equalsIgnoreCase(
                        setupPayloadEntitlement)) {
                    // Refused rather than honoured, for the same reason as
                    // the HomeKit entitlement above: the renderer would emit
                    // <false/> while everything else -- the framework, the
                    // extension target, the bridge -- is still built, and
                    // MatterAddDeviceRequest.perform() fails on the device
                    // with nothing at build time to explain it. The way to
                    // opt out is ios.home.commissioning=false, which turns
                    // the whole thing off.
                    throw new BuildException(
                            "This app adds Matter accessories but sets "
                            + "ios.entitlements.com.apple.developer.matter"
                            + ".allow-setup-payload="
                            + setupPayloadEntitlement + ". Commissioning "
                            + "cannot work without that entitlement. Remove "
                            + "the hint to have it added for you, set it to "
                            + "true, or set ios.home.commissioning=false to "
                            + "leave commissioning out of the build.");
                }
                // The app group is the only channel between the extension and
                // the app, and Apple refuses to launch an extension whose
                // group does not match its host's. ios.app_groups is the
                // established comma-separated hint the entitlements generator
                // already consumes, shared with the widgets flow.
                // Trimmed, and blank counts as absent. getArg keeps an
                // explicitly empty hint, and an empty group reaches the
                // extension's entitlements as "" -- which signs, and then
                // fails to launch, for a reason nothing reports.
                String matterGroup = request.getArg("ios.home.appGroup", "");
                matterGroup = matterGroup == null ? "" : matterGroup.trim();
                if (matterGroup.length() == 0) {
                    matterGroup = MatterExtensionBuilder.defaultAppGroup(
                            request.getPackageName());
                } else if (!matterGroup.startsWith("group.")) {
                    // Apple's own rule. Caught here rather than at codesign,
                    // where it is an opaque provisioning error.
                    throw new BuildException(
                            "ios.home.appGroup must be an app group "
                            + "identifier starting \"group.\", got \""
                            + matterGroup + "\".");
                }
                // Compared entry by entry, not as a substring: an app group
                // named group.com.acme.shared contains group.com.acme, and
                // deciding the group is already there on that basis entitles
                // the extension for one group and the host for another --
                // which fails signing, or launches an extension that cannot
                // reach its host.
                String appGroups = request.getArg("ios.app_groups", "");
                boolean present = false;
                for (String group : appGroups.split(",")) {
                    if (group.trim().equals(matterGroup)) {
                        present = true;
                        break;
                    }
                }
                if (!present) {
                    request.putArgument("ios.app_groups",
                            appGroups.trim().length() == 0 ? matterGroup
                                    : appGroups.trim() + "," + matterGroup);
                }
                matterAppGroup = matterGroup;
                // Commissioning talks to the accessory over BLE before it has
                // a network, and over mDNS afterwards. Both need declaring,
                // and the Bonjour service types are the ones the Matter
                // specification defines -- an app that omits them gets an
                // accessory that is found and then cannot be reached.
                String modes = request.getArg("ios.background_modes", "");
                if (!modes.contains("bluetooth-central")) {
                    modes = modes.length() == 0 ? "bluetooth-central"
                            : modes + ",bluetooth-central";
                    request.putArgument("ios.background_modes", modes);
                }
                // Through applyCatalogPlistEntry, which puts the value in
                // privacyUsageDescriptions as well as in the request. A bare
                // putArgument is too late to matter here: the one-time sweep
                // that copies ios.NS*UsageDescription hints into that map ran
                // long before this block, and the generated Info.plist is
                // written from the map -- so the defaults existed as build
                // arguments and never reached the device, leaving the Matter
                // flow to touch Bluetooth and the local network with no
                // declared purpose.
                // Refused before defaulting, because "false" suppresses the
                // string in the plist renderer while commissioning stays
                // fully enabled: the flow would reach Bluetooth and the local
                // network with no declaration, which iOS answers by killing
                // the app. ios.home.commissioning=false is the way to opt out
                // of the flow; there is no way to keep the flow and drop its
                // purpose strings.
                for (String matterPrivacyKey : new String[] {
                        "ios.NSBluetoothAlwaysUsageDescription",
                        "ios.NSLocalNetworkUsageDescription"}) {
                    // Blank is a refusal too, from either source. An empty
                    // hint survives applyCatalogPlistEntry -- it only fills a
                    // MISSING value -- and an empty string in ios.plistInject
                    // suppresses the generated default outright, so the app
                    // reaches Bluetooth with an empty purpose string, which
                    // iOS treats exactly as it treats none.
                    // The value the RENDERER will use. ios.plistInject wins there, and
                    // injectedPlistString alone cannot tell a key that is absent from one
                    // given <false/> -- the first takes the generated default, the second
                    // cannot, because the renderer drops the default for a key the fragment
                    // already carries. Read as "absent", <false/> shipped a commissioning app
                    // whose purpose string was the boolean false.
                    String supplied = effectivePurposeString(request, matterPrivacyKey);
                    if (supplied != null && supplied.trim().length() == 0) {
                        supplied = "false";
                    }
                    if ("false".equalsIgnoreCase(supplied)) {
                        throw new BuildException(
                                "This app adds Matter accessories but has "
                                + "no usable " + matterPrivacyKey + ": it is "
                                + "false, empty, or -- through "
                                + "ios.plistInject -- not a string at all. "
                                + "Commissioning "
                                + "uses Bluetooth to reach a new accessory "
                                + "and the local network to find it "
                                + "afterwards, and iOS terminates an app that "
                                + "does either without a purpose string -- "
                                + "and an empty one is no string at all. "
                                + "Supply one, or set "
                                + "ios.home.commissioning=false.");
                    }
                }
                applyCatalogPlistEntry(request, new String[] {
                    "NSBluetoothAlwaysUsageDescription",
                    "Used to set up new smart home accessories."});
                applyCatalogPlistEntry(request, new String[] {
                    "NSLocalNetworkUsageDescription",
                    "Used to find smart home accessories on your network."});
                // Each service considered on its own. Matter needs both --
                // _matterc._udp. to find a commissionable accessory and
                // _matter._tcp. to talk to it afterwards -- and a project
                // that already declared one used to suppress the other, so
                // the accessory was discovered and then unreachable.
                // A project that declares the array through ios.plistInject
                // owns it: the plist renderer emits the generated array only
                // when the injected fragment has no NSBonjourServices key,
                // because a plist with the key twice keeps neither value
                // reliably. So the hint below would be written and then
                // silently dropped, and the build would ship a commissioning
                // app that cannot see a new accessory -- iOS 14 and later
                // drop mDNS traffic for a service type the plist does not
                // list. Refused instead, naming the service to add: the
                // fragment is the developer's own XML and rewriting it here
                // would be guessing at their formatting.
                // The key, not its name anywhere in the fragment. A
                // comment that mentions NSBonjourServices declares nothing,
                // and taking it for a declaration refused a build whose
                // plist was fine while suppressing the array the app needs.
                if (WatchNativeBuilder.injectedPlistKeys(request)
                        .contains("NSBonjourServices")) {
                    List<String> declared = WatchNativeBuilder
                            .injectedPlistStringArray(request,
                                    "NSBonjourServices");
                    for (String service : new String[] {"_matter._tcp",
                            "_matterc._udp"}) {
                        // Whole entries, not a substring of the fragment: a
                        // comment mentioning the service, or a longer name
                        // like _matter._tcp.preview., is not the service type
                        // iOS matches mDNS traffic against.
                        boolean serviceDeclared = false;
                        for (String entry : declared) {
                            if (entry.equals(service)
                                    || entry.equals(service + ".")) {
                                serviceDeclared = true;
                                break;
                            }
                        }
                        if (!serviceDeclared) {
                            throw new BuildException(
                                    "This app adds Matter accessories and "
                                    + "declares NSBonjourServices through "
                                    + "ios.plistInject, but that array does "
                                    + "not list " + service + ". iOS drops "
                                    + "mDNS traffic for a service type the "
                                    + "plist does not name, so commissioning "
                                    + "would never find an accessory. Add "
                                    + service + ". to the array in "
                                    + "ios.plistInject, or remove the key "
                                    + "from it and let the build declare the "
                                    + "array through ios.NSBonjourServices.");
                        }
                    }
                }
                String bonjour = request.getArg("ios.NSBonjourServices", "");
                for (String service : new String[] {"_matter._tcp.",
                        "_matterc._udp."}) {
                    boolean declared = false;
                    for (String existing : bonjour.split(",")) {
                        String trimmed = existing.trim();
                        // With and without the trailing dot: both spellings
                        // appear in the wild and mean the same service.
                        if (trimmed.equals(service)
                                || (trimmed + ".").equals(service)) {
                            declared = true;
                            break;
                        }
                    }
                    if (!declared) {
                        bonjour = bonjour.trim().length() == 0 ? service
                                : bonjour.trim() + "," + service;
                    }
                }
                request.putArgument("ios.NSBonjourServices", bonjour);
            }

            // First-class health (com.codename1.health.*). Gated on
            // usesHealthStore, NOT usesHealth: an app that only streams a
            // heart-rate strap through com.codename1.health.sensors is
            // doing ordinary BLE and must not acquire HealthKit, its
            // entitlement, or an App Store health-data review.
            if (usesHealthStore) {
                // Trimmed, and blank counts as absent. A hint present
                // but empty produced an empty purpose string, which is
                // exactly what App Review rejects and what iOS enforces at
                // runtime -- the validation existed to prevent that.
                String share = trimToNull(request.getArg(
                        "ios.NSHealthShareUsageDescription", null));
                String update = trimToNull(request.getArg(
                        "ios.NSHealthUpdateUsageDescription", null));
                if (share != null) {
                    request.putArgument("ios.NSHealthShareUsageDescription",
                            share);
                }
                if (update != null) {
                    request.putArgument("ios.NSHealthUpdateUsageDescription",
                            update);
                }

                // Deliberately a hard failure rather than a defaulted
                // placeholder. Apple reviews health purpose strings against
                // what the app actually does, so a generic string is
                // precisely what gets the app rejected -- injecting one
                // would also be a privacy claim made in the developer's
                // name. Compare the camera/bluetooth entries in
                // PlatformFeatureCatalog, which do default their strings.
                // Availability alone needs no purpose string: checking
                // whether HKHealthStore exists reads nothing, so there is
                // no truthful text to demand. HealthKit still links.
                if (!usesHealthRead && !usesHealthWrite) {
                    // nothing to validate
                } else if (share == null && update == null) {
                    throw new BuildException(
                        "This app uses com.codename1.health but declares no "
                      + "HealthKit privacy strings.\n"
                      + "  Add ios.NSHealthShareUsageDescription=<why your "
                      + "app reads health data>\n"
                      + "  and/or ios.NSHealthUpdateUsageDescription=<why "
                      + "your app writes it>\n"
                      + "to codenameone_settings.properties. Codename One "
                      + "does not inject a placeholder: Apple reviews this "
                      + "text against your app's behaviour and rejects "
                      + "generic copy.");
                }
                if (usesHealthRead && share == null) {
                    throw new BuildException(
                        "This app reads health data (com.codename1.health "
                      + "read/aggregate/subscribe) but declares no "
                      + "ios.NSHealthShareUsageDescription build hint. "
                      + "iOS terminates the app when it reads HealthKit "
                      + "without it; the update string does not cover "
                      + "reads.");
                }
                if (usesHealthWrite && update == null) {
                    throw new BuildException(
                        "This app writes health data (com.codename1.health "
                      + "write/delete) but declares no "
                      + "ios.NSHealthUpdateUsageDescription build hint. "
                      + "HealthKit write authorization cannot be requested "
                      + "without it.");
                }

                String hk = "HealthKit.framework";
                if (addLibs == null || addLibs.length() == 0) {
                    addLibs = hk;
                } else if (!addLibs.toLowerCase().contains("healthkit")) {
                    addLibs = addLibs + ";" + hk;
                }
                try {
                    replaceInFile(new File(buildinRes,
                            "CodenameOne_GLViewController.h"),
                            "//#define CN1_INCLUDE_HEALTH",
                            "#define CN1_INCLUDE_HEALTH");
                } catch (Exception ex) {
                    throw new BuildException(
                            "Failed to enable CN1_INCLUDE_HEALTH", ex);
                }

                // HealthKit is entitlement-gated, unlike CoreBluetooth --
                // this has no Bluetooth precedent. The profile must also
                // carry the capability; without it the failure surfaces
                // much later as an opaque codesign error.
                //
                // Only for an app that touches health *data*. An
                // availability-only app is deliberately allowed to build
                // without purpose strings, because it accesses nothing --
                // so entitling it demanded a provisioning profile with a
                // capability its App ID may never have enabled, and an
                // otherwise harmless getAvailability() call failed
                // codesigning.
                // The explicit sub-capability hints count as usage too.
                // Each block below emits its sub-entitlement from the hint
                // alone, and a HealthKit sub-capability without
                // com.apple.developer.healthkit beneath it is not a
                // capability Apple can enable -- so an availability-only
                // app asking for background delivery produced an
                // entitlement set that could not be signed against a
                // profile and would not have worked if it had been.
                // The long-form keys count too. A sub-entitlement can be
                // asked for either through the ios.health.* alias or
                // written out in the ios.entitlements.* namespace, and
                // the generic renderer emits whatever is in that
                // namespace -- so an availability-only app that spelled
                // it out in full got its sub-capability emitted while
                // this gate, reading only the aliases, left
                // com.apple.developer.healthkit off. That is the same
                // unsignable entitlement set the aliases were fixed to
                // avoid, reachable by the other spelling.
                // The one expression, shared with the watch builder. Two copies of it came apart
                // once already: the watch read the scanner flags alone, so a project whose health
                // access is in native code -- declared only through the capability hints -- got an
                // entitled phone and an unentitled watch.
                boolean entitleHealthKit = phoneUsesHealthData(request);
                String healthKitEntitlement = request.getArg(
                        "ios.entitlements.com.apple.developer.healthkit",
                        null);
                if (entitleHealthKit && healthKitEntitlement != null
                        && !"true".equalsIgnoreCase(healthKitEntitlement)) {
                    // Refused rather than overridden. The app calls the
                    // health store and the hint says not to entitle it,
                    // and neither reading wins on its own: silently
                    // forcing the entitlement on contradicts an explicit
                    // instruction, while honouring it signs the app with
                    // <false/> and every authorization request fails at
                    // runtime with the build having looked perfectly
                    // healthy. A missing HealthKit capability is a
                    // developer bug this feature already fails the build
                    // over -- see the usage strings -- so it fails here
                    // too, saying which two things disagree.
                    error("This app uses com.codename1.health but sets "
                            + "ios.entitlements.com.apple.developer"
                            + ".healthkit=" + healthKitEntitlement
                            + ". HealthKit cannot be used without that "
                            + "entitlement: the app would be signed "
                            + "without the capability and every "
                            + "authorization request would fail at "
                            + "runtime. Remove the hint to have it added "
                            + "for you, set it to true, or stop calling "
                            + "the health store.",
                            new RuntimeException(
                                "healthkit entitlement disabled"));
                }
                if (entitleHealthKit && healthKitEntitlement == null) {
                    request.putArgument(
                        "ios.entitlements.com.apple.developer.healthkit",
                        "true");
                }
                boolean bgDelivery = "true".equalsIgnoreCase(
                        request.getArg("ios.health.backgroundDelivery",
                                "false"));
                if (bgDelivery && request.getArg(
                        "ios.entitlements.com.apple.developer.healthkit"
                        + ".background-delivery", null) == null) {
                    request.putArgument(
                        "ios.entitlements.com.apple.developer.healthkit"
                        + ".background-delivery", "true");
                }
                if ("true".equalsIgnoreCase(request.getArg(
                        "ios.health.recalibrateEstimates", "false"))
                        && request.getArg(
                            "ios.entitlements.com.apple.developer.healthkit"
                            + ".recalibrate-estimates", null) == null) {
                    request.putArgument(
                        "ios.entitlements.com.apple.developer.healthkit"
                        + ".recalibrate-estimates", "true");
                }
                if ("true".equalsIgnoreCase(
                        request.getArg("ios.health.required", "false"))) {
                    String caps = request.getArg(
                            "ios.UIRequiredDeviceCapabilities", "");
                    if (!caps.contains("healthkit")) {
                        request.putArgument("ios.UIRequiredDeviceCapabilities",
                                caps.length() == 0 ? "healthkit"
                                        : caps + "," + "healthkit");
                    }
                }
            }

            // Uncomment INCLUDE_CN1_CAMERA in CodenameOne_GLViewController.h
            // so the com.codename1.camera native bridge (CN1Camera.{h,m})
            // compiles in. This is deliberately independent of
            // INCLUDE_CAMERA_USAGE (the old modal Capture API): the new
            // AVFoundation natives are only built when the app actually
            // references com.codename1.camera.*, matching the AVFoundation
            // framework injection driven by the same scan via PlatformFeatureCatalog.
            if (usesCn1Camera) {
                try {
                    replaceInFile(new File(buildinRes,
                            "CodenameOne_GLViewController.h"),
                            "//#define INCLUDE_CN1_CAMERA",
                            "#define INCLUDE_CN1_CAMERA");
                } catch (IOException ex) {
                    throw new BuildException(
                            "Failed to enable INCLUDE_CN1_CAMERA", ex);
                }
            }

            // Augmented reality: uncomment INCLUDE_CN1_AR so the CN1AR
            // natives (ARKit + ARSCNView) compile in, and link ARKit /
            // SceneKit explicitly -- neither is default-linked and the
            // Catalog frameworks are linked below. This explicit AR block also
            // controls the native compile define and remains for compatibility.
            // Apps that never reference com.codename1.ar leave the define
            // commented out so no ARKit symbol is referenced, which keeps
            // Apple's API-usage scan quiet and tvOS/watchOS slices clean.
            if (usesCn1Ar) {
                try {
                    replaceInFile(new File(buildinRes,
                            "CodenameOne_GLViewController.h"),
                            "//#define INCLUDE_CN1_AR",
                            "#define INCLUDE_CN1_AR");
                } catch (IOException ex) {
                    throw new BuildException(
                            "Failed to enable INCLUDE_CN1_AR", ex);
                }
                String arLibs = "ARKit.framework;SceneKit.framework";
                if (addLibs == null || addLibs.length() == 0) {
                    addLibs = arLibs;
                } else if (!addLibs.toLowerCase().contains("arkit.framework")) {
                    addLibs = addLibs + ";" + arLibs;
                }
            }

            for (String framework : aiAcc.iosFrameworks()) {
                addLibs = appendFrameworks(addLibs,
                        framework + ".framework");
            }

            if (usesCn1Vision) {
                try {
                    replaceInFile(new File(buildinRes,
                            "CodenameOne_GLViewController.h"),
                            "//#define INCLUDE_CN1_VISION",
                            "#define INCLUDE_CN1_VISION");
                } catch (IOException ex) {
                    throw new BuildException(
                            "Failed to enable INCLUDE_CN1_VISION", ex);
                }
                addLibs = appendFrameworks(addLibs, "Vision.framework",
                        "CoreImage.framework", "CoreVideo.framework");
            }

            if (usesCn1Language) {
                try {
                    replaceInFile(new File(buildinRes,
                            "CodenameOne_GLViewController.h"),
                            "//#define INCLUDE_CN1_LANGUAGE",
                            "#define INCLUDE_CN1_LANGUAGE");
                } catch (IOException ex) {
                    throw new BuildException(
                            "Failed to enable INCLUDE_CN1_LANGUAGE", ex);
                }
                addLibs = appendFrameworks(addLibs,
                        "NaturalLanguage.framework");
            }

            if (usesCn1Inference) {
                try {
                    replaceInFile(new File(buildinRes,
                            "CodenameOne_GLViewController.h"),
                            "//#define INCLUDE_CN1_INFERENCE",
                            "#define INCLUDE_CN1_INFERENCE");
                } catch (IOException ex) {
                    throw new BuildException(
                            "Failed to enable INCLUDE_CN1_INFERENCE", ex);
                }
                addLibs = appendFrameworks(addLibs, "CoreML.framework",
                        "Metal.framework", "Accelerate.framework");
            }

            // CarPlay: link CarPlay.framework (+ MediaPlayer for the now-playing template) and
            // inject the per-category carplay entitlement. The CarPlay entitlements are granted by
            // Apple per app category, so we only inject the ones the project opts into via the
            // ios.carplay.<category> build hints; the binary references CarPlay symbols (gated by
            // CN1_USE_CARPLAY) which is why the framework is linked here in lockstep with the scan.
            // The phone-to-watch link references WCSession (gated by CN1_USE_WATCHCONNECTIVITY), so
            // link WatchConnectivity.framework in lockstep with the scan. It exists on both iOS and
            // watchOS, which is why it is a plain link rather than one of the watch slice's
            // weak-linked frameworks.
            if (usesWearable || surfacesWatchEnabled) {
                String wearableLib = "WatchConnectivity.framework";
                if (addLibs == null || addLibs.length() == 0) {
                    addLibs = wearableLib;
                } else if (!addLibs.toLowerCase().contains("watchconnectivity.framework")) {
                    addLibs = addLibs + ";" + wearableLib;
                }
            }

            if (usesCar) {
                String carPlayLibs = "CarPlay.framework;MediaPlayer.framework";
                if (addLibs == null || addLibs.length() == 0) {
                    addLibs = carPlayLibs;
                } else if (!addLibs.toLowerCase().contains("carplay.framework")) {
                    addLibs = addLibs + ";" + carPlayLibs;
                }
                // ios.carplay.audio / .messaging / .navigation / .poi -> the matching Apple CarPlay
                // entitlement. Default to the audio entitlement when no category is specified so a
                // basic browse/now-playing app works out of the box.
                boolean audio = request.getArg("ios.carplay.audio", "false").equals("true");
                boolean messaging = request.getArg("ios.carplay.messaging", "false").equals("true");
                boolean navigation = request.getArg("ios.carplay.navigation", "false").equals("true");
                boolean poi = request.getArg("ios.carplay.poi", "false").equals("true");
                if (!audio && !messaging && !navigation && !poi) {
                    audio = true;
                }
                if (audio) {
                    putCarPlayEntitlement(request, "com.apple.developer.carplay-audio");
                }
                if (messaging) {
                    putCarPlayEntitlement(request, "com.apple.developer.carplay-communication");
                }
                if (navigation) {
                    putCarPlayEntitlement(request, "com.apple.developer.carplay-maps");
                }
                if (poi) {
                    putCarPlayEntitlement(request, "com.apple.developer.carplay-driving-task");
                }
            }

            // External surfaces: the app target needs the shared App Group in its own
            // entitlements (the CN1Widgets extension gets it through its generated
            // .entitlements file). ios.app_groups is the established comma-separated hint the
            // cloud builder's entitlement generator consumes; local Xcode builds supply the app
            // entitlements externally via $(APP_CODE_SIGN_ENTITLEMENTS), matching the wallet
            // extension flow.
            // surfacesWatchEnabled counts too. Without the entitlement the group container does
            // not resolve on the phone, so areWidgetsSupported() answers false and
            // Surfaces.publish() returns before the bridge is reached at all -- taking the watch
            // mirror with it. A manifest of nothing but complications would then be unable to
            // update the very complications it declares, which is the one case this feature is
            // for. The group is genuinely part of the plumbing on both bundles, not an unused
            // capability.
            if (surfacesExtensionEnabled || surfacesWatchEnabled) {
                String appGroups = request.getArg("ios.app_groups", "");
                if (!declaresAppGroup(appGroups, surfacesAppGroup)) {
                    request.putArgument("ios.app_groups", appGroups.length() == 0
                            ? surfacesAppGroup : appGroups + "," + surfacesAppGroup);
                }
            }

            // Sign in with Apple requires the
            // com.apple.developer.applesignin entitlement; Apple rejects
            // builds whose binary references ASAuthorizationAppleIDProvider
            // without it. Inject the canonical "Default" value automatically.
            if (usesAppleSignIn) {
                if (request.getArg(
                        "ios.entitlements.com.apple.developer.applesignin",
                        null) == null) {
                    request.putArgument(
                            "ios.entitlements.com.apple.developer.applesignin",
                            "Default");
                }
            }

            // App Attest requires the appattest-environment entitlement; defaults
            // to development for debug builds, production otherwise. Override with
            // ios.appAttest.environment.
            if (appAttest && request.getArg(
                    "ios.entitlements.com.apple.developer.devicecheck.appattest-environment",
                    null) == null) {
                String appAttestEnv = request.getArg("ios.appAttest.environment",
                        request.getArg("ios.buildType", "debug").equals("debug") ? "development" : "production");
                request.putArgument(
                        "ios.entitlements.com.apple.developer.devicecheck.appattest-environment",
                        appAttestEnv);
            }

            // Time-sensitive / critical notification entitlements. These require a
            // matching capability to be enabled on the Apple App ID, so auto-injecting them
            // from mere notification usage would break code signing for apps that have not
            // provisioned the capability. They are therefore opt-in via build hints:
            //   ios.timeSensitiveNotifications=true -> com.apple.developer.usernotifications.time-sensitive
            //   ios.criticalAlerts=true             -> com.apple.developer.usernotifications.critical-alerts
            if ("true".equals(request.getArg("ios.timeSensitiveNotifications", "false"))
                    && request.getArg("ios.entitlements.com.apple.developer.usernotifications.time-sensitive", null) == null) {
                request.putArgument("ios.entitlements.com.apple.developer.usernotifications.time-sensitive", "true");
            }
            if ("true".equals(request.getArg("ios.criticalAlerts", "false"))
                    && request.getArg("ios.entitlements.com.apple.developer.usernotifications.critical-alerts", null) == null) {
                request.putArgument("ios.entitlements.com.apple.developer.usernotifications.critical-alerts", "true");
            }

            // Deeper-network connectivity (WiFi info / NEHotspotConfiguration
            // / Bonjour). Each block is gated on a scanner flag so apps that
            // never touch the API see no entitlement or plist changes -- this
            // keeps the App Store review process clean. Developers can
            // override any value via the matching ios.* / ios.entitlements.*
            // build hint.
            if (usesWifiInfo) {
                // Reading SSID/BSSID on iOS 13+ requires the wifi-info
                // entitlement AND a granted CoreLocation authorization.
                if (request.getArg(
                        "ios.entitlements.com.apple.developer.networking.wifi-info",
                        null) == null) {
                    request.putArgument(
                            "ios.entitlements.com.apple.developer.networking.wifi-info",
                            "true");
                }
                // CoreLocation is what iOS checks behind the scenes for
                // CNCopyCurrentNetworkInfo. Default the description if the
                // developer did not set one; the user-facing prompt comes
                // from this string.
                if (request.getArg("ios.locationUsageDescription", null) == null
                        && request.getArg("ios.NSLocationWhenInUseUsageDescription", null) == null) {
                    request.putArgument("ios.locationUsageDescription",
                            "Allow access to your location to read the current Wi-Fi network name.");
                }
                // Light up the CaptiveNetwork SSID/BSSID code path. Apps
                // that don't reference com.codename1.io.wifi.WiFi ship
                // without any CaptiveNetwork symbols.
                try {
                    replaceInFile(new File(buildinRes, "IOSNative.m"),
                            "//#define CN1_INCLUDE_WIFI_INFO",
                            "#define CN1_INCLUDE_WIFI_INFO");
                } catch (IOException ex) {
                    throw new BuildException(
                            "Failed to enable CN1_INCLUDE_WIFI_INFO", ex);
                }
            }
            if (usesWifiHotspotConfig) {
                if (request.getArg(
                        "ios.entitlements.com.apple.developer.networking.HotspotConfiguration",
                        null) == null) {
                    request.putArgument(
                            "ios.entitlements.com.apple.developer.networking.HotspotConfiguration",
                            "true");
                }
                // Light up NetworkExtension.framework only when the app uses
                // hotspot config. The conditional #define keeps stock apps
                // free of NetworkExtension symbols so the App Store API-usage
                // scanner does not flag it.
                try {
                    replaceInFile(new File(buildinRes,
                            "IOSNative.m"),
                            "//#define CN1_INCLUDE_HOTSPOT",
                            "#define CN1_INCLUDE_HOTSPOT");
                } catch (IOException ex) {
                    throw new BuildException(
                            "Failed to enable CN1_INCLUDE_HOTSPOT", ex);
                }
                if (addLibs == null || addLibs.length() == 0) {
                    addLibs = "NetworkExtension.framework";
                } else if (!addLibs.contains("NetworkExtension")) {
                    addLibs += ";NetworkExtension.framework";
                }
            }
            if (usesBonjour) {
                // iOS 14 requires NSLocalNetworkUsageDescription before any
                // mDNS traffic can flow; without it discovery silently
                // returns nothing.
                if (request.getArg("ios.NSLocalNetworkUsageDescription", null) == null) {
                    request.putArgument("ios.NSLocalNetworkUsageDescription",
                            "Allow access to devices on your local network to discover services advertised via Bonjour.");
                }
                // The NSBonjourServices array enumerates the service types
                // the app expects to discover. We seed it with HTTP since
                // that's the most common; developers should add specific
                // types via ios.NSBonjourServices = "_myapp._tcp.,_http._tcp."
                if (request.getArg("ios.NSBonjourServices", null) == null) {
                    request.putArgument("ios.NSBonjourServices", "_http._tcp.");
                }
                // Light up the NSNetServiceBrowser/NSNetService code path.
                // Apps that never call BonjourBrowser/BonjourPublisher
                // ship without the delegate, so the iOS 14 local-network
                // privacy prompt is not triggered for them.
                try {
                    replaceInFile(new File(buildinRes, "IOSNative.m"),
                            "//#define CN1_INCLUDE_BONJOUR",
                            "#define CN1_INCLUDE_BONJOUR");
                } catch (IOException ex) {
                    throw new BuildException(
                            "Failed to enable CN1_INCLUDE_BONJOUR", ex);
                }
            }

            // HCE on iOS requires the iOS 17.4+ EU-only CardSession
            // entitlement plus the AIDs to register. We inject the
            // entitlement when the scanner saw HostCardEmulationService.
            if (usesNfcHce) {
                if (request.getArg(
                        "ios.entitlements.com.apple.developer.nfc.hce",
                        null) == null) {
                    request.putArgument(
                            "ios.entitlements.com.apple.developer.nfc.hce",
                            "true");
                }
                String aids = request.getArg("ios.hceAids",
                        request.getArg("android.hceAids", null));
                if (aids != null && aids.length() > 0
                        && request.getArg(
                            "ios.entitlements.com.apple.developer.nfc.hce.iso7816.select-identifiers",
                            null) == null) {
                    StringBuilder list = new StringBuilder();
                    for (String aid : aids.split("[,;]")) {
                        aid = aid.trim();
                        if (aid.length() == 0) {
                            continue;
                        }
                        if (list.length() > 0) {
                            list.append("\n");
                        }
                        list.append(aid);
                    }
                    request.putArgument(
                            "ios.entitlements.com.apple.developer.nfc.hce.iso7816.select-identifiers",
                            list.toString());
                }
            }

            try {
                if (!runPods && googleAdUnitId != null && googleAdUnitId.length() > 0) {
                    unzip(getResourceAsStream("/google-play-services_lib-ios.zip"), classesDir, buildinRes, buildinRes);
                    if (addLibs == null || addLibs.length() == 0) {
                        addLibs = "AdSupport.framework;SystemConfiguration.framework;StoreKit.framework;CoreTelephony.framework";
                    } else {
                        addLibs = addLibs + ";AdSupport.framework;SystemConfiguration.framework;StoreKit.framework;CoreTelephony.framework";
                    }
                }

                if (enableGalleryMultiselect && photoLibraryUsage && usePhotoKitForMultigallery) {
                    if (addLibs == null || addLibs.length() == 0) {
                        addLibs = "PhotosUI.framework";
                    } else {
                        addLibs += ";PhotosUI.framework";
                    }
                }

                if ((includePush || usesLocalNotifications) && xcodeVersion >= 9) {
                    if (addLibs == null) {
                        addLibs = "UserNotifications.framework";
                    } else {
                        addLibs += ";UserNotifications.framework";
                    }
                }

                // BackgroundTasks.framework (BGTaskScheduler / BGProcessingTaskRequest,
                // iOS 13+) is referenced unconditionally by the IOSNative background
                // processing bridge, so it must always be linked.
                if (addLibs == null) {
                    addLibs = "BackgroundTasks.framework";
                } else if (!addLibs.toLowerCase().contains("backgroundtasks")) {
                    addLibs += ";BackgroundTasks.framework";
                }

                // Core Spotlight backs the indexing half of com.codename1.intents. It is
                // Objective-C and available well below this port's floor, so linking it costs
                // an app nothing in minimum version. App Intents is autolinked by Swift and
                // needs no entry here.
                if (usesIntents) {
                    if (addLibs == null) {
                        addLibs = "CoreSpotlight.framework";
                    } else if (!addLibs.toLowerCase().contains("corespotlight")) {
                        addLibs += ";CoreSpotlight.framework";
                    }
                }

                if (request.getArg("ios.useJavascriptCore", "false").equalsIgnoreCase("true")) {
                    replaceInFile(new File(buildinRes, "CodenameOne_GLViewController.h"), "//#define CN1_USE_JAVASCRIPTCORE", "#define CN1_USE_JAVASCRIPTCORE");
                    if (addLibs == null) {
                        addLibs = "JavascriptCore.framework";
                    } else {
                        addLibs += ";JavascriptCore.framework";
                    }
                }
                if (request.getArg("ios.useAVKit", "true").equalsIgnoreCase("true")) {

                    replaceInFile(new File(buildinRes, "IOSNative.m"), "//#define CN1_USE_AVKIT", "#define CN1_USE_AVKIT");
                }

                if (enableWKWebView) {
                    if (addLibs == null) {
                        addLibs = "WebKit.framework";
                    } else {
                        addLibs += ";WebKit.framework";
                    }
                }

                if (addLibs == null || addLibs.length() == 0) {
                    addLibs = "CoreImage.framework;QuartzCore.framework";
                } else {
                    if (addLibs.indexOf("CoreImage.framework") < 0) {
                        addLibs += ";CoreImage.framework";
                    }
                    if (addLibs.indexOf("QuartzCore.framework") < 0) {
                        addLibs += ";QuartzCore.framework";
                    }
                }

                if (usesPurchaseAPI) {
                    addLibs += ";StoreKit.framework";
                }
                // App review (SKStoreReviewController) also lives in StoreKit;
                // link it when detected unless the purchase API already did.
                if (usesAppReview && (addLibs == null || addLibs.toLowerCase().indexOf("storekit.framework") < 0)) {
                    if (addLibs == null || addLibs.length() == 0) {
                        addLibs = "StoreKit.framework";
                    } else {
                        addLibs += ";StoreKit.framework";
                    }
                }
            } catch (Exception ex) {
                throw new BuildException("Failed to process build hints", ex);
            }

            File userDir = new File(System.getProperty("user.dir"));
             String parparVMCompilerJar = null;
            try {
                File parparVMCompilerJarFile = getResourceAsFile("/parparvm-compiler.jar", ".jar");
                parparVMCompilerJar = parparVMCompilerJarFile.getAbsolutePath();
            } catch (IOException ex) {
                throw new BuildException("Failed to extract parparvm-compiler.jar", ex);
            }




            try {
                unzip(getResourceAsStream("/parparvm-java-api.jar"), classesDir, classesDir, classesDir);
            } catch (IOException ex) {
                throw new BuildException("Failed to load JavaAPI.jar");
            }

            String optimizerOn = request.getArg("ios.optimizer", "on");
            HashMap<String, String> env = new HashMap<String, String>();
            env.put("optimizer", optimizerOn);
            if(request.getArg("ios.superfastBuild", "false").equals("true")) {
                env.put("concatenateFiles", "true");
            }

            String fieldNullChecks = Boolean.valueOf(request.getArg("ios.fieldNullChecks", "false")) ? "true":"false";

            // includeNullChecks enables null checks on everything else (methods, arrays, etc..)
            String includeNullChecks = Boolean.valueOf(request.getArg("ios.includeNullChecks", "true")) ? "true":"false";
            String bundleVersionNumber = request.getArg("ios.bundleVersion", buildVersion);
            // On-device-debug toggle: tells the translator to emit per-frame
            // locals-address tables, the cn1_frame_info side-tables, and to
            // flip the CN1_ON_DEVICE_DEBUG #define in cn1_globals.h so the
            // generated Xcode build links the listener thread. Force-off on
            // release builds so a stray hint in codenameone_settings.properties
            // can't leak the debug listener into an App Store binary.
            boolean isReleaseBuild = !request.getArg("ios.buildType", "debug").equals("debug");
            String onDeviceDebug = !isReleaseBuild
                    && Boolean.valueOf(request.getArg("ios.onDeviceDebug", "false")) ? "true" : "false";


            if (enableGalleryMultiselect && photoLibraryUsage) {
                addMinDeploymentTarget("8.0");
            }
            if (enableWKWebView) {
                addMinDeploymentTarget("8.0");
            }

            debug("iosDeploymentTargetMajorVersionInt="+getDeploymentTargetInt(request));

            debug("Building using addLibs="+addLibs);
            stopwatch.split("Prepare ParparVM");
            try {
                List<String> parparCmd = new ArrayList<String>();
                parparCmd.add("java");
                parparCmd.add("-DsaveUnitTests=" + isUnitTestMode());
                parparCmd.add("-DfieldNullChecks=" + fieldNullChecks);
                parparCmd.add("-DINCLUDE_NPE_CHECKS=" + includeNullChecks);
                // Both keyed on the cipher flag, not on usesDatabase, and that is deliberate on
                // iOS: cn1.sqlite emits the bundled engine, and this platform already has one.
                // ByteCodeTranslator links the system libsqlite3.dylib whenever the cipher is off,
                // and the bindings in cn1_db_sqlite_impl.h resolve against it, so an application
                // that uses com.codename1.db without encryption gets a working database and pays
                // nothing for a second copy of SQLite. Emitting the bundle here would put two
                // implementations in one process. Encryption is the only thing the system engine
                // cannot do, which is why turning it on switches both flags at once.
                parparCmd.add("-Dcn1.sqlite=" + usesDatabaseCipher);
                parparCmd.add("-Dcn1.sqlcipher=" + usesDatabaseCipher);
                parparCmd.add("-Dcn1.onDeviceDebug=" + onDeviceDebug);
                parparCmd.add("-DbundleVersionNumber=" + bundleVersionNumber);
                // The UNION of every enabled product's list, in ONE argument. These used to be
                // mutually exclusive branches on the claim that the Mac list already covered the
                // others; it does not -- the watch additionally needs Metal, MapKit, WebKit,
                // StoreKit, CarPlay and SceneKit weak-linked. With macNative and a companion watch
                // both enabled, the phone archive builds the watch target as a dependency and its
                // link failed on symbols nobody had marked optional. Only one
                // -Doptional.frameworks can take effect, so the lists are merged rather than added
                // twice. Weak-linking a framework a slice does not need costs that slice nothing,
                // which is why the union is safe.
                java.util.LinkedHashSet<String> optionalFrameworks =
                        new java.util.LinkedHashSet<String>();
                if (macNativeBuilder.isEnabled()) {
                    collectOptionalFrameworks(optionalFrameworks,
                            macNativeBuilder.parparvmOptionalFrameworksArg());
                }
                if (watchNativeBuilder.isEnabled()) {
                    collectOptionalFrameworks(optionalFrameworks,
                            watchNativeBuilder.parparvmOptionalFrameworksArg());
                }
                if (tvNativeBuilder.isEnabled()) {
                    collectOptionalFrameworks(optionalFrameworks,
                            tvNativeBuilder.parparvmOptionalFrameworksArg());
                }
                if (!optionalFrameworks.isEmpty()) {
                    StringBuilder frameworksArg = new StringBuilder("-Doptional.frameworks=");
                    boolean firstFramework = true;
                    for (String framework : optionalFrameworks) {
                        if (!firstFramework) {
                            frameworksArg.append(';');
                        }
                        frameworksArg.append(framework);
                        firstFramework = false;
                    }
                    parparCmd.add(frameworksArg.toString());
                }
                // Pass through extra translator JVM options (notably a larger
                // -Xmx) from the CN1_TRANSLATOR_OPTS environment variable. The
                // forked JVM does not inherit the Maven process's -D properties,
                // so this is the only way to reach the translator for tuning.
                java.util.List<String> translatorOpts = TranslatorHeap.extraJvmOptions();
                parparCmd.addAll(translatorOpts);
                boolean heapOverridden = TranslatorHeap.specifiesHeap(translatorOpts);
                // Heap sized from the machine (see TranslatorHeap), floored at the
                // 1024m the cloud builder has always used; a -Xmx in
                // CN1_TRANSLATOR_OPTS still takes precedence. The dead-code cull
                // builds an in-memory suffix automaton over all native symbols
                // (NativeSymbolIndex, from #5236) to avoid the old O(N^2) substring
                // scan that timed out on large apps -- that index trades time for
                // memory, so the historical 384m cap OOMed local iOS builds as the
                // CN1 class count grew (issue #5344) and even 1024m is not enough
                // for a large app (issue #5511).
                int heapMB = TranslatorHeap.maxHeapMB(1024);
                if (!heapOverridden) {
                    parparCmd.add("-Xmx" + heapMB + "m");
                }
                NativeVerifyOption.addTo(parparCmd, request, "ios");
                parparCmd.add("-jar");
                parparCmd.add(parparVMCompilerJar);
                parparCmd.add("ios");
                // The phone stub's directory joins the classpath only when the stubs were
                // separated; without a watch translation the classpath is exactly what it was.
                parparCmd.add(classesDir.getAbsolutePath()
                        + (phoneStubDir != null ? ";" + phoneStubDir.getAbsolutePath() : "")
                        + ";" + resDir.getAbsolutePath() + ";"
                        + buildinRes.getAbsolutePath());
                parparCmd.add(tmpFile.getAbsolutePath());
                parparCmd.add(request.getMainClass());
                parparCmd.add(request.getPackageName());
                parparCmd.add(request.getDisplayName());
                parparCmd.add(buildVersion);
                parparCmd.add(request.getArg("ios.project_type", "ios")); // ios, iphone, ipad
                parparCmd.add(addLibs);
                int outputMark = message.length();
                // 600s, matching the cloud builder running the identical translator
                // over the identical input. Translation time grows with app size, so
                // the lower local 420s cap meant a large app could translate fine on
                // the build server yet be killed mid-run on the developer's own
                // machine -- which is usually the slower of the two.
                if (!exec(userDir, env, 600000, parparCmd.toArray(new String[0]))) {
                    // Name the failure rather than leaving the build to report a
                    // bare "translator failed" -- an out-of-memory death is
                    // fixable by the developer, but only if we say so.
                    if (!heapOverridden && TranslatorHeap.looksOutOfMemory(message.substring(outputMark))) {
                        error(TranslatorHeap.outOfMemoryAdvice(heapMB, true), null);
                    }
                    return false;
                }
                // A SECOND pass for the watch, rooted at its own stub.
                //
                // Same classes, different root: the translator walks out from the entry point it is
                // given, so the watch tree contains what the watch lifecycle class reaches and
                // nothing else. Sharing the phone's translation -- what this used to do -- meant
                // the watch binary carried the phone's entire graph with its main defined away.
                //
                // Skipped when the two entry points are the same class, because then the two passes
                // would produce the same tree twice.
                if (watchNativeBuilder.needsOwnTranslation()) {
                    File watchOut = WatchNativeBuilder.translationDir(tmpFile);
                    watchOut.mkdirs();
                    List<String> watchCmd = new ArrayList<String>(parparCmd);
                    // The tail of the command is positional: <classpath> <out> <main> <package>
                    // <display> <version> <projectType> <addLibs>. Replace the output root and the
                    // main class; everything before them -- the JVM flags, the jar, the "ios" mode
                    // and the classpath -- is shared with the phone pass by construction.
                    int outIndex = watchCmd.size() - 7;
                    // Same application classes, but the WATCH stub in place of the phone's -- the
                    // only difference that makes this a different program.
                    watchCmd.set(outIndex - 1, classesDir.getAbsolutePath() + ";"
                            + watchStubDir.getAbsolutePath() + ";" + resDir.getAbsolutePath() + ";"
                            + buildinRes.getAbsolutePath());
                    watchCmd.set(outIndex, watchOut.getAbsolutePath());
                    watchCmd.set(outIndex + 1,
                            WatchNativeBuilder.translationRoot(request.getMainClass()));
                    log("[watchNative] Translating the watch slice from "
                            + watchNativeBuilder.getWatchMain());
                    // The same 600s the phone pass above gets, and for the same reason: the watch
                    // root can reach a graph of comparable size, so a lower cap here would kill an
                    // otherwise valid companion build on its SECOND translation only.
                    if (!exec(userDir, env, 600000, watchCmd.toArray(new String[0]))) {
                        return false;
                    }
                }
            } catch (Exception ex) {
                throw new BuildException("Failure while trying to run ByteCodeTranslator of ParparVM", ex);
            }
            stopwatch.split("ParparVM Execution");
            try {
                String orientations = request.getArg("ios.interface_orientation", null);
                if (orientations != null && orientations.split(":").length < 4) {
                    orientations = orientations.toLowerCase();
                    File infoPlist = new File(tmpFile, "dist/" + request.getMainClass() + "-src/" + request.getMainClass() + "-Info.plist");
                    if (!orientations.contains("uiinterfaceorientationportrait")) {
                        replaceInFile(infoPlist, "<string>UIInterfaceOrientationPortrait</string>", "");
                    }
                    if (!orientations.contains("uiinterfaceorientationportraitupsidedown")) {
                        replaceInFile(infoPlist, "<string>UIInterfaceOrientationPortraitUpsideDown</string>", "");
                    }
                    if (!orientations.contains("uiinterfaceorientationlandscapeleft")) {
                        replaceInFile(infoPlist, "<string>UIInterfaceOrientationLandscapeLeft</string>", "");
                    }
                    if (!orientations.contains("uiinterfaceorientationlandscaperight")) {
                        replaceInFile(infoPlist, "<string>UIInterfaceOrientationLandscapeRight</string>", "");
                    }
                }

                if ("true".equals(request.getArg("ios.prerendered_icon", "false"))) {
                    log("Replacing prerendered Icon");
                    File infoPlist = new File(tmpFile, "dist/" + request.getMainClass() + "-src/" + request.getMainClass() + "-Info.plist");
                    replaceAllInFile(infoPlist, "<key>UIPrerenderedIcon</key>[^<]*<false/>", "<key>UIPrerenderedIcon</key><true/>");
                }


                if(runPods || !request.getArg("ios.buildType", "debug").equals("debug") || request.getArg("ios.force64", "false").equals("true")) {
                    File pbx = new File(tmpFile, "dist/" + request.getMainClass() + ".xcodeproj/project.pbxproj");

                    //Note:  Changed this replace to work with cocoapods but it is possible, and even likely,
                    // that the change will work for all builds.  I made it "only" for the cocoapods version
                    // to prevent inadvertent breaking of versioned builds etc...
                    if (runPods) {
                        replaceAllInFile(pbx, "ARCHS = [^;]+;", "ARCHS = \"\\$(ARCHS_STANDARD)\";");
                        replaceAllInFile(pbx, "VALID_ARCHS = [^;]+;", "VALID_ARCHS = \"\\$(ARCHS_STANDARD)\";");
                    } else {
                        replaceInFile(pbx, "ARCHS = armv7;", "ARCHS = \"\\$(ARCHS_STANDARD)\";");
                        replaceAllInFile(pbx, "VALID_ARCHS = [^;]+;", "VALID_ARCHS = \"\\$(ARCHS_STANDARD)\";");
                    }
                }


                if(bicodeHandle) {
                    String minTargetVersion = request.getArg("ios.minDeploymentTarget", "6.0");
                    if(minTargetVersion.equals("6.0")) {
                        if (xcodeVersion >= 9) {
                            minTargetVersion = "7.0";
                        }
                        if (enableGalleryMultiselect && photoLibraryUsage) {
                            minTargetVersion = "8.0";
                        }
                        if (enableWKWebView) {
                            minTargetVersion = "8.0";
                        }
                    }
                    addMinDeploymentTarget(minTargetVersion);
                    File pbx = new File(tmpFile, "dist/" + request.getMainClass() + ".xcodeproj/project.pbxproj");
                    replaceInFile(pbx, "IPHONEOS_DEPLOYMENT_TARGET = 5.1.1;", "IPHONEOS_DEPLOYMENT_TARGET = "+getDeploymentTarget(request)+";");
                    // this is based on the response here: http://stackoverflow.com/questions/32504355/error-itms-90339-this-bundle-is-invalid-the-info-plist-contains-an-invalid-ke
                    if(request.getArg("ios.bitcode", "false").equals("true")) {
                        replaceInFile(pbx, "ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;",
                                "ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;\n");
                    } else {
                        replaceInFile(pbx, "ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;",
                                "ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;\n"
                            + "				ENABLE_BITCODE = NO;\n");
                    }
                    if (xcodeVersion >= 9) {
                        replaceAllInFile(pbx, "ASSETCATALOG_COMPILER_LAUNCHIMAGE_NAME = LaunchImage;", "");
                    }
                }

                if (useMetal) {
                    File pbx = new File(tmpFile, "dist/" + request.getMainClass() + ".xcodeproj/project.pbxproj");
                    replaceInFile(pbx, "CLANG_ENABLE_MODULES = NO;", "CLANG_ENABLE_MODULES = YES;");
                }
            } catch (Exception ex) {
                throw new BuildException("Failed to update infoplist file", ex);
            }

            try {
                normalizeAssetCatalogs(request);
            } catch (Exception ex) {
                throw new BuildException("Failed to normalize iOS asset catalogs", ex);
            }
            stopwatch.split("Post-VM Setup");
            boolean walletExtensionEnabled = "true".equals(request.getArg("ios.wallet.extension", "false"));
            if (walletExtensionEnabled) {
                if (!request.getArg("ios.wallet.appGroup", "").startsWith("group.")
                        || request.getArg("ios.wallet.issuerEndpoint", "").length() == 0) {
                    log("The ios.wallet.extension build hint requires both of the following build hints:\n"
                            + "  ios.wallet.appGroup={App Group id starting with 'group.' shared by the app and the Wallet extensions}\n"
                            + "  ios.wallet.issuerEndpoint={HTTPS URL of the issuer endpoint that produces the encrypted pass payload}");
                    return false;
                }
                if ("true".equals(request.getArg("ios.wallet.includeUI", "false"))
                        && request.getArg("ios.wallet.authEndpoint", "").length() == 0) {
                    log("The ios.wallet.includeUI build hint requires the ios.wallet.authEndpoint={HTTPS URL of the login endpoint} build hint");
                    return false;
                }
            }
            // Wallet/widget extensions and .ios.appext archives mutate the Xcode project through
            // the ruby xcodeproj gem even when CocoaPods isn't otherwise needed.
            boolean needsXcodeProjectMutation = runPods || walletExtensionEnabled
                    || surfacesExtensionEnabled || matterExtensionEnabled
                    || hasAppExtensionArchives(appExtensionArchiveDir);
            if (needsXcodeProjectMutation) {
                try {
                    List<File> podSpecFileList = new ArrayList<File>();
                    if (runPods) {
                        for (File podSpec : podSpecs.listFiles()) {
                            if (podSpec.getName().startsWith(".")) {
                                continue;
                            }
                            File distDir = new File(tmpFile, "dist");
                            File targetF = new File(distDir, podSpec.getName());
                            Files.move(podSpec.toPath(), targetF.toPath(), StandardCopyOption.REPLACE_EXISTING);
                            podSpecFileList.add(targetF);

                        }
                    }

                    String deploymentTargetStr = "";
                    String targetStr = request.getArg("ios.deployment_target", xcodeVersion >= 9 ? "7.0" : "6.0");

                    if (enableGalleryMultiselect && photoLibraryUsage && getMajorVersionInt(targetStr, 6) < 8) {
                        targetStr = "8.0";
                    }
                    if (enableWKWebView && getMajorVersionInt(targetStr, 6) < 8) {
                        targetStr = "8.0";
                    }
                    addMinDeploymentTarget(targetStr);
                    String simulatorArchitectureSettings = excludeArm64Simulator
                            ? "      config.build_settings['EXCLUDED_ARCHS[sdk=iphonesimulator*]'] = 'arm64'\n"
                            : "";
                    deploymentTargetStr = "begin\n"
                            + "  xcproj.targets.find{|e|e.name=='" + request.getMainClass() + "'}.build_configurations.each{|config| \n"
                            + "    config.build_settings['PRODUCT_BUNDLE_IDENTIFIER']='"+request.getPackageName()+"'\n"
                            + "    config.build_settings['DEFINES_MODULE']='YES'\n"
                            + "    config.build_settings['SWIFT_VERSION']='5.0'\n"
                            + "    config.build_settings['SWIFT_OBJC_BRIDGING_HEADER']='$(SRCROOT)/cn1-Bridging-Header.h'\n"
                            + "  }\n"
                            + "  xcproj.targets.each do |target|\n"
                            + "    # App-extension targets (CN1Widgets, wallet issuer provisioning) own their\n"
                            + "    # deployment targets. This script re-runs after pods integration, and on the\n"
                            + "    # second pass the extension targets already exist -- without this skip the\n"
                            + "    # pass stomps them down to the app's deployment target (seen as WidgetKit\n"
                            + "    # sources compiling at iOS 14 instead of the extension's 16.1).\n"
                            + "    next if target.respond_to?(:product_type) && target.product_type == 'com.apple.product-type.app-extension'\n"
                            + "    target.build_configurations.each do |config|\n"
                            + "      config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = '" + getDeploymentTarget(request) + "'\n"
                            + simulatorArchitectureSettings
                            + "    end\n"
                            + "  end\n"
                            + "  xcproj.save\n"
                            + "rescue => e\n"
                            + "  puts \"Error during updating deployment target: #{$!}\"\n"
                            + "  puts \"Backtrace:\\n\\t#{e.backtrace.join(\"\\n\\t\")}\"\n"
                            + "  puts 'An error occurred updating deployment target, but the build still might work...'\n"
                            + "end\n";

                    // Let's extract and add app extensions here

                    File[] appExtensions = appExtensionArchiveDir == null
                            ? new File[0]
                            : extractAppExtensions(appExtensionArchiveDir, new File(tmpFile, "dist"));
                    StringBuilder appExtensionsBuilder = new StringBuilder();
                    {
                        StringBuilder sb = appExtensionsBuilder;
                        for (File appExtension : appExtensions) {
                            // We are using the notification service extension so that we can support rich push notifications

                            String buildSettingsStr = "CLANG_ANALYZER_NONNULL = YES;\n"
                                    + "				CLANG_ANALYZER_NUMBER_OBJECT_CONVERSION = YES_AGGRESSIVE;\n"
                                    + "				CLANG_CXX_LANGUAGE_STANDARD = \"gnu++14\";\n"
                                    + "				CLANG_ENABLE_MODULES = YES;\n"
                                    + "				CLANG_ENABLE_OBJC_ARC = YES;\n"
                                    + "				CLANG_ENABLE_OBJC_WEAK = YES;\n"
                                    + "				CLANG_WARN_BLOCK_CAPTURE_AUTORELEASING = YES;\n"
                                    + "				CLANG_WARN_COMMA = YES;\n"
                                    + "				CLANG_WARN_DEPRECATED_OBJC_IMPLEMENTATIONS = YES;\n"
                                    + "				CLANG_WARN_DOCUMENTATION_COMMENTS = YES;\n"
                                    + "				CLANG_WARN_EMPTY_BODY = YES;\n"
                                    + "				CLANG_WARN_ENUM_CONVERSION = YES;\n"
                                    + "				CLANG_WARN_INFINITE_RECURSION = YES;\n"
                                    + "				CLANG_WARN_INT_CONVERSION = YES;\n"
                                    + "				CLANG_WARN_NON_LITERAL_NULL_CONVERSION = YES;\n"
                                    + "				CLANG_WARN_OBJC_IMPLICIT_RETAIN_SELF = YES;\n"
                                    + "				CLANG_WARN_OBJC_LITERAL_CONVERSION = YES;\n"
                                    + "				CLANG_WARN_RANGE_LOOP_ANALYSIS = YES;\n"
                                    + "				CLANG_WARN_STRICT_PROTOTYPES = YES;\n"
                                    + "				CLANG_WARN_SUSPICIOUS_MOVE = YES;\n"
                                    + "				CLANG_WARN_UNGUARDED_AVAILABILITY = YES_AGGRESSIVE;\n"
                                    + "				CLANG_WARN_UNREACHABLE_CODE = YES;\n"
                                    + "				CLANG_WARN__DUPLICATE_METHOD_MATCH = YES;";

                            Map<String, String> buildSettingsMap = parseXcodeBuildSettings(buildSettingsStr);

                            String extensionName = appExtension.getName();
                            String codeSignEntitlements = "$(NS_CODE_SIGN_ENTITLEMENTS)";
                            // An extension folder exported from Xcode often carries a pair --
                            // WalletNonUIExtension.entitlements beside
                            // WalletNonUIExtensionRelease.entitlements -- and this used to take
                            // whichever listFiles() returned last, which is filesystem order. The
                            // file picked here is the one the target is SIGNED with, so which of
                            // them wins must not be luck.
                            List<File> entitlements = extensionFilesEndingWith(appExtension, ".entitlements");
                            File extEntitlementsFile = preferredExtensionFile(entitlements, extensionName, ".entitlements");
                            if (entitlements.size() > 1) {
                                debug("The " + extensionName + " app extension carries "
                                        + entitlements.size() + " .entitlements files; signing with "
                                        + extEntitlementsFile.getName() + ". Name the one you mean "
                                        + extensionName + ".entitlements.");
                            }
                            if (extEntitlementsFile != null) {
                                codeSignEntitlements = extensionName + "/" + extEntitlementsFile.getName();
                            }


                            // The identifier as Xcode will see it: an archive may write
                            // PRODUCT_BUNDLE_IDENTIFIER = $(EXTENSION_ID) with EXTENSION_ID beside
                            // it, which resolves to a perfectly good identifier. Judging the raw
                            // reference would refuse a build that works, and a reference this
                            // build cannot resolve is not judged at all.
                            // What this archive IS: the SDK name xcodebuild will use (versioned),
                            // the configuration it builds, and the architecture it builds for.
                            // Everything below matches conditional settings against these.
                            String archiveSdk = activeIosSdkName(request);
                            String archiveConfiguration = "Release";
                            // Derived the same way ARCHS is, rather than assumed: a debug build
                            // with ios.debug.archs=armv7 is handed ARCHS=armv7, and Xcode would
                            // then pick an [arch=armv7] setting while this read the arm64 one.
                            String archiveArch = request.getArg("ios.buildType", "debug").equals("debug")
                                    && "armv7".equals(request.getArg("ios.debug.archs", null))
                                            ? "armv7" : "arm64";
                            Map<String, String> declaredSettings = appExtensionBuildSettings(appExtension);
                            ArchiveContext plistContext = ArchiveContext.of(archiveSdk,
                                    archiveConfiguration, archiveArch, declaredSettings);
                            // What the target is built as, before the archive's own settings are
                            // folded in below and possibly override it. A literal identifier in
                            // the extension's plist is what the built bundle declares whatever
                            // this says, so the two have to be the same one: signed for a bundle
                            // the archive does not contain is a codesign failure, not a warning.
                            String declaredId = appExtensionBundleId(appExtension, null,
                                    plistContext, request.getPackageName());
                            // Each configuration gets the identity its OWN plist states, base
                            // included. declaredId is this archive's answer and belongs to it
                            // alone: writing that as the base setting handed every other
                            // configuration the identifier of whichever one happened to be
                            // built, so a Release build off the exported project carried a Debug
                            // archive's bundle id and would fail against the Release profile.
                            // Skipping the unqualified key while copying the discovered ones is
                            // what left it there. The base setting is the base plist's own
                            // statement, or the derived default when it makes none, and the
                            // qualifiers sit beside it -- which is also how the archive's own
                            // answer comes back out, since winningSetting reads them the way
                            // Xcode does. An explicit PRODUCT_BUNDLE_IDENTIFIER in
                            // buildSettings.properties is merged over all of this further down
                            // and still outranks it.
                            // Captured BEFORE anything is stamped, and it has to stay that way:
                            // two settings can name the same physical plist -- the base one and a
                            // qualified one -- and stamping the qualified context rewrites that
                            // shared file's literal to $(PRODUCT_BUNDLE_IDENTIFIER). Read
                            // afterwards, the base literal is gone and the base configuration
                            // falls back to the derived folder identifier while preflight has
                            // already used the original.
                            Map<String, String> plistIdentifiers = appExtensionPlistIdentifiers(
                                    appExtension, plistContext, request.getPackageName());
                            String basePlistId =
                                    plistIdentifiers.get("PRODUCT_BUNDLE_IDENTIFIER");
                            buildSettingsMap.put("PRODUCT_BUNDLE_IDENTIFIER", basePlistId != null
                                    ? basePlistId
                                    : request.getPackageName() + "." + extensionName);
                            for (Map.Entry<String, String> perConfiguration
                                    : plistIdentifiers.entrySet()) {
                                // Unless the archive states an identifier for that configuration
                                // itself. buildSettings.properties is about the TARGET and outranks
                                // the plist -- which is why stamping replaces a literal disagreeing
                                // with it -- so restoring that same rejected literal as a qualified
                                // setting would have Xcode build the configuration under one
                                // identifier while preflight, the profile check and the export
                                // options all used the explicit one.
                                // NON-BLANK, because a blank one is not a statement about
                                // anything: dropBlankBundleIdentifiers removes it before the
                                // merge, so treating it as governing suppressed the plist
                                // identifier and left that configuration on the base identifier
                                // while its own bundle declares the literal stamping preserved.
                                String governingKey = winningSettingKey(declaredSettings,
                                        "PRODUCT_BUNDLE_IDENTIFIER",
                                        contextForCondition(perConfiguration.getKey(),
                                                plistContext));
                                String governing = governingKey == null ? null
                                        : declaredSettings.get(governingKey);
                                // And only where that override actually COVERS the plist entry.
                                // Specificity settles which of two settings wins where both apply; it
                                // says nothing about whether one reaches everywhere the other does.
                                // An explicit [sdk=iphoneos*] beside a plist named [config=Release]
                                // is orthogonal to it -- neither contains the other -- and
                                // suppressing on the strength of this archive being in both left a
                                // simulator Release with no identifier of its own while Xcode
                                // processes the Release plist's literal.
                                if (governing != null && governing.trim().length() > 0
                                        && conditionCovers(governingKey,
                                                perConfiguration.getKey())) {
                                    continue;
                                }
                                buildSettingsMap.put(perConfiguration.getKey(),
                                        perConfiguration.getValue());
                            }
                            // The identifier THIS archive gets: a qualified setting overrides the
                            // plain one, so a stale base beside a right device value is not a
                            // reason to refuse a build Xcode would have got right.
                            String governingId = winningSetting(declaredSettings,
                                    "PRODUCT_BUNDLE_IDENTIFIER", archiveSdk, archiveConfiguration,
                                    archiveArch);
                            String declaredIdForArchive = governingId != null
                                    && governingId.trim().length() > 0
                                            ? governingId.trim()
                                            : (declaredId != null ? declaredId
                                                    : request.getPackageName() + "." + extensionName);
                            // Fully, or not at all: a partially expanded identifier is a
                            // truncation, and it would name a bundle the archive does not contain.
                            String fullyResolvedId = resolveSettingsFully(declaredIdForArchive,
                                    extensionSettingsWithBuiltIns(appExtension, declaredSettings,
                                            archiveConfiguration, archiveSdk, archiveArch));
                            String resolvedBundleId = fullyResolvedId == null ? "" : fullyResolvedId;
                            String outOfNamespace = resolvedBundleId.length() == 0 ? null
                                    : outOfNamespaceExtensionIdMessage(extensionName, resolvedBundleId,
                                            request.getPackageName());
                            if (outOfNamespace != null) {
                                // Refused rather than logged: Apple requires an embedded bundle to
                                // sit under its container's identifier, no profile of this app's
                                // can sign one that does not, and building on costs a full archive
                                // and upload to be told the same thing later.
                                throw new BuildException(outOfNamespace);
                            }
                            stampAppExtensionInfoPlist(appExtension, request,
                                    ArchiveContext.of(archiveSdk, archiveConfiguration, archiveArch,
                                            appExtensionBuildSettings(appExtension)));
                            buildSettingsMap.put("PRODUCT_NAME", "$(TARGET_NAME)");
                            buildSettingsMap.put("PROVISIONING_PROFILE", "$(NS_PROVISIONING_PROFILE)");
                            buildSettingsMap.put("CODE_SIGN_ENTITLEMENTS", codeSignEntitlements);
                            buildSettingsMap.put("LD_RUNPATH_SEARCH_PATHS", "$(inherited) @executable_path/Frameworks @executable_path/../../Frameworks");
                            buildSettingsMap.put("INFOPLIST_FILE", extensionName + "/Info.plist");
                            // Both of these every extension this builder generates sets, and the
                            // generic path did not. An extension that supports fewer device
                            // families than the app is an App Store rejection on upload, and
                            // without SKIP_INSTALL the .appex is installed into the archive's
                            // Products as a second copy of a bundle already inside the .app.
                            buildSettingsMap.put("TARGETED_DEVICE_FAMILY",
                embeddedExtensionDeviceFamily(request.getArg("ios.project_type", "ios")));
                            buildSettingsMap.put("SKIP_INSTALL", "YES");
                            if (containsSwiftSource(appExtension)) {
                                // The project's Swift settings are applied to the app target
                                // alone, so a brought-in extension with .swift in it reached the
                                // compiler with no SWIFT_VERSION and failed on "SWIFT_VERSION ''
                                // is unsupported" -- after its sources had been added to the
                                // target. Apple's own Wallet extension templates are Swift.
                                buildSettingsMap.put("SWIFT_VERSION", request.getArg("ios.swiftVersion", "5.0"));
                                buildSettingsMap.put("ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES", "YES");
                            }

                            File buildSettingsProps = new File(appExtension, "buildSettings.properties");
                            if (buildSettingsProps.exists()) {
                                // Through the same reader preflight used, rather than a second
                                // parse of the same file: the two disagreed about trailing
                                // whitespace, so the identifier that was checked and the
                                // identifier that was written into the target were not the same
                                // string.
                                // Filtered BEFORE the merge, not after. Dropping afterwards
                                // removed the key the blank had just overwritten -- the good
                                // identifier put on this map above -- and left the target with no
                                // identifier at all, which is the failure the filtering exists to
                                // prevent.
                                Map<String, String> archiveOwnSettings =
                                        appExtensionBuildSettings(appExtension);
                                for (String note
                                        : dropBlankBundleIdentifiers(archiveOwnSettings,
                                                appExtension, plistContext,
                                                request.getPackageName())) {
                                    debug("The " + extensionName + " app extension: " + note + ".");
                                }
                                // Narrowed, not taken as given: an exported folder carries the
                                // universal "1,2" from the project it came from, and copying that
                                // over the host-derived value put an iPhone-only app back to
                                // embedding an extension that claims iPad.
                                String hostFamily = embeddedExtensionDeviceFamily(
                                        request.getArg("ios.project_type", "ios"));
                                for (String key : new ArrayList<String>(archiveOwnSettings.keySet())) {
                                    if ("TARGETED_DEVICE_FAMILY".equals(key)
                                            || isQualified(key, "TARGETED_DEVICE_FAMILY")) {
                                        // Resolved first: an unexpanded $(EXTENSION_FAMILIES)
                                        // matches none of the host's numeric families, so narrowing
                                        // read it as sharing nothing and replaced it with the host's
                                        // -- silently broadening an intentionally iPhone-only
                                        // extension inside a universal app. A reference this build
                                        // cannot resolve is left exactly as written, as everywhere
                                        // else here: Xcode resolves it, and we do not get to guess.
                                        // And in the KEY's own context: a helper can be
                                        // conditional too, and resolving against the raw map
                                        // read EXTENSION_FAMILIES's base 1,2 for a
                                        // TARGETED_DEVICE_FAMILY[config=Debug] that Xcode expands
                                        // to the Debug helper's 1 -- broadening the very
                                        // extension the author narrowed.
                                        String declaredFamily = resolveSettingsFully(
                                                archiveOwnSettings.get(key),
                                                flattenForContext(archiveOwnSettings,
                                                        contextForCondition(key, plistContext)));
                                        if (declaredFamily == null) {
                                            continue;
                                        }
                                        String narrowed = narrowDeviceFamily(declaredFamily,
                                                hostFamily);
                                        if (sameDeviceFamilies(narrowed, declaredFamily)) {
                                            // Already within the host's families, so there is nothing
                                            // to clamp -- and the value is left AS WRITTEN. Replacing
                                            // it with what it resolves to here froze this archive's
                                            // answer into the setting: with
                                            // EXTENSION_FAMILIES[config=Debug] = 1 under a base 1,2, a
                                            // Release build rewrote TARGETED_DEVICE_FAMILY to 1,2 and
                                            // a later Debug build lost the narrower family the author
                                            // asked for.
                                            continue;
                                        }
                                        debug("The " + extensionName + " app extension asks for "
                                                + key + " = " + declaredFamily
                                                + ", which this app does not support; narrowed to "
                                                + narrowed + ".");
                                        // Materialized only here, where the alternative is shipping a
                                        // family the container does not have. A reference that has to
                                        // be clamped cannot also be preserved.
                                        archiveOwnSettings.put(key, narrowed);
                                    }
                                }
                                buildSettingsMap.putAll(archiveOwnSettings);
                                buildSettingsProps.delete();
                            }



                            // The minimum iOS this extension declares, which App Store validation
                            // reads out of the built .appex as MinimumOSVersion. Computed after the
                            // properties are folded in, so an archive that states its own wins.
                            // The entitlements the TARGET IS SIGNED WITH, which is not
                            // necessarily the one picked by name: buildSettings.properties may set
                            // CODE_SIGN_ENTITLEMENTS at another file, and it is that file's
                            // payment-pass-provisioning that decides whether iOS 14 is the floor.
                            // The configuration this build hands to xcodebuild is Release for a
                            // device archive whatever ios.buildType says, so that is what a
                            // [config=...] condition must be matched against.
                            // The same context, now that buildSettingsMap holds the archive's own
                            // settings as well: BUILD_VARIANTS among them.
                            ArchiveContext buildContext = ArchiveContext.of(archiveSdk,
                                    archiveConfiguration, archiveArch, buildSettingsMap);
                            // Every variant's, not one of them: see
                            // appExtensionSigningEntitlementsPerVariant. The target has a single
                            // deployment target per configuration, so the strictest of these
                            // decides it.
                            List<File> signingEntitlements =
                                    appExtensionSigningEntitlementsPerVariant(appExtension,
                                            buildSettingsMap, extEntitlementsFile, buildContext);
                            // The BASE setting is copied into every Xcode configuration, so
                            // writing the archive's answer there hands Debug a minimum belonging
                            // to Release; it gets the base value, clamped on its own. The
                            // archive's own answer is what the target is created with.
                            String extDeploymentTarget = appExtensionDeploymentTarget(
                                    buildSettingsMap.get("IPHONEOS_DEPLOYMENT_TARGET"),
                                    signingEntitlements,
                                    request.getArg("ios.deployment_target", null),
                                    appExtension, buildSettingsMap, buildContext,
                                    getDeploymentTarget(request));
                            String archiveDeploymentTarget = appExtensionDeploymentTarget(
                                    winningSetting(buildSettingsMap, "IPHONEOS_DEPLOYMENT_TARGET",
                                            buildContext),
                                    signingEntitlements,
                                    request.getArg("ios.deployment_target", null),
                                    appExtension, buildSettingsMap, buildContext,
                                    getDeploymentTarget(request));
                            buildSettingsMap.put("IPHONEOS_DEPLOYMENT_TARGET", extDeploymentTarget);
                            for (String note : repairQualifiedExtensionSettings(buildSettingsMap,
                                    request.getPackageName(),
                                    appExtensionDeploymentFloor(signingEntitlements),
                                    ArchiveContext.of(archiveSdk, archiveConfiguration, archiveArch,
                                            buildSettingsMap),
                                    getDeploymentTarget(request))) {
                                debug("The " + extensionName + " app extension: " + note + ".");
                            }

                            // Guarded so the post-dependency re-run of fix_xcode_schemes.rb
                            // doesn't create duplicate extension targets.
                            sb.append("\nif xcproj.targets.find{|e| e.name=='" + extensionName + "'}.nil?\n"
                                    + "service_target = xcproj.new_target(:app_extension, '" + extensionName + "', :ios, '" + archiveDeploymentTarget + "')\n"
                                    + "xcproj.targets.find{|e|e.name=='" + request.getMainClass() + "'}.build_configurations.each{|e| \n"
                                    + "  e.build_settings['PROVISIONING_PROFILE']='$(APP_PROVISIONING_PROFILE)'\n"
                                    + "  e.build_settings['CODE_SIGN_ENTITLEMENTS']='$(APP_CODE_SIGN_ENTITLEMENTS)'\n"
                                    + "}\n"
                                    //+ "service_target.frameworks_build_phase.add_file_reference(xcproj.files.find{|e|e.path.include? 'UserNotifications.framework'})\n"
                                    + "service_group = xcproj.new_group('" + extensionName + "')\n");
                            appendFilesToXcodeProjGroup(sb, appExtension, "service_group", "service_target", appExtension.getParentFile());
                            sb.append("main_app_target = xcproj.targets.find{|e| e.name==main_class_name}\n"
                                    + "main_app_target.add_dependency(service_target)\n"
                                    + "fileref = xcproj.groups.find{|e| e.display_name=='Products'}.new_file('" + extensionName + ".appex', \"BUILT_PRODUCTS_DIR\")\n"
                                    + "embed_phase = main_app_target.copy_files_build_phases.find{|p| p.name=='Embed App Extensions'} || main_app_target.new_copy_files_build_phase('Embed App Extensions')\n"
                                    + "embed_phase.build_action_mask = \"2147483647\"\n"
                                    + "embed_phase.dst_subfolder_spec = \"13\"\n"
                                    + "embed_phase.run_only_for_deployment_postprocessing=\"0\"\n"
                                    + "embed_phase.add_file_reference(fileref)\n"
                                    + "service_target.build_configurations.each{|e| \n");
                            for (String buildSettingKey : buildSettingsMap.keySet()) {
                                sb.append("  e.build_settings['" + buildSettingKey + "'] = \"" + buildSettingsMap.get(buildSettingKey) + "\"\n");
                            }
                            sb.append("}\n");
                            sb.append("end\n");



                        }
                        if (appExtensions.length > 0) {
                            sb.append("xcproj.save(project_file)\n");
                        }
                    }

                    if (walletExtensionEnabled) {
                        appendWalletExtensionTargets(appExtensionsBuilder, request, new File(tmpFile, "dist"));
                    }

                    if (matterExtensionEnabled) {
                        // Same ordering note as the widget extension below: this runs after
                        // the global deployment-target pass, so the extension keeps its own
                        // IPHONEOS_DEPLOYMENT_TARGET of 16.1 while the app keeps whatever it
                        // targets.
                        appendMatterExtensionTarget(appExtensionsBuilder, request, new File(tmpFile, "dist"));
                    }

                    if (surfacesExtensionEnabled) {
                        // appExtensionsBuilder is appended to the schemes script AFTER the
                        // global deployment-target pass (deploymentTargetStr), so the
                        // extension's IPHONEOS_DEPLOYMENT_TARGET=16.1 survives it -- see the
                        // ordering note in appendWidgetExtensionRuby.
                        appendWidgetExtensionTargets(appExtensionsBuilder, request, new File(tmpFile, "dist"));
                    }

                    // App Intents needs no Xcode target of its own: the declarations compile
                    // into the app target, which is what makes the system background-launch
                    // the app to run them in-process, with the app's own storage and
                    // singletons intact. Writing the sources here puts them in <Main>-src
                    // before the schemes script sweeps that directory for Swift.
                    generateAppIntentSources(new File(tmpFile, "dist"), request);

                    String installLocalizedStrings = "";
                    if (installLocalizedStringsScript.length() > 0) {
                        installLocalizedStrings = "begin\n"+
                                installLocalizedStringsScript.toString() +
                                "rescue => e\n"
                                + "  puts \"Error during processing: #{$!}\"\n"
                                + "  puts \"Backtrace:\\n\\t#{e.backtrace.join(\"\\n\\t\")}\"\n"
                                + "  puts 'An error occurred recreating schemes, but the build still might work...'\n"
                                + "end\n";

                    }

                    String arcPhaseFixScript =
                            usesCn1Vision || usesCn1Language || usesCn1Inference
                            ? "    arc_sources = ['CN1Vision.m', 'CN1Language.m', 'CN1Inference.m']\n"
                            + "    main_target.source_build_phase.files.each do |bf|\n"
                            + "      ref = bf.file_ref\n"
                            + "      name = ref && File.basename(ref.path || ref.name || '')\n"
                            + "      next unless arc_sources.include?(name)\n"
                            + "      settings = bf.settings || {}\n"
                            + "      flags = settings['COMPILER_FLAGS'].to_s.split\n"
                            + "      flags << '-fobjc-arc' unless flags.include?('-fobjc-arc')\n"
                            + "      settings['COMPILER_FLAGS'] = flags.join(' ')\n"
                            + "      bf.settings = settings\n"
                            + "    end\n"
                            : "";
                    String createSchemesScript = "#!/usr/bin/env ruby\n" +
                            "require 'xcodeproj'\n" +
                            "require 'pathname'\n" +
                            "main_class_name = \"" + request.getMainClass() + "\"\n" +
                            "project_file = \"" +
                                tmpDir.getAbsolutePath() + "/dist/" +
                                request.getMainClass() + ".xcodeproj\"\n" +
                            "xcproj = Xcodeproj::Project.open(project_file)\n" +
                            createLldbSchemeSetupScript() +
                            installLocalizedStrings  +
                            "begin\n"
                            + "  xcproj.recreate_user_schemes do |scheme, target|\n"
                            + "    configure_cn1_lldb.call(scheme)\n"
                            + "  end\n"
                            + "  Dir.glob(File.join(project_file, 'xcshareddata', 'xcschemes', '*.xcscheme')).each do |scheme_path|\n"
                            + "    scheme = Xcodeproj::XCScheme.new(scheme_path)\n"
                            + "    configure_cn1_lldb.call(scheme)\n"
                            + "    scheme.save!\n"
                            + "  end\n"
                            + "rescue => e\n"
                            + "  puts \"Error during processing: #{$!}\"\n"
                            + "  puts \"Backtrace:\\n\\t#{e.backtrace.join(\"\\n\\t\")}\"\n"
                            + "  puts 'An error occurred recreating schemes, but the build still might work...'\n"
                            + "end\n"
                            + "begin\n"
                            + "  main_target = xcproj.targets.find{|e| e.name==main_class_name}\n"
                            + "  targets_to_fix = []\n"
                            + "  if main_target\n"
                            + "    targets_to_fix << main_target\n"
                            + "  else\n"
                            + "    targets_to_fix = xcproj.targets.select{|t| t.respond_to?(:product_type) && t.product_type == 'com.apple.product-type.application'}\n"
                            + "  end\n"
                            + "  if targets_to_fix.empty?\n"
                            + "    raise \"Unable to find iOS app target for Swift phase fixups. main_class_name=#{main_class_name}, available=#{xcproj.targets.map(&:name).join(', ')}\"\n"
                            + "  end\n"
                            + "  targets_to_fix.each do |main_target|\n"
                            + "    project_root = File.dirname(project_file)\n"
                            + "    swift_paths = Dir.glob(File.join(project_root, main_class_name + '-src', '**', '*.swift'))\n"
                            // The staged WATCH translation lives under <Main>-src/watch-src and is
                            // compiled by the watch target, from the file list stageWatchTranslation
                            // returns. This glob would otherwise hand the watch slice's Swift to the
                            // PHONE target -- a second copy of a class the phone already has, from a
                            // translation describing a different program.
                            + "    swift_paths = swift_paths.reject{|p| p.include?('/"
                            + WatchNativeBuilder.WATCH_SRC_DIR + "/')}\n"
                            // The Objective-C host that Swift reaches CN1IntentHost through is
                            // staged into the same directory, and this sweep only globs Swift --
                            // so without naming it the file is present, imported by the bridging
                            // header, and never compiled, which is an undefined symbol at link
                            // rather than a missing type at compile. Named explicitly rather than
                            // globbing '*.m', because that directory is full of translated
                            // Objective-C the project already lists.
                            + "    Dir.glob(File.join(project_root, main_class_name + '-src', 'CN1IntentHost.m')).each do |host_path|\n"
                            + "      rel_path = Pathname.new(host_path).relative_path_from(Pathname.new(project_root)).to_s\n"
                            + "      ref = xcproj.files.find{|f| f.path == rel_path} || xcproj.main_group.new_file(rel_path)\n"
                            + "      unless main_target.source_build_phase.files_references.include?(ref)\n"
                            + "        main_target.source_build_phase.add_file_reference(ref, true)\n"
                            + "      end\n"
                            + "    end\n"
                            + "    swift_paths.each do |swift_path|\n"
                            + "      rel_path = Pathname.new(swift_path).relative_path_from(Pathname.new(project_root)).to_s\n"
                            + "      ref = xcproj.files.find{|f| f.path == rel_path} || xcproj.main_group.new_file(rel_path)\n"
                            + "      unless main_target.source_build_phase.files_references.include?(ref)\n"
                            + "        main_target.source_build_phase.add_file_reference(ref, true)\n"
                            + "      end\n"
                            + "      begin\n"
                            + "        main_target.resources_build_phase.remove_file_reference(ref)\n"
                            + "      rescue\n"
                            + "      end\n"
                            + "    end\n"
                            // No app-extension target's Swift sources may be swept into the
                            // APP target: on script re-runs (post dependency integration)
                            // the extension group already exists and this catch-all would
                            // otherwise add WidgetKit -- or the Matter request handler --
                            // to the app's compile phase, where it is compiled and linked
                            // into the host as well as into the .appex.
                            + "    swift_refs = xcproj.files.select do |f|\n"
                            + "      file_name = f.path || f.name || f.display_name\n"
                            + "      file_name && file_name.downcase.end_with?('.swift') && !file_name.start_with?('"
                            + SURFACES_EXTENSION_NAME + "/') && !file_name.start_with?('"
                            + SURFACES_WATCH_EXTENSION_NAME + "/') && !file_name.start_with?('"
                            + MatterExtensionBuilder.EXTENSION_NAME + "/') && !file_name.include?('/"
                            + WatchNativeBuilder.WATCH_SRC_DIR + "/')\n"
                            + "    end\n"
                            + "    swift_refs.each do |ref|\n"
                            + "      unless main_target.source_build_phase.files_references.include?(ref)\n"
                            + "        main_target.source_build_phase.add_file_reference(ref, true)\n"
                            + "      end\n"
                            + "      begin\n"
                            + "        main_target.resources_build_phase.remove_file_reference(ref)\n"
                            + "      rescue\n"
                            + "      end\n"
                            + "    end\n"
                            + "    swift_resource_files = main_target.resources_build_phase.files.select do |bf|\n"
                            + "      ref = bf.file_ref\n"
                            + "      file_name = (ref && (ref.path || ref.name || ref.display_name)) || bf.display_name\n"
                            + "      file_name && file_name.downcase.end_with?('.swift')\n"
                            + "    end\n"
                            + "    swift_resource_files.each do |bf|\n"
                            + "      main_target.resources_build_phase.files.delete(bf)\n"
                            + "    end\n"
                            + "    source_folder_resources = main_target.resources_build_phase.files.select do |bf|\n"
                            + "      ref = bf.file_ref\n"
                            + "      ref_name = ref && (ref.path || ref.name || ref.display_name)\n"
                            + "      next false unless ref_name\n"
                            + "      if ref_name =~ /(^|\\/)[^\\/]*-src$/\n"
                            + "        true\n"
                            + "      else\n"
                            + "        dir_path = File.join(project_root, ref_name)\n"
                            + "        File.directory?(dir_path) && !Dir.glob(File.join(dir_path, '**', '*.swift')).empty?\n"
                            + "      end\n"
                            + "    end\n"
                            + "    source_folder_resources.each do |bf|\n"
                            + "      main_target.resources_build_phase.files.delete(bf)\n"
                            + "    end\n"
                            + "    remaining_swift_resources = main_target.resources_build_phase.files.select do |bf|\n"
                            + "      ref = bf.file_ref\n"
                            + "      file_name = (ref && (ref.path || ref.name || ref.display_name)) || bf.display_name\n"
                            + "      if file_name && file_name.downcase.end_with?('.swift')\n"
                            + "        true\n"
                            + "      elsif file_name && file_name =~ /(^|\\/)[^\\/]*-src$/\n"
                            + "        true\n"
                            + "      elsif file_name\n"
                            + "        dir_path = File.join(project_root, file_name)\n"
                            + "        File.directory?(dir_path) && !Dir.glob(File.join(dir_path, '**', '*.swift')).empty?\n"
                            + "      else\n"
                            + "        false\n"
                            + "      end\n"
                            + "    end\n"
                            + "    unless remaining_swift_resources.empty?\n"
                            + "      names = remaining_swift_resources.map do |bf|\n"
                            + "        ref = bf.file_ref\n"
                            + "        (ref && (ref.path || ref.name || ref.display_name)) || bf.display_name || '<unknown>'\n"
                            + "      end\n"
                            + "      raise \"Swift files/resources still present in Copy Bundle Resources: #{names.join(', ')}\"\n"
                            + "    end\n"
                            + arcPhaseFixScript
                            + "  end\n"
                            + "rescue => e\n"
                            + "  puts \"Error while correcting Swift build phases: #{$!}\"\n"
                            + "  puts \"Backtrace:\\n\\t#{e.backtrace.join(\"\\n\\t\")}\"\n"
                            + "  raise e\n"
                            + "end\n"
                            + deploymentTargetStr
                            + appExtensionsBuilder.toString();
                    File bridgingHeaderFile = new File(new File(tmpDir, "dist"), "cn1-Bridging-Header.h");
                    if (!bridgingHeaderFile.exists()) {
                        this.createFile(bridgingHeaderFile, "// Codename One generated Swift bridging header\n".getBytes(StandardCharsets.UTF_8));
                    }
                    File hooksDir = new File(tmpFile, "hooks");
                    hooksDir.mkdir();
                    File fixSchemesFile = new File(hooksDir, "fix_xcode_schemes.rb");
                    this.createFile(fixSchemesFile, createSchemesScript.getBytes("UTF-8"));
                    exec(hooksDir, "echo", "chmod", "0755", fixSchemesFile.getAbsolutePath());
                    exec(hooksDir, "chmod", "0755", fixSchemesFile.getAbsolutePath());
                    exec(hooksDir, "echo", fixSchemesFile.getAbsolutePath());
                    if (!exec(hooksDir, fixSchemesFile.getAbsolutePath())) {
                        log("Failed to fix xcode project schemes.  Make sure you have the xcodeproj ruby gem installed (gem install xcodeproj; it is also bundled with Cocoapods). ");
                        return false;
                    }

                    if (runPods) {
                    if (!exec(new File(tmpFile, "dist"), podTimeout, pod, "init")) {
                        log("Failed to run "+pod+" init.  Make sure you have Cocoapods installed.");
                        return false;
                    }
                    File podFile = new File(new File(tmpFile, "dist"), "Podfile");
                    if (!podFile.exists()) {
                        log("Failed to create the PodFile at " + podFile);
                        return false;
                    }
                    String podFileContents = "target '" + request.getMainClass() + "' do\n";
                    String[] pods = deduplicatePodSpecs(iosPods).split("[,;]");
                    for (String podLib : pods) {
                        podLib = podLib.trim();
                        if (podLib.isEmpty()) {
                            continue;
                        }
                        String podLibName = podLib;
                        String podLibVersion = "";
                        if (podLibName.contains(" ")) {
                            podLibName = podLib.substring(0, podLib.indexOf(" ")).trim();
                            podLibVersion = podLib.substring(podLib.indexOf(" ") + 1).trim();
                        }
                        String podSpecPath = "";
                        for (File f : podSpecFileList) {
                            if (f.getName().equals(podLibName + ".podspec")) {
                                podSpecPath = ", :path => '.'";
                                break;
                            }
                        }
                        podFileContents += "    pod  '" + podLibName + "'" + (!podLibVersion.equals("") ? (", '" + podLibVersion + "'") : "") + podSpecPath + "\n";
                    }
                    podFileContents += "end\n";

                    podFileContents = "platform :ios, '" + getDeploymentTarget(request) + "'\n" + podFileContents;

                    if (!"false".equals(request.getArg("ios.pods.use_frameworks!", "false"))) {
                        podFileContents = "use_frameworks!\n" + podFileContents;
                    }
                    if (request.getArg("ios.pods.sources", null) != null) {
                        String[] podSources = request.getArg("ios.pods.sources", null).split("[;,]");
                        for (String podSource : podSources) {
                            podSource = podSource.trim();
                            if (podSource.length() == 0) {
                                continue;
                            }

                            podFileContents = "source '" + podSource + "'\n" + podFileContents;
                        }
                    }

                    String buildSettings = "";
                    String buildSettingsPrefix = "ios.pods.build.";
                    for (String key : request.getArgs()) {
                        if (key.startsWith(buildSettingsPrefix)) {
                            if (buildSettings.length() == 0) {
                                buildSettings += "\n";
                            }
                            buildSettings += "      config.build_settings['" + key.substring(buildSettingsPrefix.length()) + "'] = \"" + request.getArg(key, "") + "\"\n";
                            ;
                        }
                    }


                    if (useMetal) {
                        buildSettings += "      config.build_settings['CLANG_ENABLE_MODULES'] = \"YES\"\n";
                    }
                    if (excludeArm64Simulator) {
                        // Google ML Kit's binary frameworks contain device
                        // arm64 and simulator x86_64 slices. Apply the same
                        // exclusion to every pod target so CocoaPods doesn't
                        // drop its conflicting per-pod setting while merging
                        // the aggregate xcconfig.
                        buildSettings += "      config.build_settings['EXCLUDED_ARCHS[sdk=iphonesimulator*]'] = \"arm64\"\n";
                    }


                    podFileContents += "\n\npost_install do |installer|\n" +
                            "  installer.pods_project.targets.each do |target|\n" +
                            "    target.build_configurations.each do |config|\n" +
                            "      config.build_settings['IPHONEOS_DEPLOYMENT_TARGET'] = \"" + getDeploymentTarget(request) + "\"\n" +
                            "      config.build_settings['EXPANDED_CODE_SIGN_IDENTITY'] = \"\"\n" +
                            "      config.build_settings['CODE_SIGNING_REQUIRED'] = \"NO\"\n" +
                            "      config.build_settings['CODE_SIGNING_ALLOWED'] = \"NO\"\n" +
                            "      config.build_settings['ENABLE_STRICT_OBJC_MSGSEND'] = \"NO\"\n" +
                            buildSettings +
                            "    end\n" +
                            "  end\n" +
                            "end";

                    this.createFile(podFile, podFileContents.getBytes("UTF-8"));

                    File userHome = new File(System.getProperty("user.home"));
                    File masterRepo = new File(userHome, ".cocoapods/repos/master");
                    if (!masterRepo.exists()) {
                        log("Master Repo doesn't exist.  Running pod setup to create it");
                        exec(new File(tmpFile, "dist"), podTimeout * 3, pod, "setup");
                        if (!masterRepo.exists()) {
                            log("Failed to create master repo.  This might spell trouble...");
                        }

                    }

                    // We need to set default encoding for running pods
                    // https://github.com/codenameone/CodenameOne/issues/3508
                    Map<String,String> podEnv = new HashMap<String,String>();
                    podEnv.put("LANG", "en_US.UTF-8");

                    if (!exec(new File(tmpFile, "dist"), (File)null, podTimeout, podEnv, pod, "install")) {
                        // Perhaps we need to update the master repo
                        log("Failed to exec cocoapods.  Trying to update master repo...");
                        if (!exec(new File(tmpFile, "dist"), podTimeout * 3, pod, "repo", "update")) {
                            log("Failed to update cocoapods master repo.  Trying to clean up spec repos");
                            if (!exec(new File(tmpFile, "dist"), podTimeout * 3, pod, "repo", "update")) {
                                log("Failed to update cocoapods master repo event after cleaning spec repos.");
                                return false;
                            }
                        }

                        if (!exec(new File(tmpFile, "dist"), (File)null, podTimeout, podEnv,pod, "install")) {
                            log("Cocoapods failed even after updating master repo");
                            log("Trying to cleanup spec repos");
                            if (!exec(new File(tmpFile, "dist"), (File)null, podTimeout, podEnv,pod, "install")) {
                                log("Cocoapods failed even after cleaning up spec repos.");
                                return false;
                            }
                        }
                    }
                    } // end if (runPods)
                } catch (Exception ex) {
                    throw new BuildException("Failed to update the generated Xcode project", ex);
                }
                stopwatch.split("CocoaPods");
            }

            if (runSpm) {
                configureSwiftPackages(request, dependencyConfig);
                if (!runPods) {
                    ensureTopLevelWorkspace(request);
                }
                stopwatch.split("SwiftPM");
            }

            File postPodsFixSchemesFile = new File(new File(tmpFile, "hooks"), "fix_xcode_schemes.rb");
            if (postPodsFixSchemesFile.exists()) {
                try {
                    if (!exec(postPodsFixSchemesFile.getParentFile(), postPodsFixSchemesFile.getAbsolutePath())) {
                        log("Failed to re-run xcode project Swift/resource phase fixups after dependency integration.");
                        return false;
                    }
                } catch (Exception ex) {
                    throw new BuildException("Failed to re-run xcode project Swift/resource phase fixups after dependency integration.", ex);
                }
            }

            // Detect whether the project contains any Swift source files.
            // If so, inject SWIFT_VERSION and related build settings into the
            // pbxproj and create the bridging header.  This avoids adding
            // Swift-specific settings to pure Objective-C projects.
            File distDir = new File(tmpFile, "dist");
            if (hasSwiftFiles(distDir)) {
                File bridgingHeader = new File(distDir, "cn1-Bridging-Header.h");
                if (!bridgingHeader.exists()) {
                    try {
                        this.createFile(bridgingHeader, "// Codename One generated Swift bridging header\n".getBytes(StandardCharsets.UTF_8));
                    } catch (IOException ex) {
                        log("Warning: failed to create Swift bridging header: " + ex.getMessage());
                    }
                }

                try {
                    File pbx = new File(tmpFile, "dist/" + request.getMainClass() + ".xcodeproj/project.pbxproj");
                    // Inject Swift build settings by anchoring on SDKROOT which appears in
                    // every project-level build configuration.
                    replaceInFile(pbx,
                            "SDKROOT = iphoneos;",
                            "SDKROOT = iphoneos;\n\t\t\t\tSWIFT_VERSION = 5.0;");
                    // Inject target-level settings by anchoring on PRODUCT_NAME which
                    // appears in every target build configuration.
                    replaceInFile(pbx,
                            "PRODUCT_NAME = \"$(TARGET_NAME)\";",
                            "DEFINES_MODULE = YES;\n\t\t\t\tPRODUCT_NAME = \"$(TARGET_NAME)\";\n\t\t\t\tSWIFT_OBJC_BRIDGING_HEADER = \"$(SRCROOT)/cn1-Bridging-Header.h\";");
                } catch (IOException ex) {
                    throw new BuildException("Failed to inject Swift build settings into pbxproj", ex);
                }
            }

            try {
                File pbxprojFile = new File(tmpFile, "dist/" + request.getMainClass() + ".xcodeproj/project.pbxproj");
                removeLinesContaining(pbxprojFile,
                        ".swift in Resources",
                        request.getMainClass() + "-src in Resources");

                if (request.getArg("ios.buildType", "debug").equals("debug") &&
                        request.getArg("ios.no_strip", "false").equalsIgnoreCase("true")) {
                    File pbx = new File(tmpFile, "dist/" + request.getMainClass() + ".xcodeproj/project.pbxproj");
                    replaceAllInFile(pbx, "COPY_PHASE_STRIP = YES;", "COPY_PHASE_STRIP = NO;");
                    replaceAllInFile(pbx, "STRIP_STYLE = all;", "STRIP_STYLE = debugging;");
                    replaceAllInFile(pbx, "SEPARATE_STRIP = YES;", "SEPARATE_STRIP = NO;");
                }
                if ("YES".equals(request.getArg("ios.pods.build.CLANG_ENABLE_MODULES", null))) {
                    // Needed this for WebRTC.  For some reason cocoapods was not updating these build settings.
                    // After several hours of fighting cocoapods, we'll just skip that here and brute force it.
                    // Perhaps revisit this in the future.
                    File pbx = new File(tmpFile, "dist/" + request.getMainClass() + ".xcodeproj/project.pbxproj");
                    if ("YES".equals(request.getArg("ios.pods.build.CLANG_ALLOW_NON_MODULAR_INCLUDES_IN_FRAMEWORK_MODULES", null))) {
                        replaceAllInFile(pbx, "CLANG_ENABLE_MODULES = NO;", "CLANG_ENABLE_MODULES = YES; CLANG_ALLOW_NON_MODULAR_INCLUDES_IN_FRAMEWORK_MODULES = YES;");
                    } else {
                        replaceAllInFile(pbx, "CLANG_ENABLE_MODULES = NO;", "CLANG_ENABLE_MODULES = YES;");
                    }
                }

                if (googleAdUnitId != null && googleAdUnitId.length() > 0 || moPubAdUnitId != null && moPubAdUnitId.length() > 0) {
                    File pbx = new File(tmpFile, "dist/" + request.getMainClass() + ".xcodeproj/project.pbxproj");
                    replaceAllInFile(pbx, "SDKROOT = iphoneos;", "OTHER_LDFLAGS = \"-ObjC\";\n				SDKROOT = iphoneos;");

                } else {
                    if (request.getArg("ios.objC", "false").equals("true")) {
                        File pbx = new File(tmpFile, "dist/" + request.getMainClass() + ".xcodeproj/project.pbxproj");
                        replaceAllInFile(pbx, "SDKROOT = iphoneos;", "OTHER_LDFLAGS = \"-ObjC\";\n				SDKROOT = iphoneos;");

                    }
                }

                // Last, deliberately, and this is the only correct place for it.
                //
                // These schemes exist so canOpenURL: can answer honestly about a
                // jailbroken device; without the declaration iOS answers false
                // whatever is installed. But they are also the LOWEST priority thing
                // in the array, because iOS honours a limited number of entries and
                // ignores the rest -- so everything the app actually needs has to
                // claim its slots first. Declared beside the jailbreak header
                // instead, this ran before the Smart Home block appended
                // com.apple.Home, and a project at the cap got sileo as its last
                // honoured entry and com.apple.Home as an ignored one: a security
                // probe silently costing the app a feature it asked for. Anything
                // else that adds a scheme belongs above this line.
                if (detectJailbreak) {
                    declareApplicationQueriesSchemes(request, JAILBREAK_QUERY_SCHEMES,
                            "DeviceIntegrity.getCompromiseReasons() will not see a "
                            + "jailbreak that only its package manager reveals.");
                }

                injectToPlist(tmpFile, resDir, request);

                addLocalizedIconsBuildSetting(pbxprojFile);

                String teamId = request.getArg("ios.teamId", "");
                // injectDevelopmentTeam anchors on `SDKROOT = iphoneos;`, which only
                // matches the project-level XCBuildConfiguration. That stays correct
                // for the iOS slice. The Mac slice's team is routed by
                // MacNativeBuilder.applyXcodeSettings via a [sdk=macosx*] key,
                // so this regex injection is intentionally NOT broadened.
                injectDevelopmentTeam(pbxprojFile,
                        request.getArg("ios.debug.teamId", teamId),
                        request.getArg("ios.release.teamId", teamId));

                if (macNativeBuilder.isEnabled()) {
                    File appSrcDir = new File(tmpFile, "dist/" + request.getMainClass() + "-src");
                    macNativeBuilder.writeEntitlements(request, appSrcDir);
                    macNativeBuilder.writeStubHeaders(appSrcDir);
                    macNativeBuilder.applyXcodeSettings(request, tmpFile, buildVersion);
                    macNativeBuilder.writeExportOptions(request, new File(tmpFile, "dist"));
                }

                if (watchNativeBuilder.isEnabled()) {
                    File appSrcDir = new File(tmpFile, "dist/" + request.getMainClass() + "-src");
                    // Before the plist and the Xcode script, both of which describe it. Generated
                    // here rather than alongside the iOS extension because the watch APP target
                    // does not exist yet when the schemes ruby runs -- and the complication
                    // extension is embedded in that target, not the phone's.
                    writeWatchWidgetExtension(request, new File(tmpFile, "dist"), appSrcDir);
                    watchNativeBuilder.writeWatchInfoPlist(request, appSrcDir);
                    watchNativeBuilder.writeWatchEntry(request, appSrcDir);
                    watchNativeBuilder.writeStubHeaders(appSrcDir);
                    // Empty when the watch shares the phone's translation, which is what tells
                    // applyXcodeSettings to reuse the app target's sources and neutralise the
                    // phone stub's main instead.
                    watchNativeBuilder.applyXcodeSettings(request, tmpFile, buildVersion,
                            watchNativeBuilder.stageWatchTranslation(request, tmpFile, appSrcDir));
                }

                if (tvNativeBuilder.isEnabled()) {
                    File appSrcDir = new File(tmpFile, "dist/" + request.getMainClass() + "-src");
                    tvNativeBuilder.writeTvInfoPlist(request, appSrcDir, resDir);
                    tvNativeBuilder.applyXcodeSettings(request, tmpFile, buildVersion);
                }

            } catch (Exception ex) {
                throw new BuildException("Failed to inject into plist");
            }



            
        }

        stopwatch.split("Finalize");
        stopwatch.stop();

        if ("xcode".equals(getBuildTarget()) || getBuildTarget() == null) {
            xcodeProjectDir = new File(tmpFile, "dist");
            return true;
        }

        return true;
    }

    private File xcodeProjectDir;

    /// Whether `cls` is a health value type rather than the store.
    ///
    /// These travel through the BLE sensor layer, which needs no HealthKit
    /// entitlement, no framework and no purpose string.
    /**
     * Whether a {@code com.codename1.home} class is one an app can name while
     * only asking whether smart home is <b>available</b>.
     *
     * <p>The distinction earns its keep because the HomeKit entitlement has to
     * be granted on the App ID: an app that renders "smart home is not
     * supported on this device" and nothing else would otherwise be handed an
     * entitlement its profile does not carry, and fail codesigning for a
     * capability it never wanted. Exactly the trap the HealthKit block
     * documents.</p>
     *
     * <p>The capability enums are what such an app touches: it reads an
     * availability, branches on it, and possibly shows a typed error. None of
     * that reaches an accessory.</p>
     */
    /**
     * Whether {@code cls} is the Matter setup-payload parser, which needs
     * nothing from the platform.
     *
     * <p>It lives in {@code com.codename1.home.commissioning} because that is
     * where it belongs to a reader, but it is pure Java: it parses an
     * {@code MT:} QR string or a manual pairing code and checksums it, and it
     * never reaches a bridge, a native or an ecosystem SDK. An app that scans
     * a code and tells the user it is malformed -- before deciding whether to
     * commission at all, or to hand it to a hub over the network -- is doing
     * exactly that and nothing more.</p>
     *
     * <p>Without this exemption the package prefix alone would make such an
     * app declare a HomeKit purpose string, carry the restricted HomeKit and
     * Matter entitlements, own an app group and ship a generated
     * commissioning extension. It would fail the build for want of the
     * purpose string, or codesigning for want of the entitlement on its App
     * ID -- for a string parser.</p>
     */
    /**
     * Whether this build commissions onto a Matter fabric of the app's own.
     *
     * <p>Read in two places -- the define that decides what a successful flow
     * means, and the extension generator that decides what the flow does --
     * and they have to agree, so the decision lives in one place.</p>
     *
     * @param request the build request
     * @return true when the scanner saw the call or the hint asks for it
     */
    private boolean matterOwnFabric(BuildRequest request)
            throws BuildException {
        String hint = request.getArg("ios.home.commissioning.fabric", null);
        if (hint != null) {
            // The hint settles it, whatever the code says: it exists for the
            // build whose call the scanner cannot read, and a developer who
            // wrote it down has answered the question this refuses over.
            //
            // Which is exactly why a typo cannot mean "no". Read as a plain
            // boolean, "treu" silently selected the ecosystem-only build and
            // overruled a setCommissionToThisApp(true) the scanner HAD seen,
            // so the app shipped without the controller it asked for and
            // nothing said why.
            String settled = hint.trim();
            if (!"true".equalsIgnoreCase(settled)
                    && !"false".equalsIgnoreCase(settled)) {
                throw new BuildException(
                        "ios.home.commissioning.fabric must be true or false,"
                        + " got '" + hint + "'. It decides whether this build"
                        + " ships a Matter controller and commissions"
                        + " accessories onto a fabric of your own, and it is"
                        + " read in preference to the code -- so a value"
                        + " neither this nor that would quietly build the"
                        + " opposite of what the app asked for.");
            }
            return "true".equalsIgnoreCase(settled);
        }
        if (homeFabricAmbiguous || (usesHomeOwnFabric
                && usesHomeOwnFabricDeclined)) {
            throw new BuildException(
                    "This app calls"
                    + " CommissioningRequest.setCommissionToThisApp() with"
                    + (homeFabricAmbiguous ? " a value this build cannot read"
                            : " both true and false")
                    + ", and the answer has to be the same for the whole"
                    + " build: the machinery that commissions onto a fabric"
                    + " of your own is an app extension generated now and run"
                    + " outside your process, so nothing at run time can turn"
                    + " it on or off per accessory.\n"
                    + "  Set ios.home.commissioning.fabric=true to build with"
                    + " it, or =false to build without it. The build hint is"
                    + " read in preference to the code, so the call can stay"
                    + " where it is.");
        }
        return usesHomeOwnFabric;
    }

    /// The purpose string that will actually reach the rendered Info.plist for a hint key.
    ///
    /// ios.plistInject WINS: the renderer emits a generated value only for a key the fragment
    /// does not already declare, so a build validated against the direct hint approved a
    /// disclosure the plist then dropped in favour of the injected one -- and an app with a
    /// perfectly good ios.NSHomeKitUsageDescription shipped with the fragment's <false/> and was
    /// terminated the moment it touched HomeKit.
    ///
    /// Answers "false" for a declared key whose value is not a nonblank string, which every
    /// caller treats as a refusal, and null when neither source supplies one.
    ///
    /// @param request the build request
    /// @param hintKey the ios.NS*UsageDescription hint
    /// @return the effective value, "false", or null
    static String effectivePurposeString(BuildRequest request, String hintKey) {
        String bare = hintKey.substring("ios.".length());
        String tag = WatchNativeBuilder.injectedPlistValueTag(request, bare);
        if (tag != null) {
            if (!"string".equals(tag)) {
                return "false";
            }
            String injected = WatchNativeBuilder.injectedPlistString(request, bare);
            return injected == null || injected.trim().length() == 0 ? "false" : injected;
        }
        return request.getArg(hintKey, null);
    }

    private static boolean isSmartHomeSetupPayload(String cls) {
        return "com/codename1/home/commissioning/SetupPayload".equals(cls)
                || cls.indexOf(
                        "com/codename1/home/commissioning/SetupPayload$") == 0;
    }

    private static boolean isSmartHomeAvailabilityType(String cls) {
        return "com/codename1/home/HomeAvailability".equals(cls)
                || "com/codename1/home/HomeBackend".equals(cls)
                || "com/codename1/home/HomeAuthorizationStatus".equals(cls)
                || "com/codename1/home/HomeError".equals(cls)
                || "com/codename1/home/HomeException".equals(cls)
                || "com/codename1/home/HomeConfigurationException".equals(cls);
    }

    private static boolean isSharedHealthModel(String cls) {
        return "com/codename1/health/HealthSample".equals(cls)
                || "com/codename1/health/QuantitySample".equals(cls)
                || "com/codename1/health/SeriesSample".equals(cls)
                || "com/codename1/health/CategorySample".equals(cls)
                || "com/codename1/health/HealthQuantity".equals(cls)
                || "com/codename1/health/HealthUnit".equals(cls)
                || "com/codename1/health/HealthDataType".equals(cls)
                || "com/codename1/health/HealthSource".equals(cls)
                || "com/codename1/health/RecordingMethod".equals(cls)
                || "com/codename1/health/BloodPressureSample".equals(cls)
                // The error types travel the same way. A sensor callback
                // is handed a HealthException, and asking it what went
                // wrong names HealthError -- so a sensor-only app that
                // handled its errors was read as touching the store, and
                // got the Health Connect bridge bundled and its
                // minSdkVersion raised to 26, cutting off the API 21-25
                // devices the BLE-only flow is documented to support.
                || "com/codename1/health/HealthException".equals(cls)
                || "com/codename1/health/HealthError".equals(cls)
                // Same exemption as the Android scanner, for the same
                // reason. A BLE-only listener branching on
                // getType().getKind(), or asking a unit for its dimension,
                // makes a direct enum reference -- and here the cost is
                // HealthKit linked and CN1_INCLUDE_HEALTH enabled for an app
                // that never opens the store, which on iOS also means the
                // HealthKit entitlement and the privacy usage strings the
                // build demands with it.
                || "com/codename1/health/HealthDataKind".equals(cls)
                || "com/codename1/health/HealthUnitDimension".equals(cls);
    }

    public File getXcodeProjectDir() {
        return xcodeProjectDir;
    }

    private void configureSwiftPackages(BuildRequest request, IOSDependencyConfig dependencyConfig) throws BuildException {
        if (!dependencyConfig.usesSwiftPackages()) {
            return;
        }
        File hooksDir = new File(tmpFile, "hooks");
        hooksDir.mkdir();
        File configFile = new File(hooksDir, "configure_swift_packages.rb");
        StringBuilder script = new StringBuilder();
        script.append("#!/usr/bin/env ruby\n")
                .append("require 'xcodeproj'\n")
                .append("project_file = '").append(escapeRuby(new File(tmpFile, "dist/" + request.getMainClass() + ".xcodeproj").getAbsolutePath())).append("'\n")
                .append("xcproj = Xcodeproj::Project.open(project_file)\n")
                .append("target = xcproj.targets.find { |t| t.name == '").append(escapeRuby(request.getMainClass())).append("' }\n")
                .append("abort('Unable to find app target ").append(escapeRuby(request.getMainClass())).append("') unless target\n");
        for (SwiftPackageSpec spec : dependencyConfig.swiftPackages) {
            script.append("package_ref = xcproj.root_object.package_references.find { |pkg| pkg.respond_to?(:repositoryURL) && pkg.repositoryURL == '")
                    .append(escapeRuby(spec.url)).append("' }\n")
                    .append("if package_ref.nil?\n")
                    .append("  package_ref = xcproj.new(Xcodeproj::Project::Object::XCRemoteSwiftPackageReference)\n")
                    .append("  package_ref.repositoryURL = '").append(escapeRuby(spec.url)).append("'\n")
                    .append("  package_ref.requirement = ").append(toRubyRequirement(spec.requirement)).append("\n")
                    .append("  xcproj.root_object.package_references << package_ref\n")
                    .append("end\n");
            for (String product : spec.products) {
                script.append("product_dep = target.package_product_dependencies.find { |dep| dep.product_name == '")
                        .append(escapeRuby(product)).append("' }\n")
                        .append("if product_dep.nil?\n")
                        .append("  product_dep = xcproj.new(Xcodeproj::Project::Object::XCSwiftPackageProductDependency)\n")
                        .append("  product_dep.package = package_ref\n")
                        .append("  product_dep.product_name = '").append(escapeRuby(product)).append("'\n")
                        .append("  target.package_product_dependencies << product_dep\n")
                        .append("end\n")
                        .append("unless target.frameworks_build_phase.files_references.any? { |ref| ref.respond_to?(:display_name) && ref.display_name == '")
                        .append(escapeRuby(product)).append("' }\n")
                        .append("  build_file = xcproj.new(Xcodeproj::Project::Object::PBXBuildFile)\n")
                        .append("  build_file.product_ref = product_dep\n")
                        .append("  target.frameworks_build_phase.files << build_file\n")
                        .append("end\n");
            }
        }
        script.append("xcproj.save\n");
        try {
            createFile(configFile, script.toString().getBytes(StandardCharsets.UTF_8));
            exec(hooksDir, "chmod", "0755", configFile.getAbsolutePath());
            if (!exec(hooksDir, configFile.getAbsolutePath())) {
                throw new BuildException("Failed to configure Swift Package Manager dependencies for generated Xcode project");
            }
        } catch (BuildException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BuildException("Failed to configure Swift Package Manager dependencies for generated Xcode project", ex);
        }
    }

    private String toRubyRequirement(String requirement) throws BuildException {
        if (requirement.startsWith("from:")) {
            String min = requirement.substring("from:".length()).trim();
            return "{ 'kind' => 'upToNextMajorVersion', 'minimumVersion' => '" + escapeRuby(min) + "' }";
        }
        if (requirement.startsWith("exact:")) {
            String version = requirement.substring("exact:".length()).trim();
            return "{ 'kind' => 'exactVersion', 'version' => '" + escapeRuby(version) + "' }";
        }
        if (requirement.startsWith("branch:")) {
            String branch = requirement.substring("branch:".length()).trim();
            return "{ 'kind' => 'branch', 'branch' => '" + escapeRuby(branch) + "' }";
        }
        if (requirement.startsWith("revision:")) {
            String revision = requirement.substring("revision:".length()).trim();
            return "{ 'kind' => 'revision', 'revision' => '" + escapeRuby(revision) + "' }";
        }
        if (requirement.startsWith("range:")) {
            String[] bounds = requirement.substring("range:".length()).trim().split("\\.\\.<");
            if (bounds.length != 2) {
                throw new BuildException("Invalid SPM range requirement '" + requirement + "'");
            }
            return "{ 'kind' => 'versionRange', 'minimumVersion' => '" + escapeRuby(bounds[0].trim()) + "', 'maximumVersion' => '" + escapeRuby(bounds[1].trim()) + "' }";
        }
        throw new BuildException("Unsupported SPM requirement '" + requirement + "'");
    }

    private void ensureTopLevelWorkspace(BuildRequest request) throws BuildException {
        File distDir = new File(tmpFile, "dist");
        File workspaceDir = new File(distDir, request.getMainClass() + ".xcworkspace");
        if (workspaceDir.exists()) {
            return;
        }
        if (!workspaceDir.mkdirs() && !workspaceDir.isDirectory()) {
            throw new BuildException("Failed to create workspace directory " + workspaceDir.getAbsolutePath());
        }
        File workspaceData = new File(workspaceDir, "contents.xcworkspacedata");
        String contents = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<Workspace\n" +
                "   version = \"1.0\">\n" +
                "   <FileRef\n" +
                "      location = \"group:" + request.getMainClass() + ".xcodeproj\">\n" +
                "   </FileRef>\n" +
                "</Workspace>\n";
        try {
            createFile(workspaceData, contents.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new BuildException("Failed to create workspace metadata at " + workspaceData.getAbsolutePath(), ex);
        }
    }

    /**
     * The marketing version an embedded extension must declare.
     *
     * <p>Apple validates an embedded extension's versions against its containing app, so a
     * hard-coded pair fails archive validation for every release that is not literally 1.0.
     * Resolved exactly as the watch builder resolves the same two keys, including the
     * injected-plist override -- which is the whole point: an app that sets
     * CFBundleShortVersionString through ios.plistInject ships THAT version, and the raw build
     * version is then the wrong answer for every extension beside it.</p>
     *
     * <p>Used by the Matter extension and by every brought-in .ios.appext.</p>
     */
    static String embeddedExtensionShortVersion(BuildRequest request) {
        String injected = WatchNativeBuilder.injectedPlistString(request,
                "CFBundleShortVersionString");
        return injected != null ? injected : WatchNativeBuilder.shortVersion(request);
    }

    /**
     * The build version an embedded extension must declare.
     *
     * <p>The fallback is shortVersion, NOT the marketing version resolved above: the two keys
     * are independent, and deriving one from the other is what produced the watch mismatch.</p>
     */
    static String embeddedExtensionBundleVersion(BuildRequest request) {
        String injected = WatchNativeBuilder.injectedPlistString(request, "CFBundleVersion");
        return injected != null ? injected
                : request.getArg("ios.bundleVersion", WatchNativeBuilder.shortVersion(request));
    }

    /**
     * Fills in the bundle identity a brought-in {@code .ios.appext} usually leaves to Xcode,
     * because nothing in the archive supplies it here.
     *
     * <p>A modern Xcode target keeps CFBundleIdentifier and the two version strings in build
     * settings and generates them into the plist, so an extension folder exported from such a
     * project ships an Info.plist with those keys simply absent. The target gets
     * PRODUCT_BUNDLE_IDENTIFIER, but nothing copies it into the plist:
     * {@code builtin-infoPlistUtility} expands {@code $(...)} references that are already there,
     * it does not add the key. The .appex is then built with no identifier and the archive fails
     * at the very end, in the app's own target, with "Embedded binary's bundle identifier is not
     * prefixed with the parent app's bundle identifier -- Embedded Binary Bundle Identifier:
     * (null)".</p>
     *
     * <p>Apple also requires an embedded extension to carry the same version strings as the app
     * containing it, so a stale or absent version is the same failure one step later. Both are
     * aligned here, and every change is logged: this edits a file the developer supplied. A value
     * that is already correct, and one written as a {@code $(...)} reference, are left alone.</p>
     */
    void stampAppExtensionInfoPlist(File appExtension, BuildRequest request,
            ArchiveContext context) throws IOException {
        Map<String, File> plists = appExtensionInfoPlists(appExtension, context);
        Set<String> stamped = new LinkedHashSet<String>();
        for (Map.Entry<String, File> candidate : plists.entrySet()) {
            File infoPlist = candidate.getValue();
            if (infoPlist == null) {
                debug("The " + appExtension.getName() + " app extension names '" + candidate.getKey()
                        + "' as an Info.plist this build will not edit -- it either holds a build "
                        + "setting that cannot be resolved here, or it lands outside the project "
                        + "directory -- so that plist was left as it is. If the archive fails on "
                        + "the embedded binary's bundle identifier, write the path relative to the "
                        + "project directory.");
                continue;
            }
            if (!infoPlist.isFile()) {
                debug("The " + appExtension.getName() + " app extension names '" + candidate.getKey()
                        + "' as an Info.plist, and there is no such file. Xcode cannot build an "
                        + "extension target without the plist its settings point at; add it to the "
                        + ".ios.appext archive.");
                continue;
            }
            if (isHostAppInfoPlist(infoPlist, appExtension.getParentFile(), request.getMainClass())) {
                // The app's OWN plist, named by one of the extension's settings. Everything under
                // the project directory is writable on purpose -- an extension may share a plist
                // that sits beside its folder -- but the identity written here is an EXTENSION's,
                // and putting it in the container rewrites the app's version or hands its
                // identifier to $(PRODUCT_BUNDLE_IDENTIFIER), which for the app target is a
                // different value entirely.
                //
                // Note this is about the FILE, not about the condition. Every candidate is
                // stamped, applicable or not, because the generated project keeps them all and a
                // Debug rebuild off sources.tar.bz2 ships whichever one it names -- an unstamped
                // one then carries the stale identity Apple rejects. Skipping the inactive ones
                // instead would leave that hole open AND leave this one, since an ACTIVE setting
                // naming the app's plist would still be written. Do not swap this guard for a
                // conditionApplies() filter without a test that covers both.
                debug("The " + appExtension.getName() + " app extension names '" + candidate.getKey()
                        + "' as an Info.plist, and that is the containing app's own plist. An "
                        + "extension's bundle identity does not belong in it, so it was left as "
                        + "it is; point INFOPLIST_FILE at a plist inside the extension.");
                continue;
            }
            ArchiveContext candidateContext = infoPlistCandidateContext(candidate.getKey(), context);
            if (!stamped.add(infoPlist.getCanonicalPath() + "\u0000" + candidateContext)) {
                // Two settings naming the same file IN THE SAME CONTEXT. Stamping is idempotent,
                // and saying so twice in the log reads like two files were touched.
                //
                // The context is part of the key because one physical plist is routinely named by
                // the base setting and by a qualified one: a $(MARKETING_VERSION) in it resolves
                // to the app's version under Release and to a stale 1.0 under
                // [config=Debug], and the file cannot be right for both while the reference
                // stands. Deduplicating on the path alone let the Release pass leave the
                // reference and skipped the Debug pass that would have replaced it, so the Debug
                // build off these sources shipped 1.0.
                continue;
            }
            // Through the shared resolvers rather than buildVersion / the ios.bundleVersion hint
            // directly: an app that sets either version key through ios.plistInject ships that
            // value, and stamping the raw hint here would rewrite an extension version that
            // already matched its app into one that does not -- the very validation failure this
            // method exists to prevent.
            // The settings the TARGET will carry, so a $(PRODUCT_BUNDLE_IDENTIFIER) in the plist
            // is judged by the identifier it will actually resolve to -- the archive's override
            // included, which is where an identifier from another project comes in.
            // In the CANDIDATE's configuration, not the archive's. Judged against the archive's,
            // the Debug plist's own literal read as disagreeing with the Release identifier and
            // was replaced with $(PRODUCT_BUNDLE_IDENTIFIER) -- and since the per-configuration
            // discovery that follows records literals and ignores references, the Debug identity
            // was then gone: that configuration fell back to the Release identifier and a Debug
            // build would be signed with a profile issued for a different bundle. Each plist is
            // judged against the identifier ITS configuration gets, which is the one Xcode
            // resolves in it.
            Map<String, String> settings = appExtensionBuildSettings(appExtension);
            settings.put("PRODUCT_BUNDLE_IDENTIFIER", appExtensionBundleId(appExtension,
                    request.getPackageName() + "." + appExtension.getName(), candidateContext,
                    request.getPackageName()));
            // In the candidate's OWN context, not this archive's. Every candidate is stamped,
            // because the generated project keeps them all and a later Debug build off
            // sources.tar.bz2 ships whichever one it names -- but a reference inside the Debug
            // plist expands to the Debug values, and judging it against the Release ones read
            // $(MARKETING_VERSION) as already matching the app. Left in place, it becomes the
            // stale Debug version on the build that actually uses that file, which is the
            // host-mismatch rejection this stamping exists to prevent.
            List<String> changes = stampPlistFile(infoPlist, embeddedExtensionShortVersion(request),
                    embeddedExtensionBundleVersion(request), request.getPackageName(),
                    flattenForContext(settings, candidateContext));
            if (changes == null) {
                debug("Could not read " + appExtension.getName() + "/" + infoPlist.getName()
                        + " as an XML property list, so its bundle identity was left as it is. If "
                        + "the build fails on the embedded binary's bundle identifier, convert the "
                        + "file with 'plutil -convert xml1 " + infoPlist.getName() + "' and rebuild.");
                continue;
            }
            for (String change : changes) {
                debug("Adjusted " + appExtension.getName() + "/" + infoPlist.getName() + ": " + change);
            }
        }
    }

    /// Stamps one Info.plist in place, in the encoding it was written in.
    ///
    /// @return what changed, empty when the plist was already right and must not be rewritten, or
    /// null when this is not an XML plist this build can edit
    static List<String> stampPlistFile(File infoPlist, String shortVersion, String bundleVersion,
            Map<String, String> archiveSettings) throws IOException {
        return stampPlistFile(infoPlist, shortVersion, bundleVersion, null, archiveSettings);
    }

    static List<String> stampPlistFile(File infoPlist, String shortVersion, String bundleVersion,
            String hostBundleId, Map<String, String> archiveSettings) throws IOException {
        PlistText original = readPlistText(infoPlist);
        List<String> changes = new ArrayList<String>();
        String result = stampInfoPlistIdentity(original.text, shortVersion, bundleVersion,
                hostBundleId, archiveSettings, changes);
        if (changes.isEmpty()) {
            return changes;
        }
        if (result == null) {
            return null;
        }
        writePlistText(infoPlist, original, result);
        return changes;
    }

    /// An Info.plist decoded the way its own bytes say it is encoded, so it can be written back
    /// the same way.
    private static final class PlistText {
        final String text;
        final Charset charset;
        final byte[] bom;

        PlistText(String text, Charset charset, byte[] bom) {
            this.text = text;
            this.charset = charset;
            this.bom = bom;
        }
    }

    /// Reads a plist as text, honouring its byte order mark or its XML declaration.
    ///
    /// The default charset is not good enough for a file that arrives from someone else's machine:
    /// a UTF-16 plist read as UTF-8 is noise, so the stamper would decline to parse it and the
    /// extension would ship unstamped, and a Latin-1 plist read as UTF-8 loses every accented
    /// character -- which this method would then write back, corrupting a display name to fix an
    /// identifier.
    private static PlistText readPlistText(File infoPlist) throws IOException {
        byte[] data = readFileBytes(infoPlist);
        byte[] bom = bomOf(data);
        Charset charset = charsetOf(data, bom);
        int from = bom == null ? 0 : bom.length;
        return new PlistText(new String(data, from, data.length - from, charset), charset, bom);
    }

    /// Writes the stamped text back in the charset it was read in, byte order mark included, so
    /// the file's own XML declaration stays true.
    private static void writePlistText(File infoPlist, PlistText original, String text)
            throws IOException {
        byte[] body = text.getBytes(original.charset);
        byte[] out = body;
        if (original.bom != null) {
            out = new byte[original.bom.length + body.length];
            System.arraycopy(original.bom, 0, out, 0, original.bom.length);
            System.arraycopy(body, 0, out, original.bom.length, body.length);
        }
        FileOutputStream stream = new FileOutputStream(infoPlist);
        try {
            stream.write(out);
        } finally {
            try { stream.close(); } catch (Throwable t) {}
        }
    }

    private static byte[] readFileBytes(File file) throws IOException {
        byte[] data = new byte[(int) file.length()];
        DataInputStream in = new DataInputStream(new FileInputStream(file));
        try {
            in.readFully(data);
        } finally {
            try { in.close(); } catch (Throwable t) {}
        }
        return data;
    }

    private static final byte[] BOM_UTF8 = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] BOM_UTF16BE = {(byte) 0xFE, (byte) 0xFF};
    private static final byte[] BOM_UTF16LE = {(byte) 0xFF, (byte) 0xFE};

    private static byte[] bomOf(byte[] data) {
        for (byte[] bom : new byte[][]{BOM_UTF8, BOM_UTF16BE, BOM_UTF16LE}) {
            if (data.length >= bom.length) {
                boolean match = true;
                for (int i = 0; i < bom.length; i++) {
                    match &= data[i] == bom[i];
                }
                if (match) {
                    return bom;
                }
            }
        }
        return null;
    }

    /// The charset a plist's bytes declare: its byte order mark first, then the encoding named in
    /// its XML declaration, and UTF-8 when it says neither -- which is what an XML parser does.
    private static Charset charsetOf(byte[] data, byte[] bom) {
        if (bom == BOM_UTF16BE) {
            return StandardCharsets.UTF_16BE;
        }
        if (bom == BOM_UTF16LE) {
            return StandardCharsets.UTF_16LE;
        }
        // UTF-16 without a byte order mark: its declaration is NUL-interleaved, so the probe below
        // reads gibberish and falls through to UTF-8, and the plist then fails to parse and goes
        // unstamped. The first characters of an XML document are "<?".
        if (data.length >= 4 && data[0] == 0 && data[1] == '<' && data[2] == 0 && data[3] == '?') {
            return StandardCharsets.UTF_16BE;
        }
        if (data.length >= 4 && data[0] == '<' && data[1] == 0 && data[2] == '?' && data[3] == 0) {
            return StandardCharsets.UTF_16LE;
        }
        // The declaration is ASCII-compatible in every encoding that can carry one, except the
        // UTF-16 forms, which the marks above have already answered for.
        String head = new String(data, 0, Math.min(data.length, 512), StandardCharsets.ISO_8859_1);
        Matcher declared = XML_ENCODING.matcher(head);
        if (declared.find()) {
            try {
                return Charset.forName(declared.group(1));
            } catch (Exception unsupported) {
                // An encoding this JVM does not know. UTF-8 is the better guess than the platform
                // default, and a plist that then fails to parse is left alone rather than rewritten.
            }
        }
        return StandardCharsets.UTF_8;
    }

    private static final Pattern XML_ENCODING = Pattern.compile(
            "<\\?xml[^>]*encoding\\s*=\\s*[\"\']([A-Za-z0-9_.:-]+)[\"\']");

    /// Every Info.plist this extension's target might be built with, by the setting that names it.
    ///
    /// Not just INFOPLIST_FILE: Xcode honours a qualified setting -- INFOPLIST_FILE[sdk=iphoneos*]
    /// -- and the archive's buildSettings.properties are copied into the target verbatim, so a
    /// qualified one takes precedence for the builds it matches while the base value serves the
    /// rest. Which one applies depends on the sdk, configuration and arch of the build Xcode is
    /// running, so every one of them is stamped: they are all plists this extension may ship, and
    /// stamping is idempotent.
    ///
    /// (An UNescaped `INFOPLIST_FILE[sdk=iphoneos*] = x` in a .properties file is not one of
    /// these. Properties splits on that first `=`, leaving the key `INFOPLIST_FILE[sdk`, which
    /// Xcode does not recognise as a setting at all -- so the base value still decides, and this
    /// map is right to ignore it.)
    ///
    /// @return the raw setting value that named each plist, mapped to the resolved file, or to
    /// null when that value is unresolvable or lands outside the project directory
    static Map<String, File> appExtensionInfoPlists(File extensionFolder) {
        return appExtensionInfoPlists(extensionFolder, null);
    }

    /// Whether this path is the containing app's own Info.plist.
    ///
    /// The generated project puts it at {@code <dist>/<MainClass>-src/<MainClass>-Info.plist},
    /// which is inside the project directory and therefore writable -- so an extension setting
    /// that names it, by relative path or through a reference, reaches the stamper like any
    /// other candidate.
    ///
    /// @return false when anything here is unknown, since a path that cannot be compared is not
    /// one to declare safe
    static boolean isHostAppInfoPlist(File candidate, File distDir, String mainClass) {
        if (candidate == null || distDir == null || mainClass == null
                || mainClass.length() == 0) {
            return false;
        }
        File host = new File(new File(distDir, mainClass + "-src"), mainClass + "-Info.plist");
        try {
            return candidate.getCanonicalPath().equals(host.getCanonicalPath());
        } catch (IOException cannotResolve) {
            return false;
        }
    }

    /// The context a candidate from {@link #appExtensionInfoPlists} belongs to.
    ///
    /// The candidates are keyed by the setting that names them -- "INFOPLIST_FILE[config=Debug] =
    /// $(CONFIGURATION)/Info.plist" -- and the qualifier in that key is the whole difference
    /// between the two files. The path was already resolved in it; what is IN the file has to be
    /// resolved in it too.
    ///
    /// @return the archive's own context for an unqualified candidate
    static ArchiveContext infoPlistCandidateContext(String candidateKey, ArchiveContext context) {
        if (candidateKey == null) {
            return context;
        }
        // Everything up to the " = " this method's callers append, and no less. Cutting at the
        // first ']' instead dropped every qualifier group after the first, so
        // INFOPLIST_FILE[config=Debug][sdk=iphonesimulator*] was read as a Debug candidate on the
        // archive's own SDK -- and an SDK-qualified version helper inside that plist then looked
        // correct and was left, while the build that uses the file expands it to another value.
        // A settings key cannot contain " = ", so the first occurrence is the separator.
        int separator = candidateKey.indexOf(" = ");
        String key = separator < 0 ? candidateKey : candidateKey.substring(0, separator);
        return key.indexOf('[') < 0 ? context : contextForCondition(key, context);
    }

    /// The configurations the generated project carries. An extension target is created in both,
    /// and sources.tar.bz2 lets either be built later.
    private static final String[] PROJECT_CONFIGURATIONS = {"Debug", "Release"};

    /// Where the first {@code $(...)} or {@code ${...}} begins, or -1 when there is none.
    private static int firstReference(String value) {
        java.util.regex.Matcher m = BUILD_SETTING_REFERENCE.matcher(value);
        return m.find() ? m.start() : -1;
    }

    /// Whether an identifier this build cannot finish resolving is at least known to land under
    /// the host.
    ///
    /// Being written through Xcode's own settings is not enough. PRODUCT_BUNDLE_IDENTIFIER =
    /// $(EXECUTABLE_NAME) references nothing but a built-in, and Xcode expands it to the
    /// extension's executable name -- an identifier with no relation to the containing app, which
    /// the namespace refusal never sees because this build could not resolve it. What CAN be
    /// established is the literal head: an expression that begins with the host's own identifier
    /// lands under it whatever the references come to, and one that does not is a guess.
    ///
    /// @return true when the value's leading literal is the host prefix, or when there is no host
    /// to judge against
    static boolean startsUnderHost(String value, Map<String, String> settings, String hostPackage) {
        if (hostPackage == null || hostPackage.length() == 0) {
            return true;
        }
        if (value == null) {
            return false;
        }
        // Everything this build CAN expand, with the rest left standing -- resolveSettingsInValue
        // deletes what it cannot expand, and deleting a leading $(EXECUTABLE_NAME) made
        // "$(EXECUTABLE_NAME)com.example.app.Ext" read as being under the host. Xcode puts the
        // executable name back, and the identifier is not under the host at all.
        String expanded = stripResolved(value, settings);
        // Only the head before the first thing still unexpanded is known. What follows a
        // reference is anyone's guess, and what PRECEDES one is what decides the namespace.
        int reference = firstReference(expanded);
        String head = reference < 0 ? expanded : expanded.substring(0, reference);
        return head.startsWith(hostPackage + ".");
    }

    /// The values a key's own condition allows, out of the ones worth enumerating.
    ///
    /// A key that names a dimension applies to that value and no other. Enumerating the rest built
    /// candidates that contradict themselves -- [variant=profile][variant=normal] -- and nothing
    /// downstream rejects such a key, so a plist the extension never uses would be rewritten.
    ///
    /// @param condition the key's value for this dimension, or null when it names none
    private static List<String> allowedBy(List<String> values, String condition) {
        if (condition == null || values == null) {
            return values;
        }
        List<String> out = new ArrayList<String>();
        for (String value : values) {
            if (value != null && matchesCondition(condition, value)) {
                out.add(value);
            }
        }
        return out;
    }

    /// The architecture names an iOS target can be built for. A closed set, which is what makes
    /// expanding an [arch=x86*] family into its members honest -- an SDK's version is not.
    private static final String[] KNOWN_ARCHITECTURES = {"arm64", "arm64e", "armv7", "armv7s",
            "x86_64", "i386"};

    /// The architectures worth resolving a path against: this build's, and every one the archive
    /// names in a qualifier.
    ///
    /// Same rule as {@link #enumerableSdks}, and the same reason: an arm64 archive that never
    /// looked at an [arch=x86_64] helper left the Intel simulator's plist undiscovered, so a
    /// later build of it carried the arm64 identifier while its own plist declared another.
    /// Nothing is invented here either -- only architectures the archive wrote down.
    /// Visible for the test that pins which architectures are enumerated and which are not.
    static List<String> enumerableArchsForTest(ArchiveContext own, Map<String, String> declared) {
        return enumerableArchs(own, declared);
    }

    private static List<String> enumerableArchs(ArchiveContext own, Map<String, String> declared) {
        List<String> out = new ArrayList<String>();
        if (own != null && own.arch != null && own.arch.length() > 0) {
            out.add(own.arch);
        }
        if (declared != null) {
            for (String key : declared.keySet()) {
                String arch = conditionsOf(key).get("arch");
                if (arch == null || arch.length() == 0) {
                    continue;
                }
                if (!isFamilyPattern(arch)) {
                    if (!out.contains(arch)) {
                        out.add(arch);
                    }
                    continue;
                }
                // A family expands to the architectures it names. Adding the pattern itself was
                // useless -- extensionSettingsWithBuiltIns will not define CURRENT_ARCH for one,
                // so the path never resolved and that plist went unstamped. Architecture names
                // are a closed set, unlike an SDK's version, so expanding one invents nothing.
                for (String concrete : KNOWN_ARCHITECTURES) {
                    if (matchesCondition(arch, concrete) && !out.contains(concrete)) {
                        out.add(concrete);
                    }
                }
            }
            // ARCHS names them outright, with no qualified key anywhere: an archive building
            // "arm64 x86_64" through $(CURRENT_ARCH) has two plists and only one of them was ever
            // seen. Read in the archive's own context, since ARCHS can be conditional too.
            String archs = winningSetting(declared, "ARCHS", own);
            if (archs != null) {
                // Resolved first: ARCHS[config=Debug] = $(DEBUG_ARCHS) is as ordinary as any other
                // indirection, and reading the raw text meant every token was a $-token and every
                // one of them ignored -- so that configuration's architecture was never
                // enumerated and its plist went unstamped.
                archs = resolveSettingsInValue(archs, flattenForContext(declared, own));
                for (String arch : archs.trim().split("\\s+")) {
                    if (arch.length() > 0 && arch.indexOf('$') < 0 && !out.contains(arch)) {
                        out.add(arch);
                    }
                }
            }
        }
        if (out.isEmpty()) {
            out.add(own == null ? null : own.arch);
        }
        return out;
    }

    /// The SDKs worth resolving a path against: this build's, and every one the archive itself
    /// names in a qualifier.
    ///
    /// Deliberately not "device and simulator". A path written through $(SDK_NAME) needs a
    /// VERSIONED name -- iphonesimulator18.0, not iphonesimulator -- and this build has no way to
    /// know which version a later simulator build will use. Inventing the bare platform name is
    /// the stem mistake this file already made once: it resolves to a directory nothing is at,
    /// reports the wrong file missing, and leaves the real one unstamped. Such a path simply does
    /// not resolve here, which is the honest answer.
    ///
    /// What IS knowable is an SDK the archive wrote down itself, in a
    /// [sdk=...] qualifier on any of its settings -- a path that varies through such a helper
    /// resolves exactly, so those are enumerated.
    private static List<String> enumerableSdks(ArchiveContext own, Map<String, String> declared) {
        List<String> out = new ArrayList<String>();
        if (own != null && own.sdk != null && own.sdk.length() > 0) {
            out.add(own.sdk);
        }
        if (declared != null) {
            for (String key : declared.keySet()) {
                String sdk = conditionsOf(key).get("sdk");
                // A pattern names a family rather than an SDK; extensionSettingsWithBuiltIns will
                // not supply SDK_NAME for one, so a path needing it stays unresolved and is
                // skipped below -- while a path through a declared helper still resolves.
                if (sdk != null && sdk.length() > 0 && !out.contains(sdk)) {
                    out.add(sdk);
                }
            }
        }
        return out;
    }

    /// Records a candidate per configuration when the path RESOLVES differently in each.
    ///
    /// A key says which builds it applies to; it does not say that its value means the same thing
    /// in all of them. INFOPLIST_FILE = $(CONFIGURATION)/Info.plist is unqualified and still names
    /// a different file per configuration, and so is INFOPLIST_FILE = $(PLIST_PATH) when
    /// PLIST_PATH itself is conditional. Enumerated by key alone, only the archive's own file was
    /// ever seen: the other configuration's plist was never stamped, and the identifier read out
    /// of the archive's was recorded as if it were universal.
    ///
    /// Nothing is added when the path resolves the same everywhere, which is the ordinary case.
    private static void addConfigurationVariants(Map<String, File> out, String key, String value,
            File extensionFolder, Map<String, String> declared, ArchiveContext context) {
        if (context == null || value.indexOf('$') < 0) {
            return;
        }
        // From the CANDIDATE's context, not the archive's: a key qualified for some other build
        // -- [sdk=iphonesimulator*] beside a device archive -- keeps its family here too, so
        // $(SDK_NAME) stays unexpanded and the path stays unresolvable instead of being answered
        // with this archive's SDK.
        ArchiveContext own = contextForCondition(key, context);
        // Every enumerated value is filtered through the KEY's own conditions first. A key that
        // names a dimension applies to that value and no other, and enumerating the rest produced
        // self-contradicting candidates -- [variant=profile][variant=normal] -- which the stamper
        // does not reject and would have rewritten a plist the extension never uses.
        Map<String, String> constraints = conditionsOf(key);
        // Filtered through the condition rather than taken from the context: an inactive
        // [config=Deb*] leaves own.configuration as the PATTERN, and enumerating that supplies no
        // CONFIGURATION at all, so Debug/Info.plist was never discovered. Filtering answers the
        // exact case identically and the wildcard case correctly.
        List<String> configurations = allowedBy(java.util.Arrays.asList(PROJECT_CONFIGURATIONS),
                constraints.get("config"));
        for (String sdk : allowedBy(enumerableSdks(own, declared), constraints.get("sdk"))) {
            for (String configuration : configurations) {
                // ARCHS can be qualified too, so the architectures of a Debug build are not
                // necessarily this archive's: asking with the fixed context never enumerated them
                // and that configuration's plist went undiscovered.
                ArchiveContext archContext = new ArchiveContext(sdk, configuration, own.arch,
                        own.variants);
                for (String arch : allowedBy(enumerableArchs(archContext, declared),
                        constraints.get("arch"))) {
                    // Recomputed here, not captured once: BUILD_VARIANTS can itself be qualified,
                    // so the variants of a Debug build are not necessarily this archive's. Reusing
                    // the active list meant a configuration that selects another variant never had
                    // its plist discovered at all.
                    ArchiveContext dimension = new ArchiveContext(sdk, configuration, arch, null);
                    List<String> variants = allowedBy(ArchiveContext.of(sdk, configuration, arch,
                            flattenForContext(declared, dimension)).variants,
                            constraints.get("variant"));
                    for (String variant : variants) {
                        boolean sameSdk = sdk.equalsIgnoreCase(own.sdk == null ? "" : own.sdk);
                        boolean sameConfiguration = configuration == null
                                ? own.configuration == null
                                : configuration.equalsIgnoreCase(own.configuration);
                        boolean sameArch = arch == null ? own.arch == null
                                : arch.equalsIgnoreCase(own.arch);
                        if (sameSdk && sameConfiguration && sameArch && variants.size() == 1
                                && own.variants != null && own.variants.contains(variant)) {
                            continue;
                        }
                        ArchiveContext other = new ArchiveContext(sdk, configuration, arch,
                                java.util.Collections.singletonList(variant));
                        File resolved = resolveInfoPlistPath(value, extensionFolder,
                                extensionSettingsWithBuiltIns(extensionFolder, declared, other));
                        if (resolved == null) {
                            continue;
                        }
                        // NOT skipped when it is the same physical file as the archive's own. The
                        // stamper deliberately processes one file once per context, because what
                        // is INSIDE it can vary even when the path does not: a Shared.plist
                        // holding $(MARKETING_VERSION) beside a MARKETING_VERSION[config=Debug]
                        // is right for one configuration and stale for the other. Skipping the
                        // duplicate file dropped the pass that would have materialised the host's
                        // version for Debug. Candidate keys carry their qualifiers, so nothing
                        // collides here, and stampAppExtensionInfoPlist dedups by file AND
                        // context.
                        // The variant is named whenever it is not the archive's own, not only
                        // when several are in play: a configuration whose BUILD_VARIANTS selects a
                        // single OTHER variant still needs saying, or the candidate's context
                        // inherits this archive's variant and misses the target identifier Xcode
                        // picks for it.
                        boolean ownVariant = own.variants != null && own.variants.contains(variant);
                        String qualifier = (sameSdk ? "" : "[sdk=" + sdk + "]")
                                + (sameConfiguration ? "" : "[config=" + configuration + "]")
                                + (sameArch ? "" : "[arch=" + arch + "]")
                                + (variants.size() > 1 || !ownVariant
                                        ? "[variant=" + variant + "]" : "");
                        out.put(key + qualifier + " = " + value, resolved);
                    }
                }
            }
        }
    }

    /// @param context this archive, so INFOPLIST_FILE = $(CONFIGURATION)/Info.plist resolves to
    /// the plist Xcode actually processes. Without it the path came back unresolvable and the
    /// stamping skipped the file that ships -- the same hole the entitlements path had.
    static Map<String, File> appExtensionInfoPlists(File extensionFolder, ArchiveContext context) {
        // The DECLARED settings, qualified keys and all: those keys are the candidates. The
        // resolution map beside it is flattened for this archive, which is right for expanding a
        // reference and useless for finding conditionals -- reading candidates out of it made the
        // qualified entries disappear.
        Map<String, String> declared = appExtensionBuildSettings(extensionFolder);
        // Through the context-taking overload, so the archive's VARIANTS survive. The
        // five-argument form rebuilds a context out of BUILD_VARIANTS, and a path written
        // through a variant-qualified helper was then picked by map order rather than by the
        // variant being resolved -- which could map two variants onto the same plist and hand
        // the profile target the normal one's identity.
        Map<String, String> settings = context == null ? declared
                : extensionSettingsWithBuiltIns(extensionFolder, declared, context);
        Map<String, File> out = new LinkedHashMap<String, File>();
        String base = declared.get("INFOPLIST_FILE");
        if (base == null || base.trim().length() == 0) {
            File byDefault = new File(extensionFolder, "Info.plist");
            out.put("Info.plist",
                    insideProjectDir(byDefault, extensionFolder.getParentFile()) ? byDefault : null);
        } else {
            out.put("INFOPLIST_FILE = " + base.trim(),
                    resolveInfoPlistPath(base.trim(), extensionFolder, settings));
            addConfigurationVariants(out, "INFOPLIST_FILE", base.trim(), extensionFolder, declared,
                    context);
        }
        for (Map.Entry<String, String> setting : declared.entrySet()) {
            String key = setting.getKey();
            // Closing bracket included: an unescaped conditional leaves Properties with the key
            // INFOPLIST_FILE[sdk and the rest of the line as its value, and that is not a setting
            // Xcode honours -- picking it up here would send the stamper after a path built out of
            // the wreckage.
            if (!key.startsWith("INFOPLIST_FILE[") || !key.endsWith("]")) {
                continue;
            }
            String value = setting.getValue() == null ? "" : setting.getValue().trim();
            if (value.length() > 0) {
                // In the context the qualifier declares: [config=Debug] means $(CONFIGURATION) is
                // Debug for THAT entry, whatever this archive builds.
                ArchiveContext own = contextForCondition(key, context);
                // Keyed by the SETTING, not by its text: two settings can carry the same text and
                // still name different files, which is exactly what
                // $(CONFIGURATION)/Info.plist under [config=Debug] does. Keying by the text threw
                // the second one away before it was ever resolved.
                // In the candidate's own context, VARIANTS included -- the same reason the base
                // resolution above passes the context rather than its three scalars.
                out.put(key + " = " + value, resolveInfoPlistPath(value, extensionFolder,
                        context == null ? declared
                                : extensionSettingsWithBuiltIns(extensionFolder, declared, own)));
                addConfigurationVariants(out, key, value, extensionFolder, declared, context);
            }
        }
        return out;
    }

    /**
     * The text half of {@link #stampAppExtensionInfoPlist}, kept separate so it can be tested.
     *
     * @param changes collects a human-readable line per edit; empty means the plist was already
     * right and must not be rewritten
     * @return the new plist text, or null when this is not an XML plist we can edit -- in which
     * case {@code changes} carries the reason and the file is left alone
     */
    static String stampInfoPlistIdentity(String plist, String shortVersion, String bundleVersion,
            Map<String, String> archiveSettings, List<String> changes) {
        return stampInfoPlistIdentity(plist, shortVersion, bundleVersion, null, archiveSettings, changes);
    }

    /// @param hostBundleId the containing app's bundle identifier, so a literal identifier that
    /// could never be one of its extensions can be recognised; null skips that check
    static String stampInfoPlistIdentity(String plist, String shortVersion, String bundleVersion,
            String hostBundleId, Map<String, String> archiveSettings, List<String> changes) {
        if (plist == null || rootDictAt(plist) < 0) {
            changes.add("not an XML property list");
            return null;
        }
        String result = openEmptyRootDict(plist);
        // A literal left over from another project is not prefixed by the host's bundle id, and
        // PRODUCT_BUNDLE_IDENTIFIER cannot save it: the literal is what ships. One that could be
        // this app's extension is kept; one that could not is replaced.
        result = setPlistString(result, "CFBundleIdentifier", "$(PRODUCT_BUNDLE_IDENTIFIER)",
                !identifierBelongsToApp(result, hostBundleId, archiveSettings), archiveSettings, changes);
        result = setPlistString(result, "CFBundleShortVersionString", shortVersion,
                true, archiveSettings, changes);
        result = setPlistString(result, "CFBundleVersion", bundleVersion,
                true, archiveSettings, changes);
        // The rest of what makes a directory an app-extension BUNDLE rather than a folder with a
        // program in it. Without CFBundleExecutable the .appex does not claim its own binary, and
        // App Store validation rejects the upload -- "the ... binary file is not permitted. Your
        // app cannot contain standalone executables or libraries, other than a valid
        // CFBundleExecutable of supported bundles" -- after a build that succeeded and an archive
        // that exported cleanly. Every extension this builder generates itself writes exactly
        // these; a brought-in one whose plist leaves them to GENERATE_INFOPLIST_FILE arrives
        // without them, and nothing downstream puts them back.
        result = setPlistString(result, "CFBundleExecutable", "$(EXECUTABLE_NAME)",
                false, archiveSettings, changes);
        result = setPlistString(result, "CFBundlePackageType", "XPC!",
                false, archiveSettings, changes);
        result = setPlistString(result, "CFBundleName", "$(PRODUCT_NAME)",
                false, archiveSettings, changes);
        // Required of an extension, and refused at upload rather than at build: "Missing
        // Info.plist value. A value for the key 'CFBundleDisplayName' in bundle
        // <app>.app/PlugIns/<Name>.appex is required." Both extensions this builder GENERATES
        // have always written it -- the wallet one and the widget one -- and the list of keys
        // filled in here was assembled from what an exported extension folder leaves out, so it
        // was missing the one key an exported folder usually still has. Every other name in this
        // method now matches what we write for our own.
        result = setPlistString(result, "CFBundleDisplayName", "$(PRODUCT_NAME)",
                false, archiveSettings, changes);
        result = setPlistString(result, "CFBundleInfoDictionaryVersion", "6.0",
                false, archiveSettings, changes);
        // The reference rather than a literal "en", as the generated Wallet and push extensions
        // both write it: the archive's own DEVELOPMENT_LANGUAGE is copied into this target's build
        // settings, so an extension whose development language is not English gets its own value
        // here instead of advertising the wrong fallback localization.
        result = setPlistString(result, "CFBundleDevelopmentRegion", "$(DEVELOPMENT_LANGUAGE)",
                false, archiveSettings, changes);
        return result;
    }

    /// Whether the identifier the plist already carries can be an extension of this app: absent,
    /// a build-setting reference, or a literal under the host's own bundle id.
    static boolean identifierBelongsToApp(String plist, String hostBundleId,
            Map<String, String> archiveSettings) {
        if (hostBundleId == null || hostBundleId.length() == 0) {
            return true;
        }
        int afterKey = topLevelKeyEnd(plist, "CFBundleIdentifier");
        if (afterKey < 0) {
            return true;
        }
        int element = nextMarkupAt(plist, afterKey);
        if (element < 0 || !"string".equals(WatchNativeBuilder.tagAt(plist, element))) {
            return true;
        }
        int openEnd = plist.indexOf('>', element);
        if (openEnd < 0 || plist.charAt(openEnd - 1) == '/') {
            return true;
        }
        int valueEnd = WatchNativeBuilder.closeOfElement(plist, openEnd + 1, "</string>");
        if (valueEnd < 0) {
            return true;
        }
        String exact = WatchNativeBuilder.plistStringContentExact(
                plist.substring(openEnd + 1, valueEnd));
        String current = exact == null ? null : exact.trim();
        if (current == null || current.length() == 0) {
            return true;
        }
        if (!exact.equals(current)) {
            // A plist parser keeps that padding, so " com.example.app.Ext " is not the identifier
            // it reads as -- it is an invalid one. Not ours to keep, whatever it trims to.
            return false;
        }
        // Through the settings, because $(PRODUCT_BUNDLE_IDENTIFIER) is not automatically safe:
        // the archive may override PRODUCT_BUNDLE_IDENTIFIER itself, with the identifier from the
        // project the extension was exported from, and those overrides are written onto this
        // target. A reference is only as good as what it lands on.
        String resolved = resolveSettingsInValue(current, archiveSettings);
        if (resolved.length() == 0) {
            // $(EXTENSION_BUNDLE_ID) with nothing defining it does not fall back to the target's
            // identifier -- Xcode expands it to the empty string and the .appex ships with no
            // identifier at all. An unknown reference is therefore the opposite of safe, and the
            // one reference that IS safe, $(PRODUCT_BUNDLE_IDENTIFIER), resolves through the
            // settings above because the caller puts the target's own identifier in them.
            return false;
        }
        if (!resolved.startsWith(hostBundleId + ".")) {
            return false;
        }
        // And equal to what the TARGET is configured with, when the caller knows it. Being under
        // the host is necessary and not sufficient: an archive naming itself
        // com.example.app.Custom in the plist while buildSettings.properties configures the
        // target as com.example.app.FromSettings is in namespace either way, and keeping the
        // literal builds a bundle declaring one identifier and signs it for the other. The
        // target's is the one every profile and export entry was chosen for, so a literal that
        // disagrees with it is replaced.
        //
        // Both conditions, in this order. Comparing with the target ALONE accepted a plist whose
        // reference resolves to an out-of-namespace target -- agreement is not enough when what
        // they agree on cannot be embedded in this app.
        String target = archiveSettings == null ? null
                : archiveSettings.get("PRODUCT_BUNDLE_IDENTIFIER");
        // Resolved, because the target is as entitled to be written through another setting as
        // the plist is: PRODUCT_BUNDLE_IDENTIFIER = $(EXTENSION_ID) is what the preflight above
        // exists to handle. Skipping the comparison on the strength of a '$' kept a literal that
        // disagrees with what the target actually resolves to, and the .appex was then built as
        // one identifier and signed as the other.
        String resolvedTarget = target == null ? null
                : resolveSettingsFully(target.trim(), archiveSettings);
        if (resolvedTarget != null && resolvedTarget.trim().length() > 0) {
            return resolved.equals(resolvedTarget.trim());
        }
        // A target this build cannot resolve is not something to judge a plist against: the
        // namespace is all that can be checked, and preflight refuses an unresolvable identifier
        // on its own terms.
        return true;
    }

    /**
     * Sets one string key among the ROOT dict's direct children, adding it when absent.
     *
     * <p>Every lookup here is anchored to the top level because a plist is full of nested
     * dictionaries that carry keys of their own -- NSExtension, CFBundleURLTypes,
     * CFBundleDocumentTypes -- and a repository-wide text search finds whichever comes first in
     * the file, not the bundle's identity. Reading a nested one as "already present" leaves the
     * real key absent, and writing to it stamps a version number into an unrelated value.</p>
     *
     * @param overwriteNonEmpty whether a value that is already there and not empty is replaced
     * when it differs. False fills only what is missing or empty, which is what an identifier
     * wants: an explicit one is the extension's own business, an empty one is no identifier at
     * all and fails the same embedded-binary validation as a missing one.
     */
    private static String setPlistString(String plist, String key, String value,
            boolean overwriteNonEmpty, Map<String, String> archiveSettings, List<String> changes) {
        if (value == null || value.length() == 0) {
            return plist;
        }
        int afterKey = topLevelKeyEnd(plist, key);
        if (afterKey < 0) {
            int dictEnd = rootDictCloseAt(plist);
            if (dictEnd < 0) {
                return plist;
            }
            changes.add("added " + key + " = " + value);
            return plist.substring(0, dictEnd)
                    + "\t<key>" + key + "</key>\n\t<string>" + value + "</string>\n"
                    + plist.substring(dictEnd);
        }
        // The key's OWN value, not the next <string> anywhere after it. A key whose value is
        // <false/> or <integer>1</integer> has no string of its own, and scanning forward lands on
        // an unrelated later one -- the trap the comment on injectedPlistString records.
        int element = nextMarkupAt(plist, afterKey);
        if (element < 0) {
            return plist;
        }
        if (!"string".equals(WatchNativeBuilder.tagAt(plist, element))) {
            // The key's OWN value, of a type it may not have: every key this stamper manages is a
            // bundle identity key and Apple requires a string. <integer>7</integer> for
            // CFBundleVersion is not a version to preserve, it is an invalid bundle -- and leaving
            // it while reporting success is how the stamper would hand back one that still fails.
            // (Wandering off to some LATER key's <string> is the different mistake, and the search
            // above is anchored to this key precisely so that cannot happen.)
            int valueEnds = endOfElement(plist, element);
            if (valueEnds < 0) {
                return plist;
            }
            changes.add("set " + key + " to " + value + " (was "
                    + WatchNativeBuilder.tagAt(plist, element) + ", which is not a string)");
            return plist.substring(0, element) + "<string>" + value + "</string>"
                    + plist.substring(valueEnds);
        }
        int openEnd = plist.indexOf('>', element);
        if (openEnd < 0) {
            return plist;
        }
        if (plist.charAt(openEnd - 1) == '/') {
            // The empty form, <string/> or <string />: XML puts the slash against the '>' whatever
            // whitespace precedes it, so one test covers both spellings. It is this key's own
            // value and it is empty, which is never a usable identifier or version -- filled
            // regardless of overwriteNonEmpty, since there is nothing here to preserve.
            changes.add("set " + key + " to " + value + " (was empty)");
            return plist.substring(0, element) + "<string>" + value + "</string>"
                    + plist.substring(openEnd + 1);
        }
        int valueEnd = WatchNativeBuilder.closeOfElement(plist, openEnd + 1, "</string>");
        if (valueEnd < 0) {
            return plist;
        }
        String current = plist.substring(openEnd + 1, valueEnd);
        if (current.equals(value)) {
            return plist;
        }
        // What the value IS, not how it is spelled: a CDATA section resolved, a comment stripped,
        // entities decoded, and TRIMMED -- both of plistStringContent's paths end in .trim().
        // <string><!-- filled in by CI --></string> is a nonzero run of text and an empty value,
        // and reading it as "an identifier is already here" leaves the extension with none.
        //
        // So every spelling of empty arrives here as "" and needs no test of its own: whitespace
        // between the tags, a comment, an empty or whitespace-only CDATA section, and any mix.
        // <string/> and <string /> are handled further up -- XML puts the slash against the '>'
        // whatever whitespace precedes it, so the character before '>' identifies both. Please do
        // not add another emptiness special case here without a failing test first; several
        // proposed ones were already covered, and the daemon's AppExtensionInfoPlistTest pins
        // each form.
        String currentText = WatchNativeBuilder.plistStringContent(current);
        if (currentText == null) {
            currentText = "";
        }
        // And the same content untrimmed, because a plist parser keeps padding wherever it is
        // written -- <string> 5.4 </string> and <string><![CDATA[ 5.4 ]]></string> both parse as
        // " 5.4 ", which Apple compares against the app's "5.4" and rejects. Emptiness is judged
        // on the trimmed text; everything else on the exact one.
        String currentExact = WatchNativeBuilder.plistStringContentExact(current);
        if (currentExact == null) {
            currentExact = "";
        }
        if (currentText.length() == 0) {
            changes.add("set " + key + " to " + value + " (was empty)");
            return plist.substring(0, openEnd + 1) + value + plist.substring(valueEnd);
        }
        if (currentExact.equals(value)) {
            return plist;
        }
        if (!overwriteNonEmpty) {
            return plist;
        }
        // A value written as $(MARKETING_VERSION) is judged by what it RESOLVES to, not by being a
        // reference. The archive's buildSettings.properties are copied into this target's build
        // configurations further down, so the reference lands on whatever they say -- a stale 1.0
        // under an app at 5.4 -- and a setting they do not define resolves to nothing at all,
        // since the target this build generates has no version settings of its own. Both fail the
        // embedded-bundle check; only a reference that already lands on the app's own version is
        // left standing.
        // Resolved from the exact text, so padding written inside CDATA or as entities counts
        // exactly as padding written outside it would.
        String resolved = resolveSettingsInValue(currentExact, archiveSettings);
        if (value.equals(resolved)) {
            return plist;
        }
        changes.add("set " + key + " to " + value + " to match the app (was " + currentText
                + (resolved.equals(currentText) ? "" : ", which resolves to '" + resolved + "' here")
                + ")");
        return plist.substring(0, openEnd + 1) + value + plist.substring(valueEnd);
    }

    /// A root written as {@code <dict/>} carries no keys and has nowhere to put one, so it is
    /// opened into a pair before anything is added to it.
    private static String openEmptyRootDict(String plist) {
        int at = rootDictAt(plist);
        int openEnd = at < 0 ? -1 : plist.indexOf('>', at);
        if (openEnd < 0 || plist.charAt(openEnd - 1) != '/') {
            return plist;
        }
        return plist.substring(0, at) + "<dict>\n</dict>" + plist.substring(openEnd + 1);
    }

    /// Index just past the {@code </key>} of {@code key}, when that key is a DIRECT child of the
    /// root dict; -1 when the root dict has no such child. Nested dictionaries are stepped over
    /// whole, so a key of the same name inside one is not mistaken for the bundle's own.
    private static int topLevelKeyEnd(String plist, String key) {
        int at = rootDictAt(plist);
        int i = at < 0 ? -1 : plist.indexOf('>', at);
        if (i < 0 || plist.charAt(i - 1) == '/') {
            return -1;
        }
        i++;
        while (true) {
            int element = nextMarkupAt(plist, i);
            if (element < 0 || plist.startsWith("</", element)) {
                return -1;
            }
            int openEnd = plist.indexOf('>', element);
            if (openEnd < 0) {
                return -1;
            }
            if (!"key".equals(WatchNativeBuilder.tagAt(plist, element))) {
                // A value with no key of ours in front of it. Step over it whole.
                i = endOfElement(plist, element);
                if (i < 0) {
                    return -1;
                }
                continue;
            }
            if (plist.charAt(openEnd - 1) == '/') {
                i = openEnd + 1;
                continue;
            }
            int close = WatchNativeBuilder.closeOfElement(plist, openEnd + 1, "</key>");
            int afterKey = close < 0 ? -1 : plist.indexOf('>', close);
            if (afterKey < 0) {
                return -1;
            }
            afterKey++;
            // The key's CONTENT resolved, so a name spelled with CDATA or wrapped in a comment
            // still matches -- an XML parser reads all of those as the same key.
            String name = WatchNativeBuilder.plistStringContent(plist.substring(openEnd + 1, close));
            if (key.equals(name)) {
                return afterKey;
            }
            int valueElement = nextMarkupAt(plist, afterKey);
            if (valueElement < 0) {
                return -1;
            }
            i = endOfElement(plist, valueElement);
            if (i < 0) {
                return -1;
            }
        }
    }

    /// The {@code <} of the plist's root dict, or -1 when this is not a dict-rooted XML plist.
    /// The declaration, the doctype and the {@code <plist>} wrapper are stepped past.
    private static int rootDictAt(String plist) {
        int i = 0;
        while (true) {
            int element = nextMarkupAt(plist, i);
            if (element < 0) {
                return -1;
            }
            int gt = plist.indexOf('>', element);
            if (gt < 0) {
                return -1;
            }
            String tag = WatchNativeBuilder.tagAt(plist, element);
            if ("dict".equals(tag)) {
                return element;
            }
            if (tag.length() > 0 && !"plist".equals(tag)) {
                // A plist rooted in an array or a bare value. It has no keys to stamp.
                return -1;
            }
            i = gt + 1;
        }
    }

    /// The {@code <} of the tag that closes the root dict, which is where a missing key is added.
    private static int rootDictCloseAt(String plist) {
        int at = rootDictAt(plist);
        int end = at < 0 ? -1 : endOfElement(plist, at);
        // end - 1, not end: endOfElement returns the index just PAST the closing '>', which in a
        // compact plist ending "</dict></plist>" is the '<' of </plist>. An inclusive search from
        // there picked that one and inserted the keys between the two closing tags.
        return end < 1 ? -1 : plist.lastIndexOf('<', end - 1);
    }

    /// Index just past the element opening at {@code element}, everything nested inside it
    /// included. Depth is counted on the element's own name, so a dict inside a dict closes in
    /// the right place.
    private static int endOfElement(String plist, int element) {
        String tag = WatchNativeBuilder.tagAt(plist, element);
        int openEnd = plist.indexOf('>', element);
        if (openEnd < 0) {
            return -1;
        }
        if (plist.charAt(openEnd - 1) == '/') {
            return openEnd + 1;
        }
        int depth = 1;
        int i = openEnd + 1;
        while (depth > 0) {
            int at = nextMarkupAt(plist, i);
            if (at < 0) {
                return -1;
            }
            int gt = plist.indexOf('>', at);
            if (gt < 0) {
                return -1;
            }
            if (plist.startsWith("</", at)) {
                if (tag.equals(closeTagAt(plist, at))) {
                    depth--;
                }
            } else if (plist.charAt(gt - 1) != '/'
                    && tag.equals(WatchNativeBuilder.tagAt(plist, at))) {
                depth++;
            }
            i = gt + 1;
        }
        return i;
    }

    /// The element name of the end tag at {@code at}, lowercased, or empty when that is not one.
    private static String closeTagAt(String plist, int at) {
        StringBuilder tag = new StringBuilder();
        for (int j = at + 2; j < plist.length() && Character.isLetterOrDigit(plist.charAt(j)); j++) {
            tag.append(plist.charAt(j));
        }
        return tag.toString().toLowerCase(java.util.Locale.ENGLISH);
    }

    /// The {@code <} of the next markup at or after {@code from}, with comments and CDATA stepped
    /// over whole so a {@code <} inside either is not read as a tag.
    private static int nextMarkupAt(String plist, int from) {
        int i = from;
        while (i < plist.length()) {
            int at = plist.indexOf('<', i);
            if (at < 0) {
                return -1;
            }
            int skipped = WatchNativeBuilder.skipMarkupBefore(plist, at, i);
            if (skipped < 0) {
                return -1;
            }
            if (skipped != at) {
                i = skipped;
                continue;
            }
            return at;
        }
        return -1;
    }

    /// The Info.plist the extension target is actually built with.
    ///
    /// {@code <folder>/Info.plist} is only the default: the archive's buildSettings.properties
    /// may point INFOPLIST_FILE at another file, and that is the one Xcode processes into the
    /// .appex. Stamping the default in that case leaves the plist that ships without the
    /// identifier and versions the stamping is there to supply.
    ///
    /// The path is written the way Xcode reads it: relative to the project directory, which is
    /// the extension folder's parent. A value that still holds a build-setting reference after
    /// the two obvious project-root spellings is not resolvable here, and null says so rather
    /// than guessing at a file to edit.
    /// The extension folder's files with the given suffix, in a fixed order.
    static List<File> extensionFilesEndingWith(File extensionFolder, String suffix) {
        List<File> out = new ArrayList<File>();
        File[] entries = extensionFolder == null ? null : extensionFolder.listFiles();
        if (entries == null) {
            return out;
        }
        for (File f : entries) {
            if (f.isFile() && f.getName().endsWith(suffix)) {
                out.add(f);
            }
        }
        Collections.sort(out, new Comparator<File>() {
            public int compare(File a, File b) {
                return a.getName().compareTo(b.getName());
            }
        });
        return out;
    }

    /// Which of them to use: the one named after the extension if it is there, else the first by
    /// name. Never the accident of directory order, because this file decides how the target is
    /// signed and what it is signed to allow.
    static File preferredExtensionFile(List<File> candidates, String extensionName, String suffix) {
        if (candidates.isEmpty()) {
            return null;
        }
        for (File f : candidates) {
            if (f.getName().equals(extensionName + suffix)) {
                return f;
            }
        }
        return candidates.get(0);
    }

    /// The first symbolic link under {@code dir} that resolves outside {@code root}, or null.
    ///
    /// unzip refuses an absolute path or a ../ traversal in an entry NAME, but it happily creates
    /// a symlink, and the entry that plants one is an ordinary-looking file. Everything under an
    /// extension folder is then handed to Xcode -- added to the target, copied into the bundle,
    /// swept into the sources tarball -- so a link pointing at the build machine's provisioning
    /// profiles or another build's directory would be read through and shipped. The build stops
    /// rather than following it.
    static File symlinkEscaping(File dir, File root) throws IOException {
        File[] entries = dir == null ? null : dir.listFiles();
        if (entries == null) {
            return null;
        }
        String rootPath = root.getCanonicalPath();
        if (!rootPath.endsWith(File.separator)) {
            rootPath += File.separator;
        }
        for (File f : entries) {
            if (!f.getCanonicalPath().startsWith(rootPath)) {
                return f;
            }
            if (Files.isSymbolicLink(f.toPath())) {
                if (f.isDirectory()) {
                    // In-tree, so it escapes nothing -- but sub/loop -> . is a cycle, and every
                    // other walk over this folder follows it until the stack ends the build. A
                    // directory symlink is not something an extension needs.
                    return f;
                }
                continue;
            }
            if (f.isDirectory()) {
                File escaping = symlinkEscaping(f, root);
                if (escaping != null) {
                    return escaping;
                }
            }
        }
        return null;
    }

    /// Whether an extension folder holds Swift anywhere in it.
    static boolean containsSwiftSource(File dir) {
        File[] entries = dir == null ? null : dir.listFiles();
        if (entries == null) {
            return false;
        }
        for (File f : entries) {
            if (f.isDirectory()) {
                if (containsSwiftSource(f)) {
                    return true;
                }
            } else if (f.getName().endsWith(".swift")) {
                return true;
            }
        }
        return false;
    }

    /// The entitlements file the extension target is signed with.
    ///
    /// CODE_SIGN_ENTITLEMENTS is a path relative to the project directory, the same shape as
    /// INFOPLIST_FILE, and the archive may point it at a file other than the one named after the
    /// extension. What it names is what Xcode signs against, so it is also what decides the
    /// entitlement-driven deployment floor. Falls back to the named pick when the setting is
    /// absent, still a placeholder, or points somewhere this build will not read.
    static File appExtensionSignedEntitlements(File extensionFolder, String configured, File byName,
            Map<String, String> settings) {
        if (configured == null || configured.trim().length() == 0 || configured.contains("$(NS_")) {
            return byName;
        }
        // From the settings MAP, not from buildSettings.properties: that file is loaded into the
        // map and deleted before this runs, so a path holding $(PRODUCT_NAME) would resolve
        // against an override that is no longer readable and quietly name a different file.
        File resolved = resolveInfoPlistPath(configured.trim(), extensionFolder, settings);
        return resolved != null && resolved.isFile() ? resolved : byName;
    }

    /// Whether an entitlements plist grants a boolean entitlement at its top level.
    ///
    /// Read as a property list, not searched as text: entitlements may be UTF-16, in which case a
    /// byte search for the key finds nothing and a Wallet extension keeps the 12.0 floor it will
    /// be rejected for; and the same string sitting in a comment, or in some unrelated value, is
    /// not a granted entitlement -- taking it for one pushes an extension to iOS 14 and drops it
    /// off the 12 and 13 devices it would have run on.
    static boolean entitlementIsTrue(File entitlements, String key) {
        if (entitlements == null || !entitlements.isFile()) {
            return false;
        }
        String text;
        try {
            text = readPlistText(entitlements).text;
        } catch (IOException cannotRead) {
            return false;
        }
        if (rootDictAt(text) < 0) {
            // Not XML we can walk -- most often a binary plist, which Xcode writes as readily as
            // it writes XML and which signs exactly the same. Answering "no entitlement" here puts
            // an issuer-provisioning extension back on the 12.0 floor Apple rejects it for.
            return binaryEntitlementGrants(entitlements, key);
        }
        int afterKey = topLevelKeyEnd(text, key);
        if (afterKey < 0) {
            return false;
        }
        int element = nextMarkupAt(text, afterKey);
        return element >= 0 && "true".equals(WatchNativeBuilder.tagAt(text, element));
    }

    /// A binary entitlements plist, read through plutil where there is one.
    ///
    /// The fallback is a search of the bytes, which is what this used to do to every entitlements
    /// file and which was wrong for XML: there a mention inside a comment or an unrelated value
    /// reads as a grant. A binary plist has no comments, and these entitlement names do not appear
    /// as ordinary text, so on the file kind that is left it is a fair answer -- and erring toward
    /// the 14.0 floor costs iOS 12 and 13 availability, while erring the other way costs the
    /// upload.
    private static boolean binaryEntitlementGrants(File entitlements, String key) {
        String xml = plutilAsXml(entitlements);
        if (xml != null && rootDictAt(xml) >= 0) {
            int afterKey = topLevelKeyEnd(xml, key);
            if (afterKey < 0) {
                return false;
            }
            int element = nextMarkupAt(xml, afterKey);
            return element >= 0 && "true".equals(WatchNativeBuilder.tagAt(xml, element));
        }
        return fileContains(entitlements, key);
    }

    /// The file as XML through /usr/bin/plutil, or null where that is not available or it refuses.
    private static String plutilAsXml(File file) {
        File plutil = new File("/usr/bin/plutil");
        if (!plutil.canExecute()) {
            return null;
        }
        try {
            Process p = new ProcessBuilder(plutil.getAbsolutePath(), "-convert", "xml1", "-o", "-",
                    file.getAbsolutePath()).redirectErrorStream(false).start();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = p.getInputStream();
            try {
                byte[] buffer = new byte[4096];
                int read = in.read(buffer);
                while (read > 0) {
                    out.write(buffer, 0, read);
                    read = in.read(buffer);
                }
            } finally {
                try { in.close(); } catch (Throwable t) {}
            }
            return p.waitFor() == 0 ? new String(out.toByteArray(), StandardCharsets.UTF_8) : null;
        } catch (Exception cannotRun) {
            return null;
        }
    }

    /// Why an extension's identifier cannot ship, or null when it can.
    ///
    /// An embedded bundle must sit under the identifier of the app that carries it -- Apple's
    /// rule, checked on upload -- so an archive whose PRODUCT_BUNDLE_IDENTIFIER points somewhere
    /// else describes an extension this app can never ship, whatever the rest of the build does.
    static String outOfNamespaceExtensionIdMessage(String extensionName, String effectiveId,
            String hostPackage) {
        if (hostPackage == null || hostPackage.length() == 0 || effectiveId == null
                || effectiveId.startsWith(hostPackage + ".")) {
            return null;
        }
        return "The " + extensionName + " app extension is set to build as '" + effectiveId
                + "', which is not under the app's own '" + hostPackage + "'. An embedded "
                + "extension must be, or Apple refuses the upload and no profile of this app's "
                + "can sign it. Fix PRODUCT_BUNDLE_IDENTIFIER in " + extensionName
                + "/buildSettings.properties, or remove it to take the default of " + hostPackage
                + "." + extensionName + ".";
    }

    /// The archive's settings plus the ones Xcode defines for this target itself.
    ///
    /// A value written through TARGET_NAME or PRODUCT_NAME -- com.example.app.$(TARGET_NAME) is
    /// an ordinary way to write an extension's identifier -- resolves on the build machine and
    /// must resolve here too. Without them the reference is simply deleted, and what was recorded
    /// for the export-options dictionary was "com.example.app.", a key matching nothing in the
    /// archive.
    static Map<String, String> extensionSettingsWithBuiltIns(File extensionFolder,
            Map<String, String> settings) {
        return extensionSettingsWithBuiltIns(extensionFolder, settings, null, null, null);
    }

    /// @param configuration, {@code sdk} and {@code arch} the archive's own, since
    /// $(CONFIGURATION) in a path or an identifier is as ordinary as $(TARGET_NAME) and this
    /// build knows all three
    /// @param context the archive's OWN, variants included. The five-argument form rebuilds one
    /// from sdk/configuration/arch, which reads BUILD_VARIANTS afresh and so loses a single
    /// variant a caller had selected -- the per-variant entitlements walk selects exactly that,
    /// and resolving its path through the rebuilt context read a [variant=...] helper by map
    /// order instead of by the variant being examined.
    static Map<String, String> extensionSettingsWithBuiltIns(File extensionFolder,
            Map<String, String> settings, ArchiveContext context) {
        return extensionSettingsWithBuiltIns(extensionFolder, settings,
                context == null ? null : context.configuration,
                context == null ? null : context.sdk,
                context == null ? null : context.arch, context);
    }

    static Map<String, String> extensionSettingsWithBuiltIns(File extensionFolder,
            Map<String, String> settings, String configuration, String sdk, String arch) {
        return extensionSettingsWithBuiltIns(extensionFolder, settings, configuration, sdk, arch,
                null);
    }

    private static Map<String, String> extensionSettingsWithBuiltIns(File extensionFolder,
            Map<String, String> settings, String configuration, String sdk, String arch,
            ArchiveContext context) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        // Conditionals resolved to what this build gets, before anything expands a reference
        // against them: a map lookup only ever sees the plain key, and the qualified value is the
        // one Xcode uses.
        Map<String, String> flat = flattenForContext(settings, context != null ? context
                : ArchiveContext.of(sdk, configuration, arch, settings));
        if (flat != null) {
            out.putAll(flat);
        }
        String targetName = extensionFolder.getName();
        out.put("TARGET_NAME", targetName);
        File projectDir = extensionFolder.getParentFile();
        String projectPath = projectDir == null ? "." : projectDir.getAbsolutePath();
        if (!out.containsKey("SRCROOT")) {
            out.put("SRCROOT", projectPath);
        }
        if (!out.containsKey("PROJECT_DIR")) {
            out.put("PROJECT_DIR", projectPath);
        }
        // A project name is one of these too, and not knowing it is what left
        // com.example.host.$(PROJECT_NAME) unresolvable: the identifier was then recorded as its
        // own source text, which names no bundle and matches no profile. It is the .xcodeproj in
        // the project directory -- the same directory SRCROOT and PROJECT_DIR come from -- and
        // only when there is exactly one, since two would be a guess.
        String projectName = singleXcodeProjectName(projectDir);
        if (projectName != null) {
            if (!out.containsKey("PROJECT_NAME")) {
                out.put("PROJECT_NAME", projectName);
            }
            if (!out.containsKey("PROJECT")) {
                out.put("PROJECT", projectName);
            }
        }
        // Family patterns are not values. A context taken from [sdk=iphonesimulator*] that this
        // archive does not match knows the family and NOT the SDK the build will use, so
        // $(SDK_NAME) is left unexpanded and the candidate is reported as one this build cannot
        // resolve -- which is true -- rather than resolved to a path nothing will ever be at.
        if (configuration != null && !isFamilyPattern(configuration)
                && !out.containsKey("CONFIGURATION")) {
            out.put("CONFIGURATION", configuration);
        }
        if (sdk != null && !isFamilyPattern(sdk)) {
            if (!out.containsKey("SDK_NAME")) {
                out.put("SDK_NAME", sdk);
            }
            if (!out.containsKey("PLATFORM_NAME")) {
                out.put("PLATFORM_NAME", platformOf(sdk));
            }
        }
        if (arch != null && !isFamilyPattern(arch)) {
            if (!out.containsKey("CURRENT_ARCH")) {
                out.put("CURRENT_ARCH", arch);
            }
            if (!out.containsKey("arch")) {
                out.put("arch", arch);
            }
        }
        // PRODUCT_NAME last, and only now: it is $(TARGET_NAME) unless the archive says otherwise,
        // and "otherwise" may be a chain through any of the settings above --
        // PRODUCT_NAME = $(CONFIGURATION)-Wallet is one. Resolving it before CONFIGURATION,
        // SDK_NAME and CURRENT_ARCH were in the map deleted the reference and left "-Wallet",
        // which then went into an identifier and into the export-options key.
        String productName = out.get("PRODUCT_NAME");
        String resolvedProductName = productName == null ? ""
                : resolveSettingsInValue(productName, out);
        out.put("PRODUCT_NAME", resolvedProductName.length() > 0 ? resolvedProductName : targetName);
        return out;
    }

    /// Whether this context value is a family rather than a value: "iphonesimulator*", from a
    /// condition that describes some build other than this one.
    static boolean isFamilyPattern(String value) {
        return value != null && value.endsWith("*");
    }

    /// The name of the Xcode project in this directory, or null when there is not exactly one.
    ///
    /// $(PROJECT_NAME) is an Xcode built-in like $(TARGET_NAME), and an identifier written
    /// through it is ordinary. Not supplying it left the identifier unresolvable, and what got
    /// recorded for the export-options dictionary was the expression itself.
    static String singleXcodeProjectName(File projectDir) {
        if (projectDir == null) {
            return null;
        }
        File[] entries = projectDir.listFiles();
        if (entries == null) {
            return null;
        }
        String found = null;
        for (File entry : entries) {
            if (entry.getName().endsWith(".xcodeproj")) {
                if (found != null) {
                    return null;
                }
                found = entry.getName().substring(0,
                        entry.getName().length() - ".xcodeproj".length());
            }
        }
        return found == null || found.length() == 0 ? null : found;
    }

    /// Every entitlements file this target may be signed with: the plain CODE_SIGN_ENTITLEMENTS
    /// and each qualified one.
    ///
    /// Xcode honours CODE_SIGN_ENTITLEMENTS[sdk=iphoneos*] over the plain setting for the device
    /// archive, so an archive that grants payment-pass-provisioning only in its device
    /// entitlements was read as granting nothing and kept the 12.0 floor Apple rejects it for.
    static List<File> appExtensionEntitlementsCandidates(File extensionFolder,
            Map<String, String> settings, File byName) {
        return appExtensionEntitlementsCandidates(extensionFolder, settings, byName, null, null);
    }

    /// The one entitlements file this archive is signed with.
    static File appExtensionSigningEntitlements(File extensionFolder, Map<String, String> settings,
            File byName, String sdk, String configuration) {
        return appExtensionSigningEntitlements(extensionFolder, settings, byName, sdk,
                configuration, null);
    }

    static File appExtensionSigningEntitlements(File extensionFolder, Map<String, String> settings,
            File byName, String sdk, String configuration, String arch) {
        return appExtensionSigningEntitlements(extensionFolder, settings, byName,
                ArchiveContext.of(sdk, configuration, arch, settings));
    }

    /// Every entitlements file this archive signs with: ONE PER BUILD VARIANT it produces.
    ///
    /// BUILD_VARIANTS = "normal profile" makes Xcode build both, and each is signed by whichever
    /// CODE_SIGN_ENTITLEMENTS applies to it -- so [variant=normal] and [variant=profile] are both
    /// applicable settings, equally specific, and picking one shared winner picked whichever the
    /// map happened to hand over first. When the other one is the file granting
    /// payment-pass-provisioning, the floor came out 12.0 for a build Apple requires 14 of.
    ///
    /// The target carries one deployment target per configuration and cannot vary it by variant,
    /// so the floor has to be the strictest of these -- which is what
    /// {@link #appExtensionDeploymentFloor(List)} does with the list.
    static List<File> appExtensionSigningEntitlementsPerVariant(File extensionFolder,
            Map<String, String> settings, File byName, ArchiveContext context) {
        List<File> out = new ArrayList<File>();
        List<String> variants = context == null || context.variants == null
                || context.variants.isEmpty()
                        ? java.util.Collections.singletonList("normal") : context.variants;
        for (String variant : variants) {
            ArchiveContext perVariant = new ArchiveContext(
                    context == null ? null : context.sdk,
                    context == null ? null : context.configuration,
                    context == null ? null : context.arch,
                    java.util.Collections.singletonList(variant));
            File signed = appExtensionSigningEntitlements(extensionFolder, settings, byName,
                    perVariant);
            if (signed != null && !out.contains(signed)) {
                out.add(signed);
            }
        }
        return out;
    }

    static File appExtensionSigningEntitlements(File extensionFolder, Map<String, String> settings,
            File byName, ArchiveContext context) {
        String winner = winningSetting(settings, "CODE_SIGN_ENTITLEMENTS", context);
        String sdk = context == null ? null : context.sdk;
        String configuration = context == null ? null : context.configuration;
        String arch = context == null ? null : context.arch;
        if (winner == null) {
            return byName;
        }
        if (winner.trim().length() == 0) {
            // Declared and empty is not the same as not declared. An archive that sets
            // CODE_SIGN_ENTITLEMENTS[sdk=iphoneos*] to nothing is telling Xcode to sign the
            // device build with no entitlements file at all, and Xcode does -- so reading the
            // by-name file here found a Wallet entitlement the target is not signed with and
            // raised it to iOS 14 for something it does not carry.
            return null;
        }

        // With the archive's context, because $(CONFIGURATION)/Extension.entitlements is a
        // standard way to write this path; without it the path did not resolve and a different
        // file was read for the entitlement that sets the floor.
        return appExtensionSignedEntitlements(extensionFolder, winner, byName,
                extensionSettingsWithBuiltIns(extensionFolder, settings, context));
    }

    /// @param sdk the SDK this build archives against ("iphoneos" or "iphonesimulator"), and
    /// {@code configuration} its configuration ("Release" or "Debug"); a qualified setting whose
    /// condition names a different one is not part of THIS archive and does not decide its floor.
    /// Null for either means "cannot tell", and then every condition counts.
    static List<File> appExtensionEntitlementsCandidates(File extensionFolder,
            Map<String, String> settings, File byName, String sdk, String configuration) {
        List<File> out = new ArrayList<File>();
        if (settings != null) {
            for (Map.Entry<String, String> setting : settings.entrySet()) {
                String key = setting.getKey();
                if (!"CODE_SIGN_ENTITLEMENTS".equals(key)
                        && !isQualified(key, "CODE_SIGN_ENTITLEMENTS")) {
                    continue;
                }
                if (!conditionApplies(key, sdk, configuration)) {
                    // A Debug-only or simulator-only entitlement is not signed into the release
                    // device archive, so raising its minimum iOS for one costs the extension every
                    // iOS 12 and 13 device for nothing.
                    continue;
                }
                File resolved = appExtensionSignedEntitlements(extensionFolder, setting.getValue(),
                        null, settings);
                if (resolved != null && !out.contains(resolved)) {
                    out.add(resolved);
                }
            }
        }
        if (out.isEmpty() && byName != null) {
            out.add(byName);
        }
        return out;
    }

    /// The floor for an extension that may be signed with any of these: the highest any of them
    /// asks for. An extension whose DEVICE entitlements need iOS 14 needs iOS 14.
    static String appExtensionDeploymentFloor(List<File> entitlements) {
        // No entitlements is the 12.0 floor, not a crash: the File overload beside this one has
        // always taken null to mean "signed with nothing", and the two should answer alike.
        if (entitlements == null) {
            return "12.0";
        }
        for (File file : entitlements) {
            if (entitlementIsTrue(file, PAYMENT_PASS_PROVISIONING)) {
                return "14.0";
            }
        }
        return "12.0";
    }

    /// The lowest iOS an extension with these entitlements may declare.
    static String appExtensionDeploymentFloor(File entitlements) {
        return entitlementIsTrue(entitlements, PAYMENT_PASS_PROVISIONING) ? "14.0" : "12.0";
    }

    /// Brings the archive's CONDITIONAL settings in line with the ones computed above, and says
    /// what it changed.
    ///
    /// Xcode honours IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*] over the plain setting for the
    /// build it matches, and every entry in buildSettings.properties is copied onto the target
    /// verbatim -- so an archive that pins a qualified 10.0, or a qualified identifier from the
    /// project it was exported from, gets exactly that on the device archive while the values
    /// computed for the base key sit unused beside them. Clamping the base alone fixed the build
    /// nobody was shipping.
    ///
    /// A qualified deployment target below the floor is raised to it; a qualified identifier that
    /// cannot be an extension of this app is dropped, which leaves the base value -- the one this
    /// builder set -- to govern.
    ///
    /// @return a note per change, for the log
    /// The three nulls below are an sdk, a configuration and an arch this caller does not know --
    /// NOT a null context. They go to ArchiveContext.of, which always returns an object, so
    /// conditionApplies never dereferences null here and "cannot tell" ends up meaning "every
    /// condition counts", which is what an unconstrained repair should do. Do not add a
    /// null-context guard downstream on the strength of this call.
    static List<String> repairQualifiedExtensionSettings(Map<String, String> settings,
            String hostPackage, String floor) {
        return repairQualifiedExtensionSettings(settings, hostPackage, floor, null, null, null);
    }

    /// @param sdk, {@code configuration} and {@code arch} the archive being built, so a setting
    /// belonging to some OTHER build is left exactly as its author wrote it. The floor here was
    /// computed from the entitlements THIS archive is signed with, and applying it to a Debug or
    /// simulator condition raised a target that has nothing to do with those entitlements -- the
    /// edit then lives on in the generated project and in sources.tar.bz2, so a later Debug build
    /// loses iOS 12 and 13 for a Wallet entitlement it never carried.
    static List<String> repairQualifiedExtensionSettings(Map<String, String> settings,
            String hostPackage, String floor, String sdk, String configuration, String arch) {
        return repairQualifiedExtensionSettings(settings, hostPackage, floor,
                ArchiveContext.of(sdk, configuration, arch, settings));
    }

    /// @param context the whole of it. Rebuilding one here from three loose values put the variant
    /// back to "normal", so an archive declaring BUILD_VARIANTS=profile had its
    /// [variant=profile] entries skipped -- and an under-floor target among them was then copied
    /// onto the target and won for the build Xcode actually makes.
    static List<String> repairQualifiedExtensionSettings(Map<String, String> settings,
            String hostPackage, String floor, ArchiveContext context) {
        return repairQualifiedExtensionSettings(settings, hostPackage, floor, context, null);
    }

    /// @param inheritedTarget the deployment target the generated PROJECT carries, which is what
    /// $(inherited) lands on for an extension target. Null when this build cannot say.
    static List<String> repairQualifiedExtensionSettings(Map<String, String> settings,
            String hostPackage, String floor, ArchiveContext context, String inheritedTarget) {
        List<String> notes = new ArrayList<String>();
        // Flattened once, for the references these values make. A qualified identifier written as
        // $(EXTENSION_ID) resolved against the raw map, which only ever answers with the plain
        // EXTENSION_ID -- so an archive whose EXTENSION_ID[config=Release] is a perfectly good
        // extension of this app had the base value read instead, and the qualified identifier
        // Xcode would have used was dropped for being out of namespace. The generated target then
        // fell back to its base bundle id while the export options still named the custom one.
        // Per key, below, rather than one map for the pass: a qualified entry can reference a
        // helper that is qualified too, and one flattening of the archive's context answers with
        // whichever value the map happened to hand over. This map is the fallback for keys that
        // constrain nothing.
        Map<String, String> flat = flattenForContext(settings, context);
        for (Map.Entry<String, String> setting : new ArrayList<Map.Entry<String, String>>(
                settings.entrySet())) {
            String key = setting.getKey();
            // An identifier out of the host's namespace can never be embedded in this app -- in
            // ANY configuration -- so that repair runs on inactive keys too. Every entry here is
            // copied onto the target verbatim, so a [config=Debug] identifier from another
            // project survived into the generated project and failed the Debug build later. The
            // deployment floor is the opposite: it was computed from the entitlements THIS
            // archive is signed with, and applying it to a build that carries different ones
            // raises a target for something it does not have.
            boolean applies = conditionApplies(key, context);
            if (!applies && !isQualified(key, "PRODUCT_BUNDLE_IDENTIFIER")) {
                continue;
            }
            String value = setting.getValue() == null ? "" : setting.getValue().trim();
            // What the value RESOLVES to, since a qualified setting may be written through another
            // one -- IPHONEOS_DEPLOYMENT_TARGET[sdk=iphoneos*] = $(EXTENSION_MIN) with
            // EXTENSION_MIN = 16.0 is a perfectly good iOS 16 target. Comparing the raw text made
            // "$(EXTENSION_MIN)" parse as no version at all, read as below the floor, and be
            // overwritten with 12.0 -- taking an extension that compiles against iOS 16 APIs down
            // with it. An identifier that resolves to nothing is left exactly as written: this
            // build cannot evaluate it, which is not the same as knowing it is wrong. A
            // deployment target is the other way round, for the reason below.
            String resolved = resolveSettingsInValue(value, key.indexOf('[') < 0 ? flat
                    : flattenForContext(settings, contextForCondition(key, context)));
            if (resolved.length() == 0) {
                // Nothing left after expansion. For a deployment target that is not "cannot
                // tell": Xcode expands the same missing reference to the same nothing, and an
                // empty IPHONEOS_DEPLOYMENT_TARGET is not the base value -- it is no minimum at
                // all, so the qualified entry overrides the clamped base with a blank and the
                // floor is bypassed. Raised to the floor like any other under-floor value.
                //
                // Except $(inherited), which is not a setting this build failed to find but a
                // directive: Xcode replaces it with the value from the level above, and writing
                // the floor over it would pin an extension that inherits iOS 16 down to 12.
                if (!applies || !isQualified(key, "IPHONEOS_DEPLOYMENT_TARGET")) {
                    continue;
                }
                if (value.toLowerCase().indexOf("$(inherited)") >= 0
                        || value.toLowerCase().indexOf("${inherited}") >= 0) {
                    // $(inherited) on an extension target inherits the PROJECT's deployment
                    // target, not the base setting clamped above it -- and this builder writes
                    // that project value itself, from the app's own floor. Leaving the expression
                    // alone therefore hands a Wallet extension whatever the app got, which is
                    // below 14 by default: the entitlement's floor is bypassed by an expression
                    // that looked deliberate. It is kept only when what it inherits already
                    // clears the floor.
                    if (inheritedTarget != null && inheritedTarget.trim().length() > 0
                            && !isDeploymentTargetBelow(inheritedTarget.trim(), floor)) {
                        continue;
                    }
                    settings.put(key, floor);
                    notes.add(key + " = " + value + " inherits the project's "
                            + (inheritedTarget == null || inheritedTarget.trim().length() == 0
                                    ? "deployment target" : inheritedTarget.trim())
                            + ", which does not clear " + floor + ", so it was set to " + floor);
                    continue;
                }
                settings.put(key, floor);
                notes.add(key + " = " + value + " resolves to nothing, and an empty deployment "
                        + "target is no minimum at all, so it was set to " + floor);
                continue;
            }
            if (applies && isQualified(key, "IPHONEOS_DEPLOYMENT_TARGET")
                    && isDeploymentTargetBelow(resolved, floor)) {
                settings.put(key, floor);
                notes.add(key + " raised from " + value + " to " + floor);
            } else if (isQualified(key, "PRODUCT_BUNDLE_IDENTIFIER") && hostPackage != null
                    && !resolved.startsWith(hostPackage + ".")) {
                settings.remove(key);
                notes.add(key + " = " + value + " dropped, since an embedded extension must be "
                        + "under " + hostPackage);
            }
        }
        notes.addAll(dropHelpersThatLeaveTheNamespace(settings, hostPackage, context));
        return notes;
    }

    /// Every setting name {@code value} is built from, following references through the settings
    /// as far as they go.
    ///
    /// The closure, not just the names written in the text: PRODUCT_BUNDLE_IDENTIFIER =
    /// $(EXTENSION_ID) with EXTENSION_ID = com.example.$(SUFFIX) is built from both.
    static java.util.Set<String> referencedSettingNames(String value, Map<String, String> settings) {
        java.util.Set<String> out = new java.util.LinkedHashSet<String>();
        if (value == null) {
            return out;
        }
        List<String> pending = new ArrayList<String>();
        pending.add(value);
        for (int step = 0; step < MAX_SETTING_EXPANSIONS && !pending.isEmpty(); step++) {
            List<String> next = new ArrayList<String>();
            for (String text : pending) {
                java.util.regex.Matcher m = BUILD_SETTING_REFERENCE.matcher(text);
                while (m.find()) {
                    String name = m.group().substring(2, m.group().length() - 1);
                    int modifier = name.indexOf(':');
                    if (modifier >= 0) {
                        name = name.substring(0, modifier);
                    }
                    if (!out.add(name) || settings == null) {
                        continue;
                    }
                    // Its own value, and every qualified form of it: a reference reaches whichever
                    // one the build it describes selects.
                    for (Map.Entry<String, String> setting : settings.entrySet()) {
                        String key = setting.getKey();
                        int open = key.indexOf('[');
                        if (name.equals(open < 0 ? key : key.substring(0, open).trim())
                                && setting.getValue() != null) {
                            next.add(setting.getValue());
                        }
                    }
                }
            }
            pending = next;
        }
        return out;
    }

    /// Drops a QUALIFIED helper that takes the extension's identifier out of the host's namespace
    /// in the build it describes.
    ///
    /// The condition does not have to be on PRODUCT_BUNDLE_IDENTIFIER to decide it. An
    /// unqualified identifier written as $(EXTENSION_ID), over a host-prefixed base EXTENSION_ID
    /// and an EXTENSION_ID[config=Debug] from the project the archive came from, passes every
    /// check this build makes -- they all resolve in the archive's own context -- and both
    /// settings are copied onto the target, so the Debug build expands the same identifier to the
    /// foreign value and cannot be signed.
    ///
    /// The qualified helper is what makes it foreign, so that is what goes; the base helper then
    /// governs every configuration, which is the identifier the profile was issued for. The build
    /// is not refused over it: the archive being made here is fine, and refusing would fail builds
    /// that ship correctly today.
    private static List<String> dropHelpersThatLeaveTheNamespace(Map<String, String> settings,
            String hostPackage, ArchiveContext context) {
        List<String> notes = new ArrayList<String>();
        if (settings == null || hostPackage == null || hostPackage.length() == 0) {
            return notes;
        }
        String declaredId = settings.get("PRODUCT_BUNDLE_IDENTIFIER");
        if (declaredId == null || declaredId.indexOf('$') < 0) {
            return notes;
        }
        // Only when the identifier is sound as this archive has it: an identifier already out of
        // namespace here is the plain refusal's business, not this one's.
        String active = resolveSettingsInValue(declaredId, flattenForContext(settings, context));
        if (active.length() == 0 || !active.startsWith(hostPackage + ".")) {
            return notes;
        }
        // Only the settings the identifier is actually built from. Every qualified setting that
        // DESCRIBES the offending build is not the same thing as every setting that CAUSES it:
        // dropping on the context alone deleted a CODE_SIGN_ENTITLEMENTS[config=Debug] sitting
        // beside the culprit, and that target then signed with the wrong entitlements.
        java.util.Set<String> referenced = referencedSettingNames(declaredId, settings);
        for (String key : new ArrayList<String>(settings.keySet())) {
            if (key.indexOf('[') < 0 || isQualified(key, "PRODUCT_BUNDLE_IDENTIFIER")) {
                continue;
            }
            if (!referenced.contains(key.substring(0, key.indexOf('[')).trim())) {
                continue;
            }
            ArchiveContext own = contextForCondition(key, context);
            String inThatBuild = resolveSettingsInValue(declaredId,
                    flattenForContext(settings, own));
            if (inThatBuild.length() == 0 || inThatBuild.startsWith(hostPackage + ".")) {
                continue;
            }
            String removed = settings.remove(key);
            notes.add(key + " = " + removed + " would make the extension's identifier "
                    + inThatBuild + ", which is not under " + hostPackage + ", so it was dropped "
                    + "and the unqualified value governs that build too");
        }
        return notes;
    }


    /// The iOS SDK this build archives against.
    ///
    /// A local device build passes no -sdk at all and lets the destination pick the active one,
    /// so there is no version to assume -- and assuming a stale one (14.4 was the default in the
    /// hint) made an exact qualifier like [sdk=iphoneos26.0] read as some other build's. The hint
    /// answers when it is set; otherwise xcrun is asked, and if that cannot answer either the
    /// bare platform name is used, which matches any version of it.
    String activeIosSdkName(BuildRequest request) {
        // Deliberately NOT the ios.sdk hint. This builder passes no -sdk to xcodebuild -- the
        // destination picks the active SDK -- and the hint has no other use in this module, so it
        // does not control the archive. Matching an exact [sdk=iphoneos14.4] condition against a
        // stale hint under Xcode 26 selects settings the build never applies.
        try {
            // Through the Xcode this build actually uses. resolveXcodebuild honours XCODEBUILD,
            // DEVELOPER_DIR and XCODE_APP, and asking the system default instead can report a
            // different installation's SDK -- which then makes an exact [sdk=iphoneosNN] condition
            // match, or fail to match, on a version this archive never sees.
            ProcessBuilder builder = new ProcessBuilder(xcrunForSelectedXcode(), "--sdk",
                    "iphoneos", "--show-sdk-version");
            String developerDir = selectedDeveloperDir();
            if (developerDir != null) {
                builder.environment().put("DEVELOPER_DIR", developerDir);
            }
            Process p = builder.redirectErrorStream(false).start();
            java.io.BufferedReader in = new java.io.BufferedReader(
                    new java.io.InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
            String version;
            try {
                version = in.readLine();
            } finally {
                in.close();
            }
            if (p.waitFor() == 0 && version != null && version.trim().length() > 0) {
                return "iphoneos" + version.trim();
            }
        } catch (Exception noXcrun) {
            // Not a Mac, or no Xcode: the bare platform name still matches every version of it.
        }
        return "iphoneos";
    }

    /// The xcrun beside the xcodebuild this build selected, or the system one.
    private String xcrunForSelectedXcode() {
        String selected = resolveXcodebuild();
        if (selected != null) {
            File beside = new File(new File(selected).getParentFile(), "xcrun");
            if (beside.canExecute()) {
                return beside.getAbsolutePath();
            }
        }
        return "/usr/bin/xcrun";
    }

    /// The developer directory of the selected Xcode -- <Xcode.app>/Contents/Developer -- so a
    /// tool run through the system xcrun still resolves inside it. Null when it cannot be told.
    private String selectedDeveloperDir() {
        String fromEnvironment = System.getenv("DEVELOPER_DIR");
        if (fromEnvironment != null && fromEnvironment.length() > 0) {
            return fromEnvironment;
        }
        String selected = resolveXcodebuild();
        if (selected == null) {
            return null;
        }
        // .../Contents/Developer/usr/bin/xcodebuild -> .../Contents/Developer
        File developer = new File(selected).getParentFile();
        for (int i = 0; i < 2 && developer != null; i++) {
            developer = developer.getParentFile();
        }
        return isDeveloperDir(developer) ? developer.getAbsolutePath() : null;
    }

    /// Whether this really is an Xcode developer directory.
    ///
    /// Two levels up from /usr/bin/xcodebuild -- the shim most machines have on PATH, and what
    /// `which xcodebuild` reports into XCODEBUILD -- is the filesystem root, where usr/bin exists
    /// and is not a developer directory at all. Handing DEVELOPER_DIR=/ to xcrun makes it fail,
    /// the SDK name falls back to the unversioned "iphoneos", and an exact [sdk=iphoneosNN]
    /// condition is then decided by map order rather than by the SDK the archive is built with.
    ///
    /// Platforms is the thing no other directory has: the CommandLineTools tree carries usr/bin
    /// without it, and so does the root.
    static boolean isDeveloperDir(File developer) {
        return developer != null
                && new File(developer, "usr/bin/xcodebuild").isFile()
                && new File(developer, "Platforms").isDirectory();
    }

    /// The archive's settings with every conditional resolved to the value THIS build gets.
    ///
    /// A reference is expanded against a map, and a map lookup only ever sees the plain key -- so
    /// $(MARKETING_VERSION) resolved to the base 5.4 while MARKETING_VERSION[sdk=iphoneos*] = 5.3
    /// sat beside it and was the value Xcode used for the device archive. The extension then
    /// carried a version its containing app does not have, which is the rejection this stamping
    /// exists to prevent. Flattened here, once, so everything downstream expands against the
    /// values the build really has.
    static Map<String, String> flattenForContext(Map<String, String> settings,
            ArchiveContext context) {
        if (settings == null) {
            return null;
        }
        Map<String, String> flat = new LinkedHashMap<String, String>();
        for (String key : settings.keySet()) {
            int open = key.indexOf('[');
            String name = open < 0 ? key : key.substring(0, open);
            if (flat.containsKey(name)) {
                continue;
            }
            String winner = context == null ? settings.get(name)
                    : winningSetting(settings, name, context);
            if (winner != null) {
                flat.put(name, winner);
            }
        }
        return flat;
    }

    /// The context a qualified setting describes, over the top of the archive's own.
    ///
    /// INFOPLIST_FILE[config=Debug] = $(CONFIGURATION)/Info.plist means Debug/Info.plist, not
    /// Release/Info.plist -- resolving every candidate in the ACTIVE context stamped a file that
    /// belongs to another configuration and left the one the qualifier names untouched. Whatever
    /// the condition does not name stays as this archive has it.
    static ArchiveContext contextForCondition(String key, ArchiveContext context) {
        ArchiveContext active = context;
        int open = key == null ? -1 : key.indexOf('[');
        if (open < 0) {
            return active;
        }
        String sdk = active == null ? null : active.sdk;
        String configuration = active == null ? null : active.configuration;
        String arch = active == null ? null : active.arch;
        List<String> variants = active == null ? null : active.variants;
        for (String condition : key.substring(open).split("[\\[\\],]")) {
            int equals = condition.indexOf('=');
            if (equals < 0) {
                continue;
            }
            String name = condition.substring(0, equals).trim();
            String value = condition.substring(equals + 1).trim();
            boolean pattern = value.endsWith("*");
            if (pattern) {
                // A pattern names a family. If THIS archive is in that family, its own value is
                // the one Xcode will expand -- $(SDK_NAME) under [sdk=iphoneos*] is iphoneos14.4,
                // not "iphoneos", and looking for the stem's file left the real one unstamped.
                // Only when the pattern describes some other build does the stem stand in for it.
                String archiveValue = "sdk".equals(name) ? (context == null ? null : context.sdk)
                        : "config".equals(name) ? (context == null ? null : context.configuration)
                        : "arch".equals(name) ? (context == null ? null : context.arch) : null;
                if (archiveValue != null && matchesCondition(value, archiveValue)) {
                    continue;
                }
                // The variant is a LIST, so it is asked the same question a different way: if one
                // of this archive's variants is in the family, the family describes this build
                // and the archive's own variants are what Xcode expands [variant=...] helpers
                // against. Falling through to the stem put "prof" in place of "profile", and an
                // EXTENSION_ID[variant=profile] then matched nothing -- the identifier read as
                // unresolvable and was dropped.
                if ("variant".equals(name) && context != null && context.variants != null) {
                    // Narrowed to the ones that match, not kept whole. [variant=prof*] describes
                    // this build when profile is among its variants -- but the entry belongs to
                    // profile alone, and leaving normal in the context let an
                    // EXTENSION_ID[variant=normal] answer for it by map order, resolving the
                    // profile identifier out of the normal helper.
                    List<String> matching = new ArrayList<String>();
                    for (String own : context.variants) {
                        if (matchesCondition(value, own)) {
                            matching.add(own);
                        }
                    }
                    if (!matching.isEmpty()) {
                        variants = matching;
                        continue;
                    }
                }
                // The pattern is kept whole, star and all. Stripped to its stem it read as a
                // concrete value and was expanded as one: [sdk=iphonesimulator*] with
                // Wallet/$(SDK_NAME)/Info.plist went looking for Wallet/iphonesimulator, while
                // the simulator build that file belongs to expands SDK_NAME to a versioned
                // iphonesimulator18.0 -- so the wrong path was reported missing and the real
                // plist was never stamped. As a pattern it still matches the family for
                // conditionApplies, and the built-ins it cannot supply are simply not supplied.
            }
            if (value.length() == 0) {
                continue;
            }
            if ("sdk".equals(name)) {
                sdk = value;
            } else if ("config".equals(name)) {
                configuration = value;
            } else if ("arch".equals(name)) {
                arch = value;
            } else if ("variant".equals(name)) {
                variants = java.util.Collections.singletonList(value);
            }
        }
        return new ArchiveContext(sdk, configuration, arch, variants);
    }

    /// What this archive IS, for matching conditional build settings against.
    ///
    /// These four travelled as loose parameters and kept growing -- and the one that was NOT a
    /// parameter, the build variant, was hard-coded to "normal" and turned out to be settable by
    /// the archive (BUILD_VARIANTS). Held together here so the next dimension is one field rather
    /// than a signature change in six places.
    static final class ArchiveContext {
        final String sdk;
        final String configuration;
        final String arch;
        /// Every variant this build produces; a [variant=...] condition applies if it names one.
        final List<String> variants;

        ArchiveContext(String sdk, String configuration, String arch, List<String> variants) {
            this.sdk = sdk;
            this.configuration = configuration;
            this.arch = arch;
            this.variants = variants;
        }

        /// The four dimensions, for logs and for use as a map key.
        @Override
        public String toString() {
            return "sdk=" + sdk + ",config=" + configuration + ",arch=" + arch
                    + ",variants=" + variants;
        }

        /// @param settings the extension's own, since BUILD_VARIANTS in them is copied onto the
        /// target and decides which variants Xcode actually builds
        static ArchiveContext of(String sdk, String configuration, String arch,
                Map<String, String> settings) {
            List<String> variants = new ArrayList<String>();
            // The variant list is what the OTHER conditions are matched against, so it has to be
            // chosen before any of them and cannot be chosen BY one: this bootstrap context knows
            // the archive's sdk, configuration and architecture and has no variants at all, which
            // makes a [variant=...] qualifier on BUILD_VARIANTS itself apply to nothing. Xcode is
            // in the same position -- the setting decides the variants, so it cannot be selected
            // by them -- and reading only the plain key missed BUILD_VARIANTS[sdk=iphoneos*],
            // which Xcode does honour: preflight then judged the device archive as "normal" and
            // skipped the [variant=profile] target that outranked the clamped base on it.
            ArchiveContext selection = new ArchiveContext(sdk, configuration, arch,
                    java.util.Collections.<String>emptyList());
            String declaredRaw = settings == null ? null
                    : winningSetting(settings, "BUILD_VARIANTS", selection);
            // Resolved first: BUILD_VARIANTS = $(EXTENSION_VARIANTS) is a chain Xcode expands, and
            // splitting the raw text recorded "$(EXTENSION_VARIANTS)" as the variant -- so the
            // [variant=profile] settings Xcode applies were matched against a literal reference.
            // Against the flattened settings, since the helper it names may itself be qualified.
            String declared = declaredRaw == null ? null
                    : resolveSettingsInValue(declaredRaw, flattenForContext(settings, selection));
            if (declared != null) {
                for (String variant : declared.trim().split("\\s+")) {
                    if (variant.length() > 0) {
                        variants.add(variant);
                    }
                }
            }
            if (variants.isEmpty()) {
                variants.add("normal");
            }
            return new ArchiveContext(sdk, configuration, arch, variants);
        }
    }

    /// The value of {@code name} that governs THIS archive.
    ///
    /// Xcode does not merge a qualified setting with the plain one, it OVERRIDES it: with both
    /// CODE_SIGN_ENTITLEMENTS and CODE_SIGN_ENTITLEMENTS[sdk=iphoneos*] present, the device
    /// archive is signed with the qualified file alone. Reading both and taking the stricter
    /// answer raised an extension to iOS 14 for an entitlement in a file it is not signed with;
    /// reading only the plain one missed the entitlement that is. The most specific applicable
    /// condition wins, which is Xcode's own rule, and the plain setting is the least specific
    /// thing there is.
    ///
    /// @return the winning value, or null when nothing applicable is declared
    static String winningSetting(Map<String, String> settings, String name, String sdk,
            String configuration) {
        return winningSetting(settings, name, sdk, configuration, null);
    }

    static String winningSetting(Map<String, String> settings, String name, String sdk,
            String configuration, String arch) {
        return winningSetting(settings, name,
                ArchiveContext.of(sdk, configuration, arch, settings));
    }

    static String winningSetting(Map<String, String> settings, String name,
            ArchiveContext context) {
        String key = winningSettingKey(settings, name, context);
        return key == null ? null : settings.get(key);
    }

    /// The KEY of the winning setting, for callers that need to know how specific the winner was
    /// rather than only what it said.
    static String winningSettingKey(Map<String, String> settings, String name,
            ArchiveContext context) {
        if (settings == null) {
            return null;
        }
        String winner = null;
        long winningSpecificity = -1;
        for (Map.Entry<String, String> setting : settings.entrySet()) {
            String key = setting.getKey();
            boolean qualified = isQualified(key, name);
            if (!qualified && !name.equals(key)) {
                continue;
            }
            if (qualified && !conditionApplies(key, context)) {
                continue;
            }
            long specificity = qualified ? conditionSpecificity(key) : 0;
            if (specificity > winningSpecificity) {
                winningSpecificity = specificity;
                winner = key;
            }
        }
        return winner;
    }

    /// How specific a qualified key is, the way Xcode ranks it.
    ///
    /// More conditions beat fewer, and an EXACT value beats a wildcard: with
    /// [sdk=iphoneos26.0] beside [sdk=iphoneos*], Xcode uses the exact one. Counting the '='
    /// characters alone scored those equal and left the winner to Properties' iteration order --
    /// which could read the wrong entitlements file, compute the wrong floor, or judge the wrong
    /// identifier, all silently.
    static long conditionSpecificity(String key) {
        int open = key.indexOf('[');
        if (open < 0) {
            return 0;
        }
        long conditions = 0;
        long precision = 0;
        for (String condition : key.substring(open).split("[\\[\\],]")) {
            int equals = condition.indexOf('=');
            if (equals < 0) {
                continue;
            }
            conditions++;
            String value = condition.substring(equals + 1).trim();
            // An exact value beats any wildcard; between wildcards the longer prefix is the
            // narrower pattern, which is the one Xcode picks: [sdk=iphoneos14.*] over
            // [sdk=iphoneos*] for an iphoneos14.4 archive. Scoring every wildcard alike left that
            // to Properties' iteration order.
            precision += value.endsWith("*") ? value.length() - 1 : PRECISION_EXACT;
        }
        // Conditions first, precision as the tiebreak: two conditions describe a narrower build
        // than one, however precisely that one is written.
        return conditions * PRECISION_SCALE + Math.min(precision, PRECISION_SCALE - 1);
    }

    /// A value with no wildcard is as precise as a condition gets.
    private static final long PRECISION_EXACT = 1000;

    /// Wide enough that the precision sum cannot reach into the condition count above it.
    private static final long PRECISION_SCALE = 1000000;

    /// Whether a qualified setting's condition can apply to the build being made.
    ///
    /// Only the two conditions this build knows its own answer to are judged -- sdk and config.
    /// Anything else (arch, variant, a spelling not seen here) counts as applicable: guessing that
    /// a condition does not apply risks signing an extension without an entitlement it needs,
    /// which fails the upload, while over-counting only costs iOS 12 and 13 availability.
    static boolean conditionApplies(String key, String sdk, String configuration) {
        return conditionApplies(key, sdk, configuration, null);
    }

    /// @param arch the architecture the archive is built for, so [arch=arm64] and [arch=x86_64]
    /// are not both counted applicable and then decided by map order
    static boolean conditionApplies(String key, String sdk, String configuration, String arch) {
        return conditionApplies(key, new ArchiveContext(sdk, configuration, arch,
                java.util.Collections.singletonList("normal")));
    }

    static boolean conditionApplies(String key, ArchiveContext context) {
        // A null context is "no archive in hand" -- the two-argument appExtensionBundleId asks
        // exactly that -- and it means the same thing here as a context whose fields are null:
        // this build cannot tell, so every condition counts. Dereferencing it instead threw on
        // the first qualified key, which is a crash where the answer was simply unknown.
        String sdk = context == null ? null : context.sdk;
        String configuration = context == null ? null : context.configuration;
        String arch = context == null ? null : context.arch;
        int open = key.indexOf('[');
        if (open < 0) {
            return true;
        }
        for (String condition : key.substring(open).split("[\\[\\],]")) {
            int equals = condition.indexOf('=');
            if (equals < 0) {
                continue;
            }
            String name = condition.substring(0, equals).trim();
            String value = condition.substring(equals + 1).trim();
            if ("sdk".equals(name) && sdk != null && !matchesSdkCondition(value, sdk)) {
                return false;
            }
            if ("config".equals(name) && configuration != null
                    && !matchesCondition(value, configuration)) {
                return false;
            }
            if ("arch".equals(name) && arch != null && !matchesCondition(value, arch)) {
                return false;
            }
            // Against the variants this build actually produces. "normal" unless the extension's
            // own BUILD_VARIANTS says otherwise -- that setting is copied onto the target, so an
            // archive declaring profile really is built as profile and its [variant=profile]
            // settings are the ones Xcode applies.
            if ("variant".equals(name)) {
                // Same rule as the three above when there is no context to match against: unknown
                // is not "does not apply".
                List<String> against = context == null || context.variants == null
                        ? null : context.variants;
                boolean matches = against == null;
                if (against != null) {
                    for (String variant : against) {
                        matches |= matchesCondition(value, variant);
                    }
                }
                if (!matches) {
                    return false;
                }
            }
        }
        return true;
    }

    /// An sdk condition against the SDK this build names, which is versioned: xcodebuild is given
    /// iphoneos14.4, not iphoneos. [sdk=iphoneos*] and [sdk=iphoneos14.4] both mean this archive,
    /// and so does [sdk=iphoneos] -- a condition and an SDK that name the same platform match,
    /// and a version is only compared when both carry one. Erring toward applicable, as
    /// everywhere in this matching: excluding a condition that does apply loses an entitlement.
    private static boolean matchesSdkCondition(String value, String sdk) {
        if (value.endsWith("*")) {
            return matchesCondition(value, sdk);
        }
        String conditionPlatform = platformOf(value);
        String sdkPlatform = platformOf(sdk);
        if (!conditionPlatform.equalsIgnoreCase(sdkPlatform)) {
            return false;
        }
        String conditionVersion = value.substring(conditionPlatform.length());
        String sdkVersion = sdk.substring(sdkPlatform.length());
        if (sdkVersion.length() == 0) {
            // This build does not know its own SDK version -- a local archive lets the destination
            // choose it -- so a versioned condition may or may not be this one. Counted, by the
            // same rule as any condition that cannot be evaluated here.
            return true;
        }
        // Xcode matches an unwildcarded condition against the versioned SDK_NAME exactly, so
        // [sdk=iphoneos] does NOT apply to an archive built with iphoneos14.4; [sdk=iphoneos*] is
        // the spelling that does. Treating the bare one as a match picked settings Xcode ignores:
        // an entitlements file the target is not signed with, or an identifier it is not built
        // with. Erring toward applicable is for what this build cannot evaluate, not for what it
        // can evaluate and Xcode says no to.
        return conditionVersion.equals(sdkVersion);
    }

    /// The letters an SDK name starts with, which is its platform: "iphoneos" of "iphoneos14.4".
    private static String platformOf(String sdk) {
        int i = 0;
        while (i < sdk.length() && Character.isLetter(sdk.charAt(i))) {
            i++;
        }
        return sdk.substring(0, i);
    }

    /// A condition value against what this build is, with Xcode's trailing {@code *}.
    private static boolean matchesCondition(String value, String actual) {
        if (value.endsWith("*")) {
            return actual.regionMatches(true, 0, value, 0, value.length() - 1);
        }
        return value.equalsIgnoreCase(actual);
    }

    /// Whether a settings key is the conditional form of {@code name}, as Xcode writes it and as
    /// Properties preserves it only when the '=' inside the brackets is escaped.
    private static boolean isQualified(String key, String name) {
        return key.startsWith(name + "[") && key.endsWith("]");
    }

    /// The minimum iOS version a brought-in app extension declares.
    ///
    /// Xcode writes the target's IPHONEOS_DEPLOYMENT_TARGET into the built .appex as
    /// MinimumOSVersion, and App Store validation reads it there: an extension below what its own
    /// APIs require is rejected on upload, after a build that succeeded and an archive that
    /// exported. The generic path used to hand every extension 10.0, which is below the floor the
    /// current SDK will even build against, let alone what a Wallet extension needs.
    ///
    /// What the archive says wins, because an extension knows which APIs it calls -- but only
    /// above the floor, which is 14.0 when its entitlements ask for payment-pass-provisioning
    /// (PKIssuerProvisioningExtensionHandler is an iOS 14 API and Apple rejects anything lower)
    /// and 12.0 otherwise, the lowest the current SDK builds against. With nothing declared the
    /// app's own target is used, under the same floor.
    ///
    /// @param declared the extension's own IPHONEOS_DEPLOYMENT_TARGET, or null
    /// @param entitlements the extension's .entitlements, or null when it has none
    /// @param appTarget the ios.deployment_target build hint, or null
    static String appExtensionDeploymentTarget(String declared, File entitlements, String appTarget) {
        return appExtensionDeploymentTarget(declared, entitlements, appTarget, null, null);
    }

    /// @param extensionFolder and {@code settings}, so a declared target written as
    /// $(EXTENSION_MIN) is judged by the version it resolves to rather than parsed as none
    static String appExtensionDeploymentTarget(String declared, File entitlements, String appTarget,
            File extensionFolder, Map<String, String> settings) {
        return appExtensionDeploymentTarget(declared, entitlements, appTarget, extensionFolder,
                settings, null);
    }

    static String appExtensionDeploymentTarget(String declared, File entitlements, String appTarget,
            File extensionFolder, Map<String, String> settings, ArchiveContext context) {
        return appExtensionDeploymentTarget(declared,
                entitlements == null ? new ArrayList<File>() : Arrays.asList(entitlements),
                appTarget, extensionFolder, settings, context);
    }

    /// @param entitlements every file this target may be signed with, since a qualified
    /// CODE_SIGN_ENTITLEMENTS can be the one that carries the Wallet entitlement
    static String appExtensionDeploymentTarget(String declared, List<File> entitlements,
            String appTarget, File extensionFolder, Map<String, String> settings) {
        return appExtensionDeploymentTarget(declared, entitlements, appTarget, extensionFolder,
                settings, null);
    }

    /// @param context the archive's, so a target written through another setting is resolved with
    /// the conditionals THIS build gets. Rebuilding an empty context here let an inactive
    /// qualifier -- EXTENSION_MIN[config=Debug] beside a Release build -- win by specificity, and
    /// the expression was then kept on the strength of a value Xcode never expands it to.
    static String appExtensionDeploymentTarget(String declared, List<File> entitlements,
            String appTarget, File extensionFolder, Map<String, String> settings,
            ArchiveContext context) {
        return appExtensionDeploymentTarget(declared, entitlements, appTarget, extensionFolder,
                settings, context, null);
    }

    /// @param inheritedTarget what the generated PROJECT carries, which is what $(inherited)
    /// lands on. Not the same thing as {@code appTarget}: that one is the ios.deployment_target
    /// hint and is absent unless the developer set it, while the project always has a target --
    /// this builder computes it from the app's own floors. Passing only the hint meant an
    /// extension inheriting the project's minimum was written down to the extension floor
    /// whenever the hint was unset, which is the default case.
    static String appExtensionDeploymentTarget(String declared, List<File> entitlements,
            String appTarget, File extensionFolder, Map<String, String> settings,
            ArchiveContext context, String inheritedTarget) {
        // The floor is a floor, not a default. An archive exported from an old project may carry
        // IPHONEOS_DEPLOYMENT_TARGET = 10.0 of its own, and honouring that unconditionally would
        // reproduce the very rejection this exists to prevent -- 10.0 does not even build against
        // the current SDK, and an issuer-provisioning Wallet extension is refused below 14.
        String floor = appExtensionDeploymentFloor(entitlements);
        String chosen = declared != null && declared.trim().length() > 0
                ? declared.trim()
                : appTarget;
        if (chosen != null && chosen.indexOf('$') >= 0) {
            // $(inherited) on an extension target inherits the PROJECT's deployment target, which
            // is what appTarget carries. Expanded like any other unknown name it came out empty
            // and the floor replaced it, so an extension inheriting iOS 16 was written down to 12
            // or 14 and its code lost the availability the newer APIs need. Substituted first,
            // and only when this build knows what it inherits.
            String inherits = inheritedTarget != null && inheritedTarget.trim().length() > 0
                    ? inheritedTarget.trim() : appTarget;
            if (inherits != null && inherits.trim().length() > 0) {
                chosen = chosen.replace("$(inherited)", inherits.trim())
                        .replace("${inherited}", inherits.trim());
                if (chosen.indexOf('$') < 0) {
                    return isDeploymentTargetBelow(chosen, floor) ? floor
                            : normalizeVersion(chosen.trim());
                }
            }
            // Written through another setting. What it RESOLVES to decides: a reference that lands
            // on a version clearing the floor is kept as written, because Xcode resolves it on the
            // target and that is the archive author's expression to keep.
            String resolved = extensionFolder == null ? "" : resolveSettingsInValue(chosen,
                    context == null
                            ? extensionSettingsWithBuiltIns(extensionFolder, settings)
                            : extensionSettingsWithBuiltIns(extensionFolder, settings, context));
            if (resolved.length() == 0) {
                // And a reference to a setting nothing defines is not "unknown" -- Xcode expands
                // it to the empty string, so the extension would declare no minimum at all. The
                // floor is the answer, not the expression.
                return floor;
            }
            return isDeploymentTargetBelow(resolved, floor) ? floor : chosen;
        }
        return isDeploymentTargetBelow(chosen, floor) ? floor : normalizeVersion(chosen.trim());
    }

    /// A bare major ("12") as the major.minor Apple's plists carry ("12.0"). The app's own hint is
    /// written either way, and MinimumOSVersion is read by App Store validation -- not the place
    /// to find out which spellings its parser accepts.
    private static String normalizeVersion(String version) {
        return version.indexOf('.') < 0 ? version + ".0" : version;
    }

    /// Whether {@code target} names an iOS version below {@code floor}. A missing or unreadable
    /// value counts as below: the floor is then what the extension gets.
    private static boolean isDeploymentTargetBelow(String target, String floor) {
        if (target == null || target.trim().length() == 0) {
            return true;
        }
        String[] one = target.trim().split("\\.");
        String[] two = floor.split("\\.");
        for (int i = 0; i < Math.max(one.length, two.length); i++) {
            int a = i < one.length ? parseVersionPart(one[i]) : 0;
            int b = i < two.length ? parseVersionPart(two[i]) : 0;
            if (a != b) {
                return a < b;
            }
        }
        return false;
    }

    private static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part.trim());
        } catch (NumberFormatException notANumber) {
            return -1;
        }
    }

    private static final String PAYMENT_PASS_PROVISIONING =
            "com.apple.developer.payment-pass-provisioning";

    /// Whether a file's text holds a string, for the entitlement keys read out of a plist without
    /// parsing it. A missing or unreadable file holds nothing.
    private static boolean fileContains(File file, String needle) {
        if (file == null || !file.isFile()) {
            return false;
        }
        try {
            byte[] data = new byte[(int) file.length()];
            DataInputStream in = new DataInputStream(new FileInputStream(file));
            try {
                in.readFully(data);
            } finally {
                in.close();
            }
            return new String(data, StandardCharsets.UTF_8).contains(needle);
        } catch (IOException cannotRead) {
            return false;
        }
    }

    static File appExtensionInfoPlist(File extensionFolder) {
        String override = appExtensionBuildSetting(extensionFolder, "INFOPLIST_FILE");
        if (override == null) {
            // Confined like an overridden path, not trusted for sitting at the default name: a zip
            // may carry symlinks, so <folder>/Info.plist can still land outside the project.
            File byDefault = new File(extensionFolder, "Info.plist");
            return insideProjectDir(byDefault, extensionFolder.getParentFile()) ? byDefault : null;
        }
        return resolveInfoPlistPath(override, extensionFolder);
    }

    /// One INFOPLIST_FILE value as a file this build may write to, or null when it holds a setting
    /// that cannot be resolved here or lands outside the project directory.
    private static File resolveInfoPlistPath(String override, File extensionFolder) {
        return resolveInfoPlistPath(override, extensionFolder, null);
    }

    private static File resolveInfoPlistPath(String override, File extensionFolder,
            Map<String, String> settings) {
        String path = override;
        if (path.length() > 1 && path.startsWith("\"") && path.endsWith("\"")) {
            path = path.substring(1, path.length() - 1).trim();
        }
        path = resolveXcodeSettingsInPath(path, extensionFolder, settings);
        if (path == null || path.length() == 0) {
            return null;
        }
        File resolved = new File(path);
        if (!resolved.isAbsolute()) {
            resolved = new File(extensionFolder.getParentFile(), path);
        }
        return insideProjectDir(resolved, extensionFolder.getParentFile()) ? resolved : null;
    }

    /// Whether a path an uploaded archive chose is one this build is willing to write to.
    ///
    /// INFOPLIST_FILE arrives inside a customer's .ios.appext and the stamper WRITES to whatever
    /// it names, so an absolute path, a {@code ../../} traversal or a symlink planted in the
    /// archive would have this daemon rewriting a file outside the build -- another build's
    /// project, or anything else the account can write. The comparison is on canonical paths, so
    /// a symlink that leaves the project is judged by where it lands rather than by where it sits.
    ///
    /// Everything under the project directory is fair game: an extension may legitimately share a
    /// plist that sits beside its folder rather than inside it.
    static boolean insideProjectDir(File candidate, File projectDir) {
        if (candidate == null || projectDir == null) {
            return false;
        }
        try {
            String root = projectDir.getCanonicalPath();
            if (!root.endsWith(File.separator)) {
                root += File.separator;
            }
            return candidate.getCanonicalPath().startsWith(root);
        } catch (IOException cannotResolve) {
            // A path this process cannot even canonicalize is not one to write to.
            return false;
        }
    }

    /// The build settings Xcode gives every target a NON-EMPTY value for, which this builder does
    /// not model and must not treat as missing.
    ///
    /// Two things disqualify a name, and both have been learned here the hard way. A setting the
    /// PROJECT defines rather than Xcode -- MARKETING_VERSION, CURRENT_PROJECT_VERSION,
    /// DEVELOPMENT_TEAM -- expands to nothing in a project that never sets it. And a setting Xcode
    /// always defines but leaves EMPTY does the same: EXECUTABLE_PREFIX and EXECUTABLE_SUFFIX are
    /// both blank for an extension's Mach-O executable. Either way the identifier ships as
    /// "com.example.app." -- the malformed identifier this guard exists to prevent, waved through
    /// by the guard itself.
    ///
    /// A third disqualifies a name: a value that cannot be part of a bundle identifier at all.
    /// SRCROOT, PROJECT_DIR, BUILT_PRODUCTS_DIR and TARGET_BUILD_DIR expand to filesystem paths
    /// full of slashes, and ARCHS to a space-separated list -- an identifier built from one is
    /// invalid however faithfully Xcode expands it, so preserving the expression only ships the
    /// invalid identifier instead of the valid derived one.
    ///
    /// So membership is "Xcode always supplies this, it is never empty, and it can be part of an
    /// identifier". Anything else is dropped, and the derived default -- which is always valid --
    /// stands instead.
    ///
    /// Not a complete list of Xcode's settings, and it does not need to be: only the ones that
    /// plausibly appear in a bundle identifier.
    private static final java.util.Set<String> XCODE_PROVIDED_SETTINGS =
            new java.util.HashSet<String>(java.util.Arrays.asList(
                    "EXECUTABLE_NAME", "PRODUCT_NAME", "PRODUCT_MODULE_NAME", "TARGET_NAME",
                    "PROJECT_NAME", "CONFIGURATION", "SDK_NAME", "SDK_VERSION", "PLATFORM_NAME",
                    "CURRENT_ARCH", "NATIVE_ARCH", "WRAPPER_EXTENSION", "WRAPPER_NAME"));

    /// Whether every reference still left in {@code value} names a setting Xcode supplies itself.
    ///
    /// @param settings what this build could already expand, so only the leftovers are judged
    static boolean referencesOnlyXcodeSettings(String value, Map<String, String> settings) {
        if (value == null) {
            return false;
        }
        String remaining = stripResolved(value, settings);
        java.util.regex.Matcher m = BUILD_SETTING_REFERENCE.matcher(remaining);
        boolean any = false;
        while (m.find()) {
            any = true;
            String reference = m.group();
            // $(NAME) / ${NAME} / $(NAME:modifier)
            String name = reference.substring(2, reference.length() - 1);
            int modifier = name.indexOf(':');
            if (modifier >= 0) {
                name = name.substring(0, modifier);
            }
            if (!XCODE_PROVIDED_SETTINGS.contains(name)) {
                return false;
            }
        }
        return any;
    }

    /// Whether {@code governingKey}'s condition reaches every build {@code candidateKey}'s does.
    ///
    /// Not the same question as which is more specific. Specificity orders two settings where
    /// both apply; coverage asks whether one is redundant beside the other. [sdk=iphoneos*] and
    /// [config=Release] are orthogonal -- a device Release satisfies both, and neither contains
    /// the other -- so an explicit identifier qualified by one must not silence a plist
    /// identifier qualified by the other: the builds in the difference get neither.
    ///
    /// An unqualified governing key covers everything, which is what makes an archive's plain
    /// PRODUCT_BUNDLE_IDENTIFIER the last word on the target.
    static boolean conditionCovers(String governingKey, String candidateKey) {
        Map<String, String> governing = conditionsOf(governingKey);
        Map<String, String> candidate = conditionsOf(candidateKey);
        for (Map.Entry<String, String> constraint : governing.entrySet()) {
            String narrower = candidate.get(constraint.getKey());
            // A dimension the governing key constrains and the candidate does not is exactly the
            // orthogonal case: the candidate also describes builds outside it.
            if (narrower == null || !matchesCondition(constraint.getValue(), narrower)) {
                return false;
            }
        }
        return true;
    }

    /// The bracketed conditions of a settings key, as name to value.
    private static Map<String, String> conditionsOf(String key) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        int open = key == null ? -1 : key.indexOf('[');
        if (open < 0) {
            return out;
        }
        for (String condition : key.substring(open).split("[\\[\\],]")) {
            int equals = condition.indexOf('=');
            if (equals > 0) {
                out.put(condition.substring(0, equals).trim(),
                        condition.substring(equals + 1).trim());
            }
        }
        return out;
    }

    /// Whether two TARGETED_DEVICE_FAMILY values name the same families.
    ///
    /// Compared as sets rather than as text: "1, 2" and "1,2" are the same declaration, and
    /// judging them unequal materialized a reference that did not need clamping -- freezing this
    /// archive's answer into a setting other configurations read.
    static boolean sameDeviceFamilies(String left, String right) {
        return deviceFamilySet(left).equals(deviceFamilySet(right));
    }

    private static java.util.Set<String> deviceFamilySet(String value) {
        java.util.Set<String> out = new java.util.TreeSet<String>();
        if (value != null) {
            for (String family : value.split(",")) {
                if (family.trim().length() > 0) {
                    out.add(family.trim());
                }
            }
        }
        return out;
    }

    /// The archive's TARGETED_DEVICE_FAMILY, narrowed to what the host app actually supports.
    ///
    /// An exported extension folder usually carries the universal "1,2" from the project it came
    /// from, and those settings are copied onto the target verbatim -- so the host-derived default
    /// computed for an iPhone-only app was set and then immediately overwritten, and the extension
    /// went back to claiming iPad support its container does not have. That is refused at upload.
    ///
    /// Narrower than the host is left alone: a widget deliberately limited to iPhone inside a
    /// universal app is the author's call. Only the families the host does not have are dropped,
    /// and an archive that shares none of them falls back to the host's rather than to nothing.
    static String narrowDeviceFamily(String declared, String hostFamily) {
        if (declared == null || declared.trim().length() == 0) {
            return hostFamily;
        }
        if (hostFamily == null || hostFamily.trim().length() == 0) {
            return declared;
        }
        List<String> allowed = new ArrayList<String>();
        for (String family : hostFamily.split(",")) {
            if (family.trim().length() > 0) {
                allowed.add(family.trim());
            }
        }
        StringBuilder kept = new StringBuilder();
        for (String family : declared.split(",")) {
            String one = family.trim();
            if (one.length() > 0 && allowed.contains(one)) {
                if (kept.length() > 0) {
                    kept.append(",");
                }
                kept.append(one);
            }
        }
        return kept.length() == 0 ? hostFamily : kept.toString();
    }

    /// The device families an extension embedded in THIS app may declare.
    ///
    /// The app target's own come from ios.project_type: the translator rewrites the template's
    /// TARGETED_DEVICE_FAMILY to "1" for iphone and "2" for anything else that is not "ios".
    /// Every extension here was pinned to "1,2" regardless, so an iPhone-only app shipped an
    /// extension claiming iPad support -- the project builds, and App Store validation refuses
    /// the upload for an embedded bundle whose device families its container does not have.
    ///
    /// This is the DEFAULT only. An archive that states its own TARGETED_DEVICE_FAMILY is applied
    /// over it further down, since an extension deliberately narrower than its app -- a widget on
    /// iPhone alone -- is the author's call to make.
    static String embeddedExtensionDeviceFamily(String projectType) {
        if (projectType == null || "ios".equalsIgnoreCase(projectType)) {
            return "1,2";
        }
        return "iphone".equalsIgnoreCase(projectType) ? "1" : "2";
    }

    /// Every identifier the archive's own Info.plists state, keyed by the
    /// PRODUCT_BUNDLE_IDENTIFIER setting that expresses it.
    ///
    /// Two different things decide two different identifiers and nothing reconciles them: the
    /// target's PRODUCT_BUNDLE_IDENTIFIER chooses the profile that signs the .appex and keys the
    /// export options, while a LITERAL in the plist is what the built bundle declares -- Xcode
    /// copies it through untouched. An archive whose plist says com.example.app.Custom and whose
    /// target was given the derived com.example.app.&lt;folder&gt; is therefore signed for one
    /// bundle and built as another, which codesign refuses and App Store validation refuses after
    /// it.
    ///
    /// Keyed rather than reduced to one value, because the plists are per-configuration and so
    /// are the identifiers in them: an archive with INFOPLIST_FILE[config=Debug] beside the base
    /// setting states one identity for Debug and another for Release, and answering with a single
    /// unqualified value wrote the Release archive's identifier into the Debug configuration too
    /// -- where a later Debug build off these sources processes a plist still declaring the other
    /// one, which is the same mismatch one configuration along. Each stays where it belongs, and
    /// {@link #winningSetting} picks the one THIS archive gets by the same rule Xcode uses.
    ///
    /// A reference is not recorded: it resolves against the target and so already agrees with it.
    /// Nor is an identifier outside the host's namespace, which is refused elsewhere with a
    /// message that says why -- adopting it here would move that refusal onto the target and lose
    /// the explanation. Nor is a padded one, which is not the identifier it reads as.
    static Map<String, String> appExtensionPlistIdentifiers(File extensionFolder,
            ArchiveContext context, String hostPackage) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        if (extensionFolder == null || hostPackage == null || hostPackage.length() == 0) {
            return out;
        }
        Map<String, Long> specificities = new LinkedHashMap<String, Long>();
        for (Map.Entry<String, File> candidate
                : appExtensionInfoPlists(extensionFolder, context).entrySet()) {
            File file = candidate.getValue();
            if (file == null || !file.isFile()) {
                continue;
            }
            String declared = plistRootString(readPlistTextQuietly(file), "CFBundleIdentifier");
            if (declared == null || declared.length() == 0 || !declared.equals(declared.trim())
                    || declared.indexOf('$') >= 0
                    || !declared.startsWith(hostPackage + ".")) {
                continue;
            }
            String qualifier = conditionOf(candidate.getKey());
            String key = "PRODUCT_BUNDLE_IDENTIFIER" + qualifier;
            // Two candidates land on the same setting key only when one of them is the default
            // Info.plist, which appExtensionInfoPlists does not emit alongside an explicit base
            // setting -- so this comparison is a guard, not the fix. The fix is that each
            // identifier is recorded under the setting that states it: [sdk=iphoneos*] and
            // [sdk=iphoneos14.4] are different keys, both kept, and winningSetting chooses
            // between them by specificity the way Xcode does. Reducing them to one answer here
            // was what made the result depend on the order Properties happened to iterate in.
            long specificity = qualifier.length() == 0 ? 0
                    : conditionSpecificity(stripValueFromCandidateKey(candidate.getKey()));
            Long previous = specificities.get(key);
            if (previous == null || specificity >= previous.longValue()) {
                specificities.put(key, Long.valueOf(specificity));
                out.put(key, declared);
            }
        }
        return out;
    }

    /// The bracketed condition of a plist candidate's key, or "" when it has none.
    private static String conditionOf(String candidateKey) {
        String key = stripValueFromCandidateKey(candidateKey);
        int open = key.indexOf('[');
        return open < 0 ? "" : key.substring(open);
    }

    /// A candidate label without the " = value" these labels carry for the log.
    private static String stripValueFromCandidateKey(String candidateKey) {
        if (candidateKey == null) {
            return "";
        }
        int separator = candidateKey.indexOf(" = ");
        return separator < 0 ? candidateKey : candidateKey.substring(0, separator);
    }

    /// The plist's text, or "" when it cannot be read as one. The callers here are asking what an
    /// archive says about itself; a file they cannot read says nothing.
    private static String readPlistTextQuietly(File plist) {
        try {
            PlistText text = readPlistText(plist);
            return text == null || text.text == null ? "" : text.text;
        } catch (IOException cannotRead) {
            return "";
        }
    }

    /// One string value among the ROOT dict's direct children, exactly as written.
    ///
    /// Anchored to the top level for the same reason every other lookup here is: NSExtension and
    /// CFBundleURLTypes carry keys of their own, and a whole-file search finds whichever comes
    /// first rather than the bundle's own identity.
    static String plistRootString(String plist, String key) {
        if (plist == null) {
            return null;
        }
        int afterKey = topLevelKeyEnd(plist, key);
        if (afterKey < 0) {
            return null;
        }
        int element = nextMarkupAt(plist, afterKey);
        if (element < 0 || !"string".equals(WatchNativeBuilder.tagAt(plist, element))) {
            return null;
        }
        int openEnd = plist.indexOf('>', element);
        if (openEnd < 0 || plist.charAt(openEnd - 1) == '/') {
            return null;
        }
        int valueEnd = WatchNativeBuilder.closeOfElement(plist, openEnd + 1, "</string>");
        return valueEnd < 0 ? null
                : WatchNativeBuilder.plistStringContentExact(plist.substring(openEnd + 1, valueEnd));
    }

    /// Drops a PRODUCT_BUNDLE_IDENTIFIER the archive declares as nothing, and says so.
    ///
    /// Empty means different things to different settings, and this is the end of the scale where
    /// it means nothing at all: a bundle must have an identifier, so an archive setting the key
    /// to blank cannot be stating a choice the way an empty CODE_SIGN_ENTITLEMENTS does. It read
    /// as "not declared" everywhere that asks a question -- preflight validated the derived
    /// default -- and then the properties were copied onto the target verbatim, which put the
    /// blank back and built an extension with no identifier at all.
    ///
    /// @return a note per dropped key, for the log
    static List<String> dropBlankBundleIdentifiers(Map<String, String> settings) {
        return dropBlankBundleIdentifiers(settings, null);
    }

    /// @param resolution the settings a $(...) in an identifier expands against, Xcode's own
    /// built-ins included, or null to judge only literal blanks. Built PER KEY by the caller
    /// where a qualifier is involved: see the File overload.
    static List<String> dropBlankBundleIdentifiers(Map<String, String> settings,
            Map<String, String> resolution) {
        return dropBlankBundleIdentifiers(settings, resolution, null, null, null);
    }

    /// @param extensionFolder and {@code archiveContext} let each qualified identifier be
    /// resolved in ITS OWN context. Judged against one map flattened for the archive,
    /// PRODUCT_BUNDLE_IDENTIFIER[config=Debug] = $(EXTENSION_ID) with only
    /// EXTENSION_ID[config=Debug] defined resolved to nothing and was deleted -- taking the Debug
    /// configuration's identifier with it, while the plist reconciliation had already stood down
    /// for it. The device-family handling beside this does the same thing for the same reason.
    static List<String> dropBlankBundleIdentifiers(Map<String, String> settings,
            File extensionFolder, ArchiveContext archiveContext) {
        return dropBlankBundleIdentifiers(settings, extensionFolder, archiveContext, null);
    }

    /// @param hostPackage the app's own identifier, so an expression whose namespace this build
    /// cannot establish is not kept on the strength of its references being Xcode's
    static List<String> dropBlankBundleIdentifiers(Map<String, String> settings,
            File extensionFolder, ArchiveContext archiveContext, String hostPackage) {
        return dropBlankBundleIdentifiers(settings, null, extensionFolder, archiveContext,
                hostPackage);
    }

    private static List<String> dropBlankBundleIdentifiers(Map<String, String> settings,
            Map<String, String> resolution, File extensionFolder, ArchiveContext archiveContext,
            String hostPackage) {
        List<String> notes = new ArrayList<String>();
        if (settings == null) {
            return notes;
        }
        for (String key : new ArrayList<String>(settings.keySet())) {
            if (!"PRODUCT_BUNDLE_IDENTIFIER".equals(key)
                    && !isQualified(key, "PRODUCT_BUNDLE_IDENTIFIER")) {
                continue;
            }
            String value = settings.get(key);
            if (value == null || value.trim().length() == 0) {
                settings.remove(key);
                notes.add(key + " was declared with no value, and an extension cannot be built "
                        + "without a bundle identifier, so it was dropped");
                continue;
            }
            Map<String, String> perKey = resolution;
            if (perKey == null && extensionFolder != null) {
                perKey = extensionSettingsWithBuiltIns(extensionFolder, settings,
                        contextForCondition(key, archiveContext));
            }
            if (perKey != null && value.indexOf('$') >= 0) {
                // An expression that comes to nothing is the same thing one step later: Xcode
                // expands an undefined $(EXTENSION_ID) to the empty string, and the .appex ships
                // with no identifier at all. Dropped so the derived default stands, rather than
                // merged onto the target to be emptied there.
                // null means "a reference is left over", which for an identifier is the same
                // outcome: Xcode expands what it cannot find to nothing. Either way the .appex
                // would ship with no identifier, so the setting is dropped and the derived
                // default stands -- gentler than refusing the build over a setting that may be
                // perfectly valid on the machine that wrote it, and it still builds something
                // signable.
                String resolved = resolveSettingsFully(value.trim(), perKey);
                // Unless what is left over is Xcode's OWN to supply. resolveSettingsFully says
                // null for any reference it could not expand, and this build models only some of
                // the settings Xcode defines -- so com.example.$(EXECUTABLE_NAME) came back null
                // and a perfectly good identifier was deleted, dropping that configuration to the
                // base one while its plist still declares the other. Only a name nothing will
                // define is the empty identifier this guard is for.
                if (resolved == null && referencesOnlyXcodeSettings(value.trim(), perKey)
                        && startsUnderHost(value.trim(), perKey, hostPackage)) {
                    continue;
                }
                if (resolved == null || resolved.trim().length() == 0) {
                    settings.remove(key);
                    notes.add(key + " = " + value.trim() + " is a reference this build cannot "
                            + "resolve, and Xcode expands what it cannot find to nothing; an "
                            + "extension cannot be built without a bundle identifier, so it was "
                            + "dropped");
                }
            }
        }
        return notes;
    }

    /// The identifier this extension's archive declares, or the one derived from the app.
    ///
    /// Named rather than inlined because four call sites have to agree on it: an archive that
    /// overrides PRODUCT_BUNDLE_IDENTIFIER decides the export-options key, the profile that can
    /// sign it, the plist that is stamped and the namespace refusal, and one of them reading the
    /// derived default instead pairs the target with a bundle the archive does not contain.
    static String appExtensionBundleId(File extensionFolder, String defaultBundleId) {
        return appExtensionBundleId(extensionFolder, defaultBundleId, null, null);
    }

    /// @param context and {@code hostPackage} let the archive's own Info.plist have its say when
    /// buildSettings.properties does not: a literal identifier in there is what the built bundle
    /// declares, so the target has to be configured as that or the two disagree.
    static String appExtensionBundleId(File extensionFolder, String defaultBundleId,
            ArchiveContext context, String hostPackage) {
        // The declared identifier that governs THIS context, not only the plain key: an archive
        // declaring just PRODUCT_BUNDLE_IDENTIFIER[config=Debug] read as declaring nothing, so
        // the Debug plist's literal was compared against itself and preserved while Xcode builds
        // that configuration under the qualified setting -- one bundle built, another signed.
        Map<String, String> declared = appExtensionBuildSettings(extensionFolder);
        String override = winningSetting(declared, "PRODUCT_BUNDLE_IDENTIFIER", context);
        if (override != null && override.trim().length() > 0) {
            return override.trim();
        }
        // The one THIS archive gets, by the same rule Xcode applies to any conditional setting.
        String fromPlist = winningSetting(
                appExtensionPlistIdentifiers(extensionFolder, context, hostPackage),
                "PRODUCT_BUNDLE_IDENTIFIER", context);
        return fromPlist != null ? fromPlist : defaultBundleId;
    }

    /// One build setting as the extension's own buildSettings.properties overrides it, or null
    /// when the archive carries no such override.
    ///
    /// Read from the file rather than from the settings map because the callers run before the
    /// properties are folded into it -- and a setting that decides which files the build touches
    /// has to be known before we touch them.
    static String appExtensionBuildSetting(File extensionFolder, String key) {
        String value = appExtensionBuildSettings(extensionFolder).get(key);
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        return value.trim();
    }

    /// Every build setting the archive overrides, as its buildSettings.properties declares them.
    ///
    /// These are not advisory: further down each one is written into the extension target's build
    /// configurations, so they decide what a {@code $(...)} reference in the extension's own
    /// Info.plist resolves to when Xcode processes it.
    static Map<String, String> appExtensionBuildSettings(File extensionFolder) {
        Map<String, String> out = new LinkedHashMap<String, String>();
        File settings = new File(extensionFolder, "buildSettings.properties");
        if (!settings.isFile()) {
            return out;
        }
        Properties props = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(settings);
            props.load(fis);
        } catch (IOException ex) {
            return out;
        } finally {
            if (fis != null) {
                try { fis.close(); } catch (Throwable t) {}
            }
        }
        for (Object key : props.keySet()) {
            if (key instanceof String) {
                String value = props.getProperty((String) key);
                // Trimmed, because Properties keeps trailing whitespace and Xcode does not:
                // xcodebuild -showBuildSettings reports a padded value without its padding, and
                // the parser for that output trims too. Untrimmed, preflight judged
                // "com.example.app.Ext" while the target was handed "com.example.app.Ext " --
                // an identifier no profile matches and no bundle may carry, arrived at by two
                // readers of the same file disagreeing about what it says.
                out.put((String) key, value == null ? null : value.trim());
            }
        }
        return out;
    }

    /// Substitutes the build settings whose values this build already knows, so that the ordinary
    /// Xcode spelling of an extension's plist path resolves.
    ///
    /// {@code $(SRCROOT)/$(TARGET_NAME)/Info.plist} is what an Xcode project writes for the file
    /// that sits in the extension's own folder, and every part of it is known here: SRCROOT and
    /// PROJECT_DIR are the project directory, which is where the extension folders are extracted,
    /// and TARGET_NAME is the folder's name, because that is the name the target is created with.
    /// PRODUCT_NAME follows TARGET_NAME unless the archive overrode it with a literal.
    ///
    /// Both spellings, {@code $(NAME)} and {@code ${NAME}}. Anything still holding a {@code $}
    /// afterwards is a setting this build cannot evaluate -- CONFIGURATION, an SDK-dependent
    /// value -- and null says so, because the alternative is editing whichever file the
    /// half-resolved path happens to name.
    private static String resolveXcodeSettingsInPath(String path, File extensionFolder) {
        return resolveXcodeSettingsInPath(path, extensionFolder, null);
    }

    /// @param settings the target's settings when they are already gathered, since the properties
    /// file they came from is deleted once it is loaded
    private static String resolveXcodeSettingsInPath(String path, File extensionFolder,
            Map<String, String> settings) {
        String targetName = extensionFolder.getName();
        String productName = settings != null && settings.get("PRODUCT_NAME") != null
                ? settings.get("PRODUCT_NAME").trim()
                : appExtensionBuildSetting(extensionFolder, "PRODUCT_NAME");
        if (productName == null || productName.indexOf('$') >= 0) {
            productName = targetName;
        }
        File projectDir = extensionFolder.getParentFile();
        String projectPath = projectDir == null ? "." : projectDir.getAbsolutePath();
        String out = path;
        // The archive's own settings first, and to a fixed point: INFOPLIST_FILE may be written as
        // $(PLIST_DIR)/Info.plist with PLIST_DIR defined two lines above it in the same properties
        // file. Both are copied onto the target, so Xcode resolves that path -- and expanding only
        // the four names below called it unresolvable and left the plist Xcode actually builds
        // unstamped.
        Map<String, String> declared = settings != null ? settings
                : appExtensionBuildSettings(extensionFolder);
        for (int pass = 0; pass < MAX_SETTING_EXPANSIONS
                && BUILD_SETTING_REFERENCE.matcher(out).find(); pass++) {
            String before = out;
            for (Map.Entry<String, String> setting : declared.entrySet()) {
                if (setting.getValue() != null) {
                    out = replaceBuildSetting(out, setting.getKey(), setting.getValue().trim());
                }
            }
            if (out.equals(before)) {
                break;
            }
        }
        out = replaceBuildSetting(out, "SRCROOT", projectPath);
        out = replaceBuildSetting(out, "PROJECT_DIR", projectPath);
        out = replaceBuildSetting(out, "TARGET_NAME", targetName);
        out = replaceBuildSetting(out, "PRODUCT_NAME", productName);
        return out.indexOf('$') >= 0 ? null : out;
    }

    /// The same as {@link #resolveSettingsInValue}, but null when a reference is left over.
    ///
    /// resolveSettingsInValue deletes what it cannot expand, which is right when the question is
    /// "what will this be on the device" -- Xcode deletes it too. It is wrong when the answer is
    /// about to be RECORDED: com.example.app.$(SOMETHING_UNKNOWN) came out as "com.example.app.",
    /// and that partial string went into the export-options dictionary as the key for a bundle
    /// the archive does not contain, so a manual export could not pair the extension with its
    /// profile. A value this build cannot resolve completely is better left alone than recorded
    /// as a truncation of itself.
    static String resolveSettingsFully(String value, Map<String, String> settings) {
        if (value == null) {
            return null;
        }
        String resolved = resolveSettingsInValue(value, settings);
        return BUILD_SETTING_REFERENCE.matcher(value).find()
                && BUILD_SETTING_REFERENCE.matcher(stripResolved(value, settings)).find()
                ? null : resolved;
    }

    /// The value with every reference this build CAN expand already expanded, so what remains is
    /// exactly what it cannot.
    private static String stripResolved(String value, Map<String, String> settings) {
        String out = value;
        if (settings != null) {
            for (int pass = 0; pass < MAX_SETTING_EXPANSIONS
                    && BUILD_SETTING_REFERENCE.matcher(out).find(); pass++) {
                String before = out;
                for (Map.Entry<String, String> setting : settings.entrySet()) {
                    if (setting.getValue() != null) {
                        out = replaceBuildSetting(out, setting.getKey(), setting.getValue());
                    }
                }
                if (out.equals(before)) {
                    break;
                }
            }
        }
        return out;
    }

    /// A plist value with the archive's own build settings substituted, so a {@code $(...)}
    /// reference can be compared with the version it will actually resolve to on the device.
    ///
    /// A reference to a setting the archive does not define resolves to the empty string, which is
    /// what Xcode does with it too: the extension target this build generates carries only the
    /// settings written here, and no version among them.
    private static String resolveSettingsInValue(String value, Map<String, String> archiveSettings) {
        String out = value;
        if (archiveSettings != null) {
            // To a fixed point, not one pass. A setting's value may name another setting and Xcode
            // keeps expanding until none is left, while one traversal of the map expands nested
            // references only when the iteration order happens to be the dependency order -- and
            // for Properties that is hash order. MARKETING_VERSION = 5.4$(VERSION_SUFFIX) visited
            // before VERSION_SUFFIX left the inner reference behind, the strip below deleted it as
            // though nothing defined it, and a version the device resolves to 5.41 was judged to
            // be the app's own 5.4 and left standing.
            for (int pass = 0; pass < MAX_SETTING_EXPANSIONS
                    && BUILD_SETTING_REFERENCE.matcher(out).find(); pass++) {
                String before = out;
                for (Map.Entry<String, String> setting : archiveSettings.entrySet()) {
                    out = replaceBuildSetting(out, setting.getKey(),
                            setting.getValue() == null ? "" : setting.getValue());
                }
                if (out.equals(before)) {
                    // Nothing left that this archive defines; the strip below handles the rest.
                    break;
                }
            }
        }
        // What survives names a setting the archive does not define, or sits in a cycle that never
        // settles. Xcode resolves those to nothing, and so does this.
        // Not trimmed: the properties file's own trailing whitespace is written into the Xcode
        // setting verbatim, so MARKETING_VERSION = "5.4 " really does expand to "5.4 ".
        return BUILD_SETTING_REFERENCE.matcher(out).replaceAll("");
    }

    /// A build-setting reference in either spelling Xcode accepts.
    /// A build-setting reference, modifiers included: $(NAME), ${NAME} and
    /// ${NAME:rfc1034identifier}. Without the modifier form an identifier written that way looked
    /// like plain text -- "fully resolved" -- and the literal expression was recorded as a bundle
    /// id while Xcode archived the expansion of it.
    private static final Pattern BUILD_SETTING_REFERENCE =
            Pattern.compile("\\$[({][A-Za-z0-9_]+(?::[A-Za-z0-9_]+)*[)}]");

    /// Expansion passes before a value is called unresolvable. Settings nest a level or two in
    /// practice; the cap is what stops A = $(B), B = $(A) from spinning.
    private static final int MAX_SETTING_EXPANSIONS = 16;

    /// One build setting, in either of the two spellings Xcode accepts for a reference.
    private static String replaceBuildSetting(String path, String name, String value) {
        String out = path.replace("$(" + name + ")", value).replace("${" + name + "}", value);
        return applyModifiers(out, name, value);
    }

    /// Expands $(NAME:modifier) for the modifiers Xcode defines and this can reproduce.
    ///
    /// rfc1034identifier is the one that matters here: it is how an extension's identifier is
    /// ordinarily written from a product name, ${PRODUCT_NAME:rfc1034identifier}, and Xcode
    /// archives the expansion. A modifier this does not know is left in place, which keeps the
    /// value "not fully resolved" rather than recording an expression as an identifier.
    private static String applyModifiers(String value, String name, String settingValue) {
        Matcher reference = Pattern.compile("\\$[({]" + Pattern.quote(name)
                + "((?::[A-Za-z0-9_]+)+)[)}]").matcher(value);
        StringBuffer out = new StringBuffer();
        while (reference.find()) {
            String expanded = settingValue;
            boolean known = true;
            for (String modifier : reference.group(1).split(":")) {
                if (modifier.length() == 0) {
                    continue;
                }
                if ("lower".equalsIgnoreCase(modifier)) {
                    expanded = expanded.toLowerCase(java.util.Locale.ENGLISH);
                } else if ("upper".equalsIgnoreCase(modifier)) {
                    expanded = expanded.toUpperCase(java.util.Locale.ENGLISH);
                } else if ("rfc1034identifier".equalsIgnoreCase(modifier)) {
                    // Anything outside a host-name label becomes a hyphen, which is what Xcode
                    // does to make a product name usable in a bundle identifier.
                    expanded = expanded.replaceAll("[^A-Za-z0-9.-]", "-");
                } else if ("identifier".equalsIgnoreCase(modifier)
                        || "c99extidentifier".equalsIgnoreCase(modifier)) {
                    expanded = expanded.replaceAll("[^A-Za-z0-9_]", "_");
                } else {
                    known = false;
                    break;
                }
            }
            reference.appendReplacement(out, known
                    ? Matcher.quoteReplacement(expanded)
                    : Matcher.quoteReplacement(reference.group()));
        }
        reference.appendTail(out);
        return out.toString();
    }

    /**
     * Parses the pbxproj-shaped block of build settings an app extension target is
     * seeded with -- one {@code KEY = VALUE;} per line -- into the map that is written
     * back out as Ruby string literals in the project fixup script.
     *
     * Two details the value has to lose, both of which failed silently here. The
     * trailing semicolon: keeping it (the old code sliced the last character INSTEAD
     * of dropping it, so every value became ";") left CLANG_ENABLE_MODULES off, which
     * drops -fmodules, which drops clang's autolinking, which is why an extension that
     * imports UIKit reached ld with Foundation alone and died on
     * _OBJC_CLASS_$_UIView. And the quotes Xcode wraps a non-identifier value in:
     * re-emitted inside the Ruby literal those become ""gnu++14"", a syntax error that
     * takes the whole fixup script down with it.
     */
    static Map<String, String> parseXcodeBuildSettings(String buildSettingsStr) {
        Map<String, String> buildSettingsMap = new LinkedHashMap<String, String>();
        for (String line : buildSettingsStr.split("\n")) {
            if (line.trim().isEmpty()) {
                continue;
            }
            int equals = line.indexOf("=");
            if (equals < 0) {
                continue;
            }
            String key = line.substring(0, equals).trim();
            String val = line.substring(equals + 1).trim();
            if (val.endsWith(";")) {
                val = val.substring(0, val.length() - 1).trim();
            }
            if (val.length() > 1 && val.startsWith("\"") && val.endsWith("\"")) {
                val = val.substring(1, val.length() - 1);
            }
            buildSettingsMap.put(key, val);
        }
        return buildSettingsMap;
    }

    static void appendFilesToXcodeProjGroup(StringBuilder sb, File dir, String serviceGroupVarName, String serviceTargetVarName, File baseDir) {

        String basePath = baseDir.getAbsolutePath();
        if (!basePath.endsWith("/")) {
            basePath += "/";
        }
        int basePathLen = basePath.length();
        for (File f : dir.listFiles()) {
            if (f.isDirectory() && f.getName().endsWith(".xcassets")) {
                // Asset catalogs are directory packages. Adding their contents one file at a
                // time flattens every Contents.json into the extension bundle and makes Xcode
                // fail with "Multiple commands produce .../Contents.json". Add the catalog
                // itself so Xcode compiles it with actool.
                sb.append("fileref = ").append(serviceGroupVarName).append(".new_file(").append("'").append(f.getAbsolutePath().substring(basePathLen)).append("')\n");
                sb.append(serviceTargetVarName).append(".add_resources([fileref])\n");
            } else if (f.isFile()) {
                sb.append("fileref = ").append(serviceGroupVarName).append(".new_file(").append("'").append(f.getAbsolutePath().substring(basePathLen)).append("')\n");
                if (f.getName().endsWith(".m") || f.getName().endsWith(".swift")) {
                    sb.append(serviceTargetVarName).append(".add_file_references([fileref])\n");
                } else if (!f.getName().endsWith("Info.plist") && !f.getName().endsWith(".entitlements")
                        && !f.getName().endsWith(".h") && !f.getName().endsWith(".mobileprovision")){
                    sb.append(serviceTargetVarName).append(".add_resources([fileref])\n");
                }
            } else {
                appendFilesToXcodeProjGroup(sb, f, serviceGroupVarName, serviceTargetVarName, baseDir);
            }
        }
    }

    private void removeLinesContaining(File file, String... snippets) throws IOException {
        if (file == null || !file.exists() || snippets == null || snippets.length == 0) {
            return;
        }
        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(content.length());
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            while ((line = reader.readLine()) != null) {
                boolean remove = false;
                for (String snippet : snippets) {
                    if (snippet != null && !snippet.isEmpty() && line.contains(snippet)) {
                        remove = true;
                        break;
                    }
                }
                if (!remove) {
                    sb.append(line).append('\n');
                }
            }
        }
        createFile(file, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Recursively checks whether the given directory contains any {@code .swift} files.
     */
    private static boolean hasSwiftFiles(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return false;
        }
        for (File f : children) {
            if (f.isFile() && f.getName().endsWith(".swift")) {
                return true;
            }
            if (f.isDirectory() && hasSwiftFiles(f)) {
                return true;
            }
        }
        return false;
    }

    
    private String convertToJavaMethod(Class type) {
        if(type.isArray()) {
            type = type.getComponentType();
            if(Integer.class == type || Integer.TYPE == type) {
                return "nsDataToIntArray(";
            }
            if(Long.class == type || Long.TYPE == type) {
                return "nsDataToLongArray(";
            }
            if(Byte.class == type || Byte.TYPE == type) {
                return "nsDataToByteArr(";
            }
            if(Short.class == type || Short.TYPE == type) {
                return "nsDataToShortArray(";
            }
            if(Character.class == type || Character.TYPE == type) {
                return "nsDataToCharArray(";
            }
            if(Boolean.class == type || Boolean.TYPE == type) {
                return "nsDataToBooleanArray(";
            }
            if(Float.class == type || Float.TYPE == type) {
                return "nsDataToFloatArray(";
            }
            if(Double.class == type || Double.TYPE == type) {
                return "nsDataToDoubleArray(";
            }
        }
        if(String.class == type) {
            return "fromNSString(CN1_THREAD_GET_STATE_PASS_ARG ";
        }
        return "";
    }
    
    private String convertToClosing(Class type) {
        if(type.isArray()) {
            return ")";
        }
        if(String.class == type) {
            return ")";
        }
        return "";
    }
    
    private String convertToObjectiveCMethod(Class type) {
        if(type.isArray()) {
            return "arrayToData(";
        }
        if(String.class == type) {
            return "toNSString(CN1_THREAD_GET_STATE_PASS_ARG ";
        }
        return "";
    }
    
    private String getSimpleNameWithJavaLang(Class c) {
        if(c.isPrimitive()) {
            return c.getSimpleName();
        }
        if(c.isArray()) {
            return getSimpleNameWithJavaLang(c.getComponentType()) + "[]";
        }
        if(c.getClass().getName().startsWith("java.lang.")) {
            return c.getName();
        }
        return c.getSimpleName();
    }
    
    private String typeToXMLVMJavaName(Class type) {
        if(type.isArray()) {
            return getSimpleNameWithJavaLang(type.getComponentType()).replace('.', '_') + "_1ARRAY";
        }
        return getSimpleNameWithJavaLang(type).replace('.', '_');
    }
    private String typeToXMLVMName(Class type) {
        if(type.getName().equals("com.codename1.ui.PeerComponent")) {
            return "JAVA_LONG";
        }
        if(Integer.class == type || Integer.TYPE == type) {
            return "JAVA_INT";
        }
        if(Long.class == type || Long.TYPE == type) {
            return "JAVA_LONG";
        }
        if(Byte.class == type || Byte.TYPE == type) {
            return "JAVA_BYTE";
        }
        if(Short.class == type || Short.TYPE == type) {
            return "JAVA_SHORT";
        }
        if(Character.class == type || Character.TYPE == type) {
            return "JAVA_CHAR";
        }
        if(Boolean.class == type || Boolean.TYPE == type) {
            return "JAVA_BOOLEAN";
        }
        if(Void.class == type || Void.TYPE == type) {
            return "void";
        }
        if(Float.class == type || Float.TYPE == type) {
            return "JAVA_FLOAT";
        }
        if(Double.class == type || Double.TYPE == type) {
            return "JAVA_DOUBLE";
        }
        // array/string
        return "JAVA_OBJECT";
    }
    
    protected String generatePeerComponentCreationCode(String methodCallString) {
        return "PeerComponent.create(new long[] {" + methodCallString + "})";
    }


    @Override
    protected String convertPeerComponentToNative(String param) {
        return "((long[])" + param + ".getNativePeer())[0]";
    }
    
    @Override
    protected String getImplSuffix() {
        return "ImplCodenameOne";
    }

    protected boolean deriveGlobalInstrumentClasspath() {
        return true;
    }
    
    private GoogleServicePlist googleServicePlist;

    private class GoogleServicePlist {
        String reverseClientId;
        String clientId;
        boolean useSignIn;
        
    }

    private class Stopwatch {
        private long startTime;
        private long lastSplitTime;
        private final StringBuilder summary = new StringBuilder();

        public Stopwatch() {
            startTime = System.currentTimeMillis();
            lastSplitTime = startTime;
            summary.append("Build Time Statistics:\n");
            summary.append("----------------------\n");
        }

        public void split(String stepName) {
            long now = System.currentTimeMillis();
            long duration = now - lastSplitTime;
            String message = String.format("%-40s : %d ms", stepName, duration);
            log(message);
            summary.append(message).append("\n");
            lastSplitTime = now;
        }

        public void stop() {
            long now = System.currentTimeMillis();
            long totalDuration = now - startTime;
            summary.append("----------------------\n");
            summary.append(String.format("%-40s : %d ms", "Total Time", totalDuration));
            log(summary.toString());
            String statsFile = System.getenv("CN1_BUILD_STATS_FILE");
            if (statsFile != null && statsFile.length() > 0) {
                try {
                    File f = new File(statsFile);
                    if (f.getParentFile() != null) {
                        f.getParentFile().mkdirs();
                    }
                    try (FileOutputStream fos = new FileOutputStream(f)) {
                        fos.write(summary.toString().getBytes("UTF-8"));
                    }
                } catch (Exception ex) {
                    log("Failed to write build stats to file " + statsFile + ": " + ex.getMessage());
                }
            }
        }
    }

    private boolean hasAppExtensionArchives(File sourceDirectory) {
        File[] children = sourceDirectory == null ? null : sourceDirectory.listFiles();
        if (children == null) {
            return false;
        }
        for (File f : children) {
            if (f.getName().endsWith(".ios.appext")) {
                return true;
            }
        }
        return false;
    }

    private static final String[][] WALLET_INJECTION_HINTS = {
        {"ios.wallet.nonuiImportsInject", IOSWalletExtensionBuilder.MARKER_NONUI_IMPORTS},
        {"ios.wallet.statusInject", IOSWalletExtensionBuilder.MARKER_STATUS},
        {"ios.wallet.passEntriesInject", IOSWalletExtensionBuilder.MARKER_PASS_ENTRIES},
        {"ios.wallet.remotePassEntriesInject", IOSWalletExtensionBuilder.MARKER_REMOTE_PASS_ENTRIES},
        {"ios.wallet.generateRequestInject", IOSWalletExtensionBuilder.MARKER_GENERATE_REQUEST},
        {"ios.wallet.generateResponseInject", IOSWalletExtensionBuilder.MARKER_GENERATE_RESPONSE},
        {"ios.wallet.uiImportsInject", IOSWalletExtensionBuilder.MARKER_UI_IMPORTS},
        {"ios.wallet.uiViewDidLoadInject", IOSWalletExtensionBuilder.MARKER_UI_VIEWDIDLOAD},
        {"ios.wallet.uiAuthRequestInject", IOSWalletExtensionBuilder.MARKER_UI_AUTH_REQUEST},
        {"ios.wallet.uiAuthResponseInject", IOSWalletExtensionBuilder.MARKER_UI_AUTH_RESPONSE},
    };

    /**
     * Generates the Apple Wallet issuer-provisioning extension folders under dist/
     * and appends the ruby that wires them into the generated Xcode project as
     * app_extension targets. Driven by the ios.wallet.* build hints.
     */
    /**
     * Writes the MatterAddDeviceExtension folder under dist/ and appends the ruby that wires it
     * into the generated Xcode project.
     *
     * <p>Apple requires this extension before an app may add a Matter accessory: the add-device
     * sheet runs outside the app and talks to it. An app without one gets a runtime failure from
     * the first commissioning call and nothing at build time to warn it -- which is precisely why
     * generating it is the builder's job rather than something a Codename One developer is asked
     * to hand-write in Xcode.</p>
     *
     * <p>Only reached when the scanner saw {@code com.codename1.home.commissioning}.</p>
     */
    private void appendMatterExtensionTarget(StringBuilder sb, BuildRequest request, File distDir)
            throws IOException, BuildException {
        String name = MatterExtensionBuilder.EXTENSION_NAME;
        String displayName = request.getArg("ios.home.commissioning.displayName",
                request.getDisplayName() == null ? name : request.getDisplayName());
        // The host's own versions, through the helpers the watch builder uses
        // for the same rule: an embedded extension whose marketing or build
        // version differs from its containing app fails archive validation.
        String extShort = embeddedExtensionShortVersion(request);
        String extBundle = embeddedExtensionBundleVersion(request);
        // The hint is an override, not the only way in: an app whose
        // setCommissionToThisApp(true) the scanner saw needs no hint, and one
        // that reaches the API through reflection has no other way to say so.
        boolean ownFabric = matterOwnFabric(request);
        if (ownFabric) {
            log("Smart home: commissioning onto this app's own Matter fabric"
                    + " -- the extension ships a Matter controller");
        }
        IOSWalletExtensionBuilder.writeFileMap(
                MatterExtensionBuilder.buildFileMap(request.getPackageName(),
                        matterAppGroup, displayName, extShort, extBundle,
                        ownFabric, request.getArg(
                                "ios.home.commissioning.vendorId", "0xFFF1")),
                new File(distDir, name));
        // The app-side Swift shim, into <MainClass>-src, exactly where the
        // surfaces glue goes and for the same reason: this method runs while
        // the schemes ruby is still being assembled, and that script is what
        // sweeps *.swift there into the APP target's source phase. Staged any
        // later -- after the script has run, or after its post-dependency
        // re-run -- the file is on disk and in no target, and
        // NSClassFromString finds nothing at runtime.
        SmartHomeInjector.injectIosCommissioningShim(this,
                new File(distDir, request.getMainClass() + "-src"));
        log("Adding Matter add-device extension target " + name
                + " (app group " + matterAppGroup + ")");

        Map<String, String> buildSettingsMap = new LinkedHashMap<String, String>();
        buildSettingsMap.put("PRODUCT_BUNDLE_IDENTIFIER", request.getPackageName() + "." + name);
        buildSettingsMap.put("PRODUCT_NAME", "$(TARGET_NAME)");
        buildSettingsMap.put("INFOPLIST_FILE", name + "/Info.plist");
        buildSettingsMap.put("CODE_SIGN_ENTITLEMENTS", name + "/" + name + ".entitlements");
        buildSettingsMap.put("IPHONEOS_DEPLOYMENT_TARGET",
                MatterExtensionBuilder.deploymentTarget(ownFabric));
        buildSettingsMap.put("TARGETED_DEVICE_FAMILY",
                embeddedExtensionDeviceFamily(request.getArg("ios.project_type", "ios")));
        buildSettingsMap.put("LD_RUNPATH_SEARCH_PATHS",
                "$(inherited) @executable_path/Frameworks @executable_path/../../Frameworks");
        buildSettingsMap.put("SKIP_INSTALL", "YES");
        buildSettingsMap.put("CLANG_ENABLE_OBJC_ARC", "YES");
        buildSettingsMap.put("CLANG_ENABLE_MODULES", "YES");
        // The handler is Swift, because MatterSupport has no Objective-C interface. Naming the
        // version explicitly keeps the extension building when the app target's own Swift
        // settings differ or are absent entirely -- most Codename One apps have no Swift at all.
        buildSettingsMap.put("SWIFT_VERSION", request.getArg("ios.swiftVersion", "5.0"));
        buildSettingsMap.put("ALWAYS_EMBED_SWIFT_STANDARD_LIBRARIES", "YES");
        for (String key : request.getArgs()) {
            if (key.startsWith("ios.home.commissioning.buildSettings.")) {
                buildSettingsMap.put(
                        key.substring("ios.home.commissioning.buildSettings.".length()),
                        request.getArg(key, ""));
            }
        }
        // Guarded so re-running the script does not create a duplicate target; the build
        // re-executes fix_xcode_schemes.rb after dependency integration.
        sb.append("\nif xcproj.targets.find{|e| e.name=='" + name + "'}.nil?\n"
                + "service_target = xcproj.new_target(:app_extension, '" + name + "', :ios, '"
                + MatterExtensionBuilder.deploymentTarget(ownFabric) + "')\n"
                + "service_target.add_system_framework('MatterSupport')\n"
                // Matter is Apple's own CHIP stack, and it is what the
                // generated commissioning implementation drives. Linked
                // whether or not that implementation is live: the file
                // imports it either way so that uncommenting the
                // implementation is the only step, and an unused system
                // framework in an extension costs the app nothing at
                // runtime -- the extension only runs while the setup sheet
                // is on screen.
                + "service_target.add_system_framework('Matter')\n"
                + "service_group = xcproj.new_group('" + name + "')\n");
        appendFilesToXcodeProjGroup(sb, new File(distDir, name), "service_group", "service_target",
                distDir);
        sb.append("main_app_target = xcproj.targets.find{|e| e.name==main_class_name}\n"
                + "main_app_target.add_dependency(service_target)\n"
                + "fileref = xcproj.groups.find{|e| e.display_name=='Products'}.new_file('"
                + name + ".appex', \"BUILT_PRODUCTS_DIR\")\n"
                + "embed_phase = main_app_target.copy_files_build_phases.find{|p| "
                + "p.name=='Embed App Extensions'} || "
                + "main_app_target.new_copy_files_build_phase('Embed App Extensions')\n"
                + "embed_phase.build_action_mask = \"2147483647\"\n"
                + "embed_phase.dst_subfolder_spec = \"13\"\n"
                + "embed_phase.run_only_for_deployment_postprocessing=\"0\"\n"
                + "embed_phase.add_file_reference(fileref)\n"
                + "service_target.build_configurations.each{|e| \n");
        for (String buildSettingKey : buildSettingsMap.keySet()) {
            sb.append("  e.build_settings['" + buildSettingKey + "'] = \""
                    + buildSettingsMap.get(buildSettingKey) + "\"\n");
        }
        sb.append("}\n");
        sb.append("end\n");
        sb.append("xcproj.save(project_file)\n");
    }

    private void appendWalletExtensionTargets(StringBuilder sb, BuildRequest request, File distDir) throws IOException {
        IOSWalletExtensionBuilder walletBuilder = new IOSWalletExtensionBuilder()
                .setAppGroupId(request.getArg("ios.wallet.appGroup", ""))
                .setIssuerEndpoint(request.getArg("ios.wallet.issuerEndpoint", ""))
                .setAuthEndpoint(request.getArg("ios.wallet.authEndpoint", ""))
                .setNonUIExtensionName(request.getArg("ios.wallet.nonuiExtensionName", "WalletNonUIExtension"))
                .setUIExtensionName(request.getArg("ios.wallet.uiExtensionName", "WalletUIExtension"));
        for (String[] hintAndMarker : WALLET_INJECTION_HINTS) {
            walletBuilder.setInjection(hintAndMarker[1], request.getArg(hintAndMarker[0], null));
        }

        String nonUIName = walletBuilder.getNonUIExtensionName();
        IOSWalletExtensionBuilder.writeFileMap(walletBuilder.buildNonUIFileMap(), new File(distDir, nonUIName));
        appendWalletExtensionRuby(sb, request, nonUIName, distDir, "ios.wallet.nonui.buildSettings.");
        log("Adding Wallet issuer-provisioning extension target " + nonUIName);

        if ("true".equals(request.getArg("ios.wallet.includeUI", "false"))) {
            String uiName = walletBuilder.getUIExtensionName();
            IOSWalletExtensionBuilder.writeFileMap(walletBuilder.buildUIFileMap(), new File(distDir, uiName));
            appendWalletExtensionRuby(sb, request, uiName, distDir, "ios.wallet.ui.buildSettings.");
            log("Adding Wallet issuer-provisioning authorization UI extension target " + uiName);
        }
        sb.append("xcproj.save(project_file)\n");
    }

    private void appendWalletExtensionRuby(StringBuilder sb, BuildRequest request, String extensionName, File distDir, String buildSettingsHintPrefix) {
        Map<String, String> buildSettingsMap = new LinkedHashMap<String, String>();
        buildSettingsMap.put("PRODUCT_BUNDLE_IDENTIFIER", request.getPackageName() + "." + extensionName);
        buildSettingsMap.put("PRODUCT_NAME", "$(TARGET_NAME)");
        buildSettingsMap.put("INFOPLIST_FILE", extensionName + "/Info.plist");
        buildSettingsMap.put("CODE_SIGN_ENTITLEMENTS", extensionName + "/" + extensionName + ".entitlements");
        // PKIssuerProvisioningExtensionHandler requires iOS 14; the extension target
        // keeps its own deployment target even when the app targets lower.
        buildSettingsMap.put("IPHONEOS_DEPLOYMENT_TARGET", "14.0");
        buildSettingsMap.put("TARGETED_DEVICE_FAMILY",
                embeddedExtensionDeviceFamily(request.getArg("ios.project_type", "ios")));
        buildSettingsMap.put("LD_RUNPATH_SEARCH_PATHS", "$(inherited) @executable_path/Frameworks @executable_path/../../Frameworks");
        buildSettingsMap.put("SKIP_INSTALL", "YES");
        buildSettingsMap.put("CLANG_ENABLE_OBJC_ARC", "YES");
        buildSettingsMap.put("CLANG_ENABLE_MODULES", "YES");
        for (String key : request.getArgs()) {
            if (key.startsWith(buildSettingsHintPrefix)) {
                buildSettingsMap.put(key.substring(buildSettingsHintPrefix.length()), request.getArg(key, ""));
            }
        }
        // The whole fragment is guarded so re-running the script (the build
        // re-executes fix_xcode_schemes.rb after dependency integration)
        // doesn't create duplicate targets.
        sb.append("\nif xcproj.targets.find{|e| e.name=='" + extensionName + "'}.nil?\n"
                + "service_target = xcproj.new_target(:app_extension, '" + extensionName + "', :ios, '14.0')\n"
                + "service_target.add_system_framework('PassKit')\n"
                + "service_group = xcproj.new_group('" + extensionName + "')\n");
        appendFilesToXcodeProjGroup(sb, new File(distDir, extensionName), "service_group", "service_target", distDir);
        sb.append("main_app_target = xcproj.targets.find{|e| e.name==main_class_name}\n"
                + "main_app_target.add_dependency(service_target)\n"
                + "fileref = xcproj.groups.find{|e| e.display_name=='Products'}.new_file('" + extensionName + ".appex', \"BUILT_PRODUCTS_DIR\")\n"
                + "embed_phase = main_app_target.copy_files_build_phases.find{|p| p.name=='Embed App Extensions'} || main_app_target.new_copy_files_build_phase('Embed App Extensions')\n"
                + "embed_phase.build_action_mask = \"2147483647\"\n"
                + "embed_phase.dst_subfolder_spec = \"13\"\n"
                + "embed_phase.run_only_for_deployment_postprocessing=\"0\"\n"
                + "embed_phase.add_file_reference(fileref)\n"
                + "service_target.build_configurations.each{|e| \n");
        for (String buildSettingKey : buildSettingsMap.keySet()) {
            sb.append("  e.build_settings['" + buildSettingKey + "'] = \"" + buildSettingsMap.get(buildSettingKey) + "\"\n");
        }
        sb.append("}\n");
        sb.append("end\n");
    }

    /** Xcode target / folder name of the generated watchOS complication extension. */
    static final String SURFACES_WATCH_EXTENSION_NAME = "CN1WatchWidgets";

    /** Xcode target / folder name of the generated WidgetKit extension. */
    static final String SURFACES_EXTENSION_NAME = "CN1Widgets";

    /// The iOS release App Intents first shipped in.
    ///
    /// **Not contributed as a deployment floor**, and that is a measured result rather than an
    /// assumption: a target building at `IPHONEOS_DEPLOYMENT_TARGET = 13.0` with
    /// availability-fenced App Intents declarations emits a complete `Metadata.appintents`
    /// carrying every intent and entity. Xcode does not gate metadata extraction on the
    /// deployment target. So the declarations ship, the app's minimum is untouched, and the
    /// intents are simply not offered on devices too old to run them.
    ///
    /// It survives as the number quoted in diagnostics, and as the value a project can opt into
    /// through `ios.intents.minDeploymentTarget`.
    static final String APP_INTENTS_MIN_IOS = "16.0";

    /// Declares that a tap on a Spotlight result should continue into this app.
    ///
    /// Gated on using the indexing API, deliberately *not* on declaring an intent. An app that
    /// only calls Intents.index() declares none -- parseIntentsManifest treats a missing
    /// manifest as a warning precisely so that app builds -- while the native bridge still
    /// publishes its searchable items. Keying this off a declaration therefore made an entire
    /// supported configuration silently useless: the content was findable, and tapping it did
    /// nothing, because without this key iOS never continues the activity and
    /// nativeSpotlightItemSelected is never reached.
    static String withSpotlightContinuation(String inject, boolean usesIntents) {
        if (!usesIntents || inject.contains("CoreSpotlightContinuation")) {
            return inject;
        }
        return inject + "\n<key>CoreSpotlightContinuation</key><true/>";
    }

    /// Adds this app's intent ids to an NSUserActivityTypes array the project already declared.
    ///
    /// Returns the injection unchanged when the key's array cannot be located, because writing
    /// a second NSUserActivityTypes key would produce a plist iOS reads unpredictably -- worse
    /// than the ids being absent, which at least fails in one direction.
    /// The `NSUserActivityTypes` key for an app that has not declared one itself, or the empty
    /// string when there is nothing to declare.
    ///
    /// Carries only the ids `Intents.donate` can publish -- see
    /// `IOSAppIntentsBuilder.publishesUserActivity` for why advertising the rest is not
    /// harmlessly generous.
    static String userActivityTypesKey(List<Map<String, Object>> intents) {
        StringBuilder types = new StringBuilder();
        for (Map<String, Object> intent : intents) {
            Object id = intent.get("id");
            if (id instanceof String && IOSAppIntentsBuilder.publishesUserActivity(intent)) {
                types.append("<string>").append((String) id).append("</string>");
            }
        }
        if (types.length() == 0) {
            // An app whose only assistant-exposed intent is destructive reaches here and
            // contributes nothing. Writing the key with an empty array would state that the app
            // continues no activity at all -- into the plist of an app that may well continue
            // its own -- so nothing is written when there is nothing to say.
            return "";
        }
        return "\n<key>NSUserActivityTypes</key><array>" + types + "</array>";
    }

    static String mergeUserActivityTypes(String inject, List<Map<String, Object>> intents) {
        int key = inject.indexOf("NSUserActivityTypes");
        int open = key < 0 ? -1 : inject.indexOf("<array>", key);
        int close = open < 0 ? -1 : inject.indexOf("</array>", open);
        if (close < 0) {
            return inject;
        }
        String existing = inject.substring(open, close);
        StringBuilder add = new StringBuilder();
        for (Map<String, Object> intent : intents) {
            Object id = intent.get("id");
            // Same filter as the emission path above: an id donation will never publish has no
            // business in the app's own array either. See publishesUserActivity.
            if (id instanceof String
                    && IOSAppIntentsBuilder.publishesUserActivity(intent)
                    && !existing.contains("<string>" + (String) id + "</string>")) {
                add.append("<string>").append((String) id).append("</string>");
            }
        }
        if (add.length() == 0) {
            return inject;
        }
        return inject.substring(0, close) + add + inject.substring(close);
    }

    /// Reads the `intents.json` the annotation processor emitted into the project jar and
    /// decides what this app has to pay for it.
    ///
    /// The deployment target is the interesting part, and the answer is that nothing here
    /// moves it. That was measured rather than assumed: Xcode emits a complete
    /// `Metadata.appintents` for a target deploying to 13.0, so availability-fenced
    /// declarations ship and are simply not offered on devices too old for them. Indexing and
    /// donation cost nothing either, being Objective-C APIs that predate the current floor.
    ///
    /// A project that would rather not ship a build whose intents are inert for part of its
    /// audience can still contribute a floor with `ios.intents.minDeploymentTarget`, which is
    /// the only path below that reaches `addMinDeploymentTarget`.
    private void parseIntentsManifest(File resDir, BuildRequest request) throws BuildException {
        if (!usesIntents) {
            return;
        }
        // Namespaced: the root belongs to the application, and an app with its own
        // intents.json must not have it read as framework metadata. See MANIFEST_RESOURCE in
        // AppIntentAnnotationProcessor. Only this path is read -- falling back to the root
        // would reintroduce exactly the collision the namespace exists to prevent.
        File manifest = new File(resDir, "META-INF/codenameone/intents.json");
        if (!manifest.exists()) {
            // Legitimate: the app indexes content or donates shortcuts without declaring any
            // @AppIntent, so the processor had nothing to emit.
            debug("cn1: com.codename1.intents is used but no intents.json was generated; "
                    + "building with indexing and donation only");
            return;
        }
        Map<String, Object> parsed;
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(manifest), StandardCharsets.UTF_8)) {
            parsed = new com.codename1.builders.util.JSONParser().parseJSON(reader);
        } catch (IOException ex) {
            throw new BuildException("Failed to parse intents.json", ex);
        }
        intentsManifest = asMapList(parsed.get("intents"));
        entitiesManifest = asMapList(parsed.get("entities"));

        // A declared intent is what needs App Intents; an empty list means the project only
        // declared entity types, or only indexes content at runtime.
        // Only intents that actually reach the platform can justify the floor. One declared
        // solely for a language model produces no App Intent at all, so raising the app's
        // minimum for it would drop older devices for nothing.
        declaresAppIntents = false;
        for (Map<String, Object> intent : intentsManifest) {
            if (com.codename1.util.IOSAppIntentsBuilder.isExposedToAssistant(intent)) {
                declaresAppIntents = true;
                break;
            }
        }
        if (!declaresAppIntents) {
            return;
        }
        if ("false".equals(request.getArg("ios.intents.appIntents", "true"))) {
            // The explicit way to keep indexing and donation while staying off the newer floor.
            // Recorded separately from declaresAppIntents so the donation bridge is still
            // generated -- suppressing the declarations must not remove the implementation the
            // native donation path trampolines through.
            appIntentsSuppressed = true;
            declaresAppIntents = false;
            return;
        }

        // Defaults to contributing nothing; see the note on APP_INTENTS_MIN_IOS.
        String floor = request.getArg("ios.intents.minDeploymentTarget", "");
        if (floor == null || floor.trim().length() == 0) {
            return;
        }
        String pinned = request.getArg("ios.deployment_target", null);
        if (pinned != null && compareVersionStrings(pinned, floor) < 0) {
            // Raising past an explicit pin would override a decision the developer made on
            // purpose; silently dropping the intents would ship a build whose declared
            // capabilities never appear, which is worse than not offering the feature. So say
            // what happened and name both ways out. This can only fire on newly written
            // opt-in code, never on an upgrade of an existing project.
            throw new BuildException("ios.intents.minDeploymentTarget asks for iOS " + floor
                    + ", but ios.deployment_target is pinned to " + pinned + ".\n"
                    + "Raise ios.deployment_target to " + floor + ", or drop "
                    + "ios.intents.minDeploymentTarget -- it is not required. App Intents "
                    + "declarations are availability-fenced and their metadata is emitted for a "
                    + "lower target, so intents simply do not appear on devices below iOS "
                    + APP_INTENTS_MIN_IOS + ".");
        }
        addMinDeploymentTarget(floor);
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<Map<String, Object>> asMapList(Object o) {
        java.util.List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        if (o instanceof java.util.List) {
            for (Object e : (java.util.List<Object>) o) {
                if (e instanceof Map) {
                    out.add((Map<String, Object>) e);
                }
            }
        }
        return out;
    }

    /// Writes the generated Swift App Intents declarations into the app target's source
    /// directory. The existing scheme script sweeps every `.swift` under `<Main>-src` into the
    /// app target, so no Xcode plumbing is added here.
    ///
    /// Runs only when the app declares an intent. An app that merely indexes content gets Core
    /// Spotlight and no Swift at all, which is also why an empty `AppShortcutsProvider` is never
    /// emitted -- Apple rejects one.
    private void generateAppIntentSources(File distDir, BuildRequest request) throws IOException {
        if (!declaresAppIntents && !appIntentsSuppressed) {
            return;
        }
        if (appIntentsSuppressed) {
            // Before any Swift is written, not after. Donation is Objective-C and needs no
            // Swift at all -- which is what lets it keep working below the App Intents
            // minimum, and the whole reason this opt-out exists. Emitting the bridge, the
            // snippet view and the surface renderer anyway put SwiftUI types into a project
            // that had just asked to stay off them, so a target pinned below iOS 13 failed
            // Swift availability checking for a feature it had explicitly declined.
            log("App Intents declarations suppressed (ios.intents.appIntents=false); "
                    + "Spotlight indexing and donation are unaffected");
            return;
        }
        File srcDir = new File(distDir, request.getMainClass() + "-src");
        srcDir.mkdirs();

        // The static half: the Java-to-Swift bridge, the donation shim, and the Objective-C
        // host that is the only legal way for Swift to reach the translated Java.
        String[] staticFiles = {"CN1IntentBridge.swift", "CN1IntentSnippetView.swift",
                "CN1IntentHost.h", "CN1IntentHost.m"};
        for (String name : staticFiles) {
            copyResourceTo("/com/codename1/builders/intents/ios/" + name,
                    new File(srcDir, name));
        }
        // Staging the header is not the same as making it visible. Objective-C declarations in
        // the same target are not automatically in scope for Swift -- they have to come through
        // the bridging header -- and this one was written with nothing but a comment in it, so
        // every reference to CN1IntentHost in the generated Swift failed to compile with
        // "cannot find 'CN1IntentHost' in scope". A device build is the only thing that could
        // have said so, which is why the sample now declares an intent.
        //
        // Appended rather than written, and only once: the header is shared with anything else
        // that needs to reach Objective-C from Swift. The path is relative to the header's own
        // directory, which is SRCROOT -- SWIFT_OBJC_BRIDGING_HEADER points at
        // $(SRCROOT)/cn1-Bridging-Header.h.
        importIntoBridgingHeader(distDir,
                request.getMainClass() + "-src/CN1IntentHost.h");
        // A snippet is a small layout rendered while the app may be off screen, which is what a
        // widget is, so it reuses the surfaces node renderer rather than growing a second
        // layout vocabulary. Written under the same filenames the surfaces builder uses, so an
        // app that has both features gets one copy rather than two conflicting declarations.
        String[] sharedRenderer = {"CN1SurfaceModel.swift", "CN1SurfaceRenderer.swift"};
        for (String name : sharedRenderer) {
            copyResourceTo("/com/codename1/builders/surfaces/ios/" + name,
                    new File(srcDir, name));
        }
        // The model resolves the surfaces App Group through a constant the surfaces builder
        // generates. An app that declares an intent but publishes no widgets never gets that
        // file, so the renderer would not compile -- written here only when it is absent, so a
        // project using both features keeps the real app group the surfaces build wrote.
        File config = new File(srcDir, "CN1SurfaceConfig.swift");
        if (!config.exists()) {
            String body = "// Auto-generated by Codename One. This application declares app\n"
                    + "// intents but publishes no external surfaces, so there is no App Group\n"
                    + "// to resolve: an intent snippet reads its images from a per-invocation\n"
                    + "// directory rather than the shared container.\n"
                    + "import Foundation\n\n"
                    + "let cn1SurfacesAppGroup = \"\"\n";
            try (FileOutputStream out = new FileOutputStream(config)) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }

        IOSAppIntentsBuilder gen = new IOSAppIntentsBuilder(intentsManifest, entitiesManifest);
        Map<String, String> generated = gen.buildAppTargetFileMap();
        if (!gen.getOmittedShortcutIds().isEmpty()) {
            // Named rather than dropped quietly: the intents still work, they simply have no
            // spoken phrase, and a developer who wrote one has to be able to find out why it
            // never worked.
            log("Apple allows ten App Shortcut phrases per app. These intents keep working and "
                    + "are still offered in the Shortcuts app, but their phrases were left out: "
                    + gen.getOmittedShortcutIds());
        }
        for (Map.Entry<String, String> e : generated.entrySet()) {
            try (FileOutputStream out = new FileOutputStream(new File(srcDir, e.getKey()))) {
                out.write(e.getValue().getBytes(StandardCharsets.UTF_8));
            }
        }
        log("Generating App Intents declarations (" + intentsManifest.size() + " intent(s), "
                + entitiesManifest.size() + " entity type(s))");
    }

    /// Adds an `#import` to the project's Swift bridging header, creating the header when the
    /// Swift settings injection has not written it yet.
    ///
    /// Idempotent because both this and the Swift-settings pass can reach the file, and a
    /// duplicate import is a redefinition once the header is preprocessed into the Swift
    /// interface.
    private void importIntoBridgingHeader(File distDir, String relativeHeaderPath)
            throws IOException {
        File bridging = new File(distDir, "cn1-Bridging-Header.h");
        String line = "#import \"" + relativeHeaderPath + "\"\n";
        String existing = "";
        if (bridging.exists()) {
            existing = new String(java.nio.file.Files.readAllBytes(bridging.toPath()),
                    StandardCharsets.UTF_8);
            if (existing.contains(line)) {
                return;
            }
        } else {
            existing = "// Codename One generated Swift bridging header\n";
        }
        try (FileOutputStream out = new FileOutputStream(bridging)) {
            out.write((existing + line).getBytes(StandardCharsets.UTF_8));
        }
    }

    /// Copies a plugin resource to disk, failing loudly when it is missing: a silently absent
    /// bridge file would surface much later as an unresolved Swift symbol.
    private void copyResourceTo(String resource, File target) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Missing plugin resource " + resource);
            }
            try (FileOutputStream out = new FileOutputStream(target)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = in.read(buf)) > 0) {
                    out.write(buf, 0, r);
                }
            }
        }
    }

    /**
     * Parses the surfaces.json build-time manifest ({appGroup?, liveActivities?, kinds:[{id,
     * name, description, iosFamilies?, preview?}]}) and resolves the app group. Fails the
     * build loudly when the app uses com.codename1.surfaces without a manifest -- the widget
     * gallery is compiled into the native app, so kinds cannot be registered at runtime only.
     */
    @SuppressWarnings("unchecked")
    private void parseSurfacesManifest(File resDir, BuildRequest request) throws BuildException {
        surfacesExtensionEnabled = usesSurfaces
                && !"false".equals(request.getArg("ios.surfaces.extension", "true"));
        if (!surfacesExtensionEnabled) {
            // Either the app never touches com.codename1.surfaces, or the developer opted out
            // with ios.surfaces.extension=false. In both cases the iOS lowering is skipped
            // entirely (no CN1_USE_WIDGETS flip, no extension target, no plist keys): the
            // surfaces API stays an inert no-op at runtime.
            return;
        }
        File manifest = new File(resDir, "surfaces.json");
        if (!manifest.exists()) {
            throw new BuildException("This app uses com.codename1.surfaces but the project has "
                    + "no surfaces.json resource. Widget kinds must be declared at build time; "
                    + "add surfaces.json to src/main/resources, e.g.\n"
                    + "{\"appGroup\": \"group." + request.getPackageName() + "\",\n"
                    + " \"liveActivities\": true,\n"
                    + " \"kinds\": [{\"id\": \"delivery\", \"name\": \"Delivery\", "
                    + "\"description\": \"Track your order\", "
                    + "\"iosFamilies\": [\"small\", \"medium\"]}]}\n"
                    + "or disable the iOS widget extension with ios.surfaces.extension=false.");
        }
        Map<String, Object> parsed;
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(manifest), StandardCharsets.UTF_8)) {
            parsed = new com.codename1.builders.util.JSONParser().parseJSON(reader);
        } catch (IOException ex) {
            throw new BuildException("Failed to parse surfaces.json", ex);
        }
        String manifestAppGroup = parsed.get("appGroup") instanceof String
                ? (String) parsed.get("appGroup") : null;
        String hintAppGroup = request.getArg("ios.surfaces.appGroup", null);
        surfacesAppGroup = manifestAppGroup != null && manifestAppGroup.length() > 0
                ? manifestAppGroup
                : (hintAppGroup != null && hintAppGroup.length() > 0
                        ? hintAppGroup : "group." + request.getPackageName());
        // Validated further down, AFTER the kinds are parsed and it is known whether anything in
        // this manifest reaches iOS at all. A watch-only manifest produces no iOS extension and no
        // application-groups entitlement, so nothing consumes this value -- and failing the whole
        // build on a stale or non-Apple app group that is never used would reject a project that
        // is entirely valid.
        Object liveActivities = parsed.get("liveActivities");
        surfacesLiveActivities = Boolean.TRUE.equals(liveActivities)
                || "true".equals(liveActivities);
        surfacesKinds.clear();
        Object kinds = parsed.get("kinds");
        if (kinds instanceof List) {
            for (Object rawKind : (List<Object>) kinds) {
                if (!(rawKind instanceof Map)) {
                    continue;
                }
                Map<String, Object> kindMap = (Map<String, Object>) rawKind;
                Object id = kindMap.get("id");
                if (!(id instanceof String) || !((String) id).matches("[a-z][a-z0-9_]*")) {
                    throw new BuildException("surfaces.json widget kind ids must match "
                            + "[a-z][a-z0-9_]*; found: " + id);
                }
                IOSWidgetExtensionBuilder.Kind kind =
                        new IOSWidgetExtensionBuilder.Kind((String) id);
                if (kindMap.get("name") instanceof String) {
                    kind.setName((String) kindMap.get("name"));
                }
                if (kindMap.get("description") instanceof String) {
                    kind.setDescription((String) kindMap.get("description"));
                }
                if (kindMap.get("preview") instanceof String) {
                    kind.setPreviewName((String) kindMap.get("preview"));
                }
                // "families" is the portable spelling and "iosFamilies" the legacy one; the
                // shared reader picks between them so Android resolves a kind's families the
                // same way rather than parsing a key with "ios" in its name.
                List<String> families = com.codename1.util.SurfaceKindFamilies.read(kindMap);
                // Refused here as well as on Android, and for a worse failure than the one there.
                // A name this framework does not know is not a watch family, so the kind reads as
                // an iPhone surface -- and familiesSwift cannot map it either, so it falls back to
                // all three home-screen sizes. A typo therefore SHIPS three widgets the manifest
                // never asked for, rather than shipping none.
                for (String declared : families) {
                    if (!com.codename1.util.SurfaceKindFamilies.isKnown(declared)) {
                        throw new BuildException("Widget kind '" + kind.getId()
                                + "' in surfaces.json declares the family '" + declared
                                + "', which is not one this framework knows. The watch families "
                                + "are watchCircular, watchRectangular, watchInline and "
                                + "watchCorner; the phone families are small, medium, large and "
                                + "lockscreen.");
                    }
                }
                if (!families.isEmpty()) {
                    kind.setIosFamilies(families);
                }
                surfacesKinds.add(kind);
            }
        }
        if (surfacesKinds.isEmpty() && !surfacesLiveActivities) {
            throw new BuildException("surfaces.json declares neither widget kinds nor "
                    + "\"liveActivities\": true; there is nothing to build");
        }
        // Whether anything in this manifest can appear on iOS, decided HERE -- before the app
        // group, CN1_USE_WIDGETS and the app-target Swift glue are gated on
        // surfacesExtensionEnabled. Turning the flag off later, at the point the extension target
        // would have been generated, was too late: the host app had already been given an
        // application-groups entitlement and compiled widget support for an extension that is
        // never produced, and a release profile without that group then fails code signing.
        boolean anyIosSurface = surfacesLiveActivities;
        for (IOSWidgetExtensionBuilder.Kind kind : surfacesKinds) {
            if (!IOSWidgetExtensionBuilder.isWatchOnly(kind)) {
                anyIosSurface = true;
                break;
            }
        }
        // Whether a complication reaches a device is a separate question from whether an iOS
        // widget does, and it is answered separately: a watch-only manifest produces no iOS
        // extension and must still produce a watch one.
        boolean anyWatchSurface = false;
        for (IOSWidgetExtensionBuilder.Kind kind : surfacesKinds) {
            if (IOSWidgetExtensionBuilder.hasWatchFamily(kind)) {
                anyWatchSurface = true;
                break;
            }
        }
        surfacesWatchEnabled = anyWatchSurface && watchTargetEnabled(request);
        if (!anyIosSurface) {
            // Said HERE, and in full. Turning the flag off is what stops widgetExtensionBuilder
            // from ever being created, and the watch-only notice at the extension-generation site
            // is guarded on that builder existing -- so the one case the notice was written for
            // was the one case it could not reach. The build then discarded every declared surface
            // while reporting only that the iOS lowering had been skipped.
            StringBuilder names = new StringBuilder();
            for (IOSWidgetExtensionBuilder.Kind kind : surfacesKinds) {
                if (names.length() > 0) {
                    names.append(", ");
                }
                names.append(kind.getId());
            }
            if (surfacesWatchEnabled) {
                log("[surfaces] These kinds declare only watch complication families and are "
                        + "hosted by the watch app's " + SURFACES_WATCH_EXTENSION_NAME
                        + " extension: " + names + ". No iOS extension is generated, because "
                        + "there is no iPhone surface to put them on.");
            } else if (anyWatchSurface) {
                log("[surfaces] NOTE: these kinds declare only watch complication families and "
                        + "will NOT appear on any device in this build: " + names + ". They are "
                        + "hosted by the watch app, and this project declares no "
                        + "codename1.watchMain -- add one, or declare a phone family alongside "
                        + "them.");
            } else {
                log("[surfaces] NOTE: these kinds declare no family this build can host and will "
                        + "NOT appear on any device: " + names + ".");
            }
            surfacesExtensionEnabled = false;
            if (!surfacesWatchEnabled) {
                log("[surfaces] No iOS extension is generated and the iOS surface lowering is "
                        + "skipped entirely -- no app group, no widget support compiled into the "
                        + "app.");
                // Nothing further to prepare, and in particular no xcodeproj gem to require: that
                // check exists for wiring an extension into the project, and there is no
                // extension.
                return;
            }
            // Deliberately falling through with surfacesExtensionEnabled false. The watch
            // extension still has to be wired into the project, which needs the xcodeproj gem
            // checked below and an app group validated -- and the group is now something this
            // build genuinely uses, even though the PHONE gets neither an entitlement for it nor
            // an extension to use it.
            log("[surfaces] The watch app publishes surfaces; the phone app does not.");
        }
        // Only now, with a surface confirmed on one platform or the other, is the app group
        // something this build actually uses -- and only now is rejecting a malformed one the
        // right answer.
        if (!surfacesAppGroup.startsWith("group.")) {
            throw new BuildException("The surfaces app group must start with 'group.' (Apple "
                    + "requirement); found '" + surfacesAppGroup + "' (from surfaces.json "
                    + "appGroup or the ios.surfaces.appGroup build hint)");
        }
        // The extension is wired into the Xcode project through the ruby xcodeproj gem;
        // fail early with a friendly message when it is missing.
        ensureXcodeprojInstalled();
        log("External surfaces enabled: app group " + surfacesAppGroup + ", "
                + surfacesKinds.size() + " widget kind(s)"
                + (surfacesLiveActivities ? ", live activities" : ""));
    }

    /**
     * Generates the watchOS complication extension under dist/ and hands it to
     * {@link WatchNativeBuilder}, which embeds it in the watch app target.
     *
     * <p>Deliberately NOT part of {@link #appendWidgetExtensionTargets}. That path runs inside
     * the schemes ruby, at a point where the watch app target does not exist yet -- and this
     * extension is embedded in the watch app, not the phone app. So it is generated here,
     * immediately before the watch builder runs its own xcodeproj script, and wired by that
     * script instead.</p>
     *
     * <p>The app-side Swift glue goes into the same {@code <MainClass>-src} folder the phone's
     * does. The two are the same files, and the watch script adds them to the watch target by
     * name; the schemes script's sweep of that folder into the PHONE target is what already
     * handles the case where the watch shares the phone's translation.</p>
     *
     * @param request the build
     * @param distDir the generated project's dist folder
     * @param appSrcDir the {@code <MainClass>-src} folder
     */
    private void writeWatchWidgetExtension(BuildRequest request, File distDir, File appSrcDir)
            throws IOException {
        if (!surfacesWatchEnabled) {
            return;
        }
        IOSWidgetExtensionBuilder watchBuilder = new IOSWidgetExtensionBuilder()
                .setVersions(embeddedExtensionShortVersion(request),
                        embeddedExtensionBundleVersion(request))
                .setWatchTarget(true)
                .setExtensionName(SURFACES_WATCH_EXTENSION_NAME)
                // The extension is nested in the watch app, so its bundle id extends the WATCH
                // bundle id rather than the phone's.
                .setHostBundleId(request.getPackageName() + ".watchkitapp")
                .setAppGroupId(surfacesAppGroup)
                // The WATCH APP's target, not the extension's own floor. WidgetKit goes back to
                // watchOS 9 and the extension can build there, but it is embedded in the watch
                // app -- a watch that cannot install the app cannot show its complication, so
                // defaulting to the lower number advertised support that does not exist. The
                // extension's floor stays where it is for a project that lowers both.
                .setDeploymentTarget(request.getArg("watchNative.surfaces.deploymentTarget",
                        WatchNativeBuilder.MIN_DEPLOYMENT_TARGET));
        for (IOSWidgetExtensionBuilder.Kind kind : surfacesKinds) {
            watchBuilder.addKind(kind);
        }
        if (!watchBuilder.hasWatchSurface()) {
            // Cannot happen -- surfacesWatchEnabled was decided from the same predicate -- but
            // generating an empty WidgetBundle would break the watch build rather than degrade,
            // so the check is worth its two lines.
            return;
        }
        File extensionDir = new File(distDir, SURFACES_WATCH_EXTENSION_NAME);
        IOSWalletExtensionBuilder.writeFileMap(watchBuilder.buildFileMap(), extensionDir);
        IOSWalletExtensionBuilder.writeFileMap(watchBuilder.buildAppTargetFileMap(), appSrcDir);
        watchNativeBuilder.setWidgetExtension(extensionDir, surfacesAppGroup,
                watchBuilder.getDeploymentTarget());
        int complications = 0;
        for (IOSWidgetExtensionBuilder.Kind kind : surfacesKinds) {
            if (IOSWidgetExtensionBuilder.hasWatchFamily(kind)) {
                complications++;
            }
        }
        log("Adding watchOS complication extension target " + SURFACES_WATCH_EXTENSION_NAME
                + " (" + complications + " complication kind(s), watchOS "
                + watchBuilder.getDeploymentTarget() + ")");
    }

    /**
     * Generates the CN1Widgets WidgetKit extension folder under dist/, drops the app-side
     * Swift glue into &lt;MainClass&gt;-src (the schemes ruby sweeps *.swift there into the
     * APP target) and appends the ruby that wires the extension target into the generated
     * Xcode project. Modeled on {@link #appendWalletExtensionTargets}.
     */
    private void appendWidgetExtensionTargets(StringBuilder sb, BuildRequest request, File distDir) throws IOException {
        IOSWidgetExtensionBuilder widgetBuilder = new IOSWidgetExtensionBuilder()
                .setVersions(embeddedExtensionShortVersion(request),
                        embeddedExtensionBundleVersion(request))
                .setExtensionName(SURFACES_EXTENSION_NAME)
                .setHostBundleId(request.getPackageName())
                .setAppGroupId(surfacesAppGroup)
                .setDeploymentTarget(request.getArg("ios.surfaces.deploymentTarget", "16.1"))
                .setLiveActivitiesEnabled(surfacesLiveActivities);
        for (IOSWidgetExtensionBuilder.Kind kind : surfacesKinds) {
            widgetBuilder.addKind(kind);
        }
        // Named out loud, every time, whether or not the extension is generated. A watch-only
        // kind is silently dropped from the iOS bundle, so a developer who declares one and hears
        // nothing has to work out for themselves where it went -- and, when there is no watch
        // target, that it went nowhere at all.
        StringBuilder watchOnly = new StringBuilder();
        for (IOSWidgetExtensionBuilder.Kind watchKind : surfacesKinds) {
            if (IOSWidgetExtensionBuilder.isWatchOnly(watchKind)) {
                if (watchOnly.length() > 0) {
                    watchOnly.append(", ");
                }
                watchOnly.append(watchKind.getId());
            }
        }
        if (watchOnly.length() > 0) {
            if (surfacesWatchEnabled) {
                log("[surfaces] These kinds declare only watch complication families and are "
                        + "hosted by " + SURFACES_WATCH_EXTENSION_NAME + " rather than by the iOS "
                        + "extension: " + watchOnly + ".");
            } else {
                log("[surfaces] NOTE: these kinds declare only watch complication families and "
                        + "will NOT appear on any device in this build: " + watchOnly + ". They "
                        + "are hosted by the watch app, and this project declares no "
                        + "codename1.watchMain -- add one, or declare a phone family alongside "
                        + "them.");
            }
        }
        if (!widgetBuilder.hasIosSurface()) {
            // Every declared kind is a watch complication and there is no live activity, so the
            // iOS extension would host nothing -- and a WidgetBundle with an empty body does not
            // compile. Declaring only complications is legitimate: those kinds are hosted by the
            // watch extension instead, so skip this one rather than failing the build.
            log("Skipping the WidgetKit extension target: surfaces.json declares only watch "
                    + "complication families, which the iOS extension cannot host");
            return;
        }
        String extensionName = widgetBuilder.getExtensionName();
        File extensionDir = new File(distDir, extensionName);
        IOSWalletExtensionBuilder.writeFileMap(widgetBuilder.buildFileMap(), extensionDir);
        // App-target glue: the Swift CN1SurfaceBridge (reached from IOSNative.m via
        // NSClassFromString), the shared ActivityAttributes struct and the app group
        // constant. Written into <MainClass>-src so the schemes script compiles them
        // into the APP target, not the extension.
        IOSWalletExtensionBuilder.writeFileMap(widgetBuilder.buildAppTargetFileMap(),
                new File(distDir, request.getMainClass() + "-src"));
        appendWidgetExtensionRuby(sb, request, widgetBuilder, extensionDir, distDir);
        log("Adding WidgetKit extension target " + extensionName + " ("
                + surfacesKinds.size() + " widget kind(s)"
                + (surfacesLiveActivities ? " + live activities" : "") + ")");
        sb.append("xcproj.save(project_file)\n");
    }

    private void appendWidgetExtensionRuby(StringBuilder sb, BuildRequest request,
            IOSWidgetExtensionBuilder widgetBuilder, File extensionDir, File distDir) throws IOException {
        String extensionName = widgetBuilder.getExtensionName();
        Map<String, String> buildSettingsMap = new LinkedHashMap<String, String>();
        buildSettingsMap.put("PRODUCT_NAME", "$(TARGET_NAME)");
        buildSettingsMap.put("TARGETED_DEVICE_FAMILY",
                embeddedExtensionDeviceFamily(request.getArg("ios.project_type", "ios")));
        buildSettingsMap.put("LD_RUNPATH_SEARCH_PATHS", "$(inherited) @executable_path/Frameworks @executable_path/../../Frameworks");
        buildSettingsMap.put("CLANG_ENABLE_MODULES", "YES");
        // The builder's buildSettings.properties supplies the deployment target, Swift
        // version, bundle id, entitlements and Info.plist paths; deleted after loading so
        // it is not added to the Xcode group as a resource.
        File buildSettingsProps = new File(extensionDir, "buildSettings.properties");
        if (buildSettingsProps.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(buildSettingsProps)) {
                props.load(fis);
            }
            for (Object key : props.keySet()) {
                if (key instanceof String) {
                    buildSettingsMap.put((String) key, props.getProperty((String) key));
                }
            }
            buildSettingsProps.delete();
        }
        for (String key : request.getArgs()) {
            if (key.startsWith("ios.surfaces.buildSettings.")) {
                buildSettingsMap.put(key.substring("ios.surfaces.buildSettings.".length()),
                        request.getArg(key, ""));
            }
        }
        // CRITICAL ordering note: this fragment rides in appExtensionsBuilder, which the
        // schemes script appends AFTER the global deployment-target pass
        // (deploymentTargetStr stomps IPHONEOS_DEPLOYMENT_TARGET on every existing target).
        // Because this target is created after that pass ran, its 16.1 deployment target
        // below survives. Do not move this fragment before deploymentTargetStr.
        // The whole fragment is guarded so re-running the script (the build re-executes
        // fix_xcode_schemes.rb after dependency integration) doesn't duplicate the target.
        // No explicit framework linkage: Swift autolinks WidgetKit/ActivityKit (weakly
        // where guarded by canImport/@available).
        sb.append("\nif xcproj.targets.find{|e| e.name=='" + extensionName + "'}.nil?\n"
                + "service_target = xcproj.new_target(:app_extension, '" + extensionName + "', :ios, '"
                + widgetBuilder.getDeploymentTarget() + "')\n"
                + "service_group = xcproj.new_group('" + extensionName + "')\n");
        appendFilesToXcodeProjGroup(sb, extensionDir, "service_group", "service_target", distDir);
        sb.append("main_app_target = xcproj.targets.find{|e| e.name==main_class_name}\n"
                + "main_app_target.add_dependency(service_target)\n"
                + "fileref = xcproj.groups.find{|e| e.display_name=='Products'}.new_file('" + extensionName + ".appex', \"BUILT_PRODUCTS_DIR\")\n"
                + "embed_phase = main_app_target.copy_files_build_phases.find{|p| p.name=='Embed App Extensions'} || main_app_target.new_copy_files_build_phase('Embed App Extensions')\n"
                + "embed_phase.build_action_mask = \"2147483647\"\n"
                + "embed_phase.dst_subfolder_spec = \"13\"\n"
                + "embed_phase.run_only_for_deployment_postprocessing=\"0\"\n"
                + "embed_file = embed_phase.add_file_reference(fileref)\n");
        if (macNativeBuilder.isEnabled()) {
            // Mac Catalyst v1 guard: this iOS build also produces a Mac Catalyst slice
            // (macNative.enabled=true), but the CN1Widgets extension is iOS-only in v1 --
            // MacNativeBuilder marks only the APP target SUPPORTS_MACCATALYST and its Mac
            // entitlements carry no app group, so building/embedding the extension for the
            // Mac destination would fail the Catalyst archive. Platform-filter the target
            // dependency and the embed step to the iOS slice: the iOS app keeps its widgets
            // and live activities, the Mac slice simply ships without them.
            sb.append("dep = main_app_target.dependencies.find{|d| d.target && d.target.uuid == service_target.uuid}\n"
                    + "dep.platform_filter = 'ios' if dep\n"
                    + "embed_file.platform_filter = 'ios'\n");
            buildSettingsMap.put("SUPPORTS_MACCATALYST", "NO");
        }
        sb.append("service_target.build_configurations.each{|e| \n");
        for (String buildSettingKey : buildSettingsMap.keySet()) {
            sb.append("  e.build_settings['" + buildSettingKey + "'] = \"" + buildSettingsMap.get(buildSettingKey) + "\"\n");
        }
        sb.append("}\n");
        sb.append("end\n");
    }

    /**
     * Moves every {@code .ios.appext} archive out of the resources directory, into a staging
     * directory the extension wiring reads much later.
     *
     * <p>The unpacking cannot happen this early -- it wants the Xcode project that does not
     * exist yet -- but the move cannot happen any later. The resources directory is handed to
     * the translator, which copies it into {@code <main>-src}, and every file in there becomes
     * an app resource: the archive shipped inside the .app, an unbuilt second copy of the
     * extension sitting next to the .appex it had been unpacked into. Deleting it from resDir
     * at unpack time is too late to stop that copy. Every other archive kind consumed out of
     * resDir deletes itself for the same reason.</p>
     *
     * @return the staging directory, or null when the app brought no extension archive
     */
    static File stageAppExtensionArchives(File resDir, File stagingDir) throws IOException {
        File[] entries = resDir == null ? null : resDir.listFiles();
        if (entries == null) {
            return null;
        }
        File staged = null;
        for (File child : entries) {
            if (!child.isFile() || !child.getName().endsWith(".ios.appext")) {
                continue;
            }
            if (staged == null) {
                staged = stagingDir;
                staged.mkdirs();
            }
            Files.move(child.toPath(), new File(staged, child.getName()).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        return staged;
    }

    private File[] extractAppExtensions(File sourceDirectory, File targetDirectory) throws IOException {
        if (sourceDirectory == null || !sourceDirectory.isDirectory()) {
            throw new IllegalArgumentException("extractAppExtensions sourceDirectory must be an existing directory but received "+sourceDirectory);
        }
        List<File> out = new ArrayList<>();
        File[] entries = sourceDirectory.listFiles();
        if (entries == null) {
            return new File[0];
        }
        for (File appExtension : entries) {
            if (!appExtension.getName().endsWith(".ios.appext")) {
                // Only interested in files ending in .ios.appext since
                // Maven would have bundled the app extensions in this way.
                continue;
            }
            File extractedDir = new File(targetDirectory, appExtension.getName().substring(0, appExtension.getName().lastIndexOf(".ios.appext")));
            if (extractedDir.exists()) {
                delTree(extractedDir);
            }
            extractedDir.mkdir();
            try {
                boolean result = exec(sourceDirectory, "/usr/bin/unzip", appExtension.getName(), "-d", extractedDir.getAbsolutePath());
                if (!result) {
                    throw new IOException("Failed to unzip appExtension "+appExtension);
                }

                File escaping = symlinkEscaping(extractedDir, extractedDir);
                if (escaping != null) {
                    throw new IOException("The " + extractedDir.getName() + " app extension "
                            + "contains a symbolic link, " + escaping.getName() + ", that points "
                            + "outside the extension. Xcode copies what an extension folder holds "
                            + "into the app, so a link out of it would put a file from the build "
                            + "machine into your app. Remove the link and rebuild.");
                }
                out.add(extractedDir);
            } catch (IOException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IOException("Failed to unzip app extension "+appExtension, ex);
            }
            // Once we have extracted it, we should delete it because we don't need it anymore.
            appExtension.delete();

        }
        return out.toArray(new File[out.size()]);
    }

    private void injectToPlist(File tmpFile, File resDir, BuildRequest request) throws IOException {
        File buildinRes = new File(tmpFile, "btres");
        File mat = new File(buildinRes, "material-design-font.ttf");
        if(mat.exists()) {
            copy(new File(buildinRes, "material-design-font.ttf"), new File(resDir, "material-design-font.ttf"));
        }
        File[] fontFiles = resDir.listFiles(new FilenameFilter() {

            @Override
            public boolean accept(File file, String string) {
                // Core Text reads the SFNT container whether the outlines are
                // glyf or CFF, so OpenType registers through UIAppFonts exactly
                // like TrueType. Leaving .otf out here is what made a bundled
                // OpenType font fall back to the system font on the device.
                return endsWithIgnoreCase(string, ".ttf") || endsWithIgnoreCase(string, ".otf");
            }
        });
        
        String facebook = request.getArg("facebook.appId", null);
        String googleClientId = request.getArg("ios.gplus.clientId", null);
        boolean includeGooglePlus = !(googleServicePlist != null && googleServicePlist.useSignIn) && googleClientId != null;
        String statusBarWhite = request.getArg("ios.statusBarFG", null);

        File infoPlist = new File(tmpFile, "build/xcode/sys/" + request.getMainClass() + "-Info.plist");
        if(!infoPlist.exists()) {
            infoPlist = new File(tmpFile, "dist/" + request.getMainClass() + "-src/" + request.getMainClass() + "-Info.plist");
        }

        
        String lang = request.getArg("ios.devLocale", null);
        if(lang != null) {
            replaceAllInFile(infoPlist, "<string>English</string>", "<string>"  + lang + "</string>");
        }

        if ("true".equalsIgnoreCase(request.getArg("ios.uiscene", "true"))) {
            // MainWindow.xib auto-instantiates a UIWindow with visibleAtLaunch=YES; under
            // UIScene the window has no scene and FrontBoard kills the launch in iOS 26.
            // UIApplicationMain(..., @"CodenameOne_GLAppDelegate") still creates the
            // delegate from the class name, so the NIB is no longer needed.
            replaceAllInFile(infoPlist, "<key>NSMainNibFile</key>\\s*<string>[^<]*</string>", "");
        }

        // nothing to inject here? move along
        String inject = request.getArg("ios.plistInject", "<key>CFBundleShortVersionString</key> 	<string>" + buildVersion +"</string>");

        // On-device-debug: drop the proxy host/port into Info.plist so
        // cn1_debugger.m can read them at app boot without needing the
        // build to also patch source files. Skipped on release builds for
        // the same reason as the translator gate above.
        if ("true".equalsIgnoreCase(request.getArg("ios.onDeviceDebug", "false"))
                && "debug".equals(request.getArg("ios.buildType", "debug"))) {
            String proxyHost = request.getArg("ios.onDeviceDebug.proxyHost", "127.0.0.1");
            String proxyPort = request.getArg("ios.onDeviceDebug.proxyPort", "55333");
            String waitForAttach = "true".equalsIgnoreCase(
                    request.getArg("ios.onDeviceDebug.waitForAttach", "false")) ? "true" : "false";
            inject += "\n<key>CN1ProxyHost</key>\n<string>" + proxyHost + "</string>";
            inject += "\n<key>CN1ProxyPort</key>\n<integer>" + proxyPort + "</integer>";
            inject += "\n<key>CN1ProxyWaitForAttach</key><" + waitForAttach + "/>";
            // ATS exemption for localhost / arbitrary loads so the device
            // can dial out to the developer's laptop without a TLS chain.
            if (!inject.contains("NSAppTransportSecurity")) {
                inject += "\n<key>NSAppTransportSecurity</key>"
                        + "<dict>"
                        + "<key>NSAllowsArbitraryLoads</key><true/>"
                        + "</dict>";
            }
        }

        // Export compliance: when the app uses com.codename1.security.* we
        // route all crypto through Apple's Security framework / CommonCrypto
        // (and, with the ios.crypto.gcm opt-in, AES-GCM via stable SPI
        // symbols). All of these qualify for the "uses standard cryptography"
        // exemption under EAR 740.17, so we set ITSAppUsesNonExemptEncryption
        // to false. Callers can override by setting the
        // ios.appUsesNonExemptEncryption build hint -- pass it as "true" if
        // your app links proprietary crypto in addition to ours, or as ""
        // (empty) to omit the key entirely and answer in App Store Connect.
        String exemptOverride = request.getArg("ios.appUsesNonExemptEncryption", null);
        if (exemptOverride != null) {
            if (exemptOverride.length() > 0 && !inject.contains("ITSAppUsesNonExemptEncryption")) {
                inject += "\n<key>ITSAppUsesNonExemptEncryption</key><"+exemptOverride+"/>";
            }
        } else if (usesCryptoAPI && !inject.contains("ITSAppUsesNonExemptEncryption")) {
            inject += "\n<key>ITSAppUsesNonExemptEncryption</key><false/>";
        }
        
        String applicationQueriesSchemes = request.getArg("ios.applicationQueriesSchemes", null);
        if(applicationQueriesSchemes != null && applicationQueriesSchemes.length() > 0) {
            inject += "<key>LSApplicationQueriesSchemes</key>\n <array>\n";
            for(String s : applicationQueriesSchemes.split(",")) {
                inject += "<string>" + s + "</string>\n";
            }
            if (facebook != null) {
                inject += "  <string>fbauth2</string>\n";
            }
            if (googleClientId != null) {
                inject += "  <string>gplus</string>\n";
            }
            inject += "</array>";
        }
        
        
        // Some stuff for the switch to Xcode 8, but we don't need it yet
        // The keys the fragment DECLARES, not the text it happens to contain. A comment
        // mentioning NSBluetoothAlwaysUsageDescription -- or a purpose string of another key
        // that names it -- suppressed the generated value, and the app was terminated on the
        // device for a disclosure the build had approved. Recomputed here because everything
        // above appends to inject as it goes.
        java.util.List<String> declaredKeys = WatchNativeBuilder.injectedPlistKeys(inject);
        for (String privacyKey : privacyUsageDescriptions.keySet()) {
            if (!declaredKeys.contains(privacyKey)) {
                if (privacyKey.toLowerCase().contains("location")) {
                    // We add location usage descriptions after when we deal with the ios.locationUsageDescription
                    // build hint.
                    continue;
                }
                String val = privacyUsageDescriptions.get(privacyKey);
                if (!"false".equals(val)) {
                    inject += "\n<key>"+privacyKey+"</key><string>"+val+"</string>";
                }
            }
        
        }
        
        boolean multitasking = "true".equals(request.getArg("ios.multitasking", "true"));
        if(request.getArg("ios.generateSplashScreens", "false").equals(
            "true")) {
            multitasking = false;
        }
        if (multitasking && useMetal && getDeploymentTargetInt(request) < 14) {
            // An explicit ios.deployment_target below 14 cannot satisfy the
            // App Store launch screen rule for iPad multitasking apps via the
            // UILaunchScreen key (it only counts when MinimumOSVersion is 14
            // or higher), so opt out of multitasking instead of producing a
            // bundle that fails upload validation.
            log("ios.deployment_target is below 14; implicitly disabling iPad multitasking (UIRequiresFullScreen) so the launch screen passes App Store validation. Set ios.deployment_target=14.0 or higher to keep multitasking support.");
            multitasking = false;
        }


        if (!multitasking || xcodeVersion < 9) {
            if (inject.indexOf("UIRequiresFullScreen") < 0) {
                // Temporary workaround to disable iPad multitasking support.
                // Ultimately we need to migrate to storyboards to support multitasking on iPad
                // http://stackoverflow.com/questions/32559724/ipad-multitasking-support-requires-these-orientations
                inject += "\n<key>UIRequiresFullScreen</key><true/>\n";
            }
        }
        if (!"true".equals(request.getArg("ios.generateSplashScreens", "false"))) {
            if ("true".equalsIgnoreCase(request.getArg("ios.uiscene", "true"))) {
                // SplashBoard never renders the launch storyboard for scene-based
                // CN1 apps -- the system animates from a black frame instead
                // (issue #5210). The iOS 14+ UILaunchScreen generated launch
                // screen does work under UIScene: system background color
                // (light/dark aware) with the launch icon centered, matching the
                // native launch placeholder the app shows until the first EDT
                // frame. UILaunchStoryboardName must be OMITTED here: when both
                // keys are present iOS prefers the storyboard, which is exactly
                // the broken path (verified on the iOS 26 simulator with a cold
                // SplashBoard cache). The ios.launchStoryboardName hint is
                // therefore only honored with ios.uiscene=false; injecting
                // either key via ios.plistInject overrides this default.
                // UIImageName points at the loose Launch.Foreground.png in the
                // bundle root (guaranteed by generateLaunchScreen); SplashBoard
                // resolves it there but fails to render the same image from an
                // actool compiled imageset, so do NOT move it into
                // Images.xcassets.
                if (!inject.contains("UILaunchScreen") && !inject.contains("UILaunchStoryboardName")) {
                    inject += "\n<key>UILaunchScreen</key>\n"
                            + "<dict>\n"
                            + "    <key>UIImageName</key>\n"
                            + "    <string>Launch.Foreground</string>\n"
                            + "</dict>";
                }
            } else if (!inject.contains("UILaunchStoryboardName")) {
                inject += "\n<key>UILaunchStoryboardName</key><string>"+request.getArg("ios.launchStoryboardName", "LaunchScreen")+"</string>";
            }
        }
        boolean useUISceneManifest = "true".equalsIgnoreCase(request.getArg("ios.uiscene", "true"));
        // CarPlay requires the UIScene lifecycle and a dedicated
        // CPTemplateApplicationSceneSessionRoleApplication scene wired to
        // CodenameOne_CarPlaySceneDelegate. Emit the manifest when either UIScene is on or the app
        // uses CarPlay; include the phone window role only under UIScene, and the CarPlay role only
        // when the app references com.codename1.car.
        if ((useUISceneManifest || usesCar) && !inject.contains("UIApplicationSceneManifest")) {
            String carPlayScene = usesCar
                    ? "        <key>CPTemplateApplicationSceneSessionRoleApplication</key>\n"
                    + "        <array>\n"
                    + "            <dict>\n"
                    + "                <key>UISceneConfigurationName</key>\n"
                    + "                <string>CarPlay Configuration</string>\n"
                    + "                <key>UISceneDelegateClassName</key>\n"
                    + "                <string>CodenameOne_CarPlaySceneDelegate</string>\n"
                    + "            </dict>\n"
                    + "        </array>\n"
                    : "";
            String windowScene = useUISceneManifest
                    ? "        <key>UIWindowSceneSessionRoleApplication</key>\n"
                    + "        <array>\n"
                    + "            <dict>\n"
                    + "                <key>UISceneConfigurationName</key>\n"
                    + "                <string>Default Configuration</string>\n"
                    + "                <key>UISceneDelegateClassName</key>\n"
                    + "                <string>CodenameOne_GLSceneDelegate</string>\n"
                    + "            </dict>\n"
                    + "        </array>\n"
                    : "";
            inject += "\n<key>UIApplicationSceneManifest</key>\n"
                    + "<dict>\n"
                    + "    <key>UIApplicationSupportsMultipleScenes</key>\n"
                    // Keep single-scene (false): the CarPlay scene is a distinct scene ROLE
                    // (CPTemplateApplicationSceneSessionRoleApplication), not a second window of the
                    // app role, so it does not need multiple-scene support. Setting this true changed
                    // Mac Catalyst windowing and crashed the screenshot suite (26 GB / signal loop).
                    + "    <false/>\n"
                    + "    <key>UISceneConfigurations</key>\n"
                    + "    <dict>\n"
                    + windowScene
                    + carPlayScene
                    + "    </dict>\n"
                    + "</dict>";
        }

        if(request.getArg("ios.fileSharingEnabled", "false").equals("true")) {
            inject += "\n	<key>UIFileSharingEnabled</key>\n	<true/>\n";
        }
        if(inject.indexOf("CFBundleShortVersionString") < 0) {
            inject += "\n<key>CFBundleShortVersionString</key> 	<string>" + buildVersion +"</string>";
        }
        // Localized icons are emitted by actool through the asset catalog
        // (Images.xcassets/AppIcon_<locale>.appiconset) and the
        // ASSETCATALOG_COMPILER_ALTERNATE_APPICON_NAMES build setting; the
        // resulting partial Info.plist already contains the correct
        // CFBundleIcons entries, so we deliberately do not inject any
        // CFBundleAlternateIcons fragment here. Doing so would conflict with
        // actool's output during the Info.plist merge. We do, however, need
        // to inject CFBundleIconName -- without it actool will not emit the
        // partial Info.plist containing CFBundleIcons / CFBundleAlternateIcons,
        // and -[UIApplication setAlternateIconName:] would then fail at runtime
        // because the bundle does not advertise the alternate icons.
        if (!localizedIcons.isEmpty() && !inject.contains("CFBundleIconName")) {
            inject += "\n<key>CFBundleIconName</key>\n<string>AppIcon</string>";
        }
        String locationUsageDescription = null;
        if (xcodeVersion >= 9) {
            if ( (locationUsageDescription = request.getArg("ios.locationUsageDescription", null)) != null ){
                String key = "NSLocationWhenInUseUsageDescription";
                if(!inject.contains(key)) {
                    inject += "\n<key>"+key+"</key> 	<string>"+locationUsageDescription+"</string>";
                }
                if (request.getArg("ios.background_modes", "").contains("location")) {
                    key = "NSLocationAlwaysUsageDescription";
                    locationUsageDescription = request.getArg("ios.NSLocationAlwaysUsageDescription", locationUsageDescription);
                    if(!inject.contains(key)) {
                        inject += "\n<key>"+key+"</key> 	<string>"+locationUsageDescription+"</string>";
                    }
                    
                    key = "NSLocationAlwaysAndWhenInUseUsageDescription";
                    locationUsageDescription = request.getArg("ios.NSLocationAlwaysAndWhenInUseUsageDescription", locationUsageDescription);
                    if(!inject.contains(key)) {
                        inject += "\n<key>"+key+"</key> 	<string>"+locationUsageDescription+"</string>";
                    }
                    

                }


            }
        } else {
            if ( (locationUsageDescription = request.getArg("ios.locationUsageDescription", null)) != null ){
                String key = "NSLocationWhenInUseUsageDescription";
                if (request.getArg("ios.background_modes", "").contains("location")) {
                    key = "NSLocationAlwaysUsageDescription";
                }

                if(!inject.contains(key)) {
                    inject += "\n<key>"+key+"</key> 	<string>"+locationUsageDescription+"</string>";
                }
            }
        }
        // NSBonjourServices is an Array<String> in Info.plist, not a String,
        // so the generic NS*UsageDescription injector above does not handle
        // it. We expand a comma- or semicolon-separated build-hint value
        // into the required <array><string>...</string></array> fragment.
        String bonjourServices = request.getArg("ios.NSBonjourServices", null);
        if (bonjourServices != null && bonjourServices.length() > 0
                && !WatchNativeBuilder.injectedPlistKeys(inject)
                        .contains("NSBonjourServices")) {
            StringBuilder arr = new StringBuilder();
            arr.append("\n<key>NSBonjourServices</key><array>");
            for (String s : bonjourServices.split("[,;]")) {
                s = s.trim();
                if (s.length() == 0) continue;
                if (!s.endsWith(".")) s = s + ".";
                arr.append("<string>").append(s).append("</string>");
            }
            arr.append("</array>");
            inject += arr.toString();
        }

        String backgroundModesStr = request.getArg("ios.background_modes", null);
        if (includePush || "true".equals(request.getArg("ios.delayPushCompletion", "false")) ||
                "true".equals(request.getArg("delayPushCompletion", "false"))) {
            if (backgroundModesStr == null || !backgroundModesStr.contains("remote-notification")) {
                if (backgroundModesStr == null) {
                    backgroundModesStr = "";
                } else {
                    backgroundModesStr += ",";
                }
                backgroundModesStr += "remote-notification";
            }
        }

        // Constraint-aware background work / BackgroundTask map to BGTaskScheduler. The
        // permitted identifiers are declared via ios.backgroundProcessingIds (comma list,
        // default <packageName>.processing). Their presence implies the "processing"
        // background mode.
        String backgroundProcessingIds = request.getArg("ios.backgroundProcessingIds", null);
        if (backgroundProcessingIds == null && "true".equals(request.getArg("ios.usesBackgroundProcessing", "false"))) {
            backgroundProcessingIds = request.getPackageName() + ".processing";
        }
        if (backgroundProcessingIds != null && backgroundProcessingIds.trim().length() > 0) {
            if (backgroundModesStr == null || !backgroundModesStr.contains("processing")) {
                backgroundModesStr = (backgroundModesStr == null || backgroundModesStr.trim().length() == 0)
                        ? "processing" : backgroundModesStr + ",processing";
            }
        }

        if (backgroundModesStr != null) {
            String[] backgroundModes = backgroundModesStr.split(",");
            if (!inject.contains("UIBackgroundModes")) {
                inject += "\n<key>UIBackgroundModes</key><array>";
                for (String mode : backgroundModes) {
                    if (mode.trim().isEmpty()) {
                        continue;
                    }
                    if (mode.trim().equals("music")) {
                        mode = "audio";
                    }
                    inject += "<string>"+mode.trim()+"</string>\n";
                }
                inject += "</array>";
            } else {
                throw new IOException("You cannot use both ios.background_modes build hint and use UIBackgroundModes in the ios.plistInject build hint.  Choose one or the other");

            }
        }

        // BGTaskScheduler permitted identifiers (iOS 13+). Required or iOS throws when the
        // app registers/submits a background processing task.
        if (backgroundProcessingIds != null && backgroundProcessingIds.trim().length() > 0
                && !inject.contains("BGTaskSchedulerPermittedIdentifiers")) {
            inject += "\n<key>BGTaskSchedulerPermittedIdentifiers</key><array>";
            for (String id : backgroundProcessingIds.split(",")) {
                if (id.trim().length() > 0) {
                    inject += "<string>" + id.trim() + "</string>";
                }
            }
            inject += "</array>";
        }

        // Receive-shared-content: the host app reads the shared payload from this App Group
        // suite (written by the share extension). See ios.shareAppGroup build hint.
        String shareAppGroup = request.getArg("ios.shareAppGroup", null);
        if (shareAppGroup != null && shareAppGroup.trim().length() > 0 && !inject.contains("CN1ShareAppGroup")) {
            inject += "\n<key>CN1ShareAppGroup</key><string>" + shareAppGroup.trim() + "</string>";
        }

        // Wallet issuer-provisioning: com.codename1.payment.WalletExtension reads this App Group
        // suite to publish pass entries for the generated Wallet extensions. See ios.wallet.* hints.
        String walletAppGroup = request.getArg("ios.wallet.appGroup", null);
        if ("true".equals(request.getArg("ios.wallet.extension", "false"))
                && walletAppGroup != null && walletAppGroup.trim().length() > 0 && !inject.contains("CN1WalletAppGroup")) {
            inject += "\n<key>CN1WalletAppGroup</key><string>" + walletAppGroup.trim() + "</string>";
        }

        // App intents: iOS only offers an activity whose type the app declared here, so without
        // these keys donation appears to succeed while nothing is ever suggested and a Spotlight
        // result cannot continue into the app.
        //
        // Emitted for any app that declares an intent. It has nothing to do with the widget
        // extension, and nesting it in that branch made it depend on an unrelated feature being
        // switched on.
        // Emitted whenever the app declares intents, including the appIntents=false opt-out:
        // donation still runs there, and iOS only offers an activity whose type is declared
        // here, so omitting it would make the opt-out donate into a void.
        if (declaresAppIntents || appIntentsSuppressed) {
            // Each key is decided on its own. Treating any existing NSUserActivityTypes as
            // complete configuration meant an app that already declared one Handoff activity
            // through ios.plistInject silently lost every intent id -- and lost
            // CoreSpotlightContinuation too, which is a different key entirely, so a Spotlight
            // result could not continue into the app either.
            if (!inject.contains("NSUserActivityTypes")) {
                inject += userActivityTypesKey(intentsManifest);
            } else {
                // Merge into the array the application supplied rather than replacing it: its
                // own activity types have to keep working. Appended just before the closing
                // </array> of that key, and only ids it does not already list.
                inject = mergeUserActivityTypes(inject, intentsManifest);
            }
        }
        // CoreSpotlightContinuation is about Spotlight, not about App Intents, and gating it on
        // a declaration made an entire supported configuration silently useless: an app that
        // only calls Intents.index() declares no intent at all -- parseIntentsManifest treats a
        // missing manifest as a warning precisely so that app builds -- and the native bridge
        // still publishes its searchable items. Without this key a tap on one of those results
        // cannot continue into the app, so nativeSpotlightItemSelected is never reached and the
        // content is findable but dead. Emitted for anything that uses the indexing API.
        inject = withSpotlightContinuation(inject, usesIntents);

        // External surfaces: the Java bridge (IOSSurfaceBridge via IOSNative.m) resolves the
        // shared App Group container through this key; the CN1Widgets extension carries its own
        // copy in its generated Info.plist. See surfaces.json / the ios.surfaces.* build hints.
        if (surfacesExtensionEnabled || surfacesWatchEnabled) {
            if (!inject.contains("CN1SurfacesAppGroup")) {
                inject += "\n<key>CN1SurfacesAppGroup</key><string>" + surfacesAppGroup + "</string>";
            }
            // Which kinds are worth mirroring to the watch, decided here rather than at runtime.
            // The phone cannot write into the watch's App Group container -- the same identifier
            // resolves to a directory of its own there -- so a phone-side publish only reaches a
            // complication if the descriptor travels over WCSession, and that is budgeted. Naming
            // the kinds means a publish of a phone-only kind costs one dictionary lookup instead.
            if (surfacesWatchEnabled && !inject.contains("CN1SurfacesWatchKinds")) {
                StringBuilder watchKinds = new StringBuilder();
                for (IOSWidgetExtensionBuilder.Kind kind : surfacesKinds) {
                    if (IOSWidgetExtensionBuilder.hasWatchFamily(kind)) {
                        if (watchKinds.length() > 0) {
                            watchKinds.append(",");
                        }
                        watchKinds.append(kind.getId());
                    }
                }
                inject += "\n<key>CN1SurfacesWatchKinds</key><string>" + watchKinds + "</string>";
            }
            // The extension's deployment target: the runtime gates areWidgetsSupported() on it
            // (the extension cannot run or appear in the widget gallery below this version, so
            // WidgetKit's own iOS 14 floor is not the right check).
            // The PHONE's floor, always -- the watch bundle carries its own, lower one, written
            // by WatchNativeBuilder. Both are compared against the OS actually running, so one
            // shared value would be wrong on one of the two.
            if (surfacesExtensionEnabled && !inject.contains("CN1SurfacesMinOS")) {
                inject += "\n<key>CN1SurfacesMinOS</key><string>"
                        + request.getArg("ios.surfaces.deploymentTarget", "16.1") + "</string>";
            }
            // NSSupportsLiveActivities belongs in the HOST app's Info.plist (the extension only
            // renders them). Intentionally skipped when ios.surfaces.extension=false: without
            // the extension ActivityKit would accept the request but show nothing.
            if (surfacesLiveActivities && !inject.contains("NSSupportsLiveActivities")) {
                inject += "\n<key>NSSupportsLiveActivities</key><true/>";
                if ("true".equals(request.getArg("ios.surfaces.frequentUpdates", "false"))) {
                    inject += "\n<key>NSSupportsLiveActivitiesFrequentUpdates</key><true/>";
                }
            }
            // Widget taps deep link back through <scheme>:// (handled by the app delegate and
            // never stored in AppArg). Register the scheme by appending to ios.urlSchemes so it
            // rides the existing CFBundleURLTypes injection below, whichever branch runs.
            //
            // BOTH the app's own cn1surface.<bundle id> -- which is what the widget generates
            // now, and the only one the watch registers -- and the bare cn1surface, which this
            // app has always registered and which something may still hold a link built with.
            // Keeping the bare one on the phone costs nothing; dropping it could break a link
            // that works today.
            String ownScheme = IOSWidgetExtensionBuilder.surfaceScheme(request.getPackageName());
            String urlSchemes = request.getArg("ios.urlSchemes",
                    request.getArg("ios.urlScheme", ""));
            String added = "";
            if (!inject.contains(ownScheme) && !urlSchemes.contains(ownScheme)) {
                added += "<string>" + ownScheme + "</string>";
            }
            // Matched as a whole element, because the qualified scheme CONTAINS the bare one as
            // a prefix -- a substring test would read the qualified registration as the bare one
            // already being present.
            if (!inject.contains("<string>cn1surface</string>")
                    && !urlSchemes.contains("<string>cn1surface</string>")) {
                added += "<string>cn1surface</string>";
            }
            if (added.length() > 0) {
                request.putArgument("ios.urlSchemes", urlSchemes + added);
            }
        }

        BufferedReader infoReader = new BufferedReader(new InputStreamReader(
                Files.newInputStream(infoPlist.toPath()), StandardCharsets.UTF_8));
        StringBuilder b = new StringBuilder();
        String line = infoReader.readLine();
        while(line != null) {
            // here we inject everything we need
            if(line.indexOf("</dict>") > -1) {
                if(inject != null) {
                    b.append(inject);
                }
                if(facebook != null || includeGooglePlus || (googleServicePlist != null && googleServicePlist.useSignIn)) {
                    if (facebook != null) {
                        b.append("	<key>FacebookAppID</key>\n");
                        b.append("	<string>");
                        b.append(facebook);
                        b.append("</string>");

                        b.append("	<key>FacebookDisplayName</key>\n");
                        b.append("	<string>");
                        b.append(request.getDisplayName());
                        b.append("</string>");
                        if (!inject.contains("LSApplicationQueriesSchemes")) {
                            b.append("<key>LSApplicationQueriesSchemes</key>\n" +
                                "	<array>\n" +
                                "		<string>fbauth2</string>\n<string>gplus</string>" +
                                "	</array>");
                        }
                        
                    }

                    b.append("	<key>CFBundleURLTypes</key>\n");
                    b.append("	<array><dict>");
                    b.append("	    <key>CFBundleURLSchemes</key>\n");
                    b.append("	    <array>");
                    if (facebook != null) {
                        b.append("<string>fb");
                        b.append(facebook);
                        b.append("</string>");
                    }
                    b.append(request.getArg("ios.urlSchemes", request.getArg("ios.urlScheme", "")));
                    b.append("</array>\n");
                    b.append("</dict>");
                    if (includeGooglePlus) {
                        b.append("<dict>\n" +
    "			<key>CFBundleTypeRole</key>\n" +
    "			<string>Editor</string>\n" +
    "			<key>CFBundleURLName</key>\n" +
    "			<string>"+request.getPackageName()+"</string>\n" +
    "			<key>CFBundleURLSchemes</key>\n" +
    "			<array>\n" +
    "				<string>"+request.getPackageName()+"</string>\n" +
    "			</array>\n" +
    "		</dict>");
                    }
                    if (googleServicePlist != null && googleServicePlist.useSignIn) {
                        b.append("<dict>\n" +
    "			<key>CFBundleTypeRole</key>\n" +
    "			<string>Editor</string>\n" +
    "			<key>CFBundleURLName</key>\n" +
    "			<string>"+request.getPackageName()+"</string>\n" +
    "			<key>CFBundleURLSchemes</key>\n" +
    "			<array>\n" +
    "				<string>"+request.getPackageName()+"</string>\n" +
    "			</array>\n" +
    "		</dict>");
                        b.append("<dict>\n" +
    "			<key>CFBundleTypeRole</key>\n" +
    "			<string>Editor</string>\n" +
    "			<key>CFBundleURLName</key>\n" +
    "			<string>"+request.getPackageName()+"</string>\n" +
    "			<key>CFBundleURLSchemes</key>\n" +
    "			<array>\n" +
    "				<string>"+googleServicePlist.reverseClientId+"</string>\n" +
    "			</array>\n" +
    "		</dict>");
                    } else if (googleClientId != null) {
                        
                            b.append("<dict>\n" +
        "			<key>CFBundleTypeRole</key>\n" +
        "			<string>Editor</string>\n" +
        "			<key>CFBundleURLName</key>\n" +
        "			<string>"+request.getPackageName()+"</string>\n" +
        "			<key>CFBundleURLSchemes</key>\n" +
        "			<array>\n" +
        "				<string>"+createReverseGoogleClientId(googleClientId)+"</string>\n" +
        "			</array>\n" +
        "		</dict>");
                    }
                    b.append("</array>\n");
                } else {
                    String scheme = request.getArg("ios.urlSchemes", request.getArg("ios.urlScheme", null));
                    if(scheme != null && scheme.length() > 0) {
                        b.append("	<key>CFBundleURLTypes</key>\n");
                        b.append("	<array><dict><key>CFBundleURLSchemes</key><array>");
                        b.append(request.getArg("ios.urlSchemes", request.getArg("ios.urlScheme", "")));
                        b.append("</array></dict></array>\n");
                    }
                }
                if(statusBarWhite != null) {
                    b.append("	<key>UIViewControllerBasedStatusBarAppearance</key>\n");
                    b.append("	<false/>\n");
                    b.append("	<key>UIStatusBarStyle</key>");
                    b.append("	<string>");
                    b.append(statusBarWhite);
                    b.append("</string>");
                }
                if(fontFiles != null && fontFiles.length > 0) {
                    b.append("    <key>UIAppFonts</key>\n    <array>\n");
                    for(File f : fontFiles) {
                        // Escaped: a font name is an arbitrary file name, and an
                        // XML metacharacter in it (e.g. "A&B.ttf") would produce
                        // a malformed Info.plist and fail the Xcode build.
                        b.append("        <string>");
                        b.append(plistEscape(f.getName()));
                        b.append("</string>\n");
                    }
                    b.append("    </array>\n");
                }
            }
            b.append(line);
            b.append('\n');
            line = infoReader.readLine();
        }
        infoReader.close();
        
        try(FileOutputStream fo = new FileOutputStream(infoPlist)) {
            fo.write(b.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 
     * @param xcodeBuild path to xcodebuild executable
     * @return The version of xcode.  This will only return an integer.  E.g. 7 for Xcode 7
     * @throws Exception 
     */
    private int getXcodeVersion(String xcodeBuild) {
        try {
            String result = execString(new File("."), xcodeBuild, "-version");
            debug("Result is "+result);

            Scanner scanner = new Scanner(result);
            scanner.useDelimiter("\n");
            while (scanner.hasNext()) {
                String line = scanner.next();
                if (line.startsWith("Xcode")) {
                    log("Xcode version line matching pattern: "+line);
                    String[] parts = line.split(" ");
                    if (parts.length < 2) {
                        log("Xcode version line did not contain version number.");
                    }
                    if (parts[1].indexOf(".") >= 0) {
                        parts[1] = parts[1].substring(0, parts[1].indexOf(".")).trim();
                    }
                    return Integer.parseInt(parts[1]);
                }
            }
        } catch (Exception ex) {
            log("Problem getting Xcode version: "+ex.getMessage());
            ex.printStackTrace();
            return -1;
        }
        log("Did not find any lines in Xcode version that matched the patterns we were looking for.  Returning version -1");
        return -1;
    }

    private int getMajorVersionInt(String versionStr, int defaultVal) {
        int pos;
        if ((pos = versionStr.indexOf(".")) != -1) {
            try {
                return Integer.parseInt(versionStr.substring(0, pos));
            } catch (Throwable ex){}
        } else {
            try {
                return Integer.parseInt(versionStr);
            } catch (Throwable ex){}
        }
        return defaultVal;
    }

    private String[] getStubCompileSourceTarget(String javacPath) {
        String source = "1.6";
        String target = "1.6";
        int major = -1;
        String version = null;
        try {
            String versionOutput = execString(tmpFile != null ? tmpFile : new File("."), javacPath, "-version");
            if (versionOutput != null && versionOutput.trim().length() > 0) {
                String[] parts = versionOutput.trim().split("\\s+");
                version = parts[parts.length - 1];
                major = getMajorVersionInt(version, -1);
            }
        } catch (Exception ex) {
            debug("Failed to resolve javac version for iOS stub compile: " + ex.getMessage());
        }
        if (major < 0) {
            version = System.getProperty("java.version");
            major = getMajorVersionInt(version, -1);
        }
        if (major >= 9) {
            source = "8";
            target = "8";
            log("JDK " + version + " does not support -source/-target 1.6. Compiling iOS stubs with -source/-target 8.");
        }
        return new String[]{source, target};
    }

    private String resolveMetalColorSpaceDefine(String hint) {
        String value = hint == null ? "sRGB" : hint.trim();
        if (value.length() == 0) {
            value = "sRGB";
        }
        String key = value.toLowerCase().replace("-", "").replace("_", "");
        if (key.equals("srgb")) {
            return "#define CN1_METAL_COLORSPACE_SRGB";
        }
        if (key.equals("displayp3") || key.equals("p3")) {
            return "#define CN1_METAL_COLORSPACE_DISPLAY_P3";
        }
        if (key.equals("devicergb") || key.equals("device")) {
            return "#define CN1_METAL_COLORSPACE_DEVICE_RGB";
        }
        if (key.equals("linearsrgb") || key.equals("linear")) {
            return "#define CN1_METAL_COLORSPACE_LINEAR_SRGB";
        }
        if (key.equals("extendedsrgb")) {
            return "#define CN1_METAL_COLORSPACE_EXTENDED_SRGB";
        }
        if (key.equals("extendedlinearsrgb")) {
            return "#define CN1_METAL_COLORSPACE_EXTENDED_LINEAR_SRGB";
        }
        if (key.equals("none") || key.equals("default") || key.equals("system")) {
            return "#define CN1_METAL_COLORSPACE_NONE";
        }
        log("Unknown ios.metal.colorSpace value: " + hint + " - falling back to sRGB");
        return "#define CN1_METAL_COLORSPACE_SRGB";
    }

    private String resolveXcodebuild() {
        String explicitXcodebuild = System.getenv("XCODEBUILD");
        if (explicitXcodebuild != null && explicitXcodebuild.length() > 0) {
            File candidate = new File(explicitXcodebuild);
            if (candidate.exists()) {
                log("Using xcodebuild from XCODEBUILD: " + candidate.getAbsolutePath());
                return candidate.getAbsolutePath();
            }
        }

        String developerDir = System.getenv("DEVELOPER_DIR");
        if (developerDir != null && developerDir.length() > 0) {
            File candidate = new File(developerDir, "usr/bin/xcodebuild");
            if (candidate.exists()) {
                log("Using xcodebuild from DEVELOPER_DIR: " + candidate.getAbsolutePath());
                return candidate.getAbsolutePath();
            }
        }

        String xcodeApp = System.getenv("XCODE_APP");
        if (xcodeApp != null && xcodeApp.length() > 0) {
            File candidate = new File(xcodeApp, "Contents/Developer/usr/bin/xcodebuild");
            if (candidate.exists()) {
                log("Using xcodebuild from XCODE_APP: " + candidate.getAbsolutePath());
                return candidate.getAbsolutePath();
            }
        }

        File xcrun = new File("/usr/bin/xcrun");
        if (xcrun.exists()) {
            try {
                String resolved = execString(tmpFile, xcrun.getAbsolutePath(), "-f", "xcodebuild");
                if (resolved != null) {
                    resolved = resolved.trim();
                }
                if (resolved != null && resolved.length() > 0) {
                    log("Using xcodebuild resolved by xcrun: " + resolved);
                    return resolved;
                }
            } catch (Exception ex) {
                debug("xcrun failed to resolve xcodebuild: " + ex.getMessage());
            }
        }

        File usrBin = new File("/usr/bin/xcodebuild");
        if (usrBin.exists()) {
            log("Using xcodebuild at /usr/bin/xcodebuild");
            return usrBin.getAbsolutePath();
        }

        log("Using xcodebuild from PATH");
        return "xcodebuild";
    }
    

    
    private static Element getNextElement(NodeList l, int currPos) {
        int len = l.getLength();
        Element nextEl = null;
        for (int j=currPos+1; j<len; j++) {
            Node nextN = l.item(j);
            if (nextN instanceof Element) {
                nextEl = (Element)nextN;
                break;
            }
        }
        return nextEl;
    }

    
    private File getIconDirectory(BuildRequest request) {

        File iconDirectory = new File(tmpFile, "dist/" + request.getMainClass() + "-src/Images.xcassets/AppIcon.appiconset");
        if (!iconDirectory.exists()) {
            iconDirectory.mkdirs();
        }
        return iconDirectory;

    }
    
    private void copyIcon(String name, File srcDir, File destDir) throws IOException {
        copy(new File(srcDir, name), new File(destDir, name));
    }

    private String buildLocalizedIconSelectorObjC() {
        StringBuilder mapping = new StringBuilder();
        mapping.append("        @{ ");
        boolean first = true;
        for (Map.Entry<String, String> entry : localizedIcons.entrySet()) {
            if (!first) {
                mapping.append(", ");
            }
            first = false;
            mapping.append("@\"").append(entry.getValue()).append("\": @\"").append(entry.getKey()).append("\"");
        }
        mapping.append(" }");
        // Wait for UIApplicationDidBecomeActiveNotification before calling
        // -[UIApplication setAlternateIconName:]. Calling from didFinishLaunching --
        // even via dispatch_async on the main queue -- routinely fails with
        // NSCocoaErrorDomain Code=3072 ("operation was cancelled") because the
        // system alert iOS shows for an icon change has no active foreground scene
        // to anchor to yet. Deferring to the active state fixes the silent failure
        // that the user reported on top of #4870. The completion handler is wired
        // up so any remaining bundle-configuration problem surfaces in the device
        // log instead of being swallowed (the original nil handler hid this).
        return "\n    // Codename One localized app icon selection\n"
                + "    if ([[UIApplication sharedApplication] respondsToSelector:@selector(setAlternateIconName:completionHandler:)]) {\n"
                + "        NSDictionary *cn1LocalizedIcons =\n"
                + mapping + ";\n"
                + "        void (^cn1ApplyIcon)(void) = ^{\n"
                + "            NSString *cn1CurrentIcon = [[UIApplication sharedApplication] alternateIconName];\n"
                + "            NSString *cn1TargetIcon = nil;\n"
                + "            NSArray *cn1PrefLangs = [NSLocale preferredLanguages];\n"
                + "            if (cn1PrefLangs.count > 0) {\n"
                + "                NSString *cn1PrefLang = [cn1PrefLangs objectAtIndex:0];\n"
                + "                NSArray *cn1LangParts = [cn1PrefLang componentsSeparatedByCharactersInSet:\n"
                + "                    [NSCharacterSet characterSetWithCharactersInString:@\"-_\"]];\n"
                + "                NSString *cn1Lang = [[cn1LangParts objectAtIndex:0] lowercaseString];\n"
                + "                // The device region (Settings > General > Region) takes precedence\n"
                + "                // over the region embedded in the language variant (e.g. en-GB), so\n"
                + "                // a UAE-region user with English (UK) still gets cn1_icon_en_AE.\n"
                + "                NSString *cn1DeviceRegion = [[NSLocale currentLocale] objectForKey:NSLocaleCountryCode];\n"
                + "                if (cn1DeviceRegion.length == 2) {\n"
                + "                    NSString *cn1Key = [NSString stringWithFormat:@\"%@_%@\",\n"
                + "                        cn1Lang, [cn1DeviceRegion uppercaseString]];\n"
                + "                    cn1TargetIcon = [cn1LocalizedIcons objectForKey:cn1Key];\n"
                + "                }\n"
                + "                if (cn1TargetIcon == nil && cn1LangParts.count >= 2) {\n"
                + "                    // lastObject skips script subtags such as the Hans in zh-Hans-CN\n"
                + "                    NSString *cn1LangRegion = [cn1LangParts lastObject];\n"
                + "                    if (cn1LangRegion.length == 2) {\n"
                + "                        NSString *cn1Key = [NSString stringWithFormat:@\"%@_%@\",\n"
                + "                            cn1Lang, [cn1LangRegion uppercaseString]];\n"
                + "                        cn1TargetIcon = [cn1LocalizedIcons objectForKey:cn1Key];\n"
                + "                    }\n"
                + "                }\n"
                + "                if (cn1TargetIcon == nil) {\n"
                + "                    cn1TargetIcon = [cn1LocalizedIcons objectForKey:cn1Lang];\n"
                + "                }\n"
                + "            }\n"
                + "            BOOL cn1NeedsUpdate = (cn1TargetIcon == nil && cn1CurrentIcon != nil)\n"
                + "                || (cn1TargetIcon != nil && ![cn1TargetIcon isEqualToString:cn1CurrentIcon]);\n"
                + "            if (!cn1NeedsUpdate) {\n"
                + "                return;\n"
                + "            }\n"
                + "            NSString *cn1FinalTarget = cn1TargetIcon;\n"
                + "            [[UIApplication sharedApplication] setAlternateIconName:cn1FinalTarget completionHandler:^(NSError * _Nullable cn1IconErr) {\n"
                + "                if (cn1IconErr != nil) {\n"
                + "                    NSLog(@\"[CodenameOne] Failed to set alternate app icon '%@': %@\", cn1FinalTarget ?: @\"(primary)\", cn1IconErr);\n"
                + "                } else {\n"
                + "                    NSLog(@\"[CodenameOne] Set alternate app icon to '%@'\", cn1FinalTarget ?: @\"(primary)\");\n"
                + "                }\n"
                + "            }];\n"
                + "        };\n"
                + "        if ([UIApplication sharedApplication].applicationState == UIApplicationStateActive) {\n"
                + "            dispatch_async(dispatch_get_main_queue(), cn1ApplyIcon);\n"
                + "        } else {\n"
                + "            __block id cn1ActiveObs = nil;\n"
                + "            cn1ActiveObs = [[NSNotificationCenter defaultCenter]\n"
                + "                addObserverForName:UIApplicationDidBecomeActiveNotification object:nil queue:[NSOperationQueue mainQueue]\n"
                + "                usingBlock:^(NSNotification *cn1Note) {\n"
                + "                    if (cn1ActiveObs != nil) {\n"
                + "                        [[NSNotificationCenter defaultCenter] removeObserver:cn1ActiveObs];\n"
                + "                        cn1ActiveObs = nil;\n"
                + "                    }\n"
                + "                    cn1ApplyIcon();\n"
                + "                }];\n"
                + "        }\n"
                + "    }\n";
    }
    
    private void copyIcons(File srcDir, File destDir, String... icons) throws IOException {
        for (String icon : icons) {
            copyIcon(icon, srcDir, destDir);
        }
    }
    
    private boolean generateIcons(BuildRequest request) throws Exception {

        File iconDirectory = getIconDirectory(request);
        File resDir = getResDir();
        
        BufferedImage iconImage = ImageIO.read(new ByteArrayInputStream(request.getIcon()));
        // Legacy iOS icon files are still copied into the root resources, but should not
        // be placed inside AppIcon.appiconset as they are not referenced by Contents.json.
        icon512 = new File(resDir, "iTunesArtwork");
        createFile(icon512, request.getIcon());
        icon57 = new File(resDir, "Icon.png");
        createIconFile(icon57, iconImage, 57, 57);
        createIconFile(new File(iconDirectory, "iPhoneNotification@2x.png"), iconImage, 40, 40);
        createIconFile(new File(iconDirectory, "iPhoneNotification@3x.png"), iconImage, 60, 60);
        createIconFile(new File(iconDirectory, "iPhoneSpotlight.png"), iconImage, 29, 29);
        createIconFile(new File(iconDirectory, "iPhoneSpotlight@2x.png"), iconImage, 58, 58);
        createIconFile(new File(iconDirectory, "iPhoneSpotlight@3x.png"), iconImage, 87, 87);
        createIconFile(new File(iconDirectory, "iPhone7Spotlight@2x.png"), iconImage, 80, 80);
        createIconFile(new File(iconDirectory, "iPhone7Spotlight@3x.png"), iconImage, 120, 120);
        createIconFile(new File(iconDirectory, "iPhoneApp.png"), iconImage, 57, 57);
        createIconFile(new File(iconDirectory, "iPhoneApp@2x.png"), iconImage, 114, 114);
        createIconFile(new File(iconDirectory, "iPhone7App@2x.png"), iconImage, 120, 120);
        createIconFile(new File(iconDirectory, "iPhone7App@3x.png"), iconImage, 180, 180);
        createIconFile(new File(iconDirectory, "iPadNotifications.png"), iconImage, 20, 20);
        createIconFile(new File(iconDirectory, "iPadNotification@2x.png"), iconImage, 40, 40);
        createIconFile(new File(iconDirectory, "iPadSettings.png"), iconImage, 29, 29);
        createIconFile(new File(iconDirectory, "iPadSettings@2x.png"), iconImage, 58, 58);
        createIconFile(new File(iconDirectory, "iPadSpotlight7.png"), iconImage, 40, 40);
        createIconFile(new File(iconDirectory, "iPadSpotlight7@2x.png"), iconImage, 80, 80);
        createIconFile(new File(iconDirectory, "iPadSpotlight.png"), iconImage, 50, 50);
        createIconFile(new File(iconDirectory, "iPadSpotlight@2x.png"), iconImage, 100, 100);
        createIconFile(new File(iconDirectory, "iPadApp.png"), iconImage, 72, 72);
        createIconFile(new File(iconDirectory, "iPadApp@2x.png"), iconImage, 144, 144);
        createIconFile(new File(iconDirectory, "iPadApp7.png"), iconImage, 76, 76);
        createIconFile(new File(iconDirectory, "iPadApp7@2x.png"), iconImage, 152, 152);
        createIconFile(new File(iconDirectory, "iPadPro@2x.png"), iconImage, 167, 167);
        createIconFile(new File(iconDirectory, "AppStore.png"), iconImage, 1024, 1024);
        
        copyIcons(iconDirectory, resDir,
                "iPhoneNotification@2x.png",
                "iPhoneNotification@3x.png",
                "iPhoneSpotlight.png",
                "iPhoneSpotlight@2x.png",
                "iPhoneSpotlight@3x.png",
                "iPhone7Spotlight@2x.png",
                "iPhone7Spotlight@3x.png",
                "iPhoneApp.png",
                "iPhoneApp@2x.png",
                "iPhone7App@2x.png",
                "iPhone7App@3x.png",
                "iPadNotifications.png",
                "iPadNotification@2x.png",
                "iPadSettings.png",
                "iPadSettings@2x.png",
                "iPadSpotlight7.png",
                "iPadSpotlight7@2x.png",
                "iPadSpotlight.png",
                "iPadSpotlight@2x.png",
                "iPadApp.png",
                "iPadApp@2x.png",
                "iPadApp7.png",
                "iPadApp7@2x.png",
                "iPadPro@2x.png",
                "AppStore.png");

        processLocalizedIcons(resDir, request);

        return true;
    }

    /**
     * Scans the resources directory for files named cn1_icon_LANG[_COUNTRY].png
     * and registers them as iOS alternate app icons. Each detected locale is
     * written as its own AppIcon_LOC.appiconset inside Images.xcassets so that
     * actool produces a coherent CFBundleIcons / CFBundleAlternateIcons
     * partial Info.plist; mixing manual CFBundleIcons entries in the user
     * Info.plist with an asset-catalog-managed primary icon is unsafe because
     * actool's partial plist replaces ours during the merge. The matching
     * ASSETCATALOG_COMPILER_ALTERNATE_APPICON_NAMES build setting is added in
     * {@link #addLocalizedIconsBuildSetting}; runtime icon selection is wired
     * up in the GL app delegate.
     */
    private void processLocalizedIcons(File resDir, BuildRequest request) throws IOException {
        File[] candidates = resDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                String lower = name.toLowerCase();
                return lower.startsWith("cn1_icon_") && lower.endsWith(".png");
            }
        });
        if (candidates == null || candidates.length == 0) {
            return;
        }
        File assetCatalogDir = new File(tmpFile, "dist/" + request.getMainClass()
                + "-src/Images.xcassets");
        for (File candidate : candidates) {
            String base = candidate.getName();
            // strip prefix/suffix
            String core = base.substring("cn1_icon_".length(), base.length() - ".png".length());
            String[] parts = core.split("_");
            if (parts.length < 1 || parts[0].length() != 2) {
                log("Ignoring localized icon with unsupported name: " + base
                        + ". Expected cn1_icon_<lang>[_<country>].png");
                continue;
            }
            String lang = parts[0].toLowerCase();
            String country = parts.length >= 2 && parts[1].length() == 2 ? parts[1].toUpperCase() : null;
            String localeKey = country != null ? lang + "_" + country : lang;
            String iconName = "AppIcon_" + localeKey;

            BufferedImage img;
            try {
                img = ImageIO.read(candidate);
            } catch (IOException ex) {
                log("Failed to read localized icon " + base + ": " + ex.getMessage());
                candidate.delete();
                continue;
            }
            if (img == null) {
                log("Localized icon " + base + " is not a valid PNG image. Skipping.");
                candidate.delete();
                continue;
            }

            File alternateIconset = new File(assetCatalogDir, iconName + ".appiconset");
            if (!alternateIconset.exists() && !alternateIconset.mkdirs()) {
                log("Failed to create alternate icon set directory: "
                        + alternateIconset.getAbsolutePath());
                candidate.delete();
                continue;
            }
            String iphone2x = iconName + "60x60@2x.png";
            String iphone3x = iconName + "60x60@3x.png";
            String ipad2x = iconName + "76x76@2x~ipad.png";
            String ipadPro2x = iconName + "83.5x83.5@2x~ipad.png";
            createIconFile(new File(alternateIconset, iphone2x), img, 120, 120);
            createIconFile(new File(alternateIconset, iphone3x), img, 180, 180);
            createIconFile(new File(alternateIconset, ipad2x), img, 152, 152);
            createIconFile(new File(alternateIconset, ipadPro2x), img, 167, 167);
            writeAlternateAppIconContentsJson(new File(alternateIconset, "Contents.json"),
                    iphone2x, iphone3x, ipad2x, ipadPro2x);
            localizedIcons.put(iconName, localeKey);
            // Remove the original so it isn't bundled as a stray resource.
            candidate.delete();
            log("Registered localized app icon '" + iconName + "' for locale " + localeKey);
        }
    }

    private void writeAlternateAppIconContentsJson(File contentsJson,
            String iphone2x, String iphone3x, String ipad2x, String ipadPro2x) throws IOException {
        String json = "{\n"
                + "  \"images\" : [\n"
                + "    {\n"
                + "      \"size\" : \"60x60\",\n"
                + "      \"idiom\" : \"iphone\",\n"
                + "      \"filename\" : \"" + iphone2x + "\",\n"
                + "      \"scale\" : \"2x\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"size\" : \"60x60\",\n"
                + "      \"idiom\" : \"iphone\",\n"
                + "      \"filename\" : \"" + iphone3x + "\",\n"
                + "      \"scale\" : \"3x\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"size\" : \"76x76\",\n"
                + "      \"idiom\" : \"ipad\",\n"
                + "      \"filename\" : \"" + ipad2x + "\",\n"
                + "      \"scale\" : \"2x\"\n"
                + "    },\n"
                + "    {\n"
                + "      \"size\" : \"83.5x83.5\",\n"
                + "      \"idiom\" : \"ipad\",\n"
                + "      \"filename\" : \"" + ipadPro2x + "\",\n"
                + "      \"scale\" : \"2x\"\n"
                + "    }\n"
                + "  ],\n"
                + "  \"info\" : {\n"
                + "    \"version\" : 1,\n"
                + "    \"author\" : \"xcode\"\n"
                + "  }\n"
                + "}\n";
        try (Writer w = new OutputStreamWriter(Files.newOutputStream(contentsJson.toPath()),
                StandardCharsets.UTF_8)) {
            w.write(json);
        }
    }

    // Apple Team IDs are 10-character alphanumeric strings (e.g. "2K4UGY23XQ").
    // Anything outside that range is rejected to avoid corrupting the pbxproj.
    private static final Pattern TEAM_ID_PATTERN = Pattern.compile("[A-Za-z0-9]+");

    private void injectDevelopmentTeam(File pbx, String debugTeam, String releaseTeam) throws IOException {
        debugTeam = sanitizeTeamId(debugTeam, "ios.debug.teamId");
        releaseTeam = sanitizeTeamId(releaseTeam, "ios.release.teamId");
        if (debugTeam.isEmpty() && releaseTeam.isEmpty()) {
            return;
        }
        String contents = readFileToString(pbx);
        // Anchor on each XCBuildConfiguration's "buildSettings { ... }; name = Debug|Release;"
        // boundary so we can route the right value per configuration when ios.debug.teamId
        // and ios.release.teamId differ. DEVELOPMENT_TEAM is injected at the project level
        // (the configurations that declare SDKROOT) so it inherits to every target.
        Pattern blockPattern = Pattern.compile(
                "buildSettings = \\{.*?\\};\\s*name = (Debug|Release);",
                Pattern.DOTALL);
        Matcher m = blockPattern.matcher(contents);
        StringBuffer out = new StringBuffer();
        boolean modified = false;
        while (m.find()) {
            String block = m.group(0);
            String configName = m.group(1);
            String team = "Debug".equals(configName) ? debugTeam : releaseTeam;
            if (team.isEmpty()
                    || block.contains("DEVELOPMENT_TEAM")
                    || !block.contains("SDKROOT = iphoneos;")) {
                m.appendReplacement(out, Matcher.quoteReplacement(block));
                continue;
            }
            String injected = block.replace(
                    "SDKROOT = iphoneos;",
                    "SDKROOT = iphoneos;\n\t\t\t\tDEVELOPMENT_TEAM = " + team + ";");
            m.appendReplacement(out, Matcher.quoteReplacement(injected));
            modified = true;
        }
        m.appendTail(out);
        if (modified) {
            try (Writer w = new OutputStreamWriter(Files.newOutputStream(pbx.toPath()), StandardCharsets.UTF_8)) {
                w.write(out.toString());
            }
        }
    }

    /** Package-private so {@link MacNativeBuilder} can validate its own
     *  {@code macNative.teamId} hint with the same regex as the iOS team. */
    String sanitizeTeamId(String raw, String hint) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (!TEAM_ID_PATTERN.matcher(trimmed).matches()) {
            log("Ignoring " + hint + "='" + raw + "': expected an alphanumeric Apple Team ID");
            return "";
        }
        return trimmed;
    }


    private void addLocalizedIconsBuildSetting(File pbx) throws IOException {
        if (localizedIcons.isEmpty()) {
            return;
        }
        StringBuilder names = new StringBuilder();
        boolean first = true;
        for (String iconName : localizedIcons.keySet()) {
            if (!first) {
                names.append(' ');
            }
            first = false;
            names.append(iconName);
        }
        // actool reads ASSETCATALOG_COMPILER_ALTERNATE_APPICON_NAMES and emits a
        // partial Info.plist with both CFBundlePrimaryIcon and CFBundleAlternateIcons,
        // which is what we need for setAlternateIconName: to resolve at runtime.
        replaceAllInFile(pbx,
                "ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;",
                "ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon;\n"
                        + "\t\t\t\tASSETCATALOG_COMPILER_ALTERNATE_APPICON_NAMES = \""
                        + names + "\";");
    }

    
    private boolean generateLaunchScreen(BuildRequest request) throws Exception {
        File buildinRes = getBuildinRes();
        File resDir = getResDir();
        File iconDirectory = getIconDirectory(request);
        
        
        if (xcodeVersion >= 9) {
            File launchFgImage = new File(resDir, "Launch.Foreground.png");
            if (!launchFgImage.exists()) {
                copy(new File(iconDirectory, "iPadApp7@2x.png"), launchFgImage);
            }
            
            File defaultLaunchStoryBoard = new File(buildinRes, "LaunchScreen-Default.storyboard");
            File launchStoryBoard = new File(buildinRes, "LaunchScreen.storyboard");
            if (!launchStoryBoard.exists()) {
                copy(defaultLaunchStoryBoard, launchStoryBoard);
            }
            defaultLaunchStoryBoard.delete();

            // Xcode 9+ uses LaunchScreen.storyboard. Keeping the legacy launch image set
            // causes asset-catalog warnings for many missing legacy image files.
            File legacyLaunchImages = new File(tmpFile, "dist/" + request.getMainClass() + "-src/Images.xcassets/LaunchImage.launchimage");
            if (legacyLaunchImages.exists()) {
                delTree(legacyLaunchImages);
            }

        }
        return true;
    }

    private void normalizeAssetCatalogs(BuildRequest request) throws IOException {
        File appSrcDir = new File(tmpFile, "dist/" + request.getMainClass() + "-src");
        File appIconContents = new File(appSrcDir, "Images.xcassets/AppIcon.appiconset/Contents.json");
        if (appIconContents.exists()) {
            replaceInFile(appIconContents,
                    ",\n    {\n      \"size\" : \"120x120\",\n      \"idiom\" : \"iphone\",\n      \"filename\" : \"Icon7@2x.png\",\n      \"scale\" : \"1x\"\n    },\n    {\n      \"size\" : \"167x167\",\n      \"idiom\" : \"ipad\",\n      \"filename\" : \"Icon-167.png\",\n      \"scale\" : \"3x\"\n    }",
                    "");
        }

        if (xcodeVersion >= 9) {
            File legacyLaunchImages = new File(appSrcDir, "Images.xcassets/LaunchImage.launchimage");
            if (legacyLaunchImages.exists()) {
                delTree(legacyLaunchImages);
            }
        }

        if (macNativeBuilder.isEnabled()) {
            macNativeBuilder.writeAppIconset(new File(appSrcDir, "Images.xcassets"), icon512);
        }
    }

    
    private static String createReverseGoogleClientId(String clientId) {
        String[] parts = clientId.split("\\.");
        return join(reverse(parts), ".");
    }
    
    private static String[] reverse(String[] input) {
        int len = input.length;
        String[] output = new String[len];
        for (int i=0; i<len; i++) {
            output[i] = input[len-i-1];
        }
        return output;
    }
    
    private static String join(String[] strs, String sep) {
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (String str : strs) {
            if (first) {
                first = false;
            } else {
                out.append(sep);
            }
            out.append(str);
        
        }
        return out.toString();
    }

    private static boolean isVisionAnalyzerClass(String cls) {
        return "com/codename1/ai/vision/TextRecognizer".equals(cls)
                || "com/codename1/ai/vision/BarcodeScanner".equals(cls)
                || "com/codename1/ai/vision/CodeScanner".equals(cls)
                || "com/codename1/ai/vision/FaceDetector".equals(cls)
                || "com/codename1/ai/vision/ImageLabeler".equals(cls)
                || "com/codename1/ai/vision/PoseDetector".equals(cls)
                || "com/codename1/ai/vision/SelfieSegmenter".equals(cls)
                || "com/codename1/ai/vision/DocumentScanner".equals(cls);
    }

    /// The vision classes that drive the camera themselves. An app using one
    /// of these never names {@code com.codename1.camera} directly, so without
    /// this the AVFoundation natives behind the preview would be left out of
    /// the build.
    ///
    /// @param cls internal-form class name seen by the scan
    /// @return whether referencing it means the app opens a camera
    private static boolean isCameraBackedVisionClass(String cls) {
        return "com/codename1/ai/vision/CodeScanner".equals(cls)
                || "com/codename1/ai/vision/VisionCameraView".equals(cls);
    }

    private static boolean isLanguageFeatureClass(String cls) {
        return "com/codename1/ai/language/LanguageIdentifier".equals(cls)
                || "com/codename1/ai/language/Translator".equals(cls)
                || "com/codename1/ai/language/SmartReply".equals(cls);
    }


    /** Locale-independent case-insensitive suffix test. */
    static boolean endsWithIgnoreCase(String value, String suffix) {
        return value != null && value.length() >= suffix.length()
                && value.regionMatches(true, value.length() - suffix.length(), suffix, 0, suffix.length());
    }

    /** Escapes a value for inclusion in a plist/XML text node. */
    static String plistEscape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /// Splits a {@code -Doptional.frameworks=a;b;c} argument into {@code set}, so several products'
    /// lists can be merged into the single argument the translator honours.
    static void collectOptionalFrameworks(java.util.Set<String> set, String arg) {
        if (arg == null) {
            return;
        }
        int eq = arg.indexOf('=');
        String list = eq < 0 ? arg : arg.substring(eq + 1);
        for (String framework : list.split(";")) {
            String trimmed = framework.trim();
            if (trimmed.length() > 0) {
                set.add(trimmed);
            }
        }
    }

    /**
     * Whether a declared app-group list already contains a group, compared as a whole token.
     *
     * <p>The same trap the profile check documents, one layer up: a project already declaring
     * {@code group.com.example.shared} contains the string {@code group.com.example}, so a
     * substring test read the surfaces group as present and left the entitlement out. The
     * container then fails to resolve, {@code areWidgetsSupported()} answers false, and
     * {@code Surfaces.publish()} returns before the bridge -- taking the watch mirror with it, in
     * the watch-only configuration this entitlement was widened for.</p>
     *
     * <p>Split on both separators because the two builders spell the list differently -- comma
     * here, space on the build server -- and a value pasted from one into the other should not
     * change the answer.</p>
     *
     * @param declared the existing ios.app_groups value
     * @param group the group being added
     * @return true when the group is already declared
     */
    static boolean declaresAppGroup(String declared, String group) {
        if (declared == null || group == null || group.length() == 0) {
            return false;
        }
        for (String token : declared.split("[,\\s]+")) {
            if (group.equals(token.trim())) {
                return true;
            }
        }
        return false;
    }

}
