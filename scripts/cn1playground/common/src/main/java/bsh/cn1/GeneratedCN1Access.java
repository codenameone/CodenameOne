package bsh.cn1;

import com.codename1.ui.Form;
import com.codenameone.playground.PlaygroundContext;
import java.util.LinkedHashMap;
import java.util.Map;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ads;
import bsh.cn1.gen.GeneratedAccess_com_codename1_analytics;
import bsh.cn1.gen.GeneratedAccess_com_codename1_annotations;
import bsh.cn1.gen.GeneratedAccess_com_codename1_background;
import bsh.cn1.gen.GeneratedAccess_com_codename1_capture;
import bsh.cn1.gen.GeneratedAccess_com_codename1_charts;
import bsh.cn1.gen.GeneratedAccess_com_codename1_charts_compat;
import bsh.cn1.gen.GeneratedAccess_com_codename1_charts_models;
import bsh.cn1.gen.GeneratedAccess_com_codename1_charts_renderers;
import bsh.cn1.gen.GeneratedAccess_com_codename1_charts_transitions;
import bsh.cn1.gen.GeneratedAccess_com_codename1_charts_util;
import bsh.cn1.gen.GeneratedAccess_com_codename1_charts_views;
import bsh.cn1.gen.GeneratedAccess_com_codename1_cloud;
import bsh.cn1.gen.GeneratedAccess_com_codename1_codescan;
import bsh.cn1.gen.GeneratedAccess_com_codename1_compat_java_util;
import bsh.cn1.gen.GeneratedAccess_com_codename1_components;
import bsh.cn1.gen.GeneratedAccess_com_codename1_contacts;
import bsh.cn1.gen.GeneratedAccess_com_codename1_db;
import bsh.cn1.gen.GeneratedAccess_com_codename1_facebook;
import bsh.cn1.gen.GeneratedAccess_com_codename1_facebook_ui;
import bsh.cn1.gen.GeneratedAccess_com_codename1_impl;
import bsh.cn1.gen.GeneratedAccess_com_codename1_io;
import bsh.cn1.gen.GeneratedAccess_com_codename1_io_gzip;
import bsh.cn1.gen.GeneratedAccess_com_codename1_io_rest;
import bsh.cn1.gen.GeneratedAccess_com_codename1_io_services;
import bsh.cn1.gen.GeneratedAccess_com_codename1_io_tar;
import bsh.cn1.gen.GeneratedAccess_com_codename1_javascript;
import bsh.cn1.gen.GeneratedAccess_com_codename1_l10n;
import bsh.cn1.gen.GeneratedAccess_com_codename1_location;
import bsh.cn1.gen.GeneratedAccess_com_codename1_maps;
import bsh.cn1.gen.GeneratedAccess_com_codename1_maps_layers;
import bsh.cn1.gen.GeneratedAccess_com_codename1_maps_providers;
import bsh.cn1.gen.GeneratedAccess_com_codename1_media;
import bsh.cn1.gen.GeneratedAccess_com_codename1_messaging;
import bsh.cn1.gen.GeneratedAccess_com_codename1_notifications;
import bsh.cn1.gen.GeneratedAccess_com_codename1_payment;
import bsh.cn1.gen.GeneratedAccess_com_codename1_plugin;
import bsh.cn1.gen.GeneratedAccess_com_codename1_plugin_event;
import bsh.cn1.gen.GeneratedAccess_com_codename1_processing;
import bsh.cn1.gen.GeneratedAccess_com_codename1_properties;
import bsh.cn1.gen.GeneratedAccess_com_codename1_push;
import bsh.cn1.gen.GeneratedAccess_com_codename1_share;
import bsh.cn1.gen.GeneratedAccess_com_codename1_social;
import bsh.cn1.gen.GeneratedAccess_com_codename1_system;
import bsh.cn1.gen.GeneratedAccess_com_codename1_testing;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_animations;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_css;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_events;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_geom;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_html;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_layouts;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_layouts_mig;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_list;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_painter;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_plaf;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_scene;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_spinner;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_table;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_tree;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_util;
import bsh.cn1.gen.GeneratedAccess_com_codename1_ui_validation;
import bsh.cn1.gen.GeneratedAccess_com_codename1_util;
import bsh.cn1.gen.GeneratedAccess_com_codename1_util_promise;
import bsh.cn1.gen.GeneratedAccess_com_codename1_util_regex;
import bsh.cn1.gen.GeneratedAccess_com_codename1_xml;
import bsh.cn1.gen.GeneratedAccess_com_codenameone_playground;
import bsh.cn1.gen.GeneratedAccess_java_io;
import bsh.cn1.gen.GeneratedAccess_java_lang;
import bsh.cn1.gen.GeneratedAccess_java_lang_ref;
import bsh.cn1.gen.GeneratedAccess_java_lang_reflect;
import bsh.cn1.gen.GeneratedAccess_java_net;
import bsh.cn1.gen.GeneratedAccess_java_nio_charset;
import bsh.cn1.gen.GeneratedAccess_java_text;
import bsh.cn1.gen.GeneratedAccess_java_util;
import bsh.cn1.gen.GeneratedAccess_java_util_concurrent;

/**
 * Generated registry. Re-run tools/generate-cn1-access-registry.sh after updating the CN1 sources.
 */
public final class GeneratedCN1Access implements CN1Access {
    public static final GeneratedCN1Access INSTANCE = new GeneratedCN1Access();

    private static final Map<String, Class<?>> CLASS_INDEX = buildClassIndex();

    private static Map<String, Class<?>> buildClassIndex() {
        Map<String, Class<?>> index = new LinkedHashMap<String, Class<?>>();
        fillClassIndex0(index);
        fillClassIndex1(index);
        fillClassIndex2(index);
        fillClassIndex3(index);
        fillClassIndex4(index);
        fillClassIndex5(index);
        fillClassIndex6(index);
        fillClassIndex7(index);
        fillClassIndex8(index);
        fillClassIndex9(index);
        fillClassIndex10(index);
        return index;
    }

    private static void fillClassIndex0(Map<String, Class<?>> index) {
        index.put("com.codename1.ads.AdsService", com.codename1.ads.AdsService.class);
        index.put("com.codename1.ads.InnerActive", com.codename1.ads.InnerActive.class);
        index.put("com.codename1.analytics.AnalyticsService", com.codename1.analytics.AnalyticsService.class);
        index.put("com.codename1.annotations.Async", com.codename1.annotations.Async.class);
        index.put("com.codename1.background.BackgroundFetch", com.codename1.background.BackgroundFetch.class);
        index.put("com.codename1.capture.Capture", com.codename1.capture.Capture.class);
        index.put("com.codename1.capture.VideoCaptureConstraints", com.codename1.capture.VideoCaptureConstraints.class);
        index.put("com.codename1.charts.ChartComponent", com.codename1.charts.ChartComponent.class);
        index.put("com.codename1.charts.ChartUtil", com.codename1.charts.ChartUtil.class);
        index.put("com.codename1.charts.compat.Canvas", com.codename1.charts.compat.Canvas.class);
        index.put("com.codename1.charts.compat.GradientDrawable", com.codename1.charts.compat.GradientDrawable.class);
        index.put("com.codename1.charts.compat.Paint", com.codename1.charts.compat.Paint.class);
        index.put("com.codename1.charts.compat.PathMeasure", com.codename1.charts.compat.PathMeasure.class);
        index.put("com.codename1.charts.models.AreaSeries", com.codename1.charts.models.AreaSeries.class);
        index.put("com.codename1.charts.models.CategorySeries", com.codename1.charts.models.CategorySeries.class);
        index.put("com.codename1.charts.models.MultipleCategorySeries", com.codename1.charts.models.MultipleCategorySeries.class);
        index.put("com.codename1.charts.models.Point", com.codename1.charts.models.Point.class);
        index.put("com.codename1.charts.models.RangeCategorySeries", com.codename1.charts.models.RangeCategorySeries.class);
        index.put("com.codename1.charts.models.SeriesSelection", com.codename1.charts.models.SeriesSelection.class);
        index.put("com.codename1.charts.models.TimeSeries", com.codename1.charts.models.TimeSeries.class);
        index.put("com.codename1.charts.models.XYMultipleSeriesDataset", com.codename1.charts.models.XYMultipleSeriesDataset.class);
        index.put("com.codename1.charts.models.XYSeries", com.codename1.charts.models.XYSeries.class);
        index.put("com.codename1.charts.models.XYValueSeries", com.codename1.charts.models.XYValueSeries.class);
        index.put("com.codename1.charts.renderers.BasicStroke", com.codename1.charts.renderers.BasicStroke.class);
        index.put("com.codename1.charts.renderers.DefaultRenderer", com.codename1.charts.renderers.DefaultRenderer.class);
        index.put("com.codename1.charts.renderers.DialRenderer", com.codename1.charts.renderers.DialRenderer.class);
        index.put("com.codename1.charts.renderers.SimpleSeriesRenderer", com.codename1.charts.renderers.SimpleSeriesRenderer.class);
        index.put("com.codename1.charts.renderers.XYMultipleSeriesRenderer", com.codename1.charts.renderers.XYMultipleSeriesRenderer.class);
        index.put("com.codename1.charts.renderers.XYSeriesRenderer", com.codename1.charts.renderers.XYSeriesRenderer.class);
        index.put("com.codename1.charts.transitions.SeriesTransition", com.codename1.charts.transitions.SeriesTransition.class);
        index.put("com.codename1.charts.transitions.XYMultiSeriesTransition", com.codename1.charts.transitions.XYMultiSeriesTransition.class);
        index.put("com.codename1.charts.transitions.XYSeriesTransition", com.codename1.charts.transitions.XYSeriesTransition.class);
        index.put("com.codename1.charts.transitions.XYValueSeriesTransition", com.codename1.charts.transitions.XYValueSeriesTransition.class);
        index.put("com.codename1.charts.util.ColorUtil", com.codename1.charts.util.ColorUtil.class);
        index.put("com.codename1.charts.util.MathHelper", com.codename1.charts.util.MathHelper.class);
        index.put("com.codename1.charts.util.NumberFormat", com.codename1.charts.util.NumberFormat.class);
        index.put("com.codename1.charts.views.AbstractChart", com.codename1.charts.views.AbstractChart.class);
        index.put("com.codename1.charts.views.BarChart", com.codename1.charts.views.BarChart.class);
        index.put("com.codename1.charts.views.BubbleChart", com.codename1.charts.views.BubbleChart.class);
        index.put("com.codename1.charts.views.ClickableArea", com.codename1.charts.views.ClickableArea.class);
        index.put("com.codename1.charts.views.CombinedXYChart", com.codename1.charts.views.CombinedXYChart.class);
        index.put("com.codename1.charts.views.CubicLineChart", com.codename1.charts.views.CubicLineChart.class);
        index.put("com.codename1.charts.views.DialChart", com.codename1.charts.views.DialChart.class);
        index.put("com.codename1.charts.views.DoughnutChart", com.codename1.charts.views.DoughnutChart.class);
        index.put("com.codename1.charts.views.LineChart", com.codename1.charts.views.LineChart.class);
        index.put("com.codename1.charts.views.PieChart", com.codename1.charts.views.PieChart.class);
        index.put("com.codename1.charts.views.PieMapper", com.codename1.charts.views.PieMapper.class);
        index.put("com.codename1.charts.views.PieSegment", com.codename1.charts.views.PieSegment.class);
        index.put("com.codename1.charts.views.PointStyle", com.codename1.charts.views.PointStyle.class);
        index.put("com.codename1.charts.views.RadarChart", com.codename1.charts.views.RadarChart.class);
        index.put("com.codename1.charts.views.RangeBarChart", com.codename1.charts.views.RangeBarChart.class);
        index.put("com.codename1.charts.views.RangeStackedBarChart", com.codename1.charts.views.RangeStackedBarChart.class);
        index.put("com.codename1.charts.views.RoundChart", com.codename1.charts.views.RoundChart.class);
        index.put("com.codename1.charts.views.ScatterChart", com.codename1.charts.views.ScatterChart.class);
        index.put("com.codename1.charts.views.TimeChart", com.codename1.charts.views.TimeChart.class);
        index.put("com.codename1.charts.views.XYChart", com.codename1.charts.views.XYChart.class);
        index.put("com.codename1.cloud.BindTarget", com.codename1.cloud.BindTarget.class);
        index.put("com.codename1.codescan.CodeScanner", com.codename1.codescan.CodeScanner.class);
        index.put("com.codename1.codescan.ScanResult", com.codename1.codescan.ScanResult.class);
        index.put("com.codename1.compat.java.util.Objects", com.codename1.compat.java.util.Objects.class);
        index.put("com.codename1.components.Accordion", com.codename1.components.Accordion.class);
        index.put("com.codename1.components.Ads", com.codename1.components.Ads.class);
        index.put("com.codename1.components.AudioRecorderComponent", com.codename1.components.AudioRecorderComponent.class);
        index.put("com.codename1.components.ButtonList", com.codename1.components.ButtonList.class);
    }

