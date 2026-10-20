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
package com.codenameone.examples.savor.geo;

import com.codenameone.examples.savor.api.PlaceDto;

import java.io.IOException;
import java.util.List;

/// Turns text into places and a point into an address.
///
/// The app never talks to a geocoding service itself. It asks this server,
/// which asks whichever service is configured: no key ships inside the app, the
/// service can be replaced without a new release, and the server is where its
/// use can be limited.
public interface Geocoder {
    /// Places matching `text`, best first, favouring those near the point.
    List<PlaceDto> search(String text, double lat, double lng) throws IOException;

    /// The address at a point, or null when the service knows of none.
    PlaceDto reverse(double lat, double lng) throws IOException;
}
