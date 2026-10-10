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

import com.codename1.components.SpanLabel;
import com.codename1.l10n.ParseException;
import com.codename1.l10n.SimpleDateFormat;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.util.Base64;
import com.codenameone.examples.wayline.Telemetry;
import com.codenameone.examples.wayline.api.DocumentDto;
import com.codenameone.examples.wayline.api.DocumentUploadDto;
import com.codenameone.examples.wayline.api.DriverApplicationDto;
import com.codenameone.examples.wayline.api.ProfileDto;
import com.codenameone.examples.wayline.api.UserDto;
import com.codenameone.examples.wayline.net.Account;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.net.Session;
import com.codenameone.examples.wayline.ui.Chart;
import com.codenameone.examples.wayline.ui.Lang;
import com.codenameone.examples.wayline.ui.Photos;
import com.codenameone.examples.wayline.ui.Ui;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/// The application to drive, a step at a time: you, your licence, your car,
/// your documents, and a last look before it is handed in.
///
/// Each step is saved as it is left, so an application can be put down half
/// way and picked up on another day or another phone. The server keeps the
/// draft and decides what "complete" means: handing in an application that is
/// missing something comes back with what, in words, and that is shown as it
/// is rather than worked out again here.
public final class ApplicationForm {
    /// The documents asked for, in the order the server lists them.
    public static final String[] KINDS = {"photo_id", "licence", "selfie",
        "vehicle_registration", "insurance"};
    private static final String[] KIND_NAMES = {"Photo ID", "Driving licence", "A photo of you",
        "Vehicle registration", "Insurance"};
    private static final String[] KIND_HINTS = {"A passport or an identity card",
        "The side with your photo on it", "Your face, in good light, no sunglasses",
        "The paper that says the car is yours", "The page with the policy's dates on it"};
    private static final char[] KIND_ICONS = {FontImage.MATERIAL_BADGE,
        FontImage.MATERIAL_CREDIT_CARD, FontImage.MATERIAL_FACE,
        FontImage.MATERIAL_DESCRIPTION, FontImage.MATERIAL_VERIFIED_USER};
    private static final String[] STEPS = {"About you", "Your licence", "Your car",
        "Your documents", "Review and submit"};
    private static final String[] GENDERS = {"unspecified", "female", "male", "nonbinary"};
    private static final String[] GENDER_NAMES = {"Rather not say", "Woman", "Man",
        "Non-binary"};
    private static final String ISO = "yyyy-MM-dd";

    private final Form form = Ui.form("Drive with Wayline", "Application");
    private final Runnable done;
    private final Container body = Ui.page();
    private final Label stepLabel = Ui.plain("", "WlStepLabel");
    private final Label stepTitle = Ui.label("", "WlTitle");
    private final Container progress = new Container(new BorderLayout());
    private final SpanLabel error = Ui.text("", "WlError");
    private final Button next = Ui.primary("Continue", "next", e -> forward());
    private final Button back = Ui.link("Back", "backStep", e -> backward());
    /// The pictures taken on this visit, to show beside their documents.
    private final Map<String, Image> thumbs = new HashMap<String, Image>();
    private DriverApplicationDto application;
    private String gender;
    private int step;
    /// Reads what the step on screen has in its fields into [#application].
    private Runnable collect = () -> { };

    private ApplicationForm(DriverApplicationDto application, Runnable done) {
        this.application = application;
        this.done = done;
        Ui.back(form, () -> done.run());
        UserDto user = Session.user();
        ProfileDto profile = Account.profile();
        gender = profile != null && profile.gender != null && profile.gender.length() > 0
                ? profile.gender : user.gender == null || user.gender.length() == 0
                ? GENDERS[0] : user.gender;
        if (application.legalName == null || application.legalName.length() == 0) {
            application.legalName = user.displayName;
        }
        stepLabel.setName("step");
        error.setName("error");
        Container head = new Container(BoxLayout.y());
        head.setUIID("WlPage");
        head.add(stepLabel).add(stepTitle).add(progress);
        Container foot = new Container(BoxLayout.y());
        foot.setUIID("WlFooter");
        foot.setSafeArea(true);
        foot.add(error).add(next).add(back);
        form.add(BorderLayout.NORTH, head);
        form.add(BorderLayout.CENTER, body);
        form.add(BorderLayout.SOUTH, foot);
    }

