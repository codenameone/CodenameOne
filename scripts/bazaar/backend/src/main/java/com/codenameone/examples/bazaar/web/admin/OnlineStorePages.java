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
package com.codenameone.examples.bazaar.web.admin;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.HttpServer;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Controller;
import com.codename1.backend.annotations.GetMapping;
import com.codename1.backend.annotations.PathVariable;
import com.codename1.backend.annotations.PostMapping;
import com.codename1.backend.mvc.Model;
import com.codename1.backend.security.SecurityExchange;
import com.codenameone.examples.bazaar.api.BlogPostDto;
import com.codenameone.examples.bazaar.api.ContentPageDto;
import com.codenameone.examples.bazaar.api.MenuLinkDto;
import com.codenameone.examples.bazaar.api.MetafieldDefDto;
import com.codenameone.examples.bazaar.api.MetaobjectEntryDto;
import com.codenameone.examples.bazaar.api.MetaobjectTypeDto;
import com.codenameone.examples.bazaar.api.RedirectDto;
import com.codenameone.examples.bazaar.api.ThemeDto;
import com.codenameone.examples.bazaar.content.ContentEndpoint;
import com.codenameone.examples.bazaar.content.Lists;
import com.codenameone.examples.bazaar.content.Metafields;
import com.codenameone.examples.bazaar.content.Navigation;
import com.codenameone.examples.bazaar.content.Pages;
import com.codenameone.examples.bazaar.content.Themes;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.web.Shell;
import com.codenameone.examples.bazaar.web.SiteUrls;
import com.codenameone.examples.bazaar.web.WebPages;
import com.codenameone.examples.bazaar.web.account.WebSessions;
import com.codenameone.examples.bazaar.web.security.PageEtags;
import com.codenameone.examples.bazaar.web.table.DataTable;
import java.util.ArrayList;
import java.util.List;

/// The back office's online store: theme (with a signed preview link),
/// pages and policies, blog, menus, redirects, metafields and metaobjects,
/// drawn by `templates/marketing.html` like [GlobalPages]. Every change is a
/// form post answered with 303; the services ([ContentEndpoint]) check the
/// store and the role, so another store is a 404 and a wrong role a 403.
@Controller
public class OnlineStorePages {
    private final WebPages pages;
    private final ContentEndpoint content;
    private final Themes themes;
    private final StoreRepository stores;
    private final WebSessions sessions;

    public OnlineStorePages(WebPages pages, ContentEndpoint content, Themes themes,
            StoreRepository stores, WebSessions sessions) {
        this.pages = pages;
        this.content = content;
        this.themes = themes;
        this.stores = stores;
        this.sessions = sessions;
    }

    private static String base(String storeId) {
        return "/admin/" + storeId + "/online-store";
    }

    private String render(Model model, HttpServer.Request request, String storeId, String title,
            MarketingView v) throws Exception {
        Shell shell = pages.admin(request, storeId, title);
        SecurityExchange.current().setAttribute(PageEtags.NO_STORE, "1");
        v.base = "/admin/" + storeId;
        if (v.heading.length() == 0) {
            v.heading = title;
        }
        String b = base(storeId);
        String[][] subs = {{"Theme", ""}, {"Pages", "/pages"}, {"Blog", "/blog"},
            {"Menus", "/menus"}, {"Redirects", "/redirects"}, {"Metafields", "/metafields"},
            {"Metaobjects", "/metaobjects"}};
        for (int iter = 0; iter < subs.length; iter++) {
            AdminRow r = new AdminRow();
            r.title = subs[iter][0];
            r.url = b + subs[iter][1];
            v.links.add(r);
        }
        model.addAttribute("view", v);
        model.addAttribute("table", new DataTable());
        return WebPages.view(model, shell, "marketing");
    }

    private static MarketingView view(String page) {
        MarketingView v = new MarketingView();
        v.page = page;
        return v;
    }

    private interface Change {
        void run() throws Exception;
    }

