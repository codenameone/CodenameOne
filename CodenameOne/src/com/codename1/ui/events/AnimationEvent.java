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
package com.codename1.ui.events;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/// An event reported by an interactive animated image, such as a Rive
/// animation, delivered to the listeners added with
/// `Image.addAnimationListener(ActionListener)`.
///
/// The animation designer names each event and may attach custom properties
/// to it. An event of type [#TYPE_OPEN_URL] asks the application to open
/// [#getUrl()]; nothing is opened on its behalf.
public class AnimationEvent extends ActionEvent {
    /// A general event.
    public static final int TYPE_GENERAL = 0;

    /// An event asking the application to open a URL, see [#getUrl()].
    public static final int TYPE_OPEN_URL = 1;

    private final String name;
    private final int animationEventType;
    private final String url;
    private final String target;
    private final Map<String, Object> properties;

    /// Creates an event.
    ///
    /// #### Parameters
    ///
    /// - `source`: the image that reported the event
    /// - `name`: the name the designer gave the event
    /// - `type`: [#TYPE_GENERAL] or [#TYPE_OPEN_URL]
    /// - `url`: the URL of an open-URL event, or null
    /// - `target`: the browser target of an open-URL event, or null
    /// - `properties`: the custom properties of the event, or null for none
    public AnimationEvent(Object source, String name, int type, String url, String target,
                          Map<String, Object> properties) {
        super(source, ActionEvent.Type.Other);
        this.name = name == null ? "" : name;
        this.animationEventType = type;
        this.url = url;
        this.target = target;
        this.properties = properties == null
                ? Collections.unmodifiableMap(new HashMap<String, Object>())
                : Collections.unmodifiableMap(new HashMap<String, Object>(properties));
    }

    /// The name the designer gave the event.
    ///
    /// #### Returns
    ///
    /// the name, never null
    public String getName() {
        return name;
    }

    /// The kind of event, [#TYPE_GENERAL] or [#TYPE_OPEN_URL].
    ///
    /// #### Returns
    ///
    /// the type
    public int getType() {
        return animationEventType;
    }

    /// The URL of an open-URL event.
    ///
    /// #### Returns
    ///
    /// the URL, or null for a general event
    public String getUrl() {
        return url;
    }

    /// The browser target of an open-URL event: `_blank`, `_parent`, `_self`
    /// or `_top`.
    ///
    /// #### Returns
    ///
    /// the target, or null for a general event
    public String getTarget() {
        return target;
    }

    /// The custom properties of the event by name: a `Float` for a number, a
    /// `Boolean` or a `String`.
    ///
    /// #### Returns
    ///
    /// an unmodifiable map, empty when the event has none
    public Map<String, Object> getProperties() {
        return properties;
    }

    @Override
    public String toString() {
        return "AnimationEvent[" + name + "]";
    }
}
