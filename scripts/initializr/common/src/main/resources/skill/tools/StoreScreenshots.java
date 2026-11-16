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
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/// Turns screenshots of the running app into the exact images the App Store and
/// Google Play accept, headless, with nothing but a JDK.
///
/// ```
/// java tools/StoreScreenshots.java --in shots --out store --captions captions.txt --bg 1f5eff
/// java tools/StoreScreenshots.java --check store/apple/APP_IPHONE_67/1.png
/// java tools/StoreScreenshots.java --sizes
/// ```
///
/// Capture the inputs with `java tools/Sim.java screenshot shots/1.png` (simulator)
/// or `java tools/Sim.java device-screenshot ios shots/1.png` (an iPhone Pro Max
/// simulator gives the sharpest result), one per screen, in the order the store
/// should show them. Each input becomes one image per slot: the screen scaled
/// onto a `--bg` background with rounded corners and, when `--captions` names a
/// file, that line of the file as a headline above it (line 1 for the first
/// input, and so on; an empty line means no caption).
///
/// Output is `<out>/<store>/<displayType>/<n>.png`: opaque (both stores refuse
/// an alpha channel) and at exactly the slot's size. The store and display type
/// are the values the BuildCloud screenshot upload takes
/// (`POST /api/v2/console/apps/{package}/metadata/screenshots`, see
/// `references/cloud-agent.md`).
///
/// Options: `--store apple|google|all` (default all), `--ipad` to add the iPad
/// slot (needed only when the app supports iPad), `--fg RRGGBB` caption colour
/// (default white, or near-black on a light background), `--fit` to scale the
/// screen to the full slot with no frame or caption.
///
/// Exit status: 0 on success, 1 when `--check` finds a problem, 2 for a bad command line.
public class StoreScreenshots {

    /// One store slot: the store, the upload's display type, and the size it must be.
    record Slot(String store, String displayType, int width, int height, boolean ipad, String note) {
    }

    // Sizes from the stores' own specifications. Apple's 6.7"/6.9" set is the one
    // required iPhone set (smaller iPhones are scaled from it); Play takes any size
    // from 320 to 3840 px with sides at most 2:1, and 1080x1920 is its usual phone shot.
    static final List<Slot> SLOTS = List.of(
            new Slot("apple", "APP_IPHONE_67", 1290, 2796, false, "iPhone 6.7\"/6.9\", required"),
            new Slot("apple", "APP_IPAD_PRO_3GEN_129", 2048, 2732, true, "iPad 12.9\"/13\", required if the app runs on iPad"),
            new Slot("google", "phoneScreenshots", 1080, 1920, false, "Phone, 2 to 8 required"),
            new Slot("google", "tenInchScreenshots", 1600, 2560, true, "10\" tablet, needed for tablet listing"));

