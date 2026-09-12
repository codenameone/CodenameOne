/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.material;

import com.codename1.flutter.Element;
import com.codename1.flutter.StatelessElement;
import com.codename1.flutter.Widget;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.ui.Form;

import java.util.Map;

/**
 * Element for {@link MaterialApp}: installs the app's EFFECTIVE theme (per
 * themeMode/darkTheme) into the CN1 UIManager <b>before</b> the subtree
 * mounts, so every component created below picks the themed Flutter* styles
 * up; on every widget update the effective theme is recomputed and, when it
 * changed (a themeMode/theme/darkTheme switch across rebuilds), the overlay
 * is re-installed, the Form's styles are refreshed and the whole element
 * subtree re-applies its programmatic styling
 * ({@link Element#themeChanged()}).
 */
public class MaterialAppElement extends StatelessElement {

    /** The prop table last installed, for change detection. */
    private Map<String, Object> installedProps;

    /**
     * The widget the initial route resolved to, kept for the life of this element.
     *
     * <p>{@code MaterialApp.build} resolves {@code initialRoute} through
     * {@code onGenerateRoute}. Doing that on EVERY build means any rebuild of the app -
     * and an app-wide model sitting above MaterialApp causes one on every settings change -
     * re-runs the route builder and hands back a brand new page, discarding whatever the
     * user was looking at. Flutter does not re-resolve, because the route stack is
     * Navigator STATE rather than something recomputed from the widget.</p>
     *
     * <p>Resolved once and reused, so a rebuild updates the existing page in place. Routes
     * pushed later are their own Forms and are unaffected.</p>
     */
    private Widget routeContent;

    public Widget routeContent() {
        return routeContent;
    }

    public void routeContent(Widget v) {
        this.routeContent = v;
    }

    public MaterialAppElement(MaterialApp widget) {
        super(widget);
    }

    private MaterialApp app() {
        return (MaterialApp) widget();
    }

    @Override
    public void mount(Element parent, int slot) {
        // Install before super.mount: the children inflate (and create their
        // CN1 components) during the first build inside super.mount.
        RenderHost h = parent != null ? parent.host() : host();
        installEffectiveTheme(app().effectiveTheme(), h);
        super.mount(parent, slot);
    }

    // No size-changed listener here, deliberately. One was added to keep a
    // root MediaQuery snapshot honest across a window resize; that snapshot
    // turned out to be wrong for a different reason and was removed, leaving a
    // listener that rebuilt the ENTIRE application every time the Form
    // reported a size -- which a desktop window does once, just after it is
    // shown. Measured on the Mac build that was the whole first screen built
    // twice: 2099 elements and 751 components where the app has 1062 and 376.
    // MediaQuery.of resolves against the Display at the moment it is asked, so
    // there is nothing here that a resize can invalidate.

    @Override
    public void update(Widget newWidget) {
        ThemeData eff = ((MaterialApp) newWidget).effectiveTheme();
        boolean changed = !ThemeDataAdapter.themeProps(eff).equals(installedProps);
        if (changed) {
            installEffectiveTheme(eff, host());
        }
        super.update(newWidget);
        if (changed) {
            // Reused widget instances skip Element.update, so force every
            // render element to re-apply its (theme-derived) programmatic
            // styling and re-measure.
            themeChanged();
            if (host() != null) {
                host().revalidate();
            }
        }
    }

    private void installEffectiveTheme(ThemeData eff, RenderHost h) {
        installedProps = ThemeDataAdapter.themeProps(eff);
        ThemeDataAdapter.install(eff);
        Form f = h == null ? null : h.form();
        if (f != null) {
            // Re-derive the existing components' UIID styles from the new
            // overlay, then style the Form itself per-instance.
            try {
                f.refreshTheme();
            } catch (Throwable ignore) {
                // headless
            }
            ThemeDataAdapter.applyToForm(f, eff);
        }
    }
}
