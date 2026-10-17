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
package com.codenameone.examples.savor.courier;

import com.codename1.maps.LatLng;
import com.codename1.maps.Marker;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.savor.Telemetry;
import com.codenameone.examples.savor.api.CourierStatusDto;
import com.codenameone.examples.savor.api.EarningsDto;
import com.codenameone.examples.savor.api.DeliveryDto;
import com.codenameone.examples.savor.api.DeliveryState;
import com.codenameone.examples.savor.live.LiveChannel;
import com.codenameone.examples.savor.map.CarMarker;
import com.codenameone.examples.savor.map.Locator;
import com.codenameone.examples.savor.map.MapStage;
import com.codenameone.examples.savor.map.Maps;
import com.codenameone.examples.savor.map.Routes;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.ui.BottomSheet;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Menu;
import com.codenameone.examples.savor.ui.Nav;
import com.codenameone.examples.savor.ui.Pulse;
import com.codenameone.examples.savor.ui.DeliveryListForm;
import com.codenameone.examples.savor.ui.Ring;
import com.codenameone.examples.savor.ui.StatRow;
import com.codenameone.examples.savor.ui.Ui;
import java.io.IOException;

/// Onboarding: go online, answer the offer, drive the delivery.
///
/// While online the app reports the car's position every few seconds. That
/// report is three things at once to the server: where to draw the car on the
/// customer's map, how far this courier is from the next request, and the sign the
/// courier is still there -- a courier who stops reporting stops being offered
/// deliveries.
///
/// An offer is the server's to make and to withdraw. It arrives as a delivery in
/// the `OFFERED` state with a number of seconds to answer in; when they run out
/// the server offers the delivery to the next courier, and this screen finds out
/// the way it finds out everything, by asking what the courier's delivery now is.
///
/// Each step of a delivery has one thing to do next, and the sheet shows that one
/// thing as its only large button.
public class CourierForm extends LiveChannel.Adapter implements Nav.Home {
    private static final int TICK_MILLIS = 3000;
    /// How fast the made-up car drives where there is no real position:
    /// brisk, so a demonstration does not take the time a real delivery would.
    private static final double SIMULATED_SPEED = 25;
    /// Meters a second in town, for a time of arrival worked out from a
    /// distance: 30 km/h.
    private static final double CITY_SPEED = 8.3;
    private static final Net.Failed QUIET = (status, message) -> { };

    private final Form form = Ui.home("Courier");
    private final Menu menu = new Menu(form, Nav.COURIER);
    private final MapStage stage;
    private final BottomSheet sheet;
    private final CarMarker car;

    private LatLng here = Locator.here();
    private double heading;
    private boolean online;
    private DeliveryDto delivery;
    private EarningsDto earnings;
    /// What the last finished trip paid, to say so until the next one starts.
    private String lastFare;
    private String showing = "";
    /// Where the car is being driven to, as `deliveryId:state`, and the way there.
    private String target = "";
    private Routes.Way way;
    private double travelled;
    private Marker pickupMarker;
    private Marker dropoffMarker;
    private Maps.Line line;
    private Ring countdown;
    private int ticks;

