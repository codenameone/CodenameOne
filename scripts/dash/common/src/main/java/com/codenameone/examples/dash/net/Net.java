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
package com.codenameone.examples.dash.net;

import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;

/// Makes a call through a generated client and sorts out what came back.
///
/// A generated method answers with a `Response` whatever happened: null when
/// the server could not be reached, and otherwise a status with either the
/// typed result or, for a refusal, the server's reason as text. This turns that
/// into one of two callbacks, so a screen writes what to do with the result and
/// what to do with a failure, and nothing else.
///
/// ```java
/// Api.game().runner(Net.to(r -> show(r), Ui::fail));
/// ```
public final class Net {
    /// A call to a generated client method, waiting for its callback.
    public interface Call<T> {
        void run(OnComplete<Response<T>> callback);
    }

    public interface Ok<T> {
        void got(T value);
    }

    public interface Failed {
        /// @param status the HTTP status, or 0 when the server was not reached
        /// @param message something to show the user
        void failed(int status, String message);
    }

    private Net() {
    }

    /// The callback to hand a generated method: `ok` gets the result, `failed`
    /// everything else.
    public static <T> OnComplete<Response<T>> to(final Ok<T> ok, final Failed failed) {
        return new OnComplete<Response<T>>() {
            @Override
            public void completed(Response<T> response) {
                if (response == null) {
                    failed.failed(0, "Could not reach the server. Check your connection.");
                    return;
                }
                int status = response.getResponseCode();
                if (status >= 200 && status < 300) {
                    ok.got(response.getResponseData());
                    return;
                }
                failed.failed(status, reason(response, status));
            }
        };
    }

    /// Makes a call that was handed over as a value, which is how one screen
    /// serves several calls of the same shape.
    public static <T> void send(Call<T> call, Ok<T> ok, Failed failed) {
        call.run(to(ok, failed));
    }

    /// What the server said when it refused. Read as an `Object` and tested,
    /// never cast to the type the call was declared with: on a refusal the body
    /// is text whatever that type is.
    private static String reason(Response<?> response, int status) {
        Object body = response.getResponseData();
        if (body instanceof String && ((String) body).length() > 0
                && ((String) body).length() < 200 && ((String) body).indexOf('<') < 0
                && ((String) body).indexOf('{') < 0) {
            return (String) body;
        }
        if (status == 401) {
            return "Please sign in again.";
        }
        if (status == 403) {
            return "Your account is not allowed to do that.";
        }
        if (status == 429) {
            return "Too many attempts. Wait a moment and try again.";
        }
        return "Something went wrong (" + status + "). Try again.";
    }
}
