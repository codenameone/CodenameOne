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
package com.codename1.backend.security.oauth2.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Base64;
import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.Json;
import com.codename1.backend.security.Clock;
import com.codename1.backend.security.crypto.Jwk;
import com.codename1.backend.security.crypto.JwkSet;
import com.codename1.backend.security.crypto.KeyFixtures;
import com.codename1.backend.security.oauth2.core.OAuth2TokenValidator;
import com.codename1.backend.security.oauth2.jose.jws.JwsAlgorithm;
import com.codename1.backend.security.oauth2.jose.jws.MacAlgorithm;
import com.codename1.backend.security.oauth2.jose.jws.SignatureAlgorithm;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// Tokens made and verified: the examples of RFC 7515, every algorithm round
/// trip, and every way a token is refused with the reason it is given.
class JwtCodecTest {
    private static final long NOW = 1700000000L;

    /// A clock a test moves.
    static final class Moving implements Clock {
        long seconds = NOW;

        @Override
        public long currentTimeMillis() {
            return seconds * 1000L;
        }
    }

    private static byte[] der(String base64) {
        return Base64.decode(base64);
    }

    private static Jwk rsa() throws IOException {
        return Jwk.ofPrivateKey(der(KeyFixtures.RSA_PKCS8_DER));
    }

    private static Jwk p256() throws IOException {
        return Jwk.ofPrivateKey(der(KeyFixtures.EC256_PKCS8_DER));
    }

    private static Jwk p384() throws IOException {
        return Jwk.ofPrivateKey(der(KeyFixtures.EC384_PKCS8_DER));
    }

    private static JwtClaimsSet.Builder claims() {
        return JwtClaimsSet.builder().issuer("https://id.example.com").subject("ada")
                .audience("orders-api").issuedAt(NOW).expiresAt(NOW + 300)
                .claim("scope", "orders:read orders:write");
    }

    private static DefaultJwtDecoder at(DefaultJwtDecoder decoder, Moving clock,
                                        OAuth2TokenValidator<Jwt>... more) {
        JwtTimestampValidator time = new JwtTimestampValidator();
        time.setClock(clock);
        List<OAuth2TokenValidator<Jwt>> all = new ArrayList<OAuth2TokenValidator<Jwt>>();
        all.add(time);
        all.addAll(Arrays.asList(more));
        decoder.setJwtValidator(new com.codename1.backend.security.oauth2.core
                .DelegatingOAuth2TokenValidator<Jwt>(all));
        return decoder;
    }

