package java.nio.file;

public interface WatchEvent<T> {
    Kind<T> kind();
    int count();
    T context();

    interface Kind<T> {
        String name();
        Class<T> type();
    }

    interface Modifier {
        String name();
    }
}
