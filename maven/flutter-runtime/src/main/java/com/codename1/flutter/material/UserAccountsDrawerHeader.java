package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.CrossAxisAlignment;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.widgets.Column;

import dart.core.DartList;

/**
 * A Material drawer header showing the signed-in account — Flutter's
 * {@code UserAccountsDrawerHeader}. This pass renders the account picture, name
 * and email stacked in a {@link Column}; the themed background, details arrow and
 * other-account switching are captured for a later render pass.
 */
public class UserAccountsDrawerHeader extends StatelessWidget {

    private Object decoration;
    private Object margin;
    private Widget currentAccountPicture;
    private DartList<Widget> otherAccountsPictures;
    private Widget accountName;
    private Widget accountEmail;
    private Object onDetailsPressed;
    private Object arrowColor;

    public void decoration(Object v) { this.decoration = v; }
    public void margin(Object v) { this.margin = v; }
    public void currentAccountPicture(Widget v) { this.currentAccountPicture = v; }
    public void otherAccountsPictures(DartList<Widget> v) { this.otherAccountsPictures = v; }
    public void accountName(Widget v) { this.accountName = v; }
    public void accountEmail(Widget v) { this.accountEmail = v; }
    public void onDetailsPressed(Object v) { this.onDetailsPressed = v; }
    public void arrowColor(Object v) { this.arrowColor = v; }

    @Override
    public Widget build(BuildContext context) {
        DartList<Widget> children = new DartList<Widget>();
        if (currentAccountPicture != null) {
            children.add(currentAccountPicture);
        }
        if (accountName != null) {
            children.add(accountName);
        }
        if (accountEmail != null) {
            children.add(accountEmail);
        }
        Column col = new Column();
        col.crossAxisAlignment(CrossAxisAlignment.start);
        col.children(children);
        return col;
    }
}
