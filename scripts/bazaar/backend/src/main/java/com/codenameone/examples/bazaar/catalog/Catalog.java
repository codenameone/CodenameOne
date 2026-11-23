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
package com.codenameone.examples.bazaar.catalog;

import com.codename1.appkit.server.Text;
import com.codename1.backend.Base64;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codenameone.examples.bazaar.market.Translations;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.api.BulkEditDto;
import com.codenameone.examples.bazaar.api.BundleDto;
import com.codenameone.examples.bazaar.api.BundlePartDto;
import com.codenameone.examples.bazaar.api.ChannelScheduleDto;
import com.codenameone.examples.bazaar.api.CollectionDto;
import com.codenameone.examples.bazaar.api.CountDto;
import com.codenameone.examples.bazaar.api.CategoryCountDto;
import com.codenameone.examples.bazaar.api.FacetsDto;
import com.codenameone.examples.bazaar.api.OptionDto;
import com.codenameone.examples.bazaar.api.MediaUploadDto;
import com.codenameone.examples.bazaar.api.PhotoDto;
import com.codenameone.examples.bazaar.api.ProductCardDto;
import com.codenameone.examples.bazaar.api.ProductDto;
import com.codenameone.examples.bazaar.api.ProductPageDto;
import com.codenameone.examples.bazaar.api.ProductPhotoDto;
import com.codenameone.examples.bazaar.api.StockDto;
import com.codenameone.examples.bazaar.api.VariantDto;
import com.codenameone.examples.bazaar.ops.Customs;
import com.codenameone.examples.bazaar.ops.UnitPricing;
import com.codenameone.examples.bazaar.cart.Codes;
import com.codenameone.examples.bazaar.domain.Collection;
import com.codenameone.examples.bazaar.domain.CollectionProduct;
import com.codenameone.examples.bazaar.domain.InventoryLevel;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.Photo;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.ProductOption;
import com.codenameone.examples.bazaar.domain.ProductPhoto;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.loyalty.Memberships;
import com.codenameone.examples.bazaar.repository.CollectionProductRepository;
import com.codenameone.examples.bazaar.repository.CollectionRepository;
import com.codenameone.examples.bazaar.repository.InventoryLevelRepository;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.repository.PhotoRepository;
import com.codenameone.examples.bazaar.repository.ProductOptionRepository;
import com.codenameone.examples.bazaar.repository.ProductPhotoRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;
import com.codenameone.examples.bazaar.store.StoreAccess;
import com.codenameone.examples.bazaar.store.Stores;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// The catalog: products with their options, variants and pictures, and the
/// collections they are grouped in.
///
/// Shoppers see ACTIVE products of ACTIVE stores and nothing else; a draft or
/// archived product answers 404 to them exactly as a missing one does. Staff
/// see every product of their store.
///
/// A store's catalog is small enough to search in memory: one read of its
/// products, variants, levels and collections answers a search, its facets and
/// the smart collections together, which keeps every filter consistent with
/// every count.
@Component
public class Catalog {
    public static final String DRAFT = "DRAFT";
    public static final String ACTIVE = "ACTIVE";
    public static final String ARCHIVED = "ARCHIVED";
    private static final String[] STATUSES = {DRAFT, ACTIVE, ARCHIVED};
    private static final int MAX_PHOTO_CHARS = 2800000;

    private final StoreRepository stores;
    private final ProductRepository products;
    private final VariantRepository variants;
    private final ProductOptionRepository options;
    private final ProductPhotoRepository gallery;
    private final CollectionRepository collections;
    private final CollectionProductRepository members;
    private final InventoryLevelRepository levels;
    private final LocationRepository locations;
    private final PhotoRepository photos;
    private final StoreAccess access;
    private final Stores storeService;
    private final Memberships memberships;
    private final Bundles bundles;
    private final Schedules schedules;
    private final com.codename1.backend.DataSource db;

    public Catalog(StoreRepository stores, ProductRepository products, VariantRepository variants,
            ProductOptionRepository options, ProductPhotoRepository gallery,
            CollectionRepository collections, CollectionProductRepository members,
            InventoryLevelRepository levels, LocationRepository locations,
            PhotoRepository photos, StoreAccess access, Stores storeService,
            Memberships memberships, Bundles bundles, Schedules schedules,
            com.codename1.backend.DataSource db) {
        this.memberships = memberships;
        this.bundles = bundles;
        this.schedules = schedules;
        this.db = db;
        this.stores = stores;
        this.products = products;
        this.variants = variants;
        this.options = options;
        this.gallery = gallery;
        this.collections = collections;
        this.members = members;
        this.levels = levels;
        this.locations = locations;
        this.photos = photos;
        this.access = access;
        this.storeService = storeService;
    }

    /// Field-injected so the constructor stays as it was.
    @Autowired
    private CatalogFiles catalogFiles;

    /// Product and collection text in the shopper's language. Field-injected
    /// like the files.
    @Autowired
    private com.codenameone.examples.bazaar.market.ProductText productText;
    /// Forgets the translations of a picture or collection that is deleted.
    @Autowired
    private com.codenameone.examples.bazaar.market.Translations translations;


    // ------------------------------------------------------------ the snapshot

    /// One store's catalog, read once for a search.
    private final class Snapshot {
        List<Product> list;
        /// Product id to its title in the shopper's language, for a public
        /// page whose shopper reads one; null shows the originals.
        Map<String, String> titles;
        final Map<String, List<Variant>> byProduct;
        final Map<String, Integer> available = new HashMap<String, Integer>();
        final Map<String, List<String>> membership = new HashMap<String, List<String>>();
        final List<Collection> allCollections;
        /// Bundle variants to their components; a bundle's availability is
        /// what its components make ([Bundles#available]).
        final Map<String, List<Bundles.Part>> bundleParts;

        Snapshot(String storeId, String status, boolean publicView) throws IOException {
            list = status == null ? products.findByStoreIdOrderByTitleAsc(storeId)
                    : products.findByStoreIdAndStatusOrderByTitleAsc(storeId, status);
            byProduct = Search.byProduct(variants.findByStoreId(storeId));
            List<Location> places = locations.findByStoreIdOrderByPositionAsc(storeId);
            List<String> counted = new ArrayList<String>();
            for (int iter = 0; iter < places.size(); iter++) {
                if (!publicView || places.get(iter).isActive()) {
                    counted.add(places.get(iter).getId());
                }
            }
            List<InventoryLevel> all = levels.findByStoreId(storeId);
            for (int iter = 0; iter < all.size(); iter++) {
                InventoryLevel level = all.get(iter);
                if (counted.contains(level.getLocationId())) {
                    Integer had = available.get(level.getVariantId());
                    available.put(level.getVariantId(), Integer.valueOf((had == null ? 0
                            : had.intValue()) + level.getOnHand() - level.getCommitted()));
                }
            }
            bundleParts = bundles.byVariant(storeId);
            Set<String> countedSet = new HashSet<String>(counted);
            for (Map.Entry<String, List<Bundles.Part>> e : bundleParts.entrySet()) {
                available.put(e.getKey(), Integer.valueOf(bundles.available(e.getValue(),
                        countedSet)));
            }
            allCollections = collections.findByStoreIdOrderByPositionAsc(storeId);
            List<Product> everything = status == null ? list
                    : products.findByStoreIdOrderByTitleAsc(storeId);
            for (int iter = 0; iter < allCollections.size(); iter++) {
                Collection collection = allCollections.get(iter);
                List<String> ids = productIds(collection, everything, byProduct);
                for (int p = 0; p < ids.size(); p++) {
                    List<String> in = membership.get(ids.get(p));
                    if (in == null) {
                        in = new ArrayList<String>();
                        membership.put(ids.get(p), in);
                    }
                    in.add(collection.getId());
                }
            }
        }

        /// Drops what the signed-in shopper (a guest when there is none)
        /// may not see: members-only products of clubs they are not in, and
        /// products whose online store window is closed ([Schedules]).
        Snapshot forViewer(String storeId) throws IOException {
            List<String> ids = new ArrayList<String>();
            for (int iter = 0; iter < list.size(); iter++) {
                ids.add(list.get(iter).getId());
            }
            Set<String> hide = new HashSet<String>(memberships.hidden(storeId, ids, membership,
                    memberships.of(storeId, Codes.callerOrNull(), Instant.now())));
            hide.addAll(schedules.hidden(storeId, Schedules.ONLINE));
            if (!hide.isEmpty()) {
                List<Product> kept = new ArrayList<Product>();
                for (int iter = 0; iter < list.size(); iter++) {
                    if (!hide.contains(list.get(iter).getId())) {
                        kept.add(list.get(iter));
                    }
                }
                list = kept;
            }
            return this;
        }

        List<Variant> variantsOf(String productId) {
            List<Variant> found = byProduct.get(productId);
            return found == null ? new ArrayList<Variant>() : found;
        }

        long[] range(Product product) {
            return Search.priceRange(variantsOf(product.getId()));
        }

        boolean matches(Search search, Product product, String skip) {
            long[] range = range(product);
            return search.matches(product, tags(product.getTags()), range[0], range[1],
                    membership, skip);
        }

