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
package com.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.codename1.backend.Base64;
import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.Json;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.crypto.Der;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.crypto.KeyFiles;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codename1.backend.security.crypto.PasswordEncoderFactories;
import com.codename1.backend.security.crypto.SignedTokens;
import com.codename1.backend.security.oauth2.core.DelegatingOAuth2TokenValidator;
import com.codename1.backend.security.oauth2.jose.jws.JwsAlgorithm;
import com.codename1.backend.security.oauth2.jose.jws.MacAlgorithm;
import com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm;
import com.codename1.backend.security.oauth2.jwt.BadJwtException;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtEncoder;
import com.codename1.backend.security.oauth2.jwt.JwsHeader;
import com.codename1.backend.security.oauth2.jwt.Jwt;
import com.codename1.backend.security.oauth2.jwt.JwtClaimsSet;
import com.codename1.backend.security.oauth2.jwt.JwtDecoder;
import com.codename1.backend.security.oauth2.jwt.JwtEncoderParameters;
import com.codename1.backend.security.oauth2.jwt.JwtIssuerValidator;
import com.codename1.backend.security.oauth2.jwt.JwtTimestampValidator;
import com.codename1.security.Base32;
import com.codename1.security.Hash;
import com.codename1.security.Hmac;
import com.codename1.security.Otp;

/**
 * The pure Java half: keys, tokens and one-time passwords built on the primitives.
 *
 * Pure Java is not the same Java on both runtimes either. The translated binary runs it on
 * ParparVM's own class library -- its String, its HashMap, its integer arithmetic -- so what
 * a DER reader or a JSON Web Token comes to is printed and compared like everything else.
 */
final class AuthCheckTokens {
    private static final long NOW = 1700000000L;

    private AuthCheckTokens() {
    }

    static void run() throws Exception {
        keys();
        tokens();
        signedTokens();
        passwords();
        oneTimePasswords();
    }

    private static void keys() throws Exception {
        byte[] rsaPublic = KeyFiles.publicKey(KeyFixtures.RSA_PUBLIC_PEM);
        AuthCheck.check("a PUBLIC KEY block is the DER", KeyFixtures.RSA_PUBLIC_DER,
                Base64.encode(rsaPublic));
        AuthCheck.check("a PRIVATE KEY block is the DER", KeyFixtures.RSA_PKCS8_DER,
                Base64.encode(KeyFiles.privateKey(KeyFixtures.RSA_PKCS8_PEM)));
        AuthCheck.check("an RSA PRIVATE KEY block becomes the same PKCS#8",
                KeyFixtures.RSA_PKCS8_DER, Base64.encode(KeyFiles.privateKey(KeyFixtures.RSA_PKCS1_PEM)));
        byte[] sec1 = KeyFiles.privateKey(KeyFixtures.EC256_SEC1_PEM);
        AuthCheck.value("sec1 as pkcs8", sec1);
        AuthCheck.check("an EC PRIVATE KEY block signs once wrapped", "true", String.valueOf(
                Crypto.verify(Crypto.ES256, Base64.decode(KeyFixtures.EC256_PUBLIC_DER),
                        AuthCheck.ascii("x"), Crypto.sign(Crypto.ES256, sec1, AuthCheck.ascii("x")))));
        AuthCheck.check("the public key comes out of the private one", KeyFixtures.EC384_PUBLIC_DER,
                Base64.encode(Der.publicKeyOf(KeyFiles.privateKey(KeyFixtures.EC384_SEC1_PEM))));
        try {
            KeyFiles.privateKey(KeyFixtures.RSA_ENCRYPTED_PKCS8_PEM);
            AuthCheck.fail("an encrypted key", "was read");
        } catch(java.io.IOException refused) {
            AuthCheck.value("encrypted pem refusal", refused.getMessage());
        }
        try {
            KeyFiles.privateKey(KeyFixtures.RSA_ENCRYPTED_PKCS1_PEM);
            AuthCheck.fail("an encrypted traditional key", "was read");
        } catch(java.io.IOException refused) {
            AuthCheck.check("both encrypted forms are refused alike", "true",
                    String.valueOf(refused.getMessage().startsWith("This private key is encrypted")));
        }

        Jwk rsa = Jwk.ofPem(KeyFixtures.RSA_PKCS1_PEM);
        Jwk p256 = Jwk.ofPem(KeyFixtures.EC256_PKCS8_PEM);
        Jwk p384 = Jwk.ofPem(KeyFixtures.EC384_PUBLIC_PEM);
        AuthCheck.value("rsa kid", rsa.getKeyId());
        AuthCheck.value("p256 kid", p256.getKeyId());
        AuthCheck.value("p384 kid", p384.getKeyId());
        String published = JwkSet.of(rsa, p256.withUse("sig"), p384.withAlgorithm("ES384"),
                Jwk.ofSecret(new byte[32])).toJson();
        AuthCheck.value("jwks", published);
        JwkSet read = JwkSet.parse(published);
        AuthCheck.check("a published set reads back", "3", String.valueOf(read.getKeys().size()));
        AuthCheck.check("with the same RSA key", KeyFixtures.RSA_PUBLIC_DER,
                Base64.encode(read.get(rsa.getKeyId()).getPublicKey()));
        AuthCheck.check("and the same EC key", KeyFixtures.EC256_PUBLIC_DER,
                Base64.encode(read.get(p256.getKeyId()).getPublicKey()));
        AuthCheck.value("rsa bits", String.valueOf(Der.rsaModulusBits(rsaPublic)));
    }

