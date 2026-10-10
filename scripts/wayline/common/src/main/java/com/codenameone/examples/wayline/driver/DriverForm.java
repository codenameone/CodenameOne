/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.examples.wayline.driver;

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
import com.codenameone.examples.wayline.Telemetry;
import com.codenameone.examples.wayline.api.DriverStatusDto;
import com.codenameone.examples.wayline.api.EarningsDto;
import com.codenameone.examples.wayline.api.RideDto;
import com.codenameone.examples.wayline.api.RideStates;
import com.codenameone.examples.wayline.live.LiveChannel;
import com.codenameone.examples.wayline.map.CarMarker;
import com.codenameone.examples.wayline.map.Locator;
import com.codenameone.examples.wayline.map.MapStage;
import com.codenameone.examples.wayline.map.Maps;
import com.codenameone.examples.wayline.map.Routes;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.ui.BottomSheet;
import com.codenameone.examples.wayline.ui.Lang;
import com.codenameone.examples.wayline.ui.Menu;
import com.codenameone.examples.wayline.ui.Nav;
import com.codenameone.examples.wayline.ui.Pulse;
import com.codenameone.examples.wayline.ui.RideListForm;
import com.codenameone.examples.wayline.ui.Ring;
import com.codenameone.examples.wayline.ui.StatRow;
import com.codenameone.examples.wayline.ui.Ui;
import java.io.IOException;

/// Driving: go online, answer the offer, drive the ride.
///
/// While online the app reports the car's position every few seconds. That
/// report is three things at once to the server: where to draw the car on the
/// rider's map, how far this driver is from the next request, and the sign the
/// driver is still there -- a driver who stops reporting stops being offered
/// rides.
///
/// An offer is the server's to make and to withdraw. It arrives as a ride in
/// the `OFFERED` state with a number of seconds to answer in; when they run out
/// the server offers the ride to the next driver, and this screen finds out
/// the way it finds out everything, by asking what the driver's ride now is.
///
/// Each step of a ride has one thing to do next, and the sheet shows that one
/// thing as its only large button.
public class DriverForm extends LiveChannel.Adapter implements Nav.Home {
    private static final int TICK_MILLIS = 3000;
    /// How fast the made-up car drives where there is no real position:
    /// brisk, so a demonstration does not take the time a real ride would.
    private static final double SIMULATED_SPEED = 25;
    /// Meters a second in town, for a time of arrival worked out from a
    /// distance: 30 km/h.
    private static final double CITY_SPEED = 8.3;
    private static final Net.Failed QUIET = (status, message) -> { };

    private final Form form = Ui.home("Driver");
    private final Menu menu = new Menu(form, Nav.DRIVER);
    private final MapStage stage;
    private final BottomSheet sheet;
    private final CarMarker car;

    private LatLng here = Locator.here();
    private double heading;
    private boolean online;
    private RideDto ride;
    private EarningsDto earnings;
    /// What the last finished trip paid, to say so until the next one starts.
    private String lastFare;
    private String showing = "";
    /// Where the car is being driven to, as `rideId:state`, and the way there.
    private String target = "";
    private Routes.Way way;
    private double travelled;
    private Marker pickupMarker;
    private Marker dropoffMarker;
    private Maps.Line line;
    private Ring countdown;
    private int ticks;

