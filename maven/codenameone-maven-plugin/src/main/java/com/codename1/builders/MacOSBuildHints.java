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

    private static final String DEFAULT_APP_CATEGORY = "public.app-category.developer-tools";

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
    private Boolean sandbox;
    private boolean hardenedRuntime = true;
    private boolean notarize;
    private String notarizeKeychainProfile;
    private String notarizeAppleId;
    private String notarizeTeamId;
    private String notarizePassword;

    /**
     * Reads one hint, preferring the {@code macos.} spelling and falling back to
     * the legacy {@code macNative.} one.
     */
    public interface HintSource {
        String get(String key, String defaultValue);
    }

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

    /**
     * Parses the hint family. Never throws on a hint it does not recognise; an
     * unusable value is recorded in {@link #getWarnings()} and the default kept,
     * because a build must not fail over a setting whose meaning changed
     * underneath the project.
     */
    public void parse(HintSource src, String packageName) {
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
        // falls back to ios.release.teamId rather than being asked for twice.
        teamId = hint(src, "teamId", src.get("ios.release.teamId", null));

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
        signingIdentityAppStore = hint(src, "signingIdentity.appStore", null);
        signingIdentityDeveloperID = hint(src, "signingIdentity.developerID", null);
        // Screenshot CI is the only consumer: a window whose size the app cannot
        // change is what makes a strict pixel comparison meaningful.
        fixedWindowSize = hint(src, "fixedWindowSize", null);

        packaging = hint(src, "packaging", null);
        if (packaging == null) {
            packaging = defaultPackagingFor(distribution);
        }

        String sandboxHint = hint(src, "sandbox", null);
        // Unset rather than false: the App Store requires the sandbox and direct
        // distribution usually does not want it, so the default follows the
        // channel instead of being one value for both.
        sandbox = sandboxHint == null ? null : Boolean.valueOf(isTrue(sandboxHint));
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
                    + "Remove it, or build mac-catalyst if you need that behavior.");
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

    /** True when the sandbox applies, following the channel unless set explicitly. */
    public boolean isSandboxed() {
        if (sandbox != null) {
            return sandbox.booleanValue();
        }
        return DISTRIBUTION_APP_STORE.equals(distribution) || DISTRIBUTION_BOTH.equals(distribution);
    }

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

    public String getSigningIdentityAppStore() {
        return signingIdentityAppStore;
    }

    public String getSigningIdentityDeveloperID() {
        return signingIdentityDeveloperID;
    }

    public String getFixedWindowSize() {
        return fixedWindowSize;
    }

    public String getPackaging() {
        return packaging;
    }

    public boolean isHardenedRuntime() {
        return hardenedRuntime;
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
}
