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

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.reel.Caller;
import com.codenameone.examples.reel.api.AnswerDto;
import com.codenameone.examples.reel.api.AnswerResultDto;
import com.codenameone.examples.reel.api.CertificateDto;
import com.codenameone.examples.reel.api.CourseProgressDto;
import com.codenameone.examples.reel.api.LearningApiServer;

import java.util.List;

@Component
public class LearningEndpoint implements LearningApiServer {
    private final Learning learning;

    public LearningEndpoint(Learning learning) {
        this.learning = learning;
    }

    @Override
    public AnswerResultDto answer(AnswerDto answer) throws Exception {
        return learning.answer(Caller.name(), answer);
    }

    @Override
    public List<CourseProgressDto> progress(String profile) throws Exception {
        return learning.progress(Caller.name(), profile);
    }

    @Override
    public List<CertificateDto> certificates() throws Exception {
        return learning.certificates(Caller.name());
    }

    /// Public (see SecurityConfig): what an employer checking a certificate
    /// calls.
    @Override
    public CertificateDto verify(String code) throws Exception {
        return learning.verify(code);
    }
}
