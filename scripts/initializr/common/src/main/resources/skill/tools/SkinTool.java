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
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

/// SkinTool -- browse the Codename One device database and build a simulator
/// skin (.skin zip) for any phone, with the same images and skin.properties
/// the Skin Designer app writes.
///
/// Java 17 single-file source program; needs nothing but a JDK:
///
///   java tools/SkinTool.java devices pixel 9 --limit 5
///   java tools/SkinTool.java devices --platform ios --foldable
///   java tools/SkinTool.java device apple_iphone_duo
///   java tools/SkinTool.java create out.skin --device google_pixel_9
///   java tools/SkinTool.java create out.skin --width 1080 --height 2400 --ppi 420 --cutout hole
///
/// The database is the CURRENT devices.json on master, cached in
/// ~/.codenameone/skin-devices.json and refreshed once a day (--offline reads
/// only the cache). The native theme .res the skin embeds is fetched the same
/// way and cached in ~/.codenameone/skin-themes/.
///
/// Geometry mirrors SkinDesigner.createSkinBytes: the frame is drawn in a
/// 320-unit-wide viewbox and scaled to the panel, so --bezel, --corner-radius
/// and cutout sizes are in viewbox units exactly as in the designer, while
/// --safe-top/--safe-bottom are points (multiplied by the device scale).
public final class SkinTool {
    private static final String RAW = "https://raw.githubusercontent.com/codenameone/CodenameOne/master/";
    private static final String DB_URL = RAW + "scripts/skindesigner/common/src/main/resources/devices.json";
    private static final String THEME_DIR_URL = RAW + "scripts/skindesigner/common/src/main/resources/";
    private static final int VB_W = 320;
    private static final File HOME = new File(System.getProperty("user.home"), ".codenameone");
    private static boolean offline;

