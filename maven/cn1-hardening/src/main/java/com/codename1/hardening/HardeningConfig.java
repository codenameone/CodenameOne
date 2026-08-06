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
package com.codename1.hardening;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The resolved hardening settings for one build, derived from the {@code harden.*}
 * build hints. A level sets the defaults; individual switches override them; and a
 * per-platform switch ({@code harden.<platform>.enabled}) can turn the whole thing
 * off for one target. Nothing here references a builder's {@code BuildRequest}: the
 * caller hands over a plain map of already-resolved hint values so the same config
 * is usable from both the maven plugin and the cloud daemon.
 */
public final class HardeningConfig {
    private final HardeningProfile profile;
    private final boolean renameEnabled;
    private final boolean encryptConstantStrings;
    private final boolean encryptAllStrings;
    private final boolean controlFlow;
    private final boolean platformEnabled;
    private final String platform;
    private final String seed;
    private final List<String> extraKeepRules;

    private HardeningConfig(HardeningProfile profile, boolean renameEnabled,
                            boolean encryptConstantStrings, boolean encryptAllStrings,
                            boolean controlFlow, boolean platformEnabled, String platform,
                            String seed, List<String> extraKeepRules) {
        this.profile = profile;
        this.renameEnabled = renameEnabled;
        this.encryptConstantStrings = encryptConstantStrings;
        this.encryptAllStrings = encryptAllStrings;
        this.controlFlow = controlFlow;
        this.platformEnabled = platformEnabled;
        this.platform = platform;
        this.seed = seed;
        this.extraKeepRules = extraKeepRules;
    }

    /**
     * Builds a config from resolved hint values.
     *
     * @param hints    the {@code harden.*} keys (prefix included), already resolved to their
     *                 string values, e.g. {@code harden.level -> "aggressive"}
     * @param platform one of {@code and|ios|mac|linux|win|javascript|javase|watch|tv}
     * @param renameSupported false for Android, where R8 remains the sole renamer
     */
    public static HardeningConfig from(Map<String, String> hints, String platform, boolean renameSupported) {
        HardeningProfile level = HardeningProfile.parse(get(hints, "harden.level", "off"));
        if (level == null) {
            level = HardeningProfile.OFF;
        }
        boolean platformEnabled = boolTri(get(hints, "harden." + platform + ".enabled", "true"), true);

        boolean rename = renameSupported && boolTri(get(hints, "harden.rename", null), level.renamesByDefault());

        String strings = get(hints, "harden.strings", null);
        boolean encConst;
        boolean encAll;
        if (strings == null) {
            encConst = level.encryptsConstantStringsByDefault();
            encAll = level.encryptsAllStringsByDefault();
        } else {
            String v = strings.trim().toLowerCase();
            if ("off".equals(v) || "false".equals(v) || "0".equals(v)) {
                encConst = false;
                encAll = false;
            } else if ("constants".equals(v) || "1".equals(v)) {
                encConst = true;
                encAll = false;
            } else {
                // "all", "true", "2", "3"
                encConst = true;
                encAll = true;
            }
        }

        boolean cf = boolTri(get(hints, "harden.controlFlow", null), level.controlFlowByDefault());

        String seed = get(hints, "harden.seed", null);

        List<String> keep = new ArrayList<String>();
        String keepRaw = get(hints, "harden.keep", null);
        if (keepRaw != null) {
            // Split only on newlines: a semicolon is legal ProGuard syntax inside a rule body
            // (e.g. "-keep class com.example.Foo { *; }"), so splitting on ';' would shred rules.
            for (String rule : keepRaw.split("\\r?\\n")) {
                String t = rule.trim();
                if (!t.isEmpty()) {
                    keep.add(t);
                }
            }
        }

        return new HardeningConfig(level, rename, encConst, encAll, cf, platformEnabled, platform, seed, keep);
    }

    private static String get(Map<String, String> hints, String key, String def) {
        if (hints == null) {
            return def;
        }
        String v = hints.get(key);
        return v == null ? def : v;
    }

    private static boolean boolTri(String v, boolean def) {
        if (v == null) {
            return def;
        }
        String t = v.trim().toLowerCase();
        if (t.isEmpty()) {
            return def;
        }
        if ("true".equals(t) || "1".equals(t) || "2".equals(t) || "3".equals(t) || "on".equals(t)) {
            return true;
        }
        if ("false".equals(t) || "0".equals(t) || "off".equals(t)) {
            return false;
        }
        return def;
    }

    /** True when any transform should run: the level is on and this platform is not opted out. */
    public boolean isActive() {
        return profile != HardeningProfile.OFF && platformEnabled;
    }

    public HardeningProfile getProfile() {
        return profile;
    }

    public boolean isRenameEnabled() {
        return renameEnabled;
    }

    public boolean isEncryptConstantStrings() {
        return encryptConstantStrings;
    }

    public boolean isEncryptAllStrings() {
        return encryptAllStrings;
    }

    public boolean isAnyStringEncryption() {
        return encryptConstantStrings || encryptAllStrings;
    }

    public boolean isControlFlow() {
        return controlFlow;
    }

    public boolean isPlatformEnabled() {
        return platformEnabled;
    }

    public String getPlatform() {
        return platform;
    }

    public String getSeed() {
        return seed;
    }

    public List<String> getExtraKeepRules() {
        return extraKeepRules;
    }

    /** The transforms actually enabled, for the report and the mapping header. */
    public List<String> enabledTransforms() {
        List<String> t = new ArrayList<String>();
        if (renameEnabled) {
            t.add("rename");
        }
        if (encryptConstantStrings || encryptAllStrings) {
            t.add(encryptAllStrings ? "strings:all" : "strings:constants");
        }
        if (controlFlow) {
            t.add("controlFlow");
        }
        return t;
    }

    @Override
    public String toString() {
        return "HardeningConfig" + Arrays.asList(
                "profile=" + profile, "platform=" + platform, "platformEnabled=" + platformEnabled,
                "rename=" + renameEnabled, "encConst=" + encryptConstantStrings,
                "encAll=" + encryptAllStrings, "controlFlow=" + controlFlow);
    }
}