    private String change(HttpServer.Request request, String back, String done, Change c)
            throws Exception {
        sessions.touch(request, Caller.name());
        try {
            c.run();
            WebPages.toast(request, done);
        } catch (ResponseStatusException e) {
            int s = e.getStatus();
            if (s == 401 || s == 403 || s == 404) {
                throw e;
            }
            WebPages.toast(request, e.getReason() == null ? "That did not work" : e.getReason());
        }
        return "redirect:" + back;
    }

    private static String p(HttpServer.Request request, String name) {
        String v;
        try {
            v = request.param(name);
        } catch (RuntimeException malformed) {
            v = null;
        }
        return v == null ? "" : v.trim();
    }

    private static boolean on(HttpServer.Request request, String name) {
        String v = p(request, name);
        return v.length() > 0 && !"false".equals(v) && !"0".equals(v);
    }

    private static ActionCard card(String title, String detail) {
        ActionCard k = new ActionCard();
        k.title = title;
        k.detail = detail;
        return k;
    }

    private static AdminRow button(String title, String action, boolean danger) {
        AdminRow r = new AdminRow();
        r.title = title;
        r.action = action;
        r.flag = danger;
        return r;
    }

    private static FormField select(FormView f, String name, String label, String[] values,
            String current) {
        FormField field = f.add(name, label, "select", current);
        for (int iter = 0; iter < values.length; iter++) {
            field.option(values[iter], values[iter]);
        }
        return field;
    }

    // ================================================================ theme

    @GetMapping("/admin/{storeId}/online-store")
    public String theme(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("theme");
        pages.admin(request, storeId, "Online store");
        ThemeDto draft = content.theme(storeId, Themes.DRAFT);
        ThemeDto live = content.theme(storeId, Themes.LIVE);
        Store store = stores.findById(storeId);
        v.intro = "Change the draft, look at it with the preview link, then publish it. "
                + "Shoppers see the published theme only.";
        AdminRow fact = new AdminRow();
        fact.title = "Published";
        fact.detail = live.palette() + ", " + live.font() + ", " + live.radius() + " corners";
        v.facts.add(fact);
        String b = base(storeId);
        FormView f = new FormView("theme", "Draft theme", b + "/theme", "Save draft");
        f.add("accent", "Accent colour", "text", draft.accent()).hint("#rrggbb");
        select(f, "palette", "Palette", Themes.PALETTES, draft.palette());
        select(f, "font", "Font", Themes.FONTS, draft.font());
        select(f, "radius", "Corners", Themes.RADII, draft.radius());
        f.add("logoPhotoId", "Logo photo id", "text", draft.logoPhotoId())
                .hint("A photo uploaded to one of the store's products");
        f.add("sections", "Home sections, in order", "text", Lists.join(draft.sections()))
                .hint("Any of: hero, collections, featured, blog, about");
        v.forms.add(f);
        FormView pub = new FormView("publish", "Publish the draft", b + "/theme/publish",
                "Publish");
        pub.text = "Replaces the theme shoppers see.";
        v.forms.add(pub);
        if (store != null) {
            FormView sf = new FormView("search", "Search and launch", b + "/search", "Save");
            sf.add("synonyms", "Synonym groups", "textarea", store.getSearchSynonyms())
                    .hint("One group a line, words split by commas: sofa, couch, settee");
            sf.add("boosts", "Search boosts", "textarea", site.boostText(storeId))
                    .hint("product-handle=N, one a line, -100 to 100; higher ranks first");
            sf.add("password", "Storefront password", "text", store.getStorefrontPassword())
                    .hint("Empty opens the store; staff always get in");
            sf.add("domain", "Custom domain", "text",
                    store.getCustomDomain() == null ? "" : store.getCustomDomain())
                    .hint("shop.example.com; point its DNS and TLS certificate at this "
                            + "server first. Empty serves only under /s/" + store.getHandle());
            v.forms.add(sf);
            ActionCard embed = card("Buy button", "Paste this on another site to sell a "
                    + "product there; replace the handle with the product's.");
            embed.url = "/s/" + store.getHandle() + "/embed/PRODUCT-HANDLE";
            embed.extraUrl = embed.url;
            embed.extraLabel = com.codenameone.examples.bazaar.web.StoreSite.embedCode("",
                    store.getHandle(), "PRODUCT-HANDLE");
            v.cards.add(embed);
            String token = themes.previewToken(Caller.name(), storeId);
            ActionCard k = card("Preview the draft", "The link works for an hour and is "
                    + "never cached.");
            k.url = "/s/" + store.getHandle() + "?preview=" + SiteUrls.encode(token);
            k.extraUrl = k.url;
            k.extraLabel = "Open preview";
            v.cardsTitle = "Preview";
            v.cards.add(k);
        }
        return render(model, request, storeId, "Online store", v);
    }

