/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
 */

package com.codename1.impl.html5;

import com.codename1.teavm.jso.io.Blob;
import com.codename1.html5.js.JSBody;
import com.codename1.html5.js.JSFunctor;
import com.codename1.html5.js.JSObject;
import com.codename1.html5.js.JSProperty;
import com.codename1.html5.js.browser.AnimationFrameCallback;
import com.codename1.html5.js.browser.Window;
import com.codename1.html5.js.core.JSString;
import com.codename1.html5.js.dom.Event;
import com.codename1.html5.js.dom.EventListener;
import com.codename1.html5.js.dom.MouseEvent;
import com.codename1.html5.js.dom.HTMLCanvasElement;
import com.codename1.html5.js.dom.HTMLDocument;
import com.codename1.html5.js.dom.HTMLElement;
import com.codename1.html5.js.dom.HTMLImageElement;
import com.codename1.html5.js.dom.HTMLInputElement;
import com.codename1.html5.js.typedarrays.ArrayBuffer;

/**
 *
 * @author shannah
 */
public class JSOImplementations {
    
    
    public interface Console extends JSObject {
        void log(String str);
    }
    
    
    public interface JSFontMetrics extends JSObject {
        @JSProperty
        double getAscent();
        
        @JSProperty
        double getDescent();
        
        @JSProperty
        double getHeight();
        
        @JSProperty
        double getLeading();
        
        @JSProperty
        JSObject getBounds();
    }
    
    public interface WindowExt extends JSObject {
        
        
        @JSProperty
        abstract Console getConsole();
        
        abstract int requestAnimationFrame(AnimationFrameCallback callback);
        
        abstract String eval(String str);
        
        @JSProperty
        abstract public int getTEMPORARY();
        
        @JSProperty
        abstract public int getPERSISTENT();
        
        abstract void requestFileSystem(int type, int size, FileSystemCallback success, ErrorCallback error);
        
        @JSProperty
        abstract public WebkitStorageInfo getWebkitStorageInfo();

        abstract public void requestFileSystem(int persistent, int grantedBytes, FileSystemCallback fileSystemCallback);
        
        @JSBody(params={}, script="return new Object()")
        JSObject createEmptyObject();
        
        @JSProperty
        abstract public CN1Native getCn1();
        
        @JSBody(params={}, script="return new FileReader()")
        public FileReader createFileReader();
        
        @JSBody(params={"obj"}, script="return new FileWriter(obj)")
        public FileWriter createFileWriter(JSObject obj);
        
        @JSBody(params={"o"}, script="return new FileSaver(o)")
        public FileSaver createFileSaver(JSObject obj);
        
        @JSProperty
        abstract public Navigator getNavigator();
        
        abstract public Blob Base64ToBlob(String dataURL);
        
        abstract public void BlobToBase64(Blob blob, DataURLCallback callback);
        
        //abstract public String encodeURIComponent(String uri);
        
        /**
         * Gets the URL to use for the CORS proxy.  This will be used
         * In the stub by default.  Implementing in Javascript allows 
         * us to more easily override it after the app is compiled.
         * This can be overridden by adding the following in a <script> tag:
         * window.cn1CORSProxyURL="http://example.com/path/to/proxy"
         * @return The proxy URL or null if none is defined.
         */
        abstract public String getCorsProxyURL();
        
        /**
         * A native method that should return the deployment type of the 
         * application.  This should return one of "file", "directory", "war"
         * @return 
         */
        abstract public String getCN1DeploymentType();
        
        abstract public String arrayBufferToBase64(ArrayBuffer buf);
            
        
        abstract public void scrollTo(int x, int y);
        
        
        
    }
    
    public interface WindowLocation extends JSObject {
        @JSProperty
        String getHref();
        
        
        @JSProperty
        String getHash();
        
        @JSProperty
        String getHost();
        
        @JSProperty
        String getHostname();
        
        @JSProperty
        String getOrigin();
        
        @JSProperty
        String getPathname();
        
        @JSProperty
        String getPort();
        
        @JSProperty
        String getProtocol();
        
        @JSProperty
        String getSearch();
        
        void assign(String url);
        void replace(String url);
        void reload(boolean forceGet);
        
    }
    
    
    public interface TextAreaEl extends JSObject {
        
    }
    
    public interface HTMLIFrameElement extends HTMLElement {
        @JSProperty
        Window getContentWindow();
    }
    
    
    public interface DocumentExt extends HTMLDocument {
        void open(String contentType);
        void write(String content);
        void close();
    }
    
    
    // FILE SYSTEM STUFF -------------------------------------------------------
    
