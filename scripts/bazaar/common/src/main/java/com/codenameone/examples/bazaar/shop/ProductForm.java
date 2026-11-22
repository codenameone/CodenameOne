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
import com.codename1.appkit.Skeleton;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.Tabs;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codenameone.examples.bazaar.api.OptionDto;
import com.codenameone.examples.bazaar.api.ProductDto;
import com.codenameone.examples.bazaar.api.ProductPhotoDto;
import com.codenameone.examples.bazaar.api.StockDto;
import com.codenameone.examples.bazaar.api.VariantDto;
import com.codenameone.examples.bazaar.live.LiveChannel;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.ProductArt;
import com.codenameone.examples.bazaar.ui.Ui;
import com.codenameone.examples.bazaar.ui.Windows;
import java.util.List;
import java.util.Map;

/// A product: a gallery that swipes, the options as chips that pick out a
/// variant, its price and how many are left where, a quantity, and Add to
/// cart -- which flies the picture into the cart. Wishlist and share sit
/// beside the title. Side by side on a wide screen, one column on a phone.
///
/// Opened as a screen of its own, or (from a card's right-click menu on a
/// desktop) in a window of its own.
final class ProductForm {
    private static final int ESC = 27;

    private final Container root = new Container(new BorderLayout());
    private final boolean wide;
    private final Form host;
    private ProductDto product;
    private String[] chosen = new String[3];
    private int quantity = 1;
    private final Container options = new Container(BoxLayout.y());
    private final Container priceRow = new Container(new FlowLayout());
    private final Label stock = new Label("", "ShopStock");
    private final Label count = new Label("1", "ShopStepValue");
    private Button add;
    private Component gallery;
    private Image picture;

    private ProductForm(Form host, boolean wide) {
        this.host = host;
        this.wide = wide;
        root.setName("product");
        root.add(BorderLayout.CENTER, new Skeleton(6, "SvSkeleton"));
    }

    /// Opens the product with `id` as a screen, with a way back.
    static void show(final String id, String title) {
        final Form previous = CN.getCurrentForm();
        final Form form = Ui.form(title == null ? "" : title, "Product");
        form.setLayout(new BorderLayout());
        form.setScrollable(false);
        Ui.back(form, previous);
        final ProductForm view = new ProductForm(form, Layouts.wide());
        form.add(BorderLayout.CENTER, view.root);
        final ActionListener<ActionEvent> esc = e -> {
            if (previous != null) {
                previous.showBack();
            }
        };
        form.addKeyListener(ESC, esc);
        form.addShowListener(e -> view.listen());
        form.show();
        view.load(id);
    }

    /// Opens it in a window of its own where there are windows; elsewhere
    /// as a screen.
    static void openInWindow(String id, String title) {
        ProductForm view = new ProductForm(CN.getCurrentForm(), false);
        if (Windows.open(title, view.root, 120f, 160f) == null) {
            show(id, title);
            return;
        }
        view.load(id);
    }

    /// Add to cart from a card's menu: straight in when there is one variant
    /// to buy, the product page otherwise (a size has to be chosen).
    static void quickAdd(final String id) {
        Api.catalog().product(id, Net.to(p -> {
            List<VariantDto> variants = p.variants();
            if (variants != null && variants.size() == 1 && Variants.buyable(variants.get(0))) {
                ShopState.add(variants.get(0).id(), 1, () -> {
                    ShopUi.tap();
                    ShopUi.badge(CN.getCurrentForm());
                    Ui.say(Lang.tr("Added {0} to your cart", p.title()));
                }, Ui::fail);
            } else {
                show(p.id(), p.title());
            }
        }, Ui::fail));
    }

    private void listen() {
        LiveChannel.listen(new LiveChannel.Adapter() {
            @Override
            public void stockChanged(String variantId, String locationId) {
                if (product != null && has(variantId)) {
                    load(product.id());
                }
            }

            @Override
            public void changed(String type, Map<String, Object> frame) {
            }
        });
    }

