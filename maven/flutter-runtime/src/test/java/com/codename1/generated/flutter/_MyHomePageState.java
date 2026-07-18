package com.codename1.generated.flutter;

import com.codename1.flutter.*;
import com.codename1.flutter.widgets.*;
import com.codename1.flutter.material.*;
import dart.core.DartList;
import dart.runtime.DartRuntime;

public class _MyHomePageState extends State<MyHomePage> {
    private long _counter = 0L;

    private void _incrementCounter() {
        this.setState(() -> {
            this._counter = this._counter + 1L;
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
        var $t4 = new Text("You have pushed the button this many times:");
        var $t5 = new Text(DartRuntime.str(this._counter));
        $t5.style(Theme.of(context).textTheme().headlineMedium());
        $t3.children(DartList.of($t4, $t5));
        $t2.child($t3);
        $t0.body($t2);
        var $t6 = new FloatingActionButton();
        $t6.onPressed(this::_incrementCounter);
        $t6.tooltip("Increment");
        $t6.child(new Icon(Icons.add));
        $t0.floatingActionButton($t6);
        return $t0;
    }
}
