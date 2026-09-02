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
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class UnzipUtility {
    /**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;
    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     * @param zipFilePath
     * @param destDirectory
     * @throws IOException
     */
    public void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        // Canonical, because that is what resolves the "../" an archive can carry.
        Path destRoot = destDir.getCanonicalFile().toPath();
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        try {
            ZipEntry entry = zipIn.getNextEntry();
            // iterates over entries in the zip file
            while (entry != null) {
                // ZIP SLIP. An entry name is attacker-controlled and may be
                // "../../something": concatenating it onto the destination writes
                // wherever the archive says, which for these two callers means an
                // arbitrary file overwritten under the user's account while they
                // believe they are unpacking Groovy or JavaFX. Refuse anything that
                // does not land inside the destination.
                //
                // Path.startsWith compares COMPONENT-wise, not character-wise, so
                // "/tmp/dest-evil" is correctly rejected against "/tmp/dest" -- a
                // plain String.startsWith accepts it unless the prefix is given a
                // trailing separator, and then it wrongly rejects the destination
                // itself. Neither trap exists here.
                Path target = new File(destDir, entry.getName()).getCanonicalFile().toPath();
                if (!target.startsWith(destRoot)) {
                    throw new IOException("Zip entry escapes the destination directory: "
                            + entry.getName());
                }
                File targetFile = target.toFile();
                if (!entry.isDirectory()) {
                    // Nested entries can arrive before the directory that holds
                    // them, and FileOutputStream will not create it.
                    File parent = targetFile.getParentFile();
                    if (parent != null) {
                        parent.mkdirs();
                    }
                    extractFile(zipIn, targetFile);
                } else {
                    targetFile.mkdirs();
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        } finally {
            zipIn.close();
        }
    }
    /**
     * Extracts a zip entry (file entry)
     * @param zipIn
     * @param target
     * @throws IOException
     */
    private void extractFile(ZipInputStream zipIn, File target) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target));
        try {
            byte[] bytesIn = new byte[BUFFER_SIZE];
            int read = 0;
            while ((read = zipIn.read(bytesIn)) != -1) {
                bos.write(bytesIn, 0, read);
            }
        } finally {
            bos.close();
        }
    }
}
