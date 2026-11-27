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

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Graphics;
import com.codename1.ui.Stroke;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.GeneralPath;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.util.EventDispatcher;
import com.codename1.util.MathUtil;
import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Document;
import com.codename1.wordprocessing.DocumentListener;
import com.codename1.wordprocessing.DocumentModel;
import com.codename1.wordprocessing.DocumentView;
import com.codename1.wordprocessing.Paragraph;
import com.codename1.wordprocessing.Picture;
import com.codename1.wordprocessing.RichText;
import com.codename1.wordprocessing.TextStyle;
import java.util.ArrayList;
import java.util.List;

/// An editor for a presentation: a strip of slide thumbnails, the slide being edited and its
/// speaker notes.
///
/// Tap an element to select it, drag it to move, drag a corner or edge handle to resize and
/// the round handle above it to rotate (snapping to 15 degrees); every drag is one undo step.
/// Double tap a shape to edit its text in place -- the editor is a
/// `com.codename1.wordprocessing.DocumentView`, so typing, selection, the clipboard and the
/// keyboard behave exactly as they do in a document. Every edit goes through the
/// `PresentationModel`, so `getModel().undo()` undoes it.
///
/// ```java
/// void edit(Form form, byte[] bytes) throws IOException {
///     PresentationModel model = new PresentationModel(Presentation.read(bytes));
///     SlideView editor = new SlideView(model);
///     form.add(BorderLayout.CENTER, editor);
///     editor.addShape("roundRect");
/// }
/// ```
public class SlideView extends Container {
    private static final int DESK = 0xe4e7eb;
    private static final int ACCENT = 0x1f6feb;
    private static final float HANDLE_MM = 1.6f;

    private final PresentationModel model;
    private final SlideRenderer renderer = new SlideRenderer();
    private final EventDispatcher selectionListeners = new EventDispatcher();
    private final ThumbStrip thumbs = new ThumbStrip();
    private final Canvas canvas = new Canvas();
    private final DocumentView notes;
    private final PresentationListener modelListener = new PresentationListener() {
        @Override
        public void presentationChanged(PresentationModel m, Presentation before,
                Presentation after) {
            if (slide >= after.slides.size()) {
                slide = Math.max(0, after.slides.size() - 1);
            }
            if (selection != null && (after.slides.isEmpty()
                    || after.slides.get(slide).find(selection) == null)) {
                selection = null;
            }
            if (!syncingNotes) {
                loadNotes();
            }
            thumbs.setShouldCalcPreferredSize(true);
            revalidateLater();
            repaint();
        }
    };
    private int slide;
    private String selection;
    private boolean syncingNotes;
    private TextEdit textEdit;

