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
package com.codenameone.examples.brickyard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/// The deep links an invite or a push opens the app with.
class DeepLinkTest {
    @Test
    void appSchemeNamesTheExperience() {
        assertEquals("abc123", AppConfig.experienceFromLink(AppConfig.experienceLink("abc123")));
        assertEquals("abc123", AppConfig.experienceFromLink("BRICKYARD://experience/abc123?ref=x"));
    }

    @Test
    void webLinkCarriesItAsAParameter() {
        assertEquals("e9", AppConfig.experienceFromLink("https://example.com/?experience=e9#top"));
    }

    @Test
    void anythingElseIsNoExperience() {
        assertNull(AppConfig.experienceFromLink(null));
        assertNull(AppConfig.experienceFromLink("brickyard://experience/"));
        assertNull(AppConfig.experienceFromLink("brickyard://shop"));
    }
}