    @PostMapping("/admin/{storeId}/online-store/theme")
    public String saveTheme(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        return change(request, base(storeId), "Draft saved", () -> content.saveTheme(storeId,
                new ThemeDto(Themes.DRAFT, p(request, "accent"), p(request, "palette"),
                    p(request, "font"), p(request, "radius"), p(request, "logoPhotoId"),
                    Lists.split(p(request, "sections")))));
    }

    @com.codename1.backend.annotations.Autowired
    private com.codenameone.examples.bazaar.web.StoreSite site;

    @PostMapping("/admin/{storeId}/online-store/search")
    public String saveSearch(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        return change(request, base(storeId), "Search settings saved", () -> site.saveSettings(
                storeId, p(request, "synonyms"), p(request, "password"), p(request, "boosts"),
                p(request, "domain")));
    }

    @PostMapping("/admin/{storeId}/online-store/theme/publish")
    public String publishTheme(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        return change(request, base(storeId), "Theme published",
                () -> content.publishTheme(storeId));
    }

    // ================================================================ pages

    @GetMapping("/admin/{storeId}/online-store/pages")
    public String pageList(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("pages");
        pages.admin(request, storeId, "Pages");
        v.intro = "Pages and the store's policies. Text is kept as simple rich text: "
                + "paragraphs, headings, lists, bold, italics and links.";
        String b = base(storeId) + "/pages";
        List<ContentPageDto> all = content.contentPages(storeId);
        v.cardsTitle = "Pages and policies";
        for (int iter = 0; iter < all.size(); iter++) {
            ContentPageDto d = all.get(iter);
            ActionCard k = card(d.title(), d.kind() + " /" + d.handle());
            k.status = d.published() ? "Published" : "Hidden";
            k.url = b + "/" + d.id();
            k.buttons.add(button("Remove", b + "/" + d.id() + "/remove", true));
            v.cards.add(k);
        }
        v.forms.add(pageForm(b, "new-page", "Add a page", "Add page", null));
        return render(model, request, storeId, "Pages", v);
    }

    private static FormView pageForm(String action, String id, String title, String submit,
            ContentPageDto d) {
        FormView f = new FormView(id, title, action, submit);
        String[] kinds = {Pages.PAGE, Pages.POLICY};
        select(f, "kind", "Kind", kinds, d == null ? Pages.PAGE : d.kind());
        f.add("handle", "Handle", "text", d == null ? "" : d.handle())
                .hint("Policies: refund, privacy, terms or shipping");
        f.add("title", "Title", "text", d == null ? "" : d.title()).required = true;
        f.add("body", "Body (HTML: p, h2, h3, ul, ol, li, b, i, a)", "textarea",
                d == null ? "" : d.body());
        f.check("published", "Published", d != null && d.published());
        return f;
    }

    private static ContentPageDto pageDto(HttpServer.Request request) {
        return new ContentPageDto(null, p(request, "kind"), p(request, "handle"),
                p(request, "title"), p(request, "body"), on(request, "published"), 0);
    }

    @PostMapping("/admin/{storeId}/online-store/pages")
    public String addPage(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        return change(request, base(storeId) + "/pages", "Page added",
                () -> content.createContentPage(storeId, pageDto(request)));
    }