    public interface WebkitStorageInfo extends JSObject {
        public void requestQuota(int type, int size, RequestQuotaCallback success, ErrorCallback error);
    }
    
    @JSFunctor
    public interface RequestQuotaCallback extends JSObject {
        public void onQuotaGranted(int grantedBytes);
    }
    
    public interface FileSystem extends JSObject {
        @JSProperty
        public DirectoryEntry getRoot();
        
        @JSProperty
        public String getName();
        
        @JSBody(params={}, script="return new Object()")
        public FileOptions createFileOptions();
        
        
    }
    
    @JSFunctor
    public interface FileSystemCallback extends JSObject {
        public void onInit(FileSystem fs);
    }
    
    public interface FileReader extends JSObject {
        public static final int EMPTY=0;
        public static final int LOADING=1;
        public static final int DONE=2;
        
        @JSProperty
        public JSObject getError();
        
        @JSProperty
        public int getReadyState();
        
        @JSProperty
        public JSObject getResult();
        
        public void abort();
        
        public void readAsArrayBuffer(JSObject blob);
        
        public void readAsBinaryString(JSObject blob);
        
        public void readAsDataURL(JSObject blob);
        
        public void readAsText(JSObject blob);
        
        @JSProperty
        public void setOnabort(EventListener l);
        
        @JSProperty
        public void setOnerror(EventListener l);
        
        @JSProperty 
        public void setOnload(EventListener l);
        
        @JSProperty
        public void setOnloadstart(EventListener l);
        
        @JSProperty
        public void setOnloadend(EventListener l);
        
        @JSProperty
        public void setOnprogress(EventListener l);
        
    }
    
    public interface FileSaver extends JSObject {
        public static final int INIT=0;
        public static final int WRITING=1;
        public static final int DONE=2;
        
        public void abort();
        
        @JSProperty
        public int getReadyState();
        
        @JSProperty
        public JSError getError();
        
        @JSProperty
        public void setOnwritestart(EventListener l);
        
        @JSProperty
        public void setOnprogress(EventListener l);
        
        @JSProperty
        public void setOnwrite(EventListener l);
        
        @JSProperty
        public void setOnabort(EventListener l);
        
        @JSProperty
        public void setOnerror(EventListener l);
        
        @JSProperty
        public void setOnwriteend(EventListener l);
        
        
        
    }
    
    public interface FileWriter extends FileSaver {
        @JSProperty
        public int getPosition();
        
        @JSProperty
        public int getLength();
        
        public void write(JSObject data);
        public void seek(int offset);
        public void truncate(int size);
    }
    
    public interface FileEntry extends JSObject {
        
        @JSProperty
        public String getFullPath();
        
        @JSProperty
        public boolean getIsDirectory();
        
        @JSProperty
        public boolean getIsFile();
        
        @JSProperty
        public String getName();
        
        public void createWriter(FileWriterCallback success, ErrorCallback error);
        public void file(FileCallback success, ErrorCallback error);
        
        public void remove(NoArgCallback success, ErrorCallback error);
        
        
        public void getMetadata(MetadataCallback success, ErrorCallback error);
        
        public void moveTo(DirectoryEntry directory, String newName);
        
        public String toURL();
    }
    
    public interface DirectoryEntry extends FileEntry {
        public DirectoryReader createReader();
        public void getFile(String path, FileOptions options, EntryCallback successCallback, ErrorCallback errorCallback);
        public void getDirectory(String path, FileOptions options, EntryCallback successCallback, ErrorCallback errorCallback);
        public void removeRecursively(Callback success, ErrorCallback error);
        
    }
    
    public interface FileOptions extends JSObject {
        @JSProperty
        public void setCreate(boolean create);
        
        @JSProperty
        public boolean isCreate();
        
        @JSProperty
        public void setExclusive(boolean exclusive);
        
        @JSProperty
        public boolean isExclusive();
    }
    
    @JSFunctor
    public interface EntryCallback extends JSObject {
        public void onEntry(FileEntry entry);
    }
    
    @JSFunctor
    public interface ErrorCallback extends JSObject {
        public void onError(JSError error);
    }
    
    public interface DirectoryReader extends JSObject {
        public void readEntries(EntryCallback success, ErrorCallback error);
    }
    
    @JSFunctor
    public interface Callback extends JSObject {
        
    }
    
    @JSFunctor 
    public interface FileCallback extends JSObject {
        public void onFile(JSFile file);

    }
    
    public interface JSFile extends JSObject {
        @JSProperty
        public JSDate getLastModifiedDate();
        
