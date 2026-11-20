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
import com.codenameone.examples.papercut.api.RunDto;
import com.codenameone.examples.papercut.api.RunResultDto;
import com.codenameone.examples.papercut.api.RunTicketDto;
import com.codenameone.examples.papercut.api.StartRunDto;
import com.codenameone.examples.papercut.engage.Ads;
import com.codenameone.examples.papercut.net.Api;
import com.codenameone.examples.papercut.net.Session;
import com.codenameone.examples.papercut.play.Arena;
import com.codenameone.examples.papercut.play.Hud;

/// A run from the server's side: a ticket before it, the result after it.
///
/// The ticket is what makes a submitted score mean anything. It is issued
/// for one mode, carries the daily seed when there is one, and can be spent
/// once; the server then holds the score to `ScoreBounds`. A guest, or a
/// player whose server does not answer, still plays -- the run is simply not
/// submitted.
// tag::papercut-run-flow[]
public final class Runs {
    private static final String[] MODES = {"CLASSIC", "FOLD", "DAILY"};

    private Runs() {
    }

    public static String modeName(int mode) {
        return MODES[mode];
    }

    public static void play(final int mode) {
        if (Session.user() == null) {
            start(mode, mode == Arena.DAILY ? HomeForm.dailySeed() : System.currentTimeMillis(), null, 0);
            return;
        }
        Api.runs().start(new StartRunDto(MODES[mode]), Net.to(ticket -> Ui.later(() -> withErasers(mode, ticket)),
                (status, message) -> Ui.later(() -> {
            Ui.toast("Playing offline: this run will not reach the boards.");
            start(mode, mode == Arena.DAILY ? HomeForm.dailySeed() : System.currentTimeMillis(), null, 0);
        })));
    }

    /// Spends the spare erasers bought in the shop on this run.
    private static void withErasers(final int mode, final RunTicketDto ticket) {
        final long seed = mode == Arena.DAILY ? ticket.seed() : System.currentTimeMillis();
        Api.shop().wallet(Net.to(wallet -> {
            final int spare = wallet.erasers();
            if (spare <= 0 || mode == Arena.FOLD) {
                Ui.later(() -> start(mode, seed, ticket, 0));
                return;
            }
            Api.shop().useErasers(Net.to(after -> Ui.later(() -> start(mode, seed, ticket, spare)),
                    (status, message) -> Ui.later(() -> start(mode, seed, ticket, 0))));
        }, (status, message) -> Ui.later(() -> start(mode, seed, ticket, 0))));
    }

    private static void start(int mode, long seed, final RunTicketDto ticket, int erasers) {
        PlayForm f = new PlayForm(mode, seed, (arena, form) -> finished(arena, form, ticket));
        f.getView().getArena().addErasers(erasers);
        f.show();
    }

    private static void finished(final Arena a, final PlayForm form, RunTicketDto ticket) {
        Ads.betweenRuns(Ui.adFree());
        if (ticket == null) {
            form.showOver(a, Session.user() == null ? "Sign in to put runs on the boards" : null);
            return;
        }
        int seconds = Math.max(1, (int) Math.ceil(a.time));
        RunDto run = new RunDto(ticket.token(), ticket.mode(), a.score, a.kills, a.gates, seconds,
                a.peakMultiplier);
        Api.runs().submit(run, Net.to(result -> Ui.later(() -> form.showOver(a, note(result))),
                (status, message) -> Ui.later(() -> form.showOver(a, "Not submitted: " + message))));
    }

    static String note(RunResultDto r) {
        if (!r.accepted()) {
            return r.message() == null ? "The server did not take this run" : r.message();
        }
        StringBuilder b = new StringBuilder();
        if (r.newBest()) {
            b.append("New best!  ");
        }
        if (r.rank() > 0) {
            b.append("#").append(r.rank()).append(" of all time  ");
        }
        if (r.coinsEarned() > 0) {
            b.append("+").append(r.coinsEarned()).append(" ink");
        }
        if (b.length() == 0) {
            b.append("Best ").append(Hud.format(r.best()));
        }
        return b.toString().trim();
    }
}
// end::papercut-run-flow[]
