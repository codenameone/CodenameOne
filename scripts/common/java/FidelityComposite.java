import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.imageio.ImageIO;

/// Renders human-readable "fidelity cards" from a ProcessScreenshots --mode
/// fidelity comparison: for each component+state, one PNG showing the NATIVE
/// widget (left) next to the Codename One render (right), per appearance
/// (light/dark rows), with the fidelity percentage beside each pair. Also emits
/// a single contact-sheet overview. Pure host-side AWT, run after a fidelity run
/// (and in CI) to produce a visual guide of where the themes stand.
///
/// Inputs are paired by name: native references live in --native-dir as
/// "<name>.png", CN1 renders in --cn1-dir as "<name>_cn1.png"; scores come from
/// --compare-json (details.fidelity_percent keyed by test name "<name>").
public class FidelityComposite {
    // Each thumbnail is scaled to this height; width follows the tile aspect,
    // capped so very wide tiles stay readable side by side.
    private static final int THUMB_H = 90;
    private static final int THUMB_MAX_W = 380;
    private static final int PAD = 12;
    private static final int LABEL_W = 70;      // "light"/"dark" gutter
    private static final int PCT_W = 110;       // percentage column
    private static final Color CARD_BG = new Color(0xF2F2F2);
    private static final Color INK = new Color(0x202020);
    private static final Color FRAME = new Color(0xB0B0B0);
    private static final Color CHECK_A = new Color(0xFFFFFF);
    private static final Color CHECK_B = new Color(0xE0E0E0);

