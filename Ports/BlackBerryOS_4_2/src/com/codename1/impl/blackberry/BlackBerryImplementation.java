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

import com.codename1.codescan.CodeScanner;
import com.codename1.contacts.Contact;
import com.codename1.ui.Component;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.PeerComponent;
import com.codename1.ui.TextArea;
import com.codename1.ui.animations.Animation;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.impl.CodenameOneImplementation;
import com.codename1.impl.blackberry.codescan.CodeScannerImpl;
import com.codename1.impl.blackberry.codescan.MultimediaManager;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.ui.util.Resources;
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.media.MediaException;
import javax.microedition.media.Player;
import javax.microedition.midlet.MIDlet;
import net.rim.device.api.ui.Screen;
import net.rim.device.api.system.Application;
import net.rim.device.api.system.Bitmap;

import net.rim.device.api.system.GPRSInfo;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FocusChangeListener;
import net.rim.device.api.ui.Font;
import net.rim.device.api.ui.FontFamily;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.Ui;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.XYRect;
import net.rim.device.api.ui.component.ActiveAutoTextEditField;
import net.rim.device.api.ui.component.BasicEditField;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.NullField;
import net.rim.device.api.ui.component.PasswordEditField;
import net.rim.device.api.ui.container.PopupScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;
import com.codename1.io.BufferedInputStream;
import com.codename1.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.Connection;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import net.rim.device.api.servicebook.ServiceBook;
import net.rim.device.api.servicebook.ServiceRecord;
import com.codename1.io.NetworkManager;

// requires signing
import com.codename1.l10n.L10NManager;
import com.codename1.location.LocationManager;
import com.codename1.media.Media;
import com.codename1.messaging.Message;
import com.codename1.ui.Image;
import java.util.Date;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.wireless.messaging.MessageConnection;
import javax.wireless.messaging.TextMessage;
import net.rim.blackberry.api.invoke.CameraArguments;
import net.rim.blackberry.api.invoke.Invoke;
import net.rim.blackberry.api.invoke.MessageArguments;
import net.rim.blackberry.api.invoke.PhoneArguments;
import net.rim.blackberry.api.mail.Address;
import net.rim.blackberry.api.mail.Folder;
import net.rim.blackberry.api.mail.Multipart;
import net.rim.blackberry.api.mail.Session;
import net.rim.blackberry.api.mail.SupportedAttachmentPart;
import net.rim.blackberry.api.mail.TextBodyPart;
import net.rim.device.api.system.CodeModuleGroup;
import net.rim.device.api.system.CodeModuleGroupManager;
import net.rim.blackberry.api.phone.Phone;
import net.rim.device.api.applicationcontrol.ApplicationPermissions;
import net.rim.device.api.applicationcontrol.ApplicationPermissionsManager;
import net.rim.device.api.i18n.DateFormat;
import net.rim.device.api.i18n.Locale;
import net.rim.device.api.io.IOUtilities;
import net.rim.device.api.io.file.FileSystemJournal;
import net.rim.device.api.io.file.FileSystemJournalEntry;
import net.rim.device.api.io.file.FileSystemJournalListener;
import net.rim.device.api.system.ApplicationDescriptor;
import net.rim.device.api.system.ApplicationManager;
import net.rim.device.api.system.ApplicationManagerException;
import net.rim.device.api.system.Characters;
import net.rim.device.api.system.CodeModuleManager;
import net.rim.device.api.system.EventInjector;
import net.rim.device.api.system.GlobalEventListener;
import net.rim.device.api.system.JPEGEncodedImage;
import net.rim.device.api.system.RadioInfo;
import net.rim.device.api.system.SystemListener2;

/**
 * The implementation of the blackberry platform delegates the work to the underlying UI
 * application to allow for deep BB integration
 *
 * @author Shai Almog, Thorsten Schemm
 */
public class BlackBerryImplementation extends CodenameOneImplementation {
    public static boolean nativeBrowser = false;

    private static CodeModuleGroup group;
    static Hashtable fieldComponentMap = new Hashtable();
    static final int INVOKE_LATER_confirmControlView = 1;
    static final int INVOKE_LATER_finishEdit = 2;
    static final int INVOKE_LATER_initComponent = 3;
    static final int INVOKE_LATER_deinitialize = 4;
    static final int INVOKE_LATER_showNativeScreen = 5;
    static final int INVOKE_AND_WAIT_calcPreferredSize = 6;
    static final int INVOKE_AND_WAIT_setFocus = 7;
    static final int INVOKE_LATER_dirty = 8;
    static final int INVOKE_LATER_startMedia = 9;
    private static boolean minimizeOnEnd = true;
    // blackberry sometimes "breaks" the drawing color... No idea why this might happen...
    private int color;
    static final int MENU_SOFT_KEY = Keypad.KEY_MENU;
    static final int GAME_KEY_CODE_FIRE = -90;
    static final int GAME_KEY_CODE_UP = -91;
    static final int GAME_KEY_CODE_DOWN = -92;
    static final int GAME_KEY_CODE_LEFT = -93;
    static final int GAME_KEY_CODE_RIGHT = -94;
    BasicEditField nativeEdit;
    TextArea lightweightEdit;
    private BlackBerryCanvas canvas = createCanvas();
    CodenameOneUiApplication app;
    private boolean initGetProperty = true;
    private static EventDispatcher volumeListener;
    private NullField nullFld;
    private String currentAccessPoint;
    private boolean deviceSide;
    /**
     * The File Allocation Table assigns user based file names to RMS storage
     */
    private Hashtable fat = new Hashtable();
    private short currentKey = 1;
    //protected ActionListener camResponse;
    protected EventDispatcher captureCallback;
    private static boolean askForPermission = true;

    BlackBerryCanvas createCanvas() {
        return new BlackBerryCanvas(this);
    }

    public static void setAskPermission(boolean ask) {
        askForPermission = ask;
    }
    
