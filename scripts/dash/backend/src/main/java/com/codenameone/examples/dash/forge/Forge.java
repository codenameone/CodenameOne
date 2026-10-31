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
package com.codenameone.examples.dash.forge;

import com.codename1.backend.Base64;
import com.codename1.backend.Crypto;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.dash.Clock;
import com.codenameone.examples.dash.Ids;
import com.codenameone.examples.dash.account.Lists;
import com.codenameone.examples.dash.api.AssetDto;
import com.codenameone.examples.dash.api.AssetUploadDto;
import com.codenameone.examples.dash.api.LevelOutcomeDto;
import com.codenameone.examples.dash.api.LevelStarsDto;
import com.codenameone.examples.dash.api.PackDto;
import com.codenameone.examples.dash.api.PackSummaryDto;
import com.codenameone.examples.dash.api.RewardDto;
import com.codenameone.examples.dash.domain.AdminAction;
import com.codenameone.examples.dash.domain.ForgeAsset;
import com.codenameone.examples.dash.domain.ForgeEarning;
import com.codenameone.examples.dash.domain.ForgePack;
import com.codenameone.examples.dash.domain.ForgePackVersion;
import com.codenameone.examples.dash.domain.ForgeRating;
import com.codenameone.examples.dash.domain.LevelStars;
import com.codenameone.examples.dash.domain.Profile;
import com.codenameone.examples.dash.progress.Progression;
import com.codenameone.examples.dash.repository.AdminActionRepository;
import com.codenameone.examples.dash.repository.ForgeAssetRepository;
import com.codenameone.examples.dash.repository.ForgeEarningRepository;
import com.codenameone.examples.dash.repository.ForgePackRepository;
import com.codenameone.examples.dash.repository.ForgePackVersionRepository;
import com.codenameone.examples.dash.repository.ForgeRatingRepository;
import com.codenameone.examples.dash.repository.LevelStarsRepository;
import com.codenameone.examples.dash.repository.ProfileRepository;
import com.codenameone.examples.dash.rules.LevelDef;
import com.codenameone.examples.dash.rules.Run;
import com.codenameone.examples.dash.rules.Replay;
import com.codenameone.examples.dash.rules.forge.AssetRef;
import com.codenameone.examples.dash.rules.forge.ContentPack;
import com.codenameone.examples.dash.rules.CityRoute;
import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.rules.forge.ContentRef;
import com.codenameone.examples.dash.rules.forge.GlbInspector;
import com.codenameone.examples.dash.rules.Json;
import com.codenameone.examples.dash.rules.forge.LevelRun;
import com.codenameone.examples.dash.rules.forge.PackCodec;
import com.codenameone.examples.dash.rules.forge.Samples;
import com.codenameone.examples.dash.rules.forge.Validator;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// The Forge on the server: players' content packs from draft to published
/// version, the files they upload, ratings and reports, and the stars and
/// rewards their levels pay.
///
/// Nothing a client sends is trusted. A draft is only parsed; publishing runs
/// the same `Validator` the editor runs, and a level result is a replay the
/// server plays again with its own bot (`LevelRun.verify`) before it counts a
/// single star.
@Component
public class Forge {
    public static final String DRAFT = "draft";
    public static final String PUBLISHED = "published";
    public static final String HIDDEN = "hidden";
    public static final String DELETED = "deleted";

    public static final String PENDING = "pending";
    public static final String APPROVED = "approved";
    public static final String REJECTED = "rejected";

    /// The owner of the packs the game ships with.
    public static final String SYSTEM = "system";

    public static final int MAX_PACKS_PER_PLAYER = 20;
    public static final int MAX_UPLOADS_PER_DAY = 20;
    /// Reports from this many players hide a pack until a moderator looks.
    public static final int REPORTS_TO_HIDE = 3;
    /// A player's own pack, or a friend's, must not be a coin farm: an
    /// unofficial level pays at most this much, and all of them together at
    /// most `DAILY_COMMUNITY_COINS` a day. Official levels pay what their
    /// designer set.
    /// What each star of an official level pays the first time.
    public static final int OFFICIAL_STAR_COINS = 20;
    public static final int OFFICIAL_STAR_XP = 15;
    public static final int COMMUNITY_LEVEL_COINS = 30;
    public static final int COMMUNITY_LEVEL_XP = 20;
    public static final int DAILY_COMMUNITY_COINS = 150;

