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
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.wayline.Prefs;
import com.codenameone.examples.wayline.api.PlaceDto;
import com.codenameone.examples.wayline.net.Api;
import com.codenameone.examples.wayline.net.Net;
import com.codenameone.examples.wayline.ui.Ui;

import java.util.List;

/// Finds a place by name.
///
/// The search goes to the app's own server, which asks a geocoder on the app's
/// behalf. So the app holds no geocoder key, the server can swap one provider
/// for another without a release, and the provider sees the server and never a
/// user's device.
public final class PlaceSearchForm {
    public interface Picked {
        void picked(PlaceDto place);
    }

    private static final int PAUSE_MILLIS = 350;

    private final Form form = Ui.form("Where to?", "PlaceSearch");
    private final Container results = new Container(BoxLayout.y());
    private final TextField query;
    private final LatLng near;
    private final Form previous;
    private final Picked picked;
    private int typed;

    private PlaceSearchForm(Form previous, LatLng near, Picked picked) {
        this.previous = previous;
        this.near = near;
        this.picked = picked;
        Ui.back(form, previous);
        Container top = Ui.page();
        top.setScrollableY(false);
        query = Ui.field(top, "Destination", "query", "An address or a place");
        // Searches when the typing pauses, not at every letter.
        query.addDataChangedListener((type, index) -> {
            final int mine = ++typed;
            UITimer.timer(PAUSE_MILLIS, false, form, () -> {
                if (mine == typed) {
                    search();
                }
            });
        });
        query.setDoneListener(e -> search());
        results.setName("results");
        results.setScrollableY(true);
        form.add(BorderLayout.NORTH, top);
        form.add(BorderLayout.CENTER, results);
        suggest();
    }

    /// Before anything is typed: the places already known, which are most of
    /// where anybody goes.
    private void suggest() {
        results.removeAll();
        int count = 0;
        count = known(FontImage.MATERIAL_HOME, Prefs.home(), count);
        count = known(FontImage.MATERIAL_WORK, Prefs.work(), count);
        List<PlaceDto> recents = Prefs.recents();
        for (int iter = 0; iter < recents.size(); iter++) {
            count = known(FontImage.MATERIAL_SCHEDULE, recents.get(iter), count);
        }
        Ui.refresh(form);
    }

    private int known(char icon, final PlaceDto place, int count) {
        if (place == null) {
            return count;
        }
        results.add(Ui.tap(Ui.row(icon, place.name, place.address, null), "known-" + count,
                e -> {
                    previous.showBack();
                    picked.picked(place);
                }));
        return count + 1;
    }

    public static void show(Form previous, LatLng near, Picked picked) {
        PlaceSearchForm search = new PlaceSearchForm(previous, near, picked);
        search.form.show();
        search.query.startEditingAsync();
    }

    private void search() {
        final String text = query.getText().trim();
        if (text.length() < 2) {
            suggest();
            return;
        }
        Api.geo().search(text, near.getLatitude(), near.getLongitude(), Net.to(places -> {
                    // An answer to what is no longer in the field is dropped.
                    if (text.equals(query.getText().trim())) {
                        fill(places);
                    }
                }, Ui::fail));
    }

    private void fill(List<PlaceDto> places) {
        results.removeAll();
        if (places == null || places.isEmpty()) {
            Container none = Ui.page();
            none.add(Ui.label("Nothing found", "WlMuted"));
            results.add(none);
        }
        for (int iter = 0; places != null && iter < places.size(); iter++) {
            final PlaceDto place = places.get(iter);
            results.add(Ui.tap(Ui.row(FontImage.MATERIAL_PLACE, place.name, place.address, null),
                    "place-" + iter, e -> {
                        previous.showBack();
                        picked.picked(place);
                    }));
        }
        Ui.refresh(form);
    }
}
