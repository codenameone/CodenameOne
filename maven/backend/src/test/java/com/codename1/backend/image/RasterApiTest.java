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
package com.codename1.backend.image;

import org.junit.jupiter.api.Test;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The pure-Java image API, checked against the JDK's own codecs where
/// there is one to compare with.
class RasterApiTest {

    private static RasterImage noise(int w, int h, boolean alpha, long seed) {
        Random r = new Random(seed);
        RasterImage img = new RasterImage(w, h);
        int[] p = img.getPixels();
        for (int i = 0; i < p.length; i++) {
            p[i] = r.nextInt() | (alpha ? 0 : 0xff000000);
        }
        return img;
    }

    private static RasterImage smooth(int w, int h) {
        RasterImage img = new RasterImage(w, h);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int r = x * 255 / (w - 1);
                int g = y * 255 / (h - 1);
                int b = (x + y) * 255 / (w + h - 2);
                img.setRGB(x, y, 0xff000000 | r << 16 | g << 8 | b);
            }
        }
        return img;
    }

    private static BufferedImage toAwt(RasterImage img) {
        BufferedImage bi = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_RGB);
        bi.setRGB(0, 0, img.getWidth(), img.getHeight(), img.getPixels(), 0, img.getWidth());
        return bi;
    }

    /// Mean and max per-channel difference, ignoring alpha.
    private static double[] diff(RasterImage a, BufferedImage b) {
        double sum = 0;
        int max = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                int p = a.getRGB(x, y);
                int q = b.getRGB(x, y);
                for (int s = 0; s < 24; s += 8) {
                    int d = Math.abs((p >> s & 0xff) - (q >> s & 0xff));
                    sum += d;
                    max = Math.max(max, d);
                }
            }
        }
        return new double[] {sum / (a.getWidth() * a.getHeight() * 3.0), max};
    }

    private static byte[] jpegWithImageIO(BufferedImage bi, boolean progressive) throws IOException {
        ImageWriter w = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam p = w.getDefaultWriteParam();
        p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        p.setCompressionQuality(0.95f);
        if (progressive) {
            p.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
        }
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        MemoryCacheImageOutputStream out = new MemoryCacheImageOutputStream(bo);
        w.setOutput(out);
        w.write(null, new IIOImage(bi, null, null), p);
        out.close();
        w.dispose();
        return bo.toByteArray();
    }

    @Test
    void pngRoundTripKeepsEveryPixel() throws IOException {
        RasterImage a = noise(37, 23, true, 1);
        assertArrayEquals(a.getPixels(), PngCodec.decode(PngCodec.encode(a)).getPixels());
        RasterImage b = noise(19, 41, false, 2);
        byte[] png = PngCodec.encode(b);
        assertEquals(2, png[8 + 8 + 9], "opaque images are written as RGB");
        assertArrayEquals(b.getPixels(), PngCodec.decode(png).getPixels());
    }

    @Test
    void pngWrittenByTheJdkDecodes() throws IOException {
        RasterImage src = noise(29, 17, false, 3);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ImageIO.write(toAwt(src), "png", bo);
        assertArrayEquals(src.getPixels(), PngCodec.decode(bo.toByteArray()).getPixels());

        byte[] r = {0, (byte) 255, 0, 10};
        byte[] g = {0, 0, (byte) 255, 20};
        byte[] b = {0, 0, 0, 30};
        byte[] a = {(byte) 255, (byte) 255, (byte) 255, 0};
        BufferedImage pal = new BufferedImage(6, 3, BufferedImage.TYPE_BYTE_INDEXED, new IndexColorModel(2, 4, r, g, b, a));
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 6; x++) {
                pal.getRaster().setSample(x, y, 0, (x + y) % 4);
            }
        }
        bo.reset();
        ImageIO.write(pal, "png", bo);
        RasterImage d = PngCodec.decode(bo.toByteArray());
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 6; x++) {
                int i = (x + y) % 4;
                int expected = i == 3 ? 0x000a141e : pal.getRGB(x, y);
                assertEquals(expected, d.getRGB(x, y), "palette pixel " + x + "," + y);
            }
        }

        BufferedImage gray = new BufferedImage(9, 4, BufferedImage.TYPE_USHORT_GRAY);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 9; x++) {
                gray.getRaster().setSample(x, y, 0, x * 7000 + y * 300);
            }
        }
        bo.reset();
        ImageIO.write(gray, "png", bo);
        RasterImage g16 = PngCodec.decode(bo.toByteArray());
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 9; x++) {
                int v = (x * 7000 + y * 300) >> 8;
                assertEquals(0xff000000 | v << 16 | v << 8 | v, g16.getRGB(x, y));
            }
        }
    }

    @Test
    void adam7InterlacedPngDecodes() throws IOException {
        int w = 11;
        int h = 7;
        RasterImage src = noise(w, h, false, 4);
        int[][] passes = {{0, 0, 8, 8}, {4, 0, 8, 8}, {0, 4, 4, 8}, {2, 0, 4, 4}, {0, 2, 2, 4}, {1, 0, 2, 2}, {0, 1, 1, 2}};
        ByteArrayOutputStream rows = new ByteArrayOutputStream();
        for (int[] p : passes) {
            for (int y = p[1]; y < h; y += p[3]) {
                if (p[0] >= w) {
                    break;
                }
                rows.write(0);
                for (int x = p[0]; x < w; x += p[2]) {
                    int c = src.getRGB(x, y);
                    rows.write(c >> 16 & 0xff);
                    rows.write(c >> 8 & 0xff);
                    rows.write(c & 0xff);
                }
            }
        }
        byte[] png = PngCodec.encodeRows(w, h, 8, 2, null, null, rows.toByteArray());
        png[8 + 8 + 12] = 1;
        assertArrayEquals(src.getPixels(), PngCodec.decode(png).getPixels());
    }

    @Test
    void jpegFromTheJdkDecodesBaselineAndProgressive() throws IOException {
        RasterImage src = smooth(70, 45);
        BufferedImage bi = toAwt(src);
        for (boolean progressive : new boolean[] {false, true}) {
            byte[] jpg = jpegWithImageIO(bi, progressive);
            RasterImage mine = JpegCodec.decode(jpg);
            BufferedImage theirs = ImageIO.read(new ByteArrayInputStream(jpg));
            double[] d = diff(mine, theirs);
            assertTrue(d[0] < 2.0 && d[1] <= 24, "progressive=" + progressive + " mean " + d[0] + " max " + d[1]);
            assertEquals(3, JpegCodec.componentCount(jpg));
        }
    }

    @Test
    void jpegWeWriteIsReadByTheJdk() throws IOException {
        RasterImage src = smooth(53, 31);
        byte[] jpg = JpegCodec.encode(src, 0.9f);
        BufferedImage theirs = ImageIO.read(new ByteArrayInputStream(jpg));
        assertEquals(53, theirs.getWidth());
        double[] d = diff(src, theirs);
        assertTrue(d[0] < 3.0, "mean " + d[0]);
        double[] own = diff(JpegCodec.decode(jpg), theirs);
        assertTrue(own[1] <= 2, "our decoder agrees with the JDK's on our file: " + own[1]);

        byte[] grayJpg = JpegCodec.encode(src, 0.75f, true);
        assertEquals(1, JpegCodec.componentCount(grayJpg));
        int c = JpegCodec.decode(grayJpg).getRGB(10, 10);
        assertEquals(c >> 16 & 0xff, c & 0xff);
        assertThrows(IOException.class, () -> JpegCodec.decode(new byte[] {1, 2, 3, 4}));
    }

    @Test
    void resizeKeepsSolidColorAndDoesNotBleedTransparency() {
        int[] filters = {Resampler.NEAREST, Resampler.BILINEAR, Resampler.BICUBIC, Resampler.LANCZOS,
            Resampler.BOX, Resampler.HAMMING};
        RasterImage solid = new RasterImage(9, 7);
        solid.fill(0xff336699);
        RasterImage half = new RasterImage(8, 8);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                half.setRGB(x, y, x < 4 ? 0xffff0000 : 0x0000ff00);
            }
        }
        for (int f : filters) {
            RasterImage s = solid.scale(4, 13, f);
            for (int p : s.getPixels()) {
                assertEquals(0xff336699, p, "filter " + f);
            }
            RasterImage h = half.scale(5, 5, f);
            for (int p : h.getPixels()) {
                if (p >>> 24 != 0) {
                    assertEquals(0, p >> 8 & 0xff, "no green from transparent pixels, filter " + f);
                }
            }
        }
    }

    @Test
    void rotateFlipAndSubImage() {
        RasterImage a = noise(3, 2, true, 5);
        RasterImage r = a.rotate90();
        assertEquals(2, r.getWidth());
        assertEquals(a.getRGB(0, 0), r.getRGB(1, 0));
        assertEquals(a.getRGB(0, 1), r.getRGB(0, 0));
        assertArrayEquals(a.getPixels(), r.rotate270().getPixels());
        assertArrayEquals(a.rotate180().getPixels(), a.flipHorizontal().flipVertical().getPixels());
        assertEquals(a.getRGB(2, 1), a.subImage(1, 1, 2, 1).getRGB(1, 0));
        assertThrows(IllegalArgumentException.class, () -> a.getRGB(3, 0));
    }

    @Test
    void fillsAreExactOnPixelEdgesAndAntialiasedBetween() {
        RasterImage img = new RasterImage(20, 20);
        RasterGraphics g = img.createGraphics();
        g.setColor(0xff0000ff);
        g.fillRect(2, 2, 3, 3);
        assertEquals(0xff0000ff, img.getRGB(2, 2));
        assertEquals(0xff0000ff, img.getRGB(4, 4));
        assertEquals(0, img.getRGB(5, 4));
        assertEquals(0, img.getRGB(1, 2));
        g.fillRect(10.5, 10, 1, 1);
        int a = img.getRGB(10, 10) >>> 24;
        assertTrue(a > 120 && a < 136, "half covered pixel alpha " + a);

        g.setColor(0xffff0000);
        g.drawLine(0, 15.5, 20, 15.5);
        assertEquals(0xffff0000, img.getRGB(7, 15));
        assertEquals(0, img.getRGB(7, 14));
        assertEquals(0, img.getRGB(7, 16));
    }

    @Test
    void ovalsStrokesAndWindingRules() {
        RasterImage img = new RasterImage(40, 40);
        RasterGraphics g = img.createGraphics();
        g.setColor(0xff00ff00);
        g.fillOval(0, 0, 40, 40);
        assertEquals(0xff00ff00, img.getRGB(20, 20));
        assertEquals(0, img.getRGB(1, 1));

        RasterImage ring = new RasterImage(30, 30);
        RasterGraphics rg = ring.createGraphics();
        RasterPath p = RasterPath.rect(0, 0, 30, 30).append(RasterPath.rect(10, 10, 10, 10));
        rg.setColor(0xff000000);
        rg.fill(p);
        assertEquals(0xff000000, ring.getRGB(15, 15), "non-zero: same winding fills the hole");
        ring.fill(0);
        rg.setWindingRule(RasterGraphics.WIND_EVEN_ODD);
        rg.fill(p);
        assertEquals(0, ring.getRGB(15, 15), "even-odd leaves the hole");
        assertEquals(0xff000000, ring.getRGB(5, 5));

        RasterImage s = new RasterImage(30, 30);
        RasterGraphics sg = s.createGraphics();
        sg.setColor(0xff000000);
        sg.setStrokeWidth(4);
        sg.setLineJoin(RasterGraphics.JOIN_MITER);
        sg.drawRect(5, 5, 20, 20);
        assertEquals(0xff000000, s.getRGB(3, 3), "miter fills the corner");
        assertEquals(0xff000000, s.getRGB(15, 6));
        assertEquals(0, s.getRGB(15, 15));
        s.fill(0);
        sg.setLineJoin(RasterGraphics.JOIN_BEVEL);
        sg.drawRect(5, 5, 20, 20);
        assertEquals(0, s.getRGB(3, 3), "bevel cuts the corner");
        s.fill(0);
        sg.setLineCap(RasterGraphics.CAP_SQUARE);
        sg.drawLine(10, 15, 20, 15);
        assertEquals(0xff000000, s.getRGB(8, 15));
        s.fill(0);
        sg.setLineCap(RasterGraphics.CAP_BUTT);
        sg.drawLine(10, 15, 20, 15);
        assertEquals(0, s.getRGB(8, 15));
        sg.setStrokeWidth(2);
        sg.draw(new RasterPath().moveTo(2, 2).quadTo(15, 30, 28, 2));
        sg.draw(new RasterPath().moveTo(2, 28).curveTo(10, 0, 20, 0, 28, 28));
        sg.drawArc(5, 5, 20, 20, 0, 180);
    }

    @Test
    void transformClipAlphaGradientAndImages() {
        RasterImage img = new RasterImage(20, 20);
        RasterGraphics g = img.createGraphics();
        g.translate(10, 0);
        g.scale(2, 2);
        g.setColor(0xffffffff);
        g.fillRect(0, 0, 2, 2);
        assertEquals(0xffffffff, img.getRGB(13, 3));
        assertEquals(0, img.getRGB(9, 3));
        g.resetTransform();

        g.setClip(0, 0, 5, 5);
        g.setColor(0xff123456);
        g.fillRect(0, 0, 20, 20);
        assertEquals(0xff123456, img.getRGB(4, 4));
        assertEquals(0, img.getRGB(5, 5));
        g.resetClip();

        img.fill(0xff000000);
        g.setAlpha(0.5f);
        g.setColor(0xffffffff);
        g.fillRect(0, 0, 20, 20);
        int v = img.getRGB(10, 10) & 0xff;
        assertTrue(v >= 127 && v <= 128, "half white over black: " + v);
        g.setAlpha(1);

        g.setPaint(RasterGradient.linear(0, 0, 20, 0, new float[] {0, 1}, new int[] {0xff000000, 0xffffffff}));
        g.fillRect(0, 0, 20, 20);
        assertTrue((img.getRGB(1, 5) & 0xff) < (img.getRGB(18, 5) & 0xff));
        g.setPaint(RasterGradient.radial(10, 10, 10, new float[] {0, 1}, new int[] {0xffff0000, 0xff0000ff}));
        g.fillRect(0, 0, 20, 20);
        assertTrue((img.getRGB(10, 10) >> 16 & 0xff) > 200);
        g.setPaint(null);

        RasterImage tile = new RasterImage(2, 2);
        tile.fill(0xff00ff00);
        g.drawImage(tile, 4, 4, 8, 8);
        assertEquals(0xff00ff00, img.getRGB(7, 7));
        assertThrows(IllegalStateException.class, () -> g.drawString("x", 0, 0));
        g.setFont(new RasterFont() {
            public RasterPath getOutline(String text, double x, double y) {
                return RasterPath.rect(x, y - 5, 5 * text.length(), 5);
            }

            public double stringWidth(String text) {
                return 5 * text.length();
            }

            public double getAscent() {
                return 5;
            }

            public double getDescent() {
                return 0;
            }
        });
        g.setColor(0xffabcdef);
        g.drawString("ab", 0, 19);
        assertEquals(0xffabcdef, img.getRGB(8, 16));
    }
}
