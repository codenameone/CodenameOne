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
        Object[] s = super.$constKey();
        Object[] o = new Object[] {this.title};
        Object[] r = new Object[s.length + o.length];
        System.arraycopy(s, 0, r, 0, s.length);
        System.arraycopy(o, 0, r, s.length, o.length);
        return r;
    }

    static final String $cn1DartLines = "main.dart\n23:29";
}
