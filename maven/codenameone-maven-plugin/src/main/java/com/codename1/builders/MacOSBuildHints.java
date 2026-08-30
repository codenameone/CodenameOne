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
    private boolean signingConfigured;
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
        // No explicit choice means "whatever this project produced before".
        //
        // MacNativeBuilder defaults macNative.distribution to appStore, so a
        // Catalyst project that never set the hint has been producing an App
        // Store pkg. Defaulting to developerID for it would quietly change the
        // artifact on the build where its plugin was upgraded -- a dmg instead
        // of the pkg, signed with a Developer ID certificate it may not even
        // have -- which is exactly the "builds today, builds tomorrow, without
        // editing anything" promise this class is here to keep.
        //
        // macNative.enabled is what a Catalyst project carries and what
        // IPhoneBuilder still reads to switch that slice on, so it identifies
        // the projects with something to preserve. A project without it is new
        // to this target and takes developerID, which is the right default for
        // a Mac application that is not going to the store: it needs no App
        // Store credentials to produce something runnable.
        String dist = hint(src, "distribution", null);
        if (dist == null || dist.length() == 0) {
            dist = isTrue(src.get("macNative.enabled", null))
                    ? DISTRIBUTION_APP_STORE : DISTRIBUTION_DEVELOPER_ID;
        }
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
        requireValidBundleId(bundleId);

        minDeploymentTarget = hint(src, "minDeploymentTarget", DEFAULT_DEPLOYMENT_TARGET);
        requireValidDeploymentTarget(minDeploymentTarget);
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
        // Whether the DEVELOPER configured signing, as opposed to the defaults
        // this class supplies for an actual build.
        //
        // getSigningIdentityFor() answers "Developer ID Application" when
        // nothing is set, which is the right certificate to look for on a build
        // machine and the wrong thing to write into an exported Xcode project:
        // it would tell a developer's project to sign manually with a
        // certificate they may not have, where the template's ad-hoc "-" builds
        // and runs. Re-resolved with a null default so this reports presence
        // rather than the value.
        signingConfigured = hint(src, "signingIdentity", null) != null
                || hint(src, "signingIdentity.appStore", null) != null
                || hint(src, "signingIdentity.developerID", null) != null
                || hint(src, "signing.style", null) != null
                || (teamId != null && teamId.trim().length() > 0);
        // Screenshot CI is the only consumer: a window whose size the app cannot
        // change is what makes a strict pixel comparison meaningful.
        fixedWindowSize = hint(src, "fixedWindowSize", null);

        String packagingRaw = hint(src, "packaging", null);
        packagingExplicit = packagingRaw != null && packagingRaw.length() > 0;
        // Normalized once, here, the way files.userSelected already is. The
        // builder compares this value with equalsIgnoreCase, so macos.packaging
        // =PKG genuinely produces a package -- while every comparison in this
        // class was spelled lowercase and quietly disagreed with it. That split
        // let an uppercase spelling slip past the guard below and past the
        // two-package check in getInstallerIdentityFor, handing one installer
        // certificate to both channels: the exact case that guard exists for.
        packaging = packagingExplicit
                ? packagingRaw.trim().toLowerCase()
                : defaultPackagingFor(distribution);

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
        if (packagingExplicit && buildsAppStoreChannel()
                && !"pkg".equals(packaging) && !"both".equals(packaging)) {
            // The value as the developer wrote it, not the normalized one -- the
            // whole job of this warning is to name the hint they typed.
            warnings.add("macos.packaging=" + packagingRaw + " is ignored for the App Store "
                    + "channel, which is always packaged as a pkg; a dmg or a zipped .app "
                    + "cannot be submitted. It is honored for the Developer ID channel.");
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

    /// Apple bundle identifiers are alphanumerics, hyphen and period, and
    /// nothing here accepts anything else.
    ///
    /// Not cosmetic. The identifier is interpolated into project.pbxproj as
    /// PRODUCT_BUNDLE_IDENTIFIER = "..." and passed to xcodebuild, and the
    /// settings it comes from are supplied with the build rather than written
    /// here. A value carrying a quote and a newline closes that string and
    /// continues the file as more project objects -- including a shell script
    /// build phase, which the xcodebuild run that follows would then execute on
    /// the build host.
    ///
    /// REJECTED rather than escaped or stripped. An escape has to be right
    /// against every consumer -- the pbxproj, the Info.plist, an argv element
    /// -- and being wrong in one of them puts the hole back, while stripping
    /// silently builds an application under an identifier its author did not
    /// choose. An identifier outside this set cannot be signed or submitted
    /// anyway, so there is no build being taken away, only one that would have
    /// failed later and less clearly.
    /// A macOS version number, and nothing else.
    ///
    /// The same hole the bundle identifier had, in the second of the two hints
    /// that reach project.pbxproj: the value is written there as
    /// MACOSX_DEPLOYMENT_TARGET = <value>;, so a semicolon, a brace or a
    /// newline ends that setting and continues the file as more project
    /// content -- including a build phase the xcodebuild run that follows would
    /// execute on the build host. The settings arrive with the request.
    ///
    /// Digits and dots, which is every deployment target Apple accepts (11,
    /// 11.0, 13.1.2). Refused rather than escaped, for the reason
    /// requireValidBundleId gives.
    static void requireValidDeploymentTarget(String target) {
        if (target == null || target.length() == 0) {
            return;
        }
        boolean lastWasDot = true;
        for (int i = 0; i < target.length(); i++) {
            char c = target.charAt(i);
            boolean digit = c >= '0' && c <= '9';
            if (!digit && c != '.') {
                throw invalidDeploymentTarget(target);
            }
            if (c == '.' && lastWasDot) {
                throw invalidDeploymentTarget(target);
            }
            lastWasDot = c == '.';
        }
        if (lastWasDot) {
            throw invalidDeploymentTarget(target);
        }
    }

    private static IllegalArgumentException invalidDeploymentTarget(String target) {
        return new IllegalArgumentException(
                "Invalid deployment target " + describeForError(target)
                + ": macos.minDeploymentTarget must be a version number such"
                + " as 11.0.");
    }

    static void requireValidBundleId(String id) {
        if (id == null || id.length() == 0) {
            return;
        }
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                    || (c >= '0' && c <= '9') || c == '-' || c == '.';
            if (!ok) {
                throw new IllegalArgumentException(
                        "Invalid bundle identifier " + describeForError(id)
                        + ": Apple bundle identifiers may contain only letters,"
                        + " digits, hyphen and period. Set a valid"
                        + " macos.bundleId (or package name) and build again.");
            }
        }
    }

    /// The offending value, with anything unprintable escaped so a crafted
    /// identifier cannot forge log lines on its way into the build output.
    private static String describeForError(String id) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < id.length() && i < 120; i++) {
            char c = id.charAt(i);
            if (c < 0x20 || c > 0x7e || c == '"' || c == '\\') {
                b.append("\\u");
                String hex = Integer.toHexString(c);
                for (int p = hex.length(); p < 4; p++) {
                    b.append('0');
                }
                b.append(hex);
            } else {
                b.append(c);
            }
        }
        return b.append('"').toString();
    }

    public String getBundleId() {
        return bundleId;
    }

    public String getMinDeploymentTarget() {
        return minDeploymentTarget;
    }

    /// The Xcode configuration to archive. Reads the legacy macNative. spelling
    /// too, like every other setting here -- the builder used to look
    /// macos.configuration up directly, so a project saying
    /// macNative.configuration=Debug was silently archived as Release.
    public String getConfiguration() {
        return hint(source, "configuration", "Release");
    }

    /// The architectures to compile, as an ARCHS value. Same alias rule, and the
    /// same defect before it: macNative.arch=x86_64 was ignored and the build
    /// came out universal.
    public String getArch() {
        return hint(source, "arch", "arm64 x86_64");
    }

    public String getAppCategory() {
        return appCategory;
    }

    public String getCopyright() {
        return copyright;
    }

    /// Whether the developer configured signing at all, rather than taking the
    /// defaults this class supplies for a build it performs itself.
    public boolean isSigningConfigured() {
        return signingConfigured;
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
        String perChannel = perChannelInstallerIdentity(channel);
        if (perChannel != null) {
            return perChannel;
        }
        // Refused only when TWO packages would be signed with it. The App Store
        // takes a "3rd Party Mac Developer Installer" certificate and Developer
        // ID takes a "Developer ID Installer" one, so one value cannot be right
        // for both -- but that is a conflict about packages, not about channels.
        // A distribution=both build on the default packaging produces a pkg for
        // the store and a dmg for direct download, so exactly one package is
        // signed and the shared hint is the right answer for it. Counting
        // channels instead rejected that perfectly valid configuration and made
        // buildPkg fail for a missing identity the developer had supplied.
        //
        // A channel that named its own installer certificate is not a claimant
        // on this one either, for the same reason: it already has an answer and
        // will never reach here. So an App Store override standing beside a
        // shared Developer ID Installer leaves exactly one package on the shared
        // value, and counting the packaged channels rather than the sharing ones
        // refused it -- failing buildPkg for a certificate the developer had in
        // fact supplied, which is the same defect one level in.
        int sharingChannels = 0;
        for (String channel2 : getChannels()) {
            String packagingFor = getPackagingFor(channel2);
            if (!"pkg".equals(packagingFor) && !"both".equals(packagingFor)) {
                continue;
            }
            if (perChannelInstallerIdentity(channel2) == null) {
                sharingChannels++;
            }
        }
        if (sharingChannels > 1) {
            return null;
        }
        return installerIdentity;
    }

    /**
     * The installer certificate named for exactly this channel, or null when it
     * named none. Blank counts as none, the same as absent -- an empty hint is
     * how a settings file spells "unset".
     */
    private String perChannelInstallerIdentity(String channel) {
        String value = DISTRIBUTION_APP_STORE.equals(channel)
                ? installerIdentityAppStore : installerIdentityDeveloperID;
        return value != null && value.trim().length() > 0 ? value : null;
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

    /**
     * The build number written as CFBundleVersion.
     *
     * <p>{@code macos.bundleVersion}, then the legacy {@code macNative.}
     * spelling like every other setting on this class, then
     * {@code ios.bundleVersion} -- which is what an existing settings file for a
     * Catalyst project actually carries, that build having been an iOS build.
     * The builder used to read the first and third directly and skip the second,
     * so a project migrating on the promised legacy spelling silently fell
     * through to the application version and shipped a build number it did not
     * choose -- a duplicate one, if the application version had not moved.</p>
     *
     * @param applicationVersion the value to use when no build number is set
     */
    public String getBundleVersion(String applicationVersion) {
        return hint(source, "bundleVersion",
                source.get("ios.bundleVersion", applicationVersion));
    }

    /**
     * Which native theme the generated stub installs, as
     * IOSImplementation.setIosMode() takes it.
     *
     * <p>{@code modern} by DEFAULT, which is where macOS parts company with iOS.
     * iOS defaults to {@code auto} and therefore to the legacy iOS 7 theme
     * deliberately, so that existing applications and their screenshot goldens
     * are not disturbed. This port has neither: it has never shipped, so there
     * is nothing to disturb, and defaulting it to a pre-flat iOS theme would
     * give a brand new macOS port an iPhone 7 look and -- because iOS7Theme.res
     * carries no $Dark styles at all -- no dark mode whatsoever, however
     * carefully the application asks for one.</p>
     *
     * <p>{@code macos.themeMode} names it directly, the legacy macNative.
     * spelling is accepted like every other setting here, and the cross platform
     * {@code nativeTheme} meta hint is honoured with the same legacy/modern
     * translation IPhoneBuilder applies.</p>
     */
    public String getThemeMode() {
        String mode = hint(source, "themeMode", null);
        if (mode == null) {
            String shared = source.get("nativeTheme", source.get("cn1.nativeTheme", null));
            mode = "legacy".equalsIgnoreCase(shared) ? "ios7" : "modern";
        }
        // Interpolated into generated Java source, so it is constrained to the
        // vocabulary the runtime understands rather than passed through. A hint
        // reaching a source file is an injection site, and this one comes
        // straight from an uploaded settings file.
        for (int iter = 0; iter < THEME_MODES.length; iter++) {
            if (THEME_MODES[iter].equalsIgnoreCase(mode)) {
                return THEME_MODES[iter];
            }
        }
        return "modern";
    }

    /// Every value IOSImplementation.installNativeTheme() acts on.
    private static final String[] THEME_MODES = {
        "modern", "liquid", "material", "ios7", "flat", "auto",
    };

    /**
     * The remaining settings the builders used to read straight off the request.
     *
     * <p>Each one skipped the legacy {@code macNative.} spelling this class
     * promises everywhere else, so a project migrating off Catalyst on the
     * documented name silently got the default. Three of them were reported
     * separately -- the archive configuration, the architectures and the bundle
     * version -- before it was worth reading the rest as one class of defect
     * rather than waiting for each to be found on its own.</p>
     *
     * <p>The {@code ios.} fallback each of them already had is preserved, as the
     * resolver's default, so the order stays macos, then macNative, then the iOS
     * spelling an existing settings file actually carries.</p>
     */
    /**
     * AES-GCM, ON by default because that is what iOS actually ships.
     *
     * <p>IPhoneBuilder uncomments {@code //#define CN1_INCLUDE_CRYPTO}, and that
     * string is a strict PREFIX of {@code //#define CN1_INCLUDE_CRYPTO_GCM}
     * sitting on the next line, while replaceInFile is an unrestricted
     * String.replace. So every iOS application that touches
     * com.codename1.security gets GCM whether or not it asked for it. Nothing in
     * this repository sets ios.crypto.gcm and CryptoApiTest's AES-GCM round trip
     * passes on iOS regardless, which is the proof.</p>
     *
     * <p>macOS parks the GCM directive under a placeholder so the base
     * replacement cannot reach it, which is correct -- and with an opt-in
     * default of false it made the same application work on iOS and fail on
     * macOS with CN1_CRYPTO_E_UNSUPPORTED, surfacing as "crypto operation failed
     * with code -5". Hence the default here is true; {@code macos.crypto.gcm
     * =false} still trims the symbols for an application that wants that.</p>
     */
    public String getCryptoGcm() {
        return hint(source, "crypto.gcm", source.get("ios.crypto.gcm", "true"));
    }

    /** @see #getCryptoGcm() */
    public String getAddLibs() {
        return hint(source, "add_libs", source.get("ios.add_libs", null));
    }

    /** @see #getCryptoGcm() */
    public boolean isSourceOnly() {
        return "true".equalsIgnoreCase(hint(source, "sourceOnly", "false"));
    }

    /**
     * Whether the application loads code it did not ship, which a hardened
     * bundle needs the library-validation exception for.
     *
     * @see #getCryptoGcm()
     */
    public boolean isLoadsExternalCode() {
        return "true".equalsIgnoreCase(hint(source, "loadsExternalCode", "false"));
    }

    /** @see #getCryptoGcm() */
    public String getUrlSchemes() {
        return hint(source, "urlSchemes",
                source.get("ios.urlSchemes", source.get("ios.urlScheme", null)));
    }

    /** @see #getCryptoGcm() */
    public String getPlistInject() {
        return hint(source, "plistInject", source.get("ios.plistInject", null));
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
     *
     * <p>With one exception, for the same reason the sandbox has one: the App
     * Store channel is always packaged as a pkg. A dmg or a zipped .app cannot
     * be submitted at all, so honouring the override there would spend a full
     * build producing an artifact whose only defect is discovered by hand, at
     * upload time. The override is reported through {@link #getWarnings()}
     * rather than applied silently, and it still applies to the Developer ID
     * channel of the same build.</p>
     */
    public String getPackagingFor(String channel) {
        if (DISTRIBUTION_APP_STORE.equals(channel)) {
            return "pkg";
        }
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
                entTri(source, "files.downloads"),
                // Spelled apsEnvironment rather than aps-environment: the hint
                // namespace is dotted, so a dash in the key reads as part of a
                // path segment. Carried as a STRING, because the value is what a
                // locally signed build needs to change -- an Apple Development
                // profile wants "development" and a tri-state boolean had no way
                // to say it.
                entString(source, "apsEnvironment", null),
                // The BUILD SETTING, not the entitlements hint above it.
                hardenedRuntime);
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
        /// macos.entitlements.hardenedRuntime -- the JIT exceptions, NOT the
        /// ENABLE_HARDENED_RUNTIME build setting. See isJitExceptionsDeclared().
        private final boolean jitExceptionsDeclared;
        /// macos.hardenedRuntime -- the build setting the project is signed with.
        private final boolean hardenedRuntimeBuildSetting;
        private final boolean allowJit;
        private final String extra;
        private final int camera;
        private final int microphone;
        private final int bluetooth;
        private final int location;
        private final int calendars;
        private final int filesDownloads;
        private final String apsEnvironment;

        EntitlementOverrides(boolean sandbox, boolean networkClient, int networkServer,
                String filesUserSelected, boolean jitExceptionsDeclared, boolean allowJit,
                String extra, int camera, int microphone, int bluetooth, int location,
                int calendars, int filesDownloads, String apsEnvironment,
                boolean hardenedRuntimeBuildSetting) {
            this.sandbox = sandbox;
            this.networkClient = networkClient;
            this.networkServer = networkServer;
            this.filesUserSelected = filesUserSelected;
            this.jitExceptionsDeclared = jitExceptionsDeclared;
            this.hardenedRuntimeBuildSetting = hardenedRuntimeBuildSetting;
            this.allowJit = allowJit;
            this.extra = extra;
            this.camera = camera;
            this.microphone = microphone;
            this.bluetooth = bluetooth;
            this.location = location;
            this.calendars = calendars;
            this.filesDownloads = filesDownloads;
            this.apsEnvironment = apsEnvironment;
        }

        /** The defaults, for a caller that has no hints to resolve against. */
        public static EntitlementOverrides defaults(boolean appStore, boolean sandboxed) {
            return new EntitlementOverrides(sandboxed, true, UNSET, "readwrite", !appStore, false,
                    null, UNSET, UNSET, UNSET, UNSET, UNSET, UNSET, null, !appStore);
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

        /**
         * Whether the signature declares the JIT exceptions, from
         * {@code macos.entitlements.hardenedRuntime}.
         *
         * <p>Named for what it does, because the hint's name does not: it is NOT
         * the ENABLE_HARDENED_RUNTIME build setting, and reading it as one put
         * the resource entitlements behind the wrong flag once already. That
         * setting is {@code macos.hardenedRuntime}; ask
         * {@link #isHardenedRuntimeBuildSetting()} for it.</p>
         */
        public boolean isJitExceptionsDeclared() {
            return jitExceptionsDeclared;
        }

        /**
         * Whether the project is signed with the hardened runtime, from
         * {@code macos.hardenedRuntime} -- the same value MacOSNativeBuilder
         * passes as ENABLE_HARDENED_RUNTIME.
         *
         * <p>Carried here so the entitlements can be decided from what the
         * binary is actually signed with rather than from a similarly named
         * hint that means something else.</p>
         */
        public boolean isHardenedRuntimeBuildSetting() {
            return hardenedRuntimeBuildSetting;
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

        /// Whether the build declares the APNs entitlement.
        ///
        /// Unlike the sandbox entitlements around it this one is not a sandbox
        /// permission, so it is emitted for a Developer ID build too: it is what
        /// makes registerForRemoteNotifications succeed at all, and macOS
        /// refuses registration for a signed executable that does not carry it.
        ///
        /// {@code macos.entitlements.apsEnvironment} answers both questions at
        /// once -- whether to declare it and which environment to declare -- so
        /// {@code false} or {@code none} suppresses it and anything else selects
        /// a value. One key, because "which APNs environment, or none" is one
        /// question.
        public boolean push(boolean detected) {
            if (apsEnvironment == null || apsEnvironment.length() == 0) {
                return detected;
            }
            return !"false".equalsIgnoreCase(apsEnvironment)
                    && !"none".equalsIgnoreCase(apsEnvironment);
        }

        /// The aps-environment value, {@code production} unless the hint asks for
        /// {@code development}.
        ///
        /// Production is right for both channels this builder ships, which are
        /// distribution channels. Development exists for the mac-source project
        /// a developer opens in Xcode and signs with an Apple Development
        /// profile: production does not match that profile, so registration and
        /// signing both fail, and hard-coding it left no way to say so.
        public String getApsEnvironment() {
            return "development".equalsIgnoreCase(apsEnvironment) ? "development" : "production";
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
