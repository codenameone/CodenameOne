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
package com.codename1.ai;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Coverage for the package-private {@link SimulatorRedirect#maybeWrap} that the
 * simulator uses to reroute LLM traffic to a local Ollama. The platform name is
 * driven through the test implementation and the {@code cn1.ai.*} system
 * properties select the mode; the test restores both after each case.
 */
class SimulatorRedirectTest extends UITestBase {

    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434/v1";

    @AfterEach
    void clearRedirectProperties() {
        System.clearProperty("cn1.ai.simulatorRedirect");
        System.clearProperty("cn1.ai.ollamaDetected");
        System.clearProperty("cn1.ai.ollamaUrl");
        System.clearProperty("cn1.ai.ollamaModel");
    }

    private LlmClient realClient() {
        return LlmClient.localOpenAiCompatible("http://real.example/v1", "key", "model");
    }

    @Test
    void offSimulatorAlwaysPassesThrough() {
        // Default test platform is not the simulator, so even an explicit
        // "ollama" mode must leave the client untouched.
        implementation.setPlatformName("test");
        System.setProperty("cn1.ai.simulatorRedirect", "ollama");
        LlmClient real = realClient();
        assertSame(real, SimulatorRedirect.maybeWrap(real));
    }

    @Test
    void simulatorOllamaModeRedirectsToLocalClient() {
        implementation.setPlatformName("se");
        System.setProperty("cn1.ai.simulatorRedirect", "ollama");
        LlmClient real = realClient();
        LlmClient wrapped = SimulatorRedirect.maybeWrap(real);
        assertNotSame(real, wrapped);
        assertEquals(DEFAULT_OLLAMA_URL, wrapped.getBaseUrl());
        assertEquals("openai", wrapped.getProvider());
    }

    @Test
    void simulatorAutoWithoutDetectionPassesThrough() {
        implementation.setPlatformName("se");
        // mode defaults to "auto"; with no ollamaDetected flag it must not redirect.
        LlmClient real = realClient();
        assertSame(real, SimulatorRedirect.maybeWrap(real));
    }

    @Test
    void simulatorAutoWithDetectionRedirects() {
        implementation.setPlatformName("se");
        System.setProperty("cn1.ai.simulatorRedirect", "auto");
        System.setProperty("cn1.ai.ollamaDetected", "true");
        LlmClient real = realClient();
        assertNotSame(real, SimulatorRedirect.maybeWrap(real));
    }

    @Test
    void simulatorDisabledModePassesThrough() {
        implementation.setPlatformName("se");
        System.setProperty("cn1.ai.simulatorRedirect", "disabled");
        LlmClient real = realClient();
        assertSame(real, SimulatorRedirect.maybeWrap(real));
    }

    @Test
    void customOllamaUrlIsHonouredWhenRedirecting() {
        implementation.setPlatformName("se");
        System.setProperty("cn1.ai.simulatorRedirect", "ollama");
        System.setProperty("cn1.ai.ollamaUrl", "http://custom-host:9999/v1");
        LlmClient wrapped = SimulatorRedirect.maybeWrap(realClient());
        assertEquals("http://custom-host:9999/v1", wrapped.getBaseUrl());
    }
}
