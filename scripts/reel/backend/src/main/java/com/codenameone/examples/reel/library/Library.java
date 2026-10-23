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
package com.codenameone.examples.reel.library;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.reel.Ids;
import com.codenameone.examples.reel.Kinds;
import com.codenameone.examples.reel.Roles;
import com.codenameone.examples.reel.account.Devices;
import com.codenameone.examples.reel.account.Households;
import com.codenameone.examples.reel.api.DownloadDto;
import com.codenameone.examples.reel.api.Features;
import com.codenameone.examples.reel.api.HistoryEntryDto;
import com.codenameone.examples.reel.api.TitleCardDto;
import com.codenameone.examples.reel.billing.Entitlements;
import com.codenameone.examples.reel.catalog.Catalog;
import com.codenameone.examples.reel.domain.Download;
import com.codenameone.examples.reel.domain.Household;
import com.codenameone.examples.reel.domain.ListEntry;
import com.codenameone.examples.reel.domain.Progress;
import com.codenameone.examples.reel.domain.Title;
import com.codenameone.examples.reel.domain.TitleRating;
import com.codenameone.examples.reel.domain.Video;
import com.codenameone.examples.reel.playback.Licenses;
import com.codenameone.examples.reel.playback.MediaTokens;
import com.codenameone.examples.reel.repository.DownloadRepository;
import com.codenameone.examples.reel.repository.ListEntryRepository;
import com.codenameone.examples.reel.repository.ProgressRepository;
import com.codenameone.examples.reel.repository.TitleRatingRepository;
import com.codenameone.examples.reel.repository.TitleRepository;
import com.codenameone.examples.reel.repository.VideoRepository;
import com.codenameone.examples.reel.settings.Settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// A profile's list, ratings and history, and an account's downloads.
///
/// A download is the CENC file with a ClearKey license token that lasts
/// [#LICENSE_DAYS] days. The app keeps both and plays offline until the
/// token expires, then renews it with the server (which re-checks the plan).
/// A token is tied to the download, not a session, so deleting the download
/// or losing the plan stops renewals; what is already on the device plays
/// until its license runs out, as with every offline DRM.
@Component
public class Library {
    public static final int LICENSE_DAYS = 30;
    public static final int MAX_PER_DEVICE = 25;

    private final ListEntryRepository lists;
    private final TitleRatingRepository ratings;
    private final ProgressRepository progress;
    private final DownloadRepository downloads;
    private final TitleRepository titles;
    private final VideoRepository videos;
    private final Households households;
    private final Devices devices;
    private final Entitlements entitlements;
    private final Settings settings;
    private final MediaTokens tokens;
    private final Catalog catalog;
    private final String mediaDir;

    public Library(ListEntryRepository lists, TitleRatingRepository ratings, ProgressRepository progress,
            DownloadRepository downloads, TitleRepository titles, VideoRepository videos, Households households,
            Devices devices, Entitlements entitlements, Settings settings, MediaTokens tokens, Catalog catalog,
            @Value("${reel.media.dir:media}") String mediaDir) {
        this.lists = lists;
        this.ratings = ratings;
        this.progress = progress;
        this.downloads = downloads;
        this.titles = titles;
        this.videos = videos;
        this.households = households;
        this.devices = devices;
        this.entitlements = entitlements;
        this.settings = settings;
        this.tokens = tokens;
        this.catalog = catalog;
        this.mediaDir = mediaDir;
    }

