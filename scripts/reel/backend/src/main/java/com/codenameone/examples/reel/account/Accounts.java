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
package com.codenameone.examples.reel.account;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codenameone.examples.reel.Ids;
import com.codenameone.examples.reel.Roles;
import com.codenameone.examples.reel.api.AccountDto;
import com.codenameone.examples.reel.api.ProfileDto;
import com.codenameone.examples.reel.api.SignupDto;
import com.codenameone.examples.reel.api.UserDto;
import com.codenameone.examples.reel.billing.Entitlements;
import com.codenameone.examples.reel.domain.ClosedAccount;
import com.codenameone.examples.reel.domain.Profile;
import com.codenameone.examples.reel.pay.Payments;
import com.codenameone.examples.reel.repository.ClosedAccountRepository;
import com.codenameone.examples.reel.repository.ProfileRepository;

import java.time.Instant;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/// Accounts: opening one, describing one, and what an admin may change.
///
/// An account is two things kept under one name. Its sign-in -- password hash,
/// roles, whether it is enabled -- is the backend's own user store. Its profile
/// is this application's table. The name is the e-mail address, in lower case.
@Component
public class Accounts {
    private final ProfileRepository profiles;
    private final AccountCleanup cleanup;
    private final JdbcUserDetailsManager users;
    private final PasswordEncoder encoder;
    private final Moderation moderation;
    private final Payments payments;
    private final ClosedAccountRepository closed;
    private final Grants grants;
    private final Entitlements entitlements;

    public Accounts(ProfileRepository profiles, AccountCleanup cleanup, JdbcUserDetailsManager users,
            PasswordEncoder encoder, Moderation moderation, Payments payments,
            ClosedAccountRepository closed, Grants grants, Entitlements entitlements) {
        this.profiles = profiles;
        this.cleanup = cleanup;
        this.users = users;
        this.encoder = encoder;
        this.moderation = moderation;
        this.payments = payments;
        this.closed = closed;
        this.grants = grants;
        this.entitlements = entitlements;
    }

    /// Opens an account. Answers 400 for a request that cannot be one and 409
    /// when the e-mail already has one. Not a transaction itself, so that after
    /// a failure it can look again: two sign-ups racing for one e-mail both
    /// pass the check, the unique key refuses the second, and the 409 is the
    /// honest answer whatever the database called the error.
    public AccountDto register(SignupDto request) throws IOException {
        try {
            return open(request);
        } catch (ResponseStatusException answered) {
            throw answered;
        } catch (IOException | RuntimeException failed) {
            if (request != null && request.email() != null && users.userExists(email(request.email()))) {
                throw new ResponseStatusException(409, "That e-mail already has an account");
            }
            throw failed;
        }
    }

    @Transactional(rollbackFor = IOException.class)
    private AccountDto open(SignupDto request) throws IOException {
        if (request == null) {
            throw new ResponseStatusException(400, "Nothing to register");
        }
        String username = email(request.email());
        password(request.password());
        String name = required(request.name(), 120, "your name");
        if (users.userExists(username)) {
            throw new ResponseStatusException(409, "That e-mail already has an account");
        }
        // A closed account's e-mail is never handed out again: its invoices and
        // certificates are still filed under it.
        if (closed.existsById(Ids.sha256(username))) {
            throw new ResponseStatusException(409,
                    "That e-mail belonged to an account that was closed. Use another");
        }
        create(username, request.password(), name, new String[] {Roles.VIEWER},
                Instant.now());
        return describe(username);
    }

    /// Creates the sign-in and the profile. The caller has checked the name is free.
    @Transactional(rollbackFor = IOException.class)
    public void create(String username, String password, String name, String[] roles, Instant openedAt)
            throws IOException {
        users.createUser(User.withUsername(username).password(encoder.encode(password))
                .roles(roles).build());
        Profile profile = new Profile();
        profile.setUsername(username);
        profile.setDisplayName(name);
        profile.setCreatedAt(openedAt);
        profile.setLanguage("en");
        profiles.insert(profile);
    }

    public boolean exists(String username) {
        return username != null && users.userExists(username);
    }

