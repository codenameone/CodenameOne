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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Settings tool (and every desktop tool binding files through
 * FileSystemStorage) round-trips native paths through file:// URLs. On Windows
 * the stripped form used to keep a leading slash before the drive letter
 * (/C:\Users\...), which java.io.File resolves drive-relative (C:\C:\...) so
 * every read silently failed (#5443). These tests pin the tolerant decoding.
 */
public class JavaSEPortFileUrlTest {

    private JavaSEPort originalInstance;
    private boolean originalExposeFilesystem;
    private JavaSEPort port;

    @BeforeEach
    public void captureInstance() {
        // The port constructor overwrites the global JavaSEPort.instance which
        // other test classes reach through; capture and restore it so this
        // test can't cause order-dependent failures.
        originalInstance = JavaSEPort.instance;
        originalExposeFilesystem = JavaSEPort.isExposeFilesystem();
        port = new JavaSEPort();
        JavaSEPort.setExposeFilesystem(true);
    }

    @AfterEach
    public void restoreInstance() {
        JavaSEPort.instance = originalInstance;
        JavaSEPort.setExposeFilesystem(originalExposeFilesystem);
    }

    @Test
    public void stripsLeadingSlashFromWindowsDrivePaths() {
        assertEquals("C:/Users/dev/app", JavaSEPort.stripWindowsDriveSlash("/C:/Users/dev/app"));
        assertEquals("C:\\Users\\dev\\app", JavaSEPort.stripWindowsDriveSlash("/C:\\Users\\dev\\app"));
        assertEquals("c:/temp", JavaSEPort.stripWindowsDriveSlash("/c:/temp"));
    }

    @Test
    public void leavesUnixPathsUntouched() {
        assertEquals("/Users/dev/app", JavaSEPort.stripWindowsDriveSlash("/Users/dev/app"));
        assertEquals("/tmp", JavaSEPort.stripWindowsDriveSlash("/tmp"));
        assertEquals("", JavaSEPort.stripWindowsDriveSlash(""));
    }

    @Test
    public void unfileHandlesWindowsDriveLetterUrls() {
        assertEquals("C:/Users/dev/app", port.unfile("file:///C:/Users/dev/app"));
        assertEquals("C:\\Users\\dev\\app", port.unfile("file://C:\\Users\\dev\\app"));
    }

    @Test
    public void unfileHandlesUnixUrls() {
        assertEquals("/Users/dev/app", port.unfile("file:///Users/dev/app"));
        assertEquals("/Users/dev/app", port.unfile("file:/Users/dev/app"));
    }

    @Test
    public void unfileToleratesSpacesAndLiteralPercent() {
        // Spaces are common in Windows user directories; a literal % (not an
        // encoded escape) used to make the URI-decode branch throw a
        // RuntimeException that killed the caller.
        assertEquals("C:/Users/John Smith/My App",
                port.unfile("file:///C:/Users/John Smith/My App"));
        assertEquals("C:/Users/100% dev/app",
                port.unfile("file:///C:/Users/100% dev/app"));
    }

    @Test
    public void unfileStillDecodesProperlyEncodedUrls() {
        // getAbsolutePath prepends the current drive on Windows, so only pin
        // the decoded tail of the path.
        String decoded = port.unfile("file:///tmp/some%20dir/file.txt");
        assertTrue(decoded.replace('\\', '/').endsWith("/tmp/some dir/file.txt"),
                "expected decoded path, got: " + decoded);
    }
}
