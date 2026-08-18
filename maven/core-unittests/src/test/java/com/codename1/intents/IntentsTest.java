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

import com.codename1.intents.spi.IntentBridge;
import com.codename1.io.JSONParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Platform-independent coverage for the portable com.codename1.intents runtime:
 * capability gating against a fake {@link IntentBridge}, the cold-start
 * invocation queue, the exactly-once completion guarantee, declaration merging
 * and wire-format round-trips. Needs no platform Display, which is also why the
 * dispatch path runs inline here and stays deterministic.
 */
class IntentsTest {

    /** Records bridge calls so the outward projections can be asserted. */
    private static final class FakeBridge implements IntentBridge {
        boolean intentsSupported = true;
        boolean headlessSupported = true;
        boolean voiceSupported = true;
        boolean indexingSupported = true;
        String registeredJson;
        String donatedId;
        String donatedParams;
        String indexedJson;
        Map<String, byte[]> indexedImages;
        String removedJson;
        String clearedType = "unset";
        final List<String> completions = new ArrayList<String>();

        public boolean areIntentsSupported() {
            return intentsSupported;
        }

        public boolean isHeadlessExecutionSupported() {
            return headlessSupported;
        }

        public boolean isVoiceInvocationSupported() {
            return voiceSupported;
        }

        public boolean isIndexingSupported() {
            return indexingSupported;
        }

        public void registerIntents(String declarationsJson) {
            registeredJson = declarationsJson;
        }

        public void donate(String intentId, String paramsJson) {
            donatedId = intentId;
            donatedParams = paramsJson;
        }

        public void index(String entitiesJson, Map<String, byte[]> images) {
            indexedJson = entitiesJson;
            indexedImages = images;
        }

        public void removeFromIndex(String idsJson) {
            removedJson = idsJson;
        }

        public void clearIndex(String entityType) {
            clearedType = entityType;
        }

        public void completeInvocation(String token, String resultJson,
                                        Map<String, byte[]> images) {
            completions.add(token);
        }

        boolean canForeground = true;
        int foregroundRequests;

        public boolean requestForeground() {
            foregroundRequests++;
            return canForeground;
        }
    }

    /** A dispatcher standing in for the build-time generated one. */
    private static final class FakeDispatcher implements IntentDispatcher {
        final List<String> invoked = new ArrayList<String>();
        Map<String, Object> lastParams = Collections.emptyMap();
        IntentContext lastContext;
        final List<IntentDeclaration> declarations = new ArrayList<IntentDeclaration>();
        IntentResult next = IntentResult.ok();
        RuntimeException throwOnInvoke;
        List<AppEntity> queryResult = Collections.emptyList();
        String lastQueryKind;
        String lastQueryArgument;

        public List<IntentDeclaration> describe() {
            return declarations;
        }

        public IntentResult invoke(String intentId, Map<String, Object> params,
                                    IntentContext ctx) {
            invoked.add(intentId);
            lastParams = params;
            lastContext = ctx;
            if (throwOnInvoke != null) {
                throw throwOnInvoke;
            }
            if (!"known".equals(intentId)) {
                return null;
            }
            return next;
        }

        public List<AppEntity> queryEntities(String entityType, String kind, String argument) {
            lastQueryKind = kind;
            lastQueryArgument = argument;
            return queryResult;
        }
    }

    private static IntentDeclaration declaration(String id) {
        return new IntentDeclaration(id, "Title of " + id, "", true, true, false,
                "", 5, Arrays.asList("Do " + id + " in ${applicationName}"),
                Collections.<IntentParameterInfo>emptyList(),
                Arrays.asList(Exposure.ASSISTANT));
    }

    @AfterEach
    void tearDown() {
        Intents.reset();
    }

    // ------------------------------------------------------------------
    // Capability gating
    // ------------------------------------------------------------------

    @Test
    void everythingIsInertWithoutABridge() {
        assertFalse(Intents.areIntentsSupported());
        assertFalse(Intents.isHeadlessExecutionSupported());
        assertFalse(Intents.isVoiceInvocationSupported());
        assertFalse(Intents.isIndexingSupported());

        // None of these may throw on a port that cannot support intents.
        Intents.donate("anything", new HashMap<String, Object>());
        Intents.index(new AppEntity("order", "1"));
        Intents.removeFromIndex("order", "1");
        Intents.clearIndex(null);
        assertTrue(Intents.getDeclarations().isEmpty());
    }

    @Test
    void capabilitiesFollowTheBridge() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        assertTrue(Intents.areIntentsSupported());
        assertTrue(Intents.isVoiceInvocationSupported());

