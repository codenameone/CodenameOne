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
package com.codenameone.examples.skirmish.forge;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.skirmish.Caller;
import com.codenameone.examples.skirmish.Roles;
import com.codenameone.examples.skirmish.api.AssetDto;
import com.codenameone.examples.skirmish.api.AssetUploadDto;
import com.codenameone.examples.skirmish.api.ForgeApiServer;
import com.codenameone.examples.skirmish.api.ForkDto;
import com.codenameone.examples.skirmish.api.MissionOutcomeDto;
import com.codenameone.examples.skirmish.api.MissionResultDto;
import com.codenameone.examples.skirmish.api.MissionStarsDto;
import com.codenameone.examples.skirmish.api.PackDto;
import com.codenameone.examples.skirmish.api.PackReportDto;
import com.codenameone.examples.skirmish.api.PackSaveDto;
import com.codenameone.examples.skirmish.api.PackSummaryDto;
import com.codenameone.examples.skirmish.api.RateDto;
import java.util.List;

/// The Forge's REST surface; `Forge` does the work.
@Component
public class ForgeEndpoint implements ForgeApiServer {
    private final Forge forge;

    public ForgeEndpoint(Forge forge) {
        this.forge = forge;
    }

    private static boolean admin() {
        return Caller.hasRole(Roles.ADMIN);
    }

    @Override
    public List<PackSummaryDto> packs(String scope) throws Exception {
        return forge.list(Caller.name(), scope);
    }

    @Override
    public PackDto pack(String id) throws Exception {
        return forge.pack(Caller.name(), admin(), id);
    }

    @Override
    public PackDto version(String id, int version) throws Exception {
        return forge.version(Caller.name(), admin(), id, version);
    }

    @Override
    public PackDto draft(String id) throws Exception {
        return forge.draft(Caller.name(), id);
    }

    @Override
    public PackSummaryDto saveDraft(PackSaveDto body) throws Exception {
        return forge.saveDraft(Caller.name(), body == null ? null : body.content());
    }

    @Override
    public PackSummaryDto publish(String id) throws Exception {
        return forge.publish(Caller.name(), id);
    }

    @Override
    public PackSummaryDto delete(String id) throws Exception {
        return forge.delete(Caller.name(), false, id);
    }

    @Override
    public PackSummaryDto fork(String id, ForkDto body) throws Exception {
        return forge.fork(Caller.name(), admin(), id, body == null ? null : body.newId());
    }

    @Override
    public PackSummaryDto rate(RateDto body) throws Exception {
        return forge.rate(Caller.name(), body == null ? null : body.packId(), body == null ? 0 : body.stars());
    }

    @Override
    public PackSummaryDto report(PackReportDto body) throws Exception {
        return forge.report(Caller.name(), body == null ? null : body.packId(), body == null ? null : body.reason());
    }

    @Override
    public AssetDto upload(AssetUploadDto body) throws Exception {
        return forge.upload(Caller.name(), body);
    }

    @Override
    public AssetDto asset(String sha256) throws Exception {
        return forge.download(Caller.name(), admin(), sha256);
    }

    @Override
    public MissionOutcomeDto result(MissionResultDto body) throws Exception {
        return forge.result(Caller.name(), body == null ? null : body.replay());
    }

    @Override
    public List<MissionStarsDto> stars(String id) throws Exception {
        return forge.stars(Caller.name(), id);
    }
}
