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
package com.codename1.backend;

import com.codename1.impl.migration.MigrationRegistry;
import java.io.IOException;

/// The settings the build reads, met again when the server starts.
///
/// A few keys decide what is linked into a server: the generated entry point names an
/// optional part -- the security tables, the management endpoints, the MCP endpoint, the
/// OpenTelemetry exporters -- only for a build whose `application.properties` asked for it,
/// and a part nothing names is not in the binary. Such a key set only where the build does
/// not read it -- the environment, a system property, a properties file beside the binary --
/// used to be accepted and to do nothing.
///
/// Every one of them gets the same two rules, here and nowhere else:
///
/// - `true` at run time in a server the part was not built into stops the start, with a
///   message that says it is a build-time setting and where it goes. The part is not in the
///   binary, so there is no way to honour it.
/// - `false` at run time in a server the part was built into is honoured: the part is left
///   out. [#switchedOff(Config, String)] is how the code that would start it asks.
///
/// The keys are held as text. Naming the class that owns one would link that part into
/// every server, which is the thing the keys exist to avoid; tests hold each to its owner's
/// constant, and the build's list to this one.
final class BuildTimeSettings {
    /// `cn1.security.schema.enabled`: the security layer's tables.
    static final String SECURITY_SCHEMA = "cn1.security.schema.enabled";
    /// The name of the migration set [#SECURITY_SCHEMA] asks for.
    static final String SECURITY_SCHEMA_SET = "security";
    /// `cn1.management.enabled`: the management endpoints.
    static final String MANAGEMENT = "cn1.management.enabled";
    /// `cn1.mcp.enabled`: the MCP endpoint.
    static final String MCP = "cn1.mcp.enabled";
    /// `cn1.otel.enabled`: the OpenTelemetry exporters.
    static final String TELEMETRY = "cn1.otel.enabled";

    /// One row per key: the key, what it asks for with its verb, what its absence means
    /// for a server that was told to expect it, and what a server assembled by hand does
    /// in place of the build -- null when only the build can.
    private static final String[][] SETTINGS = {
        {SECURITY_SCHEMA, "the security tables were", "nothing would create them",
            "calls Migrations.register(SecuritySchema.migrations()) before it starts"},
        {MANAGEMENT, "the management endpoints were", "nothing would serve them", null},
        {MCP, "the MCP endpoint was", "nothing would serve it", null},
        {TELEMETRY, "the OpenTelemetry exporters were", "nothing would be exported",
            "passes an OtlpTracer to Backend.Builder.tracing() and an OtlpMetricExporter "
                    + "to metrics()"},
    };

    private BuildTimeSettings() {
    }

    /// Every build-time key, in the order the start checks them.
    static String[] keys() {
        String[] out = new String[SETTINGS.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = SETTINGS[i][0];
        }
        return out;
    }

    /// Refuses a configuration that asks at run time for something only the build can
    /// give. Called before anything is opened.
    ///
    /// Each flag says whether that part is in this server: the generated entry point put
    /// it there, or a server assembled by hand did.
    /// @param config the server's configuration, compiled-in settings included
    /// @param management whether the management endpoints were linked
    /// @param mcp whether the MCP endpoint was linked
    /// @param telemetry whether a tracer or a metric reader was given
    /// @throws IllegalStateException for the first key that is true and not built in
    /// @throws IOException if a setting cannot be read
    static void require(Config config, boolean management, boolean mcp, boolean telemetry)
            throws IOException {
        boolean[] builtIn = {
            MigrationRegistry.find(SECURITY_SCHEMA_SET) != null, management, mcp, telemetry
        };
        for (int i = 0; i < SETTINGS.length; i++) {
            String[] setting = SETTINGS[i];
            if (builtIn[i] || !asksFor(config, setting[0])) {
                continue;
            }
            throw new IllegalStateException(setting[0] + " is true in this server's run-time "
                    + "configuration, but " + setting[1] + " not built into this server, so "
                    + setting[2] + ". It is a build-time setting: the build reads it from "
                    + "the module's application.properties and nowhere else -- not from the "
                    + "environment, a system property or a properties file beside the "
                    + "server. Put " + setting[0] + "=true in the module's "
                    + "application.properties and build again."
                    + (setting[3] == null ? ""
                            : " A server assembled by hand " + setting[3] + " instead."));
        }
    }

    /// Whether the configuration resolves `key` to true. A value that cannot be read --
    /// `${NAME}` naming a variable nobody set -- asks for nothing: before this check
    /// existed nothing read these keys in a server built without their part, and a
    /// server that started then must not be stopped now by a reference it never used.
    private static boolean asksFor(Config config, String key) {
        try {
            return config.getBoolean(key, false);
        } catch (IOException unreadable) {
            return false;
        }
    }

    /// [#switchedOff(Config, String)], for a key nothing read at run time until now: a
    /// value that cannot be read leaves the part in, as it was left in before.
    /// @param config the server's configuration
    /// @param key one of the keys of this class
    /// @return true when the key is set, readable and not a truth value
    static boolean leftOut(Config config, String key) {
        try {
            return switchedOff(config, key);
        } catch (IOException unreadable) {
            return false;
        }
    }

    /// Whether the configuration says, in so many words, to leave out a part this server
    /// was built with. Unset is not that: the part was asked for at build time.
    /// @param config the server's configuration
    /// @param key one of the keys of this class
    /// @return true when the key is set and is not a truth value
    /// @throws IOException if the setting cannot be read
    static boolean switchedOff(Config config, String key) throws IOException {
        String set = config.get(key, null);
        return set != null && set.trim().length() > 0 && !config.getBoolean(key, true);
    }
}
