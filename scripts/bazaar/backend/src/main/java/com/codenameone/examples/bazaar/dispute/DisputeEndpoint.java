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
package com.codenameone.examples.bazaar.dispute;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.DisputeApiServer;
import com.codenameone.examples.bazaar.api.DisputeDto;
import com.codenameone.examples.bazaar.api.DisputeEvidenceDto;
import java.util.List;

/// The server's half of `DisputeApi`.
@Component
public class DisputeEndpoint implements DisputeApiServer {
    private final Disputes disputes;

    public DisputeEndpoint(Disputes disputes) {
        this.disputes = disputes;
    }

    @Override
    public List<DisputeDto> disputes(String storeId) throws Exception {
        return disputes.list(Caller.name(), storeId);
    }

    @Override
    public DisputeDto dispute(String storeId, String disputeId) throws Exception {
        return disputes.detail(Caller.name(), storeId, disputeId);
    }

    @Override
    public List<DisputeEvidenceDto> disputeEvidence(String storeId, String disputeId) throws Exception {
        return disputes.evidence(Caller.name(), storeId, disputeId);
    }

    @Override
    public List<DisputeEvidenceDto> addDisputeEvidence(String storeId, String disputeId, DisputeEvidenceDto body) throws Exception {
        return disputes.addEvidence(Caller.name(), storeId, disputeId, body);
    }

    @Override
    public DisputeDto submitDispute(String storeId, String disputeId) throws Exception {
        return disputes.submit(Caller.name(), storeId, disputeId);
    }

    @Override
    public DisputeDto acceptDispute(String storeId, String disputeId) throws Exception {
        return disputes.accept(Caller.name(), storeId, disputeId);
    }
}
