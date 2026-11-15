/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.brickyard.play;

import com.codename1.rbx.Engine;
import com.codename1.rbx.ReplicaServer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/// One running server of an experience: an authoritative SERVER engine and
/// the [ReplicaServer] that replicates it to every seated player.
///
/// Every touch of the engine happens on the room's own scheduled thread.
/// Socket input never calls in directly; it is queued and drained at the
/// start of the next frame, so the engine needs no locks.
public final class PlayRoom {
    /// Frames per second the server simulates and replicates at.
    public static final int HZ = 60;
    /// A room nobody has connected to for this long closes.
    static final long IDLE_MILLIS = 30000;

    /// Where the room sends its updates; a socket in production, a queue in tests.
    public interface Link extends ReplicaServer.Link {
    }

    /// Told when the room closes, so it can be dropped from the directory.
    interface Closed {
        void closed(PlayRoom room);
    }

    private final String id;
    private final String experienceId;
    private final int maxPlayers;
    private final boolean matchmade;
    private final ReplicaServer server;
    private final ConcurrentLinkedQueue<Runnable> inbox = new ConcurrentLinkedQueue<Runnable>();
    /// Usernames holding a seat: reserved by a ticket, or connected.
    private final Map<String, Long> seats = new ConcurrentHashMap<String, Long>();
    /// Room thread only: the connected links and their replica handles.
    private final Map<Link, Integer> handles = new HashMap<Link, Integer>();
    private final Map<Link, String> owners = new HashMap<Link, String>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private ScheduledExecutorService thread;
    private Closed onClosed;
    private long lastActive = System.currentTimeMillis();

    PlayRoom(String id, String experienceId, int maxPlayers, boolean matchmade, Engine engine) {
        this.id = id;
        this.experienceId = experienceId;
        this.maxPlayers = maxPlayers;
        this.matchmade = matchmade;
        this.server = new ReplicaServer(engine);
    }

    public String getId() {
        return id;
    }

    public String getExperienceId() {
        return experienceId;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    boolean isMatchmade() {
        return matchmade;
    }

    public boolean isClosed() {
        return closed.get();
    }

    /// Seats taken, counting tickets that have not connected yet.
    public int getSeated() {
        return seats.size();
    }

    /// Takes a seat for `username`; false when the room is full or closed.
    synchronized boolean reserve(String username) {
        if (closed.get()) {
            return false;
        }
        if (seats.containsKey(username)) {
            return true;
        }
        if (seats.size() >= maxPlayers) {
            return false;
        }
        seats.put(username, Long.valueOf(System.currentTimeMillis()));
        return true;
    }

    /// Starts ticking at [#HZ] on a thread of its own.
    void start(Closed whenClosed) {
        onClosed = whenClosed;
        thread = Executors.newScheduledThreadPool(1);
        thread.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    frame(System.currentTimeMillis());
                } catch (RuntimeException ex) {
                    // A script error must not kill the room's thread; the
                    // engine reports script errors through its own output.
                    ex.printStackTrace();
                }
            }
        }, 0, 1000 / HZ, TimeUnit.MILLISECONDS);
    }

    /// Queues `username` joining over `link`; any thread.
    public void connect(final Link link, final long userId, final String username, final String display) {
        inbox.add(new Runnable() {
            @Override
            public void run() {
                handles.put(link, Integer.valueOf(server.connect(link, userId, username, display)));
                owners.put(link, username);
            }
        });
    }

    /// Queues a message from `link`; any thread.
    public void receive(final Link link, final byte[] message) {
        inbox.add(new Runnable() {
            @Override
            public void run() {
                Integer h = handles.get(link);
                if (h != null) {
                    server.receive(h.intValue(), message);
                }
            }
        });
    }

    /// Queues `link` leaving; any thread.
    public void disconnect(final Link link) {
        inbox.add(new Runnable() {
            @Override
            public void run() {
                Integer h = handles.remove(link);
                String who = owners.remove(link);
                if (h != null) {
                    server.disconnect(h.intValue());
                }
                if (who != null && !owners.containsValue(who)) {
                    seats.remove(who);
                }
            }
        });
    }

    /// One frame: input, simulation, replication. Room thread only (tests
    /// that never [#start] drive it from their own single thread).
    void frame(long now) {
        Runnable r;
        while ((r = inbox.poll()) != null) {
            r.run();
        }
        server.getEngine().step(1.0 / HZ);
        server.flush();
        if (!handles.isEmpty()) {
            lastActive = now;
        } else {
            expireReservations(now);
            if (now - lastActive > IDLE_MILLIS && seats.isEmpty()) {
                close();
            }
        }
    }

    private void expireReservations(long now) {
        for (Map.Entry<String, Long> e : seats.entrySet()) {
            if (!owners.containsValue(e.getKey()) && now - e.getValue().longValue() > IDLE_MILLIS) {
                seats.remove(e.getKey());
            }
        }
        if (seats.isEmpty() && now - lastActive <= IDLE_MILLIS && handles.isEmpty()) {
            // Everyone left: close promptly rather than idling the full window.
            lastActive = Math.min(lastActive, now - IDLE_MILLIS + 2000);
        }
    }

    /// Stops the thread and drops the room; idempotent.
    void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        if (thread != null) {
            thread.shutdown();
        }
        if (onClosed != null) {
            onClosed.closed(this);
        }
    }

    /// Room thread only.
    ReplicaServer getServer() {
        return server;
    }
}
