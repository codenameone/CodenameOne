/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
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
package com.codenameone.examples.fieldline;

import com.codename1.backend.Json;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcResultMatchers.status;

/// The plumbing of a request: a JSON body on the way in, and the object, the
/// list or the status on the way out.
final class Http {
    private Http() {
    }

    static MockRequestBuilder body(MockRequestBuilder request, String json) {
        return request.contentType("application/json").content(json);
    }

    /// The object a request that succeeds answers with.
    static Map json(MockMvc mvc, MockRequestBuilder request) throws Exception {
        return Json.parseObject(mvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    /// The list a request that succeeds answers with.
    static List list(MockMvc mvc, MockRequestBuilder request) throws Exception {
        return (List) Json.parse(mvc.perform(request).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    /// The status a request is answered with, whatever it is.
    static int statusOf(MockMvc mvc, MockRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    static long number(Map object, String field) {
        return ((Number) object.get(field)).longValue();
    }
}
