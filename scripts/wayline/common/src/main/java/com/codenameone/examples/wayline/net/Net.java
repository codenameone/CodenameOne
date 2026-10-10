/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.net;

import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import com.codenameone.examples.wayline.ui.Lang;

/// Makes a call through a generated client and sorts out what came back.
///
/// A generated method answers with a `Response` whatever happened: null when
/// the server could not be reached, and otherwise a status with either the
/// typed result or, for a refusal, the server's reason as text. This turns that
/// into one of two callbacks, so a screen writes what to do with the result and
/// what to do with a failure, and nothing else.
///
/// ```java
/// Api.rider().active(Net.to(ride -> show(ride), Ui::fail));
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
        return Lang.tr("Something went wrong ({0}). Try again.", String.valueOf(status));
    }
}
