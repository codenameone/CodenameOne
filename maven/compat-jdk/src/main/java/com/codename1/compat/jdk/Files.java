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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/// `java.nio.file.Files` for the Codename One runtime, over
/// `com.codename1.io.FileSystemStorage`.
///
/// #### What differs from a desktop file system
///
/// - A directory's entries come back sorted by name. The JDK promises no
///   order, and a device's file systems disagree with each other.
/// - `walk`, `list`, `find` and `newDirectoryStream` read the entries when
///   they are called, not as the result is consumed.
/// - Nothing is a symbolic link, and a `LinkOption` changes nothing.
/// - The device keeps one time per file. `getLastModifiedTime` answers it,
///   and so do the creation and access times of [BasicFileAttributes].
/// - A file is executable when it is a directory, which is all "executable"
///   can mean where no program can be started.
/// - A temporary file goes to the caches directory where the device has
///   one, and to the application's home otherwise. Nothing deletes it.
/// - Text is UTF-8 unless a `Charset` says otherwise; undecodable bytes are
///   replaced rather than reported.
///
/// #### What is not provided
///
/// Channels (`newByteChannel`), POSIX permissions, owners, file stores, hard
/// and symbolic links, `probeContentType`, `setLastModifiedTime` and the
/// attribute views by name. A device has no equivalent, and a call to any of
/// them is a build error.
public final class Files {

    private static final String LINE = "\n";

    private Files() {
    }

    private static File file(Path path) {
        if (path == null) {
            throw new NullPointerException();
        }
        return path.toFile();
    }

