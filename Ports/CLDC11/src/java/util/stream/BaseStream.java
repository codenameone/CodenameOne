package java.util.stream;

import java.util.Iterator;

public interface BaseStream<T, S extends BaseStream<T, S>> extends AutoCloseable {
    Iterator<T> iterator();

    S sequential();

    S parallel();

    void close();
}
