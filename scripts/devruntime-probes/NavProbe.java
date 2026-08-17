/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
import com.codename1.ui.*;
import com.codename1.ui.events.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.plaf.*;
public class NavProbe {
    static Form home;
    static Form detail(String item) {
        Form f = new Form(item, BoxLayout.y());
        f.add(new Label("detail for " + item));
        f.getToolbar().setBackCommand("Back", e -> home.showBack());
        return f;
    }
    public static void main(String[] a) {
        home = new Form("Items", BoxLayout.y());
        for (final String s : new String[]{"alpha","beta"}) {
            Button b = new Button(s);
            b.addActionListener(e -> detail(s).show());
            home.add(b);
        }
        home.add(new Label(UIManager.getInstance().getThemeConstant("x", "themed-ok")));
        home.show();
        System.out.println("PROBE NavProbe: kids=" + home.getContentPane().getComponentCount()
            + " current=" + Display.getInstance().getCurrent().getTitle());
    }
}
