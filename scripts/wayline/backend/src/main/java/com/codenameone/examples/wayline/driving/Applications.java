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
package com.codenameone.examples.wayline.driving;

import com.codename1.backend.Base64;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codenameone.examples.wayline.Days;
import com.codenameone.examples.wayline.Roles;
import com.codenameone.examples.wayline.Text;
import com.codenameone.examples.wayline.account.Moderation;
import com.codenameone.examples.wayline.account.ProfileRepository;
import com.codenameone.examples.wayline.api.DocumentContentDto;
import com.codenameone.examples.wayline.api.DocumentDto;
import com.codenameone.examples.wayline.api.DocumentUploadDto;
import com.codenameone.examples.wayline.api.DriverApplicationDto;
import com.codenameone.examples.wayline.domain.DriverApplication;
import com.codenameone.examples.wayline.domain.DriverDocument;
import com.codenameone.examples.wayline.domain.Profile;
import com.codenameone.examples.wayline.ride.Fares;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// Applications to drive, from the first field filled in to an admin's answer.
///
/// ```
/// (none) -> draft -> pending -> approved
///             ^         |
///             +---------+-> rejected -> pending   corrected and handed in again
/// ```
///
/// The applicant fills in a form and uploads five documents, in any order and
/// over as many visits as it takes; nothing is checked for completeness until
/// they hand it in. An admin then approves it, which is the one thing that
/// grants the DRIVER role, or rejects it with a reason the applicant reads
/// before correcting it and handing it in again.
///
/// `approved` is what the rest of the server reads: going on line and being
/// offered rides both ask for it here, and not for the role alone.
@Component
public class Applications {
    public static final String NONE = "none";
    public static final String DRAFT = "draft";
    public static final String PENDING = "pending";
    public static final String APPROVED = "approved";
    public static final String REJECTED = "rejected";
    /// The documents an application needs, in the order the app asks for them.
    public static final String[] KINDS = {"photo_id", "licence", "selfie", "vehicle_registration",
        "insurance"};
    /// The largest picture taken, decoded: 1.5 MB. The app shrinks a photograph
    /// well below this before sending it; the cap is for whoever does not.
    static final int MAX_DOCUMENT_BYTES = 1572864;

    private static final String[] KIND_NAMES = {"a photo of your ID", "a photo of your licence",
        "a selfie", "the vehicle's registration", "proof of insurance"};

    private static final int LIST_LIMIT = 200;

    private final ApplicationRepository applications;
    private final ProfileRepository profiles;
    private final JdbcUserDetailsManager users;
    private final Moderation moderation;

    public Applications(ApplicationRepository applications, ProfileRepository profiles,
            JdbcUserDetailsManager users, Moderation moderation) {
        this.applications = applications;
        this.profiles = profiles;
        this.users = users;
        this.moderation = moderation;
    }

    // ------------------------------------------------------------- applicant

    /// Where an account stands: one of the five statuses.
    @Transactional(readOnly = true)
    public String status(String username) throws IOException {
        DriverApplication application = applications.find(username);
        return application == null ? NONE : application.status;
    }

    @Transactional(readOnly = true)
    public boolean approved(String username) throws IOException {
        return APPROVED.equals(status(username));
    }

    /// The status of every account that has an application, by name.
    @Transactional(readOnly = true)
    public Map<String, String> statuses() throws IOException {
        Map<String, String> out = new HashMap<String, String>();
        List<Object[]> rows = applications.statuses();
        for (int iter = 0; iter < rows.size(); iter++) {
            Object[] row = rows.get(iter);
            out.put((String) row[0], (String) row[1]);
        }
        return out;
    }

    /// An account's application, or one whose status is `none`.
    @Transactional(readOnly = true)
    public DriverApplicationDto get(String username) throws IOException {
        Profile profile = profiles.find(username);
        return toDto(username, profile == null ? "" : profile.displayName,
                applications.find(username), applications.documentSummaries(username), 0);
    }

    /// Begins an application as a draft, when the account has none. `vehicle`
    /// and `plate` are what an older app collected at registration.
    @Transactional
    public void open(String username, String vehicle, String plate) throws IOException {
        if (applications.find(username) == null) {
            applications.add(blank(username, DRAFT, vehicle, plate));
        }
    }

