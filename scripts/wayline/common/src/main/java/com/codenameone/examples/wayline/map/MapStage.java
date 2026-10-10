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
package com.codenameone.examples.wayline.map;

import com.codename1.maps.LatLng;
import com.codename1.maps.MapView;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codenameone.examples.wayline.ui.BottomSheet;
import com.codenameone.examples.wayline.ui.Ui;

/// A map that fills the screen, and what floats over it.
///
/// From the top: a shade under the status bar, so the clock stays readable
/// over any map; a bar with a round button at its start and whatever the
/// screen puts beside it -- a search card, a status; a chip for one short
/// fact. At the bottom: round buttons at the end of the screen, and under
/// them the [BottomSheet].
///
/// Under the chip, when there is something to say about where the device is:
/// that its position was refused, cannot be had, or is slow to come. It is a
/// button, and pressing it asks again; the map and everything else stay as
/// usable as they were, standing on the demo city in the meantime. See
/// [Locator].
///
/// The map is one layer and all of that is another. The parts of the upper
/// layer with nothing in them take no touches, so the map under them is
/// dragged and pinched as if nothing were there.
public final class MapStage {
    public final MapView map;
    private final Container layers;
    private final Container north = new Container(BoxLayout.y());
    private final Container south = new Container(BoxLayout.y());
    private final Container header = new Container(new BorderLayout());
    private final Container chips = new Container(new FlowLayout(Component.CENTER));
    private final Container side = new Container(new FlowLayout(Component.RIGHT));
    private final Label chip = Ui.chip("", (char) 0);
    private final Button notice = new Button("", "WlChip");
    private final Container notices = new Container(new FlowLayout(Component.CENTER));
    private final Locator.Watcher watcher = new Locator.Watcher() {
        @Override
        public void located(int state, LatLng where) {
            locationChanged(state, where);
        }
    };
    private Located located;
    private boolean everLocated;
    /// Whether the search under way was asked for with the notice, and so
    /// deserves to be seen to be under way.
    private boolean asked;
    private final BottomSheet sheet = new BottomSheet();
    /// Counts the times the camera was sent somewhere, so that a framing put
    /// off until after a layout can tell it has been overtaken.
    private int framed;

    /// Told where the device is, when that becomes known and when it changes.
    public interface Located {
        /// @param first whether this is the first position this map has had:
        ///     the moment to move the camera, which later ones must not, since
        ///     by then the user may be looking at somewhere else
        void at(LatLng where, boolean first);
    }

    /// @param lead the round button at the start of the bar: the menu on a
    ///     home screen, the way back on any other
    public MapStage(LatLng center, int zoom, Button lead) {
        map = Maps.create(center, zoom);

        Container shade = new Container(BoxLayout.y());
        shade.setUIID("WlStatusShade");
        // All of its height is the inset the device asks to be kept clear, so
        // it is exactly as tall as the status bar and absent where there is
        // none to cover.
        shade.setSafeArea(true);
        Container bar = new Container(new BorderLayout());
        bar.setUIID("WlMapBar");
        bar.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(lead));
        bar.add(BorderLayout.CENTER, header);
        chip.setName("chip");
        chip.setHidden(true);
        chips.add(chip);
        notice.setName("locationNotice");
        FontImage.setMaterialIcon(notice, FontImage.MATERIAL_LOCATION_OFF, 3.2f);
        notice.addActionListener(e -> {
            asked = true;
            Locator.retry();
        });
        notices.setHidden(true);
        notices.add(notice);
        north.add(shade).add(bar).add(chips).add(notices);
        south.add(side).add(sheet);

