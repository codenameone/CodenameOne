package com.codename1.impl.javase;

import com.codename1.security.shield.PinSet;
import com.codename1.security.shield.ShieldConfig;
import com.codename1.security.shield.ShieldException;
import com.codename1.security.shield.ShieldToken;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The simulator's shield switches have to change behaviour, not just state.
 *
 * <p>Every assertion here failed before {@link JavaSEShieldEngine} existed, for the same
 * reason: the menu wrote a field in {@link JavaSEShield}, the status dialog read the
 * field back, and nothing in the request path consulted either -- so "Force Pin Mismatch"
 * left requests succeeding and the fail-closed branch it exists to reach was unreachable.
 * A control that reports a state it does not cause is worse than no control.</p>
 */
class JavaSEShieldEngineTest {

    private JavaSEShieldEngine engine;

    @BeforeEach
    void setUp() {
        JavaSEShield.reset();
        engine = new JavaSEShieldEngine();
        engine.initialize(null, new ShieldConfig()
                .protect("api.example.com")
                .protect("*.wild.example.com"));
    }

    @AfterEach
    void tearDown() {
        JavaSEShield.reset();
    }

    @Test
    void forcingAPinMismatchActuallyFailsTheCheck() {
        assertTrue(engine.verifyPins("api.example.com", new String[] {"whatever"},
                new String[] {"whatever"}), "an unarmed simulator must not fail pins");

        JavaSEShield.forcePinMismatch = true;
        assertFalse(engine.verifyPins("api.example.com", new String[] {"whatever"},
                new String[] {"whatever"}), "the switch has to reach the pin check");
    }

    @Test
    void theForcedMismatchIsOneShot() {
        // The switch is labelled "on next request". Left armed, a developer testing what
        // happens after a mismatch would be testing a permanently broken app instead.
        JavaSEShield.forcePinMismatch = true;
        assertFalse(engine.verifyPins("api.example.com", new String[0], new String[0]));
        assertTrue(engine.verifyPins("api.example.com", new String[0], new String[0]),
                "the second request should be back to normal");
        assertFalse(JavaSEShield.forcePinMismatch, "and the menu should show it disarmed");
    }

    @Test
    void registeredHostsAreEnforcedSoTheMismatchHasSomethingToActOn() {
        // Without pins for the host, ShieldNetworkGuard never asks verifyPins() at all --
        // PinSet.isEnforcedFor() is false for a host with no pins. So a pin set that
        // covers the app's own hosts is load-bearing for the force switch, not decoration.
        PinSet pins = engine.getPinSet();
        assertTrue(pins.isEnforcedFor("api.example.com"),
                "a registered host must be enforced or the mismatch switch does nothing");
        assertFalse(pins.isEnforcedFor("unregistered.example.com"),
                "a host the app never registered must be left alone");
    }

    @Test
    void failingThePinFetchYieldsNoEnforcementRatherThanAMismatch() {
        // Pinning fails OPEN on unavailability everywhere in this design; the simulator
        // has to be able to demonstrate that rather than have it asserted in a doc.
        JavaSEShield.failPinFetch = true;
        PinSet pins = engine.getPinSet();
        assertFalse(pins.isEnforcedFor("api.example.com"),
                "an unavailable pin set is not a mismatch");
    }

    @Test
    void aSimulatedTokenIsMarkedAndHonoursTheConfiguredLifetime() throws Exception {
        JavaSEShield.tokenTtlSeconds = 120;
        ShieldToken token = engine.fetchToken(null);

        assertNotNull(token);
        assertTrue(token.isValid(), "a fresh simulated token should be usable");
        assertTrue(token.getValue().contains(JavaSEShieldEngine.SIMULATED_MARKER),
                "a backend that ever sees one of these is talking to a simulator, and the "
                + "value is the part that ends up pasted into a bug report");
        assertTrue(token.getMillisUntilExpiry() > 0);
        assertEquals(token, engine.getCachedToken());
    }

    @Test
    void servingAnExpiredTokenProducesOneThatIsActuallyExpired() throws Exception {
        // Backdating fetchedAt alone would not do it: validity is answered from a
        // monotonic reading taken at construction, precisely so a device clock cannot
        // make a lapsed token look fresh.
        JavaSEShield.serveExpiredToken = true;
        ShieldToken token = engine.fetchToken(null);
        assertFalse(token.isValid(), "the switch has to produce a token that fails");
        assertEquals(0L, token.getMillisUntilExpiry());
    }

    @Test
    void eachSimulatedFailureCarriesItsOwnStatus() {
        // The distinction between "cannot reach the shield" and "the shield says no" is
        // the most important thing in the API, so each outcome has to arrive as itself.
        JavaSEShield.attestOutcome = JavaSEShield.AttestOutcome.FAIL_REJECTED;
        assertEquals(com.codename1.security.shield.ShieldStatus.REJECTED,
                assertThrows(ShieldException.class, () -> engine.fetchToken(null)).getStatus());

        JavaSEShield.attestOutcome = JavaSEShield.AttestOutcome.FAIL_NO_NETWORK;
        assertEquals(com.codename1.security.shield.ShieldStatus.NO_NETWORK,
                assertThrows(ShieldException.class, () -> engine.fetchToken(null)).getStatus());

        JavaSEShield.attestOutcome = JavaSEShield.AttestOutcome.FAIL_RATE_LIMITED;
        assertEquals(com.codename1.security.shield.ShieldStatus.RATE_LIMITED,
                assertThrows(ShieldException.class, () -> engine.fetchToken(null)).getStatus());
    }

    @Test
    void simulatedDeviceSignalsReachTheEngine() {
        JavaSEShield.simHooked = true;
        JavaSEShield.simUntrustedAccessibility = true;
        assertEquals(2, engine.collectSignals().length,
                "the device-signal switches feed what the server is told");
    }
}
