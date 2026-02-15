---
title: Terse Table, Radar Chart and Networking Enhancements
slug: terse-table-radar-chart-networking
url: /blog/terse-table-radar-chart-networking/
original_url: https://www.codenameone.com/blog/terse-table-radar-chart-networking.html
aliases:
- /blog/terse-table-radar-chart-networking.html
date: '2019-09-13'
author: Shai Almog
---

![Header Image](/blog/terse-table-radar-chart-networking/new-features-3.jpg)

This is the 3rd installment of the updates we did over the summer and so far it isn’t the last one. We have at least one more part coming in next week…​

### Terse Table Layout

`TableLayout` is pretty darn verbose e.g. this snippet from the [TableLayout JavaDoc](/javadoc/com/codename1/ui/table/TableLayout/):
    
    
    cnt.add(tl.createConstraint().
            horizontalSpan(2).
            heightPercentage(80).
            verticalAlign(Component.CENTER).
            horizontalAlign(Component.CENTER),
                new Label("Span H")).
    
        add(new Label("BBB")).
    
        add(tl.createConstraint().
            widthPercentage(60).
            heightPercentage(20),
                new Label("CCC"));

This is pretty verbose and a bit painful to write so we added shorthand methods:

  * `vs()` = `verticalSpan()`

  * `hs()` = `horizontalSpan()`

  * `wp()` = `widthPercentage()`

  * `hp()` = `heightPercentage()`

  * `ha()` = `horizontalAlign()`

  * `va()` = `verticalAlign()`

  * `cc()` = `constraint()`

As such we can rewrite the snippet above as such:
    
    
    cnt.add(tl.cc().
            hs(2).
            hp(80).
            va(Component.CENTER).
            ha(Component.CENTER),
                new Label("Span H")).
    
        add(new Label("BBB")).
    
        add(tl.cc().
            wp(60).
            hp(20),
                new Label("CCC"));

### RadarChart

[David Day](https://github.com/dj6082013) contributed new support for `RadarChart` in this [pull request](https://github.com/codenameone/CodenameOne/pull/2876). You can check out a sample of the radar chart in the [samples](/blog/sheets-samples/). Specifically:
    
    
    public void showChart() {
        Form f = new Form("RadarChartSample", new BorderLayout());
        f.add(BorderLayout.CENTER, getSampleChart());
        f.show();
    }
    
    ChartComponent getSampleChart(){
        // Create dataset
        AreaSeries dataset = new AreaSeries();
    
        CategorySeries series1 = new CategorySeries("May");
        series1.add("Health", 0.8);
        series1.add("Attack", 0.6);
        series1.add("Defense", 0.4);
        series1.add("Critical", 0.2);
        series1.add("Speed", 1.0);
        dataset.addSeries(series1);
    
        CategorySeries series2 = new CategorySeries("Chang");
        series2.add("Health", 0.3);
        series2.add("Attack", 0.7);
        series2.add("Defense", 0.5);
        series2.add("Critical", 0.1);
        series2.add("Speed", 0.3);
        dataset.addSeries(series2);
    
        // Setup renderer
        DefaultRenderer renderer = new DefaultRenderer();
        renderer.setLegendTextSize(32);
        renderer.setLabelsTextSize(24);
        renderer.setLabelsColor(ColorUtil.BLACK);
        renderer.setShowLabels(true);
    
        SimpleSeriesRenderer r1 = new SimpleSeriesRenderer();
        r1.setColor(ColorUtil.MAGENTA);
        renderer.addSeriesRenderer(r1);
    
        SimpleSeriesRenderer r2 = new SimpleSeriesRenderer();
        r2.setColor(ColorUtil.CYAN);
        renderer.addSeriesRenderer(r2);
    
        // Create chart
        return new ChartComponent(new RadarChart(dataset,renderer));
    }

### Networking Enhancements

#### Better Error Code Handling

Up until recently if we got an error response code it wasn’t sent through the global error handler and was handled via the local error handling chain first. This is no longer the case and these errors are now handled correctly.

However, if you relied on that misbehavior of older versions we have  
`setHandleErrorCodesInGlobalErrorHandler(boolean)`. This defaults to true, you can set it to false to change the default behavior.

#### Network Monitor Stats

Network monitor now has stats for time duration etc. You can see these by opening the network monitor and inspecting the values:

![Network Monitor Enhancements](/blog/terse-table-radar-chart-networking/network-monitor-enhancements.png)

Figure 1. Network Monitor Enhancements

#### Data Interface

The Data interface can be used to abstract data that has size and can be appended to an `OutputStream`. This is very handy for providing large amounts of data for processing, so that the data itself doesn’t need to be passed around. This interface should be used in a few places in the API to facilitate working with large data objects, such as for file uploads.

E.g. For multi-part file uploads the best way to deal with large objects right now is to store them in a file, and then provide the file path to the connection request, so that the platform’s native implementation can chunk or stream it appropriately.

This interface provides potentially a cleaner more generic way to pass large amounts of data to a connection request.

The main entry point for this functionality is `ConnectionRequest.setRequestBody(Data data)`. The interface itself is pretty trivial and contains two methods:
    
    
    public interface Data {
    
        /**
         * Appends the data's content to an output stream.
         * @param output The output stream to append to.
         * @throws IOException
         */
        public void appendTo(OutputStream output) throws IOException;
    
        /**
         * Gets the size of the data content.
         * @return Size of content in bytes.
         * @throws IOException
         */
        public long getSize() throws IOException;
    }

Within the interface there are several concrete implementations specifically `StringData`,  
`FileData`, `StorageData` and `ByteData`. E.g.:
    
    
    public static class StringData implements Data {
        private byte[] bytes;
    
        public StringData(String str) {
            this(str, "UTF-8");
        }
    
        public StringData(String str, String charset) {
            try {
                bytes = str.getBytes(charset);
            } catch (UnsupportedEncodingException ex) {
                Log.e(ex);
                throw new RuntimeException("Failed to create StringData with encoding "+charset);
            }
        }
        @Override
        public void appendTo(OutputStream output) throws IOException {
            output.write(bytes);
        }
    
        @Override
        public long getSize() throws IOException {
            return bytes.length;
        }
    }

This makes things such as working with files in `Storage` (as opposed to `FileSystemStorage`) easier.

---

## Discussion

_Join the conversation via GitHub Discussions._

{{< giscus >}}
