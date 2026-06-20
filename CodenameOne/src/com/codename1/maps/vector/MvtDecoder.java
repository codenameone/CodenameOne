/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.maps.vector;

import com.codename1.io.grpc.ProtoReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Decodes a Mapbox Vector Tile (MVT 2.1, `application/x-protobuf`) into the
/// in-memory [VectorTile] model using the framework's protobuf reader
/// ([ProtoReader]).
///
/// The geometry command stream is decoded per the MVT spec: integers are
/// either `(command | count << 3)` headers or zig-zag delta-encoded
/// parameters; `MoveTo` (1) starts a part, `LineTo` (2) extends it and
/// `ClosePath` (7) ends a polygon ring. Coordinates are kept tile-local
/// (0..extent); projection to the screen happens in [TileRenderer].
public final class MvtDecoder {

    // Tile-level field numbers.
    private static final int TILE_LAYERS = 3;

    // Layer-level field numbers.
    private static final int LAYER_NAME = 1;
    private static final int LAYER_FEATURES = 2;
    private static final int LAYER_KEYS = 3;
    private static final int LAYER_VALUES = 4;
    private static final int LAYER_EXTENT = 5;

    // Feature-level field numbers.
    private static final int FEATURE_ID = 1;
    private static final int FEATURE_TAGS = 2;
    private static final int FEATURE_TYPE = 3;
    private static final int FEATURE_GEOMETRY = 4;

    // Geometry command ids.
    private static final int CMD_MOVE_TO = 1;
    private static final int CMD_LINE_TO = 2;
    private static final int CMD_CLOSE_PATH = 7;

    private static final int WIRE_LEN = 2;

    private MvtDecoder() {
    }

    /// Decodes raw tile bytes into a [VectorTile]. The input must already be
    /// decompressed (the tile sources gunzip transparently).
    public static VectorTile decode(byte[] data) throws IOException {
        ProtoReader in = new ProtoReader(data);
        List layers = new ArrayList();
        int tag;
        while ((tag = in.readTag()) != 0) {
            if ((tag >>> 3) == TILE_LAYERS) {
                layers.add(decodeLayer(in.readBytes()));
            } else {
                in.skipField(tag);
            }
        }
        return new VectorTile(layers);
    }

    private static VectorLayer decodeLayer(byte[] body) throws IOException {
        ProtoReader in = new ProtoReader(body);
        String name = "";
        int extent = 4096;
        List rawFeatures = new ArrayList();
        List keys = new ArrayList();
        List values = new ArrayList();
        int tag;
        while ((tag = in.readTag()) != 0) {
            int field = tag >>> 3;
            switch (field) {
                case LAYER_NAME:
                    name = in.readString();
                    break;
                case LAYER_FEATURES:
                    rawFeatures.add(in.readBytes());
                    break;
                case LAYER_KEYS:
                    keys.add(in.readString());
                    break;
                case LAYER_VALUES:
                    values.add(decodeValue(in.readBytes()));
                    break;
                case LAYER_EXTENT:
                    extent = in.readVarint32();
                    break;
                default:
                    in.skipField(tag);
            }
        }
        List features = new ArrayList(rawFeatures.size());
        for (int i = 0; i < rawFeatures.size(); i++) {
            features.add(decodeFeature((byte[]) rawFeatures.get(i), keys, values));
        }
        return new VectorLayer(name, extent, features);
    }

    private static Object decodeValue(byte[] body) throws IOException {
        ProtoReader in = new ProtoReader(body);
        Object result = null;
        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (tag >>> 3) {
                case 1:
                    result = in.readString();
                    break;
                case 2:
                    result = Float.valueOf(in.readFloat());
                    break;
                case 3:
                    result = Double.valueOf(in.readDouble());
                    break;
                case 4:
                case 5:
                    result = Long.valueOf(in.readVarint64());
                    break;
                case 6:
                    result = Long.valueOf(in.readSInt64());
                    break;
                case 7:
                    result = in.readBool() ? Boolean.TRUE : Boolean.FALSE;
                    break;
                default:
                    in.skipField(tag);
            }
        }
        return result;
    }

    private static VectorFeature decodeFeature(byte[] body, List keys, List values) throws IOException {
        ProtoReader in = new ProtoReader(body);
        long id = 0;
        int type = VectorFeature.GEOM_UNKNOWN;
        IntArray tags = new IntArray();
        IntArray geometry = new IntArray();
        int tag;
        while ((tag = in.readTag()) != 0) {
            int field = tag >>> 3;
            int wire = tag & 0x7;
            switch (field) {
                case FEATURE_ID:
                    id = in.readVarint64();
                    break;
                case FEATURE_TAGS:
                    readPackedUint32(in, wire, tags);
                    break;
                case FEATURE_TYPE:
                    type = in.readVarint32();
                    break;
                case FEATURE_GEOMETRY:
                    readPackedUint32(in, wire, geometry);
                    break;
                default:
                    in.skipField(tag);
            }
        }
        Map attributes = new HashMap();
        for (int i = 0; i + 1 < tags.size(); i += 2) {
            int keyIndex = tags.get(i);
            int valIndex = tags.get(i + 1);
            if (keyIndex >= 0 && keyIndex < keys.size()
                    && valIndex >= 0 && valIndex < values.size()) {
                attributes.put(keys.get(keyIndex), values.get(valIndex));
            }
        }
        return new VectorFeature(id, type, attributes, decodeGeometry(geometry, type));
    }

    private static void readPackedUint32(ProtoReader in, int wire, IntArray target) throws IOException {
        if (wire == WIRE_LEN) {
            ProtoReader sub = new ProtoReader(in.readBytes());
            while (!sub.isAtEnd()) {
                target.add(sub.readVarint32());
            }
        } else {
            // Non-packed fallback: a single scalar element.
            target.add(in.readVarint32());
        }
    }

    private static List decodeGeometry(IntArray geom, int type) {
        List parts = new ArrayList();
        int i = 0;
        int cx = 0;
        int cy = 0;
        int command = 0;
        int length = 0;
        IntArray current = null;
        int n = geom.size();
        while (i < n) {
            if (length == 0) {
                int cmdInt = geom.get(i++);
                command = cmdInt & 0x7;
                length = cmdInt >>> 3;
                if (command == CMD_MOVE_TO) {
                    current = new IntArray();
                    parts.add(current);
                }
                if (command == CMD_CLOSE_PATH) {
                    // ClosePath carries no parameters; consume its count and
                    // end the current ring so the next MoveTo starts a new one.
                    length = 0;
                    current = null;
                    continue;
                }
            }
            if (command == CMD_MOVE_TO || command == CMD_LINE_TO) {
                if (i + 1 >= n) {
                    break;
                }
                cx += ProtoReader.zagZig32(geom.get(i++));
                cy += ProtoReader.zagZig32(geom.get(i++));
                if (current == null) {
                    current = new IntArray();
                    parts.add(current);
                }
                current.add(cx);
                current.add(cy);
                length--;
            } else {
                break;
            }
        }
        List out = new ArrayList(parts.size());
        for (int p = 0; p < parts.size(); p++) {
            out.add(((IntArray) parts.get(p)).toArray());
        }
        return out;
    }
}
