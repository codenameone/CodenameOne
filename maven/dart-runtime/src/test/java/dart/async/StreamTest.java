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
package dart.async;

import dart.runtime.Funcs;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Stream/StreamSubscription/StreamController: broadcast delivery, cancel
/// firing the "no more listeners" hook, map/where, and StreamController's
/// add/addError/close.
public class StreamTest {

    @Test
    public void listenRunsOnFirstListenerOnce() {
        final int[] calls = {0};
        Stream<Object> s = new Stream<Object>() {
            @Override
            protected void onFirstListener() {
                calls[0]++;
            }
        };
        StreamSubscription<Object> a = s.listen(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
            }
        });
        StreamSubscription<Object> b = s.listen(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
            }
        });
        assertEquals(1, calls[0], "onFirstListener must run once, not per listener");
        a.cancel();
        b.cancel();
    }

    @Test
    public void everyLiveListenerReceivesEveryEvent() {
        StreamController<String> c = new StreamController<String>();
        final List<String> seenA = new ArrayList<String>();
        final List<String> seenB = new ArrayList<String>();
        c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
                seenA.add(v);
            }
        });
        c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
                seenB.add(v);
            }
        });
        c.add("one");
        c.add("two");
        assertEquals(java.util.Arrays.asList("one", "two"), seenA);
        assertEquals(java.util.Arrays.asList("one", "two"), seenB);
    }

    @Test
    public void cancellingTheLastListenerRunsOnNoListeners() {
        final int[] noListenersCalls = {0};
        Stream<Object> s = new Stream<Object>() {
            @Override
            protected void onNoListeners() {
                noListenersCalls[0]++;
            }
        };
        StreamSubscription<Object> a = s.listen(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
            }
        });
        StreamSubscription<Object> b = s.listen(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
            }
        });
        a.cancel();
        assertEquals(0, noListenersCalls[0], "a live listener remains");
        b.cancel();
        assertEquals(1, noListenersCalls[0], "the last cancel must run onNoListeners");
        // Cancelling again must not run it a second time.
        b.cancel();
        assertEquals(1, noListenersCalls[0]);
    }

    @Test
    public void errorsReachOnErrorAndCancelOnErrorStopsDelivery() {
        StreamController<String> c = new StreamController<String>();
        final List<Object> errors = new ArrayList<Object>();
        final List<String> data = new ArrayList<String>();
        c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
                data.add(v);
            }
        }, new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object e) {
                errors.add(e);
            }
        }, null, Boolean.TRUE);
        c.addError("boom");
        c.add("after error"); // cancelOnError: this subscription is gone by now
        assertEquals(1, errors.size());
        assertEquals("boom", errors.get(0));
        assertTrue(data.isEmpty(), "cancelOnError must stop further delivery to this subscription");
    }

    @Test
    public void doneClosesTheControllerAndClearsListeners() {
        StreamController<String> c = new StreamController<String>();
        final boolean[] done = {false};
        c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
            }
        }, null, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                done[0] = true;
            }
        });
        assertFalse(c.isClosed());
        c.close();
        assertTrue(done[0], "close() must deliver onDone to every live listener");
        assertTrue(c.isClosed());
    }

    @Test
    public void addingAfterCloseThrows() {
        StreamController<String> c = new StreamController<String>();
        c.close();
        org.junit.jupiter.api.Assertions.assertThrows(dart.core.StateError.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                c.add("too late");
            }
        });
    }

    @Test
    public void onListenAndOnCancelFireOnFirstListenAndLastCancel() {
        StreamController<String> c = new StreamController<String>();
        final int[] listenCalls = {0};
        final int[] cancelCalls = {0};
        c.onListen(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                listenCalls[0]++;
            }
        });
        c.onCancel(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                cancelCalls[0]++;
            }
        });
        assertFalse(c.hasListener());
        StreamSubscription<String> sub = c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
            }
        });
        assertTrue(c.hasListener());
        assertEquals(1, listenCalls[0]);
        sub.cancel();
        assertFalse(c.hasListener());
        assertEquals(1, cancelCalls[0]);
    }

    @Test
    public void broadcastFactoryBehavesLikeThePlainConstructor() {
        @SuppressWarnings("unchecked")
        StreamController<String> c = StreamController.broadcast();
        final List<String> seen = new ArrayList<String>();
        c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
                seen.add(v);
            }
        });
        c.add("x");
        assertEquals(java.util.Arrays.asList("x"), seen);
    }

    @Test
    public void mapTransformsEveryEvent() {
        StreamController<Integer> c = new StreamController<Integer>();
        Stream<String> mapped = c.stream().map(new Funcs.Func1<Integer, String>() {
            @Override
            public String call(Integer v) {
                return "<" + v + ">";
            }
        });
        final List<String> seen = new ArrayList<String>();
        mapped.listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
                seen.add(v);
            }
        });
        c.add(1);
        c.add(2);
        assertEquals(java.util.Arrays.asList("<1>", "<2>"), seen);
    }

    @Test
    public void whereFiltersEvents() {
        StreamController<Integer> c = new StreamController<Integer>();
        Stream<Integer> evens = c.stream().where(new Funcs.Func1<Integer, Boolean>() {
            @Override
            public Boolean call(Integer v) {
                return v % 2 == 0;
            }
        });
        final List<Integer> seen = new ArrayList<Integer>();
        evens.listen(new Funcs.VoidFunc1<Integer>() {
            @Override
            public void call(Integer v) {
                seen.add(v);
            }
        });
        c.add(1);
        c.add(2);
        c.add(3);
        c.add(4);
        assertEquals(java.util.Arrays.asList(2, 4), seen);
    }

    @Test
    public void derivedStreamCancelsItsUpstreamSubscriptionWhenItHasNoListenersLeft() {
        StreamController<Integer> c = new StreamController<Integer>();
        final int[] cancelCalls = {0};
        c.onCancel(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                cancelCalls[0]++;
            }
        });
        Stream<Integer> doubled = c.stream().map(new Funcs.Func1<Integer, Integer>() {
            @Override
            public Integer call(Integer v) {
                return v * 2;
            }
        });
        StreamSubscription<Integer> sub = doubled.listen(new Funcs.VoidFunc1<Integer>() {
            @Override
            public void call(Integer v) {
            }
        });
        assertTrue(c.hasListener(), "the derived stream must subscribe upstream");
        sub.cancel();
        assertFalse(c.hasListener(), "cancelling the only derived listener must cancel upstream too");
        assertEquals(1, cancelCalls[0]);
    }

    @Test
    public void pauseBuffersDataAndDeliversItInOrderOnResume() {
        StreamController<String> c = new StreamController<String>();
        final List<String> seen = new ArrayList<String>();
        StreamSubscription<String> sub = c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
                seen.add(v);
            }
        });
        c.add("one");
        sub.pause();
        assertTrue(sub.isPaused());
        c.add("queued while paused");
        assertEquals(java.util.Arrays.asList("one"), seen,
                "a paused subscription must not receive data while paused");
        sub.resume();
        assertFalse(sub.isPaused());
        c.add("two");
        assertEquals(java.util.Arrays.asList("one", "queued while paused", "two"), seen,
                "resume() must deliver what was buffered while paused, in order, before later events");
    }

    @Test
    public void pauseBuffersErrorsAndDeliversThemOnResume() {
        StreamController<String> c = new StreamController<String>();
        final List<Object> errors = new ArrayList<Object>();
        StreamSubscription<String> sub = c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
            }
        }, new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object e) {
                errors.add(e);
            }
        });
        sub.pause();
        c.addError("boom");
        assertTrue(errors.isEmpty(), "a paused subscription must not receive errors while paused");
        sub.resume();
        assertEquals(java.util.Arrays.asList((Object) "boom"), errors,
                "resume() must deliver an error that was buffered while paused");
    }

    @Test
    public void pauseBuffersDoneAndDeliversItOnResume() {
        StreamController<String> c = new StreamController<String>();
        final boolean[] done = {false};
        StreamSubscription<String> sub = c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
            }
        }, null, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                done[0] = true;
            }
        });
        sub.pause();
        c.close();
        assertFalse(done[0], "a paused subscription must not receive done while paused");
        sub.resume();
        assertTrue(done[0], "resume() must deliver a done event that was buffered while paused");
    }

    @Test
    public void cancelWhilePausedDiscardsTheBufferedQueue() {
        StreamController<String> c = new StreamController<String>();
        final List<String> seen = new ArrayList<String>();
        StreamSubscription<String> sub = c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
                seen.add(v);
            }
        });
        sub.pause();
        c.add("queued then cancelled");
        sub.cancel();
        sub.resume();
        assertTrue(seen.isEmpty(), "cancel() while paused must discard the buffered queue, not deliver it");
    }

    @Test
    public void secondPauseRequiresASecondResumeBeforeDraining() {
        // Dart's own contract: pause() nests, and resume() must be called once per
        // pause() before delivery actually resumes -- confirmed against the Dart 3.9
        // VM's _BufferingStreamSubscription, which tracks a pause COUNT rather than a
        // flag. A single boolean "paused" flag collapses two pause() calls into one,
        // so the first of the two matching resume() calls already drained everything.
        StreamController<String> c = new StreamController<String>();
        final List<String> seen = new ArrayList<String>();
        StreamSubscription<String> sub = c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
                seen.add(v);
            }
        });
        sub.pause();
        sub.pause();
        c.add("queued while doubly paused");
        sub.resume();
        assertTrue(sub.isPaused(), "one resume() must not undo two pause() calls");
        assertTrue(seen.isEmpty(), "still paused after only one matching resume(): must not deliver yet");
        sub.resume();
        assertFalse(sub.isPaused(), "the second, matching resume() must lift the pause");
        assertEquals(java.util.Arrays.asList("queued while doubly paused"), seen,
                "the second resume() must drain what was buffered while doubly paused");
    }

    @Test
    public void unmatchedResumeBeyondThePauseCountIsANoOp() {
        // The mirror case: more resume() calls than pause() calls must not throw and
        // must not do anything odd (it must not, for instance, leave the subscription
        // "negatively paused" so a later real pause() needs two resumes to clear it).
        StreamController<String> c = new StreamController<String>();
        final List<String> seen = new ArrayList<String>();
        StreamSubscription<String> sub = c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
                seen.add(v);
            }
        });
        sub.resume();
        assertFalse(sub.isPaused(), "resume() with no prior pause() must stay a no-op");
        sub.pause();
        c.add("queued");
        sub.resume();
        assertFalse(sub.isPaused());
        assertEquals(java.util.Arrays.asList("queued"), seen,
                "a single pause() must still need only a single resume() after an earlier stray resume()");
    }

    @Test
    public void closeFutureWaitsForAPausedListenerToDrainDone() {
        // Regression: close() used to hand back an already-completed future
        // even while a paused listener still had the done event only queued.
        // Measured on the Dart 3.9 VM: StreamController.close()'s future does
        // not complete until every listener has actually handled done --
        // delayed, for a paused one, until it resumes (or cancels).
        StreamController<String> c = new StreamController<String>();
        StreamSubscription<String> sub = c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
            }
        });
        sub.pause();

        Future<Object> closeFuture = c.close();
        assertFalse(closeFuture.isCompletedOrAdopting(),
                "close() must not complete while a paused listener still has done queued");

        sub.resume();
        assertTrue(closeFuture.isCompletedOrAdopting(),
                "resume() draining the queued done must complete the close future");
    }

    @Test
    public void closeFutureCompletesWhenAPausedListenerCancelsInsteadOfDraining() {
        // Cancelling a paused listener discards its queued done (see
        // cancelWhilePausedDiscardsTheBufferedQueue) rather than ever
        // delivering it, so close() must resolve there too -- confirmed
        // against the Dart 3.9 VM, which never delivers the cancelled
        // listener's done but still completes close()'s future.
        StreamController<String> c = new StreamController<String>();
        StreamSubscription<String> sub = c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
            }
        });
        sub.pause();

        Future<Object> closeFuture = c.close();
        assertFalse(closeFuture.isCompletedOrAdopting());

        sub.cancel();
        assertTrue(closeFuture.isCompletedOrAdopting(),
                "cancelling the paused listener must complete the close future too");
    }

    @Test
    public void closeFutureWaitsForEveryPausedListenerIndependently() {
        // Two paused listeners: the shared close future must not complete
        // until BOTH have been resolved, one by drain and the other by
        // cancel -- confirmed against the Dart 3.9 VM.
        StreamController<String> c = new StreamController<String>();
        StreamSubscription<String> sub1 = c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
            }
        });
        StreamSubscription<String> sub2 = c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
            }
        });
        sub1.pause();
        sub2.pause();

        Future<Object> closeFuture = c.close();
        assertFalse(closeFuture.isCompletedOrAdopting());

        sub1.resume();
        assertFalse(closeFuture.isCompletedOrAdopting(),
                "the other paused listener still has not handled done");

        sub2.cancel();
        assertTrue(closeFuture.isCompletedOrAdopting());
    }

    @Test
    public void listenAfterCloseCompletesWithDoneAndDoesNotRestartTheListenSession() {
        StreamController<String> c = new StreamController<String>();
        final int[] onListenCalls = {0};
        c.onListen(new Funcs.VoidFunc0() {
            @Override
            public void call() {
                onListenCalls[0]++;
            }
        });
        c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
            }
        });
        assertEquals(1, onListenCalls[0]);
        c.close();

        final boolean[] done = {false};
        c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
            }
        }, null, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                done[0] = true;
            }
        });
        assertTrue(done[0], "listen() on an already-done stream must complete with onDone");
        assertEquals(1, onListenCalls[0],
                "listen() on an already-done stream must not run onListen/onFirstListener again");
    }

    // --- A throwing listener callback is reported, not left to unwind emission --
    //
    // Dart's own broadcast delivery (stream_impl.dart's _sendData/_sendError/
    // _sendDone) runs every callback through Zone.runUnaryGuarded/runGuarded,
    // which catches whatever the callback throws and hands it to the zone's
    // uncaught-error handler instead of letting it escape the add()/emit()
    // that triggered it. A listener that throws must therefore neither stop
    // the remaining listeners from seeing the event nor make add()/addError()/
    // close()/resume() throw to their caller.

    @Test
    public void aThrowingOnDataDoesNotStopLaterListenersOrEscapeAdd() {
        StreamController<String> c = new StreamController<String>();
        final List<String> seenB = new ArrayList<String>();
        c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
                throw new RuntimeException("boom from listener A");
            }
        });
        c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
                seenB.add(v);
            }
        });
        c.add("one"); // must not throw out to the caller
        c.add("two");
        assertEquals(java.util.Arrays.asList("one", "two"), seenB,
                "listener A throwing must not stop listener B from receiving every event");
    }

    @Test
    public void aThrowingOnErrorDoesNotEscapeAddError() {
        StreamController<String> c = new StreamController<String>();
        final List<Object> seenB = new ArrayList<Object>();
        c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
            }
        }, new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object e) {
                throw new RuntimeException("boom from error handler A");
            }
        });
        c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
            }
        }, new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object e) {
                seenB.add(e);
            }
        });
        c.addError("boom"); // must not throw out to the caller
        assertEquals(1, seenB.size(),
                "listener A's error handler throwing must not stop listener B from receiving the error");
    }

    @Test
    public void aThrowingOnDoneDoesNotEscapeClose() {
        StreamController<String> c = new StreamController<String>();
        final boolean[] doneB = {false};
        c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
            }
        }, null, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                throw new RuntimeException("boom from done handler A");
            }
        });
        c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
            }
        }, null, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                doneB[0] = true;
            }
        });
        c.close(); // must not throw out to the caller
        assertTrue(doneB[0], "listener A's done handler throwing must not stop listener B from seeing done");
    }

    @Test
    public void aThrowingCallbackOnTheDrainPathDoesNotEscapeResume() {
        // Exercises deliverData/deliverError/deliverVoid via drain(), the
        // paused-subscription queue -- a separate call site from emit()/
        // emitError()/emitDone()'s own live-delivery loop.
        StreamController<String> c = new StreamController<String>();
        final List<String> seen = new ArrayList<String>();
        final boolean[] done = {false};
        StreamSubscription<String> sub = c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
                seen.add(v);
                if ("boom".equals(v)) {
                    throw new RuntimeException("boom while draining");
                }
            }
        }, new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object e) {
                throw new RuntimeException("boom from queued error handler");
            }
        }, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                done[0] = true;
                throw new RuntimeException("boom from queued done handler");
            }
        });
        sub.pause();
        c.add("boom");
        c.addError("queued error");
        c.close();
        sub.resume(); // must not throw out to the caller, and must still drain the whole queue
        assertEquals(java.util.Arrays.asList("boom"), seen);
        assertTrue(done[0], "a throwing data/error callback earlier in the queue must not stop done from draining");
    }

    @Test
    public void aThrowingOnDoneForListenAfterCloseDoesNotEscapeListen() {
        StreamController<String> c = new StreamController<String>();
        c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
            }
        });
        c.close();
        // listen() on an already-done stream delivers onDone synchronously,
        // through deliverVoid() directly rather than through emitDone()'s loop.
        StreamSubscription<String> sub = c.stream().listen(new Funcs.VoidFunc1<String>() {
            @Override
            public void call(String v) {
            }
        }, null, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                throw new RuntimeException("boom from onDone on an already-closed stream");
            }
        }); // must not throw out to the caller
        assertFalse(sub.isPaused());
    }
}
