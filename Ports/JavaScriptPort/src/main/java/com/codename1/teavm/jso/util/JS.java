/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.teavm.jso.util;

import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.core.JSArray;
import com.codename1.html5.js.core.JSString;
import com.codename1.html5.js.typedarrays.ArrayBuffer;
import com.codename1.html5.js.typedarrays.ArrayBufferView;
import com.codename1.html5.js.typedarrays.Int16Array;
import com.codename1.html5.js.typedarrays.Int32Array;
import com.codename1.html5.js.typedarrays.Uint8Array;

/**
 *
 * @author shannah
 */
public class JS {
    public static JSObject getGlobal() {
        return Window.current();
    }
    
    public static JSType getType(JSObject o) {
        String v = getTypeImpl(o);
        if ("string".equals(v)) {
            return JSType.STRING;
        } else if ("object".equals(v)) {
            return JSType.OBJECT;
        } else if ("boolean".equals(v)) {
            return JSType.BOOLEAN;
        } else if ("number".equals(v)) {
            return JSType.NUMBER;
        } else if ("function".equals(v)) {
            return JSType.FUNCTION;
        } else {
            return JSType.UNDEFINED;
        }
    }
    
    @JSBody(params={"o"}, script="return typeof o")
    private static native String getTypeImpl(JSObject o);
    
    @JSBody(params={"o"}, script="return (o === undefined)")
    public static native boolean isUndefined(JSObject o);
    
    public static boolean isUndefined(Object o) {
        return o == null || (o instanceof JSObject && isUndefined((JSObject)o));
    }
    
    public static String unwrapString(JSObject o) {
        if (isUndefined(o)) {
            return null;
        } else {
            return ((JSString)o).stringValue();
        }
    }
    
    @JSBody(params={"o"}, script="return o ? true : false")
    public native static boolean unwrapBoolean(JSObject o);
    
    @JSBody(params={"o"}, script="return Number(o)")
    public native static double unwrapDouble(JSObject o);
    
    @JSBody(params={"o"}, script="return Number(o)")
    public native static int unwrapInt(JSObject o);
    
    
    public static String[] unwrapStringArray(JSArray<JSString> input) {
        int len = input.getLength();
        String[] out = new String[len];
        for (int i=0; i<len; i++) {
            JSString el = input.get(i);
            out[i] = el != null && !JS.isUndefined(el) ? el.stringValue() : null;
        }
        return out;
    }
    
    public static byte[] unwrapByteArray(JSObject arr) {
        if (isArray(arr)) {
            JSArray jarr = (JSArray)arr;
            int len = jarr.getLength();
            byte[] out = new byte[len];
            
for (int i=0; i<len; i++) {
            out[i] = (byte)(unwrapInt((JSObject)jarr.get(i)) & 0xff);
        }
            return out;
        } else if (isArrayBuffer(arr)) {
            Uint8Array uarr = Uint8Array.create((ArrayBuffer)arr);
            return unwrapByteArray(uarr);
        } else if (isTypedArray(arr)) {
            ArrayBufferView av = (ArrayBufferView)arr;
            Uint8Array uarr = Uint8Array.create(av.getBuffer());
            int len = uarr.getLength();
            byte[] out = new byte[len];
            for (int i=0; i<len; i++) {
                out[i] = (byte)(uarr.get(i) & 0xff);
            }
            return out;
        } else {
            return null;
        }
    }
    
    public static int[] unwrapIntArray(JSObject arr) {
        if (isArray(arr)) {
            JSArray jarr = (JSArray)arr;
            int len = jarr.getLength();
            int[] out = new int[len];
            
for (int i=0; i<len; i++) {
            out[i] = (int)(unwrapInt((JSObject)jarr.get(i)) );
        }
            return out;
        } else if (isArrayBuffer(arr)) {
            Int32Array uarr = Int32Array.create((ArrayBuffer)arr);
            return unwrapIntArray(uarr);
        } else if (isTypedArray(arr)) {
            ArrayBufferView av = (ArrayBufferView)arr;
            Int32Array uarr = Int32Array.create(av.getBuffer());
            int len = uarr.getLength();
            int[] out = new int[len];
            for (int i=0; i<len; i++) {
                out[i] = (int)(uarr.get(i));
            }
            return out;
        } else {
            return null;
        }
    }
    
    public static long[] unwrapLongArray(JSObject arr) {
        int[] ints = unwrapIntArray(arr);
        if (ints == null) {
            return null;
        }
        int len = ints.length;
        long[] out = new long[len];
        for (int i=0; i<len; i++) {
            out[i] = ints[i];
        }
        return out;
    }
    
    public static float[] unwrapFloatArray(JSObject arr) {
        double[] dubs = unwrapDoubleArray(arr);
        if (dubs == null) {
            return null;
        }
        int len = dubs.length;
        float[] out = new float[len];
        for (int i=0; i<len; i++) {
            out[i] = (float)dubs[i];
        }
        return out;
    }
    
    public static short[] unwrapShortArray(JSObject arr) {
        if (isArray(arr)) {
            JSArray jarr = (JSArray)arr;
            int len = jarr.getLength();
            short[] out = new short[len];
            
for (int i=0; i<len; i++) {
            out[i] = (short)(unwrapInt((JSObject)jarr.get(i)) );
        }
            return out;
        } else if (isArrayBuffer(arr)) {
            Int16Array uarr = Int16Array.create((ArrayBuffer)arr);
            return unwrapShortArray(uarr);
        } else if (isTypedArray(arr)) {
            ArrayBufferView av = (ArrayBufferView)arr;
            Int16Array uarr = Int16Array.create(av.getBuffer());
            int len = uarr.getLength();
            short[] out = new short[len];
            for (short i=0; i<len; i++) {
                out[i] = (short)(uarr.get(i));
            }
            return out;
        } else {
            return null;
        }
    }
    
    public static double[] unwrapDoubleArray(JSObject arr) {
        if (isArray(arr)) {
            JSArray jarr = (JSArray)arr;
            int len = jarr.getLength();
            double[] out = new double[len];
            
for (int i=0; i<len; i++) {
            out[i] = (double)(unwrapDouble((JSObject)jarr.get(i)) );
        }
            return out;
        } 
        return null;
    }
    
    public static char[] unwrapCharArray(JSObject arr) {
        if (isArray(arr)) {
            JSArray jarr = (JSArray)arr;
            int len = jarr.getLength();
            char[] out = new char[len];
            
for (int i=0; i<len; i++) {
            out[i] = (char)(unwrapString((JSObject)jarr.get(i)).charAt(0) );
        }
            return out;
        } else if (JSType.STRING.equals(getType(arr))) {
            return unwrapString(arr).toCharArray();
        }
        return null;
    }
    
    public static boolean[] unwrapBooleanArray(JSObject arr) {
        if (isArray(arr)) {
            JSArray jarr = (JSArray)arr;
            int len = jarr.getLength();
            boolean[] out = new boolean[len];
            
for (int i=0; i<len; i++) {
            out[i] = (boolean)(unwrapBoolean((JSObject)jarr.get(i)) );
        }
            return out;
        } 
        return null;
    }
    
    @JSBody(params={"o"}, script="return o && o.constructor === Array")
    public native static boolean isArray(JSObject o);
    
    @JSBody(params={"o"}, script="return o && o instanceof ArrayBuffer")
    public native static boolean isArrayBuffer(JSObject o);
    
    @JSBody(params={"o"}, script="return o && o instanceof TypedArray")
    public native static boolean isTypedArray(JSObject o);
}
