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
package com.codename1.impl.continuity;

import com.codename1.continuity.spi.ContinuityBridge;
import com.codename1.continuity.spi.ContinuityCallback;
import com.codename1.io.Log;
import com.codename1.io.Storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A simulated continuity platform, used by the simulator, the desktop builds and the unit tests.
///
/// A simulation rather than nothing, for the reason the call and nearby bridges carry one: almost
/// everything an app does with continuity -- deciding what belongs in the payload, prompting before
/// a jump, rebuilding a screen from a route -- has nothing to do with the operating system that
/// carries the state, and a port that reported nothing would make all of it testable only on a
/// pair of phones.
///
/// It keeps the last published activity in memory so the Simulate menu can show what the app is
/// offering, and it can hand that activity straight back through `simulateArrival()` -- which is
/// what "continue this on another device" is, minus the second device. The synced store is real
/// within one machine: it is backed by `com.codename1.io.Storage`, so it survives a simulator
/// restart the way the platform store survives a device one.
///
/// Storage rather than Preferences, and not as a detail. Preferences.set() fills an in-memory
/// table whose save() discards the write's result, and Preferences.get() reads that table -- so a
/// value that never reached the disk reads back correctly right up until the next launch, and a
/// simulation that reported success for it would be teaching an application something false about
/// the device.
public class LocalContinuityBridge implements ContinuityBridge {
    /// Prefix for the simulated synced store's keys inside `Storage`.
    private static final String PREFIX = "CN1$SyncedStore$";

    /// The list of keys, kept beside them because the store is addressed by name only.
    ///
    /// A SEPARATE namespace from the values, which is what makes it safe: PREFIX ends in `$` and
    /// this does not, so no application key can ever be written to this name. Review read
    /// `PREFIX + "Keys"` as landing here -- it produces `CN1$SyncedStore$Keys`, which is a value
    /// like any other -- and the reasoning behind the answer is worth more than the answer: for a
    /// collision to exist INDEX would have to start with PREFIX, and it does not.
    ///
    /// That is the property to preserve. Dropping the `$` from PREFIX, or renaming this to
    /// something under it, would make `put("Keys", ...)` overwrite the index and then be
    /// overwritten by it -- reported as a successful write whose value reads back as the key
    /// list.
    private static final String INDEX = "CN1$SyncedStoreKeys";

    // EDT-owned. Everything here runs on the Codename One event thread: the framework calls in
    // from there, and the simulator's "Simulate ->" items reach this class through
    // SimulatorHookLoader, which dispatches every hook with Display.callSeriallyAndWait.
    //
    // The synced store below rests on the same assumption, and SyncedStore now says so where an
    // application can read it: the key index is a second stored value, so two threads writing
    // different new keys would each read it, add one key, and write it back over the other. That
    // is answered by the toolkit's threading model rather than by a lock in it -- see the
    // Threading section on SyncedStore.
    private ContinuityCallback callback;
    private String publishedType;
    private String publishedTitle;
    private Map<String, Object> publishedInfo;

    @Override
    public void setCallback(ContinuityCallback c) {
        callback = c;
    }

    @Override
    public boolean isContinuationSupported() {
        return true;
    }

    @Override
    public void publishContinuation(String activityType, String title,
            Map<String, Object> userInfo) {
        Map<String, Object> copy = userInfo == null
                ? null : new HashMap<String, Object>(userInfo);
        publishedType = activityType;
        publishedTitle = title;
        publishedInfo = copy;
    }

    @Override
    public void clearContinuation() {
        publishedType = null;
        publishedTitle = null;
        publishedInfo = null;
    }

    /// The activity type currently advertised, or null when nothing is.
    ///
    /// #### Returns
    ///
    /// the type
    public String getPublishedType() {
        return publishedType;
    }

    /// The label currently advertised, or null.
    ///
    /// #### Returns
    ///
    /// the label
    public String getPublishedTitle() {
        return publishedTitle;
    }

    /// The payload currently advertised, or null when nothing is.
    ///
    /// #### Returns
    ///
    /// a copy of the payload
    public Map<String, Object> getPublishedInfo() {
        return publishedInfo == null ? null : new HashMap<String, Object>(publishedInfo);
    }

