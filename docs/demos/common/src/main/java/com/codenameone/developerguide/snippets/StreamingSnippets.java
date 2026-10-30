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

import com.codename1.media.DrmConfig;
import com.codename1.media.DrmScheme;
import com.codename1.media.StreamRequest;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;

/// Snippets of the streaming sections of the developer guide.
public final class StreamingSnippets {
    private StreamingSnippets() {
    }

    static void playProtected() {
        // tag::reel-stream[]
        String manifestUrl = "https://reel.example.com/media/42/master.m3u8";
        String licenseUrl = "https://reel.example.com/api/license/clearkey";
        String licenseToken = "token-from-playback-start";
        int resumeSeconds = 90;
        Form player = new Form("Player", new BorderLayout());
        StreamRequest req = new StreamRequest(manifestUrl)
                .setStartPosition(resumeSeconds * 1000)
                .setDrm(new DrmConfig(DrmScheme.CLEARKEY, licenseUrl)
                        .addLicenseHeader("X-Reel-License", licenseToken));
        req.createAsync().ready(media -> {
            player.add(BorderLayout.CENTER, media.getVideoComponent());
            player.revalidate();
        });
        player.show();
        // end::reel-stream[]
    }
}
