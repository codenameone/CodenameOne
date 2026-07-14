package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.JSONParser;
import com.codename1.surfaces.SurfaceAlignment;
import com.codename1.surfaces.SurfaceBox;
import com.codename1.surfaces.SurfaceColor;
import com.codename1.surfaces.SurfaceColumn;
import com.codename1.surfaces.SurfaceDynamicText;
import com.codename1.surfaces.SurfaceFontWeight;
import com.codename1.surfaces.SurfaceImage;
import com.codename1.surfaces.SurfaceProgress;
import com.codename1.surfaces.SurfaceRow;
import com.codename1.surfaces.SurfaceSerializer;
import com.codename1.surfaces.SurfaceSpacer;
import com.codename1.surfaces.SurfaceText;
import com.codename1.surfaces.WidgetSize;
import com.codename1.surfaces.WidgetTimeline;
import com.codename1.ui.EncodedImage;

import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// External-surfaces wire format on the device VM: builds a descriptor tree that touches every
/// node type (column/row/box/text/dynamic text with a state date key/progress with a state value
/// key/image bytes/spacer), serializes it with SurfaceSerializer and parses the JSON back with
/// JSONParser, asserting the parsed structure matches what was built. The same serializer output
/// is consumed by the iOS WidgetKit extension, the Android RemoteViews renderer and the desktop
/// rasterizers, so a translation bug here (ParparVM string/number handling, the JS port's JSON
/// bridge, the Windows/Linux native VMs) silently corrupts every widget. Assertion-only test,
/// no screenshot.
public class SurfacesSerializerRoundTripTest extends BaseTest {
    private static final long ENTRY_EARLY = 1750000000000L;
    private static final long ENTRY_LATE = 1750000600000L;

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }

    @Override
    public boolean runTest() {
        try {
            byte[] pngBytes = fakePngBytes();
            Map<String, byte[]> images = new LinkedHashMap<String, byte[]>();
            String json = SurfaceSerializer.serializeTimeline("cn1ss_status",
                    buildTimeline(pngBytes), images);

            Map<String, Object> doc = new JSONParser().parseJSON(new StringReader(json));

            // document envelope
            assertEqual(1L, asLong(doc.get("v")), "wire version");
            assertEqual("cn1ss_status", (String) doc.get("kind"), "kind id");
            assertEqual("never", (String) doc.get("reload"), "reload policy");

            // layouts: default + the two explicit per-size overrides, nothing else
            Map<String, Object> layouts = asMap(doc.get("layouts"), "layouts");
            assertEqual(3, layouts.size(), "layout count (default + small + lockscreen)");
            checkDefaultLayout(asMap(layouts.get("default"), "default layout"), images, pngBytes);
            Map<String, Object> small = asMap(layouts.get("small"), "small layout");
            assertEqual("text", (String) small.get("t"), "small override type");
            assertEqual("Small", (String) small.get("text"), "small override text");
            Map<String, Object> lock = asMap(layouts.get("lockscreen"), "lockscreen layout");
            assertEqual("text", (String) lock.get("t"), "lockscreen override type");
            assertEqual("Lock", (String) lock.get("text"), "lockscreen override text");

            // entries were added out of order and must come back sorted ascending by date,
            // with state maps intact (string + number values)
            List<Object> entries = asList(doc.get("entries"), "entries");
            assertEqual(2, entries.size(), "entry count");
            Map<String, Object> first = asMap(entries.get(0), "entry[0]");
            Map<String, Object> second = asMap(entries.get(1), "entry[1]");
            assertEqual(ENTRY_EARLY, asLong(first.get("date")), "entry[0] date (sorted)");
            assertEqual(ENTRY_LATE, asLong(second.get("date")), "entry[1] date (sorted)");
            Map<String, Object> firstState = asMap(first.get("state"), "entry[0] state");
            assertEqual("Packing", (String) firstState.get("status"), "entry[0] status");
            assertEqual(ENTRY_LATE, asLong(firstState.get("eta")), "entry[0] eta millis");
            assertBool(Math.abs(asDouble(firstState.get("progress")) - 0.25) < 0.0001,
                    "entry[0] progress fraction");
            Map<String, Object> secondState = asMap(second.get("state"), "entry[1] state");
            assertEqual("Delivered", (String) secondState.get("status"), "entry[1] status");

            // the images list must name the registered blob, and the blob must be byte-exact
            assertEqual(1, images.size(), "registered image blob count");
            String imageName = images.keySet().iterator().next();
            byte[] blob = images.get(imageName);
            assertEqual(pngBytes.length, blob.length, "image blob length");
            for (int i = 0; i < pngBytes.length; i++) {
                if (pngBytes[i] != blob[i]) {
                    fail("image blob byte " + i + " differs after registration");
                    return false;
                }
            }
            List<Object> imageNames = asList(doc.get("images"), "images list");
            assertEqual(1, imageNames.size(), "referenced image name count");
            assertEqual(imageName, (String) imageNames.get(0), "referenced image name");
        } catch (Throwable t) {
            fail("Surfaces serializer round trip failed: " + t);
            return false;
        }
        done();
        return true;
    }

    private WidgetTimeline buildTimeline(byte[] pngBytes) {
        SurfaceColumn root = new SurfaceColumn()
                .setSpacing(4);
        root.setPadding(2, 3, 4, 5);
        root.setBackground(SurfaceColor.rgb(0xff112233, 0xff445566));
        root.setCornerRadius(8);
        root.add(new SurfaceText("Order ${item}")
                        .setFontSize(16)
                        .setFontWeight(SurfaceFontWeight.SEMIBOLD)
                        .setColor(SurfaceColor.rgb(0xffff0000))
                        .setMaxLines(2)
                        .setAlignment(SurfaceAlignment.LEADING))
                .add(new SurfaceDynamicText(SurfaceDynamicText.STYLE_TIMER_DOWN, "eta")
                        .setFontSize(20))
                .add(new SurfaceDynamicText(SurfaceDynamicText.STYLE_DATE, new Date(ENTRY_EARLY)))
                .add(new SurfaceRow()
                        .setSpacing(2)
                        .add(new SurfaceProgress(SurfaceProgress.STYLE_LINEAR)
                                .setValueState("progress")
                                .setColor(SurfaceColor.ACCENT))
                        .add(new SurfaceSpacer(6))
                        .add(new SurfaceProgress(SurfaceProgress.STYLE_CIRCULAR)
                                .setValue(0.25f)))
                .add(new SurfaceImage(EncodedImage.create(pngBytes))
                        .setScaleMode(SurfaceImage.SCALE_FILL))
                .add(new SurfaceBox()
                        .add(new SurfaceText("Tap")
                                .setAlignment(SurfaceAlignment.BOTTOM_TRAILING))
                        .setAction("open_box", actionParams()));

        Map<String, Object> earlyState = new HashMap<String, Object>();
        earlyState.put("status", "Packing");
        earlyState.put("eta", Long.valueOf(ENTRY_LATE));
        earlyState.put("progress", Double.valueOf(0.25));
        Map<String, Object> lateState = new HashMap<String, Object>();
        lateState.put("status", "Delivered");

        return new WidgetTimeline()
                .setContent(root)
                .setContent(WidgetSize.SMALL, new SurfaceText("Small"))
                .setContent(WidgetSize.LOCKSCREEN, new SurfaceText("Lock"))
                // deliberately added out of order; the serializer must sort ascending
                .addEntry(new Date(ENTRY_LATE), lateState)
                .addEntry(new Date(ENTRY_EARLY), earlyState)
                .setReloadPolicy(WidgetTimeline.RELOAD_NEVER);
    }

    private Map<String, Object> actionParams() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("orderId", Integer.valueOf(42));
        params.put("label", "box");
        return params;
    }

    private void checkDefaultLayout(Map<String, Object> root, Map<String, byte[]> images,
            byte[] pngBytes) {
        assertEqual("col", (String) root.get("t"), "root type");
        assertEqual(4L, asLong(root.get("spacing")), "root spacing");
        assertEqual(8L, asLong(root.get("corner")), "root corner radius");
        List<Object> pad = asList(root.get("pad"), "root pad");
        assertEqual(2L, asLong(pad.get(0)), "pad top");
        assertEqual(3L, asLong(pad.get(1)), "pad right");
        assertEqual(4L, asLong(pad.get(2)), "pad bottom");
        assertEqual(5L, asLong(pad.get(3)), "pad left");
        Map<String, Object> bg = asMap(root.get("bg"), "root bg");
        assertEqual(0xff112233L, asLong(bg.get("l")) & 0xffffffffL, "bg light argb");
        assertEqual(0xff445566L, asLong(bg.get("d")) & 0xffffffffL, "bg dark argb");

        List<Object> children = asList(root.get("ch"), "root children");
        assertEqual(6, children.size(), "root child count");

        // text node: placeholder text survives verbatim, styling keys present
        Map<String, Object> text = asMap(children.get(0), "text node");
        assertEqual("text", (String) text.get("t"), "text type");
        assertEqual("Order ${item}", (String) text.get("text"), "text placeholder");
        assertEqual(16L, asLong(text.get("size")), "text size");
        assertEqual("semibold", (String) text.get("fw"), "text weight");
        assertEqual("leading", (String) text.get("align"), "text alignment");
        assertEqual(2L, asLong(text.get("maxLines")), "text maxLines");
        Map<String, Object> textColor = asMap(text.get("color"), "text color");
        assertEqual(0xffff0000L, asLong(textColor.get("l")) & 0xffffffffL, "text color light");

        // dynamic text with a state date key
        Map<String, Object> dynKey = asMap(children.get(1), "dyn dateKey node");
        assertEqual("dyn", (String) dynKey.get("t"), "dyn type");
        assertEqual("timerDown", (String) dynKey.get("style"), "dyn style");
        assertEqual("eta", (String) dynKey.get("dateKey"), "dyn dateKey");
        assertBool(dynKey.get("date") == null, "dateKey node has no fixed date");
        assertEqual(20L, asLong(dynKey.get("size")), "dyn size");

        // dynamic text with a fixed date
        Map<String, Object> dynDate = asMap(children.get(2), "dyn date node");
        assertEqual("date", (String) dynDate.get("style"), "dyn date style");
        assertEqual(ENTRY_EARLY, asLong(dynDate.get("date")), "dyn fixed date millis");
        assertBool(dynDate.get("dateKey") == null, "fixed-date node has no dateKey");

        // row with the two progress variants and a spacer
        Map<String, Object> row = asMap(children.get(3), "row node");
        assertEqual("row", (String) row.get("t"), "row type");
        assertEqual(2L, asLong(row.get("spacing")), "row spacing");
        List<Object> rowChildren = asList(row.get("ch"), "row children");
        assertEqual(3, rowChildren.size(), "row child count");
        Map<String, Object> linear = asMap(rowChildren.get(0), "linear progress");
        assertEqual("prog", (String) linear.get("t"), "linear progress type");
        assertEqual("linear", (String) linear.get("style"), "linear progress style");
        assertEqual("progress", (String) linear.get("valueKey"), "linear progress valueKey");
        Map<String, Object> progColor = asMap(linear.get("color"), "progress color");
        assertEqual("accent", (String) progColor.get("role"), "progress color role");
        Map<String, Object> spacer = asMap(rowChildren.get(1), "spacer");
        assertEqual("spacer", (String) spacer.get("t"), "spacer type");
        assertEqual(6L, asLong(spacer.get("min")), "spacer min");
        Map<String, Object> circular = asMap(rowChildren.get(2), "circular progress");
        assertEqual("circular", (String) circular.get("style"), "circular progress style");
        assertBool(Math.abs(asDouble(circular.get("value")) - 0.25) < 0.0001,
                "circular progress value");

        // image node references the blob registered by content hash
        Map<String, Object> img = asMap(children.get(4), "image node");
        assertEqual("img", (String) img.get("t"), "image type");
        assertEqual("fill", (String) img.get("scale"), "image scale mode");
        String imageName = (String) img.get("name");
        assertBool(imageName != null && imageName.length() > 0, "image wire name present");
        assertBool(images.containsKey(imageName), "image blob registered under wire name");
        assertEqual(pngBytes.length, images.get(imageName).length, "image blob bytes");

        // box with an action carrying sorted params
        Map<String, Object> box = asMap(children.get(5), "box node");
        assertEqual("box", (String) box.get("t"), "box type");
        Map<String, Object> action = asMap(box.get("action"), "box action");
        assertEqual("open_box", (String) action.get("id"), "action id");
        Map<String, Object> params = asMap(action.get("p"), "action params");
        assertEqual(42L, asLong(params.get("orderId")), "action param number");
        assertEqual("box", (String) params.get("label"), "action param string");
        List<Object> boxChildren = asList(box.get("ch"), "box children");
        Map<String, Object> boxText = asMap(boxChildren.get(0), "box child");
        assertEqual("bottomTrailing", (String) boxText.get("align"), "box child alignment");
    }

    /// A tiny fake PNG payload: EncodedImage wraps bytes lazily and the serializer ships them
    /// verbatim (content-hash named) without ever decoding, so any bytes exercise the path.
    private static byte[] fakePngBytes() {
        byte[] b = new byte[48];
        b[0] = (byte) 0x89;
        b[1] = 'P';
        b[2] = 'N';
        b[3] = 'G';
        for (int i = 4; i < b.length; i++) {
            b[i] = (byte) (i * 7);
        }
        return b;
    }

    // --- JSON round-trip helpers: JSONParser yields Double or Long for numbers ---

    private static long asLong(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        throw new IllegalStateException("expected a number but got " + o);
    }

    private static double asDouble(Object o) {
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        throw new IllegalStateException("expected a number but got " + o);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o, String what) {
        if (o instanceof Map) {
            return (Map<String, Object>) o;
        }
        throw new IllegalStateException(what + " is not a map: " + o);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object o, String what) {
        if (o instanceof List) {
            return (List<Object>) o;
        }
        throw new IllegalStateException(what + " is not a list: " + o);
    }
}