        ProductCardDto card(Product product) {
            List<Variant> mine = variantsOf(product.getId());
            long[] range = Search.priceRange(mine);
            long compare = 0;
            boolean tracked = false;
            int stock = 0;
            // The unit price of the cheapest variant sold by measure.
            long[] unit = {-1, Long.MAX_VALUE};
            String unitPer = "";
            for (int iter = 0; iter < mine.size(); iter++) {
                Variant variant = mine.get(iter);
                long u = UnitPricing.unitPriceCents(variant.getPriceCents(),
                        variant.getUnitQuantity(), variant.getUnitName(),
                        variant.getUnitReference(), variant.getUnitReferenceName());
                if (u >= 0 && variant.getPriceCents() < unit[1]) {
                    unit[0] = u;
                    unit[1] = variant.getPriceCents();
                    unitPer = UnitPricing.per(variant.getUnitReference(),
                            variant.getUnitReferenceName());
                }
            }
            for (int iter = 0; iter < mine.size(); iter++) {
                Variant variant = mine.get(iter);
                compare = Math.max(compare, variant.getCompareAtCents());
                if (variant.isTrackInventory() || bundleParts.containsKey(variant.getId())) {
                    tracked = true;
                    Integer n = available.get(variant.getId());
                    stock += n == null ? 0 : Math.max(0, n.intValue());
                }
            }
            String title = titles == null ? null : titles.get(product.getId());
            return new ProductCardDto(product.getId(), product.getStoreId(), product.getHandle(),
                    title == null ? product.getTitle() : title, product.getVendor(), product.getProductType(),
                    tags(product.getTags()), product.getStatus(), product.getPhotoId(), range[0],
                    range[1], compare, tracked ? stock : -1, rating(product),
                    (int) product.getRatingCount(), mine.size(), unit[0] < 0 ? -1 : unit[0],
                    unit[0] < 0 ? "" : unitPer);
        }
    }

    /// The products of a collection, in order: the hand-placed ones of a
    /// manual collection, the matching ones (by title) of a smart one.
    private List<String> productIds(Collection collection, List<Product> everything,
            Map<String, List<Variant>> byProduct) {
        List<String> ids = new ArrayList<String>();
        if (collection.getRuleText().length() == 0) {
            List<CollectionProduct> placed =
                    members.findByCollectionIdOrderByPositionAsc(collection.getId());
            for (int iter = 0; iter < placed.size(); iter++) {
                ids.add(placed.get(iter).getProductId());
            }
            return ids;
        }
        if (!Rules.valid(collection.getRuleText())) {
            return ids;
        }
        Rules rules = Rules.parse(collection.getRuleText());
        for (int iter = 0; iter < everything.size(); iter++) {
            Product product = everything.get(iter);
            List<Variant> mine = byProduct.get(product.getId());
            long[] range = Search.priceRange(mine == null ? new ArrayList<Variant>() : mine);
            if (rules.matches(tags(product.getTags()), product.getProductType(),
                    product.getVendor(), range[0], range[1])) {
                ids.add(product.getId());
            }
        }
        return ids;
    }

    // ---------------------------------------------------------------- public

    @Transactional(readOnly = true)
    public ProductPageDto products(String handle, String collection, Search search)
            throws IOException {
        Store store = storeService.activeStore(handle);
        Snapshot snapshot = new Snapshot(store.getId(), ACTIVE, true).forViewer(store.getId());
        search.collectionId = collectionId(snapshot, collection, true);
        String locale = search.locale == null ? productText.shopperLocale(store.getId())
                : search.locale;
        if (locale != null && locale.length() > 0) {
            // Shown and matched in the shopper's language: a search for the
            // German word finds the product a German shopper reads.
            snapshot.titles = productText.titles(store.getId(), locale);
            search.titles = snapshot.titles;
        }
        search.synonyms = Search.synonymGroups(store.getSearchSynonyms());
        return page(snapshot, search);
    }

    /// Predictive search: up to `limit` suggestions for what a shopper has
    /// typed so far, each `{kind, title, handle}` with kind `product` or
    /// `collection`. A product matches when a word of its title or one of
    /// its tags starts with `prefix`; a published collection when a word of
    /// its title does. Products come first, the merchant's boost ahead.
    @Transactional(readOnly = true)
    public List<String[]> suggest(String handle, String prefix, int limit) throws IOException {
        List<String[]> out = new ArrayList<String[]>();
        String typed = prefix == null ? "" : prefix.trim();
        if (typed.length() == 0) {
            return out;
        }
        Store store = storeService.activeStore(handle);
        Snapshot snapshot = new Snapshot(store.getId(), ACTIVE, true).forViewer(store.getId());
        List<Product> hits = new ArrayList<Product>();
        for (int iter = 0; iter < snapshot.list.size(); iter++) {
            Product p = snapshot.list.get(iter);
            boolean hit = wordStarts(p.getTitle(), typed);
            List<String> tags = tags(p.getTags());
            for (int t = 0; t < tags.size() && !hit; t++) {
                hit = wordStarts(tags.get(t), typed);
            }
            if (hit) {
                int at = hits.size();
                while (at > 0 && hits.get(at - 1).getSearchBoost() < p.getSearchBoost()) {
                    at--;
                }
                hits.add(at, p);
            }
        }
        for (int iter = 0; iter < hits.size() && out.size() < limit; iter++) {
            out.add(new String[] {"product", hits.get(iter).getTitle(), hits.get(iter).getHandle()});
        }
        for (int iter = 0; iter < snapshot.allCollections.size() && out.size() < limit; iter++) {
            Collection c = snapshot.allCollections.get(iter);
            if (c.isPublished() && wordStarts(c.getTitle(), typed)) {
                out.add(new String[] {"collection", c.getTitle(), c.getHandle()});
            }
        }
        return out;
    }

    /// The lowest and highest variant price of `product`, in cents.
    @Transactional(readOnly = true)
    public long[] priceRange(Product product) throws IOException {
        List<Variant> mine = Search.byProduct(variants.findByStoreId(product.getStoreId()))
                .get(product.getId());
        return Search.priceRange(mine == null ? new ArrayList<Variant>() : mine);
    }

    /// Whether some word of `text` starts with `prefix`, ignoring case.
    static boolean wordStarts(String text, String prefix) {
        if (text == null) {
            return false;
        }
        for (int iter = 0; iter + prefix.length() <= text.length(); iter++) {
            boolean start = iter == 0 || !Character.isLetterOrDigit(text.charAt(iter - 1));
            if (start && text.regionMatches(true, iter, prefix, 0, prefix.length())) {
                return true;
            }
        }
        return false;
    }

    /// A page of an active store's active products for the server-rendered
    /// storefront: the same search the app's catalog runs, from plain values.
    ///
    /// @param handle the store's handle
    /// @param collection a published collection's handle, or null for all
    /// @param q the words to match, or null
    /// @param sort `relevance`, `title`, `newest`, `price`, `-price` or `rating`
    /// @param page the zero-based page
    /// @param size the page size, 1 to 100
    /// @return the page
    @Transactional(readOnly = true)
    public ProductPageDto browse(String handle, String collection, String q, String sort,
            int page, int size) throws IOException {
        return browse(handle, collection, q, sort, page, size, 0);
    }

    /// [#browse(String, String, String, String, int, int)] narrowed to a
    /// taxonomy category (0 for any); a top-level category takes the ones
    /// under it.
    @Transactional(readOnly = true)
    public ProductPageDto browse(String handle, String collection, String q, String sort,
            int page, int size, int category) throws IOException {
        return browse(handle, collection, q, sort, page, size, category, null);
    }

    /// The top-level taxonomy categories of an active store's products (in a
    /// published collection, or all for null) that match `q`, each as
    /// `{id, count}`, most products first: the storefront's category filter.
    @Transactional(readOnly = true)
    public List<int[]> categoryCounts(String handle, String collection, String q)
            throws IOException {
        Store store = storeService.activeStore(handle);
        Snapshot snapshot = new Snapshot(store.getId(), ACTIVE, true).forViewer(store.getId());
        Search search = Search.of(q, null, null, null, null, null, null, null, null);
        search.collectionId = collectionId(snapshot, collection, true);
        List<int[]> out = new ArrayList<int[]>();
        for (int iter = 0; iter < snapshot.list.size(); iter++) {
            Product product = snapshot.list.get(iter);
            int top = Taxonomy.top(product.getCategoryId());
            if (top <= 0 || !snapshot.matches(search, product, Search.CATEGORY)) {
                continue;
            }
            int[] seen = null;
            for (int c = 0; c < out.size() && seen == null; c++) {
                if (out.get(c)[0] == top) {
                    seen = out.get(c);
                }
            }
            if (seen == null) {
                out.add(new int[] {top, 1});
            } else {
                seen[1]++;
            }
        }
        // An insertion sort, most products first then taxonomy name.
        for (int i = 1; i < out.size(); i++) {
            int[] item = out.get(i);
            int j = i - 1;
            while (j >= 0 && (out.get(j)[1] < item[1] || out.get(j)[1] == item[1]
                    && Taxonomy.path(out.get(j)[0]).compareTo(Taxonomy.path(item[0])) > 0)) {
                out.set(j + 1, out.get(j));
                j--;
            }
            out.set(j + 1, item);
        }
        return out;
    }

    /// [#browse] for a shopper reading `locale` (null for the originals):
    /// titles shown, and words matched, in that language too.
    @Transactional(readOnly = true)
    public ProductPageDto browse(String handle, String collection, String q, String sort,
            int page, int size, String locale) throws IOException {
        return browse(handle, collection, q, sort, page, size, 0, locale);
    }

    /// [#browse] narrowed to a taxonomy category (0 for any) and read in
    /// `locale` (null for the originals).
    @Transactional(readOnly = true)
    public ProductPageDto browse(String handle, String collection, String q, String sort,
            int page, int size, int category, String locale) throws IOException {
        Search search = Search.of(q, null, null, null, null, null, sort,
                Integer.toString(page), Integer.toString(size));
        search.category = Math.max(0, category);
        search.locale = locale == null ? "" : locale;
        return products(handle, collection, search);
    }