        b.voiceSupported = false;
        assertFalse(Intents.isVoiceInvocationSupported());
        // Voice going away must not take the rest of the feature with it -- that
        // difference is exactly what Android looks like.
        assertTrue(Intents.areIntentsSupported());
    }

    @Test
    void indexingIsSkippedWhenTheBridgeCannotIndex() {
        FakeBridge b = new FakeBridge();
        b.indexingSupported = false;
        Intents.setBridge(b);

        Intents.index(new AppEntity("order", "1").setTitle("Order 1"));
        Intents.removeFromIndex("order", "1");
        Intents.clearIndex("order");

        assertNull(b.indexedJson);
        assertNull(b.removedJson);
        assertEquals("unset", b.clearedType);
    }

    // ------------------------------------------------------------------
    // Dispatch
    // ------------------------------------------------------------------

    @Test
    void invokeReachesTheDispatcher() {
        FakeDispatcher d = new FakeDispatcher();
        d.next = IntentResult.spoken("done");
        Intents.setDispatcher(d);

        IntentResult r = Intents.invoke("known", null);

        assertEquals(Arrays.asList("known"), d.invoked);
        assertFalse(r.isFailed());
        assertEquals("done", r.getDialog());
    }

    @Test
    void invokeWithoutADispatcherFailsCleanly() {
        IntentResult r = Intents.invoke("known", null);
        assertTrue(r.isFailed());
        assertNotNull(r.getErrorMessage());
    }

    @Test
    void unknownIntentIdFailsRatherThanThrowing() {
        Intents.setDispatcher(new FakeDispatcher());
        IntentResult r = Intents.invoke("nope", null);
        assertTrue(r.isFailed());
        assertTrue(r.getErrorMessage().contains("nope"));
    }

    @Test
    void aThrowingHandlerBecomesAFailedResult() {
        FakeDispatcher d = new FakeDispatcher();
        d.throwOnInvoke = new IllegalStateException("boom");
        Intents.setDispatcher(d);

        IntentResult r = Intents.invoke("known", null);

        assertTrue(r.isFailed());
        // The user-visible message must not be the exception text.
        assertFalse(r.getErrorMessage().contains("boom"));
    }

    @Test
    void invocationsArrivingBeforeTheDispatcherAreQueuedAndDrainedInOrder() {
        final List<String> results = new ArrayList<String>();
        IntentCompletion collect = new IntentCompletion() {
            public void onIntentResult(IntentResult result) {
                results.add(result.getDialog());
            }
        };

        FakeDispatcher d = new FakeDispatcher();
        d.next = IntentResult.spoken("ok");

        // A shortcut tap is what cold-started the process: the invocation lands
        // before the generated dispatcher has installed itself.
        Intents.dispatchInvocation("known", null, IntentSource.SHORTCUT, true, collect);
        Intents.dispatchInvocation("known", null, IntentSource.SHORTCUT, true, collect);
        assertTrue(d.invoked.isEmpty(), "nothing may run before a dispatcher exists");

        Intents.setDispatcher(d);

        assertEquals(Arrays.asList("known", "known"), d.invoked);
        assertEquals(Arrays.asList("ok", "ok"), results);
    }

    @Test
    void theContextCarriesTheSourceAndHeadlessFlag() {
        final IntentContext[] seen = new IntentContext[1];
        IntentDispatcher d = new IntentDispatcher() {
            public List<IntentDeclaration> describe() {
                return Collections.emptyList();
            }

            public IntentResult invoke(String intentId, Map<String, Object> params,
                                        IntentContext ctx) {
                seen[0] = ctx;
                return IntentResult.ok();
            }

            public List<AppEntity> queryEntities(String t, String k, String a) {
                return Collections.emptyList();
            }
        };
        Intents.setDispatcher(d);

        Intents.dispatchInvocation("known", null, IntentSource.VOICE, true, null);

        assertNotNull(seen[0]);
        assertEquals(IntentSource.VOICE, seen[0].getSource());
        assertTrue(seen[0].isHeadless());
        assertTrue(seen[0].getDeadline() > System.currentTimeMillis());
    }

    @Test
    void inAppInvocationIsNeverMarkedHeadless() {
        final IntentContext[] seen = new IntentContext[1];
        Intents.setDispatcher(new IntentDispatcher() {
            public List<IntentDeclaration> describe() {
                return Collections.emptyList();
            }

            public IntentResult invoke(String id, Map<String, Object> p, IntentContext ctx) {
                seen[0] = ctx;
                return IntentResult.ok();
            }

            public List<AppEntity> queryEntities(String t, String k, String a) {
                return Collections.emptyList();
            }
        });

        Intents.invoke("known", null);

        assertFalse(seen[0].isHeadless(),
                "an in-app call happens with the app on screen, so the UI restrictions do not apply");
        assertEquals(IntentSource.IN_APP, seen[0].getSource());
    }

    @Test
    void aResultThatNamesARouteNavigatesToIt() {
        // The platforms only know how to bring the app forward; the route table is Java, so the
        // framework has to do the navigating or an opens() result foregrounds the app onto
        // whatever screen it happened to be showing.
        final List<String> navigated = new ArrayList<String>();
        com.codename1.router.Navigation.setDispatcher(new com.codename1.router.RouteDispatcher() {
            public com.codename1.ui.Form dispatch(String url) {
                navigated.add(url);
                return null;
            }
        });
        try {
            FakeDispatcher d = new FakeDispatcher();
            d.next = IntentResult.opens("/orders/42");
            Intents.setDispatcher(d);

            Intents.invoke("known", null);

            assertEquals(Arrays.asList("/orders/42"), navigated);
        } finally {
            com.codename1.router.Navigation.setDispatcher(null);
        }
    }

    @Test
    void aDeclaredRouteTemplateIsExpandedFromTheBoundValues() {
        // opensRoute exists so a handler can return ok() and still be an "open the app here"
        // intent; without expansion the app foregrounded onto whatever screen it was showing.
        final List<String> navigated = new ArrayList<String>();
        com.codename1.router.Navigation.setDispatcher(new com.codename1.router.RouteDispatcher() {
            public com.codename1.ui.Form dispatch(String url) {
                navigated.add(url);
                return null;
            }
        });
        try {
            FakeDispatcher d = new FakeDispatcher();
            d.declarations.add(new IntentDeclaration("known", "Known", "", false, true, false,
                    "/orders/{orderId}", 5, Collections.<String>emptyList(),
                    Collections.<IntentParameterInfo>emptyList(),
                    Arrays.asList(Exposure.ASSISTANT)));
            d.next = IntentResult.ok();
            Intents.setDispatcher(d);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("orderId", "42");
            Intents.invoke("known", params);

            assertEquals(Arrays.asList("/orders/42"), navigated);
        } finally {
            com.codename1.router.Navigation.setDispatcher(null);
        }
    }

    @Test
    void aTemplateWithNoValueForAPlaceholderDoesNotNavigate() {
        final List<String> navigated = new ArrayList<String>();
        com.codename1.router.Navigation.setDispatcher(new com.codename1.router.RouteDispatcher() {
            public com.codename1.ui.Form dispatch(String url) {
                navigated.add(url);
                return null;
            }
        });
        try {
            FakeDispatcher d = new FakeDispatcher();
            d.declarations.add(new IntentDeclaration("known", "Known", "", false, true, false,
                    "/orders/{orderId}", 5, Collections.<String>emptyList(),
                    Collections.<IntentParameterInfo>emptyList(),
                    Arrays.asList(Exposure.ASSISTANT)));
            d.next = IntentResult.ok();
            Intents.setDispatcher(d);

            Intents.invoke("known", null);

            assertTrue(navigated.isEmpty(),
                    "a half-expanded URL would route somewhere unintended");
        } finally {
            com.codename1.router.Navigation.setDispatcher(null);
        }
    }

    @Test
    void aFailedResultDoesNotNavigate() {
        final List<String> navigated = new ArrayList<String>();
        com.codename1.router.Navigation.setDispatcher(new com.codename1.router.RouteDispatcher() {
            public com.codename1.ui.Form dispatch(String url) {
                navigated.add(url);
                return null;
            }
        });
        try {
            FakeDispatcher d = new FakeDispatcher();
            d.next = IntentResult.failed("nope").withOpenUrl("/orders/42");
            Intents.setDispatcher(d);

            Intents.invoke("known", null);

            assertTrue(navigated.isEmpty(),
                    "a failure that also carries a route must not navigate as if it worked");
        } finally {
            com.codename1.router.Navigation.setDispatcher(null);
        }
    }

    // ------------------------------------------------------------------
    // Declarations
    // ------------------------------------------------------------------

    @Test
    void declarationsMergeGeneratedAndRuntimeSources() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("known"));
        Intents.setDispatcher(d);
        Intents.registerDynamicIntent(new DynamicIntent("at_runtime", "known", "My usual"));

        List<IntentDeclaration> all = Intents.getDeclarations();

        assertEquals(2, all.size());
        assertNotNull(Intents.getDeclaration("known"));
        assertNotNull(Intents.getDeclaration("at_runtime"));
        assertNull(Intents.getDeclaration("never_declared"));
    }

    @Test
    void aParameterizationRunsItsBaseIntent() {
        // Registering one used to add an id nothing could run: the dispatcher only knows
        // build-time ids, so invoking it reported an unknown intent.
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("known"));
        d.next = IntentResult.spoken("ran");
        Intents.setDispatcher(d);
        Intents.registerDynamicIntent(new DynamicIntent("my_usual", "known", "My usual")
                .bind("size", "large"));

        IntentResult r = Intents.invoke("my_usual", null);

        assertFalse(r.isFailed(), "a registered parameterization must be invokable");
        assertEquals(Arrays.asList("known"), d.invoked, "it runs the intent it names");
        assertEquals("large", d.lastParams.get("size"), "bound values are supplied");
    }

    @Test
    void aSuppliedValueOverridesABoundOne() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("known"));
        Intents.setDispatcher(d);
        Intents.registerDynamicIntent(new DynamicIntent("my_usual", "known", "My usual")
                .bind("size", "large"));

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("size", "small");
        Intents.invoke("my_usual", params);

        assertEquals("small", d.lastParams.get("size"),
                "a binding is a default, not a lock");
    }

    @Test
    void aParameterizationOfAnUndeclaredIntentIsRejected() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("known"));
        Intents.setDispatcher(d);

        Intents.registerDynamicIntent(new DynamicIntent("orphan", "never_declared", "Orphan"));

        assertNull(Intents.getDeclaration("orphan"),
                "registering it would advertise something nothing can run");
    }

    @Test
    void aParameterizationCannotShadowADeclaredIntent() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("known"));
        Intents.setDispatcher(d);

        Intents.registerDynamicIntent(new DynamicIntent("known", "known", "Impostor"));

        assertEquals(1, Intents.getDeclarations().size());
        assertEquals("Title of known", Intents.getDeclaration("known").getTitle());
    }

    @Test
    void aBoundParameterIsNoLongerSomethingTheCallerSupplies() {
        IntentParameterInfo size = new IntentParameterInfo("size", "Size",
                IntentParameterType.STRING, true, null, null, null);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(new IntentDeclaration("known", "Known", "", true, true, false,
                "", 5, Collections.<String>emptyList(), Arrays.asList(size),
                Arrays.asList(Exposure.ASSISTANT)));
        Intents.setDispatcher(d);
        Intents.registerDynamicIntent(new DynamicIntent("my_usual", "known", "My usual")
                .bind("size", "large"));

        assertTrue(Intents.getDeclaration("my_usual").getParameters().isEmpty(),
                "the platform must not ask for a value that is already bound");
    }

    @Test
    void aDynamicIntentRequiresABaseIntent() {
        try {
            new DynamicIntent("my_usual", null, "My usual");
            org.junit.jupiter.api.Assertions.fail("a base intent is required");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    void installingTheDispatcherPublishesTheCatalogueToThePlatform() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("built_in"));

        Intents.setDispatcher(d);

        assertNotNull(b.registeredJson);
        assertTrue(b.registeredJson.contains("built_in"));
    }

    @Test
    void aDeclarationExposesItsParametersByName() {
        IntentParameterInfo p = new IntentParameterInfo("shop", "Which shop?",
                IntentParameterType.ENTITY, true, "coffee_shop", null, null);
        IntentDeclaration d = new IntentDeclaration("order", "Order", "", false, true, false,
                "", 5, Collections.<String>emptyList(), Arrays.asList(p),
                Arrays.asList(Exposure.ASSISTANT));

        assertSame(p, d.getParameter("shop"));
        assertNull(d.getParameter("absent"));
        assertTrue(d.isExposedTo(Exposure.ASSISTANT));
        assertFalse(d.isExposedTo(Exposure.MODEL),
                "a model must not reach an intent that never opted in");
    }

    @Test
    void anActivityArrivingBeforeTheDeclarationsIsRunOnceTheyExist() {
        // A donated shortcut tapped on a dead process delivers the activity before the
        // generated dispatcher installs itself. Rejecting it there made the app launch and do
        // nothing.
        assertTrue(Intents.dispatchUserActivity("known", null),
                "an id shaped like ours is claimed while the table is still empty");

        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("known"));
        Intents.setDispatcher(d);

        assertEquals(Arrays.asList("known"), d.invoked);
    }

    /// The queue and the dispatcher have to be one decision. Two locks let setDispatcher
    /// install and drain an empty queue between an activity being told "not ready" and being
    /// enqueued, after which nothing ever drains it again -- an activity claimed and then
    /// silently lost, which is worse than never claiming it. Racing threads cannot pin that;
    /// this pins the invariant instead, that an activity is only ever claimed while the
    /// dispatcher is genuinely absent.
    @Test
    void anActivityIsNeverClaimedOnceTheDispatcherExists() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("known"));
        Intents.setDispatcher(d);

        assertFalse(Intents.dispatchUserActivity("never_declared", null),
                "the table exists and does not declare it, so it belongs to somebody else");
        assertTrue(d.invoked.isEmpty());

        // And nothing was left behind: installing a dispatcher again must not surface an
        // activity that was refused rather than queued.
        FakeDispatcher second = new FakeDispatcher();
        second.declarations.add(declaration("never_declared"));
        Intents.setDispatcher(second);

        assertTrue(second.invoked.isEmpty(), "a refused activity must not be queued at all");
    }

    @Test
    void aThirdPartyActivityIsNeverClaimed() {
        // Handoff and third-party types are reverse-DNS, so claiming everything while the table
        // is empty would swallow activities belonging to somebody else.
        assertFalse(Intents.dispatchUserActivity("com.example.SomeOtherActivity", null));
        assertFalse(Intents.dispatchUserActivity("NSUserActivityTypeBrowsingWeb", null));
    }

    @Test
    void aQueuedActivityNothingDeclaresIsDropped() {
        Intents.dispatchUserActivity("never_declared", null);

        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("known"));
        Intents.setDispatcher(d);

        assertTrue(d.invoked.isEmpty());
    }

    @Test
    void aModelOnlyIntentIsNotDonatedToThePlatform() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(new IntentDeclaration("model_only", "Model", "", true, true, false,
                "", 5, Collections.<String>emptyList(),
                Collections.<IntentParameterInfo>emptyList(), Arrays.asList(Exposure.MODEL)));
        Intents.setDispatcher(d);

        Intents.donate("model_only", null);

        assertNull(b.donatedId,
                "donation is a platform surface, so exposure has to gate it too");
    }

    @Test
    void aDynamicIdCannotContainAColon() {
        // Indexed entities are published as type:id, and Android identifies them by that
        // separator -- a dynamic id containing one would be swept up by clearIndex.
        try {
            new DynamicIntent("call:mum", "known", "Call Mum");
            org.junit.jupiter.api.Assertions.fail("a colon must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("colon"), expected.getMessage());
        }
    }

    @Test
    void onlyModelExposedIntentsBecomeTools() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("assistant_only"));
        d.declarations.add(new IntentDeclaration("model_one", "Model", "Does a thing", true,
                true, false, "", 5, Collections.<String>emptyList(),
                Collections.<IntentParameterInfo>emptyList(), Arrays.asList(Exposure.MODEL)));
        Intents.setDispatcher(d);

        java.util.List<com.codename1.ai.Tool> tools = Intents.asTools();

        assertEquals(1, tools.size(), "a model must not reach an intent that never opted in");
        assertEquals("model_one", tools.get(0).getName());
        assertEquals("Does a thing", tools.get(0).getDescription());
    }

    @Test
    void aToolSchemaDescribesTheDeclaredParameters() {
        IntentParameterInfo kind = new IntentParameterInfo("kind", "What kind?",
                IntentParameterType.STRING, true, null, null, Arrays.asList("run", "ride"));
        IntentParameterInfo minutes = new IntentParameterInfo("minutes", "How long?",
                IntentParameterType.INTEGER, false, null, null, null);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(new IntentDeclaration("log_workout", "Log", "", true, true, false,
                "", 5, Collections.<String>emptyList(), Arrays.asList(kind, minutes),
                Arrays.asList(Exposure.MODEL)));
        Intents.setDispatcher(d);

        Map schema = parse(Intents.asTools().get(0).getParametersJsonSchema());
        Map props = (Map) schema.get("properties");

        assertEquals("integer", ((Map) props.get("minutes")).get("type"));
        assertNotNull(((Map) props.get("kind")).get("enum"), "a closed vocabulary is offered");
        // Only the required one is listed, so the model may omit the other.
        assertEquals(1, ((List) schema.get("required")).size());
    }

    /// A schema that says "string" and nothing else lets a model send an ISO date, a weekday
    /// name or a sentence, all schema-valid and all of which the dispatcher reads as null. The
    /// description is the only place the accepted forms can be stated, so it has to state them.
    /// The build rejects a non-positive timeoutSeconds, but a DynamicIntent or a declaration
    /// built at runtime can still carry one -- and every caller has to read it the same way.
    /// Intents.invoke used the raw value, so a handler declaring 0 was handed a context that
    /// had already expired before it ran a line.
    @Test
    void aNonPositiveDeclaredTimeoutFallsBackToTheDefault() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(new IntentDeclaration("known", "Known", "", true, true, false,
                "", 0, Collections.<String>emptyList(),
                Collections.<IntentParameterInfo>emptyList(),
                Arrays.asList(Exposure.ASSISTANT)));
        d.next = IntentResult.ok();
        Intents.setDispatcher(d);

        Intents.invoke("known", null);

        assertEquals(Arrays.asList("known"), d.invoked);
        assertNotNull(d.lastContext);
        assertFalse(d.lastContext.isCancelled(),
                "a declared 0 must not hand the handler an expired context");
        assertTrue(d.lastContext.getRemainingTime() > 0,
                "and it must have real time left, got " + d.lastContext.getRemainingTime());
    }

    @Test
    void aDateParameterTellsTheModelWhichFormsAreAccepted() {
        IntentParameterInfo when = new IntentParameterInfo("when", "When?",
                IntentParameterType.DATE, true, null, null, null);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(new IntentDeclaration("log_workout", "Log", "", true, true, false,
                "", 5, Collections.<String>emptyList(), Arrays.asList(when),
                Arrays.asList(Exposure.MODEL)));
        Intents.setDispatcher(d);

        Map props = (Map) parse(Intents.asTools().get(0).getParametersJsonSchema())
                .get("properties");
        String description = (String) ((Map) props.get("when")).get("description");

        assertTrue(description.contains("ISO-8601"), description);
        assertTrue(description.contains("epoch milliseconds"), description);
    }

    @Test
    void aToolRunsTheIntentAndReturnsItsSerializedResult() throws Exception {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(new IntentDeclaration("known", "Known", "", true, true, false,
                "", 5, Collections.<String>emptyList(),
                Collections.<IntentParameterInfo>emptyList(), Arrays.asList(Exposure.MODEL)));
        d.next = IntentResult.value("A-42").withDialog("done");
        Intents.setDispatcher(d);

        String json = Intents.asTools().get(0).invoke("{\"size\":\"large\"}");

        assertEquals(Arrays.asList("known"), d.invoked);
        assertEquals("large", d.lastParams.get("size"));
        Map doc = parse(json);
        assertEquals("A-42", doc.get("value"));
        assertEquals("done", doc.get("dialog"));
    }

    @Test
    void aRouteValueIsEncodedSoItCannotChangeTheUrlStructure() {
        final List<String> navigated = new ArrayList<String>();
        com.codename1.router.Navigation.setDispatcher(new com.codename1.router.RouteDispatcher() {
            public com.codename1.ui.Form dispatch(String url) {
                navigated.add(url);
                return null;
            }
        });
        try {
            FakeDispatcher d = new FakeDispatcher();
            d.declarations.add(new IntentDeclaration("known", "Known", "", false, true, false,
                    "/orders/{orderId}", 5, Collections.<String>emptyList(),
                    Collections.<IntentParameterInfo>emptyList(),
                    Arrays.asList(Exposure.ASSISTANT)));
            d.next = IntentResult.ok();
            Intents.setDispatcher(d);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("orderId", "a/b");
            Intents.invoke("known", params);

            assertEquals(1, navigated.size());
            assertFalse(navigated.get(0).contains("orders/a/b"),
                    "an unencoded slash adds a path segment and stops matching the route");
        } finally {
            com.codename1.router.Navigation.setDispatcher(null);
        }
    }

    // ------------------------------------------------------------------
    // AppEntity queries
    // ------------------------------------------------------------------

    @Test
    void entityQueriesReachTheDispatcher() {
        FakeDispatcher d = new FakeDispatcher();
        d.queryResult = Arrays.asList(new AppEntity("playlist", "1").setTitle("Focus"));
        Intents.setDispatcher(d);

        List<AppEntity> out = Intents.queryEntities("playlist", "search", "foc");

        assertEquals(1, out.size());
        assertEquals("Focus", out.get(0).getTitle());
        assertEquals("search", d.lastQueryKind);
        assertEquals("foc", d.lastQueryArgument);
    }

    @Test
    void entityQueriesWithoutADispatcherReturnEmptyRatherThanNull() {
        assertTrue(Intents.queryEntities("playlist", "suggested", null).isEmpty());
    }

    // ------------------------------------------------------------------
    // Donation and indexing
    // ------------------------------------------------------------------

    @Test
    void donationReachesTheBridgeWithSerializedParameters() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("log_workout"));
        Intents.setDispatcher(d);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("minutes", Integer.valueOf(20));
        params.put("kind", "run");
        Intents.donate("log_workout", params);

        assertEquals("log_workout", b.donatedId);
        Map parsed = parse(b.donatedParams);
        assertEquals("run", parsed.get("kind"));
    }

    /// A donation is durable in a way an ordinary call is not: Android persists a shortcut and
    /// iOS records an activity the system may suggest for weeks. One published for an id nothing
    /// declares can never dispatch, so it becomes a launcher entry or a Siri suggestion that
    /// opens the app and does nothing, with no later moment at which it gets cleaned up. A typo
    /// in an id is the ordinary way to arrive here.
    @Test
    void donatingAnUndeclaredIntentIsRefused() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("log_workout"));
        Intents.setDispatcher(d);

        Intents.donate("log_workuot", new HashMap<String, Object>());

        assertNull(b.donatedId, "a misspelled id must not reach the platform");
    }

    /// A headless handler that decides on a route at runtime has no window to show it in --
    /// the runtime it ran in has nothing on screen. Navigating without asking the port to bring
    /// the app forward builds the destination Form where nobody can see it.
    @Test
    void aHeadlessResultThatOpensARouteAsksThePortToForeground() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(new IntentDeclaration("known", "Known", "", true, true, false,
                "", 5, Collections.<String>emptyList(),
                Collections.<IntentParameterInfo>emptyList(),
                Arrays.asList(Exposure.ASSISTANT)));
        d.next = IntentResult.opens("/orders/42");
        Intents.setDispatcher(d);

        Intents.invoke("known", null);

        assertEquals(1, b.foregroundRequests);
    }

    /// The platform omits an optional parameter it was not given, and the generated dispatcher
    /// supplies the declared default to the handler. Route expansion reading only the supplied
    /// map made the route unexpandable for exactly those intents, so the invocation succeeded
    /// and the user was left on whatever screen was already showing.
    @Test
    void aRouteExpandsFromADeclaredDefaultWhenTheValueWasOmitted() {
        final List<String> navigated = new ArrayList<String>();
        com.codename1.router.Navigation.setDispatcher(new com.codename1.router.RouteDispatcher() {
            public com.codename1.ui.Form dispatch(String url) {
                navigated.add(url);
                return null;
            }
        });
        try {
            IntentParameterInfo tab = new IntentParameterInfo("tab", "Which tab?",
                    IntentParameterType.STRING, false, null, "summary", null);
            FakeDispatcher d = new FakeDispatcher();
            d.declarations.add(new IntentDeclaration("known", "Known", "", false, true, false,
                    "/reports/{tab}", 5, Collections.<String>emptyList(),
                    Arrays.asList(tab), Arrays.asList(Exposure.ASSISTANT)));
            d.next = IntentResult.ok();
            Intents.setDispatcher(d);

            Intents.invoke("known", null);

            assertEquals(Arrays.asList("/reports/summary"), navigated);
        } finally {
            com.codename1.router.Navigation.setDispatcher(null);
        }
    }

    /// A declared opensRoute is different: the platform already brought the app forward before
    /// the handler ran, which is what the flag is for. Asking again would be a second launch.
    @Test
    void aDeclaredRouteDoesNotAskTwice() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(new IntentDeclaration("known", "Known", "", true, true, false,
                "/orders", 5, Collections.<String>emptyList(),
                Collections.<IntentParameterInfo>emptyList(),
                Arrays.asList(Exposure.ASSISTANT)));
        d.next = IntentResult.ok();
        Intents.setDispatcher(d);

        Intents.invoke("known", null);

        assertEquals(0, b.foregroundRequests);
    }

    /// A null is not a value, so it must not count as a binding. Recording one hid the
    /// parameter from the parameterization's declaration -- telling a model and the simulator
    /// that nothing was needed -- while dispatch still failed its required check and donation
    /// dropped it on the way out, leaving no way to repair the invocation.
    @Test
    void bindingAParameterToNullLeavesItUnbound() {
        IntentParameterInfo kind = new IntentParameterInfo("kind", "What kind?",
                IntentParameterType.STRING, true, null, null, null);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(new IntentDeclaration("log_workout", "Log", "", true, true, false,
                "", 5, Collections.<String>emptyList(), Arrays.asList(kind),
                Arrays.asList(Exposure.ASSISTANT)));
        Intents.setDispatcher(d);

        Map<String, Object> partial = new HashMap<String, Object>();
        partial.put("kind", null);
        Intents.registerDynamicIntent(new DynamicIntent("log_run", "log_workout", "Log a run")
                .bind(partial));

        IntentDeclaration derived = Intents.getDeclaration("log_run");
        assertNotNull(derived);
        assertNotNull(derived.getParameter("kind"),
                "a parameter bound to null still has to be asked for");

        // And binding null over a real value clears it rather than shadowing it.
        DynamicIntent cleared = new DynamicIntent("log_ride", "log_workout", "Log a ride")
                .bind("kind", "ride").bind("kind", null);
        assertTrue(cleared.getBoundParameters().isEmpty());
    }

    /// The generated bootstrap installs the dispatcher before the port has booted, so the first
    /// publication finds no bridge and is deferred. Something has to ask afterwards -- and on
    /// Android that flush is what makes registerIntents run, which is what judges a request
    /// parked at a cold start. Without it the shortcut opened the app and ran nothing.
    @Test
    void declarationsInstalledBeforeTheBridgeArePublishedOnceItExists() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("known"));
        // No bridge yet, exactly as on a cold start.
        Intents.setDispatcher(d);

        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        assertNull(b.registeredJson, "nothing published while there was nowhere to publish to");

        Intents.publishPendingDeclarations();

        assertNotNull(b.registeredJson, "the catalogue has to reach the platform eventually");
        assertTrue(b.registeredJson.contains("known"));
    }

    /// And it must not publish twice: registerIntents is what consumes a parked request on
    /// Android, so a second run would judge it again.
    @Test
    void aDeferredPublicationHappensOnlyOnce() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("known"));
        Intents.setDispatcher(d);

        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        Intents.publishPendingDeclarations();
        b.registeredJson = null;

        Intents.publishPendingDeclarations();

        assertNull(b.registeredJson, "the debt was already settled");
    }

    /// A donation becomes a launcher shortcut on Android and a suggested activity on iOS, and a
    /// tap on either dispatches the handler directly -- past the confirmation the generated App
    /// Intent performs, which is the entire promise of destructive=true. The static-shortcut
    /// generator and the trampoline's unauthenticated policy already refuse destructive intents;
    /// a donation is the same one-tap path.
    @Test
    void donatingADestructiveIntentIsRefused() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(new IntentDeclaration("delete_all", "Delete everything", "",
                true, true, true, "", 5, Collections.<String>emptyList(),
                Collections.<IntentParameterInfo>emptyList(),
                Arrays.asList(Exposure.ASSISTANT)));
        Intents.setDispatcher(d);

        Intents.donate("delete_all", new HashMap<String, Object>());

        assertNull(b.donatedId,
                "a one-tap shortcut must not carry a capability that promised confirmation");
    }

    /// The capability itself is unaffected -- it is still declared, still invocable, and the
    /// assistant still offers it with confirmation. Only the unconfirmed path is closed.
    @Test
    void aDestructiveIntentIsStillDeclaredAndInvocable() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(new IntentDeclaration("known", "Known", "", true, true, true,
                "", 5, Collections.<String>emptyList(),
                Collections.<IntentParameterInfo>emptyList(),
                Arrays.asList(Exposure.ASSISTANT)));
        d.next = IntentResult.ok();
        Intents.setDispatcher(d);

        assertNotNull(Intents.getDeclaration("known"));
        Intents.invoke("known", null);
        assertEquals(Arrays.asList("known"), d.invoked);
    }

    /// A parameterization registered at runtime is a declaration too -- donating one is the
    /// point of DynamicIntent, so the check must not refuse it.
    @Test
    void donatingADynamicIntentIsAllowed() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("log_workout"));
        Intents.setDispatcher(d);
        Intents.registerDynamicIntent(new DynamicIntent("log_run", "log_workout", "Log a run")
                .bind("kind", "run"));

        Intents.donate("log_run", new HashMap<String, Object>());

        assertEquals("log_run", b.donatedId);
    }

    @Test
    void indexingSerializesEntitiesAndCollectsNoImagesWhenThereAreNone() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);

        Intents.index(Arrays.asList(
                new AppEntity("order", "42").setTitle("Two coffees").setSubtitle("Delivered")
                        .addKeywords("coffee", "latte")));

        assertNotNull(b.indexedJson);
        assertTrue(b.indexedJson.contains("Two coffees"));
        assertTrue(b.indexedJson.contains("latte"));
        assertNotNull(b.indexedImages);
        assertTrue(b.indexedImages.isEmpty());
    }

    /// Both sides of an index removal are addressed by `uid`, which is `type:id`. The ports
    /// match on it -- the JavaSE bridge keys its whole index by it -- so this pins the contract
    /// they rely on. Matching a bare id instead made removing order:42 also hide customer:42.
    /// A platform index stores one opaque identifier per entry and hands it back on a tap, so
    /// the type rides inside it as "type:id" and is split at the first colon. A colon in the
    /// type moves that boundary, and the application cannot recognise its own entity when the
    /// user taps it -- with nothing to see at the point the mistake was made.
    @Test
    void anEntityTypeMayNotContainTheUidSeparator() {
        try {
            new AppEntity("shop:order", "42");
            fail("a colon in the type breaks the identifier the platforms store");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("may not contain"), e.getMessage());
        }

        // The id is the other half and may contain colons: the split takes the first one, so
        // everything after it is the id.
        AppEntity ok = new AppEntity("order", "shop:42");
        assertEquals("order", ok.getType());
        assertEquals("shop:42", ok.getId());
    }

    @Test
    void indexedEntitiesAndRemovalsAgreeOnACompositeUid() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);

        Intents.index(Arrays.asList(new AppEntity("order", "42").setTitle("Two coffees"),
                new AppEntity("customer", "42").setTitle("Ada")));
        Intents.removeFromIndex("order", "42");

        assertTrue(b.indexedJson.contains("\"order:42\""));
        assertTrue(b.indexedJson.contains("\"customer:42\""),
                "a batch publishes every entity, each with its own identity");
        assertTrue(b.removedJson.contains("\"order:42\""));
        assertFalse(b.removedJson.contains("\"customer:42\""),
                "a removal names one entity, and the type is half of what names it");
    }

    /// The platforms disagree about a missing title rather than degrading alike: Android hands
    /// an empty long label to ShortcutInfo.Builder and can reject the shortcut, while iOS
    /// publishes a searchable item with nothing written on it. The build enforces this for a
    /// declared @IntentEntity through @EntityTitle; direct construction skips the build.
    @Test
    void anEntityWithoutATitleIsNotIndexed() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);

        Intents.index(new AppEntity("order", "42"));

        assertNull(b.indexedJson, "a search result with nothing written on it cannot be acted on");
    }

    /// And one bad entity must not take the rest of the batch with it.
    @Test
    void anUntitledEntityIsSkippedWithoutLosingItsSiblings() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);

        Intents.index(Arrays.asList(
                new AppEntity("order", "42"),
                new AppEntity("order", "43").setTitle("Two coffees")));

        assertNotNull(b.indexedJson);
        assertTrue(b.indexedJson.contains("order:43"));
        assertFalse(b.indexedJson.contains("order:42"));
    }

    @Test
    void indexingAnEmptyListDoesNotCallThePlatform() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);

        Intents.index(new ArrayList<AppEntity>());

        assertNull(b.indexedJson);
    }

    @Test
    void removalCarriesTheTypeAndId() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);

        Intents.removeFromIndex("order", "42");

        assertNotNull(b.removedJson);
        assertTrue(b.removedJson.contains("order"));
        assertTrue(b.removedJson.contains("42"));
    }

    // ------------------------------------------------------------------
    // Wire format
    // ------------------------------------------------------------------

    @Test
    void declarationsRoundTripThroughTheWireFormat() {
        IntentParameterInfo p = new IntentParameterInfo("kind", "What kind?",
                IntentParameterType.STRING, true, null, "run",
                Arrays.asList("run", "ride"));
        IntentDeclaration d = new IntentDeclaration("log_workout", "Log a workout",
                "Records one", true, true, false, "/workouts/{kind}", 5,
                Arrays.asList("Log a workout in ${applicationName}"),
                Arrays.asList(p), Arrays.asList(Exposure.ASSISTANT));

        Map doc = parse(IntentSerializer.serializeDeclarations(Arrays.asList(d)));

        List intents = (List) doc.get("intents");
        assertEquals(1, intents.size());
        Map m = (Map) intents.get(0);
        assertEquals("log_workout", m.get("id"));
        assertEquals("true", String.valueOf(m.get("headless")));
        assertEquals("/workouts/{kind}", m.get("opensRoute"));

        List params = (List) m.get("params");
        Map pm = (Map) params.get(0);
        assertEquals("kind", pm.get("name"));
        assertEquals("string", pm.get("type"));
        assertEquals("run", pm.get("default"));
        assertEquals(2, ((List) pm.get("options")).size());
    }

    @Test
    void aFailedResultCarriesItsMessageAndNothingElse() {
        Map doc = parse(IntentSerializer.serializeResult(
                IntentResult.failed("Could not reach the server"), new HashMap<String, byte[]>()));

        assertEquals("false", String.valueOf(doc.get("ok")));
        assertEquals("Could not reach the server", doc.get("error"));
        assertNull(doc.get("value"));
    }

    @Test
    void aSuccessfulResultCarriesValueDialogAndRoute() {
        IntentResult r = IntentResult.value("A-42")
                .withDialog("On its way")
                .withOpenUrl("/orders/42");

        Map doc = parse(IntentSerializer.serializeResult(r, new HashMap<String, byte[]>()));

        assertEquals("true", String.valueOf(doc.get("ok")));
        assertEquals("A-42", doc.get("value"));
        assertEquals("On its way", doc.get("dialog"));
        assertEquals("/orders/42", doc.get("openUrl"));
    }

    @Test
    void parametersReduceToWireTypesWithoutCasting() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("when", new Date(1234567890L));
        params.put("shop", new AppEntity("coffee_shop", "shop-7"));
        params.put("count", Integer.valueOf(3));
        params.put("flag", Boolean.TRUE);
        // Deliberately unsupported: it must be dropped rather than guessed at.
        params.put("junk", new Object());

        Map parsed = parse(IntentSerializer.serializeParams(params));

        assertEquals("shop-7", parsed.get("shop"),
                "an entity reduces to its id, which is all the platform needs to hand it back");
        assertEquals("true", String.valueOf(parsed.get("flag")));
        assertNotNull(parsed.get("when"));
        assertNull(parsed.get("junk"));
    }

    @Test
    void anEntityRequiresATypeAndAnId() {
        try {
            new AppEntity(null, "1");
            org.junit.jupiter.api.Assertions.fail("a type is required");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
        try {
            new AppEntity("order", "");
            org.junit.jupiter.api.Assertions.fail("an id is required");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Context
    // ------------------------------------------------------------------

    @Test
    void aContextIsCancelledOnceItsDeadlineHasPassed() {
        IntentContext ctx = new IntentContext(IntentSource.VOICE, true,
                System.currentTimeMillis() - 1);
        assertTrue(ctx.isCancelled());
        assertEquals(0, ctx.getRemainingTime());
    }

    @Test
    void cancellingIsVisibleToTheHandler() {
        IntentContext ctx = new IntentContext(IntentSource.VOICE, true,
                System.currentTimeMillis() + 60000);
        assertFalse(ctx.isCancelled());
        ctx.cancel();
        assertTrue(ctx.isCancelled());
    }

    private static Map parse(String json) {
        try {
            return new JSONParser().parseJSON(new StringReader(json));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