    /// Delivers the currently advertised activity back to the app as though it had arrived from
    /// another device, which is what the Simulate menu's "continue on this device" does.
    ///
    /// The device id inside the payload is rewritten first. Without that the framework would
    /// recognize the state as this device's own echo and correctly ignore it, and the menu item
    /// would appear to do nothing.
    ///
    /// #### Returns
    ///
    /// true when there was an activity to deliver and the app claimed it
    public boolean simulateArrival() {
        if (publishedType == null || publishedInfo == null) {
            return false;
        }
        Map<String, Object> copy = new HashMap<String, Object>(publishedInfo);
        copy.put("device", "simulated-device");
        return simulateArrival(publishedType, copy);
    }

    /// Delivers an arbitrary activity, for tests that build their own.
    ///
    /// #### Parameters
    ///
    /// - `activityType`: the type it arrives under
    /// - `userInfo`: the payload
    ///
    /// #### Returns
    ///
    /// true when the app claimed it
    public boolean simulateArrival(String activityType, Map<String, Object> userInfo) {
        ContinuityCallback c = callback;
        if (c == null) {
            return false;
        }
        try {
            return c.continuationReceived(activityType, userInfo);
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    // ------------------------------------------------------------------
    // Synced store
    // ------------------------------------------------------------------

    @Override
    public boolean isSyncedStoreSupported() {
        return true;
    }

    @Override
    public boolean syncedStorePut(String key, String value) {
        // Storage, not Preferences, and the difference is the whole point of this method's
        // return. Preferences.set() fills an in-memory table and its save() DISCARDS
        // Storage.writeObject()'s result, so a write that never reached the disk leaves the new
        // value in that table -- and Preferences.get() reads the table. The read-back below used
        // to consult the cache it had just written and agree with itself, so put() reported
        // success for a value that disappears when the simulator restarts. An oversized value
        // makes it deterministic rather than a full-disk curiosity.
        //
        // Same correction the sequence counter and the delivery marks already needed. The
        // simulation has to answer the question the device answers -- is the value there now --
        // and only a checked write can.
        if (!write(storageName(key), value)) {
            return false;
        }
        List<String> keys = indexKeys();
        if (!keys.contains(key)) {
            keys.add(key);
            if (!writeIndex(keys)) {
                // The value is stored and the index is not, so keys() would not list it. Rolled
                // BACK rather than merely reported: leaving it made "false" a lie in the other
                // direction -- the caller takes its documented fallback path while get() returns
                // the value it was told had failed, keys() omits it, and clearing the store
                // cannot reach it.
                //
                // A failed write should leave nothing behind, which is the only answer that means
                // one thing.
                //
                // VERIFIED, for the same reason syncedStoreRemove() verifies: an unchecked delete
                // made the rollback claim a cleanup it had not performed. When it cannot be
                // performed there is nothing further this simulation can do -- the index write
                // that would have listed the value is the one that just failed -- so it is
                // logged rather than passed over, because the store is then in the one state
                // this class works to avoid.
                if (!deleteValue(storageName(key))) {
                    Log.p("Continuity synced store: the value for a key whose index write failed "
                            + "could not be deleted either, so it stays readable through get() "
                            + "while keys() does not list it: " + key);
                }
                return false;
            }
        }
        return true;
    }

    /// The storage name for an application key, encoded so that distinct keys cannot collide.
    ///
    /// Storage normalizes `/`, `\\`, `%`, `?`, `*`, `:` and `=` to `_` in a file name, so
    /// "a/b" and "a_b" addressed the SAME value: both writes reported success, the index listed
    /// both keys, and either read returned whichever was written last while removing one deleted
    /// the other. That arrived with the move off Preferences -- which has no such rule -- so it
    /// is a defect this class introduced while fixing a different one, not an old one.
    ///
    /// The storage name for one application key.
    ///
    /// A WHITELIST, not a list of characters to avoid. Everything outside `[a-z0-9_-]` becomes
    /// `$` and four hex digits, so the name is pure ASCII made of characters no filesystem
    /// rewrites -- and the simulation stops depending on which folding, normalisation or
    /// reserved-name rules the host happens to apply.
    ///
    /// The list it replaces grew by one entry per review round and was wrong every time in a new
    /// way: uppercase letters, because macOS and Windows fold case; a trailing dot or space,
    /// because Windows trims them; `<`, `>`, `"` and `|`, because Windows refuses them; and then
    /// canonically equivalent Unicode, because a decomposed and a precomposed accent can name one
    /// file. Every one of those merged two distinct keys into a single value while the index went
    /// on listing both -- both reads answering with the last write, and removing either removing
    /// the other's value. A simulation that merges two keys is worse than no simulation: it looks
    /// like it works. There is no reason to believe that list was finally complete, and with a
    /// whitelist the question does not arise.
    ///
    /// The mapping is injective, which is what makes it collision-free: `$` is itself escaped, so
    /// an escape group can never be produced by literal characters.
    ///
    /// PREFIX is left alone. It is a constant, identical in every name, so nothing about it can
    /// distinguish one key from another -- and INDEX still cannot be reached from here, for the
    /// reason its own comment gives.
    private static String storageName(String key) {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-') {
                sb.append(c);
                continue;
            }
            sb.append('$');
            String hex = Integer.toHexString(c).toUpperCase();
            for (int pad = hex.length(); pad < 4; pad++) {
                sb.append('0');
            }
            sb.append(hex);
        }
        if (sb.length() <= MAX_NAME_CHARS) {
            return sb.toString();
        }
        // Escaping can multiply a key's length by five, and a filesystem will not take a name of
        // any length. Truncated and then made unique again by a hash of the WHOLE key, so two
        // long keys sharing a prefix still address different files.
        return sb.toString().substring(0, MAX_NAME_CHARS) + "$$" + hash(key);
    }

