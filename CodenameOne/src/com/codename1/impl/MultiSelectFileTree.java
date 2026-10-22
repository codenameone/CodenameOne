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

package com.codename1.impl;

import com.codename1.components.FileTree;
import com.codename1.components.FileTreeModel;
import com.codename1.ui.Button;
import com.codename1.ui.FontImage;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import java.util.ArrayList;
import java.util.List;

/// The file tree of the fallback Open dialog when several files may be picked:
/// a leaf toggles a check mark instead of closing the dialog.
class MultiSelectFileTree extends FileTree {
    // Insertion ordered, so the result lists files in the order they were picked.
    // Not initialized here: Tree's constructor builds the first branch, calling
    // createNodeComponent before this class's field initializers have run.
    private List<String> selected;

    MultiSelectFileTree(FileTreeModel model) {
        super(model);
    }

    /// The checked paths, in the order they were checked.
    String[] getSelectedPaths() {
        List<String> selected = selection();
        return selected.toArray(new String[selected.size()]);
    }

    @Override
    protected Button createNodeComponent(final Object node, int depth) {
        final Button b = super.createNodeComponent(node, depth);
        if (!(node instanceof String) || !getModel().isLeaf(node)) {
            return b;
        }
        final String path = (String) node;
        final List<String> selected = selection();
        // A collapsed folder drops its leaves and builds them again when it is
        // reopened, so the mark is read back from the selection, not kept on the button.
        mark(b, selected.contains(path));
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                boolean check = !selected.contains(path);
                if (check) {
                    selected.add(path);
                } else {
                    selected.remove(path);
                }
                mark(b, check);
            }
        });
        return b;
    }

    private List<String> selection() {
        if (selected == null) {
            selected = new ArrayList<String>();
        }
        return selected;
    }

    private void mark(Button b, boolean checked) {
        setNodeMaterialIcon(checked ? FontImage.MATERIAL_CHECK_BOX : FontImage.MATERIAL_CHECK_BOX_OUTLINE_BLANK, b, 3);
    }
}
