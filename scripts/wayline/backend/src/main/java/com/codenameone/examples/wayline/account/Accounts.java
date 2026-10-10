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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.core.userdetails.UsernameNotFoundException;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codenameone.examples.wayline.Roles;
import com.codenameone.examples.wayline.Text;
import com.codenameone.examples.wayline.api.ProfileDto;
import com.codenameone.examples.wayline.api.RegisterDto;
import com.codenameone.examples.wayline.api.UserDto;
import com.codenameone.examples.wayline.domain.Profile;
import com.codenameone.examples.wayline.driving.Applications;
import com.codenameone.examples.wayline.live.LiveHub;
import com.codenameone.examples.wayline.pay.Payments;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// Accounts: opening one, describing one, and what an admin may change.
///
/// An account is two things kept under one name. Its sign-in -- password hash,
/// roles, whether it is enabled -- is the backend's own user store. Its profile
/// -- display name, phone, car -- is this application's table. The name is the
/// e-mail address, in lower case.
///
/// Driving is not something an account is opened with. Asking to drive opens
/// an application ([Applications]), and the DRIVER role arrives when an admin
/// approves it.
@Component
public class Accounts {
    public static final String[] GENDERS = {"unspecified", "female", "male", "nonbinary"};

    private final ProfileRepository profiles;
    private final AccountCleanupRepository cleanup;
    private final JdbcUserDetailsManager users;
    private final PasswordEncoder encoder;
    private final Applications applications;
    private final Moderation moderation;
    private final Payments payments;
    private final LiveHub live;

    public Accounts(ProfileRepository profiles, AccountCleanupRepository cleanup,
            JdbcUserDetailsManager users, PasswordEncoder encoder, Applications applications,
            Moderation moderation, Payments payments, LiveHub live) {
        this.profiles = profiles;
        this.cleanup = cleanup;
        this.users = users;
        this.encoder = encoder;
        this.applications = applications;
        this.moderation = moderation;
        this.payments = payments;
        this.live = live;
    }

    /// Opens an account. Answers 400 for a request that cannot be an account
    /// and 409 when the e-mail already has one.
    @Transactional(rollbackFor = IOException.class)
    public UserDto register(RegisterDto request) throws IOException {
        if (request == null) {
            throw new ResponseStatusException(400, "Nothing to register");
        }
        String username = email(request.email);
        password(request.password);
        String gender = Text.oneOf(request.gender, GENDERS, GENDERS[0], "gender");
        String displayName = required(request.displayName, 120, "your name");
        String phone = phone(request.phone);
        String vehicle = "";
        String plate = "";
        if (request.driver) {
            vehicle = required(request.vehicle, 120, "the car you drive");
            plate = required(request.plate, 32, "its plate");
        }
        if (users.userExists(username)) {
            throw new ResponseStatusException(409, "That e-mail already has an account");
        }
        // Every account opens as a rider. Asking to drive -- `driver` is how an
        // older app asks -- begins an application, and the role waits for an
        // admin to approve it.
        create(username, request.password, displayName, phone, false, vehicle, plate, false,
                false, gender, System.currentTimeMillis());
        if (request.driver || request.wantsToDrive) {
            applications.open(username, vehicle, plate);
        }
        return describe(username);
    }

    /// Creates the sign-in and the profile. The caller has checked the name is
    /// free. An account created as a driver is an approved one: this is how the
    /// demo accounts are made, not how anyone signs up.
    @Transactional(rollbackFor = IOException.class)
    void create(String username, String password, String displayName, String phone,
            boolean phoneVerified, String vehicle, String plate, boolean driver, boolean admin)
            throws IOException {
        create(username, password, displayName, phone, phoneVerified, vehicle, plate, driver,
                admin, GENDERS[0], System.currentTimeMillis());
    }

    /// As the method above, for a caller that also knows the gender and when
    /// the account was opened; the demo data is the one caller with a past to
    /// write. The profile is stored whole, in one go, and not corrected
    /// afterwards.
    @Transactional(rollbackFor = IOException.class)
    void create(String username, String password, String displayName, String phone,
            boolean phoneVerified, String vehicle, String plate, boolean driver, boolean admin,
            String gender, long openedAt) throws IOException {
        users.createUser(User.withUsername(username).password(encoder.encode(password))
                .roles(roles(driver, admin)).build());
        Profile profile = new Profile();
        profile.username = username;
        profile.displayName = displayName;
        profile.phone = phone;
        profile.phoneVerified = phoneVerified;
        profile.vehicle = vehicle;
        profile.plate = plate;
        profile.createdAt = openedAt;
        profile.gender = gender;
        profiles.add(profile);
        if (driver) {
            applications.grant(username, username);
        }
    }

