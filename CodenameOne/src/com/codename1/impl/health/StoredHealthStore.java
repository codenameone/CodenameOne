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
package com.codename1.impl.health;

import com.codename1.health.HealthSample;
import com.codename1.io.Log;
import com.codename1.io.Storage;

import java.util.List;

/// The local store, kept across restarts.
///
/// This is what the Windows, Linux and JavaScript ports get. Those report
/// [com.codename1.health.HealthAvailability#LOCAL_ONLY], which says the
/// data is only ever this app's own -- not that it evaporates when the
/// process exits. Without this the base class's `persist` hook was a no-op
/// and every write on those ports was lost on the next launch, while the
/// developer guide promised durability.
///
/// The simulator deliberately does not use this: its dataset is scripted,
/// and a scripted run that leaked into the next one would make the
/// simulator's whole point -- a known starting state -- untrue.
///
/// #### It is not encrypted
///
/// `Storage` is plain on-device storage. There is no OS-level protected
/// health store to hand this to on a desktop or in a browser -- that is
/// precisely why these ports report `LOCAL_ONLY` rather than pretending to
/// be HealthKit. Treat it as suitable for an app's own recorded data, not
/// as a place to mirror somebody's medical history.
public class StoredHealthStore extends LocalHealthStore {

    /// The whole store rides in one entry. It is rewritten on every
    /// mutation, which is the right trade for a store that exists so
    /// desktop and browser apps can develop against real aggregation:
    /// datasets are small, and a per-record scheme would have to solve
    /// compaction and partial-write recovery for no benefit at this size.
    private static final String KEY = "cn1$health$local";

    private boolean loaded;

    public StoredHealthStore() {
        restore();
    }

    private void restore() {
        String blob = null;
        try {
            Object stored = Storage.getInstance().readObject(KEY);
            if (stored instanceof String) {
                blob = (String) stored;
            }
        } catch (RuntimeException ex) {
            // A store that cannot be read is an empty one. Failing
            // construction here would take the whole Health facade down
            // with it, which is a worse answer than starting fresh.
            Log.p("CN1 Health: could not read the local store, starting"
                    + " empty (" + ex + ")");
        }
        List<HealthSample> restored = LocalHealthCodec.decode(blob);
        for (HealthSample s : restored) {
            addSampleDirect(s);
        }
        // Only after the restore, so the writes it performs do not each
        // rewrite the file they are being read from.
        loaded = true;
    }

    @Override
    protected boolean persist() {
        if (!loaded) {
            return true;
        }
        try {
            // The return value matters: Storage reports a full or
            // unwritable store by answering false rather than throwing,
            // and ignoring it let a write be acknowledged as durable when
            // it only ever reached memory. The caller sees the failure
            // now, and the change is rolled back.
            boolean written = Storage.getInstance().writeObject(KEY,
                    LocalHealthCodec.encode(getAllSamples()));
            if (!written) {
                Log.p("CN1 Health: the local store could not be written");
            }
            return written;
        } catch (RuntimeException ex) {
            Log.p("CN1 Health: could not persist the local store (" + ex
                    + ")");
            return false;
        }
    }
}
