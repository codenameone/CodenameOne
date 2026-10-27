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
package com.codename1.impl;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/// Multicast DNS service discovery (RFC 6762 and RFC 6763) written in Java over
/// a UDP socket, for platforms without a system responder to call: the Linux
/// port, embedded boards and the headless backend. Each supplies only the
/// `raw` socket primitives, so the protocol is one implementation everywhere.
///
/// It publishes services (probing for a unique name, announcing, answering
/// queries, saying goodbye) and browses for them (querying with back-off,
/// resolving each instance's SRV, TXT and address, and reporting it lost on a
/// goodbye or when its records expire). It answers for the host's own name
/// too, so a client can reach `<host>.local` without another responder.
///
/// One thread, started with the first publish or browse, owns the socket and
/// every mutable structure. Callers on other threads only replace the
/// immutable arrays of what is published and browsed, so nothing is locked.
/// Listener callbacks run on that thread; the client platform hands them to
/// the EDT, the backend calls them as they are.
///
/// Part of the framework's implementation, not intended for application use:
/// applications use `com.codename1.io.bonjour` or the backend's `Discovery`.
@SharedWithBackend
public abstract class MdnsEngine {

    /// Receives what a browse finds. Called on the engine's thread.
    public interface Listener {
        /// An instance of the browsed type was resolved, or its address, port
        /// or TXT record changed.
        void resolved(Object browse, String name, String type, String host, int port,
                Map<String, String> txt);

        /// An instance said goodbye or its records expired.
        void lost(Object browse, String name, String type);

        /// The engine could not start or its socket failed.
        void failed(Object browse, String message);
    }

    static final int TYPE_A = 1;
    static final int TYPE_PTR = 12;
    static final int TYPE_TXT = 16;
    static final int TYPE_SRV = 33;
    static final int TYPE_ANY = 255;
    private static final int CLASS_IN = 1;
    private static final int CACHE_FLUSH = 0x8000;
    private static final int TTL_HOST = 120;
    private static final int TTL_OTHER = 4500;
    private static final int MAX_PACKET = 9000;
    private static final String SERVICES = "_services._dns-sd._udp.local";

    private final AtomicReference<Service[]> services = new AtomicReference<Service[]>(new Service[0]);
    private final AtomicReference<Browse[]> browses = new AtomicReference<Browse[]>(new Browse[0]);
    private final AtomicReference<Thread> thread = new AtomicReference<Thread>();

    // Everything below belongs to the engine thread.
    private int fd = -1;
    private String hostName;
    private byte[][] addresses = new byte[0][];
    private final ArrayList<Service> live = new ArrayList<Service>();
    private final ArrayList<Browse> watching = new ArrayList<Browse>();
    private final byte[] in = new byte[MAX_PACKET];

    /// Opens the multicast socket: UDP port 5353, joined to 224.0.0.251 on
    /// every IPv4 interface. Answers a handle >= 0, or a negative error.
    protected abstract int rawOpen();

    /// Sends a packet to 224.0.0.251:5353 on every interface. Answers >= 0 or a
    /// negative error.
    protected abstract int rawSend(int handle, byte[] packet, int length);

    /// Waits up to timeoutMs for a packet. Answers its length, 0 on a timeout,
    /// or a negative error.
    protected abstract int rawReceive(int handle, byte[] buffer, int timeoutMs);

    /// The host name without a domain, then each IPv4 address of an interface
    /// that is up, not loopback and multicast capable, one per line.
    protected abstract String rawHostInfo();

    /// Closes the socket.
    protected abstract void rawClose(int handle);

    /// Milliseconds on a clock that does not jump; `System.currentTimeMillis`
    /// by default. Tests replace it.
    protected long now() {
        return System.currentTimeMillis();
    }

    // ------------------------------------------------------------------ API

