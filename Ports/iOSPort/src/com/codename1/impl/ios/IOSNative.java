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

/**
 * Abstraction of the underlying native API's
 *
 * @author Shai Almog
 */
public class IOSNative {
    //public static native void startMainThread(Runnable r);
    public static native void initVM();
    public static native void deinitializeVM();
    public static native boolean isPainted();
    public static native int getDisplayWidth();
    public static native int getDisplayHeight();
    public static native void editStringAt(int x, int y, int w, int h, long peer, boolean singleLine, int rows, int maxSize, int constraint, String text);
    public static native void flushBuffer(long peer, int x, int y, int width, int height);
    public static native void imageRgbToIntArray(long imagePeer, int[] arr, int x, int y, int width, int height);
    public static native long createImageFromARGB(int[] argb, int width, int height);
    public static native long createImage(byte[] data, int[] widthHeight);
    public static native long scale(long peer, int width, int height);
    public static native void setNativeClippingMutable(int x, int y, int width, int height, boolean firstClip);
    public static native void setNativeClippingGlobal(int x, int y, int width, int height, boolean firstClip);
    public static native void nativeDrawLineMutable(int color, int alpha, int x1, int y1, int x2, int y2);
    public static native void nativeDrawLineGlobal(int color, int alpha, int x1, int y1, int x2, int y2);
    public static native void nativeFillRectMutable(int color, int alpha, int x, int y, int width, int height);
    public static native void nativeFillRectGlobal(int color, int alpha, int x, int y, int width, int height);
    public static native void nativeDrawRectMutable(int color, int alpha, int x, int y, int width, int height);
    public static native void nativeDrawRectGlobal(int color, int alpha, int x, int y, int width, int height);
    public static native void nativeDrawRoundRectMutable(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight);
    public static native void nativeDrawRoundRectGlobal(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight);
    public static native void nativeFillRoundRectMutable(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight);
    public static native void nativeFillRoundRectGlobal(int color, int alpha, int x, int y, int width, int height, int arcWidth, int arcHeight);
    public static native void nativeFillArcMutable(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle);
    public static native void nativeDrawArcMutable(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle);
    public static native void nativeFillArcGlobal(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle);
    public static native void nativeDrawArcGlobal(int color, int alpha, int x, int y, int width, int height, int startAngle, int arcAngle);
    public static native void nativeDrawStringMutable(int color, int alpha, long fontPeer, String str, int x, int y);
    public static native void nativeDrawStringGlobal(int color, int alpha, long fontPeer, String str, int x, int y);
    public static native void nativeDrawImageMutable(long peer, int alpha, int x, int y, int width, int height);
    public static native void nativeDrawImageGlobal(long peer, int alpha, int x, int y, int width, int height);
    public static native void nativeTileImageGlobal(long peer, int alpha, int x, int y, int width, int height);
    public static native int stringWidthNative(long peer, String str);
    public static native int charWidthNative(long peer, char ch);
    public static native int getFontHeightNative(long peer);
    public static native long createSystemFont(int face, int style, int size);
    public static byte[] loadResource(String name, String type) {
        int val = getResourceSize(name, type);
        if(val < 0) {
            return null;
        }
        byte[] data = new byte[val];
        loadResource(name, type, data);
        return data;
    }
    private static native int getResourceSize(String name, String type);
    private static native void loadResource(String name, String type, byte[] data);

    public static native long createNativeMutableImage(int w, int h, int color);

    public static native void startDrawingOnImage(int w, int h, long peer);
    public static native long finishDrawingOnImage();

    public static native void deleteNativePeer(long peer);
    public static native void deleteNativeFontPeer(long peer);

    public static native void resetAffineGlobal();

    public static native void scaleGlobal(float x, float y);

    public static native void rotateGlobal(float angle);
    public static native void rotateGlobal(float angle, int x, int y);

    public static native void shearGlobal(float x, float y);

    public static native void fillRectRadialGradientGlobal(int startColor, int endColor, int x, int y, int width, int height, float relativeX, float relativeY, float relativeSize);
    
