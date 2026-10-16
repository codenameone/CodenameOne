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
package com.codename1.flutter.cupertino;

import com.codename1.flutter.IconData;
import com.codename1.ui.FontImage;

/**
 * iOS-style icons named as in Flutter's {@code CupertinoIcons}. There is no
 * Cupertino icon font in this runtime, so each maps to the closest CN1
 * material icon-font glyph (visually approximate this pass).
 */
public final class CupertinoIcons {

    private CupertinoIcons() {
    }

    public static final IconData home = new IconData(FontImage.MATERIAL_HOME);
    public static final IconData conversation_bubble = new IconData(FontImage.MATERIAL_CHAT_BUBBLE);
    public static final IconData profile_circled = new IconData(FontImage.MATERIAL_ACCOUNT_CIRCLE);
    public static final IconData padlock_solid = new IconData(FontImage.MATERIAL_LOCK);
    public static final IconData search = new IconData(FontImage.MATERIAL_SEARCH);
    public static final IconData settings = new IconData(FontImage.MATERIAL_SETTINGS);
    public static final IconData share = new IconData(FontImage.MATERIAL_SHARE);
    public static final IconData add = new IconData(FontImage.MATERIAL_ADD);
    public static final IconData clear = new IconData(FontImage.MATERIAL_CLOSE);
    public static final IconData back = new IconData(FontImage.MATERIAL_ARROW_BACK);
}
