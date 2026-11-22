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
package com.codenameone.examples.bazaar.shop;

import com.codename1.appkit.Net;
import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codenameone.examples.bazaar.api.CollectionDto;
import com.codenameone.examples.bazaar.api.ProductCardDto;
import com.codenameone.examples.bazaar.api.ProductPageDto;
import com.codenameone.examples.bazaar.api.StoreDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.ArtPalette;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Look;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.Nav;
import com.codenameone.examples.bazaar.ui.ProductArt;
import com.codenameone.examples.bazaar.ui.SignInGate;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// The home tab, in one of two places.
///
/// - Discover, where the app opens: every store on the platform in a row
///   that scrolls sideways, new products from the stores the shopper follows,
///   what is popular, and the newest products across every store. Nobody has
///   to sign in to see any of it.
/// - A store's own page, once one is opened: its hero in its colour, its
///   collections, and its newest products in a grid that keeps loading. The
///   arrow on the hero goes back to Discover.
final class HomePane extends ShopPane {
    private static final int FEED = 24;
    private static final int ROW = 10;
    private static final float ROW_CARD_MM = 38f;
    /// A store card's width: narrow enough that a phone shows one and most
    /// of the next, so the row reads as something to swipe.
    private static final float STORE_CARD_W_MM = 40f;
    private static final float STORE_CARD_H_MM = 20f;

    private ProductGrid grid;
    private final SearchQuery featured = new SearchQuery();
    /// Numbers each build, so answers that arrive after the shopper moved on
    /// (into a store and back) fill nothing.
    private int built;

    HomePane() {
        super(new BorderLayout());
        setName("shop-home");
        featured.sort = "newest";
        if (ShopState.inStore()) {
            build();
        } else {
            discover();
        }
    }

    @Override
    void storeChanged() {
        // A product of another store was opened from the feed: Discover stays.
    }

    @Override
    void reload() {
        if (ShopState.inStore()) {
            if (grid != null) {
                grid.reload();
            }
        } else {
            discover();
        }
    }

    /// Opens the page of `store`.
    void open(StoreDto store) {
        ShopState.useStore(store);
        ShopState.setInStore(true);
        build();
    }

    private void leaveStore() {
        ShopState.setInStore(false);
        discover();
    }

    // -------------------------------------------------------------- discover

    private void discover() {
        removeAll();
        grid = null;
        final int ticket = ++built;
        Container page = new Container(BoxLayout.y());
        page.setUIID("ShopScroller");
        page.setName("discover");
        page.setScrollableY(true);
        page.setScrollVisible(false);
        Ui.pull(page, this::discover);

        Container head = new Container(BoxLayout.y());
        // Wide, it is a panel of the capped column rather than a band.
        head.setUIID(Layouts.wide() ? "ShopDiscoverHeadWide" : "ShopDiscoverHead");
        head.add(Ui.label("Discover", "ShopDiscoverTitle"));
        head.add(Ui.text("Independent stores, one cart each. Browse freely; sign in only "
                + "when you check out.", "ShopDiscoverText"));
        Button search = new Button(Lang.tr("Search every store"), "ShopSearchPill");
        search.setName("discover-search");
        Ui.icon(search, FontImage.MATERIAL_SEARCH, 3.4f);
        search.addActionListener(e -> ShopSections.show(com.codenameone.examples.bazaar.ui.Sections.SEARCH));
        head.add(search);
        page.add(head);

        page.add(title("Stores", null));
        final Container stores = row("discover-stores");
        stores.add(storePlaceholder());
        stores.add(storePlaceholder());
        page.add(stores);

        final Container followingTitle = title("From stores you follow", null);
        final Container following = row("discover-following");
        followingTitle.setHidden(true);
        following.setHidden(true);
        page.add(followingTitle);
        page.add(following);

        page.add(title("Popular right now", null));
        final Container popular = row("discover-popular");
        placeholders(popular);
        page.add(popular);

        page.add(title("New across Bazaar", null));
        final int width = Math.max(1, ProductGrid.room());
        final int columns = ProductGrid.columns(width);
        final int cardWidth = width / columns - CN.convertToPixels(3);
        final Container fresh = new Container(new com.codename1.ui.layouts.GridLayout(2, columns));
        fresh.setUIID("ShopGrid");
        fresh.setName("discover-new");
        for (int iter = 0; iter < columns * 2; iter++) {
            fresh.add(ProductCard.placeholder(cardWidth));
        }
        page.add(fresh);
        add(BorderLayout.CENTER, page);

        Api.stores().stores(Net.to(list -> {
            if (ticket != built) {
                return;
            }
            ShopState.know(list);
            ShopState.loadFollows(() -> {
                if (ticket == built) {
                    fillStores(stores, list);
                    loadFollowing(ticket, followingTitle, following);
                }
            });
            Api.discover().feed(null, "popular", String.valueOf(ROW), Net.to(answer -> {
                if (ticket == built) {
                    fillRow(popular, answer);
                }
            }, (status, message) -> popular.setHidden(true)));
            Api.discover().feed(null, "newest", String.valueOf(FEED), Net.to(answer -> {
                if (ticket == built) {
                    fillGrid(fresh, answer, columns, cardWidth);
                }
            }, (status, message) -> fresh.setHidden(true)));
        }, ShopUi.failed(this, this::discover)));
        Ui.relayout(this);
    }

