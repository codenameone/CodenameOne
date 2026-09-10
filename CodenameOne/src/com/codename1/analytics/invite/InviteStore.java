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
package com.codename1.analytics.invite;

import com.codename1.io.Log;
import com.codename1.io.Storage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// The three durable records invite attribution keeps on the device.
//
// Storage rather than Preferences, for the reason Continuity records: a
// Preferences write discards Storage.writeObject's boolean and the matching
// read comes back out of the same in-memory map, so a failed write is
// invisible. These records decide whether a user is attributed at all and
// whether the same install is attributed twice, so a silent write failure has
// to be observable.
//
// Every record is a flat map of strings. That is what survives Util's object
// serialization on every port without registering an Externalizable, and it
// keeps the format readable if it ever has to be inspected on a device.
final class InviteStore {
    // The fingerprint and the code seen before attribution resolves.
    static final String PENDING = "CN1$InvitePending";

    // The resolved attribution, plus whether the application has been told.
    static final String ATTRIBUTION = "CN1$InviteAttribution";

    // Mint registrations that have not reached the link service yet.
    static final String OUTBOX = "CN1$InviteOutbox";

    // Entries leave this queue when the server acknowledges them, so the cap is
    // a safety ceiling rather than a working limit -- and it was far too low
    // for that. A dropped registration is not recoverable: the code carries no
    // inviter, campaign, payload or parameters, so a click on a link that was
    // already shared can never be joined to any of it.
    //
    // 512 short JSON bodies is well under a megabyte, and reaching it means the
    // device minted 512 invites without once reaching the network, which is far
    // outside anything the design contemplates. An unbounded on-device queue is
    // still not something to ship, so the ceiling stays -- but breaching it is
    // logged rather than silent, because it means invites are being lost.
    static final int MAX_OUTBOX = 512;

    private InviteStore() {
    }

    static Map<String, String> read(String record) {
        try {
            Storage s = Storage.getInstance();
            if (s == null || !s.exists(record)) {
                return null;
            }
            Object o = s.readObject(record);
            // Positive instanceof guards throughout: ParparVM does not throw
            // on a failed cast, so the catch below would never see one and the
            // wrong object would simply be read as the wrong type.
            if (o instanceof Map) {
                Map raw = (Map) o;
                Map<String, String> out = new LinkedHashMap<String, String>();
                for (Object next : raw.entrySet()) {
                    if (next instanceof Map.Entry) {
                        Map.Entry en = (Map.Entry) next;
                        Object k = en.getKey();
                        Object v = en.getValue();
                        if (k instanceof String && v instanceof String) {
                            out.put((String) k, (String) v);
                        }
                    }
                }
                return out;
            }
            return null;
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }

    // Returns false when the record did not reach the disk. Callers that care
    // about exactly-once behaviour check this; the rest may ignore it.
    // The same seam for a named record. A full or read-only store cannot be
    // produced from a test, and the paths that only run when a write fails are
    // the ones most worth pinning.
    private static String failNextNamed;

    static void failNextWriteForTest(String name) {
        failNextNamed = name;
    }

    static boolean write(String record, Map<String, String> values) {
        if (record != null && record.equals(failNextNamed)) {
            failNextNamed = null;
            return false;
        }
        try {
            Storage s = Storage.getInstance();
            if (s == null) {
                return false;
            }
            return s.writeObject(record, new LinkedHashMap<String, String>(values));
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    static void delete(String record) {
        try {
            Storage s = Storage.getInstance();
            if (s != null && s.exists(record)) {
                s.deleteStorageFile(record);
            }
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    static List<String> readOutbox() {
        List<String> out = new ArrayList<String>();
        try {
            Storage s = Storage.getInstance();
            if (s == null || !s.exists(OUTBOX)) {
                return out;
            }
            Object o = s.readObject(OUTBOX);
            if (o instanceof List) {
                List raw = (List) o;
                for (Object v : raw) {
                    if (v instanceof String) {
                        out.add((String) v);
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(t);
        }
        return out;
    }

    /// Returns false when the queue could not be persisted -- a full or
    /// read-only store. The caller has to know: an entry that never reached
    /// the outbox carries the campaign, channel, payload and preview of a link
    /// that has already been handed out, and nothing can reconstruct it later.
    // Package private test seam. A full or read-only store cannot be produced
    // from a test, and the paths that only run when the write fails are the
    // ones most worth pinning -- they are what happens when the durable queue
    // is gone.
    private static boolean failNextWrite;

    static void failNextOutboxWriteForTest() {
        failNextWrite = true;
    }

    static boolean writeOutbox(List<String> entries) {
        if (failNextWrite) {
            failNextWrite = false;
            return false;
        }
        // The cap is applied OUTSIDE the try, deliberately.
        //
        // copy.remove(0) on a List<String> compiles to a CHECKCAST, and
        // ParparVM does not throw for a failed cast -- so a checked cast inside
        // a catch(Throwable) is a handler that cannot run on iOS, which
        // check-cast-semantics.sh refuses outright. Nothing here can fail
        // anyway: it is a copy, a size comparison and a removal.
        List<String> copy = new ArrayList<String>(entries);
        int dropped = 0;
        while (copy.size() > MAX_OUTBOX) {
            // Reported to Invites before it goes, so isRegistered() can keep
            // saying no about it. That method reads absence from BOTH the
            // outbox and the unacknowledged set as acknowledgement, and an
            // evicted entry is in neither -- so the one registration the server
            // is guaranteed never to have received was reported as registered,
            // and only the log below said otherwise.
            Invites.registrationEvicted(copy.remove(0));
            dropped++;
        }
        if (dropped > 0) {
            Log.p("invite: dropped " + dropped + " unacknowledged registration(s); "
                    + "those invite links can no longer be attributed", Log.ERROR);
        }
        try {
            Storage s = Storage.getInstance();
            if (s == null) {
                return false;
            }
            if (copy.isEmpty()) {
                if (s.exists(OUTBOX)) {
                    s.deleteStorageFile(OUTBOX);
                }
                return true;
            }
            return s.writeObject(OUTBOX, copy);
        } catch (Throwable t) {
            Log.e(t);
        }
        return false;
    }

    static String get(Map<String, String> record, String key, String def) {
        if (record == null) {
            return def;
        }
        String v = record.get(key);
        return v == null ? def : v;
    }

    static long getLong(Map<String, String> record, String key, long def) {
        String v = get(record, key, null);
        if (v == null || v.length() == 0) {
            return def;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    static int getInt(Map<String, String> record, String key, int def) {
        return (int) getLong(record, key, def);
    }

    static double getDouble(Map<String, String> record, String key, double def) {
        String v = get(record, key, null);
        if (v == null || v.length() == 0) {
            return def;
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    static boolean getBoolean(Map<String, String> record, String key, boolean def) {
        String v = get(record, key, null);
        if (v == null) {
            return def;
        }
        return "true".equals(v);
    }

    static void put(Map<String, String> record, String key, String value) {
        if (value != null) {
            record.put(key, value);
        }
    }
}