    public CourierForm() {
        menu.item(FontImage.MATERIAL_DIRECTIONS_CAR, "Drive", "home", () -> { });
        menu.item(FontImage.MATERIAL_HISTORY, "Your trips", "history",
                () -> DeliveryListForm.show(form, "Your trips", cb -> Api.courier().history(cb), false));
        menu.item(FontImage.MATERIAL_PAYMENTS, "Earnings", "earnings",
                () -> EarningsForm.show(form));
        stage = new MapStage(here, Maps.STREET_ZOOM, menu.button());
        sheet = stage.sheet();
        car = new CarMarker(stage.map, form, here, heading);
        stage.addRound(FontImage.MATERIAL_MY_LOCATION, "recentre", this::recentre);
        form.add(stage.layers());
        menu.install();
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            refresh();
        });
        UITimer.timer(TICK_MILLIS, true, form, this::tick);
        UITimer.timer(1000, true, form, this::second);
    }

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        display();
        // Picks up where the courier left off: still online, perhaps mid-delivery.
        Api.courier().me(Net.to(me -> {
            // Too late: the user has left for another mode or signed out, and
            // Shift was ended then. Starting it again here would report a
            // courier with no courier's screen.
            if (!Nav.isHome(form)) {
                return;
            }
            online = me.online();
            shift();
            showing = "";
            display();
            refresh();
        }, QUIET));
        loadEarnings();
    }

    public void show() {
        start();
        form.show();
    }

    // ------------------------------------------------------------ the server

    private void tick() {
        ticks++;
        if (!online) {
            return;
        }
        move();
        report(true, QUIET);
        if (!LiveChannel.connected() || ticks % 3 == 0) {
            refresh();
        }
    }

    /// Tells the server where the car is, and whether it is taking deliveries.
    private void report(final boolean wanted, Net.Failed failed) {
        final CourierStatusDto status = new CourierStatusDto(wanted, here.getLatitude(),
                here.getLongitude(), heading);
        Api.courier().status(status, Net.to(me -> {
            if (online != me.online()) {
                online = me.online();
                shift();
                Telemetry.courierOnline(online);
                showing = "";
                display();
            }
        }, failed));
    }

    private void refresh() {
        Api.courier().active(Net.to(this::update, QUIET));
    }

    /// Keeps the phone reporting in the background exactly while the server
    /// has this courier on line.
    private void shift() {
        if (online) {
            Shift.begin();
        } else {
            Shift.end();
        }
    }

    @Override
    public void deliveryChanged(String id, String state) {
        refresh();
    }

    private void update(DeliveryDto latest) {
        boolean none = latest == null || DeliveryState.NONE == latest.state();
        if (none && delivery != null && DeliveryState.OFFERED == delivery.state()) {
            Ui.say("The request went to another courier");
        } else if (none && delivery != null) {
            Ui.say("The customer cancelled");
        }
        delivery = none ? null : latest;
        if (delivery != null) {
            lastFare = null;
        }
        aim();
        display();
    }

    private void act(Net.Call<DeliveryDto> call) {
        act(call, () -> { });
    }

    /// The same, with something to do first when the server agrees.
    private void act(Net.Call<DeliveryDto> call, final Runnable agreed) {
        Net.send(call, changed -> {
            agreed.run();
            update(changed);
        }, (status, message) -> {
            Ui.fail(status, message);
            refresh();
        });
    }

    private void loadEarnings() {
        Api.courier().earnings(Net.to(earned -> {
            earnings = earned;
            showing = "";
            display();
        }, QUIET));
    }

    // --------------------------------------------------------------- the map

    /// Points the car at wherever the delivery needs it next: the pickup once the
    /// delivery is accepted, the destination once the customer is aboard.
    private void aim() {
        String wanted = delivery == null ? "" : delivery.id() + ":" + delivery.state();
        if (wanted.equals(target)) {
            return;
        }
        target = wanted;
        way = null;
        travelled = 0;
        if (pickupMarker != null) {
            stage.map.removeMarker(pickupMarker);
            pickupMarker = null;
        }
        if (dropoffMarker != null) {
            stage.map.removeMarker(dropoffMarker);
            dropoffMarker = null;
        }
        if (line != null) {
            line.remove(stage.map);
            line = null;
        }
        if (delivery == null) {
            stage.center(here, Maps.STREET_ZOOM);
            return;
        }
        final LatLng pickup = new LatLng(delivery.pickupLat(), delivery.pickupLng());
        final LatLng dropoff = new LatLng(delivery.dropoffLat(), delivery.dropoffLng());
        pickupMarker = Maps.pickup(stage.map, pickup);
        dropoffMarker = Maps.dropoff(stage.map, dropoff);
        stage.frame(here, pickup, dropoff);
        if (DeliveryState.ARRIVED == delivery.state()) {
            return;
        }
        final LatLng to = DeliveryState.IN_PROGRESS == delivery.state() ? dropoff : pickup;
        final String aimed = target;
        Routes.find(here, to, route -> {
            if (!aimed.equals(target) || delivery == null) {
                return;
            }
            line = Maps.route(stage.map, route.points);
            // An offer shows the way; only an accepted delivery is driven.
            if (DeliveryState.OFFERED != delivery.state()) {
                way = route;
            }
        });
    }

    /// Moves the car. On a phone the phone has moved it already; where there
    /// is no real position, this drives it along the route.
    private void move() {
        LatLng now;
        if (!Locator.simulated()) {
            now = Locator.here();
        } else if (way != null) {
            travelled += SIMULATED_SPEED * TICK_MILLIS / 1000d;
            now = Routes.along(way.points, travelled);
        } else {
            return;
        }
        if (Maps.meters(here, now) > 2) {
            heading = Maps.heading(here, now);
        }
        here = now;
        car.moveTo(here, heading);
        chip();
    }

    private void recentre() {
        if (delivery == null) {
            stage.center(here, Maps.STREET_ZOOM);
        } else {
            stage.frame(here, new LatLng(delivery.pickupLat(), delivery.pickupLng()),
                    new LatLng(delivery.dropoffLat(), delivery.dropoffLng()));
        }
    }

    /// How far, and about how long, to where the delivery needs the car next.
    private void chip() {
        if (delivery == null || DeliveryState.OFFERED == delivery.state()
                || DeliveryState.ARRIVED == delivery.state()) {
            stage.chip(null, (char) 0);
            return;
        }
        double meters = Maps.meters(here, next());
        stage.chip(Ui.minutes(meters / CITY_SPEED) + Ui.DOT + Ui.distance(meters),
                FontImage.MATERIAL_NAVIGATION);
    }

    private LatLng next() {
        return DeliveryState.IN_PROGRESS == delivery.state()
                ? new LatLng(delivery.dropoffLat(), delivery.dropoffLng())
                : new LatLng(delivery.pickupLat(), delivery.pickupLng());
    }

    // ------------------------------------------------------------- the sheet

    private void second() {
        // The offer has run out: the server has moved on, so ask what is left.
        if (countdown != null && countdown.secondsLeft() == 0) {
            countdown = null;
            refresh();
        }
    }

    private void display() {
        String now = (online ? "on" : "off") + ":" + (delivery == null ? "" : delivery.id() + delivery.state());
        if (now.equals(showing)) {
            return;
        }
        showing = now;
        sheet.reset();
        countdown = null;
        header();
        chip();
        if (delivery == null) {
            waiting();
        } else if (DeliveryState.OFFERED == delivery.state()) {
            offer();
        } else {
            trip();
        }
        sheet.ready(false);
    }

    /// The card at the top: online or off, and what today has made.
    private void header() {
        Container card = new Container(new BorderLayout());
        card.setUIID("WlStatusCard");
        card.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(
                Ui.badge(online ? "Online" : "Offline", online ? "WlBadgeGood" : "WlBadge")));
        if (earnings != null) {
            Container today = new Container(BoxLayout.y());
            today.add(Ui.label(Ui.money(earnings.todayCents(), earnings.currency()),
                    "WlStatusValue"));
            today.add(Ui.label(Lang.tr("Today, {0} trips", String.valueOf(earnings.todayDeliveries())),
                    "WlStatusDetail"));
            card.add(BorderLayout.EAST, today);
        }
        stage.header(card);
    }

    private void waiting() {
        Container peek = sheet.peek();
        Label title = Ui.label(online ? "Looking for deliveries" : "You are offline", "WlHeading");
        title.setName("courierState");
        peek.add(title);
        if (lastFare != null) {
            peek.add(Ui.text(Lang.tr("Trip complete: {0}", lastFare), "WlNotice"));
        }
        if (online) {
            peek.add(Ui.label("Stay close to busy streets; requests come to the nearest car.",
                    "WlMuted"));
            peek.add(FlowLayout.encloseCenter(new Pulse()));
        } else {
            peek.add(Ui.label("Go online to start taking deliveries.", "WlMuted"));
            Button go = new Button("GO", "WlGo");
            go.setName("goOnline");
            go.addActionListener(e -> report(true, Ui::fail));
            peek.add(FlowLayout.encloseCenter(go));
        }
        if (earnings != null) {
            StatRow numbers = Ui.stats(3);
            numbers.stat(Ui.money(earnings.todayCents(), earnings.currency()), "Today",
                    "earnedToday");
            numbers.stat(Ui.money(earnings.weekCents(), earnings.currency()), "This week",
                    "earnedWeek");
            numbers.stat(String.valueOf(earnings.deliveries()), "Trips", "trips");
            peek.add(numbers);
        }
        if (online) {
            peek.add(Ui.secondary("Go offline", "goOffline", e -> report(false, Ui::fail)));
        }
    }

    private void offer() {
        final String id = delivery.id();
        Container peek = sheet.peek();
        Label title = Ui.label("Delivery request", "WlHeading");
        title.setName("courierState");
        countdown = new Ring();
        countdown.setName("countdown");
        countdown.start(delivery.offerExpiresInSeconds(), delivery.offerExpiresInSeconds());
        Label fare = Ui.label(Ui.money(delivery.fareCents(), delivery.currency()), "WlFare");
        fare.setName("fare");
        Container words = new Container(BoxLayout.y());
        words.add(title);
        words.add(fare);
        Container top = new Container(new BorderLayout());
        top.add(BorderLayout.CENTER, words);
        top.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(countdown));
        peek.add(top);
        double away = Maps.meters(here, new LatLng(delivery.pickupLat(), delivery.pickupLng()));
        peek.add(Ui.row(FontImage.MATERIAL_TRIP_ORIGIN, delivery.pickupAddress(),
                Lang.tr("{0} away, {1}", Ui.distance(away), Ui.minutes(away / CITY_SPEED)), null));
        peek.add(Ui.row(FontImage.MATERIAL_PLACE, delivery.dropoffAddress(),
                Ui.distance(delivery.distanceMeters()) + Ui.DOT + Ui.minutes(delivery.durationSeconds()),
                null));
        peek.add(Ui.primary("Accept", "accept", e -> act(cb -> Api.courier().accept(id, cb),
                () -> Telemetry.offerAnswered(true))));
        peek.add(Ui.link("Decline", "decline", e -> act(cb -> Api.courier().decline(id, cb),
                () -> Telemetry.offerAnswered(false))));
        sheet.more().add(Ui.row(FontImage.MATERIAL_PERSON, delivery.customerName(), asked(), null));
        sheet.more().add(Ui.row(delivery.cash() ? FontImage.MATERIAL_PAYMENTS
                : FontImage.MATERIAL_CREDIT_CARD, delivery.cash() ? "Cash" : "Card",
                Lang.tr(Ui.product(delivery.product())), null));
    }

    private void trip() {
        final String id = delivery.id();
        DeliveryState state = delivery.state();
        Container peek = sheet.peek();
        Label title = Ui.label(DeliveryState.ACCEPTED == state
                ? Lang.tr("Pick up {0}", delivery.customerName())
                : DeliveryState.ARRIVED == state ? Lang.tr("Waiting for {0}", delivery.customerName())
                : Lang.tr("On the trip"), "WlHeading");
        title.setName("courierState");
        peek.add(title);
        boolean aboard = DeliveryState.IN_PROGRESS == state;
        peek.add(Ui.row(aboard ? FontImage.MATERIAL_PLACE : FontImage.MATERIAL_TRIP_ORIGIN,
                aboard ? delivery.dropoffAddress() : delivery.pickupAddress(),
                aboard ? "Drop-off" : "Pickup", null));
        if (delivery.cash()) {
            // Said before the trip ends, so that it is not a surprise at the door.
            Label cash = Ui.label(Lang.tr("Collect {0} in cash", Ui.money(
                    delivery.total() > 0 ? delivery.total() : delivery.fareCents(), delivery.currency())),
                    "WlNotice");
            cash.setName("collectCash");
            Ui.icon(cash, FontImage.MATERIAL_PAYMENTS, 3f);
            peek.add(cash);
        }
        if (!aboard) {
            Container reach = new Container(new GridLayout(1, 2));
            reach.setUIID("WlActions");
            reach.add(Ui.action(FontImage.MATERIAL_CALL, "Call", "call", e -> call()));
            reach.add(Ui.action(FontImage.MATERIAL_SMS, "Message", "message", e -> message()));
            peek.add(reach);
        }
        if (DeliveryState.ACCEPTED == state) {
            peek.add(Ui.primary("I have arrived", "arrived",
                    e -> act(cb -> Api.courier().arrived(id, cb))));
        } else if (DeliveryState.ARRIVED == state) {
            peek.add(Ui.primary("Start trip", "start",
                    e -> act(cb -> Api.courier().start(id, cb))));
        } else {
            peek.add(Ui.primary("Complete trip", "complete",
                    e -> Api.courier().complete(id, Net.to(finished -> {
                        Telemetry.deliveryCompleted("courier", finished.fareCents(), finished.cash());
                        lastFare = Ui.money(finished.fareCents(), finished.currency());
                        Ui.say(finished.cash() ? Lang.tr("Collect {0} in cash", Ui.money(
                                finished.total() > 0 ? finished.total() : finished.fareCents(),
                                finished.currency())) : Lang.tr("Trip complete: {0}", lastFare));
                        delivery = null;
                        aim();
                        display();
                        loadEarnings();
                    }, Ui::fail))));
        }
        Container more = sheet.more();
        more.add(Ui.row(FontImage.MATERIAL_PERSON, delivery.customerName(), asked(), null));
        more.add(Ui.row(FontImage.MATERIAL_PLACE, delivery.dropoffAddress(),
                Ui.distance(delivery.distanceMeters()) + Ui.DOT + Ui.minutes(delivery.durationSeconds()),
                Ui.label(Ui.money(delivery.fareCents(), delivery.currency()), "WlRowValue")));
        // A customer who is aboard is taken where they are going: there is no
        // cancelling from here, only finishing.
        if (!aboard) {
            more.add(Ui.link("Cancel delivery", "cancel",
                    e -> Api.courier().cancel(id, Net.to(cancelled -> {
                        Telemetry.deliveryCancelled("courier", state);
                        delivery = null;
                        aim();
                        display();
                    }, Ui::fail))));
        }
    }

    private void call() {
        String number = delivery == null ? null : delivery.customerPhone();
        if (number == null || number.length() == 0 || !CN.canDial()) {
            Ui.say("Calling is not available on this device");
            return;
        }
        CN.dial(number);
    }

    private void message() {
        String number = delivery == null ? null : delivery.customerPhone();
        if (number == null || number.length() == 0) {
            Ui.say("Your customer cannot be messaged right now");
            return;
        }
        try {
            CN.sendSMS(number, Lang.tr("Hi, this is your Savor courier."));
        } catch (IOException failed) {
            Ui.say("Messages are not available on this device");
        }
    }

    /// What the customer asked for with the delivery, in a line.
    private String asked() {
        StringBuilder asked = new StringBuilder();
        if (delivery.quietDelivery()) {
            asked.append(Lang.tr("Quiet delivery"));
        }
        if (delivery.accessibleVehicle()) {
            asked.append(asked.length() > 0 ? ", " : "").append(Lang.tr("Accessible vehicle"));
        }
        if (delivery.petFriendly()) {
            asked.append(asked.length() > 0 ? ", " : "").append(Lang.tr("Pet friendly"));
        }
        if (delivery.note() != null && delivery.note().length() > 0) {
            asked.append(asked.length() > 0 ? ", " : "").append(delivery.note());
        }
        return asked.toString();
    }
}
