/*
 * Copyright (c) 2008-2026, Codename One and/or its affiliates. All rights reserved.
 * Distributed under the same terms as the rest of Codename One.
 */
package com.codename1.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The strict biometric binding is opt-in: an app that does not ask keeps the
/// key rules it was installed with.
class SecureStorageBindingTest {
    @Test
    void strictBindingIsOffUntilAsked() {
        SecureStorage storage = new SecureStorage() {
        };
        assertFalse(storage.isStrictBiometricBinding());
        storage.setStrictBiometricBinding(true);
        assertTrue(storage.isStrictBiometricBinding());
        storage.setStrictBiometricBinding(false);
        assertFalse(storage.isStrictBiometricBinding());
    }
}
