/// Animated transitions between chart series states.
///
/// Concrete subclasses of `SeriesTransition` buffer changes to a chart's
/// model and renderer so the `ChartComponent` can either tween between the
/// previous and next state via `animateChart()` or jump to it in one shot
/// via `updateChart()`.
package com.codename1.charts.transitions;
