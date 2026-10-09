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
package com.codename1.maven;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/// How the compliance check's findings are grouped: by where they are, by
/// what kind of API they are about, and each API once.
public class ComplianceFindingsTest {

    private static ComplianceFindings sample() {
        ComplianceFindings f = new ComplianceFindings();
        // The application: one JDK API used three times in two files...
        f.add(null, "com/acme/Git.java", 40, "java/lang/ProcessBuilder", "java.lang.ProcessBuilder.start()",
                "java.lang.ProcessBuilder.start");
        f.add(null, "com/acme/Git.java", 12, "java/lang/ProcessBuilder", "java.lang.ProcessBuilder.start()",
                "java.lang.ProcessBuilder.start");
        f.add(null, "com/acme/Diff.java", 7, "java/lang/ProcessBuilder", "java.lang.ProcessBuilder.start()",
                "java.lang.ProcessBuilder.start");
        // ...a second of another family, a toolkit class and a record.
        f.add(null, "com/acme/Git.java", 50, "java/nio/file/Files", "java.nio.file.Files.readString(Path)",
                "java.nio.file.Files.readString");
        f.add(null, "com/acme/Main.java", 9, "java/awt/Robot", "java.awt.Robot", null);
        f.addLanguage(null, "com/acme/Point.java", 0, "records (java.lang.runtime.ObjectMethods)", "Write a class.");
        // Two libraries, the smaller one first.
        f.add("small-1.jar", "org/small/A.class", 0, "java/net/Socket", "java.net.Socket", null);
        f.add("big-2.jar", "org/big/A.class", 3, "java/net/Socket", "java.net.Socket", null);
        f.add("big-2.jar", "org/big/B.class", 0, "java/lang/reflect/Method", "java.lang.reflect.Method.invoke()",
                "java.lang.reflect.Method.invoke");
        f.add("big-2.jar", "org/big/B.class", 0, "org/missing/Thing", "org.missing.Thing", null);
        return f;
    }

    @Test
    public void everyApiIsListedOnceWithItsUses() {
        ComplianceFindings f = sample();
        assertEquals(10, f.total());
        // java.net.Socket is one API although two libraries use it.
        assertEquals(7, f.apiCount());
        ComplianceFindings.Source app = f.application();
        assertNull(app.library());
        assertEquals(6, app.callSites());
        assertEquals(4, app.apiCount());

        List<ComplianceFindings.Api> jdk = app.apis(ComplianceFindings.Category.JDK);
        assertEquals(2, jdk.size());
        // The most used first.
        ComplianceFindings.Api start = jdk.get(0);
        assertEquals("java.lang.ProcessBuilder.start()", start.name());
        assertEquals(3, start.count());
        assertEquals("processes", start.family());
        assertNotNull(start.alternative());
        assertEquals("com/acme/Diff.java:7; com/acme/Git.java:12, 40", ComplianceFindings.locations(start.uses(), 12));
        assertEquals("com/acme/Diff.java:7; com/acme/Git.java:12; and 1 more",
                ComplianceFindings.locations(start.uses(), 2));
        assertEquals("files", jdk.get(1).family());

        assertEquals(1, app.apis(ComplianceFindings.Category.TOOLKIT).size());
        assertEquals(1, app.apis(ComplianceFindings.Category.LANGUAGE).size());
        assertEquals("Write a class.", app.apis(ComplianceFindings.Category.LANGUAGE).get(0).alternative());
        assertTrue(app.apis(ComplianceFindings.Category.LIBRARY).isEmpty());
        assertEquals(4, app.callSites(ComplianceFindings.Category.JDK));
        // What to change, by the file it is in.
        assertEquals("[com/acme/Diff.java, com/acme/Git.java, com/acme/Main.java, com/acme/Point.java]",
                app.byFile().keySet().toString());
        assertEquals(2, app.byFile().get("com/acme/Git.java").size());
    }

    @Test
    public void theApplicationComesFirstAndThenTheLibrariesByFindings() {
        ComplianceFindings f = sample();
        assertEquals(3, f.sources().size());
        assertNull(f.sources().get(0).library());
        assertEquals("big-2.jar", f.libraries().get(0).library());
        assertEquals("small-1.jar", f.libraries().get(1).library());
        assertEquals(1, f.libraries().get(0).apis(ComplianceFindings.Category.LIBRARY).size());

        // A source without findings is not listed.
        ComplianceFindings onlyLibrary = new ComplianceFindings();
        assertTrue(onlyLibrary.isEmpty());
        onlyLibrary.add("small-1.jar", "org/small/A.class", 0, "java/net/Socket", "java.net.Socket", null);
        assertEquals(1, onlyLibrary.sources().size());
    }

