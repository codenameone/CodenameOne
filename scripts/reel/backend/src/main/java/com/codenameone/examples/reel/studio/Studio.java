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
package com.codenameone.examples.reel.studio;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.reel.Caller;
import com.codenameone.examples.reel.Ids;
import com.codenameone.examples.reel.Kinds;
import com.codenameone.examples.reel.Roles;
import com.codenameone.examples.reel.Rows;
import com.codenameone.examples.reel.account.Households;
import com.codenameone.examples.reel.api.AppealDto;
import com.codenameone.examples.reel.api.ChapterDto;
import com.codenameone.examples.reel.api.ChaptersDto;
import com.codenameone.examples.reel.api.Features;
import com.codenameone.examples.reel.api.InteractionEditDto;
import com.codenameone.examples.reel.api.InteractionsDto;
import com.codenameone.examples.reel.api.NameCountDto;
import com.codenameone.examples.reel.api.StudioTitleDto;
import com.codenameone.examples.reel.api.StudioTitleEditDto;
import com.codenameone.examples.reel.api.StudioVideoDto;
import com.codenameone.examples.reel.api.StudioVideoEditDto;
import com.codenameone.examples.reel.api.TitleAnalyticsDto;
import com.codenameone.examples.reel.billing.Entitlements;
import com.codenameone.examples.reel.domain.Answer;
import com.codenameone.examples.reel.domain.Chapter;
import com.codenameone.examples.reel.domain.Interaction;
import com.codenameone.examples.reel.domain.Invoice;
import com.codenameone.examples.reel.domain.Playback;
import com.codenameone.examples.reel.domain.Progress;
import com.codenameone.examples.reel.domain.Title;
import com.codenameone.examples.reel.domain.Video;
import com.codenameone.examples.reel.domain.VideoKey;
import com.codenameone.examples.reel.moderation.Reviews;
import com.codenameone.examples.reel.repository.AnswerRepository;
import com.codenameone.examples.reel.repository.CertificateRepository;
import com.codenameone.examples.reel.repository.ChapterRepository;
import com.codenameone.examples.reel.repository.InteractionRepository;
import com.codenameone.examples.reel.repository.InvoiceRepository;
import com.codenameone.examples.reel.repository.PlaybackRepository;
import com.codenameone.examples.reel.repository.ProgressRepository;
import com.codenameone.examples.reel.repository.TitleRepository;
import com.codenameone.examples.reel.repository.VideoKeyRepository;
import com.codenameone.examples.reel.repository.VideoRepository;
import com.codenameone.examples.reel.settings.Settings;
import com.codename1.backend.Crypto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The creator studio: titles, their videos, chapters and interactions.
///
/// A creator owns the titles they make; an admin can edit any. A title is a
/// DRAFT until submitted, then waits IN_REVIEW for a moderator (see
/// [Reviews]); admins publish without review. Editing a published title's
/// text sends it back to review, since what was approved is no longer what is
/// shown. Videos are added to a title and then uploaded ([Uploads]); each gets
/// its content key when it is created, so the key exists before any worker
/// packages a byte of it.
@Component
public class Studio {
    public static final int MAX_INTERACTIONS = 50;
    public static final int MAX_CHAPTERS = 100;

    private final TitleRepository titles;
    private final VideoRepository videos;
    private final VideoKeyRepository keys;
    private final ChapterRepository chapters;
    private final InteractionRepository interactions;
    private final PlaybackRepository playbacks;
    private final ProgressRepository progress;
    private final AnswerRepository answers;
    private final CertificateRepository certificates;
    private final InvoiceRepository invoices;
    private final Reviews reviews;
    private final Settings settings;

    public Studio(TitleRepository titles, VideoRepository videos, VideoKeyRepository keys,
            ChapterRepository chapters, InteractionRepository interactions, PlaybackRepository playbacks,
            ProgressRepository progress, AnswerRepository answers, CertificateRepository certificates,
            InvoiceRepository invoices, Reviews reviews, Settings settings) {
        this.titles = titles;
        this.videos = videos;
        this.keys = keys;
        this.chapters = chapters;
        this.interactions = interactions;
        this.playbacks = playbacks;
        this.progress = progress;
        this.answers = answers;
        this.certificates = certificates;
        this.invoices = invoices;
        this.reviews = reviews;
        this.settings = settings;
    }

