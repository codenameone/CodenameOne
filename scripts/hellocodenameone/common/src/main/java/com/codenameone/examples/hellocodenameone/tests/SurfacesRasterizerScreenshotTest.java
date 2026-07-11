package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.JSONParser;
import com.codename1.surfaces.SurfaceAlignment;
import com.codename1.surfaces.SurfaceBox;
import com.codename1.surfaces.SurfaceColor;
import com.codename1.surfaces.SurfaceColumn;
import com.codename1.surfaces.SurfaceDynamicText;
import com.codename1.surfaces.SurfaceFontWeight;
import com.codename1.surfaces.SurfaceProgress;
import com.codename1.surfaces.SurfaceRasterizer;
import com.codename1.surfaces.SurfaceRow;
import com.codename1.surfaces.SurfaceSerializer;
import com.codename1.surfaces.SurfaceSpacer;
import com.codename1.surfaces.SurfaceText;
import com.codename1.surfaces.WidgetTimeline;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.layouts.BorderLayout;

import java.io.StringReader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Renders a deterministic widget descriptor through the shared SurfaceRasterizer (the renderer
/// behind the JavaSE simulator preview and the native Windows/Linux desktop widgets) and
/// screenshots the result, so the whole pipeline -- node builders -> SurfaceSerializer wire JSON
/// -> JSONParser -> rasterized pixels -- has a per-platform visual baseline.
///
/// Determinism: the "now" clock is pinned so every dynamic text formats a constant string, and
/// only the pure-time-difference styles (timerDown/timerUp/relative) are used -- the date/time
/// styles format through Calendar.getInstance() and would depend on the runner's timezone. All
/// colors are explicit 0xFFxxxxxx values (the rasterizer's role colors are hardcoded too, but
/// explicit colors keep the intent obvious). No image nodes: PNG decode fidelity differs per
/// platform and the byte round-trip is covered by SurfacesSerializerRoundTripTest. Text renders
/// with the platform's native: fonts, which differ per OS -- absorbed by the per-platform golden
/// sets like every other text-bearing baseline in the suite.
///
/// The component re-rasterizes on EVERY paint (the descriptor is parsed once) so the captured
/// frame always reflects settled fonts -- the same guard the graphics tests use against the
/// async native-font load flake. Light and dark renders are shown together with the action hit
/// rectangles outlined.
public class SurfacesRasterizerScreenshotTest extends BaseTest {
    private static final String NAME = "SurfacesRasterizer";
    private static final int TILE = 316;
    private static final int GAP = 12;
    /// Pinned clock: 2025-06-15T15:06:40Z. Only ever used in differences, never formatted
    /// through a timezone.
    private static final long NOW = 1750000000000L;

    private Map<String, Object> layout;
    private Map<String, Object> state;

    @Override
    public boolean runTest() {
        try {
            prepareDescriptor();
            // sanity-check the Result contract once before capturing so a rasterizer
            // regression fails loudly instead of shipping a subtly wrong baseline
            SurfaceRasterizer.Result probe = SurfaceRasterizer.rasterize(
                    layout, state, null, TILE, TILE, false, NOW);
            assertEqual(TILE, probe.getImage().getWidth(), "rasterized image width");
            assertEqual(TILE, probe.getImage().getHeight(), "rasterized image height");
            assertEqual(TILE * TILE, probe.getArgb().length, "argb pixel count");
            assertEqual(1, probe.getActions().size(), "action hit rect count");
            SurfaceRasterizer.ActionRect hit = probe.getActions().get(0);
            assertEqual("open", hit.getActionId(), "action hit rect id");
            assertBool(hit.getWidth() > 0 && hit.getHeight() > 0, "action hit rect has area");
            assertEqual(7L, ((Number) hit.getParams().get("orderId")).longValue(),
                    "action hit rect params");
            // a countdown is visible, so a re-render is due one second after "now"
            assertEqual(NOW + 1000, probe.getNextTickMillis(), "next re-render tick");
        } catch (Throwable t) {
            fail("Surfaces rasterizer setup failed: " + t);
            return false;
        }

        Form form = createForm(NAME, new BorderLayout(), NAME);
        form.add(BorderLayout.CENTER, new RasterizerView());
        form.show();
        return true;
    }

    /// Builds the fixed descriptor with the real node API, ships it through the serializer and
    /// parses it back -- the pixels on screen come from the same wire JSON a platform renderer
    /// would consume, not from an in-memory shortcut.
    private void prepareDescriptor() throws Exception {
        SurfaceColumn root = new SurfaceColumn().setSpacing(6);
        root.setPadding(12);
        root.setBackground(SurfaceColor.rgb(0xfff2f5f8, 0xff101820));
        root.setCornerRadius(20);
        root.add(new SurfaceText("CN1 Surfaces")
                        .setFontSize(18)
                        .setFontWeight(SurfaceFontWeight.SEMIBOLD)
                        .setColor(SurfaceColor.rgb(0xff16324f, 0xffdce6f0))
                        .setAlignment(SurfaceAlignment.LEADING))
                .add(new SurfaceRow()
                        .setSpacing(6)
                        .add(new SurfaceText("ETA")
                                .setFontSize(12)
                                .setColor(SurfaceColor.rgb(0xff5a6b7c, 0xff9aa8b6)))
                        .add(new SurfaceDynamicText(SurfaceDynamicText.STYLE_TIMER_DOWN, "eta")
                                .setFontSize(16)
                                .setFontWeight(SurfaceFontWeight.BOLD)
                                .setColor(SurfaceColor.rgb(0xffb03030, 0xffffb4a0)))
                        .add(new SurfaceSpacer())
                        .add(new SurfaceDynamicText(SurfaceDynamicText.STYLE_TIMER_UP,
                                new Date(NOW - 90000L))
                                .setFontSize(12)
                                .setColor(SurfaceColor.rgb(0xff3a3a3a, 0xffc0c0c0))))
                .add(new SurfaceProgress(SurfaceProgress.STYLE_LINEAR)
                        .setValueState("progress")
                        .setColor(SurfaceColor.rgb(0xff2e7d32, 0xff81c784)))
                .add(new SurfaceRow()
                        .setSpacing(8)
                        .add(new SurfaceProgress(SurfaceProgress.STYLE_CIRCULAR)
                                // deterministic: (NOW - start) / (end - start) = 0.3 at the
                                // pinned clock
                                .setDateInterval(new Date(NOW - 30000L), new Date(NOW + 70000L))
                                .setColor(SurfaceColor.rgb(0xffe65100, 0xffffb74d)))
                        .add(new SurfaceText("${status}")
                                .setFontSize(12)
                                .setColor(SurfaceColor.rgb(0xff1c1c1e, 0xffe8e8e8)))
                        .add(new SurfaceSpacer())
                        .add(new SurfaceDynamicText(SurfaceDynamicText.STYLE_RELATIVE,
                                new Date(NOW + 45 * 60000L))
                                .setFontSize(12)
                                .setColor(SurfaceColor.rgb(0xff5a6b7c, 0xff9aa8b6))))
                .add(new SurfaceBox()
                        .add(new SurfaceText("Open order")
                                .setFontSize(12)
                                .setFontWeight(SurfaceFontWeight.SEMIBOLD)
                                .setColor(SurfaceColor.rgb(0xffffffff)))
                        .setBackground(SurfaceColor.rgb(0xff4a90d9, 0xff2a5a89))
                        .setCornerRadius(10)
                        .setSize(0, 36)
                        .setAction("open", actionParams()));

        Map<String, Object> published = new HashMap<String, Object>();
        published.put("status", "On the way");
        published.put("eta", Long.valueOf(NOW + 754000L)); // timerDown renders 12:34
        published.put("progress", Double.valueOf(0.62));
        String json = SurfaceSerializer.serializeTimeline("cn1ss_status",
                new WidgetTimeline()
                        .setContent(root)
                        .addEntry(new Date(NOW - 1000L), published),
                new HashMap<String, byte[]>());

        Map<String, Object> doc = new JSONParser().parseJSON(new StringReader(json));
        layout = SurfaceRasterizer.layoutForSize(doc, "small");
        Map<String, Object> entry = SurfaceRasterizer.currentEntry(doc, NOW);
        state = castMap(entry.get("state"));
    }

    private Map<String, Object> actionParams() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("orderId", Integer.valueOf(7));
        return params;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object o) {
        return (Map<String, Object>) o;
    }

    /// Paints the light and dark rasterizations (side by side when the space allows, stacked on
    /// narrow screens) over a neutral backdrop, outlining every action hit rectangle. Follows
    /// the clean-paint discipline of the graphics tests: state saved/restored, fresh render per
    /// paint.
    private final class RasterizerView extends Component {
        RasterizerView() {
            setUIID("GraphicsComponent");
        }

        @Override
        public void paint(Graphics g) {
            int alpha = g.getAlpha();
            int color = g.getColor();
            Font font = g.getFont();
            g.pushClip();
            try {
                g.setAlpha(255);
                g.setColor(0x606060);
                g.fillRect(getX(), getY(), getWidth(), getHeight());
                boolean sideBySide = getWidth() >= TILE * 2 + GAP * 3;
                int lightX = getX() + GAP;
                int lightY = getY() + GAP;
                int darkX = sideBySide ? lightX + TILE + GAP : lightX;
                int darkY = sideBySide ? lightY : lightY + TILE + GAP;
                drawTile(g, false, lightX, lightY);
                drawTile(g, true, darkX, darkY);
            } finally {
                g.popClip();
                g.setFont(font);
                g.setColor(color);
                g.setAlpha(alpha);
            }
        }

        private void drawTile(Graphics g, boolean dark, int x, int y) {
            SurfaceRasterizer.Result result = SurfaceRasterizer.rasterize(
                    layout, state, null, TILE, TILE, dark, NOW);
            g.drawImage(result.getImage(), x, y);
            List<SurfaceRasterizer.ActionRect> actions = result.getActions();
            g.setAlpha(255);
            g.setColor(0xff00ff);
            for (SurfaceRasterizer.ActionRect a : actions) {
                g.drawRect(x + a.getX(), y + a.getY(), a.getWidth() - 1, a.getHeight() - 1);
            }
        }
    }
}
