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
import com.codename1.social.LoginCallback;
import com.codename1.ui.geom.Rectangle;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Abstraction of the underlying native API's
 *
 * @author Shai Almog
 */
public final class IOSNative {
    
    
    //native void startMainThread(Runnable r);
    native void initVM();
    static native void deinitializeVM();
    native boolean isPainted();
    native int getDisplayWidth();
    native int getDisplayHeight();
    native void editStringAt(int x, int y, int w, int h, long peer, boolean singleLine, int rows, int maxSize, int constraint, String text, boolean forceSlideUp, int color, long imagePeer, int padTop, int padBottom, int padLeft, int padRight, String hint, boolean showToolbar);
    native void flushBuffer(long peer, int x, int y, int width, int height);
    native void imageRgbToIntArray(long imagePeer, int[] arr, int x, int y, int width, int height, int imgWidth, int imgHeight);
    native long createImageFromARGB(int[] argb, int width, int height);
    native long createImage(byte[] data, int[] widthHeight);
    native long createImageNSData(long nsData, int[] widthHeight);
    native long scale(long peer, int width, int height);
    native void setNativeClippingMutable(int x, int y, int width, int height, boolean firstClip);
    native void setNativeClippingGlobal(int x, int y, int width, int height, boolean firstClip);
    native void nativeDrawLineMutable(int color, int alpha, int x1, int y1, int x2, int y2);
    native void nativeDrawLineGlobal(int color, int alpha, int x1, int y1, int x2, int y2);
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
    native void nativeFillArcGlobal(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle);
    native void nativeDrawArcGlobal(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle);
    native void nativeDrawStringMutable(int color, int alpha, long fontPeer, String str, int x, int y);
    native void nativeDrawStringGlobal(int color, int alpha, long fontPeer, String str, int x, int y);
    native void nativeDrawImageMutable(long peer, int alpha, int x, int y, int width, int height);
    native void nativeDrawImageGlobal(long peer, int alpha, int x, int y, int width, int height);
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

    native boolean isTablet();
    native boolean isIOS7();
    
    native void setImageName(long nativeImage, String name);
    
    native boolean canExecute(String url);
    native void execute(String url);

    native void flashBacklight(int duration);

    native boolean isMinimized();
    
    native boolean minimizeApplication();

    native void restoreMinimizedApplication();

    native void lockOrientation(boolean portrait);
    native void unlockOrientation();
    native void lockScreen();
    native void unlockScreen();

    native void vibrate(int duration);

    native int getAudioDuration(long peer);

    native void playAudio(long peer);

    native int getAudioTime(long peer);

    native void pauseAudio(long peer);

    native void setAudioTime(long peer, int time);
    native boolean isAudioPlaying(long peer);

    native void cleanupAudio(long peer);

    native long createAudio(String uri, Runnable onCompletion);
    
    native long createAudio(byte[] data, Runnable onCompletion);
    
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

    native void setPinchToZoomEnabled(long peer, boolean e);
    native void setNativeBrowserScrollingEnabled(long peer, boolean e);
    native long createBrowserComponent(Object bc);

    native void setBrowserPage(long browserPeer, String html, String baseUrl);

    native void setBrowserURL(long browserPeer, String url);
    
    native void setBrowserUserAgent(long browserPeer, String ua);
    
    native void browserBack(long browserPeer);
    native void browserStop(long browserPeer);

    native void browserClearHistory(long browserPeer);

    native void browserExecute(long browserPeer, String javaScript);
    native String browserExecuteAndReturnString(long browserPeer, String javaScript);
    
    native void browserForward(long browserPeer);

    native boolean browserHasBack(long browserPeer);

    native boolean browserHasForward(long browserPeer);

    native void browserReload(long browserPeer);

    native String getBrowserTitle(long browserPeer);

    native String getBrowserURL(long browserPeer);
    
    native long createVideoComponent(String url);
    native long createVideoComponent(byte[] video);
    native long createVideoComponentNSData(long video);
    native long createNativeVideoComponent(String url);
    native long createNativeVideoComponent(byte[] video);
    native long createNativeVideoComponentNSData(long video);

    native void startVideoComponent(long peer); 
    
    native void stopVideoComponent(long peer);
    native void pauseVideoComponent(long peer);

    native int getMediaTimeMS(long peer);
    
    native int setMediaTimeMS(long peer, int now);

    native int getMediaDuration(long peer);
    
