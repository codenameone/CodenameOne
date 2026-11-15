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
package com.codenameone.examples.brickyard.account;

import com.codename1.appkit.server.Text;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Roles;
import com.codenameone.examples.brickyard.api.AccountDto;
import com.codenameone.examples.brickyard.api.FriendDto;
import com.codenameone.examples.brickyard.api.RegisterDto;
import com.codenameone.examples.brickyard.domain.Account;
import com.codenameone.examples.brickyard.domain.Friend;
import com.codenameone.examples.brickyard.repository.AccountRepository;
import com.codenameone.examples.brickyard.repository.FriendRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/// Accounts: the sign-in record (users and authorities, owned by the security
/// layer) plus the [Account] row with the name, the birth year and the Bricks.
@Component
public class Accounts {
    public static final int START_BRICKS = 25;
    /// The youngest a player may be to open an account.
    public static final int MINIMUM_AGE = 13;

    private final AccountRepository accounts;
    private final FriendRepository friends;
    private final JdbcUserDetailsManager users;
    private final PasswordEncoder encoder;

    public Accounts(AccountRepository accounts, FriendRepository friends,
            JdbcUserDetailsManager users, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.friends = friends;
        this.users = users;
        this.encoder = encoder;
    }

    public AccountDto register(RegisterDto request) throws IOException {
        if (request == null) {
            throw new ResponseStatusException(400, "Nothing to register");
        }
        String username = email(request.email());
        password(request.password());
        String name = displayName(request.displayName());
        int birthYear = birthYear(request.birthYear());
        if (users.userExists(username)) {
            throw new ResponseStatusException(409, "That e-mail already has an account");
        }
        try {
            create(username, request.password(), name, birthYear, new String[] {Roles.USER});
        } catch (IOException | RuntimeException failed) {
            if (users.userExists(username)) {
                throw new ResponseStatusException(409, "That e-mail already has an account");
            }
            throw failed;
        }
        return describe(username);
    }

    @Transactional(rollbackFor = IOException.class)
    public void create(String username, String password, String name, int birthYear, String[] roles)
            throws IOException {
        users.createUser(User.withUsername(username).password(encoder.encode(password))
                .roles(roles).build());
        Account a = new Account();
        a.setUsername(username);
        a.setDisplayName(name);
        a.setBirthYear(birthYear);
        a.setBricks(START_BRICKS);
        a.setCreatedAt(Clock.now());
        accounts.insert(a);
    }

    public boolean exists(String username) {
        return users.userExists(username);
    }

    @Transactional(readOnly = true)
    public AccountDto describe(String username) {
        return toDto(account(username));
    }

    @Transactional(rollbackFor = IOException.class)
    public AccountDto rename(String username, String displayName) {
        Account a = account(username);
        a.setDisplayName(displayName(displayName));
        accounts.save(a);
        return toDto(a);
    }

    /// Adds CREATOR to the account's roles: what lets it open experiences.
    public AccountDto becomeCreator(String username) {
        Account a = account(username);
        UserDetails signIn = users.loadUserByUsername(username);
        List<String> roles = roles(signIn);
        if (!roles.contains(Roles.CREATOR)) {
            roles.add(Roles.CREATOR);
            users.updateUser(User.withUserDetails(signIn).roles(roles.toArray(new String[0])).build());
        }
        return toDto(a);
    }

    /// Friends are mutual and immediate in this template: adding one writes both directions.
    @Transactional(rollbackFor = IOException.class)
    public FriendDto addFriend(String username, String friend) {
        account(username);
        String other = email(friend == null ? null : friend);
        if (other.equals(username) || accounts.findById(other) == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        link(username, other);
        link(other, username);
        return new FriendDto(other);
    }

    private void link(String a, String b) {
        String id = a + "|" + b;
        if (friends.findById(id) == null) {
            Friend f = new Friend();
            f.setId(id);
            f.setUsername(a);
            f.setFriend(b);
            friends.insert(f);
        }
    }

    @Transactional(readOnly = true)
    public List<String> friendsOf(String username) {
        List<String> out = new ArrayList<String>();
        for (Friend f : friends.of(username)) {
            out.add(f.getFriend());
        }
        return out;
    }

    public List<FriendDto> friendList(String username) {
        List<FriendDto> out = new ArrayList<FriendDto>();
        for (String f : friendsOf(username)) {
            out.add(new FriendDto(f));
        }
        return out;
    }

    /// The account's age this year, at most a year high: the gate errs towards
    /// letting a birthday that has not come yet count.
    public int age(String username) {
        return Clock.yearOf(Clock.now()) - account(username).getBirthYear();
    }

    @Transactional(readOnly = true)
    public Account account(String username) {
        Account a = username == null ? null : accounts.findById(username);
        if (a == null || !users.userExists(username)) {
            throw new ResponseStatusException(404, "No such account");
        }
        return a;
    }

    AccountDto toDto(Account a) {
        return new AccountDto(a.getUsername(), a.getDisplayName(), a.getBirthYear(),
                roles(users.loadUserByUsername(a.getUsername())), a.getBricks());
    }

    private static List<String> roles(UserDetails signIn) {
        List<String> out = new ArrayList<String>();
        Iterator<? extends GrantedAuthority> it = signIn.getAuthorities().iterator();
        while (it.hasNext()) {
            String authority = it.next().getAuthority();
            out.add(authority.startsWith("ROLE_") ? authority.substring(5) : authority);
        }
        return out;
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

    /// The age gate: a birth year that makes the player younger than
    /// [#MINIMUM_AGE] is refused, and so is one that cannot be real.
    static int birthYear(int year) {
        int now = Clock.yearOf(Clock.now());
        if (year < now - 120 || year > now) {
            throw new ResponseStatusException(400, "That birth year cannot be right");
        }
        if (now - year < MINIMUM_AGE) {
            throw new ResponseStatusException(403, "Players need to be at least "
                    + MINIMUM_AGE + " to open an account");
        }
        return year;
    }

    static String displayName(String value) {
        String name = Text.required(value, 20, "your display name").trim();
        if (name.length() < 3) {
            throw new ResponseStatusException(400, "A display name needs at least 3 characters");
        }
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) < ' ') {
                throw new ResponseStatusException(400, "That name has characters we cannot show");
            }
        }
        return name;
    }
}