    public static void main(String[] args) throws Exception {
        String in = null, out = null, captions = null, store = "all";
        String bg = "1f5eff", fg = null;
        boolean ipad = false, fit = false;
        List<String> check = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "--sizes" -> {
                    for (Slot s : SLOTS) {
                        System.out.println(s.store() + " " + s.displayType() + " " + s.width() + "x" + s.height()
                                + " - " + s.note());
                    }
                    return;
                }
                case "--check" -> {
                    while (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                        check.add(args[++i]);
                    }
                }
                case "--ipad" -> ipad = true;
                case "--fit" -> fit = true;
                case "--in" -> in = value(args, ++i, a);
                case "--out" -> out = value(args, ++i, a);
                case "--captions" -> captions = value(args, ++i, a);
                case "--store" -> store = value(args, ++i, a).toLowerCase(Locale.ROOT);
                case "--bg" -> bg = value(args, ++i, a);
                case "--fg" -> fg = value(args, ++i, a);
                default -> usage("unknown option " + a);
            }
        }
        if (!check.isEmpty()) {
            System.exit(check(check));
        }
        if (in == null || out == null) {
            usage("--in and --out are required");
        }
        if (!List.of("all", "apple", "google").contains(store)) {
            usage("--store is apple, google or all");
        }
        Color background = colour(bg);
        Color text = fg != null ? colour(fg) : luminance(background) > 0.6 ? new Color(0x1a1a1a) : Color.WHITE;
        List<Path> inputs = inputs(Paths.get(in));
        if (inputs.isEmpty()) {
            usage("no PNG or JPEG files in " + in);
        }
        List<String> lines = captions == null ? List.of()
                : Files.readAllLines(Paths.get(captions), StandardCharsets.UTF_8);
        int written = 0;
        for (Slot slot : SLOTS) {
            if (!store.equals("all") && !store.equals(slot.store()) || slot.ipad() && !ipad) {
                continue;
            }
            Path dir = Paths.get(out, slot.store(), slot.displayType());
            Files.createDirectories(dir);
            for (int n = 0; n < inputs.size(); n++) {
                BufferedImage shot = ImageIO.read(inputs.get(n).toFile());
                if (shot == null) {
                    usage("cannot read " + inputs.get(n));
                }
                String caption = n < lines.size() ? lines.get(n).trim() : "";
                BufferedImage img = fit ? fitted(shot, slot, background)
                        : framed(shot, slot, background, text, caption);
                Path file = dir.resolve((n + 1) + ".png");
                ImageIO.write(img, "png", file.toFile());
                written++;
            }
            System.out.println(slot.store() + " " + slot.displayType() + ": " + inputs.size() + " image(s) in " + dir);
        }
        if (inputs.size() > 8) {
            System.err.println("warning: Google Play takes at most 8 phone screenshots and the App Store 10;"
                    + " upload the best ones");
        }
        System.out.println("Wrote " + written + " screenshot(s)");
    }

    /// The screen on a coloured background, with a headline above it when there is one.
    static BufferedImage framed(BufferedImage shot, Slot slot, Color bg, Color fg, String caption) {
        int w = slot.width(), h = slot.height();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        hints(g);
        g.setColor(bg);
        g.fillRect(0, 0, w, h);
        int margin = Math.round(w * 0.07f);
        int top = margin;
        if (!caption.isEmpty()) {
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, Math.round(w * 0.065f));
            g.setFont(font);
            g.setColor(fg);
            List<String> wrapped = wrap(caption, g.getFontMetrics(), w - 2 * margin);
            FontMetrics fm = g.getFontMetrics();
            int y = margin + fm.getAscent();
            for (String line : wrapped.subList(0, Math.min(3, wrapped.size()))) {
                g.drawString(line, (w - fm.stringWidth(line)) / 2, y);
                y += fm.getHeight();
            }
            top = y - fm.getAscent() + Math.round(margin * 0.6f);
        }
        int boxW = w - 2 * margin, boxH = h - top - margin;
        double scale = Math.min((double) boxW / shot.getWidth(), (double) boxH / shot.getHeight());
        int sw = (int) Math.round(shot.getWidth() * scale), sh = (int) Math.round(shot.getHeight() * scale);
        int x = (w - sw) / 2;
        int radius = Math.round(sw * 0.06f);
        g.setClip(new RoundRectangle2D.Float(x, top, sw, sh, radius, radius));
        g.drawImage(shot, x, top, sw, sh, null);
        g.dispose();
        return img;
    }

    /// The screen alone, scaled to cover the slot (cropping the overflow evenly).
    static BufferedImage fitted(BufferedImage shot, Slot slot, Color bg) {
        int w = slot.width(), h = slot.height();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        hints(g);
        g.setColor(bg);
        g.fillRect(0, 0, w, h);
        double scale = Math.max((double) w / shot.getWidth(), (double) h / shot.getHeight());
        int sw = (int) Math.round(shot.getWidth() * scale), sh = (int) Math.round(shot.getHeight() * scale);
        g.drawImage(shot, (w - sw) / 2, (h - sh) / 2, sw, sh, null);
        g.dispose();
        return img;
    }

    /// Which slot each file fits, and why not when it fits none.
    static int check(List<String> files) throws Exception {
        int status = 0;
        for (String f : files) {
            BufferedImage img = ImageIO.read(Paths.get(f).toFile());
            if (img == null) {
                System.out.println(f + ": NOT AN IMAGE (PNG or JPEG only)");
                status = 1;
                continue;
            }
            int w = img.getWidth(), h = img.getHeight();
            List<String> fits = new ArrayList<>();
            for (Slot s : SLOTS) {
                if (s.width() == w && s.height() == h || s.width() == h && s.height() == w) {
                    fits.add(s.store() + " " + s.displayType());
                }
            }
            int lo = Math.min(w, h), hi = Math.max(w, h);
            boolean play = lo >= 320 && hi <= 3840 && hi <= 2 * lo;
            if (play && fits.stream().noneMatch(s -> s.startsWith("google"))) {
                fits.add("google (any phone slot: 320-3840 px, sides at most 2:1)");
            }
            String problem = img.getColorModel().hasAlpha() ? "has an alpha channel; both stores refuse it" : null;
            if (fits.isEmpty()) {
                problem = (problem == null ? "" : problem + "; ") + w + "x" + h + " fits no store slot (--sizes)";
            }
            if (problem != null) {
                System.out.println(f + ": PROBLEM " + problem);
                status = 1;
            } else {
                System.out.println(f + ": OK " + String.join(", ", fits));
            }
        }
        return status;
    }

    static List<String> wrap(String text, FontMetrics fm, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String next = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(next) > width && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(next);
            }
        }
        if (line.length() > 0) {
            lines.add(line.toString());
        }
        return lines;
    }

    static List<Path> inputs(Path in) throws Exception {
        List<Path> out = new ArrayList<>();
        if (Files.isRegularFile(in)) {
            out.add(in);
            return out;
        }
        if (!Files.isDirectory(in)) {
            return out;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(in)) {
            for (Path p : ds) {
                String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")) {
                    out.add(p);
                }
            }
        }
        // Natural order, so 2.png comes before 10.png.
        out.sort((a, b) -> {
            String x = a.getFileName().toString(), y = b.getFileName().toString();
            String dx = x.replaceAll("\\D", ""), dy = y.replaceAll("\\D", "");
            if (!dx.isEmpty() && !dy.isEmpty() && dx.length() < 10 && dy.length() < 10) {
                int c = Integer.compare(Integer.parseInt(dx), Integer.parseInt(dy));
                if (c != 0) {
                    return c;
                }
            }
            return x.compareTo(y);
        });
        return out;
    }

    static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    static Color colour(String hex) {
        String h = hex.startsWith("#") ? hex.substring(1) : hex;
        if (!h.matches("[0-9a-fA-F]{6}")) {
            usage("colours are RRGGBB, e.g. 1f5eff");
        }
        return new Color(Integer.parseInt(h, 16));
    }

    static double luminance(Color c) {
        return (0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue()) / 255.0;
    }

    static String value(String[] args, int i, String option) {
        if (i >= args.length) {
            usage("missing value for " + option);
        }
        return args[i];
    }

    static void usage(String error) {
        System.err.println("error: " + error);
        System.err.println("usage: java StoreScreenshots.java --in <dir|file> --out <dir> [--captions file]"
                + " [--bg RRGGBB] [--fg RRGGBB] [--store apple|google|all] [--ipad] [--fit]");
        System.err.println("       java StoreScreenshots.java --check <png>...   |   --sizes");
        System.exit(2);
    }
}
