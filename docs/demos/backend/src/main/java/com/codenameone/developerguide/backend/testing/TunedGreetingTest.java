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

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static org.junit.jupiter.api.Assertions.assertTrue;

// tag::backend-test-properties[]
@BackendTest(properties = {"app.greeting=Hi", "cn1.session.timeout=5"})
// end::backend-test-properties[]
class TunedGreetingTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void readsTheBodyItself() throws Exception {
        // tag::backend-test-and-return[]
        String body = mvc.perform(get("/greet/{name}", "Ada"))
                .andReturn().getResponse().getContentAsString();
        // end::backend-test-and-return[]
        assertTrue(body.contains("Hello, Ada"), body);
    }
}
