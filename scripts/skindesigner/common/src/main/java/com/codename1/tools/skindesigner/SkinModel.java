package com.codename1.tools.skindesigner;

import com.codename1.io.Preferences;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory model of the skin currently being designed. Mirrors the React
 * design's {@code skin} state. Persists itself to {@link Preferences} so the
 * wizard survives a reload, mirroring the localStorage behavior of the
 * design.
 */
public final class SkinModel {
    public static final String SOURCE_SHAPE = "shape";
    public static final String SOURCE_IMAGE = "image";
    public static final String SOURCE_BLANK = "blank";

    public static final String CUTOUT_NOTCH = "notch";
    public static final String CUTOUT_ISLAND = "island";
    public static final String CUTOUT_HOLE = "hole";

    public static final class Cutout {
        public String type;
        public int w, h, x, y;
        public String name;

        public Cutout(String type, int w, int h, int x, int y, String name) {
            this.type = type;
            this.w = w;
            this.h = h;
            this.x = x;
            this.y = y;
            this.name = name;
        }
    }

    public String presetId = "rr";
    public String name = "My skin";
    public int cornerR = 40;
    public int bezel = 40;
    public boolean homeIndicator = true;
    public List<Cutout> cutouts = new ArrayList<>();
    public int safeTop = 40;
    public int safeBottom = 0;

    private static final String P = "wiz.skin.";

    public void resetForDevice(DeviceDatabase.Device d) {
        presetId = "rr";
        name = d == null ? "My skin" : d.name + " skin";
        cornerR = 40;
        bezel = 40;
        homeIndicator = d == null || d.hasHomeIndicator;
        cutouts = new ArrayList<>();
        safeTop = d == null ? 40 : d.safeTop;
        safeBottom = d == null ? 0 : d.safeBottom;
        if (d != null) {
            if (d.hasIsland) {
                cutouts.add(new Cutout(CUTOUT_ISLAND, 120, 35, 0, 14, "Dynamic Island"));
                presetId = "island";
            } else if (d.hasNotch) {
                cutouts.add(new Cutout(CUTOUT_NOTCH, 180, 30, 0, 0, "Notch"));
                presetId = "notch";
            } else if (d.hasHole) {
                cutouts.add(new Cutout(CUTOUT_HOLE, 28, 28, 0, 20, "Camera"));
                presetId = "hole";
            } else if (!d.hasHomeIndicator) {
                presetId = "classic";
                cornerR = 20;
                bezel = 64;
            }
        }
    }

    public void save() {
        Preferences.set(P + "presetId", presetId);
        Preferences.set(P + "name", name);
        Preferences.set(P + "cornerR", cornerR);
        Preferences.set(P + "bezel", bezel);
        Preferences.set(P + "homeIndicator", homeIndicator);
        Preferences.set(P + "safeTop", safeTop);
        Preferences.set(P + "safeBottom", safeBottom);
        Preferences.set(P + "cutoutCount", cutouts.size());
        for (int i = 0; i < cutouts.size(); i++) {
            Cutout c = cutouts.get(i);
            String k = P + "cut." + i + ".";
            Preferences.set(k + "type", c.type);
            Preferences.set(k + "w", c.w);
            Preferences.set(k + "h", c.h);
            Preferences.set(k + "x", c.x);
            Preferences.set(k + "y", c.y);
            Preferences.set(k + "name", c.name);
        }
    }

    public void load() {
        presetId = Preferences.get(P + "presetId", presetId);
        name = Preferences.get(P + "name", name);
        cornerR = Preferences.get(P + "cornerR", cornerR);
        bezel = Preferences.get(P + "bezel", bezel);
        homeIndicator = Preferences.get(P + "homeIndicator", homeIndicator);
        safeTop = Preferences.get(P + "safeTop", safeTop);
        safeBottom = Preferences.get(P + "safeBottom", safeBottom);
        int n = Preferences.get(P + "cutoutCount", 0);
        cutouts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String k = P + "cut." + i + ".";
            Cutout c = new Cutout(
                    Preferences.get(k + "type", CUTOUT_HOLE),
                    Preferences.get(k + "w", 28),
                    Preferences.get(k + "h", 28),
                    Preferences.get(k + "x", 0),
                    Preferences.get(k + "y", 20),
                    Preferences.get(k + "name", "Cutout"));
            cutouts.add(c);
        }
    }

    public static void clearPersisted() {
        for (String key : new String[]{"presetId", "name", "cornerR", "bezel",
                "homeIndicator", "safeTop", "safeBottom", "cutoutCount"}) {
            Preferences.delete(P + key);
        }
        for (int i = 0; i < 32; i++) {
            String k = P + "cut." + i + ".";
            for (String f : new String[]{"type", "w", "h", "x", "y", "name"}) {
                Preferences.delete(k + f);
            }
        }
    }
}