    /// @param done run when the user leaves, with the application handed in or
    ///     not; it shows the screen to go back to
    public static void show(Form previous, DriverApplicationDto application, Runnable done) {
        ApplicationForm wizard = new ApplicationForm(application, done);
        wizard.display();
        wizard.form.show();
    }

    // -------------------------------------------------------------- the steps

    private void display() {
        stepLabel.setText(Lang.tr("Step {0} of {1}", String.valueOf(step + 1),
                String.valueOf(STEPS.length)));
        stepTitle.setText(STEPS[step]);
        progress.removeAll();
        progress.add(BorderLayout.CENTER, new Chart.Meter((step + 1) / (double) STEPS.length));
        body.removeAll();
        // A new step starts at its top, wherever the last one was left.
        body.scrollRectToVisible(0, 0, 1, 1, body);
        error.setText("");
        collect = () -> { };
        if (step == 0) {
            you();
        } else if (step == 1) {
            licence();
        } else if (step == 2) {
            car();
        } else if (step == 3) {
            documents();
        } else {
            review();
        }
        next.setText(step == STEPS.length - 1 ? "Submit application" : "Continue");
        next.setName(step == STEPS.length - 1 ? "submitApplication" : "next");
        back.setHidden(step == 0);
        Ui.refresh(form);
    }

    private void you() {
        body.add(Ui.text("As it is written on your licence. Riders see only your first name.",
                "WlMuted"));
        final TextField name = Ui.field(body, "Legal name", "legalName", "");
        name.setText(application.legalName);
        final Picker born = date(body, "Date of birth", "dateOfBirth", application.dateOfBirth);
        body.add(Ui.label("Gender", "WlLabel"));
        body.add(Ui.choice("gender", GENDER_NAMES, GENDERS, gender, value -> gender = value, 2));
        body.add(Ui.text("Riders who ask for a woman at the wheel are offered to drivers "
                + "who say they are one.", "WlMuted"));
        collect = () -> {
            application.legalName = name.getText().trim();
            application.dateOfBirth = iso(born);
        };
    }

    private void licence() {
        body.add(Ui.text("The number is on the front of the card.", "WlMuted"));
        final TextField number = Ui.field(body, "Licence number", "licenceNumber", "");
        number.setText(application.licenceNumber);
        final Picker expiry = date(body, "Expires", "licenceExpiry", application.licenceExpiry);
        collect = () -> {
            application.licenceNumber = number.getText().trim();
            application.licenceExpiry = iso(expiry);
        };
    }

    private void car() {
        Container pair = new Container(new GridLayout(1, 2));
        Container left = side("WlPairLeft");
        Container right = side("WlPairRight");
        final TextField make = Ui.field(left, "Make", "vehicleMake", "Toyota");
        final TextField model = Ui.field(right, "Model", "vehicleModel", "Prius");
        pair.add(left).add(right);
        body.add(pair);
        Container second = new Container(new GridLayout(1, 2));
        left = side("WlPairLeft");
        right = side("WlPairRight");
        final TextField year = Ui.field(left, "Year", "vehicleYear", "2021", TextArea.NUMERIC);
        final TextField colour = Ui.field(right, "Colour", "vehicleColor", "Blue");
        second.add(left).add(right);
        body.add(second);
        Container third = new Container(new GridLayout(1, 2));
        left = side("WlPairLeft");
        right = side("WlPairRight");
        final TextField plate = Ui.field(left, "Licence plate", "vehiclePlate", "7ABC123");
        final TextField seats = Ui.field(right, "Seats for riders", "vehicleSeats", "4",
                TextArea.NUMERIC);
        third.add(left).add(right);
        body.add(third);
        make.setText(application.vehicleMake);
        model.setText(application.vehicleModel);
        year.setText(application.vehicleYear > 0 ? String.valueOf(application.vehicleYear) : "");
        colour.setText(application.vehicleColor);
        plate.setText(application.vehiclePlate);
        seats.setText(String.valueOf(application.vehicleSeats > 0 ? application.vehicleSeats : 4));
        final String[] product = {application.product == null
                || application.product.length() == 0 ? "standard" : application.product};
        body.add(Ui.label("Kind of ride", "WlLabel"));
        body.add(Ui.choice("product", new String[] {"Standard", "Comfort", "XL"},
                new String[] {"standard", "comfort", "xl"}, product[0],
                value -> product[0] = value));
        body.add(Ui.text("Comfort is a newer, roomier car. XL seats six or more.", "WlMuted"));
        final boolean[] accessible = {application.accessible};
        body.add(Ui.row(FontImage.MATERIAL_ACCESSIBLE, "Takes a wheelchair",
                "A ramp or a lift, and room to secure the chair",
                Ui.toggle("accessible", accessible[0], on -> accessible[0] = on)));
        final boolean[] pets = {application.petFriendly};
        body.add(Ui.row(FontImage.MATERIAL_PETS, "Takes pets",
                "Riders travelling with a pet are offered to you",
                Ui.toggle("petFriendly", pets[0], on -> pets[0] = on)));
        collect = () -> {
            application.vehicleMake = make.getText().trim();
            application.vehicleModel = model.getText().trim();
            application.vehicleYear = number(year.getText());
            application.vehicleColor = colour.getText().trim();
            application.vehiclePlate = plate.getText().trim();
            application.vehicleSeats = number(seats.getText());
            application.product = product[0];
            application.accessible = accessible[0];
            application.petFriendly = pets[0];
        };
    }

