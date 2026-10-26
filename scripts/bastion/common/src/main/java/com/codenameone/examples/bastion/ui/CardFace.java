/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.ui;

import com.codename1.l10n.SimpleDateFormat;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import java.util.Date;

/// A payment card drawn as one: the ID-1 shape of a real card (85.60 by
/// 53.98 mm, about 1.586 to 1) with rounded corners, a gradient, the chip,
/// the brand, the number in groups of four, the cardholder and the expiry.
/// A frozen card is drawn greyed with a frost mark across it.
///
/// The colours and type come from styles (`BnCardFace`, `BnCardFaceEnd`,
/// `BnCardChip`, `BnCardBrand`, `BnCardNumber`, `BnCardCaption`,
/// `BnCardName`), so the theme re-skins it. The face is never mirrored in a
/// right-to-left language: a card is a physical object and reads the same
/// way in every country.
public final class CardFace extends Component {
    /// Width over height of an ID-1 card.
    static final float ASPECT = 85.60f / 53.98f;
    /// A card on screen is never wider than this, in mm, so a tablet or a
    /// desktop window does not draw a card the size of a placemat.
    private static final float MAX_WIDTH_MM = 95f;

    private final String last4;
    private final String holder;
    private final String expiry;
    private final boolean frozen;

    /// @param last4 the last four digits, all the app ever has
    /// @param holder the name printed on the card
    /// @param expiry the expiry, MM/YY
    /// @param frozen whether to draw it frozen
    public CardFace(String last4, String holder, String expiry, boolean frozen) {
        this.last4 = last4;
        this.holder = holder;
        this.expiry = expiry;
        this.frozen = frozen;
        setUIID("BnCardFace");
        setFocusable(false);
    }

    /// The expiry printed on a card issued at `issuedAt`: three years on,
    /// the way a virtual card is commonly issued.
    public static String expiryOf(long issuedAt) {
        long at = issuedAt <= 0 ? System.currentTimeMillis() : issuedAt;
        return new SimpleDateFormat("MM/yy").format(new Date(at + 3L * 365 * 24 * 3600 * 1000));
    }

    /// The number as printed: three masked groups and the last four.
    public static String maskedNumber(String last4) {
        return "\u2022\u2022\u2022\u2022 \u2022\u2022\u2022\u2022 \u2022\u2022\u2022\u2022 "
                + (last4 == null ? "" : last4);
    }

    @Override
    protected Dimension calcPreferredSize() {
        int room = CN.getDisplayWidth() - CN.convertToPixels(8f);
        Component parent = getParent();
        if (parent != null && parent.getWidth() > 0) {
            room = parent.getWidth() - parent.getStyle().getHorizontalPadding();
        }
        int width = Math.min(room, CN.convertToPixels(MAX_WIDTH_MM));
        return new Dimension(width, Math.round(width / ASPECT));
    }

