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
package com.codenameone.examples.wayline.rider;

import com.codename1.maps.LatLng;
import com.codename1.components.SpanLabel;
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
import com.codenameone.examples.wayline.Prefs;
import com.codenameone.examples.wayline.Telemetry;
import com.codenameone.examples.wayline.api.FareOptionDto;
import com.codenameone.examples.wayline.api.FareQuoteDto;
import com.codenameone.examples.wayline.api.NearbyDriverDto;
import com.codenameone.examples.wayline.api.PaymentMethodDto;
import com.codenameone.examples.wayline.api.PlaceDto;
import com.codenameone.examples.wayline.api.RatingDto;
import com.codenameone.examples.wayline.api.RideDto;
import com.codenameone.examples.wayline.api.RideRequestDto;
import com.codenameone.examples.wayline.api.RideStates;
import com.codenameone.examples.wayline.api.TipDto;
import com.codenameone.examples.wayline.api.UserDto;
import com.codenameone.examples.wayline.live.LiveChannel;
import com.codenameone.examples.wayline.map.CarMarker;
import com.codenameone.examples.wayline.map.Locator;
import com.codenameone.examples.wayline.map.MapStage;
import com.codenameone.examples.wayline.map.Maps;
import com.codenameone.examples.wayline.map.Routes;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.net.Session;
import com.codenameone.examples.wayline.pay.ReceiptForm;
import com.codenameone.examples.wayline.pay.WalletForm;
import com.codenameone.examples.wayline.ui.BottomSheet;
import com.codenameone.examples.wayline.ui.Lang;
import com.codenameone.examples.wayline.ui.Menu;
import com.codenameone.examples.wayline.ui.Nav;
import com.codenameone.examples.wayline.ui.Pulse;
import com.codenameone.examples.wayline.ui.RideListForm;
import com.codenameone.examples.wayline.ui.Ui;
import com.codenameone.examples.wayline.ui.VerifyPhoneForm;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Riding: the map, a search card over the top of it, and a sheet at the
/// bottom that is whatever the ride needs next -- where to, what it costs, who
/// is coming, how it was.
///
/// The sheet follows the ride's state, and the state is the server's. Nothing
/// here moves a ride along; it asks (request, cancel, rate) and then shows what
/// the server says the ride now is, whether that came back from the call, from
/// the next poll, or because the live channel said to look.
public class RiderForm extends LiveChannel.Adapter implements Nav.Home {
    private static final int POLL_MILLIS = 3000;
    /// Meters a second in town, for a time of arrival worked out from a
    /// distance: 30 km/h.
    private static final double CITY_SPEED = 8.3;
    private static final Net.Failed QUIET = (status, message) -> { };

    private final Form form = Ui.home("Rider");
    private final Menu menu = new Menu(form, Nav.RIDER);
    private final MapStage stage;
    private final BottomSheet sheet;

    private LatLng here = Locator.here();
    private String hereAddress = "Current location";
    private PlaceDto destination;
    private Routes.Way way;
    private RideRequestDto request;
    private FareQuoteDto fare;
    /// The ride on screen: under way, or just finished and not yet dismissed.
    private RideDto ride;
    /// What the sheet is showing, so that a poll that changes nothing leaves
    /// it alone.
    private String showing = "";
    private com.codename1.maps.Marker pickupMarker;
    private com.codename1.maps.Marker dropoffMarker;
    private CarMarker car;
    private Maps.Line line;
    private int polls;
    /// The cars online near here, drawn while there is no ride to look at.
    private final List<CarMarker> nearby = new ArrayList<CarMarker>();
    /// What the ride being offered is asked for with: the kind of car and the
    /// way to pay, each of them changed on the offer itself.
    private String product = "standard";
    private PaymentMethodDto paying;
    private List<PaymentMethodDto> methods;

