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
package com.codenameone.examples.reel.catalog;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.reel.Kinds;
import com.codenameone.examples.reel.Roles;
import com.codenameone.examples.reel.Rows;
import com.codenameone.examples.reel.account.Accounts;
import com.codenameone.examples.reel.account.Households;
import com.codenameone.examples.reel.api.ChapterDto;
import com.codenameone.examples.reel.api.EpisodeDto;
import com.codenameone.examples.reel.api.Features;
import com.codenameone.examples.reel.api.GenreDto;
import com.codenameone.examples.reel.api.HomeDto;
import com.codenameone.examples.reel.api.HomeRowDto;
import com.codenameone.examples.reel.api.SearchResultDto;
import com.codenameone.examples.reel.api.TitleCardDto;
import com.codenameone.examples.reel.api.TitleDto;
import com.codenameone.examples.reel.billing.Entitlements;
import com.codenameone.examples.reel.domain.Chapter;
import com.codenameone.examples.reel.domain.Genre;
import com.codenameone.examples.reel.domain.Household;
import com.codenameone.examples.reel.domain.ListEntry;
import com.codenameone.examples.reel.domain.Progress;
import com.codenameone.examples.reel.domain.Title;
import com.codenameone.examples.reel.domain.TitleRating;
import com.codenameone.examples.reel.domain.Video;
import com.codenameone.examples.reel.repository.ChapterRepository;
import com.codenameone.examples.reel.repository.GenreRepository;
import com.codenameone.examples.reel.repository.InteractionRepository;
import com.codenameone.examples.reel.repository.ListEntryRepository;
import com.codenameone.examples.reel.repository.ProgressRepository;
import com.codenameone.examples.reel.repository.TitleRatingRepository;
import com.codenameone.examples.reel.repository.TitleRepository;
import com.codenameone.examples.reel.repository.VideoRepository;
import com.codenameone.examples.reel.settings.Settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// What a profile browses: the home rows, a title's page, genres and search.
///
/// Everything is filtered by the profile's rating (a kids profile never sees
/// an R title, not even in search) and only published titles are shown.
///
/// The recommendations ([Recommender]) mix what the profile watched and
/// rated with what other profiles who watched the same titles went on to
/// watch. They are computed on request from the database: a catalog of a few
/// thousand titles is a few milliseconds, and a larger service would precompute
/// the same scores in a scheduled job.
@Component
public class Catalog {
    public static final int ROW = 20;

    private final TitleRepository titles;
    private final VideoRepository videos;
    private final GenreRepository genres;
    private final ProgressRepository progress;
    private final ListEntryRepository lists;
    private final TitleRatingRepository ratings;
    private final ChapterRepository chapters;
    private final InteractionRepository interactions;
    private final Households households;
    private final Accounts accounts;
    private final Entitlements entitlements;
    private final Settings settings;
    private final Recommender recommender;

    public Catalog(TitleRepository titles, VideoRepository videos, GenreRepository genres,
            ProgressRepository progress, ListEntryRepository lists, TitleRatingRepository ratings,
            ChapterRepository chapters, InteractionRepository interactions, Households households,
            Accounts accounts, Entitlements entitlements, Settings settings, Recommender recommender) {
        this.titles = titles;
        this.videos = videos;
        this.genres = genres;
        this.progress = progress;
        this.lists = lists;
        this.ratings = ratings;
        this.chapters = chapters;
        this.interactions = interactions;
        this.households = households;
        this.accounts = accounts;
        this.entitlements = entitlements;
        this.settings = settings;
        this.recommender = recommender;
    }

