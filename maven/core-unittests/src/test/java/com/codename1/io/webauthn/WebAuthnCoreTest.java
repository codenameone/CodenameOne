package com.codename1.io.webauthn;

import com.codename1.junit.UITestBase;
import com.codename1.util.SuccessCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Pure-Java tests for the WebAuthn core: options parsing, response parsing,
/// builder round-trips, error mapping, async dispatch when no native provider
/// is registered. No actual passkey ceremonies run -- the OS authenticator is
/// only reachable on iOS 16+ / Android API 28+ devices.
public class WebAuthnCoreTest extends UITestBase {

    @BeforeEach
    public void resetProvider() {
        WebAuthnClient.setProvider(null);
    }

    @AfterEach
    public void clearProvider() {
        WebAuthnClient.setProvider(null);
    }

    // -------- options round-trip ---------------------------------------

    @Test
    public void creationOptionsParseFieldsFromJson() {
        String json = "{"
                + "\"rp\":{\"id\":\"example.com\",\"name\":\"Example\"},"
                + "\"user\":{\"id\":\"dXNyMQ\",\"name\":\"alice@example.com\",\"displayName\":\"Alice\"},"
                + "\"challenge\":\"Y2hhbGxlbmdl\","
                + "\"pubKeyCredParams\":[{\"type\":\"public-key\",\"alg\":-7}],"
                + "\"attestation\":\"none\""
                + "}";
        PublicKeyCredentialCreationOptions opts =
                PublicKeyCredentialCreationOptions.fromJson(json);
        assertEquals("example.com", opts.getRpId());
        assertEquals("Example", opts.getRpName());
        assertEquals("dXNyMQ", opts.getUserId());
        assertEquals("alice@example.com", opts.getUserName());
        assertEquals("Alice", opts.getUserDisplayName());
        assertEquals("Y2hhbGxlbmdl", opts.getChallenge());
        // The original JSON is preserved verbatim for forwarding to the OS
        // authenticator.
        assertEquals(json, opts.toJson());
    }

    @Test
    public void creationOptionsBuilderProducesValidJson() {
        PublicKeyCredentialCreationOptions opts =
                PublicKeyCredentialCreationOptions.newBuilder()
                        .rp("example.com", "Example")
                        .user("dXNyMQ", "alice@example.com", "Alice")
                        .challenge("Y2hhbGxlbmdl")
                        .authenticatorAttachment("platform")
                        .userVerification("required")
                        .residentKey("required")
                        .build();
        assertEquals("example.com", opts.getRpId());
        assertEquals("Alice", opts.getUserDisplayName());
        // Built JSON must round-trip through fromJson() losslessly.
        PublicKeyCredentialCreationOptions reparsed =
                PublicKeyCredentialCreationOptions.fromJson(opts.toJson());
        assertEquals("example.com", reparsed.getRpId());
        assertEquals("alice@example.com", reparsed.getUserName());
        assertEquals("Y2hhbGxlbmdl", reparsed.getChallenge());

        Map<String, Object> map = opts.asMap();
        Object authSel = map.get("authenticatorSelection");
        assertTrue(authSel instanceof Map, "authenticatorSelection should be a JSON object");
        @SuppressWarnings("unchecked")
        Map<String, Object> authSelMap = (Map<String, Object>) authSel;
        assertEquals("required", authSelMap.get("userVerification"));
        assertEquals("platform", authSelMap.get("authenticatorAttachment"));
    }

    @Test
    public void creationOptionsBuilderRejectsMissingFields() {
        try {
            PublicKeyCredentialCreationOptions.newBuilder()
                    .user("u", "u@x", "u")
                    .challenge("c")
                    .build();
            fail("Expected IllegalStateException for missing rp");
        } catch (IllegalStateException expected) {}

        try {
            PublicKeyCredentialCreationOptions.newBuilder()
                    .rp("example.com", "Example")
                    .challenge("c")
                    .build();
            fail("Expected IllegalStateException for missing user");
        } catch (IllegalStateException expected) {}

        try {
            PublicKeyCredentialCreationOptions.newBuilder()
                    .rp("example.com", "Example")
                    .user("u", "u@x", "u")
                    .build();
            fail("Expected IllegalStateException for missing challenge");
        } catch (IllegalStateException expected) {}
    }

