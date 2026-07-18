package com.codename1.flutter.widgets;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

/**
 * Internal widget of {@link GestureDetector}: the transparent pointer
 * overlay mounted after the detected child so its component sits on top of
 * the child subtree in the flat container. Not part of the Dart-facing API.
 */
class GestureOverlay extends Widget {

    @Override
    public Element createElement() {
        return new GestureOverlayRenderElement(this);
    }
}
