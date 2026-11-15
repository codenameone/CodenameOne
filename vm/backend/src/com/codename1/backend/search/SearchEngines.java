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
package com.codename1.backend.search;

import com.codename1.backend.Config;
import com.codename1.backend.DataSource;
import com.codename1.impl.backend.BackendAccess;
import java.io.IOException;

/// Opens the [SearchEngine] a configuration names, and finds the one of the
/// calling thread's server.
///
/// | Setting | Default | |
/// |---|---|---|
/// | `cn1.search.url` | `database:` | see the package description; `SEARCH_URL` in the environment |
/// | `cn1.search.apiKey` | | an Elasticsearch API key, sent as `Authorization: ApiKey` |
///
/// Without `cn1.search.url`, Spring Boot's `spring.elasticsearch.uris` (the first
/// of them, with `spring.elasticsearch.username` and `.password`) is used, so a
/// Spring configuration carries over. `database:` searches the database
/// `cn1.datasource.url` names.
public final class SearchEngines {
    private static final String RESOURCE = "cn1.search";

    private SearchEngines() {
    }

    /// The engine of the calling thread's server: opened from its configuration
    /// the first time it is asked for, and closed when the server stops.
    public static SearchEngine current() throws IOException {
        return (SearchEngine) BackendAccess.get().serverResource(RESOURCE,
                new BackendAccess.ResourceFactory() {
                    @Override
                    public Object open(Config config) throws IOException {
                        return SearchEngines.open(config);
                    }
                });
    }

    /// Opens the engine `config` names; the caller closes it.
    public static SearchEngine open(Config config) throws IOException {
        String url = config.get("cn1.search.url");
        String apiKey = config.get("cn1.search.apiKey");
        if (url == null || url.trim().length() == 0) {
            String uris = config.get("spring.elasticsearch.uris");
            if (uris != null && uris.trim().length() > 0) {
                int comma = uris.indexOf(',');
                url = withUser((comma < 0 ? uris : uris.substring(0, comma)).trim(),
                        config.get("spring.elasticsearch.username"),
                        config.get("spring.elasticsearch.password"));
            }
        }
        if (url == null || url.trim().length() == 0 || url.trim().startsWith("database:")) {
            return new SqlSearchEngine(DataSource.fromConfig(config), true);
        }
        return open(url.trim(), apiKey);
    }

    /// Opens a search cluster by URL; the caller closes it.
    public static SearchEngine open(String url, String apiKey) throws IOException {
        if (url.regionMatches(true, 0, "http://", 0, 7)
                || url.regionMatches(true, 0, "https://", 0, 8)) {
            return ElasticsearchEngine.connect(url, apiKey);
        }
        boolean aoss = url.regionMatches(true, 0, "aoss://", 0, 7);
        if (aoss || url.regionMatches(true, 0, "aws://", 0, 6)) {
            String rest = url.substring(aoss ? 7 : 6);
            String region = null;
            int q = rest.indexOf("?region=");
            if (q >= 0) {
                region = rest.substring(q + 8);
                rest = rest.substring(0, q);
            }
            while (rest.endsWith("/")) {
                rest = rest.substring(0, rest.length() - 1);
            }
            if (rest.indexOf('/') >= 0 || rest.indexOf('@') >= 0 || rest.length() == 0) {
                throw new IOException("An aws:// search URL is the domain's host alone");
            }
            return ElasticsearchEngine.aws(rest, aoss ? "aoss" : "es", region);
        }
        throw new IOException("cn1.search.url \"" + url + "\" names no engine this server "
                + "knows; use database:, http(s)://, aws:// or aoss://");
    }

    private static String withUser(String url, String user, String password) {
        if (user == null || user.length() == 0) {
            return url;
        }
        int scheme = url.indexOf("://");
        if (scheme < 0) {
            return url;
        }
        return url.substring(0, scheme + 3) + escape(user) + ":"
                + escape(password == null ? "" : password) + "@" + url.substring(scheme + 3);
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' || c == '@' || c == ':' || c == '/') {
                sb.append('%').append(Character.forDigit((c >> 4) & 15, 16))
                  .append(Character.forDigit(c & 15, 16));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
