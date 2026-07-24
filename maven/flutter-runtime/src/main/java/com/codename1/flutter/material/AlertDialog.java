package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.Widget;

import dart.core.DartList;

/**
 * A material alert dialog body: a title (rendered bold when it is a plain
 * {@code Text}), optional content and a right-aligned actions row. Shown via
 * {@link Dialogs#showDialog}; the CN1 Dialog supplies the surface chrome, so
 * this widget is pure layout.
 */
public class AlertDialog extends Widget {

    private Widget title;
    private Widget content;
    private DartList<Widget> actions;
    private com.codename1.flutter.ShapeBorder shape;
    private com.codename1.flutter.Color backgroundColor;

    public void shape(com.codename1.flutter.ShapeBorder v) {
        this.shape = v;
    }

    public void backgroundColor(com.codename1.flutter.Color v) {
        this.backgroundColor = v;
    }

    public void title(Widget v) {
        this.title = v;
    }

    public void content(Widget v) {
        this.content = v;
    }

    public void actions(DartList<Widget> v) {
        this.actions = v;
    }

    public Widget getTitle() {
        return title;
    }

    public Widget getContent() {
        return content;
    }

    public DartList<Widget> getActions() {
        return actions;
    }

    @Override
    public Element createElement() {
        return new AlertDialogRenderElement(this);
    }
}