    @Transactional(readOnly = true)
    public FacetsDto facets(String handle, String collection, Search search) throws IOException {
        Store store = storeService.activeStore(handle);
        Snapshot snapshot = new Snapshot(store.getId(), ACTIVE, true).forViewer(store.getId());
        search.collectionId = collectionId(snapshot, collection, true);
        String locale = search.locale == null ? productText.shopperLocale(store.getId())
                : search.locale;
        if (locale != null && locale.length() > 0) {
            search.titles = productText.titles(store.getId(), locale);
        }
        Map<String, Integer> typeCounts = new HashMap<String, Integer>();
        Map<String, Integer> vendorCounts = new HashMap<String, Integer>();
        Map<String, Integer> tagCounts = new HashMap<String, Integer>();
        Map<String, Integer> collectionCounts = new HashMap<String, Integer>();
        List<String> typeOrder = new ArrayList<String>();
        List<String> vendorOrder = new ArrayList<String>();
        List<String> tagOrder = new ArrayList<String>();
        List<String> collectionOrder = new ArrayList<String>();
        Map<String, String> collectionHandles = new HashMap<String, String>();
        for (int iter = 0; iter < snapshot.allCollections.size(); iter++) {
            Collection c = snapshot.allCollections.get(iter);
            collectionHandles.put(c.getId(), c.isPublished() ? c.getHandle() : "");
        }
        long lo = Long.MAX_VALUE;
        long hi = 0;
        int total = 0;
        List<int[]> categoryCounts = new ArrayList<int[]>();
        for (int iter = 0; iter < snapshot.list.size(); iter++) {
            Product product = snapshot.list.get(iter);
            int top = Taxonomy.top(product.getCategoryId());
            if (top > 0 && snapshot.matches(search, product, Search.CATEGORY)) {
                int[] seen = null;
                for (int c = 0; c < categoryCounts.size() && seen == null; c++) {
                    if (categoryCounts.get(c)[0] == top) {
                        seen = categoryCounts.get(c);
                    }
                }
                if (seen == null) {
                    categoryCounts.add(new int[] {top, 1});
                } else {
                    seen[1]++;
                }
            }
            if (snapshot.matches(search, product, Search.TYPE)) {
                Search.count(typeCounts, typeOrder, product.getProductType());
            }
            if (snapshot.matches(search, product, Search.VENDOR)) {
                Search.count(vendorCounts, vendorOrder, product.getVendor());
            }
            if (snapshot.matches(search, product, Search.TAG)) {
                List<String> tags = tags(product.getTags());
                for (int t = 0; t < tags.size(); t++) {
                    Search.count(tagCounts, tagOrder, tags.get(t));
                }
            }
            if (snapshot.matches(search, product, Search.COLLECTION)) {
                List<String> in = snapshot.membership.get(product.getId());
                for (int c = 0; in != null && c < in.size(); c++) {
                    Search.count(collectionCounts, collectionOrder,
                            collectionHandles.get(in.get(c)));
                }
            }
            if (snapshot.matches(search, product, Search.PRICE)) {
                long[] range = snapshot.range(product);
                lo = Math.min(lo, range[0]);
                hi = Math.max(hi, range[1]);
            }
            if (snapshot.matches(search, product, null)) {
                total++;
            }
        }
        return new FacetsDto(Search.list(typeCounts, typeOrder),
                Search.list(vendorCounts, vendorOrder), Search.list(tagCounts, tagOrder),
                Search.list(collectionCounts, collectionOrder), lo == Long.MAX_VALUE ? 0 : lo,
                hi, total, categoryList(categoryCounts));
    }

    /// Top-level categories as the app's chips read them, most products first.
    private static List<CategoryCountDto> categoryList(List<int[]> counts) {
        List<CategoryCountDto> out = new ArrayList<CategoryCountDto>();
        for (int iter = 0; iter < counts.size(); iter++) {
            int[] c = counts.get(iter);
            CategoryCountDto d = new CategoryCountDto(c[0], Taxonomy.name(c[0]), c[1]);
            int at = 0;
            while (at < out.size() && (out.get(at).count() > d.count()
                    || out.get(at).count() == d.count()
                    && out.get(at).name().compareTo(d.name()) <= 0)) {
                at++;
            }
            out.add(at, d);
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<CollectionDto> publicCollections(String handle) throws IOException {
        return publicCollections(handle, null);
    }

    /// The published collections, titled in `locale`: null asks the request
    /// (`ProductText#shopperLocale`), empty keeps the originals.
    @Transactional(readOnly = true)
    public List<CollectionDto> publicCollections(String handle, String locale)
            throws IOException {
        Store store = storeService.activeStore(handle);
        String shown = locale == null ? productText.shopperLocale(store.getId()) : locale;
        Map<String, Map<String, String>> tr = shown == null || shown.length() == 0
                ? new HashMap<String, Map<String, String>>()
                : productText.collections(store.getId(), shown);
        Snapshot snapshot = new Snapshot(store.getId(), ACTIVE, true).forViewer(store.getId());
        List<CollectionDto> out = new ArrayList<CollectionDto>();
        for (int iter = 0; iter < snapshot.allCollections.size(); iter++) {
            Collection collection = snapshot.allCollections.get(iter);
            if (collection.isPublished()) {
                out.add(com.codenameone.examples.bazaar.market.ProductText.localize(
                        toDto(collection, visibleIds(snapshot, collection)),
                        tr.get(collection.getId())));
            }

        }
        return out;
    }

    /// The ids of the products of the snapshot in a collection.
    private static List<String> visibleIds(Snapshot snapshot, Collection collection) {
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < snapshot.list.size(); iter++) {
            List<String> in = snapshot.membership.get(snapshot.list.get(iter).getId());
            if (in != null && in.contains(collection.getId())) {
                ids.add(snapshot.list.get(iter).getId());
            }
        }
        return ids;
    }

    /// An ACTIVE product of an ACTIVE store; 404 for anything else.
    @Transactional(readOnly = true)
    public ProductDto publicProduct(String id) throws IOException {
        Product product = id == null ? null : products.findById(id);
        if (product == null || !ACTIVE.equals(product.getStatus())) {
            throw new ResponseStatusException(404, "No such product");
        }
        Store store = stores.findById(product.getStoreId());
        if (store == null || !Stores.ACTIVE.equals(store.getStatus())) {
            throw new ResponseStatusException(404, "No such product");
        }
        requireVisible(product);
        return detail(product, false);
    }

    @Transactional(readOnly = true)
    public ProductDto publicProductByHandle(String handle, String productHandle)
            throws IOException {
        Store store = storeService.activeStore(handle);
        Product product = productHandle == null ? null
                : products.findByStoreIdAndHandle(store.getId(), productHandle);
        if (product == null || !ACTIVE.equals(product.getStatus())) {
            throw new ResponseStatusException(404, "No such product");
        }
        requireVisible(product);
        return detail(product, false);
    }

    /// 404 for a members-only product the caller's clubs do not open: to
    /// anyone else it does not exist, as in the lists.
    private void requireVisible(Product product) throws IOException {
        if (schedules.isHidden(product.getId(), Schedules.ONLINE)) {
            // Before its online store window opens, or after it closes, a
            // product is not there at all.
            throw new ResponseStatusException(404, "No such product");
        }
        if (!memberships.anyRules(product.getStoreId())) {
            return;
        }
        Snapshot snapshot = new Snapshot(product.getStoreId(), ACTIVE, true).forViewer(product.getStoreId());
        for (int iter = 0; iter < snapshot.list.size(); iter++) {
            if (product.getId().equals(snapshot.list.get(iter).getId())) {
                return;
            }
        }
        throw new ResponseStatusException(404, "No such product");
    }

    /// A stored picture, if it belongs to an active store.
    @Transactional(readOnly = true)
    public PhotoDto photo(String photoId) throws IOException {
        Photo photo = photoId == null || photoId.length() > 32 ? null : photos.findById(photoId);
        Store owner = photo == null ? null : stores.findById(photo.getOwnerId());
        if (owner == null || !Stores.ACTIVE.equals(owner.getStatus())) {
            throw new ResponseStatusException(404, "No such photo");
        }
        return new PhotoDto(photo.getId(), photo.getContentType(), Base64.encode(photo.getData()));
    }

    /// A stored picture of an active store, as stored; 404 for anything else.
    /// The storefront's pages point `<img>` at it, which a JSON answer cannot be.
    @Transactional(readOnly = true)
    public Photo publicPhoto(String photoId) throws IOException {
        Photo photo = photoId == null || photoId.length() > 32 ? null : photos.findById(photoId);
        Store owner = photo == null ? null : stores.findById(photo.getOwnerId());
        if (owner == null || !Stores.ACTIVE.equals(owner.getStatus())) {
            throw new ResponseStatusException(404, "No such photo");
        }
        return photo;
    }

    // -------------------------------------------------------------- merchant

    @Transactional(readOnly = true)
    public ProductPageDto merchantProducts(String username, String storeId, String status,
            String collection, Search search) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        String wanted = Search.text(status);
        Snapshot snapshot = new Snapshot(storeId,
                wanted.length() == 0 ? null : Text.oneOf(wanted, STATUSES, ACTIVE, "the status"),
                false);
        search.collectionId = collectionId(snapshot, collection, false);
        return page(snapshot, search);
    }

    /// The margin of each product of the store with a cost recorded, in basis
    /// points: what its variants' prices leave over their costs, over the
    /// variants that have a cost. A product with none is left out.
    @Transactional(readOnly = true)
    public Map<String, Integer> margins(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Map<String, long[]> sums = new HashMap<String, long[]>();
        List<Variant> all = variants.findByStoreId(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            Variant v = all.get(iter);
            if (v.getCostCents() <= 0 || v.getPriceCents() <= 0) {
                continue;
            }
            long[] s = sums.get(v.getProductId());
            if (s == null) {
                s = new long[2];
                sums.put(v.getProductId(), s);
            }
            s[0] += v.getPriceCents();
            s[1] += v.getCostCents();
        }
        Map<String, Integer> out = new HashMap<String, Integer>();
        for (Map.Entry<String, long[]> e : sums.entrySet()) {
            out.put(e.getKey(), Integer.valueOf(com.codenameone.examples.bazaar.ops.Margins
                    .marginBps(e.getValue()[0], e.getValue()[1])));
        }
        return out;
    }

