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

import com.codename1.flutter.FlutterChannelRegistry;
import com.codename1.flutter.platform.MethodCall;
import com.codename1.flutter.platform.MissingPluginException;
import com.codename1.flutter.platform.PlatformException;
import dart.async.Completer;
import dart.async.Future;
import dart.core.Duration;
import dart.runtime.DartRuntime;
import dart.runtime.Funcs;

/**
 * The transpilation target of {@code package:flutter/services.dart}'s
 * {@code MethodChannel} -- the Dart side of a platform method channel. A
 * Dart app written against the real {@code MethodChannel} transpiles
 * against this class unchanged; its "platform side" is
 * {@link com.codename1.flutter.platform.MethodChannel}, matched by
 * {@link #name} through {@link FlutterChannelRegistry}.
 *
 * <p>The optional {@code codec} constructor/method parameters are accepted
 * and ignored, for source compatibility: every value here is an in-process
 * Java object, not a byte buffer a codec would (de)serialize. See
 * {@link MethodCall} for the type mapping this collapses to identity
 * (Dart {@code int}/{@code double}/{@code String}/{@code bool}/
 * {@code List}/{@code Map}/{@code null} &lt;-&gt; Java {@code Long}/
 * {@code Double}/{@code String}/{@code Boolean}/{@code List}/{@code Map}/
 * {@code null}).</p>
 */
public final class MethodChannel {

    private final String name;

    public MethodChannel(String name) {
        this.name = name;
    }

    /** {@code codec} is accepted for source compatibility and ignored -- see the class comment. */
    public MethodChannel(String name, Object codec) {
        this(name);
    }

    public Future<Object> invokeMethod(String method) {
        return invokeMethod(method, null);
    }

    /**
     * Calls the handler registered on the platform side
     * ({@link com.codename1.flutter.platform.MethodChannel#setMethodCallHandler}),
     * completing with its result -- or with a {@link PlatformException} for
     * a {@code Result.error} reply, or a {@link MissingPluginException} when
     * no handler is registered (or one replies
     * {@code Result.notImplemented()}).
     *
     * <p>Dispatched on a zero-length {@link dart.async.Timer}, the way
     * {@code Completer}/{@code Timer} already defer through the event loop
     * rather than completing inline -- real platform calls are never
     * synchronous either.</p>
     */
    public Future<Object> invokeMethod(final String method, final Object arguments) {
        final Completer<Object> completer = new Completer<Object>();
        new dart.async.Timer(Duration.zero, new Funcs.VoidFunc0() {
            @Override
            public void call() {
                com.codename1.flutter.platform.MethodChannel.MethodCallHandler h =
                        FlutterChannelRegistry.methodCallHandler(name);
                if (h == null) {
                    completer.completeError(missingPlugin(method));
                    return;
                }
                final boolean[] replied = {false};
                com.codename1.flutter.platform.MethodChannel.Result result =
                        new com.codename1.flutter.platform.MethodChannel.Result() {
                            @Override
                            public void success(Object result) {
                                if (replied[0]) {
                                    return;
                                }
                                replied[0] = true;
                                completer.complete(result);
                            }

                            @Override
                            public void error(String errorCode, String errorMessage, Object errorDetails) {
                                if (replied[0]) {
                                    return;
                                }
                                replied[0] = true;
                                PlatformException e = new PlatformException();
                                e.code(errorCode);
                                e.message(errorMessage);
                                e.details(errorDetails);
                                completer.completeError(e);
                            }

                            @Override
                            public void notImplemented() {
                                if (replied[0]) {
                                    return;
                                }
                                replied[0] = true;
                                completer.completeError(missingPlugin(method));
                            }
                        };
                try {
                    h.onMethodCall(new MethodCall(method, arguments), result);
                } catch (Throwable t) {
                    if (!replied[0]) {
                        replied[0] = true;
                        completer.completeError(t);
                    }
                }
            }
        });
        return completer.future();
    }

    /** {@code invokeMethod} typed for a {@code List<dynamic>} result -- the result value is unchanged, just re-typed. */
    @SuppressWarnings("unchecked")
    public Future<java.util.List<Object>> invokeListMethod(String method, Object arguments) {
        return (Future<java.util.List<Object>>) (Future<?>) invokeMethod(method, arguments);
    }

    /** {@code invokeMethod} typed for a {@code Map<dynamic, dynamic>} result -- the result value is unchanged, just re-typed. */
    @SuppressWarnings("unchecked")
    public Future<java.util.Map<Object, Object>> invokeMapMethod(String method, Object arguments) {
        return (Future<java.util.Map<Object, Object>>) (Future<?>) invokeMethod(method, arguments);
    }

    private static MissingPluginException missingPlugin(String method) {
        return new MissingPluginException("No implementation found for method " + method);
    }

    /**
     * Registers the handler that answers calls the platform side makes
     * with {@code invokeMethod}; {@code null} clears it. {@code handler} is
     * a one-argument closure, {@code (MethodCall call) => ...} or
     * {@code (MethodCall call) async => ...}, transpiled to this exact
     * functional interface so the Dart closure literal target-types
     * directly (the transpiler emits a plain Java lambda for it, with no
     * wrapping -- a parameter typed plain {@code Object} is not a
     * functional interface and the generated source fails to compile).
     * Its return value may be a plain value (completes the platform
     * caller's {@code Result} with it at once) or a {@code Future}
     * (awaited first, so an {@code async} handler body works exactly like
     * the real API).
     */
    public void setMethodCallHandler(final Funcs.Func1<MethodCall, Object> handler) {
        if (handler == null) {
            FlutterChannelRegistry.setMethodCallHandler(name, null);
            return;
        }
        FlutterChannelRegistry.setMethodCallHandler(name,
                new com.codename1.flutter.platform.MethodChannel.MethodCallHandler() {
                    @Override
                    public void onMethodCall(MethodCall call,
                            final com.codename1.flutter.platform.MethodChannel.Result result) {
                        Object r;
                        try {
                            r = handler.call(call);
                        } catch (Throwable t) {
                            replyError(result, t);
                            return;
                        }
                        if (r instanceof Future) {
                            @SuppressWarnings("unchecked")
                            Future<Object> fut = (Future<Object>) r;
                            fut.then(new Funcs.VoidFunc1<Object>() {
                                @Override
                                public void call(Object v) {
                                    result.success(v);
                                }
                            }, (Object) new Funcs.VoidFunc1<Object>() {
                                @Override
                                public void call(Object e) {
                                    replyError(result, e);
                                }
                            });
                        } else {
                            result.success(r);
                        }
                    }
                });
    }

    private static void replyError(com.codename1.flutter.platform.MethodChannel.Result result, Object thrown) {
        Object e = thrown instanceof Throwable ? DartRuntime.caught((Throwable) thrown) : thrown;
        if (e instanceof PlatformException) {
            PlatformException pe = (PlatformException) e;
            result.error(pe.code(), pe.message(), pe.details());
        } else if (e instanceof Throwable) {
            result.error("error", ((Throwable) e).getMessage(), null);
        } else {
            result.error("error", DartRuntime.str(e), null);
        }
    }
}
