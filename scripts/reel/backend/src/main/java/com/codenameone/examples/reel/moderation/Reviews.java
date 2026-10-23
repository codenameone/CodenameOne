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
package com.codenameone.examples.reel.moderation;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.reel.Ids;
import com.codenameone.examples.reel.Kinds;
import com.codenameone.examples.reel.Rows;
import com.codenameone.examples.reel.account.Moderation;
import com.codenameone.examples.reel.api.AppealDto;
import com.codenameone.examples.reel.api.BlocklistEntryDto;
import com.codenameone.examples.reel.api.Features;
import com.codenameone.examples.reel.api.ModerationDecisionDto;
import com.codenameone.examples.reel.api.ModerationItemDto;
import com.codenameone.examples.reel.api.ReportDto;
import com.codenameone.examples.reel.domain.BlockedItem;
import com.codenameone.examples.reel.domain.FrameHash;
import com.codenameone.examples.reel.domain.ModerationItem;
import com.codenameone.examples.reel.domain.Title;
import com.codenameone.examples.reel.domain.Video;
import com.codenameone.examples.reel.repository.BlockedItemRepository;
import com.codenameone.examples.reel.repository.FrameHashRepository;
import com.codenameone.examples.reel.repository.ModerationItemRepository;
import com.codenameone.examples.reel.repository.TitleRepository;
import com.codenameone.examples.reel.repository.VideoRepository;
import com.codenameone.examples.reel.settings.Settings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Content moderation: the review queue and the automatic checks that feed it.
///
/// Every title a creator submits waits in the queue for a moderator, with the
/// signals the automatic checks found attached:
///
/// - **Keywords.** The blocklist's KEYWORD entries, matched against the
///   title's name, synopsis and tags as whole words, ignoring case.
/// - **Frames.** The worker hashes frames across every video it packages (a
///   64-bit difference hash, which survives re-encoding and resizing). A video
///   whose frames come within [#MAX_DISTANCE] bits of a FRAME entry is BLOCKED
///   before it can be published, and queued for a person to confirm.
/// - **A classifier**, when a deployment supplies one ([ContentClassifier]).
///
/// Viewers' reports and creators' appeals go into the same queue. Every
/// decision is recorded against the creator's account, which the admin console
/// shows beside their other moderation history.
@Component
public class Reviews {
    public static final String TITLE = "TITLE";
    public static final String VIDEO = "VIDEO";
    public static final String REPORT = "REPORT";
    public static final String APPEAL = "APPEAL";
    public static final String KEYWORD = "KEYWORD";
    public static final String FRAME = "FRAME";
    public static final String APPROVE = "APPROVE";
    public static final String REJECT = "REJECT";
    public static final String REMOVE = "REMOVE";
    public static final String DISMISS = "DISMISS";
    /// Hamming distance under which two frame hashes are the same picture.
    public static final int MAX_DISTANCE = 10;

    private final ModerationItemRepository items;
    private final BlockedItemRepository blocked;
    private final FrameHashRepository frames;
    private final TitleRepository titles;
    private final VideoRepository videos;
    private final Moderation history;
    private final Settings settings;
    private final ContentClassifier classifier;

    public Reviews(ModerationItemRepository items, BlockedItemRepository blocked, FrameHashRepository frames,
            TitleRepository titles, VideoRepository videos, Moderation history, Settings settings,
            ContentClassifier classifier) {
        this.items = items;
        this.blocked = blocked;
        this.frames = frames;
        this.titles = titles;
        this.videos = videos;
        this.history = history;
        this.settings = settings;
        this.classifier = classifier;
    }

