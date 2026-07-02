/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.impl.ios;

import com.codename1.contacts.Contact;
import com.codename1.payment.Product;
import com.codename1.social.GoogleImpl;
import com.codename1.social.LoginCallback;
import com.codename1.ui.geom.Rectangle;
import com.codename1.util.SuccessCallback;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Abstraction of the underlying native API's
 *
 * @author Shai Almog
 */
public final class IOSNative {

    native long beginBackgroundTask();

    native void endBackgroundTask(long taskId);
    
    
    //native void startMainThread(Runnable r);
    native void initVM();

    /// Returns true on iOS builds compiled with -Dios.metal=true (i.e.
    /// CN1_USE_METAL is defined in CN1ES2compat.h). Java-side code that
    /// needs to branch between the GL and Metal mutable-image rendering
    /// paths queries this once at init -- there is no other reliable
    /// source of truth on the Java side since the build flag only
    /// affects native compilation.
    native boolean isMetalRendering();
    static native void deinitializeVM();
    native boolean isPainted();
    native int getDisplayWidth();
    native int getDisplayHeight();
    native void editStringAt(int x, int y, int w, int h, long peer, boolean singleLine,
            int rows, int maxSize, int constraint, String text, boolean forceSlideUp,
            int color, long imagePeer, int padTop, int padBottom, int padLeft, int padRight,
            String hint, int hintColor, boolean showToolbar, boolean blockCopyPaste, int alignment, int verticalAlignment,
            boolean returnExitsEditing);
    native void resizeNativeTextView(int x, int y, int w, int h, int padTop, int padRight, int padBottom, int padLeft);
    native void flushBuffer(long peer, int x, int y, int width, int height);
    native void imageRgbToIntArray(long imagePeer, int[] arr, int x, int y, int width, int height, int imgWidth, int imgHeight);
    native long createImageFromARGB(int[] argb, int width, int height);
    native long createImage(byte[] data, int[] widthHeight);
    native long createImageNSData(long nsData, int[] widthHeight);
    native long scale(long peer, int width, int height);
    native void setNativeClippingMutable(int x, int y, int width, int height, boolean firstClip);
    native void setNativeClippingGlobal(int x, int y, int width, int height, boolean firstClip);
    native void setAntiAliasedMutable(boolean antialiased) ;

    native void nativeDrawLineMutable(int color, int alpha, int x1, int y1, int x2, int y2);
    native void nativeDrawLineGlobal(int color, int alpha, int x1, int y1, int x2, int y2);
    // Queues a live-screen backdrop-filter:blur op (real glass). Enqueued in paint
    // order; the drain blurs the already-drawn screenTexture region and draws it back.
    native void nativeBlurScreenRegion(int x, int y, int width, int height, float radius);
    // Queues a live-screen "Liquid Glass" MATERIAL op (the full backdrop-filter
    // recipe -- material + blur + rounded-rect mask + refraction + specular),
    // matching the offscreen IOSImplementation.glassRegion. Enqueued in paint order.
    native void nativeGlassScreenRegion(int x, int y, int width, int height, float radius, float cornerRadius, float sat, float scale, float offset, float refract, float specular);
    // Queues a live-screen iOS 26 selection-drop LENS op (magnify + chromatic
    // aberration + dark->accent tint over the painted content). See lensScreenRegionX.
    native void nativeLensScreenRegion(int x, int y, int width, int height, float cornerRadius, float magnify, float aberration, int tintColor, float tintStrength);
    // Renders an Apple SF Symbol to a GLUIImage peer (iOS 13+). Returns 0 when the
    // symbol is unavailable; writes the pixel width/height into widthHeight[0]/[1].
    native long nativeCreateSFSymbol(String name, int color, float size, int weight, int[] widthHeight);
    native void nativeFillRectMutable(int color, int alpha, int x, int y, int width, int height);
    native void nativeFillRectGlobal(int color, int alpha, int x, int y, int width, int height);
    native void nativeDrawRectMutable(int color, int alpha, int x, int y, int width, int height);
    native void nativeDrawRectGlobal(int color, int alpha, int x, int y, int width, int height);
    native void nativeDrawRoundRectMutable(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight);
    native void nativeDrawRoundRectGlobal(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight);
    native void nativeFillRoundRectMutable(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight);
    native void nativeFillRoundRectGlobal(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight);
    native void nativeFillArcMutable(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle);
    native void nativeDrawArcMutable(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle);
    native void nativeDrawStringMutable(int color, int alpha, long fontPeer, String str, int x, int y);
    native void nativeDrawStringGlobal(int color, int alpha, long fontPeer, String str, int x, int y);
    native void nativeDrawImageMutable(long peer, int alpha, int x, int y, int width, int height, int renderingHints);
    native void nativeDrawImageGlobal(long peer, int alpha, int x, int y, int width, int height, int renderingHints);
    native void nativeTileImageGlobal(long peer, int alpha, int x, int y, int width, int height);
    native int stringWidthNative(long peer, String str);
    native int charWidthNative(long peer, char ch);
    native int getFontHeightNative(long peer);
    native int fontAscentNative(long peer);
    native int fontDescentNative(long peer);
    native long createSystemFont(int face, int style, int size);
    byte[] loadResource(String name, String type) {
        int val = getResourceSize(name, type);
        if(val < 0) {
            return null;
        }
        byte[] data = new byte[val];
        loadResource(name, type, data);
        return data;
    }
    native int getResourceSize(String name, String type);
    native void loadResource(String name, String type, byte[] data);

    native long createNativeMutableImage(int w, int h, int color);

    native void startDrawingOnImage(int w, int h, long peer);
    native long finishDrawingOnImage();

    native void deleteNativePeer(long peer);
    native void deleteNativeFontPeer(long peer);

    native void resetAffineGlobal();

    native void scaleGlobal(float x, float y);

    native void rotateGlobal(float angle);
    native void rotateGlobal(float angle, int x, int y);
    /*
    native void translateGlobal(int x, int y);
    native int getTranslateXGlobal();
    native int getTranslateYGlobal();
    */

    native void shearGlobal(float x, float y);

    native void fillRectRadialGradientGlobal(int startColor, int endColor, int x, int y, int width, int height, float relativeX, float relativeY, float relativeSize);

    native void fillLinearGradientGlobal(int startColor, int endColor, int x, int y, int width, int height, boolean horizontal);

    native void fillRectRadialGradientMutable(int startColor, int endColor, int x, int y, int width, int height, float relativeX, float relativeY, float relativeSize);

