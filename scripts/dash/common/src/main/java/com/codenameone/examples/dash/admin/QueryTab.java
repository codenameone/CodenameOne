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
package com.codenameone.examples.dash.admin;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.dash.api.ConditionDto;
import com.codenameone.examples.dash.api.PlayerDto;
import com.codenameone.examples.dash.api.QueryDto;
import com.codenameone.examples.dash.api.QueryResultDto;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;
import com.codenameone.examples.dash.ui.Ui;
import java.util.Arrays;

/// The query tool: which players meet a set of conditions, how many, and
/// who they are. The same conditions make a pipeline's audience, so an
/// operator tries a query here first, then turns it into a pipeline.
// tag::dash-query-tab[]
final class QueryTab {
    private final Container page = Desk.page();
    /// Starts as "never bought anything": the players a first offer is for.
    private final Conditions conditions = new Conditions("query", Arrays.asList(
            new ConditionDto("purchases", "=", 0)));
    private final Container results = new Container(BoxLayout.y());
    private final AdminConsole console;

    QueryTab(AdminConsole console) {
        this.console = console;
        page.setName("query");
        page.add(Desk.section("Find players"));
        Container panel = Desk.panel();
        panel.add(conditions.component());
        panel.add(Desk.actions(
                Desk.button("Make a pipeline", "queryToPipeline", e -> console.newPipeline(conditions.value())),
                run()));
        page.add(panel);
        page.add(Desk.help("Days away counts from the last time the player opened the game. Purchases are store "
                + "purchases; friends invited are players who joined through this player's link."));
        page.add(results);
    }

    private Button run() {
        Button b = Desk.primary("Run query", "queryRun", e -> Api.admin().query(
                new QueryDto(conditions.value()), Net.to(r -> Ui.later(() -> show(r)), Ui::failed)));
        FontImage.setMaterialIcon(b, FontImage.MATERIAL_SEARCH);
        return b;
    }

    Container page() {
        return page;
    }

    void refresh() {
        // The query runs when asked: it reads every player.
    }

    private void show(QueryResultDto r) {
        results.removeAll();
        com.codename1.ui.Label count = Desk.section(r.count() == 1 ? "1 player" : r.count() + " players");
        count.setName("queryCount");
        results.add(count);
        if (r.count() > r.players().size()) {
            results.add(Desk.help("The first " + r.players().size() + ", longest away first."));
        }
        results.add(Desk.tableRow(true, "Player", "Trophies", "Battles", "Gems"));
        for (final PlayerDto p : r.players()) {
            Container row = Desk.tableRow(false, "", String.valueOf(p.trophies()),
                    String.valueOf(p.wins() + p.losses() + p.draws()), String.valueOf(p.gems()));
            // The name is a link to everything the console knows about them.
            Button name = new Button(p.displayName(), "DeskLink");
            name.setName("queryPlayer-" + p.username());
            name.addActionListener(e -> console.openPlayer(p.username()));
            row.replace(row.getComponentAt(0), name, null);
            results.add(row);
        }
        page.revalidate();
    }
}
// end::dash-query-tab[]
