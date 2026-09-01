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
import com.codename1.system.Lifecycle;
import com.codename1.ui.*;

/**
 * A pushed application, entered the way the platform enters one.
 *
 * There is no main here: a real Codename One application is a Lifecycle, and
 * the runtime constructs it, calls init and start, and -- when the program is
 * stopped or replaced -- calls stop. That last one is what a program releasing
 * a recorder, a socket or a sensor depends on, and a runtime that only detached
 * its callbacks left those running against the next program.
 */
public class LifecycleStopProbe extends Lifecycle {
    @Override
    public void start() {
        Form f = new Form("Lifecycle");
        f.add(new Label("Press Stop, then read the log"));
        f.show();
        System.out.println("PROBE LifecycleStopProbe: start() ran");
    }

    @Override
    public void stop() {
        // Printed rather than shown: by the time this runs the runtime is
        // putting its own screen back, and a dialog here would fight it.
        System.out.println("PROBE LifecycleStopProbe: stop() ran");
    }
}