    private static String part(String json) {
        return Base64Url.encode(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String refusal(JwtDecoder decoder, String token) {
        return assertThrows(BadJwtException.class, () -> decoder.decode(token)).getMessage();
    }

    @Test
    @DisplayName("RFC 7515 A.2: the RS256 example verifies against its JWK")
    @SuppressWarnings("unchecked")
    void rfc7515Rs256() throws Exception {
        Map<String, Object> jwk = (Map<String, Object>) (Map) Json.parseObject("{\"kty\":\"RSA\","
                + "\"n\":\"ofgWCuLjybRlzo0tZWJjNiuSfb4p4fAkd_wWJcyQoTbji9k0l8W26mPddxHmfHQp-Vaw-4qPCJrcS2mJPMEzP1Pt0Bm4d4QlL-yRT-SFd2lZS-pCgNMsD1W_YpRPEwOWvG6b32690r2jZ47soMZo9wGzjb_7OMg0LOL-bSf63kpaSHSXndS5z5rexMdbBYUsLA9e-KXBdQOS-UTo7WTBEMa2R2CapHg665xsmtdVMTBQY4uDZlxvb3qCo5ZwKh9kG4LT6_I5IhlJH7aGhyxXFvUK-DWNmoudF8NAco9_h9iaGNj8q2ethFkMLs91kzk2PAcDTW9gb54h4FRWyuXpoQ\","
                + "\"e\":\"AQAB\"}");
        String token = "eyJhbGciOiJSUzI1NiJ9"
                + ".eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ"
                + ".cC4hiUPoj9Eetdgtv3hF80EGrhuB__dzERat0XF9g2VtQgr9PJbu3XOiZj5RZmh7AAuHIm4Bh-0Qc_lF5YKt_O8W2Fp5jujGbds9uJdbF9CUAr7t1dnZcAcQjbKBYNX4BAynRFdiuB--f_nZLgrnbyTyWzO75vRK5h6xBArLIARNPvkSjtQBMHlb1L07Qe7K0GarZRmB_eSN9383LcOLn6_dO--xi12jzDwusC-eOkHWEsqtFZESc6BfI7noOPqvhJ1phCnvWh6IeYI2w9QOYEUipUTI8np6LbgGY9Fs98rqVt5AXLIhWkWywlVmtVrBp0igcN_IoypGlUPQGe77Rw";
        Moving clock = new Moving();
        clock.seconds = 1300819000L;
        DefaultJwtDecoder decoder = at(DefaultJwtDecoder.withJwkSource(JwkSet.of(Jwk.parse(jwk)))
                .build(), clock);
        Jwt jwt = decoder.decode(token);
        assertEquals("joe", jwt.getIssuer());
        assertEquals(Long.valueOf(1300819380L), jwt.getExpiresAt());
        assertEquals(Boolean.TRUE, jwt.getClaimAsBoolean("http://example.com/is_root"));
        assertEquals("RS256", jwt.getHeaders().get("alg"));
        // The example expired in 2011, and says so today.
        clock.seconds = NOW;
        assertEquals("Jwt expired at 1300819380", refusal(decoder, token));
        // One character of the payload changed.
        assertEquals("The token's signature does not verify",
                refusal(decoder, token.replace("eyJpc3MiOiJqb2Ui", "eyJpc3MiOiJqb2Vi")));
    }

    @Test
    @DisplayName("RFC 7515 A.3: the ES256 example verifies against its JWK")
    @SuppressWarnings("unchecked")
    void rfc7515Es256() throws Exception {
        Map<String, Object> jwk = (Map<String, Object>) (Map) Json.parseObject("{\"kty\":\"EC\","
                + "\"crv\":\"P-256\",\"x\":\"f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU\","
                + "\"y\":\"x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0\"}");
        String token = "eyJhbGciOiJFUzI1NiJ9"
                + ".eyJpc3MiOiJqb2UiLA0KICJleHAiOjEzMDA4MTkzODAsDQogImh0dHA6Ly9leGFtcGxlLmNvbS9pc19yb290Ijp0cnVlfQ"
                + ".DtEhU3ljbEg8L38VWAfUAqOyKAM6-Xx-F4GawxaepmXFCgfTjDxw5djxLa8ISlSApmWQxfKTUJqPP3-Kg6NU1Q";
        Moving clock = new Moving();
        clock.seconds = 1300819000L;
        DefaultJwtDecoder decoder = at(DefaultJwtDecoder.withJwkSource(JwkSet.of(Jwk.parse(jwk)))
                .jwsAlgorithm(SignatureAlgorithm.ES256).build(), clock);
        assertEquals("joe", decoder.decode(token).getIssuer());
        assertEquals("The token's signature does not verify",
                refusal(decoder, token.replace(".DtEhU3lj", ".DtEhU3lk")));
    }

    @Test
    @DisplayName("Every algorithm: what the encoder signs, the decoder verifies")
    void roundTrips() throws Exception {
        Moving clock = new Moving();
        byte[] secret = new byte[64];
        Arrays.fill(secret, (byte) 0x5a);
        Object[][] cases = {
            {SignatureAlgorithm.RS256, rsa()}, {SignatureAlgorithm.RS384, rsa()},
            {SignatureAlgorithm.RS512, rsa()}, {SignatureAlgorithm.PS256, rsa()},
            {SignatureAlgorithm.ES256, p256()}, {SignatureAlgorithm.ES384, p384()},
            {MacAlgorithm.HS256, Jwk.ofSecret(secret)}, {MacAlgorithm.HS384, Jwk.ofSecret(secret)},
            {MacAlgorithm.HS512, Jwk.ofSecret(secret)}};
        for (Object[] c : cases) {
            JwsAlgorithm algorithm = (JwsAlgorithm) c[0];
            Jwk key = (Jwk) c[1];
            JwtEncoder encoder = new DefaultJwtEncoder(JwkSet.of(key));
            Jwt signed = encoder.encode(JwtEncoderParameters.from(
                    JwsHeader.with(algorithm).type("JWT").build(), claims().build()));
            assertEquals(algorithm.getName(), signed.getHeaders().get("alg"));
            assertEquals(key.getKeyId(), signed.getHeaders().get("kid"));
            // The decoder holds the public half only.
            JwkSet verifying = Jwk.OCT.equals(key.getKeyType()) ? JwkSet.of(key)
                    : JwkSet.of(Jwk.ofPublicKey(key.getPublicKey()));
            DefaultJwtDecoder decoder = at(DefaultJwtDecoder.withJwkSource(verifying)
                    .jwsAlgorithm(algorithm).build(), clock);
            Jwt jwt = decoder.decode(signed.getTokenValue());
            assertEquals("ada", jwt.getSubject(), algorithm.getName());
            assertEquals("https://id.example.com", jwt.getIssuer());
            assertEquals(Arrays.asList("orders-api"), jwt.getAudience());
            assertEquals(Long.valueOf(NOW + 300), jwt.getExpiresAt());
            assertEquals(Long.valueOf(NOW), jwt.getIssuedAt());
            assertEquals("orders:read orders:write", jwt.getClaimAsString("scope"));
            assertEquals(signed.getTokenValue(), jwt.getTokenValue());
            // Tampered: the last character of the payload.
            String token = signed.getTokenValue();
            int second = token.lastIndexOf('.');
            char last = token.charAt(second - 1);
            String tampered = token.substring(0, second - 1) + (last == 'A' ? 'B' : 'A')
                    + token.substring(second);
            String why = refusal(decoder, tampered);
            assertTrue(why.equals("The token's signature does not verify")
                    || why.startsWith("Malformed token"), algorithm.getName() + ": " + why);
            // Signed by somebody else.
            assertEquals("The token's signature does not verify", refusal(decoder,
                    token.substring(0, second + 1) + Base64Url.encode(new byte[
                            Base64Url.decode(token.substring(second + 1)).length])),
                    algorithm.getName());
        }
    }

    @Test
    @DisplayName("The encoder: default algorithm by key, key by id, deterministic RS256")
    void encoder() throws Exception {
        JwtClaimsSet claims = claims().build();
        // No header: the first private key, under its kind's algorithm.
        assertEquals("RS256", new DefaultJwtEncoder(JwkSet.of(rsa(), p256()))
                .encode(JwtEncoderParameters.from(claims)).getHeaders().get("alg"));
        assertEquals("ES256", new DefaultJwtEncoder(JwkSet.of(p256(), rsa()))
                .encode(JwtEncoderParameters.from(claims)).getHeaders().get("alg"));
        assertEquals("ES384", new DefaultJwtEncoder(JwkSet.of(p384()))
                .encode(JwtEncoderParameters.from(claims)).getHeaders().get("alg"));
        // A public key in front of it cannot sign and is passed over.
        assertEquals("ES384", new DefaultJwtEncoder(JwkSet.of(Jwk.ofPublicKey(rsa().getPublicKey()),
                p384())).encode(JwtEncoderParameters.from(claims)).getHeaders().get("alg"));
        // The algorithm asked for picks the key of its kind.
        JwtEncoder both = new DefaultJwtEncoder(JwkSet.of(rsa().withKeyId("old"),
                rsa().withKeyId("new"), p256()));
        assertEquals(p256().getKeyId(), both.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.ES256).build(), claims)).getHeaders().get("kid"));
        assertEquals("old", both.encode(JwtEncoderParameters.from(claims)).getHeaders().get("kid"));
        assertEquals("new", both.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId("new").build(), claims))
                .getHeaders().get("kid"));
        // RS256 is deterministic: the same token every time, on every runtime.
        String first = both.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        assertEquals(first, both.encode(JwtEncoderParameters.from(claims)).getTokenValue());
        assertEquals("{\"alg\":\"RS256\",\"kid\":\"old\"}", new String(Base64Url.decode(
                first.substring(0, first.indexOf('.'))), StandardCharsets.UTF_8));
        assertEquals("{\"iss\":\"https://id.example.com\",\"sub\":\"ada\",\"aud\":\"orders-api\","
                + "\"iat\":1700000000,\"exp\":1700000300,\"scope\":\"orders:read orders:write\"}",
                new String(Base64Url.decode(first.substring(first.indexOf('.') + 1,
                        first.lastIndexOf('.'))), StandardCharsets.UTF_8));
        // Several audiences are a list.
        Jwt many = both.encode(JwtEncoderParameters.from(JwtClaimsSet.from(claims)
                .audience("orders-api", "billing-api").build()));
        assertEquals(Arrays.asList("orders-api", "billing-api"), many.getAudience());

        assertEquals("There is no key to sign with under ES384: the source holds no private key "
                + "that fits", assertThrows(JwtEncodingException.class,
                        () -> both.encode(JwtEncoderParameters.from(JwsHeader.with(
                                SignatureAlgorithm.ES384).build(), claims))).getMessage());
        assertEquals("There is no key to sign with under RS256 with the id gone: the source holds "
                + "no private key that fits", assertThrows(JwtEncodingException.class,
                        () -> both.encode(JwtEncoderParameters.from(JwsHeader.with(
                                SignatureAlgorithm.RS256).keyId("gone").build(), claims)))
                        .getMessage());
        // An RSA key cannot be made to sign as an EC one by asking.
        assertThrows(JwtEncodingException.class, () -> new DefaultJwtEncoder(JwkSet.of(rsa()))
                .encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.ES256).build(),
                        claims)));
        assertEquals("Could not sign the token: A secret for HS512 is at least 64 bytes",
                assertThrows(JwtEncodingException.class, () -> new DefaultJwtEncoder(JwkSet.of(
                        Jwk.ofSecret(new byte[32]))).encode(JwtEncoderParameters.from(JwsHeader.with(
                                MacAlgorithm.HS512).build(), claims))).getMessage());
    }

    @Test
    @DisplayName("alg=none is refused, with or without a signature")
    void algNone() throws Exception {
        Moving clock = new Moving();
        String payload = part("{\"sub\":\"admin\",\"exp\":" + (NOW + 300) + "}");
        String[] spellings = {"none", "None", "NONE", "nOnE"};
        for (JwtDecoder decoder : new JwtDecoder[] {
            at(DefaultJwtDecoder.withPublicKey(der(KeyFixtures.RSA_PUBLIC_DER)).build(), clock),
            at(DefaultJwtDecoder.withPublicKey(der(KeyFixtures.EC256_PUBLIC_DER)).build(), clock),
            at(DefaultJwtDecoder.withSecretKey(new byte[32]).build(), clock)}) {
            for (String none : spellings) {
                String header = part("{\"alg\":\"" + none + "\",\"typ\":\"JWT\"}");
                // No third part at all, an empty one, and one with bytes in it.
                assertEquals("The token's algorithm, " + none + ", is not one this decoder accepts",
                        refusal(decoder, header + "." + payload + ".AAAA"));
                assertEquals("The token's algorithm, " + none + ", is not one this decoder accepts",
                        refusal(decoder, header + "." + payload + "."));
                assertEquals("Malformed token: not three parts joined by dots",
                        refusal(decoder, header + "." + payload));
            }
        }
        // And there is no way to ask a decoder for it.
        assertNull(SignatureAlgorithm.from("none"));
        assertNull(MacAlgorithm.from("none"));
    }

    @Test
    @DisplayName("An HS256 token made from the RSA public key is refused by the RSA verifier")
    void hmacWithPublicKeyAsSecret() throws Exception {
        Moving clock = new Moving();
        byte[] publicDer = der(KeyFixtures.RSA_PUBLIC_DER);
        String signingInput = part("{\"alg\":\"HS256\",\"typ\":\"JWT\"}") + "."
                + part("{\"sub\":\"admin\",\"exp\":" + (NOW + 300) + "}");
        // The attack: the public key -- which everybody has -- used as the HMAC
        // secret, in each of the forms a careless verifier might feed to HMAC.
        byte[][] secrets = {publicDer, KeyFixtures.RSA_PUBLIC_PEM.getBytes(StandardCharsets.US_ASCII),
            KeyFixtures.RSA_PUBLIC_PEM.trim().getBytes(StandardCharsets.US_ASCII)};
        for (byte[] secret : secrets) {
            String forged = signingInput + "." + Base64Url.encode(Crypto.hmacSha256(secret,
                    signingInput.getBytes(StandardCharsets.US_ASCII)));
            // The decoder as anyone would build it.
            assertEquals("The token's algorithm, HS256, is not one this decoder accepts",
                    refusal(at(DefaultJwtDecoder.withPublicKey(publicDer).build(), clock), forged));
            assertEquals("The token's algorithm, HS256, is not one this decoder accepts",
                    refusal(at(DefaultJwtDecoder.withJwkSource(JwkSet.of(Jwk.ofPublicKey(publicDer)))
                            .build(), clock), forged));
            // And one told, wrongly, to accept HS256 beside RS256: the key is
            // still an RSA key, and an RSA key is not an HMAC secret.
            assertEquals("There is no key to verify a HS256 token with",
                    refusal(at(DefaultJwtDecoder.withJwkSource(JwkSet.of(Jwk.ofPublicKey(publicDer)))
                            .jwsAlgorithms(SignatureAlgorithm.RS256, MacAlgorithm.HS256).build(),
                            clock), forged));
        }
        // The other direction: an RS256 token to a decoder that holds a secret.
        String rs256 = new DefaultJwtEncoder(JwkSet.of(rsa())).encode(
                JwtEncoderParameters.from(claims().build())).getTokenValue();
        assertEquals("The token's algorithm, RS256, is not one this decoder accepts",
                refusal(at(DefaultJwtDecoder.withSecretKey(new byte[32]).build(), clock), rs256));
        // An EC token to an RSA verifier told to accept ES256, and a P-384 key
        // under ES256.
        String es256 = new DefaultJwtEncoder(JwkSet.of(p256())).encode(
                JwtEncoderParameters.from(claims().build())).getTokenValue();
        assertEquals("There is no ES256 key with the token's key id, " + p256().getKeyId(),
                refusal(at(DefaultJwtDecoder.withJwkSource(JwkSet.of(Jwk.ofPublicKey(publicDer)))
                        .jwsAlgorithms(SignatureAlgorithm.RS256, SignatureAlgorithm.ES256).build(),
                        clock), es256));
        String es256Unnamed = es256Token(p256(), "{\"alg\":\"ES256\"}");
        assertEquals("There is no key to verify a ES256 token with",
                refusal(at(DefaultJwtDecoder.withJwkSource(JwkSet.of(Jwk.ofPublicKey(
                        p384().getPublicKey()))).jwsAlgorithm(SignatureAlgorithm.ES256).build(),
                        clock), es256Unnamed));
        // A key that says it is for RS256 does not verify PS256.
        String ps256 = new DefaultJwtEncoder(JwkSet.of(rsa())).encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.PS256).build(), claims().build())).getTokenValue();
        assertEquals("There is no PS256 key with the token's key id, " + rsa().getKeyId(),
                refusal(at(DefaultJwtDecoder.withJwkSource(JwkSet.of(Jwk.ofPublicKey(publicDer)
                        .withAlgorithm("RS256"))).jwsAlgorithms(SignatureAlgorithm.RS256,
                        SignatureAlgorithm.PS256).build(), clock), ps256));
        assertEquals("The secret must be at least 32 bytes; a shorter one can be found by trying",
                assertThrows(IllegalArgumentException.class,
                        () -> DefaultJwtDecoder.withSecretKey(new byte[31])).getMessage());
    }

    private static String es256Token(Jwk key, String header) throws IOException {
        String signingInput = part(header) + "." + part("{\"sub\":\"ada\",\"exp\":" + (NOW + 300) + "}");
        byte[] derSignature = Crypto.sign(Crypto.ES256, key.getPrivateKey(),
                signingInput.getBytes(StandardCharsets.US_ASCII));
        return signingInput + "." + Base64Url.encode(
                com.codename1.backend.security.crypto.Der.ecdsaDerToJose(derSignature, 32));
    }

    @Test
    @DisplayName("Claims: expired, not yet valid, wrong issuer, wrong audience, a nonce")
    @SuppressWarnings("unchecked")
    void validation() throws Exception {
        Moving clock = new Moving();
        JwtEncoder encoder = new DefaultJwtEncoder(JwkSet.of(rsa()));
        byte[] publicKey = der(KeyFixtures.RSA_PUBLIC_DER);
        DefaultJwtDecoder decoder = at(DefaultJwtDecoder.withPublicKey(publicKey).build(), clock,
                new JwtIssuerValidator("https://id.example.com"),
                new JwtAudienceValidator("orders-api", "orders-admin"));
        String good = encoder.encode(JwtEncoderParameters.from(claims().build())).getTokenValue();
        assertEquals("ada", decoder.decode(good).getSubject());

        // exp, with a minute's allowance.
        clock.seconds = NOW + 300 + 60;
        assertEquals("ada", decoder.decode(good).getSubject());
        clock.seconds = NOW + 300 + 61;
        JwtValidationException expired = assertThrows(JwtValidationException.class,
                () -> decoder.decode(good));
        assertEquals("Jwt expired at " + (NOW + 300), expired.getMessage());
        assertEquals("invalid_token", expired.getErrors().get(0).getErrorCode());
        clock.seconds = NOW;

        // nbf, with the same allowance.
        String later = encoder.encode(JwtEncoderParameters.from(claims().notBefore(NOW + 120)
                .build())).getTokenValue();
        assertEquals("Jwt used before " + (NOW + 120), refusal(decoder, later));
        clock.seconds = NOW + 60;
        assertEquals("ada", decoder.decode(later).getSubject());
        clock.seconds = NOW;

        assertEquals("The iss claim is not valid", refusal(decoder, encoder.encode(
                JwtEncoderParameters.from(claims().issuer("https://evil.example.com").build()))
                .getTokenValue()));
        // Close is not the issuer.
        assertEquals("The iss claim is not valid", refusal(decoder, encoder.encode(
                JwtEncoderParameters.from(claims().issuer("https://id.example.com/").build()))
                .getTokenValue()));
        assertEquals("The aud claim is not valid", refusal(decoder, encoder.encode(
                JwtEncoderParameters.from(claims().audience("billing-api").build()))
                .getTokenValue()));
        assertEquals("ada", decoder.decode(encoder.encode(JwtEncoderParameters.from(claims()
                .audience("billing-api", "orders-admin").build())).getTokenValue()).getSubject());
        // No issuer and no audience at all.
        String bare = encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder().subject("ada")
                .expiresAt(NOW + 300).build())).getTokenValue();
        JwtValidationException both = assertThrows(JwtValidationException.class,
                () -> decoder.decode(bare));
        assertEquals(2, both.getErrors().size(), "every failure is reported");
        // The default validators check time and nothing else, and say so by passing.
        assertEquals("ada", at(DefaultJwtDecoder.withPublicKey(publicKey).build(), clock)
                .decode(bare).getSubject());

        // An ID token's nonce.
        DefaultJwtDecoder withNonce = at(DefaultJwtDecoder.withPublicKey(publicKey).build(), clock,
                new JwtClaimValidator<Object>("nonce", "n-0S6_WzA2Mj"::equals));
        assertEquals("ada", withNonce.decode(encoder.encode(JwtEncoderParameters.from(claims()
                .claim("nonce", "n-0S6_WzA2Mj").build())).getTokenValue()).getSubject());
        assertEquals("The nonce claim is not valid", refusal(withNonce, encoder.encode(
                JwtEncoderParameters.from(claims().claim("nonce", "another").build()))
                .getTokenValue()));
        assertEquals("The nonce claim is not valid", refusal(withNonce, good));

        // JwtValidators' own compositions.
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(
                "https://id.example.com");
        Map<String, Object> headers = new LinkedHashMap<String, Object>();
        headers.put("alg", "RS256");
        Map<String, Object> past = new LinkedHashMap<String, Object>();
        past.put("iss", "https://other.example.com");
        past.put("exp", Long.valueOf(1L));
        assertEquals(2, withIssuer.validate(new Jwt("a.b.c", headers, past)).getErrors().size());
        assertTrue(JwtValidators.createDefault().validate(new Jwt("a.b.c", headers,
                new LinkedHashMap<String, Object>())).getErrors().isEmpty());
        assertEquals(1, JwtValidators.createDefaultWithValidators(new JwtAudienceValidator("x"))
                .validate(new Jwt("a.b.c", headers, past)).getErrors().size() - 1);
    }

    @Test
    @DisplayName("Malformed tokens are refused as tokens, never as server errors")
    void malformed() throws Exception {
        Moving clock = new Moving();
        DefaultJwtDecoder decoder = at(DefaultJwtDecoder.withPublicKey(
                der(KeyFixtures.RSA_PUBLIC_DER)).build(), clock);
        String good = new DefaultJwtEncoder(JwkSet.of(rsa())).encode(
                JwtEncoderParameters.from(claims().build())).getTokenValue();
        int first = good.indexOf('.');
        int second = good.lastIndexOf('.');
        String signature = good.substring(second);
        assertEquals("No token was given", refusal(decoder, ""));
        assertEquals("No token was given", refusal(decoder, null));
        assertEquals("Malformed token: not three parts joined by dots", refusal(decoder, "abc"));
        assertEquals("Malformed token: not three parts joined by dots", refusal(decoder, "a..c"));
        assertEquals("Malformed token: not three parts joined by dots", refusal(decoder, ".b.c"));
        assertEquals("Malformed token: an encrypted token, or one of more than three parts, is not "
                + "supported", refusal(decoder, "a.b.c.d.e"));
        assertEquals("Malformed token: the header is not base64url",
                refusal(decoder, "!!!" + good.substring(first)));
        assertEquals("Malformed token: the header is not a JSON object",
                refusal(decoder, part("[1,2]") + good.substring(first)));
        assertEquals("Malformed token: the header is not a JSON object",
                refusal(decoder, part("{\"alg\":") + good.substring(first)));
        assertEquals("Malformed token: the header names no algorithm",
                refusal(decoder, part("{\"typ\":\"JWT\"}") + good.substring(first)));
        assertEquals("Malformed token: the header names no algorithm",
                refusal(decoder, part("{\"alg\":256}") + good.substring(first)));
        assertEquals("Malformed token: the signature is not base64url",
                refusal(decoder, good.substring(0, second) + ".***"));
        assertEquals("Malformed token: the signature is not base64url",
                refusal(decoder, good.substring(0, second) + "."));
        assertEquals("The token has a critical header this decoder does not understand",
                refusal(decoder, part("{\"alg\":\"RS256\",\"crit\":[\"exp\"]}")
                        + good.substring(first)));
        assertEquals("The token's type, dpop+jwt, is not JWT",
                refusal(decoder, part("{\"alg\":\"RS256\",\"typ\":\"dpop+jwt\"}")
                        + good.substring(first)));
        assertEquals("Malformed token: the key id is not text",
                refusal(decoder, part("{\"alg\":\"RS256\",\"kid\":7}") + good.substring(first)));
        char[] long1 = new char[17000];
        Arrays.fill(long1, 'A');
        assertEquals("The token is too long to be one", refusal(decoder, new String(long1)));

        // A payload that is signed and is not claims. Signed for real, so the
        // refusal is about the payload and not the signature.
        Jwk key = rsa();
        String[][] payloads = {{"[1,2,3]", "Malformed token: the payload is not a JSON object"},
            {"{\"exp\":\"tomorrow\"}", "Malformed token: the exp claim is not a number"},
            {"{\"nbf\":true}", "Malformed token: the nbf claim is not a number"}};
        for (String[] p : payloads) {
            String signingInput = part("{\"alg\":\"RS256\"}") + "." + part(p[0]);
            String token = signingInput + "." + Base64Url.encode(Crypto.sign(Crypto.RS256,
                    key.getPrivateKey(), signingInput.getBytes(StandardCharsets.US_ASCII)));
            assertEquals(p[1], refusal(decoder, token));
        }
        // Unsigned garbage in the payload is a signature failure: nothing in a
        // payload is read before the signature has verified.
        assertEquals("The token's signature does not verify",
                refusal(decoder, good.substring(0, first) + "." + part("[1]") + signature));
        // An ES256 signature of the wrong length never reaches the verifier.
        DefaultJwtDecoder ec = at(DefaultJwtDecoder.withPublicKey(der(KeyFixtures.EC256_PUBLIC_DER))
                .build(), clock);
        String es = es256Token(p256(), "{\"alg\":\"ES256\"}");
        assertEquals("ada", ec.decode(es).getSubject());
        assertEquals("The token's signature does not verify", refusal(ec,
                es.substring(0, es.lastIndexOf('.') + 1) + Base64Url.encode(new byte[63])));
        // The DER form openssl writes is not the form a token carries.
        String signingInput = es.substring(0, es.lastIndexOf('.'));
        assertEquals("The token's signature does not verify", refusal(ec, signingInput + "."
                + Base64Url.encode(Crypto.sign(Crypto.ES256, p256().getPrivateKey(),
                        signingInput.getBytes(StandardCharsets.US_ASCII)))));
    }

    @Test
    @DisplayName("Key ids: the named key verifies, an unknown one is refused, none tries each")
    void keyIds() throws Exception {
        Moving clock = new Moving();
        Jwk other = Jwk.ofPrivateKey(Crypto.generateRsaKey(2048)).withKeyId("other");
        Jwk current = rsa().withKeyId("current");
        DefaultJwtDecoder decoder = at(DefaultJwtDecoder.withJwkSource(JwkSet.of(
                Jwk.ofPublicKey(other.getPublicKey()).withKeyId("other"),
                Jwk.ofPublicKey(current.getPublicKey()).withKeyId("current"))).build(), clock);
        JwtClaimsSet claims = claims().build();
        assertEquals("ada", decoder.decode(new DefaultJwtEncoder(JwkSet.of(current))
                .encode(JwtEncoderParameters.from(claims)).getTokenValue()).getSubject());
        assertEquals("ada", decoder.decode(new DefaultJwtEncoder(JwkSet.of(other))
                .encode(JwtEncoderParameters.from(claims)).getTokenValue()).getSubject());
        assertEquals("There is no RS256 key with the token's key id, retired", refusal(decoder,
                new DefaultJwtEncoder(JwkSet.of(rsa().withKeyId("retired")))
                        .encode(JwtEncoderParameters.from(claims)).getTokenValue()));
        // The right id on the wrong key.
        assertEquals("The token's signature does not verify", refusal(decoder,
                new DefaultJwtEncoder(JwkSet.of(other.withKeyId("current")))
                        .encode(JwtEncoderParameters.from(claims)).getTokenValue()));
        // No id: any key of the right kind may be the one.
        String signingInput = part("{\"alg\":\"RS256\"}") + "." + part("{\"sub\":\"ada\"}");
        String unnamed = signingInput + "." + Base64Url.encode(Crypto.sign(Crypto.RS256,
                current.getPrivateKey(), signingInput.getBytes(StandardCharsets.US_ASCII)));
        assertEquals("ada", decoder.decode(unnamed).getSubject());
        // A hostile key id is not echoed raw.
        String hostile = part("{\"alg\":\"RS256\",\"kid\":\"a\\\"b\\r\\nc\"}") + "."
                + part("{\"sub\":\"ada\"}") + ".AAAA";
        assertEquals("There is no RS256 key with the token's key id, a?b??c",
                refusal(decoder, hostile));
    }

    /// A JWKS endpoint that counts how often it is asked.
    static final class Endpoint implements RemoteJwkSet.Fetcher {
        final Map<String, String> documents = new LinkedHashMap<String, String>();
        final List<String> asked = new ArrayList<String>();
        boolean down;

        @Override
        public String fetch(String uri) throws IOException {
            asked.add(uri);
            String document = documents.get(uri);
            if (down || document == null) {
                throw new IOException("GET " + uri + " answered " + (down ? 503 : 404));
            }
            return document;
        }
    }

    @Test
    @DisplayName("A remote key set: cached, refetched on an unknown key id, at most once an interval")
    void remoteKeys() throws Exception {
        Moving time = new Moving();
        Endpoint endpoint = new Endpoint();
        String uri = "https://id.example.com/jwks";
        Jwk first = rsa().withKeyId("k1");
        endpoint.documents.put(uri, JwkSet.of(first).toJson());
        RemoteJwkSet remote = new RemoteJwkSet(uri, endpoint);
        remote.setClock(time);
        DefaultJwtDecoder decoder = at(DefaultJwtDecoder.withJwkSource(remote).build(), time);
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("ada").build();
        String k1 = new DefaultJwtEncoder(JwkSet.of(first)).encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        assertEquals(0, endpoint.asked.size(), "nothing is fetched until a token arrives");
        assertEquals("ada", decoder.decode(k1).getSubject());
        assertEquals("ada", decoder.decode(k1).getSubject());
        assertEquals(1, endpoint.asked.size(), "the set is kept");

        // The issuer rotates. A token under the new key makes one fetch.
        Jwk second = Jwk.ofPrivateKey(Crypto.generateRsaKey(2048)).withKeyId("k2");
        String k2 = new DefaultJwtEncoder(JwkSet.of(second)).encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
        assertEquals("There is no RS256 key with the token's key id, k2", refusal(decoder, k2));
        assertEquals(1, endpoint.asked.size(), "within the interval of the first fetch: no refetch");
        time.seconds += 31;
        endpoint.documents.put(uri, JwkSet.of(second, first).toJson());
        assertEquals("ada", decoder.decode(k2).getSubject());
        assertEquals(2, endpoint.asked.size());
        assertEquals("ada", decoder.decode(k1).getSubject());

        // Tokens with invented key ids do not become a stream of requests.
        for (int iter = 0 ; iter < 50 ; iter++) {
            String invented = part("{\"alg\":\"RS256\",\"kid\":\"x" + iter + "\"}") + "."
                    + part("{\"sub\":\"ada\"}") + ".AAAA";
            assertEquals("There is no RS256 key with the token's key id, x" + iter,
                    refusal(decoder, invented));
        }
        assertEquals(2, endpoint.asked.size());
        time.seconds += 31;
        refusal(decoder, part("{\"alg\":\"RS256\",\"kid\":\"x\"}") + "." + part("{}") + ".AAAA");
        assertEquals(3, endpoint.asked.size(), "once the interval has passed, one more");

        // The set goes out of date and is fetched by the next token.
        time.seconds += 301;
        assertEquals("ada", decoder.decode(k1).getSubject());
        assertEquals(4, endpoint.asked.size());

        // The issuer goes down: the keys there are go on being used, and the
        // fetch is retried no more than once an interval.
        endpoint.down = true;
        time.seconds += 301;
        assertEquals("ada", decoder.decode(k1).getSubject());
        assertEquals(5, endpoint.asked.size());
        assertEquals("ada", decoder.decode(k2).getSubject());
        assertEquals(5, endpoint.asked.size());
        time.seconds += 31;
        assertEquals("ada", decoder.decode(k1).getSubject());
        assertEquals(6, endpoint.asked.size());
        endpoint.down = false;
        time.seconds += 31;
        assertEquals("ada", decoder.decode(k1).getSubject());
        assertEquals(7, endpoint.asked.size());

        // Down from the start: there is nothing to verify with, and that is a
        // failure to judge -- not a bad token.
        Endpoint dead = new Endpoint();
        dead.down = true;
        RemoteJwkSet none = new RemoteJwkSet(uri, dead);
        none.setClock(time);
        DefaultJwtDecoder blind = at(DefaultJwtDecoder.withJwkSource(none).build(), time);
        JwtException failed = assertThrows(JwtException.class, () -> blind.decode(k1));
        assertFalse(failed instanceof BadJwtException, failed.toString());
        assertEquals("Could not get the keys to verify the token with: GET " + uri
                + " answered 503", failed.getMessage());
    }

    @Test
    @DisplayName("One request fetches the keys while the others carry on with what they have")
    void singleFlight() throws Exception {
        final Moving time = new Moving();
        final String uri = "https://id.example.com/jwks";
        final String document = JwkSet.of(rsa().withKeyId("k1")).toJson();
        final java.util.concurrent.CountDownLatch entered = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicInteger calls =
                new java.util.concurrent.atomic.AtomicInteger();
        final RemoteJwkSet remote = new RemoteJwkSet(uri, new RemoteJwkSet.Fetcher() {
            @Override
            public String fetch(String address) throws IOException {
                if (calls.incrementAndGet() == 2) {
                    entered.countDown();
                    try {
                        release.await();
                    } catch (InterruptedException err) {
                        throw new IOException(err);
                    }
                }
                return document;
            }
        });
        remote.setClock(time);
        List<Jwk> first = remote.getKeys();
        assertEquals(1, calls.get());
        time.seconds += 301;
        // One thread starts the refetch and is held inside it.
        final Object[] slow = new Object[1];
        Thread fetching = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    slow[0] = remote.getKeys();
                } catch (IOException err) {
                    slow[0] = err;
                }
            }
        });
        fetching.start();
        entered.await();
        // Everybody else gets the keys there are, at once, and asks nobody.
        for (int iter = 0 ; iter < 20 ; iter++) {
            assertSame(first, remote.getKeys());
            assertSame(first, remote.refresh());
        }
        assertEquals(2, calls.get());
        release.countDown();
        fetching.join();
        assertTrue(slow[0] instanceof List, String.valueOf(slow[0]));
        assertNotEquals(null, slow[0]);
        assertSame(slow[0], remote.getKeys());
        assertEquals(2, calls.get());
    }

    @Test
    @DisplayName("Discovery: a decoder from an issuer's metadata, which must be its own")
    void discovery() throws Exception {
        Moving time = new Moving();
        Endpoint endpoint = new Endpoint();
        String issuer = "https://id.example.com/tenant-a";
        Jwk key = p256();
        endpoint.documents.put("https://id.example.com/jwks/a", JwkSet.of(key).toJson());
        // Only the RFC 8414 location answers: the two before it are tried first.
        endpoint.documents.put("https://id.example.com/.well-known/oauth-authorization-server/tenant-a",
                "{\"issuer\":\"" + issuer + "\",\"jwks_uri\":\"https://id.example.com/jwks/a\","
                + "\"id_token_signing_alg_values_supported\":[\"RS256\",\"ES256\",\"EdDSA\",\"none\"]}");
        JwtDecoder decoder = JwtDecoders.fromIssuerLocation(issuer, endpoint);
        assertEquals(Arrays.asList(issuer + "/.well-known/openid-configuration",
                "https://id.example.com/.well-known/openid-configuration/tenant-a",
                "https://id.example.com/.well-known/oauth-authorization-server/tenant-a"),
                endpoint.asked);
        JwtEncoder encoder = new DefaultJwtEncoder(JwkSet.of(key));
        long now = System.currentTimeMillis() / 1000L;
        String token = encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                .issuer(issuer).subject("ada").expiresAt(now + 300).build())).getTokenValue();
        assertEquals("ada", decoder.decode(token).getSubject());
        assertEquals(4, endpoint.asked.size(), "the keys were fetched by the first token");
        // The decoder it makes requires the issuer.
        assertEquals("The iss claim is not valid", refusal(decoder, encoder.encode(
                JwtEncoderParameters.from(JwtClaimsSet.builder().issuer("https://id.example.com/tenant-b")
                        .subject("ada").expiresAt(now + 300).build())).getTokenValue()));
        // "none" in the metadata's list is not an algorithm, whoever lists it.
        assertEquals("The token's algorithm, none, is not one this decoder accepts", refusal(decoder,
                part("{\"alg\":\"none\"}") + "." + part("{\"iss\":\"" + issuer + "\"}") + ".AAAA"));

        // Metadata that names another issuer: a tenant served another's.
        Endpoint confused = new Endpoint();
        confused.documents.put("https://id.example.com/tenant-b/.well-known/openid-configuration",
                "{\"issuer\":\"" + issuer + "\",\"jwks_uri\":\"https://id.example.com/jwks/a\"}");
        assertEquals("The Issuer \"" + issuer + "\" provided in the configuration metadata did not "
                + "match the requested issuer \"https://id.example.com/tenant-b\"",
                assertThrows(IllegalArgumentException.class, () -> JwtDecoders.fromIssuerLocation(
                        "https://id.example.com/tenant-b", confused)).getMessage());
        Endpoint keyless = new Endpoint();
        keyless.documents.put("https://plain.example.com/.well-known/openid-configuration",
                "{\"issuer\":\"https://plain.example.com\"}");
        assertEquals("The metadata of https://plain.example.com has no jwks_uri",
                assertThrows(IllegalArgumentException.class, () -> JwtDecoders.fromIssuerLocation(
                        "https://plain.example.com", keyless)).getMessage());
        assertEquals(1, keyless.asked.size() - 0);
        Endpoint absent = new Endpoint();
        assertEquals("Unable to resolve the Configuration with the provided Issuer of "
                + "\"https://gone.example.com\": GET https://gone.example.com/.well-known/"
                + "oauth-authorization-server answered 404", assertThrows(
                        IllegalArgumentException.class, () -> JwtDecoders.fromIssuerLocation(
                                "https://gone.example.com", absent)).getMessage());
        assertEquals(2, absent.asked.size(), "an issuer without a path has two places, not three");
    }
}