    private static void fillClassIndex1(Map<String, Class<?>> index) {
        index.put("com.codename1.components.CheckBoxList", com.codename1.components.CheckBoxList.class);
        index.put("com.codename1.components.ClearableTextField", com.codename1.components.ClearableTextField.class);
        index.put("com.codename1.components.FileEncodedImage", com.codename1.components.FileEncodedImage.class);
        index.put("com.codename1.components.FileEncodedImageAsync", com.codename1.components.FileEncodedImageAsync.class);
        index.put("com.codename1.components.FileTree", com.codename1.components.FileTree.class);
        index.put("com.codename1.components.FileTreeModel", com.codename1.components.FileTreeModel.class);
        index.put("com.codename1.components.FloatingActionButton", com.codename1.components.FloatingActionButton.class);
        index.put("com.codename1.components.FloatingHint", com.codename1.components.FloatingHint.class);
        index.put("com.codename1.components.ImageViewer", com.codename1.components.ImageViewer.class);
        index.put("com.codename1.components.InfiniteProgress", com.codename1.components.InfiniteProgress.class);
        index.put("com.codename1.components.InfiniteScrollAdapter", com.codename1.components.InfiniteScrollAdapter.class);
        index.put("com.codename1.components.InteractionDialog", com.codename1.components.InteractionDialog.class);
        index.put("com.codename1.components.MasterDetail", com.codename1.components.MasterDetail.class);
        index.put("com.codename1.components.MediaPlayer", com.codename1.components.MediaPlayer.class);
        index.put("com.codename1.components.MultiButton", com.codename1.components.MultiButton.class);
        index.put("com.codename1.components.OnOffSwitch", com.codename1.components.OnOffSwitch.class);
        index.put("com.codename1.components.Progress", com.codename1.components.Progress.class);
        index.put("com.codename1.components.RSSReader", com.codename1.components.RSSReader.class);
        index.put("com.codename1.components.RadioButtonList", com.codename1.components.RadioButtonList.class);
        index.put("com.codename1.components.ReplaceableImage", com.codename1.components.ReplaceableImage.class);
        index.put("com.codename1.components.ScaleImageButton", com.codename1.components.ScaleImageButton.class);
        index.put("com.codename1.components.ScaleImageLabel", com.codename1.components.ScaleImageLabel.class);
        index.put("com.codename1.components.ShareButton", com.codename1.components.ShareButton.class);
        index.put("com.codename1.components.SignatureComponent", com.codename1.components.SignatureComponent.class);
        index.put("com.codename1.components.SliderBridge", com.codename1.components.SliderBridge.class);
        index.put("com.codename1.components.SpanButton", com.codename1.components.SpanButton.class);
        index.put("com.codename1.components.SpanLabel", com.codename1.components.SpanLabel.class);
        index.put("com.codename1.components.SpanMultiButton", com.codename1.components.SpanMultiButton.class);
        index.put("com.codename1.components.SplitPane", com.codename1.components.SplitPane.class);
        index.put("com.codename1.components.StorageImage", com.codename1.components.StorageImage.class);
        index.put("com.codename1.components.StorageImageAsync", com.codename1.components.StorageImageAsync.class);
        index.put("com.codename1.components.Switch", com.codename1.components.Switch.class);
        index.put("com.codename1.components.SwitchList", com.codename1.components.SwitchList.class);
        index.put("com.codename1.components.ToastBar", com.codename1.components.ToastBar.class);
        index.put("com.codename1.components.WebBrowser", com.codename1.components.WebBrowser.class);
        index.put("com.codename1.contacts.Address", com.codename1.contacts.Address.class);
        index.put("com.codename1.contacts.Contact", com.codename1.contacts.Contact.class);
        index.put("com.codename1.contacts.ContactsManager", com.codename1.contacts.ContactsManager.class);
        index.put("com.codename1.contacts.ContactsModel", com.codename1.contacts.ContactsModel.class);
        index.put("com.codename1.db.Cursor", com.codename1.db.Cursor.class);
        index.put("com.codename1.db.Database", com.codename1.db.Database.class);
        index.put("com.codename1.db.Row", com.codename1.db.Row.class);
        index.put("com.codename1.db.RowExt", com.codename1.db.RowExt.class);
        index.put("com.codename1.db.ThreadSafeDatabase", com.codename1.db.ThreadSafeDatabase.class);
        index.put("com.codename1.facebook.Album", com.codename1.facebook.Album.class);
        index.put("com.codename1.facebook.FBObject", com.codename1.facebook.FBObject.class);
        index.put("com.codename1.facebook.FaceBookAccess", com.codename1.facebook.FaceBookAccess.class);
        index.put("com.codename1.facebook.Page", com.codename1.facebook.Page.class);
        index.put("com.codename1.facebook.Photo", com.codename1.facebook.Photo.class);
        index.put("com.codename1.facebook.Post", com.codename1.facebook.Post.class);
        index.put("com.codename1.facebook.User", com.codename1.facebook.User.class);
        index.put("com.codename1.facebook.ui.LikeButton", com.codename1.facebook.ui.LikeButton.class);
        index.put("com.codename1.impl.CodenameOneImplementation", com.codename1.impl.CodenameOneImplementation.class);
        index.put("com.codename1.impl.CodenameOneThread", com.codename1.impl.CodenameOneThread.class);
        index.put("com.codename1.impl.FullScreenAdService", com.codename1.impl.FullScreenAdService.class);
        index.put("com.codename1.impl.VServAds", com.codename1.impl.VServAds.class);
        index.put("com.codename1.impl.VirtualKeyboardInterface", com.codename1.impl.VirtualKeyboardInterface.class);
        index.put("com.codename1.io.AccessToken", com.codename1.io.AccessToken.class);
        index.put("com.codename1.io.BufferedInputStream", com.codename1.io.BufferedInputStream.class);
        index.put("com.codename1.io.BufferedOutputStream", com.codename1.io.BufferedOutputStream.class);
        index.put("com.codename1.io.CSVParser", com.codename1.io.CSVParser.class);
        index.put("com.codename1.io.CacheMap", com.codename1.io.CacheMap.class);
        index.put("com.codename1.io.CharArrayReader", com.codename1.io.CharArrayReader.class);
        index.put("com.codename1.io.ConnectionRequest", com.codename1.io.ConnectionRequest.class);
    }

    private static void fillClassIndex2(Map<String, Class<?>> index) {
        index.put("com.codename1.io.Cookie", com.codename1.io.Cookie.class);
        index.put("com.codename1.io.Data", com.codename1.io.Data.class);
        index.put("com.codename1.io.Externalizable", com.codename1.io.Externalizable.class);
        index.put("com.codename1.io.File", com.codename1.io.File.class);
        index.put("com.codename1.io.FileSystemStorage", com.codename1.io.FileSystemStorage.class);
        index.put("com.codename1.io.IOProgressListener", com.codename1.io.IOProgressListener.class);
        index.put("com.codename1.io.JSONParseCallback", com.codename1.io.JSONParseCallback.class);
        index.put("com.codename1.io.JSONParser", com.codename1.io.JSONParser.class);
        index.put("com.codename1.io.Log", com.codename1.io.Log.class);
        index.put("com.codename1.io.MalformedURLException", com.codename1.io.MalformedURLException.class);
        index.put("com.codename1.io.MultipartRequest", com.codename1.io.MultipartRequest.class);
        index.put("com.codename1.io.NetworkEvent", com.codename1.io.NetworkEvent.class);
        index.put("com.codename1.io.NetworkManager", com.codename1.io.NetworkManager.class);
        index.put("com.codename1.io.Oauth2", com.codename1.io.Oauth2.class);
        index.put("com.codename1.io.PreferenceListener", com.codename1.io.PreferenceListener.class);
        index.put("com.codename1.io.Preferences", com.codename1.io.Preferences.class);
        index.put("com.codename1.io.Properties", com.codename1.io.Properties.class);
        index.put("com.codename1.io.Socket", com.codename1.io.Socket.class);
        index.put("com.codename1.io.SocketConnection", com.codename1.io.SocketConnection.class);
        index.put("com.codename1.io.Storage", com.codename1.io.Storage.class);
        index.put("com.codename1.io.URL", com.codename1.io.URL.class);
        index.put("com.codename1.io.Util", com.codename1.io.Util.class);
        index.put("com.codename1.io.WebServiceProxyCall", com.codename1.io.WebServiceProxyCall.class);
        index.put("com.codename1.io.gzip.Adler32", com.codename1.io.gzip.Adler32.class);
        index.put("com.codename1.io.gzip.CRC32", com.codename1.io.gzip.CRC32.class);
        index.put("com.codename1.io.gzip.Deflate", com.codename1.io.gzip.Deflate.class);
        index.put("com.codename1.io.gzip.Deflater", com.codename1.io.gzip.Deflater.class);
        index.put("com.codename1.io.gzip.DeflaterOutputStream", com.codename1.io.gzip.DeflaterOutputStream.class);
        index.put("com.codename1.io.gzip.FilterInputStream", com.codename1.io.gzip.FilterInputStream.class);
        index.put("com.codename1.io.gzip.FilterOutputStream", com.codename1.io.gzip.FilterOutputStream.class);
        index.put("com.codename1.io.gzip.GZConnectionRequest", com.codename1.io.gzip.GZConnectionRequest.class);
        index.put("com.codename1.io.gzip.GZIPException", com.codename1.io.gzip.GZIPException.class);
        index.put("com.codename1.io.gzip.GZIPHeader", com.codename1.io.gzip.GZIPHeader.class);
        index.put("com.codename1.io.gzip.GZIPInputStream", com.codename1.io.gzip.GZIPInputStream.class);
        index.put("com.codename1.io.gzip.GZIPOutputStream", com.codename1.io.gzip.GZIPOutputStream.class);
        index.put("com.codename1.io.gzip.Inflater", com.codename1.io.gzip.Inflater.class);
        index.put("com.codename1.io.gzip.InflaterInputStream", com.codename1.io.gzip.InflaterInputStream.class);
        index.put("com.codename1.io.gzip.JZlib", com.codename1.io.gzip.JZlib.class);
        index.put("com.codename1.io.gzip.ZStream", com.codename1.io.gzip.ZStream.class);
        index.put("com.codename1.io.rest.ErrorCodeHandler", com.codename1.io.rest.ErrorCodeHandler.class);
        index.put("com.codename1.io.rest.RequestBuilder", com.codename1.io.rest.RequestBuilder.class);
        index.put("com.codename1.io.rest.Response", com.codename1.io.rest.Response.class);
        index.put("com.codename1.io.rest.Rest", com.codename1.io.rest.Rest.class);
        index.put("com.codename1.io.services.CachedData", com.codename1.io.services.CachedData.class);
        index.put("com.codename1.io.services.CachedDataService", com.codename1.io.services.CachedDataService.class);
        index.put("com.codename1.io.services.ImageDownloadService", com.codename1.io.services.ImageDownloadService.class);
        index.put("com.codename1.io.services.RSSService", com.codename1.io.services.RSSService.class);
        index.put("com.codename1.io.services.TwitterRESTService", com.codename1.io.services.TwitterRESTService.class);
        index.put("com.codename1.io.tar.Octal", com.codename1.io.tar.Octal.class);
        index.put("com.codename1.io.tar.TarConstants", com.codename1.io.tar.TarConstants.class);
        index.put("com.codename1.io.tar.TarEntry", com.codename1.io.tar.TarEntry.class);
        index.put("com.codename1.io.tar.TarHeader", com.codename1.io.tar.TarHeader.class);
        index.put("com.codename1.io.tar.TarInputStream", com.codename1.io.tar.TarInputStream.class);
        index.put("com.codename1.io.tar.TarOutputStream", com.codename1.io.tar.TarOutputStream.class);
        index.put("com.codename1.io.tar.TarUtils", com.codename1.io.tar.TarUtils.class);
        index.put("com.codename1.javascript.JSFunction", com.codename1.javascript.JSFunction.class);
        index.put("com.codename1.javascript.JSObject", com.codename1.javascript.JSObject.class);
        index.put("com.codename1.javascript.JavascriptContext", com.codename1.javascript.JavascriptContext.class);
        index.put("com.codename1.l10n.DateFormat", com.codename1.l10n.DateFormat.class);
        index.put("com.codename1.l10n.DateFormatPatterns", com.codename1.l10n.DateFormatPatterns.class);
        index.put("com.codename1.l10n.DateFormatSymbols", com.codename1.l10n.DateFormatSymbols.class);
        index.put("com.codename1.l10n.Format", com.codename1.l10n.Format.class);
        index.put("com.codename1.l10n.L10NManager", com.codename1.l10n.L10NManager.class);
        index.put("com.codename1.l10n.ParseException", com.codename1.l10n.ParseException.class);
    }

