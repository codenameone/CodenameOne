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

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code macos.*} build-hint family for the native macOS (AppKit) target,
 * with the legacy {@code macNative.*} names accepted as aliases.
 *
 * <p>The alias is not a courtesy, it is the migration. An existing project was
 * written against a target that produced a Mac Catalyst app and spelled its
 * settings {@code macNative.*}; that target now produces an AppKit app under the
 * same name, and every setting that still means something has to carry over
 * untouched or the first build after an upgrade fails on settings the developer
 * never changed.</p>
 *
 * <p>A hint that no longer means anything is accepted and ignored with a warning
 * rather than rejected. {@code macNative.iosMinDeploymentTarget} described the
 * iOS half of a Catalyst slice and there is no such half any more, but it is
 * sitting in real {@code codenameone_settings.properties} files today, and
 * failing a build over a stale line the developer cannot be expected to know
 * about is the wrong trade.</p>
 */
public class MacOSBuildHints {

    /** How the app is signed and packaged. */
    public static final String DISTRIBUTION_APP_STORE = "appStore";
    public static final String DISTRIBUTION_DEVELOPER_ID = "developerID";
    public static final String DISTRIBUTION_BOTH = "both";

    /** Default deployment floor. Universal arm64 plus a mature NSTextInputClient. */
    public static final String DEFAULT_DEPLOYMENT_TARGET = "11.0";

    /** The identities the Catalyst builder defaults to, kept so a migrated project still signs. */
    public static final String DEFAULT_IDENTITY_APP_STORE = "Apple Distribution";
    public static final String DEFAULT_IDENTITY_DEVELOPER_ID = "Developer ID Application";

    /**
     * Asks for an unsigned build. Needed because an empty hint reads as unset and
     * therefore takes the default; this is how a smoke build says "no signature"
     * out loud.
     */
    public static final String NO_SIGNING_IDENTITY = "none";

    /**
     * The neutral category, and the one the build-hint documentation states.
     *
     * <p>Not developer-tools: that was the sample's own setting leaking into the
     * default, and it would classify every customer application that never sets
     * the hint -- a game, a shop, a utility -- as a developer tool in the App
     * Store.</p>
     */
    private static final String DEFAULT_APP_CATEGORY = "public.app-category.utilities";

    private final List<String> warnings = new ArrayList<String>();

    private String distribution = DISTRIBUTION_DEVELOPER_ID;
    private String teamId;
    private String bundleId;
    private boolean deriveBundleId;
    private String minDeploymentTarget = DEFAULT_DEPLOYMENT_TARGET;
    private String appCategory = DEFAULT_APP_CATEGORY;
    private String copyright;
    private String signingStyle;
    private String signingIdentityAppStore;
    private String signingIdentityDeveloperID;
    private String fixedWindowSize;
    private String packaging;
    private boolean packagingExplicit;
    private String installerIdentity;
    private String installerIdentityAppStore;
    private String installerIdentityDeveloperID;
    private Boolean sandbox;
    private boolean hardenedRuntime = true;
    private boolean notarize;
    private String notarizeKeychainProfile;
    private String notarizeAppleId;
    private String notarizeTeamId;
    private String notarizePassword;
    /// The hints this instance was parsed from, read again by the accessors that
    /// resolve per channel. Initialized to a source that answers every key with
    /// its default, so an instance nobody called parse() on behaves like a
    /// project with no hints rather than throwing at the first accessor.
    private HintSource source = NO_HINTS;

    /**
     * Reads one hint, preferring the {@code macos.} spelling and falling back to
     * the legacy {@code macNative.} one.
     */
    public interface HintSource {
        String get(String key, String defaultValue);
    }

    private static final HintSource NO_HINTS = new HintSource() {
        @Override
        public String get(String key, String defaultValue) {
            return defaultValue;
        }
    };

    private String hint(HintSource src, String suffix, String defaultValue) {
        String modern = src.get("macos." + suffix, null);
        if (modern != null && modern.length() > 0) {
            return modern;
        }
        String legacy = src.get("macNative." + suffix, null);
        if (legacy != null && legacy.length() > 0) {
            return legacy;
        }
        return defaultValue;
    }

