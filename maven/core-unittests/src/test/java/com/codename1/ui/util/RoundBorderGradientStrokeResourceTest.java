package com.codename1.ui.util;

import com.codename1.junit.UITestBase;
import com.codename1.ui.plaf.RoundBorder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Round-trips a gradient-stroke {@link RoundBorder} through the resource writer
 * ({@link EditableResources#save}) and reader ({@link Resources}) to guard the
 * three fields added in resource format 1.15 (strokeGradient / strokeColor2 /
 * strokeGradientAngle). The iOS Liquid Glass theme uses this path for button
 * rims, so a broken read/write pairing would corrupt a shipped native theme.
 */
public class RoundBorderGradientStrokeResourceTest extends UITestBase {

    @Test
    public void gradientStrokeRoundBorderSurvivesResSaveLoad() throws Exception {
        RoundBorder border = RoundBorder.create()
                .color(0x123456).opacity(200)
                .stroke(2f, true).strokeColor(0xff0000).strokeOpacity(255)
                .strokeColor2(0x00ff00).strokeGradientAngle(45f);
        // Supplying a second stroke colour implicitly flags the border as a gradient.
        assertTrue(border.isStrokeGradient(), "second stroke colour marks the border as a gradient");

        RoundBorder loaded = saveAndReload(border);
        assertTrue(loaded.isStrokeGradient(), "gradient flag survived the round-trip");
        assertEquals(0x00ff00, loaded.getStrokeColor2(), "second stroke colour survived");
        assertEquals(45f, loaded.getStrokeGradientAngle(), 0.001f, "gradient angle survived");
        // The pre-existing solid-stroke fields must be untouched by the new ones.
        assertEquals(0xff0000, loaded.getStrokeColor(), "solid stroke colour survived");
    }

    @Test
    public void solidStrokeRoundBorderStaysNonGradient() throws Exception {
        RoundBorder border = RoundBorder.create()
                .color(0x0a0b0c).opacity(255)
                .stroke(1f, true).strokeColor(0x112233).strokeOpacity(200);
        assertFalse(border.isStrokeGradient(), "a border with no second colour is not a gradient");

        RoundBorder loaded = saveAndReload(border);
        assertFalse(loaded.isStrokeGradient(), "non-gradient flag survived the round-trip");
    }

    private static RoundBorder saveAndReload(RoundBorder border) throws Exception {
        Hashtable<String, Object> theme = new Hashtable<String, Object>();
        theme.put("GradButton.border", border);

        EditableResources editable = new EditableResources();
        editable.setTheme("t", theme);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        editable.save(out);

        Resources loaded = new Resources(new ByteArrayInputStream(out.toByteArray()), -1);
        Object roundTripped = loaded.getTheme("t").get("GradButton.border");
        assertInstanceOf(RoundBorder.class, roundTripped, "border round-trips as a RoundBorder");
        return (RoundBorder) roundTripped;
    }
}
