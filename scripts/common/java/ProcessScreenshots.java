import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

public class ProcessScreenshots {
    private static final int MAX_COMMENT_BASE64 = 60_000;
    private static final int[] JPEG_QUALITY_CANDIDATES = {70, 60, 50, 40, 30, 20, 10};
    private static final byte[] PNG_SIGNATURE = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final int MAX_RETRIES = 5;
    private static final long RETRY_DELAY_MS = 2000;

    public static void main(String[] args) throws Exception {
        Arguments arguments = Arguments.parse(args);
        if (arguments == null) {
            System.exit(2);
            return;
        }
        Map<String, Object> payload;
        if ("fidelity".equals(arguments.mode)) {
            // Fidelity mode compares the CN1 render of a component against the
            // committed native widget golden of the SAME name. There is no
            // pass/fail equality here -- it emits a similarity score per pair so
            // the report/gate can track how close CN1 is to the real OS widget.
            payload = buildFidelityResults(
                    arguments.referenceDir,
                    arguments.actualEntries,
                    arguments.emitBase64,
                    arguments.previewDir,
                    arguments.backdrop,
                    arguments.spec,
                    arguments.specPlatform
            );
        } else {
            payload = buildResults(
                    arguments.referenceDir,
                    arguments.actualEntries,
                    arguments.emitBase64,
                    arguments.previewDir,
                    arguments.maxChannelDelta,
                    arguments.maxMismatchPercent
            );
        }
        String json = JsonUtil.stringify(payload);
        System.out.print(json);
    }

