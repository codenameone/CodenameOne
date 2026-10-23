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
import com.codenameone.examples.reel.api.UploadDto;
import com.codenameone.examples.reel.api.UploadStartDto;
import com.codenameone.examples.reel.domain.Upload;
import com.codenameone.examples.reel.domain.Video;
import com.codenameone.examples.reel.repository.UploadRepository;
import com.codenameone.examples.reel.repository.VideoRepository;
import com.codename1.security.Hash;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/// Resumable uploads of a video's source file.
///
/// The app starts an upload with the file's size (and optionally its SHA-256),
/// then sends it in pieces of at most [Jobs#CHUNK_BYTES], each at the offset
/// the server already has. A dropped connection resumes from
/// `receivedBytes`; a piece sent twice is refused, never appended twice. The
/// last piece moves the file to `{reel.upload.dir}/{video}`, checks the hash,
/// and queues the video for packaging.
@Component
public class Uploads {
    public static final String UPLOADING = "UPLOADING";
    public static final String COMPLETE = "COMPLETE";

    private final UploadRepository uploads;
    private final VideoRepository videos;
    private final Studio studio;
    private final Jobs jobs;
    private final String dir;
    private final long maxBytes;

    public Uploads(UploadRepository uploads, VideoRepository videos, Studio studio, Jobs jobs,
            @Value("${reel.upload.dir:uploads}") String dir,
            @Value("${reel.upload.max-mb:4096}") String maxMb) {
        this.uploads = uploads;
        this.videos = videos;
        this.studio = studio;
        this.jobs = jobs;
        this.dir = dir;
        this.maxBytes = Long.parseLong(maxMb.trim()) * 1024L * 1024L;
    }

    public String directory() {
        return dir;
    }

    @Transactional(rollbackFor = IOException.class)
    public UploadDto start(String username, UploadStartDto wanted) throws IOException {
        if (wanted == null || wanted.videoId() == null) {
            throw new ResponseStatusException(400, "Choose the video this file is for");
        }
        Video video = studio.ownVideo(username, wanted.videoId());
        if (Kinds.PROCESSING.equals(video.getStatus()) || Kinds.QUEUED.equals(video.getStatus())) {
            throw new ResponseStatusException(409, "This video is being processed; wait for it to finish");
        }
        if (wanted.totalBytes() <= 0 || wanted.totalBytes() > maxBytes) {
            throw new ResponseStatusException(413, "A video file is at most " + (maxBytes >> 20) + " MB");
        }
        String sha = wanted.sha256() == null ? "" : wanted.sha256().trim();
        if (sha.length() != 0 && sha.length() != 64) {
            throw new ResponseStatusException(400, "The SHA-256 is 64 hex digits");
        }
        long now = System.currentTimeMillis();
        Upload row = new Upload();
        row.setId(Ids.next());
        row.setVideoId(video.getId());
        row.setUsername(username);
        String name = wanted.fileName() == null ? "video" : wanted.fileName().trim();
        row.setFileName(name.length() > 120 ? name.substring(name.length() - 120) : name);
        row.setTotalBytes(wanted.totalBytes());
        row.setSha256(sha);
        row.setStatus(UPLOADING);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        uploads.insert(row);
        new File(dir).mkdirs();
        part(row).delete();
        video.setStatus(Kinds.UPLOADING);
        video.setProgressPercent(0);
        videos.save(video);
        return dto(row);
    }

    @Transactional(readOnly = true)
    public UploadDto get(String username, String id) {
        return dto(own(username, id));
    }

    @Transactional(rollbackFor = IOException.class)
    public UploadDto chunk(String username, String id, long offset, byte[] body) throws IOException {
        Upload row = own(username, id);
        if (!UPLOADING.equals(row.getStatus())) {
            throw new ResponseStatusException(409, "This upload is finished");
        }
        if (offset != row.getReceivedBytes()) {
            throw new ResponseStatusException(409, "Resume at " + row.getReceivedBytes());
        }
        if (body == null || body.length == 0 || body.length > Jobs.CHUNK_BYTES
                || offset + body.length > row.getTotalBytes()) {
            throw new ResponseStatusException(400, "Send 1 to " + Jobs.CHUNK_BYTES + " bytes, within the file");
        }
        File f = part(row);
        if (f.length() != offset) {
            // The file and the row disagree (a crash between the two): trust
            // neither and make the app start over.
            row.setStatus("FAILED");
            uploads.save(row);
            throw new ResponseStatusException(409, "The upload was interrupted; start it again");
        }
        FileOutputStream out = new FileOutputStream(f, true);
        try {
            out.write(body);
        } finally {
            out.close();
        }
        row.setReceivedBytes(offset + body.length);
        row.setUpdatedAt(System.currentTimeMillis());
        Video video = videos.findById(row.getVideoId());
        if (row.getReceivedBytes() == row.getTotalBytes()) {
            if (row.getSha256().length() > 0 && !row.getSha256().equalsIgnoreCase(sha256(f))) {
                f.delete();
                row.setStatus("FAILED");
                uploads.save(row);
                throw new ResponseStatusException(422, "The file arrived damaged; upload it again");
            }
            File source = new File(dir, row.getVideoId());
            source.delete();
            if (!f.renameTo(source)) {
                throw new IOException("Could not keep the uploaded file");
            }
            row.setStatus(COMPLETE);
            if (video != null) {
                video.setStatus(Kinds.QUEUED);
                video.setProgressPercent(0);
                videos.save(video);
            }
            jobs.enqueue(row.getVideoId());
        } else if (video != null) {
            video.setProgressPercent((int) (row.getReceivedBytes() * 100L / row.getTotalBytes()));
            videos.save(video);
        }
        uploads.save(row);
        return dto(row);
    }

    private Upload own(String username, String id) {
        Upload row = id == null ? null : uploads.findById(id);
        if (row == null || !row.getUsername().equals(username)) {
            throw new ResponseStatusException(404, "No such upload");
        }
        return row;
    }

    private File part(Upload row) {
        return new File(dir, row.getVideoId() + "." + row.getId() + ".part");
    }

    private static String sha256(File f) throws IOException {
        Hash h = Hash.create(Hash.SHA256);
        InputStream in = new FileInputStream(f);
        try {
            byte[] buf = new byte[65536];
            int n;
            while ((n = in.read(buf)) > 0) {
                h.update(buf, 0, n);
            }
        } finally {
            in.close();
        }
        return Ids.hex(h.digest());
    }

    static UploadDto dto(Upload u) {
        return new UploadDto(u.getId(), u.getVideoId(), u.getFileName(), u.getTotalBytes(), u.getReceivedBytes(),
                Jobs.CHUNK_BYTES, u.getStatus());
    }
}
