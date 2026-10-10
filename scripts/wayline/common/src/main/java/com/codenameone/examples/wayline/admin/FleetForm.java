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
package com.codenameone.examples.wayline.admin;

import com.codename1.maps.LatLng;
import com.codename1.ui.Command;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codenameone.examples.wayline.api.DriverDto;
import com.codenameone.examples.wayline.live.LiveChannel;
import com.codenameone.examples.wayline.map.CarMarker;
import com.codenameone.examples.wayline.map.Locator;
import com.codenameone.examples.wayline.map.MapStage;
import com.codenameone.examples.wayline.map.Maps;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.ui.Lang;
import com.codenameone.examples.wayline.ui.Ui;
import com.codename1.ui.util.UITimer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Every car online, on one map, moving as the drivers report in.
public class FleetForm extends LiveChannel.Adapter {
    private static final int POLL_MILLIS = 10000;

    private final Form form = Ui.home("Fleet");
    private final MapStage stage;
    private final Label count = Ui.label("", "WlHeading");
    private final Map<String, CarMarker> cars = new HashMap<String, CarMarker>();

    FleetForm(final Form previous) {
        // No title bar to carry the back arrow, so the round button over the
        // map is it -- and the screen's back command too, which is what the
        // device's own back button and gesture run.
        Command back = Command.create("", null, e -> previous.showBack());
        form.setBackCommand(back);
        stage = new MapStage(Locator.here(), Maps.CITY_ZOOM,
                Ui.round(FontImage.MATERIAL_ARROW_BACK, "back", e -> previous.showBack()));
        stage.header(Ui.label("Live map", "WlMapTitle"));
        stage.addRound(FontImage.MATERIAL_ZOOM_OUT_MAP, "recentre", this::frame);
        count.setName("fleetCount");
        stage.sheet().peek().add(count);
        stage.sheet().peek().add(Ui.label("Cars move as their drivers report in.", "WlMuted"));
        stage.sheet().ready(false);
        form.add(stage.layers());
        form.addShowListener(e -> {
            LiveChannel.listen(this);
            load();
        });
        // The channel moves the cars; this catches the one whose driver went
        // quiet without saying so.
        UITimer.timer(POLL_MILLIS, true, form, this::load);
    }

    void show() {
        counted();
        form.show();
    }

    private void load() {
        Api.admin().drivers(Net.to(this::fill, Ui::fail));
    }

    private void fill(List<DriverDto> drivers) {
        Map<String, CarMarker> gone = new HashMap<String, CarMarker>(cars);
        for (int iter = 0; drivers != null && iter < drivers.size(); iter++) {
            DriverDto driver = drivers.get(iter);
            if (driver.online) {
                gone.remove(driver.username);
                place(driver.username, driver.lat, driver.lng, driver.heading);
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