    static Map<String, Object> buildResults(
            Path referenceDir,
            List<Map.Entry<String, Path>> actualEntries,
            boolean emitBase64,
            Path previewDir,
            int maxChannelDelta,
            double maxMismatchPercent
    ) throws IOException {
        List<Map<String, Object>> results = new ArrayList<>();
        java.util.Set<String> deliveredTests = new java.util.LinkedHashSet<>();
        for (Map.Entry<String, Path> entry : actualEntries) {
            String testName = entry.getKey();
            deliveredTests.add(testName);
            Path actualPath = entry.getValue();
            Path expectedPath = referenceDir.resolve(testName + ".png");
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("test", testName);
            record.put("actual_path", actualPath.toString());
            record.put("expected_path", expectedPath.toString());
            if (!Files.exists(actualPath)) {
                record.put("status", "missing_actual");
                record.put("message", "Actual screenshot not found");
            } else if (!Files.exists(expectedPath)) {
                record.put("status", "missing_expected");
                if (emitBase64) {
                    try {
                        CommentPayload payload = loadPreviewOrBuild(testName, actualPath, previewDir);
                        recordPayload(record, payload, actualPath.getFileName().toString(), previewDir);
                    } catch (Exception ex) {
                        record.put("message", "Failed to load preview: " + ex.getMessage());
                    }
                }
            } else {
                try {
                    PNGImage actual = loadPngWithRetry(actualPath);
                    PNGImage expected = loadPngWithRetry(expectedPath);
                    // Per-test tolerance override: an optional "<test>.tolerance"
                    // file next to the reference raises the allowed pixel variance
                    // for inherently non-deterministic captures (e.g. the GPU 3D
                    // tests, whose software-renderer output differs by ~1% between
                    // CI runners). Deterministic 2D tests keep the tight defaults.
                    double[] tol = readTolerance(referenceDir, testName, maxChannelDelta, maxMismatchPercent);
                    Map<String, Object> outcome = compareImages(expected, actual, (int) tol[0], tol[1]);
                    if (Boolean.TRUE.equals(outcome.get("equal"))) {
                        record.put("status", "equal");
                    } else {
                        record.put("status", "different");
                        record.put("details", outcome);
                        if (emitBase64) {
                            CommentPayload payload = loadPreviewOrBuild(testName, actualPath, previewDir, actual);
                            recordPayload(record, payload, actualPath.getFileName().toString(), previewDir);
                        }
                    }
                } catch (Exception ex) {
                    record.put("status", "error");
                    record.put("message", ex.getMessage());
                }
            }
            results.add(record);
        }
        // The reference directory is the manifest of every screenshot a healthy
        // run MUST produce. The loop above only sees screenshots the suite
        // actually delivered, so a golden whose actual was never delivered -- the
        // signature of a suite that hung or crashed partway -- would otherwise
        // leave no record at all, and the report/comment would describe a clean
        // "N matched" pass over just the survivors while the suite was in fact
        // incomplete. Walk the goldens that no delivered actual covered and
        // record each as missing_actual so the comparison JSON, the rendered
        // summary, the PR comment and the shell-level count-regression guard all
        // agree on the same reality. (See scripts/lib/cn1ss.sh cn1ss_count_*.)
        if (referenceDir != null && Files.isDirectory(referenceDir)) {
            List<String> missingTests = new ArrayList<>();
            try (java.util.stream.Stream<Path> goldens = Files.list(referenceDir)) {
                goldens.filter(Files::isRegularFile)
                        .map(p -> p.getFileName().toString())
                        .filter(n -> n.endsWith(".png"))
                        .map(n -> n.substring(0, n.length() - ".png".length()))
                        .filter(name -> !deliveredTests.contains(name))
                        .forEach(missingTests::add);
            }
            Collections.sort(missingTests);
            for (String testName : missingTests) {
                Path expectedPath = referenceDir.resolve(testName + ".png");
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("test", testName);
                record.put("actual_path", "");
                record.put("expected_path", expectedPath.toString());
                record.put("status", "missing_actual");
                record.put("message", "No screenshot was delivered for this golden (the test did not run, hung, or the suite crashed before reaching it).");
                results.add(record);
            }
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("results", results);
        return payload;
    }

    /// Fidelity comparison: each delivered actual is the CN1 render of a
    /// component (named "<prefix>", path "<prefix>_cn1.png"); the reference dir
    /// holds the committed NATIVE widget golden "<prefix>.png". For every pair we
    /// emit a similarity score (fidelity_percent + ssim + mean_channel_delta)
    /// rather than an equal/different verdict, and we embed previews of BOTH the
    /// native golden and the CN1 render so the report can show them side by side.
    /// Goldens with no delivered CN1 render are recorded as missing_actual so the
    /// pair-count guard (cn1ss.sh cn1ss_count_covered) sees the same reality.
    static Map<String, Object> buildFidelityResults(
            Path referenceDir,
            List<Map.Entry<String, Path>> actualEntries,
            boolean emitBase64,
            Path previewDir,
            Path backdropPath,
            Path specPath,
            String specPlatform
    ) throws IOException {
        List<Map<String, Object>> results = new ArrayList<>();
        // Glass tiles (Toolbar/Tabs/Buttons over iOS Liquid Glass) are composited
        // over a shared gradient backdrop. Comparing the whole tile lets the
        // identical-ish backdrop (~70% of the pixels) inflate the score regardless
        // of how wrong the widget is. Load the backdrop so we can mask it out and
        // score ONLY the widget (see structuralFidelityGlass).
        BufferedImage backdropImg = loadBackdrop(backdropPath, referenceDir);
        // Comparison mode comes from the per-test `material:` declared in
        // fidelity-tests.yaml (test INTENT), not from image-content heuristics.
        // A missing spec (or a test the spec does not know) falls back to the
        // legacy corner heuristic so old artifact sets can still be re-scored.
        Map<String, String> materialByComponent = loadMaterialMap(specPath, referenceDir, specPlatform);
        java.util.Set<String> deliveredTests = new java.util.LinkedHashSet<>();
        for (Map.Entry<String, Path> entry : actualEntries) {
            String testName = entry.getKey();
            deliveredTests.add(testName);
            Path cn1Path = entry.getValue();
            Path nativePath = referenceDir.resolve(testName + ".png");
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("test", testName);
            record.put("cn1_path", cn1Path.toString());
            record.put("native_path", nativePath.toString());
            // Keep actual_path/expected_path mirrors so the shared cn1ss.sh
            // artifact-copy/summary plumbing can treat these records uniformly.
            record.put("actual_path", cn1Path.toString());
            record.put("expected_path", nativePath.toString());
            if (!Files.exists(cn1Path)) {
                record.put("status", "missing_actual");
                record.put("message", "CN1 render was not delivered for this component.");
            } else if (!Files.exists(nativePath)) {
                record.put("status", "missing_expected");
                record.put("message", "Native golden missing at " + nativePath
                        + " (regenerate goldens with FIDELITY_UPDATE_GOLDENS=1).");
                if (emitBase64) {
                    try {
                        recordFidelityImage(record, "", "_cn1", testName, loadPngWithRetry(cn1Path), previewDir);
                    } catch (Exception ex) {
                        record.put("message", "Failed to load CN1 preview: " + ex.getMessage());
                    }
                }
            } else {
                try {
                    PNGImage cn1 = loadPngWithRetry(cn1Path);
                    PNGImage nativeImage = loadPngWithRetry(nativePath);
                    Map<String, Object> details = new LinkedHashMap<>();
                    details.put("width", cn1.width());
                    details.put("height", cn1.height());
                    // Both renders anchor the widget at the tile's TOP-LEFT, so when
                    // the two tiles differ in size -- a few px of cross-environment
                    // rounding (iOS goldens are produced offline by a separate native
                    // app), or a larger native-vs-CN1 tile-geometry divergence that can
                    // flake between emulator runs -- we crop BOTH to their common
                    // top-left region and overlay 1:1 (never a scale). The structural
                    // metric then crops to each widget's content bbox, so an honest
                    // size/extent difference still shows up as a lower score rather than
                    // an un-scorable "size mismatch" that breaks the gate. Only a
                    // degenerate render (a near-empty common region) is treated as an
                    // error.
                    int cw = Math.min(cn1.width(), nativeImage.width());
                    int ch = Math.min(cn1.height(), nativeImage.height());
                    if (cw < 8 || ch < 8) {
                        details.put("native_width", nativeImage.width());
                        details.put("native_height", nativeImage.height());
                        details.put("fidelity_percent", 0.0d);
                        details.put("ssim", 0.0d);
                        details.put("mean_channel_delta", 255.0d);
                        record.put("status", "size_mismatch");
                        record.put("message", "CN1 tile " + cn1.width() + "x" + cn1.height()
                                + " has no usable overlap with native golden "
                                + nativeImage.width() + "x" + nativeImage.height() + ".");
                    } else {
                        PNGImage cn1c = cropTopLeft(cn1, cw, ch);
                        PNGImage natc = cropTopLeft(nativeImage, cw, ch);
                        details.put("native_width", nativeImage.width());
                        details.put("native_height", nativeImage.height());
                        // Structure-aware fidelity: see structuralFidelity(). The
                        // background colour is known from the appearance (the tile
                        // backdrop we render), so a near-white CN1 fill still counts
                        // as widget content rather than being mistaken for blank bg.
                        int bg = testName.contains("_dark") ? 0x000000 : 0xffffff;
                        double[] sf;
                        boolean glass = false;
                        String material = resolveMaterial(materialByComponent, testName);
                        String materialSource = material != null ? "spec" : "heuristic";
                        if (material != null) {
                            // Mode from declared test intent (review: material tag).
                            boolean wantsMask = "glass".equals(material) || "lens".equals(material);
                            if (wantsMask && backdropImg != null) {
                                int[] refArr = stretchCropTopLeft(backdropImg, cn1.width(), cn1.height(), cw, ch);
                                glass = true;
                                sf = structuralFidelityGlass(natc, cn1c, refArr);
                            } else {
                                if (wantsMask) {
                                    details.put("material_note",
                                            "material=" + material + " but no backdrop reference was found; scored whole-tile");
                                }
                                sf = structuralFidelity(natc, cn1c, bg);
                            }
                        } else if (backdropImg != null) {
                            // Legacy fallback: infer glass from the golden's corners
                            // matching the stretched shared backdrop (scaleToFill,
                            // ignore aspect), sampled at the cropped top-left region.
                            int[] refArr = stretchCropTopLeft(backdropImg, cn1.width(), cn1.height(), cw, ch);
                            if (isGlassTile(natc, refArr, cw, ch)) {
                                glass = true;
                                sf = structuralFidelityGlass(natc, cn1c, refArr);
                            } else {
                                sf = structuralFidelity(natc, cn1c, bg);
                            }
                        } else {
                            sf = structuralFidelity(natc, cn1c, bg);
                        }
                        double meanDelta = meanChannelDelta(natc, cn1c);
                        details.put("fidelity_percent", round2(sf[0]));
                        details.put("shape_sim", round4(sf[1]));
                        details.put("size_agreement", round4(sf[2]));
                        details.put("glass", glass);
                        details.put("material", material != null ? material : (glass ? "glass" : "normal"));
                        details.put("material_source", materialSource);
                        details.put("ssim", round4(computeSsim(natc, cn1c)));
                        details.put("mean_channel_delta", round2(meanDelta));
                        record.put("status", "compared");
                    }
                    record.put("details", details);
                    if (emitBase64) {
                        recordFidelityImage(record, "", "_cn1", testName, cn1, previewDir);
                        recordFidelityImage(record, "native_", "_native", testName, nativeImage, previewDir);
                    }
                } catch (Exception ex) {
                    record.put("status", "error");
                    record.put("message", ex.getMessage());
                }
            }
            results.add(record);
        }
        // Backfill goldens that no delivered CN1 render covered as missing_actual,
        // mirroring buildResults() so a suite that hung or crashed partway is
        // visible to the shell-level pair-count guard rather than silently
        // reporting a clean pass over only the survivors.
        if (referenceDir != null && Files.isDirectory(referenceDir)) {
            List<String> missingTests = new ArrayList<>();
            try (java.util.stream.Stream<Path> goldens = Files.list(referenceDir)) {
                goldens.filter(Files::isRegularFile)
                        .map(p -> p.getFileName().toString())
                        .filter(n -> n.endsWith(".png"))
                        .map(n -> n.substring(0, n.length() - ".png".length()))
                        .filter(name -> !deliveredTests.contains(name))
                        .forEach(missingTests::add);
            }
            Collections.sort(missingTests);
            for (String testName : missingTests) {
                Path nativePath = referenceDir.resolve(testName + ".png");
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("test", testName);
                record.put("cn1_path", "");
                record.put("native_path", nativePath.toString());
                record.put("actual_path", "");
                record.put("expected_path", nativePath.toString());
                record.put("status", "missing_actual");
                record.put("message", "No CN1 render was delivered for this native golden (the test did not run, hung, or the suite crashed before reaching it).");
                results.add(record);
            }
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("results", results);
        return payload;
    }

    /// Builds a JPEG/PNG preview of one fidelity image and records its base64 +
    /// preview-file metadata under a key prefix ("" for the CN1 render,
    /// "native_" for the native golden) so a single result can carry both. The
    /// file suffix ("_cn1"/"_native") keeps the two preview files distinct.
    private static void recordFidelityImage(Map<String, Object> record, String keyPrefix, String fileSuffix,
            String testName, PNGImage image, Path previewDir) throws IOException {
        CommentPayload payload = buildCommentPayload(image, MAX_COMMENT_BASE64);
        if (payload.base64 != null) {
            record.put(keyPrefix + "base64", payload.base64);
        } else {
            record.put(keyPrefix + "base64_omitted", payload.omittedReason);
            record.put(keyPrefix + "base64_length", payload.base64Length);
        }
        record.put(keyPrefix + "base64_mime", payload.mime);
        record.put(keyPrefix + "base64_codec", payload.codec);
        if (payload.quality != null) {
            record.put(keyPrefix + "base64_quality", payload.quality);
        }
        if (payload.note != null) {
            record.put(keyPrefix + "base64_note", payload.note);
        }
        if (previewDir != null && payload.data != null) {
            Files.createDirectories(previewDir);
            String suffix = payload.mime.equals("image/jpeg") ? ".jpg" : ".png";
            String baseName = slugify(testName) + fileSuffix;
            Path previewPath = previewDir.resolve(baseName + suffix);
            Files.write(previewPath, payload.data);
            Map<String, Object> preview = new LinkedHashMap<>();
            preview.put("path", previewPath.toString());
            preview.put("name", previewPath.getFileName().toString());
            preview.put("mime", payload.mime);
            preview.put("codec", payload.codec);
            if (payload.quality != null) {
                preview.put("quality", payload.quality);
            }
            if (payload.note != null) {
                preview.put("note", payload.note);
            }
            record.put(keyPrefix + "preview", preview);
        }
    }

    /// Structure-aware fidelity. Returns {fidelity_percent, coverage_iou}.
    ///
    /// A plain per-pixel colour delta over a tile that is mostly background
    /// rewards matching empty space and is blind to structure -- two widgets that
    /// look nothing alike (a small outlined field vs a full-width filled one) can
    /// score 95% because both tiles are mostly near-white. Instead we score only
    /// the WIDGET, defined as pixels that differ from the known tile background by
    /// more than a small threshold:
    ///   * a pixel that is widget in BOTH images contributes its colour match
    ///     (1 - normalized channel delta),
    ///   * a pixel that is widget in only ONE image contributes 0 (one has the
    ///     widget there, the other does not),
    ///   * background-in-both pixels are ignored entirely.
    /// The score is the mean over the union of widget pixels, so extent, position,
    /// shape AND colour all matter, and empty margins never inflate it. The
    /// coverage IoU (widget-pixel overlap) is returned alongside as a diagnostic.
    /// Returns {fidelity_percent, shape_sim, size_agreement}.
    ///
    /// The two widgets are compared at 1:1 PIXEL SCALE with NO resampling. Each is
    /// cropped to its content bounding box (removing the surrounding tile padding,
    /// which differs between the native and CN1 layouts) and the two crops are then
    /// overlaid top-left -- the same corner the harness anchors both tiles to. Every
    /// pixel that is content in at least one crop is compared; where only one crop
    /// has a pixel (because the widgets are different sizes) the missing side is
    /// treated as background, so a size or shape difference shows up as a real
    /// colour mismatch rather than being scaled away. We deliberately do NOT
    /// normalize the two widgets to a common canvas size -- a CN1 button rendered
    /// 1.5x Material's size is a genuine fidelity gap and must score lower.
    ///   * shape_sim     -- 1 - mean channel delta over the overlaid content pixels.
    ///   * size_agreement -- ratio of the content bounding-box dimensions, reported
    ///     as a diagnostic (the 1:1 overlay already accounts for size, so it is not
    ///     multiplied back in).
    /// fidelity = 100 * shape_sim. Identical -> 100; same widget styled slightly
    /// differently or off by a few pixels -> high 90s; a genuinely different or
    /// mis-sized widget -> lower, in proportion to the mismatched area.
    private static final int CONTENT_TAU = 10;
    private static final int MIN_CONTENT_PIXELS = 4;

    private static double[] structuralFidelity(PNGImage nativeImg, PNGImage cn1, int bgRgb) {
        BufferedImage bn = toRgbImage(nativeImg);
        BufferedImage bc = toRgbImage(cn1);
        int[] boxN = contentBBox(bn, bgRgb);
        int[] boxC = contentBBox(bc, bgRgb);
        boolean emptyN = boxN[2] <= 0;
        boolean emptyC = boxC[2] <= 0;
        if (emptyN && emptyC) {
            return new double[]{100.0d, 1.0d, 1.0d};   // both blank -> trivially identical
        }
        if (emptyN || emptyC) {
            return new double[]{0.0d, 0.0d, 0.0d};      // one has a widget, the other does not
        }
        // Compare at ABSOLUTE pixel position -- NO content-crop, NO whole-widget
        // re-alignment. The widget's margin from the tile edges is part of
        // fidelity: a control rendered 15px too high is 15px of mismatch, not
        // silently slid into place. Only a small per-pixel window absorbs genuine
        // sub-pixel anti-aliasing between the two render paths.
        //
        // We score TWO independent terms and take the WORSE:
        //   fillSim   -- mean colour agreement over the widget-content pixels. A
        //                large flat region that happens to match (e.g. a dark nav-bar
        //                fill against a dark tile) makes this high on its own, which
        //                is exactly why it cannot be the whole story.
        //   structSim -- the same colour agreement but WEIGHTED BY STRUCTURAL
        //                SALIENCE (local gradient), so flat fills count for almost
        //                nothing and the actual distinguishing content -- glyph
        //                strokes, widget edges, separators -- dominates. A title that
        //                is centred in one render and left-aligned in the other has
        //                its strokes land on the other render's flat fill, so this
        //                term collapses even though the fills match.
        // fidelity = min(fillSim, structSim): a widget must agree in BOTH its fill
        // AND its structure/placement, so "same background, totally different
        // widget" can no longer score high.
        // Headline = geometric mean of TWO complementary, well-understood terms:
        //   fillSim -- mean colour agreement over the widget-content pixels (catches
        //              a wrong fill/glyph colour: a black checkbox vs a blue one).
        //   ssim    -- windowed structural similarity, robust to the few-pixel
        //              sub-pixel offsets two render paths inevitably produce (so a
        //              title that sits 8px lower in one render is still recognised
        //              as the same title, not scored to zero).
        // sqrt(fillSim * ssim) demands BOTH be high: a genuinely-similar widget
        // (Toolbar: fill .96, ssim .95 -> 95) reads honestly, a real defect stays
        // low because at least one term collapses (a broken dark field with ssim
        // ~0 -> ~0; a wrong glyph colour drags fillSim). The earlier
        // min(fillSim, squared-salience structSim) is kept ONLY as a diagnostic:
        // its structural term could not tell "same widget, text a few px off" from
        // "different widget" and so crushed genuinely-faithful renders.
        double fillSim = absoluteShapeSim(bn, bc, bgRgb);
        double structSim = structuralSalienceSim(bn, bc, bgRgb);   // diagnostic only
        double ssim = computeSsim(nativeImg, cn1);
        double headline = Math.sqrt(Math.max(0.0d, fillSim) * Math.max(0.0d, ssim));
        double fidelity = 100.0d * headline;
        return new double[]{fidelity, structSim, fillSim};
    }

    /// A pixel further than this (mean channel delta) from the backdrop reference
    /// is treated as WIDGET; closer pixels are the shared glass backdrop and are
    /// excluded from the glass fidelity score. Generous enough to absorb the
    /// minor render/AA differences in the gradient between the two stacks.
    private static final int GLASS_TAU = 26;

    /// Load the glass backdrop PNG (the gradient that the iOS Liquid-Glass tiles
    /// composite behind the widget). Explicit --backdrop wins; otherwise fall back
    /// to the canonical location relative to the goldens dir. Returns null when no
    /// backdrop is found, in which case fidelity scoring stays whole-tile.
    private static BufferedImage loadBackdrop(Path explicit, Path referenceDir) {
        Path[] candidates = {
            explicit,
            referenceDir == null ? null
                : referenceDir.resolve("../../common/src/main/resources/glass-backdrop.png").normalize()
        };
        for (Path p : candidates) {
            if (p == null) {
                continue;
            }
            try {
                if (Files.isRegularFile(p)) {
                    BufferedImage img = javax.imageio.ImageIO.read(p.toFile());
                    if (img != null) {
                        return img;
                    }
                }
            } catch (IOException ignore) {
                // try the next candidate
            }
        }
        return null;
    }

    /// Reproduce a BACKGROUND scaleToFill (stretch to the full tile, ignoring aspect
    /// -- matching the iOS native ref's .scaleToFill and the CN1 SCALED background
    /// type), sampled over the top-left (cw x ch) crop the comparator overlays.
    /// Nearest-neighbour is sufficient: the backdrop is a smooth gradient and this
    /// is only used to classify backdrop-vs-widget pixels.
    private static int[] stretchCropTopLeft(BufferedImage bd, int fullW, int fullH, int cw, int ch) {
        int bw = bd.getWidth(), bh = bd.getHeight();
        int[] out = new int[cw * ch];
        for (int y = 0; y < ch; y++) {
            int sy = fullH <= 0 ? 0 : (int) ((long) y * bh / fullH);
            if (sy >= bh) {
                sy = bh - 1;
            }
            for (int x = 0; x < cw; x++) {
                int sx = fullW <= 0 ? 0 : (int) ((long) x * bw / fullW);
                if (sx >= bw) {
                    sx = bw - 1;
                }
                out[y * cw + x] = bd.getRGB(sx, sy) & 0xffffff;
            }
        }
        return out;
    }

    private static int colorDist(int p, int q) {
        return (Math.abs(((p >> 16) & 0xff) - ((q >> 16) & 0xff))
                + Math.abs(((p >> 8) & 0xff) - ((q >> 8) & 0xff))
                + Math.abs((p & 0xff) - (q & 0xff))) / 3;
    }

    /// Loads the per-component `material:` declarations from fidelity-tests.yaml.
    /// An explicit --spec path wins; otherwise the canonical spec is located
    /// relative to the goldens dir (mirroring loadBackdrop). Only the flat subset
    /// the spec is written in is understood: a `components:` list whose entries
    /// start with `- id:` followed by 4-space-indented `key: value` lines.
    /// Entries whose `platforms:` allow-list excludes the platform are skipped.
    /// On Android the glass/lens intents degrade to "normal": Android tiles never
    /// composite the shared photo backdrop, so there is nothing to mask.
    /// Returns null when no spec can be read (callers fall back to the heuristic).
    static Map<String, String> loadMaterialMap(Path specPath, Path referenceDir, String platform) {
        Path candidate = specPath != null ? specPath
                : (referenceDir != null
                        ? referenceDir.resolve("../../common/src/main/resources/fidelity-tests.yaml").normalize()
                        : null);
        if (candidate == null || !Files.isRegularFile(candidate)) {
            if (specPath != null) {
                System.err.println("WARNING: --spec " + specPath + " not readable; falling back to glass heuristic");
            }
            return null;
        }
        boolean android = platform != null && platform.startsWith("and");
        Map<String, String> out = new LinkedHashMap<>();
        try {
            boolean inComponents = false;
            String id = null;
            String material = null;
            boolean platformOk = true;
            List<String> lines = Files.readAllLines(candidate);
            lines.add("- id: __end__");   // flush the final entry
            for (String raw : lines) {
                String line = raw;
                int hash = line.indexOf('#');
                if (hash >= 0) {
                    line = line.substring(0, hash);
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                if (trimmed.equals("components:")) {
                    inComponents = true;
                    continue;
                }
                if (!inComponents) {
                    continue;
                }
                if (trimmed.startsWith("- ")) {
                    if (id != null && platformOk) {
                        out.put(id, normalizeMaterial(material, android));
                    }
                    id = null;
                    material = null;
                    platformOk = true;
                    trimmed = trimmed.substring(2).trim();
                }
                int idx = trimmed.indexOf(':');
                if (idx < 0) {
                    continue;
                }
                String key = trimmed.substring(0, idx).trim();
                String value = trimmed.substring(idx + 1).trim();
                if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                        || value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                switch (key) {
                    case "id" -> id = value;
                    case "material" -> material = value;
                    case "platforms" -> {
                        platformOk = false;
                        for (String p : value.split(",")) {
                            p = p.trim();
                            if (!p.isEmpty() && platform != null
                                    && (platform.startsWith(p) || p.startsWith(platform))) {
                                platformOk = true;
                                break;
                            }
                        }
                    }
                    default -> { }
                }
            }
        } catch (IOException ex) {
            System.err.println("WARNING: failed to read spec " + candidate + ": " + ex.getMessage());
            return null;
        }
        return out.isEmpty() ? null : out;
    }

    /// A spec entry without a material declaration maps to null so that test
    /// keeps the legacy heuristic; glass/lens degrade to normal on Android.
    private static String normalizeMaterial(String material, boolean android) {
        if (material == null || material.isEmpty()) {
            return null;
        }
        if (!material.equals("normal") && !material.equals("glass") && !material.equals("lens")) {
            System.err.println("WARNING: unknown material '" + material + "' in spec; treating as normal");
            return "normal";
        }
        if (android && !material.equals("normal")) {
            return "normal";
        }
        return material;
    }

    /// Maps a delivered test name ("Toolbar_normal_dark") to its component's
    /// declared material via longest-id-prefix match at an underscore boundary,
    /// so a future id containing underscores still resolves. Null = unknown.
    static String resolveMaterial(Map<String, String> materialByComponent, String testName) {
        if (materialByComponent == null || testName == null) {
            return null;
        }
        String best = null;
        int bestLen = -1;
        for (Map.Entry<String, String> e : materialByComponent.entrySet()) {
            String id = e.getKey();
            if ((testName.equals(id) || testName.startsWith(id + "_")) && id.length() > bestLen) {
                best = e.getValue();
                bestLen = id.length();
            }
        }
        return best;
    }

    /// A tile is "glass" when its corners show the backdrop (the widget is inset, so
    /// the corner pixels match the stretched backdrop reference). Solid white/black
    /// tiles (checkbox, textfield, dialog card) fail this and keep the whole-tile
    /// solid-background scoring.
    private static boolean isGlassTile(PNGImage nativeImg, int[] refArr, int w, int h) {
        if (w < 16 || h < 16) {
            return false;
        }
        BufferedImage bn = toRgbImage(nativeImg);
        // Sample the corners INSET by a few pixels: the native capture leaves a 1px
        // dark border at the very edge, so the exact corner pixel is not the backdrop.
        int m = 5;
        int[][] pts = {{m, m}, {w - 1 - m, m}, {m, h - 1 - m}, {w - 1 - m, h - 1 - m}};
        int match = 0;
        for (int[] pt : pts) {
            int idx = pt[1] * w + pt[0];
            int p = bn.getRGB(pt[0], pt[1]) & 0xffffff;
            if (colorDist(p, refArr[idx]) <= GLASS_TAU) {
                match++;
            }
        }
        return match >= 3;
    }

    private static boolean[] contentMaskRef(int[] arr, int[] ref, int tau) {
        boolean[] m = new boolean[arr.length];
        for (int i = 0; i < arr.length; i++) {
            m[i] = colorDist(arr[i], ref[i]) > tau;
        }
        return m;
    }

    /// Glass-tile fidelity: the shared gradient backdrop is masked OUT (pixels close
    /// to the backdrop reference in BOTH images) and only the WIDGET is scored, so a
    /// widget that looks nothing like the native one can no longer ride the matching
    /// backdrop to a high number. fillSim is the colour agreement over the union of
    /// widget pixels (widget-in-only-one-image counts as full mismatch); ssim is
    /// computed over the widget bounding box only, so flat backdrop windows never
    /// inflate it. Returns {fidelity_percent, fillSim, fillSim}.
    private static double[] structuralFidelityGlass(PNGImage nativeImg, PNGImage cn1, int[] refArr) {
        BufferedImage bn = toRgbImage(nativeImg);
        BufferedImage bc = toRgbImage(cn1);
        int w = Math.min(bn.getWidth(), bc.getWidth());
        int h = Math.min(bn.getHeight(), bc.getHeight());
        int[] nArr = new int[w * h];
        int[] cArr = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                nArr[y * w + x] = bn.getRGB(x, y) & 0xffffff;
                cArr[y * w + x] = bc.getRGB(x, y) & 0xffffff;
            }
        }
        boolean[] nC = contentMaskRef(nArr, refArr, GLASS_TAU);
        boolean[] cC = contentMaskRef(cArr, refArr, GLASS_TAU);
        long sum = 0;
        long count = 0;
        int x0 = w, y0 = h, x1 = -1, y1 = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                if (!nC[idx] && !cC[idx]) {
                    continue;   // shared backdrop in both -> ignore
                }
                int d1 = tolerantDelta(nArr[idx], cArr, x, y, w, h);
                int d2 = tolerantDelta(cArr[idx], nArr, x, y, w, h);
                sum += Math.max(d1, d2);
                count++;
                if (x < x0) x0 = x;
                if (x > x1) x1 = x;
                if (y < y0) y0 = y;
                if (y > y1) y1 = y;
            }
        }
        if (count == 0) {
            // Neither render put anything over the backdrop -> identical (both bare
            // backdrop). Treat as a perfect match rather than dividing by zero.
            return new double[]{100.0d, 1.0d, 1.0d};
        }
        double fillSim = 1.0d - (double) sum / (count * 255.0d);
        if (fillSim < 0) {
            fillSim = 0;
        }
        double ssim;
        int bw = x1 - x0 + 1, bh = y1 - y0 + 1;
        if (bw >= 4 && bh >= 4) {
            double[] gn = grayCrop(nArr, w, x0, y0, bw, bh);
            double[] gc = grayCrop(cArr, w, x0, y0, bw, bh);
            ssim = computeSsimGray(gn, gc, bw, bh);
        } else {
            ssim = fillSim;
        }
        if (ssim < 0) {
            ssim = 0;
        }
        double headline = Math.sqrt(fillSim * ssim);
        return new double[]{100.0d * headline, fillSim, fillSim};
    }

    private static double[] grayCrop(int[] arr, int stride, int x0, int y0, int w, int h) {
        double[] g = new double[w * h];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int p = arr[(y0 + j) * stride + (x0 + i)];
                g[j * w + i] = 0.299d * ((p >> 16) & 0xff) + 0.587d * ((p >> 8) & 0xff) + 0.114d * (p & 0xff);
            }
        }
        return g;
    }

    private static double computeSsimGray(double[] ga, double[] gb, int w, int h) {
        final double c1 = 6.5025d;
        final double c2 = 58.5225d;
        int win = 8;
        double total = 0.0d;
        int windows = 0;
        for (int y = 0; y + win <= h; y += win) {
            for (int x = 0; x + win <= w; x += win) {
                total += ssimWindow(ga, gb, w, x, y, win, win, c1, c2);
                windows++;
            }
        }
        if (windows == 0) {
            return ssimWindow(ga, gb, w, 0, 0, w, h, c1, c2);
        }
        return total / windows;
    }

    /// Top/left margin agreement (1 = identical edge-to-widget distance, decaying
    /// with the summed absolute top+left margin error). Diagnostic only -- the
    /// absolute-position shapeSim already penalizes a mis-placed widget.
    private static double marginAgreement(int[] boxN, int[] boxC) {
        int err = Math.abs(boxN[1] - boxC[1]) + Math.abs(boxN[0] - boxC[0]);
        double a = 1.0d - err / 100.0d;
        return a < 0 ? 0 : a;
    }

    /// Bounding box {x, y, w, h} of the widget (pixels >CONTENT_TAU off bg).
    /// Returns w=0 when there is no meaningful content.
    private static int[] contentBBox(BufferedImage img, int bgRgb) {
        int w = img.getWidth(), h = img.getHeight();
        int bgR = (bgRgb >> 16) & 0xff, bgG = (bgRgb >> 8) & 0xff, bgB = bgRgb & 0xff;
        int x0 = w, y0 = h, x1 = -1, y1 = -1;
        long count = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = img.getRGB(x, y);
                int r = (p >> 16) & 0xff, g = (p >> 8) & 0xff, b = p & 0xff;
                if (Math.abs(r - bgR) > CONTENT_TAU || Math.abs(g - bgG) > CONTENT_TAU || Math.abs(b - bgB) > CONTENT_TAU) {
                    if (x < x0) x0 = x;
                    if (x > x1) x1 = x;
                    if (y < y0) y0 = y;
                    if (y > y1) y1 = y;
                    count++;
                }
            }
        }
        if (x1 < 0 || count < MIN_CONTENT_PIXELS) {
            return new int[]{0, 0, 0, 0};
        }
        return new int[]{x0, y0, x1 - x0 + 1, y1 - y0 + 1};
    }

    /// Sub-pixel tolerance window. For each pixel we look for the best match in the
    /// OTHER image within +/-ALIGN_RADIUS, adding ALIGN_PENALTY per pixel of offset
    /// beyond the first (the first pixel is free -- two render paths anti-aliasing
    /// the same edge differ by ~1px). This absorbs ONLY genuine sub-pixel fuzz; it
    /// is small on purpose, so a real margin / position error (the widget rendered
    /// several px off where it belongs) is NOT absorbed and shows up as mismatch.
    private static final int ALIGN_RADIUS = 2;
    private static final int ALIGN_PENALTY = 22;   // per pixel of offset, in 0..255 channel units

    /// Compares the two tiles at ABSOLUTE pixel position (1:1, no crop, no whole-
    /// widget re-alignment). Every pixel that is content in either tile is matched
    /// against the other within the small sub-pixel window; where the widget sits at
    /// a different place (a wrong margin) the content simply does not line up and is
    /// scored as mismatch. Symmetric: each side searches the other and the worse
    /// direction is taken so content present on only one side is penalized.
    private static double absoluteShapeSim(BufferedImage bn, BufferedImage bc, int bgRgb) {
        int bgR = (bgRgb >> 16) & 0xff, bgG = (bgRgb >> 8) & 0xff, bgB = bgRgb & 0xff;
        int w = Math.min(bn.getWidth(), bc.getWidth());
        int h = Math.min(bn.getHeight(), bc.getHeight());
        int[] nArr = new int[w * h];
        int[] cArr = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                nArr[y * w + x] = bn.getRGB(x, y) & 0xffffff;
                cArr[y * w + x] = bc.getRGB(x, y) & 0xffffff;
            }
        }
        boolean[] nC = contentMask(nArr, bgR, bgG, bgB);
        boolean[] cC = contentMask(cArr, bgR, bgG, bgB);
        long sum = 0;
        long count = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                if (!nC[idx] && !cC[idx]) {
                    continue;   // background in both -> part of neither widget
                }
                int d1 = tolerantDelta(nArr[idx], cArr, x, y, w, h);
                int d2 = tolerantDelta(cArr[idx], nArr, x, y, w, h);
                sum += Math.max(d1, d2);
                count++;
            }
        }
        if (count == 0) {
            return 1.0d;
        }
        double shape = 1.0d - (double) sum / (count * 255.0d);
        return shape < 0 ? 0 : shape;
    }

    /// Gradient (local edge strength) below which a pixel is treated as flat fill
    /// and contributes nothing to the structural term.
    private static final int EDGE_TAU = 12;

    /// Colour agreement WEIGHTED BY STRUCTURAL SALIENCE (local gradient magnitude),
    /// at absolute position. Each pixel's contribution is weighted by how much
    /// structure (edge/stroke) it carries in EITHER render, so a large flat fill --
    /// however well it matches -- barely moves the score, while glyph strokes and
    /// widget edges dominate. A control whose distinguishing marks land in a
    /// different place (wrong title alignment, wrong size, missing separator) scores
    /// low here even when the surrounding fill matches perfectly. Returns 0..1.
    private static double structuralSalienceSim(BufferedImage bn, BufferedImage bc, int bgRgb) {
        int w = Math.min(bn.getWidth(), bc.getWidth());
        int h = Math.min(bn.getHeight(), bc.getHeight());
        int[] nArr = new int[w * h];
        int[] cArr = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                nArr[y * w + x] = bn.getRGB(x, y) & 0xffffff;
                cArr[y * w + x] = bc.getRGB(x, y) & 0xffffff;
            }
        }
        int[] gN = gradientMag(nArr, w, h);
        int[] gC = gradientMag(cArr, w, h);
        double wSum = 0.0d;
        double dSum = 0.0d;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                int sal = Math.max(gN[idx], gC[idx]);
                if (sal < EDGE_TAU) {
                    continue;   // flat in both -> not structural content
                }
                int d1 = tolerantDelta(nArr[idx], cArr, x, y, w, h);
                int d2 = tolerantDelta(cArr[idx], nArr, x, y, w, h);
                int delta = Math.max(d1, d2);
                // Weight by salience SQUARED so the strongest edges (glyph strokes,
                // crisp widget outlines) dominate over faint low-contrast edges (a
                // subtle bar fill boundary). A mis-placed title -- the salient mark a
                // human keys on -- then drives the score, not the incidental frame.
                double weight = (double) sal * sal;
                wSum += weight * 255.0d;
                dSum += weight * delta;
            }
        }
        if (wSum == 0.0d) {
            return 1.0d;   // neither render has any structure -> trivially equal
        }
        double s = 1.0d - dSum / wSum;
        return s < 0 ? 0 : s;
    }

    /// Per-pixel local edge strength (0..255): the larger of the right- and
    /// down-neighbour mean channel differences. High on glyph/widget edges, ~0 on
    /// flat fills.
    private static int[] gradientMag(int[] arr, int w, int h) {
        int[] g = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int p = arr[y * w + x];
                int pr = (p >> 16) & 0xff, pg = (p >> 8) & 0xff, pb = p & 0xff;
                int gx = 0, gy = 0;
                if (x + 1 < w) {
                    int q = arr[y * w + x + 1];
                    gx = (Math.abs(pr - ((q >> 16) & 0xff)) + Math.abs(pg - ((q >> 8) & 0xff)) + Math.abs(pb - (q & 0xff))) / 3;
                }
                if (y + 1 < h) {
                    int q = arr[(y + 1) * w + x];
                    gy = (Math.abs(pr - ((q >> 16) & 0xff)) + Math.abs(pg - ((q >> 8) & 0xff)) + Math.abs(pb - (q & 0xff))) / 3;
                }
                g[idxClamp(y * w + x, g.length)] = Math.max(gx, gy);
            }
        }
        return g;
    }

    private static int idxClamp(int i, int len) {
        return i < 0 ? 0 : (i >= len ? len - 1 : i);
    }

    private static boolean[] contentMask(int[] arr, int bgR, int bgG, int bgB) {
        boolean[] m = new boolean[arr.length];
        for (int i = 0; i < arr.length; i++) {
            int p = arr[i];
            int r = (p >> 16) & 0xff, g = (p >> 8) & 0xff, b = p & 0xff;
            m[i] = Math.abs(r - bgR) > CONTENT_TAU || Math.abs(g - bgG) > CONTENT_TAU
                    || Math.abs(b - bgB) > CONTENT_TAU;
        }
        return m;
    }

    /// Best (lowest) cost of matching the anchor pixel against any pixel of `other`
    /// within +/-ALIGN_RADIUS, where cost = mean channel delta + ALIGN_PENALTY per
    /// pixel of Chebyshev offset. Returns a value in 0..255.
    private static int tolerantDelta(int anchor, int[] other, int x, int y, int w, int h) {
        int ar = (anchor >> 16) & 0xff, ag = (anchor >> 8) & 0xff, ab = anchor & 0xff;
        int best = Integer.MAX_VALUE;
        for (int dy = -ALIGN_RADIUS; dy <= ALIGN_RADIUS; dy++) {
            int yy = y + dy;
            if (yy < 0 || yy >= h) {
                continue;
            }
            for (int dx = -ALIGN_RADIUS; dx <= ALIGN_RADIUS; dx++) {
                int xx = x + dx;
                if (xx < 0 || xx >= w) {
                    continue;
                }
                int p = other[yy * w + xx];
                int delta = (Math.abs(ar - ((p >> 16) & 0xff))
                        + Math.abs(ag - ((p >> 8) & 0xff))
                        + Math.abs(ab - (p & 0xff))) / 3;
                // The first pixel of offset is free: a <=1px halo is just the two
                // rendering stacks anti-aliasing the same edge a hair differently,
                // which is visually identical. From 2px out the distance penalty
                // kicks in, so genuine size/shape/position differences still count
                // (e.g. a square vs a pill corner, which differs by ~2-3px, is no
                // longer silently forgiven).
                int cheb = Math.max(Math.abs(dx), Math.abs(dy));
                int cost = delta + Math.max(0, cheb - 1) * ALIGN_PENALTY;
                if (cost < best) {
                    best = cost;
                }
            }
        }
        return best == Integer.MAX_VALUE ? 255 : Math.min(best, 255);
    }

    /// Mean absolute per-channel difference across all RGB channels, 0..255.
    /// 0 means pixel-identical; reported only as a diagnostic now.
    private static double meanChannelDelta(PNGImage a, PNGImage b) {
        int[] ar = toRgbArray(a);
        int[] br = toRgbArray(b);
        long sum = 0;
        long count = (long) ar.length * 3L;
        for (int i = 0; i < ar.length; i++) {
            int e = ar[i];
            int f = br[i];
            sum += Math.abs(((e >> 16) & 0xff) - ((f >> 16) & 0xff));
            sum += Math.abs(((e >> 8) & 0xff) - ((f >> 8) & 0xff));
            sum += Math.abs((e & 0xff) - (f & 0xff));
        }
        return count == 0 ? 0.0d : (double) sum / (double) count;
    }

    /// Structural similarity (SSIM) over 8x8 non-overlapping grayscale windows,
    /// averaged. Robust to the ~1px sub-pixel offsets a mean-delta penalises
    /// harshly, so it is the headline fidelity number while mean-delta is the
    /// diagnostic. Falls back to a single whole-image window for tiles smaller
    /// than the window size. Constants are the standard C1/C2 for 8-bit images.
    private static double computeSsim(PNGImage a, PNGImage b) {
        int w = a.width();
        int h = a.height();
        double[] ga = toGray(a);
        double[] gb = toGray(b);
        final double c1 = 6.5025d;   // (0.01*255)^2
        final double c2 = 58.5225d;  // (0.03*255)^2
        int win = 8;
        double total = 0.0d;
        int windows = 0;
        for (int y = 0; y + win <= h; y += win) {
            for (int x = 0; x + win <= w; x += win) {
                total += ssimWindow(ga, gb, w, x, y, win, win, c1, c2);
                windows++;
            }
        }
        if (windows == 0) {
            return ssimWindow(ga, gb, w, 0, 0, w, h, c1, c2);
        }
        return total / windows;
    }

    private static double ssimWindow(double[] ga, double[] gb, int stride, int x0, int y0, int ww, int wh,
            double c1, double c2) {
        int count = ww * wh;
        if (count <= 0) {
            return 1.0d;
        }
        double ma = 0.0d;
        double mb = 0.0d;
        for (int j = 0; j < wh; j++) {
            int row = (y0 + j) * stride + x0;
            for (int i = 0; i < ww; i++) {
                ma += ga[row + i];
                mb += gb[row + i];
            }
        }
        ma /= count;
        mb /= count;
        double va = 0.0d;
        double vb = 0.0d;
        double cov = 0.0d;
        for (int j = 0; j < wh; j++) {
            int row = (y0 + j) * stride + x0;
            for (int i = 0; i < ww; i++) {
                double da = ga[row + i] - ma;
                double db = gb[row + i] - mb;
                va += da * da;
                vb += db * db;
                cov += da * db;
            }
        }
        double denomCount = count > 1 ? (count - 1) : 1;
        va /= denomCount;
        vb /= denomCount;
        cov /= denomCount;
        return ((2 * ma * mb + c1) * (2 * cov + c2)) / ((ma * ma + mb * mb + c1) * (va + vb + c2));
    }

    private static double[] toGray(PNGImage image) {
        int[] rgb = toRgbArray(image);
        double[] gray = new double[rgb.length];
        for (int i = 0; i < rgb.length; i++) {
            int p = rgb[i];
            int r = (p >> 16) & 0xff;
            int g = (p >> 8) & 0xff;
            int b = p & 0xff;
            gray[i] = 0.299d * r + 0.587d * g + 0.114d * b;
        }
        return gray;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private static double round4(double value) {
        return Math.round(value * 10000.0d) / 10000.0d;
    }

    private static CommentPayload loadPreviewOrBuild(String testName, Path actualPath, Path previewDir) throws IOException {
        return loadPreviewOrBuild(testName, actualPath, previewDir, null);
    }

    private static CommentPayload loadPreviewOrBuild(String testName, Path actualPath, Path previewDir, PNGImage cached) throws IOException {
        if (previewDir != null) {
            CommentPayload external = loadExternalPreviewPayload(testName, previewDir);
            if (external != null) {
                return external;
            }
        }
        PNGImage image = cached != null ? cached : loadPngWithRetry(actualPath);
        return buildCommentPayload(image, MAX_COMMENT_BASE64);
    }

    private static CommentPayload loadExternalPreviewPayload(String testName, Path previewDir) throws IOException {
        String slug = slugify(testName);
        Path jpg = previewDir.resolve(slug + ".jpg");
        Path jpeg = previewDir.resolve(slug + ".jpeg");
        Path png = previewDir.resolve(slug + ".png");
        List<Path> candidates = new ArrayList<>();
        if (Files.exists(jpg)) candidates.add(jpg);
        if (Files.exists(jpeg)) candidates.add(jpeg);
        if (Files.exists(png)) candidates.add(png);
        if (candidates.isEmpty()) {
            return null;
        }
        Path path = candidates.get(0);
        byte[] data = Files.readAllBytes(path);
        String encoded = Base64.getEncoder().encodeToString(data);
        String mime = path.toString().toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
        if (encoded.length() <= MAX_COMMENT_BASE64) {
            return new CommentPayload(encoded.length(), encoded, mime, mime.endsWith("jpeg") ? "jpeg" : "png", null, null, "Preview provided by instrumentation", data);
        }
        return new CommentPayload(encoded.length(), null, mime, mime.endsWith("jpeg") ? "jpeg" : "png", null, "too_large", "Preview provided by instrumentation", data);
    }

    private static void recordPayload(Map<String, Object> record, CommentPayload payload, String defaultName, Path previewDir) throws IOException {
        if (payload == null) {
            return;
        }
        if (payload.base64 != null) {
            record.put("base64", payload.base64);
        } else {
            record.put("base64_omitted", payload.omittedReason);
            record.put("base64_length", payload.base64Length);
        }
        record.put("base64_mime", payload.mime);
        record.put("base64_codec", payload.codec);
        if (payload.quality != null) {
            record.put("base64_quality", payload.quality);
        }
        if (payload.note != null) {
            record.put("base64_note", payload.note);
        }
        if (previewDir != null && payload.data != null) {
            Files.createDirectories(previewDir);
            String suffix = payload.mime.equals("image/jpeg") ? ".jpg" : ".png";
            String baseName = slugify(defaultName.contains(".") ? defaultName.substring(0, defaultName.lastIndexOf('.')) : defaultName);
            Path previewPath = previewDir.resolve(baseName + suffix);
            Files.write(previewPath, payload.data);
            Map<String, Object> preview = new HashMap<>();
            preview.put("path", previewPath.toString());
            preview.put("name", previewPath.getFileName().toString());
            preview.put("mime", payload.mime);
            preview.put("codec", payload.codec);
            if (payload.quality != null) {
                preview.put("quality", payload.quality);
            }
            if (payload.note != null) {
                preview.put("note", payload.note);
            }
            record.put("preview", preview);
        }
    }

    private static String slugify(String name) {
        StringBuilder sb = new StringBuilder();
        for (char ch : name.toCharArray()) {
            if (Character.isLetterOrDigit(ch)) {
                sb.append(Character.toLowerCase(ch));
            } else {
                sb.append('_');
            }
        }
        if (sb.length() == 0) {
            sb.append("preview");
        }
        return sb.toString();
    }

    private static CommentPayload buildCommentPayload(PNGImage image, int maxLength) {
        BufferedImage rgbImage = toRgbImage(image);
        List<Double> scales = List.of(1.0, 0.7, 0.5, 0.35, 0.25);
        byte[] smallestData = null;
        Integer smallestQuality = null;
        for (double scale : scales) {
            BufferedImage candidate = rgbImage;
            if (scale < 0.999) {
                candidate = scaleImage(rgbImage, scale);
            }
            for (int quality : JPEG_QUALITY_CANDIDATES) {
                byte[] data = writeJpeg(candidate, quality);
                if (data == null) {
                    continue;
                }
                smallestData = data;
                smallestQuality = quality;
                String encoded = Base64.getEncoder().encodeToString(data);
                if (encoded.length() <= maxLength) {
                    String note = "JPEG preview quality " + quality;
                    if (scale < 0.999) {
                        note += "; downscaled to " + candidate.getWidth() + "x" + candidate.getHeight();
                    }
                    return new CommentPayload(encoded.length(), encoded, "image/jpeg", "jpeg", quality, null, note, data);
                }
            }
        }
        if (smallestData != null) {
            String encoded = Base64.getEncoder().encodeToString(smallestData);
            return new CommentPayload(encoded.length(), null, "image/jpeg", "jpeg", smallestQuality, "too_large", "All JPEG previews exceeded limit even after downscaling", smallestData);
        }
        byte[] pngBytes = encodePng(image);
        String encoded = Base64.getEncoder().encodeToString(pngBytes);
        if (encoded.length() <= maxLength) {
            return new CommentPayload(encoded.length(), encoded, "image/png", "png", null, null, null, pngBytes);
        }
        return new CommentPayload(encoded.length(), null, "image/png", "png", null, "too_large", null, pngBytes);
    }

    private static BufferedImage scaleImage(BufferedImage source, double scale) {
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage dest = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dest.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(source, 0, 0, width, height, null);
        g.dispose();
        return dest;
    }

    private static byte[] writeJpeg(BufferedImage image, int quality) {
        try {
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                if (param.canWriteCompressed()) {
                    param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                    param.setCompressionQuality(Math.max(0.0f, Math.min(1.0f, quality / 100f)));
                }
                writer.write(null, new IIOImage(image, null, null), param);
            } finally {
                writer.dispose();
            }
            return baos.toByteArray();
        } catch (Exception ex) {
            return null;
        }
    }

    /// Returns the top-left w x h crop of an image (no scaling). Pixels are stored
    /// row-major, bytesPerPixel each, width*bytesPerPixel per row.
    private static PNGImage cropTopLeft(PNGImage img, int w, int h) {
        if (w == img.width() && h == img.height()) {
            return img;
        }
        int bpp = img.bytesPerPixel();
        int srcStride = img.width() * bpp;
        int dstStride = w * bpp;
        byte[] out = new byte[h * dstStride];
        for (int y = 0; y < h; y++) {
            System.arraycopy(img.pixels(), y * srcStride, out, y * dstStride, dstStride);
        }
        return new PNGImage(w, h, img.bitDepth(), img.colorType(), out, bpp);
    }

    private static BufferedImage toRgbImage(PNGImage image) {
        BufferedImage output = new BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB);
        int stride = image.width * image.bytesPerPixel;
        int offset = 0;
        for (int y = 0; y < image.height; y++) {
            for (int x = 0; x < image.width; x++) {
                int r, g, b, a;
                switch (image.colorType) {
                    case 0 -> {
                        int v = image.pixels[offset] & 0xFF;
                        r = g = b = v;
                        a = 255;
                        offset += 1;
                    }
                    case 2 -> {
                        r = image.pixels[offset] & 0xFF;
                        g = image.pixels[offset + 1] & 0xFF;
                        b = image.pixels[offset + 2] & 0xFF;
                        a = 255;
                        offset += 3;
                    }
                    case 4 -> {
                        int v = image.pixels[offset] & 0xFF;
                        a = image.pixels[offset + 1] & 0xFF;
                        r = g = b = v;
                        offset += 2;
                    }
                    case 6 -> {
                        r = image.pixels[offset] & 0xFF;
                        g = image.pixels[offset + 1] & 0xFF;
                        b = image.pixels[offset + 2] & 0xFF;
                        a = image.pixels[offset + 3] & 0xFF;
                        offset += 4;
                    }
                    default -> throw new IllegalArgumentException("Unsupported PNG color type: " + image.colorType);
                }
                int rgb = compositePixel(r, g, b, a);
                output.setRGB(x, y, rgb);
            }
        }
        return output;
    }

    private static int compositePixel(int r, int g, int b, int a) {
        if (a >= 255) {
            return (r << 16) | (g << 8) | b;
        }
        double alpha = a / 255.0;
        int outR = (int) Math.round(r * alpha + 255 * (1 - alpha));
        int outG = (int) Math.round(g * alpha + 255 * (1 - alpha));
        int outB = (int) Math.round(b * alpha + 255 * (1 - alpha));
        return (clamp(outR) << 16) | (clamp(outG) << 8) | clamp(outB);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /// Reads an optional per-test tolerance override from
    /// "<referenceDir>/<testName>.tolerance" (simple key=value lines:
    /// maxChannelDelta and/or maxMismatchPercent). Returns
    /// {channelDelta, mismatchPercent}, falling back to the supplied defaults for
    /// any key the file omits or when the file is absent.
    private static double[] readTolerance(Path referenceDir, String testName,
            int defChannelDelta, double defMismatchPercent) {
        double[] t = { defChannelDelta, defMismatchPercent };
        Path tolPath = referenceDir.resolve(testName + ".tolerance");
        if (!Files.exists(tolPath)) {
            return t;
        }
        try {
            for (String line : Files.readAllLines(tolPath)) {
                String s = line.trim();
                if (s.isEmpty() || s.startsWith("#")) {
                    continue;
                }
                int eq = s.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = s.substring(0, eq).trim();
                String val = s.substring(eq + 1).trim();
                if (key.equals("maxChannelDelta")) {
                    t[0] = Integer.parseInt(val);
                } else if (key.equals("maxMismatchPercent")) {
                    t[1] = Double.parseDouble(val);
                }
            }
        } catch (Exception ex) {
            System.err.println("Warning: could not read tolerance " + tolPath + ": " + ex.getMessage());
        }
        return t;
    }

    private static Map<String, Object> compareImages(PNGImage expected, PNGImage actual, int maxChannelDelta, double maxMismatchPercent) {
        boolean equal = expected.width == actual.width
                && expected.height == actual.height
                && expected.bitDepth == actual.bitDepth
                && expected.colorType == actual.colorType
                && java.util.Arrays.equals(expected.pixels, actual.pixels);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("width", actual.width);
        result.put("height", actual.height);
        result.put("bit_depth", actual.bitDepth);
        result.put("color_type", actual.colorType);
        if (!equal && maxChannelDelta > 0 && maxMismatchPercent >= 0 && expected.width == actual.width && expected.height == actual.height) {
            int totalPixels = actual.width * actual.height;
            int mismatchCount = countMismatchedPixels(expected, actual, maxChannelDelta);
            double mismatchPercent = totalPixels == 0 ? 0d : (mismatchCount * 100d) / totalPixels;
            result.put("mismatch_count", mismatchCount);
            result.put("mismatch_percent", mismatchPercent);
            result.put("max_channel_delta", maxChannelDelta);
            result.put("max_mismatch_percent", maxMismatchPercent);
            equal = mismatchPercent <= maxMismatchPercent;
        }
        result.put("equal", equal);
        return result;
    }

    private static int countMismatchedPixels(PNGImage expected, PNGImage actual, int maxChannelDelta) {
        int[] expectedRgb = toRgbArray(expected);
        int[] actualRgb = toRgbArray(actual);
        int mismatched = 0;
        for (int i = 0; i < expectedRgb.length; i++) {
            int e = expectedRgb[i];
            int a = actualRgb[i];
            int er = (e >> 16) & 0xff;
            int eg = (e >> 8) & 0xff;
            int eb = e & 0xff;
            int ar = (a >> 16) & 0xff;
            int ag = (a >> 8) & 0xff;
            int ab = a & 0xff;
            if (Math.abs(er - ar) > maxChannelDelta
                    || Math.abs(eg - ag) > maxChannelDelta
                    || Math.abs(eb - ab) > maxChannelDelta) {
                mismatched++;
            }
        }
        return mismatched;
    }

    private static int[] toRgbArray(PNGImage image) {
        BufferedImage rgbImage = toRgbImage(image);
        int[] pixels = new int[image.width * image.height];
        rgbImage.getRGB(0, 0, image.width, image.height, pixels, 0, image.width);
        return pixels;
    }

    private static PNGImage loadPngWithRetry(Path path) throws IOException {
        int attempt = 0;
        long lastSize = -1;
        while (true) {
            try {
                // Stabilize check: if file size is changing, wait
                if (Files.exists(path)) {
                    long size = Files.size(path);
                    if (size != lastSize) {
                        lastSize = size;
                        if (attempt > 0) {
                            // If size changed, we should wait and retry
                            Thread.sleep(RETRY_DELAY_MS);
                            attempt++;
                            if (attempt >= MAX_RETRIES) {
                                break; // fall through to try loading anyway, will likely fail
                            }
                            continue;
                        }
                    }
                }

                return loadPng(path);
            } catch (IOException e) {
                // Only retry on truncated chunk or premature end of file
                if (e.getMessage() != null &&
                        (e.getMessage().contains("PNG chunk truncated") ||
                                e.getMessage().contains("Premature end of file") ||
                                e.getMessage().contains("Missing IHDR"))) {

                    attempt++;
                    if (attempt >= MAX_RETRIES) {
                        throw e;
                    }
                    try {
                        System.err.println("Retrying load of " + path + " (attempt " + (attempt + 1) + "/" + MAX_RETRIES + "): " + e.getMessage());
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while waiting to retry load of " + path, ie);
                    }
                } else {
                    throw e;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while loading " + path, e);
            }
        }
        return loadPng(path); // Final attempt
    }

    private static PNGImage loadPng(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (data[i] != PNG_SIGNATURE[i]) {
                throw new IOException(path + " is not a PNG file (missing signature) on " + path);
            }
        }
        int offset = PNG_SIGNATURE.length;
        int width = 0;
        int height = 0;
        int bitDepth = 0;
        int colorType = 0;
        int interlace = 0;
        List<byte[]> idatChunks = new ArrayList<>();
        boolean sawIend = false;
        while (offset + 8 <= data.length) {
            int length = readInt(data, offset);
            byte[] type = java.util.Arrays.copyOfRange(data, offset + 4, offset + 8);
            offset += 8;
            // PNG chunk length is a 31-bit unsigned int; readInt returns it as
            // signed, so a negative value here is by definition out-of-range.
            // This typically means we've walked off the end of the valid chunks
            // into trailing garbage (e.g., a truncated capture missing IEND);
            // surface a clear message instead of letting Arrays.copyOfRange
            // throw the cryptic "<from> > <to>" IllegalArgumentException.
            if (length < 0 || offset + length + 4 > data.length) {
                throw new IOException("PNG chunk truncated or out-of-range length while processing: " + path
                        + " (chunk length=" + length + ", offset=" + offset + ", file size=" + data.length + ")");
            }
            byte[] chunkData = java.util.Arrays.copyOfRange(data, offset, offset + length);
            offset += length + 4; // skip data + CRC
            String chunkType = new String(type, StandardCharsets.US_ASCII);
            if ("IHDR".equals(chunkType)) {
                width = readInt(chunkData, 0);
                height = readInt(chunkData, 4);
                bitDepth = chunkData[8] & 0xFF;
                colorType = chunkData[9] & 0xFF;
                int compression = chunkData[10] & 0xFF;
                int filter = chunkData[11] & 0xFF;
                interlace = chunkData[12] & 0xFF;
                if (compression != 0 || filter != 0) {
                    throw new IOException("Unsupported PNG compression or filter method on " + path);
                }
            } else if ("IDAT".equals(chunkType)) {
                idatChunks.add(chunkData);
            } else if ("IEND".equals(chunkType)) {
                sawIend = true;
                break;
            }
        }
        if (!sawIend) {
            throw new IOException("PNG missing IEND chunk (truncated capture?) while processing: " + path);
        }
        if (width <= 0 || height <= 0) {
            throw new IOException("Missing IHDR chunk on " + path);
        }
        if (interlace != 0) {
            throw new IOException("Interlaced PNGs are not supported " + path);
        }
        int bytesPerPixel = bytesPerPixel(bitDepth, colorType);
        byte[] combined = concat(idatChunks);
        byte[] raw = inflate(combined);
        byte[] pixels = unfilter(width, height, bytesPerPixel, raw);
        return new PNGImage(width, height, bitDepth, colorType, pixels, bytesPerPixel);
    }

    private static byte[] encodePng(PNGImage image) {
        try {
            ByteArrayOutputStream raw = new ByteArrayOutputStream();
            int stride = image.width * image.bytesPerPixel;
            for (int y = 0; y < image.height; y++) {
                raw.write(0);
                raw.write(image.pixels, y * stride, stride);
            }
            byte[] compressed = deflate(raw.toByteArray());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(PNG_SIGNATURE);
            out.write(chunk("IHDR", buildIhdr(image)));
            out.write(chunk("IDAT", compressed));
            out.write(chunk("IEND", new byte[0]));
            return out.toByteArray();
        } catch (IOException ex) {
            return new byte[0];
        }
    }

    private static byte[] deflate(byte[] data) throws IOException {
        java.util.zip.Deflater deflater = new java.util.zip.Deflater();
        deflater.setInput(data);
        deflater.finish();
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            out.write(buffer, 0, count);
        }
        deflater.end();
        return out.toByteArray();
    }

    private static byte[] buildIhdr(PNGImage image) {
        byte[] ihdr = new byte[13];
        writeInt(ihdr, 0, image.width);
        writeInt(ihdr, 4, image.height);
        ihdr[8] = (byte) image.bitDepth;
        ihdr[9] = (byte) image.colorType;
        ihdr[10] = 0;
        ihdr[11] = 0;
        ihdr[12] = 0;
        return ihdr;
    }

    private static byte[] chunk(String type, byte[] payload) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeInt(out, payload.length);
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        out.write(typeBytes);
        out.write(payload);
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(typeBytes);
        crc.update(payload);
        writeInt(out, (int) crc.getValue());
        return out.toByteArray();
    }

    private static void writeInt(ByteArrayOutputStream out, int value) {
        out.write((value >>> 24) & 0xFF);
        out.write((value >>> 16) & 0xFF);
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
    }

    private static void writeInt(byte[] array, int offset, int value) {
        array[offset] = (byte) ((value >>> 24) & 0xFF);
        array[offset + 1] = (byte) ((value >>> 16) & 0xFF);
        array[offset + 2] = (byte) ((value >>> 8) & 0xFF);
        array[offset + 3] = (byte) (value & 0xFF);
    }

    private static byte[] concat(List<byte[]> chunks) {
        int total = 0;
        for (byte[] chunk : chunks) {
            total += chunk.length;
        }
        byte[] combined = new byte[total];
        int offset = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, combined, offset, chunk.length);
            offset += chunk.length;
        }
        return combined;
    }

    private static byte[] inflate(byte[] data) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0 && inflater.needsInput()) {
                    break;
                }
                out.write(buffer, 0, count);
            }
        } catch (DataFormatException ex) {
            throw new IOException("Failed to decompress IDAT data: " + ex.getMessage(), ex);
        } finally {
            inflater.end();
        }
        return out.toByteArray();
    }

    private static byte[] unfilter(int width, int height, int bytesPerPixel, byte[] raw) throws IOException {
        int stride = width * bytesPerPixel;
        int expected = height * (stride + 1);
        if (raw.length != expected) {
            throw new IOException("PNG IDAT payload has unexpected length");
        }
        byte[] result = new byte[height * stride];
        int inOffset = 0;
        int outOffset = 0;
        for (int row = 0; row < height; row++) {
            int filter = raw[inOffset++] & 0xFF;
            switch (filter) {
                case 0 -> {
                    System.arraycopy(raw, inOffset, result, outOffset, stride);
                }
                case 1 -> {
                    for (int i = 0; i < stride; i++) {
                        int left = i >= bytesPerPixel ? result[outOffset + i - bytesPerPixel] & 0xFF : 0;
                        int val = (raw[inOffset + i] & 0xFF) + left;
                        result[outOffset + i] = (byte) (val & 0xFF);
                    }
                }
                case 2 -> {
                    for (int i = 0; i < stride; i++) {
                        int up = row > 0 ? result[outOffset + i - stride] & 0xFF : 0;
                        int val = (raw[inOffset + i] & 0xFF) + up;
                        result[outOffset + i] = (byte) (val & 0xFF);
                    }
                }
                case 3 -> {
                    for (int i = 0; i < stride; i++) {
                        int left = i >= bytesPerPixel ? result[outOffset + i - bytesPerPixel] & 0xFF : 0;
                        int up = row > 0 ? result[outOffset + i - stride] & 0xFF : 0;
                        int val = (raw[inOffset + i] & 0xFF) + ((left + up) / 2);
                        result[outOffset + i] = (byte) (val & 0xFF);
                    }
                }
                case 4 -> {
                    for (int i = 0; i < stride; i++) {
                        int left = i >= bytesPerPixel ? result[outOffset + i - bytesPerPixel] & 0xFF : 0;
                        int up = row > 0 ? result[outOffset + i - stride] & 0xFF : 0;
                        int upLeft = (row > 0 && i >= bytesPerPixel) ? result[outOffset + i - stride - bytesPerPixel] & 0xFF : 0;
                        int paeth = paethPredictor(left, up, upLeft);
                        int val = (raw[inOffset + i] & 0xFF) + paeth;
                        result[outOffset + i] = (byte) (val & 0xFF);
                    }
                }
                default -> throw new IOException("Unsupported PNG filter type: " + filter);
            }
            inOffset += stride;
            outOffset += stride;
        }
        return result;
    }

    private static int paethPredictor(int a, int b, int c) {
        int p = a + b - c;
        int pa = Math.abs(p - a);
        int pb = Math.abs(p - b);
        int pc = Math.abs(p - c);
        if (pa <= pb && pa <= pc) {
            return a;
        }
        if (pb <= pc) {
            return b;
        }
        return c;
    }

    private static int bytesPerPixel(int bitDepth, int colorType) throws IOException {
        if (bitDepth != 8) {
            throw new IOException("Unsupported bit depth: " + bitDepth);
        }
        return switch (colorType) {
            case 0 -> 1;
            case 2 -> 3;
            case 4 -> 2;
            case 6 -> 4;
            default -> throw new IOException("Unsupported color type: " + colorType);
        };
    }

    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
    }

    private static final class CommentPayload {
        final int base64Length;
        final String base64;
        final String mime;
        final String codec;
        final Integer quality;
        final String omittedReason;
        final String note;
        final byte[] data;

        CommentPayload(int base64Length, String base64, String mime, String codec, Integer quality, String omittedReason, String note, byte[] data) {
            this.base64Length = base64Length;
            this.base64 = base64;
            this.mime = mime;
            this.codec = codec;
            this.quality = quality;
            this.omittedReason = omittedReason;
            this.note = note;
            this.data = data;
        }
    }

    private record PNGImage(int width, int height, int bitDepth, int colorType, byte[] pixels, int bytesPerPixel) {
    }

    private static class Arguments {
        final Path referenceDir;
        final List<Map.Entry<String, Path>> actualEntries;
        final boolean emitBase64;
        final Path previewDir;
        final int maxChannelDelta;
        final double maxMismatchPercent;
        final String mode;
        final Path backdrop;
        final Path spec;
        final String specPlatform;

        private Arguments(Path referenceDir, List<Map.Entry<String, Path>> actualEntries, boolean emitBase64, Path previewDir,
                          int maxChannelDelta, double maxMismatchPercent, String mode, Path backdrop,
                          Path spec, String specPlatform) {
            this.referenceDir = referenceDir;
            this.actualEntries = actualEntries;
            this.emitBase64 = emitBase64;
            this.previewDir = previewDir;
            this.maxChannelDelta = maxChannelDelta;
            this.maxMismatchPercent = maxMismatchPercent;
            this.mode = mode;
            this.backdrop = backdrop;
            this.spec = spec;
            this.specPlatform = specPlatform;
        }

        static Arguments parse(String[] args) {
            Path reference = null;
            boolean emitBase64 = false;
            Path previewDir = null;
            int maxChannelDelta = 4;
            double maxMismatchPercent = 0.30d;
            String mode = "golden";
            Path backdrop = null;
            Path spec = null;
            String specPlatform = "ios";
            List<Map.Entry<String, Path>> actuals = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--mode" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --mode");
                            return null;
                        }
                        mode = args[i];
                        if (!mode.equals("golden") && !mode.equals("fidelity")) {
                            System.err.println("Unknown --mode (expected golden or fidelity): " + mode);
                            return null;
                        }
                    }
                    case "--reference-dir" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --reference-dir");
                            return null;
                        }
                        reference = Path.of(args[i]);
                    }
                    case "--backdrop" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --backdrop");
                            return null;
                        }
                        backdrop = Path.of(args[i]);
                    }
                    case "--spec" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --spec");
                            return null;
                        }
                        spec = Path.of(args[i]);
                    }
                    case "--spec-platform" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --spec-platform");
                            return null;
                        }
                        specPlatform = args[i];
                    }
                    case "--emit-base64" -> emitBase64 = true;
                    case "--preview-dir" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --preview-dir");
                            return null;
                        }
                        previewDir = Path.of(args[i]);
                    }
                    case "--actual" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --actual");
                            return null;
                        }
                        String value = args[i];
                        int idx = value.indexOf('=');
                        if (idx < 0) {
                            System.err.println("Invalid --actual value: " + value);
                            return null;
                        }
                        String name = value.substring(0, idx);
                        Path path = Path.of(value.substring(idx + 1));
                        actuals.add(Map.entry(name, path));
                    }
                    case "--max-channel-delta" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --max-channel-delta");
                            return null;
                        }
                        maxChannelDelta = parseIntArg("--max-channel-delta", args[i]);
                        if (maxChannelDelta < 0) {
                            return null;
                        }
                    }
                    case "--max-mismatch-percent" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --max-mismatch-percent");
                            return null;
                        }
                        maxMismatchPercent = parseDoubleArg("--max-mismatch-percent", args[i]);
                        if (maxMismatchPercent < 0) {
                            return null;
                        }
                    }
                    default -> {
                        System.err.println("Unknown argument: " + arg);
                        return null;
                    }
                }
            }
            if (reference == null) {
                System.err.println("--reference-dir is required");
                return null;
            }
            return new Arguments(reference, actuals, emitBase64, previewDir, maxChannelDelta, maxMismatchPercent, mode, backdrop, spec, specPlatform);
        }

        private static int parseIntArg(String flag, String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid integer for " + flag + ": " + value);
                return -1;
            }
        }

        private static double parseDoubleArg(String flag, String value) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid number for " + flag + ": " + value);
                return -1d;
            }
        }
    }
}