    @Transactional(rollbackFor = IOException.class)
    public HomeDto home(String username, String profileId) throws IOException {
        Household profile = households.resolve(username, profileId);
        List<Title> visible = visible(profile);
        Map<String, Title> byId = new HashMap<String, Title>();
        for (int iter = 0; iter < visible.size(); iter++) {
            byId.put(visible.get(iter).getId(), visible.get(iter));
        }
        Map<String, Integer> percent = new HashMap<String, Integer>();
        List<HomeRowDto> rows = new ArrayList<HomeRowDto>();

        List<Progress> recent = progress.findTop50ByProfileIdOrderByUpdatedAtDesc(profile.getId());
        List<TitleCardDto> watching = new ArrayList<TitleCardDto>();
        Set<String> seen = new HashSet<String>();
        String lastWatched = null;
        for (int iter = 0; iter < recent.size(); iter++) {
            Progress p = recent.get(iter);
            Title t = byId.get(p.getTitleId());
            if (t == null || !seen.add(t.getId())) {
                continue;
            }
            if (lastWatched == null) {
                lastWatched = t.getId();
            }
            int pct = p.getDurationSeconds() <= 0 ? 0 : p.getPositionSeconds() * 100 / p.getDurationSeconds();
            percent.put(t.getId(), Integer.valueOf(p.isCompleted() ? 100 : pct));
            if (!p.isCompleted() && watching.size() < ROW) {
                watching.add(card(t, pct, 0));
            }
        }
        if (!watching.isEmpty()) {
            rows.add(new HomeRowDto("continue", "Continue watching", "CONTINUE", watching));
        }

        List<ListEntry> mine = lists.findByProfileIdOrderByAddedAtDesc(profile.getId());
        List<TitleCardDto> myList = new ArrayList<TitleCardDto>();
        for (int iter = 0; iter < mine.size() && myList.size() < ROW; iter++) {
            Title t = byId.get(mine.get(iter).getTitleId());
            if (t != null) {
                myList.add(card(t, pct(percent, t), 0));
            }
        }
        if (!myList.isEmpty()) {
            rows.add(new HomeRowDto("my-list", "My List", "LIST", myList));
        }

        List<Recommender.Scored> picks = recommender.forProfile(profile.getId(), visible);
        if (!picks.isEmpty()) {
            rows.add(new HomeRowDto("for-you", "Recommended for you", "RECOMMENDED", cards(picks, percent)));
        }
        if (lastWatched != null) {
            List<Recommender.Scored> similar = recommender.similar(byId.get(lastWatched), visible);
            if (!similar.isEmpty()) {
                rows.add(new HomeRowDto("because", "Because you watched " + byId.get(lastWatched).getName(),
                        "RECOMMENDED", cards(similar, percent)));
            }
        }

        List<Title> courses = new ArrayList<Title>();
        for (int iter = 0; iter < visible.size(); iter++) {
            if (Kinds.COURSE.equals(visible.get(iter).getKind())) {
                courses.add(visible.get(iter));
            }
        }
        if (!courses.isEmpty()) {
            rows.add(new HomeRowDto("courses", settings.enabled(Features.CERTIFICATES)
                    ? "Courses with certificates" : "Courses", "COURSES", plain(courses, percent)));
        }

        List<Title> trending = new ArrayList<Title>(visible);
        sort(trending, true);
        rows.add(new HomeRowDto("trending", "Trending now", "TRENDING", plain(trending, percent)));
        List<Title> fresh = new ArrayList<Title>(visible);
        sort(fresh, false);
        rows.add(new HomeRowDto("new", "New on " + settings.branding().serviceName(), "NEW", plain(fresh, percent)));

        List<Genre> all = genres.findByPositionGreaterThanEqualOrderByPositionAsc(0);
        for (int iter = 0; iter < all.size(); iter++) {
            List<Title> in = inGenre(visible, all.get(iter).getId());
            if (in.size() >= 2) {
                rows.add(new HomeRowDto("genre:" + all.get(iter).getId(), all.get(iter).getName(), "GENRE",
                        plain(in, percent)));
            }
        }

        Title hero = null;
        for (int iter = 0; iter < visible.size() && hero == null; iter++) {
            if (visible.get(iter).isFeatured()) {
                hero = visible.get(iter);
            }
        }
        if (hero == null && !trending.isEmpty()) {
            hero = trending.get(0);
        }
        return new HomeDto(hero == null ? null : card(hero, pct(percent, hero), 0), rows);
    }

