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
package com.codenameone.developerguide.continuity;

import com.codename1.continuity.AppState;
import com.codename1.continuity.Continuity;
import com.codename1.continuity.ContinuityListener;
import com.codename1.continuity.RestStateRelay;
import com.codename1.continuity.StateProvider;
import com.codename1.continuity.sync.SyncedStore;
import com.codename1.continuity.sync.SyncedStoreListener;
import com.codename1.router.Navigation;
import com.codename1.ui.Dialog;
import com.codename1.ui.TextArea;

import java.util.HashMap;
import java.util.Map;

/**
 * Snippets that accompany the State Restoration and Continuity guide chapter. Each block between
 * the tag markers is included verbatim into the AsciiDoc.
 */
public class ContinuitySnippets {

    /** Stands in for the screen the application is showing. */
    private TextArea draftField = new TextArea();

    /** Stands in for the application's own session object. */
    private Session session = new Session();

    /** A state the application held back rather than acting on immediately. */
    private AppState held;

    static class Session {
        String getAccessToken() {
            return "a-token";
        }
    }

    // tag::provider[]
    public void init(Object context) {
        Continuity.setStateProvider(new StateProvider() {
            public Map<String, Object> saveState() {
                Map<String, Object> state = new HashMap<String, Object>();
                state.put("draft", draftField.getText());
                return state;
            }

            public void restoreState(Map<String, Object> state) {
                draftField.setText((String) state.get("draft"));
            }
        });
    }
    // end::provider[]

    // tag::start[]
    public void start() {
        if (!Continuity.restore()) {
            Navigation.navigate("/home");
        }
    }
    // end::start[]

    // tag::checkpoint[]
    public void onDraftSaved() {
        Continuity.setTitle("Draft to Dana");
        Continuity.checkpoint();
    }
    // end::checkpoint[]

    // tag::askFirst[]
    public void askBeforeMovingTheUser() {
        Continuity.setAutoRestore(false);
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(AppState state) {
                held = state;
                if (Dialog.show("Continue?", "Pick up \"" + state.getTitle()
                        + "\" from your other device?", "Continue", "Stay here")) {
                    Continuity.restore(held);
                } else {
                    // Declining is a decision, and it has to be recorded. Returning false alone
                    // only suppresses the state for THIS run -- false also means "keep it, I will
                    // prompt again later" -- so without this the relay's unchanged document asks
                    // the same question after every relaunch.
                    Continuity.acknowledge(state);
                }
                // Consumed either way: the decision has been made here, and recorded either way.
                return false;
            }
        });
    }
    // end::askFirst[]

    // tag::relay[]
    public void useMyOwnEndpoint() {
        Continuity.setRelay(new RestStateRelay("https://api.example.com/continuity") {
            @Override
            protected String getToken() {
                return session.getAccessToken();
            }
        });
    }
    // end::relay[]

    // tag::pollOnResume[]
    public void onAppResumed() {
        Continuity.pollRelay();
    }
    // end::pollOnResume[]

    // tag::syncedStore[]
    public String readSortOrder() {
        return SyncedStore.get("sortOrder", "byName");
    }

    public void writeSortOrder(String order) {
        if (!SyncedStore.put("sortOrder", order)) {
            // No synced store here, or it is full. The value still has to live somewhere, so
            // fall back to this device's own preferences rather than losing the choice.
            com.codename1.io.Preferences.set("sortOrder", order);
        }
    }
    // end::syncedStore[]

    // tag::syncedStoreListener[]
    public void followTheStore() {
        SyncedStore.addChangeListener(new SyncedStoreListener() {
            public void storeChanged() {
                // No values are carried, on any platform. Re-read what this screen shows.
                applySortOrder(SyncedStore.get("sortOrder", "byName"));
            }
        });
    }
    // end::syncedStoreListener[]

    // tag::capability[]
    public void describeWhatThisDeviceCanDo() {
        if (Continuity.isContinuationSupported()) {
            showBanner("Open this app on your other device to carry on there.");
        }
    }
    // end::capability[]

    // tag::logout[]
    public void onLogout() {
        Continuity.clear();
    }
    // end::logout[]

    // tag::maxAge[]
    public void expireACheckout() {
        Continuity.setMaxAge(15 * 60 * 1000);
    }
    // end::maxAge[]

    private void applySortOrder(String order) {
    }

    private void showBanner(String message) {
    }
}
