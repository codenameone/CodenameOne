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

import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.ExceptionHandler;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.RequestParam;
import com.codename1.backend.mvc.Model;
import com.codenameone.examples.bazaar.content.Lists;
import com.codenameone.examples.bazaar.content.Pages;
import com.codenameone.examples.bazaar.content.RichText;
import com.codenameone.examples.bazaar.domain.BlogPost;
import com.codenameone.examples.bazaar.domain.ContentPage;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.market.Markets;
import com.codenameone.examples.bazaar.market.Translations;
import com.codenameone.examples.bazaar.store.Stores;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The online store's own pages on the storefront: pages, policies, the
/// blog and its posts. Public and cached like the rest of the shop; their
/// words follow the shopper's market language through `Translations`.
@Controller
public class StorefrontContent {
    private final Stores stores;
    private final Pages pages;
    private final StorefrontPages shop;
    private final Markets markets;
    private final Translations translations;
    private final SiteUrls urls;

    public StorefrontContent(Stores stores, Pages pages, StorefrontPages shop, Markets markets,
            Translations translations, SiteUrls urls) {
        this.stores = stores;
        this.pages = pages;
        this.shop = shop;
        this.markets = markets;
        this.translations = translations;
        this.urls = urls;
    }

    @GetMapping("/s/{handle}/pages/{page}")
    public String page(@PathVariable("handle") String handle, @PathVariable("page") String page,
            Model model, HttpServer.Request request) throws IOException {
        Store store = stores.activeStore(handle);
        ContentPage c = pages.published(store.getId(), Pages.PAGE, page);
        show(store, c, "/s/" + store.getHandle() + "/pages/" + c.getHandle(), model, request);
        return "content";
    }

    @GetMapping("/s/{handle}/policies/{policy}")
    public String policy(@PathVariable("handle") String handle,
            @PathVariable("policy") String policy, Model model, HttpServer.Request request)
            throws IOException {
        Store store = stores.activeStore(handle);
        ContentPage c = pages.published(store.getId(), Pages.POLICY, policy);
        show(store, c, "/s/" + store.getHandle() + "/policies/" + c.getHandle(), model, request);
        return "content";
    }

    @GetMapping("/s/{handle}/blog")
    public String blog(@PathVariable("handle") String handle,
            @RequestParam(value = "tag", required = false) String tag, Model model,
            HttpServer.Request request) throws IOException {
        Store store = stores.activeStore(handle);
        String path = "/s/" + store.getHandle() + "/blog";
        PageMeta meta = shop.meta(store, request, path);
        String t = tag == null ? "" : Lists.cut(tag, 40);
        meta.title = (t.length() > 0 ? t + " - " : "") + "Blog - " + store.getName();
        StorefrontMarkets.Choice market = StorefrontMarkets.choose(store, path, request, markets,
                translations, urls);
        List<PostView> out = posts(store, pages.publishedPosts(store.getId(), t), market.locale,
                100);
        model.addAttribute("page", meta).addAttribute("posts", out)
                .addAttribute("tag", t)
                .addAttribute("feedUrl", "/s/" + store.getHandle() + "/feeds/blog.atom");
        return "blog";
    }

    @GetMapping("/s/{handle}/blog/{post}")
    public String post(@PathVariable("handle") String handle, @PathVariable("post") String post,
            Model model, HttpServer.Request request) throws IOException {
        Store store = stores.activeStore(handle);
        BlogPost p = pages.publishedPost(store.getId(), post);
        String path = "/s/" + store.getHandle() + "/blog/" + p.getHandle();
        PageMeta meta = shop.meta(store, request, path);
        meta.type = "article";
        StorefrontMarkets.Choice market = StorefrontMarkets.choose(store, path, request, markets,
                translations, urls);
        Map<String, String> tr = translations.fields(store.getId(), Translations.POST, p.getId(),
                market.locale);
        ContentView view = new ContentView();
        view.post = true;
        view.title = StorefrontMarkets.text(tr, "title", p.getTitle());
        view.author = p.getAuthor();
        view.date = StorefrontPages.day(p.getPublishedAt());
        view.blogUrl = "/s/" + store.getHandle() + "/blog";
        List<String> tags = Lists.split(p.getTags());
        for (int iter = 0; iter < tags.size(); iter++) {
            view.tags.add(new LinkView(tags.get(iter), view.blogUrl + "?tag="
                    + SiteUrls.encode(tags.get(iter)), false));
        }
        view.blocks.addAll(ContentView.blocks(StorefrontMarkets.text(tr, "body", p.getBody())));
        meta.title = view.title + " - " + store.getName();
        meta.description = StorefrontPages.describe(StorefrontMarkets.text(tr, "excerpt",
                p.getExcerpt()));
        model.addAttribute("page", meta).addAttribute("view", view);
        return "content";
    }

    private void show(Store store, ContentPage c, String path, Model model,
            HttpServer.Request request) throws IOException {
        PageMeta meta = shop.meta(store, request, path);
        StorefrontMarkets.Choice market = StorefrontMarkets.choose(store, path, request, markets,
                translations, urls);
        Map<String, String> tr = translations.fields(store.getId(), Translations.CONTENT,
                c.getId(), market.locale);
        ContentView view = new ContentView();
        view.title = StorefrontMarkets.text(tr, "title", c.getTitle());
        String body = StorefrontMarkets.text(tr, "body", c.getBody());
        view.blocks.addAll(ContentView.blocks(body));
        meta.title = view.title + " - " + store.getName();
        meta.description = StorefrontPages.describe(RichText.plainText(body));
        model.addAttribute("page", meta).addAttribute("view", view);
    }

    /// The posts as list entries, in the shopper's language.
    List<PostView> posts(Store store, List<BlogPost> rows, String locale, int max)
            throws IOException {
        List<PostView> out = new ArrayList<PostView>();
        for (int iter = 0; iter < rows.size() && iter < max; iter++) {
            BlogPost p = rows.get(iter);
            Map<String, String> tr = translations.fields(store.getId(), Translations.POST,
                    p.getId(), locale);
            PostView v = new PostView();
            v.title = StorefrontMarkets.text(tr, "title", p.getTitle());
            v.excerpt = StorefrontMarkets.text(tr, "excerpt", p.getExcerpt());
            v.author = p.getAuthor();
            v.date = StorefrontPages.day(p.getPublishedAt());
            v.url = "/s/" + store.getHandle() + "/blog/" + SiteUrls.encode(p.getHandle());
            out.add(v);
        }
        return out;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public HttpServer.Response refused(ResponseStatusException e) throws IOException {
        if (e.getStatus() == 404) {
            return NotFoundPage.response();
        }
        return HttpServer.Response.text(e.getStatus(), e.getReason() == null ? "" : e.getReason());
    }
}
