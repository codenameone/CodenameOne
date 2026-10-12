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
package com.codename1.svg.transcoder.model;

/**
 * &lt;text&gt; element. Treated as a single styled run -- &lt;tspan&gt;
 * children inherit the parent text's style but are flattened into the
 * combined content string. Supports SVG's start / middle / end anchor
 * positioning and a single (x, y) baseline; per-character coordinate
 * lists are not implemented.
 */
public final class SVGText extends SVGShape {

    public enum Anchor { START, MIDDLE, END }

    private float x;
    private float y;
    private String content = "";
    private String fontFamily;
    private float fontSize;
    private boolean bold;
    private boolean italic;
    private Anchor anchor = Anchor.START;

    /**
     * One stretch of the text: what is directly in the element, or in one
     * {@code <tspan>} of it. A stretch can be moved, and styled and set in a
     * font of its own; what it leaves null it has from the text element.
     */
    public static final class Run {
        private String text = "";
        private Float x;
        private Float y;
        private float dx;
        private float dy;
        private com.codename1.svg.transcoder.parser.SVGStyle style;
        private String fontFamily;
        private Float fontSize;
        private Boolean bold;
        private Boolean italic;

        private Float letterSpacing;
        /// The `letter-spacing` a span sets, or null for the text's own.
        public Float getLetterSpacing() { return letterSpacing; }
        public void setLetterSpacing(Float letterSpacing) { this.letterSpacing = letterSpacing; }

        private Anchor anchor;
        /// The `text-anchor` a span sets, or null for the text's own.
        public Anchor getAnchor() { return anchor; }
        public void setAnchor(Anchor anchor) { this.anchor = anchor; }

        private String pathRef;
        private float startOffset;
        private boolean startOffsetShare;

        /// The id of the path a `<textPath>` lays this text along, or null
        /// for text on a straight line.
        public String getPathRef() { return pathRef; }
        public void setPathRef(String pathRef) { this.pathRef = pathRef; }
        /// How far along that path the text begins: a length, or with
        /// [#isStartOffsetShare] a fraction of the path's own.
        public float getStartOffset() { return startOffset; }
        public boolean isStartOffsetShare() { return startOffsetShare; }
        public void setStartOffset(float startOffset, boolean share) {
            this.startOffset = startOffset;
            this.startOffsetShare = share;
        }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        /** Where the stretch starts, when it says; null to go on from the one before. */
        public Float getX() { return x; }
        public void setX(Float x) { this.x = x; }
        public Float getY() { return y; }
        public void setY(Float y) { this.y = y; }
        /** How far it is moved from where it would start. */
        public float getDx() { return dx; }
        public void setDx(float dx) { this.dx = dx; }
        public float getDy() { return dy; }
        public void setDy(float dy) { this.dy = dy; }
        /** What the span sets for itself, or null. */
        public com.codename1.svg.transcoder.parser.SVGStyle getStyle() { return style; }
        public void setStyle(com.codename1.svg.transcoder.parser.SVGStyle style) { this.style = style; }
        public String getFontFamily() { return fontFamily; }
        public void setFontFamily(String v) { this.fontFamily = v; }
        public Float getFontSize() { return fontSize; }
        public void setFontSize(Float v) { this.fontSize = v; }
        public Boolean getBold() { return bold; }
        public void setBold(Boolean v) { this.bold = v; }
        public Boolean getItalic() { return italic; }
        public void setItalic(Boolean v) { this.italic = v; }
    }

    private final java.util.List<Run> runs = new java.util.ArrayList<Run>();

    /**
     * The text in the stretches it is written in, white space collapsed as
     * a browser collapses it. {@link #getContent()} is the same text as one
     * string, as written, for a renderer that draws it in one go.
     */
    public java.util.List<Run> getRuns() { return runs; }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content == null ? "" : content; }

    public String getFontFamily() { return fontFamily; }
    public void setFontFamily(String fontFamily) { this.fontFamily = fontFamily; }

    public float getFontSize() { return fontSize; }
    public void setFontSize(float fontSize) { this.fontSize = fontSize; }

    public boolean isBold() { return bold; }
    public void setBold(boolean bold) { this.bold = bold; }

    public boolean isItalic() { return italic; }
    public void setItalic(boolean italic) { this.italic = italic; }

    private float letterSpacing;
    /// How much further apart than the font has them the glyphs are set;
    /// zero for `normal`. Only a renderer that reads the model applies it.
    public float getLetterSpacing() { return letterSpacing; }
    public void setLetterSpacing(float letterSpacing) { this.letterSpacing = letterSpacing; }

    public Anchor getAnchor() { return anchor; }
    public void setAnchor(Anchor anchor) { this.anchor = anchor == null ? Anchor.START : anchor; }
}