    public boolean exists(String username) {
        return users.userExists(username);
    }

    /// The account as the app sees it. Answers 404 for a name with no account.
    @Transactional(readOnly = true)
    public UserDto describe(String username) throws IOException {
        Profile profile = profiles.find(username);
        if (profile == null || !users.userExists(username)) {
            throw new ResponseStatusException(404, "No such account");
        }
        UserDetails signIn = users.loadUserByUsername(username);
        return toDto(profile, authorities(signIn), signIn.isEnabled(),
                applications.status(username));
    }

    /// Every account, newest first, for the admin.
    @Transactional(readOnly = true)
    public List<UserDto> all() throws IOException {
        Map<String, String> driving = applications.statuses();
        List<UserDto> out = new ArrayList<UserDto>();
        List<Profile> everyone = profiles.all();
        for (int iter = 0; iter < everyone.size(); iter++) {
            Profile profile = everyone.get(iter);
            // One sign-in read per account. The user store answers for one name
            // at a time and has no way to list its users or to load several at
            // once, and its tables are its own, not this application's to
            // query. That is two statements an account, which the admin's list
            // can afford; a store that could load them together would be asked
            // once.
            List<String> held = new ArrayList<String>();
            boolean enabled = true;
            try {
                UserDetails signIn = users.loadUserByUsername(profile.username);
                held = authorities(signIn);
                enabled = signIn.isEnabled();
            } catch (UsernameNotFoundException gone) {
                // A profile whose sign-in was deleted under it is listed with
                // no roles rather than failing the whole list.
                enabled = true;
            }
            String status = driving.get(profile.username);
            out.add(toDto(profile, held, enabled, status == null ? Applications.NONE : status));
        }
        return out;
    }

    /// Grants or withdraws driving and administering. `by` is the admin doing
    /// it, who cannot withdraw their own admin role: that is how a service ends
    /// up with nobody able to administer it.
    @Transactional(rollbackFor = IOException.class)
    public UserDto setRoles(String by, String username, boolean driver, boolean admin)
            throws IOException {
        UserDetails signIn = load(username);
        if (!admin && username.equals(by)) {
            throw new ResponseStatusException(400, "You cannot withdraw your own admin role");
        }
        users.updateUser(User.withUserDetails(signIn).roles(roles(driver, admin)).build());
        // The role and the approval go together, whichever way it was given:
        // being approved is what lets a driver go on line.
        if (driver) {
            applications.grant(by, username);
        } else {
            applications.withdraw(by, username);
            cleanup.takeOffLine(username);
        }
        moderation.record(username, by, Moderation.ROLES,
                (driver ? "driver" : "rider") + (admin ? ", admin" : ""));
        return describe(username);
    }

    /// Blocks an account or lifts the block. A blocked account cannot sign in,
    /// and a token it already holds stops working at once, because the API
    /// reads the account on every request.
    @Transactional(rollbackFor = IOException.class)
    public UserDto suspend(String by, String username, boolean suspended, String reason)
            throws IOException {
        UserDetails signIn = load(username);
        if (suspended && username.equals(by)) {
            throw new ResponseStatusException(400, "You cannot suspend your own account");
        }
        String why = Text.optional(reason, 255, "the reason");
        users.updateUser(User.withUserDetails(signIn).disabled(suspended).build());
        if (suspended) {
            cleanup.takeOffLine(username);
        }
        if (suspended == signIn.isEnabled()) {
            // Only a change is an event; saying the same thing twice is not.
            moderation.record(username, by, suspended ? Moderation.BLOCK : Moderation.UNBLOCK, why);
        }
        return describe(username);
    }

