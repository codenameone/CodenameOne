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

/**
 * The text-direction-aware form of {@link Positioned} used inside a {@code Stack}:
 * {@code start}/{@code end} resolve to left/right against the ambient text direction —
 * Flutter's {@code PositionedDirectional}.
 *
 * <p>It hosted the child and dropped every inset, so anything positioned this way landed
 * wherever the Stack happened to put it. Resolving to a real {@link Positioned} is all it
 * needs: the ambient direction decides which edge {@code start} means.</p>
 */
public class PositionedDirectional extends StatelessWidget {

    private Double start;
    private Double top;
    private Double end;
    private Double bottom;
    private Double width;
    private Double height;
    private Widget child;

    public void start(double v) {
        this.start = v;
    }

    public void top(double v) {
        this.top = v;
    }

    public void end(double v) {
        this.end = v;
    }

    public void bottom(double v) {
        this.bottom = v;
    }

    public void width(double v) {
        this.width = v;
    }

    public void height(double v) {
        this.height = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Double getStart() {
        return start;
    }

    public Double getEnd() {
        return end;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        boolean rtl = Directionality.of(context) == com.codename1.flutter.TextDirection.rtl;
        Positioned p = new Positioned();
        if (start != null) {
            if (rtl) {
                p.right(start.doubleValue());
            } else {
                p.left(start.doubleValue());
            }
        }
        if (end != null) {
            if (rtl) {
                p.left(end.doubleValue());
            } else {
                p.right(end.doubleValue());
            }
        }
        if (top != null) {
            p.top(top.doubleValue());
        }
        if (bottom != null) {
            p.bottom(bottom.doubleValue());
        }
        if (width != null) {
            p.width(width.doubleValue());
        }
        if (height != null) {
            p.height(height.doubleValue());
        }
        p.child(child);
        return p;
    }
}
