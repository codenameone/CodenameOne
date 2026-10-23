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
package com.codenameone.examples.reel.playback;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.reel.Ids;
import com.codenameone.examples.reel.Kinds;
import com.codenameone.examples.reel.Requests;
import com.codenameone.examples.reel.Roles;
import com.codenameone.examples.reel.Rows;
import com.codenameone.examples.reel.account.Devices;
import com.codenameone.examples.reel.account.Households;
import com.codenameone.examples.reel.ads.Ads;
import com.codenameone.examples.reel.api.AdBreakDto;
import com.codenameone.examples.reel.api.AdDto;
import com.codenameone.examples.reel.api.ChapterDto;
import com.codenameone.examples.reel.api.Features;
import com.codenameone.examples.reel.api.HeartbeatDto;
import com.codenameone.examples.reel.api.HeartbeatReplyDto;
import com.codenameone.examples.reel.api.InteractionDto;
import com.codenameone.examples.reel.api.PlaybackDto;
import com.codenameone.examples.reel.api.PlaybackRequestDto;
import com.codenameone.examples.reel.api.SessionDto;
import com.codenameone.examples.reel.api.StopDto;
import com.codenameone.examples.reel.api.SubtitleDto;
import com.codenameone.examples.reel.billing.Entitlements;
import com.codenameone.examples.reel.domain.AdCampaign;
import com.codenameone.examples.reel.domain.Chapter;
import com.codenameone.examples.reel.domain.Download;
import com.codenameone.examples.reel.domain.Household;
import com.codenameone.examples.reel.domain.Interaction;
import com.codenameone.examples.reel.domain.Playback;
import com.codenameone.examples.reel.domain.Progress;
import com.codenameone.examples.reel.domain.SharingAlert;
import com.codenameone.examples.reel.domain.Title;
import com.codenameone.examples.reel.domain.Video;
import com.codenameone.examples.reel.repository.ChapterRepository;
import com.codenameone.examples.reel.repository.DownloadRepository;
import com.codenameone.examples.reel.repository.InteractionRepository;
import com.codenameone.examples.reel.repository.PlaybackRepository;
import com.codenameone.examples.reel.repository.ProgressRepository;
import com.codenameone.examples.reel.repository.SharingAlertRepository;
import com.codenameone.examples.reel.repository.TitleRepository;
import com.codenameone.examples.reel.repository.VideoRepository;
import com.codenameone.examples.reel.settings.Settings;
import com.codename1.backend.Crypto;
import com.codename1.backend.Base64Url;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Streaming sessions: who is watching what, on which device, from where.
///
/// A session is opened by [#start], which checks everything that decides
/// whether a title may play -- the profile's rating, the plan or rental, the
/// device, and how many streams the plan allows at once -- and answers the
/// manifest and license URLs, each carrying a token signed for this session
/// alone. The player then reports every [#HEARTBEAT_SECONDS]; a session whose
/// heartbeats stop is swept, so a crashed app does not hold a stream slot.
///
/// Ending a session (signing a device out, an admin ending it, a suspended
/// account) is noticed at the next heartbeat, which answers `ok=false` and the
/// app stops. The media token cannot be recalled, but it lives for one
/// session's length and names one video, so a leaked URL plays only what the
/// session already could.
@Component
public class Playbacks {
    public static final int HEARTBEAT_SECONDS = 30;
    /// A session not heard from for this long is ended by the sweep.
    public static final long STALE_MILLIS = 3L * HEARTBEAT_SECONDS * 1000L;
    /// Distinct addresses (or countries) in a day that raise a sharing alert.
    public static final int SHARING_IPS = 6;
    public static final int SHARING_COUNTRIES = 3;
    private static final long DAY = 24L * 3600L * 1000L;

    private final PlaybackRepository sessions;
    private final TitleRepository titles;
    private final VideoRepository videos;
    private final ProgressRepository progress;
    private final ChapterRepository chapters;
    private final InteractionRepository interactions;
    private final SharingAlertRepository alerts;
    private final DownloadRepository downloads;
    private final Households households;
    private final Devices devices;
    private final Entitlements entitlements;
    private final Settings settings;
    private final Licenses licenses;
    private final MediaTokens tokens;
    private final Ads ads;

