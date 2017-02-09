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
package com.codename1.impl.android;

import android.Manifest;
import com.codename1.location.AndroidLocationManager;
import android.app.*;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.MotionEvent;
import com.codename1.codescan.ScanResult;
import com.codename1.media.Media;
import com.codename1.ui.geom.Dimension;


import android.webkit.CookieSyncManager;
import android.content.*;
import android.content.pm.*;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Vibrator;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.codename1.ui.BrowserComponent;

import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.Image;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.events.ActionEvent;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.VirtualKeyboardInterface;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.Resources;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Vector;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.v4.app.NotificationCompat;
import android.telephony.SmsManager;
import android.telephony.gsm.GsmCellLocation;
import android.text.Html;
import android.view.*;
import android.view.View.MeasureSpec;
import android.webkit.*;
import android.widget.*;
import com.codename1.background.BackgroundFetch;
import com.codename1.codescan.CodeScanner;
import com.codename1.contacts.Contact;
import com.codename1.db.Database;
import com.codename1.io.BufferedInputStream;
import com.codename1.io.BufferedOutputStream;
import com.codename1.io.*;
import com.codename1.l10n.L10NManager;
import com.codename1.location.LocationManager;
import com.codename1.media.Audio;
import com.codename1.media.AudioService;
import com.codename1.media.MediaProxy;
import com.codename1.messaging.Message;
import com.codename1.notifications.LocalNotification;
import com.codename1.payment.Purchase;
import com.codename1.push.PushCallback;
import com.codename1.ui.*;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.animations.CommonTransitions;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.geom.Shape;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.util.Callback;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.codename1.util.StringUtil;
import java.io.*;
import java.net.CookieHandler;
import java.net.ServerSocket;
import java.text.ParseException;
import java.util.*;
//import android.webkit.JavascriptInterface;

public class AndroidImplementation extends CodenameOneImplementation implements IntentResultListener {
    public static final Thread.UncaughtExceptionHandler exceptionHandler = new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            if(com.codename1.io.Log.isCrashBound()) {
                com.codename1.io.Log.p("Uncaught exception in thread " + t.getName());
                com.codename1.io.Log.e(e);
                com.codename1.io.Log.sendLog();
            }
        }
    };

    /**
     * make sure these important keys have a negative value when passed to
     * Codename One or they might be interpreted as characters.
     */
    static final int DROID_IMPL_KEY_LEFT = -23446;
    static final int DROID_IMPL_KEY_RIGHT = -23447;
    static final int DROID_IMPL_KEY_UP = -23448;
    static final int DROID_IMPL_KEY_DOWN = -23449;
    static final int DROID_IMPL_KEY_FIRE = -23450;
    static final int DROID_IMPL_KEY_MENU = -23451;
    static final int DROID_IMPL_KEY_BACK = -23452;
    static final int DROID_IMPL_KEY_BACKSPACE = -23453;
    static final int DROID_IMPL_KEY_CLEAR = -23454;
    static final int DROID_IMPL_KEY_SEARCH = -23455;
    static final int DROID_IMPL_KEY_CALL = -23456;
    static final int DROID_IMPL_KEY_VOLUME_UP = -23457;
    static final int DROID_IMPL_KEY_VOLUME_DOWN = -23458;
    static final int DROID_IMPL_KEY_MUTE = -23459;
    static int[] leftSK = new int[]{DROID_IMPL_KEY_MENU};

    /**
     * @return the activity
     */
    public static CodenameOneActivity getActivity() {
        return activity;
    }

    /**
     * @param aActivity the activity to set
     */
    public static void setActivity(CodenameOneActivity aActivity) {
        activity = aActivity;
    }
    CodenameOneSurface myView = null;
    CodenameOneTextPaint defaultFont;
    private final char[] tmpchar = new char[1];
    private final Rect tmprect = new Rect();
    protected int defaultFontHeight;
    private Vibrator v = null;
    private boolean vibrateInitialized = false;
    private int displayWidth;
    private int displayHeight;
    static CodenameOneActivity activity;
    private static Context context;
    RelativeLayout relativeLayout;
    final Vector nativePeers = new Vector();
    int lastDirectionalKeyEventReceivedByWrapper;
    private EventDispatcher callback;
    private int timeout = -1;
    private CodeScannerImpl scannerInstance;
    private HashMap apIds;
    private static View viewBelow;
    private static View viewAbove;
    private static int aboveSpacing;
    private static int belowSpacing;
    public static boolean asyncView = false;
    public static boolean textureView = false;
    private Media background;
    private boolean asyncEditMode = false;
    private boolean compatPaintMode;
    private MediaRecorder recorder = null;

    private boolean superPeerMode = true;

    /**
     * Keeps track of running contexts.
     * @see #startContext(Context)
     * @see #stopContext(Context)
     */
    private static HashSet<Context> activeContexts = new HashSet<Context>();

    /**
     * A method to be called when a Context begins its execution.  This adds the
     * context to the context set.  When the contenxt's execution completes, it should
     * call {@link #stopContext} to clear up resources.
     * @param ctx The context that is starting.
     * @see #stopContext(Context)
     */
    public static void startContext(Context ctx) {

        while (deinitializingEdt) {
            // It is possible that deinitialize was called just before the
            // last context was destroyed so there is a pending deinitialize
            // working its way through the system.  Give it some time
            // before forcing the deinitialize
            System.out.println("Waiting for deinitializing to complete before starting a new initialization");
            try {
                Thread.sleep(30);

            } catch (Exception ex){}
        }
        if (deinitializing && instance != null) {
            instance.deinitialize();
        }
        synchronized(activeContexts) {
            activeContexts.add(ctx);
            if (instance == null) {
                // If this is our first rodeo, just call Display.init() as that should
                // be sufficient to set everything up.
                Display.init(ctx);
            } else {
                // If we've initialized before, we should "re-initialize" the implementation
                // Reinitializing will force views to be created even if the EDT was already
                // running in background mode.
                reinit(ctx);
            }
        }
    }

    /**
     * Cleans up resources in the given context.  This method should be called by
     * any Activity or Service that called startContext() when it started.
     * @param ctx The context to stop.
     *
     * @see #startContext(Context)
     */
    public static void stopContext(Context ctx) {
        synchronized(activeContexts) {
            activeContexts.remove(ctx);
            if (activeContexts.isEmpty()) {
                // If we are the last context, we should deinitialize
                syncDeinitialize();
            } else {
                if (instance != null && getActivity() != null) {
                    // if this is an activity, then we should clean up
                    // our UI resources anyways because the last context
                    // to be cleaned up might not have access to the UI thread.
                    instance.deinitialize();
                }
            }
        }
    }

    @Override
    public void setPlatformHint(String key, String value) {
        if(key.equals("platformHint.compatPaintMode")) {
            compatPaintMode = value.equalsIgnoreCase("true");
            return;
        }
        if(key.equals("platformHint.legacyPaint")) {
            AndroidAsyncView.legacyPaintLogic = value.equalsIgnoreCase("true");;
        }
    }

    
    /**
     * This method in used internally for ads
     * @param above shown above the view
     * @param below shown below the view
     */
    public static void setViewAboveBelow(View above, View below, int spacingAbove, int spacingBelow) {
        viewBelow = below;
        viewAbove = above;
        aboveSpacing = spacingAbove;
        belowSpacing = spacingBelow;
    }
    
    static boolean hasViewAboveBelow(){
        return viewBelow != null || viewAbove != null;
    }
    
    /**
     * Copy the input stream into the output stream, closes both streams when finishing or in
     * a case of an exception
     * 
     * @param i source
     * @param o destination
     */
    private static void copy(InputStream i, OutputStream o) throws IOException {
        copy(i, o, 8192);
    }

    /**
     * Copy the input stream into the output stream, closes both streams when finishing or in
     * a case of an exception
     *
     * @param i source
     * @param o destination
     * @param bufferSize the size of the buffer, which should be a power of 2 large enoguh
     */
    private static void copy(InputStream i, OutputStream o, int bufferSize) throws IOException {
        try {
            byte[] buffer = new byte[bufferSize];
            int size = i.read(buffer);
            while(size > -1) {
                o.write(buffer, 0, size);
                size = i.read(buffer);
            }
        } finally {
            sCleanup(o);
            sCleanup(i);
        }
    }

    private static void sCleanup(Object o) {
        try {
            if(o != null) {
                if(o instanceof InputStream) {
                    ((InputStream)o).close();
                    return;
                }
                if(o instanceof OutputStream) {
                    ((OutputStream)o).close();
                    return;
                }
            }
        } catch(Throwable t) {}
    }
    
    /**
     * Copied here since the cleanup method in util would crash append notification that runs when the app isn't in the foreground
     */
    private static byte[] readInputStream(InputStream i) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        copy(i, b);
        return b.toByteArray();
    }
    
    
    public static void appendNotification(String type, String body, Context a) {
        try {
            String[] fileList = a.fileList();
            byte[] data = null;
            for (int iter = 0; iter < fileList.length; iter++) {
                if (fileList[iter].equals("CN1$AndroidPendingNotifications")) {
                    InputStream is = a.openFileInput("CN1$AndroidPendingNotifications");
                    if(is != null) {
                        data = readInputStream(is);
                        sCleanup(a);
                        break;
                    }
                }
            }
            DataOutputStream os = new DataOutputStream(a.openFileOutput("CN1$AndroidPendingNotifications", 0));
            if(data != null) {
                data[0]++;
                os.write(data);
            } else {
                os.writeByte(1);
            }
            if(type != null) {
                os.writeBoolean(true);
                os.writeUTF(type);
            } else {
                os.writeBoolean(false);
            }
            os.writeUTF(body);
            os.writeLong(System.currentTimeMillis());
        } catch(IOException err) {
            err.printStackTrace();
        }
    }

    public static void firePendingPushes(final PushCallback c, Context a) {
        try {
            if(c != null) {
                InputStream i = a.openFileInput("CN1$AndroidPendingNotifications");
                if(i == null) {
                    return;
                }
                DataInputStream is = new DataInputStream(i);
                int count = is.readByte();
                for(int iter = 0 ; iter < count ; iter++) {
                    boolean hasType = is.readBoolean();
                    String actualType = null;
                    if(hasType) {
                        actualType = is.readUTF();
                    }
                    final String t = actualType;
                    final String b = is.readUTF();
                    long s = is.readLong();
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            Display.getInstance().setProperty("pendingPush", "true");                            
                            Display.getInstance().setProperty("pushType", t);
                            if(t != null && ("3".equals(t) || "6".equals(t))) {                                
                                String[] a = b.split(";");
                                c.push(a[0]);
                                c.push(a[1]);
                            } else {
                                c.push(b);
                            }
                            Display.getInstance().setProperty("pendingPush", null);                            
                        }
                    });
                }
                a.deleteFile("CN1$AndroidPendingNotifications");
            }
        } catch(IOException err) {
        }
    }
    
    public static String[] getPendingPush(String type, Context a) {
        InputStream i = null;
        try {
            i = a.openFileInput("CN1$AndroidPendingNotifications");
            if (i == null) {
                return null;
            }
            DataInputStream is = new DataInputStream(i);
            int count = is.readByte();
            Vector v = new Vector<String>();
            for (int iter = 0; iter < count; iter++) {
                boolean hasType = is.readBoolean();
                String actualType = null;
                if (hasType) {
                    actualType = is.readUTF();
                }
                final String t = actualType;
                final String b = is.readUTF();
                long s = is.readLong();
                if(t != null && ("3".equals(t) || "6".equals(t))) {                               
                    String[] m = b.split(";");
                    v.add(m[0]);
                } else if(t != null && "4".equals(t)){
                    String[] m = b.split(";");
                    v.add(m[1]);
                } else if(t != null && "2".equals(t)){
                    continue;
                }else{
                    v.add(b);
                }
            }
            String [] retVal = new String[v.size()];
            for (int j = 0; j < retVal.length; j++) {
                retVal[j] = (String)v.get(j);
            }
            return retVal;
            
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if(i != null){
                    i.close();
                }
            } catch (IOException ex) {
            }
        }
        return null;
    }
    
    private static AndroidImplementation instance;
    
    public static AndroidImplementation getInstance() {
        return instance;
    }
    
    public static void clearAppArg() {
        if (instance != null) {
            instance.setAppArg(null);
        }
    }
    
    public static Context getContext() {
        Context out = getActivity();
        if (out != null) {
            return out;
        }
        return context;
    }
    
    public void setContext(Context c) {
        context = c;
    }
    
    @Override
    public void init(Object m) {
        if (m instanceof CodenameOneActivity) {
            setContext(null);
            setActivity((CodenameOneActivity) m);
        } else {
            setActivity(null);
            setContext((Context)m);
        }
        
        instance = this;
        if(getActivity() != null && getActivity().hasUI()){
            if (!hasActionBar()) {
                try {
                    getActivity().requestWindowFeature(Window.FEATURE_NO_TITLE);
                } catch (Exception e) {
                    //Log.d("Codename One", "No idea why this throws a Runtime Error", e);
                }
            } else {
                getActivity().invalidateOptionsMenu();
                try {
                    getActivity().requestWindowFeature(Window.FEATURE_ACTION_BAR);
                    getActivity().requestWindowFeature(Window.FEATURE_PROGRESS);                

                    if(android.os.Build.VERSION.SDK_INT >= 21){
                        //WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                        getActivity().getWindow().addFlags(-2147483648);
                    }
                } catch (Exception e) {
                    //Log.d("Codename One", "No idea why this throws a Runtime Error", e);
                }
                NotifyActionBar notify = new NotifyActionBar(getActivity(), false);
                notify.run();
            }

            if(Display.getInstance().getProperty("StatusbarHidden", "").equals("true")){
                getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }

            if(Display.getInstance().getProperty("KeepScreenOn", "").equals("true")){
                getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }

            if(Display.getInstance().getProperty("DisableScreenshots", "").equals("true")){
                getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            }

            if (m instanceof CodenameOneActivity) {
                ((CodenameOneActivity) m).setDefaultIntentResultListener(this);
                ((CodenameOneActivity) m).setIntentResultListener(this);
            }

            /**
             * translate our default font height depending on the screen density.
             * this is required for new high resolution devices. otherwise
             * everything looks awfully small.
             *
             * we use our default font height value of 16 and go from there. i
             * thought about using new Paint().getTextSize() for this value but if
             * some new version of android suddenly returns values already tranlated
             * to the screen then we might end up with too large fonts. the
             * documentation is not very precise on that.
             */
            final int defaultFontPixelHeight = 16;
            this.defaultFontHeight = this.translatePixelForDPI(defaultFontPixelHeight);


            this.defaultFont = (CodenameOneTextPaint) ((NativeFont) this.createFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM)).font;
            Display.getInstance().setTransitionYield(-1);

            initSurface();
            /**
             * devices are extremely sensitive so dragging should start a little
             * later than suggested by default implementation.
             */
            this.setDragStartPercentage(1);
            VirtualKeyboardInterface vkb = new AndroidKeyboard(this);
            Display.getInstance().registerVirtualKeyboard(vkb);
            Display.getInstance().setDefaultVirtualKeyboard(vkb);

            InPlaceEditView.endEdit();

            getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

            if (nativePeers.size() > 0) {
                for (int i = 0; i < nativePeers.size(); i++) {
                    ((AndroidImplementation.AndroidPeer) nativePeers.elementAt(i)).init();
                }
            }
        } else {
            /**
             * translate our default font height depending on the screen density.
             * this is required for new high resolution devices. otherwise
             * everything looks awfully small.
             *
             * we use our default font height value of 16 and go from there. i
             * thought about using new Paint().getTextSize() for this value but if
             * some new version of android suddenly returns values already tranlated
             * to the screen then we might end up with too large fonts. the
             * documentation is not very precise on that.
             */
            final int defaultFontPixelHeight = 16;
            this.defaultFontHeight = this.translatePixelForDPI(defaultFontPixelHeight);


            this.defaultFont = (CodenameOneTextPaint) ((NativeFont) this.createFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM)).font;
        }
        HttpURLConnection.setFollowRedirects(false);
        CookieHandler.setDefault(null);
    }



    @Override
    public boolean isInitialized(){
// Removing the check for null view to prevent strange things from happening when
// calling from a Service context.  
//        if(getActivity() != null && myView == null){
//            //if the view is null deinitialize the Display
//            if(super.isInitialized()){
//                syncDeinitialize();
//            }    
//            return false;
//        }
        return super.isInitialized();
    }

    /**
     * Reinitializes CN1.
     * @param i Context to initialize it with.
     *
     * @see #startContext(Context)
     */
    private static void reinit(Object i) {
        if (instance != null && ((i instanceof CodenameOneActivity) || instance.myView == null)) {
            instance.init(i);
        }
        Display.init(i);

        // This is a hack to fix an issue that caused the screen to appear blank when
        // the app is loaded from memory after being unloaded.

        // This issue only seems to occur when the Activity had been unloaded
        // so to test this you'll need to check the "Don't keep activities" checkbox under/
        // Developer options.
        // Developer options.
        Display.getInstance().callSerially(new Runnable() {
            public void run() {
                Display.getInstance().invokeAndBlock(new Runnable(){ public void run(){
                    try {
                        Thread.sleep(50);

                    } catch (Exception ex){}
                }});
                if (!Display.isInitialized() || Display.getInstance().isMinimized()) {
                    return;
                }
                Form cur = Display.getInstance().getCurrent();
                if (cur != null) {
                    cur.forceRevalidate();
                }
            }

        });
    }
    
    private static class InvalidateOptionsMenuImpl implements Runnable {
        private Activity activity;

        public InvalidateOptionsMenuImpl(Activity activity) {
            this.activity = activity;
        }
        
        @Override
        public void run() {
            activity.invalidateOptionsMenu();
        }
    }

    private boolean hasActionBar() {
        return android.os.Build.VERSION.SDK_INT >= 11;
    }

    public int translatePixelForDPI(int pixel) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, pixel,
                getContext().getResources().getDisplayMetrics());
    }

    /**
     * Returns the platform EDT thread priority
     */
    public int getEDTThreadPriority(){
        return Thread.NORM_PRIORITY;
    }
    
    @Override
    public int getDeviceDensity() {
        DisplayMetrics metrics = new DisplayMetrics();
        if (getActivity() != null) {
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
        } else {
            metrics = getContext().getResources().getDisplayMetrics();
        }
        switch (metrics.densityDpi) {
            case DisplayMetrics.DENSITY_LOW:
                return Display.DENSITY_LOW;
            case DisplayMetrics.DENSITY_HIGH:
            case 213: // DENSITY_TV 
                return Display.DENSITY_HIGH;
            case DisplayMetrics.DENSITY_XHIGH:
                return Display.DENSITY_VERY_HIGH;
            case 400: // DisplayMetrics.DENSITY_400
            case 420: // DisplayMetrics.DENSITY_420
            case 480: // DisplayMetrics.DENSITY_XXHIGH
                return Display.DENSITY_HD;
            case 560: // DisplayMetrics.DENSITY_560
                return Display.DENSITY_560;
            case 640: // DisplayMetrics.DENSITY_XXXHIGH 
                return Display.DENSITY_2HD;
                 
            default:
                if(metrics.densityDpi > 640) {
                    return Display.DENSITY_4K;
                }
                return Display.DENSITY_MEDIUM;
        }
    }

    /**
     * A status flag to indicate that CN1 is in the process of deinitializing.
     */
    private static boolean deinitializing;
    private static boolean deinitializingEdt;

    public static void syncDeinitialize() {
        if (deinitializingEdt){
            return;
        }
        deinitializingEdt = true; // This will get unset in {@link #deinitialize()}
        deinitializing = true;
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                Display.deinitialize();
                deinitializingEdt = false;
            }
        });
    }
    
    public void deinitialize() {
        //activity.getWindowManager().removeView(relativeLayout);

        if (getActivity() != null) {

            Runnable r = new Runnable() {
                public void run() {
                    if (nativePeers.size() > 0) {
                        for (int i = 0; i < nativePeers.size(); i++) {
                            ((AndroidImplementation.AndroidPeer) nativePeers.elementAt(i)).deinit();
                        }
                    }
                    if (relativeLayout != null) {
                        relativeLayout.removeAllViews();
                    }
                    relativeLayout = null;
                    myView = null;
                    deinitializing = false;
                }
            };

            if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                r.run();
            } else {
                getActivity().runOnUiThread(r);
            }
        } else {
            deinitializing = false;
        }
    }
    
    /**
     * init view. a lot of back and forth between this thread and the UI thread.
     */
    private void initSurface() {
        if (getActivity() != null && myView == null) {
            relativeLayout=  new RelativeLayout(getActivity());
            relativeLayout.setLayoutParams(new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.FILL_PARENT,
                    RelativeLayout.LayoutParams.FILL_PARENT));
            relativeLayout.setFocusable(false);

            getActivity().getWindow().setBackgroundDrawable(null);
            if(asyncView) {
                if(android.os.Build.VERSION.SDK_INT < 14){
                    myView = new AndroidSurfaceView(getActivity(), AndroidImplementation.this);        
                } else {
                    int hardwareAcceleration = 16777216;
                    getActivity().getWindow().setFlags(hardwareAcceleration, hardwareAcceleration);
                    myView = new AndroidAsyncView(getActivity(), AndroidImplementation.this);                
                }
            } else {
                int hardwareAcceleration = 16777216;
                getActivity().getWindow().setFlags(hardwareAcceleration, hardwareAcceleration);
                superPeerMode = true;
                myView = new AndroidAsyncView(getActivity(), AndroidImplementation.this);                
            }
            myView.getAndroidView().setVisibility(View.VISIBLE);

            relativeLayout.addView(myView.getAndroidView());
            myView.getAndroidView().setVisibility(View.VISIBLE);

            int id = getActivity().getResources().getIdentifier("main", "layout", getActivity().getApplicationInfo().packageName);
            RelativeLayout root = (RelativeLayout) LayoutInflater.from(getActivity()).inflate(id, null);
            if(viewAbove != null) {
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                lp.addRule(RelativeLayout.CENTER_HORIZONTAL);

                RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                lp2.setMargins(0, 0, aboveSpacing, 0);
                relativeLayout.setLayoutParams(lp2);
                root.addView(viewAbove, lp);
            }
            root.addView(relativeLayout);
            if(viewBelow != null) {
                RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                lp.addRule(RelativeLayout.CENTER_HORIZONTAL);

                RelativeLayout.LayoutParams lp2 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                lp2.setMargins(0, 0, 0, belowSpacing);
                relativeLayout.setLayoutParams(lp2);
                root.addView(viewBelow, lp);
            }
            getActivity().setContentView(root);
            myView.getAndroidView().requestFocus();
        }
    }

    @Override
    public void confirmControlView() {
        if(myView == null){
            return;
        }
        myView.getAndroidView().setVisibility(View.VISIBLE);
        //ugly workaround for a bug where on some android versions the async view
        //came back black from the background.
        if(myView instanceof AndroidAsyncView){
            new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        Thread.sleep(1000);                        
                        ((AndroidAsyncView)myView).setPaintViewOnBuffer(false);
                    } catch (Exception e) {
                    }
                }
            }).start();
        }
    }

    public void hideNotifyPublic() {
        super.hideNotify();
        saveTextEditingState();
    }

    public void showNotifyPublic() {
        super.showNotify();
    }

    @Override
    public boolean isMinimized() {
        return getActivity() == null || ((CodenameOneActivity)getActivity()).isBackground();
    }

    @Override
    public boolean minimizeApplication() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startMain.putExtra("WaitForResult", Boolean.FALSE);
        getContext().startActivity(startMain);
        return true;
    }

    @Override
    public void restoreMinimizedApplication() {
        if (getActivity() != null) {
            Intent i = new Intent(getActivity(), getActivity().getClass());
            i.setAction(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            getContext().startActivity(i);
        }
    }

    @Override
    public boolean isNativeInputImmediate() {
        return true;
    }
    
    public void editString(final Component cmp, int maxSize, final int constraint, String text, int keyCode) {
        if (keyCode > 0 && getKeyboardType() == Display.KEYBOARD_TYPE_QWERTY) {
            text += (char) keyCode;
        }
        InPlaceEditView.edit(this, cmp, constraint);
    }

    protected boolean editInProgress() {
        return InPlaceEditView.isEditing();
    }

    @Override
    public boolean isAsyncEditMode() {
        return asyncEditMode;
    }

    void setAsyncEditMode(boolean async) {
        asyncEditMode = async;
    }
    
    void callHideTextEditor() {
        super.hideTextEditor();
    }

    @Override
    public void hideTextEditor() {
        InPlaceEditView.hideActiveTextEditor();
    }

    @Override
    public boolean isNativeEditorVisible(Component c) {
        return super.isNativeEditorVisible(c) && !InPlaceEditView.isActiveTextEditorHidden();
    }
    
    public static void stopEditing() {
        stopEditing(false);
    }
    
    public static void stopEditing(final boolean forceVKBClose){
        if (getActivity() == null) {
            return;
        }
        final boolean[] flag = new boolean[]{false};

        // InPlaceEditView.endEdit must be called from the UI thread.
        // We must wait for this call to be over, otherwise Codename One's painting
        // of the next form will be garbled.
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Must be called from the UI thread
                InPlaceEditView.stopEdit(forceVKBClose);

                synchronized (flag) {
                    flag[0] = true;
                    flag.notify();
                }
            }
        });

        if (!flag[0]) {
            // Wait (if necessary) for the asynchronous runOnUiThread to do its work
            synchronized (flag) {

                try {
                    flag.wait();
                } catch (InterruptedException e) {
                }
            }
        }        
    }
    
    @Override
    public void saveTextEditingState() {
        stopEditing(true);
    }
    
    @Override
    public void stopTextEditing() {    
        saveTextEditingState();
    }

    protected void setLastSizeChangedWH(int w, int h) {
        // not used?
        //this.lastSizeChangeW = w;
        //this.lastSizeChangeH = h;
    }

    /*@Override
    public boolean handleEDTException(final Throwable err) {

        final boolean[] messageComplete = new boolean[]{false};

        Log.e("Codename One", "Err on EDT", err);

        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                UIManager m = UIManager.getInstance();
                final FrameLayout frameLayout = new FrameLayout(
                        activity);
                final TextView textView = new TextView(
                        activity);
                textView.setGravity(Gravity.CENTER);
                frameLayout.addView(textView, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.FILL_PARENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT));
                textView.setText("An internal application error occurred: " + err.toString());
                AlertDialog.Builder bob = new AlertDialog.Builder(
                        activity);
                bob.setView(frameLayout);
                bob.setTitle("");
                bob.setPositiveButton(m.localize("ok", "OK"),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                d.dismiss();
                                synchronized (messageComplete) {
                                    messageComplete[0] = true;
                                    messageComplete.notify();
                                }
                            }
                        });
                AlertDialog editDialog = bob.create();
                editDialog.show();
            }
        });

        synchronized (messageComplete) {
            if (messageComplete[0]) {
                return true;
            }
            try {
                messageComplete.wait();
            } catch (Exception ignored) {
                ;
            }
        }
        return true;
    }*/

    @Override
    public InputStream getResourceAsStream(Class cls, String resource) {
        try {
            if (resource.startsWith("/")) {
                resource = resource.substring(1);
            }
            return getContext().getAssets().open(resource);
        } catch (IOException ex) {
            Log.i("Codename One", "Resource not found: " + resource);
            return null;
        }
    }

    @Override
    protected void pointerPressed(final int x, final int y) {
        super.pointerPressed(x, y);
    }

    @Override
    protected void pointerPressed(final int[] x, final int[] y) {
        super.pointerPressed(x, y);
    }

    @Override
    protected void pointerReleased(final int x, final int y) {
        super.pointerReleased(x, y);
    }

    @Override
    protected void pointerReleased(final int[] x, final int[] y) {
        super.pointerReleased(x, y);
    }

    @Override
    protected void pointerDragged(int x, int y) {
        super.pointerDragged(x, y);
    }

    @Override
    protected void pointerDragged(int[] x, int[] y) {
        super.pointerDragged(x, y);
    }

    @Override
    protected int getDragAutoActivationThreshold() {
        return 1000000;
    }

    @Override
    public void flushGraphics() {
        if (myView != null) {
            myView.flushGraphics();
        }

    }

    @Override
    public void flushGraphics(int x, int y, int width, int height) {
        this.tmprect.set(x, y, x + width, y + height);
        if (myView != null) {
            myView.flushGraphics(this.tmprect);
        }
    }

    @Override
    public int charWidth(Object nativeFont, char ch) {
        this.tmpchar[0] = ch;
        float w = (nativeFont == null ? this.defaultFont
                : (Paint) ((NativeFont) nativeFont).font).measureText(this.tmpchar, 0, 1);
        if (w - (int) w > 0) {
            return (int) (w + 1);
        }
        return (int) w;
    }

    @Override
    public int charsWidth(Object nativeFont, char[] ch, int offset, int length) {
        float w = (nativeFont == null ? this.defaultFont
                : (Paint) ((NativeFont) nativeFont).font).measureText(ch, offset, length);
        if (w - (int) w > 0) {
            return (int) (w + 1);
        }
        return (int) w;
    }

    @Override
    public int stringWidth(Object nativeFont, String str) {
        float w = (nativeFont == null ? this.defaultFont
                : (Paint) ((NativeFont) nativeFont).font).measureText(str);
        if (w - (int) w > 0) {
            return (int) (w + 1);
        }
        return (int) w;
    }

    @Override
    public void setNativeFont(Object graphics, Object font) {
        if (font == null) {
            font = this.defaultFont;
        }
        if (font instanceof NativeFont) {
            ((AndroidGraphics) graphics).setFont((CodenameOneTextPaint) ((NativeFont) font).font);
        } else {
            ((AndroidGraphics) graphics).setFont((CodenameOneTextPaint) font);
        }
    }

    @Override
    public int getHeight(Object nativeFont) {
        CodenameOneTextPaint font = (nativeFont == null ? this.defaultFont
                : (CodenameOneTextPaint) ((NativeFont) nativeFont).font);
        if(font.fontHeight < 0) {
            font.fontHeight = font.getFontMetricsInt(font.getFontMetricsInt());
        }
        return font.fontHeight;
    }

    @Override
    public int getFontAscent(Object nativeFont) {
        Paint font = (nativeFont == null ? this.defaultFont
                : (Paint) ((NativeFont) nativeFont).font);
        return -Math.round(font.getFontMetrics().ascent);
    }

    @Override
    public int getFontDescent(Object nativeFont) {
        Paint font = (nativeFont == null ? this.defaultFont
                : (Paint) ((NativeFont) nativeFont).font);
        return Math.abs(Math.round(font.getFontMetrics().descent));
    }

    @Override
    public boolean isBaselineTextSupported() {
        return true;
    }
    
    
    

    
    
    public int getFace(Object nativeFont) {
        if (nativeFont == null) {
            return Font.FACE_SYSTEM;
        }
        return ((NativeFont) nativeFont).face;
    }

    public int getStyle(Object nativeFont) {
        if (nativeFont == null) {
            return Font.STYLE_PLAIN;
        }
        return ((NativeFont) nativeFont).style;
    }

    @Override
    public int getSize(Object nativeFont) {
        if (nativeFont == null) {
            return Font.SIZE_MEDIUM;
        }
        return ((NativeFont) nativeFont).size;
    }
    
    @Override
    public boolean isTrueTypeSupported() {
        return true;
    }

    @Override
    public boolean isNativeFontSchemeSupported() {
        return true;
    }
    
    private Typeface fontToRoboto(String fontName) {
            if("native:MainThin".equals(fontName)) {
                return Typeface.create("sans-serif-thin", Typeface.NORMAL);
            }
            if("native:MainLight".equals(fontName)) {
                return Typeface.create("sans-serif-light", Typeface.NORMAL);
            }
            if("native:MainRegular".equals(fontName)) {
                return Typeface.create("sans-serif", Typeface.NORMAL);
            }
            
            if("native:MainBold".equals(fontName)) {
                return Typeface.create("sans-serif-condensed", Typeface.BOLD);
            }
            
            if("native:MainBlack".equals(fontName)) {
                return Typeface.create("sans-serif-black", Typeface.BOLD);
            }
            
            if("native:ItalicThin".equals(fontName)) {
                return Typeface.create("sans-serif-thin", Typeface.ITALIC);
            }
            
            if("native:ItalicLight".equals(fontName)) {
                return Typeface.create("sans-serif-thin", Typeface.ITALIC);
            }
            
            if("native:ItalicRegular".equals(fontName)) {
                return Typeface.create("sans-serif", Typeface.ITALIC);
            }
            
            if("native:ItalicBold".equals(fontName)) {
                return Typeface.create("sans-serif-condensed", Typeface.BOLD_ITALIC);
            }
            
            if("native:ItalicBlack".equals(fontName)) {
                return Typeface.create("sans-serif-black", Typeface.BOLD_ITALIC);
            }
            
            throw new IllegalArgumentException("Unsupported native font type: " + fontName);
    }

    @Override
    public Object loadTrueTypeFont(String fontName, String fileName) {
        if(fontName.startsWith("native:")) {
            Typeface t = fontToRoboto(fontName);
            int fontStyle = com.codename1.ui.Font.STYLE_PLAIN;
            if(t.isBold()) {
                fontStyle |= com.codename1.ui.Font.STYLE_BOLD;
            }
            if(t.isItalic()) {
                fontStyle |= com.codename1.ui.Font.STYLE_ITALIC;
            }
            CodenameOneTextPaint newPaint = new CodenameOneTextPaint(t);
            newPaint.setAntiAlias(true);
            newPaint.setSubpixelText(true);
            return new NativeFont(com.codename1.ui.Font.FACE_SYSTEM, fontStyle,
                    com.codename1.ui.Font.SIZE_MEDIUM, newPaint, fileName, 0, 0);
        }
        Typeface t = Typeface.createFromAsset(getContext().getAssets(), fileName);
        if(t == null) {
            throw new RuntimeException("Font not found: " + fileName);
        }
        CodenameOneTextPaint newPaint = new CodenameOneTextPaint(t);
        newPaint.setAntiAlias(true);
        newPaint.setSubpixelText(true);
        return new NativeFont(com.codename1.ui.Font.FACE_SYSTEM,
                com.codename1.ui.Font.STYLE_PLAIN, com.codename1.ui.Font.SIZE_MEDIUM, newPaint, fileName, 0, 0);
    }
    
    static class NativeFont {
        int face;
        int style;
        int size;
        Object font;
        String fileName;
        float height;
        int weight;
        
        public NativeFont(int face, int style, int size, Object font, String fileName, float height, int weight) {
            this(face, style, size, font);
            this.fileName = fileName;
            this.height = height;
            this.weight = weight;
        }
        
        public NativeFont(int face, int style, int size, Object font) {
            this.face = face;
            this.style = style;
            this.size = size;
            this.font = font;
        }
        
        public boolean equals(Object o) {
            if(o == null) {
                return false;
            }
            NativeFont n = ((NativeFont)o);
            if(fileName != null) {
                return n.fileName != null && fileName.equals(n.fileName) && n.height == height && n.weight == weight;
            }
            return n.face == face && n.style == style && n.size == size && font.equals(n.font);
        }
        
        public int hashCode() {
            return face | style | size;
        }
    }

    @Override
    public Object deriveTrueTypeFont(Object font, float size, int weight) {
        NativeFont fnt = (NativeFont)font;
        CodenameOneTextPaint paint = (CodenameOneTextPaint)fnt.font;
        paint.setAntiAlias(true);
        Typeface type = paint.getTypeface();
        int fontstyle = Typeface.NORMAL;
        if ((weight & Font.STYLE_BOLD) != 0 || type.isBold()) {
            fontstyle |= Typeface.BOLD;
        }
        if ((weight & Font.STYLE_ITALIC) != 0 || type.isItalic()) {
            fontstyle |= Typeface.ITALIC;
        }
        type = Typeface.create(type, fontstyle);
        CodenameOneTextPaint newPaint = new CodenameOneTextPaint(type);
        newPaint.setTextSize(size);
        newPaint.setAntiAlias(true);
        NativeFont n = new NativeFont(com.codename1.ui.Font.FACE_SYSTEM, weight, com.codename1.ui.Font.SIZE_MEDIUM, newPaint, fnt.fileName, size, weight);
        return n;
    }

    @Override
    public Object createFont(int face, int style, int size) {
        Typeface typeface = null;
        switch (face) {
            case Font.FACE_MONOSPACE:
                typeface = Typeface.MONOSPACE;
                break;
            default:
                typeface = Typeface.DEFAULT;
                break;
        }

        int fontstyle = Typeface.NORMAL;
        if ((style & Font.STYLE_BOLD) != 0) {
            fontstyle |= Typeface.BOLD;
        }
        if ((style & Font.STYLE_ITALIC) != 0) {
            fontstyle |= Typeface.ITALIC;
        }


        int height = this.defaultFontHeight;
        int diff = height / 3;

        switch (size) {
            case Font.SIZE_SMALL:
                height -= diff;
                break;
            case Font.SIZE_LARGE:
                height += diff;
                break;
        }

        Paint font = new CodenameOneTextPaint(Typeface.create(typeface, fontstyle));
        font.setAntiAlias(true);
        font.setUnderlineText((style & Font.STYLE_UNDERLINED) != 0);
        font.setTextSize(height);
        return new NativeFont(face, style, size, font);

    }

    /**
     * Loads a native font based on a lookup for a font name and attributes.
     * Font lookup values can be separated by commas and thus allow fallback if
     * the primary font isn't supported by the platform.
     *
     * @param lookup string describing the font
     * @return the native font object
     */
    public Object loadNativeFont(String lookup) {
        try {
            lookup = lookup.split(";")[0];
            int typeface = Typeface.NORMAL;
            String familyName = lookup.substring(0, lookup.indexOf("-"));
            String style = lookup.substring(lookup.indexOf("-") + 1, lookup.lastIndexOf("-"));
            String size = lookup.substring(lookup.lastIndexOf("-") + 1, lookup.length());

            if (style.equals("bolditalic")) {
                typeface = Typeface.BOLD_ITALIC;
            } else if (style.equals("italic")) {
                typeface = Typeface.ITALIC;
            } else if (style.equals("bold")) {
                typeface = Typeface.BOLD;
            }
            Paint font = new CodenameOneTextPaint(Typeface.create(familyName, typeface));
            font.setAntiAlias(true);
            font.setTextSize(Integer.parseInt(size));
            return new NativeFont(0, 0, 0, font);
        } catch (Exception err) {
            return null;
        }
    }

    /**
     * Indicates whether loading a font by a string is supported by the platform
     *
     * @return true if the platform supports font lookup
     */
    @Override
    public boolean isLookupFontSupported() {
        return true;
    }

    @Override
    public boolean isAntiAliasedTextSupported() {
        return true;
    }

    @Override
    public void setAntiAliasedText(Object graphics, boolean a) {
        ((AndroidGraphics) graphics).getFont().setAntiAlias(a);
    }

    @Override
    public Object getDefaultFont() {
        CodenameOneTextPaint paint = new CodenameOneTextPaint(this.defaultFont);
        return new NativeFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_MEDIUM, paint);
    }


    private AndroidGraphics nullGraphics;

    private AndroidGraphics getNullGraphics() {
        if (nullGraphics == null) {
            Bitmap bitmap = Bitmap.createBitmap(getDisplayWidth()==0?100:getDisplayWidth(), getDisplayHeight()==0?100:getDisplayHeight(),
                    Bitmap.Config.ARGB_8888);
            nullGraphics = (AndroidGraphics) this.getNativeGraphics(bitmap);
        }
        return nullGraphics;
    }


    @Override
    public Object getNativeGraphics() {
        if(myView != null){
            nullGraphics = null;
            return myView.getGraphics();
        }else{
            return getNullGraphics();
        }
    }

    @Override
    public Object getNativeGraphics(Object image) {
        return new AndroidGraphics(this, new Canvas((Bitmap) image), true);
    }

    @Override
    public void getRGB(Object nativeImage, int[] arr, int offset, int x, int y,
            int width, int height) {
        ((Bitmap) nativeImage).getPixels(arr, offset, width, x, y, width,
                height);
    }
    
    private int sampleSizeOverride = -1;

    @Override
    public Object createImage(String path) throws IOException {
        int IMAGE_MAX_SIZE = getDisplayHeight();
        if (exists(path)) {
            Bitmap b = null;
            try {
                //Decode image size
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                o.inPreferredConfig = Bitmap.Config.ARGB_8888;

                InputStream fis = createFileInputStream(path);
                BitmapFactory.decodeStream(fis, null, o);
                fis.close();

                int scale = 1;
                if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
                    scale = (int) Math.pow(2, (int) Math.round(Math.log(IMAGE_MAX_SIZE / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
                }

                //Decode with inSampleSize
                BitmapFactory.Options o2 = new BitmapFactory.Options();
                o2.inPreferredConfig = Bitmap.Config.ARGB_8888;

                if(sampleSizeOverride != -1) {
                    o2.inSampleSize = sampleSizeOverride;
                } else {
                    String sampleSize = Display.getInstance().getProperty("android.sampleSize", null);
                    if(sampleSize != null) {
                        o2.inSampleSize = Integer.parseInt(sampleSize);
                    } else {
                        o2.inSampleSize = scale;
                    }
                }
                o2.inPurgeable = true;
                o2.inInputShareable = true;
                fis = createFileInputStream(path);
                b = BitmapFactory.decodeStream(fis, null, o2);
                fis.close();
                
                //fix rotation 
                ExifInterface exif = new ExifInterface(path);               
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                int angle = 0;
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        angle = 90;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        angle = 180;
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        angle = 270;
                        break;
                }

                if (sampleSizeOverride < 0 && angle != 0) {
                    Matrix mat = new Matrix();
                    mat.postRotate(angle);
                    Bitmap correctBmp = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), mat, true);
                    b.recycle();
                    b = correctBmp;
                }
            } catch (IOException e) {
            }
            return b;
        } else {
            InputStream in = this.getResourceAsStream(getClass(), path);
            if (in == null) {
                throw new IOException("Resource not found. " + path);
            }
            try {
                return this.createImage(in);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception ignored) {
                        ;
                    }
                }
            }
        }
    }
    
    @Override
    public boolean areMutableImagesFast() {
        if (myView == null) return false;
        return !myView.alwaysRepaintAll();
    }
    
    @Override
    public void repaint(Animation cmp) {
        if(myView != null && myView.alwaysRepaintAll()) {
            if(cmp instanceof Component) {
                Component c = (Component)cmp;
                c.setDirtyRegion(null);
                if(c.getParent() != null) {
                    cmp = c.getComponentForm();
                } else {
                    Form f = getCurrentForm();
                    if(f != null) {
                        cmp = f;
                    }
                }
            } else {
                // make sure the form is repainted for standalone anims e.g. in the case
                // of replace animation
                Form f = getCurrentForm();
                if(f != null) {
                    super.repaint(f);
                }
            }
        }
        super.repaint(cmp);
    }
    
    @Override
    public Object createImage(InputStream i) throws IOException {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeStream(i, null, opts);
    }

    @Override
    public void releaseImage(Object image) {
        Bitmap i = (Bitmap) image;
        i.recycle();
    }

    @Override
    public Object createImage(byte[] bytes, int offset, int len) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try {
            BitmapFactory.Options.class.getField("inPurgeable").set(opts, true);
        } catch (Exception e) {
            // inPurgeable not supported
            // http://www.droidnova.com/2d-sprite-animation-in-android-addendum,505.html
        }
        return BitmapFactory.decodeByteArray(bytes, offset, len, opts);
    }

    @Override
    public Object createImage(int[] rgb, int width, int height) {
        return Bitmap.createBitmap(rgb, width, height, Bitmap.Config.ARGB_8888);
    }

    @Override
    public boolean isAlphaMutableImageSupported() {
        return true;
    }

    @Override
    public Object scale(Object nativeImage, int width, int height) {
        return Bitmap.createScaledBitmap((Bitmap) nativeImage, width, height,
                false);
    }

