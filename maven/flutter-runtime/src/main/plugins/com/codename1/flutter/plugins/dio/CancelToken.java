/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.dio;

import com.codename1.io.ConnectionRequest;

/// dio's `CancelToken`: cancels the requests it was passed to.
public class CancelToken {
    private boolean cancelled;
    private Object reason;
    /// Every request this token was handed, with its cancel callback: one token is
    /// often shared by several concurrent requests, and cancel() stops all of them. A
    /// request that already finished ignores both (its completion is guarded).
    private final java.util.List<ConnectionRequest> requests = new java.util.ArrayList<ConnectionRequest>();
    private final java.util.List<Runnable> callbacks = new java.util.ArrayList<Runnable>();

    public CancelToken() {
    }

    /// `CancelToken.isCancel(e)`: whether a DioException is a cancellation.
    public static boolean isCancel(Object e) {
        return e instanceof DioException && ((DioException) e).type() == DioExceptionType.cancel;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public Object cancelError() {
        return reason;
    }

    public void cancel() {
        cancel(null);
    }

    public void cancel(Object why) {
        if (cancelled) {
            return;
        }
        cancelled = true;
        reason = why;
        ConnectionRequest[] reqs = requests.toArray(new ConnectionRequest[requests.size()]);
        Runnable[] cbs = callbacks.toArray(new Runnable[callbacks.size()]);
        requests.clear();
        callbacks.clear();
        for (int i = 0; i < reqs.length; i++) {
            reqs[i].kill();
            cbs[i].run();
        }
    }

    /// A settled request leaves the token, so a long-lived token reused for many
    /// requests does not keep every finished one (and its callback) alive.
    void detach(ConnectionRequest req) {
        // by identity: ConnectionRequest.equals matches duplicate requests (same URL)
        for (int i = 0; i < requests.size(); i++) {
            if (requests.get(i) == req) {
                requests.remove(i);
                callbacks.remove(i);
                return;
            }
        }
    }

    void attach(ConnectionRequest req, Runnable cancelled) {
        if (this.cancelled) {
            // dio fails a request made with an already-cancelled token straight away.
            req.kill();
            cancelled.run();
            return;
        }
        requests.add(req);
        callbacks.add(cancelled);
    }
}
