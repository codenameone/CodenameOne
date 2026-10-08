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
package com.codename1.desktopcompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.desktopcompat.java.awt.Desktop;
import com.codename1.desktopcompat.java.awt.Image;
import com.codename1.desktopcompat.java.awt.Toolkit;
import com.codename1.desktopcompat.java.awt.datatransfer.Clipboard;
import com.codename1.desktopcompat.java.awt.datatransfer.ClipboardOwner;
import com.codename1.desktopcompat.java.awt.datatransfer.DataFlavor;
import com.codename1.desktopcompat.java.awt.datatransfer.StringSelection;
import com.codename1.desktopcompat.java.awt.datatransfer.Transferable;
import com.codename1.desktopcompat.java.awt.datatransfer.UnsupportedFlavorException;
import com.codename1.desktopcompat.java.awt.image.BufferedImage;
import com.codename1.desktopcompat.javax.imageio.ImageIO;
import com.codename1.desktopcompat.javax.swing.ImageIcon;
import com.codename1.desktopcompat.rt.Launcher;
import com.codename1.ui.Display;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/// The services a desktop application takes from its toolkit: opening a
/// URL elsewhere, the clipboard, and reading and writing image files.
public class DesktopServicesTest extends KernelTestBase {

    private final List<String> opened = new ArrayList<String>();
    private File png;

    @Before
    public void seams() throws IOException {
        Launcher.setOpener(new Launcher.Opener() {
            @Override
            public void open(String url) {
                opened.add(url);
            }
        });
        HeadlessImplementation.pixelImages = true;
        File dir = new File("target/swing-hard-images");
        dir.mkdirs();
        png = new File(dir, "two-by-three.png");
        FileOutputStream out = new FileOutputStream(png);
        try {
            out.write(pngBytes());
        } finally {
            out.close();
        }
    }

    @After
    public void reset() {
        Launcher.setOpener(null);
        HeadlessImplementation.pixelImages = false;
        HeadlessImplementation.imageIO = null;
    }

    /// A 2 by 3 PNG, red but for one blue pixel at (1, 2), encoded by the
    /// JDK.
    private static byte[] pngBytes() throws IOException {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(2, 3,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 2; x++) {
                img.setRGB(x, y, 0xffff0000);
            }
        }
        img.setRGB(1, 2, 0xff0000ff);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    // ---------------------------------------------------------- Desktop

    @Test
    public void theDesktopHandsUrlsToTheDevice() throws Exception {
        assertTrue(Desktop.isDesktopSupported());
        Desktop d = Desktop.getDesktop();
        assertSame(d, Desktop.getDesktop());
        assertTrue(d.isSupported(Desktop.Action.BROWSE));
        assertTrue(d.isSupported(Desktop.Action.MAIL));
        assertTrue(d.isSupported(Desktop.Action.OPEN));
        assertFalse(d.isSupported(Desktop.Action.PRINT));
        d.browse(new URI("https://example.com/a?b=c"));
        d.mail(new URI("mailto:someone@example.com?subject=Hi"));
        d.mail();
        d.open(new File("/docs/report.pdf"));
        assertEquals("[https://example.com/a?b=c, mailto:someone@example.com?subject=Hi, mailto:, "
                + "file:///docs/report.pdf]", opened.toString());
        try {
            d.mail(new URI("https://example.com"));
            fail("not a mailto URI");
        } catch (IllegalArgumentException expected) {
            assertEquals(4, opened.size());
        }
        try {
            d.browse(null);
            fail("null URI");
        } catch (NullPointerException expected) {
            assertEquals(4, opened.size());
        }
    }

    // -------------------------------------------------------- clipboard

    @Test
    public void theSystemClipboardIsTheDevices() throws Exception {
        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        assertSame(c, Toolkit.getDefaultToolkit().getSystemClipboard());
        final List<Transferable> lost = new ArrayList<Transferable>();
        ClipboardOwner owner = new ClipboardOwner() {
            @Override
            public void lostOwnership(Clipboard clipboard, Transferable contents) {
                lost.add(contents);
            }
        };
        StringSelection mine = new StringSelection("copied here");
        c.setContents(mine, owner);
        assertEquals("the device has it", "copied here", Display.getInstance().getPasteDataFromClipboard());
        assertEquals("copied here", c.getData(DataFlavor.stringFlavor));
        assertSame(mine, c.getContents(null));
        assertTrue(c.isDataFlavorAvailable(DataFlavor.stringFlavor));
        assertEquals(1, c.getAvailableDataFlavors().length);

        // Something copied elsewhere is what the clipboard holds then.
        Display.getInstance().copyToClipboard("copied elsewhere");
        assertEquals("copied elsewhere", c.getData(DataFlavor.stringFlavor));
        assertTrue(c.getContents(null) instanceof StringSelection);

        // Putting something else tells the owner of what was there.
        StringSelection next = new StringSelection("next");
        c.setContents(next, next);
        assertEquals(1, lost.size());
        assertSame(mine, lost.get(0));
        assertEquals("next", Display.getInstance().getPasteDataFromClipboard());
    }

