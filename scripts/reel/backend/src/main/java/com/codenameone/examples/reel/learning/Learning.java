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
package com.codenameone.examples.reel.learning;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.reel.Ids;
import com.codenameone.examples.reel.Kinds;
import com.codenameone.examples.reel.Rows;
import com.codenameone.examples.reel.account.Accounts;
import com.codenameone.examples.reel.account.Households;
import com.codenameone.examples.reel.api.AnswerDto;
import com.codenameone.examples.reel.api.AnswerResultDto;
import com.codenameone.examples.reel.api.CertificateDto;
import com.codenameone.examples.reel.api.CourseProgressDto;
import com.codenameone.examples.reel.api.Features;
import com.codenameone.examples.reel.domain.Answer;
import com.codenameone.examples.reel.domain.Certificate;
import com.codenameone.examples.reel.domain.Household;
import com.codenameone.examples.reel.domain.Interaction;
import com.codenameone.examples.reel.domain.Playback;
import com.codenameone.examples.reel.domain.Progress;
import com.codenameone.examples.reel.domain.Title;
import com.codenameone.examples.reel.domain.Video;
import com.codenameone.examples.reel.repository.AnswerRepository;
import com.codenameone.examples.reel.repository.CertificateRepository;
import com.codenameone.examples.reel.repository.InteractionRepository;
import com.codenameone.examples.reel.repository.PlaybackRepository;
import com.codenameone.examples.reel.repository.ProgressRepository;
import com.codenameone.examples.reel.repository.TitleRepository;
import com.codenameone.examples.reel.repository.VideoRepository;
import com.codenameone.examples.reel.settings.Settings;
import com.codename1.backend.Crypto;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// The academy: answers to quizzes, polls and branches, course progress and
/// certificates.
///
/// Answers are checked here, never in the app: the player receives the
/// questions without their answers ([com.codenameone.examples.reel.playback.Playbacks]).
/// A viewer's latest answer to a question is the one that counts, so a quiz
/// can be retaken. A course's score is the share of its quiz questions whose
/// latest answer is right; a certificate is issued once every lesson is
/// complete and the score reaches the course's pass mark. Each certificate has
/// a short public code anyone can check at `/verify/{code}`.
@Component
public class Learning {
    public static final String QUIZ = "QUIZ";
    public static final String POLL = "POLL";
    public static final String BRANCH = "BRANCH";
    /// Unambiguous characters for a verification code.
    private static final String CODE = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final AnswerRepository answers;
    private final InteractionRepository interactions;
    private final VideoRepository videos;
    private final TitleRepository titles;
    private final ProgressRepository progress;
    private final CertificateRepository certificates;
    private final PlaybackRepository sessions;
    private final Households households;
    private final Accounts accounts;
    private final Settings settings;

    public Learning(AnswerRepository answers, InteractionRepository interactions, VideoRepository videos,
            TitleRepository titles, ProgressRepository progress, CertificateRepository certificates,
            PlaybackRepository sessions, Households households, Accounts accounts, Settings settings) {
        this.answers = answers;
        this.interactions = interactions;
        this.videos = videos;
        this.titles = titles;
        this.progress = progress;
        this.certificates = certificates;
        this.sessions = sessions;
        this.households = households;
        this.accounts = accounts;
        this.settings = settings;
    }

