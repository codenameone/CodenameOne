/// The main ChartComponent Package
///
/// The `charts` package enables Codename One developers to add charts and
/// visualizations to their apps without having to include external libraries or embedding web views.
/// We also wanted to harness the new features in the graphics pipeline to maximize performance.
///
/// Device Support
///
/// Since the charts package makes use of 2D transformations and shapes, it requires some
/// of the graphics features that are not yet available on all platforms. Currently the following
/// platforms are supported:
///
///
/// - Simulator
///
/// - Android
///
/// - iOS
///
/// Features
///
///
/// - **Built-in support for many common types of charts** including bar charts,
/// line charts, stacked charts, scatter charts, pie charts and more.
///
///
/// - **Pinch Zoom** - The `com.codename1.charts,ChartComponent`
/// class includes optional pinch zoom support.
///
///
/// - **Panning Support** - The
/// `com.codename1.charts,ChartComponent` class includes optional support for panning.
///
///
/// Chart Types
///
/// The `com.codename1.charts` package includes models and renderers for many
/// different types of charts. It is also extensible so that you can add your own chart types if required.
/// The following screen shots demonstrate a small sampling of the types of charts that can be
/// created.
///
///
///
/// **
///
/// The above screenshots were taken from the
/// [ChartsDemo app](https://github.com/codenameone/codenameone-demos/tree/master/ChartsDemo). Y
/// ou can start playing with this app by checking it out from our git repository.
///
///
///
///
/// How to Create A Chart
///
/// Adding a chart to your app involves four steps:
///
///
/// - **Build the model**. You can construct a model (aka data set) for the chart using one of the
/// existing model classes in the `com.codename1.charts.models` package. Essentially, this
/// is just where you add the data that you want to display.
///
///
/// - **Set up a renderer**. You can create a renderer for your chart using one of the
/// existing renderer classes in the `com.codename1.charts.renderers` package. The renderer
/// allows you to specify how the chart should look. E.g. the colors, fonts, styles, to use.
///
///
/// - **Create the Chart View**. Use one of the existing *view* classes in the
/// `com.codename1.charts.views` package.
///
///
/// - **Create a `com.codename1.charts,ChartComponent` **. In order to add your
/// chart to the UI, you need to wrap it in a `com.codename1.charts,ChartComponent` object.
///
///
/// You can check out the [ChartsDemo](https://github.com/codenameone/codenameone-demos/tree/master/ChartsDemo)
/// app for specific examples, but here is a high level view of some code that creates a Pie Chart.
///
/// ```java
/// /**
///  * Creates a renderer for the specified colors.
///  */
/// private DefaultRenderer buildCategoryRenderer(int[] colors) {
///     DefaultRenderer renderer = new DefaultRenderer();
///     renderer.setLabelsTextSize(15);
///     renderer.setLegendTextSize(15);
///     renderer.setMargins(new int[]{20, 30, 15, 0});
///     for (int color : colors) {
///         SimpleSeriesRenderer r = new SimpleSeriesRenderer();
///         r.setColor(color);
///         renderer.addSeriesRenderer(r);
///     }
///     return renderer;
/// }
///
/// /**
///  * Builds a category series using the provided values.
///  *
///  * @param titles the series titles
///  * @param values the values
///  * @return the category series
///  */
/// protected CategorySeries buildCategoryDataset(String title, double[] values) {
///     CategorySeries series = new CategorySeries(title);
///     int k = 0;
///     for (double value : values) {
///         series.add("Project " + ++k, value);
///     }
///
///     return series;
/// }
///
/// public Form createPieChartForm() {
///     // Generate the values
///     double[] values = new double[]{12, 14, 11, 10, 19};
///
///     // Set up the renderer
///     int[] colors = new int[]{ColorUtil.BLUE, ColorUtil.GREEN, ColorUtil.MAGENTA, ColorUtil.YELLOW, ColorUtil.CYAN};
///     DefaultRenderer renderer = buildCategoryRenderer(colors);
///     renderer.setZoomButtonsVisible(true);
///     renderer.setZoomEnabled(true);
///     renderer.setChartTitleTextSize(20);
///     renderer.setDisplayValues(true);
///     renderer.setShowLabels(true);
///     SimpleSeriesRenderer r = renderer.getSeriesRendererAt(0);
///     r.setGradientEnabled(true);
///     r.setGradientStart(0, ColorUtil.BLUE);
///     r.setGradientStop(0, ColorUtil.GREEN);
///     r.setHighlighted(true);
///
///     // Create the chart ... pass the values and renderer to the chart object.
///     PieChart chart = new PieChart(buildCategoryDataset("Project budget", values), renderer);
///
///     // Wrap the chart in a Component so we can add it to a form
///     ChartComponent c = new ChartComponent(chart);
///
///     // Create a form and show it.
///     Form f = new Form("Budget", new BorderLayout());
///     f.add(BorderLayout.CENTER, c);
///     return f;
///
/// }
/// ```
///
/// The charts package is derived work from the excellent [open source aChartEngine API.](http://www.achartengine.org/)
package com.codename1.charts;
