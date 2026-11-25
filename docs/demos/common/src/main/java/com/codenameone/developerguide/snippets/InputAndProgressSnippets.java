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

import com.codename1.components.ActivityTimeline;
import com.codename1.components.AudioWaveform;
import com.codename1.components.Chip;
import com.codename1.components.ChipInput;
import com.codename1.components.CircularProgress;
import com.codename1.components.ColorPicker;
import com.codename1.components.EmojiPicker;
import com.codename1.components.ExpandableText;
import com.codename1.components.MentionField;
import com.codename1.components.ScaleImageLabel;
import com.codename1.io.Log;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.MasonryLayout;
import com.codename1.components.RangeSlider;

import java.util.List;

/** Compiled source snippets for the input, progress and layout sections of the components chapter. */
public class InputAndProgressSnippets {

    public void rangeSlider(Form form, final Label summary) {
        // tag::range-slider[]
        final RangeSlider price = new RangeSlider(0, 500, 100, 350);
        price.setStep(10);
        price.addActionListener(e -> summary.setText(
                "$" + price.getLowerValue() + " - $" + price.getUpperValue()));
        form.add(price);
        // end::range-slider[]
    }

    public void chips(Form form) {
        // tag::chips[]
        Container filters = new Container(new FlowLayout());
        Chip unread = new Chip("Unread").setSelectable(true);
        unread.addActionListener(e -> Log.p("Unread filter: " + unread.isSelected()));
        Chip travel = new Chip("Travel").setRemovable(true);
        travel.addRemoveListener(e -> filters.removeComponent(travel));
        filters.add(unread).add(travel);
        form.add(filters);
        // end::chips[]
    }

    public void chipInput(Form form) {
        // tag::chip-input[]
        final ChipInput tags = new ChipInput();
        tags.setHint("Add a tag");
        tags.setSeparators(", ");
        tags.addValue("design");
        tags.addActionListener(e -> Log.p("Tags: " + tags.getValues()));
        form.add(tags);
        // end::chip-input[]
    }

    public void colorPicker(Form form, final Label preview) {
        // tag::color-picker[]
        final ColorPicker picker = new ColorPicker(0x3b82f6);
        picker.setPresets(0xef4444, 0xf59e0b, 0x10b981, 0x3b82f6, 0x8b5cf6);
        picker.addActionListener(e -> {
            preview.getAllStyles().setBgColor(picker.getColor());
            preview.repaint();
        });
        form.add(picker);
        // end::color-picker[]
    }

    public void colorPickerDialog() {
        // tag::color-picker-dialog[]
        Integer chosen = ColorPicker.showDialog("Highlight", 0xffcc00);
        if (chosen != null) {
            Log.p("Picked " + ColorPicker.toHex(chosen));
        }
        // end::color-picker-dialog[]
    }

    public void circularProgress(Form form) {
        // tag::circular-progress[]
        CircularProgress upload = new CircularProgress(0);
        upload.setRingSize(16, 2);
        form.add(upload);
        // as the work advances
        upload.setProgress(65);
        // or a label of your own instead of the percentage
        upload.setText("3 of 5");
        // end::circular-progress[]
    }

    public void audioWaveform(Form form, short[] pcm) {
        // tag::audio-waveform[]
        final AudioWaveform wave = new AudioWaveform(AudioWaveform.levelsFromPcm(pcm, 48));
        wave.setSeekable(true);
        wave.addActionListener(e -> Log.p("Seek to " + wave.getProgress()));
        form.add(wave);
        // while playing, unless the user is dragging
        if (!wave.isSeeking()) {
            wave.setProgress(0.25f);
        }
        // end::audio-waveform[]
    }

    public void expandableText(Form form, String review) {
        // tag::expandable-text[]
        ExpandableText text = new ExpandableText(review, 3);
        text.setLinkText("More", "Less");
        form.add(text);
        // end::expandable-text[]
    }

    public void activityTimeline(Form form) {
        // tag::activity-timeline[]
        ActivityTimeline tracking = new ActivityTimeline()
                .addStep("Order placed", "We received your order", "9:02", ActivityTimeline.DONE)
                .addStep("Shipped", "Left the warehouse", "13:40", ActivityTimeline.DONE)
                .addStep("Out for delivery", null, null, ActivityTimeline.CURRENT)
                .addStep("Delivered", null, null, ActivityTimeline.PENDING);
        form.add(tracking);
        // when the status changes
        tracking.setState(2, ActivityTimeline.DONE);
        tracking.setState(3, ActivityTimeline.CURRENT);
        // end::activity-timeline[]
    }

    public void emojiPicker(Form form) {
        // tag::emoji-picker[]
        TextField message = new TextField();
        EmojiPicker emoji = new EmojiPicker();
        emoji.bindTo(message);
        form.add(BorderLayout.NORTH, message);
        form.add(BorderLayout.CENTER, emoji);
        // end::emoji-picker[]
    }

    public void mentionField(Form form) {
        // tag::mention-field[]
        final MentionField comment = new MentionField();
        comment.setCandidates("Ada Lovelace", "Alan Turing", "Grace Hopper");
        comment.getTextField().setHint("Write a comment");
        comment.addActionListener(e -> {
            List<String> people = comment.getMentions();
            Log.p("Will notify " + people);
        });
        form.add(comment);
        // end::mention-field[]
    }

    public void masonry(Form form, List<Image> photos) {
        // tag::masonry[]
        Container feed = new Container(new MasonryLayout(2));
        feed.setScrollableY(true);
        for (Image photo : photos) {
            feed.add(new ScaleImageLabel(photo));
        }
        form.add(BorderLayout.CENTER, feed);
        // end::masonry[]
    }

    public void masonryAdaptive() {
        // tag::masonry-adaptive[]
        MasonryLayout layout = new MasonryLayout(2);
        layout.setMinimumColumnWidth(40);
        layout.setGap(2);
        Container feed = new Container(layout);
        // end::masonry-adaptive[]
        feed.setScrollableY(true);
    }
}
