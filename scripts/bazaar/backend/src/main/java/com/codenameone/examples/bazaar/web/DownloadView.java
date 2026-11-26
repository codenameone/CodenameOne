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
package com.codenameone.examples.bazaar.web;

import com.codenameone.examples.bazaar.api.DownloadGrantDto;

import java.util.ArrayList;
import java.util.List;

/// One download of a paid order as its buyer sees it: the link while it
/// works, how much of it is used, and any licence keys.
public final class DownloadView {
    public String title = "";
    public String file = "";
    /// The signed link, or empty once it expired, ran out or was revoked.
    public String url = "";
    public int used;
    public int max;
    public String expires = "";
    public List<String> keys = new ArrayList<String>();

    /// The views of `grants`.
    public static List<DownloadView> of(List<DownloadGrantDto> grants) {
        List<DownloadView> out = new ArrayList<DownloadView>();
        for (int iter = 0; iter < grants.size(); iter++) {
            DownloadGrantDto g = grants.get(iter);
            DownloadView v = new DownloadView();
            v.title = g.title();
            v.file = g.fileName();
            v.url = g.url();
            v.used = g.downloads();
            v.max = g.maxDownloads();
            v.expires = com.codenameone.examples.bazaar.Days.iso(g.expiresAt());
            v.keys = g.licenseKeys();
            out.add(v);
        }
        return out;
    }
}