    private boolean has(String variantId) {
        for (int iter = 0; product.variants() != null && iter < product.variants().size(); iter++) {
            if (product.variants().get(iter).id().equals(variantId)) {
                return true;
            }
        }
        return false;
    }

    private void load(String id) {
        Api.catalog().product(id, Net.to(p -> {
            boolean first = product == null;
            product = p;
            if (first) {
                chosen = Variants.initial(p.options(), p.variants());
            }
            build();
        }, ShopUi.failed(root, () -> load(id))));
    }

    private void build() {
        root.removeAll();
        Container page = Ui.page();
        page.setName("product-page");
        int width = wide ? Math.min(CN.getDisplayWidth() / 2, CN.convertToPixels(120))
                : Math.min(CN.getDisplayWidth(), Layouts.column());
        int height = width * 4 / 5;
        gallery = gallery(width, height);
        Container info = info();
        if (wide) {
            Container split = new Container(new GridLayout(1, 2));
            split.setUIID("ShopProductSplit");
            split.add(gallery).add(info);
            page.add(split);
        } else {
            page.add(gallery);
            page.add(info);
        }
        if (product.description() != null && product.description().length() > 0) {
            Ui.section(page, "About this product");
            page.add(Ui.text(product.description(), "SvText"));
        }
        Ui.section(page, "Reviews");
        page.add(ProductReviews.create(product.id(), product.title()));
        root.add(BorderLayout.CENTER, page);
        Ui.relayout(root);
    }

    // --------------------------------------------------------------- gallery

