package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.JSONParser;
import com.codename1.surfaces.SurfaceRasterizer;
import com.codename1.surfaces.SurfaceSerializer;
import com.codename1.surfaces.SurfaceText;
import com.codename1.surfaces.WidgetSize;
import com.codename1.surfaces.WidgetTimeline;

import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/// External-surfaces timeline selection logic on the device VM: the desktop surface renderers
/// (JavaSE simulator, native Windows/Linux widget windows) pick the active entry, the next entry
/// flip and the per-size layout via SurfaceRasterizer's timeline helpers, operating on the parsed
/// wire document. Runs the same fixed timeline through serialize -> parse -> helpers and asserts
/// the selection rules at probe times around each entry. Assertion-only test, no screenshot.
public class SurfacesTimelineLogicTest extends BaseTest {
    private static final long T1 = 1750000001000L;
    private static final long T2 = 1750000002000L;
    private static final long T3 = 1750000003000L;

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        try {
            Map<String, Object> doc = new JSONParser().parseJSON(
                    new StringReader(buildTimelineJson()));

            // currentEntry: the entry whose date most recently passed; before every entry the
            // FIRST entry is returned rather than null
            assertEqual(T1, entryDate(SurfaceRasterizer.currentEntry(doc, T1 - 500)),
                    "before all entries the first entry is current");
            assertEqual(T1, entryDate(SurfaceRasterizer.currentEntry(doc, T1)),
                    "an entry becomes current exactly at its date");
            assertEqual(T1, entryDate(SurfaceRasterizer.currentEntry(doc, T2 - 1)),
                    "still the first entry just before the second");
            assertEqual(T2, entryDate(SurfaceRasterizer.currentEntry(doc, T2 + 500)),
                    "second entry current between second and third");
            assertEqual(T3, entryDate(SurfaceRasterizer.currentEntry(doc, T3 + 3600000L)),
                    "last entry stays current after the timeline ends");

            // the current entry exposes its state map
            Map<String, Object> midEntry = SurfaceRasterizer.currentEntry(doc, T2 + 500);
            Map<String, Object> midState = castMap(midEntry.get("state"));
            assertEqual("two", (String) midState.get("step"), "current entry state map");

            // nextEntryFlip: the nearest future entry date, 0 when none is left
            assertEqual(T1, SurfaceRasterizer.nextEntryFlip(doc, T1 - 500),
                    "first flip is the first entry");
            assertEqual(T2, SurfaceRasterizer.nextEntryFlip(doc, T1),
                    "at an entry date the flip is the NEXT entry");
            assertEqual(T3, SurfaceRasterizer.nextEntryFlip(doc, T2 + 1),
                    "flip after the second entry is the third");
            assertEqual(0L, SurfaceRasterizer.nextEntryFlip(doc, T3),
                    "no flip after the last entry");

            // layoutForSize: explicit override wins, everything else falls back to default,
            // and an unknown/null size name is the default too
            Map<String, Object> smallLayout = SurfaceRasterizer.layoutForSize(doc, "small");
            assertEqual("Small", (String) smallLayout.get("text"), "small returns the override");
            Map<String, Object> mediumLayout = SurfaceRasterizer.layoutForSize(doc, "medium");
            assertEqual("Default", (String) mediumLayout.get("text"),
                    "medium falls back to default");
            Map<String, Object> largeLayout = SurfaceRasterizer.layoutForSize(doc, "large");
            assertEqual("Default", (String) largeLayout.get("text"),
                    "large falls back to default");
            Map<String, Object> nullLayout = SurfaceRasterizer.layoutForSize(doc, null);
            assertEqual("Default", (String) nullLayout.get("text"),
                    "null size name falls back to default");
            Map<String, Object> bogusLayout = SurfaceRasterizer.layoutForSize(doc, "bogus");
            assertEqual("Default", (String) bogusLayout.get("text"),
                    "unknown size name falls back to default");
            assertBool(SurfaceRasterizer.layoutForSize(null, "small") == null,
                    "null document yields null layout");

            // degenerate documents
            Map<String, Object> empty = new HashMap<String, Object>();
            assertBool(SurfaceRasterizer.currentEntry(empty, T1) == null,
                    "entry-less document has no current entry");
            assertEqual(0L, SurfaceRasterizer.nextEntryFlip(empty, T1),
                    "entry-less document never flips");
        } catch (Throwable t) {
            fail("Surfaces timeline logic failed: " + t);
            return false;
        }
        done();
        return true;
    }

    private String buildTimelineJson() {
        Map<String, Object> s1 = new HashMap<String, Object>();
        s1.put("step", "one");
        Map<String, Object> s2 = new HashMap<String, Object>();
        s2.put("step", "two");
        Map<String, Object> s3 = new HashMap<String, Object>();
        s3.put("step", "three");
        WidgetTimeline timeline = new WidgetTimeline()
                .setContent(new SurfaceText("Default"))
                .setContent(WidgetSize.SMALL, new SurfaceText("Small"))
                .addEntry(new Date(T1), s1)
                .addEntry(new Date(T2), s2)
                .addEntry(new Date(T3), s3);
        return SurfaceSerializer.serializeTimeline("cn1ss_status", timeline,
                new LinkedHashMap<String, byte[]>());
    }

    private static long entryDate(Map<String, Object> entry) {
        if (entry == null) {
            throw new IllegalStateException("expected a timeline entry but got null");
        }
        Object date = entry.get("date");
        if (date instanceof Number) {
            return ((Number) date).longValue();
        }
        throw new IllegalStateException("entry date is not a number: " + date);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object o) {
        if (o instanceof Map) {
            return (Map<String, Object>) o;
        }
        throw new IllegalStateException("expected a map but got " + o);
    }
}
