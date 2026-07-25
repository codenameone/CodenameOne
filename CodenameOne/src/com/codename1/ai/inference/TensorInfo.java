/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.inference;

/** Immutable model input/output metadata. */
public final class TensorInfo {
    private final String name;
    private final TensorType type;
    private final int[] shape;
    private final int index;

    public TensorInfo(String name, TensorType type, int[] shape, int index) {
        this.name = name;
        this.type = type;
        this.shape = copy(shape);
        this.index = index;
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

    public int getIndex() {
        return index;
    }

    private static int[] copy(int[] value) {
        if (value == null) {
            return new int[0];
        }
        int[] out = new int[value.length];
        System.arraycopy(value, 0, out, 0, value.length);
        return out;
    }
}
