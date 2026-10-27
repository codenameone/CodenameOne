/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.reel.packager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.stream.Stream;

/// Turns one source video into the protected renditions of a Reel title.
final class PackageJob {
    /// The facts about a packaged title, written to `info.json`.
    record Info(double duration, int width, int height, List<String> renditions, List<String> frameHashes) {
        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("durationSeconds", duration);
            m.put("width", width);
            m.put("height", height);
            m.put("renditions", renditions);
            m.put("schemes", List.of("AES_128", "CLEARKEY"));
            m.put("frameHashes", frameHashes);
            return m;
        }
    }

    Path input;
    Path out;
    String keyId;
    String key;
    String iv;
    int[] renditions = {720, 480, 360};
    int segment = 6;
    /// When positive only this many leading seconds of the source are used.
    int trimSeconds;
    IntConsumer progress = p -> { };

    private List<String> in() {
        List<String> a = new ArrayList<>();
        if (trimSeconds > 0) {
            a.add("-t");
            a.add(String.valueOf(trimSeconds));
        }
        a.add("-i");
        a.add(input.toString());
        return a;
    }

    private static int kbps(int h) {
        if (h >= 1080) {
            return 5000;
        } else if (h >= 720) {
            return 2800;
        } else if (h >= 480) {
            return 1400;
        } else if (h >= 360) {
            return 800;
        } else if (h >= 240) {
            return 400;
        }
        return Math.max(150, h * h / 160);
    }

    private static int even(double v) {
        return Math.max(2, (int) Math.round(v / 2.0) * 2);
    }

    /// Runs the whole packaging pipeline.
    Info run() throws IOException {
        if (!Hex.isHex(keyId, 32) || !Hex.isHex(key, 32) || !Hex.isHex(iv, 32)) {
            throw new IOException("--key-id, --key and --iv must each be 32 hex characters");
        }
        Files.createDirectories(out);
        Ffmpeg.Probe probe = Ffmpeg.probe(input);
        double duration = trimSeconds > 0 ? Math.min(probe.duration(), trimSeconds) : probe.duration();
        progress.accept(3);

        List<Integer> ladder = new ArrayList<>();
        for (int h : renditions) {
            if (h <= probe.height() && !ladder.contains(h)) {
                ladder.add(h);
            }
        }
        if (ladder.isEmpty()) {
            ladder.add(even(probe.height()));
        }
        ladder.sort(Comparator.reverseOrder());

        Path tmp = Files.createTempDirectory(Ffmpeg.tempRoot(), "pkg");
        try {
            Path keyFile = tmp.resolve("key.bin");
            Files.write(keyFile, Hex.decode(key));
            Path keyInfo = tmp.resolve("key.info");
            Files.writeString(keyInfo, "key\n" + keyFile + "\n" + iv + "\n", StandardCharsets.US_ASCII);

            StringBuilder master = new StringBuilder("#EXTM3U\n#EXT-X-VERSION:3\n");
            int step = 0;
            for (int h : ladder) {
                int w = even(probe.width() * (double) h / probe.height());
                int br = kbps(h);
                List<String> a = new ArrayList<>(in());
                a.addAll(videoArgs(h, br));
                a.addAll(List.of("-f", "hls", "-hls_time", String.valueOf(segment), "-hls_playlist_type", "vod",
                        "-hls_list_size", "0", "-start_number", "1",
                        "-hls_segment_filename", out.resolve("v" + h + "_%05d.ts").toString(),
                        "-hls_key_info_file", keyInfo.toString(), out.resolve("v" + h + ".m3u8").toString()));
                Ffmpeg.run(a);
                Path pl = out.resolve("v" + h + ".m3u8");
                String text = Files.readString(pl, StandardCharsets.UTF_8);
                Files.writeString(pl, text.replaceAll("URI=\"[^\"]*\"", "URI=\"key\""), StandardCharsets.UTF_8);
                master.append("#EXT-X-STREAM-INF:BANDWIDTH=").append((br + 128) * 1000)
                        .append(",RESOLUTION=").append(w).append('x').append(h)
                        .append(",CODECS=\"avc1.4d401f,mp4a.40.2\"\nv").append(h).append(".m3u8\n");
                step++;
                progress.accept(3 + 62 * step / ladder.size());
            }
            Files.deleteIfExists(out.resolve("key"));
            Files.writeString(out.resolve("master.m3u8"), master.toString(), StandardCharsets.UTF_8);

            int mid = ladder.get(ladder.size() / 2);
            List<String> c = new ArrayList<>(in());
            c.addAll(videoArgs(mid, kbps(mid)));
            c.addAll(List.of("-movflags", "frag_keyframe+empty_moov+default_base_moof",
                    "-encryption_scheme", "cenc-aes-ctr", "-encryption_key", key, "-encryption_kid", keyId,
                    "-f", "mp4", out.resolve("cenc.mp4").toString()));
            Ffmpeg.run(c);
            Mp4.injectPssh(out.resolve("cenc.mp4"), Hex.decode(keyId));
            progress.accept(80);

            List<String> p = new ArrayList<>();
            p.addAll(List.of("-ss", String.format(java.util.Locale.ROOT, "%.3f", duration * 0.1)));
            p.addAll(in());
            p.addAll(List.of("-frames:v", "1", "-vf", "scale=1280:-2", "-q:v", "3", out.resolve("poster.jpg").toString()));
            Ffmpeg.run(p);
            progress.accept(84);

            List<String> s = new ArrayList<>(in());
            s.addAll(List.of("-an", "-vf",
                    String.format(java.util.Locale.ROOT, "fps=100/%.3f,scale=160:90:force_original_aspect_ratio=decrease,"
                            + "pad=160:90:(ow-iw)/2:(oh-ih)/2,tile=10x10", duration),
                    "-frames:v", "1", "-q:v", "5", out.resolve("sprite.jpg").toString()));
            Ffmpeg.run(s);
            progress.accept(92);

            List<String> hl = new ArrayList<>(in());
            hl.addAll(List.of("-an", "-fps_mode", "passthrough", "-vf",
                    "select='isnan(prev_selected_t)+gte(t-prev_selected_t,10)',scale=9:8:flags=area,format=gray",
                    "-f", "rawvideo", "-"));
            byte[] raw = Ffmpeg.capture(hl);
            List<String> hashes = new ArrayList<>();
            for (int f = 0; f + 72 <= raw.length; f += 72) {
                hashes.add(dHash(raw, f));
            }
            Files.writeString(out.resolve("hashes.txt"), hashes.isEmpty() ? "" : String.join("\n", hashes) + "\n", StandardCharsets.US_ASCII);

            List<String> names = new ArrayList<>();
            for (int h : ladder) {
                names.add(String.valueOf(h));
            }
            Info info = new Info(duration, probe.width(), probe.height(), names, hashes);
            Files.writeString(out.resolve("info.json"), Json.write(info.toMap()), StandardCharsets.UTF_8);
            progress.accept(98);
            return info;
        } finally {
            try (Stream<Path> walk = Files.walk(tmp)) {
                walk.sorted(Comparator.reverseOrder()).forEach(x -> x.toFile().delete());
            }
        }
    }

    private List<String> videoArgs(int h, int br) {
        return Arrays.asList("-map", "0:v:0", "-map", "0:a:0?", "-vf", "scale=-2:" + h + ",format=yuv420p",
                "-c:v", "libx264", "-preset", "veryfast", "-profile:v", "main", "-b:v", br + "k",
                "-maxrate", (br * 107 / 100) + "k", "-bufsize", (br * 2) + "k", "-sc_threshold", "0",
                "-force_key_frames", "expr:gte(t,n_forced*" + segment + ")",
                "-c:a", "aac", "-b:a", "128k", "-ac", "2");
    }

    /// 64-bit difference hash of a 9x8 gray frame starting at `off`.
    static String dHash(byte[] g, int off) {
        long bits = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int l = g[off + y * 9 + x] & 0xff;
                int r = g[off + y * 9 + x + 1] & 0xff;
                bits = (bits << 1) | (l > r ? 1 : 0);
            }
        }
        return String.format("%016x", bits);
    }
}
