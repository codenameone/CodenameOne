/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 */
package com.codename1.ui.util;

import com.codename1.ui.EncodedImage;
import com.codename1.ui.Image;
import com.codename1.ui.animations.Timeline;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

/**
 * Editable variant of {@link Resources} for environments where the JavaSE designer classes aren't available.
 * <p>
 * This implementation intentionally does not support timeline, indexed image, SVG, or GUI builder resources.
 */
public class EditableResources extends Resources {
    private static final short MINOR_VERSION = 12;
    private static final short MAJOR_VERSION = 1;

    private boolean modified;

    public EditableResources() {
        super();
    }

    EditableResources(InputStream input) throws IOException {
        super();
        openFile(input);
    }

    public static EditableResources open(InputStream resource) throws IOException {
        return new EditableResources(resource);
    }

    @Override
    void startingEntry(String id, byte magic) {
        if (magic == MAGIC_TIMELINE || magic == MAGIC_SVG || magic == MAGIC_INDEXED_IMAGE_LEGACY || magic == MAGIC_UI) {
            throw new UnsupportedOperationException("Unsupported resource type in EditableResources: " + Integer.toHexString(magic & 0xff));
        }
    }

    @Override
    Image createImage(DataInputStream input) throws IOException {
        if (majorVersion == 0 && minorVersion == 0) {
            return super.createImage(input);
        }
        int type = input.readByte() & 0xff;
        switch (type) {
            case 0xF1:
            case 0xF2: {
                byte[] data = new byte[input.readInt()];
                input.readFully(data, 0, data.length);
                int width = input.readInt();
                int height = input.readInt();
                boolean opaque = input.readBoolean();
                return EncodedImage.create(data, width, height, opaque);
            }
            case 0xF6:
                return readMultiImage(input, false);
            case 0xEF:
            case 0xF3:
            case 0xF5:
            case 0xF7:
                throw new UnsupportedOperationException("Unsupported image subtype in EditableResources: " + Integer.toHexString(type));
            default:
                throw new IOException("Illegal type while creating image: " + Integer.toHexString(type));
        }
    }

    public void setImage(String name, Image value) {
        if (value instanceof Timeline) {
            throw new UnsupportedOperationException("Timeline resources are not supported in EditableResources");
        }
        if (value != null && value.isSVG()) {
            throw new UnsupportedOperationException("SVG resources are not supported in EditableResources");
        }
        setResource(name, MAGIC_IMAGE, value);
        modified = true;
    }

    public void setData(String name, byte[] data) {
        setResource(name, MAGIC_DATA, data);
        modified = true;
    }



    public void setTheme(String name, Hashtable theme) {
        setResource(name, MAGIC_THEME, theme);
        modified = true;
    }


    public void setThemeProperty(String themeName, String key, Object value) {
        Hashtable theme = getTheme(themeName);
        if (theme == null) {
            theme = new Hashtable();
        }
        if (value == null) {
            theme.remove(key);
        } else {
            theme.put(key, value);
        }
        setResource(themeName, MAGIC_THEME, theme);
        modified = true;
    }

    public void setL10N(String name, Hashtable l10n) {
        setResource(name, MAGIC_L10N, l10n);
        modified = true;
    }

    public byte[] getDataByteArray(String id) {
        return (byte[]) getResourceObject(id);
    }

    public boolean containsResource(String name) {
        return getResourceObject(name) != null;
    }

    public boolean isModified() {
        return modified;
    }

    public void setUi(String name, byte[] data) {
        throw new UnsupportedOperationException("GUI Builder resources are not supported in EditableResources");
    }

    public void setTimeline(String name, Timeline timeline) {
        throw new UnsupportedOperationException("Timeline resources are not supported in EditableResources");
    }

    public void setIndexedImage(String name, Image image) {
        throw new UnsupportedOperationException("Indexed image resources are not supported in EditableResources");
    }

    public void setSVG(String name, Image image) {
        throw new UnsupportedOperationException("SVG resources are not supported in EditableResources");
    }

    @Override
    public void clear() {
        super.clear();
        modified = false;
    }

    public void save(OutputStream out) throws IOException {
        String[] resourceNames = getResourceNames();
        Arrays.sort(resourceNames);

        DataOutputStream output = new DataOutputStream(out);
        output.writeShort(resourceNames.length + 1);
        output.writeByte(MAGIC_HEADER);
        output.writeUTF("");
        output.writeShort(6);
        output.writeShort(MAJOR_VERSION);
        output.writeShort(MINOR_VERSION);
        output.writeShort(0);

        for (String resourceName : resourceNames) {
            byte magic = getResourceType(resourceName);
            Object value = getResourceObject(resourceName);
            if (magic == MAGIC_UI || magic == MAGIC_TIMELINE || magic == MAGIC_SVG || magic == MAGIC_INDEXED_IMAGE_LEGACY) {
                throw new UnsupportedOperationException("Unsupported resource type in EditableResources: " + Integer.toHexString(magic & 0xff));
            }
            output.writeByte(magic);
            output.writeUTF(resourceName);
            switch (magic) {
                case MAGIC_IMAGE:
                    writeImage(output, (Image) value);
                    break;
                case MAGIC_DATA:
                    byte[] data = (byte[]) value;
                    output.writeInt(data.length);
                    output.write(data);
                    break;
                case MAGIC_L10N:
                    saveL10N(output, (Hashtable) value);
                    break;
                default:
                    throw new IOException("EditableResources save() currently supports IMAGE/DATA/L10N only. Unsupported type: " + Integer.toHexString(magic & 0xff));
            }
        }
        modified = false;
    }

    private void writeImage(DataOutputStream output, Image image) throws IOException {
        if (image instanceof Timeline) {
            throw new UnsupportedOperationException("Timeline resources are not supported in EditableResources");
        }
        if (image.isSVG()) {
            throw new UnsupportedOperationException("SVG resources are not supported in EditableResources");
        }
        EncodedImage enc = image instanceof EncodedImage ? (EncodedImage) image : EncodedImage.createFromImage(image, false);
        byte[] bytes = enc.getImageData();
        output.writeByte(0xF1);
        output.writeInt(bytes.length);
        output.write(bytes);
        output.writeInt(enc.getWidth());
        output.writeInt(enc.getHeight());
        output.writeBoolean(enc.isOpaque());
    }

    private void saveL10N(DataOutputStream output, Hashtable l10n) throws IOException {
        ArrayList keys = new ArrayList();
        for (Object locale : l10n.keySet()) {
            Hashtable current = (Hashtable) l10n.get(locale);
            for (Object key : current.keySet()) {
                if (!keys.contains(key)) {
                    keys.add(key);
                }
            }
        }

        output.writeShort(keys.size());
        output.writeShort(l10n.size());

        for (Object key : keys) {
            output.writeUTF((String) key);
        }

        for (Object locale : l10n.keySet()) {
            Hashtable currentLanguage = (Hashtable) l10n.get(locale);
            output.writeUTF((String) locale);
            for (Object key : keys) {
                String k = (String) currentLanguage.get(key);
                output.writeUTF(k == null ? "" : k);
            }
        }
    }

    @Override
    public InputStream getData(String id) {
        byte[] data = getDataByteArray(id);
        if (data == null) {
            return null;
        }
        return new ByteArrayInputStream(data);
    }
}