    @Transactional(rollbackFor = IOException.class)
    public List<TitleCardDto> myList(String username, String profileId) throws IOException {
        Household profile = households.resolve(username, profileId);
        List<ListEntry> rows = lists.findByProfileIdOrderByAddedAtDesc(profile.getId());
        List<TitleCardDto> out = new ArrayList<TitleCardDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Title t = titles.findById(rows.get(iter).getTitleId());
            if (t != null && Kinds.PUBLISHED.equals(t.getStatus()) && Households.allows(profile, t.getRating())) {
                out.add(catalog.card(t, 0, 0));
            }
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public void add(String username, String titleId, String profileId) throws IOException {
        Household profile = households.resolve(username, profileId);
        published(titleId);
        if (lists.findFirstByProfileIdAndTitleId(profile.getId(), titleId) != null) {
            return;
        }
        if (lists.findByProfileIdOrderByAddedAtDesc(profile.getId()).size() >= 500) {
            throw new ResponseStatusException(409, "My List holds 500 titles");
        }
        ListEntry row = new ListEntry();
        row.setId(Ids.next());
        row.setProfileId(profile.getId());
        row.setTitleId(titleId);
        row.setAddedAt(System.currentTimeMillis());
        lists.insert(row);
    }

    @Transactional(rollbackFor = IOException.class)
    public void remove(String username, String titleId, String profileId) throws IOException {
        Household profile = households.resolve(username, profileId);
        ListEntry row = lists.findFirstByProfileIdAndTitleId(profile.getId(), titleId);
        if (row != null) {
            lists.deleteById(row.getId());
        }
    }

    /// Rates a title 1-5, or clears the rating with 0. The title keeps a
    /// running sum and count, adjusted by the difference, so the average
    /// never needs a scan.
    @Transactional(rollbackFor = IOException.class)
    public void rate(String username, String titleId, String profileId, int stars) throws IOException {
        if (stars < 0 || stars > 5) {
            throw new ResponseStatusException(400, "Rate 1 to 5 stars, or 0 to clear");
        }
        Household profile = households.resolve(username, profileId);
        Title t = published(titleId);
        TitleRating row = ratings.findFirstByProfileIdAndTitleId(profile.getId(), titleId);
        int before = row == null ? 0 : row.getStars();
        if (row == null && stars > 0) {
            row = new TitleRating();
            row.setId(Ids.next());
            row.setProfileId(profile.getId());
            row.setUsername(username);
            row.setTitleId(titleId);
            row.setStars(stars);
            row.setUpdatedAt(System.currentTimeMillis());
            ratings.insert(row);
        } else if (row != null && stars == 0) {
            ratings.deleteById(row.getId());
        } else if (row != null) {
            row.setStars(stars);
            row.setUpdatedAt(System.currentTimeMillis());
            ratings.save(row);
        }
        t.setRatingSum(t.getRatingSum() - before + stars);
        t.setRatingCount(t.getRatingCount() + (before == 0 && stars > 0 ? 1 : before > 0 && stars == 0 ? -1 : 0));
        titles.save(t);
    }

    @Transactional(rollbackFor = IOException.class)
    public List<HistoryEntryDto> history(String username, String profileId) throws IOException {
        Household profile = households.resolve(username, profileId);
        List<Progress> rows = progress.findTop50ByProfileIdOrderByUpdatedAtDesc(profile.getId());
        List<HistoryEntryDto> out = new ArrayList<HistoryEntryDto>();
        Set<String> seen = new HashSet<String>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Progress p = rows.get(iter);
            if (!seen.add(p.getTitleId())) {
                continue;
            }
            Title t = titles.findById(p.getTitleId());
            if (t == null) {
                continue;
            }
            out.add(new HistoryEntryDto(t.getId(), t.getName(), p.getVideoId(), t.getPosterUrl(),
                    p.getPositionSeconds(), p.getDurationSeconds(), p.getUpdatedAt()));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public void forget(String username, String titleId, String profileId) throws IOException {
        Household profile = households.resolve(username, profileId);
        progress.deleteByProfileIdAndTitleId(profile.getId(), titleId);
    }

    // ---------------------------------------------------------- downloads

    @Transactional(readOnly = true)
    public List<DownloadDto> downloads(String username, String deviceId) {
        List<Download> rows = deviceId == null || deviceId.length() == 0 ? downloads.findByUsername(username)
                : downloads.findByUsernameAndDeviceId(username, deviceId);
        List<DownloadDto> out = new ArrayList<DownloadDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter), null, null));
        }
        return out;
    }

    /// Starts or renews a download. Renewing an existing download of the same
    /// video on the same device issues a fresh token for it.
    @Transactional(rollbackFor = IOException.class)
    public DownloadDto download(String username, String titleId, String deviceId, String episodeId)
            throws IOException {
        settings.require(Features.DOWNLOADS);
        if (deviceId == null || deviceId.length() == 0 || !devices.usable(username, deviceId)) {
            throw new ResponseStatusException(400, "Downloads need a signed-in device");
        }
        Title t = published(titleId);
        boolean staff = Roles.isStaff() || t.getCreator().equals(username);
        Entitlements.Access access = entitlements.access(username, t, staff);
        if (!access.allowed) {
            throw new ResponseStatusException(access.status, access.reason);
        }
        if (!staff && !entitlements.allowance(username).downloads) {
            throw new ResponseStatusException(402, "Your plan does not include downloads");
        }
        if (Entitlements.RENTAL.equals(t.getAccess()) && !staff) {
            throw new ResponseStatusException(409, "Rentals stream only");
        }
        Video v = playable(t, episodeId);
        long now = System.currentTimeMillis();
        List<Download> mine = downloads.findByUsernameAndDeviceId(username, deviceId);
        Download row = null;
        for (int iter = 0; iter < mine.size(); iter++) {
            if (mine.get(iter).getVideoId().equals(v.getId())) {
                row = mine.get(iter);
            }
        }
        boolean fresh = row == null;
        if (fresh) {
            if (mine.size() >= MAX_PER_DEVICE) {
                throw new ResponseStatusException(409, "A device holds " + MAX_PER_DEVICE + " downloads");
            }
            row = new Download();
            row.setId(Ids.next());
            row.setUsername(username);
            row.setDeviceId(deviceId);
            row.setTitleId(t.getId());
            row.setVideoId(v.getId());
            row.setScheme(Licenses.CLEARKEY);
            row.setCreatedAt(now);
        }
        long ttl = LICENSE_DAYS * 86400L;
        row.setLicenseExpiresAt(now + ttl * 1000L);
        if (fresh) {
            downloads.insert(row);
        } else {
            downloads.save(row);
        }
        int maxHeight = Entitlements.maxHeight(staff ? "1080" : entitlements.allowance(username).maxQuality);
        String media = tokens.media("d", row.getId(), v.getId(), maxHeight, ttl);
        String license = tokens.license("d", row.getId(), v.getId(), ttl);
        return dto(row, "/media/" + media + "/" + v.getId() + "/cenc.mp4", license);
    }

    @Transactional(rollbackFor = IOException.class)
    public void delete(String username, String id) throws IOException {
        Download row = downloads.findById(id);
        if (row == null || !row.getUsername().equals(username)) {
            throw new ResponseStatusException(404, "No such download");
        }
        downloads.deleteById(id);
    }

    private DownloadDto dto(Download d, String url, String license) {
        Title t = titles.findById(d.getTitleId());
        File f = new File(mediaDir, d.getVideoId() + "/cenc.mp4");
        return new DownloadDto(d.getId(), d.getTitleId(), t == null ? "" : t.getName(), d.getVideoId(), url,
                d.getScheme(), license, d.getLicenseExpiresAt(), f.exists() ? f.length() : 0L, d.getDeviceId());
    }

    private Title published(String titleId) {
        Title t = titleId == null ? null : titles.findById(titleId);
        if (t == null || !Kinds.PUBLISHED.equals(t.getStatus())) {
            throw new ResponseStatusException(404, "No such title");
        }
        return t;
    }

    private Video playable(Title t, String episodeId) {
        List<Video> all = videos.findByTitleIdOrderBySeasonAscNumberAsc(t.getId());
        for (int iter = 0; iter < all.size(); iter++) {
            Video v = all.get(iter);
            boolean wanted = episodeId == null || episodeId.length() == 0
                    ? !Kinds.TRAILER.equals(v.getRole()) : v.getId().equals(episodeId);
            if (wanted && Kinds.READY.equals(v.getStatus())) {
                return v;
            }
        }
        throw new ResponseStatusException(404, "Nothing to download");
    }
}
