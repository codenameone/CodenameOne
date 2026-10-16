/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code ImageRenderElement.loadImage} short-circuits when {@code source.equals
 * (loadedSource)} -- the mechanism that keeps a rebuild of the SAME {@code
 * Image.network} from re-fetching every time its parent rebuilds. A terminal failure
 * (no bytes came back, or what came back would not decode) used to leave {@code
 * loadedSource} set to the failed source anyway, so that short-circuit then kept
 * answering "already loaded" forever -- a transient network failure left the image
 * blank until the widget unmounted or its URL changed, with no later rebuild ever
 * trying again. {@link ImageRenderElement#onLoadFailed} clears the marker on a
 * terminal failure so the NEXT rebuild retries, matching Flutter's own {@code
 * ImageCache}, which evicts a failed load rather than caching the blank permanently.
 *
 * <p>Exercises the fix through the package-private seams {@code
 * simulateLoadStartedForTest}/{@code isLoadedForTest}, which drive exactly the state
 * {@code loadImage}/{@code fetchNetworkImage} manage, without needing a real {@code
 * Display}, {@code Storage} or network round trip -- none of which this fix depends
 * on to be correct.</p>
 */
class FailedNetworkImageRetryTest {

    @Test
    void terminalFailureClearsTheMarkerSoALaterRebuildRetries() {
        ImageRenderElement e = new ImageRenderElement(
                Image.network("https://example.com/a.png", null, null, null, null));
        String source = "url:https://example.com/a.png";

        e.simulateLoadStartedForTest(source);
        assertTrue(e.isLoadedForTest(source), "loadImage marks the source loaded up front");

        e.onLoadFailed(source);

        assertFalse(e.isLoadedForTest(source),
                "a terminal failure must clear the marker so a later rebuild retries");
    }

    @Test
    void aStaleFailureMustNotClearANewerLoadsMarker() {
        // The response for an old fetch can arrive after the widget has already moved
        // on to a different source (a fast-scrolling list reusing this element) -- that
        // stale failure must not clear the NEW source's marker.
        ImageRenderElement e = new ImageRenderElement(
                Image.network("https://example.com/a.png", null, null, null, null));
        String oldSource = "url:https://example.com/old.png";
        String newSource = "url:https://example.com/new.png";

        e.simulateLoadStartedForTest(oldSource);
        e.simulateLoadStartedForTest(newSource);   // the widget moved on

        e.onLoadFailed(oldSource);   // the OLD fetch's late, failed response

        assertTrue(e.isLoadedForTest(newSource), "the current load's marker must survive");
    }

    @Test
    void repeatedFailuresNeverRetryOnTheirOwn() {
        // The fix clears a marker; it must never schedule or trigger a retry itself --
        // only a LATER rebuild (a fresh loadImage call) may try again, so a network
        // that keeps failing settles at "not loaded" rather than looping.
        ImageRenderElement e = new ImageRenderElement(
                Image.network("https://example.com/a.png", null, null, null, null));
        String source = "url:https://example.com/a.png";

        e.simulateLoadStartedForTest(source);
        e.onLoadFailed(source);
        e.onLoadFailed(source);
        e.onLoadFailed(source);

        assertFalse(e.isLoadedForTest(source));
    }
}
