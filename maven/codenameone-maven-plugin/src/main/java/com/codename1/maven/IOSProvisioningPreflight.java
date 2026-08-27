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
package com.codename1.maven;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Reads the {@code .mobileprovision} an iOS device build is about to sign with, and
 * refuses the build locally when signing cannot possibly succeed.
 *
 * <p>Every case here used to be found only by a cloud build server: the profile is
 * uploaded, an ~8GB toolchain runs for minutes, and the failure arrives as an Xcode
 * export error or -- when the file could not be decoded at all -- as a bare XML parser
 * stack trace with no mention of the profile. One real sequence: a build ran four
 * minutes and died with {@code exportArchive Provisioning profile "..." is not an
 * "iOS Ad Hoc" profile}, because the project asked for {@code ad-hoc} while the profile
 * was an App Store one. Both facts were in the file, on disk, before the build was sent.
 *
 * <p>The payload of a {@code .mobileprovision} is a plain-text XML plist inside a CMS
 * envelope, so it can be read here without {@code security}, Xcode, or a Mac.
 *
 * <p>The checks fail only where the outcome is certain -- a missing, empty, unreadable
 * or expired profile, and the unambiguous type mismatches that {@code xcodebuild
 * -exportArchive} rejects. Anything murkier (in-house/enterprise profiles, which Xcode
 * accepts for more than one method) warns instead, because a false refusal here blocks
 * a build that would have worked.
 */
final class IOSProvisioningPreflight {

    /** {@code xcodebuild -exportArchive} distribution methods, as the daemon names them. */
    static final String DEVELOPMENT = "development";
    static final String AD_HOC = "ad-hoc";
    static final String APP_STORE = "app-store";
    static final String ENTERPRISE = "enterprise";

    /** What a profile is, derived from the plist. */
    static class Profile {
        String name;
        String type;
        Date expirationDate;
        /**
         * The {@code application-identifier} entitlement, team prefix and all
         * ({@code ABCD1234.com.example.app}, or {@code ABCD1234.com.example.*} for a
         * wildcard). Null when the profile does not carry one.
         */
        String applicationIdentifier;
        /**
         * The {@code com.apple.security.application-groups} entitlement, or null when the
         * profile carries none. A generated document provider always declares an App Group, so a
         * profile without it cannot sign that target however well its bundle id matches.
         */
        List<String> appGroups;
    }

    /** A problem found before the build was sent: {@code message} is written for the user. */
    static class Problem {
        final String message;
        final boolean fatal;

        Problem(String message, boolean fatal) {
            this.message = message;
            this.fatal = fatal;
        }
    }

    private IOSProvisioningPreflight() {
    }

    /**
     * The distribution method this build will actually export with, resolved exactly as
     * {@code IPhoneBuilder} resolves it on the build server: a per-build-type hint beats
     * the shared hint, which beats the default (development for debug, app-store for
     * release). Reading it any other way would refuse builds the server would have
     * accepted.
     */
    static String effectiveDistributionMethod(Properties settings, boolean release) {
        String method = release ? APP_STORE : DEVELOPMENT;
        method = arg(settings, "ios.distributionMethod", method);
        method = arg(settings, release ? "ios.release.distributionMethod" : "ios.debug.distributionMethod", method);
        // The hint can be written as ${release.method} and passed with -D, exactly like the
        // profile path. Comparing the literal placeholder against a profile's kind refused
        // builds whose method Ant was about to resolve to the matching one.
        return resolvePlaceholders(method, settings);
    }

