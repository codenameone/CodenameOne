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

import com.codename1.backend.Crypto;
import com.codename1.backend.Json;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.reel.account.DemoAccounts;
import com.codenameone.examples.reel.ads.Ads;
import com.codenameone.examples.reel.billing.Billing;
import com.codenameone.examples.reel.billing.Entitlements;
import com.codenameone.examples.reel.domain.AdCampaign;
import com.codenameone.examples.reel.domain.Chapter;
import com.codenameone.examples.reel.domain.Genre;
import com.codenameone.examples.reel.domain.Interaction;
import com.codenameone.examples.reel.domain.Plan;
import com.codenameone.examples.reel.domain.Title;
import com.codenameone.examples.reel.domain.Video;
import com.codenameone.examples.reel.domain.VideoKey;
import com.codenameone.examples.reel.learning.Learning;
import com.codenameone.examples.reel.repository.AdCampaignRepository;
import com.codenameone.examples.reel.repository.ChapterRepository;
import com.codenameone.examples.reel.repository.GenreRepository;
import com.codenameone.examples.reel.repository.InteractionRepository;
import com.codenameone.examples.reel.repository.PlanRepository;
import com.codenameone.examples.reel.repository.TitleRepository;
import com.codenameone.examples.reel.repository.VideoKeyRepository;
import com.codenameone.examples.reel.repository.VideoRepository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// What a development server starts with: the plans, the genres, the demo
/// accounts, two ad campaigns and a catalog of the Blender open movies plus a
/// short course.
///
/// The video itself is not in the repository. `tools/fetch-demo-media.sh`
/// downloads the films, packages them with fresh random keys and writes
/// `seed.json` beside them; this reads that file for each video's keys,
/// duration and renditions. Without it -- a fresh checkout, the tests -- the
/// catalog is written anyway, with keys made here, so every screen works and
/// only the picture is missing.
@Component
public class DemoData {
    /// The catalog: id, kind, name, year, rating, genres, access, synopsis.
    private static final String[][] TITLES = {
        {"big-buck-bunny", Kinds.MOVIE, "Big Buck Bunny", "2008", "G", "animation,comedy", Entitlements.FREE,
            "A gentle giant rabbit takes a stand against three bullying rodents."},
        {"sintel", Kinds.MOVIE, "Sintel", "2010", "PG-13", "animation,fantasy", Entitlements.SUBSCRIPTION,
            "A lonely girl searches the world for the baby dragon she raised."},
        {"tears-of-steel", Kinds.MOVIE, "Tears of Steel", "2012", "PG-13", "sci-fi,action", Entitlements.RENTAL,
            "In a future Amsterdam, scientists replay one breakup to save the world from robots."},
        {"elephants-dream", Kinds.MOVIE, "Elephants Dream", "2006", "PG", "sci-fi,animation",
            Entitlements.SUBSCRIPTION, "Two men explore a vast and strange machine that may only be in their heads."},
        {"streaming-101", Kinds.COURSE, "Streaming 101", "2026", "G", "learning", Entitlements.SUBSCRIPTION,
            "How a streaming service protects its video: segments, keys, licences and downloads."}};
    private static final String[][] GENRES = {
        {"animation", "Animation"}, {"comedy", "Comedy"}, {"fantasy", "Fantasy"}, {"sci-fi", "Science Fiction"},
        {"action", "Action"}, {"learning", "Learning"}};
    private static final String[] LESSONS = {
        "Welcome to Reel Academy", "Keys and licences", "Watching offline"};

    private final DemoAccounts accounts;
    private final PlanRepository plans;
    private final GenreRepository genres;
    private final TitleRepository titles;
    private final VideoRepository videos;
    private final VideoKeyRepository keys;
    private final ChapterRepository chapters;
    private final InteractionRepository interactions;
    private final AdCampaignRepository campaigns;
    private final String mediaDir;
    private final String currency;

    public DemoData(DemoAccounts accounts, PlanRepository plans, GenreRepository genres, TitleRepository titles,
            VideoRepository videos, VideoKeyRepository keys, ChapterRepository chapters,
            InteractionRepository interactions, AdCampaignRepository campaigns,
            @Value("${reel.media.dir:media}") String mediaDir,
            @Value("${reel.currency:USD}") String currency) {
        this.accounts = accounts;
        this.plans = plans;
        this.genres = genres;
        this.titles = titles;
        this.videos = videos;
        this.keys = keys;
        this.chapters = chapters;
        this.interactions = interactions;
        this.campaigns = campaigns;
        this.mediaDir = mediaDir;
        this.currency = currency;
    }

