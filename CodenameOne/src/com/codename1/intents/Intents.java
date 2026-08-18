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

import com.codename1.ai.Tool;
import com.codename1.ai.ToolHandler;
import com.codename1.intents.spi.IntentBridge;
import com.codename1.io.JSONWriter;
import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.util.StringUtil;
import com.codename1.ui.Display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The entry point for app intents: the capabilities your application offers to
/// the outside world.
///
/// You do not register intents here. You declare them with
/// `com.codename1.annotations.AppIntent` on a `public static` method and the
/// build generates the table, because the platforms compile their intent
/// catalogues into the native binary and a runtime-only registration could never
/// reach them. What this class gives you is everything around that declaration:
/// running an intent yourself, telling the system one just happened, publishing
/// content to device search, and asking what the current platform can actually
/// do.
///
/// ```java
/// // In your application code, once:
/// @AppIntent(value = "log_workout", title = "Log a workout",
///         phrases = {"Log a workout in ${applicationName}"}, headless = true)
/// public static IntentResult logWorkout(
///         @IntentParam("minutes") int minutes) {
///     WorkoutStore.append(minutes);
///     return IntentResult.spoken("Logged " + minutes + " minutes.");
/// }
///
/// // Later, after the user does it by hand, so the system learns to suggest it:
/// Intents.donate("log_workout", params);
/// ```
///
/// #### What is honestly supported where
///
/// Ask, do not assume. [#areIntentsSupported()] is true wherever intents can be
/// exposed to the platform at all, but the interesting question is usually
/// [#isVoiceInvocationSupported()], which is true on iOS and false on Android --
/// Android has no assistant contract that hands a typed result back to an app.
/// Android gets launcher shortcuts and headless execution; it does not get Siri.
/// The package documentation has the full table.
///
/// #### Zero cost when unused
///
/// Referencing this package is what makes the build inject the native plumbing.
/// An application that never touches `com.codename1.intents` gets none of it and
/// builds exactly as it did before.
public final class Intents {

    private static IntentBridge bridge;
    private static boolean bridgeOverridden;
    private static IntentDispatcher dispatcher;
    private static int defaultTimeoutSeconds = 20;

    /// Intents declared at runtime rather than by the build. Kept separate from
    /// the generated table so [#getDeclarations()] can present one list without
    /// either source being able to corrupt the other.
    private static final Map<String, DynamicIntent> dynamic =
            new LinkedHashMap<String, DynamicIntent>();

    /// Invocations that arrived before the generated dispatcher installed
    /// itself -- the cold start where a tap on a shortcut is what launched the
    /// process. Drained in arrival order once the dispatcher appears.
    private static final List<PendingInvocation> pending =
            new ArrayList<PendingInvocation>();

    private static EntitySelectionHandler selectionHandler;

    /// Search-result taps that arrived before a handler was registered -- the
    /// common case, since a tap is often what launched the process.
    private static final List<AppEntity> pendingSelections = new ArrayList<AppEntity>();

    /// Platform activities that arrived before the declarations did. Drained once the generated
    /// dispatcher installs itself, and dropped if it turns out nothing declares them.
    ///
    /// Guarded by `pending`, the dispatcher's own lock, rather than by itself. "Is the
    /// dispatcher installed, and if not, queue this" has to be one decision: with two locks the
    /// dispatcher can install and drain an empty queue in the window between the answer and the
    /// enqueue, and the activity then sits here forever with nothing left to drain it -- a
    /// donated shortcut tap on a cold start that launches the app and does nothing, which is
    /// the exact failure this queue exists to prevent.
    private static final List<PendingActivity> pendingActivities =
            new ArrayList<PendingActivity>();

    private Intents() {
    }

    // ------------------------------------------------------------------
    // Capability queries
    // ------------------------------------------------------------------

    /// True when this platform can expose intents to the system.
    ///
    /// False does not make the API useless: [#invoke] still runs your handlers
    /// in-process on every platform, because the dispatch table is generated
    /// code rather than a platform service. Only the projections outward --
    /// voice, search indexing, shortcuts -- go quiet.
    public static boolean areIntentsSupported() {
        IntentBridge b = bridgeInternal();
        return b != null && b.areIntentsSupported();
    }

    /// True when this platform can run an intent without bringing the app to the
    /// foreground.
    public static boolean isHeadlessExecutionSupported() {
        IntentBridge b = bridgeInternal();
        return b != null && b.isHeadlessExecutionSupported();
    }

    /// True when a voice assistant can invoke intents here.
    ///
    /// This is the honest discriminator between the platforms. Branch on it
    /// rather than on [#areIntentsSupported()] when deciding whether to tell a
    /// user they can talk to your app.
    public static boolean isVoiceInvocationSupported() {
        IntentBridge b = bridgeInternal();
        return b != null && b.isVoiceInvocationSupported();
    }

    /// True when [#index] can publish content to a system-wide search index.
    public static boolean isIndexingSupported() {
        IntentBridge b = bridgeInternal();
        return b != null && b.isIndexingSupported();
    }

    // ------------------------------------------------------------------
    // Declarations
    // ------------------------------------------------------------------

    /// Every intent this application declares, from the build-time table and
    /// from [#registerDynamicIntent].
    ///
    /// The simulator's Intents window is built from exactly this list, which is
    /// what makes it trustworthy: it can only show what actually shipped.
    public static List<IntentDeclaration> getDeclarations() {
        List<IntentDeclaration> out = new ArrayList<IntentDeclaration>();
        IntentDispatcher d;
        synchronized (pending) {
            d = dispatcher;
        }
        if (d != null) {
            try {
                List<IntentDeclaration> declared = d.describe();
                if (declared != null) {
                    out.addAll(declared);
                }
            } catch (Throwable t) {
                logError(t);
            }
        }
        synchronized (dynamic) {
            for (DynamicIntent dyn : dynamic.values()) {
                IntentDeclaration base = findById(out, dyn.getBaseIntentId());
                if (base != null) {
                    out.add(describeDynamic(dyn, base));
                }
            }
        }
        return Collections.unmodifiableList(out);
    }

    /// The declaration with this id, or null.
    ///
    /// #### Parameters
    ///
    /// - `intentId`: the intent id
    public static IntentDeclaration getDeclaration(String intentId) {
        if (intentId == null) {
            return null;
        }
        for (IntentDeclaration d : getDeclarations()) {
            if (intentId.equals(d.getId())) {
                return d;
            }
        }
        return null;
    }

    /// Declares a parameterization of an intent the application already declares -- a specific
    /// shortcut such as "reorder my usual", built from data only known once the app is running.
    ///
    /// It runs by running the intent it names, with the bound values filled in, which is what
    /// makes it invokable at all. It cannot introduce a new capability: the native catalogue is
    /// compiled into the app, so a genuinely new verb could never reach the platform.
    ///
    /// Ignored when the base intent is not declared, or when the id would shadow a declared one.
    ///
    /// #### Parameters
    ///
    /// - `intent`: the parameterization
    public static void registerDynamicIntent(DynamicIntent intent) {
        if (intent == null) {
            return;
        }
        if (declaredById(intent.getId()) != null) {
            logError(new IllegalArgumentException("A dynamic intent cannot take the id of a "
                    + "declared one: \"" + intent.getId() + "\""));
            return;
        }
        if (declaredById(intent.getBaseIntentId()) == null) {
            logError(new IllegalArgumentException("Dynamic intent \"" + intent.getId()
                    + "\" names base intent \"" + intent.getBaseIntentId()
                    + "\", which this application does not declare"));
            return;
        }
        synchronized (dynamic) {
            dynamic.put(intent.getId(), intent);
        }
    }

    /// The parameterization registered under this id, or null when the id is a build-time
    /// declaration or nothing at all.
    ///
    /// Ports need this to resolve a donation: a shortcut outlives the process while a
    /// parameterization does not, so the shortcut has to record the base intent and the bound
    /// values rather than a runtime id nothing will recognise later.
    ///
    /// #### Parameters
    ///
    /// - `intentId`: the id to look up
    public static DynamicIntent getDynamicIntent(String intentId) {
        if (intentId == null) {
            return null;
        }
        synchronized (dynamic) {
            return dynamic.get(intentId);
        }
    }

