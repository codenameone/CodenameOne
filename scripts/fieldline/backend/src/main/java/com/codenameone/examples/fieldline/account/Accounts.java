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
package com.codenameone.examples.fieldline.account;

import com.codename1.appkit.server.Grants;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codenameone.examples.fieldline.Ids;
import com.codenameone.examples.fieldline.Roles;
import com.codenameone.examples.fieldline.api.AccountChangeDto;
import com.codenameone.examples.fieldline.api.ProfileDto;
import com.codenameone.examples.fieldline.api.RegisterDto;
import com.codenameone.examples.fieldline.api.UserDto;
import com.codenameone.examples.fieldline.domain.ClosedAccount;
import com.codenameone.examples.fieldline.domain.Profile;
import com.codenameone.examples.fieldline.domain.Technician;
import com.codenameone.examples.fieldline.field.Crew;
import com.codenameone.examples.fieldline.live.LiveHub;
import com.codenameone.examples.fieldline.repository.ClosedAccountRepository;
import com.codenameone.examples.fieldline.repository.ProfileRepository;
import com.codenameone.examples.fieldline.repository.TechnicianRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/// Accounts: opening one, describing one, and what its owner may change.
///
/// An account is two things kept under one name. Its sign-in -- password hash,
/// roles, whether it is enabled -- is the backend's own user store. Its profile
/// -- display name, phone, language -- is this application's table. The name
/// is the e-mail address, in lower case.
///
/// Someone who signs up is a technician: [Crew] gives them a regular week and
/// a place on the board, so the scheduler can plan for them at once. The
/// DISPATCHER and ADMIN roles are granted, never asked for.
@Component
public class Accounts {
    private final ProfileRepository profiles;
    private final AccountCleanup cleanup;
    private final JdbcUserDetailsManager users;
    private final PasswordEncoder encoder;
    private final LiveHub live;
    private final ClosedAccountRepository closed;
    private final Grants grants;
    private final TechnicianRepository technicians;
    private final Crew crew;

    public Accounts(ProfileRepository profiles, AccountCleanup cleanup,
            JdbcUserDetailsManager users, PasswordEncoder encoder, LiveHub live,
            ClosedAccountRepository closed, Grants grants, TechnicianRepository technicians,
            Crew crew) {
        this.profiles = profiles;
        this.cleanup = cleanup;
        this.users = users;
        this.encoder = encoder;
        this.live = live;
        this.closed = closed;
        this.grants = grants;
        this.technicians = technicians;
        this.crew = crew;
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
        String raw = request.phone() == null ? "" : request.phone().trim();
        String phone = raw.length() == 0 ? "" : phone(raw);
        if (users.userExists(username)) {
            throw new ResponseStatusException(409, "That e-mail already has an account");
        }
        // An address whose account was closed is not given to anyone again:
        // the jobs that account worked are still filed under it. See [#retire].
        if (closed.existsById(Ids.sha256(username))) {
            throw new ResponseStatusException(409,
                    "That e-mail belonged to an account that was closed. Use another");
        }
        create(username, request.password(), displayName, phone, true, false, false,
                System.currentTimeMillis());
        crew.enroll(username, displayName);
        return describe(username);
    }

    /// Creates the sign-in and the profile. The caller has checked the name is
    /// free, and makes the technician row itself when there is to be one.
    @Transactional(rollbackFor = IOException.class)
    public void create(String username, String password, String displayName, String phone,
            boolean technician, boolean dispatcher, boolean admin, long openedAt)
            throws IOException {
        users.createUser(User.withUsername(username).password(encoder.encode(password))
                .roles(roles(technician, dispatcher, admin)).build());
        Profile profile = new Profile();
        profile.setUsername(username);
        profile.setDisplayName(displayName);
        profile.setPhone(phone);
        profile.setLanguage("en");
        profile.setCreatedAt(openedAt);
        profiles.insert(profile);
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
        Technician tech = technicians.findByUsername(username);
        return toDto(profile, authorities(signIn), signIn.isEnabled(),
                tech == null ? "" : tech.getId());
    }

