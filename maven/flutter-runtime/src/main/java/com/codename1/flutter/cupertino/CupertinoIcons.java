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
