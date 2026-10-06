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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.security.SecuritySchema;
import com.codename1.impl.backend.BackendAccess;
import com.codename1.impl.migration.MigrationRegistry;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/// A setting only the build can act on, met at run time: refused when it asks
/// for what was not built in, honoured when it asks for less.
class BuildTimeSettingsTest {
    private static final HttpServer.Handler NOTHING = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            return null;
        }
    };

    @TempDir
    File dir;

    private DataSource pool;

    @AfterEach
    void close() {
        if (pool != null) {
            pool.close();
        }
        MigrationRegistry.unregister(SecuritySchema.NAME);
    }

    private static Config config(String... keysAndValues) {
        Properties settings = new Properties();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            settings.setProperty(keysAndValues[i], keysAndValues[i + 1]);
        }
        return Config.of(settings, "prod");
    }

    private DataSource open() throws IOException {
        pool = DataSource.open(new File(dir, "security.db").getPath(), 2, 5000, 10000);
        return pool;
    }

    private boolean exists(String table) {
        try {
            pool.queryOne("SELECT COUNT(*) AS n FROM " + table, null);
            return true;
        } catch (IOException missing) {
            return false;
        }
    }

    @Test
    @DisplayName("the core's copy of the key and the set name are the security layer's own")
    void theNamesAgree() {
        assertEquals(SecuritySchema.ENABLED, Migrations.SECURITY_SCHEMA_KEY);
        assertEquals(SecuritySchema.NAME, Migrations.SECURITY_SCHEMA_SET);
        assertEquals(SecuritySchema.NAME, SecuritySchema.migrations().getName());
    }

    @Test
    @DisplayName("the key true at run time in a build without the tables stops the start, and says where it goes")
    void trueAtRunTimeWithoutTheTablesBuiltIn() throws Exception {
        open();
        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> Backend.builder(config(SecuritySchema.ENABLED, "true")).quiet().port(0)
                        .handler(NOTHING).dataSource(pool).start());
        assertEquals("cn1.security.schema.enabled is true in this server's run-time "
                + "configuration, but the security tables were not built into this server, so "
                + "nothing would create them. It is a build-time setting: the build reads it "
                + "from the module's application.properties and nowhere else -- not from the "
                + "environment, a system property or a properties file beside the server. Put "
                + "cn1.security.schema.enabled=true in the module's application.properties and "
                + "build again. A server assembled by hand calls "
                + "Migrations.register(SecuritySchema.migrations()) before it starts instead.",
                refused.getMessage());
        assertFalse(exists("cn1_users"));

        // Absent, or false, in such a build: nothing to refuse.
        for (Config fine : new Config[] {config(), config(SecuritySchema.ENABLED, "false")}) {
            Backend backend = Backend.builder(fine).quiet().port(0).handler(NOTHING)
                    .dataSource(pool).start();
            backend.stop();
        }
    }

    @Test
    @DisplayName("what the build compiled in and registered is not a mismatch")
    void compiledInAndRegistered() throws Exception {
        open();
        // What the generated entry point does for a module whose
        // application.properties has the key: the setting, compiled in as the
        // bottom layer, and the set, registered.
        Backend.Builder builder = Backend.builder(config()).quiet().port(0).handler(NOTHING)
                .dataSource(pool);
        BackendAccess.get().compiledSettings(builder,
                new String[] {SecuritySchema.ENABLED, "true"});
        Migrations.register(SecuritySchema.migrations());
        Backend backend = builder.start();
        try {
            assertTrue(exists("cn1_users"));
            assertTrue(exists("cn1_security_schema_history"));
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("false at run time in a build with the tables leaves them out")
    void falseAtRunTimeIsHonoured() throws Exception {
        open();
        Backend.Builder builder = Backend.builder(config(SecuritySchema.ENABLED, "false")).quiet()
                .port(0).handler(NOTHING).dataSource(pool);
        BackendAccess.get().compiledSettings(builder,
                new String[] {SecuritySchema.ENABLED, "true"});
        Migrations.register(SecuritySchema.migrations());
        Backend backend = builder.start();
        try {
            assertFalse(exists("cn1_users"));
            assertFalse(exists("cn1_security_schema_history"));
        } finally {
            backend.stop();
        }
    }
    // ---- the same two rules for every key the build reads ----

    /// A tracer that records whether the start asked it to open.
    private static final class Opened implements Tracer {
        boolean asked;

        @Override
        public boolean open(Config config) {
            asked = true;
            return false;
        }

        @Override
        public Span startSpan(String name, int kind, Span parent, String traceparent,
                String tracestate) {
            return null;
        }

        @Override
        public void flush(int timeoutMillis) {
        }

        @Override
        public void shutdown(int timeoutMillis) {
        }

        @Override
        public HttpServer.Handler relay() {
            return null;
        }

        @Override
        public void metrics(java.util.Map out) {
        }
    }

    @Test
    @DisplayName("each key is its owner's, and the list is the four the build reads")
    void theKeysAreTheirOwners() {
        assertEquals(java.util.Arrays.asList(SecuritySchema.ENABLED,
                com.codename1.impl.backend.Management.ENABLED,
                com.codename1.impl.backend.mcp.McpServer.ENABLED, "cn1.otel.enabled"),
                java.util.Arrays.asList(BuildTimeSettings.keys()));
    }

    private static String refusal(String key, String what, String consequence, String byHand) {
        return key + " is true in this server's run-time configuration, but " + what
                + " not built into this server, so " + consequence + ". It is a build-time "
                + "setting: the build reads it from the module's application.properties and "
                + "nowhere else -- not from the environment, a system property or a properties "
                + "file beside the server. Put " + key + "=true in the module's "
                + "application.properties and build again." + byHand;
    }

    @Test
    @DisplayName("management, MCP and OpenTelemetry asked for at run time in a build without them stop the start")
    void everyOtherKeyIsRefusedTheSameWay() {
        String[][] cases = {
            {"cn1.management.enabled", refusal("cn1.management.enabled",
                    "the management endpoints were", "nothing would serve them", "")},
            {"cn1.mcp.enabled", refusal("cn1.mcp.enabled", "the MCP endpoint was",
                    "nothing would serve it", "")},
            {"cn1.otel.enabled", refusal("cn1.otel.enabled", "the OpenTelemetry exporters were",
                    "nothing would be exported", " A server assembled by hand passes an "
                    + "OtlpTracer to Backend.Builder.tracing() and an OtlpMetricExporter to "
                    + "metrics() instead.")},
        };
        for (String[] c : cases) {
            IllegalStateException refused = assertThrows(IllegalStateException.class,
                    () -> Backend.builder(config(c[0], "true")).quiet().port(0)
                            .handler(NOTHING).start(), c[0]);
            assertEquals(c[1], refused.getMessage());
            // Absent or false in such a build: nothing was asked for.
            for (Config fine : new Config[] {config(), config(c[0], "false")}) {
                try {
                    Backend.builder(fine).quiet().port(0).handler(NOTHING).start().stop();
                } catch (Exception failed) {
                    throw new AssertionError(c[0] + " absent or false must start", failed);
                }
            }
        }
    }

    @Test
    @DisplayName("the same keys in a server built with the part start it")
    void builtInIsNotAMismatch() throws Exception {
        Properties settings = new Properties();
        settings.setProperty("cn1.management.enabled", "true");
        settings.setProperty("cn1.management.token", "operator-token");
        settings.setProperty("cn1.mcp.enabled", "true");
        settings.setProperty("cn1.mcp.token", "agent-token");
        settings.setProperty("cn1.otel.enabled", "true");
        Opened tracer = new Opened();
        Backend backend = Backend.builder(Config.of(settings, "prod")).quiet().port(0)
                .handler(NOTHING).management().mcp(null).tracing(tracer).start();
        try {
            assertTrue(tracer.asked, "a tracer that was built in and asked for is opened");
        } finally {
            backend.stop();
        }
    }

    @Test
    @DisplayName("cn1.otel.enabled=false at run time leaves a built-in tracer unopened")
    void telemetryFalseAtRunTimeIsHonoured() throws Exception {
        Opened tracer = new Opened();
        Backend.Builder builder = Backend.builder(config("cn1.otel.enabled", "false")).quiet()
                .port(0).handler(NOTHING).tracing(tracer);
        // As the build compiles it in for a module that asked.
        BackendAccess.get().compiledSettings(builder, new String[] {"cn1.otel.enabled", "true"});
        builder.start().stop();
        assertFalse(tracer.asked, "told at run time not to export, the tracer was opened anyway");

        // Unset at run time, the compiled-in true stands.
        Opened kept = new Opened();
        Backend.Builder asBuilt = Backend.builder(config()).quiet().port(0).handler(NOTHING)
                .tracing(kept);
        BackendAccess.get().compiledSettings(asBuilt, new String[] {"cn1.otel.enabled", "true"});
        asBuilt.start().stop();
        assertTrue(kept.asked);
    }

    @Test
    @DisplayName("a @BackendTest application is built without those parts and is not refused for them")
    void theTestApplicationIsNotAPackagedServer() throws Exception {
        open();
        Backend.Builder builder = Backend.builder(config()).quiet().port(0).handler(NOTHING);
        BackendAccess.get().testApplication(builder);
        // The module's application.properties, compiled into the test build too.
        BackendAccess.get().compiledSettings(builder, new String[] {
            "cn1.management.enabled", "true", "cn1.management.token", "t",
            "cn1.mcp.enabled", "true", "cn1.otel.enabled", "true"});
        builder.start().stop();

        // The tables are another matter: the test build registers them when asked, so
        // asked for and absent is still the mismatch.
        Backend.Builder tables = Backend.builder(config(SecuritySchema.ENABLED, "true")).quiet()
                .port(0).handler(NOTHING).dataSource(pool);
        BackendAccess.get().testApplication(tables);
        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> tables.start());
        assertTrue(refused.getMessage().startsWith("cn1.security.schema.enabled is true"),
                refused.getMessage());
    }
}
