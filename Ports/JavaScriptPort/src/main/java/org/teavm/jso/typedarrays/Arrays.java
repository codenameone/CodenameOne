/*
 * Copyright (c) 2026 Codename One and contributors.
 */
package org.teavm.jso.typedarrays;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSBody;

public interface ArrayBuffer extends JSObject {
    int getByteLength();
    
    @JSBody(params = {"length"}, script = "return new ArrayBuffer(length)")
    static ArrayBuffer create(int length) { return null; }
}

public interface ArrayBufferView extends JSObject {
    ArrayBuffer getBuffer();
    int getByteOffset();
    int getByteLength();
}

public interface Uint8Array extends ArrayBufferView {
    int getLength();
    byte get(int index);
    void set(int index, byte value);
    void set(Uint8Array source, int offset);
    ArrayBuffer getBuffer();
    
    @JSBody(params = {"buffer"}, script = "return new Uint8Array(buffer)")
    static Uint8Array create(ArrayBuffer buffer) { return null; }
    
    @JSBody(params = {"buffer", "offset", "length"}, script = "return new Uint8Array(buffer, offset, length)")
    static Uint8Array create(ArrayBuffer buffer, int offset, int length) { return null; }
}

public interface Uint8ClampedArray extends Uint8Array {
    @JSBody(params = {"buffer"}, script = "return new Uint8ClampedArray(buffer)")
    static Uint8ClampedArray create(ArrayBuffer buffer) { return null; }
}

public interface Int16Array extends ArrayBufferView {
    int getLength();
    short get(int index);
    void set(int index, short value);
}

public interface Int32Array extends ArrayBufferView {
    int getLength();
    int get(int index);
    void set(int index, int value);
}

public interface Float32Array extends ArrayBufferView {
    int getLength();
    float get(int index);
    void set(int index, float value);
}

public interface Float64Array extends ArrayBufferView {
    int getLength();
    double get(int index);
    void set(int index, double value);
}