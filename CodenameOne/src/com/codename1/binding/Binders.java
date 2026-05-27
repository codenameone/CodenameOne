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

import com.codename1.ui.Container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/// Public entry point for the build-time component binding framework.
///
/// `@Bindable` classes get a generated binder at build time. Each binder's
/// static initializer self-registers with this registry. The registry stays
/// empty until something triggers each generated class's `<clinit>`:
///
/// - **iOS / Android** -- the build server probes the project zip for
///   `cn1app.BinderBootstrap` and splices a
///   `new cn1app.BinderBootstrap();` into the per-build application stub
///   before `Display.init`.
/// - **JavaSE simulator + desktop** -- `JavaSEPort#postInit` calls
///   `Class.forName("cn1app.BinderBootstrap")` so the registry is
///   populated on the same boundary.
/// - **Unit tests / manual init** -- application code can call
///   `Binders.register(...)` directly.
///
/// ## Two-way bindings and the change-notification contract
///
/// A binding declared `twoWay = true` flows in both directions:
///
/// 1. **Component -> model.** The generated binder installs a listener on
///    the editable component (`TextField`, `CheckBox`, etc.). When the
///    user mutates the component, the binder calls the model's setter (or
///    writes the public field directly), and the setter's
///    instrumented exit calls `notifyChanged(this)`.
/// 2. **Model -> component.** Application code that mutates the model
///    through a setter (instrumented by the build-time processor) causes
///    `notifyChanged(this)` to fire, which walks every live binding for
///    that model and pushes the new value into the matching component.
///
/// The two paths together would loop forever if left to themselves -- the
/// model setter fires a change event, which refreshes the component, which
/// fires its own change event, which calls the model setter again. To
/// break the loop, every framework-initiated mutation runs inside an
/// "update region" guarded by a thread-local flag. While the flag is set,
/// `notifyChanged` is a no-op, and component listeners short-circuit.
///
/// Concretely:
///
/// - `Binders.bind(model, container)` enters an update region for the
///   initial model -> component push.
/// - `Binding#refresh()` enters an update region for every subsequent
///   model -> component push.
/// - `Binding#commit()` enters an update region for the component -> model
///   pull.
/// - The generated component listener enters an update region before
///   calling the setter.
///
/// **Limitation:** when a setter synchronously mutates a *second* bound
/// field (e.g. `setFirstName` also calls `setFullName`), the second field's
/// notification is also suppressed -- the user must call
/// `binding.refresh()` explicitly if they want the second component to
/// catch up. See `Annotation-Component-Binding.asciidoc` for details.
public final class Binders {

    private static final Map<String, Binder<?>> BY_NAME = new HashMap<String, Binder<?>>();

    /// Active bindings keyed by `model.getClass().getName()` so
    /// `notifyChanged(model)` can iterate them in O(matches). Each live
    /// binding registers itself in `Binders#registerBinding` from inside
    /// `Binder#bind`; the binding removes itself on `disconnect`.
    private static final Map<String, List<NotifiableBinding>> LIVE_BINDINGS =
            new HashMap<String, List<NotifiableBinding>>();

    private static final ThreadLocal<int[]> IN_UPDATE = new ThreadLocal<int[]>();

    private Binders() {
    }

    /// Installs `binder` under `binder.type().getName()`. The generated
    /// per-class binder's static initializer calls this; hand-written
    /// binders for classes outside the build's annotation scan call it
    /// explicitly.
    public static <T> void register(Binder<T> binder) {
        if (binder == null) {
            throw new IllegalArgumentException("binder is null");
        }
        BY_NAME.put(binder.type().getName(), binder);
    }

    /// Looks up the binder for `type` (by `type.getName()`) or null when
    /// none is registered.
    @SuppressWarnings("unchecked")
    public static <T> Binder<T> get(Class<T> type) {
        if (type == null) {
            return null;
        }
        return (Binder<T>) BY_NAME.get(type.getName());
    }

