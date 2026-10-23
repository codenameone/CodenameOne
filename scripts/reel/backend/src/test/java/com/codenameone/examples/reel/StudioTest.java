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
package com.codenameone.examples.reel;

import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codenameone.examples.reel.account.Accounts;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A creator's title from first upload to the catalog: the upload in pieces,
/// the worker packaging it, the moderator's review.
@BackendTest
class StudioTest {
    private static final String WORKER = "Bearer test-worker-token";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    private Map json(MockRequestBuilder request, int expected) throws Exception {
        String body = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse()
                .getContentAsString();
        return body.startsWith("{") ? Json.parseObject(body) : null;
    }

    private static String sha256(byte[] data) throws Exception {
        return Ids.hex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    @Test
    void anUploadIsPackagedReviewedAndPublished() throws Exception {
        String creator = People.viewer(accounts, "director");
        String moderator = People.viewer(accounts, "reviewer");
        String viewer = People.viewer(accounts, "audience");

        Map title = json(People.asCreator(post("/api/studio/titles"), creator).contentType("application/json")
                .content("{\"kind\":\"MOVIE\",\"name\":\"Short Film\",\"synopsis\":\"A test.\",\"year\":2026,"
                        + "\"rating\":\"PG\",\"genres\":[\"comedy\"],\"tags\":[],\"access\":\"FREE\"}"), 200);
        String titleId = (String) title.get("id");
        Map video = json(People.asCreator(post("/api/studio/titles/" + titleId + "/videos"), creator)
                .contentType("application/json")
                .content("{\"role\":\"FEATURE\",\"number\":1,\"name\":\"Short Film\"}"), 200);
        String videoId = (String) video.get("id");
        // Nothing to review before there is video.
        mvc.perform(People.asCreator(post("/api/studio/titles/" + titleId + "/submit"), creator))
                .andExpect(status().is(409));

        byte[] source = "not really a movie, but bytes all the same".getBytes("UTF-8");
        Map upload = json(People.asCreator(post("/api/studio/uploads"), creator).contentType("application/json")
                .content("{\"videoId\":\"" + videoId + "\",\"fileName\":\"film.mp4\",\"totalBytes\":"
                        + source.length + ",\"sha256\":\"" + sha256(source) + "\"}"), 200);
        String uploadId = (String) upload.get("id");
        byte[] first = new byte[10];
        byte[] rest = new byte[source.length - 10];
        System.arraycopy(source, 0, first, 0, 10);
        System.arraycopy(source, 10, rest, 0, rest.length);
        mvc.perform(People.asCreator(put("/api/studio/uploads/" + uploadId + "?offset=0"), creator)
                .contentType("application/octet-stream").content(first)).andExpect(status().isOk());
        // A piece that does not continue the file is refused.
        mvc.perform(People.asCreator(put("/api/studio/uploads/" + uploadId + "?offset=3"), creator)
                .contentType("application/octet-stream").content(rest)).andExpect(status().is(409));
        mvc.perform(People.asCreator(put("/api/studio/uploads/" + uploadId + "?offset=10"), creator)
                .contentType("application/octet-stream").content(rest)).andExpect(status().isOk());

        // The worker API wants its own secret, not a user's token.
        mvc.perform(post("/worker/jobs/claim")).andExpect(status().isUnauthorized());
        Map job = json(post("/worker/jobs/claim").header("Authorization", WORKER), 200);
        assertEquals(videoId, job.get("videoId"));
        String jobId = (String) job.get("id");
        byte[] fetched = mvc.perform(get("/worker/source/" + videoId).header("Authorization", WORKER))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals(source, fetched);

        String files = "/worker/jobs/" + jobId + "/files/";
        mvc.perform(put(files + "..%2Fescape?offset=0").header("Authorization", WORKER)
                .contentType("application/octet-stream").content(first)).andExpect(status().is(400));
        // Not everything has arrived yet.
        mvc.perform(post("/worker/jobs/" + jobId + "/done").header("Authorization", WORKER)
                .contentType("application/json").content("{}")).andExpect(status().is(409));
        for (String name : new String[] {"master.m3u8", "v360.m3u8", "v360_00001.ts", "cenc.mp4", "poster.jpg"}) {
            mvc.perform(put(files + name + "?offset=0").header("Authorization", WORKER)
                    .contentType("application/octet-stream").content(first)).andExpect(status().isOk());
        }
        mvc.perform(post("/worker/jobs/" + jobId + "/done").header("Authorization", WORKER)
                .contentType("application/json").content("{\"durationSeconds\":42,\"width\":640,\"height\":360,"
                        + "\"renditions\":[360],\"frameHashes\":[\"0123456789abcdef\"]}"))
                .andExpect(status().isOk());

        Map packaged = json(People.asCreator(get("/api/studio/titles/" + titleId), creator), 200);
        Map first0 = (Map) ((List) packaged.get("videos")).get(0);
        assertEquals("READY", first0.get("status"));
        assertEquals(42L, Rows.number(first0.get("durationSeconds")));

        // A creator's title is reviewed before anyone sees it.
        mvc.perform(People.asCreator(post("/api/studio/titles/" + titleId + "/submit"), creator))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_REVIEW"));
        mvc.perform(People.asViewer(get("/api/titles/" + titleId), viewer)).andExpect(status().isNotFound());
        String queue = mvc.perform(People.asModerator(get("/api/moderation/queue"), moderator))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String itemId = null;
        List items = (List) Json.parse(queue);
        for (Object o : items) {
            Map item = (Map) o;
            if (titleId.equals(item.get("subjectId"))) {
                itemId = (String) item.get("id");
            }
        }
        assertTrue(itemId != null, queue);
        mvc.perform(People.asModerator(post("/api/moderation/items/" + itemId), moderator)
                .contentType("application/json").content("{\"action\":\"APPROVE\",\"note\":\"fine\"}"))
                .andExpect(status().isOk());
        mvc.perform(People.asViewer(get("/api/titles/" + titleId), viewer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Short Film"));
    }
}