    native void fillLinearGradientMutable(int startColor, int endColor, int x, int y, int width, int height, boolean horizontal);

    /// Metal-only multi-stop gradient bridge to CN1MetalFillGradient. positions
    /// holds stopCount entries in [0, 1]; premultipliedRgba holds stopCount * 4
    /// floats. On GL builds this method is a no-op. mutable is true when the
    /// fill targets the current mutable image's offscreen MTLTexture.
    native void fillGradient(int kind, int stopCount, float[] positions, float[] premultipliedRgba,
                             int cycleMethod, float angleOrFromAngle,
                             float cx, float cy, float rx, float ry, int shape,
                             int x, int y, int width, int height, boolean mutable);

    native boolean isTablet();
    native boolean isIOS7();
    native boolean isRunningOnMac();

    // Returns true when the binary is running on the watchOS slice. Implemented
    // natively via the TARGET_OS_WATCH compile-time check so the iOS slice keeps
    // returning false with zero runtime cost.
    native boolean isRunningOnWatch();

    // Returns true when the binary is running on the tvOS slice. Implemented
    // natively via the TARGET_OS_TV compile-time check so the iOS slice keeps
    // returning false with zero runtime cost.
    native boolean isRunningOnTV();

    // Mac native (Catalyst): set the host window title bar text from the current form title.
    native void setWindowTitle(String title);

    // Mac native (Catalyst): replace the application menu's CN1 command items. namesNewlineJoined
    // holds the visible command labels separated by '\n'; selecting item i calls back into
    // IOSImplementation.fireMacMenuCommand(i).
    native void setNativeMenuCommands(String namesNewlineJoined);

    // Mac native: propagate the current form's brightness to the host
    // NSWindow's appearance so the Mac titlebar (rendered by AppKit, not
    // CN1) matches the app's dark/light theme. A no-op on iOS/iPadOS.
    native void setMacWindowDarkAppearance(boolean dark);

    // Mac native (Catalyst): undecorate the host window for the "custom" desktop title-bar mode -
    // hide the AppKit title bar (transparent + hidden title + full-size content view) so the CN1
    // Toolbar acts as the window title bar, and make the window movable by its background so the
    // toolbar drags it. Passing false restores the standard titled window. A no-op on iOS/iPadOS.
    native void setMacWindowUndecorated(boolean undecorated);
    
    native void setImageName(long nativeImage, String name);
    
    native boolean canExecute(String url);
    native void execute(String url);

    native void flashBacklight(int duration);
    
    native boolean isLargerTextEnabled();
    native float getLargerTextScale();

    // SJH Nov. 17, 2015 : Removing native isMinimized() method because it conflicted with
    // tracking on the java side.  It caused the app to still be minimized inside start()
    // method.  
    // Related to this issue https://groups.google.com/forum/?utm_medium=email&utm_source=footer#!msg/codenameone-discussions/Ajo2fArN8mc/KrF_e9cTDwAJ
    //native boolean isMinimized();
    
    native boolean minimizeApplication();

    native void restoreMinimizedApplication();

    native void lockOrientation(boolean portrait);
    native void unlockOrientation();
    native void lockScreen();
    native void unlockScreen();
    native void setDisableScreenshots(boolean disable);

    native void vibrate(int duration);

    native boolean isMotionSensorSupported(int type);

    native void startMotionSensor(int type, int rateMillis);

    native void stopMotionSensor(int type);

    native boolean hasMotionData(int type);

    native float getMotionSensorX(int type);

    native float getMotionSensorY(int type);

    native float getMotionSensorZ(int type);

    native int getAudioDuration(long peer);

    native void playAudio(long peer);

    native int getAudioTime(long peer);

    native void pauseAudio(long peer);

    native void setAudioTime(long peer, int time);
    native boolean isAudioPlaying(long peer);

    native void cleanupAudio(long peer);

    native long createAudio(String uri, Runnable onCompletion);

    native long createAudio(byte[] data, Runnable onCompletion);

    // ---- low latency game sound pool (com.codename1.gaming.SoundPool) ----
    native long nativeCreateSoundPool(int maxStreams);
    native long nativeLoadSound(long pool, byte[] data, int ringSize);
    native int nativePlaySound(long pool, long sound, float volume, float pan, float rate, int loop);
    native void nativeSetSoundVolume(long pool, int voiceId, float volume);
    native void nativeSetSoundRate(long pool, int voiceId, float rate);
    native void nativeSetSoundPan(long pool, int voiceId, float pan);
    native void nativePauseSound(long pool, int voiceId);
    native void nativeResumeSound(long pool, int voiceId);
    native void nativeStopSound(long pool, int voiceId);
    native void nativeStopAllSounds(long pool);
    native void nativeAutoPauseSoundPool(long pool);
    native void nativeAutoResumeSoundPool(long pool);
    native void nativeUnloadSound(long pool, long sound);
    native void nativeReleaseSoundPool(long pool);

    native float getVolume();

    native void setVolume(float vol);
    
    // Peer Component methods
    
    native void calcPreferredSize(long peer, int w, int h, int[] response);

    native void updatePeerPositionSize(long peer, int x, int y, int w, int h);
    
    native void peerInitialized(long peer, int x, int y, int w, int h);

    native void peerDeinitialized(long peer);
    native void peerSetVisible(long peer, boolean v);
    native long createPeerImage(long peer, int[] wh);

    native void releasePeer(long peer);
    native void retainPeer(long peer);

    native void setClipboardString(String s);
    native String getClipboardString();
    
    native void setPinchToZoomEnabled(long peer, boolean e);
    native void setNativeBrowserScrollingEnabled(long peer, boolean e);
    
    // Creates a UIWebView
    native long createBrowserComponent(Object bc);
    
    // Creates a WKWebView
    native long createWKBrowserComponent(Object browserComponent);
    native void setBrowserPage(long browserPeer, String html, String baseUrl);

    native void setBrowserURL(long browserPeer, String url);
    native void setBrowserURL(long browserPeer, String url, String[] keys, String[] values);
    
    native void setBrowserUserAgent(long browserPeer, String ua);
    native void setBrowserFollowTargetBlank(long browserPeer, boolean follow);
    // style: 0 = unspecified/auto (follow device), 1 = light, 2 = dark
    native void setBrowserInterfaceStyle(long browserPeer, int style);
    
    native void browserBack(long browserPeer);
    native void browserStop(long browserPeer);

    native void browserClearHistory(long browserPeer);

