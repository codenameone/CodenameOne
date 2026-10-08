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
package com.codename1.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.core.userdetails.UserDetails;
import com.codename1.backend.security.core.userdetails.UserDetailsService;
import com.codename1.backend.security.core.userdetails.UsernameNotFoundException;
import com.codename1.backend.security.crypto.BCryptPasswordEncoder;
import com.codename1.backend.security.crypto.DelegatingPasswordEncoder;
import com.codename1.backend.security.crypto.NoOpPasswordEncoder;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.crypto.PasswordEncoderFactories;
import com.codename1.backend.security.crypto.Pbkdf2Sha256PasswordEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// The password encoders, and the provider that uses them at sign-in. bcrypt is
/// written by hand in this runtime, so it is held to vectors other
/// implementations published.
class PasswordEncoderTest {
    /// {password, expected hash}: the test vectors of the jBCrypt distribution,
    /// which Spring's BCrypt carries too.
    private static final String[][] JBCRYPT = {
        {"", "$2a$06$DCq7YPn5Rq63x1Lad4cll.TV4S6ytwfsfvkgY8jIucDrjc8deX1s."},
        {"", "$2a$08$HqWuK6/Ng6sg9gQzbLrgb.Tl.ZHfXLhvt/SgVyWhQqgqcZ7ZuUtye"},
        {"a", "$2a$06$m0CrhHm10qJ3lXRY.5zDGO3rS2KdeeWLuGmsfGlMfOxih58VYVfxe"},
        {"a", "$2a$08$cfcvVd2aQ8CMvoMpP2EBfeodLEkkFJ9umNEfPD18.hUF62qqlC/V."},
        {"abc", "$2a$06$If6bvum7DFjUnE9p2uDeDu0YHzrHM6tf.iqN8.yx.jNN1ILEf7h0i"},
        {"abc", "$2a$10$WvvTPHKwdBJ3uk0Z37EMR.hLA2W6N9AEBhEgrAOljy2Ae5MtaSIUi"},
        {"abcdefghijklmnopqrstuvwxyz",
            "$2a$06$.rCVZVOThsIa97pEDOxvGuRRgzG64bvtJ0938xuqzv18d3ZpQhstC"},
        {"~!@#$%^&*()      ~!@#$%^&*()PNBFRD",
            "$2a$06$fPIsBO8qRqkjj273rfaOI.HtSV9jLDpTbZn782DC6/t7qT67P6FfO"},
    };

    /// {password, expected hash}: the vectors of Openwall's crypt_blowfish, the
    /// implementation glibc-based systems and PHP use.
    private static final String[][] OPENWALL = {
        {"U*U", "$2a$05$CCCCCCCCCCCCCCCCCCCCC.E5YPO9kmyuRGyh0XouQYb4YMJKvyOeW"},
        {"U*U*", "$2a$05$CCCCCCCCCCCCCCCCCCCCC.VGOzA784oUp/Z0DY336zx7pLYAy0lwK"},
        {"U*U*U", "$2a$05$XXXXXXXXXXXXXXXXXXXXXOAcXxm9kjPGEMsLznoKqmqw7tc8WCx4a"},
        {"", "$2a$05$CCCCCCCCCCCCCCCCCCCCC.7uG0VCzI2bS7j6ymqJi9CdcdxiRTWNy"},
        {"0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            + "chars after 72 are ignored",
            "$2a$05$abcdefghijklmnopqrstuu5s2v8.iXieOjg/.AySBTTZIIVFJeBui"},
    };

    @AfterEach
    void productionProfile() {
        NoOpPasswordEncoder.setDevelopmentProfile(false);
    }

