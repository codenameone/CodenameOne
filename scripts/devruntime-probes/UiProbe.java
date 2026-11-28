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
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.list.*;
public class UiProbe {
    public static void main(String[] a) {
        Form f = new Form("Ui", BoxLayout.y());
        final Label out = new Label("idle");
        Button b = new Button("press");
        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { out.setText("pressed"); }
        });
        f.add(out).add(b);
        DefaultListModel<String> model = new DefaultListModel<String>(new String[]{"one","two"});
        f.add(new com.codename1.ui.List<String>(model));
        f.getToolbar().addCommandToRightBar("Cmd", null, e -> out.setText("cmd"));
        f.show();
        b.pressed(); b.released();
        System.out.println("PROBE UiProbe: after=" + out.getText() + " model=" + model.getSize()
            + " title=" + f.getTitle());
    }
}
