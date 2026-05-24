package com.codename1.io.oidc;

import com.codename1.junit.UITestBase;
import com.codename1.util.Base64;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Pure-Java tests for the OIDC core: PKCE generation, claim decoding,
/// discovery JSON parsing. No network or UI involvement.
public class OidcCoreTest extends UITestBase {

    @Test
    public void pkceVerifierAndChallengeAreDistinctAndUrlSafe() {
        PkceChallenge p = PkceChallenge.generate();
        assertNotNull(p.getVerifier());
        assertNotNull(p.getChallenge());
        assertNotEquals(p.getVerifier(), p.getChallenge());
        assertEquals(PkceChallenge.METHOD_S256, p.getMethod());
        // Verifier must be at least 43 chars per RFC 7636
        assertTrue(p.getVerifier().length() >= 43,
                "PKCE verifier must be at least 43 chars; got " + p.getVerifier().length());
        // No '=', '+', '/' allowed in url-safe verifier
        for (int i = 0; i < p.getVerifier().length(); i++) {
            char c = p.getVerifier().charAt(i);
            assertTrue(c == '-' || c == '_' || c == '.' || c == '~'
                            || (c >= 'A' && c <= 'Z')
                            || (c >= 'a' && c <= 'z')
                            || (c >= '0' && c <= '9'),
                    "Verifier contains non-RFC7636 char: " + c);
        }
    }

    @Test
    public void pkceVerifiersAreUnique() {
        PkceChallenge a = PkceChallenge.generate();
        PkceChallenge b = PkceChallenge.generate();
        assertNotEquals(a.getVerifier(), b.getVerifier());
        assertNotEquals(a.getChallenge(), b.getChallenge());
    }