    private static void fillClassIndex3(Map<String, Class<?>> index) {
        index.put("com.codename1.l10n.SimpleDateFormat", com.codename1.l10n.SimpleDateFormat.class);
        index.put("com.codename1.location.Geofence", com.codename1.location.Geofence.class);
        index.put("com.codename1.location.GeofenceListener", com.codename1.location.GeofenceListener.class);
        index.put("com.codename1.location.GeofenceManager", com.codename1.location.GeofenceManager.class);
        index.put("com.codename1.location.Location", com.codename1.location.Location.class);
        index.put("com.codename1.location.LocationListener", com.codename1.location.LocationListener.class);
        index.put("com.codename1.location.LocationManager", com.codename1.location.LocationManager.class);
        index.put("com.codename1.location.LocationRequest", com.codename1.location.LocationRequest.class);
        index.put("com.codename1.maps.BoundingBox", com.codename1.maps.BoundingBox.class);
        index.put("com.codename1.maps.Coord", com.codename1.maps.Coord.class);
        index.put("com.codename1.maps.MapComponent", com.codename1.maps.MapComponent.class);
        index.put("com.codename1.maps.MapListener", com.codename1.maps.MapListener.class);
        index.put("com.codename1.maps.Mercator", com.codename1.maps.Mercator.class);
        index.put("com.codename1.maps.Projection", com.codename1.maps.Projection.class);
        index.put("com.codename1.maps.ProxyHttpTile", com.codename1.maps.ProxyHttpTile.class);
        index.put("com.codename1.maps.Tile", com.codename1.maps.Tile.class);
        index.put("com.codename1.maps.layers.AbstractLayer", com.codename1.maps.layers.AbstractLayer.class);
        index.put("com.codename1.maps.layers.ArrowLinesLayer", com.codename1.maps.layers.ArrowLinesLayer.class);
        index.put("com.codename1.maps.layers.Layer", com.codename1.maps.layers.Layer.class);
        index.put("com.codename1.maps.layers.LinesLayer", com.codename1.maps.layers.LinesLayer.class);
        index.put("com.codename1.maps.layers.PointLayer", com.codename1.maps.layers.PointLayer.class);
        index.put("com.codename1.maps.layers.PointsLayer", com.codename1.maps.layers.PointsLayer.class);
        index.put("com.codename1.maps.providers.GoogleMapsProvider", com.codename1.maps.providers.GoogleMapsProvider.class);
        index.put("com.codename1.maps.providers.MapProvider", com.codename1.maps.providers.MapProvider.class);
        index.put("com.codename1.maps.providers.OpenStreetMapProvider", com.codename1.maps.providers.OpenStreetMapProvider.class);
        index.put("com.codename1.maps.providers.TiledProvider", com.codename1.maps.providers.TiledProvider.class);
        index.put("com.codename1.media.AbstractMedia", com.codename1.media.AbstractMedia.class);
        index.put("com.codename1.media.AsyncMedia", com.codename1.media.AsyncMedia.class);
        index.put("com.codename1.media.AudioBuffer", com.codename1.media.AudioBuffer.class);
        index.put("com.codename1.media.Media", com.codename1.media.Media.class);
        index.put("com.codename1.media.MediaManager", com.codename1.media.MediaManager.class);
        index.put("com.codename1.media.MediaMetaData", com.codename1.media.MediaMetaData.class);
        index.put("com.codename1.media.MediaRecorderBuilder", com.codename1.media.MediaRecorderBuilder.class);
        index.put("com.codename1.media.RemoteControlListener", com.codename1.media.RemoteControlListener.class);
        index.put("com.codename1.media.WAVWriter", com.codename1.media.WAVWriter.class);
        index.put("com.codename1.messaging.Message", com.codename1.messaging.Message.class);
        index.put("com.codename1.notifications.LocalNotification", com.codename1.notifications.LocalNotification.class);
        index.put("com.codename1.notifications.LocalNotificationCallback", com.codename1.notifications.LocalNotificationCallback.class);
        index.put("com.codename1.payment.ApplePromotionalOffer", com.codename1.payment.ApplePromotionalOffer.class);
        index.put("com.codename1.payment.PendingPurchaseCallback", com.codename1.payment.PendingPurchaseCallback.class);
        index.put("com.codename1.payment.Product", com.codename1.payment.Product.class);
        index.put("com.codename1.payment.PromotionalOffer", com.codename1.payment.PromotionalOffer.class);
        index.put("com.codename1.payment.Purchase", com.codename1.payment.Purchase.class);
        index.put("com.codename1.payment.PurchaseCallback", com.codename1.payment.PurchaseCallback.class);
        index.put("com.codename1.payment.Receipt", com.codename1.payment.Receipt.class);
        index.put("com.codename1.payment.ReceiptStore", com.codename1.payment.ReceiptStore.class);
        index.put("com.codename1.payment.RestoreCallback", com.codename1.payment.RestoreCallback.class);
        index.put("com.codename1.plugin.Plugin", com.codename1.plugin.Plugin.class);
        index.put("com.codename1.plugin.PluginSupport", com.codename1.plugin.PluginSupport.class);
        index.put("com.codename1.plugin.event.IsGalleryTypeSupportedEvent", com.codename1.plugin.event.IsGalleryTypeSupportedEvent.class);
        index.put("com.codename1.plugin.event.OpenGalleryEvent", com.codename1.plugin.event.OpenGalleryEvent.class);
        index.put("com.codename1.plugin.event.PluginEvent", com.codename1.plugin.event.PluginEvent.class);
        index.put("com.codename1.processing.Result", com.codename1.processing.Result.class);
        index.put("com.codename1.properties.BooleanProperty", com.codename1.properties.BooleanProperty.class);
        index.put("com.codename1.properties.ByteProperty", com.codename1.properties.ByteProperty.class);
        index.put("com.codename1.properties.CharProperty", com.codename1.properties.CharProperty.class);
        index.put("com.codename1.properties.CollectionProperty", com.codename1.properties.CollectionProperty.class);
        index.put("com.codename1.properties.DoubleProperty", com.codename1.properties.DoubleProperty.class);
        index.put("com.codename1.properties.FloatProperty", com.codename1.properties.FloatProperty.class);
        index.put("com.codename1.properties.InstantUI", com.codename1.properties.InstantUI.class);
        index.put("com.codename1.properties.IntProperty", com.codename1.properties.IntProperty.class);
        index.put("com.codename1.properties.ListProperty", com.codename1.properties.ListProperty.class);
        index.put("com.codename1.properties.LongProperty", com.codename1.properties.LongProperty.class);
        index.put("com.codename1.properties.MapAdapter", com.codename1.properties.MapAdapter.class);
    }

    private static void fillClassIndex4(Map<String, Class<?>> index) {
        index.put("com.codename1.properties.MapProperty", com.codename1.properties.MapProperty.class);
        index.put("com.codename1.properties.NumericProperty", com.codename1.properties.NumericProperty.class);
        index.put("com.codename1.properties.PreferencesObject", com.codename1.properties.PreferencesObject.class);
        index.put("com.codename1.properties.Property", com.codename1.properties.Property.class);
        index.put("com.codename1.properties.PropertyBase", com.codename1.properties.PropertyBase.class);
        index.put("com.codename1.properties.PropertyBusinessObject", com.codename1.properties.PropertyBusinessObject.class);
        index.put("com.codename1.properties.PropertyChangeListener", com.codename1.properties.PropertyChangeListener.class);
        index.put("com.codename1.properties.PropertyIndex", com.codename1.properties.PropertyIndex.class);
        index.put("com.codename1.properties.SQLMap", com.codename1.properties.SQLMap.class);
        index.put("com.codename1.properties.SetProperty", com.codename1.properties.SetProperty.class);
        index.put("com.codename1.properties.UiBinding", com.codename1.properties.UiBinding.class);
        index.put("com.codename1.push.Push", com.codename1.push.Push.class);
        index.put("com.codename1.push.PushAction", com.codename1.push.PushAction.class);
        index.put("com.codename1.push.PushActionCategory", com.codename1.push.PushActionCategory.class);
        index.put("com.codename1.push.PushActionsProvider", com.codename1.push.PushActionsProvider.class);
        index.put("com.codename1.push.PushBuilder", com.codename1.push.PushBuilder.class);
        index.put("com.codename1.push.PushCallback", com.codename1.push.PushCallback.class);
        index.put("com.codename1.push.PushContent", com.codename1.push.PushContent.class);
        index.put("com.codename1.share.EmailShare", com.codename1.share.EmailShare.class);
        index.put("com.codename1.share.FacebookShare", com.codename1.share.FacebookShare.class);
        index.put("com.codename1.share.SMSShare", com.codename1.share.SMSShare.class);
        index.put("com.codename1.share.ShareService", com.codename1.share.ShareService.class);
        index.put("com.codename1.social.FacebookConnect", com.codename1.social.FacebookConnect.class);
        index.put("com.codename1.social.GoogleConnect", com.codename1.social.GoogleConnect.class);
        index.put("com.codename1.social.Login", com.codename1.social.Login.class);
        index.put("com.codename1.social.LoginCallback", com.codename1.social.LoginCallback.class);
        index.put("com.codename1.system.CrashReport", com.codename1.system.CrashReport.class);
        index.put("com.codename1.system.DefaultCrashReporter", com.codename1.system.DefaultCrashReporter.class);
        index.put("com.codename1.system.Lifecycle", com.codename1.system.Lifecycle.class);
        index.put("com.codename1.system.NativeInterface", com.codename1.system.NativeInterface.class);
        index.put("com.codename1.system.NativeLookup", com.codename1.system.NativeLookup.class);
        index.put("com.codename1.system.URLCallback", com.codename1.system.URLCallback.class);
        index.put("com.codename1.testing.AbstractTest", com.codename1.testing.AbstractTest.class);
        index.put("com.codename1.testing.DeviceRunner", com.codename1.testing.DeviceRunner.class);
        index.put("com.codename1.testing.TestReporting", com.codename1.testing.TestReporting.class);
        index.put("com.codename1.testing.TestRunnerComponent", com.codename1.testing.TestRunnerComponent.class);
        index.put("com.codename1.testing.TestUtils", com.codename1.testing.TestUtils.class);
        index.put("com.codename1.testing.UnitTest", com.codename1.testing.UnitTest.class);
        index.put("com.codename1.ui.AnimationManager", com.codename1.ui.AnimationManager.class);
        index.put("com.codename1.ui.AutoCompleteTextComponent", com.codename1.ui.AutoCompleteTextComponent.class);
        index.put("com.codename1.ui.AutoCompleteTextField", com.codename1.ui.AutoCompleteTextField.class);
        index.put("com.codename1.ui.BlockingDisallowedException", com.codename1.ui.BlockingDisallowedException.class);
        index.put("com.codename1.ui.BrowserComponent", com.codename1.ui.BrowserComponent.class);
        index.put("com.codename1.ui.BrowserWindow", com.codename1.ui.BrowserWindow.class);
        index.put("com.codename1.ui.Button", com.codename1.ui.Button.class);
        index.put("com.codename1.ui.ButtonGroup", com.codename1.ui.ButtonGroup.class);
        index.put("com.codename1.ui.CN", com.codename1.ui.CN.class);
        index.put("com.codename1.ui.CN1Constants", com.codename1.ui.CN1Constants.class);
        index.put("com.codename1.ui.Calendar", com.codename1.ui.Calendar.class);
        index.put("com.codename1.ui.CheckBox", com.codename1.ui.CheckBox.class);
        index.put("com.codename1.ui.ComboBox", com.codename1.ui.ComboBox.class);
        index.put("com.codename1.ui.Command", com.codename1.ui.Command.class);
        index.put("com.codename1.ui.CommonProgressAnimations", com.codename1.ui.CommonProgressAnimations.class);
        index.put("com.codename1.ui.Component", com.codename1.ui.Component.class);
        index.put("com.codename1.ui.ComponentGroup", com.codename1.ui.ComponentGroup.class);
        index.put("com.codename1.ui.ComponentImage", com.codename1.ui.ComponentImage.class);
        index.put("com.codename1.ui.ComponentSelector", com.codename1.ui.ComponentSelector.class);
        index.put("com.codename1.ui.Container", com.codename1.ui.Container.class);
        index.put("com.codename1.ui.Dialog", com.codename1.ui.Dialog.class);
        index.put("com.codename1.ui.Display", com.codename1.ui.Display.class);
        index.put("com.codename1.ui.DynamicImage", com.codename1.ui.DynamicImage.class);
        index.put("com.codename1.ui.Editable", com.codename1.ui.Editable.class);
        index.put("com.codename1.ui.EncodedImage", com.codename1.ui.EncodedImage.class);
        index.put("com.codename1.ui.Font", com.codename1.ui.Font.class);
    }

    private static void fillClassIndex5(Map<String, Class<?>> index) {
        index.put("com.codename1.ui.FontImage", com.codename1.ui.FontImage.class);
        index.put("com.codename1.ui.Form", com.codename1.ui.Form.class);
        index.put("com.codename1.ui.Graphics", com.codename1.ui.Graphics.class);
        index.put("com.codename1.ui.IconHolder", com.codename1.ui.IconHolder.class);
        index.put("com.codename1.ui.Image", com.codename1.ui.Image.class);
        index.put("com.codename1.ui.ImageFactory", com.codename1.ui.ImageFactory.class);
        index.put("com.codename1.ui.InfiniteContainer", com.codename1.ui.InfiniteContainer.class);
        index.put("com.codename1.ui.InputComponent", com.codename1.ui.InputComponent.class);
        index.put("com.codename1.ui.InterFormContainer", com.codename1.ui.InterFormContainer.class);
        index.put("com.codename1.ui.Label", com.codename1.ui.Label.class);
        index.put("com.codename1.ui.LinearGradientPaint", com.codename1.ui.LinearGradientPaint.class);
        index.put("com.codename1.ui.List", com.codename1.ui.List.class);
        index.put("com.codename1.ui.MenuBar", com.codename1.ui.MenuBar.class);
        index.put("com.codename1.ui.MultipleGradientPaint", com.codename1.ui.MultipleGradientPaint.class);
        index.put("com.codename1.ui.NavigationCommand", com.codename1.ui.NavigationCommand.class);
        index.put("com.codename1.ui.Paint", com.codename1.ui.Paint.class);
        index.put("com.codename1.ui.Painter", com.codename1.ui.Painter.class);
        index.put("com.codename1.ui.PeerComponent", com.codename1.ui.PeerComponent.class);
        index.put("com.codename1.ui.PickerComponent", com.codename1.ui.PickerComponent.class);
        index.put("com.codename1.ui.RGBImage", com.codename1.ui.RGBImage.class);
        index.put("com.codename1.ui.RadioButton", com.codename1.ui.RadioButton.class);
        index.put("com.codename1.ui.ReleasableComponent", com.codename1.ui.ReleasableComponent.class);
        index.put("com.codename1.ui.SelectableIconHolder", com.codename1.ui.SelectableIconHolder.class);
        index.put("com.codename1.ui.Sheet", com.codename1.ui.Sheet.class);
        index.put("com.codename1.ui.SideMenuBar", com.codename1.ui.SideMenuBar.class);
        index.put("com.codename1.ui.Slider", com.codename1.ui.Slider.class);
        index.put("com.codename1.ui.Stroke", com.codename1.ui.Stroke.class);
        index.put("com.codename1.ui.SwipeableContainer", com.codename1.ui.SwipeableContainer.class);
        index.put("com.codename1.ui.Tabs", com.codename1.ui.Tabs.class);
        index.put("com.codename1.ui.TextArea", com.codename1.ui.TextArea.class);
        index.put("com.codename1.ui.TextComponent", com.codename1.ui.TextComponent.class);
        index.put("com.codename1.ui.TextComponentPassword", com.codename1.ui.TextComponentPassword.class);
        index.put("com.codename1.ui.TextField", com.codename1.ui.TextField.class);
        index.put("com.codename1.ui.TextHolder", com.codename1.ui.TextHolder.class);
        index.put("com.codename1.ui.TextSelection", com.codename1.ui.TextSelection.class);
        index.put("com.codename1.ui.Toolbar", com.codename1.ui.Toolbar.class);
        index.put("com.codename1.ui.TooltipManager", com.codename1.ui.TooltipManager.class);
        index.put("com.codename1.ui.Transform", com.codename1.ui.Transform.class);
        index.put("com.codename1.ui.UIFragment", com.codename1.ui.UIFragment.class);
        index.put("com.codename1.ui.URLImage", com.codename1.ui.URLImage.class);
        index.put("com.codename1.ui.VirtualInputDevice", com.codename1.ui.VirtualInputDevice.class);
        index.put("com.codename1.ui.animations.Animation", com.codename1.ui.animations.Animation.class);
        index.put("com.codename1.ui.animations.AnimationObject", com.codename1.ui.animations.AnimationObject.class);
        index.put("com.codename1.ui.animations.BubbleTransition", com.codename1.ui.animations.BubbleTransition.class);
        index.put("com.codename1.ui.animations.CommonTransitions", com.codename1.ui.animations.CommonTransitions.class);
        index.put("com.codename1.ui.animations.ComponentAnimation", com.codename1.ui.animations.ComponentAnimation.class);
        index.put("com.codename1.ui.animations.FlipTransition", com.codename1.ui.animations.FlipTransition.class);
        index.put("com.codename1.ui.animations.MorphTransition", com.codename1.ui.animations.MorphTransition.class);
        index.put("com.codename1.ui.animations.Motion", com.codename1.ui.animations.Motion.class);
        index.put("com.codename1.ui.animations.Timeline", com.codename1.ui.animations.Timeline.class);
        index.put("com.codename1.ui.animations.Transition", com.codename1.ui.animations.Transition.class);
        index.put("com.codename1.ui.css.CSSThemeCompiler", com.codename1.ui.css.CSSThemeCompiler.class);
        index.put("com.codename1.ui.events.ActionEvent", com.codename1.ui.events.ActionEvent.class);
        index.put("com.codename1.ui.events.ActionListener", com.codename1.ui.events.ActionListener.class);
        index.put("com.codename1.ui.events.ActionSource", com.codename1.ui.events.ActionSource.class);
        index.put("com.codename1.ui.events.BrowserNavigationCallback", com.codename1.ui.events.BrowserNavigationCallback.class);
        index.put("com.codename1.ui.events.ComponentStateChangeEvent", com.codename1.ui.events.ComponentStateChangeEvent.class);
        index.put("com.codename1.ui.events.DataChangedListener", com.codename1.ui.events.DataChangedListener.class);
        index.put("com.codename1.ui.events.FocusListener", com.codename1.ui.events.FocusListener.class);
        index.put("com.codename1.ui.events.MessageEvent", com.codename1.ui.events.MessageEvent.class);
        index.put("com.codename1.ui.events.ScrollListener", com.codename1.ui.events.ScrollListener.class);
        index.put("com.codename1.ui.events.SelectionListener", com.codename1.ui.events.SelectionListener.class);
        index.put("com.codename1.ui.events.StyleListener", com.codename1.ui.events.StyleListener.class);
        index.put("com.codename1.ui.events.WindowEvent", com.codename1.ui.events.WindowEvent.class);
    }

