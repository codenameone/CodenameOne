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
package com.codenameone.examples.bazaar.web.commerce;

import java.util.ArrayList;
import java.util.List;

/// One fulfilment of an order on the order page: its state, the actions it
/// is ready for, and the carrier rates when it is waiting for a label.
public class ParcelView {
    public String id = "";
    public String title = "";
    public String status = "";
    public String tone = "";
    public String detail = "";
    public String tracking = "";
    public String labelUrl = "";
    public String slipUrl = "";
    public String packAction = "";
    public String labelAction = "";
    public String shipAction = "";
    public boolean canPack;
    public boolean canLabel;
    public boolean canShip;
    public boolean hasLabel;
    /// The services the carrier offers, `id` the service and `title` its
    /// name, price and days; the cheapest is pre-selected (`flag`).
    public List<AdminRow> rates = new ArrayList<AdminRow>();
}