    private static Container title(String text, Component action) {
        Container t = new Container(new BorderLayout());
        t.setUIID("ShopSectionHead");
        t.add(BorderLayout.CENTER, Ui.label(text, "ShopSectionTitle"));
        if (action != null) {
            t.add(BorderLayout.EAST, action);
        }
        return t;
    }

    private static Container row(String name) {
        Container row = new Container(BoxLayout.x());
        row.setUIID("ShopCarousel");
        row.setName(name);
        row.setScrollableX(true);
        row.setScrollVisible(false);
        return row;
    }

    private static int rowCard() {
        return CN.convertToPixels(ROW_CARD_MM);
    }

    private static void placeholders(Container row) {
        for (int iter = 0; iter < 3; iter++) {
            row.add(sized(ProductCard.placeholder(rowCard())));
        }
    }

    private static Component sized(Component card) {
        card.setPreferredW(rowCard());
        return card;
    }

    private static Component storePlaceholder() {
        Container card = new Container(BoxLayout.y());
        card.setUIID("ShopStoreCard");
        Label art = new Label("", "ShopPlaceholderArt");
        art.setPreferredH(CN.convertToPixels(STORE_CARD_H_MM));
        art.setPreferredW(CN.convertToPixels(STORE_CARD_W_MM));
        card.add(art).add(new Label(" ", "ShopPlaceholderLine"))
                .add(new Label(" ", "ShopPlaceholderLineShort"));
        return card;
    }

    private void fillStores(Container row, List<StoreDto> list) {
        row.removeAll();
        for (StoreDto s : list) {
            if ("ACTIVE".equals(s.status())) {
                row.add(storeCard(s));
            }
        }
        if (row.getComponentCount() == 0) {
            row.add(Ui.empty(FontImage.MATERIAL_STOREFRONT, "No stores yet",
                    "Stores appear here once they open."));
        }
        Ui.relayout(row);
    }

    private Component storeCard(final StoreDto s) {
        int w = CN.convertToPixels(STORE_CARD_W_MM);
        int h = CN.convertToPixels(STORE_CARD_H_MM);
        Container card = new Container(new BorderLayout());
        card.setUIID("ShopStoreCard");
        Container top = new Container(new LayeredLayout());
        int[] colors = ArtPalette.colors(s.id() + "-hero", Look.dark());
        top.getAllStyles().setBgColor(colors[1]);
        top.getAllStyles().setBgTransparency(255);
        top.add(ProductArt.label(s.id() + "-hero", s.name(), ProductArt.NO_GLYPH,
                s.heroPhotoId(), w, h, "ShopStoreCardArt"));
        int side = CN.convertToPixels(10);
        Label logo = ProductArt.label(s.id(), s.name(), "storefront", s.logoPhotoId(), side, side,
                "ShopStoreCardLogo");
        top.add(BorderLayout.south(FlowLayout.encloseIn(logo)));
        card.add(BorderLayout.NORTH, top);
        Container words = new Container(BoxLayout.y());
        words.setUIID("ShopStoreCardWords");
        Label name = Ui.plain(s.name(), "ShopStoreCardName");
        name.setShouldLocalize(false);
        words.add(name);
        if (s.tagline() != null) {
            Label tag = Ui.plain(s.tagline(), "ShopStoreCardTag");
            tag.setShouldLocalize(false);
            words.add(tag);
        }
        card.add(BorderLayout.CENTER, words);
        // The picture and the words open the store; Follow sits under them,
        // outside that tap -- a lead component takes every press in its
        // container, and puts its own button in the south of a border.
        Container tappable = Ui.tap(card, "store-" + s.handle(), e -> open(s));
        Container whole = new Container(new BorderLayout());
        whole.setUIID("ShopStoreCard");
        card.setUIID("Container");
        whole.add(BorderLayout.CENTER, tappable);
        whole.add(BorderLayout.SOUTH, FlowLayout.encloseIn(followButton(s, false)));
        whole.setPreferredW(w);
        return whole;
    }

