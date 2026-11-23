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
import com.codename1.backend.Json;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.mvc.Model;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.CartLineDto;
import com.codenameone.examples.bazaar.api.CollectionDto;
import com.codenameone.examples.bazaar.api.ProductCardDto;
import com.codenameone.examples.bazaar.api.ProductDto;
import com.codenameone.examples.bazaar.api.ProductPageDto;
import com.codenameone.examples.bazaar.api.ProductPhotoDto;
import com.codenameone.examples.bazaar.api.VariantDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.domain.Collection;
import com.codenameone.examples.bazaar.domain.Fulfillment;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.Review;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.TrackingEvent;
import com.codenameone.examples.bazaar.repository.CollectionRepository;
import com.codenameone.examples.bazaar.repository.FulfillmentRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.ReviewRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.TrackingEventRepository;
import com.codenameone.examples.bazaar.store.Stores;

import com.codenameone.examples.bazaar.web.slots.Slots;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// Builds the models of the server-rendered storefront: what search engines
/// index and what a shared link opens, and the cart its HTML checkout
/// ([CheckoutPages]) places.
///
/// Everything here reads what the app's own public endpoints read -- only
/// ACTIVE stores, ACTIVE products, published collections and reviews -- through
/// the same services, so a draft never reaches a page the API would refuse.
/// The models are plain objects with public fields, because the compiled views
/// read JavaBean properties and fields, not record accessors.
@Component
public class StorefrontPages {
    @com.codename1.backend.annotations.Autowired
    private ExpressButton expressButton;
    /// Products on a page of a collection or of search results.
    static final int PAGE_SIZE = 24;
    /// The session attribute a store's cart token is kept under, plus its id.
    static final String CART_KEY = "storefront.cart.";

    @com.codename1.backend.annotations.Autowired
    private Backorders backorders;
    private final Stores stores;
    private final Catalog catalog;
    private final Carts carts;
    private final StoreRepository storeRows;
    private final ProductRepository products;
    private final CollectionRepository collections;
    private final ReviewRepository reviews;
    private final FulfillmentRepository fulfillments;
    private final TrackingEventRepository tracking;
    private final SiteUrls urls;
    private final GeoCountry geo;
    private final Slots slots;
    private final com.codenameone.examples.bazaar.subscribe.Subscriptions subscriptions;

    private final com.codenameone.examples.bazaar.market.Markets markets;
    private final com.codenameone.examples.bazaar.market.Translations translations;
    private final com.codenameone.examples.bazaar.content.Themes themes;
    private final com.codenameone.examples.bazaar.content.Navigation navigation;
    private final com.codenameone.examples.bazaar.content.Pages content;
    private final com.codenameone.examples.bazaar.content.Metafields metafields;

    /// The exchange attribute a signed theme preview sets (see [StorefrontCaching]).
    public static final String PREVIEW = "bazaar.themePreview";

    private final com.codenameone.examples.bazaar.ops.Attribution attribution;

    private final com.codenameone.examples.bazaar.catalog.Bundles bundles;
    /// Product and collection text in the shopper's language. Field-injected
    /// so the constructor stays as it was.
    @com.codename1.backend.annotations.Autowired
    private com.codenameone.examples.bazaar.market.ProductText productText;

    public StorefrontPages(Stores stores, Catalog catalog, Carts carts, StoreRepository storeRows,
            ProductRepository products, CollectionRepository collections,
            ReviewRepository reviews, FulfillmentRepository fulfillments,
            TrackingEventRepository tracking, SiteUrls urls, Slots slots,
            com.codenameone.examples.bazaar.subscribe.Subscriptions subscriptions,
            com.codenameone.examples.bazaar.market.Markets markets,
            com.codenameone.examples.bazaar.market.Translations translations,
            com.codenameone.examples.bazaar.ops.Attribution attribution,
            com.codenameone.examples.bazaar.content.Themes themes,
            com.codenameone.examples.bazaar.content.Navigation navigation,
            com.codenameone.examples.bazaar.content.Pages content,
            com.codenameone.examples.bazaar.content.Metafields metafields,
            GeoCountry geo, com.codenameone.examples.bazaar.catalog.Bundles bundles) {
        this.bundles = bundles;
        this.geo = geo;
        this.themes = themes;
        this.navigation = navigation;
        this.content = content;
        this.metafields = metafields;
        this.attribution = attribution;
        this.subscriptions = subscriptions;
        this.markets = markets;
        this.translations = translations;
        this.stores = stores;
        this.catalog = catalog;
        this.carts = carts;
        this.storeRows = storeRows;
        this.products = products;
        this.collections = collections;
        this.reviews = reviews;
        this.fulfillments = fulfillments;
        this.tracking = tracking;
        this.urls = urls;
        this.slots = slots;
    }

    // ---------------------------------------------------------------- pages

    /// The platform's front page: every active store.
    @Transactional(readOnly = true)
    public void index(Model model, HttpServer.Request request) {
        List<StoreCard> out = new ArrayList<StoreCard>();
        List<Store> active = storeRows.findByStatusOrderByNameAsc(Stores.ACTIVE);
        for (int iter = 0; iter < active.size(); iter++) {
            Store s = active.get(iter);
            StoreCard card = new StoreCard();
            card.name = s.getName();
            card.tagline = s.getTagline();
            card.url = storeUrl(s.getHandle());
            card.accent = accent(s.getAccentColor());
            card.art = new Art(s.getId(), s.getHeroPhotoId().length() > 0 ? s.getHeroPhotoId()
                    : s.getLogoPhotoId(), s.getName());
            out.add(card);
        }
        PageMeta page = new PageMeta();
        page.title = "Bazaar - independent shops";
        page.description = "Shop independent stores on Bazaar.";
        page.canonical = urls.absolute(request, "/");
        StoreText.fromHeader(page, request);
        model.addAttribute("page", page).addAttribute("stores", out);
    }