    native void setMediaBgArtist(String artist);
    native void setMediaBgTitle(String title);
    native void setMediaBgDuration(long duration);
    native void setMediaBgPosition(long position);
    native void setMediaBgAlbumCover(long cover);
    
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

    native int fileCountInDir(String dir);
    native void listFilesInDir(String dir, String[] files);
    native void createDirectory(String dir);
    native void moveFile(String src, String dest);
    
    native long openConnection(String url, int timeout);
    native void connect(long peer);
    native void setMethod(long peer, String mtd);
    
    native int getResponseCode(long peer);

    native String getResponseMessage(long peer);

    native int getContentLength(long peer);

    native String getResponseHeader(long peer, String name);
    native int getResponseHeaderCount(long peer);
    native String getResponseHeaderName(long peer, int offset);

    native void addHeader(long peer, String key, String value);

    native void setBody(long peer, byte[] arr);    
    
    native void closeConnection(long peer);
    
    native String getUDID();
    native String getOSVersion();
    
    // location manager
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

    native void startUpdatingLocation(long clLocation);
    native void stopUpdatingLocation(long clLocation);
    
    
    // capture
    native void captureCamera(boolean movie);
    native void openGallery(int type);
    native long createAudioRecorder(String destinationFile);
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
    
    native int sqlCursorGetColumnCount(long statementPeer);
    
    native void fetchProducts(String[] skus, Product[] products);
    native void purchase(String sku);
    native boolean canMakePayments();
    native void restorePurchases();
    native void zoozPurchase(double amount, String currency, String appKey, boolean sandbox, String invoiceNumber);

    native String formatInt(int i);
    native String formatDouble(double d);
    native String formatCurrency(double d);
    native String formatDate(long date);
    native String formatDateShort(long date);
    native String formatDateTime(long date);
    native double parseDouble(String localeFormattedDecimal);
    native String formatDateTimeMedium(long date);
    native String formatDateTimeShort(long date);
    native String getCurrencySymbol();
    
    native void scanQRCode();
    native void scanBarCode();

    native long createTruetypeFont(String name);
    native long deriveTruetypeFont(long uiFont, boolean bold, boolean italic, float size);

    native void log(String text);

    native void addCookie(String key, String value, String domain, String path, boolean secure, boolean httpOnly, long expires);
    native void getCookiesForURL(String url, Vector out);

    native String getUserAgentString();
    
    native void openDatePicker(int type, long time, int x, int y, int w, int h);
    native void openStringPicker(String[] stringArray, int selection, int x, int y, int w, int h);

    native void socialShare(String text, long imagePeer, Rectangle sourceRect);
    
    // facebook connect
    public native void facebookLogin(Object callback);
    public native boolean isFacebookLoggedIn();
    public native String getFacebookToken();
    public native void facebookLogout();
    public native boolean askPublishPermissions(LoginCallback lc);    
    public native boolean hasPublishPermissions();
        
    
    public native boolean isAsyncEditMode();
    public native void setAsyncEditMode(boolean b);
    public native void foldVKB();
    public native void hideTextEditing();
    public native int getVKBHeight();
    public native int getVKBWidth();

    public native long connectSocket(String host, int port);    
    public native String getHostOrIP();
    public native void disconnectSocket(long socket);
    public native boolean isSocketConnected(long socket);
    public native String getSocketErrorMessage(long socket);
    public native int getSocketErrorCode(long socket);
    public native int getSocketAvailableInput(long socket);
    public native byte[] readFromSocketStream(long socket);
    public native void writeToSocketStream(long socket, byte[] data);

    
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
    
    
    native boolean nativeIsTransformSupportedGlobal();
    native boolean nativeIsShapeSupportedGlobal();
    native boolean nativeIsPerspectiveTransformSupportedGlobal();
    native boolean nativeIsAlphaMaskSupportedGlobal();
    
    
    native void drawTextureAlphaMask(long textureId, int color, int alpha, int x, int y, int w, int h);
    
    
    
    // End paths

    native void setNativeClippingMaskGlobal(long textureName, int x, int y, int width, int height);

    

    public native void printStackTraceToStream(Throwable t, Writer o);
    //public native String stackTraceToString(Throwable t);

    native void fillConvexPolygonGlobal(float[] points, int color, int alpha);

    native void drawConvexPolygonGlobal(float[] points, int color, int alpha, float lineWidth, int joinStyle, int capStyle, float miterLimit);

    native void setNativeClippingPolygonGlobal(float[] points);

    

}
