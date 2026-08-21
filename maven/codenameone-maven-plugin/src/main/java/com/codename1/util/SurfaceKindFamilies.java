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
package com.codename1.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The size families a {@code surfaces.json} widget kind declares, and the one place that
 * decides which of them are watch complications.
 *
 * <p>Shared by both device builders. The iOS builder needs the classification to decide what
 * the WidgetKit extensions may host; the Android builder needs it to tell a home-screen
 * widget kind from a Wear complication kind. Neither may own a private copy: the rule is
 * subtle enough that three separate call sites once implemented it as
 * {@code startsWith("watch")} and all three got {@code accessoryCircular} wrong, turning a
 * complication into three home-screen widgets. See {@link #normalize(String)}.</p>
 *
 * <p><b>Manifest key.</b> A kind declares its families under the portable {@code families}
 * key. The older {@code iosFamilies} spelling is still honoured and means the same thing --
 * it predates there being a second platform that cared -- and is consulted only when
 * {@code families} is absent.</p>
 */
public final class SurfaceKindFamilies {

    private SurfaceKindFamilies() {
    }

    /**
     * The families a kind's JSON object declares.
     *
     * <p>{@code families} wins outright when present; {@code iosFamilies} is the legacy
     * spelling and is read only in its absence. They are not merged, because a manifest
     * carrying both is far more likely to be mid-migration than to mean the union, and
     * silently unioning would resurrect a family the author had just removed.</p>
     *
     * @param kindJson one entry of the manifest's {@code kinds} array
     * @return the declared family names, never null; entries that are not strings are skipped
     */
    public static List<String> read(Map<String, Object> kindJson) {
        if (kindJson == null) {
            return Collections.emptyList();
        }
        Object declared = kindJson.get("families");
        if (!(declared instanceof List)) {
            declared = kindJson.get("iosFamilies");
        }
        if (!(declared instanceof List)) {
            return Collections.emptyList();
        }
        List<String> out = new ArrayList<String>();
        for (Object family : (List<?>) declared) {
            if (family instanceof String) {
                out.add((String) family);
            }
        }
        return out;
    }

    /**
     * The portable family name for a declaration, resolving the WidgetKit spellings.
     *
     * <p>Normalised in ONE place because several decisions read these names -- the Swift
     * family list, the watch-only classification, whether a kind has any watch family at
     * all, the corner complication's platform guard, and now the Wear codegen -- and three
     * of them once tested {@code startsWith("watch")} directly. So {@code accessoryCircular}
     * was accepted by none of them: the kind looked like an iOS surface and fell through to
     * the systemSmall/Medium/Large default, turning a complication into three home-screen
     * widgets rather than withholding it.</p>
     *
     * <p>ONLY {@code accessoryCorner} maps across. The other accessory spellings are not
     * watch families: {@code accessoryCircular}, {@code accessoryInline} and
     * {@code accessoryRectangular} are the iPhone LOCK-SCREEN families as well as watch
     * ones, and CN1DescriptorWidget.swift renders all three on iOS. Folding them into the
     * watch names withheld a lock-screen widget the manifest had asked for, which is the
     * mirror of the bug this method was added to fix.</p>
     *
     * <p>So the two namings are NOT interchangeable for those three, and
     * {@code accessoryRectangular} already said so: it maps to the portable
     * {@code lockscreen}, not to {@code watchRectangular}. The portable {@code watch*} names
     * mean "complication only"; the WidgetKit spellings mean the WidgetKit family, which on
     * iOS is the lock screen.</p>
     *
     * @param family a declared family name in either spelling
     * @return the portable name
     */
    public static String normalize(String family) {
        if ("accessoryCorner".equals(family)) {
            return "watchCorner";
        }
        return family;
    }

    /**
     * Whether one declared family is a watch complication.
     *
     * @param family a declared family name in either spelling
     * @return true for the four {@code watch*} families
     */
    public static boolean isWatch(String family) {
        return family != null && normalize(family).startsWith("watch");
    }

    /**
     * Whether any declared family is a watch complication. True for a kind that offers both
     * a phone widget and a complication, unlike {@link #isWatchOnly(List)}.
     *
     * @param families the declared families
     * @return true if at least one is a watch family
     */
    public static boolean hasWatchFamily(List<String> families) {
        if (families == null) {
            return false;
        }
        for (String family : families) {
            if (isWatch(family)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether a kind declares complication families and nothing else, so a phone surface has
     * nothing to offer it.
     *
     * <p>An empty declaration is not watch-only: it means the kind took the default, which
     * is the three home-screen sizes.</p>
     *
     * @param families the declared families
     * @return true if every declared family is a watch family
     */
    public static boolean isWatchOnly(List<String> families) {
        if (families == null || families.isEmpty()) {
            return false;
        }
        for (String family : families) {
            if (family != null && !isWatch(family)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Whether a kind reaches a phone surface -- a home or lock screen.
     *
     * <p>An empty declaration counts, because a kind that names no family takes the
     * home-screen default rather than opting out.</p>
     *
     * @param families the declared families
     * @return true if the kind has a phone surface
     */
    public static boolean hasPhoneFamily(List<String> families) {
        return !isWatchOnly(families);
    }
}
