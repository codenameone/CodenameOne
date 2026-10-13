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
package com.codenameone.examples.wayline.account;

import com.codename1.backend.DataSource;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.SecurityContextHolder;
import com.codename1.backend.security.oauth2.server.authorization.OAuth2AuthorizationService;
import com.codename1.backend.security.oauth2.server.resource.JwtAuthenticationToken;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/// The sign-ins an account has open, and ending them.
///
/// Signing in leaves a grant in the authorization server's own table, and a
/// refresh token is good for as long as its grant is there: refreshing asks
/// for no password. So a password changed because a phone was lost changes
/// nothing for whoever holds that phone, unless the grants go too.
///
/// An access token already issued is not recalled by this. It is a signed
/// token that is checked without the table, and it runs out on its own within
/// its few minutes; what ends here is the means of getting another.
@Component
public class Grants {
    private final DataSource db;
    private final OAuth2AuthorizationService authorizations;

    public Grants(DataSource db, OAuth2AuthorizationService authorizations) {
        this.db = db;
        this.authorizations = authorizations;
    }

    /// Ends every sign-in of an account but the one this request was made
    /// with, so the device a password was changed on stays signed in and every
    /// other has to give the new password.
    public void endOthers(String username) throws IOException {
        end(username, current());
    }

    /// Ends every sign-in of an account.
    public void endAll(String username) throws IOException {
        end(username, "");
    }

    private void end(String username, String kept) throws IOException {
        // The table is the authorization server's, which offers a grant by its
        // id and no list of an account's: this reads the ids and leaves the
        // removing, which takes the grant's tokens with it, to the service.
        List rows = db.query("SELECT id FROM cn1_oauth2_authorization WHERE principal_name = ?",
                new Object[] {username});
        for (int iter = 0; rows != null && iter < rows.size(); iter++) {
            Object row = rows.get(iter);
            Object id = row instanceof Map ? ((Map) row).get("id") : null;
            if (id != null && !kept.equals(String.valueOf(id))) {
                authorizations.remove(String.valueOf(id));
            }
        }
    }

    /// The id of the grant the request's token was issued under, or empty for
    /// a request that carried no such token. The server writes the grant's id
    /// before the first dot of the token's own.
    private static String current() {
        Authentication who = SecurityContextHolder.getContext().getAuthentication();
        if (!(who instanceof JwtAuthenticationToken)) {
            return "";
        }
        String id = ((JwtAuthenticationToken) who).getToken().getId();
        int dot = id == null ? -1 : id.indexOf('.');
        return dot > 0 ? id.substring(0, dot) : "";
    }
}
