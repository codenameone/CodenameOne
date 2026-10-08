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
package javafx.scene.shape;

import java.util.Collection;
import java.util.List;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.fxcompat.runtime.FxPath;

import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/// An outline built from a list of [PathElement]s. It has a black stroke
/// and no fill to begin with.
///
/// The first element has to be a [MoveTo]; a path that starts with
/// anything else draws nothing, as in JavaFX.
public class Path extends Shape {

    private final ObservableList<PathElement> elements = FXCollections.observableArrayList();
    private final ObjectProperty<FillRule> fillRule = new FxObject<FillRule>(this, "fillRule", FillRule.NON_ZERO,
            Dirty.GEOMETRY);

    /// Creates a path without elements.
    public Path() {
        super(true);
        listen();
    }

    /// Creates a path from elements.
    public Path(PathElement... elements) {
        super(true);
        listen();
        if (elements != null) {
            for (int i = 0; i < elements.length; i++) {
                this.elements.add(elements[i]);
            }
        }
    }

    /// Creates a path from elements.
    public Path(Collection<? extends PathElement> elements) {
        super(true);
        listen();
        if (elements != null) {
            this.elements.addAll(elements);
        }
    }

    private void listen() {
        elements.addListener(new ListChangeListener<PathElement>() {
            @Override
            public void onChanged(Change<? extends PathElement> change) {
                while (change.next()) {
                    List<? extends PathElement> removed = change.getRemoved();
                    for (int i = 0; i < removed.size(); i++) {
                        PathElement e = removed.get(i);
                        if (e != null) {
                            e.removeOwner(Path.this);
                        }
                    }
                    if (change.wasAdded()) {
                        List<? extends PathElement> added = change.getAddedSubList();
                        for (int i = 0; i < added.size(); i++) {
                            PathElement e = added.get(i);
                            if (e != null) {
                                e.addOwner(Path.this);
                            }
                        }
                    }
                }
                cn1Invalidated(Dirty.GEOMETRY);
            }
        });
    }

    /// Returns the elements of the outline, in drawing order.
    public final ObservableList<PathElement> getElements() {
        return elements;
    }

    /// Returns how the inside is decided.
    public final FillRule getFillRule() {
        FillRule r = fillRule.get();
        return r == null ? FillRule.NON_ZERO : r;
    }

    /// Sets how the inside is decided.
    public final void setFillRule(FillRule value) {
        fillRule.set(value);
    }

    /// How the inside is decided.
    public final ObjectProperty<FillRule> fillRuleProperty() {
        return fillRule;
    }

    @Override
    protected FxPath cn1CreatePath() {
        FxPath p = new FxPath();
        p.setEvenOdd(getFillRule() == FillRule.EVEN_ODD);
        if (elements.isEmpty() || !(elements.get(0) instanceof MoveTo)) {
            return p;
        }
        boolean closed = false;
        for (int i = 0; i < elements.size(); i++) {
            PathElement e = elements.get(i);
            if (e == null) {
                continue;
            }
            if (closed && !(e instanceof MoveTo) && !(e instanceof ClosePath)) {
                // Drawing on after a close starts again where the closed
                // part started.
                p.moveTo(p.currentX(), p.currentY());
            }
            e.addTo(p);
            closed = e instanceof ClosePath;
        }
        return p;
    }
}
