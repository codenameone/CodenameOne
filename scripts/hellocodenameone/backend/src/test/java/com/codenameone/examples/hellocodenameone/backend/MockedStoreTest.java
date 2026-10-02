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
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockitoBean;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// `@MockitoBean` replacing the store with a Mockito mock. JVM only: the compiled
/// run leaves this class out, because Mockito builds classes at run time.
@BackendTest
class MockedStoreTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PetStore store;

    @Test
    void theControllerCallsTheMock() throws Exception {
        Pet mocked = new Pet("Mocked", "owl", 9);
        mocked.id = 7;
        when(store.get(7)).thenReturn(mocked);
        mvc.perform(get("/api/pets/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.species").value("owl"));
        verify(store).get(7);
    }

    @Test
    void theMockIsResetBetweenTests() throws Exception {
        // Unstubbed after the test above: a mock answers null, and the route 404s.
        mvc.perform(get("/api/pets/7")).andExpect(status().isNotFound());
    }
}
