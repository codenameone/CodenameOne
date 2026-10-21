/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.impl;

import com.codename1.io.FileSystemStorage;
import com.codename1.io.filechooser.FileDialogResult;
import com.codename1.io.filechooser.FileTypeFilter;
import com.codename1.io.filechooser.SaveFileRequest;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileDialogSupportTest extends UITestBase {

    @Test
    void savingOntoTheSourceFileIsRecognized() {
        assertTrue(FileDialogSupport.isSameFile("file:///home/a/r.txt", "/home/a/r.txt"));
        assertTrue(FileDialogSupport.isSameFile("/home/a/r.txt", "file:///home/a/r.txt"));
        assertFalse(FileDialogSupport.isSameFile("/home/a/r.txt", "/home/a/s.txt"));
        assertFalse(FileDialogSupport.isSameFile(null, "/home/a/r.txt"));
    }

    @Test
    void withExtensionAppendsTheFiltersFirstExtensionOnlyWhenMissing() {
        FileTypeFilter pdf = new FileTypeFilter("PDF").extensions("pdf", "PDFA");
        assertEquals("r.pdf", FileDialogSupport.withExtension("r", pdf));
        assertEquals("r.PDF", FileDialogSupport.withExtension("r.PDF", pdf));
        assertEquals("r.pdfa", FileDialogSupport.withExtension("r.pdfa", pdf));
        assertEquals("r.txt.pdf", FileDialogSupport.withExtension("r.txt", pdf));
        assertEquals("r", FileDialogSupport.withExtension("r", null));
        assertEquals("r", FileDialogSupport.withExtension("r", new FileTypeFilter("Any")));
    }

    @Test
    void onlyWindowsPathsHaveTheirBackslashesConverted() {
        assertEquals("C:/Users/a/r.txt", NativeFileDialogCodec.toUrlPath("C:\\Users\\a\\r.txt"));
        assertEquals("//server/share/r.txt", NativeFileDialogCodec.toUrlPath("\\\\server\\share\\r.txt"));
        assertEquals("/tmp/a\\b.txt", NativeFileDialogCodec.toUrlPath("/tmp/a\\b.txt"));
    }

    @FormTest
    void aFailedWriterLeavesTheExistingDocumentIntact() throws IOException {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String dest = fs.getAppHomePath() + "doc.txt";
        write(fs, dest, "original");
        SaveFileRequest request = new SaveFileRequest().content((result, out) -> {
            out.write("partial".getBytes("UTF-8"));
            throw new IOException("serialization failed");
        });
        FileDialogResult result = new FileDialogResult(request, new String[]{dest}, -1, null);
        assertThrows(IOException.class, () -> FileDialogSupport.saveStaged(request, result));
        assertEquals("original", read(fs, dest));
    }

    @FormTest
    void aSuccessfulSaveReplacesTheDocumentAndLeavesNoBackup() throws IOException {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String home = fs.getAppHomePath();
        String dest = home + "doc2.txt";
        write(fs, dest, "original");
        SaveFileRequest request = new SaveFileRequest().content((result, out) -> out.write("new".getBytes("UTF-8")));
        FileDialogSupport.saveStaged(request, new FileDialogResult(request, new String[]{dest}, -1, null));
        assertEquals("new", read(fs, dest));
        for (String f : fs.listFiles(home)) {
            assertFalse(f.contains("cn1-backup") || f.contains("cn1-save"), f);
        }
    }

    private static void write(FileSystemStorage fs, String path, String text) throws IOException {
        OutputStream out = fs.openOutputStream(path);
        try {
            out.write(text.getBytes("UTF-8"));
        } finally {
            out.close();
        }
    }

    private static String read(FileSystemStorage fs, String path) throws IOException {
        InputStream in = fs.openInputStream(path);
        try {
            return new String(com.codename1.io.Util.readInputStream(in), "UTF-8");
        } finally {
            in.close();
        }
    }
}
