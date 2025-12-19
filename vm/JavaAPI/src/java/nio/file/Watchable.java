package java.nio.file;

import java.io.IOException;

public interface Watchable {
    WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException;
    WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException;
}