    /// The account as its owner sees it. Answers 404 for a name with no account.
    @Transactional(readOnly = true)
    public AccountDto describe(String username) throws IOException {
        Profile profile = profiles.findById(username);
        if (profile == null || !users.userExists(username)) {
            throw new ResponseStatusException(404, "No such account");
        }
        UserDetails signIn = users.loadUserByUsername(username);
        Entitlements.Allowance plan = entitlements.allowance(username);
        return new AccountDto(username, profile.getDisplayName(), username, roles(signIn),
                plan.planCode, plan.planName, plan.status, plan.periodEndsAt, plan.ads, plan.maxStreams,
                plan.maxQuality, plan.downloads, !signIn.isEnabled());
    }

    /// The account as the admin console lists it, without the counts the
    /// console adds.
    @Transactional(readOnly = true)
    public UserDto summary(String username, int devices, int streamsNow) throws IOException {
        Profile profile = profiles.findById(username);
        if (profile == null || !users.userExists(username)) {
            throw new ResponseStatusException(404, "No such account");
        }
        UserDetails signIn = users.loadUserByUsername(username);
        Entitlements.Allowance plan = entitlements.allowance(username);
        return new UserDto(username, profile.getDisplayName(), username, roles(signIn), plan.planCode,
                plan.status, !signIn.isEnabled(), profile.isFlagged(), devices, streamsNow,
                profile.getCreatedAt().toEpochMilli(), profile.getLastSeenAt().toEpochMilli());
    }

