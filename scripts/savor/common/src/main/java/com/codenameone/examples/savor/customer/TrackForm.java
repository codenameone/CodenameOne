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
package com.codenameone.examples.savor.customer;

import com.codename1.maps.LatLng;
import com.codename1.maps.Marker;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.savor.api.DeliveryDto;
import com.codenameone.examples.savor.api.OrderDto;
import com.codenameone.examples.savor.api.OrderLineDto;
import com.codenameone.examples.savor.api.OrderState;
import com.codenameone.examples.savor.api.RatingDto;
import com.codenameone.examples.savor.live.LiveChannel;
import com.codenameone.examples.savor.map.CarMarker;
import com.codenameone.examples.savor.map.MapStage;
import com.codenameone.examples.savor.map.Maps;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.pay.ReceiptForm;
import com.codenameone.examples.savor.ui.BottomSheet;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Ui;

/// One order on its way: the kitchen and the door on a map, the courier
/// moving between them once one has it, and four steps under it that fill
/// in as the order goes from placed to delivered.
///
/// The live channel says when something changed and where the courier is;
/// what changed is read again over REST, so a frame lost on a bad
/// connection costs nothing but the poll that follows.
public final class TrackForm extends LiveChannel.Adapter {
    private static final Net.Failed QUIET = (status, message) -> { };
    private static final int POLL_MILLIS = 10000;
    private static final String[] STEPS = {"Placed", "Cooking", "On its way", "Delivered"};

    private final String id;
    private final Form previous;
    private final Form form = Ui.home("track");
    private final MapStage stage;
    private final BottomSheet sheet;
    private OrderDto order;
    private Marker kitchen;
    private Marker door;
    private CarMarker courier;
    private boolean framed;

