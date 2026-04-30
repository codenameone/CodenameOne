import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cn1ssChunkTools {
    private static final String DEFAULT_TEST_NAME = "default";
    private static final String DEFAULT_CHANNEL = "";
    private static final Pattern CHUNK_PATTERN = Pattern.compile(
            "CN1SS(?:(?<channel>[A-Z]+))?:(?:(?<test>[A-Za-z0-9_.-]+):)?(?<index>\\d{6,}):(?<payload>.*)");

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(2);
        }
        String command = args[0];
        switch (command) {
            case "count" -> runCount(slice(args, 1));
            case "extract" -> runExtract(slice(args, 1));
            case "tests" -> runTests(slice(args, 1));
            case "check" -> runCheck(slice(args, 1));
            default -> {
                usage();
                System.exit(2);
            }
        }
    }

    private static void runCheck(String[] args) throws IOException {
        boolean error = false;
        Path path = Path.of(args[0]);
        String text = Files.readString(path, StandardCharsets.UTF_8);
        String[] lines = text.split("\r?\n");
        for(String line : lines) {
            Matcher matcher = CHUNK_PATTERN.matcher(line);
            if(line.contains("CN1SS:") && !matcher.find()) {
                error = error || line.indexOf(":ERR:") > -1;
                System.out.println(line);
            }
        }
        if(error) {
            System.exit(1);
        }
    }

    private static void runCount(String[] args) throws IOException {
        Path path = null;
        String test = null;
        String channel = DEFAULT_CHANNEL;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--test" -> {
                    if (++i >= args.length) {
                        throw new IllegalArgumentException("Missing value for --test");
                    }
                    test = args[i];
                }
                case "--channel" -> {
                    if (++i >= args.length) {
                        throw new IllegalArgumentException("Missing value for --channel");
                    }
                    channel = args[i];
                }
                default -> {
                    if (path != null) {
                        throw new IllegalArgumentException("Multiple paths provided");
                    }
                    path = Path.of(args[i]);
                }
            }
        }
        if (path == null) {
            throw new IllegalArgumentException("Path is required for count");
        }
        int count = 0;
        for (Chunk chunk : iterateChunks(path, Optional.ofNullable(test), Optional.ofNullable(channel))) {
            count++;
        }
        System.out.println(count);
    }

    private static void runExtract(String[] args) throws IOException {
        Path path = null;
        boolean decode = false;
        String test = null;
        String channel = DEFAULT_CHANNEL;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--decode" -> decode = true;
                case "--test" -> {
                    if (++i >= args.length) {
                        throw new IllegalArgumentException("Missing value for --test");
                    }
                    test = args[i];
                }
                case "--channel" -> {
                    if (++i >= args.length) {
                        throw new IllegalArgumentException("Missing value for --channel");
                    }
                    channel = args[i];
                }
                default -> {
                    if (path != null) {
                        throw new IllegalArgumentException("Multiple paths provided");
                    }
                    path = Path.of(args[i]);
                }
            }
        }
        if (path == null) {
            throw new IllegalArgumentException("Path is required for extract");
        }
        String targetTest = test != null ? test : DEFAULT_TEST_NAME;
        List<Chunk> chunks = new ArrayList<>();
        for (Chunk chunk : iterateChunks(path, Optional.ofNullable(targetTest), Optional.ofNullable(channel))) {
            chunks.add(chunk);
        }
        Collections.sort(chunks);

        // Each chunk's index is its byte offset within the emitted base64 stream
        // (Cn1ssDeviceRunnerHelper.emitChannel and the iOS Swift equivalent). A
        // valid stream covers offsets [0, totalLength) with no gaps. If a log line
        // gets dropped (logcat buffer overflow, line truncation, etc.) we'd
        // silently concatenate the surviving chunks and produce a short binary
        // that passes the magic-byte verifier but fails downstream parsers with
        // "PNG chunk truncated before CRC". Detect the gap here and refuse to
        // emit a partial stream.
        long expectedTotal = readTotalBase64Length(path, targetTest, channel);
        List<String> issues = new ArrayList<>();
        int expected = 0;
        for (Chunk chunk : chunks) {
            if (chunk.index != expected) {
                issues.add(chunk.index > expected
                        ? "missing " + (chunk.index - expected) + " base64 chars at offset "
                                + expected + " (next chunk starts at " + chunk.index + ")"
                        : "overlap of " + (expected - chunk.index) + " base64 chars at offset "
                                + chunk.index);
            }
            expected = chunk.index + chunk.payload.length();
        }
        if (expectedTotal >= 0 && expected != expectedTotal) {
            issues.add("reassembled length " + expected
                    + " does not match emitted total_b64_len=" + expectedTotal);
        }
        if (!issues.isEmpty()) {
            String channelLabel = channel == null || channel.isEmpty() ? "" : " (channel '" + channel + "')";
            System.err.println("ERROR: incomplete chunk stream for test '" + targetTest + "'"
                    + channelLabel + " in " + path + ":");
            for (String issue : issues) {
                System.err.println("  - " + issue);
            }
            System.err.println("  Got " + chunks.size() + " chunks covering "
                    + expected + " base64 chars"
                    + (expectedTotal >= 0 ? " of " + expectedTotal + " expected" : "")
                    + ". Refusing to emit a partial stream.");
            System.exit(1);
        }

        StringBuilder payload = new StringBuilder();
        for (Chunk chunk : chunks) {
            payload.append(chunk.payload);
        }
        if (decode) {
            byte[] data;
            try {
                data = Base64.getDecoder().decode(payload.toString());
            } catch (IllegalArgumentException ex) {
                data = new byte[0];
            }
            System.out.write(data);
        } else {
            System.out.print(payload.toString());
        }
    }

    /**
     * Returns the total base64 length advertised by the emitter for the given
     * test/channel, or -1 if no matching INFO line was found. The emitter logs
     * `CN1SS:INFO:test=<name> chunks=<n> total_b64_len=<len>` once it has
     * finished writing all chunks; matching against this gives us a definitive
     * "did we receive everything" check independent of chunk-index continuity.
     */
    private static long readTotalBase64Length(Path path, String testName, String channel) throws IOException {
        // The INFO line is always emitted on the default channel regardless of
        // whether the chunks themselves go to a side channel like PREVIEW, so
        // we only filter by test name here.
        String text = Files.readString(path, StandardCharsets.UTF_8);
        Pattern info = Pattern.compile(
                "CN1SS:INFO:test=" + Pattern.quote(testName)
                        + "\\b[^\\n]*?\\btotal_b64_len=(\\d+)");
        Matcher m = info.matcher(text);
        long latest = -1;
        // The same test may emit multiple channels (PNG + PREVIEW). Without a
        // channel marker on the INFO line we can't disambiguate, so we only
        // trust the value when there is exactly one. If channel is non-empty
        // (PREVIEW) we conservatively skip the length check rather than risk
        // a false positive against the PNG total.
        if (channel != null && !channel.isEmpty()) {
            return -1;
        }
        int count = 0;
        while (m.find()) {
            count++;
            try {
                latest = Long.parseLong(m.group(1));
            } catch (NumberFormatException ignored) {
                return -1;
            }
        }
        return count == 1 ? latest : -1;
    }

    private static void runTests(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("tests command requires a path argument");
        }
        Path path = Path.of(args[0]);
        List<String> names = new ArrayList<>();
        for (Chunk chunk : iterateChunks(path, Optional.empty(), Optional.of(DEFAULT_CHANNEL))) {
            if (!names.contains(chunk.testName)) {
                names.add(chunk.testName);
            }
        }
        Collections.sort(names);
        for (String name : names) {
            System.out.println(name);
        }
    }

    private static Iterable<Chunk> iterateChunks(Path path, Optional<String> testFilter, Optional<String> channelFilter) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        List<Chunk> result = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        String[] lines = text.split("\r?\n");
        for (String line : lines) {
            Matcher matcher = CHUNK_PATTERN.matcher(line);
            if (!matcher.find()) {
                continue;
            }
            String test = Optional.ofNullable(matcher.group("test")).orElse(DEFAULT_TEST_NAME);
            if (testFilter.isPresent() && !test.equals(testFilter.get())) {
                continue;
            }
            String channel = Optional.ofNullable(matcher.group("channel")).orElse(DEFAULT_CHANNEL);
            if (channelFilter.isPresent() && !channel.equals(channelFilter.get())) {
                continue;
            }
            int index = Integer.parseInt(matcher.group("index"));
            String payload = matcher.group("payload").replaceAll("[^A-Za-z0-9+/=]", "");
            if (!payload.isEmpty()) {
                String dedupeKey = test + '\u0000' + channel + '\u0000' + index + '\u0000' + payload;
                if (seen.add(dedupeKey)) {
                    result.add(new Chunk(test, channel, index, payload));
                }
            }
        }
        return result;
    }

    private static void usage() {
        System.err.println("Usage: java Cn1ssChunkTools.java <command> [options]");
        System.err.println("Commands: count, extract, tests, check");
    }

    private static String[] slice(String[] args, int from) {
        String[] out = new String[args.length - from];
        System.arraycopy(args, from, out, 0, out.length);
        return out;
    }

    private static final class Chunk implements Comparable<Chunk> {
        final String testName;
        final String channel;
        final int index;
        final String payload;

        Chunk(String testName, String channel, int index, String payload) {
            this.testName = testName;
            this.channel = channel;
            this.index = index;
            this.payload = payload;
        }

        @Override
        public int compareTo(Chunk other) {
            int cmp = Integer.compare(this.index, other.index);
            if (cmp != 0) {
                return cmp;
            }
            return this.payload.compareTo(other.payload);
        }
    }
}
