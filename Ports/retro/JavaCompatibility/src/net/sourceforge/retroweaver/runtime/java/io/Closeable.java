package net.sourceforge.retroweaver.runtime.java.io;

import java.io.IOException;

public interface Closeable {

    public void close() throws IOException;
}