    native void browserExecute(long browserPeer, String javaScript);
    native void browserExecuteAndReturnStringCallback(long browserPeer, String javaScript, SuccessCallback<String> callback);
    native String browserExecuteAndReturnString(long browserPeer, String javaScript);
    
    native void browserForward(long browserPeer);

    native boolean browserHasBack(long browserPeer);

    native boolean browserHasForward(long browserPeer);

    native void browserReload(long browserPeer);

    native String getBrowserTitle(long browserPeer);

    native String getBrowserURL(long browserPeer);
    
    native long createVideoComponent(String url, int onCompletionCallbackId);
    native long createVideoComponent(byte[] video, int onCompletionCallbackId);
    native long createVideoComponentNSData(long video, int onCompletionCallbackId);
    native long createNativeVideoComponent(String url, int onCompletionCallbackId);
    native long createNativeVideoComponent(byte[] video, int onCompletionCallbackId);
    native long createNativeVideoComponentNSData(long video, int onCompletionCallbackId);

    native void startVideoComponent(long peer); 
    
    native void stopVideoComponent(long peer);
    native void pauseVideoComponent(long peer);
    native void prepareVideoComponent(long moviePlayerPeer);

    native int getMediaTimeMS(long peer);
    
    native int setMediaTimeMS(long peer, int now);

    native int getMediaDuration(long peer);
    
    native void setMediaBgArtist(String artist);
    native void setMediaBgTitle(String title);
    native void setMediaBgDuration(long duration);
    native void setMediaBgPosition(long position);
    native void setMediaBgAlbumCover(long cover);
    native void setNativeVideoControlsEmbedded(long peer, boolean value);
    
    native boolean isVideoPlaying(long peer);

    native void setVideoFullScreen(long peer, boolean fullscreen);

    native boolean isVideoFullScreen(long peer);

    native long getVideoViewPeer(long peer);
    
    native void showNativePlayerController(long peer);
    
    // IO methods

    native int writeToFile(byte[] data, String path);
    native int appendToFile(byte[] data, String path);
    native int getFileSize(String path);
    native long getFileLastModified(String path);
    native void readFile(String path, byte[] bytes);

    native String getDocumentsDir();
    native String getCachesDir();
    native String getResourcesDir();
    native void deleteFile(String file);
    native boolean fileExists(String file);
    native boolean isDirectory(String file);

    native boolean isDarkMode();
    native boolean isDarkModeDetectionSupported();
    native boolean isVPNActive();

    // Active-network type queries used by NetworkManager.getCurrentNetworkType
    // and addNetworkTypeListener. Returns one of
    // NetworkManager.NETWORK_TYPE_* constants. Implementation uses
    // SCNetworkReachability (always available) and an interface-name probe to
    // distinguish WiFi from cellular.
    native int wifiNetworkType();
    native void wifiInstallTypeListener(Object instance);
    native void wifiUninstallTypeListener();

    // WiFi info; SSID/BSSID require the wifi-info entitlement and (since iOS
    // 13) a granted CoreLocation authorization. The build pipeline injects
    // both automatically when WiFi.getCurrentSSID/getBSSID is on the
    // classpath. Returns null when permission denied or not on WiFi.
    native String wifiCurrentSSID();
    native String wifiCurrentBSSID();
    native String wifiGateway();
    native String wifiIpAddress();

    // NEHotspotConfiguration-backed join. Requires the
    // com.apple.developer.networking.HotspotConfiguration entitlement
    // (injected by IPhoneBuilder when com.codename1.io.wifi.WiFi.connect is
    // referenced). The result is delivered via
    // com.codename1.impl.ios.IOSConnectivity.wifiConnectResult.
    native void wifiConnect(String ssid, String password, int security);
    native void wifiDisconnect(String ssid);

    // NSNetServiceBrowser-backed Bonjour discovery. Callbacks land in
    // com.codename1.impl.ios.IOSConnectivity.bonjour* static dispatchers.
    native long bonjourBrowseStart(String type);
    native void bonjourBrowseStop(long handle);
    native long bonjourPublishStart(String name, String type, int port, String[] txtKeys, String[] txtVals);
    native void bonjourPublishStop(long handle);

    native int fileCountInDir(String dir);
    native void listFilesInDir(String dir, String[] files);
    native void createDirectory(String dir);
    native void moveFile(String src, String dest);
    
    native long openConnection(String url, int timeout);
    native void connect(long peer);
    native String getSSLCertificates(long peer);
    native void setMethod(long peer, String mtd);
    native void setChunkedStreamingMode(long peer, int len);
    native int getResponseCode(long peer);

    native String getResponseMessage(long peer);

    native int getContentLength(long peer);

    native String getResponseHeader(long peer, String name);
    native int getResponseHeaderCount(long peer);
    native String getResponseHeaderName(long peer, int offset);

    native void addHeader(long peer, String key, String value);

    native void setBody(long peer, byte[] arr);  
    
    native void setBody(long peer, String file);
    
    native void closeConnection(long peer);
    
    native String getUDID();
    native String getOSVersion();
    native String getDeviceName();
    // The hardware/marketing model identifier (e.g. "iPhone15,2"). Unlike
    // getDeviceName() -- which returns the user-assigned device name and is
    // therefore personally identifying -- this is safe to use for analytics
    // device segmentation.
    native String getDeviceHardwareModel();

    // Diagnostics for the status-bar tap-to-scroll-to-top path. Surfaced to
    // user code via Display.getProperty("cn1.iosStatusBarTap.*") in
    // IOSImplementation. Lets developers detect on-device whether iOS is
    // delivering the scroll-to-top message at all when the gesture does
    // nothing visibly.
    native int getStatusBarTapCount();
    native long getStatusBarTapLastEpochMillis();
    native int getStatusBarTapLastX();
    native int getStatusBarTapLastY();
    native boolean isStatusBarTapProxyInstalled();
    
    // location manager
    native boolean isGPSEnabled();
    native long createCLLocation();
    native boolean isGoodLocation(long clLocation);
    native long getCurrentLocationObject(long clLocation);
    native double getLocationLatitude(long location);
    native double getLocationAltitude(long location);
    native double getLocationLongtitude(long location);
    native double getLocationAccuracy(long location);
    native double getLocationDirection(long location);
    native double getLocationVelocity(long location);
    native long getLocationTimeStamp(long location);

    native void startUpdatingLocation(long clLocation, int priority);
    native void stopUpdatingLocation(long clLocation);
    native void startUpdatingBackgroundLocation(long clLocation);
    native void stopUpdatingBackgroundLocation(long clLocation);
    
