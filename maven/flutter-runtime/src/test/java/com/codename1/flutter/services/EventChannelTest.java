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
package com.codename1.flutter.services;

import com.codename1.flutter.platform.PlatformException;
import dart.async.Stream;
import dart.async.StreamSubscription;
import dart.runtime.Funcs;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// com.codename1.flutter.services.EventChannel (the Dart side) and
/// com.codename1.flutter.platform.EventChannel (the Java side) matched by
/// channel name through FlutterChannelRegistry: onListen on the first
/// listener, events pushed through EventSink, onCancel on the last cancel.
public class EventChannelTest {

    @Test
    public void listeningRunsOnListenAndEventsReachTheListener() {
        String channel = "test/events/" + System.nanoTime();
        final List<com.codename1.flutter.platform.EventChannel.EventSink> sinks =
                new ArrayList<com.codename1.flutter.platform.EventChannel.EventSink>();
        new com.codename1.flutter.platform.EventChannel(channel).setStreamHandler(
                new com.codename1.flutter.platform.EventChannel.StreamHandler() {
                    @Override
                    public void onListen(Object arguments, com.codename1.flutter.platform.EventChannel.EventSink events) {
                        assertEquals("args", arguments);
                        sinks.add(events);
                    }

                    @Override
                    public void onCancel(Object arguments) {
                    }
                });
        Stream<Object> stream = new EventChannel(channel).receiveBroadcastStream("args");
        final List<Object> received = new ArrayList<Object>();
        stream.listen(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
                received.add(v);
            }
        });
        assertEquals(1, sinks.size(), "onListen must run exactly once, on the first listener");
        sinks.get(0).success("tick 1");
        sinks.get(0).success("tick 2");
        assertEquals(java.util.Arrays.asList("tick 1", "tick 2"), received);
    }

    @Test
    public void cancellingTheLastListenerRunsOnCancel() {
        String channel = "test/events/" + System.nanoTime();
        final int[] cancelCalls = {0};
        final com.codename1.flutter.platform.EventChannel.EventSink[] sink =
                new com.codename1.flutter.platform.EventChannel.EventSink[1];
        new com.codename1.flutter.platform.EventChannel(channel).setStreamHandler(
                new com.codename1.flutter.platform.EventChannel.StreamHandler() {
                    @Override
                    public void onListen(Object arguments, com.codename1.flutter.platform.EventChannel.EventSink events) {
                        sink[0] = events;
                    }

                    @Override
                    public void onCancel(Object arguments) {
                        cancelCalls[0]++;
                    }
                });
        Stream<Object> stream = new EventChannel(channel).receiveBroadcastStream();
        StreamSubscription<Object> a = stream.listen(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
            }
        });
        StreamSubscription<Object> b = stream.listen(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
            }
        });
        a.cancel();
        assertEquals(0, cancelCalls[0], "a live listener remains");
        b.cancel();
        assertEquals(1, cancelCalls[0], "the last cancel must run the platform handler's onCancel");
        // A later event has nowhere to go -- nothing throws, nothing is delivered.
        sink[0].success("late");
    }

    @Test
    public void eventSinkErrorBecomesAPlatformExceptionOnTheListener() {
        String channel = "test/events/" + System.nanoTime();
        final com.codename1.flutter.platform.EventChannel.EventSink[] sink =
                new com.codename1.flutter.platform.EventChannel.EventSink[1];
        new com.codename1.flutter.platform.EventChannel(channel).setStreamHandler(
                new com.codename1.flutter.platform.EventChannel.StreamHandler() {
                    @Override
                    public void onListen(Object arguments, com.codename1.flutter.platform.EventChannel.EventSink events) {
                        sink[0] = events;
                    }

                    @Override
                    public void onCancel(Object arguments) {
                    }
                });
        Stream<Object> stream = new EventChannel(channel).receiveBroadcastStream();
        final List<Object> errors = new ArrayList<Object>();
        stream.listen(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
            }
        }, new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object e) {
                errors.add(e);
            }
        });
        sink[0].error("E_SENSOR", "no sensor", null);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0) instanceof PlatformException);
        assertEquals("E_SENSOR", ((PlatformException) errors.get(0)).code());
    }

    @Test
    public void endOfStreamDeliversOnDone() {
        String channel = "test/events/" + System.nanoTime();
        final com.codename1.flutter.platform.EventChannel.EventSink[] sink =
                new com.codename1.flutter.platform.EventChannel.EventSink[1];
        new com.codename1.flutter.platform.EventChannel(channel).setStreamHandler(
                new com.codename1.flutter.platform.EventChannel.StreamHandler() {
                    @Override
                    public void onListen(Object arguments, com.codename1.flutter.platform.EventChannel.EventSink events) {
                        sink[0] = events;
                    }

                    @Override
                    public void onCancel(Object arguments) {
                    }
                });
        Stream<Object> stream = new EventChannel(channel).receiveBroadcastStream();
        final boolean[] done = {false};
        stream.listen(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
            }
        }, null, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                done[0] = true;
            }
        });
        sink[0].endOfStream();
        assertTrue(done[0]);
    }

    @Test
    public void twoReceiveBroadcastStreamCallsAreIndependentListenSessions() {
        String channel = "test/events/" + System.nanoTime();
        final int[] listenCalls = {0};
        new com.codename1.flutter.platform.EventChannel(channel).setStreamHandler(
                new com.codename1.flutter.platform.EventChannel.StreamHandler() {
                    @Override
                    public void onListen(Object arguments, com.codename1.flutter.platform.EventChannel.EventSink events) {
                        listenCalls[0]++;
                    }

                    @Override
                    public void onCancel(Object arguments) {
                    }
                });
        EventChannel ch = new EventChannel(channel);
        ch.receiveBroadcastStream().listen(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
            }
        });
        ch.receiveBroadcastStream().listen(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
            }
        });
        assertEquals(2, listenCalls[0], "each receiveBroadcastStream() call is its own listen session");
    }

    @Test
    public void noPlatformHandlerMeansNoEventsEver() {
        String channel = "test/events/" + System.nanoTime();
        Stream<Object> stream = new EventChannel(channel).receiveBroadcastStream();
        final List<Object> received = new ArrayList<Object>();
        // Must not throw merely because nothing is registered yet.
        StreamSubscription<Object> sub = stream.listen(new Funcs.VoidFunc1<Object>() {
            @Override
            public void call(Object v) {
                received.add(v);
            }
        });
        assertTrue(received.isEmpty());
        sub.cancel();
    }
}
