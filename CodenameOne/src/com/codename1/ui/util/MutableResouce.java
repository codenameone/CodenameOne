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

/// Mutable variant of {@link Resources} intended for non-designer environments.
///
/// This class provides a minimal API for programmatically editing resource files
/// inside Codename One core code where JavaSE/Swing designer classes are not available.
///
/// ## Supported resource categories
///
/// - `IMAGE`
/// - `DATA`
/// - `L10N`
/// - `THEME` (in-memory editing only; save serialization is intentionally limited)
///
/// ## Explicitly unsupported categories
///
/// The following are intentionally rejected with `UnsupportedOperationException`
/// when read or written:
///
/// - Timeline
/// - Indexed images
/// - SVG
/// - GUI Builder UI resources
public class MutableResouce extends Resources {
    private static final short MINOR_VERSION = 12;
    private static final short MAJOR_VERSION = 1;

    private boolean modified;

    /// Creates an empty mutable resource container.
    public MutableResouce() {
        super();
    }

    /// Creates a mutable resource container by loading a resource stream.
    ///
    /// @param input source stream containing a `.res` payload.
    /// @throws IOException if the stream cannot be parsed.
    MutableResouce(InputStream input) throws IOException {
        super();
        openFile(input);
    }

    /// Opens a mutable resource from an input stream.
    ///
    /// @param resource source stream.
    /// @return loaded mutable resource.
    /// @throws IOException if parsing fails.
    public static MutableResouce open(InputStream resource) throws IOException {
        return new MutableResouce(resource);
    }

    /// Validates each top-level resource entry while loading.
    ///
    /// @param id resource id currently being read.
    /// @param magic resource type magic.
    @Override
    void startingEntry(String id, byte magic) {
        if (magic == MAGIC_TIMELINE || magic == MAGIC_SVG || magic == MAGIC_INDEXED_IMAGE_LEGACY || magic == MAGIC_UI) {
            throw new UnsupportedOperationException("Unsupported resource type in MutableResouce: " + Integer.toHexString(magic & 0xff));
        }
    }

    /// Creates an image entry while rejecting unsupported image subtypes.
    ///
    /// @param input data stream.
    /// @return decoded image.
    /// @throws IOException if decoding fails.
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
                throw new UnsupportedOperationException("Unsupported image subtype in MutableResouce: " + Integer.toHexString(type));
            default:
                throw new IOException("Illegal type while creating image: " + Integer.toHexString(type));
        }
    }

    /// Stores or removes an image resource.
    ///
    /// @param name resource id.
    /// @param value image value, or `null` to remove.
    public void setImage(String name, Image value) {
        if (value instanceof Timeline) {
            throw new UnsupportedOperationException("Timeline resources are not supported in MutableResouce");
        }
        if (value != null && value.isSVG()) {
            throw new UnsupportedOperationException("SVG resources are not supported in MutableResouce");
        }
        setResource(name, MAGIC_IMAGE, value);
        modified = true;
    }

    /// Stores or removes raw data resource bytes.
    ///
    /// @param name resource id.
    /// @param data payload bytes, or `null` to remove.
    public void setData(String name, byte[] data) {
        setResource(name, MAGIC_DATA, data);
        modified = true;
    }

    /// Stores or replaces a theme resource.
    ///
    /// @param name theme resource id.
    /// @param theme theme properties map.
    public void setTheme(String name, Hashtable theme) {
        setResource(name, MAGIC_THEME, theme);
        modified = true;
    }

    /// Sets or removes a single property inside a named theme.
    ///
    /// If the theme does not exist, a new map is created.
    /// Passing `null` value removes the property.
    ///
    /// @param themeName theme resource id.
    /// @param key theme property key.
    /// @param value theme property value or `null` to remove.
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

    /// Stores or replaces an L10N resource bundle.
    ///
    /// @param name l10n resource id.
    /// @param l10n locale->translation map.
    public void setL10N(String name, Hashtable l10n) {
        setResource(name, MAGIC_L10N, l10n);
        modified = true;
    }

    /// Returns raw bytes for a named data resource.
    ///
    /// @param id resource id.
    /// @return data bytes, or `null` if missing.
    public byte[] getDataByteArray(String id) {
        return (byte[]) getResourceObject(id);
    }

    /// Checks whether any resource exists for a given id.
    ///
    /// @param name resource id.
    /// @return `true` when an entry exists.
    public boolean containsResource(String name) {
        return getResourceObject(name) != null;
    }

    /// Indicates whether this resource set has pending edits.
    ///
    /// @return `true` if modified.
    public boolean isModified() {
        return modified;
    }

    /// Unsupported GUI builder setter.
    ///
    /// @param name ignored.
    /// @param data ignored.
    public void setUi(String name, byte[] data) {
        throw new UnsupportedOperationException("GUI Builder resources are not supported in MutableResouce");
    }

    /// Unsupported timeline setter.
    ///
    /// @param name ignored.
    /// @param timeline ignored.
    public void setTimeline(String name, Timeline timeline) {
        throw new UnsupportedOperationException("Timeline resources are not supported in MutableResouce");
    }

    /// Unsupported indexed image setter.
    ///
    /// @param name ignored.
    /// @param image ignored.
    public void setIndexedImage(String name, Image image) {
        throw new UnsupportedOperationException("Indexed image resources are not supported in MutableResouce");
    }

    /// Unsupported SVG setter.
    ///
    /// @param name ignored.
    /// @param image ignored.
    public void setSVG(String name, Image image) {
        throw new UnsupportedOperationException("SVG resources are not supported in MutableResouce");
    }

    /// Clears all resources and resets the modified flag.
    @Override
    public void clear() {
        super.clear();
        modified = false;
    }

    /// Serializes the current resources into a `.res` stream.
    ///
    /// Entries are written in deterministic sorted order by id.
    ///
    /// @param out output stream.
    /// @throws IOException if writing fails.
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
                throw new UnsupportedOperationException("Unsupported resource type in MutableResouce: " + Integer.toHexString(magic & 0xff));
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
                    throw new IOException("MutableResouce save() currently supports IMAGE/DATA/L10N only. Unsupported type: " + Integer.toHexString(magic & 0xff));
            }
        }
        modified = false;
    }

    /// Writes an encoded image entry.
    ///
    /// @param output output stream.
    /// @param image image value.
    /// @throws IOException if writing fails.
    protected void writeImage(DataOutputStream output, Image image) throws IOException {
        if (image instanceof Timeline) {
            throw new UnsupportedOperationException("Timeline resources are not supported in MutableResouce");
        }
        if (image.isSVG()) {
            throw new UnsupportedOperationException("SVG resources are not supported in MutableResouce");
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

    /// Writes an L10N map in resource-file format.
    ///
    /// @param output output stream.
    /// @param l10n locale map.
    /// @throws IOException if writing fails.
    protected void saveL10N(DataOutputStream output, Hashtable l10n) throws IOException {
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

    /// Returns the data resource as a new input stream.
    ///
    /// @param id resource id.
    /// @return data stream or `null`.
    @Override
    public InputStream getData(String id) {
        byte[] data = getDataByteArray(id);
        if (data == null) {
            return null;
        }
        return new ByteArrayInputStream(data);
    }
}
