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
package com.codenameone.examples.reel.catalog;

import com.codename1.backend.annotations.Component;
import com.codenameone.examples.reel.Caller;
import com.codenameone.examples.reel.api.CatalogApiServer;
import com.codenameone.examples.reel.api.GenreDto;
import com.codenameone.examples.reel.api.HomeDto;
import com.codenameone.examples.reel.api.ReportDto;
import com.codenameone.examples.reel.api.SearchResultDto;
import com.codenameone.examples.reel.api.TitleCardDto;
import com.codenameone.examples.reel.api.TitleDto;
import com.codenameone.examples.reel.moderation.Reviews;

import java.util.List;

@Component
public class CatalogEndpoint implements CatalogApiServer {
    private final Catalog catalog;
    private final Reviews reviews;

    public CatalogEndpoint(Catalog catalog, Reviews reviews) {
        this.catalog = catalog;
        this.reviews = reviews;
    }

    @Override
    public HomeDto home(String profile) throws Exception {
        return catalog.home(Caller.name(), profile);
    }

    @Override
    public TitleDto title(String id, String profile) throws Exception {
        return catalog.title(Caller.name(), id, profile);
    }

    @Override
    public List<GenreDto> genres() throws Exception {
        Caller.name();
        return catalog.genres();
    }

    @Override
    public List<TitleCardDto> genre(String id, String profile) throws Exception {
        return catalog.genre(Caller.name(), id, profile);
    }

    @Override
    public SearchResultDto search(String q, String profile) throws Exception {
        return catalog.search(Caller.name(), q, profile);
    }

    @Override
    public String report(String id, ReportDto report) throws Exception {
        reviews.report(Caller.name(), id, report);
        return "ok";
    }
}
