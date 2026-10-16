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
package com.codename1.flutter;

import dart.runtime.Funcs;

/**
 * Convenience base for render elements holding a single (possibly null)
 * child widget from their configuration.
 */
public abstract class SingleChildRenderElement extends RenderElement {

    private Element child;

    protected SingleChildRenderElement(Widget widget) {
        super(widget);
    }

    /**
     * The child widget from the current configuration (may be null).
     */
    protected abstract Widget childWidget();

    @Override
    protected void syncChildren() {
        child = updateChild(child, childWidget(), 0);
    }

    public Element childElement() {
        return child;
    }

    /**
     * The render element of the child, descending through composition.
     */
    protected RenderElement renderChild() {
        return findRenderElement(child);
    }

    @Override
    public void visitChildren(Funcs.VoidFunc1<Element> visitor) {
        if (child != null) {
            visitor.call(child);
        }
    }
}
