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
package com.codename1.samples;

import com.codename1.car.Car;
import com.codename1.car.CarApplication;
import com.codename1.car.CarContext;
import com.codename1.car.CarGridItem;
import com.codename1.car.CarGridTemplate;
import com.codename1.car.CarListTemplate;
import com.codename1.car.CarNowPlayingTemplate;
import com.codename1.car.CarPaneTemplate;
import com.codename1.car.CarRow;
import com.codename1.car.CarScreen;
import com.codename1.car.CarTemplate;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

/**
 * Demonstrates the portable in-car API ({@code com.codename1.car}) that projects a driver-safe UI
 * onto Apple CarPlay and Google Android Auto. The car experience is registered in {@link #init} via
 * {@link Car#setApplication}; it appears on a connected head unit (CarPlay simulator / Android Auto
 * Desktop Head Unit), never on the phone screen -- the phone {@link Form} here just explains that and
 * reports the connection state.
 *
 * <p>The build automatically wires the native plumbing because this sample references
 * {@code com.codename1.car}; see {@code codenameone_settings.properties} for the per-category build
 * hints. See the "In-Car Experiences" chapter of the developer guide for details.</p>
 */
public class CarSample {
    private Form current;
    private Resources theme;

    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");
        Toolbar.setGlobalToolbar(true);
        // Register the in-car experience. Must happen before a head unit connects, so init() is the
        // right place. Referencing com.codename1.car here is also what makes the build wire CarPlay /
        // Android Auto support into the native projects.
        Car.setApplication(new MusicCarApp());
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Form f = new Form("Car Sample", BoxLayout.y());
        f.add(new Label("This app registers a CarPlay / Android Auto experience."));
        f.add(new Label("Connect a head unit to see it:"));
        f.add(new Label("• iOS Simulator: I/O > External Displays > CarPlay"));
        f.add(new Label("• Android: the Desktop Head Unit (DHU)"));
        f.add(new Label("Car connected: " + CN.isCarConnected()));
        f.show();
    }

    public void stop() {
        current = CN.getCurrentForm();
        if (current instanceof com.codename1.ui.Dialog) {
            ((com.codename1.ui.Dialog) current).dispose();
            current = CN.getCurrentForm();
        }
    }

    public void destroy() {
    }

    // --- the in-car experience ----------------------------------------------

    /** Root of the projected experience: a browse list that drills into grid / pane / now-playing. */
    static class MusicCarApp extends CarApplication {
        public CarScreen onCreateRootScreen(CarContext context) {
            return new LibraryScreen();
        }
    }

    static class LibraryScreen extends CarScreen {
        protected CarTemplate onCreateTemplate() {
            return new CarListTemplate().setTitle("Library")
                .addRow(new CarRow("Now Playing").setText("Tap to view the system player")
                    .setOnAction(ctx -> ctx.pushScreen(new NowPlayingScreen())))
                .addRow(new CarRow("Browse").setText("Categories").setBrowsable(true)
                    .setOnAction(ctx -> ctx.pushScreen(new BrowseScreen())))
                .addRow(new CarRow("About this album").setBrowsable(true)
                    .setOnAction(ctx -> ctx.pushScreen(new AlbumDetailScreen())));
        }
    }

    static class BrowseScreen extends CarScreen {
        protected CarTemplate onCreateTemplate() {
            CarGridTemplate t = new CarGridTemplate().setTitle("Browse");
            String[] categories = {"Charts", "Genres", "Moods", "New", "Radio", "For You"};
            for (final String c : categories) {
                t.addItem(new CarGridItem(c, null)
                    .setOnAction(ctx -> ctx.showToast("Open " + c)));
            }
            return t;
        }
    }

    static class AlbumDetailScreen extends CarScreen {
        protected CarTemplate onCreateTemplate() {
            return new CarPaneTemplate().setTitle("Discovery")
                .addRow(new CarRow("Artist").setText("Daft Punk"))
                .addRow(new CarRow("Year").setText("2001"))
                .addRow(new CarRow("Tracks").setText("14"));
        }
    }

    static class NowPlayingScreen extends CarScreen {
        protected CarTemplate onCreateTemplate() {
            // Track metadata, artwork and transport come from the platform media session; this
            // template routes the head unit to that surface.
            return new CarNowPlayingTemplate().setUpNextVisible(true);
        }
    }
}
