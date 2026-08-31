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
package com.codename1.call.directory;

import com.codename1.call.CallError;
import com.codename1.call.CallException;
import com.codename1.call.spi.CallBridge;
import com.codename1.impl.async.EdtResult;
import com.codename1.impl.call.CallRequests;
import com.codename1.impl.call.CallWire;
import com.codename1.io.FileSystemStorage;
import com.codename1.util.AsyncResource;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

/// Naming and blocking numbers, for calls that have nothing to do with this
/// app.
///
/// This is the caller-ID and spam-blocking feature: an ordinary cellular call
/// arrives, and the system asks the installed directories whether any of them
/// recognises the number.
///
/// ```java
/// DirectoryEntry[] entries = {
///     new DirectoryEntry(14155551212L, "Acme Support"),
///     new DirectoryEntry(14155559999L, null, true)     // blocked
/// };
/// CallDirectory.setEntries(entries).ready(v -> CallDirectory.reload());
/// ```
///
/// #### It is a different process, and it is not fast
///
/// On iOS the numbers are read by a separate app extension that the system
/// starts on its own schedule, with a tight memory limit and no access to
/// anything this app holds in memory. The data therefore has to be written to
/// a shared container before it can be used, which is what [#setEntries] does
/// -- and why installing it and asking the system to read it are two steps.
///
/// #### The user has to switch it on
///
/// On iOS caller identification is **off until the user enables this app** in
/// Settings, and nothing the app does can turn it on. Check
/// [DirectoryStatus#isEnabled()] before concluding that a load failed.
///
/// #### Referencing this package does not make an app a calling app
///
/// This is deliberately separate from `com.codename1.call.session`: a
/// caller-ID app carries no telephony permissions and no VoIP background
/// mode, because it never owns a call.
public final class CallDirectory {

    private CallDirectory() {
    }

    /// Whether this platform can install caller identification or blocking.
    public static boolean isSupported() {
        CallBridge b = CallRequests.bridge();
        return b != null && b.isDirectorySupported();
    }

