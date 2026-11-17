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
package com.codenameone.examples.brickyard.progress;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.annotations.Profile;
import com.codename1.backend.annotations.RequestBody;
import com.codename1.backend.annotations.RestController;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.brickyard.Access;
import com.codenameone.examples.brickyard.api.ProgressDto;
import com.codenameone.examples.brickyard.api.ProgressEventDto;
import java.io.IOException;

/// A development server's way to give a demo player a history of play: an
/// event credited as if an engine server had reported it, so a fresh
/// database shows levels, a filled season track and quests ready to claim.
/// The bean exists only under `CN1_PROFILE=dev`, and only staff may call it.
@RestController
@Profile("dev")
public class DevProgress {
    private final Progression progression;

    public DevProgress(Progression progression) {
        this.progression = progression;
    }

    @PostMapping("/api/dev/progress")
    @Transactional(rollbackFor = IOException.class)
    public ProgressDto credit(@RequestBody ProgressEventDto event) throws IOException {
        if (!Access.isStaff()) {
            throw new ResponseStatusException(403, "Staff only");
        }
        return progression.credit(event);
    }
}
