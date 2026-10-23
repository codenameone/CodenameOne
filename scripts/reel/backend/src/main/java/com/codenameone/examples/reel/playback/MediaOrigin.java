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
package com.codenameone.examples.reel.playback;

import com.codenameone.examples.reel.domain.Video;
import com.codenameone.examples.reel.repository.VideoRepository;
import com.codename1.backend.HttpServer;
import com.codename1.backend.StaticFiles;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.annotations.Value;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/// The media origin: the packaged files of every video, behind a token.
///
/// `/media/{token}/{video}/{file}` serves `{reel.media.dir}/{video}/{file}`
/// when the token was signed for that video. The token is in the path, not a
/// header or a query, because a player resolves the segment and key names a
/// playlist lists against the playlist's own URL -- a path segment is the one
/// place a credential survives that on every player, and the portable choice
/// [com.codename1.media.StreamRequest] recommends.
///
/// Only the names the packager writes are served (never `info.json` or the
/// frame hashes), renditions above the token's height are refused, and the
/// master playlist is rewritten to list only what the token may play. Playlists
/// also check the session is still live, so ending a session stops the player
/// at its next playlist read. The HLS `key` is answered from `video_key`; it
/// is never a file. A CDN in front of the origin may cache segments and
/// `cenc.mp4` (they are encrypted) but must pass playlists and keys through.
///
/// Posters and scrub sprites are public at `/art/{video}/{file}`: a catalog
/// is browsed before anyone signs in.
@RestController
public class MediaOrigin {
    private final String root;
    private final MediaTokens tokens;
    private final Licenses licenses;
    private final Playbacks playbacks;
    private final VideoRepository videos;

    public MediaOrigin(@Value("${reel.media.dir:media}") String root, MediaTokens tokens, Licenses licenses,
            Playbacks playbacks, VideoRepository videos) {
        this.videos = videos;
        this.root = root;
        // A server with no packaged media yet (a fresh install, the test profile)
        // must answer 404 for art and segments, not fail every request on a
        // document root that does not exist.
        new File(root).mkdirs();
        this.tokens = tokens;
        this.licenses = licenses;
        this.playbacks = playbacks;
    }

    @GetMapping("/media/{token}/{video}/{file}")
    public HttpServer.Response media(HttpServer.Request request, @PathVariable("token") String token,
            @PathVariable("video") String video, @PathVariable("file") String file) throws Exception {
        MediaTokens.Grant grant = tokens.verifyMedia(token);
        if (grant == null || !grant.videoId.equals(video) || !safeId(video)) {
            return HttpServer.Response.text(403, "forbidden");
        }
        boolean playlist = file.endsWith(".m3u8");
        if ((playlist || "key".equals(file)) && !playbacks.isLive(grant)) {
            return HttpServer.Response.text(403, "session ended");
        }
        if ("key".equals(file)) {
            byte[] key = licenses.aesKey(video);
            if (key == null) {
                return HttpServer.Response.text(404, "not found");
            }
            return new HttpServer.Response(200, "application/octet-stream", key, noStore());
        }
        int height = renditionHeight(file);
        if ("cenc.mp4".equals(file)) {
            // One file at one quality, so the cap is all or nothing.
            height = Licenses.cencHeight(renditions(video));
        }
        if (height < 0) {
            return HttpServer.Response.text(404, "not found");
        }
        if (height > grant.maxHeight) {
            return HttpServer.Response.text(403, "above your plan's quality");
        }
        if ("master.m3u8".equals(file)) {
            byte[] raw = read(new File(root + "/" + video + "/master.m3u8"));
            if (raw == null) {
                return HttpServer.Response.text(404, "not found");
            }
            String filtered = filterMaster(new String(raw, "UTF-8"), grant.maxHeight);
            return new HttpServer.Response(200, "application/vnd.apple.mpegurl", filtered.getBytes("UTF-8"), noStore());
        }
        HttpServer.Response out = new StaticFiles(root, "/media/" + token, "none",
                playlist ? "no-store" : "private, max-age=86400").handle(request);
        return out == null ? HttpServer.Response.text(404, "not found") : out;
    }

