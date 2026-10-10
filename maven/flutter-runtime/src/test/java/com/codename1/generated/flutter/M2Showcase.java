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
import com.codename1.flutter.rendering.BoxConstraints;
import dart.core.DartList;

/**
 * Compile-contract fixture for the M2 widget set, written in the exact
 * transpiled style the Dart transpiler emits: allocate + named-parameter
 * setters, and canonical-positional static factories for Dart named
 * constructors (positional params first, then every named param in declared
 * order, missing args passed as null / boxed).
 */
public class M2Showcase extends StatelessWidget {
    public M2Showcase(Key key) { this.key(key); }

    private Widget _buildItem(BuildContext context, long index) {
        var $t0 = new Card();
        $t0.child(new Text("Item " + index));
        return $t0;
    }

    private void _onPressed() {
    }

    @Override
    public Widget build(BuildContext context) {
        // ListView.builder(itemCount: 20, itemBuilder: (c, i) -> ...)
        var $t0 = ListView.builder(null, 20L, (c, i) -> this._buildItem(c, i),
                null, null, null, null, null, null, null, null, null);

        // GridView.count(crossAxisCount: 2, children: ...)
        var $t1 = GridView.count(null, null, null, null, 2L, null, null, null, null,
                DartList.<Widget>of(new Text("a"), new Text("b"), new Text("c"), new Text("d")));

        // GridView.count with every named argument
        var $t2 = GridView.count(null, null, null, null, 3L, 1.5, 4.0, 4.0, EdgeInsets.all(8.0),
                DartList.<Widget>of(new Icon(Icons.home), new Icon(Icons.settings)));

        // Image.asset('logo.png', width: 100)
        var $t3 = Image.asset("logo.png", null, 100.0, null, null,
                null, null, null, null, null, null, null, null, null);
        var $t4 = Image.network("https://example.com/x.png", null, 64.0, 64.0, BoxFit.cover);

        // Stack + Positioned + Align
        var $t5 = new Stack();
        $t5.alignment(Alignment.bottomRight);
        var $t6 = new Positioned();
        $t6.left(8.0);
        $t6.top(8.0);
        $t6.width(40.0);
        $t6.height(40.0);
        $t6.child($t3);
        var $t7 = new Positioned();
        $t7.right(0.0);
        $t7.bottom(0.0);
        $t7.child(new Icon(Icons.add));
        var $t8 = new Align();
        $t8.alignment(Alignment.topCenter);
        $t8.child(new Text("aligned"));
        $t5.children(DartList.<Widget>of($t4, $t6, $t7, $t8));

        // ConstrainedBox + BoxConstraints
        var $t9 = new BoxConstraints();
        $t9.minWidth(100.0);
        $t9.maxWidth(200.0);
        $t9.minHeight(0.0);
        $t9.maxHeight(80.0);
        var $t10 = new ConstrainedBox();
        $t10.constraints($t9);
        $t10.child($t5);

        // Card / Divider
        var $t11 = new Card();
        $t11.color(Colors.white);
        $t11.elevation(2.0);
        $t11.margin(EdgeInsets.all(6.0));
        $t11.child($t10);
        var $t12 = new Divider();
        $t12.height(24.0);
        $t12.thickness(2.0);
        $t12.color(Colors.grey);

        // all four buttons
        var $t13 = new ElevatedButton();
        $t13.onPressed(() -> {
        });
        $t13.child(new Text("Elevated"));
        var $t14 = new TextButton();
        $t14.onPressed(null);
        $t14.child(new Text("Disabled"));
        var $t15 = new OutlinedButton();
        $t15.onPressed(this::_onPressed);
        $t15.child(new Text("Outlined"));
        var $t16 = new IconButton();
        $t16.onPressed(this::_onPressed);
        $t16.icon(new Icon(Icons.share));
        $t16.iconSize(32.0);
        $t16.color(Colors.red);

        // GestureDetector / InkWell
        var $t17 = new GestureDetector();
        $t17.onTap(this::_onPressed);
        $t17.onLongPress(() -> {
        });
        $t17.child($t11);
        var $t18 = new InkWell();
        $t18.onTap(this::_onPressed);
        $t18.child(new Text("ink"));

        // ListView children mode + SingleChildScrollView
        var $t19 = new ListView();
        $t19.shrinkWrap(true);
        $t19.padding(EdgeInsets.symmetric(8.0, 4.0));
        $t19.children(DartList.<Widget>of($t12, $t13, $t14, $t15, $t16, $t17, $t18, $t1, $t2, $t0));
        var $t20 = new SingleChildScrollView();
        $t20.padding(EdgeInsets.all(16.0));
        $t20.child($t19);

        var $t21 = new Scaffold();
        var $t22 = new AppBar();
        $t22.title(new Text("M2 Showcase"));
        $t21.appBar($t22);
        $t21.body($t20);
        return $t21;
    }
}