    @Transactional(readOnly = true)
    public List<String> newestFirst() {
        List<Profile> rows = profiles.newestFirst();
        List<String> out = new ArrayList<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(rows.get(iter).getUsername());
        }
        return out;
    }

    @Transactional(readOnly = true)
    public String displayName(String username) {
        Profile profile = username == null ? null : profiles.findById(username);
        return profile == null ? "" : profile.getDisplayName();
    }

    /// Whether the account holds `role`, read from the user store now.
    public boolean hasRole(String username, String role) {
        if (!exists(username)) {
            return false;
        }
        return roles(users.loadUserByUsername(username)).contains(role);
    }

    /// Replaces an account's roles. VIEWER is always kept. An admin cannot take
    /// the admin role from themselves, so the service is never left with none.
    public UserDto setRoles(String by, String username, List<String> wanted) throws IOException {
        List<String> roles = new ArrayList<String>();
        roles.add(Roles.VIEWER);
        String[] known = {Roles.CREATOR, Roles.MODERATOR, Roles.ADMIN};
        for (int iter = 0; iter < known.length; iter++) {
            if (wanted != null && wanted.contains(known[iter])) {
                roles.add(known[iter]);
            }
        }
        if (by.equals(username) && !roles.contains(Roles.ADMIN)) {
            throw new ResponseStatusException(409, "You cannot take the admin role from yourself");
        }
        changeRoles(by, username, roles);
        return summary(username, 0, 0);
    }

    @Transactional(rollbackFor = IOException.class)
    private void changeRoles(String by, String username, List<String> roles) throws IOException {
        actingAdmin(by, username);
        UserDetails signIn = load(username);
        users.updateUser(User.withUsername(username).password(signIn.getPassword())
                .disabled(!signIn.isEnabled()).roles(roles.toArray(new String[roles.size()])).build());
        moderation.record(username, by, Moderation.ROLES, String.join(", ", roles));
    }

    /// Blocks an account or lifts the block. A blocked account's tokens stop
    /// working at its next request: the roles are read on every one.
    public UserDto suspend(String by, String username, boolean suspended, String reason)
            throws IOException {
        if (by.equals(username)) {
            throw new ResponseStatusException(409, "You cannot block yourself");
        }
        changeSuspension(by, username, suspended, reason);
        if (suspended) {
            grants.endAll(username);
        }
        return summary(username, 0, 0);
    }

    @Transactional(rollbackFor = IOException.class)
    private void changeSuspension(String by, String username, boolean suspended, String reason)
            throws IOException {
        actingAdmin(by, username);
        UserDetails signIn = load(username);
        List<String> roles = roles(signIn);
        users.updateUser(User.withUsername(username).password(signIn.getPassword()).disabled(suspended)
                .roles(roles.toArray(new String[roles.size()])).build());
        moderation.record(username, by, suspended ? Moderation.BLOCK : Moderation.UNBLOCK, reason);
    }

    @Transactional(rollbackFor = IOException.class)
    public UserDto flag(String by, String username, boolean flagged, String reason) throws IOException {
        Profile profile = profiles.findById(username);
        if (profile == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        String why = reason == null ? "" : reason.trim();
        profile.setFlagged(flagged);
        profile.setFlagReason(flagged ? (why.length() > 255 ? why.substring(0, 255) : why) : "");
        profiles.save(profile);
        moderation.record(username, by, flagged ? Moderation.FLAG : Moderation.UNFLAG, why);
        return summary(username, 0, 0);
    }

    @Transactional(rollbackFor = IOException.class)
    public AccountDto saveProfile(String username, ProfileDto wanted) throws IOException {
        Profile profile = profiles.findById(username);
        if (profile == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        profile.setDisplayName(required(wanted == null ? null : wanted.name(), 120, "your name"));
        profiles.save(profile);
        return describe(username);
    }

    /// Notes that the account was used, at most once a minute.
    @Transactional
    public void seen(String username) {
        Profile profile = profiles.findById(username);
        Instant now = Instant.now();
        if (profile != null && profile.getLastSeenAt().plusMillis(60000L).isBefore(now)) {
            profile.setLastSeenAt(now);
            profiles.save(profile);
        }
    }

    /// Changes the password, and ends every other sign-in: a password is changed
    /// as often as not because someone else may have the old one.
    @Transactional(rollbackFor = IOException.class)
    public AccountDto changePassword(String username, String current, String replacement)
            throws IOException {
        // Held first so two changes at once queue; the second then checks
        // against the password the first one set.
        profiles.hold(username);
        UserDetails signIn = load(username);
        if (current == null || !encoder.matches(current, signIn.getPassword())) {
            throw new ResponseStatusException(403, "That is not your current password");
        }
        password(replacement);
        users.updatePassword(signIn, encoder.encode(replacement));
        grants.endOthers(username);
        return describe(username);
    }

    /// Closes an account for good: the sign-in, the profile, the household,
    /// the devices, the saved cards and the viewing history are deleted. The
    /// invoices stay, as the service's accounts, under a retired name.
    /// `password` is asked for again: a token alone must not be enough for
    /// something that cannot be undone.
    @Transactional(rollbackFor = IOException.class)
    public void close(String username, String password) throws IOException {
        profiles.hold(username);
        UserDetails signIn = load(username);
        if (password == null || !encoder.matches(password, signIn.getPassword())) {
            throw new ResponseStatusException(403, "That is not your current password");
        }
        if (roles(signIn).contains(Roles.ADMIN)) {
            throw new ResponseStatusException(409, "Hand the admin role on before closing");
        }
        payments.forget(username);
        moderation.forget(username);
        cleanup.removeEverythingOf(username);
        if (profiles.findById(username) != null) {
            profiles.deleteById(username);
        }
        String hash = Ids.sha256(username);
        if (!closed.existsById(hash)) {
            ClosedAccount row = new ClosedAccount();
            row.setUsernameHash(hash);
            row.setClosedAt(Instant.now());
            closed.insert(row);
        }
        users.deleteUser(username);
        grants.endAll(username);
    }

    /// Holds both accounts, in name order so two admins cannot deadlock, and
    /// checks `by` still is an admin: otherwise two admins could each strip the
    /// other at the same moment and leave the service with none.
    private void actingAdmin(String by, String username) {
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
        if (!acting.isEnabled() || !roles(acting).contains(Roles.ADMIN)) {
            throw new ResponseStatusException(403, "You are no longer an admin");
        }
    }

    private UserDetails load(String username) {
        if (username == null || !users.userExists(username)) {
            throw new ResponseStatusException(404, "No such account");
        }
        return users.loadUserByUsername(username);
    }

    /// The roles, without their `ROLE_` prefix.
    private static List<String> roles(UserDetails signIn) {
        List<String> held = new ArrayList<String>();
        Iterator<? extends GrantedAuthority> it = signIn.getAuthorities().iterator();
        while (it.hasNext()) {
            String name = it.next().getAuthority();
            held.add(name.startsWith("ROLE_") ? name.substring(5) : name);
        }
        return held;
    }

    private static void password(String value) {
        if (value == null || value.length() < 8) {
            throw new ResponseStatusException(400, "Choose a password of at least 8 characters");
        }
        if (value.length() > 200) {
            throw new ResponseStatusException(400, "That password is too long");
        }
    }

    /// An e-mail address as an account name: trimmed, and its ASCII letters
    /// folded by hand, because `toLowerCase()` follows the server's locale and
    /// a key must not.
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