    @Test
    public void aClipboardOfTheApplicationsOwnHoldsWhatItIsGiven() throws Exception {
        Clipboard c = new Clipboard("mine");
        assertEquals("mine", c.getName());
        assertNull(c.getContents(this));
        assertEquals(0, c.getAvailableDataFlavors().length);
        try {
            c.getData(DataFlavor.stringFlavor);
            fail("empty");
        } catch (UnsupportedFlavorException expected) {
            assertNotNull(expected);
        }
        StringSelection s = new StringSelection("text");
        c.setContents(s, null);
        assertEquals("text", c.getData(DataFlavor.stringFlavor));
        DataFlavor html = new DataFlavor("text/HTML; charset=utf-8", "HTML");
        assertFalse(c.isDataFlavorAvailable(html));
        assertTrue(html.isMimeTypeEqual("text/html"));
        assertEquals("text", html.getPrimaryType());
        assertEquals("html", html.getSubType());
        try {
            s.getTransferData(html);
            fail("not offered");
        } catch (UnsupportedFlavorException expected) {
            assertEquals("HTML", expected.getMessage());
        }
        assertEquals(new java.awt.datatransfer.StringSelection("x").getTransferDataFlavors()[0]
                .getHumanPresentableName(), DataFlavor.stringFlavor.getHumanPresentableName());
        assertEquals(java.awt.datatransfer.DataFlavor.stringFlavor.getMimeType(),
                DataFlavor.stringFlavor.getMimeType());
    }

    // ----------------------------------------------------------- images

    @Test
    public void imagesAreReadFromFilesUrlsAndBytes() throws Exception {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Image fromFile = tk.getImage(png.getPath());
        assertEquals(2, fromFile.getWidth(null));
        assertEquals(3, fromFile.getHeight(null));
        Image fromUrl = tk.getImage(png.toURI().toURL());
        assertEquals(2, fromUrl.getWidth(null));
        assertEquals(3, tk.createImage(png.getPath()).getHeight(null));
        assertEquals(3, tk.createImage(png.toURI().toURL()).getHeight(null));
        assertEquals(2, tk.createImage(pngBytes()).getWidth(null));

        assertEquals(2, new ImageIcon(png.getPath()).getIconWidth());
        assertEquals(3, new ImageIcon(png.toURI().toURL()).getIconHeight());
        assertEquals(png.toURI().toURL().toExternalForm(), new ImageIcon(png.toURI().toURL()).getDescription());
        assertEquals(2, new ImageIcon(pngBytes()).getIconWidth());

        // What cannot be read is an image without a size, not an error.
        Image missing = tk.getImage("target/swing-hard-images/no-such-file.png");
        assertNotNull(missing);
        assertEquals(-1, missing.getWidth(null));
        assertEquals(-1, missing.getHeight(null));
        ImageIcon none = new ImageIcon("target/swing-hard-images/no-such-file.png");
        assertEquals(-1, none.getIconWidth());
        assertNull(none.getImage());
        assertEquals(-1, new ImageIcon(new File("target/swing-hard-images/none.png").toURI().toURL()).getIconWidth());
    }

    @Test
    public void imageIoReadsABufferedImage() throws Exception {
        BufferedImage fromFile = ImageIO.read(png);
        assertEquals(2, fromFile.getWidth());
        assertEquals(3, fromFile.getHeight());
        assertEquals(0xffff0000, fromFile.getRGB(0, 0));
        assertEquals(0xff0000ff, fromFile.getRGB(1, 2));
        BufferedImage fromUrl = ImageIO.read(png.toURI().toURL());
        assertEquals(0xff0000ff, fromUrl.getRGB(1, 2));
        InputStream in = new ByteArrayInputStream(pngBytes());
        BufferedImage fromStream = ImageIO.read(in);
        assertEquals(0xffff0000, fromStream.getRGB(1, 1));

        assertNull("not an image", ImageIO.read(new ByteArrayInputStream(new byte[]{1, 2, 3, 4})));
        try {
            ImageIO.read(new File("target/swing-hard-images/no-such-file.png"));
            fail("no such file");
        } catch (IOException expected) {
            assertEquals("Can't read input file!", expected.getMessage());
        }
        try {
            ImageIO.read((InputStream) null);
            fail("null stream");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected);
        }
    }

    @Test
    public void imageIoWritesWithTheDevicesEncoder() throws Exception {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        img.setRGB(0, 0, 0xff00ff00);
        img.setRGB(1, 1, 0xff123456);
        File out = new File("target/swing-hard-images/written.png");
        out.delete();

        // A device without an encoder writes nothing and says so.
        assertFalse(ImageIO.write(img, "png", out));
        assertFalse(out.exists());

        final List<String> formats = new ArrayList<String>();
        HeadlessImplementation.imageIO = new com.codename1.ui.util.ImageIO() {
            @Override
            public void save(InputStream image, OutputStream response, String format, int width, int height,
                    float quality) throws IOException {
                throw new IOException("not used");
            }

            @Override
            protected void saveImage(com.codename1.ui.Image image, OutputStream response, String format,
                    float quality) throws IOException {
                formats.add(format);
                int w = image.getWidth();
                int h = image.getHeight();
                java.awt.image.BufferedImage real = new java.awt.image.BufferedImage(w, h,
                        java.awt.image.BufferedImage.TYPE_INT_ARGB);
                real.setRGB(0, 0, w, h, image.getRGB(), 0, w);
                javax.imageio.ImageIO.write(real, "png", response);
            }

            @Override
            public boolean isFormatSupported(String format) {
                return true;
            }
        };
        assertTrue(ImageIO.write(img, "PNG", out));
        assertTrue(out.length() > 0);
        BufferedImage back = ImageIO.read(out);
        assertEquals(0xff00ff00, back.getRGB(0, 0));
        assertEquals(0xff123456, back.getRGB(1, 1));

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(img, "jpg", bytes));
        assertTrue(ImageIO.write(img, "JPEG", bytes));
        assertEquals("[png, jpeg, jpeg]", formats.toString());
        assertTrue(bytes.size() > 0);

        // A format nothing writes, as on the desktop.
        assertFalse(ImageIO.write(img, "tiff-like", bytes));
        try {
            ImageIO.write(null, "png", bytes);
            fail("null image");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected);
        }
    }
}