    public Playbacks(PlaybackRepository sessions, TitleRepository titles, VideoRepository videos,
            ProgressRepository progress, ChapterRepository chapters, InteractionRepository interactions,
            SharingAlertRepository alerts, DownloadRepository downloads, Households households, Devices devices, Entitlements entitlements,
            Settings settings, Licenses licenses, MediaTokens tokens, Ads ads) {
        this.sessions = sessions;
        this.titles = titles;
        this.videos = videos;
        this.progress = progress;
        this.chapters = chapters;
        this.interactions = interactions;
        this.alerts = alerts;
        this.downloads = downloads;
        this.households = households;
        this.devices = devices;
        this.entitlements = entitlements;
        this.settings = settings;
        this.licenses = licenses;
        this.tokens = tokens;
        this.ads = ads;
    }

    /// Opens a session. Throws 403 for a rating the profile may not watch,
    /// 402 for a title the plan or rentals do not cover, 409 when every
    /// stream the plan allows is already in use, and 404 for anything that is
    /// not published and ready.
    @Transactional(rollbackFor = IOException.class)
    public PlaybackDto start(String username, PlaybackRequestDto request) throws IOException {
        if (request == null || request.titleId() == null) {
            throw new ResponseStatusException(400, "Choose something to watch");
        }
        boolean staff = Roles.isStaff();
        Title title = titles.findById(request.titleId());
        if (title == null || (!Kinds.PUBLISHED.equals(title.getStatus()) && !staff
                && !username.equals(title.getCreator()))) {
            throw new ResponseStatusException(404, "No such title");
        }
        Video video = video(title, request.episodeId());
        Household profile = households.resolve(username, request.profileId());
        if (!Households.allows(profile, title.getRating())) {
            throw new ResponseStatusException(403, "This profile cannot watch " + title.getRating() + " titles");
        }
        boolean own = staff || username.equals(title.getCreator());
        Entitlements.Access access = entitlements.access(username, title, own);
        if (!access.allowed) {
            throw new ResponseStatusException(access.status, access.reason);
        }
        // The header every request of a signed-in app carries, else the field
        // of the request: a web player that cannot set headers names itself there.
        String deviceId = Requests.deviceId();
        if (deviceId.length() == 0 && request.deviceId() != null) {
            String named = request.deviceId().trim();
            deviceId = named.length() > 64 ? named.substring(0, 64) : named;
        }
        if (deviceId.length() > 0 && !devices.usable(username, deviceId)) {
            throw new ResponseStatusException(401, "This device was signed out");
        }
        long now = System.currentTimeMillis();
        Entitlements.Allowance allowance = entitlements.allowance(username);
        List<Playback> live = sessions.findByUsernameAndEndedAt(username, 0L);
        int streams = 0;
        for (int iter = 0; iter < live.size(); iter++) {
            Playback p = live.get(iter);
            // The same device starting again replaces its own stream (the
            // viewer picked another episode) rather than counting twice.
            if (deviceId.length() > 0 && deviceId.equals(p.getDeviceId())) {
                end(p, "REPLACED", now);
            } else if (p.getLastHeartbeatAt() < now - STALE_MILLIS) {
                end(p, "STALE", now);
            } else {
                streams++;
            }
        }
        int maxStreams = own ? Integer.MAX_VALUE : Math.max(1, allowance.maxStreams);
        if (streams >= maxStreams) {
            throw new ResponseStatusException(409, "Your plan allows " + maxStreams
                    + (maxStreams == 1 ? " stream" : " streams") + " at once; stop one to watch here");
        }
        int maxHeight = Entitlements.maxHeight(own ? "2160" : allowance.maxQuality);
        if (request.maxQuality() != null) {
            maxHeight = Math.min(maxHeight, Entitlements.maxHeight(request.maxQuality()));
        }
        List<String> wanted = request.schemes() == null ? new ArrayList<String>() : request.schemes();
        String scheme = licenses.choose(wanted, Rows.split(video.getSchemes()));
        if (!Licenses.AES_128.equals(scheme) && !Licenses.FAIRPLAY.equals(scheme)
                && Licenses.cencHeight(video.getRenditions()) > maxHeight) {
            // cenc.mp4 is a single quality above this viewer's cap; the HLS
            // ladder can be filtered down to it.
            scheme = Licenses.AES_128;
        }

        Playback session = new Playback();
        session.setId(Ids.next());
        session.setUsername(username);
        session.setProfileId(profile.getId());
        session.setTitleId(title.getId());
        session.setVideoId(video.getId());
        session.setDeviceId(deviceId);
        session.setDeviceName(Requests.deviceName());
        session.setIp(Requests.ip());
        session.setCountry(Requests.country());
        session.setScheme(scheme);
        session.setStartedAt(now);
        session.setLastHeartbeatAt(now);
        int resume = resume(profile.getId(), video);
        session.setPositionSeconds(resume);
        long ttl = MediaTokens.STREAM_SECONDS;
        String mediaToken = tokens.media("s", session.getId(), video.getId(), maxHeight, ttl);
        session.setTokenHash(hash(mediaToken));
        sessions.insert(session);
        detectSharing(username, now);

        String base = "/media/" + mediaToken + "/" + video.getId() + "/";
        boolean hls = Licenses.AES_128.equals(scheme) || Licenses.FAIRPLAY.equals(scheme);
        String licenseUrl = null;
        String certificateUrl = null;
        String licenseToken = null;
        if (Licenses.CLEARKEY.equals(scheme)) {
            licenseUrl = "/license/clearkey";
        } else if (Licenses.WIDEVINE.equals(scheme) || Licenses.FAIRPLAY.equals(scheme)) {
            LicenseProvider provider = licenses.provider(scheme);
            licenseUrl = "/license/" + (Licenses.WIDEVINE.equals(scheme) ? "widevine" : "fairplay");
            certificateUrl = provider == null ? null : provider.certificateUrl();
        }
        if (licenseUrl != null) {
            licenseToken = tokens.license("s", session.getId(), video.getId(), ttl);
        }
        boolean showAds = access.ads && settings.enabled(Features.ADS) && !Kinds.LESSON.equals(video.getRole());
        List<AdBreakDto> breaks = showAds ? ads.breaks(session.getId(), video.getDurationSeconds(), resume)
                : new ArrayList<AdBreakDto>();
        String watermark = settings.enabled(Features.WATERMARK)
                ? username + " " + session.getId().substring(0, Math.min(8, session.getId().length())) : null;
        return new PlaybackDto(session.getId(), base + (hls ? "master.m3u8" : "cenc.mp4"), scheme, licenseUrl,
                certificateUrl, video.getId(), licenseToken, resume, HEARTBEAT_SECONDS, breaks,
                new ArrayList<SubtitleDto>(), chapters(video.getId()),
                settings.enabled(Features.INTERACTIVE) ? interactions(video.getId()) : new ArrayList<InteractionDto>(),
                watermark, now + ttl * 1000L);
    }

