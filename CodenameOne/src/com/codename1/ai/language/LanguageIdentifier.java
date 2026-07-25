/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 */
package com.codename1.ai.language;

import com.codename1.impl.LanguageImpl;
import com.codename1.ui.Display;
import com.codename1.util.AsyncResource;

/** On-device language identification. */
public final class LanguageIdentifier {
    private LanguageIdentifier() {
    }

    public static boolean isSupported() {
        return isSupported(new LanguageOptions());
    }

    public static boolean isSupported(LanguageOptions options) {
        LanguageImpl impl = Display.getInstance().getLanguageBackend();
        LanguageOptions actual = options == null ? new LanguageOptions() : options;
        return impl != null && impl.isSupported("language-id",
                actual.getBackend().getId());
    }

    public static AsyncResource<LanguageCandidate[]> identify(
            String text, LanguageOptions options) {
        LanguageOptions actual = options == null ? new LanguageOptions() : options;
        LanguageImpl impl = Display.getInstance().getLanguageBackend();
        if (impl == null || !impl.isSupported("language-id",
                actual.getBackend().getId())) {
            AsyncResource<LanguageCandidate[]> out =
                    new AsyncResource<LanguageCandidate[]>();
            out.error(new UnsupportedOperationException(
                    "language-id is not supported"));
            return out;
        }
        return impl.identify(text == null ? "" : text,
                actual.getBackend().getId(), actual);
    }
}
