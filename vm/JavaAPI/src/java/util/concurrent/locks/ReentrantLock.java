package java.util.concurrent.locks;

import java.util.concurrent.TimeUnit;
import java.util.Collection;
import java.util.Date;

public class ReentrantLock implements Lock, java.io.Serializable {
    private static final long serialVersionUID = 7373984872572414699L;
    private transient Object sync = new Object();
    private transient Thread owner;
    private transient int holdCount;

    public ReentrantLock() {}

    public ReentrantLock(boolean fair) {
        // Fairness is not supported in this implementation
    }

    public void lock() {
        synchronized (sync) {
            Thread current = Thread.currentThread();
            if (owner == current) {
                holdCount++;
                return;
            }
            boolean interrupted = false;
            while (owner != null) {
                try {
                    sync.wait();
                } catch (InterruptedException e) {
                     interrupted = true;
                }
            }
            owner = current;
            holdCount = 1;
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
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
        Node next;
    }

    private class ConditionObject implements Condition {
        final ReentrantLock lock = ReentrantLock.this;
        private Node head;
        private Node tail;
        private int count;

        private void add(Node node) {
            if (head == null) {
                head = tail = node;
            } else {
                tail.next = node;
                tail = node;
            }
            count++;
        }

        private void remove(Node node) {
            if (head == null) return;
            if (head == node) {
                head = head.next;
                if (head == null) tail = null;
                count--;
                return;
            }
            Node prev = head;
            while (prev.next != null) {
                if (prev.next == node) {
                    prev.next = node.next;
                    if (prev.next == null) tail = prev;
                    count--;
                    return;
                }
                prev = prev.next;
            }
        }

        public void await() throws InterruptedException {
            if (Thread.interrupted()) throw new InterruptedException();
            Node node = new Node();
            int savedHoldCount = 0;
            synchronized (sync) {
                if (owner != Thread.currentThread()) throw new IllegalMonitorStateException();
                savedHoldCount = holdCount;
                holdCount = 0;
                owner = null;
                add(node);
                sync.notify();
            }

            synchronized (node) {
                 while (!node.signalled) {
                     try {
                         node.wait();
                     } catch (InterruptedException e) {
                         synchronized(sync) {
                             remove(node);
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
                add(node);
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
                 return 0;
             }

             Node node = new Node();
             int savedHoldCount = 0;
             synchronized (sync) {
                 if (owner != Thread.currentThread()) throw new IllegalMonitorStateException();
                 savedHoldCount = holdCount;
                 holdCount = 0;
                 owner = null;
                 add(node);
                 sync.notify();
             }

             long timeLeft = nanosTimeout;

             synchronized (node) {
                 try {
                      if (!node.signalled) {
                          node.wait(timeoutMillis, timeoutNanos);
                      }
                 } catch (InterruptedException e) {
                      synchronized(sync) { remove(node); }
                      throw e;
                 }

                 synchronized(sync) {
                     if (!node.signalled) {
                         remove(node);
                         timeLeft = 0;
                     } else {
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
                if (head != null) {
                    Node node = head;
                    head = head.next;
                    if (head == null) tail = null;
                    count--;

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
                while (head != null) {
                    Node node = head;
                    head = head.next;
                    synchronized (node) {
                        node.signalled = true;
                        node.notify();
                    }
                }
                tail = null;
                count = 0;
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
                return head != null;
            }
        }

        protected int getWaitQueueLength() {
             synchronized(sync) {
                if (owner != Thread.currentThread()) throw new IllegalMonitorStateException();
                return count;
            }
        }

        protected Collection<Thread> getWaitingThreads() {
            throw new RuntimeException("Not implemented");
        }
    }
}