    @Transactional(rollbackFor = IOException.class)
    public HeartbeatReplyDto heartbeat(String username, HeartbeatDto beat) throws IOException {
        Playback session = owned(username, beat == null ? null : beat.sessionId());
        long now = System.currentTimeMillis();
        long expires = session.getStartedAt() + MediaTokens.STREAM_SECONDS * 1000L;
        if (session.getEndedAt() > 0) {
            return new HeartbeatReplyDto(false, reason(session.getEndedReason()), expires);
        }
        session.setLastHeartbeatAt(now);
        session.setPositionSeconds(Math.max(0, beat.positionSeconds()));
        sessions.save(session);
        remember(session, beat.positionSeconds(), false, now);
        return new HeartbeatReplyDto(true, "", expires);
    }

    @Transactional(rollbackFor = IOException.class)
    public void stop(String username, StopDto stop) throws IOException {
        Playback session = owned(username, stop == null ? null : stop.sessionId());
        long now = System.currentTimeMillis();
        boolean first = session.getEndedAt() == 0;
        if (first) {
            session.setPositionSeconds(Math.max(0, stop.positionSeconds()));
            end(session, stop.completed() ? "COMPLETED" : "STOPPED", now);
        }
        remember(session, stop.positionSeconds(), stop.completed(), now);
        if (first && stop.completed()) {
            Title title = titles.findById(session.getTitleId());
            if (title != null) {
                title.setViewCount(title.getViewCount() + 1);
                titles.save(title);
            }
        }
    }