        @JSProperty
        public String getName();
        
        
        
    }
    
    public interface JSDate extends JSObject {
        
    }
    
    @JSFunctor
    public interface FileWriterCallback extends JSObject {
        public void onCreate(FileWriter writer);
    }
    
    public interface JSError extends JSObject {
        @JSProperty
        public String getMessage();
        
        @JSProperty
        public int getCode();
        
        @JSProperty
        public String getName();
    }
    
    @JSFunctor
    public interface NoArgCallback extends JSObject {
        public void callback();
    }
    
    @JSFunctor interface OneArgCallback<T> extends JSObject {
        public void callback(T result);
    }
    
    public interface FileMetadata extends JSObject {
        @JSProperty
        public int getSize();
    }
    
    @JSFunctor
    public interface MetadataCallback extends JSObject {
        public void onMetadata(FileMetadata data);
    }
    
    
    
    /// Video and Audio stuff
    
    public interface HTMLMediaElement extends HTMLElement {
        public void play();
        public void pause();
        @JSProperty
        public double getCurrentTime();
        
        @JSProperty
        public void setCurrentTime(double time);
        
        @JSProperty
        public double getDuration();
        
        @JSProperty
        public boolean isPaused();
        
        @JSProperty
        public boolean isEnded();
        
        @JSProperty
        public double getVolume();
        
        @JSProperty
        public void setVolume(double volume);
        
        @JSProperty
        public void setMuted(boolean muted);
        
        @JSProperty
        public boolean isMuted();
        
        @JSProperty
        public void setSrc(String src);
        
        @JSProperty
        public String getSrc();
        
        @JSProperty
        public boolean isControls();
        
        @JSProperty
        public void setControls(boolean controls);
        
        @JSProperty
        public int getReadyState();
        
        
        @JSProperty
        public boolean isSeeking();
        
        
    }
    
    public interface HTMLVideoElement extends HTMLMediaElement {
        @JSProperty
        public boolean isDisplayingFullscreen();
        
        @JSProperty
        public boolean getSupportsFullscreen();
        
        public void enterFullscreen();
        public void exitFullscreen();
    }
    
    public interface HTMLAudioElement extends HTMLMediaElement {
        
    }
    
    public interface CN1Native extends JSObject {
        public boolean isMobile();
        public void capturePhoto(CapturePhotoCallback callback, int targetWidth, int targetHeight);
        public String getBundledAssetAsDataURL(String assetName);
    }
    
    @JSFunctor
    public interface CapturePhotoCallback extends JSObject {
        public void callback(HTMLCanvasElement canvas);
    }
    
    public interface CanvasExt extends HTMLCanvasElement {
        public void toBlob(BlobCallback callback);
        public void toBlob(BlobCallback callback, String type);
        public void toBlob(BlobCallback callback, String type, double quality);
    }
    
    @JSFunctor
    public interface BlobCallback extends JSObject {
        public void onBlob(Blob blob);
    }
    
    public interface ImageExt extends HTMLImageElement {
        @JSProperty
        public boolean isComplete();
    }
    
    public interface Navigator extends JSObject {
        @JSProperty
        public Geolocation getGeolocation();
        
        @JSProperty
        public String getAppCodeName();
        
        @JSProperty
        public String getAppName();
        
        @JSProperty
        public String getAppVersion();
        
        @JSProperty
        public boolean isCookieEnabled();
        
        @JSProperty
        public String getLanguage();
        
        @JSProperty
        public String isOnLine();
        
        @JSProperty
        public String getPlatform();
        
        @JSProperty
        public String getUserAgent();
        
    }
    
    public interface Geolocation extends JSObject {
        public void getCurrentPosition(PositionCallback onLocation, ErrorCallback onError, PositionOptions opts);
        public int watchPosition(PositionCallback onLocation, ErrorCallback onError, PositionOptions opts);
        public void clearWatch(int watchId);
        
        public interface Position extends JSObject {
            @JSProperty
            public Coord getCoords();
            
            @JSProperty
            public int getTimestamp();
        }
        
        public interface PositionOptions extends JSObject {
            @JSProperty
            public boolean isEnableHighAccuracy();
            
            @JSProperty
            public void setEnableHighAccuracy(boolean high);
            
            @JSProperty
            public int getTimeout();
            
            @JSProperty
            public void setTimeout(int timeout);
            
            @JSProperty
            public int getMaximumAge();
            
            @JSProperty
            public void setMaximumAge(int age);
        }
        
        public interface Coord extends JSObject {
            @JSProperty
            public double getLatitude();
            
