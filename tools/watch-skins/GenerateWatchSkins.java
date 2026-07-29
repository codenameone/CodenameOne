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
 * Each generated *.skin is a ZIP containing: skin.png, skin_l.png,
 * skin.properties and a theme .res copied from the bundled iPhoneX.skin.
 *
 * Regenerate the shipped skins with:
 *   javac -d /tmp/wskin tools/watch-skins/GenerateWatchSkins.java
 *   java  -cp /tmp/wskin GenerateWatchSkins Ports/JavaSE/src/iPhoneX.skin Ports/JavaSE/src
 */
public class GenerateWatchSkins {
    static class Model {
        final String file, label;
        final int dw, dh;          // display size in points
        final boolean circular;    // a round Wear OS face rather than a rounded rectangle
        final String platformName; // drives the skin's platform overrides
        final String overrides;
        Model(String file, String label, int dw, int dh, boolean circular,
              String platformName, String overrides) {
            this.file = file; this.label = label; this.dw = dw; this.dh = dh;
            this.circular = circular; this.platformName = platformName; this.overrides = overrides;
        }
    }

    public static void main(String[] args) throws Exception {
        File srcSkin = new File(args[0]);
        File outDir = new File(args[1]);
        outDir.mkdirs();

        byte[] themeRes = extractEntry(srcSkin, ".res");
        if (themeRes == null) {
            throw new IllegalStateException("No .res theme found in " + srcSkin);
        }

        Model[] models = new Model[] {
            // Apple Watch logical point resolutions.
            new Model("AppleWatch41mm.skin", "Apple Watch 41mm", 352, 430, false,
                    "ios", "watch,ios,applewatch"),
            new Model("AppleWatch45mm.skin", "Apple Watch 45mm", 396, 484, false,
                    "ios", "watch,ios,applewatch"),
            // Wear OS. The round face is the one worth designing against: it is what most Wear
            // hardware ships and it is where a layout that assumes a rectangle falls apart.
            new Model("WearRound.skin", "Wear OS Round", 454, 454, true,
                    "and", "watch,android,android-watch"),
            new Model("WearSquare.skin", "Wear OS Square", 400, 400, false,
                    "and", "watch,android,android-watch"),
        };

        for (Model m : models) {
            generate(m, themeRes, outDir);
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
            g.fill(new RoundRectangle2D.Float(displayX, displayY, m.dw, m.dh, 56, 56));
        }
        g.dispose();

        // Watch never rotates; landscape image reuses the portrait artwork.
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(skin, "png", png);
        byte[] skinPng = png.toByteArray();

        // Safe-area inset: the curve eats the corners, so content has to stay clear of them. A round
        // face loses far more than a rounded rectangle does -- inscribing a rectangle in a circle
        // costs about 15% a side -- and getting this wrong in the simulator is precisely the bug
        // that only shows up on real hardware.
        int inset = Math.round(m.dh * (m.circular ? 0.15f : 0.06f));
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
        p.append("safePortraitX=0\n");
        p.append("safePortraitY=").append(inset).append('\n');
        p.append("safePortraitWidth=").append(m.dw).append('\n');
        p.append("safePortraitHeight=").append(m.dh - inset * 2).append('\n');
        p.append("safeLandscapeX=").append(inset).append('\n');
        p.append("safeLandscapeY=0\n");
        p.append("safeLandscapeWidth=").append(m.dh - inset * 2).append('\n');
        p.append("safeLandscapeHeight=").append(m.dw).append('\n');

        File out = new File(outDir, m.file);
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(out));
        putEntry(zos, "skin.png", skinPng);
        putEntry(zos, "skin_l.png", skinPng);
        putEntry(zos, "skin.properties", p.toString().getBytes("UTF-8"));
        putEntry(zos, "iOS7Theme.res", themeRes);
        zos.close();
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
