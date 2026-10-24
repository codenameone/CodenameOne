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

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.reel.Caller;
import com.codenameone.examples.reel.api.AppealDto;
import com.codenameone.examples.reel.api.ChaptersDto;
import com.codenameone.examples.reel.api.InteractionsDto;
import com.codenameone.examples.reel.api.JobDto;
import com.codenameone.examples.reel.api.StudioApiServer;
import com.codenameone.examples.reel.api.StudioTitleDto;
import com.codenameone.examples.reel.api.StudioTitleEditDto;
import com.codenameone.examples.reel.api.StudioVideoDto;
import com.codenameone.examples.reel.api.StudioVideoEditDto;
import com.codenameone.examples.reel.api.SubtitleUploadDto;
import com.codenameone.examples.reel.api.TitleAnalyticsDto;
import com.codenameone.examples.reel.api.UploadDto;
import com.codenameone.examples.reel.api.UploadStartDto;
import com.codenameone.examples.reel.domain.Title;
import com.codenameone.examples.reel.domain.Video;
import com.codenameone.examples.reel.repository.TitleRepository;
import com.codenameone.examples.reel.repository.VideoRepository;

import java.util.ArrayList;
import java.util.List;

/// Every studio call starts with [Studio#requireCreator]: the role rule in
/// `SecurityConfig` cannot express "or anybody, when open uploads are on".
@Component
public class StudioEndpoint implements StudioApiServer {
    private final Studio studio;
    private final Uploads uploads;
    private final Jobs jobs;
    private final TitleRepository titles;
    private final VideoRepository videos;

    public StudioEndpoint(Studio studio, Uploads uploads, Jobs jobs, TitleRepository titles,
            VideoRepository videos) {
        this.studio = studio;
        this.uploads = uploads;
        this.jobs = jobs;
        this.titles = titles;
        this.videos = videos;
    }

    private String who() {
        studio.requireCreator();
        return Caller.name();
    }

    @Override
    public List<StudioTitleDto> titles() throws Exception {
        return studio.mine(who());
    }

    @Override
    public StudioTitleDto title(String id) throws Exception {
        return studio.title(who(), id);
    }

    @Override
    public StudioTitleDto createTitle(StudioTitleEditDto title) throws Exception {
        return studio.create(who(), title);
    }

    @Override
    public StudioTitleDto updateTitle(String id, StudioTitleEditDto title) throws Exception {
        return studio.update(who(), id, title);
    }

    @Override
    public StudioTitleDto submit(String id) throws Exception {
        return studio.submit(who(), id);
    }

    @Override
    public StudioTitleDto unpublish(String id) throws Exception {
        return studio.unpublish(who(), id);
    }

    @Override
    public StudioTitleDto appeal(String id, AppealDto appeal) throws Exception {
        return studio.appeal(who(), id, appeal);
    }

    @Override
    public StudioVideoDto addVideo(String id, StudioVideoEditDto video) throws Exception {
        return studio.addVideo(who(), id, video);
    }

    @Override
    public StudioVideoDto updateVideo(String id, StudioVideoEditDto video) throws Exception {
        return studio.updateVideo(who(), id, video);
    }

    @Override
    public String deleteVideo(String id) throws Exception {
        studio.deleteVideo(who(), id);
        return "ok";
    }

    @Override
    public StudioVideoDto setChapters(String id, ChaptersDto chapters) throws Exception {
        return studio.setChapters(who(), id, chapters);
    }

    @Override
    public StudioVideoDto setSubtitle(String id, String lang, SubtitleUploadDto subtitle) throws Exception {
        return studio.setSubtitle(who(), id, lang, subtitle);
    }

    @Override
    public StudioVideoDto deleteSubtitle(String id, String lang) throws Exception {
        return studio.deleteSubtitle(who(), id, lang);
    }

    @Override
    public StudioVideoDto setInteractions(String id, InteractionsDto interactions) throws Exception {
        return studio.setInteractions(who(), id, interactions);
    }

    @Override
    public UploadDto startUpload(UploadStartDto upload) throws Exception {
        return uploads.start(who(), upload);
    }

    @Override
    public UploadDto upload(String id) throws Exception {
        return uploads.get(who(), id);
    }

    @Override
    public List<JobDto> jobs() throws Exception {
        String me = who();
        List<Title> mine = titles.findByCreatorOrderByCreatedAtDesc(me);
        List<Video> all = new ArrayList<Video>();
        for (int iter = 0; iter < mine.size(); iter++) {
            all.addAll(videos.findByTitleIdOrderBySeasonAscNumberAsc(mine.get(iter).getId()));
        }
        return jobs.forVideos(all);
    }

    @Override
    public TitleAnalyticsDto analytics(String id, int days) throws Exception {
        return studio.analytics(who(), id, days);
    }
}
