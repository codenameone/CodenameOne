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
package com.codenameone.examples.slate.sync;

import java.io.IOException;
import java.util.List;

/// A calendar the server reads busy time from on a provider's behalf.
///
/// Only busy time crosses this interface: a start and an end. Slate never
/// asks a calendar what an event is, who is at it or where, so a provider can
/// connect a calendar full of other clients' names without Slate ever holding
/// one of them.
///
/// `credential` is what [#connect] returned, kept on the link: a refresh token
/// for an account calendar, the address of a feed. It is handed back on every
/// call and never shown to the app again.
public interface ExternalCalendar {
    /// `GOOGLE`, `MICROSOFT` or `ICS`: the kind of link this reads.
    String kind();

    /// Turns what the app sent when the provider connected -- an OAuth code,
    /// or a feed's address -- into the credential kept on the link. Fails with
    /// an `IOException` the app is told about when it cannot.
    String connect(String given, String redirectUri) throws IOException;

    /// The busy intervals between `from` and `to`, each `{start, end}` in
    /// milliseconds.
    List<long[]> busy(String credential, String calendars, long from, long to)
            throws IOException;
}
