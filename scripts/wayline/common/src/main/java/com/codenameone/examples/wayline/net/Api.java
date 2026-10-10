/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.net;

import com.codename1.io.rest.RestClients;
import com.codenameone.examples.wayline.api.AccountApi;
import com.codenameone.examples.wayline.api.AdminApi;
import com.codenameone.examples.wayline.api.DriverApi;
import com.codenameone.examples.wayline.api.DrivingApi;
import com.codenameone.examples.wayline.api.GeoApi;
import com.codenameone.examples.wayline.api.PaymentApi;
import com.codenameone.examples.wayline.api.RiderApi;

/// The server, as the app calls it.
///
/// Each of these is an implementation the build generated from an interface in
/// the `shared` module. The server implements the other half of the same
/// interface, so a path or a field that changes on one side stops the other
/// from compiling.
public final class Api {
    private static AccountApi account;
    private static RiderApi rider;
    private static DriverApi driver;
    private static GeoApi geo;
    private static AdminApi admin;
    private static PaymentApi payments;
    private static DrivingApi driving;

    private Api() {
    }

    static void connect(String baseUrl) {
        account = RestClients.create(AccountApi.class, baseUrl);
        rider = RestClients.create(RiderApi.class, baseUrl);
        driver = RestClients.create(DriverApi.class, baseUrl);
        geo = RestClients.create(GeoApi.class, baseUrl);
        admin = RestClients.create(AdminApi.class, baseUrl);
        payments = RestClients.create(PaymentApi.class, baseUrl);
        driving = RestClients.create(DrivingApi.class, baseUrl);
    }

    public static AccountApi account() {
        return account;
    }

    public static RiderApi rider() {
        return rider;
    }

    public static DriverApi driver() {
        return driver;
    }

    public static GeoApi geo() {
        return geo;
    }

    public static AdminApi admin() {
        return admin;
    }

    public static PaymentApi payments() {
        return payments;
    }

    /// Applying to drive, which is open to everyone; [#driver] is for those
    /// whose application was approved.
    public static DrivingApi driving() {
        return driving;
    }
}