    private static void fillClassIndex6(Map<String, Class<?>> index) {
        index.put("com.codename1.ui.geom.AffineTransform", com.codename1.ui.geom.AffineTransform.class);
        index.put("com.codename1.ui.geom.Dimension", com.codename1.ui.geom.Dimension.class);
        index.put("com.codename1.ui.geom.Dimension2D", com.codename1.ui.geom.Dimension2D.class);
        index.put("com.codename1.ui.geom.GeneralPath", com.codename1.ui.geom.GeneralPath.class);
        index.put("com.codename1.ui.geom.PathIterator", com.codename1.ui.geom.PathIterator.class);
        index.put("com.codename1.ui.geom.Point", com.codename1.ui.geom.Point.class);
        index.put("com.codename1.ui.geom.Point2D", com.codename1.ui.geom.Point2D.class);
        index.put("com.codename1.ui.geom.Rectangle", com.codename1.ui.geom.Rectangle.class);
        index.put("com.codename1.ui.geom.Rectangle2D", com.codename1.ui.geom.Rectangle2D.class);
        index.put("com.codename1.ui.geom.Shape", com.codename1.ui.geom.Shape.class);
        index.put("com.codename1.ui.html.AsyncDocumentRequestHandler", com.codename1.ui.html.AsyncDocumentRequestHandler.class);
        index.put("com.codename1.ui.html.AsyncDocumentRequestHandlerImpl", com.codename1.ui.html.AsyncDocumentRequestHandlerImpl.class);
        index.put("com.codename1.ui.html.DefaultDocumentRequestHandler", com.codename1.ui.html.DefaultDocumentRequestHandler.class);
        index.put("com.codename1.ui.html.DefaultHTMLCallback", com.codename1.ui.html.DefaultHTMLCallback.class);
        index.put("com.codename1.ui.html.DocumentInfo", com.codename1.ui.html.DocumentInfo.class);
        index.put("com.codename1.ui.html.DocumentRequestHandler", com.codename1.ui.html.DocumentRequestHandler.class);
        index.put("com.codename1.ui.html.HTMLCallback", com.codename1.ui.html.HTMLCallback.class);
        index.put("com.codename1.ui.html.HTMLComponent", com.codename1.ui.html.HTMLComponent.class);
        index.put("com.codename1.ui.html.HTMLElement", com.codename1.ui.html.HTMLElement.class);
        index.put("com.codename1.ui.html.HTMLParser", com.codename1.ui.html.HTMLParser.class);
        index.put("com.codename1.ui.html.HTMLUtils", com.codename1.ui.html.HTMLUtils.class);
        index.put("com.codename1.ui.html.IOCallback", com.codename1.ui.html.IOCallback.class);
        index.put("com.codename1.ui.layouts.BorderLayout", com.codename1.ui.layouts.BorderLayout.class);
        index.put("com.codename1.ui.layouts.BoxLayout", com.codename1.ui.layouts.BoxLayout.class);
        index.put("com.codename1.ui.layouts.CoordinateLayout", com.codename1.ui.layouts.CoordinateLayout.class);
        index.put("com.codename1.ui.layouts.FlowLayout", com.codename1.ui.layouts.FlowLayout.class);
        index.put("com.codename1.ui.layouts.GridBagConstraints", com.codename1.ui.layouts.GridBagConstraints.class);
        index.put("com.codename1.ui.layouts.GridBagLayout", com.codename1.ui.layouts.GridBagLayout.class);
        index.put("com.codename1.ui.layouts.GridLayout", com.codename1.ui.layouts.GridLayout.class);
        index.put("com.codename1.ui.layouts.GroupLayout", com.codename1.ui.layouts.GroupLayout.class);
        index.put("com.codename1.ui.layouts.Insets", com.codename1.ui.layouts.Insets.class);
        index.put("com.codename1.ui.layouts.LayeredLayout", com.codename1.ui.layouts.LayeredLayout.class);
        index.put("com.codename1.ui.layouts.Layout", com.codename1.ui.layouts.Layout.class);
        index.put("com.codename1.ui.layouts.LayoutStyle", com.codename1.ui.layouts.LayoutStyle.class);
        index.put("com.codename1.ui.layouts.TextModeLayout", com.codename1.ui.layouts.TextModeLayout.class);
        index.put("com.codename1.ui.layouts.mig.AC", com.codename1.ui.layouts.mig.AC.class);
        index.put("com.codename1.ui.layouts.mig.BoundSize", com.codename1.ui.layouts.mig.BoundSize.class);
        index.put("com.codename1.ui.layouts.mig.CC", com.codename1.ui.layouts.mig.CC.class);
        index.put("com.codename1.ui.layouts.mig.ComponentWrapper", com.codename1.ui.layouts.mig.ComponentWrapper.class);
        index.put("com.codename1.ui.layouts.mig.ConstraintParser", com.codename1.ui.layouts.mig.ConstraintParser.class);
        index.put("com.codename1.ui.layouts.mig.ContainerWrapper", com.codename1.ui.layouts.mig.ContainerWrapper.class);
        index.put("com.codename1.ui.layouts.mig.DimConstraint", com.codename1.ui.layouts.mig.DimConstraint.class);
        index.put("com.codename1.ui.layouts.mig.Grid", com.codename1.ui.layouts.mig.Grid.class);
        index.put("com.codename1.ui.layouts.mig.InCellGapProvider", com.codename1.ui.layouts.mig.InCellGapProvider.class);
        index.put("com.codename1.ui.layouts.mig.LC", com.codename1.ui.layouts.mig.LC.class);
        index.put("com.codename1.ui.layouts.mig.LayoutCallback", com.codename1.ui.layouts.mig.LayoutCallback.class);
        index.put("com.codename1.ui.layouts.mig.LayoutUtil", com.codename1.ui.layouts.mig.LayoutUtil.class);
        index.put("com.codename1.ui.layouts.mig.LinkHandler", com.codename1.ui.layouts.mig.LinkHandler.class);
        index.put("com.codename1.ui.layouts.mig.MigLayout", com.codename1.ui.layouts.mig.MigLayout.class);
        index.put("com.codename1.ui.layouts.mig.PlatformDefaults", com.codename1.ui.layouts.mig.PlatformDefaults.class);
        index.put("com.codename1.ui.layouts.mig.UnitConverter", com.codename1.ui.layouts.mig.UnitConverter.class);
        index.put("com.codename1.ui.layouts.mig.UnitValue", com.codename1.ui.layouts.mig.UnitValue.class);
        index.put("com.codename1.ui.list.CellRenderer", com.codename1.ui.list.CellRenderer.class);
        index.put("com.codename1.ui.list.ContainerList", com.codename1.ui.list.ContainerList.class);
        index.put("com.codename1.ui.list.DefaultListCellRenderer", com.codename1.ui.list.DefaultListCellRenderer.class);
        index.put("com.codename1.ui.list.DefaultListModel", com.codename1.ui.list.DefaultListModel.class);
        index.put("com.codename1.ui.list.FilterProxyListModel", com.codename1.ui.list.FilterProxyListModel.class);
        index.put("com.codename1.ui.list.GenericListCellRenderer", com.codename1.ui.list.GenericListCellRenderer.class);
        index.put("com.codename1.ui.list.ListCellRenderer", com.codename1.ui.list.ListCellRenderer.class);
        index.put("com.codename1.ui.list.ListModel", com.codename1.ui.list.ListModel.class);
        index.put("com.codename1.ui.list.MultiList", com.codename1.ui.list.MultiList.class);
        index.put("com.codename1.ui.list.MultipleSelectionListModel", com.codename1.ui.list.MultipleSelectionListModel.class);
        index.put("com.codename1.ui.painter.BackgroundPainter", com.codename1.ui.painter.BackgroundPainter.class);
        index.put("com.codename1.ui.painter.PainterChain", com.codename1.ui.painter.PainterChain.class);
    }

    private static void fillClassIndex7(Map<String, Class<?>> index) {
        index.put("com.codename1.ui.plaf.Border", com.codename1.ui.plaf.Border.class);
        index.put("com.codename1.ui.plaf.CSSBorder", com.codename1.ui.plaf.CSSBorder.class);
        index.put("com.codename1.ui.plaf.DefaultLookAndFeel", com.codename1.ui.plaf.DefaultLookAndFeel.class);
        index.put("com.codename1.ui.plaf.LookAndFeel", com.codename1.ui.plaf.LookAndFeel.class);
        index.put("com.codename1.ui.plaf.RoundBorder", com.codename1.ui.plaf.RoundBorder.class);
        index.put("com.codename1.ui.plaf.RoundRectBorder", com.codename1.ui.plaf.RoundRectBorder.class);
        index.put("com.codename1.ui.plaf.Style", com.codename1.ui.plaf.Style.class);
        index.put("com.codename1.ui.plaf.StyleParser", com.codename1.ui.plaf.StyleParser.class);
        index.put("com.codename1.ui.plaf.UIManager", com.codename1.ui.plaf.UIManager.class);
        index.put("com.codename1.ui.scene.Bounds", com.codename1.ui.scene.Bounds.class);
        index.put("com.codename1.ui.scene.Camera", com.codename1.ui.scene.Camera.class);
        index.put("com.codename1.ui.scene.Node", com.codename1.ui.scene.Node.class);
        index.put("com.codename1.ui.scene.NodePainter", com.codename1.ui.scene.NodePainter.class);
        index.put("com.codename1.ui.scene.PerspectiveCamera", com.codename1.ui.scene.PerspectiveCamera.class);
        index.put("com.codename1.ui.scene.Point3D", com.codename1.ui.scene.Point3D.class);
        index.put("com.codename1.ui.scene.Scene", com.codename1.ui.scene.Scene.class);
        index.put("com.codename1.ui.scene.TextPainter", com.codename1.ui.scene.TextPainter.class);
        index.put("com.codename1.ui.spinner.BaseSpinner", com.codename1.ui.spinner.BaseSpinner.class);
        index.put("com.codename1.ui.spinner.DateSpinner", com.codename1.ui.spinner.DateSpinner.class);
        index.put("com.codename1.ui.spinner.DateTimeSpinner", com.codename1.ui.spinner.DateTimeSpinner.class);
        index.put("com.codename1.ui.spinner.GenericSpinner", com.codename1.ui.spinner.GenericSpinner.class);
        index.put("com.codename1.ui.spinner.NumericSpinner", com.codename1.ui.spinner.NumericSpinner.class);
        index.put("com.codename1.ui.spinner.Picker", com.codename1.ui.spinner.Picker.class);
        index.put("com.codename1.ui.spinner.TimeSpinner", com.codename1.ui.spinner.TimeSpinner.class);
        index.put("com.codename1.ui.table.AbstractTableModel", com.codename1.ui.table.AbstractTableModel.class);
        index.put("com.codename1.ui.table.DefaultTableModel", com.codename1.ui.table.DefaultTableModel.class);
        index.put("com.codename1.ui.table.SortableTableModel", com.codename1.ui.table.SortableTableModel.class);
        index.put("com.codename1.ui.table.Table", com.codename1.ui.table.Table.class);
        index.put("com.codename1.ui.table.TableLayout", com.codename1.ui.table.TableLayout.class);
        index.put("com.codename1.ui.table.TableModel", com.codename1.ui.table.TableModel.class);
        index.put("com.codename1.ui.tree.Tree", com.codename1.ui.tree.Tree.class);
        index.put("com.codename1.ui.tree.TreeModel", com.codename1.ui.tree.TreeModel.class);
        index.put("com.codename1.ui.util.Effects", com.codename1.ui.util.Effects.class);
        index.put("com.codename1.ui.util.EmbeddedContainer", com.codename1.ui.util.EmbeddedContainer.class);
        index.put("com.codename1.ui.util.EventDispatcher", com.codename1.ui.util.EventDispatcher.class);
        index.put("com.codename1.ui.util.GlassTutorial", com.codename1.ui.util.GlassTutorial.class);
        index.put("com.codename1.ui.util.ImageIO", com.codename1.ui.util.ImageIO.class);
        index.put("com.codename1.ui.util.MutableResouce", com.codename1.ui.util.MutableResouce.class);
        index.put("com.codename1.ui.util.MutableResource", com.codename1.ui.util.MutableResource.class);
        index.put("com.codename1.ui.util.Resources", com.codename1.ui.util.Resources.class);
        index.put("com.codename1.ui.util.SwipeBackSupport", com.codename1.ui.util.SwipeBackSupport.class);
        index.put("com.codename1.ui.util.UIBuilder", com.codename1.ui.util.UIBuilder.class);
        index.put("com.codename1.ui.util.UITimer", com.codename1.ui.util.UITimer.class);
        index.put("com.codename1.ui.util.WeakHashMap", com.codename1.ui.util.WeakHashMap.class);
        index.put("com.codename1.ui.validation.Constraint", com.codename1.ui.validation.Constraint.class);
        index.put("com.codename1.ui.validation.ExistInConstraint", com.codename1.ui.validation.ExistInConstraint.class);
        index.put("com.codename1.ui.validation.GroupConstraint", com.codename1.ui.validation.GroupConstraint.class);
        index.put("com.codename1.ui.validation.LengthConstraint", com.codename1.ui.validation.LengthConstraint.class);
        index.put("com.codename1.ui.validation.NotConstraint", com.codename1.ui.validation.NotConstraint.class);
        index.put("com.codename1.ui.validation.NumericConstraint", com.codename1.ui.validation.NumericConstraint.class);
        index.put("com.codename1.ui.validation.RegexConstraint", com.codename1.ui.validation.RegexConstraint.class);
        index.put("com.codename1.ui.validation.Validator", com.codename1.ui.validation.Validator.class);
        index.put("com.codename1.util.AsyncResource", com.codename1.util.AsyncResource.class);
        index.put("com.codename1.util.AsyncResult", com.codename1.util.AsyncResult.class);
        index.put("com.codename1.util.Base64", com.codename1.util.Base64.class);
        index.put("com.codename1.util.BigDecimal", com.codename1.util.BigDecimal.class);
        index.put("com.codename1.util.BigInteger", com.codename1.util.BigInteger.class);
        index.put("com.codename1.util.CStringBuilder", com.codename1.util.CStringBuilder.class);
        index.put("com.codename1.util.Callback", com.codename1.util.Callback.class);
        index.put("com.codename1.util.CallbackAdapter", com.codename1.util.CallbackAdapter.class);
        index.put("com.codename1.util.CallbackDispatcher", com.codename1.util.CallbackDispatcher.class);
        index.put("com.codename1.util.CaseInsensitiveOrder", com.codename1.util.CaseInsensitiveOrder.class);
        index.put("com.codename1.util.DateUtil", com.codename1.util.DateUtil.class);
        index.put("com.codename1.util.EasyThread", com.codename1.util.EasyThread.class);
    }

