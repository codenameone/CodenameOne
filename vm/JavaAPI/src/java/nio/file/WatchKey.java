package java.nio.file;

import java.nio.file.WatchEvent.Kind;
import java.util.List;

public interface WatchKey {
    boolean isValid();
    List<WatchEvent<?>> pollEvents();
    boolean reset();
    void cancel();
    Watchable watchable();
}