    /// A store's front page: its hero, collections and featured products.
    public void home(String handle, Model model, HttpServer.Request request) throws IOException {
        Store store = stores.activeStore(handle);
        PageMeta page = meta(store, request, "/s/" + store.getHandle());
        page.title = store.getName() + (store.getTagline().length() > 0
                ? " - " + store.getTagline() : "");
        page.description = describe(store.getDescription().length() > 0
                ? store.getDescription() : store.getTagline());
        if (store.getHeroPhotoId().length() > 0) {
            page.image = urls.absolute(request, "/media/" + store.getHeroPhotoId());
        }
        Map<String, Object> ld = new LinkedHashMap<String, Object>();
        ld.put("@context", "https://schema.org");
        ld.put("@type", "Store");
        ld.put("name", store.getName());
        ld.put("url", page.canonical);
        if (store.getDescription().length() > 0) {
            ld.put("description", store.getDescription());
        }
        if (page.image.length() > 0) {
            ld.put("image", page.image);
        }
        page.jsonLd = Json.write(ld);
        String locale = contentLocale(store, page.canonical, request);
        List<CollectionDto> cs = catalog.publicCollections(store.getHandle(), locale);
        List<LinkView> links = new ArrayList<LinkView>();
        for (int iter = 0; iter < cs.size(); iter++) {
            CollectionDto c = cs.get(iter);
            if (c.productCount() > 0) {
                links.add(new LinkView(c.title(), collectionUrl(store.getHandle(), c.handle()),
                        false));
            }
        }
        ProductPageDto featured = catalog.browse(store.getHandle(), null, null, "rating", 0, 8,
                locale);
        model.addAttribute("page", page)
                .addAttribute("hero", new Art(store.getId(), store.getHeroPhotoId(),
                        store.getName()))
                .addAttribute("tagline", store.getTagline())
                .addAttribute("about", store.getDescription())
                .addAttribute("collections", links)
                .addAttribute("products", cards(store, featured.items(), null))
                .addAttribute("posts", latestPosts(store));
    }

    /// The three newest posts, for the home page's blog section.
    private List<PostView> latestPosts(Store store) throws IOException {
        List<com.codenameone.examples.bazaar.domain.BlogPost> rows =
                content.publishedPosts(store.getId(), null);
        List<PostView> out = new ArrayList<PostView>();
        for (int iter = 0; iter < rows.size() && iter < 3; iter++) {
            com.codenameone.examples.bazaar.domain.BlogPost b = rows.get(iter);
            PostView v = new PostView();
            v.title = b.getTitle();
            v.excerpt = b.getExcerpt();
            v.author = b.getAuthor();
            v.date = day(b.getPublishedAt());
            v.url = storeUrl(store.getHandle()) + "/blog/" + SiteUrls.encode(b.getHandle());
            out.add(v);
        }
        return out;
    }

    /// A published collection, a page of it, in the order asked for.
    public void collection(String handle, String collection, String sort, int page, Model model,
            HttpServer.Request request) throws IOException {
        Store store = stores.activeStore(handle);
        Collection c = collection == null ? null
                : collections.findByStoreIdAndHandle(store.getId(), collection);
        if (c == null || !c.isPublished()) {
            throw new ResponseStatusException(404, "No such collection");
        }
        String path = collectionUrl(store.getHandle(), c.getHandle());
        String order = sort(sort);
        int category = category(param(request, "category"));
        PageMeta meta = meta(store, request, path + (page > 0 ? "?page=" + page : ""));
        String locale = contentLocale(store, path, request);
        ProductPageDto found = catalog.browse(store.getHandle(), c.getHandle(), null, order,
                Math.max(0, page), PAGE_SIZE, category, locale);
        Map<String, String> tr = locale.length() == 0 ? null
                : translations.fields(store.getId(),
                    com.codenameone.examples.bazaar.market.Translations.COLLECTION, c.getId(),
                    locale);
        String title = StorefrontMarkets.text(tr, "title", c.getTitle());
        String about = StorefrontMarkets.text(tr, "description", c.getDescription());
        meta.title = title + " - " + store.getName();
        meta.description = describe(about.length() > 0 ? about
                : title + " from " + store.getName());
        if (category > 0) {
            // A filtered page is a permutation of the collection, not a page
            // of its own for an index.
            meta.noindex = true;
        }
        list(model, store, meta, found, path, order, "", title, about, category);
        model.addAttribute("categories", categoryChips(store, c.getHandle(), path, order,
                category, meta));
    }

    /// The `category` query parameter as a taxonomy id, 0 for none or junk.
    static int category(String text) {
        int id = com.codenameone.examples.bazaar.catalog.Taxonomy.parse(text);
        return id > 0 ? id : 0;
    }

    /// The category filter of a collection: "All" and each top-level
    /// category its products are in, with how many. Empty when the products
    /// fall under fewer than two, where a filter would choose nothing.
    private List<LinkView> categoryChips(Store store, String collection, String path,
            String order, int current, PageMeta meta) throws IOException {
        List<int[]> counts = catalog.categoryCounts(store.getHandle(), collection, null);
        List<LinkView> out = new ArrayList<LinkView>();
        if (counts.size() < 2 && current == 0) {
            return out;
        }
        String sorted = "sort=" + SiteUrls.encode(order);
        out.add(new LinkView(meta.t.get("allCategories"), path + "?" + sorted, current == 0));
        for (int iter = 0; iter < counts.size(); iter++) {
            int id = counts.get(iter)[0];
            out.add(new LinkView(com.codenameone.examples.bazaar.catalog.Taxonomy.name(id)
                    + " (" + counts.get(iter)[1] + ")", path + "?category=" + id + "&" + sorted,
                    id == com.codenameone.examples.bazaar.catalog.Taxonomy.top(current)));
        }
        return out;
    }

