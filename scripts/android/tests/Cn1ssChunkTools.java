import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cn1ssChunkTools {
    private static final String DEFAULT_TEST_NAME = "default";
    private static final String DEFAULT_CHANNEL = "";
    private static final Pattern CHUNK_PATTERN = Pattern.compile(
            "CN1SS(?:(?<channel>[A-Z]+))?:(?:(?<test>[A-Za-z0-9_.-]+):)?(?<index>\\d{6}):(?<payload>.*)");

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
            default -> {
                usage();
                System.exit(2);
            }
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
                result.add(new Chunk(test, channel, index, payload));
            }
        }
        return result;
    }

    private static void usage() {
        System.err.println("Usage: java Cn1ssChunkTools.java <command> [options]");
        System.err.println("Commands: count, extract, tests");
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