    @GetMapping("/admin/{storeId}/online-store/pages/{id}")
    public String page(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            Model model, HttpServer.Request request) throws Exception {
        MarketingView v = view("page");
        pages.admin(request, storeId, "Page");
        ContentPageDto d = null;
        List<ContentPageDto> all = content.contentPages(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).id().equals(id)) {
                d = all.get(iter);
            }
        }
        if (d == null) {
            throw new ResponseStatusException(404, "No such page");
        }
        v.back = base(storeId) + "/pages";
        v.backLabel = "All pages";
        v.forms.add(pageForm(v.back + "/" + id, "page", "Edit page", "Save page", d));
        return render(model, request, storeId, d.title(), v);
    }

    @PostMapping("/admin/{storeId}/online-store/pages/{id}")
    public String savePage(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        return change(request, base(storeId) + "/pages/" + id, "Page saved",
                () -> content.saveContentPage(storeId, id, pageDto(request)));
    }

    @PostMapping("/admin/{storeId}/online-store/pages/{id}/remove")
    public String removePage(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        return change(request, base(storeId) + "/pages", "Page removed",
                () -> content.removeContentPage(storeId, id));
    }

    // ================================================================= blog

    @GetMapping("/admin/{storeId}/online-store/blog")
    public String blog(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("blog");
        pages.admin(request, storeId, "Blog");
        String b = base(storeId) + "/blog";
        List<BlogPostDto> all = content.blogPosts(storeId);
        v.cardsTitle = "Posts";
        for (int iter = 0; iter < all.size(); iter++) {
            BlogPostDto d = all.get(iter);
            ActionCard k = card(d.title(), d.author() + " - " + Lists.join(d.tags()));
            k.status = d.published() ? "Published" : "Draft";
            k.url = b + "/" + d.id();
            k.buttons.add(button("Remove", b + "/" + d.id() + "/remove", true));
            v.cards.add(k);
        }
        v.forms.add(postForm(b, "new-post", "Write a post", "Add post", null));
        return render(model, request, storeId, "Blog", v);
    }

    private static FormView postForm(String action, String id, String title, String submit,
            BlogPostDto d) {
        FormView f = new FormView(id, title, action, submit);
        f.add("title", "Title", "text", d == null ? "" : d.title()).required = true;
        f.add("handle", "Handle", "text", d == null ? "" : d.handle());
        f.add("author", "Author", "text", d == null ? "" : d.author());
        f.add("tags", "Tags", "text", d == null ? "" : Lists.join(d.tags()))
                .hint("Separated by commas");
        f.add("excerpt", "Excerpt", "textarea", d == null ? "" : d.excerpt());
        f.add("body", "Body (HTML: p, h2, h3, ul, ol, li, b, i, a)", "textarea",
                d == null ? "" : d.body());
        f.check("published", "Published", d != null && d.published());
        return f;
    }

    private static BlogPostDto postDto(HttpServer.Request request) {
        return new BlogPostDto(null, p(request, "handle"), p(request, "title"),
                p(request, "author"), Lists.split(p(request, "tags")), p(request, "excerpt"),
                p(request, "body"), on(request, "published"), 0);
    }

    @PostMapping("/admin/{storeId}/online-store/blog")
    public String addPost(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        return change(request, base(storeId) + "/blog", "Post added",
                () -> content.createBlogPost(storeId, postDto(request)));
    }

    @GetMapping("/admin/{storeId}/online-store/blog/{id}")
    public String post(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            Model model, HttpServer.Request request) throws Exception {
        MarketingView v = view("post");
        pages.admin(request, storeId, "Post");
        BlogPostDto d = null;
        List<BlogPostDto> all = content.blogPosts(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).id().equals(id)) {
                d = all.get(iter);
            }
        }
        if (d == null) {
            throw new ResponseStatusException(404, "No such post");
        }
        v.back = base(storeId) + "/blog";
        v.backLabel = "All posts";
        v.forms.add(postForm(v.back + "/" + id, "post", "Edit post", "Save post", d));
        return render(model, request, storeId, d.title(), v);
    }

    @PostMapping("/admin/{storeId}/online-store/blog/{id}")
    public String savePost(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        return change(request, base(storeId) + "/blog/" + id, "Post saved",
                () -> content.saveBlogPost(storeId, id, postDto(request)));
    }

    @PostMapping("/admin/{storeId}/online-store/blog/{id}/remove")
    public String removePost(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        return change(request, base(storeId) + "/blog", "Post removed",
                () -> content.removeBlogPost(storeId, id));
    }

    // ================================================================ menus

    @GetMapping("/admin/{storeId}/online-store/menus")
    public String menus(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("menus");
        pages.admin(request, storeId, "Menus");
        v.intro = "The header menu (two levels) and the footer links. Move a link up or "
                + "down among its siblings.";
        String b = base(storeId) + "/menus";
        List<MenuLinkDto> all = content.menuLinks(storeId);
        v.cardsTitle = "Links";
        FormView f = new FormView("new-link", "Add a link", b, "Add link");
        String[] menus = {Navigation.MAIN, Navigation.FOOTER};
        select(f, "menu", "Menu", menus, Navigation.MAIN);
        FormField parent = f.add("parentId", "Under", "select", "");
        parent.option("", "(top level)");
        for (int iter = 0; iter < all.size(); iter++) {
            MenuLinkDto d = all.get(iter);
            boolean child = d.parentId() != null && d.parentId().length() > 0;
            ActionCard k = card((child ? "-- " : "") + d.title(), d.url());
            k.meta = d.menu();
            k.extraUrl = "/admin/" + storeId + "/translations?kind=" + com.codenameone.examples.bazaar.market.Translations.MENU_LINK + "&refId=" + d.id();
            k.extraLabel = "Translate";
            k.buttons.add(button("Up", b + "/" + d.id() + "/up", false));
            k.buttons.add(button("Down", b + "/" + d.id() + "/down", false));
            k.buttons.add(button("Remove", b + "/" + d.id() + "/remove", true));
            v.cards.add(k);
            if (!child && Navigation.MAIN.equals(d.menu())) {
                parent.option(d.id(), d.title());
            }
        }
        f.add("title", "Label", "text", "").required = true;
        f.add("url", "Link", "text", "").hint("A path such as /s/shop/pages/about, or https://");
        v.forms.add(f);
        return render(model, request, storeId, "Menus", v);
    }

    @PostMapping("/admin/{storeId}/online-store/menus")
    public String addLink(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        return change(request, base(storeId) + "/menus", "Link added",
                () -> content.addMenuLink(storeId, new MenuLinkDto(null, p(request, "menu"),
                    p(request, "parentId"), p(request, "title"), p(request, "url"), 0)));
    }

    @PostMapping("/admin/{storeId}/online-store/menus/{id}/up")
    public String up(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            HttpServer.Request request) throws Exception {
        return change(request, base(storeId) + "/menus", "Link moved up",
                () -> content.moveMenuLink(storeId, id, "up"));
    }

    @PostMapping("/admin/{storeId}/online-store/menus/{id}/down")
    public String down(@PathVariable("storeId") String storeId, @PathVariable("id") String id,
            HttpServer.Request request) throws Exception {
        return change(request, base(storeId) + "/menus", "Link moved down",
                () -> content.moveMenuLink(storeId, id, "down"));
    }

    @PostMapping("/admin/{storeId}/online-store/menus/{id}/remove")
    public String removeLink(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        return change(request, base(storeId) + "/menus", "Link removed",
                () -> content.removeMenuLink(storeId, id));
    }

    // ============================================================ redirects

    @GetMapping("/admin/{storeId}/online-store/redirects")
    public String redirects(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("redirects");
        pages.admin(request, storeId, "Redirects");
        v.intro = "A storefront address that no longer exists answers 301 to the new one. "
                + "Only addresses that would otherwise be \"not found\" are redirected.";
        String b = base(storeId) + "/redirects";
        List<RedirectDto> all = content.redirects(storeId);
        v.cardsTitle = "Redirects";
        for (int iter = 0; iter < all.size(); iter++) {
            RedirectDto d = all.get(iter);
            ActionCard k = card(d.fromPath() + " -> " + d.toPath(), d.hits() + " visits");
            k.buttons.add(button("Remove", b + "/" + d.id() + "/remove", true));
            v.cards.add(k);
        }
        FormView f = new FormView("new-redirect", "Add a redirect", b, "Add redirect");
        f.add("fromPath", "Old path", "text", "").required = true;
        f.add("toPath", "New path", "text", "").required = true;
        v.forms.add(f);
        return render(model, request, storeId, "Redirects", v);
    }

    @PostMapping("/admin/{storeId}/online-store/redirects")
    public String addRedirect(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        return change(request, base(storeId) + "/redirects", "Redirect added",
                () -> content.addRedirect(storeId, new RedirectDto(null, p(request, "fromPath"),
                    p(request, "toPath"), 0)));
    }

    @PostMapping("/admin/{storeId}/online-store/redirects/{id}/remove")
    public String removeRedirect(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        return change(request, base(storeId) + "/redirects", "Redirect removed",
                () -> content.removeRedirect(storeId, id));
    }

    // =========================================================== metafields

    @GetMapping("/admin/{storeId}/online-store/metafields")
    public String metafields(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("metafields");
        pages.admin(request, storeId, "Metafields");
        v.intro = "Extra fields on products, variants, collections, customers and orders. "
                + "A product's storefront fields show in its specifications.";
        String b = base(storeId) + "/metafields";
        List<MetafieldDefDto> all = content.metafields(storeId);
        v.cardsTitle = "Fields";
        for (int iter = 0; iter < all.size(); iter++) {
            MetafieldDefDto d = all.get(iter);
            ActionCard k = card(d.name(), d.ownerKind() + "." + d.key() + " (" + d.type() + ")");
            k.status = d.storefront() ? "Storefront" : "";
            k.extraUrl = "/admin/" + storeId + "/translations?kind=" + com.codenameone.examples.bazaar.market.Translations.METAFIELD + "&refId=" + d.id();
            k.extraLabel = "Translate";
            k.buttons.add(button("Remove", b + "/" + d.id() + "/remove", true));
            v.cards.add(k);
        }
        FormView f = new FormView("new-field", "Define a field", b, "Add field");
        select(f, "ownerKind", "On", Metafields.OWNERS, Metafields.OWNERS[0]);
        f.add("key", "Key", "text", "").required = true;
        f.add("name", "Name", "text", "").required = true;
        select(f, "type", "Type", Metafields.TYPES, Metafields.TYPES[0]);
        FormField ref = f.add("refKind", "Refers to (reference fields)", "select", "");
        ref.option("", "(none)");
        List<MetaobjectTypeDto> types = content.metaobjectTypes(storeId);
        for (int iter = 0; iter < types.size(); iter++) {
            ref.option(types.get(iter).id(), types.get(iter).name());
        }
        f.check("storefront", "Show on the storefront", true);
        v.forms.add(f);
        FormView val = new FormView("value", "Set a value", b + "/values", "Save value");
        FormField def = val.add("defId", "Field", "select", "");
        for (int iter = 0; iter < all.size(); iter++) {
            def.option(all.get(iter).id(), all.get(iter).ownerKind() + "." + all.get(iter).key());
        }
        val.add("ownerId", "Product, variant, collection, customer or order id", "text", "")
                .required = true;
        val.add("value", "Value", "text", "").hint("Money as 12.50, dates as yyyy-mm-dd, "
                + "yes/no, or a metaobject entry id");
        v.forms.add(val);
        return render(model, request, storeId, "Metafields", v);
    }

    @PostMapping("/admin/{storeId}/online-store/metafields")
    public String addField(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        return change(request, base(storeId) + "/metafields", "Field added",
                () -> content.addMetafield(storeId, new MetafieldDefDto(null,
                    p(request, "ownerKind"), p(request, "key"), p(request, "name"),
                    p(request, "type"), p(request, "refKind"), on(request, "storefront"))));
    }

    @PostMapping("/admin/{storeId}/online-store/metafields/values")
    public String setValue(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        String defId = p(request, "defId");
        String kind = "";
        List<MetafieldDefDto> all = content.metafields(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).id().equals(defId)) {
                kind = all.get(iter).ownerKind();
            }
        }
        final String ownerKind = kind;
        return change(request, base(storeId) + "/metafields", "Value saved",
                () -> content.putMetafieldValue(storeId, new com.codenameone.examples.bazaar.api
                    .MetafieldValueDto(defId, ownerKind, p(request, "ownerId"), null, null, null,
                    p(request, "value"))));
    }

    @PostMapping("/admin/{storeId}/online-store/metafields/{id}/remove")
    public String removeField(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        return change(request, base(storeId) + "/metafields", "Field removed",
                () -> content.removeMetafield(storeId, id));
    }

    // ========================================================== metaobjects

    @GetMapping("/admin/{storeId}/online-store/metaobjects")
    public String metaobjects(@PathVariable("storeId") String storeId, Model model,
            HttpServer.Request request) throws Exception {
        MarketingView v = view("metaobjects");
        pages.admin(request, storeId, "Metaobjects");
        v.intro = "Shared entries (a designer, a material) that reference fields point at.";
        String b = base(storeId) + "/metaobjects";
        List<MetaobjectTypeDto> types = content.metaobjectTypes(storeId);
        v.cardsTitle = "Entries";
        List<String[]> typeOptions = new ArrayList<String[]>();
        for (int iter = 0; iter < types.size(); iter++) {
            MetaobjectTypeDto t = types.get(iter);
            typeOptions.add(new String[] {t.id(), t.name()});
            List<MetaobjectEntryDto> entries = content.metaobjects(storeId, t.id());
            for (int e = 0; e < entries.size(); e++) {
                MetaobjectEntryDto d = entries.get(e);
                ActionCard k = card(d.title(), t.name() + " /" + d.handle() + " - id " + d.id());
                k.extraUrl = "/admin/" + storeId + "/translations?kind=" + com.codenameone.examples.bazaar.market.Translations.METAOBJECT_ENTRY + "&refId=" + d.id();
                k.extraLabel = "Translate";
                k.buttons.add(button("Remove", b + "/" + d.id() + "/remove", true));
                v.cards.add(k);
            }
        }
        FormView type = new FormView("new-type", "Add a type", b + "/types", "Add type");
        type.add("handle", "Handle", "text", "").required = true;
        type.add("name", "Name", "text", "").required = true;
        v.forms.add(type);
        if (!typeOptions.isEmpty()) {
            FormView entry = new FormView("new-entry", "Add an entry", b, "Add entry");
            FormField t = entry.add("typeId", "Type", "select", "");
            for (int iter = 0; iter < typeOptions.size(); iter++) {
                t.option(typeOptions.get(iter)[0], typeOptions.get(iter)[1]);
            }
            entry.add("handle", "Handle", "text", "");
            entry.add("title", "Title", "text", "").required = true;
            v.forms.add(entry);
        }
        return render(model, request, storeId, "Metaobjects", v);
    }

    @PostMapping("/admin/{storeId}/online-store/metaobjects/types")
    public String addType(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        return change(request, base(storeId) + "/metaobjects", "Type added",
                () -> content.addMetaobjectType(storeId, new MetaobjectTypeDto(null,
                    p(request, "handle"), p(request, "name"))));
    }

    @PostMapping("/admin/{storeId}/online-store/metaobjects")
    public String addEntry(@PathVariable("storeId") String storeId,
            HttpServer.Request request) throws Exception {
        String typeId = p(request, "typeId");
        return change(request, base(storeId) + "/metaobjects", "Entry added",
                () -> content.addMetaobject(storeId, typeId, new MetaobjectEntryDto(null, typeId,
                    p(request, "handle"), p(request, "title"))));
    }

    @PostMapping("/admin/{storeId}/online-store/metaobjects/{id}/remove")
    public String removeEntry(@PathVariable("storeId") String storeId,
            @PathVariable("id") String id, HttpServer.Request request) throws Exception {
        return change(request, base(storeId) + "/metaobjects", "Entry removed",
                () -> content.removeMetaobject(storeId, id));
    }
}
