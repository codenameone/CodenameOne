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
package javafx.beans.property;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import com.codename1.fxcompat.runtime.Changes;
import com.codename1.fxcompat.runtime.ListChangeBuilder;
import com.codename1.fxcompat.runtime.ListenerSet;
import com.codename1.fxcompat.runtime.PropertyText;
import com.codename1.fxcompat.runtime.ValueListeners;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakListener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/// The full implementation of a list property except for its bean and name.
///
/// The property reports two kinds of change. Replacing the list it holds
/// tells the invalidation and change listeners, and tells the list
/// listeners that the whole content was replaced. A modification of the
/// list it holds tells the invalidation listeners and passes the report on
/// to the list listeners with this property as the list.
///
/// The property follows the list it holds as soon as that list is set or
/// the bound value changes, so the size and empty properties and the list
/// listeners are always current.
public abstract class ListPropertyBase<E> extends ListProperty<E> {

    private ObservableList<E> value;
    private ObservableValue<? extends ObservableList<E>> observable;
    private InvalidationListener listener;
    private boolean valid = true;
    private ValueListeners<ObservableList<E>> helper;
    private ListenerSet<ListChangeListener<? super E>> listListeners;
    private ObservableList<E> watched;
    private final ListChangeListener<E> contentListener = new ListChangeListener<E>() {
        @Override
        public void onChanged(ListChangeListener.Change<? extends E> change) {
            contentChanged(change);
        }
    };
    private SizeProperty size0;
    private EmptyProperty empty0;

    /// Creates the property holding no list.
    public ListPropertyBase() {
    }

    /// Creates the property with an initial list.
    public ListPropertyBase(ObservableList<E> initialValue) {
        this.value = initialValue;
        this.watched = initialValue;
        if (initialValue != null) {
            initialValue.addListener(contentListener);
        }
    }

    @Override
    public ReadOnlyIntegerProperty sizeProperty() {
        if (size0 == null) {
            size0 = new SizeProperty(this);
        }
        return size0;
    }

    @Override
    public ReadOnlyBooleanProperty emptyProperty() {
        if (empty0 == null) {
            empty0 = new EmptyProperty(this);
        }
        return empty0;
    }