            @JSProperty
            public double getLongitude();
            
            @JSProperty
            public double getAccuracy();
            
            @JSProperty
            public double getAltitude();
            
            @JSProperty
            public double getAltitudeAccuracy();
            
            @JSProperty
            public double getHeading();
            
            @JSProperty
            public double getSpeed();
            
            
        }
    }
    
    @JSFunctor
    public interface PositionCallback extends JSObject {
        public void onLocation(Geolocation.Position position);
    }
    
    @JSFunctor
    public interface DataURLCallback extends JSObject {
        public void callback(JSString dataUrl);
    }
    
    public interface KeyEvent extends Event {
        @JSProperty
        public int getKeyCode();
        
        @JSProperty
        public int getWhich();
        
        @JSProperty
        public int getCharCode();
        
        @JSProperty
        public boolean isCtrlKey();
        
        @JSProperty
        public boolean isShiftKey();
        
        @JSProperty
        public boolean isAltKey();
        
        @JSProperty
        public boolean isMetaKey();
        
        @JSProperty
        public boolean isRepeat();
    }
    
    
    
    public interface WheelEvent extends MouseEvent {
        @JSProperty
        public double getDeltaX();
        
        @JSProperty
        public double getDeltaY();
        
        @JSProperty
        public double getDeltaZ();
        
        @JSProperty
        public double getDeltaMode();
    }
    
    public interface TextElement extends HTMLInputElement {
        public void setSelectionRange(int start, int end);
    }
    
    
    public interface IntlNumberFormat extends JSObject {
        public String format(int n);
        
        public String format(double n);
        
        @JSProperty
        public JSObject getResolvedOptions();
        
    }
    
    public interface IntlDateTimeFormat extends JSObject {
        public String format(JSDate d);
        
        @JSProperty
        public JSObject getResolvedOptions();
               
    }
    
    public interface IntlDateTimeFormatOptions extends JSObject {
        public static final String WEEKDAY_NARROW = "narrow";
        public static final String WEEKDAY_SHORT = "short";
        public static final String WEEKDAY_LONG = "long";
        
        public static final String ERA_NARROW = "narrow";
        public static final String ERA_SHORT = "short";
        public static final String ERA_LONG = "long";
        
        public static final String YEAR_NUMERIC = "numeric";
        public static final String YEAR_2_DIGIT = "2-digit";
        
        public static final String MONTH_NARROW = "narrow";
        public static final String MONTH_SHORT = "short";
        public static final String MONTH_NUMERIC = "numeric";
        public static final String MONTH_2_DIGIT = "2-digit";
        
        public static final String DAY_NUMERIC = "numeric";
        public static final String DAY_2_DIGIT = "2-digit";
        
        public static final String HOUR_NUMERIC = "numeric";
        public static final String HOUR_2_DIGIT = "2-digit";
        
        public static final String MINUTE_NUMERIC = "numeric";
        public static final String MINUTE_2_DIGIT = "2-digit";
        
        public static final String SECOND_NUMERIC = "numeric";
        public static final String SECOND_2_DIGIT = "2-digit";
        
        public static final String TIMEZONE_SHORT = "short";
        public static final String TIMEZONE_LONG = "long";
        
        @JSProperty
        public String getLocaleMatcher();
        
        @JSProperty
        public void setLocaleMatcher(String s);
        
        @JSProperty
        public String getTimeZone();
        
        @JSProperty
        public void setTimeZone(String tz);
        
        @JSProperty
        public boolean isHour12();
        
        @JSProperty
        public void setHour12(boolean hour12);
        
        @JSProperty
        public String getFormatMatcher();
        
        @JSProperty
        public void setFormatMatcher(String matcher);
        
        @JSProperty
        public String getWeekday();
        
        @JSProperty
        public void setWeekday(String format);
        
        @JSProperty
        public String getEra();
        
        @JSProperty
        public void setEra(String format);
        
        @JSProperty
        public String getYear();
        
        @JSProperty
        public void setYear(String format);
        
        @JSProperty
        public String getMonth();
        
        @JSProperty
        public void setMonth(String format);
        
        @JSProperty
        public String getDay();
        
        @JSProperty
        public void setDay(String format);
        
        @JSProperty
        public String getHour();
        
        @JSProperty
        public String setHour(String format);
        
        @JSProperty
        public String getMinute();
        
        @JSProperty
        public void setMinute(String format);
        
        @JSProperty
        public String getTimeZoneName();
        
        @JSProperty
        public String setTimeZoneName(String format);
        
        
        
        
    }
    
    
    
}
