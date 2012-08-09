/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */

package com.codename1.ui.util;

import com.codename1.ui.Display;
import com.codename1.designer.ResourceEditorView;
import com.codename1.designer.DataEditor;
import com.codename1.designer.FontEditor;
import com.codename1.designer.ImageMultiEditor;
import com.codename1.designer.ImageRGBEditor;
import com.codename1.designer.L10nEditor;
import com.codename1.designer.MultiImageSVGEditor;
import com.codename1.designer.ThemeEditor;
import com.codename1.designer.TimelineEditor;
import com.codename1.designer.UserInterfaceEditor;
import com.codename1.ui.EditorFont;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.CodenameOneAccessor;
import com.codename1.ui.animations.AnimationAccessor;
import com.codename1.ui.animations.AnimationObject;
import com.codename1.ui.animations.Motion;
import com.codename1.ui.animations.Timeline;
import com.codename1.impl.javase.SVG;
import com.codename1.ui.plaf.Border;
import com.codename1.ui.plaf.Accessor;
import com.codename1.ui.plaf.Style;
import com.codename1.designer.ResourceEditorApp;
import com.codename1.impl.javase.JavaSEPortWithSVGSupport;
import java.awt.Frame;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * This class enhances the resources class by inheriting it and using package
 * friendly accessor methods.
 *
 * @author Shai Almog
 */
public class EditableResources extends Resources implements TreeModel {
    private static final short MINOR_VERSION = 4;
    private static final short MAJOR_VERSION = 1;

    private boolean modified;
    private boolean loadingMode = false;

    private boolean ignoreSVGMode;
    private boolean ignorePNGMode;

    private EditableResources overrideResource;
    private File overrideFile;
    
    public void setOverrideMode(EditableResources overrideResource, File overrideFile) {
        this.overrideResource = overrideResource;
        this.overrideFile = overrideFile;
        if(overrideResource != null) {
            overrideResource.onChange = onChange;
        }
    }
    
    /**
     * Copies the value from the base to the override resource as a starting point
     * @param the name of the resource
     */
    public void overrideResource(String name) {
        overrideResource.setResource(name, getResourceType(name), getResourceObject(name));
    }

    public boolean isOverrideMode() {
        return overrideResource != null;
    }
    
    public boolean isOverridenResource(String id) {
        if(overrideResource == null) {
            return true;
        }
        return overrideResource.getResourceObject(id) != null;
    }
    
    private void writeImageAsPNG(Image image, int type, DataOutputStream output) throws IOException {
        BufferedImage buffer = new BufferedImage(image.getWidth(), image.getHeight(), type);
        buffer.setRGB(0, 0, image.getWidth(), image.getHeight(), image.getRGB(), 0, image.getWidth());
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ImageIO.write(buffer, "png", byteOut);
        byte[] data = byteOut.toByteArray();
        output.writeInt(data.length);
        output.write(data);
        output.writeInt(buffer.getWidth());
        output.writeInt(buffer.getHeight());
        output.writeBoolean(EncodedImage.create(data).isOpaque());
    }

    /**
     * @return the ignoreSVGMode
     */
    public boolean isIgnoreSVGMode() {
        return ignoreSVGMode;
    }

    /**
     * @param ignoreSVGMode the ignoreSVGMode to set
     */
    public void setIgnoreSVGMode(boolean ignoreSVGMode) {
        this.ignoreSVGMode = ignoreSVGMode;
    }

    /**
     * @return the ignorePNGMode
     */
    public boolean isIgnorePNGMode() {
        return ignorePNGMode;
    }

    /**
     * @param ignorePNGMode the ignorePNGMode to set
     */
    public void setIgnorePNGMode(boolean ignorePNGMode) {
        this.ignorePNGMode = ignorePNGMode;
    }

    private abstract class UndoableEdit {
        private boolean previouslyModified;
        
        public final String doAction() {
            previouslyModified = modified;
            String selection = performAction();
            modified = true;
            updateModified();
            if(onChange != null) {
                onChange.run();
            }
            return selection;
        }
        
        public final String undoAction() {
            String selection = performUndo();
            modified = previouslyModified;
            updateModified();
            if(onChange != null) {
                onChange.run();
            }
            return selection;
        }
        
        protected abstract String performAction();
        protected abstract String performUndo();
    }
    
    private List<UndoableEdit> undoQueue = new ArrayList<UndoableEdit>();
    private List<UndoableEdit> redoQueue = new ArrayList<UndoableEdit>();
    
    private Runnable onChange;
    
    /**
     * Create an empty resource file
     */
    public EditableResources() {
        super();
    }
    
    EditableResources(InputStream input) throws IOException {
        super(input);
    }

    public static void setResourcesClassLoader(Class cls) {
        Resources.setClassLoader(cls);
    }

