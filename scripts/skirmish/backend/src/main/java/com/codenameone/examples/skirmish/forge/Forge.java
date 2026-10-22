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
package com.codenameone.examples.skirmish.forge;

import com.codename1.backend.Base64;
import com.codename1.backend.Crypto;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.repository.PageRequest;
import com.codenameone.examples.skirmish.Clock;
import com.codenameone.examples.skirmish.Ids;
import com.codenameone.examples.skirmish.api.AssetDto;
import com.codenameone.examples.skirmish.api.AssetUploadDto;
import com.codenameone.examples.skirmish.api.MissionOutcomeDto;
import com.codenameone.examples.skirmish.api.MissionStarsDto;
import com.codenameone.examples.skirmish.api.PackDto;
import com.codenameone.examples.skirmish.api.PackSummaryDto;
import com.codenameone.examples.skirmish.api.RewardDto;
import com.codenameone.examples.skirmish.domain.AdminAction;
import com.codenameone.examples.skirmish.domain.ForgeAsset;
import com.codenameone.examples.skirmish.domain.ForgeEarning;
import com.codenameone.examples.skirmish.domain.ForgePack;
import com.codenameone.examples.skirmish.domain.ForgePackVersion;
import com.codenameone.examples.skirmish.domain.ForgeRating;
import com.codenameone.examples.skirmish.domain.MissionStars;
import com.codenameone.examples.skirmish.domain.Profile;
import com.codenameone.examples.skirmish.progress.Progression;
import com.codenameone.examples.skirmish.repository.AdminActionRepository;
import com.codenameone.examples.skirmish.repository.ForgeAssetRepository;
import com.codenameone.examples.skirmish.repository.ForgeEarningRepository;
import com.codenameone.examples.skirmish.repository.ForgePackRepository;
import com.codenameone.examples.skirmish.repository.ForgePackVersionRepository;
import com.codenameone.examples.skirmish.repository.ForgeRatingRepository;
import com.codenameone.examples.skirmish.repository.MissionStarsRepository;
import com.codenameone.examples.skirmish.repository.ProfileRepository;
import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.rules.Replay;
import com.codenameone.examples.skirmish.rules.forge.AssetRef;
import com.codenameone.examples.skirmish.rules.forge.ContentPack;
import com.codenameone.examples.skirmish.rules.forge.ContentRef;
import com.codenameone.examples.skirmish.rules.forge.GlbInspector;
import com.codenameone.examples.skirmish.rules.forge.Json;
import com.codenameone.examples.skirmish.rules.forge.MissionDef;
import com.codenameone.examples.skirmish.rules.forge.MissionRun;
import com.codenameone.examples.skirmish.rules.forge.PackCodec;
import com.codenameone.examples.skirmish.rules.forge.Samples;
import com.codenameone.examples.skirmish.rules.forge.Validator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// The Forge on the server: players' content packs from draft to published
/// version, the files they upload, ratings and reports, and the stars and
/// rewards their missions pay.
///
/// Nothing a client sends is trusted. A draft is only parsed; publishing runs
/// the same `Validator` the editor runs, and a mission result is a replay the
/// server plays again with its own bot (`MissionRun.verify`) before it counts a
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
    /// unofficial mission pays at most this much, and all of them together at
    /// most `DAILY_COMMUNITY_COINS` a day. Official missions pay what their
    /// designer set.
    public static final int COMMUNITY_MISSION_COINS = 30;
    public static final int COMMUNITY_MISSION_XP = 20;
    public static final int DAILY_COMMUNITY_COINS = 150;

    private static final int PAGE = 50;

    private final ForgePackRepository packs;
    private final ForgePackVersionRepository versions;
    private final ForgeAssetRepository assets;
    private final MissionStarsRepository stars;
    private final ForgeRatingRepository ratings;
    private final ForgeEarningRepository earnings;
    private final ProfileRepository profiles;
    private final AdminActionRepository audit;
    private final Progression progression;

    public Forge(ForgePackRepository packs, ForgePackVersionRepository versions, ForgeAssetRepository assets,
            MissionStarsRepository stars, ForgeRatingRepository ratings, ForgeEarningRepository earnings,
            ProfileRepository profiles, AdminActionRepository audit, Progression progression) {
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
            p.setCreatedAt(Clock.now());
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
    /// validated: a half-built mission is a normal thing to save.
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
            p.setCreatedAt(Clock.now());
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
        p.setUpdatedAt(Clock.now());
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
        long since = Clock.now() - Clock.DAY;
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
        a.setCreatedAt(Clock.now());
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

    // ---- missions ----------------------------------------------------------

    /// Counts a finished mission. The replay names the pack version it was
    /// played on; the server plays the player's own commands again against
    /// its own bot and scores what happens, whatever the app claimed.
    ///
    /// The seed is the app's choice, so a determined player could search for
    /// a lucky one offline. Stars are capped by what the mission can pay, and
    /// community missions by a daily total, which is what keeps that harmless.
    @Transactional(rollbackFor = IOException.class)
    public MissionOutcomeDto result(String username, String encoded) throws IOException {
        Replay replay;
        ContentRef ref;
        try {
            replay = Replay.decode(encoded);
            ref = ContentRef.parse(replay.content());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(400, "That is not a mission replay");
        }
        if (ref == null || ref.mission == null) {
            throw new ResponseStatusException(400, "That replay is not of a mission");
        }
        ForgePack p = existing(ref.pack);
        ContentPack pack = parse(contentWithHash(p, ref.hash));
        MissionDef mission = pack.mission(ref.mission);
        if (mission == null) {
            throw new ResponseStatusException(404, "No such mission");
        }
        Match played;
        try {
            played = MissionRun.verify(pack, ref.mission, replay);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(400, e.getMessage());
        }
        int won = mission.stars(played);

        String key = Ids.sha256(username + "|" + p.getId() + "|" + ref.mission);
        MissionStars row = stars.findById(key);
        if (row == null) {
            row = new MissionStars();
            row.setId(key);
            row.setUsername(username);
            row.setPackId(p.getId());
            row.setMissionId(ref.mission);
            stars.insert(row);
        }
        row.setPlays(row.getPlays() + 1);
        row.setStars(Math.max(row.getStars(), won));
        int newStars = Math.max(0, won - row.getPaidStars());
        row.setPaidStars(row.getPaidStars() + newStars);
        row.setUpdatedAt(Clock.now());
        stars.save(row);
        p.setPlays(p.getPlays() + 1);
        packs.save(p);

        int coins = mission.rewardCoins * newStars / 3;
        int xp = mission.rewardXp * newStars / 3;
        if (username.equals(p.getOwner())) {
            // Testing your own mission is free, and pays nothing.
            coins = 0;
            xp = 0;
        } else if (!p.isOfficial()) {
            coins = Math.min(coins, COMMUNITY_MISSION_COINS * newStars / 3);
            xp = Math.min(xp, COMMUNITY_MISSION_XP * newStars / 3);
            coins = spendDailyAllowance(username, coins);
        }
        RewardDto reward = progression.missionReward(username, mission.name, coins, xp);
        return new MissionOutcomeDto(p.getId(), ref.mission, won, row.getStars(), reward);
    }

    @Transactional(readOnly = true)
    public List<MissionStarsDto> stars(String username, String packId) throws IOException {
        List<MissionStarsDto> out = new ArrayList<>();
        for (MissionStars m : stars.of(username, packId, new PageRequest(0, Validator.MAX_MISSIONS))) {
            out.add(new MissionStarsDto(m.getPackId(), m.getMissionId(), m.getStars()));
        }
        return out;
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
        p.setUpdatedAt(Clock.now());
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
        v.setPublishedAt(Clock.now());
        versions.insert(v);
        p.setVersion(version);
        p.setHash(hash);
        p.setName(clip(pack.name, 60));
        p.setAuthor(clip(pack.author, 60));
        p.setDescription(clip(pack.description, 1200));
        p.setMissions(pack.missions.size());
        p.setCards(pack.cards.size());
        p.setPendingAssets(waitingOn(pack) != null);
        if (!HIDDEN.equals(p.getStatus())) {
            p.setStatus(PUBLISHED);
        }
        p.setUpdatedAt(Clock.now());
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

    /// The content of the version with this hash: a mission is scored against
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
    /// missions today.
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
        // The byline is the player's battle name, never what the file says.
        Profile who = profiles.findById(username);
        pack.author = who == null ? username : who.getDisplayName();
        p.setDraft(PackCodec.write(pack));
        p.setName(clip(pack.name, 60));
        if (p.getVersion() == 0) {
            p.setAuthor(clip(pack.author, 60));
            p.setDescription(clip(pack.description, 1200));
            p.setMissions(pack.missions.size());
            p.setCards(pack.cards.size());
        }
        p.setUpdatedAt(Clock.now());
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
        a.setCreatedAt(Clock.now());
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
                p.getRatingCount(), p.getMissions(), p.getCards(), p.isPendingAssets(), p.getUpdatedAt());
    }

    private static AssetDto asset(ForgeAsset a, boolean withData) {
        List<String> anims = a.getAnimations().isEmpty()
                ? new ArrayList<String>() : new ArrayList<>(Arrays.asList(a.getAnimations().split(",")));
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
