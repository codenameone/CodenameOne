/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package java.util.concurrent;

/// `java.util.concurrent.Semaphore`: a count of permits that threads take
/// and give back. A thread that asks for more permits than are free waits
/// through the package's `BlockingWait`, so the UI thread keeps dispatching
/// events while it waits. The fairness flag is accepted and reported, but
/// waiters are not served in arrival order.
public class Semaphore implements java.io.Serializable {

    private final Object lock = new Object();
    private final boolean fair;
    /// Guarded by `lock`.
    private int permits;

    public Semaphore(int permits) {
        this(permits, false);
    }

    public Semaphore(int permits, boolean fair) {
        this.permits = permits;
        this.fair = fair;
    }

    public boolean isFair() {
        return fair;
    }

    public void acquire() throws InterruptedException {
        acquire(1);
    }

    public void acquire(int n) throws InterruptedException {
        check(n);
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        waitFor(n, BlockingWait.FOREVER);
    }

    public void acquireUninterruptibly() {
        acquireUninterruptibly(1);
    }

    public void acquireUninterruptibly(int n) {
        check(n);
        boolean interrupted = false;
        while (true) {
            try {
                waitFor(n, BlockingWait.FOREVER);
                break;
            } catch (InterruptedException e) {
                interrupted = true;
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    public boolean tryAcquire(int n) {
        check(n);
        synchronized (lock) {
            if (permits >= n) {
                permits -= n;
                return true;
            }
            return false;
        }
    }

    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        return tryAcquire(1, timeout, unit);
    }

    public boolean tryAcquire(int n, long timeout, TimeUnit unit) throws InterruptedException {
        check(n);
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        return waitFor(n, BlockingWait.deadline(BlockingWait.millis(timeout, unit)));
    }

    public void release() {
        release(1);
    }

    public void release(int n) {
        check(n);
        synchronized (lock) {
            permits += n;
            lock.notifyAll();
        }
    }

    public int availablePermits() {
        synchronized (lock) {
            return permits;
        }
    }

    public int drainPermits() {
        synchronized (lock) {
            int p = permits;
            if (p > 0) {
                permits = 0;
                return p;
            }
            return 0;
        }
    }

    protected void reducePermits(int reduction) {
        check(reduction);
        synchronized (lock) {
            permits -= reduction;
        }
    }

    @Override
    public String toString() {
        return super.toString() + "[Permits = " + availablePermits() + "]";
    }

    private static void check(int n) {
        if (n < 0) {
            throw new IllegalArgumentException();
        }
    }

    /// Takes `n` permits, waiting until `deadline`; false when it passed.
    private boolean waitFor(final int n, long deadline) throws InterruptedException {
        while (true) {
            synchronized (lock) {
                if (permits >= n) {
                    permits -= n;
                    return true;
                }
            }
            if (deadline != BlockingWait.FOREVER && deadline - BlockingWait.now() <= 0) {
                return false;
            }
            // Another thread may take the permits between the wait ending
            // and the lock being taken again, hence the loop.
            new BlockingWait(lock, deadline) {
                @Override
                boolean ready() {
                    return permits >= n;
                }
            }.await();
        }
    }
}