    /// Follow / Following for `s`. A guest is asked to sign in first.
    /// In the hero (`hero`) it sits beside "Shop all" and is sized like it.
    private Button followButton(final StoreDto s, boolean hero) {
        final Button b = new Button("", "ShopFollow");
        if (hero) {
            b.putClientProperty("heroFollow", Boolean.TRUE);
        }
        b.setName("follow-" + s.handle());
        paintFollow(b, ShopState.follows(s.id()));
        b.addActionListener(e -> SignInGate.require(b.getComponentForm(), "Follow your favourites",
                "Sign in to follow stores and see what is new from them first.",
                () -> ShopState.loadFollows(() -> {
                    if (Nav.guest()) {
                        return;
                    }
                    if (!ShopState.follows(s.id()) || b.getComponentForm() != null) {
                        ShopState.toggleFollow(s, () -> paintFollow(b, ShopState.follows(s.id())));
                    }
                })));
        return b;
    }

    private static void paintFollow(Button b, boolean on) {
        b.setText(Lang.tr(on ? "Following" : "Follow"));
        String prefix = b.getClientProperty("heroFollow") != null ? "ShopHero" : "Shop";
        b.setUIID(prefix + (on ? "Following" : "Follow"));
        Ui.icon(b, on ? FontImage.MATERIAL_CHECK : FontImage.MATERIAL_ADD, 2.8f);
        if (b.getParent() != null) {
            Ui.relayout(b.getParent());
        }
    }

    private void loadFollowing(final int ticket, final Container title, final Container row) {
        if (Nav.guest() || ShopState.followed().isEmpty()) {
            return;
        }
        Api.discover().followingFeed(String.valueOf(ROW), Net.to(answer -> {
            if (ticket != built || answer.items() == null || answer.items().isEmpty()) {
                return;
            }
            title.setHidden(false);
            row.setHidden(false);
            fillRow(row, answer);
        }, (status, message) -> { }));
    }

    private static void fillRow(Container row, ProductPageDto answer) {
        row.removeAll();
        for (int iter = 0; answer.items() != null && iter < answer.items().size(); iter++) {
            row.add(sized(ProductCard.create(answer.items().get(iter), rowCard())));
        }
        row.setHidden(row.getComponentCount() == 0);
        Ui.relayout(row);
    }

    private static void fillGrid(Container grid, ProductPageDto answer, int columns, int cardWidth) {
        grid.removeAll();
        List<ProductCardDto> items = answer.items();
        int count = items == null ? 0 : items.size();
        for (int iter = 0; iter < count; iter++) {
            grid.add(ProductCard.create(items.get(iter), cardWidth));
        }
        if (count == 0) {
            grid.setLayout(new BorderLayout());
            grid.add(BorderLayout.CENTER, Ui.empty(FontImage.MATERIAL_INVENTORY_2,
                    "Nothing here yet", "New products show up here as stores add them."));
        } else {
            grid.setLayout(new com.codename1.ui.layouts.GridLayout(
                    Math.max(1, (count + columns - 1) / columns), columns));
        }
        Ui.relayout(grid);
    }

    // ----------------------------------------------------------------- store

    private void build() {
        removeAll();
        ++built;
        StoreDto store = ShopState.store();
        if (store == null) {
            leaveStore();
            return;
        }
        Button back = new Button("", "ShopHeaderButton");
        back.setName("discover-back");
        FontImage.setMaterialIcon(back, FontImage.MATERIAL_ARROW_BACK, 3.6f);
        back.addActionListener(e -> leaveStore());

        Container top = new Container(BoxLayout.y());
        top.add(hero(store, back));
        final Container collections = row("collections");
        top.add(collections);
        top.add(Ui.label("New arrivals", "ShopSectionTitle"));
        final Container categories = new Container(new com.codename1.ui.layouts.FlowLayout());
        categories.setUIID("ShopChips");
        categories.setName("categories");
        categories.setHidden(true);
        top.add(categories);
        featured.type = null;
        grid = new ProductGrid(top, featured, 0);
        add(BorderLayout.CENTER, grid.component());
        grid.reload();
        Api.catalog().collections(store.handle(), Net.to(list -> fill(collections, list),
                (status, message) -> collections.setHidden(true)));
        final int ticket = built;
        Api.catalog().facets(store.handle(), null, null, null, null, null, null, null,
                Net.to(facets -> {
                    if (ticket == built) {
                        categories(categories, Categories.choices(facets.types(),
                                Categories.MAX));
                    }
                }, (status, message) -> { }));
        Ui.relayout(this);
    }

