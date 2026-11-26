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
package com.codenameone.examples.bazaar.ops;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.bazaar.api.AttributionDto;
import com.codenameone.examples.bazaar.api.BinDto;
import com.codenameone.examples.bazaar.api.CannedReplyDto;
import com.codenameone.examples.bazaar.api.ChatConversationDto;
import com.codenameone.examples.bazaar.api.ChatMessageDto;
import com.codenameone.examples.bazaar.api.ChatReplyDto;
import com.codenameone.examples.bazaar.api.CohortRowDto;
import com.codenameone.examples.bazaar.api.CombinedListingDto;
import com.codenameone.examples.bazaar.api.ForecastDto;
import com.codenameone.examples.bazaar.api.ImportDto;
import com.codenameone.examples.bazaar.api.ImportResultDto;
import com.codenameone.examples.bazaar.api.OpsApiServer;
import com.codenameone.examples.bazaar.api.PermissionSetDto;
import com.codenameone.examples.bazaar.api.PickLineDto;
import com.codenameone.examples.bazaar.api.ProfitLossDto;
import com.codenameone.examples.bazaar.api.PurchaseOrderDto;
import com.codenameone.examples.bazaar.api.ReportDto;
import com.codenameone.examples.bazaar.api.ReportSpecDto;
import com.codenameone.examples.bazaar.api.SavedReportDto;
import com.codenameone.examples.bazaar.api.StaffPermissionDto;
import com.codenameone.examples.bazaar.api.SuggestionApplyDto;
import com.codenameone.examples.bazaar.api.SuggestionDto;
import com.codenameone.examples.bazaar.api.TimelineEventDto;
import com.codenameone.examples.bazaar.api.TransferDto;
import com.codenameone.examples.bazaar.api.WarehouseMoveDto;
import com.codenameone.examples.bazaar.api.WarehouseTaskDto;
import java.util.List;

/// The REST face of the operations features: each call names its caller
/// and hands over to the service, which checks access.
@Component
public class OpsEndpoint implements OpsApiServer {
    private final Warehouse warehouse;
    private final Timelines timelines;
    private final Forecasts forecasts;
    private final Reports reports;
    private final Permissions permissions;
    private final ChatInbox chat;
    private final Listings listings;

    public OpsEndpoint(Warehouse warehouse, Timelines timelines, Forecasts forecasts, Reports reports,
            Permissions permissions, ChatInbox chat, Listings listings) {
        this.warehouse = warehouse;
        this.timelines = timelines;
        this.forecasts = forecasts;
        this.reports = reports;
        this.permissions = permissions;
        this.chat = chat;
        this.listings = listings;
    }

    private static String location(SuggestionApplyDto body) {
        return body == null ? "" : body.locationId();
    }

    private static String text(ChatReplyDto body) {
        return body == null ? null : body.body();
    }

    @Override
    public List<WarehouseTaskDto> warehouseBoard(String s, String l) throws Exception {
        return warehouse.board(Caller.name(), s, l);
    }

    @Override
    public List<WarehouseTaskDto> syncWarehouse(String s, String l) throws Exception {
        return warehouse.sync(Caller.name(), s, l);
    }

    @Override
    public WarehouseTaskDto moveWarehouseTask(String s, WarehouseMoveDto b) throws Exception {
        return warehouse.move(Caller.name(), s, b);
    }

    @Override
    public List<BinDto> bins(String s, String l) throws Exception {
        return warehouse.bins(Caller.name(), s, l);
    }

    @Override
    public List<BinDto> saveBin(String s, String l, BinDto b) throws Exception {
        return warehouse.saveBin(Caller.name(), s, l, b);
    }

    @Override
    public List<PickLineDto> pickList(String s, String l) throws Exception {
        return warehouse.pickList(Caller.name(), s, l);
    }

    @Override
    public List<TimelineEventDto> transferTimeline(String s, String id) throws Exception {
        return timelines.transfer(Caller.name(), s, id);
    }

    @Override
    public List<TimelineEventDto> purchaseOrderTimeline(String s, String id) throws Exception {
        return timelines.purchase(Caller.name(), s, id);
    }