    /// Publishes a service; answers a handle for [#unpublish(Object)]. The
    /// type is `_name._tcp` or `_name._udp`, with or without `.local.`. A name
    /// another host already uses gets a numeric suffix, as Bonjour does.
    public Object publish(String name, String type, int port, Map<String, String> txt) {
        Service s = new Service(name, normalizeType(type), port, copy(txt));
        Service[] cur;
        Service[] next;
        do {
            cur = services.get();
            next = new Service[cur.length + 1];
            System.arraycopy(cur, 0, next, 0, cur.length);
            next[cur.length] = s;
        } while (!services.compareAndSet(cur, next));
        start();
        return s;
    }

    /// Withdraws a published service; its goodbye is sent at once.
    public void unpublish(Object handle) {
        Service[] cur;
        Service[] next;
        do {
            cur = services.get();
            int i = indexOf(cur, handle);
            if (i < 0) {
                return;
            }
            next = new Service[cur.length - 1];
            System.arraycopy(cur, 0, next, 0, i);
            System.arraycopy(cur, i + 1, next, i, next.length - i);
        } while (!services.compareAndSet(cur, next));
    }

    /// Starts browsing for a type; answers a handle for [#stopBrowse(Object)].
    public Object browse(String type, Listener listener) {
        Browse b = new Browse(normalizeType(type), listener);
        Browse[] cur;
        Browse[] next;
        do {
            cur = browses.get();
            next = new Browse[cur.length + 1];
            System.arraycopy(cur, 0, next, 0, cur.length);
            next[cur.length] = b;
        } while (!browses.compareAndSet(cur, next));
        start();
        return b;
    }

    /// Stops a browse; its listener is not called again.
    public void stopBrowse(Object handle) {
        Browse[] cur;
        Browse[] next;
        do {
            cur = browses.get();
            int i = indexOf(cur, handle);
            if (i < 0) {
                return;
            }
            next = new Browse[cur.length - 1];
            System.arraycopy(cur, 0, next, 0, i);
            System.arraycopy(cur, i + 1, next, i, next.length - i);
        } while (!browses.compareAndSet(cur, next));
    }

    /// The name a published service ended up with, which differs from the one
    /// asked for when another host had it. Null for an unknown handle.
    public String publishedName(Object handle) {
        return handle instanceof Service ? ((Service) handle).currentName.get() : null;
    }

    // Arrays are built by hand throughout: toArray(T[]) creates its result
    // reflectively, which ParparVM cannot do for an arbitrary component type.
    private static int indexOf(Object[] cur, Object item) {
        for (int i = 0; i < cur.length; i++) {
            if (cur[i] == item) { // NOPMD CompareObjectsWithEquals - a handle is found by identity
                return i;
            }
        }
        return -1;
    }

    private static Map<String, String> copy(Map<String, String> txt) {
        HashMap<String, String> out = new HashMap<String, String>();
        if (txt != null) {
            out.putAll(txt);
        }
        return out;
    }

    /// `_http._tcp.` or `_http._tcp.local.` to `_http._tcp.local`.
    static String normalizeType(String type) {
        String t = type == null ? "" : type.trim();
        while (t.endsWith(".")) {
            t = t.substring(0, t.length() - 1);
        }
        if (!t.regionMatches(true, Math.max(0, t.length() - 6), ".local", 0, 6)) {
            t = t + ".local";
        }
        return t;
    }

