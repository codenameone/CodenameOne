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

package com.codenameone.examples.bazaar.pipeline;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.PipelineApiServer;
import com.codenameone.examples.bazaar.api.PipelineColumnDto;
import com.codenameone.examples.bazaar.api.PipelineMoveDto;

import java.util.List;

/// The server's half of `PipelineApi`; [Pipeline] checks the caller.
@Component
public class PipelineEndpoint implements PipelineApiServer {
    private final Pipeline pipeline;

    public PipelineEndpoint(Pipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public List<PipelineColumnDto> board(String storeId, String column, String page)
            throws Exception {
        return pipeline.board(Caller.name(), storeId, column, page(page));
    }

    @Override
    public List<PipelineColumnDto> move(String storeId, PipelineMoveDto move) throws Exception {
        return pipeline.move(Caller.name(), storeId, move);
    }

    static int page(String text) {
        if (text == null || text.trim().length() == 0) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(text.trim()));
        } catch (NumberFormatException bad) {
            throw new ResponseStatusException(400, "A page is a whole number");
        }
    }
}
