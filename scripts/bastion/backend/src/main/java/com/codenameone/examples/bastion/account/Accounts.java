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
package com.codenameone.examples.bastion.account;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.mfa.RecoveryCodeService;
import com.codename1.backend.security.mfa.TotpService;
import com.codenameone.examples.bastion.Roles;
import com.codenameone.examples.bastion.api.RegisterDto;
import com.codenameone.examples.bastion.api.UserDto;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.domain.Profile;
import com.codenameone.examples.bastion.ledger.AccountKinds;
import com.codenameone.examples.bastion.ledger.Ledger;
import com.codenameone.examples.bastion.repository.ProfileRepository;

import java.io.IOException;
import java.util.Iterator;

/// Opening accounts, describing them, and changing their passwords.
///
/// The sign-in itself -- the username, the password hash, the roles, whether
/// the account is enabled -- is the security layer's, in its own tables,
/// reached through [JdbcUserDetailsManager]. What the bank adds is a
/// [Profile] beside it and the ledger accounts that hold the money.
@Component
public class Accounts {
    /// The asset and kind every new customer starts with.
    public static final String FIRST_ASSET = "USD";

    private final JdbcUserDetailsManager users;
    private final PasswordEncoder encoder;
    private final ProfileRepository profiles;
    private final Ledger ledger;
    private final Audit audit;
    private final Grants grants;
    private final TotpService totp;
    private final RecoveryCodeService recoveryCodes;

    public Accounts(JdbcUserDetailsManager users, PasswordEncoder encoder,
            ProfileRepository profiles, Ledger ledger, Audit audit, Grants grants,
            TotpService totp, RecoveryCodeService recoveryCodes) {
        this.users = users;
        this.encoder = encoder;
        this.profiles = profiles;
        this.ledger = ledger;
        this.audit = audit;
        this.grants = grants;
        this.totp = totp;
        this.recoveryCodes = recoveryCodes;
    }

