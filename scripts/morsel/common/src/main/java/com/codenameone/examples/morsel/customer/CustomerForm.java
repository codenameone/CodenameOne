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
package com.codenameone.examples.morsel.customer;

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
import com.codenameone.examples.morsel.Prefs;
import com.codenameone.examples.morsel.Telemetry;
import com.codenameone.examples.morsel.api.FareOptionDto;
import com.codenameone.examples.morsel.api.FareQuoteDto;
import com.codenameone.examples.morsel.api.NearbyCourierDto;
import com.codenameone.examples.morsel.api.PaymentMethodDto;
import com.codenameone.examples.morsel.api.PlaceDto;
import com.codenameone.examples.morsel.api.RatingDto;
import com.codenameone.examples.morsel.api.DeliveryDto;
import com.codenameone.examples.morsel.api.DeliveryRequestDto;
import com.codenameone.examples.morsel.api.DeliveryState;
import com.codenameone.examples.morsel.api.TipDto;
import com.codenameone.examples.morsel.api.UserDto;
import com.codenameone.examples.morsel.live.LiveChannel;
import com.codenameone.examples.morsel.map.CarMarker;
import com.codenameone.examples.morsel.map.Locator;
import com.codenameone.examples.morsel.map.MapStage;
import com.codenameone.examples.morsel.map.Maps;
import com.codenameone.examples.morsel.map.Routes;
import com.codenameone.examples.morsel.net.Api;
import com.codenameone.examples.morsel.net.Net;
import com.codenameone.examples.morsel.net.Session;
import com.codenameone.examples.morsel.pay.ReceiptForm;
import com.codenameone.examples.morsel.pay.WalletForm;
import com.codenameone.examples.morsel.ui.BottomSheet;
import com.codenameone.examples.morsel.ui.Lang;
import com.codenameone.examples.morsel.ui.Menu;
import com.codenameone.examples.morsel.ui.Nav;
import com.codenameone.examples.morsel.ui.Pulse;
import com.codenameone.examples.morsel.ui.DeliveryListForm;
import com.codenameone.examples.morsel.ui.Ui;
import com.codenameone.examples.morsel.ui.VerifyPhoneForm;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Riding: the map, a search card over the top of it, and a sheet at the
/// bottom that is whatever the delivery needs next -- where to, what it costs, who
/// is coming, how it was.
///
/// The sheet follows the delivery's state, and the state is the server's. Nothing
/// here moves a delivery along; it asks (request, cancel, rate) and then shows what
/// the server says the delivery now is, whether that came back from the call, from
/// the next poll, or because the live channel said to look.
public class CustomerForm extends LiveChannel.Adapter implements Nav.Home {
    private static final int POLL_MILLIS = 3000;
    /// Meters a second in town, for a time of arrival worked out from a
    /// distance: 30 km/h.
    private static final double CITY_SPEED = 8.3;
    private static final Net.Failed QUIET = (status, message) -> { };

    private final Form form = Ui.home("Customer");
    private final Menu menu = new Menu(form, Nav.CUSTOMER);
    private final MapStage stage;
    private final BottomSheet sheet;

    private LatLng here = Locator.here();
    private String hereAddress = "Current location";
    private PlaceDto destination;
    private Routes.Way way;
    private DeliveryRequestDto request;
    private FareQuoteDto fare;
    /// The delivery on screen: under way, or just finished and not yet dismissed.
    private DeliveryDto delivery;
    /// Where the delivery's car is: as the delivery last said, or as the live channel
    /// said since, which is more recent than any poll.
    private double carLat;
    private double carLng;
    private double carHeading;
    /// What the sheet is showing, so that a poll that changes nothing leaves
    /// it alone.
    private String showing = "";
    private com.codename1.maps.Marker pickupMarker;
    private com.codename1.maps.Marker dropoffMarker;
    private CarMarker car;
    private Maps.Line line;
    private int polls;
    /// The cars online near here, drawn while there is no delivery to look at.
    private final List<CarMarker> nearby = new ArrayList<CarMarker>();
    /// What the delivery being offered is asked for with: the kind of car and the
    /// way to pay, each of them changed on the offer itself.
    private String product = "standard";
    private PaymentMethodDto paying;
    private List<PaymentMethodDto> methods;