    @PostConstruct
    public void seed() throws IOException {
        if (accounts.seed()) {
            catalog(readSeed());
        }
    }

    @Transactional(rollbackFor = IOException.class)
    void catalog(Map<String, Map> seed) throws IOException {
        long now = System.currentTimeMillis();
        plan("basic", "Basic with ads", "Everything, with short ad breaks. One screen, HD.", 699, 1, "720", true,
                false, 7, 0);
        plan("standard", "Standard", "No ads, two screens in Full HD, downloads.", 1549, 2, "1080", false, true,
                7, 1);
        plan("premium", "Premium", "No ads, four screens in the best quality there is, downloads.", 2299, 4,
                "2160", false, true, 0, 2);
        for (int iter = 0; iter < GENRES.length; iter++) {
            Genre g = new Genre();
            g.setId(GENRES[iter][0]);
            g.setName(GENRES[iter][1]);
            g.setPosition(iter);
            genres.insert(g);
        }
        for (int iter = 0; iter < TITLES.length; iter++) {
            String[] t = TITLES[iter];
            Title row = new Title();
            row.setId(t[0]);
            row.setKind(t[1]);
            row.setName(t[2]);
            row.setYear(Integer.parseInt(t[3]));
            row.setRating(t[4]);
            row.setGenres(t[5]);
            row.setAccess(t[6]);
            row.setRentalPriceCents(Entitlements.RENTAL.equals(t[6]) ? 399 : 0);
            row.setSynopsis(t[7]);
            row.setCreator(DemoAccounts.CREATOR);
            row.setStatus(Kinds.PUBLISHED);
            row.setFeatured(iter == 1);
            row.setCreatedAt(now - (TITLES.length - iter) * Days.MILLIS);
            row.setPublishedAt(row.getCreatedAt());
            if (Kinds.COURSE.equals(t[1])) {
                row.setCertificate(true);
                row.setPassPercent(70);
                String first = "academy-lesson-1";
                row.setPosterUrl("/art/" + first + "/poster.jpg");
                row.setBackdropUrl(row.getPosterUrl());
                titles.insert(row);
                for (int lesson = 0; lesson < LESSONS.length; lesson++) {
                    video("academy-lesson-" + (lesson + 1), t[0], Kinds.LESSON, lesson + 1, LESSONS[lesson],
                            seed, now);
                }
                lessonContent();
            } else {
                row.setPosterUrl("/art/" + t[0] + "/poster.jpg");
                row.setBackdropUrl(row.getPosterUrl());
                titles.insert(row);
                video(t[0], t[0], Kinds.FEATURE, 1, t[2], seed, now);
            }
        }
        campaign("ad-coffee", "Northside Roasters", "Fresh Coffee", "https://example.com/coffee", "", now, seed);
        campaign("ad-bike", "Ride More Cycles", "Ride More", "https://example.com/bikes", "action,sci-fi", now, seed);
    }

    private void plan(String code, String name, String description, long cents, int streams, String quality,
            boolean ads, boolean downloads, int trialDays, int position) throws IOException {
        Plan p = new Plan();
        p.setCode(code);
        p.setName(name);
        p.setDescription(description);
        p.setPriceCents(cents);
        p.setCurrency(currency);
        p.setBillingInterval(Billing.MONTH);
        p.setMaxStreams(streams);
        p.setMaxQuality(quality);
        p.setAdSupported(ads);
        p.setDownloads(downloads);
        p.setTrialDays(trialDays);
        p.setActive(true);
        p.setPosition(position);
        plans.insert(p);
    }

    private void video(String id, String titleId, String role, int number, String name, Map<String, Map> seed,
            long now) throws IOException {
        Map entry = seed.get(id);
        Video v = new Video();
        v.setId(id);
        v.setTitleId(titleId);
        v.setRole(role);
        v.setNumber(number);
        v.setName(name);
        v.setStatus(Kinds.READY);
        v.setProgressPercent(100);
        v.setDurationSeconds(entry == null ? 600 : (int) Rows.number(entry.get("durationSeconds")));
        v.setWidth(entry == null ? 1280 : (int) Rows.number(entry.get("width")));
        v.setHeight(entry == null ? 720 : (int) Rows.number(entry.get("height")));
        v.setRenditions(entry == null ? "360,720" : Rows.join(strings(entry.get("renditions"))));
        // The same three as a video the worker packaged: see Jobs.done.
        v.setSchemes("AES_128,CLEARKEY,WIDEVINE");
        v.setThumbnailUrl("/art/" + id + "/poster.jpg");
        v.setCreatedAt(now);
        videos.insert(v);
        VideoKey k = new VideoKey();
        k.setVideoId(id);
        k.setKeyId(entry == null ? Ids.hex(Crypto.randomBytes(16)) : Rows.text(entry.get("keyId")));
        k.setKeyHex(entry == null ? Ids.hex(Crypto.randomBytes(16)) : Rows.text(entry.get("keyHex")));
        k.setIvHex(entry == null ? Ids.hex(Crypto.randomBytes(16)) : Rows.text(entry.get("ivHex")));
        k.setCreatedAt(now);
        keys.insert(k);
    }

