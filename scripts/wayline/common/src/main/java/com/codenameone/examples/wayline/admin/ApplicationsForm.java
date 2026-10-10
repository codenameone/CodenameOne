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
package com.codenameone.examples.wayline.admin;

import com.codename1.components.ScaleImageLabel;
import com.codename1.io.Log;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.util.Base64;
import com.codenameone.examples.wayline.Telemetry;
import com.codenameone.examples.wayline.api.DocumentContentDto;
import com.codenameone.examples.wayline.api.DocumentDto;
import com.codenameone.examples.wayline.api.DriverApplicationDto;
import com.codenameone.examples.wayline.api.ReasonDto;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.ui.Lang;
import com.codenameone.examples.wayline.ui.Ui;
import java.util.List;

/// The people who asked to drive, and the decision about each.
///
/// An application is approved or sent back with a reason. Approving is what
/// makes the account a driver; nothing else in the app or on the server does.
/// The documents are pictures only an admin and the applicant can fetch, one
/// at a time, when they are opened.
public final class ApplicationsForm {
    private static final String[] KINDS = {"photo_id", "licence", "selfie",
        "vehicle_registration", "insurance"};
    private static final String[] KIND_NAMES = {"Photo ID", "Driving licence", "A photo of you",
        "Vehicle registration", "Insurance"};

    private final Panes panes;
    private final Container list = Ui.list("applications");
    private String status = "pending";

    private ApplicationsForm(Panes panes) {
        this.panes = panes;
        Container top = new Container(BoxLayout.y());
        top.setUIID("WlPage");
        top.add(Ui.choice("status", new String[] {"Waiting", "Approved", "Sent back"},
                new String[] {"pending", "approved", "rejected"}, status, value -> {
                    status = value;
                    load();
                }));
        Ui.pull(list, this::load);
        panes.list("Applications", "Applications", Panes.listed(top, list), this::load);
    }

    static void show(Form previous) {
        show(Panes.phone(previous));
    }

    static void show(Panes panes) {
        new ApplicationsForm(panes);
    }

    private void load() {
        final String asked = status;
        Api.admin().applications(asked, Net.to(found -> {
            if (asked.equals(status)) {
                fill(found);
            }
        }, Ui.failed(list, this::load)));
    }

