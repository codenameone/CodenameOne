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
package com.codename1.androidcompat.runtime;

import android.text.InputType;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Graphics;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.DataChangedListener;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.plaf.Style;

/// The peer of an [EditText]: a container holding a native Codename One text
/// field (or text area, for multi-line input) that fills it. The view paints
/// its background underneath; the field paints and edits the text, styled
/// from the view's paint, colors, padding and gravity.
public final class EditTextPeer extends Container {

    private final EditText view;
    private TextArea field;
    private boolean multiLine;
    private boolean updating;

    public EditTextPeer(EditText view) {
        super(new FillLayout());
        this.view = view;
        ViewPeer.strip(this);
        setFocusable(false);
        createField();
    }

    private void createField() {
        if (field != null) {
            removeComponent(field);
        }
        multiLine = !view.isSingleLine();
        if (multiLine) {
            TextArea ta = new TextArea() {
                @Override
                protected void initLaf(com.codename1.ui.plaf.UIManager uim) {
                    super.initLaf(uim);
                    restyle();
                }

                @Override
                public void paint(Graphics g) {
                    ensureStyle();
                    super.paint(g);
                }

                @Override
                protected void paintBackground(Graphics g) {
                }

                @Override
                protected void paintBorder(Graphics g) {
                }

                @Override
                protected void paintScrollbars(Graphics g) {
                }
            };
            ta.setGrowByContent(true);
            ta.setRows(1);
            field = ta;
        } else {
            field = new TextField() {
                @Override
                protected void initLaf(com.codename1.ui.plaf.UIManager uim) {
                    super.initLaf(uim);
                    restyle();
                }

                @Override
                public void paint(Graphics g) {
                    ensureStyle();
                    super.paint(g);
                }

                @Override
                protected void paintBackground(Graphics g) {
                }

                @Override
                protected void paintBorder(Graphics g) {
                }

                @Override
                protected void paintScrollbars(Graphics g) {
                }
            };
        }
        field.setUIID("AndroidEditText");
        // Touches go through the Android view tree like every other view's;
        // EditText starts native editing from its own onTouchEvent, so
        // listeners on the view (a date field opening a picker) still work.
        field.setIgnorePointerEvents(true);
        field.addDataChangedListener(new DataChangedListener() {
            @Override
            public void dataChanged(int type, int index) {
                if (!updating) {
                    view.onPeerTextChanged(field.getText());
                }
            }
        });
        field.setDoneListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                int action = view.imeAction();
                view.onEditorAction(action == EditorInfo.IME_ACTION_UNSPECIFIED ? EditorInfo.IME_ACTION_DONE : action);
            }
        });
        field.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(Component cmp) {
                view.requestFocus();
            }

            @Override
            public void focusLost(Component cmp) {
                view.clearFocus();
            }
        });
        addComponent(field);
        syncFromView();
    }

    /// Re-applies the view's text style, hint, input type and padding.
    public void syncFromView() {
        if (field == null) {
            return;
        }
        if (multiLine == view.isSingleLine()) {
            createField();
            return;
        }
        updating = true;
        try {
            String text = view.getText().toString();
            if (!text.equals(field.getText())) {
                field.setText(text);
            }
            CharSequence hint = view.getShownHint();
            field.setHint(hint == null ? "" : hint.toString());
            field.setConstraint(constraint(view.getInputType()));
            field.setEditable(view.isEnabled());
            restyle();
        } finally {
            updating = false;
        }
        repaint();
    }

    /// Re-applies the styling when something (a theme refresh, a style
    /// lazily recreated from the theme) replaced it. Only writes what
    /// differs, so it never triggers a repaint loop.
    void ensureStyle() {
        if (field == null) {
            return;
        }
        Style s = field.getStyle();
        if (s.getBgTransparency() != 0 || s.getFont() != view.getPaint().cn1Font()
                || s.getFgColor() != (view.getCurrentTextColor() & 0xffffff)
                || s.getPaddingLeft(false) != view.getCompoundPaddingLeft()
                || s.getPaddingTop() != view.getCompoundPaddingTop()) {
            restyle();
        }
        Label hint = field.getHintLabel();
        if (hint != null && (hint.getStyle().getFont() != view.getPaint().cn1Font()
                || hint.getStyle().getFgColor() != (view.getCurrentHintTextColor() & 0xffffff)
                || hint.getStyle().getFgAlpha() != (view.getCurrentHintTextColor() >>> 24))) {
            restyle();
        }
    }

    void restyle() {
        if (field == null || view == null) {
            return;
        }
        Style s = field.getAllStyles();
        s.setBgTransparency(0);
        s.setBgImage(null);
        s.setBorder(com.codename1.ui.plaf.Border.createEmpty());
        s.setMargin(0, 0, 0, 0);
        s.setFont(view.getPaint().cn1Font());
        s.setFgColor(view.getCurrentTextColor() & 0xffffff);
        s.setFgAlpha(view.getCurrentTextColor() >>> 24);
        s.setPaddingUnit(Style.UNIT_TYPE_PIXELS);
        s.setPadding(Component.TOP, view.getCompoundPaddingTop());
        s.setPadding(Component.BOTTOM, view.getCompoundPaddingBottom());
        s.setPadding(Component.LEFT, view.getCompoundPaddingLeft());
        s.setPadding(Component.RIGHT, view.getCompoundPaddingRight());
        int hg = Gravity.getAbsoluteGravity(view.getGravity(), view.getLayoutDirection())
                & Gravity.HORIZONTAL_GRAVITY_MASK;
        field.setAlignment(hg == Gravity.CENTER_HORIZONTAL ? Component.CENTER
                : hg == Gravity.RIGHT ? Component.RIGHT : Component.LEFT);
        Label hintLabel = field.getHintLabel();
        if (hintLabel != null) {
            Style hs = hintLabel.getAllStyles();
            hs.setFont(view.getPaint().cn1Font());
            hs.setFgColor(view.getCurrentHintTextColor() & 0xffffff);
            hs.setFgAlpha(view.getCurrentHintTextColor() >>> 24);
            hs.setBgTransparency(0);
            hs.setBorder(com.codename1.ui.plaf.Border.createEmpty());
            hs.setPadding(0, 0, 0, 0);
            hs.setMargin(0, 0, 0, 0);
        }
    }

    public void setTextFromView(String text) {
        if (field != null && !text.equals(field.getText())) {
            updating = true;
            try {
                field.setText(text);
            } finally {
                updating = false;
            }
        }
    }

    public void startEditingAsync() {
        if (field != null && field.isEditable()) {
            field.startEditingAsync();
        }
    }

    public void setCursor(int pos) {
        if (field instanceof TextField) {
            ((TextField) field).setCursorPosition(Math.max(0, Math.min(pos, field.getText().length())));
        }
    }

    public int cursor() {
        return field == null ? 0 : Math.max(0, field.getCursorPosition());
    }

    /// Whether the field has a cursor `setCursor`/`cursor` reach: a
    /// multi-line text area reports none.
    public boolean tracksCursor() {
        return field instanceof TextField;
    }

    static int constraint(int inputType) {
        int cls = inputType & InputType.TYPE_MASK_CLASS;
        int variation = inputType & InputType.TYPE_MASK_VARIATION;
        int flags = inputType & InputType.TYPE_MASK_FLAGS;
        int c = TextArea.ANY;
        if (cls == InputType.TYPE_CLASS_NUMBER) {
            c = (flags & InputType.TYPE_NUMBER_FLAG_DECIMAL) != 0 ? TextArea.DECIMAL : TextArea.NUMERIC;
            if (variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD) {
                c |= TextArea.PASSWORD;
            }
        } else if (cls == InputType.TYPE_CLASS_PHONE) {
            c = TextArea.PHONENUMBER;
        } else if (cls == InputType.TYPE_CLASS_TEXT) {
            if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    || variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) {
                c = TextArea.EMAILADDR;
            } else if (variation == InputType.TYPE_TEXT_VARIATION_URI) {
                c = TextArea.URL;
            } else if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                    || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) {
                c = TextArea.PASSWORD;
            } else if (variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                c = TextArea.NON_PREDICTIVE;
            }
            if ((flags & InputType.TYPE_TEXT_FLAG_CAP_WORDS) != 0) {
                c |= TextArea.INITIAL_CAPS_WORD;
            } else if ((flags & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) != 0) {
                c |= TextArea.INITIAL_CAPS_SENTENCE;
            } else if ((flags & InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS) != 0) {
                c |= TextArea.UPPERCASE;
            }
            if ((flags & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) {
                c |= TextArea.NON_PREDICTIVE;
            }
        }
        return c;
    }

    @Override
    public boolean isIgnorePointerEvents() {
        return true;
    }

    /// The EditText's background and `onDraw`, as the backdrop of the field.
    /// The field paints no background of its own, so when Codename One
    /// repaints it alone (focus lost, text changed) it paints the backgrounds
    /// behind it first: this, then the parents' groups. Painting the view in
    /// `paint`, with the field skipping its backgrounds, left that backdrop
    /// empty, so whatever the field had painted before -- deleted text, a
    /// caret -- was never painted over.
    @Override
    protected void paintBackground(Graphics g) {
        view.paintPeer(g, getX(), getY());
    }

    @Override
    protected Dimension calcPreferredSize() {
        return ViewPeer.measureUnbounded(view);
    }

    /// The field fills the container; Android padding is the field's padding.
    static final class FillLayout extends Layout {
        @Override
        public void layoutContainer(Container parent) {
            for (int i = 0; i < parent.getComponentCount(); i++) {
                Component c = parent.getComponentAt(i);
                c.setX(0);
                c.setY(0);
                c.setWidth(parent.getWidth());
                c.setHeight(parent.getHeight());
            }
        }

        @Override
        public Dimension getPreferredSize(Container parent) {
            return new Dimension(parent.getWidth(), parent.getHeight());
        }
    }
}
