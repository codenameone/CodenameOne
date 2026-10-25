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
package com.codenameone.examples.bastion.money;

/// Checks the integrity token a device sends when it registers, and answers
/// what the platform vouches for.
///
/// The real check is the platform's: Google Play Integrity on Android (the
/// server sends the token to Google's `decodeIntegrityToken` with the
/// project's service account and reads `deviceIntegrity` and
/// `appIntegrity` out of the verdict), App Attest on iOS. Both need an app
/// built by the Codename One build daemon, whose signing identity is what
/// the verdict names, so this sample ships [SimulatedIntegrityVerifier] and a
/// deployment replaces the bean in [MoneyConfig].
public interface IntegrityVerifier {
    /// What the platform says about the device that sent `token`.
    Verdict verify(String platform, String token, boolean reportedRooted,
            boolean reportedAttested) throws java.io.IOException;

    /// The same check with the nonce the token was requested over -- the
    /// device public key the registration carries -- for verifiers that bind
    /// the token to it, as App Attest does. The default ignores the nonce.
    default Verdict verify(String platform, String token, String nonce, boolean reportedRooted,
            boolean reportedAttested) throws java.io.IOException {
        return verify(platform, token, reportedRooted, reportedAttested);
    }

    /// A verdict: whether the platform vouched for the app and the device,
    /// and whether the device is rooted or jailbroken.
    final class Verdict {
        private final boolean attested;
        private final boolean rooted;

        public Verdict(boolean attested, boolean rooted) {
            this.attested = attested;
            this.rooted = rooted;
        }

        public boolean attested() {
            return attested;
        }

        public boolean rooted() {
            return rooted;
        }
    }
}
