/*
 * Copyright 2008 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */
package com.codename1.impl.blackberry;

import com.codename1.db.Database;
import com.codename1.io.FileSystemStorage;
import com.codename1.ui.BrowserComponent;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.EventDispatcher;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.rim.blackberry.api.invoke.CameraArguments;
import net.rim.blackberry.api.invoke.Invoke;
import net.rim.device.api.database.DatabaseIOException;
import net.rim.device.api.database.DatabasePathException;
import net.rim.device.api.script.ScriptEngine;
import net.rim.device.api.system.Branding;
import org.w3c.dom.Document;

// requires signing
import net.rim.device.api.browser.field2.BrowserField;
import net.rim.device.api.browser.field2.BrowserFieldListener;
import net.rim.device.api.database.DatabaseFactory;
import net.rim.device.api.io.URI;
import net.rim.device.api.io.file.FileSystemJournal;
import net.rim.device.api.io.file.FileSystemJournalEntry;
import net.rim.device.api.io.file.FileSystemJournalListener;
import net.rim.device.api.script.Scriptable;
import net.rim.device.api.system.Bitmap;
import net.rim.device.api.system.Characters;
import net.rim.device.api.system.DeviceInfo;
import net.rim.device.api.system.EventInjector;
import net.rim.device.api.system.JPEGEncodedImage;
import net.rim.device.api.system.PNGEncodedImage;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.UiApplication;

/**
 * Implementation class for newer blackberry devices
 *
 * @author Shai Almog, Thorsten Schemm
 */
public class BlackBerryOS5Implementation extends BlackBerryImplementation {

    BlackBerryCanvas createCanvas() {
        return new BlackBerryTouchSupport(this);
    }

    public void nativeEdit(final Component cmp, final int maxSize, final int constraint, String text, int keyCode) {
        BlackBerryVirtualKeyboard.blockFolding = true;
        super.nativeEdit(cmp, maxSize, constraint, text, keyCode);
    }

    protected void disableBlockFolding() {
        BlackBerryVirtualKeyboard.blockFolding = false;
    }

    public int getKeyboardType() {
        int keyT = Keypad.getHardwareLayout();
        switch (keyT) {
            case Keypad.HW_LAYOUT_TOUCHSCREEN_12:
            case Keypad.HW_LAYOUT_TOUCHSCREEN_24:
            case Keypad.HW_LAYOUT_TOUCHSCREEN_29:
                return Display.KEYBOARD_TYPE_VIRTUAL;
            default:
                return super.getKeyboardType();
        }
    }

    public String getProperty(String key, String defaultValue) {
        if ("User-Agent".equals(key)) {
            return "Blackberry" + DeviceInfo.getDeviceName() + "/" + DeviceInfo.getSoftwareVersion()
                    + " Profile/" + System.getProperty("microedition.profiles")
                    + " Configuration/"
                    + System.getProperty("microedition.configuration")
                    + " VendorID/" + Branding.getVendorId();
        }
        return super.getProperty(key, defaultValue);
    }

    public void copyToClipboard(Object obj) {
        if (obj instanceof String || obj instanceof StringBuffer) {
            net.rim.device.api.system.Clipboard.getClipboard().put(obj);
            super.copyToClipboard(null);
        } else {
            net.rim.device.api.system.Clipboard.getClipboard().put(null);
            super.copyToClipboard(obj);
        }
    }

    public Object getPasteDataFromClipboard() {
        Object o = net.rim.device.api.system.Clipboard.getClipboard().get();
        if (o != null) {
            return o;
        }
        return super.getPasteDataFromClipboard();
    }

    public boolean canForceOrientation() {
        return true;
    }

    public void lockOrientation(boolean portrait) {
        net.rim.device.api.ui.UiEngineInstance ue;
        ue = net.rim.device.api.ui.Ui.getUiEngineInstance();
        if (portrait) {
            ue.setAcceptableDirections(net.rim.device.api.system.Display.DIRECTION_PORTRAIT);
        } else {
            ue.setAcceptableDirections(net.rim.device.api.system.Display.DIRECTION_LANDSCAPE);
        }
    }

    public boolean isNativeBrowserComponentSupported() {
        return false;
    }

    public PeerComponent createBrowserComponent(Object browserComponent) {
        synchronized (UiApplication.getEventLock()) {
            BrowserField bff = new BrowserField();
            final BrowserComponent cmp = (BrowserComponent) browserComponent;
            bff.addListener(new BrowserFieldListener() {

                public void documentError(BrowserField browserField, Document document) throws Exception {
                    cmp.fireWebEvent("onError", new ActionEvent(document));
                    super.documentError(browserField, document);
                }

                public void documentCreated(BrowserField browserField, ScriptEngine scriptEngine, Document document) throws Exception {
                    cmp.fireWebEvent("onStart", new ActionEvent(document));
                    super.documentCreated(browserField, scriptEngine, document);
                }
                
                public void documentLoaded(BrowserField browserField, Document document) throws Exception {
                    cmp.fireWebEvent("onLoad", new ActionEvent(document));
                    super.documentLoaded(browserField, document);
                }
            });
            return PeerComponent.create(bff);
        }
    }

