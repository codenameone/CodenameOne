package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * A single tile of a Material grid — Flutter's {@code GridTile}. An optional
 * {@code header}/{@code footer} band (typically a {@link GridTileBar}) overlays the main
 * {@code child}, pinned to the top and bottom edges.
 *
 * <p>Both bands were dropped before, so every tile in the grid demo lost its caption.
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
        if (header == null && footer == null) {
            return child;
        }
        dart.core.DartList<Widget> layers = new dart.core.DartList<Widget>();
        if (child != null) {
            layers.add(Positioned.fill(null, null, null, null, null, child));
        }
        if (header != null) {
            Positioned p = new Positioned();
            p.top(0);
            p.left(0);
            p.right(0);
            p.child(header);
            layers.add(p);
        }
        if (footer != null) {
            Positioned p = new Positioned();
            p.bottom(0);
            p.left(0);
            p.right(0);
            p.child(footer);
            layers.add(p);
        }
        Stack stack = new Stack();
        stack.children(layers);
        return stack;
    }
}
