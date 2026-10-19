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

import com.codename1.io.filechooser.FileTypeFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileDialogSupportTest {

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
}
