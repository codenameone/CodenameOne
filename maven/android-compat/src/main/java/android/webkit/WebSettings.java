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

/// A web view's settings. Every value is recorded and returned as set; the
/// ones the native browser exposes are applied to it: zoom (pinch to zoom)
/// and the user agent. JavaScript and DOM storage are always enabled in the
/// native views Codename One hosts, whatever is set here.
public abstract class WebSettings {

    public enum LayoutAlgorithm {
        NORMAL, SINGLE_COLUMN, NARROW_COLUMNS, TEXT_AUTOSIZING
    }

    public enum ZoomDensity {
        FAR, MEDIUM, CLOSE
    }

    public enum RenderPriority {
        NORMAL, HIGH, LOW
    }

    public enum PluginState {
        ON, ON_DEMAND, OFF
    }

    public static final int LOAD_DEFAULT = -1;
    public static final int LOAD_NORMAL = 0;
    public static final int LOAD_CACHE_ELSE_NETWORK = 1;
    public static final int LOAD_NO_CACHE = 2;
    public static final int LOAD_CACHE_ONLY = 3;
    public static final int MIXED_CONTENT_ALWAYS_ALLOW = 0;
    public static final int MIXED_CONTENT_NEVER_ALLOW = 1;
    public static final int MIXED_CONTENT_COMPATIBILITY_MODE = 2;
    public static final int FORCE_DARK_OFF = 0;
    public static final int FORCE_DARK_AUTO = 1;
    public static final int FORCE_DARK_ON = 2;
    public static final int MENU_ITEM_NONE = 0;
    public static final int MENU_ITEM_SHARE = 1;
    public static final int MENU_ITEM_WEB_SEARCH = 2;
    public static final int MENU_ITEM_PROCESS_TEXT = 4;

    static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Mobile Safari/537.36";

    private boolean mJavaScriptEnabled;
    private boolean mDomStorageEnabled;
    private boolean mDatabaseEnabled;
    private boolean mSupportZoom = true;
    private boolean mBuiltInZoomControls;
    private boolean mDisplayZoomControls = true;
    private boolean mLoadWithOverviewMode;
    private boolean mUseWideViewPort;
    private boolean mAllowFileAccess;
    private boolean mAllowContentAccess = true;
    private boolean mAllowFileAccessFromFileURLs;
    private boolean mAllowUniversalAccessFromFileURLs;
    private boolean mLoadsImagesAutomatically = true;
    private boolean mBlockNetworkImage;
    private boolean mBlockNetworkLoads;
    private boolean mJavaScriptCanOpenWindowsAutomatically;
    private boolean mSupportMultipleWindows;
    private boolean mMediaPlaybackRequiresUserGesture = true;
    private boolean mSafeBrowsingEnabled = true;
    private boolean mOffscreenPreRaster;
    private boolean mSaveFormData = true;
    private boolean mAlgorithmicDarkening;
    private int mCacheMode = LOAD_DEFAULT;
    private int mMixedContentMode = MIXED_CONTENT_NEVER_ALLOW;
    private int mTextZoom = 100;
    private int mDefaultFontSize = 16;
    private int mDefaultFixedFontSize = 13;
    private int mMinimumFontSize = 8;
    private int mMinimumLogicalFontSize = 8;
    private int mForceDark = FORCE_DARK_AUTO;
    private int mDisabledActionModeMenuItems = MENU_ITEM_NONE;
    private String mUserAgent;
    private String mDefaultTextEncoding = "UTF-8";
    private String mStandardFontFamily = "sans-serif";
    private String mFixedFontFamily = "monospace";
    private String mSansSerifFontFamily = "sans-serif";
    private String mSerifFontFamily = "serif";
    private String mCursiveFontFamily = "cursive";
    private String mFantasyFontFamily = "fantasy";
    private LayoutAlgorithm mLayoutAlgorithm = LayoutAlgorithm.NARROW_COLUMNS;

    protected WebSettings() {
    }

    public static String getDefaultUserAgent(Context context) {
        return DEFAULT_USER_AGENT;
    }

    /// Hook for the settings the native browser applies.
    void applyZoom(boolean enabled) {
    }

    void applyUserAgent(String userAgent) {
    }

    public void setJavaScriptEnabled(boolean flag) {
        mJavaScriptEnabled = flag;
    }

    public boolean getJavaScriptEnabled() {
        return mJavaScriptEnabled;
    }