    /// Search results.
    public void search(String handle, String q, String sort, int page, Model model,
            HttpServer.Request request) throws IOException {
        Store store = stores.activeStore(handle);
        String words = q == null ? "" : q.trim();
        if (words.length() > 100) {
            words = words.substring(0, 100);
        }
        String order = sort(sort);
        String path = "/s/" + store.getHandle() + "/search";
        PageMeta meta = meta(store, request, path);
        // Matched against the titles the shopper reads, too.
        ProductPageDto found = catalog.browse(store.getHandle(), null, words, order,
                Math.max(0, page), PAGE_SIZE, contentLocale(store, path, request));
        meta.title = (words.length() > 0 ? "\"" + words + "\" - " : "Search - ") + store.getName();
        meta.description = "Search " + store.getName();
        meta.query = words;
        // Result pages are endless permutations of the catalog; the products
        // and collections are what belongs in an index.
        meta.noindex = true;
        list(model, store, meta, found, path, order, words,
                words.length() > 0 ? "Results for \"" + words + "\"" : "All products", "", 0);
        model.addAttribute("categories", new ArrayList<LinkView>());
    }

    private void list(Model model, Store store, PageMeta meta, ProductPageDto found, String path,
            String order, String q, String heading, String intro, int category) {
        String query = (q.length() > 0 ? "q=" + SiteUrls.encode(q) + "&" : "")
                + (category > 0 ? "category=" + category + "&" : "");
        List<LinkView> sorts = new ArrayList<LinkView>();
        String[][] choices = {{"relevance", "Featured"}, {"newest", "Newest"},
            {"price", "Price, low to high"}, {"-price", "Price, high to low"},
            {"rating", "Top rated"}, {"title", "A to Z"}};
        for (int iter = 0; iter < choices.length; iter++) {
            sorts.add(new LinkView(choices[iter][1], path + "?" + query + "sort="
                    + SiteUrls.encode(choices[iter][0]), choices[iter][0].equals(order)));
        }
        int size = Math.max(1, found.size());
        int pages = (found.total() + size - 1) / size;
        List<LinkView> pager = new ArrayList<LinkView>();
        String base = path + "?" + query + "sort=" + SiteUrls.encode(order) + "&page=";
        if (found.page() > 0) {
            pager.add(new LinkView("Previous", base + (found.page() - 1), false));
        }
        for (int iter = Math.max(0, found.page() - 3); iter < pages
                && iter <= found.page() + 3; iter++) {
            pager.add(new LinkView(Integer.toString(iter + 1), base + iter, iter == found.page()));
        }
        if (found.page() + 1 < pages) {
            pager.add(new LinkView("Next", base + (found.page() + 1), false));
        }
        model.addAttribute("page", meta)
                .addAttribute("heading", heading)
                .addAttribute("intro", intro)
                .addAttribute("total", Integer.valueOf(found.total()))
                .addAttribute("sorts", sorts)
                .addAttribute("pager", pages > 1 ? pager : new ArrayList<LinkView>())
                .addAttribute("products", cards(store, found.items(), null));
    }

