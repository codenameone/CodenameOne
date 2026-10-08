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

import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.RequestMapping;
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.security.Authentication;
import com.codename1.backend.security.GrantedAuthority;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Who is signed in to the sign-in chain: where a form login and a second factor
/// land, so a client that does not render pages can tell a completed sign-in (this
/// answers 200 with the user) from one still waiting for its code (401).
@RestController
@RequestMapping("/account")
public class AccountApi {
    @GetMapping("/me")
    public Map<String, Object> me(Authentication who) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("name", who.getName());
        List<Object> granted = new ArrayList<Object>();
        for (GrantedAuthority authority : who.getAuthorities()) {
            granted.add(authority.getAuthority());
        }
        out.put("authorities", granted);
        return out;
    }
}