    /// The most characters a storage name may use before it is truncated and hashed. Well inside
    /// what every filesystem this simulation runs on accepts.
    private static final int MAX_NAME_CHARS = 120;

    /// FNV-1a, 64 bit, as 16 hex digits. Only ever used to keep two truncated names apart.
    ///
    /// Formatted through Integer.toHexString on each half rather than Long.toHexString, and
    /// String.substring rather than StringBuilder.substring, because core is compiled a second
    /// time against Ports/CLDC11 and translated against vm/JavaAPI -- neither of which defines
    /// those two. The Maven build compiles against the full JDK and accepts them, so the mistake
    /// only appears in the Ant leg.
    private static String hash(String key) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < key.length(); i++) {
            h ^= key.charAt(i);
            h *= 0x100000001b3L;
        }
        return hex32((int) (h >>> 32)) + hex32((int) h);
    }

    /// One 32-bit half as exactly eight uppercase hex digits.
    private static String hex32(int value) {
        String hex = Integer.toHexString(value).toUpperCase();
        StringBuilder out = new StringBuilder();
        for (int pad = hex.length(); pad < 8; pad++) {
            out.append('0');
        }
        out.append(hex);
        return out.toString();
    }

    /// Writes one value, reporting whether it actually reached storage.
    private boolean write(String name, String value) {
        try {
            return Storage.getInstance().writeObject(name, value);
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    /// Reads one value, or null for anything that is not a stored string.
    private String read(String name) {
        try {
            if (!Storage.getInstance().exists(name)) {
                return null;
            }
            Object o = Storage.getInstance().readObject(name);
            // instanceof rather than a cast: a failed cast does not throw on the iOS virtual
            // machine, and this class is compiled into every port.
            return o instanceof String ? (String) o : null;
        } catch (Throwable t) {
            Log.e(t);
            return null;
        }
    }

    @Override
    public String syncedStoreGet(String key) {
        return read(storageName(key));
    }

    /// Deletes one stored value and reports whether it is DEFINITELY gone.
    ///
    /// The index and the values are two writes, and every caller here has to know which of them
    /// happened. Dropping the index entry for a value the delete failed to remove leaves the old
    /// value readable through get() while keys() omits it and clearing the store cannot reach it
    /// -- a value with no way to see it and no way to remove it.
    ///
    /// When the check itself fails the answer is "still there", which is the safe direction: an
    /// index entry for a value that has gone shows up as a key whose get() answers the default,
    /// and an application can see that and cope. The other way round is invisible.
    private boolean deleteValue(String name) {
        try {
            Storage.getInstance().deleteStorageFile(name);
        } catch (Throwable t) {
            Log.e(t);
        }
        try {
            return !Storage.getInstance().exists(name);
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    @Override
    public void syncedStoreRemove(String key) {
        if (!deleteValue(storageName(key))) {
            // The value is still readable, so the index keeps its entry. Reporting a key whose
            // value is still there is the truth; dropping it would hide a value that get() goes
            // on returning.
            return;
        }
        List<String> keys = indexKeys();
        if (keys.remove(key) && !writeIndex(keys)) {
            // The value is gone and the index still lists it. syncedStoreKeys() filters that out
            // so keys() stays truthful for the rest of the process, and this says so once rather
            // than leaving a durable index that disagrees with the store entirely unremarked.
            Log.p("Continuity synced store: the value for \"" + key + "\" was removed but the "
                    + "index could not be rewritten, so the stored index still lists it until a "
                    + "later write succeeds.");
        }
    }

    @Override
    public String[] syncedStoreKeys() {
        // FILTERED by what is actually stored, because the index is a second write and can be
        // left describing a value that is not there: a delete that succeeded and an index write
        // that then failed leaves the key listed while get() answers the default. The platform
        // this simulates has no such gap -- NSUbiquitousKeyValueStore enumerates its own
        // dictionary, so a phantom key cannot exist there -- and the simulation should not
        // invent one.
        //
        // Only a POSITIVE absence removes a key. When the check cannot be made the entry stays,
        // which is the same direction deleteValue() chose and for the same reason: a listed key
        // whose get() answers the default is visible and survivable, and a value nothing lists
        // is neither.
        List<String> keys = indexKeys();
        List<String> present = new ArrayList<String>();
        for (String key : keys) {
            if (definitelyAbsent(storageName(key))) {
                continue;
            }
            present.add(key);
        }
        return present.toArray(new String[present.size()]);
    }

    /// Whether this storage name is known NOT to hold a value. False when it does, and false
    /// when the question could not be answered.
    private boolean definitelyAbsent(String name) {
        try {
            return !Storage.getInstance().exists(name);
        } catch (Throwable t) {
            Log.e(t);
            return false;
        }
    }

    /// Reports a change made "on another device", which the Simulate menu uses to exercise an
    /// app's `SyncedStoreListener` without a second machine.
    public void simulateStoreChange() {
        ContinuityCallback c = callback;
        if (c == null) {
            return;
        }
        try {
            c.syncedStoreChanged();
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    private List<String> indexKeys() {
        List<String> keys = new ArrayList<String>();
        String raw = read(INDEX);
        if (raw == null || raw.length() == 0) {
            return keys;
        }
        // Newline separated AND escaped. The separator alone was not enough: a key containing a
        // newline is one this API accepts -- the platform store imposes no such rule, so neither
        // does the simulation -- and it came back from here as two phantom keys that nothing
        // could then remove.
        int start = 0;
        while (start <= raw.length()) {
            int end = raw.indexOf('\n', start);
            if (end < 0) {
                end = raw.length();
            }
            String key = unescapeIndexEntry(raw.substring(start, end));
            if (key.length() > 0 && !keys.contains(key)) {
                keys.add(key);
            }
            start = end + 1;
        }
        return keys;
    }

    /// Escapes a key for the newline-separated index: backslash first, then the separator.
    private static String escapeIndexEntry(String key) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c == '\\') {
                sb.append("\\\\");
            } else if (c == '\n') {
                sb.append("\\n");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /// Reverses `escapeIndexEntry`.
    private static String unescapeIndexEntry(String entry) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entry.length(); i++) {
            char c = entry.charAt(i);
            if (c == '\\' && i + 1 < entry.length()) {
                char next = entry.charAt(i + 1);
                if (next == 'n') {
                    sb.append('\n');
                    i++;
                    continue;
                }
                if (next == '\\') {
                    sb.append('\\');
                    i++;
                    continue;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /// Writes the key index, reporting whether it reached storage.
    ///
    /// The answer is used rather than logged: a value stored under a key the index has lost is
    /// findable by name and invisible to keys(), and a caller told its write succeeded has been
    /// told something that is only half true.
    private boolean writeIndex(List<String> keys) {
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(escapeIndexEntry(key));
        }
        return write(INDEX, sb.toString());
    }
}