    private static boolean has(Object[] options, Object option) {
        if (options != null) {
            for (Object o : options) {
                if (o == null) {
                    throw new NullPointerException();
                }
                if (o == option) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---- what is there ----

    public static boolean exists(Path path, LinkOption... options) {
        return file(path).exists();
    }

    public static boolean notExists(Path path, LinkOption... options) {
        return !file(path).exists();
    }

    public static boolean isDirectory(Path path, LinkOption... options) {
        return file(path).isDirectory();
    }

    public static boolean isRegularFile(Path path, LinkOption... options) {
        return file(path).isFile();
    }

    public static boolean isSymbolicLink(Path path) {
        file(path);
        return false;
    }

    public static boolean isReadable(Path path) {
        return file(path).exists();
    }

    public static boolean isWritable(Path path) {
        return file(path).exists();
    }

    public static boolean isExecutable(Path path) {
        return file(path).isDirectory();
    }

    public static boolean isHidden(Path path) throws IOException {
        return file(path).isHidden();
    }

    public static boolean isSameFile(Path path, Path path2) throws IOException {
        if (path.equals(path2)) {
            return true;
        }
        return path.toRealPath().equals(path2.toRealPath());
    }

    private static File existing(Path path) throws IOException {
        File f = file(path);
        if (!f.exists()) {
            throw new NoSuchFileException(path.toString());
        }
        return f;
    }

    public static long size(Path path) throws IOException {
        return existing(path).length();
    }

    public static FileTime getLastModifiedTime(Path path, LinkOption... options) throws IOException {
        return FileTime.fromMillis(existing(path).lastModified());
    }

    /// The attributes of `path`. `type` has to be `BasicFileAttributes.class`,
    /// the one set a device has.
    @SuppressWarnings("unchecked")
    public static <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
            throws IOException {
        if (type != BasicFileAttributes.class) {
            throw new UnsupportedOperationException("Only BasicFileAttributes can be read on a device");
        }
        return (A) attributes(existing(path));
    }

    private static BasicFileAttributes attributes(File f) {
        final boolean directory = f.isDirectory();
        final long size = f.length();
        final FileTime time = FileTime.fromMillis(f.lastModified());
        return new BasicFileAttributes() {
            @Override
            public FileTime lastModifiedTime() {
                return time;
            }

            @Override
            public FileTime lastAccessTime() {
                return time;
            }

            @Override
            public FileTime creationTime() {
                return time;
            }

            @Override
            public boolean isRegularFile() {
                return !directory;
            }

            @Override
            public boolean isDirectory() {
                return directory;
            }

            @Override
            public boolean isSymbolicLink() {
                return false;
            }

            @Override
            public boolean isOther() {
                return false;
            }

            @Override
            public long size() {
                return size;
            }

            @Override
            public Object fileKey() {
                return null;
            }
        };
    }

    // ---- streams ----

    public static InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        if (has(options, StandardOpenOption.WRITE) || has(options, StandardOpenOption.APPEND)) {
            throw new UnsupportedOperationException("An input stream cannot be opened for writing");
        }
        File f = existing(path);
        if (f.isDirectory()) {
            throw new IOException(path + ": Is a directory");
        }
        return new FileInputStream(f);
    }

    public static OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
        if (has(options, StandardOpenOption.READ)) {
            throw new IllegalArgumentException("READ not allowed");
        }
        boolean none = options == null || options.length == 0;
        boolean append = has(options, StandardOpenOption.APPEND);
        if (append && has(options, StandardOpenOption.TRUNCATE_EXISTING)) {
            throw new IllegalArgumentException("APPEND + TRUNCATE_EXISTING not allowed");
        }
        File f = file(path);
        boolean exists = f.exists();
        if (has(options, StandardOpenOption.CREATE_NEW)) {
            if (exists) {
                throw new FileAlreadyExistsException(path.toString());
            }
        } else if (!exists && !none && !has(options, StandardOpenOption.CREATE)) {
            throw new NoSuchFileException(path.toString());
        }
        if (f.isDirectory()) {
            throw new FileSystemException(path.toString(), null, "Is a directory");
        }
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            throw new NoSuchFileException(path.toString());
        }
        // Without APPEND or TRUNCATE_EXISTING the JDK overwrites from the
        // start and leaves a longer file's tail in place. A device stream
        // replaces the file, so the tail is put back when there is one.
        byte[] tail = null;
        if (exists && !none && !append && !has(options, StandardOpenOption.TRUNCATE_EXISTING)) {
            tail = readAllBytes(path);
        }
        OutputStream out = new FileOutputStream(f, append);
        return tail == null || tail.length == 0 ? out : new Overwrite(out, tail);
    }

    /// A stream that writes over the start of what a file held and keeps the
    /// rest: what opening a file with `WRITE` alone means.
    private static final class Overwrite extends OutputStream {
        private final OutputStream out;
        private final byte[] old;
        private long written;
        private boolean closed;