    native void addGeofencing(long clLocation, double lat, double lng, double radius, long expiration, String id);
    native void removeGeofencing(long clLocation, String id);
    
    // capture
    native void captureCamera(boolean movie, int quality, int duration);
    native void openGallery(int type);

    // Low-level camera API (com.codename1.camera). Backed by CN1Camera.m
    // which wraps AVCaptureSession. The IOSCameraImpl class on the Java side
    // routes static callbacks delivered from the capture queue.
    native String cn1CameraEnumerate();
    native long cn1CameraOpen(String cameraId, int previewW, int previewH, boolean captureAudio);
    native long cn1CameraCreatePreviewView(long sessionPeer);
    native void cn1CameraTakePhoto(long sessionPeer, int width, int height, int jpegQuality, String filePath, int callbackId);
    native boolean cn1CameraStartVideo(long sessionPeer, String filePath, boolean captureAudio);
    native void cn1CameraStopVideo(long sessionPeer, int callbackId);
    native void cn1CameraSetFrameDelivery(long sessionPeer, boolean enabled, int maxFps);
    native void cn1CameraSetFlash(long sessionPeer, int mode);
    native void cn1CameraSetZoom(long sessionPeer, float ratio);
    native void cn1CameraFocus(long sessionPeer, float xNorm, float yNorm);
    native void cn1CameraPause(long sessionPeer);
    native void cn1CameraResume(long sessionPeer);
    native void cn1CameraClose(long sessionPeer);

    // ---------------------------------------------------------------------
    // Portable 3D API (com.codename1.gpu) Metal backend. Backed by CN1GL3D.m.
    // Buffers are created over SIMD aligned Java arrays so Metal can wrap them
    // with newBufferWithBytesNoCopy (zero copy) where possible. Handles are the
    // corresponding Objective-C / Metal object pointers cast to long.
    // ---------------------------------------------------------------------

    // Creates the native Metal 3D context hosting an MTKView; returns a context
    // handle (CN1GL3D pointer cast to long) or 0 if Metal is unavailable.
    native long gl3dCreateContext();
    // Returns the UIView peer handle for the context's MTKView, hosted as a
    // NativeIPhoneView peer.
    native long gl3dGetViewPeer(long contextPeer);
    native void gl3dDestroyContext(long contextPeer);
    native void gl3dSetContinuous(long contextPeer, boolean continuous);
    native void gl3dRequestRender(long contextPeer);

    // Resource creation / update. floatCount / indexCount are element counts.
    native long gl3dCreateFloatBuffer(float[] data, int floatCount);
    native void gl3dUpdateFloatBuffer(long bufferPeer, float[] data, int floatCount);
    native long gl3dCreateShortBuffer(short[] data, int indexCount);
    native void gl3dUpdateShortBuffer(long bufferPeer, short[] data, int indexCount);
    native long gl3dCreateTexture(int[] argb, int width, int height);
    native void gl3dDisposeBuffer(long bufferPeer);
    native void gl3dDisposeTexture(long texturePeer);
    native void gl3dDisposePipeline(long pipelinePeer);

    // Compiles the supplied MSL source (once) and builds a MTLRenderPipelineState
    // for the given blend/cull/depth state. Returns the pipeline handle or 0.
    native long gl3dGetOrCreatePipeline(long contextPeer, String key, String mslSource,
            int blendMode, int cullMode, int depthTest, int depthWrite);

    native void gl3dClear(long contextPeer, int argbColor, boolean clearColor, boolean clearDepth);
    native void gl3dSetViewport(long contextPeer, int x, int y, int width, int height);

    native void gl3dDrawIndexed(long contextPeer, long pipelinePeer, long vboPeer, int strideBytes,
            long iboPeer, int indexCount, int primitive, float[] uniforms, int uniformFloats,
            long texturePeer, int texFilter, int texWrap);
    native void gl3dDrawArrays(long contextPeer, long pipelinePeer, long vboPeer, int strideBytes,
            int vertexCount, int primitive, float[] uniforms, int uniformFloats,
            long texturePeer, int texFilter, int texWrap);

    native void destroyAudioUnit(long peer);

    native long createAudioUnit(String path, int audioChannels, float sampleRate, float[] f);

    
    native void startAudioUnit(long audioUnit);
    native void stopAudioUnit(long audioUnit);
    
    native long createAudioRecorder(final String path, final String mimeType, final int sampleRate, final int bitRate, final int audioChannels, final int maxDuration);
    native void startAudioRecord(long peer);
    native void pauseAudioRecord(long peer);
    native void cleanupAudioRecord(long peer);

    native void sendEmailMessage(String[] recipients, String subject, String content, String[] attachment, String[] attachmentMimeType, boolean htmlMail);

    native boolean isContactsPermissionGranted();
    native int getContactCount(boolean withNumbers);
    native void getContactRefIds(int[] refs, boolean withNumbers);
    native void updatePersonWithRecordID(int id, Contact cnt, boolean includesFullName, boolean includesPicture, boolean includesNumbers, boolean includesEmail, boolean includeAddress);
    native long getPersonWithRecordID(int id);
    native String getPersonFirstName(long id);
    native String getPersonSurnameName(long id);
    native int getPersonPhoneCount(long id);
    native String getPersonPhone(long id, int offset);
    native String getPersonPhoneType(long id, int offset);
    native String getPersonPrimaryPhone(long id);
    native String getPersonEmail(long id);
    native String getPersonAddress(long id);
    native long createPersonPhotoImage(long id);
    native String createContact(String firstName, String surname, String officePhone, String homePhone, String cellPhone, String email);
    native boolean deleteContact(int id);
    
    native void dial(String phone);
    native void requestAppStoreReview();
    native void sendSMS(String phone, String text);

    native void registerPush();

    native void deregisterPush();
    native void setBadgeNumber(int number);

    native long createImageFile(long imagePeer, boolean jpeg, int width, int height, float quality);
    native int getNSDataSize(long nsData);
    native void nsDataToByteArray(long nsData, byte[] data);

    native long createNSData(String file);
    native long createNSDataResource(String name, String type);
    native int read(long nsData, int pointer);
    native void read(long nsData, byte[] destination, int offset, int length, int pointer);
    
    native boolean sqlDbExists(String name);
    native long sqlDbCreateAndOpen(String name);
    native void sqlDbDelete(String name);
    native void sqlDbClose(long db);

    native void sqlDbExec(long dbPeer, String sql, String[] args);

    native long sqlDbExecQuery(long dbPeer, String sql, String[] args);