    /// The declaration for a build-time intent id, or null. Deliberately does not consult the
    /// dynamic table, so registration can check for collisions against the real catalogue.
    private static IntentDeclaration declaredById(String intentId) {
        IntentDispatcher d;
        synchronized (pending) {
            d = dispatcher;
        }
        if (d == null || intentId == null) {
            return null;
        }
        List<IntentDeclaration> declared;
        try {
            declared = d.describe();
        } catch (Throwable t) {
            logError(t);
            return null;
        }
        // The walk stays outside the catch. A for-each over a typed list compiles to a
        // CHECKCAST, and ParparVM expands that to nothing -- so a catch around it would be
        // guarding against something that can never fire on the platform that matters most.
        if (declared == null) {
            return null;
        }
        for (IntentDeclaration decl : declared) {
            if (intentId.equals(decl.getId())) {
                return decl;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Invocation
    // ------------------------------------------------------------------

    /// Runs an intent on the calling thread and returns its result.
    ///
    /// This is the in-app path -- your own code deciding to perform one of its
    /// declared capabilities -- and it works on every platform, including those
    /// with no intent support at all, because the dispatch table is generated
    /// code rather than a platform service.
    ///
    /// Platform-initiated invocations do not come through here; they arrive at
    /// [#dispatchInvocation], which adds thread marshalling and an enforced
    /// deadline.
    ///
    /// The deadline on this path is a **budget the handler may consult**, not a
    /// cutoff: your own thread is blocked in this call, nothing else is waiting
    /// to report an outcome, and a handler that overruns has still done the work
    /// and produced the answer you asked for. So a late result is returned
    /// rather than discarded. Discarding is the right behaviour under
    /// [#dispatchInvocation], where the framework has already told the platform
    /// the invocation failed and a second answer would be a protocol violation.
    ///
    /// #### Parameters
    ///
    /// - `intentId`: the declared intent id
    /// - `params`: parameter values keyed by name; may be null
    ///
    /// #### Returns
    ///
    /// the handler's result, or a failed result when no such intent exists
    public static IntentResult invoke(String intentId, Map<String, Object> params) {
        IntentDeclaration decl = getDeclaration(intentId);
        int timeout = budgetFor(decl);
        // The deadline is carried so a handler can size its work the same way it would under a
        // platform invocation, and deliberately not enforced afterwards: the caller is blocked
        // in this method waiting for exactly this result, so turning a late success into a
        // failure would discard completed work with nobody left to report it to. Enforcement
        // belongs to dispatchInvocation, which has already answered the platform by then.
        IntentContext ctx = new IntentContext(IntentSource.IN_APP, false,
                System.currentTimeMillis() + timeout * 1000L);
        return invokeInternal(intentId, params, ctx);
    }

    /// Framework/port entry point: runs an intent the platform asked for and
    /// reports the outcome exactly once.
    ///
    /// Ports call this after decoding their platform payload. It owns everything
    /// the ports should not each reinvent: queuing across a cold start, running
    /// the handler off the event dispatch thread, enforcing the deadline, and
    /// guaranteeing the completion fires once and only once.
    ///
    /// #### Parameters
    ///
    /// - `intentId`: the intent to run
    /// - `params`: parameter values keyed by name; may be null
    /// - `source`: where the invocation came from
    /// - `headless`: true when the app has no UI on screen
    /// - `completion`: notified with the outcome; may be null
    public static void dispatchInvocation(final String intentId,
                                          final Map<String, Object> params,
                                          final IntentSource source,
                                          final boolean headless,
                                          final IntentCompletion completion) {
        PendingInvocation inv = new PendingInvocation(intentId, params, source,
                headless, completion);
        IntentDispatcher d;
        // Read the dispatcher and decide queue-vs-run atomically under the same
        // lock setDispatcher installs it and drains under, so an invocation
        // cannot slip in between the drain and the install and be stranded.
        synchronized (pending) {
            d = dispatcher;
            if (d == null) {
                pending.add(inv);
                return;
            }
        }
        run(inv);
    }

    /// Overrides how long a handler may run before the framework gives up, for
    /// intents that did not state their own budget.
    ///
    /// Raising this is rarely the right fix. The platform's patience is not the
    /// constraint that matters -- a spoken interaction that takes ten seconds
    /// has already failed as an interaction. An intent that genuinely needs
    /// longer should return [IntentResult#opens] and do the work in the app.
    ///
    /// #### Parameters
    ///
    /// - `seconds`: the default budget; values below 1 are ignored
    public static void setDefaultTimeout(int seconds) {
        if (seconds >= 1) {
            defaultTimeoutSeconds = seconds;
        }
    }

    /// The default handler time budget in seconds.
    public static int getDefaultTimeout() {
        return defaultTimeoutSeconds;
    }

    // ------------------------------------------------------------------
    // Donation and indexing
    // ------------------------------------------------------------------

    /// Tells the system the user just performed this capability, so it can
    /// suggest or predict it later.
    ///
    /// Donate when the user does the thing *in your app by hand*. That is the
    /// signal the system learns from; donating on every intent invocation
    /// teaches it only that the user uses shortcuts.
    ///
    /// Callable from any thread. A no-op where unsupported.
    ///
    /// #### Parameters
    ///
    /// - `intentId`: the capability that was performed
    /// - `params`: the values it was performed with; may be null
    public static void donate(String intentId, Map<String, Object> params) {
        if (intentId == null) {
            return;
        }
        IntentBridge b = bridgeInternal();
        if (b == null || !b.areIntentsSupported()) {
            return;
        }
        // exposure is a restriction, and donation is a platform surface: publishing a
        // MODEL-only capability as a launcher shortcut or a predictable activity would expose
        // exactly what it opted out of. The static builders filter this; so must the runtime.
        IntentDeclaration decl = getDeclaration(intentId);
        if (decl == null) {
            // A donation is a durable publication: Android persists a shortcut and iOS records
            // an activity the system may suggest for weeks. Publishing one for an id nothing
            // declares produces a launcher entry or a Siri suggestion that opens the app and
            // does nothing, and there is no later moment at which that gets cleaned up. A typo
            // in an id is the ordinary way to get here.
            logDiagnostic("Ignoring a donation for \"" + intentId
                    + "\", which no @AppIntent declares and no dynamic intent registers");
            return;
        }
        if (!decl.isExposedTo(Exposure.ASSISTANT)) {
            return;
        }
        if (decl.isDestructive()) {
            // A donation becomes a launcher shortcut on Android and a suggested activity on
            // iOS, and a tap on either dispatches the handler directly -- past the confirmation
            // the generated App Intent performs, which is the entire promise of destructive=true.
            // The Android static-shortcut generator and the trampoline's unauthenticated policy
            // already refuse destructive intents for this reason; a donation is the same
            // one-tap path and gets the same answer.
            //
            // The capability itself is unaffected: Siri and the Shortcuts app still offer it,
            // and confirm before it runs, which is where a destructive action belongs.
            logDiagnostic("Not donating \"" + intentId + "\": it is destructive, and a donated "
                    + "shortcut runs on one tap with no confirmation. It remains available "
                    + "through the assistant, which confirms first.");
            return;
        }
        String unusable = donationProblem(baseOf(decl, intentId), effectiveParams(intentId, params));
        if (unusable != null) {
            // Same reasoning as the unrepresentable case below, one step earlier: a donation the
            // handler could not run is a durable, permanently broken suggestion.
            logDiagnostic("Not donating \"" + intentId + "\": " + unusable);
            return;
        }
        String lost = IntentSerializer.unrepresentable(params);
        if (lost != null) {
            // A donation is durable: the platform keeps these values and replays them when the
            // user taps the shortcut, possibly weeks later. A value the wire cannot carry --
            // a NaN, an infinity, an object of no declared type -- would be dropped on the way
            // out, and the tap would then run the handler on the parameter's *default* rather
            // than on what was donated. That is a shortcut which does something other than the
            // thing it was learned from, and there is no later moment at which it gets
            // corrected. Refusing loses only a suggestion.
            logDiagnostic("Not donating \"" + intentId + "\": the value bound to \"" + lost
                    + "\" cannot be carried to the platform, and a shortcut recorded without it "
                    + "would run on that parameter's default instead.");
            return;
        }
        try {
            b.donate(intentId, IntentSerializer.serializeParams(params));
        } catch (Throwable t) {
            logError(t);
        }
    }

    /// Publishes app content to the device's search index, replacing any entry
    /// carrying the same type and id.
    ///
    /// #### Threading
    ///
    /// A background thread is the right thread, not merely a permitted one. This
    /// writes through to the platform index and encodes any thumbnails on the
    /// way, so calling it on the event dispatch thread looks instantaneous in
    /// the simulator and stalls the UI on a device.
    ///
    /// #### Parameters
    ///
    /// - `entities`: the content to publish; null and empty are no-ops
    public static void index(List<AppEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        IntentBridge b = bridgeInternal();
        if (b == null || !b.isIndexingSupported()) {
            return;
        }
        // A title is what a search result shows, and the platforms disagree about a missing one
        // rather than degrading alike: Android hands an empty long label to ShortcutInfo.Builder
        // and can reject the shortcut outright, while iOS publishes a searchable item with
        // nothing written on it. The build enforces this for a declared @IntentEntity through
        // @EntityTitle; direct construction is the path that skips the build, so it is checked
        // here instead of failing differently on each device.
        List<AppEntity> publishable = new ArrayList<AppEntity>();
        for (AppEntity e : entities) {
            if (e == null) {
                continue;
            }
            if (e.getTitle() == null || e.getTitle().trim().length() == 0) {
                logDiagnostic("Not indexing " + e.getType() + ":" + e.getId()
                        + " because it has no title. A search result with nothing written on it "
                        + "is not something a user can act on; call setTitle before indexing.");
                continue;
            }
            publishable.add(e);
        }
        if (publishable.isEmpty()) {
            return;
        }
        Map<String, byte[]> images = new LinkedHashMap<String, byte[]>();
        String json = IntentSerializer.serializeEntities(publishable, images);
        try {
            b.index(json, images);
        } catch (Throwable t) {
            logError(t);
        }
    }

    /// Publishes a single entity. Shorthand for the list form.
    ///
    /// #### Parameters
    ///
    /// - `entity`: the content to publish
    public static void index(AppEntity entity) {
        if (entity != null) {
            index(Collections.singletonList(entity));
        }
    }

    /// Removes one entry from the search index.
    ///
    /// Removal matters more than it looks. An index entry outlives the data
    /// behind it, so content the user deleted keeps appearing in device search
    /// and taps resolve to nothing until the app removes it.
    ///
    /// #### Parameters
    ///
    /// - `entityType`: the entity type id
    /// - `id`: the entity id
    public static void removeFromIndex(String entityType, String id) {
        if (entityType == null || id == null) {
            return;
        }
        // Same rule the constructor enforces, and this path never builds an entity so it never
        // reached it. removeFromIndex("shop:order", "42") composes the uid "shop:order:42",
        // which is exactly what new AppEntity("shop", "order:42") publishes -- so the call did
        // not merely fail, it deleted somebody else's content.
        AppEntity.checkType(entityType);
        IntentBridge b = bridgeInternal();
        if (b == null || !b.isIndexingSupported()) {
            return;
        }
        try {
            b.removeFromIndex(IntentSerializer.serializeEntityRef(entityType, id));
        } catch (Throwable t) {
            logError(t);
        }
    }

    /// Removes every indexed entry of one type, or everything this app indexed.
    ///
    /// #### Parameters
    ///
    /// - `entityType`: the type to clear, or null for all of this app's entries
    public static void clearIndex(String entityType) {
        if (entityType != null) {
            // Ports match a type by uid prefix, so clearIndex("shop:order") would sweep every
            // entity of type "shop" whose id begins with "order:" -- the same collision.
            AppEntity.checkType(entityType);
        }
        IntentBridge b = bridgeInternal();
        if (b == null || !b.isIndexingSupported()) {
            return;
        }
        try {
            b.clearIndex(entityType);
        } catch (Throwable t) {
            logError(t);
        }
    }

    // ------------------------------------------------------------------
    // AppEntity queries
    // ------------------------------------------------------------------

    /// Runs one of an entity type's declared queries.
    ///
    /// The platform calls this on its own when it has to disambiguate a
    /// parameter -- "which playlist?" -- and the simulator calls it to populate
    /// its picker, which is why the simulator exercises the real query rather
    /// than a stand-in.
    ///
    /// #### Parameters
    ///
    /// - `entityType`: the entity type id
    /// - `kind`: `byId`, `suggested` or `search`
    /// - `argument`: the id, the search text, or null
    ///
    /// #### Returns
    ///
    /// the matching entities, never null
    public static List<AppEntity> queryEntities(String entityType, String kind, String argument) {
        IntentDispatcher d;
        synchronized (pending) {
            d = dispatcher;
        }
        if (d == null || entityType == null) {
            return Collections.emptyList();
        }
        try {
            List<AppEntity> out = d.queryEntities(entityType, kind, argument);
            return out == null ? Collections.<AppEntity>emptyList() : out;
        } catch (Throwable t) {
            logError(t);
            return Collections.emptyList();
        }
    }

    // ------------------------------------------------------------------
    // Language-model projection
    // ------------------------------------------------------------------

    /// The intents that opted into [Exposure#MODEL], projected down to
    /// `com.codename1.ai.Tool` so they can be handed to a language model or an MCP host.
    ///
    /// Nothing is exposed by calling this. It returns descriptions; the application decides
    /// whether to give them to a model, which is deliberately a separate act from declaring the
    /// intent, because a model calls a capability because it inferred it should rather than
    /// because a person asked by name.
    ///
    /// The projection is one-way and lossy on purpose. A `Tool` is stringly typed -- a JSON
    /// schema in, a JSON string out -- which is right for a model and wrong for the platform,
    /// where entity types let the system run its own picker before the handler is reached. So
    /// the richer declaration projects down to the weaker one, never the reverse.
    ///
    /// #### Returns
    ///
    /// one tool per model-exposed intent, never null
    public static List<Tool> asTools() {
        List<Tool> out = new ArrayList<Tool>();
        for (IntentDeclaration d : getDeclarations()) {
            if (!d.isExposedTo(Exposure.MODEL)) {
                continue;
            }
            out.add(new Tool(d.getId(), toolDescription(d), toolSchema(d), new IntentTool(d)));
        }
        return out;
    }

    private static String toolDescription(IntentDeclaration d) {
        String description = d.getDescription();
        if (description != null && description.length() > 0) {
            return description;
        }
        return d.getTitle();
    }

    /// Builds the JSON schema a model needs. Entity parameters are described as strings,
    /// because that is what crosses the boundary: the id, which the handler resolves.
    private static String toolSchema(IntentDeclaration d) {
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        List<Object> required = new ArrayList<Object>();
        for (IntentParameterInfo p : d.getParameters()) {
            Map<String, Object> prop = new LinkedHashMap<String, Object>();
            prop.put("type", schemaType(p.getType()));
            prop.put("description", p.getTitle());
            if (!p.getOptions().isEmpty()) {
                prop.put("enum", new ArrayList<Object>(p.getOptions()));
            }
            if (p.getType() == IntentParameterType.ENTITY) {
                prop.put("description", p.getTitle() + " (the id of a "
                        + p.getEntityType() + ")");
            }
            if (p.getType() == IntentParameterType.DATE) {
                // A bare "string" would let a model send anything and call it schema-valid,
                // which is how a well-formed request turns into a null argument. Name both
                // forms the dispatcher actually parses.
                prop.put("description", p.getTitle()
                        + " (an ISO-8601 date such as 2026-03-14, or epoch milliseconds)");
            }
            properties.put(p.getName(), prop);
            if (p.isRequired()) {
                required.add(p.getName());
            }
        }
        Map<String, Object> schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        return JSONWriter.toJson(schema);
    }

    private static String schemaType(IntentParameterType type) {
        if (type == IntentParameterType.INTEGER) {
            return "integer";
        }
        if (type == IntentParameterType.NUMBER) {
            return "number";
        }
        if (type == IntentParameterType.BOOLEAN) {
            return "boolean";
        }
        // A date stays a string, which is the form a model writes dates in without being
        // coerced into arithmetic on epoch millis. The generated dispatcher accepts both that
        // and a numeric string, and the property description says so -- the two have to agree
        // or a schema-valid request becomes a null argument.
        return "string";
    }

    /// Runs one intent on behalf of a model.
    ///
    /// Named rather than anonymous because it holds the declaration it belongs to, and because
    /// the iOS translator's dead-code elimination is easier to reason about with a real type.
    private static final class IntentTool implements ToolHandler {
        private final IntentDeclaration declaration;

        IntentTool(IntentDeclaration declaration) {
            this.declaration = declaration;
        }

        /// Runs the intent through the same deadline-enforcing path a platform invocation
        /// uses, and blocks until it answers.
        ///
        /// Calling the dispatcher directly would have made a model the one caller whose
        /// handlers had no enforced budget: the deadline would pass, the context would report
        /// cancelled, and the late result would still be serialized and still navigate. A model
        /// is the caller least able to notice that, since it just receives a string.
        ///
        /// Blocking is correct here and is not the same as blocking under
        /// [Intents#invoke]: the tool contract is synchronous, and what arrives is whichever
        /// outcome won the race -- the handler's, or the timeout's.
        @Override
        public String invoke(String argumentsJson) throws Exception {
            Map<String, Object> args = null;
            if (argumentsJson != null && argumentsJson.length() > 0) {
                // Through the shared parser: a model writing a large id must not have it
                // rounded into a different, still-plausible number.
                args = IntentSerializer.parsePayload(argumentsJson);
            }
            ToolCompletion done = new ToolCompletion();
            dispatchInvocation(declaration.getId(), args, IntentSource.MODEL, false, done);
            IntentResult r = done.awaitResult(budgetFor(declaration));
            Map<String, byte[]> images = new LinkedHashMap<String, byte[]>();
            return IntentSerializer.serializeResult(r, images);
        }
    }

    /// Blocks a model's synchronous tool call until the framework reports an outcome.
    ///
    /// Named and static rather than anonymous so it holds nothing but its own result.
    private static final class ToolCompletion implements IntentCompletion {
        private IntentResult result;
        private boolean done;

        @Override
        public void onIntentResult(IntentResult r) {
            synchronized (this) {
                result = r;
                done = true;
                notifyAll();
            }
        }

        /// Waits for the outcome. The framework's own timeout is what ends the wait in the
        /// normal case; the margin here exists only so a completion lost to a bug cannot block
        /// the caller forever.
        IntentResult awaitResult(int timeoutSeconds) {
            long giveUpAt = System.currentTimeMillis()
                    + (timeoutSeconds + COMPLETION_BACKSTOP_SECONDS) * 1000L;
            synchronized (this) {
                while (!done) {
                    long left = giveUpAt - System.currentTimeMillis();
                    if (left <= 0) {
                        return IntentResult.failed("The action took too long and was stopped");
                    }
                    try {
                        wait(left);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return IntentResult.failed("The action was interrupted");
                    }
                }
                return result == null ? IntentResult.ok() : result;
            }
        }
    }

    /// How far past its own deadline the framework is given to report, before a blocked caller
    /// gives up on it.
    private static final int COMPLETION_BACKSTOP_SECONDS = 5;

    // ------------------------------------------------------------------
    // Search-result selection
    // ------------------------------------------------------------------

    /// Registers the single handler that receives taps on content published with
    /// [#index].
    ///
    /// Registration drains anything that arrived before it, which is the normal
    /// case: a tap in device search is frequently what started the process, so
    /// the selection is already waiting by the time your `init()` runs.
    ///
    /// #### Parameters
    ///
    /// - `handler`: the handler, or null to clear
    public static void setSelectionHandler(EntitySelectionHandler handler) {
        // Install and drain under the same lock dispatchSpotlightSelection reads
        // it under, so a selection cannot slip between the drain and the install
        // and be stranded forever.
        List<AppEntity> queued = null;
        synchronized (pendingSelections) {
            selectionHandler = handler;
            if (handler != null && !pendingSelections.isEmpty()) {
                queued = new ArrayList<AppEntity>(pendingSelections);
                pendingSelections.clear();
            }
        }
        if (queued != null) {
            for (AppEntity e : queued) {
                deliverSelection(handler, e);
            }
        }
    }

    /// Framework/port entry point: the user opened an indexed item. The id is the
    /// composite the framework indexed under, `type:id`.
    ///
    /// #### Parameters
    ///
    /// - `uniqueId`: the identifier the platform handed back
    public static void dispatchSpotlightSelection(String uniqueId) {
        if (uniqueId == null || uniqueId.length() == 0) {
            return;
        }
        int sep = uniqueId.indexOf(':');
        if (sep <= 0 || sep == uniqueId.length() - 1) {
            return;
        }
        AppEntity e = new AppEntity(uniqueId.substring(0, sep), uniqueId.substring(sep + 1));
        EntitySelectionHandler h;
        synchronized (pendingSelections) {
            h = selectionHandler;
            if (h == null) {
                pendingSelections.add(e);
                return;
            }
        }
        deliverSelection(h, e);
    }

    /// Framework/port entry point: a platform activity arrived that is not a web
    /// link. Returns true when this application claimed it.
    ///
    /// Answering honestly matters. Claiming everything would swallow handoff and
    /// third-party activities the app never declared, so an activity is claimed
    /// only when its type names an intent this application actually declares --
    /// which is the shape the platform uses to continue a donated action.
    ///
    /// #### Parameters
    ///
    /// - `activityType`: the platform activity type
    /// - `params`: the activity payload, may be null
    public static boolean dispatchUserActivity(String activityType, Map<String, Object> params) {
        if (activityType == null) {
            return false;
        }
        if (getDeclaration(activityType) != null) {
            dispatchInvocation(activityType, params, IntentSource.SHORTCUT, false, null);
            return true;
        }
        // Cold start: the platform can deliver a donated activity before the generated
        // dispatcher installs itself, so an unrecognised type is not yet evidence of anything.
        // Dropping it here is what made a shortcut tap on a dead process launch the app and
        // then do nothing.
        //
        // Only ids shaped like ours are held. A third-party or handoff activity type is
        // reverse-DNS and cannot match, so claiming this one does not swallow theirs.
        //
        // The test and the enqueue are one critical section, and it is the dispatcher's own
        // lock: anything less lets setDispatcher install and drain in the gap between them.
        synchronized (pending) {
            if (dispatcher != null) {
                // The table exists and does not contain this type, so it belongs to somebody
                // else.
                return false;
            }
            // Shape is not ownership. An application may declare its own activity type that
            // happens to look like an intent id -- "continue_reading" is entirely ordinary --
            // and claiming it here tells iOS the activity was handled, so the app's own
            // continuation never runs and drainPendingActivities quietly drops it. The warm
            // path already answers correctly, because by then the real list exists.
            //
            // So the real list is what decides, remembered by the previous launch. Only when
            // there has never been one -- a first launch, where an App Shortcut can still be
            // invoked against an app that has not run -- does the shape heuristic stand in,
            // and that window closes as soon as the app publishes its declarations once.
            String recorded = recordedDeclarationIds();
            boolean ours = recorded == null
                    ? looksLikeIntentId(activityType)
                    : recordNames(recorded, activityType);
            if (!ours) {
                return false;
            }
            pendingActivities.add(new PendingActivity(activityType, params));
        }
        return true;
    }

    /// True when a string has the shape the build enforces for an intent id: lower case, digits
    /// and underscores only, so it can never be confused with a platform activity type.
    private static boolean looksLikeIntentId(String s) {
        if (s.length() < 3 || s.length() > 64) {
            return false;
        }
        char first = s.charAt(0);
        if (first < 'a' || first > 'z') {
            return false;
        }
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static void deliverSelection(final EntitySelectionHandler h, final AppEntity e) {
        if (h == null) {
            return;
        }
        if (Display.isInitialized()) {
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    h.onEntitySelected(e);
                }
            });
        } else {
            h.onEntitySelected(e);
        }
    }

    // ------------------------------------------------------------------
    // Framework / port entry points
    // ------------------------------------------------------------------

    /// Internal: installs the build-time-generated dispatcher and drains any
    /// invocation that arrived before it. Invoked once during startup by the
    /// generated bootstrap; application code should not call this.
    ///
    /// #### Parameters
    ///
    /// - `d`: the generated dispatcher
    public static void setDispatcher(IntentDispatcher d) {
        List<PendingInvocation> queued = null;
        List<PendingActivity> activities = null;
        synchronized (pending) {
            dispatcher = d;
            if (d != null) {
                if (!pending.isEmpty()) {
                    queued = new ArrayList<PendingInvocation>(pending);
                    pending.clear();
                }
                // Taken under the same lock that just installed the dispatcher, so nothing can
                // be queued after this snapshot and before the queue stops being consulted.
                if (!pendingActivities.isEmpty()) {
                    activities = new ArrayList<PendingActivity>(pendingActivities);
                    pendingActivities.clear();
                }
            }
        }
        if (d != null) {
            publishDeclarations(d);
            drainPendingActivities(activities);
        }
        if (queued != null) {
            for (PendingInvocation q : queued) {
                run(q);
            }
        }
    }

    /// Framework/port entry point: resolves the platform bridge, which publishes any
    /// declarations that were installed before one existed.
    ///
    /// The generated bootstrap installs the dispatcher before the port has booted -- from
    /// `main()` on iOS, and before `startContext` on Android -- so the first publication finds
    /// no bridge and is deferred. Something has to ask for the bridge afterwards or the
    /// platform never learns the catalogue, and on Android a request parked at a cold start is
    /// never judged, so the shortcut opens the app and runs nothing.
    ///
    /// Ports call this once the runtime is up. It is safe to call at any time and does nothing
    /// when there is nothing owed.
    public static void publishPendingDeclarations() {
        bridgeInternal();
    }

    /// Framework/port/test entry point: overrides the bridge resolved from the
    /// platform port. Passing null restores platform resolution.
    ///
    /// #### Parameters
    ///
    /// - `b`: the bridge, or null
    public static void setBridge(IntentBridge b) {
        bridge = b;
        bridgeOverridden = b != null;
    }

    static IntentBridge bridgeInternal() {
        IntentBridge b = resolveBridge();
        if (b != null) {
            // First moment a bridge exists is the first moment declarations can be published,
            // and on iOS that is later than the moment they are installed.
            flushDeclarations(b);
        }
        return b;
    }

    private static IntentBridge resolveBridge() {
        if (bridgeOverridden) {
            return bridge;
        }
        if (!Display.isInitialized()) {
            return null;
        }
        try {
            return Display.getInstance().getIntentBridge();
        } catch (Throwable t) {
            logError(t);
            return null;
        }
    }

    /// True when declarations exist but could not be published because no bridge did yet.
    /// Guarded by `pending`.
    private static boolean declarationsOwed;

    /// Publishes declarations that were installed before the platform bridge existed.
    ///
    /// The generated bootstrap installs the dispatcher from the app's `main`, which on iOS runs
    /// **before** `Display.init` -- so the first publication attempt finds no bridge and, until
    /// this existed, gave up permanently. The native side then never learned the catalogue, and
    /// the first App Intent ran its handler and dropped the result because the port's bridge
    /// reference was still null.
    private static void flushDeclarations(IntentBridge b) {
        IntentDispatcher d;
        synchronized (pending) {
            if (!declarationsOwed) {
                return;
            }
            d = dispatcher;
            // Cleared before publishing rather than after: registerIntents can re-enter this
            // through the framework, and a second publication is not what that would mean.
            declarationsOwed = false;
        }
        if (d == null) {
            return;
        }
        publishTo(b, d);
    }

    /// Test seam: clears the bridge override, the dispatcher, queued invocations
    /// and runtime declarations.
    static void reset() {
        bridge = null;
        bridgeOverridden = false;
        defaultTimeoutSeconds = 20;
        synchronized (pending) {
            dispatcher = null;
            pending.clear();
            pendingActivities.clear();
            declarationsOwed = false;
        }
        synchronized (dynamic) {
            dynamic.clear();
        }
        synchronized (pendingSelections) {
            selectionHandler = null;
            pendingSelections.clear();
        }
        try {
            // Remembered across launches on a device, so a reset that left it behind would let
            // one test's declaration list decide what the next one claims.
            Preferences.delete(DECLARED_IDS_KEY);
        } catch (Throwable ignored) {
            // A reset must not fail because storage is unavailable.
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /// The budget an invocation of this declaration gets, in seconds.
    ///
    /// One definition on purpose. The build rejects a non-positive timeoutSeconds, but a
    /// DynamicIntent or a declaration built at runtime can still carry one, and three callers
    /// each reading it their own way is how the same handler ended up with contradictory
    /// deadlines: platform dispatch substituting the default, Intents.invoke building an
    /// already-expired context, and a model waiting on the raw number.
    /// Where the declared intent ids are remembered between launches.
    ///
    /// Prefixed like every other framework preference so an application's own key cannot
    /// collide with it.
    private static final String DECLARED_IDS_KEY = "cn1$intents$declared";

    private static int budgetFor(IntentDeclaration decl) {
        int declared = decl == null ? defaultTimeoutSeconds : decl.getTimeoutSeconds();
        return declared < 1 ? defaultTimeoutSeconds : declared;
    }

    private static IntentDeclaration findById(List<IntentDeclaration> all, String id) {
        for (IntentDeclaration d : all) {
            if (d.getId().equals(id)) {
                return d;
            }
        }
        return null;
    }

    /// Presents a parameterization as a declaration in its own right, inheriting the base
    /// intent's behaviour and dropping the parameters it has already bound -- those are no
    /// longer anything a caller has to supply.
    private static IntentDeclaration describeDynamic(DynamicIntent dyn, IntentDeclaration base) {
        List<IntentParameterInfo> remaining = new ArrayList<IntentParameterInfo>();
        Map<String, Object> bound = dyn.getBoundParameters();
        for (IntentParameterInfo p : base.getParameters()) {
            // Bound is not the same as satisfied. A binding the declared type cannot accept --
            // "abc" for an int, a value outside a closed vocabulary -- would otherwise hide the
            // parameter from this declaration, so neither the simulator nor a model schema
            // offers any way to correct it, while dispatch rejects it later in the coercion.
            // The parameterization and every donation made from it are then unusable with
            // nothing on either side saying why. A binding that cannot be honoured leaves the
            // parameter visible, where it can be supplied -- and a supplied value wins over a
            // bound one, so surfacing it is also the repair.
            if (!bound.containsKey(p.getName())
                    || !satisfies(p, bound.get(p.getName()), true)) {
                remaining.add(p);
            }
        }
        return new IntentDeclaration(dyn.getId(), dyn.getTitle(), base.getDescription(),
                base.isHeadless(), base.isDiscoverable(), base.isDestructive(),
                base.getOpensRoute(), base.getTimeoutSeconds(),
                Collections.<String>emptyList(), remaining, base.getExposure());
    }

    /// The declaration a donation actually has to satisfy.
    ///
    /// For a parameterization that is the *base* intent, not the synthesized declaration. The
    /// synthesized one hides every parameter the binding satisfied, so a supplied value
    /// overriding one of those was never examined -- and both bridges merge supplied values on
    /// top of the bindings, so an override of "abc" for a bound integer replaced a valid value
    /// with one the coercion rejects on every tap. What runs is the base intent, so what is
    /// checked is the base intent.
    private static IntentDeclaration baseOf(IntentDeclaration decl, String intentId) {
        DynamicIntent dyn = getDynamicIntent(intentId);
        if (dyn == null) {
            return decl;
        }
        IntentDeclaration base = getDeclaration(dyn.getBaseIntentId());
        return base == null ? decl : base;
    }

    /// The values a donated parameterization will actually run with.
    ///
    /// Mirrors IntentSerializer#mergeParams, which is what the ports apply: the bindings first,
    /// then anything supplied at donation time on top, because a binding is a default rather
    /// than a lock. A null supplied value is skipped rather than overriding -- the serializer
    /// drops nulls on the way out, so on the platforms the binding survives one.
    private static Map<String, Object> effectiveParams(String intentId,
                                                       Map<String, Object> params) {
        DynamicIntent dyn = getDynamicIntent(intentId);
        if (dyn == null) {
            return params;
        }
        Map<String, Object> merged =
                new LinkedHashMap<String, Object>(dyn.getBoundParameters());
        if (params != null) {
            for (Map.Entry<String, Object> e : params.entrySet()) {
                if (e.getValue() != null) {
                    merged.put(e.getKey(), e.getValue());
                }
            }
        }
        return merged;
    }

    /// Why this donation could never run, or null when it could.
    ///
    /// A donation is not an invocation, so nothing rejects it at the time it is made -- but the
    /// platforms replay the saved arguments verbatim when the user taps the shortcut, with no
    /// picker in between. A donation missing a required value, or carrying one its parameter
    /// cannot accept, therefore publishes a launcher entry or a Siri suggestion that fails on
    /// every tap, for as long as it survives. Refusing costs only a suggestion.
    ///
    /// The declaration is the authority on what is missing, and for a parameterization it is
    /// the synthesized one -- whose parameters are exactly those the binding has not already
    /// satisfied, which is what the platform will merge in.
    private static String donationProblem(IntentDeclaration decl, Map<String, Object> params) {
        for (IntentParameterInfo p : decl.getParameters()) {
            Object supplied = params == null ? null : params.get(p.getName());
            if (supplied == null) {
                // A default stands in for an absent value, which is what makes an optional
                // parameter optional. The build refuses required-with-a-default, so in practice
                // only a runtime-built declaration reaches the second half of this.
                if (p.isRequired() && (p.getDefaultValue() == null
                        || p.getDefaultValue().length() == 0)) {
                    return "it has no value for the required parameter \"" + p.getName()
                            + "\", so every tap on the shortcut would fail.";
                }
                continue;
            }
            if (!satisfies(p, supplied, false)) {
                return "the value given for \"" + p.getName() + "\" is not one that parameter "
                        + "accepts, so every tap on the shortcut would fail.";
            }
        }
        return null;
    }

    /// Whether a bound value is one the generated coercion could accept for this parameter.
    ///
    /// Mirrors that coercion rather than restating it loosely: a whole number may arrive as a
    /// number or as text, a boolean accepts only the four spellings the coercion accepts, and a
    /// closed vocabulary is matched as text because that is how the coercion matches it.
    ///
    /// #### Why the numeric width is the caller's choice
    ///
    /// A declaration records `INTEGER` for both `int` and `long`, and `NUMBER` for both `float`
    /// and `double`, so this cannot know which width the handler declared -- while the generated
    /// coercion checks the real one. Neither guess is safe for both callers, because their
    /// failure modes are opposites.
    ///
    /// Deciding whether a parameterization may *hide* a parameter, too loose is unrecoverable:
    /// a binding that can never run sits behind a parameter nobody can see, and neither the
    /// simulator nor a model schema offers any way to override it. Too strict merely surfaces a
    /// parameter that was already satisfied -- visible, and still correct at dispatch, since the
    /// bound value is merged in regardless. That caller asks for `narrow`.
    ///
    /// Deciding whether a donation *could run*, the asymmetry reverses: too strict refuses to
    /// publish a shortcut that would have dispatched perfectly well, and 5000000000 is an
    /// ordinary value for a `long`. So that caller asks for the widest the type could mean, and
    /// refuses only what no width could hold.
    private static boolean satisfies(IntentParameterInfo p, Object value, boolean narrow) {
        if (value == null) {
            return false;
        }
        IntentParameterType type = p.getType();
        if (!p.getOptions().isEmpty()) {
            return value instanceof String && p.getOptions().contains(value);
        }
        if (type == IntentParameterType.STRING) {
            return value instanceof String || value instanceof Character;
        }
        if (type == IntentParameterType.BOOLEAN) {
            if (value instanceof Boolean) {
                return true;
            }
            if (value instanceof Number) {
                double d = ((Number) value).doubleValue();
                return d == 0 || d == 1;
            }
            // equalsIgnoreCase, because that is what the coercion uses. Being stricter here is
            // not the safe direction for every caller: hiding a parameter that would have run
            // is recoverable, but refusing to donate a shortcut that would have dispatched
            // just loses it.
            String t = value.toString().trim();
            return "true".equalsIgnoreCase(t) || "false".equalsIgnoreCase(t)
                    || "1".equals(t) || "0".equals(t);
        }
        if (type == IntentParameterType.INTEGER) {
            if (value instanceof Number) {
                return isWhole(((Number) value).doubleValue(), narrow);
            }
            if (value instanceof String) {
                try {
                    return isWhole(Long.parseLong(((String) value).trim()), narrow);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return false;
        }
        if (type == IntentParameterType.NUMBER) {
            if (value instanceof Number) {
                return isFinite(((Number) value).doubleValue(), narrow);
            }
            if (value instanceof String) {
                try {
                    return isFinite(Double.parseDouble(((String) value).trim()), narrow);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return false;
        }
        if (type == IntentParameterType.DATE) {
            return IntentDates.parse(value) != null;
        }
        if (value instanceof AppEntity) {
            // An entity of the wrong type is not a near miss. The wire keeps only the id, so a
            // customer bound to an order parameter arrives as a bare id that the *order* BY_ID
            // query resolves -- either finding nothing, or finding an unrelated order that
            // happens to share the id and running the handler on it. Neither is recoverable
            // afterwards, and neither is visible: a parameterization would also have hidden the
            // parameter as satisfied.
            String declared = p.getEntityType();
            return declared == null || declared.equals(((AppEntity) value).getType());
        }
        // An id resolves through the BY_ID query, and whether it names anything is that query's
        // answer to give rather than this method's.
        return value instanceof String;
    }

    /// A whole number an `INTEGER` parameter could hold, at `int` width or at `long` width.
    ///
    /// The generated coercion rejects a fraction rather than rounding it, and rejects a value
    /// past the declared width rather than letting longValue() saturate it into a number the
    /// caller never sent. Both rejections are mirrored here; only the width varies.
    private static boolean isWhole(double d, boolean narrow) {
        if (Double.isNaN(d) || Double.isInfinite(d) || d != Math.floor(d)) {
            return false;
        }
        if (narrow) {
            return d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE;
        }
        // The upper bound is exclusive because (double) Long.MAX_VALUE rounds up to 2^63
        // exactly, which is the same reason the generated coercion writes it that way.
        return d >= -9223372036854775808.0 && d < 9223372036854775808.0;
    }

    /// A number a `NUMBER` parameter could hold, at `float` width or at `double` width.
    ///
    /// At float width both ends matter, as they do in the coercion: past Float.MAX_VALUE
    /// becomes Infinity, and a non-zero value too small to represent becomes exactly zero -- a
    /// value nobody supplied. A double holds any finite value, so there being finite is all.
    private static boolean isFinite(double d, boolean narrow) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return false;
        }
        if (!narrow) {
            return true;
        }
        if (d < -Float.MAX_VALUE || d > Float.MAX_VALUE) {
            return false;
        }
        return d == 0.0d || (float) d != 0.0f;
    }

    /// Runs the activities that arrived before the declarations did, dropping any the
    /// application turns out not to declare.
    ///
    /// Takes the snapshot as a parameter rather than reading the queue itself: the caller
    /// drained it under the lock that installed the dispatcher, which is what closes the window
    /// an activity could otherwise be queued into and never taken out of.
    private static void drainPendingActivities(List<PendingActivity> queued) {
        if (queued == null) {
            return;
        }
        for (PendingActivity a : queued) {
            if (getDeclaration(a.activityType) != null) {
                dispatchInvocation(a.activityType, a.params, IntentSource.SHORTCUT, false, null);
            }
        }
    }

    /// Navigates when a result names a route.
    ///
    /// The framework does this rather than each port, because the route table is Java and the
    /// platforms only know how to bring the app forward -- iOS through `openAppWhenRun`, Android
    /// through the trampoline. Without this the app would foreground and then sit on whatever
    /// screen it happened to be showing, which is the failure an `opens` result exists to avoid.
    private static void navigateIfRequested(IntentResult r, IntentDeclaration decl,
                                             Map<String, Object> params) {
        if (r == null || r.isFailed()) {
            return;
        }
        String url = r.getOpenUrl();
        boolean fromResult = url != null && url.length() > 0;
        if (!fromResult) {
            // The handler named no route, but the declaration may have. That is the whole point
            // of opensRoute: a handler can return ok() and still be an "open the app here"
            // intent, and without expanding the template the app foregrounds onto whatever
            // screen it happened to be showing.
            url = expandRoute(decl, params);
        }
        if (url == null || url.length() == 0) {
            return;
        }
        // A declared opensRoute has already brought the app forward -- that is what the flag is
        // for. A route the handler decided on at runtime has not, and if it ran headless the
        // destination would otherwise be built somewhere nobody can see.
        if (fromResult && decl != null && decl.runsHeadless()) {
            requestForeground(decl);
        }
        try {
            // Marshals to the EDT itself and is a no-op when no route matches.
            com.codename1.router.Navigation.dispatchExternalUrl(url);
        } catch (Throwable t) {
            logError(t);
        }
    }

    /// Asks the port to bring the app forward for a route the handler chose at runtime, and
    /// says so plainly when the platform will not.
    private static void requestForeground(IntentDeclaration decl) {
        IntentBridge b = bridgeInternal();
        if (b == null) {
            return;
        }
        try {
            if (b.requestForeground()) {
                return;
            }
        } catch (Throwable t) {
            logError(t);
            return;
        }
        // iOS is the case that reaches here: an app cannot bring itself forward, so whether it
        // does is fixed before the handler runs. Naming the fix is the useful part -- declaring
        // opensRoute is what sets openAppWhenRun, and it works on every platform.
        logDiagnostic("\"" + decl.getId() + "\" ran headless and returned a route, but this "
                + "platform does not let an app foreground itself, so the destination will not "
                + "be shown. Declare opensRoute on the @AppIntent to have the platform open the "
                + "app for it.");
    }

    /// What the generated coercion substitutes for an omitted value of this type, or null when
    /// absence really does mean null.
    ///
    /// Only the primitives have one: a String, Date or entity parameter left out arrives as
    /// null, and a URL containing "null" would route somewhere nobody asked for -- worse than
    /// not navigating.
    private static String implicitDefault(IntentParameterType type) {
        if (type == IntentParameterType.INTEGER || type == IntentParameterType.NUMBER) {
            return "0";
        }
        if (type == IntentParameterType.BOOLEAN) {
            return "false";
        }
        return null;
    }

    /// Fills a declared route template from the values the intent ran with, or returns null
    /// when there is no template or a placeholder has no value.
    private static String expandRoute(IntentDeclaration decl, Map<String, Object> params) {
        if (decl == null) {
            return null;
        }
        String template = decl.getOpensRoute();
        if (template == null || template.length() == 0) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < template.length()) {
            int open = template.indexOf('{', i);
            if (open < 0) {
                out.append(template.substring(i));
                break;
            }
            int close = template.indexOf('}', open);
            if (close < 0) {
                out.append(template.substring(i));
                break;
            }
            out.append(template, i, open);
            String name = template.substring(open + 1, close);
            Object value = params == null ? null : params.get(name);
            if (value == null) {
                // The platform omits an optional parameter it was not given, and the generated
                // dispatcher fills in the declared default before calling the handler. Reading
                // only the supplied map made the route unexpandable for exactly those intents,
                // so a valid invocation left the app sitting on whatever screen it was showing.
                IntentParameterInfo p = decl.getParameter(name);
                if (p != null && p.getDefaultValue() != null
                        && p.getDefaultValue().length() > 0) {
                    value = p.getDefaultValue();
                } else if (p != null) {
                    // A primitive cannot be absent: the generated dispatcher hands the handler
                    // the type's zero, so the route has to be built from that too. Reading only
                    // explicit defaults meant "/items/{page}" with an optional int ran the
                    // handler with page 0 and then left the foregrounded app on whatever screen
                    // it was already showing -- the invocation half-happening.
                    value = implicitDefault(p.getType());
                }
            }
            if (value == null) {
                // A half-expanded URL would route somewhere unintended, which is worse than not
                // navigating at all.
                return null;
            }
            // Encoded, because a value is data and the template is structure. An id
            // containing "/" would otherwise add a path segment and stop matching the route it
            // was built for, and "?" or "#" would invent a query or a fragment.
            out.append(com.codename1.io.Util.encodeUrl(String.valueOf(value)));
            i = close + 1;
        }
        return out.toString();
    }

    /// Logs a swallowed failure without ever being able to become one.
    ///
    /// Every `catch` in this class exists to guarantee that a broken handler
    /// still produces a result rather than an exception. `Log.e` can itself
    /// throw when the logging stack is not fully up -- which is precisely the
    /// state a headless invocation runs in, since the process may have been
    /// started for no other reason than to answer it. Letting that escape would
    /// turn a handled failure into an unhandled one at exactly the wrong moment.
    private static void logError(Throwable t) {
        try {
            Log.e(t);
        } catch (Throwable ignored) {
            // Nothing useful is left to do: the reporting path is the thing that
            // is broken. Losing the log entry is strictly better than losing the
            // result the caller is waiting for.
        }
    }

    /// Reports a request the framework declined to act on.
    ///
    /// Guarded the same way as [#logError]: `Log.p` reaches for the Display, which a headless
    /// invocation may not have started yet.
    private static void logDiagnostic(String message) {
        try {
            Log.p("[intents] " + message, Log.WARNING);
        } catch (Throwable ignored) {
            // See logError: the reporting path failing must not fail the caller.
        }
    }

    private static void publishDeclarations(IntentDispatcher d) {
        IntentBridge b = bridgeInternal();
        if (b == null) {
            // No platform bridge yet. On iOS the generated bootstrap runs from main() before
            // Display.init, so this is the ordinary path rather than an error -- but it has to
            // be remembered, or the platform never learns what this app can do.
            synchronized (pending) {
                declarationsOwed = true;
            }
            return;
        }
        if (!b.areIntentsSupported()) {
            return;
        }
        publishTo(b, d);
    }

    private static void publishTo(IntentBridge b, IntentDispatcher d) {
        if (!b.areIntentsSupported()) {
            return;
        }
        try {
            b.registerIntents(IntentSerializer.serializeDeclarations(d.describe()));
        } catch (Throwable t) {
            logError(t);
        }
        rememberDeclaredIds(d);
    }

    /// Records which activity types belong to this application's intents, for the next cold
    /// start to consult before its dispatcher exists.
    ///
    /// Written every publication rather than once: the set changes when the app is updated, and
    /// a stale entry would have the framework claim an activity type the app has since given
    /// back to its own continuation handling.
    private static void rememberDeclaredIds(IntentDispatcher d) {
        try {
            Preferences.set(DECLARED_IDS_KEY, joinDeclaredIds(d));
        } catch (Throwable t) {
            // Persisting is an optimisation for one launch, never a requirement.
            logError(t);
        }
    }

    /// The declared ids as one comma-separated string.
    ///
    /// Separate from the method above so the loop is not inside a catch(Throwable). Iterating a
    /// List<IntentDeclaration> compiles to a CHECKCAST, and ParparVM does not throw for a
    /// failed cast -- so a handler wrapped around one reads as relying on an exception that
    /// never arrives, and the repo's gate rejects the shape wherever it appears. The caller
    /// still catches, so behaviour is unchanged.
    private static String joinDeclaredIds(IntentDispatcher d) {
        List<IntentDeclaration> all = d.describe();
        if (all == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (IntentDeclaration decl : all) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(decl.getId());
        }
        return sb.toString();
    }

    /// The declaration ids a previous launch recorded, or null when there has never been one.
    ///
    /// Null and empty are different answers and the caller depends on the difference: an empty
    /// record means the app published a list and it was empty, so nothing is ours; no record at
    /// all means a genuinely first launch, which is the only case left to guess about. Returned
    /// as the raw text rather than a tri-state Boolean, because a Boolean that can be null is
    /// one auto-unboxing away from a NullPointerException.
    private static String recordedDeclarationIds() {
        try {
            return Preferences.get(DECLARED_IDS_KEY, null);
        } catch (Throwable t) {
            return null;
        }
    }

    /// Whether a recorded declaration list names this activity type.
    private static boolean recordNames(String recorded, String activityType) {
        for (String id : StringUtil.tokenize(recorded, ",")) {
            if (activityType.equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static IntentResult invokeInternal(String intentId, Map<String, Object> params,
                                               IntentContext ctx) {
        Outcome o = dispatchOnce(intentId, params, ctx);
        // The in-app path navigates unconditionally: the caller is blocked in this method and
        // there is no competing timeout to lose to.
        navigateIfRequested(o.result, o.declaration, o.params);
        return o.result;
    }

    /// Runs the handler and reports what came back, **without navigating**.
    ///
    /// Navigation is separated because it is a side effect on the user's screen, and whether it
    /// is allowed depends on something this cannot know: whether the invocation won its race
    /// against the deadline. A platform invocation that timed out has already been reported as
    /// failed, so bringing the app forward afterwards contradicts what the platform was told.
    private static Outcome dispatchOnce(String intentId, Map<String, Object> params,
                                        IntentContext ctx) {
        IntentDispatcher d;
        synchronized (pending) {
            d = dispatcher;
        }
        if (d == null) {
            return new Outcome(IntentResult.failed(
                    "No intents are declared in this application"), null, null);
        }
        String targetId = intentId;
        Map<String, Object> safe = new HashMap<String, Object>();
        DynamicIntent dyn;
        synchronized (dynamic) {
            dyn = dynamic.get(intentId);
        }
        if (dyn != null) {
            // A parameterization runs its base intent. Bound values go in first so anything
            // supplied at invocation time overrides them -- a binding is a default, not a lock.
            targetId = dyn.getBaseIntentId();
            reduceInto(safe, dyn.getBoundParameters());
        }
        if (params != null) {
            reduceInto(safe, params);
        }
        IntentDeclaration decl = getDeclaration(targetId);
        try {
            IntentResult r = d.invoke(targetId, safe, ctx);
            if (r == null) {
                return new Outcome(IntentResult.failed(
                        "Unknown intent \"" + intentId + "\""), decl, safe);
            }
            return new Outcome(r, decl, safe);
        } catch (Throwable t) {
            logError(t);
            return new Outcome(IntentResult.failed(
                    "The action could not be completed"), decl, safe);
        }
    }

    /// Copies values in, reduced exactly as they would be if they had crossed a platform
    /// boundary.
    ///
    /// Without this the two routes disagreed about the same object: donation reduces an
    /// AppEntity to its id, while in-process dispatch handed the entity itself to the generated
    /// reader, which stringified it as "type:id" and asked BY_ID to resolve that. So a dynamic
    /// intent could fail through Intents.invoke and the simulator, and work once donated --
    /// the least debuggable shape a difference can take.
    private static void reduceInto(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> e : source.entrySet()) {
            Object value = e.getValue();
            Object wire = IntentSerializer.toWire(value);
            // A value the wire format cannot carry -- NaN, an infinity, an object of no declared
            // type -- is still a value the caller supplied. Dropping it would turn "invalid"
            // into "absent", and an optional parameter would then quietly run on its default
            // instead of the coercion rejecting what was actually passed. Only a genuine null
            // means absent.
            target.put(e.getKey(), wire != null ? wire : value);
        }
    }

    /// One handler's result together with what navigating from it would need.
    private static final class Outcome {
        private final IntentResult result;
        private final IntentDeclaration declaration;
        private final Map<String, Object> params;

        Outcome(IntentResult result, IntentDeclaration declaration, Map<String, Object> params) {
            this.result = result;
            this.declaration = declaration;
            this.params = params;
        }
    }

    /// Runs one invocation off the event dispatch thread and reports it once.
    ///
    /// The handler never runs on the EDT. An invocation can arrive while the app
    /// is foregrounded and visible -- a widget button, a search hit on a running
    /// app -- and a handler that blocks the EDT for even a second there is a
    /// visible freeze. Handlers are forbidden from touching UI precisely so they
    /// do not need it.
    private static void run(final PendingInvocation inv) {
        final IntentDeclaration decl = getDeclaration(inv.intentId);
        final int timeout = budgetFor(decl);
        final IntentContext ctx = new IntentContext(inv.source, inv.headless,
                System.currentTimeMillis() + timeout * 1000L);
        final CompletionGuard guard = new CompletionGuard(inv.completion);
        final int timeoutMillis = timeout * 1000;

        Runnable body = new Runnable() {
            @Override
            public void run() {
                Outcome o = dispatchOnce(inv.intentId, inv.params, ctx);
                // Only the winner navigates. A handler that overran its deadline has already
                // had the platform told it failed, and moving the user's screen afterwards
                // contradicts that -- the app foregrounding itself onto a new form for an
                // action the assistant just said did not happen.
                //
                // Claimed, then navigated, then reported: the report releases the Android
                // service's latch and that service tears down the runtime this navigation is
                // using. try/finally because a failure to navigate must not leave the platform
                // waiting forever for an answer that is already decided.
                if (guard.claim()) {
                    try {
                        navigateIfRequested(o.result, o.declaration, o.params);
                    } finally {
                        guard.deliver(o.result);
                    }
                }
            }
        };

        if (!Display.isInitialized()) {
            // No Display: unit tests and the very earliest startup. Run inline
            // so behaviour stays deterministic rather than depending on a thread
            // pool that does not exist yet. The deadline still applies; there is
            // simply nobody else to enforce it.
            body.run();
            return;
        }

        Display.getInstance().startThread(body, "CN1 Intent " + inv.intentId).start();
        Display.getInstance().startThread(new Runnable() {
            @Override
            public void run() {
                // Wait on the guard rather than sleeping the whole budget, so a
                // handler that answers in 50ms does not leave a thread parked
                // for the remaining 20 seconds.
                if (guard.awaitCompletion(timeoutMillis)) {
                    return;
                }
                ctx.cancel();
                guard.complete(IntentResult.failed(
                        "The action took too long and was stopped"));
            }
        }, "CN1 Intent timeout").start();
    }

    /// Makes the one-call guarantee real. The platform side of this boundary --
    /// a Swift continuation on iOS -- crashes hard when it is resumed twice, and
    /// the timeout racing a slow handler is exactly the situation that would do
    /// it, so the check has to be atomic rather than a plain flag test.
    private static final class CompletionGuard {
        private final IntentCompletion completion;
        private boolean done;

        CompletionGuard(IntentCompletion completion) {
            this.completion = completion;
        }

        /// Blocks until the handler completes or the budget runs out. Returns
        /// true when it completed in time.
        boolean awaitCompletion(long millis) {
            long giveUpAt = System.currentTimeMillis() + millis;
            synchronized (this) {
                while (!done) {
                    long left = giveUpAt - System.currentTimeMillis();
                    if (left <= 0) {
                        return false;
                    }
                    try {
                        wait(left);
                    } catch (InterruptedException e) {
                        return done;
                    }
                }
                return true;
            }
        }

        /// Claims the right to report this invocation, without reporting it yet.
        ///
        /// Split from the delivery because the completion is what releases the Android
        /// service's latch, and that service then tears down the only runtime in the process.
        /// Firing it before navigation meant stopContext could run concurrently with
        /// foregrounding and route dispatch, so the destination sometimes never appeared.
        /// Claiming first still stops the timeout thread from reporting a failure underneath us.
        boolean claim() {
            synchronized (this) {
                if (done) {
                    return false;
                }
                done = true;
                notifyAll();
                return true;
            }
        }

        /// Reports an outcome this caller has already claimed.
        void deliver(IntentResult result) {
            if (completion != null) {
                try {
                    completion.onIntentResult(result == null ? IntentResult.ok() : result);
                } catch (Throwable t) {
                    logError(t);
                }
            }
        }

        /// Reports the outcome exactly once. Returns true when this caller is the one that
        /// did, which is also what decides whether it may act on the result.
        boolean complete(IntentResult result) {
            if (!claim()) {
                return false;
            }
            deliver(result);
            return true;
        }
    }

    private static final class PendingActivity {
        private final String activityType;
        private final Map<String, Object> params;

        PendingActivity(String activityType, Map<String, Object> params) {
            this.activityType = activityType;
            this.params = params;
        }
    }

    private static final class PendingInvocation {
        private final String intentId;
        private final Map<String, Object> params;
        private final IntentSource source;
        private final boolean headless;
        private final IntentCompletion completion;

        PendingInvocation(String intentId, Map<String, Object> params, IntentSource source,
                          boolean headless, IntentCompletion completion) {
            this.intentId = intentId;
            this.params = params == null
                    ? Collections.<String, Object>emptyMap()
                    : new HashMap<String, Object>(params);
            this.source = source;
            this.headless = headless;
            this.completion = completion;
        }
    }
}
