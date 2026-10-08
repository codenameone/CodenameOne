package com.codename1.compat.jdk;

import java.io.DataOutput;
import java.io.IOException;

/// `java.io.ObjectOutput` for the Codename One runtime: the stream an
/// `Externalizable` class writes itself to.
///
/// The interface is what desktop code is compiled against, so it is here for
/// classes that implement `writeExternal`. Nothing on a device implements it:
/// Java serialization needs reflection to find the classes it writes.
public interface ObjectOutput extends DataOutput, AutoCloseable {

    void writeObject(Object obj) throws IOException;

    void flush() throws IOException;

    @Override
    void close() throws IOException;
}
