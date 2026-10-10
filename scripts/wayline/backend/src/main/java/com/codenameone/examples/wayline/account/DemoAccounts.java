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
package com.codenameone.examples.wayline.account;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.wayline.Days;
import com.codenameone.examples.wayline.api.CardDto;
import com.codenameone.examples.wayline.api.DocumentUploadDto;
import com.codenameone.examples.wayline.api.DriverApplicationDto;
import com.codenameone.examples.wayline.api.PaymentSetupDto;
import com.codenameone.examples.wayline.domain.DriverApplication;
import com.codenameone.examples.wayline.domain.DriverDocument;
import com.codenameone.examples.wayline.domain.Profile;
import com.codenameone.examples.wayline.driving.Applications;
import com.codenameone.examples.wayline.pay.Payments;
import com.codenameone.examples.wayline.ride.DemoRides;
import com.codenameone.examples.wayline.ride.Fares;

import java.io.IOException;

/// The accounts a server starts with.
///
/// On a development profile, enough of each kind that every screen of the app
/// has something to show straight after a checkout: riders, approved drivers of
/// each kind of car, an admin, someone whose application to drive is waiting
/// for an answer, an account the admins have flagged, and a fortnight of rides
/// between them ([DemoRides]).
///
/// Anywhere else, at most one: the admin named by `wayline.admin.email` and
/// `wayline.admin.password` (or the same two names in the environment, in upper
/// case with `_` for each `.`), because a new deployment needs somebody able to
/// grant the first roles. It is created once; changing the setting later does
/// not change the password of an account that exists.
@Component
public class DemoAccounts {
    public static final String RIDER = "rider@wayline.example";
    public static final String DRIVER = "driver@wayline.example";
    public static final String SECOND_DRIVER = "driver2@wayline.example";
    public static final String ADMIN = "admin@wayline.example";
    /// A driver of each of the larger kinds of car; the second takes a
    /// wheelchair. That one and the first driver of all take pets.
    public static final String COMFORT_DRIVER = "driver3@wayline.example";
    public static final String XL_DRIVER = "driver4@wayline.example";
    public static final String SECOND_RIDER = "rider2@wayline.example";
    /// A rider the admins have flagged.
    public static final String FLAGGED_RIDER = "rider3@wayline.example";
    /// Someone whose application to drive is complete and waiting for an admin.
    public static final String APPLICANT = "applicant@wayline.example";
    /// The password of every demo account. Not a secret: these accounts exist
    /// only where the profile says the server is not a real one.
    public static final String PASSWORD = "wayline-demo";

    /// A small grey chequered PNG, standing in for every photographed document
    /// of the demo accounts.
    static final String PLACEHOLDER_PICTURE = "iVBORw0KGgoAAAANSUhEUgAAADAAAAAeCAAAAABPz8IdAAAAJUlE"
            + "QVR42mN4AgUnoIAQn2FEaiBWIYw/MjWMpqXRtDSaluioAQAjjbP9bRBUyQAAAABJRU5ErkJggg==";

    private final Accounts accounts;
    private final Applications applications;
    private final Moderation moderation;
    private final Payments payments;
    private final DemoRides rides;
    private final ProfileRepository profiles;
    private final DemoAccountRepository demo;
    private final Config config;
    private final String adminEmail;
    private final String adminPassword;

