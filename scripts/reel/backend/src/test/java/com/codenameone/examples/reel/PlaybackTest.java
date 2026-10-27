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
import com.codenameone.examples.reel.account.Accounts;
import com.codenameone.examples.reel.domain.VideoKey;
import com.codenameone.examples.reel.playback.Licenses;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Starting a stream: entitlement, the plan's screen limit, and the keys --
/// which are handed out only while the session that asked for them is live.
@BackendTest
class PlaybackTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Licenses keys;

    private Map start(String user, String title, String device) throws Exception {
        return start(user, title, device, "AES_128\",\"CLEARKEY");
    }

    private Map start(String user, String title, String device, String schemes) throws Exception {
        String body = mvc.perform(People.asViewer(post("/api/playback/start"), user).contentType("application/json")
                        .content("{\"titleId\":\"" + title + "\",\"deviceId\":\"" + device
                                + "\",\"schemes\":[\"" + schemes + "\"]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return Json.parseObject(body);
    }

    /// `/media/{token}/{video}/master.m3u8` -> `/media/{token}/{video}/`
    private static String base(Map playback) {
        String url = (String) playback.get("manifestUrl");
        return url.substring(0, url.lastIndexOf('/') + 1);
    }

    @Test
    void aFreeTitlePlaysWithoutAPlanAndAPaidOneAsksForOne() throws Exception {
        String user = People.viewer(accounts, "freeloader");
        Map p = start(user, "big-buck-bunny", "");
        assertEquals("AES_128", p.get("scheme"));
        assertTrue(((String) p.get("manifestUrl")).startsWith("/media/"));
        // No plan, so the free title carries ads.
        assertTrue(!((List) p.get("adBreaks")).isEmpty());
        mvc.perform(People.asViewer(post("/api/playback/start"), user).contentType("application/json")
                        .content("{\"titleId\":\"sintel\",\"schemes\":[\"AES_128\"]}"))
                .andExpect(status().is(402));
        mvc.perform(People.asViewer(post("/api/playback/start"), user).contentType("application/json")
                        .content("{\"titleId\":\"tears-of-steel\",\"schemes\":[\"AES_128\"]}"))
                .andExpect(status().is(402));
    }

    @Test
    void theKeyIsServedOnlyWhileTheSessionIsLive() throws Exception {
        String user = People.viewer(accounts, "keyholder");
        Map p = start(user, "big-buck-bunny", "");
        VideoKey key = keys.key("big-buck-bunny");
        byte[] served = mvc.perform(get(base(p) + "key")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertArrayEquals(Ids.unhex(key.getKeyHex()), served);
        // The token is for this video only.
        String other = base(p).replace("/big-buck-bunny/", "/sintel/");
        mvc.perform(get(other + "key")).andExpect(status().isForbidden());

        mvc.perform(People.asViewer(post("/api/playback/stop"), user).contentType("application/json")
                        .content("{\"sessionId\":\"" + p.get("sessionId") + "\",\"positionSeconds\":42}"))
                .andExpect(status().isOk());
        mvc.perform(get(base(p) + "key")).andExpect(status().isForbidden());
        // And the next start resumes where the viewer stopped.
        assertEquals(42L, Rows.number(start(user, "big-buck-bunny", "").get("resumeSeconds")));
    }

    @Test
    void clearKeyAnswersForTheSessionsOwnVideo() throws Exception {
        String user = People.viewer(accounts, "clearkey");
        Map p = start(user, "big-buck-bunny", "", "CLEARKEY");
        assertEquals("CLEARKEY", p.get("scheme"));
        VideoKey key = keys.key("big-buck-bunny");
        String kid = Base64.getUrlEncoder().withoutPadding().encodeToString(Ids.unhex(key.getKeyId()));
        String answer = mvc.perform(post("/license/clearkey").header("X-Reel-License", (String) p.get("licenseToken"))
                        .contentType("application/json").content("{\"kids\":[\"" + kid + "\"],\"type\":\"temporary\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Map k = (Map) ((List) Json.parseObject(answer).get("keys")).get(0);
        assertEquals(kid, k.get("kid"));
        assertArrayEquals(Ids.unhex(key.getKeyHex()), Base64.getUrlDecoder().decode((String) k.get("k")));
        mvc.perform(post("/license/clearkey").header("X-Reel-License", "forged")
                        .contentType("application/json").content("{\"kids\":[\"" + kid + "\"]}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void thePlanLimitsHowManyScreensPlayAtOnce() throws Exception {
        String user = People.viewer(accounts, "oneScreen");
        People.subscribe(mvc, user, "basic");
        start(user, "sintel", "phone");
        mvc.perform(People.asViewer(post("/api/playback/start"), user).contentType("application/json")
                        .content("{\"titleId\":\"sintel\",\"deviceId\":\"tv\",\"schemes\":[\"AES_128\"]}"))
                .andExpect(status().is(409));
        // The same device starting again replaces its own stream.
        start(user, "elephants-dream", "phone");
    }
}