    public void setDomStorageEnabled(boolean flag) {
        mDomStorageEnabled = flag;
    }

    public boolean getDomStorageEnabled() {
        return mDomStorageEnabled;
    }

    public void setDatabaseEnabled(boolean flag) {
        mDatabaseEnabled = flag;
    }

    public boolean getDatabaseEnabled() {
        return mDatabaseEnabled;
    }

    @Deprecated
    public void setAppCacheEnabled(boolean flag) {
    }

    @Deprecated
    public void setAppCachePath(String appCachePath) {
    }

    @Deprecated
    public void setDatabasePath(String databasePath) {
    }

    public void setSupportZoom(boolean support) {
        mSupportZoom = support;
        applyZoom(mSupportZoom && mBuiltInZoomControls);
    }

    public boolean supportZoom() {
        return mSupportZoom;
    }

    public void setBuiltInZoomControls(boolean enabled) {
        mBuiltInZoomControls = enabled;
        applyZoom(mSupportZoom && mBuiltInZoomControls);
    }

    public boolean getBuiltInZoomControls() {
        return mBuiltInZoomControls;
    }

    public void setDisplayZoomControls(boolean enabled) {
        mDisplayZoomControls = enabled;
    }

    public boolean getDisplayZoomControls() {
        return mDisplayZoomControls;
    }

    public void setLoadWithOverviewMode(boolean overview) {
        mLoadWithOverviewMode = overview;
    }

    public boolean getLoadWithOverviewMode() {
        return mLoadWithOverviewMode;
    }

    public void setUseWideViewPort(boolean use) {
        mUseWideViewPort = use;
    }

    public boolean getUseWideViewPort() {
        return mUseWideViewPort;
    }

    public void setAllowFileAccess(boolean allow) {
        mAllowFileAccess = allow;
    }

    public boolean getAllowFileAccess() {
        return mAllowFileAccess;
    }

    public void setAllowContentAccess(boolean allow) {
        mAllowContentAccess = allow;
    }

    public boolean getAllowContentAccess() {
        return mAllowContentAccess;
    }

    public void setAllowFileAccessFromFileURLs(boolean flag) {
        mAllowFileAccessFromFileURLs = flag;
    }

    public boolean getAllowFileAccessFromFileURLs() {
        return mAllowFileAccessFromFileURLs;
    }

    public void setAllowUniversalAccessFromFileURLs(boolean flag) {
        mAllowUniversalAccessFromFileURLs = flag;
    }

    public boolean getAllowUniversalAccessFromFileURLs() {
        return mAllowUniversalAccessFromFileURLs;
    }

    public void setLoadsImagesAutomatically(boolean flag) {
        mLoadsImagesAutomatically = flag;
    }

    public boolean getLoadsImagesAutomatically() {
        return mLoadsImagesAutomatically;
    }

    public void setBlockNetworkImage(boolean flag) {
        mBlockNetworkImage = flag;
    }

    public boolean getBlockNetworkImage() {
        return mBlockNetworkImage;
    }

    public void setBlockNetworkLoads(boolean flag) {
        mBlockNetworkLoads = flag;
    }

    public boolean getBlockNetworkLoads() {
        return mBlockNetworkLoads;
    }

    public void setJavaScriptCanOpenWindowsAutomatically(boolean flag) {
        mJavaScriptCanOpenWindowsAutomatically = flag;
    }

    public boolean getJavaScriptCanOpenWindowsAutomatically() {
        return mJavaScriptCanOpenWindowsAutomatically;
    }

    public void setSupportMultipleWindows(boolean support) {
        mSupportMultipleWindows = support;
    }

    public boolean supportMultipleWindows() {
        return mSupportMultipleWindows;
    }

    public void setMediaPlaybackRequiresUserGesture(boolean require) {
        mMediaPlaybackRequiresUserGesture = require;
    }

    public boolean getMediaPlaybackRequiresUserGesture() {
        return mMediaPlaybackRequiresUserGesture;
    }

    /// Accepted; the browser decides whether pages may ask for the location.
    public void setGeolocationEnabled(boolean flag) {
    }

    public void setSafeBrowsingEnabled(boolean enabled) {
        mSafeBrowsingEnabled = enabled;
    }

    public boolean getSafeBrowsingEnabled() {
        return mSafeBrowsingEnabled;
    }

