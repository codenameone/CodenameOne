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
package com.codenameone.examples.wayline.driver;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.wayline.Telemetry;
import com.codenameone.examples.wayline.api.DocumentDto;
import com.codenameone.examples.wayline.api.DriverApplicationDto;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.net.Session;
import com.codenameone.examples.wayline.ui.Lang;
import com.codenameone.examples.wayline.ui.Menu;
import com.codenameone.examples.wayline.ui.Nav;
import com.codenameone.examples.wayline.ui.Ui;

/// The driver's side of the app for someone who is not a driver yet: where
/// their application stands, and the way on from there.
///
/// Whether anyone drives is the server's decision and an admin's: an
/// application is filled in, handed in, and approved or sent back with a
/// reason. This screen shows which of those it is. There is no way from here
/// to going online -- the screen that has that button is not built for an
/// account the server does not call a driver, and the server refuses the call
/// from one anyway.
public class ApplyForm implements Nav.Home {
    private static final int POLL_MILLIS = 5000;

    private final Form form = Ui.home("Apply");
    private final Menu menu = new Menu(form, Nav.DRIVER);
    private final Container page = Ui.page();
    private String showing = "";

    public ApplyForm() {
        menu.item(FontImage.MATERIAL_ASSIGNMENT, "Your application", "home", () -> { });
        Container bar = new Container(new BorderLayout());
        bar.setUIID("WlHomeBar");
        bar.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(menu.flatButton()));
        bar.add(BorderLayout.CENTER, FlowLayout.encloseLeftMiddle(
                Ui.label("Drive with Wayline", "WlHeading")));
        page.setSafeArea(false);
        Container screen = new Container(new BorderLayout());
        // The strip the status bar is drawn in is the bar's colour, not
        // whatever is behind the screen.
        screen.setUIID("WlForm");
        screen.setSafeArea(true);
        screen.add(BorderLayout.NORTH, bar);
        screen.add(BorderLayout.CENTER, page);
        form.add(screen);
        menu.install();
        form.addShowListener(e -> load());
        // An answer can come at any moment, and is worth showing when it does.
        UITimer.timer(POLL_MILLIS, true, form, this::load);
    }

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        load();
    }

    private void load() {
        Api.driving().application(Net.to(this::fill, (status, message) -> { }));
    }

    private void fill(final DriverApplicationDto application) {
        String status = application.status == null ? "none" : application.status;
        if ("approved".equals(status)) {
            // The account is a driver's now: say so to the app, and let it
            // build the screen a driver gets.
            if (!"approved".equals(showing)) {
                showing = "approved";
                Session.refreshUser(Nav::home, Ui::fail);
            }
            return;
        }
        String now = status + ":" + application.submittedAt + ":" + missing(application);
        if (now.equals(showing)) {
            return;
        }
        showing = now;
        page.removeAll();
        if ("pending".equals(status)) {
            pending(application);
        } else if ("rejected".equals(status)) {
            rejected(application);
        } else {
            invite(application, "draft".equals(status) && started(application));
        }
        Ui.refresh(form);
    }

    /// Not applied yet, or part of the way through.
    private void invite(final DriverApplicationDto application, boolean started) {
        Container banner = Ui.banner(FontImage.MATERIAL_DIRECTIONS_CAR,
                started ? "Finish your application" : "Earn on your own schedule",
                started ? Lang.tr("{0} of 5 documents added. Pick up where you left off.",
                        String.valueOf(5 - missing(application)))
                        : "Tell us about you and your car, add five photos, and we take it "
                        + "from there.", "WlBannerIcon");
        page.add(banner);
        Label state = Ui.label(started ? "Draft" : "Not started", "WlBadge");
        state.setName("applicationStatus");
        page.add(FlowLayout.encloseCenter(state));
        Ui.section(page, "What you will need");
        page.add(Ui.row(FontImage.MATERIAL_BADGE, "A driving licence",
                "Its number, its expiry date and a photo of it", null));
        page.add(Ui.row(FontImage.MATERIAL_DIRECTIONS_CAR, "A car with four doors",
                "Registered and insured, with the papers to show it", null));
        page.add(Ui.row(FontImage.MATERIAL_PHOTO_CAMERA, "About ten minutes",
                "Most of it is taking five photos", null));
        final boolean begun = started;
        page.add(Ui.primary(started ? "Continue application" : "Start application", "apply",
                e -> {
                    if (!begun) {
                        Telemetry.applicationStarted(false);
                    }
                    ApplicationForm.show(form, application, this::refreshed);
                }));
    }

    private void pending(DriverApplicationDto application) {
        page.add(Ui.banner(FontImage.MATERIAL_HOURGLASS_TOP, "Application under review",
                "We have everything we asked for. Someone on our team is reading it.",
                "WlBannerIconWarn"));
        Label state = Ui.label("Under review", "WlBadgeWarn");
        state.setName("applicationStatus");
        page.add(FlowLayout.encloseCenter(state));
        Ui.section(page, "What happens next");
        page.add(Ui.row(FontImage.MATERIAL_CHECK_CIRCLE, "Application received",
                application.submittedAt > 0 ? Ui.when(application.submittedAt) : null, null));
        page.add(Ui.row(FontImage.MATERIAL_FACT_CHECK, "Documents checked",
                "Usually within two working days", null));
        page.add(Ui.row(FontImage.MATERIAL_DIRECTIONS_CAR, "You go online",
                "This screen becomes your driver's home the moment you are approved", null));
        Ui.section(page, "Your car");
        page.add(Ui.row(FontImage.MATERIAL_DIRECTIONS_CAR, car(application),
                application.vehiclePlate, null));
    }

    private void rejected(final DriverApplicationDto application) {
        page.add(Ui.banner(FontImage.MATERIAL_ERROR_OUTLINE, "We could not approve this yet",
                "Read what was missing, put it right, and send the application again.",
                "WlBannerIconBad"));
        Label state = Ui.label("Changes needed", "WlBadgeBad");
        state.setName("applicationStatus");
        page.add(FlowLayout.encloseCenter(state));
        Ui.section(page, "What the reviewer said");
        Container said = new Container(BoxLayout.y());
        said.setUIID("WlPanel");
        com.codename1.components.SpanLabel reason = Ui.text("", "WlText");
        // The reviewer's own words, in whatever language they wrote them.
        reason.setShouldLocalize(false);
        reason.setText(application.rejectionReason);
        reason.setName("rejectionReason");
        said.add(reason);
        page.add(said);
        page.add(Ui.primary("Fix and resubmit", "apply", e -> {
            Telemetry.applicationStarted(true);
            ApplicationForm.show(form, application, this::refreshed);
        }));
    }

    /// Back from the form: whatever was done there, show how things stand now.
    private void refreshed() {
        showing = "";
        form.showBack();
    }

    private static boolean started(DriverApplicationDto application) {
        return application.legalName != null && application.legalName.length() > 0
                || missing(application) < 5;
    }

    static int missing(DriverApplicationDto application) {
        int missing = 0;
        for (int iter = 0; application.documents != null && iter < application.documents.size();
                iter++) {
            DocumentDto document = application.documents.get(iter);
            if ("missing".equals(document.status)) {
                missing++;
            }
        }
        return application.documents == null ? 5 : missing;
    }

    static String car(DriverApplicationDto application) {
        StringBuilder car = new StringBuilder();
        append(car, application.vehicleColor);
        append(car, application.vehicleMake);
        append(car, application.vehicleModel);
        if (application.vehicleYear > 0) {
            append(car, String.valueOf(application.vehicleYear));
        }
        return car.toString();
    }

    private static void append(StringBuilder to, String word) {
        if (word != null && word.length() > 0) {
            to.append(to.length() > 0 ? " " : "").append(word);
        }
    }
}
