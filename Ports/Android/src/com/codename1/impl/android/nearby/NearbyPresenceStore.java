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
package com.codename1.impl.android.nearby;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.codename1.nearby.companion.CompanionDevices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Where presence events wait for an app that is not running.
///
/// Split out of `CN1CompanionDeviceService` because that class extends
/// `android.companion.CompanionDeviceService`, which does not exist before
/// API 31. Touching it from `AndroidNearbyBackend` -- which every transport,
/// ranging or companion build constructs, from API 21 up -- made the class
/// fail to resolve on anything older, and the NoClassDefFoundError escaped
/// the constructor into the reflective catch that builds the bridge. The
/// whole nearby stack then reported itself unsupported on Android 21 to 30,
/// where the transport and ranging halves work perfectly well.
///
/// Nothing here touches an API newer than SharedPreferences.
///
/// @hidden not part of the public API
class NearbyPresenceStore {

    private NearbyPresenceStore() {
    }

    /// Associations the app has explicitly STOPPED watching in this process.
    ///
    /// The filter keys off what was UNregistered, not off what was
    /// registered. Observation survives process death -- the platform keeps
    /// watching and keeps binding this service -- while a set of registered
    /// ids starts empty and fills one registration at a time, so treating it
    /// as the authoritative list made the first re-registration turn into a
    /// whitelist: an appearance for a second still-watched association was
    /// dropped until the app happened to re-register that one too, which it
    /// may never do.
    ///
    /// Not knowing yet is not the same as not wanting it. Only an explicit
    /// unregister says the app is done, and that is what this records. There
    /// is deliberately no matching set of registered ids: nothing would read
    /// it, and one that looked authoritative without being it is what caused
    /// the defect.
    private static final Set<String> UNOBSERVED =
            Collections.synchronizedSet(new HashSet<String>());

    /// Records that the app asked to watch an association, so an event for
    /// one it stopped watching is dropped rather than delivered.
    ///
    /// #### Parameters
    ///
    /// - `associationId`: the association being watched
    public static void register(String associationId) {
        if (associationId != null) {
            UNOBSERVED.remove(associationId);
        }
    }

    /// Forgets an association.
    ///
    /// #### Parameters
    ///
    /// - `associationId`: the association no longer watched
    public static void unregister(String associationId) {
        if (associationId != null) {
            UNOBSERVED.add(associationId);
        }
    }


    // ------------------------------------------------------------------
    // The backlog that outlives the process
    // ------------------------------------------------------------------

    /// Where a presence event waits for an app that is not running.
    ///
    /// The platform starts this service for the event and does NOT start the
    /// application, which is the whole premise of the feature -- so the
    /// event goes into CompanionDevices' in-memory backlog, and if Android
    /// reclaims this idle process before the user opens the app, that
    /// backlog dies with it. The platform does not replay, so the listener
    /// documented to hear about the sighting "when the app next initializes"
    /// heard nothing at all. A record of what happened while the app was
    /// away has to survive the app not being there.
    private static final String PRESENCE_PREFS = "cn1-nearby-presence";
    private static final String PRESENCE_KEY = "backlog";
    /// The same bound CompanionDevices keeps, for the same reason: a device
    /// that flaps for a week must not grow this without limit.
    private static final int MAX_PERSISTED = 64;

    /// Sequence numbers this process has already handed to CompanionDevices.
    ///
    /// Its lifetime is exactly the in-memory backlog's, which is what makes
    /// the two agree: an event this process delivered is already in that
    /// backlog, so the restore must skip it, and if the process died neither
    /// this set nor that backlog exists and every persisted event replays.
    private static final Set<String> DELIVERED_HERE =
            Collections.synchronizedSet(new HashSet<String>());

    private static long presenceSequence;

    /// Persists an event and hands it to the in-memory backlog.
    /// Whether the app has explicitly stopped watching this association.
    static boolean isUnobserved(String associationId) {
        return UNOBSERVED.contains(associationId);
    }

    static void record(Context ctx, String encoded, boolean present) {
        String seq;
        synchronized (NearbyPresenceStore.class) {
            // Qualified by pid, so a sequence minted by an earlier process
            // cannot be mistaken for one this process delivered. A recycled
            // pid is harmless: the set that would have to match it is empty
            // in a process that has delivered nothing.
            seq = android.os.Process.myPid() + "-"
                    + Long.toString(++presenceSequence);
        }
        persist(ctx, seq + '\t' + (present ? '1' : '0') + '\t' + encoded);
        DELIVERED_HERE.add(seq);
        CompanionDevices.deliverPresenceChanged(encoded, present);
    }

    private static void persist(Context ctx, String row) {
        if (ctx == null) {
            return;
        }
        try {
            SharedPreferences prefs = ctx.getSharedPreferences(
                    PRESENCE_PREFS, Context.MODE_PRIVATE);
            String existing = prefs.getString(PRESENCE_KEY, "");
            List<String> rows = new ArrayList<String>();
            if (existing.length() > 0) {
                for (String r : existing.split("\n")) {
                    if (r.length() > 0) {
                        rows.add(r);
                    }
                }
            }
            rows.add(row);
            while (rows.size() > MAX_PERSISTED) {
                rows.remove(0);
            }
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < rows.size(); i++) {
                if (i > 0) {
                    out.append('\n');
                }
                out.append(rows.get(i));
            }
            prefs.edit().putString(PRESENCE_KEY, out.toString()).commit();
        } catch (Throwable unavailable) {
            // Nothing can be done about a store that will not take it, and
            // failing the event outright would lose what the in-memory
            // backlog can still carry for a process that lives long enough.
            Log.w("CN1Nearby", "presence backlog not persisted", unavailable);
        }
    }

    /// Hands back every persisted event this process has not already
    /// delivered, and clears the store.
    ///
    /// Called when the nearby backend is built, which is what an app does on
    /// its way to registering a presence listener.
    ///
    /// #### Parameters
    ///
    /// - `ctx`: any context; the store is per-application
    ///
    /// #### Returns
    ///
    /// rows of `present-flag TAB encoded`, oldest first, never null
    static String[] takePersistedPresence(Context ctx) {
        if (ctx == null) {
            return new String[0];
        }
        String existing;
        try {
            SharedPreferences prefs = ctx.getSharedPreferences(
                    PRESENCE_PREFS, Context.MODE_PRIVATE);
            existing = prefs.getString(PRESENCE_KEY, "");
            prefs.edit().remove(PRESENCE_KEY).commit();
        } catch (Throwable unavailable) {
            return new String[0];
        }
        if (existing.length() == 0) {
            return new String[0];
        }
        List<String> out = new ArrayList<String>();
        for (String row : existing.split("\n")) {
            int tab = row.indexOf('\t');
            if (tab <= 0) {
                continue;
            }
            String seq = row.substring(0, tab);
            if (DELIVERED_HERE.remove(seq)) {
                // This process already gave it to CompanionDevices, so it is
                // in the in-memory backlog and replaying it would deliver the
                // same sighting twice.
                continue;
            }
            out.add(row.substring(tab + 1));
        }
        return out.toArray(new String[out.size()]);
    }
}