    /// Who may use the studio: creators and admins, and everyone when open
    /// uploads are on (the video platform preset).
    private static boolean trusted() {
        return Caller.hasRole(Roles.ADMIN);
    }

    public void requireCreator() {
        if (!Caller.hasRole(Roles.CREATOR) && !Caller.hasRole(Roles.ADMIN)
                && !settings.enabled(Features.OPEN_UPLOADS)) {
            throw new ResponseStatusException(403, "Only creators can upload");
        }
    }

    @Transactional(readOnly = true)
    public List<StudioTitleDto> mine(String username) {
        List<Title> rows = titles.findByCreatorOrderByCreatedAtDesc(username);
        List<StudioTitleDto> out = new ArrayList<StudioTitleDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter), false));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public StudioTitleDto title(String username, String id) {
        return dto(own(username, id), true);
    }

    @Transactional(rollbackFor = IOException.class)
    public StudioTitleDto create(String username, StudioTitleEditDto wanted) throws IOException {
        Title t = new Title();
        t.setId(Ids.next());
        t.setCreator(username);
        t.setStatus(Kinds.DRAFT);
        t.setCreatedAt(System.currentTimeMillis());
        apply(t, wanted);
        titles.insert(t);
        return dto(t, true);
    }

    @Transactional(rollbackFor = IOException.class)
    public StudioTitleDto update(String username, String id, StudioTitleEditDto wanted) throws IOException {
        Title t = own(username, id);
        apply(t, wanted);
        titles.save(t);
        reviews.changed(t, trusted());
        return dto(t, true);
    }

    @Transactional(rollbackFor = IOException.class)
    public StudioTitleDto submit(String username, String id) throws IOException {
        Title t = own(username, id);
        if (!Kinds.DRAFT.equals(t.getStatus()) && !Kinds.REJECTED.equals(t.getStatus())) {
            throw new ResponseStatusException(409, "Only a draft or a rejected title can be submitted");
        }
        if (!hasReadyVideo(t.getId())) {
            throw new ResponseStatusException(409, "Upload a video and wait for it to finish processing");
        }
        if (Caller.hasRole(Roles.ADMIN)) {
            t.setStatus(Kinds.PUBLISHED);
            t.setPublishedAt(System.currentTimeMillis());
            titles.save(t);
        } else {
            t.setStatus(Kinds.IN_REVIEW);
            titles.save(t);
            reviews.submitted(t);
        }
        return dto(t, true);
    }

    @Transactional(rollbackFor = IOException.class)
    public StudioTitleDto unpublish(String username, String id) throws IOException {
        Title t = own(username, id);
        if (Kinds.REMOVED.equals(t.getStatus())) {
            throw new ResponseStatusException(409, "A removed title can only be appealed");
        }
        t.setStatus(Kinds.DRAFT);
        titles.save(t);
        reviews.withdrawn(t.getId(), username);
        return dto(t, true);
    }

    @Transactional(rollbackFor = IOException.class)
    public StudioTitleDto appeal(String username, String id, AppealDto appeal) throws IOException {
        Title t = own(username, id);
        reviews.appeal(username, t, appeal);
        return dto(t, true);
    }

    // ------------------------------------------------------------- videos

    @Transactional(rollbackFor = IOException.class)
    public StudioVideoDto addVideo(String username, String titleId, StudioVideoEditDto wanted) throws IOException {
        Title t = own(username, titleId);
        if (videos.findByTitleIdOrderBySeasonAscNumberAsc(titleId).size() >= 500) {
            throw new ResponseStatusException(409, "A title holds at most 500 videos");
        }
        long now = System.currentTimeMillis();
        Video v = new Video();
        v.setId(Ids.next());
        v.setTitleId(t.getId());
        v.setStatus(Kinds.UPLOADING);
        v.setCreatedAt(now);
        applyVideo(v, wanted, t);
        videos.insert(v);
        VideoKey key = new VideoKey();
        key.setVideoId(v.getId());
        key.setKeyId(Ids.hex(Crypto.randomBytes(16)));
        key.setKeyHex(Ids.hex(Crypto.randomBytes(16)));
        key.setIvHex(Ids.hex(Crypto.randomBytes(16)));
        key.setCreatedAt(now);
        keys.insert(key);
        reviews.changed(t, trusted());
        return video(v, true);
    }

    @Transactional(rollbackFor = IOException.class)
    public StudioVideoDto updateVideo(String username, String id, StudioVideoEditDto wanted) throws IOException {
        Video v = ownVideo(username, id);
        Title t = titles.findById(v.getTitleId());
        applyVideo(v, wanted, t);
        videos.save(v);
        reviews.changed(t, trusted());
        return video(v, true);
    }

    /// Deletes a video's rows. Its packaged files stay on disk until the
    /// operator prunes `reel.media.dir`; the key goes, so they no longer play.
    @Transactional(rollbackFor = IOException.class)
    public void deleteVideo(String username, String id) throws IOException {
        Video v = ownVideo(username, id);
        chapters.deleteByVideoId(id);
        interactions.deleteByVideoId(id);
        if (keys.findById(id) != null) {
            keys.deleteById(id);
        }
        videos.deleteById(id);
    }

    @Transactional(rollbackFor = IOException.class)
    public StudioVideoDto setChapters(String username, String id, ChaptersDto wanted) throws IOException {
        Video v = ownVideo(username, id);
        List<ChapterDto> list = wanted == null || wanted.chapters() == null ? new ArrayList<ChapterDto>()
                : wanted.chapters();
        if (list.size() > MAX_CHAPTERS) {
            throw new ResponseStatusException(400, "At most " + MAX_CHAPTERS + " chapters");
        }
        chapters.deleteByVideoId(id);
        for (int iter = 0; iter < list.size(); iter++) {
            ChapterDto c = list.get(iter);
            String name = c.title() == null ? "" : c.title().trim();
            if (name.length() == 0) {
                continue;
            }
            Chapter row = new Chapter();
            row.setId(Ids.next());
            row.setVideoId(id);
            row.setStartSeconds(Math.max(0, c.startSeconds()));
            row.setTitle(name.length() > 120 ? name.substring(0, 120) : name);
            chapters.insert(row);
        }
        reviews.changed(titles.findById(v.getTitleId()), trusted());
        return video(v, true);
    }

    /// Replaces a video's interactions: quizzes (one or more right answers),
    /// polls (no right answer) and branches (each option jumps to a video of
    /// the same title).
    @Transactional(rollbackFor = IOException.class)
    public StudioVideoDto setInteractions(String username, String id, InteractionsDto wanted) throws IOException {
        settings.require(Features.INTERACTIVE);
        Video v = ownVideo(username, id);
        List<InteractionEditDto> list = wanted == null || wanted.interactions() == null
                ? new ArrayList<InteractionEditDto>() : wanted.interactions();
        if (list.size() > MAX_INTERACTIONS) {
            throw new ResponseStatusException(400, "At most " + MAX_INTERACTIONS + " interactions per video");
        }
        Set<String> siblings = new HashSet<String>();
        List<Video> all = videos.findByTitleIdOrderBySeasonAscNumberAsc(v.getTitleId());
        for (int iter = 0; iter < all.size(); iter++) {
            siblings.add(all.get(iter).getId());
        }
        List<Interaction> rows = new ArrayList<Interaction>();
        for (int iter = 0; iter < list.size(); iter++) {
            rows.add(interaction(id, iter, list.get(iter), siblings));
        }
        interactions.deleteByVideoId(id);
        for (int iter = 0; iter < rows.size(); iter++) {
            interactions.insert(rows.get(iter));
        }
        reviews.changed(titles.findById(v.getTitleId()), trusted());
        return video(v, true);
    }

    private Interaction interaction(String videoId, int position, InteractionEditDto e, Set<String> siblings)
            throws IOException {
        String kind = e.kind() == null ? "" : e.kind();
        boolean quiz = "QUIZ".equals(kind);
        boolean poll = "POLL".equals(kind);
        boolean branch = "BRANCH".equals(kind);
        if (!quiz && !poll && !branch) {
            throw new ResponseStatusException(400, "An interaction is a QUIZ, a POLL or a BRANCH");
        }
        String prompt = e.prompt() == null ? "" : e.prompt().trim();
        List<String> options = e.options() == null ? new ArrayList<String>() : e.options();
        if (prompt.length() == 0 || prompt.length() > 500 || options.size() < 2 || options.size() > 8) {
            throw new ResponseStatusException(400, "Each interaction needs a prompt and 2-8 options");
        }
        List<Integer> right = e.answers() == null ? new ArrayList<Integer>() : e.answers();
        for (int iter = 0; iter < right.size(); iter++) {
            Integer a = right.get(iter);
            if (a == null || a.intValue() < 0 || a.intValue() >= options.size()) {
                throw new ResponseStatusException(400, "An answer names an option that does not exist");
            }
        }
        if (quiz && right.isEmpty()) {
            throw new ResponseStatusException(400, "Mark the right answer of \"" + prompt + "\"");
        }
        List<String> targets = e.branchTargets() == null ? new ArrayList<String>() : e.branchTargets();
        if (branch) {
            if (targets.size() != options.size()) {
                throw new ResponseStatusException(400, "Each branch option needs a video to go to");
            }
            for (int iter = 0; iter < targets.size(); iter++) {
                if (!siblings.contains(targets.get(iter))) {
                    throw new ResponseStatusException(400, "A branch can only go to a video of the same title");
                }
            }
        }
        Interaction row = new Interaction();
        row.setId(Ids.next());
        row.setVideoId(videoId);
        row.setKind(kind);
        row.setAtSeconds(Math.max(0, e.atSeconds()));
        row.setPrompt(prompt);
        row.setOptions(Rows.joinLines(options));
        row.setAnswers(poll || branch ? "" : Rows.joinInts(right));
        String explanation = e.explanation() == null ? "" : e.explanation().trim();
        row.setExplanation(explanation.length() > 1000 ? explanation.substring(0, 1000) : explanation);
        row.setRequired(e.required());
        row.setBranchTargets(branch ? Rows.join(targets) : "");
        row.setPosition(position);
        return row;
    }

    // ---------------------------------------------------------- analytics

    @Transactional(readOnly = true)
    public TitleAnalyticsDto analytics(String username, String id, int days) {
        Title t = own(username, id);
        long since = System.currentTimeMillis() - Math.max(1, Math.min(365, days)) * 86400000L;
        List<Playback> plays = playbacks.findByTitleIdAndStartedAtGreaterThan(id, since);
        Set<String> viewers = new HashSet<String>();
        Map<String, Integer> countries = new HashMap<String, Integer>();
        long seconds = 0;
        for (int iter = 0; iter < plays.size(); iter++) {
            Playback p = plays.get(iter);
            viewers.add(p.getProfileId().length() > 0 ? p.getProfileId() : p.getUsername());
            long end = p.getEndedAt() > 0 ? p.getEndedAt() : p.getLastHeartbeatAt();
            seconds += Math.max(0L, (end - p.getStartedAt()) / 1000L);
            String c = p.getCountry().length() == 0 ? "??" : p.getCountry();
            Integer n = countries.get(c);
            countries.put(c, Integer.valueOf(n == null ? 1 : n.intValue() + 1));
        }
        List<Progress> marks = progress.findByTitleId(id);
        int[] reached = new int[10];
        int completed = 0;
        for (int iter = 0; iter < marks.size(); iter++) {
            Progress p = marks.get(iter);
            if (p.isCompleted()) {
                completed++;
            }
            int pct = p.isCompleted() ? 100 : p.getDurationSeconds() <= 0 ? 0
                    : p.getPositionSeconds() * 100 / p.getDurationSeconds();
            for (int d = 0; d < 10; d++) {
                if (pct >= d * 10) {
                    reached[d]++;
                }
            }
        }
        List<Integer> retention = new ArrayList<Integer>();
        for (int d = 0; d < 10; d++) {
            retention.add(Integer.valueOf(marks.isEmpty() ? 0 : reached[d] * 100 / marks.size()));
        }
        List<Answer> quiz = answers.findByTitleId(id);
        int right = 0;
        for (int iter = 0; iter < quiz.size(); iter++) {
            if (quiz.get(iter).isCorrect()) {
                right++;
            }
        }
        long revenue = 0;
        List<Invoice> paid = invoices.findByTitleIdAndCreatedAtGreaterThan(id, since);
        for (int iter = 0; iter < paid.size(); iter++) {
            if ("PAID".equals(paid.get(iter).getStatus())) {
                revenue += paid.get(iter).getAmountCents();
            }
        }
        List<NameCountDto> where = new ArrayList<NameCountDto>();
        for (Map.Entry<String, Integer> e : countries.entrySet()) {
            where.add(new NameCountDto(e.getKey(), e.getValue().intValue()));
        }
        double average = t.getRatingCount() == 0 ? 0 : (double) t.getRatingSum() / t.getRatingCount();
        return new TitleAnalyticsDto(id, t.getName(), plays.size(), viewers.size(), seconds / 60L,
                marks.isEmpty() ? 0 : completed * 100 / marks.size(), average, revenue, quiz.size(),
                quiz.isEmpty() ? 0 : right * 100 / quiz.size(), (int) certificates.countByTitleId(id), retention,
                where);
    }

    // ------------------------------------------------------------ helpers

    Title own(String username, String id) {
        Title t = id == null ? null : titles.findById(id);
        if (t == null || (!t.getCreator().equals(username) && !Caller.hasRole(Roles.ADMIN))) {
            throw new ResponseStatusException(404, "No such title");
        }
        return t;
    }

    Video ownVideo(String username, String id) {
        Video v = id == null ? null : videos.findById(id);
        if (v == null) {
            throw new ResponseStatusException(404, "No such video");
        }
        own(username, v.getTitleId());
        return v;
    }

    private boolean hasReadyVideo(String titleId) {
        List<Video> all = videos.findByTitleIdOrderBySeasonAscNumberAsc(titleId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (Kinds.READY.equals(all.get(iter).getStatus()) && !Kinds.TRAILER.equals(all.get(iter).getRole())) {
                return true;
            }
        }
        return false;
    }

    private void apply(Title t, StudioTitleEditDto e) {
        if (e == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        String kind = e.kind() == null ? Kinds.MOVIE : e.kind();
        if (!Kinds.MOVIE.equals(kind) && !Kinds.SERIES.equals(kind) && !Kinds.COURSE.equals(kind)) {
            throw new ResponseStatusException(400, "A title is a MOVIE, a SERIES or a COURSE");
        }
        t.setKind(kind);
        t.setName(required(e.name(), 120, "the name"));
        t.setSynopsis(cut(e.synopsis(), 2000));
        t.setYear(Math.max(0, Math.min(3000, e.year())));
        String rating = e.rating() == null ? "G" : e.rating();
        boolean known = false;
        for (int iter = 0; iter < Households.RATINGS.length; iter++) {
            known |= Households.RATINGS[iter].equals(rating);
        }
        if (!known) {
            throw new ResponseStatusException(400, "Rate the title G, PG, PG-13, R or NC-17");
        }
        t.setRating(rating);
        t.setAgeRestricted("R".equals(rating) || "NC-17".equals(rating));
        t.setGenres(cut(Rows.join(e.genres()), 255));
        t.setTags(cut(Rows.join(e.tags()), 255));
        String access = e.access() == null ? Entitlements.SUBSCRIPTION : e.access();
        if (!Entitlements.FREE.equals(access) && !Entitlements.SUBSCRIPTION.equals(access)
                && !Entitlements.RENTAL.equals(access)) {
            throw new ResponseStatusException(400, "Access is FREE, SUBSCRIPTION or RENTAL");
        }
        t.setAccess(access);
        t.setRentalPriceCents(Entitlements.RENTAL.equals(access) ? Math.max(50L, e.rentalPriceCents()) : 0L);
        t.setCertificate(Kinds.COURSE.equals(kind) && e.certificate());
        t.setPassPercent(Math.max(0, Math.min(100, e.passPercent() == 0 ? 70 : e.passPercent())));
    }

    private void applyVideo(Video v, StudioVideoEditDto e, Title t) {
        if (e == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        String role = e.role() == null ? defaultRole(t) : e.role();
        if (!Kinds.FEATURE.equals(role) && !Kinds.EPISODE.equals(role) && !Kinds.LESSON.equals(role)
                && !Kinds.TRAILER.equals(role) && !Kinds.AD.equals(role)) {
            throw new ResponseStatusException(400, "A video is a FEATURE, EPISODE, LESSON, TRAILER or AD");
        }
        if (Kinds.AD.equals(role) && !Caller.hasRole(Roles.ADMIN)) {
            throw new ResponseStatusException(403, "Only admins upload ads");
        }
        v.setRole(role);
        v.setSeason(Math.max(0, e.season()));
        v.setNumber(Math.max(0, e.number()));
        v.setName(required(e.name(), 120, "the video's name"));
        v.setSynopsis(cut(e.synopsis(), 1000));
    }

    private static String defaultRole(Title t) {
        if (t != null && Kinds.SERIES.equals(t.getKind())) {
            return Kinds.EPISODE;
        }
        if (t != null && Kinds.COURSE.equals(t.getKind())) {
            return Kinds.LESSON;
        }
        return Kinds.FEATURE;
    }

    StudioTitleDto dto(Title t, boolean withVideos) {
        List<StudioVideoDto> list = new ArrayList<StudioVideoDto>();
        boolean interactive = false;
        List<Video> all = videos.findByTitleIdOrderBySeasonAscNumberAsc(t.getId());
        for (int iter = 0; iter < all.size(); iter++) {
            StudioVideoDto v = video(all.get(iter), withVideos);
            interactive |= !v.interactions().isEmpty()
                    || !interactions.findByVideoIdOrderByAtSecondsAsc(all.get(iter).getId()).isEmpty();
            if (withVideos) {
                list.add(v);
            }
        }
        return new StudioTitleDto(t.getId(), t.getKind(), t.getName(), t.getSynopsis(), t.getYear(), t.getRating(),
                Rows.split(t.getGenres()), Rows.split(t.getTags()), t.getAccess(), t.getRentalPriceCents(),
                t.getStatus(), reviews.status(t.getId()), t.getModerationNote(), t.getPosterUrl(), interactive,
                t.isCertificate(), t.getPassPercent(), t.getCreatedAt(), t.getPublishedAt(), list);
    }

    StudioVideoDto video(Video v, boolean full) {
        List<ChapterDto> cs = new ArrayList<ChapterDto>();
        List<InteractionEditDto> is = new ArrayList<InteractionEditDto>();
        if (full) {
            List<Chapter> crows = chapters.findByVideoIdOrderByStartSecondsAsc(v.getId());
            for (int iter = 0; iter < crows.size(); iter++) {
                Chapter c = crows.get(iter);
                cs.add(new ChapterDto(c.getId(), c.getStartSeconds(), c.getTitle()));
            }
            List<Interaction> irows = interactions.findByVideoIdOrderByAtSecondsAsc(v.getId());
            for (int iter = 0; iter < irows.size(); iter++) {
                Interaction i = irows.get(iter);
                is.add(new InteractionEditDto(i.getId(), i.getKind(), i.getAtSeconds(), i.getPrompt(),
                        Rows.splitLines(i.getOptions()), Rows.ints(i.getAnswers()), i.getExplanation(),
                        i.isRequired(), Rows.split(i.getBranchTargets())));
            }
        }
        return new StudioVideoDto(v.getId(), v.getRole(), v.getSeason(), v.getNumber(), v.getName(),
                v.getSynopsis(), v.getStatus(), v.getProgressPercent(), v.getError(), v.getDurationSeconds(),
                v.getThumbnailUrl(), cs, is);
    }

    private static String required(String value, int max, String what) {
        String t = value == null ? "" : value.trim();
        if (t.length() == 0 || t.length() > max) {
            throw new ResponseStatusException(400, "Fill in " + what + " (at most " + max + " characters)");
        }
        return t;
    }

    private static String cut(String value, int max) {
        String t = value == null ? "" : value.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
