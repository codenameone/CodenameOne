package com.codename1.generated.flutter;

import com.codename1.flutter.material.AppBar;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.widgets.Center;
import com.codename1.flutter.widgets.Column;
import dart.core.DartList;
import dart.runtime.DartRuntime;
import com.codename1.flutter.material.FloatingActionButton;
import com.codename1.flutter.widgets.Icon;
import com.codename1.flutter.Icons;
import com.codename1.flutter.MainAxisAlignment;
import com.codename1.flutter.material.Scaffold;
import com.codename1.flutter.State;
import com.codename1.flutter.widgets.Text;
import com.codename1.flutter.material.Theme;
import com.codename1.flutter.Widget;

// Generated from main.dart — do not edit.
public class _MyHomePageState extends State<MyHomePage> {

    private long _counter = 0L;
    long get$_counter() {
        return _counter;
    }
    void set$_counter(long v) {
        this._counter = v;
    }

    void _incrementCounter() {
        this.setState(() -> {
            this._counter++;
        });
    }

    @Override
    public Widget build(BuildContext context) {
        var $t0 = new Scaffold();
        var $t1 = new AppBar();
        $t1.backgroundColor(Theme.of(context).colorScheme().inversePrimary());
        $t1.title(new Text(this.widget().get$title()));
        $t0.appBar($t1);
        var $t2 = new Center();
        var $t3 = new Column();
        $t3.mainAxisAlignment(MainAxisAlignment.center);
        var $t4 = new Text(DartRuntime.str(this._counter));
        $t4.style(Theme.of(context).textTheme().headlineMedium());
        $t3.children(DartList.<Widget>of(new Text("You have pushed the button this many times:"), $t4));
        $t2.child($t3);
        $t0.body($t2);
        var $t5 = new FloatingActionButton();
        $t5.onPressed(this::_incrementCounter);
        $t5.tooltip("Increment");
        $t5.child(new Icon(Icons.add));
        $t0.floatingActionButton($t5);
        return $t0;
    }

}
