package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.MainAxisSize;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;

import dart.core.DartList;
import dart.runtime.Funcs;

/**
 * A single-line {@link ListTile} that expands to reveal children — Flutter's
 * {@code ExpansionTile}. This milestone renders the {@code title} followed by
 * the {@code children}; the expand/collapse toggle that fires
 * {@code onExpansionChanged} is deferred (children render expanded).
 */
public class ExpansionTile extends StatelessWidget {

    private Widget title;
    private Widget subtitle;
    private Widget leading;
    private Widget trailing;
    private DartList<Widget> children;
    private boolean initiallyExpanded;
    private Funcs.VoidFunc1<Boolean> onExpansionChanged;

    public void title(Widget v) {
        this.title = v;
    }

    public void subtitle(Widget v) {
        this.subtitle = v;
    }

    public void leading(Widget v) {
        this.leading = v;
    }

    public void trailing(Widget v) {
        this.trailing = v;
    }

    public void children(DartList<Widget> v) {
        this.children = v;
    }

    public void initiallyExpanded(boolean v) {
        this.initiallyExpanded = v;
    }

    public void onExpansionChanged(Funcs.VoidFunc1<Boolean> v) {
        this.onExpansionChanged = v;
    }

    public void childrenPadding(Object v) {
    }

    public void backgroundColor(Color v) {
    }

    public void collapsedBackgroundColor(Color v) {
    }

    public void textColor(Color v) {
    }

    public void iconColor(Color v) {
    }

    public void tilePadding(Object v) {
    }

    public void expandedAlignment(Object v) {
    }

    public void expandedCrossAxisAlignment(Object v) {
    }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> kids = new DartList<Widget>();
        ListTile header = new ListTile();
        if (leading != null) {
            header.leading(leading);
        }
        if (title != null) {
            header.title(title);
        }
        if (subtitle != null) {
            header.subtitle(subtitle);
        }
        if (trailing != null) {
            header.trailing(trailing);
        }
        kids.add(header);
        if (children != null) {
            for (int i = 0; i < children.size(); i++) {
                kids.add(children.get(i));
            }
        }
        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.stretch);
        col.mainAxisSize(MainAxisSize.min);
        col.children(kids);
        return col;
    }
}
