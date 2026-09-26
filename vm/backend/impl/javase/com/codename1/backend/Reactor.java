/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Java SE twin of the epoll/kqueue reactor, over a Selector.
 *
 * Two things about NIO that the native side gets for free and this has to work
 * around, both of them thread-related:
 *
 * - register() from a thread other than the one inside select() blocks until the
 *   select returns. Workers hand descriptors back after finishing a request, so
 *   registrations are queued and applied by the selecting thread instead.
 * - cancel() is lazy: the key survives until the next select, and a channel with a
 *   live key throws IllegalBlockingModeException when a worker flips it to
 *   blocking. remove() therefore flushes with selectNow(); it is only ever called
 *   from the selecting thread (the reactor loop) or for the listener during stop.
 */
public final class Reactor {
    public static final int READ = 1;
    public static final int WRITE = 2;
    /**
     * Deliver an event for this descriptor ONCE and then disarm it, until
     * {@link #modify} re-arms it.
     *
     * This is what lets the worker threads poll the same set directly rather
     * than a reactor thread dispatching to them: the kernel guarantees exactly
     * one waiter is handed a given descriptor, so two workers cannot land on one
     * connection. Without it a level-triggered set reports the same descriptor
     * ready to every waiter at once.
     */
    public static final int ONESHOT = 4;

    private final Selector selector;
    private final Map<Integer, SelectionKey> keys = new ConcurrentHashMap<Integer, SelectionKey>();
    private final Deque<int[]> pending = new ArrayDeque<int[]>();

    private Reactor(Selector selector) {
        this.selector = selector;
    }

    /**
     * Null: this runtime has no virtual-thread hosts to wake. See the packaged
     * runtime's Reactor.
     */
    public static int[] createWakePipe() {
        return null;
    }

    public static void wake(int writeFd) {
    }

    public static void drainWake(int readFd) {
    }

    public static Reactor create() throws IOException {
        return new Reactor(Selector.open());
    }

    /** Descriptors registered with {@link #ONESHOT}, so await() knows to disarm them. */
    private final java.util.Set oneshot =
            java.util.Collections.synchronizedSet(new java.util.HashSet());

    public void add(int fd, int events) throws IOException {
        rememberOneshot(Integer.valueOf(fd), events);
        synchronized (pending) {
            pending.add(new int[] {fd, events});
        }
        selector.wakeup();
    }

    /**
     * Records whether this descriptor is armed one-shot, which is a property of
     * the LAST call that set its interest -- add() or modify() alike.
     */
    private void rememberOneshot(Integer key, int events) {
        if((events & ONESHOT) != 0) {
            oneshot.add(key);
        } else {
            oneshot.remove(key);
        }
    }

    public void modify(int fd, int events) throws IOException {
        // THE SET, NOT ONLY THE INTEREST OPS. This updated what the selector waits
        // for and left the one-shot bookkeeping at whatever add() had last said, so
        // the two halves of a registration disagreed in both directions: adding a
        // descriptor level-triggered and modifying it to ONESHOT left it
        // level-triggered, and dropping ONESHOT by modify left await() still
        // disarming it after one delivery. Both translated arms take the flag from
        // every change they make -- EPOLL_CTL_MOD carries EPOLLONESHOT, and a
        // kevent re-add carries EV_DISPATCH -- so this was the Java SE arm alone
        // answering a documented call differently.
        rememberOneshot(Integer.valueOf(fd), events);
        SelectionKey key = keys.get(Integer.valueOf(fd));
        if(key == null) {
            add(fd, events);
            return;
        }
        try {
            key.interestOps(toOps(events, key.channel()));
        } catch (CancelledKeyException err) {
            add(fd, events);
        }
        selector.wakeup();
    }

    public void remove(int fd) {
        oneshot.remove(Integer.valueOf(fd));
        SelectionKey key = keys.remove(Integer.valueOf(fd));
        if(key == null) {
            return;
        }
        key.cancel();
        try {
            // Flush the cancellation now, so the worker about to take this
            // descriptor can put it back into blocking mode.
            selector.selectNow();
        } catch (IOException ignored) {
            // A failed flush leaves the key for the next select to clear.
        }
    }

