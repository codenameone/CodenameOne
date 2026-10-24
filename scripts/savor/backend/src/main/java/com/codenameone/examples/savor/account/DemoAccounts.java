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
package com.codenameone.examples.savor.account;

import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.savor.Days;
import com.codenameone.examples.savor.api.CardDto;
import com.codenameone.examples.savor.api.DocumentUploadDto;
import com.codenameone.examples.savor.api.CourierApplicationDto;
import com.codenameone.examples.savor.api.PaymentSetupDto;
import com.codenameone.examples.savor.domain.CourierApplication;
import com.codenameone.examples.savor.domain.CourierDocument;
import com.codenameone.examples.savor.domain.Profile;
import com.codenameone.examples.savor.onboarding.Applications;
import com.codenameone.examples.savor.pay.Payments;
import com.codenameone.examples.savor.repository.CourierApplicationRepository;
import com.codenameone.examples.savor.repository.CourierDocumentRepository;
import com.codenameone.examples.savor.repository.ProfileRepository;
import com.codenameone.examples.savor.catalog.DemoRestaurants;
import com.codenameone.examples.savor.order.DemoOrders;

import java.io.IOException;
import java.time.Instant;

/// The accounts a server starts with.
///
/// On a development profile, enough of each kind that every screen of the app
/// has something to show straight after a checkout: customers, approved couriers,
/// a merchant who runs one of the restaurants ([DemoRestaurants]), an admin,
/// someone whose application to deliver is waiting for an answer, an account the
/// admins have flagged, and a fortnight of orders between them ([DemoOrders]).
///
/// Anywhere else, at most one: the admin named by `savor.admin.email` and
/// `savor.admin.password` (or the same two names in the environment, in upper
/// case with `_` for each `.`), because a new deployment needs somebody able to
/// grant the first roles. It is created once; changing the setting later does
/// not change the password of an account that exists.
@Component
public class DemoAccounts {
    public static final String CUSTOMER = "customer@savor.example";
    public static final String COURIER = "courier@savor.example";
    public static final String SECOND_COURIER = "courier2@savor.example";
    public static final String ADMIN = "admin@savor.example";
    /// Two more couriers, who carry the fortnight of past orders.
    public static final String THIRD_COURIER = "courier3@savor.example";
    public static final String FOURTH_COURIER = "courier4@savor.example";
    /// Runs the first demo restaurant's kitchen.
    public static final String MERCHANT = "merchant@savor.example";
    public static final String SECOND_CUSTOMER = "customer2@savor.example";
    /// A customer the admins have flagged.
    public static final String FLAGGED_CUSTOMER = "customer3@savor.example";
    /// Someone whose application to deliver is complete and waiting for an admin.
    public static final String APPLICANT = "applicant@savor.example";
    /// The password of every demo account. Not a secret: these accounts exist
    /// only where the profile says the server is not a real one.
    public static final String PASSWORD = "savor-demo";

    /// A small grey chequered PNG, standing in for every photographed document
    /// of the demo accounts.
    static final String PLACEHOLDER_PICTURE = "iVBORw0KGgoAAAANSUhEUgAAADAAAAAeCAAAAABPz8IdAAAAJUlE"
            + "QVR42mN4AgUnoIAQn2FEaiBWIYw/MjWMpqXRtDSaluioAQAjjbP9bRBUyQAAAABJRU5ErkJggg==";

    private final Accounts accounts;
    private final Applications applications;
    private final Moderation moderation;
    private final Payments payments;
    private final DemoRestaurants restaurants;
    private final DemoOrders orders;
    private final ProfileRepository profiles;
    private final CourierApplicationRepository forms;
    private final CourierDocumentRepository documents;
    private final Config config;
    private final String adminEmail;
    private final String adminPassword;