    private static String arg(Properties settings, String hint, String defaultValue) {
        String value = settings.getProperty("codename1.arg." + hint);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * The build target {@code cn1:buildIosOnDeviceDebug} selects. Its buildxml target submits
     * {@code codename1.ios.debug.provision} and is signed on the build server like any other
     * debug device build, so a bad profile costs a cloud build slot there too.
     */
    static final String IOS_ON_DEVICE_DEBUG = "ios-on-device-debug";

    /**
     * Whether this build is one the pre-flight has any business judging: a cloud iOS build
     * that the build server signs.
     *
     * <p>Excluded: {@code ios-source} (a local Xcode project, signed later or not at all), the
     * simulator, and the native-Mac targets -- those ride {@code platform=ios} with a different
     * signing identity, so the app's iOS profile settings do not describe them.
     */
    static boolean appliesTo(String platform, String buildTarget) {
        return platform != null && platform.contains("ios") && buildTarget != null
                && (buildTarget.startsWith("ios-device") || IOS_ON_DEVICE_DEBUG.equals(buildTarget));
    }

    /**
     * Whether this target signs with the release (distribution) profile. Everything else --
     * {@code ios-device} and {@code ios-on-device-debug} alike -- signs with the debug one.
     */
    static boolean isReleaseTarget(String buildTarget) {
        return buildTarget != null && buildTarget.contains("release");
    }

    /** The profile setting that applies to this build type. */
    static String provisioningProfileSettingKey(boolean release) {
        return release ? "codename1.ios.release.provision" : "codename1.ios.debug.provision";
    }

    /**
     * The checks that depend only on the profile file itself, safe to run before a CN1Lib's
     * properties have been merged in.
     *
     * <p>A library can supply {@code codename1.arg.ios.*.distributionMethod} through its
     * appended/required properties, which the mojo merges later -- so deciding a type mismatch
     * this early would refuse a build whose method the merge was about to make correct. That
     * decision belongs to {@link #check}, run against the merged settings. What a file is,
     * whether it exists, and whether it has expired cannot change in the merge, so those fail
     * here, before the app is packaged.
     *
     * @return the problems with the file; empty when there is nothing to say, including when
     * no profile is configured yet (the merge may still supply one).
     */
    static List<Problem> checkProfileFile(Properties settings, boolean release, Date now) {
        List<Problem> problems = new ArrayList<Problem>();
        String settingKey = provisioningProfileSettingKey(release);
        String path = settings.getProperty(settingKey);
        if (path == null || path.trim().isEmpty()) {
            return problems;
        }
        collectFileProblems(problems, settings, release, now, path, settingKey, false);
        return problems;
    }

    /**
     * @return the problems with the configured profile, in the order they should be
     * reported; empty when there is nothing to say. A fatal problem means the build
     * cannot succeed as configured. Run this against the settings the build will actually
     * be submitted with -- see {@link #checkProfileFile} for why.
     */
    static List<Problem> check(Properties settings, boolean release, Date now) {
        List<Problem> problems = new ArrayList<Problem>();
        String settingKey = provisioningProfileSettingKey(release);
        String path = settings.getProperty(settingKey);
        if (path == null || path.trim().isEmpty()) {
            // Not fatal: a profile can still reach the server another way, and refusing here
            // would invent a failure the build might not have.
            problems.add(new Problem("No provisioning profile is configured for this build ("
                    + settingKey + " is not set). An iOS device build has to be signed, so the "
                    + "build server will reject it unless the profile is supplied another way.", false));
            return problems;
        }
        collectFileProblems(problems, settings, release, now, path, settingKey, true);
        return problems;
    }

    /**
     * Whether every app extension this build embeds can actually be signed.
     *
     * <p>An {@code ios/app_extensions/<Name>/} folder becomes an embedded extension target
     * whose bundle id is {@code <package>.<Name>}, and Apple signs an extension against its
     * OWN App ID: the app's profile covers the app's bundle id and nothing under it. Supply
     * no profile for the extension and the build server has nothing to sign that target with,
     * so it inherits the app's -- and Xcode fails, minutes into a cloud build, with
     * {@code Provisioning profile "X" has app ID "com.example.app", which does not match the
     * bundle ID "com.example.app.MyExtension"}. That message names the target but not the
     * setting that fixes it, and the guide called the profile "optional", so the natural
     * reading was that Codename One had signed the extension wrongly.
     *
     * <p>Decided from files on disk: the app's profile carries the App ID it covers, and a
     * wildcard App ID legitimately covers the extension, so that case passes. Fatal only when
     * the app's profile demonstrably cannot sign the target -- anything this cannot read
     * (an unparseable profile, an unset package name, an unresolved {@code ${...}} path) is
     * left to the checks above and to the build server.
     *
     * @param iosProjectDir the iOS module directory, whose {@code app_extensions} folder holds
     * one directory or zip per extension
     * @return one problem per extension that cannot be signed as configured
     */
    static List<Problem> checkAppExtensions(Properties settings, boolean release, File iosProjectDir) {
        List<Problem> problems = new ArrayList<Problem>();
        if (iosProjectDir == null) {
            return problems;
        }
        File appExtensions = new File(iosProjectDir, "app_extensions");
        File[] extensions = appExtensions.isDirectory() ? appExtensions.listFiles() : null;
        if (extensions == null || extensions.length == 0) {
            return problems;
        }
        String packageName = settings == null ? null : settings.getProperty("codename1.packageName");
        if (packageName == null || packageName.trim().isEmpty()) {
            return problems;
        }
        packageName = packageName.trim();
        Profile appProfile = appProfile(settings, release);
        if (appProfile == null || appProfile.applicationIdentifier == null) {
            // No readable profile, or one that names no App ID: check() reports the former and
            // neither is something to refuse a build over here.
            return problems;
        }
        for (File extension : extensions) {
            String name = extensionName(extension);
            if (name == null) {
                continue;
            }
            if (hasOwnProfile(settings, extension, name, release)) {
                continue;
            }
            String bundleId = extensionBundleId(extension, packageName + "." + name);
            if (profileCoversBundleId(appProfile.applicationIdentifier, bundleId)) {
                // A wildcard App ID signs the whole subtree, so this build is fine as it is.
                continue;
            }
            problems.add(new Problem(appExtensionProfileMessage(name, bundleId, appProfile,
                    release), true));
        }
        return problems;
    }

    /**
     * Whether the app extensions this build GENERATES can be signed.
     *
     * <p>Same failure as {@link #checkAppExtensions} and the same fix, but these targets never
     * appear under {@code ios/app_extensions/}: the builder synthesizes them from build hints, so
     * nothing on disk announces them and the folder-driven check above cannot see them. Without
     * this, enabling the document provider and never creating its App ID produces a green local
     * build and an Xcode failure minutes into a cloud one, naming a bundle id the developer never
     * typed.</p>
     *
     * <p>Only the document provider is checked here. The other generated extensions (the Wallet
     * pair, CN1Widgets, CN1MatterSetup) have exactly this shape and could join the table below,
     * but each one turns a late cloud failure into an early local one for projects that build
     * today, so they are left for a change that can be verified against real projects rather than
     * folded in as a side effect.</p>
     *
     * @return one problem per generated extension that cannot be signed as configured
     */
    static List<Problem> checkGeneratedExtensions(Properties settings, boolean release) {
        List<Problem> problems = new ArrayList<Problem>();
        if (settings == null) {
            return problems;
        }
        if (!"true".equals(trimmed(settings.getProperty(
                "codename1.arg.ios.documentProvider.enabled")))) {
            return problems;
        }
        if ("false".equals(trimmed(settings.getProperty(
                "codename1.arg.ios.documentProvider.extension")))) {
            // Explicitly opted out: the build generates no target, so there is nothing to sign.
            return problems;
        }
        String packageName = trimmed(settings.getProperty("codename1.packageName"));
        if (packageName == null || packageName.isEmpty()) {
            return problems;
        }
        String name = "CN1Documents";
        if (hasOwnProfileSetting(settings, name, release)) {
            // A profile of its own, but the right one? The generated target always declares the
            // App Group it resolves its container from, so a profile issued before that group
            // was enabled on the App ID matches the bundle id and still cannot sign it -- and
            // the failure lands in Xcode, talking about an entitlement. Only a profile supplied
            // as a local PATH can be read; a hosted URL or base64 blob is checked by the server.
            Problem groupProblem = appGroupProblem(settings, name, release);
            if (groupProblem != null) {
                problems.add(groupProblem);
            }
            // And the app's own profile, which carries the same group: the builder puts the App
            // Group into the host's entitlements too, so a main profile issued before the group
            // was enabled fails signing even when the extension's profile is perfect. Checking
            // only the extension would have moved the failure rather than prevented it.
            Problem hostProblem = hostAppGroupProblem(settings, release);
            if (hostProblem != null) {
                problems.add(hostProblem);
            }
            return problems;
        }
        Profile appProfile = appProfile(settings, release);
        if (appProfile == null || appProfile.applicationIdentifier == null) {
            // No readable profile, or one that names no App ID: check() reports the former and
            // neither is something to refuse a build over here.
            return problems;
        }
        String bundleId = packageName + "." + name;
        if (profileCoversBundleId(appProfile.applicationIdentifier, bundleId)) {
            // No wildcard exemption here, unlike the folder-driven check above. A wildcard App ID
            // does cover the bundle id, but this extension always declares
            // com.apple.security.application-groups -- it resolves the container it reads from
            // that group -- and Apple does not offer App Groups on a wildcard App ID at all. The
            // profile therefore matches the name and still authorizes none of the entitlement, so
            // the build would go on to fail in signing rather than here. A hand-written extension
            // is left alone because nothing tells this check what it declares.
            problems.add(new Problem(wildcardAppGroupMessage(name, bundleId, appProfile, release),
                    true));
            return problems;
        }
        problems.add(new Problem(appExtensionProfileMessage(name, bundleId, appProfile, release),
                true));
        return problems;
    }

    /// Says why a wildcard profile cannot stand in for the generated extension's own.
    private static String wildcardAppGroupMessage(String name, String bundleId, Profile appProfile,
            boolean release) {
        String buildType = release ? "release" : "debug";
        return "The " + name + " app extension has no provisioning profile of its own, and the "
                + "wildcard profile this build signs with (\"" + appProfile.name + "\", App ID "
                + appIdPattern(appProfile.applicationIdentifier) + ") cannot sign it. The bundle "
                + "ID matches, but the extension declares the App Group it shares with the app "
                + "and Apple does not offer the App Groups capability on a wildcard App ID, so "
                + "signing fails on the entitlement rather than on the name.\n"
                + "Create an explicit App ID for " + bundleId + " in the Apple Developer portal, "
                + "enable App Groups on it and on the app's own App ID, generate a provisioning "
                + "profile for it against the same certificate as the app, and supply it in one "
                + "of these ways:\n"
                + "  1. Set codename1.ios." + buildType + ".appext." + name + ".provision="
                + "{path to the .mobileprovision} in codenameone_settings.properties.\n"
                + "  2. Host the profile and name its URL in codename1.arg.ios." + buildType
                + ".appext." + name + ".provisioningURL.\n"
                + "The Certificate Wizard does all of this for you.\n"
                + "The app's own profile has to change too: it carries the same App Group, which "
                + "a wildcard App ID cannot authorize either.";
    }

    /// Whether the extension profile supplied as a local path grants the App Group the
    /// generated target declares, or null when there is nothing to say.
    ///
    /// Silent when the profile cannot be read or carries no App Groups entitlement at all: this
    /// gates a hard refusal, and neither is evidence that signing will fail -- an unreadable
    /// file is reported by check() itself, and a profile whose entitlements this cannot parse is
    /// not a profile this should judge.
    private static Problem appGroupProblem(Properties settings, String name, boolean release) {
        String configured = configuredAppGroup(settings);
        if (configured == null) {
            return null;
        }
        String qualifier = release ? "release" : "debug";
        String[] paths = {
            "codename1.ios." + qualifier + ".appext." + name + ".provision",
            "codename1.ios.appext." + name + ".provision"
        };
        for (String key : paths) {
            String value = settings.getProperty(key);
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            String resolved = resolvePlaceholders(value.trim(), settings);
            if (resolved.indexOf("${") >= 0 || !new File(resolved).isFile()) {
                continue;
            }
            Profile profile;
            try {
                profile = parse(readFile(new File(resolved)));
            } catch (Exception ex) {
                return null;
            }
            if (profile.appGroups == null || profile.appGroups.contains(configured)) {
                return null;
            }
            return new Problem("The " + name + " provisioning profile named by " + key + " ("
                    + profile.name + ") does not grant the App Group \"" + configured + "\". The "
                    + "generated extension declares that group -- it is how it reads the tree the "
                    + "app publishes -- so signing fails on the entitlement rather than on the "
                    + "profile's name. Enable App Groups on the " + name + " App ID, add \""
                    + configured + "\" to it, and regenerate the profile: a profile is a snapshot "
                    + "of the capabilities its App ID had when it was issued, so one made before "
                    + "the change never gains them. The Certificate Wizard does this for you.",
                    true);
        }
        return null;
    }

    /// The same check against the app's own profile.
    private static Problem hostAppGroupProblem(Properties settings, boolean release) {
        String configured = configuredAppGroup(settings);
        Profile profile = appProfile(settings, release);
        if (configured == null || profile == null || profile.appGroups == null
                || profile.appGroups.contains(configured)) {
            return null;
        }
        return new Problem("The app's own provisioning profile (" + profile.name + ") does not "
                + "grant the App Group \"" + configured + "\". Publishing documents puts that "
                + "group in the app's entitlements as well as the extension's -- they meet in the "
                + "container it names -- so the app itself fails to sign. Enable App Groups on "
                + "the app's App ID, add \"" + configured + "\" to it, and regenerate the "
                + "profile. The Certificate Wizard does this for you.", true);
    }

    /// The App Group this build will ask for, or null when it cannot be worked out.
    private static String configuredAppGroup(Properties settings) {
        String configured = trimmed(settings.getProperty(
                "codename1.arg.ios.documentProvider.appGroup"));
        if (configured != null && !configured.isEmpty()) {
            return configured;
        }
        String packageName = trimmed(settings.getProperty("codename1.packageName"));
        if (packageName == null || packageName.isEmpty()) {
            return null;
        }
        return "group." + packageName;
    }

    private static String trimmed(String value) {
        return value == null ? null : value.trim();
    }

    /** The app's own profile for this build type, or null when it cannot be read. */
    private static Profile appProfile(Properties settings, boolean release) {
        String path = settings.getProperty(provisioningProfileSettingKey(release));
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        String resolved = resolvePlaceholders(path.trim(), settings);
        if (resolved.indexOf("${") >= 0) {
            return null;
        }
        File file = new File(resolved);
        if (!file.isFile()) {
            return null;
        }
        try {
            return parse(readFile(file));
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * The target name an {@code app_extensions} entry produces, mirroring what the build
     * zips up: a directory keeps its name, a zip loses the extension. Anything else is not
     * an app extension.
     */
    private static String extensionName(File extension) {
        if (extension.isDirectory()) {
            return extension.getName();
        }
        String name = extension.getName();
        if (name.toLowerCase(Locale.US).endsWith(".zip")) {
            return name.substring(0, name.length() - ".zip".length());
        }
        return null;
    }

    /**
     * Whether a profile is supplied for this extension, by any of the three carriers the
     * build server accepts: a {@code .mobileprovision} travelling inside the extension, the
     * {@code codename1.ios.appext.<Name>.provision} project setting (which the build
     * base64-encodes into {@code provisioningData}), or the hosted-URL build hint. The
     * build-type qualified form counts too, but only the one matching THIS build: the mojo
     * collapses that one into the plain key and deletes the other without promoting it, so a
     * release build holding only the debug-qualified setting reaches the server with no profile
     * at all -- which is the failure this check exists to catch, not to wave through.
     */
    private static boolean hasOwnProfile(Properties settings, File extension, String name,
            boolean release) {
        if (containsProfileFile(extension)) {
            return true;
        }
        return hasOwnProfileSetting(settings, name, release);
    }

    /**
     * The settings-only half of {@link #hasOwnProfile}, for an extension the builder GENERATES.
     * There is no folder on disk to carry a {@code .mobileprovision}, so the project settings and
     * the build hints are the only carriers.
     */
    private static boolean hasOwnProfileSetting(Properties settings, String name,
            boolean release) {
        // Only this build's qualifier, never both. CN1BuildMojo's
        // resolveAppExtensionBuildTypeQualifiers promotes the matching qualifier to the plain key
        // and REMOVES the other one, so counting the opposite build type's setting would pass a
        // release build that has only a debug profile configured -- and it would then fail
        // signing on the server, with a message about the extension's bundle ID rather than about
        // the profile nobody supplied.
        String qualifier = release ? "release" : "debug";
        // An explicitly BLANK key for this build type means "no profile for this build", and it
        // has to beat the unqualified fallback rather than be ignored. The wizard writes one when
        // automatic setup could not produce a development profile, while leaving the unqualified
        // key holding the DISTRIBUTION profile for older tooling; the build then sends that
        // profile for the extension and signing fails on the server, naming the extension's
        // bundle ID rather than the missing development profile.
        //
        // Caught here rather than in CN1BuildMojo.resolveAppExtensionBuildTypeQualifiers, which
        // deliberately keeps the unqualified fallback when a qualified value is blank -- shipped
        // widget-extension projects rely on that, and a signing preflight is the right place to
        // refuse a build the resolution cannot fix. A blank value is written by the wizard and by
        // nothing else: it is not something a developer types by hand, so reading it as "this
        // build type has no profile" cannot misfire on a hand-configured project.
        String[] blanking = {
            "codename1.ios." + qualifier + ".appext." + name + ".provision",
            "codename1.arg.ios." + qualifier + ".appext." + name + ".provisioningURL"
        };
        for (String key : blanking) {
            String value = settings.getProperty(key);
            if (value != null && value.trim().isEmpty()) {
                return false;
            }
        }
        // The two path-valued keys are checked as PATHS, the rest only for a value. A path is
        // the one carrier this can verify, and a stale or mistyped one is the likely mistake:
        // CN1BuildMojo warns that the file is missing, skips encoding it, and submits the build
        // anyway, so the extension reaches the server with no profile and fails signing -- the
        // late failure this check exists to prevent, waved through by the setting that was
        // supposed to prevent it.
        String[] paths = {
            "codename1.ios.appext." + name + ".provision",
            "codename1.ios." + qualifier + ".appext." + name + ".provision"
        };
        for (String key : paths) {
            String value = settings.getProperty(key);
            if (value != null && !value.trim().isEmpty()) {
                String resolved = resolvePlaceholders(value.trim(), settings);
                if (resolved.indexOf("${") < 0 && new File(resolved).isFile()) {
                    return true;
                }
            }
        }
        String[] keys = {
            "codename1.arg.ios.appext." + name + ".provisioningData",
            "codename1.arg.ios.appext." + name + ".provisioningURL",
            "codename1.arg.ios." + qualifier + ".appext." + name + ".provisioningURL"
        };
        for (String key : keys) {
            String value = settings.getProperty(key);
            if (value != null && !value.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** A {@code .mobileprovision} anywhere in the extension, folder or zip alike. */
    private static boolean containsProfileFile(File extension) {
        if (extension.isDirectory()) {
            File[] files = extension.listFiles();
            if (files == null) {
                return false;
            }
            for (File f : files) {
                if (f.isFile() && f.getName().toLowerCase(Locale.US).endsWith(".mobileprovision")) {
                    return true;
                }
            }
            return false;
        }
        // A zipped extension is unzipped into exactly that folder on the build server, so a
        // profile packed inside it is found the same way.
        try (ZipFile zip = new ZipFile(extension)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String entry = entries.nextElement().getName();
                if (entry.toLowerCase(Locale.US).endsWith(".mobileprovision")) {
                    return true;
                }
            }
        } catch (IOException ex) {
            // Unreadable here means unreadable for the build too; not this check's business.
            return true;
        }
        return false;
    }

    /**
     * The bundle id the extension target actually carries. An extension may override it in
     * its {@code buildSettings.properties}, and judging the default instead would refuse a
     * build whose profile matches the overridden id -- the same trap the Matter extension
     * hit on the build server.
     */
    private static String extensionBundleId(File extension, String defaultBundleId) {
        Properties buildSettings = new Properties();
        try {
            if (extension.isDirectory()) {
                File file = new File(extension, "buildSettings.properties");
                if (!file.isFile()) {
                    return defaultBundleId;
                }
                try (InputStream in = new FileInputStream(file)) {
                    buildSettings.load(in);
                }
            } else {
                try (ZipFile zip = new ZipFile(extension)) {
                    ZipEntry entry = zip.getEntry("buildSettings.properties");
                    if (entry == null) {
                        return defaultBundleId;
                    }
                    try (InputStream in = zip.getInputStream(entry)) {
                        buildSettings.load(in);
                    }
                }
            }
        } catch (Exception ex) {
            return defaultBundleId;
        }
        String override = buildSettings.getProperty("PRODUCT_BUNDLE_IDENTIFIER");
        if (override == null || override.trim().isEmpty()) {
            return defaultBundleId;
        }
        return override.trim();
    }

    /**
     * Whether a profile's {@code application-identifier} entitlement covers a bundle id.
     *
     * <p>Only the part after the team prefix is the pattern, and the one metacharacter Apple
     * defines is a trailing {@code *} -- so this is prefix matching, not a glob. Unknown
     * answers count as covered: this gates a hard refusal, and a profile that could not be
     * read is not evidence that signing will fail.
     *
     * @param applicationIdentifier the entitlement, team prefix and all
     * @param bundleId the bundle id that has to be signed
     * @return false only when the profile demonstrably cannot sign it
     */
    static boolean profileCoversBundleId(String applicationIdentifier, String bundleId) {
        if (applicationIdentifier == null || applicationIdentifier.isEmpty() || bundleId == null) {
            return true;
        }
        String pattern = appIdPattern(applicationIdentifier);
        if (pattern.endsWith("*")) {
            return bundleId.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return pattern.equals(bundleId);
    }

    /** The App ID with the team prefix stripped, which is the part that has to match. */
    private static String appIdPattern(String applicationIdentifier) {
        int teamPrefix = applicationIdentifier.indexOf('.');
        return teamPrefix < 0 ? applicationIdentifier
                : applicationIdentifier.substring(teamPrefix + 1);
    }

    /**
     * The message the developer reads instead of Xcode's. It quotes the failure they would
     * otherwise get -- so searching for it lands here -- and names all three ways to supply
     * the profile, since which one fits depends on whether the profile can live in the repo.
     */
    private static String appExtensionProfileMessage(String name, String bundleId, Profile appProfile,
            boolean release) {
        String buildType = release ? "release" : "debug";
        return "The " + name + " app extension has no provisioning profile of its own, and the "
                + "profile this build signs with (\"" + appProfile.name + "\", App ID "
                + appIdPattern(appProfile.applicationIdentifier) + ") cannot sign it. Apple "
                + "requires a separate App ID and provisioning profile for every app extension, "
                + "so Xcode will fail with: Provisioning profile \"" + appProfile.name
                + "\" has app ID \"" + appIdPattern(appProfile.applicationIdentifier)
                + "\", which does not match the bundle ID \"" + bundleId + "\".\n"
                + "Create an App ID for " + bundleId + " in the Apple Developer portal (Wallet "
                + "extensions also need the payment-pass-provisioning entitlement on it), "
                + "generate a provisioning profile for it against the same certificate as the "
                + "app, and supply it in one of these ways:\n"
                + "  1. Place the .mobileprovision file inside ios/app_extensions/" + name
                + "/ -- it is picked up automatically and kept out of the app bundle.\n"
                + "  2. Set codename1.ios." + buildType + ".appext." + name + ".provision="
                + "{path to the .mobileprovision} in codenameone_settings.properties.\n"
                + "  3. Host the profile and name its URL in codename1.arg.ios." + buildType
                + ".appext." + name + ".provisioningURL.";
    }

    /**
     * Expands {@code ${name}} references the way the generated build file's
     * {@code <property file="codenameone_settings.properties">} does: a profile path is
     * routinely written as {@code ${user.home}/certs/dev.mobileprovision}, and Ant resolves
     * that before handing the value to the build task. A name is looked up in the settings
     * themselves first, then in the JVM's system properties, which is where {@code user.home}
     * and friends live.
     *
     * <p>What cannot be resolved is left standing rather than guessed at, and the caller
     * treats a value that still carries a placeholder as one it may not judge. Refusing a
     * build over a path this code merely failed to expand would break configurations that
     * work today -- the opposite of the point.
     *
     * @return the value with every resolvable reference expanded
     */
    static String resolvePlaceholders(String value, Properties settings) {
        if (value == null) {
            return null;
        }
        String current = value;
        // Bounded: a self-referential property would otherwise spin here.
        for (int pass = 0; pass < 10 && current.indexOf("${") >= 0; pass++) {
            StringBuilder out = new StringBuilder();
            boolean expandedAny = false;
            int i = 0;
            while (i < current.length()) {
                int start = current.indexOf("${", i);
                if (start < 0) {
                    out.append(current.substring(i));
                    break;
                }
                int end = current.indexOf('}', start);
                if (end < 0) {
                    out.append(current.substring(i));
                    break;
                }
                out.append(current, i, start);
                String name = current.substring(start + 2, end);
                // JVM properties first, which is Ant's own order. Ant seeds its project
                // from the system properties before the generated build file's
                // <property file="codenameone_settings.properties"> runs, and an Ant
                // property cannot be set twice -- so -Dprofile.dir=/new beats a
                // profile.dir=/old in the file. Reading the file first meant refusing
                // /old/profile.mobileprovision as missing while the build used /new.
                String replacement = System.getProperty(name);
                if (replacement == null && settings != null) {
                    replacement = settings.getProperty(name);
                }
                if (replacement == null) {
                    out.append(current, start, end + 1);
                } else {
                    out.append(replacement);
                    expandedAny = true;
                }
                i = end + 1;
            }
            current = out.toString();
            if (!expandedAny) {
                break;
            }
        }
        return current;
    }

    /**
     * @param checkMethodMismatch whether to also compare the profile's kind against the
     * distribution method these settings resolve to. Only true once the settings are the ones
     * the build will be submitted with, since a CN1Lib can still change the method.
     */
    private static void collectFileProblems(List<Problem> problems, Properties settings, boolean release,
            Date now, String path, String settingKey, boolean checkMethodMismatch) {
        String resolved = resolvePlaceholders(path.trim(), settings);
        if (resolved.indexOf("${") >= 0) {
            // Ant resolves this when it binds the value to the build task; this check cannot,
            // because it does not have that property context. A path it cannot resolve is a
            // path it cannot judge -- and calling it missing would refuse a build that works.
            return;
        }
        File file = new File(resolved);
        if (!file.exists() || !file.isFile()) {
            problems.add(new Problem("The provisioning profile for this build was not found at "
                    + file.getAbsolutePath() + " (" + settingKey + "). Point that setting at the "
                    + ".mobileprovision file, or re-generate it with the certificate wizard.", true));
            return;
        }

        byte[] raw;
        try {
            raw = readFile(file);
        } catch (IOException ex) {
            problems.add(new Problem("The provisioning profile at " + file.getAbsolutePath()
                    + " could not be read: " + ex.getMessage(), true));
            return;
        }

        if (raw.length == 0) {
            problems.add(new Problem("The provisioning profile at " + file.getAbsolutePath()
                    + " is empty (0 bytes). Re-download it from the Apple Developer portal, or "
                    + "re-generate it with the certificate wizard.", true));
            return;
        }

        Profile profile;
        try {
            profile = parse(raw);
        } catch (Exception ex) {
            profile = null;
        }
        if (profile == null) {
            problems.add(new Problem("The file at " + file.getAbsolutePath() + " (" + raw.length
                    + " bytes) is not a valid .mobileprovision file -- it carries no readable "
                    + "provisioning plist. Re-download it from the Apple Developer portal, or "
                    + "re-generate it with the certificate wizard.", true));
            return;
        }

        String describe = profile.name == null ? file.getName() : "\"" + profile.name + "\"";
        if (profile.expirationDate == null) {
            // The key is there -- isProvisioningPlist insisted on that -- but its value is not
            // a date this can read. Skipping the expiry check on that basis let a corrupted
            // profile through on the strength of its type alone, which is the build slot this
            // check exists to save. Every Apple-issued profile carries a readable expiry.
            problems.add(new Problem("The provisioning profile " + describe + " at "
                    + file.getAbsolutePath() + " has no readable expiry date, so it is not a"
                    + " usable .mobileprovision file. Re-download it from the Apple Developer"
                    + " portal, or re-generate it with the certificate wizard.", true));
            return;
        }
        if (profile.expirationDate.before(now)) {
            problems.add(new Problem("The provisioning profile " + describe + " expired on "
                    + profile.expirationDate + ". Generate a new one in the Apple Developer portal "
                    + "and update " + settingKey + ".", true));
            return;
        }

        if (!checkMethodMismatch) {
            return;
        }
        String method = effectiveDistributionMethod(settings, release);
        if (method.indexOf("${") >= 0) {
            // Same rule as the path: what this cannot resolve, it may not judge.
            return;
        }
        Problem mismatch = checkMethod(profile, method, describe, release);
        if (mismatch != null) {
            problems.add(mismatch);
        }
    }

    /**
     * The type mismatch {@code xcodebuild -exportArchive} would reject minutes into the
     * cloud build. Enterprise profiles only warn: Xcode accepts an in-house profile for
     * more than one method, so refusing one risks blocking a build that works.
     */
    private static Problem checkMethod(Profile profile, String method, String describe, boolean release) {
        if (profile.type == null || method == null || method.equals(profile.type)) {
            return null;
        }
        String hint = release ? "ios.release.distributionMethod" : "ios.debug.distributionMethod";
        String message = "The provisioning profile " + describe + " is " + article(profile.type)
                + " profile, but this build is configured to export with method \"" + method
                + "\" (" + hint + "). Xcode will refuse to export with a mismatched profile."
                + " Either set " + hint + "=" + profile.type + ", or use " + article(method)
                + " provisioning profile.";
        if (ENTERPRISE.equals(profile.type) || ENTERPRISE.equals(method)) {
            return new Problem(message, false);
        }
        return new Problem(message, true);
    }

    private static String article(String type) {
        if (AD_HOC.equals(type) || APP_STORE.equals(type) || ENTERPRISE.equals(type)) {
            return "an " + describeType(type);
        }
        return "a " + describeType(type);
    }

    private static String describeType(String type) {
        if (AD_HOC.equals(type)) {
            return "Ad Hoc";
        }
        if (APP_STORE.equals(type)) {
            return "App Store distribution";
        }
        if (ENTERPRISE.equals(type)) {
            return "in-house (enterprise)";
        }
        return "Development";
    }

    /**
     * Reads the plist payload of a {@code .mobileprovision}.
     *
     * @return what the profile is, or null when the bytes carry no provisioning plist.
     */
    static Profile parse(byte[] raw) throws Exception {
        byte[] plist = extractEmbeddedPlist(raw);
        if (plist == null) {
            return null;
        }
        DocumentBuilder db = secureDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(plist));

        if (!isProvisioningPlist(doc)) {
            // An ordinary plist parses perfectly well. Info.plist has no device list, so
            // deriveType would have called it an App Store profile, and a release build --
            // whose default method is app-store -- would have sailed through this check and
            // uploaded a file that cannot provision or sign anything.
            return null;
        }

        Profile profile = new Profile();
        Element name = valueForKey(doc, "Name");
        if (name != null) {
            profile.name = name.getTextContent().trim();
        }
        // Left null when the value is not a <date>, or is one this cannot read; the caller
        // treats that as an unusable profile rather than as "no expiry to check".
        Element expires = valueForKey(doc, "ExpirationDate");
        if (expires != null && "date".equals(expires.getTagName())) {
            profile.expirationDate = parseDate(expires.getTextContent().trim());
        }
        // Nested inside the Entitlements dict, which valueForKey reaches because it walks
        // every <key> in the document. This is what says which bundle ids the profile can
        // sign -- see profileCoversBundleId.
        Element applicationIdentifier = valueForKey(doc, "application-identifier");
        if (applicationIdentifier != null && "string".equals(applicationIdentifier.getTagName())) {
            profile.applicationIdentifier = applicationIdentifier.getTextContent().trim();
        }
        // Same nesting, same reason: this is what says whether the profile can sign a target
        // that declares an App Group, which every generated document provider does.
        Element groups = valueForKey(doc, "com.apple.security.application-groups");
        if (groups != null && "array".equals(groups.getTagName())) {
            List<String> found = new ArrayList<String>();
            NodeList values = groups.getElementsByTagName("string");
            for (int i = 0; i < values.getLength(); i++) {
                String value = values.item(i).getTextContent().trim();
                if (!value.isEmpty()) {
                    found.add(value);
                }
            }
            profile.appGroups = found;
        }
        profile.type = deriveType(doc);
        return profile;
    }

    /**
     * A parser that treats the profile as what it is: untrusted input that nothing has
     * verified. A {@code .mobileprovision} is signed, but neither this check nor the build
     * server validates that signature, and a profile can arrive from anyone -- a client
     * sending one for their app, a repository, a support ticket.
     *
     * <p>The DOCTYPE itself has to stay legal, because every real plist declares one. What is
     * refused is entity resolution: without this, an entity declared in a crafted profile's
     * internal subset is resolved by the JDK parser, so a profile could read a local file or
     * make a network request during someone's build -- and, used as the profile {@code Name},
     * have the result printed back in an error message. Turning off external general and
     * parameter entities, entity-reference expansion, XInclude and external DTD access closes
     * that; secure processing additionally caps entity expansion, so a profile cannot hang a
     * build with nested entities.
     *
     * <p>None of these is set "best effort": a parser that cannot be told to stop resolving
     * entities has no business reading an untrusted profile, so the exception propagates and
     * the profile is reported as unreadable rather than parsed unsafely.
     */
    private static DocumentBuilder secureDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        // the plist DOCTYPE also points at apple.com, so this keeps a local check from
        // depending on Apple's web server answering
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        dbf.setXIncludeAware(false);
        dbf.setExpandEntityReferences(false);
        return dbf.newDocumentBuilder();
    }

    /**
     * Whether this plist is a provisioning profile rather than some other plist the setting
     * happens to point at.
     *
     * <p>Keyed on what every Apple-issued profile carries and no ordinary plist does: the
     * profile's own UUID, when it expires, the entitlements it grants, and the certificates it
     * was issued to. Checked against real development, distribution and Xcode-team profiles.
     *
     * <p>Each is checked by shape, not merely by name. A corrupted file can keep the key and
     * lose the value -- an empty UUID, Entitlements as a string, DeveloperCertificates as an
     * empty array -- and presence alone accepted all of those. With a readable future expiry
     * and no device list, deriveType then called it an App Store profile and a default release
     * build sailed through.
     *
     * <p>Still deliberately a small set: the more that is demanded here, the more likely this
     * refuses a profile that is perfectly good, which is the failure mode that matters most.
     */
    private static boolean isProvisioningPlist(Document doc) {
        // ExpirationDate is required to be PRESENT here and is shape-checked downstream:
        // a profile whose date is the wrong type or unreadable deserves the message that
        // says so, rather than the generic "this is not a profile" this method produces.
        return hasNonEmptyString(doc, "UUID")
                && valueForKey(doc, "ExpirationDate") != null
                && hasValueTagged(doc, "Entitlements", "dict")
                && hasDeveloperCertificates(doc);
    }

    /** A key whose value is a {@code <string>} with something in it. */
    private static boolean hasNonEmptyString(Document doc, String key) {
        Element value = valueForKey(doc, key);
        return value != null && "string".equals(value.getTagName())
                && value.getTextContent().trim().length() > 0;
    }

    /** A key whose value is of the expected plist type. */
    private static boolean hasValueTagged(Document doc, String key, String tag) {
        Element value = valueForKey(doc, key);
        return value != null && tag.equals(value.getTagName());
    }

    /** An array holding at least one certificate with bytes in it. */
    private static boolean hasDeveloperCertificates(Document doc) {
        Element value = valueForKey(doc, "DeveloperCertificates");
        if (value == null || !"array".equals(value.getTagName())) {
            return false;
        }
        NodeList certificates = value.getElementsByTagName("data");
        for (int i = 0; i < certificates.getLength(); i++) {
            if (certificates.item(i).getTextContent().trim().length() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * The four profile kinds, told apart the way Apple's own tooling does: an in-house
     * profile provisions all devices, a profile that lists devices is Development when it
     * allows the debugger to attach ({@code get-task-allow}) and Ad Hoc when it does not,
     * and a profile that lists no devices at all is an App Store one.
     */
    private static String deriveType(Document doc) {
        Element provisionsAllDevices = valueForKey(doc, "ProvisionsAllDevices");
        if (provisionsAllDevices != null && "true".equals(provisionsAllDevices.getTagName())) {
            return ENTERPRISE;
        }
        Element devices = valueForKey(doc, "ProvisionedDevices");
        if (devices != null && "array".equals(devices.getTagName())) {
            Element getTaskAllow = valueForKey(doc, "get-task-allow");
            if (getTaskAllow != null && "true".equals(getTaskAllow.getTagName())) {
                return DEVELOPMENT;
            }
            return AD_HOC;
        }
        return APP_STORE;
    }

    /** The element that follows the named {@code <key>} -- a plist's value for that key. */
    private static Element valueForKey(Document doc, String key) {
        NodeList keys = doc.getElementsByTagName("key");
        for (int i = 0; i < keys.getLength(); i++) {
            Element k = (Element) keys.item(i);
            if (!key.equals(k.getTextContent().trim())) {
                continue;
            }
            Node n = k.getNextSibling();
            while (n != null && !(n instanceof Element)) {
                n = n.getNextSibling();
            }
            if (n != null) {
                return (Element) n;
            }
        }
        return null;
    }

    /**
     * The profile's expiry, or null when the value is not exactly the timestamp Apple writes.
     *
     * <p>Strict on both counts, because the default is not. Lenient parsing rolls an
     * impossible date over -- {@code 2099-02-30} becomes 2 March -- and
     * {@code parse(String)} stops at the first character it cannot use, so a value with
     * trailing garbage came back as a perfectly good date. Either way a corrupted profile
     * looked valid and went on to spend a cloud build slot.
     */
    private static Date parseDate(String value) {
        // Locale.US, not the machine's: the no-argument constructor takes the default
        // locale's calendar, so under th_TH that is a BuddhistCalendar and Apple's 2099
        // reads as Gregorian 1556 -- a perfectly good profile refused as expired on one
        // developer's laptop and accepted on the next.
        SimpleDateFormat format =
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        format.setLenient(false);
        ParsePosition at = new ParsePosition(0);
        Date parsed = format.parse(value, at);
        if (parsed == null || at.getIndex() != value.length()) {
            return null;
        }
        return parsed;
    }

    /**
     * Lifts the {@code <?xml ... </plist>} payload out of the CMS envelope. The payload is
     * not encrypted, which is what lets this run without {@code security} or a Mac.
     */
    static byte[] extractEmbeddedPlist(byte[] raw) {
        if (raw == null) {
            return null;
        }
        int start = indexOf(raw, "<?xml".getBytes(StandardCharsets.UTF_8), 0);
        if (start < 0) {
            return null;
        }
        byte[] end = "</plist>".getBytes(StandardCharsets.UTF_8);
        int endPos = indexOf(raw, end, start);
        if (endPos < 0) {
            return null;
        }
        int len = endPos + end.length - start;
        byte[] plist = new byte[len];
        System.arraycopy(raw, start, plist, 0, len);
        return plist;
    }

    private static int indexOf(byte[] haystack, byte[] needle, int from) {
        outer:
        for (int i = Math.max(0, from); i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static byte[] readFile(File f) throws IOException {
        try (InputStream is = new FileInputStream(f)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int amount;
            while ((amount = is.read(buffer)) > -1) {
                baos.write(buffer, 0, amount);
            }
            return baos.toByteArray();
        }
    }
}
