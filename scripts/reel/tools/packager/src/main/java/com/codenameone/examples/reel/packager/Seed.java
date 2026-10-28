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
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/// Prepares the offline demo catalog: Blender films, generated lessons and ads.
final class Seed {
    /// `posterSeconds` is a frame picked by hand for the poster: the packager's
    /// default, a tenth of the way in, lands on the opening titles of every film,
    /// and always does for a `--small` catalog, which keeps only the first minute.
    private record Film(String code, String slug, String url, double posterSeconds) {
        String fileName() {
            String n = url.substring(url.lastIndexOf('/') + 1);
            return n.endsWith(".zip") ? n.substring(0, n.length() - 4) : n;
        }
    }

    private static final String BLENDER = "https://download.blender.org/";
    private static final List<Film> FILMS = List.of(
            new Film("bbb", "big-buck-bunny", BLENDER + "peach/bigbuckbunny_movies/big_buck_bunny_480p_h264.mov.zip", 108),
            new Film("sintel", "sintel", BLENDER + "durian/movies/Sintel.2010.720p.mkv.zip", 202),
            new Film("tos", "tears-of-steel", BLENDER + "demo/movies/ToS/tears_of_steel_720p.mov", 308),
            new Film("ed", "elephants-dream", BLENDER + "ED/elephantsdream-480-h264-st-aac.mov", 162));

    private static final String[] COLORS = {"0x1b3a5c", "0x3c1b5c", "0x1b5c3a"};
    private static final String[][] LESSONS = {
        {"Welcome to Reel Academy", "Streams are cut into small segments", "Each segment is encrypted"},
        {"Keys and licences", "The player asks the server for a key", "Only entitled viewers get one"},
        {"Watching offline", "Downloads keep their encryption", "Licences expire on schedule"}};

    private final Path media;
    private final Path sources;
    private final boolean small;
    private final SecureRandom rnd = new SecureRandom();
    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();

    Seed(Path media, boolean small) {
        this.media = media.toAbsolutePath();
        this.sources = this.media.getParent().resolve("sources");
        this.small = small;
    }

    private String randomHex() {
        byte[] b = new byte[16];
        rnd.nextBytes(b);
        return Hex.encode(b);
    }

    private boolean intact(String slug) {
        Path d = media.resolve(slug);
        for (String f : new String[] {"master.m3u8", "cenc.mp4", "poster.jpg", "sprite.jpg", "info.json", "hashes.txt"}) {
            if (!Files.isRegularFile(d.resolve(f))) {
                return false;
            }
        }
        return true;
    }