    /// The ads of one break of a session. A creative that is a packaged Reel
    /// video gets a media token of its own, tied to the same session.
    @Transactional(readOnly = true)
    public List<AdDto> adsFor(String username, String sessionId, String breakId) {
        Playback session = owned(username, sessionId);
        List<AdDto> out = new ArrayList<AdDto>();
        if (breakId == null || !breakId.startsWith(session.getId() + "~") || session.getEndedAt() > 0
                || !settings.enabled(Features.ADS)) {
            return out;
        }
        Title title = titles.findById(session.getTitleId());
        AdCampaign campaign = ads.choose(title == null ? null : Rows.split(title.getGenres()), breakId);
        if (campaign == null) {
            return out;
        }
        String creative = campaign.getCreativeUrl();
        String media = creative;
        if (creative.startsWith(Ads.VIDEO)) {
            String video = creative.substring(Ads.VIDEO.length());
            media = "/media/" + tokens.media("s", session.getId(), video, 720, 3600L) + "/" + video + "/master.m3u8";
        }
        out.add(Ads.dto(campaign, media, breakId));
        return out;
    }

    /// Throws 404 unless the session is the caller's; ad events may arrive
    /// just after a session ended, so an ended one still counts.
    @Transactional(readOnly = true)
    public void liveOrRecent(String username, String sessionId) {
        owned(username, sessionId);
    }

    /// Ends what a device is streaming, when the device is signed out.
    @Transactional
    public void endForDevice(String username, String deviceId) {
        List<Playback> live = sessions.findByUsernameAndEndedAt(username, 0L);
        long now = System.currentTimeMillis();
        for (int iter = 0; iter < live.size(); iter++) {
            if (live.get(iter).getDeviceId().equals(deviceId)) {
                end(live.get(iter), "SIGNED_OUT", now);
            }
        }
    }

    /// Ends every stream of an account (suspension, sharing enforcement) and
    /// withdraws its offline licenses: a download is a stream that outlives
    /// the session, and must not outlive the reason the sessions ended.
    @Transactional
    public int endAll(String username, String reason) {
        List<Playback> live = sessions.findByUsernameAndEndedAt(username, 0L);
        long now = System.currentTimeMillis();
        for (int iter = 0; iter < live.size(); iter++) {
            end(live.get(iter), reason, now);
        }
        downloads.deleteByUsername(username);
        return live.size();
    }

    /// Ends one session, from the admin console.
    @Transactional(rollbackFor = IOException.class)
    public void endSession(String id) throws IOException {
        Playback p = sessions.findById(id);
        if (p == null) {
            throw new ResponseStatusException(404, "No such session");
        }
        if (p.getEndedAt() == 0) {
            end(p, "ENDED_BY_ADMIN", System.currentTimeMillis());
        }
    }

    @Transactional(readOnly = true)
    public List<SessionDto> liveFor(String username) {
        return dtos(sessions.findByUsernameAndEndedAt(username, 0L));
    }

    @Transactional(readOnly = true)
    public List<SessionDto> allLive() {
        return dtos(sessions.findByEndedAtOrderByStartedAtDesc(0L));
    }

    @Transactional(readOnly = true)
    public int liveCount() {
        return (int) sessions.countByEndedAt(0L);
    }

