/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import javax.imageio.ImageIO;

/// Makes an app icon from the usual ingredients -- a background colour or
/// gradient, one glyph from the Material icon font every Codename One app ships,
/// or the app's initials -- headless, with nothing but a JDK.
///
/// ```
/// java tools/MakeIcon.java --glyph check_circle --bg 1f5eff --bg2 6a3df0 --out common/icon.png
/// java tools/MakeIcon.java --text HT --bg 0f766e --out common/icon.png
/// java tools/MakeIcon.java --list-glyphs heart      # glyph names containing "heart"
/// java tools/MakeIcon.java --palettes               # brand palettes that read well as icons
/// ```
///
/// Options: `--bg RRGGBB` (default 1f5eff), `--bg2 RRGGBB` for a diagonal
/// gradient, `--fg RRGGBB` (default white, or near-black on a light background),
/// `--glyph NAME` or `--text AB`, `--scale 0.56` (glyph size as a fraction of
/// the icon), `--size 1024`, `--shape square|rounded|circle` (default square:
/// the stores mask it themselves), `--out FILE` (default icon.png),
/// `--adaptive DIR` to also write Android adaptive-icon layers
/// (`ic_foreground.png` with transparent padding, `ic_background.png`).
///
/// The default output is what both stores want from a source icon: 1024 x 1024,
/// opaque (App Store Connect rejects an alpha channel), full bleed, the glyph
/// inside the central safe area so no mask clips it. Codename One's build makes
/// every size the platforms need from `common/icon.png` (`icon.png` in a Gradle
/// project).
///
/// The glyph names are FontImage's `MATERIAL_*` constants, lower case without
/// the prefix (`MATERIAL_FAVORITE` is `favorite`). They and the font come from
/// the newest `codenameone-core` jar in ~/.m2 or the Gradle cache, so build the
/// project once first. `--text` needs no jar.
///
/// Prints `WROTE <file>` and exits 0; exit 2 on a usage or discovery error.
public class MakeIcon {

    private static final String[][] PALETTES = {
        {"blue", "1f5eff", "6a3df0"}, {"teal", "0f766e", "14b8a6"}, {"green", "15803d", "4ade80"},
        {"orange", "ff7a1f", "ffb020"}, {"red", "d93a42", "ff6b6b"}, {"purple", "7c3aed", "c026d3"},
        {"pink", "db2777", "fb7185"}, {"navy", "1f3a8a", "3b82f6"}, {"graphite", "1f2937", "4b5563"},
        {"amber", "c8862f", "f59e0b"},
    };

