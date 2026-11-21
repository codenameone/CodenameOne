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
package com.codename1.wordprocessing;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A `DocumentView` with its embedded components alive: each component box on screen holds a
/// real Codename One component, made by the factory `EmbeddedComponents` has for its type,
/// that the user can tap, drag and type into.
///
/// ```java
/// DocumentEditor editor = new DocumentEditor(new DocumentModel(doc));
/// form.add(BorderLayout.CENTER, editor);
/// ```
///
/// Only the components in view exist. One that scrolls out of view, or is replaced by an edit,
/// first gives its state back to the model through its factory, as an undoable edit; call
/// `commitComponents` before saving to collect the state of those still showing.
public class DocumentEditor extends Container {
    private final DocumentView view;
    private final Map<String, Peer> peers = new HashMap<String, Peer>();
    private boolean syncing;

    /// A live component and the embedded component it shows.
    private static final class Peer {
        final Component component;
        EmbeddedComponent embedded;
        final String paragraphId;
        final int offset;
        final int[] bounds = new int[4];

        Peer(Component component, EmbeddedComponent embedded, String paragraphId, int offset) {
            this.component = component;
            this.embedded = embedded;
            this.paragraphId = paragraphId;
            this.offset = offset;
        }
    }

    /// Places the view over the whole container and every peer over its box.
    private final class EditorLayout extends Layout {
        @Override
        public void layoutContainer(Container parent) {
            view.setX(0);
            view.setY(0);
            view.setWidth(parent.getLayoutWidth());
            view.setHeight(parent.getLayoutHeight());
            for (Peer p : peers.values()) {
                p.component.setX(p.bounds[0]);
                p.component.setY(p.bounds[1]);
                p.component.setWidth(p.bounds[2]);
                p.component.setHeight(p.bounds[3]);
                if (p.component instanceof Container) {
                    ((Container) p.component).layoutContainer();
                }
            }
        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            return view.getPreferredSize();
        }
    }

    /// An editor of an empty document.
    public DocumentEditor() {
        this(new DocumentModel());
    }

    /// An editor of a model.
    ///
    /// #### Parameters
    ///
    /// - `model`: the model
    public DocumentEditor(DocumentModel model) {
        this(new DocumentView(model));
    }

    /// An editor around a view, for a `DocumentView` subclass.
    ///
    /// #### Parameters
    ///
    /// - `view`: the view
    public DocumentEditor(DocumentView view) {
        this.view = view;
        setLayout(new EditorLayout());
        setUIID("DocumentEditor");
        add(view);
        view.setViewportListener(new Runnable() {
            @Override
            public void run() {
                syncPeers();
            }
        });
    }

    /// The view.
    ///
    /// #### Returns
    ///
    /// the document view
    public DocumentView getView() {
        return view;
    }

    /// The model.
    ///
    /// #### Returns
    ///
    /// the view's model
    public DocumentModel getModel() {
        return view.getModel();
    }

    /// The live component showing an embedded component, while it is in view.
    ///
    /// #### Parameters
    ///
    /// - `paragraphId`: the paragraph holding it
    ///
    /// - `offset`: its offset in the paragraph
    ///
    /// #### Returns
    ///
    /// the component, or null
    public Component getLiveComponent(String paragraphId, int offset) {
        Peer p = peers.get(paragraphId + "@" + offset);
        return p == null ? null : p.component;
    }

    /// The number of live components.
    ///
    /// #### Returns
    ///
    /// how many embedded components are alive now
    public int getLiveComponentCount() {
        return peers.size();
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        syncPeers();
    }

    @Override
    protected void deinitialize() {
        commitComponents();
        super.deinitialize();
    }

    /// Writes the state of every live component into the model.
    public void commitComponents() {
        for (Peer p : new ArrayList<Peer>(peers.values())) {
            commit(p);
        }
    }

    private void commit(Peer p) {
        EmbeddedComponents.Factory f = EmbeddedComponents.get(p.embedded.type);
        if (f == null) {
            return;
        }
        String state = f.state(p.component);
        if (state == null || state.equals(p.embedded.state)) {
            return;
        }
        DocumentModel m = view.getModel();
        Paragraph para = m.findParagraph(p.paragraphId);
        if (para == null || p.offset >= para.text.length()
                || !p.embedded.equals(para.text.objectAt(p.offset))) {
            return;
        }
        EmbeddedComponent next = p.embedded.withState(state);
        // the peer already shows the new state: it must not be taken for a stale one
        p.embedded = next;
        boolean was = syncing;
        syncing = true;
        try {
            m.replaceParagraph(para.withText(para.text.withObject(p.offset, next)));
        } finally {
            syncing = was;
        }
    }

    /// Creates, moves and drops live components to match what the view shows.
    void syncPeers() {
        if (syncing) {
            return;
        }
        syncing = true;
        try {
            Map<String, Object[]> wanted = visibleComponents();
            for (String key : new ArrayList<String>(peers.keySet())) {
                Peer p = peers.get(key);
                Object[] w = wanted.get(key);
                if (w == null) {
                    // scrolled out of view: keep what the user did to it
                    peers.remove(key);
                    commitLater(p);
                    removeComponent(p.component);
                } else if (!p.embedded.equals(((LayoutItem) w[1]).component)) {
                    // an edit or an undo replaced it: the document wins
                    peers.remove(key);
                    removeComponent(p.component);
                }
            }
            for (Map.Entry<String, Object[]> e : wanted.entrySet()) {
                LayoutItem it = (LayoutItem) e.getValue()[1];
                int page = ((Integer) e.getValue()[0]).intValue();
                Peer p = peers.get(e.getKey());
                if (p == null) {
                    Component c = EmbeddedComponents.create(it.component);
                    if (c == null) {
                        continue;
                    }
                    p = new Peer(c, it.component, it.paragraphId, it.start);
                    peers.put(e.getKey(), p);
                    addComponent(c);
                }
                int[] b = view.itemBounds(page, it);
                System.arraycopy(b, 0, p.bounds, 0, 4);
            }
        } finally {
            syncing = false;
        }
        revalidateLater();
        repaint();
    }

    private final List<Peer> pending = new ArrayList<Peer>();

    private void commitLater(Peer p) {
        // committing edits the model, which lays the view out again: not while walking peers
        pending.add(p);
        if (pending.size() == 1) {
            com.codename1.ui.Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    List<Peer> all = new ArrayList<Peer>(pending);
                    pending.clear();
                    for (Peer q : all) {
                        commit(q);
                    }
                }
            });
        }
    }

    /// The component items on the pages in view, by key, as `{page, item}`.
    private Map<String, Object[]> visibleComponents() {
        Map<String, Object[]> out = new HashMap<String, Object[]>();
        DocumentLayout l = view.getLayout();
        int h = view.getHeight();
        int w = view.getWidth();
        if (h <= 0 || w <= 0) {
            return out;
        }
        for (int page = 0; page < l.getPageCount(); page++) {
            for (LayoutItem it : l.getPage(page).getItems()) {
                if (it.kind != LayoutItem.COMPONENT || it.paragraphId == null) {
                    continue;
                }
                int[] b = view.itemBounds(page, it);
                if (b[1] + b[3] < 0 || b[1] > h || b[0] + b[2] < 0 || b[0] > w) {
                    continue;
                }
                out.put(it.paragraphId + "@" + it.start, new Object[] {Integer.valueOf(page), it});
            }
        }
        return out;
    }
}
