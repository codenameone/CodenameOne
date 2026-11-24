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
/// Modern, interactive and animated charts.
///
/// `Chart` is the component; series (`com.codename1.ui.chart.series`) hold data and paint
/// themselves, axes (`com.codename1.ui.chart.axis`) map values to the plot. Labels are laid out
/// so they never overlap, and pan, zoom, tooltips and legend toggles work with touch, mouse,
/// trackpad and keyboard.
///
/// ```java
/// Form form = new Form("Revenue", new BorderLayout());
/// Chart chart = new Chart().setTitle("Revenue")
///         .setXAxis(new CategoryAxis(Axis.BOTTOM, "Q1", "Q2", "Q3", "Q4"));
/// chart.addSeries(new BarSeries("Sales").setData(null, new double[]{120, 180, 150, 210}));
/// form.add(BorderLayout.CENTER, chart);
/// ```
///
/// Every change to the data animates, and several series can share a chart. `Chart.syncWith`
/// links charts so a price pane and its volume pane pan and zoom together. `Sparkline` is a
/// small trend for list rows. For readers who cannot see the picture, a chart describes itself
/// to screen readers and `Chart.toTable` returns the same data as a table.
///
/// This package replaces the deprecated `com.codename1.charts`.
package com.codename1.ui.chart;
