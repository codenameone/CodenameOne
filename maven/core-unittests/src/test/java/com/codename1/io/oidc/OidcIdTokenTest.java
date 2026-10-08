/*
 * Copyright (c) 2012-2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
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
package com.codename1.io.oidc;

import com.codename1.io.JSONParser;
import com.codename1.junit.UITestBase;
import com.codename1.security.Jwt;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.codename1.io.oidc.OidcTestSupport.await;
import static com.codename1.io.oidc.OidcTestSupport.text;
import static com.codename1.io.oidc.OidcTestSupport.utf8;
import static org.junit.jupiter.api.Assertions.*;

/**
 * What {@link OidcClient} asks of an authorization response and of an ID token before it
 * hands tokens over: the issuer the response names, the token's claims, and its signature
 * against the provider's keys.
 *
 * <p>Every key here is a TEST FIXTURE: made by the JDK when the class loads, used to sign the
 * tokens this test's own stub provider hands out, and gone with the JVM. None is, or ever
 * was, a key of anything.</p>
 */
public class OidcIdTokenTest extends UITestBase {

    private static final String ISSUER = "https://login.example.com";
    private static final String TOKEN_EP = ISSUER + "/oauth2/token";
    private static final String JWKS = ISSUER + "/oauth2/jwks";
    private static final String CLIENT = "app";
    private static final String REDIRECT = "com.example.app:/cb";

    /** Test fixture keys; see the class comment. */
    private static KeyPair rsa;
    private static KeyPair rsaOther;
    private static KeyPair p256;
    private static KeyPair p384;

    @BeforeAll
    static void makeFixtureKeys() throws Exception {
        KeyPairGenerator r = KeyPairGenerator.getInstance("RSA");
        r.initialize(2048);
        rsa = r.generateKeyPair();
        rsaOther = r.generateKeyPair();
        KeyPairGenerator e = KeyPairGenerator.getInstance("EC");
        e.initialize(new ECGenParameterSpec("secp256r1"));
        p256 = e.generateKeyPair();
        e.initialize(new ECGenParameterSpec("secp384r1"));
        p384 = e.generateKeyPair();
    }

    private final List<String> tokenRequests = Collections.synchronizedList(new ArrayList<String>());
    private volatile int jwksFetches;
    private volatile String jwksBody;
    private volatile int jwksStatus;
    private volatile String idToken;
    private volatile String accessToken;
    private OidcTestSupport.MemoryStore store;
    private OidcClient client;

    @BeforeEach
    void serve() {
        tokenRequests.clear();
        jwksFetches = 0;
        jwksStatus = 200;
        jwksBody = jwks(jwk("k-rsa", rsa), jwk("k-256", p256), jwk("k-384", p384));
        accessToken = "AT-1";
        idToken = null;
        store = new OidcTestSupport.MemoryStore();
        client = newClient(true, false);
        TestCodenameOneImplementation.getInstance().setNetworkMockHandler(
                new TestCodenameOneImplementation.NetworkMockHandler() {
                    public void handle(TestCodenameOneImplementation.TestConnection c) {
                        String body = text(c.getOutputData());
                        c.clearRequest();
                        if (c.getUrl().startsWith(TOKEN_EP)) {
                            tokenRequests.add(body);
                            c.respond(200, "OK", utf8("{\"access_token\":\"" + accessToken
                                    + "\",\"token_type\":\"Bearer\",\"expires_in\":3600,"
                                    + "\"refresh_token\":\"RT-2\""
                                    + (idToken == null ? "" : ",\"id_token\":\"" + idToken + "\"")
                                    + "}"));
                        } else if (c.getUrl().startsWith(JWKS)) {
                            jwksFetches++;
                            if (jwksStatus < 0) {
                                throw new IllegalStateException("offline");
                            }
                            c.respond(jwksStatus, "x", utf8(jwksBody));
                        } else {
                            c.respond(404, "Not Found", utf8("nothing"));
                        }
                    }
                });
    }

    @AfterEach
    void forget() {
        TestCodenameOneImplementation.getInstance().clearNetworkMocks();
    }