    /// The category row: "All" and each category; a tap narrows the grid
    /// to it ([Categories] decides what a tap means).
    private void categories(Container row, List<String> names) {
        row.removeAll();
        row.setHidden(names.isEmpty());
        for (int iter = -1; iter < names.size(); iter++) {
            final String name = iter < 0 ? null : names.get(iter);
            Button chip = new Button(name == null ? Lang.tr("All") : name,
                    Categories.chosen(featured.type, name) ? "ShopChipOn" : "ShopChip");
            chip.setName("category-" + (name == null ? "all" : name));
            chip.addActionListener(e -> {
                ShopUi.tap();
                featured.type = Categories.tap(featured.type, name);
                categories(row, names);
                grid.reload();
            });
            row.add(chip);
        }
        Ui.relayout(row);
    }

    private Container hero(final StoreDto store, Button back) {
        int width = Math.min(CN.getDisplayWidth(), Layouts.column() * 2);
        int height = CN.convertToPixels(Layouts.wide() ? 46 : 52);
        Container hero = new Container(new LayeredLayout());
        hero.setUIID("ShopHero");
        // A long name or description makes the hero taller than its art; the
        // art's own deeper colour fills what it does not cover, not a grey band.
        int[] colors = ArtPalette.colors(store.id() + "-hero", Look.dark());
        hero.getAllStyles().setBgColor(colors[1]);
        hero.getAllStyles().setBgTransparency(255);
        // No glyph on the hero's drawn art: the store's name is laid over it.
        Label picture = ProductArt.label(store.id() + "-hero", store.name(),
                ProductArt.NO_GLYPH, store.heroPhotoId(), width, height, "ShopHeroArt");
        hero.add(picture);
        // One scrim over the whole hero, so its top is not a lighter band
        // above the words.
        Container shade = new Container();
        shade.setUIID("ShopHeroShade");
        hero.add(shade);
        Container words = new Container(BoxLayout.y());
        words.setUIID("ShopHeroWords");
        SpanLabel name = Ui.text(store.name(), "ShopHeroTitle");
        name.setShouldLocalize(false);
        words.add(name);
        String about = store.description() != null && store.description().length() > 0
                ? store.description() : store.tagline();
        if (about != null) {
            words.add(Ui.text(about, "ShopHeroText"));
        }
        Container actions = new Container(BoxLayout.x());
        actions.add(Ui.primary("Shop all", "shop-all", e -> ShopSections.search(null)));
        actions.add(followButton(store, true));
        words.add(actions);
        hero.add(BorderLayout.south(words));
        hero.add(FlowLayout.encloseIn(back));
        return hero;
    }

    private void fill(Container row, List<CollectionDto> collections) {
        row.removeAll();
        int w = CN.convertToPixels(34);
        int h = CN.convertToPixels(22);
        for (final CollectionDto c : collections) {
            if (!c.published()) {
                continue;
            }
            Container tile = new Container(new LayeredLayout());
            tile.setUIID("ShopCollection");
            tile.add(ProductArt.label(c.id(), c.title(), c.title(), c.photoId(), w, h,
                    "ShopCollectionArt"));
            Container words = new Container(BoxLayout.y());
            words.setUIID("ShopCollectionWords");
            words.add(Ui.plain(c.title(), "ShopCollectionTitle"));
            words.add(Ui.plain(Lang.tr("{0} products", String.valueOf(c.productCount())), "ShopCollectionCount"));
            tile.add(BorderLayout.south(words));
            row.add(Ui.tap(BorderLayout.center(tile), "collection-" + c.handle(),
                    e -> ShopSections.collection(c.handle(), c.title())));
        }
        row.setHidden(row.getComponentCount() == 0);
        Ui.relayout(row);
    }
}
