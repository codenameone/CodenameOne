package com.codename1.ui.util;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Font;
import com.codename1.ui.Image;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

class ResourcesManualPopulationTest extends UITestBase {

    @FormTest
    void manualResourceSetupExposesTypesAndNames() {
        Resources res = new Resources();
        Image img = Image.createImage(3, 3);
        Font font = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
        Hashtable<String, Hashtable<String, String>> localeToBundle = new Hashtable<String, Hashtable<String, String>>();
        Hashtable<String, String> bundle = new Hashtable<String, String>();
        bundle.put("hi", "there");
        localeToBundle.put("en", bundle);

        res.setResource("img", Resources.MAGIC_IMAGE, img);
        res.setResource("font", Resources.MAGIC_FONT, font);
        res.setResource("data", Resources.MAGIC_DATA, new byte[]{1, 2, 3});
        res.setResource("l10n", Resources.MAGIC_L10N, localeToBundle);

        Resources.setGlobalResources(res);
        assertSame(res, Resources.getGlobalResources());

        assertTrue(res.isImage("img"));
        assertFalse(res.isImage("data"));
        assertArrayEquals(new String[]{"img"}, res.getImageResourceNames());
        assertArrayEquals(new String[]{"font"}, res.getFontResourceNames());
        assertArrayEquals(new String[]{"data"}, res.getDataResourceNames());
        assertArrayEquals(new String[]{"l10n"}, res.getL10NResourceNames());

        assertSame(img, res.getImage("img"));
        byte[] expected = new byte[]{1, 2, 3};
        byte[] actual = new byte[expected.length];
        try {
            res.getData("data").read(actual);
        } catch (Exception ex) {
            fail(ex);
        }
        assertArrayEquals(expected, actual);
        assertEquals("there", ((Hashtable) res.getL10N("l10n", "en")).get("hi"));

        res.setResource("img", Resources.MAGIC_IMAGE, null);
        assertEquals(0, res.getImageResourceNames().length);
    }

    @FormTest
    void systemResourceLookupFailsGracefully() {
        Resources sys = Resources.getSystemResource();
        assertNull(sys);
    }
}
