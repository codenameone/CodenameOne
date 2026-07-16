/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.ui;

import com.codename1.ui.editor.EditorHost;
import com.codename1.ui.editor.PureEditor;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;
import java.util.List;

/// Base class for the native visual editors (rich text and code) introduced by Codename One.
///
/// The editor components are designed around a *semantic command channel* rather than a single
/// hard-coded implementation. Every concrete editor (`RichTextArea`, `CodeEditor`) speaks to its
/// backend exclusively through `#command(String, String)`, `#query(String, String, SuccessCallback)`
/// and the inbound `#onEditorEvent(String, String)` dispatch. Two interchangeable backends honor
/// that channel:
///
/// 1. The pure Codename One text engine (`com.codename1.ui.editor`) which renders the document itself
///    with `Graphics`/`Font` and binds to the platform text input source (soft keyboard, hardware
///    keyboard and IME). This is the default where the port exposes low-level text input.
/// 2. A `BrowserComponent` fallback on ports that don't expose low-level text input. Its
///    `contenteditable` surface remains editable through the platform web view.
/// 3. An optional native backend supplied by the platform port (see
///    `com.codename1.impl.CodenameOneImplementation#createNativeEditorPeer(AbstractEditorComponent, String)`).
///    When a port returns a non-null native peer the editor drives it through
///    `editorPeerCommand` / `editorPeerQuery` instead of the pure engine, allowing a platform to provide a
///    genuinely native experience.
///
/// All backends are addressed with the same vocabulary so concrete editors never need to know which
/// one is active.
///
/// @author Shai Almog
public abstract class AbstractEditorComponent extends Container implements EditorHost {
    /// Prefix used for messages sent from the browser fallback to Codename One.
    static final String MESSAGE_PREFIX = "cn1ed:";

    private BrowserComponent browser;
    private PeerComponent nativePeer;
    private PureEditor pureEditor;
    private boolean nativeMode;
    private boolean ready;
    private boolean editable = true;
    private final List<Runnable> readyQueue = new ArrayList<Runnable>();
    private final EventDispatcher changeListeners = new EventDispatcher();
    private final EventDispatcher readyListeners = new EventDispatcher();
    private final Label placeholder = new Label("");