    @Test
    @DisplayName("bcrypt reproduces the published jBCrypt and Openwall vectors")
    void bcryptMatchesPublishedVectors() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        for (String[][] set : new String[][][] {JBCRYPT, OPENWALL}) {
            for (String[] vector : set) {
                assertTrue(encoder.matches(vector[0], vector[1]),
                        "\"" + vector[0] + "\" did not verify against " + vector[1]);
                // And not by accident of a comparison that accepts anything.
                assertFalse(encoder.matches("x" + vector[0], vector[1]),
                        "a different password verified against " + vector[1]);
            }
        }
    }

    @Test
    @DisplayName("bcrypt reads $2a$, $2b$ and $2y$, and a hash Apache htpasswd made")
    void bcryptReadsEveryCurrentPrefix() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String tail = "$10$WvvTPHKwdBJ3uk0Z37EMR.hLA2W6N9AEBhEgrAOljy2Ae5MtaSIUi";
        assertTrue(encoder.matches("abc", "$2a" + tail));
        assertTrue(encoder.matches("abc", "$2b" + tail));
        assertTrue(encoder.matches("abc", "$2y" + tail));
        // htpasswd -bnBC 6 "" "correct horse battery staple", and cost 4 of "".
        assertTrue(encoder.matches("correct horse battery staple",
                "$2y$06$3uzYgUPn.Q58VVTCZ4hYf.6Q/CeyTie2n8Khwccz/lupTc661fZcy"));
        assertTrue(encoder.matches("",
                "$2y$04$nlM2p8nQnF/9PXX7GUVtpudPsDCwbQeU76hKghrLU7dfqvlieGaNq"));
        assertFalse(encoder.matches("correct horse battery stapler",
                "$2y$06$3uzYgUPn.Q58VVTCZ4hYf.6Q/CeyTie2n8Khwccz/lupTc661fZcy"));
    }

    @Test
    @DisplayName("bcrypt hashes with a fresh salt, at its cost, and refuses what is not bcrypt")
    void bcryptEncodes() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
        String first = encoder.encode("s3cret");
        String second = encoder.encode("s3cret");
        assertTrue(first.startsWith("$2a$04$"), first);
        assertEquals(60, first.length());
        assertNotEquals(first, second, "two hashes of one password shared a salt");
        assertTrue(encoder.matches("s3cret", first));
        assertTrue(encoder.matches("s3cret", second));
        assertFalse(encoder.matches("S3cret", first));
        // Non-ASCII takes part as UTF-8.
        String accented = encoder.encode("p\u00e4ssw\u00f6rd");
        assertTrue(encoder.matches("p\u00e4ssw\u00f6rd", accented));
        assertFalse(encoder.matches("passwo\u0308rd", accented));

        assertFalse(encoder.matches("s3cret", null));
        assertFalse(encoder.matches("s3cret", ""));
        assertFalse(encoder.matches("s3cret", "s3cret"));
        assertFalse(encoder.matches("s3cret", "$2x" + first.substring(3)), "the defective $2x$");
        assertFalse(encoder.matches("s3cret", first.substring(0, 59)));
        assertFalse(encoder.matches("s3cret", "$2a$03" + first.substring(6)), "a cost below 4");
        assertFalse(encoder.matches("s3cret", first.substring(0, 30) + "!" + first.substring(31)));

        assertFalse(encoder.upgradeEncoding(first));
        assertTrue(new BCryptPasswordEncoder(5).upgradeEncoding(first));
        assertEquals("Bad strength", assertThrows(IllegalArgumentException.class,
                () -> new BCryptPasswordEncoder(3)).getMessage());
        assertTrue(new BCryptPasswordEncoder('y', 4).encode("x").startsWith("$2y$04$"));
    }

    @Test
    @DisplayName("pbkdf2-sha256 verifies, salts, and asks to be upgraded below today's rounds")
    void pbkdf2() throws Exception {
        Pbkdf2Sha256PasswordEncoder encoder = new Pbkdf2Sha256PasswordEncoder();
        String stored = encoder.encode("s3cret");
        assertTrue(stored.startsWith("pbkdf2$" + Crypto.PASSWORD_ITERATIONS + "$"), stored);
        assertNotEquals(stored, encoder.encode("s3cret"));
        assertTrue(encoder.matches("s3cret", stored));
        assertFalse(encoder.matches("s3cres", stored));
        assertFalse(encoder.matches("s3cret", ""));
        assertFalse(encoder.upgradeEncoding(stored));

        // A hash made when fewer rounds were the rule still verifies, and says so.
        byte[] salt = "0123456789abcdef".getBytes("UTF-8");
        byte[] key = Crypto.pbkdf2Sha256("s3cret".getBytes("UTF-8"), salt, 1000, 32);
        String old = "pbkdf2$1000$" + Base64Url.encode(salt) + "$" + Base64Url.encode(key);
        assertTrue(encoder.matches("s3cret", old));
        assertTrue(encoder.upgradeEncoding(old));
    }

    @Test
    @DisplayName("the delegating encoder reads the {id}, encodes with pbkdf2-sha256, and names what it cannot read")
    void delegating() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        String stored = encoder.encode("s3cret");
        assertTrue(stored.startsWith("{pbkdf2-sha256}pbkdf2$"), stored);
        assertTrue(encoder.matches("s3cret", stored));
        assertFalse(encoder.matches("wrong", stored));
        assertFalse(encoder.upgradeEncoding(stored));

        String bcrypt = "{bcrypt}$2a$10$WvvTPHKwdBJ3uk0Z37EMR.hLA2W6N9AEBhEgrAOljy2Ae5MtaSIUi";
        assertTrue(encoder.matches("abc", bcrypt));
        assertFalse(encoder.matches("abd", bcrypt));
        assertTrue(encoder.upgradeEncoding(bcrypt), "a bcrypt hash is not today's encoding");

        IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
                () -> encoder.matches("x", "{argon2}whatever"));
        assertEquals("There is no PasswordEncoder mapped for the id \"argon2\"",
                unknown.getMessage());
        IllegalArgumentException bare = assertThrows(IllegalArgumentException.class,
                () -> encoder.matches("x", "$2a$10$WvvTPHKwdBJ3uk0Z37EMR.hLA2W6N9AEBhEgrAOljy2Ae5MtaSIUi"));
        assertTrue(bare.getMessage().contains("You have entered a password with no "
                + "PasswordEncoder"), bare.getMessage());

        // A store that predates the prefixes names its scheme once.
        Map<String, PasswordEncoder> schemes = new LinkedHashMap<String, PasswordEncoder>();
        schemes.put("pbkdf2-sha256", new Pbkdf2Sha256PasswordEncoder());
        DelegatingPasswordEncoder legacy = new DelegatingPasswordEncoder("pbkdf2-sha256", schemes);
        legacy.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
        assertTrue(legacy.matches("abc",
                "$2a$10$WvvTPHKwdBJ3uk0Z37EMR.hLA2W6N9AEBhEgrAOljy2Ae5MtaSIUi"));
        assertTrue(legacy.upgradeEncoding(
                "$2a$10$WvvTPHKwdBJ3uk0Z37EMR.hLA2W6N9AEBhEgrAOljy2Ae5MtaSIUi"));

        assertEquals("idForEncode nope is not found in idToPasswordEncoder [pbkdf2-sha256]",
                assertThrows(IllegalArgumentException.class,
                        () -> new DelegatingPasswordEncoder("nope", schemes)).getMessage());
    }

    @Test
    @DisplayName("{noop} verifies on a development profile and nowhere else")
    void noopIsForDevelopment() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        NoOpPasswordEncoder.setDevelopmentProfile(false);
        assertFalse(encoder.matches("secret", "{noop}secret"),
                "a clear-text password verified outside a development profile");
        NoOpPasswordEncoder.setDevelopmentProfile(true);
        assertTrue(encoder.matches("secret", "{noop}secret"));
        assertFalse(encoder.matches("Secret", "{noop}secret"));
        assertTrue(encoder.upgradeEncoding("{noop}secret"));
    }

    /// An encoder that records what it was asked to compare.
    private static final class Counting implements PasswordEncoder {
        final List<String> compared = new ArrayList<String>();

        @Override
        public String encode(CharSequence rawPassword) {
            return "enc:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            compared.add(rawPassword + " vs " + encodedPassword);
            return encodedPassword.equals("enc:" + rawPassword);
        }
    }

    @Test
    @DisplayName("an unknown user still costs one comparison, and is reported as bad credentials")
    void anUnknownUserIsNotTold() {
        Counting encoder = new Counting();
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(
                new InMemoryUserDetailsManager(User.withUsername("ada").password("enc:pw")
                        .roles("USER").build()));
        provider.setPasswordEncoder(encoder);

        BadCredentialsException missing = assertThrows(BadCredentialsException.class,
                () -> provider.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated("nobody", "pw")));
        assertEquals("Bad credentials", missing.getMessage());
        assertEquals("[pw vs enc:userNotFoundPassword]", encoder.compared.toString());

        encoder.compared.clear();
        BadCredentialsException wrong = assertThrows(BadCredentialsException.class,
                () -> provider.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated("ada", "nope")));
        assertEquals("Bad credentials", wrong.getMessage());
        assertEquals(1, encoder.compared.size());

        provider.setHideUserNotFoundExceptions(false);
        assertThrows(UsernameNotFoundException.class, () -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("nobody", "pw")));

        Authentication accepted = provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("ADA", "pw"));
        assertTrue(accepted.isAuthenticated());
        assertEquals("ada", accepted.getName());
        assertEquals("[ROLE_USER]", accepted.getAuthorities().toString());
        assertTrue(accepted.getPrincipal() instanceof UserDetails);
    }

    @Test
    @DisplayName("a password in an older encoding is stored again in today's on sign-in")
    void aSignInUpgradesTheStoredPassword() {
        NoOpPasswordEncoder.setDevelopmentProfile(true);
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{bcrypt}" + new BCryptPasswordEncoder(4)
                        .encode("s3cret")).roles("USER").build(),
                User.withUsername("bob").password("{noop}hunter2").roles("USER").build());
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setUserDetailsPasswordService(users);
        ProviderManager manager = new ProviderManager(provider);

        // A refused sign-in upgrades nothing.
        assertThrows(BadCredentialsException.class, () -> manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("ada", "wrong")));
        assertTrue(users.loadUserByUsername("ada").getPassword().startsWith("{bcrypt}"));

        Authentication ada = manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("ada", "s3cret"));
        String upgraded = users.loadUserByUsername("ada").getPassword();
        assertTrue(upgraded.startsWith("{pbkdf2-sha256}pbkdf2$"), upgraded);
        // The accepted token no longer carries the password, in either place.
        assertEquals(null, ada.getCredentials());
        assertEquals(null, ((UserDetails) ada.getPrincipal()).getPassword());
        // And the new hash is of the same password.
        manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("ada", "s3cret"));
        assertEquals(upgraded, users.loadUserByUsername("ada").getPassword(),
                "an up-to-date password was stored again");

        manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("bob", "hunter2"));
        assertTrue(users.loadUserByUsername("bob").getPassword().startsWith("{pbkdf2-sha256}"));

        // Without somewhere to store it, the old hash stays and still verifies.
        InMemoryUserDetailsManager fixed = new InMemoryUserDetailsManager(
                User.withUsername("cy").password("{noop}pw").roles("USER").build());
        DaoAuthenticationProvider plain = new DaoAuthenticationProvider(fixed);
        plain.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("cy", "pw"));
        assertEquals("{noop}pw", fixed.loadUserByUsername("cy").getPassword());
    }

    @Test
    @DisplayName("an account that may not be used is refused by what is wrong with it")
    void accountStates() {
        NoOpPasswordEncoder.setDevelopmentProfile(true);
        UserDetailsService users = new InMemoryUserDetailsManager(
                User.withUsername("locked").password("{noop}pw").roles("USER")
                        .accountLocked(true).build(),
                User.withUsername("disabled").password("{noop}pw").roles("USER")
                        .disabled(true).build(),
                User.withUsername("expired").password("{noop}pw").roles("USER")
                        .accountExpired(true).build(),
                User.withUsername("stale").password("{noop}pw").roles("USER")
                        .credentialsExpired(true).build());
        ProviderManager manager = new ProviderManager(new DaoAuthenticationProvider(users));
        assertEquals("User account is locked", assertThrows(LockedException.class,
                () -> manager.authenticate(UsernamePasswordAuthenticationToken
                        .unauthenticated("locked", "pw"))).getMessage());
        assertEquals("User is disabled", assertThrows(DisabledException.class,
                () -> manager.authenticate(UsernamePasswordAuthenticationToken
                        .unauthenticated("disabled", "pw"))).getMessage());
        assertEquals("User account has expired", assertThrows(AccountExpiredException.class,
                () -> manager.authenticate(UsernamePasswordAuthenticationToken
                        .unauthenticated("expired", "pw"))).getMessage());
        assertEquals("User credentials have expired",
                assertThrows(CredentialsExpiredException.class,
                        () -> manager.authenticate(UsernamePasswordAuthenticationToken
                                .unauthenticated("stale", "pw"))).getMessage());
        // The expiry of a password is told only to someone who knows it.
        assertEquals("Bad credentials", assertThrows(BadCredentialsException.class,
                () -> manager.authenticate(UsernamePasswordAuthenticationToken
                        .unauthenticated("stale", "wrong"))).getMessage());

        assertEquals("ROLE_ADMIN cannot start with ROLE_ (it is automatically added)",
                assertThrows(IllegalArgumentException.class,
                        () -> User.withUsername("x").password("y").roles("ROLE_ADMIN"))
                        .getMessage());
    }
}
