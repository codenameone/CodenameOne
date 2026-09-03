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


package com.codename1.impl.javase;

import com.codename1.ui.ClipboardContent;
import com.codename1.ui.ClipboardDataProvider;
import com.codename1.ui.NativeDragOperation;
import org.junit.jupiter.api.Test;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The two conversions that decide whether a desktop drag lands correctly: the
 * {@code ClipboardContent} the application offers becomes an AWT transferable other applications
 * understand, and the transferable another application drops becomes a {@code ClipboardContent}.
 *
 * <p>These run headless -- they never open a window or start a real drag -- because everything
 * platform specific about the drag is AWT's, while everything that can be wrong is in the
 * mapping.</p>
 */
class JavaSENativeDragAndDropTest {

    private static DataFlavor flavorFor(Transferable t, String mime) {
        for (DataFlavor f : t.getTransferDataFlavors()) {
            if (f.isMimeTypeEqual(mime)) {
                return f;
            }
        }
        return null;
    }

    /** A transferable that serves fixed values, standing in for another application's drag. */
    private static final class FakeTransferable implements Transferable {
        private final List<DataFlavor> flavors = new ArrayList<DataFlavor>();
        private final List<Object> values = new ArrayList<Object>();
        int reads;

        FakeTransferable add(DataFlavor flavor, Object value) {
            flavors.add(flavor);
            values.add(value);
            return this;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return flavors.toArray(new DataFlavor[flavors.size()]);
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavors.contains(flavor);
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
            int index = flavors.indexOf(flavor);
            if (index < 0) {
                throw new UnsupportedFlavorException(flavor);
            }
            reads++;
            return values.get(index);
        }
    }

    // ------------------------------------------------------------------------------------
    // Dragging out
    // ------------------------------------------------------------------------------------

    @Test
    void richTextIsOfferedInEveryFormatItWasGiven() throws Exception {
        ClipboardContent content = new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, "hello")
                .setData(ClipboardContent.MIME_HTML, "<b>hello</b>");
        Transferable t = new JavaSEPort.RichTransferable(content);

        assertTrue(t.isDataFlavorSupported(DataFlavor.stringFlavor));
        assertEquals("hello", t.getTransferData(DataFlavor.stringFlavor));

