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

import dart.core.DartList;
import dart.core.DartMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

    /// A value the Java side hands to Dart, in the types transpiled Dart code
    /// uses: Dart's `int` is a Java `Long` and its `double` a `Double`, and its
    /// lists and maps are `DartList`/`DartMap`. Flutter's codec does this
    /// conversion on the real platform channel; here nothing is serialized, so
    /// without it a Java handler answering `result.success(87)` -- an `Integer`,
    /// what ported Kotlin `Int` code produces -- reached Dart code expecting a
    /// `Long`, and a plain `java.util.List` reached code calling Dart list
    /// methods. On iOS a cast is unchecked (ParparVM), so that is a native crash,
    /// not an exception. Lists and maps are converted recursively; every other
    /// value, including `null`, `String`, `Boolean` and the Dart types themselves,
    /// passes through unchanged.
    public static Object toDart(Object value) {
        if (value instanceof Integer || value instanceof Short || value instanceof Byte) {
            return Long.valueOf(((Number) value).longValue());
        }
        if (value instanceof Float) {
            return Double.valueOf(((Float) value).doubleValue());
        }
        if (value instanceof List && !(value instanceof DartList)) {
            DartList<Object> out = new DartList<Object>();
            for (Object e : (List<?>) value) {
                out.add(toDart(e));
            }
            return out;
        }
        if (value instanceof Map && !(value instanceof DartMap)) {
            DartMap<Object, Object> out = new DartMap<Object, Object>();
            Iterator<? extends Map.Entry<?, ?>> it = ((Map<?, ?>) value).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> e = it.next();
                out.put(toDart(e.getKey()), toDart(e.getValue()));
            }
            return out;
        }
        return value;
    }
}
