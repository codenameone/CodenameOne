package com.codename1.generated.flutter;

import com.codename1.flutter.Key;
import com.codename1.flutter.State;
import com.codename1.flutter.StatefulWidget;

// Generated from main.dart -- do not edit.
public class MyHomePage extends StatefulWidget implements dart.runtime.ConstCanon.Keyed {

    private String title;
    public String get$title() {
        return title;
    }

    public MyHomePage(Key key, String title) {
        this.key(key);
        this.title = title;
    }

    @Override
    public State<MyHomePage> createState() {
        //@dart:29 main.dart
        return new _MyHomePageState();
    }

    public Object[] $constKey() {
        return dart.runtime.ConstCanon.join(super.$constKey(), new Object[] {this.title});
    }

    static final String $cn1DartLines = "main.dart\n23:29";
}
