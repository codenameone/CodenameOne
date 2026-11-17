/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.rbx.place;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.rbx.Engine;
import com.codename1.rbx.Scheduler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/// Place I/O: a hand-written `.rbxlx` loads into an engine and its script
/// sees the values the file holds; a save of that engine survives XML and
/// binary round trips unchanged.
class PlaceIoTest {
    static byte[] resource(String name) throws IOException {
        InputStream in = PlaceIoTest.class.getResourceAsStream("/place/" + name);
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) {
            b.write(buf, 0, n);
        }
        in.close();
        return b.toByteArray();
    }

    static List<String> run(Engine e) {
        final List<String> out = new ArrayList<String>();
        e.getScheduler().setOutput(new Scheduler.Output() {
            @Override
            public void message(String type, String text, Object owner) {
                out.add(type + ": " + text);
            }
        });
        for (int i = 0; i < 120; i++) {
            e.step(1 / 60.0);
        }
        return out;
    }

    static Engine load(byte[] data, CompatReport[] report) {
        Engine e = new Engine(Engine.SOLO);
        report[0] = PlaceLoader.loadPlace(e, PlaceFile.read(data));
        return e;
    }

    @Test
    void loadsAndRuns() throws IOException {
        CompatReport[] r = new CompatReport[1];
        Engine e = load(resource("sample.rbxlx"), r);
        List<String> out = run(e);
        assertTrue(out.toString().contains(": DONE"), out.toString());
        CompatReport rep = r[0];
        assertEquals(Integer.valueOf(1), rep.unknownClasses.get("Sparkler"), rep.toString());
        assertTrue(rep.unknownProperties.containsKey("Part.SomethingNew"), rep.toString());
        assertTrue(!rep.unknownProperties.containsKey("Part.FrontSurface"), rep.toString());
        assertTrue(rep.missingAssets.containsKey("rbxassetid://123456"), rep.toString());
        assertTrue(rep.scriptFindings.get("Main").toString().contains("NoSuchService"), rep.toString());
        assertEquals(1, rep.scripts);
    }

    static String dump(PlaceFile f) {
        StringBuilder b = new StringBuilder();
        for (PlaceNode n : f.all()) {
            b.append(n.className).append(' ').append(n.name()).append(" parent=")
                    .append(n.parent == null ? "-" : n.parent.name()).append('\n');
            for (Map.Entry<String, PlaceValue> p : n.props.entrySet()) {
                Object v = p.getValue().value;
                String vs = v instanceof PlaceNode ? "->" + ((PlaceNode) v).name()
                        : v instanceof byte[] ? new String((byte[]) v) : v instanceof String[]
                        ? java.util.Arrays.toString((String[]) v) : v instanceof com.codename1.rbx.RbxType
                        ? ((com.codename1.rbx.RbxType) v).str() : String.valueOf(v);
                b.append("  ").append(p.getKey()).append(' ').append(p.getValue().type).append(' ').append(vs)
                        .append('\n');
            }
        }
        return b.toString();
    }

    @Test
    void saveRoundTrips() throws IOException {
        CompatReport[] r = new CompatReport[1];
        Engine e = load(resource("sample.rbxlx"), r);
        PlaceFile saved = PlaceSaver.save(e);
        String expect = dump(saved);
        PlaceFile viaXml = RbxlxReader.read(RbxlxWriter.write(saved));
        assertEquals(expect, dump(viaXml));
        byte[] bin = RbxlWriter.write(viaXml);
        assertTrue(RbxlReader.isBinary(bin));
        PlaceFile viaBin = PlaceFile.read(bin);
        assertEquals(dump(viaXml).replace(" float ", " num ").replace(" int ", " num "),
                dump(viaBin).replace(" float ", " num ").replace(" int ", " num "));
        // The saved place, loaded again, still passes its own script.
        Engine again = new Engine(Engine.SOLO);
        PlaceLoader.loadPlace(again, viaBin);
        List<String> out = run(again);
        assertTrue(out.toString().contains(": DONE"), out.toString());
    }

    @Test
    void axisAlignedRotationsRoundTrip() {
        for (int id = 1; id <= 36; id++) {
            int k = id - 1;
            if (k / 6 % 3 == k % 6 % 3) {
                continue;
            }
            assertEquals(id, RbxlReader.rotationId(RbxlReader.specialRotation(id)));
        }
        assertEquals(2, RbxlReader.rotationId(new double[] {1, 0, 0, 0, 1, 0, 0, 0, 1}));
    }

    @Test
    void lz4Blocks() {
        // literal "abc", then a 9 byte match at offset 3 (overlapping), then literal "z".
        byte[] block = {0x35, 'a', 'b', 'c', 3, 0, 0x10, 'z'};
        assertEquals("abcabcabcabcz", new String(Lz4.decompress(block, 0, block.length, 13)));
    }

    @Test
    public void forgedChunkSizeIsRejectedBeforeAllocating() {
        // A tiny LZ4 chunk claiming ~2 GB once decompressed must fail as a
        // format error, not exhaust the heap on new byte[ulen].
        byte[] block = {0x10, 'a'};
        try {
            RbxlReader.checkChunkSizes(block.length, block.length, 0x7FFFFFF0, block, 0);
            org.junit.jupiter.api.Assertions.fail("accepted a 2 GB chunk");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("chunk"));
        }
        try {
            RbxlReader.checkChunkSizes(block.length, block.length, 1 << 20, block, 0);
            org.junit.jupiter.api.Assertions.fail("accepted an impossible LZ4 ratio");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("ratio"));
        }
        // Uncompressed chunk longer than the file.
        try {
            RbxlReader.checkChunkSizes(10, 0, 11, block, 0);
            org.junit.jupiter.api.Assertions.fail("accepted an overrunning raw chunk");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("overruns"));
        }
        RbxlReader.checkChunkSizes(block.length, block.length, 1, block, 0);
    }
}
