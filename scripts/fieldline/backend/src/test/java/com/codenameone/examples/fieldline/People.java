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
package com.codenameone.examples.fieldline;

import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.fieldline.account.Accounts;
import com.codenameone.examples.fieldline.api.RegisterDto;

import static com.codename1.backend.test.SecurityMockMvcRequestPostProcessors.jwt;

/// The people a test needs, made fresh for it.
///
/// Every test class shares one running server and one database, so a test that
/// borrowed a demo account would meet whatever the test before it left behind.
/// Each test that changes an account makes its own.
final class People {
    static final String PASSWORD = "a-long-password";

    private People() {
    }

    /// Opens a technician's account, as the app's registration does, and
    /// returns its name.
    static String technician(Accounts accounts, String name) throws Exception {
        RegisterDto request = new RegisterDto(name + "@test.example", PASSWORD, name, "+15550111");
        return accounts.register(request).username();
    }

    /// The request, made as a technician. The authorities are stated here and
    /// not read from the account: `AccessTest` is the one that signs in for real.
    static MockRequestBuilder asTechnician(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_TECHNICIAN"));
    }

    static MockRequestBuilder asDispatcher(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_DISPATCHER"));
    }
}