    /// Whether a product is a bundle, and its components with what each has
    /// available and how many bundles they make.
    @Transactional(readOnly = true)
    public BundleDto bundle(String username, String storeId, String productId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Product product = product(storeId, productId);
        return bundleOf(product);
    }

    private BundleDto bundleOf(Product product) throws IOException {
        List<Bundles.Part> parts = bundles.parts(product.getId());
        List<BundlePartDto> out = new ArrayList<BundlePartDto>();
        for (int iter = 0; iter < parts.size(); iter++) {
            Bundles.Part part = parts.get(iter);
            Variant v = variants.findById(part.variantId);
            Product owner = v == null ? null : products.findById(v.getProductId());
            List<Bundles.Part> single = new ArrayList<Bundles.Part>();
            single.add(new Bundles.Part(part.variantId, 1));
            out.add(new BundlePartDto(part.variantId, v == null ? "" : v.getSku(), part.quantity,
                    owner == null ? "" : Bundles.title(owner.getTitle(), v.getTitle()),
                    v == null ? 0 : bundles.available(single, null)));
        }
        return new BundleDto(product.getId(), Bundles.BUNDLE.equals(product.getKind()), out,
                parts.isEmpty() ? 0 : bundles.available(parts, null));
    }

    /// The variants of the store a bundle may contain -- those of products
    /// that are not bundles themselves -- as id and name, by name.
    @Transactional(readOnly = true)
    public List<String[]> bundleChoices(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List rs = db.query("SELECT v.id, v.title AS vtitle, v.sku, p.title AS ptitle FROM variant v "
                + "JOIN product p ON p.id = v.product_id WHERE v.store_id = ? AND p.kind <> ? AND "
                + "p.status <> 'ARCHIVED' ORDER BY p.title, v.position", new Object[] {storeId,
                    Bundles.BUNDLE});
        List<String[]> out = new ArrayList<String[]>();
        for (int iter = 0; iter < rs.size(); iter++) {
            Map r = (Map) rs.get(iter);
            String sku = String.valueOf(r.get("sku") == null ? "" : r.get("sku"));
            out.add(new String[] {String.valueOf(r.get("id")), Bundles.title(
                    String.valueOf(r.get("ptitle")), String.valueOf(r.get("vtitle") == null ? ""
                        : r.get("vtitle"))) + (sku.length() > 0 ? " (" + sku + ")" : "")});
        }
        return out;
    }

