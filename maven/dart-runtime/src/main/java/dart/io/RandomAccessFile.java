/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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

import java.util.List;

import dart.async.Future;
import dart.convert.ByteCodec;
import dart.typed_data.Uint8List;

/**
 * Dart's {@code RandomAccessFile}, what {@link File#open} answers.
 *
 * <p>Codename One's file system reads and writes whole streams, not positions, so the
 * file is held in memory while it is open: {@code open} reads it, every read and write
 * works on that copy, and {@code flush} and {@code close} write it back. When everything
 * changed since the last write-back lies past the end of what is stored -- the way an
 * append-only log such as Hive's box file grows -- only the new tail is appended;
 * otherwise the file is rewritten. Memory therefore grows with the file, which suits the
 * small key-value stores that use this, not a multi-megabyte archive.</p>
 *
 * <p>The asynchronous methods do their work before returning a completed future, like
 * {@link File}'s. The file is not shared with another process, so {@code lock} and
 * {@code unlock} have nothing to exclude and succeed.</p>
 */
public class RandomAccessFile {
    private final String path;
    private final FileMode mode;
    private byte[] data;
    private int length;
    private long position;
    /** Bytes stored on disk; everything at or past {@link #dirtyFrom} differs from them. */
    private int stored;
    private int dirtyFrom = Integer.MAX_VALUE;
    private boolean closed;

    RandomAccessFile(String path, FileMode mode) {
        this.path = path;
        this.mode = mode == null ? FileMode.read : mode;
        Fs fs = Fs.get();
        boolean exists = fs.exists(path) && !fs.isDirectory(path);
        if (!exists && !this.mode.writable) {
            throw new FileSystemException("Cannot open file, path = '" + path + "' (No such file or directory)",
                    path);
        }
        if (fs.isDirectory(path)) {
            throw new FileSystemException("Cannot open file, path = '" + path + "' (Is a directory)", path);
        }
        if (!exists || this.mode.truncates) {
            fs.write(path, new byte[0], false);
            data = new byte[0];
        } else {
            data = fs.read(path);
        }
        length = data.length;
        stored = length;
        position = this.mode.append_ ? length : 0;
    }

    /** The file's path, as {@code File(path).open()} was given it. */
    public String path() {
        return path;
    }

    // ------------------------------------------------------------- position

    public long positionSync() {
        checkOpen();
        return position;
    }

    public Future<Long> position() {
        return attempt(new Op<Long>() {
            @Override
            public Long run() {
                return Long.valueOf(positionSync());
            }
        });
    }

    public void setPositionSync(long p) {
        checkOpen();
        if (p < 0) {
            throw new FileSystemException("Cannot set position in file, path = '" + path + "' (Invalid argument)",
                    path);
        }
        position = p;
    }

    public Future<RandomAccessFile> setPosition(final long p) {
        return attempt(new Op<RandomAccessFile>() {
            @Override
            public RandomAccessFile run() {
                setPositionSync(p);
                return RandomAccessFile.this;
            }
        });
    }

    public long lengthSync() {
        checkOpen();
        return length;
    }

    public Future<Long> length() {
        return attempt(new Op<Long>() {
            @Override
            public Long run() {
                return Long.valueOf(lengthSync());
            }
        });
    }

    // ----------------------------------------------------------------- read

    /** The next byte, or -1 at the end of the file. */
    public long readByteSync() {
        checkReadable();
        if (position >= length) {
            return -1;
        }
        return data[(int) position++] & 0xff;
    }

    public Future<Long> readByte() {
        return attempt(new Op<Long>() {
            @Override
            public Long run() {
                return Long.valueOf(readByteSync());
            }
        });
    }

    /** Up to {@code count} bytes from the position; fewer at the end of the file. */
    public Uint8List readSync(long count) {
        checkReadable();
        if (count < 0) {
            throw new dart.core.ArgumentError("count must not be negative: " + count);
        }
        int from = (int) Math.min(position, length);
        int n = (int) Math.min(count, length - from);
        byte[] out = new byte[n];
        System.arraycopy(data, from, out, 0, n);
        position = from + n;
        return Uint8List.fromBytes(out);
    }

