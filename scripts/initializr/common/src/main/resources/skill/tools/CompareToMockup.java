import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/// Scores how closely a rendered Codename One screen matches a designer mockup
/// and prints a similarity percentage. This is the deterministic convergence
/// signal an agent needs when building a UI from a mockup: instead of eyeballing
/// the result, run this tool, read the number, adjust the CSS/layout, repeat.
///
/// It reports two complementary scores (both 0..1):
///
///   STRUCTURAL - an SSIM-style perceptual/structural similarity. This is the
///                headline number. It is robust to the small pixel differences
///                that always exist between a live render and a vector mockup
///                (font hinting, anti-aliasing, sub-pixel positioning).
///   PIXEL      - the fraction of pixels whose max ARGB channel delta is <= 3,
///                using the same "same pixel" definition as the framework's
///                TestUtils.imagesWithinTolerance. A stricter, lower number that
///                is useful for spotting exact-match regions.
///
/// The render is auto-resized to the mockup's pixel dimensions before comparing
/// (both depict the same full screen at different scales), so you can capture at
/// any simulator skin size.
///
/// PARTIAL / REGION MODE - mockups frequently include device chrome (a status-bar
/// mock at the top, a home indicator at the bottom, a watermark). Compare only
/// what matters:
///
///   --region X,Y,W,H     compare ONLY this rectangle (mockup pixel coords)
///   --ignore X,Y,W,H     exclude a rectangle; may be repeated
///   --ignore-top N       exclude a band N pixels (or N%) tall from the top edge
///   --ignore-bottom N    exclude a band N pixels (or N%) tall from the bottom edge
///
/// Usage:
///
///     java tools/CompareToMockup.java <render.png> <mockup.png> [options]
///
/// Options:
///
///     --region X,Y,W,H      restrict comparison to this rectangle
///     --ignore X,Y,W,H      exclude this rectangle (repeatable)
///     --ignore-top N[%]     exclude a top band (e.g. a status bar): 96 or 8%
///     --ignore-bottom N[%]  exclude a bottom band (home indicator / tab bar)
///     --resize fit|none     fit (default) scales the render to the mockup size;
///                           none requires the two images to already match
///     --diff <out.png>      write a heatmap diff image (masked areas dimmed)
///     --min <0..1>          fail (exit 1) if the STRUCTURAL score is below this
///     --json                print a single-line JSON result instead of text
///
/// Exit codes:
///
///   0 - compared successfully (and, if --min given, structural score >= min)
///   1 - structural score below --min
///   2 - usage / IO error (bad args, unreadable image, size mismatch with --resize none)
public class CompareToMockup {
    // Mirrors TestUtils.imagesWithinTolerance: a per-channel delta of <= 3 is
    // treated as "the same pixel".
    private static final int TOLERATED_CHANNEL_DELTA = 3;

    // Standard SSIM stabilising constants for an 8-bit dynamic range (L = 255).
    private static final double C1 = (0.01 * 255) * (0.01 * 255);
    private static final double C2 = (0.03 * 255) * (0.03 * 255);
    private static final int SSIM_WINDOW = 8;
    private static final int SSIM_STEP = 4;