    @GetMapping("/art/{video}/{file}")
    public HttpServer.Response art(HttpServer.Request request, @PathVariable("video") String video,
            @PathVariable("file") String file) throws Exception {
        if (!safeId(video) || !("poster.jpg".equals(file) || "sprite.jpg".equals(file))) {
            return HttpServer.Response.text(404, "not found");
        }
        HttpServer.Response out = new StaticFiles(root, "/art", "none", "public, max-age=86400").handle(request);
        return out == null ? HttpServer.Response.text(404, "not found") : out;
    }

    private String renditions(String videoId) throws java.io.IOException {
        Video v = videos.findById(videoId);
        return v == null ? "" : v.getRenditions();
    }

    /// The height a served file belongs to: 0 for files every quality shares,
    /// the rendition's height for `v<h>.m3u8` and `v<h>_<n>.ts`, and -1 for a
    /// name the packager never writes.
    public static int renditionHeight(String file) {
        if ("master.m3u8".equals(file) || "cenc.mp4".equals(file)) {
            return 0;
        }
        if (file.length() < 3 || file.charAt(0) != 'v') {
            return -1;
        }
        int at = 1;
        while (at < file.length() && Character.isDigit(file.charAt(at))) {
            at++;
        }
        if (at == 1 || at > 5) {
            return -1;
        }
        int height = Integer.parseInt(file.substring(1, at));
        String rest = file.substring(at);
        if (".m3u8".equals(rest)) {
            return height;
        }
        if (rest.length() > 4 && rest.charAt(0) == '_' && rest.endsWith(".ts")) {
            for (int iter = 1; iter < rest.length() - 3; iter++) {
                if (!Character.isDigit(rest.charAt(iter))) {
                    return -1;
                }
            }
            return height;
        }
        return -1;
    }

    /// Drops every variant taller than `maxHeight` from a master playlist,
    /// keeping the smallest when all are taller so something still plays.
    static String filterMaster(String master, int maxHeight) {
        String[] lines = com.codenameone.examples.reel.Rows.split(master, '\n').toArray(new String[0]);
        StringBuilder out = new StringBuilder();
        StringBuilder smallest = null;
        int smallestHeight = Integer.MAX_VALUE;
        boolean any = false;
        for (int iter = 0; iter < lines.length; iter++) {
            String line = lines[iter];
            if (line.startsWith("#EXT-X-STREAM-INF") && iter + 1 < lines.length) {
                String uri = lines[iter + 1];
                int h = heightOf(line, uri);
                iter++;
                if (h <= maxHeight) {
                    out.append(line).append('\n').append(uri).append('\n');
                    any = true;
                } else if (h < smallestHeight) {
                    smallestHeight = h;
                    smallest = new StringBuilder().append(line).append('\n').append(uri).append('\n');
                }
            } else {
                out.append(line).append('\n');
            }
        }
        if (!any && smallest != null) {
            out.append(smallest);
        }
        return out.toString();
    }

    private static int heightOf(String info, String uri) {
        int at = info.indexOf("RESOLUTION=");
        if (at >= 0) {
            int x = info.indexOf('x', at);
            int end = x + 1;
            while (end < info.length() && Character.isDigit(info.charAt(end))) {
                end++;
            }
            if (x > 0 && end > x + 1) {
                return Integer.parseInt(info.substring(x + 1, end));
            }
        }
        int h = renditionHeight(uri.trim());
        return h < 0 ? Integer.MAX_VALUE : h;
    }

    /// Video ids are generated ids or seed slugs: letters, digits and dashes.
    public static boolean safeId(String id) {
        if (id == null || id.length() == 0 || id.length() > 64) {
            return false;
        }
        for (int iter = 0; iter < id.length(); iter++) {
            char c = id.charAt(iter);
            if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9') && c != '-') {
                return false;
            }
        }
        return true;
    }

    private static Map<String, String> noStore() {
        Map<String, String> h = new HashMap<String, String>();
        h.put("Cache-Control", "no-store");
        return h;
    }

    static byte[] read(File f) throws IOException {
        if (!f.exists()) {
            return null;
        }
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