    private static void fillClassIndex8(Map<String, Class<?>> index) {
        index.put("com.codename1.util.FailureCallback", com.codename1.util.FailureCallback.class);
        index.put("com.codename1.util.LazyValue", com.codename1.util.LazyValue.class);
        index.put("com.codename1.util.MathUtil", com.codename1.util.MathUtil.class);
        index.put("com.codename1.util.OnComplete", com.codename1.util.OnComplete.class);
        index.put("com.codename1.util.RunnableWithResult", com.codename1.util.RunnableWithResult.class);
        index.put("com.codename1.util.RunnableWithResultSync", com.codename1.util.RunnableWithResultSync.class);
        index.put("com.codename1.util.StringUtil", com.codename1.util.StringUtil.class);
        index.put("com.codename1.util.SuccessCallback", com.codename1.util.SuccessCallback.class);
        index.put("com.codename1.util.Wrapper", com.codename1.util.Wrapper.class);
        index.put("com.codename1.util.promise.ExecutorFunction", com.codename1.util.promise.ExecutorFunction.class);
        index.put("com.codename1.util.promise.Functor", com.codename1.util.promise.Functor.class);
        index.put("com.codename1.util.promise.Promise", com.codename1.util.promise.Promise.class);
        index.put("com.codename1.util.regex.CharacterArrayCharacterIterator", com.codename1.util.regex.CharacterArrayCharacterIterator.class);
        index.put("com.codename1.util.regex.CharacterIterator", com.codename1.util.regex.CharacterIterator.class);
        index.put("com.codename1.util.regex.RE", com.codename1.util.regex.RE.class);
        index.put("com.codename1.util.regex.RECharacter", com.codename1.util.regex.RECharacter.class);
        index.put("com.codename1.util.regex.RECompiler", com.codename1.util.regex.RECompiler.class);
        index.put("com.codename1.util.regex.REDebugCompiler", com.codename1.util.regex.REDebugCompiler.class);
        index.put("com.codename1.util.regex.REProgram", com.codename1.util.regex.REProgram.class);
        index.put("com.codename1.util.regex.RESyntaxException", com.codename1.util.regex.RESyntaxException.class);
        index.put("com.codename1.util.regex.REUtil", com.codename1.util.regex.REUtil.class);
        index.put("com.codename1.util.regex.ReaderCharacterIterator", com.codename1.util.regex.ReaderCharacterIterator.class);
        index.put("com.codename1.util.regex.StreamCharacterIterator", com.codename1.util.regex.StreamCharacterIterator.class);
        index.put("com.codename1.util.regex.StringCharacterIterator", com.codename1.util.regex.StringCharacterIterator.class);
        index.put("com.codename1.util.regex.StringReader", com.codename1.util.regex.StringReader.class);
        index.put("com.codename1.xml.Element", com.codename1.xml.Element.class);
        index.put("com.codename1.xml.ParserCallback", com.codename1.xml.ParserCallback.class);
        index.put("com.codename1.xml.XMLParser", com.codename1.xml.XMLParser.class);
        index.put("com.codename1.xml.XMLWriter", com.codename1.xml.XMLWriter.class);
        index.put("com.codenameone.playground.CN1Playground", com.codenameone.playground.CN1Playground.class);
        index.put("com.codenameone.playground.PlaygroundContext", com.codenameone.playground.PlaygroundContext.class);
        index.put("java.io.ByteArrayInputStream", java.io.ByteArrayInputStream.class);
        index.put("java.io.ByteArrayOutputStream", java.io.ByteArrayOutputStream.class);
        index.put("java.io.DataInput", java.io.DataInput.class);
        index.put("java.io.DataInputStream", java.io.DataInputStream.class);
        index.put("java.io.DataOutput", java.io.DataOutput.class);
        index.put("java.io.DataOutputStream", java.io.DataOutputStream.class);
        index.put("java.io.EOFException", java.io.EOFException.class);
        index.put("java.io.Flushable", java.io.Flushable.class);
        index.put("java.io.IOException", java.io.IOException.class);
        index.put("java.io.InputStream", java.io.InputStream.class);
        index.put("java.io.InputStreamReader", java.io.InputStreamReader.class);
        index.put("java.io.InterruptedIOException", java.io.InterruptedIOException.class);
        index.put("java.io.OutputStream", java.io.OutputStream.class);
        index.put("java.io.OutputStreamWriter", java.io.OutputStreamWriter.class);
        index.put("java.io.PrintStream", java.io.PrintStream.class);
        index.put("java.io.Reader", java.io.Reader.class);
        index.put("java.io.Serializable", java.io.Serializable.class);
        index.put("java.io.StringReader", java.io.StringReader.class);
        index.put("java.io.StringWriter", java.io.StringWriter.class);
        index.put("java.io.UTFDataFormatException", java.io.UTFDataFormatException.class);
        index.put("java.io.UnsupportedEncodingException", java.io.UnsupportedEncodingException.class);
        index.put("java.io.Writer", java.io.Writer.class);
        index.put("java.lang.Appendable", java.lang.Appendable.class);
        index.put("java.lang.ArithmeticException", java.lang.ArithmeticException.class);
        index.put("java.lang.ArrayIndexOutOfBoundsException", java.lang.ArrayIndexOutOfBoundsException.class);
        index.put("java.lang.ArrayStoreException", java.lang.ArrayStoreException.class);
        index.put("java.lang.AssertionError", java.lang.AssertionError.class);
        index.put("java.lang.AutoCloseable", java.lang.AutoCloseable.class);
        index.put("java.lang.Boolean", java.lang.Boolean.class);
        index.put("java.lang.Byte", java.lang.Byte.class);
        index.put("java.lang.CharSequence", java.lang.CharSequence.class);
        index.put("java.lang.Character", java.lang.Character.class);
        index.put("java.lang.Class", java.lang.Class.class);
    }

    private static void fillClassIndex9(Map<String, Class<?>> index) {
        index.put("java.lang.ClassCastException", java.lang.ClassCastException.class);
        index.put("java.lang.ClassLoader", java.lang.ClassLoader.class);
        index.put("java.lang.ClassNotFoundException", java.lang.ClassNotFoundException.class);
        index.put("java.lang.CloneNotSupportedException", java.lang.CloneNotSupportedException.class);
        index.put("java.lang.Cloneable", java.lang.Cloneable.class);
        index.put("java.lang.Comparable", java.lang.Comparable.class);
        index.put("java.lang.Deprecated", java.lang.Deprecated.class);
        index.put("java.lang.Double", java.lang.Double.class);
        index.put("java.lang.Enum", java.lang.Enum.class);
        index.put("java.lang.Error", java.lang.Error.class);
        index.put("java.lang.Exception", java.lang.Exception.class);
        index.put("java.lang.Float", java.lang.Float.class);
        index.put("java.lang.IllegalAccessException", java.lang.IllegalAccessException.class);
        index.put("java.lang.IllegalArgumentException", java.lang.IllegalArgumentException.class);
        index.put("java.lang.IllegalMonitorStateException", java.lang.IllegalMonitorStateException.class);
        index.put("java.lang.IllegalStateException", java.lang.IllegalStateException.class);
        index.put("java.lang.IncompatibleClassChangeError", java.lang.IncompatibleClassChangeError.class);
        index.put("java.lang.IndexOutOfBoundsException", java.lang.IndexOutOfBoundsException.class);
        index.put("java.lang.InstantiationException", java.lang.InstantiationException.class);
        index.put("java.lang.Integer", java.lang.Integer.class);
        index.put("java.lang.InterruptedException", java.lang.InterruptedException.class);
        index.put("java.lang.Iterable", java.lang.Iterable.class);
        index.put("java.lang.LinkageError", java.lang.LinkageError.class);
        index.put("java.lang.Long", java.lang.Long.class);
        index.put("java.lang.Math", java.lang.Math.class);
        index.put("java.lang.NegativeArraySizeException", java.lang.NegativeArraySizeException.class);
        index.put("java.lang.NoClassDefFoundError", java.lang.NoClassDefFoundError.class);
        index.put("java.lang.NoSuchFieldError", java.lang.NoSuchFieldError.class);
        index.put("java.lang.NullPointerException", java.lang.NullPointerException.class);
        index.put("java.lang.Number", java.lang.Number.class);
        index.put("java.lang.NumberFormatException", java.lang.NumberFormatException.class);
        index.put("java.lang.Object", java.lang.Object.class);
        index.put("java.lang.OutOfMemoryError", java.lang.OutOfMemoryError.class);
        index.put("java.lang.Override", java.lang.Override.class);
        index.put("java.lang.Runnable", java.lang.Runnable.class);
        index.put("java.lang.Runtime", java.lang.Runtime.class);
        index.put("java.lang.RuntimeException", java.lang.RuntimeException.class);
        index.put("java.lang.SafeVarargs", java.lang.SafeVarargs.class);
        index.put("java.lang.SecurityException", java.lang.SecurityException.class);
        index.put("java.lang.Short", java.lang.Short.class);
        index.put("java.lang.StackTraceElement", java.lang.StackTraceElement.class);
        index.put("java.lang.String", java.lang.String.class);
        index.put("java.lang.StringBuffer", java.lang.StringBuffer.class);
        index.put("java.lang.StringBuilder", java.lang.StringBuilder.class);
        index.put("java.lang.StringIndexOutOfBoundsException", java.lang.StringIndexOutOfBoundsException.class);
        index.put("java.lang.System", java.lang.System.class);
        index.put("java.lang.Thread", java.lang.Thread.class);
        index.put("java.lang.ThreadLocal", java.lang.ThreadLocal.class);
        index.put("java.lang.Throwable", java.lang.Throwable.class);
        index.put("java.lang.UnsupportedOperationException", java.lang.UnsupportedOperationException.class);
        index.put("java.lang.VirtualMachineError", java.lang.VirtualMachineError.class);
        index.put("java.lang.Void", java.lang.Void.class);
        index.put("java.lang.ref.Reference", java.lang.ref.Reference.class);
        index.put("java.lang.ref.WeakReference", java.lang.ref.WeakReference.class);
        index.put("java.lang.reflect.Array", java.lang.reflect.Array.class);
        index.put("java.lang.reflect.Constructor", java.lang.reflect.Constructor.class);
        index.put("java.lang.reflect.Method", java.lang.reflect.Method.class);
        index.put("java.lang.reflect.Type", java.lang.reflect.Type.class);
        index.put("java.net.URI", java.net.URI.class);
        index.put("java.net.URISyntaxException", java.net.URISyntaxException.class);
        index.put("java.nio.charset.Charset", java.nio.charset.Charset.class);
        index.put("java.text.DateFormat", java.text.DateFormat.class);
        index.put("java.text.DateFormatSymbols", java.text.DateFormatSymbols.class);
        index.put("java.text.Format", java.text.Format.class);
    }

