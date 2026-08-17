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
package com.codename1.samples;

import com.codename1.annotations.AppIntent;
import com.codename1.annotations.EntityId;
import com.codename1.annotations.EntityQuery;
import com.codename1.annotations.EntitySubtitle;
import com.codename1.annotations.EntityTitle;
import com.codename1.annotations.IntentEntity;
import com.codename1.annotations.IntentParam;
import com.codename1.annotations.Route;
import com.codename1.intents.AppEntity;
import com.codename1.intents.IntentContext;
import com.codename1.intents.IntentResult;
import com.codename1.intents.Intents;
import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Toolbar;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Exercises com.codename1.intents end to end: a headless intent that answers without the app
 * appearing, an intent that opens a route instead, an entity the platform can disambiguate, and
 * content published to device search.
 *
 * Run it in the simulator and open Simulate -&gt; App Intents: the window lists what this class
 * declares, because it reads the same generated table a device does.
 */
public class AppIntentsSample {

    private Form current;
    private Resources theme;

    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");
        Toolbar.setGlobalToolbar(true);
        Log.bindCrashProtection(true);

        // Registered in init() rather than in start(), so a search-result tap that cold-started
        // the process is delivered as soon as there is somewhere to deliver it to. The framework
        // queues anything that arrived first.
        Intents.setSelectionHandler(new com.codename1.intents.EntitySelectionHandler() {
            public void onEntitySelected(AppEntity entity) {
                com.codename1.router.Navigation.navigate("/workouts/" + entity.getId());
            }
        });
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        Form hi = new Form("App Intents", BoxLayout.y());

        hi.add(new Label("Total logged: " + totalMinutes() + " minutes"));
        hi.add(new Label(Intents.isVoiceInvocationSupported()
                ? "Ask Siri to log a workout."
                : "Voice invocation is iOS-only; use the launcher shortcut."));

        Button log = new Button("Log 20 minute run");
        log.addActionListener(e -> {
            logWorkout("run", 20);
            // Donate what the user just did by hand -- that is the signal the system learns
            // from. Donating on every intent invocation would only teach it that the user uses
            // shortcuts.
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("kind", "run");
            params.put("minutes", Integer.valueOf(20));
            Intents.donate("log_workout", params);
            publishRecent();
            start();
        });
        hi.add(log);

        Button index = new Button("Publish workouts to device search");
        index.addActionListener(e -> Display.getInstance().startThread(new Runnable() {
            public void run() {
                // Off the EDT on purpose: indexing writes through to the platform and encodes
                // any thumbnails on the way.
                publishRecent();
            }
        }, "index").start());
        hi.add(index);

        hi.show();
        current = hi;
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
    }

    public void destroy() {
    }

    // ------------------------------------------------------------------
    // The declarations
    // ------------------------------------------------------------------

    /**
     * Answers without the app ever appearing. Storage is fine here; a Form would not be.
     */
    @AppIntent(value = "log_workout", title = "Log a workout",
            description = "Records a completed workout",
            phrases = {"Log a workout in ${applicationName}",
                       "Log a ${minutes} minute ${kind} in ${applicationName}"},
            headless = true, timeoutSeconds = 5)
    public static IntentResult logWorkout(
            @IntentParam(value = "kind", title = "What kind of workout?",
                         options = {"run", "ride", "swim"}) String kind,
            @IntentParam(value = "minutes", title = "How many minutes?") int minutes) {
        int total = Preferences.get("totalMinutes", 0) + minutes;
        Preferences.set("totalMinutes", total);
        Preferences.set("lastKind", kind);
        return IntentResult.value(String.valueOf(total))
                .withDialog("Logged a " + minutes + " minute " + kind
                        + ". That is " + total + " minutes this week.");
    }

    /**
     * Opens the app instead of answering in place, through the same route table a deep link
     * uses -- so the screen has one address, not two.
     */
    @AppIntent(value = "show_workout", title = "Show a workout",
            phrases = {"Show my ${workout} in ${applicationName}"},
            opensRoute = "/workouts/:id")
    public static IntentResult showWorkout(
            @IntentParam(value = "workout", title = "Which workout?") Workout workout) {
        return IntentResult.opens("/workouts/" + workout.getId());
    }

    /**
     * Takes the context so it can give up politely rather than being cut off mid-way.
     */
    @AppIntent(value = "weekly_total", title = "Weekly total", headless = true,
            phrases = {"How much did I train in ${applicationName}"})
    public static IntentResult weeklyTotal(IntentContext ctx) {
        if (ctx.isCancelled()) {
            return IntentResult.failed("That took too long");
        }
        return IntentResult.spoken(Preferences.get("totalMinutes", 0)
                + " minutes so far this week.");
    }

    @Route("/workouts/:id")
    public static Form workoutForm(@com.codename1.annotations.RouteParam("id") String id) {
        Form f = new Form("Workout " + id, BoxLayout.y());
        f.add(new Label("Workout " + id));
        return f;
    }

    // ------------------------------------------------------------------
    // The entity
    // ------------------------------------------------------------------

    /**
     * An app noun the platform can list, search and hand back. The id is a stable identifier
     * rather than a list position, because the search index and donated shortcuts persist it.
     */
    @IntentEntity(value = "workout", title = "Workout", indexed = true)
    public static class Workout {
        private final String id;
        private final String name;
        private final String detail;

        public Workout(String id, String name, String detail) {
            this.id = id;
            this.name = name;
            this.detail = detail;
        }

        @EntityId
        public String getId() {
            return id;
        }

        @EntityTitle
        public String getName() {
            return name;
        }

        @EntitySubtitle
        public String getDetail() {
            return detail;
        }

        @EntityQuery(EntityQuery.Kind.BY_ID)
        public static Workout byId(String id) {
            for (Workout w : all()) {
                if (w.getId().equals(id)) {
                    return w;
                }
            }
            return null;
        }

        @EntityQuery(EntityQuery.Kind.SUGGESTED)
        public static List<Workout> recent() {
            return all();
        }

        @EntityQuery(EntityQuery.Kind.SEARCH)
        public static List<Workout> matching(String query) {
            List<Workout> out = new ArrayList<Workout>();
            String q = query == null ? "" : query.toLowerCase();
            for (Workout w : all()) {
                if (w.getName().toLowerCase().indexOf(q) >= 0) {
                    out.add(w);
                }
            }
            return out;
        }

        static List<Workout> all() {
            List<Workout> out = new ArrayList<Workout>();
            out.add(new Workout("1", "Morning run", "5km, easy"));
            out.add(new Workout("2", "Evening ride", "20km, hilly"));
            out.add(new Workout("3", "Pool swim", "1km"));
            return out;
        }
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private static int totalMinutes() {
        return Preferences.get("totalMinutes", 0);
    }

    /** Publishes the workouts to device search so they show up outside the app. */
    private static void publishRecent() {
        List<AppEntity> entities = new ArrayList<AppEntity>();
        for (Workout w : Workout.all()) {
            entities.add(new AppEntity("workout", w.getId())
                    .setTitle(w.getName())
                    .setSubtitle(w.getDetail())
                    .addKeywords("workout", "training"));
        }
        Intents.index(entities);
    }
}
