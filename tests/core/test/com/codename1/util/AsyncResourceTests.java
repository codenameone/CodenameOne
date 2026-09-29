/*
 * Copyright (c) 2019, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.util;

import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;

public class AsyncResourceTests extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {

        AsyncResource r1 = new AsyncResource();
        new Thread(() -> {
            Util.sleep(500);
            r1.complete(new Integer(1));
        }).start();
        assertTrue(!r1.isCancelled());
        assertTrue(!r1.isDone());
        assertTrue(!r1.isReady());
        assertEqual(new Integer(0), r1.get(new Integer(0)));
        AsyncResource.await(r1);
        assertTrue(r1.isDone());
        assertTrue(r1.isReady());
        assertEqual(new Integer(1), r1.get(new Integer(0)));
        assertEqual(new Integer(1), r1.get());
        Integer[] val = new Integer[1];
        r1.ready(o -> {
            val[0] = (Integer) o;
        });
        assertEqual(new Integer(1), val[0]);
        val[0] = null;
        r1.ready(o -> {
            val[0] = (Integer) o;
        });
        assertEqual(new Integer(1), val[0]);

        // Now test all() to make sure that its ready fires when all components are ready.
        AsyncResource r2 = new AsyncResource();
        AsyncResource r3 = new AsyncResource();
        AsyncResource r4 = AsyncResource.all(r2, r3);
        assertTrue(!r2.isDone());
        assertTrue(!r3.isDone());
        assertTrue(!r4.isDone());

        r2.complete(new Integer(1));
        assertTrue(r2.isDone());
        assertTrue(r2.isReady());
        assertTrue(!r3.isDone());
        assertTrue(!r3.isReady());
        assertTrue(!r4.isDone());
        assertTrue(!r4.isReady());
        r3.complete(new Integer(2));
        assertTrue(r3.isReady());
        assertTrue(r3.isDone());
        assertTrue(r4.isReady());
        assertTrue(r4.isDone());

        AsyncResource r5 = new AsyncResource();
        AsyncResource r6 = new AsyncResource();
        AsyncResource r7 = AsyncResource.all(r5, r6);
        r5.complete(new Integer(1));
        r6.error(new RuntimeException("Foo"));
        assertTrue(!r7.isReady());
        assertTrue(r7.isDone());
        Throwable[] t7 = new Throwable[1];
        r7.except(ex -> {
            t7[0] = (Throwable) ex;
        });
        assertTrue(t7[0] != null);
        t7[0] = null;
        try {
            r7.get();
        } catch (Throwable ex) {
            t7[0] = ex;
        }
        assertTrue(t7[0] != null);
        t7[0] = null;
        try {
            AsyncResource.await(r7);
        } catch (Throwable ex) {
            t7[0] = ex;
        }
        assertTrue(t7[0] != null);

        return true;
    }

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }
    
    

}
