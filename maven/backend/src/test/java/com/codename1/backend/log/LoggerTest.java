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
package com.codename1.backend.log;

import com.codename1.backend.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggerTest {
    private final List<String> lines = new ArrayList<String>();
    private final List<Throwable> errors = new ArrayList<Throwable>();

    private void capture() {
        Logging.setSink(new LogSink() {
            @Override
            public void write(Level level, String logger, String line, Throwable error) {
                lines.add(line);
                errors.add(error);
            }
        });
    }

    @AfterEach
    void reset() {
        Logging.setSink(null);
        Logging.setLevel("root", null);
        Logging.setLevel("com.example", null);
        Logging.setLevel("com.example.push", null);
        MDC.clear();
    }

    @Test
    void placeholdersAndAThrowableLast() {
        capture();
        Logger log = Logger.getLogger("com.example.Push");
        IllegalStateException failure = new IllegalStateException("x");
        log.info("sent {} to {} \\{}", 3, "app", failure);
        assertEquals(1, lines.size());
        String line = lines.get(0);
        assertTrue(line.matches("\\d{4}-\\d\\d-\\d\\dT\\d\\d:\\d\\d:\\d\\d\\.\\d{3}Z INFO  \\[.+\\] com\\.example\\.Push - sent 3 to app \\{\\}"), line);
        assertSame(failure, errors.get(0));
        log.warn("only {}", failure);
        assertNull(errors.get(1), "a throwable a placeholder took is not also the stack trace");
    }

    @Test
    void levelsComeFromTheLongestConfiguredName() throws Exception {
        Properties p = new Properties();
        p.setProperty("logging.level.root", "warn");
        p.setProperty("logging.level.com.example", "INFO");
        p.setProperty("logging.level.com.example.push", "debug");
        Logging.configure(Config.of(p, null));
        assertTrue(Logger.getLogger("com.example.push.Apns").isDebugEnabled());
        assertFalse(Logger.getLogger("com.example.pushy.X").isDebugEnabled());
        assertTrue(Logger.getLogger("com.example.pushy.X").isInfoEnabled());
        assertFalse(Logger.getLogger("org.other.Y").isInfoEnabled());
        assertTrue(Logger.getLogger("org.other.Y").isWarnEnabled());
        Logger cached = Logger.getLogger("org.other.Y");
        assertFalse(cached.isInfoEnabled());
        Logging.setLevel("org.other", Level.TRACE);
        assertTrue(cached.isTraceEnabled(), "a change reaches a logger that cached its level");
    }

    @Test
    void anUnknownLevelIsRefused() {
        Properties p = new Properties();
        p.setProperty("logging.level.com.example", "LOUD");
        assertThrows(java.io.IOException.class, () -> Logging.configure(Config.of(p, null)));
    }

    @Test
    void theContextMapIsAppended() {
        capture();
        MDC.put("request", "r-1");
        Logger.getLogger("a").info("hi");
        MDC.remove("request");
        Logger.getLogger("a").info("bye");
        assertTrue(lines.get(0).endsWith(" - hi {request=r-1}"), lines.get(0));
        assertTrue(lines.get(1).endsWith(" - bye"), lines.get(1));
    }

    @Test
    void isoTimestamps() {
        StringBuilder out = new StringBuilder();
        Timestamps.appendIso(out, 0L);
        out.append(' ');
        Timestamps.appendIso(out, 951782400123L);
        out.append(' ');
        Timestamps.appendIso(out, -1L);
        assertEquals("1970-01-01T00:00:00.000Z 2000-02-29T00:00:00.123Z 1969-12-31T23:59:59.999Z", out.toString());
    }
}
