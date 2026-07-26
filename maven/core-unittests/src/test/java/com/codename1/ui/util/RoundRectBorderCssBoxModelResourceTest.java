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
package com.codename1.ui.util;

import com.codename1.junit.UITestBase;
import com.codename1.ui.plaf.RoundRectBorder;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Round-trips a {@link RoundRectBorder} through the resource writer
 * ({@link EditableResources#save}) and reader ({@link Resources}) to guard the CSS box
 * model flag added in resource format 1.16. Every border the CSS compiler generates
 * carries the flag, so a broken read/write pairing would silently restore the sizing
 * regression from https://github.com/codenameone/CodenameOne/discussions/5454 in a
 * shipped theme.
 */
public class RoundRectBorderCssBoxModelResourceTest extends UITestBase {

    @Test
    public void cssBoxModelFlagSurvivesResSaveLoad() throws Exception {
        RoundRectBorder border = RoundRectBorder.create()
                .cornerRadius(3f)
                .cssBoxModel(true)
                .topLeftMode(false)
                .bottomLeftMode(false)
                .topRightMode(true)
                .bottomRightMode(true);

        RoundRectBorder loaded = saveAndReload(border);
        assertTrue(loaded.isCssBoxModel(), "CSS box model flag survived the round-trip");
        // The fields written around the new one must be unaffected by it.
        assertEquals(3f, loaded.getCornerRadius(), 0.001f, "corner radius survived");
        assertFalse(loaded.isTopLeft(), "square top-left corner survived");
        assertTrue(loaded.isTopRight(), "rounded top-right corner survived");
        assertTrue(loaded.isBottomRight(), "rounded bottom-right corner survived");
        assertFalse(loaded.isBottomLeft(), "square bottom-left corner survived");
    }

    @Test
    public void handWrittenBorderStaysOnTheLegacySizing() throws Exception {
        RoundRectBorder border = RoundRectBorder.create().cornerRadius(2f);
        assertFalse(border.isCssBoxModel(), "borders default to the legacy pill sizing");

        assertFalse(saveAndReload(border).isCssBoxModel(), "legacy sizing survived the round-trip");
    }

    @Test
    public void resourcesWrittenBeforeTheFlagExistedLoadAsLegacy() throws Exception {
        byte[] resource = save(RoundRectBorder.create().cornerRadius(2f).cssBoxModel(true));
        // Rewrite the header as the last format that had no CSS box model flag. The reader
        // must then ignore the trailing byte rather than mistaking it for another field.
        assertEquals(16, minorVersionOf(resource), "the writer bumped the format to 1.16");
        setMinorVersion(resource, 15);

        Resources loaded = new Resources(new ByteArrayInputStream(resource), -1);
        RoundRectBorder border = (RoundRectBorder) loaded.getTheme("t").get("RoundButton.border");
        assertFalse(border.isCssBoxModel(), "a 1.15 resource predates the flag, so it is legacy");
        assertEquals(2f, border.getCornerRadius(), 0.001f, "the fields before it still read back");
    }

    private static RoundRectBorder saveAndReload(RoundRectBorder border) throws Exception {
        Resources loaded = new Resources(new ByteArrayInputStream(save(border)), -1);
        Object roundTripped = loaded.getTheme("t").get("RoundButton.border");
        assertInstanceOf(RoundRectBorder.class, roundTripped, "border round-trips as a RoundRectBorder");
        return (RoundRectBorder) roundTripped;
    }

    private static byte[] save(RoundRectBorder border) throws Exception {
        Hashtable<String, Object> theme = new Hashtable<String, Object>();
        theme.put("RoundButton.border", border);

        EditableResources editable = new EditableResources();
        editable.setTheme("t", theme);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        editable.save(out);
        return out.toByteArray();
    }

    /// The minor version lives in the header, after the resource count, the magic byte, an
    /// empty UTF string, the header size and the major version.
    private static int minorVersionOfOffset(byte[] resource) throws Exception {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(resource));
        in.readShort();
        in.readByte();
        int utfLength = in.readUnsignedShort();
        return 2 + 1 + 2 + utfLength + 2 + 2;
    }

    private static int minorVersionOf(byte[] resource) throws Exception {
        int offset = minorVersionOfOffset(resource);
        return ((resource[offset] & 0xff) << 8) | (resource[offset + 1] & 0xff);
    }

    private static void setMinorVersion(byte[] resource, int version) throws Exception {
        int offset = minorVersionOfOffset(resource);
        resource[offset] = (byte) (version >> 8);
        resource[offset + 1] = (byte) version;
    }
}
