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

package com.codename1.tools.translator;

import java.io.*;

/**
 * Created by san on 2/13/16.
 */
public class ConcatenatingFileOutputStream extends java.io.OutputStream {

    public static final int MODULO = 32;

    ByteArrayOutputStream []dest = new ByteArrayOutputStream[MODULO];
    ByteArrayOutputStream current;
    private File outputDirectory;

    public ConcatenatingFileOutputStream(File outputDirectory) {

        this.outputDirectory = outputDirectory;
    }

    public void beginNextFile(String fileid) {
        int destIndex = Math.abs(fileid.hashCode() % MODULO);
        current = dest[destIndex];
        if (current == null) {
            current = dest[destIndex] = new ByteArrayOutputStream();
        }
    }

    @Override
    public void write(int b) throws IOException {
        if (current == null) {
            throw new RuntimeException("beginNextFile() not called.");
        }
        current.write(b);
    }

    @Override
    public void close() {
        current.write('\n');
    }

    public void realClose() throws IOException {
        for (int i = 0; i < dest.length; i++) {
            ByteArrayOutputStream byteArrayOutputStream = dest[i];
            File destFile = new File(outputDirectory, "concatenated_" + i+"."+ByteCodeTranslator.output.extension());
            if (dest == null || dest[i].size() ==0) {
                destFile.delete();
            } else {
                FileOutputStream pfos = new FileOutputStream(destFile);
                pfos.write(byteArrayOutputStream.toByteArray());
                pfos.close();
            }
        }
    }
}
