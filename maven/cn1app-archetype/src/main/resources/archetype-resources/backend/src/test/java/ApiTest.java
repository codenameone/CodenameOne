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
package ${package};

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcResultMatchers.content;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;

/**
 * Tests for Api, in the shape of Spring Boot's: the annotations and the MockMvc
 * calls are Spring's, under Codename One's package names.
 *
 * &#64;BackendTest starts this module's server, wired exactly as it ships, and
 * injects what the test asks for. MockMvc dispatches a request in process,
 * without a socket; TestRestTemplate (with a RANDOM_PORT web environment) sends
 * it over a real port instead, as ServedApiTest does. Run them with
 *
 *     mvn -pl backend -Dcodename1.platform=backend test
 *
 * and add -Dcn1.backend.compiledTests=true to also translate the same tests into
 * a native test binary and run that, which is how the server ships.
 */
@BackendTest
class ApiTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void greetsThroughTheInjectedService() throws Exception {
        mvc.perform(get("/greet/{name}", "Ada"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, Ada"));
    }

    @Test
    void echoAnswersJson() throws Exception {
        mvc.perform(get("/echo").param("say", "hi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.say").value("hi"));
        mvc.perform(get("/echo")).andExpect(jsonPath("$.say").value("hello"));
    }
}
