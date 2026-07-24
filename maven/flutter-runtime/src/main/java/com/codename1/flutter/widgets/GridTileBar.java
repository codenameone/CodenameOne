package com.codename1.flutter.widgets;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * The header/footer band shown inside a {@link GridTile} — Flutter's
 * {@code GridTileBar}: an optional {@code leading} widget, a {@code title} and
 * {@code subtitle}, and a {@code trailing} widget over a translucent
 * {@code backgroundColor}. This pass lays the pieces out as a horizontal
 * {@link Row}; precise Material spacing is deferred.
 */
public class GridTileBar extends StatelessWidget {

    private Color backgroundColor;
    private Widget leading;
    private Widget title;
    private Widget subtitle;
    private Widget trailing;

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void leading(Widget v) {
        this.leading = v;
    }

    public void title(Widget v) {
        this.title = v;
    }

    public void subtitle(Widget v) {
        this.subtitle = v;
    }

    public void trailing(Widget v) {
        this.trailing = v;
    }

    public Widget getTitle() {
        return title;
    }

    @Override
    public Widget build(BuildContext context) {
        Column texts = new Column();
        dart.core.DartList<Widget> lines = new dart.core.DartList<Widget>();
        if (title != null) {
            lines.add(title);
        }
        if (subtitle != null) {
            lines.add(subtitle);
        }
        texts.children(lines);

        Row row = new Row();
        dart.core.DartList<Widget> kids = new dart.core.DartList<Widget>();
        if (leading != null) {
            kids.add(leading);
        }
        kids.add(texts);
        if (trailing != null) {
            kids.add(trailing);
        }
        row.children(kids);
        return row;
    }
}
