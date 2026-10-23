/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.reel.packager;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/// ISO BMFF helpers: a box walker and the PSSH injection ffmpeg does not do.
final class Mp4 {
    /// SystemID of the W3C Common PSSH box (ClearKey): 1077efec-c0b2-4d02-ace3-3c1e52e2fb4b.
    static final byte[] COMMON_SYSTEM_ID = Hex.decode("1077efecc0b24d02ace33c1e52e2fb4b");

    private Mp4() {
    }

    private static int i32(byte[] d, int o) {
        return ((d[o] & 0xff) << 24) | ((d[o + 1] & 0xff) << 16) | ((d[o + 2] & 0xff) << 8) | (d[o + 3] & 0xff);
    }

    private static String type(byte[] d, int o) {
        return new String(d, o + 4, 4, java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    /// Finds a box by slash separated path (for example `moov/trak/mdia`)
    /// and returns `{boxStart, boxEnd}`, or null when it is absent. Descends into
    /// `stsd` and the encrypted sample entries, which carry fixed headers before their children.
    static int[] find(byte[] d, int start, int end, String path) {
        String[] parts = path.split("/");
        int s = start;
        int e = end;
        int[] box = null;
        for (String part : parts) {
            box = null;
            int p = s;
            while (p + 8 <= e) {
                long size = i32(d, p) & 0xffffffffL;
                int hdr = 8;
                if (size == 1) {
                    size = ByteBuffer.wrap(d, p + 8, 8).getLong();
                    hdr = 16;
                } else if (size == 0) {
                    size = e - p;
                }
                if (size < hdr || p + size > e) {
                    break;
                }
                if (type(d, p).equals(part)) {
                    box = new int[] {p, (int) (p + size)};
                    s = p + hdr + childOffset(part);
                    e = (int) (p + size);
                    break;
                }
                p += (int) size;
            }
            if (box == null) {
                return null;
            }
        }
        return box;
    }

    private static int childOffset(String type) {
        switch (type) {
            case "stsd":
                return 8;
            case "encv":
                return 78;
            case "enca":
                return 28;
            default:
                return 0;
        }
    }

    /// Builds a version 1 W3C Common PSSH box listing the key id, with no data.
    static byte[] psshBox(byte[] kid) {
        ByteBuffer b = ByteBuffer.allocate(52);
        b.putInt(52);
        b.put(new byte[] {'p', 's', 's', 'h'});
        b.putInt(0x01000000);
        b.put(COMMON_SYSTEM_ID);
        b.putInt(1);
        b.put(kid);
        b.putInt(0);
        return b.array();
    }

    /// Appends a common PSSH box to `moov` in place. The moov size is fixed up, and when
    /// moov precedes mdat (not fragmented) every stco/co64 chunk offset is shifted.
    static void injectPssh(Path file, byte[] kid) throws IOException {
        byte[] pssh = psshBox(kid);
        long len = Files.size(file);
        long moovOff = -1;
        long moovSize = 0;
        boolean mdatAfter = false;
        try (FileChannel ch = FileChannel.open(file, StandardOpenOption.READ)) {
            long p = 0;
            ByteBuffer h = ByteBuffer.allocate(16);
            while (p + 8 <= len) {
                h.clear();
                ch.read(h, p);
                byte[] hd = h.array();
                long size = i32(hd, 0) & 0xffffffffL;
                if (size == 1) {
                    size = ByteBuffer.wrap(hd, 8, 8).getLong();
                } else if (size == 0) {
                    size = len - p;
                }
                String t = type(hd, 0);
                if (t.equals("moov")) {
                    moovOff = p;
                    moovSize = size;
                } else if (t.equals("mdat") && moovOff >= 0) {
                    mdatAfter = true;
                }
                if (size < 8) {
                    break;
                }
                p += size;
            }
            if (moovOff < 0 || moovSize > Integer.MAX_VALUE) {
                throw new IOException("No usable moov box in " + file);
            }
            byte[] moov = new byte[(int) moovSize];
            ch.read(ByteBuffer.wrap(moov), moovOff);
            byte[] out = Arrays.copyOf(moov, moov.length + pssh.length);
            System.arraycopy(pssh, 0, out, moov.length, pssh.length);
            ByteBuffer.wrap(out).putInt(0, out.length);
            if (mdatAfter) {
                shiftOffsets(out, pssh.length);
            }
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStream os = Files.newOutputStream(tmp)) {
                copy(ch, 0, moovOff, os);
                os.write(out);
                copy(ch, moovOff + moovSize, len - moovOff - moovSize, os);
            }
            ch.close();
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void shiftOffsets(byte[] moov, int delta) {
        int p = 8;
        while (p + 8 <= moov.length) {
            int size = i32(moov, p);
            if (size < 8) {
                break;
            }
            if (type(moov, p).equals("trak")) {
                int[] stbl = find(moov, p + 8, p + size, "mdia/minf/stbl");
                if (stbl != null) {
                    for (String t : new String[] {"stco", "co64"}) {
                        int[] b = find(moov, stbl[0] + 8, stbl[1], t);
                        if (b != null) {
                            int n = i32(moov, b[0] + 12);
                            int q = b[0] + 16;
                            for (int i = 0; i < n; i++) {
                                if (t.equals("stco")) {
                                    ByteBuffer.wrap(moov).putInt(q, i32(moov, q) + delta);
                                    q += 4;
                                } else {
                                    ByteBuffer bb = ByteBuffer.wrap(moov);
                                    bb.putLong(q, bb.getLong(q) + delta);
                                    q += 8;
                                }
                            }
                        }
                    }
                }
            }
            p += size;
        }
    }

    private static void copy(FileChannel ch, long from, long count, OutputStream os) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(1 << 20);
        long done = 0;
        while (done < count) {
            buf.clear();
            buf.limit((int) Math.min(buf.capacity(), count - done));
            int n = ch.read(buf, from + done);
            if (n <= 0) {
                break;
            }
            os.write(buf.array(), 0, n);
            done += n;
        }
    }
}
