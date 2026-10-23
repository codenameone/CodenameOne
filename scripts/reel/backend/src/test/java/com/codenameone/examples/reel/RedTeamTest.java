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
import com.codenameone.examples.reel.playback.Licenses;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// The holes the first red-team round found, each held shut.
@BackendTest
class RedTeamTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Licenses licenses;

    @Test
    void aPinIsCheckedOnEveryCallNotOnlyThePicker() throws Exception {
        String user = People.viewer(accounts, "pinholder");
        List<?> profiles = (List<?>) Json.parse(mvc.perform(People.asViewer(get("/api/profiles"), user))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String adult = (String) ((Map<?, ?>) profiles.get(0)).get("id");
        mvc.perform(People.asViewer(put("/api/profiles/" + adult), user).contentType("application/json")
                .content("{\"name\":\"Me\",\"maxRating\":\"NC-17\",\"pin\":\"1234\"}")).andExpect(status().isOk());

        // Now guarded: the profile, and its default stand-in, need the key.
        mvc.perform(People.asViewer(get("/api/home?profile=" + adult), user)).andExpect(status().isForbidden());
        mvc.perform(People.asViewer(get("/api/home"), user)).andExpect(status().isForbidden());
        mvc.perform(People.asViewer(post("/api/profiles/" + adult + "/unlock"), user)
                .contentType("application/json").content("{\"pin\":\"0000\"}")).andExpect(status().isForbidden());
        String unlocked = mvc.perform(People.asViewer(post("/api/profiles/" + adult + "/unlock"), user)
                .contentType("application/json").content("{\"pin\":\"1234\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String key = (String) Json.parseObject(unlocked).get("key");
        mvc.perform(People.asViewer(get("/api/home?profile=" + adult), user).header(Requests.PROFILE_KEY, key))
                .andExpect(status().isOk());

        // A kids profile cannot be turned into a grown-up one without that key.
        String kid = (String) Json.parseObject(mvc.perform(People.asViewer(post("/api/profiles"), user)
                .header(Requests.PROFILE_KEY, key).contentType("application/json")
                .content("{\"name\":\"Kid\",\"kids\":true}")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).get("id");
        mvc.perform(People.asViewer(put("/api/profiles/" + kid), user).contentType("application/json")
                .content("{\"name\":\"Kid\",\"kids\":false,\"maxRating\":\"NC-17\"}"))
                .andExpect(status().isForbidden());
        // ...and a kid's own key is not a grown-up's.
        String kidKey = (String) Json.parseObject(mvc.perform(People.asViewer(post("/api/profiles/" + kid
                + "/unlock"), user).contentType("application/json").content("{}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("key");
        mvc.perform(People.asViewer(put("/api/profiles/" + kid), user).header(Requests.PROFILE_KEY, kidKey)
                .contentType("application/json").content("{\"name\":\"Kid\",\"kids\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void onlyAPublishedTitleIsForRent() throws Exception {
        String user = People.viewer(accounts, "renter");
        People.subscribe(mvc, user, "basic");
        String draft = (String) Json.parseObject(mvc.perform(People.asCreator(post("/api/studio/titles"),
                        com.codenameone.examples.reel.account.DemoAccounts.CREATOR).contentType("application/json")
                .content("{\"kind\":\"MOVIE\",\"name\":\"Unreleased\",\"synopsis\":\"x\",\"year\":2026,"
                        + "\"rating\":\"PG\",\"genres\":[],\"tags\":[],\"access\":\"RENTAL\",\"rentalCents\":399}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id");
        mvc.perform(People.asViewer(post("/api/billing/rent"), user).contentType("application/json")
                .content("{\"titleId\":\"" + draft + "\"}")).andExpect(status().isNotFound());
    }

    @Test
    void hardwareSchemesNeedTheirOwnPackaging() {
        List<String> clearOnly = Arrays.asList(Licenses.AES_128, Licenses.CLEARKEY);
        assertEquals(Licenses.CLEARKEY, licenses.choose(Arrays.asList(Licenses.WIDEVINE, Licenses.CLEARKEY),
                clearOnly));
        assertEquals(Licenses.AES_128, licenses.choose(Arrays.asList(Licenses.FAIRPLAY), clearOnly));
    }

    @Test
    void theSingleFileIsAsTallAsTheLadderMiddle() {
        assertEquals(480, Licenses.cencHeight("720,480,360"));
        assertEquals(1080, Licenses.cencHeight("720,2160,1080"));
        assertEquals(360, Licenses.cencHeight("360,720"));
        assertEquals(0, Licenses.cencHeight(""));
    }
}
