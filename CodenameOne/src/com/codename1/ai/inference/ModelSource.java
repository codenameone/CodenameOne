/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.inference;

/** A `.tflite` model supplied as bytes, a filesystem path, or an app resource. */
public final class ModelSource {
    public static final int BYTES = 1;
    public static final int FILE = 2;
    public static final int RESOURCE = 3;

    private final int kind;
    private final byte[] bytes;
    private final String path;

    private ModelSource(int kind, byte[] bytes, String path) {
        this.kind = kind;
        this.bytes = bytes;
        this.path = path;
    }

    public static ModelSource bytes(byte[] value) {
        if (value == null || value.length == 0) {
            throw new IllegalArgumentException("Model bytes must not be empty");
        }
        byte[] copy = new byte[value.length];
        System.arraycopy(value, 0, copy, 0, value.length);
        return new ModelSource(BYTES, copy, null);
    }

    public static ModelSource file(String path) {
        return named(FILE, path);
    }

    public static ModelSource resource(String path) {
        return named(RESOURCE, path);
    }

    private static ModelSource named(int kind, String path) {
        if (path == null || path.length() == 0) {
            throw new IllegalArgumentException("Model path must not be empty");
        }
        return new ModelSource(kind, null, path);
    }

    public int getKind() {
        return kind;
    }

    public byte[] getBytes() {
        if (bytes == null) return null;
        byte[] out = new byte[bytes.length];
        System.arraycopy(bytes, 0, out, 0, bytes.length);
        return out;
    }

    public String getPath() {
        return path;
    }
}