    /// Whether a token still stands for something live: a session that has
    /// not ended, or a download that was not deleted and whose license has
    /// not run out. The origin asks this for playlists and keys only (segments
    /// are too many to look each up), and the license endpoints for every
    /// license, so ending a session or deleting a download stops the next
    /// playlist refresh and the next license renewal.
    @Transactional(readOnly = true)
    public boolean isLive(MediaTokens.Grant grant) {
        if (grant.isDownload()) {
            Download d = downloads.findById(grant.owner);
            return d != null && d.getLicenseExpiresAt() > System.currentTimeMillis()
                    && devices.usable(d.getUsername(), d.getDeviceId());
        }
        Playback p = sessions.findById(grant.owner);
        return p != null && p.getEndedAt() == 0;
    }

    /// Ends sessions whose app stopped reporting.
    @Scheduled(fixedDelay = 60000, initialDelay = 60000, lock = "reel-sweep-sessions")
    @Transactional
    public void sweep() {
        long now = System.currentTimeMillis();
        List<Playback> stale = sessions.findByEndedAtAndLastHeartbeatAtLessThan(0L, now - STALE_MILLIS);
        for (int iter = 0; iter < stale.size(); iter++) {
            end(stale.get(iter), "STALE", now);
        }
    }

    // ------------------------------------------------------------ helpers

    private Video video(Title title, String episodeId) {
        if (episodeId != null && episodeId.length() > 0) {
            Video v = videos.findById(episodeId);
            if (v == null || !v.getTitleId().equals(title.getId()) || !Kinds.READY.equals(v.getStatus())) {
                throw new ResponseStatusException(404, "No such episode");
            }
            return v;
        }
        List<Video> all = videos.findByTitleIdOrderBySeasonAscNumberAsc(title.getId());
        for (int iter = 0; iter < all.size(); iter++) {
            Video v = all.get(iter);
            if (!Kinds.TRAILER.equals(v.getRole()) && Kinds.READY.equals(v.getStatus())) {
                return v;
            }
        }
        throw new ResponseStatusException(404, "This title has nothing ready to play yet");
    }

    private int resume(String profileId, Video video) {
        List<Progress> rows = progress.findByProfileIdAndVideoId(profileId, video.getId());
        if (rows.isEmpty()) {
            return 0;
        }
        Progress p = rows.get(0);
        // A finished video, or one within its last minute, starts over.
        if (p.isCompleted() || p.getPositionSeconds() >= video.getDurationSeconds() - 60) {
            return 0;
        }
        return Math.max(0, p.getPositionSeconds());
    }

    private void remember(Playback session, int position, boolean completed, long now) throws IOException {
        Video video = videos.findById(session.getVideoId());
        int duration = video == null ? 0 : video.getDurationSeconds();
        int at = Math.max(0, duration > 0 ? Math.min(position, duration) : position);
        List<Progress> rows = progress.findByProfileIdAndVideoId(session.getProfileId(), session.getVideoId());
        Progress p;
        boolean fresh = rows.isEmpty();
        if (fresh) {
            p = new Progress();
            p.setId(Ids.next());
            p.setProfileId(session.getProfileId());
            p.setUsername(session.getUsername());
            p.setTitleId(session.getTitleId());
            p.setVideoId(session.getVideoId());
        } else {
            p = rows.get(0);
        }
        p.setPositionSeconds(at);
        p.setDurationSeconds(duration);
        // Completion sticks: rewatching the start of a finished lesson must not
        // take its tick away.
        p.setCompleted(p.isCompleted() || completed || (duration > 0 && at >= duration - 30));
        p.setUpdatedAt(now);
        if (fresh) {
            progress.insert(p);
        } else {
            progress.save(p);
        }
    }

    private void end(Playback p, String reason, long now) {
        p.setEndedAt(now);
        p.setEndedReason(reason);
        sessions.save(p);
    }

