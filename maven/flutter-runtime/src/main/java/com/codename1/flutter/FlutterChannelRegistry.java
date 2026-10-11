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
package com.codename1.flutter;

import com.codename1.flutter.platform.EventChannel;
import com.codename1.flutter.platform.MethodChannel;

import java.util.HashMap;
import java.util.Map;

/**
 * The in-process directory platform channels are matched by name through.
 *
 * <p>There is exactly one Flutter runtime inside a Codename One app --
 * transpiled Dart and hand-written Java run in the same classes, the same
 * thread, with no transport between them -- so "sending" a platform message
 * is just looking up the other side's handler by channel name and calling
 * it directly. Either side may register first: {@code
 * com.codename1.flutter.services.MethodChannel} (the Dart-visible stub
 * binding) and {@code com.codename1.flutter.platform.MethodChannel} (the
 * hand-written Java porting target, mirroring Android's
 * {@code io.flutter.plugin.common.MethodChannel}) both read and write this
 * same map, keyed by the identical {@code String} channel name real Flutter
 * uses.</p>
 *
 * <p>Not part of the public API: application code never calls this class
 * directly, it only constructs a {@code MethodChannel}/{@code EventChannel}
 * on whichever side it is writing.</p>
 */
public final class FlutterChannelRegistry {

    private FlutterChannelRegistry() {
    }

    private static final Map<String, MethodChannel.MethodCallHandler> METHOD_HANDLERS =
            new HashMap<String, MethodChannel.MethodCallHandler>();
    private static final Map<String, EventChannel.StreamHandler> STREAM_HANDLERS =
            new HashMap<String, EventChannel.StreamHandler>();

    /** Registers (or, with {@code handler == null}, clears) the method handler for {@code channel}. */
    public static void setMethodCallHandler(String channel, MethodChannel.MethodCallHandler handler) {
        if (handler == null) {
            METHOD_HANDLERS.remove(channel);
        } else {
            METHOD_HANDLERS.put(channel, handler);
        }
    }

    /** The method handler registered for {@code channel}, or {@code null} if none. */
    public static MethodChannel.MethodCallHandler methodCallHandler(String channel) {
        return METHOD_HANDLERS.get(channel);
    }

    /** Registers (or, with {@code handler == null}, clears) the stream handler for {@code channel}. */
    public static void setStreamHandler(String channel, EventChannel.StreamHandler handler) {
        if (handler == null) {
            STREAM_HANDLERS.remove(channel);
        } else {
            STREAM_HANDLERS.put(channel, handler);
        }
    }

    /** The stream handler registered for {@code channel}, or {@code null} if none. */
    public static EventChannel.StreamHandler streamHandler(String channel) {
        return STREAM_HANDLERS.get(channel);
    }
}
