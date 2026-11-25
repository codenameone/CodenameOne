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
package com.codename1.art;

import com.codename1.art.effects.Effect;
import com.codename1.art.effects.Effects;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.ComboBox;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.Slider;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/// A simple vector and raster editor: an `ArtView` with a tool bar, a layers panel and an
/// inspector.
///
/// The tools select, move and resize; pan; draw rectangles, ellipses and lines; draw Bezier
/// paths with the pen and freehand strokes; paint and erase pixels; place text; pick a color;
/// and fill a shape or a region of pixels. Every change goes through the `ArtModel`, so it
/// can be undone. The layers panel and the inspector are ordinary components, available
/// separately through `getLayersPanel` and `getInspector`.
///
/// ```java
/// byte[] svg = "<svg xmlns='http://www.w3.org/2000/svg' width='200' height='120'><circle cx='60' cy='60' r='40' fill='#1e88e5'/></svg>".getBytes("UTF-8");
/// ArtEditor editor = new ArtEditor(new ArtModel(ArtFormats.read(svg)));
/// Form form = new Form("Draw", new BorderLayout());
/// form.add(BorderLayout.CENTER, editor);
/// form.show();
/// ```
public class ArtEditor extends Container implements ArtListener {
    /// Selects, moves and resizes nodes.
    public static final int TOOL_SELECT = 0;
    /// Drags the view.
    public static final int TOOL_PAN = 1;
    /// Draws rectangles.
    public static final int TOOL_RECTANGLE = 2;
    /// Draws ellipses.
    public static final int TOOL_ELLIPSE = 3;
    /// Draws straight lines.
    public static final int TOOL_LINE = 4;
    /// Draws Bezier paths: tap to add a corner, drag to add a smooth point, tap the first
    /// point to close.
    public static final int TOOL_PEN = 5;
    /// Draws smoothed freehand strokes.
    public static final int TOOL_FREEHAND = 6;
    /// Paints pixels onto the selected pixel layer, or a new one.
    public static final int TOOL_BRUSH = 7;
    /// Erases pixels from the selected pixel layer.
    public static final int TOOL_ERASER = 8;
    /// Places text.
    public static final int TOOL_TEXT = 9;
    /// Picks the color under the pointer as the fill color.
    public static final int TOOL_EYEDROPPER = 10;
    /// Fills a shape, or a region of similar pixels, with the fill color.
    public static final int TOOL_FILL = 11;

    private static final int[] PALETTE = {0xff000000, 0xffffffff, 0xff9e9e9e, 0xffe53935, 0xfffb8c00, 0xfffdd835,
        0xff43a047, 0xff00acc1, 0xff1e88e5, 0xff5e35b1, 0xffd81b60, 0xff6d4c41};

    private final ArtModel model;
    private final ArtView view;
    private final Container toolbar = new Container(BoxLayout.x());
    private final Container layers = new Container(BoxLayout.y());
    private final Container inspector = new Container(BoxLayout.y());
    private final Container side = new Container(BoxLayout.y());
    private final List<Button> toolButtons = new ArrayList<Button>();
    private int tool = -1;
    private int fillColor = 0xff1e88e5;
    private int strokeColor = 0xff000000;
    private float strokeWidth = 2;
    private float brushSize = 12;
    private boolean rebuilding;
    private boolean panelsPending;

    /// Creates an editor of an empty document.
    public ArtEditor() {
        this(new ArtModel());
    }

    /// Creates an editor.
    ///
    /// #### Parameters
    ///
    /// - `model`: the model to edit
    public ArtEditor(ArtModel model) {
        super(new BorderLayout());
        setUIID("ArtEditor");
        this.model = model;
        view = new ArtView(model);
        toolbar.setScrollableX(true);
        toolbar.setUIID("ArtEditorToolbar");
        side.setScrollableY(true);
        side.setUIID("ArtEditorPanel");
        side.add(new Label("Layers", "ArtEditorHeading"));
        side.add(layers);
        side.add(new Label("Properties", "ArtEditorHeading"));
        side.add(inspector);
        buildToolbar();
        add(BorderLayout.NORTH, toolbar);
        add(BorderLayout.CENTER, view);
        add(BorderLayout.EAST, side);
        side.setPreferredW(Math.min(Display.getInstance().getDisplayWidth() / 3,
                Display.getInstance().convertToPixels(55)));
        setTool(TOOL_SELECT);
        rebuildPanels();
    }

    // The editor listens only while it is on screen, so a model that outlives its forms does
    // not keep every editor it was ever shown in.
    @Override
    protected void initComponent() {
        super.initComponent();
        model.addArtListener(this);
        rebuildPanels();
    }

    @Override
    protected void deinitialize() {
        model.removeArtListener(this);
        super.deinitialize();
    }

    /// The model.
    public ArtModel getModel() {
        return model;
    }