    @Override
    public void addListener(InvalidationListener listener) {
        helper = ValueListeners.add(helper, this, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        helper = ValueListeners.remove(helper, listener);
    }

    @Override
    public void addListener(ChangeListener<? super ObservableList<E>> listener) {
        helper = ValueListeners.add(helper, this, listener);
    }

    @Override
    public void removeListener(ChangeListener<? super ObservableList<E>> listener) {
        helper = ValueListeners.remove(helper, listener);
    }

    @Override
    public void addListener(ListChangeListener<? super E> listener) {
        listListeners = ListenerSet.addChange(listListeners, listener);
    }

    @Override
    public void removeListener(ListChangeListener<? super E> listener) {
        listListeners = ListenerSet.removeChange(listListeners, listener);
    }

    /// Tells the invalidation and change listeners the held list may have
    /// been replaced.
    protected void fireValueChangedEvent() {
        ValueListeners.fire(helper);
    }

    /// Tells the list listeners about a change of the content.
    @SuppressWarnings({"unchecked", "rawtypes"})
    protected void fireValueChangedEvent(ListChangeListener.Change<? extends E> change) {
        List<ListChangeListener<? super E>> targets = ListenerSet.changeListeners(listListeners);
        for (int i = 0; i < targets.size(); i++) {
            change.reset();
            ((ListChangeListener) targets.get(i)).onChanged(change);
        }
    }

    /// Called when the held list was replaced or modified, before listeners
    /// are told.
    protected void invalidated() {
    }

    private void contentChanged(ListChangeListener.Change<? extends E> change) {
        fireDerived();
        invalidated();
        fireValueChangedEvent();
        fireValueChangedEvent(new Changes.Relayed<E>(this, change));
    }

    private void fireDerived() {
        if (size0 != null) {
            size0.changed();
        }
        if (empty0 != null) {
            empty0.changed();
        }
    }

    private ObservableList<E> current() {
        return observable == null ? value : observable.getValue();
    }

    private void markInvalid() {
        ObservableList<E> before = watched;
        ObservableList<E> now = current();
        if (before != now) {
            if (before != null) {
                before.removeListener(contentListener);
            }
            watched = now;
            if (now != null) {
                now.addListener(contentListener);
            }
        }
        if (valid) {
            valid = false;
            invalidated();
            fireValueChangedEvent();
        }
        if (before != now) {
            fireDerived();
            List<E> removed = before == null ? new ArrayList<E>() : new ArrayList<E>(before);
            int added = now == null ? 0 : now.size();
            if (!removed.isEmpty() || added > 0) {
                ListChangeBuilder<E> builder = new ListChangeBuilder<E>(this);
                builder.beginChange();
                builder.nextReplace(0, added, removed);
                ListChangeListener.Change<E> change = builder.endChange();
                if (change != null) {
                    fireValueChangedEvent(change);
                }
            }
        }
    }

    @Override
    public ObservableList<E> get() {
        valid = true;
        return current();
    }

    /// Replaces the held list.
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: when the property is bound
    @Override
    public void set(ObservableList<E> newValue) {
        if (isBound()) {
            throw new RuntimeException(PropertyText.owner(this) + "A bound value cannot be set.");
        }
        if (value != newValue) {
            value = newValue;
            markInvalid();
        }
    }

    @Override
    public boolean isBound() {
        return observable != null;
    }

    /// Makes this property follow another value until [#unbind()]. The
    /// property observes it weakly, so being bound does not keep it alive.
    @Override
    public void bind(final ObservableValue<? extends ObservableList<E>> newObservable) {
        if (newObservable == null) {
            throw new NullPointerException("Cannot bind to null");
        }
        if (!sameObservable(newObservable)) {
            unbind();
            observable = newObservable;
            if (listener == null) {
                listener = new Listener(this);
            }
            observable.addListener(listener);
            markInvalid();
        }
    }

    /// An observable list value compares as a list, so the bound value is
    /// recognized by identity.
    private boolean sameObservable(Object candidate) {
        return candidate == observable;
    }

    /// Stops following the bound value and keeps the list it held.
    @Override
    public void unbind() {
        if (observable != null) {
            value = observable.getValue();
            observable.removeListener(listener);
            observable = null;
        }
    }

    @Override
    public String toString() {
        String tail;
        if (isBound()) {
            tail = valid ? "bound, value: " + get() : "bound, invalid";
        } else {
            tail = "value: " + get();
        }
        return PropertyText.describe("ListProperty", this, tail);
    }

    /// The number of elements of a list property.
    private static final class SizeProperty extends ReadOnlyIntegerPropertyBase {
        private final ListPropertyBase<?> owner;

        SizeProperty(ListPropertyBase<?> owner) {
            this.owner = owner;
        }

        void changed() {
            fireValueChangedEvent();
        }

        @Override
        public int get() {
            return owner.size();
        }

        @Override
        public Object getBean() {
            return owner;
        }

        @Override
        public String getName() {
            return "size";
        }
    }

    /// Whether a list property is empty.
    private static final class EmptyProperty extends ReadOnlyBooleanPropertyBase {
        private final ListPropertyBase<?> owner;

        EmptyProperty(ListPropertyBase<?> owner) {
            this.owner = owner;
        }

        void changed() {
            fireValueChangedEvent();
        }

        @Override
        public boolean get() {
            return owner.isEmpty();
        }

        @Override
        public Object getBean() {
            return owner;
        }

        @Override
        public String getName() {
            return "empty";
        }
    }

    /// Forwards invalidations of the bound value without keeping the
    /// property reachable from it.
    private static final class Listener implements InvalidationListener, WeakListener {
        private final WeakReference ref;

        Listener(ListPropertyBase<?> property) {
            this.ref = new WeakReference(property);
        }

        @Override
        public void invalidated(Observable source) {
            Object property = ref.get();
            if (property instanceof ListPropertyBase) {
                ((ListPropertyBase<?>) property).markInvalid();
            } else {
                source.removeListener(this);
            }
        }

        @Override
        public boolean wasGarbageCollected() {
            return ref.get() == null;
        }
    }
}