    private TrackForm(final Form previous, String id) {
        this.id = id;
        this.previous = previous;
        Button back = Ui.round(FontImage.MATERIAL_ARROW_BACK, "back", e -> previous.showBack());
        stage = new MapStage(CustomerForm.destination(), Maps.STREET_ZOOM, back);
        sheet = stage.sheet();
        form.add(stage.layers());
        Ui.back(form, previous);
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            load();
        });
        UITimer.timer(POLL_MILLIS, true, form, () -> {
            if (!LiveChannel.connected() && order != null && Food.active(order.state())) {
                load();
            }
        });
    }

    /// Opens the order `id` over `previous`.
    public static void show(Form previous, String id) {
        new TrackForm(previous, id).form.show();
    }

    private void load() {
        Api.orders().order(id, Net.to(this::fill, (status, message) -> {
            if (order == null) {
                Ui.fail(status, message);
                previous.showBack();
            }
        }));
    }

    private void fill(OrderDto found) {
        order = found;
        markers();
        sheet.reset();
        Container peek = sheet.peek();
        peek.add(Ui.plain(Food.state(found.state()), "SvSheetTitle"));
        if (Food.active(found.state())) {
            peek.add(Ui.plain(eta(found), "SvMuted"));
        }
        peek.add(steps(Food.step(found.state())));

        Container more = sheet.more();
        DeliveryDto delivery = found.delivery();
        if (delivery != null && delivery.courierName() != null && Food.active(found.state())) {
            more.add(Ui.person(delivery.courierName(), delivery.courierUsername(),
                    Ui.plain(delivery.vehicle() == null ? "" : delivery.vehicle(), "SvMutedSmall")));
        }
        if (found.reason() != null && found.reason().length() > 0 && found.state().isCancelled()) {
            more.add(Ui.text(found.reason(), "SvMuted"));
        }
        Ui.section(more, found.restaurantName());
        for (OrderLineDto line : found.lines()) {
            more.add(Ui.fact(line.quantity() + " x " + line.name(),
                    Ui.money(line.lineCents(), found.currency())));
        }
        more.add(Ui.fact(Lang.tr("Total"), Ui.money(found.totalCents(), found.currency()),
                "SvTotal"));
        more.add(Ui.row(FontImage.MATERIAL_PLACE, found.dropoffAddress(), null, null));
        if (found.state() == OrderState.PLACED) {
            more.add(Ui.danger("Cancel order", "cancel", e -> cancel()));
        }
        if (found.state() == OrderState.DELIVERED) {
            if (found.stars() == 0) {
                more.add(stars());
            }
            more.add(Ui.secondary("Receipt", "receipt", e -> ReceiptForm.open(form, id)));
        }
        sheet.ready(!Food.active(found.state()));
        form.revalidate();
    }

    private static String eta(OrderDto order) {
        if (order.etaMinutes() <= 0) {
            return "";
        }
        return Lang.tr("Arriving in about {0} min", String.valueOf(order.etaMinutes()));
    }

    /// The four steps, the ones reached in the brand's colour.
    private static Container steps(int reached) {
        Container row = new Container(new GridLayout(1, STEPS.length));
        row.setUIID("SvSteps");
        row.setName("steps");
        for (int iter = 0; iter < STEPS.length; iter++) {
            Container step = new Container(BoxLayout.y());
            step.setUIID(iter <= reached ? "SvStepDone" : "SvStep");
            Label bar = new Label("", iter <= reached ? "SvStepBarDone" : "SvStepBar");
            step.add(bar);
            step.add(Ui.plain(Lang.tr(STEPS[iter]), iter <= reached ? "SvStepText" : "SvMutedSmall"));
            row.add(step);
        }
        if (reached < 0) {
            row.setHidden(true);
        }
        return row;
    }

    private Container stars() {
        Container row = new Container(new GridLayout(1, 5));
        row.setName("stars");
        for (int iter = 1; iter <= 5; iter++) {
            final int given = iter;
            row.add(Ui.flat(FontImage.MATERIAL_STAR_BORDER, "star-" + iter,
                    e -> Api.orders().rate(id, new RatingDto(given), Net.to(rated -> {
                        Ui.say("Thanks for rating");
                        fill(rated);
                    }, Ui::fail))));
        }
        Container rate = new Container(BoxLayout.y());
        rate.add(Ui.plain(Lang.tr("How was it?"), "SvRowTitle"));
        rate.add(row);
        return rate;
    }

    private void cancel() {
        if (!Dialog.show(Lang.tr("Cancel this order?"),
                Lang.tr("The kitchen has not started on it, so nothing is charged."),
                Lang.tr("Cancel order"), Lang.tr("Keep it"))) {
            return;
        }
        Api.orders().cancel(id, Net.to(this::fill, Ui::fail));
    }

    private void markers() {
        LatLng from = new LatLng(order.restaurantLat(), order.restaurantLng());
        LatLng to = new LatLng(order.dropoffLat(), order.dropoffLng());
        if (kitchen == null) {
            kitchen = Maps.pickup(stage.map, from);
            door = Maps.dropoff(stage.map, to);
        }
        DeliveryDto delivery = order.delivery();
        boolean carried = delivery != null && order.state() == OrderState.PICKED_UP
                || delivery != null && delivery.courierUsername() != null
                && Food.active(order.state());
        if (carried && (delivery.courierLat() != 0 || delivery.courierLng() != 0)) {
            moved(delivery.courierLat(), delivery.courierLng(), delivery.courierHeading());
        } else if (!Food.active(order.state()) && courier != null) {
            courier.remove();
            courier = null;
        }
        if (!framed) {
            framed = true;
            stage.frame(from, to);
        }
    }

    private void moved(double lat, double lng, double heading) {
        LatLng at = new LatLng(lat, lng);
        if (courier == null) {
            courier = new CarMarker(stage.map, form, at, heading);
        } else {
            courier.moveTo(at, heading);
        }
    }

    @Override
    public void orderChanged(String changed, String state) {
        if (id.equals(changed) && CN.getCurrentForm() == form) {
            load();
        }
    }

    @Override
    public void deliveryChanged(String changed, String state) {
        orderChanged(changed, state);
    }

    @Override
    public void courierMoved(String deliveryId, double lat, double lng, double heading) {
        if (id.equals(deliveryId) && order != null && Food.active(order.state())) {
            moved(lat, lng, heading);
        }
    }
}
