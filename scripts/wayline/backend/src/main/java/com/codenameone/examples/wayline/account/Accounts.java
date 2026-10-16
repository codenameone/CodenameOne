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
import com.codenameone.examples.wayline.Ids;
import com.codenameone.examples.wayline.Roles;
import com.codenameone.examples.wayline.Text;
import com.codenameone.examples.wayline.api.ProfileDto;
import com.codenameone.examples.wayline.api.RegisterDto;
import com.codenameone.examples.wayline.api.UserDto;
import com.codenameone.examples.wayline.domain.ClosedAccount;
import com.codenameone.examples.wayline.domain.Profile;
import com.codenameone.examples.wayline.driving.Applications;
import com.codenameone.examples.wayline.live.LiveHub;
import com.codenameone.examples.wayline.pay.Payments;
import com.codenameone.examples.wayline.repository.ClosedAccountRepository;
import com.codenameone.examples.wayline.repository.ProfileRepository;

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
    private final AccountCleanup cleanup;
    private final JdbcUserDetailsManager users;
    private final PasswordEncoder encoder;
    private final Applications applications;
    private final Moderation moderation;
    private final Payments payments;
    private final LiveHub live;
    private final ClosedAccountRepository closed;
    private final Grants grants;

    public Accounts(ProfileRepository profiles, AccountCleanup cleanup,
            JdbcUserDetailsManager users, PasswordEncoder encoder, Applications applications,
            Moderation moderation, Payments payments, LiveHub live,
            ClosedAccountRepository closed, Grants grants) {
        this.closed = closed;
        this.grants = grants;
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
    ///
    /// Not a transaction itself, so that it can look again after one failed.
    /// Two requests for the same e-mail can both find it free; the name's
    /// unique key then refuses the second, and what the database says about
    /// that differs with the database. So nothing here reads the error: if the
    /// account exists once the transaction is over, that is why it failed, and
    /// the answer is the same 409 the check gives.
    public UserDto register(RegisterDto request) throws IOException {
        try {
            return open(request);
        } catch (ResponseStatusException answered) {
            throw answered;
        } catch (IOException failed) {
            throw taken(request, failed);
        } catch (RuntimeException failed) {
            throw taken(request, failed);
        }
    }

    /// The 409 for a registration that failed because the account was opened
    /// meanwhile, or the failure itself when it was something else.
    private <T extends Exception> T taken(RegisterDto request, T failed) {
        if (request != null && request.email() != null
                && users.userExists(email(request.email()))) {
            throw new ResponseStatusException(409, "That e-mail already has an account");
        }
        return failed;
    }

    @Transactional(rollbackFor = IOException.class)
    private UserDto open(RegisterDto request) throws IOException {
        if (request == null) {
            throw new ResponseStatusException(400, "Nothing to register");
        }
        String username = email(request.email());
        password(request.password());
        String gender = Text.oneOf(request.gender(), GENDERS, GENDERS[0], "gender");
        String displayName = required(request.displayName(), 120, "your name");
        String phone = phone(request.phone());
        String vehicle = "";
        String plate = "";
        if (request.driver()) {
            vehicle = required(request.vehicle(), 120, "the car you drive");
            plate = required(request.plate(), 32, "its plate");
        }
        if (users.userExists(username)) {
            throw new ResponseStatusException(409, "That e-mail already has an account");
        }
        // An address whose account was closed is not given to anyone again:
        // the rides that account took are still filed under it. See
        // [#retire].
        if (closed.existsById(Ids.sha256(username))) {
            throw new ResponseStatusException(409,
                    "That e-mail belonged to an account that was closed. Use another");
        }
        // Every account opens as a rider. Asking to drive -- `driver` is how an
        // older app asks -- begins an application, and the role waits for an
        // admin to approve it.
        create(username, request.password(), displayName, phone, false, vehicle, plate, false,
                false, gender, System.currentTimeMillis());
        if (request.driver() || request.wantsToDrive()) {
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
        profile.setUsername(username);
        profile.setDisplayName(displayName);
        profile.setPhone(phone);
        profile.setPhoneVerified(phoneVerified);
        profile.setVehicle(vehicle);
        profile.setPlate(plate);
        profile.setCreatedAt(openedAt);
        profile.setGender(gender);
        profiles.insert(profile);
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
        Profile profile = profiles.findById(username);
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
        List<Profile> everyone = profiles.newestFirst();
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
                UserDetails signIn = users.loadUserByUsername(profile.getUsername());
                held = authorities(signIn);
                enabled = signIn.isEnabled();
            } catch (UsernameNotFoundException gone) {
                // A profile whose sign-in was deleted under it is listed with
                // no roles rather than failing the whole list.
                enabled = true;
            }
            String status = driving.get(profile.getUsername());
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
        actingAdmin(by, username);
        UserDetails signIn = load(username);
        if (!admin && username.equals(by)) {
            throw new ResponseStatusException(400, "You cannot withdraw your own admin role");
        }
        // Not from under a rider. The role is read on every request, so a
        // driver who lost it mid-ride could no longer arrive, start, complete
        // or cancel, and the trip would sit there until somebody noticed. The
        // admin ends the ride first, or waits for it to end.
        if (!driver && cleanup.drivesRideUnderWay(username)) {
            throw new ResponseStatusException(409,
                    "That driver is on a ride. Withdraw the role when it has ended");
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
        actingAdmin(by, username);
        UserDetails signIn = load(username);
        if (suspended && username.equals(by)) {
            throw new ResponseStatusException(400, "You cannot suspend your own account");
        }
        String why = Text.optional(reason, 255, "the reason");
        // Not in the middle of a ride, for the reason a driver's role is not
        // withdrawn then: a blocked account's requests are all refused, so a
        // driver could no longer arrive, start, finish or cancel, and a rider
        // could not cancel, and the other person would be left with a ride
        // nobody can end. The admin cancels the ride first, or waits.
        if (suspended && signIn.isEnabled() && cleanup.inRideUnderWay(username)) {
            throw new ResponseStatusException(409,
                    "That account is on a ride. Block it when the ride has ended");
        }
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
        // The profile row first, as the other admin changes hold it: a flag
        // racing the account's closure either lands before erase() (which
        // then forgets it with the rest) or finds no account. Otherwise the
        // moderation row recorded below would outlive the closure's cleanup.
        if (profiles.hold(username) == 0) {
            throw new ResponseStatusException(404, "No such account");
        }
        load(username);
        String why = Text.optional(reason, 255, "the reason");
        if (flagged && why.length() == 0) {
            throw new ResponseStatusException(400, "Say why the account is flagged");
        }
        Profile profile = profiles.findById(username);
        if (profile != null) {
            profile.setFlagged(flagged);
            profile.setFlagReason(flagged ? why : "");
        }
        moderation.record(username, by, flagged ? Moderation.FLAG : Moderation.UNFLAG, why);
        return describe(username);
    }

    // ------------------------------------------------------------ one's own

    @Transactional(readOnly = true)
    public ProfileDto profile(String username) throws IOException {
        Profile row = profiles.findById(username);
        if (row == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        return new ProfileDto(row.getDisplayName(), row.getGender(), row.getEmergencyName(),
                row.getEmergencyPhone(), row.getHomeAddress(), row.getWorkAddress());
    }

    @Transactional(rollbackFor = IOException.class)
    public UserDto saveProfile(String username, ProfileDto profile) throws IOException {
        if (profile == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        String contact = profile.emergencyContactPhone() == null ? ""
                : profile.emergencyContactPhone().trim();
        // Everything is checked before anything is assigned: the row is written
        // from what it holds when the transaction commits.
        String name = required(profile.name(), 120, "your name");
        String gender = Text.oneOf(profile.gender(), GENDERS, GENDERS[0], "gender");
        String emergencyName = Text.optional(profile.emergencyContactName(), 120,
                "the contact's name");
        String emergencyPhone = contact.length() == 0 ? "" : phone(contact);
        String home = Text.optional(profile.homeAddress(), 255, "an address");
        String work = Text.optional(profile.workAddress(), 255, "an address");
        Profile row = profiles.findById(username);
        if (row == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        row.setDisplayName(name);
        row.setGender(gender);
        row.setEmergencyName(emergencyName);
        row.setEmergencyPhone(emergencyPhone);
        row.setHomeAddress(home);
        row.setWorkAddress(work);
        return describe(username);
    }

    /// Changes a password, for someone who knows the one it replaces. A token
    /// in hand is not enough: it is what a borrowed phone has. The account's
    /// other sign-ins end with the old password.
    @Transactional(rollbackFor = IOException.class)
    public UserDto changePassword(String username, String current, String replacement)
            throws IOException {
        // Before the password is read. Two changes at once would otherwise
        // both check the same old password and both succeed, the last one
        // winning: whoever else knows the old password could race the owner's
        // change and keep the account. Behind the hold the second change
        // checks against the password the first one set.
        profiles.hold(username);
        UserDetails signIn = load(username);
        if (current == null || !encoder.matches(current, signIn.getPassword())) {
            throw new ResponseStatusException(403, "That is not your current password");
        }
        password(replacement);
        users.updatePassword(signIn, encoder.encode(replacement));
        // A password is changed, as often as not, because someone else may
        // have the old one or a device that was signed in with it. A refresh
        // token asks for no password, so every sign-in but this one ends here.
        grants.endOthers(username);
        return describe(username);
    }

    /// Closes an account, for good: the sign-in, the profile, the saved cards
    /// and the driving application with its documents are deleted, not hidden.
    /// The rides it took stay, as the other party's history and the service's
    /// accounts, under a name that no longer has anyone behind it.
    ///
    /// Refused while a ride is under way, which has a driver or a rider waiting
    /// on it, and for an admin, so that a service is not left with none.
    ///
    /// `password` is the account's own, asked for again as a change of
    /// password asks for it. A token alone is not enough for something that
    /// cannot be undone: a phone left unlocked, or a token that leaked, would
    /// otherwise be all it takes to destroy an account.
    public UserDto close(String username, String password) throws IOException {
        UserDto was = erase(username, password);
        // After the commit and not inside it: what is said over a live
        // connection is acted on at once, and must not be said about a change
        // that could still be rolled back.
        live.drop(username);
        // The account as it was, answered as what it now is: unable to sign in.
        return new UserDto(was.username(), was.displayName(), was.phone(), was.phoneVerified(),
                was.rider(), was.driver(), was.admin(), true, was.vehicle(), was.plate(),
                was.gender(), was.driverStatus(), was.flagged(), was.flagReason(),
                was.language());
    }

    /// Everything closing an account deletes, as one transaction: either the
    /// account is gone from every table or it is in all of them still. The
    /// user store works on the same database and so takes part in it.
    @Transactional(rollbackFor = IOException.class)
    UserDto erase(String username, String password) throws IOException {
        // Before anything is read. Asking for a ride holds the same row, so
        // of a ride and a closure sent together one waits for the other: the
        // closure then sees the ride and is refused, or the ride finds no
        // profile. Without it both could pass, leaving a ride under way that
        // belongs to nobody who can cancel it.
        profiles.hold(username);
        UserDetails signIn = load(username);
        if (password == null || !encoder.matches(password, signIn.getPassword())) {
            throw new ResponseStatusException(403, "That is not your current password");
        }
        UserDto was = describe(username);
        if (was.admin()) {
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
        Profile profile = profiles.findById(username);
        if (profile != null) {
            profiles.deleteById(username);
        }
        // The address is retired with the account, in the same transaction:
        // nobody can register it and inherit the rides kept under it.
        retire(username);
        // Last: the tokens in hand stop working when the sign-in is gone.
        users.deleteUser(username);
        grants.endAll(username);
        return was;
    }

    /// Retires an e-mail for good, as only a hash of it.
    ///
    /// A ride outlives the account that took it, under the e-mail that account
    /// signed in with, and so does a token until it expires. Were the address
    /// free to register again, whoever took it would be handed the rides, the
    /// receipts and the ratings of the person who left, and that person's old
    /// token would sign in to the new account. Only a hash is kept: enough to
    /// refuse it, and not a list of who has left.
    private void retire(String username) throws IOException {
        String hash = Ids.sha256(username);
        if (!closed.existsById(hash)) {
            ClosedAccount row = new ClosedAccount();
            row.setUsernameHash(hash);
            row.setClosedAt(System.currentTimeMillis());
            closed.insert(row);
        }
    }

    /// Makes the rest of the caller's transaction take its turn with any other
    /// that holds the same account. See [ProfileRepository#hold]. Answers 404
    /// when the account is gone: it was closed while the request was under
    /// way, its clean-up has run, and a row the caller went on to insert would
    /// never be deleted.
    public void hold(String username) {
        if (profiles.hold(username) == 0) {
            throw new ResponseStatusException(404, "No such account");
        }
    }

    @Transactional(rollbackFor = IOException.class)
    public void markPhoneVerified(String username) throws IOException {
        Profile profile = profiles.findById(username);
        if (profile != null) {
            profile.setPhoneVerified(true);
        }
    }

    /// Makes a change one admin is making to an account take its turn with any
    /// other touching either of the two, and then checks `by` still is an
    /// admin. Call it before anything is read.
    ///
    /// Without it two admins could each take the other's role, or block each
    /// other, at the same moment. Each request was authorized before the other
    /// committed and neither touches its own account, so both would go through
    /// and the service would be left with nobody to administer it. Both
    /// profiles are held, in the order of their names so that the two requests
    /// queue and cannot deadlock; whichever comes second then reads what the
    /// first left, finds it is no longer an admin, and is refused.
    private void actingAdmin(String by, String username) {
        if (by == null || username == null) {
            return;
        }
        String first = by.compareTo(username) <= 0 ? by : username;
        String second = first.equals(by) ? username : by;
        profiles.hold(first);
        if (!second.equals(first)) {
            profiles.hold(second);
        }
        if (!users.userExists(by)) {
            throw new ResponseStatusException(403, "You are no longer an admin");
        }
        UserDetails acting = users.loadUserByUsername(by);
        if (!acting.isEnabled() || !authorities(acting).contains("ROLE_ADMIN")) {
            throw new ResponseStatusException(403, "You are no longer an admin");
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
        boolean rider = authorities.contains("ROLE_" + Roles.RIDER);
        boolean driver = authorities.contains("ROLE_" + Roles.DRIVER);
        boolean admin = authorities.contains("ROLE_" + Roles.ADMIN);
        return new UserDto(profile.getUsername(), profile.getDisplayName(), profile.getPhone(),
                profile.isPhoneVerified(), rider, driver, admin, !enabled, profile.getVehicle(),
                profile.getPlate(), profile.getGender(), driverStatus, profile.isFlagged(),
                profile.getFlagReason(), profile.getLanguage());
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
