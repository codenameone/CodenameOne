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
package java.util.zip;

import java.io.*;
/** A stream which updates the supplied checksum. */
public class CheckedOutputStream extends FilterOutputStream {
    private final Checksum checksum;
    public CheckedOutputStream(OutputStream stream, Checksum checksum) {
        super(stream);
        if (checksum == null)
            throw new NullPointerException();
        this.checksum = checksum;
    }
    public Checksum getChecksum() {
        return checksum;
    }
    public void write(int b) throws IOException {
        out.write(b);
        checksum.update(b);
    }
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        checksum.update(b, off, len);
    }
}
