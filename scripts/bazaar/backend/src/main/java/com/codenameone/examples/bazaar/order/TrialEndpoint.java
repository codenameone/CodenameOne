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
package com.codenameone.examples.bazaar.order;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.TrialApiServer;
import com.codenameone.examples.bazaar.api.TrialDto;
import java.util.List;

/// The server's half of `TrialApi`.
@Component
public class TrialEndpoint implements TrialApiServer {
    private final Trials trials;

    public TrialEndpoint(Trials trials) {
        this.trials = trials;
    }

    @Override
    public List<TrialDto> trials(String storeId) throws Exception {
        return trials.list(Caller.name(), storeId);
    }

    @Override
    public List<TrialDto> setTrial(String storeId, String productId, TrialDto trial)
            throws Exception {
        return trials.set(Caller.name(), storeId, productId, trial == null ? 0 : trial.days());
    }
}
