/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

package com.codenameone.developerguide.snippets.generated;

import com.codename1.gpu.*;
import com.codename1.ui.*;
import com.codename1.ui.animations.*;
import com.codename1.ui.events.*;
import com.codename1.ui.geom.*;
import com.codename1.ui.layouts.*;
import com.codename1.ui.list.*;
import com.codename1.ui.plaf.*;
import com.codename1.ui.util.*;
import com.codename1.components.*;
import com.codename1.charts.models.*;
import com.codename1.charts.renderers.*;
import com.codename1.charts.views.*;
import com.codename1.capture.*;
import com.codename1.io.*;
import com.codename1.l10n.*;
import com.codename1.location.*;
import com.codename1.maps.*;
import com.codename1.media.*;
import com.codename1.messaging.*;
import com.codename1.payment.*;
import com.codename1.processing.*;
import com.codename1.properties.*;
import com.codename1.push.*;
import com.codename1.security.*;
import com.codename1.social.*;
import com.codename1.ui.spinner.*;
import java.io.*;
import com.codename1.mapping.*;
import com.codename1.xml.*;
import java.util.*;
import com.codename1.annotations.*;
import com.codename1.properties.*;

class AnnotationJsonXmlMappingJava003Snippet {

    // tag::annotation-json-xml-mapping-java-003[]
    // A type from a third-party jar the build cannot annotate.
    static class LatLon {
        double lat;
        double lon;
    }

    static class LatLonMapper implements Mapper<LatLon> {
        @Override
        public Class<LatLon> type() {
            return LatLon.class;
        }

        @Override
        public Map<String, Object> toMap(LatLon instance) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("lat", Double.valueOf(instance.lat));
            m.put("lon", Double.valueOf(instance.lon));
            return m;
        }

        @Override
        public LatLon fromMap(Map<String, Object> map) {
            LatLon out = new LatLon();
            out.lat = readDouble(map.get("lat"));
            out.lon = readDouble(map.get("lon"));
            return out;
        }

        @Override
        public String xmlRootName() {
            return "latLon";
        }

        @Override
        public void writeXml(LatLon instance, Element root) {
            root.setAttribute("lat", String.valueOf(instance.lat));
            root.setAttribute("lon", String.valueOf(instance.lon));
        }

        @Override
        public LatLon readXml(Element root) {
            LatLon out = new LatLon();
            out.lat = Double.parseDouble(root.getAttribute("lat"));
            out.lon = Double.parseDouble(root.getAttribute("lon"));
            return out;
        }

        // JSONParser hands back a Double for every number, but a map that came
        // from somewhere else may hold any Number. Read through the interface
        // rather than casting to Double: a failed cast does not throw on iOS,
        // so the catch you would write for it never runs.
        private double readDouble(Object value) {
            return value instanceof Number ? ((Number) value).doubleValue() : 0;
        }
    }

    void registerMappers() {
        Mappers.register(new LatLonMapper());
    }
    // end::annotation-json-xml-mapping-java-003[]
}
