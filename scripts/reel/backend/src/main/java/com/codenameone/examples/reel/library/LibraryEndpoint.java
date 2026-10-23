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
package com.codenameone.examples.reel.library;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.reel.Caller;
import com.codenameone.examples.reel.api.DownloadDto;
import com.codenameone.examples.reel.api.HistoryEntryDto;
import com.codenameone.examples.reel.api.LibraryApiServer;
import com.codenameone.examples.reel.api.RatingDto;
import com.codenameone.examples.reel.api.TitleCardDto;

import java.util.List;

@Component
public class LibraryEndpoint implements LibraryApiServer {
    private final Library library;

    public LibraryEndpoint(Library library) {
        this.library = library;
    }

    @Override
    public List<TitleCardDto> myList(String profile) throws Exception {
        return library.myList(Caller.name(), profile);
    }

    @Override
    public String addToList(String titleId, String profile) throws Exception {
        library.add(Caller.name(), titleId, profile);
        return "ok";
    }

    @Override
    public String removeFromList(String titleId, String profile) throws Exception {
        library.remove(Caller.name(), titleId, profile);
        return "ok";
    }

    @Override
    public String rate(String titleId, String profile, RatingDto rating) throws Exception {
        library.rate(Caller.name(), titleId, profile, rating == null ? 0 : rating.stars());
        return "ok";
    }

    @Override
    public List<HistoryEntryDto> history(String profile) throws Exception {
        return library.history(Caller.name(), profile);
    }

    @Override
    public String forget(String titleId, String profile) throws Exception {
        library.forget(Caller.name(), titleId, profile);
        return "ok";
    }

    @Override
    public List<DownloadDto> downloads(String device) throws Exception {
        return library.downloads(Caller.name(), device);
    }

    @Override
    public DownloadDto download(String titleId, String device, String episode, String profile) throws Exception {
        return library.download(Caller.name(), titleId, device, episode, profile);
    }

    @Override
    public String deleteDownload(String id) throws Exception {
        library.delete(Caller.name(), id);
        return "ok";
    }
}
