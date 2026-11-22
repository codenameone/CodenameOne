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
package com.codenameone.examples.bazaar.merchant;

import com.codename1.appkit.Net;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.Log;
import com.codename1.io.Util;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.util.Base64;
import com.codenameone.examples.bazaar.api.DigitalAssetDto;
import com.codenameone.examples.bazaar.api.DigitalUploadDto;
import com.codenameone.examples.bazaar.api.LicenseKeysDto;
import com.codenameone.examples.bazaar.api.VariantDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;

import java.io.IOException;
import java.io.InputStream;

/// The download a digital variant delivers, edited from the variant dialog:
/// the file buyers get, how many times and for how many days a link works,
/// and the licence keys a sale hands out one per unit.
final class DigitalFiles {
    private DigitalFiles() {
    }

    /// The button that opens the download dialog of a saved digital variant.
    static Button button(final VariantDto v) {
        return Ui.secondary(Lang.tr("Download file"), "variant-download", e -> open(v));
    }

    private static void open(final VariantDto v) {
        Api.digital().download(MerchantState.storeId(), v.id(), Net.to((DigitalAssetDto a) -> show(v, a), Ui::fail));
    }

    private static void show(final VariantDto v, DigitalAssetDto a) {
        Container body = new Container(BoxLayout.y());
        final Label file = Ui.label(a.name().length() == 0 ? Lang.tr("No file yet")
                : a.name() + " (" + (a.sizeBytes() / 1024) + " KB)", "SvMuted");
        file.setName("download-file");
        body.add(file);
        final String[] chosen = new String[1];
        Button choose = Ui.secondary(Lang.tr("Choose file"), "download-choose",
                e -> CN.openGallery(picked -> {
                    if (picked != null && picked.getSource() instanceof String) {
                        chosen[0] = (String) picked.getSource();
                        file.setText(name(chosen[0]));
                        file.getParent().revalidate();
                    }
                }, Display.GALLERY_ALL));
        body.add(choose);
        body.add(Ui.label(Lang.tr("Downloads per sale"), "SvLabel"));
        final TextField max = new TextField(Integer.toString(a.maxDownloads()), "", 6,
                TextArea.NUMERIC);
        max.setUIID("SvField");
        max.setName("download-max");
        body.add(max);
        body.add(Ui.label(Lang.tr("Days a link lasts"), "SvLabel"));
        final TextField days = new TextField(Integer.toString(a.expiryDays()), "", 6,
                TextArea.NUMERIC);
        days.setUIID("SvField");
        days.setName("download-days");
        body.add(days);
        body.add(Ui.label(Lang.tr("Licence keys, one per line"), "SvLabel"));
        body.add(Ui.label(Lang.tr("{0} free, {1} assigned", Integer.toString(a.keysFree()),
                Integer.toString(a.keysAssigned())), "SvMuted"));
        final TextArea keys = new TextArea(4, 30);
        keys.setUIID("SvField");
        keys.setName("download-keys");
        body.add(keys);
        Command save = Command.create(Lang.tr("Save download"), null, null);
        Command cancel = Command.create(Lang.tr("Cancel"), null, null);
        if (Dialog.show(v.title(), body, cancel, save) != save) {
            return;
        }
        String data = "";
        String name = "";
        if (chosen[0] != null) {
            byte[] bytes = read(chosen[0]);
            if (bytes == null) {
                return;
            }
            data = Base64.encodeNoNewline(bytes);
            name = name(chosen[0]);
        }
        final String pasted = keys.getText().trim();
        Api.digital().setDownload(MerchantState.storeId(), v.id(), new DigitalUploadDto(name,
                "application/octet-stream", data, number(max.getText()), number(days.getText())),
                Net.to(saved -> {
                    if (pasted.length() == 0) {
                        Ui.say(Lang.tr("Download saved"));
                        return;
                    }
                    Api.digital().addLicenseKeys(MerchantState.storeId(), v.id(),
                            new LicenseKeysDto(pasted), Net.to(
                                k -> Ui.say(Lang.tr("Download saved")), Ui::fail));
                }, Ui::fail));
    }

    /// The bytes of a picked file, or null after saying why.
    static byte[] read(String path) {
        InputStream in = null;
        try {
            in = FileSystemStorage.getInstance().openInputStream(path);
            return Util.readInputStream(in);
        } catch (IOException failed) {
            Log.e(failed);
            Ui.say(Lang.tr("That file could not be read"));
            return null;
        } finally {
            Util.cleanup(in);
        }
    }

    /// The last path segment of a file path or URL.
    static String name(String path) {
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private static int number(String text) {
        int v = 0;
        String t = text == null ? "" : text.trim();
        for (int iter = 0; iter < t.length() && iter < 6; iter++) {
            char c = t.charAt(iter);
            if (c < '0' || c > '9') {
                return 0;
            }
            v = v * 10 + (c - '0');
        }
        return v;
    }
}
