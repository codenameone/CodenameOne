package com.codename1.generated.flutter;

import com.codename1.flutter.material.AppBar;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.material.Card;
import com.codename1.flutter.widgets.Column;
import dart.core.DartList;
import dart.runtime.DartRuntime;
import com.codename1.flutter.material.Divider;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.material.ElevatedButton;
import com.codename1.flutter.widgets.Expanded;
import com.codename1.flutter.widgets.Icon;
import com.codename1.flutter.Icons;
import com.codename1.flutter.widgets.ListView;
import com.codename1.flutter.MainAxisAlignment;
import com.codename1.flutter.material.OutlinedButton;
import com.codename1.flutter.widgets.Padding;
import com.codename1.flutter.widgets.Row;
import com.codename1.flutter.material.Scaffold;
import com.codename1.flutter.widgets.SizedBox;
import com.codename1.flutter.State;
import com.codename1.flutter.widgets.Text;
import com.codename1.flutter.material.TextButton;
import com.codename1.flutter.Widget;

// Generated from main.dart — do not edit.
public class _DemoPageState extends State<DemoPage> {

    private final DartList<String> _items = DartList.<String>of("Alpha", "Beta", "Gamma");
    DartList<String> get$_items() {
        return _items;
    }

    private long _taps = 0L;
    long get$_taps() {
        return _taps;
    }
    void set$_taps(long v) {
        this._taps = v;
    }

    @Override
    public Widget build(BuildContext context) {
        var $t0 = new Scaffold();
        var $t1 = new AppBar();
        $t1.title(new Text("M2 Widgets"));
        $t0.appBar($t1);
        var $t2 = new Column();
        var $t3 = new Padding();
        $t3.padding(EdgeInsets.all(8.0));
        var $t4 = new Row();
        $t4.mainAxisAlignment(MainAxisAlignment.spaceEvenly);
        var $t5 = new ElevatedButton();
        $t5.onPressed(this::_addItem);
        $t5.child(new Text("Add"));
        var $t6 = new OutlinedButton();
        $t6.onPressed(this::_clear);
        $t6.child(new Text("Clear"));
        var $t7 = new TextButton();
        $t7.onPressed(() -> {
            this.setState(() -> {
                this._taps++;
            });
        });
        $t7.child(new Text("Taps: " + DartRuntime.str(this._taps)));
        $t4.children(DartList.<Widget>of($t5, $t6, $t7));
        $t3.child($t4);
        var $t8 = new Expanded();
        $t8.child(ListView.builder(null, this._items.length(), (context$0, index) -> {
            var $t9 = new Card();
            var $t10 = new Padding();
            $t10.padding(EdgeInsets.all(12.0));
            var $t11 = new Row();
            var $t12 = new SizedBox();
            $t12.width(8.0);
            var $t13 = new Expanded();
            $t13.child(new Text(this._items.idx(index)));
            $t11.children(DartList.<Widget>of(new Icon(Icons.favorite), $t12, $t13, new Text("#" + DartRuntime.str(index))));
            $t10.child($t11);
            $t9.child($t10);
            return $t9;
        }, null, null, null, null, null, null, null, null, null));
        $t2.children(DartList.<Widget>of($t3, new Divider(), $t8));
        $t0.body($t2);
        return $t0;
    }

    void _addItem() {
        this.setState(() -> {
            this._items.add("Item " + DartRuntime.str(this._items.length() + 1L));
        });
    }

    void _clear() {
        this.setState(() -> {
            this._items.clear();
            this._taps = 0L;
        });
    }

}
