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
    public void pauseStopsDeliveryUntilResume() {
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
        c.add("dropped while paused");
        sub.resume();
        assertFalse(sub.isPaused());
        c.add("two");
        assertEquals(java.util.Arrays.asList("one", "two"), seen);
    }
}
