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
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.reel.Ids;
import com.codenameone.examples.reel.Kinds;
import com.codenameone.examples.reel.Rows;
import com.codenameone.examples.reel.api.JobDto;
import com.codenameone.examples.reel.api.WorkerJobDto;
import com.codenameone.examples.reel.api.WorkerProgressDto;
import com.codenameone.examples.reel.api.WorkerResultDto;
import com.codenameone.examples.reel.domain.Job;
import com.codenameone.examples.reel.domain.Title;
import com.codenameone.examples.reel.domain.Video;
import com.codenameone.examples.reel.domain.VideoKey;
import com.codenameone.examples.reel.moderation.Reviews;
import com.codenameone.examples.reel.playback.MediaOrigin;
import com.codenameone.examples.reel.repository.JobRepository;
import com.codenameone.examples.reel.repository.TitleRepository;
import com.codenameone.examples.reel.repository.VideoKeyRepository;
import com.codenameone.examples.reel.repository.VideoRepository;

import java.time.Instant;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// The packaging queue the workers (`tools/packager worker`) pull from.
///
/// A worker claims the oldest queued job and holds a lease on it that every
/// progress report renews. A worker that dies lets its lease run out and the
/// job is handed to the next one that asks, up to [#MAX_ATTEMPTS] times. The
/// worker uploads what it produced into a staging directory of the job's own,
/// and only `done` moves it into place in one rename, so a player never sees a
/// half-packaged video and a re-packaged one replaces the old files whole.
///
/// The worker never sees the server's database; it gets the source and the
/// video's key with the job, which is why the worker endpoints are behind a
/// token of their own and must only be reachable by your workers.
@Component
public class Jobs {
    public static final String PACKAGE = "PACKAGE";
    public static final String RUNNING = "RUNNING";
    public static final String DONE = "DONE";
    public static final int MAX_ATTEMPTS = 3;
    public static final long LEASE_MILLIS = 10L * 60L * 1000L;
    /// The largest piece of a file a worker sends in one request; the HTTP
    /// server caps a request body at 8 MiB.
    public static final int CHUNK_BYTES = 4 * 1024 * 1024;
    private static final List<String> RENDITIONS = Arrays.asList("720", "480", "360");

    private final JobRepository jobs;
    private final VideoRepository videos;
    private final VideoKeyRepository keys;
    private final TitleRepository titles;
    private final Reviews reviews;
    private final String mediaDir;

    public Jobs(JobRepository jobs, VideoRepository videos, VideoKeyRepository keys, TitleRepository titles,
            Reviews reviews, @Value("${reel.media.dir:media}") String mediaDir) {
        this.jobs = jobs;
        this.videos = videos;
        this.keys = keys;
        this.titles = titles;
        this.reviews = reviews;
        this.mediaDir = mediaDir;
    }

    @Transactional(rollbackFor = IOException.class)
    public Job enqueue(String videoId) throws IOException {
        Job job = new Job();
        job.setId(Ids.next());
        job.setVideoId(videoId);
        job.setKind(PACKAGE);
        job.setStatus(Kinds.QUEUED);
        job.setCreatedAt(Instant.now());
        jobs.insert(job);
        return job;
    }

    /// The next job for a worker, or null when there is none.
    @Transactional(rollbackFor = IOException.class)
    public WorkerJobDto claim(String worker) throws IOException {
        Instant now = Instant.now();
        Job job = null;
        List<Job> lapsed = jobs.findByStatusAndLeaseUntilLessThan(RUNNING, now);
        for (int iter = 0; iter < lapsed.size(); iter++) {
            Job j = lapsed.get(iter);
            if (j.getAttempts() >= MAX_ATTEMPTS) {
                failFor(j, "The worker stopped reporting " + MAX_ATTEMPTS + " times", now);
            } else if (job == null) {
                job = j;
            }
        }
        if (job == null) {
            List<Job> queued = jobs.findByStatusOrderByCreatedAtAsc(Kinds.QUEUED);
            if (queued.isEmpty()) {
                return null;
            }
            job = queued.get(0);
        }
        VideoKey key = keys.findById(job.getVideoId());
        Video video = videos.findById(job.getVideoId());
        if (key == null || video == null) {
            failFor(job, "The video was deleted", now);
            return null;
        }
        job.setStatus(RUNNING);
        job.setWorker(cut(worker, 64));
        job.setAttempts(job.getAttempts() + 1);
        job.setStartedAt(now);
        job.setLeaseUntil(now.plusMillis(LEASE_MILLIS));
        job.setProgressPercent(0);
        jobs.save(job);
        video.setStatus(Kinds.PROCESSING);
        video.setProgressPercent(0);
        videos.save(video);
        clear(staging(job.getId()));
        return new WorkerJobDto(job.getId(), job.getVideoId(), job.getKind(), "/worker/source/" + job.getVideoId(),
                key.getKeyId(), key.getKeyHex(), key.getIvHex(), RENDITIONS, 6);
    }

