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
package com.codenameone.examples.parley.bridge;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.parley.Caller;
import com.codenameone.examples.parley.api.BridgeApiServer;
import com.codenameone.examples.parley.api.BridgeLinkDto;
import com.codenameone.examples.parley.api.BridgeRequestDto;
import com.codenameone.examples.parley.api.BridgeStatusDto;

import java.util.List;

/// The members' side of bridges: what can be linked, and linking it.
@Component
public class BridgeEndpoint implements BridgeApiServer {
    private final Bridges bridges;

    public BridgeEndpoint(Bridges bridges) {
        this.bridges = bridges;
    }

    @Override
    public BridgeStatusDto status() throws Exception {
        return bridges.status();
    }

    @Override
    public List<BridgeLinkDto> list() throws Exception {
        return bridges.list(Caller.name());
    }

    @Override
    public BridgeLinkDto link(BridgeRequestDto request) throws Exception {
        try {
            return bridges.link(Caller.name(), request);
        } catch (ResponseStatusException refused) {
            throw refused;
        } catch (RuntimeException raced) {
            // Another admin linked the same conversation or chat in the same
            // moment, and the unique index turned this commit away.
            throw new ResponseStatusException(409, "That chat or conversation was just linked");
        }
    }

    @Override
    public BridgeLinkDto unlink(String id) throws Exception {
        return bridges.unlink(Caller.name(), id);
    }
}
