/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.parley.api;

import com.codename1.annotations.Mapped;

import java.util.List;

/// What a device publishes when it is linked to an account.
///
/// @param name The device's name.
/// @param platform ios, android, web, desktop or simulator.
/// @param identityKey The device's identity public key, base64 SubjectPublicKeyInfo. It both agrees keys and signs the signed prekey.
/// @param signedPreKey The signed prekey.
/// @param oneTimePreKeys A batch of one-time prekeys.
@Mapped
public record DeviceRegistrationDto(
        String name,
        String platform,
        String identityKey,
        SignedPreKeyDto signedPreKey,
        List<PreKeyDto> oneTimePreKeys) {
}
