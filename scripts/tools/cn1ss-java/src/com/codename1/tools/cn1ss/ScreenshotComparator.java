package com.codename1.tools.cn1ss;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

final class ScreenshotComparator {
    private static final int MAX_COMMENT_BASE64 = 60_000;
    private static final float[] SCALES = new float[] {1.0f, 0.7f, 0.5f, 0.35f, 0.25f};
    private static final int[] JPEG_QUALITIES = new int[] {70, 60, 50, 40, 30, 20, 10};

    private ScreenshotComparator() {
    }

    static ComparisonReport buildReport(
            Path referenceDir,
            List<ActualEntry> actualEntries,
            boolean emitBase64,
            Path previewDir,
            Path previewSourceDir
    ) throws IOException {
        List<Map<String, Object>> results = new ArrayList<>();
        for (ActualEntry entry : actualEntries) {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("test", entry.testName);
            record.put("actual_path", entry.path.toString());
            Path expected = referenceDir.resolve(entry.testName + ".png");
            record.put("expected_path", expected.toString());

            if (!Files.exists(entry.path)) {
                record.put("status", "missing_actual");
                record.put("message", "Actual screenshot not found");
                results.add(record);
                continue;
            }

            if (!Files.exists(expected)) {
                record.put("status", "missing_expected");
                if (emitBase64) {
                    CommentPayload payload = loadExternalPreview(entry.testName, previewSourceDir);
                    if (payload == null) {
                        payload = buildCommentPayload(readImage(entry.path));
                    }
                    recordPayload(record, payload, entry.path.getFileName().toString(), previewDir);
                }
                results.add(record);
                continue;
            }

            try {
                BufferedImage actual = readImage(entry.path);
                BufferedImage expectedImage = readImage(expected);
                ComparisonDetails details = compare(expectedImage, actual);
                if (details.equal) {
                    record.put("status", "equal");
                } else {
                    record.put("status", "different");
                    record.put("details", details.toMap());
                    if (emitBase64) {
                        CommentPayload payload = loadExternalPreview(entry.testName, previewSourceDir);
                        if (payload == null) {
                            payload = buildCommentPayload(actual);
                        }
                        recordPayload(record, payload, entry.path.getFileName().toString(), previewDir);
                    }
                }
            } catch (Exception ex) {
                record.put("status", "error");
                record.put("message", ex.getMessage());
            }
            results.add(record);
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("results", results);
        return new ComparisonReport(results, payload);
    }

    private static BufferedImage readImage(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);
        try (ByteArrayInputStream in = new ByteArrayInputStream(data)) {
            BufferedImage image = ImageIO.read(in);
            if (image == null) {
                throw new IOException("Unsupported image format: " + path);
            }
            if (image.getType() == BufferedImage.TYPE_INT_ARGB) {
                return image;
            }
            BufferedImage converted = new BufferedImage(
                    image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g = converted.createGraphics();
            try {
                g.drawImage(image, 0, 0, null);
            } finally {
                g.dispose();
            }
            return converted;
        }
    }

    private static ComparisonDetails compare(BufferedImage expected, BufferedImage actual) {
        boolean equal = expected.getWidth() == actual.getWidth()
                && expected.getHeight() == actual.getHeight();
        int width = actual.getWidth();
        int height = actual.getHeight();

        if (equal) {
            outer:
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (expected.getRGB(x, y) != actual.getRGB(x, y)) {
                        equal = false;
                        break outer;
                    }
                }
            }
        }

