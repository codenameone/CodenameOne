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
package com.codename1.appkit;

import com.codename1.io.rest.Response;
import com.codename1.util.OnComplete;
import com.codename1.ui.plaf.UIManager;

/// Makes a call through a generated client and sorts out what came back.
///
/// A generated method answers with a `Response` whatever happened: null when
/// the server could not be reached, and otherwise a status with either the
/// typed result or, for a refusal, the server's reason as text. This turns that
/// into one of two callbacks, so a screen writes what to do with the result and
/// what to do with a failure, and nothing else.
///
/// ```java
/// Api.orders().order(id, Net.to(order -> show(order), Ui::fail));
/// ```
///
/// Every message it makes up is passed through the theme's localization
/// bundle with the English text as its key, so an app translates them in its
/// bundles like any other text. Two hooks fit it to an app that needs more:
/// [#setEpoch(Epoch)] drops answers that belong to a session no longer on
/// screen, and [#setReasons(Reasons)] turns a server's refusal into the words
/// shown.
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

    /// The session an answer belongs to. A call remembers the value when it
    /// is made, and its answer is dropped, neither callback running, when the
    /// value has moved on by the time it arrives: the app locked, or the user
    /// signed out or in again while it was out.
    public interface Epoch {
        int current();
    }

    /// What a failure says to the user. Asked about every failure, so an app
    /// that keeps state about the last one (a refusal code it reads in its
    /// [Failed] callback) can clear it when the next failure has none.
    public interface Reasons {
        /// @param status the HTTP status, or 0 when the server was not reached
        /// @param body the text the server refused with, when it answered with
        ///     a short line of text; otherwise null
        /// @return what to show, or null for what this class would show
        String reason(int status, String body);
    }

    private static Epoch epoch;
    private static Reasons reasons;

    private Net() {
    }

    /// Sets the session counter answers are checked against; null, the
    /// default, delivers every answer.
    public static void setEpoch(Epoch epoch) {
        Net.epoch = epoch;
    }

    /// Sets what a refusal is shown as; null, the default, shows the server's
    /// text.
    public static void setReasons(Reasons reasons) {
        Net.reasons = reasons;
    }

    private static int epochNow() {
        Epoch e = epoch;
        return e == null ? 0 : e.current();
    }

    private static String custom(int status, String body) {
        Reasons r = reasons;
        return r == null ? null : r.reason(status, body);
    }

    private static String tr(String text) {
        return UIManager.getInstance().localize(text, text);
    }

    /// The callback to hand a generated method: `ok` gets the result, `failed`
    /// everything else.
    public static <T> OnComplete<Response<T>> to(final Ok<T> ok, final Failed failed) {
        final int asked = epochNow();
        return new OnComplete<Response<T>>() {
            @Override
            public void completed(Response<T> response) {
                if (asked != epochNow()) {
                    return;
                }
                if (response == null) {
                    String shown = custom(0, null);
                    failed.failed(0, shown != null ? shown
                            : tr("Could not reach the server. Check your connection."));
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
        Object data = response.getResponseData();
        String body = null;
        if (data instanceof String && ((String) data).length() > 0
                && ((String) data).length() < 300 && ((String) data).indexOf('<') < 0
                && ((String) data).indexOf('{') < 0) {
            body = (String) data;
        }
        String shown = custom(status, body);
        if (shown != null) {
            return shown;
        }
        if (body != null) {
            return body;
        }
        if (status == 401) {
            return tr("Please sign in again.");
        }
        if (status == 403) {
            return tr("Your account is not allowed to do that.");
        }
        if (status == 429) {
            return tr("Too many attempts. Wait a moment and try again.");
        }
        String text = tr("Something went wrong ({0}). Try again.");
        int at = text.indexOf("{0}");
        return at < 0 ? text : text.substring(0, at) + status + text.substring(at + 3);
    }
}
