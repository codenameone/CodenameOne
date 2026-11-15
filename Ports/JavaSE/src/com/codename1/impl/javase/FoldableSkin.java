/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
 * Please contact Codename One through http://www.codenameone.com/ if
 * you need additional information or have any questions.
 */
package com.codename1.impl.javase;

import com.codename1.io.Properties;
import com.codename1.ui.DevicePosture;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// The foldable description of a simulator skin, read from the optional
/// `foldable*` keys of `skin.properties`. A skin without `foldable=true`
/// yields no instance at all, so every legacy skin behaves exactly as before.
///
/// Keys (all coordinates in skin image pixels):
///
/// - `foldable=true`
/// - `foldOrientation=vertical|horizontal` - a book fold (hinge runs top to
///   bottom) or a flip fold (hinge runs left to right). Default vertical.
/// - `foldType=continuous|dualScreen` - one flexible panel, or two panels
///   with a physical gap between them. Default continuous.
/// - `hingePortrait=x,y,w,h` and `hingeLandscape=x,y,w,h` - the hinge in the
///   portrait and landscape skin images. A continuous fold may give a zero
///   width (vertical) or zero height (horizontal); the crease is then the
///   centre line of the screen when the key is absent.
/// - `foldPostures=flat,half,closed` - the postures the device supports, in
///   the order the simulator menu lists them. Default all three.
/// - `crease=true` - draw a faint crease on the flat inner screen.
/// - `coverPortraitScreenX/Y/Width/Height`, `coverLandscapeScreenX/...` and
///   `coverPpi` - the cover (outer) display, in the `cover.skin` and
///   `cover.skin_l` images. A device with no cover display omits them, and
///   its closed posture then shows the back of the device with a dark screen.
/// - `coverCutouts` / `coverSafePortraitX...` - as the inner keys, prefixed.
final class FoldableSkin {

    /// Below this hinge angle a device reads as closed, above
    /// `FLAT_THRESHOLD` as flat, in between as half opened. These are the
    /// thresholds Jetpack WindowManager reports postures with.
    static final int CLOSED_THRESHOLD = 30;
    static final int FLAT_THRESHOLD = 150;

    static final String POSTURE_FLAT = "flat";
    static final String POSTURE_HALF = "half";
    static final String POSTURE_CLOSED = "closed";

    private final boolean vertical;
    private final boolean dualScreen;
    private final boolean crease;
    private final Rectangle hingePortrait;
    private final Rectangle hingeLandscape;
    private final List<String> postures;
    private final Rectangle coverPortraitScreen;
    private final Rectangle coverLandscapeScreen;
    private final double coverPpi;

    private FoldableSkin(Properties p) {
        vertical = !"horizontal".equalsIgnoreCase(p.getProperty("foldOrientation", "vertical").trim());
        dualScreen = "dualScreen".equalsIgnoreCase(p.getProperty("foldType", "continuous").trim());
        crease = "true".equalsIgnoreCase(p.getProperty("crease", "false").trim());
        hingePortrait = parseRect(p.getProperty("hingePortrait"));
        hingeLandscape = parseRect(p.getProperty("hingeLandscape"));
        List<String> list = new ArrayList<String>();
        String spec = p.getProperty("foldPostures", POSTURE_FLAT + "," + POSTURE_HALF + "," + POSTURE_CLOSED);
        for (String s : spec.split(",")) {
            String name = s.trim();
            if ((POSTURE_FLAT.equalsIgnoreCase(name) || POSTURE_HALF.equalsIgnoreCase(name)
                    || POSTURE_CLOSED.equalsIgnoreCase(name)) && !containsIgnoreCase(list, name)) {
                list.add(asciiLower(name));
            }
        }
        if (list.isEmpty()) {
            list.add(POSTURE_FLAT);
        }
        postures = Collections.unmodifiableList(list);
        coverPortraitScreen = parseScreen(p, "coverPortraitScreen");
        coverLandscapeScreen = parseScreen(p, "coverLandscapeScreen");
        double ppi = 0;
        String ppiSpec = p.getProperty("coverPpi");
        if (ppiSpec != null) {
            try {
                ppi = Double.parseDouble(ppiSpec.trim());
            } catch (NumberFormatException err) {
                ppi = 0;
            }
        }
        coverPpi = ppi;
    }

    /// Returns the foldable description of a skin, or null when the skin is
    /// not foldable.
    static FoldableSkin parse(Properties p) {
        if (p == null || !"true".equalsIgnoreCase(p.getProperty("foldable", "false").trim())) {
            return null;
        }
        return new FoldableSkin(p);
    }

    boolean isVertical() {
        return vertical;
    }

    int getFoldOrientation() {
        return vertical ? DevicePosture.FOLD_ORIENTATION_VERTICAL : DevicePosture.FOLD_ORIENTATION_HORIZONTAL;
    }

