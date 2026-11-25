/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.art;

import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.geom.PathIterator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Readers, writers and the renderer of `com.codename1.art`.
class ArtFormatsTest {
    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static int rgb(Raster r, int x, int y) {
        return r.get(x, y) & 0xffffff;
    }

    private static void near(int expected, int actual) {
        for (int s = 0; s < 24; s += 8) {
            int a = (expected >> s) & 0xff;
            int b = (actual >> s) & 0xff;
            assertTrue(Math.abs(a - b) <= 3, Integer.toHexString(expected) + " vs " + Integer.toHexString(actual));
        }
    }

    private static int segments(GeneralPath p) {
        int n = 0;
        PathIterator it = p.getPathIterator();
        float[] c = new float[6];
        while (!it.isDone()) {
            it.currentSegment(c);
            n++;
            it.next();
        }
        return n;
    }

    @Test
    void pathDataHandlesRelativeArcsAndImplicitRepeats() {
        GeneralPath p = PathData.parse("M10 10 l10 0 10 0 a5 5 0 0 1 10 0 z");
        assertTrue(segments(p) >= 5);
        float[] b = p.getBounds2D();
        assertEquals(10, b[0], 0.01);
        assertEquals(40, b[0] + b[2], 0.01);
    }

    @Test
    void pathDataStopsAtMalformedInput() {
        GeneralPath p = PathData.parse("M0 0 L10 10 L x 5");
        assertNotNull(p);
        assertEquals(2, segments(p));
    }

    @Test
    void compositorMatchesW3cFormulas() {
        // Opaque layers: multiply and screen on 0x80 gray over 0x40 gray.
        near(0x202020, Compositor.blend(0xff808080, 0xff404040, BlendMode.MULTIPLY, 1) & 0xffffff);
        near(0xa0a0a0, Compositor.blend(0xff808080, 0xff404040, BlendMode.SCREEN, 1) & 0xffffff);
        near(0x404040, Compositor.blend(0xff808080, 0xffc0c0c0, BlendMode.DIFFERENCE, 1) & 0xffffff);
        // Half opacity normal is a mix.
        near(0x808080, Compositor.blend(0xffffffff, 0xff000000, BlendMode.NORMAL, 0.5f) & 0xffffff);
        // Over a transparent backdrop the layer's own color shows, whatever the mode.
        assertEquals(0xff808080, Compositor.blend(0xff808080, 0, BlendMode.MULTIPLY, 1));
    }

    @Test
    void svgRendersShapesGradientsAndUse() throws IOException {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' xmlns:xlink='http://www.w3.org/1999/xlink'"
                + " width='40' height='20'><defs><rect id='r' width='10' height='10' fill='#ff0000'/></defs>"
                + "<use xlink:href='#r' x='0' y='0'/><use href='#r' x='20' y='0' style='fill:blue'/>"
                + "<circle cx='35' cy='15' r='4' fill='rgb(0,255,0)'/></svg>";
        ArtDocument d = SvgReader.read(utf8(svg));
        assertEquals(40, d.getWidth(), 0.001);
        Raster r = ArtRenderer.render(d, 1);
        assertEquals(0xff0000, rgb(r, 5, 5));
        // A fill attribute on the referenced element wins over the use's style.
        assertEquals(0xff0000, rgb(r, 25, 5));
        assertEquals(0x00ff00, rgb(r, 35, 15));
        assertEquals(0, r.get(15, 15) >>> 24);
    }

    @Test
    void svgRecursiveUseFailsCleanly() throws IOException {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'>"
                + "<g id='a'><use href='#b'/></g><g id='b'><use href='#a'/></g></svg>";
        ArtDocument d = SvgReader.read(utf8(svg));
        assertNotNull(ArtRenderer.render(d, 1));
    }

    @Test
    void svgRoundTripKeepsStructure() throws IOException {
        ArtDocument d = new ArtDocument(30, 30);
        ShapeNode s = ShapeNode.rect(5, 5, 20, 20, 0);
        s.fill(new SolidPaint(0xff1e88e5));
        s.setBlendMode(BlendMode.MULTIPLY);
        s.setOpacity(0.5f);
        d.getRoot().getChildren().add(s);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SvgWriter.write(d, out);
        ArtDocument back = SvgReader.read(out.toByteArray());
        Raster a = ArtRenderer.render(d, 1);
        Raster b = ArtRenderer.render(back, 1);
        near(a.get(15, 15), b.get(15, 15));
        assertEquals(a.get(15, 15) >>> 24, b.get(15, 15) >>> 24);
    }