    private static void fillClassIndex10(Map<String, Class<?>> index) {
        index.put("java.text.ParseException", java.text.ParseException.class);
        index.put("java.text.SimpleDateFormat", java.text.SimpleDateFormat.class);
        index.put("java.util.AbstractCollection", java.util.AbstractCollection.class);
        index.put("java.util.AbstractList", java.util.AbstractList.class);
        index.put("java.util.AbstractMap", java.util.AbstractMap.class);
        index.put("java.util.AbstractQueue", java.util.AbstractQueue.class);
        index.put("java.util.AbstractSequentialList", java.util.AbstractSequentialList.class);
        index.put("java.util.AbstractSet", java.util.AbstractSet.class);
        index.put("java.util.ArrayDeque", java.util.ArrayDeque.class);
        index.put("java.util.ArrayList", java.util.ArrayList.class);
        index.put("java.util.Arrays", java.util.Arrays.class);
        index.put("java.util.BitSet", java.util.BitSet.class);
        index.put("java.util.Calendar", java.util.Calendar.class);
        index.put("java.util.Collection", java.util.Collection.class);
        index.put("java.util.Collections", java.util.Collections.class);
        index.put("java.util.Comparator", java.util.Comparator.class);
        index.put("java.util.ConcurrentModificationException", java.util.ConcurrentModificationException.class);
        index.put("java.util.Date", java.util.Date.class);
        index.put("java.util.Deque", java.util.Deque.class);
        index.put("java.util.Dictionary", java.util.Dictionary.class);
        index.put("java.util.EmptyStackException", java.util.EmptyStackException.class);
        index.put("java.util.Enumeration", java.util.Enumeration.class);
        index.put("java.util.EventListener", java.util.EventListener.class);
        index.put("java.util.HashMap", java.util.HashMap.class);
        index.put("java.util.HashSet", java.util.HashSet.class);
        index.put("java.util.Hashtable", java.util.Hashtable.class);
        index.put("java.util.IdentityHashMap", java.util.IdentityHashMap.class);
        index.put("java.util.Iterator", java.util.Iterator.class);
        index.put("java.util.LinkedHashMap", java.util.LinkedHashMap.class);
        index.put("java.util.LinkedHashSet", java.util.LinkedHashSet.class);
        index.put("java.util.LinkedList", java.util.LinkedList.class);
        index.put("java.util.List", java.util.List.class);
        index.put("java.util.ListIterator", java.util.ListIterator.class);
        index.put("java.util.Locale", java.util.Locale.class);
        index.put("java.util.Map", java.util.Map.class);
        index.put("java.util.NavigableMap", java.util.NavigableMap.class);
        index.put("java.util.NavigableSet", java.util.NavigableSet.class);
        index.put("java.util.NoSuchElementException", java.util.NoSuchElementException.class);
        index.put("java.util.Objects", java.util.Objects.class);
        index.put("java.util.Observable", java.util.Observable.class);
        index.put("java.util.Observer", java.util.Observer.class);
        index.put("java.util.PriorityQueue", java.util.PriorityQueue.class);
        index.put("java.util.Queue", java.util.Queue.class);
        index.put("java.util.Random", java.util.Random.class);
        index.put("java.util.RandomAccess", java.util.RandomAccess.class);
        index.put("java.util.Set", java.util.Set.class);
        index.put("java.util.SortedMap", java.util.SortedMap.class);
        index.put("java.util.SortedSet", java.util.SortedSet.class);
        index.put("java.util.Stack", java.util.Stack.class);
        index.put("java.util.StringTokenizer", java.util.StringTokenizer.class);
        index.put("java.util.TimeZone", java.util.TimeZone.class);
        index.put("java.util.Timer", java.util.Timer.class);
        index.put("java.util.TimerTask", java.util.TimerTask.class);
        index.put("java.util.TreeMap", java.util.TreeMap.class);
        index.put("java.util.TreeSet", java.util.TreeSet.class);
        index.put("java.util.Vector", java.util.Vector.class);
        index.put("java.util.concurrent.ThreadLocalRandom", java.util.concurrent.ThreadLocalRandom.class);
    }

    private GeneratedCN1Access() {
    }

