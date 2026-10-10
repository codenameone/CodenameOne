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
package com.codename1.fxcompat.runtime;

/// What a property change makes stale. A property created through
/// [FxDouble], [FxBoolean], [FxObject] or [FxString] reports the bits it
/// was given to its owner each time its value is invalidated.
public final class Dirty {

    /// Nothing beyond the listeners.
    public static final int NONE = 0;
    /// The pixels of the node.
    public static final int PAINT = 1;
    /// The size the node asks for, or how it arranges its children.
    public static final int LAYOUT = 2;
    /// Where the node sits in its parent: position, translation, scale,
    /// rotation.
    public static final int BOUNDS = 4;
    /// What a style sheet may match: id, style class, inline style.
    public static final int STYLE = 8;
    /// The state mirrored into the Codename One component of a control.
    public static final int NATIVE = 16;
    /// The shape of a node that computes its own geometry.
    public static final int GEOMETRY = 32;
    /// First bit free for a subclass.
    public static final int USER = 256;

    private Dirty() {
    }

    /// The owner of properties that report what they make stale.
    public interface Owner {
        /// Called when a property changed; `what` is the bits it was
        /// created with.
        void cn1Invalidated(int what);
    }
}