    native boolean sqlCursorFirst(long statementPeer);
    native boolean sqlCursorNext(long statementPeer);
    native String sqlGetColName(long statementPeer, int index);
    native void sqlCursorCloseStatement(long statement);

    native byte[] sqlCursorValueAtColumnBlob(long statement, int col);
    native double sqlCursorValueAtColumnDouble(long statement, int col);
    native float sqlCursorValueAtColumnFloat(long statement, int col);
    native int sqlCursorValueAtColumnInteger(long statement, int col);
    native long sqlCursorValueAtColumnLong(long statement, int col);
    native short sqlCursorValueAtColumnShort(long statement, int col);
    native String sqlCursorValueAtColumnString(long statement, int col);
    native boolean sqlCursorNullValueAtColumn(long statement, int col); //Warning. This function only works if no automatic type conversions have occurred for the value in question. So it must be called before any of the sqlCursorValueAtColumn* methods. After a type conversion, the result of calling this method is undefined, though harmless
    
    native int sqlCursorGetColumnCount(long statementPeer);
    
    native void fetchProducts(String[] skus, Product[] products);
    native void purchase(String sku);
    native boolean canMakePayments();
    native void restorePurchases();
    native void zoozPurchase(double amount, String currency, String appKey, boolean sandbox, String invoiceNumber);

    native void setLocale(String localeStr);
    native String formatInt(int i);
    native String formatDouble(double d);
    native String formatCurrency(double d);
    native String formatDate(long date);
    native String formatDateShort(long date);
    native String formatDateTime(long date);
    native double parseDouble(String localeFormattedDecimal);
    native String formatDateTimeMedium(long date);
    native String formatDateTimeShort(long date);
    native String getLongMonthName(long time);
    native String getShortMonthName(long time);
    native String getCurrencySymbol();
    
    native void scanQRCode();
    native void scanBarCode();

    native long createTruetypeFont(String name);
    native long deriveTruetypeFont(long uiFont, boolean bold, boolean italic, float size);

    native void log(String text);

    native void addCookie(String key, String value, String domain, String path, boolean secure, boolean httpOnly, long expires);
    native void getCookiesForURL(String url, Vector out);

    native String getUserAgentString(String callbackId);
    
    native void openDatePicker(int type, long time, int x, int y, int w, int h, int preferredWidth, int preferredHeight, int minuteStep);
    native void openStringPicker(String[] stringArray, int selection, int x, int y, int w, int h, int preferredWidth, int preferredHeight);
    
    native void socialShare(String text, long imagePeer, Rectangle sourceRect);

    // Same as socialShare but reports the outcome via
    // IOSImplementation.socialShareCallback(int, String, String) using
    // the supplied callbackId. Status: 1=SHARED_TO, 2=DISMISSED, 3=FAILED.
    native void socialShareWithCallback(String text, long imagePeer, Rectangle sourceRect, int callbackId);

    // Printing via UIPrintInteractionController
    native boolean isPrintingAvailable();

    // Prints the document at path and reports the outcome via
    // IOSImplementation.printDocumentCallback(int, int, String) using
    // the supplied callbackId. Status: 1=COMPLETED, 2=CANCELLED, 3=FAILED.
    native void printDocument(String path, String mimeType, int callbackId);

    // facebook connect
    public native void facebookLogin(Object callback);
    public native boolean isFacebookLoggedIn();
    public native String getFacebookToken();
    public native void facebookLogout();
    public native boolean askPublishPermissions(LoginCallback lc);
    public native boolean hasPublishPermissions();

    // OidcClient / SystemBrowser -- ASWebAuthenticationSession (iOS 12+).
    // See nativeSources/CN1OidcBrowser.m for the Obj-C side.
    public native boolean oidcSystemBrowserSupported();
    public native String oidcStartAuthorization(String authUrl, String redirectScheme);

    // AppleSignIn -- ASAuthorizationAppleIDProvider (iOS 13+).
    // See nativeSources/CN1AppleSignIn.m for the Obj-C side.
    public native boolean appleSignInSupported();
    public native String appleSignIn(String scopes, String nonce);
    public native boolean appleSignInIsLoggedIn();
    public native void appleSignInSignOut();

    // Crash protection -- see nativeSources/CN1CrashProtection.m.
    // crashProtectionInstall() is idempotent; hooks SIGSEGV/SIGABRT/
    // SIGBUS/SIGILL/SIGFPE/SIGPIPE/SIGTRAP plus
    // NSSetUncaughtExceptionHandler, writes a JSON record to the
    // documents directory before the process dies.
    // crashProtectionLogSnapshot() returns the recent stderr/NSLog
    // ring buffer (~32 KB cap). crashProtectionConsumePending() reads
    // and deletes the pending-crash JSON written on a prior launch.
    public native void crashProtectionInstall();
    public native String crashProtectionLogSnapshot();
    public native String crashProtectionConsumePending();

    // WebAuthn / passkeys --
    // ASAuthorizationPlatformPublicKeyCredentialProvider (iOS 16+).
    // See nativeSources/CN1WebAuthn.m for the Obj-C side.
    public native boolean webauthnSupported();
    public native String webauthnCreate(String optionsJson);
    public native String webauthnGet(String optionsJson);


    
    public native boolean isAsyncEditMode();
    public native void setAsyncEditMode(boolean b);
    public native void foldVKB();
    public native void hideTextEditing();
    public native int getVKBHeight();
    public native int getVKBWidth();

    public native long connectSocket(String host, int port, int connectTimeout);
    public native String getHostOrIP();
    public native void disconnectSocket(long socket);
    public native boolean isSocketConnected(long socket);
    public native String getSocketErrorMessage(long socket);
    public native int getSocketErrorCode(long socket);
    public native int getSocketAvailableInput(long socket);
    public native byte[] readFromSocketStream(long socket);
    public native void writeToSocketStream(long socket, byte[] data);
    public native void writeToSocketStream(long socket, byte[] data, int offset, int len);

    public native long createWebSocketNative(int connectionId, String url);
    public native void connectWebSocketNative(long handle, int connectTimeoutMs, String subprotocolsCsv);
    public native void closeWebSocketNative(long handle);
    public native void sendWebSocketTextNative(long handle, String text);
    public native void sendWebSocketBinaryNative(long handle, byte[] data);
    public native void releaseWebSocketNative(long handle);

    
    // Paths
    native long nativePathStrokerCreate(long consumerOutPtr, float lineWidth, int capStyle, int joinStyle, float miterLimit);
    native void nativePathStrokerCleanup(long ptr);
    native void nativePathStrokerReset(long ptr, float lineWidth, int capStyle, int joinStyle, float miterLimit);
    native long nativePathStrokerGetConsumer(long ptr);
    
