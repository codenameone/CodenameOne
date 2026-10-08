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
package com.codename1.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.DataSource;
import com.codename1.backend.Migrations;
import com.codename1.migration.MigrateResult;
import com.codename1.migration.MigrationInfo;
import com.codename1.migration.MigrationState;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/// What the security set writes as the script of each version, and what it makes of a
/// history written before the versions had names of their own.
class SecuritySchemaHistoryTest {
    /// What every row of such a history holds: the one class behind all versions.
    private static final String BEFORE = "com.codename1.backend.security.SecuritySchema.Tables";

    @TempDir
    File dir;

    @Test
    void upgradeRetainsAnExistingRateLimitUntilItsWindowRollsOver() throws Exception {
        DataSource pool = DataSource.open(new File(dir, "upgrade.db").getPath(), 2, 5000, 10000);
        try {
            Migrations.of(pool, SecuritySchema.migrations()).target("9").migrate();
            final long[] now = {1700000000000L};
            pool.execute("INSERT INTO cn1_rate_limit (limit_key, window_start, hits) VALUES (?, ?, 1)",
                    new Object[] {"daily|client", Long.valueOf(now[0])});
            assertEquals(1, Migrations.of(pool, SecuritySchema.migrations()).target("10").migrate().getMigrationsExecuted());
            com.codename1.backend.security.ratelimit.JdbcRateLimiter limiter =
                    new com.codename1.backend.security.ratelimit.JdbcRateLimiter(pool, "daily", 1, 86400);
            limiter.setClock(new Clock() {
                @Override
                public long currentTimeMillis() {
                    return now[0];
                }
            });
            now[0] += 3601000;
            assertEquals(0, limiter.deleteExpired(3600));
            assertFalse(limiter.tryAcquire("client"));
            now[0] += 86400000;
            assertTrue(limiter.tryAcquire("client"));
            now[0] += 90001000;
            assertEquals(1, limiter.deleteExpired(3600));
        } finally {
            pool.close();
        }
    }

    private static List<String> scripts(DataSource pool) throws Exception {
        List<String> out = new ArrayList<String>();
        for (Object row : pool.query(
                "SELECT script FROM cn1_security_schema_history ORDER BY installed_rank", null)) {
            out.add(String.valueOf(((Map) row).get("script")));
        }
        return out;
    }

    @Test
    void upgradingRememberMeKeepsExistingTokens() throws Exception {
        DataSource pool = DataSource.open(new File(dir, "remember-upgrade.db").getPath(), 2, 5000, 10000);
        try {
            Migrations.of(pool, SecuritySchema.migrations()).target("10").migrate();
            pool.execute("INSERT INTO cn1_persistent_logins (series, username_key, username, token_hash, last_used) "
                    + "VALUES ('browser', 'ada', 'Ada', 'old-hash', 1000)", null);
            assertEquals(1, Migrations.of(pool, SecuritySchema.migrations()).migrate().getMigrationsExecuted());
            com.codename1.backend.security.rememberme.JdbcTokenRepository repository =
                    new com.codename1.backend.security.rememberme.JdbcTokenRepository(pool);
            assertEquals("old-hash", repository.getTokenForSeries("browser").getTokenHash());
            assertEquals(null, repository.getTokenForSeries("browser").getPreviousTokenHash());
            assertTrue(repository.updateToken("browser", "old-hash", "new-hash", 2000));
            assertEquals("old-hash", repository.getTokenForSeries("browser").getPreviousTokenHash());
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("each version is recorded, and reported, under a name that says which it is")
    void everyVersionHasItsOwnName() throws Exception {
        DataSource pool = DataSource.open(new File(dir, "named.db").getPath(), 2, 5000, 10000);
        try {
            MigrateResult result = Migrations.of(pool, SecuritySchema.migrations()).migrate();
            assertEquals(11, result.getMigrationsExecuted());
            assertEquals(11, new TreeSet<String>(result.getApplied()).size(),
                    "eleven versions reported under fewer names: " + result.getApplied());
            assertEquals("com.codename1.backend.security.SecuritySchema.V1__users_and_authorities",
                    result.getApplied().get(0));
            assertEquals("com.codename1.backend.security.SecuritySchema.V9__passkeys",
                    result.getApplied().get(8));
            assertEquals(result.getApplied(), scripts(pool));
        } finally {
            pool.close();
        }
    }

    @Test
    @DisplayName("a history written under the class name still validates, and migrates to nothing")
    void aHistoryWrittenBeforeTheNamesStillValidates() throws Exception {
        DataSource pool = DataSource.open(new File(dir, "before.db").getPath(), 2, 5000, 10000);
        try {
            Migrations.of(pool, SecuritySchema.migrations()).migrate();
            // Exactly what a server from before this change left behind.
            pool.execute("UPDATE cn1_security_schema_history SET script = ?", new Object[] {BEFORE});
            List<String> before = scripts(pool);
            assertEquals(1, new TreeSet<String>(before).size());

            Migrations.of(pool, SecuritySchema.migrations()).validate();
            MigrateResult again = Migrations.of(pool, SecuritySchema.migrations()).migrate();
            assertEquals(0, again.getMigrationsExecuted());
            assertEquals("11", again.getTargetVersion());
            MigrationInfo[] info = Migrations.of(pool, SecuritySchema.migrations()).info();
            assertEquals(11, info.length);
            for (MigrationInfo row : info) {
                assertEquals(MigrationState.SUCCESS, row.getState(), "version " + row.getVersion());
            }
            // Nothing rewrote the rows: the history is a record of what ran.
            assertEquals(before, scripts(pool));
        } finally {
            pool.close();
        }
    }
}
