/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.reel.api;

/// What a title's classification says beside its rating: the content that
/// earned it. A creator picks from this list in the studio; a title's page
/// lists them under the rating, and a profile's maturity limit is still the
/// rating alone.
public final class Advisories {
    public static final String VIOLENCE = "violence";
    public static final String FRIGHTENING = "frightening";
    public static final String LANGUAGE = "language";
    public static final String SEXUAL_CONTENT = "sexual";
    public static final String SUBSTANCES = "substances";
    public static final String FLASHING_LIGHTS = "flashing";

    /// Every advisory, in the order a page lists them.
    public static final String[] ALL = {VIOLENCE, FRIGHTENING, LANGUAGE, SEXUAL_CONTENT, SUBSTANCES, FLASHING_LIGHTS};

    /// What each of [#ALL] is called, in the same order.
    public static final String[] LABELS = {"Violence", "Frightening scenes", "Strong language", "Sexual content",
        "Drug or alcohol use", "Flashing lights"};

    private Advisories() {
    }

    /// What `code` is called, or null when it is not one of [#ALL].
    public static String label(String code) {
        for (int iter = 0; iter < ALL.length; iter++) {
            if (ALL[iter].equals(code)) {
                return LABELS[iter];
            }
        }
        return null;
    }
}
