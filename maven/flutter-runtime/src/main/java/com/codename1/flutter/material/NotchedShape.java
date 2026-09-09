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
package com.codename1.flutter.material;

/**
 * The strategy that carves a notch out of a shape for a docked FAB — Flutter's
 * {@code NotchedShape} interface.
 */
public abstract class NotchedShape {

    /**
     * The outline of {@code host} with a notch carved for {@code guest}.
     *
     * <p>Declared here so a transpiled strategy -- the mail study's
     * {@code WaterfallNotchedRectangle} is one -- can be CALLED. This was an empty marker
     * class, so the shape a BottomAppBar was given could be stored and never asked for
     * anything, and the bar drew as a plain rectangle with the docked button sitting on
     * an edge it should have been cut into.</p>
     *
     * <p>Returns null when the strategy cannot produce a path, which is the answer for
     * anything that has not overridden it; the caller then draws the host unchanged.</p>
     */
    public com.codename1.flutter.Path getOuterPath(com.codename1.flutter.Rect host,
            com.codename1.flutter.Rect guest) {
        return null;
    }
}
