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
package com.codenameone.examples.savor.admin;

import com.codename1.maps.LatLng;
import com.codename1.ui.Command;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codenameone.examples.savor.api.CourierDto;
import com.codenameone.examples.savor.live.LiveChannel;
import com.codenameone.examples.savor.map.CarMarker;
import com.codenameone.examples.savor.map.Locator;
import com.codenameone.examples.savor.map.MapStage;
import com.codenameone.examples.savor.map.Maps;
import com.codenameone.examples.savor.net.Api;
import com.codenameone.examples.savor.net.Net;
import com.codenameone.examples.savor.ui.Lang;
import com.codenameone.examples.savor.ui.Ui;
import com.codename1.ui.util.UITimer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Every car online, on one map, moving as the couriers report in.
public class FleetForm extends LiveChannel.Adapter {
    private static final int POLL_MILLIS = 10000;

    private final Form form = Ui.home("Fleet");
    private final MapStage stage;
    private final Label count = Ui.label("", "SvHeading");
    private final Map<String, CarMarker> cars = new HashMap<String, CarMarker>();

    FleetForm(final Form previous) {
        // No title bar to carry the back arrow, so the round button over the
        // map is it -- and the screen's back command too, which is what the
        // device's own back button and gesture run.
        Command back = Command.create("", null, e -> previous.showBack());
        form.setBackCommand(back);
        stage = new MapStage(Locator.here(), Maps.CITY_ZOOM,
                Ui.round(FontImage.MATERIAL_ARROW_BACK, "back", e -> previous.showBack()));
        stage.header(Ui.label("Live map", "SvMapTitle"));
        stage.addRound(FontImage.MATERIAL_ZOOM_OUT_MAP, "recentre", this::frame);
        count.setName("fleetCount");
        stage.sheet().peek().add(count);
        stage.sheet().peek().add(Ui.label("Cars move as their couriers report in.", "SvMuted"));
        stage.sheet().ready(false);
        form.add(stage.layers());
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            load();
        });
        // The channel moves the cars; this catches the one whose courier went
        // quiet without saying so.
        UITimer.timer(POLL_MILLIS, true, form, this::load);
    }

    void show() {
        counted();
        form.show();
    }

    private void load() {
        Api.admin().couriers(Net.to(this::fill, Ui::fail));
    }

    private void fill(List<CourierDto> couriers) {
        Map<String, CarMarker> gone = new HashMap<String, CarMarker>(cars);
        for (int iter = 0; couriers != null && iter < couriers.size(); iter++) {
            CourierDto courier = couriers.get(iter);
            if (courier.online()) {
                gone.remove(courier.username());
                place(courier.username(), courier.lat(), courier.lng(), courier.heading());
            }
        }
        for (Map.Entry<String, CarMarker> left : gone.entrySet()) {
            left.getValue().remove();
            cars.remove(left.getKey());
        }
        counted();
    }

    @Override
    public void fleetChanged(String username, boolean online, double lat, double lng,
            double heading) {
        if (online) {
            place(username, lat, lng, heading);
        } else {
            CarMarker car = cars.remove(username);
            if (car != null) {
                car.remove();
            }
        }
        counted();
    }

    private void place(String username, double lat, double lng, double heading) {
        LatLng at = new LatLng(lat, lng);
        CarMarker car = cars.get(username);
        if (car == null) {
            cars.put(username, new CarMarker(stage.map, form, at, heading));
        } else {
            car.moveTo(at, heading);
        }
    }

    /// Moves the camera to take in every car there is.
    private void frame() {
        List<LatLng> all = new ArrayList<LatLng>();
        for (CarMarker car : cars.values()) {
            all.add(car.position());
        }
        if (all.isEmpty()) {
            stage.map.moveCamera(Locator.here(), Maps.CITY_ZOOM);
        } else {
            stage.frame(all.toArray(new LatLng[all.size()]));
        }
    }

    private void counted() {
        int size = cars.size();
        count.setText(size == 0 ? Lang.tr("No cars online") : size == 1
                ? Lang.tr("1 car online") : Lang.tr("{0} cars online", String.valueOf(size)));
        stage.map.repaint();
        Ui.refresh(form);
    }
}
