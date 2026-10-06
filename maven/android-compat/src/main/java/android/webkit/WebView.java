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
package android.webkit;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.View;

import com.codename1.androidcompat.runtime.JsBridge;
import com.codename1.androidcompat.runtime.JsInterfaceDispatch;
import com.codename1.androidcompat.runtime.WebAssets;
import com.codename1.androidcompat.runtime.WebViewPeer;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.BrowserNavigationCallback;
import com.codename1.util.SuccessCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A view showing web content, backed by the platform's native browser
/// through Codename One's `BrowserComponent`.
///
/// Where it differs from Android:
///
/// - It is a `View`, not an `AbsoluteLayout`; child views cannot be added.
/// - The native browser handles its own touches, so touch listeners on the
///   view receive nothing.
/// - `shouldOverrideUrlLoading` runs on the UI thread as on Android, but the
///   native browser asks off it and cannot wait: the navigation is held, the
///   client is asked, and a navigation it allows is started again. A POST
///   form submission continues as a GET of its target.
/// - Methods exposed with [#addJavascriptInterface(Object, String)] are
///   called asynchronously: in JavaScript they return a `Promise` of the
///   Java result instead of the result itself, and they are installed when a
///   page finishes loading, so a page's inline scripts cannot call them
///   while it loads.
public class WebView extends View {

    public static final String SCHEME_TEL = "tel:";
    public static final String SCHEME_MAILTO = "mailto:";
    public static final String SCHEME_GEO = "geo:0,0?q=";

    /// What is under a long press. The native browser does not report it,
    /// so the type is always [#UNKNOWN_TYPE].
    public static class HitTestResult {
        public static final int UNKNOWN_TYPE = 0;
        @Deprecated
        public static final int ANCHOR_TYPE = 1;
        public static final int PHONE_TYPE = 2;
        public static final int GEO_TYPE = 3;
        public static final int EMAIL_TYPE = 4;
        public static final int IMAGE_TYPE = 5;
        @Deprecated
        public static final int IMAGE_ANCHOR_TYPE = 6;
        public static final int SRC_ANCHOR_TYPE = 7;
        public static final int SRC_IMAGE_ANCHOR_TYPE = 8;
        public static final int EDIT_TEXT_TYPE = 9;

        public int getType() {
            return UNKNOWN_TYPE;
        }

        public String getExtra() {
            return null;
        }
    }

    private static boolean sDebuggingEnabled;

    private final BrowserComponent mBrowser;
    private final WebSettings mSettings;
    private WebViewClient mClient;
    private WebChromeClient mChromeClient;
    private String mUrl;
    private String mOriginalUrl;
    private int mProgress = 100;
    /// A URL Java code started loading, which the navigation check lets
    /// through without asking the client (Android does not ask for
    /// `loadUrl`). Written on the EDT only.
    private String mPendingLoad;
    /// The base URL of the last `loadDataWithBaseURL` page and the history
    /// URL reported in its place, or null. Written on the EDT only.
    private String mHistoryBase;
    private String mHistoryUrl;
    private final Map<String, Object> mJsInterfaces = new HashMap<String, Object>();
    private SuccessCallback<BrowserComponent.JSRef> mJsCallback;
    private boolean mDestroyed;

    public WebView(Context context) {
        this(context, null);
    }