    public Future<Uint8List> read(final long count) {
        return attempt(new Op<Uint8List>() {
            @Override
            public Uint8List run() {
                return readSync(count);
            }
        });
    }

    /**
     * Reads into {@code buffer[start, end)} and answers how many bytes it read, fewer at
     * the end of the file.
     */
    public long readIntoSync(Object buffer, long start, Long end) {
        checkReadable();
        int size = sizeOf(buffer);
        int to = end == null ? size : (int) end.longValue();
        checkRange(start, to, size);
        int from = (int) Math.min(position, length);
        int n = (int) Math.min(to - start, length - from);
        for (int i = 0; i < n; i++) {
            store(buffer, (int) start + i, data[from + i] & 0xff);
        }
        position = from + n;
        return n;
    }

    public Future<Long> readInto(final Object buffer, final long start, final Long end) {
        return attempt(new Op<Long>() {
            @Override
            public Long run() {
                return Long.valueOf(readIntoSync(buffer, start, end));
            }
        });
    }

    // ---------------------------------------------------------------- write

    public RandomAccessFile writeByteSync(long value) {
        put(new byte[] {(byte) value}, 0, 1);
        return this;
    }

    public Future<RandomAccessFile> writeByte(final long value) {
        return attempt(new Op<RandomAccessFile>() {
            @Override
            public RandomAccessFile run() {
                return writeByteSync(value);
            }
        });
    }

    /** Writes {@code buffer[start, end)} at the position, growing the file as needed. */
    public RandomAccessFile writeFromSync(Object buffer, long start, Long end) {
        byte[] bytes = ByteCodec.bytesOf(buffer);
        int to = end == null ? bytes.length : (int) end.longValue();
        checkRange(start, to, bytes.length);
        put(bytes, (int) start, to - (int) start);
        return this;
    }

    public Future<RandomAccessFile> writeFrom(final Object buffer, final long start, final Long end) {
        return attempt(new Op<RandomAccessFile>() {
            @Override
            public RandomAccessFile run() {
                return writeFromSync(buffer, start, end);
            }
        });
    }

    public RandomAccessFile writeStringSync(String string, Object encoding) {
        ByteCodec codec = encoding instanceof ByteCodec ? (ByteCodec) encoding : dart.convert.Convert.utf8;
        return writeFromSync(ByteCodec.bytesOf(codec.encode(string)), 0, null);
    }

    public Future<RandomAccessFile> writeString(final String string, final Object encoding) {
        return attempt(new Op<RandomAccessFile>() {
            @Override
            public RandomAccessFile run() {
                return writeStringSync(string, encoding);
            }
        });
    }

    /** Cuts the file to {@code newLength} bytes, or pads it with zeros up to it. */
    public RandomAccessFile truncateSync(long newLength) {
        checkWritable();
        if (newLength < 0) {
            throw new FileSystemException("Cannot truncate file, path = '" + path + "' (Invalid argument)", path);
        }
        int n = (int) newLength;
        ensure(n);
        if (n > length) {
            java.util.Arrays.fill(data, length, n, (byte) 0);
        }
        length = n;
        // a shorter file is never an append; a longer one is, from the old end
        dirtyFrom = Math.min(dirtyFrom, n < stored ? 0 : stored);
        return this;
    }

    public Future<RandomAccessFile> truncate(final long newLength) {
        return attempt(new Op<RandomAccessFile>() {
            @Override
            public RandomAccessFile run() {
                return truncateSync(newLength);
            }
        });
    }

    // -------------------------------------------------------------- persist

    /** Writes what changed back to the file system. */
    public RandomAccessFile flushSync() {
        checkOpen();
        if (dirtyFrom == Integer.MAX_VALUE) {
            return this;
        }
        byte[] copy;
        if (dirtyFrom >= stored) {
            copy = new byte[length - stored];
            System.arraycopy(data, stored, copy, 0, copy.length);
            Fs.get().write(path, copy, true);
        } else {
            copy = new byte[length];
            System.arraycopy(data, 0, copy, 0, length);
            Fs.get().write(path, copy, false);
        }
        stored = length;
        dirtyFrom = Integer.MAX_VALUE;
        return this;
    }

