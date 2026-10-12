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
import java.io.*;
import java.util.*;
import java.util.zip.*;
import com.codename1.io.zip.ZipArchiveOutputStream;

public class ZipApiApp {
    static void require(boolean value, String message) {
        if (!value)
            throw new RuntimeException(message);
    }
    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] b = new byte[7919];
        int n;
        while ((n = in.read(b)) != -1) out.write(b, 0, n);
        return out.toByteArray();
    }
    static ZipEntry entry(String name) {
        ZipEntry e = new ZipEntry(name);
        e.setTime(0);
        return e;
    }
    static void checksums() throws Exception {
        byte[] data = "123456789".getBytes("UTF-8");
        CRC32 crc = new CRC32();
        crc.update(data);
        require(crc.getValue() == 0xcbf43926L, "CRC32");
        Adler32 adler = new Adler32();
        adler.update(data);
        require(adler.getValue() == 0x091e01deL, "Adler32");
        crc.reset();
        CheckedOutputStream out = new CheckedOutputStream(new ByteArrayOutputStream(), crc);
        out.write(data);
        require(crc.getValue() == 0xcbf43926L, "checked output");
        adler.reset();
        CheckedInputStream in = new CheckedInputStream(new ByteArrayInputStream(data), adler);
        require(in.skip(3) == 3, "checked skip");
        readAll(in);
        require(adler.getValue() == 0x091e01deL, "checked input");
    }
    static byte[] payload() {
        byte[] data = new byte[180000];
        Random r = new Random(42);
        for (int i = 0; i < data.length; i++) data[i] = i % 5 == 0 ? (byte) r.nextInt() : (byte) (i % 31);
        return data;
    }
    static void compression(byte[] data) throws Exception {
        for (int raw = 0; raw < 2; raw++)
            for (int level = 0; level <= 9; level += 3) {
                Deflater def = new Deflater(level, raw == 1);
                Inflater inf = new Inflater(raw == 1);
                try {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    DeflaterOutputStream out = new DeflaterOutputStream(bytes, def, 137);
                    for (int at = 0; at < data.length; at += 101) out.write(data, at, Math.min(101, data.length - at));
                    out.finish();
                    require(Arrays.equals(data,
                                    readAll(new InflaterInputStream(
                                            new ByteArrayInputStream(bytes.toByteArray()), inf, 113))),
                            "deflate round trip");
                    require(def.getBytesRead() == data.length && inf.getBytesWritten() == data.length, "byte counts");
                    def.reset();
                    inf.reset();
                    require(def.getBytesRead() == 0 && inf.getBytesWritten() == 0, "reset");
                } finally {
                    def.end();
                    inf.end();
                }
            }
        byte[] dictionary = "shared-dictionary-shared-dictionary".getBytes("UTF-8");
        byte[] dictionaryData = new byte[10000];
        for (int n = 0; n < dictionaryData.length; n++) dictionaryData[n] = dictionary[n % dictionary.length];
        for (int raw = 0; raw < 2; raw++) {
            Deflater d = new Deflater(6, raw == 1);
            Inflater i = new Inflater(raw == 1);
            try {
                d.setDictionary(dictionary);
                d.setInput(dictionaryData);
                d.finish();
                ByteArrayOutputStream compressed = new ByteArrayOutputStream();
                byte[] buf = new byte[97];
                while (!d.finished()) {
                    int n = d.deflate(buf);
                    compressed.write(buf, 0, n);
                }
                if (raw == 1) i.setDictionary(dictionary);
                i.setInput(compressed.toByteArray());
                ByteArrayOutputStream decoded = new ByteArrayOutputStream();
                while (!i.finished()) {
                    int n = i.inflate(buf);
                    decoded.write(buf, 0, n);
                    if (i.needsDictionary()) {
                        int expected = i.getAdler();
                        boolean rejected = false;
                        try {
                            i.setDictionary(new byte[] {1, 2, 3});
                        } catch (IllegalArgumentException wrong) {
                            rejected = true;
                        }
                        require(rejected && i.needsDictionary() && i.getAdler() == expected,
                                "wrong dictionary preserves retry state");
                        i.setDictionary(dictionary);
                    }
                }
                require(Arrays.equals(dictionaryData, decoded.toByteArray()), "dictionary raw=" + raw);
            } finally {
                d.end();
                i.end();
            }
        }
    }

    static void gzip(byte[] data, String prefix) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        GZIPOutputStream g = new GZIPOutputStream(b, 123, true);
        g.write(data, 0, 37);
        g.flush();
        g.write(data, 37, data.length - 37);
        g.finish();
        g.close();
        byte[] encoded = b.toByteArray();
        try (FileOutputStream out = new FileOutputStream(prefix + ".gz")) {
            out.write(encoded);
        }
        require(Arrays.equals(data, readAll(new GZIPInputStream(new ByteArrayInputStream(encoded), 71))), "gzip");
        ByteArrayOutputStream twice = new ByteArrayOutputStream();
        twice.write(encoded);
        twice.write(encoded);
        require(readAll(new GZIPInputStream(new ByteArrayInputStream(twice.toByteArray()), 71)).length
                        == data.length * 2,
                "concatenated gzip");
        encoded[encoded.length - 8] ^= 1;
        boolean corrupt = false;
        try {
            readAll(new GZIPInputStream(new ByteArrayInputStream(encoded)));
        } catch (ZipException ex) {
            corrupt = true;
        }
        require(corrupt, "gzip crc failure");
    }
    static void archive(byte[] data, String prefix) throws Exception {
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(prefix + "-standard.zip"))) {
            out.putNextEntry(entry("standard.txt"));
            out.write(data);
            out.closeEntry();
            out.putNextEntry(entry("empty"));
            out.closeEntry();
            out.finish();
            out.finish();
        }
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(new FileOutputStream(prefix + ".zip"))) {
            out.setComment("archive comment");
            out.putNextEntry(entry("project/"), 0755);
            out.closeEntry();
            ZipEntry launcher = entry("project/run.sh");
            launcher.setComment("launcher");
            out.putNextEntry(launcher, 0755);
            out.write("#!/bin/sh\necho works\n".getBytes("UTF-8"));
            out.closeEntry();
            ZipEntry stored = entry("project/zero");
            stored.setMethod(ZipEntry.STORED);
            stored.setSize(0);
            stored.setCrc(0);
            out.putNextEntry(stored, 0644);
            out.closeEntry();
            out.putNextEntry(entry("project/caf\u00e9-\u05e9.txt"), 0644);
            for (int at = 0; at < data.length; at += 4093) out.write(data, at, Math.min(4093, data.length - at));
            out.closeEntry();
            boolean duplicate = false;
            try {
                out.putNextEntry(entry("project/run.sh"));
            } catch (ZipException ex) {
                duplicate = true;
            }
            require(duplicate, "duplicates refused");
            ZipEntry large = entry("large");
            large.setSize(0xffffffffL);
            boolean limit = false;
            try {
                out.putNextEntry(large);
            } catch (ZipException ex) {
                limit = true;
            }
            require(limit, "ZIP64 refused");
        }
        try (ZipInputStream in = new ZipInputStream(new FileInputStream(prefix + ".zip"))) {
            int entries = 0;
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                byte[] bytes = readAll(in);
                entries++;
                if (e.getName().endsWith(".txt"))
                    require(Arrays.equals(bytes, data), "ZIP payload");
            }
            require(entries == 4, "ZIP entry count");
        }
    }
    static class Sink extends OutputStream {
        int remaining = 80;
        boolean closed;
        public void write(int b) throws IOException {
            if (--remaining < 0)
                throw new IOException("cancelled");
        }
        public void close() {
            closed = true;
        }
    }
    static class Source extends ByteArrayInputStream {
        boolean closed;
        Source(byte[] data) {
            super(data);
        }
        public void close() {
            closed = true;
        }
    }
    static void cancellation(byte[] data) throws Exception {
        Sink sink = new Sink();
        boolean failed = false;
        Source source = new Source(data);
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(sink)) {
            out.putNextEntry(entry("payload"));
            try (InputStream input = source) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = input.read(buffer)) != -1) out.write(buffer, 0, n);
            }
            out.finish();
        } catch (IOException ex) {
            failed = true;
        }
        require(failed && sink.closed && source.closed, "cancelled source and destination closed");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ZipArchiveOutputStream out = new ZipArchiveOutputStream(bytes);
        out.putNextEntry(entry("first"));
        out.closeEntry();
        out.finish();
        bytes.write(42);
        require(bytes.toByteArray()[bytes.size() - 1] == 42, "finish leaves destination open");
        out.close();
    }
    static class Discard extends OutputStream {
        public void write(int value) {
        }
        public void write(byte[] b, int off, int len) {
        }
    }
    static ZipEntry emptyStored(String name) {
        ZipEntry e = entry(name);
        e.setMethod(ZipEntry.STORED);
        e.setSize(0);
        e.setCrc(0);
        return e;
    }
    static void limits() throws Exception {
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(new Discard())) {
            for (int i = 0; i < 65534; i++) {
                out.putNextEntry(emptyStored("f" + i));
                out.closeEntry();
            }
            boolean refused = false;
            try {
                out.putNextEntry(emptyStored("overflow"));
            } catch (ZipException ex) {
                refused = true;
            }
            require(refused, "entry count limit");
        }
        char[] chars = new char[65535];
        Arrays.fill(chars, 'x');
        String comment = new String(chars);
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(new Discard())) {
            boolean refused = false;
            for (int i = 0; i < 300; i++) {
                ZipEntry e = emptyStored("f" + i);
                e.setComment(comment);
                try {
                    out.putNextEntry(e);
                    out.closeEntry();
                } catch (ZipException ex) {
                    refused = true;
                    break;
                }
            }
            require(refused, "metadata memory limit");
        }
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(new Discard())) {
            ZipEntry e = entry("bad-extra");
            e.setExtra(new byte[] {1, 0, 0, 0});
            boolean refused = false;
            try {
                out.putNextEntry(e);
            } catch (ZipException ex) {
                refused = true;
            }
            require(refused, "ZIP64 extra refused");
            e.setExtra(new byte[] {2, 0, 9, 0});
            refused = false;
            try {
                out.putNextEntry(e);
            } catch (ZipException ex) {
                refused = true;
            }
            require(refused, "malformed extra refused");
        }
        boolean mismatch = false;
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(new Discard())) {
            ZipEntry e = entry("stored");
            e.setMethod(ZipEntry.STORED);
            e.setSize(1);
            e.setCrc(0);
            out.putNextEntry(e);
            out.write(42);
            out.closeEntry();
        } catch (ZipException ex) {
            mismatch = true;
        }
        require(mismatch, "STORED CRC checked");
    }
    static void badZip() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            ZipEntry e = entry("stored");
            e.setMethod(ZipEntry.STORED);
            e.setSize(1);
            CRC32 crc = new CRC32();
            crc.update(42);
            e.setCrc(crc.getValue());
            out.putNextEntry(e);
            out.write(42);
            out.closeEntry();
        }
        byte[] archive = bytes.toByteArray();
        int payloadAt = 30 + (archive[26] & 255) + ((archive[27] & 255) << 8) + (archive[28] & 255)
                + ((archive[29] & 255) << 8);
        archive[payloadAt] ^= 1;
        boolean corrupt = false;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(archive))) {
            in.getNextEntry();
            readAll(in);
        } catch (ZipException ex) {
            corrupt = true;
        }
        require(corrupt, "ZIP CRC checked");
        boolean truncated = false;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(archive, 0, payloadAt - 1))) {
            in.getNextEntry();
        } catch (EOFException ex) {
            truncated = true;
        }
        require(truncated, "truncated ZIP header");
    }
    static void checkedCloseFailures(boolean customRuntime) throws Exception {
        for (int mode = 0; mode <= 3; mode++) {
            final int failures = mode;
            final int[] calls = {0, 0};
            OutputStream sink = new OutputStream() {
                public void write(int value) { }
                public void flush() throws IOException {
                    calls[0]++;
                    if ((failures & 1) != 0) throw new IOException("flush failed");
                }
                public void close() throws IOException {
                    calls[1]++;
                    if ((failures & 2) != 0) throw new IOException("close failed");
                }
            };
            boolean rejected = false;
            CheckedOutputStream checked = new CheckedOutputStream(sink, new CRC32());
            try { checked.close(); }
            catch (IOException expected) { rejected = true; }
            require(rejected == (mode != 0), "checked stream reports destination errors");
            // JDK 8 predates the idempotent FilterOutputStream close guard.
            if (customRuntime) checked.close();
            require(calls[0] == 1 && calls[1] == 1, "checked close attempts flush/close exactly once");
        }
    }
    static byte[] zipNameHeader(byte[] name, boolean utf8) {
        byte[] header = new byte[30 + name.length];
        header[0] = 0x50; header[1] = 0x4b; header[2] = 3; header[3] = 4;
        header[4] = 20;
        header[7] = (byte) (utf8 ? 8 : 0);
        header[12] = 0x21; // 1980-01-01 DOS date
        header[26] = (byte) name.length;
        header[27] = (byte) (name.length >>> 8);
        System.arraycopy(name, 0, header, 30, name.length);
        return header;
    }
    static void zipNameDecoding(boolean customRuntime) throws Exception {
        for (byte[] name : new byte[][] {{-1}, {-64, -81}, {-30, -126}, {-19, -96, -128}}) {
            // The UTF-8 flag overrides the caller's otherwise valid Latin-1 charset.
            boolean rejected = false;
            try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zipNameHeader(name, true)),
                    java.nio.charset.Charset.forName("ISO-8859-1"))) {
                in.getNextEntry();
            } catch (ZipException expected) { rejected = true; }
            catch (IllegalArgumentException expected) { require(!customRuntime, "runtime uses ZipException"); rejected = true; }
            require(rejected, "malformed UTF-8 ZIP name rejected: " + Arrays.toString(name));
        }
        boolean rejected = false;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zipNameHeader(new byte[] {-1}, false)),
                java.nio.charset.Charset.forName("US-ASCII"))) {
            in.getNextEntry();
        } catch (ZipException expected) { rejected = true; }
        catch (IllegalArgumentException expected) { require(!customRuntime, "runtime uses ZipException"); rejected = true; }
        require(rejected, "malformed caller charset ZIP name rejected");
        for (String name : new String[] {"replacement-\ufffd", "unicode-\ud83d\ude00"}) {
            try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zipNameHeader(name.getBytes("UTF-8"), true)))) {
                require(name.equals(in.getNextEntry().getName()), "valid UTF-8 name preserved");
            }
        }
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zipNameHeader(new byte[] {-1}, false)),
                java.nio.charset.Charset.forName("ISO-8859-1"))) {
            require("\u00ff".equals(in.getNextEntry().getName()), "valid caller charset name preserved");
        }
    }
    static void skipZipEntries() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream out = new ZipOutputStream(bytes)) {
            for (int i = 0; i < 100; i++) {
                out.putNextEntry(entry("entry-" + i));
                if ((i & 1) == 0) out.write(new byte[9000]);
                out.closeEntry();
            }
        }
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            for (int i = 0; i < 100; i++) {
                require(("entry-" + i).equals(in.getNextEntry().getName()), "drain advances to next entry");
                if (i % 3 == 0) readAll(in);
                else if (i % 3 == 1) in.closeEntry();
            }
            require(in.getNextEntry() == null, "drained all entries");
            in.closeEntry();
        }
    }
    static void changeParameters(Deflater def, int change) {
        if (change == 0) def.setLevel(9); // Same compression algorithm as the default level.
        else if (change == 1) def.setLevel(1); // Changing algorithm may itself emit bytes.
        else def.setStrategy(Deflater.HUFFMAN_ONLY);
    }
    static void requireFlushed(byte[] compressed, byte[] expected) throws Exception {
        Inflater inf = new Inflater();
        try {
            inf.setInput(compressed);
            byte[] actual = new byte[expected.length + 1];
            int count = 0, n;
            while ((n = inf.inflate(actual, count, actual.length - count)) > 0) count += n;
            require(count == expected.length && Arrays.equals(expected, Arrays.copyOf(actual, count)),
                    "parameter change flush exposes entire payload before finish");
        } finally { inf.end(); }
    }
    static void parameterFlushes() throws Exception {
        byte[] data = "buffered payload before parameter change".getBytes("UTF-8");
        for (int change = 0; change < 3; change++) {
            for (int size : new int[] {1, 8, 512}) {
                Deflater def = new Deflater();
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (DeflaterOutputStream out = new DeflaterOutputStream(bytes, def, size, true)) {
                    out.write(data);
                    changeParameters(def, change);
                    out.flush();
                    requireFlushed(bytes.toByteArray(), data);
                } finally { def.end(); }
            }
            for (int flush : new int[] {Deflater.SYNC_FLUSH, Deflater.FULL_FLUSH}) {
                for (int size : new int[] {1, 8, 512}) {
                    Deflater def = new Deflater();
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    try {
                        byte[] output = new byte[512];
                        def.setInput(data);
                        while (!def.needsInput()) {
                            int n = def.deflate(output);
                            bytes.write(output, 0, n);
                        }
                        changeParameters(def, change);
                        int n = def.deflate(output, 0, size, flush);
                        bytes.write(output, 0, n);
                        if (n == size) {
                            // Drain tiny initial buffers with enough room to finish the flush marker.
                            do {
                                n = def.deflate(output, 0, output.length, flush);
                                bytes.write(output, 0, n);
                            } while (n == output.length);
                        }
                        requireFlushed(bytes.toByteArray(), data);
                    } finally { def.end(); }
                }
            }
        }
    }
    static void gzipLiveSource() throws Exception {
        byte[] data = "completed member on an open source".getBytes("UTF-8");
        ByteArrayOutputStream member = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(member)) { out.write(data); }
        for (int members = 1; members <= 2; members++) {
            for (int size : new int[] {1, 512}) {
                ByteArrayOutputStream encoded = new ByteArrayOutputStream();
                ByteArrayOutputStream expected = new ByteArrayOutputStream();
                for (int i = 0; i < members; i++) {
                    encoded.write(member.toByteArray());
                    expected.write(data);
                }
                final ByteArrayInputStream arrived = new ByteArrayInputStream(encoded.toByteArray());
                final boolean advertise = size == 1;
                InputStream live = new InputStream() {
                    public int available() { return advertise ? arrived.available() : 0; }
                    public int read() {
                        require(arrived.available() > 0, "GZIP must not probe an idle live source");
                        return arrived.read();
                    }
                    public int read(byte[] b, int off, int len) {
                        require(arrived.available() > 0, "GZIP must not fill from an idle live source");
                        return arrived.read(b, off, len);
                    }
                };
                try (GZIPInputStream in = new GZIPInputStream(live, size)) {
                    // With size 512, concatenated members are already buffered
                    // even though the source advertises no additional bytes.
                    require(Arrays.equals(expected.toByteArray(), readAll(in)), "live GZIP members");
                    require(in.read() == -1 && in.available() == 0, "completed live GZIP stays at EOF");
                }
            }
        }
    }
    static void compatibilityRegressions(boolean customRuntime) throws Exception {
        byte[] data = "flushed payload".getBytes("UTF-8");
        // JDK 8 itself loops indefinitely with buffers smaller than the flush marker.
        for (int size = customRuntime ? 1 : 8; size <= 8; size++) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            Deflater def = new Deflater();
            try (DeflaterOutputStream out = new DeflaterOutputStream(bytes, def, size, true)) {
                out.flush();
                out.write(data);
                out.flush();
                out.flush();
                out.write(data);
            } finally {
                def.end();
            }
            require(bytes.size() < 200, "tiny flush buffer stays bounded");
            byte[] twice = new byte[data.length * 2];
            System.arraycopy(data, 0, twice, 0, data.length);
            System.arraycopy(data, 0, twice, data.length, data.length);
            require(Arrays.equals(twice, readAll(new InflaterInputStream(
                    new ByteArrayInputStream(bytes.toByteArray())))), "tiny buffer sync flush");
            bytes.reset();
            try (GZIPOutputStream out = new GZIPOutputStream(bytes, size, true)) {
                out.write(data);
                out.flush();
                out.flush();
            }
            require(Arrays.equals(data, readAll(new GZIPInputStream(
                    new ByteArrayInputStream(bytes.toByteArray())))), "tiny gzip sync flush");
        }
        ByteArrayOutputStream member = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(member)) { out.write(data); }
        for (byte[] tail : new byte[][] { {42}, {31}, {31, -117}, {31, -117, 0, 0, 0, 0, 0, 0, 0, 0} }) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bytes.write(member.toByteArray());
            bytes.write(tail);
            require(Arrays.equals(data, readAll(new GZIPInputStream(
                    new ByteArrayInputStream(bytes.toByteArray())))), "gzip trailing bytes");
        }
        boolean badHeader = false;
        try { new GZIPInputStream(new ByteArrayInputStream(new byte[] {42})); }
        catch (IOException expected) { badHeader = true; }
        require(badHeader, "first gzip header remains strict");

        for (String name : new String[] {"\u00e9", "\ud800", "\udc00"}) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(bytes,
                    java.nio.charset.Charset.forName("US-ASCII"))) {
                boolean rejected = false;
                try { out.putNextEntry(entry(name)); }
                catch (IOException expected) { rejected = true; }
                require(rejected, "lossy ZIP encoding rejected");
                out.putNextEntry(entry("?"));
                out.closeEntry();
            }
        }
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(new ByteArrayOutputStream())) {
            boolean rejected = false;
            try { out.putNextEntry(entry("\ud800")); }
            catch (IOException expected) { rejected = true; }
            require(rejected, "malformed UTF-8 surrogate rejected");
            out.putNextEntry(entry("unicode-\ud83d\ude00"));
            out.closeEntry();
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ZipEntry future = entry("future");
        future.setExtra(new byte[] {0x55, 0x54, 5, 0, 1, 0, 0, 0, 0, 0x34, 0x12, 1, 0, 42});
        future.setTime(2208988800000L); // 2040-01-01, outside signed 32-bit UT time.
        try (ZipArchiveOutputStream out = new ZipArchiveOutputStream(bytes)) {
            out.putNextEntry(future);
            out.closeEntry();
        }
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            ZipEntry restored = in.getNextEntry();
            require(restored.getTime() == future.getTime(), "stale UT timestamp removed");
            require(Arrays.equals(restored.getExtra(), new byte[] {0x34, 0x12, 1, 0, 42}),
                    "unrelated extra preserved");
        }
    }
    public static void main(String[] args) throws Exception {
        byte[] data = payload();
        checksums();
        gzipLiveSource();
        checkedCloseFailures(args.length > 1);
        skipZipEntries();
        zipNameDecoding(args.length > 1);
        // JDK 8 also returns early after parameter changes; enforce the stronger
        // flush guarantee on the patched JavaAPI and translated runtime.
        if (args.length > 1) parameterFlushes();
        compatibilityRegressions(args.length > 1);
        compression(data);
        gzip(data, args[0]);
        archive(data, args[0]);
        cancellation(data);
        limits();
        badZip();
        if (args.length > 1) {
            try (ZipInputStream in = new ZipInputStream(new FileInputStream(args[1]))) {
                require(in.getNextEntry() != null, "JDK fixture entry");
                require(Arrays.equals(readAll(in), data), "JDK fixture payload");
                require(in.getNextEntry() != null, "JDK empty fixture entry");
                require(readAll(in).length == 0, "JDK empty fixture");
                require(in.getNextEntry() == null, "JDK fixture end");
            }
        }
        System.out.println("ZIP_API_OK");
    }
}
