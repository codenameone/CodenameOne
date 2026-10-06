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

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.annotations.Bean;
import com.codename1.backend.annotations.Primary;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.TestConfiguration;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A test bean replacing the application's, with no mocking library: a
/// `@Primary` bean of the same type wins every injection. Runs on the JVM and
/// compiled alike.
@BackendTest
class FakeStoreTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private PetStore store;

    @TestConfiguration
    static class Fakes {
        @Bean
        @Primary
        public PetStore cannedStore() {
            PetStore canned = new PetStore();
            canned.add(new Pet("Canned", "fish", 1));
            return canned;
        }
    }

    @Test
    void theControllerGetsTheTestsStore() throws Exception {
        mvc.perform(get("/api/pets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Canned"));
        assertTrue(store.get(1) != null, "the test was injected another store");
    }
}
