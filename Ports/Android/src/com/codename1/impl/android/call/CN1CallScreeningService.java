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
package com.codename1.impl.android.call;

import android.content.Context;
import android.telecom.Call;
import android.telecom.CallScreeningService;

import com.codename1.impl.call.CallWire;
import com.codename1.util.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/// Screens incoming calls against the numbers the app installed.
///
/// Bound by the system only once the user has granted this app the call
/// screening role, which is why [CallScreeningRole] exists and why
/// `DirectoryStatus.isEnabled()` answers false until they do.
///
/// #### The data is read from disk, not from memory
///
/// The system starts this service in its own right and the application may
/// not be running, so the numbers come from the file
/// `com.codename1.call.directory.CallDirectory` wrote. The same file backs
/// the iOS Call Directory extension, for the same reason.
public class CN1CallScreeningService extends CallScreeningService {

    private static Map<Long, Boolean> blocked;

    @Override
    public void onScreenCall(Call.Details details) {
        CallResponse.Builder response = new CallResponse.Builder();
        long number = numberOf(details);
        if (number > 0 && isBlocked(number)) {
            response.setDisallowCall(true)
                    .setRejectCall(true)
                    .setSkipCallLog(false)
                    .setSkipNotification(true);
        }
        respondToCall(details, response.build());
    }

    private static long numberOf(Call.Details details) {
        if (details == null || details.getHandle() == null) {
            return -1;
        }
        String raw = details.getHandle().getSchemeSpecificPart();
        if (raw == null) {
            return -1;
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }
        if (digits.length() == 0) {
            return -1;
        }
        try {
            return Long.parseLong(digits.toString());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean isBlocked(long number) {
        Map<Long, Boolean> table = load(this);
        Boolean b = table.get(Long.valueOf(number));
        return b != null && b.booleanValue();
    }

    /// Reads the installed directory, once per process.
    ///
    /// Through the SERVICE's own Context rather than FileSystemStorage.
    /// Android starts this service in a cold process to screen a call, which
    /// is exactly the case where Display.init has not run -- and
    /// getAppHomePath() then dereferences a null implementation and throws an
    /// NPE that the IOException handler below does not catch. onScreenCall
    /// never reached respondToCall, so a number the user had blocked rang
    /// every time screening woke the app from stopped.
    ///
    /// It is the same file: getAppHomePath() on this platform is
    /// getFilesDir() with a file: prefix, so CallDirectory's writer and this
    /// reader address one path by two routes.
    private static synchronized Map<Long, Boolean> load(Context context) {
        if (blocked != null) {
            return blocked;
        }
        Map<Long, Boolean> table = new HashMap<Long, Boolean>();
        try {
            File file = new File(context.getFilesDir(),
                    "cn1calldirectory.tsv");
            if (file.exists()) {
                InputStream in = new FileInputStream(file);
                try {
                    StringBuilder sb = new StringBuilder();
                    byte[] buf = new byte[4096];
                    int r;
                    while ((r = in.read(buf)) > 0) {
                        sb.append(new String(buf, 0, r, "UTF-8"));
                    }
                    for (String line : StringUtil.tokenize(sb.toString(), '\n')) {
                        String[] f = CallWire.split(line);
                        String n = CallWire.field(f, 0);
                        if (n.length() == 0) {
                            continue;
                        }
                        try {
                            table.put(Long.valueOf(Long.parseLong(n.trim())),
                                    Boolean.valueOf(CallWire.flag(f, 2)));
                        } catch (NumberFormatException ignored) {
                            // One unusable row must not lose the rest of the
                            // list, which is routinely six figures long.
                            continue;
                        }
                    }
                } finally {
                    in.close();
                }
            }
        } catch (IOException e) {
            // A directory that cannot be read screens nothing, which is the
            // safe failure: letting a call through is recoverable and
            // blocking every call is not.
        }
        blocked = table;
        return table;
    }

    /// Forgets the cached directory, after the app installs a new one.
    public static synchronized void invalidate() {
        blocked = null;
    }
}