    private static ArtDocument twoLayers() {
        ArtDocument d = new ArtDocument(16, 12);
        ShapeNode back = ShapeNode.rect(0, 0, 16, 12, 0);
        back.fill(new SolidPaint(0xff808080));
        back.setName("Back");
        ShapeNode top = ShapeNode.rect(4, 4, 8, 4, 0);
        top.fill(new SolidPaint(0xff808080));
        top.setName("Top");
        top.setBlendMode(BlendMode.MULTIPLY);
        GroupNode g = new GroupNode();
        g.setName("Folder");
        g.getChildren().add(top);
        d.getRoot().getChildren().add(back);
        d.getRoot().getChildren().add(g);
        return d;
    }

    @Test
    void psdRoundTripKeepsLayersNamesAndBlending() throws IOException {
        ArtDocument d = twoLayers();
        byte[] psd = PsdWriter.toBytes(d);
        assertEquals("psd", ArtFormats.detect(psd));
        ArtDocument back = PsdReader.read(psd);
        assertEquals(16, back.getWidth(), 0.001);
        Raster r = ArtRenderer.render(back, 1);
        near(0x404040, rgb(r, 6, 5));
        near(0x808080, rgb(r, 1, 1));
        assertNotNull(find(back.getRoot(), "Top"));
        ArtNode folder = find(back.getRoot(), "Folder");
        assertTrue(folder instanceof GroupNode);
        assertEquals(BlendMode.MULTIPLY, find(back.getRoot(), "Top").getBlendMode());
    }

    private static ArtNode find(GroupNode g, String name) {
        for (int i = 0; i < g.getChildren().size(); i++) {
            ArtNode c = g.getChildren().get(i);
            if (name.equals(c.getName())) {
                return c;
            }
            if (c instanceof GroupNode) {
                ArtNode f = find((GroupNode) c, name);
                if (f != null) {
                    return f;
                }
            }
        }
        return null;
    }

    @Test
    void psdHostileInputFailsWithIOException() throws IOException {
        byte[] psd = PsdWriter.toBytes(twoLayers());
        byte[] cut = new byte[psd.length / 2];
        System.arraycopy(psd, 0, cut, 0, cut.length);
        assertThrows(IOException.class, () -> PsdReader.read(cut));
        byte[] huge = new byte[psd.length];
        System.arraycopy(psd, 0, huge, 0, psd.length);
        // Height and width fields set to 2^30.
        huge[14] = 0x40;
        huge[18] = 0x40;
        assertThrows(IOException.class, () -> PsdReader.read(huge));
        assertThrows(IOException.class, () -> PsdReader.read(new byte[]{'8', 'B', 'P', 'S'}));
    }

