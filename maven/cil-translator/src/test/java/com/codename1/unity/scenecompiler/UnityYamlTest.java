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
package com.codename1.unity.scenecompiler;

import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/// The parts of Unity's YAML that have each been read wrongly once.
public class UnityYamlTest {
    /// A plain scalar too long for one line goes on, indented further, on
    /// the next. Editors of 2017 and before broke the type name of every
    /// event this way; read as the end of the mapping, the line ended every
    /// mapping around it and the rest of the component was dropped.
    @Test
    public void aPlainScalarGoesOnOverTheNextLine() {
        String text = "%YAML 1.1\n"
                + "--- !u!114 &7\n"
                + "MonoBehaviour:\n"
                + "  m_OnCullStateChanged:\n"
                + "    m_PersistentCalls:\n"
                + "      m_Calls: []\n"
                + "    m_TypeName: UnityEngine.UI.MaskableGraphic+CullStateChangedEvent, UnityEngine.UI,\n"
                + "      Version=1.0.0.0, Culture=neutral, PublicKeyToken=null\n"
                + "  m_FontData:\n"
                + "    m_FontSize: 14\n"
                + "  m_Text: Restart\n"
                + "  m_List:\n"
                + "  - first item that is long,\n"
                + "    and goes on\n"
                + "  - second\n"
                + "  m_After: 1\n";
        List<UnityYaml.Document> docs = UnityYaml.parse(text);
        Assert.assertEquals(1, docs.size());
        Map<String, Object> p = docs.get(0).properties;
        Map<?, ?> event = (Map<?, ?>) p.get("m_OnCullStateChanged");
        Assert.assertEquals("UnityEngine.UI.MaskableGraphic+CullStateChangedEvent, UnityEngine.UI,"
                + " Version=1.0.0.0, Culture=neutral, PublicKeyToken=null", event.get("m_TypeName"));
        Assert.assertEquals("14", ((Map<?, ?>) p.get("m_FontData")).get("m_FontSize"));
        Assert.assertEquals("Restart", p.get("m_Text"));
        List<?> list = (List<?>) p.get("m_List");
        Assert.assertEquals(2, list.size());
        Assert.assertEquals("first item that is long, and goes on", list.get(0));
        Assert.assertEquals("second", list.get(1));
        Assert.assertEquals("1", p.get("m_After"));
    }

    /// What follows a key with a value is not always a continuation: a
    /// sequence under a key sits at the key's own indentation.
    @Test
    public void aSequenceAtTheIndentationOfItsKeyIsNotAContinuation() {
        Map<String, Object> meta = UnityYaml.parseMeta("guid: abc\n"
                + "TextureImporter:\n"
                + "  spriteMode: 3\n"
                + "  spriteSheet:\n"
                + "    outline:\n"
                + "    - - {x: 0, y: 2}\n"
                + "      - {x: -1, y: -1}\n"
                + "      - {x: 1, y: -1}\n"
                + "    physicsShape: []\n"
                + "  alignment: 0\n");
        Map<?, ?> importer = (Map<?, ?>) meta.get("TextureImporter");
        Assert.assertEquals("3", importer.get("spriteMode"));
        Assert.assertEquals("0", importer.get("alignment"));
        List<?> outline = (List<?>) ((Map<?, ?>) importer.get("spriteSheet")).get("outline");
        Assert.assertEquals(1, outline.size());
        Assert.assertEquals(3, ((List<?>) outline.get(0)).size());
        Assert.assertEquals("-1", ((Map<?, ?>) ((List<?>) outline.get(0)).get(1)).get("x"));
    }
}
