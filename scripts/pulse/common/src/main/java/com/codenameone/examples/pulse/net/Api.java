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
package com.codenameone.examples.pulse.net;

import com.codename1.io.rest.RestClients;
import com.codenameone.examples.pulse.api.AccountApi;
import com.codenameone.examples.pulse.api.StudioApi;
import com.codenameone.examples.pulse.api.BookingApi;
import com.codenameone.examples.pulse.api.HouseholdApi;
import com.codenameone.examples.pulse.api.MembershipApi;
import com.codenameone.examples.pulse.api.PaymentApi;
import com.codenameone.examples.pulse.api.EngagementApi;
import com.codenameone.examples.pulse.api.SocialApi;
import com.codenameone.examples.pulse.api.DeskApi;
import com.codenameone.examples.pulse.api.TrainerApi;
import com.codenameone.examples.pulse.api.OwnerApi;
import com.codenameone.examples.pulse.api.AdminApi;

/// The server, as the app calls it.
///
/// Each of these is an implementation the build generated from an interface in
/// the `shared` module. The server implements the other half of the same
/// interface, so a path or a field that changes on one side stops the other
/// from compiling.
public final class Api {
    private static AccountApi account;
    private static StudioApi studio;
    private static BookingApi bookings;
    private static HouseholdApi household;
    private static MembershipApi membership;
    private static PaymentApi payments;
    private static EngagementApi engagement;
    private static SocialApi social;
    private static DeskApi desk;
    private static TrainerApi trainer;
    private static OwnerApi owner;
    private static AdminApi admin;

    private Api() {
    }

    static void connect(String baseUrl) {
        account = RestClients.create(AccountApi.class, baseUrl);
        studio = RestClients.create(StudioApi.class, baseUrl);
        bookings = RestClients.create(BookingApi.class, baseUrl);
        household = RestClients.create(HouseholdApi.class, baseUrl);
        membership = RestClients.create(MembershipApi.class, baseUrl);
        payments = RestClients.create(PaymentApi.class, baseUrl);
        engagement = RestClients.create(EngagementApi.class, baseUrl);
        social = RestClients.create(SocialApi.class, baseUrl);
        desk = RestClients.create(DeskApi.class, baseUrl);
        trainer = RestClients.create(TrainerApi.class, baseUrl);
        owner = RestClients.create(OwnerApi.class, baseUrl);
        admin = RestClients.create(AdminApi.class, baseUrl);
    }

    /// Registration, the account, its preferences and push devices.
    public static AccountApi account() {
        return account;
    }

    /// Locations, class types, trainers and the timetable.
    public static StudioApi studio() {
        return studio;
    }

    /// The household's places in classes, and the queues for them.
    public static BookingApi bookings() {
        return bookings;
    }

    /// The caller and their children, their waivers and membership passes.
    public static HouseholdApi household() {
        return household;
    }

    /// Plans, memberships, freezes and invoices.
    public static MembershipApi membership() {
        return membership;
    }

    /// Saved cards and the server's payment config.
    public static PaymentApi payments() {
        return payments;
    }

    /// Stats, away periods, nudges and workouts.
    public static EngagementApi engagement() {
        return engagement;
    }

    /// Friends, invites and kudos.
    public static SocialApi social() {
        return social;
    }

    /// The front desk: check-in, member lookup, walk-ins and today.
    public static DeskApi desk() {
        return desk;
    }

    /// A trainer's classes, rosters and shifts.
    public static TrainerApi trainer() {
        return trainer;
    }

    /// The owner's dashboard, plans, staff and campaigns.
    public static OwnerApi owner() {
        return owner;
    }

    /// Moderation and account administration.
    public static AdminApi admin() {
        return admin;
    }
}
