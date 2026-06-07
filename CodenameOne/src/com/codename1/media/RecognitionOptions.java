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
package com.codename1.media;

/// Tuning knobs for [SpeechRecognizer]. All fields are optional; the
/// platform picks sensible defaults for anything left unset.
public final class RecognitionOptions {
    private String languageTag = "en-US";
    private boolean partialResults = true;
    private boolean continuous = false;
    private int maxResults = 1;

    /// BCP-47 language tag (e.g. `"en-US"`, `"de-DE"`, `"fr-CA"`).
    /// Defaults to `"en-US"`.
    public RecognitionOptions setLanguageTag(String tag) {
        this.languageTag = tag;
        return this;
    }

    public String getLanguageTag() {
        return languageTag;
    }

    /// Whether the callback should receive partial transcripts as the
    /// user speaks. Defaults to `true`.
    public RecognitionOptions setPartialResults(boolean partial) {
        this.partialResults = partial;
        return this;
    }

    public boolean isPartialResults() {
        return partialResults;
    }

    /// Whether recognition should keep listening across silences.
    /// Defaults to `false` (single-utterance).
    public RecognitionOptions setContinuous(boolean c) {
        this.continuous = c;
        return this;
    }

    public boolean isContinuous() {
        return continuous;
    }

    /// Maximum alternative transcripts requested per final result.
    /// iOS supports up to 10; Android up to ~5 depending on the
    /// vendor. Defaults to 1.
    public RecognitionOptions setMaxResults(int n) {
        this.maxResults = Math.max(1, n);
        return this;
    }

    public int getMaxResults() {
        return maxResults;
    }
}
