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
package com.codename1.backend.security.webauthn;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.codename1.backend.Base64Url;
import com.codename1.backend.Json;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/// The two ceremonies as [WebAuthnRelyingPartyOperations] performs them:
/// against the specification's own test vectors, which are bytes this code
/// did not produce, and against a software authenticator that can be made to
/// answer wrongly in each way a ceremony must refuse.
class WebAuthnCeremonyTest {
    private static final String RP = "example.org";
    private static final String ORIGIN = "https://example.org";

    private final InMemoryPublicKeyCredentialUserEntityRepository users =
            new InMemoryPublicKeyCredentialUserEntityRepository();
    private final InMemoryUserCredentialRepository credentials =
            new InMemoryUserCredentialRepository();

    private WebAuthnRelyingPartyOperations operations(String... origins) {
        return new WebAuthnRelyingPartyOperations(new PublicKeyCredentialRpEntity(RP, "Example"),
                Arrays.asList(origins.length == 0 ? new String[] {ORIGIN} : origins), users,
                credentials);
    }

    private interface Ceremony {
        void run() throws Exception;
    }

    /// The reason `ceremony` is refused for; fails when it is not refused, or
    /// by anything but a ceremony's own exception.
    private static String refusal(Ceremony ceremony) {
        try {
            ceremony.run();
        } catch (WebAuthnException refused) {
            assertNotNull(refused.getMessage());
            return refused.getReason();
        } catch (Throwable other) {
            return fail("refused with " + other, other);
        }
        return fail("the ceremony was accepted");
    }

    private static String message(Ceremony ceremony) {
        try {
            ceremony.run();
        } catch (WebAuthnException refused) {
            return refused.getMessage();
        } catch (Throwable other) {
            return fail("refused with " + other, other);
        }
        return fail("the ceremony was accepted");
    }

    private static String b64(String hex) {
        return Base64Url.encode(W3cTestVectors.hex(hex));
    }

    // ------------------------------------- the specification's test vectors

    private PublicKeyCredentialUserEntity vectorUser() {
        return users.save(new PublicKeyCredentialUserEntity("ada", new byte[] {1, 2, 3, 4}, "Ada"));
    }

    private static PublicKeyCredentialCreationOptions creation(String[] vector,
            PublicKeyCredentialUserEntity user, String verification) {
        return new PublicKeyCredentialCreationOptions(new PublicKeyCredentialRpEntity(RP, "Example"),
                user, W3cTestVectors.hex(vector[1]), 300000, new ArrayList<byte[]>(),
                new ArrayList<List<String>>(), null, "required", verification);
    }

