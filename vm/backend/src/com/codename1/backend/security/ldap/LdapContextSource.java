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
package com.codename1.backend.security.ldap;

import com.codename1.backend.Config;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Where the directory is and how to reach it: the server URLs, tried in
/// order, the base every name is under, the account searches run as, and the
/// TLS settings. Spring's `LdapContextSource`, from the same settings Spring
/// Boot reads (`spring.ldap.*` there, `cn1.ldap.*` here):
///
/// ```
/// cn1.ldap.urls=ldaps://ldap1.corp.example,ldaps://ldap2.corp.example
/// cn1.ldap.base=dc=corp,dc=example
/// cn1.ldap.username=cn=reader,dc=corp,dc=example
/// cn1.ldap.password=${LDAP_READER_PASSWORD}
/// ```
///
/// plus `cn1.ldap.starttls` (upgrade `ldap://` URLs), `cn1.ldap.ca-file` (PEM
/// of the authorities to trust), `cn1.ldap.connect-timeout-millis` and
/// `cn1.ldap.read-timeout-millis` (both 10 seconds by default).
///
/// Connections are not pooled: each sign-in opens its own and closes it, so a
/// directory that is down costs one connect timeout per attempt and nothing
/// stays open between sign-ins.
public final class LdapContextSource {
    public static final String PREFIX = "cn1.ldap.";

    private final List<String> urls = new ArrayList<String>();
    private String base = "";
    private String userDn = "";
    private String password = "";
    private boolean startTls;
    private String caFile;
    private int connectTimeoutMillis = 10000;
    private int readTimeoutMillis = 10000;

    /// @param urls `ldap://host[:port][/base]` or `ldaps://...`; the base of
    /// the first is used when [#setBase] is not called
    public LdapContextSource(String... urls) {
        for (String url : urls) {
            String u = url.trim();
            if (u.length() > 0) {
                this.urls.add(u);
            }
        }
        if (this.urls.isEmpty()) {
            throw new IllegalArgumentException("An LDAP context source needs a URL");
        }
        try {
            base = LdapConnection.LdapUrl.parse(this.urls.get(0)).base;
        } catch (IOException err) {
            throw new IllegalArgumentException(err.getMessage(), err);
        }
    }

    /// The context source the `cn1.ldap.*` settings describe, or null when
    /// `cn1.ldap.urls` is not set.
    public static LdapContextSource fromConfig(Config config) throws IOException {
        String urls = config.get(PREFIX + "urls");
        if (urls == null || urls.trim().length() == 0) {
            return null;
        }
        LdapContextSource s = new LdapContextSource(LdapEncoder.split(urls, ','));
        String base = config.get(PREFIX + "base");
        if (base != null) {
            s.setBase(base);
        }
        s.setUserDn(config.get(PREFIX + "username", ""));
        s.setPassword(config.get(PREFIX + "password", ""));
        s.setStartTls(config.getBoolean(PREFIX + "starttls", false));
        s.setCaFile(config.get(PREFIX + "ca-file"));
        s.setConnectTimeoutMillis(config.getInt(PREFIX + "connect-timeout-millis", 10000));
        s.setReadTimeoutMillis(config.getInt(PREFIX + "read-timeout-millis", 10000));
        return s;
    }

    public List<String> getUrls() {
        return Collections.unmodifiableList(urls);
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base == null ? "" : base.trim();
    }

    /// The account searches run as; empty for anonymous searches.
    public void setUserDn(String userDn) {
        this.userDn = userDn == null ? "" : userDn;
    }

    public void setPassword(String password) {
        this.password = password == null ? "" : password;
    }

    public void setStartTls(boolean startTls) {
        this.startTls = startTls;
    }

    public void setCaFile(String caFile) {
        this.caFile = caFile;
    }

    public void setConnectTimeoutMillis(int millis) {
        this.connectTimeoutMillis = millis;
    }

    public void setReadTimeoutMillis(int millis) {
        this.readTimeoutMillis = millis;
    }

    /// `name` under the base: `uid=ada,ou=people` becomes
    /// `uid=ada,ou=people,dc=corp,dc=example`.
    public String absolute(String name) {
        if (base.length() == 0) {
            return name;
        }
        if (name.length() == 0) {
            return base;
        }
        return name + "," + base;
    }

    /// A connection to the first server that answers, not bound.
    public LdapConnection connect() throws IOException {
        IOException last = null;
        for (String url : urls) {
            try {
                return LdapConnection.open(url, startTls, caFile, connectTimeoutMillis,
                        readTimeoutMillis);
            } catch (LdapException refused) {
                throw refused;
            } catch (IOException unreachable) {
                last = unreachable;
            }
        }
        throw last;
    }

    /// A connection bound as the search account, or anonymous when none is set.
    public LdapConnection readOnly() throws IOException {
        LdapConnection c = connect();
        try {
            if (userDn.length() > 0) {
                c.bind(userDn, password);
            }
            return c;
        } catch (IOException err) {
            c.close();
            throw err;
        }
    }
}