//    @Override
//    public Object rotate(Object image, int degrees) {
//        Matrix matrix = new Matrix();
//        matrix.postRotate(degrees);
//        return Bitmap.createBitmap((Bitmap) image, 0, 0, ((Bitmap) image).getWidth(), ((Bitmap) image).getHeight(), matrix, true);
//    }
    @Override
    public boolean isRotationDrawingSupported() {
        return false;
    }

    @Override
    protected boolean cacheLinearGradients() {
        return false;
    }

    @Override
    public boolean isNativeInputSupported() {
        return true;
    }

    /**
     * Returns true if the underlying OS supports opening the native navigation
     * application
     * @return true if the underlying OS supports launch of native navigation app
     */
    public boolean isOpenNativeNavigationAppSupported(){
        return true;
    }
    
    /**
     * Opens the native navigation app in the given coordinate.
     * @param latitude 
     * @param longitude 
     */ 
    public void openNativeNavigationApp(double latitude, double longitude){    
        execute("google.navigation:ll=" + latitude+ "," + longitude);
    }


    @Override
    public void openNativeNavigationApp(String location) {    
        execute("google.navigation:q=" + Util.encodeUrl(location));
    }
    
    @Override
    public Object createMutableImage(int width, int height, int fillColor) {
        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        AndroidGraphics graphics = (AndroidGraphics) this.getNativeGraphics(bitmap);
        graphics.fillBitmap(fillColor);
        return bitmap;
    }

    @Override
    public int getImageHeight(Object i) {
        return ((Bitmap) i).getHeight();
    }

    @Override
    public int getImageWidth(Object i) {
        return ((Bitmap) i).getWidth();
    }

    @Override
    public void drawImage(Object graphics, Object img, int x, int y) {
        ((AndroidGraphics) graphics).drawImage(img, x, y);
    }
    
    @Override
    public void tileImage(Object graphics, Object img, int x, int y, int w, int h) {
        ((AndroidGraphics) graphics).tileImage(img, x, y, w, h);
    }

    public boolean isScaledImageDrawingSupported() {
        return true;
    }

    public void drawImage(Object graphics, Object img, int x, int y, int w, int h) {
        ((AndroidGraphics) graphics).drawImage(img, x, y, w, h);
    }

    @Override
    public void drawLine(Object graphics, int x1, int y1, int x2, int y2) {
        ((AndroidGraphics) graphics).drawLine(x1, y1, x2, y2);
    }

    @Override
    public boolean isAntiAliasingSupported() {
        return true;
    }

    @Override
    public void setAntiAliased(Object graphics, boolean a) {
        ((AndroidGraphics) graphics).getPaint().setAntiAlias(a);
    }

    @Override
    public void drawPolygon(Object graphics, int[] xPoints, int[] yPoints, int nPoints) {
        ((AndroidGraphics) graphics).drawPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void fillPolygon(Object graphics, int[] xPoints, int[] yPoints, int nPoints) {
        ((AndroidGraphics) graphics).fillPolygon(xPoints, yPoints, nPoints);
    }

    @Override
    public void drawRGB(Object graphics, int[] rgbData, int offset, int x,
            int y, int w, int h, boolean processAlpha) {
        ((AndroidGraphics) graphics).drawRGB(rgbData, offset, x, y, w, h, processAlpha);
    }

    @Override
    public void drawRect(Object graphics, int x, int y, int width, int height) {
        ((AndroidGraphics) graphics).drawRect(x, y, width, height);
    }

    @Override
    public void drawRoundRect(Object graphics, int x, int y, int width,
            int height, int arcWidth, int arcHeight) {
        ((AndroidGraphics) graphics).drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public void drawString(Object graphics, String str, int x, int y) {
        ((AndroidGraphics) graphics).drawString(str, x, y);
    }

    @Override
    public void drawArc(Object graphics, int x, int y, int width, int height,
            int startAngle, int arcAngle) {
        ((AndroidGraphics) graphics).drawArc(x, y, width, height, startAngle, arcAngle);
    }

    @Override
    public void fillArc(Object graphics, int x, int y, int width, int height,
            int startAngle, int arcAngle) {
        ((AndroidGraphics) graphics).fillArc(x, y, width, height, startAngle, arcAngle);
    }
    
    @Override
    public void fillRect(Object graphics, int x, int y, int width, int height) {
        ((AndroidGraphics) graphics).fillRect(x, y, width, height);
    }

    @Override
    public void fillRect(Object graphics, int x, int y, int w, int h, byte alpha) {
        ((AndroidGraphics) graphics).fillRect(x, y, w, h, alpha);
    }

    @Override
    public void paintComponentBackground(Object graphics, int x, int y, int width, int height, Style s) {
        if((!asyncView) || compatPaintMode ) {
            super.paintComponentBackground(graphics, x, y, width, height, s);
            return;
        }
        ((AndroidGraphics) graphics).paintComponentBackground(x, y, width, height, s);
    }

    @Override
    public void fillLinearGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height, boolean horizontal) {
        if(!asyncView) {
            super.fillLinearGradient(graphics, startColor, endColor, x, y, width, height, horizontal);
            return;
        }
        ((AndroidGraphics)graphics).fillLinearGradient(startColor, endColor, x, y, width, height, horizontal);
    }

    @Override
    public void fillRectRadialGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height, float relativeX, float relativeY, float relativeSize) {
        if(!asyncView) {
            super.fillRectRadialGradient(graphics, startColor, endColor, x, y, width, height, relativeX, relativeY, relativeSize);
            return;
        }
        ((AndroidGraphics)graphics).fillRectRadialGradient(startColor, endColor, x, y, width, height, relativeX, relativeY, relativeSize);
    }

    @Override
    public void fillRadialGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height) {
        ((AndroidGraphics)graphics).fillRadialGradient(startColor, endColor, x, y, width, height);
    }
        
    @Override
    public void fillRadialGradient(Object graphics, int startColor, int endColor, int x, int y, int width, int height, int startAngle, int arcAngle) {
        ((AndroidGraphics)graphics).fillRadialGradient(startColor, endColor, x, y, width, height, startAngle, arcAngle);
    }
    
    @Override
    public void drawLabelComponent(Object nativeGraphics, int cmpX, int cmpY, int cmpHeight, int cmpWidth, Style style, String text, Object icon, Object stateIcon, int preserveSpaceForState, int gap, boolean rtl, boolean isOppositeSide, int textPosition, int stringWidth, boolean isTickerRunning, int tickerShiftText, boolean endsWith3Points, int valign) {
        if(AndroidAsyncView.legacyPaintLogic) {
            super.drawLabelComponent(nativeGraphics, cmpX, cmpY, cmpHeight, cmpWidth, style, text, icon, stateIcon, preserveSpaceForState, gap, rtl, isOppositeSide, textPosition, stringWidth, isTickerRunning, tickerShiftText, endsWith3Points, valign);
            return;
        }
        ((AndroidGraphics)nativeGraphics).drawLabelComponent(cmpX, cmpY, cmpHeight, cmpWidth, style, text, 
                (Bitmap)icon, (Bitmap)stateIcon, preserveSpaceForState, gap, rtl, isOppositeSide, textPosition, stringWidth, 
                isTickerRunning, tickerShiftText, endsWith3Points, valign);
    }

   
    @Override
    public void fillRoundRect(Object graphics, int x, int y, int width,
            int height, int arcWidth, int arcHeight) {
        ((AndroidGraphics) graphics).fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    @Override
    public int getAlpha(Object graphics) {
        return ((AndroidGraphics) graphics).getAlpha();
    }

    @Override
    public void setAlpha(Object graphics, int alpha) {
        ((AndroidGraphics) graphics).setAlpha(alpha);
    }

    @Override
    public boolean isAlphaGlobal() {
        return true;
    }

    @Override
    public void setColor(Object graphics, int RGB) {
        ((AndroidGraphics) graphics).setColor((getColor(graphics) & 0xff000000) | RGB);
    }

    @Override
    public int getBackKeyCode() {
        return DROID_IMPL_KEY_BACK;
    }

    @Override
    public int getBackspaceKeyCode() {
        return DROID_IMPL_KEY_BACKSPACE;
    }

    @Override
    public int getClearKeyCode() {
        return DROID_IMPL_KEY_CLEAR;
    }

    @Override
    public int getClipHeight(Object graphics) {
        return ((AndroidGraphics) graphics).getClipHeight();
    }

    @Override
    public int getClipWidth(Object graphics) {
        return ((AndroidGraphics) graphics).getClipWidth();
    }

    @Override
    public int getClipX(Object graphics) {
        return ((AndroidGraphics) graphics).getClipX();
    }

    @Override
    public int getClipY(Object graphics) {
        return ((AndroidGraphics) graphics).getClipY();
    }

    @Override
    public void setClip(Object graphics, int x, int y, int width, int height) {
        ((AndroidGraphics) graphics).setClip(x, y, width, height);
    }
    
    @Override
    public boolean isShapeClipSupported(Object graphics){
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB;
    }
    
    @Override
    public void setClip(Object graphics, Shape shape) {
        //Path p = cn1ShapeToAndroidPath(shape);
        ((AndroidGraphics) graphics).setClip(shape);
    }
    

    @Override
    public void clipRect(Object graphics, int x, int y, int width, int height) {
        ((AndroidGraphics) graphics).clipRect(x, y, width, height);
    }

    @Override
    public int getColor(Object graphics) {
        return ((AndroidGraphics) graphics).getColor();
    }

    @Override
    public int getDisplayHeight() {
        if (this.myView != null) {
            int h = this.myView.getViewHeight();
            displayHeight = h;
            return h;
        }
        return displayHeight;
    }

    @Override
    public int getDisplayWidth() {
        if (this.myView != null) {
            int w = this.myView.getViewWidth();
            displayWidth = w;
            return w;
        }
        return displayWidth;
    }

    @Override
    public int getActualDisplayHeight() {
        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        return dm.heightPixels;
    }

    @Override
    public int getGameAction(int keyCode) {
        switch (keyCode) {
            case DROID_IMPL_KEY_DOWN:
                return Display.GAME_DOWN;
            case DROID_IMPL_KEY_UP:
                return Display.GAME_UP;
            case DROID_IMPL_KEY_LEFT:
                return Display.GAME_LEFT;
            case DROID_IMPL_KEY_RIGHT:
                return Display.GAME_RIGHT;
            case DROID_IMPL_KEY_FIRE:
                return Display.GAME_FIRE;
            default:
                return 0;
        }
    }

    @Override
    public int getKeyCode(int gameAction) {
        switch (gameAction) {
            case Display.GAME_DOWN:
                return DROID_IMPL_KEY_DOWN;
            case Display.GAME_UP:
                return DROID_IMPL_KEY_UP;
            case Display.GAME_LEFT:
                return DROID_IMPL_KEY_LEFT;
            case Display.GAME_RIGHT:
                return DROID_IMPL_KEY_RIGHT;
            case Display.GAME_FIRE:
                return DROID_IMPL_KEY_FIRE;
            default:
                return 0;
        }
    }

    @Override
    public int[] getSoftkeyCode(int index) {
        if (index == 0) {
            return leftSK;
        }
        return null;
    }

    @Override
    public int getSoftkeyCount() {
        /**
         * one menu button only. we may have to stuff some code here as soon as
         * there are devices that no longer have only a single menu button.
         */
        return 1;
    }

    @Override
    public void vibrate(int duration) {
        if (!this.vibrateInitialized) {
            try {
                v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            } catch (Throwable e) {
                Log.e("Codename One", "problem with virbrator(0)", e);
            } finally {
                this.vibrateInitialized = true;
            }
        }
        if (v != null) {
            try {
                v.vibrate(duration);
            } catch (Throwable e) {
                Log.e("Codename One", "problem with virbrator(1)", e);
            }
        }
    }

    @Override
    public boolean isTouchDevice() {
        return getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN);
    }

    @Override
    public boolean hasPendingPaints() {
        //if the view is not visible make sure the edt won't wait.
        if (myView != null && myView.getAndroidView().getVisibility() != View.VISIBLE) {
            return true;
        } else {
            return super.hasPendingPaints();
        }
    }

    public void revalidate() {
        if (myView != null) {
            myView.getAndroidView().setVisibility(View.VISIBLE);
            getCurrentForm().revalidate();
            flushGraphics();
        }

    }

    @Override
    public int getKeyboardType() {
        if (Display.getInstance().getDefaultVirtualKeyboard().isVirtualKeyboardShowing()) {
            return Display.KEYBOARD_TYPE_VIRTUAL;
        }
        /**
         * can we detect this? but even if we could i think it is best to have
         * this fixed to qwerty. we pass unicode values to Codename One in any
         * case. check AndroidView.onKeyUpDown() method. and read comment below.
         */
        return Display.KEYBOARD_TYPE_QWERTY;
        /**
         * some info from the MIDP docs about keycodes:
         *
         * "Applications receive keystroke events in which the individual keys
         * are named within a space of key codes. Every key for which events are
         * reported to MIDP applications is assigned a key code. The key code
         * values are unique for each hardware key unless two keys are obvious
         * synonyms for each other. MIDP defines the following key codes:
         * KEY_NUM0, KEY_NUM1, KEY_NUM2, KEY_NUM3, KEY_NUM4, KEY_NUM5, KEY_NUM6,
         * KEY_NUM7, KEY_NUM8, KEY_NUM9, KEY_STAR, and KEY_POUND. (These key
         * codes correspond to keys on a ITU-T standard telephone keypad.) Other
         * keys may be present on the keyboard, and they will generally have key
         * codes distinct from those list above. In order to guarantee
         * portability, applications should use only the standard key codes.
         *
         * The standard key codes values are equal to the Unicode encoding for
         * the character that represents the key. If the device includes any
         * other keys that have an obvious correspondence to a Unicode
         * character, their key code values should equal the Unicode encoding
         * for that character. For keys that have no corresponding Unicode
         * character, the implementation must use negative values. Zero is
         * defined to be an invalid key code."
         *
         * Because the MIDP implementation is our reference and that
         * implementation does not interpret the given keycodes we behave alike
         * and pass on the unicode values.
         */
    }

    /**
     * Exits the application...
     */
    public void exitApplication() {
        android.os.Process.killProcess(android.os.Process.myPid()); 
    }

    @Override
    public void notifyCommandBehavior(int commandBehavior) {
        if (commandBehavior == Display.COMMAND_BEHAVIOR_NATIVE) {
            if (getActivity() instanceof CodenameOneActivity) {
                ((CodenameOneActivity) getActivity()).enableNativeMenu(true);
            }
        }
    }
    
    private static class NotifyActionBar implements Runnable {
        private Activity activity;
        private boolean show;
        
        public NotifyActionBar(Activity activity, int commandBehavior) {
            this.activity = activity;
            show = commandBehavior == Display.COMMAND_BEHAVIOR_NATIVE;
        }
        
        public NotifyActionBar(Activity activity, boolean show) {
            this.activity = activity;
            this.show = show;
        }
        
        @Override
        public void run() {
            activity.invalidateOptionsMenu();
            if (show) {
                activity.getActionBar().show();
            } else {
                activity.getActionBar().hide();
            }
        }
    }

    @Override
    public String getAppArg() {
        if (super.getAppArg() != null) {
            // This just maintains backward compatibility in case people are manually
            // setting the AppArg in their properties.  It reproduces the general
            // behaviour the existed when AppArg was just another Display property.
            return super.getAppArg();
        }
        if (getActivity() == null) {
            return null;
        }
        
        android.content.Intent intent = getActivity().getIntent();
        if (intent != null) {
            Uri u = intent.getData();
            String scheme = intent.getScheme();
            if (u == null && intent.getExtras() != null) {
                if (intent.getExtras().keySet().contains("android.intent.extra.STREAM")) {
                    try {
                        u = (Uri)intent.getParcelableExtra("android.intent.extra.STREAM");
                        scheme = u.getScheme();
                        System.out.println("u="+u);
                    } catch (Exception ex) {
                        Log.d("Codename One", "Failed to load parcelable extra from intent: "+ex.getMessage());
                    }
                }

            }
            if (u != null) {
                //String scheme = intent.getScheme();
                intent.setData(null);
                if ("content".equals(scheme)) {
                    try {
                        InputStream attachment = getActivity().getContentResolver().openInputStream(u);
                        if (attachment != null) {
                            String name = getContentName(getActivity().getContentResolver(), u);
                            if (name != null) {
                                String filePath = getAppHomePath()
                                        + getFileSystemSeparator() + name;
                                File f = new File(filePath);
                                OutputStream tmp = createFileOuputStream(f);
                                byte[] buffer = new byte[1024];
                                while (attachment.read(buffer) > 0) {
                                    tmp.write(buffer);
                                }
                                tmp.close();
                                attachment.close();
                                setAppArg(filePath);
                                return filePath;
                            }
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        return null;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                } else {
                    
                    /*
                    // Why do we need this special case?  u.toString()
                    // will include the full URL including query string.
                    // This special case causes urls like myscheme://part1/part2
                    // to only return "/part2" which is obviously problematic and
                    // is inconsistent with iOS.  Is this special case necessary
                    // in some versions of Android?
                    String encodedPath = u.getEncodedPath();
                    if (encodedPath != null && encodedPath.length() > 0) {
                        String query = u.getQuery();
                        if(query != null && query.length() > 0){
                            encodedPath += "?" + query;
                        }
                        setAppArg(encodedPath);
                        return encodedPath;
                    }
                    */
                    setAppArg(u.toString());
                    return u.toString();
                }
            }
        }
        return null;
    }
    
    
    

    /**
     * @inheritDoc
     */
    public String getProperty(String key, String defaultValue) {
        if(key.equalsIgnoreCase("cn1_push_prefix")) {
            /*if(!checkForPermission(Manifest.permission.READ_PHONE_STATE, "This is required to get notifications")){
                return "";
            }*/
            boolean has = hasAndroidMarket();
            if(has) {
                return "gcm";
            }
            return defaultValue;
        }
        if ("OS".equals(key)) {
            return "Android";
        }
        if ("androidId".equals(key)) {
            return Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        
        if ("cellId".equals(key)) {
            try {
                if(!checkForPermission(Manifest.permission.READ_PHONE_STATE, "This is required to get the cellId")){
                    return defaultValue;
                }
                String serviceName = Context.TELEPHONY_SERVICE;
                TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(serviceName);
                int cellId = ((GsmCellLocation) telephonyManager.getCellLocation()).getCid();
                return "" + cellId;
            } catch (Throwable t) {
                return defaultValue;
            }
        }
        if ("AppName".equals(key)) {
            
            final PackageManager pm = getContext().getPackageManager();
            ApplicationInfo ai;
            try {
                ai = pm.getApplicationInfo(getContext().getPackageName(), 0);
            } catch (NameNotFoundException e) {
                ai = null;
            }
            String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : null);
            if(applicationName == null){
                return defaultValue;
            }
            return applicationName;
        }
        if ("AppVersion".equals(key)) {
            try {
                PackageInfo i = getContext().getPackageManager().getPackageInfo(getContext().getApplicationInfo().packageName, 0);
                return i.versionName;
            } catch (NameNotFoundException ex) {
                ex.printStackTrace();
            }
            return defaultValue;
        }
        if ("Platform".equals(key)) {
            String p = System.getProperty("platform");
            if(p == null) {
                return defaultValue;
            }
            return p;
        }
        if ("User-Agent".equals(key)) {
            String ua = getUserAgent();
            if(ua == null) {
                return defaultValue;
            }
            return ua;
        }
        if("OSVer".equals(key)) {
            return "" + android.os.Build.VERSION.RELEASE;
        }
        if("DeviceName".equals(key)) {
            return "" + android.os.Build.MODEL;
        }
        try {
            if ("IMEI".equals(key) || "UDID".equals(key)) {
                if(!checkForPermission(Manifest.permission.READ_PHONE_STATE, "This is required to get the device ID")){
                    return "";
                }
                TelephonyManager tm = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
                return tm.getDeviceId();
            }
            if ("MSISDN".equals(key)) {
                if(!checkForPermission(Manifest.permission.READ_PHONE_STATE, "This is required to get the device ID")){
                    return "";
                }
                TelephonyManager tm = (TelephonyManager) getContext().getSystemService(Context.TELEPHONY_SERVICE);
                return tm.getLine1Number();
            }
        } catch(Throwable t) {
            // will be caused by no permissions.
            return defaultValue;
        }

        if (getActivity() != null) {
            android.content.Intent intent = getActivity().getIntent();
            if(intent != null){
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    String value = extras.getString(key);
                    if(value != null) {
                        return value;
                    }
                }
            }
        }
        
        //these keys/values are from the Application Resources (strings values)
        try {
            int id = getContext().getResources().getIdentifier(key, "string", getContext().getApplicationInfo().packageName);
            if (id != 0) {
                String val = getContext().getResources().getString(id);
                return val;
            }
        } catch (Exception e) {
        }
        return System.getProperty(key, super.getProperty(key, defaultValue));
    }

    private String getContentName(ContentResolver resolver, Uri uri) {
        Cursor cursor = resolver.query(uri, null, null, null, null);
        cursor.moveToFirst();
        int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
        if (nameIndex >= 0) {
            String name = cursor.getString(nameIndex);
            cursor.close();
            return name;
        }
        return null;
    }
    
    private String getUserAgent() {
        try {
            String userAgent = System.getProperty("http.agent");            
            if(userAgent != null){
                return userAgent;
            }
        } catch (Exception e) {
        }
        if (getActivity() == null) {
            return "Android-CN1";
        }
        try {
            Constructor<WebSettings> constructor = WebSettings.class.getDeclaredConstructor(Context.class, WebView.class);
            constructor.setAccessible(true);
            try {
                WebSettings settings = constructor.newInstance(getActivity(), null);
                return settings.getUserAgentString();
            } finally {
                constructor.setAccessible(false);
            }
        } catch (Exception e) {
            final StringBuffer ua = new StringBuffer();
            if (Thread.currentThread().getName().equalsIgnoreCase("main")) {
                WebView m_webview = new WebView(getActivity());
                ua.append(m_webview.getSettings().getUserAgentString());
                m_webview.destroy();
            } else {
                final boolean[] flag = new boolean[1];
                Thread thread = new Thread() {
                    public void run() {
                        Looper.prepare();
                        WebView m_webview = new WebView(getActivity());
                        ua.append(m_webview.getSettings().getUserAgentString());
                        m_webview.destroy();
                        Looper.loop();
                        flag[0] = true;
                        synchronized (flag) {
                            flag.notify();
                        }
                    }
                };
                thread.setUncaughtExceptionHandler(AndroidImplementation.exceptionHandler);
                thread.start();
                while (!flag[0]) {
                    synchronized (flag) {
                        try {
                            flag.wait(100);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }
            return ua.toString();
        }
    }

    private String getMimeType(String url){
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getMimeTypeFromExtension(extension);
        }
        return type;
    }
    
    private Intent createIntentForURL(String url) {
        Intent intent;
        Uri uri;
        try {
            if (url.startsWith("intent")) {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            } else {
                if(!checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, "This is required to open the file")){
                    return null;
                }
                url = fixAttachmentPath(url);
                intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                if (url.startsWith("/")) {
                    uri = Uri.fromFile(new File(url));
                }else{
                    uri = Uri.parse(url);
                }
                String mimeType = getMimeType(url);
                if(mimeType != null){
                    intent.setDataAndType(uri, mimeType);            
                }else{
                    intent.setData(uri);
                }
            }

            return intent;
        } catch(Exception err) {
            com.codename1.io.Log.e(err);
            return null;
        }
    }

    @Override
    public Boolean canExecute(String url) {
        try {
            Intent it = createIntentForURL(url);
            if(it == null) {
                return false;
            }
            final PackageManager mgr = getContext().getPackageManager();
            List<ResolveInfo> list = mgr.queryIntentActivities(it, PackageManager.MATCH_DEFAULT_ONLY);
            return list.size() > 0;            
        } catch(Exception err) {
            com.codename1.io.Log.e(err);
            return false;
        }
    }

    
    public void execute(String url, ActionListener response) {
        if (response != null) {
            callback = new EventDispatcher();
            callback.addListener(response);
        }

        try {
            Intent intent = createIntentForURL(url);
            if(intent == null) {
                return;
            }
            if(response != null && getActivity() != null){
                getActivity().startActivityForResult(intent, IntentResultListener.URI_SCHEME);
            }else {
                getContext().startActivity(intent);
            }
            return;
        } catch (Exception ex) {           
            com.codename1.io.Log.e(ex);
        }
        
        try {
            if(editInProgress()) {
                stopEditing(true);
            }
            getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * @inheritDoc
     */
    @Override
    public void execute(String url) {
        execute(url, null);
    }

    /**
     * @inheritDoc
     */
    public void playBuiltinSound(String soundIdentifier) {
        if (getActivity() != null && Display.SOUND_TYPE_BUTTON_PRESS == soundIdentifier) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    if (myView != null) {
                        myView.getAndroidView().playSoundEffect(AudioManager.FX_KEY_CLICK);
                    }
                }
            });
        }
    }

    /**
     * @inheritDoc
     */
    protected void playNativeBuiltinSound(Object data) {
    }

    /**
     * @inheritDoc
     */
    public boolean isBuiltinSoundAvailable(String soundIdentifier) {
        return true;
    }

    /**
     * @inheritDoc
     */
    @Override
    public boolean isNativeVideoPlayerControlsIncluded() {
        return true;
    }
    
   
    @Override
    public Media createBackgroundMedia(String uri) throws IOException {

        Intent serviceIntent = new Intent(getContext(), AudioService.class);
        serviceIntent.putExtra("mediaLink", uri);
        
        final ServiceConnection mConnection = new ServiceConnection() {

            public void onServiceDisconnected(ComponentName name) {
                background = null;
            }

            public void onServiceConnected(ComponentName name, IBinder service) {
                AudioService.LocalBinder mLocalBinder = (AudioService.LocalBinder) service;
                background = mLocalBinder.getService();
            }
        };

        getContext().bindService(serviceIntent, mConnection, getContext().BIND_AUTO_CREATE);
        getContext().startService(serviceIntent);
        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                while (background == null) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        });

        Media retVal = new MediaProxy(background) {

            @Override
            public void cleanup() {
                super.cleanup();
                getContext().unbindService(mConnection);
            }
        };
        return retVal;
    }

    
    /**
     * @inheritDoc
     */
    @Override
    public Media createMedia(final String uri, boolean isVideo, final Runnable onCompletion) throws IOException {
        if (getActivity() == null) {
            return null;
        }
        if(!checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, "This is required to play media")){
            return null;
        }
        if (uri.startsWith("file://")) {
            return createMedia(uri.substring(7), isVideo, onCompletion);
        }
        File file = null;
        if (uri.indexOf(':') < 0) {
            // use a file object to play to try and workaround this issue:
            // http://code.google.com/p/android/issues/detail?id=4124
            file = new File(uri);
        }

        Media retVal;

        if (isVideo) {
            final AndroidImplementation.Video[] video = new AndroidImplementation.Video[1];
            final boolean[] flag = new boolean[1];
            final File f = file;
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    VideoView v = new VideoView(getActivity());
                    v.setZOrderMediaOverlay(true);
                    if (f != null) {
                        v.setVideoURI(Uri.fromFile(f));
                    } else {
                        v.setVideoURI(Uri.parse(uri));
                    }
                    video[0] = new AndroidImplementation.Video(v, getActivity(), onCompletion);
                    flag[0] = true;
                    synchronized (flag) {
                        flag.notify();
                    }
                }
            });
            while (!flag[0]) {
                synchronized (flag) {
                    try {
                        flag.wait(100);
                    } catch (InterruptedException ex) {
                    }
                }
            }
            return video[0];
        } else {
            MediaPlayer player;
            if (file != null) {
                FileInputStream is = new FileInputStream(file);
                player = new MediaPlayer();
                player.setDataSource(is.getFD());
                player.prepare();
            } else {
                player = MediaPlayer.create(getActivity(), Uri.parse(uri));
            }
            retVal = new Audio(getActivity(), player, null, onCompletion);
        }
        return retVal;
    }

    /**
     * @inheritDoc
     */
    @Override
    public Media createMedia(InputStream stream, String mimeType, final Runnable onCompletion) throws IOException {
        if (getActivity() == null) {
            return null;
        }
        if(!checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, "This is required to play media")){
            return null;
        }
        boolean isVideo = mimeType.contains("video");

        if (!isVideo && stream instanceof FileInputStream) {
            MediaPlayer player = new MediaPlayer();
            player.setDataSource(((FileInputStream) stream).getFD());
            player.prepare();
            return new Audio(getActivity(), player, stream, onCompletion);
        }

        final File temp = File.createTempFile("mtmp", "dat");
        temp.deleteOnExit();
        OutputStream out = createFileOuputStream(temp);
       
        byte buf[] = new byte[256];
        int len = 0;
        while ((len = stream.read(buf, 0, buf.length)) > -1) {
            out.write(buf, 0, len);
        }
        out.close();
        stream.close();
        
        final Runnable finish = new Runnable() {

            @Override
            public void run() {
                if(onCompletion != null){
                    Display.getInstance().callSerially(onCompletion);
                    
                    // makes sure the file is only deleted after the onCompletion was invoked
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            temp.delete();                
                        }
                    });
                    return;
                }
                temp.delete();                
            }
        };

        if (isVideo) {
            final AndroidImplementation.Video[] retVal = new AndroidImplementation.Video[1];
            final boolean[] flag = new boolean[1];

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    VideoView v = new VideoView(getActivity());
                    v.setZOrderMediaOverlay(true);
                    v.setVideoURI(Uri.fromFile(temp));
                    retVal[0] = new AndroidImplementation.Video(v, getActivity(), finish);
                    flag[0] = true;
                    synchronized (flag) {
                        flag.notify();
                    }
                }
            });
            while (!flag[0]) {
                synchronized (flag) {
                    try {
                        flag.wait(100);
                    } catch (InterruptedException ex) {
                    }
                }
            }

            return retVal[0];
        } else {
            return createMedia(createFileInputStream(temp), mimeType, finish);
        }

    }

    @Override
    public Media createMediaRecorder(final String path, final String mimeType) throws IOException {
        if (getActivity() == null) {
            return null;
        }
        if(!checkForPermission(Manifest.permission.RECORD_AUDIO, "This is required to record audio")){
            return null;
        }
        final AndroidRecorder[] record = new AndroidRecorder[1];
        final IOException[] error = new IOException[1];

        final Object lock = new Object();
        synchronized (lock) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (lock) {
                        MediaRecorder recorder = new MediaRecorder();
                        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                        if(mimeType.contains("amr")){
                            recorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
                            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                        }else{
                            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);            
                            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                        }
                        recorder.setOutputFile(path);
                        try {
                            recorder.prepare();
                            record[0] = new AndroidRecorder(recorder);
                        } catch (IllegalStateException ex) {
                            Logger.getLogger(AndroidImplementation.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            error[0] = ex;
                        } finally {
                            lock.notify();
                        }


                    }
                }
            });

            try {
                lock.wait();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }

            if (error[0] != null) {
                throw error[0];
            }

            return record[0];
        }
    }
    
    public String [] getAvailableRecordingMimeTypes(){
        return new String[]{"audio/amr", "audio/aac"};
    }
    

    /**
     * @inheritDoc
     */
    public Object createSoftWeakRef(Object o) {
        return new SoftReference(o);
    }

    /**
     * @inheritDoc
     */
    public Object extractHardRef(Object o) {
        SoftReference w = (SoftReference) o;
        if (w != null) {
            return w.get();
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public PeerComponent createNativePeer(Object nativeComponent) {
        if (!(nativeComponent instanceof View)) {
            throw new IllegalArgumentException(nativeComponent.getClass().getName());
        }
        return new AndroidImplementation.AndroidPeer((View) nativeComponent);
    }

    private void blockNativeFocusAll(boolean block) {
        synchronized (this.nativePeers) {
            final int size = this.nativePeers.size();
            for (int i = 0; i < size; i++) {
                AndroidImplementation.AndroidPeer next = (AndroidImplementation.AndroidPeer) this.nativePeers.get(i);
                next.blockNativeFocus(block);
            }
        }
    }

    public void onFocusChange(View view, boolean bln) {

        if (bln) {
            /**
             * whenever the base view receives focus we automatically block
             * possible native subviews from gaining focus.
             */
            blockNativeFocusAll(true);
            if (this.lastDirectionalKeyEventReceivedByWrapper != 0) {
                /**
                 * because we also consume any key event in the OnKeyListener of
                 * the native wrappers, we have to simulate key events to make
                 * Codename One move the focus to the next component.
                 */
                if (myView == null) {
                    return;
                }
                if (!myView.getAndroidView().isInTouchMode()) {
                    switch (lastDirectionalKeyEventReceivedByWrapper) {
                        case AndroidImplementation.DROID_IMPL_KEY_LEFT:
                        case AndroidImplementation.DROID_IMPL_KEY_RIGHT:
                        case AndroidImplementation.DROID_IMPL_KEY_UP:
                        case AndroidImplementation.DROID_IMPL_KEY_DOWN:
                            Display.getInstance().keyPressed(lastDirectionalKeyEventReceivedByWrapper);
                            Display.getInstance().keyReleased(lastDirectionalKeyEventReceivedByWrapper);
                            break;
                        default:
                            Log.d("Codename One", "unexpected keycode: " + lastDirectionalKeyEventReceivedByWrapper);
                            break;
                    }
                } else {
                    Log.d("Codename One", "base view gained focus but no key event to process.");
                }
                lastDirectionalKeyEventReceivedByWrapper = 0;
            }
        }

    }

    @Override
    public void edtIdle(boolean enter) {
        super.edtIdle(enter);
        if(enter) {
            // check if we have peers waiting for resize...
            if(myView instanceof AndroidAsyncView) {
                ((AndroidAsyncView)myView).resizeViews();
            }
        }
    }

    /**
     * wrapper component that capsules a native view object in a Codename One
     * component. this involves A LOT of back and forth between the Codename One
     * EDT and the Android UI thread.
     *
     *
     * To use it you would:
     *
     * 1) create your native Android view(s). Make sure to work on the Android
     * UI thread when constructing and modifying them. 2) create a Codename One
     * peer component by calling:
     *
     * com.codename1.ui.PeerComponent.create(myAndroidView);
     *
     * 3) currently the view's size is not automatically calculated from the
     * native view. so you should set the preferred size of the Codename One
     * component manually.
     *
     *
     */
    class AndroidPeer extends PeerComponent {

        private View v;
        private AndroidImplementation.AndroidRelativeLayout layoutWrapper = null;
        private int currentVisible = View.INVISIBLE;
        private boolean lightweightMode;

        public AndroidPeer(View vv) {
            super(vv);
            this.v = vv;
            if(!superPeerMode) {
                v.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                        MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            }
        }

        @Override
        protected Image generatePeerImage() {
            try {
                Bitmap bmp = AndroidNativeUtil.renderViewOnBitmap(v, getWidth(), getHeight());
                if(bmp == null) {
                    return Image.createImage(5, 5);
                }
                Image image = new AndroidImplementation.NativeImage(bmp);
                return image;
            } catch(Throwable t) {
                t.printStackTrace();
                return Image.createImage(5, 5);
            }
        }
        
        protected boolean shouldRenderPeerImage() {
            return !superPeerMode && (lightweightMode || !isInitialized());
        }

        protected void setLightweightMode(boolean l) {
            if(superPeerMode) {
                return;
            }
            doSetVisibility(!l);
            if (lightweightMode == l) {
                return;
            }
            lightweightMode = l;
        }

        @Override
        public void setVisible(boolean visible) {
            super.setVisible(visible);
            this.doSetVisibility(visible);
        }

        void doSetVisibility(final boolean visible) {
            if (getActivity() == null) {
                return;
            }
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    currentVisible = visible ? View.VISIBLE : View.INVISIBLE;
                    v.setVisibility(currentVisible);
                    if (visible) {
                        v.bringToFront();
                    }
                }
            });
            if(visible){
                layoutPeer();
            }
        }

        private void doSetVisibilityInternal(final boolean visible) {
            if (getActivity() == null) {
                return;
            }
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    currentVisible = visible ? View.VISIBLE : View.INVISIBLE;
                    v.setVisibility(currentVisible);
                    if (visible) {
                        v.bringToFront();
                    }
                }
            });
        }
        
        protected void deinitialize() {
            if(!superPeerMode) {
                Image i = generatePeerImage();
                setPeerImage(i);
                super.deinitialize();
                synchronized (nativePeers) {
                    nativePeers.remove(this);
                }
                deinit();
            }else{
                if (peerImage == null) {
                    peerImage = generatePeerImage();
                }
                if(myView instanceof AndroidAsyncView){
                    ((AndroidAsyncView)myView).removePeerView(v);
                }
            }
        }

        public void deinit(){
            if (getActivity() == null) {
                return;
            }
            if (peerImage == null) {
                peerImage = generatePeerImage();
            }
            final boolean [] removed = new boolean[1];
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    if (layoutWrapper != null && AndroidImplementation.this.relativeLayout != null) {
                        AndroidImplementation.this.relativeLayout.removeView(layoutWrapper);
                        AndroidImplementation.this.relativeLayout.requestLayout();
                        layoutWrapper = null;
                    }
                    removed[0] = true;
                }
            });
            Display.getInstance().invokeAndBlock(new Runnable() {
                public void run() {
                    while (!removed[0]) {
                        try {
                            Thread.sleep(5);
                        } catch(InterruptedException er) {}
                    }
                }
            });
        }
        
        protected void initComponent() {
            super.initComponent();
            if(!superPeerMode) {
                synchronized (nativePeers) {
                    nativePeers.add(this);
                }
                init();
                setPeerImage(null);
            }
        }

        public void init(){
            if(superPeerMode || getActivity() == null) {
                return;
            }
            runOnUiThreadAndBlock(new Runnable() {
                public void run() {
                    if (layoutWrapper == null) {
                        /**
                         * wrap the native item in a layout that we can move
                         * around on the surface view as we like.
                         */
                        layoutWrapper = new AndroidImplementation.AndroidRelativeLayout(activity, AndroidImplementation.AndroidPeer.this, v);
                        layoutWrapper.setBackgroundDrawable(null);
                        v.setVisibility(currentVisible);
                        v.setFocusable(AndroidImplementation.AndroidPeer.this.isFocusable());
                        v.setFocusableInTouchMode(true);
                        ArrayList<View> viewList = new ArrayList<View>();
                        viewList.add(layoutWrapper);
                        v.addFocusables(viewList, View.FOCUS_DOWN);
                        v.addFocusables(viewList, View.FOCUS_UP);
                        v.addFocusables(viewList, View.FOCUS_LEFT);
                        v.addFocusables(viewList, View.FOCUS_RIGHT);
                        if (v.isFocusable() || v.isFocusableInTouchMode()) {
                            if (AndroidImplementation.AndroidPeer.super.hasFocus()) {
                                AndroidImplementation.this.blockNativeFocusAll(true);
                                blockNativeFocus(false);
                                v.requestFocus();

                            } else {
                                blockNativeFocus(true);
                            }
                            layoutWrapper.setOnKeyListener(new View.OnKeyListener() {
                                public boolean onKey(View view, int i, KeyEvent ke) {
                                    lastDirectionalKeyEventReceivedByWrapper = CodenameOneView.internalKeyCodeTranslate(ke.getKeyCode());

                                    // move focus back to base view.
                                    if (AndroidImplementation.this.myView == null) return false;
                                    AndroidImplementation.this.myView.getAndroidView().requestFocus();

                                    /**
                                     * if the wrapper has focus, then only because
                                     * the wrapped native component just lost focus.
                                     * we consume whatever key events we receive,
                                     * just to make sure no half press/release
                                     * sequence reaches the base view (and therefore
                                     * Codename One).
                                     */
                                    return true;
                                }
                            });
                            layoutWrapper.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                                public void onFocusChange(View view, boolean bln) {
                                    Log.d("Codename One", "on focus change. " + view.toString() + " focus:" + bln + " touchmode: " + v.isInTouchMode());
                                }
                            });
                            layoutWrapper.setOnTouchListener(new View.OnTouchListener() {
                                public boolean onTouch(View v, MotionEvent me) {
                                    if (myView == null) return false;
                                    return myView.getAndroidView().onTouchEvent(me);
                                }
                            });
                        }
                        if(AndroidImplementation.this.relativeLayout != null){
                            // not sure why this happens but we got an exception where add view was called with
                            // a layout that was already added...
                            if(layoutWrapper.getParent() != null) {
                                ((ViewGroup)layoutWrapper.getParent()).removeView(layoutWrapper);
                            }
                            AndroidImplementation.this.relativeLayout.addView(layoutWrapper);
                        }
                    }
                }
            });
        }
        private Image peerImage;
        public void paint(final Graphics g) {
            if(superPeerMode) {
                Object nativeGraphics = com.codename1.ui.Accessor.getNativeGraphics(g);

                Object o = v.getLayoutParams();
                AndroidAsyncView.LayoutParams lp;
                if(o instanceof AndroidAsyncView.LayoutParams) {
                    lp = (AndroidAsyncView.LayoutParams) o;
                    if (lp == null) {
                        lp = new AndroidAsyncView.LayoutParams(
                                getX() + g.getTranslateX(),
                                getY() + g.getTranslateY(),
                                getWidth(),
                                getHeight(), AndroidPeer.this);
                        final AndroidAsyncView.LayoutParams finalLp = lp;
                        v.post(new Runnable() {
                            @Override
                            public void run() {
                                v.setLayoutParams(finalLp);
                            }
                        });
                        lp.dirty = true;
                    } else {
                        int x = getX() + g.getTranslateX();
                        int y = getY() + g.getTranslateY();
                        int w = getWidth();
                        int h = getHeight();
                        if (x != lp.x || y != lp.y || w != lp.w || h != lp.h) {
                            lp.dirty = true;
                            lp.x = x;
                            lp.y = y;
                            lp.w = w;
                            lp.h = h;
                        }
                    }
                } else {
                    final AndroidAsyncView.LayoutParams finalLp = new AndroidAsyncView.LayoutParams(
                            getX() + g.getTranslateX(),
                            getY() + g.getTranslateY(),
                            getWidth(),
                            getHeight(), AndroidPeer.this);
                    v.post(new Runnable() {
                        @Override
                        public void run() {
                            v.setLayoutParams(finalLp);
                        }
                    });
                    finalLp.dirty = true;
                    lp = finalLp;
                }

                // this is a mutable image or side menu etc. where the peer is drawn on a different form...
                // Special case...
                if(nativeGraphics.getClass() == AndroidGraphics.class) {
                    if(peerImage == null) {
                        peerImage = generatePeerImage();
                    }
                    //systemOut("Drawing native image");
                    g.drawImage(peerImage, getX(), getY());
                    return;
                }
                peerImage = null;

                ((AndroidGraphics)nativeGraphics).drawView(v, lp);
            } else {
                super.paint(g);
            }
        }

        @Override
        protected void onPositionSizeChange() {
            if(!superPeerMode) {
                Form f = getComponentForm();
                if (v.getVisibility() == View.INVISIBLE
                        && f != null
                        && Display.getInstance().getCurrent() == f) {
                    doSetVisibilityInternal(true);
                    return;
                }
                layoutPeer();
            }
        }

        protected void layoutPeer(){
            if (getActivity() == null) {
                return;
            }
            if(!superPeerMode) {
                // called by Codename One EDT to position the native component.
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        if (layoutWrapper != null) {
                            if (v.getVisibility() == View.VISIBLE) {

                                RelativeLayout.LayoutParams layoutParams = layoutWrapper.createMyLayoutParams(
                                        AndroidImplementation.AndroidPeer.this.getAbsoluteX(),
                                        AndroidImplementation.AndroidPeer.this.getAbsoluteY(),
                                        AndroidImplementation.AndroidPeer.this.getWidth(),
                                        AndroidImplementation.AndroidPeer.this.getHeight());
                                layoutWrapper.setLayoutParams(layoutParams);
                                if (AndroidImplementation.this.relativeLayout != null) {
                                    AndroidImplementation.this.relativeLayout.requestLayout();
                                }

                            }
                        }
                    }
                });
            }
        }
        
        void blockNativeFocus(boolean block) {
            if (layoutWrapper != null) {
                layoutWrapper.setDescendantFocusability(block
                        ? ViewGroup.FOCUS_BLOCK_DESCENDANTS : ViewGroup.FOCUS_AFTER_DESCENDANTS);
            }
        }

        @Override
        public boolean isFocusable() {
            // EDT
            if (v != null) {
                return v.isFocusableInTouchMode() || v.isFocusable();
            } else {
                return super.isFocusable();
            }
        }

        @Override
        public void setFocusable(final boolean focusable) {
            // EDT
            super.setFocusable(focusable);
            if (getActivity() == null) {
                return;
            }
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    v.setFocusable(focusable);
                }
            });
        }

        @Override
        protected void focusGained() {
            Log.d("Codename One", "native focus gain");
            // EDT
            super.focusGained();
            if (getActivity() == null) {
                return;
            }
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    // allow this one to gain focus
                    blockNativeFocus(false);
                    if (v.isInTouchMode()) {
                        v.requestFocusFromTouch();
                    } else {
                        v.requestFocus();
                    }
                }
            });
        }

        @Override
        protected void focusLost() {
            Log.d("Codename One", "native focus loss");
            // EDT
            super.focusLost();
            if (layoutWrapper != null && getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if(isInitialized()) {
                            // request focus of the wrapper. that will trigger the
                            // android focus listener and move focus back to the
                            // base view.
                            layoutWrapper.requestFocus();
                        }
                    }
                });
            }
        }

        public void release() {
            deinitialize();
        }

        @Override
        protected Dimension calcPreferredSize() {
            int w = 1;
            int h = 1;
            Drawable d = v.getBackground();
            if (d != null) {
                w = d.getMinimumWidth();
                h = d.getMinimumHeight();
            }
            w = Math.max(v.getMeasuredWidth(), w);
            h = Math.max(v.getMeasuredHeight(), h);
            if (v instanceof TextView) {
                w = (int) android.text.Layout.getDesiredWidth(((TextView) v).getText(), ((TextView) v).getPaint());
            }
            return new Dimension(w, h);
        }
    }

    /**
     * inner class that wraps the native components. this is a useful thingy to
     * handle focus stuff and buffering.
     */
    class AndroidRelativeLayout extends RelativeLayout {

        private AndroidImplementation.AndroidPeer peer;

        public AndroidRelativeLayout(Context activity, AndroidImplementation.AndroidPeer peer, View v) {
            super(activity);

            this.peer = peer;
            this.setLayoutParams(createMyLayoutParams(peer.getAbsoluteX(), peer.getAbsoluteY(),
                    peer.getWidth(), peer.getHeight()));
            if (v.getParent() != null) {
                ((ViewGroup)v.getParent()).removeView(v);
            }
            this.addView(v, new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.FILL_PARENT,
                    RelativeLayout.LayoutParams.FILL_PARENT));
            this.setDrawingCacheEnabled(false);
            this.setAlwaysDrawnWithCacheEnabled(false);
            this.setFocusable(true);
            this.setFocusableInTouchMode(false);
            this.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        }

        /**
         * create a layout parameter object that holds the native component's
         * position.
         *
         * @return
         */
        private RelativeLayout.LayoutParams createMyLayoutParams(int x, int y, int width, int height) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            layoutParams.width = width;
            layoutParams.height = height;
            layoutParams.leftMargin = x;
            layoutParams.topMargin = y;
            return layoutParams;
        }
        
        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            
            int keycode = event.getKeyCode();
            keycode = CodenameOneView.internalKeyCodeTranslate(keycode);
            if (keycode == AndroidImplementation.DROID_IMPL_KEY_BACK) {
                switch (event.getAction()) {
                    case KeyEvent.ACTION_DOWN:
                        Display.getInstance().keyPressed(keycode);
                        break;
                    case KeyEvent.ACTION_UP:
                        Display.getInstance().keyReleased(keycode);
                        break;
                }
                return true;
            } else {
                return super.dispatchKeyEvent(event);
            }
        }
        

    }
    
    private boolean testedNativeTheme;
    private boolean nativeThemeAvailable;

    public boolean hasNativeTheme() {
        if (!testedNativeTheme) {
            testedNativeTheme = true;
            try {
                InputStream is;
                if (android.os.Build.VERSION.SDK_INT < 14 && !isTablet()) {
                    is = getResourceAsStream(getClass(), "/androidTheme.res");
                } else {
                    is = getResourceAsStream(getClass(), "/android_holo_light.res");
                }
                nativeThemeAvailable = is != null;
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return nativeThemeAvailable;
    }

    /**
     * Installs the native theme, this is only applicable if hasNativeTheme()
     * returned true. Notice that this method might replace the
     * DefaultLookAndFeel instance and the default transitions.
     */
    public void installNativeTheme() {
        hasNativeTheme();
        if (nativeThemeAvailable) {
            try {
                InputStream is;
                if (android.os.Build.VERSION.SDK_INT < 14 && !isTablet() || Display.getInstance().getProperty("and.hololight", "false").equals("true")) {
                    is = getResourceAsStream(getClass(), "/androidTheme.res");
                } else {
                    is = getResourceAsStream(getClass(), "/android_holo_light.res");
                }
                Resources r = Resources.open(is);
                Hashtable h = r.getTheme(r.getThemeResourceNames()[0]);
                h.put("@commandBehavior", "Native");
                UIManager.getInstance().setThemeProps(h);
                is.close();
                Display.getInstance().setCommandBehavior(Display.COMMAND_BEHAVIOR_NATIVE);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public boolean isNativeBrowserComponentSupported() {
        return true;
    }

    @Override
    public void setNativeBrowserScrollingEnabled(final PeerComponent browserPeer, final boolean e) {
        super.setNativeBrowserScrollingEnabled(browserPeer, e);
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                AndroidBrowserComponent bc = (AndroidBrowserComponent)browserPeer;
                bc.setScrollingEnabled(e);
            }
        });
    }

    @Override
    public void setPinchToZoomEnabled(final PeerComponent browserPeer, final boolean e) {
        super.setPinchToZoomEnabled(browserPeer, e);
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                AndroidBrowserComponent bc = (AndroidBrowserComponent)browserPeer;
                bc.setPinchZoomEnabled(e);
            }
        });
    }

    public PeerComponent createBrowserComponent(final Object parent) {
        if (getActivity() == null) {
            return null;
        }
        final AndroidImplementation.AndroidBrowserComponent[] bc = new AndroidImplementation.AndroidBrowserComponent[1];

        final Object lock = new Object();

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    WebView wv = new WebView(getActivity()) {

                        public boolean onKeyDown(int keyCode, KeyEvent event) {
                            switch (keyCode) {
                                case KeyEvent.KEYCODE_BACK:
                                    Display.getInstance().keyPressed(AndroidImplementation.DROID_IMPL_KEY_BACK);
                                    return true;
                                case KeyEvent.KEYCODE_MENU:
                                    //if the native commands are used don't handle the keycode
                                    if (Display.getInstance().getCommandBehavior() != Display.COMMAND_BEHAVIOR_NATIVE) {
                                        Display.getInstance().keyPressed(AndroidImplementation.DROID_IMPL_KEY_MENU);
                                        return true;
                                    }
                            }
                            return super.onKeyDown(keyCode, event);
                        }

                        public boolean onKeyUp(int keyCode, KeyEvent event) {
                            switch (keyCode) {
                                case KeyEvent.KEYCODE_BACK:
                                    Display.getInstance().keyReleased(AndroidImplementation.DROID_IMPL_KEY_BACK);
                                    return true;
                                case KeyEvent.KEYCODE_MENU:
                                    //if the native commands are used don't handle the keycode
                                    if (Display.getInstance().getCommandBehavior() != Display.COMMAND_BEHAVIOR_NATIVE) {
                                        Display.getInstance().keyPressed(AndroidImplementation.DROID_IMPL_KEY_MENU);
                                        return true;
                                    }
                            }
                            return super.onKeyUp(keyCode, event);
                        }
                    };
                    wv.setOnTouchListener(new View.OnTouchListener() {

                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            switch (event.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                case MotionEvent.ACTION_UP:
                                    if (!v.hasFocus()) {
                                        v.requestFocus();
                                    }
                                    break;
                            }
                            return false;
                        }
                    });
                    wv.getSettings().setDomStorageEnabled(true);
                    wv.requestFocus(View.FOCUS_DOWN);
                    wv.setFocusableInTouchMode(true);
                    bc[0] = new AndroidImplementation.AndroidBrowserComponent(wv, getActivity(), parent);
                    lock.notify();
                }
            }
        });
        Display.getInstance().invokeAndBlock(new Runnable() {
            public void run() {
                synchronized (lock) {
                    while (bc[0] == null) {
                        try {
                            lock.wait();
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }

        });

        return bc[0];
    }

    public void setBrowserProperty(PeerComponent browserPeer, String key, Object value) {
        ((AndroidImplementation.AndroidBrowserComponent) browserPeer).setProperty(key, value);
    }

    public String getBrowserTitle(PeerComponent browserPeer) {
        return ((AndroidImplementation.AndroidBrowserComponent) browserPeer).getTitle();
    }

    public String getBrowserURL(PeerComponent browserPeer) {
        return ((AndroidImplementation.AndroidBrowserComponent) browserPeer).getURL();
    }

    public void setBrowserURL(PeerComponent browserPeer, String url) {
        if (url.startsWith("jar:")) {
            url = url.substring(6);
            if(url.indexOf("/") != 0) {
                url = "/"+url;
            }

            url = "file:///android_asset"+url;
        }
        AndroidImplementation.AndroidBrowserComponent bc = (AndroidImplementation.AndroidBrowserComponent) browserPeer;
        if(bc.parent.getBrowserNavigationCallback().shouldNavigate(url)) {
            bc.setURL(url);
        }
    }

    public void browserStop(PeerComponent browserPeer) {
        ((AndroidImplementation.AndroidBrowserComponent) browserPeer).stop();
    }

    public void browserDestroy(PeerComponent browserPeer) {
        ((AndroidImplementation.AndroidBrowserComponent) browserPeer).destroy();
    }
    
    /**
     * Reload the current page
     *
     * @param browserPeer browser instance
     */
    public void browserReload(PeerComponent browserPeer) {
        ((AndroidImplementation.AndroidBrowserComponent) browserPeer).reload();
    }

    /**
     * Indicates whether back is currently available
     *
     * @param browserPeer browser instance
     * @return true if back should work
     */
    public boolean browserHasBack(PeerComponent browserPeer) {
        return ((AndroidImplementation.AndroidBrowserComponent) browserPeer).hasBack();
    }

    public boolean browserHasForward(PeerComponent browserPeer) {
        return ((AndroidImplementation.AndroidBrowserComponent) browserPeer).hasForward();
    }

    public void browserBack(PeerComponent browserPeer) {
        ((AndroidImplementation.AndroidBrowserComponent) browserPeer).back();
    }

    public void browserForward(PeerComponent browserPeer) {
        ((AndroidImplementation.AndroidBrowserComponent) browserPeer).forward();
    }

    public void browserClearHistory(PeerComponent browserPeer) {
        ((AndroidImplementation.AndroidBrowserComponent) browserPeer).clearHistory();
    }

    public void setBrowserPage(PeerComponent browserPeer, String html, String baseUrl) {
        ((AndroidImplementation.AndroidBrowserComponent) browserPeer).setPage(html, baseUrl);
    }

    public void browserExposeInJavaScript(PeerComponent browserPeer, Object o, String name) {
        ((AndroidImplementation.AndroidBrowserComponent) browserPeer).exposeInJavaScript(o, name);
    }


    /**
     * Executes javascript and returns a string result where appropriate.
     * @param browserPeer
     * @param javaScript
     * @return
     */
    @Override
    public String browserExecuteAndReturnString(final PeerComponent browserPeer, final String javaScript) {
        final AndroidImplementation.AndroidBrowserComponent bc = (AndroidImplementation.AndroidBrowserComponent) browserPeer;

        // The jsCallback is a special java object exposed to javascript that we use
        // to return values from javascript to java.
        synchronized (bc.jsCallback){
            // Initialize the return value to null
            bc.jsCallback.setReturnValue(null);

            // Reset the callback so that it will fire the notify() when
            // a value is set.
            bc.jsCallback.reset();
        }

        // We are placing the javascript inside eval() so we need to escape
        // the input.
        String escaped = StringUtil.replaceAll(javaScript, "\\", "\\\\");
        escaped = StringUtil.replaceAll(escaped, "'", "\\'");

        final String js = "javascript:(function(){"
                + AndroidBrowserComponentCallback.JS_RETURNVAL_VARNAME+"=null;try{"
                + AndroidBrowserComponentCallback.JS_RETURNVAL_VARNAME
                + "=eval('"+escaped +"');} catch (e){console.log(e)};"
                + AndroidBrowserComponentCallback.JS_VAR_NAME+".setReturnValue(''+"
                + AndroidBrowserComponentCallback.JS_RETURNVAL_VARNAME
                + ");})()";

        // Send the Javascript string via SetURL.
        // NOTE!! This is sent asynchronously so we will need to wait for
        // the result to come in.
        bc.setURL(js);
        if(Display.getInstance().isEdt()) {
            // If we are on the EDT then we need to invokeAndBlock
            // so that we wait for the javascript result, but we don't
            // prevent the EDT from executing the rest of the pipeline.
            Display.getInstance().invokeAndBlock(new Runnable() {
                public void run() {
                    // Loop/wait until the callback value has been set.
                    // The callback.setReturnValue() method, which will
                    // be called from Javascript issues a notify() to
                    // let us know it is done.
                    while (!bc.jsCallback.isValueSet()) {
                        synchronized(bc.jsCallback){
                            try {
                                bc.jsCallback.wait(200);
                            } catch (InterruptedException ex) {}
                        }
                    }
                }
            });
        } else {
            // If we are not on the EDT, then it is safe to just loop and wait.
            while (!bc.jsCallback.isValueSet()) {
                synchronized(bc.jsCallback){
                    try {
                        bc.jsCallback.wait(200);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
        return bc.jsCallback.getReturnValue();
    }

    
    public boolean canForceOrientation() {
        return true;
    }

    public void lockOrientation(boolean portrait) {
        if (getActivity() == null) {
            return;
        }
        if(portrait){
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }else{
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);        
        }
    }

    public void unlockOrientation() {
        if (getActivity() == null) {
            return;
        }
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
    }
    
    
    
    public boolean isAffineSupported() {
        return true;
    }

    public void resetAffine(Object nativeGraphics) {
        ((AndroidGraphics) nativeGraphics).resetAffine();
    }

    public void scale(Object nativeGraphics, float x, float y) {
        ((AndroidGraphics) nativeGraphics).scale(x, y);
    }

    public void rotate(Object nativeGraphics, float angle) {
        ((AndroidGraphics) nativeGraphics).rotate(angle);
    }

    public void rotate(Object nativeGraphics, float angle, int x, int y) {
        ((AndroidGraphics) nativeGraphics).rotate(angle, x, y);
    }

    public void shear(Object nativeGraphics, float x, float y) {
    }

    public boolean isTablet() {
        return (getContext().getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * Executes r on the UI thread and blocks the EDT to completion
     * @param r runnable to execute
     */
    public static void runOnUiThreadAndBlock(final Runnable r) {
        if (getActivity() == null) {
            throw new RuntimeException("Cannot run on UI thread because getActivity() is null.  This generally means we are running inside a service in the background so UI access is disabled.");
        }
                
        final boolean[] completed = new boolean[1];
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                r.run();
                completed[0] = true;
                synchronized(completed) {
                    completed.notify();
                }
            }
        });
        Display.getInstance().invokeAndBlock(new Runnable() {
            @Override
            public void run() {
                synchronized(completed) {
                    while(!completed[0]) {
                        try {
                            completed.wait();
                        } catch(InterruptedException err) {}
                    }
                }
            }
        });
    }
    
    public int convertToPixels(int dipCount, boolean horizontal) {
        DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        float ppi = dm.density * 160f;
        return (int) (((float) dipCount) / 25.4f * ppi);
    }

    public boolean isPortrait() {
        int orientation = getContext().getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_UNDEFINED
                || orientation == Configuration.ORIENTATION_SQUARE) {
            return super.isPortrait();
        }
        return orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    
    class AndroidBrowserComponent extends AndroidImplementation.AndroidPeer {

        private Activity act;
        private WebView web;
        private BrowserComponent parent;
        private boolean scrollingEnabled = true;
        protected AndroidBrowserComponentCallback jsCallback;
        private boolean lightweightMode = false;        
        private ProgressDialog progressBar;
        private boolean hideProgress;
        private int layerType;
        
        
        public AndroidBrowserComponent(final WebView web, Activity act, Object p) {
            super(web);
            if(!superPeerMode) {
                doSetVisibility(false);
            }
            parent = (BrowserComponent) p;
            this.web = web;
            layerType = web.getLayerType();
            web.getSettings().setJavaScriptEnabled(true);
            web.getSettings().setSupportZoom(parent.isPinchToZoomEnabled());
            this.act = act;
            jsCallback = new AndroidBrowserComponentCallback();
            hideProgress = Display.getInstance().getProperty("WebLoadingHidden", "false").equals("true");
            
            web.addJavascriptInterface(jsCallback, AndroidBrowserComponentCallback.JS_VAR_NAME);

            web.setWebViewClient(new WebViewClient() {
                public void onLoadResource(WebView view, String url) {
                    if (Display.getInstance().getProperty("syncNativeCookies", "true").equals("true")) {
                        try {
                            URI uri = new URI(url);
                            CookieManager mgr = CookieManager.getInstance();
                            String cookieStr = mgr.getCookie(url);
                            if (cookieStr != null) {
                                String[] cookies = cookieStr.split(";");
                                int len = cookies.length;
                                Vector out = new Vector();
                                String domain = uri.getHost();
                                for (int i = 0; i < len; i++) {
                                    Cookie c = new Cookie();
                                    String[] parts = cookies[i].split("=");
                                    c.setName(parts[0].trim());
                                    if (parts.length > 1) {
                                        c.setValue(parts[1].trim());
                                    } else {
                                        c.setValue("");
                                    }
                                    c.setDomain(domain);
                                    out.add(c);
                                }
                                Cookie[] cookiesArr = new Cookie[out.size()];
                                out.toArray(cookiesArr);
                                AndroidImplementation.this.addCookie(cookiesArr, false);
                            }

                        } catch (URISyntaxException ex) {

                        }
                    }
                    parent.fireWebEvent("onLoadResource", new ActionEvent(url));
                    super.onLoadResource(view, url);
                    setShouldCalcPreferredSize(true);
                }

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    if (getActivity() == null) {
                        return;
                    }
                            
                    parent.fireWebEvent("onStart", new ActionEvent(url));
                    super.onPageStarted(view, url, favicon);
                    dismissProgress();
                    //show the progress only if there is no ActionBar
                    if(!hideProgress && !isNativeTitle()){
                        progressBar = ProgressDialog.show(getActivity(), null, "Loading...");
                        //if the page hasn't finished for more the 10 sec, dismiss 
                        //the dialog
                        Timer t= new Timer();
                        t.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                dismissProgress();
                            }
                        }, 10000);
                    }
                }

                public void onPageFinished(WebView view, String url) {
                    parent.fireWebEvent("onLoad", new ActionEvent(url));
                    super.onPageFinished(view, url);
                    setShouldCalcPreferredSize(true);
                    dismissProgress();
                }
                
                private void dismissProgress() {
                    if (progressBar != null && progressBar.isShowing()) {
                        progressBar.dismiss();
                        Display.getInstance().callSerially(new Runnable() {

                            public void run() {
                                setVisible(true);
                                repaint();
                            }
                        });
                    }
                }

                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    parent.fireWebEvent("onError", new ActionEvent(description, errorCode));
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    super.shouldOverrideKeyEvent(view, null);
                    dismissProgress();
                }

                public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
                    int keyCode = event.getKeyCode();
                    if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
                        return true;
                    }

                    return super.shouldOverrideKeyEvent(view, event);
                }

                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    if (url.startsWith("jar:")) {
                        setURL(url);
                        return true;
                    }
                    
                    // this will fail if dial permission isn't declared
                    if(url.startsWith("tel:")) {
                        if(parent.getBrowserNavigationCallback().shouldNavigate(url)) {
                            try {
                                Intent dialer = new Intent(android.content.Intent.ACTION_DIAL, Uri.parse(url));
                                getContext().startActivity(dialer);
                            } catch(Throwable t) {}
                        }
                        return true;
                    }
                    // this will fail if dial permission isn't declared
                    if(url.startsWith("mailto:")) {
                        if(parent.getBrowserNavigationCallback().shouldNavigate(url)) {
                            try {
                                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));        
                                getContext().startActivity(emailIntent);
                            } catch(Throwable t) {}
                        }
                        return true;
                    }
                    return !parent.getBrowserNavigationCallback().shouldNavigate(url); 
                }
                
                
            });
            
            web.setWebChromeClient(new WebChromeClient(){
                @Override
                public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                    com.codename1.io.Log.p("["+consoleMessage.messageLevel()+"] "+consoleMessage.message()+" On line "+consoleMessage.lineNumber()+" of "+consoleMessage.sourceId());
                    return true;
                }

               @Override 
               public void onProgressChanged(WebView view, int newProgress) {
                    if(!hideProgress && isNativeTitle() && getCurrentForm() != null && getCurrentForm().getTitle() != null && getCurrentForm().getTitle().length() > 0 ){
                        if(getActivity() != null){
                            try{
                                getActivity().setProgressBarVisibility(true);
                                getActivity().setProgress(newProgress * 100);
                                if(newProgress == 100){
                                    getActivity().setProgressBarVisibility(false);
                                }                            
                            }catch(Throwable t){
                            }
                        }
                    }
                }
                 
                @Override
                public void onGeolocationPermissionsShowPrompt(String origin,
                        GeolocationPermissions.Callback callback) {
                    // Always grant permission since the app itself requires location
                    // permission and the user has therefore already granted it
                    callback.invoke(origin, true, false);
                }
            });
        }
        
        @Override
        protected void initComponent() {
            if(android.os.Build.VERSION.SDK_INT == 21 && web.getLayerType() != layerType){
                    act.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            web.setLayerType(layerType, null); //setting layer type to original state
                        }
                    });
            }
            super.initComponent();
            blockNativeFocus(false);
            setPeerImage(null);
        }
        
        
        @Override
        protected Image generatePeerImage() {
            try {
                final Bitmap nativeBuffer = Bitmap.createBitmap(
                        getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
                Image image = new AndroidImplementation.NativeImage(nativeBuffer);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Canvas canvas = new Canvas(nativeBuffer);
                            web.draw(canvas);
                        } catch(Throwable t) {
                            t.printStackTrace();
                        }
                    }
                });
                return image;
            } catch(Throwable t) {
                t.printStackTrace();
                return Image.createImage(5, 5);
            }
        }
        
        protected boolean shouldRenderPeerImage() {
            return lightweightMode || !isInitialized();
        }

        protected void setLightweightMode(boolean l) {
            doSetVisibility(!l);
            if (lightweightMode == l) {
                return;
            }
            lightweightMode = l;
        }
        
        

        public void setScrollingEnabled(final boolean enabled){
            this.scrollingEnabled = enabled;
            act.runOnUiThread(new Runnable() {
                public void run() {
                    web.setHorizontalScrollBarEnabled(enabled);
                    web.setVerticalScrollBarEnabled(enabled);
                    if ( !enabled ){
                        web.setOnTouchListener(new View.OnTouchListener(){

                            @Override
                            public boolean onTouch(View view, MotionEvent me) {
                                return (me.getAction() == MotionEvent.ACTION_MOVE);
                            }

                        });
                    } else {
                       web.setOnTouchListener(null);
                    }
                }
            });
            
        }
        
        public boolean isScrollingEnabled(){
            return scrollingEnabled;
        }
        
        public void setProperty(final String key, final Object value) {
            act.runOnUiThread(new Runnable() {
                public void run() {
                    WebSettings s = web.getSettings();
                    if(key.equalsIgnoreCase("useragent")) {
                        s.setUserAgentString((String)value);
                        return;
                    }
                    String methodName = "set" + key;
                    for (Method m : s.getClass().getMethods()) {
                        if (m.getName().equalsIgnoreCase(methodName) && m.getParameterTypes().length == 0) {
                            try {
                                m.invoke(s, value);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                            return;
                        }
                    }
                }
            });
        }

        public String getTitle() {
            final String[] retVal = new String[1];
            
            act.runOnUiThread(new Runnable() {
                public void run() {
                    retVal[0] = web.getTitle();
                }
            });
            
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    while (retVal[0] == null) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            });
            return retVal[0];
        }

        public String getURL() {
            final String[] retVal = new String[1];
            
            act.runOnUiThread(new Runnable() {
                public void run() {
                    retVal[0] = web.getUrl();
                }
            });
            
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    while (retVal[0] == null) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            });
            return retVal[0];
        }

        public void setURL(final String url) {
            act.runOnUiThread(new Runnable() {
                public void run() {
                    web.loadUrl(url);
                }
            });
        }

        public void reload() {
            act.runOnUiThread(new Runnable() {
                public void run() {
                    web.reload();
                }
            });
        }

        public boolean hasBack() {
            final Boolean [] retVal = new Boolean[1];
            
            act.runOnUiThread(new Runnable() {
                public void run() {
                    retVal[0] = web.canGoBack();
                }
            });
            
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    while (retVal[0] == null) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            });
            return retVal[0].booleanValue();
        }

        public boolean hasForward() {
            final Boolean [] retVal = new Boolean[1];
            
            act.runOnUiThread(new Runnable() {
                public void run() {
                    retVal[0] = web.canGoForward();
                }
            });
            
            Display.getInstance().invokeAndBlock(new Runnable() {
                @Override
                public void run() {
                    while (retVal[0] == null) {
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            });
            return retVal[0].booleanValue();
        }

        public void back() {
            act.runOnUiThread(new Runnable() {
                public void run() {
                    web.goBack();
                }
            });
        }

        public void forward() {
            act.runOnUiThread(new Runnable() {
                public void run() {
                    web.goForward();
                }
            });
        }

        public void clearHistory() {
            act.runOnUiThread(new Runnable() {
                public void run() {
                    web.clearHistory();
                }
            });
        }

        public void stop() {
            act.runOnUiThread(new Runnable() {
                public void run() {
                    web.stopLoading();
                }
            });
        }
        
        public void destroy() {
            act.runOnUiThread(new Runnable() {
                public void run() {
                    web.destroy();
                }
            });
        }
        
        public void setPage(final String html, final String baseUrl) {
            act.runOnUiThread(new Runnable() {
                public void run() {
                    web.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null);
                }
            });
        }

        public void exposeInJavaScript(final Object o, final String name) {
            act.runOnUiThread(new Runnable() {
                public void run() {
                    web.addJavascriptInterface(o, name);
                }
            });
        }

        public  void setPinchZoomEnabled(final boolean e) {
            act.runOnUiThread(new Runnable() {
                public void run() {
                    web.getSettings().setSupportZoom(e);
                    web.getSettings().setBuiltInZoomControls(e);
                }
            });
        }

        @Override
        protected void deinitialize() {
            act.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(android.os.Build.VERSION.SDK_INT == 21) { // bugfix for Android 5.0.x
                        web.setLayerType(View.LAYER_TYPE_SOFTWARE, null); //setting layer type to software to prevent the sigseg 11 crash
                    }
                }
            });
            super.deinitialize();
        }
    }

    
    
    public Object connect(String url, boolean read, boolean write, int timeout) throws IOException {
        URL u = new URL(url);
        CookieHandler.setDefault(null);
        URLConnection con = u.openConnection();
        if (con instanceof HttpURLConnection) {
            HttpURLConnection c = (HttpURLConnection) con;
            c.setUseCaches(false);
            c.setDefaultUseCaches(false);
            c.setInstanceFollowRedirects(false);
            if(timeout > -1) {
                c.setConnectTimeout(timeout);
            }
            if(read){
                if(timeout > -1) {                
                    c.setReadTimeout(timeout);
                }else{
                    c.setReadTimeout(10000);                
                }
            }
            if (android.os.Build.VERSION.SDK_INT > 13) { 
                c.setRequestProperty("Connection", "close"); 
            }
        }
        con.setDoInput(read);
        con.setDoOutput(write);
        return con;
    }
    
    /**
     * @inheritDoc
     */
    public Object connect(String url, boolean read, boolean write) throws IOException {
        return connect(url, read, write, timeout);
    }

    /**
     * @inheritDoc
     */
    public void setHeader(Object connection, String key, String val) {
        ((URLConnection) connection).setRequestProperty(key, val);
    }

    @Override
    public void setChunkedStreamingMode(Object connection, int bufferLen){    
        HttpURLConnection con = ((HttpURLConnection) connection);
        con.setChunkedStreamingMode(bufferLen);
    }
    
    

    /**
     * @inheritDoc
     */
    public OutputStream openOutputStream(Object connection) throws IOException {
        if (connection instanceof String) {
            String con = (String)connection;
            if (con.startsWith("file://")) {
                con = con.substring(7);
            }

            OutputStream fc = createFileOuputStream((String) con);
            BufferedOutputStream o = new BufferedOutputStream(fc, (String) con);
            return o;
        }
        return new BufferedOutputStream(((URLConnection) connection).getOutputStream(), connection.toString());
    }

    /**
     * @inheritDoc
     */
    public OutputStream openOutputStream(Object connection, int offset) throws IOException {
        String con = (String) connection;
        if (con.startsWith("file://")) {
            con = con.substring(7);
        }
        RandomAccessFile rf = new RandomAccessFile(con, "rw");
        rf.seek(offset);
        FileOutputStream fc = new FileOutputStream(rf.getFD());
        BufferedOutputStream o = new BufferedOutputStream(fc, con);
        o.setConnection(rf);
        return o;
    }

    /**
     * @inheritDoc
     */
    public void cleanup(Object o) {
        try {
            super.cleanup(o);
            if (o != null) {
                if (o instanceof RandomAccessFile) {
                    ((RandomAccessFile) o).close();
                }
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @inheritDoc
     */
    public InputStream openInputStream(Object connection) throws IOException {
        if (connection instanceof String) {
            String con = (String) connection;
            if (con.startsWith("file://")) {
                con = con.substring(7);
            }
            InputStream fc = createFileInputStream(con);
            BufferedInputStream o = new BufferedInputStream(fc, con);
            return o;
        }
        if(connection instanceof HttpURLConnection) {
            HttpURLConnection ht = (HttpURLConnection)connection;
            if(ht.getResponseCode() < 400) {
                return new BufferedInputStream(ht.getInputStream());
            }
            return new BufferedInputStream(ht.getErrorStream());
        } else {
            return new BufferedInputStream(((URLConnection) connection).getInputStream());
        }        
    }

    /**
     * @inheritDoc
     */
    public void setHttpMethod(Object connection, String method) throws IOException {
        ((HttpURLConnection) connection).setRequestMethod(method);
    }

    /**
     * @inheritDoc
     */
    public void setPostRequest(Object connection, boolean p) {
        try {
            if (p) {
                ((HttpURLConnection) connection).setRequestMethod("POST");
            } else {
                ((HttpURLConnection) connection).setRequestMethod("GET");
            }
        } catch (IOException err) {
            // an exception here doesn't make sense
            err.printStackTrace();
        }
    }

    /**
     * @inheritDoc
     */
    public int getResponseCode(Object connection) throws IOException {
        // workaround for Android bug discussed here: http://stackoverflow.com/questions/17638398/androids-httpurlconnection-throws-eofexception-on-head-requests
        HttpURLConnection con = (HttpURLConnection) connection;
        if("head".equalsIgnoreCase(con.getRequestMethod())) {
            con.setRequestProperty( "Accept-Encoding", "" );
        }
        return ((HttpURLConnection) connection).getResponseCode();
    }

    /**
     * @inheritDoc
     */
    public String getResponseMessage(Object connection) throws IOException {
        return ((HttpURLConnection) connection).getResponseMessage();
    }

    /**
     * @inheritDoc
     */
    public int getContentLength(Object connection) {
        return ((HttpURLConnection) connection).getContentLength();
    }

    /**
     * @inheritDoc
     */
    public String getHeaderField(String name, Object connection) throws IOException {
        return ((HttpURLConnection) connection).getHeaderField(name);
    }

    /**
     * @inheritDoc
     */
    public String[] getHeaderFieldNames(Object connection) throws IOException {
        Set<String> s = ((HttpURLConnection) connection).getHeaderFields().keySet();
        String[] resp = new String[s.size()];
        s.toArray(resp);
        return resp;
    }

    /**
     * @inheritDoc
     */
    public String[] getHeaderFields(String name, Object connection) throws IOException {
        HttpURLConnection c = (HttpURLConnection) connection;
        List<String> headers = new ArrayList<String>();
        
        // we need to merge headers with differing case since this should be case insensitive
        for(String key : c.getHeaderFields().keySet()) {
            if(key != null && key.equalsIgnoreCase(name)) {
                headers.addAll(c.getHeaderFields().get(key));
            }
        }
        if (headers.size() > 0) {
            List<String> v = new ArrayList<String>();
            v.addAll(headers);
            Collections.reverse(v);
            String[] s = new String[v.size()];
            v.toArray(s);
            return s;
        }
        // workaround for a bug in some android devices
        String f = c.getHeaderField(name);
        if(f != null && f.length() > 0) {
            return new String[] {f};
        }
        return null;



    }

    /**
     * @inheritDoc
     */
    public void deleteStorageFile(String name) {
        getContext().deleteFile(name);
    }

    /**
     * @inheritDoc
     */
    public OutputStream createStorageOutputStream(String name) throws IOException {
        return getContext().openFileOutput(name, 0);
    }

    /**
     * @inheritDoc
     */
    public InputStream createStorageInputStream(String name) throws IOException {
        return getContext().openFileInput(name);
    }

    /**
     * @inheritDoc
     */
    public boolean storageFileExists(String name) {
        String[] fileList = getContext().fileList();
        for (int iter = 0; iter < fileList.length; iter++) {
            if (fileList[iter].equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @inheritDoc
     */
    public String[] listStorageEntries() {
        return getContext().fileList();
    }

    /**
     * @inheritDoc
     */
    public int getStorageEntrySize(String name) {
        return (int)new File(getContext().getFilesDir(), name).length();
    }
    
    /**
     * @inheritDoc
     */
    public String[] listFilesystemRoots() {
        
        if(!checkForPermission(Manifest.permission.READ_EXTERNAL_STORAGE, "This is required to browse the file system")){
            return new String[]{};
        }
        
        String [] storageDirs = getStorageDirectories();
        if(storageDirs != null){
            String [] roots = new String[storageDirs.length + 1];
            System.arraycopy(storageDirs, 0, roots, 0, storageDirs.length);
            roots[roots.length - 1] = Environment.getRootDirectory().getAbsolutePath();
            return roots;
        }
        return new String[]{Environment.getRootDirectory().getAbsolutePath()};
    }

    @Override
    public boolean hasCachesDir() {
        return true;
    }

    @Override
    public String getCachesDir() {
        return getContext().getCacheDir().getAbsolutePath();
    }
    
    
    
    private String[] getStorageDirectories() {
        String [] storageDirs = null;
        
        String storageDev = Environment.getExternalStorageDirectory().getPath();
        String storageRoot = storageDev.substring(0, storageDev.length() - 1);
        BufferedReader bufReader = null;
        
        try {
            bufReader = new BufferedReader(new FileReader("/proc/mounts"));
            ArrayList<String> list = new ArrayList<String>();
            String line;
            
            while ((line = bufReader.readLine()) != null) {
                if (line.contains("vfat") || line.contains("/mnt") || line.contains("/storage")) {
                    StringTokenizer tokens = new StringTokenizer(line, " ");
                    String s = tokens.nextToken();
                    s = tokens.nextToken(); // Take the second token, i.e. mount point
                    
                    if (s.indexOf("secure") != -1) {
                        continue;
                    }

                    if (s.startsWith(storageRoot) == true) {
                        list.add(s);
                        continue;
                    }

                    if (line.contains("vfat") && line.contains("/mnt")) {
                        list.add(s);
                        continue;
                    }
                }
            }

            int count = list.size();
            
            if (count < 2) {
                storageDirs = new String[] {
                    storageDev
                };
            }
            else {
                storageDirs = new String[count];

                for (int i = 0; i < count; i++) {
                    storageDirs[i] = (String) list.get(i);
                }
            }
        }
        catch (FileNotFoundException e) {}
        catch (IOException e) {}
        finally {
            if (bufReader != null) {
                try {
                    bufReader.close();
                }
                catch (IOException e) {}
            }

            return storageDirs;
        }
    }    

    /**
     * @inheritDoc
     */
    public String getAppHomePath() {
        return getContext().getFilesDir().getAbsolutePath() + "/";
    }

    /**
     * @inheritDoc
     */
    public String[] listFiles(String directory) throws IOException {
        return new File(directory).list();
    }

    /**
     * @inheritDoc
     */
    public long getRootSizeBytes(String root) {
        return -1;
    }

    /**
     * @inheritDoc
     */
    public long getRootAvailableSpace(String root) {
        return -1;
    }

    /**
     * @inheritDoc
     */
    public void mkdir(String directory) {
        new File(directory).mkdirs();
    }

    /**
     * @inheritDoc
     */
    public void deleteFile(String file) {
        File f = new File(file);
        f.delete();
    }

    /**
     * @inheritDoc
     */
    public boolean isHidden(String file) {
        return new File(file).isHidden();
    }

    /**
     * @inheritDoc
     */
    public void setHidden(String file, boolean h) {
    }

    /**
     * @inheritDoc
     */
    public long getFileLength(String file) {
        return new File(file).length();
    }

    /**
     * @inheritDoc
     */
    public long getFileLastModified(String file) {
        return new File(file).lastModified();
    }
    
    /**
     * @inheritDoc
     */
    public boolean isDirectory(String file) {
        return new File(file).isDirectory();
    }

    /**
     * @inheritDoc
     */
    public char getFileSystemSeparator() {
        return File.separatorChar;
    }

    /**
     * @inheritDoc
     */
    public OutputStream openFileOutputStream(String file) throws IOException {
        OutputStream os = null;
        try{
            os = createFileOuputStream(file);        
        }catch(FileNotFoundException fne){
            //It is impossible to know if a path is considered an external 
            //storage on the various android's versions.
            //So we try to open the path and if failed due to permission we will 
            //ask for the permission from the user
            if(fne.getMessage().contains("Permission denied")){
                
                if(!checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, "This is required to access the file")){
                    //The user refused to give access.
                    return null;
                }else{
                    //The user gave permission try again to access the path
                    return createFileOuputStream(file);
                }
                
            }else{
                throw fne;
            }
        }
        
        return os;        
    }

    /**
     * @inheritDoc
     */
    public InputStream openFileInputStream(String file) throws IOException {
        InputStream is = null;
        try{
            is = createFileInputStream(file);        
        }catch(FileNotFoundException fne){
            //It is impossible to know if a path is considered an external 
            //storage on the various android's versions.
            //So we try to open the path and if failed due to permission we will 
            //ask for the permission from the user
            if(fne.getMessage().contains("Permission denied")){
                
                if(!checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, "This is required to access the file")){
                    //The user refused to give access.
                    return null;
                }else{
                    //The user gave permission try again to access the path
                    return openFileInputStream(file);
                }
                
            }else{
                throw fne;
            }
        }
        
        return is;
    }

    @Override
    public boolean isMultiTouch() {
        return true;
    }

    /**
     * @inheritDoc
     */
    public boolean exists(String file) {
        if (file.startsWith("file://")) {
            file = file.substring(7);
        }
        return new File(file).exists();
    }

    /**
     * @inheritDoc
     */
    public void rename(String file, String newName) {
        if (file.startsWith("file://")) {
            file = file.substring(7);
        }
        new File(file).renameTo(new File(new File(file).getParentFile(), newName));
    }

    protected File createFileObject(String fileName) {
        return new File(fileName);
    }
    
    protected InputStream createFileInputStream(String fileName) throws FileNotFoundException {
        return new FileInputStream(fileName);
    }
    
    protected InputStream createFileInputStream(File f) throws FileNotFoundException {
        return new FileInputStream(f);
    }

    protected OutputStream createFileOuputStream(String fileName) throws FileNotFoundException {
        return new FileOutputStream(fileName);
    }
        
    protected OutputStream createFileOuputStream(java.io.File f) throws FileNotFoundException {
        return new FileOutputStream(f);
    }
    
    /**
     * @inheritDoc
     */
    public boolean shouldWriteUTFAsGetBytes() {
        return true;
    }


    /**
     * @inheritDoc
     */
    public void closingOutput(OutputStream s) {
        // For some reasons the Android guys chose not doing this by default:
        // http://android-developers.blogspot.com/2010/12/saving-data-safely.html
        // this seems to be a mistake of sacrificing stability for minor performance
        // gains which will only be noticeable on a server.
        if (s != null) {
            if (s instanceof FileOutputStream) {
                try {
                    FileDescriptor fd = ((FileOutputStream) s).getFD();
                    if (fd != null) {
                        fd.sync();
                    }
                } catch (IOException ex) {
                    // this exception doesn't help us
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * @inheritDoc
     */
    public void printStackTraceToStream(Throwable t, Writer o) {
        PrintWriter p = new PrintWriter(o);
        t.printStackTrace(p);
    }

    /**
     * This method returns the platform Location Control
     *
     * @return LocationControl Object
     */
    public LocationManager getLocationManager() {
        if(!checkForPermission(Manifest.permission.ACCESS_FINE_LOCATION, "This is required to get the location")){
            return null;
        }
        
        boolean includesPlayServices = Display.getInstance().getProperty("IncludeGPlayServices", "false").equals("true");
        if (includesPlayServices && hasAndroidMarket()) {
            try {
                Class clazz = Class.forName("com.codename1.location.AndroidLocationPlayServiceManager");
                return (com.codename1.location.LocationManager)clazz.getMethod("getInstance").invoke(null);
            } catch (Exception e) {
                return AndroidLocationManager.getInstance(getContext());
            }
        } else {
            return AndroidLocationManager.getInstance(getContext());
        }
    }

    private String fixAttachmentPath(String attachment) {
        if (attachment.contains(getAppHomePath())) {
            FileSystemStorage fs = FileSystemStorage.getInstance();
            final char sep = fs.getFileSystemSeparator();
            String fileName = attachment.substring(attachment.lastIndexOf(sep) + 1);
            String[] roots = FileSystemStorage.getInstance().getRoots();
            // iOS doesn't have an SD card
            String root = roots[0];
            for (int i = 0; i < roots.length; i++) {
                //media_rw is a protected system lib
                if (FileSystemStorage.getInstance().getRootType(roots[i]) == FileSystemStorage.ROOT_TYPE_SDCARD && !roots[i].contains("media_rw")) {
                    root = roots[i];
                    break;
                }
            }
            //might happen if only the media_rw is of type ROOT_TYPE_SDCARD
            if(root.contains("media_rw")){
                //try again without checking the root type
                for (int i = 0; i < roots.length; i++) {
                    //media_rw is a protected system lib
                    if (!roots[i].contains("media_rw")) {
                        root = roots[i];
                        break;
                    }
                }            
            }
            
            String fileUri = root + sep + "tmp" + sep + fileName;
            FileSystemStorage.getInstance().mkdir(root + sep + "tmp");
            try {
                InputStream is = FileSystemStorage.getInstance().openInputStream(attachment);
                OutputStream os = FileSystemStorage.getInstance().openOutputStream(fileUri);
                byte [] buf = new byte[1024];
                int len;
                while((len = is.read(buf)) > -1){
                    os.write(buf, 0, len);
                }
                is.close();
                os.close();
            } catch (IOException ex) {
                Logger.getLogger(AndroidImplementation.class.getName()).log(Level.SEVERE, null, ex);
            }

            attachment = fileUri;
        }
        if (attachment.indexOf(":") < 0) {
            attachment = "file://" + attachment;
        }
        return attachment;
    }
    
    /**
     * @inheritDoc
     */
    public void sendMessage(String[] recipients, String subject, Message msg) {
        if(editInProgress()) {
            stopEditing(true);
        }
        Intent emailIntent;
        String attachment = msg.getAttachment();
        boolean hasAttachment = (attachment != null && attachment.length() > 0) || msg.getAttachments().size() > 0;
            
        if(msg.getMimeType().equals(Message.MIME_TEXT) && !hasAttachment){
            StringBuilder to = new StringBuilder();
            for (int i = 0; i < recipients.length; i++) {
                to.append(recipients[i]);
                to.append(";");
            }
            emailIntent = new Intent(Intent.ACTION_SENDTO,
                    Uri.parse(
                    "mailto:" + to.toString()
                    + "?subject=" + Uri.encode(subject)
                    + "&body=" + Uri.encode(msg.getContent())));        
        }else{
            if (hasAttachment) {
                if(msg.getAttachments().size() > 1) {
                    emailIntent = new Intent(android.content.Intent.ACTION_SEND_MULTIPLE);
                    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, recipients);
                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
                    emailIntent.setType(msg.getMimeType());
                    ArrayList<Uri> uris = new ArrayList<Uri>();
                    
                    for(String path : msg.getAttachments().keySet()) {
                        uris.add(Uri.parse(fixAttachmentPath(path)));
                    }
                    
                    emailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                } else {
                    emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, recipients);
                    emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
                    emailIntent.setType(msg.getMimeType());
                    emailIntent.setType(msg.getAttachmentMimeType());
                    //if the attachment is in the uder home dir we need to copy it 
                    //to an accessible dir
                    attachment = fixAttachmentPath(attachment);
                    emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(attachment));
                }
            } else {
                emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, recipients);
                emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
                emailIntent.setType(msg.getMimeType());
            }
            if (msg.getMimeType().equals(Message.MIME_HTML)) {
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, Html.fromHtml(msg.getContent()));                                
            }else{
                /*
                // Attempted this workaround to fix the ClassCastException that occurs on android when
                // there are multiple attachments.  Unfortunately, this fixes the stack trace, but 
                // has the unwanted side-effect of producing a blank message body.
                // Same workaround for HTML mimetype also fails the same way.
                // Conclusion, Just live with the stack trace.  It doesn't seem to affect the 
                // execution of the program... treat it as a warning.
                // See https://github.com/codenameone/CodenameOne/issues/1782
                if (msg.getAttachments().size() > 1) {
                    ArrayList<String> contentArr = new ArrayList<String>();
                    contentArr.add(msg.getContent());
                    emailIntent.putStringArrayListExtra(android.content.Intent.EXTRA_TEXT, contentArr);
                } else {
                    emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, msg.getContent());                    
                    
                }*/
                emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, msg.getContent());
            }
            
        }
        final String attach = attachment;
        AndroidNativeUtil.startActivityForResult(Intent.createChooser(emailIntent, "Send mail..."), new IntentResultListener() {

            @Override
            public void onActivityResult(int requestCode, int resultCode, Intent data) {
                if(attach != null && attach.length() > 0 && attach.contains("tmp")){
                    FileSystemStorage.getInstance().delete(attach);
                }
            }
        });
    }

    /**
     * @inheritDoc
     */
    public void dial(String phoneNumber) {
        Intent dialer = new Intent(android.content.Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
        getContext().startActivity(dialer);
    }

    @Override
    public int getSMSSupport() {
        return Display.SMS_BOTH;
    }
    
    /**
     * @inheritDoc
     */
    public void sendSMS(final String phoneNumber, final String message, boolean i) throws IOException {
        if(!checkForPermission(Manifest.permission.SEND_SMS, "This is required to send a SMS")){
            return;
        }
        if(!checkForPermission(Manifest.permission.READ_PHONE_STATE, "This is required to send a SMS")){
            return;
        }
        if(i) {            
            Intent smsIntent = null;
            if(android.os.Build.VERSION.SDK_INT < 19){
                smsIntent = new Intent(Intent.ACTION_VIEW);
                smsIntent.setType("vnd.android-dir/mms-sms");
                smsIntent.putExtra("address", phoneNumber);
                smsIntent.putExtra("sms_body",message);
            }else{
                smsIntent = new Intent(Intent.ACTION_SENDTO);
                smsIntent.setData(Uri.parse("smsto:" + Uri.encode(phoneNumber)));   
                smsIntent.putExtra("sms_body", message); 
            }
            getContext().startActivity(smsIntent);            
            
        } else {
            SmsManager sms = SmsManager.getDefault();
            ArrayList<String> parts = sms.divideMessage(message);
            sms.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
        }
    }
    
    @Override
    public void dismissNotification(Object o) {
        NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Activity.NOTIFICATION_SERVICE);
        if(o != null){
            Integer n = (Integer)o;
            notificationManager.cancel("CN1", n.intValue());
        }else{
            notificationManager.cancelAll();
        }
    }
    
    @Override
    public boolean isNotificationSupported() {
        return true;
    }

    public Object notifyStatusBar(String tickerText, String contentTitle,
            String contentBody, boolean vibrate, boolean flashLights, Hashtable args) {
        int id = getContext().getResources().getIdentifier("icon", "drawable", getContext().getApplicationInfo().packageName);

        NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Activity.NOTIFICATION_SERVICE);
        
        Intent notificationIntent = new Intent();
        notificationIntent.setComponent(getActivity().getComponentName());
        PendingIntent contentIntent = PendingIntent.getActivity(getContext(), 0, notificationIntent, 0);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext())
                .setContentIntent(contentIntent)
                .setSmallIcon(id)
                .setContentTitle(contentTitle)
                .setTicker(tickerText);
        if(flashLights){
            builder.setLights(0, 1000, 1000);
        }
        if(vibrate){
            builder.setVibrate(new long[]{0, 100, 1000});
        }
        if(args != null) {
            Boolean b = (Boolean)args.get("persist");
            if(b != null && b.booleanValue()) {
                builder.setAutoCancel(false);
                builder.setOngoing(true);
            } else {
                builder.setAutoCancel(false);
            }
        } else {
            builder.setAutoCancel(true);
        }
        Notification notification = builder.build();
        int notifyId = 10001;
        notificationManager.notify("CN1", notifyId, notification);
        return new Integer(notifyId);
    }

    public boolean isContactsPermissionGranted() {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            return true;
        }

        if (android.support.v4.content.ContextCompat.checkSelfPermission(getContext(),
                Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }

    
    @Override
    public String[] getAllContacts(boolean withNumbers) {
        if(!checkForPermission(Manifest.permission.READ_CONTACTS, "This is required to get the contacts")){
            return new String[]{};
        }
        return AndroidContactsManager.getInstance().getContacts(getContext(), withNumbers);
    }

    @Override
    public Contact getContactById(String id) {
        if(!checkForPermission(Manifest.permission.READ_CONTACTS, "This is required to get the contacts")){
            return null;
        }
        return AndroidContactsManager.getInstance().getContact(getContext(), id);
    }

    @Override 
    public Contact getContactById(String id, boolean includesFullName, boolean includesPicture, 
            boolean includesNumbers, boolean includesEmail, boolean includeAddress){
        if(!checkForPermission(Manifest.permission.READ_CONTACTS, "This is required to get the contacts")){
            return null;
        }
        return AndroidContactsManager.getInstance().getContact(getContext(), id, includesFullName, includesPicture, 
            includesNumbers, includesEmail, includeAddress);
    }
    
    @Override
    public Contact[] getAllContacts(boolean withNumbers, boolean includesFullName, boolean includesPicture, boolean includesNumbers, boolean includesEmail, boolean includeAddress) {
        if(!checkForPermission(Manifest.permission.READ_CONTACTS, "This is required to get the contacts")){
            return new Contact[]{};
        }
        return AndroidContactsManager.getInstance().getAllContacts(getContext(), withNumbers, includesFullName, includesPicture, includesNumbers, includesEmail, includeAddress);
    }

    @Override
    public boolean isGetAllContactsFast() {
        return true;
    }
    
    public String createContact(String firstName, String surname, String officePhone, String homePhone, String cellPhone, String email) {
        if(!checkForPermission(Manifest.permission.WRITE_CONTACTS, "This is required to create a contact")){
            return null;
        }
         return AndroidContactsManager.getInstance().createContact(getContext(), firstName, surname, officePhone, homePhone, cellPhone, email);
    }

    public boolean deleteContact(String id) {
        if(!checkForPermission(Manifest.permission.WRITE_CONTACTS, "This is required to delete a contact")){
            return false;
        }
        return AndroidContactsManager.getInstance().deleteContact(getContext(), id);
    }
    
    @Override
    public boolean isNativeShareSupported() {
        return true;
    }

    @Override
    public void share(String text, String image, String mimeType, Rectangle sourceRect){   
        /*if(!checkForPermission(Manifest.permission.READ_PHONE_STATE, "This is required to perform share")){
            return;
        }*/
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        if(image == null){
            shareIntent.setType("text/plain");
            shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, text);
        }else{
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(fixAttachmentPath(image)));
            shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        }
        getContext().startActivity(Intent.createChooser(shareIntent, "Share with..."));
    }

    /**
     * @inheritDoc
     */
    public String getPlatformName() {
        return "and";
    }

    /**
     * @inheritDoc
     */
    public String[] getPlatformOverrides() {
        if (isTablet()) {
            return new String[]{"tablet", "android", "android-tab"};
        } else {
            return new String[]{"phone", "android", "android-phone"};
        }
    }
    
    /**
     * @inheritDoc
     */
    public void copyToClipboard(final Object obj) {
        if (getActivity() == null) {
            return;
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int sdk = android.os.Build.VERSION.SDK_INT;
                if (sdk < 11) {
                    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setText(obj.toString());
                } else {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = ClipData.newPlainText("Codename One", obj.toString());
                    clipboard.setPrimaryClip(clip);
                }        
            }
        });
    }

    /**
     * @inheritDoc
     */
    public Object getPasteDataFromClipboard() {
        if (getContext() == null) {
            return null;
        }
        final Object[] response = new Object[1];
        runOnUiThreadAndBlock(new Runnable() {
            @Override
            public void run() {
                int sdk = android.os.Build.VERSION.SDK_INT;
                if (sdk < 11) {
                    android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    response[0] = clipboard.getText().toString();
                } else {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                    response[0] = item.getText();    
                }
            }
        });
        return response[0];
    }
    
    
    

    public class Video extends AndroidImplementation.AndroidPeer implements Media {

        private VideoView nativeVideo;
        private Activity activity;
        private boolean fullScreen = false;
        private Rectangle bounds;
        private boolean nativeController = true;
        private boolean nativePlayer;
        private Form curentForm;

        public Video(final VideoView nativeVideo, final Activity activity, final Runnable onCompletion) {
            super(new RelativeLayout(activity));
            this.nativeVideo = nativeVideo;
            RelativeLayout rl = (RelativeLayout)getNativePeer();

            rl.addView(nativeVideo);
            RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(getWidth(), getHeight());
            layout.addRule(RelativeLayout.CENTER_HORIZONTAL);
            layout.addRule(RelativeLayout.CENTER_VERTICAL);
            rl.setLayoutParams(layout);
            rl.requestLayout();
            
            this.activity = activity;
            if (nativeController) {
                MediaController mc = new AndroidImplementation.CN1MediaController();
                nativeVideo.setMediaController(mc);
            }

            nativeVideo.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer arg0) {
                    if (onCompletion != null) {
                        onCompletion.run();
                    }
                }
            });

            nativeVideo.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    if (onCompletion != null) {
                        onCompletion.run();
                    }
                    return false;
                }
            });

        }
        
        @Override
        public void init() {
            super.init();
            setVisible(true);
        }        
        
        public void prepare() { 
        }

        @Override
        public void play() {
            if (nativePlayer && curentForm == null) {
                curentForm = Display.getInstance().getCurrent();
                Form f = new Form();
                f.setBackCommand(new Command("") {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        Component cmp = getVideoComponent();
                        if(cmp != null) {
                            cmp.remove();
                            pause();
                        }
                        curentForm.showBack();
                        curentForm = null;
                    }
                });
                f.setLayout(new BorderLayout());
                Component cmp = getVideoComponent();
                if(cmp.getParent() != null) {
                    cmp.getParent().removeComponent(cmp);
                }
                f.addComponent(BorderLayout.CENTER, cmp);
                f.show();
            }
            nativeVideo.start();
        }

        @Override
        public void pause() {
            if(nativeVideo != null && nativeVideo.canPause()){
                nativeVideo.pause();
            }
        }

        @Override
        public void cleanup() {
            if(nativeVideo != null) {
                nativeVideo.stopPlayback();
            }
            nativeVideo = null;
            if (nativePlayer && curentForm != null) {
                curentForm.showBack();
                curentForm = null;
            }
        }

        @Override
        public int getTime() {
            if(nativeVideo != null){
                return nativeVideo.getCurrentPosition();
            }
            return -1;
        }

        @Override
        public void setTime(int time) {
            if(nativeVideo != null){
                nativeVideo.seekTo(time);
            }
        }

        @Override
        public int getDuration() {
            if(nativeVideo != null){
                return nativeVideo.getDuration();
            }
            return -1;
        }

        @Override
        public void setVolume(int vol) {
            // float v = ((float) vol) / 100.0F;
            AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            am.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
        }

        @Override
        public int getVolume() {
            AudioManager am = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
            return am.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        @Override
        public boolean isVideo() {
            return true;
        }

        @Override
        public boolean isFullScreen() {
            return fullScreen || nativePlayer;
        }

        @Override
        public void setFullScreen(boolean fullScreen) {
            this.fullScreen = fullScreen;
            if (fullScreen) {
                bounds = new Rectangle(getBounds());
                setX(0);
                setY(0);
                setWidth(Display.getInstance().getDisplayWidth());
                setHeight(Display.getInstance().getDisplayHeight());
            } else {
                if (bounds != null) {
                    setX(bounds.getX());
                    setY(bounds.getY());
                    setWidth(bounds.getSize().getWidth());
                    setHeight(bounds.getSize().getHeight());
                }
            }
            repaint();
        }

        @Override
        public Component getVideoComponent() {
            return this;
        }

        @Override
        protected Dimension calcPreferredSize() {
            if(nativeVideo != null){
                return new Dimension(nativeVideo.getWidth(), nativeVideo.getHeight());
            }
            return new Dimension();
        }
        
        @Override
        public void setWidth(int width) {
            super.setWidth(width);
            if(nativeVideo != null){
                activity.runOnUiThread(new Runnable() {

                    public void run() {
                        RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(getWidth(), getHeight());
                        layout.addRule(RelativeLayout.CENTER_HORIZONTAL);
                        layout.addRule(RelativeLayout.CENTER_VERTICAL);                        
                        nativeVideo.setLayoutParams(layout);
                        nativeVideo.requestLayout();
                        nativeVideo.getHolder().setSizeFromLayout();
                    }
                });
            }
        }

        @Override
        public void setHeight(int height) {
            super.setHeight(height);
            if(nativeVideo != null){
                activity.runOnUiThread(new Runnable() {

                    public void run() {
                        RelativeLayout.LayoutParams layout = new RelativeLayout.LayoutParams(getWidth(), getHeight());
                        layout.addRule(RelativeLayout.CENTER_HORIZONTAL);
                        layout.addRule(RelativeLayout.CENTER_VERTICAL);                        
                        nativeVideo.setLayoutParams(layout);
                        nativeVideo.requestLayout();
                        nativeVideo.getHolder().setSizeFromLayout();
                    }
                });
            }
        }

        @Override
        public void setNativePlayerMode(boolean nativePlayer) {
            this.nativePlayer = nativePlayer;
        }

        @Override
        public boolean isNativePlayerMode() {
            return nativePlayer;
        }

        @Override
        public boolean isPlaying() {
            if(nativeVideo != null){
                return nativeVideo.isPlaying();
            }
            return false;
        }

        public void setVariable(String key, Object value) {
        }

        public Object getVariable(String key) {
            return null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        
        if (requestCode == ZOOZ_PAYMENT) {
            ((IntentResultListener) pur).onActivityResult(requestCode, resultCode, intent);
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAPTURE_IMAGE) {
                try {
                    String imageUri = (String) Storage.getInstance().readObject("imageUri");
                    Vector pathandId = StringUtil.tokenizeString(imageUri, ";");
                    String path = (String)pathandId.get(0);
                    String lastId = (String)pathandId.get(1);                    
                    Storage.getInstance().deleteStorageFile("imageUri");                                        
                    clearMediaDB(lastId, path);
                    callback.fireActionEvent(new ActionEvent(path));
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (requestCode == CAPTURE_VIDEO) {
                String path = (String) Storage.getInstance().readObject("videoUri");
                Storage.getInstance().deleteStorageFile("videoUri");                                        
                callback.fireActionEvent(new ActionEvent(path));
                return;
            } else if (requestCode == CAPTURE_AUDIO) {
                Uri data = intent.getData();
                String path = convertImageUriToFilePath(data, getContext());
                callback.fireActionEvent(new ActionEvent(path));
                return;
            } else if (requestCode == OPEN_GALLERY) {
                Uri selectedImage = intent.getData();
                String scheme = intent.getScheme();
                
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = getContext().getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                
                // this happens on Android devices, not exactly sure what the use case is
                if(cursor == null) {
                    callback.fireActionEvent(null);
                    return;
                }
                
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String filePath = cursor.getString(columnIndex);
                cursor.close();
                
                if (filePath == null && "content".equals(scheme)) {
                    //if the file is not on the filesystem download it and save it 
                    //locally
                    try {
                        InputStream inputStream = getContext().getContentResolver().openInputStream(selectedImage);
                        if (inputStream != null) {
                            String name = getContentName(getContext().getContentResolver(), selectedImage);
                            if (name != null) {
                                filePath = getAppHomePath()
                                        + getFileSystemSeparator() + name;
                                File f = new File(filePath);
                                OutputStream tmp = createFileOuputStream(f);
                                byte[] buffer = new byte[1024];
                                while (inputStream.read(buffer) > 0) {
                                    tmp.write(buffer);
                                }
                                tmp.close();
                                inputStream.close();                                                           
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                callback.fireActionEvent(new ActionEvent(filePath));
                return;
            } else {
                if(callback != null) {
                    callback.fireActionEvent(new ActionEvent("ok"));
                }
                return;
            }
        }
        //clean imageUri
        String imageUri = (String) Storage.getInstance().readObject("imageUri");
        if(imageUri != null){
            Storage.getInstance().deleteStorageFile("imageUri");                                        
        }
        
        if(callback != null) {
            callback.fireActionEvent(null);
        }
    }

    @Override
    public void capturePhoto(ActionListener response) {
        if (getActivity() == null) {
            throw new RuntimeException("Cannot capture photo in background mode");
        }
        if(!checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, "This is required to take a picture")){
            return;
        }
        callback = new EventDispatcher();
        callback.addListener(response);
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

        File newFile = getOutputMediaFile(false);
        Uri imageUri = Uri.fromFile(newFile);

        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageUri);
        
        String lastImageID = getLastImageId();
        Storage.getInstance().writeObject("imageUri", newFile.getAbsolutePath() + ";" + lastImageID);

        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        this.getActivity().startActivityForResult(intent, CAPTURE_IMAGE);
    }

    @Override
    public void captureVideo(ActionListener response) {
        if (getActivity() == null) {
            throw new RuntimeException("Cannot capture video in background mode");
        }
        if(!checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, "This is required to take a video")){
            return;
        }
        callback = new EventDispatcher();
        callback.addListener(response);
        Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
        
        File newFile = getOutputMediaFile(true);
        Uri videoUri = Uri.fromFile(newFile);

        Storage.getInstance().writeObject("videoUri", newFile.getAbsolutePath());

        intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, videoUri);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        this.getActivity().startActivityForResult(intent, CAPTURE_VIDEO);
    }

    public void captureAudio(final ActionListener response) {

        if(!checkForPermission(Manifest.permission.RECORD_AUDIO, "This is required to record the audio")){
            return;
        }
        
        try {
            final Form current = Display.getInstance().getCurrent();

            final File temp = File.createTempFile("mtmp", "dat");
            temp.deleteOnExit();

            if (recorder != null) {
                recorder.release();
            }
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);
            recorder.setOutputFile(temp.getAbsolutePath());

            final Form recording = new Form("Recording");
            recording.setTransitionInAnimator(CommonTransitions.createEmpty());
            recording.setTransitionOutAnimator(CommonTransitions.createEmpty());
            recording.setLayout(new BorderLayout());

            recorder.prepare();
            recorder.start();

            final Label time = new Label("00:00");
            time.getAllStyles().setAlignment(Component.CENTER);
            Font f = Font.createSystemFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE);
            f = f.derive(getDisplayHeight() / 10, Font.STYLE_PLAIN);
            time.getAllStyles().setFont(f);
            recording.addComponent(BorderLayout.CENTER, time);

            recording.registerAnimated(new Animation() {

                long current = System.currentTimeMillis();
                long zero = current;
                int sec = 0;

                public boolean animate() {
                    long now = System.currentTimeMillis();
                    if (now - current > 1000) {
                        current = now;
                        sec++;
                        return true;
                    }
                    return false;
                }

                public void paint(Graphics g) {
                    int seconds = sec % 60;
                    int minutes = sec / 60;

                    String secStr = seconds < 10 ? "0" + seconds : "" + seconds;
                    String minStr = minutes < 10 ? "0" + minutes : "" + minutes;

                    String txt = minStr + ":" + secStr;
                    time.setText(txt);
                }
            });

            Container south = new Container(new com.codename1.ui.layouts.GridLayout(1, 2));
            Command cancel = new Command("Cancel") {

                @Override
                public void actionPerformed(ActionEvent evt) {
                    if (recorder != null) {
                        recorder.stop();
                        recorder.release();
                        recorder = null;
                    }
                    current.showBack();
                    response.actionPerformed(null);
                }

            };
            recording.setBackCommand(cancel);
            south.add(new com.codename1.ui.Button(cancel));
            south.add(new com.codename1.ui.Button(new Command("Save") {

                @Override
                public void actionPerformed(ActionEvent evt) {
                    if (recorder != null) {
                        recorder.stop();
                        recorder.release();
                        recorder = null;
                    }
                    current.showBack();
                    response.actionPerformed(new ActionEvent(temp.getAbsolutePath()));
                }

            }));
            recording.addComponent(BorderLayout.SOUTH, south);
            recording.show();

        } catch (IOException ex) {
            ex.printStackTrace();
            throw new RuntimeException("failed to start audio recording");
        }

    }

    /**
     * Opens the device image gallery
     *
     * @param response callback for the resulting image
     */
    public void openImageGallery(ActionListener response) {
        if (getActivity() == null) {
            throw new RuntimeException("Cannot open image gallery in background mode");
        }
        if(!checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, "This is required to browse the photos")){
            return;
        }
        
        if(editInProgress()) {
            stopEditing(true);
        }
        
        callback = new EventDispatcher();
        callback.addListener(response);
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        this.getActivity().startActivityForResult(galleryIntent, OPEN_GALLERY);
    }
    
    public void openGallery(final ActionListener response, int type){
        if (getActivity() == null) {
            throw new RuntimeException("Cannot open galery in background mode");
        }
        if(!checkForPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, "This is required to browse the photos")){
            return;
        }
        callback = new EventDispatcher();
        callback.addListener(response);
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        if(type == Display.GALLERY_VIDEO){
            galleryIntent.setType("video/*");
        }else if(type == Display.GALLERY_IMAGE){
            galleryIntent.setType("image/*");
        }else if (type == -9999) {
            galleryIntent = new Intent();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                galleryIntent.setAction(Intent.ACTION_OPEN_DOCUMENT);
            } else {
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
            }
            galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);

            // set MIME type for image
            galleryIntent.setType("*/*");
            galleryIntent.putExtra(Intent.EXTRA_MIME_TYPES, Display.getInstance().getProperty("android.openGallery.accept", "*/*").split(","));
        }else{
            galleryIntent.setType("*/*");
        }
        this.getActivity().startActivityForResult(galleryIntent, OPEN_GALLERY);        
    }

    class NativeImage extends Image {

        public NativeImage(Bitmap nativeImage) {
            super(nativeImage);
        }
    }

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile(boolean isVideo) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        if (getActivity() != null) {
            return GetOutputMediaFile.getOutputMediaFile(isVideo, getActivity());
        } else {
            return GetOutputMediaFile.getOutputMediaFile(isVideo, getContext(), "Video");
        }
    }
    
    private static class GetOutputMediaFile {
        
        public static File getOutputMediaFile(boolean isVideo,Activity activity) {
            activity.getComponentName();
            return getOutputMediaFile(isVideo, activity, activity.getTitle());
        }
        
        public static File getOutputMediaFile(boolean isVideo, Context activity, CharSequence title) {
            

            File mediaStorageDir = null;
            if(android.os.Build.VERSION.SDK_INT >= 8) {
                mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                     Environment.DIRECTORY_PICTURES), "" + title);
            } else {
                mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "" + title);
            }            
            
            // This location works best if you want the created images to be shared
            // between applications and persist after your app has been uninstalled.

            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    Log.d(Display.getInstance().getProperty("AppName", "CodenameOne"), "failed to create directory");
                    return null;
                }
            }

            // Create a media file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File mediaFile = null;
            if (!isVideo) {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator
                        + "IMG_" + timeStamp + ".jpg");
            } else {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator
                        + "VID_" + timeStamp + ".mp4");
            }

            return mediaFile;
        }
    }
    
    @Override
    public void systemOut(String content){
        Log.d(Display.getInstance().getProperty("AppName", "CodenameOne"), content);
    }

    private boolean hasAndroidMarket() {
        return hasAndroidMarket(getContext());
    }

    private static final String GooglePlayStorePackageNameOld = "com.google.market";
    private static final String GooglePlayStorePackageNameNew = "com.android.vending";

    /**
     * Indicates whether this is a Google certified device which means that it
     * has Android market etc.
     */
    public static boolean hasAndroidMarket(Context activity) {
        final PackageManager packageManager = activity.getPackageManager();
        List<PackageInfo> packages = packageManager.getInstalledPackages(PackageManager.GET_UNINSTALLED_PACKAGES);
        for (PackageInfo packageInfo : packages) {
            if (packageInfo.packageName.equals(GooglePlayStorePackageNameOld) ||
                packageInfo.packageName.equals(GooglePlayStorePackageNameNew)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void registerPush(Hashtable metaData, boolean noFallback) {
        if (getActivity() == null) {
            return;
        }
        boolean has = hasAndroidMarket();
        if (noFallback && !has) {
            Log.d("Codename One", "Device doesn't have Android market/google play can't register for push!");
            return;
        }
        String id = (String)metaData.get(com.codename1.push.Push.GOOGLE_PUSH_KEY);
        if(has) {
            Log.d("Codename One", "Sending async push request for id: " + id);
            ((CodenameOneActivity) getActivity()).registerForPush(id);
        } else {
            PushNotificationService.forceStartService(getActivity().getPackageName() + ".PushNotificationService", getActivity());
            if(!registerServerPush(id, getApplicationKey(), (byte)10, "", getPackageName())) {
                sendPushRegistrationError("Server registration error", 1);
            } 
        }
    }

    public static void stopPollingLoop() {
        stopPolling();
    }

    public static void registerPolling() {
        registerPollingFallback();
    }

    @Override
    public void deregisterPush() {
        boolean has = hasAndroidMarket();
        if (has) {
            ((CodenameOneActivity) getActivity()).stopReceivingPush();
            deregisterPushFromServer();
        } else {
            super.deregisterPush();
        }
    }

    private static String convertImageUriToFilePath(Uri imageUri, Context activity) {
        Cursor cursor = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        cursor = activity.getContentResolver().query(imageUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String path = cursor.getString(column_index);
        cursor.close();
        return path;
    }

    class CN1MediaController extends MediaController {

        public CN1MediaController() {
            super(getActivity());
        }

        @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            int keycode = event.getKeyCode();
            keycode = CodenameOneView.internalKeyCodeTranslate(keycode);
            if (keycode == AndroidImplementation.DROID_IMPL_KEY_BACK) {
                Display.getInstance().keyPressed(keycode);
                Display.getInstance().keyReleased(keycode);
                return true;
            } else {
                return super.dispatchKeyEvent(event);
            }
        }
    }
    private L10NManager l10n;

    /**
     * @inheritDoc
     */
    public L10NManager getLocalizationManager() {
        if (l10n == null) {
            Locale l = Locale.getDefault();
            l10n = new L10NManager(l.getLanguage(), l.getCountry()) {
                public double parseDouble(String localeFormattedDecimal) {
                    try {
                        return NumberFormat.getNumberInstance().parse(localeFormattedDecimal).doubleValue();
                    } catch (ParseException err) {
                        return Double.parseDouble(localeFormattedDecimal);
                    }
                }
                
                public String format(int number) {
                    return NumberFormat.getNumberInstance().format(number);
                }

                public String format(double number) {
                    return NumberFormat.getNumberInstance().format(number);
                }

                public String formatCurrency(double currency) {
                    return NumberFormat.getCurrencyInstance().format(currency);
                }

                public String formatDateLongStyle(Date d) {
                    return DateFormat.getDateInstance(DateFormat.LONG).format(d);
                }

                public String formatDateShortStyle(Date d) {
                    return DateFormat.getDateInstance(DateFormat.SHORT).format(d);
                }

                public String formatDateTime(Date d) {
                    return DateFormat.getDateTimeInstance().format(d);
                }

                public String formatDateTimeMedium(Date d) {
                    DateFormat dd = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
                    return dd.format(d);
                }

                public String formatDateTimeShort(Date d) {
                    DateFormat dd = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
                    return dd.format(d);
                }

                public String getCurrencySymbol() {
                    return NumberFormat.getInstance().getCurrency().getSymbol();
                }

                public void setLocale(String locale, String language) {
                    super.setLocale(locale, language);
                    Locale l = new Locale(language, locale);
                    Locale.setDefault(l);
                }
            };
        }
        return l10n;
    }
    private com.codename1.ui.util.ImageIO imIO;

    @Override
    public com.codename1.ui.util.ImageIO getImageIO() {
        if (imIO == null) {
            imIO = new com.codename1.ui.util.ImageIO() {
                @Override
                public Dimension getImageSize(String imageFilePath) throws IOException {
                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inJustDecodeBounds = true;
                    o.inPreferredConfig = Bitmap.Config.ARGB_8888;

                    InputStream fis = createFileInputStream(imageFilePath);
                    BitmapFactory.decodeStream(fis, null, o);
                    fis.close();

                    ExifInterface exif = new ExifInterface(imageFilePath);               
                    
                    // if the image is in portrait mode
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    if(orientation == ExifInterface.ORIENTATION_ROTATE_90 || orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                        return new Dimension(o.outHeight, o.outWidth);
                    }
                    return new Dimension(o.outWidth, o.outHeight);
                }
                
                private Dimension getImageSizeNoRotation(String imageFilePath) throws IOException {
                    BitmapFactory.Options o = new BitmapFactory.Options();
                    o.inJustDecodeBounds = true;
                    o.inPreferredConfig = Bitmap.Config.ARGB_8888;

                    InputStream fis = createFileInputStream(imageFilePath);
                    BitmapFactory.decodeStream(fis, null, o);
                    fis.close();

                    return new Dimension(o.outWidth, o.outHeight);
                }
                
                @Override
                public void save(InputStream image, OutputStream response, String format, int width, int height, float quality) throws IOException {
                    Bitmap.CompressFormat f = Bitmap.CompressFormat.PNG;
                    if (format == FORMAT_JPEG) {
                        f = Bitmap.CompressFormat.JPEG;
                    }
                    Image img = Image.createImage(image).scaled(width, height);
                    Bitmap b = (Bitmap) img.getImage();
                    b.compress(f, (int) (quality * 100), response);
                }

                @Override
                public String saveAndKeepAspect(String imageFilePath, String preferredOutputPath, String format, int width, int height, float quality, boolean onlyDownscale, boolean scaleToFill) throws IOException{
                    ExifInterface exif = new ExifInterface(imageFilePath);               
                    Dimension d = getImageSizeNoRotation(imageFilePath);
                    if(onlyDownscale) {
                        if(scaleToFill) {
                            if(d.getHeight() <= height || d.getWidth() <= width) {
                                return imageFilePath;
                            }
                        } else {
                            if(d.getHeight() <= height && d.getWidth() <= width) {
                                return imageFilePath;
                            }
                        }
                    }

                    float ratio = ((float)d.getWidth()) / ((float)d.getHeight());
                    int heightBasedOnWidth = (int)(((float)width) / ratio);
                    int widthBasedOnHeight = (int)(((float)height) * ratio);
                    if(scaleToFill) {
                        if(heightBasedOnWidth >= width) {
                            height = heightBasedOnWidth;
                        } else {
                            width = widthBasedOnHeight;
                        }
                    } else {
                        if(heightBasedOnWidth > width) {
                            width = widthBasedOnHeight;
                        } else {
                            height = heightBasedOnWidth;
                        }
                    }
                    sampleSizeOverride = Math.max(d.getWidth()/width, d.getHeight()/height);
                    OutputStream im = FileSystemStorage.getInstance().openOutputStream(preferredOutputPath);
                    Image i = Image.createImage(imageFilePath);
                    Image newImage = i.scaled(width, height);
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                    int angle = 0;
                    switch (orientation) {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            angle = 90;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            angle = 180;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            angle = 270;
                            break;
                    }
                    if (angle != 0) {
                        Matrix mat = new Matrix();
                        mat.postRotate(angle);
                        Bitmap b = (Bitmap)newImage.getImage();
                        Bitmap correctBmp = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), mat, true);
                        b.recycle();
                        newImage.dispose();
                        Image tmp = Image.createImage(correctBmp);
                        newImage = tmp;
                        save(tmp, im, format, quality);
                    } else {
                        save(imageFilePath, im, format, width, height, quality);
                    }
                    sampleSizeOverride =  -1;
                    return preferredOutputPath;
                }

                @Override
                public void save(String imageFilePath, OutputStream response, String format, int width, int height, float quality) throws IOException {
                    Image i = Image.createImage(imageFilePath);
                    Image newImage = i.scaled(width, height);
                    save(newImage, response, format, quality);
                    newImage.dispose();
                    i.dispose();
                } 

                @Override
                protected void saveImage(Image img, OutputStream response, String format, float quality) throws IOException {
                    Bitmap.CompressFormat f = Bitmap.CompressFormat.PNG;
                    if (format == FORMAT_JPEG) {
                        f = Bitmap.CompressFormat.JPEG;
                    }
                    Bitmap b = (Bitmap) img.getImage();
                    b.compress(f, (int) (quality * 100), response);
                }

                @Override
                public boolean isFormatSupported(String format) {
                    return format == FORMAT_JPEG || format == FORMAT_PNG;
                }
            };
        }
        return imIO;
    }

    @Override
    public Database openOrCreateDB(String databaseName) throws IOException {
        SQLiteDatabase db = getContext().openOrCreateDatabase(databaseName, getContext().MODE_PRIVATE, null);        
        return new AndroidDB(db);
    }

    @Override
    public void deleteDB(String databaseName) throws IOException {
        getContext().deleteDatabase(databaseName);
    }

    @Override
    public boolean existsDB(String databaseName) {
        File db = new File(getContext().getApplicationInfo().dataDir + "/databases/" + databaseName);
        return db.exists();
    }

    public String getDatabasePath(String databaseName) {
        File db = new File(getContext().getApplicationInfo().dataDir + "/databases/" + databaseName);
        return db.getAbsolutePath();
    }
    
    public boolean isNativeTitle() {
        Form f = getCurrentForm();
        boolean nativeCommand;
        if(f != null){
            nativeCommand = f.getMenuBar().getCommandBehavior() == Display.COMMAND_BEHAVIOR_NATIVE;
        }else{
            nativeCommand = getCommandBehavior() == Display.COMMAND_BEHAVIOR_NATIVE;
        }
        return hasActionBar() && nativeCommand;
    }

    public void refreshNativeTitle(){
        if (getActivity() == null) {
            return;
        }
        Form f = getCurrentForm();
        if (f != null && isNativeTitle() &&  !(f instanceof Dialog)) {
            getActivity().runOnUiThread(new SetCurrentFormImpl(getActivity(), f));
        }
    }
    
    public void setCurrentForm(final Form f) {
        if (getActivity() == null) {
            return;
        }
        if(getCurrentForm() == null){
            flushGraphics();
        }
        if(editInProgress()) {
            stopEditing(true);
        }
        super.setCurrentForm(f);
        if (isNativeTitle() &&  !(f instanceof Dialog)) {
            getActivity().runOnUiThread(new SetCurrentFormImpl(getActivity(), f));
        }
    }

    @Override
    public void setNativeCommands(Vector commands) {
        refreshNativeTitle();
    }
    
    @Override
    public boolean isScreenLockSupported() {
        return true;
    }
    
    @Override
    public void lockScreen(){
        ((CodenameOneActivity)getContext()).lockScreen();
    }
    
    @Override
    public void unlockScreen(){
        ((CodenameOneActivity)getContext()).unlockScreen();
    }
    
    private static class SetCurrentFormImpl implements Runnable {
        private Activity activity;
        private Form f;
        
        public SetCurrentFormImpl(Activity activity, Form f) {
            this.activity = activity;
            this.f = f;
        }

        @Override
        public void run() {
            ActionBar ab = activity.getActionBar();
            String title = f.getTitle();
            boolean hasMenuBtn = false;
            if(android.os.Build.VERSION.SDK_INT >= 14){
                try {
                    ViewConfiguration vc = ViewConfiguration.get(activity);
                    Method m = vc.getClass().getMethod("hasPermanentMenuKey", (Class[])null);
                    hasMenuBtn = ((Boolean)m.invoke(vc, (Object[])null)).booleanValue();
                } catch(Throwable t) {
                    t.printStackTrace();
                }
            }
            if((title != null && title.length() > 0) || (f.getCommandCount() > 0 && !hasMenuBtn)){
                activity.runOnUiThread(new NotifyActionBar(activity, true));
            }else{
                activity.runOnUiThread(new NotifyActionBar(activity, false));
                return;
            }

            ab.setTitle(title);
            ab.setDisplayHomeAsUpEnabled(f.getBackCommand() != null);
            if(android.os.Build.VERSION.SDK_INT >= 14){
                Image icon = f.getTitleComponent().getIcon();
                try {
                    if(icon != null){
                        ab.getClass().getMethod("setIcon", Drawable.class).invoke(ab, new BitmapDrawable(activity.getResources(), (Bitmap)icon.getImage()));
                    }else{
                        if(activity.getApplicationInfo().icon != 0){
                            ab.getClass().getMethod("setIcon", Integer.TYPE).invoke(ab, activity.getApplicationInfo().icon);
                        }
                    }
                    activity.runOnUiThread(new InvalidateOptionsMenuImpl(activity));
                } catch(Throwable t) {
                    t.printStackTrace();
                }
            }
            return;
        }
        
    }
    
    private Purchase pur;
    
    @Override
    public Purchase getInAppPurchase() {
        try {
            pur = ZoozPurchase.class.newInstance();
            return pur;
        } catch(Throwable t) {
            return super.getInAppPurchase();
        }
    }

    @Override
    public boolean isTimeoutSupported() {
        return true;
    }

    @Override
    public void setTimeout(int t) {
        timeout = t;
    }

    @Override
    public CodeScanner getCodeScanner() {
        if(scannerInstance == null) {
            scannerInstance = new CodeScannerImpl();
        }
        return scannerInstance;
    }

    public void addCookie(Cookie c, boolean addToWebViewCookieManager, boolean sync) {
        if(addToWebViewCookieManager) {
            CookieManager mgr;
            CookieSyncManager syncer;
            try {
                syncer = CookieSyncManager.getInstance();
                mgr = CookieManager.getInstance();
            } catch(IllegalStateException ex) {
                syncer = CookieSyncManager.createInstance(this.getContext());
                mgr = CookieManager.getInstance();
            }
            String cookieString = c.getName()+"="+c.getValue()+
                    "; Domain="+c.getDomain()+
                    "; Path="+c.getPath()+
                    "; "+(c.isSecure()?"Secure;":"")
                    +(c.isHttpOnly()?"httpOnly;":"");
            mgr.setCookie("http"+
                    (c.isSecure()?"s":"")+"://"+
                    c.getDomain()+
                    c.getPath(), cookieString);
            if(sync) {
                syncer.sync();
            }
        }
        super.addCookie(c);
            
        
        
    }

    public void addCookie(Cookie[] cs, boolean addToWebViewCookieManager, boolean sync) {
        if(addToWebViewCookieManager) {
            CookieManager mgr;
            CookieSyncManager syncer;
            try {
                syncer = CookieSyncManager.getInstance();
                mgr = CookieManager.getInstance();
            } catch(IllegalStateException ex) {
                syncer = CookieSyncManager.createInstance(this.getContext());
                mgr = CookieManager.getInstance();
            }
            for (Cookie c : cs) {
                String cookieString = c.getName() + "=" + c.getValue() +
                        "; Domain=" + c.getDomain() +
                        "; Path=" + c.getPath() +
                        "; " + (c.isSecure() ? "Secure;" : "")
                        + (c.isHttpOnly() ? "httpOnly;" : "");
                mgr.setCookie("http" +
                        (c.isSecure() ? "s" : "") + "://" +
                        c.getDomain() +
                        c.getPath(), cookieString);
            }
            if(sync) {
                syncer.sync();
            }
        }
        super.addCookie(cs);



    }

    @Override
    public void addCookie(Cookie c) {
        if(isUseNativeCookieStore()) {
            this.addCookie(c, true, true);
        } else {
            super.addCookie(c);
        }
    }
    
    

    @Override
    public void addCookie(Cookie[] cookiesArray) {
        if(isUseNativeCookieStore()) {
            this.addCookie(cookiesArray, true);
        } else {
            super.addCookie(cookiesArray);
        }
    }
    
    public void addCookie(Cookie[] cookiesArray, boolean addToWebViewCookieManager){
        addCookie(cookiesArray, addToWebViewCookieManager, false);

    }
    
    
    
    class CodeScannerImpl extends CodeScanner implements IntentResultListener {
        private ScanResult callback;
        
        @Override
        public void scanQRCode(ScanResult callback) {
            if (getActivity() == null) {
                return;
            }
            if (getActivity() instanceof CodenameOneActivity) {
                ((CodenameOneActivity) getActivity()).setIntentResultListener(this);
            }
            this.callback = callback;
            IntentIntegrator in = new IntentIntegrator(getActivity());
            if(!in.initiateScan(IntentIntegrator.QR_CODE_TYPES, "QR_CODE_MODE")){
                // restore old activity handling
                 Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            if(CodeScannerImpl.this != null && CodeScannerImpl.this.callback != null) {
                                CodeScannerImpl.this.callback.scanError(-1, "no scan app");
                                CodeScannerImpl.this.callback = null;
                            }
                        }
                    });
                 
                if (getActivity() instanceof CodenameOneActivity) {
                    ((CodenameOneActivity) getActivity()).restoreIntentResultListener();
                }
            }
        }

        @Override
        public void scanBarCode(ScanResult callback) {
            if (getActivity() == null) {
                return;
            }
            if (getActivity() instanceof CodenameOneActivity) {
                ((CodenameOneActivity) getActivity()).setIntentResultListener(this);
            }
            this.callback = callback;
            IntentIntegrator in = new IntentIntegrator(getActivity());
            Collection<String> types = IntentIntegrator.PRODUCT_CODE_TYPES;
            if(Display.getInstance().getProperty("scanAllCodeTypes", "false").equals("true")) {
                types = IntentIntegrator.ALL_CODE_TYPES;
            } 
            if(Display.getInstance().getProperty("android.scanTypes", null) != null) {
                String[] arr = Display.getInstance().getProperty("android.scanTypes", null).split(";");
                types = Arrays.asList(arr);
            } 
            
            if(!in.initiateScan(types, "ONE_D_MODE")){
                // restore old activity handling
                 Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            CodeScannerImpl.this.callback.scanError(-1, "no scan app");
                            CodeScannerImpl.this.callback = null;
                        }
                    });

                if (getActivity() instanceof CodenameOneActivity) {
                    ((CodenameOneActivity) getActivity()).restoreIntentResultListener();
                }
            }
        }

        public void onActivityResult(int requestCode, final int resultCode, Intent data) {
            if (requestCode == IntentIntegrator.REQUEST_CODE && callback != null) {
                final ScanResult sr = callback;
                if (resultCode == Activity.RESULT_OK) {
                    final String contents = data.getStringExtra("SCAN_RESULT");
                    final String formatName = data.getStringExtra("SCAN_RESULT_FORMAT");
                    final byte[] rawBytes = data.getByteArrayExtra("SCAN_RESULT_BYTES");
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            sr.scanCompleted(contents, formatName, rawBytes);
                        }
                    });
                } else if(resultCode == Activity.RESULT_CANCELED) {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            sr.scanCanceled();
                        }
                    });
                
                } else {
                    Display.getInstance().callSerially(new Runnable() {
                        @Override
                        public void run() {
                            sr.scanError(resultCode, null);
                        }
                    });
                }
                callback = null;
            }
            
            // restore old activity handling
            if (getActivity() instanceof CodenameOneActivity) {
                ((CodenameOneActivity) getActivity()).restoreIntentResultListener();
            }
        }
    }
    
    public boolean hasCamera() {
        try {
            int numCameras = Camera.getNumberOfCameras();
            return numCameras > 0;
        } catch(Throwable t) {
            return true;
        }
    }

    public String getCurrentAccessPoint() {

        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info == null) {
            return null;
        }
        String apName = info.getTypeName() + "_" + info.getSubtypeName();
        if (info.getExtraInfo() != null) {
            apName += "_" + info.getExtraInfo();
        }
        return apName;
    }

    /**
     * @inheritDoc
     */
    public String[] getAPIds() {
        if (apIds == null) {
            apIds = new HashMap();
            NetworkInfo[] aps = ((ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE)).getAllNetworkInfo();
            for (int i = 0; i < aps.length; i++) {
                String apName = aps[i].getTypeName() + "_" + aps[i].getSubtypeName();
                if (aps[i].getExtraInfo() != null) {
                    apName += "_" + aps[i].getExtraInfo();
                }
                apIds.put(apName, aps[i]);
            }
        }
        if (apIds.isEmpty()) {
            return null;
        }
        String[] ret = new String[apIds.size()];
        Iterator iter = apIds.keySet().iterator();
        for (int i = 0; iter.hasNext(); i++) {
            ret[i] = iter.next().toString();
        }
        return ret;

    }

    /**
     * @inheritDoc
     */
    public int getAPType(String id) {
        if (apIds == null) {
            getAPIds();
        }
        NetworkInfo info = (NetworkInfo) apIds.get(id);
        if (info == null) {
            return NetworkManager.ACCESS_POINT_TYPE_UNKNOWN;
        }
        int type = info.getType();
        int subType = info.getSubtype();
        if (type == ConnectivityManager.TYPE_WIFI) {
            return NetworkManager.ACCESS_POINT_TYPE_WLAN;
        } else if (type == ConnectivityManager.TYPE_MOBILE) {
            switch (subType) {
                case TelephonyManager.NETWORK_TYPE_1xRTT:
                    return NetworkManager.ACCESS_POINT_TYPE_NETWORK2G; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_CDMA:
                    return NetworkManager.ACCESS_POINT_TYPE_NETWORK2G; // ~ 14-64 kbps
                case TelephonyManager.NETWORK_TYPE_EDGE:
                    return NetworkManager.ACCESS_POINT_TYPE_NETWORK2G; // ~ 50-100 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_0:
                    return NetworkManager.ACCESS_POINT_TYPE_NETWORK3G; // ~ 400-1000 kbps
                case TelephonyManager.NETWORK_TYPE_EVDO_A:
                    return NetworkManager.ACCESS_POINT_TYPE_NETWORK3G; // ~ 600-1400 kbps
                case TelephonyManager.NETWORK_TYPE_GPRS:
                    return NetworkManager.ACCESS_POINT_TYPE_NETWORK2G; // ~ 100 kbps
                case TelephonyManager.NETWORK_TYPE_HSDPA:
                    return NetworkManager.ACCESS_POINT_TYPE_NETWORK3G; // ~ 2-14 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPA:
                    return NetworkManager.ACCESS_POINT_TYPE_NETWORK3G; // ~ 700-1700 kbps
                case TelephonyManager.NETWORK_TYPE_HSUPA:
                    return NetworkManager.ACCESS_POINT_TYPE_NETWORK3G; // ~ 1-23 Mbps
                case TelephonyManager.NETWORK_TYPE_UMTS:
                    return NetworkManager.ACCESS_POINT_TYPE_NETWORK3G; // ~ 400-7000 kbps
            /*
                 * Above API level 7, make sure to set android:targetSdkVersion
                 * to appropriate level to use these
                 */
                case TelephonyManager.NETWORK_TYPE_EHRPD: // API level 11
                    return NetworkManager.ACCESS_POINT_TYPE_NETWORK3G; // ~ 1-2 Mbps
                case TelephonyManager.NETWORK_TYPE_EVDO_B: // API level 9
                    return NetworkManager.ACCESS_POINT_TYPE_NETWORK3G; // ~ 5 Mbps
                case TelephonyManager.NETWORK_TYPE_HSPAP: // API level 13
                    return NetworkManager.ACCESS_POINT_TYPE_NETWORK3G; // ~ 10-20 Mbps
                case TelephonyManager.NETWORK_TYPE_IDEN: // API level 8
                    return NetworkManager.ACCESS_POINT_TYPE_NETWORK2G; // ~25 kbps
                case TelephonyManager.NETWORK_TYPE_LTE: // API level 11
                    return NetworkManager.ACCESS_POINT_TYPE_NETWORK3G; // ~ 10+ Mbps
                // Unknown
                case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                default:
                    return NetworkManager.ACCESS_POINT_TYPE_NETWORK2G;
            }
        } else {
            return NetworkManager.ACCESS_POINT_TYPE_UNKNOWN;
        }
    }
   
    /**
     * @inheritDoc
     */
    public void setCurrentAccessPoint(String id) {

        if (apIds == null) {
            getAPIds();
        }
        NetworkInfo info = (NetworkInfo) apIds.get(id);
        if (info == null || info.isConnectedOrConnecting()) {
            return;

        }
        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        cm.setNetworkPreference(info.getType());
    }
    
    private void scanMedia(File file) {
        Uri uri = Uri.fromFile(file);
        Intent scanFileIntent = new Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
        getActivity().sendBroadcast(scanFileIntent);
    }
    
    /**
     * Gets the last image id from the media store
     *
     * @return
     */
    private String getLastImageId() {
        int idVal = 0;;
        final String[] imageColumns = {MediaStore.Images.Media._ID};
        final String imageOrderBy = MediaStore.Images.Media._ID + " DESC";
        final String imageWhere = null;
        final String[] imageArguments = null;
        Cursor imageCursor = getContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageColumns, imageWhere, imageArguments, imageOrderBy);
        if (imageCursor.moveToFirst()) {
            int id = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.Media._ID));
            imageCursor.close();
            idVal = id;
        } 
        return "" + idVal;
    }
    
    private void clearMediaDB(String lastId, String capturePath) {
        final String[] imageColumns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_TAKEN, MediaStore.Images.Media.SIZE, MediaStore.Images.Media._ID};
        final String imageOrderBy = MediaStore.Images.Media._ID + " DESC";
        final String imageWhere = MediaStore.Images.Media._ID + ">?";
        final String[] imageArguments = {lastId};
        Cursor imageCursor = getContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageColumns, imageWhere, imageArguments, imageOrderBy);
        if (imageCursor.getCount() > 1) {
            while (imageCursor.moveToNext()) {
                int id = imageCursor.getInt(imageCursor.getColumnIndex(MediaStore.Images.Media._ID));
                String path = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.Images.Media.DATA));
                Long takenTimeStamp = imageCursor.getLong(imageCursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN));
                Long size = imageCursor.getLong(imageCursor.getColumnIndex(MediaStore.Images.Media.SIZE));
                if (path.contentEquals(capturePath)) {
                    // Remove it
                    ContentResolver cr = getContext().getContentResolver();
                    cr.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media._ID + "=?", new String[]{Long.toString(id)});
                    break;
                }
            }
        }
        imageCursor.close();
    }
    
    
    @Override
    public boolean isNativePickerTypeSupported(int pickerType) {
        if(android.os.Build.VERSION.SDK_INT >= 11) {
            return pickerType == Display.PICKER_TYPE_DATE || pickerType == Display.PICKER_TYPE_TIME || pickerType == Display.PICKER_TYPE_STRINGS;
        }
        return pickerType == Display.PICKER_TYPE_DATE || pickerType == Display.PICKER_TYPE_TIME;
    }
    
    @Override
    public Object showNativePicker(final int type, final Component source, final Object currentValue, final Object data) {
        if (getActivity() == null) {
            return null;
        }
        final boolean [] canceled = new boolean[1];
        final boolean [] dismissed = new boolean[1];
        
        if(editInProgress()) {
            stopEditing(true);
        }
        if(type == Display.PICKER_TYPE_TIME) {
            
            class TimePick implements TimePickerDialog.OnTimeSetListener, TimePickerDialog.OnCancelListener, Runnable {
                int result = ((Integer)currentValue).intValue();
                public void onTimeSet(TimePicker tp, int hour, int minute) {
                    result = hour * 60 + minute;
                    dismissed[0] = true;
                    synchronized(this) {
                        notify();
                    }
                }
                
                public void run() {
                    while(!dismissed[0]) {
                        synchronized(this) {
                            try {
                                wait(50);
                            } catch(InterruptedException er) {}
                        }
                    }
                }

                @Override
                public void onCancel(DialogInterface di) {
                    dismissed[0] = true;
                    canceled[0] = true;
                    synchronized (this) {
                        notify();
                    }
                }
            }
            final TimePick pickInstance = new TimePick();
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    int hour = ((Integer)currentValue).intValue() / 60;
                    int minute = ((Integer)currentValue).intValue() % 60;
                    TimePickerDialog tp = new TimePickerDialog(getActivity(), pickInstance, hour, minute, true){

                        @Override
                        public void cancel() {
                            super.cancel();
                            dismissed[0] = true;
                            canceled[0] = true;
                        }

                        @Override
                        public void dismiss() {
                            super.dismiss();
                            dismissed[0] = true;
                        }
                    
                    };
                    tp.setOnCancelListener(pickInstance);
                        //DateFormat.is24HourFormat(activity));
                    tp.show();
                }
            });
            Display.getInstance().invokeAndBlock(pickInstance);
            if(canceled[0]) {
                return null;
            }
            return new Integer(pickInstance.result);
        }
        if(type == Display.PICKER_TYPE_DATE) {
            final java.util.Calendar cl = java.util.Calendar.getInstance();
            cl.setTime((Date)currentValue);
            class DatePick implements DatePickerDialog.OnDateSetListener,DatePickerDialog.OnCancelListener, Runnable {
                Date result = (Date)currentValue;
                
                public void onDateSet(DatePicker dp, int year, int month, int day) {
                    java.util.Calendar c = java.util.Calendar.getInstance();
                    c.set(java.util.Calendar.YEAR, year);
                    c.set(java.util.Calendar.MONTH, month);
                    c.set(java.util.Calendar.DAY_OF_MONTH, day);
                    result = c.getTime();
                    dismissed[0] = true;
                    synchronized(this) {
                        notify();
                    }                    
                }
                
                public void run() {
                    while(!dismissed[0]) {
                        synchronized(this) {
                            try {
                                wait(50);
                            } catch(InterruptedException er) {}
                        }
                    }
                }

                public void onCancel(DialogInterface di) {
                    result = null;
                    dismissed[0] = true;
                    canceled[0] = true;
                    synchronized(this) {
                        notify();
                    }
                }
            }
            final DatePick pickInstance = new DatePick();
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    DatePickerDialog tp = new DatePickerDialog(getActivity(), pickInstance, cl.get(java.util.Calendar.YEAR), cl.get(java.util.Calendar.MONTH), cl.get(java.util.Calendar.DAY_OF_MONTH)){

                        @Override
                        public void cancel() {
                            super.cancel();
                            dismissed[0] = true;
                            canceled[0] = true;
                        }

                        @Override
                        public void dismiss() {
                            super.dismiss();
                            dismissed[0] = true;
                        }
                        
                    };
                    tp.setOnCancelListener(pickInstance);
                    tp.show();
                }
            });
            Display.getInstance().invokeAndBlock(pickInstance);
            return pickInstance.result;
        }
        if(type == Display.PICKER_TYPE_STRINGS) {
            final String[] values = (String[])data;
            class StringPick implements Runnable, NumberPicker.OnValueChangeListener {
                int result = -1;
                
                StringPick() {
                }
                
                public void run() {
                    while(!dismissed[0]) {
                        synchronized(this) {
                            try {
                                wait(50);
                            } catch(InterruptedException er) {}
                        }
                    }
                }

                public void cancel() {
                    dismissed[0] = true;
                    canceled[0] = true;
                    synchronized(this) {
                        notify();
                    }
                }

                public void ok() {
                    canceled[0] = false;
                    dismissed[0] = true;
                    synchronized(this) {
                        notify();
                    }
                }

                @Override
                public void onValueChange(NumberPicker np, int oldVal, int newVal) {
                    result = newVal;
                }
            }
            
            final StringPick pickInstance = new StringPick();
            for(int iter = 0 ; iter < values.length ; iter++) {
                if(values[iter].equals(currentValue)) {
                    pickInstance.result = iter;
                    break;
                }
            }

            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    NumberPicker picker = new NumberPicker(getActivity());
                    if(source.getClientProperty("showKeyboard") == null) {
                        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
                    }
                    picker.setMinValue(0);
                    picker.setMaxValue(values.length - 1);
                    picker.setDisplayedValues(values);
                    picker.setOnValueChangedListener(pickInstance);
                    if(pickInstance.result > -1) {
                        picker.setValue(pickInstance.result);
                    }
                    RelativeLayout linearLayout = new RelativeLayout(getActivity());
                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(50, 50);
                    RelativeLayout.LayoutParams numPicerParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    numPicerParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

                    linearLayout.setLayoutParams(params);
                    linearLayout.addView(picker,numPicerParams);

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                    alertDialogBuilder.setView(linearLayout);
                    alertDialogBuilder
                            .setCancelable(false)
                            .setPositiveButton("Ok",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int id) {
                                            pickInstance.ok();
                                        }
                                    })
                            .setNegativeButton("Cancel",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog,
                                                            int id) {
                                            dialog.cancel();
                                            pickInstance.cancel();
                                        }
                                    });
                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            });
            Display.getInstance().invokeAndBlock(pickInstance);
            if(canceled[0]) {
                return null;
            }
            if(pickInstance.result < 0) {
                return null;
            }
            return values[pickInstance.result];
        }
        return null;
    }

    class SocketImpl {
        java.net.Socket socketInstance;
        int errorCode = -1;
        String errorMessage = null;
        InputStream is;
        OutputStream os;

        public boolean connect(String param, int param1) {
            try {
                socketInstance = new java.net.Socket(param, param1);
                return true;
            } catch(Exception err) {
                err.printStackTrace();
                errorMessage = err.toString();
                return false;
            }
        }

        private InputStream getInput() throws IOException {
            if(is == null) {
                if(socketInstance != null) {
                    is = socketInstance.getInputStream();
                } else {

                }
            }
            return is;
        }

        private OutputStream getOutput() throws IOException {
            if(os == null) {
                os = socketInstance.getOutputStream();
            }
            return os;
        }

        public int getAvailableInput() {
            try {
                return getInput().available();
            } catch(IOException err) {
                errorMessage = err.toString();
                err.printStackTrace();
            }
            return 0;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getIP() {
            try {
                return java.net.InetAddress.getLocalHost().getHostAddress();
            } catch(Throwable t) {
                t.printStackTrace();
                errorMessage = t.toString();
                return t.getMessage();
            }
        }

        public byte[] readFromStream() {
            try {
                int av = getAvailableInput();
                if(av > 0) {
                    byte[] arr = new byte[av];
                    int size = getInput().read(arr);
                    if(size == arr.length) {
                        return arr;
                    }
                    return shrink(arr, size);
                }
                byte[] arr = new byte[8192];
                int size = getInput().read(arr);
                if(size == arr.length) {
                    return arr;
                }
                return shrink(arr, size);
            } catch(IOException err) {
                err.printStackTrace();
                errorMessage = err.toString();
                return null;
            }
        }

        private byte[] shrink(byte[] arr, int size) {
            if(size == -1) {
                return null;
            }
            byte[] n = new byte[size];
            System.arraycopy(arr, 0, n, 0, size);
            return n;
        }

        public void writeToStream(byte[] param) {
            try {
                OutputStream os = getOutput();
                os.write(param);
                os.flush();
            } catch(IOException err) {
                errorMessage = err.toString();
                err.printStackTrace();
            }
        }

        public void disconnect() {
            try {
                if(socketInstance != null) {
                    if(is != null) {
                        try {
                            is.close();
                        } catch(IOException err) {}
                    }
                    if(os != null) {
                        try {
                            os.close();
                        } catch(IOException err) {}
                    }
                    socketInstance.close();
                    socketInstance = null;
                }
            } catch(IOException err) {
                errorMessage = err.toString();
                err.printStackTrace();
            }
        }

        public Object listen(int param) {
            try {
                ServerSocket serverSocketInstance = new ServerSocket(param);
                socketInstance = serverSocketInstance.accept();
                return socketInstance;
            } catch(Exception err) {
                errorMessage = err.toString();
                err.printStackTrace();
                return null;
            }
        }

        public boolean isConnected() {
            return socketInstance != null;
        }

        public int getErrorCode() {
            return errorCode;
        }
    }
    
    @Override
    public Object connectSocket(String host, int port) {
        SocketImpl i = new SocketImpl();
        if(i.connect(host, port)) {
            return i;
        }
        return null;
    }
    
    @Override
    public Object listenSocket(int port) {
        return new SocketImpl().listen(port);
    }
    
    @Override
    public String getHostOrIP() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch(Throwable t) {
            t.printStackTrace();
            return t.getMessage();
        }
    }

    @Override
    public void disconnectSocket(Object socket) {
        ((SocketImpl)socket).disconnect();
    }    
    
    @Override
    public boolean isSocketConnected(Object socket) {
        return ((SocketImpl)socket).isConnected();
    }
    
    @Override
    public boolean isServerSocketAvailable() {
        return true;
    }

    @Override
    public boolean isSocketAvailable() {
        return true;
    }
    
    @Override
    public String getSocketErrorMessage(Object socket) {
        return ((SocketImpl)socket).getErrorMessage();
    }
    
    @Override
    public int getSocketErrorCode(Object socket) {
        return ((SocketImpl)socket).getErrorCode();
    }
    
    @Override
    public int getSocketAvailableInput(Object socket) {
        return ((SocketImpl)socket).getAvailableInput();
    }
    
    @Override
    public byte[] readFromSocketStream(Object socket) {
        return ((SocketImpl)socket).readFromStream();
    }
    
    @Override
    public void writeToSocketStream(Object socket, byte[] data) {
        ((SocketImpl)socket).writeToStream(data);
    }
    
    //Begin new Graphics Work
    @Override
    public boolean isShapeSupported(Object graphics) {
        return true;
    }

    @Override
    public boolean isTransformSupported(Object graphics) {
        return true;
    }
    
    @Override
    public boolean isPerspectiveTransformSupported(Object graphics){
    	return android.os.Build.VERSION.SDK_INT >= 14;
    }

    @Override
    public void fillShape(Object graphics, com.codename1.ui.geom.Shape shape) {
        AndroidGraphics ag = (AndroidGraphics)graphics;
        Path p = cn1ShapeToAndroidPath(shape);
        ag.fillPath(p);
    }

    @Override
    public void drawShape(Object graphics, com.codename1.ui.geom.Shape shape, com.codename1.ui.Stroke stroke) {
        AndroidGraphics ag = (AndroidGraphics)graphics;
        Path p = cn1ShapeToAndroidPath(shape);
        ag.drawPath(p, stroke);
        
    }
    
    

    // BEGIN TRANSFORMATION METHODS---------------------------------------------------------
    
    
    
    @Override
    public boolean transformEqualsImpl(Transform t1, Transform t2) {
        
        if ( t1 != null ){
            CN1Matrix4f m1 = (CN1Matrix4f)t1.getNativeTransform();
            CN1Matrix4f m2 = (CN1Matrix4f)t2.getNativeTransform();
            return m1.equals(m2);
        } else {
            return t2 == null;
        }
    }

    @Override
    public boolean isTransformSupported() {
        return true;
    }

    @Override
    public boolean isPerspectiveTransformSupported() {
    	
        return true;
    }
    
    
    
    

    @Override
    public Object makeTransformTranslation(float translateX, float translateY, float translateZ) {
        return CN1Matrix4f.makeTranslation(translateX, translateY, translateZ);
    }
    
    @Override
    public void setTransformTranslation(Object nativeTransform, float translateX, float translateY, float translateZ) {
        CN1Matrix4f m = (CN1Matrix4f)nativeTransform;
        m.reset();
        m.translate(translateX, translateY, translateZ);
    }

    @Override
    public Object makeTransformScale(float scaleX, float scaleY, float scaleZ) {
        CN1Matrix4f t = CN1Matrix4f.makeIdentity();
        t.scale(scaleX, scaleY, scaleZ);
        return t;
    }
    
    @Override
    public void setTransformScale(Object nativeTransform, float scaleX, float scaleY, float scaleZ) {
        CN1Matrix4f t = (CN1Matrix4f)nativeTransform;
        t.reset();
        t.scale(scaleX, scaleY, scaleZ);
    }

    @Override
    public Object makeTransformRotation(float angle, float x, float y, float z) {
        return CN1Matrix4f.makeRotation(angle, x, y, z);
    }
    
    @Override
    public void setTransformRotation(Object nativeTransform, float angle, float x, float y, float z) {
        CN1Matrix4f m = (CN1Matrix4f)nativeTransform;
        m.reset();
        m.rotate(angle, x, y, z);
    }

    @Override
    public Object makeTransformPerspective(float fovy, float aspect, float zNear, float zFar) {
        return CN1Matrix4f.makePerspective(fovy, aspect, zNear, zFar);
    }
    
    @Override
    public void setTransformPerspective(Object nativeGraphics, float fovy, float aspect, float zNear, float zFar) {
        CN1Matrix4f m = (CN1Matrix4f)nativeGraphics;
        m.setPerspective(fovy, aspect, zNear, zFar);
    }

    @Override
    public Object makeTransformOrtho(float left, float right, float bottom, float top, float near, float far) {
        return CN1Matrix4f.makeOrtho(left, right, bottom, top, near, far);
    }
    
    @Override
    public void setTransformOrtho(Object nativeGraphics, float left, float right, float bottom, float top, float near, float far) {
        CN1Matrix4f m = (CN1Matrix4f)nativeGraphics;
        m.setOrtho(left, right, bottom, top, near, far);
    }

    @Override
    public Object makeTransformCamera(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
       return CN1Matrix4f.makeCamera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
    }
    
    @Override
    public void setTransformCamera(Object nativeGraphics, float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        CN1Matrix4f m = (CN1Matrix4f)nativeGraphics;
        m.setCamera(eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
    }


    @Override
    public void transformRotate(Object nativeTransform, float angle, float x, float y, float z) {
        ((CN1Matrix4f)nativeTransform).rotate(angle, x, y, z);
    }

    @Override
    public void transformTranslate(Object nativeTransform, float x, float y, float z) {
        //((Matrix) nativeTransform).preTranslate(x, y);
        ((CN1Matrix4f)nativeTransform).translate(x, y, z);
    }

    @Override
    public void transformScale(Object nativeTransform, float x, float y, float z) {
        //((Matrix) nativeTransform).preScale(x, y);
        ((CN1Matrix4f)nativeTransform).scale(x, y, z);
    }

    @Override
    public Object makeTransformInverse(Object nativeTransform) {
        
        CN1Matrix4f inverted = CN1Matrix4f.makeIdentity();
        inverted.setData(((CN1Matrix4f)nativeTransform).getData());
        if( inverted.invert()){
            return inverted;
        }
        return null;
        
        //Matrix inverted = new Matrix();
        //if(((Matrix) nativeTransform).invert(inverted)){
        //    return inverted;
        //}
        //return null;
    }
    
    @Override
    public void setTransformInverse(Object nativeTransform) throws com.codename1.ui.Transform.NotInvertibleException {
        
        CN1Matrix4f m = (CN1Matrix4f)nativeTransform;
        if (!m.invert()) {
            throw new com.codename1.ui.Transform.NotInvertibleException();
        }
    }

    @Override
    public void setTransformIdentity(Object transform) {
        CN1Matrix4f m = (CN1Matrix4f)transform;
        m.setIdentity();
    }
    
    @Override
    public Object makeTransformIdentity() {
        return CN1Matrix4f.makeIdentity();
    }

    @Override
    public void copyTransform(Object src, Object dest) {
        CN1Matrix4f t1 = (CN1Matrix4f) src;
        CN1Matrix4f t2 = (CN1Matrix4f) dest;
        t2.setData(t1.getData());
    }

    @Override
    public void concatenateTransform(Object t1, Object t2) {
        //((Matrix) t1).preConcat((Matrix) t2);
        ((CN1Matrix4f)t1).concatenate((CN1Matrix4f)t2);
    }

    @Override
    public void transformPoint(Object nativeTransform, float[] in, float[] out) {
        //Matrix t = (Matrix) nativeTransform;
        //t.mapPoints(in, 0, out, 0, 2);
        ((CN1Matrix4f)nativeTransform).transformCoord(in, out);
    }

    @Override
    public void setTransform(Object graphics, Transform transform) {
        AndroidGraphics ag = (AndroidGraphics) graphics;
        Transform existing = ag.getTransform();
        if (existing == null) {
            existing = transform == null ? Transform.makeIdentity() : transform.copy();
            ag.setTransform(existing);
        } else {
            if (transform == null) {
                existing.setIdentity();
            } else {
                existing.setTransform(transform);
            }
            ag.setTransform(existing); // sets dirty flag for transform
        }
        
    }

    @Override
    public com.codename1.ui.Transform getTransform(Object graphics) {
        com.codename1.ui.Transform t = ((AndroidGraphics) graphics).getTransform();
        if (t == null) {
            return Transform.makeIdentity();
        }
        Transform t2 = Transform.makeIdentity();
        t2.setTransform(t);
        return t2;
    }

    @Override
    public void getTransform(Object graphics, Transform transform) {
        com.codename1.ui.Transform t = ((AndroidGraphics) graphics).getTransform();
        if (t == null) {
            transform.setIdentity();
        } else {
            transform.setTransform(t);
        }
    }
    
    
    // END TRANSFORM STUFF


    static Path cn1ShapeToAndroidPath(com.codename1.ui.geom.Shape shape, Path p) {
        //Path p = new Path();
        p.rewind();
        com.codename1.ui.geom.PathIterator it = shape.getPathIterator();
        //p.setWindingRule(it.getWindingRule() == com.codename1.ui.geom.PathIterator.WIND_EVEN_ODD ? GeneralPath.WIND_EVEN_ODD : GeneralPath.WIND_NON_ZERO);
        float[] buf = new float[6];
        while (!it.isDone()) {
            int type = it.currentSegment(buf);
            switch (type) {
                case com.codename1.ui.geom.PathIterator.SEG_MOVETO:
                    p.moveTo(buf[0], buf[1]);
                    break;
                case com.codename1.ui.geom.PathIterator.SEG_LINETO:
                    p.lineTo(buf[0], buf[1]);
                    break;
                case com.codename1.ui.geom.PathIterator.SEG_QUADTO:
                    p.quadTo(buf[0], buf[1], buf[2], buf[3]);
                    break;
                case com.codename1.ui.geom.PathIterator.SEG_CUBICTO:
                    p.cubicTo(buf[0], buf[1], buf[2], buf[3], buf[4], buf[5]);
                    break;
                case com.codename1.ui.geom.PathIterator.SEG_CLOSE:
                    p.close();
                    break;

            }
            it.next();
        }

        return p;
    }

    static Path cn1ShapeToAndroidPath(com.codename1.ui.geom.Shape shape) {
        return cn1ShapeToAndroidPath(shape, new Path());
    }

    /**
     * The ID used for a local notification that should actually trigger a background
     * fetch.  This type of notification is handled specially by the {@link LocalNotificationPublisher}.  It
     * doesn't display a notification to the user, but instead just calls the {@link #performBackgroundFetch() }
     * method.
     */
    static final String BACKGROUND_FETCH_NOTIFICATION_ID="$$$CN1_BACKGROUND_FETCH$$$";
    
    
    /**
     * Calls the background fetch callback.  If the app is in teh background, this will
     * check to see if the lifecycle class implements the {@link com.codename1.background.BackgroundFetch}
     * interface.  If it does, it will execute its {@link com.codename1.background.BackgroundFetch#performBackgroundFetch(long, com.codename1.util.Callback) }
     * method.
     * @param blocking True if this should block until it is complete.
     */
    public static void performBackgroundFetch(boolean blocking) {
        
        if (Display.getInstance().isMinimized()) {
            // By definition, background fetch should only occur if the app is minimized.
            // This keeps it consistent with the iOS implementation that doesn't have a 
            // choice
            final boolean[] complete = new boolean[1];
            final Object lock = new Object();
            final BackgroundFetch bgFetchListener = instance.getBackgroundFetchListener();
            final long timeout = System.currentTimeMillis()+25000;
            if (bgFetchListener != null) {
                Display.getInstance().callSerially(new Runnable() {
                    public void run() {
                        bgFetchListener.performBackgroundFetch(timeout, new Callback<Boolean>() {

                            @Override
                            public void onSucess(Boolean value) {
                                // On Android the OS doesn't care whether it worked or not
                                // So we'll just consume this.
                                synchronized (lock) {
                                    complete[0] = true;
                                    lock.notify();
                                }
                            }

                            @Override
                            public void onError(Object sender, Throwable err, int errorCode, String errorMessage) {
                                com.codename1.io.Log.e(err);
                                synchronized (lock) {
                                    complete[0] = true;
                                    lock.notify();
                                }
                            }

                        });
                    }
                });
                
            }

            while (blocking && !complete[0]) {
                synchronized(lock) {
                    try {
                        lock.wait(1000);
                    } catch (Exception ex){}
                }
                if (!complete[0]) {
                    System.out.println("Waiting for background fetch to complete.  Make sure your background fetch handler calls onSuccess() or onError() in the callback when complete");
                
                }
                if (System.currentTimeMillis() > timeout) {
                    System.out.println("Background fetch exceeded time alotted.  Not waiting for its completion");
                    break;
                }
                
            }
            
            
        }
    }
    
    /**
     * Starts the background fetch service.
     */
    public void startBackgroundFetchService() {
        LocalNotification n = new LocalNotification();
        n.setId(BACKGROUND_FETCH_NOTIFICATION_ID);
        cancelLocalNotification(BACKGROUND_FETCH_NOTIFICATION_ID);
        // We schedule a local notification
        // First callback will be at the repeat interval
        // We don't specify a repeat interval because the scheduleLocalNotification will 
        // set that for us using the getPreferredBackgroundFetchInterval method.
        scheduleLocalNotification(n, System.currentTimeMillis() + getPreferredBackgroundFetchInterval() * 1000, 0);
    }
    
    public void stopBackgroundFetchService() {
        cancelLocalNotification(BACKGROUND_FETCH_NOTIFICATION_ID);
    }
    
    
    private boolean backgroundFetchInitialized;
    
    @Override
    public void setPreferredBackgroundFetchInterval(int seconds) {
        int oldInterval = getPreferredBackgroundFetchInterval();
        super.setPreferredBackgroundFetchInterval(seconds);
        
        if (!backgroundFetchInitialized || oldInterval != seconds) {
            backgroundFetchInitialized = true;
            if (seconds > 0) {
                startBackgroundFetchService();
            } else {
                stopBackgroundFetchService();
            }
        }
    }

    
    
    @Override
    public boolean isBackgroundFetchSupported() {
        return true;
    }
    public static BackgroundFetch backgroundFetchListener;
    
    BackgroundFetch getBackgroundFetchListener() {
        if (getActivity() != null && getActivity().getApp() instanceof BackgroundFetch) {
            return (BackgroundFetch)getActivity().getApp();
        } else if (backgroundFetchListener != null) {
            return backgroundFetchListener;
        } else {
            return null;
        }
    }
    
    public void scheduleLocalNotification(LocalNotification notif, long firstTime, int repeat) {
        
        final Intent notificationIntent = new Intent(getContext(), LocalNotificationPublisher.class);
        notificationIntent.setAction(getContext().getApplicationInfo().packageName + "." + notif.getId());        
        notificationIntent.putExtra(LocalNotificationPublisher.NOTIFICATION, createBundleFromNotification(notif));
        
        Intent contentIntent = new Intent();
        if (getActivity() != null) {
            contentIntent.setComponent(getActivity().getComponentName());
        }
        contentIntent.putExtra("LocalNotificationID", notif.getId());
        if (BACKGROUND_FETCH_NOTIFICATION_ID.equals(notif.getId()) && getBackgroundFetchListener() != null) {
            Context context = AndroidNativeUtil.getContext();

            Intent intent = new Intent(context, BackgroundFetchHandler.class);
            //there is an bug that causes this to not to workhttps://code.google.com/p/android/issues/detail?id=81812
            //intent.putExtra("backgroundClass", getBackgroundLocationListener().getName());
            //an ugly workaround to the putExtra bug 
            intent.setData(Uri.parse("http://codenameone.com/a?" + getBackgroundFetchListener().getClass().getName()));
            PendingIntent pendingIntent = PendingIntent.getService(context, 0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            notificationIntent.putExtra(LocalNotificationPublisher.BACKGROUND_FETCH_INTENT, pendingIntent);

        }
        PendingIntent pendingContentIntent = PendingIntent.getActivity(getContext(), 0, contentIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationIntent.putExtra(LocalNotificationPublisher.NOTIFICATION_INTENT, pendingContentIntent);
        
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        if (BACKGROUND_FETCH_NOTIFICATION_ID.equals(notif.getId())) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, firstTime, getPreferredBackgroundFetchInterval() * 1000, pendingIntent);
        } else {
            if(repeat == LocalNotification.REPEAT_NONE){
                alarmManager.set(AlarmManager.RTC_WAKEUP, firstTime, pendingIntent);

            }else if(repeat == LocalNotification.REPEAT_MINUTE){

                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, firstTime, 60*1000, pendingIntent);

            }else if(repeat == LocalNotification.REPEAT_HOUR){

                alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstTime, AlarmManager.INTERVAL_HALF_HOUR, pendingIntent);

            }else if(repeat == LocalNotification.REPEAT_DAY){

                alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstTime, AlarmManager.INTERVAL_DAY, pendingIntent);

            }else if(repeat == LocalNotification.REPEAT_WEEK){

                alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, firstTime, AlarmManager.INTERVAL_DAY * 7, pendingIntent);

            }
        }
    }

    public void cancelLocalNotification(String notificationId) {
        Intent notificationIntent = new Intent(getContext(), LocalNotificationPublisher.class);
        notificationIntent.setAction(getContext().getApplicationInfo().packageName + "." + notificationId);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);        
        AlarmManager alarmManager = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }

    static Bundle createBundleFromNotification(LocalNotification notif){
        Bundle b = new Bundle();
        b.putString("NOTIF_ID", notif.getId());
        b.putString("NOTIF_TITLE", notif.getAlertTitle());
        b.putString("NOTIF_BODY", notif.getAlertBody());
        b.putString("NOTIF_SOUND", notif.getAlertSound());
        b.putString("NOTIF_IMAGE", notif.getAlertImage());
        b.putInt("NOTIF_NUMBER", notif.getBadgeNumber());
        return b;
    }
    
    static LocalNotification createNotificationFromBundle(Bundle b){
        LocalNotification n = new LocalNotification();
        n.setId(b.getString("NOTIF_ID"));
        n.setAlertTitle(b.getString("NOTIF_TITLE"));
        n.setAlertBody(b.getString("NOTIF_BODY"));
        n.setAlertSound(b.getString("NOTIF_SOUND"));
        n.setAlertImage(b.getString("NOTIF_IMAGE"));
        n.setBadgeNumber(b.getInt("NOTIF_NUMBER"));
        return n;
    }

    boolean brokenGaussian;
    public Image gaussianBlurImage(Image image, float radius) {
        try {
            Bitmap outputBitmap = Bitmap.createBitmap((Bitmap)image.getImage());

            RenderScript rs = RenderScript.create(getContext());
            ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            Allocation tmpIn = Allocation.createFromBitmap(rs, (Bitmap)image.getImage());
            Allocation tmpOut = Allocation.createFromBitmap(rs, outputBitmap);
            theIntrinsic.setRadius(radius);
            theIntrinsic.setInput(tmpIn);
            theIntrinsic.forEach(tmpOut);
            tmpOut.copyTo(outputBitmap);

            return new NativeImage(outputBitmap);
        } catch(Throwable t) {
            brokenGaussian = true;
            return image;
        }
    }

    public boolean isGaussianBlurSupported() {
        return (!brokenGaussian) && android.os.Build.VERSION.SDK_INT >= 11;
    }
    
    public static boolean checkForPermission(String permission, String description){
        return checkForPermission(permission, description, false);
    }
    
    public static boolean checkForPermission(String permission, String description, boolean forceAsk){
        //before sdk 23 no need to ask for permission
        if(android.os.Build.VERSION.SDK_INT < 23){
            return true;
        }

        String prompt = Display.getInstance().getProperty(permission, description);
        
        if (android.support.v4.content.ContextCompat.checkSelfPermission(getContext(),
                permission)
                != PackageManager.PERMISSION_GRANTED) {

            if (getActivity() == null) {
                return false;
            }
                    
            // Should we show an explanation?
            if (!forceAsk && android.support.v4.app.ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    permission)) {

                // Show an expanation to the user *asynchronously* -- don't block
                if(Dialog.show("Requires permission", prompt, "Ask again", "Don't Ask")){
                    return checkForPermission(permission, description, true);
                }else {
                    return false;
                }
            } else {

                // No explanation needed, we can request the permission.
                ((CodenameOneActivity)getActivity()).setRequestForPermission(true);
                android.support.v4.app.ActivityCompat.requestPermissions(getActivity(),
                        new String[]{permission},
                        1);
                //wait for a response
                Display.getInstance().invokeAndBlock(new Runnable() {
                    @Override
                    public void run() {
                        while(((CodenameOneActivity)getActivity()).isRequestForPermission()) {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                //check again if the permission is given after the dialog was displayed
                return android.support.v4.content.ContextCompat.checkSelfPermission(getActivity(),
                        permission) == PackageManager.PERMISSION_GRANTED;

            }
        }
        return true;
    }
 
    public boolean isJailbrokenDevice() {
        try {
            Runtime.getRuntime().exec("su");
            return true;
        } catch(Throwable t) {
            com.codename1.io.Log.e(t);
        }
        return false;
    }    
}
