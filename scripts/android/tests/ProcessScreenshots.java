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

    public static void main(String[] args) throws Exception {
        Arguments arguments = Arguments.parse(args);
        if (arguments == null) {
            System.exit(2);
            return;
        }
        Map<String, Object> payload = buildResults(
                arguments.referenceDir,
                arguments.actualEntries,
                arguments.emitBase64,
                arguments.previewDir
        );
        String json = JsonUtil.stringify(payload);
        System.out.print(json);
    }

    static Map<String, Object> buildResults(
            Path referenceDir,
            List<Map.Entry<String, Path>> actualEntries,
            boolean emitBase64,
            Path previewDir
    ) throws IOException {
        List<Map<String, Object>> results = new ArrayList<>();
        for (Map.Entry<String, Path> entry : actualEntries) {
            String testName = entry.getKey();
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
                    CommentPayload payload = loadPreviewOrBuild(testName, actualPath, previewDir);
                    recordPayload(record, payload, actualPath.getFileName().toString(), previewDir);
                }
            } else {
                try {
                    PNGImage actual = loadPng(actualPath);
                    PNGImage expected = loadPng(expectedPath);
                    Map<String, Object> outcome = compareImages(expected, actual);
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
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("results", results);
        return payload;
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
        PNGImage image = cached != null ? cached : loadPng(actualPath);
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

    private static Map<String, Object> compareImages(PNGImage expected, PNGImage actual) {
        boolean equal = expected.width == actual.width
                && expected.height == actual.height
                && expected.bitDepth == actual.bitDepth
                && expected.colorType == actual.colorType
                && java.util.Arrays.equals(expected.pixels, actual.pixels);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("equal", equal);
        result.put("width", actual.width);
        result.put("height", actual.height);
        result.put("bit_depth", actual.bitDepth);
        result.put("color_type", actual.colorType);
        return result;
    }

    private static PNGImage loadPng(Path path) throws IOException {
        byte[] data = Files.readAllBytes(path);
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (data[i] != PNG_SIGNATURE[i]) {
                throw new IOException(path + " is not a PNG file (missing signature)");
            }
        }
        int offset = PNG_SIGNATURE.length;
        int width = 0;
        int height = 0;
        int bitDepth = 0;
        int colorType = 0;
        int interlace = 0;
        List<byte[]> idatChunks = new ArrayList<>();
        while (offset + 8 <= data.length) {
            int length = readInt(data, offset);
            byte[] type = java.util.Arrays.copyOfRange(data, offset + 4, offset + 8);
            offset += 8;
            if (offset + length + 4 > data.length) {
                throw new IOException("PNG chunk truncated before CRC");
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
                    throw new IOException("Unsupported PNG compression or filter method");
                }
            } else if ("IDAT".equals(chunkType)) {
                idatChunks.add(chunkData);
            } else if ("IEND".equals(chunkType)) {
                break;
            }
        }
        if (width <= 0 || height <= 0) {
            throw new IOException("Missing IHDR chunk");
        }
        if (interlace != 0) {
            throw new IOException("Interlaced PNGs are not supported");
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

        private Arguments(Path referenceDir, List<Map.Entry<String, Path>> actualEntries, boolean emitBase64, Path previewDir) {
            this.referenceDir = referenceDir;
            this.actualEntries = actualEntries;
            this.emitBase64 = emitBase64;
            this.previewDir = previewDir;
        }

        static Arguments parse(String[] args) {
            Path reference = null;
            boolean emitBase64 = false;
            Path previewDir = null;
            List<Map.Entry<String, Path>> actuals = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                switch (arg) {
                    case "--reference-dir" -> {
                        if (++i >= args.length) {
                            System.err.println("Missing value for --reference-dir");
                            return null;
                        }
                        reference = Path.of(args[i]);
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
            return new Arguments(reference, actuals, emitBase64, previewDir);
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