    /// The view.
    public ArtView getView() {
        return view;
    }

    /// The layers panel: the node tree, with visibility, order and selection.
    public Container getLayersPanel() {
        return layers;
    }

    /// The inspector: the selected node's properties and effects.
    public Container getInspector() {
        return inspector;
    }

    /// Shows or hides the side panel holding the layers and the inspector.
    ///
    /// #### Parameters
    ///
    /// - `show`: true to show it
    public void setPanelsVisible(boolean show) {
        side.setHidden(!show);
        revalidate();
    }

    /// The current tool, one of the `TOOL_` constants.
    public int getTool() {
        return tool;
    }

    /// Picks a tool.
    ///
    /// #### Parameters
    ///
    /// - `t`: one of the `TOOL_` constants
    public void setTool(int t) {
        if (t == tool) {
            return;
        }
        finishPen();
        tool = t;
        view.setPointerHandler(t == TOOL_PAN ? null : new ToolHandler());
        for (int i = 0; i < toolButtons.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            toolButtons.get(i).setUIID(i == t ? "ArtEditorToolSelected" : "ArtEditorTool");
        }
        toolbar.repaint();
    }

    /// The color new shapes, text and the brush use.
    public int getFillColor() {
        return fillColor;
    }

    /// Sets the fill color.
    ///
    /// #### Parameters
    ///
    /// - `argb`: the color
    public void setFillColor(int argb) {
        fillColor = argb;
    }

    /// The outline color for lines, paths and freehand strokes.
    public int getStrokeColor() {
        return strokeColor;
    }

    /// Sets the outline color.
    ///
    /// #### Parameters
    ///
    /// - `argb`: the color
    public void setStrokeColor(int argb) {
        strokeColor = argb;
    }

    /// Sets the outline width in document units.
    ///
    /// #### Parameters
    ///
    /// - `w`: the width
    public void setStrokeWidth(float w) {
        strokeWidth = Math.max(0.1f, w);
    }

    /// Sets the brush and eraser diameter in document units.
    ///
    /// #### Parameters
    ///
    /// - `size`: the diameter
    public void setBrushSize(float size) {
        brushSize = Math.max(1, size);
    }

    /// Writes the document as SVG.
    ///
    /// #### Parameters
    ///
    /// - `out`: the destination; not closed
    public void exportSvg(OutputStream out) throws IOException {
        SvgWriter.write(model.getDocument(), out);
    }

    /// Writes the document as a layered PSD.
    ///
    /// #### Parameters
    ///
    /// - `out`: the destination; not closed
    public void exportPsd(OutputStream out) throws IOException {
        PsdWriter.write(model.getDocument(), out);
    }

    /// Writes the document as a PNG.
    ///
    /// #### Parameters
    ///
    /// - `out`: the destination; not closed
    /// - `scale`: pixels per document unit
    public void exportPng(OutputStream out, double scale) throws IOException {
        out.write(ArtImages.encodePng(ArtRenderer.render(model.getDocument(), scale)));
    }

    /// Writes the document losslessly as `.cn1gfx`.
    ///
    /// #### Parameters
    ///
    /// - `out`: the destination; not closed
    public void exportNative(OutputStream out) throws IOException {
        NativeFormat.write(model.getDocument(), out);
    }

    // ---- toolbar ----