    /// A product: its pictures, choices, price, stock and reviews, and the
    /// structured data search engines show as a rich result.
    public void product(String handle, String productHandle, String variantId, Model model,
            HttpServer.Request request) throws IOException {
        Store store = stores.activeStore(handle);
        ProductDto p = catalog.publicProductByHandle(store.getHandle(), productHandle);
        String path = productUrl(store.getHandle(), p.handle());
        PageMeta meta = meta(store, request, path);
        meta.appUrl = meta.appUrl + "&product=" + SiteUrls.encode(p.handle());
        meta.type = "product";
        meta.title = (p.seoTitle() != null && p.seoTitle().length() > 0 ? p.seoTitle()
                : p.title()) + " - " + store.getName();
        meta.description = describe(p.seoDescription() != null && p.seoDescription().length() > 0
                ? p.seoDescription() : p.description().length() > 0 ? p.description()
                : p.title() + " from " + store.getName());
        ProductView view = new ProductView();
        view.id = p.id();
        view.url = path;
        view.title = p.title();
        view.vendor = p.vendor();
        view.description = p.description();
        view.includes = bundles.describe(p.id());
        view.currency = store.getCurrency();
        StorefrontMarkets.Choice market = StorefrontMarkets.choose(store, path, request, markets,
                translations, urls, geo);
        if (market.suggested != null) {
            view.geoNote = market.suggested.label;
            view.geoUrl = market.suggested.url;
        }
        if (market.locale != null) {
            // Everything below -- variants, pictures, structured data -- reads
            // the product in the shopper's language from here on.
            p = productText.localize(p, market.locale);
            view.title = p.title();
            view.description = p.description();
            meta.title = (p.seoTitle() != null && p.seoTitle().length() > 0 ? p.seoTitle()
                    : view.title) + " - " + store.getName();
            meta.description = describe(p.seoDescription() != null
                    && p.seoDescription().length() > 0 ? p.seoDescription()
                    : view.description.length() > 0 ? view.description : view.title);
            view.lang = market.locale;
        }
        final com.codenameone.examples.bazaar.market.Markets.Pricer pricer = market.pricer;
        final String shown = pricer == null ? store.getCurrency() : pricer.currency();
        view.currency = shown;
        List<String> images = new ArrayList<String>();
        List<ProductPhotoDto> photos = p.photos() == null ? new ArrayList<ProductPhotoDto>()
                : p.photos();
        String poster = "";
        for (int iter = 0; iter < photos.size() && poster.length() == 0; iter++) {
            if (!"VIDEO".equals(photos.get(iter).kind()) && !"MODEL".equals(photos.get(iter).kind())
                    && photos.get(iter).photoId().length() > 0) {
                poster = "/media/" + photos.get(iter).photoId();
            }
        }
        for (int iter = 0; iter < photos.size(); iter++) {
            ProductPhotoDto ph = photos.get(iter);
            if ("VIDEO".equals(ph.kind()) || "MODEL".equals(ph.kind())) {
                view.gallery.add(new Art(ph.id(), ph.photoId(), alt(ph, p), ph.kind(), ph.url(),
                        poster));
                view.hasModel |= "MODEL".equals(ph.kind()) && ph.url() != null
                        && ph.url().startsWith("/");
                continue;
            }
            view.gallery.add(new Art(ph.id(), ph.photoId(), alt(ph, p)));
            images.add(urls.absolute(request, "/media/" + ph.photoId()));
        }
        if (view.gallery.isEmpty()) {
            view.gallery.add(new Art(p.id(), p.photoId(), p.title()));
            if (p.photoId() != null && p.photoId().length() > 0) {
                images.add(urls.absolute(request, "/media/" + p.photoId()));
            }
        }
        if (!images.isEmpty()) {
            meta.image = images.get(0);
        }
        List<VariantDto> vs = p.variants() == null ? new ArrayList<VariantDto>() : p.variants();
        VariantDto chosen = null;
        for (int iter = 0; iter < vs.size(); iter++) {
            VariantDto v = vs.get(iter);
            if (v.id().equals(variantId) || chosen == null && variantId == null
                    && sellable(v)) {
                chosen = v;
            }
        }
        if (chosen == null && !vs.isEmpty()) {
            chosen = vs.get(0);
        }
        for (int iter = 0; iter < vs.size(); iter++) {
            VariantDto v = vs.get(iter);
            VariantView vv = new VariantView();
            vv.id = v.id();
            vv.title = v.title().length() > 0 ? v.title() : "Default";
            vv.price = Money.format(pricer == null ? v.priceCents()
                    : pricer.price(v.id(), v.priceCents()), shown);
            vv.available = sellable(v) || carts.preorderAllowance(v.id()) > 0;
            vv.selected = v == chosen;
            view.variants.add(vv);
        }
        view.hasVariants = vs.size() > 1;
        if (chosen != null) {
            view.variantId = chosen.id();
            view.variantTitle = chosen.title();
            long cents = pricer == null ? chosen.priceCents()
                    : pricer.price(chosen.id(), chosen.priceCents());
            view.price = Money.format(cents, shown);
            view.amount = Money.plain(cents);
            // Per the price actually shown, so a market's price has its own.
            long unit = com.codenameone.examples.bazaar.ops.UnitPricing.unitPriceCents(cents,
                    chosen.unitQuantity(), chosen.unitName(), chosen.unitReference(),
                    chosen.unitReferenceName());
            if (unit >= 0) {
                view.unitPrice = Money.format(unit, shown) + " / "
                        + com.codenameone.examples.bazaar.ops.UnitPricing.per(
                            chosen.unitReference(), chosen.unitReferenceName());
                view.unitAmount = Money.plain(unit);
                view.unitReference = chosen.unitReference();
                view.unitReferenceName = chosen.unitReferenceName();
            }
            view.compareAt = chosen.compareAtCents() > chosen.priceCents()
                    ? Money.format(pricer == null ? chosen.compareAtCents()
                        : pricer.price("", chosen.compareAtCents()), shown) : "";
            if (pricer != null) {
                // Checkout charges the market's currency at this rate
                // (Checkouts.place), so the price shown is the price paid.
                view.marketNote = "Shown in " + shown + "; checkout charges " + shown + ".";
            }
            view.available = sellable(chosen);
            view.digital = chosen.digital();
            if (!view.available && carts.preorderAllowance(chosen.id()) > 0) {
                view.available = true;
                view.preorder = true;
            }
            java.util.List<com.codenameone.examples.bazaar.api.MetafieldValueDto> fields =
                    metafields.storefront(store.getId(), "PRODUCT", p.id(), market.locale);
            for (int iter = 0; iter < fields.size(); iter++) {
                view.specs.add(new String[] {fields.get(iter).name(), fields.get(iter).value()});
            }
            // The same for every visitor, so the page stays public and cached.
            java.util.List<com.codenameone.examples.bazaar.api.SellingPlanDto> plans =
                    subscriptions.productPlans(store.getId(), p.id());
            for (int iter = 0; iter < plans.size(); iter++) {
                com.codenameone.examples.bazaar.api.SellingPlanDto sp = plans.get(iter);
                VariantView pv = new VariantView();
                pv.id = sp.id();
                pv.title = "Subscribe, " + planWords(sp.unit(), sp.cadence()) + ", save "
                        + com.codenameone.examples.bazaar.web.admin.CommerceData.percent(
                        sp.discountBps()) + "%";
                pv.price = Money.format(chosen.priceCents() - chosen.priceCents()
                        * sp.discountBps() / 10000, store.getCurrency());
                pv.available = view.available;
                view.plans.add(pv);
            }
        }
        view.availability = view.preorder ? "https://schema.org/PreOrder"
                : view.available ? "https://schema.org/InStock"
                : "https://schema.org/OutOfStock";
        view.ratingCount = p.ratingCount();
        view.rating = p.ratingCount() > 0 ? stars10(p.rating()) : "";
        view.stars = p.ratingCount() > 0 ? starGlyphs((p.rating() + 5) / 10) : "";
        List<Review> rs = reviews.findByProductIdAndStatusOrderByCreatedAtDesc(p.id(), "PUBLISHED");
        for (int iter = 0; iter < rs.size() && iter < 20; iter++) {
            Review r = rs.get(iter);
            ReviewView rv = new ReviewView();
            rv.author = r.getAuthorName().length() > 0 ? r.getAuthorName() : "A customer";
            rv.title = r.getTitle();
            rv.body = r.getBody();
            rv.rating = Math.max(0, Math.min(5, r.getRating()));
            rv.stars = starGlyphs(rv.rating);
            rv.date = day(r.getCreatedAt());
            rv.verified = r.isVerified();
            view.reviews.add(rv);
        }
        ProductPageDto more = catalog.browse(store.getHandle(), null, null, "rating", 0, 5,
                market.locale == null ? "" : market.locale);
        view.related = cards(store, more.items(), p.id());
        while (view.related.size() > 4) {
            view.related.remove(view.related.size() - 1);
        }
        view.category = com.codenameone.examples.bazaar.catalog.Taxonomy.path(p.categoryId());
        if (view.hasModel) {
            com.codenameone.examples.bazaar.web.security.WebHeaders.allowModelViewer();
        }
        if (view.available) {
            expressButton.decorate(meta, store, null, 0);
        }
        meta.jsonLd = productLd(p, view, meta, images, rs);
        model.addAttribute("page", meta).addAttribute("product", view);
    }

