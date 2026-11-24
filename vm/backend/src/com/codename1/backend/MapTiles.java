/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.backend;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

/// Serves map tiles out of a [PMTiles](https://github.com/protomaps/PMTiles)
/// version 3 archive, so an application's own backend is its map tile server
/// and the client needs no third-party tile host.
///
/// A PMTiles archive is one file holding a whole tile pyramid: an OpenStreetMap
/// extract of a city, a country or the planet, as vector tiles (Protomaps
/// basemaps, OpenMapTiles) or as raster images. Producing one is a build step
/// outside this class (`pmtiles extract` cuts a region from a published planet
/// build; `planetiler` and `tippecanoe` write one from OSM data). Serving it is
/// this handler:
///
/// ```java
/// MapTiles tiles = new MapTiles("/srv/maps/region.pmtiles", "/tiles");
/// server.handle(tiles);
/// ```
///
/// - `GET /tiles` (or `/tiles/tiles.json`) answers a TileJSON 3.0 document:
/// the tile URL template, zoom range, bounds, center and the archive's own
/// metadata (attribution, `vector_layers`). Pass that URL to the client's
/// `HttpTileSource` / `MvtTileSource` and it resolves the template itself.
/// - `GET /tiles/{z}/{x}/{y}.{ext}` answers one tile, with the extension that
/// matches the archive's tile type (`mvt` or `pbf`, `png`, `jpg`, `webp`,
/// `avif`, `mlt`). A tile inside the archive's zoom range that the archive
/// does not contain answers 204, the convention MapLibre treats as an empty
/// tile; a tile outside the pyramid answers 404.
///
/// Tile bytes go out on the same zero-copy path as [StaticFiles]: a tile is a
/// byte range of the archive, sent with sendfile() where the platform has it.
/// A gzip-stored tile is sent as stored, with `Content-Encoding: gzip`, to a
/// client that accepts gzip, and inflated here for one that does not. Archives
/// whose directories or tiles use brotli or zstd are refused when the handler is
/// constructed, since neither can be decoded here.
///
/// The archive can be replaced while the server runs: every request checks the
/// open file's identity (inode, size, modification time) against the one the
/// directories were read from, and a changed file is re-read before it is
/// served from. Replace it by renaming a complete file over it, never by
/// rewriting it in place.
///
/// OpenStreetMap data is licensed under the ODbL and must be attributed. The
/// attribution in the archive's metadata is passed through in the TileJSON; the
/// client is responsible for showing it.
public final class MapTiles implements HttpServer.Handler {
    private static final int HEADER_LENGTH = 127;
    private static final int COMPRESSION_UNKNOWN = 0;
    private static final int COMPRESSION_NONE = 1;
    private static final int COMPRESSION_GZIP = 2;
    private static final int MAX_ZOOM = 30;
    private static final int MAX_DIRECTORY_DEPTH = 4;
    private static final int LEAF_CACHE_SIZE = 64;
    // A directory or a tile larger than this is a corrupt archive, not a tile.
    private static final long MAX_READ = 16L * 1024 * 1024;

    private final String path;
    private final String prefix;
    private final AtomicReference<Archive> current = new AtomicReference<Archive>();
    private String publicUrl;
    private String cacheControl = "public, max-age=86400";

    /// Opens the archive and reads its header, root directory and metadata.
    ///
    /// - `archive`: path of the `.pmtiles` file
    /// - `prefix`: the URL path the handler is mounted at, "" or "/" for the root
    /// @throws IOException when the file is missing, is not a version 3
    /// archive, or uses a compression this handler cannot decode
    public MapTiles(String archive, String prefix) throws IOException {
        this.path = archive;
        this.prefix = prefix == null || "/".equals(prefix) ? ""
                : stripTrailingSlash(HttpServer.canonicalDeclaredPath(prefix));
        int fd = FileIo.openRead(archive);
        if (fd < 0) {
            throw new IOException("Map tile archive does not exist: " + archive);
        }
        try {
            long[] info = new long[4];
            if (FileIo.stat(fd, info) != 0 || info[2] != 0) {
                throw new IOException("Not a readable file: " + archive);
            }
            current.set(new Archive(fd, info, archive));
        } finally {
            FileIo.close(fd);
        }
    }

