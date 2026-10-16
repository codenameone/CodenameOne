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
import com.codename1.backend.test.HttpStatus;
import com.codename1.backend.test.TestRestTemplate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The same server on a real port, reached through the backend's own HTTP client:
 * the request goes through the socket, the parser and the response writer that a
 * deployed server uses. The port is chosen at random, so a server already running
 * on 8080 does not get in the way.
 */
@BackendTest(webEnvironment = BackendTest.WebEnvironment.RANDOM_PORT)
class ServedApiTest {
    @Autowired
    private TestRestTemplate rest;

    @Test
    void answersOverTheNetwork() throws Exception {
        assertEquals("ok", rest.getForObject("/healthz", String.class));
        assertEquals("Hello, Ada", rest.getForObject("/greet/{name}", String.class, "Ada"));
        assertEquals(HttpStatus.NOT_FOUND, rest.getForEntity("/nowhere", String.class).getStatusCode());
    }
}
