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
package java.io;

/**
 * The stream behind System.in. Not a FileInputStream: standard input is not
 * seekable, so neither skip nor available can be answered by seeking, and
 * InputStream's defaults (skip by reading, available 0) are the correct answers
 * here. This mirrors NSLogOutputStream, which plays the same role for System.out.
 */
public class StandardInputStream extends InputStream {
    public int read() throws IOException {
        byte[] one = new byte[1];
        int n = read(one, 0, 1);
        if(n <= 0) {
            return -1;
        }
        return one[0] & 0xff;
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if(b == null) {
            throw new NullPointerException();
        }
        if(off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        }
        if(len == 0) {
            return 0;
        }
        int n = readImpl(b, off, len);
        if(n < -1) {
            throw new IOException("Read failed");
        }
        return n;
    }

    private static native int readImpl(byte[] buffer, int offset, int length);
}
