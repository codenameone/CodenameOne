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

/// App intents: the capabilities your application offers to the world outside it.
///
/// Siri, Spotlight, the Shortcuts app, an Android launcher shortcut, a button on
/// a home-screen widget and a language model are all asking your app the same
/// question -- *what can you do, and will you do it now?* -- so Codename One
/// models them as one concept. You declare a capability once and the framework
/// projects it to each of them.
///
/// #### Three nouns
///
/// An **intent** is a named, titled, invokable capability. It takes
/// **parameters**, which are either primitives or **entities** -- your app's own
/// nouns, which the platform can list, search and ask the user to choose between
/// before your code ever runs. It produces an **IntentResult**: a value, a line
/// to speak, a snippet to show, a route to open, or any combination.
///
/// #### Declaring one
///
/// A handler is a `public static` method carrying
/// `com.codename1.annotations.AppIntent`:
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
/// It is `static` for a reason that matters on iOS: the build generates a direct
/// call to it, and a direct call is the only kind that survives the iOS
/// translator's dead-code elimination and Android's obfuscation. There is no
/// reflection anywhere in this framework, because on a translated iOS build
/// there is no reflection to have.
///
/// #### Intents and routes are different questions
///
/// A `com.codename1.annotations.Route` answers *"given this URL, what do I
/// show?"* and hands back a `Form`. An intent answers *"given this verb and
/// these arguments, what do I do and what do I return?"* and hands back a value.
/// The one place they meet is an intent that opens the app, so that is the one
/// place they are joined: declare `opensRoute` and the framework builds the URL
/// from the bound parameters and navigates through the existing route table. One
/// screen, one route pattern, two ways in.
///
/// #### The headless contract
///
/// A `headless` intent can run in a process the system started only to answer
/// it, with nothing on screen. That is what lets an assistant answer without
/// your app appearing, and it is a real constraint rather than a flag.
///
/// A headless handler **may** use `Storage`, `Preferences`, `Database`,
/// `NetworkManager`, `Log`, `com.codename1.surfaces` publishing and
/// [Intents#index]. It **must not** touch `Form`, `Dialog`, `Component`,
/// `Display#getCurrent()`, the camera, capture, or anything else that needs a
/// window: there is no window. It must also not call
/// `Display#callSeriallyAndWait`, which can deadlock against a foregrounded
/// event dispatch thread.
///
/// Handlers never run on the event dispatch thread, on any platform and from any
/// source. An invocation can arrive while your app is visible -- a widget button,
/// a search result tapped on a running app -- and a handler blocking the event
/// thread there is a visible freeze. Since handlers are forbidden from touching
/// UI, they do not need that thread.
///
/// #### Time
///
/// Every invocation carries a deadline, and [IntentContext#isCancelled()] flips
/// when it passes. Cancellation is cooperative: nothing interrupts a running
/// handler, and anything it returns afterwards is discarded, so commit durable
/// work to storage as you go rather than only at the end.
///
/// The platform will usually allow around twenty seconds. Do not use them. A
/// spoken interaction that takes ten seconds has already failed as an
/// interaction; aim under two, and for anything genuinely slower return
/// [IntentResult#opens] and do the work in the app where there is a UI to show
/// progress in.
///
/// #### What each platform actually does
///
/// | Capability | iOS | Android | Simulator | Other ports |
/// |---|---|---|---|---|
/// | Declared intent | App Intent + App Shortcut | launcher shortcut | Intents window | declaration only |
/// | Voice invocation | Siri, from `phrases` | **none** | typed in the window | none |
/// | Headless execution | background launch | service, no Activity | in-process | in-process |
/// | Open a route | native, then the route table | trampoline, then the route table | shows the form | route only |
/// | AppEntity parameters | system picker and search | passed by id | picker from the real queries | by id |
/// | System disambiguation | yes | **no** -- the app foregrounds its own picker | in-app picker | in-app picker |
/// | Content indexing | device search | launcher shortcuts | searchable list | no-op |
/// | Donation | learned suggestions | dynamic shortcuts | logged | no-op |
/// | Spoken result | spoken aloud | **dropped** | shown as text | n/a |
/// | Snippet | rendered natively | notification, opt-in | rasterized preview | n/a |
/// | Returned value | piped to the next action | result extras | shown | returned |
///
/// **Android is not Siri parity and this framework does not pretend otherwise.**
/// Android has no assistant contract that hands a typed result back to an app,
/// so phrases, system disambiguation and spoken results are iOS-only.
/// [Intents#isVoiceInvocationSupported()] is the honest thing to branch on.
///
/// #### The floor everything else sits on
///
/// An intent is guaranteed only to be *invokable with its parameters supplied*
/// and to *produce a value*. Everything else -- phrases, pickers, speech,
/// snippets, indexing, donation -- is an enhancement some platform may not
/// offer. Write each handler so it is still correct when nothing arrives but its
/// parameters and nothing is consumed but its return value, and it will behave
/// the same everywhere.
///
/// Because dispatch is generated code rather than a platform service,
/// [Intents#invoke] works on **every** port, including those with no intent
/// support at all. An app can use its own intents as an internal command layer
/// and get the platform integration as a bonus where it exists.
///
/// #### Zero cost when unused
///
/// Referencing this package is what makes the build inject the native plumbing.
/// An application that never touches `com.codename1.intents` gets none of it and
/// builds byte-for-byte as it did before.
///
/// #### What this costs your iOS deployment target
///
/// Nothing, unless you declare an intent -- and possibly nothing even then.
///
/// Indexing and donation ([Intents#index], [Intents#donate]) run on Core
/// Spotlight and `NSUserActivity`, which have been available since long before
/// the framework's current minimum, so an application that only publishes
/// content to device search never changes its deployment target. That is most of
/// this feature's value for most applications, available at no cost.
///
/// Declaring an `AppIntent` brings in Apple's App Intents framework, which needs
/// a newer minimum. Every generated type is availability-guarded, so the
/// intended outcome is that your target is untouched and the intents simply do
/// not appear on older devices. Should a build toolchain refuse that, the build
/// raises the floor **only for applications that declare an intent**, and only
/// as far as that framework requires; set the `ios.intents.appIntents` build
/// hint to `false` to keep indexing and donation while suppressing App Intents
/// generation entirely.
///
/// If you have pinned a deployment target below what a declared intent needs,
/// the build says so and stops rather than quietly moving your pin or quietly
/// dropping your intents.
package com.codename1.intents;
