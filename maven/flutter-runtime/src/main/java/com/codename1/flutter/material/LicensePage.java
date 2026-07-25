package com.codename1.flutter.material;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;

/**
 * The Material page listing the open-source licenses of the app's packages —
 * Flutter's {@code LicensePage}. Signature-only: the application metadata is
 * captured; no license registry is enumerated this pass.
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

    /**
     * Dart's top-level {@code showLicensePage(...)}: pushes a license page.
     * Deferred — records nothing and returns.
     */
    public static void show(BuildContext context, String applicationName, String applicationVersion,
            Widget applicationIcon, String applicationLegalese, Boolean useRootNavigator) {
    }

    @Override
    public Widget build(BuildContext context) {
        com.codename1.flutter.FlutterErrorReport.unimplemented("LicensePage", "renders nothing");
        return null;
    }
}
