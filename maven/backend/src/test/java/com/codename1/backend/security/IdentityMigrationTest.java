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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Config;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.crypto.BCryptPasswordEncoder;
import com.codename1.backend.security.crypto.DelegatingPasswordEncoder;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.crypto.KeyFiles;
import com.codename1.backend.security.crypto.KeyFixtures;
import com.codename1.backend.security.crypto.ModularPbkdf2PasswordEncoder;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.crypto.Pbkdf2Sha256PasswordEncoder;
import com.codename1.backend.security.mfa.InMemoryTotpRepository;
import com.codename1.backend.security.migration.IdentityMigration;
import com.codename1.backend.security.migration.MigrationReport;
import com.codename1.backend.security.oauth2.client.InMemoryFederatedIdentityRepository;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtEncoder;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import com.codename1.backend.security.oauth2.jwt.JwtClaimsSet;
import com.codename1.backend.security.oauth2.jwt.JwtEncoderParameters;
import com.codename1.backend.security.oauth2.server.authorization.AuthorizationServerKeys;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/// Moving another identity store in (#6000): hashes that verify as they were
/// written, subjects and kids that do not change, and a report of the rest.
class IdentityMigrationTest {
    // Vectors computed with Python's hashlib, an implementation independent of ours.
    private static final String PASSWORD = "correct horse";
    private static final String DJANGO =
            "pbkdf2_sha256$1000$seasalt$mQnueSakb748zqBAC1tmWVZsZbi2zPGZarEzTGdfmso=";
    private static final String DJANGO_SHA1 =
            "pbkdf2_sha1$1000$seasalt$iQvkNOF1wEL4Khh8eogJ8rUhipM=";
    private static final String PASSLIB = "$pbkdf2-sha256$1000$AAECAwQFBgcICQoLDA0ODw$"
            + "yRTMTwbMbo9G0VfjobWqerzuuxe7BETNTErBbKKumGQ";
    private static final String PASSLIB_512 = "$pbkdf2-sha512$1000$AAECAwQFBgcICQoLDA0ODw$"
            + "Xpx07WjVx4vCIvrmBRj8uOoVVtGqJqtUv2J5bhizSQs7osCteF7W4A61dZDqSIqQjO.dxO6p5FT/Uy7QRB"
            + "XSXA";
    private static final String WERKZEUG = "pbkdf2:sha256:1000$NaCl1234$09648d88c553b8723dc823d"
            + "15e17163d3110a5d75b040381a3557783e98e82ef";

    @TempDir
    File dir;

    private static DelegatingPasswordEncoder passwords() {
        Map<String, PasswordEncoder> encoders = new LinkedHashMap<String, PasswordEncoder>();
        encoders.put("pbkdf2-sha256", new Pbkdf2Sha256PasswordEncoder());
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put("django", new ModularPbkdf2PasswordEncoder(
                ModularPbkdf2PasswordEncoder.Format.DJANGO));
        encoders.put("passlib", new ModularPbkdf2PasswordEncoder(
                ModularPbkdf2PasswordEncoder.Format.PASSLIB));
        encoders.put("werkzeug", new ModularPbkdf2PasswordEncoder(
                ModularPbkdf2PasswordEncoder.Format.WERKZEUG));
        return new DelegatingPasswordEncoder("pbkdf2-sha256", encoders);
    }

