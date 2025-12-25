package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;

/**
 * A capability-based lock with three modes for controlling read/write
 * access.
 */
public class StampedLock implements java.io.Serializable {
    private static final long serialVersionUID = -6001602636862214143L;

    private static final long WRITE_MASK = 0x8000000000000000L;
    private static final long READ_LOCK_BIT = 1L;
    private static final long VERSION_MASK = ~(WRITE_MASK | READ_LOCK_BIT);

    private transient Object sync;
    // Start at 256 to avoid 0 and allow some initial headroom
    private transient long version;
    private transient int readers;
    private transient boolean writing;

    public StampedLock() {
        sync = new Object();
        version = 256;
        readers = 0;
        writing = false;
    }

    public long writeLock() {
        boolean interrupted = false;
        synchronized (sync) {
            while (writing || readers > 0) {
                try {
                    sync.wait();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            writing = true;
            long stamp = version | WRITE_MASK;
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            return stamp;
        }
    }

    public long tryWriteLock() {
        synchronized (sync) {
            if (writing || readers > 0) return 0L;
            writing = true;
            return version | WRITE_MASK;
        }
    }

    public long tryWriteLock(long time, TimeUnit unit) throws InterruptedException {
        long timeout = unit.toMillis(time);
        long deadline = System.currentTimeMillis() + timeout;
        synchronized (sync) {
            if (Thread.interrupted()) throw new InterruptedException();
            while (writing || readers > 0) {
                long timeLeft = deadline - System.currentTimeMillis();
                if (timeLeft <= 0) return 0L;
                sync.wait(timeLeft);
            }
            writing = true;
            return version | WRITE_MASK;
        }
    }

    public long writeLockInterruptibly() throws InterruptedException {
        synchronized (sync) {
            if (Thread.interrupted()) throw new InterruptedException();
            while (writing || readers > 0) {
                sync.wait();
            }
            writing = true;
            return version | WRITE_MASK;
        }
    }

    public long readLock() {
        boolean interrupted = false;
        synchronized (sync) {
            while (writing) {
                try {
                    sync.wait();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
            readers++;
            long stamp = version | READ_LOCK_BIT;
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
            return stamp;
        }
    }

    public long tryReadLock() {
        synchronized (sync) {
            if (writing) return 0L;
            readers++;
            return version | READ_LOCK_BIT;
        }
    }

    public long tryReadLock(long time, TimeUnit unit) throws InterruptedException {
        long timeout = unit.toMillis(time);
        long deadline = System.currentTimeMillis() + timeout;
        synchronized (sync) {
             if (Thread.interrupted()) throw new InterruptedException();
             while (writing) {
                 long timeLeft = deadline - System.currentTimeMillis();
                 if (timeLeft <= 0) return 0L;
                 sync.wait(timeLeft);
             }
             readers++;
             return version | READ_LOCK_BIT;
        }
    }

    public long readLockInterruptibly() throws InterruptedException {
        synchronized (sync) {
            if (Thread.interrupted()) throw new InterruptedException();
            while (writing) {
                sync.wait();
            }
            readers++;
            return version | READ_LOCK_BIT;
        }
    }

    public long tryOptimisticRead() {
        synchronized (sync) {
            if (writing) return 0L;
            return version;
        }
    }

    public boolean validate(long stamp) {
        synchronized (sync) {
            return !writing && (stamp & VERSION_MASK) == version;
        }
    }

    public void unlockWrite(long stamp) {
        synchronized (sync) {
            if (!writing || (stamp & WRITE_MASK) == 0 || (stamp & VERSION_MASK) != version) {
                throw new IllegalMonitorStateException();
            }
            writing = false;
            version += 2; // Increment version
            if (version == 0) version = 256;
            sync.notifyAll();
        }
    }

    public void unlockRead(long stamp) {
        synchronized (sync) {
            if (readers <= 0 || (stamp & READ_LOCK_BIT) == 0 || (stamp & VERSION_MASK) != version) {
                throw new IllegalMonitorStateException();
            }
            readers--;
            if (readers == 0) {
                sync.notifyAll();
            }
        }
    }

    public void unlock(long stamp) {
        synchronized (sync) {
            if ((stamp & WRITE_MASK) != 0) {
                unlockWrite(stamp);
            } else {
                unlockRead(stamp);
            }
        }
    }

    public long tryConvertToWriteLock(long stamp) {
        synchronized (sync) {
            if ((stamp & VERSION_MASK) != version) return 0L;

            if ((stamp & WRITE_MASK) != 0) {
                if (writing) return stamp;
                return 0L;
            }

            if ((stamp & READ_LOCK_BIT) != 0) {
                // Read lock
                if (writing) return 0L;
                if (readers == 1) {
                    readers = 0;
                    writing = true;
                    return version | WRITE_MASK;
                }
                return 0L;
            } else {
                // Optimistic
                if (writing) return 0L;
                if (readers == 0) {
                    writing = true;
                    return version | WRITE_MASK;
                }
                return 0L;
            }
        }
    }

    public long tryConvertToReadLock(long stamp) {
        synchronized (sync) {
             if ((stamp & VERSION_MASK) != version) return 0L;

             if ((stamp & READ_LOCK_BIT) != 0) {
                 // Already read lock
                 return stamp;
             }

             if ((stamp & WRITE_MASK) != 0) {
                 // Write lock -> downgrade
                 if (writing) {
                     writing = false;
                     readers++;
                     version += 2;
                     if (version == 0) version = 256;
                     sync.notifyAll();
                     // Return new version | READ_LOCK_BIT
                     return version | READ_LOCK_BIT;
                 }
                 return 0L;
             }

             // Optimistic -> Read
             if (writing) return 0L;
             readers++;
             return version | READ_LOCK_BIT;
        }
    }

    public long tryConvertToOptimisticRead(long stamp) {
        synchronized (sync) {
            if ((stamp & VERSION_MASK) != version) return 0L;

            // If Write Lock
            if ((stamp & WRITE_MASK) != 0) {
                if (writing) {
                    writing = false;
                    version += 2;
                    if (version == 0) version = 256;
                    sync.notifyAll();
                    return version; // New version
                }
                return 0L;
            }

            // If Read Lock
            if ((stamp & READ_LOCK_BIT) != 0) {
                if (readers > 0) {
                    readers--;
                    if (readers == 0) sync.notifyAll();
                    return version;
                }
                return 0L;
            }

            // If Optimistic
            if (!writing) return version;
            return 0L;
        }
    }

    public Lock asReadLock() { return new ReadLockView(this); }
    public Lock asWriteLock() { return new WriteLockView(this); }
    public ReadWriteLock asReadWriteLock() { return new ReadWriteLockView(this); }

    static final class ReadLockView implements Lock {
        private final StampedLock lock;
        ReadLockView(StampedLock lock) { this.lock = lock; }
        public void lock() { lock.readLock(); }
        public void lockInterruptibly() throws InterruptedException { lock.readLockInterruptibly(); }
        public boolean tryLock() { return lock.tryReadLock() != 0L; }
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException { return lock.tryReadLock(time, unit) != 0L; }
        public void unlock() { lock.unlockReadNoStamp(); }
        public Condition newCondition() { throw new UnsupportedOperationException(); }
    }

    static final class WriteLockView implements Lock {
        private final StampedLock lock;
        WriteLockView(StampedLock lock) { this.lock = lock; }
        public void lock() { lock.writeLock(); }
        public void lockInterruptibly() throws InterruptedException { lock.writeLockInterruptibly(); }
        public boolean tryLock() { return lock.tryWriteLock() != 0L; }
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException { return lock.tryWriteLock(time, unit) != 0L; }
        public void unlock() { lock.unlockWriteNoStamp(); }
        public Condition newCondition() { throw new UnsupportedOperationException(); }
    }

    static final class ReadWriteLockView implements ReadWriteLock {
        private final StampedLock lock;
        ReadWriteLockView(StampedLock lock) { this.lock = lock; }
        public Lock readLock() { return lock.asReadLock(); }
        public Lock writeLock() { return lock.asWriteLock(); }
    }

    final void unlockReadNoStamp() {
         synchronized (sync) {
            if (readers > 0) {
                readers--;
                if (readers == 0) sync.notifyAll();
            } else {
                 throw new IllegalMonitorStateException();
            }
        }
    }

    final void unlockWriteNoStamp() {
        synchronized (sync) {
            if (writing) {
                writing = false;
                version += 2;
                if (version == 0) version = 256;
                sync.notifyAll();
            } else {
                throw new IllegalMonitorStateException();
            }
        }
    }

    public boolean isReadLocked() {
        synchronized (sync) {
            return readers > 0;
        }
    }

    public boolean isWriteLocked() {
        synchronized (sync) {
            return writing;
        }
    }

    public int getReadLockCount() {
         synchronized (sync) {
            return readers;
        }
    }

    public String toString() {
         synchronized (sync) {
            return super.toString() + (writing ? "[Write-locked]" : (readers > 0 ? "[Read-locks:" + readers + "]" : "[Unlocked]"));
        }
    }

    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        sync = new Object();
        version = 256;
        readers = 0;
        writing = false;
    }
}
