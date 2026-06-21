/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.appreview;

import com.codename1.components.SpanLabel;
import com.codename1.messaging.Message;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextArea;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

/// The Codename One drawn rating widget shown by [AppReview] when no native
/// review prompt is available. It presents a row of stars; a high rating routes
/// the user to the store while a low rating opens a private feedback step.
///
/// This class is intentionally package private -- apps interact with it
/// exclusively through [AppReview].
class RatingDialog {
    private static final int MAX_STARS = 5;

    private RatingDialog() {
    }

    static void show(final AppReview config) {
        int rating = showStars(config);
        if (rating <= 0) {
            // Dismissed without rating. We leave the "completed" flag alone so
            // the scheduler may try again after daysBetweenPrompts, unless the
            // user explicitly opted out (handled inside showStars).
            return;
        }
        if (rating >= config.getHighRatingThreshold()) {
            config.markCompleted();
            openStore(config);
        } else {
            collectFeedback(config, rating);
        }
    }

    private static int showStars(final AppReview config) {
        String appName = CN.getProperty("AppName", "this app");
        final Dialog dialog = new Dialog("Enjoying " + appName + "?");
        dialog.setLayout(BoxLayout.y());

        SpanLabel prompt = new SpanLabel("Tap a star to rate your experience.");
        prompt.setUIID("DialogBody");
        dialog.add(prompt);

        final int[] result = new int[]{0};
        Container stars = new Container(new FlowLayout(Component.CENTER));
        for (int i = 1; i <= MAX_STARS; i++) {
            final int value = i;
            Button star = new Button();
            FontImage.setMaterialIcon(star, FontImage.MATERIAL_STAR_BORDER, 5);
            star.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    result[0] = value;
                    dialog.dispose();
                }
            });
            stars.add(star);
        }
        dialog.add(stars);

        Button notNow = new Button("Not now");
        notNow.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                result[0] = 0;
                dialog.dispose();
            }
        });
        Button never = new Button("Don't ask again");
        never.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                result[0] = 0;
                config.markCompleted();
                dialog.dispose();
            }
        });
        Container actions = new Container(new FlowLayout(Component.CENTER));
        actions.add(notNow);
        actions.add(never);
        dialog.add(actions);

        dialog.show();
        return result[0];
    }

    private static void openStore(AppReview config) {
        // Even within the fallback widget the native prompt may have become
        // available (e.g. a positive rating on an older flow); prefer it.
        if (CN.isNativeInAppReviewSupported()) {
            CN.requestNativeInAppReview(null);
            return;
        }
        String storeUrl = config.getStoreUrl();
        if (storeUrl != null && storeUrl.length() > 0) {
            CN.execute(storeUrl);
        }
    }

    private static void collectFeedback(final AppReview config, final int rating) {
        FeedbackListener listener = config.getFeedbackListener();
        if (listener != null && listener.lowRating(rating)) {
            // The listener is presenting its own feedback experience.
            config.markCompleted();
            return;
        }

        String supportEmail = config.getSupportEmail();
        if ((listener == null && (supportEmail == null || supportEmail.length() == 0))) {
            // Nowhere to route the feedback -- nothing more to do.
            config.markCompleted();
            return;
        }

        final Dialog dialog = new Dialog("Help us improve");
        dialog.setLayout(BoxLayout.y());
        SpanLabel prompt = new SpanLabel("Sorry to hear it wasn't great. What can we do better?");
        prompt.setUIID("DialogBody");
        dialog.add(prompt);

        final TextArea feedback = new TextArea("", 4, 20);
        feedback.setHint("Your feedback");
        dialog.add(feedback);

        Button send = new Button("Send");
        send.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                String text = feedback.getText();
                if (text == null) {
                    text = "";
                }
                config.markCompleted();
                dialog.dispose();
                deliverFeedback(config, rating, text);
            }
        });
        Button cancel = new Button("Cancel");
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                dialog.dispose();
            }
        });
        Container actions = new Container(new FlowLayout(Component.CENTER));
        actions.add(cancel);
        actions.add(send);
        dialog.add(actions);

        dialog.show();
    }

    private static void deliverFeedback(AppReview config, int rating, String text) {
        FeedbackListener listener = config.getFeedbackListener();
        if (listener != null) {
            listener.feedback(rating, text);
            return;
        }
        String supportEmail = config.getSupportEmail();
        if (supportEmail != null && supportEmail.length() > 0) {
            String appName = CN.getProperty("AppName", "the app");
            Message msg = new Message(text);
            Message.sendMessage(new String[]{supportEmail}, "Feedback for " + appName, msg);
        }
    }
}