    public CustomerForm() {
        menu.item(FontImage.MATERIAL_HAIL, "Delivery", "home", () -> { });
        menu.item(FontImage.MATERIAL_HISTORY, "Your deliveries", "history",
                () -> DeliveryListForm.show(form, "Your deliveries", cb -> Api.customer().history(cb), false,
                        (list, picked) -> {
                            if (DeliveryState.COMPLETED == picked.state()) {
                                ReceiptForm.open(list, picked.id());
                            }
                        }));
        menu.item(FontImage.MATERIAL_ACCOUNT_BALANCE_WALLET, "Wallet", "wallet",
                () -> WalletForm.show(form));
        UserDto user = Session.user();
        if (!user.courier()) {
            menu.item(FontImage.MATERIAL_DIRECTIONS_CAR, "Drive with Morsel", "drive",
                    () -> Nav.switchTo(Nav.COURIER));
        }
        stage = new MapStage(here, Maps.STREET_ZOOM, menu.button());
        sheet = stage.sheet();
        stage.addRound(FontImage.MATERIAL_MY_LOCATION, "recentre", this::recentre);
        // The position comes when the platform has it, which is after this
        // screen is up: until then the screen stands on the demo city.
        stage.whenLocated((where, first) -> {
            here = where;
            if (first && delivery == null && destination == null && "idle".equals(showing)) {
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
            if (place != null && place.name() != null && place.name().length() > 0) {
                hereAddress = place.name();
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
        if (delivery == null || !delivery.state().isActive()) {
            // Nothing to follow, so the map shows who is about: often enough
            // to see them move, seldom enough to cost nothing.
            if (delivery == null && polls % 2 == 0) {
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
        if (delivery != null) {
            final String id = delivery.id();
            Api.customer().delivery(id, Net.to(this::update, QUIET));
        } else {
            Api.customer().active(Net.to(this::update, QUIET));
        }
    }

    @Override
    public void deliveryChanged(String id, String state) {
        refresh();
    }

    @Override
    public void courierMoved(String deliveryId, double lat, double lng, double heading) {
        if (delivery != null && delivery.id().equals(deliveryId)) {
            carLat = lat;
            carLng = lng;
            carHeading = heading;
            car();
        }
    }

    private void update(DeliveryDto latest) {
        if (latest == null || DeliveryState.NONE == latest.state()) {
            return;
        }
        boolean first = delivery == null || !delivery.id().equals(latest.id());
        // Seen to end here, and not found already over when the app started.
        if (!first && DeliveryState.COMPLETED != delivery.state()
                && DeliveryState.COMPLETED == latest.state()) {
            Telemetry.deliveryCompleted("customer", latest.fareCents(), latest.cash());
        }
        delivery = latest;
        carLat = latest.courierLat();
        carLng = latest.courierLng();
        carHeading = latest.courierHeading();
        if (first) {
            clearNearby();
            draw();
        }
        car();
        DeliveryCard.follow(latest);
        String now = latest.id() + ":" + latest.state();
        if (!now.equals(showing)) {
            showing = now;
            deliveryDisplay();
        }
    }

    /// The customer's ways to pay, for the offer to name the one in use.
    private void loadMethods() {
        Api.payments().methods(Net.to(found -> {
            methods = found;
            paying = null;
            if ("offer".equals(showing) && fare != null) {
                offer();
            }
        }, QUIET));
    }

    /// The method this delivery is paid with: the one picked for it, else the
    /// customer's default.
    private PaymentMethodDto method() {
        if (paying != null) {
            return paying;
        }
        for (int iter = 0; methods != null && iter < methods.size(); iter++) {
            if (methods.get(iter).isDefault()) {
                return methods.get(iter);
            }
        }
        return null;
    }

    private void loadNearby() {
        Api.customer().nearby(here.getLatitude(), here.getLongitude(), Net.to(cars -> {
            if (delivery == null) {
                showNearby(cars);
            }
        }, QUIET));
    }

    /// Moves the cars already drawn to where the cars are now. The server does
    /// not say which car is which -- a customer has no business knowing -- so each
    /// position goes to the nearest marker not yet used, which is the same car
    /// unless two have crossed, and then nobody can tell.
    private void showNearby(List<NearbyCourierDto> cars) {
        List<CarMarker> free = new ArrayList<CarMarker>(nearby);
        nearby.clear();
        for (int iter = 0; cars != null && iter < cars.size(); iter++) {
            NearbyCourierDto found = cars.get(iter);
            LatLng at = new LatLng(found.lat(), found.lng());
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
                moved.moveTo(at, found.heading());
                nearby.add(moved);
            } else {
                nearby.add(new CarMarker(stage.map, form, at, found.heading()));
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

    /// Draws the delivery's two ends, and its route once that is known.
    private void draw() {
        final String id = delivery.id();
        final LatLng pickup = new LatLng(delivery.pickupLat(), delivery.pickupLng());
        final LatLng dropoff = new LatLng(delivery.dropoffLat(), delivery.dropoffLng());
        draw(pickup, dropoff, null);
        Routes.find(pickup, dropoff, route -> {
            if (delivery != null && delivery.id().equals(id)) {
                draw(pickup, dropoff, route);
                car();
            }
        });
    }

    /// Puts the courier's car where the delivery says it is, and says in the chip
    /// how long until it gets where it is going.
    private void car() {
        // By the courier's name: the server keeps the courier's sign-in address
        // to the courier.
        boolean assigned = delivery != null && delivery.courierName() != null
                && delivery.courierName().length() > 0 && delivery.state().isActive()
                && DeliveryState.OFFERED != delivery.state()
                && (carLat != 0 || carLng != 0);
        if (!assigned) {
            if (car != null) {
                car.remove();
                car = null;
            }
            if (delivery != null) {
                stage.chip(null, (char) 0);
            }
            return;
        }
        LatLng at = new LatLng(carLat, carLng);
        boolean first = false;
        if (car == null) {
            car = new CarMarker(stage.map, form, at, carHeading);
            first = true;
        } else {
            car.moveTo(at, carHeading);
        }
        stage.map.repaint();
        if (DeliveryState.ARRIVED == delivery.state()) {
            stage.chip(Lang.tr("Your courier is here"), FontImage.MATERIAL_PLACE);
            return;
        }
        boolean aboard = DeliveryState.IN_PROGRESS == delivery.state();
        double meters = Maps.meters(at, aboard ? new LatLng(delivery.dropoffLat(), delivery.dropoffLng())
                : new LatLng(delivery.pickupLat(), delivery.pickupLng()));
        stage.chip(Lang.tr(aboard ? "{0} to destination" : "Pickup in {0}",
                Ui.minutes(meters / CITY_SPEED)) + Ui.DOT + Ui.distance(meters),
                FontImage.MATERIAL_SCHEDULE);
        if (first) {
            // The car has just appeared: the map is fitted again with it in.
            recentre();
        }
    }

    private void recentre() {
        if (delivery != null && delivery.state().isActive()) {
            stage.frame(new LatLng(delivery.pickupLat(), delivery.pickupLng()),
                    new LatLng(delivery.dropoffLat(), delivery.dropoffLng()), car == null ? null
                            : car.position());
        } else if (destination != null) {
            stage.frame(here, new LatLng(destination.lat(), destination.lng()));
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
        delivery = null;
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
        peek.add(Ui.label(Lang.tr("Hello, {0}", firstName(user.displayName())), "WlHeading"));
        peek.add(Ui.label("Where are you going?", "WlMuted"));
        if (!user.phoneVerified()) {
            peek.add(Ui.text("Verify your phone number to request a delivery.", "WlNotice"));
            peek.add(Ui.link("Verify now", "verify",
                    e -> VerifyPhoneForm.show(() -> new CustomerForm().show())));
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
            sheet.more().add(Ui.tap(Ui.row(FontImage.MATERIAL_SCHEDULE, place.name(),
                    place.address(), null), "recent-" + iter, e -> go(place)));
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
        if (place.lat() == 0 && place.lng() == 0) {
            // A place that came with the account from another device: the
            // server keeps what it is called, and where that is is looked up.
            double lat = here.getLatitude();
            double lng = here.getLongitude();
            Api.geo().search(place.name(), lat, lng, Net.to(found -> {
                if (found == null || found.isEmpty()) {
                    Ui.say("That place could not be found. Choose it again.");
                    return;
                }
                go(new PlaceDto(place.name(), place.address(), found.get(0).lat(),
                        found.get(0).lng()));
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
        header(destination.name());
        sheet.peek().add(Ui.label(destination.name(), "WlHeading"));
        sheet.peek().add(Ui.label("Working out the fare", "WlMuted"));
        sheet.peek().add(FlowLayout.encloseCenter(new Pulse()));
        sheet.ready(false);
        final PlaceDto chosen = destination;
        final LatLng to = new LatLng(chosen.lat(), chosen.lng());
        Routes.find(here, to, route -> {
            if (destination != chosen) { //NOPMD CompareObjectsWithEquals
                return;
            }
            way = route;
            draw(here, to, route);
            request = request();
            Api.customer().quote(request, Net.to(quoted -> {
                if (destination == chosen) { //NOPMD CompareObjectsWithEquals
                    fare = quoted;
                    Telemetry.deliveryQuoted(quoted.options() == null ? 0 : quoted.options().size(),
                            quoted.distanceMeters());
                    offer();
                }
            }, (status, message) -> {
                Ui.fail(status, message);
                idle();
            }));
        });
    }

    /// The request, with what the customer asked for in Settings.
    private DeliveryRequestDto request() {
        return new DeliveryRequestDto(here.getLatitude(), here.getLongitude(), hereAddress,
                destination.lat(), destination.lng(), destination.name(), way.meters, way.seconds,
                Prefs.paymentMethod(), null, Prefs.courierGender(), Prefs.quietDelivery(),
                Prefs.accessibleVehicle(), Prefs.petFriendly(), null);
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
        Label length = Ui.plain(Ui.distance(fare.distanceMeters()) + Ui.DOT
                + Ui.minutes(fare.durationSeconds()), "WlMuted");
        length.setName("tripLength");
        Container headed = new Container(new BorderLayout());
        headed.add(BorderLayout.CENTER, Ui.label("Choose a delivery", "WlHeading"));
        headed.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(length));
        peek.add(headed);
        FareOptionDto picked = null;
        List<FareOptionDto> options = fare.options();
        for (int iter = 0; options != null && iter < options.size(); iter++) {
            final FareOptionDto option = options.get(iter);
            boolean mine = option.product().equals(product);
            if (mine) {
                picked = option;
            }
            peek.add(option(option, mine));
        }
        long price = picked == null ? fare.amountCents() : picked.total();
        Label total = Ui.plain(Ui.money(price, fare.currency()), "WlRowValue");
        total.setName("fare");
        final PaymentMethodDto method = method();
        Container pay = Ui.row(method == null || "cash".equals(method.kind())
                ? FontImage.MATERIAL_PAYMENTS : FontImage.MATERIAL_CREDIT_CARD,
                WalletForm.label(method), asked(), total);
        ((Label) ((Container) pay.getComponentAt(1)).getComponentAt(0)).setName("payWith");
        peek.add(Ui.tap(pay, "payment", e -> WalletForm.pick(form,
                method == null ? Prefs.PAYMENT_CASH : method.id(), chosen -> {
                    paying = chosen;
                    offer();
                })));
        final Button go = Ui.primary(picked == null ? Lang.tr("Request delivery")
                : Lang.tr("Request {0}", Lang.tr(picked.name())), "request", null);
        go.addActionListener(e -> {
            go.setEnabled(false);
            PaymentMethodDto with = method();
            // The trip that was quoted, as the kind of car and the payment
            // chosen on this sheet.
            DeliveryRequestDto q = request;
            DeliveryRequestDto asking = new DeliveryRequestDto(q.pickupLat(), q.pickupLng(),
                    q.pickupAddress(), q.dropoffLat(), q.dropoffLng(), q.dropoffAddress(),
                    q.distanceMeters(), q.durationSeconds(),
                    with == null ? Prefs.PAYMENT_CASH : with.id(), product, q.courierGender(),
                    q.quietDelivery(), q.accessibleVehicle(), q.petFriendly(), q.note());
            request = asking;
            final boolean cash = with == null || "cash".equals(with.kind());
            Api.customer().request(asking, Net.to(asked -> {
                Telemetry.deliveryRequested(product, cash);
                update(asked);
            }, (status, message) -> {
                go.setEnabled(true);
                Ui.fail(status, message);
            }));
        });
        peek.add(go);
        peek.add(Ui.link("Cancel", "back", e -> idle()));
        sheet.ready(false);
        stage.frame(here, new LatLng(destination.lat(), destination.lng()));
    }

    /// One kind of car: what it is, how long until one comes, what it costs.
    private Container option(final FareOptionDto option, boolean picked) {
        Container row = new Container(new BorderLayout());
        row.setUIID(picked ? "WlOptionPicked" : "WlOption");
        Label icon = new Label("", "WlOptionIcon");
        Ui.icon(icon, "xl".equals(option.product())
                ? FontImage.MATERIAL_AIRPORT_SHUTTLE : "comfort".equals(option.product())
                ? FontImage.MATERIAL_LOCAL_TAXI : FontImage.MATERIAL_DIRECTIONS_CAR, 4.4f);
        row.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(icon));
        Container lines = new Container(BoxLayout.y());
        // How many it seats sits beside the name as a figure, which leaves
        // the line under it for how far off a car is, whole in any language.
        Label seats = Ui.plain(String.valueOf(option.seats()), "WlRowDetail");
        Ui.icon(seats, FontImage.MATERIAL_PERSON, 2.5f);
        Container named = new Container(new BorderLayout());
        named.add(BorderLayout.WEST, Ui.label(option.name(), "WlRowTitle"));
        named.add(BorderLayout.CENTER, seats);
        lines.add(named);
        lines.add(Ui.plain(option.couriersNearby() == 0 ? Lang.tr("None nearby")
                : Lang.tr("{0} away", Ui.minutes(option.etaMinutes() * 60d)), "WlRowDetail"));
        row.add(BorderLayout.CENTER, lines);
        Label price = Ui.plain(Ui.money(option.total(), fare.currency()), "WlRowValue");
        price.setName("price-" + option.product());
        row.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(price));
        return Ui.tap(row, "product-" + option.product(), e -> {
            product = option.product();
            offer();
        });
    }

    /// What the customer's settings ask of every delivery, in a line.
    private static String asked() {
        StringBuilder asked = new StringBuilder();
        if (Prefs.GENDER_WOMEN.equals(Prefs.courierGender())) {
            asked.append(Lang.tr("Women couriers only"));
        }
        if (Prefs.quietDelivery()) {
            asked.append(asked.length() > 0 ? ", " : "").append(Lang.tr("Quiet delivery"));
        }
        if (Prefs.accessibleVehicle()) {
            asked.append(asked.length() > 0 ? ", " : "").append(Lang.tr("Accessible vehicle"));
        }
        if (Prefs.petFriendly()) {
            asked.append(asked.length() > 0 ? ", " : "").append(Lang.tr("Pet friendly"));
        }
        return asked.toString();
    }

    /// A delivery exists: the sheet is its state.
    private void deliveryDisplay() {
        sheet.reset();
        DeliveryState state = delivery.state();
        Label title = Ui.label(Ui.state(state), "WlHeading");
        title.setName("deliveryState");
        Container peek = sheet.peek();
        header(delivery.dropoffAddress());
        if (DeliveryState.REQUESTED == state || DeliveryState.OFFERED == state) {
            stage.chip(null, (char) 0);
            peek.add(title);
            peek.add(Ui.label(Lang.tr("To {0}", delivery.dropoffAddress()), "WlMuted"));
            peek.add(FlowLayout.encloseCenter(new Pulse()));
            peek.add(cancelButton());
        } else if (state.isActive()) {
            peek.add(title);
            peek.add(courierCard());
            peek.add(actions(DeliveryState.IN_PROGRESS != state));
            Container more = sheet.more();
            more.add(Ui.row(FontImage.MATERIAL_TRIP_ORIGIN, delivery.pickupAddress(), null, null));
            more.add(Ui.row(FontImage.MATERIAL_PLACE, delivery.dropoffAddress(), null,
                    Ui.label(Ui.money(delivery.fareCents(), delivery.currency()), "WlRowValue")));
            more.add(Ui.row(FontImage.MATERIAL_PAYMENTS, "Payment", paid(), null));
        } else if (DeliveryState.COMPLETED == state) {
            stage.chip(null, (char) 0);
            receipt();
        } else {
            stage.chip(null, (char) 0);
            peek.add(title);
            peek.add(Ui.text(DeliveryState.NO_COURIERS == state
                    ? "Nobody could take the delivery just now. You have not been charged."
                    : DeliveryState.CANCELLED_BY_COURIER == state
                            ? "Your courier had to cancel. You have not been charged."
                            : "The delivery was cancelled.", "WlText"));
            peek.add(Ui.primary("OK", "dismiss", e -> idle()));
        }
        sheet.ready(false);
    }

    /// Who is coming, and in what.
    private Container courierCard() {
        Container card = new Container(new BorderLayout());
        card.setUIID("WlCourierCard");
        String initials = delivery.courierPhotoInitials();
        Label face = Ui.avatar(delivery.courierName());
        if (initials != null && initials.length() > 0) {
            face.setText(initials);
        }
        Container lines = new Container(BoxLayout.y());
        Label name = Ui.plain(delivery.courierName(), "WlRowTitle");
        Ui.shrink(name, 2.8f);
        lines.add(name);
        // The rating leads the line about the car, with its star.
        Label car = Ui.plain(delivery.courierRating() > 0
                ? Ui.tenths(delivery.courierRating()) + Ui.DOT + delivery.vehicle() : delivery.vehicle(),
                "WlRowDetail");
        if (delivery.courierRating() > 0) {
            Ui.icon(car, FontImage.MATERIAL_STAR, 2.5f);
        }
        Ui.shrink(car, 2.2f);
        lines.add(car);
        if (delivery.plate() != null && delivery.plate().length() > 0) {
            // The plate has a line of its own: beside the name it cut the
            // name short on a narrow phone.
            Label plate = new Label("", "WlPlate");
            // A plate is a plate in any language.
            plate.setShouldLocalize(false);
            plate.setText(delivery.plate());
            lines.add(FlowLayout.encloseIn(plate));
        }
        card.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(face));
        card.add(BorderLayout.CENTER, lines);
        return card;
    }

    /// What can be done about a courier who is on the way.
    private Container actions(boolean cancellable) {
        Container actions = new Container(new GridLayout(1, cancellable ? 4 : 3));
        actions.setUIID("WlActions");
        // The courier's number comes with the delivery while the courier has it,
        // and is gone from it when the delivery is over.
        actions.add(Ui.action(FontImage.MATERIAL_CALL, "Call", "call", e -> call()));
        actions.add(Ui.action(FontImage.MATERIAL_SMS, "Message", "message", e -> message()));
        actions.add(Ui.action(FontImage.MATERIAL_SHARE, "Share", "share", e -> CN.share(
                Lang.tr("I am on my way to {0} with {1} in a {2} ({3}).", delivery.dropoffAddress(),
                        delivery.courierName(), delivery.vehicle(), delivery.plate()), null, null)));
        if (cancellable) {
            final String id = delivery.id();
            final DeliveryState state = delivery.state();
            Button cancel = Ui.action(FontImage.MATERIAL_CLOSE, "Cancel", "cancel",
                    e -> Api.customer().cancel(id, Net.to(cancelled -> {
                        Telemetry.deliveryCancelled("customer", state);
                        idle();
                    }, Ui::fail)));
            cancel.setUIID("WlActionDanger");
            actions.add(cancel);
        }
        return actions;
    }

    private void call() {
        String number = delivery == null ? null : delivery.courierPhone();
        if (number == null || number.length() == 0 || !CN.canDial()) {
            Ui.say("Calling is not available on this device");
            return;
        }
        CN.dial(number);
    }

    private void message() {
        String number = delivery == null ? null : delivery.courierPhone();
        if (number == null || number.length() == 0) {
            Ui.say("Your courier cannot be messaged right now");
            return;
        }
        try {
            CN.sendSMS(number, Lang.tr("Hi, this is your Morsel customer."));
        } catch (IOException failed) {
            Ui.say("Messages are not available on this device");
        }
    }

    private String paid() {
        String label = delivery.paymentMethodLabel();
        return delivery.cash() || label == null || label.length() == 0 ? Lang.tr("Cash") : label;
    }

    private Button cancelButton() {
        final String id = delivery.id();
        final DeliveryState state = delivery.state();
        return Ui.danger("Cancel delivery", "cancel",
                e -> Api.customer().cancel(id, Net.to(cancelled -> {
                    Telemetry.deliveryCancelled("customer", state);
                    idle();
                }, Ui::fail)));
    }

    /// The delivery is over: what it cost, how it was, and a tip if the customer
    /// wants to leave one. The stars are sent as they are tapped; the tip goes
    /// with Done, so that a slip of the thumb is not a charge.
    private void receipt() {
        Container peek = sheet.peek();
        Label title = Ui.label("You have arrived", "WlHeading");
        title.setName("deliveryState");
        Label price = Ui.plain(Ui.money(delivery.total() > 0 ? delivery.total() : delivery.fareCents(),
                delivery.currency()), "WlPrice");
        price.setName("fare");
        Container top = new Container(new BorderLayout());
        top.add(BorderLayout.CENTER, title);
        top.add(BorderLayout.EAST, price);
        peek.add(top);
        Label how = Ui.plain(Ui.distance(delivery.distanceMeters()) + Ui.DOT
                + Ui.minutes(delivery.durationSeconds()) + Ui.DOT + paid(), "WlMuted");
        how.setName("paidWith");
        peek.add(how);
        if ("FAILED".equals(delivery.paymentStatus())) {
            peek.add(Ui.text("The card was declined. Choose another way to pay in your "
                    + "wallet.", "WlNotice"));
        }
        SpanLabel asked = Ui.text("", "WlText");
        asked.setShouldLocalize(false);
        asked.setText(Lang.tr("How was the delivery with {0}?", delivery.courierName()));
        peek.add(asked);
        final Container stars = new Container(new FlowLayout(CN.CENTER));
        final String id = delivery.id();
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
                RatingDto rating = new RatingDto(given);
                Api.customer().rate(id, rating, Net.to(rated -> Telemetry.rated(given),
                        Ui::fail));
            });
            stars.add(button);
        }
        peek.add(stars);
        final int[] percent = {Prefs.tipPercent()};
        boolean tippable = "PAID".equals(delivery.paymentStatus()) && delivery.tip() == 0;
        if (tippable) {
            peek.add(Ui.label("Tip", "WlLabel"));
            peek.add(Ui.choice("tip", new String[] {"None", "10%", "15%", "20%"},
                    new String[] {"0", "10", "15", "20"}, String.valueOf(percent[0]),
                    value -> percent[0] = Integer.parseInt(value)));
        }
        final long fareCents = delivery.fareCents();
        final boolean tipping = tippable;
        peek.add(Ui.primary("Done", "done", e -> {
            if (tipping && percent[0] > 0) {
                TipDto tip = new TipDto(Math.max(100L, Math.round(fareCents * percent[0] / 100d)));
                final int share = percent[0];
                Api.payments().tip(id, tip, Net.to(tipped -> {
                    Telemetry.tipped("delivery", share);
                    Ui.say("Thank you. Your tip was sent.");
                }, Ui::fail));
            }
            idle();
        }));
        peek.add(Ui.link("View receipt", "receipt", e -> ReceiptForm.open(form, id)));
        Container more = sheet.more();
        more.add(Ui.row(FontImage.MATERIAL_TRIP_ORIGIN, delivery.pickupAddress(), null, null));
        more.add(Ui.row(FontImage.MATERIAL_PLACE, delivery.dropoffAddress(), null, null));
        more.add(Ui.row(FontImage.MATERIAL_PERSON, delivery.courierName(), delivery.vehicle(), null));
    }

    private static String firstName(String name) {
        String text = name == null ? "" : name.trim();
        int space = text.indexOf(' ');
        return space < 0 ? text : text.substring(0, space);
    }
}
