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
    /// Products on a page of a collection or of search results.
    static final int PAGE_SIZE = 24;
    /// The session attribute a store's cart token is kept under, plus its id.
    static final String CART_KEY = "storefront.cart.";

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
    private final Slots slots;

    public StorefrontPages(Stores stores, Catalog catalog, Carts carts, StoreRepository storeRows,
            ProductRepository products, CollectionRepository collections,
            ReviewRepository reviews, FulfillmentRepository fulfillments,
            TrackingEventRepository tracking, SiteUrls urls, Slots slots) {
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
        List<CollectionDto> cs = catalog.publicCollections(store.getHandle());
        List<LinkView> links = new ArrayList<LinkView>();
        for (int iter = 0; iter < cs.size(); iter++) {
            CollectionDto c = cs.get(iter);
            if (c.productCount() > 0) {
                links.add(new LinkView(c.title(), collectionUrl(store.getHandle(), c.handle()),
                        false));
            }
        }
        ProductPageDto featured = catalog.browse(store.getHandle(), null, null, "rating", 0, 8);
        model.addAttribute("page", page)
                .addAttribute("hero", new Art(store.getId(), store.getHeroPhotoId(),
                        store.getName()))
                .addAttribute("tagline", store.getTagline())
                .addAttribute("about", store.getDescription())
                .addAttribute("collections", links)
                .addAttribute("products", cards(store, featured.items(), null));
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
        ProductPageDto found = catalog.browse(store.getHandle(), c.getHandle(), null, order,
                Math.max(0, page), PAGE_SIZE);
        PageMeta meta = meta(store, request, path + (page > 0 ? "?page=" + page : ""));
        meta.title = c.getTitle() + " - " + store.getName();
        meta.description = describe(c.getDescription().length() > 0 ? c.getDescription()
                : c.getTitle() + " from " + store.getName());
        list(model, store, meta, found, path, order, "", c.getTitle(), c.getDescription());
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
        ProductPageDto found = catalog.browse(store.getHandle(), null, words, order,
                Math.max(0, page), PAGE_SIZE);
        String path = "/s/" + store.getHandle() + "/search";
        PageMeta meta = meta(store, request, path);
        meta.title = (words.length() > 0 ? "\"" + words + "\" - " : "Search - ") + store.getName();
        meta.description = "Search " + store.getName();
        meta.query = words;
        // Result pages are endless permutations of the catalog; the products
        // and collections are what belongs in an index.
        meta.noindex = true;
        list(model, store, meta, found, path, order, words,
                words.length() > 0 ? "Results for \"" + words + "\"" : "All products", "");
    }

    private void list(Model model, Store store, PageMeta meta, ProductPageDto found, String path,
            String order, String q, String heading, String intro) {
        String query = q.length() > 0 ? "q=" + SiteUrls.encode(q) + "&" : "";
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
        view.currency = store.getCurrency();
        List<String> images = new ArrayList<String>();
        List<ProductPhotoDto> photos = p.photos() == null ? new ArrayList<ProductPhotoDto>()
                : p.photos();
        for (int iter = 0; iter < photos.size(); iter++) {
            ProductPhotoDto ph = photos.get(iter);
            view.gallery.add(new Art(ph.id(), ph.photoId(), p.title()));
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
            vv.price = Money.format(v.priceCents(), store.getCurrency());
            vv.available = sellable(v);
            vv.selected = v == chosen;
            view.variants.add(vv);
        }
        view.hasVariants = vs.size() > 1;
        if (chosen != null) {
            view.variantId = chosen.id();
            view.variantTitle = chosen.title();
            view.price = Money.format(chosen.priceCents(), store.getCurrency());
            view.amount = Money.plain(chosen.priceCents());
            view.compareAt = chosen.compareAtCents() > chosen.priceCents()
                    ? Money.format(chosen.compareAtCents(), store.getCurrency()) : "";
            view.available = sellable(chosen);
        }
        view.availability = view.available ? "https://schema.org/InStock"
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
        ProductPageDto more = catalog.browse(store.getHandle(), null, null, "rating", 0, 5);
        view.related = cards(store, more.items(), p.id());
        while (view.related.size() > 4) {
            view.related.remove(view.related.size() - 1);
        }
        meta.jsonLd = productLd(p, view, meta, images, rs);
        model.addAttribute("page", meta).addAttribute("product", view);
    }

    private static String productLd(ProductDto p, ProductView view, PageMeta meta,
            List<String> images, List<Review> rs) {
        Map<String, Object> ld = new LinkedHashMap<String, Object>();
        ld.put("@context", "https://schema.org");
        ld.put("@type", "Product");
        ld.put("name", p.title());
        ld.put("description", meta.description);
        ld.put("url", meta.canonical);
        if (!images.isEmpty()) {
            ld.put("image", images);
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
        String token = token(request, store, false);
        CartDto dto = token == null ? null : carts.get(store.getId(), null, token);
        PageMeta meta = meta(store, request, "/s/" + store.getHandle() + "/cart");
        meta.title = "Your cart - " + store.getName();
        meta.noindex = true;
        CartView view = new CartView();
        if (dto != null) {
            remember(request, store, dto.token());
            Map<String, String> handles = handles(dto.lines());
            for (int iter = 0; iter < dto.lines().size(); iter++) {
                CartLineDto l = dto.lines().get(iter);
                CartLineView lv = new CartLineView();
                lv.id = l.id();
                lv.title = l.title();
                lv.variant = l.variantTitle();
                String ph = handles.get(l.productId());
                lv.url = ph == null ? "" : productUrl(store.getHandle(), ph);
                lv.quantity = l.quantity();
                lv.unit = Money.format(l.priceCents(), store.getCurrency());
                lv.total = Money.format(l.priceCents() * l.quantity(), store.getCurrency());
                lv.available = l.available();
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
        meta.title = "Privacy and cookies - " + store.getName();
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
        page.description = describe(store.getTagline());
        try {
            slots.decorate(page, store, request);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
        return page;
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
