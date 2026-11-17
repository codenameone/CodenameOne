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

import com.codename1.backend.log.Logger;
import com.codenameone.examples.brickyard.rbx.Engine;
import com.codenameone.examples.brickyard.rbx.Instance;
import com.codenameone.examples.brickyard.rbx.ReplicaServer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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

    /// Told when the room closes, so it can be dropped from the directory,
    /// and when a player joins or leaves, for presence and analytics. Called
    /// on the room's thread.
    interface Listener {
        void closed(PlayRoom room);

        void joined(PlayRoom room, String username);

        /// `millis` connected, or -1 for a seat that never connected.
        void left(PlayRoom room, String username, long millis);

        /// A leaderstat of `username` moved by `delta` since the last report.
        /// The server reads leaderstats; a client never writes a score.
        void scored(PlayRoom room, String username, String stat, long delta);
    }

    /// How often leaderstats are read for the leaderboards.
    static final long SCORE_MILLIS = 2000;

    private static final Logger LOG = Logger.getLogger(PlayRoom.class);
    /// Frames one link may have waiting for the room thread. The socket
    /// thread fills the inbox faster than a tick drains it if a client
    /// floods; past this the link is dropped rather than queued without end.
    static final int MAX_QUEUED_FRAMES = 256;
    /// Bytes one link may have waiting, for the same reason.
    static final int MAX_QUEUED_BYTES = 1 << 20;

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
    /// Room thread only: when each connected username first connected.
    private final Map<String, Long> since = new HashMap<String, Long>();
    /// Any thread: frames and bytes each link has queued and not yet applied.
    private final Map<Link, AtomicInteger[]> backlog = new ConcurrentHashMap<Link, AtomicInteger[]>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private ScheduledExecutorService thread;
    private Listener listener;
    private long lastActive = System.currentTimeMillis();
    /// Room thread only: the leaderstat values last reported, per username.
    private final Map<String, Map<String, Long>> reported = new HashMap<String, Map<String, Long>>();
    private long lastScores;

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

    /// Sets who hears about joins, leaves and scores; [#start] does this, and
    /// tests that drive [#frame] themselves call it directly.
    void listen(Listener l) {
        listener = l;
    }

    /// Starts ticking at [#HZ] on a thread of its own.
    void start(Listener l) {
        listen(l);
        thread = Executors.newScheduledThreadPool(1);
        thread.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    frame(System.currentTimeMillis());
                } catch (Throwable ex) { // NOPMD AvoidCatchingThrowable - an escaped Error silently cancels the schedule
                    // Anything thrown out of here makes scheduleAtFixedRate
                    // cancel the room for good, without a word. A link's
                    // input is already guarded in frame; this is the last line.
                    LOG.error("Room {} frame failed", id, ex);
                }
            }
        }, 0, 1000 / HZ, TimeUnit.MILLISECONDS);
    }

    /// Queues `username` joining over `link`; any thread.
    public void connect(final Link link, final long userId, final String username, final String display) {
        connect(link, userId, username, display, null);
    }

    /// [#connect(Link, long, String, String)] wearing `body`, the account's
    /// chosen avatar body (null for the default figure).
    public void connect(final Link link, final long userId, final String username, final String display,
            final String body) {
        inbox.add(new Runnable() {
            @Override
            public void run() {
                Map<String, Object> attrs = null;
                if (body != null) {
                    attrs = new HashMap<String, Object>();
                    attrs.put("AvatarBody", body);
                }
                handles.put(link, Integer.valueOf(server.connect(link, userId, display, display, attrs)));
                owners.put(link, username);
                if (!since.containsKey(username)) {
                    since.put(username, Long.valueOf(System.currentTimeMillis()));
                    if (listener != null) {
                        listener.joined(PlayRoom.this, username);
                    }
                }
            }
        });
    }

    /// Queues a message from `link`; any thread.
    public void receive(final Link link, final byte[] message) {
        AtomicInteger[] b = backlog.get(link);
        if (b == null) {
            AtomicInteger[] fresh = {new AtomicInteger(), new AtomicInteger()};
            b = backlog.putIfAbsent(link, fresh);
            if (b == null) {
                b = fresh;
            }
        }
        final AtomicInteger[] counts = b;
        int frames = counts[0].incrementAndGet();
        int bytes = counts[1].addAndGet(message.length);
        if (frames > MAX_QUEUED_FRAMES || bytes > MAX_QUEUED_BYTES) {
            counts[0].decrementAndGet();
            counts[1].addAndGet(-message.length);
            if (backlog.remove(link, counts)) {
                LOG.warn("Room {} dropped a link that flooded its inbox", id);
                disconnect(link);
            }
            return;
        }
        inbox.add(new Runnable() {
            @Override
            public void run() {
                counts[0].decrementAndGet();
                counts[1].addAndGet(-message.length);
                Integer h = handles.get(link);
                if (h == null) {
                    return;
                }
                try {
                    server.receive(h.intValue(), message);
                } catch (Throwable bad) { // NOPMD AvoidCatchingThrowable - one link's input must never end the room
                    // A message that escapes ReplicaServer's own guards (an
                    // Error such as a stack overflow) costs that link its
                    // seat, never the room everyone else is in.
                    LOG.warn("Room {} dropped a link whose message failed", id, bad);
                    disconnect(link);
                }
            }
        });
    }

    /// Queues `link` leaving; any thread.
    public void disconnect(final Link link) {
        inbox.add(new Runnable() {
            @Override
            public void run() {
                backlog.remove(link);
                Integer h = handles.remove(link);
                String who = owners.remove(link);
                if (h != null) {
                    server.disconnect(h.intValue());
                }
                if (who != null && !owners.containsValue(who)) {
                    seats.remove(who);
                    reported.remove(who);
                    Long from = since.remove(who);
                    if (listener != null) {
                        listener.left(PlayRoom.this, who,
                                from == null ? -1 : System.currentTimeMillis() - from.longValue());
                    }
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
        if (now - lastScores >= SCORE_MILLIS) {
            lastScores = now;
            reportScores();
        }
        if (!handles.isEmpty()) {
            lastActive = now;
        } else {
            expireReservations(now);
            if (now - lastActive > IDLE_MILLIS && seats.isEmpty()) {
                close();
            }
        }
    }

    /// Sends each numeric leaderstat's change since the last report. The first
    /// reading of a stat is its baseline, so a value restored from a data
    /// store is not counted again.
    private void reportScores() {
        if (listener == null) {
            return;
        }
        for (Map.Entry<Link, Integer> h : handles.entrySet()) {
            String who = owners.get(h.getKey());
            Instance player = server.player(h.getValue().intValue());
            Instance stats = player == null ? null : player.findFirstChild("leaderstats");
            if (who == null || stats == null) {
                continue;
            }
            Map<String, Long> last = reported.get(who);
            if (last == null) {
                last = new HashMap<String, Long>();
                reported.put(who, last);
            }
            for (Instance v : stats.getChildren()) {
                Object value = v.prop("Value");
                if (!(value instanceof Number)) {
                    continue;
                }
                long now = ((Number) value).longValue();
                Long before = last.put(v.getName(), Long.valueOf(now));
                if (before != null && now > before.longValue()) {
                    listener.scored(this, who, v.getName(), now - before.longValue());
                }
            }
        }
    }

    private void expireReservations(long now) {
        for (Map.Entry<String, Long> e : seats.entrySet()) {
            if (!owners.containsValue(e.getKey()) && now - e.getValue().longValue() > IDLE_MILLIS) {
                seats.remove(e.getKey());
                if (listener != null) {
                    listener.left(this, e.getKey(), -1);
                }
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
        if (listener != null) {
            listener.closed(this);
        }
    }

    /// Room thread only.
    ReplicaServer getServer() {
        return server;
    }
}