    @Transactional(rollbackFor = IOException.class)
    public AnswerResultDto answer(String username, AnswerDto dto) throws IOException {
        settings.require(Features.INTERACTIVE);
        Interaction q = dto == null || dto.interactionId() == null ? null : interactions.findById(dto.interactionId());
        if (q == null) {
            throw new ResponseStatusException(404, "No such question");
        }
        Video video = videos.findById(q.getVideoId());
        Title title = video == null ? null : titles.findById(video.getTitleId());
        if (title == null) {
            throw new ResponseStatusException(404, "No such question");
        }
        // The answer is tied to the profile of the session it was given in, so
        // a household's profiles keep separate scores.
        // It also proves the caller was entitled to the video: a question is
        // only answered from inside a playback of it.
        Playback s = dto.sessionId() == null ? null : sessions.findById(dto.sessionId());
        if (s == null || !s.getUsername().equals(username) || !s.getVideoId().equals(video.getId())) {
            throw new ResponseStatusException(404, "No such session");
        }
        String profileId = s.getProfileId();
        List<String> options = Rows.splitLines(q.getOptions());
        List<Integer> chosen = dto.choices() == null ? new ArrayList<Integer>() : dto.choices();
        if (chosen.isEmpty()) {
            throw new ResponseStatusException(400, "Choose an answer");
        }
        Set<Integer> picked = new HashSet<Integer>();
        for (int iter = 0; iter < chosen.size(); iter++) {
            Integer c = chosen.get(iter);
            if (c == null || c.intValue() < 0 || c.intValue() >= options.size()) {
                throw new ResponseStatusException(400, "That option does not exist");
            }
            picked.add(c);
        }
        if ((POLL.equals(q.getKind()) || BRANCH.equals(q.getKind())) && picked.size() != 1) {
            throw new ResponseStatusException(400, "Choose one option");
        }
        Set<Integer> right = new HashSet<Integer>(Rows.ints(q.getAnswers()));
        boolean correct = QUIZ.equals(q.getKind()) ? right.equals(picked) : true;

        // A quiz answer is final. Every answer is told whether it was right,
        // so a quiz that took another would be passed by trying each option
        // in turn -- and the certificate would say nothing. A poll or a
        // branch can be changed.
        List<Answer> previous = answers.findByUsernameAndInteractionId(username, q.getId());
        for (int iter = 0; iter < previous.size(); iter++) {
            if (previous.get(iter).getProfileId().equals(profileId)) {
                if (QUIZ.equals(q.getKind())) {
                    throw new ResponseStatusException(409, "This question was already answered");
                }
                answers.deleteById(previous.get(iter).getId());
            }
        }
        Answer row = new Answer();
        row.setId(Ids.next());
        row.setInteractionId(q.getId());
        row.setVideoId(video.getId());
        row.setTitleId(title.getId());
        row.setUsername(username);
        row.setProfileId(profileId);
        row.setChoices(Rows.joinInts(new ArrayList<Integer>(picked)));
        row.setCorrect(correct);
        row.setCreatedAt(System.currentTimeMillis());
        answers.insert(row);

        List<Integer> tally = new ArrayList<Integer>();
        if (POLL.equals(q.getKind())) {
            int[] counts = new int[options.size()];
            List<Answer> all = answers.findByInteractionId(q.getId());
            for (int iter = 0; iter < all.size(); iter++) {
                List<Integer> c = Rows.ints(all.get(iter).getChoices());
                for (int k = 0; k < c.size(); k++) {
                    if (c.get(k).intValue() < counts.length) {
                        counts[c.get(k).intValue()]++;
                    }
                }
            }
            for (int iter = 0; iter < counts.length; iter++) {
                tally.add(Integer.valueOf(counts[iter]));
            }
        }
        String next = null;
        if (BRANCH.equals(q.getKind())) {
            List<String> targets = Rows.split(q.getBranchTargets());
            int at = picked.iterator().next().intValue();
            next = at < targets.size() ? targets.get(at) : null;
        }
        int score = score(username, profileId, title.getId());
        if (QUIZ.equals(q.getKind())) {
            maybeCertify(username, profileId, title);
        }
        return new AnswerResultDto(correct, QUIZ.equals(q.getKind()) ? q.getExplanation() : "", tally, next, score);
    }