    @Test
    @DisplayName("Django, passlib and Werkzeug hashes verify as those frameworks wrote them")
    void modularFormats() {
        String[][] cases = {{"DJANGO", DJANGO}, {"DJANGO", DJANGO_SHA1}, {"PASSLIB", PASSLIB},
            {"PASSLIB", PASSLIB_512}, {"WERKZEUG", WERKZEUG}};
        for (String[] c : cases) {
            ModularPbkdf2PasswordEncoder encoder = new ModularPbkdf2PasswordEncoder(
                    ModularPbkdf2PasswordEncoder.Format.valueOf(c[0]));
            assertTrue(encoder.matches(PASSWORD, c[1]), c[1]);
            assertFalse(encoder.matches("correct horsE", c[1]), c[1]);
            assertTrue(encoder.isReadable(c[1]));
            assertTrue(encoder.upgradeEncoding(c[1]));
            // Its own output reads back.
            encoder.setEncodeRounds(1000);
            String fresh = encoder.encode(PASSWORD);
            assertTrue(encoder.matches(PASSWORD, fresh), fresh);
        }
        ModularPbkdf2PasswordEncoder django = new ModularPbkdf2PasswordEncoder(
                ModularPbkdf2PasswordEncoder.Format.DJANGO);
        // Another format, an unknown digest, a work factor past the bound.
        assertFalse(django.isReadable(WERKZEUG));
        assertFalse(django.matches(PASSWORD, "pbkdf2_md5$1000$seasalt$AAAA"));
        django.setMaxRounds(999);
        assertFalse(django.matches(PASSWORD, DJANGO));
        assertFalse(new ModularPbkdf2PasswordEncoder(ModularPbkdf2PasswordEncoder.Format.WERKZEUG)
                .matches(PASSWORD, "pbkdf2:sha256$NaCl1234$09648d"));
    }