class JsonUtil {
    private JsonUtil() {}

    public static Object parse(String text) {
        return new Parser(text).parseValue();
    }

    public static String stringify(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> asObject(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                if (key instanceof String s) {
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

    private static void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String s) {
            writeString(sb, s);
        } else if (value instanceof Number || value instanceof Boolean) {
            sb.append(value.toString());
        } else if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                if (!(key instanceof String sKey)) {
                    continue;
                }
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeString(sb, sKey);
                sb.append(':');
                writeValue(sb, entry.getValue());
            }
            sb.append('}');
        } else if (value instanceof List<?> list) {
            sb.append('[');
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeValue(sb, item);
            }
            sb.append(']');
        } else {
            writeString(sb, value.toString());
        }
    }

    private static void writeString(StringBuilder sb, String value) {
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
                }
            }
        }
        sb.append('"');
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
                Object value = parseValue();
                result.put(key, value);
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
                Object value = parseValue();
                result.add(value);
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
                    if (index >= text.length()) {
                        throw new IllegalArgumentException("Invalid escape sequence");
                    }
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
                        default -> throw new IllegalArgumentException("Invalid escape character: " + esc);
                    });
                } else {
                    sb.append(ch);
                }
            }
            throw new IllegalArgumentException("Unterminated string");
        }

        private char parseUnicode() {
            if (index + 4 > text.length()) {
                throw new IllegalArgumentException("Incomplete unicode escape");
            }
            int value = 0;
            for (int i = 0; i < 4; i++) {
                char ch = text.charAt(index++);
                int digit = Character.digit(ch, 16);
                if (digit < 0) {
                    throw new IllegalArgumentException("Invalid hex digit in unicode escape");
                }
                value = (value << 4) | digit;
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
            if (peek('0')) {
                index++;
            } else {
                if (!Character.isDigit(peekChar())) {
                    throw new IllegalArgumentException("Invalid number");
                }
                while (Character.isDigit(peekChar())) {
                    index++;
                }
            }
            boolean isFloat = false;
            if (peek('.')) {
                isFloat = true;
                index++;
                if (!Character.isDigit(peekChar())) {
                    throw new IllegalArgumentException("Invalid fractional number");
                }
                while (Character.isDigit(peekChar())) {
                    index++;
                }
            }
            if (peek('e') || peek('E')) {
                isFloat = true;
                index++;
                if (peek('+') || peek('-')) {
                    index++;
                }
                if (!Character.isDigit(peekChar())) {
                    throw new IllegalArgumentException("Invalid exponent");
                }
                while (Character.isDigit(peekChar())) {
                    index++;
                }
            }
            String number = text.substring(start, index);
            try {
                if (!isFloat) {
                    long value = Long.parseLong(number);
                    if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
                        return (int) value;
                    }
                    return value;
                }
                return Double.parseDouble(number);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid number: " + number, ex);
            }
        }

        private void expect(char ch) {
            if (!peek(ch)) {
                throw new IllegalArgumentException("Expected '" + ch + "'");
            }
        }

        private boolean peek(char ch) {
            return index < text.length() && text.charAt(index) == ch;
        }

        private char peekChar() {
            return index < text.length() ? text.charAt(index) : '\0';
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