    public static native void fillLinearGradientGlobal(int startColor, int endColor, int x, int y, int width, int height, boolean horizontal);
    
    public static native void fillRectRadialGradientMutable(int startColor, int endColor, int x, int y, int width, int height, float relativeX, float relativeY, float relativeSize);
    
    public static native void fillLinearGradientMutable(int startColor, int endColor, int x, int y, int width, int height, boolean horizontal);

    public static native boolean isTablet();
    
    public static native void setImageName(long nativeImage, String name);
    
    public static native void execute(String url);

    public static native void flashBacklight(int duration);

    public static native boolean isMinimized();
    
    public static native boolean minimizeApplication();

    public static native void restoreMinimizedApplication();

    public static native void lockOrientation(boolean portrait);

    public static native void vibrate(int duration);

    public static native int getAudioDuration(long peer);

    public static native void playAudio(long peer);

    public static native int getAudioTime(long peer);

    public static native void pauseAudio(long peer);

    public static native void setAudioTime(long peer, int time);
    public static native boolean isAudioPlaying(long peer);

    public static native void cleanupAudio(long peer);

    public static native long createAudio(String uri, Runnable onCompletion);
    
    public static native long createAudio(byte[] data, Runnable onCompletion);
    
    public static native float getVolume();

    public static native void setVolume(float vol);
    
    // Peer Component methods
    
    public static native void calcPreferredSize(long peer, int w, int h, int[] response);

    public static native void updatePeerPositionSize(long peer, int x, int y, int w, int h);
    
    public static native void peerInitialized(long peer, int x, int y, int w, int h);

    public static native void peerDeinitialized(long peer);
    public static native void peerSetVisible(long peer, boolean v);

    public static native void releasePeer(long peer);
    public static native void retainPeer(long peer);

    public static native long createBrowserComponent(Object bc);

    public static native void setBrowserPage(long browserPeer, String html, String baseUrl);

    public static native void setBrowserURL(long browserPeer, String url);
    
    public static native void browserBack(long browserPeer);
    public static native void browserStop(long browserPeer);

    public static native void browserClearHistory(long browserPeer);

    public static native void browserExecute(long browserPeer, String javaScript);

    public static native void browserForward(long browserPeer);

    public static native boolean browserHasBack(long browserPeer);

    public static native boolean browserHasForward(long browserPeer);

    public static native void browserReload(long browserPeer);

    public static native String getBrowserTitle(long browserPeer);

    public static native String getBrowserURL(long browserPeer);
    
    public static native long createVideoComponent(String url);

    public static native long createVideoComponent(byte[] video);

    public static native void startVideoComponent(long peer); 
    
    public static native void stopVideoComponent(long peer);

    public static native int getMediaTimeMS(long peer);
    
    public static native int setMediaTimeMS(long peer, int now);

    public static native int getMediaDuration(long peer);

    public static native boolean isVideoPlaying(long peer);

    public static native void setVideoFullScreen(long peer, boolean fullscreen);

    public static native boolean isVideoFullScreen(long peer);

    public static native long getVideoViewPeer(long peer);
    
    public static native void showNativePlayerController(long peer);
    
    // IO methods

    public static native int writeToFile(byte[] data, String path);
    public static native int getFileSize(String path);
    public static native void readFile(String path, byte[] bytes);

    public static native String getDocumentsDir();
    public static native String getCachesDir();
    public static native String getResourcesDir();
    public static native void deleteFile(String file);
    public static native boolean fileExists(String file);
    public static native boolean isDirectory(String file);

    public static native int fileCountInDir(String dir);
    public static native void listFilesInDir(String dir, String[] files);
    public static native void createDirectory(String dir);
    public static native void moveFile(String src, String dest);
    
    public static native long openConnection(String url, int timeout);
    public static native void connect(long peer);
    public static native void setMethod(long peer, String mtd);
    
    public static native int getResponseCode(long peer);

    public static native String getResponseMessage(long peer);

    public static native int getContentLength(long peer);

    public static native String getResponseHeader(long peer, String name);
    public static native int getResponseHeaderCount(long peer);
    public static native String getResponseHeaderName(long peer, int offset);

