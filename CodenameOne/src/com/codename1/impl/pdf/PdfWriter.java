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
package com.codename1.impl.pdf;

import com.codename1.io.gzip.DeflaterOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/// Writes the file structure of a PDF: header, numbered indirect objects,
/// the cross-reference table and the trailer. Objects are written as soon
/// as they are complete; only their offsets are kept.
@com.codename1.impl.SharedWithBackend
public final class PdfWriter {
    private final OutputStream out;
    private long position;
    private long[] offsets = new long[64];
    private int nextObject = 1;

    /// Starts a PDF 1.7 file on `out`.
    public PdfWriter(OutputStream out) throws IOException {
        this.out = out;
        write("%PDF-1.7\n");
        // A comment of high bytes marks the file as binary for transfer tools.
        byte[] marker = {'%', (byte) 0xe2, (byte) 0xe3, (byte) 0xcf, (byte) 0xd3, '\n'};
        write(marker);
    }

    /// Reserves the next object number without writing anything.
    public int reserve() {
        int n = nextObject++;
        if (n >= offsets.length) {
            long[] grown = new long[offsets.length * 2];
            System.arraycopy(offsets, 0, grown, 0, offsets.length);
            offsets = grown;
        }
        return n;
    }

    /// Writes object `number`, whose body is `body` (a dictionary, array or
    /// any other direct object).
    public void object(int number, String body) throws IOException {
        offsets[number] = position;
        write(number + " 0 obj\n");
        write(body);
        write("\nendobj\n");
    }

    /// Writes stream object `number`. `dict` holds the entries without
    /// `/Length` and `/Filter`; with `compress` the data is deflated and
    /// `/Filter /FlateDecode` added, unless `dict` already names a filter,
    /// in which case the data is written as it is.
    public void stream(int number, String dict, byte[] data, boolean compress) throws IOException {
        byte[] body = data;
        String filter = "";
        if (compress && dict.indexOf("/Filter") < 0) {
            body = deflate(data);
            filter = " /Filter /FlateDecode";
        }
        offsets[number] = position;
        write(number + " 0 obj\n<<" + dict + filter + " /Length " + body.length + ">>\nstream\n");
        write(body);
        write("\nendstream\nendobj\n");
    }

    /// Writes the cross-reference table and trailer and flushes. `info`
    /// is 0 when the file has no information dictionary.
    public void finish(int root, int info, byte[] id) throws IOException {
        long xref = position;
        StringBuilder sb = new StringBuilder();
        sb.append("xref\n0 ").append(nextObject).append('\n');
        sb.append("0000000000 65535 f\r\n");
        for (int i = 1; i < nextObject; i++) {
            String off = Long.toString(offsets[i]);
            for (int pad = off.length(); pad < 10; pad++) {
                sb.append('0');
            }
            sb.append(off).append(" 00000 n\r\n");
        }
        sb.append("trailer\n<</Size ").append(nextObject).append(" /Root ").append(root).append(" 0 R");
        if (info > 0) {
            sb.append(" /Info ").append(info).append(" 0 R");
        }
        if (id != null) {
            sb.append(" /ID [");
            PdfFormat.hex(sb, id);
            PdfFormat.hex(sb, id);
            sb.append(']');
        }
        sb.append(">>\nstartxref\n").append(xref).append("\n%%EOF\n");
        write(sb.toString());
        out.flush();
    }

    /// Returns `data` compressed with zlib, as FlateDecode expects.
    public static byte[] deflate(byte[] data) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream(data.length / 2 + 64);
        DeflaterOutputStream d = new DeflaterOutputStream(bo);
        d.write(data);
        d.close();
        return bo.toByteArray();
    }

    private void write(String ascii) throws IOException {
        write(PdfFormat.ascii(ascii));
    }

    private void write(byte[] b) throws IOException {
        out.write(b);
        position += b.length;
    }
}
