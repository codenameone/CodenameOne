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

import com.codename1.continuity.AppState;
import com.codename1.continuity.Continuity;
import com.codename1.continuity.ContinuityListener;
import com.codename1.continuity.StateProvider;
import com.codename1.continuity.sync.SyncedStore;
import com.codename1.continuity.sync.SyncedStoreListener;
import com.codename1.ui.Button;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;

import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates {@code com.codename1.continuity}: keeping the user's work across a process death,
 * and handing it to another device they own.
 *
 * <p>Deliberately without {@code @Route}. An app whose screens are declared with routes gets its
 * navigation stack restored for free and shows nothing of the mechanism, which makes a poor
 * demonstration -- so this one carries its whole state in the payload, which is also the harder
 * of the two cases and the one that needs the code below.</p>
 *
 * <p>To see it work in the simulator: type into the field, then use
 * {@code Simulate -> Continuity -> Continue Here (As Another Device)}. On two Apple devices signed
 * in to the same account, type on one and launch the app on the other.</p>
 */
public class ContinuitySample {

    private Form current;
    private Resources theme;

    /** The whole of this app's state. Read by the provider, written by the field. */
    private String draft = "";

    /** Where the field was scrolled to, which is the sort of thing a route cannot carry. */
    private int caret;

    private TextArea field;
    private Label status;

    public void init(Object context) {
        theme = UIManager.initFirstTheme("/theme");
        Toolbar.setGlobalToolbar(true);

        // Installing a provider is what turns the framework on. Nothing before this line has any
        // effect, which is what keeps an app that does not use continuity behaving as it always
        // did.
        Continuity.setStateProvider(new StateProvider() {
            public Map<String, Object> saveState() {
                Map<String, Object> state = new HashMap<String, Object>();
                state.put("draft", draft);
                state.put("caret", Integer.valueOf(caret));
                return state;
            }

            public void restoreState(Map<String, Object> state) {
                Object savedDraft = state.get("draft");
                if (savedDraft instanceof String) {
                    draft = (String) savedDraft;
                }
                Object savedCaret = state.get("caret");
                // instanceof rather than a cast: a state that crossed from another device came
                // through JSON, where every number is a Double, and a failed cast does not throw
                // on the iOS virtual machine.
                if (savedCaret instanceof Number) {
                    caret = ((Number) savedCaret).intValue();
                }
            }
        });

        // Ask before moving the user. Jumping them somewhere without warning is the wrong default
        // for anything they might be midway through, and holding the state is a one-liner.
        Continuity.setAutoRestore(false);
        Continuity.addContinuationListener(new ContinuityListener() {
            public boolean stateReceived(final AppState state) {
                String label = state.getTitle() == null ? "your other device" : state.getTitle();
                if (Dialog.show("Continue?", "Pick up \"" + label + "\"?", "Continue", "Stay")) {
                    Continuity.restore(state);
                    showDraftForm();
                }
                // Consumed either way: the decision has been made here, so no other listener is
                // asked and nothing is restored behind this one's back.
                return false;
            }
        });

        SyncedStore.addChangeListener(new SyncedStoreListener() {
            public void storeChanged() {
                refreshStatus();
            }
        });
    }

    public void start() {
        if (current != null) {
            current.show();
            return;
        }
        // "Restore, or else begin". This app records no routes, so restore() hands the payload to
        // the provider and answers false -- the screen is still this app's to show.
        Continuity.restore();
        showDraftForm();
    }

    public void stop() {
        current = Display.getInstance().getCurrent();
        if (current instanceof Dialog) {
            ((Dialog) current).dispose();
            current = Display.getInstance().getCurrent();
        }
    }

    public void destroy() {
    }

    private void showDraftForm() {
        Form form = new Form("Continuity", BoxLayout.y());

        field = new TextArea(draft, 5, 40);
        field.addActionListener(new ActionListener<ActionEvent>() {
            public void actionPerformed(ActionEvent evt) {
                capture();
            }
        });
        form.add(new Label("Type something, then continue it elsewhere:"));
        form.add(field);

        status = new Label("");
        form.add(status);

        Button checkpoint = new Button("Save a checkpoint now");
        checkpoint.addActionListener(new ActionListener<ActionEvent>() {
            public void actionPerformed(ActionEvent evt) {
                capture();
                Dialog.show("Saved", "Advertised as \"" + Continuity.getTitle() + "\".", "OK", null);
            }
        });
        form.add(checkpoint);

        Button remember = new Button("Remember this device's choice");
        remember.addActionListener(new ActionListener<ActionEvent>() {
            public void actionPerformed(ActionEvent evt) {
                // A write that reports whether it happened, because the store does not exist on
                // most platforms and is finite where it does.
                if (!SyncedStore.put("lastEditor", Display.getInstance().getPlatformName())) {
                    Dialog.show("No synced store", "This platform has none, so the choice stays "
                            + "on this device.", "OK", null);
                }
                refreshStatus();
            }
        });
        form.add(remember);

        Button forget = new Button("Log out (forget everything)");
        forget.addActionListener(new ActionListener<ActionEvent>() {
            public void actionPerformed(ActionEvent evt) {
                draft = "";
                caret = 0;
                // The advertised activity outlives this screen, so an account's work would stay
                // on offer to the devices around it without this.
                Continuity.clear();
                field.setText("");
                refreshStatus();
            }
        });
        form.add(forget);

        refreshStatus();
        form.show();
    }

    /** Reads the screen into the fields the provider reports, then checkpoints. */
    private void capture() {
        draft = field.getText();
        caret = field.getCursorPosition();
        // A title names the WORK, not the screen: it is what another device shows the user before
        // they accept.
        Continuity.setTitle(draft.length() == 0 ? "An empty draft"
                : "Draft: " + draft.substring(0, Math.min(24, draft.length())));
        Continuity.checkpoint();
        refreshStatus();
    }

    private void refreshStatus() {
        if (status == null) {
            return;
        }
        status.setText("continuation: " + (Continuity.isContinuationSupported() ? "yes" : "no")
                + " | synced store: " + (SyncedStore.isSupported() ? "yes" : "no")
                + " | last editor: " + SyncedStore.get("lastEditor", "none"));
        if (status.getComponentForm() != null) {
            status.getComponentForm().revalidate();
        }
    }
}
