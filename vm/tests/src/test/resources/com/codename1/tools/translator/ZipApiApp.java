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
                    if (i.needsDictionary()) i.setDictionary(dictionary);
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
    public static void main(String[] args) throws Exception {
        byte[] data = payload();
        checksums();
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
