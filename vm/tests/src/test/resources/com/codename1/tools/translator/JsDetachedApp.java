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

import com.codename1.annotations.JavaScriptDetached;

/// `@JavaScriptDetached`: a caller that cannot suspend runs the method until it
/// waits and then continues while the rest finishes on its own thread; a caller
/// that is itself a generator runs the body in place and waits for it.
public class JsDetachedApp {
    public static int result;
    static int done;
    static final Object LOCK = new Object();

    @JavaScriptDetached
    private void post(int tag) {
        try {
            Thread.sleep(5);
        } catch (InterruptedException err) {
            return;
        }
        done |= tag;
    }

    // The monitor is entered before the split and exited after it, by the
    // detached thread -- which must therefore be the thread that entered it.
    @JavaScriptDetached
    private void lockedPost(int tag) {
        synchronized (LOCK) {
            try {
                Thread.sleep(5);
            } catch (InterruptedException err) {
                return;
            }
            done |= tag;
        }
    }

    // Nothing here suspends, so this is a plain function and post detaches.
    void fromSyncCaller() {
        lockedPost(8);
        post(1);
        if ((done & 1) == 0) {
            result |= 1;
        }
    }

    // Already a generator (it yields), so post runs in place and completes.
    void fromSuspendingCaller() {
        Thread.yield();
        post(2);
        if ((done & 2) != 0) {
            result |= 2;
        }
    }

    public static void main(String[] args) throws Exception {
        JsDetachedApp app = new JsDetachedApp();
        app.fromSyncCaller();
        app.fromSuspendingCaller();
        Thread.sleep(50);
        if ((done & 1) != 0) {
            // the detached body still ran to completion
            result |= 4;
        }
        if ((done & 8) != 0) {
            result |= 8;
        }
    }
}
