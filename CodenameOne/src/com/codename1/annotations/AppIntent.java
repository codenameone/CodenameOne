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
package com.codename1.annotations;

import com.codename1.intents.Exposure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Exposes a capability of your application to the system: Siri, Spotlight, the
/// Shortcuts app, an Android launcher shortcut, a widget button, or a language
/// model.
///
/// ```java
/// @AppIntent(value = "log_workout", title = "Log a workout",
///         description = "Records a completed workout",
///         phrases = {"Log a workout in ${applicationName}"},
///         headless = true, timeoutSeconds = 5)
/// public static IntentResult logWorkout(
///         @IntentParam(value = "kind", title = "What kind of workout?",
///                      options = {"run", "ride", "swim"}) String kind,
///         @IntentParam(value = "minutes", title = "How many minutes?") int minutes) {
///     WorkoutStore.append(kind, minutes);
///     return IntentResult.spoken("Logged a " + minutes + " minute " + kind + ".");
/// }
/// ```
///
/// #### The handler must be `public static`
///
/// This is not a style preference. The build generates a **direct static call**
/// to your method, and a direct call is the only form that survives the iOS
/// translator's dead-code elimination and Android's obfuscation -- a reflective
/// lookup would be stripped on iOS and renamed on Android, in both cases
/// silently. `static` also makes the contract visible: a handler can be asked to
/// run in a process that exists only to answer it, where no instance of yours
/// has been constructed and nothing is on screen.
///
/// The method returns `com.codename1.intents.IntentResult`, or `void` when it
/// has nothing to report. It may take `com.codename1.intents.IntentContext` as
/// its first parameter to see the deadline, the source and the cancellation
/// flag. Every other parameter must carry [IntentParam].
///
/// #### At build time
///
/// The Codename One Maven plugin scans the project's compiled bytecode,
/// validates every `@AppIntent`, and generates both the reflection-free dispatch
/// table and the native declarations each platform compiles into the app. A
/// malformed declaration fails the build rather than going quiet on a device.
///
/// #### Phrases
///
/// Apple enforces three rules on a spoken phrase, all of them as build failures
/// that produce no App Intents metadata at all. The build checks them here
/// instead, so the message names your declaration rather than arriving as an
/// opaque failure from `appintentsmetadataprocessor`:
///
/// - Every phrase must contain `${applicationName}`.
/// - A phrase may reference **at most one** parameter. Write one phrase per
///   parameter rather than combining them.
/// - A phrase parameter must be an [IntentEntity] type. A primitive cannot
///   appear in a phrase, which is not much of a loss: leave it out and the
///   platform still asks for it, using the title on its [IntentParam].
///
/// An intent that declares phrases must also be `discoverable`, since a phrase
/// is only reachable through an App Shortcut.
///
/// Phrases are ignored on platforms with no voice invocation, which today means
/// Android.
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface AppIntent {

    /// The stable id, matching `[a-z][a-z0-9_]{2,63}`. Required.
    ///
    /// It is stable in the strong sense: the system stores it in donated
    /// shortcuts and the user's own Shortcuts workflows, so renaming one breaks
    /// what people already built.
    String value();

    /// The human-readable name shown in the Shortcuts app. Required.
    String title();

    /// A longer explanation shown alongside the title.
    String description() default "";

    /// Spoken phrases that invoke this intent. Each must contain
    /// `${applicationName}`.
    String[] phrases() default {};

    /// True when this intent may run without bringing the app to the foreground.
    ///
    /// A headless handler must not touch `Form`, `Dialog`, or anything else
    /// needing a window; see the `com.codename1.intents` package documentation
    /// for the full contract.
    boolean headless() default false;

    /// True when the platform may offer this intent before the user has ever run
    /// it. False restricts it to appearing after a donation.
    boolean discoverable() default true;

    /// True when the platform should confirm with the user before running this.
    /// Set it on anything that deletes, sends, or spends.
    ///
    /// It also closes the paths that cannot confirm. A destructive intent is not published as
    /// an Android launcher shortcut, is refused when an unauthenticated caller asks for it, and
    /// is not donated -- each of those runs on a single tap with nothing in between. The
    /// capability stays fully available through the assistant and the Shortcuts app, which
    /// confirm first; what goes away is the one-tap route to it.
    boolean destructive() default false;

    /// A route template to open instead of answering in place, for example
    /// `/orders/{orderId}`, where each `{name}` names one of this intent's
    /// parameters.
    ///
    /// The URL is resolved through the same [Route] table that handles deep
    /// links, so an intent and a link to the same screen cannot drift apart.
    /// A non-empty value is also what tells the platform to open the app.
    String opensRoute() default "";

    /// Which consumers this intent is offered to. Defaults to the platform only;
    /// add `Exposure.MODEL` to also offer it to a language model through
    /// `com.codename1.intents.Intents#asTools()`.
    Exposure[] exposure() default {Exposure.ASSISTANT};

    /// How long the handler may run before the framework reports a failure.
    ///
    /// The platform usually allows around twenty seconds. Do not use them: a
    /// spoken interaction that takes ten seconds has already failed as an
    /// interaction. Aim under two, and return `IntentResult.opens(...)` for
    /// anything genuinely slower.
    int timeoutSeconds() default 20;
}
