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

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.zip.*;
import javax.imageio.ImageIO;

/**
 * Generates the Apple Watch and Wear OS simulator skins the Codename One JavaSE
 * simulator ships, so a watch layout can be developed on the desktop instead of
 * only on a device.
 *
 * These are functional development skins -- correct display geometry, the round
 * flag, honest safe-area insets and {@code watch=true} so {@code CN.isWatch()}
 * is true and the "watch" override layer applies -- with simple programmatically
 * drawn bezel artwork. Replace skin.png with final design art when available.
 *
 * Each generated *.skin is a ZIP containing: skin.png, skin_l.png, skin_map.png,
 * skin_map_l.png, skin.properties and a theme .res copied from the bundled
 * iPhoneX.skin.
 *
 * Regenerate the shipped skins with:
 *   javac -d /tmp/wskin tools/watch-skins/GenerateWatchSkins.java
 *   java  -cp /tmp/wskin GenerateWatchSkins path/to/iPhoneX.skin outDir Themes/AndroidMaterialTheme.res
 */
public class GenerateWatchSkins {
    /// Corner radius of the drawn display on a non-circular face, in pixels.
    ///
    /// Named because two things depend on it and they must not drift: the artwork that draws the
    /// rounded display, and the safe-area inset that keeps content inside it.
    static final int DISPLAY_CORNER_RADIUS = 28;

    static class Model {
        final String file, label;
        final int dw, dh;          // display size in points
        final boolean circular;    // a round Wear OS face rather than a rounded rectangle
        final String platformName; // drives the skin's platform overrides
        final String overrides;
        /// The theme packaged INSIDE the skin, named as JavaSEPort expects to find it.
        ///
        /// It is not decoration: the simulator's "Embedded" native-theme option, and a project
        /// with nativeTheme=custom, both fall back to this resource. Giving every skin the theme
        /// lifted out of iPhoneX.skin meant a Wear preview rendered as iOS at exactly the moment
        /// the developer asked for the platform's own look.
        final String themeEntry;
        Model(String file, String label, int dw, int dh, boolean circular,
              String platformName, String overrides, String themeEntry) {
            this.file = file; this.label = label; this.dw = dw; this.dh = dh;
            this.circular = circular; this.platformName = platformName; this.overrides = overrides;
            this.themeEntry = themeEntry;
        }
    }

    public static void main(String[] args) throws Exception {
        File srcSkin = new File(args[0]);
        File outDir = new File(args[1]);
        outDir.mkdirs();

        byte[] iosTheme = extractEntry(srcSkin, ".res");
        if (iosTheme == null) {
            throw new IllegalStateException("No .res theme found in " + srcSkin);
        }
        // The Wear skins carry Android's own theme, passed in explicitly rather than looked for in
        // the output directory: the build copies the shipped themes there AFTER this runs, so
        // reading from there would find nothing and the ordering would be a trap for whoever
        // rearranged the pom next.
        if (args.length < 3) {
            throw new IllegalStateException("usage: GenerateWatchSkins <iPhoneX.skin> <outDir> <"
                    + ANDROID_THEME + ">");
        }
        File androidThemeFile = new File(args[2]);
        if (!androidThemeFile.isFile()) {
            throw new IllegalStateException("No Android theme at " + androidThemeFile
                    + "; the Wear skins must not fall back to an iOS theme");
        }
        byte[] androidTheme = readFully(androidThemeFile);

        Model[] models = new Model[] {
            // Apple Watch logical point resolutions.
            new Model("AppleWatch41mm.skin", "Apple Watch 41mm", 352, 430, false,
                    "ios", "watch,ios,applewatch", IOS_THEME),
            new Model("AppleWatch45mm.skin", "Apple Watch 45mm", 396, 484, false,
                    "ios", "watch,ios,applewatch", IOS_THEME),
            // Wear OS. The round face is the one worth designing against: it is what most Wear
            // hardware ships and it is where a layout that assumes a rectangle falls apart.
            new Model("WearRound.skin", "Wear OS Round", 454, 454, true,
                    "and", "watch,android,android-watch", ANDROID_THEME),
            new Model("WearSquare.skin", "Wear OS Square", 400, 400, false,
                    "and", "watch,android,android-watch", ANDROID_THEME),
        };

        for (Model m : models) {
            generate(m, ANDROID_THEME.equals(m.themeEntry) ? androidTheme : iosTheme, outDir);
            System.out.println("Wrote " + new File(outDir, m.file));
        }
    }

    static void generate(Model m, byte[] themeRes, File outDir) throws Exception {
        // Bezel margins around the display; the crown sits on the right edge.
        int marginX = 70, marginTop = 90, marginBottom = 90;
        int imgW = m.dw + marginX * 2;
        int imgH = m.dh + marginTop + marginBottom;
        int displayX = marginX;
        int displayY = marginTop;

        BufferedImage skin = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = skin.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Transparent backdrop.
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, imgW, imgH);
        g.setComposite(AlphaComposite.SrcOver);