    /// Builds or refreshes `seed.json` under the media directory.
    void run(List<String> filmCodes) throws IOException {
        Files.createDirectories(media);
        Files.createDirectories(sources);
        Map<String, Map<String, Object>> entries = new LinkedHashMap<>();
        Path seedJson = media.resolve("seed.json");
        if (Files.isRegularFile(seedJson)) {
            try {
                Object vs = Json.parseObject(Files.readString(seedJson, StandardCharsets.UTF_8)).get("videos");
                if (vs instanceof List<?> l) {
                    for (Object o : l) {
                        if (o instanceof Map<?, ?> m && m.get("slug") instanceof String s && intact(s)) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> e = (Map<String, Object>) m;
                            entries.put(s, e);
                        }
                    }
                }
            } catch (RuntimeException e) {
                System.err.println("warning: ignoring unreadable seed.json: " + e.getMessage());
            }
        }
        boolean drawtext = Ffmpeg.hasFilter("drawtext");
        if (!drawtext) {
            System.err.println("note: ffmpeg has no drawtext filter; using testsrc2 overlays for generated clips");
        }
        List<String> slugs = new ArrayList<>();
        Map<String, Path> inputs = new LinkedHashMap<>();
        for (Film f : FILMS) {
            if (filmCodes.contains(f.code())) {
                slugs.add(f.slug());
                if (!entries.containsKey(f.slug())) {
                    inputs.put(f.slug(), fetch(f));
                }
            }
        }
        for (int i = 1; i <= 3; i++) {
            String slug = "academy-lesson-" + i;
            slugs.add(slug);
            if (!entries.containsKey(slug)) {
                Path p = sources.resolve(slug + ".mp4");
                if (!Files.isRegularFile(p)) {
                    generate(p, 45, COLORS[i - 1], "Academy Lesson " + i, LESSONS[i - 1], 220 + 110 * i, drawtext);
                }
                inputs.put(slug, p);
            }
        }
        String[][] ads = {{"ad-coffee", "Fresh Coffee", "0x5c3a1b"}, {"ad-bike", "Ride More", "0x1b5c5c"}};
        for (String[] ad : ads) {
            slugs.add(ad[0]);
            if (!entries.containsKey(ad[0])) {
                Path p = sources.resolve(ad[0] + ".mp4");
                if (!Files.isRegularFile(p)) {
                    generate(p, 15, ad[2], ad[1], new String[] {ad[1]}, 440, drawtext);
                }
                inputs.put(ad[0], p);
            }
        }
        for (Map.Entry<String, Path> in : inputs.entrySet()) {
            if (in.getValue() == null) {
                continue;
            }
            String slug = in.getKey();
            Path dir = media.resolve(slug);
            deleteTree(dir);
            PackageJob pj = new PackageJob();
            pj.input = in.getValue();
            pj.out = dir;
            pj.keyId = randomHex();
            pj.key = randomHex();
            pj.iv = randomHex();
            pj.trimSeconds = small && FILMS.stream().anyMatch(f -> f.slug().equals(slug)) ? 60 : 0;
            System.out.println("packaging " + slug);
            PackageJob.Info info = pj.run();
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("slug", slug);
            e.put("keyId", pj.keyId);
            e.put("keyHex", pj.key);
            e.put("ivHex", pj.iv);
            e.putAll(info.toMap());
            entries.put(slug, e);
            writeSeed(seedJson, slugs, entries);
        }
        writeSeed(seedJson, slugs, entries);
        for (Film f : FILMS) {
            Path source = sources.resolve(f.fileName());
            Path dir = media.resolve(f.slug());
            if (filmCodes.contains(f.code()) && Files.isRegularFile(source) && Files.isDirectory(dir)) {
                // One frame from the full film, so it is cheap to redo on every run
                // and a catalog packaged before the frame was chosen picks it up.
                Ffmpeg.run(List.of("-ss", String.valueOf(f.posterSeconds()), "-i", source.toString(),
                        "-frames:v", "1", "-vf", "scale=1280:-2", "-q:v", "3",
                        dir.resolve("poster.jpg").toString()));
            }
        }
        System.out.println("seed.json lists " + entries.size() + " videos");
    }

    private void writeSeed(Path file, List<String> order, Map<String, Map<String, Object>> entries) throws IOException {
        List<Object> list = new ArrayList<>();
        for (String s : order) {
            if (entries.containsKey(s)) {
                list.add(entries.get(s));
            }
        }
        for (Map.Entry<String, Map<String, Object>> e : entries.entrySet()) {
            if (!order.contains(e.getKey())) {
                list.add(e.getValue());
            }
        }
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("videos", list);
        Files.writeString(file, Json.write(root), StandardCharsets.UTF_8);
    }

    private static void deleteTree(Path d) throws IOException {
        if (Files.exists(d)) {
            try (Stream<Path> w = Files.walk(d)) {
                w.sorted(Comparator.reverseOrder()).forEach(x -> x.toFile().delete());
            }
        }
    }

    /// Returns the downloaded source, or null (with a warning) when the download failed.
    private Path fetch(Film f) {
        Path target = sources.resolve(f.fileName());
        if (Files.isRegularFile(target)) {
            return target;
        }
        Path part = sources.resolve(f.fileName() + ".part");
        try {
            System.out.println("downloading " + f.url());
            HttpResponse<Path> r = http.send(HttpRequest.newBuilder(URI.create(f.url())).GET().build(),
                    HttpResponse.BodyHandlers.ofFile(part));
            if (r.statusCode() != 200) {
                throw new IOException("HTTP " + r.statusCode());
            }
            if (f.url().endsWith(".zip")) {
                unzip(part, f.fileName(), target);
                Files.deleteIfExists(part);
            } else {
                Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return target;
        } catch (IOException e) {
            System.err.println("warning: could not fetch " + f.slug() + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            Files.deleteIfExists(part);
        } catch (IOException e) {
            // best effort
        }
        return null;
    }

    private static void unzip(Path zip, String wanted, Path target) throws IOException {
        try (InputStream raw = Files.newInputStream(zip); ZipInputStream zin = new ZipInputStream(raw)) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (!e.isDirectory() && e.getName().endsWith(wanted)) {
                    Path tmp = target.resolveSibling(target.getFileName() + ".part");
                    Files.copy(zin, tmp, StandardCopyOption.REPLACE_EXISTING);
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                    return;
                }
            }
        }
        throw new IOException(wanted + " not found in " + zip.getFileName());
    }

    private static String fontOption() {
        for (String f : new String[] {"/System/Library/Fonts/Helvetica.ttc", "/Library/Fonts/Arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"}) {
            if (Files.isRegularFile(Path.of(f))) {
                return ":fontfile=" + f;
            }
        }
        return "";
    }

    /// Generates a slide clip: a colour background with one slide per 15 seconds.
    static void generate(Path out, int seconds, String color, String title, String[] bullets, int tone,
            boolean drawtext) throws IOException {
        int slides = Math.max(1, seconds / 15);
        StringBuilder vf = new StringBuilder();
        String src;
        if (drawtext) {
            src = "color=c=" + color + ":s=1280x720:r=25:d=" + seconds;
            String font = fontOption();
            for (int s = 0; s < slides; s++) {
                String when = ":enable='between(t," + (s * 15) + "," + (s * 15 + 15) + ")'";
                if (vf.length() > 0) {
                    vf.append(',');
                }
                vf.append("drawtext=text='").append(title).append("':fontcolor=white:fontsize=72:x=(w-text_w)/2:y=160")
                        .append(font).append(when);
                String b = bullets[Math.min(s, bullets.length - 1)];
                if (bullets.length > 1 || s > 0) {
                    vf.append(",drawtext=text='").append(b).append("':fontcolor=yellow:fontsize=48:x=(w-text_w)/2:y=360")
                            .append(font).append(when);
                }
            }
        } else {
            src = "testsrc2=s=1280x720:r=25:d=" + seconds;
            vf.append("drawbox=x=0:y=0:w=iw:h=ih:color=").append(color).append("@0.55:t=fill");
            for (int s = 0; s < slides; s++) {
                String when = ":enable='between(t," + (s * 15) + "," + (s * 15 + 15) + ")'";
                vf.append(",drawbox=x=40:y=600:w=").append(300 * (s + 1)).append(":h=40:color=yellow@0.9:t=fill").append(when);
            }
        }
        List<String> a = new ArrayList<>(List.of("-f", "lavfi", "-i", src, "-f", "lavfi", "-i",
                "sine=frequency=" + tone + ":sample_rate=44100:duration=" + seconds, "-vf", vf.toString(),
                "-af", "volume=0.15", "-c:v", "libx264", "-preset", "veryfast", "-pix_fmt", "yuv420p",
                "-c:a", "aac", "-shortest", out.toString()));
        Ffmpeg.run(a);
    }
}
