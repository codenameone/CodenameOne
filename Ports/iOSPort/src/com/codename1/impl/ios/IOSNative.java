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
import java.util.Vector;

/**
 * Abstraction of the underlying native API's
 *
 * @author Shai Almog
 */
final class IOSNative {
    //native void startMainThread(Runnable r);
    native void initVM();
    static native void deinitializeVM();
    native boolean isPainted();
    native int getDisplayWidth();
    native int getDisplayHeight();
    native void editStringAt(int x, int y, int w, int h, long peer, boolean singleLine, int rows, int maxSize, int constraint, String text);
    native void flushBuffer(long peer, int x, int y, int width, int height);
    native void imageRgbToIntArray(long imagePeer, int[] arr, int x, int y, int width, int height);
    native long createImageFromARGB(int[] argb, int width, int height);
    native long createImage(byte[] data, int[] widthHeight);
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

    native void shearGlobal(float x, float y);

    native void fillRectRadialGradientGlobal(int startColor, int endColor, int x, int y, int width, int height, float relativeX, float relativeY, float relativeSize);
    
    native void fillLinearGradientGlobal(int startColor, int endColor, int x, int y, int width, int height, boolean horizontal);
    
    native void fillRectRadialGradientMutable(int startColor, int endColor, int x, int y, int width, int height, float relativeX, float relativeY, float relativeSize);
    
    native void fillLinearGradientMutable(int startColor, int endColor, int x, int y, int width, int height, boolean horizontal);

    native boolean isTablet();
    
    native void setImageName(long nativeImage, String name);
    
    native void execute(String url);

    native void flashBacklight(int duration);

    native boolean isMinimized();
    
    native boolean minimizeApplication();

    native void restoreMinimizedApplication();

    native void lockOrientation(boolean portrait);
    native void unlockOrientation();

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

    native void releasePeer(long peer);
    native void retainPeer(long peer);

    native void setPinchToZoomEnabled(long peer, boolean e);
    native long createBrowserComponent(Object bc);

    native void setBrowserPage(long browserPeer, String html, String baseUrl);

    native void setBrowserURL(long browserPeer, String url);
    
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

    native void startVideoComponent(long peer); 
    
    native void stopVideoComponent(long peer);

    native int getMediaTimeMS(long peer);
    
    native int setMediaTimeMS(long peer, int now);

    native int getMediaDuration(long peer);

    native boolean isVideoPlaying(long peer);

    native void setVideoFullScreen(long peer, boolean fullscreen);

    native boolean isVideoFullScreen(long peer);

    native long getVideoViewPeer(long peer);
    
    native void showNativePlayerController(long peer);
    
    // IO methods

    native int writeToFile(byte[] data, String path);
    native int appendToFile(byte[] data, String path);
    native int getFileSize(String path);
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
    native long createAudioRecorder(String destinationFile);
    native void startAudioRecord(long peer);
    native void pauseAudioRecord(long peer);
    native void cleanupAudioRecord(long peer);

    native void sendEmailMessage(String recipients, String subject, String content, String attachment, String attachmentMimeType);

    native int getContactCount(boolean withNumbers);
    native void getContactRefIds(int[] refs, boolean withNumbers);
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
    
    native void dial(String phone);
    native void sendSMS(String phone, String text);

    native void registerPush();

    native void deregisterPush();

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
    
    native void zoozPurchase(double amount, String currency, String appKey, boolean sandbox, String invoiceNumber);

    native String formatInt(int i);
    native String formatDouble(double d);
    native String formatCurrency(double d);
    native String formatDate(long date);
    native String formatDateShort(long date);
    native String formatDateTime(long date);
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
}
