/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.flutter.plugins.emojipicker;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.TextEditingController;
import com.codename1.flutter.rendering.Size;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * emoji_picker_flutter's EmojiPicker, drawn with Codename One components: a tab per
 * category (recents first), a grid of emoji buttons and a backspace button. A
 * selection is appended to the textEditingController when there is one, then
 * reported; backspace removes the last emoji from it. Only Config.height is applied.
 */
public class EmojiPicker extends Widget {
    Funcs.VoidFunc2<Category, Emoji> onEmojiSelected;
    Funcs.VoidFunc0 onBackspacePressed;
    TextEditingController textEditingController;
    Config config = new Config();

    public EmojiPicker() {
    }

    /** The built-in set, a static getter in Dart. */
    public static final DartList<CategoryEmoji> defaultEmojiSet = EmojiData.all();

    public void onEmojiSelected(Funcs.VoidFunc2<Category, Emoji> v) {
        onEmojiSelected = v;
    }

    public void onBackspacePressed(Funcs.VoidFunc0 v) {
        onBackspacePressed = v;
    }

    public void textEditingController(TextEditingController v) {
        textEditingController = v;
    }

    /** Accepted and ignored. */
    public void scrollController(Object v) {
    }

    public void config(Config v) {
        if (v != null) {
            config = v;
        }
    }

    Size preferredLayoutSize() {
        return new Size(360, config.height);
    }

    void selected(Category c, Emoji e) {
        EmojiData.remember(e);
        if (textEditingController != null) {
            String t = textEditingController.text();
            textEditingController.text((t == null ? "" : t) + e.emoji());
        }
        if (onEmojiSelected != null) {
            onEmojiSelected.call(c, e);
        }
    }

    void backspace() {
        if (textEditingController != null) {
            String t = textEditingController.text();
            if (t != null && t.length() > 0) {
                int cut = t.length() - 1;
                if (cut > 0 && Character.isLowSurrogate(t.charAt(cut)) && Character.isHighSurrogate(t.charAt(cut - 1))) {
                    cut--;
                }
                textEditingController.text(t.substring(0, cut));
            }
        }
        if (onBackspacePressed != null) {
            onBackspacePressed.call();
        }
    }

    @Override
    public Element createElement() {
        return new EmojiPickerRenderElement(this);
    }
}
