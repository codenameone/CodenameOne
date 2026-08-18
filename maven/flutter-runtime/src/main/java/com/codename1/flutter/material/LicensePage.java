package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * The Material page listing the open-source licenses of the app's packages —
 * Flutter's {@code LicensePage}.
 *
 * <p>It rendered nothing at all, so "View licenses" led to a blank screen. There is no
 * package license registry to enumerate here — that is a Dart-tooling artifact — so the page
 * shows the application's own identity and legalese, which is the part an app actually
 * supplies.
 */
public class LicensePage extends StatelessWidget {

    private String applicationName;
    private String applicationVersion;
    private Widget applicationIcon;
    private String applicationLegalese;

    public void applicationName(String v) { this.applicationName = v; }
    public void applicationVersion(String v) { this.applicationVersion = v; }
    public void applicationIcon(Widget v) { this.applicationIcon = v; }
    public void applicationLegalese(String v) { this.applicationLegalese = v; }

    /** Dart's top-level {@code showLicensePage(...)}: pushes a license page. */
    public static void show(BuildContext context, String applicationName, String applicationVersion,
            Widget applicationIcon, String applicationLegalese, Boolean useRootNavigator) {
        final LicensePage page = new LicensePage();
        page.applicationName(applicationName);
        page.applicationVersion(applicationVersion);
        page.applicationIcon(applicationIcon);
        page.applicationLegalese(applicationLegalese);
        com.codename1.flutter.navigation.MaterialPageRoute<Object> route =
                new com.codename1.flutter.navigation.MaterialPageRoute<Object>();
        route.builder(new dart.runtime.Funcs.Func1<BuildContext, Widget>() {
            @Override
            public Widget call(BuildContext routeContext) {
                return page;
            }
        });
        com.codename1.flutter.navigation.Navigator.push(context, route);
    }

    @Override
    public Widget build(BuildContext context) {
        dart.core.DartList<Widget> rows = new dart.core.DartList<Widget>();
        if (applicationIcon != null) {
            rows.add(applicationIcon);
        }
        if (applicationName != null) {
            rows.add(new com.codename1.flutter.widgets.Text(applicationName));
        }
        if (applicationVersion != null) {
            rows.add(new com.codename1.flutter.widgets.Text(applicationVersion));
        }
        if (applicationLegalese != null) {
            rows.add(new com.codename1.flutter.widgets.Text(applicationLegalese));
        }
        com.codename1.flutter.widgets.Column body = new com.codename1.flutter.widgets.Column();
        body.children(rows);

        com.codename1.flutter.widgets.Padding padded =
                new com.codename1.flutter.widgets.Padding();
        padded.padding(com.codename1.flutter.EdgeInsets.all(24));
        padded.child(body);

        AppBar bar = new AppBar();
        bar.title(new com.codename1.flutter.widgets.Text("Licenses"));
        Scaffold page = new Scaffold();
        page.appBar(bar);
        page.body(padded);
        return page;
    }
}
