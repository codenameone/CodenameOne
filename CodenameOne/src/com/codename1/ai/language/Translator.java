/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.language;

import com.codename1.impl.LanguageImpl;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

/** On-device translation with lazily installed language-pair models. */
public final class Translator {
    private Translator() {
    }

    public static boolean isSupported() {
        return isSupported(new LanguageOptions());
    }

    public static boolean isSupported(LanguageOptions options) {
        LanguageOptions actual = options == null ? new LanguageOptions() : options;
        LanguageImpl impl = Display.getInstance().getLanguageBackend();
        return impl != null && impl.isSupported("translation",
                actual.getBackend().getId());
    }

    public static AsyncResource<String> translate(String text, String sourceLanguage,
                                                   String targetLanguage,
                                                   LanguageOptions options) {
        LanguageOptions actual = options == null ? new LanguageOptions() : options;
        LanguageImpl impl = Display.getInstance().getLanguageBackend();
        if (impl == null || !impl.isSupported("translation", actual.getBackend().getId())) {
            AsyncResource<String> out = new AsyncResource<String>();
            out.error(new UnsupportedOperationException("translation is not supported"));
            return out;
        }
        return impl.translate(text == null ? "" : text, sourceLanguage, targetLanguage,
                actual.getBackend().getId(), actual);
    }
}
