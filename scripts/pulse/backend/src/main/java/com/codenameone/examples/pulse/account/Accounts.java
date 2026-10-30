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
package com.codenameone.examples.pulse.account;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.core.userdetails.UsernameNotFoundException;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codenameone.examples.pulse.Ids;
import com.codenameone.examples.pulse.Roles;
import com.codenameone.examples.pulse.Text;
import com.codenameone.examples.pulse.api.ProfileDto;
import com.codenameone.examples.pulse.api.RegisterDto;
import com.codenameone.examples.pulse.api.UserDto;
import com.codenameone.examples.pulse.domain.ClosedAccount;
import com.codenameone.examples.pulse.domain.Member;
import com.codenameone.examples.pulse.domain.Profile;
import com.codenameone.examples.pulse.domain.Staff;
import com.codenameone.examples.pulse.live.LiveHub;
import com.codenameone.examples.pulse.pay.Payments;
import com.codenameone.examples.pulse.repository.ClassSessionRepository;
import com.codenameone.examples.pulse.repository.ClosedAccountRepository;
import com.codenameone.examples.pulse.repository.MemberRepository;
import com.codenameone.examples.pulse.repository.ProfileRepository;
import com.codenameone.examples.pulse.repository.StaffRepository;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/// Accounts: opening one, describing one, and what an owner may change.
///
/// An account is two things kept under one name. Its sign-in -- password hash,
/// roles, whether it is enabled -- is the backend's own user store. Its profile
/// -- display name, phone, who referred it -- is this application's table. The
/// name is the e-mail address, in lower case.
///
/// An account is a household. Opening one also creates its holder's own
/// member record, the one that is booked into classes and walks through the
/// door; children are added to it afterwards ([com.codenameone.examples.pulse.studio.Household]).
/// Everyone opens as a member. The staff roles -- trainer, front desk, owner --
/// are given by an owner.
@Component
public class Accounts {
    /// The youngest age at which someone may hold an account of their own.
    /// Younger people are members of a parent's household.
    public static final int ACCOUNT_AGE = 16;

    private final ProfileRepository profiles;
    private final AccountCleanup cleanup;
    private final JdbcUserDetailsManager users;
    private final PasswordEncoder encoder;
    private final Moderation moderation;
    private final Payments payments;
    private final LiveHub live;
    private final ClosedAccountRepository closed;
    private final Grants grants;
    private final MemberRepository members;
    private final StaffRepository staffRows;
    private final ClassSessionRepository sessions;

    public Accounts(ProfileRepository profiles, AccountCleanup cleanup,
            JdbcUserDetailsManager users, PasswordEncoder encoder, Moderation moderation,
            Payments payments, LiveHub live, ClosedAccountRepository closed, Grants grants,
            MemberRepository members, StaffRepository staffRows,
            ClassSessionRepository sessions) {
        this.closed = closed;
        this.grants = grants;
        this.profiles = profiles;
        this.cleanup = cleanup;
        this.users = users;
        this.encoder = encoder;
        this.moderation = moderation;
        this.payments = payments;
        this.live = live;
        this.members = members;
        this.staffRows = staffRows;
        this.sessions = sessions;
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
        String displayName = required(request.displayName(), 120, "your name");
        String phone = phone(request.phone());
        LocalDate born = birthDate(request.birthDate());
        if (age(born, LocalDate.now()) < ACCOUNT_AGE) {
            throw new ResponseStatusException(400, "Under " + ACCOUNT_AGE
                    + "s join as part of a parent's household");
        }
        // A referral names an existing member; anything else is dropped rather
        // than refused, since it only decides who is thanked.
        String referrer = "";
        if (request.referredBy() != null && request.referredBy().trim().length() > 0) {
            String named = email(request.referredBy());
            if (users.userExists(named)) {
                referrer = named;
            }
        }
        if (users.userExists(username)) {
            throw new ResponseStatusException(409, "That e-mail already has an account");
        }
        // An address whose account was closed is not given to anyone again:
        // the invoices and visits that account had are still filed under it.
        // See [#retire].
        if (closed.existsById(Ids.sha256(username))) {
            throw new ResponseStatusException(409,
                    "That e-mail belonged to an account that was closed. Use another");
        }
        create(username, request.password(), displayName, phone, false, born, false, false,
                false, System.currentTimeMillis());
        if (referrer.length() > 0) {
            profiles.findById(username).setReferredBy(referrer);
        }
        return describe(username);
    }

