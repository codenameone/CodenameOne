/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.widgets;

import com.codename1.flutter.RenderElement;
import com.codename1.flutter.TextAlign;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.Size;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.RichTextComponent;
import com.codename1.ui.geom.Dimension;

import java.util.ArrayList;
import java.util.List;

/**
 * Render box for {@link RichText}: it {@link #flatten flattens} the {@link TextSpan} tree into
 * resolved {@link Run}s and renders them with a shared {@link RichTextComponent} (UIID
 * "FlutterRichText"), which owns word wrapping, per-run styling and multi-size line layout. The
 * same component backs the general-purpose Codename One rich text API, so RichText inherits its
 * rendering rather than duplicating a wrapping engine.
 *
 * <p>Span flattening and style resolution live here (they are pure and headless-testable); wrapping
 * and painting are delegated to {@link RichTextComponent}.</p>
 */
public class RichTextRenderElement extends RenderElement {

    public RichTextRenderElement(RichText widget) {
        super(widget);
    }

    private RichText richText() {
        return (RichText) widget();
    }

    // ------------------------------------------------------------------
    // Component
    // ------------------------------------------------------------------

    @Override
    protected Component createComponent() {
        if (!Display.isInitialized()) {
            // headless unit tests: no CN1 components can exist
            return null;
        }
        RichTextComponent c = new RichTextComponent();
        c.setUIID("FlutterRichText");
        c.getAllStyles().setPadding(0, 0, 0, 0);
        c.getAllStyles().setMargin(0, 0, 0, 0);
        c.getAllStyles().setBgTransparency(0);
        applyContent(c);
        return c;
    }

    @Override
    protected void updateComponent(Component comp) {
        applyContent((RichTextComponent) comp);
    }

    /** Rebuilds the component's content from the current span tree and alignment. */
    private void applyContent(RichTextComponent c) {
        c.clear();
        for (Run run : flatten(richText().getText())) {
            c.append(run.text, toEditorStyle(run.style));
        }
        c.setTextAlign(cn1Align());
    }

    private int cn1Align() {
        TextAlign a = richText().getTextAlign();
        if (a == null) {
            return Component.LEFT;
        }
        switch (a) {
            case right:
            case end:
                return Component.RIGHT;
            case center:
                return Component.CENTER;
            default:
                return Component.LEFT;
        }
    }

    /**
     * Maps a resolved Flutter {@link TextStyle} to the editor {@link com.codename1.ui.editor.TextStyle}
     * the rich text component consumes: font size becomes an absolute pixel size, bold weight and
     * color carry over. (This minimal Flutter TextStyle exposes no italic/decoration.)
     */
    static com.codename1.ui.editor.TextStyle toEditorStyle(TextStyle style) {
        com.codename1.ui.editor.TextStyle s = com.codename1.ui.editor.TextStyle.DEFAULT;
        if (style == null) {
            return s;
        }
        if (style.getFontSize() != null) {
            s = s.withFontSizePx((int) Math.round(Dp.px(style.getFontSize())));
        }
        if (style.getFontWeight() != null && style.getFontWeight().isBold()) {
            s = s.withBold(true);
        }
        if (style.getColor() != null) {
            s = s.withForeColor(style.getColor().rgb());
        }
        return s;
    }

    // ------------------------------------------------------------------
    // Layout (delegated to the rich text component's height-for-width sizing)
    // ------------------------------------------------------------------

    @Override
    protected Size performLayout(BoxConstraints constraints) {
        RichTextComponent c = (RichTextComponent) component();
        if (c == null) {
            return constraints.smallest();
        }
        double maxWidth = constraints.maxWidth();
        int w = maxWidth == Double.POSITIVE_INFINITY || maxWidth > Integer.MAX_VALUE / 4
                ? Integer.MAX_VALUE / 4
                : (int) Math.ceil(maxWidth);
        Dimension d = c.preferredSizeForWidth(w);
        return constraints.constrain(new Size(d.getWidth(), d.getHeight()));
    }

    // ------------------------------------------------------------------
    // Span flattening (pure — headless-testable)
    // ------------------------------------------------------------------

    /**
     * A flattened text run with its fully RESOLVED style.
     */
    public static class Run {
        public final String text;
        public final TextStyle style;

        public Run(String text, TextStyle style) {
            this.text = text;
            this.style = style;
        }
    }

    /**
     * Depth-first flattening of a span tree: a span's own text precedes its
     * children; each node's effective style is its own style with null
     * properties inherited from the parent chain. Null/empty texts
     * contribute no run (their children still do).
     */
    public static List<Run> flatten(TextSpan root) {
        List<Run> out = new ArrayList<Run>();
        collect(root, null, out);
        return out;
    }

    private static void collect(TextSpan span, TextStyle inherited, List<Run> out) {
        if (span == null) {
            return;
        }
        TextStyle eff = resolve(inherited, span.getStyle());
        if (span.getText() != null && span.getText().length() > 0) {
            out.add(new Run(span.getText(), eff));
        }
        if (span.getChildren() != null) {
            for (TextSpan c : span.getChildren()) {
                collect(c, eff, out);
            }
        }
    }

    /**
     * Style inheritance: the child's non-null properties win, everything
     * else comes from the parent. Identity is preserved when one side is
     * null (so downstream style handling can key resolved styles by identity).
     */
    public static TextStyle resolve(TextStyle parent, TextStyle child) {
        if (child == null) {
            return parent;
        }
        if (parent == null) {
            return child;
        }
        TextStyle out = new TextStyle();
        if (child.getFontSize() != null) {
            out.fontSize(child.getFontSize());
        } else if (parent.getFontSize() != null) {
            out.fontSize(parent.getFontSize());
        }
        if (child.getFontWeight() != null) {
            out.fontWeight(child.getFontWeight());
        } else if (parent.getFontWeight() != null) {
            out.fontWeight(parent.getFontWeight());
        }
        if (child.getColor() != null) {
            out.color(child.getColor());
        } else if (parent.getColor() != null) {
            out.color(parent.getColor());
        }
        if (child.getFontFamily() != null) {
            out.fontFamily(child.getFontFamily());
        } else if (parent.getFontFamily() != null) {
            out.fontFamily(parent.getFontFamily());
        }
        return out;
    }
}
