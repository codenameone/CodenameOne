/*
 * Copyright (c) 2012, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
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
