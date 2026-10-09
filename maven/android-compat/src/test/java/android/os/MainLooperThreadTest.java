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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/// The main looper's thread is the event dispatch thread. It used to be
/// null, so `Looper.getMainLooper().getThread() == Thread.currentThread()`
/// -- a common main-thread check -- was false everywhere.
public class MainLooperThreadTest {

    @Test
    public void theMainLooperThreadIsTheEdt() {
        AndroidTestSupport.context();
        final Thread[] seen = new Thread[2];
        Display.getInstance().callSeriallyAndWait(new Runnable() {
            @Override
            public void run() {
                seen[0] = Thread.currentThread();
                seen[1] = Looper.getMainLooper().getThread();
            }
        });
        assertNotNull(seen[0]);
        assertSame("on the EDT", seen[0], seen[1]);
        assertSame("asked from another thread", seen[0], Looper.getMainLooper().getThread());
        assertFalse(Looper.getMainLooper().isCurrentThread());
    }
}