    private static final int PAGE = 50;

    private final ForgePackRepository packs;
    private final ForgePackVersionRepository versions;
    private final ForgeAssetRepository assets;
    private final LevelStarsRepository stars;
    private final ForgeRatingRepository ratings;
    private final ForgeEarningRepository earnings;
    private final ProfileRepository profiles;
    private final AdminActionRepository audit;
    private final Progression progression;
    private final Scores scores;

    public Forge(ForgePackRepository packs, ForgePackVersionRepository versions, ForgeAssetRepository assets,
            LevelStarsRepository stars, ForgeRatingRepository ratings, ForgeEarningRepository earnings,
            ProfileRepository profiles, AdminActionRepository audit, Progression progression, Scores scores) {
        this.scores = scores;
        this.packs = packs;
        this.versions = versions;
        this.assets = assets;
        this.stars = stars;
        this.ratings = ratings;
        this.earnings = earnings;
        this.profiles = profiles;
        this.audit = audit;
        this.progression = progression;
    }

    /// Publishes the official campaign the first time the server starts, and
    /// again whenever the game's own copy of it changes.
    @PostConstruct
    @Transactional(rollbackFor = IOException.class)
    public void seedOfficial() throws IOException {
        ContentPack official = Samples.official();
        ForgePack p = packs.findById(official.id);
        boolean fresh = p == null;
        if (fresh) {
            p = new ForgePack();
            p.setId(official.id);
            p.setOwner(SYSTEM);
            p.setCreatedAt(Instant.now());
        } else if (!SYSTEM.equals(p.getOwner())) {
            return;
        }
        official.version = p.getVersion();
        String canonical = PackCodec.write(official);
        if (!fresh && ContentPack.hashOf(canonical).equals(p.getHash())) {
            return;
        }
        p.setDraft(canonical);
        p.setOfficial(true);
        publishVersion(p, official, fresh);
    }

