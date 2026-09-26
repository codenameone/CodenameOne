package com.codename1.generated.flutter;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.material.ColorScheme;
import com.codename1.flutter.Colors;
import com.codename1.flutter.Key;
import com.codename1.flutter.material.MaterialApp;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.material.ThemeData;
import com.codename1.flutter.Widget;

// Generated from main.dart — do not edit.
public class DemoApp extends StatelessWidget {

    public DemoApp(Key key) {
        this.key(key);
    }

    @Override
    public Widget build(BuildContext context) {
        var $t0 = new MaterialApp();
        $t0.title("M2 Demo");
        var $t1 = new ThemeData();
        $t1.colorScheme(ColorScheme.fromSeed(Colors.blue, null));
        $t1.useMaterial3(true);
        $t0.theme($t1);
        $t0.home(new DemoPage(null));
        return $t0;
    }

}
