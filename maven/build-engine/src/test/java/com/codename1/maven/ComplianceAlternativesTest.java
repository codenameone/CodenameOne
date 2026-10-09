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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The table of nearest alternatives: every class and member a row names
/// has to exist in what an application is built against, and a lookup has to
/// find the most specific row.
public class ComplianceAlternativesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /// The public members of each public class of the jars an application
    /// is built against, by the class's dotted name.
    private Map<String, Set<String>> api() throws Exception {
        File scratch = tmp.newFolder();
        Map<String, Set<String>> out = new HashMap<String, Set<String>>();
        for (File jar : new File[] {RealCompatJars.core(scratch), RealCompatJars.jar("java-runtime", scratch),
            RealCompatJars.swing(scratch), RealCompatJars.javafx(scratch), RealCompatJars.jdk(scratch)}) {
            ZipFile zip = new ZipFile(jar);
            try {
                java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry e = entries.nextElement();
                    if (!e.getName().endsWith(".class")) {
                        continue;
                    }
                    final Set<String> members = new HashSet<String>();
                    final boolean[] visible = new boolean[1];
                    InputStream in = zip.getInputStream(e);
                    try {
                        new ClassReader(DependencyClassifier.readAll(in)).accept(new ClassVisitor(Opcodes.ASM9) {
                            @Override
                            public void visit(int version, int access, String name, String signature,
                                              String superName, String[] interfaces) {
                                visible[0] = (access & Opcodes.ACC_PUBLIC) != 0;
                            }

                            @Override
                            public MethodVisitor visitMethod(int access, String name, String descriptor,
                                                             String signature, String[] exceptions) {
                                if ((access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0) {
                                    members.add(name);
                                }
                                return null;
                            }

                            @Override
                            public FieldVisitor visitField(int access, String name, String descriptor,
                                                           String signature, Object value) {
                                if ((access & Opcodes.ACC_PUBLIC) != 0) {
                                    members.add(name);
                                }
                                return null;
                            }
                        }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    } finally {
                        in.close();
                    }
                    if (visible[0]) {
                        String name = e.getName().substring(0, e.getName().length() - ".class".length());
                        out.put(name.replace('/', '.').replace('$', '.'), members);
                    }
                }
            } finally {
                zip.close();
            }
        }
        return out;
    }

    @Test
    public void everyRowNamesSomethingThatExists() throws Exception {
        Map<String, Set<String>> api = api();
        List<String> missing = new ArrayList<String>();
        assertTrue("The table was read", ComplianceAlternatives.rows().size() > 50);
        for (ComplianceAlternatives.Row row : ComplianceAlternatives.rows()) {
            for (String reference : row.references()) {
                int hash = reference.indexOf('#');
                String cls = hash < 0 ? reference : reference.substring(0, hash);
                Set<String> members = api.get(cls);
                if (members == null) {
                    missing.add(row.prefix() + " names the class " + cls + ", which is not there");
                } else if (hash >= 0 && !members.contains(reference.substring(hash + 1))) {
                    missing.add(row.prefix() + " names " + reference + ", and " + cls + " has no such member");
                }
            }
        }
        assertTrue(missing.toString().replace(", ", "\n"), missing.isEmpty());
    }

    /// Advice is prose, and prose can name a class its row does not list.
    /// Every `com.codename1...` name in the text has to be among the row's
    /// references, so that the check above covers it.
    @Test
    public void adviceNamesOnlyWhatTheRowLists() {
        java.util.regex.Pattern name = java.util.regex.Pattern.compile("com\\.codename1(\\.[a-z0-9]+)*\\.[A-Z]\\w*");
        for (ComplianceAlternatives.Row row : ComplianceAlternatives.rows()) {
            java.util.regex.Matcher m = name.matcher(row.advice());
            while (m.find()) {
                boolean listed = false;
                for (String reference : row.references()) {
                    listed |= reference.equals(m.group()) || reference.startsWith(m.group() + "#");
                }
                assertTrue(row.prefix() + " advises " + m.group() + " without listing it", listed);
            }
        }
    }

    @Test
    public void aRowCoversThePrefixAndNothingBesideIt() {
        // A package covers everything in it.
        assertTrue(ComplianceAlternatives.lookup("java.sql.Connection.prepareStatement").contains("Database"));
        // A class covers itself, its members and its nested classes...
        assertTrue(ComplianceAlternatives.lookup("java.lang.ProcessBuilder").contains("CN.execute"));
        assertTrue(ComplianceAlternatives.lookup("java.lang.ProcessBuilder.start").contains("CN.execute"));
        assertTrue(ComplianceAlternatives.lookup("java.lang.ProcessBuilder.Redirect").contains("CN.execute"));
        // ...and not a class whose name merely starts the same way.
        assertEquals("java.lang.ProcessHandle", ComplianceAlternatives.row("java.lang.ProcessHandle").prefix());
        assertNull(ComplianceAlternatives.lookup("java.lang.Processor"));
        // A member covers the members that start with it, and no other.
        assertNotNull(ComplianceAlternatives.lookup("java.lang.Class.getDeclaredFields"));
        assertNull(ComplianceAlternatives.lookup("java.lang.Class.getName"));
        assertNull(ComplianceAlternatives.lookup("java.lang.Runtime.availableProcessors"));
        assertNull(ComplianceAlternatives.lookup("java.lang.String.length"));
        assertNull(ComplianceAlternatives.lookup(null));
    }

    @Test
    public void theMostSpecificRowWins() {
        assertEquals("java.nio.file.WatchService", ComplianceAlternatives.row("java.nio.file.WatchService.take")
                .prefix());
        assertEquals("java.nio.file.", ComplianceAlternatives.row("java.nio.file.Files.readString").prefix());
        assertEquals("java.security.MessageDigest", ComplianceAlternatives.row("java.security.MessageDigest.digest")
                .prefix());
        assertEquals("java.security.", ComplianceAlternatives.row("java.security.KeyFactory").prefix());
    }

    /// What the package asks the table to cover at the least.
    @Test
    public void theGapsADesktopApplicationHitsAllHaveARow() {
        for (String api : new String[] {"java.lang.ProcessBuilder", "java.lang.Runtime.exec",
            "java.net.http.HttpClient", "java.net.HttpURLConnection", "java.net.Socket",
            "java.nio.file.WatchService", "java.lang.reflect.Method.invoke", "java.io.ObjectOutputStream",
            "javax.crypto.Cipher", "java.security.MessageDigest", "java.sql.DriverManager",
            "java.util.prefs.Preferences", "java.lang.Thread.sleep", "java.awt.Robot", "java.awt.SystemTray",
            "java.awt.Taskbar", "java.awt.print.PrinterJob", "java.awt.dnd.DropTarget", "javax.swing.JEditorPane",
            "java.lang.Class.getResource", "com.fasterxml.jackson.databind.ObjectMapper"}) {
            assertNotNull(api, ComplianceAlternatives.lookup(api));
        }
    }

    @Test
    public void aLineThatIsNoRowIsAnError() {
        assertEquals(1, ComplianceAlternatives.parse("# note\n\na.b. | | No equivalent.\n").size());
        try {
            ComplianceAlternatives.parse("a.b. | No equivalent.\n");
            fail("Two columns are not a row");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains(":1:"));
        }
        try {
            ComplianceAlternatives.parse("a.b. | x.Y |\n");
            fail("A row without advice says nothing");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("required"));
        }
    }
}