    @Override
    public void paint(Graphics g) {
        int w = Math.min(getWidth(), Math.round(getHeight() * ASPECT));
        int h = Math.round(w / ASPECT);
        int x = getX() + (getWidth() - w) / 2;
        int y = getY();
        boolean anti = g.isAntiAliased();
        g.setAntiAliased(true);
        int radius = Math.max(4, w / 22);
        int from = color(frozen ? "BnCardFaceFrozen" : "BnCardFace");
        int to = color(frozen ? "BnCardFaceFrozenEnd" : "BnCardFaceEnd");
        // A diagonal gradient, a row at a time, each row cut in at the
        // rounded corners.
        for (int row = 0; row < h; row++) {
            int inset = 0;
            int dy = row < radius ? radius - row : row >= h - radius ? row - (h - radius - 1) : 0;
            if (dy > 0) {
                inset = radius - (int) Math.round(Math.sqrt(radius * radius - dy * dy));
            }
            int left = mix(from, to, (float) row / h * 0.6f);
            int right = mix(from, to, 0.4f + (float) row / h * 0.6f);
            int span = w - 2 * inset;
            if (span > 0) {
                g.fillLinearGradient(left, right, x + inset, y + row, span, 1, true);
            }
        }
        int pad = Math.round(w * 0.07f);
        // The brand: the shield and the name, top left.
        Style brand = UIManager.getInstance().getComponentStyle("BnCardBrand");
        Font brandFont = brand.getFont();
        g.setColor(brand.getFgColor());
        g.setFont(brandFont);
        Font icon = FontImage.getMaterialDesignFont().derive(brandFont.getHeight(), Font.STYLE_PLAIN);
        g.setFont(icon);
        g.drawString(String.valueOf(FontImage.MATERIAL_SHIELD), x + pad, y + pad);
        int iconWidth = icon.charWidth(FontImage.MATERIAL_SHIELD);
        g.setFont(brandFont);
        g.drawString("Bastion", x + pad + iconWidth + pad / 4, y + pad);
        // What kind of card, top right, with the contactless mark under it.
        Style caption = UIManager.getInstance().getComponentStyle("BnCardCaption");
        Font captionFont = caption.getFont();
        g.setFont(captionFont);
        g.setColor(caption.getFgColor());
        String kind = Lang.tr("Virtual debit");
        g.drawString(kind, x + w - pad - captionFont.stringWidth(kind),
                y + pad + (brandFont.getHeight() - captionFont.getHeight()) / 2);
        // The chip, left, a third of the way down.
        int chipW = Math.round(w * 0.13f);
        int chipH = Math.round(chipW * 0.76f);
        int chipX = x + pad;
        int chipY = y + Math.round(h * 0.34f);
        int chip = color("BnCardChip");
        g.setColor(chip);
        g.fillRoundRect(chipX, chipY, chipW, chipH, chipH / 3, chipH / 3);
        g.setColor(mix(chip, 0x000000, 0.35f));
        g.drawRoundRect(chipX, chipY, chipW, chipH, chipH / 3, chipH / 3);
        g.drawLine(chipX + chipW / 3, chipY, chipX + chipW / 3, chipY + chipH);
        g.drawLine(chipX + 2 * chipW / 3, chipY, chipX + 2 * chipW / 3, chipY + chipH);
        g.drawLine(chipX, chipY + chipH / 2, chipX + chipW / 3, chipY + chipH / 2);
        g.drawLine(chipX + 2 * chipW / 3, chipY + chipH / 2, chipX + chipW, chipY + chipH / 2);
        Font contactless = FontImage.getMaterialDesignFont().derive(chipH, Font.STYLE_PLAIN);
        g.setFont(contactless);
        g.setColor(caption.getFgColor());
        g.drawString(String.valueOf(FontImage.MATERIAL_CONTACTLESS),
                chipX + chipW + pad / 2, chipY);
        // The number, in groups of four, under the chip.
        Style number = UIManager.getInstance().getComponentStyle("BnCardNumber");
        Font numberFont = number.getFont();
        g.setFont(numberFont);
        g.setColor(number.getFgColor());
        g.drawString(maskedNumber(last4), x + pad, chipY + chipH + Math.round(h * 0.07f));
        // The cardholder and the expiry along the bottom, each under a
        // small caption.
        Style name = UIManager.getInstance().getComponentStyle("BnCardName");
        Font nameFont = name.getFont();
        int bottom = y + h - pad;
        int valueY = bottom - nameFont.getHeight();
        int captionY = valueY - captionFont.getHeight();
        String expires = Lang.tr("Expires");
        int expiryWidth = Math.max(nameFont.stringWidth(expiry), captionFont.stringWidth(expires));
        int expiryX = x + w - pad - expiryWidth;
        g.setFont(captionFont);
        g.setColor(caption.getFgColor());
        g.drawString(Lang.tr("Cardholder"), x + pad, captionY);
        g.drawString(expires, expiryX, captionY);
        g.setFont(nameFont);
        g.setColor(name.getFgColor());
        g.drawString(fit(holder, nameFont, expiryX - x - 2 * pad), x + pad, valueY);
        g.drawString(expiry, expiryX, valueY);
        if (frozen) {
            // The frost: a veil over the face and the mark in its middle.
            int alpha = g.getAlpha();
            g.setColor(0xffffff);
            g.setAlpha(70);
            g.fillRoundRect(x, y, w, h, 2 * radius, 2 * radius);
            g.setAlpha(alpha);
            String label = Lang.tr("Frozen");
            Font mark = FontImage.getMaterialDesignFont().derive(h / 4, Font.STYLE_PLAIN);
            int markW = mark.charWidth(FontImage.MATERIAL_AC_UNIT);
            int labelW = brandFont.stringWidth(label);
            int total = markW + pad / 3 + labelW;
            int left = x + (w - total) / 2;
            int middle = y + h / 2;
            g.setColor(0xffffff);
            g.setFont(mark);
            g.drawString(String.valueOf(FontImage.MATERIAL_AC_UNIT), left,
                    middle - mark.getHeight() / 2);
            g.setFont(brandFont);
            g.drawString(label, left + markW + pad / 3, middle - brandFont.getHeight() / 2);
        }
        g.setAntiAliased(anti);
    }

    /// `text`, cut with an ellipsis only when it is wider than `width` --
    /// a name longer than the card, which a real card would print short too.
    private static String fit(String text, Font font, int width) {
        if (text == null) {
            return "";
        }
        if (font.stringWidth(text) <= width) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && font.stringWidth(cut + "\u2026") > width) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "\u2026";
    }

    private static int color(String uiid) {
        return UIManager.getInstance().getComponentStyle(uiid).getFgColor();
    }

    private static int mix(int a, int b, float t) {
        float u = Math.max(0f, Math.min(1f, t));
        int r = Math.round(((a >> 16) & 0xff) * (1 - u) + ((b >> 16) & 0xff) * u);
        int gr = Math.round(((a >> 8) & 0xff) * (1 - u) + ((b >> 8) & 0xff) * u);
        int bl = Math.round((a & 0xff) * (1 - u) + (b & 0xff) * u);
        return (r << 16) | (gr << 8) | bl;
    }
}
