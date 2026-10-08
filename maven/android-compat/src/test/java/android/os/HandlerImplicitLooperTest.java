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

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.ui.Display;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// `new Handler()` binds to the calling thread's looper. On a thread without
/// one it throws, as on Android; it used to bind to the main looper and run
/// the worker's callbacks on the UI thread.
public class HandlerImplicitLooperTest {

    @Test
    public void workerWithoutLooperIsRefused() throws Exception {
        AndroidTestSupport.context();
        final Object[] seen = new Object[2];
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    seen[0] = new Handler();
                } catch (RuntimeException e) {
                    seen[1] = e;
                }
            }
        });
        t.start();
        t.join();
        assertNull("a handler was created on a thread with no looper", seen[0]);
        assertTrue(String.valueOf(seen[1]), seen[1] instanceof RuntimeException
                && ((RuntimeException) seen[1]).getMessage().indexOf("Looper.prepare()") >= 0);
    }

    @Test
    public void edtAndPreparedThreadsStillWork() throws Exception {
        AndroidTestSupport.context();
        final Looper[] edt = new Looper[1];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                edt[0] = new Handler().getLooper();
            }
        });
        assertSame(Looper.getMainLooper(), edt[0]);
        final Looper[] worker = new Looper[2];
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                worker[0] = Looper.myLooper();
                worker[1] = new Handler((Handler.Callback) null).getLooper();
            }
        });
        t.start();
        t.join();
        assertSame(worker[0], worker[1]);
    }
}