    native long nativePathRendererCreate(int pix_boundsX, int pix_boundsY,
                           int pix_boundsWidth, int pix_boundsHeight,
                           int windingRule);
    native void nativePathRendererSetup(int subpixelLgPositionsX, int subpixelLgPositionsY);
    native void nativePathRendererCleanup(long ptr);
    native void nativePathRendererReset(long ptr, int pix_boundsX, int pix_boundsY,
                           int pix_boundsWidth, int pix_boundsHeight,
                           int windingRule);
    native void nativePathRendererGetOutputBounds(long ptr, int[] bounds);
    native long nativePathRendererGetConsumer(long ptr);
    native long nativePathRendererCreateTexture(long ptr);
    native int[] nativePathRendererToARGB(long ptr, int color);
    native void nativeDeleteTexture(long textureID);
    
    native void nativePathConsumerMoveTo(long ptr, float x, float y);
    native void nativePathConsumerLineTo(long ptr, float x, float y);
    native void nativePathConsumerQuadTo(long ptr, float xc, float yc, float x1, float y1);
    native void nativePathConsumerCurveTo(long ptr, float xc1, float yc1, float xc2, float yc2, float x1, float y1);
    native void nativePathConsumerClose(long ptr);
    native void nativePathConsumerDone(long ptr);
   
    
    native void nativeDrawPath(int color, int alpha, long ptr);
    native void nativeSetTransform( 
            float a0, float a1, float a2, float a3, 
            float b0, float b1, float b2, float b3,
            float c0, float c1, float c2, float c3,
            float d0, float d1, float d2, float d3,
            int originX, int originY
    );
    native void nativeSetTransformMutable( 
            float a0, float a1, float a2, float a3, 
            float b0, float b1, float b2, float b3,
            float c0, float c1, float c2, float c3,
            float d0, float d1, float d2, float d3,
            int originX, int originY
    );
    
    
    native boolean nativeIsTransformSupportedGlobal();
    native boolean nativeIsShapeSupportedGlobal();
    native boolean nativeIsPerspectiveTransformSupportedGlobal();
    native boolean nativeIsAlphaMaskSupportedGlobal();
    
    
    native void drawTextureAlphaMask(long textureId, int color, int alpha, int x, int y, int w, int h);
    
    native void nativeFillShapeMutable(int color, int alpha, int commandsLen, byte[] commandsArr, int pointsLen, float[] pointsArr); 
    native void nativeDrawShadowMutable(long image, int x, int y, int offsetX, int offsetY, int blurRadius, int spreadRadius, int color, float opacity);
    native void nativeDrawShapeMutable(int color, int alpha, int commandsLen, byte[] commandsArr, int pointsLen, float[] pointsArr, float lineWidth, int capStyle, int joinStyle, float miterLimit);
    
    // End paths

    native void setNativeClippingMaskGlobal(long textureName, int x, int y, int width, int height);

    

    public native void printStackTraceToStream(Throwable t, Writer o);
    //public native String stackTraceToString(Throwable t);

    native void fillConvexPolygonGlobal(float[] points, int color, int alpha);

    native void drawConvexPolygonGlobal(float[] points, int color, int alpha, float lineWidth, int joinStyle, int capStyle, float miterLimit);

    native void setNativeClippingPolygonGlobal(float[] points);

    
    native void clearNativeCookies();

    native void splitString(String source, char separator, ArrayList<String> out) ;

    native void readFile(long nsFileHandle, byte[] b, int off, int len);

    native int getNSFileOffset(long nsFileHandle);

    native int getNSFileAvailable(long nsFileHandle);

    native int getNSFileSize(long nsFileHandle);

    native long createNSFileHandle(String name, String type);

    native long createNSFileHandle(String file);

    native void setNSFileOffset(long nsFileHandle, int off);

    /**
     * Reads a single byte from filehandle.
     * @param nsFileHandle
     * @return 
     */
    native int readNSFile(long nsFileHandle);

    public native boolean isGoogleLoggedIn();

    public native void googleLogin(Object callback);

    public native String getGoogleToken();

    public native void googleLogout();

    public native void inviteFriends(String appLinkUrl, String previewImageUrl);
    
    native void sendLocalNotification(String id, String alertTitle, String alertBody, String alertSound, int badgeNumber, long fireDate, int repeatType, boolean foreground);

    /// Enriched local notification scheduling carrying actions, grouping, time-sensitive
    /// flag and an image attachment. actionsEncoded packs the actions as
    /// idtitleplaceholderbutton records separated by .
    native void sendLocalNotification2(String id, String alertTitle, String alertBody, String alertSound, int badgeNumber, long fireDate, int repeatType, boolean foreground, String categoryId, String threadId, boolean timeSensitive, String imageAttachmentPath, String actionsEncoded);

    native void cancelLocalNotification(String id);

    /// Requests notification authorization with the given UNAuthorizationOptions mask. The
    /// result is delivered asynchronously to IOSImplementation.notificationPermissionResult.
    native void requestNotificationPermission(int optionsMask);

    /// Registers a BGTaskScheduler processing task identifier. Must be called before
    /// application:didFinishLaunchingWithOptions: returns.
    native void registerBackgroundProcessingTask(String identifier);

    /// Submits a BGProcessingTaskRequest for the given identifier.
    native void submitBackgroundProcessingTask(String identifier, double earliestBeginEpochSeconds, boolean requiresNetwork, boolean requiresPower);

    /// Cancels a pending BGTaskScheduler request by identifier.
    native void cancelBackgroundTask(String identifier);

    /// True if BGTaskScheduler (iOS 13+) is available.
    native boolean isBackgroundProcessingSupported();

    /// Reads and clears any shared content payload written by the share extension into the
    /// shared App Group user defaults. Returns a JSON string or null if there is none.
    native String getPendingSharedContent(String appGroupId);

    // --- Wallet issuer-provisioning extension (PassKit) ---------------------
    // The App Group id is read natively from the CN1WalletAppGroup Info.plist
    // key injected by the build when ios.wallet.extension is enabled.

    /// True on iOS 14+ when the CN1WalletAppGroup Info.plist key is present.
    native boolean isWalletExtensionSupported();

    /// Removes all published pass entries from the iPhone (remote=false) or
    /// Apple Watch (remote=true) list, including their card-art files.
    native void walletExtensionClearPassEntries(boolean remote);