    @Transactional(rollbackFor = IOException.class)
    public void progress(String id, WorkerProgressDto p) throws IOException {
        Job job = running(id);
        int pct = p == null ? 0 : Math.max(0, Math.min(100, p.progressPercent()));
        job.setProgressPercent(pct);
        job.setLeaseUntil(Instant.now().plusMillis(LEASE_MILLIS));
        jobs.save(job);
        Video video = videos.findById(job.getVideoId());
        if (video != null) {
            video.setProgressPercent(pct);
            videos.save(video);
        }
    }

    /// Appends one piece of an output file. `offset` must be where the file
    /// ends now, so a retried piece is refused rather than written twice.
    @Transactional(readOnly = true)
    public void file(String id, String name, long offset, byte[] body) throws IOException {
        running(id);
        if (!outputName(name)) {
            throw new ResponseStatusException(400, "Not a file the packager writes: " + name);
        }
        if (body == null || body.length > CHUNK_BYTES) {
            throw new ResponseStatusException(413, "Send at most " + CHUNK_BYTES + " bytes at a time");
        }
        File dir = staging(id);
        dir.mkdirs();
        File f = new File(dir, name);
        long have = f.exists() ? f.length() : 0L;
        if (offset != have) {
            throw new ResponseStatusException(409, "The file is " + have + " bytes long, not " + offset);
        }
        FileOutputStream out = new FileOutputStream(f, true);
        try {
            out.write(body);
        } finally {
            out.close();
        }
    }

    @Transactional(rollbackFor = IOException.class)
    public void done(String id, WorkerResultDto result) throws IOException {
        Job job = running(id);
        File dir = staging(id);
        if (!new File(dir, "master.m3u8").exists() || !new File(dir, "cenc.mp4").exists()) {
            throw new ResponseStatusException(409, "The playlists and cenc.mp4 have not all arrived");
        }
        Instant now = Instant.now();
        Video video = videos.findById(job.getVideoId());
        if (video == null) {
            failFor(job, "The video was deleted", now);
            return;
        }
        File target = new File(mediaDir, video.getId());
        if (target.exists()) {
            File old = new File(mediaDir, ".old/" + video.getId() + "-" + now);
            old.getParentFile().mkdirs();
            if (!target.renameTo(old)) {
                throw new IOException("Could not move the old files of " + video.getId() + " aside");
            }
        }
        if (!dir.renameTo(target)) {
            throw new IOException("Could not move the packaged files of " + video.getId() + " into place");
        }
        video.setDurationSeconds(result == null ? 0 : Math.max(0, result.durationSeconds()));
        video.setWidth(result == null ? 0 : result.width());
        video.setHeight(result == null ? 0 : result.height());
        video.setRenditions(Rows.join(result == null ? null : result.renditions()));
        // The packager writes an AES-128 HLS ladder and a CENC (cenc-aes-ctr)
        // fragmented MP4. CENC plays with ClearKey, and with Widevine once a
        // license service wraps the key. FairPlay needs SAMPLE-AES (cbcs)
        // packaging, which this packager does not produce.
        video.setSchemes("AES_128,CLEARKEY,WIDEVINE");
        video.setThumbnailUrl("/art/" + video.getId() + "/poster.jpg");
        video.setProgressPercent(100);
        video.setError("");
        Title title = titles.findById(video.getTitleId());
        String owner = title == null ? "" : title.getCreator();
        boolean blocked = reviews.screenFrames(video, owner, result == null ? null : result.frameHashes());
        video.setStatus(blocked ? Kinds.BLOCKED : Kinds.READY);
        if (blocked) {
            video.setError("Held for review: frames match the blocklist");
        }
        videos.save(video);
        // A new or re-packaged video of a published title is new footage
        // nobody approved. The worker carries no user, so even an admin's
        // upload is looked at.
        reviews.changed(title, false);
        if (title != null && title.getPosterUrl().length() == 0 && !Kinds.TRAILER.equals(video.getRole())) {
            title.setPosterUrl("/art/" + video.getId() + "/poster.jpg");
            title.setBackdropUrl("/art/" + video.getId() + "/poster.jpg");
            titles.save(title);
        }
        job.setStatus(DONE);
        job.setProgressPercent(100);
        job.setFinishedAt(now);
        jobs.save(job);
    }

