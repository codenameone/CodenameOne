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

import com.codename1.flutter.platform.MissingPluginException;
import com.codename1.flutter.platform.PlatformException;
import dart.async.Await;
import dart.async.Future;
import dart.runtime.Funcs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// com.codename1.flutter.services.MethodChannel (the Dart side) and
/// com.codename1.flutter.platform.MethodChannel (the Java side) matched by
/// channel name through FlutterChannelRegistry, in both directions. Every
/// case runs off the main thread (@Timeout SEPARATE_THREAD) because a reply
/// is dispatched through a zero-length Timer, never inline -- see both
/// classes' class comments.
public class MethodChannelTest {

    // --- Dart invokes, a Java (platform-side) handler answers -------------

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void dartInvokeReachesTheJavaHandlerAndCompletesWithItsResult() {
        String channel = "test/channel/" + System.nanoTime();
        new com.codename1.flutter.platform.MethodChannel(channel).setMethodCallHandler(
                new com.codename1.flutter.platform.MethodChannel.MethodCallHandler() {
                    @Override
                    public void onMethodCall(com.codename1.flutter.platform.MethodCall call,
                            com.codename1.flutter.platform.MethodChannel.Result result) {
                        assertEquals("greet", call.method());
                        assertEquals("world", call.arguments());
                        result.success("hello world");
                    }
                });
        MethodChannel dart = new MethodChannel(channel);
        Future<Object> f = dart.invokeMethod("greet", "world");
        // getNow() reads a settled future's value without blocking (null while
        // pending); asserting on it here is how a test outside dart.async can
        // observe "not synchronous" without Future's package-private isDone().
        assertNull(f.getNow(), "the reply must not be synchronous");
        assertEquals("hello world", Await.await$(f));
    }

