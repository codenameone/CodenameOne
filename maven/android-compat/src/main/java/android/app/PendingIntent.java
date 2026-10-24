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
package android.app;

import android.content.Context;
import android.content.Intent;

/// An intent to run later, on behalf of this application. There is no other
/// application to hand one to here, so a pending intent is the intent and the
/// call that will start it; [#send()] makes that call. Notifications keep the
/// one they are given (tapping a Codename One local notification brings the
/// application forward, it does not run the intent).
public final class PendingIntent {

    public static final int FLAG_ONE_SHOT = 1 << 30;
    public static final int FLAG_NO_CREATE = 1 << 29;
    public static final int FLAG_CANCEL_CURRENT = 1 << 28;
    public static final int FLAG_UPDATE_CURRENT = 1 << 27;
    public static final int FLAG_IMMUTABLE = 1 << 26;
    public static final int FLAG_MUTABLE = 1 << 25;
    public static final int FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT = 1 << 24;

    static final int KIND_ACTIVITY = 0;
    static final int KIND_SERVICE = 1;
    static final int KIND_FOREGROUND_SERVICE = 2;
    static final int KIND_BROADCAST = 3;

    /// Thrown by [#send()] once a one-shot or cancelled intent has been used.
    public static class CanceledException extends Exception {
        public CanceledException() {
        }

        public CanceledException(String name) {
            super(name);
        }
    }

    private final Context context;
    private final Intent intent;
    private final int kind;
    private final int requestCode;
    private final int flags;
    private boolean cancelled;

    private PendingIntent(Context context, int requestCode, Intent intent, int flags, int kind) {
        this.context = context;
        this.requestCode = requestCode;
        this.intent = intent == null ? new Intent() : new Intent(intent);
        this.flags = flags;
        this.kind = kind;
    }

    public static PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags) {
        return new PendingIntent(context, requestCode, intent, flags, KIND_ACTIVITY);
    }

    public static PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags,
            android.os.Bundle options) {
        return getActivity(context, requestCode, intent, flags);
    }

    public static PendingIntent getService(Context context, int requestCode, Intent intent, int flags) {
        return new PendingIntent(context, requestCode, intent, flags, KIND_SERVICE);
    }

    public static PendingIntent getForegroundService(Context context, int requestCode, Intent intent, int flags) {
        return new PendingIntent(context, requestCode, intent, flags, KIND_FOREGROUND_SERVICE);
    }

    public static PendingIntent getBroadcast(Context context, int requestCode, Intent intent, int flags) {
        return new PendingIntent(context, requestCode, intent, flags, KIND_BROADCAST);
    }

    public void send() throws CanceledException {
        if (cancelled) {
            throw new CanceledException();
        }
        if ((flags & FLAG_ONE_SHOT) != 0) {
            cancelled = true;
        }
        switch (kind) {
            case KIND_ACTIVITY:
                context.startActivity(intent);
                break;
            case KIND_SERVICE:
                context.startService(intent);
                break;
            case KIND_FOREGROUND_SERVICE:
                context.startForegroundService(intent);
                break;
            default:
                context.sendBroadcast(intent);
                break;
        }
    }

    public void send(int code) throws CanceledException {
        send();
    }

    public void cancel() {
        cancelled = true;
    }

    public String getCreatorPackage() {
        return context.getPackageName();
    }

    public boolean isActivity() {
        return kind == KIND_ACTIVITY;
    }

    public boolean isService() {
        return kind == KIND_SERVICE || kind == KIND_FOREGROUND_SERVICE;
    }

    public boolean isBroadcast() {
        return kind == KIND_BROADCAST;
    }

    public boolean isImmutable() {
        return (flags & FLAG_IMMUTABLE) != 0;
    }

    int requestCode() {
        return requestCode;
    }
}
