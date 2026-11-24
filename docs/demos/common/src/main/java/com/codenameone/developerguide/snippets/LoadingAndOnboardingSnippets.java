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
import com.codename1.components.Skeleton;
import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Tabs;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.util.UITimer;

/** Compiled source snippets for the loading, carousel and onboarding sections of the components chapter. */
public class LoadingAndOnboardingSnippets {

    public void skeletonList() {
        // tag::skeleton-list[]
        Form form = new Form("Orders", new BorderLayout());
        Container list = new Container(BoxLayout.y());
        list.add(Skeleton.rows(5));
        form.add(BorderLayout.CENTER, list);
        form.show();
        // Stands in for a network call that answers after 2 seconds.
        UITimer.timer(2000, false, form, () -> {
            String[] orders = {"Order #101", "Order #102", "Order #103"};
            list.removeAll();
            if (orders.length == 0) {
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

    public void skeletonShapes() {
        // tag::skeleton-shapes[]
        Form form = new Form("Loading", BoxLayout.y());
        Container header = BorderLayout.centerEastWest(Skeleton.text(2), null, Skeleton.circle(10));
        form.add(header);
        form.add(Skeleton.rect(0, 30));      // an image across the full width
        form.add(Skeleton.card());           // image, title and two lines
        form.add(Skeleton.rows(3).shimmer(false).uiid("MyPlaceholder"));
        form.show();
        // end::skeleton-shapes[]
    }

    public void skeletonMask() {
        // tag::skeleton-mask[]
        Form form = new Form("Profile", BoxLayout.y());
        SpanLabel bio = new SpanLabel();
        form.add(new Label("About")).add(bio);
        form.show();
        Skeleton.show(bio, Skeleton.text(3));
        // Stands in for loading the profile.
        UITimer.timer(2000, false, form, () -> {
            bio.setText("Builds mobile apps in Java and Kotlin, and drinks too much coffee.");
            Skeleton.hide(bio, CommonTransitions.createFade(200));
        });
        // end::skeleton-mask[]
    }

    public void emptyStateRetry() {
        // tag::empty-state-retry[]
        Form form = new Form("Orders", new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER));
        form.add(BorderLayout.CENTER, new EmptyState(FontImage.MATERIAL_CLOUD_OFF, "Can't load orders",
                "Check your connection and try again.")
                .setAction("Retry", e -> System.out.println("Retry tapped")));
        form.show();
        // end::empty-state-retry[]
    }

    public void carousel() {
        // tag::carousel[]
        Form form = new Form("Carousel", new BorderLayout());
        Carousel carousel = new Carousel();
        int[] colors = {0x3b82f6, 0x10b981, 0xf59e0b};
        for (int i = 0; i < colors.length; i++) {
            Label page = new Label("Page " + (i + 1));
            page.getAllStyles().setBgColor(colors[i]);
            page.getAllStyles().setBgTransparency(255);
            page.getAllStyles().setFgColor(0xffffff);
            page.getAllStyles().setAlignment(CN.CENTER);
            page.setPreferredH(CN.convertToPixels(30));
            carousel.addPage(page);
        }
        carousel.setAutoPlay(4000);
        carousel.addSelectionListener((oldPage, newPage) ->
                System.out.println("Showing page " + (newPage + 1)));
        form.add(BorderLayout.NORTH, carousel);
        form.show();
        // end::carousel[]
    }

    public void pageIndicator() {
        // tag::page-indicator[]
        Form form = new Form("Pages", new BorderLayout());
        Tabs pages = new Tabs();
        pages.hideTabs();
        pages.addTab("", new Label("First page"));
        pages.addTab("", new Label("Second page"));
        pages.addTab("", new Label("Third page"));
        PageIndicator dots = new PageIndicator();
        dots.bind(pages);
        form.add(BorderLayout.CENTER, pages).add(BorderLayout.SOUTH, dots);
        form.show();
        // end::page-indicator[]
    }

    public void coachMarks() {
        // tag::coach-marks[]
        Form form = new Form("Orders", BoxLayout.y());
        Button search = new Button("Search");
        Button filter = new Button("Filter");
        Button add = new Button("New order");
        form.add(FlowLayout.encloseIn(search, filter, add));
        form.show();
        new CoachMarks(form)
                .add(search, "Search", "Find any order by number or name.")
                .add(filter, "Filters", "Narrow the list to what is still open.")
                .add(add, "New order", "Start an order from here.")
                .setOnFinish(() -> form.getContentPane().requestFocus())
                .show();
        // end::coach-marks[]
    }

    public void coachMarksReset() {
        // tag::coach-marks-reset[]
        CoachMarks.resetShowOnce("orders-tour");
        // end::coach-marks-reset[]
    }
}
