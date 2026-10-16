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
