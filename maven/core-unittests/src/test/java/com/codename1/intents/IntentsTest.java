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
    }

    /** A dispatcher standing in for the build-time generated one. */
    private static final class FakeDispatcher implements IntentDispatcher {
        final List<String> invoked = new ArrayList<String>();
        Map<String, Object> lastParams = Collections.emptyMap();
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

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("minutes", Integer.valueOf(20));
        params.put("kind", "run");
        Intents.donate("log_workout", params);

        assertEquals("log_workout", b.donatedId);
        Map parsed = parse(b.donatedParams);
        assertEquals("run", parsed.get("kind"));
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
