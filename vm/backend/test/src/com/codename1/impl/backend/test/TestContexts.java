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
package com.codename1.impl.backend.test;

import com.codename1.backend.Backend;
import com.codename1.backend.Config;
import com.codename1.impl.backend.BackendAccess;
import java.util.Properties;

/// The one running test application, started for a context and kept for the
/// next class whose context has the same key -- the context cache Spring keeps,
/// with the size this runtime allows: one backend runs per process.
public final class TestContexts {
    /// BackendTest.WebEnvironment.DEFINED_PORT's ordinal.
    private static final int DEFINED_PORT = 2;

    private static String currentKey;
    private static TestEnvironment current;

    private TestContexts() {
    }

    /// The running application for `context`, starting it -- and stopping one
    /// started for another key -- when it is not already running.
    public static synchronized TestEnvironment acquire(TestContext context) throws Exception {
        if (current != null && context.key().equals(currentKey)) {
            return current;
        }
        shutdown();
        Properties settings = new Properties();
        String[] pairs = context.properties();
        for (int iter = 0 ; iter + 1 < pairs.length ; iter += 2) {
            settings.setProperty(pairs[iter], pairs[iter + 1]);
        }
        String[] profileSettings = context.profileSettings();
        if (Config.of(null, context.profile()).isDevelopmentProfile()
                && settings.getProperty(Config.DATASOURCE_URL) == null
                && !names(profileSettings, Config.DATASOURCE_URL)) {
            // A committed application.properties names the production database --
            // cn1.datasource.url=${DATABASE_URL} -- and is compiled in under this.
            // A test on a development profile gets an in-memory one instead, unless
            // the test or the profile's file names one, as the dev profile does.
            // In the top layer, so a DATABASE_URL exported in the developer's shell
            // does not point the tests at that database.
            settings.setProperty(Config.DATASOURCE_URL, ":memory:");
        }
        boolean defined = context.webEnvironment() == DEFINED_PORT;
        // MOCK and NONE listen too, on a free loopback port. Spring starts no web
        // server for NONE, but here the HttpServer is also what hosts the
        // application's executors and virtual threads (Tasks.Registry.server), so
        // an application with no server would run its tasks differently from the
        // one it tests. A loopback port nobody connects to is the cheaper price.
        if (!defined && settings.getProperty(Config.SERVER_PORT) == null) {
            // A free port the system picks, read back from the listener.
            settings.setProperty(Config.SERVER_PORT, "0");
        }
        BackendAccess access = BackendAccess.get();
        Config config = access.testConfig(settings, context.profile());
        Backend.Builder builder = Backend.builder(config).quiet();
        if (!defined) {
            builder.host("127.0.0.1"); //NOPMD AvoidUsingHardCodedIP - a test server listens on loopback only
        }
        if (context.requiresDataSource()) {
            builder.requiresDataSource();
        }
        access.compiledSettings(builder, concat(context.compiledSettings(), profileSettings));
        context.prepare();
        access.application(builder, context.createApplication());
        Backend backend = builder.start();
        current = new TestEnvironment(context, backend);
        currentKey = context.key();
        return current;
    }

    /// Whether `pairs`, keys and values alternating, sets `key`.
    private static boolean names(String[] pairs, String key) {
        for (int iter = 0 ; iter + 1 < pairs.length ; iter += 2) {
            if (key.equals(pairs[iter])) {
                return true;
            }
        }
        return false;
    }

    /// The base file's pairs, then the profile file's: later pairs win in the
    /// compiled layer, as the profile file wins over the base file.
    private static String[] concat(String[] base, String[] profile) {
        String[] out = new String[base.length + profile.length];
        System.arraycopy(base, 0, out, 0, base.length);
        System.arraycopy(profile, 0, out, base.length, profile.length);
        return out;
    }

    /// Stops the running application, if any. The JUnit extension calls this from a
    /// shutdown hook; the compiled runner calls it before it exits.
    public static synchronized void shutdown() {
        if (current != null) {
            TestEnvironment stopping = current;
            current = null;
            currentKey = null;
            stopping.backend().stop();
        }
    }
}
