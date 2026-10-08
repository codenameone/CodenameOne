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
package javafx.collections.transformation;

import java.util.List;

import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.collections.WeakListChangeListener;

/// Base class for a list that presents another observable list differently
/// and follows its changes. The view observes its source weakly, so an
/// unused view is collected while the source lives on.
public abstract class TransformationList<E, F> extends ObservableListBase<E> {

    private final ObservableList<? extends F> source;
    private final ListChangeListener<F> sourceListener;

    /// Creates a view of a source list.
    protected TransformationList(ObservableList<? extends F> source) {
        if (source == null) {
            throw new NullPointerException();
        }
        this.source = source;
        this.sourceListener = new ListChangeListener<F>() {
            @Override
            public void onChanged(Change<? extends F> change) {
                TransformationList.this.sourceChanged(change);
            }
        };
        source.addListener(new WeakListChangeListener<F>(sourceListener));
    }

    /// Returns the list this one is a view of.
    public final ObservableList<? extends F> getSource() {
        return source;
    }

    /// Returns whether a list is this view's source, or the source of its
    /// source, and so on.
    public final boolean isInTransformationChain(ObservableList<?> list) {
        List<?> current = source;
        while (current != list) {
            if (!(current instanceof TransformationList)) {
                return false;
            }
            current = ((TransformationList<?, ?>) current).source;
        }
        return true;
    }

    /// Called with every change of the source.
    protected abstract void sourceChanged(Change<? extends F> c);

    /// Returns the position in the source of the element at a position of
    /// this view.
    public abstract int getSourceIndex(int index);

    /// Returns the position, in a list somewhere down the chain of sources,
    /// of the element at a position of this view.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when the list is not in the chain
    public final int getSourceIndexFor(ObservableList<?> list, int index) {
        if (!isInTransformationChain(list)) {
            throw new IllegalArgumentException(
                    "Provided list is not in the transformation chain of this transformation list");
        }
        List<?> current = source;
        int result = getSourceIndex(index);
        while (current != list && current instanceof TransformationList) {
            TransformationList<?, ?> view = (TransformationList<?, ?>) current;
            result = view.getSourceIndex(result);
            current = view.source;
        }
        return result;
    }

    /// Returns the position in this view of the element at a position of
    /// the source, or -1 when the view does not show it.
    public abstract int getViewIndex(int index);
}
