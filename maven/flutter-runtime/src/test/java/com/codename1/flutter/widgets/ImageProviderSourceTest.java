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

import com.codename1.flutter.MemoryImage;
import com.codename1.flutter.NetworkImage;
import dart.core.DartMap;
import dart.typed_data.Uint8List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/// Image(image: provider) keeps everything the render element needs to load
/// what the provider describes.
public class ImageProviderSourceTest {

    @Test
    public void networkImageHeadersReachTheWidget() {
        DartMap<String, String> headers = new DartMap<String, String>();
        headers.put("Authorization", "Bearer abc");
        NetworkImage p = new NetworkImage("https://example.com/a.png");
        p.headers(headers);
        Image img = new Image();
        img.image(p);
        assertEquals("https://example.com/a.png", img.getUrl());
        assertNotNull(img.getHeaders(), "the headers must not be dropped");
        assertEquals("Bearer abc", img.getHeaders().get("Authorization"));
    }

    @Test
    public void aChangedTokenIsADifferentSource() {
        // The render element reloads only when the source key changes, so a
        // refreshed token has to change it.
        Image a = withHeader("Bearer one");
        Image b = withHeader("Bearer two");
        assertNotEquals(a.sourceKey(), b.sourceKey());
    }

    @Test
    public void noHeadersKeepsTheBareUrlSource() {
        Image img = new Image();
        img.image(new NetworkImage("https://example.com/a.png"));
        assertNull(img.getHeaders());
        assertEquals("url:https://example.com/a.png", img.sourceKey());
    }

    @Test
    public void memoryImageCarriesItsBytes() {
        Uint8List bytes = new Uint8List(4);
        Image img = new Image();
        img.image(new MemoryImage(bytes));
        assertSame(bytes.toBytes(), img.getMemoryBytes(), "the buffer to decode");
        assertNull(img.getUrl());
        assertNull(img.getAssetName());
    }

    @Test
    public void memoryImageKeepsItsScale() {
        MemoryImage provider = new MemoryImage(new Uint8List(4));
        provider.scale(2.0);
        Image img = new Image();
        img.image(provider);
        assertEquals(2.0, img.getMemoryScale(), 0.0,
                "two encoded pixels per logical pixel, not one");
        Image plain = new Image();
        plain.image(new MemoryImage(new Uint8List(4)));
        assertEquals(1.0, plain.getMemoryScale(), 0.0);
    }

    @Test
    public void aDifferentBufferIsADifferentSource() {
        Image a = new Image();
        a.image(new MemoryImage(new Uint8List(4)));
        Image b = new Image();
        b.image(new MemoryImage(new Uint8List(4)));
        assertNotEquals(a.sourceKey(), b.sourceKey());
    }

    private static Image withHeader(String token) {
        DartMap<String, String> headers = new DartMap<String, String>();
        headers.put("Authorization", token);
        NetworkImage p = new NetworkImage("https://example.com/a.png");
        p.headers(headers);
        Image img = new Image();
        img.image(p);
        return img;
    }
}