    @Test
    @DisplayName("Imported users sign in with their old passwords, are re-hashed, and keep their names")
    void usersEndToEnd() throws Exception {
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager();
        DelegatingPasswordEncoder passwords = passwords();
        InMemoryFederatedIdentityRepository links = new InMemoryFederatedIdentityRepository();
        IdentityMigration migration = new IdentityMigration(users, passwords);
        migration.setFederatedIdentities(links);
        migration.setPasswordPrefix("django");
        migration.user("Ada.Lovelace@Example.com", DJANGO, Arrays.asList("ROLE_ADMIN", "billing"),
                true);
        migration.user("grace", "{werkzeug}" + WERKZEUG, Arrays.asList("ROLE_USER"), true);
        migration.user("old", "{md4}0123", Arrays.asList("ROLE_USER"), true);
        migration.user("disabled", DJANGO, null, false);
        migration.user("grace", DJANGO, null, true);
        migration.user("social", null, null, true);
        migration.federatedIdentity("google", "1093847561", "social");
        migration.federatedIdentity("google", "1093847561", "grace");
        migration.federatedIdentity("github", "77", "nobody");
        MigrationReport report = migration.finish();

        assertEquals(4, report.count(MigrationReport.Outcome.MIGRATED), report.toString());
        assertEquals(2, report.count(MigrationReport.Outcome.NEEDS_ACTION), report.toString());
        assertEquals(3, report.count(MigrationReport.Outcome.SKIPPED), report.toString());
        assertEquals(5, report.count(MigrationReport.Outcome.NOT_CARRIED));
        assertFalse(report.isComplete());
        assertTrue(report.toString().contains("old: the password hash is in a format"),
                report.toString());
        // Nothing secret is in the report.
        assertFalse(report.toString().contains("seasalt"));

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(passwords);
        provider.setUserDetailsPasswordService(users);
        ProviderManager manager = new ProviderManager(provider);
        Authentication ada = manager.authenticate(UsernamePasswordAuthenticationToken
                .unauthenticated("Ada.Lovelace@Example.com", PASSWORD));
        assertEquals("Ada.Lovelace@Example.com", ada.getName());
        assertTrue(ada.getAuthorities().toString().contains("ROLE_ADMIN"));
        assertTrue(ada.getAuthorities().toString().contains("billing"));
        assertTrue(users.loadUserByUsername("Ada.Lovelace@Example.com").getPassword()
                .startsWith("{pbkdf2-sha256}"));
        manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated("grace", PASSWORD));
        assertThrows(AuthenticationException.class, () -> manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("disabled", PASSWORD)));
        assertThrows(AuthenticationException.class, () -> manager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("old", "0123")));
        assertEquals("social", links.findUsername("google", "1093847561"));

        // A second run over the same rows writes nothing and skips them all.
        IdentityMigration again = new IdentityMigration(users, passwords);
        again.user("grace", DJANGO, null, true);
        assertEquals(1, again.finish().count(MigrationReport.Outcome.SKIPPED));
    }

    @Test
    @DisplayName("A dry run reports what a run would do and writes nothing")
    void dryRun() {
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager();
        IdentityMigration migration = new IdentityMigration(users, passwords());
        migration.setDryRun(true);
        InMemoryTotpRepository totp = new InMemoryTotpRepository();
        migration.setTotp(totp);
        migration.user("ada", "{django}" + DJANGO, null, true);
        migration.totp("ada", new byte[20]);
        migration.totp("ada", new byte[4]);
        MigrationReport report = migration.finish();
        assertTrue(report.isDryRun());
        assertEquals(2, report.count(MigrationReport.Outcome.MIGRATED), report.toString());
        assertEquals(1, report.count(MigrationReport.Outcome.NEEDS_ACTION));
        assertFalse(users.userExists("ada"));
        assertEquals(null, totp.find("ada"));
        assertThrows(IllegalStateException.class, () -> {
            IdentityMigration late = new IdentityMigration(users, passwords());
            late.user("x", null, null, true);
            late.setDryRun(true);
        });
    }

    @Test
    @DisplayName("TOTP secrets move confirmed, so the authenticator app keeps working")
    void totp() {
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager();
        InMemoryTotpRepository totp = new InMemoryTotpRepository();
        IdentityMigration migration = new IdentityMigration(users, passwords());
        migration.setTotp(totp);
        migration.user("ada", "{django}" + DJANGO, null, true);
        byte[] secret = "12345678901234567890".getBytes();
        migration.totp("ada", secret);
        migration.totp("nobody", secret);
        MigrationReport report = migration.finish();
        assertEquals(1, report.count(MigrationReport.Outcome.SKIPPED));
        assertNotNull(totp.find("ada"));
        assertTrue(totp.find("ada").isConfirmed());
        assertTrue(Arrays.equals(secret, totp.find("ada").getSecret()));
    }

    @Test
    @DisplayName("Signing keys keep their kid, and a retired key still verifies what it signed")
    void signingKeysKeepTheirKid() throws Exception {
        // The old server signed with this key, under its own kid.
        Jwk oldKey = Jwk.ofPrivateKey(KeyFiles.privateKey(KeyFixtures.RSA_PKCS8_PEM))
                .withKeyId("legacy-2024");
        long now = System.currentTimeMillis() / 1000L;
        String oldToken = new DefaultJwtEncoder(JwkSet.of(new Jwk[] {oldKey})).encode(
                JwtEncoderParameters.from(JwtClaimsSet.builder().subject("ada").expiresAt(now + 300)
                        .build())).getTokenValue();

        File rsa = new File(dir, "rsa.pem");
        Files.write(rsa.toPath(), KeyFixtures.RSA_PKCS8_PEM.getBytes("US-ASCII"));
        Properties p = new Properties();
        p.setProperty(AuthorizationServerKeys.KEYS, rsa.getPath());
        p.setProperty(AuthorizationServerKeys.KEY_IDS, "legacy-2024");
        List<Jwk> keys = AuthorizationServerKeys.load(Config.of(p, "prod")).getKeys();
        assertEquals("legacy-2024", keys.get(0).getKeyId());
        Jwt decoded = DefaultJwtDecoder.withJwkSource(AuthorizationServerKeys.of(keys)).build()
                .decode(oldToken);
        assertEquals("ada", decoded.getSubject());

        // Rotated: a new key signs, the old one's public JWK Set only verifies.
        File ec = new File(dir, "ec.pem");
        Files.write(ec.toPath(), KeyFixtures.EC256_PKCS8_PEM.getBytes("US-ASCII"));
        File oldSet = new File(dir, "old-jwks.json");
        Files.write(oldSet.toPath(), ("{\"keys\":[" + publicJwk(oldKey) + "]}").getBytes("UTF-8"));
        Properties rotated = new Properties();
        rotated.setProperty(AuthorizationServerKeys.KEYS, ec.getPath());
        rotated.setProperty(AuthorizationServerKeys.VERIFICATION_KEYS, oldSet.getPath());
        List<Jwk> after = AuthorizationServerKeys.load(Config.of(rotated, "prod")).getKeys();
        assertEquals(2, after.size());
        assertTrue(after.get(0).isPrivate());
        assertFalse(after.get(1).isPrivate());
        assertEquals("legacy-2024", after.get(1).getKeyId());
        assertEquals("ada", DefaultJwtDecoder.withJwkSource(AuthorizationServerKeys.of(after))
                .jwsAlgorithms(new com.codename1.backend.security.oauth2.jose.jws.JwsAlgorithm[] {
                    com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm.RS256,
                    com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm.ES256})
                .build().decode(oldToken).getSubject());
        // A private key is not a verification key.
        rotated.setProperty(AuthorizationServerKeys.VERIFICATION_KEYS, rsa.getPath());
        assertThrows(IllegalStateException.class,
                () -> AuthorizationServerKeys.load(Config.of(rotated, "prod")));
        // A key the set declares for encryption never verifies.
        Files.write(oldSet.toPath(), ("{\"keys\":[" + publicJwk(oldKey.withUse("enc")) + "]}")
                .getBytes("UTF-8"));
        rotated.setProperty(AuthorizationServerKeys.VERIFICATION_KEYS, oldSet.getPath());
        assertEquals(1, AuthorizationServerKeys.load(Config.of(rotated, "prod")).getKeys().size());
    }

    @Test
    @DisplayName("A name that differs only in case attaches nothing to the other account")
    void caseCollision() {
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager();
        InMemoryFederatedIdentityRepository links = new InMemoryFederatedIdentityRepository();
        InMemoryTotpRepository totp = new InMemoryTotpRepository();
        IdentityMigration migration = new IdentityMigration(users, passwords());
        migration.setFederatedIdentities(links);
        migration.setTotp(totp);
        migration.user("admin", "{django}" + DJANGO, Arrays.asList("ROLE_ADMIN"), true);
        migration.user("Admin", "{django}" + DJANGO, Arrays.asList("ROLE_USER"), true);
        migration.federatedIdentity("google", "attacker", "Admin");
        migration.totp("Admin", "12345678901234567890".getBytes());
        MigrationReport report = migration.finish();
        assertEquals(1, report.count(MigrationReport.Outcome.MIGRATED), report.toString());
        assertEquals(3, report.count(MigrationReport.Outcome.SKIPPED), report.toString());
        assertTrue(report.toString().contains("differs only in case"), report.toString());
        assertEquals(null, links.findUsername("google", "attacker"));
        assertEquals(null, totp.find("admin"));

        // The same against a user that was already there.
        IdentityMigration later = new IdentityMigration(users, passwords());
        later.setFederatedIdentities(links);
        later.federatedIdentity("google", "attacker", "ADMIN");
        assertEquals(1, later.finish().count(MigrationReport.Outcome.SKIPPED));
        assertEquals(null, links.findUsername("google", "attacker"));

        // And in a dry run, where nothing is created to collide with.
        IdentityMigration dry = new IdentityMigration(new InMemoryUserDetailsManager(), passwords());
        dry.setDryRun(true);
        dry.user("admin", null, null, true);
        dry.user("Admin", null, null, true);
        assertEquals(1, dry.finish().count(MigrationReport.Outcome.SKIPPED));
    }

    @Test
    @DisplayName("A hash in a format the id's encoder cannot read needs a reset, not a silent import")
    void unreadableFormatUnderAKnownId() {
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager();
        IdentityMigration migration = new IdentityMigration(users, passwords());
        migration.setPasswordPrefix("werkzeug");
        migration.user("flask", "scrypt:32768:8:1$salt$00ff", null, true);
        migration.user("django", "{django}argon2$argon2id$v=19$m=102400,t=2,p=8$c2FsdA$aGFzaA", null,
                true);
        MigrationReport report = migration.finish();
        assertEquals(2, report.count(MigrationReport.Outcome.NEEDS_ACTION), report.toString());
    }

    private static String publicJwk(Jwk key) {
        return com.codename1.backend.Json.write(key.toPublicJson());
    }
}
