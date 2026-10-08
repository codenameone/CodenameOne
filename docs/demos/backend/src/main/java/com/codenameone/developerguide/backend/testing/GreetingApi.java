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
package com.codenameone.developerguide.backend.testing;

import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** The controller the tests in this package exercise. */
// tag::backend-test-api[]
@RestController
public class GreetingApi {
    private final Greetings greetings;

    public GreetingApi(Greetings greetings) {
        this.greetings = greetings;
    }

    /** A JSON object holding the greeting. */
    @GetMapping("/greet/{name}")
    public Map<String, Object> greet(@PathVariable("name") String name) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("greeting", greetings.greet(name));
        return out;
    }
}
// end::backend-test-api[]
