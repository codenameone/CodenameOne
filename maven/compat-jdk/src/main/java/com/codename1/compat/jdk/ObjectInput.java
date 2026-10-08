package com.codename1.compat.jdk;

import java.io.DataInput;
import java.io.IOException;

/// `java.io.ObjectInput` for the Codename One runtime: the stream an
/// `Externalizable` class reads itself from. See [ObjectOutput].
public interface ObjectInput extends DataInput, AutoCloseable {

    Object readObject() throws ClassNotFoundException, IOException;

    int read() throws IOException;

    int read(byte[] b) throws IOException;

    int read(byte[] b, int off, int len) throws IOException;

    long skip(long n) throws IOException;

    int available() throws IOException;

    @Override
    void close() throws IOException;
}