    private Playback owned(String username, String sessionId) {
        Playback p = sessionId == null ? null : sessions.findById(sessionId);
        if (p == null || !p.getUsername().equals(username)) {
            throw new ResponseStatusException(404, "No such session");
        }
        return p;
    }

    private static String reason(String code) {
        if ("SIGNED_OUT".equals(code)) {
            return "This device was signed out";
        }
        if ("ENDED_BY_ADMIN".equals(code) || "SUSPENDED".equals(code)) {
            return "Playback was stopped by Reel support";
        }
        if ("REPLACED".equals(code)) {
            return "Playback moved to another title on this device";
        }
        return "This session has ended";
    }

    /// Raises a sharing alert when an account streamed from more addresses or
    /// countries in a day than a household plausibly has. The alert is for a
    /// person to look at; nothing is blocked automatically.
    private void detectSharing(String username, long now) throws IOException {
        if (!settings.enabled(Features.SHARING_DETECTION)) {
            return;
        }
        List<Playback> day = sessions.findByUsernameAndStartedAtGreaterThan(username, now - DAY);
        Set<String> ips = new HashSet<String>();
        Set<String> countries = new HashSet<String>();
        for (int iter = 0; iter < day.size(); iter++) {
            Playback p = day.get(iter);
            if (p.getIp().length() > 0) {
                ips.add(p.getIp());
            }
            if (p.getCountry().length() > 0) {
                countries.add(p.getCountry());
            }
        }
        if (ips.size() < SHARING_IPS && countries.size() < SHARING_COUNTRIES) {
            return;
        }
        if (!alerts.findByUsernameAndStatus(username, Kinds.OPEN).isEmpty()) {
            return;
        }
        SharingAlert alert = new SharingAlert();
        alert.setId(Ids.next());
        alert.setUsername(username);
        alert.setKind(countries.size() >= SHARING_COUNTRIES ? "COUNTRIES" : "ADDRESSES");
        alert.setDetail(ips.size() + " addresses and " + countries.size() + " countries in 24 hours");
        alert.setDistinctIps(ips.size());
        alert.setDistinctCountries(countries.size());
        alert.setStatus(Kinds.OPEN);
        alert.setCreatedAt(now);
        alerts.insert(alert);
    }

    private List<ChapterDto> chapters(String videoId) {
        List<Chapter> rows = chapters.findByVideoIdOrderByStartSecondsAsc(videoId);
        List<ChapterDto> out = new ArrayList<ChapterDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Chapter c = rows.get(iter);
            out.add(new ChapterDto(c.getId(), c.getStartSeconds(), c.getTitle()));
        }
        return out;
    }

    /// The interactions as the player sees them: never the answers, which
    /// the server checks.
    private List<InteractionDto> interactions(String videoId) {
        List<Interaction> rows = interactions.findByVideoIdOrderByAtSecondsAsc(videoId);
        List<InteractionDto> out = new ArrayList<InteractionDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(interaction(rows.get(iter), 0));
        }
        return out;
    }

    public static InteractionDto interaction(Interaction i, int passPercent) {
        return new InteractionDto(i.getId(), i.getKind(), i.getAtSeconds(), i.getPrompt(), Rows.splitLines(i.getOptions()),
                i.isRequired(), passPercent, Rows.split(i.getBranchTargets()));
    }

    private List<SessionDto> dtos(List<Playback> rows) {
        List<SessionDto> out = new ArrayList<SessionDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Playback p = rows.get(iter);
            Title t = titles.findById(p.getTitleId());
            Household h = p.getProfileId().length() == 0 ? null : households.find(p.getProfileId());
            out.add(new SessionDto(p.getId(), p.getUsername(), p.getProfileId(), h == null ? "" : h.getName(),
                    p.getTitleId(), t == null ? "" : t.getName(), p.getDeviceName(), p.getIp(), p.getCountry(),
                    p.getStartedAt(), p.getLastHeartbeatAt()));
        }
        return out;
    }

    static String hash(String token) {
        return Base64Url.encode(Crypto.sha256(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