    /// Queues a submitted title, with what the text checks found.
    @Transactional(rollbackFor = IOException.class)
    public void submitted(Title title) throws IOException {
        List<String> signals = keywordSignals(title.getName() + " " + title.getSynopsis() + " "
                + title.getTags().replace(',', ' '));
        List<String> more = classifier.classify(title.getName(), title.getSynopsis(), Rows.split(title.getTags()));
        if (more != null) {
            signals.addAll(more);
        }
        List<Video> all = videos.findByTitleIdOrderBySeasonAscNumberAsc(title.getId());
        for (int iter = 0; iter < all.size(); iter++) {
            if (Kinds.BLOCKED.equals(all.get(iter).getStatus())) {
                signals.add("frames:" + all.get(iter).getId());
            }
        }
        dropOpen(title.getId(), TITLE);
        queue(TITLE, title.getId(), title.getCreator(), "Submitted for publication", "", signals);
    }

    /// Checks a packaged video's frame hashes against the blocklist and keeps
    /// them. Answers true when the video matched and must stay BLOCKED.
    @Transactional(rollbackFor = IOException.class)
    public boolean screenFrames(Video video, String owner, List<String> hashes) throws IOException {
        frames.deleteByVideoId(video.getId());
        List<BlockedItem> list = blocked.findByKind(FRAME);
        List<String> hits = new ArrayList<String>();
        for (int iter = 0; hashes != null && iter < hashes.size(); iter++) {
            String h = hashes.get(iter);
            if (!hash64(h)) {
                continue;
            }
            FrameHash row = new FrameHash();
            row.setId(Ids.next());
            row.setVideoId(video.getId());
            row.setHash(h);
            row.setPosition(iter);
            frames.insert(row);
            for (int b = 0; b < list.size(); b++) {
                if (hash64(list.get(b).getValue()) && distance(h, list.get(b).getValue()) <= MAX_DISTANCE) {
                    hits.add("frame " + iter + " ~ " + list.get(b).getNote());
                    break;
                }
            }
        }
        if (hits.isEmpty()) {
            return false;
        }
        dropOpen(video.getId(), VIDEO);
        queue(VIDEO, video.getId(), owner, "Frames match the blocklist",
                hits.size() + " of " + hashes.size() + " sampled frames matched", hits);
        return true;
    }

    @Transactional(rollbackFor = IOException.class)
    public void report(String username, String titleId, ReportDto report) throws IOException {
        settings.require(Features.REPORTS);
        Title title = titles.findById(titleId);
        if (title == null || !Kinds.PUBLISHED.equals(title.getStatus())) {
            throw new ResponseStatusException(404, "No such title");
        }
        String reason = report == null || report.reason() == null ? "" : report.reason().trim();
        if (reason.length() == 0) {
            throw new ResponseStatusException(400, "Choose why you are reporting this title");
        }
        // One open report per viewer per title: more say nothing new.
        List<ModerationItem> open = items.findBySubjectIdAndStatus(titleId, Kinds.OPEN);
        for (int iter = 0; iter < open.size(); iter++) {
            ModerationItem m = open.get(iter);
            if (REPORT.equals(m.getKind()) && m.getSignals().indexOf("by:" + username) >= 0) {
                return;
            }
        }
        List<String> signals = new ArrayList<String>();
        signals.add("by:" + username);
        queue(REPORT, titleId, title.getCreator(), cut(reason, 120), cut(report.detail(), 1000), signals);
    }

    @Transactional(rollbackFor = IOException.class)
    public void appeal(String username, Title title, AppealDto appeal) throws IOException {
        if (!Kinds.REJECTED.equals(title.getStatus()) && !Kinds.REMOVED.equals(title.getStatus())) {
            throw new ResponseStatusException(409, "Only a rejected or removed title can be appealed");
        }
        String text = appeal == null || appeal.text() == null ? "" : appeal.text().trim();
        if (text.length() < 10) {
            throw new ResponseStatusException(400, "Explain the appeal in a sentence or two");
        }
        dropOpen(title.getId(), APPEAL);
        queue(APPEAL, title.getId(), username, "Creator appeal", cut(text, 1000), new ArrayList<String>());
    }

