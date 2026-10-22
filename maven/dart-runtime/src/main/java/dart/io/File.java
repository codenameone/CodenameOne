/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.io;

import dart.async.Future;
import dart.convert.ByteCodec;
import dart.convert.Convert;
import dart.core.DartList;
import dart.typed_data.Uint8List;

/**
 * Dart's {@code File}. The asynchronous methods do their I/O before returning a completed
 * future, which is what the app observes once it awaits them; failures arrive through the
 * future as a {@link FileSystemException}, never thrown from the call.
 */
public class File extends FileSystemEntity {

    public File(String path) {
        super(path);
    }

    /** Dart's {@code File.fromUri}. */
    public static File fromUri(dart.core.DartUri uri) {
        return new File(uriToFilePath(uri));
    }

    public File absolute() {
        return isAbsolute() ? this : new File(absolutePath(path));
    }

    private static ByteCodec codec(Object encoding) {
        return encoding instanceof ByteCodec ? (ByteCodec) encoding : Convert.utf8;
    }

    // ---------------------------------------------------------------- read

    public Uint8List readAsBytesSync() {
        return Uint8List.fromBytes(Fs.get().read(path));
    }

    public Future<Uint8List> readAsBytes() {
        try {
            return Future.value(readAsBytesSync());
        } catch (RuntimeException e) {
            return Future.error(e);
        }
    }

    public String readAsStringSync() {
        return readAsStringSync(null);
    }

    public String readAsStringSync(Object encoding) {
        return (String) codec(encoding).decode(readAsBytesSync());
    }

    public Future<String> readAsString() {
        return readAsString(null);
    }

    public Future<String> readAsString(Object encoding) {
        try {
            return Future.value(readAsStringSync(encoding));
        } catch (RuntimeException e) {
            return Future.error(e);
        }
    }

    public DartList<String> readAsLinesSync() {
        return readAsLinesSync(null);
    }

    public DartList<String> readAsLinesSync(Object encoding) {
        return dart.convert.LineSplitter.split(readAsStringSync(encoding));
    }

    public Future<DartList<String>> readAsLines() {
        return readAsLines(null);
    }

    public Future<DartList<String>> readAsLines(Object encoding) {
        try {
            return Future.value(readAsLinesSync(encoding));
        } catch (RuntimeException e) {
            return Future.error(e);
        }
    }

    /** Dart's {@code openRead()}: the content as one chunk. */
    public dart.async.Stream<Object> openRead() {
        return openRead(null, null);
    }

    public dart.async.Stream<Object> openRead(Long start, Long end) {
        try {
            // Dart rejects a negative bound or an end before the start; clamping them
            // made a bad range look like an empty file.
            if (start != null && start.longValue() < 0) {
                throw new dart.core.RangeError("Invalid value: start must be non-negative: " + start);
            }
            if (end != null && (end.longValue() < 0 || end.longValue() < (start == null ? 0 : start.longValue()))) {
                throw new dart.core.RangeError("Invalid value: end must not be before start: " + end);
            }
            byte[] all = Fs.get().read(path);
            int s = start == null ? 0 : (int) Math.min(all.length, start.longValue());
            int e = end == null ? all.length : (int) Math.min(all.length, end.longValue());
            byte[] part = new byte[Math.max(0, e - s)];
            System.arraycopy(all, s, part, 0, part.length);
            return dart.async.Stream.<Object>value(Uint8List.fromBytes(part));
        } catch (RuntimeException ex) {
            return dart.async.Stream.<Object>error(ex);
        }
    }

    // ---------------------------------------------------------------- write

    private static boolean appends(Object mode) {
        return mode instanceof FileMode && ((FileMode) mode).append_;
    }

    public void writeAsBytesSync(Object bytes) {
        writeAsBytesSync(bytes, null, null);
    }

    public void writeAsBytesSync(Object bytes, Object mode, Boolean flush) {
        Fs.get().write(path, ByteCodec.bytesOf(bytes), appends(mode));
    }

    public Future<File> writeAsBytes(Object bytes) {
        return writeAsBytes(bytes, null, null);
    }

    public Future<File> writeAsBytes(Object bytes, Object mode, Boolean flush) {
        try {
            writeAsBytesSync(bytes, mode, flush);
            return Future.value(this);
        } catch (RuntimeException e) {
            return Future.error(e);
        }
    }

    public void writeAsStringSync(String contents) {
        writeAsStringSync(contents, null, null, null);
    }

    public void writeAsStringSync(String contents, Object mode, Object encoding, Boolean flush) {
        Fs.get().write(path, ByteCodec.bytesOf(codec(encoding).encode(contents)), appends(mode));
    }

    public Future<File> writeAsString(String contents) {
        return writeAsString(contents, null, null, null);
    }

    public Future<File> writeAsString(String contents, Object mode, Object encoding, Boolean flush) {
        try {
            writeAsStringSync(contents, mode, encoding, flush);
            return Future.value(this);
        } catch (RuntimeException e) {
            return Future.error(e);
        }
    }

    // ---------------------------------------------------------------- other

    public void createSync() {
        createSync(null, null);
    }

    public void createSync(Boolean recursive, Boolean exclusive) {
        Fs fs = Fs.get();
        if (fs.exists(path)) {
            if (fs.isDirectory(path)) {
                // a directory is not a file that already exists: Dart fails here
                throw new FileSystemException("Cannot create file, path is a directory", path);
            }
            if (exclusive != null && exclusive.booleanValue()) {
                throw new FileSystemException("Cannot create file, it already exists", path);
            }
            return;
        }
        if (recursive != null && recursive.booleanValue()) {
            fs.mkdirs(Fs.parentOf(path));
        }
        fs.write(path, new byte[0], false);
    }

    public Future<File> create() {
        return create(null, null);
    }

    public Future<File> create(Boolean recursive, Boolean exclusive) {
        try {
            createSync(recursive, exclusive);
            return Future.value(this);
        } catch (RuntimeException e) {
            return Future.error(e);
        }
    }

    public long lengthSync() {
        return Fs.get().length(path);
    }

    public Future<Long> length() {
        try {
            return Future.value(Long.valueOf(lengthSync()));
        } catch (RuntimeException e) {
            return Future.error(e);
        }
    }

    public dart.core.DateTime lastModifiedSync() {
        return dart.core.DateTime.fromMillisecondsSinceEpoch(Fs.get().lastModified(path), false);
    }

    public Future<dart.core.DateTime> lastModified() {
        try {
            return Future.value(lastModifiedSync());
        } catch (RuntimeException e) {
            return Future.error(e);
        }
    }

    public File copySync(String newPath) {
        Fs.get().write(newPath, Fs.get().read(path), false);
        return new File(newPath);
    }

    public Future<File> copy(String newPath) {
        try {
            return Future.value(copySync(newPath));
        } catch (RuntimeException e) {
            return Future.error(e);
        }
    }

    public File renameSync(String newPath) {
        Fs fs = Fs.get();
        if (absolutePath(newPath).equals(absolutePath(path))) {
            // Renaming onto itself is a no-op; the copy-and-delete below would delete it.
            return new File(newPath);
        }
        fs.write(newPath, fs.read(path), false);
        fs.delete(path);
        return new File(newPath);
    }

    public Future<File> rename(String newPath) {
        try {
            return Future.value(renameSync(newPath));
        } catch (RuntimeException e) {
            return Future.error(e);
        }
    }

    @Override
    public String toString() {
        return "File: '" + path + "'";
    }
}