    public DemoAccounts(Accounts accounts, Applications applications, Moderation moderation,
            Payments payments, DemoRestaurants restaurants, DemoOrders orders,
            ProfileRepository profiles,
            CourierApplicationRepository forms, CourierDocumentRepository documents, Config config,
            @Value("${savor.admin.email:}") String adminEmail,
            @Value("${savor.admin.password:}") String adminPassword) {
        this.accounts = accounts;
        this.applications = applications;
        this.moderation = moderation;
        this.payments = payments;
        this.restaurants = restaurants;
        this.orders = orders;
        this.profiles = profiles;
        this.forms = forms;
        this.documents = documents;
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
        // The marker is an account made before the orders, so a start-up that
        // failed between the two leaves a development database with the
        // accounts and no fortnight; the answer there is to drop the database.
        boolean first = !accounts.exists(THIRD_COURIER);
        // The tests sign in as the first accounts and expect to find them as
        // new accounts are: no orders behind them, no card saved, cash by
        // default. Under the `test` profile the fortnight is the others' alone.
        boolean everyone = !"test".equals(config.getProfile());

        customer(CUSTOMER, "Riley Customer", "+15550100", 0, everyone);
        courier(COURIER, "Dana Courier", "+15550101", "Blue", "Vespa", "Primavera", "SAV 101", 0);
        courier(SECOND_COURIER, "Devon Courier", "+15550102", "Black", "Trek", "Allant+",
                "SAV 102", 0);
        add(ADMIN, PASSWORD, "Alex Admin", "+15550103", "", "");
        courier(THIRD_COURIER, "Carmen Courier", "+15550104", "Red", "Honda", "PCX", "SAV 103",
                12);
        courier(FOURTH_COURIER, "Dmitri Courier", "+15550105", "Silver", "Toyota", "Yaris",
                "SAV 104", 9);
        customer(SECOND_CUSTOMER, "Robin Customer", "+15550106", 11, true);
        boolean flag = !accounts.exists(FLAGGED_CUSTOMER);
        customer(FLAGGED_CUSTOMER, "Rae Customer", "+15550107", 6, false);
        if (flag) {
            flag(FLAGGED_CUSTOMER, "Three orders refused at the door in one week", 3);
        }
        customer(MERCHANT, "Morgan Merchant", "+15550109", 20, false);
        applicant();
        restaurants.seed(MERCHANT);
        if (first) {
            orders.seed(everyone ? new String[] {CUSTOMER, SECOND_CUSTOMER, FLAGGED_CUSTOMER}
                            : new String[] {SECOND_CUSTOMER, FLAGGED_CUSTOMER},
                    everyone ? new String[] {COURIER, SECOND_COURIER, THIRD_COURIER,
                        FOURTH_COURIER} : new String[] {THIRD_COURIER, FOURTH_COURIER});
        }
    }

    /// Marks an account as one the admins flagged some days ago, with the
    /// event that says who did and why.
    @Transactional(rollbackFor = IOException.class)
    void flag(String username, String why, int daysAgo) throws IOException {
        Profile profile = profiles.findById(username);
        profile.setFlagged(true);
        profile.setFlagReason(why);
        moderation.record(username, ADMIN, Moderation.FLAG, why,
                Instant.now().minusMillis(daysAgo * Days.MILLIS));
    }

    /// An admin, or the plainest kind of account.
    private void add(String username, String password, String name, String phone, String vehicle,
            String plate) throws IOException {
        if (accounts.exists(username)) {
            return;
        }
        try {
            accounts.create(username, password, name, phone, true, vehicle, plate, false,
                    ADMIN.equals(username) || !config.isDevelopmentProfile());
        } catch (IOException | RuntimeException failed) {
            // Two servers started together against a new database both find
            // no admin and both make one; the second insert fails on the name.
            // create() is one transaction, so the loser left nothing behind,
            // and the account the other server made is the one wanted. Any
            // other failure leaves no account, and is the start-up's to report.
            if (!accounts.exists(username)) {
                throw failed;
            }
        }
    }