    public static void setCurrentPassword(String password) {
        currentPassword = password;
        if(currentPassword.length() == 0) {
            currentPassword = null;
            key = null;
        } else {
            setPassword(currentPassword);
            try {
                key = currentPassword.getBytes("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }
        }
    }

    private static byte[] key;
    private static String currentPassword;
    void checkKey(String id) {
        JPasswordField password = new JPasswordField();
        if(currentPassword != null) {
            password.setText(currentPassword);
        }
        int v = JOptionPane.showConfirmDialog(java.awt.Frame.getFrames()[0], password, "Enter Password", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if(v == JOptionPane.OK_OPTION) {
            currentPassword = password.getText();
            setPassword(currentPassword);
            try {
                key = currentPassword.getBytes("UTF-8");
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }
            char l = (char)encode(id.charAt(0));
            char w = (char)encode(id.charAt(1));
            //keyOffset = 0;
            if(l != 'l' || w != 'w') {
                // incorrect password!
                JOptionPane.showMessageDialog(java.awt.Frame.getFrames()[0],
                        "Incorrect Password!", "Error", JOptionPane.ERROR_MESSAGE);
                throw new IllegalStateException("Incorrect password");
            }
            return;
        }
        super.checkKey(id);
    }

    private int encode(int val) {
        val = key[keyOffset] ^ val;
        keyOffset++;
        if(keyOffset == key.length) {
            keyOffset = 0;
        }
        return val;
    }

    
    public void setOnChange(Runnable run) {
        onChange = run;
        if(overrideResource != null) {
            overrideResource.onChange = run;
        }
    }
    
    void setResource(final String id, final byte type, final Object value) {
        /*if(!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        setResource(id, type, value);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            } 
            return;
        }*/
        if(overrideResource != null) {
            overrideResource.setResource(id, type, value);
            return;
        }
        boolean exists = false;
        int index = -1;
        if(value != null) {
            exists = getResourceObject(id) != null;
        } else {
            index = getIndexOfChild(getParent(type), id);
        }

        Object superValue = value;
        if(multiPending != null) {
            if(superValue instanceof com.codename1.ui.EncodedImage) {
                superValue = multiPending;
            }
            multiPending = null;
        }

        super.setResource(id, type, superValue);
        if(superValue != null) {
            index = getIndexOfChild(getParent(type), id);
            if(exists) {
                fireTreeNodeChanged(id, index);
            } else {
                fireTreeNodeAdded(id, index);
            }
        } else {
            fireTreeNodeRemoved(id, type, index);
        }
    }

    public void clear() {
        if(overrideResource != null) {
            overrideResource.clear();
            return;
        }
        super.clear();
        modified = false;
        for(String name : getResourceNames()) {
            setResource(name, getResourceType(name), null);
        }
        updateModified();
    }


    @Override
    public void openFile(final InputStream input) throws IOException {
        loadingMode = true;
        com.codename1.ui.Font.clearBitmapCache();
        super.openFile(input);
        loadingMode = false;
        modified = false;
        updateModified();
        undoQueue.clear();
        redoQueue.clear();
        ThemeEditor.resetThemeLoaded();
    }

    /**
     * Undo the last operation
     */
    public String undo() {
        if(overrideResource != null) {
            return overrideResource.undo();
        }
        if(isUndoable()) {
            UndoableEdit edit = undoQueue.remove(undoQueue.size() - 1);
            redoQueue.add(edit);
            return edit.undoAction();
        }
        return null;
    }
    
    public boolean isUndoable() {
        if(overrideResource != null) {
            return overrideResource.isUndoable();
        }
        return !undoQueue.isEmpty();
    }

    public boolean isRedoable() {
        if(overrideResource != null) {
            return overrideResource.isRedoable();
        }
        return !redoQueue.isEmpty();
    }

    public boolean containsResource(String res) {
        if(overrideResource != null) {
            return overrideResource.containsResource(res);
        }
        return getResourceObject(res) != null;
    }
    
    public String redo() {
        if(overrideResource != null) {
            return overrideResource.redo();
        }
        if(isRedoable()) {
            UndoableEdit edit = redoQueue.remove(redoQueue.size() - 1);
            undoQueue.add(edit);
            return edit.doAction();
        }
        return null;
    }
    
    public boolean isModified() {
        if(overrideResource != null) {
            return overrideResource.isModified() || modified;
        }
        return modified;
    }

    private void updateModified() {
        if(overrideResource != null) {
            overrideResource.updateModified();
            return;
        }
        if(ResourceEditorApp.IS_MAC) {
            for(java.awt.Window w : java.awt.Frame.getWindows()) {
                if(w instanceof JFrame) {
                    if(modified) {
                        ((JFrame)w).getRootPane().putClientProperty("Window.documentModified", Boolean.TRUE);
                    } else {
                        ((JFrame)w).getRootPane().putClientProperty("Window.documentModified", Boolean.FALSE);
                    }
                }
            }
        }
    }
    
    public void setModified() {
        if(overrideResource != null) {
            overrideResource.setModified();
            return;
        }
        this.modified = true;
        updateModified();
    }
    
    public byte[] getDataByteArray(String id) {
        if(overrideResource != null) {
            byte[] d = overrideResource.getDataByteArray(id);
            if(d != null) {
                return d;
            }
        }
        Object o = getResourceObject(id);
        return ((byte[])o);
    }

    public long getDataSize(String id) {
        if(overrideResource != null) {
            long i = overrideResource.getDataSize(id);
            if(i > -1) {
                return i;
            }
        }
        Object o = getResourceObject(id);
        if ((o != null) && (o instanceof byte[]))
            return ((byte[])o).length;
        else
            return -1;
    }

    /**
     * Adapts loaded themes to the new resource selected/unselected modes
     */
    Hashtable loadTheme(String id, boolean newerVersion) throws IOException {
        if(overrideResource != null) {
            Hashtable h = overrideResource.loadTheme(id, newerVersion);
            if(h != null) {
                return h;
            }
        }
        Hashtable h = super.loadTheme(id, newerVersion);

        Iterator keyIter = h.keySet().iterator();
        while(keyIter.hasNext()) {
            String key = (String)keyIter.next();
            if(key.indexOf(".bgSelection") > -1 || key.indexOf(".fgSelection") > -1 ||
                key.equals("bgSelection") || key.equals("fgSelection")) {
                Object value = h.get(key);
                h.remove(key);
                int pointPos = key.indexOf('.');
                if(pointPos > -1) {
                    key = key.substring(0, pointPos) + ".sel#" + key.substring(pointPos + 1).replace("Selection", "");
                } else {
                    key = "sel#" + key.replace("Selection", "");
                }
                h.put(key, value);
                keyIter = h.keySet().iterator();
            }
            if(key.indexOf("scaledImage") > -1) {
                Object value = h.get(key);
                h.remove(key);
                int pointPos = key.indexOf('.');
                if(pointPos > -1) {
                    key = key.substring(0, pointPos) + ".bgType";
                } else {
                    key = "bgType";
                }
                if(((String)value).equals("true")) {
                    h.put(key, new Byte(Style.BACKGROUND_IMAGE_SCALED));
                } else {
                    h.put(key, new Byte(Style.BACKGROUND_IMAGE_TILE_BOTH));
                }
                keyIter = h.keySet().iterator();
            }
        }

        return h;
    }
    
    /**
     * Allows us to store a modified resource file
     */
    public void save(OutputStream out) throws IOException {
        if(overrideFile != null) {
            overrideResource.save(new FileOutputStream(overrideFile));
        }
        // disable override for the duration of the save so stuff from the override doesn't
        // get into the main resource file
        File overrideFileBackup = overrideFile;
        EditableResources overrideResourceBackup = overrideResource;
        overrideResource = null;
        overrideFile = null;
        try {
            DataOutputStream output = new DataOutputStream(out);
            String[] resourceNames = getResourceNames();

            keyOffset = 0;
            if(currentPassword != null) {
                output.writeShort(resourceNames.length + 2);
                output.writeByte(MAGIC_PASSWORD);
                output.writeUTF("" + ((char)encode('l')) + ((char)encode('w')));
                output.writeByte(encode(MAGIC_HEADER & 0xff));
            } else {
                output.writeShort(resourceNames.length + 1);
                // write the header of the resource file
                output.writeByte(MAGIC_HEADER);
            }
            output.writeUTF("");

            // the size of the header
            output.writeShort(6);
            output.writeShort(MAJOR_VERSION);
            output.writeShort(MINOR_VERSION);

            // currently resource file meta-data isn't supported
            output.writeShort(0);

            for(int iter = 0 ; iter < resourceNames.length ; iter++) {
                // write the magic number
                byte magic = getResourceType(resourceNames[iter]);
                switch(magic) {
                    case MAGIC_TIMELINE:
                    case MAGIC_ANIMATION_LEGACY:
                    case MAGIC_IMAGE_LEGACY:
                    case MAGIC_INDEXED_IMAGE_LEGACY:
                        magic = MAGIC_IMAGE;
                        break;
                    case MAGIC_THEME_LEGACY:
                        magic = MAGIC_THEME;
                        break;
                    case MAGIC_FONT_LEGACY:
                        magic = MAGIC_FONT;
                        break;
                }
                if(currentPassword != null) {
                    output.writeByte(encode(magic & 0xff));
                    char[] chars = resourceNames[iter].toCharArray();
                    for(int i = 0 ; i < chars.length ; i++) {
                        chars[i] = (char)encode(chars[i] & 0xffff);
                    }
                    output.writeUTF(new String(chars));
                } else {
                    output.writeByte(magic);
                    output.writeUTF(resourceNames[iter]);
                }
                switch(magic) {
                    case MAGIC_IMAGE:
                        Object o = getResourceObject(resourceNames[iter]);
                        if(!(o instanceof MultiImage)) {
                            o = null;
                        }
                        saveImage(output, getImage(resourceNames[iter]), (MultiImage)o, BufferedImage.TYPE_INT_ARGB);
                        continue;
                    case MAGIC_THEME:
                        saveTheme(output, getTheme(resourceNames[iter]), magic == MAGIC_THEME_LEGACY);
                        continue;
                    case MAGIC_FONT:
                        saveFont(output, false, resourceNames[iter]);
                        continue;
                    case MAGIC_DATA: {
                        InputStream i = getData(resourceNames[iter]);
                        ByteArrayOutputStream outArray = new ByteArrayOutputStream();
                        int val = i.read();
                        while(val != -1) {
                            outArray.write(val);
                            val = i.read();
                        }
                        byte[] data = outArray.toByteArray();
                        output.writeInt(data.length);
                        output.write(data);
                        continue;
                    }
                    case MAGIC_UI: {
                        InputStream i = getUi(resourceNames[iter]);
                        ByteArrayOutputStream outArray = new ByteArrayOutputStream();
                        int val = i.read();
                        while(val != -1) {
                            outArray.write(val);
                            val = i.read();
                        }
                        byte[] data = outArray.toByteArray();
                        output.writeInt(data.length);
                        output.write(data);
                        continue;
                    }
                    case MAGIC_L10N:
                        // we are getting the theme which allows us to acces the l10n data
                        saveL10N(output, getTheme(resourceNames[iter]));
                        continue;
                    default:
                        throw new IOException("Corrupt theme file unrecognized magic number: " + Integer.toHexString(magic & 0xff));
                }
            }
            modified = false;
            updateModified();
            undoQueue.clear();
            redoQueue.clear();
        } finally {
            overrideFile = overrideFileBackup;
            overrideResource = overrideResourceBackup;
        }
    }

    private void removeMultiConstants(Hashtable h) {
        for(Object k : h.keySet()) {
            String key = (String)k;
            if(key.startsWith("@")) {
                Object val = h.get(k);
                if(val instanceof MultiImage) {
                    h.put(k, ((MultiImage)val).getBest());
                    removeMultiConstants(h);
                    return;
                }
            }
        }
    }
    
    /**
     * Returns the data resource from the file
     * 
     * @param id name of the data resource
     * @return newly created input stream that allows reading the data of the resource
     */
    public InputStream getData(String id) {
        if(overrideResource != null) {
            InputStream h = overrideResource.getData(id);
            if(h != null) {
                return h;
            }
        }
        return super.getData(id);
    }
    
    /**
     * Returns the ui resource from the file
     *
     * @param id name of the ui resource
     * @return newly created input stream that allows reading the ui of the resource
     */
    InputStream getUi(String id) {
        if(overrideResource != null) {
            InputStream h = overrideResource.getUi(id);
            if(h != null) {
                return h;
            }
        }
        return super.getUi(id);
    }
    
    public Hashtable getTheme(String id) {
        if(overrideResource != null) {
            Hashtable h = overrideResource.getTheme(id);
            if(h != null) {
                return h;
            }
        }
        if(loadingMode) {
            return new Hashtable();
        }
        Hashtable h = super.getTheme(id);
        if(h != null) {
            removeMultiConstants(h);
            h.remove("name");
        }
        return h;
    }
    
    private String[] mergeArrays(String[] a, String[] b) {
        List<String> l = new ArrayList<String>(Arrays.asList(a));
        for(String s : b) {
            if(!l.contains(s)) {
                l.add(s);
            }
        }
        String[] res = new String[l.size()];
        l.toArray(res);
        return res;
    }

    public String[] getResourceNames() {
        if(overrideResource != null) {
            return mergeArrays(overrideResource.getResourceNames(), super.getResourceNames());
        }
        return super.getResourceNames();
    }
    
    public String[] getL10NResourceNames() {
        if(overrideResource != null) {
            return mergeArrays(overrideResource.getL10NResourceNames(), super.getL10NResourceNames());
        }
        return super.getL10NResourceNames();
    }

    public String[] getFontResourceNames() {
        if(overrideResource != null) {
            return mergeArrays(overrideResource.getFontResourceNames(), super.getFontResourceNames());
        }
        return super.getFontResourceNames();
    }

    public String[] getThemeResourceNames() {
        if(overrideResource != null) {
            return mergeArrays(overrideResource.getThemeResourceNames(), super.getThemeResourceNames());
        }
        return super.getThemeResourceNames();
    }

    public String[] getImageResourceNames() {
        if(overrideResource != null) {
            return mergeArrays(overrideResource.getImageResourceNames(), super.getImageResourceNames());
        }
        return super.getImageResourceNames();
    }
    
    public String[] getUIResourceNames() {
        if(overrideResource != null) {
            return mergeArrays(overrideResource.getUIResourceNames(), super.getUIResourceNames());
        }
        return super.getUIResourceNames();
    }

    public String[] getDataResourceNames() {
        if(overrideResource != null) {
            return mergeArrays(overrideResource.getDataResourceNames(), super.getDataResourceNames());
        }
        return super.getDataResourceNames();
    }
    
    
    private void saveTheme(DataOutputStream output, Hashtable theme, boolean newVersion) throws IOException {
        theme.remove("name");
        
        output.writeShort(theme.size());
        for(Object currentKey : theme.keySet()) {
            String key = (String)currentKey;
            output.writeUTF(key);

            if(key.startsWith("@")) {
                if(key.endsWith("Image")) {
                    output.writeUTF(findId(theme.get(key)));
                } else {
                    output.writeUTF((String)theme.get(key));
                }
                continue;
            }
            
            // if this is a simple numeric value
            if(key.endsWith("Color")) {
                output.writeInt(Integer.decode("0x" + theme.get(key)));
                continue;
            } 

            if(key.endsWith("align") || key.endsWith("textDecoration")) {
                output.writeShort(((Number)theme.get(key)).shortValue());
                continue;
            }

            // if this is a short numeric value
            if(key.endsWith("transparency")) {
                output.writeByte(Integer.parseInt((String)theme.get(key)));
                continue;
            } 

            // if this is a padding or margin then we will have the 4 values as bytes
            if(key.endsWith("padding") || key.endsWith("margin")) {
                String[] arr = ((String)theme.get(key)).split(",");
                output.writeByte(Integer.parseInt(arr[0]));
                output.writeByte(Integer.parseInt(arr[1]));
                output.writeByte(Integer.parseInt(arr[2]));
                output.writeByte(Integer.parseInt(arr[3]));
                continue;
            }

            // padding and or margin type
            if(key.endsWith("Unit")) {
                for(byte b : (byte[])theme.get(key)) {
                    output.writeByte(b);
                }
                continue;
            }

            if(key.endsWith("border")) {
                Border border = (Border)theme.get(key);
                writeBorder(output, border, newVersion);
                continue;
            }
            
            // if this is a font
            if(key.endsWith("font")) {
                com.codename1.ui.Font f = (com.codename1.ui.Font)theme.get(key);

                // is this a new font?
                boolean newFont = f instanceof EditorFont;
                output.writeBoolean(newFont);
                if(newFont) {
                    String fontId = findId(f);
                    output.writeUTF(fontId);
                } else {
                    output.writeByte(f.getFace());
                    output.writeByte(f.getStyle());
                    output.writeByte(f.getSize());
                }
                continue;
            } 

            // if this is a background image
            if(key.endsWith("bgImage")) {
                String imageId = findId(theme.get(key));
                output.writeUTF(imageId);
                continue;
            } 

            if(key.endsWith("scaledImage")) {
                output.writeBoolean(theme.get(key).equals("true"));
                continue;
            }

            if(key.endsWith("derive")) {
                output.writeUTF((String)theme.get(key));
                continue;
            }
            
            // if this is a background gradient
            if(key.endsWith("bgGradient")) {
                Object[] gradient = (Object[])theme.get(key);
                output.writeInt(((Integer)gradient[0]).intValue());
                output.writeInt(((Integer)gradient[1]).intValue());
                output.writeFloat(((Float)gradient[2]).floatValue());
                output.writeFloat(((Float)gradient[3]).floatValue());
                output.writeFloat(((Float)gradient[4]).floatValue());
                continue;
            }

            if(key.endsWith(Style.BACKGROUND_TYPE) || key.endsWith(Style.BACKGROUND_ALIGNMENT)) {
                output.writeByte(((Number)theme.get(key)).intValue());
                continue;
            }

            // thow an exception no idea what this is
            throw new IOException("Error while trying to read theme property: " + key);
        }
    }
    
    private void writeBorder(DataOutputStream output, Border border, boolean newVersion) throws IOException {
        int type = Accessor.getType(border);
        switch(type) {
            case BORDER_TYPE_EMPTY:
                output.writeShort(0xff01);
                return;
            case BORDER_TYPE_LINE:
                output.writeShort(0xff02);

                // use theme colors?
                if(Accessor.isThemeColors(border)) {
                    output.writeBoolean(true);
                    output.writeByte(Accessor.getThickness(border));
                } else {
                    output.writeBoolean(false);
                    output.writeByte(Accessor.getThickness(border));
                    output.writeInt(Accessor.getColorA(border));
                }
                return;
            case BORDER_TYPE_ROUNDED:
                output.writeShort(0xff03);

                // use theme colors?
                if(Accessor.isThemeColors(border)) {
                    output.writeBoolean(true);
                    output.writeByte(Accessor.getArcWidth(border));
                    output.writeByte(Accessor.getArcHeight(border));
                } else {
                    output.writeBoolean(false);
                    output.writeByte(Accessor.getArcWidth(border));
                    output.writeByte(Accessor.getArcHeight(border));
                    output.writeInt(Accessor.getColorA(border));
                }
                return;
            case BORDER_TYPE_ETCHED_RAISED:
                output.writeShort(0xff05);
                writeEtchedBorder(output, border);
                return;
            case BORDER_TYPE_ETCHED_LOWERED:
                output.writeShort(0xff04);
                writeEtchedBorder(output, border);
                return;
            case BORDER_TYPE_BEVEL_LOWERED:
                output.writeShort(0xff06);
                writeBevelBorder(output, border);
                return;
            case BORDER_TYPE_BEVEL_RAISED:
                output.writeShort(0xff07);
                writeBevelBorder(output, border);
                return;
            case BORDER_TYPE_IMAGE:
                output.writeShort(0xff08);
                writeImageBorder(output, border);
                return;
            case BORDER_TYPE_IMAGE_HORIZONTAL:
                output.writeShort(0xff09);
                writeImageHVBorder(output, border);
                return;
            case BORDER_TYPE_IMAGE_VERTICAL:
                output.writeShort(0xff10);
                writeImageHVBorder(output, border);
                return;
            case BORDER_TYPE_IMAGE_SCALED:
                output.writeShort(0xff11);
                writeImageBorder(output, border);
                return;
        }
    }

    private void writeBevelBorder(DataOutputStream output, Border border) throws IOException {
        // use theme colors?
        if(Accessor.isThemeColors(border)) {
            output.writeBoolean(true);
        } else {
            output.writeBoolean(false);
            output.writeInt(Accessor.getColorA(border));
            output.writeInt(Accessor.getColorB(border));
            output.writeInt(Accessor.getColorC(border));
            output.writeInt(Accessor.getColorD(border));
        }
    }

    private void writeEtchedBorder(DataOutputStream output, Border border) throws IOException {
        // use theme colors?
        if(Accessor.isThemeColors(border)) {
            output.writeBoolean(true);
        } else {
            output.writeBoolean(false);
            output.writeInt(Accessor.getColorA(border));
            output.writeInt(Accessor.getColorB(border));
        }
    }
    
    private void writeImageHVBorder(DataOutputStream output, Border border) throws IOException {
        Image[] images = Accessor.getImages(border);
        output.writeUTF(findId(images[0]));
        output.writeUTF(findId(images[1]));
        output.writeUTF(findId(images[2]));
    }

    private void writeImageBorder(DataOutputStream output, Border border) throws IOException {
        // Write the number of images can be 2, 3, 8 or 9
        Image[] images = Accessor.getImages(border);
        int resourceCount = 0;
        for(int iter = 0 ; iter < images.length ; iter++) {
            if(images[iter] != null && findId(images[iter]) != null) {
                resourceCount++;
            }
        }
        if(resourceCount != 2 && resourceCount != 3 && resourceCount != 8 && resourceCount != 9) {
            System.out.println("Odd resource count for image border: " + resourceCount);
            resourceCount = 2;
        }
        output.writeByte(resourceCount);
        switch(resourceCount) {
            case 2:
                output.writeUTF(findId(images[0]));
                output.writeUTF(findId(images[4]));
                break;
            case 3:
                output.writeUTF(findId(images[0]));
                output.writeUTF(findId(images[4]));
                output.writeUTF(findId(images[8]));
                break;
            case 8:
                for(int iter = 0 ; iter < 8 ; iter++) {
                    output.writeUTF(findId(images[iter]));
                }
                break;
            case 9:
                for(int iter = 0 ; iter < 9 ; iter++) {
                    output.writeUTF(findId(images[iter]));
                }
                break;
        }
    }

    com.codename1.ui.Font loadFont(DataInputStream input, String id, boolean packed) throws IOException {
        if(getMinorVersion() == 0 && getMajorVersion() == 0) {
            com.codename1.ui.Font bitmapFont = super.loadFont(input, id, packed);
            return new EditorFont(com.codename1.ui.Font.createSystemFont(com.codename1.ui.Font.FACE_SYSTEM, com.codename1.ui.Font.STYLE_PLAIN, com.codename1.ui.Font.SIZE_MEDIUM),
                    null, "Arial-plain-12", true, RenderingHints.VALUE_TEXT_ANTIALIAS_ON, bitmapFont.getCharset());
        } else {
            // read a system font fallback
            int fallback = input.readByte() & 0xff;
            com.codename1.ui.Font systemFont = com.codename1.ui.Font.createSystemFont(fallback & 0x60, fallback & 7, fallback & 0x18);
            com.codename1.ui.Font bitmapFont = null;
            byte[] truetypeFont = null;
            String lookupFont = null;

            // do we have an emedded truetype font? Do we support embedded fonts?
            boolean trueTypeIncluded = input.readBoolean();
            if(trueTypeIncluded) {
                truetypeFont = new byte[input.readInt()];
                input.readFully(truetypeFont);
            }
            boolean lookupIncluded = input.readBoolean();
            if(lookupIncluded) {
                lookupFont = input.readUTF();
            }
            boolean bitmapIncluded = input.readBoolean();
            if(bitmapIncluded) {
                bitmapFont = loadBitmapFont(input, id, null);
                return new EditorFont(systemFont, truetypeFont, lookupFont, true, renderingHint, bitmapFont.getCharset());
            }
            return new EditorFont(systemFont, truetypeFont, lookupFont, false, null, null);
        }
    }

    private Object renderingHint;
    void readRenderingHint(DataInputStream i) throws IOException {
        renderingHint = EditorFont.RENDERING_HINTS[i.readByte()];
    }
    
    private void saveFont(DataOutputStream output, boolean packed, String id) throws IOException {
        EditorFont f = (EditorFont)getFont(id);

        // write the system font fallback
        output.writeByte(f.getSystemFallback().getFace() | f.getSystemFallback().getSize() | f.getSystemFallback().getStyle());

        // do we have an emedded truetype font? Do we support embedded fonts?
        boolean trueTypeIncluded = f.getTruetypeFont() != null;
        output.writeBoolean(trueTypeIncluded);
        if(trueTypeIncluded) {
            output.writeInt(f.getTruetypeFont().length);
            output.write(f.getTruetypeFont());
        }
        boolean lookupIncluded = f.getLookupFont() != null;
        output.writeBoolean(lookupIncluded);
        if(lookupIncluded) {
            output.writeUTF(f.getLookupFont());
        }
        boolean bitmapIncluded = f.isIncludesBitmap();
        output.writeBoolean(bitmapIncluded);
        if(bitmapIncluded) {
            com.codename1.ui.Font bitmapFont = f.getBitmapFont();
            com.codename1.ui.Image cache = CodenameOneAccessor.getImage(bitmapFont);
            int[] imageArray = cache.getRGB();
            for(int iter = 0 ; iter < imageArray.length ; iter++) {
                imageArray[iter] = (imageArray[iter] >> 8) & 0xff0000;
            }
            saveImage(output, com.codename1.ui.Image.createImage(imageArray, cache.getWidth(), cache.getHeight()), null, BufferedImage.TYPE_INT_RGB);
            String charset = bitmapFont.getCharset();
            int charCount = charset.length();
            output.writeShort(charCount);
            int[] cutOffsets = CodenameOneAccessor.getOffsets(bitmapFont);
            int[] charWidth = CodenameOneAccessor.getWidths(bitmapFont);
            for(int iter = 0 ; iter < charCount ; iter++) {
                output.writeShort(cutOffsets[iter]);
            }
            for(int iter = 0 ; iter < charCount ; iter++) {
                output.writeByte(charWidth[iter]);
            }
            output.writeUTF(charset);
            output.writeByte(f.getRenderingHint());
        }
    }

    public Object getResourceObject(String res) {
        if(overrideResource != null) {
            Object o = overrideResource.getResourceObject(res);
            if(o != null) {
                return o;
            }
        }
        return super.getResourceObject(res);
    }
    
    public String findId(Object value) {
        if(overrideResource != null) {
            String o = overrideResource.findId(value);
            if(o != null) {
                return o;
            }
        }
        for(String key : getResourceNames()) {
            Object o = getResourceObject(key);

            // special case for multi image which can be all of the internal images...
            if(o instanceof MultiImage) {
                for(Object c : ((MultiImage)o).getInternalImages()) {
                    if(c == value) {
                        return key;
                    }
                }
            }

            if(o == value) {
                return key;
            }
        }
        return null;
    }

    private int getImageType(com.codename1.ui.Image image, MultiImage mi) {
        if(mi != null) {
            return 0xF6;
        }

        if(image instanceof Timeline) {
            return MAGIC_TIMELINE;
        }

        if(image.isSVG()) {
            return 0xf7;
        }

        // png image
        return 0xf1;
    }

    private void saveImage(DataOutputStream output, com.codename1.ui.Image image, MultiImage mi, int type) throws IOException {
        int rType = getImageType(image, mi);
        if(ignoreSVGMode && (rType == 0xf5 || rType == 0xf7)) {
            output.writeByte(0xf1);
        } else {
            output.writeByte(rType);
        }
        switch(rType) {
            // PNG file
            case 0xf1:

            // JPEG File
            case 0xf2:
                if(image instanceof EncodedImage) {
                    byte[] data = ((EncodedImage)image).getImageData();
                    output.writeInt(data.length);
                    output.write(data);
                    output.writeInt(image.getWidth());
                    output.writeInt(image.getHeight());
                    output.writeBoolean(image.isOpaque());
                } else {
                    writeImageAsPNG(image,type, output);
                }
                break;

            // SVG
            case 0xf5:
            // multiimage with SVG
            case 0xf7:
                if(ignoreSVGMode) {
                    writeImageAsPNG(image, type, output);
                    break;
                }
                saveSVG(output, image, rType == 0xf7);
                break;

            case 0xF6:
                writeMultiImage(output, mi);
                break;

            // Timeline
            case MAGIC_TIMELINE:
                writeTimeline(output, (Timeline)image);
                break;

            // Fail this is the wrong data type
            default:
                throw new IOException("Illegal type while creating image: " + Integer.toHexString(type));
        }
    }

    private void writeMultiImage(DataOutputStream output, MultiImage mi) throws IOException {
        output.writeInt(mi.getDpi().length);
        for(int iter = 0 ; iter < mi.getDpi().length ; iter++) {
            output.writeInt(mi.getDpi()[iter]);
            output.writeInt(mi.getInternalImages()[iter].getImageData().length);
        }
        for(int iter = 0 ; iter < mi.getDpi().length ; iter++) {
            output.write(mi.getInternalImages()[iter].getImageData());
        }
    }

    private void writeTimeline(DataOutputStream output, Timeline t) throws IOException {
        output.writeInt(t.getDuration());
        output.writeInt(t.getSize().getWidth());
        output.writeInt(t.getSize().getHeight());
        AnimationObject[] animations = new AnimationObject[t.getAnimationCount()];
        output.writeShort(animations.length);
        for(int iter = 0 ; iter < animations.length ; iter++) {
            animations[iter] = t.getAnimation(iter);
            String name = AnimationAccessor.getImageName(animations[iter]);
            if(name == null) {
                name = findId(AnimationAccessor.getImage(animations[iter]));
            }
            output.writeUTF(name);
            int startTime = animations[iter].getStartTime();
            int animDuration = animations[iter].getEndTime() - startTime;
            output.writeInt(startTime);
            output.writeInt(animDuration);

            Motion motionX = AnimationAccessor.getMotionX(animations[iter]);
            Motion motionY = AnimationAccessor.getMotionY(animations[iter]);
            output.writeInt(motionX.getSourceValue());
            output.writeInt(motionY.getSourceValue());

            int frameDelay = AnimationAccessor.getFrameDelay(animations[iter]);
            output.writeInt(frameDelay);
            if(frameDelay > -1) {
                output.writeInt(AnimationAccessor.getFrameWidth(animations[iter]));
                output.writeInt(AnimationAccessor.getFrameHeight(animations[iter]));
            }

            if(motionX.getSourceValue() != motionX.getDestinationValue()) {
                output.writeBoolean(true);
                output.writeInt(AnimationAccessor.getMotionType(motionX));
                output.writeInt(motionX.getDestinationValue());
            } else {
                output.writeBoolean(false);
            }

            if(motionY.getSourceValue() != motionY.getDestinationValue()) {
                output.writeBoolean(true);
                output.writeInt(AnimationAccessor.getMotionType(motionY));
                output.writeInt(motionY.getDestinationValue());
            } else {
                output.writeBoolean(false);
            }

            writeMotion(AnimationAccessor.getWidth(animations[iter]), output);
            writeMotion(AnimationAccessor.getHeight(animations[iter]), output);
            writeMotion(AnimationAccessor.getOpacity(animations[iter]), output);
            writeMotion(AnimationAccessor.getOrientation(animations[iter]), output);
        }
    }

    private void writeMotion(Motion m, DataOutputStream output) throws IOException {
        if(m != null) {
            output.writeBoolean(true);
            output.writeInt(AnimationAccessor.getMotionType(m));
            output.writeInt(m.getSourceValue());
            output.writeInt(m.getDestinationValue());
        } else {
            output.writeBoolean(false);
        }
    }

    private void saveSVG(DataOutputStream out, Image i, boolean isMultiImage) throws IOException {
        SVG s = (SVG)i.getSVGDocument();
        out.writeInt(s.getSvgData().length);
        out.write(s.getSvgData());
        if(s.getBaseURL() == null) {
            out.writeUTF("");
        } else {
            out.writeUTF(s.getBaseURL());
        }

        // unknown???
        out.writeBoolean(true);

        if(ignorePNGMode) {
            out.writeFloat(s.getRatioW());
            out.writeFloat(s.getRatioH());
            out.writeInt(0);
        } else {
            if(isMultiImage) {
                writeMultiImage(out, svgToMulti(i));
            } else {
                out.writeFloat(s.getRatioW());
                out.writeFloat(s.getRatioH());
                writeImageAsPNG(i, BufferedImage.TYPE_INT_ARGB, out);
            }
        }
    }

    private com.codename1.ui.EncodedImage toEncodedImage(Image image) throws IOException {
        if(image instanceof EncodedImage) {
            return (com.codename1.ui.EncodedImage)image;
        }
        BufferedImage buffer = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        buffer.setRGB(0, 0, image.getWidth(), image.getHeight(), image.getRGB(), 0, image.getWidth());
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ImageIO.write(buffer, "png", byteOut);
        byteOut.close();
        return com.codename1.ui.EncodedImage.create(byteOut.toByteArray());
    }

    private MultiImage svgToMulti(Image image) throws IOException {
        SVG s = (SVG)image.getSVGDocument();
        MultiImage mi = new MultiImage();
        mi.dpi = s.getDpis();
        if(mi.dpi == null || mi.dpi.length == 0) {
            mi.dpi = new int[] {com.codename1.ui.Display.DENSITY_MEDIUM};
            mi.internalImages = new com.codename1.ui.EncodedImage[] {toEncodedImage(image)};
            return mi;
        }
        mi.internalImages = new com.codename1.ui.EncodedImage[mi.dpi.length];
        for(int iter = 0 ; iter < mi.dpi.length ; iter++) {
            Image currentImage = image.scaled(s.getWidthForDPI()[iter], s.getHeightForDPI()[iter]);
            mi.internalImages[iter] = toEncodedImage(currentImage);
        }
        return mi;
    }


    @Override
    com.codename1.ui.Image createSVG(boolean animated, byte[] data) throws IOException {
        com.codename1.ui.Image img = super.createSVG(animated, data);
        SVG s = (SVG)img.getSVGDocument();
        s.setDpis(dpisLoaded);
        s.setWidthForDPI(widthForDPI);
        s.setHeightForDPI(heightForDPI);
        return img;
    }


    private int[] dpisLoaded;
    private int[] widthForDPI;
    private int[] heightForDPI;
    private MultiImage multiPending;

    @Override
    Image readMultiImage(DataInputStream input, boolean skipAll) throws IOException {
        com.codename1.ui.EncodedImage resultImage = null;
        int dpi = com.codename1.ui.Display.getInstance().getDeviceDensity();
        int dpiCount = input.readInt();
        int bestFitOffset = 0;
        int bestFitDPI = 0;
        int[] lengths = new int[dpiCount];
        dpisLoaded = new int[dpiCount];
        widthForDPI = new int[dpiCount];
        heightForDPI = new int[dpiCount];
        for(int iter = 0 ; iter < dpiCount ; iter++) {
            int currentDPI = input.readInt();
            dpisLoaded[iter] = currentDPI;
            lengths[iter] = input.readInt();
            if(bestFitDPI != dpi && dpi >= currentDPI && currentDPI >= bestFitDPI) {
                bestFitDPI = currentDPI;
                bestFitOffset = iter;
            }
        }

        multiPending = new MultiImage();
        multiPending.setDpi(dpisLoaded);
        multiPending.setInternalImages(new EncodedImage[dpisLoaded.length]);
        for(int iter = 0 ; iter < lengths.length ; iter++) {
            int size = lengths[iter];
            if(!skipAll && bestFitOffset == iter) {
                byte[] multiImageData = new byte[size];
                input.readFully(multiImageData, 0, size);
                resultImage = com.codename1.ui.EncodedImage.create(multiImageData);
                widthForDPI[iter] = resultImage.getWidth();
                heightForDPI[iter] = resultImage.getHeight();
                multiPending.getInternalImages()[iter] = resultImage;
            } else {
                byte[] multiImageData = new byte[size];
                input.readFully(multiImageData, 0, size);
                com.codename1.ui.EncodedImage tmp = com.codename1.ui.EncodedImage.create(multiImageData);
                widthForDPI[iter] = tmp.getWidth();
                heightForDPI[iter] = tmp.getHeight();
                multiPending.getInternalImages()[iter] = tmp;
            }
        }

        if(resultImage == null) {
            return Image.createImage(5, 5);
        }
        return resultImage;
    }

    private float ratioW;
    private float ratioH;
    @Override
    void loadSVGRatios(DataInputStream input) throws IOException {
        ratioW = input.readFloat();
        ratioH = input.readFloat();
    }

    @Override
    Image createImage() throws IOException {
        Image i = super.createImage();
        if(i.isSVG()) {
            SVG s = (SVG)i.getSVGDocument();
            s.setRatioH(ratioH);
            s.setRatioW(ratioW);
        }
        return i;
    }

    public com.codename1.ui.Image getImage(String id) {
        if(overrideResource != null) {
            com.codename1.ui.Image o = overrideResource.getImage(id);
            if(o != null) {
                return o;
            }
        }
        Object o = getResourceObject(id);
        if(o instanceof MultiImage) {
            MultiImage m = (MultiImage)o;
            return m.getBest();
        }
        return (com.codename1.ui.Image)o;
    }

    
    private void pushUndoable(UndoableEdit edit) {
        undoQueue.add(edit);
        edit.doAction();
        redoQueue.clear();
    }
    
    public void setImage(final String name, final com.codename1.ui.Image value) {
        if(overrideResource != null) {
            overrideResource.setImage(name, value);
            return;
        }
        // we need to replace the image in all the themes...
        final com.codename1.ui.Image oldValue = getImage(name);
        byte type;
        if(value instanceof Timeline) {
            type = MAGIC_TIMELINE;
        } else {
            type = MAGIC_IMAGE;
        }
        final byte finalType = type;
        pushUndoable(new UndoableEdit() {
            @Override
            protected String performAction() {
                replaceThemeValue(oldValue, value);
                setResource(name, finalType, value);
                return name;
            }

            @Override
            protected String performUndo() {
                replaceThemeValue(value, oldValue);
                setResource(name, finalType, oldValue);
                return name;
            }
        });
    }

    public boolean isMultiImage(String name) {
        return getResourceObject(name) instanceof MultiImage;
    }


    public void setMultiImage(final String name, final MultiImage value) {
        // we need to replace the image in all the themes...
        final Object oldValue = getResourceObject(name);
        pushUndoable(new UndoableEdit() {
            @Override
            protected String performAction() {
                replaceThemeValue(oldValue, value);
                setResource(name, MAGIC_IMAGE, value);
                return name;
            }

            @Override
            protected String performUndo() {
                replaceThemeValue(value, value);
                setResource(name, MAGIC_IMAGE, oldValue);
                return name;
            }
        });
    }

    public void setSVGDPIs(final String name, final int[] dpi, final int[] widths, final int[] heights) {
        final SVG sv = (SVG)getImage(name).getSVGDocument();
        final int[] currentDPIs = sv.getDpis();
        final int[] currentWidths = sv.getWidthForDPI();
        final int[] currentHeights = sv.getHeightForDPI();
        pushUndoable(new UndoableEdit() {
            @Override
            protected String performAction() {
                sv.setDpis(dpi);
                sv.setWidthForDPI(widths);
                sv.setHeightForDPI(heights);
                return name;
            }

            @Override
            protected String performUndo() {
                sv.setDpis(currentDPIs);
                sv.setWidthForDPI(currentWidths);
                sv.setHeightForDPI(currentHeights);
                return name;
            }
        });
    }

    public void setTheme(final String name, final Hashtable theme) {
        final Hashtable oldTheme = getTheme(name);
        pushUndoable(new UndoableEdit() {
            @Override
            protected String performAction() {
                setResource(name, MAGIC_THEME_LEGACY, theme);
                return name;
            }
            
            @Override
            protected String performUndo() {
                setResource(name, MAGIC_THEME_LEGACY, oldTheme);
                return name;
            }
        });
    }

    public void setL10N(final String name, final Hashtable l10n) {
        final Hashtable oldL10N = (Hashtable)getResourceObject(name);
        pushUndoable(new UndoableEdit() {
            @Override
            protected String performAction() {
                setResource(name, MAGIC_L10N, l10n);
                return name;
            }

            @Override
            protected String performUndo() {
                setResource(name, MAGIC_L10N, oldL10N);
                return name;
            }
        });
    }

    /**
     * Place a locale property into the resource editor
     */
    public void setLocaleProperty(final String localeName, final String locale, final String key, final Object value) {
        final Object oldValue = getL10N(localeName, locale).get(key);
        pushUndoable(new UndoableEdit() {
            @Override
            protected String performAction() {
                if(value == null) {
                    getL10N(localeName, locale).remove(key);
                    return null;
                } else {
                    getL10N(localeName, locale).put(key, value);
                    return localeName;
                }
            }

            @Override
            protected String performUndo() {
                if(oldValue == null) {
                    getL10N(localeName, locale).remove(key);
                } else {
                    getL10N(localeName, locale).put(key, oldValue);
                }
                return localeName;
            }
        });
    }

    /**
     * Remove a locale 
     */
    public void removeLocale(final String localeName, final String locale) {
        final Hashtable current = (Hashtable)((Hashtable)getResourceObject(localeName)).get(locale);
        pushUndoable(new UndoableEdit() {
            @Override
            protected String performAction() {
                ((Hashtable)getResourceObject(localeName)).remove(locale);
                return localeName;
            }

            @Override
            protected String performUndo() {
                ((Hashtable)getResourceObject(localeName)).put(locale, current);
                return localeName;
            }
        });
    }
    
    /**
     * Adds a new locale baring the given name
     */
    public void addLocale(final String localeName, final String locale) {
        pushUndoable(new UndoableEdit() {
            @Override
            protected String performAction() {
                ((Hashtable)getResourceObject(localeName)).put(locale, new Hashtable());
                return localeName;
            }

            @Override
            protected String performUndo() {
                ((Hashtable)getResourceObject(localeName)).remove(locale);
                return localeName;
            }
        });
    }
    
    public Iterator getLocales(String localeName) {
        return ((Hashtable)getResourceObject(localeName)).keySet().iterator();
    }

    /**
     * Place a theme property into the resource editor
     */
    public void setThemeProperty(final String themeName, final String key, final Object value) {
        final Object oldValue = getTheme(themeName).get(key);
        pushUndoable(new UndoableEdit() {
            @Override
            protected String performAction() {
                if(value == null) {
                    getTheme(themeName).remove(key);
                } else {
                    getTheme(themeName).put(key, value);
                }
                return themeName;
            }

            @Override
            protected String performUndo() {
                if(oldValue == null) {
                    getTheme(themeName).remove(key);
                } else {
                    getTheme(themeName).put(key, oldValue);
                }
                return themeName;
            }
        });
    }

    /**
     * Place a theme property into the resource editor
     */
    public void setThemeProperties(final String themeName, final Hashtable newTheme) {
        final Hashtable oldTheme = new Hashtable(getTheme(themeName));
        pushUndoable(new UndoableEdit() {
            @Override
            protected String performAction() {
                getTheme(themeName).clear();
                getTheme(themeName).putAll(newTheme);
                return themeName;
            }

            @Override
            protected String performUndo() {
                getTheme(themeName).clear();
                getTheme(themeName).putAll(oldTheme);
                return themeName;
            }
        });
    }

    /**
     * Place a theme property into the resource editor
     */
    public void setThemeProperties(final String themeName, final String[] keys, final Object[] values) {
        final Object[] oldValues = new Object[keys.length];
        Hashtable th = getTheme(themeName);
        if(th == null) {
            return;
        }
        for(int iter = 0 ; iter < keys.length ; iter++) {
            oldValues[iter] = th.get(keys[iter]);
        }
        pushUndoable(new UndoableEdit() {
            @Override
            protected String performAction() {
                for(int iter = 0 ; iter < keys.length ; iter++) {
                    if(values[iter] == null) {
                        getTheme(themeName).remove(keys[iter]);
                    } else {
                        getTheme(themeName).put(keys[iter], values[iter]);
                    }
                }
                return themeName;
            }

            @Override
            protected String performUndo() {
                for(int iter = 0 ; iter < keys.length ; iter++) {
                    if(oldValues[iter] == null) {
                        getTheme(themeName).remove(keys[iter]);
                    } else {
                        getTheme(themeName).put(keys[iter], oldValues[iter]);
                    }
                }
                return themeName;
            }
        });
    }

    public void setData(final String name, final byte[] data) {
        final byte[] oldData = (byte[])getResourceObject(name);
        pushUndoable(new UndoableEdit() {
            @Override
            protected String performAction() {
                setResource(name, MAGIC_DATA, data);
                return name;
            }

            @Override
            protected String performUndo() {
                setResource(name, MAGIC_DATA, oldData);
                return name;
            }
        });
    }

    public void setUi(final String name, final byte[] data) {
        final byte[] oldData = (byte[])getResourceObject(name);
        pushUndoable(new UndoableEdit() {
            @Override
            protected String performAction() {
                setResource(name, MAGIC_UI, data);
                return name;
            }

            @Override
            protected String performUndo() {
                setResource(name, MAGIC_UI, oldData);
                return name;
            }
        });
    }

    /**
     * Renames an entry in the resource tree
     */
    public void addResourceObjectDuplicate(final String originalName, final String name, final Object value) {
        pushUndoable(new UndoableEdit() {
            private byte type;

            @Override
            protected String performAction() {
                type = getResourceType(originalName);
                setResource(name, type, value);
                return name;
            }

            @Override
            protected String performUndo() {
                setResource(name, type, null);
                return name;
            }
        });
    }

    /**
     * Renames an entry in the resource tree
     */
    public void renameEntry(final String oldName, final String newName) {
        pushUndoable(new UndoableEdit() {
            @Override
            protected String performAction() {
                byte type = getResourceType(oldName);
                Object value = getResourceObject(oldName);
                setResource(oldName, type, null);
                setResource(newName, type, value);
                return newName;
            }

            @Override
            protected String performUndo() {
                byte type = getResourceType(newName);
                Object value = getResourceObject(newName);
                setResource(newName, type, null);
                setResource(oldName, type, value);
                return oldName;
            }
        });
    }
    

    private void saveL10N(DataOutputStream output, Hashtable l10n) throws IOException {
        List keys = new ArrayList();
        for(Object locale : l10n.keySet()) {
            Hashtable current = (Hashtable)l10n.get(locale);
            for(Object key : current.keySet()) {
                if(!keys.contains(key)) {
                    keys.add(key);
                }
            }
        }
        
        output.writeShort(keys.size());
        output.writeShort(l10n.size());

        for(Object key : keys) {
            output.writeUTF((String)key);
        }
        
        for(Object locale : l10n.keySet()) {        
            Hashtable currentLanguage = (Hashtable)l10n.get(locale);
            output.writeUTF((String)locale);
            for(Object key : keys) {
                String k = (String)currentLanguage.get(key);
                if(k != null) {
                    output.writeUTF(k);
                } else {
                    output.writeUTF("");
                }
            }
        }
    }
    
    
    public void setFont(final String name, final com.codename1.ui.Font value) {
        // we need to replace the font in all the themes...
        final com.codename1.ui.Font oldValue = getFont(name);
        if(oldValue == null || !oldValue.equals(value)) {
            pushUndoable(new UndoableEdit() {
                @Override
                protected String performAction() {
                    replaceThemeValue(oldValue, value);
                    setResource(name, MAGIC_FONT, value);
                    return name;
                }

                @Override
                protected String performUndo() {
                    replaceThemeValue(value, oldValue);
                    setResource(name, MAGIC_FONT, oldValue);
                    return name;
                }
            });
        }
    }
    
    public void refreshThemeMultiImages() {
        EditableResources ed = (EditableResources)JavaSEPortWithSVGSupport.getNativeTheme();
        if(ed != null && ed != this) {
            ed.refreshThemeMultiImages();
        }
        for(String themeName : getThemeResourceNames()) {
            Hashtable theme = getTheme(themeName);
            for(Object key : theme.keySet()) {
                Object currentValue = theme.get(key);
                if(currentValue instanceof com.codename1.ui.Image) {
                    String id = findId(currentValue);
                    if(isMultiImage(id)) {
                        theme.put(key, ((MultiImage)getResourceObject(id)).getBest());
                    }
                }
                if(currentValue instanceof com.codename1.ui.plaf.Border) {
                    com.codename1.ui.Image[] images = Accessor.getImages((com.codename1.ui.plaf.Border)currentValue);
                    if(images != null) {
                        for(int iter = 0 ; iter < images.length ; iter++) {
                            com.codename1.ui.Image img = images[iter];
                            if(img != null) {
                                String id = findId(img);
                                if(isMultiImage(id)) {
                                    images[iter] = ((MultiImage)getResourceObject(id)).getBest();
                                }
                            }
                        }
                    }
                }
            }
        }
        com.codename1.ui.Form f = Display.getInstance().getCurrent();
        if(f != null) {
            f.revalidate();
        }
    }

    /**
     * Used when changing a font or an image to update the theme
     */
    private void replaceThemeValue(Object oldValue, Object value) {
        if(oldValue != null && value != null) {
            if(oldValue instanceof MultiImage) {
                MultiImage m = (MultiImage)oldValue;
                Object newValue = value;
                if(newValue instanceof MultiImage) {
                    newValue = ((MultiImage)newValue).getBest();
                }
                for(com.codename1.ui.EncodedImage e : m.getInternalImages()) {
                    replaceThemeValue(e, newValue);
                }
            }
            for(String themeName : getThemeResourceNames()) {
                Hashtable theme = getTheme(themeName);
                for(Object key : theme.keySet()) {
                    Object currentValue = theme.get(key);
                    if(currentValue == oldValue) {
                        theme.put(key, value);
                    }
                }
            }
            // we need to check the existance of image borders to replace images there...
            if(value instanceof Image) {
                for(String themeName : getThemeResourceNames()) {
                    Hashtable theme = getTheme(themeName);
                    for(Object v : theme.values()) {
                        if(v instanceof Border) {
                            Border b = (Border)v;
                            if(Accessor.getType(b) == BORDER_TYPE_IMAGE ||
                                    Accessor.getType(b) == BORDER_TYPE_IMAGE_VERTICAL ||
                                    Accessor.getType(b) == BORDER_TYPE_IMAGE_HORIZONTAL) {
                                Image[] images = Accessor.getImages(b);
                                for(int i = 0 ; i < images.length ; i++) {
                                    if(images[i] == oldValue) {
                                        images[i] = (Image)value;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // check if a timeline is making use of said image and replace it
            for(String image : getImageResourceNames()) {
                com.codename1.ui.Image current = getImage(image);
                if(current instanceof com.codename1.ui.animations.Timeline) {
                    com.codename1.ui.animations.Timeline time = (com.codename1.ui.animations.Timeline)current;
                    for(int iter = 0 ; iter < time.getAnimationCount() ; iter++) {
                        com.codename1.ui.animations.AnimationObject o = time.getAnimation(iter);
                        if(AnimationAccessor.getImage(o) == oldValue) {
                            AnimationAccessor.setImage(o, (com.codename1.ui.Image)value);
                        }
                    }
                }
            }
        }
    }
    
    public void remove(final String name) {
        if(overrideResource != null) {
            overrideResource.remove(name);
            return;
        }
        final Object removedObject = getResourceObject(name);
        final byte type = getResourceType(name);
        pushUndoable(new UndoableEdit() {
            @Override
            protected String performAction() {
                setResource(name, type, null);
                return name;
            }

            @Override
            protected String performUndo() {
                setResource(name, type, removedObject);
                return name;
            }
        });
    }

    public String getResourceTypeAsString(String name) {
        if(name == null) {
            return "";
        }
        byte t = getResourceType(name);
        switch(t) {
            case MAGIC_SVG: return "SVG";
            case MAGIC_TIMELINE: return "Timeline";
            case MAGIC_THEME: return "Theme";
            case MAGIC_FONT: return "Font";
            case MAGIC_IMAGE: return "Image";
            case MAGIC_L10N: return "L10n";
            case MAGIC_DATA: return "Data";
            case MAGIC_UI: return "GUI";
        }
        return "Unknown: " + Integer.toHexString(t & 0xff);
    }
    
    byte getResourceType(String name) {
        if(overrideResource != null && isOverridenResource(name)) {
            return overrideResource.getResourceType(name);
        }
        return super.getResourceType(name);
    }

    public JComponent getResourceEditor(String name, ResourceEditorView  view) {
        byte magic = getResourceType(name);
        switch(magic) {
            case MAGIC_IMAGE:
            case MAGIC_IMAGE_LEGACY:
                Image i = getImage(name);
                if(getResourceObject(name) instanceof MultiImage) {
                    ImageMultiEditor tl = new ImageMultiEditor(this, name, view);
                    tl.setImage((MultiImage)getResourceObject(name));
                    return tl;
                }
                if(i instanceof Timeline) {
                    TimelineEditor tl = new TimelineEditor(this, name, view);
                    tl.setImage((Timeline)i);
                    return tl;
                }
                if(i.isSVG()) {
                    MultiImageSVGEditor img = new MultiImageSVGEditor(this, name);
                    img.setImage(i);
                    return img;
                }
                ImageRGBEditor img = new ImageRGBEditor(this, name, view);
                img.setImage(i);
                return img;
            case MAGIC_TIMELINE:
                TimelineEditor tl = new TimelineEditor(this, name, view);
                tl.setImage((Timeline)getImage(name));
                return tl;
            case MAGIC_THEME:
            case MAGIC_THEME_LEGACY:
                ThemeEditor theme = new ThemeEditor(this, name, getTheme(name), view);
                return theme;
            case MAGIC_FONT:
            case MAGIC_FONT_LEGACY:
            case MAGIC_INDEXED_FONT_LEGACY:
                FontEditor fonts = new FontEditor(this, getFont(name), name);
                return fonts;
            case MAGIC_DATA:
                DataEditor data = new DataEditor(this, name);
                return data;
            case MAGIC_UI:
                UserInterfaceEditor uie = new UserInterfaceEditor(name, this, view.getProjectGeneratorSettings(), view);
                return uie;
            case MAGIC_L10N:
                // we are cheating this isn't a theme but it should work since
                // this is a hashtable that will include the nested locales
                L10nEditor l10n = new L10nEditor(this, name);
                return l10n;
            default:
                throw new IllegalArgumentException("Unrecognized magic number: " + Integer.toHexString(magic & 0xff));
        }
    }

    public static EditableResources open(InputStream resource) throws IOException {
        return new EditableResources(resource);
    }

    // ------------------------------------------------------------------------------------------------
    // Tree Model implementation
    // ------------------------------------------------------------------------------------------------

    private static final Object root = new Object();
    private List<TreeModelListener> listeners = new ArrayList<TreeModelListener>();
    private Node IMAGES = new Node("Images", "images.png") {
        @Override
        public String[] children() {
            return getImageResourceNames();
        }
    };
    private Node THEMES = new Node("Themes", "theme.png") {
        @Override
        public String[] children() {
            return getThemeResourceNames();
        }
    };
    private Node FONTS = new Node("Fonts", "font.png") {
        @Override
        public String[] children() {
            return getFontResourceNames();
        }
    };
    private Node L10N = new Node("Localization", "localization.png") {
        @Override
        public String[] children() {
            return getL10NResourceNames();
        }
    };
    private Node DATA = new Node("Data", "database.png") {
        @Override
        public String[] children() {
            return getDataResourceNames();
        }
    };
    private Node[] nodes = {
        IMAGES, THEMES, FONTS, L10N, DATA
    };
    
    public Object getRoot() {
        return root;
    }

    public Object getChild(Object parent, int index) {
        if(parent == root) {
            return nodes[index];
        }
        return ((Node)parent).children()[index];
    }

    public int getChildCount(Object parent) {
        if(parent == root) {
            return nodes.length;
        }
        if(parent instanceof Node) {
            return ((Node)parent).children().length;
        }
        return 0;
    }

    public boolean isLeaf(Object node) {
        return node instanceof String;
    }

    public void valueForPathChanged(TreePath path, Object newValue) {
        Object oldValue = path.getLastPathComponent();
        renameEntry((String)oldValue, (String)newValue);
        TreeModelEvent ev = new TreeModelEvent(this, path.getParentPath().pathByAddingChild(newValue));
        for(TreeModelListener l : listeners) {
            l.treeNodesChanged(ev);
        }
    }
    
    private Node getParent(byte type) {
        Node parent = IMAGES;
        switch(type) {
            case MAGIC_THEME:
            case MAGIC_THEME_LEGACY:
                parent = THEMES;
                break;
            case MAGIC_FONT:
            case MAGIC_FONT_LEGACY:
                parent = FONTS;
                break;
            case MAGIC_DATA:
                parent = DATA;
                break;
            case MAGIC_UI:
                parent = DATA;
                break;
            case MAGIC_L10N:
                parent = L10N;
                break;
        }
        return parent;
    }

    private TreeModelEvent createEventForNode(String nodeName, byte type, int index) {
        return new TreeModelEvent(this, new Object[]{root, getParent(type)}, new int[] {index}, new Object[] {nodeName});
    }
    
    public void fireTreeNodeAdded(final String nodeName, int index) {
        if(nodeName == null) {
            for(TreeModelListener l : listeners) {
                l.treeNodesInserted(null);
            }
            return;
        }
        TreeModelEvent ev = createEventForNode(nodeName, getResourceType(nodeName), index);
        for(TreeModelListener l : listeners) {
            l.treeNodesInserted(ev);
        }
    }

    public void fireTreeNodeChanged(final String nodeName, int index) {
        TreeModelEvent ev = createEventForNode(nodeName, getResourceType(nodeName), index);
        for(TreeModelListener l : listeners) {
            l.treeNodesChanged(ev);
        }
    }

    public void fireTreeNodeRemoved(final String nodeName, final byte type, int index) {
        TreeModelEvent ev = createEventForNode(nodeName, type, index);
        for(TreeModelListener l : listeners) {
            l.treeNodesRemoved(ev);
        }
    }
    
    public int getIndexOfChild(Object parent, Object child) {
        if(parent == root) {
            for(int i = 0 ; i < nodes.length ; i++) {
                if(nodes[i] == child) {
                    return i;
                }
            }
        }
        String[] c = ((Node)parent).children();
        for(int i = 0 ; i < c.length ; i++) {
            if(c[i] == child) {
                return i;
            }
        }
        return -1;
    }

    public void addTreeModelListener(TreeModelListener l) {
        if(!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }
    
    public static abstract class Node {
        private String name;
        private Icon icon;
        Node(String name, String icon) {
            this.name =name;
        }
        public String getName() {
            return name;
        }
        public Icon getIcon() {
            return icon;
        }
        public abstract String[] children();
    }

    public static class MultiImage {
        private com.codename1.ui.EncodedImage[] internalImages;
        private int[] dpi;

        /**
         * @return the internalImages
         */
        public com.codename1.ui.EncodedImage[] getInternalImages() {
            return internalImages;
        }

        /**
         * @param internalImages the internalImages to set
         */
        public void setInternalImages(com.codename1.ui.EncodedImage[] internalImages) {
            this.internalImages = internalImages;
        }

        /**
         * @return the dpi
         */
        public int[] getDpi() {
            return dpi;
        }

        /**
         * @param dpi the dpi to set
         */
        public void setDpi(int[] dpi) {
            this.dpi = dpi;
        }

        public com.codename1.ui.EncodedImage getBest() {
            if(internalImages.length == 0) {
                return null;
            }
            int dpiVal = com.codename1.ui.Display.getInstance().getDeviceDensity();
            int bestFitOffset = 0;
            int bestFitDPI = 0;
            for(int iter = 0 ; iter < getDpi().length ; iter++) {
                int currentDPI = getDpi()[iter];
                if(bestFitDPI != dpiVal && dpiVal >= currentDPI && currentDPI >= bestFitDPI) {
                    bestFitDPI = currentDPI;
                    bestFitOffset = iter;
                }
            }
            return getInternalImages()[bestFitOffset];
        }
    }
}
