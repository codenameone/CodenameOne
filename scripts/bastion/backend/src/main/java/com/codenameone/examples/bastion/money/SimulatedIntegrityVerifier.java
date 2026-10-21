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

/// Takes the device's word for it, which is exactly what a real verifier
/// must never do: a rooted phone reports whatever its owner -- or the malware
/// on it -- likes.
///
/// It exists so the rest of the policy can be built and tested before the app
/// is signed by the build daemon and a real verdict can be had. One rule it
/// keeps from the real thing: no token, no attestation. A device that sent
/// nothing to check is never counted as attested, whatever it claims.
public class SimulatedIntegrityVerifier implements IntegrityVerifier {
    @Override
    public Verdict verify(String platform, String token, boolean reportedRooted,
            boolean reportedAttested) {
        boolean hasToken = token != null && token.length() > 0;
        return new Verdict(reportedAttested && hasToken && !reportedRooted, reportedRooted);
    }
}
