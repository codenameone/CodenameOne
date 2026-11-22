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
package com.codename1.maven.processors;

import com.codename1.build.SystemStreamLog;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ProcessorContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

/// The engines a generated wiring registers come from the URLs the build sees.
public class BackendEngineRegistrationTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String PG = "com.codename1.db.sql.Postgres";
    private static final String MY = "com.codename1.db.sql.MySql";
    private static final String MS = "com.codename1.db.sql.SqlServer";
    private static final String ORA = "com.codename1.db.sql.Oracle";

    @Test
    public void schemesMapAsEnginesNamedDoes() {
        assertEquals("postgres", BackendEngineRegistration.named("postgres://h/db"));
        assertEquals("postgres", BackendEngineRegistration.named("PostgreSQL://h/db"));
        assertEquals("mysql", BackendEngineRegistration.named("mysql://h/db"));
        assertEquals("mysql", BackendEngineRegistration.named("mariadb://h/db"));
        assertEquals("sqlserver", BackendEngineRegistration.named("sqlserver://h/db"));
        assertEquals("sqlserver", BackendEngineRegistration.named("mssql://h/db"));
        assertEquals("oracle", BackendEngineRegistration.named("oracle://h/db"));
        assertNull(BackendEngineRegistration.named("sqlite:app.db"));
        assertNull(BackendEngineRegistration.named("jdbc:postgresql://h/db"));
        assertNull(BackendEngineRegistration.named("${DATABASE_URL}"));
        assertEquals("postgres", BackendEngineRegistration.named("${DATABASE_URL:postgres://localhost/app}"));
        assertEquals("oracle", BackendEngineRegistration.named(" ${DB:oracle://h/s} "));
        assertNull(BackendEngineRegistration.named("${DATABASE_URL:/var/app.db}"));
        assertEquals("postgres", BackendEngineRegistration.named("postgres://${DB_HOST}/db"));
        assertNull(BackendEngineRegistration.named(null));
    }

    @Test
    public void connectorsAreDeduplicatedInAFixedOrder() {
        assertEquals(Arrays.asList(PG, MY, MS, ORA), BackendEngineRegistration.connectors(Arrays.asList(
                "oracle://h/x", "mssql://h/x", "mariadb://h/x", "postgres://h/x", "postgresql://h/y",
                "sqlite:x.db", null)));
        assertEquals(Collections.emptyList(),
                BackendEngineRegistration.connectors(Arrays.asList("sqlite:x.db", null)));
    }

    @Test
    public void propertiesFilesAndProfilesAreRead() throws Exception {
        assumeNoBuildOverride();
        File module = tmp.newFolder("backend");
        write(module, "application.properties", "cn1.datasource.url=sqlite:app.db\n");
        write(module, "application-prod.properties", "cn1.datasource.url=postgres://db/app\n");
        write(module, "application-tenants.properties", "cn1.tenancy.datasource.url=mysql://db/t\n");
        assertEquals(Arrays.asList(PG, MY), BackendEngineRegistration.connectors(context(module)));
    }

    @Test
    public void noUrlRegistersNothing() throws Exception {
        assumeNoBuildOverride();
        File module = tmp.newFolder("plain");
        write(module, "application.properties", "cn1.server.port=8080\n");
        assertEquals(Collections.emptyList(), BackendEngineRegistration.connectors(context(module)));
        File empty = tmp.newFolder("empty");
        assertEquals(Collections.emptyList(), BackendEngineRegistration.connectors(context(empty)));
    }

    @Test
    public void theBuildsOwnUrlIsAdded() throws Exception {
        assumeNoBuildOverride();
        File module = tmp.newFolder("override");
        write(module, "application.properties", "cn1.datasource.url=${DATABASE_URL}\n");
        ProcessorContext ctx = context(module);
        assertEquals(Collections.emptyList(), BackendEngineRegistration.connectors(ctx));
        ctx.setAttribute(BackendEngineRegistration.OVERRIDE_ATTRIBUTE, "sqlserver://db/app");
        List<String> found = BackendEngineRegistration.connectors(ctx);
        assertEquals(Collections.singletonList(MS), found);
    }

    private static void assumeNoBuildOverride() {
        assumeTrue(System.getProperty("cn1.datasource.url") == null
                && System.getenv("CN1_DATASOURCE_URL") == null);
    }

    private static ProcessorContext context(File module) {
        File classes = new File(module, "target/classes");
        classes.mkdirs();
        return new ProcessorContext(classes, new File(module, "target/stubs"),
                new LinkedHashMap<String, AnnotatedClass>(), new SystemStreamLog(), module,
                null, null, null, null, null);
    }

    private static void write(File module, String name, String text) throws Exception {
        File f = new File(module, "src/main/resources/" + name);
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
    }
}
