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
package com.codename1.ui.kanban;

import com.codename1.kanban.KanbanCard;
import com.codename1.ui.Font;
import com.codename1.ui.Graphics;
import com.codename1.ui.plaf.Style;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The renderer a board starts with: a card with its title, up to two lines
/// of description, its tags as small labels and its badge, and a strip of
/// colour down its leading edge when its style key has one.
///
/// Colours and fonts come from the UIIDs `KanbanCard`, `KanbanCardSelected`
/// and `KanbanCardTag`.
public class DefaultKanbanCardRenderer implements KanbanCardRenderer {
    private final Map<String, Integer> accents = new HashMap<String, Integer>();
    private final Map<String, Integer> tagColors = new HashMap<String, Integer>();
    private int titleLines = 2;
    private int descriptionLines = 2;
    // the wrapped lines of the cards painted lately, by card id
    private final Map<String, Wrapped> wrapped = new HashMap<String, Wrapped>();

    private static final class Wrapped {
        String title;
        String description;
        int width;
        Font font;
        List<String> titleRows;
        List<String> descriptionRows;
    }

    /// Gives the cards with a style key a colour, drawn down their leading
    /// edge.
    public void setAccent(String styleKey, int rgb) {
        accents.put(styleKey, Integer.valueOf(rgb));
    }

    /// Gives a tag a background colour of its own.
    public void setTagColor(String tag, int rgb) {
        tagColors.put(tag, Integer.valueOf(rgb));
    }

    /// The most lines a title takes before it is cut short; 2 by default.
    public int getTitleLines() {
        return titleLines;
    }

    /// Sets the most lines a title takes.
    public void setTitleLines(int titleLines) {
        this.titleLines = Math.max(1, titleLines);
        wrapped.clear();
    }

    /// The most lines a description takes; 2 by default, 0 to hide it.
    public int getDescriptionLines() {
        return descriptionLines;
    }

    /// Sets the most lines a description takes.
    public void setDescriptionLines(int descriptionLines) {
        this.descriptionLines = Math.max(0, descriptionLines);
        wrapped.clear();
    }

    private static int pad() {
        return KanbanLook.mm(1.5f);
    }

    private Wrapped rows(KanbanCard card, int width) {
        Font f = KanbanLook.style("KanbanCard").getFont();
        Wrapped w = wrapped.get(card.getId());
        String title = card.getTitle();
        String desc = card.getDescription();
        if (w != null && w.width == width && w.font == f // NOPMD CompareObjectsWithEquals - the theme's font itself
                && (title == null ? w.title == null : title.equals(w.title))
                && (desc == null ? w.description == null : desc.equals(w.description))) {
            return w;
        }
        // a bound only against cards that come and go: a board's own cards
        // fit, as clearing on every layout of a large board would re-wrap
        // every card it measures
        if (wrapped.size() > 4096) {
            wrapped.clear();
        }
        w = new Wrapped();
        w.title = title;
        w.description = desc;
        w.width = width;
        w.font = f;
        w.titleRows = KanbanLook.wrap(title, f, width, titleLines);
        w.descriptionRows = KanbanLook.wrap(desc, KanbanLook.style("KanbanCardTag").getFont(), width,
                descriptionLines);
        wrapped.put(card.getId(), w);
        return w;
    }

    @Override
    public int getCardHeight(KanbanBoard board, KanbanCard card, int width) {
        Font f = KanbanLook.style("KanbanCard").getFont();
        Font small = KanbanLook.style("KanbanCardTag").getFont();
        int pad = pad();
        Wrapped w = rows(card, width - pad * 2 - strip(card));
        int h = pad * 2 + Math.max(1, w.titleRows.size()) * f.getHeight();
        h += w.descriptionRows.size() * small.getHeight();
        if (!card.getTags().isEmpty() || card.getBadge() != null) {
            h += small.getHeight() + pad;
        }
        return h;
    }

    private int strip(KanbanCard card) {
        return card.getStyleKey() != null && accents.containsKey(card.getStyleKey()) ? KanbanLook.mm(1f) : 0;
    }

    @Override
    public void paintCard(Graphics g, KanbanBoard board, KanbanCard card, int x, int y, int w, int h,
            boolean selected, float lift) {
        Style s = KanbanLook.style("KanbanCard");
        Style sel = KanbanLook.style("KanbanCardSelected");
        Style ts = KanbanLook.style("KanbanCardTag");
        int bg = KanbanLook.bg(s, 0xffffff);
        int fg = s.getFgColor();
        int arc = KanbanLook.mm(1.6f);
        int pad = pad();
        KanbanLook.shadow(g, x, y, w, h, arc, 0.5f + lift * 1.5f);
        g.setColor(bg);
        g.fillRoundRect(x, y, w, h, arc, arc);
        int strip = strip(card);
        if (strip > 0) {
            g.setColor(accents.get(card.getStyleKey()).intValue());
            g.fillRoundRect(x, y, strip * 2, h, arc, arc);
            g.setColor(bg);
            g.fillRect(x + strip, y, strip, h);
        }
        g.setColor(selected ? sel.getFgColor() : KanbanLook.mix(bg, fg, 40));
        g.drawRoundRect(x, y, w - 1, h - 1, arc, arc);
        if (selected) {
            g.drawRoundRect(x + 1, y + 1, w - 3, h - 3, arc, arc);
        }
        Font f = s.getFont();
        Font small = ts.getFont();
        int tx = x + pad + strip;
        int room = w - pad * 2 - strip;
        Wrapped rows = rows(card, room);
        int ty = y + pad;
        g.setFont(f);
        g.setColor(fg);
        for (int i = 0; i < rows.titleRows.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
            g.drawString(rows.titleRows.get(i), tx, ty);
            ty += f.getHeight();
        }
        if (rows.titleRows.isEmpty()) {
            ty += f.getHeight();
        }
        g.setFont(small);
        g.setColor(KanbanLook.mix(bg, fg, 160));
        for (int i = 0; i < rows.descriptionRows.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
            g.drawString(rows.descriptionRows.get(i), tx, ty);
            ty += small.getHeight();
        }
        List<String> tags = card.getTags();
        String badge = card.getBadge();
        if (tags.isEmpty() && badge == null) {
            return;
        }
        ty += pad / 2;
        int right = x + w - pad;
        if (badge != null) {
            int bw = small.stringWidth(badge);
            g.setColor(KanbanLook.mix(bg, fg, 160));
            g.drawString(badge, right - bw, ty);
            right -= bw + pad;
        }
        int px = tx;
        int pillPad = Math.max(2, pad / 2);
        int pillH = small.getHeight();
        for (int i = 0; i < tags.size(); i++) { // NOPMD ForLoopCanBeForeach - indexed: painted every frame, no iterator
            String tag = tags.get(i);
            int pw = small.stringWidth(tag) + pillPad * 2;
            if (px + pw > right) {
                break;
            }
            Integer own = tagColors.get(tag);
            int pillBg = own == null ? KanbanLook.bg(ts, KanbanLook.mix(bg, fg, 30)) : own.intValue();
            g.setColor(pillBg);
            g.fillRoundRect(px, ty, pw, pillH, pillH, pillH);
            g.setColor(own == null ? ts.getFgColor() : contrast(pillBg));
            g.drawString(tag, px + pillPad, ty);
            px += pw + pillPad;
        }
    }

    private static int contrast(int rgb) {
        int luma = (((rgb >> 16) & 0xff) * 299 + ((rgb >> 8) & 0xff) * 587 + (rgb & 0xff) * 114) / 1000;
        return luma < 140 ? 0xffffff : 0x1d1b20;
    }
}
