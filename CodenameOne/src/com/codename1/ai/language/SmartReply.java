/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.language;

import com.codename1.impl.LanguageImpl;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

/** On-device short reply suggestions for a conversation. */
public final class SmartReply {
    private SmartReply() {
    }

    public static boolean isSupported() {
        return isSupported(new LanguageOptions());
    }

    public static boolean isSupported(LanguageOptions options) {
        LanguageOptions actual = options == null
                ? new LanguageOptions() : options;
        LanguageImpl impl = Display.getInstance().getLanguageBackend();
        return impl != null && impl.isSupported(
                "smart-reply", actual.getBackend().getId());
    }

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