    @Transactional(rollbackFor = IOException.class)
    public List<CourseProgressDto> progress(String username, String profileId) throws IOException {
        Household profile = households.resolve(username, profileId);
        List<Progress> rows = progress.findTop50ByProfileIdOrderByUpdatedAtDesc(profile.getId());
        Set<String> seen = new HashSet<String>();
        List<CourseProgressDto> out = new ArrayList<CourseProgressDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Title t = titles.findById(rows.get(iter).getTitleId());
            if (t == null || !Kinds.COURSE.equals(t.getKind()) || !seen.add(t.getId())) {
                continue;
            }
            int[] lessons = lessons(profile.getId(), t.getId());
            Certificate cert = maybeCertify(username, profile.getId(), t);
            out.add(new CourseProgressDto(t.getId(), t.getName(), lessons[0], lessons[1],
                    score(username, profile.getId(), t.getId()), cert != null, cert == null ? null : cert.getId()));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<CertificateDto> certificates(String username) {
        List<Certificate> rows = certificates.findByUsernameOrderByIssuedAtDesc(username);
        List<CertificateDto> out = new ArrayList<CertificateDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    /// Public: anyone holding a code can check it.
    @Transactional(readOnly = true)
    public CertificateDto verify(String code) {
        Certificate c = code == null ? null : certificates.findFirstByVerifyCode(code.trim());
        if (c == null) {
            throw new ResponseStatusException(404, "No certificate has that code");
        }
        return dto(c);
    }

    /// Issues the certificate of a course once it is earned; answers the
    /// existing one when it already was.
    Certificate maybeCertify(String username, String profileId, Title t) throws IOException {
        List<Certificate> have = certificates.findByUsernameAndTitleId(username, t.getId());
        if (!have.isEmpty()) {
            return have.get(0);
        }
        if (!t.isCertificate() || !settings.enabled(Features.CERTIFICATES)) {
            return null;
        }
        int[] lessons = lessons(profileId, t.getId());
        int score = score(username, profileId, t.getId());
        if (lessons[0] == 0 || lessons[1] < lessons[0] || score < t.getPassPercent()) {
            return null;
        }
        Certificate c = new Certificate();
        c.setId(Ids.next());
        c.setUsername(username);
        c.setTitleId(t.getId());
        c.setHolderName(accounts.displayName(username));
        c.setScorePercent(score);
        c.setIssuedAt(System.currentTimeMillis());
        c.setVerifyCode(code());
        certificates.insert(c);
        return c;
    }

    /// {total lessons, completed lessons} of a course for a profile.
    private int[] lessons(String profileId, String titleId) {
        List<Video> all = videos.findByTitleIdOrderBySeasonAscNumberAsc(titleId);
        Set<String> lessonIds = new HashSet<String>();
        for (int iter = 0; iter < all.size(); iter++) {
            Video v = all.get(iter);
            if (Kinds.READY.equals(v.getStatus()) && !Kinds.TRAILER.equals(v.getRole())) {
                lessonIds.add(v.getId());
            }
        }
        int done = 0;
        List<Progress> rows = progress.findByProfileIdAndTitleId(profileId, titleId);
        for (int iter = 0; iter < rows.size(); iter++) {
            if (rows.get(iter).isCompleted() && lessonIds.contains(rows.get(iter).getVideoId())) {
                done++;
            }
        }
        return new int[] {lessonIds.size(), done};
    }

    /// The share of a course's quiz questions whose latest answer is right.
    private int score(String username, String profileId, String titleId) {
        int total = 0;
        int right = 0;
        List<Video> all = videos.findByTitleIdOrderBySeasonAscNumberAsc(titleId);
        List<Answer> mine = answers.findByUsernameAndTitleId(username, titleId);
        for (int v = 0; v < all.size(); v++) {
            List<Interaction> qs = interactions.findByVideoIdOrderByAtSecondsAsc(all.get(v).getId());
            for (int iter = 0; iter < qs.size(); iter++) {
                if (!QUIZ.equals(qs.get(iter).getKind())) {
                    continue;
                }
                total++;
                for (int a = 0; a < mine.size(); a++) {
                    Answer ans = mine.get(a);
                    if (ans.getInteractionId().equals(qs.get(iter).getId()) && ans.getProfileId().equals(profileId)
                            && ans.isCorrect()) {
                        right++;
                        break;
                    }
                }
            }
        }
        return total == 0 ? 100 : right * 100 / total;
    }

    private CertificateDto dto(Certificate c) {
        Title t = titles.findById(c.getTitleId());
        return new CertificateDto(c.getId(), c.getTitleId(), t == null ? "" : t.getName(), c.getHolderName(),
                c.getScorePercent(), c.getIssuedAt(), c.getVerifyCode(), "/verify/" + c.getVerifyCode());
    }

    private static String code() throws IOException {
        byte[] random = Crypto.randomBytes(10);
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < random.length; iter++) {
            if (iter == 5) {
                out.append('-');
            }
            out.append(CODE.charAt((random[iter] & 0xff) % CODE.length()));
        }
        return out.toString();
    }
}