    /// Makes a product a bundle of the components sent, or a standard product
    /// again. A product is a bundle exactly when it has components: the kind
    /// and the rows change together, here only. Refused while an open order
    /// holds the bundle unshipped, since shipping and releasing it move the
    /// components it has when they happen.
    @Transactional(rollbackFor = IOException.class)
    public BundleDto setBundle(String username, String storeId, String productId,
            BundleDto wanted) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Product product = product(storeId, productId);
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        if (db.queryOne("SELECT l.id FROM order_line l JOIN store_order o ON o.id = l.order_id "
                + "JOIN variant v ON v.id = l.variant_id WHERE v.product_id = ? AND o.status = 'OPEN' "
                + "AND l.quantity > l.fulfilled_qty LIMIT 1", new Object[] {productId}) != null) {
            throw new ResponseStatusException(409, "An open order holds this product unshipped; "
                    + "change its components once it has shipped or been cancelled");
        }
        List<Bundles.Part> parts = new ArrayList<Bundles.Part>();
        if (wanted.bundle()) {
            List<BundlePartDto> sent = wanted.parts() == null ? new ArrayList<BundlePartDto>()
                    : wanted.parts();
            for (int iter = 0; iter < sent.size(); iter++) {
                BundlePartDto p = sent.get(iter);
                String id = p.variantId();
                if ((id == null || id.length() == 0) && p.sku() != null
                        && p.sku().trim().length() > 0) {
                    Map found = db.queryOne("SELECT id FROM variant WHERE store_id = ? AND sku = ?",
                            new Object[] {storeId, p.sku().trim()});
                    if (found == null) {
                        throw new ResponseStatusException(404, "No variant has the SKU "
                                + p.sku().trim());
                    }
                    id = String.valueOf(found.get("id"));
                }
                parts.add(new Bundles.Part(id, p.quantity()));
            }
            if (parts.isEmpty()) {
                throw new ResponseStatusException(400, "A bundle needs at least one component");
            }
            if (bundles.isComponent(productId)) {
                throw new ResponseStatusException(400, "This product is part of a bundle, so it "
                        + "cannot be a bundle itself");
            }
        }
        bundles.set(storeId, productId, parts);
        product.setKind(parts.isEmpty() ? Bundles.STANDARD : Bundles.BUNDLE);
        product.setUpdatedAt(Instant.now());
        products.save(product);
        return bundleOf(product);
    }

    /// A product's publishing window on each channel of its store.
    @Transactional(readOnly = true)
    public List<ChannelScheduleDto> schedule(String username, String storeId, String productId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        product(storeId, productId);
        return schedules.windows(storeId, productId);
    }

    /// Sets a product's window on one channel; both bounds 0 removes it.
    @Transactional(rollbackFor = IOException.class)
    public List<ChannelScheduleDto> setSchedule(String username, String storeId, String productId,
            ChannelScheduleDto wanted) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        product(storeId, productId);
        if (wanted == null || wanted.channel() == null) {
            throw new ResponseStatusException(400, "Say which channel");
        }
        schedules.set(storeId, productId, wanted.channel(), wanted.publishAt(),
                wanted.unpublishAt(), System.currentTimeMillis());
        return schedules.windows(storeId, productId);
    }

    @Transactional(readOnly = true)
    public ProductDto merchantProduct(String username, String storeId, String productId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        return detail(product(storeId, productId), true);
    }

    @Transactional(rollbackFor = IOException.class)
    public ProductDto create(String username, String storeId, ProductDto wanted)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        Product product = new Product();
        product.setId(Ids.next());
        product.setStoreId(storeId);
        product.setCreatedAt(Instant.now());
        product.setStatus(DRAFT);
        fill(product, wanted);
        product.setHandle(uniqueHandle(storeId, wanted.handle(), product.getTitle(), null));
        products.insert(product);
        saveChildren(product, wanted);
        return detail(products.findById(product.getId()), true);
    }

    @Transactional(rollbackFor = IOException.class)
    public ProductDto update(String username, String storeId, String productId,
            ProductDto wanted) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Product product = product(storeId, productId);
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        fill(product, wanted);
        if (wanted.handle() != null && wanted.handle().trim().length() > 0
                && !wanted.handle().trim().equals(product.getHandle())) {
            product.setHandle(uniqueHandle(storeId, wanted.handle(), product.getTitle(),
                    product.getId()));
        }
        products.save(product);
        saveChildren(product, wanted);
        return detail(products.findById(productId), true);
    }

    @Transactional(rollbackFor = IOException.class)
    public ProductDto archive(String username, String storeId, String productId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Product product = product(storeId, productId);
        product.setStatus(ARCHIVED);
        product.setUpdatedAt(Instant.now());
        products.save(product);
        return detail(product, true);
    }

    /// A DRAFT copy: the same options, variants (new SKUs, no barcodes),
    /// gallery and manual collections, and no stock.
    @Transactional(rollbackFor = IOException.class)
    public ProductDto duplicate(String username, String storeId, String productId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Product source = product(storeId, productId);
        Product copy = new Product();
        copy.setId(Ids.next());
        copy.setStoreId(storeId);
        copy.setTitle(clip(source.getTitle() + " (copy)", 200));
        copy.setHandle(uniqueHandle(storeId, source.getHandle() + "-copy", copy.getTitle(), null));
        copy.setDescription(source.getDescription());
        copy.setVendor(source.getVendor());
        copy.setProductType(source.getProductType());
        copy.setTags(source.getTags());
        copy.setStatus(DRAFT);
        copy.setSeoTitle(source.getSeoTitle());
        copy.setSeoDescription(source.getSeoDescription());
        copy.setPhotoId(source.getPhotoId());
        // A copied bundle is a bundle of nothing until its components are
        // set; the category goes across.
        copy.setCategoryId(source.getCategoryId());
        copy.setCreatedAt(Instant.now());
        copy.setUpdatedAt(Instant.now());
        products.insert(copy);
        List<ProductOption> sourceOptions = options.findByProductIdOrderByPositionAsc(productId);
        for (int iter = 0; iter < sourceOptions.size(); iter++) {
            ProductOption o = sourceOptions.get(iter);
            ProductOption n = new ProductOption();
            n.setId(Ids.next());
            n.setProductId(copy.getId());
            n.setName(o.getName());
            n.setOptionValues(o.getOptionValues());
            n.setPosition(o.getPosition());
            options.insert(n);
        }
        List<Variant> sourceVariants = variants.findByProductIdOrderByPositionAsc(productId);
        List<Location> places = locations.findByStoreIdOrderByPositionAsc(storeId);
        List<InventoryLevel> made = new ArrayList<InventoryLevel>();
        for (int iter = 0; iter < sourceVariants.size(); iter++) {
            Variant v = sourceVariants.get(iter);
            Variant n = new Variant();
            n.setId(Ids.next());
            n.setProductId(copy.getId());
            n.setStoreId(storeId);
            n.setTitle(v.getTitle());
            n.setOption1(v.getOption1());
            n.setOption2(v.getOption2());
            n.setOption3(v.getOption3());
            n.setSku(uniqueSku(storeId, v.getSku().length() == 0 ? "" : v.getSku() + "-COPY"));
            n.setPriceCents(v.getPriceCents());
            n.setCompareAtCents(v.getCompareAtCents());
            n.setCostCents(v.getCostCents());
            n.setWeightGrams(v.getWeightGrams());
            n.setTaxable(v.isTaxable());
            n.setTrackInventory(v.isTrackInventory());
            n.setAllowBackorder(v.isAllowBackorder());
            n.setPosition(v.getPosition());
            n.setDigital(v.isDigital());
            n.setHsCode(v.getHsCode());
            n.setOriginCountry(v.getOriginCountry());
            n.setUnitQuantity(v.getUnitQuantity());
            n.setUnitName(v.getUnitName());
            n.setUnitReference(v.getUnitReference());
            n.setUnitReferenceName(v.getUnitReferenceName());
            variants.insert(n);
            for (int p = 0; p < places.size(); p++) {
                made.add(Inventory.newLevel(storeId, n.getId(), places.get(p).getId(), 0));
            }
        }
        levels.saveAll(made);
        List<ProductPhoto> pictures = gallery.findByProductIdOrderByPositionAsc(productId);
        for (int iter = 0; iter < pictures.size(); iter++) {
            ProductPhoto p = pictures.get(iter);
            ProductPhoto n = new ProductPhoto();
            n.setId(Ids.next());
            n.setProductId(copy.getId());
            n.setPhotoId(p.getPhotoId());
            n.setAlt(p.getAlt());
            n.setPosition(p.getPosition());
            n.setKind(p.getKind());
            n.setUrl(p.getUrl());
            gallery.insert(n);
        }
        List<CollectionProduct> placed = members.findByProductId(productId);
        for (int iter = 0; iter < placed.size(); iter++) {
            place(placed.get(iter).getCollectionId(), copy.getId());
        }
        return detail(products.findById(copy.getId()), true);
    }

    @Transactional(rollbackFor = IOException.class)
    public CountDto bulkEdit(String username, String storeId, BulkEditDto edit)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (edit == null || edit.productIds() == null || edit.productIds().isEmpty()) {
            throw new ResponseStatusException(400, "Choose the products to change");
        }
        String status = Text.oneOf(edit.status(), STATUSES, "", "the status");
        List<Product> chosen = new ArrayList<Product>();
        for (int iter = 0; iter < edit.productIds().size(); iter++) {
            chosen.add(product(storeId, edit.productIds().get(iter)));
        }
        for (int iter = 0; iter < chosen.size(); iter++) {
            Product product = chosen.get(iter);
            if (status.length() > 0) {
                product.setStatus(status);
            }
            product.setUpdatedAt(Instant.now());
            products.save(product);
            if (edit.priceCents() > 0 || edit.adjustBasisPoints() != 0) {
                List<Variant> mine = variants.findByProductIdOrderByPositionAsc(product.getId());
                for (int v = 0; v < mine.size(); v++) {
                    Variant variant = mine.get(v);
                    long price = edit.priceCents() > 0 ? edit.priceCents()
                            : variant.getPriceCents();
                    if (edit.adjustBasisPoints() != 0) {
                        price = price + price * edit.adjustBasisPoints() / 10000L;
                    }
                    variant.setPriceCents(Math.max(0, price));
                    variants.save(variant);
                }
            }
        }
        return new CountDto(chosen.size());
    }

    /// Most entries a gallery holds, pictures, videos and models together.
    public static final int MAX_GALLERY = 30;

    /// Adds a video (an uploaded MP4 or WebM, or an `https` link) or a 3D
    /// model (an uploaded `.glb`) to the end of the product's gallery.
    @Transactional(rollbackFor = IOException.class)
    public ProductDto addMedia(String username, String storeId, String productId,
            MediaUploadDto media) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Product product = product(storeId, productId);
        if (media == null || media.kind() == null) {
            throw new ResponseStatusException(400, "Say what kind of media it is");
        }
        String kind = media.kind();
        if (!Media.VIDEO.equals(kind) && !Media.MODEL.equals(kind)) {
            throw new ResponseStatusException(400, "Media is a VIDEO or a MODEL");
        }
        List<ProductPhoto> pictures = gallery.findByProductIdOrderByPositionAsc(productId);
        if (pictures.size() >= MAX_GALLERY) {
            throw new ResponseStatusException(409, "A gallery holds at most " + MAX_GALLERY
                    + " entries");
        }
        String fileId = "";
        String url = "";
        boolean hasData = media.data() != null && media.data().length() > 0;
        if (hasData) {
            fileId = catalogFiles.storeBase64(storeId, kind, media.name(), media.contentType(),
                    media.data());
        } else if (Media.VIDEO.equals(kind)) {
            url = Media.httpsUrl(media.url());
            if (url == null) {
                throw new ResponseStatusException(400, "A video link is an https address");
            }
        } else {
            throw new ResponseStatusException(400, "No file");
        }
        return addMediaRow(product, kind, fileId, url, pictures.size());
    }

    /// Adds an already stored catalog file (or a link) to the gallery.
    @Transactional(rollbackFor = IOException.class)
    public ProductDto addMediaRow(Product product, String kind, String fileId, String url,
            int position) throws IOException {
        ProductPhoto row = new ProductPhoto();
        row.setId(Ids.next());
        row.setProductId(product.getId());
        row.setPhotoId(fileId);
        row.setUrl(url);
        row.setKind(kind);
        row.setAlt(clip(product.getTitle(), 200));
        row.setPosition(position);
        gallery.insert(row);
        return detail(products.findById(product.getId()), true);
    }

    /// The product a merchant may change, for the web back office's uploads.
    @Transactional(readOnly = true)
    public Product managedProduct(String username, String storeId, String productId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        return product(storeId, productId);
    }

    /// Moves a gallery entry one place towards the front (`up`) or the back.
    /// The product's main photo follows the first picture.
    @Transactional(rollbackFor = IOException.class)
    public ProductDto moveMedia(String username, String storeId, String productId,
            String entryId, boolean up) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Product product = product(storeId, productId);
        List<ProductPhoto> pictures = gallery.findByProductIdOrderByPositionAsc(productId);
        int at = -1;
        for (int iter = 0; iter < pictures.size(); iter++) {
            if (pictures.get(iter).getId().equals(entryId)) {
                at = iter;
            }
        }
        if (at < 0) {
            throw new ResponseStatusException(404, "No such photo");
        }
        int to = up ? at - 1 : at + 1;
        if (to >= 0 && to < pictures.size()) {
            ProductPhoto moved = pictures.remove(at);
            pictures.add(to, moved);
        }
        for (int iter = 0; iter < pictures.size(); iter++) {
            ProductPhoto p = pictures.get(iter);
            if (p.getPosition() != iter) {
                p.setPosition(iter);
                gallery.save(p);
            }
        }
        product.setPhotoId(Media.firstImage(pictures));
        products.save(product);
        return detail(products.findById(productId), true);
    }

    /// Stores a JPEG or PNG for the store and adds it to the product's
    /// gallery; the first picture becomes the product's main photo.
    @Transactional(rollbackFor = IOException.class)
    public ProductDto addPhoto(String username, String storeId, String productId,
            PhotoDto photo) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Product product = product(storeId, productId);
        String photoId = storePhoto(storeId, photo);
        List<ProductPhoto> pictures = gallery.findByProductIdOrderByPositionAsc(productId);
        ProductPhoto row = new ProductPhoto();
        row.setId(Ids.next());
        row.setProductId(productId);
        row.setPhotoId(photoId);
        row.setAlt(clip(product.getTitle(), 200));
        row.setPosition(pictures.size());
        gallery.insert(row);
        if (product.getPhotoId().length() == 0) {
            product.setPhotoId(photoId);
            products.save(product);
        }
        return detail(products.findById(productId), true);
    }

    /// Takes a picture out of the gallery, by the gallery row's id or the
    /// stored photo's.
    @Transactional(rollbackFor = IOException.class)
    public ProductDto removePhoto(String username, String storeId, String productId,
            String photoId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Product product = product(storeId, productId);
        List<ProductPhoto> pictures = gallery.findByProductIdOrderByPositionAsc(productId);
        String removed = null;
        for (int iter = 0; iter < pictures.size(); iter++) {
            ProductPhoto p = pictures.get(iter);
            if (p.getId().equals(photoId) || p.getPhotoId().equals(photoId)) {
                removed = p.getPhotoId();
                gallery.delete(p);
                translations.forget(storeId, Translations.IMAGE, p.getId());
                if (!Media.isImage(p) && p.getUrl().length() == 0 && p.getPhotoId().length() > 0
                        && gallery.findByPhotoId(p.getPhotoId()).isEmpty()) {
                    catalogFiles.remove(p.getPhotoId());
                }
                break;
            }
        }
        if (removed == null) {
            throw new ResponseStatusException(404, "No such photo");
        }
        if (removed.equals(product.getPhotoId())) {
            List<ProductPhoto> left = gallery.findByProductIdOrderByPositionAsc(productId);
            product.setPhotoId(Media.firstImage(left));
            products.save(product);
        }
        return detail(products.findById(productId), true);
    }

    /// Stores a picture as the store's and answers its id.
    @Transactional(rollbackFor = IOException.class)
    public String storePhoto(String storeId, PhotoDto photo) throws IOException {
        if (photo == null || photo.data() == null || photo.data().length() == 0) {
            throw new ResponseStatusException(400, "No photo");
        }
        String type = photo.contentType();
        if (!"image/jpeg".equals(type) && !"image/png".equals(type)) {
            throw new ResponseStatusException(415, "A photo is a JPEG or a PNG");
        }
        if (photo.data().length() > MAX_PHOTO_CHARS) {
            throw new ResponseStatusException(413, "That photo is too large");
        }
        for (int iter = 0; iter < photo.data().length(); iter++) {
            char c = photo.data().charAt(iter);
            if (!(c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9'
                    || c == '+' || c == '/' || c == '=')) {
                throw new ResponseStatusException(400, "The photo is not base64");
            }
        }
        byte[] bytes = Base64.decode(photo.data());
        if (bytes == null || bytes.length < 8) {
            throw new ResponseStatusException(400, "The photo is not base64");
        }
        // Sniffed by its bytes, held to a size, and kept only as the server
        // re-encoded it: nothing the upload carried besides its pixels stays.
        com.codenameone.examples.bazaar.web.security.Uploads.Image clean =
                com.codenameone.examples.bazaar.web.security.Uploads.image(bytes, type,
                        MAX_PHOTO_CHARS / 4 * 3);
        Photo row = new Photo();
        row.setId(Ids.next());
        row.setOwnerId(storeId);
        row.setContentType(clean.contentType);
        row.setData(clean.bytes);
        row.setCreatedAt(Instant.now());
        photos.insert(row);
        return row.getId();
    }

    @Transactional(readOnly = true)
    public List<CollectionDto> merchantCollections(String username, String storeId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        Snapshot snapshot = new Snapshot(storeId, null, false);
        List<CollectionDto> out = new ArrayList<CollectionDto>();
        for (int iter = 0; iter < snapshot.allCollections.size(); iter++) {
            Collection collection = snapshot.allCollections.get(iter);
            out.add(toDto(collection, visibleIds(snapshot, collection)));
        }
        return out;
    }

    @Transactional(rollbackFor = IOException.class)
    public CollectionDto createCollection(String username, String storeId, CollectionDto wanted)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        Collection collection = new Collection();
        collection.setId(Ids.next());
        collection.setStoreId(storeId);
        collection.setPosition(collections.findByStoreIdOrderByPositionAsc(storeId).size());
        fill(collection, wanted);
        collection.setHandle(uniqueCollectionHandle(storeId, wanted.handle(),
                collection.getTitle(), null));
        collections.insert(collection);
        placeAll(collection, wanted.productIds());
        return collection(storeId, collection.getId());
    }

    @Transactional(rollbackFor = IOException.class)
    public CollectionDto updateCollection(String username, String storeId, String collectionId,
            CollectionDto wanted) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Collection collection = ownCollection(storeId, collectionId);
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        fill(collection, wanted);
        if (wanted.handle() != null && wanted.handle().trim().length() > 0
                && !wanted.handle().trim().equals(collection.getHandle())) {
            collection.setHandle(uniqueCollectionHandle(storeId, wanted.handle(),
                    collection.getTitle(), collection.getId()));
        }
        if (wanted.position() >= 0) {
            collection.setPosition(wanted.position());
        }
        collections.save(collection);
        if (wanted.productIds() != null || collection.getRuleText().length() > 0) {
            List<CollectionProduct> old =
                    members.findByCollectionIdOrderByPositionAsc(collectionId);
            for (int iter = 0; iter < old.size(); iter++) {
                members.delete(old.get(iter));
            }
            placeAll(collection, wanted.productIds());
        }
        return collection(storeId, collectionId);
    }

    @Transactional(rollbackFor = IOException.class)
    public CollectionDto deleteCollection(String username, String storeId, String collectionId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Collection collection = ownCollection(storeId, collectionId);
        CollectionDto answer = toDto(collection, new ArrayList<String>());
        List<CollectionProduct> old = members.findByCollectionIdOrderByPositionAsc(collectionId);
        for (int iter = 0; iter < old.size(); iter++) {
            members.delete(old.get(iter));
        }
        collections.delete(collection);
        translations.forget(storeId, Translations.COLLECTION, collectionId);
        return answer;
    }

    // --------------------------------------------------------------- helpers

    private CollectionDto collection(String storeId, String collectionId) throws IOException {
        Snapshot snapshot = new Snapshot(storeId, null, false);
        return toDto(collections.findById(collectionId),
                visibleIds(snapshot, collections.findById(collectionId)));
    }

    private void placeAll(Collection collection, List<String> productIds) {
        if (collection.getRuleText().length() > 0 || productIds == null) {
            return;
        }
        for (int iter = 0; iter < productIds.size(); iter++) {
            product(collection.getStoreId(), productIds.get(iter));
            place(collection.getId(), productIds.get(iter));
        }
    }

    private void place(String collectionId, String productId) {
        List<CollectionProduct> placed = members.findByCollectionIdOrderByPositionAsc(collectionId);
        for (int iter = 0; iter < placed.size(); iter++) {
            if (placed.get(iter).getProductId().equals(productId)) {
                return;
            }
        }
        CollectionProduct row = new CollectionProduct();
        try {
            row.setId(Ids.next());
        } catch (IOException failed) {
            throw new ResponseStatusException(500, "No random bytes");
        }
        row.setCollectionId(collectionId);
        row.setProductId(productId);
        row.setPosition(placed.size());
        members.insert(row);
    }

    private Collection ownCollection(String storeId, String collectionId) {
        Collection collection = collectionId == null ? null : collections.findById(collectionId);
        StoreAccess.requireIn(collection == null ? null : collection.getStoreId(), storeId,
                "collection");
        return collection;
    }

    private String collectionId(Snapshot snapshot, String handle, boolean publishedOnly) {
        String wanted = Search.text(handle);
        if (wanted.length() == 0) {
            return "";
        }
        for (int iter = 0; iter < snapshot.allCollections.size(); iter++) {
            Collection c = snapshot.allCollections.get(iter);
            if ((c.getHandle().equals(wanted) || c.getId().equals(wanted))
                    && (!publishedOnly || c.isPublished())) {
                return c.getId();
            }
        }
        throw new ResponseStatusException(404, "No such collection");
    }

    private ProductPageDto page(Snapshot snapshot, Search search) {
        List<Product> hits = new ArrayList<Product>();
        for (int iter = 0; iter < snapshot.list.size(); iter++) {
            if (snapshot.matches(search, snapshot.list.get(iter), null)) {
                hits.add(snapshot.list.get(iter));
            }
        }
        sort(hits, snapshot, search);
        List<ProductCardDto> items = new ArrayList<ProductCardDto>();
        int from = search.page * search.size;
        for (int iter = from; iter < hits.size() && iter < from + search.size; iter++) {
            items.add(snapshot.card(hits.get(iter)));
        }
        return new ProductPageDto(items, hits.size(), search.page, search.size);
    }

    /// Sorts in place; a stable insertion sort over a list that is already
    /// by title, so ties stay alphabetical.
    private static void sort(List<Product> hits, Snapshot snapshot, Search search) {
        String sort = search.sort;
        if ("title".equals(sort) || "relevance".equals(sort) && search.words.length == 0) {
            return;
        }
        long[] keys = new long[hits.size()];
        for (int iter = 0; iter < hits.size(); iter++) {
            Product p = hits.get(iter);
            long key;
            if ("newest".equals(sort)) {
                key = -p.getCreatedAt().toEpochMilli();
            } else if ("price".equals(sort)) {
                key = snapshot.range(p)[0];
            } else if ("-price".equals(sort)) {
                key = -snapshot.range(p)[0];
            } else if ("rating".equals(sort)) {
                key = -rating(p);
            } else {
                key = -search.score(p);
            }
            keys[iter] = key;
        }
        for (int i = 1; i < hits.size(); i++) {
            Product item = hits.get(i);
            long key = keys[i];
            int j = i - 1;
            while (j >= 0 && keys[j] > key) {
                hits.set(j + 1, hits.get(j));
                keys[j + 1] = keys[j];
                j--;
            }
            hits.set(j + 1, item);
            keys[j + 1] = key;
        }
    }

    private Product product(String storeId, String productId) {
        Product product = productId == null ? null : products.findById(productId);
        StoreAccess.requireIn(product == null ? null : product.getStoreId(), storeId, "product");
        return product;
    }

    /// A product in full; costs only for staff, and stock of inactive
    /// locations only for staff.
    private ProductDto detail(Product product, boolean staff) throws IOException {
        List<Location> places = locations.findByStoreIdOrderByPositionAsc(product.getStoreId());
        Map<String, String> names = new HashMap<String, String>();
        for (int iter = 0; iter < places.size(); iter++) {
            if (staff || places.get(iter).isActive()) {
                names.put(places.get(iter).getId(), places.get(iter).getName());
            }
        }
        List<Variant> mine = variants.findByProductIdOrderByPositionAsc(product.getId());
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < mine.size(); iter++) {
            ids.add(mine.get(iter).getId());
        }
        List<InventoryLevel> stock = ids.isEmpty() ? new ArrayList<InventoryLevel>()
                : levels.findByVariantIdIn(ids);
        List<VariantDto> variantDtos = new ArrayList<VariantDto>();
        List<Bundles.Part> parts = bundles.parts(product.getId());
        for (int iter = 0; iter < mine.size(); iter++) {
            VariantDto d = toDto(mine.get(iter), stock, places, names, staff);
            if (!parts.isEmpty()) {
                d = bundleStock(d, parts, places, names);
            }
            variantDtos.add(d);
        }
        List<OptionDto> optionDtos = new ArrayList<OptionDto>();
        List<ProductOption> rows = options.findByProductIdOrderByPositionAsc(product.getId());
        for (int iter = 0; iter < rows.size(); iter++) {
            ProductOption o = rows.get(iter);
            optionDtos.add(new OptionDto(o.getId(), o.getName(), tags(o.getOptionValues()),
                    o.getPosition()));
        }
        List<ProductPhotoDto> photoDtos = new ArrayList<ProductPhotoDto>();
        List<ProductPhoto> pictures = gallery.findByProductIdOrderByPositionAsc(product.getId());
        for (int iter = 0; iter < pictures.size(); iter++) {
            ProductPhoto p = pictures.get(iter);
            photoDtos.add(new ProductPhotoDto(p.getId(), p.getPhotoId(), p.getAlt(),
                    p.getPosition(), p.getKind(), Media.url(p)));
        }
        List<String> collectionIds = new ArrayList<String>();
        List<Collection> all = collections.findByStoreIdOrderByPositionAsc(product.getStoreId());
        List<Product> one = new ArrayList<Product>();
        one.add(product);
        Map<String, List<Variant>> byProduct = new HashMap<String, List<Variant>>();
        byProduct.put(product.getId(), mine);
        for (int iter = 0; iter < all.size(); iter++) {
            Collection c = all.get(iter);
            if ((staff || c.isPublished()) && productIds(c, one, byProduct)
                    .contains(product.getId())) {
                collectionIds.add(c.getId());
            }
        }
        return new ProductDto(product.getId(), product.getStoreId(), product.getHandle(),
                product.getTitle(), product.getDescription(), product.getVendor(),
                product.getProductType(), tags(product.getTags()), product.getStatus(),
                product.getSeoTitle(), product.getSeoDescription(), product.getPhotoId(),
                rating(product), (int) product.getRatingCount(), optionDtos, variantDtos,
                photoDtos, collectionIds, product.getCategoryId());
    }

    /// A bundle variant as its components make it: tracked, with the
    /// bundles each listed location can make.
    private VariantDto bundleStock(VariantDto d, List<Bundles.Part> parts, List<Location> places,
            Map<String, String> names) throws IOException {
        List<StockDto> per = new ArrayList<StockDto>();
        int total = 0;
        for (int p = 0; p < places.size(); p++) {
            String locationId = places.get(p).getId();
            if (!names.containsKey(locationId)) {
                continue;
            }
            Set<String> one = new HashSet<String>();
            one.add(locationId);
            int n = bundles.available(parts, one);
            total += n;
            per.add(new StockDto(locationId, names.get(locationId), n));
        }
        return new VariantDto(d.id(), d.productId(), d.title(), d.option1(), d.option2(),
                d.option3(), d.sku(), d.barcode(), d.priceCents(), d.compareAtCents(),
                d.costCents(), d.weightGrams(), d.taxable(), true, false, d.position(),
                Math.min(total, Bundles.UNLIMITED), per, d.digital(), d.hsCode(),
                d.originCountry(), d.unitQuantity(), d.unitName(), d.unitReference(),
                d.unitReferenceName());
    }

    private static VariantDto toDto(Variant v, List<InventoryLevel> stock, List<Location> places,
            Map<String, String> names, boolean staff) {
        List<StockDto> per = new ArrayList<StockDto>();
        int total = 0;
        for (int p = 0; p < places.size(); p++) {
            String locationId = places.get(p).getId();
            if (!names.containsKey(locationId)) {
                continue;
            }
            int available = 0;
            for (int iter = 0; iter < stock.size(); iter++) {
                InventoryLevel level = stock.get(iter);
                if (level.getVariantId().equals(v.getId())
                        && level.getLocationId().equals(locationId)) {
                    available = level.getOnHand() - level.getCommitted();
                }
            }
            total += Math.max(0, available);
            per.add(new StockDto(locationId, names.get(locationId),
                    staff ? available : Math.max(0, available)));
        }
        return new VariantDto(v.getId(), v.getProductId(), v.getTitle(), v.getOption1(),
                v.getOption2(), v.getOption3(), v.getSku(), v.getBarcode(), v.getPriceCents(),
                v.getCompareAtCents(), staff ? v.getCostCents() : 0, v.getWeightGrams(),
                v.isTaxable(), v.isTrackInventory(), v.isAllowBackorder(), v.getPosition(),
                v.isTrackInventory() ? total : -1, per, v.isDigital(), v.getHsCode(),
                v.getOriginCountry(), v.getUnitQuantity(), v.getUnitName(),
                v.getUnitReference(), v.getUnitReferenceName());
    }

    public static CollectionDto toDto(Collection c, List<String> productIds) {
        return new CollectionDto(c.getId(), c.getHandle(), c.getTitle(), c.getDescription(),
                c.getPhotoId(), c.getRuleText(), c.getPosition(), c.isPublished(), productIds,
                productIds.size());
    }

    private static void fill(Product product, ProductDto wanted) {
        product.setTitle(Text.required(wanted.title(), 200, "the title"));
        product.setDescription(Text.optional(wanted.description(), 20000, "the description"));
        product.setVendor(Text.optional(wanted.vendor(), 120, "the vendor"));
        product.setProductType(Text.optional(wanted.productType(), 80, "the type"));
        product.setTags(join(wanted.tags(), 500, "the tags"));
        if (wanted.status() != null && wanted.status().length() > 0) {
            product.setStatus(Text.oneOf(wanted.status(), STATUSES, DRAFT, "the status"));
        }
        product.setSeoTitle(Text.optional(wanted.seoTitle(), 200, "the search title"));
        product.setSeoDescription(Text.optional(wanted.seoDescription(), 320,
                "the search description"));
        // Negative leaves the category as it is (an import that does not
        // know it); 0 clears it.
        if (wanted.categoryId() > 0 && !Taxonomy.known(wanted.categoryId())) {
            throw new ResponseStatusException(400, "Choose a category from the list");
        }
        if (wanted.categoryId() >= 0) {
            product.setCategoryId(wanted.categoryId());
        }
        product.setUpdatedAt(Instant.now());
    }

    private static void fill(Collection collection, CollectionDto wanted) {
        collection.setTitle(Text.required(wanted.title(), 200, "the title"));
        collection.setDescription(Text.optional(wanted.description(), 2000, "the description"));
        collection.setPhotoId(Text.optional(wanted.photoId(), 32, "the photo"));
        String rules = Text.optional(wanted.ruleText(), 500, "the rules");
        Rules.parse(rules);
        collection.setRuleText(rules);
        collection.setPublished(wanted.published());
    }

    /// Options, variants, gallery order and manual collections, as `wanted`
    /// gives them. A list left null is left as it is.
    private void saveChildren(Product product, ProductDto wanted) throws IOException {
        String storeId = product.getStoreId();
        if (wanted.options() != null) {
            List<ProductOption> old = options.findByProductIdOrderByPositionAsc(product.getId());
            for (int iter = 0; iter < old.size(); iter++) {
                options.delete(old.get(iter));
            }
            if (wanted.options().size() > 3) {
                throw new ResponseStatusException(400, "A product has at most three options");
            }
            for (int iter = 0; iter < wanted.options().size(); iter++) {
                OptionDto o = wanted.options().get(iter);
                ProductOption row = new ProductOption();
                row.setId(Ids.next());
                row.setProductId(product.getId());
                row.setName(Text.required(o.name(), 60, "the option's name"));
                row.setOptionValues(join(o.values(), 500, "the option's values"));
                row.setPosition(iter);
                options.insert(row);
            }
        }
        if (wanted.variants() != null) {
            saveVariants(product, wanted.variants());
        } else if (variants.findByProductIdOrderByPositionAsc(product.getId()).isEmpty()) {
            List<VariantDto> one = new ArrayList<VariantDto>();
            one.add(new VariantDto(null, null, "", "", "", "", "", "", 0, 0, 0, 0, true, true,
                    false, 0, 0, null, false, null, null, 0, null, 0, null));
            saveVariants(product, one);
        }
        if (wanted.photos() != null) {
            List<ProductPhoto> pictures = gallery.findByProductIdOrderByPositionAsc(product.getId());
            for (int iter = 0; iter < pictures.size(); iter++) {
                ProductPhoto p = pictures.get(iter);
                int at = -1;
                for (int w = 0; w < wanted.photos().size(); w++) {
                    if (p.getId().equals(wanted.photos().get(w).id())) {
                        at = w;
                    }
                }
                if (at < 0) {
                    gallery.delete(p);
                    translations.forget(product.getStoreId(), Translations.IMAGE, p.getId());
                } else {
                    p.setPosition(at);
                    p.setAlt(Text.optional(wanted.photos().get(at).alt(), 200, "the caption"));
                    gallery.save(p);
                }
            }
            List<ProductPhoto> left = gallery.findByProductIdOrderByPositionAsc(product.getId());
            Product fresh = products.findById(product.getId());
            fresh.setPhotoId(Media.firstImage(left));
            products.save(fresh);
        }
        if (wanted.collectionIds() != null) {
            List<Collection> all = collections.findByStoreIdOrderByPositionAsc(storeId);
            for (int iter = 0; iter < all.size(); iter++) {
                Collection c = all.get(iter);
                if (c.getRuleText().length() > 0) {
                    continue;
                }
                boolean want = wanted.collectionIds().contains(c.getId());
                List<CollectionProduct> placed =
                        members.findByCollectionIdOrderByPositionAsc(c.getId());
                CollectionProduct mine = null;
                for (int p = 0; p < placed.size(); p++) {
                    if (placed.get(p).getProductId().equals(product.getId())) {
                        mine = placed.get(p);
                    }
                }
                if (want && mine == null) {
                    place(c.getId(), product.getId());
                } else if (!want && mine != null) {
                    members.delete(mine);
                }
            }
        }
    }

    private void saveVariants(Product product, List<VariantDto> wanted) throws IOException {
        String storeId = product.getStoreId();
        if (wanted.isEmpty()) {
            throw new ResponseStatusException(400, "A product has at least one variant");
        }
        if (wanted.size() > 100) {
            throw new ResponseStatusException(400, "A product has at most 100 variants");
        }
        List<Variant> old = variants.findByProductIdOrderByPositionAsc(product.getId());
        List<String> kept = new ArrayList<String>();
        for (int iter = 0; iter < wanted.size(); iter++) {
            if (wanted.get(iter).id() != null) {
                kept.add(wanted.get(iter).id());
            }
        }
        for (int iter = 0; iter < old.size(); iter++) {
            Variant v = old.get(iter);
            if (!kept.contains(v.getId())) {
                if (bundles.isVariantComponent(v.getId())) {
                    throw new ResponseStatusException(409, "A variant that is part of a bundle "
                            + "cannot be deleted; take it out of the bundle first");
                }
                List<InventoryLevel> held = levels.findByVariantId(v.getId());
                for (int l = 0; l < held.size(); l++) {
                    levels.delete(held.get(l));
                }
                variants.delete(v);
            }
        }
        List<Location> places = locations.findByStoreIdOrderByPositionAsc(storeId);
        for (int iter = 0; iter < wanted.size(); iter++) {
            VariantDto w = wanted.get(iter);
            Variant v;
            boolean fresh = w.id() == null || w.id().length() == 0;
            if (fresh) {
                v = new Variant();
                v.setId(Ids.next());
                v.setProductId(product.getId());
                v.setStoreId(storeId);
            } else {
                v = variants.findById(w.id());
                if (v == null || !v.getProductId().equals(product.getId())) {
                    throw new ResponseStatusException(404, "No such variant");
                }
            }
            v.setOption1(Text.optional(w.option1(), 80, "the option"));
            v.setOption2(Text.optional(w.option2(), 80, "the option"));
            v.setOption3(Text.optional(w.option3(), 80, "the option"));
            String title = Text.optional(w.title(), 200, "the variant's title");
            if (title.length() == 0) {
                title = variantTitle(v);
            }
            v.setTitle(title);
            String sku = Text.optional(w.sku(), 64, "the SKU");
            if (sku.length() > 0 && skuTaken(storeId, sku, v.getId())) {
                throw new ResponseStatusException(409, "Another variant has the SKU " + sku);
            }
            v.setSku(sku);
            String barcode = Text.optional(w.barcode(), 64, "the barcode");
            if (barcode.length() > 0 && barcodeTaken(storeId, barcode, v.getId())) {
                throw new ResponseStatusException(409, "Another variant has the barcode "
                        + barcode);
            }
            v.setBarcode(barcode);
            if (w.priceCents() < 0 || w.compareAtCents() < 0 || w.costCents() < 0
                    || w.weightGrams() < 0) {
                throw new ResponseStatusException(400, "Prices and weights are not negative");
            }
            v.setPriceCents(w.priceCents());
            v.setCompareAtCents(w.compareAtCents());
            v.setCostCents(w.costCents());
            v.setWeightGrams(w.weightGrams());
            v.setTaxable(w.taxable());
            v.setTrackInventory(w.trackInventory());
            v.setAllowBackorder(w.allowBackorder());
            v.setDigital(w.digital());
            customsAndUnits(v, w);
            v.setPosition(iter);
            if (fresh) {
                variants.insert(v);
                List<InventoryLevel> made = new ArrayList<InventoryLevel>();
                for (int p = 0; p < places.size(); p++) {
                    made.add(Inventory.newLevel(storeId, v.getId(), places.get(p).getId(), 0));
                }
                levels.saveAll(made);
            } else {
                variants.save(v);
            }
        }
    }

    /// The customs and unit pricing fields of `w` into `v`, checked. A field
    /// sent as null is left as it is, so a client that does not know them
    /// (an import, an older app) never clears them.
    static void customsAndUnits(Variant v, VariantDto w) {
        if (w.hsCode() != null) {
            String hs = Customs.hsCode(w.hsCode());
            if (hs == null) {
                throw new ResponseStatusException(400, "An HS code is 6 to 10 digits");
            }
            v.setHsCode(hs);
        }
        if (w.originCountry() != null) {
            String c = Customs.country(w.originCountry());
            if (c == null) {
                throw new ResponseStatusException(400,
                        "The country of origin is a two-letter ISO code");
            }
            v.setOriginCountry(c);
        }
        if (w.unitName() == null) {
            return;
        }
        if (w.unitName().length() == 0 || w.unitQuantity() == 0) {
            v.setUnitQuantity(0);
            v.setUnitName("");
            v.setUnitReference(0);
            v.setUnitReferenceName("");
            return;
        }
        // No reference unit: the price is shown per the measure's own unit.
        String ref = w.unitReferenceName() == null || w.unitReferenceName().length() == 0
                ? w.unitName() : w.unitReferenceName();
        if (!UnitPricing.compatible(w.unitName(), ref)) {
            throw new ResponseStatusException(400,
                    "The measure and the unit price must use units of the same kind");
        }
        if (w.unitQuantity() < 0 || w.unitQuantity() > UnitPricing.MAX_QUANTITY
                || w.unitReference() <= 0 || w.unitReference() > UnitPricing.MAX_REFERENCE) {
            throw new ResponseStatusException(400, "The measure is out of range");
        }
        v.setUnitQuantity(w.unitQuantity());
        v.setUnitName(w.unitName());
        v.setUnitReference(w.unitReference());
        v.setUnitReferenceName(ref);
    }

    private static String variantTitle(Variant v) {
        StringBuilder out = new StringBuilder();
        String[] parts = {v.getOption1(), v.getOption2(), v.getOption3()};
        for (int iter = 0; iter < parts.length; iter++) {
            if (parts[iter].length() > 0) {
                if (out.length() > 0) {
                    out.append(" / ");
                }
                out.append(parts[iter]);
            }
        }
        return out.length() == 0 ? "Default" : out.toString();
    }

    private boolean skuTaken(String storeId, String sku, String variantId) {
        List<Variant> all = variants.findByStoreId(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            Variant v = all.get(iter);
            if (v.getSku().equalsIgnoreCase(sku) && !v.getId().equals(variantId)) {
                return true;
            }
        }
        return false;
    }

    private boolean barcodeTaken(String storeId, String barcode, String variantId) {
        List<Variant> all = variants.findByStoreIdAndBarcode(storeId, barcode);
        for (int iter = 0; iter < all.size(); iter++) {
            if (!all.get(iter).getId().equals(variantId)) {
                return true;
            }
        }
        return false;
    }

    private String uniqueSku(String storeId, String sku) {
        if (sku.length() == 0) {
            return "";
        }
        String candidate = clip(sku, 60);
        for (int n = 2; skuTaken(storeId, candidate, null); n++) {
            candidate = clip(sku, 56) + "-" + n;
        }
        return candidate;
    }

    private String uniqueHandle(String storeId, String wanted, String title, String productId) {
        String base = slug(wanted != null && wanted.trim().length() > 0 ? wanted : title);
        String candidate = base;
        for (int n = 2; ; n++) {
            Product other = products.findByStoreIdAndHandle(storeId, candidate);
            if (other == null || other.getId().equals(productId)) {
                return candidate;
            }
            candidate = clip(base, 110) + "-" + n;
        }
    }

    private String uniqueCollectionHandle(String storeId, String wanted, String title,
            String collectionId) {
        String base = slug(wanted != null && wanted.trim().length() > 0 ? wanted : title);
        String candidate = base;
        for (int n = 2; ; n++) {
            Collection other = collections.findByStoreIdAndHandle(storeId, candidate);
            if (other == null || other.getId().equals(collectionId)) {
                return candidate;
            }
            candidate = clip(base, 110) + "-" + n;
        }
    }

    /// Lower-case ASCII letters and digits, with single dashes between words.
    public static String slug(String text) {
        StringBuilder out = new StringBuilder();
        String source = text == null ? "" : text;
        for (int iter = 0; iter < source.length() && out.length() < 110; iter++) {
            char c = source.charAt(iter);
            if (c >= 'A' && c <= 'Z') {
                c = (char) (c + ('a' - 'A'));
            }
            if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                out.append(c);
            } else if (out.length() > 0 && out.charAt(out.length() - 1) != '-') {
                out.append('-');
            }
        }
        while (out.length() > 0 && out.charAt(out.length() - 1) == '-') {
            out.setLength(out.length() - 1);
        }
        return out.length() == 0 ? "item" : out.toString();
    }

    /// Stars times ten, rounded to the nearest tenth as the reviews' own
    /// average is (`ReviewEndpoint`), 0 without reviews.
    static int rating(Product product) {
        long count = product.getRatingCount();
        return count <= 0 ? 0 : (int) ((product.getRatingTotal() * 10 + count / 2) / count);
    }

    /// A comma-separated list, trimmed, without empty entries.
    public static List<String> tags(String text) {
        List<String> out = new ArrayList<String>();
        if (text == null) {
            return out;
        }
        int start = 0;
        for (int iter = 0; iter <= text.length(); iter++) {
            if (iter == text.length() || text.charAt(iter) == ',') {
                String part = text.substring(start, iter).trim();
                if (part.length() > 0) {
                    out.add(part);
                }
                start = iter + 1;
            }
        }
        return out;
    }

    static String join(List<String> parts, int max, String what) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; parts != null && iter < parts.size(); iter++) {
            String part = parts.get(iter) == null ? "" : parts.get(iter).trim();
            if (part.length() == 0) {
                continue;
            }
            if (part.indexOf(',') >= 0) {
                throw new ResponseStatusException(400, "No commas in " + what);
            }
            if (out.length() > 0) {
                out.append(", ");
            }
            out.append(part);
        }
        if (out.length() > max) {
            throw new ResponseStatusException(400, "Too long: " + what);
        }
        return out.toString();
    }

    private static String clip(String text, int max) {
        return text.length() > max ? text.substring(0, max) : text;
    }
}