    /// Creates the sign-in, the profile and the holder's own member record.
    /// The caller has checked the name is free. An account created with staff
    /// roles is how the demo accounts are made, not how anyone signs up.
    /// `openedAt` is when the account was opened; the demo data is the one
    /// caller with a past to write.
    @Transactional(rollbackFor = IOException.class)
    public void create(String username, String password, String displayName, String phone,
            boolean phoneVerified, LocalDate born, boolean trainer, boolean desk, boolean owner,
            long openedAt) throws IOException {
        users.createUser(User.withUsername(username).password(encoder.encode(password))
                .roles(roles(trainer, desk, owner)).build());
        Instant opened = Instant.ofEpochMilli(openedAt);
        Profile profile = new Profile();
        profile.setUsername(username);
        profile.setDisplayName(displayName);
        profile.setPhone(phone);
        profile.setPhoneVerified(phoneVerified);
        profile.setCreatedAt(opened);
        profiles.insert(profile);
        Member self = new Member();
        self.setId(Ids.next());
        self.setAccount(username);
        self.setName(displayName);
        self.setBirthDate(born);
        self.setRelation(SELF);
        self.setCreatedAt(opened);
        members.insert(self);
        if (trainer) {
            staffRow(username, true);
        }
    }

    /// The relation of an account holder's own member record.
    public static final String SELF = "SELF";

    public boolean exists(String username) {
        return users.userExists(username);
    }

    /// Whether the account exists and has not been blocked, read once.
    public boolean enabled(String username) {
        try {
            return users.loadUserByUsername(username).isEnabled();
        } catch (UsernameNotFoundException gone) {
            return false;
        }
    }

    /// The account as the app sees it. Answers 404 for a name with no account.
    @Transactional(readOnly = true)
    public UserDto describe(String username) throws IOException {
        Profile profile = profiles.findById(username);
        if (profile == null || !users.userExists(username)) {
            throw new ResponseStatusException(404, "No such account");
        }
        UserDetails signIn = users.loadUserByUsername(username);
        return toDto(profile, authorities(signIn), signIn.isEnabled());
    }

    /// Every account, newest first, for the owner.
    @Transactional(readOnly = true)
    public List<UserDto> all() throws IOException {
        List<UserDto> out = new ArrayList<UserDto>();
        List<Profile> everyone = profiles.newestFirst();
        for (int iter = 0; iter < everyone.size(); iter++) {
            Profile profile = everyone.get(iter);
            // One sign-in read per account. The user store answers for one name
            // at a time and has no way to list its users or to load several at
            // once, and its tables are its own, not this application's to
            // query.
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
            out.add(toDto(profile, held, enabled));
        }
        return out;
    }

    /// Gives or withdraws the staff roles. `by` is the owner doing it, who
    /// cannot withdraw their own owner role: that is how a studio ends up with
    /// nobody able to run it.
    @Transactional(rollbackFor = IOException.class)
    public UserDto setRoles(String by, String username, boolean trainer, boolean desk,
            boolean owner) throws IOException {
        actingOwner(by, username);
        UserDetails signIn = load(username);
        if (!owner && username.equals(by)) {
            throw new ResponseStatusException(400, "You cannot withdraw your own owner role");
        }
        List<String> held = authorities(signIn);
        boolean wasTrainer = held.contains("ROLE_" + Roles.TRAINER);
        // Not from under a timetable. A class with nobody to teach it would be
        // booked, paid for and turned up to; the owner gives its classes to
        // someone else first.
        if (wasTrainer && !trainer && sessions.existsByTrainerAndStateAndStartsAtGreaterThan(
                username, "SCHEDULED", Instant.now())) {
            throw new ResponseStatusException(409,
                    "That trainer has classes coming up. Give them to someone else first");
        }
        users.updateUser(User.withUserDetails(signIn).roles(roles(trainer, desk, owner))
                .build());
        if (trainer != wasTrainer) {
            staffRow(username, trainer);
        }
        moderation.record(username, by, Moderation.ROLES, describeRoles(trainer, desk, owner));
        UserDto now = describe(username);
        boolean wasStaff = wasTrainer || held.contains("ROLE_" + Roles.DESK)
                || held.contains("ROLE_" + Roles.OWNER);
        if (wasStaff && !(trainer || desk || owner)) {
            // A live connection opened as staff goes on hearing who came
            // through the door; it is closed, and the app opens a member's.
            live.drop(username);
        }
        return now;
    }