    private void customer(String username, String name, String phone, int daysAgo,
            boolean card) throws IOException {
        if (accounts.exists(username)) {
            return;
        }
        accounts.create(username, PASSWORD, name, phone, true, "", "", false, false,
                opened(daysAgo));
        if (card) {
            PaymentSetupDto setup = payments.startSetup(username, name);
            // Only a simulated card can be made up. With a real processor
            // configured the demo customers pay cash until someone adds a card.
            if (!setup.hosted()) {
                CardDto visa = new CardDto("4242424242424242", 12, year() + 3, "123", name);
                payments.completeSetup(username, setup.id(), visa);
            }
        }
    }

    /// An approved courier, with the application an approval implies: filled
    /// in, its documents accepted.
    private void courier(String username, String name, String phone, String colour, String make,
            String model, String plate, int daysAgo) throws IOException {
        if (accounts.exists(username)) {
            return;
        }
        accounts.create(username, PASSWORD, name, phone, true, colour + " " + make + " " + model,
                plate, true, false, opened(daysAgo));
        approved(username, name, colour, make, model, plate, daysAgo);
    }

    /// Fills in the application an account created as a courier was given, and
    /// adds its documents, already accepted.
    @Transactional(rollbackFor = IOException.class)
    void approved(String username, String name, String colour, String make, String model,
            String plate, int daysAgo) throws IOException {
        long handedIn = System.currentTimeMillis() - (daysAgo + 1) * Days.MILLIS;
        CourierApplication form = forms.findById(username);
        form.setLegalName(name);
        form.setDateOfBirth("1988-04-12");
        form.setLicenceNumber("D" + (4400000 + plate.charAt(plate.length() - 1) * 7919));
        form.setLicenceExpiry((year() + 2) + "-06-30");
        form.setVehicleMake(make);
        form.setVehicleModel(model);
        form.setVehicleYear(year() - 3);
        form.setVehicleColor(colour);
        form.setVehiclePlate(plate);
        form.setSubmittedAt(Instant.ofEpochMilli(handedIn));
        form.setReviewedBy(ADMIN);
        for (int iter = 0; iter < Applications.KINDS.length; iter++) {
            CourierDocument picture = new CourierDocument();
            picture.setUsername(username);
            picture.setKind(Applications.KINDS[iter]);
            picture.setContentType("image/png");
            picture.setStatus("accepted");
            picture.setUploadedAt(Instant.ofEpochMilli(handedIn));
            picture.setData(PLACEHOLDER_PICTURE);
            documents.insert(picture);
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
                false, opened(4));
        applications.open(APPLICANT, "", "");
        // What an applicant fills in. The rest (who, the status, the documents,
        // the dates) is the server's to say and is not read from a form.
        CourierApplicationDto form = new CourierApplicationDto(null, null, null,
                "Avery Jordan Applicant", "1992-09-03", "D4471902", (year() + 3) + "-03-31",
                "Honda", "Civic", year() - 2, "Grey", "SAV 207", null, null, 0L, 0L, null);
        applications.save(APPLICANT, form);
        for (int iter = 0; iter < Applications.KINDS.length; iter++) {
            DocumentUploadDto picture = new DocumentUploadDto(Applications.KINDS[iter],
                    "image/png", PLACEHOLDER_PICTURE);
            applications.upload(APPLICANT, picture);
        }
        applications.submit(APPLICANT);
        handedIn(APPLICANT, 2);
    }

    /// Dates an application that was just handed in to some days ago, so that
    /// the admins' queue shows one that has been waiting.
    @Transactional(rollbackFor = IOException.class)
    void handedIn(String username, int daysAgo) throws IOException {
        CourierApplication form = forms.findById(username);
        form.setSubmittedAt(Instant.ofEpochMilli(System.currentTimeMillis() - daysAgo * Days.MILLIS));
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
