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
package com.codenameone.examples.brickyard.ui;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.brickyard.api.AccountDto;
import com.codenameone.examples.brickyard.api.ExperienceDto;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;
import com.codenameone.examples.brickyard.net.Session;
import com.codenameone.examples.brickyard.studio.Studio;

import java.util.ArrayList;
import java.util.List;

/// Create: start a new experience, or open one of your own in the studio.
public final class CreateForm {
    static final String CREATOR = "creator";

    private CreateForm() {
    }

    public static void show() {
        final Form f = Ui.section("Create", "create", 1);
        final Container content = Ui.content(f);
        AccountDto me = Session.user();
        if (me != null && me.roles() != null && !me.roles().contains(CREATOR)) {
            content.add(Ui.text("Turn on creator tools to publish experiences of your own.", "Body"));
            content.add(Ui.primary("Become a creator", "becomeCreatorButton",
                    e -> Api.account().becomeCreator(Net.to(updated -> {
                        Session.setUser(updated);
                        show();
                    }, Ui::failed))));
            f.show();
            return;
        }
        content.add(Ui.primary("New experience", "newExperienceButton", e -> Studio.open(null)));
        content.add(Ui.label("Your experiences", "RowTitle"));
        final Container list = new Container(BoxLayout.y());
        list.setName("myExperiences");
        content.add(list);
        f.show();
        // The server's list of the caller's own experiences, drafts included.
        Api.experiences().mine(Net.to(found -> {
            List<ExperienceDto> mine = found == null ? new ArrayList<ExperienceDto>() : found;
            if (mine.isEmpty()) {
                list.add(Ui.label("Nothing yet.", "Muted"));
            }
            for (final ExperienceDto x : mine) {
                list.add(Ui.panel(Ui.label(x.title(), "CardTitle"),
                        Ui.label(x.publishedVersion() > 0 ? "Published, version " + x.publishedVersion()
                                : "Draft", "CardMeta"),
                        Ui.secondary("Edit", "edit" + x.id(), e -> Studio.open(x.id())),
                        Ui.link("Dashboard", "dashboard" + x.id(), e -> DashboardForm.show(x))));
            }
            f.revalidate();
        }, Ui::failed));
    }

    /// The experiences `owner` made, out of a list of anyone's.
    static List<ExperienceDto> mine(List<ExperienceDto> all, String owner) {
        List<ExperienceDto> out = new ArrayList<>();
        if (all == null || owner == null) {
            return out;
        }
        for (ExperienceDto e : all) {
            if (owner.equals(e.owner())) {
                out.add(e);
            }
        }
        return out;
    }
}