    private static String describeRoles(boolean trainer, boolean desk, boolean owner) {
        StringBuilder out = new StringBuilder("member");
        if (trainer) {
            out.append(", trainer");
        }
        if (desk) {
            out.append(", front desk");
        }
        if (owner) {
            out.append(", owner");
        }
        return out.toString();
    }

    /// Makes a trainer's staff row exist and be active, or retires it. The row
    /// stays when the role goes, so the classes they taught still name them.
    private void staffRow(String username, boolean active) {
        Staff row = staffRows.findById(username);
        if (row == null) {
            if (!active) {
                return;
            }
            row = new Staff();
            row.setUsername(username);
            row.setTitle("Trainer");
            row.setActive(true);
            staffRows.insert(row);
            return;
        }
        row.setActive(active);
    }

    /// Blocks an account or lifts the block. A blocked account cannot sign in,
    /// and a token it already holds stops working at once, because the API
    /// reads the account on every request. Its bookings stay: the desk can
    /// still see who was expected, and the door refuses a blocked account by
    /// itself.
    @Transactional(rollbackFor = IOException.class)
    public UserDto suspend(String by, String username, boolean suspended, String reason)
            throws IOException {
        actingOwner(by, username);
        UserDetails signIn = load(username);
        if (suspended && username.equals(by)) {
            throw new ResponseStatusException(400, "You cannot suspend your own account");
        }
        String why = Text.optional(reason, 255, "the reason");
        users.updateUser(User.withUserDetails(signIn).disabled(suspended).build());
        if (suspended == signIn.isEnabled()) {
            // Only a change is an event; saying the same thing twice is not.
            moderation.record(username, by, suspended ? Moderation.BLOCK : Moderation.UNBLOCK, why);
        }
        return describe(username);
    }

    /// Marks an account for the owners' attention, or clears the mark. A flag
    /// changes nothing the account can do; blocking is what does that.
    @Transactional(rollbackFor = IOException.class)
    public UserDto flag(String by, String username, boolean flagged, String reason)
            throws IOException {
        // The profile row first, as the other owner changes hold it: a flag
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
        return new ProfileDto(row.getDisplayName());
    }

