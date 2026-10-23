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
package com.codenameone.examples.bastion;

/// The roles an account can hold. Every account opened through the app is a
/// customer; ADMIN and AUDITOR are given only by configuration
/// ([account.DemoAccounts]) and never by any request, so no bug in an endpoint
/// can hand them out.
///
/// The three are disjoint, which is the segregation of duties a bank's
/// examiners ask for: a customer moves their own money, an ADMIN operates the
/// books (adjusts, reverses, closes periods -- above a threshold only with a
/// second admin's approval) and can never rewrite the audit chain, and an
/// AUDITOR reads everything and can change nothing. Every read a member of
/// staff makes of the books or the audit trail is itself audited.
public final class Roles {
    public static final String CUSTOMER = "CUSTOMER";
    public static final String ADMIN = "ADMIN";
    public static final String AUDITOR = "AUDITOR";

    private Roles() {
    }
}