    public static native void addHeader(long peer, String key, String value);

    public static native void setBody(long peer, byte[] arr);    
    
    public static native void closeConnection(long peer);
    
    public static native String getUDID();
    
    // location manager
    public static native long createCLLocation();
    public static native boolean isGoodLocation(long clLocation);
    public static native long getCurrentLocationObject(long clLocation);
    public static native double getLocationLatitude(long location);
    public static native double getLocationAltitude(long location);
    public static native double getLocationLongtitude(long location);
    public static native double getLocationAccuracy(long location);
    public static native double getLocationDirection(long location);
    public static native double getLocationVelocity(long location);
    public static native long getLocationTimeStamp(long location);

    public static native void startUpdatingLocation(long clLocation);
    public static native void stopUpdatingLocation(long clLocation);
    
    
    // capture
    public static native void captureCamera(boolean movie);
    public static native long createAudioRecorder(String destinationFile);
    public static native void startAudioRecord(long peer);
    public static native void pauseAudioRecord(long peer);
    public static native void cleanupAudioRecord(long peer);

    public static native void sendEmailMessage(String recipients, String subject, String content, String attachment, String attachmentMimeType);

    public static native int getContactCount(boolean withNumbers);
    public static native void getContactRefIds(int[] refs, boolean withNumbers);
    public static native long getPersonWithRecordID(int id);
    public static native String getPersonFirstName(long id);
    public static native String getPersonSurnameName(long id);
    public static native int getPersonPhoneCount(long id);
    public static native String getPersonPhone(long id, int offset);
    public static native String getPersonPhoneType(long id, int offset);
    public static native String getPersonPrimaryPhone(long id);
    public static native String getPersonEmail(long id);
    public static native String getPersonAddress(long id);
    public static native long createPersonPhotoImage(long id);
    
    public static native void dial(String phone);
    public static native void sendSMS(String phone, String text);

    public static native void registerPush();

    public static native void deregisterPush();

    public static native long createImageFile(long imagePeer, boolean jpeg, int width, int height, float quality);
    public static native int getNSDataSize(long nsData);
    public static native void nsDataToByteArray(long nsData, byte[] data);

    public static native boolean sqlDbExists(String name);
    public static native long sqlDbCreateAndOpen(String name);
    public static native void sqlDbDelete(String name);
    public static native void sqlDbClose(long db);

    public static native void sqlDbExec(long dbPeer, String sql, String[] args);

    public static native long sqlDbExecQuery(long dbPeer, String sql, String[] args);

    public static native boolean sqlCursorFirst(long statementPeer);
    public static native boolean sqlCursorNext(long statementPeer);
    public static native String sqlGetColName(long statementPeer, int index);
    public static native void sqlCursorCloseStatement(long statement);

    public static native byte[] sqlCursorValueAtColumnBlob(long statement, int col);
    public static native double sqlCursorValueAtColumnDouble(long statement, int col);
    public static native float sqlCursorValueAtColumnFloat(long statement, int col);
    public static native int sqlCursorValueAtColumnInteger(long statement, int col);
    public static native long sqlCursorValueAtColumnLong(long statement, int col);
    public static native short sqlCursorValueAtColumnShort(long statement, int col);
    public static native String sqlCursorValueAtColumnString(long statement, int col);
    
    public static native int sqlCursorGetColumnCount(long statementPeer);
    
    public static native void fetchProducts(String[] skus, Product[] products);
    public static native void purchase(String sku);

    public static native String formatInt(int i);
    public static native String formatDouble(double d);
    public static native String formatCurrency(double d);
    public static native String formatDate(long date);
    public static native String formatDateShort(long date);
    public static native String formatDateTime(long date);
    public static native String formatDateTimeMedium(long date);
    public static native String formatDateTimeShort(long date);
    public static native String getCurrencySymbol();
    
    public static native void scanQRCode();
    public static native void scanBarCode();

    public static native long createTruetypeFont(String name);
    public static native long deriveTruetypeFont(long uiFont, boolean bold, boolean italic, float size);
}
