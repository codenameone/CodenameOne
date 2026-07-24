package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * A single tile of a Material grid — Flutter's {@code GridTile}. An optional
 * {@code header}/{@code footer} band (typically a {@link GridTileBar}) overlays
 * the main {@code child}. This pass renders the {@code child}; overlaying the
 * header/footer via a Stack is deferred.
 */
public class GridTile extends StatelessWidget {

    private Widget header;
    private Widget footer;
    private Widget child;

    public void header(Widget v) {
        this.header = v;
    }

    public void footer(Widget v) {
        this.footer = v;
    }

    public void child(Widget v) {
        this.child = v;
    }

    public Widget getHeader() {
        return header;
    }

    public Widget getFooter() {
        return footer;
    }

    public Widget getChild() {
        return child;
    }

    @Override
    public Widget build(BuildContext context) {
        return child;
    }
}