    @Override
    public List<ForecastDto> forecast(String s) throws Exception {
        return forecasts.forecast(Caller.name(), s);
    }

    @Override
    public List<SuggestionDto> suggestions(String s) throws Exception {
        return forecasts.suggestions(Caller.name(), s);
    }

    @Override
    public List<PurchaseOrderDto> draftReorders(String s, SuggestionApplyDto b) throws Exception {
        return forecasts.draftReorders(Caller.name(), s, location(b));
    }

    @Override
    public List<TransferDto> draftTransfers(String s, SuggestionApplyDto b) throws Exception {
        return forecasts.draftTransfers(Caller.name(), s, location(b));
    }

    @Override
    public ReportDto runReport(String s, ReportSpecDto b) throws Exception {
        return reports.run(Caller.name(), s, b);
    }

    @Override
    public List<SavedReportDto> savedReports(String s) throws Exception {
        return reports.saved(Caller.name(), s);
    }

    @Override
    public SavedReportDto saveReport(String s, ReportSpecDto b) throws Exception {
        return reports.save(Caller.name(), s, b);
    }

    @Override
    public ReportDto savedReport(String s, String id) throws Exception {
        return reports.runSaved(Caller.name(), s, id);
    }

    @Override
    public List<CohortRowDto> cohorts(String s) throws Exception {
        return reports.cohorts(Caller.name(), s);
    }

    @Override
    public ProfitLossDto profitLoss(String s, String from, String to) throws Exception {
        return reports.profitLoss(Caller.name(), s, from, to);
    }

    @Override
    public List<AttributionDto> attribution(String s, String from, String to) throws Exception {
        return reports.attribution(Caller.name(), s, from, to);
    }

    @Override
    public List<PermissionSetDto> permissionSets(String s) throws Exception {
        return permissions.sets(Caller.name(), s);
    }

    @Override
    public PermissionSetDto createPermissionSet(String s, PermissionSetDto b) throws Exception {
        return permissions.create(Caller.name(), s, b);
    }

    @Override
    public PermissionSetDto updatePermissionSet(String s, String id, PermissionSetDto b)
            throws Exception {
        return permissions.update(Caller.name(), s, id, b);
    }

    @Override
    public List<StaffPermissionDto> staffPermissions(String s) throws Exception {
        return permissions.assignments(Caller.name(), s);
    }

    @Override
    public List<StaffPermissionDto> assignPermissionSet(String s, StaffPermissionDto b)
            throws Exception {
        return permissions.assign(Caller.name(), s, b);
    }

    @Override
    public List<ChatConversationDto> chats(String s) throws Exception {
        return chat.list(Caller.name(), s);
    }

    @Override
    public List<ChatMessageDto> chatMessages(String s, String id) throws Exception {
        return chat.messages(Caller.name(), s, id);
    }

    @Override
    public List<ChatMessageDto> replyChat(String s, String id, ChatReplyDto b) throws Exception {
        return chat.reply(Caller.name(), s, id, text(b));
    }

    @Override
    public ChatConversationDto assignChat(String s, String id, ChatReplyDto b) throws Exception {
        return chat.assign(Caller.name(), s, id, text(b));
    }

    @Override
    public ChatConversationDto closeChat(String s, String id) throws Exception {
        return chat.close(Caller.name(), s, id);
    }

    @Override
    public List<CannedReplyDto> cannedReplies(String s) throws Exception {
        return chat.canned(Caller.name(), s);
    }

    @Override
    public List<CannedReplyDto> saveCannedReply(String s, CannedReplyDto b) throws Exception {
        return chat.saveCanned(Caller.name(), s, b);
    }

    @Override
    public List<CombinedListingDto> combinedListings(String s) throws Exception {
        return listings.list(Caller.name(), s);
    }

    @Override
    public CombinedListingDto saveCombinedListing(String s, CombinedListingDto b) throws Exception {
        return listings.save(Caller.name(), s, b);
    }

    @Override
    public ImportResultDto importMarketplace(String s, ImportDto b) throws Exception {
        return listings.importCsv(Caller.name(), s, b);
    }
}