    boolean isDualScreen() {
        return dualScreen;
    }

    boolean hasCrease() {
        return crease;
    }

    List<String> getPostures() {
        return postures;
    }

    boolean supports(String posture) {
        return postures.contains(posture);
    }

    boolean hasCover() {
        return coverPortraitScreen != null;
    }

    boolean hasCoverLandscape() {
        return coverLandscapeScreen != null;
    }

    /// The cover display in the cover skin image, or null without a cover.
    Rectangle getCoverScreen(boolean portrait) {
        Rectangle r = portrait || coverLandscapeScreen == null ? coverPortraitScreen : coverLandscapeScreen;
        return r == null ? null : new Rectangle(r);
    }

    double getCoverPpi() {
        return coverPpi;
    }

    /// The hinge in skin image coordinates, or null when the skin gives
    /// none for that orientation.
    Rectangle getHinge(boolean portrait) {
        Rectangle r = portrait || hingeLandscape == null ? hingePortrait : hingeLandscape;
        return r == null ? null : new Rectangle(r);
    }

    /// The posture a hinge angle reads as, a `DevicePosture` POSTURE_ constant.
    static int postureForAngle(int angle) {
        if (angle < CLOSED_THRESHOLD) {
            return DevicePosture.POSTURE_CLOSED;
        }
        if (angle > FLAT_THRESHOLD) {
            return DevicePosture.POSTURE_FLAT;
        }
        return DevicePosture.POSTURE_HALF_OPENED;
    }

    /// The resting hinge angle of a named posture.
    static int angleForPosture(String posture) {
        if (POSTURE_CLOSED.equals(posture)) {
            return 0;
        }
        if (POSTURE_HALF.equals(posture)) {
            return 90;
        }
        return 180;
    }

    /// The fold in display coordinates, as `DevicePosture.getFoldBounds`
    /// answers it. `screen` is the inner screen rectangle in skin pixels and
    /// `displayWidth`/`displayHeight` the logical display size. A dual
    /// screen device answers its real gap; a continuous one a two pixel line
    /// at the hinge (the centre without a hinge key).
    Rectangle foldBounds(boolean portrait, Rectangle screen, int displayWidth, int displayHeight) {
        Rectangle hinge = getHinge(portrait);
        boolean alongX = vertical == portrait;
        if (hinge == null || screen == null || screen.width <= 0 || screen.height <= 0) {
            return alongX
                    ? new Rectangle(displayWidth / 2 - 1, 0, 2, displayHeight)
                    : new Rectangle(0, displayHeight / 2 - 1, displayWidth, 2);
        }
        if (alongX) {
            double sx = displayWidth / (double) screen.width;
            int x = (int) Math.round((hinge.x - screen.x) * sx);
            int w = dualScreen ? Math.max(2, (int) Math.round(hinge.width * sx)) : 2;
            if (!dualScreen) {
                x = (int) Math.round((hinge.x + hinge.width / 2.0 - screen.x) * sx) - 1;
            }
            return new Rectangle(clamp(x, 0, displayWidth - w), 0, w, displayHeight);
        }
        double sy = displayHeight / (double) screen.height;
        int y = (int) Math.round((hinge.y - screen.y) * sy);
        int h = dualScreen ? Math.max(2, (int) Math.round(hinge.height * sy)) : 2;
        if (!dualScreen) {
            y = (int) Math.round((hinge.y + hinge.height / 2.0 - screen.y) * sy) - 1;
        }
        return new Rectangle(0, clamp(y, 0, displayHeight - h), displayWidth, h);
    }

    private static int clamp(int v, int min, int max) {
        return v < min ? min : (v > max ? max : v);
    }

    private static Rectangle parseScreen(Properties p, String prefix) {
        String x = p.getProperty(prefix + "X");
        String y = p.getProperty(prefix + "Y");
        String w = p.getProperty(prefix + "Width");
        String h = p.getProperty(prefix + "Height");
        if (x == null || y == null || w == null || h == null) {
            return null;
        }
        return parseRect(x + "," + y + "," + w + "," + h);
    }

    static Rectangle parseRect(String spec) {
        if (spec == null) {
            return null;
        }
        String[] parts = spec.trim().split(",");
        if (parts.length != 4) {
            return null;
        }
        try {
            int w = Integer.parseInt(parts[2].trim());
            int h = Integer.parseInt(parts[3].trim());
            if (w < 0 || h < 0) {
                return null;
            }
            return new Rectangle(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()), w, h);
        } catch (NumberFormatException err) {
            return null;
        }
    }

    private static boolean containsIgnoreCase(List<String> list, String s) {
        for (String e : list) {
            if (e.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }

    // Posture names are ASCII by specification; String.toLowerCase is locale
    // sensitive (CLAUDE.md, "Never case-fold a protocol token").
    private static String asciiLower(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return b.toString();
    }
}
