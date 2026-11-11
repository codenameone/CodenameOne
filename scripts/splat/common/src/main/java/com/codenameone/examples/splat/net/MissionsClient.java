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
package com.codenameone.examples.splat.net;

import com.codename1.appkit.Net;
import com.codenameone.examples.splat.api.MissionDto;
import com.codenameone.examples.splat.rules.Mission;
import com.codenameone.examples.splat.rules.Mode;
import java.util.ArrayList;
import java.util.List;

/// The Mission Builder's calls to the server, in the editor's own terms: a
/// [Mission] goes up as its JSON and comes back as one.
public final class MissionsClient {
    /// One line of the published list.
    public static final class Row {
        public final String code;
        public final String name;
        public final String owner;
        public final String modeTitle;
        public final int plays;

        Row(MissionDto d) {
            code = d.code();
            name = d.name();
            owner = d.ownerName() == null ? "" : d.ownerName();
            String title = d.mode();
            try {
                title = Mode.byName(d.mode()).title;
            } catch (IllegalArgumentException unknown) {
                // shown as the server named it
            }
            modeTitle = title;
            plays = d.plays();
        }
    }

    public interface Got<T> {
        void got(T value);
    }

    private MissionsClient() {
    }

    /// Publishes; answers the server's code, or null when it was refused.
    public static void publish(Mission m, final Got<String> done) {
        MissionDto body = new MissionDto("", m.name, m.mode.name(), "", "", 0, 0L, m.toJson());
        Api.mission().publishMission(body, Net.to(d -> done.got(d.code()), (status, msg) -> done.got(null)));
    }

    public static void list(final Got<List<Row>> done) {
        Api.mission().missions(Net.to(rows -> {
            List<Row> out = new ArrayList<Row>();
            if (rows != null) {
                for (MissionDto d : rows) {
                    out.add(new Row(d));
                }
            }
            done.got(out);
        }, (status, msg) -> done.got(new ArrayList<Row>())));
    }

    /// The mission with that code, or null.
    public static void get(String code, final Got<Mission> done) {
        Api.mission().mission(code, Net.to(d -> {
            Mission m = null;
            try {
                m = Mission.fromJson(d.json());
                m.code = d.code();
            } catch (IllegalArgumentException bad) {
                m = null;
            }
            done.got(m);
        }, (status, msg) -> done.got(null)));
    }
}
