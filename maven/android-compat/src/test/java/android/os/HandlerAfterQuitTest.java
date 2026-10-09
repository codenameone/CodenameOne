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
package android.os;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

/// A message sent to a background looper that has quit is refused, as on
/// Android. It used to be queued and reported accepted, although the loop
/// had exited and would never deliver it, so the handler kept it forever.
public class HandlerAfterQuitTest {

    private static final Runnable NOTHING = new Runnable() {
        @Override
        public void run() {
        }
    };

    @Test
    public void postAfterQuitIsRefusedAndNotRetained() throws Exception {
        HandlerThread t = new HandlerThread("after-quit");
        t.start();
        Handler h = new Handler(t.getLooper());
        t.quit();
        t.join(10000);
        assertFalse("looper exited", t.isAlive());
        assertFalse("post after quit is refused", h.post(NOTHING));
        assertFalse("front-of-queue post after quit is refused", h.postAtFrontOfQueue(NOTHING));
        assertFalse("sendEmptyMessage after quit is refused", h.sendEmptyMessage(1));
        assertFalse("the refused runnable is not retained", h.hasCallbacks(NOTHING));
        assertFalse("the refused message is not retained", h.hasMessages(1));
    }

    @Test
    public void postAfterQuitSafelyIsRefused() throws Exception {
        HandlerThread t = new HandlerThread("after-quit-safely");
        t.start();
        Handler h = new Handler(t.getLooper());
        t.quitSafely();
        assertFalse("post after quitSafely is refused", h.post(NOTHING));
        t.join(10000);
        assertFalse("looper exited", t.isAlive());
        assertFalse(h.hasCallbacks(NOTHING));
    }
}