    public static void main(String[] argv) throws Exception {
        System.setProperty("java.awt.headless", "true");
        List<String> args = new ArrayList<>();
        for (String a : argv) {
            if ("--offline".equals(a)) {
                offline = true;
            } else {
                args.add(a);
            }
        }
        if (args.isEmpty() || args.contains("--help") || args.contains("-h")) {
            usage();
            System.exit(args.isEmpty() ? 2 : 0);
        }
        String cmd = args.remove(0);
        try {
            switch (cmd) {
                case "devices": devices(args); break;
                case "device": device(args); break;
                case "create": create(args); break;
                default: usage(); System.exit(2);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("error: " + e.getMessage());
            System.exit(2);
        } catch (IOException e) {
            System.err.println("error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void usage() {
        System.err.println(String.join("\n",
            "usage: java SkinTool.java [--offline] <command> ...",
            "  devices [query] [--brand X] [--platform ios|android] [--foldable] [--limit N] [--json]",
            "      search the device database (query matches id/brand/name, all words)",
            "  device <id>          print the full JSON record",
            "  create <out.skin> (--device <id> | --width W --height H --ppi P) [overrides]",
            "      --name S --platform ios|android --tablet true|false --scale F",
            "      --cutout none|notch|island|hole  --corner-radius N  --bezel N  (viewbox units, 320 wide)",
            "      --body-color RRGGBB  --home-button true|false  --safe-top PT  --safe-bottom PT",
            "      --foldable [true|false] --fold-orientation vertical|horizontal",
            "      --fold-type continuous|dualScreen  --hinge-width N (viewbox units, dualScreen gap)",
            "      --cover-width PX --cover-height PX --cover-ppi P  --postures flat,half,closed",
            "      --crease true|false  --no-theme (skip embedding the native theme .res)"));
    }

    // ---------------------------------------------------------------- database

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> loadDevices() throws IOException {
        File cache = new File(HOME, "skin-devices.json");
        boolean fresh = cache.exists() && System.currentTimeMillis() - cache.lastModified() < 24L * 3600 * 1000;
        if (!fresh && !offline) {
            try {
                byte[] b = download(DB_URL);
                HOME.mkdirs();
                Files.write(cache.toPath(), b);
            } catch (IOException e) {
                if (!cache.exists()) {
                    throw new IOException("cannot download " + DB_URL + " (" + e.getMessage() + ") and no cache at " + cache);
                }
                System.err.println("warning: using stale cache " + cache + " (" + e.getMessage() + ")");
            }
        }
        if (!cache.exists()) {
            throw new IOException("offline and no cached database at " + cache);
        }
        Object root = new Json(Files.readString(cache.toPath(), StandardCharsets.UTF_8)).parse();
        Object list = ((Map<String, Object>) root).get("devices");
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : (List<Object>) list) {
            out.add((Map<String, Object>) o);
        }
        return out;
    }

    private static byte[] download(String url) throws IOException {
        try {
            HttpClient c = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpResponse<byte[]> r = c.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(60)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
            if (r.statusCode() != 200) {
                throw new IOException("HTTP " + r.statusCode() + " for " + url);
            }
            return r.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("interrupted");
        }
    }

    private static Map<String, Object> findDevice(String id) throws IOException {
        for (Map<String, Object> d : loadDevices()) {
            if (id.equals(str(d, "id", ""))) {
                return d;
            }
        }
        throw new IllegalArgumentException("no device with id '" + id + "' (try: devices <query>)");
    }

    private static void devices(List<String> a) throws IOException {
        String brand = null;
        String platform = null;
        boolean foldable = false;
        boolean json = false;
        int limit = 40;
        List<String> words = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            String s = a.get(i);
            switch (s) {
                case "--brand": brand = a.get(++i); break;
                case "--platform": platform = platformCode(a.get(++i)); break;
                case "--foldable": foldable = true; break;
                case "--json": json = true; break;
                case "--limit": limit = Integer.parseInt(a.get(++i)); break;
                default:
                    if (s.startsWith("--")) {
                        throw new IllegalArgumentException("unknown option " + s);
                    }
                    words.add(s.toLowerCase(Locale.ROOT));
            }
        }
        List<Map<String, Object>> hits = new ArrayList<>();
        for (Map<String, Object> d : loadDevices()) {
            if (brand != null && !brand.equalsIgnoreCase(str(d, "brand", ""))) continue;
            if (platform != null && !platform.equals(str(d, "platform", "and"))) continue;
            if (foldable && !(d.get("fold") instanceof Map)) continue;
            String hay = (str(d, "id", "") + " " + str(d, "brand", "") + " " + str(d, "name", "")).toLowerCase(Locale.ROOT);
            boolean ok = true;
            for (String w : words) {
                if (!hay.contains(w)) { ok = false; break; }
            }
            if (ok) hits.add(d);
        }
        int shown = Math.min(limit, hits.size());
        if (json) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < shown; i++) {
                if (i > 0) sb.append(",\n");
                Json.write(sb, hits.get(i));
            }
            System.out.println(sb.append("]"));
            return;
        }
        for (int i = 0; i < shown; i++) {
            Map<String, Object> d = hits.get(i);
            String cut = bool(d, "hasIsland") ? "island" : bool(d, "hasNotch") ? "notch"
                    : bool(d, "hasHole") ? "hole" : bool(d, "hasHome") ? "none" : "home-button";
            System.out.println(String.format(Locale.ROOT, "%-34s %-30s %5dx%-5d %4dppi %5.2fin %-11s%s%s",
                    str(d, "id", ""), trim(str(d, "brand", "") + " " + str(d, "name", ""), 30),
                    num(d, "w", 0), num(d, "h", 0), num(d, "ppi", 0), dbl(d, "inches", 0), cut,
                    d.get("fold") instanceof Map ? " foldable" : "", bool(d, "tablet") ? " tablet" : ""));
        }
        System.out.println(shown + " of " + hits.size() + " match(es)" + (hits.size() > shown ? " (raise --limit)" : ""));
    }

    private static void device(List<String> a) throws IOException {
        if (a.isEmpty()) throw new IllegalArgumentException("device <id>");
        StringBuilder sb = new StringBuilder();
        Json.write(sb, findDevice(a.get(0)));
        System.out.println(sb);
    }

    // ---------------------------------------------------------------- model

    /// The fields SkinDesigner reads from its Device and SkinModel.
    private static final class Spec {
        String name = "skin";
        int w, h, ppi;
        String platform = "and";
        boolean tablet;
        double scale;
        String sysFont = "Helvetica", propFont = "Helvetica", monoFont = "Courier";
        int cornerR = 40, bezel = 14;
        int bodyColor = 0x2a2f3a;
        String cutout;
        int safeTop = 40, safeBottom;
        boolean foldable, foldVertical = true, foldDual, foldHalf = true, foldClosed = true, foldCrease = true;
        int hingeVb = 6;
        int coverW, coverH, coverPpi;
    }

    @SuppressWarnings("unchecked")
    private static Spec fromDevice(Map<String, Object> d) {
        Spec s = new Spec();
        s.name = str(d, "brand", "") + " " + str(d, "name", "skin");
        s.w = num(d, "w", 0);
        s.h = num(d, "h", 0);
        s.ppi = num(d, "ppi", 0);
        s.platform = str(d, "platform", "and");
        s.tablet = bool(d, "tablet");
        s.scale = dbl(d, "scale", 0);
        if (d.get("fonts") instanceof Map) {
            Map<String, Object> f = (Map<String, Object>) d.get("fonts");
            s.sysFont = str(f, "system", "Helvetica");
            s.propFont = str(f, "prop", s.sysFont);
            s.monoFont = str(f, "mono", "Courier");
        }
        s.safeTop = num(d, "safeTop", 0);
        s.safeBottom = num(d, "safeBottom", 0);
        // SkinModel.resetForDevice presets.
        if (bool(d, "hasIsland")) {
            s.cutout = "island";
        } else if (bool(d, "hasNotch")) {
            s.cutout = "notch";
        } else if (bool(d, "hasHole")) {
            s.cutout = "hole";
        } else if (!bool(d, "hasHome")) {
            s.cornerR = 20;
            s.bezel = 64;
        }
        if (d.get("fold") instanceof Map) {
            Map<String, Object> f = (Map<String, Object>) d.get("fold");
            s.foldable = true;
            s.foldVertical = !"horizontal".equals(str(f, "orientation", "vertical"));
            s.foldDual = "dualScreen".equals(str(f, "type", "continuous"));
            s.foldCrease = !s.foldDual;
            s.coverW = num(f, "coverW", 0);
            s.coverH = num(f, "coverH", 0);
            s.coverPpi = num(f, "coverPpi", 0);
            // A Duo-style device's two screens have no camera cutout; the
            // camera is in the bezel.
            if (s.foldDual) {
                s.cutout = null;
                s.cornerR = 40;
                s.bezel = 14;
            }
        }
        return s;
    }

    private static void create(List<String> a) throws IOException {
        if (a.isEmpty() || a.get(0).startsWith("--")) throw new IllegalArgumentException("create <out.skin> ...");
        File out = new File(a.remove(0));
        Map<String, String> o = new LinkedHashMap<>();
        for (int i = 0; i < a.size(); i++) {
            String k = a.get(i);
            if (!k.startsWith("--")) throw new IllegalArgumentException("unexpected argument " + k);
            boolean flag = "--foldable".equals(k) || "--no-theme".equals(k);
            if (flag && (i + 1 >= a.size() || a.get(i + 1).startsWith("--"))) {
                o.put(k, "true");
            } else {
                if (i + 1 >= a.size()) throw new IllegalArgumentException(k + " needs a value");
                o.put(k, a.get(++i));
            }
        }
        Spec s;
        if (o.containsKey("--device")) {
            s = fromDevice(findDevice(o.remove("--device")));
        } else {
            s = new Spec();
            if (!o.containsKey("--width") || !o.containsKey("--height") || !o.containsKey("--ppi")) {
                throw new IllegalArgumentException("need --device <id> or --width, --height and --ppi");
            }
            s.name = "Custom";
        }
        boolean noTheme = false;
        for (Map.Entry<String, String> e : o.entrySet()) {
            String v = e.getValue();
            switch (e.getKey()) {
                case "--width": s.w = Integer.parseInt(v); break;
                case "--height": s.h = Integer.parseInt(v); break;
                case "--ppi": s.ppi = Integer.parseInt(v); break;
                case "--name": s.name = v; break;
                case "--platform": s.platform = platformCode(v); break;
                case "--tablet": s.tablet = Boolean.parseBoolean(v); break;
                case "--scale": s.scale = Double.parseDouble(v); break;
                case "--cutout":
                    if (!v.matches("none|notch|island|hole")) throw new IllegalArgumentException("--cutout none|notch|island|hole");
                    s.cutout = "none".equals(v) ? null : v;
                    break;
                case "--corner-radius": s.cornerR = Integer.parseInt(v); break;
                case "--bezel": s.bezel = Integer.parseInt(v); break;
                case "--body-color": s.bodyColor = Integer.parseInt(v.replace("#", "").replace("0x", ""), 16); break;
                case "--home-button":
                    if (Boolean.parseBoolean(v)) {
                        s.cutout = null;
                        if (!o.containsKey("--corner-radius")) s.cornerR = 20;
                        if (!o.containsKey("--bezel")) s.bezel = 64;
                    }
                    break;
                case "--safe-top": s.safeTop = Integer.parseInt(v); break;
                case "--safe-bottom": s.safeBottom = Integer.parseInt(v); break;
                case "--foldable": s.foldable = Boolean.parseBoolean(v); break;
                case "--fold-orientation":
                    if (!v.matches("vertical|horizontal")) throw new IllegalArgumentException("--fold-orientation vertical|horizontal");
                    s.foldVertical = "vertical".equals(v);
                    break;
                case "--fold-type":
                    if (!v.matches("continuous|dualScreen")) throw new IllegalArgumentException("--fold-type continuous|dualScreen");
                    s.foldDual = "dualScreen".equals(v);
                    if (!o.containsKey("--crease")) s.foldCrease = !s.foldDual;
                    break;
                case "--hinge-width": s.hingeVb = Integer.parseInt(v); break;
                case "--cover-width": s.coverW = Integer.parseInt(v); break;
                case "--cover-height": s.coverH = Integer.parseInt(v); break;
                case "--cover-ppi": s.coverPpi = Integer.parseInt(v); break;
                case "--postures":
                    s.foldHalf = v.contains("half");
                    s.foldClosed = v.contains("closed");
                    break;
                case "--crease": s.foldCrease = Boolean.parseBoolean(v); break;
                case "--no-theme": noTheme = Boolean.parseBoolean(v); break;
                default: throw new IllegalArgumentException("unknown option " + e.getKey());
            }
        }
        if (s.w <= 0 || s.h <= 0 || s.ppi <= 0) throw new IllegalArgumentException("width, height and ppi must be positive");
        if (s.scale <= 0) {
            // DeviceDatabase.parseDevice's fallback for catalogs without "scale".
            s.scale = !"ios".equals(s.platform) ? s.ppi / 160.0
                    : (s.w == 1080 && s.h == 1920) ? 1080.0 / 414.0 : Math.max(1, Math.round(s.ppi / 163.0));
        }
        if (s.foldable && s.coverPpi <= 0 && s.coverW > 0) s.coverPpi = s.ppi;

        BufferedImage portrait = frame(s, s.w, s.h, true);
        int bezelPx = bezelPx(s);
        BufferedImage map = new BufferedImage(portrait.getWidth(), portrait.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D mg = map.createGraphics();
        mg.setColor(Color.BLACK);
        mg.fillRect(bezelPx, bezelPx, s.w, s.h);
        mg.dispose();

        String themeFile = "ios".equals(s.platform) ? "iOS7Theme.res" : "and".equals(s.platform) ? "android_holo_light.res" : "winTheme.res";
        byte[] theme = noTheme ? null : theme(themeFile);
        File parent = out.getAbsoluteFile().getParentFile();
        if (parent != null) parent.mkdirs();
        try (OutputStream fos = Files.newOutputStream(out.toPath()); ZipOutputStream zos = new ZipOutputStream(fos)) {
            entry(zos, "skin.png", png(portrait));
            entry(zos, "skin_l.png", png(rotate90(portrait)));
            entry(zos, "skin_map.png", png(map));
            entry(zos, "skin_map_l.png", png(rotate90(map)));
            if (hasCover(s)) {
                BufferedImage cover = coverFrame(s);
                entry(zos, "cover.png", png(cover));
                entry(zos, "cover_l.png", png(rotate90(cover)));
            }
            if (theme != null) entry(zos, themeFile, theme);
            zos.putNextEntry(new ZipEntry("skin.properties"));
            properties(s, portrait.getWidth(), portrait.getHeight()).store(zos, "Created by the Codename One skin designer");
            zos.closeEntry();
        }
        System.out.println(out.getAbsolutePath());
        System.out.println(String.format(Locale.ROOT, "%s: %dx%d @%dppi (%s), frame %dx%d, bezel %dpx, cutout %s%s%s",
                s.name, s.w, s.h, s.ppi, "ios".equals(s.platform) ? "ios" : "android",
                portrait.getWidth(), portrait.getHeight(), bezelPx, s.cutout == null ? "none" : s.cutout,
                s.foldable ? ", foldable " + (s.foldVertical ? "vertical " : "horizontal ") + (s.foldDual ? "dualScreen" : "continuous")
                        + (hasCover(s) ? " + cover " + s.coverW + "x" + s.coverH : "") : "",
                theme == null ? ", no theme" : ", theme " + themeFile));
    }

    private static byte[] theme(String file) throws IOException {
        File cache = new File(new File(HOME, "skin-themes"), file);
        if (cache.exists() && (offline || System.currentTimeMillis() - cache.lastModified() < 7L * 24 * 3600 * 1000)) {
            return Files.readAllBytes(cache.toPath());
        }
        if (offline) {
            throw new IOException("offline and native theme " + file + " is not cached in " + cache.getParent()
                    + " (go online once, or pass --no-theme)");
        }
        try {
            byte[] b = download(THEME_DIR_URL + file);
            cache.getParentFile().mkdirs();
            Files.write(cache.toPath(), b);
            return b;
        } catch (IOException e) {
            if (cache.exists()) return Files.readAllBytes(cache.toPath());
            throw new IOException("cannot download native theme " + file + " (" + e.getMessage()
                    + ") and it is not cached; retry online or pass --no-theme");
        }
    }

    private static boolean hasCover(Spec s) {
        return s.foldable && s.coverW > 0 && s.coverH > 0 && s.coverPpi > 0;
    }

    /// SkinDesigner.pxPerUnit: frame units are 0.2 mm at the device's real
    /// density, so a bezel or camera hole is the same physical size on every
    /// device; the 320th-of-the-width scale is only the fallback.
    private static float vbScale(Spec s) {
        if (s.ppi > 0) return (float) (s.ppi * 0.2 / 25.4);
        return (float) s.w / VB_W;
    }

    private static int bezelPx(Spec s) {
        return Math.round(s.bezel * vbScale(s));
    }

    // ---------------------------------------------------------------- images

    /// SkinDesigner.generatePortraitImage / generateCoverImage: dark base, a
    /// lighter body round rect inset by w/200, the outer corners masked
    /// transparent, the screen carved transparent with a radius 8 units
    /// smaller, then the cutouts painted opaque black over the screen.
    private static BufferedImage frame(Spec s, int sw, int sh, boolean cutouts) {
        float k = vbScale(s);
        int bezelPx = bezelPx(s);
        int cornerPx = Math.round(s.cornerR * k);
        int screenCornerPx = Math.max(0, cornerPx - Math.round(8 * k));
        int tw = sw + bezelPx * 2;
        int th = sh + bezelPx * 2;
        BufferedImage img = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xff121822, true));
        g.fillRect(0, 0, tw, th);
        g.setColor(new Color(s.bodyColor | 0xff000000, true));
        int inset = Math.max(1, tw / 200);
        int arc = Math.max(0, (cornerPx - inset) * 2);
        g.fill(new RoundRectangle2D.Float(inset, inset, tw - inset * 2, th - inset * 2, arc, arc));
        g.dispose();
        int[] data = img.getRGB(0, 0, tw, th, null, 0, tw);
        maskCorners(data, tw, th, cornerPx);
        carveScreen(data, tw, th, bezelPx, bezelPx, sw, sh, screenCornerPx);
        if (cutouts && s.cutout != null) {
            int[] r = cutoutRect(s);
            int x0 = bezelPx + r[0];
            int y0 = bezelPx + r[1];
            if ("notch".equals(s.cutout)) {
                int rad = Math.max(2, Math.round(8 * k));
                fillRounded(data, tw, th, x0, y0, r[2], r[3], rad);
                fillRounded(data, tw, th, x0, y0, r[2], Math.min(r[3], rad), 0);
            } else if ("island".equals(s.cutout)) {
                fillRounded(data, tw, th, x0, y0, r[2], r[3], r[3] / 2);
            } else {
                fillEllipse(data, tw, th, x0, y0, r[2], r[3]);
            }
        }
        img.setRGB(0, 0, tw, th, data, 0, tw);
        return img;
    }

    /// SkinDesigner.generateCoverImage: the closed device is the folded half
    /// of the open body, with the cover screen at its physical size.
    private static BufferedImage coverFrame(Spec s) {
        float k = vbScale(s);
        int bezelPx = bezelPx(s);
        int cornerPx = Math.round(s.cornerR * k);
        int screenCornerPx = Math.max(0, cornerPx - Math.round(8 * k));
        int[] body = coverBody(s);
        int[] cs = coverScreen(s);
        int tw = body[0], th = body[1];
        BufferedImage img = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0xff121822, true));
        g.fillRect(0, 0, tw, th);
        g.setColor(new Color(s.bodyColor | 0xff000000, true));
        int inset = Math.max(1, tw / 200);
        int arc = Math.max(0, (cornerPx - inset) * 2);
        g.fill(new RoundRectangle2D.Float(inset, inset, tw - inset * 2, th - inset * 2, arc, arc));
        g.dispose();
        int[] data = img.getRGB(0, 0, tw, th, null, 0, tw);
        maskCorners(data, tw, th, cornerPx);
        carveScreen(data, tw, th, cs[0], cs[1], cs[2], cs[3], Math.min(screenCornerPx, Math.min(cs[2], cs[3]) / 4));
        img.setRGB(0, 0, tw, th, data, 0, tw);
        return img;
    }

    private static int[] coverBody(Spec s) {
        int b = bezelPx(s);
        int tw = s.w + b * 2, th = s.h + b * 2;
        return s.foldVertical ? new int[] {tw / 2, th} : new int[] {tw, th / 2};
    }

    /// SkinDesigner.coverScreenRect: centred on a book fold, at the top of a flip.
    private static int[] coverScreen(Spec s) {
        int b = bezelPx(s);
        int[] body = coverBody(s);
        double k = s.coverPpi > 0 ? s.ppi / (double) s.coverPpi : 1;
        double cw = s.coverW * k, ch = s.coverH * k;
        double fit = Math.min(1, Math.min((body[0] - b * 2) / cw, (body[1] - b * 2) / ch));
        int w = Math.max(1, (int) Math.round(cw * fit));
        int h = Math.max(1, (int) Math.round(ch * fit));
        return new int[] {(body[0] - w) / 2, s.foldVertical ? (body[1] - h) / 2 : b, w, h};
    }

    /// SkinModel cutout presets (viewbox units) -> screen-relative pixels.
    private static int[] cutoutRect(Spec s) {
        int w, h, y;
        switch (s.cutout) {
            case "island": w = 120; h = 35; y = 14; break;
            case "notch": w = 180; h = 30; y = 0; break;
            default: w = 17; h = 17; y = 15; break;
        }
        float k = vbScale(s);
        int cw = Math.round(w * k);
        int ch = Math.round(h * k);
        return new int[] {s.w / 2 - cw / 2, Math.round(y * k), cw, ch};
    }

    /// SkinDesigner.applyRoundRectAlphaMask: every pixel of each r x r
    /// corner box, outermost row and column included, with anti-aliased
    /// coverage sampled at the pixel centre.
    private static void maskCorners(int[] d, int w, int h, int radius) {
        if (radius <= 0) return;
        int r = Math.min(radius, Math.min(w, h) / 2);
        for (int corner = 0; corner < 4; corner++) {
            boolean right = corner == 1 || corner == 3;
            boolean bottom = corner >= 2;
            float cx = right ? w - r : r;
            float cy = bottom ? h - r : r;
            int bx = right ? w - r : 0;
            int by = bottom ? h - r : 0;
            for (int py = by; py < by + r; py++) {
                for (int px = bx; px < bx + r; px++) {
                    float dx = px + 0.5f - cx, dy = py + 0.5f - cy;
                    float dist = (float) Math.sqrt(dx * dx + dy * dy) - r;
                    int i = py * w + px;
                    if (dist >= 0.5f) d[i] = 0;
                    else if (dist > -0.5f) {
                        int a = Math.round(((d[i] >>> 24) & 0xff) * (0.5f - dist));
                        d[i] = (a << 24) | (d[i] & 0xffffff);
                    }
                }
            }
        }
    }

    private static void carveScreen(int[] d, int w, int h, int x, int y, int rw, int rh, int radius) {
        int r = Math.max(0, Math.min(radius, Math.min(rw, rh) / 2));
        int right = x + rw - r - 1;
        int bottom = y + rh - r - 1;
        for (int yy = Math.max(0, y); yy < Math.min(h, y + rh); yy++) {
            for (int xx = Math.max(0, x); xx < Math.min(w, x + rw); xx++) {
                int rx = xx - x;
                int ry = yy - y;
                int dx, dy;
                if (r > 0 && rx < r && ry < r) { dx = r - rx; dy = r - ry; }
                else if (r > 0 && xx > right && ry < r) { dx = xx - right; dy = r - ry; }
                else if (r > 0 && rx < r && yy > bottom) { dx = r - rx; dy = yy - bottom; }
                else if (r > 0 && xx > right && yy > bottom) { dx = xx - right; dy = yy - bottom; }
                else { d[yy * w + xx] = 0; continue; }
                if (dx * dx + dy * dy <= r * r) d[yy * w + xx] = 0;
            }
        }
    }

    private static void fillRounded(int[] d, int w, int h, int x, int y, int rw, int rh, int r) {
        for (int yy = Math.max(0, y); yy < Math.min(h, y + rh); yy++) {
            for (int xx = Math.max(0, x); xx < Math.min(w, x + rw); xx++) {
                int dx = xx - x, dy = yy - y, mw = rw - 1, mh = rh - 1, cx, cy;
                if (dx < r && dy < r) { cx = r - dx; cy = r - dy; }
                else if (dx > mw - r && dy < r) { cx = dx - (mw - r); cy = r - dy; }
                else if (dx < r && dy > mh - r) { cx = r - dx; cy = dy - (mh - r); }
                else if (dx > mw - r && dy > mh - r) { cx = dx - (mw - r); cy = dy - (mh - r); }
                else { d[yy * w + xx] = 0xff000000; continue; }
                if (cx * cx + cy * cy <= r * r) d[yy * w + xx] = 0xff000000;
            }
        }
    }

    private static void fillEllipse(int[] d, int w, int h, int x, int y, int rw, int rh) {
        if (rw <= 0 || rh <= 0) return;
        float ax = rw / 2f, by = rh / 2f, cx = x + ax - 0.5f, cy = y + by - 0.5f;
        for (int yy = Math.max(0, y); yy < Math.min(h, y + rh); yy++) {
            for (int xx = Math.max(0, x); xx < Math.min(w, x + rw); xx++) {
                float dx = (xx - cx) / ax, dy = (yy - cy) / by;
                if (dx * dx + dy * dy <= 1f) d[yy * w + xx] = 0xff000000;
            }
        }
    }

    /// Clockwise, as Image.rotate90Degrees(true).
    private static BufferedImage rotate90(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage dst = new BufferedImage(h, w, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                dst.setRGB(h - 1 - y, x, src.getRGB(x, y));
            }
        }
        return dst;
    }

    private static byte[] png(BufferedImage img) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }

    private static void entry(ZipOutputStream zos, String name, byte[] b) throws IOException {
        zos.putNextEntry(new ZipEntry(name));
        zos.write(b);
        zos.closeEntry();
    }

    // ---------------------------------------------------------------- properties

    /// SkinDesigner.buildProperties + putFoldProperties, key for key.
    private static Properties properties(Spec s, int totalW, int totalH) {
        Properties p = new Properties();
        p.put("touch", "true");
        p.put("platformName", s.platform);
        p.put("tablet", String.valueOf(s.tablet));
        p.put("systemFontFamily", s.sysFont);
        p.put("proportionalFontFamily", s.propFont);
        p.put("monospaceFontFamily", s.monoFont);
        p.put("ppi", String.valueOf(s.ppi));
        p.put("pixelRatio", String.valueOf(s.ppi / 25.4));
        String form = s.tablet ? "tablet" : "phone";
        String names;
        if ("ios".equals(s.platform)) names = form + ",ios," + (s.tablet ? "ipad" : "iphone");
        else if ("and".equals(s.platform)) names = form + ",android," + (s.tablet ? "android-tablet" : "android-phone");
        else names = form + ",windows,desktop";
        p.put("overrideNames", names);
        int bezelPx = bezelPx(s);
        p.put("roundScreen", "true");
        p.put("displayX", String.valueOf(bezelPx));
        p.put("displayY", String.valueOf(bezelPx));
        p.put("displayWidth", String.valueOf(s.w));
        p.put("displayHeight", String.valueOf(s.h));
        int cutoutBottom = 0;
        if (s.cutout != null) {
            int[] r = cutoutRect(s);
            cutoutBottom = r[1] + r[3];
            p.put("cutouts", r[0] + "," + r[1] + "," + r[2] + "," + r[3]);
        }
        int safeTop = Math.max((int) Math.round(s.safeTop * s.scale), cutoutBottom);
        int safeBottom = (int) Math.round(s.safeBottom * s.scale);
        int safeH = Math.max(1, s.h - safeTop - safeBottom);
        p.put("safePortraitX", "0");
        p.put("safePortraitY", String.valueOf(safeTop));
        p.put("safePortraitWidth", String.valueOf(s.w));
        p.put("safePortraitHeight", String.valueOf(safeH));
        p.put("safeLandscapeX", String.valueOf(safeBottom));
        p.put("safeLandscapeY", "0");
        p.put("safeLandscapeWidth", String.valueOf(safeH));
        p.put("safeLandscapeHeight", String.valueOf(s.w));
        if (!s.foldable) return p;
        p.put("foldable", "true");
        p.put("foldOrientation", s.foldVertical ? "vertical" : "horizontal");
        p.put("foldType", s.foldDual ? "dualScreen" : "continuous");
        p.put("foldPostures", "flat" + (s.foldHalf ? ",half" : "") + (s.foldClosed ? ",closed" : ""));
        p.put("crease", String.valueOf(s.foldCrease && !s.foldDual));
        int gap = s.foldDual ? Math.max(2, Math.round(s.hingeVb * vbScale(s))) : 0;
        int hx, hy, hw, hh;
        if (s.foldVertical) {
            hx = bezelPx + s.w / 2 - gap / 2; hy = bezelPx; hw = gap; hh = s.h;
        } else {
            hx = bezelPx; hy = bezelPx + s.h / 2 - gap / 2; hw = s.w; hh = gap;
        }
        p.put("hingePortrait", hx + "," + hy + "," + hw + "," + hh);
        p.put("hingeLandscape", (totalH - hy - hh) + "," + hx + "," + hh + "," + hw);
        if (hasCover(s)) {
            int[] cs = coverScreen(s);
            int coverH = coverBody(s)[1];
            p.put("coverPortraitScreenX", String.valueOf(cs[0]));
            p.put("coverPortraitScreenY", String.valueOf(cs[1]));
            p.put("coverPortraitScreenWidth", String.valueOf(cs[2]));
            p.put("coverPortraitScreenHeight", String.valueOf(cs[3]));
            // Clockwise rotation of the cover image: (x,y,w,h) -> (H-y-h, x, h, w).
            p.put("coverLandscapeScreenX", String.valueOf(coverH - cs[1] - cs[3]));
            p.put("coverLandscapeScreenY", String.valueOf(cs[0]));
            p.put("coverLandscapeScreenWidth", String.valueOf(cs[3]));
            p.put("coverLandscapeScreenHeight", String.valueOf(cs[2]));
            // SkinDesigner: the cover's ppi scaled by the size it was drawn at,
            // so a cover shrunk to fit keeps its physical size.
            p.put("coverPpi", String.valueOf(Math.max(1, Math.round(s.coverPpi * (double) cs[2] / s.coverW))));
        }
        return p;
    }

    // ---------------------------------------------------------------- helpers

    private static String platformCode(String v) {
        String l = v.toLowerCase(Locale.ROOT);
        if (l.equals("ios")) return "ios";
        if (l.equals("android") || l.equals("and")) return "and";
        throw new IllegalArgumentException("platform must be ios or android");
    }

    private static String trim(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 1) + "~";
    }

    private static String str(Map<String, Object> m, String k, String def) {
        Object v = m.get(k);
        return v == null ? def : v.toString();
    }

    private static int num(Map<String, Object> m, String k, int def) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).intValue() : def;
    }

    private static double dbl(Map<String, Object> m, String k, double def) {
        Object v = m.get(k);
        return v instanceof Number ? ((Number) v).doubleValue() : def;
    }

    private static boolean bool(Map<String, Object> m, String k) {
        return Boolean.TRUE.equals(m.get(k));
    }

    /// Minimal JSON reader/writer; enough for devices.json.
    private static final class Json {
        private final String s;
        private int i;

        Json(String s) { this.s = s; }

        Object parse() {
            ws();
            char c = s.charAt(i);
            if (c == '{') {
                Map<String, Object> m = new LinkedHashMap<>();
                i++; ws();
                if (s.charAt(i) == '}') { i++; return m; }
                while (true) {
                    ws(); String k = string(); ws(); i++; // ':'
                    m.put(k, parse()); ws();
                    if (s.charAt(i++) == '}') return m;
                }
            }
            if (c == '[') {
                List<Object> l = new ArrayList<>();
                i++; ws();
                if (s.charAt(i) == ']') { i++; return l; }
                while (true) {
                    l.add(parse()); ws();
                    if (s.charAt(i++) == ']') return l;
                }
            }
            if (c == '"') return string();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            int st = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            String n = s.substring(st, i);
            if (n.isEmpty()) throw new IllegalStateException("bad JSON at " + st);
            if (n.contains(".") || n.contains("e") || n.contains("E")) return Double.parseDouble(n);
            return Long.parseLong(n);
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
        }

        private String string() {
            StringBuilder b = new StringBuilder();
            i++;
            while (true) {
                char c = s.charAt(i++);
                if (c == '"') return b.toString();
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n': b.append('\n'); break;
                        case 't': b.append('\t'); break;
                        case 'r': b.append('\r'); break;
                        case 'b': b.append('\b'); break;
                        case 'f': b.append('\f'); break;
                        case 'u': b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); i += 4; break;
                        default: b.append(e);
                    }
                } else {
                    b.append(c);
                }
            }
        }

        @SuppressWarnings("unchecked")
        static void write(StringBuilder b, Object o) {
            if (o instanceof Map) {
                b.append('{');
                boolean first = true;
                for (Map.Entry<String, Object> e : ((Map<String, Object>) o).entrySet()) {
                    if (!first) b.append(',');
                    first = false;
                    write(b, e.getKey());
                    b.append(':');
                    write(b, e.getValue());
                }
                b.append('}');
            } else if (o instanceof List) {
                b.append('[');
                List<Object> l = (List<Object>) o;
                for (int k = 0; k < l.size(); k++) {
                    if (k > 0) b.append(',');
                    write(b, l.get(k));
                }
                b.append(']');
            } else if (o instanceof String) {
                b.append('"');
                for (char c : ((String) o).toCharArray()) {
                    if (c == '"' || c == '\\') b.append('\\').append(c);
                    else if (c < 0x20 || c > 0x7e) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
                }
                b.append('"');
            } else {
                b.append(o);
            }
        }
    }
}