        int bitDepth = estimateBitDepth(actual.getColorModel());
        int colorType = actual.getColorModel().getColorSpace().getType();
        return new ComparisonDetails(equal, width, height, bitDepth, colorType);
    }

    private static int estimateBitDepth(ColorModel model) {
        if (model == null) {
            return 8;
        }
        int components = Math.max(1, model.getNumComponents());
        int bits = model.getPixelSize();
        if (components > 0 && bits > 0) {
            return Math.max(1, bits / components);
        }
        return Math.max(1, model.getComponentSize(0));
    }

    private static CommentPayload loadExternalPreview(String testName, Path previewSourceDir) throws IOException {
        if (previewSourceDir == null) {
            return null;
        }
        if (!Files.isDirectory(previewSourceDir)) {
            return null;
        }
        String slug = slugify(testName);
        Path[] candidates = new Path[] {
                previewSourceDir.resolve(slug + ".jpg"),
                previewSourceDir.resolve(slug + ".jpeg"),
                previewSourceDir.resolve(slug + ".png"),
        };
        for (Path candidate : candidates) {
            if (!Files.exists(candidate)) {
                continue;
            }
            byte[] data = Files.readAllBytes(candidate);
            String encoded = Base64.getEncoder().encodeToString(data);
            if (encoded.length() <= MAX_COMMENT_BASE64) {
                return new CommentPayload(encoded, encoded.length(), mimeFor(candidate), codecFor(candidate), null, "Preview provided by instrumentation", data);
            }
            return new CommentPayload(null, encoded.length(), mimeFor(candidate), codecFor(candidate), null, "Preview provided by instrumentation", data, "too_large");
        }
        return null;
    }

    private static String mimeFor(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "image/png";
    }

    private static String codecFor(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "jpeg";
        }
        return "png";
    }

    private static CommentPayload buildCommentPayload(BufferedImage image) throws IOException {
        CommentPayload fallback = buildPngPayload(image, null);
        BufferedImage rgb = convertToRgb(image);
        for (float scale : SCALES) {
            BufferedImage candidate = rgb;
            if (scale < 0.999f) {
                int width = Math.max(1, Math.round(rgb.getWidth() * scale));
                int height = Math.max(1, Math.round(rgb.getHeight() * scale));
                candidate = resize(rgb, width, height);
            }
            for (int quality : JPEG_QUALITIES) {
                byte[] data;
                try {
                    data = encodeJpeg(candidate, quality / 100f);
                } catch (IOException ex) {
                    continue;
                }
                String encoded = Base64.getEncoder().encodeToString(data);
                if (encoded.length() <= MAX_COMMENT_BASE64) {
                    String note = "JPEG preview quality " + quality;
                    if (scale < 0.999f) {
                        note += "; downscaled to " + candidate.getWidth() + "x" + candidate.getHeight();
                    }
                    return new CommentPayload(encoded, encoded.length(), "image/jpeg", "jpeg", quality, note, data);
                }
                fallback = new CommentPayload(null, encoded.length(), "image/jpeg", "jpeg", quality, "All JPEG previews exceeded limit", data, "too_large");
            }
        }
        return fallback;
    }

    private static CommentPayload buildPngPayload(BufferedImage image, String note) throws IOException {
        byte[] data = encodePng(image);
        String encoded = Base64.getEncoder().encodeToString(data);
        if (encoded.length() <= MAX_COMMENT_BASE64) {
            return new CommentPayload(encoded, encoded.length(), "image/png", "png", null, note, data);
        }
        return new CommentPayload(null, encoded.length(), "image/png", "png", null, note, data, "too_large");
    }

    private static BufferedImage convertToRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) {
            return src;
        }
        BufferedImage image = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(src, 0, 0, null);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static BufferedImage resize(BufferedImage src, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, src.getType());
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(src, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        var writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("No JPEG encoder available");
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageOutputStream stream = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(Math.max(0.05f, Math.min(1.0f, quality)));
            }
            writer.write(null, new IIOImage(image, null, null), param);
            return baos.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private static byte[] encodePng(BufferedImage image) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return baos.toByteArray();
        }
    }

    private static void recordPayload(
            Map<String, Object> record,
            CommentPayload payload,
            String defaultName,
            Path previewDir
    ) throws IOException {
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
            String base = slugify(defaultName.replaceFirst("\\.[^.]+$", ""));
            Path target = previewDir.resolve(base + suffix);
            Files.write(target, payload.data);
            Map<String, Object> preview = new LinkedHashMap<>();
            preview.put("path", target.toString());
            preview.put("name", target.getFileName().toString());
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

    static String slugify(String name) {
        StringBuilder builder = new StringBuilder(name.length());
        for (char ch : name.toCharArray()) {
            if (Character.isLetterOrDigit(ch)) {
                builder.append(ch);
            } else {
                builder.append('_');
            }
        }
        return builder.toString();
    }

    static final class ActualEntry {
        final String testName;
        final Path path;

        ActualEntry(String testName, Path path) {
            this.testName = testName;
            this.path = path;
        }
    }

    static final class CommentPayload {
        final String base64;
        final int base64Length;
        final String mime;
        final String codec;
        final Integer quality;
        final String note;
        final byte[] data;
        final String omittedReason;

        CommentPayload(String base64, int base64Length, String mime, String codec, Integer quality, String note, byte[] data) {
            this(base64, base64Length, mime, codec, quality, note, data, null);
        }

        CommentPayload(String base64, int base64Length, String mime, String codec, Integer quality, String note, byte[] data, String omittedReason) {
            this.base64 = base64;
            this.base64Length = base64Length;
            this.mime = mime;
            this.codec = codec;
            this.quality = quality;
            this.note = note;
            this.data = data;
            this.omittedReason = omittedReason;
        }
    }

    private static final class ComparisonDetails {
        final boolean equal;
        final int width;
        final int height;
        final int bitDepth;
        final int colorType;

        ComparisonDetails(boolean equal, int width, int height, int bitDepth, int colorType) {
            this.equal = equal;
            this.width = width;
            this.height = height;
            this.bitDepth = bitDepth;
            this.colorType = colorType;
        }

        Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("equal", equal);
            map.put("width", width);
            map.put("height", height);
            map.put("bit_depth", bitDepth);
            map.put("color_type", colorType);
            return map;
        }
    }

    static final class ComparisonReport {
        private final List<Map<String, Object>> results;
        private final Map<String, Object> payload;

        ComparisonReport(List<Map<String, Object>> results, Map<String, Object> payload) {
            this.results = Collections.unmodifiableList(results);
            this.payload = payload;
        }

        List<Map<String, Object>> results() {
            return results;
        }

        Map<String, Object> payload() {
            return payload;
        }

        String toJson() {
            return Json.stringify(payload);
        }
    }
}
