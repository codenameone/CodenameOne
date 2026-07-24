package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.Scaffold;

/**
 * The basic iOS page layout — Flutter's {@code CupertinoPageScaffold}: an
 * optional navigation bar above a full-bleed body. Composed onto the material
 * {@link Scaffold} (its app-bar slot renders the navigation bar as the top
 * strip), which is faithful to the structure.
 */
public class CupertinoPageScaffold extends StatelessWidget {

    private Widget navigationBar;
    private Color backgroundColor;
    private Widget child;

    public void navigationBar(Widget v) {
        this.navigationBar = v;
    }

    public void backgroundColor(Color v) {
        this.backgroundColor = v;
    }

    public void resizeToAvoidBottomInset(boolean v) {
    }

    public void child(Widget v) {
        this.child = v;
    }

    @Override
    public Widget build(BuildContext context) {
        Scaffold s = new Scaffold();
        if (navigationBar != null) {
            s.appBar(navigationBar);
        }
        s.body(child);
        return s;
    }
}
