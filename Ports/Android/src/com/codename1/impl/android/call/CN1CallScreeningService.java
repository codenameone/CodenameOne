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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;

import com.codename1.call.session.Calls;
import com.codename1.impl.call.CallWire;
import com.codename1.io.FileSystemStorage;
import com.codename1.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/// Screens incoming calls against the numbers the app installed.
///
/// Bound by the system only once the user has granted this app the call
/// screening role, which is why [#requestRole] exists and why
/// `DirectoryStatus.isEnabled()` answers false until they do.
///
/// #### The data is read from disk, not from memory
///
/// The system starts this service in its own right and the application may
/// not be running, so the numbers come from the file
/// `com.codename1.call.directory.CallDirectory` wrote. The same file backs
/// the iOS Call Directory extension, for the same reason.
public class CN1CallScreeningService extends CallScreeningService {

    private static volatile boolean enabled;

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
        Map<Long, Boolean> table = load();
        Boolean b = table.get(Long.valueOf(number));
        return b != null && b.booleanValue();
    }

    /// Reads the installed directory, once per process.
    private static synchronized Map<Long, Boolean> load() {
        if (blocked != null) {
            return blocked;
        }
        Map<Long, Boolean> table = new HashMap<Long, Boolean>();
        try {
            FileSystemStorage fs = FileSystemStorage.getInstance();
            String path = fs.getAppHomePath();
            if (!path.endsWith("/")) {
                path = path + "/";
            }
            path = path + "cn1calldirectory.tsv";
            if (fs.exists(path)) {
                InputStream in = fs.openInputStream(path);
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

    /// Whether the user has granted this app the screening role.
    public static boolean isEnabled() {
        return enabled;
    }

    /// Asks the user for the call screening role.
    ///
    /// `android.app.role.RoleManager` is API 29 and the port compiles against
    /// an older SDK, so it is reached reflectively for the same reason
    /// `AndroidVpnBridge` reaches `VpnManager` that way: naming it directly
    /// would mean raising the SDK the whole port builds against, to buy a
    /// capability that degrades perfectly well.
    public static void requestRole(Activity activity, final int requestId) {
        if (Build.VERSION.SDK_INT < 29) {
            Calls.deliverAck(requestId, false,
                    com.codename1.call.CallError.NOT_SUPPORTED.ordinal(),
                    "The call screening role needs Android 10 or newer");
            return;
        }
        try {
            Class<?> rmClass = Class.forName("android.app.role.RoleManager");
            // Every reflective result is tested with instanceof before it is
            // cast. ParparVM does not check CHECKCAST, so a cast that fails
            // there does not throw and cannot be caught -- and this file is
            // gated by scripts/check-cast-semantics.sh for exactly that. The
            // guards are worth having on their own terms too: a reflective
            // call that answers the wrong type should degrade, not crash.
            String role = asString(
                    rmClass.getField("ROLE_CALL_SCREENING").get(null));
            Object rm = activity.getSystemService(rmClass);
            if (rm == null || role == null) {
                unsupported(requestId);
                return;
            }
            if (!isTrue(rmClass.getMethod("isRoleAvailable", String.class)
                    .invoke(rm, role))) {
                unsupported(requestId);
                return;
            }
            if (isTrue(rmClass.getMethod("isRoleHeld", String.class)
                    .invoke(rm, role))) {
                enabled = true;
                Calls.deliverAck(requestId, true, 0, null);
                return;
            }
            Intent intent = asIntent(rmClass
                    .getMethod("createRequestRoleIntent", String.class)
                    .invoke(rm, role));
            if (intent == null) {
                unsupported(requestId);
                return;
            }
            com.codename1.impl.android.AndroidNativeUtil.startActivityForResult(
                    intent, new RoleResult(requestId));
        } catch (Exception e) {
            unsupported(requestId);
        }
    }

    /// A reflective answer as a `String`, or null when it is not one.
    ///
    /// The narrowing lives in its own method, outside any `try`, on purpose:
    /// ParparVM does not check CHECKCAST, so a cast that fails there does not
    /// throw and cannot be caught, and a cast sitting inside a broad handler
    /// is exactly what scripts/check-cast-semantics.sh reports.
    private static String asString(Object o) {
        if (o instanceof String) {
            return (String) o;
        }
        return null;
    }

    /// A reflective answer as an `Intent`, or null when it is not one.
    private static Intent asIntent(Object o) {
        if (o instanceof Intent) {
            return (Intent) o;
        }
        return null;
    }

    /// Whether a reflective answer is a `Boolean` that is true.
    private static boolean isTrue(Object o) {
        return o instanceof Boolean && ((Boolean) o).booleanValue();
    }

    private static void unsupported(int requestId) {
        Calls.deliverAck(requestId, false,
                com.codename1.call.CallError.NOT_SUPPORTED.ordinal(),
                "This device offers no call screening role");
    }

    /// Answers the role request once the user has decided.
    ///
    /// A named class rather than an anonymous one so it carries no synthetic
    /// reference to the activity, which outlives the dialog.
    private static final class RoleResult
            implements com.codename1.impl.android.IntentResultListener {
        private final int requestId;

        RoleResult(int requestId) {
            this.requestId = requestId;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode,
                Intent data) {
            enabled = resultCode == Activity.RESULT_OK;
            Calls.deliverAck(requestId, enabled,
                    com.codename1.call.CallError.UNAUTHORIZED.ordinal(),
                    "The user declined the call screening role");
        }
    }
}