    public void setOffscreenPreRaster(boolean enabled) {
        mOffscreenPreRaster = enabled;
    }

    public boolean getOffscreenPreRaster() {
        return mOffscreenPreRaster;
    }

    /// Accepted; the browser does not take focus on its own.
    public void setNeedInitialFocus(boolean flag) {
    }

    @Deprecated
    public void setSaveFormData(boolean save) {
        mSaveFormData = save;
    }

    @Deprecated
    public boolean getSaveFormData() {
        return mSaveFormData;
    }

    @Deprecated
    public void setSavePassword(boolean save) {
    }

    @Deprecated
    public void setRenderPriority(RenderPriority priority) {
    }

    @Deprecated
    public void setPluginState(PluginState state) {
    }

    public void setAlgorithmicDarkeningAllowed(boolean allow) {
        mAlgorithmicDarkening = allow;
    }

    public boolean isAlgorithmicDarkeningAllowed() {
        return mAlgorithmicDarkening;
    }

    @Deprecated
    public void setForceDark(int forceDark) {
        mForceDark = forceDark;
    }

    @Deprecated
    public int getForceDark() {
        return mForceDark;
    }

    public void setCacheMode(int mode) {
        mCacheMode = mode;
    }

    public int getCacheMode() {
        return mCacheMode;
    }

    public void setMixedContentMode(int mode) {
        mMixedContentMode = mode;
    }

    public int getMixedContentMode() {
        return mMixedContentMode;
    }

    public void setTextZoom(int textZoom) {
        mTextZoom = textZoom;
    }

    public int getTextZoom() {
        return mTextZoom;
    }

    public void setDefaultFontSize(int size) {
        mDefaultFontSize = size;
    }

    public int getDefaultFontSize() {
        return mDefaultFontSize;
    }

    public void setDefaultFixedFontSize(int size) {
        mDefaultFixedFontSize = size;
    }

    public int getDefaultFixedFontSize() {
        return mDefaultFixedFontSize;
    }

    public void setMinimumFontSize(int size) {
        mMinimumFontSize = size;
    }

    public int getMinimumFontSize() {
        return mMinimumFontSize;
    }

    public void setMinimumLogicalFontSize(int size) {
        mMinimumLogicalFontSize = size;
    }

    public int getMinimumLogicalFontSize() {
        return mMinimumLogicalFontSize;
    }

    public void setDisabledActionModeMenuItems(int menuItems) {
        mDisabledActionModeMenuItems = menuItems;
    }

    public int getDisabledActionModeMenuItems() {
        return mDisabledActionModeMenuItems;
    }

    public void setUserAgentString(String ua) {
        mUserAgent = ua;
        applyUserAgent(ua == null || ua.length() == 0 ? DEFAULT_USER_AGENT : ua);
    }

    public String getUserAgentString() {
        return mUserAgent == null || mUserAgent.length() == 0 ? DEFAULT_USER_AGENT : mUserAgent;
    }

    public void setDefaultTextEncodingName(String encoding) {
        mDefaultTextEncoding = encoding;
    }

    public String getDefaultTextEncodingName() {
        return mDefaultTextEncoding;
    }

    public void setStandardFontFamily(String font) {
        mStandardFontFamily = font;
    }

    public String getStandardFontFamily() {
        return mStandardFontFamily;
    }

    public void setFixedFontFamily(String font) {
        mFixedFontFamily = font;
    }

    public String getFixedFontFamily() {
        return mFixedFontFamily;
    }

    public void setSansSerifFontFamily(String font) {
        mSansSerifFontFamily = font;
    }

    public String getSansSerifFontFamily() {
        return mSansSerifFontFamily;
    }

    public void setSerifFontFamily(String font) {
        mSerifFontFamily = font;
    }

    public String getSerifFontFamily() {
        return mSerifFontFamily;
    }

    public void setCursiveFontFamily(String font) {
        mCursiveFontFamily = font;
    }

    public String getCursiveFontFamily() {
        return mCursiveFontFamily;
    }

    public void setFantasyFontFamily(String font) {
        mFantasyFontFamily = font;
    }

    public String getFantasyFontFamily() {
        return mFantasyFontFamily;
    }

    public void setLayoutAlgorithm(LayoutAlgorithm l) {
        mLayoutAlgorithm = l;
    }

    public LayoutAlgorithm getLayoutAlgorithm() {
        return mLayoutAlgorithm;
    }
}
