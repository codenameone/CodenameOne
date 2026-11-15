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
package com.codenameone.examples.brickyard.moderation;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.brickyard.Staff;
import com.codenameone.examples.brickyard.api.ActionDto;
import com.codenameone.examples.brickyard.api.AdjustDto;
import com.codenameone.examples.brickyard.api.AppealDto;
import com.codenameone.examples.brickyard.api.AuditDto;
import com.codenameone.examples.brickyard.api.CatalogItemDto;
import com.codenameone.examples.brickyard.api.FeaturedDto;
import com.codenameone.examples.brickyard.api.FraudFlagDto;
import com.codenameone.examples.brickyard.api.JobDto;
import com.codenameone.examples.brickyard.api.KpiDto;
import com.codenameone.examples.brickyard.api.ModerationApiServer;
import com.codenameone.examples.brickyard.api.PayoutDto;
import com.codenameone.examples.brickyard.api.QuestDto;
import com.codenameone.examples.brickyard.api.ReportDto;
import com.codenameone.examples.brickyard.api.RoleDto;
import com.codenameone.examples.brickyard.api.SanctionDto;
import com.codenameone.examples.brickyard.api.SeasonDto;
import com.codenameone.examples.brickyard.api.TractionDayDto;
import com.codenameone.examples.brickyard.api.WalletDto;
import com.codenameone.examples.brickyard.economy.Economy;
import com.codenameone.examples.brickyard.progress.Progression;
import com.codenameone.examples.brickyard.progress.Seasons;
import java.util.List;

/// The report, moderation and admin console routes. The permission each
/// needs is checked here or in [Moderation], never left to the route matcher.
@Component
public class ModerationEndpoint implements ModerationApiServer {
    private final Moderation moderation;
    private final Economy economy;
    private final Progression progression;
    private final Seasons seasons;

    public ModerationEndpoint(Moderation moderation, Economy economy, Progression progression, Seasons seasons) {
        this.moderation = moderation;
        this.economy = economy;
        this.progression = progression;
        this.seasons = seasons;
    }

    @Override
    public ReportDto report(ReportDto report) throws Exception {
        return moderation.report(Caller.name(), report);
    }

    @Override
    public List<ReportDto> queue(String status) throws Exception {
        return moderation.queue(status);
    }

    @Override
    public ReportDto resolveReport(String reportId, ActionDto action) throws Exception {
        return moderation.resolve(reportId, action);
    }

    @Override
    public SanctionDto sanction(SanctionDto sanction) throws Exception {
        return moderation.sanction(sanction);
    }

    @Override
    public List<SanctionDto> sanctionsOf(String username) throws Exception {
        return moderation.sanctionsOf(username, true);
    }

    @Override
    public AuditDto unpublish(String experienceId, ActionDto action) throws Exception {
        return moderation.unpublish(experienceId, action == null ? null : action.reason());
    }

    @Override
    public List<SanctionDto> mySanctions() throws Exception {
        return moderation.sanctionsOf(Caller.name(), false);
    }

    @Override
    public AppealDto appeal(AppealDto appeal) throws Exception {
        return moderation.appeal(Caller.name(), appeal);
    }

    @Override
    public List<AppealDto> appeals() throws Exception {
        return moderation.openAppeals();
    }

    @Override
    public AppealDto decideAppeal(String appealId, ActionDto action) throws Exception {
        return moderation.decide(appealId, action);
    }

    @Override
    public List<AuditDto> audit() throws Exception {
        return moderation.audit();
    }

    @Override
    public AuditDto setRole(RoleDto role) throws Exception {
        return moderation.setRole(role);
    }

    @Override
    public FeaturedDto feature(FeaturedDto featured) throws Exception {
        return moderation.feature(featured);
    }

    @Override
    public FeaturedDto unfeature(String featuredId) throws Exception {
        return moderation.unfeature(featuredId);
    }

    @Override
    public List<FeaturedDto> featured() throws Exception {
        return moderation.home();
    }

    @Override
    public KpiDto kpis() throws Exception {
        return moderation.kpis();
    }

    @Override
    public List<TractionDayDto> traction(int days) throws Exception {
        return moderation.traction(days);
    }

    @Override
    public WalletDto adjust(AdjustDto adjust) throws Exception {
        Staff.require(Staff.ADMINISTER);
        if (adjust == null) {
            throw new ResponseStatusException(400, "Nothing to do");
        }
        return economy.adjust(adjust.username(), adjust.amount(), adjust.reason());
    }

    @Override
    public WalletDto refund(ActionDto action) throws Exception {
        Staff.require(Staff.REFUND);
        if (action == null) {
            throw new ResponseStatusException(400, "Name the purchase");
        }
        return economy.refund(action.target(), action.reason());
    }

    @Override
    public List<FraudFlagDto> fraudFlags() throws Exception {
        return moderation.fraudFlags();
    }

    @Override
    public List<PayoutDto> payouts() throws Exception {
        Staff.require(Staff.ADMINISTER);
        return economy.pendingPayouts();
    }

    @Override
    public PayoutDto decidePayout(String payoutId, ActionDto action) throws Exception {
        Staff.require(Staff.ADMINISTER);
        return economy.decidePayout(payoutId, action == null ? null : action.kind(),
                action == null ? "" : action.reason());
    }

    @Override
    public QuestDto defineQuest(QuestDto quest) throws Exception {
        Staff.require(Staff.ADMINISTER);
        return progression.define(quest);
    }

    @Override
    public CatalogItemDto addCatalogItem(CatalogItemDto item) throws Exception {
        Staff.require(Staff.ADMINISTER);
        return economy.addCatalogItem(item);
    }

    /// The scheduled jobs, run on demand by an admin. The backend also runs
    /// them itself, once a day (see ScheduledJobs); both are safe to repeat.
    @Override
    public JobDto runJob(String job) throws Exception {
        Staff.require(Staff.ADMINISTER);
        if ("engagement-payouts".equals(job)) {
            return economy.payEngagement();
        }
        if ("plus-stipends".equals(job)) {
            return economy.payStipends();
        }
        throw new ResponseStatusException(404, "No such job");
    }

    @Override
    public List<SanctionDto> activeSanctions(int page) throws Exception {
        return moderation.activeSanctions(page);
    }

    @Override
    public FeaturedDto updateFeatured(String featuredId, FeaturedDto featured) throws Exception {
        return moderation.updateFeatured(featuredId, featured);
    }

    @Override
    public SeasonDto startSeason(SeasonDto season) throws Exception {
        return seasons.start(season);
    }
}
