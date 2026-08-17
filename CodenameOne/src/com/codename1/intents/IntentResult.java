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
package com.codename1.intents;

import com.codename1.surfaces.SurfaceNode;

/// What an intent handler hands back.
///
/// A result carries up to four independent things, and every consumer takes the
/// parts it understands and ignores the rest:
///
/// - a **value**, which the Shortcuts app pipes into the next action
/// - a **spoken line**, which an assistant reads aloud
/// - a **snippet**, a small layout shown alongside the answer
/// - an **open route**, which continues the interaction inside the app
///
/// Nothing is mandatory. `IntentResult.ok()` is a complete, valid answer meaning
/// "done, nothing to report".
///
/// ```java
/// return IntentResult.value(orderId)
///         .withDialog("Your coffee is on the way")
///         .withSnippet(orderCard);
/// ```
///
/// #### The snippet is a surface, not a Form
///
/// A snippet is rendered by the platform while your app may not be on screen, so
/// it uses the `com.codename1.surfaces` node catalog and obeys the same
/// dead-process rule: it serializes to data at the moment you return it. That is
/// also why there is no way to hand back a `Form` -- a live component tree has
/// nowhere to live once the handler returns.
public final class IntentResult {

    private final boolean failed;
    private Object value;
    private String dialog;
    private SurfaceNode snippet;
    private String openUrl;
    private Entity entity;
    private String errorMessage;

    private IntentResult(boolean failed) {
        this.failed = failed;
    }

    /// A successful result with nothing to report.
    public static IntentResult ok() {
        return new IntentResult(false);
    }

    /// A successful result carrying a value the platform can pipe onward.
    ///
    /// #### Parameters
    ///
    /// - `value`: a String, Number, Boolean or Date
    public static IntentResult value(Object value) {
        IntentResult r = new IntentResult(false);
        r.value = value;
        return r;
    }

    /// A successful result whose only content is a line for the assistant to
    /// speak. Shorthand for `ok().withDialog(spoken)`.
    ///
    /// #### Parameters
    ///
    /// - `spoken`: the line to speak
    public static IntentResult spoken(String spoken) {
        IntentResult r = new IntentResult(false);
        r.dialog = spoken;
        return r;
    }

    /// A successful result identifying one of the app's nouns, so the platform
    /// can offer it as the input to a following action.
    ///
    /// #### Parameters
    ///
    /// - `e`: the entity produced
    public static IntentResult entity(Entity e) {
        IntentResult r = new IntentResult(false);
        r.entity = e;
        return r;
    }

    /// A result that opens the app at a route rather than answering in place.
    ///
    /// The URL is resolved through the same `com.codename1.annotations.Route`
    /// table that handles deep links, so an intent and a link to the same screen
    /// stay in agreement by construction.
    ///
    /// #### Parameters
    ///
    /// - `routeUrl`: the route to navigate to, e.g. `/orders/42`
    public static IntentResult opens(String routeUrl) {
        IntentResult r = new IntentResult(false);
        r.openUrl = routeUrl;
        return r;
    }

    /// A failed result. The message is shown or spoken to the user, so write it
    /// for them rather than for a log.
    ///
    /// #### Parameters
    ///
    /// - `userVisibleMessage`: what went wrong, in the user's terms
    public static IntentResult failed(String userVisibleMessage) {
        IntentResult r = new IntentResult(true);
        r.errorMessage = userVisibleMessage;
        return r;
    }

    /// Adds the line an assistant speaks.
    ///
    /// #### Parameters
    ///
    /// - `spoken`: the line to speak
    ///
    /// #### Returns
    ///
    /// this result, for chaining
    public IntentResult withDialog(String spoken) {
        this.dialog = spoken;
        return this;
    }

    /// Adds a small layout shown alongside the answer.
    ///
    /// #### Parameters
    ///
    /// - `node`: the surface node tree to render
    ///
    /// #### Returns
    ///
    /// this result, for chaining
    public IntentResult withSnippet(SurfaceNode node) {
        this.snippet = node;
        return this;
    }

    /// Adds a route to open after the result is presented.
    ///
    /// #### Parameters
    ///
    /// - `routeUrl`: the route to navigate to
    ///
    /// #### Returns
    ///
    /// this result, for chaining
    public IntentResult withOpenUrl(String routeUrl) {
        this.openUrl = routeUrl;
        return this;
    }

    /// True when the handler reported a failure.
    public boolean isFailed() {
        return failed;
    }

    /// The user-visible failure message, or null on success.
    public String getErrorMessage() {
        return errorMessage;
    }

    /// The value carried onward, or null.
    public Object getValue() {
        return value;
    }

    /// The line for an assistant to speak, or null.
    public String getDialog() {
        return dialog;
    }

    /// The snippet layout, or null.
    public SurfaceNode getSnippet() {
        return snippet;
    }

    /// The route to open, or null.
    public String getOpenUrl() {
        return openUrl;
    }

    /// The entity produced, or null.
    public Entity getEntity() {
        return entity;
    }

    @Override
    public String toString() {
        if (failed) {
            return "IntentResult[failed: " + errorMessage + "]";
        }
        return "IntentResult[value=" + value + ", dialog=" + dialog + "]";
    }
}