    @Transactional(readOnly = true)
    public List<ModerationItemDto> queue(String kind) {
        List<ModerationItem> rows = kind == null || kind.length() == 0
                ? items.findByStatusOrderByCreatedAtAsc(Kinds.OPEN)
                : items.findByStatusAndKindOrderByCreatedAtAsc(Kinds.OPEN, kind);
        List<ModerationItemDto> out = new ArrayList<ModerationItemDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        // Highest score first, oldest first among equals (the list is oldest
        // first already and the sort is stable).
        for (int i = 1; i < out.size(); i++) {
            ModerationItemDto d = out.get(i);
            int j = i - 1;
            while (j >= 0 && out.get(j).score() < d.score()) {
                out.set(j + 1, out.get(j));
                j--;
            }
            out.set(j + 1, d);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public String status(String subjectId) {
        List<ModerationItem> rows = items.findBySubjectIdOrderByCreatedAtDesc(subjectId);
        return rows.isEmpty() ? "" : rows.get(0).getStatus();
    }

    @Transactional(readOnly = true)
    public int pending() {
        return (int) items.countByStatus(Kinds.OPEN);
    }

    @Transactional(rollbackFor = IOException.class)
    public ModerationItemDto decide(String by, String id, ModerationDecisionDto decision) throws IOException {
        ModerationItem item = items.findById(id);
        if (item == null) {
            throw new ResponseStatusException(404, "No such item");
        }
        if (!Kinds.OPEN.equals(item.getStatus())) {
            throw new ResponseStatusException(409, "Already decided by " + item.getDecidedBy());
        }
        String action = decision == null || decision.action() == null ? "" : decision.action();
        String note = cut(decision == null ? null : decision.note(), 500);
        long now = System.currentTimeMillis();
        String kind = item.getKind();
        if (TITLE.equals(kind) || APPEAL.equals(kind)) {
            Title title = title(item.getSubjectId());
            if (APPROVE.equals(action)) {
                if (hasBlockedVideo(title.getId())) {
                    throw new ResponseStatusException(409, "Clear the blocked video first");
                }
                title.setStatus(Kinds.PUBLISHED);
                title.setModerationNote("");
                if (title.getPublishedAt() == 0) {
                    title.setPublishedAt(now);
                }
                history.record(title.getCreator(), by, APPEAL.equals(kind) ? Moderation.RESTORE : Moderation.APPROVE,
                        title.getName());
            } else if (REJECT.equals(action)) {
                if (note.length() == 0) {
                    throw new ResponseStatusException(400, "Tell the creator why");
                }
                if (TITLE.equals(kind)) {
                    title.setStatus(Kinds.REJECTED);
                }
                title.setModerationNote(note);
                history.record(title.getCreator(), by, Moderation.REJECT, title.getName() + ": " + note);
            } else {
                throw new ResponseStatusException(400, "Approve or reject");
            }
            titles.save(title);
        } else if (REPORT.equals(kind)) {
            if (REMOVE.equals(action)) {
                Title title = title(item.getSubjectId());
                title.setStatus(Kinds.REMOVED);
                title.setModerationNote(note.length() > 0 ? note : item.getReason());
                titles.save(title);
                history.record(title.getCreator(), by, Moderation.TAKEDOWN, title.getName() + ": " + item.getReason());
                closeOthers(item.getSubjectId(), REPORT, by, action, now);
            } else if (!DISMISS.equals(action)) {
                throw new ResponseStatusException(400, "Remove the title or dismiss the report");
            }
        } else if (VIDEO.equals(kind)) {
            Video video = videos.findById(item.getSubjectId());
            if (video != null && APPROVE.equals(action)) {
                // A false positive: the frames only looked like a listed one.
                video.setStatus(Kinds.READY);
                video.setError("");
                videos.save(video);
            } else if (!REJECT.equals(action)) {
                throw new ResponseStatusException(400, "Approve or reject");
            }
        }
        // Closed either way; the decision column says which way.
        item.setStatus(DISMISS.equals(action) ? Kinds.DISMISSED : Kinds.UPHELD);
        item.setDecision(action);
        item.setDecidedBy(by);
        item.setNote(note);
        item.setDecidedAt(now);
        items.save(item);
        return dto(item);
    }

    // ---------------------------------------------------------- blocklist

    @Transactional(readOnly = true)
    public List<BlocklistEntryDto> blocklist() {
        List<BlockedItem> all = blocked.findAll();
        List<BlocklistEntryDto> out = new ArrayList<BlocklistEntryDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            BlockedItem b = all.get(iter);
            out.add(new BlocklistEntryDto(b.getId(), b.getKind(), b.getValue(), b.getNote(), b.getCreatedAt()));
        }
        return out;
    }

    /// Adds a keyword, a frame hash, or every frame hash of a video already
    /// on the service (`video:<id>`), which is how a moderator blocks
    /// re-uploads of something taken down.
    @Transactional(rollbackFor = IOException.class)
    public BlocklistEntryDto block(String by, BlocklistEntryDto entry) throws IOException {
        if (entry == null || entry.value() == null || entry.value().trim().length() == 0) {
            throw new ResponseStatusException(400, "Nothing to block");
        }
        String value = entry.value().trim();
        String note = cut(entry.note(), 200);
        if (KEYWORD.equals(entry.kind())) {
            return add(KEYWORD, fold(value), note, by);
        }
        if (FRAME.equals(entry.kind())) {
            if (value.startsWith("video:")) {
                List<FrameHash> hashes = frames.findByVideoIdOrderByPositionAsc(value.substring(6));
                if (hashes.isEmpty()) {
                    throw new ResponseStatusException(404, "That video has no frame hashes");
                }
                BlocklistEntryDto last = null;
                for (int iter = 0; iter < hashes.size(); iter++) {
                    last = add(FRAME, hashes.get(iter).getHash(), note.length() > 0 ? note : value, by);
                }
                return last;
            }
            if (!hash64(value)) {
                throw new ResponseStatusException(400, "A frame hash is 16 hex digits");
            }
            return add(FRAME, value, note, by);
        }
        throw new ResponseStatusException(400, "Block a KEYWORD or a FRAME");
    }

    @Transactional(rollbackFor = IOException.class)
    public void unblock(String id) throws IOException {
        if (blocked.findById(id) == null) {
            throw new ResponseStatusException(404, "No such entry");
        }
        blocked.deleteById(id);
    }

    // ------------------------------------------------------------ helpers

    private BlocklistEntryDto add(String kind, String value, String note, String by) throws IOException {
        BlockedItem row = new BlockedItem();
        row.setId(Ids.next());
        row.setKind(kind);
        row.setValue(cut(value, 120));
        row.setNote(note);
        row.setCreatedBy(by);
        row.setCreatedAt(System.currentTimeMillis());
        blocked.insert(row);
        return new BlocklistEntryDto(row.getId(), kind, row.getValue(), note, row.getCreatedAt());
    }

    private void queue(String kind, String subject, String owner, String reason, String detail, List<String> signals)
            throws IOException {
        ModerationItem row = new ModerationItem();
        row.setId(Ids.next());
        row.setKind(kind);
        row.setSubjectId(subject);
        row.setOwner(owner == null ? "" : owner);
        row.setReason(cut(reason, 120));
        row.setDetail(cut(detail, 1000));
        row.setSignals(cut(Rows.join(signals), 1000));
        row.setScore(score(kind, signals));
        row.setStatus(Kinds.OPEN);
        row.setCreatedAt(System.currentTimeMillis());
        items.insert(row);
    }

    private static double score(String kind, List<String> signals) {
        double s = REPORT.equals(kind) ? 0.3 : VIDEO.equals(kind) ? 0.9 : 0.1;
        for (int iter = 0; iter < signals.size(); iter++) {
            if (!signals.get(iter).startsWith("by:")) {
                s += 0.2;
            }
        }
        return Math.min(1.0, s);
    }

    private void dropOpen(String subject, String kind) {
        List<ModerationItem> open = items.findBySubjectIdAndStatus(subject, Kinds.OPEN);
        for (int iter = 0; iter < open.size(); iter++) {
            if (kind.equals(open.get(iter).getKind())) {
                open.get(iter).setStatus(Kinds.DISMISSED);
                open.get(iter).setNote("Superseded");
                items.save(open.get(iter));
            }
        }
    }

    private void closeOthers(String subject, String kind, String by, String action, long now) {
        List<ModerationItem> open = items.findBySubjectIdAndStatus(subject, Kinds.OPEN);
        for (int iter = 0; iter < open.size(); iter++) {
            ModerationItem m = open.get(iter);
            if (kind.equals(m.getKind())) {
                m.setStatus(Kinds.UPHELD);
                m.setDecision(action);
                m.setDecidedBy(by);
                m.setDecidedAt(now);
                items.save(m);
            }
        }
    }

    private boolean hasBlockedVideo(String titleId) {
        List<Video> all = videos.findByTitleIdOrderBySeasonAscNumberAsc(titleId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (Kinds.BLOCKED.equals(all.get(iter).getStatus())) {
                return true;
            }
        }
        return false;
    }

    private Title title(String id) {
        Title t = titles.findById(id);
        if (t == null) {
            throw new ResponseStatusException(404, "The title is gone");
        }
        return t;
    }

    /// Whole-word keyword matches in `text`, ignoring ASCII case.
    List<String> keywordSignals(String text) {
        List<String> out = new ArrayList<String>();
        List<BlockedItem> words = blocked.findByKind(KEYWORD);
        if (words.isEmpty()) {
            return out;
        }
        String folded = " " + fold(text).replaceAll("[^a-z0-9]+", " ") + " ";
        for (int iter = 0; iter < words.size(); iter++) {
            String w = words.get(iter).getValue();
            if (w.length() > 0 && folded.indexOf(" " + w + " ") >= 0) {
                out.add("keyword:" + w);
            }
        }
        return out;
    }

    private ModerationItemDto dto(ModerationItem m) {
        String name = "";
        String thumb = "";
        if (VIDEO.equals(m.getKind())) {
            Video v = videos.findById(m.getSubjectId());
            name = v == null ? "" : v.getName();
            thumb = v == null ? "" : "/art/" + v.getId() + "/poster.jpg";
        } else {
            Title t = titles.findById(m.getSubjectId());
            name = t == null ? "" : t.getName();
            thumb = t == null ? "" : t.getPosterUrl();
        }
        return new ModerationItemDto(m.getId(), m.getKind(), m.getSubjectId(), name, m.getOwner(), m.getReason(),
                m.getDetail(), Rows.split(m.getSignals()), m.getScore(), thumb, m.getStatus(), m.getCreatedAt());
    }

    static boolean hash64(String h) {
        if (h == null || h.length() != 16) {
            return false;
        }
        for (int iter = 0; iter < 16; iter++) {
            if (Character.digit(h.charAt(iter), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    static int distance(String a, String b) {
        int d = 0;
        for (int iter = 0; iter < 16; iter++) {
            d += Integer.bitCount(Character.digit(a.charAt(iter), 16) ^ Character.digit(b.charAt(iter), 16));
        }
        return d;
    }

    /// ASCII lower case, by hand: blocklist words are compared without the
    /// locale (see the project's rule on `toLowerCase`).
    static String fold(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int iter = 0; iter < s.length(); iter++) {
            char c = s.charAt(iter);
            out.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return out.toString();
    }

    private static String cut(String value, int max) {
        String t = value == null ? "" : value.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
