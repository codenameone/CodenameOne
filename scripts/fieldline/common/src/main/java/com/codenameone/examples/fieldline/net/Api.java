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
package com.codenameone.examples.fieldline.net;

import com.codenameone.examples.fieldline.api.AccountApi;
import com.codenameone.examples.fieldline.api.AdminApi;
import com.codenameone.examples.fieldline.api.DispatchApi;
import com.codenameone.examples.fieldline.api.FieldApi;
import com.codenameone.examples.fieldline.api.GeoApi;
import com.codename1.io.rest.RestClients;

/// The server, as the app calls it.
///
/// Each of these is an implementation the build generated from an interface in
/// the `shared` module. The server implements the other half of the same
/// interface, so a path or a field that changes on one side stops the other
/// from compiling.
public final class Api {
    private static AccountApi account;
    private static FieldApi field;
    private static DispatchApi dispatch;
    private static GeoApi geo;
    private static AdminApi admin;

    private Api() {
    }

    static void connect(String baseUrl) {
        account = RestClients.create(AccountApi.class, baseUrl);
        field = RestClients.create(FieldApi.class, baseUrl);
        dispatch = RestClients.create(DispatchApi.class, baseUrl);
        geo = RestClients.create(GeoApi.class, baseUrl);
        admin = RestClients.create(AdminApi.class, baseUrl);
    }

    public static AccountApi account() {
        return account;
    }

    /// The technician's calls: the day, each job, availability, the assistant.
    public static FieldApi field() {
        return field;
    }

    /// The dispatcher's calls: the board and the scheduler.
    public static DispatchApi dispatch() {
        return dispatch;
    }

    /// The admin's calls: accounts and the crew's limits.
    public static AdminApi admin() {
        return admin;
    }

    public static GeoApi geo() {
        return geo;
    }
}
