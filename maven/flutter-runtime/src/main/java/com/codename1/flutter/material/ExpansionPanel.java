package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.Color;
import com.codename1.flutter.Widget;

import dart.runtime.Funcs;

/**
 * One panel in an {@link ExpansionPanelList} — Flutter's {@code ExpansionPanel}.
 * A configuration object with a {@code headerBuilder(context, isExpanded)} and a
 * {@code body}.
 */
public class ExpansionPanel {

    private Funcs.Func2<BuildContext, Boolean, Widget> headerBuilder;
    private Widget body;
    private boolean isExpanded;

    public void headerBuilder(Funcs.Func2<BuildContext, Boolean, Widget> v) {
        this.headerBuilder = v;
    }

    public void body(Widget v) {
        this.body = v;
    }

    public void isExpanded(boolean v) {
        this.isExpanded = v;
    }

    public void canTapOnHeader(boolean v) {
    }

    public void backgroundColor(Color v) {
    }

    public Funcs.Func2<BuildContext, Boolean, Widget> getHeaderBuilder() {
        return headerBuilder;
    }

    public Widget getBody() {
        return body;
    }

    public boolean isExpanded() {
        return isExpanded;
    }
}