    public DriverForm() {
        menu.item(FontImage.MATERIAL_DIRECTIONS_CAR, "Drive", "home", () -> { });
        menu.item(FontImage.MATERIAL_HISTORY, "Your trips", "history",
                () -> RideListForm.show(form, "Your trips", cb -> Api.driver().history(cb), false));
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
        // Picks up where the driver left off: still online, perhaps mid-ride.
        Api.driver().me(Net.to(me -> {
            online = me.online;
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

    /// Tells the server where the car is, and whether it is taking rides.
    private void report(final boolean wanted, Net.Failed failed) {
        final DriverStatusDto status = new DriverStatusDto();
        status.online = wanted;
        status.lat = here.getLatitude();
        status.lng = here.getLongitude();
        status.heading = heading;
        Api.driver().status(status, Net.to(me -> {
            if (online != me.online) {
                online = me.online;
                Telemetry.driverOnline(online);
                showing = "";
                display();
            }
        }, failed));
    }

    private void refresh() {
        Api.driver().active(Net.to(this::update, QUIET));
    }

    @Override
    public void rideChanged(String id, String state) {
        refresh();
    }

    private void update(RideDto latest) {
        boolean none = latest == null || RideStates.NONE.equals(latest.state);
        if (none && ride != null && RideStates.OFFERED.equals(ride.state)) {
            Ui.say("The request went to another driver");
        } else if (none && ride != null) {
            Ui.say("The rider cancelled");
        }
        ride = none ? null : latest;
        if (ride != null) {
            lastFare = null;
        }
        aim();
        display();
    }

    private void act(Net.Call<RideDto> call) {
        act(call, () -> { });
    }

    /// The same, with something to do first when the server agrees.
    private void act(Net.Call<RideDto> call, final Runnable agreed) {
        Net.send(call, changed -> {
            agreed.run();
            update(changed);
        }, (status, message) -> {
            Ui.fail(status, message);
            refresh();
        });
    }

    private void loadEarnings() {
        Api.driver().earnings(Net.to(earned -> {
            earnings = earned;
            showing = "";
            display();
        }, QUIET));
    }

    // --------------------------------------------------------------- the map

    /// Points the car at wherever the ride needs it next: the pickup once the
    /// ride is accepted, the destination once the rider is aboard.
    private void aim() {
        String wanted = ride == null ? "" : ride.id + ":" + ride.state;
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
        if (ride == null) {
            stage.center(here, Maps.STREET_ZOOM);
            return;
        }
        final LatLng pickup = new LatLng(ride.pickupLat, ride.pickupLng);
        final LatLng dropoff = new LatLng(ride.dropoffLat, ride.dropoffLng);
        pickupMarker = Maps.pickup(stage.map, pickup);
        dropoffMarker = Maps.dropoff(stage.map, dropoff);
        stage.frame(here, pickup, dropoff);
        if (RideStates.ARRIVED.equals(ride.state)) {
            return;
        }
        final LatLng to = RideStates.IN_PROGRESS.equals(ride.state) ? dropoff : pickup;
        final String aimed = target;
        Routes.find(here, to, route -> {
            if (!aimed.equals(target) || ride == null) {
                return;
            }
            line = Maps.route(stage.map, route.points);
            // An offer shows the way; only an accepted ride is driven.
            if (!RideStates.OFFERED.equals(ride.state)) {
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
        if (ride == null) {
            stage.center(here, Maps.STREET_ZOOM);
        } else {
            stage.frame(here, new LatLng(ride.pickupLat, ride.pickupLng),
                    new LatLng(ride.dropoffLat, ride.dropoffLng));
        }
    }

    /// How far, and about how long, to where the ride needs the car next.
    private void chip() {
        if (ride == null || RideStates.OFFERED.equals(ride.state)
                || RideStates.ARRIVED.equals(ride.state)) {
            stage.chip(null, (char) 0);
            return;
        }
        double meters = Maps.meters(here, next());
        stage.chip(Ui.minutes(meters / CITY_SPEED) + Ui.DOT + Ui.distance(meters),
                FontImage.MATERIAL_NAVIGATION);
    }

    private LatLng next() {
        return RideStates.IN_PROGRESS.equals(ride.state)
                ? new LatLng(ride.dropoffLat, ride.dropoffLng)
                : new LatLng(ride.pickupLat, ride.pickupLng);
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
        String now = (online ? "on" : "off") + ":" + (ride == null ? "" : ride.id + ride.state);
        if (now.equals(showing)) {
            return;
        }
        showing = now;
        sheet.reset();
        countdown = null;
        header();
        chip();
        if (ride == null) {
            waiting();
        } else if (RideStates.OFFERED.equals(ride.state)) {
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
            today.add(Ui.label(Ui.money(earnings.todayCents, earnings.currency), "WlStatusValue"));
            today.add(Ui.label(Lang.tr("Today, {0} trips", String.valueOf(earnings.rides)),
                    "WlStatusDetail"));
            card.add(BorderLayout.EAST, today);
        }
        stage.header(card);
    }

    private void waiting() {
        Container peek = sheet.peek();
        Label title = Ui.label(online ? "Looking for rides" : "You are offline", "WlHeading");
        title.setName("driverState");
        peek.add(title);
        if (lastFare != null) {
            peek.add(Ui.text(Lang.tr("Trip complete: {0}", lastFare), "WlNotice"));
        }
        if (online) {
            peek.add(Ui.label("Stay close to busy streets; requests come to the nearest car.",
                    "WlMuted"));
            peek.add(FlowLayout.encloseCenter(new Pulse()));
        } else {
            peek.add(Ui.label("Go online to start taking rides.", "WlMuted"));
            Button go = new Button("GO", "WlGo");
            go.setName("goOnline");
            go.addActionListener(e -> report(true, Ui::fail));
            peek.add(FlowLayout.encloseCenter(go));
        }
        if (earnings != null) {
            StatRow numbers = Ui.stats(3);
            numbers.stat(Ui.money(earnings.todayCents, earnings.currency), "Today",
                    "earnedToday");
            numbers.stat(Ui.money(earnings.weekCents, earnings.currency), "This week",
                    "earnedWeek");
            numbers.stat(String.valueOf(earnings.rides), "Trips", "trips");
            peek.add(numbers);
        }
        if (online) {
            peek.add(Ui.secondary("Go offline", "goOffline", e -> report(false, Ui::fail)));
        }
    }

    private void offer() {
        final String id = ride.id;
        Container peek = sheet.peek();
        Label title = Ui.label("Ride request", "WlHeading");
        title.setName("driverState");
        countdown = new Ring();
        countdown.setName("countdown");
        countdown.start(ride.offerExpiresInSeconds, ride.offerExpiresInSeconds);
        Label fare = Ui.label(Ui.money(ride.fareCents, ride.currency), "WlFare");
        fare.setName("fare");
        Container words = new Container(BoxLayout.y());
        words.add(title);
        words.add(fare);
        Container top = new Container(new BorderLayout());
        top.add(BorderLayout.CENTER, words);
        top.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(countdown));
        peek.add(top);
        double away = Maps.meters(here, new LatLng(ride.pickupLat, ride.pickupLng));
        peek.add(Ui.row(FontImage.MATERIAL_TRIP_ORIGIN, ride.pickupAddress,
                Lang.tr("{0} away, {1}", Ui.distance(away), Ui.minutes(away / CITY_SPEED)), null));
        peek.add(Ui.row(FontImage.MATERIAL_PLACE, ride.dropoffAddress,
                Ui.distance(ride.distanceMeters) + Ui.DOT + Ui.minutes(ride.durationSeconds),
                null));
        peek.add(Ui.primary("Accept", "accept", e -> act(cb -> Api.driver().accept(id, cb),
                () -> Telemetry.offerAnswered(true))));
        peek.add(Ui.link("Decline", "decline", e -> act(cb -> Api.driver().decline(id, cb),
                () -> Telemetry.offerAnswered(false))));
        sheet.more().add(Ui.row(FontImage.MATERIAL_PERSON, ride.riderName, asked(), null));
        sheet.more().add(Ui.row(ride.cash ? FontImage.MATERIAL_PAYMENTS
                : FontImage.MATERIAL_CREDIT_CARD, ride.cash ? "Cash" : "Card",
                Lang.tr(Ui.product(ride.product)), null));
    }

    private void trip() {
        final String id = ride.id;
        String state = ride.state;
        Container peek = sheet.peek();
        Label title = Ui.label(RideStates.ACCEPTED.equals(state)
                ? Lang.tr("Pick up {0}", ride.riderName)
                : RideStates.ARRIVED.equals(state) ? Lang.tr("Waiting for {0}", ride.riderName)
                : Lang.tr("On the trip"), "WlHeading");
        title.setName("driverState");
        peek.add(title);
        boolean aboard = RideStates.IN_PROGRESS.equals(state);
        peek.add(Ui.row(aboard ? FontImage.MATERIAL_PLACE : FontImage.MATERIAL_TRIP_ORIGIN,
                aboard ? ride.dropoffAddress : ride.pickupAddress,
                aboard ? "Drop-off" : "Pickup", null));
        if (ride.cash) {
            // Said before the trip ends, so that it is not a surprise at the door.
            Label cash = Ui.label(Lang.tr("Collect {0} in cash", Ui.money(
                    ride.total > 0 ? ride.total : ride.fareCents, ride.currency)), "WlNotice");
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
        if (RideStates.ACCEPTED.equals(state)) {
            peek.add(Ui.primary("I have arrived", "arrived",
                    e -> act(cb -> Api.driver().arrived(id, cb))));
        } else if (RideStates.ARRIVED.equals(state)) {
            peek.add(Ui.primary("Start trip", "start",
                    e -> act(cb -> Api.driver().start(id, cb))));
        } else {
            peek.add(Ui.primary("Complete trip", "complete",
                    e -> Api.driver().complete(id, Net.to(finished -> {
                        Telemetry.rideCompleted("driver", finished.fareCents, finished.cash);
                        lastFare = Ui.money(finished.fareCents, finished.currency);
                        Ui.say(finished.cash ? Lang.tr("Collect {0} in cash", Ui.money(
                                finished.total > 0 ? finished.total : finished.fareCents,
                                finished.currency)) : Lang.tr("Trip complete: {0}", lastFare));
                        ride = null;
                        aim();
                        display();
                        loadEarnings();
                    }, Ui::fail))));
        }
        Container more = sheet.more();
        more.add(Ui.row(FontImage.MATERIAL_PERSON, ride.riderName, asked(), null));
        more.add(Ui.row(FontImage.MATERIAL_PLACE, ride.dropoffAddress,
                Ui.distance(ride.distanceMeters) + Ui.DOT + Ui.minutes(ride.durationSeconds),
                Ui.label(Ui.money(ride.fareCents, ride.currency), "WlRowValue")));
        // A rider who is aboard is taken where they are going: there is no
        // cancelling from here, only finishing.
        if (!aboard) {
            more.add(Ui.link("Cancel ride", "cancel",
                    e -> Api.driver().cancel(id, Net.to(cancelled -> {
                        Telemetry.rideCancelled("driver", state);
                        ride = null;
                        aim();
                        display();
                    }, Ui::fail))));
        }
    }

    private void call() {
        String number = ride == null ? null : ride.riderPhone;
        if (number == null || number.length() == 0 || !CN.canDial()) {
            Ui.say("Calling is not available on this device");
            return;
        }
        CN.dial(number);
    }

    private void message() {
        String number = ride == null ? null : ride.riderPhone;
        if (number == null || number.length() == 0) {
            Ui.say("Your rider cannot be messaged right now");
            return;
        }
        try {
            CN.sendSMS(number, Lang.tr("Hi, this is your Wayline driver."));
        } catch (IOException failed) {
            Ui.say("Messages are not available on this device");
        }
    }

    /// What the rider asked for with the ride, in a line.
    private String asked() {
        StringBuilder asked = new StringBuilder();
        if (ride.quietRide) {
            asked.append(Lang.tr("Quiet ride"));
        }
        if (ride.accessibleVehicle) {
            asked.append(asked.length() > 0 ? ", " : "").append(Lang.tr("Accessible vehicle"));
        }
        if (ride.petFriendly) {
            asked.append(asked.length() > 0 ? ", " : "").append(Lang.tr("Pet friendly"));
        }
        if (ride.note != null && ride.note.length() > 0) {
            asked.append(asked.length() > 0 ? ", " : "").append(ride.note);
        }
        return asked.toString();
    }
}
