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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.appkit.Net;
import com.codename1.scheduling.DefaultScheduleModel;
import com.codename1.scheduling.ScheduleItem;
import com.codename1.ui.FontImage;
import com.codename1.ui.scheduler.Scheduler;
import com.codename1.ui.scheduler.SchedulerAdapter;
import com.codenameone.examples.bazaar.api.PromoEventDto;
import com.codenameone.examples.bazaar.api.RescheduleDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;
import java.util.TimeZone;

/// The promotions calendar: sales, discounts and campaign sends on a month
/// grid. Dragging an event to another day reschedules it through the API,
/// keeping its time of day and length (`PromoDates.moveToDay`). A move the
/// rules forbid is refused before anything is sent; one the server refuses
/// shows its message and the month is reloaded to put the event back.
final class PromotionsPane extends MerchantPane {
    private long shown = System.currentTimeMillis();
    private Scheduler scheduler;
    private boolean busy;

    PromotionsPane() {
        super("Promotions", "merchant-promotions");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_REFRESH, "Refresh", "promotions-refresh", this::load);
        final TimeZone zone = TimeZone.getDefault();
        long[] window = PromoDates.monthWindow(shown, zone);
        Api.sales().calendar(MerchantState.storeId(), String.valueOf(window[0]),
                String.valueOf(window[1]), Net.to(this::fill, Ui::fail));
    }

    private void fill(List<PromoEventDto> events) {
        DefaultScheduleModel model = new DefaultScheduleModel();
        long now = System.currentTimeMillis();
        for (int iter = 0; iter < events.size(); iter++) {
            PromoEventDto e = events.get(iter);
            long end = e.endsAt() > e.startsAt() ? e.endsAt()
                    : e.startsAt() + PromoDates.CAMPAIGN_LENGTH;
            boolean movable = PromoDates.refusal(e.kind(), e.status(), e.startsAt() + 1,
                    end + 1, now) == null || PromoDates.SALE.equals(e.kind())
                    && "SCHEDULED".equals(e.status());
            model.addItem(new ScheduleItem(e.kind() + ":" + e.id(), e.title(), e.startsAt(), end)
                    .setColor(PromoDates.colour(e.colour(), 0x6366f1))
                    .setMovable(movable)
                    .setResizable(false)
                    .setUserObject(e));
        }
        if (scheduler == null) {
            scheduler = new Scheduler(model);
            scheduler.setName("promotions-calendar");
            scheduler.setView(Scheduler.View.MONTH);
            scheduler.setEditable(true);
            scheduler.setDragEdits(true);
            scheduler.addSchedulerListener(new Moves());
        } else {
            scheduler.setModel(model);
        }
        scheduler.setDate(shown);
        root(scheduler);
    }

    private final class Moves extends SchedulerAdapter {
        @Override
        public boolean itemChanging(Scheduler s, ScheduleItem item, long start, long end,
                String resourceId) {
            if (busy || !(item.getUserObject() instanceof PromoEventDto)) {
                return false;
            }
            PromoEventDto e = (PromoEventDto) item.getUserObject();
            long[] to = PromoDates.moveToDay(e.startsAt(),
                    e.endsAt() > e.startsAt() ? e.endsAt() : e.startsAt(), start,
                    TimeZone.getDefault());
            String why = PromoDates.refusal(e.kind(), e.status(), to[0],
                    PromoDates.CAMPAIGN.equals(e.kind()) ? to[0] + PromoDates.CAMPAIGN_LENGTH : to[1],
                    System.currentTimeMillis());
            if (why != null) {
                Ui.say(why);
                return false;
            }
            busy = true;
            shown = start;
            Api.sales().reschedule(MerchantState.storeId(),
                    new RescheduleDto(e.kind(), e.id(), to[0], to[1]), Net.to(moved -> {
                        busy = false;
                        Ui.say("Rescheduled");
                        load();
                    }, (status, message) -> {
                        busy = false;
                        Ui.fail(status, message);
                        load();
                    }));
            return true;
        }

        @Override
        public void visibleRangeChanged(Scheduler s, long start, long end) {
            long middle = start + (end - start) / 2;
            if (PromoDates.daysBetween(shown, middle, TimeZone.getDefault()) > 20
                    || PromoDates.daysBetween(middle, shown, TimeZone.getDefault()) > 20) {
                shown = middle;
                load();
            }
        }
    }
}
