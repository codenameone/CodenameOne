/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.channel;

/// A marketplace call that did not succeed. `retryable` failures (network,
/// 429, 5xx, an expired token) are tried again with backoff; the others
/// (a SKU the marketplace does not know, a malformed request) wait for the
/// merchant but are still retried, slowly, in case the merchant fixed it on
/// the marketplace's side.
public class ChannelException extends Exception {
    private static final long serialVersionUID = 1L;
    private final int status;
    private final boolean retryable;

    public ChannelException(int status, boolean retryable, String message) {
        super(message);
        this.status = status;
        this.retryable = retryable;
    }

    /// The HTTP status, or 0 when none was answered.
    public int getStatus() {
        return status;
    }

    public boolean isRetryable() {
        return retryable;
    }

    /// Whether the access token was refused, so a refresh may fix it.
    public boolean isAuth() {
        return status == 401;
    }

    /// The failure of an HTTP status: 401, 408, 429 and 5xx are retryable.
    static ChannelException of(int status, String what) {
        boolean retry = status == 0 || status == 401 || status == 408 || status == 429
                || status >= 500;
        return new ChannelException(status, retry, what + " answered " + status);
    }
}
