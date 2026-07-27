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

    /// @return whether the selected backend supports translation
    public static boolean isSupported(LanguageOptions options) {
        LanguageOptions actual = options == null ? new LanguageOptions() : options;
        LanguageImpl impl = Display.getInstance().getLanguageBackend();
        return impl != null && impl.isSupported("translation",
                actual.getBackend().getId());
    }

    /// Option values are copied before asynchronous backend work begins.
    ///
    /// @param text source text
    /// @param sourceLanguage BCP-47/ML Kit source language tag
    /// @param targetLanguage BCP-47/ML Kit target language tag
    /// @param options backend options, or {@code null}
    /// @return asynchronous translated text
    public static AsyncResource<String> translate(String text, String sourceLanguage,
                                                   String targetLanguage,
                                                   LanguageOptions options) {
        LanguageOptions actual = (options == null
                ? new LanguageOptions() : options).snapshot();
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
