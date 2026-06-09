/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */
package com.codename1.impl.platform.js;

/**
 * Host bridge that dispatches Codename One {@code NativeInterface} method calls
 * to their JavaScript implementation registered in {@code cn1_native_interfaces}.
 *
 * <p>The generated {@code <Interface>Impl} classes (emitted by the JavaScript
 * builder) delegate every interface method to one of the {@code call*} natives
 * below. Because this class lives in {@code com.codename1.impl.platform.js}, the
 * translator categorizes these natives as HOST_HOOK: the worker suspends and the
 * call is replayed on the <em>main thread</em> (via {@code browser_bridge.js}),
 * where the developer-authored JS stub runs with full DOM access and completes
 * the call through its callback. The worker resumes with the returned value.</p>
 *
 * <p>This preserves the existing JS native-interface impl format
 * ({@code cn1_native_interfaces["<pkg>_<Iface>"]["<method>_<paramTypes>"](args..., callback)})
 * so existing stubs work unchanged.</p>
 *
 * <p>{@code iface} is the interface class name with dots replaced by underscores
 * (the {@code cn1_native_interfaces} registry key) and {@code method} is the
 * trailing-underscore method key (e.g. {@code "isDarkMode_"}). {@code args}
 * holds the (boxed) Java arguments, or an empty array for a no-arg method.</p>
 */
public final class NativeInterfaceBridge {
    private NativeInterfaceBridge() {
    }

    public static native boolean callBoolean(String iface, String method, Object[] args);

    public static native int callInt(String iface, String method, Object[] args);

    public static native long callLong(String iface, String method, Object[] args);

    public static native double callDouble(String iface, String method, Object[] args);

    public static native float callFloat(String iface, String method, Object[] args);

    public static native byte callByte(String iface, String method, Object[] args);

    public static native short callShort(String iface, String method, Object[] args);

    public static native char callChar(String iface, String method, Object[] args);

    public static native String callString(String iface, String method, Object[] args);

    public static native Object callObject(String iface, String method, Object[] args);

    public static native void callVoid(String iface, String method, Object[] args);
}
