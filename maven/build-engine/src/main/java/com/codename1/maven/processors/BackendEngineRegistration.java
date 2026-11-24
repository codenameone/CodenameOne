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
package com.codename1.maven.processors;

import com.codename1.maven.annotations.ProcessorContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/// The server database engines a backend registers before it opens anything.
///
/// `Database.open` serves only an engine registered with `Database.useEngine`,
/// so that a server carries the wire clients it can reach and no others: each
/// one is a class only that registration references, and the native build
/// strips the rest. The engines are the ones the build can see the module
/// name -- `cn1.datasource.url` and `cn1.tenancy.datasource.url` in
/// `application.properties` and every `application-<profile>.properties`, and
/// a `cn1.datasource.url` given to the build itself (the Maven or Gradle
/// property, the system property, or `CN1_DATASOURCE_URL`). A URL decided only
/// at run time is not seen; the application registers that engine itself.
final class BackendEngineRegistration {
    /// The `ProcessorContext` attribute a build tool sets to the
    /// `cn1.datasource.url` it was given, if any.
    static final String OVERRIDE_ATTRIBUTE = "cn1.datasource.url";

    /// The settings that name a database URL.
    private static final String[] KEYS = {"cn1.datasource.url", "cn1.tenancy.datasource.url"};

    /// Engine name and connector class, in the order the lines are written.
    private static final String[][] ENGINES = {
        {"postgres", "com.codename1.db.sql.Postgres"},
        {"mysql", "com.codename1.db.sql.MySql"},
        {"sqlserver", "com.codename1.db.sql.SqlServer"},
        {"oracle", "com.codename1.db.sql.Oracle"},
    };

    private BackendEngineRegistration() {
    }

    /// The connector classes this module's backend registers.
    static List<String> connectors(ProcessorContext ctx) {
        List<String> urls = new ArrayList<String>();
        if (ctx != null) {
            for (String key : KEYS) {
                urls.addAll(RestControllerAnnotationProcessor.applicationPropertyValues(ctx, key));
            }
            Object override = ctx.getAttribute(OVERRIDE_ATTRIBUTE);
            if (override instanceof String) {
                urls.add((String) override);
            }
        }
        urls.add(System.getProperty("cn1.datasource.url"));
        urls.add(System.getenv("CN1_DATASOURCE_URL"));
        return connectors(urls);
    }

    /// The connector classes for `urls`, without duplicates, in a fixed order.
    static List<String> connectors(Collection<String> urls) {
        boolean[] wanted = new boolean[ENGINES.length];
        for (String url : urls) {
            String engine = named(url);
            for (int i = 0; i < ENGINES.length; i++) {
                if (ENGINES[i][0].equals(engine)) {
                    wanted[i] = true;
                }
            }
        }
        List<String> out = new ArrayList<String>();
        for (int i = 0; i < ENGINES.length; i++) {
            if (wanted[i]) {
                out.add(ENGINES[i][1]);
            }
        }
        return out;
    }

    /// The engine a URL names, by the scheme rules of
    /// `com.codename1.db.sql.Engines.named`, or null. A `${...}` reference that
    /// does not begin the value leaves the scheme literal and readable; one that
    /// does hides it, and the value names nothing.
    static String named(String url) {
        if (url == null) {
            return null;
        }
        String u = url.trim();
        // ${DATABASE_URL:postgres://localhost/app} -- the default names the
        // engine the deployment is expected to supply.
        if (u.startsWith("${")) {
            int colon = u.indexOf(':');
            int close = u.lastIndexOf('}');
            if (colon < 0 || close < colon) {
                return null;
            }
            u = u.substring(colon + 1, close).trim();
        }
        if (scheme(u, "postgres://") || scheme(u, "postgresql://")) {
            return "postgres";
        }
        if (scheme(u, "mysql://") || scheme(u, "mariadb://")) {
            return "mysql";
        }
        if (scheme(u, "sqlserver://") || scheme(u, "mssql://")) {
            return "sqlserver";
        }
        if (scheme(u, "oracle://")) {
            return "oracle";
        }
        return null;
    }

    private static boolean scheme(String url, String scheme) {
        return url.regionMatches(true, 0, scheme, 0, scheme.length());
    }
}
