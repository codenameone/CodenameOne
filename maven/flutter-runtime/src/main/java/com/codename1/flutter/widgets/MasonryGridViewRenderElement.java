package com.codename1.flutter.widgets;

import com.codename1.flutter.Widget;

/**
 * Scroll boundary for {@link MasonryGridView}. The staggered/windowed layout is
 * deferred to a later milestone, so for now the scrollable has no content body.
 */
public class MasonryGridViewRenderElement extends ScrollRenderElement {

    public MasonryGridViewRenderElement(MasonryGridView widget) {
        super(widget);
    }

    @Override
    protected Widget buildContent() {
        return null;
    }
}