    /// Saves the form. Nothing has to be complete, but what is there has to be
    /// well formed. An application being reviewed or already approved is not
    /// the applicant's to change.
    ///
    /// A refusal does not undo the transaction: the draft a first visit begins
    /// is kept whether or not what came with it was well formed. Nothing else
    /// is written before the last check has passed.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public DriverApplicationDto save(String username, DriverApplicationDto form)
            throws IOException {
        if (form == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        DriverApplication application = editable(username);
        String birth = date(form.dateOfBirth, "your date of birth");
        String expiry = date(form.licenceExpiry, "the licence's expiry date");
        if (form.vehicleYear < 0 || form.vehicleYear > 2200 || form.vehicleSeats < 0
                || form.vehicleSeats > 20) {
            throw new ResponseStatusException(400, "Check the vehicle's year and seats");
        }
        // Every field is checked before the first is assigned: the application
        // is a managed entity, and a field assigned ahead of a refusal would be
        // written with the draft.
        String legalName = Text.optional(form.legalName, 120, "your name");
        String licenceNumber = Text.optional(form.licenceNumber, 40, "the licence number");
        String make = Text.optional(form.vehicleMake, 40, "the vehicle's make");
        String model = Text.optional(form.vehicleModel, 40, "the vehicle's model");
        String color = Text.optional(form.vehicleColor, 24, "the vehicle's colour");
        String plate = Text.optional(form.vehiclePlate, 32, "the vehicle's plate");
        String product = Fares.product(form.product);
        application.legalName = legalName;
        application.dateOfBirth = birth;
        application.licenceNumber = licenceNumber;
        application.licenceExpiry = expiry;
        application.vehicleMake = make;
        application.vehicleModel = model;
        application.vehicleYear = form.vehicleYear;
        application.vehicleColor = color;
        application.vehiclePlate = plate;
        application.vehicleSeats = form.vehicleSeats;
        application.product = product;
        application.wheelchair = form.accessible;
        application.pets = form.petFriendly;
        return get(username);
    }

    /// Stores a document, in place of an earlier one of its kind.
    ///
    /// What arrives is checked to be what it says: base64 that decodes, to no
    /// more than the cap, to bytes that begin as a JPEG or a PNG does. The
    /// declared type alone is the caller's word, and this is later sent to an
    /// admin's screen to be drawn.
    @Transactional
    public DriverApplicationDto upload(String username, DocumentUploadDto upload)
            throws IOException {
        if (upload == null) {
            throw new ResponseStatusException(400, "No document to store");
        }
        String kind = Text.oneOf(upload.kind, KINDS, "", "the kind of document");
        if (kind.length() == 0) {
            throw new ResponseStatusException(400, "Say which document this is");
        }
        String data = upload.dataBase64 == null ? "" : upload.dataBase64.trim();
        // Refused by length first, so an oversized upload is not decoded at all.
        if (data.length() == 0 || data.length() > (MAX_DOCUMENT_BYTES / 3 + 1) * 4) {
            throw new ResponseStatusException(data.length() == 0 ? 400 : 413,
                    data.length() == 0 ? "The document is empty" : "That picture is too large");
        }
        byte[] bytes = Base64.decode(data);
        if (bytes == null) {
            throw new ResponseStatusException(400, "The document is not base64");
        }
        if (bytes.length > MAX_DOCUMENT_BYTES) {
            throw new ResponseStatusException(413, "That picture is too large");
        }
        String type;
        if (bytes.length > 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
                && (bytes[2] & 0xff) == 0xff) {
            type = "image/jpeg";
        } else if (bytes.length > 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P'
                && bytes[2] == 'N' && bytes[3] == 'G') {
            type = "image/png";
        } else {
            throw new ResponseStatusException(400, "Send a JPEG or a PNG picture");
        }
        editable(username);
        long now = System.currentTimeMillis();
        // Replaced with a statement where there is one already, so the picture
        // it held is not read only to be overwritten.
        if (applications.replace(username, kind, type, "uploaded", now, data) == 0) {
            DriverDocument document = new DriverDocument();
            document.username = username;
            document.kind = kind;
            document.contentType = type;
            document.status = "uploaded";
            document.uploadedAt = now;
            document.data = data;
            applications.add(document);
        }
        return get(username);
    }

    /// A stored document, picture included. The caller decides who may ask:
    /// the applicant for their own, an admin for anyone's.
    @Transactional(readOnly = true)
    public DocumentContentDto document(String username, String kind) throws IOException {
        DriverDocument stored = kind == null || kind.length() == 0 || kind.length() > 24
                || username == null || username.length() == 0 ? null
                : applications.document(username, kind);
        if (stored == null) {
            throw new ResponseStatusException(404, "No such document");
        }
        DocumentContentDto dto = new DocumentContentDto();
        dto.kind = stored.kind;
        dto.contentType = stored.contentType;
        dto.dataBase64 = stored.data;
        return dto;
    }

    /// Hands the application in. Answers 400, naming what is missing, until it
    /// is complete. As in [#save], that refusal keeps the draft a first visit
    /// began.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public DriverApplicationDto submit(String username) throws IOException {
        String was = editable(username).status;
        DriverApplicationDto form = get(username);
        List<String> missing = new ArrayList<String>();
        String today = Days.iso(System.currentTimeMillis());
        need(missing, form.legalName.length() > 0, "your legal name");
        // Dates in this form compare as text. Eighteen years ago is today with
        // the year lowered.
        String adult = (Integer.parseInt(today.substring(0, 4)) - 18) + today.substring(4);
        need(missing, form.dateOfBirth.length() > 0 && form.dateOfBirth.compareTo(adult) <= 0,
                "a date of birth that makes you 18");
        need(missing, form.licenceNumber.length() > 0, "your licence number");
        need(missing, form.licenceExpiry.compareTo(today) >= 0, "a licence that has not expired");
        need(missing, form.vehicleMake.length() > 0 && form.vehicleModel.length() > 0,
                "the vehicle's make and model");
        need(missing, form.vehicleYear >= 1990, "the vehicle's year");
        need(missing, form.vehicleColor.length() > 0, "the vehicle's colour");
        need(missing, form.vehiclePlate.length() > 0, "the vehicle's plate");
        need(missing, form.vehicleSeats >= (Fares.XL.equals(form.product) ? 6 : 4),
                Fares.XL.equals(form.product) ? "six seats for XL" : "the number of seats");
        for (int iter = 0; iter < form.documents.size(); iter++) {
            need(missing, !"missing".equals(form.documents.get(iter).status), KIND_NAMES[iter]);
        }
        if (!missing.isEmpty()) {
            StringBuilder what = new StringBuilder("Still needed: ");
            for (int iter = 0; iter < missing.size(); iter++) {
                what.append(iter == 0 ? "" : ", ").append(missing.get(iter));
            }
            throw new ResponseStatusException(400, what.toString());
        }
        // Held to the status it was read in: handed in twice, it is handed in once.
        applications.submit(username, was, PENDING, System.currentTimeMillis());
        return get(username);
    }

