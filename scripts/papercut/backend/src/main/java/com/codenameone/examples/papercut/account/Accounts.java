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
package com.codenameone.examples.papercut.account;

import com.codename1.appkit.server.Text;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codenameone.examples.papercut.Roles;
import com.codenameone.examples.papercut.api.PlayerDto;
import com.codenameone.examples.papercut.api.ProfileDto;
import com.codenameone.examples.papercut.api.RegisterDto;
import com.codenameone.examples.papercut.domain.Profile;
import com.codenameone.examples.papercut.repository.ProfileRepository;
import com.codenameone.examples.papercut.shop.Catalog;
import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;

/// Accounts: the sign-in record (users and authorities, owned by the security
/// layer) plus the [Profile] row that holds everything the game knows.
@Component
public class Accounts {
    /// What a new player's wallet starts with: enough for a spare eraser pack
    /// on day one, not enough for a paper.
    public static final int START_COINS = 50;

    private final ProfileRepository profiles;
    private final JdbcUserDetailsManager users;
    private final PasswordEncoder encoder;

    public Accounts(ProfileRepository profiles, JdbcUserDetailsManager users, PasswordEncoder encoder) {
        this.profiles = profiles;
        this.users = users;
        this.encoder = encoder;
    }

    public PlayerDto register(RegisterDto request) throws IOException {
        if (request == null) {
            throw new ResponseStatusException(400, "Nothing to register");
        }
        String username = email(request.email());
        password(request.password());
        String name = displayName(request.displayName());
        if (users.userExists(username)) {
            throw new ResponseStatusException(409, "That e-mail already has an account");
        }
        try {
            create(username, request.password(), name, false);
        } catch (IOException | RuntimeException failed) {
            // Two registrations racing for one address: the loser sees 409,
            // not whatever the database said about the duplicate key.
            if (users.userExists(username)) {
                throw new ResponseStatusException(409, "That e-mail already has an account");
            }
            throw failed;
        }
        return describe(username);
    }

    @Transactional(rollbackFor = IOException.class)
    public void create(String username, String password, String name, boolean admin) throws IOException {
        users.createUser(User.withUsername(username).password(encoder.encode(password))
                .roles(admin ? new String[] {Roles.PLAYER, Roles.ADMIN} : new String[] {Roles.PLAYER})
                .build());
        Profile p = new Profile();
        p.setUsername(username);
        p.setDisplayName(name);
        p.setInkCoins(START_COINS);
        p.setQuill(Catalog.FREE_QUILL);
        p.setOwned(Catalog.FREE_QUILL);
        p.setCreatedAt(Instant.now());
        p.setLastSeen(Instant.now());
        profiles.insert(p);
    }

    public boolean exists(String username) {
        return users.userExists(username);
    }

    /// The signed-in player, which also counts as being seen today: the
    /// console's "active today" and a lapsed broadcast both read `lastSeen`.
    @Transactional(rollbackFor = IOException.class)
    public PlayerDto me(String username) throws IOException {
        Profile p = profile(username);
        p.setLastSeen(Instant.now());
        profiles.save(p);
        return toDto(p);
    }

    @Transactional(readOnly = true)
    public PlayerDto describe(String username) throws IOException {
        Profile p = profiles.findById(username);
        if (p == null || !users.userExists(username)) {
            throw new ResponseStatusException(404, "No such account");
        }
        return toDto(p);
    }

    public PlayerDto toDto(Profile p) {
        UserDetails signIn = users.loadUserByUsername(p.getUsername());
        return new PlayerDto(p.getUsername(), p.getDisplayName(), p.getInkCoins(), p.getBestClassic(),
                p.getBestFold(), p.getBestDaily(), p.getRuns(), isAdmin(signIn),
                !signIn.isEnabled() || isBenched(p), p.isFlagged(), p.getQuill(),
                Lists.split(p.getOwned()));
    }

    /// Changes the name and, when given, the equipped paper (which must be owned).
    @Transactional(rollbackFor = IOException.class)
    public PlayerDto saveProfile(String username, ProfileDto profile) throws IOException {
        if (profile == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        Profile p = profile(username);
        if (profile.displayName() != null) {
            p.setDisplayName(displayName(profile.displayName()));
        }
        if (profile.quill() != null && profile.quill().length() > 0) {
            if (!Lists.contains(p.getOwned(), profile.quill())) {
                throw new ResponseStatusException(403, "You do not own that paper");
            }
            p.setQuill(profile.quill());
        }
        profiles.save(p);
        return toDto(p);
    }

    /// Whether a player is kept off the boards: banned, or suspended until a
    /// time still to come.
    public static boolean isBenched(Profile p) {
        return p.isBanned() || p.getSuspendedUntil().toEpochMilli() > System.currentTimeMillis();
    }

    /// Enables or disables sign-in. A disabled account's tokens stop working on
    /// its next request ([com.codenameone.examples.papercut.SecurityConfig] reads
    /// the account every time).
    public void setEnabled(String username, boolean enabled) {
        UserDetails signIn = users.loadUserByUsername(username);
        if (signIn.isEnabled() != enabled) {
            users.updateUser(User.withUserDetails(signIn).disabled(!enabled).build());
        }
    }

    public boolean isAdmin(String username) {
        return users.userExists(username) && isAdmin(users.loadUserByUsername(username));
    }

    private static boolean isAdmin(UserDetails signIn) {
        Iterator<? extends GrantedAuthority> it = signIn.getAuthorities().iterator();
        while (it.hasNext()) {
            if (("ROLE_" + Roles.ADMIN).equals(it.next().getAuthority())) {
                return true;
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    public Profile profile(String username) {
        Profile p = profiles.findById(username);
        if (p == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        return p;
    }

    /// An e-mail address, folded to lower case by hand: `toLowerCase` depends
    /// on the language the server runs in.
    public static String email(String value) {
        String text = value == null ? "" : value.trim();
        int at = text.indexOf('@');
        if (at < 1 || at != text.lastIndexOf('@') || at > text.length() - 4
                || text.indexOf('.', at) < 0 || text.length() > 190) {
            throw new ResponseStatusException(400, "That is not an e-mail address");
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c <= ' ' || c > '~') {
                throw new ResponseStatusException(400, "That is not an e-mail address");
            }
            out.append(c >= 'A' && c <= 'Z' ? (char) (c + ('a' - 'A')) : c);
        }
        return out.toString();
    }

    static void password(String value) {
        if (value == null || value.length() < 8) {
            throw new ResponseStatusException(400, "Choose a password of at least 8 characters");
        }
        if (value.length() > 200) {
            throw new ResponseStatusException(400, "That password is too long");
        }
    }

    /// A player name: 3 to 20 characters, shown on every board, so control
    /// characters and leading or trailing spaces are refused.
    static String displayName(String value) {
        String name = Text.required(value, 20, "your player name").trim();
        if (name.length() < 3) {
            throw new ResponseStatusException(400, "A player name needs at least 3 characters");
        }
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) < ' ') {
                throw new ResponseStatusException(400, "That name has characters we cannot show");
            }
        }
        return name;
    }
}
