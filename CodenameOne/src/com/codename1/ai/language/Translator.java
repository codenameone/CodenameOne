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

/// Translates text on device with lazily installed language-pair models.
/// The first request for a pair may take longer while ML Kit downloads the
/// model; download failures are reported through the returned resource.
public final class Translator {
    private Translator() {
    }

    /// @return whether automatic on-device translation is available
    public static boolean isSupported() {
        return isSupported(new LanguageOptions());
    }

    /// Tests whether translation is available for a backend selection.
    ///
    /// This capability check creates and closes a temporary native backend.
    /// Retain a {@link Session} from {@link #open(LanguageOptions)} when the
    /// application will immediately perform repeated translations.
    ///
    /// @param options backend selection, or {@code null} for defaults
    /// @return whether the selected backend supports translation
    public static boolean isSupported(LanguageOptions options) {
        return LanguageSession.isSupported("translation", options);
    }

    /// Opens a reusable translation session. Reuse it for repeated requests
    /// so native translation clients and downloaded model state are retained.
    ///
    /// @param options backend options, or {@code null}
    /// @return a reusable session that owns one native language backend
    /// @throws UnsupportedOperationException if the selected backend is absent
    public static Session open(LanguageOptions options) {
        return new Session(LanguageSession.open("translation", options));
    }

    /// Option values are copied before asynchronous backend work begins.
    /// Current ML Kit backends accept a BCP-47 tag with an optional script or
    /// region, such as {@code en-US}, and select the corresponding supported
    /// base-language model ({@code en} in this example). The asynchronous
    /// resource fails when either tag does not identify a supported model.
    ///
    /// @param text source text; {@code null} is treated as empty
    /// @param sourceLanguage BCP-47 source language tag
    /// @param targetLanguage BCP-47 target language tag
    /// @param options backend options, or {@code null}
    /// @return asynchronous translated text
    public static AsyncResource<String> translate(String text, String sourceLanguage,
                                                   String targetLanguage,
                                                   LanguageOptions options) {
        final Session session;
        try {
            session = open(options);
        } catch (RuntimeException error) {
            AsyncResource<String> out = new AsyncResource<String>();
            out.error(error);
            return out;
        }
        AsyncResource<String> result = session.translate(text,
                sourceLanguage, targetLanguage);
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

    /// Reusable owner of a native translation backend.
    ///
    /// A session can serve multiple language pairs and retains native clients
    /// between calls. Calling {@link #close()} prevents new requests and
    /// defers release until pending translations finish.
    public static final class Session implements AutoCloseable {
        private final LanguageSession session;

        private Session(LanguageSession session) {
            this.session = session;
        }

        /// Translates text without recreating the native backend.
        ///
        /// @param text source text; {@code null} is treated as empty
        /// @param sourceLanguage supported BCP-47 source language tag
        /// @param targetLanguage supported BCP-47 target language tag
        /// @return asynchronous translated text
        /// @throws IllegalStateException if this session is closed
        public AsyncResource<String> translate(final String text,
                                               final String sourceLanguage,
                                               final String targetLanguage) {
            return session.execute(new LanguageSession.Operation<String>() {
                public AsyncResource<String> run(
                        LanguageImpl implementation,
                        LanguageOptions options) {
                    return implementation.translate(
                            text == null ? "" : text, sourceLanguage,
                            targetLanguage, options.getBackend().getId(),
                            options);
                }
            });
        }

        /// Closes the session. This method is idempotent; native release is
        /// deferred until pending translations finish.
        public void close() {
            session.close();
        }
    }
}
