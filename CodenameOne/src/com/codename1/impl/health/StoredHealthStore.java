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

    /// The last blob known to be on disk.
    ///
    /// Held because `Storage.writeObject` *deletes* the entry when the
    /// write fails -- so a failed save does not leave the previous
    /// contents in place, it destroys them. Without this, one full disk
    /// took every older record with it while the caller was told only
    /// that the latest write had failed.
    private String lastGood;

    public StoredHealthStore() {
        restore();
    }

    /// Set when the entry exists but this process could not read it.
    ///
    /// Writing while it is set would replace history the store cannot see.
    private boolean unreadable;

    private void restore() {
        String blob = null;
        // Asked before the read, because Storage.readObject cannot tell us
        // afterwards: it catches its own failures and answers null, so a
        // corrupt or briefly unreadable entry looks exactly like no entry at
        // all. Restoring empty on the second is right; on the first it wiped
        // the user's history, because the next write replaced the same key
        // with only the samples this session happened to add.
        // Unknown counts as "there might be something there". A backend that
        // throws on the existence check leaves us unable to tell an empty
        // store from a full one, and readObject cannot settle it either --
        // it runs the same check internally, catches the same failure and
        // answers null. Treating that as confirmed absence meant a storage
        // layer that recovered before the next write let that write replace
        // an entry we had never managed to look at.
        boolean had = true;
        try {
            had = Storage.getInstance().exists(KEY);
        } catch (RuntimeException ex) {
            Log.p("CN1 Health: could not check whether the local store"
                    + " exists, assuming it does (" + ex + ")");
        }
        try {
            Object stored = Storage.getInstance().readObject(KEY);
            if (stored instanceof String) {
                blob = (String) stored;
            }
        } catch (RuntimeException ex) {
            Log.p("CN1 Health: could not read the local store (" + ex + ")");
        }
        if (had && blob == null) {
            // There is history here and this process cannot see it. Reads
            // answer from an empty store, which is unavoidable, but writing
            // is refused so the unreadable entry is left intact for a later
            // launch -- or for whatever tool the user has to recover it.
            // persist() already fails the write and rolls the change back,
            // so the caller is told rather than being handed a success that
            // destroyed data.
            unreadable = true;
            Log.p("CN1 Health: the local store exists but could not be read;"
                    + " writes are refused so the existing data is not"
                    + " overwritten");
        }
        List<HealthSample> restored = LocalHealthCodec.decode(blob);
        for (HealthSample s : restored) {
            addSampleDirect(s);
        }
        lastGood = blob;
        // Only after the restore, so the writes it performs do not each
        // rewrite the file they are being read from.
        loaded = true;
    }

    @Override
    protected boolean persist() {
        if (!loaded) {
            return true;
        }
        if (unreadable) {
            return false;
        }
        String blob = null;
        try {
            // The return value matters: Storage reports a full or
            // unwritable store by answering false rather than throwing,
            // and ignoring it let a write be acknowledged as durable when
            // it only ever reached memory. The caller sees the failure
            // now, and the change is rolled back.
            blob = LocalHealthCodec.encode(getAllSamples());
            if (writeBlob(blob)) {
                lastGood = blob;
                return true;
            }
        } catch (RuntimeException ex) {
            Log.p("CN1 Health: could not persist the local store (" + ex
                    + ")");
        }
        restoreLastGood();
        return false;
    }

    /// The single call that touches storage.
    ///
    /// Isolated so the failure this class exists to survive can be
    /// reproduced in a test: `Storage.writeObject` deletes the entry and
    /// answers false when it cannot write, and that combination is the
    /// whole reason the previous blob has to be put back.
    protected boolean writeBlob(String blob) {
        return Storage.getInstance().writeObject(KEY, blob);
    }

    /// Puts the previous contents back after a failed save.
    ///
    /// `Storage.writeObject` deletes the entry when it fails, so by the
    /// time it answers false the older data is already gone -- rolling
    /// the in-memory change back is not enough on its own, and one full
    /// disk would otherwise cost every record the app had ever written
    /// rather than the one write that failed.
    private void restoreLastGood() {
        if (lastGood == null) {
            return;
        }
        try {
            if (writeBlob(lastGood)) {
                Log.p("CN1 Health: the local store could not be written;"
                        + " the previous contents were put back");
                return;
            }
        } catch (RuntimeException ex) {
            // Falls through to the warning below.
        }
        Log.p("CN1 Health: the local store could not be written and the"
                + " previous contents could not be put back either; the"
                + " app's health data is now only in memory");
    }
}
