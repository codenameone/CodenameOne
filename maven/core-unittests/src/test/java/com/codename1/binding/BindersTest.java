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
package com.codename1.binding;

import com.codename1.junit.UITestBase;
import com.codename1.ui.Container;
import com.codename1.ui.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BindersTest extends UITestBase {

    /** A trivial model class used to key the registry. */
    static final class Model {
    }

    /** Another model class so we can prove lookups are keyed by type name. */
    static final class OtherModel {
    }

    /** A hand-written binder whose bind() returns a stub without touching UI. */
    static final class StubBinder implements Binder<Model> {
        private final Binding result;

        StubBinder(Binding result) {
            this.result = result;
        }

        public Class<Model> type() {
            return Model.class;
        }

        public Binding bind(Model model, Container container) {
            return result;
        }
    }

    /** A no-op binding handle. */
    static final class StubBinding implements Binding {
        public void refresh() {
        }

        public void commit() {
        }

        public void disconnect() {
        }

        public Validator getValidator() {
            return null;
        }
    }

    /** A notifiable binding that records how often refresh() fired. */
    static final class RecordingBinding implements NotifiableBinding {
        private final Object source;
        int refreshes;

        RecordingBinding(Object source) {
            this.source = source;
        }

        public String modelTypeName() {
            return source.getClass().getName();
        }

        public boolean matches(Object model) {
            return model == source;
        }

        public void refresh() {
            refreshes++;
        }

        public void commit() {
        }

        public void disconnect() {
        }

        public Validator getValidator() {
            return null;
        }
    }

    @Test
    void registerThenGetReturnsBinder() {
        StubBinder binder = new StubBinder(new StubBinding());
        Binders.register(binder);
        assertSame(binder, Binders.get(Model.class));
    }

    @Test
    void getReturnsNullForUnregisteredOrNullType() {
        assertNull(Binders.get(OtherModel.class));
        assertNull(Binders.get(null));
    }

    @Test
    void registerRejectsNullBinder() {
        assertThrows(IllegalArgumentException.class, () -> Binders.register(null));
    }

    @Test
    void bindDelegatesToRegisteredBinder() {
        StubBinding expected = new StubBinding();
        Binders.register(new StubBinder(expected));
        Container c = new Container();
        assertSame(expected, Binders.bind(new Model(), c));
    }

    @Test
    void bindRejectsNullModelAndContainer() {
        Binders.register(new StubBinder(new StubBinding()));
        assertThrows(IllegalArgumentException.class, () -> Binders.bind(null, new Container()));
        assertThrows(IllegalArgumentException.class, () -> Binders.bind(new Model(), null));
    }

    @Test
    void bindWithoutRegisteredBinderThrowsIllegalState() {
        // OtherModel has no binder registered.
        assertThrows(IllegalStateException.class,
                () -> Binders.bind(new OtherModel(), new Container()));
    }

    @Test
    void updateRegionDepthNestsAndUnwinds() {
        assertFalse(Binders.isInUpdate());
        Binders.enterUpdate();
        assertTrue(Binders.isInUpdate());
        Binders.enterUpdate();
        assertTrue(Binders.isInUpdate());
        Binders.exitUpdate();
        // Still inside after one of two exits.
        assertTrue(Binders.isInUpdate());
        Binders.exitUpdate();
        assertFalse(Binders.isInUpdate());
    }

    @Test
    void exitUpdateWithoutEnterIsHarmless() {
        assertFalse(Binders.isInUpdate());
        Binders.exitUpdate();
        assertFalse(Binders.isInUpdate());
    }

    @Test
    void notifyChangedRefreshesMatchingBinding() {
        Model model = new Model();
        RecordingBinding binding = new RecordingBinding(model);
        Binders.registerBinding(binding);
        try {
            Binders.notifyChanged(model);
            assertEquals(1, binding.refreshes);
        } finally {
            Binders.unregisterBinding(binding);
        }
    }

    @Test
    void notifyChangedIgnoresUnrelatedInstanceOfSameClass() {
        Model bound = new Model();
        Model other = new Model();
        RecordingBinding binding = new RecordingBinding(bound);
        Binders.registerBinding(binding);
        try {
            // Same class, different identity -> matches() is false.
            Binders.notifyChanged(other);
            assertEquals(0, binding.refreshes);
        } finally {
            Binders.unregisterBinding(binding);
        }
    }

    @Test
    void notifyChangedIsNoOpInsideUpdateRegion() {
        Model model = new Model();
        RecordingBinding binding = new RecordingBinding(model);
        Binders.registerBinding(binding);
        try {
            Binders.enterUpdate();
            try {
                Binders.notifyChanged(model);
            } finally {
                Binders.exitUpdate();
            }
            assertEquals(0, binding.refreshes);
        } finally {
            Binders.unregisterBinding(binding);
        }
    }

    @Test
    void notifyChangedIgnoresNullAndUnknownModels() {
        // Null model and a class with no live bindings must not throw.
        Binders.notifyChanged(null);
        Binders.notifyChanged(new OtherModel());
    }

    @Test
    void unregisterBindingRemovesIt() {
        Model model = new Model();
        RecordingBinding binding = new RecordingBinding(model);
        Binders.registerBinding(binding);
        Binders.unregisterBinding(binding);
        // After removal a notification no longer reaches it.
        Binders.notifyChanged(model);
        assertEquals(0, binding.refreshes);
    }

    @Test
    void registerBindingAndUnregisterBindingTolerateNull() {
        Binders.registerBinding(null);
        Binders.unregisterBinding(null);
    }
}
