package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.surfaces.SurfaceActionEvent;
import com.codename1.surfaces.SurfaceActionHandler;
import com.codename1.surfaces.Surfaces;
import com.codename1.ui.CN;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// External-surfaces action routing on the real EDT: actions dispatched BEFORE a handler is
/// registered (the widget tap that cold-started the app) must queue, then flush to the handler
/// in arrival order flagged cold start; actions dispatched with a live handler must arrive on
/// the EDT without the flag, params delivered verbatim. This is the exact path the platform
/// ports (widget trampoline activity on Android, deep link on iOS, window click on desktop)
/// drive via Surfaces.dispatchAction. Assertion-only test, no screenshot.
public class SurfacesActionDispatchTest extends BaseTest {

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        // Drain anything an earlier platform event might have queued so the ordering
        // assertions below start from a clean slate; clearing the handler afterwards
        // re-arms the cold-start queue.
        Surfaces.setActionHandler(new SurfaceActionHandler() {
            @Override
            public void onSurfaceAction(SurfaceActionEvent evt) {
            }
        });
        Surfaces.setActionHandler(null);

        Map<String, Object> firstParams = new HashMap<String, Object>();
        firstParams.put("orderId", Integer.valueOf(7));
        firstParams.put("label", "first");
        firstParams.put("urgent", Boolean.TRUE);
        Map<String, Object> warmParams = new HashMap<String, Object>();
        warmParams.put("label", "warm");

        // no handler yet: both queue as cold-start events
        Surfaces.dispatchAction("cn1ss_status", "cold_one", firstParams);
        Surfaces.dispatchAction("cn1ss_status", "cold_two", null);

        final List<SurfaceActionEvent> received = new ArrayList<SurfaceActionEvent>();
        final List<Boolean> onEdt = new ArrayList<Boolean>();
        Surfaces.setActionHandler(new SurfaceActionHandler() {
            @Override
            public void onSurfaceAction(SurfaceActionEvent evt) {
                received.add(evt);
                onEdt.add(Boolean.valueOf(CN.isEdt()));
            }
        });

        // live handler: delivered (still via the EDT queue) without the cold-start flag
        Surfaces.dispatchAction("cn1ss_live", "warm_one", warmParams);

        // every delivery above rides Display.callSerially, so a serial call issued after
        // them observes the fully flushed sequence
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                verify(received, onEdt);
            }
        });
        return true;
    }

    private void verify(List<SurfaceActionEvent> received, List<Boolean> onEdt) {
        try {
            assertEqual(3, received.size(), "delivered action count");

            SurfaceActionEvent first = received.get(0);
            assertEqual("cold_one", first.getActionId(), "queued actions flush in arrival order");
            assertEqual("cn1ss_status", first.getSource(), "first action source");
            assertBool(first.isColdStart(), "pre-handler action is flagged cold start");
            assertEqual(7L, ((Number) first.getParams().get("orderId")).longValue(),
                    "number param delivered verbatim");
            assertEqual("first", (String) first.getParams().get("label"),
                    "string param delivered verbatim");
            assertBool(Boolean.TRUE.equals(first.getParams().get("urgent")),
                    "boolean param delivered verbatim");

            SurfaceActionEvent second = received.get(1);
            assertEqual("cold_two", second.getActionId(), "second queued action follows");
            assertBool(second.isColdStart(), "second pre-handler action is cold start too");
            assertNotNull(second.getParams(), "null params arrive as an empty map");
            assertEqual(0, second.getParams().size(), "null params arrive empty");

            SurfaceActionEvent third = received.get(2);
            assertEqual("warm_one", third.getActionId(), "live-handler action delivered");
            assertEqual("cn1ss_live", third.getSource(), "live-handler action source");
            assertBool(!third.isColdStart(), "live-handler action is not cold start");
            assertEqual("warm", (String) third.getParams().get("label"),
                    "live-handler params delivered verbatim");

            for (Boolean edt : onEdt) {
                assertBool(Boolean.TRUE.equals(edt), "handler runs on the EDT");
            }
        } catch (Throwable t) {
            fail("Surfaces action dispatch failed: " + t);
            return;
        } finally {
            Surfaces.setActionHandler(null);
        }
        done();
    }
}
