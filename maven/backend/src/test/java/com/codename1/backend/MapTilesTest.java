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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapTilesTest {
    @TempDir
    File dir;

    @Test
    void tileIdsMatchTheSpecExamples() {
        assertEquals(0, MapTiles.tileId(0, 0, 0));
        assertEquals(1, MapTiles.tileId(1, 0, 0));
        assertEquals(2, MapTiles.tileId(1, 0, 1));
        assertEquals(3, MapTiles.tileId(1, 1, 1));
        assertEquals(4, MapTiles.tileId(1, 1, 0));
        assertEquals(5, MapTiles.tileId(2, 0, 0));
        assertEquals(19078479, MapTiles.tileId(12, 3423, 1763));
    }

    @Test
    void servesTilesFromRootAndLeafDirectories() throws Exception {
        File archive = buildArchive();
        MapTiles tiles = new MapTiles(archive.getPath(), "/tiles");
        assertEquals(0, tiles.getMinZoom());
        assertEquals(2, tiles.getMaxZoom());

        // Root entry, zoom 0, stored gzip and served as stored to a gzip client.
        HttpServer.Response r = get(tiles, "/tiles/0/0/0.mvt", true);
        assertEquals(200, r.getStatus());
        assertEquals("gzip", r.extraHeaders.get("Content-Encoding"));
        assertArrayEquals(gzip(tile("z0")), fileBytes(archive, r));
        FileIo.close(r.fileFd);

        // A run of three ids sharing one blob: (1,0,0), (1,0,1), (1,1,1).
        for (String p : new String[] {"/tiles/1/0/0.pbf", "/tiles/1/0/1.mvt", "/tiles/1/1/1.mvt"}) {
            HttpServer.Response run = get(tiles, p, true);
            assertEquals(200, run.getStatus(), p);
            assertArrayEquals(gzip(tile("run")), fileBytes(archive, run));
            FileIo.close(run.fileFd);
        }

        // Reached through a leaf directory, inflated for a client without gzip.
        HttpServer.Response leaf = get(tiles, "/tiles/2/0/0.mvt", false);
        assertEquals(200, leaf.getStatus());
        assertNull(leaf.extraHeaders.get("Content-Encoding"));
        assertArrayEquals(tile("leaf"), leaf.getBody());

        // Inside the pyramid but absent: empty tile. Outside it: not found.
        assertEquals(204, get(tiles, "/tiles/1/1/0.mvt", true).getStatus());
        assertEquals(404, get(tiles, "/tiles/3/0/0.mvt", true).getStatus());
        assertEquals(404, get(tiles, "/tiles/1/2/0.mvt", true).getStatus());
        assertEquals(404, get(tiles, "/tiles/0/0/0.png", true).getStatus());
        assertEquals(404, get(tiles, "/tiles/0/0/-1.mvt", true).getStatus());
        assertNull(get(tiles, "/tiles2/0/0/0.mvt", true));
        assertNull(get(tiles, "/other", true));
    }

    @Test
    void conditionalRequestAnswersNotModified() throws Exception {
        MapTiles tiles = new MapTiles(buildArchive().getPath(), "/tiles");
        HttpServer.Response first = get(tiles, "/tiles/0/0/0.mvt", true);
        FileIo.close(first.fileFd);
        String etag = (String) first.extraHeaders.get("ETag");
        Map<String, String> h = new HashMap<>();
        h.put("accept-encoding", "gzip");
        h.put("if-none-match", etag);
        HttpServer.Response again = tiles.handle(HttpServer.Request.of("GET", "/tiles/0/0/0.mvt", h, null));
        assertEquals(304, again.getStatus());
    }

    @Test
    void tileJsonDescribesTheArchive() throws Exception {
        MapTiles tiles = new MapTiles(buildArchive().getPath(), "/tiles");
        Map<String, String> h = new HashMap<>();
        h.put("host", "maps.example.com:8080");
        HttpServer.Response r = tiles.handle(HttpServer.Request.of("GET", "/tiles", h, null));
        assertEquals(200, r.getStatus());
        Map doc = Json.parseObject(new String(r.getBody(), StandardCharsets.UTF_8));
        assertEquals("http://maps.example.com:8080/tiles/{z}/{x}/{y}.mvt", ((List) doc.get("tiles")).get(0));
        assertEquals("(c) OpenStreetMap contributors", doc.get("attribution"));
        assertEquals(2, ((Number) doc.get("maxzoom")).intValue());
        assertTrue(doc.get("vector_layers") instanceof List);

        tiles.publicUrl("https://cdn.example.com/t/");
        r = tiles.handle(HttpServer.Request.of("GET", "/tiles/tiles.json", h, null));
        doc = Json.parseObject(new String(r.getBody(), StandardCharsets.UTF_8));
        assertEquals("https://cdn.example.com/t/{z}/{x}/{y}.mvt", ((List) doc.get("tiles")).get(0));

        tiles.publicUrl(null);
        h.put("host", "evil\"host");
        assertEquals(400, tiles.handle(HttpServer.Request.of("GET", "/tiles", h, null)).getStatus());
    }

    @Test
    void refusesWhatItCannotServe() throws Exception {
        File junk = new File(dir, "junk.pmtiles");
        try (FileOutputStream out = new FileOutputStream(junk)) {
            out.write(new byte[200]);
        }
        assertThrows(IOException.class, () -> new MapTiles(junk.getPath(), "/"));
        assertThrows(IOException.class, () -> new MapTiles(new File(dir, "none").getPath(), "/"));
        File brotli = buildArchive();
        try (RandomAccessFile raf = new RandomAccessFile(brotli, "rw")) {
            raf.seek(98);
            raf.write(3);
        }
        assertThrows(IOException.class, () -> new MapTiles(brotli.getPath(), "/"));
    }

    private static HttpServer.Response get(MapTiles tiles, String path, boolean gzip) throws Exception {
        Map<String, String> h = new HashMap<>();
        if (gzip) {
            h.put("accept-encoding", "gzip");
        }
        return tiles.handle(HttpServer.Request.of("GET", path, h, null));
    }

    private static byte[] fileBytes(File archive, HttpServer.Response r) throws IOException {
        assertTrue(r.fileFd >= 0);
        byte[] out = new byte[(int) r.fileLength];
        try (RandomAccessFile raf = new RandomAccessFile(archive, "r")) {
            raf.seek(r.fileOffset);
            raf.readFully(out);
        }
        return out;
    }

    private static byte[] tile(String name) {
        return ("tile-" + name).getBytes(StandardCharsets.UTF_8);
    }

    // A gzip-compressed vector archive, zooms 0-2: tile 0 and a run of 1..3 in
    // the root, and tile 5 (2/0/0) behind a leaf directory.
    private File buildArchive() throws IOException {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        byte[] t0 = gzip(tile("z0"));
        byte[] tRun = gzip(tile("run"));
        byte[] tLeaf = gzip(tile("leaf"));
        data.write(t0);
        data.write(tRun);
        data.write(tLeaf);

        byte[] leafDir = gzip(directory(new long[][] {{5, 1, tLeaf.length, t0.length + tRun.length}}));
        byte[] rootDir = gzip(directory(new long[][] {
            {0, 1, t0.length, 0},
            {1, 3, tRun.length, t0.length},
            {5, 0, leafDir.length, 0},
        }));
        byte[] meta = gzip(("{\"name\":\"test\",\"attribution\":\"(c) OpenStreetMap contributors\","
                + "\"vector_layers\":[{\"id\":\"roads\"}]}").getBytes(StandardCharsets.UTF_8));

        long rootOff = 127;
        long metaOff = rootOff + rootDir.length;
        long leafOff = metaOff + meta.length;
        long dataOff = leafOff + leafDir.length;
        byte[] h = new byte[127];
        System.arraycopy("PMTiles".getBytes(StandardCharsets.US_ASCII), 0, h, 0, 7);
        h[7] = 3;
        le64(h, 8, rootOff);
        le64(h, 16, rootDir.length);
        le64(h, 24, metaOff);
        le64(h, 32, meta.length);
        le64(h, 40, leafOff);
        le64(h, 48, leafDir.length);
        le64(h, 56, dataOff);
        le64(h, 64, data.size());
        h[96] = 1;
        h[97] = 2;
        h[98] = 2;
        h[99] = 1;
        h[100] = 0;
        h[101] = 2;
        le32(h, 102, -1800000000);
        le32(h, 106, -850000000);
        le32(h, 110, 1800000000);
        le32(h, 114, 850000000);
        h[118] = 1;

        File f = new File(dir, "test" + System.nanoTime() + ".pmtiles");
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(h);
            out.write(rootDir);
            out.write(meta);
            out.write(leafDir);
            out.write(data.toByteArray());
        }
        return f;
    }

    // Entries of {tileId, runLength, length, offset}, encoded per the spec.
    private static byte[] directory(long[][] entries) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        varint(o, entries.length);
        long last = 0;
        for (long[] e : entries) {
            varint(o, e[0] - last);
            last = e[0];
        }
        for (long[] e : entries) {
            varint(o, e[1]);
        }
        for (long[] e : entries) {
            varint(o, e[2]);
        }
        List<long[]> list = new ArrayList<>();
        for (long[] e : entries) {
            list.add(e);
        }
        for (int i = 0; i < list.size(); i++) {
            long[] e = list.get(i);
            if (i > 0 && e[3] == list.get(i - 1)[3] + list.get(i - 1)[2]) {
                varint(o, 0);
            } else {
                varint(o, e[3] + 1);
            }
        }
        return o.toByteArray();
    }

    private static void varint(ByteArrayOutputStream o, long v) {
        while (v >= 0x80) {
            o.write((int) (v & 0x7f) | 0x80);
            v >>>= 7;
        }
        o.write((int) v);
    }

    private static byte[] gzip(byte[] in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (GZIPOutputStream g = new GZIPOutputStream(b)) {
            g.write(in);
        }
        return b.toByteArray();
    }

    private static void le64(byte[] b, int at, long v) {
        for (int i = 0; i < 8; i++) {
            b[at + i] = (byte) (v >>> (8 * i));
        }
    }

    private static void le32(byte[] b, int at, int v) {
        for (int i = 0; i < 4; i++) {
            b[at + i] = (byte) (v >>> (8 * i));
        }
    }
}