    /// An editor of a model.
    ///
    /// #### Parameters
    ///
    /// - `model`: the model every edit goes through
    public SlideView(PresentationModel model) {
        super(new BorderLayout());
        if (model == null) {
            throw new IllegalArgumentException("model");
        }
        this.model = model;
        setUIIDFinal("SlideView");
        notes = new DocumentView(new DocumentModel(new Document()));
        notes.setFitWidth(true);
        notes.setPreferredH(Display.getInstance().convertToPixels(22));
        notes.getModel().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(DocumentModel m, Document before, Document after) {
                if (syncingNotes || getSlideCount() == 0) {
                    return;
                }
                syncingNotes = true;
                try {
                    SlideView.this.model.setNotes(slide, after.blocks);
                } finally {
                    syncingNotes = false;
                }
            }
        });
        add(BorderLayout.WEST, thumbs);
        add(BorderLayout.CENTER, canvas);
        add(BorderLayout.SOUTH, notes);
        loadNotes();
    }

    private int getSlideCount() {
        return model.getSlideCount();
    }

    private void loadNotes() {
        syncingNotes = true;
        try {
            List<Block> n = getSlideCount() == 0 ? new ArrayList<Block>()
                    : model.getSlide(slide).notes;
            notes.getModel().reset(new Document(n.isEmpty() ? placeholderNotes() : n, null, null,
                    1));
        } finally {
            syncingNotes = false;
        }
    }

    private static List<Block> placeholderNotes() {
        List<Block> l = new ArrayList<Block>();
        l.add(new Paragraph("notes", RichText.of("")));
        return l;
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        model.addPresentationListener(modelListener);
    }

    @Override
    protected void deinitialize() {
        // a drag or a text edit cut short by leaving the form must not leave an undo group
        // open, or every later edit would join it
        endTextEdit();
        canvas.endDrag();
        model.removePresentationListener(modelListener);
        super.deinitialize();
    }

    /// The model.
    ///
    /// #### Returns
    ///
    /// the model
    public PresentationModel getModel() {
        return model;
    }

    /// The renderer the slides are painted with.
    ///
    /// #### Returns
    ///
    /// the renderer
    public SlideRenderer getRenderer() {
        return renderer;
    }

    /// The notes editor, to hide it or style it.
    ///
    /// #### Returns
    ///
    /// the notes view
    public DocumentView getNotesView() {
        return notes;
    }

    /// The index of the slide being edited.
    ///
    /// #### Returns
    ///
    /// the index
    public int getSlideIndex() {
        return slide;
    }

    /// Edits another slide.
    ///
    /// #### Parameters
    ///
    /// - `index`: the slide
    public void setSlideIndex(int index) {
        if (index < 0 || index >= getSlideCount()) {
            throw new IndexOutOfBoundsException("slide " + index);
        }
        endTextEdit();
        slide = index;
        selection = null;
        loadNotes();
        fireSelection();
        repaint();
    }

    /// The id of the selected element, or null.
    ///
    /// #### Returns
    ///
    /// the id
    public String getSelection() {
        return selection;
    }

    /// Selects an element of the current slide.
    ///
    /// #### Parameters
    ///
    /// - `elementId`: the id, or null for nothing
    public void setSelection(String elementId) {
        if (elementId != null && (getSlideCount() == 0
                || model.getSlide(slide).find(elementId) == null)) {
            throw new IllegalArgumentException("No element " + elementId);
        }
        if (textEdit != null && !textEdit.id.equals(elementId)) {
            endTextEdit();
        }
        selection = elementId;
        fireSelection();
        canvas.repaint();
    }

    /// Adds a listener told when the selection or the slide changes.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void addSelectionListener(ActionListener<ActionEvent> l) {
        selectionListeners.addListener(l);
    }

    /// Removes a selection listener.
    ///
    /// #### Parameters
    ///
    /// - `l`: the listener
    public void removeSelectionListener(ActionListener<ActionEvent> l) {
        selectionListeners.removeListener(l);
    }

    private void fireSelection() {
        if (selectionListeners.hasListeners()) {
            selectionListeners.fireActionEvent(new ActionEvent(this, ActionEvent.Type.Other));
        }
    }

    private Slide currentSlide() {
        return getSlideCount() == 0 ? null : model.getSlide(slide);
    }

    private SlideElement selected() {
        Slide s = currentSlide();
        return s == null || selection == null ? null : s.find(selection);
    }

    // ------------------------------------------------------------ editing commands

    /// Adds a preset shape in the middle of the slide and selects it.
    ///
    /// #### Parameters
    ///
    /// - `geometry`: a preset name, see `ShapeGeometry.presets()`
    ///
    /// #### Returns
    ///
    /// the new element's id
    public String addShape(String geometry) {
        Presentation p = model.getPresentation();
        float w = p.width / 4;
        float h = ShapeGeometry.isLine(geometry) ? 0 : p.height / 4;
        String id = model.newId('e');
        Theme t = p.theme;
        ShapeElement e = new ShapeElement(id, null, geometry, (p.width - w) / 2,
                (p.height - h) / 2, w, h, 0, false, false,
                ShapeGeometry.isLine(geometry) ? Fill.NO_FILL
                        : Fill.solid(t.color(Theme.ACCENT1)),
                new LineStyle(ShapeGeometry.isLine(geometry) ? t.color(Theme.DARK1)
                        : t.color(Theme.ACCENT1), 1.5f), null, null, null);
        return addAndSelect(e);
    }

    /// Adds a text box and starts editing it.
    ///
    /// #### Returns
    ///
    /// the new element's id
    public String addTextBox() {
        Presentation p = model.getPresentation();
        String id = model.newId('e');
        TextFrame f = TextFrame.of(id + "p", "Text", TextStyle.DEFAULT.withSize(24)
                .withFont(p.theme.minorFont), Paragraph.LEFT);
        addAndSelect(ShapeElement.textBox(id, p.width / 4, p.height / 2 - 30, p.width / 2, 60,
                f));
        beginTextEdit(id);
        return id;
    }

    /// Adds a picture, fitted inside half the slide, and selects it.
    ///
    /// #### Parameters
    ///
    /// - `picture`: the picture
    ///
    /// #### Returns
    ///
    /// the new element's id
    public String addPicture(Picture picture) {
        Presentation p = model.getPresentation();
        float w = Math.max(1, picture.width);
        float h = Math.max(1, picture.height);
        float k = Math.min(1, Math.min(p.width / 2 / w, p.height / 2 / h));
        w *= k;
        h *= k;
        return addAndSelect(new PictureElement(model.newId('e'), picture, (p.width - w) / 2,
                (p.height - h) / 2, w, h));
    }

    private String addAndSelect(SlideElement e) {
        if (getSlideCount() == 0) {
            model.addSlide(-1);
            slide = 0;
        }
        model.addElement(slide, e);
        setSelection(e.id);
        return e.id;
    }

    /// Removes the selected element.
    ///
    /// #### Returns
    ///
    /// false when nothing was selected
    public boolean deleteSelection() {
        if (selection == null) {
            return false;
        }
        endTextEdit();
        String id = selection;
        selection = null;
        boolean done = model.removeElement(slide, id);
        fireSelection();
        return done;
    }

    /// Moves the selected element in front of every other.
    public void bringToFront() {
        if (selection != null) {
            model.moveElement(slide, selection, Integer.MAX_VALUE);
        }
    }

    /// Moves the selected element behind every other.
    public void sendToBack() {
        if (selection != null) {
            model.moveElement(slide, selection, 0);
        }
    }

    // ------------------------------------------------------------ text editing

    /// Starts editing a shape's text in place.
    ///
    /// #### Parameters
    ///
    /// - `elementId`: a shape on the current slide
    ///
    /// #### Returns
    ///
    /// false when the element is not a shape
    public boolean beginTextEdit(String elementId) {
        Slide s = currentSlide();
        SlideElement e = s == null ? null : s.find(elementId);
        if (!(e instanceof ShapeElement) || ((ShapeElement) e).isLine()) {
            return false;
        }
        endTextEdit();
        selection = elementId;
        ShapeElement shape = (ShapeElement) e;
        TextFrame frame = shape.text == null ? new TextFrame(placeholderNotes()) : shape.text;
        textEdit = new TextEdit(elementId, frame);
        canvas.add(textEdit.view);
        canvas.revalidate();
        textEdit.view.requestFocus();
        fireSelection();
        return true;
    }

    /// The text editor while a shape's text is being edited, else null.
    ///
    /// #### Returns
    ///
    /// the editor
    public DocumentView getTextEditor() {
        return textEdit == null ? null : textEdit.view;
    }

    /// Ends in-place text editing; the text is already in the model.
    public void endTextEdit() {
        if (textEdit == null) {
            return;
        }
        TextEdit t = textEdit;
        textEdit = null;
        t.close();
        canvas.removeComponent(t.view);
        canvas.repaint();
    }

    /// The editing of one shape's text: a chromeless document view at the canvas scale whose
    /// every change replaces the shape's text, all of it one undo step.
    private final class TextEdit implements DocumentListener {
        final String id;
        final DocumentView view;
        final TextFrame frame;
        private boolean grouped;

        TextEdit(String id, TextFrame frame) {
            this.id = id;
            this.frame = frame;
            DocumentModel dm = new DocumentModel(new Document(frame.blocks, null, null, 1));
            view = new DocumentView(dm);
            view.setChromeless(true);
            view.getAllStyles().setBgTransparency(0);
            view.getAllStyles().setPadding(0, 0, 0, 0);
            view.getAllStyles().setMargin(0, 0, 0, 0);
            view.getAllStyles().setBorder(null);
            dm.addDocumentListener(this);
        }

        @Override
        public void documentChanged(DocumentModel m, Document before, Document after) {
            if (!grouped) {
                model.beginGroup();
                grouped = true;
            }
            model.setText(slide, id, after.blocks);
        }

        void close() {
            view.getModel().removeDocumentListener(this);
            if (grouped) {
                model.endGroup();
                grouped = false;
            }
        }
    }

    // ------------------------------------------------------------ the canvas

    /// Where the slide is drawn in the canvas: x, y in canvas coordinates and the scale.
    float[] slideArea() {
        Presentation p = model.getPresentation();
        int margin = Display.getInstance().convertToPixels(4);
        float w = Math.max(1, canvas.getWidth() - 2 * margin);
        float h = Math.max(1, canvas.getHeight() - 2 * margin);
        float scale = Math.min(w / p.width, h / p.height);
        return new float[] {(canvas.getWidth() - p.width * scale) / 2,
            (canvas.getHeight() - p.height * scale) / 2, scale};
    }

    /// A point of the canvas, in absolute pixels, in slide points.
    float[] toSlide(int absX, int absY) {
        float[] a = slideArea();
        return new float[] {(absX - canvas.getAbsoluteX() - a[0]) / a[2],
            (absY - canvas.getAbsoluteY() - a[1]) / a[2]};
    }

    /// The corners and handle points of an element in slide points, rotated: 8 resize handles
    /// clockwise from the top left, then the rotation handle.
    static float[][] handles(SlideElement e, float handleOffset) {
        float[][] local = {
            {-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};
        float cx = e.x + e.width / 2;
        float cy = e.y + e.height / 2;
        double a = Math.toRadians(e.rotation);
        float cos = (float) Math.cos(a);
        float sin = (float) Math.sin(a);
        float[][] out = new float[9][];
        for (int i = 0; i < 8; i++) {
            float lx = local[i][0] * e.width / 2;
            float ly = local[i][1] * e.height / 2;
            out[i] = new float[] {cx + lx * cos - ly * sin, cy + lx * sin + ly * cos};
        }
        float ly = -e.height / 2 - handleOffset;
        out[8] = new float[] {cx - ly * sin, cy + ly * cos};
        return out;
    }

    /// The resize directions of the 8 handles.
    private static final int[][] HANDLE_DIR = {
        {-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};

    /// The element's box resized by dragging a handle by (`dx`, `dy`) slide points, keeping
    /// the opposite side where it is under any rotation.
    static float[] resize(SlideElement e, int handle, float dx, float dy) {
        double a = Math.toRadians(e.rotation);
        float cos = (float) Math.cos(a);
        float sin = (float) Math.sin(a);
        float ldx = dx * cos + dy * sin;
        float ldy = -dx * sin + dy * cos;
        int sx = HANDLE_DIR[handle][0];
        int sy = HANDLE_DIR[handle][1];
        float nw = Math.max(1, e.width + sx * ldx);
        float nh = e.height == 0 && sy == 0 ? 0 : Math.max(e.height == 0 ? 0 : 1,
                e.height + sy * ldy);
        float shx = sx * (nw - e.width) / 2;
        float shy = sy * (nh - e.height) / 2;
        float cx = e.x + e.width / 2 + shx * cos - shy * sin;
        float cy = e.y + e.height / 2 + shx * sin + shy * cos;
        return new float[] {cx - nw / 2, cy - nh / 2, nw, nh};
    }

    /// The rotation a drag of the rotation handle to a point gives, snapped near multiples
    /// of 15 degrees.
    static float rotationTo(SlideElement e, float px, float py) {
        float cx = e.x + e.width / 2;
        float cy = e.y + e.height / 2;
        double deg = Math.toDegrees(MathUtil.atan2(py - cy, px - cx)) + 90;
        // atan2 answers -180..180, so one turn at most brings it into 0..360
        if (deg < 0) {
            deg += 360;
        }
        if (deg >= 360) {
            deg -= 360;
        }
        double snap = Math.round(deg / 15) * 15.0;
        if (Math.abs(deg - snap) < 3) {
            deg = snap % 360;
        }
        return (float) deg;
    }

    private final class Canvas extends Container {
        private static final int NONE = -1;
        private static final int MOVE = 9;
        private static final int ROTATE = 8;
        private int mode = NONE;
        private float[] pressAt;
        private SlideElement pressed;
        private boolean grouped;
        private long lastTap;
        private String lastTapId;

        Canvas() {
            super(new EditorPlacement());
            setUIIDFinal("SlideViewCanvas");
            setFocusable(true);
            setGrabsPointerEvents(true);
        }

        private float handleRadius() {
            return Display.getInstance().convertToPixels(HANDLE_MM) / slideArea()[2];
        }

        @Override
        public void paintBackground(Graphics g) {
            g.setColor(DESK);
            g.fillRect(getX(), getY(), getWidth(), getHeight());
        }

        @Override
        public void paint(Graphics g) {
            Slide s = currentSlide();
            if (s == null) {
                return;
            }
            float[] a = slideArea();
            int sx = getX() + Math.round(a[0]);
            int sy = getY() + Math.round(a[1]);
            Presentation p = model.getPresentation();
            g.setColor(0xc5cad1);
            g.fillRect(sx + 2, sy + 2, Math.round(p.width * a[2]), Math.round(p.height * a[2]));
            // while text is edited the editor draws it; the shape itself stays
            renderer.paint(g, p, textEdit != null ? withoutText(s, textEdit.id) : s, sx, sy,
                    a[2]);
            SlideElement e = selected();
            if (e != null) {
                paintSelection(g, e, sx, sy, a[2]);
            }
            super.paint(g);
        }

        private Slide withoutText(Slide s, String id) {
            SlideElement e = s.find(id);
            int i = s.indexOf(id);
            if (!(e instanceof ShapeElement) || i < 0) {
                return s;
            }
            return s.withElement(i, ((ShapeElement) e).withText(null));
        }

        private void paintSelection(Graphics g, SlideElement e, int sx, int sy, float scale) {
            float r = handleRadius();
            float[][] h = handles(e, r * 5);
            g.setColor(ACCENT);
            GeneralPath box = new GeneralPath();
            int[] order = {0, 2, 4, 6};
            for (int i = 0; i < order.length; i++) {
                float[] pt = h[order[i]];
                float px = sx + pt[0] * scale;
                float py = sy + pt[1] * scale;
                if (i == 0) {
                    box.moveTo(px, py);
                } else {
                    box.lineTo(px, py);
                }
            }
            box.closePath();
            int rp = Math.max(3, Math.round(r * scale));
            if (g.isShapeSupported()) {
                g.drawShape(box, new Stroke(1, Stroke.CAP_BUTT, Stroke.JOIN_MITER, 1));
            }
            g.drawLine(Math.round(sx + h[1][0] * scale), Math.round(sy + h[1][1] * scale),
                    Math.round(sx + h[8][0] * scale), Math.round(sy + h[8][1] * scale));
            for (int i = 0; i < 9; i++) {
                int px = Math.round(sx + h[i][0] * scale);
                int py = Math.round(sy + h[i][1] * scale);
                g.setColor(0xffffff);
                g.fillArc(px - rp, py - rp, rp * 2, rp * 2, 0, 360);
                g.setColor(ACCENT);
                g.drawArc(px - rp, py - rp, rp * 2, rp * 2, 0, 360);
            }
        }

        private int handleAt(SlideElement e, float px, float py) {
            float r = handleRadius() * 1.8f;
            float[][] h = handles(e, handleRadius() * 5);
            for (int i = 8; i >= 0; i--) {
                float dx = px - h[i][0];
                float dy = py - h[i][1];
                if (dx * dx + dy * dy <= r * r) {
                    return i;
                }
            }
            return NONE;
        }

        private SlideElement elementAt(float px, float py) {
            Slide s = currentSlide();
            if (s == null) {
                return null;
            }
            float slop = handleRadius();
            for (int i = s.elements.size() - 1; i >= 0; i--) {
                SlideElement e = s.elements.get(i);
                if (e.contains(px, py) || (e.width < slop * 2 || e.height < slop * 2)
                        && e.withBounds(e.x - slop, e.y - slop, e.width + 2 * slop,
                        e.height + 2 * slop).contains(px, py)) {
                    return e;
                }
            }
            return null;
        }

        @Override
        public void pointerPressed(int x, int y) {
            // a press with a drag still open means its release was lost
            endDrag();
            float[] pt = toSlide(x, y);
            SlideElement sel = selected();
            int h = sel == null ? NONE : handleAt(sel, pt[0], pt[1]);
            if (textEdit != null) {
                SlideElement edited = currentSlide().find(textEdit.id);
                if (edited != null && edited.contains(pt[0], pt[1])) {
                    super.pointerPressed(x, y);
                    return;
                }
                endTextEdit();
            }
            if (h != NONE) {
                mode = h;
                pressed = sel;
            } else {
                SlideElement hit = elementAt(pt[0], pt[1]);
                long now = System.currentTimeMillis();
                if (hit != null && hit.id.equals(lastTapId) && now - lastTap < 400) {
                    beginTextEdit(hit.id);
                    mode = NONE;
                    return;
                }
                lastTap = now;
                lastTapId = hit == null ? null : hit.id;
                setSelection(hit == null ? null : hit.id);
                mode = hit == null ? NONE : MOVE;
                pressed = hit;
            }
            pressAt = pt;
            requestFocus();
        }

        @Override
        public void pointerDragged(int x, int y) {
            if (mode == NONE || pressed == null) {
                super.pointerDragged(x, y);
                return;
            }
            float[] pt = toSlide(x, y);
            if (!grouped) {
                model.beginGroup();
                grouped = true;
            }
            float dx = pt[0] - pressAt[0];
            float dy = pt[1] - pressAt[1];
            if (mode == MOVE) {
                model.setBounds(slide, pressed.id, pressed.x + dx, pressed.y + dy,
                        pressed.width, pressed.height);
            } else if (mode == ROTATE) {
                model.setRotation(slide, pressed.id, rotationTo(pressed, pt[0], pt[1]));
            } else {
                float[] b = resize(pressed, mode, dx, dy);
                model.setBounds(slide, pressed.id, b[0], b[1], b[2], b[3]);
            }
        }

        @Override
        public void pointerReleased(int x, int y) {
            endDrag();
            super.pointerReleased(x, y);
        }

        /// Ends a drag, closing its undo group; also when its release never arrived.
        void endDrag() {
            if (grouped) {
                model.endGroup();
                grouped = false;
            }
            mode = NONE;
            pressed = null;
        }

        @Override
        public void keyPressed(int keyCode) {
            if (textEdit != null) {
                super.keyPressed(keyCode);
                return;
            }
            SlideElement e = selected();
            int game = Display.getInstance().getGameAction(keyCode);
            if (e != null && (keyCode == 8 || keyCode == 127 || keyCode == -8)) {
                deleteSelection();
            } else if (e != null && (game == Display.GAME_LEFT || game == Display.GAME_RIGHT
                    || game == Display.GAME_UP || game == Display.GAME_DOWN)) {
                float nx = game == Display.GAME_LEFT ? -1 : game == Display.GAME_RIGHT ? 1 : 0;
                float ny = game == Display.GAME_UP ? -1 : game == Display.GAME_DOWN ? 1 : 0;
                model.setBounds(slide, e.id, e.x + nx, e.y + ny, e.width, e.height);
            } else {
                super.keyPressed(keyCode);
            }
        }
    }

    /// Lays the in-place text editor over the shape it edits.
    private final class EditorPlacement extends Layout {
        @Override
        public void layoutContainer(Container parent) {
            if (textEdit == null || parent.getComponentCount() == 0) {
                return;
            }
            Slide s = currentSlide();
            SlideElement e = s == null ? null : s.find(textEdit.id);
            if (!(e instanceof ShapeElement)) {
                return;
            }
            float[] a = slideArea();
            TextBoxLayout tl = TextBoxLayout.of(textEdit.frame, e.width, e.height,
                    renderer.getMeasurer());
            Component c = textEdit.view;
            c.setX(Math.round(a[0] + e.x * a[2]));
            c.setY(Math.round(a[1] + (e.y + tl.offsetY) * a[2]));
            c.setWidth(Math.max(1, Math.round(e.width * a[2])));
            c.setHeight(Math.max(1, Math.round(Math.max(tl.usedHeight, e.height - tl.offsetY)
                    * a[2])));
            textEdit.view.setFixedScale(a[2] * tl.shrink);
        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            return new Dimension(Display.getInstance().convertToPixels(100),
                    Display.getInstance().convertToPixels(60));
        }
    }

    // ------------------------------------------------------------ thumbnails

    /// The slides as a column of thumbnails; tap one to edit it.
    private final class ThumbStrip extends Component {
        ThumbStrip() {
            setUIIDFinal("SlideViewThumbnails");
            setScrollableY(true);
            setFocusable(false);
        }

        private int thumbWidth() {
            return Display.getInstance().convertToPixels(22);
        }

        private int thumbHeight() {
            Presentation p = model.getPresentation();
            return Math.round(thumbWidth() * p.height / p.width);
        }

        private int gap() {
            return Display.getInstance().convertToPixels(2);
        }

        @Override
        protected Dimension calcPreferredSize() {
            int n = getSlideCount();
            return new Dimension(thumbWidth() + gap() * 3, n * (thumbHeight() + gap()) + gap());
        }

        @Override
        public void paint(Graphics g) {
            Presentation p = model.getPresentation();
            int tw = thumbWidth();
            int th = thumbHeight();
            int x = getX() + gap() * 2;
            float scale = tw / p.width;
            for (int i = 0; i < p.slides.size(); i++) {
                int y = getY() + gap() + i * (th + gap());
                if (y + th < getY() + getScrollY() || y > getY() + getScrollY() + getHeight()) {
                    continue;
                }
                if (i == slide) {
                    g.setColor(ACCENT);
                    g.fillRect(x - 3, y - 3, tw + 6, th + 6);
                }
                renderer.paint(g, p, p.slides.get(i), x, y, scale);
                if (p.slides.get(i).hidden) {
                    int was = g.getAlpha();
                    g.setColor(0xffffff);
                    g.setAlpha(150 * was / 255);
                    g.fillRect(x, y, tw, th);
                    g.setAlpha(was);
                }
                // in the strip's own font: the renderer leaves the graphics on whatever
                // scaled slide font it used last, which after a slide of small table
                // text drew the number a pixel high
                g.setFont(getStyle().getFont());
                g.setColor(0x6b7280);
                g.drawString(String.valueOf(i + 1), getX() + 2, y);
            }
        }

        @Override
        public void pointerReleased(int x, int y) {
            super.pointerReleased(x, y);
            int ry = y - getAbsoluteY() + getScrollY() - gap();
            int i = ry / (thumbHeight() + gap());
            if (ry >= 0 && i >= 0 && i < getSlideCount()) {
                setSlideIndex(i);
            }
        }
    }
}
