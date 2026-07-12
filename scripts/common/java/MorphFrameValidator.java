import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/// Deterministic animation-frame validation (the fidelity suite's motion gate).
///
/// The device captures a morphing component FROZEN at fixed progress values
/// ("&lt;id&gt;_t&lt;NNN&gt;_&lt;appearance&gt;_cn1.png", e.g. TabsMorph_t050_light_cn1.png);
/// each capture is a pure function of (theme, progress), so motion regressions
/// are caught even when the start/end screenshots look right. Per (id,
/// appearance) group this tool runs:
///
/// 1. GOLDEN REGRESSION -- every frame is compared 1:1 against its committed
///    golden (goldens-dir/&lt;name&gt;.png, without the _cn1 suffix). Any pixel drift
///    beyond the tolerance fails (exit 22). A missing golden is seeded from the
///    current frame when --seed-missing is set (loud; the seeded files must be
///    committed), otherwise it fails.
/// 2. MOTION PROPERTIES -- pixel-level checks of the motion itself (exit 21):
///    - all declared frames delivered and pairwise DISTINCT during travel (the
///      "morph gets stuck in the middle" bug class renders identical frames);
///    - the selection moved: first and last frames differ meaningfully;
///    - monotonic travel: the rightmost changed pixel (vs the t=0 frame) never
///      moves backwards while progress increases up to the settle window; the
///      final settle may pull back (the spring overshoot) within --overshoot-px.
/// 3. FRAME STRIP -- a labelled horizontal montage per group
///    ("&lt;id&gt;_&lt;appearance&gt;_strip.png" in --strip-dir) so reviewers can see the
///    whole motion at a glance in the PR artifacts.
///
/// The same fixed progress points are pinned numerically against the pure
/// motion model in TabSelectionMorphTest -- this tool closes the loop from the
/// model to the actual painted pixels.
public class MorphFrameValidator {
    private static final int EXIT_USAGE = 2;
    private static final int EXIT_IO = 1;
    private static final int EXIT_PROPERTY = 21;
    private static final int EXIT_GOLDEN = 22;

    /// A pixel differing from the t=0 frame by more than this (max channel
    /// delta) counts as "changed" for the motion-property checks.
    private static final int CHANGE_TAU = 24;
    /// Two frames are "distinct" when at least this fraction of the tile changed.
    private static final double DISTINCT_MIN_FRACTION = 0.001;

    private static final Pattern FRAME_NAME =
            Pattern.compile("^([A-Za-z0-9]+)_t(\\d{3})_([a-z]+)_cn1\\.png$");

