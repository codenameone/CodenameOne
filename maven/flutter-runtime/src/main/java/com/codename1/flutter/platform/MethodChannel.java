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
package com.codename1.flutter.platform;

import com.codename1.flutter.FlutterChannelRegistry;
import dart.core.Duration;
import dart.runtime.Funcs;

/**
 * Mirrors Android's {@code io.flutter.plugin.common.MethodChannel}: the
 * "platform side" of a Flutter method channel, written exactly like porting
 * code for a classic Flutter plugin would -- {@code implements
 * MethodChannel.MethodCallHandler}, {@code result.success(...)} -- except
 * there is no transport to abstract, so the constructor takes only the
 * channel name (no {@code BinaryMessenger}).
 *
 * <p>The Dart side of the SAME channel is
 * {@code package:flutter/services.dart}'s {@code MethodChannel}, transpiled
 * to {@code com.codename1.flutter.services.MethodChannel}. Both sides are
 * matched purely by {@link #name}, through the shared in-process registry
 * ({@link com.codename1.flutter.FlutterChannelRegistry}) -- there is no
 * separate "connect" step, and either side may call
 * {@code setMethodCallHandler} first.</p>
 *
 * <p>A handler's {@link Result} may be completed later, asynchronously
 * (store it and call it from a callback, a loaded resource, a timer); the
 * channel does not require a reply before {@link MethodCallHandler#onMethodCall}
 * returns.</p>
 */
public final class MethodChannel {

    private final String name;

    public MethodChannel(String name) {
        this.name = name;
    }

    /** Registers the handler that answers calls the Dart side makes with {@code invokeMethod}. */
    public void setMethodCallHandler(MethodCallHandler handler) {
        FlutterChannelRegistry.setMethodCallHandler(name, handler);
    }

    /** Calls the handler the Dart side registered with {@code setMethodCallHandler}, ignoring any reply. */
    public void invokeMethod(String method, Object arguments) {
        invokeMethod(method, arguments, null);
    }

    /**
     * Calls the handler the Dart side registered with {@code setMethodCallHandler}.
     * {@code callback} may be {@code null} to ignore the reply, exactly like
     * the no-callback overload above but written out, matching Android's API.
     *
     * <p>The call is dispatched on a zero-length {@link dart.async.Timer}
     * rather than inline, the way {@code Completer}/{@code Timer} already
     * defer through the event loop: real Flutter never answers a platform
     * call synchronously either, and a handler that happens to reply at
     * once should not be distinguishable from one that replies later.</p>
     */
    public void invokeMethod(final String method, final Object arguments, final Result callback) {
        new dart.async.Timer(Duration.zero, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                MethodCallHandler h = FlutterChannelRegistry.methodCallHandler(name);
                Result r = callback != null ? callback : NO_OP_RESULT;
                if (h == null) {
                    r.notImplemented();
                    return;
                }
                try {
                    h.onMethodCall(new MethodCall(method, arguments), r);
                } catch (Throwable t) {
                    r.error("error", t.getMessage(), null);
                }
            }
        });
    }

    private static final Result NO_OP_RESULT = new Result() {
        @Override
        public void success(Object result) {
        }

        @Override
        public void error(String errorCode, String errorMessage, Object errorDetails) {
        }

        @Override
        public void notImplemented() {
        }
    };

    /** A handler for calls the OTHER side makes with {@code invokeMethod}. */
    public interface MethodCallHandler {
        void onMethodCall(MethodCall call, Result result);
    }

    /** The reply to one {@link MethodCallHandler#onMethodCall} -- call exactly one method, exactly once. */
    public interface Result {
        void success(Object result);

        void error(String errorCode, String errorMessage, Object errorDetails);

        void notImplemented();
    }
}