    public int await(int[] readyFds, int timeoutMillis) throws IOException {
        applyPending();
        // ZERO IS A PROBE HERE TOO. The translated arms are epoll_wait(..., 0)
        // and kevent with a zero timespec, both of which return at once and
        // report whatever is ready now; Selector's zero is select(), which waits.
        // Mapping a NEGATIVE timeout to 0 was right for the same reason and by
        // the same accident -- epoll_wait(-1) waits without a deadline and
        // select() is how that is spelled -- so it is now spelled deliberately.
        //
        // The sibling in ServerSocket.awaitReadable had this and was fixed one
        // finding earlier; that fix should have been a sweep of every select()
        // in this arm rather than the one site reported. Deadlines' two are the
        // third case and are CORRECT as they stand: there a zero timeout is a
        // socket deadline of zero, which means no deadline on the translated arm
        // too (SO_RCVTIMEO of zero blocks), so select(0) already agrees.
        if(timeoutMillis == 0) {
            selector.selectNow();
        } else if(timeoutMillis < 0) {
            selector.select();
        } else {
            selector.select(timeoutMillis);
        }
        int count = 0;
        Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
        while(iterator.hasNext()) {
            SelectionKey key = iterator.next();
            iterator.remove();
            if(count >= readyFds.length) {
                break;
            }
            Object attachment = key.attachment();
            if(attachment instanceof Integer) {
                // ONESHOT emulation: NIO has no equivalent, so clear the interest
                // set the way epoll disarms a one-shot descriptor. modify()
                // re-arms it. Without this the flag would be silently inert here
                // and two threads polling one selector would both be handed the
                // same connection -- the exact hazard ONESHOT exists to remove.
                if(oneshot.contains(attachment)) {
                    key.interestOps(0);
                }
                readyFds[count++] = ((Integer)attachment).intValue();
            }
        }
        return count;
    }

    public void close() {
        try {
            selector.close();
        } catch (IOException ignored) {
            // already gone
        }
        keys.clear();
    }

    private void applyPending() {
        while(true) {
            int[] entry;
            synchronized (pending) {
                entry = pending.poll();
            }
            if(entry == null) {
                return;
            }
            Object channel = Descriptors.get(entry[0]);
            if(!(channel instanceof SelectableChannel)) {
                continue;
            }
            SelectableChannel selectable = (SelectableChannel)channel;
            try {
                // NON-BLOCKING FIRST, because register() refuses a blocking
                // channel -- IllegalBlockingModeException -- and the catch below
                // would then drop the registration silently: add() would appear to
                // work and await() would never report the descriptor. poll() and
                // kevent on the translated arms do not care what mode a descriptor
                // is in, so a caller doing exactly what the API documents got an
                // answer on the device and silence here. Selecting on a channel is
                // what this class is for, so the mode it requires is this class's
                // to set.
                selectable.configureBlocking(false);
                SelectionKey key = selectable.register(selector,
                        toOps(entry[1], selectable), Integer.valueOf(entry[0]));
                keys.put(Integer.valueOf(entry[0]), key);
            } catch (Exception err) {
                // A closed or already-cancelled channel simply does not come back.
                keys.remove(Integer.valueOf(entry[0]));
            }
        }
    }

    private static int toOps(int events, SelectableChannel channel) {
        int ops = 0;
        if((events & READ) != 0) {
            // A server socket reports readiness to accept, not to read; the shared
            // code above says READ for both, as poll does.
            ops |= (channel.validOps() & SelectionKey.OP_ACCEPT) != 0
                    ? SelectionKey.OP_ACCEPT : SelectionKey.OP_READ;
        }
        if((events & WRITE) != 0 && (channel.validOps() & SelectionKey.OP_WRITE) != 0) {
            ops |= SelectionKey.OP_WRITE;
        }
        return ops;
    }
}