    @Test
    public void creationOptionsFromJsonRejectsNull() {
        try {
            PublicKeyCredentialCreationOptions.fromJson(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {}
    }

    @Test
    public void requestOptionsParseAndBuildersRoundTrip() {
        String json = "{\"rpId\":\"example.com\","
                + "\"challenge\":\"Y2hhbGxlbmdl\","
                + "\"userVerification\":\"required\","
                + "\"allowCredentials\":[{\"type\":\"public-key\",\"id\":\"a\"},{\"type\":\"public-key\",\"id\":\"b\"}]}";
        PublicKeyCredentialRequestOptions opts =
                PublicKeyCredentialRequestOptions.fromJson(json);
        assertEquals("example.com", opts.getRpId());
        assertEquals("Y2hhbGxlbmdl", opts.getChallenge());
        assertEquals("required", opts.getUserVerification());

        PublicKeyCredentialRequestOptions built =
                PublicKeyCredentialRequestOptions.newBuilder()
                        .rpId("example.com")
                        .challenge("Y2hhbGxlbmdl")
                        .userVerification("preferred")
                        .build();
        // Round-trip the built JSON.
        PublicKeyCredentialRequestOptions reparsed =
                PublicKeyCredentialRequestOptions.fromJson(built.toJson());
        assertEquals("example.com", reparsed.getRpId());
        assertEquals("Y2hhbGxlbmdl", reparsed.getChallenge());
        assertEquals("preferred", reparsed.getUserVerification());
    }

    // -------- response parsing -----------------------------------------

    @Test
    public void registrationResponseParsesAllFields() {
        String registrationJson = "{"
                + "\"id\":\"cred-1\",\"rawId\":\"cred-1\",\"type\":\"public-key\","
                + "\"authenticatorAttachment\":\"platform\","
                + "\"response\":{\"clientDataJSON\":\"Y2RhdGE\","
                + "\"attestationObject\":\"YXR0\","
                + "\"transports\":[\"internal\"]},"
                + "\"clientExtensionResults\":{}"
                + "}";
        PublicKeyCredential cred = PublicKeyCredential.fromJson(registrationJson);
        assertEquals("cred-1", cred.getId());
        assertEquals("cred-1", cred.getRawId());
        assertEquals("platform", cred.getAuthenticatorAttachment());
        assertEquals("Y2RhdGE", cred.getClientDataJSON());
        assertEquals("YXR0", cred.getAttestationObject());
        assertNull(cred.getSignature());
        assertNull(cred.getUserHandle());
        assertTrue(cred.isRegistration());
        assertEquals(registrationJson, cred.toJson());
    }

    @Test
    public void assertionResponseParsesAllFields() {
        String assertionJson = "{"
                + "\"id\":\"cred-1\",\"rawId\":\"cred-1\",\"type\":\"public-key\","
                + "\"response\":{\"clientDataJSON\":\"Y2RhdGE\","
                + "\"authenticatorData\":\"YXV0aA\","
                + "\"signature\":\"c2ln\","
                + "\"userHandle\":\"dXNy\"},"
                + "\"clientExtensionResults\":{}"
                + "}";
        PublicKeyCredential cred = PublicKeyCredential.fromJson(assertionJson);
        assertEquals("cred-1", cred.getId());
        assertEquals("Y2RhdGE", cred.getClientDataJSON());
        assertEquals("c2ln", cred.getSignature());
        assertEquals("dXNy", cred.getUserHandle());
        assertNull(cred.getAttestationObject());
        assertFalse(cred.isRegistration());
    }

    @Test
    public void publicKeyCredentialFromJsonRejectsBadInputs() {
        try {
            PublicKeyCredential.fromJson(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {}
        try {
            PublicKeyCredential.fromJson("not-json");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {}
    }

    // -------- exception ------------------------------------------------

    @Test
    public void webauthnExceptionPreservesCodeAndMessage() {
        WebAuthnException e = new WebAuthnException(
                WebAuthnException.SECURITY_ERROR, "RP-id mismatch");
        assertEquals("SecurityError", e.getError());
        assertEquals("RP-id mismatch", e.getErrorDescription());
        assertEquals("RP-id mismatch", e.getMessage());

        Throwable cause = new RuntimeException("inner");
        WebAuthnException withCause = new WebAuthnException(
                WebAuthnException.NOT_ALLOWED, "user dismissed", cause);
        assertEquals("user dismissed", withCause.getMessage());
        assertEquals(cause, withCause.getCause());
    }

    @Test
    public void exceptionCodesMatchSpecNames() {
        // Sanity-check the W3C-named codes so a typo in the constants gets
        // caught early -- server libraries do `switch (err.error)` against
        // these strings.
        assertEquals("NotAllowedError",   WebAuthnException.NOT_ALLOWED);
        assertEquals("InvalidStateError", WebAuthnException.INVALID_STATE);
        assertEquals("NotSupportedError", WebAuthnException.NOT_SUPPORTED);
        assertEquals("SecurityError",     WebAuthnException.SECURITY_ERROR);
        assertEquals("AbortError",        WebAuthnException.ABORTED);
        assertEquals("ConstraintError",   WebAuthnException.CONSTRAINT_ERROR);
    }

    // -------- WebAuthnClient async dispatch ----------------------------

    @Test
    public void clientFailsFastWithoutProvider() throws Exception {
        // No provider registered, isSupported() must be false.
        assertFalse(WebAuthnClient.isSupported());

        PublicKeyCredentialCreationOptions opts =
                PublicKeyCredentialCreationOptions.newBuilder()
                        .rp("example.com", "Example")
                        .user("dXNy", "u@x", "U")
                        .challenge("Y2hhbA")
                        .build();
        // create() must error synchronously (no thread spawned when there is
        // no provider), so .except() fires either inline or before we return
        // here. Wait briefly to cover the inline case.
        WebAuthnException err = awaitError(WebAuthnClient.getInstance().create(opts));
        assertEquals(WebAuthnException.NOT_IMPLEMENTED, err.getError());
    }

    @Test
    public void clientRoutesThroughInstalledProvider() throws Exception {
        final AtomicReference<String> capturedCreate = new AtomicReference<String>();
        final AtomicReference<String> capturedGet = new AtomicReference<String>();
        WebAuthnClient.setProvider(new WebAuthnNative() {
            public boolean isSupported() {
                return true;
            }
            public String createPasskey(String optionsJson) {
                capturedCreate.set(optionsJson);
                return "{\"id\":\"cred-99\",\"rawId\":\"cred-99\",\"type\":\"public-key\","
                        + "\"response\":{\"clientDataJSON\":\"Y2RhdGE\","
                        + "\"attestationObject\":\"YXR0\"}}";
            }
            public String getPasskey(String optionsJson) {
                capturedGet.set(optionsJson);
                return "{\"id\":\"cred-99\",\"rawId\":\"cred-99\",\"type\":\"public-key\","
                        + "\"response\":{\"clientDataJSON\":\"Y2RhdGE\","
                        + "\"authenticatorData\":\"YXV0aA\",\"signature\":\"c2ln\","
                        + "\"userHandle\":\"dXNy\"}}";
            }
        });
        assertTrue(WebAuthnClient.isSupported());

        PublicKeyCredentialCreationOptions opts =
                PublicKeyCredentialCreationOptions.newBuilder()
                        .rp("example.com", "Example")
                        .user("dXNy", "u@x", "U")
                        .challenge("Y2hhbA")
                        .build();
        PublicKeyCredential created = awaitSuccess(
                WebAuthnClient.getInstance().create(opts));
        assertEquals("cred-99", created.getId());
        assertTrue(created.isRegistration());
        assertEquals(opts.toJson(), capturedCreate.get());

        PublicKeyCredentialRequestOptions req =
                PublicKeyCredentialRequestOptions.newBuilder()
                        .rpId("example.com")
                        .challenge("Y2hhbA")
                        .build();
        PublicKeyCredential asserted = awaitSuccess(
                WebAuthnClient.getInstance().get(req));
        assertEquals("c2ln", asserted.getSignature());
        assertFalse(asserted.isRegistration());
        assertEquals(req.toJson(), capturedGet.get());
    }

    @Test
    public void clientPropagatesNativeWebAuthnException() throws Exception {
        WebAuthnClient.setProvider(new WebAuthnNative() {
            public boolean isSupported() {
                return true;
            }
            public String createPasskey(String optionsJson) throws WebAuthnException {
                throw new WebAuthnException(WebAuthnException.INVALID_STATE,
                        "credential already exists");
            }
            public String getPasskey(String optionsJson) {
                return null;
            }
        });
        PublicKeyCredentialCreationOptions opts =
                PublicKeyCredentialCreationOptions.newBuilder()
                        .rp("example.com", "Example")
                        .user("dXNy", "u@x", "U")
                        .challenge("Y2hhbA")
                        .build();
        WebAuthnException err = awaitError(WebAuthnClient.getInstance().create(opts));
        assertEquals(WebAuthnException.INVALID_STATE, err.getError());
    }

    @Test
    public void clientMapsNullReturnToUserCancelled() throws Exception {
        WebAuthnClient.setProvider(new WebAuthnNative() {
            public boolean isSupported() {
                return true;
            }
            public String createPasskey(String optionsJson) {
                return null;
            }
            public String getPasskey(String optionsJson) {
                return null;
            }
        });
        PublicKeyCredentialRequestOptions req =
                PublicKeyCredentialRequestOptions.newBuilder()
                        .rpId("example.com")
                        .challenge("Y2hhbA")
                        .build();
        WebAuthnException err = awaitError(WebAuthnClient.getInstance().get(req));
        assertEquals(WebAuthnException.NOT_ALLOWED, err.getError());
    }

    // ------------------------------------------------------------------
    //
    // Avoid the missed-notify race in AsyncResource#get(timeout): the
    // observer that wakes the timed wait registers AFTER the boolean check,
    // so a worker-thread error() that fires between check and wait is
    // missed. The .ready()/.except() callbacks are race-safe (they run
    // immediately when the resource is already done at registration time),
    // so a CountDownLatch wired through them gives deterministic results.

    private static <V> V awaitSuccess(com.codename1.util.AsyncResource<V> r)
            throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<V> ok = new AtomicReference<V>();
        final AtomicReference<Throwable> bad = new AtomicReference<Throwable>();
        r.ready(new SuccessCallback<V>() {
            public void onSucess(V value) {
                ok.set(value);
                latch.countDown();
            }
        }).except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                bad.set(t);
                latch.countDown();
            }
        });
        if (!latch.await(15, TimeUnit.SECONDS)) {
            fail("AsyncResource never completed within 15 s");
        }
        if (bad.get() != null) {
            throw new AssertionError("Expected success but got error: " + bad.get(),
                    bad.get());
        }
        assertNotNull(ok.get(), "Resource completed but value was null");
        return ok.get();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static WebAuthnException awaitError(com.codename1.util.AsyncResource r)
            throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> bad = new AtomicReference<Throwable>();
        final AtomicReference<Object> good = new AtomicReference<Object>();
        r.ready(new SuccessCallback() {
            public void onSucess(Object value) {
                good.set(value);
                latch.countDown();
            }
        }).except(new SuccessCallback<Throwable>() {
            public void onSucess(Throwable t) {
                bad.set(t);
                latch.countDown();
            }
        });
        if (!latch.await(15, TimeUnit.SECONDS)) {
            fail("AsyncResource never errored within 15 s");
        }
        if (good.get() != null) {
            fail("Expected error but resource completed successfully with: " + good.get());
        }
        Throwable t = bad.get();
        while (t != null && !(t instanceof WebAuthnException)) {
            t = t.getCause();
        }
        assertNotNull(t, "Expected a WebAuthnException in the cause chain, got: " + bad.get());
        return (WebAuthnException) t;
    }
}
