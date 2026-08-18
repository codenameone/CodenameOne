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

    private static IntentDeclaration declarationWithIntParam(String id, String param) {
        return new IntentDeclaration(id, "Title of " + id, "", true, true, false,
                "", 5, Arrays.asList("Do " + id + " in ${applicationName}"),
                Arrays.asList(new IntentParameterInfo(param, "How many?",
                        IntentParameterType.INTEGER, true, null, null, null)),
                Arrays.asList(Exposure.ASSISTANT));
    }

    private static IntentDeclaration declarationWithStringParam(String id, String param,
                                                                List<String> options) {
        return new IntentDeclaration(id, "Title of " + id, "", true, true, false,
                "", 5, Arrays.asList("Do " + id + " in ${applicationName}"),
                Arrays.asList(new IntentParameterInfo(param, "What?",
                        IntentParameterType.STRING, true, null, null, options)),
                Arrays.asList(Exposure.ASSISTANT));
    }

    private static IntentDeclaration modelIntentWithNumeric(String id, String param,
                                                            IntentParameterType type, int bits) {
        return new IntentDeclaration(id, "Title of " + id, "", true, true, false,
                "", 5, Collections.<String>emptyList(),
                Arrays.asList(new IntentParameterInfo(param, "How many?", type, true,
                        null, null, null, bits)),
                Arrays.asList(Exposure.MODEL));
    }

    private static IntentDeclaration modelIntent(String id, boolean destructive) {
        return new IntentDeclaration(id, "Title of " + id, "", true, true, destructive,
                "", 5, Collections.<String>emptyList(),
                Collections.<IntentParameterInfo>emptyList(),
                Arrays.asList(Exposure.MODEL));
    }

    private static IntentDeclaration declarationWithNumericParam(String id, String param,
                                                                 IntentParameterType type,
                                                                 int bits) {
        return new IntentDeclaration(id, "Title of " + id, "", true, true, false,
                "", 5, Arrays.asList("Do " + id + " in ${applicationName}"),
                Arrays.asList(new IntentParameterInfo(param, "How many?", type, true,
                        null, null, null, bits)),
                Arrays.asList(Exposure.ASSISTANT));
    }

    private static IntentDeclaration declarationWithBooleanParam(String id, String param) {
        return new IntentDeclaration(id, "Title of " + id, "", true, true, false,
                "", 5, Arrays.asList("Do " + id + " in ${applicationName}"),
                Arrays.asList(new IntentParameterInfo(param, "On?",
                        IntentParameterType.BOOLEAN, true, null, null, null)),
                Arrays.asList(Exposure.ASSISTANT));
    }

    private static IntentDeclaration declarationWithDateParam(String id, String param) {
        return new IntentDeclaration(id, "Title of " + id, "", true, true, false,
                "", 5, Arrays.asList("Do " + id + " in ${applicationName}"),
                Arrays.asList(new IntentParameterInfo(param, "When?",
                        IntentParameterType.DATE, true, null, null, null)),
                Arrays.asList(Exposure.ASSISTANT));
    }

    private static IntentDeclaration declarationWithEntityParam(String id, String param,
                                                                 String entityType) {
        return new IntentDeclaration(id, "Title of " + id, "", true, true, false,
                "", 5, Arrays.asList("Do " + id + " in ${applicationName}"),
                Arrays.asList(new IntentParameterInfo(param, "Which?",
                        IntentParameterType.ENTITY, true, entityType, null, null)),
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
        // A donated shortcut tapped on a dead process can deliver the activity before the
        // generated dispatcher installs itself. Rejecting it there made the app launch and do
        // nothing.
        //
        // What makes it ours is the list a previous launch recorded, never the shape of the
        // id: an application's own activity type may look exactly like one of these.
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher first = new FakeDispatcher();
        first.declarations.add(declaration("known"));
        Intents.setDispatcher(first);
        Intents.setDispatcher(null);

        assertTrue(Intents.dispatchUserActivity("known", null),
                "an id this application recorded is claimed while the table is empty");

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

    /// Shape is not ownership. An application may declare its own activity type that happens
    /// to look like an intent id, and claiming it on a cold start tells iOS the activity was
    /// handled -- so the app's own continuation never runs, and the queued activity is dropped
    /// later with nothing said. The warm path already answers correctly; the cold one now
    /// consults the list a previous launch recorded.
    @Test
    void anApplicationsOwnActivityTypeIsNotClaimedOnAColdStart() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("known"));
        Intents.setDispatcher(d);

        // A later cold start: the process is new, so there is no dispatcher yet, but what this
        // app declares was recorded by the launch above.
        Intents.setDispatcher(null);

        assertFalse(Intents.dispatchUserActivity("continue_reading", null),
                "an activity type this app never declared belongs to the app, not to intents");
        assertTrue(Intents.dispatchUserActivity("known", null),
                "and one it did declare is still claimed before the dispatcher exists");
    }

    @Test
    void aThirdPartyActivityIsNeverClaimed() {
        // Handoff and third-party types are reverse-DNS, so claiming everything while the table
        // is empty would swallow activities belonging to somebody else.
        assertFalse(Intents.dispatchUserActivity("com.example.SomeOtherActivity", null));
        assertFalse(Intents.dispatchUserActivity("NSUserActivityTypeBrowsingWeb", null));

        // And neither is one that merely looks like an intent id. Nothing has been recorded
        // here, and reaching this point at all means no dispatcher was installed -- which on a
        // real device means the application declared no intents, since the generated bootstrap
        // runs before UIKit can deliver anything. There is nothing here to own.
        assertFalse(Intents.dispatchUserActivity("continue_reading", null),
                "shape is not ownership, and there is no first-launch exception to that");
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

    /// A primitive cannot be absent -- the dispatcher hands the handler the type's zero -- so
    /// the route has to be built from that too. Reading only explicit defaults meant the handler
    /// ran and the foregrounded app stayed on whatever screen it was already showing.
    @Test
    void aRouteExpandsFromAPrimitivesImplicitDefault() {
        final List<String> navigated = new ArrayList<String>();
        com.codename1.router.Navigation.setDispatcher(new com.codename1.router.RouteDispatcher() {
            public com.codename1.ui.Form dispatch(String url) {
                navigated.add(url);
                return null;
            }
        });
        try {
            IntentParameterInfo page = new IntentParameterInfo("page", "Which page?",
                    IntentParameterType.INTEGER, false, null, null, null);
            FakeDispatcher d = new FakeDispatcher();
            d.declarations.add(new IntentDeclaration("known", "Known", "", false, true, false,
                    "/items/{page}", 5, Collections.<String>emptyList(),
                    Arrays.asList(page), Arrays.asList(Exposure.ASSISTANT)));
            d.next = IntentResult.ok();
            Intents.setDispatcher(d);

            Intents.invoke("known", null);

            assertEquals(Arrays.asList("/items/0"), navigated);
        } finally {
            com.codename1.router.Navigation.setDispatcher(null);
        }
    }

    /// A String left out arrives as null, and a URL containing "null" would route somewhere
    /// nobody asked for -- worse than not navigating.
    @Test
    void aRouteWithAnAbsentStringDoesNotExpand() {
        final List<String> navigated = new ArrayList<String>();
        com.codename1.router.Navigation.setDispatcher(new com.codename1.router.RouteDispatcher() {
            public com.codename1.ui.Form dispatch(String url) {
                navigated.add(url);
                return null;
            }
        });
        try {
            IntentParameterInfo tab = new IntentParameterInfo("tab", "Which tab?",
                    IntentParameterType.STRING, false, null, null, null);
            FakeDispatcher d = new FakeDispatcher();
            d.declarations.add(new IntentDeclaration("known", "Known", "", false, true, false,
                    "/reports/{tab}", 5, Collections.<String>emptyList(),
                    Arrays.asList(tab), Arrays.asList(Exposure.ASSISTANT)));
            d.next = IntentResult.ok();
            Intents.setDispatcher(d);

            Intents.invoke("known", null);

            assertTrue(navigated.isEmpty(), "got " + navigated);
        } finally {
            com.codename1.router.Navigation.setDispatcher(null);
        }
    }

    /// Whether an invocation actually runs headless is one question with one answer. It was
    /// being recomputed in four places -- the trampoline, the service's recheck, the parked
    /// path and the donation URI -- and each was fixed as a separate bug when it disagreed.
    @Test
    void aRoutedIntentNeverRunsHeadlessHoweverItWasDeclared() {
        IntentDeclaration routed = new IntentDeclaration("show_order", "Show", "", true, true,
                false, "/orders/{id}", 5, Collections.<String>emptyList(),
                Collections.<IntentParameterInfo>emptyList(), Arrays.asList(Exposure.ASSISTANT));
        IntentDeclaration plain = new IntentDeclaration("log_workout", "Log", "", true, true,
                false, "", 5, Collections.<String>emptyList(),
                Collections.<IntentParameterInfo>emptyList(), Arrays.asList(Exposure.ASSISTANT));

        assertTrue(routed.isHeadless(), "the declaration still says what it said");
        assertFalse(routed.runsHeadless(), "but a route has to open somewhere visible");
        assertTrue(plain.isHeadless());
        assertTrue(plain.runsHeadless());
    }

    /// The convenience parser materialises every number as a Double, which rounds anything past
    /// 2^53 into a different, still-integral number -- so every downstream check accepts it and
    /// the handler acts on an id the caller never sent. Snowflake ids and database keys are
    /// routinely that large, and nothing about the corruption is visible at any layer.
    @Test
    void aLargeIntegerSurvivesTheWireIntact() throws Exception {
        Map<String, Object> parsed = IntentSerializer.parsePayload("{\"id\": 9007199254740993}");

        assertNotNull(parsed);
        Object v = parsed.get("id");
        assertTrue(v instanceof Long, "a whole number has to stay whole, got " + v.getClass());
        assertEquals(9007199254740993L, ((Long) v).longValue());
    }

    /// The constructor rejects a colon in the type; these paths take a type without ever
    /// building an entity, so they never reached that check. removeFromIndex("shop:order","42")
    /// composes the uid "shop:order:42" -- exactly what new AppEntity("shop","order:42")
    /// publishes -- so the call did not fail, it deleted somebody else's content.
    @Test
    void theStringKeyedIndexPathsEnforceTheSameTypeRule() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);

        try {
            Intents.removeFromIndex("shop:order", "42");
            fail("a colon in the type collides with another entity's identifier");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("may not contain"), e.getMessage());
        }
        try {
            Intents.clearIndex("shop:order");
            fail("clearing matches by uid prefix, so it collides the same way");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("may not contain"), e.getMessage());
        }
        assertNull(b.removedJson);
        assertEquals("unset", b.clearedType);

        // The legitimate forms still work, including a colon in the id.
        Intents.removeFromIndex("order", "shop:42");
        assertNotNull(b.removedJson);
        assertTrue(b.removedJson.contains("order:shop:42"));
        Intents.clearIndex("order");
        assertEquals("order", b.clearedType);
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

    /// Reducing arguments to wire types must not turn "invalid" into "absent": an optional
    /// parameter would then quietly run on its default instead of the coercion rejecting what
    /// the caller actually passed. Only a genuine null means absent.
    @Test
    void anUnrepresentableArgumentStaysPresent() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("known"));
        d.next = IntentResult.ok();
        Intents.setDispatcher(d);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("ratio", Double.valueOf(Double.NaN));
        Intents.invoke("known", params);

        assertTrue(d.lastParams.containsKey("ratio"),
                "a value the wire cannot carry is still a value the caller supplied");
    }

    /// A title of spaces is not a title: Android forwards them to ShortcutInfo.Builder and iOS
    /// puts them in the Spotlight title, which is the unusable result the check exists to stop.
    @Test
    void aWhitespaceOnlyTitleIsNotIndexed() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);

        Intents.index(new AppEntity("order", "42").setTitle("   "));

        assertNull(b.indexedJson);
    }

    /// A donation is durable, so a value the wire cannot carry must stop the donation rather
    /// than be dropped from it: the recorded shortcut would otherwise replay the parameter's
    /// default weeks later, doing something other than the thing it was learned from.
    @Test
    void anUnrepresentableDonationValueIsRefused() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("known"));
        Intents.setDispatcher(d);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("ratio", Double.valueOf(Double.POSITIVE_INFINITY));
        Intents.donate("known", params);

        assertNull(b.donatedId, "a donation that cannot carry its values is not a donation");
    }

    /// Binding a value the wire cannot carry leaves the parameter unbound rather than
    /// appearing bound, so it stays visible and suppliable instead of silently defaulting.
    @Test
    void anUnrepresentableBindingIsNotRecorded() {
        DynamicIntent i = new DynamicIntent("nan_ride", "log_workout", "Log a ride")
                .bind("ratio", Double.valueOf(Double.NaN));

        assertFalse(i.getBoundParameters().containsKey("ratio"));
    }

    /// Bound is not satisfied. A binding the declared type cannot accept used to hide the
    /// parameter, so nothing could correct it while dispatch rejected it in the coercion --
    /// the parameterization and every donation from it unusable, with no way to see why.
    @Test
    void aBindingTheTypeCannotAcceptLeavesTheParameterVisible() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declarationWithIntParam("count_it", "count"));
        Intents.setDispatcher(d);
        Intents.registerDynamicIntent(new DynamicIntent("count_abc", "count_it", "Count")
                .bind("count", "abc"));

        IntentDeclaration derived = Intents.getDeclaration("count_abc");
        assertNotNull(derived);
        assertNotNull(derived.getParameter("count"),
                "a binding the coercion would reject has to stay correctable");
    }

    /// The same declaration must not read one way here and another on the device.
    @Test
    void aWhitespaceOnlyParameterPromptFallsBackToTheName() {
        IntentParameterInfo p = new IntentParameterInfo("count", "   ",
                IntentParameterType.INTEGER, true, null, null, null);

        assertEquals("count", p.getTitle());
    }

    /// A binding the type does accept still hides the parameter, which is the whole point of
    /// a parameterization -- this widened what is checked, not what is hidden.
    @Test
    void aValidBindingStillSatisfiesItsParameter() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declarationWithIntParam("count_it", "count"));
        Intents.setDispatcher(d);
        Intents.registerDynamicIntent(new DynamicIntent("count_5", "count_it", "Count five")
                .bind("count", Integer.valueOf(5)));

        assertNull(Intents.getDeclaration("count_5").getParameter("count"));
    }

    /// One rule, every door. A title of spaces is present, so each door's empty-only fallback
    /// declined to fire and the spaces travelled out as an Android shortcut label, an iOS
    /// NSUserActivity.title, a Spotlight row, or a parameter prompt -- an entry the user can
    /// see and cannot read. Four review findings were four instances of this.
    @Test
    void aBlankNameIsNotANameAtAnyDoor() {
        assertEquals("ride_home",
                new DynamicIntent("ride_home", "log_workout", "   ").getTitle(),
                "a parameterization falls back to its id");

        assertEquals("log_workout", declaration("log_workout").getId());
        assertEquals("log_workout",
                new IntentDeclaration("log_workout", "  ", "", true, true, false, "", 5,
                        Collections.<String>emptyList(),
                        Collections.<IntentParameterInfo>emptyList(),
                        Arrays.asList(Exposure.ASSISTANT)).getTitle(),
                "a declaration falls back to its id");

        assertEquals("count", new IntentParameterInfo("count", "\t", 
                IntentParameterType.INTEGER, true, null, null, null).getTitle(),
                "a parameter falls back to its name");

        assertNull(new AppEntity("order", "42").setTitle("   ").getTitle(),
                "an entity has no title rather than a blank one");
    }

    /// The Android trampoline's unauthenticated policy keys on exactly this pair, and the port
    /// has no unit harness of its own, so the contract it depends on is pinned here.
    ///
    /// getDeclaration resolving a parameterization is what made the hole: a fabricated
    /// nonce-free URI naming a guessable dynamic id passed the discoverable/non-destructive
    /// checks and ran the base handler with the user's own bound values. getDynamicIntent
    /// answering for the same id is what lets the trampoline tell the two apart. If either
    /// half ever stopped being true the guard would silently disarm.
    @Test
    void aParameterizationIsResolvableAsBothDeclarationAndDynamicIntent() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("order_coffee"));
        Intents.setDispatcher(d);
        Intents.registerDynamicIntent(
                new DynamicIntent("usual_coffee", "order_coffee", "My usual"));

        assertNotNull(Intents.getDeclaration("usual_coffee"),
                "the policy's own lookup still sees a parameterization as a declaration");
        assertNotNull(Intents.getDynamicIntent("usual_coffee"),
                "so the guard has to be able to recognise one");
        assertNull(Intents.getDynamicIntent("order_coffee"),
                "and must not mistake the build-time intent for one");
    }

    /// A declaration records INTEGER for both int and long and NUMBER for both float and
    /// double, so a binding cannot be checked against the width the handler declared -- while
    /// the coercion checks the real one. Hiding a binding that can never run is unrecoverable;
    /// surfacing one that was already satisfied is harmless, since dispatch merges it anyway.
    /// So an out-of-range binding must leave its parameter visible.
    @Test
    void anOutOfRangeBindingLeavesTheParameterVisible() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declarationWithIntParam("count_it", "count"));
        Intents.setDispatcher(d);

        Intents.registerDynamicIntent(new DynamicIntent("count_huge", "count_it", "Huge")
                .bind("count", Double.valueOf(1e20)));
        assertNotNull(Intents.getDeclaration("count_huge").getParameter("count"),
                "past the long range the coercion rejects it, so it must stay correctable");

        Intents.registerDynamicIntent(new DynamicIntent("count_wide", "count_it", "Wide")
                .bind("count", Long.valueOf(5000000000L)));
        assertNotNull(Intents.getDeclaration("count_wide").getParameter("count"),
                "and the declaration cannot tell an int parameter from a long one");

        Intents.registerDynamicIntent(new DynamicIntent("count_frac", "count_it", "Fraction")
                .bind("count", Double.valueOf(1.5)));
        assertNotNull(Intents.getDeclaration("count_frac").getParameter("count"),
                "the coercion rejects a fraction rather than rounding it");
    }

    /// The platforms replay a donation's saved arguments verbatim on tap, with no picker in
    /// between, so a donation that could never run publishes a launcher entry or a Siri
    /// suggestion that fails on every tap for as long as it survives.
    @Test
    void aDonationThatCouldNeverRunIsRefused() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declarationWithIntParam("count_it", "count"));
        Intents.setDispatcher(d);

        Intents.donate("count_it", new HashMap<String, Object>());
        assertNull(b.donatedId, "a required parameter with no value would fail every tap");

        Map<String, Object> wrong = new HashMap<String, Object>();
        wrong.put("count", "abc");
        Intents.donate("count_it", wrong);
        assertNull(b.donatedId, "and so would a value the parameter cannot accept");

        Map<String, Object> good = new HashMap<String, Object>();
        good.put("count", Integer.valueOf(3));
        Intents.donate("count_it", good);
        assertEquals("count_it", b.donatedId, "a donation that can run is still donated");
    }

    /// A parameterization's own bindings satisfy its parameters, and the platform merges them
    /// in -- so the donation must not be refused for values the binding already supplies.
    @Test
    void aParameterizationDonationCountsItsBoundValues() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declarationWithIntParam("count_it", "count"));
        Intents.setDispatcher(d);
        Intents.registerDynamicIntent(new DynamicIntent("count_3", "count_it", "Count three")
                .bind("count", Integer.valueOf(3)));

        Intents.donate("count_3", new HashMap<String, Object>());

        assertEquals("count_3", b.donatedId);
    }

    /// The synthesized declaration hides every parameter the binding satisfied, so an override
    /// of one of those was never examined -- while both bridges merge supplied values on top of
    /// the bindings, so the bad override is exactly what the platform would replay.
    @Test
    void anInvalidOverrideOfABoundValueIsRefused() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declarationWithIntParam("count_it", "count"));
        Intents.setDispatcher(d);
        Intents.registerDynamicIntent(new DynamicIntent("count_3", "count_it", "Count three")
                .bind("count", Integer.valueOf(3)));

        Map<String, Object> override = new HashMap<String, Object>();
        override.put("count", "abc");
        Intents.donate("count_3", override);
        assertNull(b.donatedId, "the override is what would run, so it is what is checked");

        // A valid override still donates, and so does one that leaves the binding alone.
        Map<String, Object> good = new HashMap<String, Object>();
        good.put("count", Integer.valueOf(7));
        Intents.donate("count_3", good);
        assertEquals("count_3", b.donatedId);
    }

    /// A date the coercion would reject must not be donated or hide a parameter: the platforms
    /// replay it verbatim, so the shortcut fails on every tap. The grammar is core's now, so
    /// this checks the same one dispatch checks rather than a second opinion about it.
    @Test
    void aValueThatIsNotAMomentInTimeIsNotADate() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declarationWithDateParam("remind_me", "when"));
        Intents.setDispatcher(d);

        Intents.registerDynamicIntent(new DynamicIntent("remind_bad", "remind_me", "Bad")
                .bind("when", "not-a-date"));
        assertNotNull(Intents.getDeclaration("remind_bad").getParameter("when"));

        Intents.registerDynamicIntent(new DynamicIntent("remind_frac", "remind_me", "Fraction")
                .bind("when", Double.valueOf(1.5)));
        assertNotNull(Intents.getDeclaration("remind_frac").getParameter("when"),
                "1.5 is not a count of milliseconds");

        // The three forms that are moments in time still satisfy it.
        Intents.registerDynamicIntent(new DynamicIntent("remind_iso", "remind_me", "Iso")
                .bind("when", "2026-03-01T09:30:00Z"));
        assertNull(Intents.getDeclaration("remind_iso").getParameter("when"));
        Intents.registerDynamicIntent(new DynamicIntent("remind_ms", "remind_me", "Millis")
                .bind("when", Long.valueOf(1772000000000L)));
        assertNull(Intents.getDeclaration("remind_ms").getParameter("when"));
        Intents.registerDynamicIntent(new DynamicIntent("remind_obj", "remind_me", "Obj")
                .bind("when", new java.util.Date(1772000000000L)));
        assertNull(Intents.getDeclaration("remind_obj").getParameter("when"));
    }

    /// The wire keeps only an entity's id, so an entity of the wrong type arrives as a bare id
    /// that the *declared* type's BY_ID query resolves -- finding nothing, or finding an
    /// unrelated object that happens to share the id and running the handler on it.
    @Test
    void anEntityOfAnotherTypeDoesNotSatisfyTheParameter() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declarationWithEntityParam("open_order", "order", "order"));
        Intents.setDispatcher(d);

        Intents.registerDynamicIntent(new DynamicIntent("open_wrong", "open_order", "Wrong")
                .bind("order", new AppEntity("customer", "42")));
        assertNotNull(Intents.getDeclaration("open_wrong").getParameter("order"),
                "a customer is not an order, however alike their ids look");

        Intents.registerDynamicIntent(new DynamicIntent("open_right", "open_order", "Right")
                .bind("order", new AppEntity("order", "42")));
        assertNull(Intents.getDeclaration("open_right").getParameter("order"));
    }

    /// The coercion reads a string boolean with equalsIgnoreCase, so refusing "TRUE" here
    /// declined to donate a shortcut that would have dispatched perfectly well.
    @Test
    void aBooleanBindingIsReadTheWayTheCoercionReadsIt() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declarationWithBooleanParam("set_flag", "on"));
        Intents.setDispatcher(d);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("on", "TRUE");
        Intents.donate("set_flag", params);
        assertEquals("set_flag", b.donatedId);

        Intents.registerDynamicIntent(new DynamicIntent("flag_off", "set_flag", "Off")
                .bind("on", "False"));
        assertNull(Intents.getDeclaration("flag_off").getParameter("on"),
                "a value the coercion accepts satisfies the parameter");
    }

    /// Taking the Z and ignoring what followed accepted "...T12:00:00Zjunk" -- and worse
    /// "...Z+05:00", which names two different moments -- as UTC. Every caller shares this
    /// parser, so that reached declared defaults, donations and dispatch alike.
    @Test
    void trailingTextAfterAUtcSuffixIsNotADate() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declarationWithDateParam("remind_me", "when"));
        Intents.setDispatcher(d);

        Intents.registerDynamicIntent(new DynamicIntent("remind_junk", "remind_me", "Junk")
                .bind("when", "2026-03-14T12:00:00Zjunk"));
        assertNotNull(Intents.getDeclaration("remind_junk").getParameter("when"));

        Intents.registerDynamicIntent(new DynamicIntent("remind_two", "remind_me", "Two")
                .bind("when", "2026-03-14T12:00:00Z+05:00"));
        assertNotNull(Intents.getDeclaration("remind_two").getParameter("when"),
                "a value naming two zones names no moment");

        // A bare Z is still the whole suffix, and still UTC.
        Intents.registerDynamicIntent(new DynamicIntent("remind_z", "remind_me", "Z")
                .bind("when", "2026-03-14T12:00:00Z"));
        assertNull(Intents.getDeclaration("remind_z").getParameter("when"));
    }

    /// A declaration collapses int and long into INTEGER, so the two callers of the value
    /// check want opposite strictness: hiding a parameter wrongly is unrecoverable, while
    /// refusing a donation wrongly just loses a shortcut that would have dispatched.
    @Test
    void aWideValueIsDonatableEvenThoughItStillSurfacesItsParameter() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declarationWithIntParam("count_it", "count"));
        Intents.setDispatcher(d);

        Map<String, Object> wide = new HashMap<String, Object>();
        wide.put("count", Long.valueOf(5000000000L));
        Intents.donate("count_it", wide);
        assertEquals("count_it", b.donatedId,
                "5000000000 is an ordinary value for a long, and this may be one");

        Intents.registerDynamicIntent(new DynamicIntent("count_wide2", "count_it", "Wide")
                .bind("count", Long.valueOf(5000000000L)));
        assertNotNull(Intents.getDeclaration("count_wide2").getParameter("count"),
                "but it must still stay correctable, since this may be an int");

        // What no width can hold is refused by both.
        b.donatedId = null;
        Map<String, Object> impossible = new HashMap<String, Object>();
        impossible.put("count", Double.valueOf(1e20));
        Intents.donate("count_it", impossible);
        assertNull(b.donatedId);
    }

    /// The declaration records the width now, so neither guess is needed: an int parameter
    /// refuses a donation that would fail on every tap, and a long parameter accepts one that
    /// would dispatch. Both were wrong under a single guessed width, in opposite directions.
    @Test
    void aDonationIsCheckedAgainstTheWidthTheHandlerDeclared() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declarationWithNumericParam("count_int", "count",
                IntentParameterType.INTEGER, 32));
        d.declarations.add(declarationWithNumericParam("count_long", "count",
                IntentParameterType.INTEGER, 64));
        d.declarations.add(declarationWithNumericParam("ratio_float", "ratio",
                IntentParameterType.NUMBER, 32));
        d.declarations.add(declarationWithNumericParam("ratio_double", "ratio",
                IntentParameterType.NUMBER, 64));
        Intents.setDispatcher(d);

        Map<String, Object> wide = new HashMap<String, Object>();
        wide.put("count", Long.valueOf(5000000000L));
        Intents.donate("count_int", wide);
        assertNull(b.donatedId, "an int cannot hold it, so the shortcut would fail every tap");
        Intents.donate("count_long", wide);
        assertEquals("count_long", b.donatedId, "a long holds it perfectly well");

        b.donatedId = null;
        Map<String, Object> huge = new HashMap<String, Object>();
        huge.put("ratio", Double.valueOf(1e100));
        Intents.donate("ratio_float", huge);
        assertNull(b.donatedId, "a float would arrive as Infinity");
        Intents.donate("ratio_double", huge);
        assertEquals("ratio_double", b.donatedId);
    }

    /// Long.MAX_VALUE is exact as a long and rounds up to 2^63 as a double, so checking it
    /// through a double rejected a value the declared type holds perfectly -- and the
    /// invocation accepts, because the generated requiredLong reads an integral box directly.
    @Test
    void anExactLongAtTheBoundaryIsStillDonatable() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declarationWithNumericParam("count_long", "count",
                IntentParameterType.INTEGER, 64));
        Intents.setDispatcher(d);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("count", Long.valueOf(Long.MAX_VALUE));
        Intents.donate("count_long", params);

        assertEquals("count_long", b.donatedId);
    }

    /// A Tool is a name, a schema and a handler: nowhere in it to say "ask first", and
    /// IntentTool.invoke dispatches immediately. So a destructive capability projected here
    /// would let a model delete or spend because it inferred a call, while every other surface
    /// refuses or confirms.
    @Test
    void aDestructiveIntentIsNotOfferedAsAModelTool() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(modelIntent("wipe_all", true));
        d.declarations.add(modelIntent("log_run", false));
        Intents.setDispatcher(d);

        List<com.codename1.ai.Tool> tools = Intents.asTools();

        List<String> names = new ArrayList<String>();
        for (com.codename1.ai.Tool t : tools) {
            names.add(t.getName());
        }
        assertFalse(names.contains("wipe_all"),
                "a model must not be able to run a destructive action on inference alone");
        assertTrue(names.contains("log_run"), "and the rest of the projection is unaffected");
    }

    /// A suggestion the system kept can outlive the policy that allowed it. Tapping one
    /// donated before an update that marked the intent destructive would delete, send or spend
    /// on one tap -- past the confirmation the generated App Intent performs, and past the
    /// check donate() applies to every new donation. The declaration in front of us decides.
    @Test
    void aSuggestedActivityIsRefusedWhenTheDeclarationNoLongerAllowsOneTap() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(new IntentDeclaration("wipe_all", "Wipe", "", true, true, true,
                "", 5, Collections.<String>emptyList(),
                Collections.<IntentParameterInfo>emptyList(),
                Arrays.asList(Exposure.ASSISTANT)));
        d.declarations.add(declaration("log_run"));
        Intents.setDispatcher(d);

        assertTrue(Intents.dispatchUserActivity("wipe_all", null),
                "the activity type is ours, so it is claimed rather than left to the system");
        assertTrue(d.invoked.isEmpty(), "but the handler must not run");

        Intents.dispatchUserActivity("log_run", null);
        assertEquals(Arrays.asList("log_run"), d.invoked,
                "and an intent the declaration still allows runs as before");
    }

    /// The generated asString stringifies anything non-null and oneOf compares the result, so
    /// refusing a non-String here declined donations that would have reached the handler.
    @Test
    void aStringDonationIsCheckedTheWayTheCoercionReadsIt() {
        FakeBridge b = new FakeBridge();
        Intents.setBridge(b);
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declarationWithStringParam("note_it", "note", null));
        d.declarations.add(declarationWithStringParam("pick_it", "size",
                Arrays.asList("123", "small")));
        Intents.setDispatcher(d);

        Map<String, Object> numeric = new HashMap<String, Object>();
        numeric.put("note", Integer.valueOf(123));
        Intents.donate("note_it", numeric);
        assertEquals("note_it", b.donatedId, "asString would have made this \"123\"");

        b.donatedId = null;
        Map<String, Object> inVocabulary = new HashMap<String, Object>();
        inVocabulary.put("size", Integer.valueOf(123));
        Intents.donate("pick_it", inVocabulary);
        assertEquals("pick_it", b.donatedId, "oneOf compares the stringified value");

        b.donatedId = null;
        Map<String, Object> outside = new HashMap<String, Object>();
        outside.put("size", "enormous");
        Intents.donate("pick_it", outside);
        assertNull(b.donatedId, "a value outside the vocabulary is still refused");
    }

    /// Util.encodeUrl walks UTF-16 code units, so a supplementary character -- an emoji in an
    /// entity id -- is encoded as two three-byte sequences that are not valid UTF-8 at all.
    /// The router decodes them as UTF-8 and gets replacement characters, so the handler ran on
    /// the right id and the screen it opened was built from a corrupted one.
    @Test
    void aRouteValueOutsideTheBasicPlaneSurvivesEncoding() {
        final List<String> navigated = new ArrayList<String>();
        com.codename1.router.Navigation.setDispatcher(new com.codename1.router.RouteDispatcher() {
            public com.codename1.ui.Form dispatch(String url) {
                navigated.add(url);
                return null;
            }
        });
        try {
            FakeDispatcher d = new FakeDispatcher();
            d.declarations.add(new IntentDeclaration("known", "Open", "", false, true, false,
                    "/notes/{id}", 5, Collections.<String>emptyList(),
                    Arrays.asList(new IntentParameterInfo("id", "Which?",
                            IntentParameterType.STRING, true, null, null, null)),
                    Arrays.asList(Exposure.ASSISTANT)));
            d.next = IntentResult.ok();
            Intents.setDispatcher(d);

            Map<String, Object> params = new HashMap<String, Object>();
            params.put("id", "note-\uD83D\uDE00");
            Intents.invoke("known", params);

            assertEquals(1, navigated.size(), "the declared route has to be navigated");
            String url = navigated.get(0);
            // U+1F600 is F0 9F 98 80 in UTF-8. The broken form emits ED A0 BD ED B8 80.
            assertTrue(url.contains("%F0%9F%98%80"),
                    "the surrogate pair has to become one four-byte sequence: " + url);
            assertFalse(url.contains("%ED"),
                    "a lone surrogate encoded on its own is not UTF-8: " + url);
        } finally {
            com.codename1.router.Navigation.setDispatcher(null);
        }
    }

    /// A model reading "integer" may produce 5000000000 for a handler that declared an int --
    /// valid against the schema and rejected by the coercion before it reaches the handler,
    /// which is a failure the model had no way to avoid. The width is on the declaration, so
    /// the schema can say it.
    @Test
    void aToolSchemaBoundsANumberToTheDeclaredWidth() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(modelIntentWithNumeric("count_int", "count",
                IntentParameterType.INTEGER, 32));
        d.declarations.add(modelIntentWithNumeric("count_long", "count",
                IntentParameterType.INTEGER, 64));
        d.declarations.add(modelIntentWithNumeric("ratio_double", "ratio",
                IntentParameterType.NUMBER, 64));
        Intents.setDispatcher(d);

        Map<String, String> schemas = new HashMap<String, String>();
        for (com.codename1.ai.Tool t : Intents.asTools()) {
            schemas.put(t.getName(), t.getParametersJsonSchema());
        }

        assertTrue(schemas.get("count_int").contains("2147483647"),
                "an int says so: " + schemas.get("count_int"));
        assertTrue(schemas.get("count_long").contains("9223372036854775807"),
                "and a long says so too: " + schemas.get("count_long"));
        assertFalse(schemas.get("ratio_double").contains("maximum"),
                "a double is unbounded, which is the honest description of a double: "
                        + schemas.get("ratio_double"));
    }

    /// The deadline has to survive navigation. claim() runs before the route is dispatched,
    /// and dispatchExternalUrl waits on the event dispatch thread -- so a busy or stalled EDT
    /// left the platform with no answer at all while the watchdog had already gone home.
    @Test
    void aStalledNavigationStillAnswersThePlatform() throws Exception {
        final java.util.concurrent.CountDownLatch released =
                new java.util.concurrent.CountDownLatch(1);
        com.codename1.router.Navigation.setDispatcher(new com.codename1.router.RouteDispatcher() {
            public com.codename1.ui.Form dispatch(String url) {
                try {
                    // Stands in for an event dispatch thread that is not answering. Bounded so
                    // a regression cannot hang the suite.
                    released.await(10, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            }
        });
        try {
            // A bridge that can foreground, because a route from a headless handler is only
            // navigated when the app actually comes forward.
            FakeBridge b = new FakeBridge();
            b.canForeground = true;
            Intents.setBridge(b);

            FakeDispatcher d = new FakeDispatcher();
            d.declarations.add(new IntentDeclaration("known", "Known", "", true, true, false,
                    "", 1, Collections.<String>emptyList(),
                    Collections.<IntentParameterInfo>emptyList(),
                    Arrays.asList(Exposure.ASSISTANT)));
            d.next = IntentResult.opens("/orders/42");
            Intents.setDispatcher(d);

            final java.util.concurrent.CountDownLatch answered =
                    new java.util.concurrent.CountDownLatch(1);
            final List<IntentResult> reported = new ArrayList<IntentResult>();
            Thread caller = new Thread(new Runnable() {
                public void run() {
                    Intents.dispatchInvocation("known", null, IntentSource.SHORTCUT, true,
                            new IntentCompletion() {
                                public void onIntentResult(IntentResult r) {
                                    reported.add(r);
                                    answered.countDown();
                                }
                            });
                }
            });
            caller.start();

            assertTrue(answered.await(6, java.util.concurrent.TimeUnit.SECONDS),
                    "the platform must be told even while navigation is stuck");
            assertTrue(reported.get(0).isFailed(),
                    "and told that it took too long, rather than nothing at all");
            released.countDown();
            caller.join(10000);
        } finally {
            com.codename1.router.Navigation.setDispatcher(null);
        }
    }

    /// iOS never lets an app foreground itself and Android can fail or time out, and the
    /// route was built anyway -- so the application changed what it shows next while staying in
    /// the background, and the user finds that screen already open on their next launch, for an
    /// action they were told could not be shown.
    @Test
    void aRouteIsNotBuiltWhenTheAppCannotComeForward() {
        final List<String> navigated = new ArrayList<String>();
        com.codename1.router.Navigation.setDispatcher(new com.codename1.router.RouteDispatcher() {
            public com.codename1.ui.Form dispatch(String url) {
                navigated.add(url);
                return null;
            }
        });
        try {
            FakeBridge b = new FakeBridge();
            b.canForeground = false;
            Intents.setBridge(b);

            FakeDispatcher d = new FakeDispatcher();
            d.declarations.add(declaration("known"));
            d.next = IntentResult.opens("/orders/42");
            Intents.setDispatcher(d);

            Intents.invoke("known", null);

            assertTrue(navigated.isEmpty(),
                    "the destination must not be built where nobody can see it");
            assertEquals(1, b.foregroundRequests, "and it did ask first");

            // When the app can come forward, the same result navigates as before.
            b.canForeground = true;
            Intents.invoke("known", null);
            assertEquals(Arrays.asList("/orders/42"), navigated);
        } finally {
            com.codename1.router.Navigation.setDispatcher(null);
        }
    }

    /// The description recommends epoch milliseconds and IntentDates parses them, so declaring
    /// string alone had a schema-enforcing caller reject a value the schema had just
    /// recommended -- a call the model could not have got right.
    @Test
    void aDateToolSchemaAcceptsBothFormsItParses() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(new IntentDeclaration("remind_me", "Remind", "", true, true, false,
                "", 5, Collections.<String>emptyList(),
                Arrays.asList(new IntentParameterInfo("when", "When?",
                        IntentParameterType.DATE, true, null, null, null)),
                Arrays.asList(Exposure.MODEL)));
        Intents.setDispatcher(d);

        String schema = Intents.asTools().get(0).getParametersJsonSchema();

        assertTrue(schema.contains("\"type\":[\"string\",\"integer\"]"),
                "epoch milliseconds are a number, and the description says to send them: "
                        + schema);
        assertTrue(schema.contains("9223372036854775807"),
                "and bounded to what IntentDates will actually take: " + schema);
    }

    /// The two routes disagreed about the same object: donation reduced an AppEntity to its id
    /// while in-process dispatch handed the entity itself to the generated reader, which
    /// stringified it as "type:id" and asked BY_ID to resolve that. A dynamic intent could fail
    /// through invoke() and the simulator, and work once donated.
    @Test
    void anEntityArgumentReachesTheHandlerAsItsIdEitherWay() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("known"));
        d.next = IntentResult.ok();
        Intents.setDispatcher(d);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("shop", new AppEntity("coffee_shop", "shop-7"));
        Intents.invoke("known", params);

        assertEquals("shop-7", d.lastParams.get("shop"),
                "the handler sees what it would have seen from the platform");
    }

    /// And through a parameterization's bound values, which took the other path entirely.
    @Test
    void aBoundEntityIsReducedTheSameWay() {
        FakeDispatcher d = new FakeDispatcher();
        d.declarations.add(declaration("known"));
        d.next = IntentResult.ok();
        Intents.setDispatcher(d);
        Intents.registerDynamicIntent(new DynamicIntent("usual", "known", "The usual")
                .bind("shop", new AppEntity("coffee_shop", "shop-7")));

        Intents.invoke("usual", null);

        assertEquals("shop-7", d.lastParams.get("shop"));
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
