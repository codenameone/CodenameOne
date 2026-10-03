/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.generated.flutter;

import com.codename1.flutter.*;
import com.codename1.flutter.widgets.*;
import com.codename1.flutter.material.*;
import com.codename1.flutter.navigation.MaterialPageRoute;
import com.codename1.flutter.navigation.Navigator;
import dart.core.DartList;
import dart.core.Duration;

/**
 * Compile-contract fixture for the M3 widget set (input widgets, Navigator/
 * routes, dialogs) in the exact transpiled style the Dart transpiler emits:
 * allocate + named-parameter setters, canonical positional statics, Funcs
 * SAM callbacks. Compilation of this class IS the contract check — it is
 * never instantiated by the tests.
 */
public class M3Showcase extends StatelessWidget {

    private TextEditingController _nameCtl = new TextEditingController();
    private boolean _agreed = false;
    private String _flavor = "vanilla";
    private double _volume = 0.5;
    private long _tab = 0L;

    public M3Showcase(Key key) {
        this.key(key);
    }

    // The exact statements from the M3 API contract.
    private void _contract(BuildContext context, SnackBar $someSnack) {
        var $t0 = new TextField();
        $t0.controller(this._nameCtl);
        var $t1 = new InputDecoration();
        $t1.labelText("Name");
        $t0.decoration($t1);
        $t0.onChanged((s) -> { /* ... */ });
        var $t2 = new Checkbox();
        $t2.value(this._agreed);
        $t2.onChanged((v) -> { /* ... */ });
        var $t3 = new MaterialPageRoute();
        $t3.builder((context1) -> new MyApp(null));
        Navigator.push(context, $t3);
        Navigator.pop(context);
        Dialogs.showDialog(context, (context2) -> {
            var $t4 = new AlertDialog();
            $t4.title(new Text("Hi"));
            $t4.actions(DartList.<Widget>of(new TextButton()));
            return $t4;
        });
        ScaffoldMessenger.of(context).showSnackBar($someSnack);
    }

    @Override
    public Widget build(BuildContext context) {
        // TextField with the full parameter surface
        var $t0 = new TextField();
        $t0.controller(this._nameCtl);
        var $t1 = new InputDecoration();
        $t1.labelText("Name");
        $t1.hintText("Your name");
        $t0.decoration($t1);
        $t0.obscureText(false);
        $t0.enabled(true);
        $t0.onChanged((s) -> this._nameCtl.text());
        $t0.onSubmitted((s) -> { /* ... */ });
        this._nameCtl.addListener(() -> { /* ... */ });
        this._nameCtl.setText("preset");
        this._nameCtl.clear();

        // Checkbox / Switch / Radio / Slider (controlled)
        var $t2 = new Checkbox();
        $t2.value(this._agreed);
        $t2.onChanged((v) -> { /* ... */ });
        var $t3 = new Switch();
        $t3.value(this._agreed);
        $t3.onChanged((v) -> { /* ... */ });
        var $t4 = new Radio();
        $t4.value("vanilla");
        $t4.groupValue(this._flavor);
        $t4.onChanged((v) -> { /* ... */ });
        var $t5 = new Slider();
        $t5.value(this._volume);
        $t5.min(0.0);
        $t5.max(10.0);
        $t5.divisions(20L);
        $t5.onChanged((v) -> { /* ... */ });

        // ListTile
        var $t6 = new ListTile();
        $t6.leading(new Icon(Icons.home));
        $t6.title(new Text("Home"));
        $t6.subtitle(new Text("Front page"));
        $t6.trailing(new Icon(Icons.add));
        $t6.onTap(() -> { /* ... */ });

        // BottomNavigationBar
        var $t7 = new BottomNavigationBarItem();
        $t7.icon(new Icon(Icons.home));
        $t7.label("Home");
        var $t8 = new BottomNavigationBarItem();
        $t8.icon(new Icon(Icons.settings));
        $t8.label("Settings");
        var $t9 = new BottomNavigationBar();
        $t9.items(DartList.<BottomNavigationBarItem>of($t7, $t8));
        $t9.currentIndex(this._tab);
        $t9.onTap((i) -> { /* ... */ });

        // SnackBar with a Duration
        var $t10 = new SnackBar();
        $t10.content(new Text("Saved"));
        $t10.duration(Duration.of(0L, 0L, 0L, 2L, 0L, 0L));

        // AlertDialog with the full parameter surface
        var $t11 = new AlertDialog();
        $t11.title(new Text("Hi"));
        $t11.content(new Text("Body"));
        var $t12 = new TextButton();
        $t12.onPressed(() -> Navigator.pop(null));
        $t12.child(new Text("OK"));
        $t11.actions(DartList.<Widget>of($t12));

        // Drawer + Scaffold with the new named parameters
        var $t13 = new Drawer();
        $t13.child(new Text("menu"));
        var $t14 = new Scaffold();
        var $t15 = new AppBar();
        $t15.title(new Text("M3 Showcase"));
        $t14.appBar($t15);
        var $t16 = new ListView();
        $t16.children(DartList.<Widget>of($t0, $t2, $t3, $t4, $t5, $t6));
        $t14.body($t16);
        $t14.drawer($t13);
        $t14.bottomNavigationBar($t9);
        return $t14;
    }
}
