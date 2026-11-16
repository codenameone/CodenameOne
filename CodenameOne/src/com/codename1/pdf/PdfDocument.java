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
package com.codename1.pdf;

import com.codename1.impl.pdf.FontState;
import com.codename1.impl.pdf.PdfFormat;
import com.codename1.impl.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Builds a PDF file. Works the same in an application and on a Codename
/// One backend server: nothing here needs a display.
///
/// ```java
/// PdfDocument doc = new PdfDocument();
/// doc.setTitle("Invoice 2031");
/// PdfPage page = doc.addPage(PdfPageSize.A4);
/// PdfCanvas c = page.getCanvas();
/// c.setFont(PdfFont.HELVETICA_BOLD, 20);
/// c.drawText("Invoice 2031", 72, 96);
/// byte[] pdf = doc.toByteArray();
/// ```
///
/// For flowing text, tables and automatic page breaks use [PdfLayout] on
/// top of a document. A document is not safe for use by several threads
/// at once, but separate documents are independent.
@com.codename1.impl.SharedWithBackend
public final class PdfDocument {
    private final List<PdfPage> pages = new ArrayList<PdfPage>();
    private final Map<PdfFont, FontState> fonts = new HashMap<PdfFont, FontState>();
    private final List<FontState> fontOrder = new ArrayList<FontState>();
    private final Map<PdfImage, String> imageNames = new HashMap<PdfImage, String>();
    private final List<PdfImage> imageOrder = new ArrayList<PdfImage>();
    private final Map<PdfImage, Integer> imageObjects = new HashMap<PdfImage, Integer>();
    private final Map<String, String> extGStates = new HashMap<String, String>();
    private final List<String> extGStateOrder = new ArrayList<String>();
    private final List<PdfBookmark> bookmarks = new ArrayList<PdfBookmark>();
    private String title;
    private String author;
    private String subject;
    private String keywords;
    private String creator;
    private long creationDate = System.currentTimeMillis();
    private boolean compress = true;

    /// Adds a page of `size` at the end of the document.
    public PdfPage addPage(PdfPageSize size) {
        PdfPage p = new PdfPage(this, size.getWidth(), size.getHeight());
        pages.add(p);
        return p;
    }

    /// Adds a page of `width` by `height` points.
    public PdfPage addPage(float width, float height) {
        return addPage(new PdfPageSize(width, height));
    }

    /// The number of pages so far.
    public int getPageCount() {
        return pages.size();
    }

    /// Page `index`, from zero.
    public PdfPage getPage(int index) {
        return pages.get(index);
    }

    int indexOf(PdfPage page) {
        return pages.indexOf(page);
    }

    /// Adds a top-level outline entry that jumps to `y` points from the
    /// top of `page`.
    public PdfBookmark addBookmark(String title, PdfPage page, float y) {
        PdfBookmark b = new PdfBookmark(title, page, y);
        bookmarks.add(b);
        return b;
    }

    /// Sets the document title readers show in place of the file name.
    public PdfDocument setTitle(String title) {
        this.title = title;
        return this;
    }

    /// Sets the author metadata.
    public PdfDocument setAuthor(String author) {
        this.author = author;
        return this;
    }

    /// Sets the subject metadata.
    public PdfDocument setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    /// Sets the keywords metadata.
    public PdfDocument setKeywords(String keywords) {
        this.keywords = keywords;
        return this;
    }

    /// Sets the name of the application that created the content.
    public PdfDocument setCreator(String creator) {
        this.creator = creator;
        return this;
    }

    /// Sets the creation date, in milliseconds since the epoch; defaults to
    /// the time the document object was made.
    public PdfDocument setCreationDate(long millis) {
        this.creationDate = millis;
        return this;
    }

    /// Turns stream compression off, for a file that can be read in a text
    /// editor while debugging. On by default.
    public PdfDocument setCompress(boolean compress) {
        this.compress = compress;
        return this;
    }

    FontState fontState(PdfFont font) {
        FontState s = fonts.get(font);
        if (s == null) {
            s = font.createState();
            s.resourceName = "F" + (fontOrder.size() + 1);
            fonts.put(font, s);
            fontOrder.add(s);
        }
        return s;
    }