    public RiderForm() {
        menu.item(FontImage.MATERIAL_HAIL, "Ride", "home", () -> { });
        menu.item(FontImage.MATERIAL_HISTORY, "Your rides", "history",
                () -> RideListForm.show(form, "Your rides", cb -> Api.rider().history(cb), false,
                        (list, picked) -> {
                            if (RideStates.COMPLETED.equals(picked.state)) {
                                ReceiptForm.open(list, picked.id);
                            }
                        }));
        menu.item(FontImage.MATERIAL_ACCOUNT_BALANCE_WALLET, "Wallet", "wallet",
                () -> WalletForm.show(form));
        UserDto user = Session.user();
        if (!user.driver) {
            menu.item(FontImage.MATERIAL_DIRECTIONS_CAR, "Drive with Wayline", "drive",
                    () -> Nav.switchTo(Nav.DRIVER));
        }
        stage = new MapStage(here, Maps.STREET_ZOOM, menu.button());
        sheet = stage.sheet();
        stage.addRound(FontImage.MATERIAL_MY_LOCATION, "recentre", this::recentre);
        // The position comes when the platform has it, which is after this
        // screen is up: until then the screen stands on the demo city.
        stage.whenLocated((where, first) -> {
            here = where;
            if (first && ride == null && destination == null && "idle".equals(showing)) {
                start();
                loadNearby();
            }
        });
        form.add(stage.layers());
        menu.install();
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            refresh();
            loadMethods();
        });
        UITimer.timer(POLL_MILLIS, true, form, this::poll);
    }

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        idle();
        Api.geo().reverse(here.getLatitude(), here.getLongitude(), Net.to(place -> {
            if (place != null && place.name != null && place.name.length() > 0) {
                hereAddress = place.name;
            }
        }, QUIET));
    }

    public void show() {
        start();
        form.show();
    }

    // ------------------------------------------------------------ the server

    private void poll() {
        polls++;
        if (ride == null || !RideStates.isActive(ride.state)) {
            // Nothing to follow, so the map shows who is about: often enough
            // to see them move, seldom enough to cost nothing.
            if (ride == null && polls % 2 == 0) {
                loadNearby();
            }
            return;
        }
        // The live channel says when to look; the poll is for when it cannot.
        if (!LiveChannel.connected() || polls % 4 == 0) {
            refresh();
        }
    }

    private void refresh() {
        if (ride != null) {
            final String id = ride.id;
            Api.rider().ride(id, Net.to(this::update, QUIET));
        } else {
            Api.rider().active(Net.to(this::update, QUIET));
        }
    }

    @Override
    public void rideChanged(String id, String state) {
        refresh();
    }

    @Override
    public void driverMoved(String rideId, double lat, double lng, double heading) {
        if (ride != null && ride.id.equals(rideId)) {
            ride.driverLat = lat;
            ride.driverLng = lng;
            ride.driverHeading = heading;
            car();
        }
    }

    private void update(RideDto latest) {
        if (latest == null || RideStates.NONE.equals(latest.state)) {
            return;
        }
        boolean first = ride == null || !ride.id.equals(latest.id);
        // Seen to end here, and not found already over when the app started.
        if (!first && !RideStates.COMPLETED.equals(ride.state)
                && RideStates.COMPLETED.equals(latest.state)) {
            Telemetry.rideCompleted("rider", latest.fareCents, latest.cash);
        }
        ride = latest;
        if (first) {
            clearNearby();
            draw();
        }
        car();
        String now = latest.id + ":" + latest.state;
        if (!now.equals(showing)) {
            showing = now;
            rideDisplay();
        }
    }

    /// The rider's ways to pay, for the offer to name the one in use.
    private void loadMethods() {
        Api.payments().methods(Net.to(found -> {
            methods = found;
            paying = null;
            if ("offer".equals(showing) && fare != null) {
                offer();
            }
        }, QUIET));
    }

    /// The method this ride is paid with: the one picked for it, else the
    /// rider's default.
    private PaymentMethodDto method() {
        if (paying != null) {
            return paying;
        }
        for (int iter = 0; methods != null && iter < methods.size(); iter++) {
            if (methods.get(iter).isDefault) {
                return methods.get(iter);
            }
        }
        return null;
    }

    private void loadNearby() {
        Api.rider().nearby(here.getLatitude(), here.getLongitude(), Net.to(cars -> {
            if (ride == null) {
                showNearby(cars);
            }
        }, QUIET));
    }

    /// Moves the cars already drawn to where the cars are now. The server does
    /// not say which car is which -- a rider has no business knowing -- so each
    /// position goes to the nearest marker not yet used, which is the same car
    /// unless two have crossed, and then nobody can tell.
    private void showNearby(List<NearbyDriverDto> cars) {
        List<CarMarker> free = new ArrayList<CarMarker>(nearby);
        nearby.clear();
        for (int iter = 0; cars != null && iter < cars.size(); iter++) {
            NearbyDriverDto found = cars.get(iter);
            LatLng at = new LatLng(found.lat, found.lng);
            int closest = -1;
            double least = 0;
            for (int other = 0; other < free.size(); other++) {
                double meters = Maps.meters(free.get(other).position(), at);
                if (closest < 0 || meters < least) {
                    closest = other;
                    least = meters;
                }
            }
            if (closest >= 0) {
                CarMarker moved = free.remove(closest);
                moved.moveTo(at, found.heading);
                nearby.add(moved);
            } else {
                nearby.add(new CarMarker(stage.map, form, at, found.heading));
            }
        }
        for (int iter = 0; iter < free.size(); iter++) {
            free.get(iter).remove();
        }
        stage.map.repaint();
    }

    private void clearNearby() {
        for (int iter = 0; iter < nearby.size(); iter++) {
            nearby.get(iter).remove();
        }
        nearby.clear();
    }

    // --------------------------------------------------------------- the map

    private void clearMap() {
        if (pickupMarker != null) {
            stage.map.removeMarker(pickupMarker);
            pickupMarker = null;
        }
        if (dropoffMarker != null) {
            stage.map.removeMarker(dropoffMarker);
            dropoffMarker = null;
        }
        if (car != null) {
            car.remove();
            car = null;
        }
        if (line != null) {
            line.remove(stage.map);
            line = null;
        }
    }

    private void draw(LatLng pickup, LatLng dropoff, Routes.Way route) {
        clearMap();
        if (route != null) {
            line = Maps.route(stage.map, route.points);
        }
        pickupMarker = Maps.pickup(stage.map, pickup);
        dropoffMarker = Maps.dropoff(stage.map, dropoff);
        stage.frame(pickup, dropoff);
    }

    /// Draws the ride's two ends, and its route once that is known.
    private void draw() {
        final String id = ride.id;
        final LatLng pickup = new LatLng(ride.pickupLat, ride.pickupLng);
        final LatLng dropoff = new LatLng(ride.dropoffLat, ride.dropoffLng);
        draw(pickup, dropoff, null);
        Routes.find(pickup, dropoff, route -> {
            if (ride != null && ride.id.equals(id)) {
                draw(pickup, dropoff, route);
                car();
            }
        });
    }

    /// Puts the driver's car where the ride says it is, and says in the chip
    /// how long until it gets where it is going.
    private void car() {
        boolean assigned = ride != null && ride.driverUsername != null
                && ride.driverUsername.length() > 0 && RideStates.isActive(ride.state)
                && !RideStates.OFFERED.equals(ride.state)
                && (ride.driverLat != 0 || ride.driverLng != 0);
        if (!assigned) {
            if (car != null) {
                car.remove();
                car = null;
            }
            if (ride != null) {
                stage.chip(null, (char) 0);
            }
            return;
        }
        LatLng at = new LatLng(ride.driverLat, ride.driverLng);
        boolean first = false;
        if (car == null) {
            car = new CarMarker(stage.map, form, at, ride.driverHeading);
            first = true;
        } else {
            car.moveTo(at, ride.driverHeading);
        }
        stage.map.repaint();
        if (RideStates.ARRIVED.equals(ride.state)) {
            stage.chip(Lang.tr("Your driver is here"), FontImage.MATERIAL_PLACE);
            return;
        }
        boolean aboard = RideStates.IN_PROGRESS.equals(ride.state);
        double meters = Maps.meters(at, aboard ? new LatLng(ride.dropoffLat, ride.dropoffLng)
                : new LatLng(ride.pickupLat, ride.pickupLng));
        stage.chip(Lang.tr(aboard ? "{0} to destination" : "Pickup in {0}",
                Ui.minutes(meters / CITY_SPEED)) + Ui.DOT + Ui.distance(meters),
                FontImage.MATERIAL_SCHEDULE);
        if (first) {
            // The car has just appeared: the map is fitted again with it in.
            recentre();
        }
    }

    private void recentre() {
        if (ride != null && RideStates.isActive(ride.state)) {
            stage.frame(new LatLng(ride.pickupLat, ride.pickupLng),
                    new LatLng(ride.dropoffLat, ride.dropoffLng), car == null ? null
                            : car.position());
        } else if (destination != null) {
            stage.frame(here, new LatLng(destination.lat, destination.lng));
        } else {
            stage.center(here, Maps.STREET_ZOOM);
        }
    }

    // ------------------------------------------------------------- the sheet

    private void display(String key) {
        showing = key;
        sheet.reset();
    }

    /// The card at the top: the search when there is nowhere to go yet, and
    /// from where to where once there is.
    private void header(String to) {
        if (to == null) {
            Button where = new Button("Where to?", "WlSearchCard");
            where.setName("whereTo");
            Ui.icon(where, FontImage.MATERIAL_SEARCH, 3.8f);
            where.addActionListener(e -> PlaceSearchForm.show(form, here, this::go));
            stage.header(where);
            return;
        }
        Container trip = new Container(BoxLayout.y());
        trip.setUIID("WlTripCard");
        trip.add(stop(FontImage.MATERIAL_TRIP_ORIGIN, hereAddress));
        trip.add(stop(FontImage.MATERIAL_PLACE, to));
        stage.header(trip);
    }

    private static Label stop(char icon, String text) {
        Label stop = Ui.label(text, "WlTripStop");
        Ui.icon(stop, icon, 2.8f);
        return stop;
    }

    /// Nothing under way: where to?
    private void idle() {
        display("idle");
        ride = null;
        destination = null;
        way = null;
        fare = null;
        request = null;
        paying = null;
        product = "standard";
        clearMap();
        stage.chip(null, (char) 0);
        stage.center(here, Maps.STREET_ZOOM);
        header(null);
        UserDto user = Session.user();
        Container peek = sheet.peek();
        peek.add(Ui.label(Lang.tr("Hello, {0}", firstName(user.displayName)), "WlHeading"));
        peek.add(Ui.label("Where are you going?", "WlMuted"));
        if (!user.phoneVerified) {
            peek.add(Ui.text("Verify your phone number to request a ride.", "WlNotice"));
            peek.add(Ui.link("Verify now", "verify",
                    e -> VerifyPhoneForm.show(() -> new RiderForm().show())));
        }
        // Home and work, always offered: with a place, they go there; without
        // one, they ask for it.
        Container saved = new Container(new GridLayout(1, 2));
        saved.add(shortcut(FontImage.MATERIAL_HOME, "Home", "home", Prefs.home()));
        saved.add(shortcut(FontImage.MATERIAL_WORK, "Work", "work", Prefs.work()));
        peek.add(saved);
        List<PlaceDto> recents = Prefs.recents();
        for (int iter = 0; iter < recents.size(); iter++) {
            final PlaceDto place = recents.get(iter);
            sheet.more().add(Ui.tap(Ui.row(FontImage.MATERIAL_SCHEDULE, place.name, place.address,
                    null), "recent-" + iter, e -> go(place)));
        }
        sheet.ready(false);
        loadNearby();
    }

    private Button shortcut(char icon, String title, final String which, final PlaceDto place) {
        Button shortcut = new Button(title, "WlShortcut");
        shortcut.setName("shortcut-" + which);
        Ui.icon(shortcut, icon, 3.4f);
        shortcut.addActionListener(e -> {
            if (place != null) {
                go(place);
                return;
            }
            PlaceSearchForm.show(form, here, picked -> {
                if ("home".equals(which)) {
                    Prefs.setHome(picked);
                } else {
                    Prefs.setWork(picked);
                }
                idle();
            });
        });
        return shortcut;
    }

    /// A destination is chosen.
    private void go(final PlaceDto place) {
        if (place.lat == 0 && place.lng == 0) {
            // A place that came with the account from another device: the
            // server keeps what it is called, and where that is is looked up.
            Api.geo().search(place.name, here.getLatitude(), here.getLongitude(), Net.to(found -> {
                if (found == null || found.isEmpty()) {
                    Ui.say("That place could not be found. Choose it again.");
                    return;
                }
                place.lat = found.get(0).lat;
                place.lng = found.get(0).lng;
                go(place);
            }, Ui::fail));
            return;
        }
        destination = place;
        Prefs.addRecent(place);
        quote();
    }

    /// The route to the destination, and what it would cost.
    private void quote() {
        display("quote");
        header(destination.name);
        sheet.peek().add(Ui.label(destination.name, "WlHeading"));
        sheet.peek().add(Ui.label("Working out the fare", "WlMuted"));
        sheet.peek().add(FlowLayout.encloseCenter(new Pulse()));
        sheet.ready(false);
        final PlaceDto chosen = destination;
        final LatLng to = new LatLng(chosen.lat, chosen.lng);
        Routes.find(here, to, route -> {
            if (destination != chosen) { //NOPMD CompareObjectsWithEquals
                return;
            }
            way = route;
            draw(here, to, route);
            request = request();
            Api.rider().quote(request, Net.to(quoted -> {
                if (destination == chosen) { //NOPMD CompareObjectsWithEquals
                    fare = quoted;
                    Telemetry.rideQuoted(quoted.options == null ? 0 : quoted.options.size(),
                            quoted.distanceMeters);
                    offer();
                }
            }, (status, message) -> {
                Ui.fail(status, message);
                idle();
            }));
        });
    }

    /// The request, with what the rider asked for in Settings.
    private RideRequestDto request() {
        RideRequestDto request = new RideRequestDto();
        request.pickupLat = here.getLatitude();
        request.pickupLng = here.getLongitude();
        request.pickupAddress = hereAddress;
        request.dropoffLat = destination.lat;
        request.dropoffLng = destination.lng;
        request.dropoffAddress = destination.name;
        request.distanceMeters = way.meters;
        request.durationSeconds = way.seconds;
        request.paymentMethodId = Prefs.paymentMethod();
        request.driverGender = Prefs.driverGender();
        request.quietRide = Prefs.quietRide();
        request.accessibleVehicle = Prefs.accessibleVehicle();
        return request;
    }

    /// The offer: the kinds of car there are and what each costs, how it is
    /// paid, and the button that asks for it.
    private void offer() {
        display("offer");
        clearNearby();
        // How far and how long is said in the sheet here, not in the chip:
        // the sheet leaves a strip of map just tall enough for the route, and
        // a chip in it would sit on the pins.
        stage.chip(null, (char) 0);
        Container peek = sheet.peek();
        Label length = Ui.plain(Ui.distance(fare.distanceMeters) + Ui.DOT
                + Ui.minutes(fare.durationSeconds), "WlMuted");
        length.setName("tripLength");
        Container headed = new Container(new BorderLayout());
        headed.add(BorderLayout.CENTER, Ui.label("Choose a ride", "WlHeading"));
        headed.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(length));
        peek.add(headed);
        FareOptionDto picked = null;
        List<FareOptionDto> options = fare.options;
        for (int iter = 0; options != null && iter < options.size(); iter++) {
            final FareOptionDto option = options.get(iter);
            boolean mine = option.product.equals(product);
            if (mine) {
                picked = option;
            }
            peek.add(option(option, mine));
        }
        long price = picked == null ? fare.amountCents : picked.total;
        Label total = Ui.plain(Ui.money(price, fare.currency), "WlRowValue");
        total.setName("fare");
        final PaymentMethodDto method = method();
        Container pay = Ui.row(method == null || "cash".equals(method.kind)
                ? FontImage.MATERIAL_PAYMENTS : FontImage.MATERIAL_CREDIT_CARD,
                WalletForm.label(method), asked(), total);
        ((Label) ((Container) pay.getComponentAt(1)).getComponentAt(0)).setName("payWith");
        peek.add(Ui.tap(pay, "payment", e -> WalletForm.pick(form,
                method == null ? Prefs.PAYMENT_CASH : method.id, chosen -> {
                    paying = chosen;
                    offer();
                })));
        final Button go = Ui.primary(picked == null ? Lang.tr("Request ride")
                : Lang.tr("Request {0}", Lang.tr(picked.name)), "request", null);
        go.addActionListener(e -> {
            go.setEnabled(false);
            request.product = product;
            PaymentMethodDto with = method();
            request.paymentMethodId = with == null ? Prefs.PAYMENT_CASH : with.id;
            final boolean cash = with == null || "cash".equals(with.kind);
            Api.rider().request(request, Net.to(asked -> {
                Telemetry.rideRequested(product, cash);
                update(asked);
            }, (status, message) -> {
                go.setEnabled(true);
                Ui.fail(status, message);
            }));
        });
        peek.add(go);
        peek.add(Ui.link("Cancel", "back", e -> idle()));
        sheet.ready(false);
        stage.frame(here, new LatLng(destination.lat, destination.lng));
    }

    /// One kind of car: what it is, how long until one comes, what it costs.
    private Container option(final FareOptionDto option, boolean picked) {
        Container row = new Container(new BorderLayout());
        row.setUIID(picked ? "WlOptionPicked" : "WlOption");
        Label icon = new Label("", "WlOptionIcon");
        Ui.icon(icon, "xl".equals(option.product)
                ? FontImage.MATERIAL_AIRPORT_SHUTTLE : "comfort".equals(option.product)
                ? FontImage.MATERIAL_LOCAL_TAXI : FontImage.MATERIAL_DIRECTIONS_CAR, 4.4f);
        row.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(icon));
        Container lines = new Container(BoxLayout.y());
        // How many it seats sits beside the name as a figure, which leaves
        // the line under it for how far off a car is, whole in any language.
        Label seats = Ui.plain(String.valueOf(option.seats), "WlRowDetail");
        Ui.icon(seats, FontImage.MATERIAL_PERSON, 2.5f);
        Container named = new Container(new BorderLayout());
        named.add(BorderLayout.WEST, Ui.label(option.name, "WlRowTitle"));
        named.add(BorderLayout.CENTER, seats);
        lines.add(named);
        lines.add(Ui.plain(option.driversNearby == 0 ? Lang.tr("None nearby")
                : Lang.tr("{0} away", Ui.minutes(option.etaMinutes * 60d)), "WlRowDetail"));
        row.add(BorderLayout.CENTER, lines);
        Label price = Ui.plain(Ui.money(option.total, fare.currency), "WlRowValue");
        price.setName("price-" + option.product);
        row.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(price));
        return Ui.tap(row, "product-" + option.product, e -> {
            product = option.product;
            offer();
        });
    }

    /// What the rider's settings ask of every ride, in a line.
    private static String asked() {
        StringBuilder asked = new StringBuilder();
        if (Prefs.GENDER_WOMEN.equals(Prefs.driverGender())) {
            asked.append(Lang.tr("Women drivers only"));
        }
        if (Prefs.quietRide()) {
            asked.append(asked.length() > 0 ? ", " : "").append(Lang.tr("Quiet ride"));
        }
        if (Prefs.accessibleVehicle()) {
            asked.append(asked.length() > 0 ? ", " : "").append(Lang.tr("Accessible vehicle"));
        }
        if (Prefs.petFriendly()) {
            asked.append(asked.length() > 0 ? ", " : "").append(Lang.tr("Pet friendly"));
        }
        return asked.toString();
    }

    /// A ride exists: the sheet is its state.
    private void rideDisplay() {
        sheet.reset();
        String state = ride.state;
        Label title = Ui.label(Ui.state(state), "WlHeading");
        title.setName("rideState");
        Container peek = sheet.peek();
        header(ride.dropoffAddress);
        if (RideStates.REQUESTED.equals(state) || RideStates.OFFERED.equals(state)) {
            stage.chip(null, (char) 0);
            peek.add(title);
            peek.add(Ui.label(Lang.tr("To {0}", ride.dropoffAddress), "WlMuted"));
            peek.add(FlowLayout.encloseCenter(new Pulse()));
            peek.add(cancelButton());
        } else if (RideStates.isActive(state)) {
            peek.add(title);
            peek.add(driverCard());
            peek.add(actions(!RideStates.IN_PROGRESS.equals(state)));
            Container more = sheet.more();
            more.add(Ui.row(FontImage.MATERIAL_TRIP_ORIGIN, ride.pickupAddress, null, null));
            more.add(Ui.row(FontImage.MATERIAL_PLACE, ride.dropoffAddress, null,
                    Ui.label(Ui.money(ride.fareCents, ride.currency), "WlRowValue")));
            more.add(Ui.row(FontImage.MATERIAL_PAYMENTS, "Payment", paid(), null));
        } else if (RideStates.COMPLETED.equals(state)) {
            stage.chip(null, (char) 0);
            receipt();
        } else {
            stage.chip(null, (char) 0);
            peek.add(title);
            peek.add(Ui.text(RideStates.NO_DRIVERS.equals(state)
                    ? "Nobody could take the ride just now. You have not been charged."
                    : RideStates.CANCELLED_BY_DRIVER.equals(state)
                            ? "Your driver had to cancel. You have not been charged."
                            : "The ride was cancelled.", "WlText"));
            peek.add(Ui.primary("OK", "dismiss", e -> idle()));
        }
        sheet.ready(false);
    }

    /// Who is coming, and in what.
    private Container driverCard() {
        Container card = new Container(new BorderLayout());
        card.setUIID("WlDriverCard");
        String initials = ride.driverPhotoInitials;
        Label face = Ui.avatar(ride.driverName);
        if (initials != null && initials.length() > 0) {
            face.setText(initials);
        }
        Container lines = new Container(BoxLayout.y());
        Label name = Ui.plain(ride.driverName, "WlRowTitle");
        Ui.shrink(name, 2.8f);
        lines.add(name);
        // The rating leads the line about the car, with its star.
        Label car = Ui.plain(ride.driverRating > 0
                ? Ui.tenths(ride.driverRating) + Ui.DOT + ride.vehicle : ride.vehicle,
                "WlRowDetail");
        if (ride.driverRating > 0) {
            Ui.icon(car, FontImage.MATERIAL_STAR, 2.5f);
        }
        Ui.shrink(car, 2.2f);
        lines.add(car);
        if (ride.plate != null && ride.plate.length() > 0) {
            // The plate has a line of its own: beside the name it cut the
            // name short on a narrow phone.
            Label plate = new Label("", "WlPlate");
            // A plate is a plate in any language.
            plate.setShouldLocalize(false);
            plate.setText(ride.plate);
            lines.add(FlowLayout.encloseIn(plate));
        }
        card.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(face));
        card.add(BorderLayout.CENTER, lines);
        return card;
    }

    /// What can be done about a driver who is on the way.
    private Container actions(boolean cancellable) {
        Container actions = new Container(new GridLayout(1, cancellable ? 4 : 3));
        actions.setUIID("WlActions");
        // The driver's number comes with the ride while the driver has it,
        // and is gone from it when the ride is over.
        actions.add(Ui.action(FontImage.MATERIAL_CALL, "Call", "call", e -> call()));
        actions.add(Ui.action(FontImage.MATERIAL_SMS, "Message", "message", e -> message()));
        actions.add(Ui.action(FontImage.MATERIAL_SHARE, "Share", "share", e -> CN.share(
                Lang.tr("I am on my way to {0} with {1} in a {2} ({3}).", ride.dropoffAddress,
                        ride.driverName, ride.vehicle, ride.plate), null, null)));
        if (cancellable) {
            final String id = ride.id;
            final String state = ride.state;
            Button cancel = Ui.action(FontImage.MATERIAL_CLOSE, "Cancel", "cancel",
                    e -> Api.rider().cancel(id, Net.to(cancelled -> {
                        Telemetry.rideCancelled("rider", state);
                        idle();
                    }, Ui::fail)));
            cancel.setUIID("WlActionDanger");
            actions.add(cancel);
        }
        return actions;
    }

    private void call() {
        String number = ride == null ? null : ride.driverPhone;
        if (number == null || number.length() == 0 || !CN.canDial()) {
            Ui.say("Calling is not available on this device");
            return;
        }
        CN.dial(number);
    }

    private void message() {
        String number = ride == null ? null : ride.driverPhone;
        if (number == null || number.length() == 0) {
            Ui.say("Your driver cannot be messaged right now");
            return;
        }
        try {
            CN.sendSMS(number, Lang.tr("Hi, this is your Wayline rider."));
        } catch (IOException failed) {
            Ui.say("Messages are not available on this device");
        }
    }

    private String paid() {
        String label = ride.paymentMethodLabel;
        return ride.cash || label == null || label.length() == 0 ? Lang.tr("Cash") : label;
    }

    private Button cancelButton() {
        final String id = ride.id;
        final String state = ride.state;
        return Ui.danger("Cancel ride", "cancel",
                e -> Api.rider().cancel(id, Net.to(cancelled -> {
                    Telemetry.rideCancelled("rider", state);
                    idle();
                }, Ui::fail)));
    }

    /// The ride is over: what it cost, how it was, and a tip if the rider
    /// wants to leave one. The stars are sent as they are tapped; the tip goes
    /// with Done, so that a slip of the thumb is not a charge.
    private void receipt() {
        Container peek = sheet.peek();
        Label title = Ui.label("You have arrived", "WlHeading");
        title.setName("rideState");
        Label price = Ui.plain(Ui.money(ride.total > 0 ? ride.total : ride.fareCents,
                ride.currency), "WlPrice");
        price.setName("fare");
        Container top = new Container(new BorderLayout());
        top.add(BorderLayout.CENTER, title);
        top.add(BorderLayout.EAST, price);
        peek.add(top);
        Label how = Ui.plain(Ui.distance(ride.distanceMeters) + Ui.DOT
                + Ui.minutes(ride.durationSeconds) + Ui.DOT + paid(), "WlMuted");
        how.setName("paidWith");
        peek.add(how);
        if ("FAILED".equals(ride.paymentStatus)) {
            peek.add(Ui.text("The card was declined. Choose another way to pay in your "
                    + "wallet.", "WlNotice"));
        }
        SpanLabel asked = Ui.text("", "WlText");
        asked.setShouldLocalize(false);
        asked.setText(Lang.tr("How was the ride with {0}?", ride.driverName));
        peek.add(asked);
        final Container stars = new Container(new FlowLayout(CN.CENTER));
        final String id = ride.id;
        for (int star = 1; star <= 5; star++) {
            final int given = star;
            Button button = new Button("", "WlStar");
            button.setName("star-" + star);
            Ui.icon(button, FontImage.MATERIAL_STAR_BORDER, 5.6f);
            button.addActionListener(e -> {
                for (int iter = 0; iter < 5; iter++) {
                    Ui.icon((Button) stars.getComponentAt(iter),
                            iter < given ? FontImage.MATERIAL_STAR
                                    : FontImage.MATERIAL_STAR_BORDER, 5.6f);
                }
                stars.revalidate();
                RatingDto rating = new RatingDto();
                rating.stars = given;
                Api.rider().rate(id, rating, Net.to(rated -> Telemetry.rated(given),
                        Ui::fail));
            });
            stars.add(button);
        }
        peek.add(stars);
        final int[] percent = {Prefs.tipPercent()};
        boolean tippable = "PAID".equals(ride.paymentStatus) && ride.tip == 0;
        if (tippable) {
            peek.add(Ui.label("Tip", "WlLabel"));
            peek.add(Ui.choice("tip", new String[] {"None", "10%", "15%", "20%"},
                    new String[] {"0", "10", "15", "20"}, String.valueOf(percent[0]),
                    value -> percent[0] = Integer.parseInt(value)));
        }
        final long fareCents = ride.fareCents;
        final boolean tipping = tippable;
        peek.add(Ui.primary("Done", "done", e -> {
            if (tipping && percent[0] > 0) {
                TipDto tip = new TipDto();
                tip.amount = Math.max(100L, Math.round(fareCents * percent[0] / 100d));
                final int share = percent[0];
                Api.payments().tip(id, tip, Net.to(tipped -> {
                    Telemetry.tipped("ride", share);
                    Ui.say("Thank you. Your tip was sent.");
                }, Ui::fail));
            }
            idle();
        }));
        peek.add(Ui.link("View receipt", "receipt", e -> ReceiptForm.open(form, id)));
        Container more = sheet.more();
        more.add(Ui.row(FontImage.MATERIAL_TRIP_ORIGIN, ride.pickupAddress, null, null));
        more.add(Ui.row(FontImage.MATERIAL_PLACE, ride.dropoffAddress, null, null));
        more.add(Ui.row(FontImage.MATERIAL_PERSON, ride.driverName, ride.vehicle, null));
    }

    private static String firstName(String name) {
        String text = name == null ? "" : name.trim();
        int space = text.indexOf(' ');
        return space < 0 ? text : text.substring(0, space);
    }
}