        Overwrite(OutputStream out, byte[] old) {
            this.out = out;
            this.old = old;
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            written++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            out.write(b, off, len);
            written += len;
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                if (written < old.length) {
                    out.write(old, (int) written, old.length - (int) written);
                }
                out.close();
            }
        }
    }

    public static BufferedReader newBufferedReader(Path path, Charset cs) throws IOException {
        return new BufferedReader(JdkCharsets.reader(newInputStream(path), cs));
    }

    public static BufferedReader newBufferedReader(Path path) throws IOException {
        return newBufferedReader(path, StandardCharsets.UTF_8);
    }

    public static BufferedWriter newBufferedWriter(Path path, Charset cs, OpenOption... options) throws IOException {
        return new BufferedWriter(JdkCharsets.writer(newOutputStream(path, options), cs));
    }

    public static BufferedWriter newBufferedWriter(Path path, OpenOption... options) throws IOException {
        return newBufferedWriter(path, StandardCharsets.UTF_8, options);
    }

    // ---- reading ----

    public static byte[] readAllBytes(Path path) throws IOException {
        InputStream in = newInputStream(path);
        try {
            return JdkCharsets.drain(in);
        } finally {
            in.close();
        }
    }

    public static String readString(Path path) throws IOException {
        return readString(path, StandardCharsets.UTF_8);
    }

    public static String readString(Path path, Charset cs) throws IOException {
        return JdkCharsets.decode(readAllBytes(path), cs);
    }

    public static List<String> readAllLines(Path path) throws IOException {
        return readAllLines(path, StandardCharsets.UTF_8);
    }

    /// The lines of the file. A line ends at `\n`, `\r` or `\r\n`, and a
    /// file that ends with a terminator has no empty last line.
    public static List<String> readAllLines(Path path, Charset cs) throws IOException {
        String text = readString(path, cs);
        List<String> lines = new ArrayList<String>();
        int n = text.length();
        int start = 0;
        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                lines.add(text.substring(start, i));
                if (c == '\r' && i + 1 < n && text.charAt(i + 1) == '\n') {
                    i++;
                }
                start = i + 1;
            }
        }
        if (start < n) {
            lines.add(text.substring(start));
        }
        return lines;
    }

    public static Stream<String> lines(Path path) throws IOException {
        return lines(path, StandardCharsets.UTF_8);
    }

    public static Stream<String> lines(Path path, Charset cs) throws IOException {
        return JdkCollections.defaultStream(readAllLines(path, cs));
    }

    // ---- writing ----

    public static Path write(Path path, byte[] bytes, OpenOption... options) throws IOException {
        if (bytes == null) {
            throw new NullPointerException();
        }
        OutputStream out = newOutputStream(path, options);
        try {
            out.write(bytes, 0, bytes.length);
        } finally {
            out.close();
        }
        return path;
    }

    public static Path write(Path path, Iterable<? extends CharSequence> lines, Charset cs, OpenOption... options)
            throws IOException {
        if (lines == null) {
            throw new NullPointerException();
        }
        StringBuilder sb = new StringBuilder();
        for (CharSequence line : lines) {
            sb.append(line.toString()).append(LINE);
        }
        return write(path, JdkCharsets.getBytes(sb.toString(), cs), options);
    }

    public static Path write(Path path, Iterable<? extends CharSequence> lines, OpenOption... options)
            throws IOException {
        return write(path, lines, StandardCharsets.UTF_8, options);
    }

    public static Path writeString(Path path, CharSequence csq, OpenOption... options) throws IOException {
        return writeString(path, csq, StandardCharsets.UTF_8, options);
    }

    public static Path writeString(Path path, CharSequence csq, Charset cs, OpenOption... options)
            throws IOException {
        if (csq == null) {
            throw new NullPointerException();
        }
        return write(path, JdkCharsets.getBytes(csq.toString(), cs), options);
    }

    // ---- creating and deleting ----

    public static Path createFile(Path path, FileAttribute<?>... attrs) throws IOException {
        File f = file(path);
        if (f.exists()) {
            throw new FileAlreadyExistsException(path.toString());
        }
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            throw new NoSuchFileException(path.toString());
        }
        new FileOutputStream(f, false).close();
        return path;
    }

    public static Path createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        File f = file(dir);
        if (f.exists()) {
            throw new FileAlreadyExistsException(dir.toString());
        }
        File parent = f.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            throw new NoSuchFileException(dir.toString());
        }
        if (!f.mkdir()) {
            throw new IOException("Could not create the directory " + dir);
        }
        return dir;
    }

    public static Path createDirectories(Path dir, FileAttribute<?>... attrs) throws IOException {
        File f = file(dir);
        if (f.isDirectory()) {
            return dir;
        }
        if (f.exists()) {
            throw new FileAlreadyExistsException(dir.toString());
        }
        f.mkdirs();
        if (!f.isDirectory()) {
            throw new IOException("Could not create the directory " + dir);
        }
        return dir;
    }

    private static Path temporaryDirectory() {
        return PathImpl.of(JdkSystem.getProperty("java.io.tmpdir"));
    }

    private static Path temporary(Path dir, String prefix, String suffix, boolean directory) throws IOException {
        Path parent = dir == null ? temporaryDirectory() : dir;
        String p = prefix == null ? "" : prefix;
        long n = System.currentTimeMillis();
        while (true) {
            Path candidate = parent.resolve(p + n + suffix);
            if (!file(candidate).exists()) {
                return directory ? createDirectory(candidate) : createFile(candidate);
            }
            n++;
        }
    }

    public static Path createTempFile(Path dir, String prefix, String suffix, FileAttribute<?>... attrs)
            throws IOException {
        if (dir == null) {
            throw new NullPointerException();
        }
        return temporary(dir, prefix, suffix == null ? ".tmp" : suffix, false);
    }

    public static Path createTempFile(String prefix, String suffix, FileAttribute<?>... attrs) throws IOException {
        return temporary(null, prefix, suffix == null ? ".tmp" : suffix, false);
    }

    public static Path createTempDirectory(Path dir, String prefix, FileAttribute<?>... attrs) throws IOException {
        if (dir == null) {
            throw new NullPointerException();
        }
        return temporary(dir, prefix, "", true);
    }

    public static Path createTempDirectory(String prefix, FileAttribute<?>... attrs) throws IOException {
        return temporary(null, prefix, "", true);
    }

    public static void delete(Path path) throws IOException {
        File f = existing(path);
        if (f.isDirectory()) {
            String[] entries = f.list();
            if (entries != null && entries.length > 0) {
                throw new DirectoryNotEmptyException(path.toString());
            }
        }
        if (!f.delete()) {
            throw new IOException("Could not delete " + path);
        }
    }

    public static boolean deleteIfExists(Path path) throws IOException {
        if (!file(path).exists()) {
            return false;
        }
        delete(path);
        return true;
    }

    // ---- copying and moving ----

    /// Clears the way for a copy or a move to `target`, or says why not.
    private static void replace(Path target, CopyOption[] options) throws IOException {
        if (file(target).exists()) {
            if (!has(options, StandardCopyOption.REPLACE_EXISTING)) {
                throw new FileAlreadyExistsException(target.toString());
            }
            delete(target);
        }
    }

    private static void transfer(File from, File to) throws IOException {
        InputStream in = new FileInputStream(from);
        try {
            OutputStream out = new FileOutputStream(to, false);
            try {
                pump(in, out);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private static long pump(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        long total = 0;
        int n;
        while ((n = in.read(buffer, 0, buffer.length)) >= 0) {
            out.write(buffer, 0, n);
            total += n;
        }
        return total;
    }

    /// Copies a file, or creates an empty directory where `source` is one:
    /// a directory's entries are not copied, as in the JDK.
    public static Path copy(Path source, Path target, CopyOption... options) throws IOException {
        File from = existing(source);
        if (file(target).exists() && isSameFile(source, target)) {
            return target;
        }
        replace(target, options);
        if (from.isDirectory()) {
            createDirectory(target);
        } else {
            File to = file(target);
            File parent = to.getParentFile();
            if (parent != null && !parent.isDirectory()) {
                throw new NoSuchFileException(target.toString());
            }
            transfer(from, to);
        }
        return target;
    }

    public static long copy(InputStream in, Path target, CopyOption... options) throws IOException {
        if (in == null) {
            throw new NullPointerException();
        }
        replace(target, options);
        OutputStream out = newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        try {
            return pump(in, out);
        } finally {
            out.close();
        }
    }

    public static long copy(Path source, OutputStream out) throws IOException {
        if (out == null) {
            throw new NullPointerException();
        }
        InputStream in = newInputStream(source);
        try {
            return pump(in, out);
        } finally {
            in.close();
        }
    }

    private static void copyTree(File from, File to) throws IOException {
        if (from.isDirectory()) {
            if (!to.mkdir()) {
                throw new IOException("Could not create the directory " + to.getPath());
            }
            String[] names = from.list();
            if (names != null) {
                for (String name : names) {
                    copyTree(new File(from, name), new File(to, name));
                }
            }
        } else {
            transfer(from, to);
        }
    }

    private static void deleteTree(File f) {
        if (f.isDirectory()) {
            String[] names = f.list();
            if (names != null) {
                for (String name : names) {
                    deleteTree(new File(f, name));
                }
            }
        }
        f.delete();
    }

    /// Moves a file or a directory with everything in it. Where the device
    /// cannot rename in place the tree is copied and the original deleted.
    public static Path move(Path source, Path target, CopyOption... options) throws IOException {
        File from = existing(source);
        if (file(target).exists() && isSameFile(source, target)) {
            return target;
        }
        replace(target, options);
        File to = file(target);
        File parent = to.getParentFile();
        if (parent != null && !parent.isDirectory()) {
            throw new NoSuchFileException(target.toString());
        }
        if (!from.renameTo(to)) {
            if (!from.isDirectory()) {
                throw new IOException("Could not move " + source + " to " + target);
            }
            copyTree(from, to);
            deleteTree(from);
        }
        return target;
    }

    /// The position of the first byte two files differ in, or -1 when they
    /// hold the same bytes.
    public static long mismatch(Path path, Path path2) throws IOException {
        byte[] a = readAllBytes(path);
        byte[] b = readAllBytes(path2);
        int n = Math.min(a.length, b.length);
        for (int i = 0; i < n; i++) {
            if (a[i] != b[i]) {
                return i;
            }
        }
        return a.length == b.length ? -1L : n;
    }

    // ---- directories ----

    private static List<Path> entries(Path dir) throws IOException {
        File f = existing(dir);
        if (!f.isDirectory()) {
            throw new NotDirectoryException(dir.toString());
        }
        String[] names = f.list();
        List<Path> out = new ArrayList<Path>();
        if (names != null) {
            Arrays.sort(names);
            for (String name : names) {
                out.add(dir.resolve(name));
            }
        }
        return out;
    }

    public static Stream<Path> list(Path dir) throws IOException {
        return JdkCollections.defaultStream(entries(dir));
    }

    private static void walk(Path path, int depth, int maxDepth, List<Path> out) throws IOException {
        out.add(path);
        if (depth < maxDepth && file(path).isDirectory()) {
            for (Path entry : entries(path)) {
                walk(entry, depth + 1, maxDepth, out);
            }
        }
    }

    public static Stream<Path> walk(Path start, int maxDepth, FileVisitOption... options) throws IOException {
        if (maxDepth < 0) {
            throw new IllegalArgumentException("'maxDepth' is negative");
        }
        existing(start);
        List<Path> out = new ArrayList<Path>();
        walk(start, 0, maxDepth, out);
        return JdkCollections.defaultStream(out);
    }

    public static Stream<Path> walk(Path start, FileVisitOption... options) throws IOException {
        return walk(start, Integer.MAX_VALUE, options);
    }

    public static Stream<Path> find(Path start, int maxDepth, BiPredicate<Path, BasicFileAttributes> matcher,
                                    FileVisitOption... options) throws IOException {
        if (maxDepth < 0) {
            throw new IllegalArgumentException("'maxDepth' is negative");
        }
        if (matcher == null) {
            throw new NullPointerException();
        }
        existing(start);
        List<Path> all = new ArrayList<Path>();
        walk(start, 0, maxDepth, all);
        List<Path> out = new ArrayList<Path>();
        for (Path p : all) {
            if (matcher.test(p, attributes(file(p)))) {
                out.add(p);
            }
        }
        return JdkCollections.defaultStream(out);
    }

    public static Path walkFileTree(Path start, FileVisitor<? super Path> visitor) throws IOException {
        return walkFileTree(start, null, Integer.MAX_VALUE, visitor);
    }

    public static Path walkFileTree(Path start, Set<FileVisitOption> options, int maxDepth,
                                    FileVisitor<? super Path> visitor) throws IOException {
        if (maxDepth < 0) {
            throw new IllegalArgumentException("'maxDepth' is negative");
        }
        if (visitor == null) {
            throw new NullPointerException();
        }
        visit(start, 0, maxDepth, visitor);
        return start;
    }

    private static FileVisitResult checked(FileVisitResult result) {
        if (result == null) {
            throw new NullPointerException();
        }
        return result;
    }

    /// Visits `path` and what is under it. Answers `TERMINATE` to stop the
    /// walk, `SKIP_SIBLINGS` to stop the directory it is an entry of, and
    /// `CONTINUE` otherwise.
    private static FileVisitResult visit(Path path, int depth, int maxDepth, FileVisitor<? super Path> visitor)
            throws IOException {
        File f = file(path);
        if (!f.exists()) {
            return checked(visitor.visitFileFailed(path, new NoSuchFileException(path.toString())));
        }
        BasicFileAttributes attrs = attributes(f);
        if (!attrs.isDirectory() || depth >= maxDepth) {
            return checked(visitor.visitFile(path, attrs));
        }
        FileVisitResult pre = checked(visitor.preVisitDirectory(path, attrs));
        if (pre == FileVisitResult.SKIP_SUBTREE) {
            return FileVisitResult.CONTINUE;
        }
        if (pre != FileVisitResult.CONTINUE) {
            return pre;
        }
        for (Path entry : entries(path)) {
            FileVisitResult result = visit(entry, depth + 1, maxDepth, visitor);
            if (result == FileVisitResult.TERMINATE) {
                return result;
            }
            if (result == FileVisitResult.SKIP_SIBLINGS) {
                break;
            }
        }
        FileVisitResult post = checked(visitor.postVisitDirectory(path, null));
        return post == FileVisitResult.SKIP_SUBTREE ? FileVisitResult.CONTINUE : post;
    }

    public static DirectoryStream<Path> newDirectoryStream(Path dir) throws IOException {
        return new Listing(entries(dir));
    }

    public static DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter)
            throws IOException {
        if (filter == null) {
            throw new NullPointerException();
        }
        List<Path> out = new ArrayList<Path>();
        for (Path entry : entries(dir)) {
            if (filter.accept(entry)) {
                out.add(entry);
            }
        }
        return new Listing(out);
    }

    /// The entries of `dir` whose names match `glob`: `*` and `?` for any
    /// characters and any one, `[a-c]` and `[!a-c]` for a set of them,
    /// `{one,two}` for alternatives, and a backslash to take the next
    /// character literally.
    public static DirectoryStream<Path> newDirectoryStream(Path dir, String glob) throws IOException {
        if (glob == null) {
            throw new NullPointerException();
        }
        List<Path> out = new ArrayList<Path>();
        for (Path entry : entries(dir)) {
            String name = entry.getFileName().toString();
            if (Glob.matches(glob, 0, glob.length(), name, 0)) {
                out.add(entry);
            }
        }
        return new Listing(out);
    }

    private static final class Listing implements DirectoryStream<Path> {
        private final List<Path> entries;
        private boolean iterated;
        private boolean closed;

        Listing(List<Path> entries) {
            this.entries = entries;
        }

        @Override
        public Iterator<Path> iterator() {
            if (closed) {
                throw new IllegalStateException("Directory stream is closed");
            }
            if (iterated) {
                throw new IllegalStateException("Iterator already obtained");
            }
            iterated = true;
            final Iterator<Path> it = entries.iterator();
            return new Iterator<Path>() {
                @Override
                public boolean hasNext() {
                    return !closed && it.hasNext();
                }

                @Override
                public Path next() {
                    if (closed) {
                        throw new NoSuchElementException();
                    }
                    return it.next();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
