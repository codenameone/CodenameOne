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
package com.codenameone.examples.savor.onboarding;

import com.codename1.backend.Base64;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.orm.repository.PageRequest;
import com.codename1.orm.session.Identifier;
import com.codenameone.examples.savor.Days;
import com.codenameone.examples.savor.Roles;
import com.codenameone.examples.savor.Rows;
import com.codenameone.examples.savor.Text;
import com.codenameone.examples.savor.account.Moderation;
import com.codenameone.examples.savor.api.DocumentContentDto;
import com.codenameone.examples.savor.api.DocumentDto;
import com.codenameone.examples.savor.api.DocumentUploadDto;
import com.codenameone.examples.savor.api.CourierApplicationDto;
import com.codenameone.examples.savor.domain.CourierApplication;
import com.codenameone.examples.savor.domain.CourierDocument;
import com.codenameone.examples.savor.domain.Profile;
import com.codenameone.examples.savor.repository.CourierApplicationRepository;
import com.codenameone.examples.savor.repository.CourierDocumentRepository;
import com.codenameone.examples.savor.repository.ProfileRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// Applications to deliver, from the first field filled in to an admin's answer.
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
/// grants the COURIER role, or rejects it with a reason the applicant reads
/// before correcting it and handing it in again.
///
/// `approved` is what the rest of the server reads: going on line and being
/// offered deliveries both ask for it here, and not for the role alone.
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

    /// The most pixels a document may have on a side, read from its header.
    public static final int MAX_DOCUMENT_SIDE = 4096;

    private static final String[] KIND_NAMES = {"a photo of your ID", "a photo of your licence",
        "a selfie", "the vehicle's registration", "proof of insurance"};

    private static final int LIST_LIMIT = 200;

    private final CourierApplicationRepository applications;
    private final CourierDocumentRepository documents;
    private final ProfileRepository profiles;
    private final JdbcUserDetailsManager users;
    private final Moderation moderation;

    public Applications(CourierApplicationRepository applications,
            CourierDocumentRepository documents, ProfileRepository profiles,
            JdbcUserDetailsManager users, Moderation moderation) {
        this.applications = applications;
        this.documents = documents;
        this.profiles = profiles;
        this.users = users;
        this.moderation = moderation;
    }

    // ------------------------------------------------------------- applicant

    /// Where an account stands: one of the five statuses.
    @Transactional(readOnly = true)
    public String status(String username) throws IOException {
        CourierApplication application = find(username);
        return application == null ? NONE : application.getStatus();
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
    public CourierApplicationDto get(String username) throws IOException {
        Profile profile = profiles.findById(username);
        return toDto(username, profile == null ? "" : profile.getDisplayName(),
                find(username), documents.summaries(username), 0);
    }

    /// Begins an application as a draft, when the account has none. `vehicle`
    /// and `plate` are what an older app collected at registration.
    @Transactional
    public void open(String username, String vehicle, String plate) throws IOException {
        if (find(username) == null) {
            applications.insert(blank(username, DRAFT, vehicle, plate));
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
    public CourierApplicationDto save(String username, CourierApplicationDto form)
            throws IOException {
        if (form == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        // Held first, as an upload holds it: what is saved here must not land
        // on an application that was handed in after it was read.
        hold(username);
        CourierApplication application = editable(username);
        String birth = date(form.dateOfBirth(), "your date of birth");
        String expiry = date(form.licenceExpiry(), "the licence's expiry date");
        if (form.vehicleYear() < 0 || form.vehicleYear() > 2200) {
            throw new ResponseStatusException(400, "Check the vehicle's year");
        }
        // Every field is checked before the first is assigned: the application
        // is a managed entity, and a field assigned ahead of a refusal would be
        // written with the draft.
        String legalName = Text.optional(form.legalName(), 120, "your name");
        String licenceNumber = Text.optional(form.licenceNumber(), 40, "the licence number");
        String make = Text.optional(form.vehicleMake(), 40, "the vehicle's make");
        String model = Text.optional(form.vehicleModel(), 40, "the vehicle's model");
        String color = Text.optional(form.vehicleColor(), 24, "the vehicle's colour");
        String plate = Text.optional(form.vehiclePlate(), 32, "the vehicle's plate");
        application.setLegalName(legalName);
        application.setDateOfBirth(birth);
        application.setLicenceNumber(licenceNumber);
        application.setLicenceExpiry(expiry);
        application.setVehicleMake(make);
        application.setVehicleModel(model);
        application.setVehicleYear(form.vehicleYear());
        application.setVehicleColor(color);
        application.setVehiclePlate(plate);
        return get(username);
    }

    /// Stores a document, in place of an earlier one of its kind.
    ///
    /// What arrives is checked to be what it says: base64 that decodes, to no
    /// more than the cap, to bytes that begin as a JPEG or a PNG does. The
    /// declared type alone is the caller's word, and this is later sent to an
    /// admin's screen to be drawn.
    @Transactional
    public CourierApplicationDto upload(String username, DocumentUploadDto upload)
            throws IOException {
        if (upload == null) {
            throw new ResponseStatusException(400, "No document to store");
        }
        String kind = Text.oneOf(upload.kind(), KINDS, "", "the kind of document");
        if (kind.length() == 0) {
            throw new ResponseStatusException(400, "Say which document this is");
        }
        String data = upload.dataBase64() == null ? "" : upload.dataBase64().trim();
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
        // The size the picture says it has, before anyone decodes it. A few
        // kilobytes can declare a picture of millions of pixels, and an admin's
        // app decoding that runs out of memory: the app sends at most 1280 on
        // a side, so anything far past that is refused.
        checkSize(bytes, type, "Send a JPEG or a PNG picture");
        // Held before it is read. The check below says the application may
        // still be changed, and the picture is stored after it: without the
        // hold, an application handed in between the two had a document
        // replaced while it was under review, perhaps after an admin had
        // looked at the one it replaced.
        hold(username);
        editable(username);
        long now = System.currentTimeMillis();
        // Replaced with a statement where there is one already, so the picture
        // it held is not read only to be overwritten.
        if (documents.replace(username, kind, type, "uploaded", Instant.ofEpochMilli(now), data) == 0) {
            CourierDocument document = new CourierDocument();
            document.setUsername(username);
            document.setKind(kind);
            document.setContentType(type);
            document.setStatus("uploaded");
            document.setUploadedAt(Instant.ofEpochMilli(now));
            document.setData(data);
            documents.insert(document);
        }
        return get(username);
    }

    /// A stored document, picture included. The caller decides who may ask:
    /// the applicant for their own, an admin for anyone's.
    @Transactional(readOnly = true)
    public DocumentContentDto document(String username, String kind) throws IOException {
        CourierDocument stored = kind == null || kind.length() == 0 || kind.length() > 24
                || username == null || username.length() == 0 ? null
                : documents.findById(Identifier.of(username, kind));
        if (stored == null) {
            throw new ResponseStatusException(404, "No such document");
        }
        return new DocumentContentDto(stored.getKind(), stored.getContentType(),
                stored.getData());
    }

    /// Hands the application in. Answers 400, naming what is missing, until it
    /// is complete. As in [#save], that refusal keeps the draft a first visit
    /// began.
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public CourierApplicationDto submit(String username) throws IOException {
        // Held before the form is read and checked: a save that lands between
        // the check and the move below would otherwise hand in an application
        // emptied after it was found complete, where it can no longer be
        // corrected. Behind the hold a save either went first, and is what is
        // checked, or comes after and finds it handed in.
        hold(username);
        String was = editable(username).getStatus();
        CourierApplicationDto form = get(username);
        List<String> missing = new ArrayList<String>();
        String today = Days.iso(System.currentTimeMillis());
        need(missing, form.legalName().length() > 0, "your legal name");
        // Dates in this form compare as text. Eighteen years ago is today with
        // the year lowered.
        String adult = (Integer.parseInt(today.substring(0, 4)) - 18) + today.substring(4);
        need(missing, form.dateOfBirth().length() > 0 && form.dateOfBirth().compareTo(adult) <= 0,
                "a date of birth that makes you 18");
        need(missing, form.licenceNumber().length() > 0, "your licence number");
        need(missing, form.licenceExpiry().compareTo(today) >= 0, "a licence that has not expired");
        need(missing, form.vehicleMake().length() > 0 && form.vehicleModel().length() > 0,
                "the vehicle's make and model");
        need(missing, form.vehicleYear() >= 1990, "the vehicle's year");
        need(missing, form.vehicleColor().length() > 0, "the vehicle's colour");
        need(missing, form.vehiclePlate().length() > 0, "the vehicle's plate");
        for (int iter = 0; iter < form.documents().size(); iter++) {
            need(missing, !"missing".equals(form.documents().get(iter).status()), KIND_NAMES[iter]);
        }
        if (!missing.isEmpty()) {
            StringBuilder what = new StringBuilder("Still needed: ");
            for (int iter = 0; iter < missing.size(); iter++) {
                what.append(iter == 0 ? "" : ", ").append(missing.get(iter));
            }
            throw new ResponseStatusException(400, what.toString());
        }
        // Held to the status it was read in: handed in twice, it is handed in once.
        applications.submit(username, was, PENDING, Instant.now());
        return get(username);
    }

    // ----------------------------------------------------------------- admin

    /// Applications, the longest waiting first among those waiting. `status`
    /// narrows them to one status; empty lists all of them.
    @Transactional(readOnly = true)
    public List<CourierApplicationDto> list(String status) throws IOException {
        String wanted = Text.oneOf(status, new String[] {DRAFT, PENDING, APPROVED, REJECTED}, "",
                "an application's status");
        PageRequest page = new PageRequest(0, LIST_LIMIT);
        List<CourierApplication> rows = wanted.length() == 0 ? applications.newest(page)
                : applications.inStatus(wanted, page);
        List<CourierApplicationDto> out = new ArrayList<CourierApplicationDto>();
        if (rows.isEmpty()) {
            return out;
        }
        List<String> owners = new ArrayList<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            owners.add(rows.get(iter).getUsername());
        }
        // The applicants' names in one query, and the documents of all of them
        // in another, without the pictures.
        Map<String, String> names = new HashMap<String, String>();
        List<Object[]> named = profiles.displayNames(owners);
        for (int iter = 0; iter < named.size(); iter++) {
            Object[] row = named.get(iter);
            names.put((String) row[0], (String) row[1]);
        }
        Map<String, List<Object[]>> byOwner = new HashMap<String, List<Object[]>>();
        List<Object[]> all = documents.summaries(owners);
        for (int iter = 0; iter < all.size(); iter++) {
            Object[] row = all.get(iter);
            String owner = (String) row[0];
            List<Object[]> held = byOwner.get(owner);
            if (held == null) {
                held = new ArrayList<Object[]>();
                byOwner.put(owner, held);
            }
            held.add(row);
        }
        for (int iter = 0; iter < rows.size(); iter++) {
            CourierApplication row = rows.get(iter);
            String name = names.get(row.getUsername());
            List<Object[]> held = byOwner.get(row.getUsername());
            out.add(toDto(row.getUsername(), name == null ? "" : name, row,
                    held == null ? new ArrayList<Object[]>() : held, 1));
        }
        return out;
    }

    /// How many applications are waiting for an admin's answer.
    @Transactional(readOnly = true)
    public int waiting() throws IOException {
        return (int) applications.countByStatus(PENDING);
    }

    /// An application for an admin: 404 for an account that never began one.
    @Transactional(readOnly = true)
    public CourierApplicationDto review(String username) throws IOException {
        if (find(username) == null) {
            throw new ResponseStatusException(404, "No such application");
        }
        return get(username);
    }

    /// Approves an application that was handed in: the account becomes a
    /// courier, with the car on the application as the car customers look for.
    @Transactional
    public CourierApplicationDto approve(String by, String username) throws IOException {
        // The account first, the lock blocking an account takes too: role()
        // rewrites the whole user row, its enabled flag with it, so an approval
        // that read the account before a block and wrote it after would undo
        // the block. Behind the hold it reads what the block left.
        profiles.hold(username);
        decide(by, username, APPROVED, "");
        documents.markAll(username, "accepted");
        // Read after markAll, which empties the session.
        CourierApplication application = find(username);
        Profile profile = profiles.findById(username);
        if (application.getVehicleMake().length() > 0 && profile != null) {
            String car = (application.getVehicleColor() + " " + application.getVehicleMake() + " "
                    + application.getVehicleModel()).trim();
            profile.setVehicle(car.length() > 120 ? car.substring(0, 120) : car);
            profile.setPlate(application.getVehiclePlate());
        }
        role(username, true);
        moderation.record(username, by, Moderation.APPROVE, "");
        return get(username);
    }

    /// Rejects an application that was handed in, with the reason the
    /// applicant is shown.
    @Transactional
    public CourierApplicationDto reject(String by, String username, String reason)
            throws IOException {
        String why = Text.required(reason, 255, "why it is rejected");
        decide(by, username, REJECTED, why);
        moderation.record(username, by, Moderation.REJECT, why);
        return get(username);
    }

    /// Makes an account an approved courier without an application having been
    /// handed in: the admin's own switch, and the demo accounts. Whatever
    /// application the account has is marked approved, and one is made if it
    /// has none, so that "approved" has one meaning everywhere.
    @Transactional
    public void grant(String by, String username) throws IOException {
        if (find(username) == null) {
            applications.insert(blank(username, APPROVED, "", ""));
            return;
        }
        applications.approveUnlessApproved(username, APPROVED, Instant.now(), by);
    }

    /// Takes the approval back, when an admin withdraws the COURIER role. The
    /// application is left rejected, so it can be corrected and handed in again.
    @Transactional
    public void withdraw(String by, String username) throws IOException {
        applications.move(username, APPROVED, REJECTED, "Onboarding was withdrawn by an admin",
                Instant.now(), by);
    }

    /// Everything kept about an account's application, for an account being
    /// closed.
    @Transactional
    public void forget(String username) throws IOException {
        documents.deleteByUsername(username);
        applications.deleteByUsername(username);
    }

    // ------------------------------------------------------------- internals

    /// The one conditional update behind both answers: it changes an
    /// application only while it is waiting, so of two admins answering at
    /// once one decides and the other is told it has been decided.
    private void decide(String by, String username, String to, String reason)
            throws IOException {
        if (find(username) == null) {
            throw new ResponseStatusException(404, "No such application");
        }
        if (applications.move(username, PENDING, to, reason, Instant.now(), by)
                != 1) {
            throw new ResponseStatusException(409, "That application is not waiting for an answer");
        }
    }

    /// Grants or withdraws the COURIER role, leaving the account's other roles
    /// as they are.
    private void role(String username, boolean courier) {
        if (!users.userExists(username)) {
            throw new ResponseStatusException(404, "No such account");
        }
        UserDetails signIn = users.loadUserByUsername(username);
        String wanted = "ROLE_" + Roles.COURIER;
        List<String> held = new ArrayList<String>();
        Iterator<? extends GrantedAuthority> it = signIn.getAuthorities().iterator();
        while (it.hasNext()) {
            String authority = it.next().getAuthority();
            if (!wanted.equals(authority)) {
                held.add(authority);
            }
        }
        if (courier) {
            held.add(wanted);
        }
        users.updateUser(User.withUserDetails(signIn)
                .authorities(held.toArray(new String[held.size()])).build());
    }

    /// The application, made as a draft if there is none, when the applicant
    /// may still change it; 409 otherwise.
    /// Takes turns with every other change to an account's application. One
    /// that has none yet has no row to hold: the first save, upload or
    /// hand-in makes it, and two at once would both insert one under the same
    /// name, so they take turns on the profile instead.
    ///
    /// No profile to hold means Accounts.erase committed first, after
    /// forget() had already deleted the application and its documents: going
    /// on would make a new draft and store identity documents for an account
    /// that no longer exists, which nothing would ever delete. 404 instead, as
    /// PushDevices.register does.
    private void hold(String username) {
        if (applications.hold(username) == 0 && profiles.hold(username) == 0) {
            throw new ResponseStatusException(404, "No such account");
        }
    }

    /// An account's application, or null when it has none.
    private CourierApplication find(String username) {
        if (username == null || username.length() == 0 || username.length() > 190) {
            return null;
        }
        return applications.findById(username);
    }

    private CourierApplication editable(String username) {
        CourierApplication application = find(username);
        if (application == null) {
            application = blank(username, DRAFT, "", "");
            applications.insert(application);
        }
        if (PENDING.equals(application.getStatus())) {
            throw new ResponseStatusException(409, "Your application is being reviewed");
        }
        if (APPROVED.equals(application.getStatus())) {
            throw new ResponseStatusException(409, "Your application was approved");
        }
        return application;
    }

    /// A new application with nothing filled in but what an older app
    /// collected at registration. One made already approved was reviewed the
    /// moment it was made.
    private static CourierApplication blank(String username, String status, String vehicle,
            String plate) {
        long now = System.currentTimeMillis();
        CourierApplication application = new CourierApplication();
        application.setUsername(username);
        application.setStatus(status);
        application.setVehicleModel(vehicle.length() > 40 ? vehicle.substring(0, 40) : vehicle);
        application.setVehiclePlate(plate);
        application.setCreatedAt(Instant.ofEpochMilli(now));
        application.setReviewedAt(APPROVED.equals(status) ? Instant.ofEpochMilli(now) : Instant.EPOCH);
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
    private static CourierApplicationDto toDto(String username, String name,
            CourierApplication row, List<Object[]> documents, int from) {
        List<DocumentDto> listed = new ArrayList<DocumentDto>();
        for (int kind = 0; kind < KINDS.length; kind++) {
            String status = "missing";
            long uploadedAt = 0L;
            for (int iter = 0; iter < documents.size(); iter++) {
                Object[] stored = documents.get(iter);
                if (KINDS[kind].equals(stored[from])) {
                    status = (String) stored[from + 1];
                    uploadedAt = Rows.millis(stored[from + 2]);
                }
            }
            listed.add(new DocumentDto(KINDS[kind], status, uploadedAt));
        }
        if (row == null) {
            // Nobody has applied: an empty form, on the defaults it opens with.
            return new CourierApplicationDto(username, name, NONE, "", "", "", "", "", "", 0, "",
                    "", listed, "", 0L, 0L, "");
        }
        return new CourierApplicationDto(username, name, row.getStatus(), row.getLegalName(),
                row.getDateOfBirth(), row.getLicenceNumber(), row.getLicenceExpiry(),
                row.getVehicleMake(), row.getVehicleModel(), row.getVehicleYear(),
                row.getVehicleColor(), row.getVehiclePlate(), listed,
                row.getRejectionReason(), row.getSubmittedAt().toEpochMilli(), row.getReviewedAt().toEpochMilli(),
                row.getReviewedBy());
    }

    /// Refuses a picture whose header declares no size, or more than
    /// [#MAX_DOCUMENT_SIDE] pixels on a side, before anyone decodes it.
    /// `type` is `image/png` or `image/jpeg`; `unreadable` is the message for
    /// a header that cannot be read.
    public static void checkSize(byte[] bytes, String type, String unreadable) {
        int[] size = "image/png".equals(type) ? pngSize(bytes) : jpegSize(bytes);
        if (size == null || size[0] <= 0 || size[1] <= 0) {
            throw new ResponseStatusException(400, unreadable);
        }
        if (size[0] > MAX_DOCUMENT_SIDE || size[1] > MAX_DOCUMENT_SIDE) {
            throw new ResponseStatusException(413, "That picture is too large");
        }
    }

    /// Width and height from a PNG's header chunk, or null without one.
    static int[] pngSize(byte[] bytes) {
        if (bytes.length < 24 || bytes[12] != 'I' || bytes[13] != 'H' || bytes[14] != 'D'
                || bytes[15] != 'R') {
            return null;
        }
        return new int[] {int32(bytes, 16), int32(bytes, 20)};
    }

    /// Width and height from a JPEG's frame header, or null when the scan
    /// starts, or the bytes run out, before one.
    static int[] jpegSize(byte[] bytes) {
        int at = 2;
        while (at + 3 < bytes.length) {
            if ((bytes[at] & 0xff) != 0xff) {
                return null;
            }
            int marker = bytes[at + 1] & 0xff;
            if (marker == 0xff) {
                // Fill before a marker.
                at++;
                continue;
            }
            if (marker == 0x01 || (marker >= 0xd0 && marker <= 0xd8)) {
                // Markers that carry no length.
                at += 2;
                continue;
            }
            if (marker == 0xda || marker == 0xd9) {
                return null;
            }
            int length = ((bytes[at + 2] & 0xff) << 8) | (bytes[at + 3] & 0xff);
            boolean frame = marker >= 0xc0 && marker <= 0xcf && marker != 0xc4
                    && marker != 0xc8 && marker != 0xcc;
            if (frame) {
                if (at + 8 >= bytes.length) {
                    return null;
                }
                int height = ((bytes[at + 5] & 0xff) << 8) | (bytes[at + 6] & 0xff);
                int width = ((bytes[at + 7] & 0xff) << 8) | (bytes[at + 8] & 0xff);
                return new int[] {width, height};
            }
            if (length < 2) {
                return null;
            }
            at += 2 + length;
        }
        return null;
    }

    private static int int32(byte[] bytes, int at) {
        return ((bytes[at] & 0xff) << 24) | ((bytes[at + 1] & 0xff) << 16)
                | ((bytes[at + 2] & 0xff) << 8) | (bytes[at + 3] & 0xff);
    }
}
