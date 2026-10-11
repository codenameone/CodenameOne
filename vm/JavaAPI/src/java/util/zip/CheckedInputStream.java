/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * This code is licensed under GPLv2 with the Classpath exception.
 */
package java.util.zip;

import java.io.*;
/** A stream which updates the supplied checksum. */
public class CheckedInputStream extends FilterInputStream {
    private final Checksum checksum;
    public CheckedInputStream(InputStream stream, Checksum checksum) {
        super(stream);
        if (checksum == null)
            throw new NullPointerException();
        this.checksum = checksum;
    }
    public Checksum getChecksum() {
        return checksum;
    }
    public int read() throws IOException {
        int b = in.read();
        if (b >= 0)
            checksum.update(b);
        return b;
    }
    public int read(byte[] b, int off, int len) throws IOException {
        int n = in.read(b, off, len);
        if (n > 0)
            checksum.update(b, off, n);
        return n;
    }
    public long skip(long n) throws IOException {
        byte[] b = new byte[512];
        long total = 0;
        while (total < n) {
            int count = read(b, 0, (int) Math.min(n - total, b.length));
            if (count < 0)
                break;
            total += count;
        }
        return total;
    }
}