    /// Appends one pass entry to the shared App Group suite and writes its
    /// card art PNG into the group container.
    native void walletExtensionAddPassEntry(boolean remote, String identifier, String title,
            String cardholderName, String accountSuffix, String network, String description, byte[] artPng);

    /// Sets the requires-authentication flag read by the extension's status callback.
    native void walletExtensionSetRequiresAuthentication(boolean requiresAuthentication);

    /// Stores the auth token forwarded to the issuer endpoint; null removes it.
    native void walletExtensionSetAuthToken(String token);

    /// Clears all wallet extension data from the App Group.
    native void walletExtensionClear();

    // --- Biometrics (LocalAuthentication.framework) -------------------------

    /** True when LAContext.canEvaluatePolicy(deviceOwnerAuthenticationWithBiometrics) succeeds. */
    native boolean isBiometricsSupported();

    /** Same as {@link #isBiometricsSupported()} but also requires at least one biometric to be enrolled. */
    native boolean canAuthenticateBiometric();

    /** Bitmask: bit 0 = FINGERPRINT (Touch ID), bit 1 = FACE (Face ID). */
    native int getAvailableBiometricTypes();

    /**
     * Triggers an asynchronous biometric prompt. Native code calls back into
     * {@code IOSBiometrics.nativeAuthSuccess(int)} or
     * {@code IOSBiometrics.nativeAuthError(int, int, String)} with the same
     * requestId.
     */
    native void authenticateBiometric(int requestId, String reason);

    /** Invalidates the LAContext so the in-flight prompt resolves with LAErrorAppCancel. */
    native void stopBiometricAuthentication();

    // --- App Attest (DeviceCheck.framework) ---------------------------------

    /** True when {@code DCAppAttestService.sharedService.isSupported} on this device. */
    native boolean isAppAttestSupported();

    /**
     * Generates/uses an App Attest hardware key and produces an attestation bound
     * to the SHA-256 of {@code nonce}. Native code calls back into
     * {@code IOSDeviceIntegrity.nativeAttestSuccess(int, String)} or
     * {@code IOSDeviceIntegrity.nativeAttestError(int, String)} with the same
     * requestId. The success token is {@code base64(keyId):base64(attestationObject)}
     * for the backend to verify with Apple.
     */
    native void requestAppAttestToken(int requestId, String nonce);

    // --- CarPlay (CarPlay.framework) ----------------------------------------
    // All gated natively by CN1_USE_CARPLAY (the build flips it on when the app references
    // com.codename1.car). When the define is off these compile to harmless stubs so the symbols
    // always resolve. The Java side (IOSCarBridge) describes each CarTemplate as a compact JSON
    // string; native (CodenameOne_CarPlaySceneDelegate) parses it and builds the CPTemplate tree.

    /** True while a CarPlay head unit is connected and the interface controller is live. */
    native boolean isCarPlayConnected();

    /**
     * Renders the supplied template description on the CarPlay interface controller. When
     * {@code isRoot} is true it becomes the root template, otherwise it is pushed onto the stack.
     * {@code screenId} ties native selection callbacks back to the originating CarScreen.
     */
    native void carPlaySetTemplate(int screenId, String json, boolean isRoot);

    /** Pops the top CarPlay template (returns to the previous screen). */
    native void carPlayPopTemplate();

    /** Rebuilds the template for an already-pushed screen in place (CarScreen.invalidate()). */
    native void carPlayUpdateTemplate(int screenId, String json);

    /** Registers a PNG image referenced by {@code key} in a subsequent template JSON. */
    native void carPlayRegisterImage(String key, byte[] png);

    /** Shows a transient CarPlay alert/banner with the supplied message for {@code seconds}. */
    native void carPlayShowToast(String message, int seconds);

    // --- Secure storage (Security.framework keychain) -----------------------

    /** Sets the kSecAttrAccessGroup applied to subsequent keychain operations. {@code null} clears. */
    native void setSecureStorageAccessGroup(String accessGroup);

    /** Async keychain read; result via IOSSecureStorage.nativeStorageStringResult / nativeStorageError. */
    native void secureStorageGet(int requestId, String reason, String account);

    /** Async keychain write; result via IOSSecureStorage.nativeStorageBooleanResult / nativeStorageError. */
    native void secureStorageSet(int requestId, String reason, String account, String value);

    /** Async keychain delete; result via IOSSecureStorage.nativeStorageBooleanResult / nativeStorageError. */
    native void secureStorageRemove(int requestId, String reason, String account);

    // --- NFC (Core NFC) -----------------------------------------------------

    /** True when NFCNDEFReaderSession is available (iOS 11+) and the device has NFC hardware. */
    native boolean isNfcSupported();

    /** True when Core NFC reader sessions can be started right now. */
    native boolean canReadNfc();

    /** True when Core NFC tag sessions (ISO-DEP / FeliCa / MIFARE) are available (iOS 13+). */
    native boolean canReadNfcTags();

    /** True when CardSession (HCE) is available; iOS 17.4+ EU-only with entitlement. */
    native boolean canHostEmulateNfc();

    /**
     * Starts an NDEF-only NFCNDEFReaderSession. Result is delivered via
     * IOSNfc.nativeNdefResult(int, byte[]) or
     * IOSNfc.nativeNfcError(int, int, String).
     */
    native void startNdefRead(int requestId, String alertMessage, long timeoutMs);

    /**
     * Starts an NFCTagReaderSession that accepts ISO-DEP / FeliCa / MIFARE.
     * `polling` is a bitmask: 1 = NFC-A, 2 = NFC-B, 4 = NFC-F, 8 = NFC-V (Core
     * NFC does not actually expose B/V; the request is silently downgraded
     * by the OS). `aidsArr`, when non-null, lists ISO 7816 AIDs to auto-SELECT.
     * `felicaSystemCodes` is a list of 2-byte hex strings.
     * Result via IOSNfc.nativeTagDiscovered(int, byte[], int) and
     * IOSNfc.nativeNfcError(int, int, String).
     */
    native void startTagRead(int requestId, String alertMessage,
            int polling, String[] felicaSystemCodes, byte[][] aidsArr,
            long timeoutMs);

    /** Cancels the active reader session. */
    native void stopNfcRead(int requestId);

    /** Sends an APDU on the currently-connected ISO 7816 tag.
     * Result via IOSNfc.nativeTransceiveResult(int, byte[]) or
     * IOSNfc.nativeNfcError(int, int, String). */
    native void nfcTransceive(int requestId, long tagHandle, byte[] payload);

