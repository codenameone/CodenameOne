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

package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.io.FileSystemStorage;
import com.codename1.ui.ClipboardContent;
import com.codename1.ui.Display;
import com.codename1.ui.Image;
import com.codename1.util.Base64;

import java.io.OutputStream;

/**
 * Exercises the generic {@link ClipboardContent} clipboard on the device: it copies each
 * representation (plain text, HTML, a PNG image and a file reference) to the real system clipboard and
 * reads it straight back, asserting the value survives the round trip on every platform. Each
 * representation is copied on its own because a text-only OS clipboard (Linux/Windows) can only hold one
 * native format at a time; the assertions are deliberately lenient where a platform legitimately
 * transforms the payload (images are re-encoded, file paths are rewritten as URIs), checking that the
 * value is semantically preserved rather than byte-identical.
 */
public class ClipboardRoundTripTest extends BaseTest {

    // A 1x1 PNG. Kept as a literal so the test never depends on an image encoder being present.
    private static final String PNG_1X1_BASE64 =
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

    @Override
    public boolean runTest() {
        try {
            roundTripText();
            roundTripHtml();
            roundTripImage();
            roundTripFile();
        } catch (Throwable t) {
            fail("clipboard round-trip failed: " + t);
            return false;
        }
        done();
        return true;
    }

    private void roundTripText() {
        Display.getInstance().copyToClipboard(
                new ClipboardContent().setData(ClipboardContent.MIME_TEXT, "cn1-clip-text"));
        ClipboardContent pasted = paste();
        assertNotNull(pasted, "text paste returned no ClipboardContent");
        assertEqual("cn1-clip-text", pasted.getText(ClipboardContent.MIME_TEXT),
                "plain text did not survive the clipboard round trip");
    }

    private void roundTripHtml() {
        Display.getInstance().copyToClipboard(new ClipboardContent()
                .setData(ClipboardContent.MIME_TEXT, "bold text")
                .setData(ClipboardContent.MIME_HTML, "<b>bold text</b>"));
        ClipboardContent pasted = paste();
        assertNotNull(pasted, "html paste returned no ClipboardContent");
        String html = pasted.getText(ClipboardContent.MIME_HTML);
        assertNotNull(html, "html representation missing after round trip");
        assertBool(html.indexOf("bold text") >= 0, "html content lost its text after round trip");
    }

    private void roundTripImage() {
        byte[] png = Base64.decode(PNG_1X1_BASE64.getBytes());
        Display.getInstance().copyToClipboard(
                new ClipboardContent().setData(ClipboardContent.MIME_PNG, png));
        ClipboardContent pasted = paste();
        assertNotNull(pasted, "image paste returned no ClipboardContent");
        byte[] out = pasted.getBytes(ClipboardContent.MIME_PNG);
        assertNotNull(out, "image bytes missing after round trip");
        assertBool(out.length > 0, "image bytes were empty after round trip");
        // The bytes may be re-encoded by the native clipboard, so assert they still decode to an image.
        Image decoded = Image.createImage(out, 0, out.length);
        assertNotNull(decoded, "pasted image bytes did not decode to an image");
        assertBool(decoded.getWidth() > 0 && decoded.getHeight() > 0,
                "decoded clipboard image has no dimensions");
    }

    private void roundTripFile() {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String path = fs.getAppHomePath();
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        path = path + "cn1-clip-roundtrip.txt";
        try {
            OutputStream os = fs.openOutputStream(path);
            os.write("clipboard file payload".getBytes());
            os.close();
        } catch (Throwable t) {
            fail("could not create the file used for the clipboard file round trip: " + t);
            return;
        }
        Display.getInstance().copyToClipboard(
                new ClipboardContent().setData(ClipboardContent.MIME_FILE, path));
        ClipboardContent pasted = paste();
        assertNotNull(pasted, "file paste returned no ClipboardContent");
        Object files = pasted.getData(ClipboardContent.MIME_FILE);
        assertNotNull(files, "file reference missing after round trip");
        String joined = files instanceof String[] ? join((String[]) files) : String.valueOf(files);
        assertBool(joined.indexOf("cn1-clip-roundtrip.txt") >= 0,
                "file reference lost its name after round trip (was " + joined + ")");
    }

    /** Reads the clipboard back as a {@link ClipboardContent}, wrapping a plain-text result. */
    private static ClipboardContent paste() {
        Object data = Display.getInstance().getPasteDataFromClipboard();
        if (data instanceof ClipboardContent) {
            return (ClipboardContent) data;
        }
        if (data instanceof String) {
            return new ClipboardContent().setData(ClipboardContent.MIME_TEXT, (String) data);
        }
        return null;
    }

    private static String join(String[] parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return false;
    }
}
