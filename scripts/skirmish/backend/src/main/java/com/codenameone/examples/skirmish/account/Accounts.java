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
package com.codenameone.examples.skirmish.account;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codenameone.examples.skirmish.Clock;
import com.codenameone.examples.skirmish.Roles;
import com.codenameone.examples.skirmish.Text;
import com.codenameone.examples.skirmish.api.PlayerDto;
import com.codenameone.examples.skirmish.api.RegisterDto;
import com.codenameone.examples.skirmish.domain.Profile;
import com.codenameone.examples.skirmish.repository.ProfileRepository;
import com.codenameone.examples.skirmish.rules.Deck;
import com.codenameone.examples.skirmish.shop.Catalog;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/// Accounts: the sign-in record (users and authorities, owned by the security
/// layer) plus the [Profile] row that holds everything the game knows.
@Component
public class Accounts {
    public static final int START_COINS = 100;
    public static final int START_GEMS = 20;

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
        p.setCoins(START_COINS);
        p.setGems(START_GEMS);
        p.setDeck(Lists.join(Deck.starter()));
        p.setOwned(Catalog.DEFAULT_ARENA + "," + Catalog.DEFAULT_EMOTES);
        p.setArena(Catalog.DEFAULT_ARENA);
        p.setCreatedAt(Clock.now());
        p.setLastSeen(Clock.now());
        profiles.insert(p);
    }

    public boolean exists(String username) {
        return users.userExists(username);
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
        return new PlayerDto(p.getUsername(), p.getDisplayName(), p.getTrophies(), p.getWins(),
                p.getLosses(), p.getDraws(), p.getCoins(), p.getGems(), isAdmin(signIn),
                !signIn.isEnabled(), p.isMuted(), p.isFlagged(), Lists.split(p.getDeck()),
                Lists.split(p.getOwned()), p.getArena());
    }

    @Transactional(rollbackFor = IOException.class)
    public PlayerDto rename(String username, String displayName) throws IOException {
        Profile p = profile(username);
        p.setDisplayName(displayName(displayName));
        profiles.save(p);
        return toDto(p);
    }

    /// Enables or disables sign-in. A disabled account's tokens stop working on
    /// its next request, and the game socket is closed by the caller.
    public void setEnabled(String username, boolean enabled) {
        UserDetails signIn = users.loadUserByUsername(username);
        if (signIn.isEnabled() != enabled) {
            users.updateUser(User.withUserDetails(signIn).disabled(!enabled).build());
        }
    }

    public boolean isEnabled(String username) {
        return users.userExists(username) && users.loadUserByUsername(username).isEnabled();
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

    public static String email(String value) {
        String text = value == null ? "" : value.trim();
        int at = text.indexOf('@');
        if (at < 1 || at != text.lastIndexOf('@') || at > text.length() - 4
                || text.indexOf('.', at) < 0 || text.length() > 190) {
            throw new ResponseStatusException(400, "That is not an e-mail address");
        }
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c <= ' ' || c > '~') {
                throw new ResponseStatusException(400, "That is not an e-mail address");
            }
            if (c >= 'A' && c <= 'Z') {
                chars[i] = (char) (c + ('a' - 'A'));
            }
        }
        return new String(chars);
    }

    static void password(String value) {
        if (value == null || value.length() < 8) {
            throw new ResponseStatusException(400, "Choose a password of at least 8 characters");
        }
        if (value.length() > 200) {
            throw new ResponseStatusException(400, "That password is too long");
        }
    }

    /// A player name: 3 to 20 characters, shown to opponents, so control
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

    static List<String> names(List<Profile> list) {
        List<String> out = new ArrayList<String>();
        for (Profile p : list) {
            out.add(p.getUsername());
        }
        return out;
    }
}
