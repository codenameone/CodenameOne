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

import com.codename1.flutter.ComposedElement;
import com.codename1.flutter.Widget;

/// Hosts an Image that has a frameBuilder, loadingBuilder or errorBuilder. The
/// element's widget is the Image itself, so an ordinary rebuild updates it in
/// place; its child is the same Image drawn by the plain render element, passed
/// through the builders. The render element reports the load outcome here.
final class ImageBuildersElement extends ComposedElement {
    private boolean loaded;
    private Object error;

    ImageBuildersElement(Image widget) {
        super(widget);
    }

    static void notifyLoaded(Image image) {
        ImageBuildersElement e = image.listener;
        if (e != null && (!e.loaded || e.error != null)) {
            e.loaded = true;
            e.error = null;
            e.markNeedsBuild();
        }
    }

    static void notifyFailed(Image image, Object error) {
        ImageBuildersElement e = image.listener;
        if (e != null && e.error == null && image.errorBuilder != null) {
            e.error = error;
            e.markNeedsBuild();
        }
    }

    @Override
    public void update(Widget newWidget) {
        Image old = (Image) widget();
        if (newWidget instanceof Image && !((Image) newWidget).sourceKey().equals(old.sourceKey())) {
            loaded = false;
            error = null;
        }
        super.update(newWidget);
    }

    @Override
    protected Widget build() {
        Image img = (Image) widget();
        img.listener = this;
        if (error != null && img.errorBuilder != null) {
            img.innerPass = false;
            return img.errorBuilder.call(this, error, null);
        }
        img.innerPass = true;
        Widget child = img;
        if (img.getFrameBuilder() != null) {
            child = img.getFrameBuilder().call(this, child, loaded ? Long.valueOf(0) : null, Boolean.FALSE);
        }
        if (img.loadingBuilder != null) {
            child = img.loadingBuilder.call(this, child, loaded ? null : new ImageChunkEvent(0, null));
        }
        return child;
    }
}