    @Test
    void packBitsRoundTrips() {
        byte[] data = new byte[300];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i < 150 ? 7 : i);
        }
        byte[] packed = PsdWriter.packBits(data, data.length);
        assertTrue(packed.length < data.length);
        byte[] out = new byte[data.length];
        PsdReader.packBits(packed, 0, packed.length, out, 0, out.length);
        for (int i = 0; i < data.length; i++) {
            assertEquals(data[i], out[i]);
        }
    }

    @Test
    void nativeFormatIsLossless() throws IOException {
        ArtDocument d = twoLayers();
        RasterNode px = new RasterNode(new Raster(2, 3, 2, 2, new int[]{0xffff0000, 0x8000ff00, 0, 0xff0000ff}));
        px.setName("Pixels");
        d.getRoot().getChildren().add(px);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        NativeFormat.write(d, out);
        ArtDocument back = ArtFormats.read(out.toByteArray());
        Raster a = ArtRenderer.render(d, 1);
        Raster b = ArtRenderer.render(back, 1);
        for (int i = 0; i < a.getPixels().length; i++) {
            assertEquals(a.getPixels()[i], b.getPixels()[i], "pixel " + i);
        }
        assertEquals(BlendMode.MULTIPLY, find(back.getRoot(), "Top").getBlendMode());
    }

    @Test
    void sketchReadsPagesArtboardsAndShapes() throws IOException {
        String page = "{\"_class\":\"page\",\"name\":\"Page 1\",\"frame\":{\"x\":0,\"y\":0,\"width\":100,\"height\":50},"
                + "\"layers\":[{\"_class\":\"artboard\",\"name\":\"Board\",\"frame\":{\"x\":0,\"y\":0,\"width\":100,\"height\":50},"
                + "\"hasBackgroundColor\":true,\"backgroundColor\":{\"red\":1,\"green\":1,\"blue\":1,\"alpha\":1},"
                + "\"layers\":[{\"_class\":\"rectangle\",\"name\":\"Box\",\"isVisible\":true,"
                + "\"frame\":{\"x\":10,\"y\":10,\"width\":20,\"height\":20},\"isClosed\":true,"
                + "\"points\":[{\"point\":\"{0, 0}\",\"curveFrom\":\"{0, 0}\",\"curveTo\":\"{0, 0}\",\"curveMode\":1},"
                + "{\"point\":\"{1, 0}\",\"curveFrom\":\"{1, 0}\",\"curveTo\":\"{1, 0}\",\"curveMode\":1},"
                + "{\"point\":\"{1, 1}\",\"curveFrom\":\"{1, 1}\",\"curveTo\":\"{1, 1}\",\"curveMode\":1},"
                + "{\"point\":\"{0, 1}\",\"curveFrom\":\"{0, 1}\",\"curveTo\":\"{0, 1}\",\"curveMode\":1}],"
                + "\"style\":{\"fills\":[{\"isEnabled\":true,\"fillType\":0,"
                + "\"color\":{\"red\":1,\"green\":0,\"blue\":0,\"alpha\":1}}]}}]}]}";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ZipOutputStream z = new ZipOutputStream(bo);
        z.putNextEntry(new ZipEntry("document.json"));
        z.write(utf8("{\"_class\":\"document\",\"pages\":[{\"_ref\":\"pages/P1\"}]}"));
        z.putNextEntry(new ZipEntry("pages/P1.json"));
        z.write(utf8(page));
        z.putNextEntry(new ZipEntry("meta.json"));
        z.write(utf8("{\"version\":136}"));
        z.close();
        byte[] sketch = bo.toByteArray();
        assertEquals("sketch", ArtFormats.detect(sketch));
        ArtDocument d = ArtFormats.read(sketch);
        assertEquals(100, d.getWidth(), 0.001);
        Raster r = ArtRenderer.render(d, 1);
        assertEquals(0xff0000, rgb(r, 20, 20));
        assertEquals(0xffffff, rgb(r, 60, 40));
        assertNotNull(find(d.getRoot(), "Box"));
    }

    @Test
    void figmaReadsFramesGeometryAndPaints() throws IOException {
        String json = "{\"name\":\"F\",\"document\":{\"type\":\"DOCUMENT\",\"children\":[{\"type\":\"CANVAS\","
                + "\"name\":\"Page\",\"children\":[{\"type\":\"FRAME\",\"name\":\"Frame\",\"absoluteBoundingBox\":"
                + "{\"x\":0,\"y\":0,\"width\":60,\"height\":40},\"size\":{\"x\":60,\"y\":40},"
                + "\"relativeTransform\":[[1,0,0],[0,1,0]],\"fills\":[{\"type\":\"SOLID\",\"color\":"
                + "{\"r\":0,\"g\":0,\"b\":1,\"a\":1}}],\"children\":[{\"type\":\"RECTANGLE\",\"name\":\"Rect\","
                + "\"size\":{\"x\":10,\"y\":10},\"relativeTransform\":[[1,0,20],[0,1,10]],"
                + "\"fills\":[{\"type\":\"SOLID\",\"color\":{\"r\":0,\"g\":1,\"b\":0,\"a\":1}}],"
                + "\"fillGeometry\":[{\"path\":\"M0 0L10 0L10 10L0 10Z\",\"windingRule\":\"NONZERO\"}]}]}]}]}}";
        assertEquals("figma", ArtFormats.detect(utf8(json)));
        ArtDocument d = FigmaReader.read(utf8(json), new HashMap<String, byte[]>());
        Raster r = ArtRenderer.render(d, 1);
        assertEquals(0x0000ff, rgb(r, 5, 5));
        assertEquals(0x00ff00, rgb(r, 25, 15));
        assertNotNull(find(d.getRoot(), "Rect"));
    }

    @Test
    void detectionRejectsUnknownAndOldSketch() {
        assertThrows(IOException.class, () -> ArtFormats.read(utf8("SQLite format 3\u0000....")));
        assertThrows(IOException.class, () -> ArtFormats.read(new byte[]{1, 2, 3, 4}));
        assertFalse("psd".equals(ArtFormats.detect(utf8("<svg/>"))));
    }

    @Test
    void knockoutRevealsTheGroupBackdropAndSurvivesPsd() throws IOException {
        ArtDocument d = new ArtDocument(10, 10);
        ShapeNode back = ShapeNode.rect(0, 0, 10, 10, 0);
        back.fill(new SolidPaint(0xff0000ff));
        GroupNode g = new GroupNode();
        g.setName("G");
        g.setPassThrough(false);
        ShapeNode mid = ShapeNode.rect(0, 0, 10, 10, 0);
        mid.fill(new SolidPaint(0xffff0000));
        ShapeNode hole = ShapeNode.rect(2, 2, 4, 4, 0);
        hole.fill(new SolidPaint(0xff00ff00));
        hole.setOpacity(0.5f);
        hole.setKnockout(true);
        hole.setName("Hole");
        g.getChildren().add(mid);
        g.getChildren().add(hole);
        d.getRoot().getChildren().add(back);
        d.getRoot().getChildren().add(g);
        Raster r = ArtRenderer.render(d, 1);
        // Half green over the blue backdrop, with the red of the group erased under it.
        near(0x008080, rgb(r, 3, 3));
        assertEquals(0xff0000, rgb(r, 8, 8));
        ArtDocument back2 = PsdReader.read(PsdWriter.toBytes(d));
        assertTrue(find(back2.getRoot(), "Hole").isKnockout());
        near(0x008080, rgb(ArtRenderer.render(back2, 1), 3, 3));
    }

    @Test
    void floodFillStopsAtEdges() {
        Raster r = new Raster(0, 0, 4, 1, new int[]{0xffffffff, 0xffffffff, 0xff000000, 0xffffffff});
        ArtEditor.floodFill(r, 0, 0, 0xffff0000);
        assertEquals(0xffff0000, r.get(0, 0));
        assertEquals(0xffff0000, r.get(1, 0));
        assertEquals(0xff000000, r.get(2, 0));
        assertEquals(0xffffffff, r.get(3, 0));
    }

    private static void u16(ByteArrayOutputStream o, int v) {
        o.write(v >> 8);
        o.write(v);
    }

    private static void u32(ByteArrayOutputStream o, long v) {
        u16(o, (int) (v >> 16) & 0xffff);
        u16(o, (int) v & 0xffff);
    }

    @Test
    void psdPatternLibraryIsDecoded() throws IOException {
        // One 2x1 RGB pattern, raw channels, the second pixel half transparent.
        ByteArrayOutputStream p = new ByteArrayOutputStream();
        u32(p, 1);
        u32(p, 3);
        u16(p, 1);
        u16(p, 2);
        u32(p, 1);
        u16(p, 'P');
        String id = "pat-1";
        p.write(id.length());
        p.write(utf8(id), 0, id.length());
        u32(p, 3);
        ByteArrayOutputStream va = new ByteArrayOutputStream();
        u32(va, 0);
        u32(va, 0);
        u32(va, 1);
        u32(va, 2);
        u32(va, 3);
        int[][] chans = {{0xff, 0x00}, {0x00, 0xff}, {0x00, 0x00}, {0xff, 0x80}};
        for (int[] c : chans) {
            u32(va, 1);
            u32(va, 4 + 16 + 2 + 1 + 2);
            u32(va, 8);
            u32(va, 0);
            u32(va, 0);
            u32(va, 1);
            u32(va, 2);
            u16(va, 8);
            va.write(0);
            va.write(c[0]);
            va.write(c[1]);
        }
        u32(va, 0);
        byte[] vb = va.toByteArray();
        u32(p, vb.length);
        p.write(vb, 0, vb.length);
        byte[] pat = p.toByteArray();
        ByteArrayOutputStream block = new ByteArrayOutputStream();
        u32(block, pat.length);
        block.write(pat, 0, pat.length);
        while (block.size() % 4 != 0) {
            block.write(0);
        }
        Raster tile = PsdReader.readPattern(block.toByteArray(), id);
        assertNotNull(tile);
        assertEquals(2, tile.getWidth());
        assertEquals(0xffff0000, tile.get(0, 0));
        assertEquals(0x8000ff00, tile.get(1, 0));
        // A pattern overlay paints the tile, repeated, over the layer.
        ArtDocument d = new ArtDocument(4, 1);
        ShapeNode s = ShapeNode.rect(0, 0, 4, 1, 0);
        s.fill(new SolidPaint(0xff000000));
        s.getEffects().add(com.codename1.art.effects.Effects.patternOverlay(tile, 1));
        d.getRoot().getChildren().add(s);
        Raster r = ArtRenderer.render(d, 1);
        assertEquals(0xff0000, rgb(r, 2, 0));
        near(0x008000, rgb(r, 3, 0));
    }

    private static String sketchShapeGroup(int op) {
        return "{\"_class\":\"shapeGroup\",\"name\":\"SG\",\"isVisible\":true,"
                + "\"frame\":{\"x\":0,\"y\":0,\"width\":40,\"height\":20},"
                + "\"style\":{\"fills\":[{\"isEnabled\":true,\"fillType\":0,"
                + "\"color\":{\"red\":1,\"green\":0,\"blue\":0,\"alpha\":1}}]},\"layers\":["
                + sketchRect(0, 0, 30, 20, -1) + "," + sketchRect(10, 0, 30, 20, op) + "]}";
    }

    private static String sketchRect(int x, int y, int w, int h, int op) {
        return "{\"_class\":\"rectangle\",\"isVisible\":true,\"booleanOperation\":" + op + ","
                + "\"frame\":{\"x\":" + x + ",\"y\":" + y + ",\"width\":" + w + ",\"height\":" + h + "},"
                + "\"isClosed\":true,\"points\":["
                + "{\"point\":\"{0, 0}\",\"curveFrom\":\"{0, 0}\",\"curveTo\":\"{0, 0}\",\"curveMode\":1},"
                + "{\"point\":\"{1, 0}\",\"curveFrom\":\"{1, 0}\",\"curveTo\":\"{1, 0}\",\"curveMode\":1},"
                + "{\"point\":\"{1, 1}\",\"curveFrom\":\"{1, 1}\",\"curveTo\":\"{1, 1}\",\"curveMode\":1},"
                + "{\"point\":\"{0, 1}\",\"curveFrom\":\"{0, 1}\",\"curveTo\":\"{0, 1}\",\"curveMode\":1}]}";
    }

    private static Raster sketchBoolean(int op) throws IOException {
        String page = "{\"_class\":\"page\",\"name\":\"P\",\"frame\":{\"x\":0,\"y\":0,\"width\":40,\"height\":20},"
                + "\"layers\":[" + sketchShapeGroup(op) + "]}";
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ZipOutputStream z = new ZipOutputStream(bo);
        z.putNextEntry(new ZipEntry("document.json"));
        z.write(utf8("{\"_class\":\"document\",\"pages\":[{\"_ref\":\"pages/P1\"}]}"));
        z.putNextEntry(new ZipEntry("pages/P1.json"));
        z.write(utf8(page));
        z.putNextEntry(new ZipEntry("meta.json"));
        z.write(utf8("{\"version\":136}"));
        z.close();
        return ArtRenderer.render(ArtFormats.read(bo.toByteArray()), 1);
    }

    @Test
    void sketchBooleanOperationsKeepTheirRegion() throws IOException {
        // Rectangles 0..30 and 10..40: probe the left only, the overlap and the right only.
        Raster sub = sketchBoolean(1);
        assertEquals(0xff, sub.get(5, 10) >>> 24);
        assertEquals(0, sub.get(20, 10) >>> 24);
        assertEquals(0, sub.get(35, 10) >>> 24);
        Raster inter = sketchBoolean(2);
        assertEquals(0, inter.get(5, 10) >>> 24);
        assertEquals(0xff, inter.get(20, 10) >>> 24);
        assertEquals(0, inter.get(35, 10) >>> 24);
        Raster diff = sketchBoolean(3);
        assertEquals(0xff, diff.get(5, 10) >>> 24);
        assertEquals(0, diff.get(20, 10) >>> 24);
        assertEquals(0xff, diff.get(35, 10) >>> 24);
        Raster union = sketchBoolean(0);
        assertEquals(0xff, union.get(20, 10) >>> 24);
        assertEquals(0xff, union.get(35, 10) >>> 24);
    }

    @Test
    void svgTextPathFollowsThePath() throws IOException {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='100' height='100'>"
                + "<defs><path id='p' d='M10 10 L10 90'/></defs>"
                + "<text font-size='10'><textPath href='#p'>abc</textPath></text></svg>";
        ArtDocument d = SvgReader.read(new java.io.ByteArrayInputStream(utf8(svg)));
        java.util.List<TextNode> glyphs = new java.util.ArrayList<TextNode>();
        collect(d.getRoot(), glyphs);
        assertEquals(3, glyphs.size());
        for (TextNode t : glyphs) {
            Matrix m = t.getTransform();
            // Turned a quarter turn to run down the vertical path, and placed on it.
            assertEquals(0, m.mapX(0, 0) - 10, 0.01);
            assertTrue(m.mapY(0, 0) > 10 && m.mapY(0, 0) < 90);
            assertEquals(1, m.mapY(1, 0) - m.mapY(0, 0), 0.01);
        }
    }

    private static void collect(GroupNode g, java.util.List<TextNode> out) {
        for (ArtNode n : g.getChildren()) {
            if (n instanceof TextNode && ((TextNode) n).getTransform() != Matrix.IDENTITY) {
                out.add((TextNode) n);
            } else if (n instanceof GroupNode) {
                collect((GroupNode) n, out);
            }
        }
    }

    @Test
    void strokeEmbossLightsOnlyTheStroke() {
        ArtDocument d = new ArtDocument(40, 40);
        ShapeNode s = ShapeNode.rect(10, 10, 20, 20, 0);
        s.fill(new SolidPaint(0xff808080));
        s.getEffects().add(com.codename1.art.effects.Effects.stroke(0xff808080, 4, "outside"));
        d.getRoot().getChildren().add(s);
        Raster plain = ArtRenderer.render(d, 1);
        s.getEffects().add(com.codename1.art.effects.Effects.bevel("stroke", 4, 1, 120, 30).set("strokeWidth", 4));
        Raster lit = ArtRenderer.render(d, 1);
        assertEquals(plain.get(20, 20), lit.get(20, 20));
        boolean changed = false;
        for (int x = 6; x < 34; x++) {
            changed |= plain.get(x, 7) != lit.get(x, 7);
        }
        assertTrue(changed);
    }

    @Test
    void vibranceSaturationIsRendered() {
        ArtDocument d = new ArtDocument(4, 4);
        ShapeNode s = ShapeNode.rect(0, 0, 4, 4, 0);
        s.fill(new SolidPaint(0xffff0000));
        s.getEffects().add(com.codename1.art.effects.Effects.vibrance(0).set("saturation", -1f));
        d.getRoot().getChildren().add(s);
        int c = ArtRenderer.render(d, 1).get(1, 1);
        assertEquals((c >> 16) & 0xff, (c >> 8) & 0xff);
        assertEquals((c >> 8) & 0xff, c & 0xff);
    }

    @Test
    void svgUseExpansionIsBounded() throws IOException {
        // Ten references per level, 31 levels: 10^31 copies if nothing counted them.
        StringBuilder sb = new StringBuilder("<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><defs>");
        sb.append("<rect id='a31' width='1' height='1'/>");
        for (int l = 30; l >= 0; l--) {
            sb.append("<g id='a").append(l).append("'>");
            for (int k = 0; k < 10; k++) {
                sb.append("<use href='#a").append(l + 1).append("'/>");
            }
            sb.append("</g>");
        }
        sb.append("</defs><use href='#a0'/></svg>");
        long start = System.currentTimeMillis();
        ArtDocument d = SvgReader.read(utf8(sb.toString()));
        assertTrue(System.currentTimeMillis() - start < 20000);
        assertFalse(d.getNotes().isEmpty());
    }

    @Test
    void hostilePsdAndImagesFailCleanly() throws IOException {
        byte[] psd = PsdWriter.toBytes(twoLayers());
        // Every corruption of every byte of a real file must answer an IOException or a document.
        for (int i = 26; i < Math.min(psd.length, 600); i++) {
            byte[] bad = psd.clone();
            bad[i] = (byte) 0x80;
            try {
                ArtFormats.read(bad);
            } catch (IOException expected) {
                // fine
            }
        }
        // A PNG whose header claims 30000x30000 is refused before it is decoded.
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13, 'I', 'H', 'D', 'R',
            0, 0, 0x75, 0x30, 0, 0, 0x75, 0x30, 8, 6, 0, 0, 0};
        assertEquals(null, ArtImages.decode(png));
    }

    @Test
    void morphologyRadiusIsBoundedAndZeroDisables() throws IOException {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='20' height='20'>"
                + "<filter id='f'><feMorphology operator='dilate' radius='%s'/></filter>"
                + "<rect x='8' y='8' width='4' height='4' fill='#f00' filter='url(#f)'/></svg>";
        long start = System.currentTimeMillis();
        Raster huge = ArtRenderer.render(SvgReader.read(utf8(svg.replace("%s", "1e12"))), 1);
        assertTrue(System.currentTimeMillis() - start < 10000);
        assertEquals(0xff0000, rgb(huge, 1, 1));
        Raster neg = ArtRenderer.render(SvgReader.read(utf8(svg.replace("%s", "-3"))), 1);
        assertEquals(0xff0000, rgb(neg, 10, 10));
        assertEquals(0, neg.get(2, 2) >>> 24);
    }

    @Test
    void dropShadowKeepsItsBlendMode() {
        ArtDocument d = new ArtDocument(20, 20);
        ShapeNode back = ShapeNode.rect(0, 0, 20, 20, 0);
        back.fill(new SolidPaint(0xffffff00));
        ShapeNode s = ShapeNode.rect(2, 2, 6, 6, 0);
        s.fill(new SolidPaint(0xff0000ff));
        s.getEffects().add(com.codename1.art.effects.Effects.dropShadow(0xffff00ff, 8, 8, 0, 0)
                .set("blend", BlendMode.MULTIPLY));
        d.getRoot().getChildren().add(back);
        d.getRoot().getChildren().add(s);
        // Yellow multiplied by magenta is red; drawn normally the shadow would be magenta.
        near(0xff0000, rgb(ArtRenderer.render(d, 1), 13, 13));
    }

    @Test
    void createSvgFallbackIsAnOrdinaryImage() throws IOException {
        com.codename1.ui.Image img = ArtImages.renderSvg(utf8(
                "<svg xmlns='http://www.w3.org/2000/svg' width='8' height='4'><rect width='8' height='4' fill='#00f'/></svg>"));
        assertEquals(8, img.getWidth());
        assertFalse(img.isSVG());
        assertThrows(IOException.class, () -> ArtImages.renderSvg(utf8(
                "<svg xmlns='http://www.w3.org/2000/svg' width='9000' height='10'/>")));
    }

    @Test
    void hostileDashPatternsDrawSolid() {
        Polylines line = new Polylines();
        line.lines.add(new double[]{0, 0, 5000, 0});
        line.closed.add(Boolean.FALSE);
        assertSame(line, Stroker.dash(line, new float[]{-1, 1}, 0, 1));
        assertSame(line, Stroker.dash(line, new float[]{1e-6f, 1e-6f}, 0, 1));
        assertSame(line, Stroker.dash(line, new float[]{Float.NaN, 1}, 0, 1));
        assertTrue(Stroker.dash(line, new float[]{10, 10}, 0, 1).lines.size() > 100);
    }
}
