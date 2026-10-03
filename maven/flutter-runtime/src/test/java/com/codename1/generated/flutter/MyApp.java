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

public class MyApp extends StatelessWidget {
    public MyApp(Key key) { this.key(key); }
    @Override
    public Widget build(BuildContext context) {
        var $t0 = new MaterialApp();
        $t0.title("Flutter Demo");
        var $t1 = new ThemeData();
        $t1.colorScheme(ColorScheme.fromSeed(Colors.deepPurple));
        $t1.useMaterial3(true);
        $t0.theme($t1);
        $t0.home(new MyHomePage(null, "Flutter Demo Home Page"));
        return $t0;
    }
}
