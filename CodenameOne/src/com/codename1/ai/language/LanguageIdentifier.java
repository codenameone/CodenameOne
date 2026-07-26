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
        LanguageImpl impl = Display.getInstance().getLanguageBackend();
        LanguageOptions actual = options == null ? new LanguageOptions() : options;
        return impl != null && impl.isSupported("language-id",
                actual.getBackend().getId());
    }

    /// Identifies possible languages off the EDT without uploading text.
    /// @param text non-null text to classify
    /// @param options backend and confidence options, or {@code null}
    /// @return asynchronous ranked candidates; may be empty for undetermined text
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
