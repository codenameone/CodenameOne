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
package com.codename1.jdkparity;

import com.codename1.impl.JdkHost;
import jdkparity.java.util.logging.Level;
import jdkparity.java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * JavaAPI's {@code java.util.logging} against the JDK's: levels parse and
 * compare alike, and a logger writes through the host's log at the folded
 * level.
 */
class LoggingParityTest {

    @AfterEach
    void restore() {
        JdkHost.install(null);
    }

    @Test
    void levelsParseAndCompareAsTheJdksDo() {
        for (java.util.logging.Level jdk : new java.util.logging.Level[] {java.util.logging.Level.OFF,
            java.util.logging.Level.SEVERE, java.util.logging.Level.WARNING, java.util.logging.Level.INFO,
            java.util.logging.Level.CONFIG, java.util.logging.Level.FINE, java.util.logging.Level.FINER,
            java.util.logging.Level.FINEST, java.util.logging.Level.ALL}) {
            Level mine = Level.parse(jdk.getName());
            assertEquals(jdk.getName(), mine.getName());
            assertEquals(jdk.intValue(), mine.intValue());
            assertSame(mine, Level.parse(String.valueOf(jdk.intValue())));
            assertEquals(jdk.toString(), mine.toString());
        }
        Level custom = new Level("AUDIT", 850) {
            private static final long serialVersionUID = 1L;
        };
        assertEquals("AUDIT", custom.getName());
        assertNull(custom.getResourceBundleName());
        assertEquals(java.util.logging.Level.parse("123").getName(), Level.parse("123").getName());
        assertThrows(IllegalArgumentException.class, () -> java.util.logging.Level.parse("LOUD"));
        assertThrows(IllegalArgumentException.class, () -> Level.parse("LOUD"));
        Logger quiet = Logger.getAnonymousLogger();
        java.util.logging.Logger theirs = java.util.logging.Logger.getAnonymousLogger();
        assertEquals(theirs.isLoggable(java.util.logging.Level.INFO), quiet.isLoggable(Level.INFO));
        assertEquals(theirs.isLoggable(java.util.logging.Level.FINE), quiet.isLoggable(Level.FINE));
        quiet.setLevel(Level.OFF);
        assertFalse(quiet.isLoggable(Level.SEVERE));
        assertSame(Logger.getLogger("a.b"), Logger.getLogger("a.b"));
        assertEquals(java.util.logging.Logger.GLOBAL_LOGGER_NAME, Logger.getGlobal().getName());
    }

    @Test
    void writesThroughTheHostLog() {
        final List<String> lines = new ArrayList<String>();
        final List<Throwable> thrown = new ArrayList<Throwable>();
        JdkHost.install(new JdkHost() {
            @Override
            public void log(int level, String message, Throwable t) {
                lines.add(level + " " + message);
                if (t != null) {
                    thrown.add(t);
                }
            }
        });
        Logger log = Logger.getLogger("parity");
        log.severe("s");
        log.warning("w");
        log.info("i");
        log.config("c");
        log.fine("f");
        log.setLevel(Level.ALL);
        log.config("c");
        log.finest("f");
        log.log(Level.INFO, "{0} and {1}, {2} and {x}", new Object[] {"a", Integer.valueOf(2)});
        log.log(Level.WARNING, "q", "one");
        log.log(Level.WARNING, () -> "supplied");
        RuntimeException failure = new RuntimeException("boom");
        log.log(Level.SEVERE, "failed", failure);
        assertEquals(java.util.Arrays.asList(
                JdkHost.LOG_ERROR + " parity: s",
                JdkHost.LOG_WARNING + " parity: w",
                JdkHost.LOG_INFO + " parity: i",
                JdkHost.LOG_INFO + " parity: c",
                JdkHost.LOG_DEBUG + " parity: f",
                JdkHost.LOG_INFO + " parity: a and 2, {2} and {x}",
                JdkHost.LOG_WARNING + " parity: q",
                JdkHost.LOG_WARNING + " parity: supplied",
                JdkHost.LOG_ERROR + " parity: failed"), lines);
        assertEquals(1, thrown.size());
        assertSame(failure, thrown.get(0));
        assertTrue(java.util.logging.Logger.getLogger("parity").isLoggable(java.util.logging.Level.INFO));
    }
}