    @Transactional(rollbackFor = IOException.class)
    public void fail(String id, WorkerProgressDto why) throws IOException {
        Job job = running(id);
        Instant now = Instant.now();
        String error = why == null || why.error() == null ? "The worker failed" : why.error();
        if (job.getAttempts() < MAX_ATTEMPTS) {
            job.setStatus(Kinds.QUEUED);
            job.setError(cut(error, 255));
            job.setLeaseUntil(Instant.EPOCH);
            jobs.save(job);
            return;
        }
        failFor(job, error, now);
    }

    @Transactional(rollbackFor = IOException.class)
    public JobDto retry(String id) throws IOException {
        Job job = jobs.findById(id);
        if (job == null) {
            throw new ResponseStatusException(404, "No such job");
        }
        if (!Kinds.FAILED.equals(job.getStatus())) {
            throw new ResponseStatusException(409, "Only a failed job can be retried");
        }
        job.setStatus(Kinds.QUEUED);
        job.setAttempts(0);
        job.setError("");
        job.setLeaseUntil(Instant.EPOCH);
        jobs.save(job);
        Video video = videos.findById(job.getVideoId());
        if (video != null) {
            video.setStatus(Kinds.QUEUED);
            video.setError("");
            videos.save(video);
        }
        return dto(job);
    }

    @Transactional(readOnly = true)
    public List<JobDto> list(String status) {
        List<Job> rows = status == null || status.length() == 0 ? jobs.findTop200ByCreatedAtGreaterThanOrderByCreatedAtDesc(Instant.EPOCH)
                : jobs.findByStatusOrderByCreatedAtAsc(status);
        List<JobDto> out = new ArrayList<JobDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(dto(rows.get(iter)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<JobDto> forVideos(List<Video> mine) {
        List<JobDto> out = new ArrayList<JobDto>();
        for (int iter = 0; iter < mine.size(); iter++) {
            List<Job> rows = jobs.findByVideoIdOrderByCreatedAtDesc(mine.get(iter).getId());
            if (!rows.isEmpty()) {
                out.add(dto(rows.get(0)));
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public int count(String status) {
        return (int) jobs.countByStatus(status);
    }

    private void failFor(Job job, String error, Instant now) {
        job.setStatus(Kinds.FAILED);
        job.setError(cut(error, 255));
        job.setFinishedAt(now);
        jobs.save(job);
        Video video = videos.findById(job.getVideoId());
        if (video != null) {
            video.setStatus(Kinds.FAILED);
            video.setError(cut(error, 255));
            videos.save(video);
        }
    }

    private Job running(String id) {
        Job job = id == null ? null : jobs.findById(id);
        if (job == null || !RUNNING.equals(job.getStatus())) {
            throw new ResponseStatusException(409, "That job is not running");
        }
        return job;
    }

    private File staging(String jobId) {
        return new File(mediaDir, ".staging/" + jobId);
    }

    private static void clear(File dir) {
        File[] files = dir.listFiles();
        for (int iter = 0; files != null && iter < files.length; iter++) {
            files[iter].delete();
        }
    }

    static boolean outputName(String name) {
        return name != null && ("poster.jpg".equals(name) || "sprite.jpg".equals(name)
                || MediaOrigin.renditionHeight(name) >= 0);
    }

    static JobDto dto(Job j) {
        return new JobDto(j.getId(), j.getVideoId(), j.getKind(), j.getStatus(), j.getProgressPercent(),
                j.getWorker(), j.getError(), j.getCreatedAt().toEpochMilli(), j.getStartedAt().toEpochMilli(),
                j.getFinishedAt().toEpochMilli());
    }

    private static String cut(String value, int max) {
        String t = value == null ? "" : value.trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
