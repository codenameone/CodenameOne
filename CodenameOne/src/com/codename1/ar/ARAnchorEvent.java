/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ar;

/// Describes a change to an `ARAnchor`, delivered to `ARAnchorListener`s on
/// the EDT. Anchors recognized by the platform - detected reference images and
/// tracked faces - arrive here as `ARImageAnchor` / `ARFaceAnchor` subtypes;
/// use `instanceof` to handle them specifically.
public final class ARAnchorEvent {
    /// The kind of change that occurred.
    public enum Kind {
        /// The anchor was added - either created by the application or
        /// recognized by the platform (image or face).
        ADDED,

        /// The anchor's pose or tracking state was refined.
        UPDATED,

        /// The anchor was removed and is now detached.
        REMOVED
    }

    private final Kind kind;
    private final ARAnchor anchor;
    private final ARSession session;

    ARAnchorEvent(Kind kind, ARAnchor anchor, ARSession session) {
        this.kind = kind;
        this.anchor = anchor;
        this.session = session;
    }

    /// The kind of change that occurred.
    public Kind getKind() {
        return kind;
    }

    /// The anchor that changed.
    public ARAnchor getAnchor() {
        return anchor;
    }

    /// The session the anchor belongs to.
    public ARSession getSession() {
        return session;
    }
}