    /// The UN/CEFACT common code of a unit, as schema.org's `unitCode` takes it.
    static String unitCode(String unit) {
        String[][] codes = {{"mg", "MGM"}, {"g", "GRM"}, {"kg", "KGM"}, {"ml", "MLT"},
            {"cl", "CLT"}, {"l", "LTR"}, {"mm", "MMT"}, {"cm", "CMT"}, {"m", "MTR"},
            {"m2", "MTK"}, {"oz", "ONZ"}, {"lb", "LBR"}, {"ct", "C62"}};
        for (int iter = 0; iter < codes.length; iter++) {
            if (codes[iter][0].equals(unit)) {
                return codes[iter][1];
            }
        }
        return "C62";
    }

    /// "every month", "every 2 weeks".
    public static String planWords(String unit, int n) {
        String u = "DAY".equals(unit) ? "day" : "WEEK".equals(unit) ? "week" : "month";
        return n <= 1 ? "every " + u : "every " + n + " " + u + "s";
    }

    private static String productLd(ProductDto p, ProductView view, PageMeta meta,
            List<String> images, List<Review> rs) {
        Map<String, Object> ld = new LinkedHashMap<String, Object>();
        ld.put("@context", "https://schema.org");
        ld.put("@type", "Product");
        ld.put("name", p.title());
        ld.put("description", meta.description);
        ld.put("url", meta.canonical);
        if (view.lang != null && view.lang.length() > 0) {
            ld.put("inLanguage", view.lang);
        }
        if (!images.isEmpty()) {
            ld.put("image", images);
        }
        if (view.category.length() > 0) {
            ld.put("category", view.category);
        }
        if (p.vendor().length() > 0) {
            Map<String, Object> brand = new LinkedHashMap<String, Object>();
            brand.put("@type", "Brand");
            brand.put("name", p.vendor());
            ld.put("brand", brand);
        }
        if (view.variantId.length() > 0) {
            ld.put("sku", view.variantId);
            Map<String, Object> offer = new LinkedHashMap<String, Object>();
            offer.put("@type", "Offer");
            offer.put("url", meta.canonical);
            offer.put("priceCurrency", view.currency);
            offer.put("price", view.amount);
            offer.put("availability", view.availability);
            offer.put("itemCondition", "https://schema.org/NewCondition");
            if (view.unitAmount.length() > 0) {
                Map<String, Object> spec = new LinkedHashMap<String, Object>();
                spec.put("@type", "UnitPriceSpecification");
                spec.put("price", view.unitAmount);
                spec.put("priceCurrency", view.currency);
                Map<String, Object> ref = new LinkedHashMap<String, Object>();
                ref.put("@type", "QuantitativeValue");
                ref.put("value", Integer.valueOf(view.unitReference));
                ref.put("unitCode", unitCode(view.unitReferenceName));
                ref.put("unitText", view.unitReferenceName);
                spec.put("referenceQuantity", ref);
                offer.put("priceSpecification", spec);
            }
            ld.put("offers", offer);
        }
        if (p.ratingCount() > 0) {
            Map<String, Object> rating = new LinkedHashMap<String, Object>();
            rating.put("@type", "AggregateRating");
            rating.put("ratingValue", view.rating);
            rating.put("reviewCount", Integer.valueOf(p.ratingCount()));
            rating.put("bestRating", "5");
            rating.put("worstRating", "1");
            ld.put("aggregateRating", rating);
        }
        List<Object> list = new ArrayList<Object>();
        for (int iter = 0; iter < rs.size() && iter < 5; iter++) {
            Review r = rs.get(iter);
            Map<String, Object> one = new LinkedHashMap<String, Object>();
            one.put("@type", "Review");
            Map<String, Object> author = new LinkedHashMap<String, Object>();
            author.put("@type", "Person");
            author.put("name", r.getAuthorName().length() > 0 ? r.getAuthorName() : "A customer");
            one.put("author", author);
            Map<String, Object> given = new LinkedHashMap<String, Object>();
            given.put("@type", "Rating");
            given.put("ratingValue", Integer.toString(Math.max(1, Math.min(5, r.getRating()))));
            one.put("reviewRating", given);
            if (r.getBody().length() > 0) {
                one.put("reviewBody", r.getBody());
            }
            one.put("datePublished", day(r.getCreatedAt()));
            list.add(one);
        }
        if (!list.isEmpty()) {
            ld.put("review", list);
        }
        return Json.write(ld);
    }

    // ----------------------------------------------------------------- cart

    /// The cart page. A visitor with no cart yet gets an empty one shown, and
    /// none made: a crawler following the cart link must not fill the table.
    public void cart(String handle, Model model, HttpServer.Request request) throws IOException {
        Store store = stores.activeStore(handle);
        // `?r=` is the abandoned-cart reminder's recovery token: it puts that
        // cart back into this session.
        String recovery = param(request, "r");
        String recovered = recovery == null ? null : carts.recover(store.getId(), recovery);
        if (recovered != null) {
            remember(request, store, recovered);
        }
        String token = token(request, store, false);
        CartDto dto = token == null ? null : carts.get(store.getId(), null, token);
        PageMeta meta = meta(store, request, "/s/" + store.getHandle() + "/cart");
        meta.title = meta.t.get("cart.title") + " - " + store.getName();
        meta.noindex = true;
        CartView view = new CartView();
        if (dto != null) {
            remember(request, store, dto.token());
            Map<String, String> handles = handles(dto.lines());
            String locale = contentLocale(store, "/s/" + store.getHandle() + "/cart", request);
            for (int iter = 0; iter < dto.lines().size(); iter++) {
                CartLineDto l = dto.lines().get(iter);
                CartLineView lv = new CartLineView();
                lv.id = l.id();
                lineNames(lv, store, l.productId(), l.title(), l.variantTitle(), locale);

                String ph = handles.get(l.productId());
                lv.url = ph == null ? "" : productUrl(store.getHandle(), ph);
                lv.quantity = l.quantity();
                lv.unit = Money.format(l.priceCents(), store.getCurrency());
                lv.total = Money.format(l.priceCents() * l.quantity(), store.getCurrency());
                lv.available = l.available();
                lv.preorder = l.preorder();
                lv.backordered = !l.preorder() && backorders.wouldBackorder(l.variantId(),
                        l.quantity());
                lv.art = new Art(l.productId(), l.photoId(), l.title());
                view.lines.add(lv);
                view.count += l.quantity();
            }
            view.subtotal = Money.format(dto.subtotalCents(), store.getCurrency());
            view.discount = dto.discountCents() > 0
                    ? "-" + Money.format(dto.discountCents(), store.getCurrency()) : "";
            view.tax = Money.format(dto.taxCents(), store.getCurrency());
            view.taxInclusive = dto.taxInclusive();
            view.total = Money.format(dto.totalCents(), store.getCurrency());
            view.problems = dto.problems() == null ? new ArrayList<String>() : dto.problems();
        }
        view.empty = view.lines.isEmpty();
        if (!view.empty) {
            expressButton.decorate(meta, store, null, dto == null ? 0 : dto.dueCents());
        }
        view.checkoutUrl = SiteUrls.checkout(store.getHandle());
        model.addAttribute("page", meta).addAttribute("cart", view);
    }

