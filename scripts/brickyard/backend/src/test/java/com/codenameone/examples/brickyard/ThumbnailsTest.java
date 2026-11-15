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
package com.codenameone.examples.brickyard;

import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.Base64;
import com.codenameone.examples.brickyard.account.Accounts;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// Experience tile pictures: only the owner sets one, only a JPEG or PNG of
/// a sensible size, and every player reads it back.
@BackendTest
class ThumbnailsTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    @Test
    void theOwnerSetsATileEveryoneReads() throws Exception {
        String owner = Api.player(accounts, "tileowner");
        String player = Api.player(accounts, "tileplayer");
        String id = Api.experience(mvc, owner, "PUBLIC", "ALL");
        Api.publishSample(mvc, owner, id);
        assertEquals("", Api.json(mvc, Api.as(get("/api/experiences/" + id + "/thumbnail"), player)).get("data"),
                "no tile yet: the client draws one");

        byte[] jpeg = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, (byte) 0xe0, 0, 0x10, 'J', 'F', 'I', 'F'};
        String data = Base64.encode(jpeg);
        Api.json(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/thumbnail"),
                "{\"data\":\"" + data + "\"}"), owner));
        Map t = Api.json(mvc, Api.as(get("/api/experiences/" + id + "/thumbnail"), player));
        assertEquals(data, t.get("data"));

        assertEquals(403, Api.statusOf(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/thumbnail"),
                "{\"data\":\"" + data + "\"}"), player)), "only the owner sets the tile");
        String text = Base64.encode("<html>".getBytes("UTF-8"));
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/thumbnail"),
                "{\"data\":\"" + text + "\"}"), owner)), "only a JPEG or a PNG reaches players' decoders");
        byte[] huge = new byte[400 * 1024 + 1];
        System.arraycopy(jpeg, 0, huge, 0, jpeg.length);
        assertEquals(413, Api.statusOf(mvc, Api.as(Api.body(post("/api/experiences/" + id + "/thumbnail"),
                "{\"data\":\"" + Base64.encode(huge) + "\"}"), owner)));
    }
}
