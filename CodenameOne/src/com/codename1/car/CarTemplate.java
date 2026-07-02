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
package com.codename1.car;

/// The base type for the fixed catalog of in-car templates. Unlike Codename One `Form`s, the head
/// unit (Apple CarPlay / Google Android Auto) does not allow arbitrary rendering -- it only displays
/// one of a small, driver-distraction-vetted set of templates. A `CarScreen` produces exactly one
/// `CarTemplate` from its `CarScreen#createTemplate()` method.
///
/// Concrete templates:
///
/// - `CarListTemplate` -- a (optionally sectioned) scrolling list of rows.
/// - `CarGridTemplate` -- a grid of image-forward items.
/// - `CarPaneTemplate` -- a detail pane of label/value rows plus action buttons.
/// - `CarMessageTemplate` -- a short message with actions (and read-aloud/voice-reply for messaging).
/// - `CarNavigationTemplate` -- a turn-by-turn surface with a drawable map (navigation apps only).
/// - `CarNowPlayingTemplate` -- the system now-playing surface for audio apps.
public abstract class CarTemplate {
    private String title;

    /// Returns the template title shown in the head-unit header, or null.
    public String getTitle() {
        return title;
    }

    /// Sets the template title shown in the head-unit header.
    ///
    /// #### Parameters
    ///
    /// - `title`: the header title
    void setTitleInternal(String title) {
        this.title = title;
    }
}