    /// Chapters, a quiz, a poll and a branch, at the points the generated
    /// lessons show them.
    private void lessonContent() throws IOException {
        chapter("academy-lesson-1", 0, "Why segments");
        chapter("academy-lesson-1", 15, "Encryption");
        chapter("academy-lesson-2", 0, "Asking for a key");
        chapter("academy-lesson-3", 0, "Downloads");
        interaction("academy-lesson-1", Learning.QUIZ, 30, "What does a player download first?",
                Arrays.asList("The master playlist", "The last segment", "The key"), "0",
                "The master playlist lists the renditions, and each rendition lists its segments.", true, "", 0);
        interaction("academy-lesson-2", Learning.POLL, 20, "Which device do you watch on most?",
                Arrays.asList("Phone", "Tablet", "TV", "Computer"), "", "", false, "", 0);
        interaction("academy-lesson-2", Learning.QUIZ, 35, "Who gets a decryption key?",
                Arrays.asList("Anyone with the link", "Entitled viewers", "Only admins"), "1",
                "The license server checks the session before it hands over a key.", true, "", 1);
        interaction("academy-lesson-3", Learning.BRANCH, 40, "Where next?",
                Arrays.asList("Review keys and licences", "Finish the course"), "", "", false,
                "academy-lesson-2,", 0);
    }

    private void chapter(String video, int at, String name) throws IOException {
        Chapter c = new Chapter();
        c.setId(Ids.next());
        c.setVideoId(video);
        c.setStartSeconds(at);
        c.setTitle(name);
        chapters.insert(c);
    }

    private void interaction(String video, String kind, int at, String prompt, List<String> options,
            String answers, String explanation, boolean required, String targets, int position) throws IOException {
        Interaction i = new Interaction();
        i.setId(Ids.next());
        i.setVideoId(video);
        i.setKind(kind);
        i.setAtSeconds(at);
        i.setPrompt(prompt);
        i.setOptions(Rows.joinLines(options));
        i.setAnswers(answers);
        i.setExplanation(explanation);
        i.setRequired(required);
        i.setBranchTargets(targets);
        i.setPosition(position);
        interactions.insert(i);
    }

    private void campaign(String video, String advertiser, String name, String click, String genreList, long now,
            Map<String, Map> seed) throws IOException {
        video(video, "", Kinds.AD, 1, name, seed, now);
        AdCampaign c = new AdCampaign();
        c.setId(Ids.next());
        c.setAdvertiser(advertiser);
        c.setName(name);
        c.setCreativeUrl(Ads.VIDEO + video);
        c.setDurationSeconds(15);
        c.setSkippableAfterSeconds(5);
        c.setClickUrl(click);
        c.setCpmCents(1200);
        c.setBudgetCents(50000);
        c.setGenres(genreList);
        c.setActive(true);
        c.setStartsAt(now - Days.MILLIS);
        c.setEndsAt(now + 90 * Days.MILLIS);
        c.setCreatedAt(now);
        campaigns.insert(c);
    }

    /// `seed.json` keyed by slug, or empty when there is none.
    Map<String, Map> readSeed() throws IOException {
        Map<String, Map> out = new HashMap<String, Map>();
        File f = new File(mediaDir, "seed.json");
        if (f.isFile()) {
            Object list = Json.parseObject(new String(read(f), "UTF-8")).get("videos");
            if (list instanceof List) {
                List rows = (List) list;
                for (int iter = 0; iter < rows.size(); iter++) {
                    Object row = rows.get(iter);
                    if (row instanceof Map) {
                        out.put(Rows.text(((Map) row).get("slug")), (Map) row);
                    }
                }
            }
        }
        return out;
    }

    private static List<String> strings(Object value) {
        List<String> out = new ArrayList<String>();
        if (value instanceof List) {
            List list = (List) value;
            for (int iter = 0; iter < list.size(); iter++) {
                out.add(String.valueOf(Rows.number(list.get(iter))));
            }
        }
        return out;
    }

    private static byte[] read(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            for (int n = in.read(buf); n > 0; n = in.read(buf)) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
