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
package com.codename1.flutter.plugins.geocoding;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.io.Util;
import com.codename1.ui.Display;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import dart.async.Completer;
import dart.async.Future;
import dart.core.DartList;
import dart.core.DateTime;

/**
 * geocoding's top-level functions. Codename One has no geocoder of its own, so these call
 * Google's Geocoding REST API with the key in the Display property
 * {@code google.geocoding.key} (set it with {@code Display.setProperty} before the first
 * lookup). With no key, no display or no match the future fails with
 * {@link NoResultFoundException}, the error the plugin raises for an empty answer.
 * {@code setLocaleIdentifier} becomes the request's {@code language}.
 */
public final class Geocoding {
    static final String KEY_PROPERTY = "google.geocoding.key";
    private static String locale;

    private Geocoding() {
    }

    public static Future<Object> setLocaleIdentifier(String localeIdentifier) {
        locale = localeIdentifier;
        return Future.value(null);
    }

    public static Future<Boolean> isPresent() {
        return Future.value(Boolean.valueOf(apiKey() != null));
    }

    public static Future<DartList<Placemark>> placemarkFromCoordinates(double latitude, double longitude) {
        final Completer<DartList<Placemark>> done = new Completer<DartList<Placemark>>();
        query("latlng=" + latitude + "," + longitude, new Answer() {
            @Override
            void results(List<?> results) {
                DartList<Placemark> out = new DartList<Placemark>();
                for (Object r : results) {
                    if (r instanceof Map) {
                        out.add(placemark((Map<?, ?>) r));
                    }
                }
                done.complete(out);
            }

            @Override
            void failed(Object error) {
                done.completeError(error);
            }
        });
        return done.future();
    }

    public static Future<DartList<Location>> locationFromAddress(String address) {
        final Completer<DartList<Location>> done = new Completer<DartList<Location>>();
        query("address=" + Util.encodeUrl(address == null ? "" : address), new Answer() {
            @Override
            void results(List<?> results) {
                DartList<Location> out = new DartList<Location>();
                for (Object r : results) {
                    Object geo = r instanceof Map ? ((Map<?, ?>) r).get("geometry") : null;
                    Object loc = geo instanceof Map ? ((Map<?, ?>) geo).get("location") : null;
                    if (loc instanceof Map) {
                        out.add(new Location(num(((Map<?, ?>) loc).get("lat")), num(((Map<?, ?>) loc).get("lng")),
                                DateTime.now()));
                    }
                }
                done.complete(out);
            }

            @Override
            void failed(Object error) {
                done.completeError(error);
            }
        });
        return done.future();
    }

    private static double num(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0;
    }

    private static String apiKey() {
        if (!Display.isInitialized()) {
            return null;
        }
        String k = Display.getInstance().getProperty(KEY_PROPERTY, null);
        return k == null || k.length() == 0 ? null : k;
    }

    abstract static class Answer {
        abstract void results(List<?> results);

        abstract void failed(Object error);
    }

    private static void query(String what, final Answer answer) {
        String key = apiKey();
        if (key == null) {
            answer.failed(new NoResultFoundException());
            return;
        }
        String url = "https://maps.googleapis.com/maps/api/geocode/json?" + what + "&key=" + Util.encodeUrl(key);
        if (locale != null) {
            url += "&language=" + Util.encodeUrl(locale.replace('_', '-'));
        }
        ConnectionRequest req = new ConnectionRequest() {
            private List<?> found;

            @Override
            protected void readResponse(InputStream input) throws IOException {
                Map<String, Object> m = new JSONParser().parseJSON(new InputStreamReader(input, "UTF-8"));
                Object r = m == null ? null : m.get("results");
                found = r instanceof List ? (List<?>) r : null;
            }

            @Override
            protected void postResponse() {
                if (found == null || found.isEmpty()) {
                    answer.failed(new NoResultFoundException());
                } else {
                    answer.results(found);
                }
            }

            @Override
            protected void handleException(Exception err) {
                answer.failed(new NoResultFoundException(String.valueOf(err.getMessage())));
            }

            @Override
            protected void handleErrorResponseCode(int code, String message) {
                answer.failed(new NoResultFoundException("HTTP " + code));
            }
        };
        req.setUrl(url);
        req.setPost(false);
        req.setFailSilently(true);
        NetworkManager.getInstance().addToQueue(req);
    }

    static Placemark placemark(Map<?, ?> r) {
        String streetNumber = null;
        String route = null;
        String country = null;
        String iso = null;
        String postal = null;
        String admin1 = null;
        String admin2 = null;
        String locality = null;
        String subLocality = null;
        Object comps = r.get("address_components");
        if (comps instanceof List) {
            for (Object c : (List<?>) comps) {
                if (!(c instanceof Map)) {
                    continue;
                }
                Map<?, ?> m = (Map<?, ?>) c;
                Object types = m.get("types");
                Object ln = m.get("long_name");
                Object sn = m.get("short_name");
                String longName = ln instanceof String ? (String) ln : null;
                String shortName = sn instanceof String ? (String) sn : null;
                if (!(types instanceof List)) {
                    continue;
                }
                List<?> t = (List<?>) types;
                if (t.contains("street_number")) {
                    streetNumber = longName;
                } else if (t.contains("route")) {
                    route = longName;
                } else if (t.contains("country")) {
                    country = longName;
                    iso = shortName;
                } else if (t.contains("postal_code")) {
                    postal = longName;
                } else if (t.contains("administrative_area_level_1")) {
                    admin1 = longName;
                } else if (t.contains("administrative_area_level_2")) {
                    admin2 = longName;
                } else if (t.contains("locality")) {
                    locality = longName;
                } else if (t.contains("sublocality") || t.contains("neighborhood")) {
                    subLocality = longName;
                }
            }
        }
        String street = route == null ? null : (streetNumber == null ? route : route + " " + streetNumber);
        String name = streetNumber != null ? streetNumber : (route != null ? route : locality);
        return new Placemark(name, street, iso, country, postal, admin1, admin2, locality, subLocality,
                route, streetNumber);
    }
}
