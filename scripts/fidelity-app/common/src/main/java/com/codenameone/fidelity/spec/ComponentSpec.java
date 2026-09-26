/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
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
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.fidelity.spec;

import java.util.ArrayList;
import java.util.List;

/**
 * One component under fidelity test, as parsed from fidelity-tests.yaml. Plain
 * fields only (no records / no generics-heavy API) so it translates cleanly on
 * ParparVM and the JavaScript port as well as running in the simulator.
 */
public class ComponentSpec {
    private String id;
    private String cn1Uiid;
    private String nativeKind;
    private String nativeAndroidKind;
    private int tileWidthPx = -1;
    private int tileHeightPx = -1;
    private String nativeWindowsKind;
    private String nativeMacKind;
    private String nativeGnomeKind;
    private String text;
    private String backdrop;
    private String material;
    private int tileWidthMm = -1;
    private int tileHeightMm = -1;
    private List states = new ArrayList();
    private List platforms = new ArrayList();
    private List frames = new ArrayList();
    private List goldenSets = new ArrayList();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCn1Uiid() {
        return cn1Uiid != null ? cn1Uiid : id;
    }

    public void setCn1Uiid(String cn1Uiid) {
        this.cn1Uiid = cn1Uiid;
    }

    /**
     * Native widget key for the given platform, or null when this component has none there.
     *
     * <p>Every platform is named explicitly and an unrecognised one returns null. The
     * previous form fell through to the iOS key for anything that was not Android, which was
     * harmless while iOS and Android were the only platforms and becomes a trap the moment a
     * third exists: {@code appliesToPlatform} treats a non-null key as "this component runs
     * here", so every mobile row would have claimed to run on every desktop platform, against
     * a UIKit widget key that means nothing there.</p>
     */
    public String getNativeKind(String platformName) {
        if (platformName == null) {
            return null;
        }
        if (platformName.startsWith("ios")) {
            return nativeKind;
        }
        if (platformName.startsWith("and")) {
            return nativeAndroidKind;
        }
        if (platformName.startsWith("win")) {
            return nativeWindowsKind;
        }
        if (platformName.startsWith("mac")) {
            return nativeMacKind;
        }
        if (platformName.startsWith("gnome") || platformName.startsWith("linux")) {
            return nativeGnomeKind;
        }
        return null;
    }

    /// Tile size in LOGICAL PIXELS, for the desktop platforms.
    ///
    /// Millimetres are the right unit on a phone, where physical size is the design unit and
    /// the same widget must occupy the same amount of thumb. Desktop toolkits are specified
    /// the other way round: WinUI in effective pixels at 96dpi, GTK in logical pixels, AppKit
    /// in points. Sizing a desktop tile in mm produces a plausible number that describes a
    /// control no desktop toolkit would ever draw, so the desktop rows carry their own unit
    /// and the mobile rows are left exactly as they were.
    public int getTileWidthPx() {
        return tileWidthPx;
    }

    public void setTileWidthPx(int tileWidthPx) {
        this.tileWidthPx = tileWidthPx;
    }

    public int getTileHeightPx() {
        return tileHeightPx;
    }

    public void setTileHeightPx(int tileHeightPx) {
        this.tileHeightPx = tileHeightPx;
    }

    public String getNativeKindWindows() {
        return nativeWindowsKind;
    }

    public void setNativeKindWindows(String nativeWindowsKind) {
        this.nativeWindowsKind = nativeWindowsKind;
    }

    public String getNativeKindMac() {
        return nativeMacKind;
    }

    public void setNativeKindMac(String nativeMacKind) {
        this.nativeMacKind = nativeMacKind;
    }

    public String getNativeKindGnome() {
        return nativeGnomeKind;
    }

    public void setNativeKindGnome(String nativeGnomeKind) {
        this.nativeGnomeKind = nativeGnomeKind;
    }

    public String getNativeKindIos() {
        return nativeKind;
    }

    public void setNativeKindIos(String nativeKind) {
        this.nativeKind = nativeKind;
    }

    public String getNativeKindAndroid() {
        return nativeAndroidKind;
    }