    private Component gallery(int width, int height) {
        List<ProductPhotoDto> photos = product.photos();
        Tabs tabs = new Tabs();
        tabs.setUIID("ShopGallery");
        tabs.hideTabs();
        tabs.setSwipeActivated(true);
        tabs.setName("gallery");
        picture = null;
        if (photos != null && !photos.isEmpty()) {
            for (ProductPhotoDto photo : photos) {
                Label l = ProductArt.label(product.id(), product.title(), product.productType(),
                        photo.photoId(), width, height, "ShopGalleryImage");
                if (picture == null) {
                    picture = l.getIcon();
                }
                tabs.addTab("", l);
            }
        } else {
            // No photos: the product's art, then one picture per variant, so
            // there is something to swipe through.
            tabs.addTab("", art(product.id(), width, height));
            List<VariantDto> variants = product.variants();
            for (int iter = 0; variants != null && iter < variants.size() && iter < 3; iter++) {
                if (variants.size() > 1) {
                    tabs.addTab("", art(variants.get(iter).id(), width, height));
                }
            }
        }
        // The dots are buttons, one per picture, that jump to it -- a swipe
        // is not the only way through, and a mouse cannot swipe. Each says
        // which picture it is and whether it is the one showing, the way a
        // radio button would.
        final Container dots = new Container(new FlowLayout(Component.CENTER));
        dots.setUIID("ShopDots");
        final int pages = tabs.getTabCount();
        for (int iter = 0; iter < pages && pages > 1; iter++) {
            final int index = iter;
            Button dot = new Button("", iter == 0 ? "ShopDotOn" : "ShopDot");
            dot.setName("gallery-dot-" + (iter + 1));
            dot.addActionListener(e -> tabs.setSelectedIndex(index, true));
            dots.add(dot);
        }
        markDots(dots, 0);
        tabs.addSelectionListener((old, now) -> {
            markDots(dots, now);
            dots.repaint();
        });
        tabs.setPreferredH(height);
        Container g = new Container(new LayeredLayout());
        g.add(tabs);
        g.add(BorderLayout.south(dots));
        if (pages > 1 && (wide || CN.isDesktop())) {
            // A mouse gets arrows as well, either side of the picture.
            Button back = arrow(FontImage.MATERIAL_CHEVRON_LEFT, "gallery-previous",
                    Lang.tr("Previous picture"), e -> tabs.setSelectedIndex(
                            (tabs.getSelectedIndex() + pages - 1) % pages, true));
            Button next = arrow(FontImage.MATERIAL_CHEVRON_RIGHT, "gallery-next",
                    Lang.tr("Next picture"), e -> tabs.setSelectedIndex(
                            (tabs.getSelectedIndex() + 1) % pages, true));
            Container sides = new Container(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER));
            sides.add(BorderLayout.WEST, FlowLayout.encloseCenterMiddle(back));
            sides.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(next));
            g.add(sides);
        }
        return g;
    }

    private static Button arrow(char icon, String name, String says,
            ActionListener<ActionEvent> onClick) {
        Button b = new Button("", "ShopGalleryArrow");
        b.setName(name);
        b.setAccessibilityText(says);
        FontImage.setMaterialIcon(b, icon, 3.6f);
        b.addActionListener(onClick);
        return b;
    }

    private static void markDots(Container dots, int now) {
        int pages = dots.getComponentCount();
        for (int iter = 0; iter < pages; iter++) {
            Component dot = dots.getComponentAt(iter);
            dot.setUIID(iter == now ? "ShopDotOn" : "ShopDot");
            dot.setAccessibilityText(Lang.tr(iter == now ? "Picture {0} of {1}, showing"
                    : "Picture {0} of {1}", String.valueOf(iter + 1), String.valueOf(pages)));
        }
    }

    private Label art(String seed, int width, int height) {
        Label l = new Label("", "ShopGalleryImage");
        Image image = ProductArt.art(seed, product.title(), product.productType(), width, height);
        if (picture == null) {
            picture = image;
        }
        l.setIcon(image);
        return l;
    }

    // ------------------------------------------------------------------ info

    private Container info() {
        Container info = new Container(BoxLayout.y());
        info.setUIID("ShopProductInfo");
        Container head = new Container(new BorderLayout());
        Container words = new Container(BoxLayout.y());
        if (product.vendor() != null && product.vendor().length() > 0) {
            words.add(Ui.plain(product.vendor(), "ShopVendor"));
        }
        words.add(Ui.text(product.title(), "SvTitle"));
        head.add(BorderLayout.CENTER, words);
        Container actions = new Container(BoxLayout.x());
        final Button heart = new Button("", "ShopIconButton");
        heart.setName("wishlist");
        FontImage.setMaterialIcon(heart, ProductCard.wishIcon(ShopState.wished(product.id())), 3.8f);
        heart.addActionListener(e -> {
            if (com.codenameone.examples.bazaar.ui.Nav.guest()) {
                // The wishlist is an account's: saved once signed in.
                com.codenameone.examples.bazaar.ui.SignInGate.require(heart.getComponentForm(),
                        "Save it for later", "Sign in and your wishlist follows you to "
                        + "every device.", () -> ShopState.syncAccount(() -> {
                            if (!ShopState.wished(product.id())) {
                                ShopState.toggleWish(product.id());
                            }
                        }));
                return;
            }
            boolean on = ShopState.toggleWish(product.id());
            ShopUi.tap();
            FontImage.setMaterialIcon(heart, ProductCard.wishIcon(on), 3.8f);
            Ui.say(on ? "Saved to your wishlist" : "Removed from your wishlist");
        });
        Button share = new Button("", "ShopIconButton");
        share.setName("share");
        FontImage.setMaterialIcon(share, FontImage.MATERIAL_SHARE, 3.8f);
        share.setAccessibilityText(Lang.tr("Share"));
        share.addActionListener(e -> ShopSections.share(share, product.storeId(),
                product.title(), product.handle()));
        heart.setAccessibilityText(Lang.tr("Save to wishlist"));
        actions.add(heart).add(share);
        head.add(BorderLayout.EAST, actions);
        info.add(head);
        if (product.ratingCount() > 0) {
            // The stars open the reviews behind them.
            Button rating = new Button("", "ShopRatingLink");
            rating.setName("rating");
            rating.setAccessibilityText(Lang.tr("{0} reviews", String.valueOf(product.ratingCount())));
            Container row = ShopUi.stars(product.rating(), product.ratingCount());
            Container tap = new Container(new BorderLayout());
            tap.setUIID("ShopRatingRow");
            tap.add(BorderLayout.CENTER, row);
            Label more = new Label(Lang.tr("See reviews"), "ShopRatingMore");
            tap.add(BorderLayout.EAST, more);
            tap.setLeadComponent(rating);
            tap.add(BorderLayout.WEST, rating);
            rating.addActionListener(e -> ProductReviews.showAll(product.id(), product.title()));
            info.add(tap);
        }
        priceRow.setUIID("ShopPriceRow");
        info.add(priceRow);
        options.removeAll();
        info.add(options);
        info.add(stock);
        info.add(stepper());
        add = Ui.primary("Add to cart", "add-to-cart", e -> addToCart());
        FontImage.setMaterialIcon(add, FontImage.MATERIAL_ADD_SHOPPING_CART, 3.4f);
        info.add(add);
        drawOptions();
        return info;
    }

    private Container stepper() {
        // The label sits on the stepper's middle line (a BorderLayout row
        // with its centre centred), not at the top of a flow row.
        Container row = new Container(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE));
        row.setUIID("ShopQtyRow");
        row.add(BorderLayout.WEST, Ui.label("Quantity", "ShopQtyLabel"));
        Container stepper = stepper(count, "qty", e -> step(-1), e -> step(1));
        row.add(BorderLayout.CENTER, FlowLayout.encloseMiddle(stepper));
        count.setText(String.valueOf(quantity));
        return row;
    }

    /// An outlined `- value +` box: square buttons either side of `value`,
    /// each named for a test and labelled for a screen reader. The cart's
    /// lines use the same one.
    static Container stepper(Label value, String name, ActionListener<ActionEvent> less,
            ActionListener<ActionEvent> more) {
        Container stepper = new Container(BoxLayout.x());
        stepper.setUIID("ShopStepper");
        Button down = new Button("", "ShopStepButton");
        down.setName(name + "-less");
        down.setAccessibilityText(Lang.tr("Decrease quantity"));
        FontImage.setMaterialIcon(down, FontImage.MATERIAL_REMOVE, 3.2f);
        Button up = new Button("", "ShopStepButton");
        up.setName(name + "-more");
        up.setAccessibilityText(Lang.tr("Increase quantity"));
        FontImage.setMaterialIcon(up, FontImage.MATERIAL_ADD, 3.2f);
        value.setUIID("ShopStepValue");
        value.setName(name);
        down.addActionListener(less);
        up.addActionListener(more);
        stepper.add(down).add(value).add(up);
        return stepper;
    }

    private void step(int by) {
        int most = 99;
        VariantDto v = variant();
        if (v != null && v.trackInventory() && !v.allowBackorder() && v.available() > 0) {
            most = Math.min(most, v.available());
        }
        quantity = Math.max(1, Math.min(most, quantity + by));
        count.setText(String.valueOf(quantity));
        ShopUi.tap();
        count.getParent().revalidate();
    }

    private VariantDto variant() {
        return Variants.resolve(product.options(), product.variants(), chosen);
    }

    /// Draws the chips, the price and the availability for what is chosen.
    private void drawOptions() {
        options.removeAll();
        List<OptionDto> list = product.options();
        List<VariantDto> variants = product.variants();
        boolean single = variants != null && variants.size() == 1 && (list == null
                || list.isEmpty() || list.get(0).values() == null
                || list.get(0).values().size() <= 1);
        for (int position = 0; !single && list != null && position < list.size()
                && position < 3; position++) {
            OptionDto option = list.get(position);
            options.add(Ui.label(Lang.tr("{0}: {1}", option.name(),
                    chosen[position] == null ? "" : chosen[position]), "ShopFilterTitle"));
            Container chips = new Container(new FlowLayout());
            chips.setUIID("ShopChips");
            for (final String value : option.values()) {
                final int at = position;
                boolean on = value.equals(chosen[position]);
                boolean possible = Variants.possible(list, variants, chosen, position, value);
                Button chip = new Button(value, on ? "ShopChipOn"
                        : possible ? "ShopChip" : "ShopChipOut");
                chip.setName("option-" + position + "-" + value);
                // Said like a radio button: which value, and its state.
                chip.setAccessibilityText(on ? Lang.tr("{0}, selected", value)
                        : possible ? value : Lang.tr("{0}, unavailable", value));
                chip.addActionListener(e -> {
                    chosen[at] = value;
                    if (Variants.resolve(product.options(), product.variants(), chosen) == null) {
                        // No such combination: keep the value, take the first
                        // variant that has it for the rest.
                        for (VariantDto v : product.variants()) {
                            if (value.equals(Variants.value(v, at))) {
                                for (int p = 0; p < 3; p++) {
                                    chosen[p] = p < product.options().size()
                                            ? Variants.value(v, p) : null;
                                }
                                break;
                            }
                        }
                    }
                    ShopUi.tap();
                    drawOptions();
                    Ui.relayout(options);
                });
                chips.add(chip);
            }
            options.add(chips);
        }
        drawPrice();
    }

    private void drawPrice() {
        priceRow.removeAll();
        VariantDto v = variant();
        if (v == null) {
            priceRow.add(Ui.plain(Lang.tr("Not available in this combination"), "SvMuted"));
            stock.setText("");
            add.setEnabled(false);
            return;
        }
        priceRow.add(Ui.plain(ShopState.money(v.priceCents()), "ShopPriceBig"));
        int off = Money.percentOff(v.priceCents(), v.compareAtCents());
        if (off > 0) {
            priceRow.add(Ui.plain(ShopState.money(v.compareAtCents()), "ShopCompareAt"));
            priceRow.add(Ui.badge(Lang.tr("Save {0}%", String.valueOf(off)), "ShopSale"));
        }
        boolean buyable = Variants.buyable(v);
        add.setEnabled(buyable);
        add.setText(Lang.tr(buyable ? "Add to cart" : "Sold out"));
        StockDto best = Variants.bestStock(v);
        if (!v.trackInventory()) {
            stock.setText(Lang.tr("In stock"));
            stock.setUIID("ShopStock");
        } else if (Variants.low(best)) {
            stock.setText(ShopUi.left(best.available(), best.locationName()));
            stock.setUIID("ShopStockLow");
        } else if (best != null) {
            stock.setText(Lang.tr("In stock at {0}", best.locationName()));
            stock.setUIID("ShopStock");
        } else if (v.allowBackorder()) {
            stock.setText(Lang.tr("On backorder: ships when restocked"));
            stock.setUIID("ShopStockLow");
        } else {
            stock.setText(Lang.tr("Sold out"));
            stock.setUIID("ShopStockOut");
        }
        quantity = Math.max(1, quantity);
        count.setText(String.valueOf(quantity));
    }

    private void addToCart() {
        final VariantDto v = variant();
        if (v == null) {
            return;
        }
        add.setEnabled(false);
        ShopState.add(v.id(), quantity, () -> {
            add.setEnabled(true);
            ShopUi.tap();
            final Form form = host != null ? host : CN.getCurrentForm();
            ShopUi.fly(form, picture, gallery, add, () -> {
                ShopUi.badge(CN.getCurrentForm());
                Ui.say(Lang.tr("Added {0} to your cart", product.title()));
            });
        }, (status, message) -> {
            add.setEnabled(true);
            Ui.fail(status, message);
        });
    }
}
