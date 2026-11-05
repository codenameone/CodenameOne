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
package com.codenameone.examples.splat.ui;

import com.codenameone.examples.splat.api.ReplayDto;
import com.codenameone.examples.splat.net.Api;
import com.codenameone.examples.splat.net.Net;
import com.codenameone.examples.splat.net.Session;
import com.codenameone.examples.splat.rules.Replay;
import com.codenameone.examples.splat.rules.RulesVersion;
import java.util.List;

/// A finished match, simulated again on the phone from its seed and the human
/// inputs. The server sends no video and no positions: the file is a few
/// kilobytes, and the bots replay their own decisions because they think inside
/// the rules.
// tag::splat-replay-form[]
public final class ReplayForm {
    private ReplayForm() {
    }

    /// Fetches match `id` and plays it, following the signed-in player.
    public static void show(String id, final Runnable back) {
        Api.game().replay(id, Net.to(r -> Ui.later(() -> open(r, back)), Ui::failed));
    }

    public static void load(String id) {
        show(id, () -> {
            if (Session.user() == null) {
                SignInForm.show();
            } else {
                HistoryForm.show();
            }
        });
    }

    public static void open(ReplayDto r, Runnable back) {
        Replay replay;
        try {
            replay = Replay.decode(r.replay());
        } catch (RuntimeException bad) {
            Ui.toast("This replay could not be read.");
            return;
        }
        if (replay.rulesVersion() != RulesVersion.ID) {
            Ui.toast("This match was played on older rules and can no longer be replayed.");
            return;
        }
        MatchForm.replay(replay, follow(r.names()), back == null ? HistoryForm::show : back);
    }

    private static int follow(List<String> names) {
        if (names == null || Session.user() == null) {
            return 0;
        }
        for (int i = 0; i < names.size(); i++) {
            if (Session.user().displayName().equals(names.get(i))) {
                return i;
            }
        }
        return 0;
    }
}
// end::splat-replay-form[]