    // ---- browsing -------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PackSummaryDto> list(String username, String scope) throws IOException {
        PageRequest page = new PageRequest(0, PAGE);
        List<ForgePack> rows;
        if ("mine".equals(scope)) {
            rows = packs.ownedBy(username, page);
            List<ForgePack> live = new ArrayList<>();
            for (ForgePack p : rows) {
                if (!DELETED.equals(p.getStatus())) {
                    live.add(p);
                }
            }
            rows = live;
        } else if ("official".equals(scope)) {
            rows = packs.official(PUBLISHED, true, page);
        } else if ("new".equals(scope)) {
            rows = packs.newest(PUBLISHED, false, page);
        } else {
            rows = packs.popular(PUBLISHED, false, page);
        }
        return summaries(rows);
    }

    /// The newest published version. Anyone with the link may open a published
    /// pack, even one whose files are still waiting for a moderator: that is
    /// how a player shares a pack with friends before it is listed.
    @Transactional(readOnly = true)
    public PackDto pack(String username, boolean admin, String id) throws IOException {
        ForgePack p = visible(username, admin, id);
        if (p.getVersion() == 0) {
            throw new ResponseStatusException(404, "That pack has not been published yet");
        }
        return version(username, admin, id, p.getVersion());
    }

    @Transactional(readOnly = true)
    public PackDto version(String username, boolean admin, String id, int version) throws IOException {
        ForgePack p = visible(username, admin, id);
        ForgePackVersion v = versions.findById(versionId(id, version));
        if (v == null) {
            throw new ResponseStatusException(404, "No such version");
        }
        return new PackDto(summary(p), v.getContent());
    }

    @Transactional(readOnly = true)
    public PackDto draft(String username, String id) throws IOException {
        ForgePack p = owned(username, id);
        return new PackDto(summary(p), p.getDraft());
    }

    // ---- editing --------------------------------------------------------

    /// Saves a draft. It is parsed, so it is known to be a pack, but not
    /// validated: a half-built level is a normal thing to save.
    @Transactional(rollbackFor = IOException.class)
    public PackSummaryDto saveDraft(String username, String content) throws IOException {
        ContentPack pack = parse(content);
        if (!Validator.isSlug(pack.id, 3, 40)) {
            throw new ResponseStatusException(400, "A pack id is 3 to 40 lower-case letters, digits and dashes");
        }
        ForgePack p = packs.findById(pack.id);
        if (p == null) {
            if (packs.countByOwner(username) >= MAX_PACKS_PER_PLAYER) {
                throw new ResponseStatusException(409, "You have " + MAX_PACKS_PER_PLAYER + " packs already");
            }
            p = new ForgePack();
            p.setId(pack.id);
            p.setOwner(username);
            p.setStatus(DRAFT);
            p.setCreatedAt(Instant.now());
            fillDraft(p, pack, username);
            packs.insert(p);
        } else {
            if (!username.equals(p.getOwner())) {
                throw new ResponseStatusException(409, "That pack id is taken");
            }
            if (DELETED.equals(p.getStatus())) {
                p.setStatus(p.getVersion() == 0 ? DRAFT : PUBLISHED);
            }
            fillDraft(p, pack, username);
            packs.save(p);
        }
        return summary(p);
    }

    /// Validates the draft and makes it the pack's next version.
    @Transactional(rollbackFor = IOException.class)
    public PackSummaryDto publish(String username, String id) throws IOException {
        ForgePack p = owned(username, id);
        ContentPack pack = parse(p.getDraft());
        List<String> problems = Validator.check(pack);
        if (!problems.isEmpty()) {
            throw new ResponseStatusException(400, problems.get(0));
        }
        publishVersion(p, pack, false);
        return summary(p);
    }

    @Transactional(rollbackFor = IOException.class)
    public PackSummaryDto delete(String username, boolean admin, String id) throws IOException {
        ForgePack p = admin ? existing(id) : owned(username, id);
        if (SYSTEM.equals(p.getOwner())) {
            throw new ResponseStatusException(403, "The game's own packs cannot be deleted");
        }
        // Versions are kept: a replay names the version it was played on.
        p.setStatus(DELETED);
        p.setUpdatedAt(Instant.now());
        packs.save(p);
        return summary(p);
    }

    /// Copies a published pack into a new draft of the caller's own, the way a
    /// player remixes a friend's campaign.
    @Transactional(rollbackFor = IOException.class)
    public PackSummaryDto fork(String username, boolean admin, String id, String newId) throws IOException {
        PackDto source = pack(username, admin, id);
        ContentPack pack = parse(source.content());
        pack.id = newId == null ? "" : newId;
        pack.version = 0;
        PackSummaryDto made = saveDraft(username, PackCodec.write(pack));
        ForgePack p = packs.findById(made.id());
        p.setForkedFrom(id);
        packs.save(p);
        return summary(p);
    }

    // ---- ratings and reports ---------------------------------------------

    @Transactional(rollbackFor = IOException.class)
    public PackSummaryDto rate(String username, String packId, int value) throws IOException {
        if (value < 1 || value > 5) {
            throw new ResponseStatusException(400, "A rating is one to five stars");
        }
        ForgePack p = listed(packId);
        if (username.equals(p.getOwner())) {
            throw new ResponseStatusException(409, "You cannot rate your own pack");
        }
        String key = Ids.sha256("rate|" + username + "|" + packId);
        ForgeRating r = ratings.findById(key);
        if (r == null) {
            r = new ForgeRating();
            r.setId(key);
            r.setUsername(username);
            r.setPackId(packId);
            r.setStars(value);
            ratings.insert(r);
            p.setRatingCount(p.getRatingCount() + 1);
            p.setRatingSum(p.getRatingSum() + value);
        } else {
            p.setRatingSum(p.getRatingSum() - r.getStars() + value);
            r.setStars(value);
            ratings.save(r);
        }
        packs.save(p);
        return summary(p);
    }

    /// Counts a player's report once, and hides the pack for review once
    /// enough players agree. The row lives beside the ratings, keyed apart
    /// from them, so the rating sums never see it.
    @Transactional(rollbackFor = IOException.class)
    public PackSummaryDto report(String username, String packId, String reason) throws IOException {
        ForgePack p = listed(packId);
        String key = Ids.sha256("report|" + username + "|" + packId);
        if (ratings.findById(key) == null && !username.equals(p.getOwner())) {
            ForgeRating r = new ForgeRating();
            r.setId(key);
            r.setUsername(username);
            r.setPackId(packId);
            ratings.insert(r);
            p.setReports(p.getReports() + 1);
            if (p.getReports() >= REPORTS_TO_HIDE && !p.isOfficial()) {
                p.setStatus(HIDDEN);
            }
            packs.save(p);
            record("player:" + username, packId, "REPORT", reason == null ? "" : reason);
        }
        return summary(p);
    }

    // ---- files ------------------------------------------------------------

    /// Stores a model or an image. Files are addressed by their SHA-256, so the
    /// same file uploaded twice is stored once, and a pack names exactly the
    /// bytes it was published with. Every new file waits for a moderator
    /// before a pack using it is listed.
    @Transactional(rollbackFor = IOException.class)
    public AssetDto upload(String username, AssetUploadDto file) throws IOException {
        if (file == null || file.data() == null) {
            throw new ResponseStatusException(400, "Nothing uploaded");
        }
        String kind = AssetRef.IMAGE.equals(file.kind()) ? AssetRef.IMAGE : AssetRef.MODEL;
        byte[] bytes = Base64.decode(file.data());
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(400, "The file is not base64");
        }
        List<String> animations = new ArrayList<>();
        if (AssetRef.MODEL.equals(kind)) {
            GlbInspector.Report report = GlbInspector.inspect(bytes);
            if (!report.ok()) {
                throw new ResponseStatusException(400, "That model cannot be used: " + report.problem);
            }
            animations = report.animations;
        } else if (bytes.length > Validator.MAX_IMAGE_BYTES || !isImage(bytes)) {
            throw new ResponseStatusException(400, "An image is a PNG or JPEG of at most 1 MB");
        }
        String sha = Ids.hex(Crypto.sha256(bytes));
        ForgeAsset a = assets.findById(sha);
        if (a != null) {
            return asset(a, false);
        }
        Instant since = Instant.now().minusMillis(Clock.DAY);
        if (assets.countByOwnerAndCreatedAtGreaterThan(username, since) >= MAX_UPLOADS_PER_DAY) {
            throw new ResponseStatusException(429, "That is enough uploads for today");
        }
        a = new ForgeAsset();
        a.setSha256(sha);
        a.setOwner(username);
        a.setName(clip(file.name(), 80));
        a.setKind(kind);
        a.setBytes(bytes.length);
        a.setStatus(PENDING);
        a.setAnimations(clip(join(animations), 2000));
        a.setData(file.data());
        a.setCreatedAt(Instant.now());
        assets.insert(a);
        return asset(a, false);
    }

    /// A file's bytes. A rejected file is gone for everyone but its owner and
    /// the moderators.
    @Transactional(readOnly = true)
    public AssetDto download(String username, boolean admin, String sha) throws IOException {
        ForgeAsset a = assets.findById(sha);
        if (a == null || REJECTED.equals(a.getStatus()) && !admin && !username.equals(a.getOwner())) {
            throw new ResponseStatusException(404, "No such file");
        }
        return asset(a, true);
    }

    // ---- levels ----------------------------------------------------------

    /// Counts a finished level. The replay names the pack version it was
    /// played on; the server plays the player's own commands again against
    /// its own bot and scores what happens, whatever the app claimed.
    ///
    /// The seed is the app's choice, so a determined player could search for
    /// a lucky one offline. Stars are capped by what the level can pay, and
    /// community levels by a daily total, which is what keeps that harmless;
    /// the boards take only the seeds the app was handed ([Scores#fairSeed]).
    @Transactional(rollbackFor = IOException.class)
    public LevelOutcomeDto result(String username, String encoded, String city) throws IOException {
        Replay replay;
        try {
            replay = Replay.decode(encoded);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(400, "That is not a run replay");
        }
        if (replay.levelId().startsWith(CityRoute.ID_PREFIX)) {
            return cityResult(username, replay, city);
        }
        ContentRef ref = replay.levelId().indexOf('@') < 0 ? null : ContentRef.parse(replay.levelId());
        String packId = ref == null ? Samples.OFFICIAL_ID : ref.pack;
        String levelId = ref == null ? replay.levelId() : ref.level;
        ForgePack p = existing(packId);
        ContentPack pack = ref == null ? Samples.official() : parse(contentWithHash(p, ref.hash));
        LevelDef level = pack.level(levelId);
        if (level == null) {
            throw new ResponseStatusException(404, "No such level");
        }
        // tag::dash-forge-result[]
        Run played;
        try {
            played = LevelRun.verify(pack, levelId, replay);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(400, e.getMessage());
        }
        int won = played.stars(0);
        int score = played.runner(0).score();
        int paidBefore = paidStars(username, p.getId(), levelId);
        LevelStars row = record(username, p.getId(), levelId, won, score);
        int newStars = row.getPaidStars() - paidBefore;
        p.setPlays(p.getPlays() + 1);
        packs.save(p);

        int coins = OFFICIAL_STAR_COINS * newStars;
        int xp = OFFICIAL_STAR_XP * newStars;
        if (username.equals(p.getOwner())) {
            // Testing your own level is free, and pays nothing.
            coins = 0;
            xp = 0;
        } else if (!p.isOfficial()) {
            coins = Math.min(coins, COMMUNITY_LEVEL_COINS * newStars / 3);
            xp = Math.min(xp, COMMUNITY_LEVEL_XP * newStars / 3);
            coins = spendDailyAllowance(username, coins);
        }
        // end::dash-forge-result[]
        int rank = scores.submit(username, ref == null ? levelId : packId + "/" + levelId, replay, played);
        RewardDto reward = progression.levelReward(username, level.name, coins, xp);
        return new LevelOutcomeDto(p.getId(), levelId, won, score, row.getBest(), rank, reward);
    }

    /// A city run. The result carries the route, and the server builds the
    /// level from it ([CityRoute#level]) rather than trusting anything about
    /// it: the level's id is a hash of the route, so a route edited to an
    /// easier one names another level, with a board of its own. Coins and XP
    /// both come out of the community levels' daily allowance, since a new
    /// route is always a new level.
    private LevelOutcomeDto cityResult(String username, Replay replay, String spec) throws IOException {
        CityRoute route;
        try {
            route = CityRoute.decode(spec);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(400, "That is not a city route");
        }
        LevelDef level = route.level();
        if (!level.id.equals(replay.levelId())) {
            throw new ResponseStatusException(400, "That run is of another route");
        }
        Run played;
        try {
            played = LevelRun.verify(level, Content.catalog(), replay);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(400, e.getMessage());
        }
        int won = played.stars(0);
        int score = played.runner(0).score();
        int paidBefore = paidStars(username, CITY_PACK, level.id);
        LevelStars row = record(username, CITY_PACK, level.id, won, score);
        int newStars = row.getPaidStars() - paidBefore;
        int coins = spendDailyAllowance(username, COMMUNITY_LEVEL_COINS * newStars / 3);
        // XP follows the coins the allowance granted: any edit of a route is a
        // new level with unpaid stars, so an uncapped XP would farm forever.
        int xp = COMMUNITY_LEVEL_XP * coins / COMMUNITY_LEVEL_COINS;
        int rank = scores.submit(username, level.id, replay, played);
        RewardDto reward = progression.levelReward(username, level.name, coins, xp);
        return new LevelOutcomeDto(CITY_PACK, level.id, won, score, row.getBest(), rank, reward);
    }

    /// The pack id the stars of city runs are kept under; no pack has it.
    static final String CITY_PACK = "city";

    private int paidStars(String username, String packId, String levelId) throws IOException {
        LevelStars row = stars.findById(Ids.sha256(username + "|" + packId + "|" + levelId));
        return row == null ? 0 : row.getPaidStars();
    }

    /// Counts a play of a level and keeps the best stars and score; the
    /// stars not paid for before are marked paid.
    private LevelStars record(String username, String packId, String levelId, int won, int score)
            throws IOException {
        String key = Ids.sha256(username + "|" + packId + "|" + levelId);
        LevelStars row = stars.findById(key);
        if (row == null) {
            row = new LevelStars();
            row.setId(key);
            row.setUsername(username);
            row.setPackId(packId);
            row.setLevelId(levelId);
            stars.insert(row);
        }
        row.setPlays(row.getPlays() + 1);
        row.setStars(Math.max(row.getStars(), won));
        row.setBest(Math.max(row.getBest(), score));
        row.setPaidStars(Math.max(row.getPaidStars(), won));
        row.setUpdatedAt(Instant.now());
        stars.save(row);
        return row;
    }

    @Transactional(readOnly = true)
    public List<LevelStarsDto> stars(String username, String packId) throws IOException {
        List<LevelStarsDto> out = new ArrayList<>();
        for (LevelStars m : stars.of(username, packId, new PageRequest(0, Validator.MAX_LEVELS))) {
            out.add(new LevelStarsDto(m.getPackId(), m.getLevelId(), m.getStars(), m.getBest()));
        }
        return out;
    }

    /// The published content of a pack, for a race or a leaderboard; null when
    /// it is not public.
    @Transactional(readOnly = true)
    public ContentPack published(String packId) throws IOException {
        if (Samples.OFFICIAL_ID.equals(packId)) {
            return Samples.official();
        }
        ForgePack p = packs.findById(packId);
        if (p == null || !PUBLISHED.equals(p.getStatus())) {
            return null;
        }
        return parse(contentWithHash(p, p.getHash()));
    }

    /// One exact version of a pack, as a replay names it.
    @Transactional(readOnly = true)
    public ContentPack atHash(String packId, String hash) throws IOException {
        if (Samples.OFFICIAL_ID.equals(packId)) {
            return Samples.official();
        }
        return parse(contentWithHash(existing(packId), hash));
    }

    // ---- moderation ---------------------------------------------------------

    @Transactional(readOnly = true)
    public List<PackSummaryDto> queue() throws IOException {
        return summaries(packs.queue(0, HIDDEN, new PageRequest(0, PAGE)));
    }

    /// Files waiting for a moderator, without their bytes; `download` fetches
    /// one to look at.
    @Transactional(readOnly = true)
    public List<AssetDto> pendingAssets() throws IOException {
        List<AssetDto> out = new ArrayList<>();
        for (ForgeAsset a : assets.inStatus(PENDING, new PageRequest(0, PAGE))) {
            out.add(asset(a, false));
        }
        return out;
    }

    /// `hide`, `restore`, `official`, `unofficial` or `delete`.
    @Transactional(rollbackFor = IOException.class)
    public PackSummaryDto packAction(String admin, String id, String action, String reason) throws IOException {
        ForgePack p = existing(id);
        if ("hide".equals(action)) {
            p.setStatus(HIDDEN);
        } else if ("restore".equals(action)) {
            p.setStatus(p.getVersion() == 0 ? DRAFT : PUBLISHED);
            p.setReports(0);
        } else if ("official".equals(action) || "unofficial".equals(action)) {
            // The game's designer builds a campaign in the same editor any
            // player has, and promotes it here.
            p.setOfficial("official".equals(action));
        } else if ("delete".equals(action)) {
            p.setStatus(DELETED);
        } else {
            throw new ResponseStatusException(400, "Unknown action");
        }
        p.setUpdatedAt(Instant.now());
        packs.save(p);
        record(admin, id, "PACK_" + action, reason);
        return summary(p);
    }

    /// `approve` or `reject`. Either can list a pack that was waiting.
    @Transactional(rollbackFor = IOException.class)
    public AssetDto assetAction(String admin, String sha, String action, String reason) throws IOException {
        ForgeAsset a = assets.findById(sha);
        if (a == null) {
            throw new ResponseStatusException(404, "No such file");
        }
        if ("approve".equals(action)) {
            a.setStatus(APPROVED);
        } else if ("reject".equals(action)) {
            a.setStatus(REJECTED);
        } else {
            throw new ResponseStatusException(400, "Unknown action");
        }
        assets.save(a);
        for (ForgePack p : packs.awaitingAssets(true, new PageRequest(0, 500))) {
            ForgePackVersion v = versions.findById(versionId(p.getId(), p.getVersion()));
            if (v != null) {
                String waiting = waitingOn(parse(v.getContent()));
                p.setPendingAssets(waiting != null);
                packs.save(p);
            }
        }
        record(admin, a.getOwner(), "ASSET_" + action,
                clip(a.getName() + ": " + (reason == null ? "" : reason), 255));
        return asset(a, false);
    }

    // ---- helpers ------------------------------------------------------------

    private void publishVersion(ForgePack p, ContentPack pack, boolean insert) throws IOException {
        String missing = missingAsset(pack);
        if (missing != null) {
            throw new ResponseStatusException(400, "The file " + missing + " was never uploaded, or was rejected");
        }
        int version = p.getVersion() + 1;
        pack.version = version;
        String canonical = PackCodec.write(pack);
        String hash = ContentPack.hashOf(canonical);
        ForgePackVersion v = new ForgePackVersion();
        v.setId(versionId(p.getId(), version));
        v.setPackId(p.getId());
        v.setVersion(version);
        v.setHash(hash);
        v.setContent(canonical);
        v.setPublishedAt(Instant.now());
        versions.insert(v);
        p.setVersion(version);
        p.setHash(hash);
        p.setName(clip(pack.name, 60));
        p.setAuthor(clip(pack.author, 60));
        p.setDescription(clip(pack.description, 1200));
        p.setLevels(pack.levels.size());
        p.setDefs(pack.defs.size());
        p.setPendingAssets(waitingOn(pack) != null);
        if (!HIDDEN.equals(p.getStatus())) {
            p.setStatus(PUBLISHED);
        }
        p.setUpdatedAt(Instant.now());
        if (insert) {
            packs.insert(p);
        } else {
            packs.save(p);
        }
    }

    /// The first file the pack names that the server does not have, or a
    /// moderator rejected.
    private String missingAsset(ContentPack pack) throws IOException {
        for (AssetRef r : pack.assets) {
            ForgeAsset a = assets.findById(r.sha256);
            if (a == null || REJECTED.equals(a.getStatus())) {
                return r.name.isEmpty() ? r.sha256 : r.name;
            }
        }
        return null;
    }

    private String waitingOn(ContentPack pack) throws IOException {
        for (AssetRef r : pack.assets) {
            ForgeAsset a = assets.findById(r.sha256);
            if (a == null || !APPROVED.equals(a.getStatus())) {
                return r.sha256;
            }
        }
        return null;
    }

    /// The content of the version with this hash: a level is scored against
    /// exactly what was played, even after the author has published another.
    private String contentWithHash(ForgePack p, String hash) throws IOException {
        if (hash != null && hash.equals(p.getHash())) {
            ForgePackVersion v = versions.findById(versionId(p.getId(), p.getVersion()));
            if (v != null) {
                return v.getContent();
            }
        }
        for (ForgePackVersion v : versions.of(p.getId(), new PageRequest(0, 200))) {
            if (v.getHash().equals(hash)) {
                return v.getContent();
            }
        }
        throw new ResponseStatusException(404, "That version of the pack is gone");
    }

    /// Takes up to `coins` from what the player may still earn from community
    /// levels today.
    private int spendDailyAllowance(String username, int coins) throws IOException {
        if (coins <= 0) {
            return 0;
        }
        long day = Clock.dayOf(Clock.now());
        String key = Ids.sha256(username + "|" + day);
        ForgeEarning e = earnings.findById(key);
        if (e == null) {
            e = new ForgeEarning();
            e.setId(key);
            e.setUsername(username);
            e.setDay(day);
            earnings.insert(e);
        }
        int paid = Math.min(coins, Math.max(0, DAILY_COMMUNITY_COINS - e.getCoins()));
        e.setCoins(e.getCoins() + paid);
        earnings.save(e);
        return paid;
    }

    private void fillDraft(ForgePack p, ContentPack pack, String username) throws IOException {
        // The byline is the player's race name, never what the file says.
        Profile who = profiles.findById(username);
        pack.author = who == null ? username : who.getDisplayName();
        p.setDraft(PackCodec.write(pack));
        p.setName(clip(pack.name, 60));
        if (p.getVersion() == 0) {
            p.setAuthor(clip(pack.author, 60));
            p.setDescription(clip(pack.description, 1200));
            p.setLevels(pack.levels.size());
            p.setDefs(pack.defs.size());
        }
        p.setUpdatedAt(Instant.now());
    }

    private static ContentPack parse(String content) {
        if (content == null || content.length() > Json.MAX_CHARS) {
            throw new ResponseStatusException(400, "A pack is at most " + (Json.MAX_CHARS / 1024) + " KB");
        }
        try {
            return PackCodec.read(content);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(400, "That is not a content pack: " + e.getMessage());
        }
    }

    private ForgePack existing(String id) throws IOException {
        ForgePack p = id == null ? null : packs.findById(id);
        if (p == null) {
            throw new ResponseStatusException(404, "No such pack");
        }
        return p;
    }

    private ForgePack owned(String username, String id) throws IOException {
        ForgePack p = existing(id);
        if (!username.equals(p.getOwner()) || DELETED.equals(p.getStatus())) {
            throw new ResponseStatusException(404, "No such pack");
        }
        return p;
    }

    /// A pack the caller may open: deleted ones are gone, hidden ones are for
    /// the owner and the moderators.
    private ForgePack visible(String username, boolean admin, String id) throws IOException {
        ForgePack p = existing(id);
        if (admin) {
            return p;
        }
        if (DELETED.equals(p.getStatus()) || HIDDEN.equals(p.getStatus()) && !username.equals(p.getOwner())) {
            throw new ResponseStatusException(404, "No such pack");
        }
        return p;
    }

    private ForgePack listed(String id) throws IOException {
        ForgePack p = existing(id);
        if (!PUBLISHED.equals(p.getStatus()) && !HIDDEN.equals(p.getStatus())) {
            throw new ResponseStatusException(404, "No such pack");
        }
        return p;
    }

    private void record(String admin, String target, String action, String reason) throws IOException {
        AdminAction a = new AdminAction();
        a.setId(Ids.next());
        a.setAdmin(clip(admin, 190));
        a.setUsername(clip("pack:" + target, 190));
        a.setAction(clip(action, 24));
        a.setReason(clip(reason == null ? "" : reason, 255));
        a.setCreatedAt(Instant.now());
        audit.insert(a);
    }

    private static String versionId(String id, int version) {
        return id + ":" + version;
    }

    private static List<PackSummaryDto> summaries(List<ForgePack> rows) {
        List<PackSummaryDto> out = new ArrayList<>();
        for (ForgePack p : rows) {
            out.add(summary(p));
        }
        return out;
    }

    static PackSummaryDto summary(ForgePack p) {
        int rating = p.getRatingCount() == 0 ? 0 : (p.getRatingSum() * 10 + p.getRatingCount() / 2) / p.getRatingCount();
        return new PackSummaryDto(p.getId(), p.getName(), p.getAuthor(), p.getOwner(), p.getDescription(),
                p.getVersion(), p.getHash(), p.isOfficial(), p.getStatus(), p.getPlays(), rating,
                p.getRatingCount(), p.getLevels(), p.getDefs(), p.isPendingAssets(), p.getUpdatedAt().toEpochMilli());
    }

    private static AssetDto asset(ForgeAsset a, boolean withData) {
        List<String> anims = Lists.split(a.getAnimations());
        return new AssetDto(a.getSha256(), a.getName(), a.getKind(), a.getBytes(), a.getStatus(), anims,
                withData ? a.getData() : null);
    }

    private static boolean isImage(byte[] b) {
        boolean png = b.length > 8 && (b[0] & 0xff) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
        boolean jpeg = b.length > 3 && (b[0] & 0xff) == 0xff && (b[1] & 0xff) == 0xd8;
        return png || jpeg;
    }

    private static String join(List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (String s : parts) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(s.replace(',', '_'));
        }
        return sb.toString();
    }

    private static String clip(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