    private void documents() {
        body.add(Ui.text("Photograph each one flat, with all four corners in the picture.",
                "WlMuted"));
        for (int iter = 0; iter < KINDS.length; iter++) {
            body.add(document(iter));
        }
    }

    /// One document: what it is, whether we have it, and the two ways to give
    /// it to us.
    private Container document(final int index) {
        final String kind = KINDS[index];
        String status = status(kind);
        boolean have = !"missing".equals(status);
        Container box = new Container(BoxLayout.y());
        box.setUIID("WlOption");
        Label picture = new Label("", "WlThumb");
        Image thumb = thumbs.get(kind);
        if (thumb != null) {
            int side = CN.convertToPixels(9f);
            picture.setIcon(thumb.fill(side, side));
        } else {
            Ui.icon(picture, KIND_ICONS[index], 4f);
        }
        Label state = Ui.badge(have ? "rejected".equals(status) ? "Rejected"
                : "accepted".equals(status) ? "Accepted" : "Added" : "Needed",
                have ? "rejected".equals(status) ? "WlBadgeBad" : "WlBadgeGood" : "WlBadgeWarn");
        state.setName("status-" + kind);
        // The state stands over the name and not beside it: the name is long
        // in some languages and has to keep the whole line.
        state.getAllStyles().setMargin(0, 0, 0, 0);
        Container lines = new Container(BoxLayout.y());
        lines.add(FlowLayout.encloseIn(state));
        lines.add(Ui.label(KIND_NAMES[index], "WlRowTitle"));
        lines.add(Ui.text(KIND_HINTS[index], "WlRowDetail"));
        Container top = new Container(new BorderLayout());
        top.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(picture));
        top.add(BorderLayout.CENTER, lines);
        box.add(top);
        Container ways = new Container(new GridLayout(1, 2));
        Button camera = Ui.link(have ? "Retake" : "Camera", "camera-" + kind,
                e -> Photos.camera((jpeg, taken) -> upload(kind, jpeg, taken)));
        Ui.icon(camera, FontImage.MATERIAL_PHOTO_CAMERA, 3f);
        Button gallery = Ui.link("Gallery", "gallery-" + kind,
                e -> Photos.gallery((jpeg, taken) -> upload(kind, jpeg, taken)));
        Ui.icon(gallery, FontImage.MATERIAL_PHOTO_LIBRARY, 3f);
        ways.add(camera).add(gallery);
        box.add(ways);
        return box;
    }

    private void upload(final String kind, byte[] jpeg, final Image picture) {
        DocumentUploadDto document = new DocumentUploadDto();
        document.kind = kind;
        document.contentType = "image/jpeg";
        document.dataBase64 = Base64.encodeNoNewline(jpeg);
        Api.driving().upload(document, Net.to(saved -> {
            application = saved;
            thumbs.put(kind, picture);
            if (step == 3) {
                display();
            }
        }, this::failed));
    }

    private void review() {
        body.add(Ui.text("Have a last look. You can change anything until it is handed in.",
                "WlMuted"));
        Ui.section(body, "You");
        body.add(Ui.fact("Legal name", application.legalName));
        body.add(Ui.fact("Date of birth", application.dateOfBirth));
        body.add(Ui.fact("Licence", application.licenceNumber));
        body.add(Ui.fact("Licence expires", application.licenceExpiry));
        Ui.section(body, "Your car");
        body.add(Ui.fact("Car", ApplyForm.car(application)));
        body.add(Ui.fact("Licence plate", application.vehiclePlate));
        body.add(Ui.fact("Seats for riders", String.valueOf(application.vehicleSeats)));
        body.add(Ui.fact("Kind of ride", Lang.tr(Ui.product(application.product))));
        body.add(Ui.fact("Takes a wheelchair", Lang.tr(application.accessible ? "Yes" : "No")));
        body.add(Ui.fact("Takes pets", Lang.tr(application.petFriendly ? "Yes" : "No")));
        Ui.section(body, "Documents");
        int have = KINDS.length - ApplyForm.missing(application);
        Container documents = Ui.fact("Photos added", have + " / " + KINDS.length);
        documents.getComponentAt(1).setName("documentsAdded");
        body.add(documents);
        body.add(Ui.text("By submitting you confirm that all of this is true and that the "
                + "documents are your own.", "WlMuted"));
    }

    // ----------------------------------------------------------- moving along

    private void forward() {
        collect.run();
        // What the step asks for, asked for here: the server would take a
        // half-empty draft, and say what is missing only at the very end.
        if (incomplete()) {
            error.setText(Lang.tr("Fill in everything on this step to continue."));
            Ui.refresh(form);
            return;
        }
        next.setEnabled(false);
        error.setText("");
        if (step == STEPS.length - 1) {
            Api.driving().submit(Net.to(submitted -> {
                Telemetry.applicationSubmitted();
                next.setEnabled(true);
                Ui.say("Application sent. We will be in touch.");
                done.run();
            }, this::failed));
            return;
        }
        if (step == 3) {
            // Nothing typed on this step: each picture was sent as it was taken.
            next.setEnabled(true);
            step++;
            display();
            return;
        }
        final boolean named = step == 0;
        Api.driving().save(application, Net.to(saved -> {
            application = saved;
            next.setEnabled(true);
            if (named) {
                // The gender is the account's, not the application's.
                ProfileDto profile = Account.profile();
                Account.saveProfile(profile == null ? Session.user().displayName : profile.name,
                        gender, () -> { }, (status, message) -> { });
            }
            step++;
            display();
        }, this::failed));
    }

    private boolean incomplete() {
        if (step == 0) {
            return empty(application.legalName) || empty(application.dateOfBirth);
        }
        if (step == 1) {
            return empty(application.licenceNumber) || empty(application.licenceExpiry);
        }
        if (step == 2) {
            return empty(application.vehicleMake) || empty(application.vehicleModel)
                    || empty(application.vehicleColor) || empty(application.vehiclePlate)
                    || application.vehicleYear <= 0 || application.vehicleSeats <= 0;
        }
        return false;
    }

    private static boolean empty(String text) {
        return text == null || text.length() == 0;
    }

    private void backward() {
        collect.run();
        if (step > 0) {
            step--;
            display();
        }
    }

    private void failed(int status, String message) {
        next.setEnabled(true);
        // The server's own sentence: what is missing, or what is wrong with it.
        error.setText(Lang.tr(message));
        Ui.refresh(form);
    }

    private String status(String kind) {
        for (int iter = 0; application.documents != null && iter < application.documents.size();
                iter++) {
            DocumentDto document = application.documents.get(iter);
            if (kind.equals(document.kind)) {
                return document.status;
            }
        }
        return "missing";
    }

    private static Container side(String uiid) {
        Container side = new Container(BoxLayout.y());
        side.setUIID(uiid);
        return side;
    }

    /// A field for a date, which opens the platform's own way of choosing one.
    private static Picker date(Container to, String caption, String name, String value) {
        Picker picker = new Picker();
        picker.setType(Display.PICKER_TYPE_DATE);
        picker.setUIID("WlField");
        picker.setName(name);
        picker.setFormatter(new SimpleDateFormat("d MMM yyyy"));
        if (value != null && value.length() > 0) {
            try {
                picker.setDate(new SimpleDateFormat(ISO).parse(value));
            } catch (ParseException unreadable) {
                // Left on today: the server would not have stored such a date.
                picker.setDate(new Date());
            }
        }
        to.add(Ui.label(caption, "WlLabel")).add(picker);
        return picker;
    }

    private static String iso(Picker picker) {
        Date date = picker.getDate();
        return date == null ? "" : new SimpleDateFormat(ISO).format(date);
    }

    private static int number(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.length() == 0 || trimmed.length() > 6) {
            return 0;
        }
        for (int iter = 0; iter < trimmed.length(); iter++) {
            if (trimmed.charAt(iter) < '0' || trimmed.charAt(iter) > '9') {
                return 0;
            }
        }
        return Integer.parseInt(trimmed);
    }
}