    public void init(Object m) {
        
        app = (CodenameOneUiApplication) m;        

        if(askForPermission) {
            app.invokeAndWait(new Runnable() {
                public void run() {
                    ApplicationPermissions permRequest = new ApplicationPermissions();
                    permRequest.addPermission(ApplicationPermissions.PERMISSION_EMAIL);
                    permRequest.addPermission(ApplicationPermissions.PERMISSION_FILE_API);
                    permRequest.addPermission(ApplicationPermissions.PERMISSION_WIFI);
                    try {
                        //ApplicationPermissions.PERMISSION_CROSS_APPLICATION_COMMUNICATION
                        permRequest.addPermission(11);
                    } catch (Exception e) {
                    }
                    try {
                        //ApplicationPermissions.PERMISSION_MEDIA
                        permRequest.addPermission(21);
                    } catch (Exception e) {
                    }
                    try {
                        //ApplicationPermissions.PERMISSION_INPUT_SIMULATION
                        permRequest.addPermission(6);
                    } catch (Exception e) {
                    }
                    try {
                        //ApplicationPermissions.PERMISSION_LOCATION_DATA
                        permRequest.addPermission(14);
                    } catch (Exception e) {
                    }
                    try {
                        //ApplicationPermissions.PERMISSION_ORGANIZER_DATA
                        permRequest.addPermission(16);
                    } catch (Exception e) {
                    }
                    try {
                        //ApplicationPermissions.PERMISSION_INTERNET
                        permRequest.addPermission(7);
                    } catch (Exception e) {
                    }
                    try {
                        //ApplicationPermissions.PERMISSION_RECORDING
                        permRequest.addPermission(17);
                    } catch (Exception e) {
                    }

                    ApplicationPermissionsManager apm = ApplicationPermissionsManager.getInstance();
                    if (!apm.invokePermissionsRequest(permRequest)) {
                        exitApplication();
                        return;
                    }
                }
            });        
        }
        
        app.enableKeyUpEvents(true);
        if (!app.isHandlingEvents()) {
            new Thread() {

                public void run() {
                    app.enterEventDispatcher();
                }
            }.start();
        }
        Dialog.setCommandsAsButtons(true);
        UIManager.getInstance().addThemeRefreshListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                Hashtable themeProps = new Hashtable();
                themeProps.put("SoftButton.margin", "0,0,0,0");
                themeProps.put("SoftButton.padding", "0,0,0,0");
                UIManager.getInstance().addThemeProps(themeProps);
            }
        });


        RecordEnumeration e = null;
        RecordStore r = null;
        try {
            r = RecordStore.openRecordStore("FAT", true);
            if (r.getNumRecords() > 0) {
                e = r.enumerateRecords(null, null, false);
                while (e.hasNextElement()) {
                    byte[] rec = e.nextRecord();
                    ByteArrayInputStream bi = new ByteArrayInputStream(rec);
                    DataInputStream di = new DataInputStream(bi);
                    String name = di.readUTF();
                    short key = di.readShort();
                    di.close();
                    bi.close();
                    fat.put(name, new Short(key));
                    if (key >= currentKey) {
                        currentKey += key;
                    }
                }
                e.destroy();
                e = null;
            }
            r.closeRecordStore();
            r = null;
        } catch (Exception ex) {
            ex.printStackTrace();
            cleanup(r);
            cleanup(e);
        }
    }

    public void confirmControlView() {
        InvokeLaterWrapper i = new InvokeLaterWrapper(INVOKE_LATER_confirmControlView);
        i.fld = canvas;
        app.invokeLater(i);
    }

    protected void keyPressed(final int keyCode) {
        // expose the method to the canvas
        super.keyPressed(keyCode);
    }

    protected void keyReleased(final int keyCode) {
        // expose the method to the canvas
        super.keyReleased(keyCode);
    }

    public int getDisplayWidth() {
        // the alternative undeprecated API requires a signature
        return net.rim.device.api.ui.Graphics.getScreenWidth();
    }

    public int getDisplayHeight() {
        // the alternative undeprecated API requires a signature
        return net.rim.device.api.ui.Graphics.getScreenHeight();
    }

    public void editString(final Component cmp, final int maxSize, final int constraint, final String text, int keyCode) {
        TextArea txtCmp = (TextArea) cmp;
        String edit = (String) txtCmp.getClientProperty("RIM.nativePopup");
  
        if(edit != null){
            EditPopup editpop = new EditPopup(txtCmp, maxSize);
            editpop.startEdit();
        }else{        
            nativeEdit(txtCmp, txtCmp.getMaxSize(), txtCmp.getConstraint(), txtCmp.getText(), keyCode);
        }
    }

    public void nativeEdit(final Component cmp, final int maxSize, final int constraint, String text, int keyCode) {
        if (nativeEdit != null) {
            finishEdit(true);
        }

        lightweightEdit = (TextArea) cmp;
        if (keyCode > 0 && getKeyboardType() == Display.KEYBOARD_TYPE_QWERTY) {
            text += ((char) keyCode);
            lightweightEdit.setText(text);
        }
        class LightweightEdit implements Runnable, Animation {

            public void run() {
                long type = 0;
                TextArea lightweightEditTmp = lightweightEdit;
                if (lightweightEditTmp == null) {
                    return;
                }
                int constraint = lightweightEditTmp.getConstraint();
                if ((constraint & TextArea.DECIMAL) == TextArea.DECIMAL) {
                    type = BasicEditField.FILTER_REAL_NUMERIC;
                } else if ((constraint & TextArea.EMAILADDR) == TextArea.EMAILADDR) {
                    type = BasicEditField.FILTER_EMAIL;
                } else if ((constraint & TextArea.NUMERIC) == TextArea.NUMERIC) {
                    type = BasicEditField.FILTER_NUMERIC;
                } else if ((constraint & TextArea.PHONENUMBER) == TextArea.PHONENUMBER) {
                    type = BasicEditField.FILTER_PHONE;
                } else if ((constraint & TextArea.NON_PREDICTIVE) == TextArea.NON_PREDICTIVE) {
                    type = BasicEditField.NO_COMPLEX_INPUT;
                }
                        
                    
                

                if (lightweightEditTmp.isSingleLineTextArea()) {
                    type |= BasicEditField.NO_NEWLINE;
                }

                if ((constraint & TextArea.PASSWORD) == TextArea.PASSWORD) {
                    nativeEdit = new BBPasswordEditField(lightweightEditTmp, type, maxSize);
                } else {
                    nativeEdit = new BBEditField(lightweightEditTmp, type, maxSize);
                }
                nativeEdit.setEditable(true);
                Font f = nativeEdit.getFont();
                if (f.getHeight() > lightweightEditTmp.getStyle().getFont().getHeight()) {
                    nativeEdit.setFont(f.derive(f.getStyle(), lightweightEditTmp.getStyle().getFont().getHeight()));
                }
                canvas.add(nativeEdit);
                nativeEdit.setCursorPosition(lightweightEditTmp.getText().length());
                try {
                    nativeEdit.setFocus();
                } catch (Throwable t) {
                    // no idea why this throws an exception sometimes
                    //t.printStackTrace();
                }
            }

            public boolean animate() {
                BasicEditField ef = nativeEdit;
                Component lw = lightweightEdit;
                if (lw == null || lw.getComponentForm() != Display.getInstance().getCurrent()) {
                    Display.getInstance().getCurrent().deregisterAnimated(this);
                    finishEdit(false);
                } else {
                    if (ef != null) {
                        if (ef.isDirty()) {
                            lw.repaint();
                        }
                    }
                }
                return false;
            }

            public void paint(com.codename1.ui.Graphics g) {
            }
        }
        LightweightEdit lw = new LightweightEdit();
        Display.getInstance().getCurrent().registerAnimated(lw);
        Application.getApplication().invokeLater(lw);
    }

    public void setCurrentForm(Form f) {
        super.setCurrentForm(f);

        nullFld = null;
        synchronized (UiApplication.getEventLock()) {
            while (canvas.getFieldCount() > 0) {
                canvas.delete(canvas.getField(0));
            }
        }
    }

    protected void disableBlockFolding() {
    }

    void finishEdit(final boolean canceled) {
        // assigning the field here prevents a null pointer exception in the layout
        BasicEditField be = nativeEdit;
        if (be != null) {
            if (!Application.isEventDispatchThread()) {
                InvokeLaterWrapper i = new InvokeLaterWrapper(INVOKE_LATER_finishEdit);
                Application.getApplication().invokeLater(i);
                return;
            }
            if (!canceled && lightweightEdit != null) {
                Display.getInstance().onEditingComplete(lightweightEdit, be.getText());
            }
            lightweightEdit = null;
            nativeEdit = null;
            canvas.delete(be);
            flushGraphics();
            disableBlockFolding();
            Display.getInstance().setShowVirtualKeyboard(false);
        }
    }

    public void setNativeCommands(Vector commands) {
        canvas.setNativeCommands(commands);
    }

    public void flushGraphics(int x, int y, int width, int height) {
        canvas.flush(x, y, width, height, app);
    }

    public void flushGraphics() {
        canvas.flush(0, 0, getDisplayWidth(), getDisplayHeight(), app);
    }

    public void getRGB(Object nativeImage, int[] arr, int offset, int x, int y, int width, int height) {
        Bitmap b = (Bitmap) nativeImage;
        b.getARGB(arr, offset, width, x, y, width, height);
    }

    public Object createImage(int[] rgb, int width, int height) {
        Bitmap image = new Bitmap(width, height);
        image.setARGB(rgb, 0, width, 0, 0, width, height);
        return image;
    }

    public Object createImage(String path) throws IOException {
        if (path.startsWith("file:") && exists(path)) {
            InputStream is = null;
            try {
                is = openInputStream(path);
                return createImage(is);
            } finally {
                is.close();
            }
        }

        try {
            return createImage(com.codename1.ui.Image.class.getResourceAsStream(path));
        } catch (RuntimeException err) {
            throw new IOException(err.toString());
        }
    }

    public Object createImage(InputStream i) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int len;
        while ((len = i.read(buf)) > -1) {
            out.write(buf, 0, len);
        }
        out.close();
        i.close();
        byte[] b = out.toByteArray();
        return Bitmap.createBitmapFromBytes(b, 0, b.length, 1);
    }

    public boolean isAlphaMutableImageSupported() {
        return true;
    }

    public Object createMutableImage(int width, int height, int fillColor) {
        Bitmap b = new Bitmap(width, height);
        Graphics g = new Graphics(b);
        if ((fillColor & 0xff000000) != 0xff000000) {
            g.setColor(fillColor & 0xffffff);
            int oldAlpha = g.getGlobalAlpha();
            g.setGlobalAlpha((fillColor >> 24) & 0xff);
            g.clear();
            g.setGlobalAlpha(oldAlpha);
        } else {
            g.setColor(fillColor & 0xffffff);
            g.fillRect(0, 0, width, height);
        }
        return b;
    }

    public Object createImage(byte[] bytes, int offset, int len) {
        return Bitmap.createBitmapFromBytes(bytes, offset, len, 1);
    }

    public int getImageWidth(Object i) {
        return ((Bitmap) i).getWidth();
    }

    public int getImageHeight(Object i) {
        return ((Bitmap) i).getHeight();
    }

    /**
     * @inheritDoc
     */
    public Object scale(Object nativeImage, int width, int height) {
        Bitmap image = (Bitmap) nativeImage;
        int srcWidth = image.getWidth();
        int srcHeight = image.getHeight();

        // no need to scale
        if (srcWidth == width && srcHeight == height) {
            return image;
        }

        int[] currentArray = new int[srcWidth];
        int[] destinationArray = new int[width * height];
        scaleArray(image, srcWidth, srcHeight, height, width, currentArray, destinationArray);

        return createImage(destinationArray, width, height);
    }

    private void scaleArray(Bitmap currentImage, int srcWidth, int srcHeight, int height, int width, int[] currentArray, int[] destinationArray) {
        // Horizontal Resize
        int yRatio = (srcHeight << 16) / height;
        int xRatio = (srcWidth << 16) / width;
        int xPos = xRatio / 2;
        int yPos = yRatio / 2;

        // if there is more than 16bit color there is no point in using mutable
        // images since they won't save any memory
        for (int y = 0; y < height; y++) {
            int srcY = yPos >> 16;
            getRGB(currentImage, currentArray, 0, 0, srcY, srcWidth, 1);
            for (int x = 0; x < width; x++) {
                int srcX = xPos >> 16;
                int destPixel = x + y * width;
                if ((destPixel >= 0 && destPixel < destinationArray.length) && (srcX < currentArray.length)) {
                    destinationArray[destPixel] = currentArray[srcX];
                }
                xPos += xRatio;
            }
            yPos += yRatio;
            xPos = xRatio / 2;
        }
    }

    public int getSoftkeyCount() {
        return 1;
    }

    public int[] getSoftkeyCode(int index) {
        return new int[]{MENU_SOFT_KEY};
    }

    public int getClearKeyCode() {
        return Keypad.KEY_DELETE;
    }

    public int getBackspaceKeyCode() {
        return Keypad.KEY_BACKSPACE;
    }

    public int getBackKeyCode() {
        return Keypad.KEY_ESCAPE;
    }

    public int getGameAction(int keyCode) {
        switch (keyCode) {
            // the enter key should also map to fire
            case '\n':
            case GAME_KEY_CODE_FIRE:
                return Display.GAME_FIRE;
            case GAME_KEY_CODE_UP:
                return Display.GAME_UP;
            case GAME_KEY_CODE_DOWN:
                return Display.GAME_DOWN;
            case GAME_KEY_CODE_LEFT:
                return Display.GAME_LEFT;
            case GAME_KEY_CODE_RIGHT:
                return Display.GAME_RIGHT;
        }
        return 0;
    }

    public int getKeyCode(int gameAction) {
        switch (gameAction) {
            case Display.GAME_FIRE:
                return GAME_KEY_CODE_FIRE;
            case Display.GAME_UP:
                return GAME_KEY_CODE_UP;
            case Display.GAME_DOWN:
                return GAME_KEY_CODE_DOWN;
            case Display.GAME_LEFT:
                return GAME_KEY_CODE_LEFT;
            case Display.GAME_RIGHT:
                return GAME_KEY_CODE_RIGHT;
        }
        return 0;
    }

    public boolean isTouchDevice() {
        return canvas.isTouchDevice();
    }

    public int getColor(Object graphics) {
        if (graphics == canvas.getGlobalGraphics()) {
            // make sure color wasn't broken...
            ((Graphics) graphics).setColor(color);
            return color;
        }
        return ((Graphics) graphics).getColor() & 0xffffff;
    }

    public void setColor(Object graphics, int RGB) {
        if (graphics == canvas.getGlobalGraphics()) {
            color = RGB;
        }
        ((Graphics) graphics).setColor(RGB);
    }

    public boolean isAlphaGlobal() {
        return true;
    }

    public void setAlpha(Object graphics, int alpha) {
        ((Graphics) graphics).setGlobalAlpha(alpha);
    }

    public int getAlpha(Object graphics) {
        return ((Graphics) graphics).getGlobalAlpha();
    }

    public void setNativeFont(Object graphics, Object font) {
        ((Graphics) graphics).setFont(font(font));
    }

    public int getClipX(Object graphics) {
        XYRect r = new XYRect();
        ((Graphics) graphics).getAbsoluteClippingRect(r);
        return r.x;
    }

    public int getClipY(Object graphics) {
        XYRect r = new XYRect();
        ((Graphics) graphics).getAbsoluteClippingRect(r);
        return r.y;
    }

    public int getClipWidth(Object graphics) {
        XYRect r = new XYRect();
        ((Graphics) graphics).getAbsoluteClippingRect(r);
        return r.width;
    }

    public int getClipHeight(Object graphics) {
        XYRect r = new XYRect();
        ((Graphics) graphics).getAbsoluteClippingRect(r);
        return r.height;
    }

    public void setClip(Object graphics, int x, int y, int width, int height) {
        Graphics g = (net.rim.device.api.ui.Graphics) graphics;
        net.rim.device.api.ui.Font oldFont = g.getFont();
        int oldColor = g.getColor();
        int oldAlpha = g.getGlobalAlpha();
        while (g.getContextStackSize() > 1) {
            g.popContext();
        }
        g.pushRegion(x, y, width, height, 0, 0);
        g.translate(-g.getTranslateX(), -g.getTranslateY());
        /**
         * applying a clip will automatically
         * reset some information that we need to keep track of
         * manually (it seems).
         */
        g.setFont(oldFont == null ? (net.rim.device.api.ui.Font) getDefaultFont() : oldFont);
        g.setColor(oldColor);
        g.setGlobalAlpha(oldAlpha);
    }

    public void clipRect(Object graphics, int x, int y, int width, int height) {
        Graphics g = (Graphics) graphics;
        g.pushRegion(x, y, width, height, 0, 0);
        g.translate(-g.getTranslateX(), -g.getTranslateY());
    }

    public void drawLine(Object graphics, int x1, int y1, int x2, int y2) {
        ((Graphics) graphics).drawLine(x1, y1, x2, y2);
    }

    public void fillRect(Object graphics, int x, int y, int width, int height) {
        ((Graphics) graphics).fillRect(x, y, width, height);
    }

    public void drawRect(Object graphics, int x, int y, int width, int height) {
        ((Graphics) graphics).drawRect(x, y, width, height);
    }

    public void drawRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        ((Graphics) graphics).drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void fillRoundRect(Object graphics, int x, int y, int width, int height, int arcWidth, int arcHeight) {
        ((Graphics) graphics).fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void fillArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        ((Graphics) graphics).fillArc(x, y, width, height, startAngle, arcAngle);
    }

    public void drawArc(Object graphics, int x, int y, int width, int height, int startAngle, int arcAngle) {
        ((Graphics) graphics).drawArc(x, y, width, height, startAngle, arcAngle);
    }

    public void drawString(Object graphics, String str, int x, int y) {
        ((Graphics) graphics).drawText(str, x, y);
    }

