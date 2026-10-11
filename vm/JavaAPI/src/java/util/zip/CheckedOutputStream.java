/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * This code is licensed under GPLv2 with the Classpath exception.
 */
package java.util.zip;

import java.io.*;
/** A stream which updates the supplied checksum. */
public class CheckedOutputStream extends FilterOutputStream {
    private final Checksum checksum;
    public CheckedOutputStream(OutputStream stream, Checksum checksum) {
        super(stream);
        if (checksum == null)
            throw new NullPointerException();
        this.checksum = checksum;
    }
    public Checksum getChecksum() {
        return checksum;
    }
    public void write(int b) throws IOException {
        out.write(b);
        checksum.update(b);
    }
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        checksum.update(b, off, len);
    }
}
