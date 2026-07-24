package com.codename1.flutter.widgets;

import com.codename1.flutter.FlexFit;

/**
 * Marks a child of Row/Column as flexible — Flutter's {@code Flexible}. It
 * receives a share of the free main-axis space proportional to its flex factor
 * (default 1). {@code Expanded} is {@code Flexible} with {@code fit: tight};
 * this class reuses that flex machinery ({@link ExpandedRenderElement} reads
 * the flex factor), with {@code fit} retained but not yet distinguished from
 * tight in the layout pass.
 */
public class Flexible extends Expanded {

    private FlexFit fit = FlexFit.loose;

    public void fit(FlexFit v) {
        this.fit = v == null ? FlexFit.loose : v;
    }

    public FlexFit getFit() {
        return fit;
    }
}
