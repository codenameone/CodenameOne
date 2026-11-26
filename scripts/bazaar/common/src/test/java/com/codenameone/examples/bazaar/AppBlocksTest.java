/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codenameone.examples.bazaar.api.BlockValueDto;
import com.codenameone.examples.bazaar.api.ExtensionBlockDto;
import com.codenameone.examples.bazaar.ops.Blocks;
import com.codenameone.examples.bazaar.ui.AppBlockModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/// The block-tree parser and the render model the client draws app blocks
/// from. Nothing here opens a display or a bundle.
class AppBlocksTest {
    private static final String TREE = "{\"type\":\"stack\",\"children\":["
            + "{\"type\":\"heading\",\"text\":\"Loyalty\"},"
            + "{\"type\":\"text\",\"text\":\"{{name}} has {{points}} points\"},"
            + "{\"type\":\"badge\",\"text\":\"Gold\",\"tone\":\"success\"},"
            + "{\"type\":\"list\",\"items\":[\"One\",\"Two\"]},"
            + "{\"type\":\"button\",\"label\":\"History\",\"url\":\"https://app.example/h\"},"
            + "{\"type\":\"divider\"},"
            + "{\"type\":\"field\",\"key\":\"bonus\",\"label\":\"Bonus\",\"kind\":\"number\","
            + "\"required\":true},"
            + "{\"type\":\"field\",\"key\":\"tier\",\"label\":\"Tier\",\"kind\":\"select\","
            + "\"options\":[\"Gold\",\"Silver\"]},"
            + "{\"type\":\"button\",\"label\":\"Grant\",\"action\":\"grant\"}]}";

    private static ExtensionBlockDto dto(String tree, List<BlockValueDto> values) {
        return new ExtensionBlockDto("ext1", "inst1", "Points", Blocks.ORDER_BLOCK, "", tree,
                values, "");
    }

    @Test
    void aTreeFlattensToRowsWithItsDataBound() {
        List<BlockValueDto> values = new ArrayList<BlockValueDto>();
        values.add(new BlockValueDto("name", "Ada"));
        values.add(new BlockValueDto("points", "42"));
        AppBlockModel m = AppBlockModel.of(dto(TREE, values));
        assertEquals("", m.error);
        List<String> kinds = new ArrayList<String>();
        for (Blocks.Row r : m.rows) {
            kinds.add(r.kind);
        }
        assertTrue(kinds.contains("heading"), kinds.toString());
        assertTrue(kinds.contains("badge"), kinds.toString());
        assertTrue(kinds.contains("link"), kinds.toString());
        assertTrue(kinds.contains("divider"), kinds.toString());
        String bound = null;
        int items = 0;
        for (Blocks.Row r : m.rows) {
            if (r.text.indexOf("points") >= 0) {
                bound = r.text;
            }
            if ("item".equals(r.kind)) {
                items++;
            }
        }
        assertEquals("Ada has 42 points", bound);
        assertEquals(2, items);
        assertEquals(2, m.fields.size());
        assertEquals(1, m.actions.size());
        assertEquals("grant", m.actions.get(0).action);
        assertEquals("app:ext1:bonus", m.fieldId(m.fields.get(0)));
    }

    @Test
    void aMissingValueBindsEmptyAndValuesAreNeverMarkup() {
        AppBlockModel m = AppBlockModel.of(dto("{\"type\":\"text\",\"text\":\"Hi {{who}}!\"}",
                null));
        assertEquals("Hi !", m.rows.get(0).text);
        List<BlockValueDto> v = new ArrayList<BlockValueDto>();
        v.add(new BlockValueDto("who", "<b>x</b>"));
        m = AppBlockModel.of(dto("{\"type\":\"text\",\"text\":\"Hi {{who}}\"}", v));
        assertEquals("Hi <b>x</b>", m.rows.get(0).text, "bound as text, to be drawn as text");
    }