    public void setNativeKindAndroid(String nativeAndroidKind) {
        this.nativeAndroidKind = nativeAndroidKind;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /**
     * The tile backdrop behind the rendered widget. Accepted values:
     *   a 6-hex colour ("808080") => a solid fill of that colour;
     *   "gradient" => a vertical blue (#1e64ff top) to green (#28c850 bottom) ramp;
     *   "photo"    => the shared glass-backdrop.png asset.
     * When omitted, material glass/lens tests default to "photo" so the Liquid
     * Glass blend has content behind it; every other component defaults to none
     * (a plain tile, returned as null here).
     */
    public String getBackdrop() {
        if (backdrop != null) {
            return backdrop;
        }
        String m = getMaterial();
        if ("glass".equals(m) || "lens".equals(m)) {
            return "photo";
        }
        return null;
    }

    public void setBackdrop(String backdrop) {
        this.backdrop = backdrop;
    }

    /**
     * The comparison intent declared in fidelity-tests.yaml: "normal", "glass" or
     * "lens" (see the spec header). The host comparator picks its scoring mode
     * from this field rather than inferring glass from corner/backdrop
     * heuristics. When omitted, the legacy glass kinds (Tabs, Toolbar, Button,
     * RaisedButton, FlatButton) default to "glass" for compatibility with specs
     * that predate the field; everything else defaults to "normal".
     */
    public String getMaterial() {
        if (material != null && material.length() > 0) {
            return material;
        }
        if ("Tabs".equals(id) || "Toolbar".equals(id) || "Button".equals(id)
                || "RaisedButton".equals(id) || "FlatButton".equals(id)) {
            return "glass";
        }
        return "normal";
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public int getTileWidthMm() {
        return tileWidthMm;
    }

    public void setTileWidthMm(int tileWidthMm) {
        this.tileWidthMm = tileWidthMm;
    }

    public int getTileHeightMm() {
        return tileHeightMm;
    }

    public void setTileHeightMm(int tileHeightMm) {
        this.tileHeightMm = tileHeightMm;
    }

    public List getStates() {
        return states;
    }

    public void setStates(List states) {
        this.states = states;
    }

    public List getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List platforms) {
        this.platforms = platforms;
    }

    /**
     * Animation-frame progress values (0..100, as strings) for deterministic
     * animation validation. A component with a non-empty frames list is captured
     * once per value with its animation frozen at exactly that progress
     * ("&lt;id&gt;_t&lt;value&gt;_&lt;appearance&gt;_cn1.png") instead of the
     * regular per-state render; there is no native golden for frames -- they are
     * regression-compared against committed CN1 frame goldens and validated
     * against the motion model on the host.
     */
    public List getFrames() {
        return frames;
    }

    public void setFrames(List frames) {
        this.frames = frames;
    }

    /**
     * The golden sets (OS design generations, e.g. ios-27-metal) a frames row belongs
     * to; empty = every set. A motion that only one generation plays is captured and
     * validated only in that generation's run.
     */
    public List getGoldenSets() {
        return goldenSets;
    }

    public void setGoldenSets(List goldenSets) {
        this.goldenSets = goldenSets;
    }

    /**
     * True when this row belongs to a golden set whose name starts with the given
     * generation prefix (e.g. "ios-27-"), or declares no golden sets at all.
     */
    public boolean appliesToGeneration(String generationPrefix) {
        if (goldenSets == null || goldenSets.isEmpty() || generationPrefix == null) {
            return true;
        }
        for (int i = 0; i < goldenSets.size(); i++) {
            String g = (String) goldenSets.get(i);
            if (g != null && g.startsWith(generationPrefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True when this component should run on the given platform. A component with
     * no explicit platforms list runs everywhere; otherwise the platform name
     * must match one of the listed entries (matched by prefix so "and" covers
     * "android"). Also returns false when the platform has no native widget key --
     * except for animation-frame tests, which have no native reference at all
     * (frames are validated against committed CN1 goldens + the motion model).
     */
    public boolean appliesToPlatform(String platformName) {
        if (platformName == null) {
            return false;
        }
        if (getNativeKind(platformName) == null && (frames == null || frames.isEmpty())) {
            return false;
        }
        if (platforms == null || platforms.isEmpty()) {
            return true;
        }
        for (int i = 0; i < platforms.size(); i++) {
            String p = (String) platforms.get(i);
            if (p == null) {
                continue;
            }
            if (platformName.startsWith(p) || p.startsWith(platformName)) {
                return true;
            }
        }
        return false;
    }
}
