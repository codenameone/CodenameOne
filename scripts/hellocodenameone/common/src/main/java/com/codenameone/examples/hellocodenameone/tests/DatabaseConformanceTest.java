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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.db.Database;
import com.codename1.testing.DatabaseConformanceSuite;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the portable {@code com.codename1.db} contract on the device.
 *
 * The framework ships the assertions in {@link DatabaseConformanceSuite} so that this test, the
 * simulator unit tests and anyone writing a new port all check the same thing. Subclasses pick
 * which group to run and whether to assert the current contract or the legacy compatibility mode.
 *
 * Ports without a database self-skip, so Windows and Linux stayed green before they had one and
 * turn green on their own once they do, with no edit here.
 */
public abstract class DatabaseConformanceTest extends BaseTest {

    /** Name shown in the CN1SS skip line. */
    protected abstract String testName();

    /** Which group of the suite to run. */
    protected abstract void runGroup(int mode, DatabaseConformanceSuite.Reporter reporter)
            throws Exception;

    /** Whether to assert the current contract or the previous per-platform behaviour. */
    protected int mode() {
        return DatabaseConformanceSuite.MODE_STRICT;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        // Assertions only; there is nothing to look at.
        return false;
    }

    @Override
    public boolean runTest() {
        final List<String> failures = new ArrayList<String>();
        final boolean[] skipped = {false};
        DatabaseConformanceSuite.Reporter reporter = new DatabaseConformanceSuite.Reporter() {
            public void check(boolean condition, String message) {
                if (!condition) {
                    failures.add(message);
                }
            }

            public void skip(String reason) {
                skipped[0] = true;
                System.out.println("CN1SS:INFO:test=" + testName()
                        + " status=SKIPPED reason=" + reason);
            }

            public void info(String message) {
                System.out.println("CN1SS:INFO:test=" + testName() + " note=" + message);
            }
        };

        boolean previousLegacy = Database.isLegacyBehavior();
        try {
            if (!DatabaseConformanceSuite.isDatabaseAvailable(reporter)) {
                done();
                return true;
            }
            Database.setLegacyBehavior(mode() == DatabaseConformanceSuite.MODE_LEGACY);
            runGroup(mode(), reporter);
        } catch (Throwable t) {
            fail(testName() + " threw " + t);
            return false;
        } finally {
            Database.setLegacyBehavior(previousLegacy);
        }

        if (!failures.isEmpty()) {
            StringBuilder b = new StringBuilder();
            b.append(testName()).append(": ").append(failures.size()).append(" failures");
            for (int iter = 0; iter < failures.size(); iter++) {
                b.append("\n  - ").append(failures.get(iter));
            }
            fail(b.toString());
            return false;
        }
        if (skipped[0]) {
            System.out.println("CN1SS:INFO:test=" + testName() + " note=partially skipped");
        }
        done();
        return true;
    }

    /** Opens a scratch database, runs the body, and closes and deletes it afterwards. */
    protected void withScratchDatabase(DatabaseBody body) throws Exception {
        String name = "cn1-conformance-" + testName() + ".db";
        if (Database.exists(name)) {
            Database.delete(name);
        }
        Database db = Database.openOrCreate(name);
        try {
            body.run(db);
        } finally {
            try {
                db.close();
            } catch (Throwable ignored) {
                // The assertions matter more than a cleanup failure.
            }
            try {
                if (Database.exists(name)) {
                    Database.delete(name);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /** Body of a test that needs an open database. */
    protected interface DatabaseBody {
        void run(Database db) throws Exception;
    }
}
