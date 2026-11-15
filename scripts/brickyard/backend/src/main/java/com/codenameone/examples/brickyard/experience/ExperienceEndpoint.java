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
package com.codenameone.examples.brickyard.experience;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.brickyard.api.ExperienceApiServer;
import com.codenameone.examples.brickyard.api.ExperienceDto;
import com.codenameone.examples.brickyard.api.ExperienceSaveDto;
import com.codenameone.examples.brickyard.api.PlaceDto;
import com.codenameone.examples.brickyard.api.PlaceUploadDto;
import com.codenameone.examples.brickyard.api.RollbackDto;
import com.codenameone.examples.brickyard.api.VersionDto;
import com.codenameone.examples.brickyard.api.VoteDto;

import java.util.List;

/// The experience routes; the work is in [Experiences].
@Component
public class ExperienceEndpoint implements ExperienceApiServer {
    private final Experiences experiences;

    public ExperienceEndpoint(Experiences experiences) {
        this.experiences = experiences;
    }

    @Override
    public ExperienceDto create(ExperienceSaveDto body) throws Exception {
        return experiences.create(Caller.name(), body);
    }

    @Override
    public List<ExperienceDto> browse(String genre) throws Exception {
        return experiences.browse(genre);
    }

    @Override
    public ExperienceDto experience(String id) throws Exception {
        return experiences.describe(id);
    }

    @Override
    public VersionDto saveDraft(String id, PlaceUploadDto body) throws Exception {
        return experiences.upload(id, body == null ? null : body.data(), false);
    }

    @Override
    public VersionDto publish(String id, PlaceUploadDto body) throws Exception {
        return experiences.upload(id, body == null ? null : body.data(), true);
    }

    @Override
    public VersionDto rollback(String id, RollbackDto body) throws Exception {
        return experiences.rollback(id, body == null ? 0 : body.number());
    }

    @Override
    public List<VersionDto> versions(String id) throws Exception {
        return experiences.versions(id);
    }

    @Override
    public PlaceDto place(String id) throws Exception {
        return experiences.place(id);
    }

    @Override
    public ExperienceDto visit(String id) throws Exception {
        return experiences.visit(id);
    }

    @Override
    public ExperienceDto favourite(String id) throws Exception {
        return experiences.favourite(id);
    }

    @Override
    public ExperienceDto vote(String id, VoteDto body) throws Exception {
        return experiences.vote(id, body == null ? 0 : body.vote());
    }
}