        // Aluminium body: rounded rectangle the size of the whole image.
        int bodyArc = Math.min(imgW, imgH) / 3;
        g.setColor(new Color(0x1c1c1e));
        g.fill(new RoundRectangle2D.Float(0, 0, imgW, imgH, bodyArc, bodyArc));

        // Subtle bezel highlight.
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(0x3a3a3c));
        g.draw(new RoundRectangle2D.Float(6, 6, imgW - 12, imgH - 12, bodyArc - 6, bodyArc - 6));

        // Rotary input nub on the right edge: the Digital Crown on Apple, the rotating side button
        // on Wear. Both scroll the focused container, so the artwork says the same thing.
        g.setColor(new Color(0x5a5a5e));
        g.fillRoundRect(imgW - 10, imgH / 2 - 34, 16, 68, 10, 10);
        // Side button below the crown.
        g.fillRoundRect(imgW - 8, imgH / 2 + 48, 12, 54, 8, 8);

        // The display recess (the rest of the screen is painted by the simulator).
        g.setColor(Color.BLACK);
        if (m.circular) {
            g.fillOval(displayX, displayY, m.dw, m.dh);
        } else {
            g.fill(new RoundRectangle2D.Float(displayX, displayY, m.dw, m.dh,
                    DISPLAY_CORNER_RADIUS * 2, DISPLAY_CORNER_RADIUS * 2));
        }
        g.dispose();

        // Watch never rotates; landscape image reuses the portrait artwork.
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(skin, "png", png);
        byte[] skinPng = png.toByteArray();

        // The coordinate map, and it is not optional artwork.
        //
        // JavaSEPort takes the screen rectangle and the bezel hotspot table out of a companion
        // image the same size as the skin: white is bezel, black is the display, any other colour
        // is a key mapped through a c<rrggbb> property. A skin without one loaded fine right up to
        // initializeCoordinates, which dereferences it -- so every NON-round watch skin (both Apple
        // Watch sizes and Wear Square) died with a NullPointerException the moment it was selected.
        // The round path never reads the map, which is why Wear Round alone appeared to work.
        //
        // Emitted for the round faces too. It costs a few hundred bytes, it keeps every archive the
        // same shape, and it means the answer no longer depends on which branch of the loader a
        // future edit happens to take.
        BufferedImage mapImage = new BufferedImage(imgW, imgH, BufferedImage.TYPE_INT_RGB);
        Graphics2D mg = mapImage.createGraphics();
        mg.setColor(Color.WHITE);
        mg.fillRect(0, 0, imgW, imgH);
        // A rectangle even for a circular face: the loader reads the black region's BOUNDING BOX,
        // and an oval's bounding box is this rectangle anyway. Filling the rectangle keeps the two
        // faces reporting identical geometry instead of leaving it to antialiasing at the rim.
        mg.setColor(Color.BLACK);
        mg.fillRect(displayX, displayY, m.dw, m.dh);
        mg.dispose();
        // No hotspots: these skins have no mapped bezel keys. The crown and the side button are
        // artwork -- rotary input arrives as a wheel event, not as a skin key.
        ByteArrayOutputStream mapPng = new ByteArrayOutputStream();
        ImageIO.write(mapImage, "png", mapPng);
        byte[] skinMapPng = mapPng.toByteArray();

        // Safe-area inset: the curve eats the corners, so content has to stay clear of them. A round
        // face loses far more than a rounded rectangle does -- inscribing a rectangle in a circle
        // costs about 15% a side -- and getting this wrong in the simulator is precisely the bug
        // that only shows up on real hardware.
        // Inscribing a rectangle in a circle costs on BOTH axes, so a circular face insets its width
        // as well as its height. Reporting the full display width on a round watch is what lets a
        // layout that correctly honours the safe area still put content in the clipped left and right
        // corners.
        // A rounded rectangle loses its CORNERS, so it has to inset horizontally too.
        //
        // The old answer was zero on the X axis, on the reasoning that a rounded rectangle keeps
        // its full width. It keeps it along the middle and nowhere near the top and bottom, and the
        // safe area is one rectangle: with insetX = 0 the advertised rectangle's own corners fell
        // outside the drawn display, so a component that correctly honours getDisplaySafeArea()
        // could still be clipped by the bezel -- which is the one bug this metadata exists to
        // prevent, and the one that only shows up on hardware.
        //
        // The corner arc has its centre at (r, r), so a point inset by d on both axes is inside it
        // when (r - d)^2 * 2 <= r^2, i.e. d >= r * (1 - 1/sqrt(2)). Rounded up, and applied as a
        // FLOOR rather than a replacement: the vertical inset is already larger than this on every
        // shipped size, and shrinking it to the geometric minimum would hand back margin that the
        // bezel curve does not actually leave usable.
        int cornerInset = m.circular ? 0
                : (int) Math.ceil(DISPLAY_CORNER_RADIUS * (1.0 - 1.0 / Math.sqrt(2.0)));
        int inset = Math.max(cornerInset, Math.round(m.dh * (m.circular ? 0.15f : 0.06f)));
        int insetX = m.circular ? Math.round(m.dw * 0.15f) : cornerInset;
        StringBuilder p = new StringBuilder();
        p.append("# ").append(m.label).append(" - Codename One simulator skin (placeholder art)\n");
        p.append("touch=true\n");
        p.append("ppi=326\n");
        p.append("smallFontSize=").append(Math.round(m.dw * 0.045f)).append('\n');
        p.append("mediumFontSize=").append(Math.round(m.dw * 0.06f)).append('\n');
        p.append("largeFontSize=").append(Math.round(m.dw * 0.08f)).append('\n');
        p.append("systemFontFamily=Helvetica Neue\n");
        p.append("proportionalFontFamily=Helvetica Neue\n");
        p.append("monospaceFontFamily=Courier\n");
        p.append("keyboardType=3\n");
        p.append("softbuttonCount=0\n");
        p.append("platformName=").append(m.platformName).append('\n');
        p.append("overrideNames=").append(m.overrides).append('\n');
        p.append("watch=true\n");
        // Only a genuinely circular face is a round screen. Apple Watch is a heavily rounded
        // rectangle, and claiming otherwise would inscribe its safe area in a circle and waste a
        // third of the display.
        p.append("roundScreen=").append(m.circular).append('\n');
        p.append("displayX=").append(displayX).append('\n');
        p.append("displayY=").append(displayY).append('\n');
        p.append("displayWidth=").append(m.dw).append('\n');
        p.append("displayHeight=").append(m.dh).append('\n');
        p.append("safePortraitX=").append(insetX).append('\n');
        p.append("safePortraitY=").append(inset).append('\n');
        p.append("safePortraitWidth=").append(m.dw - insetX * 2).append('\n');
        p.append("safePortraitHeight=").append(m.dh - inset * 2).append('\n');
        // IDENTICAL to the portrait values, not the transpose of them.
        //
        // There is no landscape watch. skin_l.png and skin_map_l.png are the portrait artwork and
        // the portrait coordinate map, because that is the only face the device has -- so swapping
        // the display dimensions here described a screen that is not the one being drawn or
        // clicked. A rotated rectangular Apple Watch or Wear Square skin then measured layouts
        // against a safe area wider than the display it was inset from, and previewed them out of
        // bounds. The simulator disables rotation for a watch skin as well; these values are what
        // keeps a persisted or forced landscape state honest rather than merely unreachable.
        p.append("safeLandscapeX=").append(insetX).append('\n');
        p.append("safeLandscapeY=").append(inset).append('\n');
        p.append("safeLandscapeWidth=").append(m.dw - insetX * 2).append('\n');
        p.append("safeLandscapeHeight=").append(m.dh - inset * 2).append('\n');

        File out = new File(outDir, m.file);
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out));
        putEntry(zos, "skin.png", skinPng);
        putEntry(zos, "skin_l.png", skinPng);
        putEntry(zos, "skin_map.png", skinMapPng);
        putEntry(zos, "skin_map_l.png", skinMapPng);
        putEntry(zos, "skin.properties", p.toString().getBytes("UTF-8"));
        putEntry(zos, m.themeEntry, themeRes);
        zos.close();
    }

    static final String IOS_THEME = "iOS7Theme.res";
    static final String ANDROID_THEME = "AndroidMaterialTheme.res";

    static byte[] readFully(File f) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        java.io.FileInputStream in = new java.io.FileInputStream(f);
        try {
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) {
                bos.write(buf, 0, r);
            }
        } finally {
            in.close();
        }
        return bos.toByteArray();
    }

    static void putEntry(ZipOutputStream zos, String name, byte[] data) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(data);
        zos.closeEntry();
    }

    static byte[] extractEntry(File zip, String suffix) throws IOException {
        ZipInputStream z = new ZipInputStream(new FileInputStream(zip));
        ZipEntry e;
        try {
            while ((e = z.getNextEntry()) != null) {
                if (e.getName().endsWith(suffix)) {
                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = z.read(buf)) > 0) {
                        b.write(buf, 0, n);
                    }
                    return b.toByteArray();
                }
            }
        } finally {
            z.close();
        }
        return null;
    }
}