    /// Pushes the model values into the matching components in
    /// `container`, wiring up the two-way listeners declared on its
    /// `@Bind` fields. Throws `IllegalStateException` when no binder is
    /// registered for `model.getClass()`.
    @SuppressWarnings("unchecked")
    public static <T> Binding bind(T model, Container container) {
        if (model == null) {
            throw new IllegalArgumentException("model is null");
        }
        if (container == null) {
            throw new IllegalArgumentException("container is null");
        }
        Binder<T> binder = (Binder<T>) BY_NAME.get(model.getClass().getName());
        if (binder == null) {
            throw new IllegalStateException("No binder registered for "
                    + model.getClass().getName() + ". Add @Bindable and ensure the "
                    + "cn1:process-annotations Mojo ran during build, then re-run -- the "
                    + "generated BinderBootstrap populates this registry at startup.");
        }
        return binder.bind(model, container);
    }

    // ---------------------------------------------------------------
    // Binding-aware change notification
    // ---------------------------------------------------------------

    /// Called from the build-time-instrumented setter at every return
    /// point. Walks the live bindings for `model.getClass().getName()`
    /// and refreshes those whose source is `model`. Short-circuits when
    /// the calling thread is already inside an update region, breaking
    /// the model -> component -> model loop.
    ///
    /// Application code rarely calls this directly; the build-time
    /// instrumentation wires it into generated setters automatically.
    public static void notifyChanged(Object model) {
        if (model == null) {
            return;
        }
        if (isInUpdate()) {
            return;
        }
        List<NotifiableBinding> bindings = LIVE_BINDINGS.get(model.getClass().getName());
        if (bindings == null || bindings.isEmpty()) {
            return;
        }
        // Snapshot so a refresh that disconnects/reinstalls bindings
        // doesn't break the iteration.
        NotifiableBinding[] snapshot;
        synchronized (LIVE_BINDINGS) {
            snapshot = bindings.toArray(new NotifiableBinding[0]);
        }
        for (NotifiableBinding b : snapshot) {
            if (b.matches(model)) {
                enterUpdate();
                try {
                    b.refresh();
                } finally {
                    exitUpdate();
                }
            }
        }
    }

    /// Generated binders call this to enroll a new live binding in the
    /// per-class registry. Call `unregisterBinding` from `disconnect` to
    /// remove it.
    public static void registerBinding(NotifiableBinding binding) {
        if (binding == null) {
            return;
        }
        synchronized (LIVE_BINDINGS) {
            List<NotifiableBinding> list = LIVE_BINDINGS.get(binding.modelTypeName());
            if (list == null) {
                list = new ArrayList<NotifiableBinding>();
                LIVE_BINDINGS.put(binding.modelTypeName(), list);
            }
            list.add(binding);
        }
    }

    /// Inverse of `registerBinding`. Called from
    /// `Binding#disconnect`.
    public static void unregisterBinding(NotifiableBinding binding) {
        if (binding == null) {
            return;
        }
        synchronized (LIVE_BINDINGS) {
            List<NotifiableBinding> list = LIVE_BINDINGS.get(binding.modelTypeName());
            if (list == null) {
                return;
            }
            for (Iterator<NotifiableBinding> it = list.iterator(); it.hasNext(); ) {
                NotifiableBinding b = it.next();
                if (b == binding) { //NOPMD CompareObjectsWithEquals -- identity dedup
                    it.remove();
                    break;
                }
            }
            if (list.isEmpty()) {
                LIVE_BINDINGS.remove(binding.modelTypeName());
            }
        }
    }

    /// Enter an update region. Generated binder code calls this around
    /// every framework-initiated mutation -- model->component pushes and
    /// component->model pulls -- so the setter notification and the
    /// component change listener both short-circuit while we're inside.
    public static void enterUpdate() {
        int[] depth = IN_UPDATE.get();
        if (depth == null) {
            depth = new int[]{0};
            IN_UPDATE.set(depth);
        }
        depth[0]++;
    }

    /// Exit an update region. Call once per `enterUpdate`.
    public static void exitUpdate() {
        int[] depth = IN_UPDATE.get();
        if (depth == null) {
            return;
        }
        depth[0]--;
        if (depth[0] <= 0) {
            IN_UPDATE.remove();
        }
    }

    /// True while the calling thread is inside a binding update region.
    /// Generated component listeners check this and bail out early.
    public static boolean isInUpdate() {
        int[] depth = IN_UPDATE.get();
        return depth != null && depth[0] > 0;
    }
}