    public static void main(String[] args) throws Exception {
        String bg = "1f5eff", bg2 = null, fg = null, glyph = null, text = null, shape = "square";
        String out = "icon.png", adaptive = null, listFilter = null;
        double scale = 0.56;
        int size = 1024;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.equals("--palettes")) {
                for (String[] p : PALETTES) System.out.println(p[0] + "  --bg " + p[1] + " --bg2 " + p[2]);
                return;
            }
            if (a.equals("--list-glyphs")) {
                listFilter = i + 1 < args.length && !args[i + 1].startsWith("--") ? args[++i] : "";
                continue;
            }
            if (i + 1 >= args.length) usage("missing value for " + a);
            String v = args[++i];
            switch (a) {
                case "--bg" -> bg = hex(v);
                case "--bg2" -> bg2 = hex(v);
                case "--fg" -> fg = hex(v);
                case "--glyph" -> glyph = v;
                case "--text" -> text = v;
                case "--shape" -> shape = v;
                case "--out" -> out = v;
                case "--adaptive" -> adaptive = v;
                case "--scale" -> scale = Double.parseDouble(v);
                case "--size" -> size = Integer.parseInt(v);
                default -> usage("unknown option " + a);
            }
        }
        System.setProperty("java.awt.headless", "true");

        Path core = null;
        if (listFilter != null || glyph != null) {
            core = findLatestJar("codenameone-core");
            if (core == null) {
                System.err.println("No codenameone-core jar in ~/.codenameone/agent, ~/.m2 or the Gradle cache. Run create-app or build the project once "
                        + "(./mvnw -pl common compile, or ./gradlew classes), or use --text.");
                System.exit(2);
            }
        }
        if (listFilter != null) {
            String f = listFilter.toLowerCase(Locale.ROOT);
            for (String n : glyphNames(core)) if (n.contains(f)) System.out.println(n);
            return;
        }
        if (glyph == null && text == null) usage("give --glyph NAME or --text AB");

        Color back = new Color(Integer.parseInt(bg, 16));
        Color back2 = bg2 == null ? null : new Color(Integer.parseInt(bg2, 16));
        Color front = fg != null ? new Color(Integer.parseInt(fg, 16)) : (luminance(back) > 0.6 ? new Color(0x111827) : Color.WHITE);

        Font font;
        String chars;
        if (glyph != null) {
            chars = String.valueOf(glyphChar(core, glyph));
            try (JarFile jar = new JarFile(core.toFile())) {
                ZipEntry e = jar.getEntry("material-design-font.ttf");
                if (e == null) {
                    System.err.println("material-design-font.ttf is not in " + core);
                    System.exit(2);
                }
                try (InputStream in = jar.getInputStream(e)) {
                    font = Font.createFont(Font.TRUETYPE_FONT, in);
                }
            }
        } else {
            chars = text;
            font = new Font(Font.SANS_SERIF, Font.BOLD, 10);
        }

        boolean opaque = shape.equals("square");
        BufferedImage icon = new BufferedImage(size, size, opaque ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = start(icon);
        Shape outline = switch (shape) {
            case "square" -> new Rectangle2D.Double(0, 0, size, size);
            case "rounded" -> new RoundRectangle2D.Double(0, 0, size, size, size * 0.45, size * 0.45);
            case "circle" -> new java.awt.geom.Ellipse2D.Double(0, 0, size, size);
            default -> { usage("--shape is square, rounded or circle"); yield null; }
        };
        paintBackground(g, outline, size, back, back2);
        paintGlyph(g, font, chars, front, size, scale, glyph == null);
        g.dispose();
        write(icon, out);

        if (adaptive != null) {
            // Android adaptive icons: 108dp layers of which the central 72dp is
            // always visible, so the glyph sits in the middle two thirds.
            Path dir = Paths.get(adaptive);
            Files.createDirectories(dir);
            int a = 432;
            BufferedImage fgLayer = new BufferedImage(a, a, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gf = start(fgLayer);
            paintGlyph(gf, font, chars, front, a, scale * 0.66, glyph == null);
            gf.dispose();
            write(fgLayer, dir.resolve("ic_foreground.png").toString());
            BufferedImage bgLayer = new BufferedImage(a, a, BufferedImage.TYPE_INT_RGB);
            Graphics2D gb = start(bgLayer);
            paintBackground(gb, new Rectangle2D.Double(0, 0, a, a), a, back, back2);
            gb.dispose();
            write(bgLayer, dir.resolve("ic_background.png").toString());
        }
    }

    private static Graphics2D start(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        return g;
    }

    private static void paintBackground(Graphics2D g, Shape outline, int size, Color back, Color back2) {
        if (back2 != null) {
            g.setPaint(new GradientPaint(0, 0, back, size, size, back2));
        } else {
            g.setColor(back);
        }
        g.fill(outline);
    }

    /// Centres the glyph's actual ink, not its font box, so an icon glyph and a
    /// pair of initials both sit optically in the middle.
    private static void paintGlyph(Graphics2D g, Font base, String chars, Color color, int size, double scale,
                                   boolean isText) {
        Font font = base.deriveFont((float) (size * scale * (isText && chars.length() > 1 ? 0.75 : 1.0)));
        GlyphVector gv = font.createGlyphVector(g.getFontRenderContext(), chars);
        Rectangle2D ink = gv.getVisualBounds();
        double target = size * scale;
        double fit = Math.min(target / ink.getWidth(), target / ink.getHeight());
        if (fit < 1) {
            font = font.deriveFont((float) (font.getSize2D() * fit));
            gv = font.createGlyphVector(g.getFontRenderContext(), chars);
            ink = gv.getVisualBounds();
        }
        float x = (float) ((size - ink.getWidth()) / 2 - ink.getX());
        float y = (float) ((size - ink.getHeight()) / 2 - ink.getY());
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(color);
        g.fill(gv.getOutline(x, y));
    }

    private static void write(BufferedImage img, String out) throws Exception {
        Path p = Paths.get(out).toAbsolutePath();
        if (p.getParent() != null) Files.createDirectories(p.getParent());
        ImageIO.write(img, "png", p.toFile());
        System.out.println("WROTE " + p);
    }

    private static double luminance(Color c) {
        return (0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue()) / 255.0;
    }

    private static String hex(String v) {
        String h = v.startsWith("#") ? v.substring(1) : v;
        if (!h.matches("[0-9a-fA-F]{6}")) usage("colours are RRGGBB, got " + v);
        return h;
    }

    private static List<String> glyphNames(Path core) throws Exception {
        List<String> names = new ArrayList<>();
        for (Field f : fontImage(core).getFields()) {
            if (Modifier.isStatic(f.getModifiers()) && f.getType() == char.class && f.getName().startsWith("MATERIAL_")) {
                names.add(f.getName().substring("MATERIAL_".length()).toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    private static char glyphChar(Path core, String name) throws Exception {
        String field = name.toUpperCase(Locale.ROOT);
        if (!field.startsWith("MATERIAL_")) field = "MATERIAL_" + field;
        try {
            return fontImage(core).getField(field).getChar(null);
        } catch (NoSuchFieldException e) {
            System.err.println("No glyph named " + name + "; try --list-glyphs " + name.toLowerCase(Locale.ROOT));
            System.exit(2);
            return 0;
        }
    }

    private static Class<?> fontImage(Path core) throws Exception {
        URLClassLoader loader = new URLClassLoader(new URL[] {core.toUri().toURL()}, MakeIcon.class.getClassLoader());
        return Class.forName("com.codename1.ui.FontImage", false, loader);
    }

    private static void usage(String error) {
        System.err.println("error: " + error);
        System.err.println("usage: java tools/MakeIcon.java (--glyph NAME | --text AB) [--bg RRGGBB] [--bg2 RRGGBB]");
        System.err.println("         [--fg RRGGBB] [--scale 0.56] [--size 1024] [--shape square|rounded|circle]");
        System.err.println("         [--out icon.png] [--adaptive DIR]");
        System.err.println("       java tools/MakeIcon.java --list-glyphs [filter] | --palettes");
        System.exit(2);
    }

    /// The newest jar of a com.codenameone artifact in ~/.m2 or the Gradle cache;
    /// the same lookup as the other tools here.
    private static Path findLatestJar(String artifactId) throws Exception {
        List<Path[]> found = new ArrayList<>();
        String home = System.getProperty("user.home");
        // CN1_CORE_JAR names one outright. Otherwise the jars create-app
        // downloaded (CN1_AGENT_HOME, default ~/.codenameone/agent, as
        // lib/<version>-<kit>/<artifact>.jar), so an icon can be made before the
        // project has ever been built.
        String named = System.getenv("CN1_CORE_JAR");
        if ("codenameone-core".equals(artifactId) && named != null && Files.isRegularFile(Paths.get(named))) {
            return Paths.get(named);
        }
        String agentHome = System.getenv("CN1_AGENT_HOME");
        Path agentLib = (agentHome != null && !agentHome.isEmpty() ? Paths.get(agentHome)
                : Paths.get(home, ".codenameone", "agent")).resolve("lib");
        if (Files.isDirectory(agentLib)) {
            try (DirectoryStream<Path> versions = Files.newDirectoryStream(agentLib)) {
                for (Path versionDir : versions) {
                    Path jar = versionDir.resolve(artifactId + ".jar");
                    if (Files.isRegularFile(jar)) {
                        found.add(new Path[] {versionDir, jar});
                    }
                }
            }
        }
        collect(Paths.get(home, ".m2", "repository", "com", "codenameone", artifactId), artifactId, false, found);
        String gradleHome = System.getenv("GRADLE_USER_HOME");
        Path gradleBase = gradleHome != null && !gradleHome.isEmpty() ? Paths.get(gradleHome) : Paths.get(home, ".gradle");
        collect(gradleBase.resolve(Paths.get("caches", "modules-2", "files-2.1", "com.codenameone", artifactId)),
                artifactId, true, found);
        if (found.isEmpty()) return null;
        found.sort((a, b) -> compareVersions(b[0].getFileName().toString(), a[0].getFileName().toString()));
        return found.get(0)[1];
    }

    private static void collect(Path baseDir, String artifactId, boolean hashed, List<Path[]> out) throws Exception {
        if (!Files.isDirectory(baseDir)) return;
        try (DirectoryStream<Path> versions = Files.newDirectoryStream(baseDir)) {
            for (Path versionDir : versions) {
                if (!Files.isDirectory(versionDir)) continue;
                List<Path> dirs = new ArrayList<>();
                if (hashed) {
                    try (DirectoryStream<Path> hashes = Files.newDirectoryStream(versionDir)) {
                        for (Path h : hashes) if (Files.isDirectory(h)) dirs.add(h);
                    }
                } else {
                    dirs.add(versionDir);
                }
                for (Path dir : dirs) {
                    try (DirectoryStream<Path> jars = Files.newDirectoryStream(dir, artifactId + "-*.jar")) {
                        for (Path jar : jars) {
                            String n = jar.getFileName().toString();
                            if (n.endsWith("-sources.jar") || n.endsWith("-javadoc.jar")) continue;
                            out.add(new Path[] {versionDir, jar});
                        }
                    }
                }
            }
        }
    }

    private static int compareVersions(String left, String right) {
        String[] l = left.split("-", 2)[0].split("\\.");
        String[] r = right.split("-", 2)[0].split("\\.");
        for (int i = 0; i < Math.max(l.length, r.length); i++) {
            int c = Integer.compare(part(l, i), part(r, i));
            if (c != 0) return c;
        }
        return left.compareTo(right);
    }

    private static int part(String[] parts, int i) {
        if (i >= parts.length) return 0;
        try {
            return Integer.parseInt(parts[i]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
