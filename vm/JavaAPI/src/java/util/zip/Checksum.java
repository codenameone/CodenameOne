/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * This code is licensed under GPLv2 with the Classpath exception.
 */
package java.util.zip;

/** A running checksum of bytes. */
public interface Checksum {
    void update(int value);
    void update(byte[] data, int offset, int length);
    long getValue();
    void reset();
}
