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
package com.codenameone.examples.restock;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The app boots as its own server and refuses what Bazaar did not sign.
@BackendTest
class RestockServerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private RestockAlerts app;

    @Test
    void forgedCallsAreRefusedAndTheInstallStartsAtBazaar() throws Exception {
        app.useCredentials("bzc_restock", "bzs_server-test-secret");
        MockResponse r = mvc.perform(post("/restock/webhooks").header("X-Bazaar-Topic",
                "app/uninstalled").header("X-Bazaar-Signature", "sha256=00")
                .contentType("application/json").content("{\"storeId\":\"s\"}"))
                .andReturn().getResponse();
        assertEquals(401, r.getStatus());
        assertEquals(401, mvc.perform(post("/restock/blocks/data")
                .contentType("application/json").content("{}")).andReturn().getResponse()
                .getStatus());
        assertEquals(401, mvc.perform(get("/restock/proxy/s?shop=x&timestamp=1&signature=0"))
                .andReturn().getResponse().getStatus());
        assertEquals(401, mvc.perform(get("/restock/admin?session=a.b.c")).andReturn()
                .getResponse().getStatus());
        r = mvc.perform(get("/restock/install?store=s1")).andReturn().getResponse();
        assertEquals(302, r.getStatus());
        String to = r.getHeader("Location");
        assertTrue(to.startsWith("https://bazaar.example/account/apps/authorize?"), to);
        assertTrue(to.contains("code_challenge_method=S256"), to);
    }
}
