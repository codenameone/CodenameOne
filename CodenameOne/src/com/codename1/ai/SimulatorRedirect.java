/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

import com.codename1.ui.Display;

/// Centralizes the JavaSE simulator's "redirect every LLM call to a
/// local Ollama" behaviour. Always a no-op on device builds because
/// the relevant system property is only set inside `JavaSEPort`.
///
/// Three modes via `cn1.ai.simulatorRedirect`:
/// - `disabled` / unset on device: passthrough.
/// - `auto`: on simulator only; if `JavaSEPort` detected Ollama on
///   startup, redirect. Default in the simulator.
/// - `ollama`: force-redirect regardless of detection.
///
/// The decision is made *per factory call* so an app can flip the
/// property at runtime (the simulator's Tools menu does this).
final class SimulatorRedirect {

    private SimulatorRedirect() {
    }

    static LlmClient maybeWrap(LlmClient real) {
        if (!isSimulator()) {
            return real;
        }
        // CN1 core's java.lang.System exposes only the single-arg
        // getProperty; default-value handling is done by hand.
        String mode = readProperty("cn1.ai.simulatorRedirect", "auto");
        boolean force = "ollama".equalsIgnoreCase(mode);
        boolean auto = "auto".equalsIgnoreCase(mode);
        if (!force && !auto) {
            return real;
        }
        if (auto && !"true".equals(readProperty("cn1.ai.ollamaDetected", "false"))) {
            return real;
        }
        // Build a fresh Ollama-pointed OpenAI-compatible client. We
        // intentionally lose the original baseUrl/key here; the user
        // opted into local mode and the simulator banner already
        // disclosed that.
        String localUrl = readProperty("cn1.ai.ollamaUrl", "http://localhost:11434/v1");
        String model = readProperty("cn1.ai.ollamaModel", "llama3.2");
        return LlmClient.localOpenAiCompatible(localUrl, "", model);
    }

    private static String readProperty(String key, String defaultValue) {
        String v = System.getProperty(key);
        return v != null ? v : defaultValue;
    }

    private static boolean isSimulator() {
        try {
            String platform = Display.getInstance().getPlatformName();
            // JavaSEPort returns "se" for the simulator on most
            // configurations; check defensively in case that ever
            // changes.
            return "se".equalsIgnoreCase(platform)
                    || "javase".equalsIgnoreCase(platform);
        } catch (Throwable t) {
            return false;
        }
    }
}
