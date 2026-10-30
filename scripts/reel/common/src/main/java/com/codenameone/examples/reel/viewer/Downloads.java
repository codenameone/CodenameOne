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
import java.util.ArrayList;
import java.util.List;

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

    /// Fetches the file in the background. What the library needs to list and
    /// play it is kept with it, so the downloads show with no network -- the
    /// one time a viewer is sure to want them.
    public static void fetch(final DownloadDto d) {
        Preferences.set("reel.dl.token." + d.id(), d.licenseToken());
        Preferences.set("reel.dl.done." + d.id(), false);
        Preferences.set("reel.dl.name." + d.id(), d.titleName() == null ? "" : d.titleName());
        Preferences.set("reel.dl.title." + d.id(), d.titleId() == null ? "" : d.titleId());
        Preferences.set("reel.dl.until." + d.id(), d.licenseExpiresAt());
        remember(d.id());
        ConnectionRequest r = new ConnectionRequest(Art.url(d.manifestUrl()), false) {
            @Override
            protected void postResponse() {
                if (getResponseCode() == 200) {
                    Preferences.set("reel.dl.done." + d.id(), true);
                    Ui.say("Download finished");
                } else {
                    failed(d);
                }
            }

            @Override
            protected void handleException(Exception err) {
                failed(d);
            }

            @Override
            protected void handleErrorResponseCode(int code, String message) {
                failed(d);
            }
        };
        r.setDestinationFile(path(d.id()));
        r.setFailSilently(true);
        NetworkManager.getInstance().addToQueue(r);
    }

    /// A download that did not finish leaves no partial file and no entry
    /// that would read "Downloading" forever.
    private static void failed(final DownloadDto d) {
        com.codename1.ui.CN.callSerially(() -> {
            if (!ids().contains(d.id())) {
                return;
            }
            forget(d.id());
            Ui.say("The download did not finish. Try again when you are online.");
        });
    }

    /// The downloads kept on this device, newest first, as the library lists them.
    public static List<DownloadDto> list() {
        List<DownloadDto> out = new ArrayList<DownloadDto>();
        List<String> ids = ids();
        for (int iter = ids.size() - 1; iter >= 0; iter--) {
            String id = ids.get(iter);
            out.add(new DownloadDto(id, Preferences.get("reel.dl.title." + id, ""),
                    Preferences.get("reel.dl.name." + id, ""), null, null, "CLEARKEY", token(id),
                    Preferences.get("reel.dl.until." + id, 0L), 0L, null));
        }
        return out;
    }

    public static void forget(String downloadId) {
        FileSystemStorage fs = FileSystemStorage.getInstance();
        if (fs.exists(path(downloadId))) {
            fs.delete(path(downloadId));
        }
        Preferences.delete("reel.dl.token." + downloadId);
        Preferences.delete("reel.dl.done." + downloadId);
        Preferences.delete("reel.dl.name." + downloadId);
        Preferences.delete("reel.dl.title." + downloadId);
        Preferences.delete("reel.dl.until." + downloadId);
        List<String> ids = ids();
        ids.remove(downloadId);
        store(ids);
    }

    /// Every download on this device, for signing out and changing servers:
    /// the files are licensed to the account that leaves.
    public static void forgetAll() {
        List<String> ids = ids();
        for (int iter = 0; iter < ids.size(); iter++) {
            forget(ids.get(iter));
        }
    }

    private static void remember(String id) {
        List<String> ids = ids();
        if (!ids.contains(id)) {
            ids.add(id);
            store(ids);
        }
    }

    private static List<String> ids() {
        List<String> out = new ArrayList<String>();
        String all = Preferences.get("reel.dl.ids", "");
        int from = 0;
        while (from < all.length()) {
            int comma = all.indexOf(',', from);
            if (comma < 0) {
                comma = all.length();
            }
            if (comma > from) {
                out.add(all.substring(from, comma));
            }
            from = comma + 1;
        }
        return out;
    }

    private static void store(List<String> ids) {
        StringBuilder sb = new StringBuilder();
        for (int iter = 0; iter < ids.size(); iter++) {
            if (iter > 0) {
                sb.append(',');
            }
            sb.append(ids.get(iter));
        }
        Preferences.set("reel.dl.ids", sb.toString());
    }
}
