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
package com.codenameone.examples.brickyard.net;

import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

/// Turns a REST client's raw answer into one of two callbacks: the value on a
/// 2xx, or a status and a message a person can read otherwise.
public final class Net {
    /// The value of a successful call.
    public interface Ok<T> {
        void got(T value);
    }

    /// A call that failed: `status` is 0 when the server was never reached.
    public interface Failed {
        void failed(int status, String message);
    }

    private Net() {
    }

    public static <T> OnComplete<Response<T>> to(final Ok<T> ok, final Failed failed) {
        return response -> {
            if (response == null) {
                failed.failed(0, "Could not reach the server. Check your connection.");
                return;
            }
            int status = response.getResponseCode();
            if (status >= 200 && status < 300) {
                ok.got(response.getResponseData());
                return;
            }
            failed.failed(status, reason(status, response.getResponseErrorMessage()));
        };
    }

    static String reason(int status, String message) {
        if (status == 401 || status == 403) {
            return "You are not allowed to do that.";
        }
        if (status == 404) {
            return "That is not there any more.";
        }
        if (status == 429) {
            return "Too many attempts. Wait a minute and try again.";
        }
        if (message != null && message.length() > 0 && message.length() < 200) {
            return message;
        }
        return "Something went wrong (" + status + ").";
    }
}
