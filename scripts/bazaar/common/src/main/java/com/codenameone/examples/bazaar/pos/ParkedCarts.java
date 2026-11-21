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

package com.codenameone.examples.bazaar.pos;

import com.codenameone.examples.bazaar.api.ParkedCartDto;
import java.util.ArrayList;
import java.util.List;

/// Carts put on hold at this register, kept on the device so holding and
/// resuming work offline.
///
/// Every hold is named by a `clientId` the register makes, so the server
/// keeps a hold sent twice once. What the register did while offline is
/// replayed in order: the hold first, then its resume or discard. A cart is
/// never deleted on either side, only moved to RESUMED or DISCARDED, so a
/// hold replayed late cannot bring back a cart that was already rung up.
/// Pure logic over a [Outbox.Persist]; the network half lives in the caller.
public final class ParkedCarts {
    public static final String PARKED = "PARKED";
    public static final String RESUMED = "RESUMED";
    public static final String DISCARDED = "DISCARDED";
    static final String KEY = "pos.parked";
    /// Finished holds kept after the server has them, newest last.
    static final int KEEP_DONE = 50;

    /// Turns a hold into one line of text and back.
    public interface Codec {
        String encode(ParkedCartDto cart);

        ParkedCartDto decode(String text);
    }

    /// One hold and what the server has been told of it.
    public static final class Entry {
        public final ParkedCartDto cart;
        public String status = PARKED;
        /// The server has the hold.
        public boolean parkSent;
        /// The server has its RESUMED or DISCARDED.
        public boolean moveSent;
        public String serverId = "";

        Entry(ParkedCartDto cart) {
            this.cart = cart;
        }

        public String clientId() {
            return cart.clientId();
        }

        /// What still has to be sent: "PARK", "RESUME", "DISCARD" or null.
        public String due() {
            if (!parkSent) {
                return "PARK";
            }
            if (!PARKED.equals(status) && !moveSent) {
                return RESUMED.equals(status) ? "RESUME" : "DISCARD";
            }
            return null;
        }
    }

    private final Outbox.Persist persist;
    private final Codec codec;
    private final List<Entry> entries = new ArrayList<Entry>();

    public ParkedCarts(Outbox.Persist persist, Codec codec) {
        this.persist = persist;
        this.codec = codec;
        load();
    }

    public Entry find(String clientId) {
        for (int iter = 0; clientId != null && iter < entries.size(); iter++) {
            if (entries.get(iter).clientId().equals(clientId)) {
                return entries.get(iter);
            }
        }
        return null;
    }

    /// Puts a cart on hold. The same `clientId` again is the same hold.
    public Entry park(ParkedCartDto cart) {
        Entry e = find(cart.clientId());
        if (e != null) {
            return e;
        }
        e = new Entry(cart);
        entries.add(e);
        save();
        return e;
    }

    /// Takes a hold back into the cart; null when it is not on hold any
    /// more (resumed or discarded, here or at another register).
    public ParkedCartDto resume(String clientId) {
        return move(clientId, RESUMED);
    }

    public ParkedCartDto discard(String clientId) {
        return move(clientId, DISCARDED);
    }

    private ParkedCartDto move(String clientId, String to) {
        Entry e = find(clientId);
        if (e == null || !PARKED.equals(e.status)) {
            return null;
        }
        e.status = to;
        e.moveSent = false;
        save();
        return e.cart;
    }

    /// The carts on hold, oldest first.
    public List<Entry> held() {
        List<Entry> out = new ArrayList<Entry>();
        for (int iter = 0; iter < entries.size(); iter++) {
            if (PARKED.equals(entries.get(iter).status)) {
                out.add(entries.get(iter));
            }
        }
        return out;
    }

    /// The holds with something to send, in the order it happened.
    public List<Entry> due() {
        List<Entry> out = new ArrayList<Entry>();
        for (int iter = 0; iter < entries.size(); iter++) {
            if (entries.get(iter).due() != null) {
                out.add(entries.get(iter));
            }
        }
        return out;
    }

    /// The server answered for the hold: its id and its status there. A
    /// hold the server has as RESUMED or DISCARDED is so here too, whatever
    /// the register thought -- another register took it.
    public void parked(String clientId, ParkedCartDto server) {
        Entry e = find(clientId);
        if (e == null || server == null) {
            return;
        }
        e.parkSent = true;
        e.serverId = server.id() == null ? "" : server.id();
        if (!PARKED.equals(server.status()) && server.status() != null) {
            e.status = server.status();
            e.moveSent = true;
        }
        trim();
        save();
    }

    /// The server took the resume or discard (or answered that it was
    /// already so).
    public void moved(String clientId) {
        Entry e = find(clientId);
        if (e == null) {
            return;
        }
        e.moveSent = true;
        trim();
        save();
    }

    /// The server refused the move: the hold went the other way elsewhere.
    public void refused(String clientId, String serverStatus) {
        Entry e = find(clientId);
        if (e == null) {
            return;
        }
        if (serverStatus != null && serverStatus.length() > 0) {
            e.status = serverStatus;
        }
        e.moveSent = true;
        save();
    }

    private void trim() {
        int done = 0;
        for (int iter = entries.size() - 1; iter >= 0; iter--) {
            Entry e = entries.get(iter);
            if (e.due() == null && !PARKED.equals(e.status)) {
                done++;
                if (done > KEEP_DONE) {
                    entries.remove(iter);
                }
            }
        }
    }

    // ---------------------------------------------------------------- disk

    private void save() {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < entries.size(); iter++) {
            Entry e = entries.get(iter);
            b.append(e.status).append('\t').append(e.parkSent ? '1' : '0')
                    .append(e.moveSent ? '1' : '0').append('\t').append(e.serverId)
                    .append('\t').append(codec.encode(e.cart)).append('\n');
        }
        persist.write(KEY, b.toString());
    }

    private void load() {
        String text = persist.read(KEY);
        if (text == null) {
            return;
        }
        int start = 0;
        while (start < text.length()) {
            int end = text.indexOf('\n', start);
            if (end < 0) {
                end = text.length();
            }
            entry(text.substring(start, end));
            start = end + 1;
        }
    }

    private void entry(String line) {
        int a = line.indexOf('\t');
        int b = a < 0 ? -1 : line.indexOf('\t', a + 1);
        int c = b < 0 ? -1 : line.indexOf('\t', b + 1);
        if (c < 0 || b - a != 3) {
            return;
        }
        ParkedCartDto cart;
        try {
            cart = codec.decode(line.substring(c + 1));
        } catch (RuntimeException bad) {
            return;
        }
        if (cart == null || cart.clientId() == null) {
            return;
        }
        Entry e = new Entry(cart);
        e.status = line.substring(0, a);
        e.parkSent = line.charAt(a + 1) == '1';
        e.moveSent = line.charAt(a + 2) == '1';
        e.serverId = line.substring(b + 1, c);
        entries.add(e);
    }
}