    /// Adds a variant to the visitor's cart, making the cart when it is the first.
    public void add(String handle, CartForm form, HttpServer.Request request) throws IOException {
        Store store = stores.activeStore(handle);
        int qty = form == null ? 0 : Math.max(1, Math.min(Carts.MAX_QUANTITY, form.quantity));
        CartDto dto = carts.add(store.getId(), null, token(request, store, true),
                new CartLineChangeDto(form == null ? null : form.variantId, qty));
        remember(request, store, dto.token());
        attribution.stampCart(request, handle, store.getId(), dto.id());
    }

    /// Sets a line's quantity; zero or less removes it.
    public void update(String handle, CartForm form, boolean remove, HttpServer.Request request)
            throws IOException {
        Store store = stores.activeStore(handle);
        String token = token(request, store, false);
        if (token == null || form == null) {
            return;
        }
        CartDto dto = carts.update(store.getId(), null, token, form.lineId,
                remove ? 0 : Math.min(Carts.MAX_QUANTITY, form.quantity));
        remember(request, store, dto.token());
    }

    static String token(HttpServer.Request request, Store store, boolean create) {
        com.codename1.backend.HttpSession session = request.getSession(create);
        Object token = session == null ? null : session.getAttribute(CART_KEY + store.getId());
        return token instanceof String ? (String) token : null;
    }

    /// The cart token the visitor's session holds for store `storeId`, or
    /// null; never makes a session.
    public static String cartToken(HttpServer.Request request, String storeId) {
        com.codename1.backend.HttpSession session = request == null ? null
                : request.getSession(false);
        Object token = session == null ? null : session.getAttribute(CART_KEY + storeId);
        return token instanceof String ? (String) token : null;
    }

    static void remember(HttpServer.Request request, Store store, String token) {
        com.codename1.backend.HttpSession session = request.getSession(true);
        if (session != null && token != null) {
            session.setAttribute(CART_KEY + store.getId(), token);
        }
    }