    String imageName(PdfImage image) {
        String n = imageNames.get(image);
        if (n == null) {
            n = "Im" + (imageOrder.size() + 1);
            imageNames.put(image, n);
            imageOrder.add(image);
        }
        return n;
    }

    int imageObject(PdfImage image) {
        return imageObjects.get(image).intValue();
    }

    String extGState(String key, float value) {
        StringBuilder sb = new StringBuilder("/").append(key).append(' ');
        PdfFormat.number(sb, value);
        String body = sb.toString();
        String name = extGStates.get(body);
        if (name == null) {
            name = "GS" + (extGStateOrder.size() + 1);
            extGStates.put(body, name);
            extGStateOrder.add(body);
        }
        return name;
    }

    private int[] extGStateObjects;

    String extGStateResources() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < extGStateOrder.size(); i++) {
            sb.append('/').append(extGStates.get(extGStateOrder.get(i))).append(' ')
                    .append(extGStateObjects[i]).append(" 0 R ");
        }
        return sb.toString();
    }

    /// Writes the document to `out`. The stream is flushed, not closed.
    /// A document with no pages gets one empty page, since a PDF must have
    /// at least one.
    public void save(OutputStream out) throws IOException {
        if (pages.isEmpty()) {
            addPage(PdfPageSize.A4);
        }
        PdfWriter w = new PdfWriter(out);
        int catalog = w.reserve();
        int tree = w.reserve();
        for (int i = 0; i < pages.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            pages.get(i).objectNumber = w.reserve();
        }
        for (int i = 0; i < fontOrder.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            fontOrder.get(i).objectNumber = w.reserve();
        }
        for (int i = 0; i < imageOrder.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            imageObjects.put(imageOrder.get(i), Integer.valueOf(w.reserve()));
        }
        extGStateObjects = new int[extGStateOrder.size()];
        for (int i = 0; i < extGStateObjects.length; i++) {
            extGStateObjects[i] = w.reserve();
            w.object(extGStateObjects[i], "<</Type /ExtGState " + extGStateOrder.get(i) + ">>");
        }
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            PdfPage p = pages.get(i);
            int content = w.reserve();
            w.stream(content, "", PdfFormat.ascii(p.content()), compress);
            StringBuilder sb = new StringBuilder("<</Type /Page /Parent ").append(tree).append(" 0 R /MediaBox [0 0 ");
            PdfFormat.number(sb, p.getWidth());
            sb.append(' ');
            PdfFormat.number(sb, p.getHeight());
            sb.append("] /Resources ").append(p.resources()).append(" /Contents ").append(content).append(" 0 R")
                    .append(p.annotations()).append(">>");
            w.object(p.objectNumber, sb.toString());
            kids.append(p.objectNumber).append(" 0 R ");
        }
        w.object(tree, "<</Type /Pages /Kids [" + kids + "] /Count " + pages.size() + ">>");
        for (int i = 0; i < fontOrder.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            fontOrder.get(i).write(w);
        }
        for (int i = 0; i < imageOrder.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            PdfImage img = imageOrder.get(i);
            img.write(w, imageObject(img));
        }
        int outlines = bookmarks.isEmpty() ? 0 : writeOutlines(w);
        StringBuilder cat = new StringBuilder("<</Type /Catalog /Pages ").append(tree).append(" 0 R");
        if (outlines > 0) {
            cat.append(" /Outlines ").append(outlines).append(" 0 R /PageMode /UseOutlines");
        }
        cat.append(">>");
        w.object(catalog, cat.toString());
        int info = w.reserve();
        w.object(info, info());
        w.finish(catalog, info, fileId());
    }

    /// Returns the document as the bytes of a PDF file.
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        save(bo);
        return bo.toByteArray();
    }

    private int writeOutlines(PdfWriter w) throws IOException {
        int root = w.reserve();
        reserve(w, bookmarks);
        int total = writeLevel(w, bookmarks, root);
        w.object(root, "<</Type /Outlines /First " + bookmarks.get(0).objectNumber + " 0 R /Last "
                + bookmarks.get(bookmarks.size() - 1).objectNumber + " 0 R /Count " + total + ">>");
        return root;
    }

    private static void reserve(PdfWriter w, List<PdfBookmark> level) {
        for (int i = 0; i < level.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed like the surrounding decoder loops
            PdfBookmark b = level.get(i);
            b.objectNumber = w.reserve();
            reserve(w, b.children);
        }
    }

    private int writeLevel(PdfWriter w, List<PdfBookmark> level, int parent) throws IOException {
        int count = 0;
        for (int i = 0; i < level.size(); i++) {
            PdfBookmark b = level.get(i);
            int below = b.children.isEmpty() ? 0 : writeLevel(w, b.children, b.objectNumber);
            StringBuilder sb = new StringBuilder("<</Title ");
            PdfFormat.textString(sb, b.title);
            sb.append(" /Parent ").append(parent).append(" 0 R");
            if (i > 0) {
                sb.append(" /Prev ").append(level.get(i - 1).objectNumber).append(" 0 R");
            }
            if (i + 1 < level.size()) {
                sb.append(" /Next ").append(level.get(i + 1).objectNumber).append(" 0 R");
            }
            if (below > 0) {
                sb.append(" /First ").append(b.children.get(0).objectNumber).append(" 0 R /Last ")
                        .append(b.children.get(b.children.size() - 1).objectNumber).append(" 0 R /Count ")
                        .append(-below);
            }
            sb.append(" /Dest [").append(b.page.objectNumber).append(" 0 R /XYZ 0 ");
            PdfFormat.number(sb, b.page.getHeight() - b.y);
            sb.append(" null]>>");
            w.object(b.objectNumber, sb.toString());
            count += 1 + below;
        }
        return count;
    }

    private String info() {
        StringBuilder sb = new StringBuilder("<</Producer (Codename One)");
        entry(sb, "Title", title);
        entry(sb, "Author", author);
        entry(sb, "Subject", subject);
        entry(sb, "Keywords", keywords);
        entry(sb, "Creator", creator);
        sb.append(" /CreationDate ");
        PdfFormat.literal(sb, pdfDate(creationDate));
        sb.append(">>");
        return sb.toString();
    }

    private static void entry(StringBuilder sb, String key, String value) {
        if (value != null) {
            sb.append(" /").append(key).append(' ');
            PdfFormat.textString(sb, value);
        }
    }

    /// `D:YYYYMMDDHHmmSSZ` for `millis` in UTC, computed without a
    /// calendar class (days-to-civil, Howard Hinnant's algorithm).
    static String pdfDate(long millis) {
        long secs = millis / 1000;
        if (millis < 0 && secs * 1000 != millis) {
            secs--;
        }
        long days = secs / 86400;
        long rem = secs - days * 86400;
        if (rem < 0) {
            rem += 86400;
            days--;
        }
        long z = days + 719468;
        long era = (z >= 0 ? z : z - 146096) / 146097;
        long doe = z - era * 146097;
        long yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365;
        long y = yoe + era * 400;
        long doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
        long mp = (5 * doy + 2) / 153;
        long d = doy - (153 * mp + 2) / 5 + 1;
        long m = mp < 10 ? mp + 3 : mp - 9;
        if (m <= 2) {
            y++;
        }
        StringBuilder sb = new StringBuilder("D:");
        pad(sb, y, 4);
        pad(sb, m, 2);
        pad(sb, d, 2);
        pad(sb, rem / 3600, 2);
        pad(sb, (rem / 60) % 60, 2);
        pad(sb, rem % 60, 2);
        sb.append('Z');
        return sb.toString();
    }

    private static void pad(StringBuilder sb, long v, int width) {
        String s = Long.toString(v);
        for (int i = s.length(); i < width; i++) {
            sb.append('0');
        }
        sb.append(s);
    }

    private byte[] fileId() {
        long h1 = 0x9e3779b97f4a7c15L ^ creationDate;
        long h2 = 0xc2b2ae3d27d4eb4fL + pages.size();
        String seed = String.valueOf(title) + String.valueOf(author) + System.currentTimeMillis();
        for (int i = 0; i < seed.length(); i++) {
            h1 = (h1 ^ seed.charAt(i)) * 0x100000001b3L;
            h2 = (h2 + seed.charAt(i)) * 0xff51afd7ed558ccdL;
        }
        byte[] id = new byte[16];
        for (int i = 0; i < 8; i++) {
            id[i] = (byte) (h1 >>> (i * 8));
            id[8 + i] = (byte) (h2 >>> (i * 8));
        }
        return id;
    }
}
