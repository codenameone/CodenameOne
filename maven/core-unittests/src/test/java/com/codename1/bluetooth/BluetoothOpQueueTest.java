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
package com.codename1.bluetooth;

import com.codename1.bluetooth.gatt.GattCharacteristic;
import com.codename1.bluetooth.gatt.GattService;
import com.codename1.junit.UITestBase;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.codename1.bluetooth.BtTestUtil.bytes;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The per-peripheral GATT operation queue observed through the public
 * {@link com.codename1.bluetooth.le.BlePeripheral} API: strict
 * serialization toward the platform SPI, per-call result correlation,
 * skipping of operations cancelled while queued, and the safety timeout
 * that fails a lost operation without wedging the queue. The only
 * wall-clock dependency is the deliberately short 50ms operation timeout,
 * awaited through a latch with a generous hang-guard bound.
 */
class BluetoothOpQueueTest extends UITestBase {

    private static final BluetoothUuid SVC = BluetoothUuid.fromShort(0x180D);

    private FakeBlePeripheral p;
    private GattCharacteristic c1;
    private GattCharacteristic c2;
    private GattCharacteristic c3;

    @BeforeEach
    void connectPeripheral() {
        p = new FakeBlePeripheral("AA:BB:CC:DD:EE:01", "queued");
        GattService s = p.buildService(SVC);
        c1 = p.buildCharacteristic(s, BluetoothUuid.fromShort(0x2A37),
                GattCharacteristic.PROPERTY_READ);
        c2 = p.buildCharacteristic(s, BluetoothUuid.fromShort(0x2A38),
                GattCharacteristic.PROPERTY_READ);
        c3 = p.buildCharacteristic(s, BluetoothUuid.fromShort(0x2A39),
                GattCharacteristic.PROPERTY_READ);
        p.connectNow();
    }

    @Test
    void cancellingAPublishingOperationReachesTheQueuedOne() {
        // AsyncResource.cancel() marks itself done and notifies OBSERVERS; it
        // never runs the callback chain. Propagating the cancel through an
        // onResult listener was therefore dead code for the single event it
        // existed to catch -- so a cancelled requestMtu still started when
        // its turn came, and still wrote the MTU cache behind a caller who
        // had given up.
        AsyncResource<byte[]> inFlight = p.readCharacteristic(c1);
        AsyncResource<Integer> cancelled = p.requestMtu(185);
        assertEquals(1, p.pendingCount(),
                "the read is in flight; the mtu request is queued behind it");

        cancelled.cancel(true);
        p.completeNext(bytes(1, 2));
        assertArrayEquals(bytes(1, 2), inFlight.get());

        // The cancelled operation is skipped rather than started, so the
        // platform is never asked and the queue is not left holding it.
        assertEquals(0, p.pendingCount(),
                "a cancelled queued operation must be skipped, not started");
        assertEquals(23, p.getMtu(),
                "a cancelled request must not publish an MTU");

        // And the queue is still usable afterwards.
        AsyncResource<byte[]> after = p.readCharacteristic(c2);
        p.completeNext(bytes(5, 6));
        assertArrayEquals(bytes(5, 6), after.get());
    }

    @Test
    void cancellingAnInFlightOperationKeepsTheNativeSlot() {
        // Cancel cancels the CALLER's interest; the platform still has the
        // request and will still answer it. Starting the next operation
        // immediately put two in flight at once -- on Android the second
        // overwrites the first's pending slot, so the first callback settles
        // the second request with the wrong result. The slot stays reserved
        // until the platform answers.
        AsyncResource<Integer> inFlight = p.requestMtu(185);
        AsyncResource<byte[]> behind = p.readCharacteristic(c1);
        assertEquals(1, p.pendingCount(), "the read is queued behind it");

        inFlight.cancel(true);
        assertEquals(1, p.pendingCount(),
                "the platform still owns the cancelled request");

        // The platform answers the cancelled request; the queue moves on and
        // the answer is not published to the caller who gave up.
        p.completeNext(Integer.valueOf(185));
        assertEquals(23, p.getMtu(),
                "a cancelled request must not publish an MTU");
        assertSame(c1, p.peekNext().characteristic,
                "the next operation runs once the slot is free");
        p.completeNext(bytes(7, 8));
        assertArrayEquals(bytes(7, 8), behind.get());
    }

