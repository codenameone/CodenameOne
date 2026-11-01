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

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/// Flutter's {@code SliverPersistentHeader}: a header among slivers built by its
/// delegate.
///
/// The header is built at its full extent, with a shrinkOffset of 0 and nothing
/// under it: the sliver scroll views here lay slivers out as boxes and report no
/// scroll offset to a sliver, so a pinned or floating header scrolls away with
/// the content instead of shrinking. pinned and floating are kept for
/// isPinned()/isFloating().
public class SliverPersistentHeader extends StatelessWidget {

    private SliverPersistentHeaderDelegate delegate;
    private boolean pinned;
    private boolean floating;

    public void delegate(SliverPersistentHeaderDelegate v) {
        this.delegate = v;
    }

    public void pinned(boolean v) {
        this.pinned = v;
    }

    public void floating(boolean v) {
        this.floating = v;
    }

    public SliverPersistentHeaderDelegate getDelegate() {
        return delegate;
    }

    public boolean isPinned() {
        return pinned;
    }

    public boolean isFloating() {
        return floating;
    }

    @Override
    public Widget build(BuildContext context) {
        if (delegate == null) {
            return new SizedBox();
        }
        SizedBox box = new SizedBox();
        box.height(delegate.maxExtent());
        box.child(delegate.build(context, 0.0, false));
        return box;
    }
}
