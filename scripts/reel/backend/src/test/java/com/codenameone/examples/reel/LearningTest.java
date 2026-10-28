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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcResultMatchers.jsonPath;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The academy: a course's lessons watched to the end and its quizzes passed
/// earn a certificate anyone can check by its code.
@BackendTest
class LearningTest {
    private static final String COURSE = "streaming-101";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    private Map start(String user, String lesson) throws Exception {
        String body = mvc.perform(People.asViewer(post("/api/playback/start"), user).contentType("application/json")
                        .content("{\"titleId\":\"" + COURSE + "\",\"episodeId\":\"" + lesson
                                + "\",\"schemes\":[\"AES_128\"]}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return Json.parseObject(body);
    }

    private Map answer(String user, String session, String interaction, int choice, int expected) throws Exception {
        String body = mvc.perform(People.asViewer(post("/api/learning/answer"), user).contentType("application/json")
                        .content("{\"interactionId\":\"" + interaction + "\",\"sessionId\":\"" + session
                                + "\",\"choices\":[" + choice + "]}"))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return expected == 200 ? Json.parseObject(body) : null;
    }

    private void finish(String user, Map playback) throws Exception {
        mvc.perform(People.asViewer(post("/api/playback/stop"), user).contentType("application/json")
                        .content("{\"sessionId\":\"" + playback.get("sessionId")
                                + "\",\"positionSeconds\":600,\"completed\":true}"))
                .andExpect(status().isOk());
    }

    /// The first quiz of the playback, or null.
    private static Map quiz(Map playback, int index) {
        List all = (List) playback.get("interactions");
        int seen = 0;
        for (Object o : all) {
            Map i = (Map) o;
            if ("QUIZ".equals(i.get("kind")) && seen++ == index) {
                return i;
            }
        }
        return null;
    }

    @Test
    void passingTheCourseEarnsAVerifiableCertificate() throws Exception {
        String user = People.viewer(accounts, "student");
        People.subscribe(mvc, user, "standard");

        Map one = start(user, "academy-lesson-1");
        // A lesson carries no ads, whatever the plan.
        assertTrue(((List) one.get("adBreaks")).isEmpty());
        Map q1 = quiz(one, 0);
        Map result = answer(user, (String) one.get("sessionId"), (String) q1.get("id"), 0, 200);
        assertEquals(Boolean.TRUE, result.get("correct"));
        // A quiz answer is final.
        answer(user, (String) one.get("sessionId"), (String) q1.get("id"), 1, 409);
        finish(user, one);

        Map two = start(user, "academy-lesson-2");
        // Another viewer's session does not answer for this one.
        String other = People.viewer(accounts, "classmate");
        mvc.perform(People.asViewer(post("/api/learning/answer"), other).contentType("application/json")
                        .content("{\"interactionId\":\"" + quiz(two, 0).get("id") + "\",\"sessionId\":\""
                                + two.get("sessionId") + "\",\"choices\":[1]}"))
                .andExpect(status().isNotFound());
        answer(user, (String) two.get("sessionId"), (String) quiz(two, 0).get("id"), 1, 200);
        finish(user, two);
        mvc.perform(People.asViewer(get("/api/learning/certificates"), user)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        finish(user, start(user, "academy-lesson-3"));
        String certs = mvc.perform(People.asViewer(get("/api/learning/progress"), user))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(certs.contains("\"certificateEarned\":true"), certs);
        String mine = mvc.perform(People.asViewer(get("/api/learning/certificates"), user))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Map cert = (Map) ((List) Json.parse(mine)).get(0);
        assertEquals(100L, Rows.number(cert.get("scorePercent")));
        mvc.perform(get("/api/verify/" + cert.get("verifyCode")).header("Host", SignIn.HOST))
                .andExpect(status().isOk()).andExpect(jsonPath("$.titleName").value("Streaming 101"));
    }
}
