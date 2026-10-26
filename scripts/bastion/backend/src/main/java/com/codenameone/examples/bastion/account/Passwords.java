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
package com.codenameone.examples.bastion.account;

import com.codenameone.examples.bastion.Refused;

/// What a password must be.
///
/// Length is the rule that matters, and the one NIST SP 800-63B asks for:
/// twelve characters at the least, and no rules about mixing classes of
/// character, which push people to `Password1!`. The password may not be the
/// username, nor the part of the e-mail address before the `@`, since that is
/// the first thing anyone guesses. A real bank would also refuse passwords
/// that appear in breach corpora (a k-anonymity lookup such as Have I Been
/// Pwned's range API); that needs a network call this sample does not make.
public final class Passwords {
    public static final int MINIMUM = 12;
    /// The hash is PBKDF2 or bcrypt, both of which cost the same for any
    /// length -- bcrypt reads only 72 bytes -- but a megabyte of password is
    /// still a megabyte to parse.
    public static final int MAXIMUM = 200;

    private Passwords() {
    }

    /// Refuses `password` for `username` with a 400 that says why.
    public static void check(String username, String password) {
        if (password == null || password.length() < MINIMUM) {
            throw Refused.with(400, "choose-password-least-characters", "Choose a password of at least {0} characters", MINIMUM);
        }
        if (password.length() > MAXIMUM) {
            throw Refused.with(400, "password-too-long", "That password is too long");
        }
        if (username != null) {
            int at = username.indexOf('@');
            String local = at > 0 ? username.substring(0, at) : username;
            // equalsIgnoreCase compares character by character, without
            // folding the whole string in the server's language.
            if (password.trim().equalsIgnoreCase(username)
                    || password.trim().equalsIgnoreCase(local)) {
                throw Refused.with(400, "password-cannot-e-mail-address", "Your password cannot be your e-mail address");
            }
        }
    }
}