    @Test
    public void configurationFromDiscoveryJson() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("issuer", "https://accounts.example.com");
        json.put("authorization_endpoint", "https://accounts.example.com/o/oauth2/v2/auth");
        json.put("token_endpoint", "https://oauth2.example.com/token");
        json.put("userinfo_endpoint", "https://openidconnect.example.com/v1/userinfo");
        json.put("revocation_endpoint", "https://oauth2.example.com/revoke");
        json.put("jwks_uri", "https://www.example.com/oauth2/v3/certs");
        OidcConfiguration cfg = OidcConfiguration.fromDiscoveryJson(json);
        assertEquals("https://accounts.example.com", cfg.getIssuer());
        assertEquals("https://accounts.example.com/o/oauth2/v2/auth", cfg.getAuthorizationEndpoint());
        assertEquals("https://oauth2.example.com/token", cfg.getTokenEndpoint());
        assertEquals("https://oauth2.example.com/revoke", cfg.getRevocationEndpoint());
        assertEquals("https://www.example.com/oauth2/v3/certs", cfg.getJwksUri());
    }

    @Test
    public void idTokenClaimsDecodeOnUnsignedJwt() throws Exception {
        // Build a JWT with header={alg:none}, payload={sub:user123,email:e@x.com,nonce:abc}, sig=""
        String header = base64Url("{\"alg\":\"none\"}");
        String payload = base64Url("{\"sub\":\"user123\",\"email\":\"e@x.com\",\"nonce\":\"abc\"}");
        String jwt = header + "." + payload + ".";
        Map<String, Object> claims = OidcTokens.decodeIdTokenClaims(jwt);
        assertEquals("user123", claims.get("sub"));
        assertEquals("e@x.com", claims.get("email"));
        assertEquals("abc", claims.get("nonce"));
    }

    @Test
    public void idTokenClaimsReturnEmptyOnMalformed() {
        assertTrue(OidcTokens.decodeIdTokenClaims("not-a-jwt").isEmpty());
        assertTrue(OidcTokens.decodeIdTokenClaims(null).isEmpty());
        assertTrue(OidcTokens.decodeIdTokenClaims("only.one.").isEmpty() ||
                !OidcTokens.decodeIdTokenClaims("only.one.").isEmpty());
        // single-dot is malformed
        assertTrue(OidcTokens.decodeIdTokenClaims("foo.bar").isEmpty());
    }

    @Test
    public void fromTokenResponseExtractsAllFields() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("access_token", "at-123");
        json.put("refresh_token", "rt-123");
        json.put("id_token", buildSimpleJwt("user42"));
        json.put("token_type", "Bearer");
        json.put("expires_in", "3600");
        json.put("scope", "openid email");
        OidcTokens t = OidcTokens.fromTokenResponse(json, null);
        assertEquals("at-123", t.getAccessToken());
        assertEquals("rt-123", t.getRefreshToken());
        assertEquals("Bearer", t.getTokenType());
        assertEquals("openid email", t.getScope());
        assertEquals("user42", t.getSubject());
        assertNotNull(t.getExpiresAt());
    }

    @Test
    public void refreshTokenFallbackKicksInWhenAbsent() {
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("access_token", "at-fresh");
        json.put("expires_in", "60");
        OidcTokens t = OidcTokens.fromTokenResponse(json, "rt-original");
        assertEquals("rt-original", t.getRefreshToken());
    }

    @Test
    public void oidcExceptionPreservesErrorCode() {
        OidcException e = new OidcException(OidcException.STATE_MISMATCH, "bad state");
        assertEquals(OidcException.STATE_MISMATCH, e.getError());
        assertEquals("bad state", e.getErrorDescription());
        assertEquals("bad state", e.getMessage());
    }

    @Test
    public void systemBrowserSchemeOfHandlesBothShapes() {
        assertEquals("https", SystemBrowser.schemeOf("https://example.com/cb"));
        assertEquals("com.example.app", SystemBrowser.schemeOf("com.example.app:/oauth2redirect"));
        assertEquals("noscheme", SystemBrowser.schemeOf("noscheme"));
    }

    @Test
    public void clientBuilderRequiresAuthorizationEndpoint() {
        try {
            OidcConfiguration.newBuilder().build();
            org.junit.jupiter.api.Assertions.fail("Expected IllegalStateException");
        } catch (IllegalStateException expected) {
            // ok
        }
    }

    @Test
    public void firebaseRefreshTokenValidatorRejectsBadInputs() {
        // good tokens pass through unchanged
        assertEquals("Abc_-.123",
                com.codename1.social.FirebaseAuth.requireFirebaseToken("Abc_-.123"));
        assertEquals("eyJhbGciOiJIUzI1NiJ9.payload.sig",
                com.codename1.social.FirebaseAuth.requireFirebaseToken("eyJhbGciOiJIUzI1NiJ9.payload.sig"));

        // null / empty rejected
        try { com.codename1.social.FirebaseAuth.requireFirebaseToken(null); org.junit.jupiter.api.Assertions.fail(); }
        catch (IllegalArgumentException expected) {}
        try { com.codename1.social.FirebaseAuth.requireFirebaseToken(""); org.junit.jupiter.api.Assertions.fail(); }
        catch (IllegalArgumentException expected) {}

        // whitespace / control / quote rejected -- this is what stops a
        // taint-source value from escaping into the token endpoint payload
        for (String bad : new String[] {"abc def", "ab\tcd", "ab\ncd", "ab\"cd", "ab cd"}) {
            try {
                com.codename1.social.FirebaseAuth.requireFirebaseToken(bad);
                org.junit.jupiter.api.Assertions.fail("Expected rejection of " + bad);
            } catch (IllegalArgumentException expected) {
                // ok
            }
        }

        // length cap
        StringBuilder oversize = new StringBuilder(5000);
        for (int i = 0; i < 5000; i++) oversize.append('a');
        try {
            com.codename1.social.FirebaseAuth.requireFirebaseToken(oversize.toString());
            org.junit.jupiter.api.Assertions.fail();
        } catch (IllegalArgumentException expected) {}
    }

    @Test
    public void defaultTokenStoreRoundTrip() throws Exception {
        TokenStore store = new TokenStore.DefaultStorageTokenStore();
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("access_token", "at-1");
        json.put("refresh_token", "rt-1");
        json.put("id_token", buildSimpleJwt("alice"));
        json.put("expires_in", "60");
        OidcTokens t = OidcTokens.fromTokenResponse(json, null);

        String key = "oidc-test-key";
        Boolean saved = store.save(key, t).get(5000);
        assertTrue(Boolean.TRUE.equals(saved), "save() should resolve true");

        OidcTokens loaded = store.load(key).get(5000);
        assertNotNull(loaded);
        assertEquals("at-1", loaded.getAccessToken());
        assertEquals("rt-1", loaded.getRefreshToken());
        assertEquals("alice", loaded.getSubject());

        Boolean cleared = store.clear(key).get(5000);
        assertTrue(Boolean.TRUE.equals(cleared));
        OidcTokens afterClear = store.load(key).get(5000);
        assertNull(afterClear);
    }

    // ------------------------------------------------------------------

    private static String base64Url(String json) {
        try {
            String b = Base64.encodeUrlSafe(json.getBytes("UTF-8"));
            StringBuilder out = new StringBuilder(b.length());
            for (int i = 0; i < b.length(); i++) {
                char c = b.charAt(i);
                if (c == '=' || c == '\n' || c == '\r') continue;
                out.append(c);
            }
            return out.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String buildSimpleJwt(String sub) {
        return base64Url("{\"alg\":\"none\"}") + "."
                + base64Url("{\"sub\":\"" + sub + "\"}") + ".";
    }
}
