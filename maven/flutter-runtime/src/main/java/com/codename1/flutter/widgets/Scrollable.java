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
import com.codename1.flutter.Element;
import com.codename1.flutter.RenderElement;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import dart.async.Future;
import dart.runtime.Funcs;

/// Flutter's `Scrollable` statics. {@code ensureVisible} scrolls the nearest
/// scrolling ancestor of the context's widget until it is in view.
public final class Scrollable {

    private Scrollable() {
    }

    public static Future<Object> ensureVisible(BuildContext context, Object alignment, Object duration,
                                               Object curve, Object alignmentPolicy) {
        Component c = componentOf(context);
        if (c != null) {
            Component child = c;
            for (Container p = c.getParent(); p != null; child = p, p = p.getParent()) {
                if (p.isScrollableY() || p.isScrollableX()) {
                    p.scrollComponentToVisible(child == c ? c : child);
                    break;
                }
            }
        }
        return Future.value(null);
    }

    private static Component componentOf(BuildContext context) {
        if (!(context instanceof Element)) {
            return null;
        }
        final Component[] found = new Component[1];
        Funcs.VoidFunc1<Element> visit = new Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element e) {
                if (found[0] != null) {
                    return;
                }
                if (e instanceof RenderElement && ((RenderElement) e).component() != null) {
                    found[0] = ((RenderElement) e).component();
                    return;
                }
                e.visitChildren(this);
            }
        };
        visit.call((Element) context);
        return found[0];
    }
}
