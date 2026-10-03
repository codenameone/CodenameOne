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
import com.codename1.backend.test.MediaType;
import com.codename1.backend.test.MockMvc;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.multipart;
import static com.codename1.backend.test.MockMvcRequestBuilders.options;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static com.codename1.backend.test.MockMvcRequestBuilders.delete;
import static com.codename1.backend.test.MockMvcResultMatchers.content;
import static com.codename1.backend.test.MockMvcResultMatchers.cookie;
import static com.codename1.backend.test.MockMvcResultMatchers.header;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.redirectedUrl;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The typed API through MockMvc: in process, no socket.
@BackendTest
class PetApiTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void createReadUpdateDelete() throws Exception {
        String created = mvc.perform(post("/api/pets").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rex\",\"species\":\"dog\",\"age\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rex"))
                .andExpect(jsonPath("$.age").value(3))
                .andReturn().getResponse().getContentAsString();
        long id = idOf(created);

        mvc.perform(get("/api/pets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.species").value("dog"))
                .andExpect(jsonPath("$.vaccinated").value(false));
        mvc.perform(put("/api/pets/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Rex\",\"species\":\"dog\",\"age\":4,"
                                + "\"vaccinated\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.age").value(4));
        mvc.perform(get("/api/pets"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].vaccinated").value(true));
        mvc.perform(delete("/api/pets/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/pets/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void malformedJsonIsTheClientsError() throws Exception {
        mvc.perform(post("/api/pets").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aFailedExpectationSaysWhatItSaw() {
        // A lambda, through assertThrows: the compiled run translates both, so this
        // is also the check that the JUnit subset behaves the same in the binary.
        AssertionError failed = assertThrows(AssertionError.class,
                () -> mvc.perform(get("/api/pets/{id}", 999)).andExpect(status().isOk()));
        assertTrue(failed.getMessage().contains("404"), failed.getMessage());
    }

    /// The "id" of a JSON object, read without a parser or a regular expression,
    /// either of which a compiled test would have to bring along.
    private static long idOf(String json) {
        int at = json.indexOf("\"id\":") + 5;
        int end = at;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        return Long.parseLong(json.substring(at, end));
    }
}
