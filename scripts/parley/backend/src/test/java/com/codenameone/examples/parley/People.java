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
package com.codenameone.examples.parley;

import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.parley.account.Accounts;
import com.codenameone.examples.parley.account.DemoAccounts;
import com.codenameone.examples.parley.api.RegisterDto;

import static com.codename1.backend.test.SecurityMockMvcRequestPostProcessors.jwt;

/// The people a test needs, made fresh for it.
///
/// Test classes share a running server and its database for as long as they
/// can, so a test that borrowed a demo account would meet whatever the test
/// before it left behind: a device registered, a contact blocked, a call
/// still ringing. Each test makes its own people instead.
public final class People {
    public static final String PASSWORD = "a-long-password";

    private People() {
    }

    /// Opens an account whose phone number is already proved, and returns its
    /// name.
    public static String user(Accounts accounts, String name) throws Exception {
        RegisterDto request = new RegisterDto(name + "@test.example", PASSWORD, name, "+15550111");
        String username = accounts.register(request).username();
        accounts.markPhoneVerified(username);
        return username;
    }

    /// An account that is also an admin.
    public static String admin(Accounts accounts, String name) throws Exception {
        String username = user(accounts, name);
        accounts.setRoles(DemoAccounts.ADMIN, username, true);
        return username;
    }

    /// The request, made as a user. The authorities are stated here and not
    /// read from the account: these are the tests of what a role may do, and
    /// `AccessTest` is the one that signs in for real.
    public static MockRequestBuilder asUser(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_USER"));
    }

    public static MockRequestBuilder asAdmin(MockRequestBuilder request, String username) {
        return request.with(jwt().subject(username).authorities("ROLE_USER", "ROLE_ADMIN"));
    }
}
