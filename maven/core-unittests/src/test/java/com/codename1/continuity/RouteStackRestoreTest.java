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
package com.codename1.continuity;

import com.codename1.impl.continuity.LocalContinuityBridge;
import com.codename1.io.Storage;
import com.codename1.junit.FormTest;
import com.codename1.router.Navigation;
import com.codename1.router.RouteDispatcher;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Where the route table and state restoration meet.
 *
 * <p>{@link Navigation#restoreStack} is the whole reason an app whose screens carry {@code @Route}
 * gets them back with no code: the saved state is a list of paths, and each one has to become a
 * stack frame again or {@link Navigation#back()} would land on a screen that was never rebuilt.</p>
 *
 * <p>Everything the route half needs is public API -- {@link Navigation#setDispatcher} takes the
 * generated table, and a test double stands in for it -- so this lives beside the other continuity
 * tests rather than in {@code com.codename1.router}, and reaches the framework's package-private
 * test seams from there.</p>
 */
class RouteStackRestoreTest extends UITestBase {

    /** Returns a fresh titled Form for a registered path, null for anything else. */
    private static final class FakeDispatcher implements RouteDispatcher {
        final Map<String, Boolean> known = new HashMap<String, Boolean>();
        final List<String> dispatched = new ArrayList<String>();

        FakeDispatcher route(String path) {
            known.put(path, Boolean.TRUE);
            return this;
        }

        public Form dispatch(String url) {
            dispatched.add(url);
            if (known.containsKey(url)) {
                Form f = new Form();
                f.setTitle(url);
                return f;
            }
            return null;
        }
    }

    @BeforeEach
    void resetFramework() {
        Continuity.reset();
        Storage.getInstance().clearStorage();
        Continuity.setBridge(new LocalContinuityBridge());
        Navigation.setDispatcher(null);
        new Form("start").show();
    }

    @AfterEach
    void clearFramework() {
        Continuity.reset();
        Navigation.setDispatcher(null);
        Storage.getInstance().clearStorage();
    }

    @FormTest
    void restoringRebuildsEveryFrameAndShowsOnlyTheLast() {
        FakeDispatcher dispatcher = new FakeDispatcher().route("/home").route("/users")
                .route("/users/42");
        Navigation.setDispatcher(dispatcher);

        assertTrue(Navigation.restoreStack(Arrays.asList("/home", "/users", "/users/42")));

        assertEquals(3, Navigation.getStack().size());
        assertEquals("/users/42", Navigation.getCurrent().getPath());
        assertEquals("/users/42", Display.getInstance().getCurrent().getTitle());
        // Every frame was built, which is what makes going back land on a real screen rather
        // than on nothing.
        assertEquals(Arrays.asList("/home", "/users", "/users/42"), dispatcher.dispatched);
    }

    @FormTest
    void goingBackAfterARestoreLandsOnTheRebuiltFrame() {
        Navigation.setDispatcher(new FakeDispatcher().route("/home").route("/users/42"));
        Navigation.restoreStack(Arrays.asList("/home", "/users/42"));

        assertTrue(Navigation.back());

        assertEquals("/home", Navigation.getCurrent().getPath());
        assertEquals("/home", Display.getInstance().getCurrent().getTitle());
    }

    /**
     * A screen goes away in a rebuild and the states already sitting on the user's other devices
     * still name it. Losing the whole session over one frame the user was not even on would be a
     * worse answer than restoring the rest.
     */
    @FormTest
    void aPathThisBuildNoLongerRoutesIsSkippedRatherThanFailingTheRestore() {
        Navigation.setDispatcher(new FakeDispatcher().route("/home").route("/users/42"));

        assertTrue(Navigation.restoreStack(
                Arrays.asList("/home", "/a-screen-that-was-removed", "/users/42")));

        assertEquals(2, Navigation.getStack().size());
        assertEquals("/users/42", Navigation.getCurrent().getPath());
    }

    @FormTest
    void aStackWhoseEveryPathIsGoneRestoresNothingAndSaysSo() {
        Navigation.setDispatcher(new FakeDispatcher().route("/home"));

        assertFalse(Navigation.restoreStack(Arrays.asList("/gone", "/also-gone")));
    }

    @FormTest
    void restoringWithNoDispatcherOrNoPathsIsAnInertFalse() {
        assertFalse(Navigation.restoreStack(Arrays.asList("/home")));

        Navigation.setDispatcher(new FakeDispatcher().route("/home"));
        assertFalse(Navigation.restoreStack(null));
        assertFalse(Navigation.restoreStack(new ArrayList<String>()));
    }

    // ------------------------------------------------------------------
    // End to end: navigate, checkpoint, forget everything, restore
    // ------------------------------------------------------------------

    /**
     * The whole feature in one test: the user walks through three screens, the process is
     * replaced, and the app comes back where they were with the payload intact.
     */
    @FormTest
    void aNavigatedSessionSurvivesTheProcessBeingReplaced() {
        Navigation.setDispatcher(new FakeDispatcher().route("/home").route("/users")
                .route("/users/42"));
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("scrollY", Integer.valueOf(240));
        Continuity.setStateProvider(provider);

        // The navigation stack is process-global static, so the app is put ON /home by replacing
        // the stack rather than by navigating to it -- a plain navigate would append to whatever
        // an earlier test in this class left behind, and the assertion below would be reading
        // that instead of this session.
        Navigation.restoreStack(Arrays.asList("/home"));
        Navigation.navigate("/users");
        Navigation.navigate("/users/42");
        flushSerialCalls();

        // The process is replaced: the framework forgets everything it holds in memory, the
        // stored checkpoint is all that is left, and the route table is reinstalled by the
        // generated bootstrap exactly as it is at startup.
        AppState onDisk = Continuity.getRestorableState();
        assertNotNull(onDisk);
        assertEquals(Arrays.asList("/home", "/users", "/users/42"), onDisk.getRoutes());
        Continuity.reset();
        Navigation.restoreStack(new ArrayList<String>());
        Navigation.setDispatcher(new FakeDispatcher().route("/home").route("/users")
                .route("/users/42"));
        RecordingProvider afterRestart = new RecordingProvider();
        Continuity.setBridge(new LocalContinuityBridge());
        Continuity.setStateProvider(afterRestart);

        assertTrue(Continuity.restore());

        assertEquals(3, Navigation.getStack().size());
        assertEquals("/users/42", Navigation.getCurrent().getPath());
        assertEquals(Integer.valueOf(240), afterRestart.restored.get("scrollY"));
    }

    /**
     * An app that navigates with {@code new MyForm().show()} records no routes, so restoration is
     * the payload alone -- and {@link Continuity#restore()} answers false, which is what lets
     * "restore, or else begin" still show that app's first screen.
     */
    @FormTest
    void anAppWithNoRoutesRestoresThePayloadAndShowsNothing() {
        RecordingProvider provider = new RecordingProvider();
        provider.saved.put("draft", "unsent");
        Continuity.setStateProvider(provider);
        Continuity.checkpoint();

        Continuity.reset();
        Continuity.setBridge(new LocalContinuityBridge());
        RecordingProvider afterRestart = new RecordingProvider();
        Continuity.setStateProvider(afterRestart);

        assertFalse(Continuity.restore());
        assertEquals("unsent", afterRestart.restored.get("draft"));
    }

    static class RecordingProvider implements StateProvider {
        final Map<String, Object> saved = new HashMap<String, Object>();
        Map<String, Object> restored;

        public Map<String, Object> saveState() {
            return saved;
        }

        public void restoreState(Map<String, Object> payload) {
            restored = payload;
        }
    }
}
