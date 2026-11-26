/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.presentation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// A slide presentation: slides of a common size, the theme they were designed with, and the
/// formats it is read from and written to.
///
/// A presentation is immutable. `PresentationModel` is the editable, undoable facade a
/// `SlideView` edits; `SlideShow` plays one.
///
/// ```java
/// void toPdf(InputStream in, OutputStream out) throws IOException {
///     Presentation deck = Presentation.read(in, Presentation.Format.PPTX);
///     Log.p(deck.getSlides().size() + " slides, first: " + deck.getSlides().get(0).title());
///     deck.write(out, Presentation.Format.PDF);
/// }
/// ```
///
/// Text inside slides is the word processing model of `com.codename1.wordprocessing`, so
/// every formatting feature a document has -- fonts, colors, lists, links, tables -- a slide
/// has too, laid out and drawn by the same code.
public final class Presentation {
    /// The formats a presentation is read from and written to.
    public enum Format {
        /// Codename One's own `.cn1slides`: a ZIP holding `presentation.xml`, with each text
        /// box in the `.cn1doc` vocabulary. Keeps everything the model holds, diagrams
        /// included.
        NATIVE("cn1slides", true, true),

        /// Office Open XML, the format of PowerPoint 2007 and later. Reads and writes shapes
        /// with preset geometry, fills, outlines and arrowheads, text with layout and master
        /// placeholder inheritance, pictures with cropping, tables with merged cells, groups,
        /// charts, speaker notes, transitions and entrance/exit animations. A diagram is
        /// written as a group of shapes.
        PPTX("pptx", true, true),

        /// OpenDocument presentation, the format of LibreOffice Impress and Keynote's
        /// export. Reads and writes the same features as PPTX.
        ODP("odp", true, true),

        /// Apple Keynote 2013 and later (`.key`). Read only: slides, text boxes and
        /// placeholders with their text, shapes, pictures and speaker notes are read from the
        /// package's archived objects. What cannot be decoded is listed in `getWarnings()`.
        KEY("key", true, false),

        /// PowerPoint 97-2003 (`.ppt`). Read only: text, shapes, pictures, notes and slide
        /// backgrounds.
        PPT("ppt", true, false),

        /// PDF, one page per visible slide. Write only.
        PDF("pdf", false, true);

        private final String extension;
        private final boolean readable;
        private final boolean writable;

        Format(String extension, boolean readable, boolean writable) {
            this.extension = extension;
            this.readable = readable;
            this.writable = writable;
        }

        /// The file extension without a dot.
        ///
        /// #### Returns
        ///
        /// the extension
        public String extension() {
            return extension;
        }

        /// True when a presentation can be read from this format.
        ///
        /// #### Returns
        ///
        /// true when readable
        public boolean canRead() {
            return readable;
        }

        /// True when a presentation can be written in this format.
        ///
        /// #### Returns
        ///
        /// true when writable
        public boolean canWrite() {
            return writable;
        }

        /// The format a file name's extension names, ignoring case.
        ///
        /// #### Parameters
        ///
        /// - `fileName`: the name
        ///
        /// #### Returns
        ///
        /// the format, or null when the extension is not a presentation format
        public static Format forFileName(String fileName) {
            if (fileName == null) {
                return null;
            }
            int dot = fileName.lastIndexOf('.');
            if (dot < 0) {
                return null;
            }
            String ext = fileName.substring(dot + 1);
            for (Format f : values()) {
                if (f.extension.equalsIgnoreCase(ext)) {
                    return f;
                }
            }
            return null;
        }
    }

    /// 16:9 at 960 by 540 points, PowerPoint's and Keynote's default.
    public static final float WIDE_WIDTH = 960;

    /// The height of a 16:9 slide in points.
    public static final float WIDE_HEIGHT = 540;

    /// The slide width in points.
    public final float width;

    /// The slide height in points.
    public final float height;

    /// The slides, never null.
    public final List<Slide> slides;

    /// The theme.
    public final Theme theme;

    /// The title, or null.
    public final String title;

    /// The next number `newId` hands out.
    public final long nextId;

    private final List<String> warnings;

    /// An empty 16:9 presentation with the default theme.
    public Presentation() {
        this(WIDE_WIDTH, WIDE_HEIGHT, null, Theme.DEFAULT, null, 1, null);
    }

