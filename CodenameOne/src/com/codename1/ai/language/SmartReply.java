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

/// Produces short reply suggestions for a chronological conversation without
/// uploading its messages. ML Kit may return no suggestions when the language
/// or conversation context is unsupported.
public final class SmartReply {
    private SmartReply() {
    }

    /// @return whether automatic Smart Reply is available
    public static boolean isSupported() {
        return isSupported(new LanguageOptions());
    }

    /// Tests whether Smart Reply is available for a backend selection.
    ///
    /// This capability check creates and closes a temporary native backend.
    /// Retain a {@link Session} from {@link #open(LanguageOptions)} when the
    /// application will immediately request repeated suggestions.
    ///
    /// @param options backend selection, or {@code null} for defaults
    /// @return whether the selected backend supports Smart Reply
    public static boolean isSupported(LanguageOptions options) {
        return LanguageSession.isSupported("smart-reply", options);
    }

    /// Opens a reusable Smart Reply session. Reuse it for conversations that
    /// need repeated suggestions to avoid recreating the native client.
    ///
    /// @param options backend options, or {@code null}
    /// @return a reusable session that owns one native language backend
    /// @throws UnsupportedOperationException if the selected backend is absent
    public static Session open(LanguageOptions options) {
        return new Session(LanguageSession.open("smart-reply", options));
    }

    /// The conversation array and option values are copied before backend work
    /// begins. {@link SmartReplyMessage} values are immutable.
    /// Cancelling the returned resource suppresses late result callbacks; the
    /// temporary backend is released after its pending native work settles.
    ///
    /// @param conversation chronological messages, oldest first
    /// @param options backend options, or {@code null}
    /// @return asynchronous suggestions, possibly an empty array
    public static AsyncResource<String[]> suggest(SmartReplyMessage[] conversation,
                                                   LanguageOptions options) {
        final Session session;
        try {
            session = open(options);
        } catch (RuntimeException error) {
            AsyncResource<String[]> out = new AsyncResource<String[]>();
            out.error(error);
            return out;
        }
        return session.suggest(conversation, true);
    }

    private static SmartReplyMessage[] copyConversation(
            SmartReplyMessage[] conversation) {
        if (conversation == null) {
            return new SmartReplyMessage[0];
        }
        SmartReplyMessage[] copy =
                new SmartReplyMessage[conversation.length];
        for (int i = 0; i < conversation.length; i++) {
            copy[i] = conversation[i];
        }
        return copy;
    }

    /// Reusable owner of a native Smart Reply backend.
    ///
    /// Calling {@link #close()} prevents new requests immediately and defers
    /// native release until suggestions already in progress have completed.
    public static final class Session implements AutoCloseable {
        private final LanguageSession session;

        private Session(LanguageSession session) {
            this.session = session;
        }

        /// Generates reply suggestions without recreating the native backend.
        /// Cancelling the returned resource suppresses late callbacks without
        /// closing this reusable session.
        ///
        /// @param conversation chronological messages, oldest first;
        ///        {@code null} is treated as an empty conversation
        /// @return asynchronous suggestions, possibly empty
        /// @throws IllegalStateException if this session is closed
        public AsyncResource<String[]> suggest(
                final SmartReplyMessage[] conversation) {
            return suggest(conversation, false);
        }

        private AsyncResource<String[]> suggest(
                final SmartReplyMessage[] conversation,
                boolean closeWhenFinished) {
            final SmartReplyMessage[] snapshot =
                    copyConversation(conversation);
            return session.execute(new LanguageSession.Operation<String[]>() {
                public AsyncResource<String[]> run(
                        LanguageImpl implementation,
                        LanguageOptions options) {
                    return implementation.suggestReplies(snapshot,
                            options.getBackend().getId(), options);
                }
            }, closeWhenFinished);
        }

        /// Closes the session. This method is idempotent; native release is
        /// deferred until pending suggestions finish.
        public void close() {
            session.close();
        }
    }
}
