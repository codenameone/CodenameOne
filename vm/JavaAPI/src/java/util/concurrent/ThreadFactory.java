package java.util.concurrent;

public interface ThreadFactory {
    Thread newThread(Runnable r);
}