    /// Java values reach Dart in Dart's types: an Integer (what ported Kotlin
    /// `Int` code answers with) as a Long, a java.util.List as a DartList with
    /// its elements converted too. See FlutterChannelRegistry.toDart.
    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void javaValuesReachDartInDartTypes() {
        String channel = "test/channel/" + System.nanoTime();
        new com.codename1.flutter.platform.MethodChannel(channel).setMethodCallHandler(
                new com.codename1.flutter.platform.MethodChannel.MethodCallHandler() {
                    @Override
                    public void onMethodCall(com.codename1.flutter.platform.MethodCall call,
                            com.codename1.flutter.platform.MethodChannel.Result result) {
                        if ("level".equals(call.method())) {
                            result.success(Integer.valueOf(87));
                        } else {
                            java.util.List<Object> l = new java.util.ArrayList<Object>();
                            l.add(Integer.valueOf(1));
                            l.add(Float.valueOf(2.5f));
                            result.success(l);
                        }
                    }
                });
        MethodChannel dart = new MethodChannel(channel);
        assertEquals(Long.valueOf(87), Await.await$(dart.invokeMethod("level", null)));
        Object list = Await.await$(dart.invokeMethod("list", null));
        assertTrue(list instanceof dart.core.DartList, "a Java List must arrive as a DartList");
        assertEquals(Long.valueOf(1), ((java.util.List<?>) list).get(0));
        assertEquals(Double.valueOf(2.5), ((java.util.List<?>) list).get(1));
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void javaErrorReplyBecomesAPlatformException() {
        String channel = "test/channel/" + System.nanoTime();
        new com.codename1.flutter.platform.MethodChannel(channel).setMethodCallHandler(
                new com.codename1.flutter.platform.MethodChannel.MethodCallHandler() {
                    @Override
                    public void onMethodCall(com.codename1.flutter.platform.MethodCall call,
                            com.codename1.flutter.platform.MethodChannel.Result result) {
                        result.error("E_BAD", "bad input", "the details");
                    }
                });
        Future<Object> f = new MethodChannel(channel).invokeMethod("x", null);
        RuntimeException thrown = assertThrows(RuntimeException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                Await.await$(f);
            }
        });
        PlatformException e = (PlatformException) thrown;
        assertEquals("E_BAD", e.code());
        assertEquals("bad input", e.message());
        assertEquals("the details", e.details());
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void noHandlerCompletesWithMissingPluginException() {
        String channel = "test/channel/" + System.nanoTime();
        Future<Object> f = new MethodChannel(channel).invokeMethod("x", null);
        RuntimeException thrown = assertThrows(RuntimeException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                Await.await$(f);
            }
        });
        assertTrue(thrown instanceof MissingPluginException, "expected MissingPluginException, got " + thrown);
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void notImplementedCompletesWithMissingPluginExceptionToo() {
        String channel = "test/channel/" + System.nanoTime();
        new com.codename1.flutter.platform.MethodChannel(channel).setMethodCallHandler(
                new com.codename1.flutter.platform.MethodChannel.MethodCallHandler() {
                    @Override
                    public void onMethodCall(com.codename1.flutter.platform.MethodCall call,
                            com.codename1.flutter.platform.MethodChannel.Result result) {
                        result.notImplemented();
                    }
                });
        final Future<Object> f = new MethodChannel(channel).invokeMethod("x", null);
        assertThrows(MissingPluginException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                Await.await$(f);
            }
        });
    }

    // --- Java invokes, a Dart (services-side) handler answers -------------

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void javaInvokeReachesTheDartHandlerAndRepliesWithItsValue() throws Exception {
        String channel = "test/channel/" + System.nanoTime();
        new MethodChannel(channel).setMethodCallHandler(
                new Funcs.Func1<com.codename1.flutter.platform.MethodCall, Object>() {
                    @Override
                    public Object call(com.codename1.flutter.platform.MethodCall call) {
                        assertEquals("add", call.method());
                        return "got:" + call.arguments();
                    }
                });
        final Object[] replied = {null};
        final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        new com.codename1.flutter.platform.MethodChannel(channel).invokeMethod("add", "42",
                new com.codename1.flutter.platform.MethodChannel.Result() {
                    @Override
                    public void success(Object result) {
                        replied[0] = result;
                        done.countDown();
                    }

                    @Override
                    public void error(String errorCode, String errorMessage, Object errorDetails) {
                        done.countDown();
                    }

                    @Override
                    public void notImplemented() {
                        done.countDown();
                    }
                });
        assertTrue(done.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals("got:42", replied[0]);
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void aDartHandlerReturningAFutureIsAwaitedBeforeReplying() throws Exception {
        String channel = "test/channel/" + System.nanoTime();
        final dart.async.Completer<Object> gate = new dart.async.Completer<Object>();
        new MethodChannel(channel).setMethodCallHandler(
                new Funcs.Func1<com.codename1.flutter.platform.MethodCall, Object>() {
                    @Override
                    public Object call(com.codename1.flutter.platform.MethodCall call) {
                        return gate.future();
                    }
                });
        final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        final Object[] replied = {null};
        new com.codename1.flutter.platform.MethodChannel(channel).invokeMethod("x", null,
                new com.codename1.flutter.platform.MethodChannel.Result() {
                    @Override
                    public void success(Object result) {
                        replied[0] = result;
                        done.countDown();
                    }

                    @Override
                    public void error(String errorCode, String errorMessage, Object errorDetails) {
                        done.countDown();
                    }

                    @Override
                    public void notImplemented() {
                        done.countDown();
                    }
                });
        // The platform caller must not be answered until the Dart handler's
        // own future settles -- give the dispatch a moment, then confirm it
        // has NOT replied yet before completing the gate.
        assertFalse(done.await(300, java.util.concurrent.TimeUnit.MILLISECONDS));
        gate.complete("async result");
        assertTrue(done.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals("async result", replied[0]);
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void aThrowingDartHandlerRepliesWithError() throws Exception {
        String channel = "test/channel/" + System.nanoTime();
        new MethodChannel(channel).setMethodCallHandler(
                new Funcs.Func1<com.codename1.flutter.platform.MethodCall, Object>() {
                    @Override
                    public Object call(com.codename1.flutter.platform.MethodCall call) {
                        PlatformException e = new PlatformException();
                        e.code("E_THROWN");
                        e.message("thrown by the handler");
                        throw e;
                    }
                });
        final String[] codeSeen = {null};
        final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        new com.codename1.flutter.platform.MethodChannel(channel).invokeMethod("x", null,
                new com.codename1.flutter.platform.MethodChannel.Result() {
                    @Override
                    public void success(Object result) {
                        done.countDown();
                    }

                    @Override
                    public void error(String errorCode, String errorMessage, Object errorDetails) {
                        codeSeen[0] = errorCode;
                        done.countDown();
                    }

                    @Override
                    public void notImplemented() {
                        done.countDown();
                    }
                });
        assertTrue(done.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertEquals("E_THROWN", codeSeen[0]);
    }

    @Test
    @Timeout(value = 10, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
    public void noDartHandlerRepliesNotImplemented() throws Exception {
        String channel = "test/channel/" + System.nanoTime();
        final boolean[] notImplementedCalled = {false};
        final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);
        new com.codename1.flutter.platform.MethodChannel(channel).invokeMethod("x", null,
                new com.codename1.flutter.platform.MethodChannel.Result() {
                    @Override
                    public void success(Object result) {
                        done.countDown();
                    }

                    @Override
                    public void error(String errorCode, String errorMessage, Object errorDetails) {
                        done.countDown();
                    }

                    @Override
                    public void notImplemented() {
                        notImplementedCalled[0] = true;
                        done.countDown();
                    }
                });
        assertTrue(done.await(5, java.util.concurrent.TimeUnit.SECONDS));
        assertTrue(notImplementedCalled[0]);
    }

    @Test
    public void settingTheHandlerToNullClearsIt() {
        String channel = "test/channel/" + System.nanoTime();
        new MethodChannel(channel).setMethodCallHandler(
                new Funcs.Func1<com.codename1.flutter.platform.MethodCall, Object>() {
                    @Override
                    public Object call(com.codename1.flutter.platform.MethodCall call) {
                        return "v";
                    }
                });
        assertTrue(com.codename1.flutter.FlutterChannelRegistry.methodCallHandler(channel) != null);
        new MethodChannel(channel).setMethodCallHandler(null);
        assertNull(com.codename1.flutter.FlutterChannelRegistry.methodCallHandler(channel));
    }
}
