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
package com.codenameone.developerguide.snippets;

import com.codename1.components.Carousel;
import com.codename1.components.CoachMarks;
import com.codename1.components.EmptyState;
import com.codename1.components.MultiButton;
import com.codename1.components.PageIndicator;
import com.codename1.components.ScaleImageLabel;
import com.codename1.components.Skeleton;
import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.Tabs;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import java.util.List;

/** Compiled source snippets for the loading, carousel and onboarding sections of the components chapter. */
public class LoadingAndOnboardingSnippets {

    interface Callback<T> {
        void done(T value);
    }

    void fetchOrders(Callback<List<String>> callback) {
    }

    void loadProfile(Callback<String> callback) {
    }

    public void skeletonList(Form form) {
        // tag::skeleton-list[]
        final Container list = new Container(BoxLayout.y());
        list.add(Skeleton.rows(5));
        form.add(BorderLayout.CENTER, list);
        fetchOrders(orders -> {
            list.removeAll();
            if (orders.isEmpty()) {
                list.add(new EmptyState(FontImage.MATERIAL_INBOX, "No orders yet",
                        "Orders you place show up here."));
            }
            for (String order : orders) {
                list.add(new MultiButton(order));
            }
            list.animateLayout(200);
        });
        // end::skeleton-list[]
    }

    public void skeletonShapes(Container profile) {
        // tag::skeleton-shapes[]
        Container header = BorderLayout.centerEastWest(Skeleton.text(2), null, Skeleton.circle(10));
        profile.add(header);
        profile.add(Skeleton.rect(0, 30));      // an image across the full width
        profile.add(Skeleton.card());           // image, title and two lines
        profile.add(Skeleton.rows(3).shimmer(false).uiid("MyPlaceholder"));
        // end::skeleton-shapes[]
    }

    public void skeletonMask(final SpanLabel bio) {
        // tag::skeleton-mask[]
        Skeleton.show(bio, Skeleton.text(3));
        loadProfile(text -> {
            bio.setText(text);
            Skeleton.hide(bio, CommonTransitions.createFade(200));
        });
        // end::skeleton-mask[]
    }

    public void emptyStateRetry(Container list) {
        // tag::empty-state-retry[]
        list.add(new EmptyState(FontImage.MATERIAL_CLOUD_OFF, "Can't load orders",
                "Check your connection and try again.")
                .setAction("Retry", e -> reload()));
        // end::empty-state-retry[]
    }

    void reload() {
    }

    public void carousel(Form form, Image banner1, Image banner2, Image banner3) {
        // tag::carousel[]
        Carousel carousel = new Carousel();
        carousel.addPage(new ScaleImageLabel(banner1));
        carousel.addPage(new ScaleImageLabel(banner2));
        carousel.addPage(new ScaleImageLabel(banner3));
        carousel.setAutoPlay(4000);
        carousel.addSelectionListener((oldPage, newPage) -> track(newPage));
        form.add(BorderLayout.NORTH, carousel);
        // end::carousel[]
    }

    void track(int page) {
    }

    public void pageIndicator(Form form, Component page1, Component page2, Component page3) {
        // tag::page-indicator[]
        Tabs pages = new Tabs();
        pages.hideTabs();
        pages.addTab("", page1);
        pages.addTab("", page2);
        pages.addTab("", page3);
        PageIndicator dots = new PageIndicator();
        dots.bind(pages);
        form.add(BorderLayout.CENTER, pages).add(BorderLayout.SOUTH, dots);
        // end::page-indicator[]
    }

    public void coachMarks(Form form, Button search, Label filter, Button add) {
        // tag::coach-marks[]
        new CoachMarks(form)
                .add(search, "Search", "Find any order by number or name.")
                .add(filter, "Filters", "Narrow the list to what is still open.")
                .add(add, "New order", "Start an order from here.")
                .setOnFinish(() -> form.getContentPane().requestFocus())
                .showOnce("orders-tour");
        // end::coach-marks[]
    }

    public void coachMarksReset() {
        // tag::coach-marks-reset[]
        CoachMarks.resetShowOnce("orders-tour");
        // end::coach-marks-reset[]
    }
}