    // ----------------------------------------------------------------- admin

    /// Applications, the longest waiting first among those waiting. `status`
    /// narrows them to one status; empty lists all of them.
    @Transactional(readOnly = true)
    public List<DriverApplicationDto> list(String status) throws IOException {
        String wanted = Text.oneOf(status, new String[] {DRAFT, PENDING, APPROVED, REJECTED}, "",
                "an application's status");
        List<DriverApplication> rows = wanted.length() == 0 ? applications.newest(LIST_LIMIT)
                : applications.inStatus(wanted, LIST_LIMIT);
        List<DriverApplicationDto> out = new ArrayList<DriverApplicationDto>();
        if (rows.isEmpty()) {
            return out;
        }
        List<String> owners = new ArrayList<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            owners.add(rows.get(iter).username);
        }
        // The applicants' names in one query, and the documents of all of them
        // in another, without the pictures.
        Map<String, String> names = new HashMap<String, String>();
        List<Object[]> named = applications.displayNames(owners);
        for (int iter = 0; iter < named.size(); iter++) {
            Object[] row = named.get(iter);
            names.put((String) row[0], (String) row[1]);
        }
        Map<String, List<Object[]>> documents = new HashMap<String, List<Object[]>>();
        List<Object[]> all = applications.documentSummaries(owners);
        for (int iter = 0; iter < all.size(); iter++) {
            Object[] row = all.get(iter);
            String owner = (String) row[0];
            List<Object[]> held = documents.get(owner);
            if (held == null) {
                held = new ArrayList<Object[]>();
                documents.put(owner, held);
            }
            held.add(row);
        }
        for (int iter = 0; iter < rows.size(); iter++) {
            DriverApplication row = rows.get(iter);
            String name = names.get(row.username);
            List<Object[]> held = documents.get(row.username);
            out.add(toDto(row.username, name == null ? "" : name, row,
                    held == null ? new ArrayList<Object[]>() : held, 1));
        }
        return out;
    }

    /// How many applications are waiting for an admin's answer.
    @Transactional(readOnly = true)
    public int waiting() throws IOException {
        return (int) applications.count(PENDING);
    }

    /// An application for an admin: 404 for an account that never began one.
    @Transactional(readOnly = true)
    public DriverApplicationDto review(String username) throws IOException {
        if (applications.find(username) == null) {
            throw new ResponseStatusException(404, "No such application");
        }
        return get(username);
    }

    /// Approves an application that was handed in: the account becomes a
    /// driver, with the car on the application as the car riders look for.
    @Transactional
    public DriverApplicationDto approve(String by, String username) throws IOException {
        decide(by, username, APPROVED, "");
        applications.markDocuments(username, "accepted");
        // Read after the statements above, which empty the session.
        DriverApplication application = applications.find(username);
        Profile profile = profiles.find(username);
        if (application.vehicleMake.length() > 0 && profile != null) {
            String car = (application.vehicleColor + " " + application.vehicleMake + " "
                    + application.vehicleModel).trim();
            profile.vehicle = car.length() > 120 ? car.substring(0, 120) : car;
            profile.plate = application.vehiclePlate;
        }
        role(username, true);
        moderation.record(username, by, Moderation.APPROVE, "");
        return get(username);
    }

    /// Rejects an application that was handed in, with the reason the
    /// applicant is shown.
    @Transactional
    public DriverApplicationDto reject(String by, String username, String reason)
            throws IOException {
        String why = Text.required(reason, 255, "why it is rejected");
        decide(by, username, REJECTED, why);
        moderation.record(username, by, Moderation.REJECT, why);
        return get(username);
    }

    /// Makes an account an approved driver without an application having been
    /// handed in: the admin's own switch, and the demo accounts. Whatever
    /// application the account has is marked approved, and one is made if it
    /// has none, so that "approved" has one meaning everywhere.
    @Transactional
    public void grant(String by, String username) throws IOException {
        if (applications.find(username) == null) {
            applications.add(blank(username, APPROVED, "", ""));
            return;
        }
        applications.approveUnlessApproved(username, APPROVED, System.currentTimeMillis(), by);
    }

    /// Takes the approval back, when an admin withdraws the DRIVER role. The
    /// application is left rejected, so it can be corrected and handed in again.
    @Transactional
    public void withdraw(String by, String username) throws IOException {
        applications.move(username, APPROVED, REJECTED, "Driving was withdrawn by an admin",
                System.currentTimeMillis(), by);
    }

    /// Everything kept about an account's application, for an account being
    /// closed.
    @Transactional
    public void forget(String username) throws IOException {
        applications.deleteDocuments(username);
        applications.delete(username);
    }

    // ------------------------------------------------------------- internals

    /// The one conditional update behind both answers: it changes an
    /// application only while it is waiting, so of two admins answering at
    /// once one decides and the other is told it has been decided.
    private void decide(String by, String username, String to, String reason)
            throws IOException {
        if (applications.find(username) == null) {
            throw new ResponseStatusException(404, "No such application");
        }
        if (applications.move(username, PENDING, to, reason, System.currentTimeMillis(), by)
                != 1) {
            throw new ResponseStatusException(409, "That application is not waiting for an answer");
        }
    }

    /// Grants or withdraws the DRIVER role, leaving the account's other roles
    /// as they are.
    private void role(String username, boolean driver) {
        if (!users.userExists(username)) {
            throw new ResponseStatusException(404, "No such account");
        }
        UserDetails signIn = users.loadUserByUsername(username);
        String wanted = "ROLE_" + Roles.DRIVER;
        List<String> held = new ArrayList<String>();
        Iterator<? extends GrantedAuthority> it = signIn.getAuthorities().iterator();
        while (it.hasNext()) {
            String authority = it.next().getAuthority();
            if (!wanted.equals(authority)) {
                held.add(authority);
            }
        }
        if (driver) {
            held.add(wanted);
        }
        users.updateUser(User.withUserDetails(signIn)
                .authorities(held.toArray(new String[held.size()])).build());
    }

    /// The application, made as a draft if there is none, when the applicant
    /// may still change it; 409 otherwise.
    private DriverApplication editable(String username) {
        DriverApplication application = applications.find(username);
        if (application == null) {
            application = blank(username, DRAFT, "", "");
            applications.add(application);
        }
        if (PENDING.equals(application.status)) {
            throw new ResponseStatusException(409, "Your application is being reviewed");
        }
        if (APPROVED.equals(application.status)) {
            throw new ResponseStatusException(409, "Your application was approved");
        }
        return application;
    }

    /// A new application with nothing filled in but what an older app
    /// collected at registration. One made already approved was reviewed the
    /// moment it was made.
    private static DriverApplication blank(String username, String status, String vehicle,
            String plate) {
        long now = System.currentTimeMillis();
        DriverApplication application = new DriverApplication();
        application.username = username;
        application.status = status;
        application.vehicleModel = vehicle.length() > 40 ? vehicle.substring(0, 40) : vehicle;
        application.vehiclePlate = plate;
        application.vehicleSeats = 4;
        application.product = Fares.STANDARD;
        application.createdAt = now;
        application.reviewedAt = APPROVED.equals(status) ? now : 0L;
        return application;
    }

    private static void need(List<String> missing, boolean present, String what) {
        if (!present) {
            missing.add(what);
        }
    }

    /// A date field: empty, or `yyyy-MM-dd`.
    private static String date(String value, String what) {
        String text = value == null ? "" : value.trim();
        if (text.length() > 0 && !Days.isIso(text)) {
            throw new ResponseStatusException(400, "Write " + what + " as yyyy-MM-dd");
        }
        return text;
    }

    /// `documents` holds `{kind, status, uploadedAt}` from the index `from` on:
    /// a list read for several accounts has the owner's name in front.
    private static DriverApplicationDto toDto(String username, String name,
            DriverApplication row, List<Object[]> documents, int from) {
        DriverApplicationDto dto = new DriverApplicationDto();
        dto.username = username;
        dto.name = name;
        dto.status = row == null ? NONE : row.status;
        dto.legalName = row == null ? "" : row.legalName;
        dto.dateOfBirth = row == null ? "" : row.dateOfBirth;
        dto.licenceNumber = row == null ? "" : row.licenceNumber;
        dto.licenceExpiry = row == null ? "" : row.licenceExpiry;
        dto.vehicleMake = row == null ? "" : row.vehicleMake;
        dto.vehicleModel = row == null ? "" : row.vehicleModel;
        dto.vehicleYear = row == null ? 0 : row.vehicleYear;
        dto.vehicleColor = row == null ? "" : row.vehicleColor;
        dto.vehiclePlate = row == null ? "" : row.vehiclePlate;
        dto.vehicleSeats = row == null ? 4 : row.vehicleSeats;
        dto.product = row == null ? Fares.STANDARD : row.product;
        dto.accessible = row != null && row.wheelchair;
        dto.petFriendly = row != null && row.pets;
        dto.rejectionReason = row == null ? "" : row.rejectionReason;
        dto.submittedAt = row == null ? 0L : row.submittedAt;
        dto.reviewedAt = row == null ? 0L : row.reviewedAt;
        dto.reviewedBy = row == null ? "" : row.reviewedBy;
        dto.documents = new ArrayList<DocumentDto>();
        for (int kind = 0; kind < KINDS.length; kind++) {
            DocumentDto document = new DocumentDto();
            document.kind = KINDS[kind];
            document.status = "missing";
            for (int iter = 0; iter < documents.size(); iter++) {
                Object[] stored = documents.get(iter);
                if (KINDS[kind].equals(stored[from])) {
                    document.status = (String) stored[from + 1];
                    Object at = stored[from + 2];
                    document.uploadedAt = at instanceof Number ? ((Number) at).longValue() : 0L;
                }
            }
            dto.documents.add(document);
        }
        return dto;
    }
}
