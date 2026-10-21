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
package com.codenameone.examples.bastion.net;

import com.codenameone.examples.bastion.AppConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/// What the network guard lets through.
class GuardTest {
    @Test
    void cleartextAndUnlistedHostsAreRefused() {
        assertNull(Guard.refusal("http://localhost:8080/api/me"));
        assertNull(Guard.refusal("http://127.0.0.1:8080/login"));
        assertNull(Guard.refusal("https://api.bastion.example/api/me"));
        assertNull(Guard.refusal("https://API.Bastion.Example:443/api/me"));
        assertNotNull(Guard.refusal("http://api.bastion.example/api/me"));
        assertNotNull(Guard.refusal("https://evil.example/api/me"));
        // The user-info trick: the host is what follows the '@'.
        assertNotNull(Guard.refusal("https://api.bastion.example@evil.example/"));
        assertNotNull(Guard.refusal("https://localhost.evil.example/"));
        assertNotNull(Guard.refusal("ftp://api.bastion.example/"));
    }

    @Test
    void hostOf() {
        assertEquals("api.bastion.example", AppConfig.hostOf("https://api.bastion.example/x?y"));
        assertEquals("[::1]", AppConfig.hostOf("http://[::1]:8080/"));
        assertNull(AppConfig.hostOf("javascript:alert(1)"));
    }
}
