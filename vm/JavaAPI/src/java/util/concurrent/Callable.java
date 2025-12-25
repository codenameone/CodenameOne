package java.util.concurrent;

public interface Callable<V> {
    V call() throws Exception;
}
