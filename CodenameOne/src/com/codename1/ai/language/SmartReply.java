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
import com.codename1.ui.Display;
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

    /// @return whether the selected backend supports Smart Reply
    public static boolean isSupported(LanguageOptions options) {
        LanguageOptions actual = options == null
                ? new LanguageOptions() : options;
        LanguageImpl impl = Display.getInstance().getLanguageBackend();
        return impl != null && impl.isSupported(
                "smart-reply", actual.getBackend().getId());
    }

    /// @param conversation chronological messages, oldest first
    /// @param options backend options, or {@code null}
    /// @return asynchronous suggestions, possibly an empty array
    public static AsyncResource<String[]> suggest(SmartReplyMessage[] conversation,
                                                   LanguageOptions options) {
        LanguageOptions actual = options == null ? new LanguageOptions() : options;
        LanguageImpl impl = Display.getInstance().getLanguageBackend();
        if (impl == null || !impl.isSupported("smart-reply", actual.getBackend().getId())) {
            AsyncResource<String[]> out = new AsyncResource<String[]>();
            out.error(new UnsupportedOperationException("smart reply is not supported"));
            return out;
        }
        return impl.suggestReplies(conversation == null
                        ? new SmartReplyMessage[0] : conversation,
                actual.getBackend().getId(), actual);
    }
}
