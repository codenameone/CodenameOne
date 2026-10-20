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
package com.codenameone.examples.parley.call;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.parley.Caller;
import com.codenameone.examples.parley.api.CallApiServer;
import com.codenameone.examples.parley.api.CallDto;
import com.codenameone.examples.parley.api.CallSignalDto;
import com.codenameone.examples.parley.api.CallStartDto;
import com.codenameone.examples.parley.api.IceConfigDto;

import java.util.List;

@Component
public class CallEndpoint implements CallApiServer {
    private final Calls calls;

    public CallEndpoint(Calls calls) {
        this.calls = calls;
    }

    @Override
    public CallDto start(CallStartDto call) throws Exception {
        return calls.start(Caller.name(), call);
    }

    @Override
    public CallDto signal(String id, CallSignalDto signal) throws Exception {
        return calls.signal(Caller.name(), id, signal);
    }

    @Override
    public List<CallDto> history() throws Exception {
        return calls.history(Caller.name());
    }

    @Override
    public IceConfigDto iceServers() throws Exception {
        return calls.ice(Caller.name());
    }
}