    /// A presentation.
    ///
    /// #### Parameters
    ///
    /// - `width`: the slide width in points, greater than zero
    ///
    /// - `height`: the slide height in points, greater than zero
    ///
    /// - `slides`: the slides, or null
    ///
    /// - `theme`: the theme, or null for the default
    ///
    /// - `title`: the title, or null
    ///
    /// - `nextId`: the next id number
    ///
    /// - `warnings`: what a reader could not convert, or null
    public Presentation(float width, float height, List<Slide> slides, Theme theme,
            String title, long nextId, List<String> warnings) {
        // !(width > 0) also refuses NaN, which width <= 0 lets through
        if (!(width > 0) || !(height > 0) || width > 100000 || height > 100000) { // NOPMD LogicInversion
            throw new IllegalArgumentException("Bad slide size " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        List<Slide> s = new ArrayList<Slide>();
        if (slides != null) {
            for (Slide x : slides) {
                if (x != null) {
                    s.add(x);
                }
            }
        }
        this.slides = Collections.unmodifiableList(s);
        this.theme = theme == null ? Theme.DEFAULT : theme;
        this.title = title;
        this.nextId = Math.max(1, nextId);
        List<String> w = new ArrayList<String>();
        if (warnings != null) {
            w.addAll(warnings);
        }
        this.warnings = Collections.unmodifiableList(w);
    }

    /// The slides.
    ///
    /// #### Returns
    ///
    /// the slides
    public List<Slide> getSlides() {
        return slides;
    }

    /// What the reader could not convert -- an unknown element, an undecodable Keynote
    /// object -- one line each. Empty for a presentation made in code.
    ///
    /// #### Returns
    ///
    /// the warnings
    public List<String> getWarnings() {
        return warnings;
    }

    /// An id that no element or slide in this presentation has, for a new element made from
    /// it. The presentation is immutable, so callers that make several ids in a row use
    /// `PresentationModel.newId` or `withNextId`.
    ///
    /// #### Parameters
    ///
    /// - `prefix`: a letter that starts the id
    ///
    /// #### Returns
    ///
    /// the id
    public String newId(char prefix) {
        return String.valueOf(prefix) + nextId;
    }

    /// A slide by id.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// #### Returns
    ///
    /// the index or -1
    public int indexOf(String id) {
        for (int i = 0; i < slides.size(); i++) {
            if (slides.get(i).id.equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /// This presentation with other slides.
    ///
    /// #### Parameters
    ///
    /// - `value`: the slides
    ///
    /// #### Returns
    ///
    /// the presentation
    public Presentation withSlides(List<Slide> value) {
        return new Presentation(width, height, value, theme, title, nextId, warnings);
    }

    /// This presentation with one slide replaced.
    ///
    /// #### Parameters
    ///
    /// - `index`: the position
    ///
    /// - `slide`: the slide
    ///
    /// #### Returns
    ///
    /// the presentation
    public Presentation withSlide(int index, Slide slide) {
        List<Slide> l = new ArrayList<Slide>(slides);
        l.set(index, slide);
        return withSlides(l);
    }

    /// This presentation with a slide inserted.
    ///
    /// #### Parameters
    ///
    /// - `index`: the position
    ///
    /// - `slide`: the slide
    ///
    /// #### Returns
    ///
    /// the presentation
    public Presentation insert(int index, Slide slide) {
        List<Slide> l = new ArrayList<Slide>(slides);
        l.add(index, slide);
        return withSlides(l);
    }

    /// This presentation without a slide.
    ///
    /// #### Parameters
    ///
    /// - `index`: the position
    ///
    /// #### Returns
    ///
    /// the presentation
    public Presentation remove(int index) {
        List<Slide> l = new ArrayList<Slide>(slides);
        l.remove(index);
        return withSlides(l);
    }

    /// This presentation at another slide size. Elements keep their positions in points.
    ///
    /// #### Parameters
    ///
    /// - `w`: the width in points
    ///
    /// - `h`: the height in points
    ///
    /// #### Returns
    ///
    /// the presentation
    public Presentation withSize(float w, float h) {
        return new Presentation(w, h, slides, theme, title, nextId, warnings);
    }

    /// This presentation with another theme.
    ///
    /// #### Parameters
    ///
    /// - `value`: the theme
    ///
    /// #### Returns
    ///
    /// the presentation
    public Presentation withTheme(Theme value) {
        return new Presentation(width, height, slides, value, title, nextId, warnings);
    }

    /// This presentation with another title.
    ///
    /// #### Parameters
    ///
    /// - `value`: the title
    ///
    /// #### Returns
    ///
    /// the presentation
    public Presentation withTitle(String value) {
        return new Presentation(width, height, slides, theme, value, nextId, warnings);
    }

    /// This presentation with another next id number.
    ///
    /// #### Parameters
    ///
    /// - `value`: the number
    ///
    /// #### Returns
    ///
    /// the presentation
    public Presentation withNextId(long value) {
        return new Presentation(width, height, slides, theme, title, value, warnings);
    }

    /// This presentation with warnings.
    ///
    /// #### Parameters
    ///
    /// - `value`: the warnings
    ///
    /// #### Returns
    ///
    /// the presentation
    public Presentation withWarnings(List<String> value) {
        return new Presentation(width, height, slides, theme, title, nextId, value);
    }

    /// The text of every slide, slide by slide.
    ///
    /// #### Returns
    ///
    /// the text
    public String plainText() {
        StringBuilder sb = new StringBuilder();
        for (Slide s : slides) {
            String t = s.plainText();
            if (t.length() > 0) {
                if (sb.length() > 0) {
                    sb.append("\n\n");
                }
                sb.append(t);
            }
        }
        return sb.toString();
    }

    /// Reads a presentation.
    ///
    /// #### Parameters
    ///
    /// - `in`: the stream; it is read to the end but not closed
    ///
    /// - `format`: the format, which must be readable
    ///
    /// #### Returns
    ///
    /// the presentation
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the stream fails or the file is not in that format
    public static Presentation read(InputStream in, Format format) throws IOException {
        if (in == null || format == null) {
            throw new IllegalArgumentException("A stream and a format are required");
        }
        switch (format) {
            case NATIVE:
                return NativeSlidesFormat.read(in);
            case PPTX:
                return PptxReader.read(in);
            case ODP:
                return OdpReader.read(in);
            case KEY:
                return KeynoteReader.read(in);
            case PPT:
                return PptReader.read(in);
            default:
                throw new IOException(format + " cannot be read");
        }
    }

    /// Reads a presentation from bytes, telling the format from the content: a ZIP holding
    /// `ppt/presentation.xml` is PPTX, one with an ODF presentation mimetype is ODP, one
    /// with `Index/` archives is Keynote, one holding `presentation.xml` alone is
    /// `.cn1slides`, and a compound file is PowerPoint 97-2003.
    ///
    /// #### Parameters
    ///
    /// - `data`: the file
    ///
    /// #### Returns
    ///
    /// the presentation
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the bytes are none of the formats
    public static Presentation read(byte[] data) throws IOException {
        return read(new ByteArrayInputStream(data), detect(data));
    }

    /// Tells the format of a file from its content.
    ///
    /// #### Parameters
    ///
    /// - `data`: the file
    ///
    /// #### Returns
    ///
    /// the format
    ///
    /// #### Throws
    ///
    /// - `IOException`: when it is none of the readable formats
    public static Format detect(byte[] data) throws IOException {
        if (data == null || data.length < 8) {
            throw new IOException("Not a presentation");
        }
        if (com.codename1.impl.office.CompoundFile.hasSignature(data)) {
            return Format.PPT;
        }
        if (data[0] != 'P' || data[1] != 'K') {
            throw new IOException("Not a presentation");
        }
        if (contains(data, "ppt/presentation.xml")) {
            return Format.PPTX;
        }
        if (contains(data, "application/vnd.oasis.opendocument.presentation")) {
            return Format.ODP;
        }
        if (contains(data, "Index/Document.iwa") || contains(data, ".iwa")) {
            return Format.KEY;
        }
        if (contains(data, "presentation.xml")) {
            return Format.NATIVE;
        }
        throw new IOException("Not a presentation");
    }

    private static boolean contains(byte[] data, String s) {
        int n = s.length();
        for (int i = 0; i + n <= data.length; i++) {
            if (matchesAt(data, i, s)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAt(byte[] data, int at, String s) {
        for (int j = 0; j < s.length(); j++) {
            if (data[at + j] != s.charAt(j)) {
                return false;
            }
        }
        return true;
    }

    /// Writes the presentation.
    ///
    /// #### Parameters
    ///
    /// - `out`: the stream; it is flushed but not closed
    ///
    /// - `format`: the format, which must be writable
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the stream fails or the format cannot be written
    public void write(OutputStream out, Format format) throws IOException {
        if (out == null || format == null) {
            throw new IllegalArgumentException("A stream and a format are required");
        }
        switch (format) {
            case NATIVE:
                NativeSlidesFormat.write(this, out);
                break;
            case PPTX:
                PptxWriter.write(this, out);
                break;
            case ODP:
                OdpWriter.write(this, out);
                break;
            case PDF:
                SlidePdfExport.write(this, out);
                break;
            default:
                throw new IOException(format + " cannot be written");
        }
        out.flush();
    }

    /// The presentation as the bytes of a file.
    ///
    /// #### Parameters
    ///
    /// - `format`: a writable format
    ///
    /// #### Returns
    ///
    /// the bytes
    ///
    /// #### Throws
    ///
    /// - `IOException`: when the format cannot be written
    public byte[] toBytes(Format format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, format);
        return out.toByteArray();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Presentation)) {
            return false;
        }
        Presentation p = (Presentation) o;
        return Float.floatToIntBits(width) == Float.floatToIntBits(p.width) && Float.floatToIntBits(height) == Float.floatToIntBits(p.height) && slides.equals(p.slides)
                && theme.equals(p.theme) && (title == null ? p.title == null
                : title.equals(p.title));
    }

    @Override
    public int hashCode() {
        return slides.hashCode() * 31 + Float.floatToIntBits(width);
    }
}
