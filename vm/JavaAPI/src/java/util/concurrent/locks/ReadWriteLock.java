package java.util.concurrent.locks;

public interface ReadWriteLock {
    Lock readLock();
    Lock writeLock();
}
