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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

/// Draws the bundled foldable skins: simple vector bezels, no photographic
/// art, generated at build time like the watch skins (`*.skin` is gitignored).
///
/// `iPhoneDuo.skin` is a book fold after Apple's iPhone Duo: a 7.6 inch inner
/// display folding down the middle and a 5.4 inch cover display. Apple has
/// published no resolutions or densities, so the pixel sizes below are
/// ESTIMATES chosen to give those diagonals at one density. Replace them when
/// the real figures are known; nothing else depends on them.
///
/// Usage: `GenerateFoldableSkins <iPhoneX.skin> <outDir>` (the theme `.res` is
/// lifted out of the iPhoneX skin, as the watch generator does).
public class GenerateFoldableSkins {

    // Estimated geometry, built from the two published diagonals and one
    // rule: closed, the device is exactly the folded half of the open one.
    // The inner display, 2482x2140 at 430 ppi, is 7.6 inches; the cover,
    // 1171x2005, is 5.4 inches and fills the half's width inside the same
    // bezel, with a wider band above and below it. Keep the catalog's
    // apple_iphone_duo record equal to these.
    static final int PPI = 430;
    static final int INNER_W = 2482;
    static final int INNER_H = 2140;
    static final int BEZEL = 70;
    static final int COVER_W = (INNER_W + BEZEL * 2) / 2 - BEZEL * 2;
    static final int COVER_H = 2005;
    // Concentric corners: the screen's radius is the body's minus the bezel.
    static final int BODY_RADIUS = 190;
    static final int SCREEN_RADIUS = BODY_RADIUS - BEZEL;
    static final int CAMERA = 44;
    static final int CAMERA_TOP = 30;
    // The safe band below a camera, in screen pixels.
    static final int SAFE_TOP = CAMERA_TOP + CAMERA + 40;

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalStateException("usage: GenerateFoldableSkins <iPhoneX.skin> <outDir>");
        }
        byte[] theme = extractEntry(new File(args[0]), ".res");
        if (theme == null) {
            throw new IllegalStateException("No .res theme found in " + args[0]);
        }
        File outDir = new File(args[1]);
        outDir.mkdirs();
        File out = new File(outDir, "iPhoneDuo.skin");
        writeDuo(theme, out);
        System.out.println("Wrote " + out);
    }

    static void writeDuo(byte[] theme, File out) throws IOException {
        int w = INNER_W + BEZEL * 2;
        int h = INNER_H + BEZEL * 2;
        Rectangle innerScreen = new Rectangle(BEZEL, BEZEL, INNER_W, INNER_H);
        // A continuous fold: the hinge is the centre line, zero wide.
        Rectangle hinge = new Rectangle(w / 2, 0, 0, h);

        BufferedImage inner = body(w, h, BODY_RADIUS);
        Graphics2D g = gfx(inner);
        screenRecess(g, innerScreen, SCREEN_RADIUS);
        // Hinge knuckles on the top and bottom bezel say where it folds.
        g.setColor(new Color(0x5a5a60));
        g.fillRoundRect(w / 2 - 8, 14, 16, BEZEL - 28, 8, 8);
        g.fillRoundRect(w / 2 - 8, h - BEZEL + 14, 16, BEZEL - 28, 8, 8);
        // The inner camera is a punch hole drawn over the UI, at the top of
        // the right half.
        int camX = INNER_W * 3 / 4 - CAMERA / 2;
        g.setColor(new Color(0x0a0a0c));
        g.fillOval(BEZEL + camX, BEZEL + CAMERA_TOP, CAMERA, CAMERA);
        g.dispose();

        int cw = w / 2;
        Rectangle coverScreen = new Rectangle(BEZEL, (h - COVER_H) / 2, COVER_W, COVER_H);
        BufferedImage cover = body(cw, h, BODY_RADIUS);
        g = gfx(cover);
        screenRecess(g, coverScreen, SCREEN_RADIUS);
        int coverCamX = COVER_W / 2 - CAMERA / 2;
        g.setColor(new Color(0x0a0a0c));
        g.fillOval(coverScreen.x + coverCamX, coverScreen.y + CAMERA_TOP, CAMERA, CAMERA);
        // Side button on the right edge, where the device is held closed.
        g.setColor(new Color(0x5a5a60));
        g.fillRoundRect(cw - 8, h / 3, 10, 200, 6, 6);
        g.dispose();

        StringBuilder p = new StringBuilder();
        p.append("# iPhone Duo - Codename One simulator skin (placeholder art, estimated resolutions)\n");
        p.append("touch=true\n");
        p.append("platformName=ios\n");
        p.append("overrideNames=phone,ios,iphone\n");
        p.append("ppi=").append(PPI).append('\n');
        p.append("pixelRatio=").append(PPI / 25.4).append('\n');
        p.append("systemFontFamily=Helvetica Neue\n");
        p.append("proportionalFontFamily=Helvetica Neue\n");
        p.append("monospaceFontFamily=Courier\n");
        p.append("keyboardType=3\n");
        p.append("softbuttonCount=0\n");
        // The frame is painted over the UI, so its rounded screen corners
        // mask the app the way the glass does.
        p.append("roundScreen=true\n");
        p.append("displayX=").append(innerScreen.x).append('\n');
        p.append("displayY=").append(innerScreen.y).append('\n');
        p.append("displayWidth=").append(INNER_W).append('\n');
        p.append("displayHeight=").append(INNER_H).append('\n');
        p.append("safePortraitX=0\nsafePortraitY=").append(SAFE_TOP).append('\n');
        p.append("safePortraitWidth=").append(INNER_W).append('\n');
        p.append("safePortraitHeight=").append(INNER_H - SAFE_TOP).append('\n');
        // Turned clockwise, the camera sits on the right edge.
        p.append("safeLandscapeX=0\nsafeLandscapeY=0\n");
        p.append("safeLandscapeWidth=").append(INNER_H - SAFE_TOP).append('\n');
        p.append("safeLandscapeHeight=").append(INNER_W).append('\n');
        p.append("cutouts=").append(camX).append(',').append(CAMERA_TOP).append(',')
                .append(CAMERA).append(',').append(CAMERA).append('\n');
        p.append("foldable=true\n");
        p.append("foldOrientation=vertical\n");
        p.append("foldType=continuous\n");
        p.append("crease=true\n");
        p.append("foldPostures=flat,half,closed\n");
        p.append("hingePortrait=").append(rect(hinge)).append('\n');
        p.append("hingeLandscape=").append(rect(rotateRect(hinge, h))).append('\n');
        p.append("coverPortraitScreenX=").append(coverScreen.x).append('\n');
        p.append("coverPortraitScreenY=").append(coverScreen.y).append('\n');
        p.append("coverPortraitScreenWidth=").append(coverScreen.width).append('\n');
        p.append("coverPortraitScreenHeight=").append(coverScreen.height).append('\n');
        Rectangle coverL = rotateRect(coverScreen, h);
        p.append("coverLandscapeScreenX=").append(coverL.x).append('\n');
        p.append("coverLandscapeScreenY=").append(coverL.y).append('\n');
        p.append("coverLandscapeScreenWidth=").append(coverL.width).append('\n');
        p.append("coverLandscapeScreenHeight=").append(coverL.height).append('\n');
        p.append("coverPpi=").append(PPI).append('\n');
        p.append("coverCutouts=").append(coverCamX).append(',').append(CAMERA_TOP).append(',')
                .append(CAMERA).append(',').append(CAMERA).append('\n');

        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out));
        try {
            put(zos, "skin.png", png(inner));
            put(zos, "skin_l.png", png(rotate(inner)));
            put(zos, "skin_map.png", png(map(w, h, innerScreen)));
            put(zos, "skin_map_l.png", png(rotate(map(w, h, innerScreen))));
            put(zos, "cover.png", png(cover));
            put(zos, "cover_l.png", png(rotate(cover)));
            put(zos, "skin.properties", p.toString().getBytes("UTF-8"));
            put(zos, "iOS7Theme.res", theme);
        } finally {
            zos.close();
        }
    }

    static String rect(Rectangle r) {
        return r.x + "," + r.y + "," + r.width + "," + r.height;
    }

    /// A rectangle in an image of height `h`, after the clockwise turn the
    /// simulator gives every landscape skin: (x, y, w, h) -> (H - y - h, x, h, w).
    static Rectangle rotateRect(Rectangle r, int h) {
        return new Rectangle(h - r.y - r.height, r.x, r.height, r.width);
    }

    static BufferedImage body(int w, int h, int arc) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = gfx(img);
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, w, h);
        g.setComposite(AlphaComposite.SrcOver);
        // Brushed titanium, lighter toward the top left.
        g.setPaint(new GradientPaint(0, 0, new Color(0x8e8e93), w, h, new Color(0x4a4a4f)));
        g.fill(new RoundRectangle2D.Float(0, 0, w, h, arc * 2, arc * 2));
        g.setColor(new Color(0x1c1c1e));
        g.fill(new RoundRectangle2D.Float(6, 6, w - 12, h - 12, arc * 2 - 8, arc * 2 - 8));
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(0x3a3a3c));
        g.draw(new RoundRectangle2D.Float(10, 10, w - 20, h - 20, arc * 2 - 14, arc * 2 - 14));
        g.dispose();
        return img;
    }

    /// Clears the screen area to transparent, so the simulator's screen shows
    /// through, keeping the rounded display corners as bezel.
    static void screenRecess(Graphics2D g, Rectangle s, int radius) {
        g.setComposite(AlphaComposite.Clear);
        g.fill(new RoundRectangle2D.Float(s.x, s.y, s.width, s.height, radius * 2, radius * 2));
        g.setComposite(AlphaComposite.SrcOver);
    }

    static BufferedImage map(int w, int h, Rectangle screen) {
        BufferedImage m = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = m.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.BLACK);
        g.fillRect(screen.x, screen.y, screen.width, screen.height);
        g.dispose();
        return m;
    }

    static BufferedImage rotate(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getHeight(), src.getWidth(),
                src.getType() == BufferedImage.TYPE_INT_RGB ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.translate(src.getHeight(), 0);
        g.rotate(Math.PI / 2);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    static Graphics2D gfx(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g;
    }

    static byte[] png(BufferedImage img) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        ImageIO.write(img, "png", b);
        return b.toByteArray();
    }

    static void put(ZipOutputStream zos, String name, byte[] data) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(data);
        zos.closeEntry();
    }

    static byte[] extractEntry(File zip, String suffix) throws IOException {
        ZipInputStream z = new ZipInputStream(new FileInputStream(zip));
        try {
            ZipEntry e;
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
