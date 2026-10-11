/* Copyright (c) 2026 Codename One. GPLv2 with the Classpath exception. */
package java.util.zip;
import java.io.*;

/** RFC 1952 encoder with deterministic (zero) header timestamp. */
public class GZIPOutputStream extends DeflaterOutputStream {
    protected CRC32 crc = new CRC32();
    private boolean trailer;
    public GZIPOutputStream(OutputStream out) throws IOException {
        this(out, 512, false);
    }
    public GZIPOutputStream(OutputStream out, int size) throws IOException {
        this(out, size, false);
    }
    public GZIPOutputStream(OutputStream out, boolean syncFlush) throws IOException {
        this(out, 512, syncFlush);
    }
    public GZIPOutputStream(OutputStream out, int size, boolean syncFlush) throws IOException {
        super(out, new Deflater(Deflater.DEFAULT_COMPRESSION, true), size, syncFlush);
        try {
            out.write(new byte[] {31, (byte) 139, 8, 0, 0, 0, 0, 0, 0, (byte) 255});
        } catch (IOException ex) {
            def.end();
            throw ex;
        }
    }
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        crc.update(b, off, len);
    }
    public void finish() throws IOException {
        if (trailer)
            return;
        super.finish();
        byte[] b = new byte[8];
        long checksum = crc.getValue(), size = def.getBytesRead();
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (checksum >>> (8 * i));
            b[4 + i] = (byte) (size >>> (8 * i));
        }
        out.write(b);
        trailer = true;
    }
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            def.end();
        }
    }
}