    @Override
    public Class<?> findClass(String name) {
        if (shouldDebugFindClass(name)) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass(" + name + ") size=" + CLASS_INDEX.size() + " contains=" + CLASS_INDEX.containsKey(name));
        }
        if (name == null) {
            if (shouldDebugFindClass(name)) {
                com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass miss " + name);
            }
            return null;
        }
        Class<?> found = CLASS_INDEX.get(name);
        if (shouldDebugFindClass(name) && found != null) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass hit " + name + " -> " + found);
        }
        if (found != null) {
            return found;
        }
        if (shouldDebugFindClass(name)) {
            com.codenameone.playground.PlaygroundContext.debug("GeneratedCN1Access.findClass miss " + name);
        }
        return null;
    }

    public static int debugClassIndexSize() {
        return CLASS_INDEX.size();
    }

    public static boolean debugClassIndexContains(String name) {
        return CLASS_INDEX.containsKey(name);
    }

    @Override
    public Object construct(Class<?> type, Object[] args) throws Exception {
        String packageName = packageName(type);
        if ("com.codename1.ads".equals(packageName)) {
            return GeneratedAccess_com_codename1_ads.construct(type, args);
        }
        if ("com.codename1.analytics".equals(packageName)) {
            return GeneratedAccess_com_codename1_analytics.construct(type, args);
        }
        if ("com.codename1.annotations".equals(packageName)) {
            return GeneratedAccess_com_codename1_annotations.construct(type, args);
        }
        if ("com.codename1.background".equals(packageName)) {
            return GeneratedAccess_com_codename1_background.construct(type, args);
        }
        if ("com.codename1.capture".equals(packageName)) {
            return GeneratedAccess_com_codename1_capture.construct(type, args);
        }
        if ("com.codename1.charts".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts.construct(type, args);
        }
        if ("com.codename1.charts.compat".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_compat.construct(type, args);
        }
        if ("com.codename1.charts.models".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_models.construct(type, args);
        }
        if ("com.codename1.charts.renderers".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_renderers.construct(type, args);
        }
        if ("com.codename1.charts.transitions".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_transitions.construct(type, args);
        }
        if ("com.codename1.charts.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_util.construct(type, args);
        }
        if ("com.codename1.charts.views".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_views.construct(type, args);
        }
        if ("com.codename1.cloud".equals(packageName)) {
            return GeneratedAccess_com_codename1_cloud.construct(type, args);
        }
        if ("com.codename1.codescan".equals(packageName)) {
            return GeneratedAccess_com_codename1_codescan.construct(type, args);
        }
        if ("com.codename1.compat.java.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_compat_java_util.construct(type, args);
        }
        if ("com.codename1.components".equals(packageName)) {
            return GeneratedAccess_com_codename1_components.construct(type, args);
        }
        if ("com.codename1.contacts".equals(packageName)) {
            return GeneratedAccess_com_codename1_contacts.construct(type, args);
        }
        if ("com.codename1.db".equals(packageName)) {
            return GeneratedAccess_com_codename1_db.construct(type, args);
        }
        if ("com.codename1.facebook".equals(packageName)) {
            return GeneratedAccess_com_codename1_facebook.construct(type, args);
        }
        if ("com.codename1.facebook.ui".equals(packageName)) {
            return GeneratedAccess_com_codename1_facebook_ui.construct(type, args);
        }
        if ("com.codename1.impl".equals(packageName)) {
            return GeneratedAccess_com_codename1_impl.construct(type, args);
        }
        if ("com.codename1.io".equals(packageName)) {
            return GeneratedAccess_com_codename1_io.construct(type, args);
        }
        if ("com.codename1.io.gzip".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_gzip.construct(type, args);
        }
        if ("com.codename1.io.rest".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_rest.construct(type, args);
        }
        if ("com.codename1.io.services".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_services.construct(type, args);
        }
        if ("com.codename1.io.tar".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_tar.construct(type, args);
        }
        if ("com.codename1.javascript".equals(packageName)) {
            return GeneratedAccess_com_codename1_javascript.construct(type, args);
        }
        if ("com.codename1.l10n".equals(packageName)) {
            return GeneratedAccess_com_codename1_l10n.construct(type, args);
        }
        if ("com.codename1.location".equals(packageName)) {
            return GeneratedAccess_com_codename1_location.construct(type, args);
        }
        if ("com.codename1.maps".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps.construct(type, args);
        }
        if ("com.codename1.maps.layers".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps_layers.construct(type, args);
        }
        if ("com.codename1.maps.providers".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps_providers.construct(type, args);
        }
        if ("com.codename1.media".equals(packageName)) {
            return GeneratedAccess_com_codename1_media.construct(type, args);
        }
        if ("com.codename1.messaging".equals(packageName)) {
            return GeneratedAccess_com_codename1_messaging.construct(type, args);
        }
        if ("com.codename1.notifications".equals(packageName)) {
            return GeneratedAccess_com_codename1_notifications.construct(type, args);
        }
        if ("com.codename1.payment".equals(packageName)) {
            return GeneratedAccess_com_codename1_payment.construct(type, args);
        }
        if ("com.codename1.plugin".equals(packageName)) {
            return GeneratedAccess_com_codename1_plugin.construct(type, args);
        }
        if ("com.codename1.plugin.event".equals(packageName)) {
            return GeneratedAccess_com_codename1_plugin_event.construct(type, args);
        }
        if ("com.codename1.processing".equals(packageName)) {
            return GeneratedAccess_com_codename1_processing.construct(type, args);
        }
        if ("com.codename1.properties".equals(packageName)) {
            return GeneratedAccess_com_codename1_properties.construct(type, args);
        }
        if ("com.codename1.push".equals(packageName)) {
            return GeneratedAccess_com_codename1_push.construct(type, args);
        }
        if ("com.codename1.share".equals(packageName)) {
            return GeneratedAccess_com_codename1_share.construct(type, args);
        }
        if ("com.codename1.social".equals(packageName)) {
            return GeneratedAccess_com_codename1_social.construct(type, args);
        }
        if ("com.codename1.system".equals(packageName)) {
            return GeneratedAccess_com_codename1_system.construct(type, args);
        }
        if ("com.codename1.testing".equals(packageName)) {
            return GeneratedAccess_com_codename1_testing.construct(type, args);
        }
        if ("com.codename1.ui".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui.construct(type, args);
        }
        if ("com.codename1.ui.animations".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_animations.construct(type, args);
        }
        if ("com.codename1.ui.css".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_css.construct(type, args);
        }
        if ("com.codename1.ui.events".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_events.construct(type, args);
        }
        if ("com.codename1.ui.geom".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_geom.construct(type, args);
        }
        if ("com.codename1.ui.html".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_html.construct(type, args);
        }
        if ("com.codename1.ui.layouts".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_layouts.construct(type, args);
        }
        if ("com.codename1.ui.layouts.mig".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_layouts_mig.construct(type, args);
        }
        if ("com.codename1.ui.list".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_list.construct(type, args);
        }
        if ("com.codename1.ui.painter".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_painter.construct(type, args);
        }
        if ("com.codename1.ui.plaf".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_plaf.construct(type, args);
        }
        if ("com.codename1.ui.scene".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_scene.construct(type, args);
        }
        if ("com.codename1.ui.spinner".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_spinner.construct(type, args);
        }
        if ("com.codename1.ui.table".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_table.construct(type, args);
        }
        if ("com.codename1.ui.tree".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_tree.construct(type, args);
        }
        if ("com.codename1.ui.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_util.construct(type, args);
        }
        if ("com.codename1.ui.validation".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_validation.construct(type, args);
        }
        if ("com.codename1.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_util.construct(type, args);
        }
        if ("com.codename1.util.promise".equals(packageName)) {
            return GeneratedAccess_com_codename1_util_promise.construct(type, args);
        }
        if ("com.codename1.util.regex".equals(packageName)) {
            return GeneratedAccess_com_codename1_util_regex.construct(type, args);
        }
        if ("com.codename1.xml".equals(packageName)) {
            return GeneratedAccess_com_codename1_xml.construct(type, args);
        }
        if ("com.codenameone.playground".equals(packageName)) {
            return GeneratedAccess_com_codenameone_playground.construct(type, args);
        }
        if ("java.io".equals(packageName)) {
            return GeneratedAccess_java_io.construct(type, args);
        }
        if ("java.lang".equals(packageName)) {
            return GeneratedAccess_java_lang.construct(type, args);
        }
        if ("java.lang.ref".equals(packageName)) {
            return GeneratedAccess_java_lang_ref.construct(type, args);
        }
        if ("java.lang.reflect".equals(packageName)) {
            return GeneratedAccess_java_lang_reflect.construct(type, args);
        }
        if ("java.net".equals(packageName)) {
            return GeneratedAccess_java_net.construct(type, args);
        }
        if ("java.nio.charset".equals(packageName)) {
            return GeneratedAccess_java_nio_charset.construct(type, args);
        }
        if ("java.text".equals(packageName)) {
            return GeneratedAccess_java_text.construct(type, args);
        }
        if ("java.util".equals(packageName)) {
            return GeneratedAccess_java_util.construct(type, args);
        }
        if ("java.util.concurrent".equals(packageName)) {
            return GeneratedAccess_java_util_concurrent.construct(type, args);
        }
        throw unsupportedConstruct(type, args);
    }

    @Override
    public Object invokeStatic(Class<?> type, String name, Object[] args) throws Exception {
        String packageName = packageName(type);
        if ("com.codename1.ads".equals(packageName)) {
            return GeneratedAccess_com_codename1_ads.invokeStatic(type, name, args);
        }
        if ("com.codename1.analytics".equals(packageName)) {
            return GeneratedAccess_com_codename1_analytics.invokeStatic(type, name, args);
        }
        if ("com.codename1.annotations".equals(packageName)) {
            return GeneratedAccess_com_codename1_annotations.invokeStatic(type, name, args);
        }
        if ("com.codename1.background".equals(packageName)) {
            return GeneratedAccess_com_codename1_background.invokeStatic(type, name, args);
        }
        if ("com.codename1.capture".equals(packageName)) {
            return GeneratedAccess_com_codename1_capture.invokeStatic(type, name, args);
        }
        if ("com.codename1.charts".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts.invokeStatic(type, name, args);
        }
        if ("com.codename1.charts.compat".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_compat.invokeStatic(type, name, args);
        }
        if ("com.codename1.charts.models".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_models.invokeStatic(type, name, args);
        }
        if ("com.codename1.charts.renderers".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_renderers.invokeStatic(type, name, args);
        }
        if ("com.codename1.charts.transitions".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_transitions.invokeStatic(type, name, args);
        }
        if ("com.codename1.charts.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_util.invokeStatic(type, name, args);
        }
        if ("com.codename1.charts.views".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_views.invokeStatic(type, name, args);
        }
        if ("com.codename1.cloud".equals(packageName)) {
            return GeneratedAccess_com_codename1_cloud.invokeStatic(type, name, args);
        }
        if ("com.codename1.codescan".equals(packageName)) {
            return GeneratedAccess_com_codename1_codescan.invokeStatic(type, name, args);
        }
        if ("com.codename1.compat.java.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_compat_java_util.invokeStatic(type, name, args);
        }
        if ("com.codename1.components".equals(packageName)) {
            return GeneratedAccess_com_codename1_components.invokeStatic(type, name, args);
        }
        if ("com.codename1.contacts".equals(packageName)) {
            return GeneratedAccess_com_codename1_contacts.invokeStatic(type, name, args);
        }
        if ("com.codename1.db".equals(packageName)) {
            return GeneratedAccess_com_codename1_db.invokeStatic(type, name, args);
        }
        if ("com.codename1.facebook".equals(packageName)) {
            return GeneratedAccess_com_codename1_facebook.invokeStatic(type, name, args);
        }
        if ("com.codename1.facebook.ui".equals(packageName)) {
            return GeneratedAccess_com_codename1_facebook_ui.invokeStatic(type, name, args);
        }
        if ("com.codename1.impl".equals(packageName)) {
            return GeneratedAccess_com_codename1_impl.invokeStatic(type, name, args);
        }
        if ("com.codename1.io".equals(packageName)) {
            return GeneratedAccess_com_codename1_io.invokeStatic(type, name, args);
        }
        if ("com.codename1.io.gzip".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_gzip.invokeStatic(type, name, args);
        }
        if ("com.codename1.io.rest".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_rest.invokeStatic(type, name, args);
        }
        if ("com.codename1.io.services".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_services.invokeStatic(type, name, args);
        }
        if ("com.codename1.io.tar".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_tar.invokeStatic(type, name, args);
        }
        if ("com.codename1.javascript".equals(packageName)) {
            return GeneratedAccess_com_codename1_javascript.invokeStatic(type, name, args);
        }
        if ("com.codename1.l10n".equals(packageName)) {
            return GeneratedAccess_com_codename1_l10n.invokeStatic(type, name, args);
        }
        if ("com.codename1.location".equals(packageName)) {
            return GeneratedAccess_com_codename1_location.invokeStatic(type, name, args);
        }
        if ("com.codename1.maps".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps.invokeStatic(type, name, args);
        }
        if ("com.codename1.maps.layers".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps_layers.invokeStatic(type, name, args);
        }
        if ("com.codename1.maps.providers".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps_providers.invokeStatic(type, name, args);
        }
        if ("com.codename1.media".equals(packageName)) {
            return GeneratedAccess_com_codename1_media.invokeStatic(type, name, args);
        }
        if ("com.codename1.messaging".equals(packageName)) {
            return GeneratedAccess_com_codename1_messaging.invokeStatic(type, name, args);
        }
        if ("com.codename1.notifications".equals(packageName)) {
            return GeneratedAccess_com_codename1_notifications.invokeStatic(type, name, args);
        }
        if ("com.codename1.payment".equals(packageName)) {
            return GeneratedAccess_com_codename1_payment.invokeStatic(type, name, args);
        }
        if ("com.codename1.plugin".equals(packageName)) {
            return GeneratedAccess_com_codename1_plugin.invokeStatic(type, name, args);
        }
        if ("com.codename1.plugin.event".equals(packageName)) {
            return GeneratedAccess_com_codename1_plugin_event.invokeStatic(type, name, args);
        }
        if ("com.codename1.processing".equals(packageName)) {
            return GeneratedAccess_com_codename1_processing.invokeStatic(type, name, args);
        }
        if ("com.codename1.properties".equals(packageName)) {
            return GeneratedAccess_com_codename1_properties.invokeStatic(type, name, args);
        }
        if ("com.codename1.push".equals(packageName)) {
            return GeneratedAccess_com_codename1_push.invokeStatic(type, name, args);
        }
        if ("com.codename1.share".equals(packageName)) {
            return GeneratedAccess_com_codename1_share.invokeStatic(type, name, args);
        }
        if ("com.codename1.social".equals(packageName)) {
            return GeneratedAccess_com_codename1_social.invokeStatic(type, name, args);
        }
        if ("com.codename1.system".equals(packageName)) {
            return GeneratedAccess_com_codename1_system.invokeStatic(type, name, args);
        }
        if ("com.codename1.testing".equals(packageName)) {
            return GeneratedAccess_com_codename1_testing.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.animations".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_animations.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.css".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_css.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.events".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_events.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.geom".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_geom.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.html".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_html.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.layouts".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_layouts.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.layouts.mig".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_layouts_mig.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.list".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_list.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.painter".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_painter.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.plaf".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_plaf.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.scene".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_scene.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.spinner".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_spinner.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.table".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_table.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.tree".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_tree.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_util.invokeStatic(type, name, args);
        }
        if ("com.codename1.ui.validation".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_validation.invokeStatic(type, name, args);
        }
        if ("com.codename1.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_util.invokeStatic(type, name, args);
        }
        if ("com.codename1.util.promise".equals(packageName)) {
            return GeneratedAccess_com_codename1_util_promise.invokeStatic(type, name, args);
        }
        if ("com.codename1.util.regex".equals(packageName)) {
            return GeneratedAccess_com_codename1_util_regex.invokeStatic(type, name, args);
        }
        if ("com.codename1.xml".equals(packageName)) {
            return GeneratedAccess_com_codename1_xml.invokeStatic(type, name, args);
        }
        if ("com.codenameone.playground".equals(packageName)) {
            return GeneratedAccess_com_codenameone_playground.invokeStatic(type, name, args);
        }
        if ("java.io".equals(packageName)) {
            return GeneratedAccess_java_io.invokeStatic(type, name, args);
        }
        if ("java.lang".equals(packageName)) {
            return GeneratedAccess_java_lang.invokeStatic(type, name, args);
        }
        if ("java.lang.ref".equals(packageName)) {
            return GeneratedAccess_java_lang_ref.invokeStatic(type, name, args);
        }
        if ("java.lang.reflect".equals(packageName)) {
            return GeneratedAccess_java_lang_reflect.invokeStatic(type, name, args);
        }
        if ("java.net".equals(packageName)) {
            return GeneratedAccess_java_net.invokeStatic(type, name, args);
        }
        if ("java.nio.charset".equals(packageName)) {
            return GeneratedAccess_java_nio_charset.invokeStatic(type, name, args);
        }
        if ("java.text".equals(packageName)) {
            return GeneratedAccess_java_text.invokeStatic(type, name, args);
        }
        if ("java.util".equals(packageName)) {
            return GeneratedAccess_java_util.invokeStatic(type, name, args);
        }
        if ("java.util.concurrent".equals(packageName)) {
            return GeneratedAccess_java_util_concurrent.invokeStatic(type, name, args);
        }
        throw unsupportedStatic(type, name, args);
    }

    @Override
    public Object invoke(Object target, String name, Object[] args) throws Exception {
        if (interceptShownForm(target, name, args)) {
            return null;
        }
        CN1AccessException unsupported = null;
        try {
            return GeneratedAccess_com_codename1_ads.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_analytics.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_annotations.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_background.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_capture.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_compat.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_models.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_renderers.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_transitions.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_util.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_views.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_cloud.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_codescan.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_compat_java_util.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_components.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_contacts.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_db.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_facebook.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_facebook_ui.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_impl.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io_gzip.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io_rest.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io_services.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io_tar.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_javascript.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_l10n.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_location.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_maps.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_maps_layers.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_maps_providers.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_media.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_messaging.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_notifications.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_payment.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_plugin.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_plugin_event.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_processing.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_properties.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_push.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_share.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_social.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_system.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_testing.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_animations.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_css.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_events.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_geom.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_html.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_layouts.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_layouts_mig.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_list.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_painter.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_plaf.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_scene.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_spinner.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_table.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_tree.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_util.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_validation.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_util.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_util_promise.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_util_regex.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_xml.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codenameone_playground.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_io.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_lang.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_lang_ref.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_lang_reflect.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_net.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_nio_charset.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_text.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_util.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_util_concurrent.invoke(target, name, args);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedInstance(target, name, args);
    }

    private static boolean interceptShownForm(Object target, String name, Object[] args) {
        PlaygroundContext context = PlaygroundContext.getCurrent();
        if (context == null || !(target instanceof Form) || !"show".equals(name)) {
            return false;
        }
        if (args != null && args.length != 0) {
            return false;
        }
        context.captureShownForm((Form) target);
        return true;
    }

    @Override
    public Object getStaticField(Class<?> type, String name) throws Exception {
        String packageName = packageName(type);
        if ("com.codename1.ads".equals(packageName)) {
            return GeneratedAccess_com_codename1_ads.getStaticField(type, name);
        }
        if ("com.codename1.analytics".equals(packageName)) {
            return GeneratedAccess_com_codename1_analytics.getStaticField(type, name);
        }
        if ("com.codename1.annotations".equals(packageName)) {
            return GeneratedAccess_com_codename1_annotations.getStaticField(type, name);
        }
        if ("com.codename1.background".equals(packageName)) {
            return GeneratedAccess_com_codename1_background.getStaticField(type, name);
        }
        if ("com.codename1.capture".equals(packageName)) {
            return GeneratedAccess_com_codename1_capture.getStaticField(type, name);
        }
        if ("com.codename1.charts".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts.getStaticField(type, name);
        }
        if ("com.codename1.charts.compat".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_compat.getStaticField(type, name);
        }
        if ("com.codename1.charts.models".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_models.getStaticField(type, name);
        }
        if ("com.codename1.charts.renderers".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_renderers.getStaticField(type, name);
        }
        if ("com.codename1.charts.transitions".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_transitions.getStaticField(type, name);
        }
        if ("com.codename1.charts.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_util.getStaticField(type, name);
        }
        if ("com.codename1.charts.views".equals(packageName)) {
            return GeneratedAccess_com_codename1_charts_views.getStaticField(type, name);
        }
        if ("com.codename1.cloud".equals(packageName)) {
            return GeneratedAccess_com_codename1_cloud.getStaticField(type, name);
        }
        if ("com.codename1.codescan".equals(packageName)) {
            return GeneratedAccess_com_codename1_codescan.getStaticField(type, name);
        }
        if ("com.codename1.compat.java.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_compat_java_util.getStaticField(type, name);
        }
        if ("com.codename1.components".equals(packageName)) {
            return GeneratedAccess_com_codename1_components.getStaticField(type, name);
        }
        if ("com.codename1.contacts".equals(packageName)) {
            return GeneratedAccess_com_codename1_contacts.getStaticField(type, name);
        }
        if ("com.codename1.db".equals(packageName)) {
            return GeneratedAccess_com_codename1_db.getStaticField(type, name);
        }
        if ("com.codename1.facebook".equals(packageName)) {
            return GeneratedAccess_com_codename1_facebook.getStaticField(type, name);
        }
        if ("com.codename1.facebook.ui".equals(packageName)) {
            return GeneratedAccess_com_codename1_facebook_ui.getStaticField(type, name);
        }
        if ("com.codename1.impl".equals(packageName)) {
            return GeneratedAccess_com_codename1_impl.getStaticField(type, name);
        }
        if ("com.codename1.io".equals(packageName)) {
            return GeneratedAccess_com_codename1_io.getStaticField(type, name);
        }
        if ("com.codename1.io.gzip".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_gzip.getStaticField(type, name);
        }
        if ("com.codename1.io.rest".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_rest.getStaticField(type, name);
        }
        if ("com.codename1.io.services".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_services.getStaticField(type, name);
        }
        if ("com.codename1.io.tar".equals(packageName)) {
            return GeneratedAccess_com_codename1_io_tar.getStaticField(type, name);
        }
        if ("com.codename1.javascript".equals(packageName)) {
            return GeneratedAccess_com_codename1_javascript.getStaticField(type, name);
        }
        if ("com.codename1.l10n".equals(packageName)) {
            return GeneratedAccess_com_codename1_l10n.getStaticField(type, name);
        }
        if ("com.codename1.location".equals(packageName)) {
            return GeneratedAccess_com_codename1_location.getStaticField(type, name);
        }
        if ("com.codename1.maps".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps.getStaticField(type, name);
        }
        if ("com.codename1.maps.layers".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps_layers.getStaticField(type, name);
        }
        if ("com.codename1.maps.providers".equals(packageName)) {
            return GeneratedAccess_com_codename1_maps_providers.getStaticField(type, name);
        }
        if ("com.codename1.media".equals(packageName)) {
            return GeneratedAccess_com_codename1_media.getStaticField(type, name);
        }
        if ("com.codename1.messaging".equals(packageName)) {
            return GeneratedAccess_com_codename1_messaging.getStaticField(type, name);
        }
        if ("com.codename1.notifications".equals(packageName)) {
            return GeneratedAccess_com_codename1_notifications.getStaticField(type, name);
        }
        if ("com.codename1.payment".equals(packageName)) {
            return GeneratedAccess_com_codename1_payment.getStaticField(type, name);
        }
        if ("com.codename1.plugin".equals(packageName)) {
            return GeneratedAccess_com_codename1_plugin.getStaticField(type, name);
        }
        if ("com.codename1.plugin.event".equals(packageName)) {
            return GeneratedAccess_com_codename1_plugin_event.getStaticField(type, name);
        }
        if ("com.codename1.processing".equals(packageName)) {
            return GeneratedAccess_com_codename1_processing.getStaticField(type, name);
        }
        if ("com.codename1.properties".equals(packageName)) {
            return GeneratedAccess_com_codename1_properties.getStaticField(type, name);
        }
        if ("com.codename1.push".equals(packageName)) {
            return GeneratedAccess_com_codename1_push.getStaticField(type, name);
        }
        if ("com.codename1.share".equals(packageName)) {
            return GeneratedAccess_com_codename1_share.getStaticField(type, name);
        }
        if ("com.codename1.social".equals(packageName)) {
            return GeneratedAccess_com_codename1_social.getStaticField(type, name);
        }
        if ("com.codename1.system".equals(packageName)) {
            return GeneratedAccess_com_codename1_system.getStaticField(type, name);
        }
        if ("com.codename1.testing".equals(packageName)) {
            return GeneratedAccess_com_codename1_testing.getStaticField(type, name);
        }
        if ("com.codename1.ui".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui.getStaticField(type, name);
        }
        if ("com.codename1.ui.animations".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_animations.getStaticField(type, name);
        }
        if ("com.codename1.ui.css".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_css.getStaticField(type, name);
        }
        if ("com.codename1.ui.events".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_events.getStaticField(type, name);
        }
        if ("com.codename1.ui.geom".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_geom.getStaticField(type, name);
        }
        if ("com.codename1.ui.html".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_html.getStaticField(type, name);
        }
        if ("com.codename1.ui.layouts".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_layouts.getStaticField(type, name);
        }
        if ("com.codename1.ui.layouts.mig".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_layouts_mig.getStaticField(type, name);
        }
        if ("com.codename1.ui.list".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_list.getStaticField(type, name);
        }
        if ("com.codename1.ui.painter".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_painter.getStaticField(type, name);
        }
        if ("com.codename1.ui.plaf".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_plaf.getStaticField(type, name);
        }
        if ("com.codename1.ui.scene".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_scene.getStaticField(type, name);
        }
        if ("com.codename1.ui.spinner".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_spinner.getStaticField(type, name);
        }
        if ("com.codename1.ui.table".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_table.getStaticField(type, name);
        }
        if ("com.codename1.ui.tree".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_tree.getStaticField(type, name);
        }
        if ("com.codename1.ui.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_util.getStaticField(type, name);
        }
        if ("com.codename1.ui.validation".equals(packageName)) {
            return GeneratedAccess_com_codename1_ui_validation.getStaticField(type, name);
        }
        if ("com.codename1.util".equals(packageName)) {
            return GeneratedAccess_com_codename1_util.getStaticField(type, name);
        }
        if ("com.codename1.util.promise".equals(packageName)) {
            return GeneratedAccess_com_codename1_util_promise.getStaticField(type, name);
        }
        if ("com.codename1.util.regex".equals(packageName)) {
            return GeneratedAccess_com_codename1_util_regex.getStaticField(type, name);
        }
        if ("com.codename1.xml".equals(packageName)) {
            return GeneratedAccess_com_codename1_xml.getStaticField(type, name);
        }
        if ("com.codenameone.playground".equals(packageName)) {
            return GeneratedAccess_com_codenameone_playground.getStaticField(type, name);
        }
        if ("java.io".equals(packageName)) {
            return GeneratedAccess_java_io.getStaticField(type, name);
        }
        if ("java.lang".equals(packageName)) {
            return GeneratedAccess_java_lang.getStaticField(type, name);
        }
        if ("java.lang.ref".equals(packageName)) {
            return GeneratedAccess_java_lang_ref.getStaticField(type, name);
        }
        if ("java.lang.reflect".equals(packageName)) {
            return GeneratedAccess_java_lang_reflect.getStaticField(type, name);
        }
        if ("java.net".equals(packageName)) {
            return GeneratedAccess_java_net.getStaticField(type, name);
        }
        if ("java.nio.charset".equals(packageName)) {
            return GeneratedAccess_java_nio_charset.getStaticField(type, name);
        }
        if ("java.text".equals(packageName)) {
            return GeneratedAccess_java_text.getStaticField(type, name);
        }
        if ("java.util".equals(packageName)) {
            return GeneratedAccess_java_util.getStaticField(type, name);
        }
        if ("java.util.concurrent".equals(packageName)) {
            return GeneratedAccess_java_util_concurrent.getStaticField(type, name);
        }
        throw unsupportedStaticField(type, name);
    }

    @Override
    public Object getField(Object target, String name) throws Exception {
        CN1AccessException unsupported = null;
        try {
            return GeneratedAccess_com_codename1_ads.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_analytics.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_annotations.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_background.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_capture.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_compat.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_models.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_renderers.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_transitions.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_util.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_charts_views.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_cloud.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_codescan.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_compat_java_util.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_components.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_contacts.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_db.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_facebook.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_facebook_ui.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_impl.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io_gzip.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io_rest.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io_services.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_io_tar.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_javascript.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_l10n.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_location.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_maps.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_maps_layers.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_maps_providers.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_media.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_messaging.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_notifications.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_payment.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_plugin.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_plugin_event.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_processing.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_properties.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_push.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_share.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_social.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_system.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_testing.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_animations.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_css.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_events.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_geom.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_html.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_layouts.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_layouts_mig.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_list.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_painter.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_plaf.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_scene.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_spinner.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_table.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_tree.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_util.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_ui_validation.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_util.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_util_promise.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_util_regex.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codename1_xml.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_com_codenameone_playground.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_io.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_lang.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_lang_ref.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_lang_reflect.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_net.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_nio_charset.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_text.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_util.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            return GeneratedAccess_java_util_concurrent.getField(target, name);
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedField(target, name);
    }

    @Override
    public void setStaticField(Class<?> type, String name, Object value) throws Exception {
        String packageName = packageName(type);
        if ("com.codename1.ads".equals(packageName)) {
            GeneratedAccess_com_codename1_ads.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.analytics".equals(packageName)) {
            GeneratedAccess_com_codename1_analytics.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.annotations".equals(packageName)) {
            GeneratedAccess_com_codename1_annotations.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.background".equals(packageName)) {
            GeneratedAccess_com_codename1_background.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.capture".equals(packageName)) {
            GeneratedAccess_com_codename1_capture.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.charts".equals(packageName)) {
            GeneratedAccess_com_codename1_charts.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.charts.compat".equals(packageName)) {
            GeneratedAccess_com_codename1_charts_compat.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.charts.models".equals(packageName)) {
            GeneratedAccess_com_codename1_charts_models.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.charts.renderers".equals(packageName)) {
            GeneratedAccess_com_codename1_charts_renderers.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.charts.transitions".equals(packageName)) {
            GeneratedAccess_com_codename1_charts_transitions.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.charts.util".equals(packageName)) {
            GeneratedAccess_com_codename1_charts_util.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.charts.views".equals(packageName)) {
            GeneratedAccess_com_codename1_charts_views.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.cloud".equals(packageName)) {
            GeneratedAccess_com_codename1_cloud.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.codescan".equals(packageName)) {
            GeneratedAccess_com_codename1_codescan.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.compat.java.util".equals(packageName)) {
            GeneratedAccess_com_codename1_compat_java_util.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.components".equals(packageName)) {
            GeneratedAccess_com_codename1_components.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.contacts".equals(packageName)) {
            GeneratedAccess_com_codename1_contacts.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.db".equals(packageName)) {
            GeneratedAccess_com_codename1_db.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.facebook".equals(packageName)) {
            GeneratedAccess_com_codename1_facebook.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.facebook.ui".equals(packageName)) {
            GeneratedAccess_com_codename1_facebook_ui.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.impl".equals(packageName)) {
            GeneratedAccess_com_codename1_impl.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.io".equals(packageName)) {
            GeneratedAccess_com_codename1_io.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.io.gzip".equals(packageName)) {
            GeneratedAccess_com_codename1_io_gzip.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.io.rest".equals(packageName)) {
            GeneratedAccess_com_codename1_io_rest.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.io.services".equals(packageName)) {
            GeneratedAccess_com_codename1_io_services.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.io.tar".equals(packageName)) {
            GeneratedAccess_com_codename1_io_tar.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.javascript".equals(packageName)) {
            GeneratedAccess_com_codename1_javascript.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.l10n".equals(packageName)) {
            GeneratedAccess_com_codename1_l10n.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.location".equals(packageName)) {
            GeneratedAccess_com_codename1_location.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.maps".equals(packageName)) {
            GeneratedAccess_com_codename1_maps.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.maps.layers".equals(packageName)) {
            GeneratedAccess_com_codename1_maps_layers.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.maps.providers".equals(packageName)) {
            GeneratedAccess_com_codename1_maps_providers.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.media".equals(packageName)) {
            GeneratedAccess_com_codename1_media.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.messaging".equals(packageName)) {
            GeneratedAccess_com_codename1_messaging.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.notifications".equals(packageName)) {
            GeneratedAccess_com_codename1_notifications.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.payment".equals(packageName)) {
            GeneratedAccess_com_codename1_payment.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.plugin".equals(packageName)) {
            GeneratedAccess_com_codename1_plugin.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.plugin.event".equals(packageName)) {
            GeneratedAccess_com_codename1_plugin_event.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.processing".equals(packageName)) {
            GeneratedAccess_com_codename1_processing.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.properties".equals(packageName)) {
            GeneratedAccess_com_codename1_properties.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.push".equals(packageName)) {
            GeneratedAccess_com_codename1_push.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.share".equals(packageName)) {
            GeneratedAccess_com_codename1_share.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.social".equals(packageName)) {
            GeneratedAccess_com_codename1_social.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.system".equals(packageName)) {
            GeneratedAccess_com_codename1_system.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.testing".equals(packageName)) {
            GeneratedAccess_com_codename1_testing.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui".equals(packageName)) {
            GeneratedAccess_com_codename1_ui.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.animations".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_animations.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.css".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_css.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.events".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_events.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.geom".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_geom.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.html".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_html.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.layouts".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_layouts.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.layouts.mig".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_layouts_mig.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.list".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_list.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.painter".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_painter.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.plaf".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_plaf.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.scene".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_scene.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.spinner".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_spinner.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.table".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_table.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.tree".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_tree.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.util".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_util.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.ui.validation".equals(packageName)) {
            GeneratedAccess_com_codename1_ui_validation.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.util".equals(packageName)) {
            GeneratedAccess_com_codename1_util.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.util.promise".equals(packageName)) {
            GeneratedAccess_com_codename1_util_promise.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.util.regex".equals(packageName)) {
            GeneratedAccess_com_codename1_util_regex.setStaticField(type, name, value);
            return;
        }
        if ("com.codename1.xml".equals(packageName)) {
            GeneratedAccess_com_codename1_xml.setStaticField(type, name, value);
            return;
        }
        if ("com.codenameone.playground".equals(packageName)) {
            GeneratedAccess_com_codenameone_playground.setStaticField(type, name, value);
            return;
        }
        if ("java.io".equals(packageName)) {
            GeneratedAccess_java_io.setStaticField(type, name, value);
            return;
        }
        if ("java.lang".equals(packageName)) {
            GeneratedAccess_java_lang.setStaticField(type, name, value);
            return;
        }
        if ("java.lang.ref".equals(packageName)) {
            GeneratedAccess_java_lang_ref.setStaticField(type, name, value);
            return;
        }
        if ("java.lang.reflect".equals(packageName)) {
            GeneratedAccess_java_lang_reflect.setStaticField(type, name, value);
            return;
        }
        if ("java.net".equals(packageName)) {
            GeneratedAccess_java_net.setStaticField(type, name, value);
            return;
        }
        if ("java.nio.charset".equals(packageName)) {
            GeneratedAccess_java_nio_charset.setStaticField(type, name, value);
            return;
        }
        if ("java.text".equals(packageName)) {
            GeneratedAccess_java_text.setStaticField(type, name, value);
            return;
        }
        if ("java.util".equals(packageName)) {
            GeneratedAccess_java_util.setStaticField(type, name, value);
            return;
        }
        if ("java.util.concurrent".equals(packageName)) {
            GeneratedAccess_java_util_concurrent.setStaticField(type, name, value);
            return;
        }
        throw unsupportedStaticFieldWrite(type, name, value);
    }

    @Override
    public void setField(Object target, String name, Object value) throws Exception {
        CN1AccessException unsupported = null;
        try {
            GeneratedAccess_com_codename1_ads.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_analytics.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_annotations.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_background.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_capture.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_charts.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_charts_compat.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_charts_models.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_charts_renderers.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_charts_transitions.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_charts_util.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_charts_views.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_cloud.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_codescan.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_compat_java_util.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_components.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_contacts.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_db.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_facebook.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_facebook_ui.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_impl.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_io.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_io_gzip.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_io_rest.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_io_services.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_io_tar.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_javascript.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_l10n.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_location.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_maps.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_maps_layers.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_maps_providers.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_media.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_messaging.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_notifications.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_payment.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_plugin.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_plugin_event.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_processing.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_properties.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_push.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_share.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_social.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_system.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_testing.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_animations.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_css.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_events.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_geom.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_html.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_layouts.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_layouts_mig.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_list.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_painter.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_plaf.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_scene.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_spinner.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_table.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_tree.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_util.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_ui_validation.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_util.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_util_promise.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_util_regex.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codename1_xml.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_com_codenameone_playground.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_io.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_lang.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_lang_ref.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_lang_reflect.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_net.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_nio_charset.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_text.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_util.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        try {
            GeneratedAccess_java_util_concurrent.setField(target, name, value);
            return;
        } catch (CN1AccessException ex) {
            unsupported = ex;
        }
        if (unsupported != null) {
            throw unsupported;
        }
        throw unsupportedFieldWrite(target, name, value);
    }

    private static String packageName(Class<?> type) {
        String name = type.getName();
        int lastDot = name.lastIndexOf('.');
        return lastDot < 0 ? "" : name.substring(0, lastDot);
    }

    private static String packageName(String name) {
        if (name == null) {
            return null;
        }
        int lastDot = name.lastIndexOf('.');
        return lastDot < 0 ? null : name.substring(0, lastDot);
    }

    private static String simpleName(String name) {
        if (name == null) {
            return null;
        }
        int lastDot = name.lastIndexOf('.');
        return lastDot < 0 || lastDot == name.length() - 1 ? null : name.substring(lastDot + 1);
    }

    private static CN1AccessException unsupportedConstruct(Class<?> type, Object[] args) {
        return new CN1AccessException("Generated constructor dispatch not implemented for " + type.getName() + describeArgs(args));
    }

    private static CN1AccessException unsupportedStatic(Class<?> type, String name, Object[] args) {
        return new CN1AccessException("Generated static dispatch not implemented for " + type.getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedInstance(Object target, String name, Object[] args) {
        return new CN1AccessException("Generated instance dispatch not implemented for " + target.getClass().getName() + "." + name + describeArgs(args));
    }

    private static CN1AccessException unsupportedStaticField(Class<?> type, String name) {
        return new CN1AccessException("Generated static field access not implemented for " + type.getName() + "." + name);
    }

    private static CN1AccessException unsupportedField(Object target, String name) {
        return new CN1AccessException("Generated field access not implemented for " + target.getClass().getName() + "." + name);
    }

    private static CN1AccessException unsupportedStaticFieldWrite(Class<?> type, String name, Object value) {
        return new CN1AccessException("Generated static field write not implemented for " + type.getName() + "." + name + " value=" + describeValue(value));
    }

    private static CN1AccessException unsupportedFieldWrite(Object target, String name, Object value) {
        return new CN1AccessException("Generated field write not implemented for " + target.getClass().getName() + "." + name + " value=" + describeValue(value));
    }

    private static boolean shouldDebugFindClass(String name) {
        return name != null && (name.startsWith("com.codename1.ui.") || name.startsWith("com.codename1.components."));
    }

    private static String describeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "()";
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(describeValue(args[i]));
        }
        sb.append(')');
        return sb.toString();
    }

    private static String describeValue(Object value) {
        return value == null ? "null" : value.getClass().getName();
    }
}
