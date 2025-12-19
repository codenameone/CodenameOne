package java.nio.file;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public interface WatchService extends Closeable {
    WatchKey poll();
    WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException;
    WatchKey take() throws InterruptedException;
    void close() throws IOException;
}