    /// Marks an account for the admins' attention, or clears the mark. A flag
    /// changes nothing the account can do; blocking is what does that.
    @Transactional(rollbackFor = IOException.class)
    public UserDto flag(String by, String username, boolean flagged, String reason)
            throws IOException {
        load(username);
        String why = Text.optional(reason, 255, "the reason");
        if (flagged && why.length() == 0) {
            throw new ResponseStatusException(400, "Say why the account is flagged");
        }
        Profile profile = profiles.find(username);
        if (profile != null) {
            profile.flagged = flagged;
            profile.flagReason = flagged ? why : "";
        }
        moderation.record(username, by, flagged ? Moderation.FLAG : Moderation.UNFLAG, why);
        return describe(username);
    }

    // ------------------------------------------------------------ one's own

    @Transactional(readOnly = true)
    public ProfileDto profile(String username) throws IOException {
        Profile row = profiles.find(username);
        if (row == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        ProfileDto dto = new ProfileDto();
        dto.name = row.displayName;
        dto.gender = row.gender;
        dto.emergencyContactName = row.emergencyName;
        dto.emergencyContactPhone = row.emergencyPhone;
        dto.homeAddress = row.homeAddress;
        dto.workAddress = row.workAddress;
        return dto;
    }

    @Transactional(rollbackFor = IOException.class)
    public UserDto saveProfile(String username, ProfileDto profile) throws IOException {
        if (profile == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        String contact = profile.emergencyContactPhone == null ? ""
                : profile.emergencyContactPhone.trim();
        // Everything is checked before anything is assigned: the row is written
        // from what it holds when the transaction commits.
        String name = required(profile.name, 120, "your name");
        String gender = Text.oneOf(profile.gender, GENDERS, GENDERS[0], "gender");
        String emergencyName = Text.optional(profile.emergencyContactName, 120,
                "the contact's name");
        String emergencyPhone = contact.length() == 0 ? "" : phone(contact);
        String home = Text.optional(profile.homeAddress, 255, "an address");
        String work = Text.optional(profile.workAddress, 255, "an address");
        Profile row = profiles.find(username);
        if (row == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        row.displayName = name;
        row.gender = gender;
        row.emergencyName = emergencyName;
        row.emergencyPhone = emergencyPhone;
        row.homeAddress = home;
        row.workAddress = work;
        return describe(username);
    }

    /// Changes a password, for someone who knows the one it replaces. A token
    /// in hand is not enough: it is what a borrowed phone has.
    @Transactional(rollbackFor = IOException.class)
    public UserDto changePassword(String username, String current, String replacement)
            throws IOException {
        UserDetails signIn = load(username);
        if (current == null || !encoder.matches(current, signIn.getPassword())) {
            throw new ResponseStatusException(403, "That is not your current password");
        }
        password(replacement);
        users.updatePassword(signIn, encoder.encode(replacement));
        return describe(username);
    }

    /// Closes an account, for good: the sign-in, the profile, the saved cards
    /// and the driving application with its documents are deleted, not hidden.
    /// The rides it took stay, as the other party's history and the service's
    /// accounts, under a name that no longer has anyone behind it.
    ///
    /// Refused while a ride is under way, which has a driver or a rider waiting
    /// on it, and for an admin, so that a service is not left with none.
    public UserDto close(String username) throws IOException {
        UserDto was = erase(username);
        // After the commit and not inside it: what is said over a live
        // connection is acted on at once, and must not be said about a change
        // that could still be rolled back.
        live.drop(username);
        was.suspended = true;
        return was;
    }

    /// Everything closing an account deletes, as one transaction: either the
    /// account is gone from every table or it is in all of them still. The
    /// user store works on the same database and so takes part in it.
    @Transactional(rollbackFor = IOException.class)
    UserDto erase(String username) throws IOException {
        UserDto was = describe(username);
        if (was.admin) {
            throw new ResponseStatusException(409, "Hand the admin role on before closing");
        }
        if (cleanup.hasRideUnderWay(username)) {
            throw new ResponseStatusException(409, "Finish your ride before closing the account");
        }
        payments.forget(username);
        applications.forget(username);
        moderation.forget(username);
        cleanup.removeEverythingOf(username);
        // Read again: the statements above each cleared what the session had
        // loaded, the profile that was described among it.
        Profile profile = profiles.find(username);
        if (profile != null) {
            profiles.remove(profile);
        }
        // Last: the tokens in hand stop working when the sign-in is gone.
        users.deleteUser(username);
        return was;
    }

    @Transactional(rollbackFor = IOException.class)
    public void markPhoneVerified(String username) throws IOException {
        Profile profile = profiles.find(username);
        if (profile != null) {
            profile.phoneVerified = true;
        }
    }

    private UserDetails load(String username) {
        if (username == null || !users.userExists(username)) {
            throw new ResponseStatusException(404, "No such account");
        }
        return users.loadUserByUsername(username);
    }

    private static String[] roles(boolean driver, boolean admin) {
        List<String> roles = new ArrayList<String>();
        roles.add(Roles.RIDER);
        if (driver) {
            roles.add(Roles.DRIVER);
        }
        if (admin) {
            roles.add(Roles.ADMIN);
        }
        return roles.toArray(new String[roles.size()]);
    }

    private static void password(String value) {
        if (value == null || value.length() < 8) {
            throw new ResponseStatusException(400, "Choose a password of at least 8 characters");
        }
        if (value.length() > 200) {
            throw new ResponseStatusException(400, "That password is too long");
        }
    }

    private static List<String> authorities(UserDetails signIn) {
        List<String> held = new ArrayList<String>();
        Iterator<? extends GrantedAuthority> it = signIn.getAuthorities().iterator();
        while (it.hasNext()) {
            held.add(it.next().getAuthority());
        }
        return held;
    }

    private static UserDto toDto(Profile profile, List<String> authorities, boolean enabled,
            String driverStatus) {
        UserDto dto = new UserDto();
        dto.username = profile.username;
        dto.displayName = profile.displayName;
        dto.phone = profile.phone;
        dto.phoneVerified = profile.phoneVerified;
        dto.vehicle = profile.vehicle;
        dto.plate = profile.plate;
        dto.rider = authorities.contains("ROLE_" + Roles.RIDER);
        dto.driver = authorities.contains("ROLE_" + Roles.DRIVER);
        dto.admin = authorities.contains("ROLE_" + Roles.ADMIN);
        dto.suspended = !enabled;
        dto.gender = profile.gender;
        dto.language = profile.language;
        dto.flagged = profile.flagged;
        dto.flagReason = profile.flagReason;
        dto.driverStatus = driverStatus;
        return dto;
    }

    /// An e-mail address as an account name: trimmed, and its ASCII letters in
    /// lower case. Folded by hand, because `toLowerCase()` follows the server's
    /// locale and a key must not: under a Turkish locale the `I` of `INFO@` would
    /// fold to a dotless i and name a different account.
    public static String email(String value) {
        String text = value == null ? "" : value.trim();
        int at = text.indexOf('@');
        if (at < 1 || at != text.lastIndexOf('@') || at > text.length() - 4
                || text.indexOf('.', at) < 0 || text.length() > 190) {
            throw new ResponseStatusException(400, "That is not an e-mail address");
        }
        char[] chars = text.toCharArray();
        for (int iter = 0; iter < chars.length; iter++) {
            char c = chars[iter];
            if (c <= ' ' || c > '~') {
                throw new ResponseStatusException(400, "That is not an e-mail address");
            }
            if (c >= 'A' && c <= 'Z') {
                chars[iter] = (char) (c + ('a' - 'A'));
            }
        }
        return new String(chars);
    }

    /// A phone number in the form it is sent to and stored in: a `+` and its
    /// digits, with the spaces, dashes and brackets people type taken out.
    static String phone(String value) {
        String text = value == null ? "" : value.trim();
        StringBuilder digits = new StringBuilder();
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            } else if (c != ' ' && c != '-' && c != '(' && c != ')' && c != '.'
                    && !(c == '+' && iter == 0)) {
                throw new ResponseStatusException(400, "That is not a phone number");
            }
        }
        if (digits.length() < 7 || digits.length() > 15) {
            throw new ResponseStatusException(400, "That is not a phone number");
        }
        return "+" + digits;
    }

    private static String required(String value, int max, String what) {
        String text = value == null ? "" : value.trim();
        if (text.length() == 0) {
            throw new ResponseStatusException(400, "Tell us " + what);
        }
        if (text.length() > max) {
            throw new ResponseStatusException(400, "That is too long for " + what);
        }
        return text;
    }
}