    private static Map<String, Object> attested(String[] vector) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("clientDataJSON", b64(vector[2]));
        response.put("attestationObject", b64(vector[3]));
        Map<String, Object> credential = new LinkedHashMap<String, Object>();
        credential.put("id", b64(vector[4]));
        credential.put("rawId", b64(vector[4]));
        credential.put("type", "public-key");
        credential.put("response", response);
        return credential;
    }

    private static PublicKeyCredentialRequestOptions request(String[] vector, byte[] userId,
                                                             String verification) {
        List<byte[]> allow = new ArrayList<byte[]>();
        List<List<String>> transports = new ArrayList<List<String>>();
        if (userId != null) {
            allow.add(W3cTestVectors.hex(vector[4]));
            transports.add(new ArrayList<String>());
        }
        return new PublicKeyCredentialRequestOptions(W3cTestVectors.hex(vector[5]), 300000, RP,
                allow, transports, verification, userId);
    }

    private static Map<String, Object> asserted(String[] vector) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("clientDataJSON", b64(vector[7]));
        response.put("authenticatorData", b64(vector[6]));
        response.put("signature", b64(vector[8]));
        Map<String, Object> credential = new LinkedHashMap<String, Object>();
        credential.put("id", b64(vector[4]));
        credential.put("rawId", b64(vector[4]));
        credential.put("type", "public-key");
        credential.put("response", response);
        return credential;
    }

    @ParameterizedTest
    @ValueSource(strings = {"none-es256", "packed-self-es256", "none-es256-long-credential-id"})
    @DisplayName("W3C vectors: a registration and the sign-in of the same credential verify")
    void specificationVectors(String name) {
        String[] vector = W3cTestVectors.get(name);
        WebAuthnRelyingPartyOperations ops = operations();
        PublicKeyCredentialUserEntity user = vectorUser();
        CredentialRecord made = ops.registerCredential(creation(vector, user, "preferred"),
                attested(vector), "  the vector  ");
        assertArrayEquals(W3cTestVectors.hex(vector[4]), made.getCredentialId());
        assertArrayEquals(user.getId(), made.getUserEntityUserId());
        assertEquals(CoseKey.ES256, made.getAlgorithm());
        assertEquals("the vector", made.getLabel());
        assertEquals(0, made.getSignatureCount());
        // Stored, and found again by its id and by its user.
        assertNotNull(credentials.findByCredentialId(made.getCredentialId()));
        assertEquals(1, credentials.findByUserId(user.getId()).size());

        WebAuthnRelyingPartyOperations.Assertion signedIn = ops.authenticate(
                request(vector, user.getId(), "preferred"), asserted(vector));
        assertEquals("ada", signedIn.getUser().getName());
        assertArrayEquals(made.getCredentialId(), signedIn.getCredential().getCredentialId());

        // The instrument is not vacuous: one byte of the signature, and one of
        // what was signed, each refuse it.
        Map<String, Object> forged = asserted(vector);
        byte[] signature = W3cTestVectors.hex(vector[8]);
        signature[signature.length - 1] ^= 1;
        ((Map<String, Object>) forged.get("response")).put("signature",
                Base64Url.encode(signature));
        assertEquals(WebAuthnException.SIGNATURE_INVALID, refusal(() -> ops.authenticate(
                request(vector, user.getId(), "preferred"), forged)));
        Map<String, Object> altered = asserted(vector);
        byte[] authData = W3cTestVectors.hex(vector[6]);
        authData[36] ^= 1;
        ((Map<String, Object>) altered.get("response")).put("authenticatorData",
                Base64Url.encode(authData));
        assertEquals(WebAuthnException.SIGNATURE_INVALID, refusal(() -> ops.authenticate(
                request(vector, user.getId(), "preferred"), altered)));
    }

    @Test
    @DisplayName("W3C vector flags: the none vector did not verify the user, the packed one did")
    void specificationVectorFlags() {
        // none-es256 was made without user verification, in both ceremonies.
        String[] none = W3cTestVectors.get("none-es256");
        PublicKeyCredentialUserEntity user = vectorUser();
        assertEquals(WebAuthnException.USER_NOT_VERIFIED, refusal(() -> operations()
                .registerCredential(creation(none, user, "required"), attested(none), null)));
        CredentialRecord made = operations().registerCredential(creation(none, user, "preferred"),
                attested(none), null);
        assertFalse(made.isUvInitialized());
        assertTrue(made.isBackupEligible() && made.isBackupState());
        assertEquals(WebAuthnException.USER_NOT_VERIFIED, refusal(() -> operations().authenticate(
                request(none, user.getId(), "required"), asserted(none))));
        assertFalse(operations().authenticate(request(none, user.getId(), "preferred"),
                asserted(none)).isUserVerified());
        // packed-self-es256 was, at registration.
        String[] packed = W3cTestVectors.get("packed-self-es256");
        assertTrue(operations().registerCredential(creation(packed, user, "required"),
                attested(packed), null).isUvInitialized());
    }

    @Test
    @DisplayName("W3C vectors: a ceremony in a frame of another origin is refused unless allowed")
    void specificationCrossOrigin() {
        for (String name : new String[] {"none-es256-crossOrigin", "none-es256-topOrigin"}) {
            String[] vector = W3cTestVectors.get(name);
            PublicKeyCredentialUserEntity user = vectorUser();
            assertEquals(WebAuthnException.CROSS_ORIGIN, refusal(() -> operations()
                    .registerCredential(creation(vector, user, "preferred"), attested(vector),
                            null)), name);
            WebAuthnRelyingPartyOperations ops = operations();
            ops.setAllowCrossOrigin(true);
            ops.registerCredential(creation(vector, user, "preferred"), attested(vector), null);
            ops.authenticate(request(vector, user.getId(), "preferred"), asserted(vector));
        }
    }

    @ParameterizedTest
    @CsvSource({"packed-es256,packed,true", "packed-rs256,packed,true", "tpm-es256,tpm,false",
        "android-key-es256,android-key,false", "apple-es256,apple,false",
        "fido-u2f-es256,fido-u2f,false"})
    @DisplayName("W3C vectors: an attestation this server does not verify is refused by name, or accepted as none when told to")
    void unverifiedAttestation(String name, String format, boolean chain) {
        String[] vector = W3cTestVectors.get(name);
        PublicKeyCredentialUserEntity user = vectorUser();
        WebAuthnRelyingPartyOperations strict = operations();
        assertEquals(WebAuthnException.UNSUPPORTED_ATTESTATION, refusal(() -> strict
                .registerCredential(creation(vector, user, "preferred"), attested(vector), null)));
        String said = message(() -> strict.registerCredential(creation(vector, user, "preferred"),
                attested(vector), null));
        assertTrue(said.contains("\"" + format + "\"") && said.contains(
                "allowUnverifiedAttestation(true)"), said);
        assertEquals(chain, said.contains("with a certificate chain"), said);
        assertNull(credentials.findByCredentialId(W3cTestVectors.hex(vector[4])));

        WebAuthnRelyingPartyOperations lenient = operations();
        lenient.setAllowUnverifiedAttestation(true);
        CredentialRecord made = lenient.registerCredential(creation(vector, user, "preferred"),
                attested(vector), null);
        assertEquals(name.contains("rs256") ? CoseKey.RS256 : CoseKey.ES256, made.getAlgorithm());
        // The credential itself is as good as any: its sign-in verifies -- an
        // RS256 one among them, against a key and a signature made elsewhere.
        lenient.authenticate(request(vector, user.getId(), "preferred"), asserted(vector));
    }

    @ParameterizedTest
    @CsvSource({"packed-es384,-35", "packed-eddsa,-8"})
    @DisplayName("W3C vectors: a credential of an algorithm this server does not verify is refused, saying which")
    void unsupportedAlgorithm(String name, String algorithm) {
        String[] vector = W3cTestVectors.get(name);
        PublicKeyCredentialUserEntity user = vectorUser();
        WebAuthnRelyingPartyOperations ops = operations();
        // Even when any attestation would do: it is the key that cannot be used.
        ops.setAllowUnverifiedAttestation(true);
        assertEquals(WebAuthnException.UNSUPPORTED_ALGORITHM, refusal(() -> ops
                .registerCredential(creation(vector, user, "preferred"), attested(vector), null)));
        String said = message(() -> ops.registerCredential(creation(vector, user, "preferred"),
                attested(vector), null));
        assertTrue(said.contains("COSE algorithm " + algorithm) && said.contains("ES256 (-7)")
                && said.contains("RS256 (-257)"), said);
    }

    // ----------------------------------------------- a software authenticator

    private Map<String, Object> register(WebAuthnRelyingPartyOperations ops,
            SoftAuthenticator authenticator, String username) throws Exception {
        PublicKeyCredentialCreationOptions options =
                ops.createPublicKeyCredentialCreationOptions(username);
        Map<String, Object> answer = roundTrip(authenticator.create(roundTrip(options.toMap())));
        ops.registerCredential(PublicKeyCredentialCreationOptions.fromMap(
                roundTrip(options.toMap())), answer, "test");
        return answer;
    }

    /// Through JSON and back, as everything a client sends and a session keeps
    /// has been.
    private static Map<String, Object> roundTrip(Map<String, Object> value) throws Exception {
        return Json.parseObject(Json.write(value));
    }

    private WebAuthnRelyingPartyOperations.Assertion signIn(WebAuthnRelyingPartyOperations ops,
            SoftAuthenticator authenticator, String username) throws Exception {
        PublicKeyCredentialRequestOptions options = ops.createCredentialRequestOptions(username);
        return ops.authenticate(PublicKeyCredentialRequestOptions.fromMap(roundTrip(
                options.toStoredMap())), roundTrip(authenticator.get(roundTrip(options.toMap()))));
    }

    @ParameterizedTest
    @CsvSource({"false,none,false", "false,packed,false", "true,none,false", "true,packed,false",
        "false,none,true", "true,packed,true"})
    @DisplayName("ES256 and RS256, with none and packed self attestation, register and sign in")
    void softAuthenticator(boolean rsa, String format, boolean indefinite) throws Exception {
        WebAuthnRelyingPartyOperations ops = operations();
        SoftAuthenticator authenticator = new SoftAuthenticator(rsa, RP, ORIGIN);
        authenticator.format = format;
        authenticator.indefinite = indefinite;
        register(ops, authenticator, "ada");
        CredentialRecord stored = credentials.findByCredentialId(authenticator.credentialId);
        assertEquals(rsa ? CoseKey.RS256 : CoseKey.ES256, stored.getAlgorithm());
        assertEquals(Arrays.asList("internal", "hybrid"), stored.getTransports());
        assertTrue(stored.isUvInitialized());
        assertEquals("test", stored.getLabel());

        // Without saying who: the answer names the user.
        WebAuthnRelyingPartyOperations.Assertion first = signIn(ops, authenticator, null);
        assertEquals("ada", first.getUser().getName());
        assertTrue(first.isUserVerified());
        assertEquals(1, first.getCredential().getSignatureCount());
        assertEquals(1, credentials.findByCredentialId(authenticator.credentialId)
                .getSignatureCount());
        // Saying who first: the options list the credential, and the answer
        // need not name anybody.
        authenticator.sendUserHandle = false;
        WebAuthnRelyingPartyOperations.Assertion second = signIn(ops, authenticator, "ADA");
        assertEquals("ada", second.getUser().getName());
        assertEquals(2, second.getCredential().getSignatureCount());
    }

    @Test
    @DisplayName("the options are the WebAuthn JSON a client takes, and a challenge is 32 new bytes")
    void optionsJson() throws Exception {
        WebAuthnRelyingPartyOperations ops = operations();
        ops.setAuthenticatorAttachment("platform");
        ops.setUserVerification("required");
        SoftAuthenticator authenticator = new SoftAuthenticator(false, RP, ORIGIN);
        register(ops, authenticator, "ada");
        Map<String, Object> creation = roundTrip(
                ops.createPublicKeyCredentialCreationOptions("ada").toMap());
        assertEquals("[rp, user, challenge, pubKeyCredParams, timeout, excludeCredentials, "
                + "authenticatorSelection, attestation, extensions]",
                String.valueOf(new ArrayList<String>(creation.keySet())));
        assertEquals("{id=example.org, name=Example}", String.valueOf(creation.get("rp")));
        Map user = (Map) creation.get("user");
        assertEquals("ada ada", user.get("name") + " " + user.get("displayName"));
        assertEquals(32, Base64Url.decode((String) user.get("id")).length);
        assertEquals(32, Base64Url.decode((String) creation.get("challenge")).length);
        assertFalse(((String) creation.get("challenge")).contains("="));
        assertEquals("[{type=public-key, alg=-7}, {type=public-key, alg=-257}]",
                String.valueOf(creation.get("pubKeyCredParams")));
        assertEquals("300000", String.valueOf(creation.get("timeout")));
        assertEquals("[{type=public-key, id=" + Base64Url.encode(authenticator.credentialId)
                + ", transports=[internal, hybrid]}]",
                String.valueOf(creation.get("excludeCredentials")));
        assertEquals("{authenticatorAttachment=platform, residentKey=required, "
                + "requireResidentKey=true, userVerification=required}",
                String.valueOf(creation.get("authenticatorSelection")));
        assertEquals("none {credProps=true}", creation.get("attestation") + " "
                + creation.get("extensions"));
        // The same user keeps the same handle; every ceremony has its own challenge.
        Map<String, Object> again = ops.createPublicKeyCredentialCreationOptions("Ada").toMap();
        assertEquals(user.get("id"), ((Map) again.get("user")).get("id"));
        assertFalse(creation.get("challenge").equals(again.get("challenge")));

        Map<String, Object> nobody = roundTrip(ops.createCredentialRequestOptions(null).toMap());
        assertEquals("[challenge, timeout, rpId, allowCredentials, userVerification]",
                String.valueOf(new ArrayList<String>(nobody.keySet())));
        assertEquals("example.org [] required 300000", nobody.get("rpId") + " "
                + nobody.get("allowCredentials") + " " + nobody.get("userVerification") + " "
                + nobody.get("timeout"));
        assertEquals(32, Base64Url.decode((String) nobody.get("challenge")).length);
        Map<String, Object> named = ops.createCredentialRequestOptions("ada").toMap();
        assertEquals("[{type=public-key, id=" + Base64Url.encode(authenticator.credentialId)
                + ", transports=[internal, hybrid]}]", String.valueOf(named.get("allowCredentials")));
        // The user's handle stays on the server: it is not in what is sent.
        assertFalse(Json.write(named).contains(Base64Url.encode(authenticator.userHandle)));
        // A name with no passkey looks like no name at all.
        assertEquals("[]", String.valueOf(ops.createCredentialRequestOptions("nobody").toMap()
                .get("allowCredentials")));
        assertNull(ops.createCredentialRequestOptions("nobody").getUserId());
    }

    /// Makes an authenticator wrong in one way.
    private interface Fault {
        void apply(SoftAuthenticator authenticator) throws Exception;
    }

    private String registrationRefusal(Fault fault) throws Exception {
        return registrationRefusal(operations(), "packed", fault);
    }

    private String registrationRefusal(WebAuthnRelyingPartyOperations ops, String format,
                                       Fault fault) throws Exception {
        SoftAuthenticator authenticator = new SoftAuthenticator(false, RP, ORIGIN);
        authenticator.format = format;
        fault.apply(authenticator);
        String reason = refusal(() -> register(ops, authenticator, "ada"));
        // And nothing was stored for it.
        assertNull(credentials.findByCredentialId(authenticator.credentialId), reason);
        return reason;
    }

    @Test
    @DisplayName("a registration is refused for each thing wrong with it, and for that reason")
    void registrationRefusals() throws Exception {
        // The instrument: with nothing wrong it registers.
        register(operations(), new SoftAuthenticator(false, RP, ORIGIN), "ada");

        assertEquals(WebAuthnException.ORIGIN_MISMATCH,
                registrationRefusal(a -> a.origin = "https://evil.example"));
        // An origin is compared whole: a prefix, a suffix, another scheme, a
        // port and a trailing slash are each another origin.
        for (String origin : new String[] {"https://example.org.evil.example",
            "https://evil.example.org", "http://example.org", "https://example.org:8443",
            "https://example.org/", "HTTPS://EXAMPLE.ORG", "example.org", ""}) {
            assertEquals(WebAuthnException.ORIGIN_MISMATCH,
                    registrationRefusal(a -> a.origin = origin), origin);
        }
        assertEquals(WebAuthnException.RP_ID_MISMATCH,
                registrationRefusal(a -> a.rpId = "evil.example"));
        assertEquals(WebAuthnException.CHALLENGE_MISMATCH, registrationRefusal(
                a -> a.challengeOverride = Base64Url.encode(SoftAuthenticator.random(32))));
        assertEquals(WebAuthnException.CHALLENGE_MISMATCH,
                registrationRefusal(a -> a.challengeOverride = ""));
        assertEquals(WebAuthnException.CHALLENGE_MISMATCH,
                registrationRefusal(a -> a.challengeOverride = "not base64url!"));
        assertEquals(WebAuthnException.WRONG_TYPE,
                registrationRefusal(a -> a.typeOverride = "webauthn.get"));
        assertEquals(WebAuthnException.WRONG_TYPE,
                registrationRefusal(a -> a.typeOverride = "payment.get"));
        assertEquals(WebAuthnException.CROSS_ORIGIN,
                registrationRefusal(a -> a.crossOrigin = Boolean.TRUE));
        assertEquals(WebAuthnException.USER_NOT_PRESENT,
                registrationRefusal(a -> a.flags = SoftAuthenticator.UV));
        WebAuthnRelyingPartyOperations verifying = operations();
        verifying.setUserVerification("required");
        assertEquals(WebAuthnException.USER_NOT_VERIFIED, registrationRefusal(verifying, "none",
                a -> a.flags = SoftAuthenticator.UP));
        assertEquals(WebAuthnException.BACKUP_STATE_INVALID, registrationRefusal(
                a -> a.flags = SoftAuthenticator.UP | SoftAuthenticator.BS));
        // Signed by another key than the one it attests.
        assertEquals(WebAuthnException.ATTESTATION_INVALID,
                registrationRefusal(a -> a.signer = SoftAuthenticator.newKeys(false)));
        // An algorithm this server does not verify, and a key that is not of
        // the algorithm it names.
        assertEquals(WebAuthnException.UNSUPPORTED_ALGORITHM,
                registrationRefusal(a -> a.coseAlgorithm = Long.valueOf(-8)));
        assertEquals(WebAuthnException.UNSUPPORTED_ALGORITHM,
                registrationRefusal(a -> a.coseAlgorithm = Long.valueOf(-36)));
        assertEquals(WebAuthnException.INVALID_KEY,
                registrationRefusal(a -> a.coseAlgorithm = Long.valueOf(-257)));
        // Bytes after what the flags announce, and extensions that are not a map.
        assertEquals(WebAuthnException.MALFORMED_AUTHENTICATOR_DATA,
                registrationRefusal(a -> a.trailing = new byte[] {0}));
        assertEquals(WebAuthnException.MALFORMED_AUTHENTICATOR_DATA,
                registrationRefusal(a -> a.extensions = new byte[] {0x01}));
        assertEquals(WebAuthnException.MALFORMED_CBOR,
                registrationRefusal(a -> a.extensions = new byte[] {(byte) 0xa1, 0x01}));
        assertEquals(WebAuthnException.MALFORMED_AUTHENTICATOR_DATA,
                registrationRefusal(a -> a.credentialId = new byte[1024]));
        // Extensions that are a map are read past, and change nothing.
        register(operations(), with(a -> a.extensions = SoftAuthenticator.cbor(
                Collections.singletonMap("credProtect", Long.valueOf(2)), false)), "ada");
    }

    private static SoftAuthenticator with(Fault fault) throws Exception {
        SoftAuthenticator authenticator = new SoftAuthenticator(false, RP, ORIGIN);
        fault.apply(authenticator);
        return authenticator;
    }

    @Test
    @DisplayName("an answer that is not the JSON of a credential is refused as malformed")
    void malformedAnswers() throws Exception {
        WebAuthnRelyingPartyOperations ops = operations();
        SoftAuthenticator authenticator = new SoftAuthenticator(false, RP, ORIGIN);
        PublicKeyCredentialCreationOptions options =
                ops.createPublicKeyCredentialCreationOptions("ada");
        Map<String, Object> good = authenticator.create(options.toMap());
        List<Map<String, Object>> broken = new ArrayList<Map<String, Object>>();
        for (String field : new String[] {"clientDataJSON", "attestationObject"}) {
            // Missing, a number, a list, text that is not base64url, and empty.
            for (Object value : new Object[] {null, Long.valueOf(7), Arrays.asList("x"), "a*b",
                "a", ""}) {
                Map<String, Object> copy = roundTrip(good);
                if (value == null) {
                    ((Map) copy.get("response")).remove(field);
                } else {
                    ((Map<String, Object>) copy.get("response")).put(field, value);
                }
                broken.add(copy);
            }
        }
        Map<String, Object> copy = roundTrip(good);
        copy.put("type", "password");
        broken.add(copy);
        copy = roundTrip(good);
        copy.put("response", "x");
        broken.add(copy);
        copy = roundTrip(good);
        copy.remove("response");
        broken.add(copy);
        copy = roundTrip(good);
        copy.put("rawId", Base64Url.encode(new byte[] {9, 9}));
        broken.add(copy);
        copy = roundTrip(good);
        copy.put("rawId", Long.valueOf(7));
        broken.add(copy);
        // Client data that is not a JSON object.
        for (String json : new String[] {"[]", "\"x\"", "{", "nonsense", "7"}) {
            copy = roundTrip(good);
            ((Map<String, Object>) copy.get("response")).put("clientDataJSON",
                    Base64Url.encode(json.getBytes("UTF-8")));
            broken.add(copy);
        }
        for (Map<String, Object> answer : broken) {
            assertEquals(WebAuthnException.MALFORMED, refusal(() -> ops.registerCredential(options,
                    answer, null)), String.valueOf(answer));
        }
        assertEquals(WebAuthnException.MALFORMED, refusal(() -> ops.registerCredential(options,
                null, null)));
        // An attestation object that is CBOR and not an attestation object.
        for (String cbor : new String[] {"00", "80", "a0", "a163666d74646e6f6e65",
            "a363666d7401" + "6761747453746d74a0" + "686175746844617461" + "40"}) {
            copy = roundTrip(good);
            ((Map<String, Object>) copy.get("response")).put("attestationObject",
                    Base64Url.encode(W3cTestVectors.hex(cbor)));
            Map<String, Object> answer = copy;
            assertEquals(WebAuthnException.MALFORMED, refusal(() -> ops.registerCredential(options,
                    answer, null)), cbor);
        }
        // No options: nothing was waiting.
        assertEquals(WebAuthnException.NO_CHALLENGE, refusal(() -> ops.registerCredential(null,
                good, null)));
        assertEquals(WebAuthnException.NO_CHALLENGE, refusal(() -> ops.authenticate(null, good)));
        // And the untouched answer still registers.
        ops.registerCredential(options, roundTrip(good), null);
    }

    @Test
    @DisplayName("a credential id is one user's: a second registration of it is refused, whoever asks")
    void duplicateCredentialId() throws Exception {
        WebAuthnRelyingPartyOperations ops = operations();
        SoftAuthenticator adas = new SoftAuthenticator(false, RP, ORIGIN);
        register(ops, adas, "ada");
        // Eve's authenticator claims the id of ada's credential, with a key of
        // its own: were it stored, eve would sign in as whoever the id finds.
        SoftAuthenticator eves = new SoftAuthenticator(false, RP, ORIGIN);
        eves.credentialId = adas.credentialId.clone();
        assertEquals(WebAuthnException.CREDENTIAL_EXISTS, refusal(() -> register(ops, eves, "eve")));
        // Nor a second time for ada herself.
        assertEquals(WebAuthnException.CREDENTIAL_EXISTS, refusal(() -> register(ops, adas, "ada")));
        CredentialRecord stored = credentials.findByCredentialId(adas.credentialId);
        assertArrayEquals(users.findByUsername("ada").getId(), stored.getUserEntityUserId());
        assertArrayEquals(CoseKey.of(Cbor.decode(SoftAuthenticator.cbor(adas.coseKey(), false)))
                .getPublicKey(), stored.getPublicKey());
        assertEquals(0, credentials.findByUserId(users.findByUsername("eve").getId()).size());
    }

    private String signInRefusal(Fault fault) throws Exception {
        WebAuthnRelyingPartyOperations ops = operations();
        SoftAuthenticator authenticator = new SoftAuthenticator(false, RP, ORIGIN);
        register(ops, authenticator, "user-" + users.hashCode() + "-" + System.nanoTime());
        // The instrument: before the fault it signs in.
        signIn(ops, authenticator, null);
        fault.apply(authenticator);
        long before = credentials.findByCredentialId(authenticator.credentialId) == null ? -1
                : credentials.findByCredentialId(authenticator.credentialId).getSignatureCount();
        String reason = refusal(() -> signIn(ops, authenticator, null));
        CredentialRecord after = credentials.findByCredentialId(authenticator.credentialId);
        if (after != null) {
            // A refused sign-in leaves the credential as it was.
            assertEquals(before, after.getSignatureCount(), reason);
        }
        return reason;
    }

    @Test
    @DisplayName("a sign-in is refused for each thing wrong with it, and for that reason")
    void signInRefusals() throws Exception {
        assertEquals(WebAuthnException.ORIGIN_MISMATCH,
                signInRefusal(a -> a.origin = "https://evil.example"));
        assertEquals(WebAuthnException.RP_ID_MISMATCH,
                signInRefusal(a -> a.rpId = "evil.example"));
        assertEquals(WebAuthnException.CHALLENGE_MISMATCH, signInRefusal(
                a -> a.challengeOverride = Base64Url.encode(SoftAuthenticator.random(32))));
        assertEquals(WebAuthnException.WRONG_TYPE,
                signInRefusal(a -> a.typeOverride = "webauthn.create"));
        assertEquals(WebAuthnException.CROSS_ORIGIN,
                signInRefusal(a -> a.crossOrigin = Boolean.TRUE));
        assertEquals(WebAuthnException.USER_NOT_PRESENT,
                signInRefusal(a -> a.flags = SoftAuthenticator.UV));
        // Signed by another key: somebody who knows the credential's id and
        // the user's handle, and holds no key of theirs.
        assertEquals(WebAuthnException.SIGNATURE_INVALID,
                signInRefusal(a -> a.signer = SoftAuthenticator.newKeys(false)));
        assertEquals(WebAuthnException.UNKNOWN_CREDENTIAL,
                signInRefusal(a -> a.credentialId = SoftAuthenticator.random(32)));
        assertEquals(WebAuthnException.UNKNOWN_CREDENTIAL,
                signInRefusal(a -> credentials.delete(a.credentialId)));
        // The answer names another user than the credential's, or nobody.
        assertEquals(WebAuthnException.USER_MISMATCH,
                signInRefusal(a -> a.userHandle = SoftAuthenticator.random(32)));
        assertEquals(WebAuthnException.USER_MISMATCH,
                signInRefusal(a -> a.sendUserHandle = false));
        // The credential was not eligible for backup when it was made.
        assertEquals(WebAuthnException.BACKUP_STATE_INVALID, signInRefusal(
                a -> a.flags = SoftAuthenticator.UP | SoftAuthenticator.UV | SoftAuthenticator.BE));
        assertEquals(WebAuthnException.BACKUP_STATE_INVALID, signInRefusal(
                a -> a.flags = SoftAuthenticator.UP | SoftAuthenticator.BS));
        assertEquals(WebAuthnException.MALFORMED_AUTHENTICATOR_DATA,
                signInRefusal(a -> a.trailing = new byte[] {0}));
        // The counter: back to what it was, and further back.
        assertEquals(WebAuthnException.COUNTER_REGRESSION, signInRefusal(a -> a.counts = false));
        assertEquals(WebAuthnException.COUNTER_REGRESSION, signInRefusal(a -> a.counter = -1));

        WebAuthnRelyingPartyOperations verifying = operations();
        SoftAuthenticator unverified = new SoftAuthenticator(false, RP, ORIGIN);
        unverified.flags = SoftAuthenticator.UP;
        register(operations(), unverified, "grace");
        verifying.setUserVerification("required");
        assertEquals(WebAuthnException.USER_NOT_VERIFIED,
                refusal(() -> signIn(verifying, unverified, null)));
        assertFalse(signIn(operations(), unverified, null).isUserVerified());
    }

    @Test
    @DisplayName("what was signed is what is verified: a changed byte anywhere refuses the sign-in")
    void tampering() throws Exception {
        WebAuthnRelyingPartyOperations ops = operations();
        SoftAuthenticator authenticator = new SoftAuthenticator(false, RP, ORIGIN);
        register(ops, authenticator, "ada");
        PublicKeyCredentialRequestOptions options = ops.createCredentialRequestOptions(null);
        Map<String, Object> answer = authenticator.get(options.toMap());
        Map<String, Object> response = (Map<String, Object>) answer.get("response");
        // Authenticator data: the counter raised by one, and the UV flag cleared.
        byte[] authData = Base64Url.decode((String) response.get("authenticatorData"));
        byte[] raised = authData.clone();
        raised[36]++;
        byte[] unverified = authData.clone();
        unverified[32] &= ~SoftAuthenticator.UV;
        for (byte[] changed : new byte[][] {raised, unverified}) {
            Map<String, Object> copy = roundTrip(answer);
            ((Map<String, Object>) copy.get("response")).put("authenticatorData",
                    Base64Url.encode(changed));
            assertEquals(WebAuthnException.SIGNATURE_INVALID,
                    refusal(() -> ops.authenticate(options, copy)));
        }
        // Client data: the same JSON with a space after it, which every check
        // of its fields passes.
        String clientData = new String(Base64Url.decode((String) response.get("clientDataJSON")),
                "UTF-8");
        Map<String, Object> spaced = roundTrip(answer);
        ((Map<String, Object>) spaced.get("response")).put("clientDataJSON",
                Base64Url.encode((clientData + " ").getBytes("UTF-8")));
        assertEquals(WebAuthnException.SIGNATURE_INVALID,
                refusal(() -> ops.authenticate(options, spaced)));
        // A signature that is not one: empty bytes, a truncation, noise.
        byte[] signature = Base64Url.decode((String) response.get("signature"));
        for (byte[] bad : new byte[][] {{0}, Arrays.copyOf(signature, signature.length - 1),
            SoftAuthenticator.random(70)}) {
            Map<String, Object> copy = roundTrip(answer);
            ((Map<String, Object>) copy.get("response")).put("signature", Base64Url.encode(bad));
            assertEquals(WebAuthnException.SIGNATURE_INVALID,
                    refusal(() -> ops.authenticate(options, copy)));
        }
        // None of that moved the counter, and the untouched answer signs in.
        assertEquals(0, credentials.findByCredentialId(authenticator.credentialId)
                .getSignatureCount());
        ops.authenticate(options, roundTrip(answer));

        // The same at registration, where packed attestation signs the same two.
        SoftAuthenticator packed = new SoftAuthenticator(false, RP, ORIGIN);
        packed.format = "packed";
        PublicKeyCredentialCreationOptions creation =
                ops.createPublicKeyCredentialCreationOptions("ada");
        Map<String, Object> made = packed.create(creation.toMap());
        String createData = new String(Base64Url.decode((String) ((Map) made.get("response"))
                .get("clientDataJSON")), "UTF-8");
        Map<String, Object> respaced = roundTrip(made);
        ((Map<String, Object>) respaced.get("response")).put("clientDataJSON",
                Base64Url.encode((createData + " ").getBytes("UTF-8")));
        assertEquals(WebAuthnException.ATTESTATION_INVALID,
                refusal(() -> ops.registerCredential(creation, respaced, null)));
    }

    @Test
    @DisplayName("a counter that goes backwards refuses the sign-in and is reported; a counter of zero is no counter")
    void signatureCounter() throws Exception {
        WebAuthnRelyingPartyOperations ops = operations();
        final List<String> reported = new ArrayList<String>();
        ops.setSignatureCounterListener((record, received) -> reported.add(
                Base64Url.encode(record.getCredentialId()) + " " + record.getSignatureCount()
                        + " -> " + received));
        SoftAuthenticator authenticator = new SoftAuthenticator(false, RP, ORIGIN);
        register(ops, authenticator, "ada");
        signIn(ops, authenticator, null);
        signIn(ops, authenticator, null);
        authenticator.counter = 7;
        signIn(ops, authenticator, null);
        assertEquals(8, credentials.findByCredentialId(authenticator.credentialId)
                .getSignatureCount());
        assertTrue(reported.isEmpty(), reported.toString());
        // A copy of the key, still at an earlier count.
        authenticator.counter = 3;
        assertEquals(WebAuthnException.COUNTER_REGRESSION,
                refusal(() -> signIn(ops, authenticator, null)));
        // The same count again is a replay or a copy just the same.
        authenticator.counter = 7;
        assertEquals(WebAuthnException.COUNTER_REGRESSION,
                refusal(() -> signIn(ops, authenticator, null)));
        // And a counter that went back to zero after counting.
        authenticator.counter = 0;
        authenticator.counts = false;
        assertEquals(WebAuthnException.COUNTER_REGRESSION,
                refusal(() -> signIn(ops, authenticator, null)));
        String id = Base64Url.encode(authenticator.credentialId);
        assertEquals(Arrays.asList(id + " 8 -> 4", id + " 8 -> 8", id + " 8 -> 0"), reported);
        assertEquals(8, credentials.findByCredentialId(authenticator.credentialId)
                .getSignatureCount());

        // A passkey that keeps no counter sends zero every time, and signs in
        // every time.
        SoftAuthenticator synced = new SoftAuthenticator(false, RP, ORIGIN);
        synced.counts = false;
        register(ops, synced, "grace");
        signIn(ops, synced, null);
        signIn(ops, synced, null);
        assertEquals(0, credentials.findByCredentialId(synced.credentialId).getSignatureCount());
        assertEquals(3, reported.size());
    }

    @Test
    @DisplayName("a credential signs in its own user, and nobody else")
    void credentialOfAnotherUser() throws Exception {
        WebAuthnRelyingPartyOperations ops = operations();
        SoftAuthenticator adas = new SoftAuthenticator(false, RP, ORIGIN);
        SoftAuthenticator eves = new SoftAuthenticator(false, RP, ORIGIN);
        register(ops, adas, "ada");
        register(ops, eves, "eve");
        // Eve starts a sign-in as ada and answers with her own credential.
        assertEquals(WebAuthnException.CREDENTIAL_NOT_ALLOWED,
                refusal(() -> signIn(ops, eves, "ada")));
        // Eve answers a sign-in for nobody with her credential and ada's handle.
        eves.userHandle = adas.userHandle.clone();
        assertEquals(WebAuthnException.USER_MISMATCH, refusal(() -> signIn(ops, eves, null)));
        // Options that are for ada, and a credential that is listed in them but
        // is eve's: the stored owner decides, not the list.
        PublicKeyCredentialRequestOptions forAda = ops.createCredentialRequestOptions("ada");
        List<byte[]> allow = forAda.getAllowCredentials();
        allow.add(eves.credentialId);
        PublicKeyCredentialRequestOptions widened = new PublicKeyCredentialRequestOptions(
                forAda.getChallenge(), 300000, RP, allow, Arrays.asList(
                        (List<String>) new ArrayList<String>(), new ArrayList<String>()),
                "preferred", forAda.getUserId());
        eves.sendUserHandle = false;
        assertEquals(WebAuthnException.USER_MISMATCH,
                refusal(() -> ops.authenticate(widened, eves.get(widened.toMap()))));
        // Each still signs in as themselves.
        eves.sendUserHandle = true;
        eves.userHandle = users.findByUsername("eve").getId();
        assertEquals("eve", signIn(ops, eves, null).getUser().getName());
        assertEquals("ada", signIn(ops, adas, "ada").getUser().getName());
        // A user whose handle is gone has no account to sign in to.
        users.delete(adas.userHandle);
        assertEquals(WebAuthnException.ACCOUNT_UNAVAILABLE, refusal(() -> signIn(ops, adas, null)));
    }

    @Test
    @DisplayName("an Android application's origin is one more allowed origin, compared whole")
    void androidOrigin() throws Exception {
        String app = "android:apk-key-hash:" + Base64Url.encode(SoftAuthenticator.random(32));
        WebAuthnRelyingPartyOperations ops = operations(ORIGIN, app);
        SoftAuthenticator phone = new SoftAuthenticator(false, RP, app);
        register(ops, phone, "ada");
        assertEquals("ada", signIn(ops, phone, null).getUser().getName());
        // Another application's hash, and a server that was told of none.
        phone.origin = "android:apk-key-hash:" + Base64Url.encode(SoftAuthenticator.random(32));
        assertEquals(WebAuthnException.ORIGIN_MISMATCH, refusal(() -> signIn(ops, phone, null)));
        phone.origin = app;
        assertEquals(WebAuthnException.ORIGIN_MISMATCH,
                refusal(() -> signIn(operations(), phone, null)));
        String said = message(() -> signIn(operations(), phone, null));
        assertTrue(said.contains(app) && said.contains("[https://example.org]"), said);
    }

    @Test
    @DisplayName("what is configured wrongly is refused when it is configured")
    void configuration() {
        for (String origin : new String[] {"https://example.org/", "https://example.org/login",
            "example.org", "", "https://", "https://example.org?x"}) {
            assertThrows(IllegalArgumentException.class, () -> operations(origin), origin);
        }
        assertThrows(IllegalArgumentException.class, () -> new WebAuthnRelyingPartyOperations(
                new PublicKeyCredentialRpEntity(RP, null), new ArrayList<String>(), users,
                credentials));
        for (String id : new String[] {"https://example.org", "example.org:443", "example.org/",
            ""}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new PublicKeyCredentialRpEntity(id, "x"), id);
        }
        assertEquals("example.org", new PublicKeyCredentialRpEntity(RP, null).getName());
        WebAuthnRelyingPartyOperations ops = operations("http://localhost:8080");
        assertThrows(IllegalArgumentException.class, () -> ops.setUserVerification("always"));
        assertThrows(IllegalArgumentException.class, () -> ops.setResidentKey("yes"));
        assertThrows(IllegalArgumentException.class, () -> ops.setAuthenticatorAttachment("usb"));
        assertThrows(IllegalArgumentException.class, () -> ops.setTimeoutMillis(10));
        assertThrows(IllegalArgumentException.class,
                () -> ops.createPublicKeyCredentialCreationOptions(""));
        assertThrows(IllegalArgumentException.class,
                () -> new PublicKeyCredentialUserEntity("ada", new byte[65], null));
        // Stored options that are not options are nothing, not an exception.
        assertNull(PublicKeyCredentialCreationOptions.fromMap(null));
        assertNull(PublicKeyCredentialCreationOptions.fromMap(new LinkedHashMap<String, Object>()));
        assertNull(PublicKeyCredentialRequestOptions.fromMap(Collections.singletonMap("challenge",
                (Object) Long.valueOf(1))));
    }

    @Test
    @DisplayName("no truncation and no flipped bit of what an authenticator sends makes a ceremony do anything but refuse")
    void fuzz() throws Exception {
        assertTimeoutPreemptively(Duration.ofSeconds(180), () -> {
            WebAuthnRelyingPartyOperations ops = operations();
            SoftAuthenticator authenticator = new SoftAuthenticator(false, RP, ORIGIN);
            authenticator.format = "packed";
            authenticator.extensions = SoftAuthenticator.cbor(
                    Collections.singletonMap("credProtect", Long.valueOf(2)), false);
            PublicKeyCredentialCreationOptions creation =
                    ops.createPublicKeyCredentialCreationOptions("ada");
            Map<String, Object> made = authenticator.create(creation.toMap());
            byte[] attestation = Base64Url.decode((String) ((Map) made.get("response"))
                    .get("attestationObject"));
            int refused = 0;
            for (int length = 0 ; length < attestation.length ; length++) {
                refused += fuzzedRegistration(ops, creation, made,
                        Arrays.copyOf(attestation, length)) ? 0 : 1;
            }
            assertEquals(attestation.length, refused, "a truncated attestation registered");
            int accepted = 0;
            for (int bit = 0 ; bit < attestation.length * 8 ; bit++) {
                byte[] flipped = attestation.clone();
                flipped[bit / 8] ^= (byte) (1 << (bit % 8));
                if (fuzzedRegistration(ops, creation, made, flipped)) {
                    accepted++;
                }
            }
            // Packed attestation signs the authenticator data, so the only
            // flips that still register are outside it: there are none among
            // the bytes that mean anything, and the count says how few.
            assertTrue(accepted < 8, accepted + " flipped attestations registered");
            // The untouched one registers; then the same for a sign-in.
            ops.registerCredential(creation, roundTrip(made), null);
            PublicKeyCredentialRequestOptions options = ops.createCredentialRequestOptions(null);
            Map<String, Object> answer = authenticator.get(options.toMap());
            for (String field : new String[] {"authenticatorData", "signature",
                "clientDataJSON"}) {
                byte[] bytes = Base64Url.decode((String) ((Map) answer.get("response"))
                        .get(field));
                for (int length = 0 ; length < bytes.length ; length++) {
                    assertFalse(fuzzedSignIn(ops, options, answer, field,
                            Arrays.copyOf(bytes, length)), field + " truncated to " + length);
                }
                for (int bit = 0 ; bit < bytes.length * 8 ; bit++) {
                    byte[] flipped = bytes.clone();
                    flipped[bit / 8] ^= (byte) (1 << (bit % 8));
                    assertFalse(fuzzedSignIn(ops, options, answer, field, flipped),
                            field + " with bit " + bit + " flipped signed in");
                }
            }
            assertEquals(0, credentials.findByCredentialId(authenticator.credentialId)
                    .getSignatureCount());
            ops.authenticate(options, roundTrip(answer));
        });
    }

    private boolean fuzzedRegistration(WebAuthnRelyingPartyOperations ops,
            PublicKeyCredentialCreationOptions options, Map<String, Object> made,
            byte[] attestation) throws Exception {
        Map<String, Object> copy = roundTrip(made);
        copy.remove("id");
        copy.remove("rawId");
        ((Map<String, Object>) copy.get("response")).put("attestationObject",
                Base64Url.encode(attestation));
        try {
            CredentialRecord record = ops.registerCredential(options, copy, null);
            credentials.delete(record.getCredentialId());
            return true;
        } catch (WebAuthnException refused) {
            return false;
        }
    }

    private boolean fuzzedSignIn(WebAuthnRelyingPartyOperations ops,
            PublicKeyCredentialRequestOptions options, Map<String, Object> answer, String field,
            byte[] value) throws Exception {
        Map<String, Object> copy = roundTrip(answer);
        ((Map<String, Object>) copy.get("response")).put(field, Base64Url.encode(value));
        try {
            ops.authenticate(options, copy);
            return true;
        } catch (WebAuthnException refused) {
            return false;
        }
    }
}