    public WebView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.webViewStyle);
    }

    public WebView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @Deprecated
    public WebView(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
        this(context, attrs, defStyleAttr, 0);
    }

    public WebView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mBrowser = new BrowserComponent();
        mBrowser.setFireCallbacksOnEdt(true);
        mBrowser.setDebugMode(sDebuggingEnabled);
        mSettings = new Settings();
        setFocusable(true);
        setFocusableInTouchMode(true);
        installListeners();
    }

    @Override
    protected Component createPeer() {
        return new WebViewPeer(this, mBrowser);
    }

    /// The Codename One browser behind this view.
    public BrowserComponent getBrowserComponent() {
        return mBrowser;
    }

    private final class Settings extends WebSettings {
        @Override
        void applyZoom(boolean enabled) {
            mBrowser.setPinchToZoomEnabled(enabled);
        }

        @Override
        void applyUserAgent(String userAgent) {
            mBrowser.setProperty("User-Agent", userAgent);
        }
    }

    // ------------------------------------------------------------ events

    private static void onEdt(Runnable r) {
        if (Display.getInstance().isEdt()) {
            r.run();
        } else {
            Display.getInstance().callSerially(r);
        }
    }

    private static String sourceUrl(ActionEvent evt) {
        Object src = evt == null ? null : evt.getSource();
        return src instanceof String ? WebAssets.toAndroidUrl((String) src) : null;
    }

    private void installListeners() {
        mBrowser.addWebEventListener(BrowserComponent.onStart, new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent evt) {
                onEdt(new Runnable() {
                    @Override
                    public void run() {
                        pageStarted(sourceUrl(evt));
                    }
                });
            }
        });
        mBrowser.addWebEventListener(BrowserComponent.onLoad, new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent evt) {
                onEdt(new Runnable() {
                    @Override
                    public void run() {
                        pageFinished(sourceUrl(evt));
                    }
                });
            }
        });
        mBrowser.addWebEventListener(BrowserComponent.onError, new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent evt) {
                final String message = evt.getSource() instanceof String ? (String) evt.getSource() : null;
                final int code = evt.getKeyEvent();
                onEdt(new Runnable() {
                    @Override
                    public void run() {
                        pageFailed(code, message);
                    }
                });
            }
        });
        mBrowser.addWebEventListener("onLoadResource", new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent evt) {
                onEdt(new Runnable() {
                    @Override
                    public void run() {
                        String url = sourceUrl(evt);
                        if (mClient != null && url != null && !mDestroyed) {
                            mClient.onLoadResource(WebView.this, url);
                        }
                    }
                });
            }
        });
        mBrowser.addBrowserNavigationCallback(new BrowserNavigationCallback() {
            @Override
            public boolean shouldNavigate(String url) {
                return navigationAllowed(url);
            }
        });
    }

    /// URLs the browser uses internally (script results, blank pages, data
    /// documents) are never shown to the application.
    private static boolean isInternal(String url) {
        return URLUtil.isJavaScriptUrl(url) || URLUtil.isAboutUrl(url) || URLUtil.isDataUrl(url)
                || url.regionMatches(true, 0, "blob:", 0, 5) || url.indexOf("/!cn1return/") >= 0
                || url.startsWith("cn1-") || url.length() == 0;
    }

    /// Called by the native browser before it navigates, on its own thread
    /// unless the platform calls back on the EDT. It must answer at once.
    boolean navigationAllowed(String url) {
        if (url == null || isInternal(url) || mDestroyed) {
            return true;
        }
        String pending = mPendingLoad;
        if (pending != null && (pending.equals(url) || WebAssets.toAndroidUrl(url).equals(pending))) {
            return true;
        }
        if (Display.getInstance().isEdt()) {
            return !overrides(url);
        }
        final String target = url;
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                if (!mDestroyed && !overrides(target)) {
                    mPendingLoad = target;
                    mBrowser.setURL(target);
                }
            }
        });
        return false;
    }

    /// Whether the application takes over the navigation to `url`. Without a
    /// client Android hands it to the system, which opens a browser.
    private boolean overrides(String url) {
        String appUrl = WebAssets.toAndroidUrl(url);
        if (mClient != null) {
            return mClient.shouldOverrideUrlLoading(this, new Request(appUrl, true, false, "GET"));
        }
        if (URLUtil.isFileUrl(url) || URLUtil.isAssetUrl(appUrl)) {
            return false;
        }
        Display.getInstance().execute(url);
        return true;
    }

    /// The URL the application is told for `url`: the history URL in place
    /// of the base of a `loadDataWithBaseURL` page.
    private String reported(String url) {
        return mHistoryUrl != null && url.equals(mHistoryBase) ? mHistoryUrl : url;
    }

    private void pageStarted(String url) {
        if (mDestroyed) {
            return;
        }
        if (url != null && !isInternal(url)) {
            mUrl = reported(url);
        }
        mProgress = 10;
        if (mClient != null) {
            mClient.onPageStarted(this, mUrl, null);
        }
        if (mChromeClient != null) {
            mChromeClient.onProgressChanged(this, mProgress);
        }
    }

    private void pageFinished(String url) {
        if (mDestroyed) {
            return;
        }
        mPendingLoad = null;
        if (url != null && !isInternal(url)) {
            mUrl = reported(url);
        }
        mProgress = 100;
        installJsInterfaces();
        if (mChromeClient != null) {
            mChromeClient.onProgressChanged(this, mProgress);
            String title = getTitle();
            if (title != null) {
                mChromeClient.onReceivedTitle(this, title);
            }
        }
        if (mClient != null) {
            mClient.doUpdateVisitedHistory(this, mUrl, false);
            mClient.onPageFinished(this, mUrl);
        }
    }

    private void pageFailed(int code, String message) {
        if (mDestroyed) {
            return;
        }
        mPendingLoad = null;
        mProgress = 100;
        if (mClient != null) {
            int androidCode = code <= WebViewClient.ERROR_UNKNOWN && code >= WebViewClient.ERROR_UNSAFE_RESOURCE
                    ? code : WebViewClient.ERROR_UNKNOWN;
            String url = mUrl == null ? "" : mUrl;
            mClient.onReceivedError(this, new Request(url, true, false, "GET"),
                    new ResourceError(androidCode, message == null ? "net::ERR_FAILED" : message));
        }
    }

    // ------------------------------------------------------------ loading

    public void loadUrl(String url) {
        loadUrl(url, null);
    }

    public void loadUrl(String url, Map<String, String> additionalHttpHeaders) {
        if (url == null || mDestroyed) {
            return;
        }
        if (URLUtil.isJavaScriptUrl(url)) {
            mBrowser.execute(url.substring("javascript:".length()));
            return;
        }
        if (url.equals(mHistoryBase)) {
            mHistoryBase = null;
            mHistoryUrl = null;
        }
        mOriginalUrl = url;
        mUrl = url;
        String loadable = WebAssets.toLoadableUrl(url);
        mPendingLoad = loadable;
        if (additionalHttpHeaders != null && !additionalHttpHeaders.isEmpty()
                && mBrowser.isURLWithCustomHeadersSupported()) {
            mBrowser.setURL(loadable, additionalHttpHeaders);
        } else {
            mBrowser.setURL(loadable);
        }
    }

    /// Loads `data` as a document. `encoding` "base64" means the data is
    /// base64 encoded; HTML is shown directly, anything else as a data URL.
    public void loadData(String data, String mimeType, String encoding) {
        if (mDestroyed) {
            return;
        }
        String mime = mimeType == null ? "text/html" : mimeType;
        boolean base64 = "base64".equalsIgnoreCase(encoding);
        mUrl = "data:" + mime + (base64 ? ";base64," : ",") + (data == null ? "" : data);
        mOriginalUrl = mUrl;
        if (!base64 && mime.regionMatches(true, 0, "text/html", 0, 9)) {
            mBrowser.setPage(data == null ? "" : data, null);
        } else {
            mBrowser.setURL(mUrl);
        }
    }

    public void loadDataWithBaseURL(String baseUrl, String data, String mimeType, String encoding,
                                    String historyUrl) {
        if (mDestroyed) {
            return;
        }
        String base = baseUrl == null ? "about:blank" : baseUrl;
        // Android names the page by `historyUrl` (when the base is not a
        // `data:` URL): `getUrl()`, the client callbacks and the history
        // entry report it. The native browser only knows the base, so the
        // base is reported as the history URL -- also when going back to
        // the page -- until another `loadDataWithBaseURL` replaces it or the
        // base itself is loaded. A null `historyUrl` keeps reporting the
        // base rather than Android's `about:blank`.
        if (historyUrl != null && !URLUtil.isDataUrl(base)) {
            mHistoryBase = base;
            mHistoryUrl = historyUrl;
        } else {
            mHistoryBase = null;
            mHistoryUrl = null;
        }
        mUrl = reported(base);
        mOriginalUrl = mUrl;
        String loadableBase = WebAssets.toLoadableUrl(base);
        if (!isInternal(loadableBase)) {
            mPendingLoad = loadableBase;
        }
        mBrowser.setPage(data == null ? "" : data, isInternal(loadableBase) ? null : loadableBase);
    }

    /// Posts `postData` (form encoded) to `url` by submitting a generated
    /// form, since the native browser loads URLs only with GET.
    public void postUrl(String url, byte[] postData) {
        if (mDestroyed || url == null) {
            return;
        }
        if (!URLUtil.isNetworkUrl(url)) {
            loadUrl(url);
            return;
        }
        StringBuilder html = new StringBuilder("<html><body><form id='f' method='POST' action=");
        html.append(attr(url)).append('>');
        String body = postData == null ? "" : utf8(postData);
        int start = 0;
        while (start <= body.length() && body.length() > 0) {
            int amp = body.indexOf('&', start);
            String pair = amp < 0 ? body.substring(start) : body.substring(start, amp);
            if (pair.length() > 0) {
                int eq = pair.indexOf('=');
                String k = eq < 0 ? pair : pair.substring(0, eq);
                String v = eq < 0 ? "" : pair.substring(eq + 1);
                html.append("<input type='hidden' name=").append(attr(Util.decode(k, "UTF-8", true)))
                        .append(" value=").append(attr(Util.decode(v, "UTF-8", true))).append('>');
            }
            if (amp < 0) {
                break;
            }
            start = amp + 1;
        }
        html.append("</form><script>document.getElementById('f').submit();</script></body></html>");
        mOriginalUrl = url;
        mUrl = url;
        mPendingLoad = url;
        mBrowser.setPage(html.toString(), null);
    }

    private static String utf8(byte[] b) {
        try {
            return new String(b, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported");
        }
    }

    private static String attr(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') {
                sb.append("&quot;");
            } else if (c == '&') {
                sb.append("&amp;");
            } else if (c == '<') {
                sb.append("&lt;");
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    public void reload() {
        if (!mDestroyed) {
            mBrowser.reload();
        }
    }

    public void stopLoading() {
        if (!mDestroyed) {
            mBrowser.stop();
        }
    }

    public boolean canGoBack() {
        return !mDestroyed && mBrowser.hasBack();
    }

    public boolean canGoForward() {
        return !mDestroyed && mBrowser.hasForward();
    }

    public void goBack() {
        if (!mDestroyed) {
            mBrowser.back();
        }
    }

    public void goForward() {
        if (!mDestroyed) {
            mBrowser.forward();
        }
    }

    /// Codename One's browser reports only whether one entry exists in each
    /// direction, not how deep the history goes, so a move of more than one
    /// entry cannot be confirmed and answers false rather than enabling a
    /// control for a position that may not exist.
    public boolean canGoBackOrForward(int steps) {
        if (steps == 0) {
            return true;
        }
        if (steps < -1 || steps > 1) {
            return false;
        }
        return steps < 0 ? canGoBack() : canGoForward();
    }

    /// Moves `steps` entries through the history, one at a time.
    public void goBackOrForward(int steps) {
        for (int i = 0; i < Math.abs(steps); i++) {
            if (steps < 0) {
                goBack();
            } else {
                goForward();
            }
        }
    }

    public String getUrl() {
        return mUrl;
    }

    public String getOriginalUrl() {
        return mOriginalUrl;
    }

    public String getTitle() {
        if (mDestroyed) {
            return null;
        }
        try {
            return mBrowser.getTitle();
        } catch (RuntimeException e) {
            Log.e(e);
            return null;
        }
    }

    public Bitmap getFavicon() {
        return null;
    }

    public int getProgress() {
        return mProgress;
    }

    public int getContentHeight() {
        return getHeight();
    }

    public float getScale() {
        return 1f;
    }

    public HitTestResult getHitTestResult() {
        return new HitTestResult();
    }

    // ------------------------------------------------------------ JavaScript

    /// Evaluates `script` in the page and passes the JSON form of its value
    /// to `resultCallback` ("null" when it has none), as Android does.
    public void evaluateJavascript(String script, final ValueCallback<String> resultCallback) {
        if (mDestroyed || script == null) {
            return;
        }
        String js = "try { var __cn1r = (0, eval)(" + JsBridge.quote(script) + ");"
                + " callback.onSuccess(__cn1r === undefined ? 'null' : JSON.stringify(__cn1r)); }"
                + " catch (e) { callback.onSuccess('null'); }";
        mBrowser.execute(js, new EvaluateResult(resultCallback));
    }

    /// Exposes the `@JavascriptInterface` methods of `object` to the page's
    /// JavaScript as `window.<name>`, from the next page load on.
    public void addJavascriptInterface(Object object, String name) {
        if (object == null || name == null) {
            return;
        }
        mJsInterfaces.put(name, object);
    }

    public void removeJavascriptInterface(String name) {
        mJsInterfaces.remove(name);
        if (!mDestroyed && name != null) {
            mBrowser.execute("try { delete window[" + JsBridge.quote(name) + "]; } catch (e) {}");
        }
    }

    private void installJsInterfaces() {
        if (mJsInterfaces.isEmpty()) {
            return;
        }
        if (mJsCallback == null) {
            mJsCallback = new SuccessCallback<BrowserComponent.JSRef>() {
                @Override
                public void onSucess(BrowserComponent.JSRef value) {
                    onJsCall(value == null ? null : value.getValue());
                }
            };
        }
        for (Map.Entry<String, Object> e : mJsInterfaces.entrySet()) {
            String methods = JsInterfaceDispatch.methods(e.getValue());
            String js = "(function(){var n=" + JsBridge.quote(e.getKey()) + ",ms=" + JsBridge.quote(methods)
                    + ".split(','),o={},q=window.__cn1JsQ||(window.__cn1JsQ={});"
                    + "function mk(m){return function(){var a=[],z='';"
                    + "for(var i=0;i<arguments.length;i++){var v=arguments[i];"
                    + "if(v===null||v===undefined){a.push('');z+='1';}else{a.push(String(v));z+='0';}}"
                    + "window.__cn1JsSeq=(window.__cn1JsSeq||0)+1;var id=n+':'+window.__cn1JsSeq,p;"
                    + "if(typeof Promise!=='undefined'){p=new Promise(function(res,rej){q[id]=[res,rej];});}"
                    + "callback.onSuccess(JSON.stringify({n:n,id:id,m:m,a:a,z:z}));return p;};}"
                    + "for(var i=0;i<ms.length;i++){if(ms[i]){o[ms[i]]=mk(ms[i]);}}window[n]=o;})();";
            mBrowser.addJSCallback(js, mJsCallback);
        }
    }

    /// A call from the page: `{n: interface, id, m: method, a: args, z: nulls}`.
    private void onJsCall(String json) {
        if (json == null || mDestroyed) {
            return;
        }
        Map<String, Object> call;
        try {
            call = com.codename1.io.JSONParser.parseJSON(json);
        } catch (java.io.IOException e) {
            Log.e(e);
            return;
        }
        Object n = call.get("n");
        Object id = call.get("id");
        Object m = call.get("m");
        Object a = call.get("a");
        Object z = call.get("z");
        if (!(n instanceof String) || !(id instanceof String) || !(m instanceof String)) {
            return;
        }
        String name = (String) n;
        String callId = (String) id;
        String method = (String) m;
        String nulls = z instanceof String ? (String) z : "";
        List<Object> list = a instanceof List ? castList(a) : new ArrayList<Object>();
        String[] args = new String[list.size()];
        for (int i = 0; i < args.length; i++) {
            Object v = list.get(i);
            boolean isNull = i < nulls.length() && nulls.charAt(i) == '1';
            args[i] = isNull ? null : v == null ? "" : v.toString();
        }
        Object target = mJsInterfaces.get(n);
        String result = null;
        String error = null;
        if (target == null) {
            error = name + " is not defined";
        } else {
            try {
                result = JsInterfaceDispatch.invoke(target, method, args);
                if (result == null) {
                    error = "Method not found: " + method;
                }
            } catch (RuntimeException e) {
                Log.e(e);
                error = "Java exception was raised during method invocation";
            }
        }
        String key = JsBridge.quote(callId);
        String js = "(function(){var q=window.__cn1JsQ&&window.__cn1JsQ[" + key + "];if(q){delete window.__cn1JsQ["
                + key + "];" + (error == null ? "q[0](" + result + ");" : "q[1](new Error(" + JsBridge.quote(error)
                + "));") + "}})();";
        mBrowser.execute(js);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> castList(Object o) {
        return (List<Object>) o;
    }

    // ------------------------------------------------------------ clients

    public WebSettings getSettings() {
        return mSettings;
    }

    public void setWebViewClient(WebViewClient client) {
        mClient = client;
    }

    public WebViewClient getWebViewClient() {
        return mClient;
    }

    public void setWebChromeClient(WebChromeClient client) {
        mChromeClient = client;
    }

    public WebChromeClient getWebChromeClient() {
        return mChromeClient;
    }

    public static void setWebContentsDebuggingEnabled(boolean enabled) {
        sDebuggingEnabled = enabled;
    }

    // ------------------------------------------------------------ lifecycle

    public void onPause() {
    }

    public void onResume() {
    }

    public void pauseTimers() {
    }

    public void resumeTimers() {
    }

    public void clearCache(boolean includeDiskFiles) {
    }

    public void clearHistory() {
        if (!mDestroyed) {
            mBrowser.clearHistory();
        }
    }

    public void clearFormData() {
    }

    public void clearMatches() {
    }

    public void clearSslPreferences() {
    }

    public void setInitialScale(int scaleInPercent) {
    }

    public void setNetworkAvailable(boolean networkUp) {
    }

    public boolean zoomIn() {
        return false;
    }

    public boolean zoomOut() {
        return false;
    }

    public boolean pageUp(boolean top) {
        mBrowser.execute(top ? "window.scrollTo(0,0);" : "window.scrollBy(0,-window.innerHeight);");
        return true;
    }

    public boolean pageDown(boolean bottom) {
        mBrowser.execute(bottom ? "window.scrollTo(0,document.body.scrollHeight);"
                : "window.scrollBy(0,window.innerHeight);");
        return true;
    }

    public void destroy() {
        if (mDestroyed) {
            return;
        }
        mDestroyed = true;
        mJsInterfaces.clear();
        mBrowser.destroy();
    }

    // ------------------------------------------------------------ request

    static final class Request implements WebResourceRequest {
        private final Uri url;
        private final boolean mainFrame;
        private final boolean redirect;
        private final String method;

        Request(String url, boolean mainFrame, boolean redirect, String method) {
            this.url = Uri.parse(url);
            this.mainFrame = mainFrame;
            this.redirect = redirect;
            this.method = method;
        }

        @Override
        public Uri getUrl() {
            return url;
        }

        @Override
        public boolean isForMainFrame() {
            return mainFrame;
        }

        @Override
        public boolean isRedirect() {
            return redirect;
        }

        @Override
        public boolean hasGesture() {
            return mainFrame;
        }

        @Override
        public String getMethod() {
            return method;
        }

        @Override
        public Map<String, String> getRequestHeaders() {
            return new HashMap<String, String>();
        }
    }

    static final class ResourceError extends WebResourceError {
        private final int code;
        private final String description;

        ResourceError(int code, String description) {
            this.code = code;
            this.description = description;
        }

        @Override
        public int getErrorCode() {
            return code;
        }

        @Override
        public CharSequence getDescription() {
            return description;
        }
    }

    /// Hands evaluateJavascript's JSON result to the application's callback.
    private static final class EvaluateResult implements SuccessCallback<BrowserComponent.JSRef> {
        private final ValueCallback<String> callback;

        EvaluateResult(ValueCallback<String> callback) {
            this.callback = callback;
        }

        @Override
        public void onSucess(BrowserComponent.JSRef value) {
            if (callback != null) {
                String v = value == null ? null : value.getValue();
                callback.onReceiveValue(v == null ? "null" : v);
            }
        }
    }
}
