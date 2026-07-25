/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.vision;

/** Failure reported by an on-device vision backend. */
public class VisionException extends RuntimeException {
    public static final int UNSUPPORTED = 1;
    public static final int INVALID_IMAGE = 2;
    public static final int MODEL_UNAVAILABLE = 3;
    public static final int CANCELLED = 4;
    public static final int BACKEND_ERROR = 5;

    private final int code;

    public VisionException(int code, String message) {
        super(message);
        this.code = code;
    }

    public VisionException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
