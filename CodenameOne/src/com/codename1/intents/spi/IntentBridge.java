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
package com.codename1.intents.spi;

import java.util.Map;

/// The platform seam of the app intents framework, implemented by ports and
/// returned from `CodenameOneImplementation.getIntentBridge()` -- null on
/// unsupported ports, which makes the whole public API an inert no-op.
///
/// Everything crosses this boundary as data: JSON strings produced by the core
/// serializer plus named PNG blobs, never live model objects. The reason is the
/// same one the surfaces framework gives: the peer on the other side is Swift or
/// Kotlin, and an invocation can arrive while the app process has no UI and was
/// started only to answer it. Keeping the wire format to strings is also what
/// leaves the door open to hosting intents in a separate process later without
/// changing a line of Java.
///
/// Invocations travel the other way: the port decodes its platform payload and
/// calls `com.codename1.intents.Intents.dispatchInvocation`, which owns thread
/// marshalling, the cold-start queue and the deadline.
public interface IntentBridge {

    /// True when this port can expose intents to the platform at all.
    boolean areIntentsSupported();

    /// True when this port can run an intent without bringing the app to the
    /// foreground.
    boolean isHeadlessExecutionSupported();

    /// True when a voice assistant can invoke intents on this port. False on
    /// Android, where no assistant contract of that shape exists.
    boolean isVoiceInvocationSupported();

    /// True when this port can publish app content to a system-wide search index.
    boolean isIndexingSupported();

    /// Hands the port the application's full intent catalogue during startup, so
    /// it can validate against what was compiled into the native app and prepare
    /// whatever the platform needs.
    ///
    /// #### Parameters
    ///
    /// - `declarationsJson`: the serialized declarations
    void registerIntents(String declarationsJson);

    /// Tells the platform the user just ran this intent, so it can suggest it
    /// later.
    ///
    /// #### Parameters
    ///
    /// - `intentId`: the intent that ran
    /// - `paramsJson`: the parameter values it ran with
    void donate(String intentId, String paramsJson);

    /// Publishes app content to the system search index, replacing any entry
    /// with the same id.
    ///
    /// #### Parameters
    ///
    /// - `entitiesJson`: the serialized entities
    /// - `images`: PNG blobs keyed by the name used in the JSON; may be empty,
    ///   never null
    void index(String entitiesJson, Map<String, byte[]> images);

    /// Removes specific entries from the system search index.
    ///
    /// #### Parameters
    ///
    /// - `idsJson`: the serialized `{type, id}` pairs to remove
    void removeFromIndex(String idsJson);

    /// Removes every entry of one type, or the whole index.
    ///
    /// #### Parameters
    ///
    /// - `entityType`: the type to clear, or null for everything this app indexed
    void clearIndex(String entityType);

    /// Hands the platform the outcome of an invocation it started.
    ///
    /// Called at most once per token; the framework enforces that, because the
    /// iOS side of this boundary crashes when a continuation is resumed twice.
    ///
    /// #### Parameters
    ///
    /// - `token`: the invocation token the port supplied
    /// - `resultJson`: the serialized result
    /// - `images`: PNG blobs referenced by a snippet; may be empty, never null
    void completeInvocation(String token, String resultJson, Map<String, byte[]> images);
}
