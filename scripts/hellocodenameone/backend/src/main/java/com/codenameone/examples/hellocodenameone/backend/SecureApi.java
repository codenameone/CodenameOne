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
package com.codenameone.examples.hellocodenameone.backend;

import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.DeleteMapping;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.RequestBody;
import com.codename1.backend.annotations.RequestMapping;
import com.codename1.backend.annotations.ResponseStatus;
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.GrantedAuthority;
import com.codename1.backend.security.oauth2.server.resource.JwtAuthenticationToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// What the app's sign-in tests call with the access token this server issued.
/// Every route is under `/api/secure`, which [SecurityConfig] guards with a
/// stateless resource-server chain: a bearer token on each request, no session.
///
/// | Route | Needs |
/// |---|---|
/// | `GET /api/secure/whoami` | any valid token |
/// | `GET /api/secure/notes` | scope `notes:read` |
/// | `POST /api/secure/notes` | scope `notes:write` |
/// | `DELETE /api/secure/notes/{id}` | scope `notes:write` |
/// | `GET /api/secure/admin` | scope `notes:write` |
/// | `GET /api/secure/summary/{owner}` | any valid token at the chain; `notes:read` and being `owner` at the method |
@RestController
@RequestMapping("/api/secure")
public class SecureApi {
    private final NoteService notes;

    public SecureApi(NoteService notes) {
        this.notes = notes;
    }

    /// Who the token says the caller is, and what it grants.
    @GetMapping("/whoami")
    public Map<String, Object> whoami(Authentication who) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("name", who.getName());
        List<Object> granted = new ArrayList<Object>();
        for (GrantedAuthority authority : who.getAuthorities()) {
            granted.add(authority.getAuthority());
        }
        out.put("authorities", granted);
        if (who instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken token = (JwtAuthenticationToken) who;
            out.put("issuer", token.getToken().getIssuer());
            out.put("scope", token.getToken().getClaimAsString("scope"));
        }
        return out;
    }

    /// The rows the migrations seeded, and any added since.
    @GetMapping("/notes")
    public List<Note> notes() throws IOException {
        return notes.all();
    }

    @PostMapping("/notes")
    @ResponseStatus(201)
    public Note add(@RequestBody Note note) throws IOException {
        return notes.add(note);
    }

    /// Takes back a note [#add] made, so a test leaves the three seeded rows as it
    /// found them: 204, or 404 for a seeded row or an id that is not there.
    @DeleteMapping("/notes/{id}")
    public HttpServer.Response remove(@PathVariable("id") long id) throws IOException {
        return notes.remove(id) ? HttpServer.Response.empty(204, null, null)
                : HttpServer.Response.text(404, "no removable note " + id);
    }

    /// Behind the scope the app's tests never ask for, so they can see a 403.
    @GetMapping("/admin")
    public Map<String, Object> admin(Authentication who) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("admin", who.getName());
        return out;
    }

    /// Guarded by [NoteService#summaryFor] rather than by a URL rule.
    @GetMapping("/summary/{owner}")
    public Map<String, Object> summary(@PathVariable("owner") String owner) throws IOException {
        return notes.summaryFor(owner);
    }
}