    public static void main(String[] args) {
        if (args.length < 2) {
            usage();
            System.exit(2);
        }

        String renderPath = null;
        String mockupPath = null;
        int[] region = null;                       // {x,y,w,h} or null
        List<int[]> ignores = new ArrayList<>();    // list of {x,y,w,h}
        String ignoreTop = null;
        String ignoreBottom = null;
        boolean resizeFit = true;
        String diffPath = null;
        Double min = null;
        boolean json = false;

        try {
            for (int i = 0; i < args.length; i++) {
                String a = args[i];
                switch (a) {
                    case "--region":      region = parseRect(args[++i]); break;
                    case "--ignore":      ignores.add(parseRect(args[++i])); break;
                    case "--ignore-top":  ignoreTop = args[++i]; break;
                    case "--ignore-bottom": ignoreBottom = args[++i]; break;
                    case "--resize":      resizeFit = !"none".equals(args[++i]); break;
                    case "--diff":        diffPath = args[++i]; break;
                    case "--min":         min = Double.valueOf(args[++i]); break;
                    case "--json":        json = true; break;
                    default:
                        if (a.startsWith("--")) {
                            System.err.println("Unknown option: " + a);
                            usage();
                            System.exit(2);
                        } else if (renderPath == null) {
                            renderPath = a;
                        } else if (mockupPath == null) {
                            mockupPath = a;
                        } else {
                            System.err.println("Unexpected argument: " + a);
                            usage();
                            System.exit(2);
                        }
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.err.println("Missing value for the final option.");
            usage();
            System.exit(2);
            return;
        } catch (NumberFormatException ex) {
            System.err.println("Bad numeric argument: " + ex.getMessage());
            System.exit(2);
            return;
        }

        if (renderPath == null || mockupPath == null) {
            usage();
            System.exit(2);
        }

        try {
            BufferedImage render = readImage(renderPath, "render");
            BufferedImage mockup = readImage(mockupPath, "mockup");

            int w = mockup.getWidth();
            int h = mockup.getHeight();

            if (render.getWidth() != w || render.getHeight() != h) {
                if (!resizeFit) {
                    System.err.println("Size mismatch: render is "
                            + render.getWidth() + "x" + render.getHeight()
                            + " but mockup is " + w + "x" + h
                            + " and --resize none was set.");
                    System.exit(2);
                }
                render = scaleTo(render, w, h);
                System.err.println("[CompareToMockup] resized render to mockup dimensions "
                        + w + "x" + h);
            }

            int topBand = ignoreTop == null ? 0 : resolveBand(ignoreTop, h);
            int bottomBand = ignoreBottom == null ? 0 : resolveBand(ignoreBottom, h);

            boolean[] mask = buildMask(w, h, region, ignores, topBand, bottomBand);
            int comparedPixels = 0;
            for (boolean m : mask) {
                if (m) comparedPixels++;
            }
            if (comparedPixels == 0) {
                System.err.println("The mask excludes every pixel - nothing to compare.");
                System.exit(2);
            }

            int[] renderArgb = argb(render);
            int[] mockupArgb = argb(mockup);

            double pixel = pixelScore(renderArgb, mockupArgb, mask);
            double structural = ssimScore(renderArgb, mockupArgb, mask, w, h);

            if (diffPath != null) {
                writeDiff(renderArgb, mockupArgb, mask, w, h, diffPath);
                System.err.println("[CompareToMockup] wrote diff heatmap to " + diffPath);
            }

            String ignoredNote = describeIgnored(region, ignores, topBand, bottomBand);
            if (json) {
                System.out.println("{\"structural\":" + round(structural)
                        + ",\"pixel\":" + round(pixel)
                        + ",\"width\":" + w + ",\"height\":" + h
                        + ",\"comparedPixels\":" + comparedPixels
                        + ",\"totalPixels\":" + (w * h) + "}");
            } else {
                System.out.println("STRUCTURAL " + fmt(structural)
                        + "  PIXEL " + fmt(pixel)
                        + "   (compared " + comparedPixels + " of " + (w * h)
                        + " px at " + w + "x" + h + ignoredNote + ")");
            }

            if (min != null && structural < min) {
                System.err.println("[CompareToMockup] structural score " + fmt(structural)
                        + " is below --min " + min);
                System.exit(1);
            }
        } catch (IOException ex) {
            System.err.println("IO error: " + ex.getMessage());
            System.exit(2);
        }
    }

    private static void usage() {
        System.err.println("Usage: java CompareToMockup.java <render.png> <mockup.png> "
                + "[--region X,Y,W,H] [--ignore X,Y,W,H]... "
                + "[--ignore-top N[%]] [--ignore-bottom N[%]] "
                + "[--resize fit|none] [--diff out.png] [--min 0..1] [--json]");
    }

    private static BufferedImage readImage(String path, String label) throws IOException {
        File f = new File(path);
        if (!f.isFile()) {
            throw new IOException("Cannot read " + label + " image: " + path);
        }
        BufferedImage img = ImageIO.read(f);
        if (img == null) {
            throw new IOException("Unsupported or corrupt " + label + " image: " + path);
        }
        return img;
    }

    private static int[] parseRect(String s) {
        String[] parts = s.split(",");
        if (parts.length != 4) {
            throw new NumberFormatException("expected X,Y,W,H but got '" + s + "'");
        }
        int[] r = new int[4];
        for (int i = 0; i < 4; i++) {
            r[i] = Integer.parseInt(parts[i].trim());
        }
        return r;
    }

    /// Resolves an N or N% band height against the image height.
    private static int resolveBand(String spec, int height) {
        spec = spec.trim();
        if (spec.endsWith("%")) {
            double pct = Double.parseDouble(spec.substring(0, spec.length() - 1).trim());
            return (int) Math.round(height * pct / 100.0);
        }
        return Integer.parseInt(spec);
    }

    private static BufferedImage scaleTo(BufferedImage src, int w, int h) {
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    private static int[] argb(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] out = new int[w * h];
        img.getRGB(0, 0, w, h, out, 0, w);
        return out;
    }

    private static boolean[] buildMask(int w, int h, int[] region, List<int[]> ignores,
                                       int topBand, int bottomBand) {
        boolean[] mask = new boolean[w * h];
        if (region == null) {
            java.util.Arrays.fill(mask, true);
        } else {
            paintRect(mask, w, h, region, true);
        }
        for (int[] ig : ignores) {
            paintRect(mask, w, h, ig, false);
        }
        if (topBand > 0) {
            paintRect(mask, w, h, new int[]{0, 0, w, Math.min(topBand, h)}, false);
        }
        if (bottomBand > 0) {
            int band = Math.min(bottomBand, h);
            paintRect(mask, w, h, new int[]{0, h - band, w, band}, false);
        }
        return mask;
    }

    private static void paintRect(boolean[] mask, int w, int h, int[] r, boolean value) {
        int x0 = Math.max(0, r[0]);
        int y0 = Math.max(0, r[1]);
        int x1 = Math.min(w, r[0] + r[2]);
        int y1 = Math.min(h, r[1] + r[3]);
        for (int y = y0; y < y1; y++) {
            int row = y * w;
            for (int x = x0; x < x1; x++) {
                mask[row + x] = value;
            }
        }
    }

    /// Fraction of masked pixels whose maximum ARGB channel delta is within
    /// TOLERATED_CHANNEL_DELTA. Same "same pixel" rule as the framework.
    private static double pixelScore(int[] a, int[] b, boolean[] mask) {
        long same = 0;
        long total = 0;
        for (int i = 0; i < a.length; i++) {
            if (!mask[i]) continue;
            total++;
            int pa = a[i];
            int pb = b[i];
            int dA = Math.abs(((pa >> 24) & 0xff) - ((pb >> 24) & 0xff));
            int dR = Math.abs(((pa >> 16) & 0xff) - ((pb >> 16) & 0xff));
            int dG = Math.abs(((pa >> 8) & 0xff) - ((pb >> 8) & 0xff));
            int dB = Math.abs((pa & 0xff) - (pb & 0xff));
            int max = Math.max(Math.max(dA, dR), Math.max(dG, dB));
            if (max <= TOLERATED_CHANNEL_DELTA) {
                same++;
            }
        }
        return total == 0 ? 0.0 : same / (double) total;
    }

    /// Mean SSIM over sliding windows whose pixels are all inside the mask.
    /// Returns the mean clamped to [0,1].
    private static double ssimScore(int[] a, int[] b, boolean[] mask, int w, int h) {
        double[] lumA = luminance(a);
        double[] lumB = luminance(b);

        double sum = 0.0;
        long windows = 0;

        for (int y = 0; y + SSIM_WINDOW <= h; y += SSIM_STEP) {
            for (int x = 0; x + SSIM_WINDOW <= w; x += SSIM_STEP) {
                if (!windowFullyMasked(mask, w, x, y)) continue;

                double sumA = 0, sumB = 0, sumAA = 0, sumBB = 0, sumAB = 0;
                int n = SSIM_WINDOW * SSIM_WINDOW;
                for (int wy = 0; wy < SSIM_WINDOW; wy++) {
                    int row = (y + wy) * w + x;
                    for (int wx = 0; wx < SSIM_WINDOW; wx++) {
                        double va = lumA[row + wx];
                        double vb = lumB[row + wx];
                        sumA += va;
                        sumB += vb;
                        sumAA += va * va;
                        sumBB += vb * vb;
                        sumAB += va * vb;
                    }
                }
                double muA = sumA / n;
                double muB = sumB / n;
                double varA = sumAA / n - muA * muA;
                double varB = sumBB / n - muB * muB;
                double cov = sumAB / n - muA * muB;

                double ssim = ((2 * muA * muB + C1) * (2 * cov + C2))
                        / ((muA * muA + muB * muB + C1) * (varA + varB + C2));
                sum += ssim;
                windows++;
            }
        }
        if (windows == 0) {
            // Mask too small for a full window: fall back to a global single-window
            // estimate over the masked pixels so we still return a meaningful score.
            return globalSsim(lumA, lumB, mask);
        }
        double mean = sum / windows;
        return Math.max(0.0, Math.min(1.0, mean));
    }

    private static double globalSsim(double[] a, double[] b, boolean[] mask) {
        double sumA = 0, sumB = 0, sumAA = 0, sumBB = 0, sumAB = 0;
        long n = 0;
        for (int i = 0; i < a.length; i++) {
            if (!mask[i]) continue;
            double va = a[i];
            double vb = b[i];
            sumA += va;
            sumB += vb;
            sumAA += va * va;
            sumBB += vb * vb;
            sumAB += va * vb;
            n++;
        }
        if (n == 0) return 0.0;
        double muA = sumA / n;
        double muB = sumB / n;
        double varA = sumAA / n - muA * muA;
        double varB = sumBB / n - muB * muB;
        double cov = sumAB / n - muA * muB;
        double ssim = ((2 * muA * muB + C1) * (2 * cov + C2))
                / ((muA * muA + muB * muB + C1) * (varA + varB + C2));
        return Math.max(0.0, Math.min(1.0, ssim));
    }

    private static boolean windowFullyMasked(boolean[] mask, int w, int x, int y) {
        for (int wy = 0; wy < SSIM_WINDOW; wy++) {
            int row = (y + wy) * w + x;
            for (int wx = 0; wx < SSIM_WINDOW; wx++) {
                if (!mask[row + wx]) return false;
            }
        }
        return true;
    }

    private static double[] luminance(int[] argb) {
        double[] lum = new double[argb.length];
        for (int i = 0; i < argb.length; i++) {
            int p = argb[i];
            int r = (p >> 16) & 0xff;
            int g = (p >> 8) & 0xff;
            int b = p & 0xff;
            lum[i] = 0.299 * r + 0.587 * g + 0.114 * b;
        }
        return lum;
    }

    /// Writes a heatmap: green=match, yellow/red=large luminance delta. Masked-out
    /// pixels are shown as a dimmed grayscale of the mockup for orientation.
    private static void writeDiff(int[] render, int[] mockup, boolean[] mask,
                                  int w, int h, String path) throws IOException {
        BufferedImage diff = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int[] out = new int[w * h];
        for (int i = 0; i < out.length; i++) {
            if (!mask[i]) {
                int p = mockup[i];
                int gray = (int) (0.299 * ((p >> 16) & 0xff)
                        + 0.587 * ((p >> 8) & 0xff)
                        + 0.114 * (p & 0xff));
                gray = gray / 3 + 40; // dim
                out[i] = (gray << 16) | (gray << 8) | gray;
            } else {
                int pa = render[i];
                int pb = mockup[i];
                double la = 0.299 * ((pa >> 16) & 0xff) + 0.587 * ((pa >> 8) & 0xff) + 0.114 * (pa & 0xff);
                double lb = 0.299 * ((pb >> 16) & 0xff) + 0.587 * ((pb >> 8) & 0xff) + 0.114 * (pb & 0xff);
                double d = Math.abs(la - lb) / 255.0; // 0..1
                out[i] = heat(d);
            }
        }
        diff.setRGB(0, 0, w, h, out, 0, w);
        String fmt = path.toLowerCase().endsWith(".jpg") || path.toLowerCase().endsWith(".jpeg")
                ? "jpg" : "png";
        ImageIO.write(diff, fmt, new File(path));
    }

    /// Maps 0..1 onto green -> yellow -> red.
    private static int heat(double d) {
        d = Math.max(0.0, Math.min(1.0, d));
        int r, g;
        if (d < 0.5) {
            r = (int) (510 * d);   // 0 -> 255 across first half
            g = 255;
        } else {
            r = 255;
            g = (int) (510 * (1.0 - d)); // 255 -> 0 across second half
        }
        return (r << 16) | (g << 8);
    }

    private static String describeIgnored(int[] region, List<int[]> ignores,
                                          int topBand, int bottomBand) {
        StringBuilder sb = new StringBuilder();
        if (region != null) {
            sb.append(", region ").append(region[0]).append(',').append(region[1])
              .append(',').append(region[2]).append(',').append(region[3]);
        }
        if (topBand > 0) sb.append(", ignored top ").append(topBand).append("px");
        if (bottomBand > 0) sb.append(", ignored bottom ").append(bottomBand).append("px");
        if (!ignores.isEmpty()) sb.append(", ").append(ignores.size()).append(" ignore rect(s)");
        return sb.toString();
    }

    private static String fmt(double v) {
        return String.format(java.util.Locale.US, "%.3f", v);
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