    public void setBrowserProperty(PeerComponent browserPeer, String key, Object value) {
    }

    public String getBrowserTitle(PeerComponent browserPeer) {
        synchronized (UiApplication.getEventLock()) {
            return ((BrowserField) browserPeer.getNativePeer()).getDocumentTitle();
        }
    }

    public String getBrowserURL(PeerComponent browserPeer) {
        synchronized (UiApplication.getEventLock()) {
            return ((BrowserField) browserPeer.getNativePeer()).getDocumentUrl();
        }
    }

    public void setBrowserURL(PeerComponent browserPeer, String url) {
        if (url.startsWith("jar://")) {
            //ApplicationDescriptor ad = ApplicationDescriptor.currentApplicationDescriptor();
            //url = "cod://" + ad.getModuleName() +  url.substring(6);
            //super.setBrowserURL(browserPeer, url);
            //url = "local://" + url.substring(6);

            // load from jar:// URL's
            try {
                InputStream i = Display.getInstance().getResourceAsStream(getClass(), url.substring(6));
                if (i == null) {
                    System.out.println("Local resource not found: " + url);
                    return;
                }
                byte[] buffer = new byte[4096];
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                int size = i.read(buffer);
                while (size > -1) {
                    bo.write(buffer, 0, size);
                    size = i.read(buffer);
                }
                i.close();
                bo.close();
                String htmlText = new String(bo.toByteArray(), "UTF-8");
                int pos = url.lastIndexOf('/');
                if (pos > 6) {
                    url = url.substring(6, pos);
                } else {
                    url = "/";
                }
                String baseUrl = "local://" + url;
                setBrowserPage(browserPeer, htmlText, baseUrl);
                return;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return;
        }
        synchronized (UiApplication.getEventLock()) {
            ((BrowserField) browserPeer.getNativePeer()).requestContent(url);
        }
    }

    public void browserReload(PeerComponent browserPeer) {
        synchronized (UiApplication.getEventLock()) {
            ((BrowserField) browserPeer.getNativePeer()).refresh();
        }
    }

    public boolean browserHasBack(PeerComponent browserPeer) {
        return ((BrowserField) browserPeer.getNativePeer()).getHistory().canGoBack();
    }

    public boolean browserHasForward(PeerComponent browserPeer) {
        synchronized (UiApplication.getEventLock()) {
            return ((BrowserField) browserPeer.getNativePeer()).getHistory().canGoForward();
        }
    }

    public void browserBack(PeerComponent browserPeer) {
        synchronized (UiApplication.getEventLock()) {
            ((BrowserField) browserPeer.getNativePeer()).back();
        }
    }

    public void browserForward(PeerComponent browserPeer) {
        synchronized (UiApplication.getEventLock()) {
            ((BrowserField) browserPeer.getNativePeer()).forward();
        }
    }

    public void browserClearHistory(PeerComponent browserPeer) {
        synchronized (UiApplication.getEventLock()) {
            ((BrowserField) browserPeer.getNativePeer()).getHistory().clearHistory();
        }
    }

    public void setBrowserPage(PeerComponent browserPeer, String html, String baseUrl) {
        synchronized (UiApplication.getEventLock()) {
            ((BrowserField) browserPeer.getNativePeer()).displayContent(html, baseUrl);
        }
    }

    public void browserExecute(PeerComponent browserPeer, String javaScript) {
        synchronized (UiApplication.getEventLock()) {
            ((BrowserField) browserPeer.getNativePeer()).executeScript(javaScript);
        }
    }

    protected int getFieldWidth(Field fld) {
        return Math.max(fld.getWidth(), fld.getPreferredWidth()) + fld.getPaddingLeft() + fld.getPaddingRight();
    }

    protected int getFieldHeight(Field fld) {
        return Math.max(fld.getHeight(), fld.getPreferredHeight()) + fld.getPaddingBottom() + fld.getPaddingTop();
    }
    
    public void browserExposeInJavaScript(PeerComponent browserPeer, Object o, String name) {
        synchronized (UiApplication.getEventLock()) {
            try {
                ((BrowserField) browserPeer.getNativePeer()).extendScriptEngine(name, (Scriptable) o);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void captureVideo(ActionListener response) {
        captureCallback = new EventDispatcher();
        captureCallback.addListener(response);

        UiApplication.getUiApplication().addFileSystemJournalListener(new FileSystemJournalListener() {

            private long lastUSN;
            private String videoPath;

            public void fileJournalChanged() {
                // next sequence number file system will use
                long USN = FileSystemJournal.getNextUSN();

                for (long i = USN - 1; i >= lastUSN && i < USN; --i) {
                    FileSystemJournalEntry entry = FileSystemJournal.getEntry(i);
                    if (entry == null) {
                        break;
                    }

                    String path = entry.getPath();
                    if (entry.getEvent() == FileSystemJournalEntry.FILE_ADDED
                            && videoPath == null) {
                        int index = path.indexOf(".3GP");
                        if (index != -1) {
                            videoPath = path;
                        }
                    } else if (entry.getEvent() == FileSystemJournalEntry.FILE_RENAMED) {
                        if (path != null && path.equals(videoPath)) {
                            //close the camera
                            UiApplication.getUiApplication().removeFileSystemJournalListener(this);

                            try {
                                EventInjector.KeyEvent inject = new EventInjector.KeyEvent(EventInjector.KeyEvent.KEY_DOWN, Characters.ESCAPE, 0, 200);
                                inject.post();
                                inject.post();
                            } catch (Exception e) {
                                //try to close the camera
                            }

                            captureCallback.fireActionEvent(new ActionEvent("file://" + path));
                            captureCallback = null;
                            videoPath = null;
                            break;
                        }
                    }
                }
                lastUSN = USN;
            }
        });
        app.setWaitingForReply(true);
        synchronized (UiApplication.getEventLock()) {
            Invoke.invokeApplication(Invoke.APP_TYPE_CAMERA, new CameraArguments(CameraArguments.ARG_VIDEO_RECORDER));
        }
    }
    
    
    private com.codename1.ui.util.ImageIO imIO;
    
    /**
     * @inheritDoc
     */
    public com.codename1.ui.util.ImageIO getImageIO() {
        if(imIO == null) {
            imIO = new com.codename1.ui.util.ImageIO() {

                public void save(InputStream image, OutputStream response, String format, int width, int height, float quality) throws IOException {
                    Image img = Image.createImage(image).scaled(width, height);
                    if(width < 0) {
                        width = img.getWidth();
                    }
                    if(height < 0) {
                        width = img.getHeight();
                    }
                    Bitmap bitmap = (Bitmap) img.getImage();
                    if(format == FORMAT_JPEG) {
                        JPEGEncodedImage enc = JPEGEncodedImage.encode(bitmap, (int)(quality*100));
                        response.write(enc.getData(), 0, enc.getData().length); 
                    }else{
                        PNGEncodedImage enc = PNGEncodedImage.encode(bitmap);
                        response.write(enc.getData(), 0, enc.getData().length);                         
                    }
                }

                protected void saveImage(Image img, OutputStream response, String format, float quality) throws IOException {
                    Bitmap bitmap = (Bitmap) img.getImage();
                    if(format == FORMAT_JPEG) {
                        JPEGEncodedImage enc = JPEGEncodedImage.encode(bitmap, (int)(quality*100));
                        response.write(enc.getData(), 0, enc.getData().length); 
                    }else{
                        PNGEncodedImage enc = PNGEncodedImage.encode(bitmap);
                        response.write(enc.getData(), 0, enc.getData().length);                         
                    }
                }

                public boolean isFormatSupported(String format) {
                    return format == FORMAT_JPEG || format == FORMAT_PNG;
                }
            };
        }
        return imIO;
    }
    
    public Database openOrCreateDB(String databaseName) throws IOException {
        try {
            URI dbURI = URI.create(getDBDir() + databaseName);
            net.rim.device.api.database.Database db = DatabaseFactory.openOrCreate(dbURI);
            return new BBDatabase(db);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public void deleteDB(String databaseName) throws IOException {
        try {
            URI dbURI = URI.create(getDBDir() + databaseName);
            DatabaseFactory.delete(dbURI);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IOException(ex.getMessage());
        }
    }

    public boolean existsDB(String databaseName) {
        try {
            URI dbURI = URI.create(getDBDir() + databaseName);
            return DatabaseFactory.exists(dbURI);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }
    
    private String getDBDir() {
        String[] roots = listFilesystemRoots();
        // iOS doesn't have an SD card
        String file = null;
        for (int i = 0; i < roots.length; i++) {
            if (roots[i].indexOf("SDCard") > -1) {
                file = roots[i];
                break;
            }
        }
        //no sd card try a different location
        if (file == null) {
            for (int i = 0; i < roots.length; i++) {
                if (getRootType(roots[i]) == FileSystemStorage.ROOT_TYPE_SDCARD) {
                    file = roots[i];
                    break;
                }
            }
        }
        if(file == null){
            file = roots[0];
        }
        file += "Databases/";
        if (!exists(file)) {
            mkdir(file);
        }
        return file;
    }
}
