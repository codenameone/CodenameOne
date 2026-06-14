/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.printing;

import com.codename1.io.FileSystemStorage;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;
import java.io.IOException;
import java.io.OutputStream;

/// Cross platform printing API that hands a document to the platform
/// printing system, typically by popping up the native print dialog where
/// the user picks a printer and options.
///
/// The document is a file in [com.codename1.io.FileSystemStorage]
/// identified by its path and mime type. All platforms accept PDF
/// (`application/pdf`) and common image types (`image/png`, `image/jpeg`);
/// other mime types fail with [PrintResult#STATUS_FAILED] on platforms that
/// can't render them.
///
/// Sample usage:
///
/// ```java
/// if (Printer.isPrintingSupported()) {
///     Printer.print(reportPath, "application/pdf", new PrintResultListener() {
///         public void onResult(PrintResult result) {
///             if (result.isFailed()) {
///                 ToastBar.showErrorMessage("Print failed: " + result.getError());
///             }
///         }
///     });
/// }
/// ```
public final class Printer {

    private Printer() {
    }

    /// Returns true if the underlying platform can print documents. When
    /// this returns false [#print] reports [PrintResult#STATUS_FAILED] to
    /// its listener without showing any UI.
    public static boolean isPrintingSupported() {
        return Display.getInstance().isPrintingSupported();
    }

    /// Print a document file through the platform printing system,
    /// typically showing the native print dialog.
    ///
    /// #### Parameters
    ///
    /// - `filePath`: path of the document in [com.codename1.io.FileSystemStorage]
    ///
    /// - `mimeType`: the document type, e.g. `application/pdf`, `image/png`
    ///
    /// - `listener`: callback for the print outcome, invoked on the EDT. May be null.
    public static void print(String filePath, String mimeType, PrintResultListener listener) {
        Display.getInstance().print(filePath, mimeType, listener);
    }

    /// Convenience variant of [#print(String,String,PrintResultListener)]
    /// for PDF documents.
    public static void printPDF(String filePath, PrintResultListener listener) {
        print(filePath, "application/pdf", listener);
    }

    /// Print an image through the platform printing system. The image is
    /// encoded to a temporary PNG file that is deleted once the print flow
    /// finishes.
    ///
    /// #### Parameters
    ///
    /// - `image`: the image to print
    ///
    /// - `listener`: callback for the print outcome, invoked on the EDT. May be null.
    public static void printImage(Image image, final PrintResultListener listener) {
        final FileSystemStorage fs = FileSystemStorage.getInstance();
        final String path = fs.getAppHomePath() + "cn1-print-image-temp.png";
        try {
            OutputStream os = fs.openOutputStream(path);
            try {
                ImageIO.getImageIO().save(image, os, ImageIO.FORMAT_PNG, 1);
            } finally {
                os.close();
            }
        } catch (IOException err) {
            if (listener != null) {
                listener.onResult(PrintResult.failed(err.toString()));
            }
            return;
        }
        print(path, "image/png", new PrintResultListener() {
            @Override
            public void onResult(PrintResult result) {
                fs.delete(path);
                if (listener != null) {
                    listener.onResult(result);
                }
            }
        });
    }
}
