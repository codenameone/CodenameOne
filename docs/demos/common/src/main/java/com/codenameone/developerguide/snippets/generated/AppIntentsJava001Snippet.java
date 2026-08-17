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
package com.codenameone.developerguide.snippets.generated;

import com.codename1.gpu.*;
import com.codename1.ui.*;
import com.codename1.ui.animations.*;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.*;
import com.codename1.components.*;
import com.codename1.charts.models.*;
import com.codename1.charts.renderers.*;
import com.codename1.charts.views.*;
import com.codename1.capture.*;
import com.codename1.io.*;
import com.codename1.l10n.*;
import com.codename1.location.*;
import com.codename1.maps.*;
import com.codename1.media.*;
import com.codename1.messaging.*;
import com.codename1.payment.*;
import com.codename1.processing.*;
import com.codename1.properties.*;
import com.codename1.push.*;
import com.codename1.security.*;
import com.codename1.social.*;
import com.codename1.ui.spinner.*;
import com.codename1.intents.*;
import com.codename1.annotations.*;
import com.codename1.router.*;
import java.io.*;
import java.util.*;


class AppIntentsJava001Snippet {

    Object context;
    String orderId = "42";
    void snippet() throws Exception {
        // Declarations are methods; see the tagged block below.
    }

    // tag::app-intents-java-001[]
    @AppIntent(value = "log_workout", title = "Log a workout",
            description = "Records a completed workout",
            phrases = {"Log a workout in ${applicationName}",
                       "Log a ${minutes} minute ${kind} in ${applicationName}"},
            headless = true, timeoutSeconds = 5)
    public static IntentResult logWorkout(
            @IntentParam(value = "kind", title = "What kind of workout?",
                         options = {"run", "ride", "swim"}) String kind,
            @IntentParam(value = "minutes", title = "How many minutes?") int minutes) {
        WorkoutStore.append(kind, minutes);
        return IntentResult.spoken("Logged a " + minutes + " minute " + kind + ".");
    }
    // end::app-intents-java-001[]

    static class WorkoutStore {
        static void append(String kind, int minutes) {
        }
    }
}
