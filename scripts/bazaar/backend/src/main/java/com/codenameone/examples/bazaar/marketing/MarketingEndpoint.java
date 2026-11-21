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
package com.codenameone.examples.bazaar.marketing;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.AudienceDto;
import com.codenameone.examples.bazaar.api.AudiencePreviewDto;
import com.codenameone.examples.bazaar.api.CampaignDto;
import com.codenameone.examples.bazaar.api.CampaignStatsDto;
import com.codenameone.examples.bazaar.api.FormResultDto;
import com.codenameone.examples.bazaar.api.FormSubmitDto;
import com.codenameone.examples.bazaar.api.MarketingApiServer;
import com.codenameone.examples.bazaar.api.ScheduleDto;
import com.codenameone.examples.bazaar.api.SignupFormDto;

import java.time.Instant;
import java.util.List;

/// The server's half of `MarketingApi`.
@Component
public class MarketingEndpoint implements MarketingApiServer {
    private final Audiences audiences;
    private final Campaigns campaigns;
    private final SignupForms forms;

    public MarketingEndpoint(Audiences audiences, Campaigns campaigns, SignupForms forms) {
        this.audiences = audiences;
        this.campaigns = campaigns;
        this.forms = forms;
    }

    @Override
    public List<AudienceDto> segments(String storeId) throws Exception {
        return audiences.list(Caller.name(), storeId);
    }

    @Override
    public AudienceDto createSegment(String storeId, AudienceDto body) throws Exception {
        return audiences.save(Caller.name(), storeId, null, body);
    }

    @Override
    public AudienceDto saveSegment(String storeId, String segmentId, AudienceDto body)
            throws Exception {
        return audiences.save(Caller.name(), storeId, segmentId, body);
    }

    @Override
    public List<AudienceDto> removeSegment(String storeId, String segmentId) throws Exception {
        return audiences.remove(Caller.name(), storeId, segmentId);
    }

    @Override
    public AudiencePreviewDto preview(String storeId, AudienceDto body) throws Exception {
        return audiences.preview(Caller.name(), storeId, body);
    }

    @Override
    public List<CampaignDto> campaigns(String storeId) throws Exception {
        return campaigns.list(Caller.name(), storeId);
    }

    @Override
    public CampaignDto createCampaign(String storeId, CampaignDto body) throws Exception {
        return campaigns.save(Caller.name(), storeId, null, body);
    }

    @Override
    public CampaignDto saveCampaign(String storeId, String campaignId, CampaignDto body)
            throws Exception {
        return campaigns.save(Caller.name(), storeId, campaignId, body);
    }

    @Override
    public CampaignDto schedule(String storeId, String campaignId, ScheduleDto body)
            throws Exception {
        return campaigns.schedule(Caller.name(), storeId, campaignId, body == null ? 0 : body.at());
    }

    @Override
    public CampaignDto cancelCampaign(String storeId, String campaignId) throws Exception {
        return campaigns.cancel(Caller.name(), storeId, campaignId);
    }

    @Override
    public CampaignStatsDto stats(String storeId, String campaignId) throws Exception {
        return campaigns.stats(Caller.name(), storeId, campaignId);
    }

    @Override
    public List<SignupFormDto> forms(String storeId) throws Exception {
        return forms.list(Caller.name(), storeId);
    }

    @Override
    public SignupFormDto createForm(String storeId, SignupFormDto body) throws Exception {
        return forms.save(Caller.name(), storeId, null, body);
    }

    @Override
    public SignupFormDto saveForm(String storeId, String formId, SignupFormDto body)
            throws Exception {
        return forms.save(Caller.name(), storeId, formId, body);
    }

    @Override
    public SignupFormDto publicForm(String storeId, String formId) throws Exception {
        return forms.publicForm(storeId, formId);
    }

    @Override
    public FormResultDto submit(String storeId, String formId, FormSubmitDto body) throws Exception {
        return forms.submit(storeId, formId, body, Instant.now());
    }
}