    private void buildToolbar() {
        char[] icons = {FontImage.MATERIAL_NEAR_ME, FontImage.MATERIAL_PAN_TOOL, FontImage.MATERIAL_CROP_SQUARE,
            FontImage.MATERIAL_RADIO_BUTTON_UNCHECKED, FontImage.MATERIAL_REMOVE, FontImage.MATERIAL_TIMELINE,
            FontImage.MATERIAL_GESTURE, FontImage.MATERIAL_BRUSH, FontImage.MATERIAL_AUTO_FIX_NORMAL,
            FontImage.MATERIAL_TEXT_FIELDS, FontImage.MATERIAL_COLORIZE, FontImage.MATERIAL_FORMAT_COLOR_FILL};
        String[] names = {"Select", "Pan", "Rectangle", "Ellipse", "Line", "Pen", "Freehand", "Brush", "Eraser",
            "Text", "Eyedropper", "Fill"};
        for (int i = 0; i < icons.length; i++) {
            final int t = i;
            Button b = new Button("", "ArtEditorTool");
            FontImage.setMaterialIcon(b, icons[i]);
            b.setAccessibilityText(names[i]);
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    setTool(t);
                }
            });
            toolButtons.add(b);
            toolbar.add(b);
        }
        Button undo = new Button("", "ArtEditorTool");
        FontImage.setMaterialIcon(undo, FontImage.MATERIAL_UNDO);
        undo.setAccessibilityText("Undo");
        undo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                finishPen();
                model.undo();
            }
        });
        Button redo = new Button("", "ArtEditorTool");
        FontImage.setMaterialIcon(redo, FontImage.MATERIAL_REDO);
        redo.setAccessibilityText("Redo");
        redo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                model.redo();
            }
        });
        Button fit = new Button("", "ArtEditorTool");
        FontImage.setMaterialIcon(fit, FontImage.MATERIAL_ZOOM_OUT_MAP);
        fit.setAccessibilityText("Fit");
        fit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                view.fit();
            }
        });
        Button panel = new Button("", "ArtEditorTool");
        FontImage.setMaterialIcon(panel, FontImage.MATERIAL_LAYERS);
        panel.setAccessibilityText("Layers");
        panel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                setPanelsVisible(side.isHidden());
            }
        });
        toolbar.add(undo);
        toolbar.add(redo);
        toolbar.add(fit);
        toolbar.add(panel);
        for (int i = 0; i < PALETTE.length; i++) { // NOPMD ForLoopCanBeForeach - indexed: no iterator allocation
            toolbar.add(swatch(PALETTE[i]));
        }
    }

    private Button swatch(final int color) {
        Button b = new Button(" ", "ArtEditorSwatch");
        b.getAllStyles().setBgColor(color & 0xffffff);
        b.getAllStyles().setBgTransparency(255);
        b.setAccessibilityText(Colors.toHex(color));
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                applyColor(color);
            }
        });
        return b;
    }

    /// Makes a color the fill color and, when a shape or text is selected, gives it that fill.
    private void applyColor(int color) {
        fillColor = color;
        ArtNode sel = model.getSelected();
        if (sel instanceof ShapeNode) {
            ShapeNode s = (ShapeNode) sel;
            if (s.getFills().isEmpty() && !s.getStrokes().isEmpty()) {
                StrokeStyle st = s.getStrokes().get(0).copy();
                st.setPaint(new SolidPaint(color));
                model.setStroke(s, st);
            } else {
                model.setFill(s, new SolidPaint(color));
            }
        } else if (sel instanceof TextNode) {
            final TextNode t = (TextNode) sel;
            model.edit(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < t.getRuns().size(); i++) {
                        t.getRuns().get(i).setFill(new SolidPaint(fillColor));
                    }
                }
            });
        }
    }

    // ---- panels ----

    @Override
    public void artChanged(ArtModel m, boolean contentChanged) {
        // A drag reports a change per pointer event; rebuild once per EDT cycle.
        if (panelsPending) {
            return;
        }
        panelsPending = true;
        Display.getInstance().callSerially(new Runnable() {
            @Override
            public void run() {
                panelsPending = false;
                rebuildPanels();
            }
        });
    }

    private void rebuildPanels() {
        if (rebuilding) {
            return;
        }
        rebuilding = true;
        try {
            layers.removeAll();
            addLayerRows(model.getDocument().getRoot(), 0);
            inspector.removeAll();
            buildInspector();
            side.revalidateLater();
        } finally {
            rebuilding = false;
        }
    }

    private void addLayerRows(GroupNode g, int depth) {
        List<ArtNode> ch = g.getChildren();
        for (int i = ch.size() - 1; i >= 0; i--) {
            final ArtNode n = ch.get(i);
            layers.add(layerRow(n, depth));
            if (n instanceof GroupNode && depth < 12) {
                addLayerRows((GroupNode) n, depth + 1);
            }
        }
    }

    private Component layerRow(final ArtNode n, int depth) {
        Container row = new Container(new BorderLayout());
        row.setUIID(model.isSelected(n) ? "ArtEditorLayerSelected" : "ArtEditorLayer");
        row.getAllStyles().setPaddingLeft(depth * 3);
        Button eye = new Button("", "ArtEditorLayerButton");
        FontImage.setMaterialIcon(eye, n.isVisible() ? FontImage.MATERIAL_VISIBILITY : FontImage.MATERIAL_VISIBILITY_OFF);
        eye.setAccessibilityText(n.isVisible() ? "Hide" : "Show");
        eye.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                model.setVisible(n, !n.isVisible());
            }
        });
        Button name = new Button(label(n), "ArtEditorLayerName");
        name.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                model.select(n, false);
            }
        });
        Container moves = new Container(BoxLayout.x());
        Button up = new Button("", "ArtEditorLayerButton");
        FontImage.setMaterialIcon(up, FontImage.MATERIAL_ARROW_UPWARD);
        up.setAccessibilityText("Bring forward");
        up.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                model.reorder(n, true);
            }
        });
        Button down = new Button("", "ArtEditorLayerButton");
        FontImage.setMaterialIcon(down, FontImage.MATERIAL_ARROW_DOWNWARD);
        down.setAccessibilityText("Send backward");
        down.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                model.reorder(n, false);
            }
        });
        moves.add(up).add(down);
        row.add(BorderLayout.WEST, eye).add(BorderLayout.CENTER, name).add(BorderLayout.EAST, moves);
        return row;
    }

    private static String label(ArtNode n) {
        if (n.getName() != null && n.getName().length() > 0) {
            return n.getName();
        }
        if (n instanceof GroupNode) {
            return "Group";
        }
        if (n instanceof TextNode) {
            String t = ((TextNode) n).getText();
            return t.length() > 24 ? t.substring(0, 24) : t;
        }
        if (n instanceof RasterNode) {
            return "Pixels";
        }
        if (n instanceof AdjustmentNode) {
            return ((AdjustmentNode) n).getAdjustment().getKind();
        }
        return "Shape";
    }

    private void buildInspector() {
        final ArtNode n = model.getSelected();
        if (n == null) {
            inspector.add(new Label("Nothing selected", "ArtEditorHint"));
            final Slider brush = slider(1, 200, Math.round(brushSize));
            brush.addDataChangedListener(new DataChangedListener() {
                @Override
                public void dataChanged(int type, int index) {
                    brushSize = Math.max(1, brush.getProgress());
                }
            });
            inspector.add(new Label("Brush size", "ArtEditorField")).add(brush);
            return;
        }
        final TextField name = new TextField(n.getName() == null ? "" : n.getName(), "Name");
        name.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                model.setName(n, name.getText());
            }
        });
        inspector.add(name);
        final Slider op = slider(0, 100, Math.round(n.getOpacity() * 100));
        final boolean[] saved = new boolean[1];
        op.addDataChangedListener(new DataChangedListener() {
            @Override
            public void dataChanged(int type, int index) {
                if (rebuilding) {
                    return;
                }
                // The slider changes the node live; one undo step is saved before the first change.
                if (!saved[0]) {
                    saved[0] = true;
                    model.checkpoint();
                }
                n.setOpacity(op.getProgress() / 100f);
                view.invalidateCache();
            }
        });
        inspector.add(new Label("Opacity", "ArtEditorField")).add(op);
        BlendMode[] modes = BlendMode.values();
        String[] modeNames = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            modeNames[i] = modes[i].name();
        }
        final ComboBox<Object> blend = new ComboBox<Object>((Object[]) modeNames);
        blend.setSelectedIndex(n.getBlendMode().ordinal());
        blend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                int i = blend.getSelectedIndex();
                if (i >= 0 && i < BlendMode.values().length && BlendMode.values()[i] != n.getBlendMode()) {
                    model.setBlendMode(n, BlendMode.values()[i]);
                }
            }
        });
        inspector.add(new Label("Blend mode", "ArtEditorField")).add(blend);
        final CheckBox locked = new CheckBox("Locked");
        locked.setSelected(n.isLocked());
        locked.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                model.setLocked(n, locked.isSelected());
            }
        });
        inspector.add(locked);
        if (n instanceof TextNode) {
            final TextField text = new TextField(((TextNode) n).getText(), "Text");
            text.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    model.setText((TextNode) n, text.getText());
                }
            });
            inspector.add(new Label("Text", "ArtEditorField")).add(text);
        }
        if (n instanceof ShapeNode) {
            final ShapeNode s = (ShapeNode) n;
            float w = s.getStrokes().isEmpty() ? 0 : s.getStrokes().get(0).getWidth();
            final Slider sw = slider(0, 50, Math.round(w));
            final boolean[] swSaved = new boolean[1];
            sw.addDataChangedListener(new DataChangedListener() {
                @Override
                public void dataChanged(int type, int index) {
                    if (rebuilding) {
                        return;
                    }
                    if (!swSaved[0]) {
                        swSaved[0] = true;
                        model.checkpoint();
                    }
                    int v = sw.getProgress();
                    if (v <= 0) {
                        s.getStrokes().clear();
                    } else if (s.getStrokes().isEmpty()) {
                        s.getStrokes().add(new StrokeStyle(new SolidPaint(strokeColor), v));
                    } else {
                        s.getStrokes().get(0).setWidth(v);
                    }
                    view.invalidateCache();
                }
            });
            inspector.add(new Label("Outline width", "ArtEditorField")).add(sw);
        }
        if (n instanceof GroupNode && !((GroupNode) n).getChildren().isEmpty()) {
            Button ungroup = new Button("Ungroup", "ArtEditorAction");
            ungroup.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    model.ungroup((GroupNode) n);
                }
            });
            inspector.add(ungroup);
        }
        if (model.getSelection().size() > 1) {
            Button group = new Button("Group", "ArtEditorAction");
            group.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    model.group(model.getSelection());
                }
            });
            inspector.add(group);
        }
        Button delete = new Button("Delete", "ArtEditorAction");
        FontImage.setMaterialIcon(delete, FontImage.MATERIAL_DELETE);
        delete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                model.removeSelection();
            }
        });
        inspector.add(delete);
        effectsSection(n);
    }

    private void effectsSection(final ArtNode n) {
        inspector.add(new Label("Effects", "ArtEditorHeading"));
        for (int i = 0; i < n.getEffects().size(); i++) {
            final int index = i;
            Effect e = n.getEffects().get(i);
            Container row = new Container(new BorderLayout());
            final CheckBox on = new CheckBox(e.getKind());
            on.setSelected(e.isEnabled());
            on.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    final boolean v = on.isSelected();
                    model.edit(new Runnable() {
                        @Override
                        public void run() {
                            n.getEffects().get(index).setEnabled(v);
                        }
                    });
                }
            });
            Button remove = new Button("", "ArtEditorLayerButton");
            FontImage.setMaterialIcon(remove, FontImage.MATERIAL_CLEAR);
            remove.setAccessibilityText("Remove effect");
            remove.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    model.removeEffect(n, index);
                }
            });
            row.add(BorderLayout.CENTER, on).add(BorderLayout.EAST, remove);
            inspector.add(row);
        }
        String[] kinds = {"Add effect...", "Drop shadow", "Inner shadow", "Outer glow", "Inner glow", "Blur",
            "Outline", "Color overlay", "Bevel"};
        final ComboBox<Object> add = new ComboBox<Object>((Object[]) kinds);
        add.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                Effect e = newEffect(add.getSelectedIndex());
                if (e != null) {
                    model.addEffect(n, e);
                }
            }
        });
        inspector.add(add);
    }

    private Effect newEffect(int i) {
        switch (i) {
            case 1:
                return Effects.dropShadow(0x80000000, 4, 4, 8, 0);
            case 2:
                return Effects.innerShadow(0x80000000, 3, 3, 6, 0);
            case 3:
                return Effects.outerGlow(0xc0ffeb3b, 10, 0);
            case 4:
                return Effects.innerGlow(0xc0ffeb3b, 8, false);
            case 5:
                return Effects.gaussianBlur(6);
            case 6:
                return Effects.stroke(strokeColor, 3, "outside");
            case 7:
                return Effects.colorOverlay(fillColor);
            case 8:
                return Effects.bevel("inner", 6, 1, 120, 30);
            default:
                return null;
        }
    }

    private static Slider slider(int min, int max, int value) {
        Slider s = new Slider();
        s.setEditable(true);
        s.setMinValue(min);
        s.setMaxValue(max);
        s.setProgress(Math.max(min, Math.min(max, value)));
        return s;
    }

    // ---- tools ----

    private GeneralPath freehandPath(List<double[]> pts) {
        GeneralPath p = new GeneralPath();
        if (pts.isEmpty()) {
            return p;
        }
        double[] a = pts.get(0);
        p.moveTo(a[0], a[1]);
        if (pts.size() == 1) {
            p.lineTo(a[0] + 0.01, a[1]);
            return p;
        }
        // Quadratic curves through the midpoints smooth the jitter of a finger.
        for (int i = 1; i < pts.size() - 1; i++) {
            double[] c = pts.get(i);
            double[] nx = pts.get(i + 1);
            p.quadTo(c[0], c[1], (c[0] + nx[0]) / 2, (c[1] + nx[1]) / 2);
        }
        double[] last = pts.get(pts.size() - 1);
        p.lineTo(last[0], last[1]);
        return p;
    }

    private final List<double[]> penPoints = new ArrayList<double[]>();
    private ShapeNode penShape;

    private void finishPen() {
        livePen();
        if (penShape != null && penPoints.size() < 2) {
            // A single click is not a path.
            model.remove(penShape);
        }
        penShape = null;
        penPoints.clear();
    }

    /// The path being drawn as the current tree has it. Each pen point is its own undo step,
    /// with no group held open between gestures, so an undo between two clicks installs a copy
    /// of the path; it is picked up again by id, or dropped when the undo removed it.
    private void livePen() {
        if (penShape == null) {
            return;
        }
        ArtNode n = model.findById(penShape.getId());
        if (n instanceof ShapeNode) {
            penShape = (ShapeNode) n;
        } else {
            penShape = null;
            penPoints.clear();
        }
    }

    private GeneralPath penPath(boolean close) {
        GeneralPath p = new GeneralPath();
        for (int i = 0; i < penPoints.size(); i++) {
            double[] k = penPoints.get(i);
            if (i == 0) {
                p.moveTo(k[0], k[1]);
            } else {
                double[] prev = penPoints.get(i - 1);
                p.curveTo(prev[4], prev[5], k[2], k[3], k[0], k[1]);
            }
        }
        if (close && penPoints.size() > 1) {
            double[] last = penPoints.get(penPoints.size() - 1);
            double[] first = penPoints.get(0);
            p.curveTo(last[4], last[5], first[2], first[3], first[0], first[1]);
            p.closePath();
        }
        return p;
    }

    /// Dispatches pointer events to the current tool.
    private final class ToolHandler implements ArtView.PointerHandler {
        private double sx;
        private double sy;
        private double lx;
        private double ly;
        private ShapeNode drawing;
        private final List<double[]> points = new ArrayList<double[]>();
        private RasterNode canvas;
        private int resizeCorner = -1;
        private Matrix resizeStart;
        private double[] resizeBounds;
        private boolean moving;

        @Override
        public void pressed(ArtView v, double x, double y) {
            sx = x;
            sy = y;
            lx = x;
            ly = y;
            switch (tool) {
                case TOOL_SELECT:
                    pressSelect(v, x, y);
                    break;
                case TOOL_RECTANGLE:
                case TOOL_ELLIPSE:
                case TOOL_LINE:
                    model.beginGroup();
                    drawing = new ShapeNode(new GeneralPath());
                    if (tool == TOOL_LINE) {
                        drawing.stroke(new StrokeStyle(new SolidPaint(strokeColor), strokeWidth));
                        drawing.setName("Line");
                    } else {
                        drawing.fill(new SolidPaint(fillColor));
                        drawing.setName(tool == TOOL_RECTANGLE ? "Rectangle" : "Ellipse");
                    }
                    model.add(drawing);
                    break;
                case TOOL_FREEHAND:
                    model.beginGroup();
                    points.clear();
                    points.add(new double[]{x, y});
                    drawing = new ShapeNode(freehandPath(points));
                    StrokeStyle st = new StrokeStyle(new SolidPaint(strokeColor), strokeWidth);
                    st.setCap(StrokeStyle.CAP_ROUND);
                    st.setJoin(StrokeStyle.JOIN_ROUND);
                    drawing.stroke(st);
                    drawing.setName("Freehand");
                    model.add(drawing);
                    break;
                case TOOL_PEN:
                    pressPen(x, y);
                    break;
                case TOOL_BRUSH:
                case TOOL_ERASER:
                    pressPaint(x, y);
                    break;
                case TOOL_TEXT:
                    TextNode t = new TextNode("Text", 24, fillColor);
                    t.setPosition(x, y);
                    model.add(t);
                    break;
                case TOOL_EYEDROPPER:
                    pick(x, y);
                    break;
                case TOOL_FILL:
                    fill(v, x, y);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void dragged(ArtView v, double x, double y) {
            switch (tool) {
                case TOOL_SELECT:
                    dragSelect(x, y);
                    break;
                case TOOL_RECTANGLE:
                case TOOL_ELLIPSE:
                case TOOL_LINE:
                    if (drawing != null) {
                        drawing.setPath(shapePath(x, y));
                        model.changed();
                    }
                    break;
                case TOOL_FREEHAND:
                    if (drawing != null) {
                        points.add(new double[]{x, y});
                        drawing.setPath(freehandPath(points));
                        model.changed();
                    }
                    break;
                case TOOL_PEN:
                    if (!penPoints.isEmpty() && penShape != null) {
                        double[] k = penPoints.get(penPoints.size() - 1);
                        k[4] = x;
                        k[5] = y;
                        k[2] = 2 * k[0] - x;
                        k[3] = 2 * k[1] - y;
                        penShape.setPath(penPath(false));
                        model.changed();
                    }
                    break;
                case TOOL_BRUSH:
                case TOOL_ERASER:
                    paintTo(x, y);
                    break;
                default:
                    break;
            }
            lx = x;
            ly = y;
        }

        @Override
        public void released(ArtView v, double x, double y) {
            switch (tool) {
                case TOOL_SELECT:
                    if (moving || resizeCorner >= 0) {
                        model.endGroup();
                    }
                    moving = false;
                    resizeCorner = -1;
                    break;
                case TOOL_RECTANGLE:
                case TOOL_ELLIPSE:
                case TOOL_LINE:
                case TOOL_FREEHAND:
                    model.endGroup();
                    if (drawing != null && Math.abs(x - sx) < 1 && Math.abs(y - sy) < 1 && tool != TOOL_FREEHAND) {
                        // A tap is not a shape.
                        model.undo();
                    }
                    drawing = null;
                    break;
                case TOOL_BRUSH:
                case TOOL_ERASER:
                    if (canvas != null) {
                        model.endGroup();
                    }
                    canvas = null;
                    break;
                default:
                    break;
            }
        }

        private GeneralPath shapePath(double x, double y) {
            double x0 = Math.min(sx, x);
            double y0 = Math.min(sy, y);
            double w = Math.abs(x - sx);
            double h = Math.abs(y - sy);
            if (tool == TOOL_ELLIPSE) {
                return Shapes.ellipse(x0 + w / 2, y0 + h / 2, w / 2, h / 2);
            }
            if (tool == TOOL_LINE) {
                GeneralPath p = new GeneralPath();
                p.moveTo(sx, sy);
                p.lineTo(x, y);
                return p;
            }
            return Shapes.roundRect(x0, y0, w, h, 0, 0);
        }

        private void pressSelect(ArtView v, double x, double y) {
            ArtNode sel = model.getSelected();
            if (sel != null) {
                double[] b = v.viewBounds(sel);
                if (b != null) {
                    double px = x * v.getZoom() + v.getViewTransform().getE();
                    double py = y * v.getZoom() + v.getViewTransform().getF();
                    double r = Display.getInstance().convertToPixels(2.5f);
                    double[] cx = {b[0], b[0] + b[2], b[0] + b[2], b[0]};
                    double[] cy = {b[1], b[1], b[1] + b[3], b[1] + b[3]};
                    for (int i = 0; i < 4; i++) {
                        if (Math.abs(px - cx[i]) <= r && Math.abs(py - cy[i]) <= r) {
                            resizeCorner = i;
                            resizeStart = sel.getTransform();
                            Matrix toDoc = model.toDocument(sel);
                            double[] lb = sel.getLocalBounds();
                            // The document-space box the drag scales, anchored at the opposite corner.
                            double ax = toDoc.mapX(i == 0 || i == 3 ? lb[0] + lb[2] : lb[0], i < 2 ? lb[1] + lb[3] : lb[1]);
                            double ay = toDoc.mapY(i == 0 || i == 3 ? lb[0] + lb[2] : lb[0], i < 2 ? lb[1] + lb[3] : lb[1]);
                            resizeBounds = new double[]{ax, ay, x, y};
                            model.beginGroup();
                            model.checkpoint();
                            return;
                        }
                    }
                }
            }
            ArtNode hit = v.nodeAt(x, y);
            if (hit == null) {
                model.clearSelection();
                return;
            }
            if (!model.isSelected(hit)) {
                model.select(hit, false);
            }
            moving = true;
            model.beginGroup();
        }

        private void dragSelect(double x, double y) {
            if (resizeCorner >= 0) {
                ArtNode sel = model.getSelected();
                if (sel == null) {
                    return;
                }
                double ax = resizeBounds[0];
                double ay = resizeBounds[1];
                double w0 = resizeBounds[2] - ax;
                double h0 = resizeBounds[3] - ay;
                double fx = Math.abs(w0) < 1e-6 ? 1 : (x - ax) / w0;
                double fy = Math.abs(h0) < 1e-6 ? 1 : (y - ay) / h0;
                Matrix scaleDoc = Matrix.translate(ax, ay).multiply(Matrix.scale(fx, fy)).multiply(Matrix.translate(-ax, -ay));
                GroupNode p = model.parentOf(sel);
                Matrix parentToDoc = p == null ? Matrix.IDENTITY : model.toDocument(p);
                Matrix inv = parentToDoc.invert();
                if (inv != null) {
                    sel.setTransform(inv.multiply(scaleDoc).multiply(parentToDoc).multiply(resizeStart));
                    model.changed();
                }
                return;
            }
            if (moving) {
                model.translateSelection(x - lx, y - ly);
            }
        }

        private void pressPen(double x, double y) {
            double slop = Display.getInstance().convertToPixels(2) / view.getZoom();
            livePen();
            if (penShape != null && penPoints.size() > 1) {
                double[] first = penPoints.get(0);
                if (Math.abs(first[0] - x) <= slop && Math.abs(first[1] - y) <= slop) {
                    model.checkpoint();
                    penShape.setPath(penPath(true));
                    penShape.fill(new SolidPaint(fillColor));
                    model.changed();
                    penShape = null;
                    penPoints.clear();
                    return;
                }
            }
            if (penShape == null) {
                penShape = new ShapeNode(new GeneralPath());
                penShape.stroke(new StrokeStyle(new SolidPaint(strokeColor), strokeWidth));
                penShape.setName("Path");
                model.add(penShape);
            } else {
                model.checkpoint();
            }
            penPoints.add(new double[]{x, y, x, y, x, y});
            penShape.setPath(penPath(false));
            model.changed();
        }

        private void pressPaint(double x, double y) {
            ArtNode sel = model.getSelected();
            model.beginGroup();
            model.checkpoint();
            if (sel instanceof RasterNode) {
                canvas = (RasterNode) sel;
                canvas.setImage(canvas.getImage().copy());
            } else if (tool == TOOL_BRUSH) {
                ArtDocument d = model.getDocument();
                canvas = new RasterNode(new Raster(0, 0, (int) Math.ceil(d.getWidth()), (int) Math.ceil(d.getHeight())));
                canvas.setName("Paint");
                model.add(canvas);
            } else {
                model.endGroup();
                canvas = null;
                return;
            }
            dab(x, y);
            model.changed();
        }

        private void paintTo(double x, double y) {
            if (canvas == null) {
                return;
            }
            double dx = x - lx;
            double dy = y - ly;
            double dist = Math.sqrt(dx * dx + dy * dy);
            double step = Math.max(0.5, brushSize / 4);
            int n = Math.max(1, (int) Math.ceil(dist / step));
            for (int i = 1; i <= n; i++) {
                dab(lx + dx * i / n, ly + dy * i / n);
            }
            model.changed();
        }

        /// One round, soft-edged dab of the brush, in the canvas's pixels.
        private void dab(double x, double y) {
            Matrix inv = model.toDocument(canvas).invert();
            if (inv == null) {
                return;
            }
            Raster r = canvas.getImage();
            double cx = inv.mapX(x, y) - r.getX();
            double cy = inv.mapY(x, y) - r.getY();
            double rad = brushSize / 2 * inv.meanScale();
            int[] px = r.getPixels();
            int w = r.getWidth();
            int h = r.getHeight();
            int x0 = Math.max(0, (int) Math.floor(cx - rad));
            int x1 = Math.min(w - 1, (int) Math.ceil(cx + rad));
            int y0 = Math.max(0, (int) Math.floor(cy - rad));
            int y1 = Math.min(h - 1, (int) Math.ceil(cy + rad));
            double[] scratch = new double[Compositor.SCRATCH];
            for (int yy = y0; yy <= y1; yy++) {
                for (int xx = x0; xx <= x1; xx++) {
                    double ddx = xx + 0.5 - cx;
                    double ddy = yy + 0.5 - cy;
                    double d = Math.sqrt(ddx * ddx + ddy * ddy);
                    if (d > rad) {
                        continue;
                    }
                    // Full strength inside 70% of the radius, fading to nothing at the edge.
                    double a = d < rad * 0.7 ? 1 : (rad - d) / (rad * 0.3);
                    int i = yy * w + xx;
                    if (tool == TOOL_ERASER) {
                        int al = (int) Math.round((px[i] >>> 24) * (1 - a));
                        px[i] = (al << 24) | (px[i] & 0xffffff);
                    } else {
                        px[i] = Compositor.blendPixel(fillColor, px[i], BlendMode.NORMAL, (float) a, scratch, i);
                    }
                }
            }
        }

        private void pick(double x, double y) {
            Raster r = new ArtRenderer().render(model.getDocument(), Matrix.translate(-x, -y), new int[]{0, 0, 1, 1});
            int c = r.getPixels()[0];
            if ((c >>> 24) != 0) {
                fillColor = 0xff000000 | (c & 0xffffff);
            }
        }

        private void fill(ArtView v, double x, double y) {
            ArtNode hit = v.nodeAt(x, y);
            if (hit instanceof ShapeNode) {
                model.setFill((ShapeNode) hit, new SolidPaint(fillColor));
                return;
            }
            if (hit instanceof RasterNode) {
                RasterNode rn = (RasterNode) hit;
                Matrix inv = model.toDocument(rn).invert();
                if (inv == null) {
                    return;
                }
                model.checkpoint();
                rn.setImage(rn.getImage().copy());
                Raster r = rn.getImage();
                floodFill(r, (int) Math.floor(inv.mapX(x, y)) - r.getX(), (int) Math.floor(inv.mapY(x, y)) - r.getY(),
                        fillColor);
                model.changed();
            }
        }
    }

    /// Fills the 4-connected region of pixels within 32 of the seed's color on every channel.
    static void floodFill(Raster r, int sx, int sy, int color) {
        int w = r.getWidth();
        int h = r.getHeight();
        if (sx < 0 || sy < 0 || sx >= w || sy >= h) {
            return;
        }
        int[] px = r.getPixels();
        int seed = px[sy * w + sx];
        if (seed == color) {
            return;
        }
        boolean[] seen = new boolean[w * h];
        int[] stack = new int[Math.min(w * h, 1 << 20)];
        int top = 0;
        stack[top++] = sy * w + sx;
        seen[sy * w + sx] = true;
        while (top > 0) {
            int i = stack[--top];
            px[i] = color;
            int x = i % w;
            int y = i / w;
            int[] next = {x > 0 ? i - 1 : -1, x < w - 1 ? i + 1 : -1, y > 0 ? i - w : -1, y < h - 1 ? i + w : -1};
            for (int k = 0; k < 4; k++) {
                int j = next[k];
                if (j >= 0 && !seen[j] && close(px[j], seed)) {
                    seen[j] = true;
                    if (top == stack.length) {
                        int[] grown = new int[Math.min(w * h, stack.length * 2)];
                        System.arraycopy(stack, 0, grown, 0, top);
                        stack = grown;
                    }
                    stack[top++] = j;
                }
            }
        }
    }

    private static boolean close(int a, int b) {
        for (int s = 0; s < 32; s += 8) {
            if (Math.abs(((a >>> s) & 0xff) - ((b >>> s) & 0xff)) > 32) {
                return false;
            }
        }
        return true;
    }
}
