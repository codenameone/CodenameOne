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
package com.codenameone.examples.reel.viewer;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.NetworkManager;
import com.codename1.io.Preferences;
import com.codenameone.examples.reel.api.DownloadDto;
import com.codenameone.examples.reel.ui.Art;
import com.codenameone.examples.reel.ui.Ui;

/// Offline copies. The server licenses a download to this device and hands a
/// media URL for the single encrypted file (`cenc.mp4`) and a license token;
/// the file is stored here still encrypted, and playing it asks the license
/// server for the key every time -- so a download stops playing when the
/// license runs out, the device is signed out or the account is suspended.
public final class Downloads {
    private Downloads() {
    }

    /// Where the file of a download is kept.
    public static String path(String downloadId) {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        String dir = fs.getAppHomePath() + "downloads";
        if (!fs.exists(dir)) {
            fs.mkdir(dir);
        }
        return dir + "/" + downloadId + ".mp4";
    }

    public static boolean have(String downloadId) {
        return FileSystemStorage.getInstance().exists(path(downloadId))
                && Preferences.get("reel.dl.done." + downloadId, false);
    }

    /// The license token of a download, kept from when it was licensed.
    public static String token(String downloadId) {
        return Preferences.get("reel.dl.token." + downloadId, "");
    }

    /// Fetches the file in the background.
    public static void fetch(final DownloadDto d) {
        Preferences.set("reel.dl.token." + d.id(), d.licenseToken());
        Preferences.set("reel.dl.done." + d.id(), false);
        ConnectionRequest r = new ConnectionRequest(Art.url(d.manifestUrl()), false) {
            @Override
            protected void postResponse() {
                if (getResponseCode() == 200) {
                    Preferences.set("reel.dl.done." + d.id(), true);
                    Ui.say("Download finished");
                }
            }
        };
        r.setDestinationFile(path(d.id()));
        r.setFailSilently(true);
        NetworkManager.getInstance().addToQueue(r);
    }

    public static void forget(String downloadId) {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        if (fs.exists(path(downloadId))) {
            fs.delete(path(downloadId));
        }
        Preferences.delete("reel.dl.token." + downloadId);
        Preferences.delete("reel.dl.done." + downloadId);
    }
}
