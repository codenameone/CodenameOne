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
package com.codenameone.examples.bazaar.web.account;

import com.codename1.qrcode.QrCode;

/// A QR code drawn as one SVG path, so a page shows it inline: no image request,
/// nothing for the CSP to allow, and it scales sharply. The compiled views have
/// no raw-HTML output, so the page writes `<svg><path d=...>` itself and takes
/// the path data and view box from here.
public final class QrSvg {
    /// Modules of quiet zone around the code, as the standard asks.
    static final int QUIET = 4;

    private QrSvg() {
    }

    /// The path data of `text`'s QR code: one unit square per dark module.
    public static String path(String text) {
        QrCode qr = QrCode.encode(text, QrCode.ECC_MEDIUM);
        int n = qr.getSize();
        StringBuilder d = new StringBuilder(n * n * 4);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                if (qr.isDark(x, y)) {
                    d.append('M').append(x + QUIET).append(' ').append(y + QUIET)
                            .append("h1v1h-1z");
                }
            }
        }
        return d.toString();
    }

    /// The view box that frames [#path] of `text` with its quiet zone.
    public static String box(String text) {
        int side = QrCode.encode(text, QrCode.ECC_MEDIUM).getSize() + 2 * QUIET;
        return "0 0 " + side + " " + side;
    }
}
