package java.util.concurrent.locks;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ReentrantReadWriteLock implements ReadWriteLock, java.io.Serializable {
    private static final long serialVersionUID = -6992448646407690164L;

    /** Inner class providing readlock */
    private ReadLock readerLock;
    /** Inner class providing writelock */
    private WriteLock writerLock;

    /** Performs all synchronization mechanics */
    private transient Object sync;

    private transient Thread writer;
    private transient int writeHolds;
    private transient int readers;
    private transient Map<Thread, Integer> readHolds;

    public ReentrantReadWriteLock() {
        this(false);
    }

    public ReentrantReadWriteLock(boolean fair) {
        sync = new Object();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
        readHolds = new HashMap<Thread, Integer>();
    }

    public ReadLock readLock() { return readerLock; }
    public WriteLock writeLock() { return writerLock; }

    public static class ReadLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -5992448646407690164L;
        private final ReentrantReadWriteLock lock;

        protected ReadLock(ReentrantReadWriteLock lock) {
            this.lock = lock;
        }

        public void lock() {
            synchronized (lock.sync) {
                Thread current = Thread.currentThread();
                // If there is a writer, and it's not us, we wait.
                // (Reentrancy: writer can acquire read lock)
                boolean interrupted = false;
                while (lock.writer != null && lock.writer != current) {
                    try {
                        lock.sync.wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }

                lock.readers++;
                Integer count = lock.readHolds.get(current);
                if (count == null) {
                    lock.readHolds.put(current, 1);
                } else {
                    lock.readHolds.put(current, count + 1);
                }
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        public void lockInterruptibly() throws InterruptedException {
            synchronized (lock.sync) {
                if (Thread.interrupted()) throw new InterruptedException();
                Thread current = Thread.currentThread();
                while (lock.writer != null && lock.writer != current) {
                    lock.sync.wait();
                }
                lock.readers++;
                Integer count = lock.readHolds.get(current);
                if (count == null) {
                    lock.readHolds.put(current, 1);
                } else {
                    lock.readHolds.put(current, count + 1);
                }
            }
        }

        public boolean tryLock() {
            synchronized (lock.sync) {
                Thread current = Thread.currentThread();
                if (lock.writer != null && lock.writer != current) {
                    return false;
                }
                lock.readers++;
                Integer count = lock.readHolds.get(current);
                if (count == null) {
                    lock.readHolds.put(current, 1);
                } else {
                    lock.readHolds.put(current, count + 1);
                }
                return true;
            }
        }

        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            long nanos = unit.toNanos(timeout);
            long end = System.currentTimeMillis() + unit.toMillis(timeout);

            synchronized (lock.sync) {
                if (Thread.interrupted()) throw new InterruptedException();
                Thread current = Thread.currentThread();

                if (lock.writer == null || lock.writer == current) {
                    lock.readers++;
                    Integer count = lock.readHolds.get(current);
                    if (count == null) {
                        lock.readHolds.put(current, 1);
                    } else {
                        lock.readHolds.put(current, count + 1);
                    }
                    return true;
                }

                long remaining = unit.toMillis(timeout);
                while (remaining > 0 && lock.writer != null && lock.writer != current) {
                    lock.sync.wait(remaining);
                    if (lock.writer == null || lock.writer == current) {
                         lock.readers++;
                         Integer count = lock.readHolds.get(current);
                         if (count == null) {
                             lock.readHolds.put(current, 1);
                         } else {
                             lock.readHolds.put(current, count + 1);
                         }
                         return true;
                    }
                    remaining = end - System.currentTimeMillis();
                }
                return false;
            }
        }

        public void unlock() {
            synchronized (lock.sync) {
                Thread current = Thread.currentThread();
                Integer count = lock.readHolds.get(current);
                if (count == null || count <= 0) {
                    throw new IllegalMonitorStateException();
                }
                int c = count - 1;
                if (c == 0) {
                    lock.readHolds.remove(current);
                } else {
                    lock.readHolds.put(current, c);
                }
                lock.readers--;
                if (lock.readers == 0) {
                    lock.sync.notifyAll(); // Notify potential writers
                }
            }
        }

        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }

        public String toString() {
            return super.toString() + "[ReadLock]";
        }
    }

    public static class WriteLock implements Lock, java.io.Serializable {
        private static final long serialVersionUID = -4992448646407690164L;
        private final ReentrantReadWriteLock lock;

        protected WriteLock(ReentrantReadWriteLock lock) {
            this.lock = lock;
        }

        public void lock() {
            synchronized (lock.sync) {
                Thread current = Thread.currentThread();
                if (lock.writer == current) {
                    lock.writeHolds++;
                    return;
                }

                boolean interrupted = false;
                while (lock.readers > 0 || lock.writer != null) {
                    try {
                        lock.sync.wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
                lock.writer = current;
                lock.writeHolds = 1;
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        public void lockInterruptibly() throws InterruptedException {
            synchronized (lock.sync) {
                if (Thread.interrupted()) throw new InterruptedException();
                Thread current = Thread.currentThread();
                if (lock.writer == current) {
                    lock.writeHolds++;
                    return;
                }
                while (lock.readers > 0 || lock.writer != null) {
                    lock.sync.wait();
                }
                lock.writer = current;
                lock.writeHolds = 1;
            }
        }

        public boolean tryLock() {
            synchronized (lock.sync) {
                Thread current = Thread.currentThread();
                if (lock.writer == current) {
                    lock.writeHolds++;
                    return true;
                }
                if (lock.readers == 0 && lock.writer == null) {
                    lock.writer = current;
                    lock.writeHolds = 1;
                    return true;
                }
                return false;
            }
        }

        public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
            long nanos = unit.toNanos(timeout);
            long end = System.currentTimeMillis() + unit.toMillis(timeout);

            synchronized (lock.sync) {
                if (Thread.interrupted()) throw new InterruptedException();
                Thread current = Thread.currentThread();
                if (lock.writer == current) {
                    lock.writeHolds++;
                    return true;
                }
                if (lock.readers == 0 && lock.writer == null) {
                    lock.writer = current;
                    lock.writeHolds = 1;
                    return true;
                }

                long remaining = unit.toMillis(timeout);
                while (remaining > 0) {
                     lock.sync.wait(remaining);
                     if (lock.writer == current) { // re-check after wait? No, logic above covers it.
                         lock.writeHolds++;
                         return true;
                     }
                     if (lock.readers == 0 && lock.writer == null) {
                         lock.writer = current;
                         lock.writeHolds = 1;
                         return true;
                     }
                     remaining = end - System.currentTimeMillis();
                }
                return false;
            }
        }

        public void unlock() {
            synchronized (lock.sync) {
                if (lock.writer != Thread.currentThread()) {
                    throw new IllegalMonitorStateException();
                }
                lock.writeHolds--;
                if (lock.writeHolds == 0) {
                    lock.writer = null;
                    lock.sync.notifyAll(); // Notify waiting readers or writers
                }
            }
        }

        public Condition newCondition() {
            // Simplified condition implementation reusing ReentrantLock's style if needed
            // But Condition for WriteLock usually requires full AQS support.
            // The ReentrantLock implementation uses ConditionObject which is tied to the lock instance.
            // I should probably support it if possible, but ReadWriteLock conditions are only supported for WriteLock.
            // For now, throwing UnsupportedOperationException as implementing Condition correctly for RWLock is non-trivial without AQS.
            throw new UnsupportedOperationException();
        }

        public boolean isHeldByCurrentThread() {
            synchronized (lock.sync) {
                return lock.writer == Thread.currentThread();
            }
        }

        public int getHoldCount() {
            synchronized (lock.sync) {
                return (lock.writer == Thread.currentThread()) ? lock.writeHolds : 0;
            }
        }

        public String toString() {
            return super.toString() + "[WriteLock]";
        }
    }

    public final boolean isFair() {
        return false;
    }

    protected Thread getOwner() {
        synchronized (sync) {
            return writer;
        }
    }

    public int getReadLockCount() {
        synchronized (sync) {
            return readers;
        }
    }

    public boolean isWriteLocked() {
        synchronized (sync) {
            return writer != null;
        }
    }

    public boolean isWriteLockedByCurrentThread() {
        synchronized (sync) {
            return writer == Thread.currentThread();
        }
    }

    public int getWriteHoldCount() {
        synchronized (sync) {
            return (writer == Thread.currentThread()) ? writeHolds : 0;
        }
    }

    public int getReadHoldCount() {
        synchronized (sync) {
            Integer count = readHolds.get(Thread.currentThread());
            return (count == null) ? 0 : count;
        }
    }

    protected Collection<Thread> getQueuedWriterThreads() {
        throw new RuntimeException("Not implemented");
    }

    protected Collection<Thread> getQueuedReaderThreads() {
        throw new RuntimeException("Not implemented");
    }

    public final boolean hasQueuedThreads() {
        throw new RuntimeException("Not implemented");
    }

    public final boolean hasQueuedThread(Thread thread) {
        throw new RuntimeException("Not implemented");
    }

    public final int getQueueLength() {
        throw new RuntimeException("Not implemented");
    }

    protected Collection<Thread> getQueuedThreads() {
        throw new RuntimeException("Not implemented");
    }

    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        sync = new Object();
        readHolds = new HashMap<Thread, Integer>();
        readerLock = new ReadLock(this);
        writerLock = new WriteLock(this);
    }
}