    @Test
    public void aClassIsFiledByWhatItIs() {
        assertEquals(ComplianceFindings.Category.TOOLKIT, ComplianceFindings.categoryOf("javax/swing/JTable"));
        assertEquals(ComplianceFindings.Category.TOOLKIT, ComplianceFindings.categoryOf("javafx/scene/web/WebView"));
        assertEquals(ComplianceFindings.Category.UI_LIBRARY, ComplianceFindings.categoryOf("org/jdesktop/swingx/JXTable"));
        assertEquals(ComplianceFindings.Category.JDK, ComplianceFindings.categoryOf("java/sql/Connection"));
        assertEquals(ComplianceFindings.Category.JDK, ComplianceFindings.categoryOf("sun/misc/Unsafe"));
        assertEquals(ComplianceFindings.Category.LIBRARY, ComplianceFindings.categoryOf("com/sun/jna/Native"));
        assertEquals(ComplianceFindings.Category.LIBRARY, ComplianceFindings.categoryOf("org/slf4j/Logger"));
        assertEquals(ComplianceFindings.Category.OTHER, ComplianceFindings.categoryOf(null));

        assertEquals("serialization", ComplianceFindings.familyOf(ComplianceFindings.Category.JDK,
                "java/io/ObjectOutputStream"));
        assertEquals("files and streams", ComplianceFindings.familyOf(ComplianceFindings.Category.JDK,
                "java/io/FileChannel"));
        assertEquals("security and crypto", ComplianceFindings.familyOf(ComplianceFindings.Category.JDK,
                "javax/crypto/Cipher"));
        assertEquals("org.slf4j", ComplianceFindings.familyOf(ComplianceFindings.Category.LIBRARY,
                "org/slf4j/Logger"));
        assertEquals("org.apache.commons", ComplianceFindings.familyOf(ComplianceFindings.Category.LIBRARY,
                "org/apache/commons/io/FileUtils"));
    }

    @Test
    public void theConsoleGetsOneLineForEachSourceAndCategory() {
        assertEquals("10 references to 7 APIs a device does not have:\n"
                + "  The application: 6 call sites, 4 APIs\n"
                + "    Swing, AWT and JavaFX API the compatibility layers lack: 1 API, 1 call site -- java.awt.Robot 1\n"
                + "    JDK API a device lacks: 2 APIs, 4 call sites -- processes 3, files 1\n"
                + "    Language features a device cannot run: 1 API, 1 call site -- "
                + "records (java.lang.runtime.ObjectMethods) 1\n"
                + "  big-2.jar: 3 call sites, 3 APIs -- networking 1, org.missing 1, reflection 1\n"
                + "  small-1.jar: 1 call site, 1 API -- networking 1", sample().summary());
    }

    @Test
    public void theFileNamesEachApiOnceWithWhereAndWhatInstead() {
        String report = sample().report();
        assertTrue(report, report.startsWith("The application: 6 call sites of 4 missing APIs\n"
                + "==============================================="));
        assertTrue(report, report.contains("\nJDK API a device lacks (2 APIs, 4 call sites)\n"
                + "  by family: processes 3, files 1\n"
                + "\n"
                + "  3 x java.lang.ProcessBuilder.start()\n"
                + "      com/acme/Diff.java:7; com/acme/Git.java:12, 40\n"
                + "      Instead: "));
        assertTrue(report, report.contains("\nbig-2.jar: 3 call sites of 3 missing APIs\n"));
        assertTrue(report, report.contains("  1 x org.missing.Thing\n      org/big/B.class\n"));
        assertEquals("Each API of a source is listed once", report.indexOf("x java.lang.ProcessBuilder.start()"),
                report.lastIndexOf("x java.lang.ProcessBuilder.start()"));
        assertTrue("The application comes before any library",
                report.indexOf("The application:") < report.indexOf("big-2.jar:"));
        assertTrue(report.indexOf("big-2.jar:") < report.indexOf("small-1.jar:"));
    }
}
