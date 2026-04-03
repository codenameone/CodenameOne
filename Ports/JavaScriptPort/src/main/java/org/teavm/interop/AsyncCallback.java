/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 */
package org.teavm.interop;

public interface AsyncCallback<T> {
    void complete(T result);
    void error(Throwable t);
}