    public static void main(String[] args) throws Exception {
        Path framesDir = null;
        Path goldensDir = null;
        Path outJson = null;
        Path stripDir = null;
        Path specPath = null;
        boolean seedMissing = false;
        int maxChannelDelta = 8;
        double maxMismatchPercent = 0.5d;
        int overshootPx = 24;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--frames-dir" -> framesDir = Path.of(args[++i]);
                case "--goldens-dir" -> goldensDir = Path.of(args[++i]);
                case "--out-json" -> outJson = Path.of(args[++i]);
                case "--strip-dir" -> stripDir = Path.of(args[++i]);
                case "--spec" -> specPath = Path.of(args[++i]);
                case "--seed-missing" -> seedMissing = true;
                case "--max-channel-delta" -> maxChannelDelta = Integer.parseInt(args[++i]);
                case "--max-mismatch-percent" -> maxMismatchPercent = Double.parseDouble(args[++i]);
                case "--overshoot-px" -> overshootPx = Integer.parseInt(args[++i]);
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    System.exit(EXIT_USAGE);
                }
            }
        }
        if (framesDir == null || !Files.isDirectory(framesDir)) {
            System.err.println("--frames-dir is required and must exist");
            System.exit(EXIT_USAGE);
            return;
        }
        // The DECLARED frame set per component id (fidelity-tests.yaml `frames:`),
        // so a capture that died mid-sequence cannot validate green on just the
        // frames it happened to deliver.
        Map<String, List<Integer>> declaredFrames = loadDeclaredFrames(specPath);

        // Group delivered frames by (id, appearance), ordered by progress value.
        Map<String, TreeMap<Integer, Path>> groups = new TreeMap<>();
        try (var stream = Files.list(framesDir)) {
            for (Path p : stream.sorted().toList()) {
                Matcher m = FRAME_NAME.matcher(p.getFileName().toString());
                if (m.matches()) {
                    groups.computeIfAbsent(m.group(1) + "_" + m.group(3), k -> new TreeMap<>())
                            .put(Integer.parseInt(m.group(2)), p);
                }
            }
        }
        // Fail-closed on ABSENT groups: the spec declares which components
        // capture frames, so a capture pipeline that silently stops delivering
        // (or drops one appearance) must fail here -- grouping only delivered
        // files would otherwise validate green on whatever survived.
        List<String> missingGroups = new ArrayList<>();
        for (String componentId : declaredFrames.keySet()) {
            for (String appearance : new String[]{"light", "dark"}) {
                String key = componentId + "_" + appearance;
                if (!groups.containsKey(key)) {
                    missingGroups.add(key + ": no frames delivered for a spec-declared frame component");
                }
            }
        }
        if (!missingGroups.isEmpty()) {
            System.err.println("[morph] FAIL: " + missingGroups.size() + " missing frame group(s):");
            for (String m : missingGroups) {
                System.err.println("  - " + m);
            }
            if (outJson != null) {
                Files.writeString(outJson, "{\"groups\":{}}\n", StandardCharsets.UTF_8);
            }
            System.exit(22);
            return;
        }
        if (groups.isEmpty()) {
            System.out.println("[morph] no animation frames found in " + framesDir
                    + " and the spec declares none; nothing to validate");
            if (outJson != null) {
                Files.writeString(outJson, "{\"groups\":{}}\n", StandardCharsets.UTF_8);
            }
            return;
        }

        List<String> propertyFailures = new ArrayList<>();
        List<String> goldenFailures = new ArrayList<>();
        StringBuilder json = new StringBuilder("{\"groups\":{");
        boolean firstGroup = true;
        for (Map.Entry<String, TreeMap<Integer, Path>> group : groups.entrySet()) {
            String name = group.getKey();
            TreeMap<Integer, Path> frames = group.getValue();
            System.out.println("[morph] group " + name + ": " + frames.size() + " frame(s) " + frames.keySet());

            Map<Integer, BufferedImage> images = new TreeMap<>();
            for (Map.Entry<Integer, Path> f : frames.entrySet()) {
                images.put(f.getKey(), ImageIO.read(f.getValue().toFile()));
            }

            // ---- 1. motion properties (validated BEFORE goldens, so a broken
            //         sequence can never seed goldens) ----
            List<String> groupPropertyFailures = new ArrayList<>();
            // Every DECLARED frame must have been delivered: the device runner
            // isolates per-frame failures, so a capture that died mid-sequence
            // would otherwise validate green on the survivors.
            String componentId = name.substring(0, name.lastIndexOf('_'));
            List<Integer> declared = declaredFrames.get(componentId);
            if (declared != null) {
                for (Integer expected : declared) {
                    if (!images.containsKey(expected)) {
                        groupPropertyFailures.add(name + ": declared frame t" + expected
                                + " was not delivered (capture died or the probe failed)");
                    }
                }
            } else if (specPath != null) {
                groupPropertyFailures.add(name + ": component '" + componentId
                        + "' has no frames declaration in the spec");
            }
            List<Integer> ts = new ArrayList<>(images.keySet());
            Map<Integer, Double> changedFraction = new LinkedHashMap<>();
            Map<Integer, Integer> rightmost = new LinkedHashMap<>();
            BufferedImage base = images.get(ts.get(0));
            for (int t : ts) {
                boolean[] changed = changeMask(base, images.get(t));
                int w = Math.min(base.getWidth(), images.get(t).getWidth());
                long count = 0;
                int rx = -1;
                for (int i = 0; i < changed.length; i++) {
                    if (changed[i]) {
                        count++;
                        int x = i % w;
                        if (x > rx) {
                            rx = x;
                        }
                    }
                }
                changedFraction.put(t, (double) count / changed.length);
                rightmost.put(t, rx);
            }
            if (ts.size() < 3) {
                groupPropertyFailures.add(name + ": only " + ts.size() + " frame(s) delivered; need at least 3");
            } else {
                int tFirst = ts.get(0);
                int tLast = ts.get(ts.size() - 1);
                if (changedFraction.get(tLast) < DISTINCT_MIN_FRACTION) {
                    groupPropertyFailures.add(name + ": selection did not move (t" + tFirst + " and t" + tLast
                            + " are visually identical)");
                }
                // EVERY adjacent frame pair must be distinct: a frozen/stuck morph
                // renders the same pixels for different progress values (including
                // one that never starts, so the first pair is checked too).
                for (int i = 0; i < ts.size() - 1; i++) {
                    int a = ts.get(i);
                    int b = ts.get(i + 1);
                    if (mismatchPercent(images.get(a), images.get(b), CHANGE_TAU) < DISTINCT_MIN_FRACTION * 100) {
                        groupPropertyFailures.add(name + ": frames t" + a + " and t" + b
                                + " are identical -- the morph is stuck between these progress values");
                    }
                }
                // Monotonic rightward travel (the morph runs first tab -> last tab).
                // The final settle may pull back: the spring overshoot returns to the
                // target AND the travelling bubble relaxes to its resting width, so
                // the allowance scales with the tile (10% of its width) with
                // --overshoot-px as a floor. A genuinely broken settle (snapping back
                // toward the source tab) is a large fraction of the tile and still fails.
                // The settle window opens at t=86: the spring's overshoot peaks near
                // t~0.87 (travelEnd 0.78 + peak of sin(u*pi)*(1-u)), so pull-back is
                // legitimate physics from there on. 15% of the tile: the measured
                // iOS-26 morph pulls back ~12.6% of the tile at settle (overshoot +
                // bubble-width relaxation at 3x device scale); a broken settle that
                // snaps back toward the source tab is a whole cell (~30%+) and fails.
                int settleAllowancePx = Math.max(overshootPx, base.getWidth() * 15 / 100);
                int maxSeen = -1;
                for (int i = 1; i < ts.size(); i++) {
                    int t = ts.get(i);
                    int rx = rightmost.get(t);
                    boolean settleWindow = t >= 86;
                    if (rx + 2 < maxSeen && !settleWindow) {
                        groupPropertyFailures.add(String.format(
                                "%s: rightmost change moved BACKWARDS at t%d (%dpx after reaching %dpx)",
                                name, t, rx, maxSeen));
                    }
                    if (settleWindow && rx < maxSeen - settleAllowancePx) {
                        groupPropertyFailures.add(String.format(
                                "%s: settle at t%d pulled back %dpx (> settle allowance %dpx)",
                                name, t, maxSeen - rx, settleAllowancePx));
                    }
                    maxSeen = Math.max(maxSeen, rx);
                }
            }
            propertyFailures.addAll(groupPropertyFailures);

            // ---- 2. golden regression (seeding gated on the properties above) ----
            List<String> seeded = new ArrayList<>();
            if (goldensDir != null) {
                Files.createDirectories(goldensDir);
                for (Map.Entry<Integer, Path> f : frames.entrySet()) {
                    String goldenName = f.getValue().getFileName().toString().replace("_cn1.png", ".png");
                    Path golden = goldensDir.resolve(goldenName);
                    if (!Files.isRegularFile(golden)) {
                        if (seedMissing && groupPropertyFailures.isEmpty()) {
                            Files.copy(f.getValue(), golden);
                            seeded.add(goldenName);
                            System.out.println("[morph]   SEEDED golden " + goldenName + " (commit it!)");
                        } else if (seedMissing) {
                            goldenFailures.add(name + " t" + f.getKey()
                                    + ": golden missing and seeding WITHHELD (this sequence failed its motion properties)");
                        } else {
                            goldenFailures.add(name + " t" + f.getKey() + ": golden missing (" + golden + ")");
                        }
                        continue;
                    }
                    BufferedImage g = ImageIO.read(golden.toFile());
                    BufferedImage actual = images.get(f.getKey());
                    // Size is part of the self-golden contract: comparing only the
                    // overlap would hide a tile-size regression entirely.
                    if (g.getWidth() != actual.getWidth() || g.getHeight() != actual.getHeight()) {
                        goldenFailures.add(String.format(
                                "%s t%d: frame size %dx%d differs from golden %dx%d",
                                name, f.getKey(), actual.getWidth(), actual.getHeight(),
                                g.getWidth(), g.getHeight()));
                        continue;
                    }
                    double mismatch = mismatchPercent(g, actual, maxChannelDelta);
                    if (mismatch > maxMismatchPercent) {
                        goldenFailures.add(String.format(
                                "%s t%d: %.3f%% of pixels drifted beyond delta %d (allowed %.3f%%)",
                                name, f.getKey(), mismatch, maxChannelDelta, maxMismatchPercent));
                    }
                }
            }

            // ---- 3. frame strip ----
            if (stripDir != null) {
                Files.createDirectories(stripDir);
                Path strip = stripDir.resolve(name + "_strip.png");
                ImageIO.write(renderStrip(images), "png", strip.toFile());
                System.out.println("[morph]   strip " + strip);
            }

            if (!firstGroup) {
                json.append(',');
            }
            firstGroup = false;
            json.append('"').append(name).append("\":{\"frames\":").append(ts.toString())
                    .append(",\"rightmost_changed_px\":").append(new ArrayList<>(rightmost.values()))
                    .append(",\"changed_fraction\":").append(fractions(changedFraction))
                    .append(",\"seeded_goldens\":").append(seeded.size())
                    .append('}');
        }
        json.append("}}");
        if (outJson != null) {
            Files.writeString(outJson, json + "\n", StandardCharsets.UTF_8);
        }

        boolean failed = false;
        if (!goldenFailures.isEmpty()) {
            System.err.println("[morph] FAIL: " + goldenFailures.size() + " frame golden regression(s):");
            goldenFailures.forEach(f -> System.err.println("  - " + f));
            failed = true;
        }
        if (!propertyFailures.isEmpty()) {
            System.err.println("[morph] FAIL: " + propertyFailures.size() + " motion property violation(s):");
            propertyFailures.forEach(f -> System.err.println("  - " + f));
        }
        if (!propertyFailures.isEmpty()) {
            System.exit(EXIT_PROPERTY);
        }
        if (failed) {
            System.exit(EXIT_GOLDEN);
        }
        System.out.println("[morph] OK: " + groups.size() + " group(s) validated");
    }

    /// Loads the per-component `frames:` declarations from fidelity-tests.yaml
    /// (the same flat subset ProcessScreenshots reads). Returns an empty map when
    /// no spec is given/readable -- delivered-set completeness is then not checked.
    private static Map<String, List<Integer>> loadDeclaredFrames(Path specPath) {
        Map<String, List<Integer>> out = new TreeMap<>();
        if (specPath == null || !Files.isRegularFile(specPath)) {
            if (specPath != null) {
                System.err.println("[morph] WARNING: --spec " + specPath + " not readable; declared-frame check skipped");
            }
            return out;
        }
        try {
            String id = null;
            List<String> lines = new ArrayList<>(Files.readAllLines(specPath));
            lines.add("- id: __end__");
            boolean inComponents = false;
            for (String raw : lines) {
                String line = stripComment(raw).trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.equals("components:")) {
                    inComponents = true;
                    continue;
                }
                if (!inComponents) {
                    continue;
                }
                if (line.startsWith("- ")) {
                    id = null;
                    line = line.substring(2).trim();
                }
                int idx = line.indexOf(':');
                if (idx < 0) {
                    continue;
                }
                String key = line.substring(0, idx).trim();
                String value = unquote(line.substring(idx + 1).trim());
                if ("id".equals(key)) {
                    id = value;
                } else if ("frames".equals(key) && id != null) {
                    List<Integer> vals = new ArrayList<>();
                    for (String part : value.split(",")) {
                        try {
                            vals.add(Integer.parseInt(part.trim()));
                        } catch (NumberFormatException ignored) {
                            // malformed entries are simply not required
                        }
                    }
                    out.put(id, vals);
                }
            }
        } catch (IOException ex) {
            System.err.println("[morph] WARNING: failed to read spec " + specPath + ": " + ex.getMessage());
        }
        return out;
    }

    /// Strips a trailing "#" comment, but only when the "#" is outside quotes and
    /// at the line start or preceded by whitespace (so quoted values keep theirs).
    private static String stripComment(String line) {
        boolean inQuote = false;
        char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuote) {
                if (c == quote) {
                    inQuote = false;
                }
            } else if (c == '"' || c == '\'') {
                inQuote = true;
                quote = c;
            } else if (c == '#' && (i == 0 || Character.isWhitespace(line.charAt(i - 1)))) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                || value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    /// Percentage (0..100) of overlapping pixels whose max channel delta exceeds tau.
    private static double mismatchPercent(BufferedImage a, BufferedImage b, int tau) {
        int w = Math.min(a.getWidth(), b.getWidth());
        int h = Math.min(a.getHeight(), b.getHeight());
        long bad = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (channelDelta(a.getRGB(x, y), b.getRGB(x, y)) > tau) {
                    bad++;
                }
            }
        }
        return 100.0d * bad / ((long) w * h);
    }

    private static boolean[] changeMask(BufferedImage a, BufferedImage b) {
        int w = Math.min(a.getWidth(), b.getWidth());
        int h = Math.min(a.getHeight(), b.getHeight());
        boolean[] m = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                m[y * w + x] = channelDelta(a.getRGB(x, y), b.getRGB(x, y)) > CHANGE_TAU;
            }
        }
        return m;
    }

    private static int channelDelta(int p1, int p2) {
        int dr = Math.abs(((p1 >> 16) & 0xff) - ((p2 >> 16) & 0xff));
        int dg = Math.abs(((p1 >> 8) & 0xff) - ((p2 >> 8) & 0xff));
        int db = Math.abs((p1 & 0xff) - (p2 & 0xff));
        return Math.max(dr, Math.max(dg, db));
    }

    private static String fractions(Map<Integer, Double> values) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (double v : values.values()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append(String.format("%.5f", v));
        }
        return sb.append(']').toString();
    }

    /// Horizontal montage of the frames with a "t=NN%" label bar under each.
    private static BufferedImage renderStrip(Map<Integer, BufferedImage> images) {
        int gap = 4;
        int labelH = 26;
        int w = 0;
        int h = 0;
        for (BufferedImage img : images.values()) {
            w += img.getWidth() + gap;
            h = Math.max(h, img.getHeight());
        }
        BufferedImage strip = new BufferedImage(Math.max(1, w - gap), h + labelH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = strip.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, strip.getWidth(), strip.getHeight());
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        int x = 0;
        for (Map.Entry<Integer, BufferedImage> e : images.entrySet()) {
            g.drawImage(e.getValue(), x, 0, null);
            g.setColor(Color.DARK_GRAY);
            g.drawString("t=" + e.getKey() + "%", x + 4, h + labelH - 8);
            x += e.getValue().getWidth() + gap;
        }
        g.dispose();
        return strip;
    }
}
