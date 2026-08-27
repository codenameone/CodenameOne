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
package com.codename1.build.shared;

import com.codename1.build.shared.BuildHints.Hint;

import java.util.List;

/**
 * Open-ended hint families whose names are built by concatenation, so the set
 * of valid keys is unbounded.
 *
 * <p>These are deliberately <b>not</b> exposed as annotation attributes. Each
 * one is really a map &mdash; permission name to setting, entitlement key to
 * value &mdash; and a Java annotation member cannot express a
 * {@code Map<String,String>}. The shapes that could work (a nested
 * {@code @Permission[]}, or a {@code String[]} of {@code "KEY=VALUE"} pairs)
 * are a materially different design; until one is chosen these are set through
 * {@code codenameone_settings.properties}.</p>
 *
 * <p>They are catalogued anyway because the drift gate needs them: a mined key
 * that matches one of these patterns is accounted for rather than reported as
 * an unknown hint.</p>
 */
final class BuildHintsDynamic {

    private BuildHintsDynamic() {
    }

    static void register(List<Hint> h) {
        family(h, "android.permission.*", "android",
                "true/false. Whether to include a particular permission. Preferred over "
                        + "android.xpermissions because it avoids conflicts with libraries. See "
                        + "Android's Manifest.permission documentation for the full list. The "
                        + "optional .maxSdkVersion suffix becomes the maxSdkVersion attribute of "
                        + "the generated <uses-permission> tag, and .required marks the "
                        + "permission required.");
        family(h, "android.uses_feature.*", "android",
                "Adds a <uses-feature> element named by the suffix.");
        family(h, "android.uses_permission.*", "android",
                "Adds a <uses-permission> element named by the suffix.");
        family(h, "android.playService.*", "android",
                "Opts a single Google Play service in or out. The sibling "
                        + "<name>.minPlayServicesVersion pins its version.");
        family(h, "android.cusom_layout*", "android",
                "Numbered custom layout resources: android.cusom_layout1, android.cusom_layout2 "
                        + "and upward. The misspelling is load-bearing: it's the key the "
                        + "builder actually reads, so correcting it drops the layout with "
                        + "no warning.");
        family(h, "ios.NS*UsageDescription", "ios",
                "Info.plist privacy strings. The commonly used keys are catalogued "
                        + "individually and exposed through @IosPrivacy; this entry covers the "
                        + "open tail that the builder sweeps by prefix.");
        family(h, "ios.entitlements.*", "ios",
                "Adds an arbitrary entitlement key to the generated entitlements file.");
        family(h, "ios.spm.products.*", "ios",
                "Selects which products of a Swift Package Manager package to link, keyed "
                        + "by package identity.");
        family(h, "ios.pods.build.*", "ios",
                "Overrides an Xcode build setting for the generated CocoaPods project.");
        family(h, "ios.home.commissioning.buildSettings.*", "ios",
                "Overrides an Xcode build setting for the Matter commissioning extension.");
        family(h, "ios.surfaces.buildSettings.*", "ios",
                "Overrides an Xcode build setting for the external-surfaces extension.");
        family(h, "ios.*.appext.*", "ios",
                "Per-app-extension signing. ios.debug.appext.<Name>.* and "
                        + "ios.release.appext.<Name>.* are collapsed to unqualified keys before "
                        + "the request is sent.");
        family(h, "harden.*.enabled", "general",
                "Enables or disables hardening for one platform slice.");
        family(h, "harden.*", "general",
                "The whole hardening namespace is swept into the hardening engine's "
                        + "configuration, so a hint added there reaches it without a dedicated "
                        + "reader.");
        family(h, "macNative.provisioningProfile.*", "mac",
                "Per-profile provisioning data for a native macOS build, keyed by profile "
                        + "name.");
        family(h, "var.*", "general",
                "Defines a variable that any other hint can interpolate as ${var.name}, "
                        + "with ${var.name:default} for a fallback.");
    }

    private static void family(List<Hint> h, String pattern, String platform, String doc) {
        h.add(new Hint(pattern)
                .group(HintGroup.NONE)
                .type(HintType.STRING)
                .dynamic(pattern)
                .platform(platform)
                .doc(doc));
    }
}