    /// Writes `entries` where the system can read them, sorting them first.
    ///
    /// The sort is not a convenience. Both platforms reject an out-of-order
    /// list wholesale, naming no row, so doing it here removes a failure mode
    /// that is otherwise very hard to diagnose from the error the platform
    /// gives.
    ///
    /// Duplicate numbers are collapsed, keeping the first, because a
    /// duplicate is also grounds for rejection.
    public static AsyncResource<Boolean> setEntries(DirectoryEntry[] entries) {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            EdtResult<Boolean> r = new EdtResult<Boolean>();
            r.error(new CallException(CallError.NOT_SUPPORTED));
            return r;
        }
        // The write AND the handoff, together. The file has one canonical
        // name -- the Android screening service reads exactly that path, and
        // the iOS port copies from it into the App Group -- so releasing the
        // lock in between let a second caller replace it before the port had
        // consumed the first caller's, and both requests then described the
        // second caller's entries. The port consumes the file inside this
        // call, so the lock is held only for that.
        synchronized (WRITE_LOCK) {
            String path;
            try {
                path = writeLocked(entries == null
                        ? new DirectoryEntry[0] : entries);
            } catch (IOException e) {
                EdtResult<Boolean> r = new EdtResult<Boolean>();
                r.error(new CallException(CallError.DIRECTORY_FAILED,
                        "Could not stage the directory: " + e.getMessage(),
                        e));
                return r;
            }
            int id = CallRequests.nextId();
            EdtResult<Boolean> r = CallRequests.openAck(id);
            b.setDirectorySource(id, path);
            return r;
        }
    }

    /// Asks the system to re-read what [#setEntries] installed.
    public static AsyncResource<Boolean> reload() {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            EdtResult<Boolean> r = new EdtResult<Boolean>();
            r.error(new CallException(CallError.NOT_SUPPORTED));
            return r;
        }
        int id = CallRequests.nextId();
        EdtResult<Boolean> r = CallRequests.openAck(id);
        b.reloadDirectory(id);
        return r;
    }

    /// Asks the system what it currently thinks of this app's directory.
    public static AsyncResource<DirectoryStatus> getStatus() {
        final EdtResult<DirectoryStatus> out = new EdtResult<DirectoryStatus>();
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            out.error(new CallException(CallError.NOT_SUPPORTED));
            return out;
        }
        int id = CallRequests.nextId();
        EdtResult<String> raw = CallRequests.openString(id);
        raw.onResult(new StatusDecoder(out));
        b.getDirectoryStatus(id);
        return out;
    }

    /// Asks the user to let this app screen incoming calls.
    ///
    /// Android only; on iOS the equivalent is the user enabling the app in
    /// Settings, which an app cannot prompt for. Resolves false where the
    /// role was refused or does not exist.
    public static AsyncResource<Boolean> requestScreeningRole() {
        CallBridge b = CallRequests.bridge();
        if (b == null) {
            EdtResult<Boolean> r = new EdtResult<Boolean>();
            r.error(new CallException(CallError.NOT_SUPPORTED));
            return r;
        }
        int id = CallRequests.nextId();
        EdtResult<Boolean> r = CallRequests.openAck(id);
        b.requestScreeningRole(id);
        return r;
    }

    /// Serialises writers. Two concurrent setEntries calls opened the same
    /// staging path, so the second truncated the first's file and either
    /// could rename it into place half-written -- which is what the staging
    /// was added to make impossible.
    private static final Object WRITE_LOCK = new Object();

    /// Sorts, de-duplicates and writes the entries, answering with the path.
    private static String writeLocked(DirectoryEntry[] entries)
            throws IOException {
        DirectoryEntry[] sorted = new DirectoryEntry[entries.length];
        System.arraycopy(entries, 0, sorted, 0, entries.length);
        Arrays.sort(sorted, new ByNumber());
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String path = fs.getAppHomePath();
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        path = path + "cn1calldirectory.tsv";
        // Written beside the live file and moved into place, because the
        // reader is ANOTHER PROCESS: Android starts the screening service to
        // judge a call whenever one arrives, including while this list is
        // being written. Opening the live path truncates it first, so a
        // screening that landed in the middle read an empty or half-written
        // directory and let a number through that both the old list and the
        // new one blocked -- and invalidating the cache afterwards cannot
        // take back a call that has already rung.
        String staging = path + ".new";
        OutputStream os = fs.openOutputStream(staging);
        try {
            StringBuilder sb = new StringBuilder();
            long previous = -1;
            for (DirectoryEntry entry : sorted) {
                if (entry == null || entry.getNumber() == previous) {
                    continue;
                }
                previous = entry.getNumber();
                sb.append(previous).append(CallWire.SEPARATOR)
                        .append(CallWire.sanitize(entry.getLabel()))
                        .append(CallWire.SEPARATOR)
                        .append(CallWire.flagOf(entry.isBlocked()))
                        .append('\n');
            }
            os.write(sb.toString().getBytes("UTF-8"));
        } finally {
            os.close();
        }
        // The move, WITHOUT deleting first. rename() takes a name rather than
        // a path, and on the platform where this matters it is File.renameTo,
        // which replaces the destination in one step -- so a screening that
        // lands mid-replacement sees either the whole old list or the whole
        // new one. Removing the old file first reintroduced the very window
        // the staging was added to close, a moment in which a number the
        // replacement blocks is allowed through.
        fs.rename(staging, "cn1calldirectory.tsv");
        if (fs.exists(staging)) {
            // A platform whose rename cannot replace. Nothing else is
            // available there, and no such platform runs the reader in
            // another process.
            fs.delete(path);
            fs.rename(staging, "cn1calldirectory.tsv");
            if (fs.exists(staging) || !fs.exists(path)) {
                // CHECKED, because this branch has already deleted the live
                // list. A rename that fails here leaves no directory at all,
                // and returning the path regardless had the bridge
                // acknowledge success and drop its cache -- after which every
                // screened call consulted an empty list and numbers the app
                // had blocked were allowed straight through, silently and for
                // as long as the app kept running. Failing is the only honest
                // answer; setEntries reports it and the app can retry.
                throw new IOException("The call directory could not be"
                        + " published: the replacement is still at " + staging);
            }
        }
        return path;
    }

    /// Orders entries by number ascending, which is what both platforms
    /// require. A named class rather than an anonymous one so it carries no
    /// synthetic outer reference.
    private static final class ByNumber implements Comparator<DirectoryEntry> {
        @Override
        public int compare(DirectoryEntry a, DirectoryEntry b) {
            long x = a == null ? 0 : a.getNumber();
            long y = b == null ? 0 : b.getNumber();
            return x < y ? -1 : (x > y ? 1 : 0);
        }
    }

    /// Turns the port's status record into a [DirectoryStatus].
    private static final class StatusDecoder
            implements com.codename1.util.AsyncResult<String> {
        private final EdtResult<DirectoryStatus> out;

        StatusDecoder(EdtResult<DirectoryStatus> out) {
            this.out = out;
        }

        @Override
        public void onReady(String value, Throwable error) {
            if (error != null) {
                out.error(error);
                return;
            }
            String[] f = CallWire.split(value);
            out.complete(new DirectoryStatus(CallWire.flag(f, 0),
                    CallWire.integer(f, 1, -1), CallWire.field(f, 2)));
        }
    }

    /// Answers [#getStatus] with a wire record.
    ///
    /// @hidden not part of the public API.
    public static void deliverStatus(int requestId, String statusWire) {
        EdtResult<String> r = CallRequests.takeString(requestId);
        if (r != null) {
            r.complete(statusWire == null ? "" : statusWire);
        }
    }

    /// Fails a status request.
    ///
    /// @hidden not part of the public API.
    public static void deliverStatusFailed(int requestId, int errorOrdinal,
            String message) {
        EdtResult<String> r = CallRequests.takeString(requestId);
        if (r != null) {
            r.error(CallWire.decodeError(errorOrdinal, message));
        }
    }

    /// No static state to clear; present so the family's reset is uniform.
    ///
    /// @hidden not part of the public API; test-only.
    public static void resetForTest() {
    }
}