    /** A clock that stands still, so a token's times mean the same on every run. */
    private static final Clock FIXED = new Clock() {
        public long currentTimeMillis() {
            return NOW * 1000L;
        }
    };

    private static DefaultJwtDecoder timed(DefaultJwtDecoder decoder, String issuer) {
        JwtTimestampValidator time = new JwtTimestampValidator();
        time.setClock(FIXED);
        List validators = new ArrayList();
        validators.add(time);
        if(issuer != null) {
            validators.add(new JwtIssuerValidator(issuer));
        }
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator(validators));
        return decoder;
    }

    private static void tokens() throws Exception {
        Jwk rsa = Jwk.ofPrivateKey(Base64.decode(KeyFixtures.RSA_PKCS8_DER));
        Jwk p256 = Jwk.ofPrivateKey(Base64.decode(KeyFixtures.EC256_PKCS8_DER));
        Jwk p384 = Jwk.ofPrivateKey(Base64.decode(KeyFixtures.EC384_PKCS8_DER));
        byte[] secretBytes = new byte[64];
        for(int iter = 0 ; iter < secretBytes.length ; iter++) {
            secretBytes[iter] = (byte) (iter * 3 + 5);
        }
        Jwk secret = Jwk.ofSecret(secretBytes);
        List audience = new ArrayList();
        audience.add("orders-api");
        audience.add("billing-api");
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("https://id.example.com").subject("ada")
                .audience(audience).issuedAt(NOW).expiresAt(NOW + 300)
                .claim("scope", "orders:read orders:write").claim("admin", Boolean.TRUE)
                .claim("name", "Ada \"the first\" Lovelace").build();

        Object[][] cases = {{SignatureAlgorithm.RS256, rsa, "yes"}, {SignatureAlgorithm.RS384, rsa, "yes"},
            {SignatureAlgorithm.RS512, rsa, "yes"}, {MacAlgorithm.HS256, secret, "yes"},
            {MacAlgorithm.HS384, secret, "yes"}, {MacAlgorithm.HS512, secret, "yes"},
            {SignatureAlgorithm.PS256, rsa, "no"}, {SignatureAlgorithm.ES256, p256, "no"},
            {SignatureAlgorithm.ES384, p384, "no"}};
        for(int iter = 0 ; iter < cases.length ; iter++) {
            JwsAlgorithm algorithm = (JwsAlgorithm) cases[iter][0];
            Jwk key = (Jwk) cases[iter][1];
            Jwt signed = new DefaultJwtEncoder(JwkSet.of(key)).encode(JwtEncoderParameters.from(
                    JwsHeader.with(algorithm).type("JWT").build(), claims));
            String token = signed.getTokenValue();
            if("yes".equals(cases[iter][2])) {
                // Deterministic: the whole token, signature included.
                AuthCheck.value("jwt " + algorithm.getName(), token);
            } else {
                // Randomized: everything up to the signature.
                AuthCheck.value("jwt " + algorithm.getName() + " signing input",
                        token.substring(0, token.lastIndexOf('.')));
            }
            JwkSet verifying = key.getPublicKey() == null ? JwkSet.of(key)
                    : JwkSet.of(Jwk.ofPublicKey(key.getPublicKey()));
            JwtDecoder decoder = timed(DefaultJwtDecoder.withJwkSource(verifying)
                    .jwsAlgorithm(algorithm).build(), "https://id.example.com");
            Jwt jwt = decoder.decode(token);
            AuthCheck.check(algorithm.getName() + " decodes its own", "ada|[orders-api, billing-api]|"
                    + (NOW + 300) + "|true|Ada \"the first\" Lovelace", jwt.getSubject() + "|"
                    + jwt.getAudience() + "|" + jwt.getExpiresAt() + "|" + jwt.getClaimAsBoolean("admin")
                    + "|" + jwt.getClaimAsString("name"));
            int dot = token.indexOf('.');
            refused(algorithm.getName() + " tampered", decoder,
                    token.substring(0, dot + 4) + (token.charAt(dot + 4) == 'A' ? 'B' : 'A')
                    + token.substring(dot + 5), null);
        }

        JwtDecoder rs = timed(DefaultJwtDecoder.withPublicKey(Base64.decode(
                KeyFixtures.RSA_PUBLIC_DER)).build(), null);
        JwtDecoder es = timed(DefaultJwtDecoder.withPublicKey(Base64.decode(
                KeyFixtures.EC256_PUBLIC_DER)).build(), null);
        String good = new DefaultJwtEncoder(JwkSet.of(rsa)).encode(
                JwtEncoderParameters.from(claims)).getTokenValue();

        // RFC 7515 A.3, verified against the key in the RFC.
        Map example = Json.parseObject("{\"kty\":\"EC\",\"crv\":\"P-256\","
                + "\"x\":\"f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU\","
                + "\"y\":\"x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0\"}");
        DefaultJwtDecoder rfc = DefaultJwtDecoder.withJwkSource(JwkSet.of(Jwk.parse(example)))
                .jwsAlgorithm(SignatureAlgorithm.ES256).build();
        JwtTimestampValidator in2011 = new JwtTimestampValidator();
        in2011.setClock(new Clock() {
            public long currentTimeMillis() {
                return 1300819000000L;
            }
        });
        rfc.setJwtValidator(in2011);
        String rfcToken = "eyJhbGciOiJFUzI1NiJ9.eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogIm"
                + "h0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ.DtEhU3ljbEg8L38VWAfUAqOyKAM6-Xx-F4Gawx"
                + "aepmXFCgfTjDxw5djxLa8ISlSApmWQxfKTUJqPP3-Kg6NU1Q";
        AuthCheck.check("the RFC 7515 A.3 example verifies", "joe", rfc.decode(rfcToken).getIssuer());
        refused("the RFC 7515 example, today", timed(DefaultJwtDecoder.withJwkSource(JwkSet.of(
                Jwk.parse(example))).jwsAlgorithm(SignatureAlgorithm.ES256).build(), null), rfcToken,
                "Jwt expired at 1300819380");

        // The refusals, each with its reason.
        String none = part("{\"alg\":\"none\"}") + "." + part("{\"sub\":\"admin\"}") + ".";
        refused("alg none", rs, none, "The token's algorithm, none, is not one this decoder accepts");
        refused("alg none to an EC verifier", es, none,
                "The token's algorithm, none, is not one this decoder accepts");
        String signingInput = part("{\"alg\":\"HS256\",\"typ\":\"JWT\"}") + "."
                + part("{\"sub\":\"admin\",\"exp\":" + (NOW + 300) + "}");
        String forged = signingInput + "." + Base64Url.encode(Crypto.hmacSha256(
                Base64.decode(KeyFixtures.RSA_PUBLIC_DER), AuthCheck.ascii(signingInput)));
        AuthCheck.value("hs256 forged with the public key", forged);
        refused("HS256 signed with the RSA public key", rs, forged,
                "The token's algorithm, HS256, is not one this decoder accepts");
        refused("the same, to a decoder told to accept HS256", timed(DefaultJwtDecoder.withJwkSource(
                JwkSet.of(Jwk.ofPublicKey(Base64.decode(KeyFixtures.RSA_PUBLIC_DER))))
                .jwsAlgorithms(new JwsAlgorithm[] {SignatureAlgorithm.RS256, MacAlgorithm.HS256}).build(),
                null), forged, "There is no key to verify a HS256 token with");
        refused("an RS256 token to an EC verifier", es, good,
                "The token's algorithm, RS256, is not one this decoder accepts");
        refused("wrong issuer", timed(DefaultJwtDecoder.withPublicKey(Base64.decode(
                KeyFixtures.RSA_PUBLIC_DER)).build(), "https://other.example.com"), good,
                "The iss claim is not valid");
        refused("an unknown key id", timed(DefaultJwtDecoder.withJwkSource(JwkSet.of(Jwk.ofPublicKey(
                Base64.decode(KeyFixtures.RSA_PUBLIC_DER)).withKeyId("k2"))).build(), null), good,
                "There is no RS256 key with the token's key id, " + rsa.getKeyId());
        refused("garbage", rs, "not.a.token", null);
        refused("two parts", rs, "ab.cd", "Malformed token: not three parts joined by dots");
        DefaultJwtDecoder later = DefaultJwtDecoder.withPublicKey(Base64.decode(
                KeyFixtures.RSA_PUBLIC_DER)).build();
        JwtTimestampValidator afterwards = new JwtTimestampValidator();
        afterwards.setClock(new Clock() {
            public long currentTimeMillis() {
                return (NOW + 361) * 1000L;
            }
        });
        later.setJwtValidator(afterwards);
        refused("expired", later, good, "Jwt expired at " + (NOW + 300));
    }

    private static void refused(String name, JwtDecoder decoder, String token, String expected) {
        try {
            Jwt jwt = decoder.decode(token);
            AuthCheck.fail(name, "was accepted as " + jwt);
        } catch(BadJwtException refused) {
            AuthCheck.value("jwt refusal: " + name, refused.getMessage());
            if(expected != null) {
                AuthCheck.check(name, expected, refused.getMessage());
            } else {
                AuthCheck.check(name, "refused", "refused");
            }
        }
    }

    private static String part(String json) {
        return Base64Url.encode(AuthCheck.ascii(json));
    }

    private static void signedTokens() {
        byte[] secret = new byte[32];
        for(int iter = 0 ; iter < secret.length ; iter++) {
            secret[iter] = (byte) (iter * 7 + 1);
        }
        SignedTokens tokens = new SignedTokens(secret);
        tokens.setClock(FIXED);
        String plain = tokens.create("email-verify", "user:42", 600);
        String bound = tokens.create("password-reset", "ada", 3600, "{bcrypt}$2a$10$old");
        AuthCheck.value("signed token", plain);
        AuthCheck.value("signed token with a fingerprint", bound);
        AuthCheck.check("a signed token verifies", "user:42", tokens.verify("email-verify", plain));
        AuthCheck.check("not for another purpose", null, tokens.verify("password-reset", plain));
        AuthCheck.check("with its fingerprint", "ada",
                tokens.verify("password-reset", bound, "{bcrypt}$2a$10$old"));
        AuthCheck.check("not once the fingerprint moved", null,
                tokens.verify("password-reset", bound, "{bcrypt}$2a$10$new"));
        AuthCheck.check("not changed", null, tokens.verify("email-verify", "A" + plain.substring(1)));
        SignedTokens later = new SignedTokens(secret);
        later.setClock(new Clock() {
            public long currentTimeMillis() {
                return (NOW + 600) * 1000L;
            }
        });
        AuthCheck.check("not after it expires", null, later.verify("email-verify", plain));
    }

    private static void passwords() {
        PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        // Written by Spring Security: its documentation's example, and two from 6.4.4 for a
        // password that is not ASCII.
        String documented = "{pbkdf2}5d923b44a6d129f3ddf3e3c8d29412723dcbde72445e8ef6bf3b508fbf17fa4e"
                + "d4d6b99ca763d8dc";
        String umlaut = "p" + (char) 0xe4 + "ssword";
        AuthCheck.check("Spring's documented {pbkdf2} hash", "true",
                String.valueOf(encoder.matches("password", documented)));
        AuthCheck.check("and not for another password", "false",
                String.valueOf(encoder.matches("passwore", documented)));
        AuthCheck.check("{pbkdf2} from Spring 6.4, non-ASCII", "true", String.valueOf(encoder.matches(
                umlaut, "{pbkdf2}8025389de04d342c376027189064f478268cfc8d7f25d423761ae9e069a5ceea4491"
                + "c3c4f7dc621d")));
        AuthCheck.check("{pbkdf2@SpringSecurity_v5_8} from Spring 6.4, non-ASCII", "true",
                String.valueOf(encoder.matches(umlaut, "{pbkdf2@SpringSecurity_v5_8}beccbc5a992abfdbe1"
                + "204d108a798498144110429e43b1603480da558ae0cb919b18ea38944038e582a93d599a6fc86b")));
    }

    private static void oneTimePasswords() {
        // The client's own implementation, shared with the server and compiled into it: the
        // RFC 6238 appendix B secrets and times.
        byte[] sha1 = AuthCheck.ascii("12345678901234567890");
        byte[] sha256 = AuthCheck.ascii("12345678901234567890123456789012");
        byte[] sha512 = AuthCheck.ascii(
                "1234567890123456789012345678901234567890123456789012345678901234");
        AuthCheck.check("TOTP SHA-1 at 59", "94287082", Otp.totp(sha1, 59000L, 30, 8, Hash.SHA1));
        AuthCheck.check("TOTP SHA-256 at 59", "46119246", Otp.totp(sha256, 59000L, 30, 8, Hash.SHA256));
        AuthCheck.check("TOTP SHA-512 at 59", "90693936", Otp.totp(sha512, 59000L, 30, 8, Hash.SHA512));
        AuthCheck.check("TOTP SHA-1 at 1111111109", "07081804",
                Otp.totp(sha1, 1111111109000L, 30, 8, Hash.SHA1));
        AuthCheck.check("TOTP SHA-512 at 20000000000", "47863826",
                Otp.totp(sha512, 20000000000000L, 30, 8, Hash.SHA512));
        AuthCheck.check("HOTP counter 0", "755224", Otp.hotp(sha1, 0, 6));
        AuthCheck.value("totp six digits", Otp.totp(sha1, NOW * 1000L, 30, 6, Hash.SHA1));
        AuthCheck.check("base32", "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ", Base32.encode(sha1));
        AuthCheck.check("base32 back", AuthCheck.hex(sha1), AuthCheck.hex(Base32.decode(
                "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")));
        // The shared pure Java digests and the native ones are the same functions.
        byte[] data = AuthCheck.ascii("what do ya want for nothing?");
        AuthCheck.check("shared SHA-512 is the native one", AuthCheck.hex(Crypto.sha512(data)),
                AuthCheck.hex(Hash.sha512(data)));
        AuthCheck.check("shared SHA-384 is the native one", AuthCheck.hex(Crypto.sha384(data)),
                AuthCheck.hex(Hash.sha384(data)));
        AuthCheck.check("shared SHA-1 is the native one", AuthCheck.hex(Crypto.sha1(data)),
                AuthCheck.hex(Hash.sha1(data)));
        AuthCheck.check("shared HMAC-SHA-1 is the native one",
                AuthCheck.hex(Crypto.hmac(Crypto.SHA1, sha1, data)), AuthCheck.hex(Hmac.sha1(sha1, data)));
        AuthCheck.check("shared HMAC-SHA-512 is the native one",
                AuthCheck.hex(Crypto.hmac(Crypto.SHA512, sha1, data)),
                AuthCheck.hex(Hmac.sha512(sha1, data)));
    }
}