    @Transactional(rollbackFor = IOException.class)
    public TitleDto title(String username, String id, String profileId) throws IOException {
        Household profile = households.resolve(username, profileId);
        Title t = titles.findById(id);
        boolean own = t != null && (t.getCreator().equals(username) || Roles.isStaff());
        if (t == null || (!Kinds.PUBLISHED.equals(t.getStatus()) && !own)) {
            throw new ResponseStatusException(404, "No such title");
        }
        if (!Households.allows(profile, t.getRating())) {
            throw new ResponseStatusException(403, "This profile cannot watch " + t.getRating() + " titles");
        }
        Entitlements.Access access = entitlements.access(username, t, own);
        Entitlements.Allowance allowance = entitlements.allowance(username);
        List<Video> all = videos.findByTitleIdOrderBySeasonAscNumberAsc(t.getId());
        List<EpisodeDto> episodes = new ArrayList<EpisodeDto>();
        String trailer = null;
        Video first = null;
        boolean interactive = false;
        Map<String, Progress> marks = new HashMap<String, Progress>();
        List<Progress> mine = progress.findByProfileIdAndTitleId(profile.getId(), t.getId());
        for (int iter = 0; iter < mine.size(); iter++) {
            marks.put(mine.get(iter).getVideoId(), mine.get(iter));
        }
        int resume = 0;
        for (int iter = 0; iter < all.size(); iter++) {
            Video v = all.get(iter);
            if (!Kinds.READY.equals(v.getStatus())) {
                continue;
            }
            if (Kinds.TRAILER.equals(v.getRole())) {
                trailer = v.getId();
                continue;
            }
            if (first == null) {
                first = v;
            }
            interactive |= !interactions.findByVideoIdOrderByAtSecondsAsc(v.getId()).isEmpty();
            Progress p = marks.get(v.getId());
            int pct = p == null || v.getDurationSeconds() <= 0 ? 0
                    : p.isCompleted() ? 100 : p.getPositionSeconds() * 100 / v.getDurationSeconds();
            if (p != null && !p.isCompleted() && resume == 0) {
                resume = p.getPositionSeconds();
            }
            episodes.add(new EpisodeDto(v.getId(), v.getSeason(), v.getNumber(), v.getName(), v.getSynopsis(),
                    v.getThumbnailUrl(), v.getDurationSeconds(), pct, p != null && p.isCompleted(), !access.allowed));
        }
        List<ChapterDto> cs = new ArrayList<ChapterDto>();
        if (first != null) {
            List<Chapter> rows = chapters.findByVideoIdOrderByStartSecondsAsc(first.getId());
            for (int iter = 0; iter < rows.size(); iter++) {
                cs.add(new ChapterDto(rows.get(iter).getId(), rows.get(iter).getStartSeconds(),
                        rows.get(iter).getTitle()));
            }
        }
        TitleRating mineRating = ratings.findFirstByProfileIdAndTitleId(profile.getId(), t.getId());
        boolean listed = lists.findFirstByProfileIdAndTitleId(profile.getId(), t.getId()) != null;
        List<Recommender.Scored> similar = recommender.similar(t, visible(profile));
        Map<String, Integer> none = new HashMap<String, Integer>();
        return new TitleDto(t.getId(), t.getKind(), t.getName(), t.getSynopsis(), t.getPosterUrl(),
                t.getBackdropUrl(), t.getYear(), t.getRating(), Rows.split(t.getGenres()), Rows.split(t.getTags()),
                t.getCreator(), accounts.displayName(t.getCreator()), first == null ? 0 : first.getDurationSeconds(),
                average(t), t.getRatingCount(), mineRating == null ? 0 : mineRating.getStars(), listed, resume,
                t.getAccess(), t.getRentalPriceCents(), settings.currency(), access.allowed,
                access.allowed && allowance.downloads && settings.enabled(Features.DOWNLOADS),
                interactive && settings.enabled(Features.INTERACTIVE),
                t.isCertificate() && settings.enabled(Features.CERTIFICATES), trailer, cs, episodes,
                cards(similar, none));
    }

