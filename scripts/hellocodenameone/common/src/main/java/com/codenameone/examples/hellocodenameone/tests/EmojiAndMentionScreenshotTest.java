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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.components.EmojiPicker;
import com.codename1.components.MentionField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.Layout;

/// A `MentionField` showing suggestions for a half typed mention over an
/// `EmojiPicker`, in light and dark. The recent tab is off, since what it
/// holds depends on earlier runs.
public class EmojiAndMentionScreenshotTest extends DualAppearanceBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=EmojiAndMention status=SKIPPED reason=phone-layout-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "EmojiAndMention";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        MentionField mention = new MentionField();
        mention.setCandidates("Ada Lovelace", "Alan Turing", "Grace Hopper", "Alonzo Church");
        mention.setText("Thanks @al");
        EmojiPicker emoji = new EmojiPicker();
        emoji.setRecentsEnabled(false);
        form.add(BorderLayout.NORTH, mention);
        form.add(BorderLayout.CENTER, emoji);
    }
}
