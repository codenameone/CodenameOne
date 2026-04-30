/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package com.codename1.html5.interop;

/**
 * Callback interface for async operations.
 * This is a replacement for TeaVM's AsyncCallback.
 */
public interface AsyncCallback<T> {
    void complete(T result);
    void error(Throwable e);
}