    /// Opens a customer's account and a US dollar checking account with it,
    /// in one transaction: there is never a customer with nowhere to put
    /// money, nor a checking account with no customer.
    public UserDto register(RegisterDto request) throws IOException {
        try {
            return open(request);
        } catch (ResponseStatusException answered) {
            throw answered;
        } catch (IOException | RuntimeException failed) {
            // Two registrations of one address at once both pass the check
            // below and the second insert fails on the key. The transaction
            // rolled back, so nothing is left of it; say what happened.
            if (request != null && request.email() != null
                    && users.userExists(email(request.email()))) {
                throw new ResponseStatusException(409, "That e-mail already has an account");
            }
            throw failed;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    private UserDto open(RegisterDto request) throws IOException {
        if (request == null) {
            throw new ResponseStatusException(400, "Nothing to register");
        }
        String username = email(request.email());
        Passwords.check(username, request.password());
        String name = displayName(request.displayName());
        if (users.userExists(username)) {
            // Saying so tells a stranger the address banks here. Every sign-up
            // form does, since the alternative -- "check your e-mail" for both
            // cases -- needs a mail service this sample does not have. The
            // rate limit on this route is what keeps it from being a cheap
            // directory of customers.
            throw new ResponseStatusException(409, "That e-mail already has an account");
        }
        create(username, request.password(), name, false, System.currentTimeMillis());
        LedgerAccount checking = ledger.open(username, FIRST_ASSET, AccountKinds.CHECKING,
                "Checking");
        audit.append(username, "REGISTERED", username, "opened " + checking.getId());
        return describe(username);
    }

    /// Makes the sign-in and the profile. A customer, or with `admin` an
    /// administrator and nothing else: staff do not bank on their admin
    /// account.
    @Transactional(rollbackFor = Exception.class)
    void create(String username, String password, String displayName, boolean admin,
            long openedAt) throws IOException {
        users.createUser(User.withUsername(username).password(encoder.encode(password))
                .roles(admin ? Roles.ADMIN : Roles.CUSTOMER).build());
        Profile profile = new Profile();
        profile.setUsername(username);
        profile.setDisplayName(displayName);
        profile.setCreatedAt(openedAt);
        profiles.insert(profile);
    }

    public boolean exists(String username) {
        return users.userExists(username);
    }

    @Transactional(readOnly = true)
    public UserDto describe(String username) throws IOException {
        Profile profile = profiles.findById(username);
        if (profile == null || !users.userExists(username)) {
            throw new ResponseStatusException(404, "No such account");
        }
        UserDetails signIn = users.loadUserByUsername(username);
        boolean customer = false;
        boolean admin = false;
        Iterator<? extends GrantedAuthority> it = signIn.getAuthorities().iterator();
        while (it.hasNext()) {
            String held = it.next().getAuthority();
            customer |= ("ROLE_" + Roles.CUSTOMER).equals(held);
            admin |= ("ROLE_" + Roles.ADMIN).equals(held);
        }
        boolean second = totp.isEnabled(username);
        return new UserDto(username, profile.getDisplayName(), customer, admin, second,
                second ? recoveryCodes.remaining(username) : 0, profile.getCreatedAt());
    }

    public UserDto changePassword(String username, String current, String replacement)
            throws IOException {
        if (!replace(username, current, replacement)) {
            // Recorded outside the change's transaction, which a refusal rolls
            // back: someone guessing at the current password with a stolen
            // token is exactly what the audit is for.
            audit.append(username, "PASSWORD_CHANGE_REFUSED", username, "wrong current password");
            throw new ResponseStatusException(403, "That is not your current password");
        }
        return describe(username);
    }

    /// Changes the password, or answers false when `current` is not it.
    @Transactional(rollbackFor = Exception.class)
    private boolean replace(String username, String current, String replacement)
            throws IOException {
        // Before the password is read. Two changes at once would otherwise
        // both check the same old password and both succeed: whoever else
        // knows the old one could race the owner's change and keep the
        // account. Behind the hold the second checks the password the first set.
        profiles.hold(username);
        UserDetails signIn = users.loadUserByUsername(username);
        if (current == null || !encoder.matches(current, signIn.getPassword())) {
            return false;
        }
        Passwords.check(username, replacement);
        users.updatePassword(signIn, encoder.encode(replacement));
        // A password is changed, as often as not, because somebody else may
        // have the old one or a device signed in with it. A refresh token asks
        // for no password, so every sign-in but this one ends here.
        grants.endOthers(username);
        audit.append(username, "PASSWORD_CHANGED", username, "");
        return true;
    }

    /// An e-mail address as the username it is stored under: trimmed, and the
    /// ASCII letters folded to lower case by hand. `toLowerCase` would fold in
    /// the server's language, and under a Turkish locale `I` is not `i`.
    public static String email(String value) {
        String text = value == null ? "" : value.trim();
        int at = text.indexOf('@');
        if (at < 1 || at != text.lastIndexOf('@') || at > text.length() - 4
                || text.indexOf('.', at) < 0 || text.length() > 190) {
            throw new ResponseStatusException(400, "That is not an e-mail address");
        }
        String folded = asciiLower(text);
        for (int iter = 0; iter < folded.length(); iter++) {
            char c = folded.charAt(iter);
            if (c <= ' ' || c > '~') {
                throw new ResponseStatusException(400, "That is not an e-mail address");
            }
        }
        return folded;
    }

    /// `text` with A-Z folded to a-z and nothing else touched.
    static String asciiLower(String text) {
        char[] chars = text.toCharArray();
        for (int iter = 0; iter < chars.length; iter++) {
            char c = chars[iter];
            if (c >= 'A' && c <= 'Z') {
                chars[iter] = (char) (c + ('a' - 'A'));
            }
        }
        return new String(chars);
    }

    private static String displayName(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() == 0) {
            throw new ResponseStatusException(400, "Tell us your name");
        }
        if (text.length() > 120) {
            throw new ResponseStatusException(400, "That name is too long");
        }
        return text;
    }
}