    private static boolean isTrue(String v) {
        return "true".equalsIgnoreCase(v) || "1".equals(v) || "on".equalsIgnoreCase(v);
    }

    /** The first non-empty value, or null. */
    private static String first(String... values) {
        for (String v : values) {
            if (v != null && v.length() > 0) {
                return v;
            }
        }
        return null;
    }

    /**
     * A tri-state entitlement hint: the developer's value, or {@code fallback}
     * when unset. Separate from {@link #isTrue} because "unset" and "false" mean
     * different things here -- most of these default to something other than
     * false, so treating a missing hint as false would turn every default off.
     */
    private static boolean entBool(HintSource src, String suffix, boolean fallback) {
        String v = src.get("macos.entitlements." + suffix, null);
        if (v == null || v.length() == 0) {
            v = src.get("macNative.entitlements." + suffix, null);
        }
        if (v == null || v.length() == 0) {
            return fallback;
        }
        return isTrue(v);
    }

    private static String entString(HintSource src, String suffix, String fallback) {
        String v = src.get("macos.entitlements." + suffix, null);
        if (v == null || v.length() == 0) {
            v = src.get("macNative.entitlements." + suffix, null);
        }
        return v == null || v.length() == 0 ? fallback : v;
    }

    /**
     * Parses the hint family. Never throws on a hint it does not recognise; an
     * unusable value is recorded in {@link #getWarnings()} and the default kept,
     * because a build must not fail over a setting whose meaning changed
     * underneath the project.
     */
    public void parse(HintSource src, String packageName) {
        source = src;
        String dist = hint(src, "distribution", DISTRIBUTION_DEVELOPER_ID);
        if (DISTRIBUTION_APP_STORE.equalsIgnoreCase(dist)) {
            distribution = DISTRIBUTION_APP_STORE;
        } else if (DISTRIBUTION_BOTH.equalsIgnoreCase(dist)) {
            distribution = DISTRIBUTION_BOTH;
        } else if (DISTRIBUTION_DEVELOPER_ID.equalsIgnoreCase(dist)) {
            distribution = DISTRIBUTION_DEVELOPER_ID;
        } else {
            warnings.add("Unrecognized macos.distribution '" + dist + "'; using "
                    + DISTRIBUTION_DEVELOPER_ID + ". Valid values are appStore, developerID and both.");
            distribution = DISTRIBUTION_DEVELOPER_ID;
        }

        // The Apple team id can legitimately be shared with the iOS build, so it
        // is not asked for twice. The whole legacy chain, not just the first
        // link: a project that only ever set ios.teamId or ios.debug.teamId
        // signed fine under the Catalyst builder, and dropping either link
        // leaves automatic signing with no DEVELOPMENT_TEAM -- which fails, or
        // worse picks another team the account happens to belong to.
        teamId = hint(src, "teamId",
                first(src.get("ios.release.teamId", null),
                        src.get("ios.teamId", null),
                        src.get("ios.debug.teamId", null)));

        deriveBundleId = isTrue(hint(src, "deriveBundleId", "false"));
        bundleId = hint(src, "bundleId", null);
        if (bundleId == null || bundleId.length() == 0) {
            // A separate identifier by default. A macOS app and an iOS app are
            // distinct products in App Store Connect, and giving them one id is
            // a submission failure discovered late rather than a build failure
            // discovered now.
            bundleId = deriveBundleId ? packageName : packageName + ".mac";
        }

        minDeploymentTarget = hint(src, "minDeploymentTarget", DEFAULT_DEPLOYMENT_TARGET);
        appCategory = hint(src, "appCategory", DEFAULT_APP_CATEGORY);
        copyright = hint(src, "copyright", null);
        signingStyle = hint(src, "signing.style", null);
        // Defaulted, not left null. The Catalyst builder defaults these to the
        // same two strings, so a project carrying only a team id signed there
        // and would silently produce an unsigned dmg or pkg here -- a build you
        // paid for and cannot ship. NO_SIGNING_IDENTITY is the way to ask for an
        // unsigned build on purpose, which an empty value cannot express because
        // an empty hint reads as unset.
        signingIdentityAppStore = hint(src, "signingIdentity.appStore",
                DEFAULT_IDENTITY_APP_STORE);
        signingIdentityDeveloperID = hint(src, "signingIdentity.developerID",
                DEFAULT_IDENTITY_DEVELOPER_ID);
        // Screenshot CI is the only consumer: a window whose size the app cannot
        // change is what makes a strict pixel comparison meaningful.
        fixedWindowSize = hint(src, "fixedWindowSize", null);

        packaging = hint(src, "packaging", null);
        packagingExplicit = packaging != null && packaging.length() > 0;
        if (!packagingExplicit) {
            packaging = defaultPackagingFor(distribution);
        }

        // The installer identity is a different certificate from the application
        // one -- "3rd Party Mac Developer Installer" / "Developer ID Installer"
        // -- and productbuild wants that one. Signing a pkg with the application
        // identity produces a package the store rejects, so it is asked for
        // separately rather than derived.
        installerIdentity = hint(src, "signingIdentity.installer", null);
        // Per-channel spellings, for distribution=both where the two packages
        // need different installer certificates.
        installerIdentityAppStore = hint(src, "signingIdentity.installer.appStore", null);
        installerIdentityDeveloperID = hint(src, "signingIdentity.installer.developerID", null);

        String sandboxHint = hint(src, "sandbox", null);
        // Unset rather than false: the App Store requires the sandbox and direct
        // distribution usually does not want it, so the default follows the
        // channel instead of being one value for both.
        sandbox = sandboxHint == null ? null : Boolean.valueOf(isTrue(sandboxHint));
        if (sandbox != null && !sandbox.booleanValue() && buildsAppStoreChannel()) {
            warnings.add("macos.sandbox=false is ignored for the App Store channel, which "
                    + "requires the sandbox; the package would be rejected at submission. It is "
                    + "honored for the Developer ID channel.");
        }
        hardenedRuntime = !"false".equalsIgnoreCase(hint(src, "hardenedRuntime", "true"));

        notarize = isTrue(hint(src, "notarize", "false"));
        notarizeKeychainProfile = hint(src, "notarize.keychainProfile", null);
        notarizeAppleId = hint(src, "notarize.appleId", null);
        notarizeTeamId = hint(src, "notarize.teamId", teamId);
        notarizePassword = hint(src, "notarize.password", null);

        String staleIosTarget = src.get("macNative.iosMinDeploymentTarget", null);
        if (staleIosTarget != null && staleIosTarget.length() > 0) {
            warnings.add("macNative.iosMinDeploymentTarget is ignored by the native macOS build; "
                    + "it described the iOS half of a Mac Catalyst slice, and this target has none. "
                    + "Remove it, or set macNative.enabled and build the iOS target if you "
                    + "still need Mac Catalyst.");
        }
    }

