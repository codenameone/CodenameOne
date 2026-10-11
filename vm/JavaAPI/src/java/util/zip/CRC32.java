/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * This code is licensed under GPLv2 with the Classpath exception.
 */
package java.util.zip;

/** Standard CRC32 checksum. */
public class CRC32 implements Checksum {
    private final com.codename1.impl.zip.CRC32 delegate = new com.codename1.impl.zip.CRC32();
    public void update(int value) {
        update(new byte[] {(byte) value}, 0, 1);
    }
    public void update(byte[] data) {
        update(data, 0, data.length);
    }
    public void update(byte[] data, int offset, int length) {
        if (data == null)
            throw new NullPointerException();
        if (offset < 0 || length < 0 || offset > data.length - length)
            throw new ArrayIndexOutOfBoundsException();
        delegate.update(data, offset, length);
    }
    public long getValue() {
        return delegate.getValue();
    }
    public void reset() {
        delegate.reset();
    }
}