    private void fill(List<DriverApplicationDto> found) {
        list.removeAll();
        if (found == null || found.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_INBOX, "Nothing here",
                    "pending".equals(status) ? "Every application has had its answer." : null));
        }
        for (int iter = 0; found != null && iter < found.size(); iter++) {
            final DriverApplicationDto application = found.get(iter);
            Container row = Ui.row((char) 0, application.name, car(application),
                    Ui.badge(word(application.status), badge(application.status)));
            row.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(
                    Ui.avatar(application.name)));
            final Container picked = row;
            list.add(Ui.tap(row, "application-" + application.username, e -> {
                panes.picked(picked);
                review(application.username);
            }));
        }
        panes.refresh();
    }

    /// One application in full, fetched fresh: it may have been answered by
    /// another admin since the list was drawn.
    private void review(final String username) {
        Api.admin().application(username, Net.to(application -> show(application), Ui::fail));
    }

    private void show(final DriverApplicationDto application) {
        Container page = Ui.page();
        Label state = Ui.badge(word(application.status), badge(application.status));
        state.setName("applicationStatus");
        page.add(Ui.person(application.name, application.username, state));
        if ("rejected".equals(application.status)) {
            page.add(Ui.text(application.rejectionReason, "WlNotice"));
        }

        Ui.section(page, "Applicant");
        page.add(Ui.fact("Legal name", application.legalName));
        page.add(Ui.fact("Date of birth", application.dateOfBirth));
        page.add(Ui.fact("Licence", application.licenceNumber));
        page.add(Ui.fact("Licence expires", application.licenceExpiry));
        if (application.submittedAt > 0) {
            page.add(Ui.fact("Submitted", Ui.when(application.submittedAt)));
        }
        if (application.reviewedAt > 0) {
            page.add(Ui.fact("Reviewed", Ui.when(application.reviewedAt)));
            page.add(Ui.fact("Reviewed by", application.reviewedBy));
        }

        Ui.section(page, "Vehicle");
        page.add(Ui.fact("Car", car(application)));
        page.add(Ui.fact("Licence plate", application.vehiclePlate));
        page.add(Ui.fact("Seats for riders", String.valueOf(application.vehicleSeats)));
        page.add(Ui.fact("Kind of ride", Lang.tr(Ui.product(application.product))));
        page.add(Ui.fact("Takes a wheelchair", Lang.tr(application.accessible ? "Yes" : "No")));
        page.add(Ui.fact("Takes pets", Lang.tr(application.petFriendly ? "Yes" : "No")));

        Ui.section(page, "Documents");
        for (int iter = 0; iter < KINDS.length; iter++) {
            final String kind = KINDS[iter];
            final String title = KIND_NAMES[iter];
            String had = status(application, kind);
            boolean missing = "missing".equals(had);
            Container row = Ui.row(FontImage.MATERIAL_IMAGE, title,
                    missing ? "Not provided" : "Tap to open", Ui.badge(
                            missing ? "Missing" : "Provided",
                            missing ? "WlBadgeBad" : "WlBadgeGood"));
            if (missing) {
                page.add(row);
            } else {
                page.add(Ui.tap(row, "document-" + kind,
                        e -> picture(application.username, kind, title)));
            }
        }

        if ("pending".equals(application.status)) {
            page.add(Ui.primary("Approve", "approve", e -> Api.admin().approve(
                    application.username, Net.to(approved -> {
                        Telemetry.applicationReviewed(true);
                        Ui.say(Lang.tr("{0} can now drive", application.name));
                        panes.home();
                    }, Ui::fail))));
            page.add(Ui.danger("Send back", "reject", e -> panes.reason("Send back",
                    Lang.tr("{0} sees what you write, and can apply again once it is put "
                            + "right.", application.name), "Send back", why -> {
                        ReasonDto reason = new ReasonDto();
                        reason.reason = why;
                        Api.admin().reject(application.username, reason, Net.to(rejected -> {
                            Telemetry.applicationReviewed(false);
                            panes.home();
                        }, Ui::fail));
                    })));
        }
        panes.detail(application.name, "ApplicationReview", page);
    }

    /// A document, as large as the screen allows.
    private void picture(String username, String kind, String title) {
        final Container stage = new Container(new BorderLayout());
        stage.setUIID("WlPicture");
        panes.over(title, "Document", stage);
        Api.admin().document(username, kind, Net.to(content -> {
            Image image = decode(content);
            if (image == null) {
                stage.add(BorderLayout.CENTER, Ui.empty(FontImage.MATERIAL_BROKEN_IMAGE,
                        "This picture could not be opened", null));
            } else {
                ScaleImageLabel shown = new ScaleImageLabel(image);
                shown.setUIID("WlPicture");
                shown.setBackgroundType(Style.BACKGROUND_IMAGE_SCALED_FIT);
                shown.setName("picture");
                stage.add(BorderLayout.CENTER, shown);
            }
            panes.refresh();
        }, Ui::fail));
    }

    private static Image decode(DocumentContentDto content) {
        if (content == null || content.dataBase64 == null || content.dataBase64.length() == 0) {
            return null;
        }
        try {
            byte[] data = Base64.decode(content.dataBase64.getBytes("UTF-8"));
            return data == null ? null : Image.createImage(data, 0, data.length);
        } catch (java.io.UnsupportedEncodingException impossible) {
            Log.e(impossible);
            return null;
        } catch (RuntimeException unreadable) {
            // Not a picture after all; the screen says so.
            Log.e(unreadable);
            Telemetry.error(unreadable);
            return null;
        }
    }

    private static String status(DriverApplicationDto application, String kind) {
        for (int iter = 0; application.documents != null && iter < application.documents.size();
                iter++) {
            DocumentDto document = application.documents.get(iter);
            if (kind.equals(document.kind)) {
                return document.status;
            }
        }
        return "missing";
    }

    private static String car(DriverApplicationDto application) {
        StringBuilder car = new StringBuilder();
        String[] words = {application.vehicleColor, application.vehicleMake,
            application.vehicleModel,
            application.vehicleYear > 0 ? String.valueOf(application.vehicleYear) : ""};
        for (int iter = 0; iter < words.length; iter++) {
            if (words[iter] != null && words[iter].length() > 0) {
                car.append(car.length() > 0 ? " " : "").append(words[iter]);
            }
        }
        return car.toString();
    }

    static String word(String status) {
        return "pending".equals(status) ? "Waiting" : "approved".equals(status) ? "Approved"
                : "rejected".equals(status) ? "Sent back" : "Draft";
    }

    static String badge(String status) {
        return "pending".equals(status) ? "WlBadgeWarn" : "approved".equals(status)
                ? "WlBadgeGood" : "rejected".equals(status) ? "WlBadgeBad" : "WlBadge";
    }
}
