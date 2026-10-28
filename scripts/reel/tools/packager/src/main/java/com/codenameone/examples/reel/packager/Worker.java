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
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/// Polls a Reel server for packaging jobs and uploads the results.
final class Worker {
    private final String server;
    private final String token;
    private final String name;
    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    Worker(String server, String token, String name) {
        this.server = server.endsWith("/") ? server.substring(0, server.length() - 1) : server;
        this.token = token;
        this.name = name;
    }

    /// Loops until interrupted, or processes at most one job when `once`.
    void run(boolean once) throws IOException, InterruptedException {
        while (true) {
            boolean worked = pollOnce();
            if (once) {
                return;
            }
            if (!worked) {
                Thread.sleep(5000);
            }
        }
    }

    private HttpRequest.Builder req(String path) {
        return HttpRequest.newBuilder(URI.create(server + path)).header("Authorization", "Bearer " + token);
    }

    private HttpResponse<String> post(String path, Object json) throws IOException, InterruptedException {
        String body = json == null ? "" : Json.write(json);
        return http.send(req(path).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    /// Claims and runs one job; returns false when there was none.
    boolean pollOnce() throws IOException, InterruptedException {
        HttpResponse<String> r = post("/worker/jobs/claim?worker=" + java.net.URLEncoder.encode(name, StandardCharsets.UTF_8), null);
        if (r.statusCode() == 204) {
            return false;
        }
        if (r.statusCode() != 200) {
            System.err.println("claim failed: HTTP " + r.statusCode());
            return false;
        }
        Map<String, Object> job = Json.parseObject(r.body());
        String id = String.valueOf(job.get("id"));
        System.out.println("job " + id + " (" + job.get("kind") + ")");
        Path tmp = Files.createTempDirectory(Ffmpeg.tempRoot(), "job");
        try {
            process(id, job, tmp);
        } catch (IOException | RuntimeException e) {
            String msg = String.valueOf(e.getMessage());
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("progressPercent", 0);
            f.put("status", "FAILED");
            f.put("error", msg.length() > 400 ? msg.substring(0, 400) : msg);
            post("/worker/jobs/" + id + "/fail", f);
            System.err.println("job " + id + " failed: " + msg);
        } finally {
            try (Stream<Path> walk = Files.walk(tmp)) {
                walk.sorted(Comparator.reverseOrder()).forEach(x -> x.toFile().delete());
            }
        }
        return true;
    }

    /// The server takes at most [#CHUNK] bytes a request (its request body
    /// cap is 8 MiB), so a file goes up in pieces, each saying where it
    /// starts; the server refuses a piece that does not continue the file.
    static final int CHUNK = 4 * 1024 * 1024;

    private void upload(String id, String name, Path f) throws IOException, InterruptedException {
        long size = Files.size(f);
        try (RandomAccessFile in = new RandomAccessFile(f.toFile(), "r")) {
            long offset = 0;
            do {
                byte[] piece = new byte[(int) Math.min(CHUNK, size - offset)];
                in.readFully(piece);
                HttpResponse<String> up = http.send(req("/worker/jobs/" + id + "/files/" + name + "?offset=" + offset)
                        .header("Content-Type", "application/octet-stream")
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(piece)).build(),
                        HttpResponse.BodyHandlers.ofString());
                if (up.statusCode() / 100 != 2) {
                    throw new IOException("upload of " + name + " at " + offset + " failed: HTTP " + up.statusCode());
                }
                offset += piece.length;
            } while (offset < size);
        }
    }

    private void progress(String id, int pct) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("progressPercent", pct);
        m.put("status", "RUNNING");
        m.put("error", "");
        try {
            post("/worker/jobs/" + id + "/progress", m);
        } catch (IOException e) {
            System.err.println("progress failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void process(String id, Map<String, Object> job, Path tmp) throws IOException, InterruptedException {
        String src = String.valueOf(job.get("sourceUrl"));
        Path source = tmp.resolve("source.bin");
        HttpResponse<Path> dl = http.send(req(src.startsWith("/") ? src : "/" + src).GET().build(),
                HttpResponse.BodyHandlers.ofFile(source));
        if (dl.statusCode() != 200) {
            throw new IOException("source download failed: HTTP " + dl.statusCode());
        }
        progress(id, 2);
        PackageJob pj = new PackageJob();
        pj.input = source;
        pj.out = tmp.resolve("out");
        pj.keyId = String.valueOf(job.get("keyId"));
        pj.key = String.valueOf(job.get("keyHex"));
        pj.iv = String.valueOf(job.get("ivHex"));
        if (job.get("renditions") instanceof List<?> l && !l.isEmpty()) {
            pj.renditions = l.stream().mapToInt(o -> Integer.parseInt(String.valueOf(o))).toArray();
        }
        if (job.get("segmentSeconds") instanceof Number n && n.intValue() > 0) {
            pj.segment = n.intValue();
        }
        int[] last = {-1};
        pj.progress = p -> {
            if (p / 20 != last[0]) {
                last[0] = p / 20;
                progress(id, Math.min(p, 95));
            }
        };
        PackageJob.Info info = pj.run();
        List<Path> files = new ArrayList<>();
        try (Stream<Path> s = Files.list(pj.out)) {
            s.sorted().forEach(files::add);
        }
        for (Path f : files) {
            String n = f.getFileName().toString();
            if (n.equals("info.json") || n.equals("hashes.txt")) {
                continue;
            }
            upload(id, n, f);
        }
        progress(id, 97);
        Map<String, Object> done = new LinkedHashMap<>();
        done.put("durationSeconds", info.duration());
        done.put("width", info.width());
        done.put("height", info.height());
        done.put("renditions", info.renditions());
        done.put("frameHashes", info.frameHashes());
        done.put("posterPath", "poster.jpg");
        done.put("spritePath", "sprite.jpg");
        HttpResponse<String> d = post("/worker/jobs/" + id + "/done", done);
        if (d.statusCode() / 100 != 2) {
            throw new IOException("done rejected: HTTP " + d.statusCode());
        }
        System.out.println("job " + id + " done");
    }
}
