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
package com.codenameone.examples.savor.api;

/// The words the app and the server agree on for a delivery's Live Activity: the
/// "your courier is on the way" card on the lock screen.
///
/// The app starts the activity with a state built from these keys, and the
/// server sends later states with the same keys.
public final class DeliveryActivityKeys {
    /// The kind of activity.
    public static final String KIND = "order";
    /// What is happening, in words: "Preparing your order", "On its way".
    public static final String STATUS = "status";
    /// The restaurant's name.
    public static final String RESTAURANT = "restaurant";
    /// The courier's name, once one has the order.
    public static final String COURIER = "courier";
    /// The courier's vehicle, as they described it.
    public static final String CAR = "car";
    /// The vehicle's plate.
    public static final String PLATE = "plate";
    /// When the order reaches the door, in words: "12 min", or empty once it
    /// is there.
    public static final String ETA = "eta";
    /// How far along the order is, a number from 0 to 1 for the progress bar.
    public static final String PROGRESS = "progress";
    /// The values of [#PROGRESS] at each step.
    public static final double PLACED = 0.1d;
    public static final double PREPARING = 0.35d;
    public static final double READY = 0.55d;
    public static final double ON_THE_WAY = 0.8d;
    public static final double DONE = 1d;

    private DeliveryActivityKeys() {
    }
}
