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
import com.codenameone.examples.reel.playback.Subtitles;
import com.codenameone.examples.reel.playback.Tracks;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.delete;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.put;
import static com.codename1.backend.test.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Subtitles, audio tracks, qualities, the advisories beside a rating and
/// the public page a shared link opens.
@BackendTest
class TracksTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    private Map json(com.codename1.backend.test.MockRequestBuilder request, int expected) throws Exception {
        String body = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse()
                .getContentAsString();
        // A refusal answers with a message, not an object.
        return body.startsWith("{") ? Json.parseObject(body) : new java.util.HashMap();
    }

    @Test
    void qualitiesAreTheRenditionsUnderTheCapTallestFirst() {
        assertEquals(Arrays.asList(720, 480), Tracks.qualities("480,1080,720,junk,720", 720));
        assertEquals(1080, Tracks.tallest("480,1080,720"));
        assertEquals(0, Tracks.tallest(""));
    }

    @Test
    void languageTagsAreCheckedBeforeTheyBecomeFileNames() {
        assertTrue(Subtitles.validLanguage("en"));
        assertTrue(Subtitles.validLanguage("pt-BR"));
        assertFalse(Subtitles.validLanguage("e"));
        assertFalse(Subtitles.validLanguage("../x"));
        assertFalse(Subtitles.validLanguage("en-"));
        assertFalse(Subtitles.validLanguage("en--US"));
        assertFalse(Subtitles.validLanguage("english"));
    }

    @Test
    void aLessonPlaysWithItsSubtitlesAndServesThem() throws Exception {
        String user = People.viewer(accounts, "learner");
        People.subscribe(mvc, user, "basic");
        Map p = json(People.asViewer(post("/api/playback/start"), user).contentType("application/json")
                .content("{\"titleId\":\"streaming-101\",\"episodeId\":\"academy-lesson-1\","
                        + "\"schemes\":[\"AES_128\"]}"), 200);
        assertEquals("Streaming 101", p.get("titleName"));
        assertEquals("academy-lesson-2", p.get("nextEpisodeId"));
        List subs = (List) p.get("subtitles");
        assertEquals(4, subs.size());
        String url = null;
        for (Object each : subs) {
            if ("es".equals(((Map) each).get("language"))) {
                url = (String) ((Map) each).get("url");
            }
        }
        String vtt = mvc.perform(get(url)).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        assertTrue(vtt.startsWith("WEBVTT"), vtt);
        // Another video's token does not reach this video's tracks, and a
        // language with no track is not found.
        mvc.perform(get(url.replace("/academy-lesson-1/", "/academy-lesson-2/"))).andExpect(status().isForbidden());
        mvc.perform(get(url.replace("sub_es.vtt", "sub_it.vtt"))).andExpect(status().isNotFound());
        List audio = (List) p.get("audioTracks");
        assertEquals(1, audio.size());
        assertEquals("main", p.get("audioTrack"));
    }

    @Test
    void anAudioTrackIsMadeTheDefaultOfItsGroup() {
        String master = "#EXTM3U\n"
                + "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"a\",NAME=\"main\",DEFAULT=YES,AUTOSELECT=YES,URI=\"a.m3u8\"\n"
                + "#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"a\",NAME=\"dub\",URI=\"b.m3u8\"\n"
                + "#EXT-X-STREAM-INF:BANDWIDTH=1,AUDIO=\"a\"\nv.m3u8\n";
        String out = com.codenameone.examples.reel.playback.MediaOriginAccess.preferAudio(master, "dub");
        assertTrue(out.indexOf("NAME=\"main\",DEFAULT=NO,AUTOSELECT=NO") > 0, out);
        String dub = out.substring(out.indexOf("NAME=\"dub\""));
        dub = dub.substring(0, dub.indexOf('\n'));
        assertTrue(dub.indexOf("DEFAULT=YES") > 0 && dub.indexOf("AUTOSELECT=YES") > 0, out);
        assertEquals(master, com.codenameone.examples.reel.playback.MediaOriginAccess.preferAudio(master, "none"));
    }

    @Test
    void aCreatorAddsAndRemovesSubtitlesAndAdvisories() throws Exception {
        String creator = People.viewer(accounts, "captioner");
        String stranger = People.viewer(accounts, "meddler");
        Map title = json(People.asCreator(post("/api/studio/titles"), creator).contentType("application/json")
                .content("{\"kind\":\"MOVIE\",\"name\":\"Captioned\",\"synopsis\":\"A test.\",\"year\":2026,"
                        + "\"rating\":\"PG-13\",\"advisories\":[\"violence\",\"language\"],\"genres\":[],"
                        + "\"tags\":[],\"access\":\"FREE\"}"), 200);
        assertEquals(Arrays.asList("violence", "language"), title.get("advisories"));
        json(People.asCreator(post("/api/studio/titles"), creator).contentType("application/json")
                .content("{\"kind\":\"MOVIE\",\"name\":\"Bad\",\"synopsis\":\"\",\"year\":2026,\"rating\":\"PG\","
                        + "\"advisories\":[\"made-up\"],\"genres\":[],\"tags\":[],\"access\":\"FREE\"}"), 400);
        String titleId = (String) title.get("id");
        Map video = json(People.asCreator(post("/api/studio/titles/" + titleId + "/videos"), creator)
                .contentType("application/json").content("{\"role\":\"FEATURE\",\"number\":1,\"name\":\"F\"}"), 200);
        String videoId = (String) video.get("id");
        String path = "/api/studio/videos/" + videoId + "/subtitles/";

        Map added = json(People.asCreator(put(path + "fr"), creator).contentType("application/json")
                .content("{\"label\":\"Fran\\u00e7ais\",\"vtt\":\"WEBVTT\\n\\n00:00:01.000 --> 00:00:02.000\\nBonjour\\n\"}"),
                200);
        assertEquals(1, ((List) added.get("subtitles")).size());
        json(People.asCreator(put(path + "de"), creator).contentType("application/json")
                .content("{\"label\":\"Deutsch\",\"vtt\":\"not a subtitle file\"}"), 400);
        json(People.asCreator(put(path + "x"), creator).contentType("application/json")
                .content("{\"label\":\"Bad\",\"vtt\":\"WEBVTT\\n\"}"), 400);
        mvc.perform(People.asCreator(put(path + "es"), stranger).contentType("application/json")
                .content("{\"label\":\"Espa\\u00f1ol\",\"vtt\":\"WEBVTT\\n\"}")).andExpect(status().is(404));

        Map removed = json(People.asCreator(delete(path + "fr"), creator), 200);
        assertTrue(((List) removed.get("subtitles")).isEmpty());
    }

    @Test
    void aPublishedTitleHasAPublicPageAndADraftDoesNot() throws Exception {
        String page = mvc.perform(get("/watch/sintel")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        assertTrue(page.indexOf("og:title") > 0, page);
        assertTrue(page.indexOf("Sintel") > 0, page);
        assertTrue(page.indexOf("Frightening") > 0 || page.indexOf("frightening") > 0, page);
        assertTrue(page.indexOf("/?title=sintel") > 0, page);
        mvc.perform(get("/watch/no-such-title")).andExpect(status().isNotFound());

        String user = People.viewer(accounts, "sharer");
        Map t = json(People.asViewer(get("/api/titles/sintel"), user), 200);
        assertEquals("/watch/sintel", t.get("shareUrl"));
        assertTrue(((List) t.get("advisories")).contains("frightening"));
    }
}
