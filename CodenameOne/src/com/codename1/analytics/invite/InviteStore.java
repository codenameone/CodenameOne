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
import java.util.Iterator;
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

    // A viral inviter can mint faster than a bad network drains the queue.
    // Dropping the oldest is right: an unregistered invite still attributes
    // once the server sees the click, so the newest are the ones whose
    // registration is still worth racing.
    static final int MAX_OUTBOX = 32;

    private InviteStore() {
    }

    static Map<String, String> read(String record) {
        try {
            Storage s = Storage.getInstance();
            if (s == null || !s.exists(record)) {
                return null;
            }
            Object o = s.readObject(record);
            if (!(o instanceof Map)) {
                return null;
            }
            Map<String, String> out = new LinkedHashMap<String, String>();
            Map raw = (Map) o;
            for (Iterator i = raw.keySet().iterator(); i.hasNext();) {
                Object k = i.next();
                Object v = raw.get(k);
                if (k instanceof String && v instanceof String) {
                    out.put((String) k, (String) v);
                }
            }
            return out;
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }

    // Returns false when the record did not reach the disk. Callers that care
    // about exactly-once behaviour check this; the rest may ignore it.
    static boolean write(String record, Map<String, String> values) {
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
            if (!(o instanceof List)) {
                return out;
            }
            List raw = (List) o;
            for (int i = 0; i < raw.size(); i++) {
                Object v = raw.get(i);
                if (v instanceof String) {
                    out.add((String) v);
                }
            }
        } catch (Throwable t) {
            Log.e(t);
        }
        return out;
    }

    static void writeOutbox(List<String> entries) {
        try {
            Storage s = Storage.getInstance();
            if (s == null) {
                return;
            }
            List<String> copy = new ArrayList<String>(entries);
            while (copy.size() > MAX_OUTBOX) {
                copy.remove(0);
            }
            if (copy.isEmpty()) {
                if (s.exists(OUTBOX)) {
                    s.deleteStorageFile(OUTBOX);
                }
                return;
            }
            s.writeObject(OUTBOX, copy);
        } catch (Throwable t) {
            Log.e(t);
        }
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
