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
package com.codename1.impl.platform.js;

/**
 * Host bridge that dispatches Codename One {@code NativeInterface} method calls
 * to their JavaScript implementation registered in {@code cn1_native_interfaces}.
 *
 * <p>The generated {@code <Interface>Impl} classes (emitted by the JavaScript
 * builder) delegate every interface method to one of the {@code call*} natives
 * below, picked by the method's return type. These natives are runtime-
 * implemented in {@code parparvm_runtime.js}: the worker suspends, the call is
 * replayed on the <em>main thread</em> (via {@code browser_bridge.js}) where the
 * developer-authored JS stub runs with full DOM access and completes the call
 * through its callback, and the worker resumes with the result coerced to the
 * declared Java type.</p>
 *
 * <p>Supported types mirror {@code NativeInterface}: all primitives, {@code String},
 * primitive arrays plus {@code String[]} (via {@link #callArray}), and
 * {@code com.codename1.ui.PeerComponent} (routed through {@link #callObject}).</p>
 *
 * <p>{@code iface} is the interface class name with dots replaced by underscores
 * (the {@code cn1_native_interfaces} registry key), {@code method} is the
 * trailing-underscore method key (e.g. {@code "isDarkMode_"}), and {@code args}
 * holds the (boxed) Java arguments, or an empty array for a no-arg method.</p>
 */
public final class NativeInterfaceBridge {
    private NativeInterfaceBridge() {
    }

    public static native boolean callBoolean(String iface, String method, Object[] args);

    public static native byte callByte(String iface, String method, Object[] args);

    public static native short callShort(String iface, String method, Object[] args);

    public static native int callInt(String iface, String method, Object[] args);

    public static native char callChar(String iface, String method, Object[] args);

    public static native long callLong(String iface, String method, Object[] args);

    public static native float callFloat(String iface, String method, Object[] args);

    public static native double callDouble(String iface, String method, Object[] args);

    public static native String callString(String iface, String method, Object[] args);

    public static native Object callObject(String iface, String method, Object[] args);

    public static native void callVoid(String iface, String method, Object[] args);

    /**
     * Array-returning call. {@code componentToken} identifies the element type so
     * the runtime can build the correctly-typed Java array: {@code "JAVA_INT"},
     * {@code "JAVA_BYTE"}, {@code "JAVA_LONG"}, {@code "JAVA_DOUBLE"},
     * {@code "JAVA_FLOAT"}, {@code "JAVA_BOOLEAN"}, {@code "JAVA_CHAR"},
     * {@code "JAVA_SHORT"} or {@code "java_lang_String"}. The caller casts the
     * result to the concrete array type.
     */
    public static native Object callArray(String iface, String method, Object[] args, String componentToken);
}
