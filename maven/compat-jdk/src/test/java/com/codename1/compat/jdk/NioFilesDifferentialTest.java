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
package com.codename1.compat.jdk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// `Files`, run step by step through this module's class on a Codename One
/// file system and through the JDK's on this machine's, from the same script.
/// Each step's answer -- or the exception it ended in -- has to be the same.
public class NioFilesDifferentialTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static java.io.File deviceRoot;

    @BeforeClass
    public static void startCodenameOne() throws IOException {
        deviceRoot = java.nio.file.Files.createTempDirectory("cn1-nio").toFile();
        DiskFileSystem.install(deviceRoot);
    }

    /// One operation of `Files`, on paths given relative to a working
    /// directory, answering text.
    private interface Ops {
        String op(String name, String a, String b, String text) throws IOException;
    }

    private static String sorted(Iterator<?> it, Object base) {
        List<String> out = new ArrayList<String>();
        while (it.hasNext()) {
            out.add(relative(base, it.next()));
        }
        Collections.sort(out);
        return out.toString();
    }

    private static String relative(Object base, Object path) {
        if (base instanceof Path) {
            return ((Path) base).relativize((Path) path).toString();
        }
        return ((java.nio.file.Path) base).relativize((java.nio.file.Path) path).toString();
    }

    private static final class Jdk implements Ops {
        private final java.nio.file.Path base;

        Jdk(java.nio.file.Path base) {
            this.base = base;
        }

        @Override
        public String op(String name, String a, String b, String text) throws IOException {
            final java.nio.file.Path p = base.resolve(a);
            java.nio.file.Path q = b == null ? null : base.resolve(b);
            byte[] bytes = text == null ? null : text.getBytes(StandardCharsets.UTF_8);
            switch (name) {
                case "state":
                    return java.nio.file.Files.exists(p) + "," + java.nio.file.Files.notExists(p) + ","
                            + java.nio.file.Files.isDirectory(p) + "," + java.nio.file.Files.isRegularFile(p) + ","
                            + java.nio.file.Files.isSymbolicLink(p) + "," + java.nio.file.Files.isReadable(p) + ","
                            + java.nio.file.Files.isWritable(p);
                case "size":
                    return String.valueOf(java.nio.file.Files.size(p));
                case "attrs":
                    java.nio.file.attribute.BasicFileAttributes at = java.nio.file.Files.readAttributes(p,
                            java.nio.file.attribute.BasicFileAttributes.class);
                    return at.isDirectory() + "," + at.isRegularFile() + "," + at.isSymbolicLink() + ","
                            + at.isOther() + "," + (at.isDirectory() ? 0 : at.size()) + ","
                            + (at.lastModifiedTime().toMillis() > 0);
                case "write":
                    return relative(base, java.nio.file.Files.write(p, bytes));
                case "writeCreateNew":
                    return relative(base, java.nio.file.Files.write(p, bytes,
                            java.nio.file.StandardOpenOption.CREATE_NEW));
                case "writeAppend":
                    return relative(base, java.nio.file.Files.write(p, bytes,
                            java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND));
                case "writeNoCreate":
                    return relative(base, java.nio.file.Files.write(p, bytes,
                            java.nio.file.StandardOpenOption.WRITE));
                case "writeTruncate":
                    return relative(base, java.nio.file.Files.write(p, bytes,
                            java.nio.file.StandardOpenOption.TRUNCATE_EXISTING));
                case "writeString":
                    return relative(base, java.nio.file.Files.write(p, bytes));
                case "writeLines":
                    return relative(base, java.nio.file.Files.write(p, Arrays.asList(text.split(",")),
                            StandardCharsets.UTF_8));
                case "read":
                    return new String(java.nio.file.Files.readAllBytes(p), StandardCharsets.UTF_8).replace(
                            System.lineSeparator(), "\n");
                case "readString":
                    return new String(java.nio.file.Files.readAllBytes(p), StandardCharsets.UTF_8);
                case "readLatin":
                    return new String(java.nio.file.Files.readAllBytes(p), StandardCharsets.ISO_8859_1);
                case "lines":
                    return java.nio.file.Files.readAllLines(p).toString() + java.nio.file.Files.lines(p).count();
                case "reader":
                    java.io.BufferedReader r = java.nio.file.Files.newBufferedReader(p);
                    try {
                        return r.readLine() + "/" + r.readLine();
                    } finally {
                        r.close();
                    }
                case "writer":
                    java.io.BufferedWriter w = java.nio.file.Files.newBufferedWriter(p);
                    w.write(text);
                    w.close();
                    return "ok";
                case "stream":
                    OutputStream out = java.nio.file.Files.newOutputStream(p);
                    out.write(bytes);
                    out.close();
                    InputStream in = java.nio.file.Files.newInputStream(p);
                    try {
                        return String.valueOf(in.read());
                    } finally {
                        in.close();
                    }
                case "createFile":
                    return relative(base, java.nio.file.Files.createFile(p));
                case "createDirectory":
                    return relative(base, java.nio.file.Files.createDirectory(p));
                case "createDirectories":
                    return relative(base, java.nio.file.Files.createDirectories(p));
                case "delete":
                    java.nio.file.Files.delete(p);
                    return "ok";
                case "deleteIfExists":
                    return String.valueOf(java.nio.file.Files.deleteIfExists(p));
                case "copy":
                    return relative(base, java.nio.file.Files.copy(p, q));
                case "copyReplace":
                    return relative(base, java.nio.file.Files.copy(p, q,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING));
                case "copyIn":
                    return String.valueOf(java.nio.file.Files.copy(new ByteArrayInputStream(bytes), p));
                case "copyOut":
                    ByteArrayOutputStream sink = new ByteArrayOutputStream();
                    return java.nio.file.Files.copy(p, sink) + ":" + sink.toString("UTF-8");
                case "move":
                    return relative(base, java.nio.file.Files.move(p, q));
                case "moveReplace":
                    return relative(base, java.nio.file.Files.move(p, q,
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING));
                case "mismatch":
                    byte[] mx = java.nio.file.Files.readAllBytes(p);
                    byte[] my = java.nio.file.Files.readAllBytes(q);
                    int n = Math.min(mx.length, my.length);
                    for (int i = 0; i < n; i++) {
                        if (mx[i] != my[i]) {
                            return String.valueOf(i);
                        }
                    }
                    return String.valueOf(mx.length == my.length ? -1 : n);
                case "same":
                    return String.valueOf(java.nio.file.Files.isSameFile(p, q));
                case "list":
                    return sorted(java.nio.file.Files.list(p).iterator(), base);
                case "walk":
                    return sorted(java.nio.file.Files.walk(p).iterator(), base);
                case "walk1":
                    return sorted(java.nio.file.Files.walk(p, 1).iterator(), base);
                case "find":
                    return sorted(java.nio.file.Files.find(p, 9, (x, at2) -> at2.isRegularFile()
                            && x.getFileName().toString().endsWith(".txt")).iterator(), base);
                case "dir":
                    java.nio.file.DirectoryStream<java.nio.file.Path> ds = java.nio.file.Files.newDirectoryStream(p);
                    try {
                        return sorted(ds.iterator(), base);
                    } finally {
                        ds.close();
                    }
                case "glob":
                    java.nio.file.DirectoryStream<java.nio.file.Path> gs =
                            java.nio.file.Files.newDirectoryStream(p, text);
                    try {
                        return sorted(gs.iterator(), base);
                    } finally {
                        gs.close();
                    }
                case "tree":
                    final List<String> log = new ArrayList<String>();
                    final String stop = text;
                    java.nio.file.Files.walkFileTree(p, new java.nio.file.SimpleFileVisitor<java.nio.file.Path>() {
                        @Override
                        public java.nio.file.FileVisitResult preVisitDirectory(java.nio.file.Path d,
                                java.nio.file.attribute.BasicFileAttributes at3) {
                            log.add("pre " + relative(p, d));
                            return d.getFileName().toString().equals(stop)
                                    ? java.nio.file.FileVisitResult.SKIP_SUBTREE
                                    : java.nio.file.FileVisitResult.CONTINUE;
                        }

                        @Override
                        public java.nio.file.FileVisitResult visitFile(java.nio.file.Path f,
                                java.nio.file.attribute.BasicFileAttributes at3) {
                            log.add("file " + relative(p, f));
                            return java.nio.file.FileVisitResult.CONTINUE;
                        }

                        @Override
                        public java.nio.file.FileVisitResult postVisitDirectory(java.nio.file.Path d,
                                IOException e) {
                            log.add("post " + relative(p, d));
                            return java.nio.file.FileVisitResult.CONTINUE;
                        }
                    });
                    Collections.sort(log);
                    return log.toString();
                default:
                    throw new IllegalStateException(name);
            }
        }
    }

    private static final class Shim implements Ops {
        private final Path base;

        Shim(Path base) {
            this.base = base;
        }

        @Override
        public String op(String name, String a, String b, String text) throws IOException {
            final Path p = base.resolve(a);
            Path q = b == null ? null : base.resolve(b);
            byte[] bytes = text == null ? null : text.getBytes(StandardCharsets.UTF_8);
            switch (name) {
                case "state":
                    return Files.exists(p) + "," + Files.notExists(p) + "," + Files.isDirectory(p) + ","
                            + Files.isRegularFile(p) + "," + Files.isSymbolicLink(p) + "," + Files.isReadable(p)
                            + "," + Files.isWritable(p);
                case "size":
                    return String.valueOf(Files.size(p));
                case "attrs":
                    BasicFileAttributes at = Files.readAttributes(p, BasicFileAttributes.class);
                    return at.isDirectory() + "," + at.isRegularFile() + "," + at.isSymbolicLink() + ","
                            + at.isOther() + "," + (at.isDirectory() ? 0 : at.size()) + ","
                            + (at.lastModifiedTime().toMillis() > 0);
                case "write":
                    return relative(base, Files.write(p, bytes));
                case "writeCreateNew":
                    return relative(base, Files.write(p, bytes, StandardOpenOption.CREATE_NEW));
                case "writeAppend":
                    return relative(base, Files.write(p, bytes, StandardOpenOption.CREATE,
                            StandardOpenOption.APPEND));
                case "writeNoCreate":
                    return relative(base, Files.write(p, bytes, StandardOpenOption.WRITE));
                case "writeTruncate":
                    return relative(base, Files.write(p, bytes, StandardOpenOption.TRUNCATE_EXISTING));
                case "writeString":
                    return relative(base, Files.writeString(p, text));
                case "writeLines":
                    return relative(base, Files.write(p, Arrays.asList(text.split(",")), StandardCharsets.UTF_8));
                case "read":
                    return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
                case "readString":
                    return Files.readString(p);
                case "readLatin":
                    return Files.readString(p, StandardCharsets.ISO_8859_1);
                case "lines":
                    return Files.readAllLines(p).toString() + Files.lines(p).count();
                case "reader":
                    BufferedReader r = Files.newBufferedReader(p);
                    try {
                        return r.readLine() + "/" + r.readLine();
                    } finally {
                        r.close();
                    }
                case "writer":
                    BufferedWriter w = Files.newBufferedWriter(p);
                    w.write(text);
                    w.close();
                    return "ok";
                case "stream":
                    OutputStream out = Files.newOutputStream(p);
                    out.write(bytes);
                    out.close();
                    InputStream in = Files.newInputStream(p);
                    try {
                        return String.valueOf(in.read());
                    } finally {
                        in.close();
                    }
                case "createFile":
                    return relative(base, Files.createFile(p));
                case "createDirectory":
                    return relative(base, Files.createDirectory(p));
                case "createDirectories":
                    return relative(base, Files.createDirectories(p));
                case "delete":
                    Files.delete(p);
                    return "ok";
                case "deleteIfExists":
                    return String.valueOf(Files.deleteIfExists(p));
                case "copy":
                    return relative(base, Files.copy(p, q));
                case "copyReplace":
                    return relative(base, Files.copy(p, q, StandardCopyOption.REPLACE_EXISTING));
                case "copyIn":
                    return String.valueOf(Files.copy(new ByteArrayInputStream(bytes), p));
                case "copyOut":
                    ByteArrayOutputStream sink = new ByteArrayOutputStream();
                    return Files.copy(p, sink) + ":" + sink.toString("UTF-8");
                case "move":
                    return relative(base, Files.move(p, q));
                case "moveReplace":
                    return relative(base, Files.move(p, q, StandardCopyOption.REPLACE_EXISTING));
                case "mismatch":
                    return String.valueOf(Files.mismatch(p, q));
                case "same":
                    return String.valueOf(Files.isSameFile(p, q));
                case "list":
                    return sorted(Files.list(p).iterator(), base);
                case "walk":
                    return sorted(Files.walk(p).iterator(), base);
                case "walk1":
                    return sorted(Files.walk(p, 1).iterator(), base);
                case "find":
                    return sorted(Files.find(p, 9, new BiPredicate<Path, BasicFileAttributes>() {
                        @Override
                        public boolean test(Path x, BasicFileAttributes at2) {
                            return at2.isRegularFile() && x.getFileName().toString().endsWith(".txt");
                        }
                    }).iterator(), base);
                case "dir":
                    DirectoryStream<Path> ds = Files.newDirectoryStream(p);
                    try {
                        return sorted(ds.iterator(), base);
                    } finally {
                        ds.close();
                    }
                case "glob":
                    DirectoryStream<Path> gs = Files.newDirectoryStream(p, text);
                    try {
                        return sorted(gs.iterator(), base);
                    } finally {
                        gs.close();
                    }
                case "tree":
                    final List<String> log = new ArrayList<String>();
                    final String stop = text;
                    Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes at3) {
                            log.add("pre " + relative(p, d));
                            return d.getFileName().toString().equals(stop) ? FileVisitResult.SKIP_SUBTREE
                                    : FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path f, BasicFileAttributes at3) {
                            log.add("file " + relative(p, f));
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path d, IOException e) {
                            log.add("post " + relative(p, d));
                            return FileVisitResult.CONTINUE;
                        }
                    });
                    Collections.sort(log);
                    return log.toString();
                default:
                    throw new IllegalStateException(name);
            }
        }
    }

    /// The script: operation, path, second path, text.
    private static final String[][] SCRIPT = {
        {"state", "missing.txt", null, null},
        {"size", "missing.txt", null, null},
        {"read", "missing.txt", null, null},
        {"delete", "missing.txt", null, null},
        {"deleteIfExists", "missing.txt", null, null},
        {"write", "a.txt", null, "hello"},
        {"state", "a.txt", null, null},
        {"size", "a.txt", null, null},
        {"attrs", "a.txt", null, null},
        {"read", "a.txt", null, null},
        {"writeCreateNew", "a.txt", null, "again"},
        {"writeCreateNew", "fresh.txt", null, "new"},
        {"writeAppend", "a.txt", null, " world"},
        {"read", "a.txt", null, null},
        {"writeNoCreate", "a.txt", null, "HE"},
        {"read", "a.txt", null, null},
        {"writeNoCreate", "nothing.txt", null, "x"},
        {"writeTruncate", "a.txt", null, "cut"},
        {"read", "a.txt", null, null},
        {"writeTruncate", "nothing.txt", null, "x"},
        {"write", "a.txt", null, "short"},
        {"readString", "a.txt", null, null},
        {"writeString", "s.txt", null, "caf\u00e9 \u00fcber"},
        {"readString", "s.txt", null, null},
        {"readLatin", "s.txt", null, null},
        {"size", "s.txt", null, null},
        {"writeLines", "l.txt", null, "one,two,,four"},
        {"read", "l.txt", null, null},
        {"lines", "l.txt", null, null},
        {"reader", "l.txt", null, null},
        {"writeString", "crlf.txt", null, "a\r\nb\rc\n\nd"},
        {"lines", "crlf.txt", null, null},
        {"writer", "w.txt", null, "written"},
        {"read", "w.txt", null, null},
        {"stream", "st.txt", null, "A"},
        {"write", "no/such/dir/x.txt", null, "x"},
        {"createFile", "made.txt", null, null},
        {"createFile", "made.txt", null, null},
        {"createFile", "no/dir/made.txt", null, null},
        {"size", "made.txt", null, null},
        {"createDirectory", "d", null, null},
        {"createDirectory", "d", null, null},
        {"createDirectory", "x/y", null, null},
        {"createDirectories", "x/y/z", null, null},
        {"createDirectories", "x/y/z", null, null},
        {"createDirectories", "a.txt", null, null},
        {"state", "d", null, null},
        {"attrs", "d", null, null},
        {"read", "d", null, null},
        {"write", "d", null, "x"},
        {"write", "d/one.txt", null, "1"},
        {"write", "d/two.md", null, "22"},
        {"write", "d/three.txt", null, "333"},
        {"write", "x/y/z/deep.txt", null, "deep"},
        {"write", "x/top.txt", null, "top"},
        {"delete", "d", null, null},
        {"list", "d", null, null},
        {"list", "a.txt", null, null},
        {"list", "missing", null, null},
        {"dir", "d", null, null},
        {"glob", "d", null, "*.txt"},
        {"glob", "d", null, "t*"},
        {"glob", "d", null, "*.{md,txt}"},
        {"glob", "d", null, "?n?.txt"},
        {"glob", "d", null, "[a-o]*"},
        {"glob", "d", null, "[!a-o]*"},
        {"glob", "d", null, "t\\wo.md"},
        {"walk", "x", null, null},
        {"walk1", "x", null, null},
        {"walk", "a.txt", null, null},
        {"walk", "missing", null, null},
        {"find", "x", null, null},
        {"tree", "x", null, "none"},
        {"tree", "x", null, "y"},
        {"tree", "a.txt", null, "none"},
        {"copy", "a.txt", "copy.txt", null},
        {"read", "copy.txt", null, null},
        {"copy", "a.txt", "copy.txt", null},
        {"copy", "s.txt", "copy.txt", null},
        {"copyReplace", "s.txt", "copy.txt", null},
        {"read", "copy.txt", null, null},
        {"copy", "missing.txt", "copy2.txt", null},
        {"copy", "a.txt", "no/dir/copy.txt", null},
        {"copy", "d", "dcopy", null},
        {"list", "dcopy", null, null},
        {"copyReplace", "a.txt", "d", null},
        {"copy", "a.txt", "a.txt", null},
        {"copyIn", "in.txt", null, "streamed"},
        {"copyIn", "in.txt", null, "streamed"},
        {"copyOut", "in.txt", null, null},
        {"mismatch", "a.txt", "copy.txt", null},
        {"mismatch", "a.txt", "a.txt", null},
        {"mismatch", "d/one.txt", "d/three.txt", null},
        {"mismatch", "a.txt", "missing.txt", null},
        {"same", "a.txt", "a.txt", null},
        {"same", "a.txt", "x/../a.txt", null},
        {"same", "a.txt", "copy.txt", null},
        {"move", "copy.txt", "moved.txt", null},
        {"state", "copy.txt", null, null},
        {"read", "moved.txt", null, null},
        {"move", "moved.txt", "a.txt", null},
        {"moveReplace", "moved.txt", "a.txt", null},
        {"read", "a.txt", null, null},
        {"move", "a.txt", "d/in-d.txt", null},
        {"read", "d/in-d.txt", null, null},
        {"move", "missing.txt", "x.txt", null},
        {"move", "d", "renamed", null},
        {"list", "renamed", null, null},
        {"move", "renamed", "x/y/under", null},
        {"walk", "x", null, null},
        {"state", "renamed", null, null},
        {"delete", "x/y/z/deep.txt", null, null},
        {"delete", "x/y/z", null, null},
        {"deleteIfExists", "x/y/z", null, null},
        {"walk", "x", null, null},
    };

    private static String step(Ops ops, String[] s) {
        try {
            return ops.op(s[0], s[1], s[2], s[3]);
        } catch (IOException e) {
            return e.getClass().getSimpleName();
        } catch (RuntimeException e) {
            return e.getClass().getSimpleName();
        }
    }

    @Test
    public void theScriptAnswersTheSameOnBothFileSystems() throws Exception {
        Ops jdk = new Jdk(tmp.newFolder("jdk").toPath());
        new java.io.File(deviceRoot, "work").mkdirs();
        Ops shim = new Shim(Paths.get("/work"));
        for (int i = 0; i < SCRIPT.length; i++) {
            String[] s = SCRIPT[i];
            String expected = step(jdk, s);
            // The JDK reports a directory opened as a file in several ways,
            // by platform; all of them are an IOException.
            if (expected.equals("IOException") || expected.equals("FileSystemException")
                    || expected.equals("AccessDeniedException")) {
                expected = "<io>";
            }
            String actual = step(shim, s);
            if (actual.equals("IOException") || actual.equals("FileSystemException")
                    || actual.equals("AccessDeniedException")) {
                actual = "<io>";
            }
            assertEquals("step " + i + " " + Arrays.toString(s), expected, actual);
        }
        assertTrue(SCRIPT.length > 100);
    }

    @Test
    public void aRelativePathIsUnderTheApplicationHome() throws Exception {
        Path settings = Paths.get("settings", "app.properties");
        assertFalse(settings.isAbsolute());
        assertEquals("file:///home/settings/app.properties", settings.toAbsolutePath().toString());
        Files.createDirectories(settings.getParent());
        Files.writeString(settings, "theme=dark\n");
        assertTrue(new java.io.File(deviceRoot, "home/settings/app.properties").isFile());
        assertEquals("theme=dark\n", Files.readString(settings.toAbsolutePath()));
        assertEquals("theme=dark\n", Files.readString(new File("settings/app.properties").toPath()));
        assertEquals(settings.toAbsolutePath(), settings.toRealPath());
        assertTrue(Files.isSameFile(settings, Paths.get("settings/../settings/app.properties")));
    }

    @Test
    public void aTemporaryFileIsCreatedAndUnique() throws Exception {
        Path a = Files.createTempFile("pre", ".bin");
        Path b = Files.createTempFile("pre", ".bin");
        assertTrue(Files.isRegularFile(a));
        assertTrue(Files.isRegularFile(b));
        assertFalse(a.equals(b));
        assertTrue(a.getFileName().toString().startsWith("pre"));
        assertTrue(a.getFileName().toString().endsWith(".bin"));
        Path dir = Files.createTempDirectory("work");
        assertTrue(Files.isDirectory(dir));
        Path inside = Files.createTempFile(dir, null, null);
        assertEquals(dir, inside.getParent());
        assertTrue(inside.getFileName().toString().endsWith(".tmp"));
    }

    @Test
    public void aFileTimePrintsAsTheJdkPrintsIt() {
        long[] times = {0L, 1L, 999L, 1000L, 1709209805250L, 1700000000000L, -1L, -86400000L, 253402300799999L,
            951782400000L, 951868800000L, 4102444800000L, -62135596800000L, 1234567890120L, 1234567890100L};
        for (long t : times) {
            assertEquals(java.nio.file.attribute.FileTime.fromMillis(t).toString(),
                    FileTime.fromMillis(t).toString());
            assertEquals(t, FileTime.fromMillis(t).toInstant().toEpochMilli());
            assertEquals(java.nio.file.attribute.FileTime.fromMillis(t).to(java.util.concurrent.TimeUnit.SECONDS),
                    FileTime.fromMillis(t).to(TimeUnit.SECONDS));
        }
        assertEquals(0, FileTime.fromMillis(5).compareTo(FileTime.from(5, TimeUnit.MILLISECONDS)));
        assertTrue(FileTime.fromMillis(5).compareTo(FileTime.fromMillis(6)) < 0);
    }
}
