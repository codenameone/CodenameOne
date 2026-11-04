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
package com.codenameone.examples.dash;

import com.codename1.backend.Base64;
import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codenameone.examples.dash.account.Accounts;
import com.codenameone.examples.dash.forge.Forge;
import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.rules.Replay;
import com.codenameone.examples.dash.rules.forge.ContentPack;
import com.codenameone.examples.dash.rules.forge.PackCodec;
import com.codenameone.examples.dash.rules.forge.Samples;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The Forge: drafts, publishing, sharing, files, and solo runs the
/// server checks by playing them again.
@BackendTest
class ForgeTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;

    private static String draftBody(ContentPack p) {
        Map<String, Object> m = new HashMap<>();
        m.put("content", PackCodec.write(p));
        return Json.write(m);
    }

    private static String replayBody(String replay) {
        Map<String, Object> m = new HashMap<>();
        m.put("replay", replay);
        return Json.write(m);
    }

    private static ContentPack mine(String id) {
        ContentPack p = Samples.official();
        p.id = id;
        p.name = "Remix " + id;
        return p;
    }

    @Test
    void theOfficialCampaignIsPublishedOnStartup() throws Exception {
        String who = Api.player(accounts, "forge-reader");
        List official = Api.list(mvc, Api.as(get("/api/forge/packs?scope=official"), who));
        assertEquals(1, official.size());
        Map summary = (Map) official.get(0);
        assertEquals(Samples.OFFICIAL_ID, summary.get("id"));
        assertEquals(Boolean.TRUE, summary.get("official"));
        Map pack = Api.json(mvc, Api.as(get("/api/forge/packs/" + Samples.OFFICIAL_ID), who));
        ContentPack read = PackCodec.read((String) pack.get("content"));
        assertEquals(summary.get("hash"), read.hash());
    }

    @Test
    void aDraftIsPrivateUntilPublishedAndTheIdBelongsToItsOwner() throws Exception {
        String author = Api.player(accounts, "forge-author");
        String friend = Api.player(accounts, "forge-friend");
        Api.json(mvc, Api.as(Api.body(post("/api/forge/drafts"), draftBody(mine("my-war"))), author));
        assertEquals(404, Api.statusOf(mvc, Api.as(get("/api/forge/drafts/my-war"), friend)));
        assertEquals(404, Api.statusOf(mvc, Api.as(get("/api/forge/packs/my-war"), friend)));
        // Someone else's id cannot be taken over by saving a draft under it.
        assertEquals(409, Api.statusOf(mvc, Api.as(Api.body(post("/api/forge/drafts"),
                draftBody(mine("my-war"))), friend)));
        Map published = Api.json(mvc, Api.as(post("/api/forge/drafts/my-war/publish"), author));
        assertEquals(1L, Api.number(published, "version"));
        assertEquals("published", published.get("status"));
        // The byline is the author's battle name, whatever the file said.
        assertEquals("forge-author", published.get("author"));
        Map shared = Api.json(mvc, Api.as(get("/api/forge/packs/my-war"), friend));
        assertTrue(((String) shared.get("content")).contains("Remix my-war"));
        // A remix is the friend's own draft, and remembers where it came from.
        Map fork = Api.json(mvc, Api.as(Api.body(post("/api/forge/packs/my-war/fork"),
                "{\"newId\":\"friends-war\"}"), friend));
        assertEquals("draft", fork.get("status"));
        assertEquals(friend, fork.get("owner"));
    }

    @Test
    void anInvalidPackIsNotPublished() throws Exception {
        String author = Api.player(accounts, "forge-sloppy");
        ContentPack p = mine("broken-war");
        p.levels.get(0).id = "Not A Slug!";
        Api.json(mvc, Api.as(Api.body(post("/api/forge/drafts"), draftBody(p)), author));
        assertEquals(400, Api.statusOf(mvc, Api.as(post("/api/forge/drafts/broken-war/publish"), author)));
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/forge/drafts"),
                "{\"content\":\"{not json\"}"), author)));
    }

    @Test
    void reportsHideAPackAndAModeratorRestoresIt() throws Exception {
        String author = Api.player(accounts, "forge-edgy");
        Api.json(mvc, Api.as(Api.body(post("/api/forge/drafts"), draftBody(mine("edgy-war"))), author));
        Api.json(mvc, Api.as(post("/api/forge/drafts/edgy-war/publish"), author));
        Map last = null;
        for (int i = 0; i < Forge.REPORTS_TO_HIDE; i++) {
            String reporter = Api.player(accounts, "forge-reporter" + i);
            // The same player reporting twice counts once.
            Api.json(mvc, Api.as(Api.body(post("/api/forge/report"),
                    "{\"packId\":\"edgy-war\",\"reason\":\"rude\"}"), reporter));
            last = Api.json(mvc, Api.as(Api.body(post("/api/forge/report"),
                    "{\"packId\":\"edgy-war\",\"reason\":\"rude\"}"), reporter));
        }
        assertEquals("hidden", last.get("status"));
        String other = Api.player(accounts, "forge-passerby");
        assertEquals(404, Api.statusOf(mvc, Api.as(get("/api/forge/packs/edgy-war"), other)));
        assertEquals(403, Api.statusOf(mvc, Api.as(get("/api/admin/forge/packs"), other)));
        String admin = Api.player(accounts, "forge-admin");
        assertFalse(Api.list(mvc, Api.asAdmin(get("/api/admin/forge/packs"), admin)).isEmpty());
        Map restored = Api.json(mvc, Api.asAdmin(Api.body(post("/api/admin/forge/packs/action"),
                "{\"target\":\"edgy-war\",\"action\":\"restore\",\"reason\":\"fine\"}"), admin));
        assertEquals("published", restored.get("status"));
    }

    @Test
    void anUploadedModelIsCheckedAndWaitsForAModerator() throws Exception {
        String who = Api.player(accounts, "forge-modeller");
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/forge/assets"),
                "{\"name\":\"x.glb\",\"kind\":\"model\",\"data\":\"" + Base64.encode(new byte[64]) + "\"}"), who)));
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0};
        Map image = Api.json(mvc, Api.as(Api.body(post("/api/forge/assets"),
                "{\"name\":\"banner.png\",\"kind\":\"image\",\"data\":\"" + Base64.encode(png) + "\"}"), who));
        assertEquals("pending", image.get("status"));
        String admin = Api.player(accounts, "forge-curator");
        Map approved = Api.json(mvc, Api.asAdmin(Api.body(post("/api/admin/forge/assets/action"),
                "{\"target\":\"" + image.get("sha256") + "\",\"action\":\"approve\",\"reason\":\"ok\"}"), admin));
        assertEquals("approved", approved.get("status"));
    }

    // tag::dash-forge-result-test[]
    @Test
    void aRunPaysForStarsTheServerSawOnceAndEntersTheBoard() throws Exception {
        String who = Api.player(accounts, "forge-hero");
        Replay run = Runs.bot(Content.tutorial(), Content.catalog(), 1L, 99);
        String replay = run.encode();
        String body = replayBody(replay);
        Map first = Api.json(mvc, Api.as(Api.body(post("/api/forge/results"), body), who));
        assertTrue(Api.number(first, "stars") > 0);
        assertEquals(1L, Api.number(first, "rank"));
        // The same stars again pay nothing more.
        Map again = Api.json(mvc, Api.as(Api.body(post("/api/forge/results"), body), who));
        assertEquals(0L, Api.number((Map) again.get("reward"), "coins"));
        List board = Api.list(mvc, Api.as(get("/api/scores?level=" + Content.TUTORIAL + "&board=all&limit=10"), who));
        assertEquals(Api.number(first, "score"), Api.number((Map) board.get(0), "score"));
        // A replay of content the server never published is refused.
        String forged = replay.replace("|" + Content.TUTORIAL + "|",
                "|" + Samples.OFFICIAL_ID + "@0000000000000000#" + Content.TUTORIAL + "|");
        assertEquals(404, Api.statusOf(mvc, Api.as(Api.body(post("/api/forge/results"),
                replayBody(forged)), who)));
        // A run that was edited after the fact no longer replays to its claim.
        String tampered = replay.substring(0, replay.lastIndexOf('|') + 1);
        assertEquals(400, Api.statusOf(mvc, Api.as(Api.body(post("/api/forge/results"),
                replayBody(tampered + "0,0,99")), who)));
    }
    // end::dash-forge-result-test[]
}