    private OidcClient newClient(boolean withJwks, boolean issParameter) {
        OidcConfiguration.Builder b = OidcConfiguration.newBuilder().issuer(ISSUER)
                .authorizationEndpoint(ISSUER + "/oauth2/authorize").tokenEndpoint(TOKEN_EP)
                .authorizationResponseIssParameterSupported(issParameter);
        if (withJwks) {
            b.jwksUri(JWKS);
        }
        return OidcClient.create(b.build()).setClientId(CLIENT).setRedirectUri(REDIRECT)
                .setScopes("openid").setTokenStore(store);
    }

    // ---- making tokens ------------------------------------------------------

    private static String b64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] unsigned(BigInteger value, int width) {
        byte[] raw = value.toByteArray();
        int from = raw.length > 1 && raw[0] == 0 ? 1 : 0;
        int length = raw.length - from;
        if (width <= 0) {
            return Arrays.copyOfRange(raw, from, raw.length);
        }
        byte[] out = new byte[width];
        System.arraycopy(raw, from, out, width - length, length);
        return out;
    }

    private static String jwk(String kid, KeyPair pair) {
        if (pair.getPublic() instanceof RSAPublicKey) {
            RSAPublicKey k = (RSAPublicKey) pair.getPublic();
            return "{\"kty\":\"RSA\",\"use\":\"sig\",\"kid\":\"" + kid + "\",\"n\":\""
                    + b64(unsigned(k.getModulus(), 0)) + "\",\"e\":\""
                    + b64(unsigned(k.getPublicExponent(), 0)) + "\"}";
        }
        ECPublicKey k = (ECPublicKey) pair.getPublic();
        int width = (k.getParams().getCurve().getField().getFieldSize() + 7) / 8;
        return "{\"kty\":\"EC\",\"use\":\"sig\",\"kid\":\"" + kid + "\",\"crv\":\"P-"
                + (width * 8) + "\",\"x\":\"" + b64(unsigned(k.getW().getAffineX(), width))
                + "\",\"y\":\"" + b64(unsigned(k.getW().getAffineY(), width)) + "\"}";
    }

    private static String jwks(String... keys) {
        StringBuilder sb = new StringBuilder("{\"keys\":[");
        for (int i = 0; i < keys.length; i++) {
            sb.append(i == 0 ? "" : ",").append(keys[i]);
        }
        return sb.append("]}").toString();
    }

    private static Map<String, Object> claims() {
        Map<String, Object> c = new LinkedHashMap<String, Object>();
        long now = System.currentTimeMillis() / 1000L;
        c.put("iss", ISSUER);
        c.put("sub", "ada");
        c.put("aud", CLIENT);
        c.put("iat", Long.valueOf(now));
        c.put("exp", Long.valueOf(now + 300));
        return c;
    }

    private static String json(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        for (Map.Entry<String, Object> e : map.entrySet()) {
            sb.append(sb.length() == 1 ? "" : ",").append('"').append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v instanceof Number) {
                sb.append(v);
            } else if (v instanceof List) {
                sb.append('[');
                for (int i = 0; i < ((List) v).size(); i++) {
                    sb.append(i == 0 ? "" : ",").append('"').append(((List) v).get(i)).append('"');
                }
                sb.append(']');
            } else {
                sb.append('"').append(v).append('"');
            }
        }
        return sb.append('}').toString();
    }

    /** A compact JWS over `claims`, signed with a fixture key the way a provider signs. */
    private static String sign(String alg, String kid, KeyPair pair, Map<String, Object> claims)
            throws Exception {
        String header = "{\"alg\":\"" + alg + "\",\"typ\":\"JWT\""
                + (kid == null ? "" : ",\"kid\":\"" + kid + "\"") + "}";
        String input = b64(header.getBytes("UTF-8")) + "." + b64(json(claims).getBytes("UTF-8"));
        boolean ec = alg.startsWith("ES");
        java.security.Signature s = java.security.Signature.getInstance(
                "SHA" + alg.substring(2) + (ec ? "withECDSA" : "withRSA"));
        s.initSign(pair.getPrivate());
        s.update(input.getBytes("UTF-8"));
        byte[] sig = s.sign();
        if (ec) {
            sig = derToJose(sig, "ES256".equals(alg) ? 32 : 48);
        }
        return input + "." + b64(sig);
    }

    /** r and s out of an ASN.1 ECDSA signature, each at the curve's width. */
    private static byte[] derToJose(byte[] der, int width) {
        int at = 2 + ((der[1] & 0x80) != 0 ? der[1] & 0x7f : 0);
        byte[] out = new byte[2 * width];
        for (int part = 0; part < 2; part++) {
            int length = der[at + 1] & 0xff;
            byte[] number = Arrays.copyOfRange(der, at + 2, at + 2 + length);
            byte[] fixed = unsigned(new BigInteger(1, number), width);
            System.arraycopy(fixed, 0, out, part * width, width);
            at += 2 + length;
        }
        return out;
    }

    private OidcTestSupport.Outcome<OidcTokens> refresh() {
        return await(client.refreshTokens(new OidcTokens("previous-access", null, "RT-1", "Bearer",
                "openid", null, claims(), Collections.<String, Object>emptyMap())));
    }

    private void refused(String code, String because) {
        // Whatever an earlier, accepted, token left there.
        store.saved = null;
        OidcTestSupport.Outcome<OidcTokens> outcome = refresh();
        assertNull(outcome.value, "the tokens were handed over");
        assertInstanceOf(OidcException.class, outcome.error, String.valueOf(outcome.error));
        OidcException error = (OidcException) outcome.error;
        assertEquals(code, error.getError(), error.getMessage());
        assertTrue(error.getMessage().contains(because), error.getMessage());
        assertNull(store.saved, "a refused token set was stored");
    }

    // ---- the signature ------------------------------------------------------

    @Test
    void anIdTokenSignedByTheProvidersKeyIsAccepted() throws Exception {
        idToken = sign("RS256", "k-rsa", rsa, claims());
        OidcTestSupport.Outcome<OidcTokens> first = refresh();
        assertNull(first.error, String.valueOf(first.error));
        assertEquals("ada", first.value.getSubject());
        assertEquals(idToken, store.saved.getIdToken());
        assertEquals(1, jwksFetches);

        // The keys are kept: the next token costs no second fetch.
        idToken = sign("ES256", "k-256", p256, claims());
        assertNull(refresh().error);
        idToken = sign("ES384", "k-384", p384, claims());
        assertNull(refresh().error);
        idToken = sign("RS512", "k-rsa", rsa, claims());
        assertNull(refresh().error);
        assertEquals(1, jwksFetches);

        // A token that names no key is tried against every key of its kind.
        idToken = sign("RS256", null, rsa, claims());
        assertNull(refresh().error);
    }

    @Test
    void aSignatureThatDoesNotVerifyIsRefused() throws Exception {
        String good = sign("RS256", "k-rsa", rsa, claims());
        // The claims changed after signing: somebody else's subject under a real signature.
        Map<String, Object> forged = claims();
        forged.put("sub", "root");
        String[] parts = good.split("\\.");
        idToken = parts[0] + "." + b64(json(forged).getBytes("UTF-8")) + "." + parts[2];
        refused(OidcException.INVALID_ID_TOKEN, "signature does not verify");

        // Signed by a key that is not the provider's, under the provider's key id.
        idToken = sign("RS256", "k-rsa", rsaOther, claims());
        refused(OidcException.INVALID_ID_TOKEN, "signature does not verify");

        // An EC token under an RSA key's id: no key of that kind has that id.
        idToken = sign("ES256", "k-rsa", p256, claims());
        refused(OidcException.INVALID_ID_TOKEN, "hold none for this ID token");
    }

    @Test
    void anUnsignedTokenOrOneSignedWithASharedSecretIsRefused() throws Exception {
        String payload = b64(json(claims()).getBytes("UTF-8"));
        idToken = b64("{\"alg\":\"none\"}".getBytes("UTF-8")) + "." + payload + ".";
        refused(OidcException.INVALID_ID_TOKEN, "signed with none, which is not accepted");

        // HS256 keyed with the public key's own bytes: the confusion attack.
        String input = b64("{\"alg\":\"HS256\",\"kid\":\"k-rsa\"}".getBytes("UTF-8")) + "." + payload;
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(rsa.getPublic().getEncoded(), "HmacSHA256"));
        idToken = input + "." + b64(mac.doFinal(input.getBytes("UTF-8")));
        refused(OidcException.INVALID_ID_TOKEN, "signed with HS256, which is not accepted");
        assertEquals(0, jwksFetches, "no key is fetched for an algorithm that takes none");

        idToken = "not-a-jwt";
        refused(OidcException.INVALID_ID_TOKEN, "not a JWT");
    }

    @Test
    void aRotatedKeyIsFetchedOnceItIsNamed() throws Exception {
        idToken = sign("RS256", "k-rsa", rsa, claims());
        assertNull(refresh().error);
        assertEquals(1, jwksFetches);

        // The provider rotates: a new key, a token signed with it.
        jwksBody = jwks(jwk("k-new", rsaOther));
        idToken = sign("RS256", "k-new", rsaOther, claims());
        // Within the window the set is taken as complete: an unknown key id is refused
        // rather than answered with a fetch for every token somebody makes up.
        refused(OidcException.INVALID_ID_TOKEN, "hold none for this ID token");
        assertEquals(1, jwksFetches);
        client.jwksRefetchMillis = 0;
        assertNull(refresh().error);
        assertEquals(2, jwksFetches);
        // And the old key is gone with the old set.
        idToken = sign("RS256", "k-rsa", rsa, claims());
        client.jwksRefetchMillis = 60000;
        refused(OidcException.INVALID_ID_TOKEN, "hold none for this ID token");
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullSource
    @org.junit.jupiter.params.provider.ValueSource(strings = {"k-rsa"})
    void rotatedSignaturesRefreshAfterTheCacheWindow(String kid) throws Exception {
        idToken = sign("RS256", kid, rsa, claims());
        assertNull(refresh().error);
        assertEquals(1, jwksFetches);
        idToken = sign("RS256", kid, rsaOther, claims());
        assertNotNull(refresh().error);
        assertEquals(1, jwksFetches, "bad signatures must respect the refetch interval");
        client.jwksRefetchMillis = 0;
        // Even after fetching, an untrusted key must still be refused, exactly once.
        assertNotNull(refresh().error);
        assertEquals(2, jwksFetches);
        jwksBody = jwks(jwk("k-rsa", rsaOther));
        assertNull(refresh().error, "rotation must recover without recreating the client");
        assertEquals(3, jwksFetches);
        assertNull(refresh().error);
        assertEquals(3, jwksFetches, "a matching cached signature needs no fetch");
    }

    @Test
    void keysThatCannotBeHadMeanATokenThatIsNotAccepted() throws Exception {
        idToken = sign("RS256", "k-rsa", rsa, claims());
        jwksStatus = -1;
        refused(OidcException.TRANSPORT_ERROR, "could not be fetched, so the ID token was not accepted");
        jwksStatus = 500;
        jwksBody = "oops";
        refused(OidcException.INVALID_ID_TOKEN, "could not be read");
        jwksStatus = 200;
        jwksBody = "{\"keys\":\"none\"}";
        refused(OidcException.INVALID_ID_TOKEN, "could not be read");

        // A configuration that names no key set at all.
        client = newClient(false, false);
        refused(OidcException.INVALID_ID_TOKEN, "names no jwksUri");
    }

    @Test
    void theOptOutIsExplicitAndStillChecksTheClaims() throws Exception {
        client = newClient(false, false).setVerifyIdTokenSignature(false);
        idToken = sign("RS256", "k-unknown", rsaOther, claims());
        OidcTestSupport.Outcome<OidcTokens> outcome = refresh();
        assertNull(outcome.error, String.valueOf(outcome.error));
        assertEquals(0, jwksFetches);

        Map<String, Object> other = claims();
        other.put("aud", "somebody-else");
        idToken = sign("RS256", "k-unknown", rsaOther, other);
        store.saved = null;
        refused(OidcException.INVALID_ID_TOKEN, "not for this client");
    }

    @Test
    void aVerificationThatCannotBeDoneIsARefusalThatNamesTheOptOut() throws Exception {
        // A key the platform will not verify with: an RSA modulus of 64 bits, which this
        // one refuses outright. The verification throws, and the token is refused -- never
        // waved through.
        jwksBody = jwks("{\"kty\":\"RSA\",\"kid\":\"k-rsa\",\"n\":\""
                + b64(new byte[] {(byte) 0xc1, 2, 3, 4, 5, 6, 7, 9}) + "\",\"e\":\"AQAB\"}");
        idToken = sign("RS256", "k-rsa", rsa, claims());
        OidcTestSupport.Outcome<OidcTokens> outcome = refresh();
        assertNull(outcome.value);
        OidcException error = (OidcException) outcome.error;
        assertEquals(OidcException.INVALID_ID_TOKEN, error.getError());
        assertTrue(error.getMessage().startsWith("This platform could not verify an RS256 "
                + "signature ("), error.getMessage());
        assertTrue(error.getMessage().contains("so the ID token was not accepted")
                && error.getMessage().contains("OidcClient.setVerifyIdTokenSignature(false)"),
                error.getMessage());
        assertNull(store.saved);
    }

    // ---- the claims ---------------------------------------------------------

    @Test
    void refreshCannotChangeTheStoredSubjectEvenAfterAnIdTokenIsOmitted() throws Exception {
        String initialId = sign("RS256", "k-rsa", rsa, claims());
        Map<String, Object> initial = new LinkedHashMap<String, Object>();
        initial.put("access_token", "original-access");
        initial.put("refresh_token", "RT-1");
        initial.put("id_token", initialId);
        OidcTokens original = OidcTokens.fromTokenResponse(initial, null);
        store.saved = original;
        OidcRequestAuthorizer authorizer = new OidcRequestAuthorizer(client);
        authorizer.setTokens(original);
        Map<String, Object> other = claims();
        other.put("sub", "another-user");
        idToken = sign("RS256", "k-rsa", rsa, other);
        OidcTestSupport.Outcome<OidcTokens> rejected = await(client.refresh("RT-1"));
        assertNull(rejected.value);
        assertEquals(OidcException.INVALID_ID_TOKEN, ((OidcException) rejected.error).getError());
        assertSame(original, store.saved);
        assertSame(original, authorizer.getTokens());

        idToken = initialId;
        OidcTestSupport.Outcome<OidcTokens> matching = await(client.refresh("RT-1"));
        assertNull(matching.error, String.valueOf(matching.error));
        assertEquals(original.getSubject(), matching.value.getSubject());
        idToken = null;
        OidcTestSupport.Outcome<OidcTokens> omitted = await(client.refresh("RT-2"));
        assertNull(omitted.error);
        assertEquals(original.getSubject(), omitted.value.getSubject());
        assertNull(omitted.value.getIdToken(), "do not report the old ID token as newly issued");

        // Restoring after an application restart retains the identity binding.
        store.saved = TokenJson.fromJson(TokenJson.toJson(omitted.value));
        client = newClient(true, false);
        idToken = sign("RS256", "k-rsa", rsa, other);
        rejected = await(client.refresh("RT-2"));
        assertNull(rejected.value);
        assertEquals(OidcException.INVALID_ID_TOKEN, ((OidcException) rejected.error).getError());
        assertEquals(original.getSubject(), store.saved.getSubject());
    }

    @Test
    void idTokensRequireAnExpectedIssuer() throws Exception {
        client = OidcClient.create(OidcConfiguration.newBuilder().authorizationEndpoint(ISSUER + "/auth").tokenEndpoint(TOKEN_EP)
                .jwksUri(JWKS).build()).setClientId(CLIENT).setScopes("openid").setTokenStore(store);
        idToken = sign("RS256", "k-rsa", rsa, claims());
        refused(OidcException.INVALID_ID_TOKEN, "expected issuer");
        client.setVerifyIdTokenSignature(false);
        refused(OidcException.INVALID_ID_TOKEN, "expected issuer");
    }

    @Test
    void aRefreshedIdTokenNeedsTheOriginalSubject() throws Exception {
        idToken = sign("RS256", "k-rsa", rsa, claims());
        OidcTestSupport.Outcome<OidcTokens> result = await(client.refresh("RT-1"));
        assertNull(result.value);
        assertEquals(OidcException.INVALID_ID_TOKEN, ((OidcException) result.error).getError());
        assertTrue(result.error.getMessage().contains("previous subject"));
        assertNull(store.saved);
    }

    @Test
    void theClaimsAreHeldToThisClientThisIssuerAndNow() throws Exception {
        Object[][] cases = {
            {"iss", "https://evil.example.com", "was issued by https://evil.example.com"},
            {"aud", "somebody-else", "not for this client"},
            {"aud", Arrays.asList(CLIENT, "other"), "issued to another party"},
            {"azp", "other", "issued to another party"},
            {"exp", Long.valueOf(System.currentTimeMillis() / 1000L - 3600), "has expired"},
            {"nbf", Long.valueOf(System.currentTimeMillis() / 1000L + 3600), "not valid yet"},
            {"at_hash", "AAAAAAAAAAAAAAAAAAAAAA", "at_hash does not match"},
        };
        for (Object[] one : cases) {
            Map<String, Object> c = claims();
            c.put((String) one[0], one[1]);
            idToken = sign("RS256", "k-rsa", rsa, c);
            refused(OidcException.INVALID_ID_TOKEN, (String) one[2]);
        }
        Map<String, Object> noExpiry = claims();
        noExpiry.remove("exp");
        idToken = sign("RS256", "k-rsa", rsa, noExpiry);
        refused(OidcException.INVALID_ID_TOKEN, "has no expiry");

        // What is allowed: two audiences with this client as the authorized party, a
        // clock a minute off, and the right at_hash.
        Map<String, Object> fine = claims();
        fine.put("aud", Arrays.asList(CLIENT, "other"));
        fine.put("azp", CLIENT);
        fine.put("exp", Long.valueOf(System.currentTimeMillis() / 1000L - 60));
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(accessToken.getBytes("UTF-8"));
        fine.put("at_hash", b64(Arrays.copyOf(digest, 16)));
        idToken = sign("RS256", "k-rsa", rsa, fine);
        assertNull(refresh().error);
        // The hash is of the algorithm's own size: ES384 takes the left half of SHA-384.
        digest = MessageDigest.getInstance("SHA-384").digest(accessToken.getBytes("UTF-8"));
        fine.put("at_hash", b64(Arrays.copyOf(digest, 24)));
        idToken = sign("ES384", "k-384", p384, fine);
        assertNull(refresh().error);
        // And a skew of nothing refuses the minute-old expiry.
        client.setIdTokenClockSkew(0);
        refused(OidcException.INVALID_ID_TOKEN, "has expired");
    }

    @Test
    void issuanceTimeIsRequiredAndMustBeNumeric() throws Exception {
        for (Object value : new Object[] {null, "123", Boolean.TRUE}) {
            Map<String, Object> invalid = claims();
            if (value == null) invalid.remove("iat");
            else invalid.put("iat", value);
            idToken = sign("RS256", "k-rsa", rsa, invalid);
            refused(OidcException.INVALID_ID_TOKEN, "issuance time");
        }
    }

    @Test
    void ordinaryIssuersMustMatchTheirSchemeExactly() throws Exception {
        Jwt token = Jwt.parse(sign("RS256", "k", rsa,
                with(claims(), "iss", "login.example.com")));
        assertNotNull(IdTokenVerifier.checkClaims(token, "https://login.example.com", CLIENT,
                null, null, 300, System.currentTimeMillis()));
    }

    @Test
    void aTokenThatNamesNoSubjectIsNotASignIn() throws Exception {
        Map<String, Object> anonymous = claims();
        anonymous.remove("sub");
        OidcException refused = IdTokenVerifier.checkClaims(Jwt.parse(sign("RS256", "k", rsa, anonymous)),
                ISSUER, CLIENT, null, null, 300, System.currentTimeMillis());
        assertNotNull(refused);
        assertEquals(OidcException.INVALID_ID_TOKEN, refused.getError());
        assertEquals("The ID token names no subject", refused.getMessage());
        // An empty one names nobody either, and neither does one that is not text.
        assertNotNull(IdTokenVerifier.checkClaims(Jwt.parse(sign("RS256", "k", rsa,
                with(claims(), "sub", ""))), ISSUER, CLIENT, null, null, 300, System.currentTimeMillis()));
        Map<String, Object> numbered = claims();
        numbered.put("sub", Long.valueOf(7));
        assertNotNull(IdTokenVerifier.checkClaims(Jwt.parse(sign("RS256", "k", rsa, numbered)),
                ISSUER, CLIENT, null, null, 300, System.currentTimeMillis()));
        assertNull(IdTokenVerifier.checkClaims(Jwt.parse(sign("RS256", "k", rsa, claims())),
                ISSUER, CLIENT, null, null, 300, System.currentTimeMillis()));
    }

    @Test
    void anIssuerWrittenTheProvidersOwnWayIsStillThatIssuer() throws Exception {
        // Microsoft's multi-tenant discovery document names its issuer with a placeholder.
        Jwt token = Jwt.parse(sign("RS256", "k", rsa, with(claims(), "iss",
                "https://login.microsoftonline.com/9188040d/v2.0", "tid", "9188040d")));
        assertNull(IdTokenVerifier.checkClaims(token,
                "https://login.microsoftonline.com/{tenantid}/v2.0", CLIENT, null, null, 300,
                System.currentTimeMillis()));
        Jwt otherTenant = Jwt.parse(sign("RS256", "k", rsa, with(claims(), "iss",
                "https://login.microsoftonline.com/evil/v2.0", "tid", "9188040d")));
        assertNotNull(IdTokenVerifier.checkClaims(otherTenant,
                "https://login.microsoftonline.com/{tenantid}/v2.0", CLIENT, null, null, 300,
                System.currentTimeMillis()));
        // Google's tokens may leave the scheme out.
        Jwt google = Jwt.parse(sign("RS256", "k", rsa, with(claims(), "iss",
                "accounts.google.com")));
        assertNull(IdTokenVerifier.checkClaims(google, "https://accounts.google.com", CLIENT,
                null, null, 300, System.currentTimeMillis()));
        assertNotNull(IdTokenVerifier.checkClaims(google, "https://accounts.google.com.evil",
                CLIENT, null, null, 300, System.currentTimeMillis()));
    }

    private static Map<String, Object> with(Map<String, Object> claims, String... pairs) {
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            claims.put(pairs[i], pairs[i + 1]);
        }
        return claims;
    }

    // ---- the authorization response -----------------------------------------

    @Test
    void anInitialOpenIdResponseRequiresAnIdTokenButRefreshDoesNot() {
        OidcTestSupport.Outcome<OidcTokens> result = redirect("code=c&state=s", "s", "n");
        assertNull(result.value);
        assertEquals(OidcException.INVALID_ID_TOKEN, ((OidcException) result.error).getError());
        assertNull(store.saved);
        assertNull(refresh().error, "refresh may omit the ID token");
        client.setScopes("profile");
        assertNull(redirect("code=c&state=s", "s", "n").error,
                "plain OAuth does not require an ID token");
    }

    private OidcTestSupport.Outcome<OidcTokens> redirect(String query, String state, String nonce) {
        AsyncResource<OidcTokens> out = new AsyncResource<OidcTokens>();
        client.handleRedirect(REDIRECT + "?" + query, state, nonce, PkceChallenge.generate(), out);
        return await(out);
    }

    @Test
    void theNonceOfTheRequestMustComeBackInTheToken() throws Exception {
        idToken = sign("RS256", "k-rsa", rsa, with(claims(), "nonce", "n-1"));
        OidcTestSupport.Outcome<OidcTokens> good = redirect("code=c&state=s-1", "s-1", "n-1");
        assertNull(good.error, String.valueOf(good.error));
        assertEquals("ada", good.value.getSubject());

        OidcTestSupport.Outcome<OidcTokens> other = redirect("code=c&state=s-1", "s-1", "n-2");
        assertEquals(OidcException.NONCE_MISMATCH, ((OidcException) other.error).getError());

        // A token with no nonce at all is not one that answers this request either.
        idToken = sign("RS256", "k-rsa", rsa, claims());
        OidcTestSupport.Outcome<OidcTokens> none = redirect("code=c&state=s-1", "s-1", "n-1");
        assertEquals(OidcException.NONCE_MISMATCH, ((OidcException) none.error).getError());
        assertTrue(none.error.getMessage().contains("carries no nonce"), none.error.getMessage());

        client.setEnforceNonce(false);
        assertNull(redirect("code=c&state=s-1", "s-1", "n-1").error);
    }

    @Test
    void aResponseFromAnotherIssuerIsRefusedBeforeItsCodeIsUsed() throws Exception {
        idToken = sign("RS256", "k-rsa", rsa, with(claims(), "nonce", "n-1"));
        // Named, and ours.
        assertNull(redirect("code=c&state=s-1&iss=https%3A%2F%2Flogin.example.com", "s-1",
                "n-1").error);
        assertEquals(1, tokenRequests.size());

        // Named, and somebody else's: the mix-up. The code never reaches a token endpoint.
        OidcTestSupport.Outcome<OidcTokens> mixed = redirect(
                "code=stolen&state=s-1&iss=https%3A%2F%2Fevil.example.com", "s-1", "n-1");
        assertEquals(OidcException.ISSUER_MISMATCH, ((OidcException) mixed.error).getError());
        assertTrue(mixed.error.getMessage().contains("is from https://evil.example.com, not "
                + "from https://login.example.com"), mixed.error.getMessage());
        assertEquals(1, tokenRequests.size(), "the code was redeemed all the same");
        // The same for an error response: who it claims to be from comes first.
        OidcTestSupport.Outcome<OidcTokens> error = redirect(
                "error=access_denied&state=s-1&iss=https%3A%2F%2Fevil.example.com", "s-1", "n-1");
        assertEquals(OidcException.ISSUER_MISMATCH, ((OidcException) error.error).getError());

        // Not named, by a provider that does not say it names itself: as before.
        assertNull(redirect("code=c&state=s-1", "s-1", "n-1").error);

        // Not named, by one that says it always does.
        client = newClient(true, true);
        OidcTestSupport.Outcome<OidcTokens> missing = redirect("code=c&state=s-1", "s-1", "n-1");
        assertEquals(OidcException.ISSUER_MISMATCH, ((OidcException) missing.error).getError());
        assertTrue(missing.error.getMessage().contains("names no issuer"),
                missing.error.getMessage());
        assertNull(redirect("code=c&state=s-1&iss=https%3A%2F%2Flogin.example.com", "s-1",
                "n-1").error);
    }

    @Test
    void anErrorIsBelievedOnlyWithTheStateOfTheRequest() throws Exception {
        // The provider's own refusal carries the state it was sent.
        OidcTestSupport.Outcome<OidcTokens> denied = redirect(
                "error=access_denied&error_description=No&state=s-1", "s-1", "n-1");
        assertEquals(OidcException.ACCESS_DENIED, ((OidcException) denied.error).getError());
        assertEquals("No", ((OidcException) denied.error).getErrorDescription());

        // A link anyone can send carries none, or a guess.
        OidcTestSupport.Outcome<OidcTokens> bare = redirect("error=access_denied", "s-1", "n-1");
        assertEquals(OidcException.STATE_MISMATCH, ((OidcException) bare.error).getError());
        OidcTestSupport.Outcome<OidcTokens> guessed = redirect(
                "error=server_error&state=guess", "s-1", "n-1");
        assertEquals(OidcException.STATE_MISMATCH, ((OidcException) guessed.error).getError());
        assertEquals(0, tokenRequests.size());
    }

    @Test
    void theDiscoveryDocumentSaysWhetherTheIssuerIsNamed() throws Exception {
        Map<String, Object> doc = new JSONParser().parseJSON(new StringReader("{\"issuer\":\""
                + ISSUER + "\",\"authorization_endpoint\":\"" + ISSUER + "/a\",\"jwks_uri\":\""
                + JWKS + "\",\"authorization_response_iss_parameter_supported\":true}"));
        OidcConfiguration cfg = OidcConfiguration.fromDiscoveryJson(doc);
        assertTrue(cfg.isAuthorizationResponseIssParameterSupported());
        assertEquals(JWKS, cfg.getJwksUri());
        assertTrue(OidcConfiguration.newBuilder(cfg).build()
                .isAuthorizationResponseIssParameterSupported());
        doc.remove("authorization_response_iss_parameter_supported");
        assertFalse(OidcConfiguration.fromDiscoveryJson(doc)
                .isAuthorizationResponseIssParameterSupported());
    }

    // ---- the keys themselves ------------------------------------------------

    @Test
    void aJwkBecomesTheKeyThePlatformWouldHaveReadFromAFile() throws Exception {
        for (KeyPair pair : new KeyPair[] {rsa, p256, p384}) {
            Map<String, Object> jwk = new JSONParser().parseJSON(new StringReader(jwk("k", pair)));
            assertArrayEquals(pair.getPublic().getEncoded(),
                    IdTokenVerifier.publicKey(jwk).getEncoded(),
                    "the X.509 form built from a JWK is the JDK's own for that key");
        }
    }
}
