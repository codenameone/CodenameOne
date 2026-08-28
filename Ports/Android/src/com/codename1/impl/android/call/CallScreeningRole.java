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
import android.content.Intent;
import android.os.Build;

import android.content.Context;

import com.codename1.call.session.Calls;

import java.util.ArrayList;
import java.util.List;

/// The call screening ROLE, and nothing that touches `CallScreeningService`.
///
/// Split out of [CN1CallScreeningService] because that class extends
/// `android.telecom.CallScreeningService`, which arrives in API 24. Naming it
/// resolves it, and resolving a class whose superclass is absent is a
/// NoClassDefFoundError -- so a plain `Calls.getGrantedPermissions()` on an
/// API 19 to 23 device crashed on the role check, before the SDK guard inside
/// the method it was calling could run. A guard cannot protect the class that
/// declares it.
///
/// The floor that made this reachable is deliberate: the call packages carry
/// no `androidMinimumSdk`, because every one of these capabilities is a
/// runtime query an app is meant to degrade around rather than an install
/// requirement. That only holds if nothing here forces a class to load on a
/// device that has no such class, which is what this split is for.
///
/// Everything below reaches `android.app.role.RoleManager` reflectively -- it
/// is API 29, and the port compiles against an older SDK.
class CallScreeningRole {

    private static volatile boolean enabled;

    private CallScreeningRole() {
    }

    /// Whether the user has granted this app the screening role.
    ///
    /// Kept for the in-process case; [#isRoleHeld] is what a status query
    /// should use, because this flag is false in any process that did not
    /// itself run the request.
    public static boolean isEnabled() {
        return enabled;
    }

    /// Asks Android whether this app currently holds the screening role.
    ///
    /// The authority, rather than the static flag above: the role may have
    /// been granted in an earlier process or from Settings, and either way
    /// the flag starts false -- so a status query that trusted it reported
    /// screening disabled while Android was binding the service.
    ///
    /// @param context any context
    /// @return true when the role is held
    public static boolean isRoleHeld(Context context) {
        if (Build.VERSION.SDK_INT < 29 || context == null) {
            return false;
        }
        try {
            Class<?> rmClass = Class.forName("android.app.role.RoleManager");
            Object role = rmClass.getField("ROLE_CALL_SCREENING").get(null);
            Object rm = context.getSystemService(rmClass);
            if (rm == null || !(role instanceof String)) {
                return false;
            }
            boolean held = isTrue(rmClass.getMethod("isRoleHeld", String.class)
                    .invoke(rm, role));
            enabled = held;
            return held;
        } catch (Exception e) {
            return false;
        }
    }

    /// Asks the user for the call screening role.
    ///
    /// `android.app.role.RoleManager` is API 29 and the port compiles against
    /// an older SDK, so it is reached reflectively for the same reason
    /// `AndroidVpnBridge` reaches `VpnManager` that way: naming it directly
    /// would mean raising the SDK the whole port builds against, to buy a
    /// capability that degrades perfectly well.
    /// Requests the role and runs `whenDone` once the user has decided.
    ///
    /// The permission API needs the OUTCOME rather than an acknowledgement,
    /// because Calls.requestPermissions answers with a mask that has to
    /// include the role by the time it is delivered.
    public static void requestRole(Activity activity, Runnable whenDone) {
        requestRole(activity, -1, whenDone);
    }

    public static void requestRole(Activity activity, final int requestId) {
        requestRole(activity, requestId, null);
    }

