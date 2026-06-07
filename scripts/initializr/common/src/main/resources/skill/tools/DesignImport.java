import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/// Parses a designer file (Figma, Sketch, or Adobe XD) - or an HTML/React design's
/// CSS design tokens (tokens.css / styles.css, e.g. a Claude-generated mockup) -
/// into a STARTER Codename One style so an agent building a screen from a mockup
/// starts near the target instead of from scratch. It emits three files into the
/// output directory:
///
///   theme.css   - seeded CN1 CSS (Form/Title/Label/Button + per-named-layer UIIDs)
///   tokens.json - the extracted palette, type scale and spacing scale
///   layout.md   - the component tree with frames, text and a suggested CN1 layout
///
/// The output is a STARTING POINT, extracted mechanically. Refine it by hand and
/// measure how close you are with tools/CompareToMockup.java. Validate the emitted
/// CSS with tools/IsCssValid.java before wiring it in.
///
/// This tool is fully self-contained: it includes a small JSON parser and uses
/// only the JDK (java.net.http for the Figma REST call). It does NOT need the CN1
/// jars in ~/.m2.
///
/// Usage:
///
///   # Local ZIP+JSON formats
///   java tools/DesignImport.java path/to/design.sketch [--out DIR] [--px-per-mm N]
///   java tools/DesignImport.java path/to/design.xd     [--out DIR] [--px-per-mm N]
///
///   # HTML/React design tokens (a tokens.css/styles.css, or a dir containing one)
///   java tools/DesignImport.java path/to/tokens.css    [--out DIR] [--px-per-mm N]
///   java tools/DesignImport.java path/to/design-dir/   [--out DIR] [--px-per-mm N]
///
///   # Figma over its REST API (needs a personal access token + the file key)
///   java tools/DesignImport.java figma --token <PAT> --file <FILE_KEY> \
///        [--node <NODE_ID>] [--out DIR] [--px-per-mm N]
///
/// Options:
///
///   --out DIR        output directory (default: cn1-design-import)
///   --px-per-mm N    pixels-per-millimetre used to convert sizes to CN1 mm units
///                    (default 11.8, roughly a 3x retina mockup). Tune to taste.
///   --token TOKEN    Figma personal access token (figma mode only)
///   --file KEY       Figma file key from the file URL (figma mode only)
///   --node ID        restrict to a single Figma node/frame id (figma mode only)
///
/// Exit codes:
///
///   0 - imported successfully
///   1 - parsed but produced nothing useful (no recognisable layers)
///   2 - usage / IO / network error
public class DesignImport {
    public static void main(String[] args) {
        if (args.length < 1) {
            usage();
            System.exit(2);
        }

        String source = args[0];
        String out = "cn1-design-import";
        double pxPerMm = 11.8;
        boolean pxPerMmSet = false;
        String token = null;
        String fileKey = null;
        String nodeId = null;

        try {
            for (int i = 1; i < args.length; i++) {
                switch (args[i]) {
                    case "--out":       out = args[++i]; break;
                    case "--px-per-mm": pxPerMm = Double.parseDouble(args[++i]); pxPerMmSet = true; break;
                    case "--token":     token = args[++i]; break;
                    case "--file":      fileKey = args[++i]; break;
                    case "--node":      nodeId = args[++i]; break;
                    default:
                        System.err.println("Unknown option: " + args[i]);
                        usage();
                        System.exit(2);
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.err.println("Missing value for the final option.");
            usage();
            System.exit(2);
        }

        try {
            Node root;
            String label;
            String lower = source.toLowerCase(Locale.US);

            // CSS design-token mode: a tokens.css / styles.css (or a directory
            // containing one) produced by an HTML/React design such as the ones
            // Claude generates. We extract the CSS custom properties instead of a
            // layer tree, so there is no Node graph - emit straight from here.
            Path cssTokens = cssTokenSource(source, lower);
            if (cssTokens != null) {
                if (!pxPerMmSet) {
                    pxPerMm = 3.78; // CSS px are 1x logical px (~3.78 px/mm)
                }
                Extracted ex = parseCssTokens(cssTokens);
                if (ex.nodeCount == 0) {
                    System.err.println("No CSS custom properties (--name: value) found in " + cssTokens + ".");
                    System.exit(1);
                }
                Path outDir = Paths.get(out);
                Files.createDirectories(outDir);
                Files.writeString(outDir.resolve("tokens.json"), renderTokens(ex));
                Files.writeString(outDir.resolve("theme.css"), renderCss(ex, pxPerMm, "CSS tokens " + cssTokens));
                Files.writeString(outDir.resolve("layout.md"), renderCssLayout(cssTokens));
                System.out.println("IMPORTED " + ex.colorCounts.size() + " colors, "
                        + ex.fontSizes.size() + " text sizes, " + ex.spacings.size()
                        + " spacings -> " + outDir.toAbsolutePath());
                System.err.println("[DesignImport] next: validate CSS with "
                        + "`java tools/IsCssValid.java " + outDir.resolve("theme.css") + "`, "
                        + "then iterate with tools/CompareToMockup.java");
                return;
            }

            if (source.equals("figma")) {
                if (token == null || fileKey == null) {
                    System.err.println("figma mode requires --token and --file");
                    System.exit(2);
                }
                root = parseFigma(token, fileKey, nodeId);
                label = "Figma file " + fileKey;
            } else if (lower.endsWith(".sketch")) {
                root = parseSketch(Paths.get(source));
                label = "Sketch file " + source;
            } else if (lower.endsWith(".xd")) {
                root = parseXd(Paths.get(source));
                label = "Adobe XD file " + source;
            } else {
                System.err.println("Unrecognised source. Expected a .sketch / .xd / .css file, "
                        + "a directory with a tokens.css/styles.css, or the literal 'figma'.");
                usage();
                System.exit(2);
                return;
            }

            Extracted ex = new Extracted();
            collect(root, ex);

            if (ex.nodeCount == 0) {
                System.err.println("No recognisable layers were found in " + label + ".");
                System.exit(1);
            }

            Path outDir = Paths.get(out);
            Files.createDirectories(outDir);
            Files.writeString(outDir.resolve("tokens.json"), renderTokens(ex));
            Files.writeString(outDir.resolve("theme.css"), renderCss(ex, pxPerMm, label));
            Files.writeString(outDir.resolve("layout.md"), renderLayout(root, ex, pxPerMm, label));

            System.out.println("IMPORTED " + ex.nodeCount + " layers, "
                    + ex.colorCounts.size() + " colors, "
                    + ex.fontSizes.size() + " text sizes -> " + outDir.toAbsolutePath());
            System.err.println("[DesignImport] next: validate CSS with "
                    + "`java tools/IsCssValid.java " + outDir.resolve("theme.css") + "`, "
                    + "then iterate with tools/CompareToMockup.java");
        } catch (IOException | InterruptedException ex) {
            System.err.println("Error: " + ex.getMessage());
            System.exit(2);
        }
    }

    private static void usage() {
        System.err.println("Usage:");
        System.err.println("  java DesignImport.java design.sketch [--out DIR] [--px-per-mm N]");
        System.err.println("  java DesignImport.java design.xd     [--out DIR] [--px-per-mm N]");
        System.err.println("  java DesignImport.java figma --token PAT --file KEY [--node ID] [--out DIR]");
        System.err.println("  java DesignImport.java tokens.css    [--out DIR] [--px-per-mm N]   (HTML/React design tokens)");
        System.err.println("  java DesignImport.java path/to/design-dir/                          (dir containing tokens.css)");
    }

    // ===================================================================
    // CSS design-token mode (HTML / React designs, e.g. Claude output)
    // ===================================================================

    /// Resolves a CSS-token source: a .css file, or a directory that contains a
    /// tokens.css / styles.css. Returns null when the source is not CSS-based.
    private static Path cssTokenSource(String source, String lower) {
        Path p = Paths.get(source);
        if (lower.endsWith(".css") && Files.isRegularFile(p)) {
            return p;
        }
        if (Files.isDirectory(p)) {
            for (String candidate : new String[]{"tokens.css", "styles.css", "theme.css"}) {
                Path c = p.resolve(candidate);
                if (Files.isRegularFile(c)) {
                    return c;
                }
            }
        }
        return null;
    }

    /// Extracts CSS custom properties (`--name: value;`) from a design's
    /// tokens/styles CSS into the same Extracted model used by the layer importers.
    /// Only the FIRST value seen for each name is kept, so the light theme (declared
    /// first under :root) wins over a later dark-theme override. Colors come from
    /// --color-* (or any hex/rgb value), sizes from --fs-*/--font-size, spacing from
    /// --space-*/--gap, radii from --radius-*.
    private static Extracted parseCssTokens(Path file) throws IOException {
        String css = Files.readString(file);
        Map<String, String> vars = new LinkedHashMap<>();
        Matcher m = Pattern.compile("--([A-Za-z0-9-]+)\\s*:\\s*([^;}]+)").matcher(css);
        while (m.find()) {
            String name = m.group(1).trim().toLowerCase(Locale.US);
            String value = m.group(2).trim();
            if (!vars.containsKey(name)) {
                vars.put(name, value);
            }
        }

        Extracted ex = new Extracted();
        for (Map.Entry<String, String> e : vars.entrySet()) {
            String name = e.getKey();
            String value = e.getValue();
            Integer color = parseCssColor(value);
            boolean isBg = name.contains("bg") || name.contains("background")
                    || name.contains("surface") || name.contains("panel");
            boolean isAccent = name.contains("accent") || name.contains("primary");
            boolean isColor = color != null && (name.startsWith("color") || isBg || isAccent
                    || name.contains("border") || name.contains("brand") || name.contains("text"));
            if (isColor) {
                // Force the dominant background to rank first and the accent second,
                // so renderCss reads get(0) as the surface and get(1) as the accent.
                // (Plain frequency fails: white is reused across many surface tokens.)
                int weight;
                if (isAccent) {
                    weight = 10000;
                } else if (isBg && ex.dominantBackground == null) {
                    weight = 20000;
                    ex.dominantBackground = color;
                } else {
                    weight = 1;
                }
                ex.colorCounts.merge(hex(color), weight, Integer::sum);
                ex.nodeCount++;
            } else if (name.startsWith("fs-") || name.contains("font-size") || name.startsWith("text-")) {
                Double px = parsePx(value);
                if (px != null && px > 0 && !ex.fontSizes.contains(px)) {
                    ex.fontSizes.add(px);
                    ex.nodeCount++;
                }
            } else if (name.startsWith("space") || name.equals("gap") || name.startsWith("radius")) {
                Double px = parsePx(value);
                if (px != null && px > 0 && !ex.spacings.contains(px)) {
                    ex.spacings.add(px);
                    ex.nodeCount++;
                }
            }
        }
        Collections.sort(ex.fontSizes);
        Collections.sort(ex.spacings);
        return ex;
    }

    /// Parses a CSS color literal (#rgb, #rrggbb, rgb(), rgba()) to 0xRRGGBB, or
    /// null for non-literal values (var(), color-mix(), named colors, transparent).
    private static Integer parseCssColor(String raw) {
        String v = raw.trim().toLowerCase(Locale.US);
        if (v.startsWith("#")) {
            String h = v.substring(1);
            try {
                if (h.length() == 3) {
                    int r = Integer.parseInt(h.substring(0, 1), 16);
                    int g = Integer.parseInt(h.substring(1, 2), 16);
                    int b = Integer.parseInt(h.substring(2, 3), 16);
                    return (r * 17 << 16) | (g * 17 << 8) | (b * 17);
                }
                if (h.length() == 6 || h.length() == 8) {
                    return (int) (Long.parseLong(h.substring(0, 6), 16) & 0xFFFFFF);
                }
            } catch (NumberFormatException ignored) {
                return null;
            }
            return null;
        }
        Matcher rgb = Pattern.compile("rgba?\\(\\s*([0-9]+)[,\\s]+([0-9]+)[,\\s]+([0-9]+)").matcher(v);
        if (rgb.find()) {
            return (clampByte(Double.parseDouble(rgb.group(1))) << 16)
                    | (clampByte(Double.parseDouble(rgb.group(2))) << 8)
                    | clampByte(Double.parseDouble(rgb.group(3)));
        }
        return null;
    }

    /// Parses a leading px length ("14px", "28px") to its numeric value, or null.
    private static Double parsePx(String raw) {
        Matcher m = Pattern.compile("(-?[0-9]+(?:\\.[0-9]+)?)\\s*px").matcher(raw.trim());
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return null;
    }

    private static String renderCssLayout(Path file) {
        return "# Layout map - CSS design tokens\n\n"
                + "Source: `" + file + "`\n\n"
                + "This import came from a CSS-token design (an HTML/React mockup), so there is no\n"
                + "layer tree to map. Use the generated `theme.css` as a *palette + type-scale*\n"
                + "starting point and build the layout from the design's own HTML/JSX structure:\n\n"
                + "| Design CSS idiom            | Codename One equivalent                         |\n"
                + "| --------------------------- | ----------------------------------------------- |\n"
                + "| `display:flex; flex-direction:row` | `Container` with `BoxLayout.x()` / `FlowLayout` |\n"
                + "| `display:flex; flex-direction:column` | `Container` with `BoxLayout.y()`           |\n"
                + "| `display:grid` (N columns)  | `Container` with `GridLayout(rows, N)`          |\n"
                + "| header / body / footer      | `BorderLayout` NORTH / CENTER / SOUTH           |\n"
                + "| a `--color-*` variable      | a hardcoded hex per UIID (CN1 CSS has no vars)  |\n"
                + "| `:root` light / `[data-theme=dark]` | a `Foo` UIID + a parallel `FooDark` UIID |\n\n"
                + "To capture the design itself as a comparison mockup, render its HTML headlessly\n"
                + "(e.g. Playwright) to a PNG, then score with `tools/CompareToMockup.java`.\n";
    }

    // ===================================================================
    // Intermediate model
    // ===================================================================

    private static final class Node {
        String name = "";
        String type = "";
        double x, y, w, h;
        boolean hasFrame;
        Integer fill;           // 0xRRGGBB or null
        Integer stroke;
        double cornerRadius;
        String text;            // text content if this is a text layer
        String fontFamily;
        Double fontSize;        // px
        String layoutMode;      // VERTICAL / HORIZONTAL / null
        double gap;
        final List<Node> children = new ArrayList<>();
    }

    private static final class Extracted {
        int nodeCount;
        final Map<String, Integer> colorCounts = new LinkedHashMap<>();
        final List<Double> fontSizes = new ArrayList<>();
        final List<Double> spacings = new ArrayList<>();
        Integer dominantBackground;
        final List<Node> namedComponents = new ArrayList<>();
    }

    private static void collect(Node n, Extracted ex) {
        ex.nodeCount++;
        if (n.fill != null) {
            String hex = hex(n.fill);
            ex.colorCounts.merge(hex, 1, Integer::sum);
        }
        if (n.stroke != null) {
            ex.colorCounts.merge(hex(n.stroke), 1, Integer::sum);
        }
        if (n.fontSize != null && n.fontSize > 0 && !ex.fontSizes.contains(n.fontSize)) {
            ex.fontSizes.add(n.fontSize);
        }
        if (n.gap > 0 && !ex.spacings.contains(n.gap)) {
            ex.spacings.add(n.gap);
        }
        // A large filled rectangle that contains other layers is a likely background.
        if (n.fill != null && n.hasFrame && n.w * n.h > 100000 && !n.children.isEmpty()) {
            ex.dominantBackground = n.fill;
        }
        if (isComponentName(n.name) && (n.fill != null || n.text != null)) {
            ex.namedComponents.add(n);
        }
        for (Node c : n.children) {
            collect(c, ex);
        }
        Collections.sort(ex.fontSizes);
        Collections.sort(ex.spacings);
    }

    private static boolean isComponentName(String name) {
        if (name == null || name.isEmpty()) return false;
        // Skip Sketch/Figma symbol-path names and auto-generated names.
        if (name.contains("/")) return false;
        String l = name.toLowerCase(Locale.US);
        return !l.equals("rectangle") && !l.equals("group") && !l.equals("frame")
                && !l.startsWith("vector") && !l.startsWith("ellipse") && !l.startsWith("line");
    }

    // ===================================================================
    // Figma
    // ===================================================================

    @SuppressWarnings("unchecked")
    private static Node parseFigma(String token, String fileKey, String nodeId)
            throws IOException, InterruptedException {
        String url = nodeId == null
                ? "https://api.figma.com/v1/files/" + fileKey
                : "https://api.figma.com/v1/files/" + fileKey + "/nodes?ids=" + enc(nodeId);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("X-Figma-Token", token)
                .GET()
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("Figma API returned HTTP " + resp.statusCode()
                    + ": " + truncate(resp.body(), 300));
        }
        Object json = Json.parse(resp.body());
        Map<String, Object> top = asMap(json);
        Object document;
        if (nodeId == null) {
            document = top.get("document");
        } else {
            Map<String, Object> nodes = asMap(top.get("nodes"));
            Map<String, Object> first = nodes.isEmpty() ? null
                    : asMap(nodes.values().iterator().next());
            document = first == null ? null : first.get("document");
        }
        Node root = new Node();
        root.name = stringOr(top.get("name"), "Figma");
        root.type = "DOCUMENT";
        if (document != null) {
            root.children.add(figmaNode(asMap(document)));
        }
        return root;
    }

    @SuppressWarnings("unchecked")
    private static Node figmaNode(Map<String, Object> m) {
        Node n = new Node();
        n.name = stringOr(m.get("name"), "");
        n.type = stringOr(m.get("type"), "");

        Map<String, Object> box = asMap(m.get("absoluteBoundingBox"));
        if (!box.isEmpty()) {
            n.x = num(box.get("x"));
            n.y = num(box.get("y"));
            n.w = num(box.get("width"));
            n.h = num(box.get("height"));
            n.hasFrame = true;
        }
        n.cornerRadius = num(m.get("cornerRadius"));

        List<Object> fills = asList(m.get("fills"));
        for (Object f : fills) {
            Map<String, Object> fill = asMap(f);
            if ("SOLID".equals(fill.get("type")) && enabled(fill)) {
                n.fill = figmaColor(asMap(fill.get("color")));
                break;
            }
        }
        List<Object> strokes = asList(m.get("strokes"));
        for (Object s : strokes) {
            Map<String, Object> st = asMap(s);
            if ("SOLID".equals(st.get("type"))) {
                n.stroke = figmaColor(asMap(st.get("color")));
                break;
            }
        }

        if ("TEXT".equals(n.type)) {
            n.text = stringOr(m.get("characters"), null);
            Map<String, Object> style = asMap(m.get("style"));
            n.fontFamily = stringOr(style.get("fontFamily"), null);
            double fs = num(style.get("fontSize"));
            if (fs > 0) n.fontSize = fs;
        }

        String lm = stringOr(m.get("layoutMode"), null);
        if ("VERTICAL".equals(lm) || "HORIZONTAL".equals(lm)) {
            n.layoutMode = lm;
            n.gap = num(m.get("itemSpacing"));
        }

        for (Object c : asList(m.get("children"))) {
            n.children.add(figmaNode(asMap(c)));
        }
        return n;
    }

    private static boolean enabled(Map<String, Object> fill) {
        Object v = fill.get("visible");
        return !(v instanceof Boolean) || (Boolean) v;
    }

    private static Integer figmaColor(Map<String, Object> c) {
        if (c.isEmpty()) return null;
        return rgb(num(c.get("r")), num(c.get("g")), num(c.get("b")), true);
    }

    // ===================================================================
    // Sketch (.sketch is a ZIP of JSON)
    // ===================================================================

    private static Node parseSketch(Path file) throws IOException {
        Node root = new Node();
        root.name = "Sketch";
        root.type = "DOCUMENT";
        try (ZipFile zip = new ZipFile(file.toFile())) {
            var entries = zip.entries();
            List<String> pageEntries = new ArrayList<>();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.getName().startsWith("pages/") && e.getName().endsWith(".json")) {
                    pageEntries.add(e.getName());
                }
            }
            Collections.sort(pageEntries);
            for (String name : pageEntries) {
                ZipEntry e = zip.getEntry(name);
                try (InputStream in = zip.getInputStream(e)) {
                    Object json = Json.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    root.children.add(sketchLayer(asMap(json)));
                }
            }
        }
        return root;
    }

    private static Node sketchLayer(Map<String, Object> m) {
        Node n = new Node();
        n.name = stringOr(m.get("name"), "");
        n.type = stringOr(m.get("_class"), "");

        Map<String, Object> frame = asMap(m.get("frame"));
        if (!frame.isEmpty()) {
            n.x = num(frame.get("x"));
            n.y = num(frame.get("y"));
            n.w = num(frame.get("width"));
            n.h = num(frame.get("height"));
            n.hasFrame = true;
        }
        n.cornerRadius = num(m.get("fixedRadius"));

        Map<String, Object> style = asMap(m.get("style"));
        for (Object f : asList(style.get("fills"))) {
            Map<String, Object> fill = asMap(f);
            Object on = fill.get("isEnabled");
            if (on instanceof Boolean && !((Boolean) on)) continue;
            n.fill = sketchColor(asMap(fill.get("color")));
            if (n.fill != null) break;
        }
        for (Object b : asList(style.get("borders"))) {
            Map<String, Object> border = asMap(b);
            n.stroke = sketchColor(asMap(border.get("color")));
            if (n.stroke != null) break;
        }

        if ("text".equals(n.type)) {
            Map<String, Object> attr = asMap(m.get("attributedString"));
            n.text = stringOr(attr.get("string"), null);
            Map<String, Object> textStyle = asMap(asMap(style.get("textStyle"))
                    .getOrDefault("encodedAttributes", Collections.emptyMap()));
            double fs = num(asMap(textStyle.get("MSAttributedStringFontAttribute")).get("size"));
            if (fs == 0) {
                // Newer Sketch nests the size under attributes.font / a number directly.
                fs = num(textStyle.get("size"));
            }
            if (fs > 0) n.fontSize = fs;
        }

        for (Object c : asList(m.get("layers"))) {
            n.children.add(sketchLayer(asMap(c)));
        }
        return n;
    }

    private static Integer sketchColor(Map<String, Object> c) {
        if (c.isEmpty()) return null;
        return rgb(num(c.get("red")), num(c.get("green")), num(c.get("blue")), true);
    }

    // ===================================================================
    // Adobe XD (.xd is a ZIP; artboards live in graphicContent.agc JSON)
    // ===================================================================

    private static Node parseXd(Path file) throws IOException {
        Node root = new Node();
        root.name = "AdobeXD";
        root.type = "DOCUMENT";
        try (ZipFile zip = new ZipFile(file.toFile())) {
            var entries = zip.entries();
            List<String> agc = new ArrayList<>();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                if (e.getName().endsWith("graphicContent.agc")) {
                    agc.add(e.getName());
                }
            }
            Collections.sort(agc);
            for (String name : agc) {
                ZipEntry e = zip.getEntry(name);
                try (InputStream in = zip.getInputStream(e)) {
                    Object json = Json.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    Map<String, Object> m = asMap(json);
                    Map<String, Object> artboard = asMap(m.get("artboard"));
                    Object children = artboard.isEmpty() ? m.get("children") : artboard.get("children");
                    Node board = new Node();
                    board.name = name;
                    board.type = "artboard";
                    for (Object c : asList(children)) {
                        board.children.add(xdNode(asMap(c)));
                    }
                    root.children.add(board);
                }
            }
        }
        return root;
    }

    private static Node xdNode(Map<String, Object> m) {
        Node n = new Node();
        n.name = stringOr(m.get("name"), "");
        n.type = stringOr(m.get("type"), "");

        Map<String, Object> transform = asMap(m.get("transform"));
        Map<String, Object> shape = asMap(m.get("shape"));
        if (!shape.isEmpty()) {
            n.w = num(shape.get("width"));
            n.h = num(shape.get("height"));
            n.x = num(transform.get("tx"));
            n.y = num(transform.get("ty"));
            n.hasFrame = n.w > 0 || n.h > 0;
            n.cornerRadius = num(shape.get("r"));
        }

        Map<String, Object> style = asMap(m.get("style"));
        Map<String, Object> fill = asMap(style.get("fill"));
        if (!fill.isEmpty()) {
            n.fill = xdColor(asMap(fill.get("color")));
        }
        Map<String, Object> stroke = asMap(style.get("stroke"));
        if (!stroke.isEmpty()) {
            n.stroke = xdColor(asMap(stroke.get("color")));
        }

        Map<String, Object> text = asMap(m.get("text"));
        if (!text.isEmpty()) {
            n.text = stringOr(text.get("rawText"), null);
            Map<String, Object> font = asMap(asMap(style.get("font")).isEmpty()
                    ? text.get("font") : style.get("font"));
            double fs = num(font.get("size"));
            if (fs > 0) n.fontSize = fs;
            n.fontFamily = stringOr(font.get("postscriptName"), stringOr(font.get("family"), null));
        }

        for (Object c : asList(m.get("children"))) {
            n.children.add(xdNode(asMap(c)));
        }
        return n;
    }

    private static Integer xdColor(Map<String, Object> c) {
        if (c.isEmpty()) return null;
        Map<String, Object> v = asMap(c.get("value"));
        if (v.isEmpty()) return null;
        return rgb(num(v.get("r")), num(v.get("g")), num(v.get("b")), false);
    }

    // ===================================================================
    // Output rendering
    // ===================================================================

    private static String renderTokens(Extracted ex) {
        List<Map.Entry<String, Integer>> colors = new ArrayList<>(ex.colorCounts.entrySet());
        colors.sort((a, b) -> b.getValue() - a.getValue());
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"colors\": [\n");
        for (int i = 0; i < colors.size(); i++) {
            Map.Entry<String, Integer> e = colors.get(i);
            sb.append("    {\"hex\": \"").append(e.getKey())
              .append("\", \"count\": ").append(e.getValue()).append("}");
            sb.append(i < colors.size() - 1 ? ",\n" : "\n");
        }
        sb.append("  ],\n  \"fontSizesPx\": ").append(numberList(ex.fontSizes)).append(",\n");
        sb.append("  \"spacingPx\": ").append(numberList(ex.spacings)).append("\n}\n");
        return sb.toString();
    }

    private static String renderCss(Extracted ex, double pxPerMm, String label) {
        List<Map.Entry<String, Integer>> colors = new ArrayList<>(ex.colorCounts.entrySet());
        colors.sort((a, b) -> b.getValue() - a.getValue());
        String bg = ex.dominantBackground != null ? hex(ex.dominantBackground)
                : (colors.isEmpty() ? "#ffffff" : colors.get(0).getKey());
        String accent = colors.size() > 1 ? colors.get(1).getKey()
                : (colors.isEmpty() ? "#1d4ed8" : colors.get(0).getKey());
        String onAccent = contrastColor(accent);
        double bodyPx = ex.fontSizes.isEmpty() ? 0 : median(ex.fontSizes);
        double titlePx = ex.fontSizes.isEmpty() ? 0 : ex.fontSizes.get(ex.fontSizes.size() - 1);

        StringBuilder sb = new StringBuilder();
        sb.append("/*\n * STARTER theme.css generated by tools/DesignImport.java from ")
          .append(label).append(".\n")
          .append(" * Colors are extracted by frequency; sizes converted at ")
          .append(trim(pxPerMm)).append(" px/mm.\n")
          .append(" * This is a starting point - refine it, validate with tools/IsCssValid.java,\n")
          .append(" * and measure convergence with tools/CompareToMockup.java.\n */\n\n");

        sb.append("Form {\n    background-color: ").append(bg).append(";\n}\n\n");

        sb.append("Title {\n    color: ").append(contrastColor(bg)).append(";\n");
        if (titlePx > 0) sb.append("    font-size: ").append(mm(titlePx, pxPerMm)).append(";\n");
        sb.append("}\n\n");

        sb.append("Label {\n    color: ").append(contrastColor(bg)).append(";\n");
        if (bodyPx > 0) sb.append("    font-size: ").append(mm(bodyPx, pxPerMm)).append(";\n");
        sb.append("}\n\n");

        sb.append("Button {\n    background-color: ").append(accent).append(";\n")
          .append("    color: ").append(onAccent).append(";\n")
          .append("    border-radius: 2mm;\n")
          .append("    padding: 2mm 4mm;\n");
        if (bodyPx > 0) sb.append("    font-size: ").append(mm(bodyPx, pxPerMm)).append(";\n");
        sb.append("}\n");

        int emitted = 0;
        for (Node c : ex.namedComponents) {
            if (emitted >= 24) break; // keep the starter readable
            String uiid = toUiid(c.name);
            if (uiid.isEmpty()) continue;
            sb.append('\n').append(uiid).append(" {\n");
            if (c.fill != null) {
                sb.append("    background-color: ").append(hex(c.fill)).append(";\n");
                sb.append("    color: ").append(contrastColor(c.fill)).append(";\n");
            }
            if (c.cornerRadius > 0) {
                sb.append("    border-radius: ").append(mm(c.cornerRadius, pxPerMm)).append(";\n");
            }
            if (c.fontSize != null && c.fontSize > 0) {
                sb.append("    font-size: ").append(mm(c.fontSize, pxPerMm)).append(";\n");
            }
            sb.append("}\n");
            emitted++;
        }
        return sb.toString();
    }

    private static String renderLayout(Node root, Extracted ex, double pxPerMm, String label) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Layout map - ").append(label).append("\n\n");
        sb.append("Generated by tools/DesignImport.java. Use this as the structure to build, then ")
          .append("measure with tools/CompareToMockup.java.\n\n");
        sb.append("Sizes are shown as `WxH px` (design pixels). Convert to CN1 `mm` at ")
          .append(trim(pxPerMm)).append(" px/mm.\n\n");
        sb.append("## Component tree\n\n```\n");
        for (Node c : root.children) {
            printTree(c, 0, sb);
        }
        sb.append("```\n\n");

        sb.append("## Suggested CN1 layouts\n\n");
        sb.append("| Design container layout | Codename One equivalent |\n");
        sb.append("| --- | --- |\n");
        sb.append("| Vertical auto-layout / stack | `BoxLayout.y()` |\n");
        sb.append("| Horizontal auto-layout / row | `BoxLayout.x()` |\n");
        sb.append("| Absolute / free positioning | `LayeredLayout` with percentage insets |\n");
        sb.append("| Header / body / footer split | `BorderLayout` (NORTH/CENTER/SOUTH) |\n\n");

        List<String> texts = new ArrayList<>();
        collectText(root, texts);
        if (!texts.isEmpty()) {
            sb.append("## Text strings\n\n");
            for (String t : texts) {
                sb.append("- ").append(t.replace("\n", " ")).append('\n');
            }
        }
        return sb.toString();
    }

    private static void printTree(Node n, int depth, StringBuilder sb) {
        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append(n.name.isEmpty() ? "(" + n.type + ")" : n.name);
        if (!n.type.isEmpty()) sb.append(" [").append(n.type).append(']');
        if (n.hasFrame) {
            sb.append("  ").append(Math.round(n.w)).append('x').append(Math.round(n.h)).append("px");
        }
        if (n.fill != null) sb.append("  fill=").append(hex(n.fill));
        if (n.layoutMode != null) sb.append("  layout=").append(n.layoutMode);
        if (n.text != null) sb.append("  text=\"").append(truncate(n.text.replace("\n", " "), 40)).append('"');
        sb.append('\n');
        for (Node c : n.children) {
            printTree(c, depth + 1, sb);
        }
    }

    private static void collectText(Node n, List<String> out) {
        if (n.text != null && !n.text.isBlank()) out.add(n.text);
        for (Node c : n.children) collectText(c, out);
    }

    // ===================================================================
    // Helpers
    // ===================================================================

    /// Builds a 0xRRGGBB int from channel values. floatScale=true means the
    /// inputs are 0..1 (Figma/Sketch); false means they may already be 0..255 (XD),
    /// although XD sometimes uses 0..1 too, so we detect.
    private static Integer rgb(double r, double g, double b, boolean floatScale) {
        boolean looksFloat = floatScale || (r <= 1.0 && g <= 1.0 && b <= 1.0);
        int ri = clampByte(looksFloat ? r * 255.0 : r);
        int gi = clampByte(looksFloat ? g * 255.0 : g);
        int bi = clampByte(looksFloat ? b * 255.0 : b);
        return (ri << 16) | (gi << 8) | bi;
    }

    private static int clampByte(double v) {
        return (int) Math.round(Math.max(0, Math.min(255, v)));
    }

    private static String hex(int rgb) {
        return String.format("#%06x", rgb & 0xffffff);
    }

    /// Chooses black or white text for readability against a background color.
    private static String contrastColor(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        double lum = 0.299 * r + 0.587 * g + 0.114 * b;
        return lum > 140 ? "#000000" : "#ffffff";
    }

    private static String contrastColor(String hex) {
        return contrastColor(Integer.parseInt(hex.substring(1), 16));
    }

    private static String mm(double px, double pxPerMm) {
        double v = px / pxPerMm;
        return trim(Math.round(v * 10.0) / 10.0) + "mm /* " + trim(px) + "px */";
    }

    private static String toUiid(String name) {
        StringBuilder sb = new StringBuilder();
        boolean upper = true;
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                sb.append(upper ? Character.toUpperCase(ch) : ch);
                upper = false;
            } else {
                upper = true;
            }
        }
        String s = sb.toString();
        if (!s.isEmpty() && Character.isDigit(s.charAt(0))) s = "C" + s;
        return s;
    }

    private static double median(List<Double> sorted) {
        if (sorted.isEmpty()) return 0;
        return sorted.get(sorted.size() / 2);
    }

    private static String numberList(List<Double> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            sb.append(trim(values.get(i)));
            if (i < values.size() - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }

    private static String trim(double v) {
        if (v == Math.rint(v)) return Long.toString((long) v);
        return String.valueOf(v);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private static String enc(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object o) {
        return o instanceof List ? (List<Object>) o : Collections.emptyList();
    }

    private static double num(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0.0;
    }

    private static String stringOr(Object o, String dflt) {
        return o instanceof String ? (String) o : dflt;
    }

    // ===================================================================
    // Minimal dependency-free JSON parser
    // ===================================================================

    private static final class Json {
        private final String s;
        private int i;

        private Json(String s) { this.s = s; }

        static Object parse(String text) throws IOException {
            Json j = new Json(text);
            j.ws();
            Object v = j.value();
            j.ws();
            if (j.i < j.s.length()) {
                throw new IOException("Trailing JSON content at offset " + j.i);
            }
            return v;
        }

        private Object value() throws IOException {
            char c = peek();
            switch (c) {
                case '{': return object();
                case '[': return array();
                case '"': return string();
                case 't': case 'f': return bool();
                case 'n': literal("null"); return null;
                default:  return number();
            }
        }

        private Map<String, Object> object() throws IOException {
            Map<String, Object> m = new LinkedHashMap<>();
            expect('{');
            ws();
            if (peek() == '}') { i++; return m; }
            while (true) {
                ws();
                String key = string();
                ws();
                expect(':');
                ws();
                m.put(key, value());
                ws();
                char c = next();
                if (c == '}') break;
                if (c != ',') throw err("expected , or } in object");
            }
            return m;
        }

        private List<Object> array() throws IOException {
            List<Object> list = new ArrayList<>();
            expect('[');
            ws();
            if (peek() == ']') { i++; return list; }
            while (true) {
                ws();
                list.add(value());
                ws();
                char c = next();
                if (c == ']') break;
                if (c != ',') throw err("expected , or ] in array");
            }
            return list;
        }

        private String string() throws IOException {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') break;
                if (c == '\\') {
                    char e = next();
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                            break;
                        default: throw err("bad escape \\" + e);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Object number() throws IOException {
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            String n = s.substring(start, i);
            if (n.isEmpty()) throw err("unexpected character '" + peek() + "'");
            return Double.parseDouble(n);
        }

        private Boolean bool() throws IOException {
            if (peek() == 't') { literal("true"); return Boolean.TRUE; }
            literal("false");
            return Boolean.FALSE;
        }

        private void literal(String lit) throws IOException {
            if (!s.startsWith(lit, i)) throw err("expected '" + lit + "'");
            i += lit.length();
        }

        private void ws() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\t' || c == '\n' || c == '\r') i++;
                else break;
            }
        }

        private char peek() throws IOException {
            if (i >= s.length()) throw err("unexpected end of JSON");
            return s.charAt(i);
        }

        private char next() throws IOException {
            if (i >= s.length()) throw err("unexpected end of JSON");
            return s.charAt(i++);
        }

        private void expect(char c) throws IOException {
            if (next() != c) throw err("expected '" + c + "'");
        }

        private IOException err(String msg) {
            return new IOException("JSON parse error at offset " + i + ": " + msg);
        }
    }
}