    @Transactional(readOnly = true)
    public List<GenreDto> genres() {
        List<Title> published = titles.findByStatus(Kinds.PUBLISHED);
        List<Genre> all = genres.findByPositionGreaterThanEqualOrderByPositionAsc(0);
        List<GenreDto> out = new ArrayList<GenreDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            out.add(new GenreDto(all.get(iter).getId(), all.get(iter).getName(),
                    inGenre(published, all.get(iter).getId()).size()));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public List<TitleCardDto> genre(String username, String id, String profileId) throws IOException {
        Household profile = households.resolve(username, profileId);
        List<Title> in = inGenre(visible(profile), id);
        sort(in, true);
        return plain(in, new HashMap<String, Integer>(), 200);
    }

    /// Search over names, synopses, tags, genres and creators: every word of
    /// the query must appear somewhere, and a name match ranks first.
    @Transactional(rollbackFor = IOException.class)
    public SearchResultDto search(String username, String q, String profileId) throws IOException {
        Household profile = households.resolve(username, profileId);
        String query = q == null ? "" : q.trim();
        if (query.length() == 0) {
            return new SearchResultDto("", 0, new ArrayList<TitleCardDto>());
        }
        String[] words = fold(query).split("[^a-z0-9]+");
        List<Title> visible = visible(profile);
        List<Recommender.Scored> hits = new ArrayList<Recommender.Scored>();
        for (int iter = 0; iter < visible.size(); iter++) {
            Title t = visible.get(iter);
            String name = fold(t.getName());
            String rest = fold(t.getSynopsis() + " " + t.getTags() + " " + t.getGenres() + " "
                    + accounts.displayName(t.getCreator()));
            double score = 0;
            boolean all = true;
            for (int w = 0; w < words.length && all; w++) {
                if (words[w].length() == 0) {
                    continue;
                }
                if (name.indexOf(words[w]) >= 0) {
                    score += 3;
                } else if (rest.indexOf(words[w]) >= 0) {
                    score += 1;
                } else {
                    all = false;
                }
            }
            if (all && score > 0) {
                hits.add(new Recommender.Scored(t, score + Math.log(1 + t.getViewCount()) / 10.0));
            }
        }
        Recommender.sortScored(hits);
        List<TitleCardDto> cards = cards(hits, new HashMap<String, Integer>());
        return new SearchResultDto(query, hits.size(), cards);
    }

    // ------------------------------------------------------------ helpers

    List<Title> visible(Household profile) {
        List<Title> published = titles.findByStatus(Kinds.PUBLISHED);
        List<Title> out = new ArrayList<Title>();
        for (int iter = 0; iter < published.size(); iter++) {
            Title t = published.get(iter);
            if (Households.allows(profile, t.getRating()) && hasPlayable(t)
                    && (settings.enabled(Features.RENTALS) || !Entitlements.RENTAL.equals(t.getAccess()))) {
                out.add(t);
            }
        }
        return out;
    }

    private boolean hasPlayable(Title t) {
        List<Video> all = videos.findByTitleIdOrderBySeasonAscNumberAsc(t.getId());
        for (int iter = 0; iter < all.size(); iter++) {
            if (Kinds.READY.equals(all.get(iter).getStatus()) && !Kinds.TRAILER.equals(all.get(iter).getRole())
                    && !Kinds.AD.equals(all.get(iter).getRole())) {
                return true;
            }
        }
        return false;
    }

    static List<Title> inGenre(List<Title> from, String genre) {
        List<Title> out = new ArrayList<Title>();
        for (int iter = 0; iter < from.size(); iter++) {
            if (Rows.split(from.get(iter).getGenres()).contains(genre)) {
                out.add(from.get(iter));
            }
        }
        return out;
    }

    private static void sort(List<Title> list, final boolean byViews) {
        for (int i = 1; i < list.size(); i++) {
            Title t = list.get(i);
            int j = i - 1;
            while (j >= 0 && (byViews ? list.get(j).getViewCount() < t.getViewCount()
                    : list.get(j).getPublishedAt() < t.getPublishedAt())) {
                list.set(j + 1, list.get(j));
                j--;
            }
            list.set(j + 1, t);
        }
    }

    private List<TitleCardDto> plain(List<Title> list, Map<String, Integer> percent) {
        return plain(list, percent, ROW);
    }

    private List<TitleCardDto> plain(List<Title> list, Map<String, Integer> percent, int max) {
        List<TitleCardDto> out = new ArrayList<TitleCardDto>();
        for (int iter = 0; iter < list.size() && out.size() < max; iter++) {
            out.add(card(list.get(iter), pct(percent, list.get(iter)), 0));
        }
        return out;
    }

    private List<TitleCardDto> cards(List<Recommender.Scored> list, Map<String, Integer> percent) {
        List<TitleCardDto> out = new ArrayList<TitleCardDto>();
        for (int iter = 0; iter < list.size() && out.size() < ROW; iter++) {
            Title t = list.get(iter).title;
            out.add(card(t, pct(percent, t), list.get(iter).score));
        }
        return out;
    }

    private static int pct(Map<String, Integer> percent, Title t) {
        Integer p = percent.get(t.getId());
        return p == null ? 0 : p.intValue();
    }

    public TitleCardDto card(Title t, int progressPercent, double score) {
        int duration = 0;
        List<Video> all = videos.findByTitleIdOrderBySeasonAscNumberAsc(t.getId());
        for (int iter = 0; iter < all.size(); iter++) {
            if (Kinds.READY.equals(all.get(iter).getStatus()) && !Kinds.TRAILER.equals(all.get(iter).getRole())) {
                duration = all.get(iter).getDurationSeconds();
                break;
            }
        }
        String badge = "";
        long now = System.currentTimeMillis();
        if (Kinds.COURSE.equals(t.getKind()) && t.isCertificate()) {
            badge = "CERTIFICATE";
        } else if (t.getPublishedAt() > now - 14L * 86400000L) {
            badge = "NEW";
        }
        return new TitleCardDto(t.getId(), t.getKind(), t.getName(), t.getPosterUrl(), t.getBackdropUrl(),
                t.getYear(), t.getRating(), duration, score, progressPercent, badge,
                Entitlements.FREE.equals(t.getAccess()), Entitlements.RENTAL.equals(t.getAccess()));
    }

    static double average(Title t) {
        return t.getRatingCount() == 0 ? 0 : Math.round(t.getRatingSum() * 10.0 / t.getRatingCount()) / 10.0;
    }

    public static String fold(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            out.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return out.toString();
    }
}
