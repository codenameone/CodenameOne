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
import com.codename1.flutter.Offset;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Size;
import dart.core.DartList;

/// Flutter's {@code CustomSingleChildLayout}: one child, sized and placed by a
/// SingleChildLayoutDelegate.
///
/// The child is laid out under the delegate's constraints with its largest size
/// taken as its size (the box Flutter would hand it when the delegate's
/// constraints are tight, the usual case); getPositionForChild then places it.
public class CustomSingleChildLayout extends StatelessWidget {

    private SingleChildLayoutDelegate delegate;
    private Widget child;

    public void delegate(SingleChildLayoutDelegate v) {
        this.delegate = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public SingleChildLayoutDelegate getDelegate() {
        return delegate;
    }

    @Override
    public Widget build(BuildContext context) {
        LayoutBuilder lb = new LayoutBuilder();
        lb.builder((ctx, constraints) -> layout(constraints));
        return lb;
    }

    Widget layout(BoxConstraints constraints) {
        Widget c = child != null ? child : new SizedBox();
        if (delegate == null) {
            return c;
        }
        Size size = delegate.getSize(constraints);
        BoxConstraints cc = delegate.getConstraintsForChild(constraints);
        Size childSize = new Size(cc.maxWidth(), cc.maxHeight());
        Offset at = delegate.getPositionForChild(size, childSize);
        Positioned p = new Positioned();
        p.left(at.dx());
        p.top(at.dy());
        if (!Double.isInfinite(childSize.width())) {
            p.width(childSize.width());
        }
        if (!Double.isInfinite(childSize.height())) {
            p.height(childSize.height());
        }
        p.child(c);
        Stack st = new Stack();
        st.children(DartList.of(p));
        SizedBox box = new SizedBox();
        box.width(size.width());
        box.height(size.height());
        box.child(st);
        return box;
    }
}
