/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ai.language;

import com.codename1.impl.LanguageImpl;
import com.codename1.util.AsyncResource;
import com.codename1.util.SuccessCallback;

/// Identifies possible languages entirely on device. Results are ranked by
/// descending backend confidence and filtered by
/// {@link LanguageOptions#getMinimumConfidence()}.
public final class LanguageIdentifier {
    private LanguageIdentifier() {
    }

    /// @return whether automatic language identification is available
    public static boolean isSupported() {
        return isSupported(new LanguageOptions());
    }

    /// @param options backend selection, or {@code null} for defaults
    /// @return whether the selected backend is available on this target
    public static boolean isSupported(LanguageOptions options) {
        return LanguageSession.isSupported("language-id", options);
    }

    /// Opens a reusable language-identification session. Reusing a session
    /// avoids repeatedly creating the native recognizer when classifying many
    /// strings. Close it when no more identifications are pending.
    ///
    /// @param options backend and confidence options, or {@code null}
    /// @return a reusable session that owns one native language backend
    /// @throws UnsupportedOperationException if the selected backend is absent
    public static Session open(LanguageOptions options) {
        return new Session(LanguageSession.open("language-id", options));
    }

    /// Identifies possible languages off the EDT without uploading text. The
    /// option values are copied before the backend starts, so later mutations
    /// of the supplied object cannot alter the pending identification.
    /// @param text non-null text to classify
    /// @param options backend and confidence options, or {@code null}
    /// @return asynchronous ranked candidates; may be empty for undetermined text
    public static AsyncResource<LanguageCandidate[]> identify(
            String text, LanguageOptions options) {
        final Session session;
        try {
            session = open(options);
        } catch (RuntimeException error) {
            AsyncResource<LanguageCandidate[]> out =
                    new AsyncResource<LanguageCandidate[]>();
            out.error(error);
            return out;
        }
        AsyncResource<LanguageCandidate[]> result = session.identify(text);
        closeWhenFinished(result, session);
        return result;
    }

    private static <T> void closeWhenFinished(AsyncResource<T> result,
                                               final Session session) {
        result.ready(new SuccessCallback<T>() {
            public void onSucess(T value) {
                session.close();
            }
        }).except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable error) {
                session.close();
            }
        });
    }

    /// Reusable owner of a native language-identification backend.
    ///
    /// A session accepts multiple asynchronous requests. Calling
    /// {@link #close()} prevents new requests immediately and defers native
    /// release until requests already in progress have completed.
    public static final class Session implements AutoCloseable {
        private final LanguageSession session;

        private Session(LanguageSession session) {
            this.session = session;
        }

        /// Identifies languages without recreating the native backend.
        ///
        /// @param text text to classify; {@code null} is treated as empty
        /// @return asynchronous ranked candidates, possibly empty
        /// @throws IllegalStateException if this session is closed
        public AsyncResource<LanguageCandidate[]> identify(final String text) {
            return session.execute(
                    new LanguageSession.Operation<LanguageCandidate[]>() {
                        public AsyncResource<LanguageCandidate[]> run(
                                LanguageImpl implementation,
                                LanguageOptions options) {
                            return implementation.identify(
                                    text == null ? "" : text,
                                    options.getBackend().getId(), options);
                        }
                    });
        }

        /// Closes the session. This method is idempotent; native release is
        /// deferred until pending identifications finish.
        public void close() {
            session.close();
        }
    }
}