//    /**
//     * @inheritDoc
//     */
//    public VideoComponent createVideoPeer(String url) throws IOException {
//        try {
//            Player p = Manager.createPlayer(url);
//            p.realize();
//            VideoControl vidc = (VideoControl) p.getControl("VideoControl");
//            Field f = (Field) vidc.initDisplayMode(VideoControl.USE_GUI_PRIMITIVE, "net.rim.device.api.ui.Field");
//            RIMVideoComponent r = new RIMVideoComponent(f, p, vidc);
//            r.putClientProperty("VideoControl", vidc);
//            r.putClientProperty("Player", p);
//            return r;
//        } catch (MediaException ex) {
//            ex.printStackTrace();
//            throw new IOException(ex.toString());
//        }
//    }
    /**
     * Plays the sound in the given URI which is partially platform specific.
     *
     * @param uri the platform specific location for the sound
     * @param onCompletion invoked when the audio file finishes playing, may be null
     * @return a handle that can be used to control the playback of the audio
     * @throws java.io.IOException if the URI access fails
     */
    public Media createMedia(String uri, boolean isVideo, Runnable onCompletion) throws IOException {

        MMAPIPlayer player = MMAPIPlayer.createPlayer(uri, onCompletion);
        if (isVideo) {
            VideoMainScreen video = new VideoMainScreen(player, this);
            return video;
        }
        return player;
    }

    /**
     * Plays the sound in the given stream
     *
     * @param stream the stream containing the media data
     * @param mimeType the type of the data in the stream
     * @param onCompletion invoked when the audio file finishes playing, may be null
     * @return a handle that can be used to control the playback of the audio
     * @throws java.io.IOException if the URI access fails
     */
    public Media createMedia(InputStream stream, String mimeType, Runnable onCompletion) throws IOException {

        MMAPIPlayer player = MMAPIPlayer.createPlayer(stream, mimeType, onCompletion);
        if (mimeType.indexOf("video") > -1) {
            VideoMainScreen video = new VideoMainScreen(player, this);
            return video;
        }
        return player;
    }

    /**
     * This method returns the platform Location Control
     * @return LocationControl Object
     */
    public LocationManager getLocationManager() {
        return new RIMLocationManager();
    }

    public void drawImage(Object graphics, Object img, int x, int y) {
        Bitmap b = (Bitmap) img;
        ((Graphics) graphics).drawBitmap(x, y, b.getWidth(), b.getHeight(), b, 0, 0);
    }

    public void drawRGB(Object graphics, int[] rgbData, int offset, int x, int y, int w, int h, boolean processAlpha) {
        Graphics nativeGraphics = (Graphics) graphics;
        int rgbX = x;
        int rgbY = y;

        //if the x or y are positive simply redirect the call to midp Graphics
        if (rgbX >= 0) {
            if (processAlpha) {
                nativeGraphics.drawARGB(rgbData, offset, w, rgbX, rgbY, w, h);
            } else {
                nativeGraphics.drawRGB(rgbData, offset, w, rgbX, rgbY, w, h);
            }
            return;
        }

        //if the translate causes us to paint out of the bounds
        //we will paint only the relevant rows row by row to avoid some devices bugs
        //such as BB that fails to paint if the coordinates are negative.
        if (rgbX < 0) {
            if (rgbX + w > 0) {
                if (w < rgbData.length) {
                    for (int i = 1; i <= rgbData.length / w; i++) {
                        offset = -rgbX + (w * (i - 1));
                        rgbY++;
                        if (rgbY >= 0) {
                            nativeGraphics.drawARGB(rgbData, offset, (w + rgbX), 0, rgbY, w + rgbX, 1);
                        }
                    }
                }
            }
        }
    }

    public Object getNativeGraphics() {
        return canvas.getGlobalGraphics();
    }

    public Object getNativeGraphics(Object image) {
        return new Graphics((Bitmap) image);
    }

    public boolean isAntiAliasingSupported() {
        return true;
    }

    public void setAntiAliased(Object graphics, boolean a) {
        ((Graphics) graphics).setDrawingStyle(Graphics.DRAWSTYLE_AALINES, a);
        ((Graphics) graphics).setDrawingStyle(Graphics.DRAWSTYLE_AAPOLYGONS, a);
    }

    public boolean isAntiAliased(Object graphics) {
        return ((Graphics) graphics).isDrawingStyleSet(Graphics.DRAWSTYLE_AALINES);
    }

    public boolean isAntiAliasedTextSupported() {
        return false;
    }

    public void setAntiAliasedText(Object graphics, boolean a) {
        // this method uses undocumented behavior so it won't compile on different JDE versions
        /*Graphics g = (Graphics)graphics;
        Font f = g.getFont();
        if(a) {
        if(f.getAntialiasMode() != Font.ANTIALIAS_SUBPIXEL) {
        f = f.derive(f.getStyle(), f.getHeight(), Ui.UNITS_px, Font.ANTIALIAS_SUBPIXEL, f.getEffects());
        g.setFont(f);
        }
        } else {
        if(f.getAntialiasMode() == Font.ANTIALIAS_SUBPIXEL) {
        f = f.derive(f.getStyle(), f.getHeight(), Ui.UNITS_px, Font.ANTIALIAS_NONE, f.getEffects());
        g.setFont(f);
        }
        }*/
    }

    public boolean isAntiAliasedText(Object graphics) {
        // this method uses undocumented behavior so it won't compile on different JDE versions
        /*Graphics g = (Graphics)graphics;
        Font f = g.getFont();
        return f.getAntialiasMode() == Font.ANTIALIAS_SUBPIXEL;*/
        return false;
    }

    public int charsWidth(Object nativeFont, char[] ch, int offset, int length) {
        return font(nativeFont).getAdvance(ch, offset, length);
    }

    private Font font(Object nativeFont) {
        if (nativeFont == null) {
            return Font.getDefault();
        }
        return (Font) nativeFont;
    }

    public int stringWidth(Object nativeFont, String str) {
        return font(nativeFont).getAdvance(str);
    }

    public int charWidth(Object nativeFont, char ch) {
        return font(nativeFont).getAdvance(ch);
    }

    public int getHeight(Object nativeFont) {
        return font(nativeFont).getHeight();
    }

    public Object getDefaultFont() {
        return Font.getDefault();
    }

    public int getFace(Object nativeFont) {
        Font nf = (Font) font(nativeFont);
        int s = nf.getStyle();
        int result = 0;
        if ((s & Font.BOLD) == Font.BOLD) {
            result |= com.codename1.ui.Font.STYLE_BOLD;
        }
        if ((s & Font.ITALIC) == Font.ITALIC) {
            result |= com.codename1.ui.Font.STYLE_ITALIC;
        }
        if ((s & Font.UNDERLINED) == Font.UNDERLINED) {
            result |= com.codename1.ui.Font.STYLE_UNDERLINED;
        }
        return result;
    }

    public int getSize(Object nativeFont) {
        Font nf = (Font) font(nativeFont);
        int bbSize = Font.getDefault().getHeight();
        int diff = Font.getDefault().getHeight() / 3;

        if (nf.getHeight() == bbSize - diff) {
            return com.codename1.ui.Font.SIZE_SMALL;
        }
        if (nf.getHeight() == bbSize + diff) {
            return com.codename1.ui.Font.SIZE_LARGE;
        }
        return com.codename1.ui.Font.SIZE_MEDIUM;
    }

    public int getStyle(Object nativeFont) {
        Font nf = (Font) font(nativeFont);
        int s = nf.getStyle();
        if ((s & FontFamily.MONO_BITMAP_FONT) == FontFamily.MONO_BITMAP_FONT) {
            return com.codename1.ui.Font.FACE_MONOSPACE;
        }
        if ((s & FontFamily.SCALABLE_FONT) == FontFamily.SCALABLE_FONT) {
            return com.codename1.ui.Font.FACE_PROPORTIONAL;
        }
        return com.codename1.ui.Font.FACE_SYSTEM;
    }

    public Object createFont(int face, int style, int size) {
        Font font = Font.getDefault();
        int bbSize = Font.getDefault().getHeight();
        int diff = Font.getDefault().getHeight() / 3;
        switch (size) {
            case com.codename1.ui.Font.SIZE_SMALL:
                bbSize -= diff;
                break;
            case com.codename1.ui.Font.SIZE_LARGE:
                bbSize += diff;
                break;
        }
        int bbStyle = Font.PLAIN;
        switch (style) {
            case com.codename1.ui.Font.STYLE_BOLD:
                bbStyle = Font.BOLD;
                break;
            case com.codename1.ui.Font.STYLE_ITALIC:
                bbStyle = Font.ITALIC;
                break;
            case com.codename1.ui.Font.STYLE_PLAIN:
                bbStyle = Font.PLAIN;
                break;
            case com.codename1.ui.Font.STYLE_UNDERLINED:
                bbStyle = Font.UNDERLINED;
                break;
            default:
                // probably bold/italic
                bbStyle = Font.BOLD | Font.ITALIC;
                break;
        }
        switch (face) {
            case com.codename1.ui.Font.FACE_MONOSPACE:
                font = font.getFontFamily().getFont(bbStyle | FontFamily.MONO_BITMAP_FONT, bbSize);
                break;
            case com.codename1.ui.Font.FACE_PROPORTIONAL:
                font = font.getFontFamily().getFont(bbStyle | FontFamily.SCALABLE_FONT, bbSize);
                break;
            case com.codename1.ui.Font.FACE_SYSTEM:
                font = font.derive(bbStyle, bbSize, Ui.UNITS_px, Font.ANTIALIAS_SUBPIXEL, font.getEffects());
                break;
        }
        return font;
    }

    public boolean isNativeInputSupported() {
        return true;
    }

    public int getKeyboardType() {
        int keyT = Keypad.getHardwareLayout();
        switch (keyT) {
            case Keypad.HW_LAYOUT_LEGACY:
            case Keypad.HW_LAYOUT_32:
            case Keypad.HW_LAYOUT_39:
            case Keypad.HW_LAYOUT_PHONE:
                return Display.KEYBOARD_TYPE_QWERTY;
            case Keypad.HW_LAYOUT_REDUCED:
            case Keypad.HW_LAYOUT_REDUCED_24:
                return Display.KEYBOARD_TYPE_HALF_QWERTY;
            case 1230263636: // HW_LAYOUT_ITUT

                return Display.KEYBOARD_TYPE_NUMERIC;
        }
        return Display.KEYBOARD_TYPE_QWERTY;
    }

    public boolean isMultiTouch() {
        return canvas.isMultiTouch();
    }

    public boolean isClickTouchScreen() {
        return canvas.isClickTouchScreen();
    }

    protected void pointerDragged(final int[] x, final int[] y) {
        super.pointerDragged(x, y);
    }

    protected void pointerPressed(final int[] x, final int[] y) {
        super.pointerPressed(x, y);
    }

    protected void pointerReleased(final int[] x, final int[] y) {
        super.pointerReleased(x, y);
    }

    protected void pointerHover(final int[] x, final int[] y) {
        super.pointerHover(x, y);
    }

    protected void pointerHoverPressed(final int[] x, final int[] y) {
        super.pointerHoverPressed(x, y);
    }

    protected void pointerHoverPressed(final int x, final int y) {
        super.pointerHoverPressed(x, y);
    }

    protected void pointerDragged(final int x, final int y) {
        super.pointerDragged(x, y);
    }

    protected void pointerPressed(final int x, final int y) {
        super.pointerPressed(x, y);
    }

    protected void pointerReleased(final int x, final int y) {
        super.pointerReleased(x, y);
    }

    protected void pointerHoverReleased(final int x, final int y) {
        super.pointerHoverReleased(x, y);
    }

    protected void pointerHoverReleased(final int[] x, final int[] y) {
        super.pointerHoverReleased(x, y);
    }

    protected void pointerHover(final int x, final int y) {
        super.pointerHover(x, y);
    }

    protected void sizeChanged(int w, int h) {
        super.sizeChanged(w, h);
    }

    public boolean minimizeApplication() {
        app.requestBackground();
        return true;
    }

    public void restoreMinimizedApplication() {
        app.requestForeground();
    }

    public boolean isThirdSoftButton() {
        return false;
    }

    /**
     * Indicates whether the application should minimize or exit when the end key is pressed
     *
     * @return the minimizeOnEnd
     */
    public static boolean isMinimizeOnEnd() {
        return minimizeOnEnd;
    }

    /**
     * Indicates whether the application should minimize or exit when the end key is pressed
     * 
     * @param aMinimizeOnEnd the minimizeOnEnd to set
     */
    public static void setMinimizeOnEnd(boolean aMinimizeOnEnd) {
        minimizeOnEnd = aMinimizeOnEnd;
    }

    /**
     * Volume listener is invoked when the volume up/down buttons on the blackberry
     * device are pressed. The key event would be either Characters.CONTROL_VOLUME_UP
     * or Characters.CONTROL_VOLUME_DOWN
     *
     * @param al action listener callback
     */
    public static void addVolumeListener(ActionListener al) {
        if (volumeListener == null) {
            volumeListener = new EventDispatcher();
        }
        volumeListener.addListener(al);
    }

    /**
     * Remove the volume listener instance
     *
     * @param al action listener to remove
     */
    public static void removeVolumeListener(ActionListener al) {
        if (volumeListener != null) {
            volumeListener.removeListener(al);
            if (volumeListener.getListenerVector() == null || volumeListener.getListenerVector().size() == 0) {
                volumeListener = null;
            }
        }
    }

    static EventDispatcher getVolumeListener() {
        return volumeListener;
    }
    
    class FinishEditFocus implements FocusChangeListener {
        public void focusChanged(Field field, int eventType) {
            if (lightweightEdit != null) {
                finishEdit(false);
            }
        }
    }

    class PeerFocus implements FocusChangeListener {
        private Field fld;
        private PeerComponent p;
        PeerFocus(Field fld, PeerComponent p) {
            this.fld = fld;
            this.p = p;
        }
        public void focusChanged(Field field, int eventType) {
            if (p.getNativePeer() == fld && eventType == FocusChangeListener.FOCUS_LOST) {
                fld.setFocusListener(null);
                nullFld.setFocus();
                canvas.eventTarget = null;
                canvas.repeatLastNavigation();
            }
        }
    }
    
    /**
     * @inheritDoc
     */
    public PeerComponent createNativePeer(Object nativeComponent) {
        if (nativeComponent instanceof Field) {
            if (nullFld == null) {
                nullFld = new NullField();
                nullFld.setFocusListener(new FinishEditFocus());
                synchronized (UiApplication.getEventLock()) {
                    canvas.add(nullFld);
                }
            }
            final Field fld = (Field) nativeComponent;
            final PeerComponent peer = new PeerComponent(fld) {

                public boolean isFocusable() {
                    if (fld != null) {
                        return fld.isFocusable();
                    }
                    return super.isFocusable();
                }

                public void setFocus(boolean b) {
                    if (hasFocus() == b) {
                        return;
                    }
                    if (b) {
                        canvas.eventTarget = fld;
                        fld.setFocusListener(new PeerFocus(fld, this));
                    } else {
                        fld.setFocusListener(null);
                        if (canvas.eventTarget == fld) {
                            canvas.eventTarget = null;
                        }
                    }
                    if (isInitialized()) {
                        InvokeLaterWrapper i = new InvokeLaterWrapper(INVOKE_AND_WAIT_setFocus);
                        i.val = b;
                        i.fld = fld;
                        app.invokeAndWait(i);
                    } else {
                        super.setFocus(b);
                    }
                }

                public boolean animate() {
                    if (fld.isDirty()) {
                        repaint();
                    }
                    return super.animate();
                }

                protected Dimension calcPreferredSize() {
                    InvokeLaterWrapper i = new InvokeLaterWrapper(INVOKE_AND_WAIT_calcPreferredSize);
                    i.dim = new Dimension();
                    i.fld = fld;
                    app.invokeAndWait(i);
                    return i.dim;
                }

                protected void onPositionSizeChange() {
                    InvokeLaterWrapper i = new InvokeLaterWrapper(INVOKE_LATER_dirty);
                    i.fld = fld;
                    app.invokeLater(i);
                }

                protected void initComponent() {
                    fieldComponentMap.put(fld, this);
                    InvokeLaterWrapper i = new InvokeLaterWrapper(INVOKE_LATER_initComponent);
                    i.fld = fld;
                    app.invokeLater(i);
                    setFocus(super.hasFocus());
                    if (hasFocus()) {
                        canvas.eventTarget = fld;
                    }
                    getComponentForm().registerAnimated(this);
                }

                protected void deinitialize() {
                    getComponentForm().deregisterAnimated(this);
                    canvas.eventTarget = null;
                    InvokeLaterWrapper i = new InvokeLaterWrapper(INVOKE_LATER_deinitialize);
                    i.fld = fld;
                    app.invokeLater(i);
                    fieldComponentMap.remove(fld);
                }
            };
            fieldComponentMap.put(fld, peer);
            return peer;
        }

        throw new IllegalArgumentException(nativeComponent.getClass().getName());
    }

    /**
     * @inheritDoc
     */
    public void showNativeScreen(Object nativeFullScreenPeer) {
        InvokeLaterWrapper i = new InvokeLaterWrapper(INVOKE_LATER_showNativeScreen);
        i.fld = (Screen) nativeFullScreenPeer;
        app.invokeLater(i);
    }

    /**
     * #######################################################################
     * #######################################################################
     *
     * see editString() method
     */
    private class EditPopup extends PopupScreen implements FocusChangeListener, Runnable {

        protected final TextArea lightweightEdit;
        private boolean finished = false;
        private boolean cancel = false;
        private String okString;
        private String cancelString;
        protected final BasicEditField nativeEdit;

        protected EditPopup(TextArea lightweightEdit, int maxSize) {
            super(new VerticalFieldManager(VerticalFieldManager.VERTICAL_SCROLL), Field.FOCUSABLE | Field.EDITABLE | Screen.DEFAULT_MENU);

            UIManager m = UIManager.getInstance();
            okString = m.localize("ok", "OK");
            cancelString = m.localize("cancel", "Cancel");
            this.lightweightEdit = lightweightEdit;
            long type = 0;
            int constraint = lightweightEdit.getConstraint();
            if ((constraint & TextArea.DECIMAL) == TextArea.DECIMAL) {
                type = BasicEditField.FILTER_REAL_NUMERIC;
            } else if ((constraint & TextArea.EMAILADDR) == TextArea.EMAILADDR) {
                type = BasicEditField.FILTER_EMAIL;
            } else if ((constraint & TextArea.NUMERIC) == TextArea.NUMERIC) {
                type = BasicEditField.FILTER_NUMERIC;
            } else if ((constraint & TextArea.PHONENUMBER) == TextArea.PHONENUMBER) {
                type = BasicEditField.FILTER_PHONE;
            } else if ((constraint & TextArea.NON_PREDICTIVE) == TextArea.NON_PREDICTIVE) {
                type = BasicEditField.NO_COMPLEX_INPUT;
            }


            if (lightweightEdit.isSingleLineTextArea()) {
                type |= BasicEditField.NO_NEWLINE;
            }

            if ((constraint & TextArea.PASSWORD) == TextArea.PASSWORD) {
                nativeEdit = new BBPasswordEditField(lightweightEdit, type, maxSize);
            } else {
                nativeEdit = new BBEditField(lightweightEdit, type, maxSize);
            }
            
            
            // using Field.EDITABLE flag now because of bug with DevTrack ID 354265 at
            // https://www.blackberry.com/jira/browse/JAVAAPI-101
            //nativeEdit.setEditable(true);
            net.rim.device.api.ui.Font f = nativeEdit.getFont();
            if (f.getHeight() > lightweightEdit.getStyle().getFont().getHeight()) {
                nativeEdit.setFont(f.derive(f.getStyle(),
                        lightweightEdit.getStyle().getFont().getHeight()));
            }
            add(nativeEdit);
            nativeEdit.setFocus();
            nativeEdit.setFocusListener(this);
        }

        protected void makeMenu(Menu menu, int arg1) {
            menu.add(new MenuItem(okString, 0, 99) {

                public void run() {
                    finishEdit();
                }
            });
            menu.add(new MenuItem(cancelString, 1, 100) {

                public void run() {
                    EditPopup.this.cancel = true;
                    finishEdit();
                }
            });
            super.makeMenu(menu, arg1);
        }

        protected void onMenuDismissed(Menu arg0) {
            super.onMenuDismissed(arg0);
            /**
             * at this point the EDT is still blocked, so we repaint the
             * screen with our old buffer and request some fresh paint
             * for the textfield.
             */
            this.invalidate();
        }

        protected void onExposed() {
            super.onExposed();
            /**
             * we reach this if the context menu opened another dialog
             * on top.
             *
             * at this point the EDT is still blocked, so we repaint the
             * screen with our old buffer and request some fresh paint
             * for the textfield.
             */
            this.invalidate();
        }

        protected boolean keyDown(int keycode, int time) {
            if (Keypad.key(keycode) == Keypad.KEY_ESCAPE) {
                this.cancel = true;
                finishEdit();
                return true;
            }
            if (Keypad.key(keycode) == Keypad.KEY_ENTER) {
                /**
                 * single line text field accept input.
                 */
                if (this.lightweightEdit.isSingleLineTextArea()) {
                    finishEdit();
                    return true;
                }
            }
            return super.keyDown(keycode, time);
        }

        public void focusChanged(Field field, int type) {
            if (field == this.nativeEdit && type == FOCUS_LOST) {
                finishEdit();
            }
        }

        protected void startEdit() {
            BlackBerryImplementation.this.app.invokeAndWait(this);
        }

        public void run() {
            UiApplication.getUiApplication().pushScreen(this);
        }

        protected void finishEdit() {
            if (this.finished) {
                return;
            }
            this.finished = true;
            if (!this.cancel) {
                Display.getInstance().onEditingComplete(lightweightEdit, nativeEdit.getText());
            }
            if (isTouchDevice()) {
                UiApplication.getUiApplication().invokeLater(new Runnable() {

                    public void run() {
                        Display.getInstance().setShowVirtualKeyboard(false);
                    }
                });
            }
            UiApplication.getUiApplication().popScreen(this);
        }
    }

    /**
     * #######################################################################
     * #######################################################################
     *
     * see editString() method
     */
    private class BBEditField extends ActiveAutoTextEditField {

        private TextArea lightweightEdit = null;
        private NativeEditCallback callback = new NativeEditCallback();

        public BBEditField(TextArea lightweightEdit, long type, int maxSize) {
            super("", lightweightEdit.getText(), maxSize, Field.EDITABLE | Field.FOCUSABLE | Field.SPELLCHECKABLE | type);
            this.lightweightEdit = lightweightEdit;

        }

        public void paintBackground(net.rim.device.api.ui.Graphics g) {
            g.setBackgroundColor(lightweightEdit.getSelectedStyle().getBgColor());
            g.setColor(lightweightEdit.getSelectedStyle().getBgColor());
            super.paintBackground(g);
            g.setColor(lightweightEdit.getSelectedStyle().getBgColor());
            g.fillRect(0, 0, this.getWidth(),
                    this.getHeight());
        }

        public void paint(net.rim.device.api.ui.Graphics g) {
            g.setBackgroundColor(lightweightEdit.getSelectedStyle().getBgColor());
            g.setColor(lightweightEdit.getSelectedStyle().getFgColor());
            super.paint(g);
        }

        protected boolean keyDown(int keycode, int time) {
            if (Keypad.key(keycode) == Keypad.KEY_ESCAPE) {
                finishEdit(true);
                return true;
            }
            if (Keypad.key(keycode) == Keypad.KEY_ENTER) {
                if (lightweightEdit.isSingleLineTextArea()) {
                    finishEdit(false);
                    return true;
                }
            }
            return super.keyDown(keycode, time);
        }

        protected boolean keyUp(int keycode, int time) {
            boolean b = super.keyUp(keycode, time);
            callback.onChanged();
            return b;
        }

        protected boolean navigationMovement(int dx, int dy, int status, int time) {
            boolean b = super.navigationMovement(dx, dy, status, time);
            callback.onChanged();
            return b;
        }

        protected void update(int i) {
            super.update(i);
            lightweightEdit.setText(getText());
            callback.onChanged();
        }
    }

    /**
     * #######################################################################
     * #######################################################################
     *
     * see editString() method
     */
    private class BBPasswordEditField extends PasswordEditField {

        private TextArea lightweightEdit = null;
        private NativeEditCallback callback = new NativeEditCallback();

        public BBPasswordEditField(TextArea lightweightEdit, long type, int maxSize) {
            super("", lightweightEdit.getText(), maxSize, Field.EDITABLE | Field.FOCUSABLE | type);
            this.lightweightEdit = lightweightEdit;
        }

        public void paintBackground(net.rim.device.api.ui.Graphics g) {
            g.setBackgroundColor(lightweightEdit.getSelectedStyle().getBgColor());
            g.setColor(lightweightEdit.getSelectedStyle().getBgColor());
            super.paintBackground(g);
            g.setColor(lightweightEdit.getSelectedStyle().getBgColor());
            g.fillRect(0, 0, this.getWidth(),
                    this.getHeight());
        }

        public void paint(net.rim.device.api.ui.Graphics g) {
            g.setBackgroundColor(lightweightEdit.getSelectedStyle().getBgColor());
            g.setColor(lightweightEdit.getSelectedStyle().getFgColor());
            super.paint(g);
        }

        protected boolean keyDown(int keycode, int time) {
            if (Keypad.key(keycode) == Keypad.KEY_ESCAPE) {
                finishEdit(true);
                return true;
            }
            if (Keypad.key(keycode) == Keypad.KEY_ENTER) {
                if (lightweightEdit.isSingleLineTextArea()) {
                    finishEdit(false);
                    return true;
                }
            }
            return super.keyDown(keycode, time);
        }

        protected boolean keyUp(int keycode, int time) {
            callback.onChanged();
            return super.keyUp(keycode, time);
        }

        protected boolean navigationMovement(int dx, int dy, int status, int time) {
            callback.onChanged();
            return super.navigationMovement(dx, dy, status, time);
        }
        
        protected void update(int i) {
            super.update(i);
            lightweightEdit.setText(getText());
            callback.onChanged();
        }
    }

    class NativeEditCallback implements Runnable {

        private String text;
        private int cursorLocation;

        public void onChanged() {
            if (nativeEdit != null) {
                text = nativeEdit.getText();
                cursorLocation = nativeEdit.getCursorPosition();
                Display.getInstance().callSerially(this);
            }
        }

        public void run() {
            // prevent a race condition by copying the global scope
            TextArea t = lightweightEdit;
            if (t != null) {
                Dimension d = t.getPreferredSize();
                t.setText(text);
                Dimension current = t.getPreferredSize();

                Form f = t.getComponentForm();

                // allows the lightweight text area to grow to fill up the screen
                if (d.getHeight() != current.getHeight() || d.getWidth() != current.getWidth()) {
                    f.getComponentForm().revalidate();
                }

                int currentLineLength = 0;
                int numOfChars = 0;
                int currentY = 0;
                while (numOfChars <= cursorLocation && currentY < t.getLines()) {
                    String currentLine = t.getTextAt(currentY);
                    currentLineLength = currentLine.length();
                    if (numOfChars + currentLineLength < text.length()
                            && (text.charAt(numOfChars + currentLineLength) == '\n'
                            || text.charAt(numOfChars + currentLineLength) == ' ')) {
                        currentLineLength++;
                    }
                    numOfChars += currentLineLength;
                    currentY++;
                }
                int cursorY = Math.max(0, currentY - 1);
                com.codename1.ui.Font textFont = t.getStyle().getFont();
                int rowsGap = t.getRowsGap();
                int lineHeight = textFont.getHeight() + rowsGap;
                t.scrollRectToVisible(t.getScrollX(), cursorY * lineHeight, t.getWidth(), lineHeight, t);
                
                app.invokeLater(new Runnable() {
                    public void run() {
                        canvas.updateRIMLayout();
                    }
                });
            }
        }
    }
    
    protected int getFieldWidth(Field fld) {
        return Math.max(fld.getWidth(), fld.getPreferredWidth());
    }

    protected int getFieldHeight(Field fld) {
        return Math.max(fld.getHeight(), fld.getPreferredHeight());
    }

    /**
     * This class is here to unify multiple invocations of invoke later
     */
    class InvokeLaterWrapper implements Runnable {

        public Field fld;
        public Dimension dim;
        public boolean val;
        private int i;
        public Player v;

        public InvokeLaterWrapper(int i) {
            this.i = i;
        }

        public void run() {
            switch (i) {
                case INVOKE_LATER_confirmControlView:
                    if (app.getActiveScreen() != canvas) {
                        app.pushScreen(canvas);
                    }
                    break;

                case INVOKE_LATER_finishEdit:
                    finishEdit(val);
                    break;

                case INVOKE_LATER_initComponent:
                    canvas.add(fld);
                    break;

                case INVOKE_LATER_deinitialize:
                    canvas.delete(fld);
                    break;

                case INVOKE_LATER_showNativeScreen:
                    if (app.getActiveScreen() != fld) {
                        app.pushScreen((Screen) fld);
                    }
                    break;

                case INVOKE_AND_WAIT_calcPreferredSize:
                    dim.setWidth(getFieldWidth(fld));
                    dim.setHeight(getFieldHeight(fld));
                    break;

                case INVOKE_AND_WAIT_setFocus:
                    try {
                        // Not sure why an exception is thrown here during a touch event... 
                        if (val) {
                            fld.setFocus();
                        } else {
                            nullFld.setFocus();
                        }
                    } catch (Throwable t) {
                        //t.printStackTrace();
                    }
                    break;
                case INVOKE_LATER_dirty:
                    if (fld.getScreen() == canvas) {
                        fld.setDirty(true);
                        canvas.updateRIMLayout();
                    }
                    break;
                case INVOKE_LATER_startMedia:
                    try {
                        v.start();
                    } catch (MediaException ex) {
                        ex.printStackTrace();
                    }
                    break;
            }
        }
    }

    public void execute(String url) {
        app.setWaitingForReply(true);
        // requires signing
        net.rim.blackberry.api.browser.BrowserSession browserSession = net.rim.blackberry.api.browser.Browser.getDefaultSession();
        browserSession.displayPage(url);
        browserSession.showBrowser();
    }

    public void exitApplication() {
        System.exit(0);
    }

    public String getProperty(String key, String defaultValue) {
        String val = null;
        if ("OS".equals(key)) {
            return "RIM";
        }
        
        if ("IMEI".equals(key)) {
            return GPRSInfo.imeiToString(GPRSInfo.getIMEI());
        }
        
        //requires signing
        if ("MSISDN".equals(key)) {
            return Phone.getDevicePhoneNumber(true);
        }
        if ("AppVersion".equals(key)) {
            return ApplicationDescriptor.currentApplicationDescriptor().getVersion();
        }
        if (initGetProperty) {
            initGetProperty = false;
            ApplicationDescriptor ad = ApplicationDescriptor.currentApplicationDescriptor();
            if (ad != null) {
                String moduleName = ad.getModuleName();

                if (moduleName != null) {
                    CodeModuleGroup[] allGroups = CodeModuleGroupManager.loadAll();
                    if (allGroups != null) {
                        for (int i = 0; i < allGroups.length; i++) {
                            if (allGroups[i].containsModule(moduleName)) {
                                group = allGroups[i];
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (group != null) {
            val = group.getProperty(key);
        }

        if (val == null) {
            return defaultValue;
        }
        return val;
    }

    /**
     * @inheritDoc
     */
    public void playBuiltinSound(String soundIdentifier) {
        if (!playUserSound(soundIdentifier)) {
            // todo...
        }
    }

    /**
     * @inheritDoc
     */
    protected void playNativeBuiltinSound(Object data) {
        try {
            try {
                Media m = createMedia(new ByteArrayInputStream((byte[]) data), "audio/mpeg", null);
                m.play();
            } catch (Exception err) {
                // some simulators take issue with the audio/mpeg string but the mp3 string
                // works fine
                Media m = createMedia(new ByteArrayInputStream((byte[]) data), "audio/mp3", null);
                m.play();
            }
        } catch (IOException ex) {
            // not likely since the stream is a byte array input stream
            ex.printStackTrace();
        }
    }

    /**
     * @inheritDoc
     */
    public boolean isBuiltinSoundAvailable(String soundIdentifier) {
        if (soundIdentifier.equals(Display.SOUND_TYPE_ALARM)) {
            return true;
        }
        if (soundIdentifier.equals(Display.SOUND_TYPE_CONFIRMATION)) {
            return true;
        }
        if (soundIdentifier.equals(Display.SOUND_TYPE_ERROR)) {
            return true;
        }
        if (soundIdentifier.equals(Display.SOUND_TYPE_INFO)) {
            return true;
        }
        if (soundIdentifier.equals(Display.SOUND_TYPE_WARNING)) {
            return true;
        }
        return super.isBuiltinSoundAvailable(soundIdentifier);
    }
    private boolean testedNativeTheme;
    private boolean nativeThemeAvailable;

    public boolean hasNativeTheme() {
        if (!testedNativeTheme) {
            testedNativeTheme = true;
            try {
                InputStream is = getResourceAsStream(getClass(), "/blackberry_theme.res");
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
     * Installs the native theme, this is only applicable if hasNativeTheme() returned true. Notice that this method
     * might replace the DefaultLookAndFeel instance and the default transitions.
     */
    public void installNativeTheme() {
        if (nativeThemeAvailable) {
            try {
                InputStream is = getResourceAsStream(getClass(), "/blackberry_theme.res");
                Resources r = Resources.open(is);
                UIManager.getInstance().setThemeProps(r.getTheme(r.getThemeResourceNames()[0]));
                is.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * @inheritDoc
     */
    public boolean isAPSupported() {
        return true;
    }

    private Vector getValidSBEntries() {
        Vector v = new Vector();
        ServiceBook bk = ServiceBook.getSB();
        ServiceRecord[] recs = bk.getRecords();
        for (int iter = 0; iter < recs.length; iter++) {
            ServiceRecord sr = recs[iter];
            if (sr.isValid() && !sr.isDisabled() && sr.getUid() != null
                    && sr.getUid().length() != 0) {
                v.addElement(sr);
            }
        }
        return v;
    }

    /**
     * @inheritDoc
     */
    public String[] getAPIds() {
        Vector v = getValidSBEntries();
        String[] s = new String[v.size()];
        for (int iter = 0; iter < s.length; iter++) {
            s[iter] = "" + ((ServiceRecord) v.elementAt(iter)).getUid();
        }
        return s;
    }

    /**
     * @inheritDoc
     */
    public int getAPType(String id) {
        Vector v = getValidSBEntries();
        for (int iter = 0; iter < v.size(); iter++) {
            ServiceRecord r = (ServiceRecord) v.elementAt(iter);
            if (("" + r.getUid()).equals(id)) {
                if (r.getUid().toLowerCase().indexOf("wifi") > -1) {
                    return NetworkManager.ACCESS_POINT_TYPE_WLAN;
                }
                // wap2
                if (r.getCid().toLowerCase().indexOf("wptcp") > -1) {
                    return NetworkManager.ACCESS_POINT_TYPE_NETWORK3G;
                }
                return NetworkManager.ACCESS_POINT_TYPE_UNKNOWN;
            }
        }
        return NetworkManager.ACCESS_POINT_TYPE_UNKNOWN;
    }

    /**
     * @inheritDoc
     */
    public String getAPName(String id) {
        Vector v = getValidSBEntries();
        for (int iter = 0; iter < v.size(); iter++) {
            ServiceRecord r = (ServiceRecord) v.elementAt(iter);
            if (("" + r.getUid()).equals(id)) {
                return r.getName();
            }
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public String getCurrentAccessPoint() {
        if (currentAccessPoint != null) {
            return currentAccessPoint;
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public void setCurrentAccessPoint(String id) {
        currentAccessPoint = id;
        int t = getAPType(id);
        deviceSide = t == NetworkManager.ACCESS_POINT_TYPE_NETWORK3G || 
                t == NetworkManager.ACCESS_POINT_TYPE_NETWORK2G || 
                t == NetworkManager.ACCESS_POINT_TYPE_WLAN;
    }

    /**
     * @inheritDoc
     */
    public boolean shouldAutoDetectAccessPoint() {
        return false;
    }
    
    /**
     * @inheritDoc
     */
    public Object connect(String url, boolean read, boolean write) throws IOException {
        if (currentAccessPoint != null) {
            url += ";ConnectionUID=" + currentAccessPoint;
            if (deviceSide) {
                url += ";deviceside=true";
            }
        } else {
            URLFactory uu = new URLFactory(url);
            TransportDetective td = new TransportDetective();
            int tr = td.getBestTransportForActiveCoverage();
            switch(tr) {
            case TransportDetective.TRANSPORT_TCP_WIFI:
                url = uu.getHttpTcpWiFiUrl();
                break;
            case TransportDetective.TRANSPORT_MDS:
                url = uu.getHttpMdsUrl(false);
                break;
            case TransportDetective.TRANSPORT_BIS_B:
                url = uu.getHttpBisUrl();
                break;
            case TransportDetective.TCP_CELLULAR_APN_SERVICE_BOOK:
            case TransportDetective.TRANSPORT_TCP_CELLULAR:
                url = uu.getHttpTcpCellularUrl(null, null, null);
                break;
            }
        }
        return connectImpl(url, read, write);
    }

    private Object connectImpl(String url, boolean read, boolean write) throws IOException {
        int mode;
        if (read && write) {
            mode = Connector.READ_WRITE;
        } else {
            if (write) {
                mode = Connector.WRITE;
            } else {
                mode = Connector.READ;
            }
        }
        return Connector.open(url, mode);
    }

    /**
     * @inheritDoc
     */
    public void setHeader(Object connection, String key, String val) {
        try {
            ((HttpConnection) connection).setRequestProperty(key, val);
        } catch (IOException err) {
            // this exception doesn't make sense since at this point no connection is in place
            err.printStackTrace();
        }
    }

    /**
     * @inheritDoc
     */
    public int getContentLength(Object connection) {
        return (int) ((HttpConnection) connection).getLength();
    }

    /**
     * @inheritDoc
     */
    public void cleanup(Object o) {
        try {
            if (o != null) {
                if (o instanceof Connection) {
                    ((Connection) o).close();
                    return;
                }
                if (o instanceof RecordEnumeration) {
                    ((RecordEnumeration) o).destroy();
                    return;
                }
                if (o instanceof RecordStore) {
                    ((RecordStore) o).closeRecordStore();
                    return;
                }
                super.cleanup(o);
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @inheritDoc
     */
    public OutputStream openOutputStream(Object connection) throws IOException {
        if (connection instanceof String) {
            FileConnection fc = (FileConnection) Connector.open((String) connection, Connector.READ_WRITE);
            if (!fc.exists()) {
                fc.create();
            }
            BufferedOutputStream o = new BufferedOutputStream(fc.openOutputStream(), (String) connection);
            o.setConnection(fc);
            return o;
        }
        OutputStream os = new BlackBerryOutputStream(((HttpConnection) connection).openOutputStream());
        return new BufferedOutputStream(os, ((HttpConnection) connection).getURL());
    }

    /**
     * @inheritDoc
     */
    public OutputStream openOutputStream(Object connection, int offset) throws IOException {
        FileConnection fc = (FileConnection) Connector.open((String) connection, Connector.READ_WRITE);
        if (!fc.exists()) {
            fc.create();
        }
        BufferedOutputStream o = new BufferedOutputStream(fc.openOutputStream(offset), (String) connection);
        o.setConnection(fc);
        return o;
    }

    /**
     * @inheritDoc
     */
    public InputStream openInputStream(Object connection) throws IOException {
        if (connection instanceof String) {
            FileConnection fc = (FileConnection) Connector.open((String) connection, Connector.READ);
            BufferedInputStream o = new BufferedInputStream(fc.openInputStream(), (String) connection);
            o.setConnection(fc);
            return o;
        }
        return new BufferedInputStream(((HttpConnection) connection).openInputStream(), ((HttpConnection) connection).getURL());
    }

    /**
     * @inheritDoc
     */
    public void setPostRequest(Object connection, boolean p) {
        try {
            if (p) {
                ((HttpConnection) connection).setRequestMethod(HttpConnection.POST);
            } else {
                ((HttpConnection) connection).setRequestMethod(HttpConnection.GET);
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
        return ((HttpConnection) connection).getResponseCode();
    }

    /**
     * @inheritDoc
     */
    public String getResponseMessage(Object connection) throws IOException {
        return ((HttpConnection) connection).getResponseMessage();
    }

    /**
     * @inheritDoc
     */
    public String getHeaderField(String name, Object connection) throws IOException {
        return ((HttpConnection) connection).getHeaderField(name);
    }
    
    /**
     * @inheritDoc
     */
    public String[] getHeaderFieldNames(Object connection) throws IOException {
        HttpConnection c = (HttpConnection)connection;
        Vector r = new Vector();
        int i = 0;
        String key = c.getHeaderFieldKey(i);
        while (key != null) {
            if(r.indexOf(key) < 0) {
                r.addElement(key);
            }
            i++;
            key = c.getHeaderFieldKey(i);
        }
        
        if(r.size() == 0) {
            return null;
        }
        String[] response = new String[r.size()];
        for(int iter = 0 ; iter < response.length ; iter++) {
            response[iter] = (String)r.elementAt(iter);
        }
        return response;
    }

    /**
     * @inheritDoc
     */
    public String[] getHeaderFields(String name, Object connection) throws IOException {
        HttpConnection c = (HttpConnection) connection;
        Vector r = new Vector();
        int i = 0;
        while (c.getHeaderFieldKey(i) != null) {
            if (c.getHeaderFieldKey(i).equalsIgnoreCase(name)) {
                String val = c.getHeaderField(i);
                r.addElement(val);
            }
            i++;
        }

        if (r.size() == 0) {
            return null;
        }
        String[] response = new String[r.size()];
        for (int iter = 0; iter < response.length; iter++) {
            response[iter] = (String) r.elementAt(iter);
        }
        return response;
    }

    /**
     * @inheritDoc
     */
    public void deleteStorageFile(String name) {
        Short key = (Short) fat.get(name);
        fat.remove(name);
        resaveFat();
        if (key != null) {
            try {
                for (char c = 'A'; c < 'Z'; c++) {
                    RecordStore.deleteRecordStore("" + c + key);
                }
            } catch (RecordStoreException e) {
            }
        }
    }

    private void resaveFat() {
        RecordStore r = null;
        RecordEnumeration e = null;
        try {
            r = RecordStore.openRecordStore("FAT", true);
            Vector keys = new Vector();
            Enumeration fatKeys = fat.keys();
            while (fatKeys.hasMoreElements()) {
                keys.addElement(fatKeys.nextElement());
            }
            e = r.enumerateRecords(null, null, false);
            while (e.hasNextElement()) {
                int recordId = e.nextRecordId();
                byte[] rec = r.getRecord(recordId);
                ByteArrayInputStream bi = new ByteArrayInputStream(rec);
                DataInputStream di = new DataInputStream(bi);
                String name = di.readUTF();
                short key = di.readShort();
                di.close();
                bi.close();
                Short fatKey = (Short) fat.get(name);
                if (fatKey == null) {
                    // we need to remove this record...
                    r.deleteRecord(recordId);
                } else {
                    // we need to update the record
                    if (fatKey.shortValue() != key) {
                        byte[] bd = toRecord(name, fatKey.shortValue());
                        r.setRecord(recordId, bd, 0, bd.length);
                    }
                }
                keys.removeElement(name);
            }
            e.destroy();
            e = null;

            Enumeration remainingKeys = keys.elements();
            while (remainingKeys.hasMoreElements()) {
                String name = (String) remainingKeys.nextElement();
                Short key = (Short) fat.get(name);
                byte[] bd = toRecord(name, key.shortValue());
                r.addRecord(bd, 0, bd.length);
            }
            r.closeRecordStore();
        } catch (Exception err) {
            // This might be a valid exception and some platforms (e..g. RIM) don't respond well to PST
            //err.printStackTrace();
            cleanup(e);
            cleanup(r);
        }
    }

    private byte[] toRecord(String name, short key) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        DataOutputStream d = new DataOutputStream(bo);
        d.writeUTF(name);
        d.writeShort(key);
        d.close();
        bo.close();
        return bo.toByteArray();
    }

    /**
     * @inheritDoc
     */
    public OutputStream openFileOutputStream(String file) throws IOException {
        return openOutputStream(file);
    }

    /**
     * @inheritDoc
     */
    public InputStream openFileInputStream(String file) throws IOException {
        return openInputStream(file);
    }

    private class RMSOutputStream extends OutputStream {

        private short key;
        private char letter = 'A';
        private ByteArrayOutputStream cache;
        private int offset;

        public RMSOutputStream(short key) {
            this.key = key;

            // first we need to cleanup existing files
            try {
                for (char c = 'A'; c < 'Z'; c++) {
                    RecordStore.deleteRecordStore("" + c + key);
                }
            } catch (RecordStoreException e) {
            }

            cache = new ByteArrayOutputStream();
        }

        public void close() throws IOException {
            flush();
            cache = null;
        }

        public void flush() throws IOException {
            if (cache != null) {
                byte[] data = cache.toByteArray();
                if (data.length > 0) {
                    RecordStore r = null;
                    try {
                        r = RecordStore.openRecordStore("" + letter + key, true);
                        r.addRecord(data, 0, data.length);
                        r.closeRecordStore();
                        if (letter == 'Z') {
                            letter = 'a';
                        } else {
                            letter++;
                        }
                        cache.reset();
                    } catch (RecordStoreException ex) {
                        ex.printStackTrace();
                        cleanup(r);
                        throw new IOException(ex.toString());
                    }
                }
            }
        }

        public void write(byte[] arg0) throws IOException {
            cache.write(arg0);
            if (cache.size() > 32536) {
                flush();
            }
        }

        public void write(byte[] arg0, int arg1, int arg2) throws IOException {
            cache.write(arg0, arg1, arg2);
            if (cache.size() > 32536) {
                flush();
            }
        }

        public void write(int arg0) throws IOException {
            cache.write(arg0);
            if (cache.size() > 32536) {
                flush();
            }
        }
    }

    private class RMSInputStream extends InputStream {

        private InputStream current;
        private int offset;
        private short key;

        public RMSInputStream(short key) throws IOException {
            this.key = key;
            RecordStore r = null;
            RecordEnumeration e = null;
            char letter = 'A';
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                r = open("" + letter + key);
                while (r != null) {
                    e = r.enumerateRecords(null, null, false);
                    while (e.hasNextElement()) {
                        byte[] data = e.nextRecord();
                        os.write(data);
                    }
                    e.destroy();
                    r.closeRecordStore();
                    letter++;
                    r = open("" + letter + key);
                    if (letter == 'Z') {
                        letter = 'a' - ((char) 1);
                    }
                }
                os.close();
                current = new ByteArrayInputStream(os.toByteArray());
            } catch (RecordStoreException ex) {
                ex.printStackTrace();
                cleanup(r);
                cleanup(e);
                throw new IOException(ex.toString());
            }
        }

        private RecordStore open(String s) {
            try {
                return RecordStore.openRecordStore(s, false);
            } catch (RecordStoreException ex) {
                return null;
            }
        }

        public long skip(long n) throws IOException {
            return super.skip(n);
        }

        public void close() throws IOException {
            current.close();
        }

        public int read(byte[] arg0) throws IOException {
            int r = current.read(arg0);
            offset += r;
            return r;
        }

        public int read(byte[] arg0, int arg1, int arg2) throws IOException {
            int r = current.read(arg0, arg1, arg2);
            offset += r;
            return r;
        }

        public int read() throws IOException {
            offset++;
            return current.read();
        }
    }

    /**
     * @inheritDoc
     */
    public OutputStream createStorageOutputStream(String name) throws IOException {
        RecordStore r = null;
        RMSOutputStream os = null;
        DataOutputStream out = null;
        try {
            Short key = (Short) fat.get(name);
            if (key == null) {
                // need to add a key to the FAT
                key = new Short(currentKey);
                fat.put(name, key);
                r = RecordStore.openRecordStore("FAT", true);
                byte[] data = toRecord(name, currentKey);
                currentKey++;
                r.addRecord(data, 0, data.length);
                r.closeRecordStore();
                r = null;
            }
            os = new RMSOutputStream(key.shortValue());
            return os;
        } catch (Exception err) {
            cleanup(r);
            cleanup(os);
            cleanup(out);
            throw new IOException(err.getMessage());
        }
    }

    /**
     * @inheritDoc
     */
    public InputStream createStorageInputStream(String name) throws IOException {
        Short key = (Short) fat.get(name);
        if (key == null) {
            return null;
        }

        try {
            return new RMSInputStream(key.shortValue());
        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }

    /**
     * @inheritDoc
     */
    public boolean storageFileExists(String name) {
        if (name == null) {
            return false;
        }
        return fat.containsKey(name);
    }

    /**
     * @inheritDoc
     */
    public String[] listStorageEntries() {
        String[] a = new String[fat.size()];
        Enumeration e = fat.keys();
        int i = 0;
        while (e.hasMoreElements()) {
            a[i] = (String) e.nextElement();
            i++;
        }
        return a;
    }

    /**
     * @inheritDoc
     */
    public String[] listFilesystemRoots() {
        String[] res = enumToStringArr(FileSystemRegistry.listRoots());
        for (int iter = 0; iter < res.length; iter++) {
            res[iter] = "file:///" + res[iter];
        }
        return res;
    }

    private String[] enumToStringArr(Enumeration e) {
        Vector v = new Vector();
        while (e.hasMoreElements()) {
            v.addElement(e.nextElement());
        }
        String[] response = new String[v.size()];
        for (int iter = 0; iter < response.length; iter++) {
            response[iter] = (String) v.elementAt(iter);
        }
        return response;
    }

    /**
     * @inheritDoc
     */
    public String[] listFiles(String directory) throws IOException {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(directory, Connector.READ);
            return enumToStringArr(fc.list());
        } finally {
            cleanup(fc);
        }
    }

    /**
     * @inheritDoc
     */
    public long getRootSizeBytes(String root) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(root, Connector.READ);
            return fc.totalSize();
        } catch (IOException err) {
            err.printStackTrace();
            return -1;
        } finally {
            cleanup(fc);
        }
    }

    /**
     * @inheritDoc
     */
    public long getRootAvailableSpace(String root) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(root, Connector.READ);
            return fc.availableSize();
        } catch (IOException err) {
            err.printStackTrace();
            return -1;
        } finally {
            cleanup(fc);
        }
    }

    /**
     * @inheritDoc
     */
    public void mkdir(String directory) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(directory, Connector.READ_WRITE);
            fc.mkdir();
        } catch (IOException err) {
            err.printStackTrace();
        } finally {
            cleanup(fc);
        }
    }

    /**
     * @inheritDoc
     */
    public void deleteFile(String file) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(file, Connector.WRITE);
            fc.delete();
        } catch (IOException err) {
            err.printStackTrace();
        } finally {
            cleanup(fc);
        }
    }

    /**
     * @inheritDoc
     */
    public boolean isHidden(String file) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(file, Connector.READ);
            return fc.isHidden();
        } catch (IOException err) {
            err.printStackTrace();
            return false;
        } finally {
            cleanup(fc);
        }
    }

    /**
     * @inheritDoc
     */
    public void setHidden(String file, boolean h) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(file, Connector.READ_WRITE);
            fc.setHidden(h);
        } catch (IOException err) {
            err.printStackTrace();
        } finally {
            cleanup(fc);
        }
    }

    /**
     * @inheritDoc
     */
    public long getFileLength(String file) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(file, Connector.READ);
            return fc.fileSize();
        } catch (IOException err) {
            err.printStackTrace();
            return -1;
        } finally {
            cleanup(fc);
        }
    }

    /**
     * @inheritDoc
     */
    public boolean isDirectory(String file) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(file, Connector.READ);
            return fc.isDirectory();
        } catch (IOException err) {
            err.printStackTrace();
            return false;
        } finally {
            cleanup(fc);
        }
    }

    /**
     * @inheritDoc
     */
    public char getFileSystemSeparator() {
        return '/';
    }

    /**
     * @inheritDoc
     */
    public boolean exists(String file) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(file, Connector.READ);
            return fc.exists();
        } catch (IOException err) {
            err.printStackTrace();
            return false;
        } finally {
            cleanup(fc);
        }
    }

    /**
     * @inheritDoc
     */
    public void rename(String file, String newName) {
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(file, Connector.READ_WRITE);
            fc.rename(newName);
        } catch (IOException err) {
            err.printStackTrace();
        } finally {
            cleanup(fc);
        }
    }

    public void capturePhoto(ActionListener response) {        
        EventLog.getInstance().logInformationEvent("capturePhoto");
        captureCallback = new EventDispatcher();
        captureCallback.addListener(response);

        UiApplication.getUiApplication().addFileSystemJournalListener(new FileSystemJournalListener() {

            private long lastUSN;

            public void fileJournalChanged() {
                long USN = FileSystemJournal.getNextUSN();
                for (long i = USN - 1; i >= lastUSN; --i) {
                    FileSystemJournalEntry entry = FileSystemJournal.getEntry(i);
                    if (entry != null) {
                        if (entry.getEvent() == FileSystemJournalEntry.FILE_CHANGED) {
                            if (entry.getPath().indexOf(".jpg") != -1) {
                                lastUSN = USN;
                                String path = entry.getPath();
                                //close the camera
                                UiApplication.getUiApplication().removeFileSystemJournalListener(this);

                                try {
                                    EventInjector.KeyEvent inject = new EventInjector.KeyEvent(EventInjector.KeyEvent.KEY_DOWN, Characters.ESCAPE, 0, 200);
                                    inject.post();
                                    inject.post();
                                } catch (Exception e) {
                                    //try to close the camera
                                }
                                EventLog.getInstance().logInformationEvent("path " + path);
                                captureCallback.fireActionEvent(new ActionEvent("file://" + path));
                                captureCallback = null;
                            }
                        }
                    }
                }
                lastUSN = USN;
            }
        });
        app.setWaitingForReply(true);
        synchronized (UiApplication.getEventLock()) {
            Invoke.invokeApplication(Invoke.APP_TYPE_CAMERA, new CameraArguments());
        }
    }

    public void captureVideo(ActionListener response) {
        throw new RuntimeException("capture video is supported on RIM OS 5.0 and up");
    }

    public void captureAudio(ActionListener response) {
        int h = CodeModuleManager.getModuleHandle("net_rim_bb_voicenotesrecorder");

        if (h == 0) {
            throw new RuntimeException("capture audio works only if Voice Notes is installed");
        }

        captureCallback = new EventDispatcher();
        captureCallback.addListener(response);

        UiApplication.getUiApplication().addFileSystemJournalListener(new FileSystemJournalListener() {

            private long lastUSN;

            public void fileJournalChanged() {
                long USN = FileSystemJournal.getNextUSN();
                for (long i = USN - 1; i >= lastUSN; --i) {
                    FileSystemJournalEntry entry = FileSystemJournal.getEntry(i);
                    if (entry != null) {
                        String path = entry.getPath();
                        if (entry.getEvent() == FileSystemJournalEntry.FILE_ADDED
                                && path.endsWith(".amr")) {

                            UiApplication.getUiApplication().removeFileSystemJournalListener(this);

                            try {
                                EventInjector.KeyEvent inject = new EventInjector.KeyEvent(EventInjector.KeyEvent.KEY_DOWN, Characters.ESCAPE, 0, 200);
                                inject.post();
                            } catch (Exception e) {
                                //try to close the voicenotesrecorder
                            }
                            captureCallback.fireActionEvent(new ActionEvent("file://" + path));
                            captureCallback = null;
                            break;
                        }

                    }
                }
                lastUSN = USN;
            }
        });

        app.setWaitingForReply(true);
        ApplicationDescriptor desc = new ApplicationDescriptor(CodeModuleManager.getApplicationDescriptors(h)[0], null);
        try {
            ApplicationManager.getApplicationManager().runApplication(desc, true);
        } catch (ApplicationManagerException e) {
            EventLog.getInstance().logErrorEvent("err " + e.getMessage());            
            e.printStackTrace();
        }
    }

    public void sendMessage(String[] recipients, String subject, Message msg) {
        Folder folders[] = Session.getDefaultInstance().getStore().list(Folder.SENT);
        net.rim.blackberry.api.mail.Message message = new net.rim.blackberry.api.mail.Message(folders[0]);
        try {
            Address toAdds[] = new Address[recipients.length];
            for (int i = 0; i < recipients.length; i++) {
                Address address = new Address(recipients[i], "");
                toAdds[i] = address;
            }
            message.addRecipients(net.rim.blackberry.api.mail.Message.RecipientType.TO, toAdds);
            message.setSubject(subject);
        } catch (Exception e) {
            EventLog.getInstance().logErrorEvent("err " + e.getMessage());            
        }

        try {

            if (msg.getAttachment() != null && msg.getAttachment().length() > 0) {
                Multipart content = new Multipart();
                TextBodyPart tbp = new TextBodyPart(content,msg.getContent());                
                content.addBodyPart(tbp);
                
                InputStream stream = com.codename1.io.FileSystemStorage.getInstance().openInputStream(msg.getAttachment());
                byte[] buf;
                buf = IOUtilities.streamToBytes(stream);
                stream.close();
                SupportedAttachmentPart sap = new SupportedAttachmentPart(content, message.getContentType(), "image.png", buf);
                content.addBodyPart(sap);                
                message.setContent(content);
            } else {
                message.setContent(msg.getContent());
            }

            app.setWaitingForReply(true);
            Invoke.invokeApplication(Invoke.APP_TYPE_MESSAGES, new MessageArguments(message));
        } catch (Exception ex) {
            EventLog.getInstance().logErrorEvent("err " + ex.getMessage());
        }
    }

    public void dial(String phoneNumber) {   
        try {
            PhoneArguments call = new PhoneArguments(PhoneArguments.ARG_CALL, phoneNumber);
            app.setWaitingForReply(true);
            Invoke.invokeApplication(Invoke.APP_TYPE_PHONE, call);            
        } catch (Exception e) {
            EventLog.getInstance().logErrorEvent("unable to open dialer " + e.getMessage());
        }
    }

    public void sendSMS(final String phoneNumber, final String message) throws IOException {
        //why on hell?? RIM has 2 different API's one for CDMA and 
        //one for GSM
        if (!isCDMA()) {
            String address = "sms://" + phoneNumber + ":5000";
            MessageConnection con = null;

            try {
                con = (MessageConnection) Connector.open(address);
                TextMessage txtmessage =
                        (TextMessage) con.newMessage(MessageConnection.TEXT_MESSAGE);
                txtmessage.setAddress(address);
                txtmessage.setPayloadText(message);
                con.send(txtmessage);
            } catch (Exception e) {
                throw new IOException("failed to send sms " + e.getMessage());
            } finally {
                if (con != null) {
                    try {
                        con.close();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }
        } else {
            DatagramConnection connection = null;
            try {
                byte[] data = (message).getBytes("UTF-8");
                connection = (DatagramConnection) Connector.open(
                        "sms://" + phoneNumber + ":5016");
                Datagram dg = connection.newDatagram(
                        connection.getMaximumLength());
                dg.setData(data, 0, data.length);

                connection.send(dg);
            } catch (IOException e) {
                throw new IOException("failed to send sms " + e.getMessage());
            } finally {
                try {
                    connection.close();
                    connection = null;
                } catch (Exception e) {
                }
            }

        }
    }

    
    private boolean isCDMA() {
        if ((RadioInfo.getActiveWAFs() & RadioInfo.WAF_CDMA)
                == RadioInfo.WAF_CDMA) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * @inheritDoc
     */
    public String [] getAllContacts(boolean withNumbers) {
        return RIMContactsManager.getInstance().getAllContacts(withNumbers);
    }
    
    /**
     * @inheritDoc
     */
    public Contact getContactById(String id){
        return RIMContactsManager.getInstance().getContactById(id);
    }
    
    /**
     * @inheritDoc
     */
    public Media createMediaRecorder(String path) throws IOException {
        return new MediaRecorder(path);        
    }    
    
    /**
     * @inheritDoc
     */
    public String getPlatformName() {
        return "rim";
    }

    /**
     * @inheritDoc
     */
    public String[] getPlatformOverrides() {
        return new String[]{"phone", "rim"};
    }

    private L10NManager l10n;

    /**
     * @inheritDoc
     */
    public L10NManager getLocalizationManager() {
        if(l10n == null) {
            Locale l = Locale.getDefault();
            l10n = new L10NManager(l.getLanguage(), l.getCountry()) {
                public String format(int number) {
                    return super.format(number);
                }

                public String format(double number) {
                    return super.format(number);
                }

                public String formatCurrency(double currency) {
                    return super.formatCurrency(currency);
                }

                public String formatDateLongStyle(Date d) {
                    return DateFormat.getInstance(DateFormat.DATE_LONG).format(d);
                }

                public String formatDateShortStyle(Date d) {
                    return DateFormat.getInstance(DateFormat.DATE_SHORT).format(d);
                }

                public String formatDateTime(Date d) {
                    String date = DateFormat.getInstance(DateFormat.DATE_FULL).format(d);
                    String time = DateFormat.getInstance(DateFormat.TIME_FULL).format(d);
                    return date + " " + time;
                }

                public String getCurrencySymbol() {
                    return super.getCurrencySymbol();
                }
                
                public void setLocale(String locale, String language) {
                    super.setLocale(locale, language);
                    Locale.setDefault(Locale.get(language, locale));
                }
            };
        }
        return l10n;
    }

    public void systemOut(String content){
        EventLog.getInstance().logInformationEvent(content);
    }
    
    public CodeScanner getCodeScanner() {
        systemOut("MultimediaManager");
        return new CodeScannerImpl(new MultimediaManager());
    }
}
