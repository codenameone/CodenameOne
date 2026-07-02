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
package com.codename1.car;

/// One screen of the in-car experience -- the unit of the head-unit back stack. A screen produces a
/// single `CarTemplate` from [#onCreateTemplate()]; the framework pulls it when the screen is pushed
/// or invalidated. Conceptually equivalent to `androidx.car.app.Screen`; on CarPlay a screen
/// corresponds to one pushed `CPTemplate`.
///
/// ```java
/// class AlbumsScreen extends CarScreen {
///     protected CarTemplate onCreateTemplate() {
///         CarListTemplate t = new CarListTemplate().setTitle("Albums");
///         for (Album a : albums) {
///             t.addRow(new CarRow(a.name).setImage(a.art)
///                 .setBrowsable(true)
///                 .setOnAction(ctx -> ctx.pushScreen(new TracksScreen(a))));
///         }
///         return t;
///     }
/// }
/// ```
public abstract class CarScreen {
    private CarContext context;

    /// Builds the template to display for this screen. Called by the framework when the screen is
    /// pushed and again after [#invalidate()]. Must return a non-null `CarTemplate`.
    ///
    /// #### Returns
    ///
    /// the template to render on the head unit
    protected abstract CarTemplate onCreateTemplate();

    /// Lifecycle hook invoked once, when the screen is first pushed onto the stack, before its
    /// template is built. Default is a no-op.
    protected void onCreate() {
    }

    /// Lifecycle hook invoked when the screen becomes the visible top of stack. Default is a no-op.
    protected void onResume() {
    }

    /// Lifecycle hook invoked when the screen is covered by a pushed screen. Default is a no-op.
    protected void onPause() {
    }

    /// Lifecycle hook invoked when the screen is popped and discarded. Default is a no-op.
    protected void onDestroy() {
    }

    /// Returns the context this screen is attached to, or null if it has not been pushed yet.
    ///
    /// #### Returns
    ///
    /// the car context, or null
    public final CarContext getContext() {
        return context;
    }

    /// Rebuilds and re-renders this screen's template (after the model behind it changed). No-op when
    /// the screen is not currently attached or no head unit is connected.
    public final void invalidate() {
        if (context != null) {
            context.invalidateScreen(this);
        }
    }

    /// Pops this screen off the stack (equivalent to `getContext().popScreen()` when this is the top
    /// screen). No-op when not attached.
    public final void finish() {
        if (context != null) {
            context.popScreen();
        }
    }

    // --- framework internals -------------------------------------------------

    final void attach(CarContext ctx) {
        this.context = ctx;
    }

    final void detach() {
        this.context = null;
    }

    /// Framework entry point -- pulls the template, used by the platform `CarBridge`.
    public final CarTemplate dispatchCreateTemplate() {
        return onCreateTemplate();
    }

    /// Framework entry point -- dispatches the {@code onCreate} lifecycle callback.
    public final void dispatchCreate() {
        onCreate();
    }

    /// Framework entry point -- dispatches the {@code onResume} lifecycle callback.
    public final void dispatchResume() {
        onResume();
    }

    /// Framework entry point -- dispatches the {@code onPause} lifecycle callback.
    public final void dispatchPause() {
        onPause();
    }

    /// Framework entry point -- dispatches the {@code onDestroy} lifecycle callback.
    public final void dispatchDestroy() {
        onDestroy();
    }
}
