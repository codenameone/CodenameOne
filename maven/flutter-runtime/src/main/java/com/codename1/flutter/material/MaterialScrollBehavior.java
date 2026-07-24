package com.codename1.flutter.material;

import com.codename1.flutter.widgets.ScrollBehavior;

/**
 * The Material default scroll behaviour — Flutter's {@code
 * MaterialScrollBehavior}. new_gallery's shrine app installs
 * {@code const MaterialScrollBehavior().copyWith(scrollbars: false)} on its
 * MaterialApp.
 */
public class MaterialScrollBehavior extends ScrollBehavior {

    public MaterialScrollBehavior() {
    }

    @Override
    protected ScrollBehavior newInstance() {
        return new MaterialScrollBehavior();
    }
}