    /** Reads the NDEF message on the currently-connected tag (after tag session). */
    native void nfcReadNdefFromTag(int requestId, long tagHandle);

    /** Writes an NDEF message to the currently-connected tag. */
    native void nfcWriteNdefToTag(int requestId, long tagHandle, byte[] ndef);

    /** Permanently locks the NDEF area on the currently-connected tag. */
    native void nfcLockTag(int requestId, long tagHandle);

    /** Registers / clears HCE AID list. Called by IOSNfc.registerHostCardEmulationService. */
    native void registerHceAids(String[] aids);

    /** Sends the HCE response for the APDU currently outstanding on CardSession. */
    native void hceSendResponse(byte[] response);

    native long gausianBlurImage(long peer, float radius);
    
    /**
     * Removes an observer from NSNotificationCenter
     * @param nsObserverPeer The opaque Objective-C class that is being used as the observer.
     */
    native void removeNotificationCenterObserver(long nsObserverPeer);

    /**
     * This one simply hides the native editing component, but doesn't fold the 
     * keyboard or remove the component.  It is used to bridge the gap in async
     * edit mode between when the user clicks "next" and when the next 
     * editing component is ready.
     * @param b 
     */
    native void setNativeEditingComponentVisible(boolean b) ;

    native void setNativeClippingMutable(int commandsLen, byte[] commandsArr, int pointsLen, float[] pointsArr);

    native void refreshContacts();

    native void translatePoints(int pointSize, float tX, float tY, float tX0, float[] in, int srcPos, float[] out, int destPos, int numPoints);

    native void scalePoints(int pointSize, float sX, float sY, float sZ, float[] in, int srcPos, float[] out, int destPos, int numPoints);

    native void updateNativeEditorText(String text);

    native void fireUIBackgroundFetchResultNoData();

    native void fireUIBackgroundFetchResultNewData();

    native void fireUIBackgroundFetchResultFailed();

    native void setPreferredBackgroundFetchInterval(int seconds);

    native boolean isBackgroundFetchSupported();

    native int countLinkedContacts(int recId);

    native void getLinkedContactIds(int num, int recId, int[] out);

    native void fillRadialGradientMutable(int startColor, int endColor, int x, int y, int width, int height, int startAngle, int arcAngle);

    native void applyRadialGradientPaintMutable(int startColor, int endColor, int x, int y, int width, int height);

    native void clearRadialGradientPaintMutable();

    native void applyRadialGradientPaintGlobal(int startColor, int endColor, int x, int y, int width, int height);

    native void clearRadialGradientPaintGlobal();

    native void clearRectMutable(int x, int y, int width, int height);

    native void nativeClearRectGlobal(int x, int y, int width, int height);

    native void blockCopyPaste(boolean blockCopyPaste);

    //#define INCLUDE_CONTACTS_USAGE
    //#define INCLUDE_CALENDARS_USAGE
    //#define INCLUDE_CAMERA_USAGE
    //#define INCLUDE_FACEID_USAGE
    //#define INCLUDE_LOCATION_USAGE
    //#define INCLUDE_MICROPHONE_USAGE
    //#define INCLUDE_MOTION_USAGE
    //#define INCLUDE_PHOTOLIBRARYADD_USAGE
    //#define INCLUDE_PHOTOLIBRARY_USAGE
    //#define INCLUDE_REMINDERS_USAGE
    //#define INCLUDE_SIRI_USAGE
    //#define INCLUDE_SPEECHRECOGNITION_USAGE
    //#define INCLUDE_NFCREADER_USAGE
    native boolean checkContactsUsage();
    native boolean checkCalendarsUsage();
    native boolean checkCameraUsage();
    native boolean checkFaceIDUsage();
    native boolean checkLocationUsage();
    native boolean checkMicrophoneUsage();
    native boolean checkMotionUsage();
    native boolean checkPhotoLibraryAddUsage();
    native boolean checkPhotoLibraryUsage();
    native boolean checkRemindersUsage();
    native boolean checkSiriUsage();
    native boolean checkSpeechRecognitionUsage();
    native boolean checkNFCReaderUsage();

    // Checks avaiable bytes for NetworkConnection
    native int available(long peer);

    // Read pending data from NetworkConnection
    native int readData(long peer, byte[] bytes, int off, int len);

    // Reads next byte from NetworkConnection
    native int shiftByte(long peer);

    // Appends pending data to NetworkConnection
    // data is a NSData* object
    // We go through java in order to use locking concurrency
    native void appendData(long peer, long data);

    native void screenshot();
    
    native void fillPolygonGlobal(int color, int alpha, int[] xPoints, int[] yPoints, int nPoints);

    native void registerPushAction(String id, String title, String textInputPlaceholder, String replyButtonText);

    native void startPushActionCategory(String id);

    native void addPushActionToCategory(String id);

    native void endPushActionCategory();

    native void registerPushCategories();
    
    native void firePushCompletionHandler();

    native boolean isMultiGallerySelectSupported();

    native void setConnectionId(long peer, int id);
    native void setInsecure(long peer, boolean insecure);
    
    native int getDisplaySafeInsetLeft();

    native int getDisplaySafeInsetTop();

    native int getDisplaySafeInsetRight();

    native int getDisplaySafeInsetBottom();

    native boolean isRTLString(String javaString);

    public static native void announceForAccessibility(String text);

    // ============================================================
    // Crypto bridge -- backed by CN1Crypto.{h,m} in nativeSources/.
    //
    // Each method returns the number of bytes written to its output buffer,
    // or a negative CN1_CRYPTO_E_* error code on failure. The Java side in
    // IOSImplementation trims to that length and translates failures into
    // CryptoException.

    native void secureRandomBytes(byte[] out);

    native int aesCbc(int encrypt, byte[] key, byte[] iv,
                      byte[] in, byte[] out, int padding);

    native int aesGcm(int encrypt, byte[] key, byte[] iv,
                      byte[] aad, byte[] in, byte[] out);

    native int rsaEncrypt(int paddingKind, byte[] x509, byte[] in, byte[] out);

    native int rsaDecrypt(int paddingKind, byte[] pkcs8, byte[] in, byte[] out);

    native int sign(int algorithm, byte[] pkcs8, byte[] data, byte[] out);

    native int verify(int algorithm, byte[] x509, byte[] data, byte[] sig);

    /// `lengths[0]` is set to public-key DER length, `lengths[1]` to
    /// private-key DER length. Returns 0 on success, negative on error.
    native int generateRsaKeyPair(int bits, byte[] outPub, byte[] outPriv, int[] lengths);

}