    @Transactional(readOnly = true)
    public ProfileDto profile(String username) throws IOException {
        Profile row = profiles.findById(username);
        if (row == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        return new ProfileDto(row.getDisplayName(), row.getPhone());
    }

    /// Saves the name and phone. A technician's name on the board follows.
    @Transactional(rollbackFor = IOException.class)
    public UserDto saveProfile(String username, ProfileDto profile) throws IOException {
        if (profile == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        // Everything is checked before anything is assigned: the row is written
        // from what it holds when the transaction commits.
        String name = required(profile.name(), 120, "your name");
        String raw = profile.phone() == null ? "" : profile.phone().trim();
        String phone = raw.length() == 0 ? "" : phone(raw);
        Profile row = profiles.findById(username);
        if (row == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        row.setDisplayName(name);
        row.setPhone(phone);
        Technician tech = technicians.findByUsername(username);
        if (tech != null) {
            tech.setName(name);
            tech.setInitials(Crew.initials(name));
        }
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

    /// Closes an account, for good: the sign-in, the profile and the settings
    /// are deleted, not hidden. The jobs it worked stay, as the customer's
    /// record, under a technician no longer linked to anyone. Refused for an
    /// admin, so that a service is not left with none.
    ///
    /// `password` is the account's own, asked for again: a token alone is not
    /// enough for something that cannot be undone.
    public UserDto close(String username, String password) throws IOException {
        UserDto was = erase(username, password);
        // After the commit and not inside it: what is said over a live
        // connection is acted on at once, and must not be said about a change
        // that could still be rolled back.
        live.drop(username);
        return new UserDto(was.username(), was.displayName(), was.phone(), was.technician(),
                was.dispatcher(), was.admin(), true, was.techId(), was.language());
    }

    /// Everything closing an account deletes, as one transaction.
    @Transactional(rollbackFor = IOException.class)
    UserDto erase(String username, String password) throws IOException {
        profiles.hold(username);
        UserDetails signIn = load(username);
        if (password == null || !encoder.matches(password, signIn.getPassword())) {
            throw new ResponseStatusException(403, "That is not your current password");
        }
        UserDto was = describe(username);
        if (was.admin()) {
            throw new ResponseStatusException(409, "Hand the admin role on before closing");
        }
        cleanup.removeEverythingOf(username);
        // Read again: the statements above each cleared what the session had
        // loaded.
        if (profiles.findById(username) != null) {
            profiles.deleteById(username);
        }
        retire(username);
        // Last: the tokens in hand stop working when the sign-in is gone.
        users.deleteUser(username);
        grants.endAll(username);
        return was;
    }

    /// Retires an e-mail for good, as only a hash of it.
    ///
    /// A job's record outlives the account that worked it, under the e-mail that account
    /// signed in with, and so does a token until it expires. Were the address
    /// free to register again, whoever took it would be handed the history of the
    /// person who left, and that person's old
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

    /// Every account, newest first, for the admin's list.
    @Transactional(readOnly = true)
    public List<UserDto> all() throws IOException {
        List<Profile> everyone = new ArrayList<Profile>(profiles.findAll());
        Collections.sort(everyone, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        List<UserDto> out = new ArrayList<UserDto>();
        for (Profile profile : everyone) {
            // One sign-in read per account: the user store answers for one
            // name at a time. A profile whose sign-in is gone is skipped
            // rather than failing the whole list.
            if (!users.userExists(profile.getUsername())) {
                continue;
            }
            UserDetails signIn = users.loadUserByUsername(profile.getUsername());
            Technician tech = technicians.findByUsername(profile.getUsername());
            out.add(toDto(profile, authorities(signIn), signIn.isEnabled(),
                    tech == null ? "" : tech.getId()));
        }
        return out;
    }

    /// Sets an account's roles and suspension, as an admin. An admin cannot
    /// take their own admin role away or suspend themselves: the console would
    /// be gone with nobody left to give it back. A suspended account cannot
    /// sign in, and the token it holds stops working on its next request,
    /// because the API reads the account every time.
    ///
    /// @return whether the technician role changed, so the caller re-plans
    @Transactional(rollbackFor = IOException.class)
    public boolean change(String by, AccountChangeDto change) throws IOException {
        if (change == null || change.username() == null) {
            throw new ResponseStatusException(400, "Name the account to change");
        }
        String username = email(change.username());
        UserDetails signIn = load(username);
        if (username.equals(by) && !change.admin()) {
            throw new ResponseStatusException(400, "You cannot withdraw your own admin role");
        }
        if (username.equals(by) && change.suspended()) {
            throw new ResponseStatusException(400, "You cannot suspend your own account");
        }
        if ((!change.admin() || change.suspended())
                && authorities(signIn).contains("ROLE_" + Roles.ADMIN) && signIn.isEnabled()) {
            // Another admin may be demoted only while someone is left to
            // run the console afterwards.
            int others = 0;
            for (UserDto u : all()) {
                if (u.admin() && !u.suspended() && !username.equals(u.username())) {
                    others++;
                }
            }
            if (others == 0) {
                throw new ResponseStatusException(400, "Keep at least one active admin");
            }
        }
        boolean wasTech = authorities(signIn).contains("ROLE_" + Roles.TECHNICIAN);
        users.updateUser(User.withUserDetails(signIn)
                .roles(roles(change.technician(), change.dispatcher(), change.admin()))
                .disabled(change.suspended()).build());
        Technician tech = technicians.findByUsername(username);
        boolean planned = change.technician() && !change.suspended();
        if (planned && tech == null) {
            Profile profile = profiles.findById(username);
            crew.enroll(username, profile == null ? username : profile.getDisplayName());
            return true;
        }
        if (tech != null && tech.isActive() != planned) {
            // Off the plan while suspended or no longer a technician; the
            // record stays, so the jobs they did keep their name.
            tech.setActive(planned);
            technicians.save(tech);
            return true;
        }
        return wasTech != change.technician();
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

    private UserDetails load(String username) {
        if (username == null || !users.userExists(username)) {
            throw new ResponseStatusException(404, "No such account");
        }
        return users.loadUserByUsername(username);
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

    private static String[] roles(boolean technician, boolean dispatcher, boolean admin) {
        List<String> roles = new ArrayList<String>();
        if (technician) {
            roles.add(Roles.TECHNICIAN);
        }
        if (dispatcher) {
            roles.add(Roles.DISPATCHER);
        }
        if (admin) {
            roles.add(Roles.ADMIN);
        }
        return roles.toArray(new String[roles.size()]);
    }

    private static UserDto toDto(Profile profile, List<String> authorities, boolean enabled,
            String techId) {
        return new UserDto(profile.getUsername(), profile.getDisplayName(), profile.getPhone(),
                authorities.contains("ROLE_" + Roles.TECHNICIAN),
                authorities.contains("ROLE_" + Roles.DISPATCHER),
                authorities.contains("ROLE_" + Roles.ADMIN), !enabled, techId,
                profile.getLanguage());
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
