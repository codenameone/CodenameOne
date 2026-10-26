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
package com.codenameone.examples.bastion.ui;

import com.codename1.io.gzip.DeflaterOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// A PDF of whole-page pictures, one per page: what [Printout] hands to the
/// print dialog. Each page is a sheet of the given size in points with one
/// RGB image on it, deflated, filling the sheet. Nothing else of PDF is
/// written -- no fonts, no text -- so a reader of any version opens it.
final class PdfPages {
    private final int widthPoints;
    private final int heightPoints;
    private final List<byte[]> images = new ArrayList<byte[]>();
    private final List<int[]> sizes = new ArrayList<int[]>();

    PdfPages(int widthPoints, int heightPoints) {
        this.widthPoints = widthPoints;
        this.heightPoints = heightPoints;
    }

    /// Adds a page showing `argb`, `width` by `height` pixels, row by row
    /// from the top; alpha is ignored.
    void add(int[] argb, int width, int height) throws IOException {
        ByteArrayOutputStream packed = new ByteArrayOutputStream();
        DeflaterOutputStream deflate = new DeflaterOutputStream(packed);
        byte[] row = new byte[width * 3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = argb[y * width + x];
                row[x * 3] = (byte) (pixel >> 16);
                row[x * 3 + 1] = (byte) (pixel >> 8);
                row[x * 3 + 2] = (byte) pixel;
            }
            deflate.write(row);
        }
        deflate.close();
        images.add(packed.toByteArray());
        sizes.add(new int[] {width, height});
    }

    int pageCount() {
        return images.size();
    }

    /// The document. Objects: 1 the catalog, 2 the page tree, then three per
    /// page -- the page, its drawing, its image.
    byte[] toBytes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<Integer>();
        ascii(out, "%PDF-1.4\n");
        offsets.add(Integer.valueOf(out.size()));
        ascii(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        StringBuilder kids = new StringBuilder();
        for (int iter = 0; iter < images.size(); iter++) {
            kids.append(3 + iter * 3).append(" 0 R ");
        }
        offsets.add(Integer.valueOf(out.size()));
        ascii(out, "2 0 obj\n<< /Type /Pages /Kids [" + kids + "] /Count " + images.size()
                + " >>\nendobj\n");
        for (int iter = 0; iter < images.size(); iter++) {
            int page = 3 + iter * 3;
            int[] size = sizes.get(iter);
            byte[] image = images.get(iter);
            String draw = "q " + widthPoints + " 0 0 " + heightPoints + " 0 0 cm /P Do Q\n";
            offsets.add(Integer.valueOf(out.size()));
            ascii(out, page + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + widthPoints
                    + " " + heightPoints + "] /Resources << /XObject << /P " + (page + 2)
                    + " 0 R >> >> /Contents " + (page + 1) + " 0 R >>\nendobj\n");
            offsets.add(Integer.valueOf(out.size()));
            ascii(out, (page + 1) + " 0 obj\n<< /Length " + draw.length() + " >>\nstream\n" + draw
                    + "endstream\nendobj\n");
            offsets.add(Integer.valueOf(out.size()));
            ascii(out, (page + 2) + " 0 obj\n<< /Type /XObject /Subtype /Image /Width " + size[0]
                    + " /Height " + size[1] + " /ColorSpace /DeviceRGB /BitsPerComponent 8"
                    + " /Filter /FlateDecode /Length " + image.length + " >>\nstream\n");
            out.write(image);
            ascii(out, "\nendstream\nendobj\n");
        }
        int xref = out.size();
        ascii(out, "xref\n0 " + (offsets.size() + 1) + "\n0000000000 65535 f \n");
        for (Integer at : offsets) {
            ascii(out, pad10(at.intValue()) + " 00000 n \n");
        }
        ascii(out, "trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\nstartxref\n"
                + xref + "\n%%EOF\n");
        return out.toByteArray();
    }

    private static String pad10(int value) {
        String text = String.valueOf(value);
        while (text.length() < 10) {
            text = "0" + text;
        }
        return text;
    }

    /// Everything but the image data is ASCII, written a byte per character.
    private static void ascii(ByteArrayOutputStream out, String text) {
        for (int iter = 0; iter < text.length(); iter++) {
            out.write(text.charAt(iter));
        }
    }
}
