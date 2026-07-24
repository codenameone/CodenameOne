package com.codename1.flutter.widgets;

import com.codename1.flutter.Widget;

/**
 * Implemented by single-child wrapper widgets that render their child
 * unchanged (accessibility, clipping, hover, tooltip, ...). Lets a single
 * {@link PassThroughRenderElement} serve every such widget.
 */
public interface HasChild {

    /**
     * The wrapped child widget (may be null).
     */
    Widget getChild();
}