    @Test
    void aLostCallbackAfterACancelDoesNotWedgeTheQueue() {
        // The case the reserved slot would otherwise strand: the caller
        // cancels and the platform never answers. Releasing on the safety
        // timeout is the difference between one lost operation and a
        // peripheral that accepts nothing else for the life of the
        // connection.
        p.setOpTimeout(50);
        AsyncResource<Integer> inFlight = p.requestMtu(185);
        AsyncResource<byte[]> behind = p.readCharacteristic(c1);
        inFlight.cancel(true);

        long limit = System.currentTimeMillis() + 5000;
        while (p.pendingCount() < 2 && System.currentTimeMillis() < limit) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        assertEquals(2, p.pendingCount(),
                "the timeout must release a cancelled operation's slot");
        p.takeNext();
        p.completeNext(bytes(9, 9));
        assertArrayEquals(bytes(9, 9), behind.get());
    }

    @Test
    void aThrowingCallbackDoesNotWedgeTheQueue() {
        // requestMtu, not readCharacteristic. Only the operations that
        // publish state before completing -- requestMtu and discoverServices
        // -- have the hazard: the queue watches their INTERNAL resource, and
        // the listener that completes the caller's is registered on it
        // FIRST, ahead of the queue's advancement listener. Completing the
        // caller's resource runs the app's callbacks inline, so an app
        // callback that threw came out through the publishing listener, the
        // advancement never ran, and every later GATT operation on this
        // peripheral queued behind one nothing would start again. The
        // peripheral was dead for the life of the connection with no error
        // anywhere. Operations with no publish step register the advancement
        // before the app can attach anything, which is why they were fine.
        AsyncResource<Integer> mtuRequest = p.requestMtu(185);
        mtuRequest.ready(new SuccessCallback<Integer>() {
            @Override
            public void onSucess(Integer value) {
                throw new IllegalStateException("a badly behaved app");
            }
        });
        AsyncResource<byte[]> queued = p.readCharacteristic(c1);

        assertEquals(FakeBlePeripheral.OpKind.REQUEST_MTU, p.peekNext().kind);
        p.completeNext(Integer.valueOf(185));
        assertTrue(mtuRequest.isDone());
        assertEquals(185, p.getMtu(),
                "the state update still happens; only the app callback threw");

        assertEquals(1, p.pendingCount(),
                "the queue must advance even though the callback threw");
        assertSame(c1, p.peekNext().characteristic);
        p.completeNext(bytes(3, 4));
        assertArrayEquals(bytes(3, 4), queued.get(),
                "the operation behind the throwing callback still answers");
    }

    @Test
    void secondOperationIsNotStartedUntilTheFirstCompletes() {
        AsyncResource<byte[]> r1 = p.readCharacteristic(c1);
        AsyncResource<byte[]> r2 = p.readCharacteristic(c2);

        assertEquals(1, p.pendingCount(),
                "second do* must not be invoked while the first is in flight");
        assertSame(c1, p.peekNext().characteristic);
        assertFalse(r1.isDone());
        assertFalse(r2.isDone());

        FakeBlePeripheral.PendingOp op1 = p.completeNext(bytes(0x11));
        assertSame(c1, op1.characteristic);
        assertTrue(r1.isDone());
        assertFalse(r2.isDone(), "completing op1 must not resolve op2");

        // completing op1 released op2 to the SPI
        assertEquals(1, p.pendingCount());
        FakeBlePeripheral.PendingOp op2 = p.completeNext(bytes(0x22));
        assertSame(c2, op2.characteristic);

        // results correlate to the right handle
        assertArrayEquals(bytes(0x11), r1.get());
        assertArrayEquals(bytes(0x22), r2.get());
    }

