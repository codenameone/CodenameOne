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
package com.codename1.flutter.animation;

import com.codename1.flutter.Element;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.foundation.Listenable;

/**
 * A widget that rebuilds when a {@link Listenable} it is given notifies its
 * listeners — Flutter's {@code AnimatedWidget}. Subclasses implement
 * {@code build(BuildContext)} and read {@link #listenable()} (usually an
 * {@code Animation}) to derive the current frame. Modelled on top of
 * {@link StatelessWidget}: its build runs as a pure function of the current
 * listenable value.
 */
public abstract class AnimatedWidget extends StatelessWidget {

    private Listenable listenable;

    /** Named parameter setter for the Dart {@code listenable:} parameter. */
    public void listenable(Listenable v) {
        this.listenable = v;
    }

    /** Getter for the driving {@link Listenable}. */
    public Listenable listenable() {
        return listenable;
    }

    /**
     * An element that LISTENS. The whole point of the type is that a notification rebuilds
     * the widget, so the plain {@code StatelessElement} a StatelessWidget would otherwise
     * get leaves every subclass frozen on its first frame.
     */
    @Override
    public Element createElement() {
        return new AnimatedWidgetElement(this);
    }
}