    public DemoAccounts(Accounts accounts, Applications applications, Moderation moderation,
            Payments payments, DemoRides rides, ProfileRepository profiles,
            DemoAccountRepository demo, Config config,
            @Value("${wayline.admin.email:}") String adminEmail,
            @Value("${wayline.admin.password:}") String adminPassword) {
        this.accounts = accounts;
        this.applications = applications;
        this.moderation = moderation;
        this.payments = payments;
        this.rides = rides;
        this.profiles = profiles;
        this.demo = demo;
        this.config = config;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    /// Runs once, when the server starts. Not a transaction itself: each
    /// account is made by a transactional method -- [Accounts]' or one of this
    /// class's own -- so a server stopped half way through start-up is left
    /// with whole accounts, and the next start adds the ones that are missing.
    @PostConstruct
    public void seed() throws IOException {
        if (!config.isDevelopmentProfile()) {
            if (adminEmail.length() > 0 && adminPassword.length() >= 8) {
                add(Accounts.email(adminEmail), adminPassword, "Administrator", "+10000000", "",
                        "");
            }
            return;
        }
        // The fortnight is written once, with the accounts it is shared among,
        // and not again when a server with a database that persists restarts.
        boolean first = !accounts.exists(COMFORT_DRIVER);
        // The tests sign in as the first three and expect to find them as new
        // accounts are: no rides behind them, no card saved, cash by default.
        // Under the `test` profile the fortnight is therefore the other
        // accounts' alone.
        boolean everyone = !"test".equals(config.getProfile());

        rider(RIDER, "Riley Rider", "+15550100", "unspecified", 0, everyone);
        driver(DRIVER, "Dana Driver", "+15550101", "female", Fares.STANDARD, "Blue", "Toyota",
                "Prius", "WAY 101", 4, false, true, 0);
        driver(SECOND_DRIVER, "Devon Driver", "+15550102", "male", Fares.STANDARD, "White", "Kia",
                "Niro", "WAY 102", 4, false, false, 0);
        add(ADMIN, PASSWORD, "Alex Admin", "+15550103", "", "");
        driver(COMFORT_DRIVER, "Carmen Driver", "+15550104", "female", Fares.COMFORT, "Black",
                "Tesla", "Model 3", "WAY 103", 4, false, false, 12);
        driver(XL_DRIVER, "Dmitri Driver", "+15550105", "male", Fares.XL, "Silver", "Toyota",
                "Sienna", "WAY 104", 6, true, true, 9);
        rider(SECOND_RIDER, "Robin Rider", "+15550106", "nonbinary", 11, true);
        boolean flag = !accounts.exists(FLAGGED_RIDER);
        rider(FLAGGED_RIDER, "Rae Rider", "+15550107", "female", 6, false);
        if (flag) {
            flag(FLAGGED_RIDER, "Three no-shows at the pickup in one week", 3);
        }
        applicant();
        if (first) {
            rides.seed(everyone ? new String[] {RIDER, SECOND_RIDER, FLAGGED_RIDER}
                            : new String[] {SECOND_RIDER, FLAGGED_RIDER},
                    everyone ? new String[] {DRIVER, SECOND_DRIVER, COMFORT_DRIVER, XL_DRIVER}
                            : new String[] {COMFORT_DRIVER, XL_DRIVER}, everyone);
        }
    }

    /// Marks an account as one the admins flagged some days ago, with the
    /// event that says who did and why.
    @Transactional(rollbackFor = IOException.class)
    void flag(String username, String why, int daysAgo) throws IOException {
        Profile profile = profiles.find(username);
        profile.flagged = true;
        profile.flagReason = why;
        moderation.record(username, ADMIN, Moderation.FLAG, why,
                System.currentTimeMillis() - daysAgo * Days.MILLIS);
    }

    /// An admin, or the plainest kind of account.
    private void add(String username, String password, String name, String phone, String vehicle,
            String plate) throws IOException {
        if (!accounts.exists(username)) {
            accounts.create(username, password, name, phone, true, vehicle, plate, false,
                    ADMIN.equals(username) || !config.isDevelopmentProfile());
        }
    }

    private void rider(String username, String name, String phone, String gender, int daysAgo,
            boolean card) throws IOException {
        if (accounts.exists(username)) {
            return;
        }
        accounts.create(username, PASSWORD, name, phone, true, "", "", false, false, gender,
                opened(daysAgo));
        if (card) {
            PaymentSetupDto setup = payments.startSetup(username, name);
            // Only a simulated card can be made up. With a real processor
            // configured the demo riders pay cash until someone adds a card.
            if (!setup.hosted) {
                CardDto visa = new CardDto();
                visa.number = "4242424242424242";
                visa.expMonth = 12;
                visa.expYear = year() + 3;
                visa.cvc = "123";
                visa.holder = name;
                payments.completeSetup(username, setup.id, visa);
            }
        }
    }

    /// An approved driver, with the application an approval implies: filled
    /// in, its documents accepted.
    private void driver(String username, String name, String phone, String gender, String product,
            String colour, String make, String model, String plate, int seats,
            boolean wheelchair, boolean pets, int daysAgo) throws IOException {
        if (accounts.exists(username)) {
            return;
        }
        accounts.create(username, PASSWORD, name, phone, true, colour + " " + make + " " + model,
                plate, true, false, gender, opened(daysAgo));
        approved(username, name, product, colour, make, model, plate, seats, wheelchair, pets,
                daysAgo);
    }

    /// Fills in the application an account created as a driver was given, and
    /// adds its documents, already accepted.
    @Transactional(rollbackFor = IOException.class)
    void approved(String username, String name, String product, String colour, String make,
            String model, String plate, int seats, boolean wheelchair, boolean pets, int daysAgo)
            throws IOException {
        long handedIn = System.currentTimeMillis() - (daysAgo + 1) * Days.MILLIS;
        DriverApplication form = demo.application(username);
        form.legalName = name;
        form.dateOfBirth = "1988-04-12";
        form.licenceNumber = "D" + (4400000 + plate.charAt(plate.length() - 1) * 7919);
        form.licenceExpiry = (year() + 2) + "-06-30";
        form.vehicleMake = make;
        form.vehicleModel = model;
        form.vehicleYear = year() - 3;
        form.vehicleColor = colour;
        form.vehiclePlate = plate;
        form.vehicleSeats = seats;
        form.product = product;
        form.wheelchair = wheelchair;
        form.pets = pets;
        form.submittedAt = handedIn;
        form.reviewedBy = ADMIN;
        for (int iter = 0; iter < Applications.KINDS.length; iter++) {
            DriverDocument picture = new DriverDocument();
            picture.username = username;
            picture.kind = Applications.KINDS[iter];
            picture.contentType = "image/png";
            picture.status = "accepted";
            picture.uploadedAt = handedIn;
            picture.data = PLACEHOLDER_PICTURE;
            demo.add(picture);
        }
    }

    /// The applicant, taken through the same steps the app takes: a form, five
    /// pictures, and handing it in. Which also means the demo application is
    /// one the server itself accepts as complete.
    private void applicant() throws IOException {
        if (accounts.exists(APPLICANT)) {
            return;
        }
        accounts.create(APPLICANT, PASSWORD, "Avery Applicant", "+15550108", true, "", "", false,
                false, "male", opened(4));
        applications.open(APPLICANT, "", "");
        DriverApplicationDto form = new DriverApplicationDto();
        form.legalName = "Avery Jordan Applicant";
        form.dateOfBirth = "1992-09-03";
        form.licenceNumber = "D4471902";
        form.licenceExpiry = (year() + 3) + "-03-31";
        form.vehicleMake = "Honda";
        form.vehicleModel = "Civic";
        form.vehicleYear = year() - 2;
        form.vehicleColor = "Grey";
        form.vehiclePlate = "WAY 207";
        form.vehicleSeats = 4;
        form.product = Fares.STANDARD;
        applications.save(APPLICANT, form);
        for (int iter = 0; iter < Applications.KINDS.length; iter++) {
            DocumentUploadDto picture = new DocumentUploadDto();
            picture.kind = Applications.KINDS[iter];
            picture.contentType = "image/png";
            picture.dataBase64 = PLACEHOLDER_PICTURE;
            applications.upload(APPLICANT, picture);
        }
        applications.submit(APPLICANT);
        handedIn(APPLICANT, 2);
    }

    /// Dates an application that was just handed in to some days ago, so that
    /// the admins' queue shows one that has been waiting.
    @Transactional(rollbackFor = IOException.class)
    void handedIn(String username, int daysAgo) throws IOException {
        DriverApplication form = demo.application(username);
        form.submittedAt = System.currentTimeMillis() - daysAgo * Days.MILLIS;
    }

    /// When an account opened that many days ago was opened, so that the chart
    /// of new accounts is not one bar.
    private static long opened(int daysAgo) {
        return System.currentTimeMillis() - daysAgo * Days.MILLIS;
    }

    private static int year() {
        return Integer.parseInt(Days.iso(System.currentTimeMillis()).substring(0, 4));
    }
}
