/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.papercut.ui;

import com.codename1.appkit.Net;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.papercut.api.FriendAddDto;
import com.codenameone.examples.papercut.api.FriendDto;
import com.codenameone.examples.papercut.net.Api;
import com.codenameone.examples.papercut.play.Hud;
import java.util.List;

/// The people on the Friends board. Adding someone is one way: they appear on
/// your board, and you on theirs only if they add you back.
public final class FriendsForm {
    private FriendsForm() {
    }

    public static void show() {
        Form f = Ui.form("Friends", "friends");
        Ui.desktopColumn(f);
        Ui.back(f, Nav::home);
        final Container list = new Container(BoxLayout.y());
        final TextField name = new TextField("", "Name or username", 20, TextField.ANY);
        name.setName("friendName");
        f.add(Ui.panel(name, Ui.primary("Add", "addFriend", e -> Api.friends().add(
                new FriendAddDto(name.getText().trim()), Net.to(rows -> Ui.later(() -> {
                    name.setText("");
                    fill(list, rows);
                }), Ui::failed)))));
        f.add(list);
        Api.friends().friends(Net.to(rows -> Ui.later(() -> fill(list, rows)), Ui::failed));
        f.show();
    }

    static void fill(final Container list, List<FriendDto> rows) {
        list.removeAll();
        if (rows.isEmpty()) {
            list.add(new Label("No friends on your board yet.", "Muted"));
        }
        for (final FriendDto fr : rows) {
            Container row = new Container(new BorderLayout(), "Row");
            row.add(BorderLayout.CENTER, BoxLayout.encloseY(new Label(fr.displayName()),
                    new Label("best " + Hud.format(fr.bestClassic()), "Muted")));
            row.add(BorderLayout.EAST, Ui.button("Remove", "Plain", "remove-" + fr.username(),
                    e -> Api.friends().remove(new FriendAddDto(fr.username()),
                            Net.to(after -> Ui.later(() -> fill(list, after)), Ui::failed))));
            list.add(row);
        }
        list.revalidate();
    }
}