    /// Creates the editor and begins asynchronous backend initialization.
    ///
    /// #### Parameters
    ///
    /// - `uiid`: the UIID applied to the editor container
    protected AbstractEditorComponent(String uiid) {
        setUIID(uiid);
        setLayout(new BorderLayout());
        placeholder.setShowEvenIfBlank(true);
        addComponent(BorderLayout.CENTER, placeholder);
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                initBackend();
            }
        });
    }

    private void initBackend() {
        nativePeer = Display.impl.createNativeEditorPeer(this, getEditorType());
        if (nativePeer != null) {
            nativeMode = true;
            removeComponent(placeholder);
            addComponent(BorderLayout.CENTER, nativePeer);
            // the native peer signals readiness through onEditorEvent("ready", null); if a platform
            // creates the peer fully initialized it may call that immediately, otherwise we mark ready
            // here to flush queued commands as soon as the peer is attached.
            markReady();
            revalidateLater();
            return;
        }
        nativeMode = false;
        if (isTextInputSupported()) {
            pureEditor = createPureEditor();
            removeComponent(placeholder);
            addComponent(BorderLayout.CENTER, pureEditor.getView());
            markReady();
            revalidateLater();
            return;
        }
        initBrowserBackend();
    }

    private void initBrowserBackend() {
        browser = new BrowserComponent();
        browser.setProperty("BackgroundColor", 0xffffff);
        browser.addWebEventListener(BrowserComponent.onMessage, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                handleBrowserMessage((String) evt.getSource());
            }
        });
        browser.addWebEventListener(BrowserComponent.onLoad, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                markReady();
            }
        });
        removeComponent(placeholder);
        addComponent(BorderLayout.CENTER, browser);
        String engineUrl = getEngineURL();
        if (engineUrl != null) {
            try {
                browser.setURLHierarchy(engineUrl);
            } catch (java.io.IOException err) {
                browser.setURL(engineUrl);
            }
        } else {
            browser.setPage(createEditorHtml(), getEditorBaseURL());
        }
        revalidateLater();
    }

    private void handleBrowserMessage(String msg) {
        if (msg == null || !msg.startsWith(MESSAGE_PREFIX)) {
            return;
        }
        String body = msg.substring(MESSAGE_PREFIX.length());
        int colon = body.indexOf(':');
        onEditorEvent(colon < 0 ? body : body.substring(0, colon),
                colon < 0 ? null : body.substring(colon + 1));
    }

    private void markReady() {
        if (ready) {
            return;
        }
        ready = true;
        applyEditableState();
        List<Runnable> copy = new ArrayList<Runnable>(readyQueue);
        readyQueue.clear();
        for (Runnable r : copy) {
            r.run();
        }
        readyListeners.fireActionEvent(new ActionEvent(this));
    }

    /// Creates the pure Codename One backend for this editor. Subclasses override to supply a code or
    /// rich text feature layer.
    PureEditor createPureEditor() {
        return new PureEditor(this, getEditorType());
    }

    /// Returns the editor type identifier passed to the native peer factory, e.g.
    /// `"richtext"` or `"code"`.
    abstract String getEditorType();

    /// Returns the self-contained page used by the browser fallback.
    abstract String createEditorHtml();

    /// Base URL for relative resources in the browser fallback page.
    String getEditorBaseURL() {
        return "https://cn1editor.codenameone.com/";
    }

    /// Optional app-hierarchy URL for a custom browser editor engine.
    String getEngineURL() {
        return null;
    }

    /// Inbound event dispatch from either backend. Subclasses override to react to editor side events
    /// (content changes, selection changes, completion requests, ...). Always call
    /// `super.onEditorEvent` for the shared `"change"` and `"ready"` handling.
    ///
    /// #### Parameters
    ///
    /// - `type`: the event type, e.g. `"change"`, `"ready"`, `"selection"`
    ///
    /// - `value`: an optional string payload, may be null
    void onEditorEvent(String type, String value) {
        if ("ready".equals(type)) {
            markReady();
            return;
        }
        if ("change".equals(type)) {
            changeListeners.fireActionEvent(new ActionEvent(this));
        }
    }

    /// Entry point invoked by native editor peers to deliver events back to Codename One. This routes
    /// to the same dispatch path used by the browser message bridge so subclasses handle events
    /// uniformly regardless of backend.
    ///
    /// #### Parameters
    ///
    /// - `type`: the event type
    ///
    /// - `value`: optional payload, may be null
    @Override
    public void fireEditorEvent(final String type, final String value) {
        if (CN.isEdt()) {
            onEditorEvent(type, value);
        } else {
            CN.callSerially(new Runnable() {
                @Override
                public void run() {
                    onEditorEvent(type, value);
                }
            });
        }
    }

    /// Sends a one way command to the active backend. If the backend is not ready yet the command is
    /// queued and replayed once initialization completes.
    ///
    /// #### Parameters
    ///
    /// - `name`: the semantic command name understood by both backends
    ///
    /// - `arg`: an optional string argument, may be null
    protected void command(final String name, final String arg) {
        if (!ready) {
            readyQueue.add(new Runnable() {
                @Override
                public void run() {
                    command(name, arg);
                }
            });
            return;
        }
        if (nativeMode) {
            Display.impl.editorPeerCommand(nativePeer, name, arg);
        } else if (pureEditor != null) {
            pureEditor.cmd(name, arg);
        } else {
            browser.execute("window.cn1editor.cmd(${0}, ${1})", new Object[]{name, arg == null ? "" : arg});
        }
    }

    /// Queries the active backend for a string value asynchronously. The callback is always invoked on
    /// the EDT. If the backend is not ready the query is deferred until it is.
    ///
    /// #### Parameters
    ///
    /// - `name`: the semantic query name understood by both backends
    ///
    /// - `arg`: an optional string argument, may be null
    ///
    /// - `callback`: receives the query result
    protected void query(final String name, final String arg, final SuccessCallback<String> callback) {
        if (!ready) {
            readyQueue.add(new Runnable() {
                @Override
                public void run() {
                    query(name, arg, callback);
                }
            });
            return;
        }
        if (nativeMode) {
            callback.onSucess(Display.impl.editorPeerQuery(nativePeer, name, arg));
            return;
        }
        if (pureEditor != null) {
            callback.onSucess(pureEditor.query(name, arg));
            return;
        }
        browser.execute("callback.onSuccess(window.cn1editor.query(${0}, ${1}))",
                new Object[]{name, arg == null ? "" : arg}, new JSRefStringCallback(callback));
    }

    private static final class JSRefStringCallback implements SuccessCallback<BrowserComponent.JSRef> {
        private final SuccessCallback<String> delegate;

        JSRefStringCallback(SuccessCallback<String> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void onSucess(BrowserComponent.JSRef value) {
            delegate.onSucess(value == null ? null : value.getValue());
        }
    }

    /// Runs the supplied task once the editor backend is ready, or immediately if it already is.
    ///
    /// #### Parameters
    ///
    /// - `r`: the task to run on the EDT when the editor is ready
    public void onReady(final Runnable r) {
        if (ready) {
            r.run();
        } else {
            readyQueue.add(r);
        }
    }

    /// Returns true once the underlying editor backend has finished initializing and is ready to accept
    /// commands.
    public boolean isEditorReady() {
        return ready;
    }

    /// True when a platform supplied native editor backend is in use, false for the pure and browser
    /// backends.
    public boolean isNativeEditor() {
        return nativeMode;
    }

    /// Returns the browser fallback, or null when the pure or native backend is active.
    public BrowserComponent getInternalBrowser() {
        return browser;
    }

    /// Adds a listener notified whenever the editor content changes.
    ///
    /// #### Parameters
    ///
    /// - `l`: the change listener
    public void addChangeListener(ActionListener l) {
        changeListeners.addListener(l);
    }

    /// Removes a previously registered change listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the change listener
    public void removeChangeListener(ActionListener l) {
        changeListeners.removeListener(l);
    }

    /// Adds a listener notified once when the editor backend becomes ready.
    ///
    /// #### Parameters
    ///
    /// - `l`: the ready listener
    public void addReadyListener(ActionListener l) {
        if (ready) {
            l.actionPerformed(new ActionEvent(this));
        } else {
            readyListeners.addListener(l);
        }
    }

    /// Removes a previously registered ready listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the ready listener
    public void removeReadyListener(ActionListener l) {
        readyListeners.removeListener(l);
    }

    /// Enables or disables editing. A disabled editor still displays content but rejects input.
    ///
    /// #### Parameters
    ///
    /// - `editable`: true to allow editing
    public void setEditable(boolean editable) {
        this.editable = editable;
        if (ready) {
            applyEditableState();
        }
    }

    private void applyEditableState() {
        command("setEditable", editable ? "1" : "0");
    }

    /// Returns true if the editor currently allows editing.
    @Override
    public boolean isEditable() {
        return editable;
    }

    /// Requests keyboard focus for the editing surface, showing the virtual keyboard on touch devices.
    public void focusEditor() {
        command("focus", null);
    }

    /// Removes keyboard focus from the editing surface, hiding the virtual keyboard on touch devices.
    public void blurEditor() {
        command("blur", null);
    }

    // ---- EditorHost (pure backend bridge to the platform text input source) ----

    /// {@inheritDoc}
    @Override
    public boolean isTextInputSupported() {
        return Display.impl.isTextInputSupported();
    }

    /// {@inheritDoc}
    @Override
    public Object startTextInput(TextInputClient client, TextInputConfig config) {
        return Display.impl.startTextInput(client, config);
    }

    /// {@inheritDoc}
    @Override
    public void updateTextInputState(Object handle, TextInputState state) {
        Display.impl.updateTextInputState(handle, state);
    }

    /// {@inheritDoc}
    @Override
    public void stopTextInput(Object handle) {
        Display.impl.stopTextInput(handle);
    }

    /// {@inheritDoc}
    @Override
    public void editorChanged() {
        onEditorEvent("change", null);
    }
}