    public static void main(String[] args) throws Exception {
        Path nativeDir = null;
        Path cn1Dir = null;
        Path compareJson = null;
        Path outDir = null;
        String title = "Native theme fidelity";
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--native-dir" -> nativeDir = Path.of(args[++i]);
                case "--cn1-dir" -> cn1Dir = Path.of(args[++i]);
                case "--compare-json" -> compareJson = Path.of(args[++i]);
                case "--out" -> outDir = Path.of(args[++i]);
                case "--title" -> title = args[++i];
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    System.exit(2);
                }
            }
        }
        if (nativeDir == null || cn1Dir == null || outDir == null) {
            System.err.println("--native-dir, --cn1-dir and --out are required");
            System.exit(2);
            return;
        }
        Files.createDirectories(outDir);
        Map<String, Double> scores = loadScores(compareJson);

        // Discover pairs: a CN1 render "<name>_cn1.png" with a native "<name>.png".
        // Group by base = component+state (the name minus its _light/_dark suffix);
        // each group has up to two appearance rows.
        Map<String, Map<String, String>> groups = new TreeMap<>(); // base -> appearance -> name
        try (java.util.stream.Stream<Path> s = Files.list(cn1Dir)) {
            List<String> names = s.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith("_cn1.png"))
                    .map(n -> n.substring(0, n.length() - "_cn1.png".length()))
                    .sorted()
                    .toList();
            for (String name : names) {
                String appearance = name.endsWith("_dark") ? "dark"
                        : name.endsWith("_light") ? "light" : "";
                String base = appearance.isEmpty() ? name
                        : name.substring(0, name.length() - (appearance.length() + 1));
                groups.computeIfAbsent(base, k -> new TreeMap<>()).put(appearance, name);
            }
        }

        List<Path> cards = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> g : groups.entrySet()) {
            Path card = renderCard(g.getKey(), g.getValue(), nativeDir, cn1Dir, scores, outDir);
            if (card != null) {
                cards.add(card);
            }
        }
        Path sheet = renderContactSheet(title, cards, outDir);
        System.out.println("Wrote " + cards.size() + " fidelity card(s) to " + outDir);
        if (sheet != null) {
            System.out.println("Contact sheet: " + sheet);
        }
    }

    private static Path renderCard(String base, Map<String, String> byAppearance, Path nativeDir,
            Path cn1Dir, Map<String, Double> scores, Path outDir) throws IOException {
        List<String> appearances = new ArrayList<>(byAppearance.keySet());
        int rowH = THUMB_H + PAD;
        int titleH = 34;
        int headerH = 22;
        int contentW = LABEL_W + THUMB_MAX_W + PAD + THUMB_MAX_W + PCT_W + PAD * 2;
        int cardW = contentW + PAD * 2;
        int cardH = titleH + headerH + appearances.size() * rowH + PAD * 2;

        BufferedImage card = new BufferedImage(cardW, cardH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = card.createGraphics();
        hints(g);
        g.setColor(CARD_BG);
        g.fillRect(0, 0, cardW, cardH);

        g.setColor(INK);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString(prettyTitle(base), PAD, PAD + 18);

        // Column headers
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(0x606060));
        int colNativeX = PAD + LABEL_W;
        int colCn1X = colNativeX + THUMB_MAX_W + PAD;
        int y0 = PAD + titleH;
        g.drawString("native", colNativeX, y0 + 14);
        g.drawString("Codename One", colCn1X, y0 + 14);
        g.drawString("fidelity", colCn1X + THUMB_MAX_W + PAD, y0 + 14);

        int y = y0 + headerH;
        for (String appearance : appearances) {
            String name = byAppearance.get(appearance);
            BufferedImage nat = readImage(nativeDir.resolve(name + ".png"));
            BufferedImage cn1 = readImage(cn1Dir.resolve(name + "_cn1.png"));
            g.setColor(INK);
            g.setFont(new Font("SansSerif", Font.PLAIN, 13));
            g.drawString(appearance, PAD, y + THUMB_H / 2 + 4);
            drawThumb(g, nat, colNativeX, y);
            drawThumb(g, cn1, colCn1X, y);
            Double pct = scores.get(name);
            g.setFont(new Font("SansSerif", Font.BOLD, 17));
            g.setColor(scoreColor(pct));
            g.drawString(pct == null ? "n/a" : String.format("%.1f%%", pct),
                    colCn1X + THUMB_MAX_W + PAD, y + THUMB_H / 2 + 6);
            y += rowH;
        }
        g.dispose();
        Path out = outDir.resolve(base + ".png");
        ImageIO.write(card, "png", out.toFile());
        return out;
    }

    private static void drawThumb(Graphics2D g, BufferedImage img, int x, int y) {
        // Checkerboard behind so an all-dark (or all-light) tile is still framed
        // and distinguishable from the card background.
        int w = THUMB_MAX_W;
        int h = THUMB_H;
        if (img != null) {
            double scale = Math.min((double) THUMB_MAX_W / img.getWidth(), (double) THUMB_H / img.getHeight());
            w = Math.max(1, (int) Math.round(img.getWidth() * scale));
            h = Math.max(1, (int) Math.round(img.getHeight() * scale));
        }
        checker(g, x, y, w, h);
        if (img != null) {
            g.drawImage(img, x, y, w, h, null);
        }
        g.setColor(FRAME);
        g.drawRect(x, y, w, h);
    }

    private static void checker(Graphics2D g, int x, int y, int w, int h) {
        int c = 8;
        for (int yy = 0; yy < h; yy += c) {
            for (int xx = 0; xx < w; xx += c) {
                g.setColor(((xx / c + yy / c) % 2 == 0) ? CHECK_A : CHECK_B);
                g.fillRect(x + xx, y + yy, Math.min(c, w - xx), Math.min(c, h - yy));
            }
        }
    }

    private static Path renderContactSheet(String title, List<Path> cards, Path outDir) throws IOException {
        if (cards.isEmpty()) {
            return null;
        }
        List<BufferedImage> imgs = new ArrayList<>();
        int maxW = 0;
        int totalH = 0;
        for (Path c : cards) {
            BufferedImage img = readImage(c);
            if (img != null) {
                imgs.add(img);
                maxW = Math.max(maxW, img.getWidth());
                totalH += img.getHeight() + PAD;
            }
        }
        int titleH = 46;
        BufferedImage sheet = new BufferedImage(maxW + PAD * 2, totalH + titleH + PAD, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        hints(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setColor(INK);
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString(title, PAD, 32);
        int y = titleH;
        for (BufferedImage img : imgs) {
            g.drawImage(img, PAD, y, null);
            y += img.getHeight() + PAD;
        }
        g.dispose();
        Path out = outDir.resolve("fidelity-overview.png");
        ImageIO.write(sheet, "png", out.toFile());
        return out;
    }

    private static Color scoreColor(Double pct) {
        if (pct == null) {
            return new Color(0x808080);
        }
        if (pct >= 95) {
            return new Color(0x1B7A30);
        }
        if (pct >= 80) {
            return new Color(0x9A7A00);
        }
        if (pct >= 60) {
            return new Color(0xB85C00);
        }
        return new Color(0xB00020);
    }

    private static String prettyTitle(String base) {
        int us = base.indexOf('_');
        if (us < 0) {
            return base;
        }
        return base.substring(0, us) + " -- " + base.substring(us + 1).replace('_', ' ');
    }

    private static BufferedImage readImage(Path p) {
        try {
            if (Files.isRegularFile(p)) {
                return ImageIO.read(p.toFile());
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setComposite(AlphaComposite.SrcOver);
    }

    private static Map<String, Double> loadScores(Path compareJson) {
        Map<String, Double> scores = new LinkedHashMap<>();
        if (compareJson == null || !Files.isRegularFile(compareJson)) {
            return scores;
        }
        try {
            String text = Files.readString(compareJson, StandardCharsets.UTF_8);
            Map<String, Object> data = JsonUtil.asObject(JsonUtil.parse(text));
            for (Object item : JsonUtil.asArray(data.get("results"))) {
                Map<String, Object> r = JsonUtil.asObject(item);
                String test = String.valueOf(r.get("test"));
                Object fid = JsonUtil.asObject(r.get("details")).get("fidelity_percent");
                if (fid instanceof Number n) {
                    scores.put(test, n.doubleValue());
                }
            }
        } catch (IOException ignored) {
        }
        return scores;
    }
}

class JsonUtil {
    private JsonUtil() {}

    public static Object parse(String text) {
        return new Parser(text).parseValue();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String s) {
                    result.put(s, entry.getValue());
                }
            }
            return result;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> asArray(Object value) {
        if (value instanceof List<?> list) {
            return new ArrayList<>((List<Object>) list);
        }
        return new ArrayList<>();
    }

    private static final class Parser {
        private final String text;
        private int index;

        Parser(String text) {
            this.text = text;
        }

        Object parseValue() {
            skipWhitespace();
            if (index >= text.length()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            char ch = text.charAt(index);
            return switch (ch) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            index++;
            Map<String, Object> result = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return result;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                index++;
                result.put(key, parseValue());
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    break;
                }
                expect(',');
                index++;
            }
            return result;
        }

        private List<Object> parseArray() {
            index++;
            List<Object> result = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return result;
            }
            while (true) {
                result.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    break;
                }
                expect(',');
                index++;
            }
            return result;
        }

        private String parseString() {
            expect('"');
            index++;
            StringBuilder sb = new StringBuilder();
            while (index < text.length()) {
                char ch = text.charAt(index++);
                if (ch == '"') {
                    return sb.toString();
                }
                if (ch == '\\') {
                    char esc = text.charAt(index++);
                    sb.append(switch (esc) {
                        case '"' -> '"';
                        case '\\' -> '\\';
                        case '/' -> '/';
                        case 'b' -> '\b';
                        case 'f' -> '\f';
                        case 'n' -> '\n';
                        case 'r' -> '\r';
                        case 't' -> '\t';
                        case 'u' -> parseUnicode();
                        default -> throw new IllegalArgumentException("Invalid escape: " + esc);
                    });
                } else {
                    sb.append(ch);
                }
            }
            throw new IllegalArgumentException("Unterminated string");
        }

        private char parseUnicode() {
            int value = 0;
            for (int i = 0; i < 4; i++) {
                value = (value << 4) | Character.digit(text.charAt(index++), 16);
            }
            return (char) value;
        }

        private Object parseLiteral(String literal, Object value) {
            if (!text.startsWith(literal, index)) {
                throw new IllegalArgumentException("Expected '" + literal + "'");
            }
            index += literal.length();
            return value;
        }

        private Number parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            while (index < text.length() && (Character.isDigit(text.charAt(index)) || ".eE+-".indexOf(text.charAt(index)) >= 0)) {
                index++;
            }
            String number = text.substring(start, index);
            if (number.indexOf('.') >= 0 || number.indexOf('e') >= 0 || number.indexOf('E') >= 0) {
                return Double.parseDouble(number);
            }
            long value = Long.parseLong(number);
            if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                return (int) value;
            }
            return value;
        }

        private void expect(char ch) {
            if (!peek(ch)) {
                throw new IllegalArgumentException("Expected '" + ch + "' at " + index);
            }
        }

        private boolean peek(char ch) {
            return index < text.length() && text.charAt(index) == ch;
        }

        private void skipWhitespace() {
            while (index < text.length()) {
                char ch = text.charAt(index);
                if (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') {
                    index++;
                } else {
                    break;
                }
            }
        }
    }
}