    Map<String, String> handles(List<CartLineDto> lines) {
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < lines.size(); iter++) {
            ids.add(lines.get(iter).productId());
        }
        Map<String, String> out = new HashMap<String, String>();
        List<Product> found = ids.isEmpty() ? new ArrayList<Product>() : products.findByIdIn(ids);
        for (int iter = 0; iter < found.size(); iter++) {
            if (Catalog.ACTIVE.equals(found.get(iter).getStatus())) {
                out.put(found.get(iter).getId(), found.get(iter).getHandle());
            }
        }
        return out;
    }

    // ------------------------------------------------------------- tracking

    /// A shipment by its tracking number.
    @Transactional(readOnly = true)
    public void track(String number, Model model, HttpServer.Request request) {
        Fulfillment f = number == null || number.length() == 0 || number.length() > 64 ? null
                : fulfillments.findByTrackingNumber(number);
        Store store = f == null ? null : storeRows.findById(f.getStoreId());
        if (f == null || store == null) {
            throw new ResponseStatusException(404, "No such shipment");
        }
        TrackView view = new TrackView();
        view.number = f.getTrackingNumber();
        view.store = store.getName();
        view.storeUrl = Stores.ACTIVE.equals(store.getStatus()) ? storeUrl(store.getHandle()) : "";
        view.carrier = f.getCarrier() + (f.getService().length() > 0 ? " " + f.getService() : "");
        view.status = label(f.getStatus());
        view.stage = stage(f.getStatus());
        view.delivered = "DELIVERED".equals(f.getStatus()) || "PICKED_UP".equals(f.getStatus());
        view.cancelled = "CANCELLED".equals(f.getStatus());
        List<TrackingEvent> events = tracking.findByFulfillmentIdOrderByCreatedAtAsc(f.getId());
        for (int iter = events.size() - 1; iter >= 0; iter--) {
            TrackingEvent e = events.get(iter);
            TrackStep step = new TrackStep();
            step.status = label(e.getStatus());
            step.message = e.getMessage();
            step.place = e.getPlace();
            step.when = moment(e.getCreatedAt());
            view.steps.add(step);
        }
        PageMeta meta = new PageMeta();
        meta.title = "Tracking " + view.number + " - " + store.getName();
        meta.canonical = urls.absolute(request, "/track/" + SiteUrls.encode(view.number));
        meta.accent = accent(store.getAccentColor());
        meta.brand = store.getName();
        meta.home = view.storeUrl.length() > 0 ? view.storeUrl : "/";
        meta.noindex = true;
        StoreText.fromHeader(meta, request);
        meta.title = meta.t.get("tracking") + " " + view.number + " - " + store.getName();
        view.status = StoreText.translate(meta.t, view.status);
        for (int iter = 0; iter < view.steps.size(); iter++) {
            view.steps.get(iter).status = StoreText.translate(meta.t, view.steps.get(iter).status);
        }
        model.addAttribute("page", meta).addAttribute("track", view);
    }

    private static int stage(String status) {
        String[] order = {"PACKED", "SHIPPED", "IN_TRANSIT", "OUT_FOR_DELIVERY", "DELIVERED"};
        for (int iter = 0; iter < order.length; iter++) {
            if (order[iter].equals(status)) {
                return iter;
            }
        }
        return "PICKED_UP".equals(status) ? 4 : 0;
    }

    /// `OUT_FOR_DELIVERY` as `Out for delivery`.
    static String label(String status) {
        if (status == null || status.length() == 0) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < status.length(); iter++) {
            char c = status.charAt(iter);
            if (c == '_') {
                out.append(' ');
            } else if (iter > 0 && c >= 'A' && c <= 'Z') {
                out.append((char) (c + 0x20));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /// A store's privacy and cookie page: its policies, the apps that may load
    /// and a form to change the consent choice.
    @Transactional(readOnly = true)
    public void privacy(String handle, Model model, HttpServer.Request request)
            throws IOException {
        Store store = stores.activeStore(handle);
        String path = "/s/" + store.getHandle() + "/privacy";
        PageMeta meta = meta(store, request, path);
        meta.title = meta.t.get("privacy") + " - " + store.getName();
        meta.consentAsk = false;
        meta.consentBack = path;
        model.addAttribute("page", meta).addAttribute("privacy", slots.privacy(store, request));
    }

    // -------------------------------------------------------------- helpers

    PageMeta meta(Store store, HttpServer.Request request, String path) {
        PageMeta page = new PageMeta();
        page.canonical = urls.absolute(request, path);
        page.accent = accent(store.getAccentColor());
        page.brand = store.getName();
        page.home = storeUrl(store.getHandle());
        page.cartUrl = "/s/" + store.getHandle() + "/cart";
        page.searchUrl = "/s/" + store.getHandle() + "/search";
        page.suggestUrl = "/s/" + store.getHandle() + "/suggest";
        page.chatStore = store.getHandle();
        // "Open the app" from a store's page opens that store in the app
        // (DeepLink in the app reads it); a product page adds the product.
        page.appUrl = "/app/?store=" + SiteUrls.encode(store.getHandle());
        page.description = describe(store.getTagline());
        try {
            // The interface language; choose() records the Vary (or the
            // private copy) the choice needs, once per request.
            int query = path.indexOf('?');
            StorefrontMarkets.Choice choice = StorefrontMarkets.choose(store,
                    query < 0 ? path : path.substring(0, query), request, markets,
                    translations, urls);
            StoreText.apply(page, choice.ui);
            StoreText.offer(page, request);
            page.marketName = store.getCurrency();
            if (choice.picker.size() > 1) {
                page.markets = choice.picker;
                for (int iter = 1; iter < choice.picker.size(); iter++) {
                    LinkView m = choice.picker.get(iter);
                    int open = m.label.lastIndexOf('(');
                    if (m.current && open >= 0 && m.label.endsWith(")")) {
                        page.marketName = m.label.substring(open + 1, m.label.length() - 1);
                    }
                }
            }
            decorate(page, store, request, path);
            slots.decorate(page, store, request);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        return page;
    }

    /// The theme (or a signed preview of the draft), the logo, the menus and
    /// the policy links every storefront page carries.
    private void decorate(PageMeta page, Store store, HttpServer.Request request, String path)
            throws IOException {
        com.codenameone.examples.bazaar.domain.StoreTheme theme = null;
        String token = param(request, "preview");
        if (token != null) {
            theme = themes.preview(store, token);
        }
        if (theme != null) {
            page.preview = true;
            page.noindex = true;
            com.codename1.backend.security.SecurityExchange here =
                    com.codename1.backend.security.SecurityExchange.current();
            if (here != null) {
                here.setAttribute(PREVIEW, "1");
            }
        } else {
            theme = themes.liveOf(store.getId());
        }
        page.themeClass = com.codenameone.examples.bazaar.content.Themes.classes(theme);
        if (theme.getAccent().length() == 7) {
            page.accent = theme.getAccent();
        }
        String logo = theme.getLogoPhotoId();
        if (logo.length() == 0) {
            logo = store.getLogoPhotoId();
        }
        page.logoUrl = logo.length() > 0 ? "/media/" + logo : "";
        page.sections = com.codenameone.examples.bazaar.content.Themes.sectionList(theme);
        List<com.codenameone.examples.bazaar.domain.MenuLink> links =
                navigation.ordered(store.getId());
        // Menu labels in the market's language: only a store that translated
        // a label asks, so the others' pages vary by nothing new. choose()
        // records the Vary / personal headers the language now needs.
        java.util.Map<String, java.util.Map<String, String>> labels =
                new java.util.HashMap<String, java.util.Map<String, String>>();
        if (!links.isEmpty() && translations.has(store.getId(), com.codenameone.examples.bazaar.market.Translations.MENU_LINK)) {
            StorefrontMarkets.Choice market = StorefrontMarkets.choose(store, path, request,
                    markets, translations, urls);
            labels = translations.allOf(store.getId(),
                    com.codenameone.examples.bazaar.market.Translations.MENU_LINK, market.locale);
        }
        MenuView top = null;
        for (int iter = 0; iter < links.size(); iter++) {
            com.codenameone.examples.bazaar.domain.MenuLink l = label(links.get(iter), labels);
            if (com.codenameone.examples.bazaar.content.Navigation.FOOTER.equals(l.getMenu())) {
                page.footerLinks.add(new LinkView(l.getTitle(), l.getUrl(), false));
            } else if (l.getParentId().length() == 0) {
                top = new MenuView();
                top.label = l.getTitle();
                top.url = l.getUrl();
                page.menu.add(top);
            } else if (top != null) {
                top.children.add(new LinkView(l.getTitle(), l.getUrl(), false));
            }
        }
        List<com.codenameone.examples.bazaar.domain.ContentPage> policies =
                content.publishedPolicies(store.getId());
        for (int iter = 0; iter < policies.size(); iter++) {
            com.codenameone.examples.bazaar.domain.ContentPage c = policies.get(iter);
            page.footerLinks.add(new LinkView(c.getTitle(),
                    storeUrl(store.getHandle()) + "/policies/" + c.getHandle(), false));
        }
    }

    /// `l` with its title translated where `labels` has one; a copy, never
    /// the row itself.
    static com.codenameone.examples.bazaar.domain.MenuLink label(
            com.codenameone.examples.bazaar.domain.MenuLink l,
            java.util.Map<String, java.util.Map<String, String>> labels) {
        java.util.Map<String, String> tr = labels.get(l.getId());
        String t = tr == null ? null : tr.get("title");
        if (t == null || t.length() == 0) {
            return l;
        }
        com.codenameone.examples.bazaar.domain.MenuLink copy =
                new com.codenameone.examples.bazaar.domain.MenuLink();
        copy.setId(l.getId());
        copy.setMenu(l.getMenu());
        copy.setParentId(l.getParentId());
        copy.setUrl(l.getUrl());
        copy.setTitle(t);
        return copy;
    }

    static String param(HttpServer.Request request, String name) {
        try {
            String v = request.param(name);
            return v == null || v.length() == 0 ? null : v;
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    /// A picture's alternative text: its own (translated), else the title.
    private static String alt(ProductPhotoDto ph, ProductDto p) {
        return ph.alt() != null && ph.alt().length() > 0 ? ph.alt() : p.title();
    }

    /// The locale this page shows products in, or empty for the originals:
    /// [StorefrontMarkets#choose]'s, which the page's frame already asked.
    public String contentLocale(Store store, String path, HttpServer.Request request)
            throws IOException {
        StorefrontMarkets.Choice c = StorefrontMarkets.choose(store, path, request, markets,
                translations, urls);
        return c.locale == null ? "" : c.locale;
    }

    /// Fills a cart or order line's names in `locale` (empty keeps the names
    /// the line recorded).
    public void lineNames(CartLineView lv, Store store, String productId, String title,
            String variant, String locale) throws IOException {
        String[] n = productText.line(store.getId(), productId, title, variant, locale);
        lv.title = n[0];
        lv.variant = n[1] == null ? "" : n[1];
    }

    /// An order's language ([StoreOrder#getLanguage]) as a locale the store
    /// translated into, or empty.
    public String orderLocale(Store store, String language) throws IOException {
        String l = productText.locale(store.getId(), language);
        return l == null ? "" : l;
    }

    private List<CardView> cards(Store store, List<ProductCardDto> items, String skip) {

        List<CardView> out = new ArrayList<CardView>();
        for (int iter = 0; iter < items.size(); iter++) {
            ProductCardDto c = items.get(iter);
            if (c.id().equals(skip)) {
                continue;
            }
            CardView v = new CardView();
            v.title = c.title();
            v.vendor = c.vendor();
            v.url = productUrl(store.getHandle(), c.handle());
            v.price = (c.maxPriceCents() > c.minPriceCents() ? "From " : "")
                    + Money.format(c.minPriceCents(), store.getCurrency());
            v.compareAt = c.compareAtCents() > c.minPriceCents()
                    ? Money.format(c.compareAtCents(), store.getCurrency()) : "";
            v.unitPrice = c.unitPriceCents() < 0 || c.unitPer() == null ? ""
                    : Money.format(c.unitPriceCents(), store.getCurrency()) + " / " + c.unitPer();
            v.soldOut = c.available() == 0;
            v.rating = c.ratingCount() > 0 ? stars10(c.rating()) : "";
            v.ratingCount = c.ratingCount();
            v.art = new Art(c.id(), c.photoId(), c.title());
            out.add(v);
        }
        return out;
    }

    private static boolean sellable(VariantDto v) {
        return !v.trackInventory() || v.allowBackorder() || v.available() > 0;
    }

    static String sort(String sort) {
        String[] known = {"relevance", "newest", "price", "-price", "rating", "title"};
        for (int iter = 0; iter < known.length; iter++) {
            if (known[iter].equals(sort)) {
                return sort;
            }
        }
        return "relevance";
    }

    public static String storeUrl(String handle) {
        return "/s/" + SiteUrls.encode(handle);
    }

    static String collectionUrl(String handle, String collection) {
        return storeUrl(handle) + "/collections/" + SiteUrls.encode(collection);
    }

    static String productUrl(String handle, String product) {
        return storeUrl(handle) + "/products/" + SiteUrls.encode(product);
    }

    /// The store colour, an RGB int, as `#rrggbb`; the platform's when unset.
    static String accent(int rgb) {
        if (rgb == 0) {
            return "#4f46e5";
        }
        String hex = Integer.toHexString(0x1000000 | (rgb & 0xffffff));
        return "#" + hex.substring(1);
    }

    /// A meta description: one line, at most 160 characters.
    static String describe(String text) {
        String t = text == null ? "" : text.replace('\n', ' ').replace('\r', ' ').trim();
        return t.length() <= 160 ? t : t.substring(0, 157) + "...";
    }

    /// A rating kept in tenths as stars: 45 -> `4.5`.
    static String stars10(int tenths) {
        int t = Math.max(0, Math.min(50, tenths));
        return (t / 10) + "." + (t % 10);
    }

    static String starGlyphs(int stars) {
        int n = Math.max(0, Math.min(5, stars));
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < 5; iter++) {
            out.append(iter < n ? "\u2605" : "\u2606");
        }
        return out.toString();
    }

    static String day(Instant at) {
        String s = at == null ? "" : at.toString();
        return s.length() >= 10 ? s.substring(0, 10) : s;
    }

    static String moment(Instant at) {
        String s = at == null ? "" : at.toString();
        return s.length() >= 16 ? s.substring(0, 10) + " " + s.substring(11, 16) + " UTC" : s;
    }
}
