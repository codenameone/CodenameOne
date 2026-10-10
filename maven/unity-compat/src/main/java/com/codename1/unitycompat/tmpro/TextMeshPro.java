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
package com.codename1.unitycompat.tmpro;

import com.codename1.unitycompat.unityengine.Component;
import com.codename1.unitycompat.unityengine.DrawCommand;
import com.codename1.unitycompat.unityengine.DrawView;
import com.codename1.unitycompat.unityengine.RectTransform;
import com.codename1.unitycompat.unityengine.Transform;

/// `TMPro.TextMeshPro`: text that stands in the world, among the sprites.
///
/// Its rectangle is its `RectTransform`'s size about the pivot, scaled as
/// the object is, and a font size of ten is one world unit tall, which is
/// TextMesh Pro's own measure. It is drawn upright: the turn of the
/// object, and of the camera, is not applied to it. See [TMP_Text] for
/// what else of TextMesh Pro is not here.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public final class TextMeshPro extends TMP_Text {
    private int sortingOrder;
    private int sortingLayerID;
    private final float[] box = new float[4];

    /// What a scene file sets, from the mesh renderer beside the text.
    public void $sorting(int layerID, int order) {
        sortingLayerID = layerID;
        sortingOrder = order;
    }

    @Override
    public Component $new() {
        return new TextMeshPro();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        TextMeshPro t = (TextMeshPro) source;
        sortingOrder = t.sortingOrder;
        sortingLayerID = t.sortingLayerID;
    }

    @Override
    public int $roles() {
        return DRAWS;
    }

    @Override
    public void $draw(DrawView view) {
        if (!$live()) {
            return;
        }
        Transform t = get_transform();
        float width = 0f;
        float height = 0f;
        float pivotX = 0.5f;
        float pivotY = 0.5f;
        if (t instanceof RectTransform) {
            ((RectTransform) t).$box(box);
            width = box[0];
            height = box[1];
            pivotX = box[2];
            pivotY = box[3];
        }
        float sx = t.$worldScaleX();
        float sy = t.$worldScaleY();
        if (sx < 0f) {
            sx = -sx;
        }
        if (sy < 0f) {
            sy = -sy;
        }
        float w = width * sx;
        float h = height * sy;
        float before = w * pivotX;
        float above = h * (1f - pivotY);
        DrawCommand d = view.$text(t.$worldX() - before, t.$worldY() + above, t.$worldZ(), w, h,
                sortingOrder, sortingLayerID);
        if (d != null) {
            fill(d, view.$scale() * sy * 0.1f, view.$scale() * sy);
        }
    }

    public int get_sortingOrder() {
        return sortingOrder;
    }

    public void set_sortingOrder(int value) {
        sortingOrder = value;
    }

    public int get_sortingLayerID() {
        return sortingLayerID;
    }

    public void set_sortingLayerID(int value) {
        sortingLayerID = value;
    }
}
