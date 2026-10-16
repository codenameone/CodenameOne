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

/**
 * Describes how scrollables should behave app-wide — Flutter's
 * {@code ScrollBehavior}: which input devices drag, whether scrollbars and
 * overscroll indicators appear, and the default physics. {@link #copyWith}
 * produces a derived behaviour with selected properties overridden.
 */
public class ScrollBehavior {

    private Boolean scrollbars;
    private Boolean overscroll;

    public ScrollBehavior() {
    }

    /**
     * {@code ScrollBehavior.copyWith}: a copy of this behaviour with the given
     * properties overridden. Unmodelled properties are accepted and ignored.
     */
    public ScrollBehavior copyWith(Boolean scrollbars, Boolean overscroll,
            Object physics, Object platform, Object dragDevices) {
        ScrollBehavior b = newInstance();
        b.scrollbars = scrollbars != null ? scrollbars : this.scrollbars;
        b.overscroll = overscroll != null ? overscroll : this.overscroll;
        return b;
    }

    /** Allows subclasses (e.g. MaterialScrollBehavior) to preserve their type. */
    protected ScrollBehavior newInstance() {
        return new ScrollBehavior();
    }

    public Boolean getScrollbars() {
        return scrollbars;
    }

    public Boolean getOverscroll() {
        return overscroll;
    }
}