    /// Sets the public base URL written into the TileJSON template, for
    /// example `https://maps.example.com/tiles`. Without it the template is
    /// built from the request's scheme and `Host` header, which is wrong
    /// behind a proxy that rewrites either.
    /// @param url the base URL, without a trailing slash, or null to derive it
    /// @return this handler
    public MapTiles publicUrl(String url) {
        this.publicUrl = url == null ? null : stripTrailingSlash(url);
        return this;
    }

    /// Sets the `Cache-Control` value of tile and TileJSON responses; the
    /// default is one day. Null omits the header.
    /// @param value the header value
    /// @return this handler
    public MapTiles cacheControl(String value) {
        this.cacheControl = value;
        return this;
    }

    /// The lowest zoom level the archive holds.
    /// @return the zoom level
    public int getMinZoom() {
        return current.get().minZoom;
    }

    /// The highest zoom level the archive holds.
    /// @return the zoom level
    public int getMaxZoom() {
        return current.get().maxZoom;
    }

    /// The file extension tiles of this archive are requested with.
    /// @return `mvt`, `png`, `jpg`, `webp`, `avif`, `mlt`, or `bin` when the
    /// archive does not declare a type
    public String getExtension() {
        return extension(current.get().tileType);
    }

    @Override
    public HttpServer.Response handle(HttpServer.Request request) throws Exception {
        String target = request.pathFrom(0);
        if (prefix.length() > 0) {
            if (!target.startsWith(prefix)
                    || (target.length() > prefix.length() && target.charAt(prefix.length()) != '/')) {
                return null;
            }
            target = target.substring(prefix.length());
        }
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
            return HttpServer.Response.text(405, "method not allowed");
        }
        // One descriptor per request, stat'd before anything is read from it:
        // the directories, the inflated body and the bytes sendfile() sends all
        // come from the file whose identity was just checked.
        int fd = FileIo.openRead(path);
        if (fd < 0) {
            return HttpServer.Response.text(503, "tile archive unavailable");
        }
        boolean release = true;
        try {
            Archive archive = archiveFor(fd);
            if (archive == null) {
                return HttpServer.Response.text(503, "tile archive unavailable");
            }
            if (target.length() == 0 || "/".equals(target) || "/tiles.json".equals(target)) {
                return tileJson(request, archive);
            }
            int[] zxy = parseTilePath(target, archive.tileType);
            if (zxy == null) {
                return HttpServer.Response.text(404, "not found");
            }
            int z = zxy[0];
            long x = zxy[1];
            long y = zxy[2];
            if (z < archive.minZoom || z > archive.maxZoom || x >= (1L << z) || y >= (1L << z)) {
                return HttpServer.Response.text(404, "not found");
            }
            long[] found = archive.find(fd, tileId(z, x, y));
            Map headers = new LinkedHashMap();
            if (cacheControl != null) {
                headers.put("Cache-Control", cacheControl);
            }
            headers.put("Access-Control-Allow-Origin", "*");
            String type = contentType(archive.tileType);
            if (found == null) {
                return HttpServer.Response.empty(204, type, headers);
            }
            long offset = found[0];
            long length = found[1];
            boolean gzipped = archive.tileCompression == COMPRESSION_GZIP;
            boolean inflate = gzipped && !Compression.acceptsGzip(request);
            // The inflated answer is different bytes from the stored one, so it
            // carries its own validator: a cache revalidating one must never be
            // told 304 about the other.
            String etag = "\"" + archive.identity + "-" + offset + (inflate ? "-identity" : "") + "\"";
            headers.put("ETag", etag);
            if (gzipped) {
                headers.put("Vary", "Accept-Encoding");
            }
            if (matchesEtag(request.getHeader("if-none-match"), etag)) {
                return HttpServer.Response.empty(304, type, headers);
            }
            if (inflate) {
                return new HttpServer.Response(200, type, gunzip(readFully(fd, offset, (int) length)),
                        headers);
            }
            if (gzipped) {
                headers.put("Content-Encoding", "gzip");
            }
            release = false; // the server owns the descriptor from here
            return HttpServer.Response.file(200, type, StaticFiles.trackFile(fd), offset, length, headers);
        } finally {
            if (release) {
                FileIo.close(fd);
            }
        }
    }

    // The parsed archive that describes the file open on fd, re-reading it when
    // the file was replaced since; null when the replacement cannot be read.
    private Archive archiveFor(int fd) {
        long[] info = new long[4];
        if (FileIo.stat(fd, info) != 0 || info[2] != 0) {
            return null;
        }
        Archive archive = current.get();
        if (archive.identity.equals(identity(info))) {
            return archive;
        }
        try {
            Archive fresh = new Archive(fd, info, path);
            current.set(fresh);
            return fresh;
        } catch (IOException err) {
            return null;
        }
    }

    private HttpServer.Response tileJson(HttpServer.Request request, Archive archive) {
        String base = publicUrl;
        if (base == null) {
            String host = request.getHeader("host");
            if (host == null || !isPlainHost(host)) {
                return HttpServer.Response.text(400, "bad host");
            }
            base = (request.isSecure() ? "https://" : "http://") + host + prefix;
        }
        Map doc = new LinkedHashMap();
        doc.put("tilejson", "3.0.0");
        List tiles = new ArrayList();
        tiles.add(base + "/{z}/{x}/{y}." + extension(archive.tileType));
        doc.put("tiles", tiles);
        doc.put("scheme", "xyz");
        doc.put("minzoom", Integer.valueOf(archive.minZoom));
        doc.put("maxzoom", Integer.valueOf(archive.maxZoom));
        doc.put("bounds", doubles(archive.bounds));
        doc.put("center", doubles(archive.center));
        copy(archive.metadata, "name", doc);
        copy(archive.metadata, "description", doc);
        // OpenStreetMap data must be credited (ODbL); an archive built without
        // an attribution of its own still gets that credit.
        Object credit = archive.metadata.get("attribution");
        doc.put("attribution", credit != null ? credit
                : "<a href=\"https://www.openstreetmap.org/copyright\">&copy; OpenStreetMap contributors</a>");
        copy(archive.metadata, "version", doc);
        copy(archive.metadata, "vector_layers", doc);
        Map headers = new LinkedHashMap();
        if (cacheControl != null) {
            headers.put("Cache-Control", cacheControl);
        }
        headers.put("Access-Control-Allow-Origin", "*");
        if (publicUrl == null) {
            headers.put("Vary", "Host");
        }
        return new HttpServer.Response(200, "application/json; charset=utf-8",
                Utf8.encode(Json.write(doc)), headers);
    }

    private static void copy(Map metadata, String key, Map doc) {
        Object v = metadata.get(key);
        if (v != null) {
            doc.put(key, v);
        }
    }

    private static List doubles(double[] values) {
        List out = new ArrayList();
        for (double v : values) {
            out.add(Double.valueOf(v));
        }
        return out;
    }

    // A Host header is echoed into the document, so anything but a host name,
    // an IPv6 literal and a port is refused rather than escaped.
    private static boolean isPlainHost(String host) {
        if (host.length() == 0 || host.length() > 255) {
            return false;
        }
        for (int i = 0; i < host.length(); i++) {
            char c = host.charAt(i);
            boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                    || c == '.' || c == '-' || c == ':' || c == '[' || c == ']';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static int[] parseTilePath(String target, int tileType) {
        // /{z}/{x}/{y}.{ext}
        int s1 = target.indexOf('/', 1);
        if (!target.startsWith("/") || s1 < 0) {
            return null;
        }
        int s2 = target.indexOf('/', s1 + 1);
        if (s2 < 0) {
            return null;
        }
        int dot = target.indexOf('.', s2 + 1);
        if (dot < 0 || target.indexOf('/', s2 + 1) >= 0) {
            return null;
        }
        if (!extensionMatches(target.substring(dot + 1), tileType)) {
            return null;
        }
        int z = parseSmall(target, 1, s1);
        int x = parseSmall(target, s1 + 1, s2);
        int y = parseSmall(target, s2 + 1, dot);
        if (z < 0 || x < 0 || y < 0 || z > MAX_ZOOM) {
            return null;
        }
        return new int[] {z, x, y};
    }

    private static boolean extensionMatches(String ext, int tileType) {
        if (ext.equalsIgnoreCase(extension(tileType))) {
            return true;
        }
        switch (tileType) {
            case 1: return "pbf".equalsIgnoreCase(ext);
            case 3: return "jpeg".equalsIgnoreCase(ext);
            default: return false;
        }
    }

    // Decimal digits only, at most 10 of them, no sign: -1 when it is not that.
    private static int parseSmall(String s, int from, int to) {
        if (to <= from || to - from > 10) {
            return -1;
        }
        long v = 0;
        for (int i = from; i < to; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            v = v * 10 + (c - '0');
        }
        return v > Integer.MAX_VALUE ? -1 : (int) v;
    }

    private static String extension(int tileType) {
        switch (tileType) {
            case 1: return "mvt";
            case 2: return "png";
            case 3: return "jpg";
            case 4: return "webp";
            case 5: return "avif";
            case 6: return "mlt";
            default: return "bin";
        }
    }

    private static String contentType(int tileType) {
        switch (tileType) {
            case 1: return "application/vnd.mapbox-vector-tile";
            case 2: return "image/png";
            case 3: return "image/jpeg";
            case 4: return "image/webp";
            case 5: return "image/avif";
            case 6: return "application/vnd.maplibre-vector-tile";
            default: return "application/octet-stream";
        }
    }

    private static boolean matchesEtag(String header, String etag) {
        if (header == null) {
            return false;
        }
        int at = 0;
        while (at < header.length()) {
            int comma = header.indexOf(',', at);
            String one = (comma < 0 ? header.substring(at) : header.substring(at, comma)).trim();
            if ("*".equals(one) || etag.equals(one) || ("W/" + etag).equals(one)) {
                return true;
            }
            if (comma < 0) {
                break;
            }
            at = comma + 1;
        }
        return false;
    }

    /// The PMTiles tile id of a tile: the number of tiles in every lower zoom
    /// level plus the tile's position on the zoom level's Hilbert curve.
    static long tileId(int z, long x, long y) {
        long acc = ((1L << (2 * z)) - 1) / 3;
        long n = 1L << z;
        long d = 0;
        for (long s = n / 2; s > 0; s /= 2) {
            long rx = (x & s) > 0 ? 1 : 0;
            long ry = (y & s) > 0 ? 1 : 0;
            d += s * s * ((3 * rx) ^ ry);
            if (ry == 0) {
                if (rx == 1) {
                    x = n - 1 - x;
                    y = n - 1 - y;
                }
                long t = x;
                x = y;
                y = t;
            }
        }
        return acc + d;
    }

    private static String identity(long[] info) {
        return info[0] + "-" + info[1] + "-" + info[3];
    }

    private static byte[] gunzip(byte[] data) throws IOException {
        InputStream in = new GZIPInputStream(new ByteArrayInputStream(data));
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(data.length * 4);
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf, 0, buf.length)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > MAX_READ) {
                    throw new IOException("Decompressed section too large");
                }
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    private static byte[] readFully(int fd, long position, int length) throws IOException {
        if (length < 0 || length > MAX_READ) {
            throw new IOException("Bad section length " + length);
        }
        byte[] out = new byte[length];
        int done = 0;
        while (done < length) {
            int n = FileIo.pread(fd, out, done, length - done, position + done);
            if (n <= 0) {
                throw new IOException("Unexpected end of map tile archive");
            }
            done += n;
        }
        return out;
    }

    private static long le64(byte[] b, int at) {
        long v = 0;
        for (int i = 7; i >= 0; i--) {
            v = (v << 8) | (b[at + i] & 0xff);
        }
        return v;
    }

    private static int le32(byte[] b, int at) {
        return (b[at] & 0xff) | (b[at + 1] & 0xff) << 8 | (b[at + 2] & 0xff) << 16
                | (b[at + 3] & 0xff) << 24;
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    /// Everything read out of one version of the archive file. Immutable apart
    /// from its leaf cache, so a replaced file swaps the whole of it at once.
    private static final class Archive {
        final String identity;
        final String path;
        final long leafOffset;
        final long leafLength;
        final long tileDataOffset;
        final long tileDataLength;
        final int internalCompression;
        final int tileCompression;
        final int tileType;
        final int minZoom;
        final int maxZoom;
        final double[] bounds;
        final double[] center;
        final Directory root;
        final Map metadata;
        private final Map leafCache = new LinkedHashMap(16, 0.75f, true);

        Archive(int fd, long[] info, String path) throws IOException {
            this.path = path;
            this.identity = identity(info);
            long fileSize = info[0];
            if (fileSize < HEADER_LENGTH) {
                throw new IOException("Not a PMTiles archive: " + path);
            }
            byte[] h = readFully(fd, 0, HEADER_LENGTH);
            if (h[0] != 'P' || h[1] != 'M' || h[2] != 'T' || h[3] != 'i' || h[4] != 'l'
                    || h[5] != 'e' || h[6] != 's') {
                throw new IOException("Not a PMTiles archive: " + path);
            }
            if (h[7] != 3) {
                throw new IOException("Unsupported PMTiles version " + h[7] + ": " + path);
            }
            long rootOffset = le64(h, 8);
            long rootLength = le64(h, 16);
            long metaOffset = le64(h, 24);
            long metaLength = le64(h, 32);
            leafOffset = le64(h, 40);
            leafLength = le64(h, 48);
            tileDataOffset = le64(h, 56);
            tileDataLength = le64(h, 64);
            internalCompression = h[97] & 0xff;
            tileCompression = h[98] & 0xff;
            tileType = h[99] & 0xff;
            minZoom = h[100] & 0xff;
            maxZoom = Math.min(h[101] & 0xff, MAX_ZOOM);
            bounds = new double[] {le32(h, 102) / 1e7, le32(h, 106) / 1e7,
                le32(h, 110) / 1e7, le32(h, 114) / 1e7};
            center = new double[] {le32(h, 119) / 1e7, le32(h, 123) / 1e7, h[118] & 0xff};
            if (internalCompression != COMPRESSION_NONE && internalCompression != COMPRESSION_GZIP) {
                throw new IOException("Unsupported directory compression " + internalCompression
                        + " (only none and gzip can be served): " + path);
            }
            if (tileCompression != COMPRESSION_NONE && tileCompression != COMPRESSION_GZIP
                    && tileCompression != COMPRESSION_UNKNOWN) {
                throw new IOException("Unsupported tile compression " + tileCompression
                        + " (only none and gzip can be served): " + path);
            }
            if (minZoom > maxZoom) {
                throw new IOException("Minimum zoom above maximum zoom in " + path);
            }
            checkSection(rootOffset, rootLength, fileSize);
            checkSection(metaOffset, metaLength, fileSize);
            checkSection(leafOffset, leafLength, fileSize);
            checkSection(tileDataOffset, tileDataLength, fileSize);
            root = Directory.decode(decompress(readFully(fd, rootOffset, (int) Math.min(rootLength, MAX_READ + 1))));
            Map meta = null;
            if (metaLength > 0) {
                byte[] raw = decompress(readFully(fd, metaOffset, (int) Math.min(metaLength, MAX_READ + 1)));
                Object parsed = Json.parse(Utf8.decode(raw, 0, raw.length));
                if (parsed instanceof Map) {
                    meta = (Map) parsed;
                }
            }
            metadata = meta == null ? new LinkedHashMap() : meta;
        }

        // {absolute offset, length} of the tile, or null when the archive lacks it.
        long[] find(int fd, long tileId) throws IOException {
            Directory dir = root;
            for (int depth = 0; depth < MAX_DIRECTORY_DEPTH; depth++) {
                int i = dir.search(tileId);
                if (i < 0) {
                    return null;
                }
                long run = dir.runLengths[i];
                long offset = dir.offsets[i];
                long length = dir.lengths[i];
                if (run > 0) {
                    if (tileId >= dir.tileIds[i] + run || length <= 0 || length > MAX_READ
                            || offset > tileDataLength - length) {
                        return null;
                    }
                    return new long[] {tileDataOffset + offset, length};
                }
                // Subtract rather than add: an offset near Long.MAX_VALUE wraps.
                if (length <= 0 || offset > leafLength - length) {
                    throw new IOException("Corrupt leaf directory pointer in " + path);
                }
                dir = leaf(fd, leafOffset + offset, length);
            }
            throw new IOException("Leaf directories nested too deep in " + path);
        }

        private Directory leaf(int fd, long offset, long length) throws IOException {
            Long key = Long.valueOf(offset);
            synchronized (leafCache) {
                Directory cached = (Directory) leafCache.get(key);
                if (cached != null) {
                    return cached;
                }
            }
            Directory decoded = Directory.decode(decompress(
                    readFully(fd, offset, (int) Math.min(length, MAX_READ + 1))));
            synchronized (leafCache) {
                leafCache.put(key, decoded);
                if (leafCache.size() > LEAF_CACHE_SIZE) {
                    Iterator it = leafCache.keySet().iterator();
                    it.next();
                    it.remove();
                }
            }
            return decoded;
        }

        private byte[] decompress(byte[] data) throws IOException {
            return internalCompression == COMPRESSION_GZIP ? gunzip(data) : data;
        }

        private void checkSection(long offset, long length, long fileSize) throws IOException {
            if (offset < 0 || length < 0 || offset + length < 0 || offset + length > fileSize) {
                throw new IOException("Section outside the file in " + path);
            }
        }
    }

    /// One decoded PMTiles directory: parallel columns sorted by tile id.
    static final class Directory {
        final long[] tileIds;
        final long[] runLengths;
        final long[] lengths;
        final long[] offsets;

        private Directory(int n) {
            tileIds = new long[n];
            runLengths = new long[n];
            lengths = new long[n];
            offsets = new long[n];
        }

        static Directory decode(byte[] data) throws IOException {
            int[] pos = new int[1];
            long count = varint(data, pos);
            // Every entry takes at least four bytes, which bounds a lying count.
            if (count <= 0 || count > data.length / 4) {
                throw new IOException("Corrupt map tile directory");
            }
            int n = (int) count;
            Directory d = new Directory(n);
            long last = 0;
            for (int i = 0; i < n; i++) {
                last += varint(data, pos);
                d.tileIds[i] = last;
            }
            for (int i = 0; i < n; i++) {
                d.runLengths[i] = varint(data, pos);
            }
            for (int i = 0; i < n; i++) {
                d.lengths[i] = varint(data, pos);
            }
            for (int i = 0; i < n; i++) {
                long v = varint(data, pos);
                if (v == 0 && i > 0) {
                    d.offsets[i] = d.offsets[i - 1] + d.lengths[i - 1];
                } else if (v == 0) {
                    throw new IOException("Corrupt map tile directory");
                } else {
                    d.offsets[i] = v - 1;
                }
                // A varint past 2^63 wraps negative and would pass the bounds
                // checks made against the section length.
                if (d.offsets[i] < 0 || d.lengths[i] < 0 || d.runLengths[i] < 0) {
                    throw new IOException("Corrupt map tile directory");
                }
            }
            return d;
        }

        // Index of the last entry whose tile id is <= id, or -1.
        int search(long id) {
            int lo = 0;
            int hi = tileIds.length - 1;
            int best = -1;
            while (lo <= hi) {
                int mid = (lo + hi) >>> 1;
                if (tileIds[mid] <= id) {
                    best = mid;
                    lo = mid + 1;
                } else {
                    hi = mid - 1;
                }
            }
            return best;
        }

        private static long varint(byte[] b, int[] pos) throws IOException {
            long v = 0;
            int shift = 0;
            while (true) {
                if (pos[0] >= b.length || shift > 63) {
                    throw new IOException("Corrupt map tile directory");
                }
                int c = b[pos[0]++] & 0xff;
                v |= (long) (c & 0x7f) << shift;
                if ((c & 0x80) == 0) {
                    return v;
                }
                shift += 7;
            }
        }
    }
}