    private void start() {
        if (thread.get() != null) {
            return;
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                loop();
            }
        }, "mDNS");
        // Discovery never keeps a process alive by itself.
        t.setDaemon(true);
        if (thread.compareAndSet(null, t)) {
            t.start();
        }
    }

    // --------------------------------------------------------------- engine

    private void loop() {
        fd = rawOpen();
        if (fd < 0) {
            fail("mDNS socket could not be opened (error " + (-fd) + ")");
            return;
        }
        readHostInfo();
        long lastHostInfo = now();
        while (true) {
            long t = now();
            if (t - lastHostInfo > 30000) {
                // Addresses come and go (DHCP, a cable); re-read them now and then.
                readHostInfo();
                lastHostInfo = t;
            }
            syncServices(t);
            syncBrowses(t);
            if (live.isEmpty() && watching.isEmpty() && services.get().length == 0
                    && browses.get().length == 0) {
                // Nothing left to do; a later publish or browse starts a new thread.
                rawClose(fd);
                fd = -1;
                thread.set(null);
                if (services.get().length == 0 && browses.get().length == 0) {
                    return;
                }
                start();
                return;
            }
            long next = t + 1000;
            for (Service s : live) {
                next = Math.min(next, s.due);
            }
            for (Browse b : watching) {
                next = Math.min(next, b.nextQuery);
            }
            int wait = (int) Math.max(20, Math.min(1000, next - now()));
            int n = rawReceive(fd, in, wait);
            if (n < 0) {
                fail("mDNS receive failed (error " + (-n) + ")");
                rawClose(fd);
                fd = -1;
                thread.set(null);
                return;
            }
            if (n > 0) {
                handlePacket(in, n);
            }
        }
    }

    private void fail(String message) {
        Browse[] b = browses.get();
        for (Browse each : b) {
            each.listener.failed(each, message);
        }
    }

    private void readHostInfo() {
        String info = rawHostInfo();
        ArrayList<byte[]> list = new ArrayList<byte[]>();
        String host = null;
        if (info != null) {
            int start = 0;
            for (int i = 0; i <= info.length(); i++) {
                if (i == info.length() || info.charAt(i) == '\n') {
                    String line = info.substring(start, i).trim();
                    start = i + 1;
                    if (line.length() == 0) {
                        continue;
                    }
                    if (host == null) {
                        host = line;
                    } else {
                        byte[] a = parseIpv4(line);
                        if (a != null) {
                            list.add(a);
                        }
                    }
                }
            }
        }
        if (host == null || host.length() == 0) {
            host = "codenameone";
        }
        int dot = host.indexOf('.');
        if (dot > 0) {
            host = host.substring(0, dot);
        }
        hostName = host + ".local";
        byte[][] out = new byte[list.size()][];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i);
        }
        addresses = out;
    }

    static byte[] parseIpv4(String s) {
        byte[] out = new byte[4];
        int part = 0;
        int value = -1;
        for (int i = 0; i <= s.length(); i++) {
            char c = i == s.length() ? '.' : s.charAt(i);
            if (c == '.') {
                if (value < 0 || value > 255 || part > 3) {
                    return null;
                }
                out[part++] = (byte) value;
                value = -1;
            } else if (c >= '0' && c <= '9') {
                value = (value < 0 ? 0 : value * 10) + (c - '0');
            } else {
                return null;
            }
        }
        return part == 4 ? out : null;
    }

    // ------------------------------------------------------------ publishing

    private void syncServices(long t) {
        Service[] want = services.get();
        // Withdrawn services say goodbye.
        for (Iterator<Service> it = live.iterator(); it.hasNext();) {
            Service s = it.next();
            if (!contains(want, s)) {
                it.remove();
                if (s.state >= Service.ANNOUNCING) {
                    Packet p = new Packet(true);
                    addServiceRecords(p, s, 0);
                    send(p);
                }
            }
        }
        for (int i = 0; i < want.length; i++) {
            if (!live.contains(want[i])) {
                want[i].due = t;
                live.add(want[i]);
            }
        }
        for (Service s : live) {
            if (s.due > t) {
                continue;
            }
            if (s.state == Service.PROBING) {
                if (s.step < 3) {
                    // A probe asks who owns the name and proposes our records.
                    Packet p = new Packet(false);
                    p.question(s.instance(), TYPE_ANY, true);
                    p.section = AUTHORITY;
                    p.srv(s.instance(), TTL_HOST, s.port, hostName, false);
                    send(p);
                    s.step++;
                    s.due = t + 250;
                } else {
                    s.state = Service.ANNOUNCING;
                    s.step = 0;
                    s.due = t;
                }
            }
            if (s.state == Service.ANNOUNCING && s.due <= t) {
                Packet p = new Packet(true);
                addServiceRecords(p, s, -1);
                send(p);
                s.step++;
                if (s.step >= 2) {
                    s.state = Service.LIVE;
                    s.due = Long.MAX_VALUE;
                } else {
                    s.due = t + 1000;
                }
            }
        }
    }

    private static boolean contains(Object[] a, Object o) {
        for (Object each : a) {
            if (each == o) { // NOPMD CompareObjectsWithEquals - membership by identity
                return true;
            }
        }
        return false;
    }

    /// The PTR, SRV and TXT records of a service and the host's addresses; ttl
    /// -1 means each record's usual TTL, 0 a goodbye.
    private void addServiceRecords(Packet p, Service s, int ttl) {
        p.ptr(s.type, ttl < 0 ? TTL_OTHER : ttl, s.instance());
        p.ptr(SERVICES, ttl < 0 ? TTL_OTHER : ttl, s.type);
        p.srv(s.instance(), ttl < 0 ? TTL_HOST : ttl, s.port, hostName, true);
        p.txt(s.instance(), ttl < 0 ? TTL_OTHER : ttl, s.txt);
        if (ttl != 0) {
            addAddresses(p);
        }
    }

    private void addAddresses(Packet p) {
        for (byte[] address : addresses) {
            p.a(hostName, TTL_HOST, address);
        }
    }

    private void answer(String name, int type) {
        Packet p = new Packet(true);
        boolean any = type == TYPE_ANY;
        for (Service s : live) {
            if (s.state == Service.PROBING) {
                continue;
            }
            if ((type == TYPE_PTR || any) && name.equalsIgnoreCase(s.type)) {
                p.section = ANSWER;
                p.ptr(s.type, TTL_OTHER, s.instance());
                // The rest of what a browser needs travels as additionals.
                p.section = ADDITIONAL;
                p.srv(s.instance(), TTL_HOST, s.port, hostName, true);
                p.txt(s.instance(), TTL_OTHER, s.txt);
                addAddresses(p);
            } else if ((type == TYPE_PTR || any) && SERVICES.equalsIgnoreCase(name)) {
                p.section = ANSWER;
                p.ptr(SERVICES, TTL_OTHER, s.type);
            } else if (name.equalsIgnoreCase(s.instance())) {
                p.section = ANSWER;
                if (type == TYPE_SRV || any) {
                    p.srv(s.instance(), TTL_HOST, s.port, hostName, true);
                }
                if (type == TYPE_TXT || any) {
                    p.txt(s.instance(), TTL_OTHER, s.txt);
                }
                p.section = ADDITIONAL;
                addAddresses(p);
            }
        }
        if ((type == TYPE_A || any) && name.equalsIgnoreCase(hostName) && !live.isEmpty()) {
            p.section = ANSWER;
            addAddresses(p);
        }
        if (p.answers() > 0) {
            send(p);
        }
    }

    private void send(Packet p) {
        if (fd >= 0) {
            byte[] bytes = p.toBytes();
            rawSend(fd, bytes, bytes.length);
        }
    }

    // ------------------------------------------------------------- browsing

    private void syncBrowses(long t) {
        Browse[] want = browses.get();
        for (Iterator<Browse> it = watching.iterator(); it.hasNext();) {
            if (!contains(want, it.next())) {
                it.remove();
            }
        }
        for (int i = 0; i < want.length; i++) {
            if (!watching.contains(want[i])) {
                want[i].nextQuery = t;
                watching.add(want[i]);
            }
        }
        for (Browse b : watching) {
            // Expired instances are lost; incomplete ones are asked about again.
            for (Iterator<Instance> it = b.instances.values().iterator(); it.hasNext();) {
                Instance inst = it.next();
                if (inst.expires <= t) {
                    it.remove();
                    if (inst.reported) {
                        b.listener.lost(b, inst.label, displayType(b.type));
                    }
                } else if (!inst.complete() && inst.nextAsk <= t) {
                    Packet p = new Packet(false);
                    if (inst.target == null) {
                        p.question(inst.fullName, TYPE_SRV, false);
                    }
                    if (inst.txt == null) {
                        p.question(inst.fullName, TYPE_TXT, false);
                    }
                    if (inst.target != null && inst.host == null) {
                        p.question(inst.target, TYPE_A, false);
                    }
                    send(p);
                    inst.nextAsk = t + 1000;
                }
            }
            if (b.nextQuery <= t) {
                Packet p = new Packet(false);
                p.question(b.type, TYPE_PTR, false);
                send(p);
                b.interval = b.interval == 0 ? 1000 : Math.min(b.interval * 2, 60000);
                b.nextQuery = t + b.interval;
            }
        }
    }

    private static String displayType(String type) {
        // _http._tcp.local to the _http._tcp. form BonjourService reports.
        return type.substring(0, type.length() - 5);
    }

    // -------------------------------------------------------------- packets

    private void handlePacket(byte[] data, int length) {
        Reader r = new Reader(data, length);
        ArrayList<Record> records = new ArrayList<Record>();
        try {
            r.pos = 2;
            int flags = r.u16();
            int qd = r.u16();
            int an = r.u16();
            int ns = r.u16();
            int ar = r.u16();
            boolean response = (flags & 0x8000) != 0;
            for (int i = 0; i < qd; i++) {
                String name = r.name();
                int type = r.u16();
                r.u16();
                if (!response) {
                    answer(name, type);
                }
            }
            if (!response) {
                // A probe's authority section is the other host's claim.
                for (int i = 0; i < ns; i++) {
                    Record rec = r.record();
                    if (rec != null) {
                        checkConflict(rec, true);
                    }
                }
                return;
            }
            int total = an + ns + ar;
            for (int i = 0; i < total; i++) {
                Record rec = r.record();
                if (rec != null) {
                    records.add(rec);
                }
            }
        } catch (RuntimeException malformed) {
            // A truncated or malformed packet from the network is ignored.
            return;
        }
        // Acted on outside the try, so nothing here relies on the catch.
        for (Record rec : records) {
            checkConflict(rec, false);
        }
        absorb(records);
    }

    /// Another host claiming the name one of our services is probing for, or
    /// already uses, renames ours: "Name (2)", then "(3)" and so on.
    private void checkConflict(Record rec, boolean probe) {
        if (rec.type != TYPE_SRV && rec.type != TYPE_TXT) {
            return;
        }
        for (Service s : live) {
            if (!rec.name.equalsIgnoreCase(s.instance())) {
                continue;
            }
            if (rec.type == TYPE_SRV && rec.port == s.port && hostName.equalsIgnoreCase(rec.target)) {
                // Our own packet, looped back.
                continue;
            }
            if (rec.type == TYPE_TXT) {
                continue;
            }
            // Two hosts probing for one name at once: the lexicographically later
            // claim keeps it (RFC 6762 8.2, compared here on port then target),
            // so exactly one of them renames rather than both, forever.
            boolean lose = !probe || (s.state == Service.PROBING && (rec.port > s.port
                    || (rec.port == s.port && asciiLower(rec.target).compareTo(asciiLower(hostName)) > 0)));
            if (lose) {
                s.suffix++;
                s.currentName.set(s.baseName + " (" + (s.suffix + 1) + ")");
                s.state = Service.PROBING;
                s.step = 0;
                s.due = now() + 250;
            }
        }
    }

    private void absorb(ArrayList<Record> records) {
        long t = now();
        for (Browse b : watching) {
            for (Record rec : records) {
                if (rec.type == TYPE_PTR && rec.name.equalsIgnoreCase(b.type)) {
                    String key = asciiLower(rec.target);
                    Instance inst = b.instances.get(key);
                    if (rec.ttl == 0) {
                        if (inst != null) {
                            // A goodbye: gone in a second unless re-announced (RFC 6762 10.1).
                            inst.expires = Math.min(inst.expires, t + 1000);
                        }
                        continue;
                    }
                    if (inst == null) {
                        inst = new Instance(rec.target, label(rec.target, b.type));
                        b.instances.put(key, inst);
                    }
                    inst.expires = t + rec.ttl * 1000L;
                }
            }
            for (Instance inst : b.instances.values()) {
                boolean changed = false;
                for (Record rec : records) {
                    if (rec.type == TYPE_SRV && rec.name.equalsIgnoreCase(inst.fullName) && rec.ttl > 0) {
                        if (inst.port != rec.port || !rec.target.equalsIgnoreCase(inst.target)) {
                            inst.port = rec.port;
                            inst.target = rec.target;
                            inst.host = null;
                            changed = true;
                        }
                    } else if (rec.type == TYPE_TXT && rec.name.equalsIgnoreCase(inst.fullName) && rec.ttl > 0) {
                        if (inst.txt == null || !inst.txt.equals(rec.txt)) {
                            inst.txt = rec.txt;
                            changed = true;
                        }
                    }
                }
                if (inst.target != null) {
                    for (Record rec : records) {
                        if (rec.type == TYPE_A && rec.name.equalsIgnoreCase(inst.target) && rec.ttl > 0) {
                            if (!rec.address.equals(inst.host)) {
                                inst.host = rec.address;
                                changed = true;
                            }
                            break;
                        }
                    }
                }
                if (inst.complete() && (changed || !inst.reported)) {
                    inst.reported = true;
                    b.listener.resolved(b, inst.label, displayType(b.type), inst.host, inst.port,
                            new HashMap<String, String>(inst.txt));
                }
            }
        }
    }

    /// The instance label of `My Device._http._tcp.local`.
    private static String label(String fullName, String type) {
        int end = fullName.length() - type.length() - 1;
        return end > 0 ? fullName.substring(0, end) : fullName;
    }

    /// ASCII-only lower case: `toLowerCase` folds by locale, and a DNS name is
    /// compared without regard to ASCII case only (RFC 4343).
    static String asciiLower(String s) {
        char[] c = s.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] >= 'A' && c[i] <= 'Z') {
                c[i] = (char) (c[i] + 32);
            }
        }
        return new String(c);
    }

    // ---------------------------------------------------------------- model

    static final class Service {
        static final int PROBING = 0;
        static final int ANNOUNCING = 1;
        static final int LIVE = 2;

        final String baseName;
        final AtomicReference<String> currentName;
        final String type;
        final int port;
        final Map<String, String> txt;
        int state;
        int step;
        int suffix;
        long due;

        Service(String name, String type, int port, Map<String, String> txt) {
            String n = name == null || name.length() == 0 ? "Codename One" : name;
            // A label is at most 63 bytes; leave room for a " (n)" suffix.
            while (utf8(n).length > 56) {
                n = n.substring(0, n.length() - 1);
            }
            this.baseName = n;
            this.currentName = new AtomicReference<String>(n);
            this.type = type;
            this.port = port;
            this.txt = txt;
        }

        String instance() {
            return currentName.get() + "." + type;
        }
    }

    static final class Browse {
        final String type;
        final Listener listener;
        final HashMap<String, Instance> instances = new HashMap<String, Instance>();
        long nextQuery;
        long interval;

        Browse(String type, Listener listener) {
            this.type = type;
            this.listener = listener;
        }
    }

    static final class Instance {
        final String fullName;
        final String label;
        String target;
        int port;
        String host;
        Map<String, String> txt;
        long expires;
        long nextAsk;
        boolean reported;

        Instance(String fullName, String label) {
            this.fullName = fullName;
            this.label = label;
        }

        boolean complete() {
            return target != null && host != null && txt != null;
        }
    }

    static final class Record {
        String name;
        int type;
        long ttl;
        // Empty rather than null for a record of another type, so a match on
        // the type is all a reader checks.
        String target = "";
        int port;
        String address = "";
        Map<String, String> txt;
    }

    // ------------------------------------------------------------- encoding

    static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            // Every runtime has UTF-8; this cannot happen.
            throw new IllegalStateException("UTF-8", e);
        }
    }

    static String utf8(byte[] b, int off, int len) {
        try {
            return new String(b, off, len, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8", e);
        }
    }

    /// Splits a name into labels. The first label of a service instance may
    /// contain dots, so instance names are split after it by the caller.
    private static void writeName(Writer w, String name) {
        String rest = name;
        // An instance name is "<label>.<type>": everything before a label that
        // starts with an underscore is one label, dots and all.
        int service = rest.indexOf("._");
        if (service > 0 && rest.charAt(0) != '_') {
            w.label(rest.substring(0, service));
            rest = rest.substring(service + 1);
        }
        int start = 0;
        for (int i = 0; i <= rest.length(); i++) {
            if (i == rest.length() || rest.charAt(i) == '.') {
                if (i > start) {
                    w.label(rest.substring(start, i));
                }
                start = i + 1;
            }
        }
        w.u8(0);
    }

    static final class Writer {
        byte[] buf = new byte[512];
        int pos;

        void ensure(int n) {
            if (pos + n > buf.length) {
                byte[] b = new byte[Math.max(buf.length * 2, pos + n)];
                System.arraycopy(buf, 0, b, 0, pos);
                buf = b;
            }
        }

        void u8(int v) {
            ensure(1);
            buf[pos++] = (byte) v;
        }

        void u16(int v) {
            u8(v >> 8);
            u8(v);
        }

        void u32(long v) {
            u16((int) (v >> 16));
            u16((int) v);
        }

        void bytes(byte[] b) {
            ensure(b.length);
            System.arraycopy(b, 0, buf, pos, b.length);
            pos += b.length;
        }

        void label(String s) {
            byte[] b = utf8(s);
            int n = Math.min(63, b.length);
            u8(n);
            ensure(n);
            System.arraycopy(b, 0, buf, pos, n);
            pos += n;
        }
    }

    static final int ANSWER = 0;
    static final int AUTHORITY = 1;
    static final int ADDITIONAL = 2;

    /// One outgoing message. Records go to the section [#section] names, each
    /// section into its own buffer, so they may be added in any order.
    static final class Packet {
        final boolean response;
        final Writer questionBody = new Writer();
        final Writer[] bodies = {new Writer(), new Writer(), new Writer()};
        final int[] counts = new int[3];
        int questions;
        int section = ANSWER;

        Packet(boolean response) {
            this.response = response;
        }

        int answers() {
            return counts[ANSWER];
        }

        void question(String name, int type, boolean unicastResponse) {
            writeName(questionBody, name);
            questionBody.u16(type);
            questionBody.u16(CLASS_IN | (unicastResponse ? 0x8000 : 0));
            questions++;
        }

        private Writer begin(String name, int type, long ttl, boolean unique) {
            Writer w = bodies[section];
            writeName(w, name);
            w.u16(type);
            w.u16(CLASS_IN | (unique ? CACHE_FLUSH : 0));
            w.u32(ttl);
            w.u16(0);
            counts[section]++;
            return w;
        }

        private static void end(Writer w, int start) {
            int len = w.pos - start;
            w.buf[start - 2] = (byte) (len >> 8);
            w.buf[start - 1] = (byte) len;
        }

        void ptr(String name, long ttl, String target) {
            Writer w = begin(name, TYPE_PTR, ttl, false);
            int s = w.pos;
            writeName(w, target);
            end(w, s);
        }

        void srv(String name, long ttl, int port, String target, boolean unique) {
            Writer w = begin(name, TYPE_SRV, ttl, unique);
            int s = w.pos;
            w.u16(0);
            w.u16(0);
            w.u16(port);
            writeName(w, target);
            end(w, s);
        }

        void txt(String name, long ttl, Map<String, String> txt) {
            Writer w = begin(name, TYPE_TXT, ttl, true);
            int s = w.pos;
            if (txt.isEmpty()) {
                w.u8(0);
            } else {
                for (Map.Entry<String, String> e : txt.entrySet()) {
                    byte[] b = utf8(e.getValue() == null ? e.getKey() : e.getKey() + "=" + e.getValue());
                    int n = Math.min(255, b.length);
                    w.u8(n);
                    w.ensure(n);
                    System.arraycopy(b, 0, w.buf, w.pos, n);
                    w.pos += n;
                }
            }
            end(w, s);
        }

        void a(String name, long ttl, byte[] address) {
            Writer w = begin(name, TYPE_A, ttl, true);
            int s = w.pos;
            w.bytes(address);
            end(w, s);
        }

        byte[] toBytes() {
            int size = 12 + questionBody.pos + bodies[0].pos + bodies[1].pos + bodies[2].pos;
            byte[] out = new byte[size];
            int flags = response ? 0x8400 : 0;
            int[] header = {flags, questions, counts[ANSWER], counts[AUTHORITY], counts[ADDITIONAL]};
            for (int i = 0; i < header.length; i++) {
                out[2 + i * 2] = (byte) (header[i] >> 8);
                out[3 + i * 2] = (byte) header[i];
            }
            int pos = 12;
            System.arraycopy(questionBody.buf, 0, out, pos, questionBody.pos);
            pos += questionBody.pos;
            for (int i = 0; i < 3; i++) {
                System.arraycopy(bodies[i].buf, 0, out, pos, bodies[i].pos);
                pos += bodies[i].pos;
            }
            return out;
        }
    }

    static final class Reader {
        final byte[] buf;
        final int length;
        int pos;

        Reader(byte[] buf, int length) {
            this.buf = buf;
            this.length = length;
        }

        int u8() {
            if (pos >= length) {
                throw new IllegalArgumentException("truncated");
            }
            return buf[pos++] & 0xff;
        }

        int u16() {
            return (u8() << 8) | u8();
        }

        long u32() {
            return ((long) u16() << 16) | u16();
        }

        String name() {
            StringBuilder sb = new StringBuilder();
            int p = pos;
            int jumps = 0;
            boolean jumped = false;
            while (true) {
                if (p >= length) {
                    throw new IllegalArgumentException("truncated name");
                }
                int len = buf[p] & 0xff;
                if (len == 0) {
                    p++;
                    break;
                }
                if ((len & 0xc0) == 0xc0) {
                    jumps++;
                    if (p + 1 >= length || jumps > 32) {
                        throw new IllegalArgumentException("bad pointer");
                    }
                    int target = ((len & 0x3f) << 8) | (buf[p + 1] & 0xff);
                    if (!jumped) {
                        pos = p + 2;
                        jumped = true;
                    }
                    p = target;
                    continue;
                }
                if (p + 1 + len > length) {
                    throw new IllegalArgumentException("truncated label");
                }
                if (sb.length() > 0) {
                    sb.append('.');
                }
                sb.append(utf8(buf, p + 1, len));
                p += 1 + len;
            }
            if (!jumped) {
                pos = p;
            }
            return sb.toString();
        }

        /// The next resource record, or null for a type this engine ignores.
        Record record() {
            Record r = new Record();
            r.name = name();
            r.type = u16();
            u16();
            r.ttl = u32();
            int len = u16();
            int end = pos + len;
            if (end > length) {
                throw new IllegalArgumentException("truncated record");
            }
            switch (r.type) {
                case TYPE_PTR:
                    r.target = name();
                    break;
                case TYPE_SRV:
                    u16();
                    u16();
                    r.port = u16();
                    r.target = name();
                    break;
                case TYPE_A:
                    if (len != 4) {
                        pos = end;
                        return null;
                    }
                    r.address = (buf[pos] & 0xff) + "." + (buf[pos + 1] & 0xff) + "."
                            + (buf[pos + 2] & 0xff) + "." + (buf[pos + 3] & 0xff);
                    break;
                case TYPE_TXT:
                    r.txt = new HashMap<String, String>();
                    int p = pos;
                    while (p < end) {
                        int n = buf[p] & 0xff;
                        if (n > 0 && p + 1 + n <= end) {
                            String entry = utf8(buf, p + 1, n);
                            int eq = entry.indexOf('=');
                            if (eq > 0) {
                                r.txt.put(entry.substring(0, eq), entry.substring(eq + 1));
                            } else if (eq < 0) {
                                r.txt.put(entry, "");
                            }
                        }
                        p += 1 + n;
                    }
                    break;
                default:
                    pos = end;
                    return null;
            }
            pos = end;
            return r;
        }
    }
}
