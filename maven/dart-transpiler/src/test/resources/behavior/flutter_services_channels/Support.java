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
// Fixture-only Java test harness for flutter_services_channels (see
// BehaviorTest.runCase and support.stub.dart). A plain Dart `import` of a
// default-package Java type does not compile (Java forbids importing one at
// all), so this needs a real package -- TestSupport.writeJavaSupportFile
// places the file under it by reading this declaration, independent of
// where the fixture directory itself lives.
//
// This plays the "platform side" a real Flutter plugin author would write:
// installJavaMethodHandler/installJavaStreamHandler register Java handlers the
// Dart side (com.codename1.flutter.services.*) calls into, and
// invokeJavaSideCall/fireEvent are this test's stand-in for native code
// calling the Dart side (com.codename1.flutter.platform.*) -- blocking with a
// CountDownLatch is fine here ONLY because this file is compiled by plain
// javac and run on a plain JVM; it is never translated.
package com.codename1.dart.behaviortest.flutterservices;

import com.codename1.flutter.platform.EventChannel;
import com.codename1.flutter.platform.MethodCall;
import com.codename1.flutter.platform.MethodChannel;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class Support {

    private Support() {
    }

    public static void installJavaMethodHandler(String channel) {
        new MethodChannel(channel).setMethodCallHandler(new MethodChannel.MethodCallHandler() {
            @Override
            public void onMethodCall(MethodCall call, MethodChannel.Result result) {
                if ("echo".equals(call.method())) {
                    result.success("echo:" + call.arguments());
                } else if ("fail".equals(call.method())) {
                    result.error("E_FAIL", "it failed", "the detail");
                } else {
                    result.notImplemented();
                }
            }
        });
    }

    /**
     * Calls the Dart-registered handler on {@code channel} and blocks until it
     * replies -- Dart's {@code main()} calls this as a plain (non-async)
     * external function, so there is no Future to hand back to it; blocking
     * here is what stands in for the real native call being synchronous from
     * Dart's point of view in this test.
     */
    public static Object invokeJavaSideCall(final String channel, final String method, final Object arguments) {
        final Object[] success = {null};
        final RuntimeException[] failure = {null};
        final CountDownLatch done = new CountDownLatch(1);
        new MethodChannel(channel).invokeMethod(method, arguments, new MethodChannel.Result() {
            @Override
            public void success(Object result) {
                success[0] = result;
                done.countDown();
            }

            @Override
            public void error(String errorCode, String errorMessage, Object errorDetails) {
                failure[0] = new RuntimeException(errorCode + ":" + errorMessage);
                done.countDown();
            }

            @Override
            public void notImplemented() {
                failure[0] = new RuntimeException("notImplemented");
                done.countDown();
            }
        });
        boolean arrived;
        try {
            arrived = done.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (!arrived) {
            throw new RuntimeException("timed out waiting for " + channel + "#" + method);
        }
        if (failure[0] != null) {
            throw failure[0];
        }
        return success[0];
    }

    private static EventChannel.EventSink sink;
    private static int cancelCount;

    public static void installJavaStreamHandler(String channel) {
        new EventChannel(channel).setStreamHandler(new EventChannel.StreamHandler() {
            @Override
            public void onListen(Object arguments, EventChannel.EventSink events) {
                sink = events;
            }

            @Override
            public void onCancel(Object arguments) {
                sink = null;
                cancelCount++;
            }
        });
    }

    public static void fireEvent(String value) {
        if (sink != null) {
            sink.success(value);
        }
    }

    public static int cancelCount() {
        return cancelCount;
    }
}
