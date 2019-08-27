/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.util;

import com.codename1.io.Util;
import com.codename1.testing.AbstractTest;

/**
 *
 * @author shannah
 */
public class AsyncResourceTests extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {

        AsyncResource r1 = new AsyncResource();
        new Thread(() -> {
            Util.sleep(100);
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

}