        DataFlavor html = flavorFor(t, ClipboardContent.MIME_HTML);
        assertNotNull(html, "a rich text editor asks for text/html and has to find it");
        assertEquals("<b>hello</b>", t.getTransferData(html));
    }

    @Test
    void filesAreOfferedBothAsAFileListAndAsAUriList() throws Exception {
        File a = File.createTempFile("cn1-dnd-a", ".txt");
        File b = File.createTempFile("cn1-dnd-b", ".txt");
        a.deleteOnExit();
        b.deleteOnExit();
        ClipboardContent content = new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, "two files")
                .setFiles(new String[]{a.getAbsolutePath(), b.getAbsolutePath()});
        Transferable t = new JavaSEPort.RichTransferable(content);

        assertTrue(t.isDataFlavorSupported(DataFlavor.javaFileListFlavor),
                "a drop on the desktop or in a file manager reads the file list flavor");
        List<?> files = (List<?>) t.getTransferData(DataFlavor.javaFileListFlavor);
        assertEquals(2, files.size(), "a drag of several files stays a drag of several files");
        assertEquals(a.getAbsolutePath(), ((File) files.get(0)).getAbsolutePath());

        DataFlavor uriList = flavorFor(t, ClipboardContent.MIME_URI_LIST);
        assertNotNull(uriList, "GTK targets ask for text/uri-list and nothing else");
        String uris = (String) t.getTransferData(uriList);
        assertTrue(uris.contains(a.toURI().toString()));
        assertTrue(uris.contains(b.toURI().toString()));
    }

    @Test
    void aPromisedFileIsNotBuiltUntilTheDropReadsIt() throws Exception {
        final int[] built = {0};
        final File promised = File.createTempFile("cn1-dnd-promise", ".txt");
        promised.deleteOnExit();
        ClipboardContent content = new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, "report")
                .setDataProvider(ClipboardContent.MIME_FILE, new ClipboardDataProvider() {
                    public Object getClipboardData(String mimeType) {
                        built[0]++;
                        return promised.getAbsolutePath();
                    }
                });

        Transferable t = new JavaSEPort.RichTransferable(content);
        assertTrue(t.isDataFlavorSupported(DataFlavor.javaFileListFlavor),
                "the file flavor is advertised from the MIME type alone");
        assertEquals(0, built[0],
                "starting the drag must not write the file -- the user may drop it nowhere");

        List<?> files = (List<?>) t.getTransferData(DataFlavor.javaFileListFlavor);
        assertEquals(1, files.size());
        assertEquals(1, built[0], "the drop is what builds it");
    }

    @Test
    void binaryContentIsOfferedAsAStream() throws Exception {
        byte[] pdf = new byte[]{'%', 'P', 'D', 'F'};
        ClipboardContent content = new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, "doc")
                .setData("application/pdf", pdf);
        Transferable t = new JavaSEPort.RichTransferable(content);

        DataFlavor flavor = flavorFor(t, "application/pdf");
        assertNotNull(flavor, "an arbitrary binary payload has to reach other applications somehow");
        Object value = t.getTransferData(flavor);
        assertTrue(value instanceof InputStream);
        byte[] read = new byte[4];
        ((InputStream) value).read(read);
        assertArrayEquals(pdf, read);
    }

    @Test
    void anImageEncodingNothingCanDecodeDoesNotClaimTheStandardImageFlavor() {
        ClipboardContent content = new ClipboardContent()
                .setData("image/webp", new byte[]{'R', 'I', 'F', 'F'});
        Transferable t = new JavaSEPort.RichTransferable(content);

        assertFalse(t.isDataFlavorSupported(DataFlavor.imageFlavor),
                "a desktop receiver commonly picks the standard image flavor ahead of the "
                        + "MIME specific stream, so claiming it for an encoding ImageIO cannot "
                        + "read loses the whole drop to an UnsupportedFlavorException");
        assertNotNull(flavorFor(t, "image/webp"),
                "the bytes are still perfectly readable by anything that wants that type");
    }

    @Test
    void aDecodableImageStillClaimsTheStandardImageFlavor() throws Exception {
        ClipboardContent content = new ClipboardContent()
                .setData(ClipboardContent.MIME_PNG, onePixelPng());
        Transferable t = new JavaSEPort.RichTransferable(content);

        assertTrue(t.isDataFlavorSupported(DataFlavor.imageFlavor));
        assertTrue(t.getTransferData(DataFlavor.imageFlavor) instanceof java.awt.Image);
    }

    @Test
    void theStandardImageFlavorServesAnyEncodingItWasAdvertisedFor() throws Exception {
        java.io.ByteArrayOutputStream bmp = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(
                new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_RGB),
                "bmp", bmp);
        ClipboardContent content = new ClipboardContent().setData("image/bmp", bmp.toByteArray());
        Transferable t = new JavaSEPort.RichTransferable(content);

        assertTrue(t.isDataFlavorSupported(DataFlavor.imageFlavor),
                "ImageIO reads BMP, so the flavor is advertised");
        assertTrue(t.getTransferData(DataFlavor.imageFlavor) instanceof java.awt.Image,
                "and what is advertised has to be servable -- reading only the three encodings "
                        + "the framework names left this one throwing");
    }

    private static byte[] onePixelPng() throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(
                new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB),
                "png", out);
        return out.toByteArray();
    }

    @Test
    void anUnofferedFlavorIsRefusedRatherThanAnsweredWithNull() {
        ClipboardContent content = new ClipboardContent().setData(ClipboardContent.MIME_TEXT, "hi");
        final Transferable t = new JavaSEPort.RichTransferable(content);
        assertThrows(UnsupportedFlavorException.class,
                () -> t.getTransferData(DataFlavor.javaFileListFlavor));
    }

    // ------------------------------------------------------------------------------------
    // Receiving a drop
    // ------------------------------------------------------------------------------------

    @Test
    void describingADragInProgressReadsNoData() {
        FakeTransferable t = new FakeTransferable()
                .add(DataFlavor.stringFlavor, "hello")
                .add(DataFlavor.javaFileListFlavor, Arrays.asList(new File("/tmp/a.txt")));

        ClipboardContent content = JavaSENativeDragAndDrop.contentFor(t, t.getTransferDataFlavors(), false);
        assertTrue(content.hasMimeType(ClipboardContent.MIME_TEXT));
        assertTrue(content.hasMimeType(ClipboardContent.MIME_FILE));
        assertEquals(0, t.reads,
                "a drag merely passing over the window must not pull data across; on several "
                        + "platforms the data does not exist until the drop");
    }

    @Test
    void aTextFlavorCarriedAsBytesStillReadsAsText() throws Exception {
        DataFlavor htmlBytes = new DataFlavor("text/html;charset=UTF-8;class=\"[B\"");
        FakeTransferable t = new FakeTransferable()
                .add(DataFlavor.stringFlavor, "plain")
                .add(htmlBytes, "<b>hi</b>".getBytes("UTF-8"));

        ClipboardContent content = JavaSENativeDragAndDrop.contentFor(t, t.getTransferDataFlavors(), true);
        assertEquals("<b>hi</b>", content.getText(ClipboardContent.MIME_HTML),
                "the flavor declares its own charset; storing its bytes as a binary payload "
                        + "made getText() null for a type the drop had just accepted");
    }

    @Test
    void copyingDoesNotBuildAPromisedRepresentation() {
        final int[] built = { 0 };
        ClipboardContent content = new ClipboardContent()
                .setDataProvider(ClipboardContent.MIME_TEXT, new ClipboardDataProvider() {
                    @Override
                    public Object getClipboardData(String mimeType) {
                        built[0]++;
                        return "expensive";
                    }
                });

        // Constructing a port overwrites the global JavaSEPort.instance, and other test classes
        // reach through that static to drive the live Display -- so it goes back exactly as it
        // was. JavaSEPortFontMappingTest documents the same hazard.
        JavaSEPort previous = JavaSEPort.instance;
        try {
            new JavaSEPort().copyToClipboard(content);
        } catch (Throwable headlessOrUninitialised) {
            // The clipboard itself is not reachable from a test JVM. What is under test happens
            // before that: whether putting the content on the clipboard reads it.
        } finally {
            JavaSEPort.instance = previous;
        }

        assertEquals(0, built[0],
                "a representation registered as a provider is built when a consumer reads it, "
                        + "not when something is copied -- writing the file or encoding the "
                        + "image is exactly what deferring it is for");
    }

    @Test
    void aStreamedTextFlavorIsReadOnlyOnce() throws Exception {
        DataFlavor htmlStream = new DataFlavor("text/html;charset=UTF-8;class=java.io.InputStream");
        FakeTransferable t = new FakeTransferable()
                .add(htmlStream, new ByteArrayInputStream("<b>once</b>".getBytes("UTF-8")));

        ClipboardContent content = JavaSENativeDragAndDrop.contentFor(t, t.getTransferDataFlavors(), true);
        assertEquals("<b>once</b>", content.getText(ClipboardContent.MIME_HTML));
        assertEquals(1, t.reads,
                "a source that produces its stream once loses the representation on the second "
                        + "ask, and one that produces a fresh stream transfers everything twice");
    }

    @Test
    void aTextFlavorCarriedAsAStreamStillReadsAsText() throws Exception {
        DataFlavor htmlStream = new DataFlavor("text/html;charset=UTF-16;class=java.io.InputStream");
        FakeTransferable t = new FakeTransferable()
                .add(htmlStream, new ByteArrayInputStream("<i>x</i>".getBytes("UTF-16")));

        ClipboardContent content = JavaSENativeDragAndDrop.contentFor(t, t.getTransferDataFlavors(), true);
        assertEquals("<i>x</i>", content.getText(ClipboardContent.MIME_HTML),
                "and a charset that is not UTF-8 has to be honoured rather than assumed");
    }

    @Test
    void aDroppedFileListBecomesFilePaths() {
        FakeTransferable t = new FakeTransferable()
                .add(DataFlavor.javaFileListFlavor,
                        Arrays.asList(new File("/tmp/a.txt"), new File("/tmp/b.txt")));

        ClipboardContent content = JavaSENativeDragAndDrop.contentFor(t, t.getTransferDataFlavors(), true);
        assertArrayEquals(new String[]{"/tmp/a.txt", "/tmp/b.txt"}, content.getFiles());
    }

    @Test
    void aLinkDraggedFromABrowserIsNotAFileDrag() throws Exception {
        DataFlavor uriList = new DataFlavor("text/uri-list;class=java.lang.String");
        FakeTransferable t = new FakeTransferable()
                .add(uriList, "https://www.codenameone.com/\r\n");

        ClipboardContent hovering = JavaSENativeDragAndDrop.contentFor(t, t.getTransferDataFlavors(), false);
        assertFalse(hovering.hasMimeType(ClipboardContent.MIME_FILE),
                "a URI list is not a file list, and a file-only target that lights up for one "
                        + "is refused the drop it was promised");

        ClipboardContent dropped = JavaSENativeDragAndDrop.contentFor(t, t.getTransferDataFlavors(), true);
        assertFalse(dropped.hasMimeType(ClipboardContent.MIME_FILE),
                "and the drop agrees with the hover, which is the whole point");
    }

    @Test
    void hoveringDoesNotConsumeAStreamedUriList() throws Exception {
        DataFlavor uriStream = new DataFlavor("text/uri-list;class=java.io.InputStream");
        FakeTransferable t = new FakeTransferable()
                .add(uriStream, new ByteArrayInputStream(
                        (new File("/tmp/a.txt").toURI() + "\r\n").getBytes("UTF-8")));

        JavaSENativeDragAndDrop.contentFor(t, t.getTransferDataFlavors(), false);
        JavaSENativeDragAndDrop.contentFor(t, t.getTransferDataFlavors(), false);
        assertEquals(0, t.reads,
                "the description is rebuilt for every drag event, so reading a one-shot source "
                        + "to classify it spends it on the first hover and leaves the drop with "
                        + "nothing");
    }

    @Test
    void aFileUriListIsStillAFileDragWhileItHovers() throws Exception {
        DataFlavor uriList = new DataFlavor("text/uri-list;class=java.lang.String");
        FakeTransferable t = new FakeTransferable()
                .add(uriList, new File("/tmp/a.txt").toURI() + "\r\n");

        ClipboardContent hovering = JavaSENativeDragAndDrop.contentFor(t, t.getTransferDataFlavors(), false);
        assertTrue(hovering.hasMimeType(ClipboardContent.MIME_FILE),
                "a Linux file manager offers only this spelling, and a file target has to be "
                        + "able to accept it while the drag is still hovering");
    }

    @Test
    void aDroppedFileListAlsoBecomesAUriList() {
        FakeTransferable t = new FakeTransferable()
                .add(DataFlavor.javaFileListFlavor, Arrays.asList(new File("/tmp/a.txt"), new File("/tmp/b.txt")));

        ClipboardContent content = JavaSENativeDragAndDrop.contentFor(t, t.getTransferDataFlavors(), true);
        String uris = content.getText(ClipboardContent.MIME_URI_LIST);
        assertNotNull(uris, "the Finder and Explorer offer only javaFileListFlavor, so a target "
                + "filtered to text/uri-list refused the one source every desktop user has");
        assertTrue(uris.contains("file:/"), uris);
        assertTrue(uris.contains("a.txt") && uris.contains("b.txt"), uris);
    }

    @Test
    void describingAFileDragDeclaresTheUriListWithoutReadingIt() {
        FakeTransferable t = new FakeTransferable()
                .add(DataFlavor.javaFileListFlavor, Arrays.asList(new File("/tmp/a.txt")));

        ClipboardContent content = JavaSENativeDragAndDrop.contentFor(t, t.getTransferDataFlavors(), false);
        assertTrue(content.hasMimeType(ClipboardContent.MIME_URI_LIST),
                "a hover is filtered against the advertised types, so the pair has to be "
                        + "declared before the drop as well");
        assertEquals(0, t.reads, "and declaring it must still read nothing");
    }

    @Test
    void aDroppedUriListAlsoBecomesFilePaths() throws Exception {
        DataFlavor uriList = new DataFlavor(ClipboardContent.MIME_URI_LIST + ";class=java.lang.String",
                ClipboardContent.MIME_URI_LIST);
        FakeTransferable t = new FakeTransferable()
                .add(uriList, new File("/tmp/a.txt").toURI() + "\r\n" + new File("/tmp/b.txt").toURI() + "\r\n");

        ClipboardContent content = JavaSENativeDragAndDrop.contentFor(t, t.getTransferDataFlavors(), true);
        assertArrayEquals(new String[]{"/tmp/a.txt", "/tmp/b.txt"}, content.getFiles(),
                "a drop out of a Linux file manager only offers uri-list, and still has to yield files");
    }

    @Test
    void theRichestRepresentationOfOneMimeTypeWins() throws Exception {
        DataFlavor htmlString = new DataFlavor(ClipboardContent.MIME_HTML + ";class=java.lang.String",
                ClipboardContent.MIME_HTML);
        DataFlavor htmlStream = new DataFlavor(ClipboardContent.MIME_HTML + ";class=java.io.InputStream",
                ClipboardContent.MIME_HTML);
        FakeTransferable t = new FakeTransferable()
                .add(htmlString, "<b>first</b>")
                .add(htmlStream, new ByteArrayInputStream("<b>second</b>".getBytes("UTF-8")));

        ClipboardContent content = JavaSENativeDragAndDrop.contentFor(t, t.getTransferDataFlavors(), true);
        assertEquals("<b>first</b>", content.getText(ClipboardContent.MIME_HTML),
                "AWT lists flavors in the source's preference order, so the first one is the answer");
    }

    @Test
    void actionsMapBothWays() {
        assertEquals(NativeDragOperation.ACTION_COPY,
                JavaSENativeDragAndDrop.fromAwtActions(
                        JavaSENativeDragAndDrop.toAwtActions(NativeDragOperation.ACTION_COPY)));
        int all = NativeDragOperation.ACTION_COPY | NativeDragOperation.ACTION_MOVE
                | NativeDragOperation.ACTION_LINK;
        assertEquals(all, JavaSENativeDragAndDrop.fromAwtActions(JavaSENativeDragAndDrop.toAwtActions(all)));
        assertEquals(NativeDragOperation.ACTION_COPY, JavaSENativeDragAndDrop.preferred(all),
                "a copy is preferred because it cannot destroy the source's data");
        assertEquals(NativeDragOperation.ACTION_NONE,
                JavaSENativeDragAndDrop.preferred(NativeDragOperation.ACTION_NONE));
    }
}