    private static void requestRole(Activity activity, final int requestId,
            final Runnable whenDone) {
        if (Build.VERSION.SDK_INT < 29) {
            if (requestId >= 0) {
                // Below API 29 the role does not exist, which the contract
                // calls a false rather than a failure.
                Calls.deliverAckValue(requestId, false);
            }
            if (whenDone != null) {
                whenDone.run();
            }
            return;
        }
        // Filled only when the prompt could not be launched; see below.
        List<RoleResult> orphaned = new ArrayList<RoleResult>();
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
                finish(requestId, whenDone, false);
                return;
            }
            if (!isTrue(rmClass.getMethod("isRoleAvailable", String.class)
                    .invoke(rm, role))) {
                finish(requestId, whenDone, false);
                return;
            }
            if (isTrue(rmClass.getMethod("isRoleHeld", String.class)
                    .invoke(rm, role))) {
                enabled = true;
                if (requestId >= 0) {
                    Calls.deliverAck(requestId, true, 0, null);
                }
                if (whenDone != null) {
                    whenDone.run();
                }
                return;
            }
            Intent intent = asIntent(rmClass
                    .getMethod("createRequestRoleIntent", String.class)
                    .invoke(rm, role));
            if (intent == null) {
                finish(requestId, whenDone, false);
                return;
            }
            // One dialog at a time. CodenameOneActivity keeps a SINGLE
            // result listener and setIntentResultListener ignores a
            // replacement while it is waiting, so a second request started
            // over the first left one of them in CallRequests for ever with
            // no error anywhere.
            synchronized (CallScreeningRole.class) {
                if (rolePending) {
                    // QUEUED behind the prompt, not answered now. Answering
                    // immediately reported the role as absent -- or BUSY --
                    // while the very prompt that grants it was still on
                    // screen, so a second caller was told "denied" moments
                    // before the user granted it. That is the premature
                    // answer this whole sequence exists to avoid, arrived at
                    // through the one path that does not block: requestRole
                    // returns as soon as the dialog is up, so the permission
                    // lock is already released by the time the second caller
                    // gets here.
                    //
                    // Added under the monitor the result drains under, so a
                    // waiter cannot be parked after the drain has run.
                    ROLE_WAITERS.add(new RoleResult(requestId, whenDone));
                    return;
                }
                rolePending = true;
            }
            try {
                com.codename1.impl.android.AndroidNativeUtil
                        .startActivityForResult(intent,
                                new RoleResult(requestId, whenDone));
            } catch (RuntimeException launchFailed) {
                // The current activity can be gone by now, and
                // startActivityForResult throws when it is. Without this the
                // prompt never opened and nothing cleared the flag, so every
                // later request was refused as BUSY for the life of the
                // process -- the same shape the VPN consent path had.
                //
                // The WAITERS go too. rolePending is set inside the monitor
                // and released before this call, so a second request can be
                // parked in between -- and clearing only the flag left those
                // waiting on a prompt that never opened, for ever. The outer
                // catch fails this request; nothing else would have failed
                // theirs. Drained under the monitor that fills the list, so
                // one cannot be added after this has run.
                synchronized (CallScreeningRole.class) {
                    rolePending = false;
                    orphaned = new ArrayList<RoleResult>(ROLE_WAITERS);
                    ROLE_WAITERS.clear();
                }
                throw launchFailed;
            }
        } catch (Exception e) {
            finish(requestId, whenDone, false);
        }
        // OUTSIDE the try, and not merely for tidiness: iterating a
        // List<RoleResult> compiles to a CHECKCAST on each element, and a
        // CHECKCAST inside a catch(Exception) is what
        // scripts/check-cast-semantics.sh fails the build on -- ParparVM does
        // not throw for a failed cast, so that handler would never run on
        // iOS. Answering the waiters is not something a broad catch should be
        // wrapped around anyway.
        for (RoleResult w : orphaned) {
            w.neverPrompted();
        }
    }

    /// Answers a role request that ended without a prompt.
    private static void finish(int requestId, Runnable whenDone, boolean ok) {
        if (requestId >= 0) {
            if (ok) {
                Calls.deliverAck(requestId, true, 0, null);
            } else {
                unsupported(requestId);
            }
        }
        if (whenDone != null) {
            whenDone.run();
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
        // FALSE, not an error. requestScreeningRole() documents that it
        // "resolves false where the role was refused or does not exist", so
        // an app handling an ordinary denial in its success callback got an
        // exception instead -- for the outcome the contract calls normal.
        Calls.deliverAckValue(requestId, false);
    }

    /// Answers the role request once the user has decided.
    ///
    /// A named class rather than an anonymous one so it carries no synthetic
    /// reference to the activity, which outlives the dialog.
    /// Whether a role dialog owns the activity's single result channel.
    private static boolean rolePending;

    /// Requests that arrived while the prompt was already up, answered from
    /// its real result. Guarded by CallScreeningRole.class.
    ///
    /// Nothing here is answered twice: a waiter is parked instead of being
    /// answered, and the drain empties the list under the same monitor that
    /// fills it. A prompt whose result never arrives leaves them unanswered,
    /// which is the exposure the FIRST request already has -- the queue does
    /// not add a failure mode, it shares one.
    private static final List<RoleResult> ROLE_WAITERS =
            new ArrayList<RoleResult>();

    private static final class RoleResult
            implements com.codename1.impl.android.IntentResultListener {
        private final int requestId;

        private final Runnable whenDone;

        RoleResult(int requestId, Runnable whenDone) {
            this.requestId = requestId;
            this.whenDone = whenDone;
        }

        /// Answers as a request whose prompt never opened.
        private void neverPrompted() {
            finish(requestId, whenDone, false);
        }

        /// Hands this one caller the outcome, exactly once.
        private void answer(boolean granted) {
            if (requestId >= 0) {
                // The user's answer either way, as a value. A decline is
                // the documented false, not an exception.
                Calls.deliverAckValue(requestId, granted);
            }
            if (whenDone != null) {
                // The mask callers re-read getGrantedPermissions(), so they
                // see what Android actually holds rather than this flag.
                whenDone.run();
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode,
                Intent data) {
            // First, and whatever the outcome: a declined prompt that never
            // cleared this would block every later request for the life of
            // the process.
            // Copied, not toArray'd, for the reason the launch-failure path
            // gives: the same implicit cast, kept out of both places so the
            // pattern is uniform.
            List<RoleResult> waiting;
            synchronized (CallScreeningRole.class) {
                rolePending = false;
                waiting = new ArrayList<RoleResult>(ROLE_WAITERS);
                ROLE_WAITERS.clear();
            }
            enabled = resultCode == Activity.RESULT_OK;
            answer(enabled);
            for (RoleResult w : waiting) {
                w.answer(enabled);
            }
        }
    }
}