        Container floating = new Container(new BorderLayout());
        floating.add(BorderLayout.NORTH, north);
        floating.add(BorderLayout.SOUTH, south);
        layers = new Container(new LayeredLayout()) {
            @Override
            protected void initComponent() {
                super.initComponent();
                // After this screen is up: an answer that is already known is
                // told at once, and telling it lays the screen out.
                CN.callSerially(new Runnable() {
                    @Override
                    public void run() {
                        if (isInitialized()) {
                            Locator.watch(watcher);
                        }
                    }
                });
            }

            @Override
            protected void deinitialize() {
                Locator.unwatch(watcher);
                super.deinitialize();
            }
        };
        layers.add(map).add(floating);
    }

    /// Has `listener` told where the device is. Until it is, the screen has
    /// [Locator#here], which is the demo city when nothing better is known.
    public void whenLocated(Located listener) {
        located = listener;
    }

    private void locationChanged(int state, LatLng where) {
        String text = null;
        if (state == Locator.FOUND) {
            boolean first = !everLocated;
            everLocated = true;
            if (located != null) {
                located.at(where, first);
            }
        } else if (state == Locator.DENIED) {
            text = "Location is turned off. Try again";
        } else if (state == Locator.UNAVAILABLE) {
            text = "Can't find your location. Try again";
        } else if (state == Locator.SLOW) {
            text = "Still looking for your location. Try again";
        } else if (state == Locator.SEARCHING && asked) {
            text = "Looking for your location...";
        }
        if (state != Locator.SEARCHING) {
            asked = false;
        }
        boolean none = text == null;
        if (!none) {
            notice.setText(text);
        }
        if (notices.isHidden() != none || !none) {
            notices.setHidden(none);
            north.revalidate();
        }
    }

    /// The map with everything over it, to add to a screen.
    public Container layers() {
        return layers;
    }

    public BottomSheet sheet() {
        return sheet;
    }

    /// Puts `content` beside the round button at the top, in place of what
    /// was there; null leaves the place empty.
    public void header(Component content) {
        header.removeAll();
        if (content != null) {
            header.add(BorderLayout.CENTER, content);
        }
        north.revalidate();
    }

    /// Adds a round button above the sheet: recentre, and the like.
    public Button addRound(char icon, String name, final Runnable onClick) {
        Button button = Ui.round(icon, name, e -> onClick.run());
        side.add(button);
        return button;
    }

    /// Shows `text` in the chip under the bar; null or empty takes the chip
    /// away.
    public void chip(String text, char icon) {
        boolean none = text == null || text.length() == 0;
        if (!none) {
            chip.setText(text);
            if (icon != 0) {
                FontImage.setMaterialIcon(chip, icon, 3.2f);
            }
        }
        if (chip.isHidden() != none || !none) {
            chip.setHidden(none);
            north.revalidate();
        }
    }

    /// Moves the camera so that `points` are all in the part of the map
    /// nothing covers.
    ///
    /// Twice, the second time after the screen has been laid out again: this
    /// is asked for while the sheet is being filled, when the sheet is still
    /// as tall as what it showed before.
    public void frame(final LatLng... points) {
        final int asked = ++framed;
        fit(points);
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                // Not if the camera has been sent somewhere else since.
                if (asked == framed) {
                    layers.revalidate();
                    fit(points);
                }
            }
        });
    }

    /// Puts `center` in the middle of the map, in place of whatever was
    /// being framed.
    public void center(LatLng center, int zoom) {
        framed++;
        map.moveCamera(center, zoom);
    }

    private void fit(LatLng... points) {
        // The chip counts whether it is showing or not: it comes and goes
        // with the driver's car, and the pins must not end up under it.
        int top = north.getHeight() + (chip.isHidden() ? CN.convertToPixels(9f) : 0);
        int bottom = Math.max(south.getHeight(), south.getPreferredH());
        if (map.getHeight() > 0 && bottom == 0) {
            // Asked before the first layout: the sheet will take about this.
            bottom = map.getHeight() * 2 / 5;
        }
        // A little over, so a pin at the edge is not half under the sheet.
        int margin = CN.convertToPixels(4f);
        Maps.frame(map, top + margin, bottom + margin, points);
    }
}
