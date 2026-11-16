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

import com.codename1.impl.pdf.PdfContent;
import com.codename1.impl.pdf.PdfDict;
import com.codename1.impl.pdf.PdfFile;
import com.codename1.impl.pdf.PdfTextExtractor;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/// Reads an existing PDF: its pages, their sizes, its metadata and its
/// text. Runs headless, so it works on a server as well as on a device.
///
/// ```java
/// PdfReader r = PdfReader.open(bytes);
/// for (int i = 0; i < r.getPageCount(); i++) {
///     String text = r.extractText(i);
/// }
/// ```
///
/// Files with a broken cross-reference table are repaired as they are
/// read. Encrypted files that use the standard security handler (RC4,
/// AES-128 or AES-256) open with the user or owner password, and those
/// encrypted with an empty user password open without one.
@com.codename1.impl.SharedWithBackend
public final class PdfReader {
    private final PdfFile file;

    private PdfReader(PdfFile file) {
        this.file = file;
    }

    /// Opens a PDF held in memory.
    ///
    /// @throws IOException if the data is not a PDF, is damaged beyond
    /// repair, or is encrypted with a password other than the empty one
    public static PdfReader open(byte[] data) throws IOException {
        return open(data, null);
    }

    /// Opens an encrypted PDF with `password`, the user or owner password.
    ///
    /// @throws IOException if the data cannot be read or the password is wrong
    public static PdfReader open(byte[] data, String password) throws IOException {
        return new PdfReader(new PdfFile(data, password));
    }

    /// Reads `in` to its end and opens it. The stream is not closed.
    public static PdfReader open(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) {
            bo.write(buf, 0, n);
        }
        return open(bo.toByteArray(), null);
    }

    /// The number of pages.
    public int getPageCount() {
        return file.getPages().size();
    }

    /// True when the file was encrypted.
    public boolean isEncrypted() {
        return file.isEncrypted();
    }

    private PdfDict page(int index) {
        List<PdfDict> pages = file.getPages();
        if (index < 0 || index >= pages.size()) {
            throw new IndexOutOfBoundsException("Page " + index + " of " + pages.size());
        }
        return pages.get(index);
    }

    /// The rotation of page `index` in clockwise degrees: 0, 90, 180 or 270.
    public int getPageRotation(int index) {
        return PdfContent.rotation(page(index));
    }

    /// The width of page `index` in points as displayed, after rotation.
    public double getPageWidth(int index) {
        PdfDict p = page(index);
        double[] b = PdfContent.box(p);
        return PdfContent.rotation(p) % 180 == 0 ? b[2] : b[3];
    }

    /// The height of page `index` in points as displayed, after rotation.
    public double getPageHeight(int index) {
        PdfDict p = page(index);
        double[] b = PdfContent.box(p);
        return PdfContent.rotation(p) % 180 == 0 ? b[3] : b[2];
    }

    /// The text of page `index`, in drawing order, with lines separated by `\n`.
    public String extractText(int index) {
        return PdfTextExtractor.extract(page(index));
    }

    /// The text of every page, pages separated by a form feed (`\f`).
    public String extractText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getPageCount(); i++) {
            if (i > 0) {
                sb.append('\f');
            }
            sb.append(extractText(i));
        }
        return sb.toString();
    }

    private String info(String key) {
        PdfDict info = file.getInfo();
        return info == null ? null : info.getText(key);
    }

    /// The document title, or null.
    public String getTitle() {
        return info("Title");
    }

    /// The author, or null.
    public String getAuthor() {
        return info("Author");
    }

    /// The subject, or null.
    public String getSubject() {
        return info("Subject");
    }

    /// The keywords, or null.
    public String getKeywords() {
        return info("Keywords");
    }

    /// The application that created the original document, or null.
    public String getCreator() {
        return info("Creator");
    }

    /// The application that wrote the PDF, or null.
    public String getProducer() {
        return info("Producer");
    }

    /// The parsed file, for the client renderer.
    PdfFile file() {
        return file;
    }
}