    static String defaultPackagingFor(String distribution) {
        if (DISTRIBUTION_APP_STORE.equals(distribution)) {
            return "pkg";
        }
        if (DISTRIBUTION_BOTH.equals(distribution)) {
            return "both";
        }
        return "dmg";
    }

    // No whole-build isSandboxed()/getPackaging()/getSigningIdentity<Channel>()
    // accessors. Both are per channel now, and a second way to ask that answers
    // for "the build" would have to pick one channel's answer for a
    // distribution=both build -- which is how the App Store package came to be
    // signed with the Developer ID entitlements in the first place.

    public List<String> getWarnings() {
        return warnings;
    }

    public String getDistribution() {
        return distribution;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getBundleId() {
        return bundleId;
    }

    public String getMinDeploymentTarget() {
        return minDeploymentTarget;
    }

    public String getAppCategory() {
        return appCategory;
    }

    public String getCopyright() {
        return copyright;
    }

    public String getSigningStyle() {
        return signingStyle;
    }



    /**
     * The provisioning profile NAME configured for one channel, or null.
     *
     * <p>Through {@link #hint}, so the legacy {@code macNative.} spelling is
     * accepted: the Catalyst path consumed it, and a project migrating off that
     * path keeps it. Read straight off the request, such a build supplies no
     * profile at all and manual signing fails for a configuration that is
     * complete.</p>
     */
    public String getProvisioningProfileFor(String channel) {
        return hint(source, DISTRIBUTION_APP_STORE.equals(channel)
                ? "provisioningProfile.appStore" : "provisioningProfile.developerID", null);
    }

    /** The installer certificate productbuild signs a {@code .pkg} with, or null. */
    public String getInstallerIdentity() {
        return installerIdentity;
    }

    /**
     * The installer certificate for one channel.
     *
     * <p>Per channel because they are different certificates: the App Store
     * wants a "3rd Party Mac Developer Installer" and Developer ID a "Developer
     * ID Installer". With {@code distribution=both} and a pkg on each side, one
     * shared value signed both packages with the same certificate and exactly
     * one of them was then unusable -- which the channel-specific error message
     * this class already produces was implicitly promising not to do.</p>
     *
     * <p>{@code macos.signingIdentity.installer} stays as the value for a build
     * that ships one channel, which is nearly all of them.</p>
     */
    public String getInstallerIdentityFor(String channel) {
        String perChannel = DISTRIBUTION_APP_STORE.equals(channel)
                ? installerIdentityAppStore : installerIdentityDeveloperID;
        if (perChannel != null && perChannel.trim().length() > 0) {
            return perChannel;
        }
        // The shared hint answers for a build that ships ONE channel, which is
        // nearly all of them. It must not answer for both: the App Store takes a
        // "3rd Party Mac Developer Installer" certificate and Developer ID takes
        // a "Developer ID Installer" one, so a single value cannot be right for
        // the two of them -- and signing each package with the same certificate
        // produces two artifacts of which at least one is rejected, after a
        // build that reported success.
        if (getChannels().size() > 1) {
            return null;
        }
        return installerIdentity;
    }

    /**
     * The application signing identity for one channel, or {@code null} when that
     * channel is unsigned. Unsigned is a legitimate outcome -- it is what the
     * screenshot suite and a local smoke test want -- so it is not an error.
     */
    public String getSigningIdentityFor(String channel) {
        String identity = DISTRIBUTION_APP_STORE.equals(channel)
                ? signingIdentityAppStore : signingIdentityDeveloperID;
        if (identity == null || identity.trim().length() == 0
                || NO_SIGNING_IDENTITY.equalsIgnoreCase(identity.trim())) {
            return null;
        }
        return identity;
    }

    /**
     * Whether Xcode resolves the certificate itself.
     *
     * <p>Manual is the default, deliberately, even though it reads as the less
     * convenient one. A build server has an installed certificate and no Xcode
     * account session, and automatic signing there fails asking to sign in --
     * which is why the Catalyst ExportOptions default to manual too. Automatic
     * is for a developer building on their own machine.</p>
     */
    public boolean usesAutomaticSigning() {
        return "automatic".equalsIgnoreCase(signingStyle);
    }

    /**
     * The hardened runtime, as an Xcode build setting rather than an entitlement.
     * Notarization requires it, so it is on unless {@code macos.hardenedRuntime}
     * turns it off. Distinct from {@code macos.entitlements.hardenedRuntime},
     * which decides the JIT exceptions inside the signature.
     */
    public boolean isHardenedRuntime() {
        return hardenedRuntime;
    }

    public String getFixedWindowSize() {
        return fixedWindowSize;
    }

    public boolean isNotarize() {
        return notarize;
    }

    public String getNotarizeKeychainProfile() {
        return notarizeKeychainProfile;
    }

    public String getNotarizeAppleId() {
        return notarizeAppleId;
    }

    public String getNotarizeTeamId() {
        return notarizeTeamId;
    }

    public String getNotarizePassword() {
        return notarizePassword;
    }

    public boolean buildsAppStoreChannel() {
        return DISTRIBUTION_APP_STORE.equals(distribution) || DISTRIBUTION_BOTH.equals(distribution);
    }

    public boolean buildsDeveloperIdChannel() {
        return DISTRIBUTION_DEVELOPER_ID.equals(distribution) || DISTRIBUTION_BOTH.equals(distribution);
    }

    /**
     * The channels this build produces, in order. {@code distribution=both} is
     * two channels and therefore two xcodebuild invocations: they differ in the
     * signing certificate AND in the entitlements (the App Store one must be
     * sandboxed), so one binary cannot serve both no matter how it is packaged.
     */
    public List<String> getChannels() {
        List<String> channels = new ArrayList<String>();
        if (buildsAppStoreChannel()) {
            channels.add(DISTRIBUTION_APP_STORE);
        }
        if (buildsDeveloperIdChannel()) {
            channels.add(DISTRIBUTION_DEVELOPER_ID);
        }
        return channels;
    }

    /**
     * The packaging for one channel. An explicit {@code macos.packaging} applies
     * to every channel; unset, each channel takes its own default -- {@code pkg}
     * for the App Store, because productbuild's output is what you upload, and
     * {@code dmg} for Developer ID. So {@code distribution=both} yields a pkg and
     * a dmg rather than one artifact that is neither.
     */
    public String getPackagingFor(String channel) {
        return packagingExplicit ? packaging : defaultPackagingFor(channel);
    }

    /**
     * Whether one channel is sandboxed.
     *
     * <p>The App Store channel always is. The sandbox is mandatory there, so
     * honouring an explicit {@code macos.sandbox=false} would build a package
     * that is rejected at submission -- days later, by an email. It is honoured
     * for Developer ID, where turning it off is a legitimate choice, and the
     * override is reported in {@link #getWarnings()} rather than applied
     * silently.</p>
     */
    public boolean isSandboxedFor(String channel) {
        if (DISTRIBUTION_APP_STORE.equals(channel)) {
            return true;
        }
        if (sandbox != null) {
            return sandbox.booleanValue();
        }
        return false;
    }

    /**
     * The documented {@code macos.entitlements.*} family (also accepted spelled
     * {@code macNative.entitlements.*}), resolved for one channel.
     *
     * <p>Resolved here rather than at parse time because two of the defaults
     * follow the channel: the sandbox is required for the App Store, and the
     * hardened runtime is what notarization requires of Developer ID.</p>
     */
    public EntitlementOverrides entitlementsFor(String channel) {
        boolean appStore = DISTRIBUTION_APP_STORE.equals(channel);
        // The sandbox is not negotiable on the App Store channel, so it is read
        // back from isSandboxedFor rather than from the hint -- see the warning
        // parse() records when the two disagree.
        int calendars = entTri(source, "personalInformation.calendars");
        if (calendars == EntitlementOverrides.UNSET) {
            // Inherited from the iOS side the same way the Catalyst builder does
            // it: the sandbox calendars entitlement gates all EventKit access, so
            // any calendar or reminder usage description implies it -- including
            // write-only and reminders-only apps.
            boolean needsCalendar = source.get("ios.NSCalendarsUsageDescription", null) != null
                    || source.get("ios.NSCalendarsFullAccessUsageDescription", null) != null
                    || source.get("ios.NSCalendarsWriteOnlyAccessUsageDescription", null) != null
                    || source.get("ios.NSRemindersUsageDescription", null) != null
                    || source.get("ios.NSRemindersFullAccessUsageDescription", null) != null;
            if (needsCalendar) {
                calendars = EntitlementOverrides.ON;
            }
        }
        return new EntitlementOverrides(
                appStore || entBool(source, "appSandbox", isSandboxedFor(channel)),
                entBool(source, "network.client", true),
                entTri(source, "network.server"),
                entString(source, "files.userSelected", "readwrite").toLowerCase(),
                entBool(source, "hardenedRuntime", !appStore && hardenedRuntime),
                entBool(source, "allowJit", false),
                entString(source, "extra", null),
                entTri(source, "device.camera"),
                entTri(source, "device.microphone"),
                entTri(source, "device.bluetooth"),
                entTri(source, "personalInformation.location"),
                calendars,
                entTri(source, "files.downloads"));
    }

    /**
     * One usage-description string, or null.
     *
     * <p>{@code macos.NS...} wins, then the iOS spelling, which is what the
     * feature catalog and existing settings files populate. Falling back to it
     * matters because these strings are the same sentence on both platforms and
     * nobody writes them twice.</p>
     */
    public String getUsageDescription(String key) {
        return first(source.get("macos." + key, null),
                source.get("macNative." + key, null),
                source.get("ios." + key, null));
    }

    /// Tri-state as an int rather than a nullable Boolean: "unset" is a third
    /// answer here, not an absent one, and every caller has to distinguish it
    /// from false. A null Boolean would say the same thing and unbox into a
    /// crash at the one call site that forgets.
    private static int entTri(HintSource src, String suffix) {
        String v = src.get("macos.entitlements." + suffix, null);
        if (v == null || v.length() == 0) {
            v = src.get("macNative.entitlements." + suffix, null);
        }
        if (v == null || v.length() == 0) {
            return EntitlementOverrides.UNSET;
        }
        return isTrue(v) ? EntitlementOverrides.ON : EntitlementOverrides.OFF;
    }

    /**
     * The resolved {@code macos.entitlements.*} settings for one channel.
     *
     * <p>The device and personal-information settings are tri-state: unset means
     * "follow what the application was detected to use", which is the default and
     * the reason a build does not have to declare a capability it obviously
     * needs. Each is read through a resolver that takes the detected value, so an
     * explicit setting overrides the scan in both directions -- an app reaching
     * the camera through a cn1lib the scanner cannot see can ask for the
     * entitlement, and one that links the API without using it can decline the
     * permission prompt.</p>
     */
    public static final class EntitlementOverrides {
        static final int UNSET = -1;
        static final int OFF = 0;
        static final int ON = 1;

        private final boolean sandbox;
        private final boolean networkClient;
        private final int networkServer;
        private final String filesUserSelected;
        private final boolean hardenedRuntime;
        private final boolean allowJit;
        private final String extra;
        private final int camera;
        private final int microphone;
        private final int bluetooth;
        private final int location;
        private final int calendars;
        private final int filesDownloads;

        EntitlementOverrides(boolean sandbox, boolean networkClient, int networkServer,
                String filesUserSelected, boolean hardenedRuntime, boolean allowJit, String extra,
                int camera, int microphone, int bluetooth, int location, int calendars,
                int filesDownloads) {
            this.sandbox = sandbox;
            this.networkClient = networkClient;
            this.networkServer = networkServer;
            this.filesUserSelected = filesUserSelected;
            this.hardenedRuntime = hardenedRuntime;
            this.allowJit = allowJit;
            this.extra = extra;
            this.camera = camera;
            this.microphone = microphone;
            this.bluetooth = bluetooth;
            this.location = location;
            this.calendars = calendars;
            this.filesDownloads = filesDownloads;
        }

        /** The defaults, for a caller that has no hints to resolve against. */
        public static EntitlementOverrides defaults(boolean appStore, boolean sandboxed) {
            return new EntitlementOverrides(sandboxed, true, UNSET, "readwrite", !appStore, false,
                    null, UNSET, UNSET, UNSET, UNSET, UNSET, UNSET);
        }

        public boolean isSandbox() {
            return sandbox;
        }

        public boolean isNetworkClient() {
            return networkClient;
        }

        /** {@code readwrite}, {@code readonly} or anything else for none. */
        public String getFilesUserSelected() {
            return filesUserSelected;
        }

        public boolean isHardenedRuntime() {
            return hardenedRuntime;
        }

        public boolean isAllowJit() {
            return allowJit;
        }

        /** Free-form XML inserted verbatim into the entitlements dict, or null. */
        public String getExtra() {
            return extra;
        }

        public boolean networkServer(boolean detected) {
            return resolve(networkServer, detected);
        }

        /**
         * Read/write access to the Downloads folder, which is a capability of
         * its own and not part of files.userSelected.
         */
        public boolean filesDownloads(boolean detected) {
            return resolve(filesDownloads, detected);
        }

        public boolean camera(boolean detected) {
            return resolve(camera, detected);
        }

        public boolean microphone(boolean detected) {
            return resolve(microphone, detected);
        }

        public boolean bluetooth(boolean detected) {
            return resolve(bluetooth, detected);
        }

        public boolean location(boolean detected) {
            return resolve(location, detected);
        }

        public boolean calendars(boolean detected) {
            return resolve(calendars, detected);
        }

        private static boolean resolve(int override, boolean detected) {
            return override == UNSET ? detected : override == ON;
        }
    }

}
