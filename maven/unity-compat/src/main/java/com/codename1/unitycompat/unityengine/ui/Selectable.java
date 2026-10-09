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
package com.codename1.unitycompat.unityengine.ui;

import com.codename1.unitycompat.unityengine.Component;
import com.codename1.unitycompat.unityengine.UnityRuntime;
import com.codename1.unitycompat.unityengine.eventsystems.UIBehaviour;

/// `UnityEngine.UI.Selectable`: the base of every control a pointer works
/// -- what it means to be interactable, and how the control shows the
/// state it is in.
///
/// Of Unity's transitions the one implemented is the default, *Color
/// Tint*: the target graphic's colour is multiplied by the colour of the
/// state. Unity fades from one to the next over a tenth of a second; here
/// the change is immediate. The states, most pressing first, are
/// disabled, pressed, selected (the control last clicked), highlighted
/// (the pointer is over it) and normal.
@SuppressWarnings("PMD.MethodNamingConventions") // C# member names: translated code binds to them by name
public class Selectable extends UIBehaviour {
    private boolean interactable = true;
    /// `Selectable.Transition`: 0 none, 1 colour tint.
    private int transition = 1;
    private Graphic targetGraphic;
    private final int[] colors = {0xffffffff, 0xfff5f5f5, 0xffc8c8c8, 0xfff5f5f5, 0x80c8c8c8};
    private int state;

    /// What a scene file sets.
    public void $setup(boolean canInteract, int transitionKind, Graphic target) {
        interactable = canInteract;
        transition = transitionKind;
        targetGraphic = target;
        show();
    }

    /// The tint of one state as ARGB, already multiplied by the colour
    /// multiplier: 0 normal, 1 highlighted, 2 pressed, 3 selected,
    /// 4 disabled.
    public void $color(int which, int argb) {
        colors[which] = argb;
        show();
    }

    @Override
    public Component $new() {
        return new Selectable();
    }

    @Override
    public void $copyFrom(Component source) {
        super.$copyFrom(source);
        Selectable s = (Selectable) source;
        interactable = s.interactable;
        transition = s.transition;
        Object target = UnityRuntime.$remap(s.targetGraphic);
        targetGraphic = target instanceof Graphic ? (Graphic) target : null;
        System.arraycopy(s.colors, 0, colors, 0, colors.length);
        state = 0;
        show();
    }

    /// Tells the control where the pointer is, for it to show. The
    /// runtime calls this when one of the three changes.
    public void $pointer(boolean pressed, boolean selected, boolean over) {
        state = pressed ? 2 : selected ? 3 : over ? 1 : 0;
        show();
    }

    private void show() {
        if (targetGraphic != null && transition == 1) {
            targetGraphic.$tint(colors[interactable ? state : 4]);
        }
    }

    public boolean get_interactable() {
        return interactable;
    }

    public void set_interactable(boolean value) {
        interactable = value;
        show();
    }

    public boolean IsInteractable() {
        return interactable;
    }

    /// The pointer went down and came up again over this control.
    public void $click() {
    }
}
