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
package com.codenameone.examples.bazaar;

import com.codename1.testing.AbstractTest;
import com.codenameone.examples.bazaar.net.SecondFactor;

/// What the sign-in reads about a second factor, without a server or a
/// screen: when a code is asked for, when it was refused, and how a typed
/// code is cleaned.
public class SecondFactorTest extends AbstractTest {
    @Override
    public boolean runTest() throws Exception {
        // The installed app sees the redirect.
        assertTrue(SecondFactor.wanted(false, 302, "https://shop.example/login/second-factor",
                null), "a redirect to the code page asks for a code");
        assertFalse(SecondFactor.wanted(false, 302, "https://shop.example/", null),
                "any other redirect is the password's answer");
        assertFalse(SecondFactor.wanted(false, 200, null, null), "no redirect, no code");
        // The app in a browser reads the page the redirect ends on.
        assertTrue(SecondFactor.wanted(true, 401, null, "required"), "the code page");
        assertFalse(SecondFactor.wanted(true, 200, null, null), "signed in");
        assertFalse(SecondFactor.wanted(true, 401, null, null), "a wrong password");

        assertFalse(SecondFactor.refused(200, null), "the code was taken");
        assertTrue(SecondFactor.refused(302, "https://shop.example/login/second-factor?error"),
                "sent back to the code page");
        assertTrue(SecondFactor.refused(401, null), "the page, with the error");
        assertTrue(SecondFactor.refused(429, null), "too many tries");

        assertEqual("123456", SecondFactor.clean(" 123 456 "), "spaces go");
        assertEqual("ab12-cd34", SecondFactor.clean("ab12-cd34"), "a recovery code's dash stays");
        assertEqual("", SecondFactor.clean(null), "nothing typed");
        return true;
    }
}
