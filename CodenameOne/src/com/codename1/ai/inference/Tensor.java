/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.inference;

/** Named tensor value passed to or returned from an inference session. */
public final class Tensor {
    private final String name;
    private final TensorType type;
    private final int[] shape;
    private final Object data;

    public Tensor(String name, TensorType type, int[] shape, Object data) {
        if (type == null || data == null) {
            throw new NullPointerException("type and data are required");
        }
        validateData(type, data);
        int count = elementCount(shape);
        if (count >= 0 && count != dataLength(data)) {
            throw new IllegalArgumentException("Tensor shape does not match data length");
        }
        this.name = name;
        this.type = type;
        this.shape = copy(shape);
        this.data = copyData(data);
    }

    public static Tensor floats(String name, int[] shape, float[] data) {
        return new Tensor(name, TensorType.FLOAT32, shape, data);
    }

    public static Tensor ints(String name, int[] shape, int[] data) {
        return new Tensor(name, TensorType.INT32, shape, data);
    }

    public static Tensor bytes(String name, TensorType type, int[] shape, byte[] data) {
        return new Tensor(name, type, shape, data);
    }

    public String getName() {
        return name;
    }

    public TensorType getType() {
        return type;
    }

    public int[] getShape() {
        return copy(shape);
    }

    public Object getData() {
        return copyData(data);
    }

    private static void validateData(TensorType type, Object data) {
        boolean valid;
        switch (type) {
            case FLOAT32: valid = data instanceof float[]; break;
            case INT32: valid = data instanceof int[]; break;
            case INT64: valid = data instanceof long[]; break;
            case UINT8:
            case INT8:
            case BOOL: valid = data instanceof byte[]; break;
            default: valid = false;
        }
        if (!valid) {
            throw new IllegalArgumentException("Data array does not match " + type);
        }
    }

    private static int elementCount(int[] shape) {
        if (shape == null || shape.length == 0) {
            return 1;
        }
        int out = 1;
        for (int i = 0; i < shape.length; i++) {
            if (shape[i] < 0) {
                return -1;
            }
            out *= shape[i];
        }
        return out;
    }

    private static int dataLength(Object value) {
        if (value instanceof float[]) return ((float[]) value).length;
        if (value instanceof int[]) return ((int[]) value).length;
        if (value instanceof long[]) return ((long[]) value).length;
        if (value instanceof byte[]) return ((byte[]) value).length;
        throw new IllegalArgumentException("Unsupported tensor data array");
    }

    private static int[] copy(int[] value) {
        if (value == null) return new int[0];
        int[] out = new int[value.length];
        System.arraycopy(value, 0, out, 0, value.length);
        return out;
    }

    private static Object copyData(Object value) {
        int length = dataLength(value);
        if (value instanceof float[]) {
            float[] out = new float[length];
            System.arraycopy(value, 0, out, 0, length);
            return out;
        }
        if (value instanceof int[]) {
            int[] out = new int[length];
            System.arraycopy(value, 0, out, 0, length);
            return out;
        }
        if (value instanceof long[]) {
            long[] out = new long[length];
            System.arraycopy(value, 0, out, 0, length);
            return out;
        }
        if (value instanceof byte[]) {
            byte[] out = new byte[length];
            System.arraycopy(value, 0, out, 0, length);
            return out;
        }
        throw new IllegalArgumentException("Unsupported tensor data array");
    }
}
