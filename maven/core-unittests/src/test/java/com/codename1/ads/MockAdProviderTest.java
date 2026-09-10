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
package com.codename1.ads;

import com.codename1.ads.mock.MockAdProvider;
import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MockAdProviderTest extends UITestBase {
    private final List<String> events = new ArrayList<String>();

    @org.junit.jupiter.api.BeforeEach
    void resetEvents() { events.clear(); }

    private AdListener listener() {
        return new AdListener() {
            @Override public void onShown() { events.add("shown"); }
            @Override public void onImpression() { events.add("impression"); }
            @Override public void onDismissed() { events.add("dismissed"); }
        };
    }

    @FormTest
    void interstitialStaysVisibleUntilClosedAndRestoresPreviousForm() {
        Form previous = CN.getCurrentForm();
        MockAdProvider.install();
        InterstitialAd ad = new InterstitialAd("mock");
        ad.setAdListener(listener());
        ad.load();
        ad.show();
        Form showing = CN.getCurrentForm();
        assertNotSame(previous, showing);
        assertEquals("Mock advertisement", showing.getTitle());
        assertEquals(Arrays.asList("shown", "impression"), events);
        Button close = (Button) showing.getContentPane().getComponentAt(1);
        close.pressed();
        close.released();
        assertSame(previous, CN.getCurrentForm());
        assertEquals(Arrays.asList("shown", "impression", "dismissed"), events);
        showing.getBackCommand().actionPerformed(new ActionEvent(showing));
        assertEquals(3, events.size(), "Closing twice must not repeat callbacks");
        ad.dispose();
    }

    @FormTest
    void rewardedBackCloseDeliversRewardBeforeDismissal() {
        Form previous = CN.getCurrentForm();
        MockAdProvider.install();
        RewardedAd ad = new RewardedAd("mock");
        ad.setAdListener(listener());
        ad.setOnUserEarnedRewardListener(reward -> events.add("reward"));
        ad.load();
        ad.show();
        Form showing = CN.getCurrentForm();
        assertEquals(Arrays.asList("shown", "impression"), events);
        showing.getBackCommand().actionPerformed(new ActionEvent(showing));
        assertSame(previous, CN.getCurrentForm());
        assertEquals(Arrays.asList("shown", "impression", "reward", "dismissed"), events);
        ad.dispose();
    }

    @FormTest
    void disposingVisibleAdRestoresFormWithoutRewardOrDismissal() {
        Form previous = CN.getCurrentForm();
        MockAdProvider.install();
        RewardedAd ad = new RewardedAd("mock");
        ad.setAdListener(listener());
        ad.setOnUserEarnedRewardListener(reward -> events.add("reward"));
        ad.load();
        ad.show();
        ad.dispose();
        assertSame(previous, CN.getCurrentForm());
        assertEquals(Arrays.asList("shown", "impression"), events);
    }
}