    public Future<RandomAccessFile> flush() {
        return attempt(new Op<RandomAccessFile>() {
            @Override
            public RandomAccessFile run() {
                return flushSync();
            }
        });
    }

    public void closeSync() {
        if (closed) {
            return;
        }
        flushSync();
        closed = true;
        data = null;
    }

    public Future<Object> close() {
        return attempt(new Op<Object>() {
            @Override
            public Object run() {
                closeSync();
                return null;
            }
        });
    }

    /** {@code lockSync([mode, start, end])}; see the class comment. */
    public RandomAccessFile lockSync(Object mode, long start, long end) {
        checkOpen();
        return this;
    }

    public Future<RandomAccessFile> lock(final Object mode, final long start, final long end) {
        return attempt(new Op<RandomAccessFile>() {
            @Override
            public RandomAccessFile run() {
                return lockSync(mode, start, end);
            }
        });
    }

    public RandomAccessFile unlockSync(long start, long end) {
        checkOpen();
        return this;
    }

    public Future<RandomAccessFile> unlock(final long start, final long end) {
        return attempt(new Op<RandomAccessFile>() {
            @Override
            public RandomAccessFile run() {
                return unlockSync(start, end);
            }
        });
    }

    @Override
    public String toString() {
        return "RandomAccessFile: '" + path + "'";
    }

    // -------------------------------------------------------------- helpers

    private void put(byte[] bytes, int offset, int count) {
        checkWritable();
        if (mode.append_) {
            // Dart: a write on a file opened to append always goes to the end
            position = length;
        }
        int at = (int) position;
        int end = at + count;
        ensure(end);
        // a write past the end leaves zeros between, which are as new as the write
        dirtyFrom = Math.min(dirtyFrom, Math.min(at, length));
        if (at > length) {
            java.util.Arrays.fill(data, length, at, (byte) 0);
        }
        System.arraycopy(bytes, offset, data, at, count);
        if (end > length) {
            length = end;
        }
        position = end;
    }

    private void ensure(int size) {
        if (size > data.length) {
            byte[] grown = new byte[Math.max(size, data.length * 2)];
            System.arraycopy(data, 0, grown, 0, length);
            data = grown;
        }
    }

    private void checkOpen() {
        if (closed) {
            throw new FileSystemException("File closed", path);
        }
    }

    private void checkReadable() {
        checkOpen();
        if (!mode.readable) {
            throw new FileSystemException("Cannot read from file, path = '" + path + "' (Bad file descriptor)",
                    path);
        }
    }

    private void checkWritable() {
        checkOpen();
        if (!mode.writable) {
            throw new FileSystemException("Cannot write to file, path = '" + path + "' (Bad file descriptor)",
                    path);
        }
    }

    private static void checkRange(long start, long end, int size) {
        if (start < 0 || start > end || end > size) {
            throw new dart.core.RangeError("Invalid range " + start + ".." + end + " of " + size);
        }
    }

    private static int sizeOf(Object buffer) {
        if (buffer instanceof Uint8List) {
            return (int) ((Uint8List) buffer).length();
        }
        if (buffer instanceof List) {
            return ((List<?>) buffer).size();
        }
        throw new dart.core.TypeError("Expected a List<int>");
    }

    @SuppressWarnings("unchecked")
    private static void store(Object buffer, int i, int value) {
        if (buffer instanceof Uint8List) {
            ((Uint8List) buffer).$indexSet(i, value);
        } else {
            ((List<Object>) buffer).set(i, Long.valueOf(value));
        }
    }

    private interface Op<T> {
        T run();
    }

    private static <T> Future<T> attempt(Op<T> op) {
        try {
            return Future.value(op.run());
        } catch (RuntimeException e) {
            return Future.error(e);
        }
    }
}