    @Test
    void badTreesAreRefused() {
        String[] bad = {
            "{\"type\":\"script\"}",
            "{\"type\":\"text\",\"text\":\"x\",\"style\":\"y\"}",
            "{\"type\":\"image\",\"src\":\"http://cdn.example/a.png\"}",
            "{\"type\":\"image\",\"src\":\"javascript:alert(1)\"}",
            "{\"type\":\"button\",\"label\":\"x\",\"url\":\"https://a.example\",\"action\":\"y\"}",
            "{\"type\":\"field\",\"key\":\"Bad Key\",\"label\":\"x\",\"kind\":\"text\"}",
            "{\"type\":\"field\",\"key\":\"k\",\"label\":\"x\",\"kind\":\"color\"}",
            "{\"type\":\"text\",\"text\":\"a\",\"text\":\"b\"}",
            "{\"type\":\"stack\",\"children\":[]}",
            "[1,2]",
            "{\"type\":\"text\",\"text\":\"x\"",
        };
        for (String tree : bad) {
            assertThrows(Blocks.Invalid.class, () -> Blocks.parse(tree), tree);
            AppBlockModel m = AppBlockModel.of(dto(tree, null));
            assertTrue(m.error.length() > 0, tree);
            assertEquals(0, m.rows.size(), tree);
        }
        String deep = "{\"type\":\"text\",\"text\":\"x\"}";
        for (int iter = 0; iter < Blocks.MAX_DEPTH + 1; iter++) {
            deep = "{\"type\":\"stack\",\"children\":[" + deep + "]}";
        }
        final String tooDeep = deep;
        assertThrows(Blocks.Invalid.class, () -> Blocks.parse(tooDeep));
    }

    @Test
    void targetsLimitWhatATreeMayHold() throws Blocks.Invalid {
        Blocks.Node field = Blocks.parse("{\"type\":\"field\",\"key\":\"k\",\"label\":\"x\","
                + "\"kind\":\"text\"}");
        assertThrows(Blocks.Invalid.class, () -> Blocks.checkTarget(Blocks.STOREFRONT_BLOCK,
                field));
        Blocks.checkTarget(Blocks.CHECKOUT_BLOCK, field);
        Blocks.Node button = Blocks.parse("{\"type\":\"button\",\"label\":\"x\","
                + "\"action\":\"go\"}");
        assertThrows(Blocks.Invalid.class, () -> Blocks.checkTarget(Blocks.CHECKOUT_BLOCK,
                button));
        Blocks.checkTarget(Blocks.ORDER_ACTION, button);
        assertThrows(Blocks.Invalid.class, () -> Blocks.checkTarget(Blocks.ORDER_ACTION, field));
    }

    @Test
    void answersAreCheckedByKind() throws Blocks.Invalid {
        AppBlockModel m = AppBlockModel.of(dto(TREE, null));
        Map<String, String> given = new HashMap<String, String>();
        given.put("bonus", "abc");
        assertThrows(Blocks.Invalid.class, () -> m.answers(given));
        given.put("bonus", "");
        assertThrows(Blocks.Invalid.class, () -> m.answers(given), "required");
        given.put("bonus", " 12 ");
        given.put("tier", "Bronze");
        assertThrows(Blocks.Invalid.class, () -> m.answers(given), "not an option");
        given.put("tier", "Silver");
        List<BlockValueDto> a = m.answers(given);
        assertEquals("12", a.get(0).value());
        assertEquals("Silver", a.get(1).value());
    }

    @Test
    void appDataIsCapped() throws Blocks.Invalid {
        StringBuilder long1 = new StringBuilder();
        for (int iter = 0; iter < 300; iter++) {
            long1.append('x');
        }
        Map<String, String> v = Blocks.values("{\"values\":{\"a\":\"" + long1 + "\",\"n\":7}}");
        assertEquals(200, v.get("a").length());
        assertEquals("7", v.get("n"));
        assertThrows(Blocks.Invalid.class, () -> Blocks.values("{\"values\":[1]}"));
    }
}