    @Transactional(rollbackFor = IOException.class)
    public UserDto saveProfile(String username, ProfileDto profile) throws IOException {
        if (profile == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        String name = required(profile.name(), 120, "your name");
        Profile row = profiles.findById(username);
        if (row == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        row.setDisplayName(name);
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

    /// Closes an account, for good: the sign-in, the profile, the household,
    /// its passes, saved cards and workouts are deleted, not hidden. Invoices and
    /// visits stay, as the studio's records, under a name that no longer has
    /// anyone behind it. Places held in classes to come are handed back, and
    /// the waitlists move up on [com.codenameone.examples.pulse.studio.Bookings]'
    /// next sweep.
    ///
    /// Refused for an owner, so that a studio is not left with none.
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
        return new UserDto(was.username(), was.displayName(), was.phone(), was.phoneVerified(),
                was.member(), was.trainer(), was.desk(), was.owner(), true, was.flagged(),
                was.flagReason(), was.language());
    }

    /// Everything closing an account deletes, as one transaction: either the
    /// account is gone from every table or it is in all of them still. The
    /// user store works on the same database and so takes part in it.
    @Transactional(rollbackFor = IOException.class)
    UserDto erase(String username, String password) throws IOException {
        // Before anything is read: a booking or a payment for the account
        // holds the same row, so it either lands before the clean-up, which
        // then removes it, or finds no account.
        profiles.hold(username);
        UserDetails signIn = load(username);
        if (password == null || !encoder.matches(password, signIn.getPassword())) {
            throw new ResponseStatusException(403, "That is not your current password");
        }
        UserDto was = describe(username);
        if (was.owner()) {
            throw new ResponseStatusException(409, "Hand the owner role on before closing");
        }
        // A charge the processor may still be making would be lost track of
        // with the card it was made on.
        payments.forget(username);
        moderation.forget(username);
        cleanup.removeEverythingOf(username);
        if (was.trainer()) {
            Staff staff = staffRows.findById(username);
            if (staff != null) {
                staffRows.deleteById(username);
            }
        }
        // Read again: the statements above each cleared what the session had
        // loaded, the profile that was described among it.
        Profile profile = profiles.findById(username);
        if (profile != null) {
            profiles.deleteById(username);
        }
        // The address is retired with the account, in the same transaction:
        // nobody can register it and inherit the invoices kept under it.
        retire(username);
        // Last: the tokens in hand stop working when the sign-in is gone.
        users.deleteUser(username);
        grants.endAll(username);
        return was;
    }

    /// Retires an e-mail for good, as only a hash of it.
    ///
    /// A delivery outlives the account that took it, under the e-mail that account
    /// signed in with, and so does a token until it expires. Were the address
    /// free to register again, whoever took it would be handed the deliveries, the
    /// receipts and the ratings of the person who left, and that person's old
    /// token would sign in to the new account. Only a hash is kept: enough to
    /// refuse it, and not a list of who has left.
    private void retire(String username) throws IOException {
        String hash = Ids.sha256(username);
        if (!closed.existsById(hash)) {
            ClosedAccount row = new ClosedAccount();
            row.setUsernameHash(hash);
            row.setClosedAt(Instant.now());
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

    /// Makes a change one owner is making to an account take its turn with any
    /// other touching either of the two, and then checks `by` still is an
    /// owner. Call it before anything is read.
    ///
    /// Without it two owners could each take the other's role, or block each
    /// other, at the same moment. Each request was authorized before the other
    /// committed and neither touches its own account, so both would go through
    /// and the service would be left with nobody to run it. Both
    /// profiles are held, in the order of their names so that the two requests
    /// queue and cannot deadlock; whichever comes second then reads what the
    /// first left, finds it is no longer an owner, and is refused.
    private void actingOwner(String by, String username) {
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
            throw new ResponseStatusException(403, "You are no longer an owner");
        }
        UserDetails acting = users.loadUserByUsername(by);
        if (!acting.isEnabled() || !authorities(acting).contains("ROLE_" + Roles.OWNER)) {
            throw new ResponseStatusException(403, "You are no longer an owner");
        }
    }

    private UserDetails load(String username) {
        if (username == null || !users.userExists(username)) {
            throw new ResponseStatusException(404, "No such account");
        }
        return users.loadUserByUsername(username);
    }

    private static String[] roles(boolean trainer, boolean desk, boolean owner) {
        List<String> roles = new ArrayList<String>();
        roles.add(Roles.MEMBER);
        if (trainer) {
            roles.add(Roles.TRAINER);
        }
        if (desk) {
            roles.add(Roles.DESK);
        }
        if (owner) {
            roles.add(Roles.OWNER);
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

    private static UserDto toDto(Profile profile, List<String> authorities, boolean enabled) {
        return new UserDto(profile.getUsername(), profile.getDisplayName(), profile.getPhone(),
                profile.isPhoneVerified(), authorities.contains("ROLE_" + Roles.MEMBER),
                authorities.contains("ROLE_" + Roles.TRAINER),
                authorities.contains("ROLE_" + Roles.DESK),
                authorities.contains("ROLE_" + Roles.OWNER), !enabled, profile.isFlagged(),
                profile.getFlagReason(), profile.getLanguage());
    }

    /// A birth date as the app sends it, `yyyy-MM-dd`. Answers 400 for one
    /// that is not a date, or is in the future.
    public static LocalDate birthDate(String value) {
        LocalDate date;
        try {
            date = LocalDate.parse(value == null ? "" : value.trim());
        } catch (DateTimeParseException bad) {
            throw new ResponseStatusException(400, "That is not a date of birth");
        }
        if (date.isAfter(LocalDate.now()) || date.getYear() < 1900) {
            throw new ResponseStatusException(400, "That is not a date of birth");
        }
        return date;
    }

    /// Whole years between a birth date and a day.
    public static int age(LocalDate born, LocalDate on) {
        int years = on.getYear() - born.getYear();
        if (on.getMonthValue() < born.getMonthValue() || on.getMonthValue() == born.getMonthValue()
                && on.getDayOfMonth() < born.getDayOfMonth()) {
            years--;
        }
        return years;
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
