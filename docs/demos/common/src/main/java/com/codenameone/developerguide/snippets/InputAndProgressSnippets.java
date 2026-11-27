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
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.MasonryLayout;
import com.codename1.components.RangeSlider;

/** Compiled source snippets for the input, progress and layout sections of the components chapter. */
public class InputAndProgressSnippets {

    public void rangeSlider() {
        // tag::range-slider[]
        Form form = new Form("Filters", BoxLayout.y());
        Label summary = new Label("$100 - $350");
        RangeSlider price = new RangeSlider(0, 500, 100, 350);
        price.setStep(10);
        price.addActionListener(e -> summary.setText(
                "$" + price.getLowerValue() + " - $" + price.getUpperValue()));
        form.addAll(summary, price);
        form.show();
        // end::range-slider[]
    }

    public void chips() {
        // tag::chips[]
        Form form = new Form("Chips", BoxLayout.y());
        Container filters = new Container(new FlowLayout());
        Chip unread = new Chip("Unread").setSelectable(true);
        unread.addActionListener(e -> Log.p("Unread filter: " + unread.isSelected()));
        Chip travel = new Chip("Travel").setRemovable(true);
        travel.addRemoveListener(e -> filters.removeComponent(travel));
        filters.add(unread).add(travel);
        form.add(filters);
        form.show();
        // end::chips[]
    }

    public void chipInput() {
        // tag::chip-input[]
        Form form = new Form("Tags", BoxLayout.y());
        ChipInput tags = new ChipInput();
        tags.setHint("Add a tag");
        tags.setSeparators(", ");
        tags.addValue("design");
        tags.addActionListener(e -> Log.p("Tags: " + tags.getValues()));
        form.add(tags);
        form.show();
        // end::chip-input[]
    }

    public void colorPicker() {
        // tag::color-picker[]
        Form form = new Form("Accent color", BoxLayout.y());
        Label preview = new Label("Preview");
        preview.getAllStyles().setBgTransparency(255);
        ColorPicker picker = new ColorPicker(0x3b82f6);
        picker.setPresets(0xef4444, 0xf59e0b, 0x10b981, 0x3b82f6, 0x8b5cf6);
        picker.addActionListener(e -> {
            preview.getAllStyles().setBgColor(picker.getColor());
            preview.repaint();
        });
        form.addAll(picker, preview);
        form.show();
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

    public void circularProgress() {
        // tag::circular-progress[]
        Form form = new Form("Upload", BoxLayout.y());
        CircularProgress upload = new CircularProgress(0);
        upload.setRingSize(16, 2);
        form.add(upload);
        form.show();
        // as the work advances
        upload.setProgress(65);
        // or a label of your own instead of the percentage
        upload.setText("3 of 5");
        // end::circular-progress[]
    }

    public void audioWaveform() {
        // tag::audio-waveform[]
        Form form = new Form("Voice message", BoxLayout.y());
        // 16-bit samples, normally decoded from the recording
        short[] pcm = new short[4800];
        for (int i = 0; i < pcm.length; i++) {
            pcm[i] = (short) (Math.sin(i / 40.0) * 12000 * Math.sin(i / 900.0));
        }
        AudioWaveform wave = new AudioWaveform(AudioWaveform.levelsFromPcm(pcm, 48));
        wave.setSeekable(true);
        wave.addActionListener(e -> Log.p("Seek to " + wave.getProgress()));
        form.add(wave);
        form.show();
        // while playing, unless the user is dragging
        if (!wave.isSeeking()) {
            wave.setProgress(0.25f);
        }
        // end::audio-waveform[]
    }

    public void expandableText() {
        // tag::expandable-text[]
        Form form = new Form("Review", BoxLayout.y());
        String review = "The battery lasts a full day of heavy use, the screen is bright "
                + "enough to read outdoors and the camera is quick to focus, even in low light.";
        ExpandableText text = new ExpandableText(review, 3);
        text.setLinkText("More", "Less");
        form.add(text);
        form.show();
        // end::expandable-text[]
    }

    public void activityTimeline() {
        // tag::activity-timeline[]
        Form form = new Form("Order", BoxLayout.y());
        ActivityTimeline tracking = new ActivityTimeline()
                .addStep("Order placed", "We received your order", "9:02", ActivityTimeline.DONE)
                .addStep("Shipped", "Left the warehouse", "13:40", ActivityTimeline.DONE)
                .addStep("Out for delivery", null, null, ActivityTimeline.CURRENT)
                .addStep("Delivered", null, null, ActivityTimeline.PENDING);
        form.add(tracking);
        form.show();
        // when the status changes
        tracking.setState(2, ActivityTimeline.DONE);
        tracking.setState(3, ActivityTimeline.CURRENT);
        // end::activity-timeline[]
    }

    public void emojiPicker() {
        // tag::emoji-picker[]
        Form form = new Form("Message", new BorderLayout());
        TextField message = new TextField();
        EmojiPicker emoji = new EmojiPicker();
        emoji.bindTo(message);
        form.add(BorderLayout.NORTH, message);
        form.add(BorderLayout.CENTER, emoji);
        form.show();
        // end::emoji-picker[]
    }

    public void mentionField() {
        // tag::mention-field[]
        Form form = new Form("Comment", BoxLayout.y());
        MentionField comment = new MentionField();
        comment.setCandidates("Ada Lovelace", "Alan Turing", "Grace Hopper");
        comment.getTextField().setHint("Write a comment");
        comment.addActionListener(e -> Log.p("Will notify " + comment.getMentions()));
        form.add(comment);
        form.show();
        // end::mention-field[]
    }

    public void masonry() {
        // tag::masonry[]
        Form form = new Form("Gallery", new BorderLayout());
        Container feed = new Container(new MasonryLayout(2));
        feed.setScrollableY(true);
        int[] heights = {240, 160, 300, 200, 180, 260};
        for (int h : heights) {
            // stands in for a downloaded photo
            Image photo = Image.createImage(200, h, 0xff90caf9);
            feed.add(new ScaleImageLabel(photo));
        }
        form.add(BorderLayout.CENTER, feed);
        form.show();
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
