package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;
import java.util.Collection;
import java.util.Date;
import java.util.ArrayList;

public class ReentrantLock implements Lock, java.io.Serializable {
    private static final long serialVersionUID = 7373984872572414699L;
    private transient Object sync = new Object();
    private transient Thread owner;
    private transient int holdCount;

    public ReentrantLock() {}

    public ReentrantLock(boolean fair) {
        // Fairness is not supported in this implementation
    }

    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        sync = new Object();
        // holdCount and owner are transient, initialized to 0/null by default which is correct (unlocked state)
    }

    public void lock() {
        synchronized (sync) {
            Thread current = Thread.currentThread();
            if (owner == current) {
                holdCount++;
                return;
            }
            while (owner != null) {
                try {
                    sync.wait();
                } catch (InterruptedException e) {
                     Thread.currentThread().interrupt();
                }
            }
            owner = current;
            holdCount = 1;
        }
    }

    public void lockInterruptibly() throws InterruptedException {
        synchronized (sync) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            Thread current = Thread.currentThread();
            if (owner == current) {
                holdCount++;
                return;
            }
            while (owner != null) {
                sync.wait();
            }
            owner = current;
            holdCount = 1;
        }
    }

    public boolean tryLock() {
        synchronized (sync) {
            Thread current = Thread.currentThread();
            if (owner == current) {
                holdCount++;
                return true;
            }
            if (owner == null) {
                owner = current;
                holdCount = 1;
                return true;
            }
            return false;
        }
    }

    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        long end = System.currentTimeMillis() + unit.toMillis(timeout);

        synchronized (sync) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            Thread current = Thread.currentThread();
            if (owner == current) {
                holdCount++;
                return true;
            }
            if (owner == null) {
                owner = current;
                holdCount = 1;
                return true;
            }

            long remaining = unit.toMillis(timeout);
            while (remaining > 0 && owner != null) {
                sync.wait(remaining);
                if (owner == null) {
                    owner = current;
                    holdCount = 1;
                    return true;
                }
                if (owner == current) {
                     holdCount++;
                     return true;
                }
                remaining = end - System.currentTimeMillis();
            }
            return false;
        }
    }

    public void unlock() {
        synchronized (sync) {
            if (Thread.currentThread() != owner) {
                throw new IllegalMonitorStateException();
            }
            holdCount--;
            if (holdCount == 0) {
                owner = null;
                sync.notify();
            }
        }
    }

    public Condition newCondition() {
        return new ConditionObject();
    }

    public int getHoldCount() {
        synchronized (sync) {
            return (Thread.currentThread() == owner) ? holdCount : 0;
        }
    }

    public boolean isHeldByCurrentThread() {
        synchronized (sync) {
            return Thread.currentThread() == owner;
        }
    }

    public boolean isLocked() {
        synchronized (sync) {
            return owner != null;
        }
    }

    public final boolean isFair() {
        return false;
    }

    protected Thread getOwner() {
        synchronized (sync) {
            return owner;
        }
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

    public boolean hasWaiters(Condition condition) {
        if (condition == null) throw new NullPointerException();
        if (!(condition instanceof ConditionObject) || ((ConditionObject)condition).lock != this) throw new IllegalArgumentException("not owner");
        return ((ConditionObject)condition).hasWaiters();
    }

    public int getWaitQueueLength(Condition condition) {
        if (condition == null) throw new NullPointerException();
        if (!(condition instanceof ConditionObject) || ((ConditionObject)condition).lock != this) throw new IllegalArgumentException("not owner");
        return ((ConditionObject)condition).getWaitQueueLength();
    }

    protected Collection<Thread> getWaitingThreads(Condition condition) {
        if (condition == null) throw new NullPointerException();
        if (!(condition instanceof ConditionObject) || ((ConditionObject)condition).lock != this) throw new IllegalArgumentException("not owner");
        return ((ConditionObject)condition).getWaitingThreads();
    }

    private static class Node {
        boolean signalled = false;
    }

    private class ConditionObject implements Condition {
        final ReentrantLock lock = ReentrantLock.this;
        private ArrayList<Node> waitingNodes = new ArrayList<Node>();

        public void await() throws InterruptedException {
            if (Thread.interrupted()) throw new InterruptedException();
            Node node = new Node();
            int savedHoldCount = 0;
            synchronized (sync) {
                if (owner != Thread.currentThread()) throw new IllegalMonitorStateException();
                savedHoldCount = holdCount;
                holdCount = 0;
                owner = null;
                waitingNodes.add(node);
                sync.notify();
            }

            synchronized (node) {
                 while (!node.signalled) {
                     try {
                         node.wait();
                     } catch (InterruptedException e) {
                         synchronized(sync) {
                             waitingNodes.remove(node);
                         }
                         throw e;
                     }
                 }
            }

            reacquire(savedHoldCount);
        }

        public void awaitUninterruptibly() {
            Node node = new Node();
            int savedHoldCount = 0;
            synchronized (sync) {
                if (owner != Thread.currentThread()) throw new IllegalMonitorStateException();
                savedHoldCount = holdCount;
                holdCount = 0;
                owner = null;
                waitingNodes.add(node);
                sync.notify();
            }

            synchronized (node) {
                 while (!node.signalled) {
                     try {
                         node.wait();
                     } catch (InterruptedException e) {
                         Thread.currentThread().interrupt();
                     }
                 }
            }
            reacquire(savedHoldCount);
        }

        public long awaitNanos(long nanosTimeout) throws InterruptedException {
             if (Thread.interrupted()) throw new InterruptedException();
             if (nanosTimeout <= 0) return nanosTimeout;
             long start = System.currentTimeMillis();
             long timeoutMillis = nanosTimeout / 1000000;
             int timeoutNanos = (int)(nanosTimeout % 1000000);

             if (timeoutMillis == 0 && timeoutNanos == 0) {
                 // Should be covered by nanosTimeout <= 0 check, but safe guard
                 return 0;
             }

             Node node = new Node();
             int savedHoldCount = 0;
             synchronized (sync) {
                 if (owner != Thread.currentThread()) throw new IllegalMonitorStateException();
                 savedHoldCount = holdCount;
                 holdCount = 0;
                 owner = null;
                 waitingNodes.add(node);
                 sync.notify();
             }

             long timeLeft = nanosTimeout;

             synchronized (node) {
                 try {
                      if (!node.signalled) {
                          node.wait(timeoutMillis, timeoutNanos);
                      }
                 } catch (InterruptedException e) {
                      synchronized(sync) { waitingNodes.remove(node); }
                      throw e;
                 }

                 synchronized(sync) {
                     if (waitingNodes.contains(node)) {
                         // Still in queue -> timed out without signal
                         waitingNodes.remove(node);
                         timeLeft = 0;
                     } else {
                         // Removed from queue -> Signalled
                         // Or we removed it ourselves? No, we only remove if timeout.
                         // But wait! If we timed out, we check if we were signalled concurrently.
                         // Signal() removes from queue then sets signalled=true then notify.
                         // If signal happened, node is not in waitingNodes.
                         long elapsed = System.currentTimeMillis() - start;
                         timeLeft = nanosTimeout - (elapsed * 1000000);
                     }
                 }
             }

             reacquire(savedHoldCount);
             return timeLeft;
        }

        public boolean await(long time, TimeUnit unit) throws InterruptedException {
            return awaitNanos(unit.toNanos(time)) > 0;
        }

        public boolean awaitUntil(Date deadline) throws InterruptedException {
             long dist = deadline.getTime() - System.currentTimeMillis();
             if (dist <= 0) return false;
             return awaitNanos(dist * 1000000) > 0;
        }

        public void signal() {
            synchronized (sync) {
                if (owner != Thread.currentThread()) throw new IllegalMonitorStateException();
                if (!waitingNodes.isEmpty()) {
                    Node node = waitingNodes.remove(0);
                    synchronized (node) {
                        node.signalled = true;
                        node.notify();
                    }
                }
            }
        }

        public void signalAll() {
            synchronized (sync) {
                if (owner != Thread.currentThread()) throw new IllegalMonitorStateException();
                for (Node node : waitingNodes) {
                    synchronized (node) {
                        node.signalled = true;
                        node.notify();
                    }
                }
                waitingNodes.clear();
            }
        }

        private void reacquire(int savedHoldCount) {
             synchronized (sync) {
                 while (owner != null) {
                      try {
                          sync.wait();
                      } catch (InterruptedException e) {
                          Thread.currentThread().interrupt();
                      }
                 }
                 owner = Thread.currentThread();
                 holdCount = savedHoldCount;
             }
        }

        protected boolean hasWaiters() {
            synchronized(sync) {
                if (owner != Thread.currentThread()) throw new IllegalMonitorStateException();
                return !waitingNodes.isEmpty();
            }
        }

        protected int getWaitQueueLength() {
             synchronized(sync) {
                if (owner != Thread.currentThread()) throw new IllegalMonitorStateException();
                return waitingNodes.size();
            }
        }

        protected Collection<Thread> getWaitingThreads() {
            throw new RuntimeException("Not implemented");
        }
    }
}