    @Test
    void resultsCorrelateAcrossMixedOperationKinds() {
        AsyncResource<byte[]> read = p.readCharacteristic(c1);
        AsyncResource<Boolean> write = p.writeCharacteristic(c2,
                bytes(0x7F), true);
        AsyncResource<Integer> rssi = p.readRssi();

        assertEquals(FakeBlePeripheral.OpKind.READ_CHARACTERISTIC,
                p.completeNext(bytes(0x01)).kind);
        FakeBlePeripheral.PendingOp writeOp = p.peekNext();
        assertEquals(FakeBlePeripheral.OpKind.WRITE_CHARACTERISTIC,
                writeOp.kind);
        assertArrayEquals(bytes(0x7F), writeOp.value);
        assertTrue(writeOp.withResponse);
        p.completeNext(Boolean.TRUE);
        assertEquals(FakeBlePeripheral.OpKind.READ_RSSI,
                p.completeNext(Integer.valueOf(-51)).kind);

        assertArrayEquals(bytes(0x01), read.get());
        assertEquals(Boolean.TRUE, write.get());
        assertEquals(Integer.valueOf(-51), rssi.get());
    }

    @Test
    void operationCancelledWhileQueuedIsSkipped() {
        AsyncResource<byte[]> r1 = p.readCharacteristic(c1);
        AsyncResource<byte[]> r2 = p.readCharacteristic(c2);
        AsyncResource<byte[]> r3 = p.readCharacteristic(c3);

        assertTrue(r2.cancel(true));
        assertTrue(r2.isDone());

        p.completeNext(bytes(0x11));
        // r2 was skipped: the next SPI call is for c3
        assertEquals(1, p.pendingCount());
        assertSame(c3, p.peekNext().characteristic);
        p.completeNext(bytes(0x33));

        assertArrayEquals(bytes(0x11), r1.get());
        assertTrue(r2.isCancelled());
        assertArrayEquals(bytes(0x33), r3.get());
    }

    @Test
    void timedOutOperationFailsWithTimeoutAndTheQueueAdvances()
            throws InterruptedException {
        p.setOpTimeout(50);

        AsyncResource<byte[]> r1 = p.readCharacteristic(c1);
        AsyncResource<byte[]> r2 = p.readCharacteristic(c2);
        // r1 is already armed with the 50ms timeout; disable the timeout
        // again so r2 (armed when the timer thread advances the queue)
        // cannot race the test's own completeNext call
        p.setOpTimeout(0);

        final CountDownLatch timedOut = new CountDownLatch(1);
        final AtomicReference<Throwable> err =
                new AtomicReference<Throwable>();
        r1.except(t -> {
            err.set(t);
            timedOut.countDown();
        });

        // generous hang guard; the timeout itself is 50ms
        assertTrue(timedOut.await(20, TimeUnit.SECONDS),
                "operation timeout never fired");
        assertTrue(err.get() instanceof BluetoothException);
        assertEquals(BluetoothError.TIMEOUT,
                ((BluetoothException) err.get()).getError());

        // the queue is not wedged: the second op reaches the SPI (the timer
        // thread advances it, so wait with a hang guard)
        p.awaitPendingCount(2, 20000);
        // drain the abandoned first op, then serve the second
        assertSame(c1, p.takeNext().characteristic);
        FakeBlePeripheral.PendingOp op2 = p.completeNext(bytes(0x22));
        assertSame(c2, op2.characteristic);
        assertArrayEquals(bytes(0x22), r2.get());
    }

    @Test
    void spiThrowingSynchronouslyFailsTheOperationAndAdvances() {
        // a read on a fresh, never-connected peripheral is fail-fast; here
        // instead we exercise a do* that throws: subclass the fake inline
        FakeBlePeripheral throwing = new FakeBlePeripheral("AA:00", "boom") {
            private boolean first = true;

            @Override
            protected void doReadRssi(AsyncResource<Integer> out) {
                if (first) {
                    first = false;
                    throw new IllegalStateException("stack hiccup");
                }
                super.doReadRssi(out);
            }
        };
        throwing.connectNow();
        AsyncResource<Integer> r1 = throwing.readRssi();
        AsyncResource<Integer> r2 = throwing.readRssi();

        assertTrue(r1.isDone(), "a throwing do* must fail its operation");
        assertEquals(BluetoothError.UNKNOWN,
                ((BluetoothException) BtTestUtil.errorOf(r1)).getError());

        // the queue advanced to the second operation
        assertEquals(1, throwing.pendingCount());
        throwing.completeNext(Integer.valueOf(-60));
        assertEquals(Integer.valueOf(-60), r2.get());
    }
}
