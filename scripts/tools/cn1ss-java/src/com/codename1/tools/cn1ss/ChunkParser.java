package com.codename1.tools.cn1ss;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities for parsing CN1SS chunk streams from simulator and instrumentation logs.
 */
final class ChunkParser {
    private static final Pattern CHUNK_PATTERN = Pattern.compile(
            "CN1SS(?:(?<channel>[A-Z]+))?:(?:(?<test>[A-Za-z0-9_.-]+):)?(?<index>\\d{6}):(?<payload>.*)"
    );

    private ChunkParser() {
    }

    static List<String> listTests(Path logFile) throws IOException {
        Set<String> tests = new LinkedHashSet<>();
        for (Chunk chunk : iterate(logFile, null, "")) {
            tests.add(chunk.testName);
        }
        List<String> sorted = new ArrayList<>(tests);
        Collections.sort(sorted);
        return sorted;
    }

    static byte[] decode(Path logFile, String testName, String channel) throws IOException {
        List<Chunk> chunks = iterate(logFile, testName, channel);
        if (chunks.isEmpty()) {
            return new byte[0];
        }
        Collections.sort(chunks, Comparator.comparingInt(c -> c.index));
        StringBuilder builder = new StringBuilder();
        for (Chunk chunk : chunks) {
            builder.append(chunk.payload);
        }
        String data = builder.toString();
        if (data.isEmpty()) {
            return new byte[0];
        }
        try {
            return Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException ex) {
            return new byte[0];
        }
    }

    private static List<Chunk> iterate(Path logFile, String testFilter, String channelFilter) throws IOException {
        List<Chunk> result = new ArrayList<>();
        for (String line : Files.readAllLines(logFile, StandardCharsets.UTF_8)) {
            Matcher matcher = CHUNK_PATTERN.matcher(line);
            if (!matcher.find()) {
                continue;
            }
            String test = matcher.group("test");
            if (test == null || test.isEmpty()) {
                test = "default";
            }
            if (testFilter != null && !test.equals(testFilter)) {
                continue;
            }
            String channel = matcher.group("channel");
            if (channel == null) {
                channel = "";
            }
            if (channelFilter != null && !channel.equals(channelFilter)) {
                continue;
            }
            String payload = matcher.group("payload");
            if (payload == null) {
                continue;
            }
            payload = payload.replaceAll("[^A-Za-z0-9+/=]", "");
            if (payload.isEmpty()) {
                continue;
            }
            int index = Integer.parseInt(matcher.group("index"));
            result.add(new Chunk(test, channel, index, payload));
        }
        return result;
    }

    private static final class Chunk {
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
    }
}
