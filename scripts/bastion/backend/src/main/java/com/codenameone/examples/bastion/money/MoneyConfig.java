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

import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.ConditionalOnMissingBean;
import com.codename1.backend.annotations.Configuration;
import com.codename1.backend.Config;

/// The pieces of money movement a deployment replaces.
@Configuration
public class MoneyConfig {
    /// The device integrity check. Simulated, and only on a development
    /// profile (`dev`, `test`, `local`): it believes whatever the device
    /// reports, so on any other profile the server refuses to start until
    /// the deployment declares its own [IntegrityVerifier] bean -- one that
    /// sends the Play Integrity token to Google's `decodeIntegrityToken` and
    /// checks an App Attest assertion against Apple's, see [IntegrityVerifier].
    /// Declaring that bean makes this one step aside.
    @Bean
    @ConditionalOnMissingBean(IntegrityVerifier.class)
    public IntegrityVerifier integrityVerifier(Config config) {
        return SimulatedIntegrityVerifier.forProfile(config.isDevelopmentProfile());
    }
}
