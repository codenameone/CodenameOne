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

import com.codename1.impl.JdkHost;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/// java.util.logging: levels parse and compare alike, and on Codename One a
/// logger writes through the host's log at the folded level.
public final class LoggingChecks {
    private LoggingChecks() {
    }

    /// Runs the group.
    public static void run() {
        Bulk.check("Logging.levelsParseAndCompareAsTheJdksDo", () -> {
            for (Level level : new Level[] {Level.OFF, Level.SEVERE, Level.WARNING, Level.INFO, Level.CONFIG,
                Level.FINE, Level.FINER, Level.FINEST, Level.ALL}) {
                Level parsed = Level.parse(level.getName());
                String k = "[" + level.getName() + "]";
                Bulk.p("name" + k, parsed.getName());
                Bulk.p("intValue" + k, parsed.intValue());
                Bulk.p("sameAsParsedInt" + k, parsed == Level.parse(String.valueOf(level.intValue())));
                Bulk.p("toString" + k, parsed.toString());
            }
            Level custom = new Level("AUDIT", 850) {
                private static final long serialVersionUID = 1L;
            };
            Bulk.p("customName", custom.getName());
            Bulk.p("customBundle", custom.getResourceBundleName());
            Bulk.p("parse123", Level.parse("123").getName());
            Bulk.pThrown("parseLoud", IllegalArgumentException.class, () -> Level.parse("LOUD"));
            Logger quiet = Logger.getAnonymousLogger();
            Bulk.p("anonymousInfo", quiet.isLoggable(Level.INFO));
            Bulk.p("anonymousFine", quiet.isLoggable(Level.FINE));
            quiet.setLevel(Level.OFF);
            Bulk.p("offSevere", quiet.isLoggable(Level.SEVERE));
            Bulk.p("sameLogger", Logger.getLogger("a.b") == Logger.getLogger("a.b"));
            Bulk.p("globalName", Logger.getGlobal().getName());
        });
        // A host the check installs receives every record, at the level the
        // host's four levels fold the JDK's into.
        Bulk.cn1Only("Logging.writesThroughTheHostLog", () -> {
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
            Bulk.expectEquals(Arrays.asList(
                    JdkHost.LOG_ERROR + " parity: s",
                    JdkHost.LOG_WARNING + " parity: w",
                    JdkHost.LOG_INFO + " parity: i",
                    JdkHost.LOG_INFO + " parity: c",
                    JdkHost.LOG_DEBUG + " parity: f",
                    JdkHost.LOG_INFO + " parity: a and 2, {2} and {x}",
                    JdkHost.LOG_WARNING + " parity: q",
                    JdkHost.LOG_WARNING + " parity: supplied",
                    JdkHost.LOG_ERROR + " parity: failed"), lines, "the host's log");
            Bulk.expectEquals(Integer.valueOf(1), Integer.valueOf(thrown.size()), "one throwable logged");
            Bulk.expect(thrown.size() == 1 && thrown.get(0) == failure, "the throwable logged is the one given");
        });
    }
}
