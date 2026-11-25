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
package com.codenameone.examples.bazaar.store;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.catalog.Catalog;
import com.codenameone.examples.bazaar.domain.Collection;
import com.codenameone.examples.bazaar.domain.CollectionProduct;
import com.codenameone.examples.bazaar.domain.InventoryLevel;
import com.codenameone.examples.bazaar.domain.InventoryMove;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.Photo;
import com.codenameone.examples.bazaar.pay.Payments;
import com.codenameone.examples.bazaar.domain.Product;
import com.codenameone.examples.bazaar.domain.ProductOption;
import com.codenameone.examples.bazaar.domain.ProductPhoto;
import com.codenameone.examples.bazaar.domain.PurchaseLine;
import com.codenameone.examples.bazaar.domain.PurchaseOrder;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreStaff;
import com.codenameone.examples.bazaar.domain.Supplier;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.inventory.Stock;
import com.codenameone.examples.bazaar.repository.CollectionProductRepository;
import com.codenameone.examples.bazaar.repository.OrderLineRepository;
import com.codenameone.examples.bazaar.repository.RegisterRepository;
import com.codenameone.examples.bazaar.repository.StoreOrderRepository;
import com.codenameone.examples.bazaar.domain.OrderLine;
import com.codenameone.examples.bazaar.domain.Register;
import com.codenameone.examples.bazaar.domain.StoreOrder;
import com.codenameone.examples.bazaar.order.Orders;
import com.codenameone.examples.bazaar.repository.CollectionRepository;
import com.codenameone.examples.bazaar.repository.InventoryLevelRepository;
import com.codenameone.examples.bazaar.repository.InventoryMoveRepository;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.repository.ProductOptionRepository;
import com.codenameone.examples.bazaar.repository.ProductRepository;
import com.codenameone.examples.bazaar.repository.PurchaseLineRepository;
import com.codenameone.examples.bazaar.repository.PurchaseOrderRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.StoreStaffRepository;
import com.codenameone.examples.bazaar.repository.SupplierRepository;
import com.codenameone.examples.bazaar.repository.PhotoRepository;
import com.codenameone.examples.bazaar.repository.ProductPhotoRepository;
import com.codenameone.examples.bazaar.repository.ReviewRepository;
import com.codenameone.examples.bazaar.domain.Review;
import com.codenameone.examples.bazaar.repository.VariantRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// The stores a development server starts with: Northwind Outfitters
/// (apparel and outdoor gear, three locations) and Little Clay Studio
/// (ceramics, one location), with about thirty products each, stock that is
/// plentiful, low and gone, collections of both kinds, a supplier and an open
/// purchase order.
///
/// Every product has a photo, and some have two: JPEGs under
/// `src/main/resources/seed-media`, named by the product's handle (`-2` for
/// the second), with their provenance in `IMAGE-CREDITS.txt` beside them.
/// Each store's hero is one of its product photos.
/// Every opening stock level is written with a RESTOCK move beside it, so the
/// ledger adds up from the first request. A store whose handle exists is left
/// alone, so starting the server again adds nothing.
@Component
public class DemoStores {
    public static final String NORTHWIND = "northwind";
    public static final String LITTLE_CLAY = "littleclay";
    /// The demo cashier's register PIN. Not a secret, as the demo passwords
    /// are not.
    public static final String CASHIER_PIN = "1234";

    /// Title, type, vendor, tags, price in cents, options (S size, C colour,
    /// SC both, empty none), and a line of description.
    private static final String[][] NORTHWIND_PRODUCTS = {
        {"Ridgeline Rain Shell", "Jackets", "Northwind", "rain, waterproof, hiking", "14900", "SC",
            "A three-layer waterproof shell with pit zips and a helmet-ready hood."},
        {"Summit Down Parka", "Jackets", "Northwind", "winter, down, insulated", "26900", "SC",
            "700-fill responsibly sourced down for the coldest mornings at camp."},
        {"Trailhead Fleece Jacket", "Jackets", "Northwind", "fleece, layering", "8900", "SC",
            "Midweight recycled fleece that layers under a shell or stands alone."},
        {"Coastal Windbreaker", "Jackets", "Harbor & Pine", "wind, packable, summer", "6900", "SC",
            "Packs into its own chest pocket and shrugs off a sea breeze."},
        {"Alpine Softshell", "Jackets", "Northwind", "softshell, climbing", "15900", "SC",
            "Stretchy, breathable and water resistant for fast days on rock."},
        {"Merino Base Layer Crew", "Tops", "Woolly Goat", "merino, base-layer, winter", "7900",
            "SC", "Soft 200 g/m2 merino that stays warm when damp and never smells."},
        {"Basecamp Flannel Shirt", "Tops", "Harbor & Pine", "flannel, casual", "5900", "SC",
            "Brushed cotton flannel in a classic plaid, cut for movement."},
        {"Sun Hoodie", "Tops", "Northwind", "sun, summer, upf50", "5400", "SC",
            "UPF 50 lightweight hoodie with thumb loops for long days on the water."},
        {"Everyday Organic Tee", "Tops", "Harbor & Pine", "cotton, summer, basics", "2800", "SC",
            "Heavyweight organic cotton tee, garment dyed for a lived-in feel."},
        {"Quarter-Zip Pullover", "Tops", "Woolly Goat", "merino, layering", "9800", "SC",
            "A merino-blend quarter zip that goes from trail to town."},
        {"Switchback Hiking Pants", "Bottoms", "Northwind", "hiking, stretch", "8900", "SC",
            "Four-way stretch with a gusseted crotch and zip thigh pocket."},
        {"Convertible Trek Pants", "Bottoms", "Northwind", "hiking, summer, convertible", "7900",
            "SC", "Zip-off legs turn these into shorts at the trailhead."},
        {"Canyon Shorts", "Bottoms", "Harbor & Pine", "summer, shorts", "4900", "SC",
            "Quick-dry nylon shorts with a 7 inch inseam and a stash pocket."},
        {"Thermal Leggings", "Bottoms", "Woolly Goat", "merino, base-layer, winter", "6900", "S",
            "Merino leggings for under ski pants or a cold night in the tent."},
        {"Ridge Runner Trail Shoes", "Footwear", "Peakstep", "trail-running, shoes", "13900",
            "S", "Grippy lugs, a rock plate and a roomy toe box for long miles."},
        {"Timberline Hiking Boots", "Footwear", "Peakstep", "boots, waterproof, hiking", "18900",
            "S", "Waterproof leather mid boots that need almost no breaking in."},
        {"Camp Slip-Ons", "Footwear", "Peakstep", "camp, casual", "5900", "S",
            "Insulated slip-ons for the walk from the tent to the coffee."},
        {"Merino Hiking Socks", "Accessories", "Woolly Goat", "merino, socks, hiking", "2400",
            "S", "Cushioned merino socks with a lifetime guarantee."},
        {"Wool Beanie", "Accessories", "Woolly Goat", "winter, hats, gift", "3200", "C",
            "Ribbed merino beanie with a fold-up cuff."},
        {"Trucker Cap", "Accessories", "Northwind", "hats, summer, gift", "2800", "C",
            "Embroidered mountain patch on a breathable mesh-back cap."},
        {"Insulated Water Bottle", "Gear", "Northwind", "hydration, gift", "3400", "C",
            "Keeps water cold for a day and coffee hot through the morning."},
        {"Daypack 22L", "Gear", "Northwind", "packs, hiking", "9900", "C",
            "A 22 litre daypack with a ventilated back panel and hip belt pockets."},
        {"Expedition Pack 60L", "Gear", "Northwind", "packs, backpacking", "24900", "",
            "Adjustable torso, removable lid and room for a week out."},
        {"Ultralight Tent 2P", "Gear", "Summit Supply", "camping, tents, backpacking", "37900",
            "", "Two-person freestanding tent at under 1.3 kg packed."},
        {"Down Sleeping Bag", "Gear", "Summit Supply", "camping, sleeping, down", "29900", "",
            "Rated to -7 C with a draft collar and a zip that never snags."},
        {"Trekking Poles", "Gear", "Summit Supply", "hiking, poles", "11900", "",
            "Carbon three-section poles with cork grips and flick locks."},
        {"Headlamp 400", "Gear", "Summit Supply", "lighting, camping, gift", "4400", "",
            "400 lumens, a red night mode and USB-C charging."},
        {"Camp Mug", "Gear", "Northwind", "camp, gift", "1800", "C",
            "Enamel steel mug that goes from the stove to your hands."},
        {"Packable Rain Poncho", "Gear", "Harbor & Pine", "rain, packable", "3900", "",
            "Covers you and your pack, and folds into a fist-sized pouch."},
        {"Gift Card Envelope", "Accessories", "Northwind", "gift", "500", "",
            "A recycled-paper envelope and card for a gift card. Track-free."},
    };

    private static final String[][] LITTLE_CLAY_PRODUCTS = {
        {"Speckled Morning Mug", "Mugs", "Little Clay", "stoneware, gift, bestseller", "3200", "C",
            "Wheel-thrown stoneware with an oatmeal speckle; holds 350 ml."},
        {"Tall Latte Mug", "Mugs", "Little Clay", "stoneware, coffee", "3600", "C",
            "A tall, narrow mug for a proper latte and a comfortable handle."},
        {"Espresso Cup Pair", "Mugs", "Little Clay", "espresso, gift", "4200", "C",
            "Two 90 ml cups with a glossy interior and raw clay foot."},
        {"Handleless Tea Cup", "Mugs", "Little Clay", "tea, gift", "2400", "C",
            "A tea bowl to wrap both hands around on a cold evening."},
        {"Camp-Style Enamel Mug", "Mugs", "Kiln & Co", "rustic, coffee", "2600", "C",
            "Our take on the camp mug: blue rim, white glaze, hand-dipped."},
        {"Travel Tumbler", "Mugs", "Kiln & Co", "coffee, lid", "3800", "C",
            "Porcelain tumbler with a cork lid for the commute."},
        {"Dinner Plate", "Tableware", "Little Clay", "dinnerware, stoneware", "3400", "C",
            "A 27 cm plate with a gentle lip that holds sauces in."},
        {"Side Plate", "Tableware", "Little Clay", "dinnerware, stoneware", "2400", "C",
            "Twenty centimetres across, for toast, cake or tapas."},
        {"Pasta Bowl", "Tableware", "Little Clay", "dinnerware, bowls", "3200", "C",
            "Wide and shallow, made for a twirl of spaghetti."},
        {"Cereal Bowl", "Tableware", "Little Clay", "dinnerware, bowls, bestseller", "2600", "C",
            "Deep enough for a generous breakfast, stackable in the cupboard."},
        {"Serving Platter", "Tableware", "Kiln & Co", "serving, gift", "6800", "C",
            "An oval platter for the roast or a cheese board."},
        {"Salad Bowl Large", "Tableware", "Kiln & Co", "serving, bowls", "7400", "C",
            "A large salad bowl with a carved outer wall."},
        {"Butter Dish", "Tableware", "Little Clay", "kitchen, gift", "3800", "",
            "Keeps butter soft on the counter and dust off it."},
        {"Olive Oil Cruet", "Kitchen", "Little Clay", "kitchen, pouring", "4400", "",
            "A drip-free pouring spout and a cork stopper."},
        {"Salt Cellar", "Kitchen", "Little Clay", "kitchen, gift", "2200", "",
            "Wide mouth for a pinch, with an olive wood lid."},
        {"Utensil Crock", "Kitchen", "Kiln & Co", "kitchen, storage", "4800", "C",
            "Heavy enough that the spatulas never tip it over."},
        {"Berry Colander", "Kitchen", "Little Clay", "kitchen, summer", "3600", "",
            "Pierced bowl and saucer for rinsing and serving berries."},
        {"Pour-Over Coffee Dripper", "Kitchen", "Little Clay", "coffee, gift", "4200", "C",
            "Spiral ribs inside give an even, clean extraction."},
        {"Teapot", "Kitchen", "Kiln & Co", "tea, gift", "8800", "C",
            "A one-litre teapot with an internal strainer."},
        {"Bud Vase", "Home", "Little Clay", "vases, gift, bestseller", "2200", "C",
            "Small vase for a single stem on a windowsill."},
        {"Table Vase", "Home", "Little Clay", "vases, home", "5800", "C",
            "A tall vase with a textured carved surface."},
        {"Hanging Planter", "Home", "Kiln & Co", "plants, home", "4600", "C",
            "Comes with a jute hanger and a drainage hole."},
        {"Succulent Pot Trio", "Home", "Kiln & Co", "plants, gift", "3900", "",
            "Three little pots with saucers, sized for succulents."},
        {"Incense Holder", "Home", "Little Clay", "home, gift", "1600", "C",
            "A smooth pebble-shaped holder that catches the ash."},
        {"Ring Dish", "Home", "Little Clay", "jewelry, gift", "1800", "C",
            "A small dish by the sink for rings and earrings."},
        {"Candle in Ceramic Cup", "Home", "Kiln & Co", "candles, gift", "3400", "",
            "Soy wax candle poured into a reusable cup; fig and cedar."},
        {"Wall Tile Set", "Home", "Kiln & Co", "art, home", "9600", "",
            "Four hand-painted tiles to hang or to use as trivets."},
        {"Beginner Wheel Class", "Classes", "Little Clay", "class, experience, gift", "8500", "",
            "Two hours on the wheel with a potter; we glaze and fire your piece."},
        {"Glaze Sampler Workshop", "Classes", "Little Clay", "class, experience", "6500", "",
            "Learn how glazes behave and leave with a set of test tiles."},
        {"Seconds Mystery Box", "Home", "Little Clay", "seconds, sale", "4000", "",
            "Three pieces with small flaws, chosen by us. Final sale."},
    };

    private static final String[] SIZES = {"S", "M", "L", "XL"};
    private static final String[] SHOE_SIZES = {"8", "9", "10", "11"};
    private static final String[] APPAREL_COLORS = {"Forest", "Charcoal", "Sand"};
    private static final String[] CLAY_COLORS = {"Oatmeal", "Sea glass", "Charcoal"};

    private final StoreRepository stores;
    private final StoreStaffRepository staff;
    private final LocationRepository locations;
    private final ProductRepository products;
    private final ProductOptionRepository options;
    private final VariantRepository variants;
    private final InventoryLevelRepository levels;
    private final InventoryMoveRepository moves;
    private final CollectionRepository collections;
    private final CollectionProductRepository members;
    private final SupplierRepository suppliers;
    private final PurchaseOrderRepository orders;
    private final PurchaseLineRepository orderLines;
    private final Accounts accounts;
    private final PasswordEncoder encoder;
    private final RegisterRepository registers;
    private final StoreOrderRepository sales;
    private final OrderLineRepository saleLines;
    private final com.codenameone.examples.bazaar.pricing.DemoPricing pricing;
    private final com.codenameone.examples.bazaar.automation.Automations automations;
    private final com.codenameone.examples.bazaar.marketing.DemoMarketing marketing;
    private final PhotoRepository photos;
    private final ProductPhotoRepository gallery;
    private final ReviewRepository reviews;
    private final DemoFunnel funnel;
    private final com.codenameone.examples.bazaar.repository.RefundRepository refunds;

    private static final String[] REVIEWERS = {"Maya", "Daniel", "Priya", "Tom", "Lena",
        "Omar", "Grace", "Kenji", "Sofia", "Ben", "Aisha", "Lucas"};
    private static final String[][] REVIEW_TEXT = {
        {"Exactly as pictured", "Lovely finish and it arrived well packed."},
        {"Use it every day", "Feels solid and has held up beautifully."},
        {"Great gift", "Bought one for my sister and ended up ordering another for me."},
        {"Good, not perfect", "Nice quality, though a little smaller than I expected."},
        {"Worth it", "You can tell it was made with care. Shipping was quick too."},
    };

    public DemoStores(StoreRepository stores, StoreStaffRepository staff,
            LocationRepository locations, ProductRepository products,
            ProductOptionRepository options, VariantRepository variants,
            InventoryLevelRepository levels, InventoryMoveRepository moves,
            CollectionRepository collections, CollectionProductRepository members,
            SupplierRepository suppliers, PurchaseOrderRepository orders,
            PurchaseLineRepository orderLines, Accounts accounts, PasswordEncoder encoder,
            com.codenameone.examples.bazaar.pricing.DemoPricing pricing,
            com.codenameone.examples.bazaar.automation.Automations automations,
            RegisterRepository registers, StoreOrderRepository sales,
            OrderLineRepository saleLines,
            com.codenameone.examples.bazaar.marketing.DemoMarketing marketing,
            PhotoRepository photos, ProductPhotoRepository gallery, ReviewRepository reviews,
            DemoFunnel funnel,
            com.codenameone.examples.bazaar.repository.RefundRepository refunds,
            com.codenameone.examples.bazaar.repository.PaymentRepository payments) {
        this.payments = payments;
        this.funnel = funnel;
        this.refunds = refunds;
        this.reviews = reviews;
        this.photos = photos;
        this.gallery = gallery;
        this.marketing = marketing;
        this.registers = registers;
        this.sales = sales;
        this.saleLines = saleLines;
        this.pricing = pricing;
        this.automations = automations;
        this.stores = stores;
        this.staff = staff;
        this.locations = locations;
        this.products = products;
        this.options = options;
        this.variants = variants;
        this.levels = levels;
        this.moves = moves;
        this.collections = collections;
        this.members = members;
        this.suppliers = suppliers;
        this.orders = orders;
        this.orderLines = orderLines;
        this.accounts = accounts;
        this.encoder = encoder;
    }

    /// Makes both stores, unless they exist. `owner` owns both, so the demo
    /// shows switching between stores; `cashier` works Northwind's downtown
    /// register.
    public void seed(String owner, String cashier) throws IOException {
        if (!exists(NORTHWIND)) {
            northwind(owner, cashier, encoder.encode(CASHIER_PIN));
        }
        if (!exists(LITTLE_CLAY)) {
            littleClay(owner);
        }
        seedPricing(NORTHWIND);
        seedPricing(LITTLE_CLAY);
        seedFloor(NORTHWIND, owner, cashier);
        seedFloor(LITTLE_CLAY, owner, owner);
        seedMarketing(NORTHWIND);
        seedMarketing(LITTLE_CLAY);
        accounts.setMerchant(owner, true);
        accounts.setMerchant(cashier, true);
    }

    /// Loyalty, a club, sales, segments, campaigns and a journey, once per
    /// store; after the floor, so the campaigns have customers and orders.
    private void seedMarketing(String handle) throws IOException {
        Store store = stores.findByHandle(handle);
        if (store != null) {
            marketing.seed(store.getId());
        }
    }

    /// Shipping zones, taxes, the demo discount codes and the demo
    /// automations, once per store.
    private void seedPricing(String handle) throws IOException {
        Store store = stores.findByHandle(handle);
        if (store != null) {
            pricing.seed(store.getId());
            automations.seedDemo(store.getId());
        }
    }

    /// A register at every shop floor, and a month of past sales, once per
    /// store, so the register opens and the dashboard and analytics have
    /// something to show on a first run.
    private void seedFloor(String handle, String owner, String cashier) throws IOException {
        Store store = stores.findByHandle(handle);
        if (store == null) {
            return;
        }
        List<Register> tills = new ArrayList<Register>();
        if (registers.findByStoreId(store.getId()).isEmpty()) {
            com.codenameone.examples.bazaar.domain.TaxRate tax = pricing.counterTax(store.getId());
            for (Location place : locations.findByStoreIdOrderByPositionAsc(store.getId())) {
                if ("WAREHOUSE".equals(place.getKind())) {
                    continue;
                }
                Register r = new Register();
                r.setId(Ids.next());
                r.setStoreId(store.getId());
                r.setLocationId(place.getId());
                r.setName(place.getName() + " register");
                r.setActive(place.isActive());
                if (tax != null) {
                    r.setTaxCountry(tax.getCountry());
                    r.setTaxRegion(tax.getRegion());
                }
                registers.insert(r);
                tills.add(r);
            }
        } else {
            tills.addAll(registers.findByStoreId(store.getId()));
        }
        if (sales.findFirstByStoreIdOrderByNumberDesc(store.getId()) == null) {
            history(store, tills, owner, cashier);
        }
    }

    @Transactional(rollbackFor = IOException.class)
    void history(Store store, List<Register> tills, String owner, String cashier)
            throws IOException {
        List<Variant> stock = variants.findByStoreId(store.getId());
        if (stock.isEmpty()) {
            return;
        }
        java.util.Map<String, String> titles = new java.util.HashMap<String, String>();
        List<String> browse = new ArrayList<String>();
        for (Product p : products.findByStoreIdOrderByTitleAsc(store.getId())) {
            titles.put(p.getId(), p.getTitle());
            if (Catalog.ACTIVE.equals(p.getStatus())) {
                browse.add(p.getId());
            }
        }
        boolean big = NORTHWIND.equals(store.getHandle());
        long day = 24L * 60 * 60 * 1000;
        long now = System.currentTimeMillis();
        long today = now / day;
        // The same variant order on every run: the store's query has none.
        stock = new ArrayList<Variant>(stock);
        java.util.Collections.sort(stock, (x, y) -> x.getSku().compareTo(y.getSku()));
        int number = 1001;
        int serial = 0;
        List<StoreOrder> made = new ArrayList<StoreOrder>();
        List<OrderLine> madeLines = new ArrayList<OrderLine>();
        List<Object[]> behind = new ArrayList<Object[]>();
        List<com.codenameone.examples.bazaar.domain.Payment> madePayments =
                new ArrayList<com.codenameone.examples.bazaar.domain.Payment>();
        List<com.codenameone.examples.bazaar.domain.Refund> madeRefunds =
                new ArrayList<com.codenameone.examples.bazaar.domain.Refund>();
        for (int ago = HISTORY_DAYS - 1; ago >= 0; ago--) {
            long epochDay = today - ago;
            int count = ordersOn(big, epochDay, ago);
            if (FENNEL_FIG.equals(store.getHandle())) {
                // A shade quieter than Little Clay, and not its twin: the same
                // curve gave both small stores the same order count.
                count = Math.max(1, count * 4 / 5 + mix((int) epochDay, 15) % 2);
            }
            for (int k = 0; k < count; k++) {
                // Spread over the thirteen hours before this time of day,
                // counted from now rather than from midnight: a seed made at
                // 02:00 still has a morning of orders "today" and the charts
                // look the same whatever the clock says.
                long at = now - ago * day - 20 * 60_000L
                        - (count - 1 - k) * (13 * 3600_000L) / count
                        - (mix(number, 5) % 25) * 60_000L;
                serial++;
                boolean pos = !tills.isEmpty() && mix(serial, 1) % 10 < 3;
                StoreOrder o = new StoreOrder();
                o.setId(Ids.next());
                o.setStoreId(store.getId());
                o.setNumber(number++);
                // client_id is unique; the id itself never collides.
                o.setClientId(o.getId());
                o.setChannel(pos ? Orders.POS : Orders.ONLINE);
                o.setCurrency(store.getCurrency());
                String[] who = SHOPPERS[shopper(serial)];
                o.setEmail(who[2] + "@example.com");
                if (mix(serial, 12) % 5 < 2) {
                    // A first-time shopper: two in five orders, so about a
                    // third of a month's customers have bought before.
                    String[] last = SHOPPERS[mix(serial, 13) % SHOPPERS.length];
                    who = new String[] {who[0], last[1], who[0] + "." + last[1] + serial};
                    o.setEmail(asciiLower(who[2]) + "@example.com");
                }
                if (pos) {
                    Register till = tills.get(serial % tills.size());
                    o.setLocationId(till.getLocationId());
                    o.setRegisterId(till.getId());
                    o.setStaffUsername(cashier);
                } else {
                    o.setShipName(who[0] + " " + who[1]);
                    o.setShipCountry("US");
                }
                List<OrderLine> lines = new ArrayList<OrderLine>();
                long subtotal = 0;
                int lineCount = 1 + (mix(serial, 2) % 3 == 0 ? 1 : 0)
                        + (mix(serial, 3) % 7 == 0 ? 1 : 0);
                for (int l = 0; l < lineCount; l++) {
                    Variant v = stock.get(popular(serial * 3 + l, stock.size()));
                    OrderLine line = new OrderLine();
                    line.setId(Ids.next());
                    line.setOrderId(o.getId());
                    line.setProductId(v.getProductId());
                    line.setVariantId(v.getId());
                    String title = titles.get(v.getProductId());
                    line.setTitle(title == null ? v.getTitle() : title);
                    line.setVariantTitle(v.getTitle());
                    line.setSku(v.getSku());
                    line.setQuantity(mix(serial, 4 + l) % 5 == 0 ? 2 : 1);
                    line.setPriceCents(v.getPriceCents());
                    line.setLocationId(o.getLocationId());
                    subtotal += v.getPriceCents() * line.getQuantity();
                    lines.add(line);
                }
                long discount = 0;
                if (!pos && mix(serial, 6) % 6 == 1) {
                    discount = subtotal / 10;
                    o.setDiscountCode("WELCOME10");
                }
                o.setSubtotalCents(subtotal);
                o.setDiscountCents(discount);
                o.setTotalCents(subtotal - discount);
                o.setFinancial(Orders.PAID);
                // Today's online orders are a working day's queue: the
                // morning's have gone out, the latest wait to be packed, and
                // one in four of the rest is part shipped. A list of nothing
                // but "Fulfilled" read as a store with nothing to do.
                int units = 0;
                for (int l = 0; l < lines.size(); l++) {
                    units += lines.get(l).getQuantity();
                }
                boolean dayQueue = !pos && ago == 0;
                boolean waiting = dayQueue && k * 3 >= count;
                boolean partial = dayQueue && !waiting && units > 1 && k * 3 >= count - count / 2
                        || waiting && units > 1 && mix(serial, 9) % 4 == 0;
                if (partial) {
                    waiting = false;
                }
                o.setStatus(waiting || partial ? Orders.OPEN : Orders.CLOSED);
                o.setFulfilment(partial ? Orders.PARTIAL
                        : waiting ? Orders.UNFULFILLED : Orders.FULFILLED);
                int sent = partial ? units / 2 : waiting ? 0 : units;
                for (int l = 0; l < lines.size(); l++) {
                    int q = Math.min(sent, lines.get(l).getQuantity());
                    lines.get(l).setFulfilledQty(q);
                    sent -= q;
                }
                Instant when = Instant.ofEpochMilli(at);
                o.setCreatedAt(when);
                o.setUpdatedAt(when);
                if (!waiting && !partial) {
                    o.setClosedAt(when);
                }
                if (ago >= 3 && mix(serial, 7) % 31 == 4) {
                    // A return: one line back, or now and then the whole order.
                    boolean whole = mix(serial, 8) % 4 == 0;
                    long back = whole ? o.getTotalCents()
                            : Math.min(o.getTotalCents(), lines.get(0).getPriceCents());
                    com.codenameone.examples.bazaar.domain.Refund r =
                            new com.codenameone.examples.bazaar.domain.Refund();
                    r.setId(Ids.next());
                    r.setOrderId(o.getId());
                    r.setAmountCents(back);
                    r.setReason(whole ? "Changed their mind" : "Wrong size");
                    r.setUsername(owner);
                    r.setCreatedAt(when.plusMillis(2 * day));
                    madeRefunds.add(r);
                    o.setRefundedCents(back);
                    o.setFinancial(whole ? Orders.REFUNDED : Orders.PARTIALLY_REFUNDED);
                }
                if (waiting) {
                    // Its payment is a hold until the first units ship
                    // (payment() below), so the order says so too: the
                    // merchant's list, the admin's and the capture on ship
                    // (Orders.capturePayment) all read this field.
                    o.setFinancial(Orders.AUTHORIZED);
                }
                madePayments.add(payment(o, pos, waiting, serial));
                made.add(o);
                madeLines.addAll(lines);
                if (!pos && ago < FUNNEL_DAYS) {
                    behind.add(new Object[] {o, lines, Integer.valueOf(serial)});
                }
            }
        }
        for (int iter = 0; iter < made.size(); iter++) {
            sales.insert(made.get(iter));
        }
        saleLines.saveAll(madeLines);
        for (int iter = 0; iter < madeRefunds.size(); iter++) {
            refunds.insert(madeRefunds.get(iter));
        }
        for (int iter = 0; iter < madePayments.size(); iter++) {
            payments.insert(madePayments.get(iter));
        }
        for (int iter = 0; iter < behind.size(); iter++) {
            Object[] f = behind.get(iter);
            StoreOrder o = (StoreOrder) f[0];
            List<OrderLine> ls = new ArrayList<OrderLine>();
            for (int l = 0; l < madeLines.size(); l++) {
                if (madeLines.get(l).getOrderId().equals(o.getId())) {
                    ls.add(madeLines.get(l));
                }
            }
            // The views and carts the order came from (DemoFunnel).
            funnel.behind(o, ls, browse, ((Integer) f[2]).intValue());
        }
    }

    /// The payment behind a seeded order. The admin's overview and charts
    /// count payments, not orders, and a history of orders with none read as
    /// a marketplace that had never taken a cent. An order still waiting to
    /// ship holds an authorization, as a live one does until it is captured.
    private static com.codenameone.examples.bazaar.domain.Payment payment(StoreOrder o,
            boolean pos, boolean waiting, int serial) {
        com.codenameone.examples.bazaar.domain.Payment p =
                new com.codenameone.examples.bazaar.domain.Payment();
        p.setOrderId(o.getId());
        p.setUsername(o.getEmail());
        String[] card = DEMO_CARDS[mix(serial, 14) % DEMO_CARDS.length];
        p.setMethodId(pos ? "reader" : "card");
        p.setMethodLabel(pos ? "Card reader" : card[0]);
        p.setAmountCents(o.getTotalCents());
        p.setCurrency(o.getCurrency());
        // Payments.SEEDED keeps it away from a real processor.
        p.setProviderRef(Payments.SEEDED + o.getId());
        p.setCreatedAt(o.getCreatedAt());
        long refunded = o.getRefundedCents();
        String status = waiting ? Payments.AUTHORIZED
                : refunded <= 0L ? Payments.PAID
                : refunded >= o.getTotalCents() ? Payments.REFUNDED
                : Payments.PARTIALLY_REFUNDED;
        p.setStatus(status);
        if (!waiting) {
            p.setPaidAt(o.getCreatedAt());
        }
        if (refunded > 0L) {
            p.setRefundedCents(refunded);
            p.setRefundRef(Payments.SEEDED + "r_" + o.getId());
            p.setRefundedAt(o.getCreatedAt().plusMillis(2L * 24 * 60 * 60 * 1000));
        }
        return p;
    }

    private static final String[][] DEMO_CARDS = {
        {"Visa ending 4242"}, {"Mastercard ending 4444"}, {"Visa ending 1881"},
        {"Amex ending 0005"},
    };

    /// `text` with ASCII capitals folded; names here are ASCII and the
    /// locale must not fold them.
    static String asciiLower(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            out.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return out.toString();
    }

    /// Days of seeded order history.
    static final int HISTORY_DAYS = 90;

    /// The days the seeded funnel (views and carts) covers: the default
    /// analytics period and the one it is compared with. Older orders have
    /// none, which keeps the seed quick.
    static final int FUNNEL_DAYS = 61;

    /// Shoppers of the seeded history (first, last, mailbox); the first
    /// dozen are regulars. Mailboxes are written out: no case folding.
    private static final String[][] SHOPPERS = {
        {"Ava", "Brooks", "ava.brooks"}, {"Liam", "Carter", "liam.carter"},
        {"Mia", "Nguyen", "mia.nguyen"}, {"Noah", "Patel", "noah.patel"},
        {"Emma", "Rossi", "emma.rossi"}, {"Ethan", "Kim", "ethan.kim"},
        {"Zoe", "Martin", "zoe.martin"}, {"Owen", "Garcia", "owen.garcia"},
        {"Chloe", "Silva", "chloe.silva"}, {"Leo", "Fischer", "leo.fischer"},
        {"Ivy", "Cohen", "ivy.cohen"}, {"Jack", "Murphy", "jack.murphy"},
        {"Nora", "Hughes", "nora.hughes"}, {"Eli", "Turner", "eli.turner"},
        {"Ruby", "Walsh", "ruby.walsh"}, {"Finn", "Larsen", "finn.larsen"},
        {"Hazel", "Price", "hazel.price"}, {"Theo", "Ward", "theo.ward"},
        {"Lily", "Dubois", "lily.dubois"}, {"Max", "Novak", "max.novak"},
        {"Stella", "Reyes", "stella.reyes"}, {"Sam", "Okafor", "sam.okafor"},
        {"Clara", "Berg", "clara.berg"}, {"Hugo", "Lambert", "hugo.lambert"},
        {"Iris", "Moreno", "iris.moreno"}, {"Felix", "Weber", "felix.weber"},
        {"Alice", "Young", "alice.young"}, {"Oscar", "Ibrahim", "oscar.ibrahim"},
        {"Julia", "Kowalski", "julia.kowalski"}, {"Miles", "Bennett", "miles.bennett"},
        {"Elena", "Sato", "elena.sato"}, {"Caleb", "Shaw", "caleb.shaw"},
        {"Freya", "Lund", "freya.lund"}, {"Adam", "Haddad", "adam.haddad"},
        {"Rosa", "Ortiz", "rosa.ortiz"}, {"Jonah", "Reed", "jonah.reed"},
        {"Lucy", "Tanaka", "lucy.tanaka"}, {"Ezra", "Klein", "ezra.klein"},
        {"Maeve", "Doyle", "maeve.doyle"}, {"Ari", "Levi", "ari.levi"},
        {"Tessa", "Moss", "tessa.moss"}, {"Diego", "Ramos", "diego.ramos"},
        {"Nina", "Holm", "nina.holm"}, {"Paul", "Ferreira", "paul.ferreira"},
        {"Sara", "Lindqvist", "sara.lindqvist"}, {"Kai", "Mendes", "kai.mendes"}
    };

    /// A cheap, deterministic scramble of `serial` for choice `salt`.
    static int mix(int serial, int salt) {
        int h = serial * 0x9E3779B1 + salt * 0x85EBCA77;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        return h & 0x7fffffff;
    }

    /// Who placed order `serial`: half the orders come from twelve regulars.
    static int shopper(int serial) {
        int h = mix(serial, 9);
        return h % 2 == 0 ? h / 2 % 12 : h / 2 % SHOPPERS.length;
    }

    /// A variant index skewed toward the front, so a few products lead.
    static int popular(int seed, int size) {
        int h = mix(seed, 10);
        return h % 5 < 3 ? h / 5 % Math.max(1, size / 4) : h / 5 % size;
    }

    /// Orders placed on `epochDay`, `ago` days back: weekends busier, a gentle
    /// growth across the window, and some day-to-day noise.
    static int ordersOn(boolean big, long epochDay, int ago) {
        // 1970-01-01 was a Thursday; 0 is Monday here.
        int weekday = (int) ((epochDay + 3) % 7);
        int[] shape = {90, 85, 95, 100, 115, 140, 120};
        int base = big ? 7 : 3;
        int growth = 100 - ago * 30 / HISTORY_DAYS;
        int noise = 85 + mix((int) epochDay, 11) % 31;
        return Math.max(1, base * shape[weekday] * growth / 100 * noise / 10000);
    }

    @Transactional(readOnly = true)
    boolean exists(String handle) {
        return stores.findByHandle(handle) != null;
    }

    @Transactional(rollbackFor = IOException.class)
    void northwind(String owner, String cashier, String pinHash) throws IOException {
        Store store = store(NORTHWIND, "Northwind Outfitters", "Gear for the long way round",
                "Outdoor clothing and equipment, tested on the trails around our shop since "
                + "2009. Free repairs for life on anything we make.", 0x2f6f4f, owner,
                "12 Harbor Street, Portland, ME", 43.6591, -70.2568);
        List<Location> places = new ArrayList<Location>();
        places.add(location(store, "Warehouse", "WAREHOUSE", "400 Commerce Way, Westbrook, ME",
                true, 0));
        places.add(location(store, "Downtown store", "RETAIL", "12 Harbor Street, Portland, ME",
                true, 1));
        places.add(location(store, "Weekend pop-up", "POPUP", "Farmers market, Deering Oaks",
                false, 2));
        locations.saveAll(places);
        staff.insert(member(store, owner, StoreAccess.OWNER, "", "", "Morgan Owner"));
        staff.insert(member(store, cashier, StoreAccess.CASHIER, pinHash, places.get(1).getId(),
                "Casey Cashier"));
        List<Product> made = catalog(store, places, NORTHWIND_PRODUCTS, "NW", true);
        hero(store, "ultralight-tent-2p-2.jpg");
        collection(store, "Staff picks", "staff-picks",
                "What we reach for before a weekend in the hills.", "", 0,
                pick(made, new int[] {0, 5, 10, 14, 21, 26}));
        collection(store, "Outerwear", "outerwear", "Shells, parkas and fleece.", "type:Jackets",
                1, null);
        collection(store, "Under $50", "under-50", "Small gifts and everyday basics.",
                "price<5000", 2, null);
        collection(store, "Summer", "summer", "Light layers and sun protection.", "tag:summer",
                3, null);
        collection(store, "Winter warmth", "winter", "Merino, down and wool.",
                "tag:winter AND price>3000", 4, null);

        Supplier supplier = new Supplier();
        supplier.setId(Ids.next());
        supplier.setStoreId(store.getId());
        supplier.setName("Peakstep Footwear Co.");
        supplier.setContact("Jordan Lee");
        supplier.setEmail("orders@peakstep.example");
        supplier.setPhone("+12075550190");
        supplier.setAddress("88 Mill Road, Lewiston, ME");
        supplier.setNote("Net 30. Minimum order 12 pairs.");
        suppliers.insert(supplier);
        Supplier wool = new Supplier();
        wool.setId(Ids.next());
        wool.setStoreId(store.getId());
        wool.setName("Woolly Goat Knitwear");
        wool.setContact("Sam Rivera");
        wool.setEmail("wholesale@woollygoat.example");
        wool.setNote("Ships Tuesdays.");
        suppliers.insert(wool);

        // An order placed with the boot maker, not yet arrived: its pairs are
        // incoming at the warehouse.
        PurchaseOrder order = new PurchaseOrder();
        order.setId(Ids.next());
        order.setStoreId(store.getId());
        order.setNumber(1001);
        order.setSupplierId(supplier.getId());
        order.setLocationId(places.get(0).getId());
        order.setStatus(Stock.ORDERED);
        order.setExpectedAt(Instant.now().plusMillis(5L * 24 * 60 * 60 * 1000));
        order.setNote("Restock before the autumn season.");
        order.setCreatedAt(Instant.now().minusMillis(2L * 24 * 60 * 60 * 1000));
        order.setUpdatedAt(order.getCreatedAt());
        long total = 0;
        List<PurchaseLine> lines = new ArrayList<PurchaseLine>();
        List<Variant> boots = variants.findByProductIdOrderByPositionAsc(made.get(15).getId());
        for (int iter = 0; iter < boots.size(); iter++) {
            Variant v = boots.get(iter);
            PurchaseLine line = new PurchaseLine();
            line.setId(Ids.next());
            line.setPurchaseOrderId(order.getId());
            line.setVariantId(v.getId());
            line.setQuantity(6);
            line.setCostCents(v.getCostCents());
            total += v.getCostCents() * 6;
            lines.add(line);
            InventoryLevel level = levels.findByVariantIdAndLocationId(v.getId(),
                    places.get(0).getId());
            level.setIncoming(level.getIncoming() + 6);
            levels.save(level);
        }
        order.setTotalCents(total);
        orders.insert(order);
        orderLines.saveAll(lines);
    }

    @Transactional(rollbackFor = IOException.class)
    void littleClay(String owner) throws IOException {
        Store store = store(LITTLE_CLAY, "Little Clay Studio", "Handmade ceramics for every day",
                "Small-batch stoneware and porcelain thrown, glazed and fired in our studio. "
                + "Every piece is a little different; that is the point.", 0xb5603a, owner,
                "7 Kiln Lane, Asheville, NC", 35.5951, -82.5515);
        List<Location> places = new ArrayList<Location>();
        places.add(location(store, "Studio shop", "RETAIL", "7 Kiln Lane, Asheville, NC", true,
                0));
        locations.saveAll(places);
        staff.insert(member(store, owner, StoreAccess.OWNER, "", "", "Morgan Owner"));
        List<Product> made = catalog(store, places, LITTLE_CLAY_PRODUCTS, "LC", false);
        hero(store, "beginner-wheel-class.jpg");
        collection(store, "New arrivals", "new-arrivals", "Fresh out of the kiln this month.",
                "", 0, pick(made, new int[] {2, 8, 17, 20, 25}));
        collection(store, "Mugs & cups", "mugs", "Something to hold your coffee or tea.",
                "type:Mugs", 1, null);
        collection(store, "Gifts under $40", "gifts-under-40", "Easy to wrap, hard to forget.",
                "tag:gift AND price<4000", 2, null);
        collection(store, "Classes", "classes", "Get your hands dirty.", "type:Classes", 3,
                null);
    }

    /// The third store, a small one: only the demo shopper's walkthrough adds
    /// it ([ShopperDemo#seed]), so its order is the one that earns the
    /// Explorer badge. Not part of [#seed], so the merchant tours and the
    /// backend tests see the two stores they always did.
    public static final String FENNEL_FIG = "fennelfig";

    private static final String[][] FENNEL_FIG_PRODUCTS = {
        // Every product here has a seed photo of its own kind (named by its
        // handle in seed-media): the demo shows no drawn placeholder.
        {"Olive Oil Cruet", "Pantry", "Fennel & Fig", "glass, pantry, gift", "2400", "",
            "A half-litre glass cruet with a cork stopper and a drip-free spout."},
        {"Pour-Over Coffee Dripper", "Coffee & tea", "Fennel & Fig", "coffee, bestseller",
            "3400", "", "A cone, a glass server and a scale: a slow cup, made well."},
        {"Salt Cellar", "Pantry", "Fennel & Fig", "stoneware, gift", "1800", "",
            "A lidded stoneware cellar with a little wooden spoon."},
        {"Linen Tea Towel", "Linens", "Fennel & Fig", "linen, gift", "1400", "C",
            "Stonewashed linen that dries a glass without a streak."},
        {"Cast Iron Skillet", "Cookware", "Fennel & Fig", "iron, stovetop, oven", "4800", "",
            "A 26 cm skillet, seasoned and ready for its first egg."},
        {"Teapot", "Coffee & tea", "Fennel & Fig", "porcelain, tea", "3900", "",
            "A white porcelain pot for four cups, with a lid that stays put as you pour."},
        {"Butter Dish", "Tableware", "Fennel & Fig", "stoneware, gift", "2600", "",
            "Speckled stoneware with a domed lid; holds a standard block."},
        {"Travel Tumbler", "Drinkware", "Fennel & Fig", "steel, insulated", "2900", "",
            "Double-walled steel that keeps coffee hot through the morning commute."},
    };

    /// The handles of seeded products, in every store, with no seed photo:
    /// each would show the drawn placeholder. The demo wants none.
    public static List<String> productsWithoutPhotos() {
        List<String> missing = new ArrayList<String>();
        String[][][] all = {NORTHWIND_PRODUCTS, LITTLE_CLAY_PRODUCTS, FENNEL_FIG_PRODUCTS};
        for (int s = 0; s < all.length; s++) {
            for (int iter = 0; iter < all[s].length; iter++) {
                String handle = Catalog.slug(all[s][iter][0]);
                if (DemoStores.class.getResource("/seed-media/" + handle + ".jpg") == null) {
                    missing.add(handle);
                }
            }
        }
        return missing;
    }

    /// Seeds [#FENNEL_FIG] once, owned by `owner`.
    private final com.codenameone.examples.bazaar.repository.PaymentRepository payments;

    public void fennelFig(String owner) throws IOException {
        if (exists(FENNEL_FIG)) {
            return;
        }
        Store store = store(FENNEL_FIG, "Fennel & Fig", "Kitchen goods that last",
                "Cookware, storage and linens chosen to be used every day and handed down. "
                + "Everything ships from our shop in Portland.", 0x3f7d4e, owner,
                "18 Alder St, Portland, OR", 45.5202, -122.6742);
        List<Location> places = new ArrayList<Location>();
        places.add(location(store, "Alder St shop", "RETAIL", "18 Alder St, Portland, OR", true,
                0));
        locations.saveAll(places);
        staff.insert(member(store, owner, StoreAccess.OWNER, "", "", "Morgan Owner"));
        catalog(store, places, FENNEL_FIG_PRODUCTS, "FF", false);
        hero(store, "butter-dish.jpg");
        // Shipping and tax, or nothing at checkout can be bought.
        pricing.seed(store.getId());
        // A loyalty programme, as the other two stores have: the walkthrough's
        // order here shows the points it will earn.
        marketing.programme(store.getId());
        // Its own few months of sales, as the other stores have: without them
        // the admin's store list showed a live shop with no orders at all.
        if (sales.findFirstByStoreIdOrderByNumberDesc(store.getId()) == null) {
            history(store, new ArrayList<Register>(), owner, owner);
        }
    }

    private Store store(String handle, String name, String tagline, String description,
            int accent, String owner, String address, double lat, double lng) throws IOException {
        Store store = new Store();
        store.setId(Ids.next());
        store.setHandle(handle);
        store.setName(name);
        store.setTagline(tagline);
        store.setDescription(description);
        store.setCurrency("USD");
        store.setAccentColor(accent);
        store.setOwnerUsername(owner);
        store.setStatus(Stores.ACTIVE);
        store.setEmail("hello@" + handle + ".example");
        store.setPhone("+15550123");
        store.setAddress(address);
        store.setLat(lat);
        store.setLng(lng);
        store.setPolicies("Returns: unused items within 30 days for a full refund.\n"
                + "Shipping: orders ship within two business days.\n"
                + "Privacy: we never sell your details.");
        store.setCreatedAt(Instant.now().minusMillis(90L * 24 * 60 * 60 * 1000));
        store.setUpdatedAt(Instant.now());
        stores.insert(store);
        return store;
    }

    private static Location location(Store store, String name, String kind, String address,
            boolean online, int position) throws IOException {
        Location location = new Location();
        location.setId(Ids.next());
        location.setStoreId(store.getId());
        location.setName(name);
        location.setKind(kind);
        location.setAddress(address);
        location.setFulfilsOnline(online);
        location.setActive(true);
        location.setPosition(position);
        return location;
    }

    private static StoreStaff member(Store store, String username, String role, String pinHash,
            String locationId, String name) throws IOException {
        StoreStaff member = new StoreStaff();
        member.setId(Ids.next());
        member.setStoreId(store.getId());
        member.setUsername(username);
        member.setRole(role);
        member.setPinHash(pinHash);
        member.setLocationId(locationId);
        member.setDisplayName(name);
        member.setCreatedAt(Instant.now());
        return member;
    }

    /// The products, options, variants, levels and opening moves of a store,
    /// written in a few batches.
    private List<Product> catalog(Store store, List<Location> places, String[][] rows,
            String skuPrefix, boolean apparel) throws IOException {
        List<Product> made = new ArrayList<Product>();
        List<ProductOption> optionRows = new ArrayList<ProductOption>();
        List<Variant> variantRows = new ArrayList<Variant>();
        List<InventoryLevel> levelRows = new ArrayList<InventoryLevel>();
        List<InventoryMove> moveRows = new ArrayList<InventoryMove>();
        long now = System.currentTimeMillis();
        int serial = 0;
        for (int iter = 0; iter < rows.length; iter++) {
            String[] row = rows[iter];
            Product product = new Product();
            product.setId(Ids.next());
            product.setStoreId(store.getId());
            product.setTitle(row[0]);
            product.setHandle(Catalog.slug(row[0]));
            product.setProductType(row[1]);
            product.setVendor(row[2]);
            product.setTags(row[3]);
            product.setDescription(row[6] + "\n\n" + (apparel
                    ? "Designed in Maine and backed by our lifetime repair promise."
                    : "Dishwasher and microwave safe. Each piece is made by hand, so sizes "
                    + "and glazes vary a little."));
            // A couple of each store's products are not for sale yet, or not
            // any more, so the storefront has something to hide.
            product.setStatus(iter == rows.length - 2 ? Catalog.DRAFT
                    : iter == rows.length - 3 ? Catalog.ARCHIVED : Catalog.ACTIVE);
            product.setSeoTitle(row[0] + " | " + store.getName());
            product.setSeoDescription(row[6]);
            product.setRatingCount(iter % 4 == 3 ? 0 : 3 + iter % 9);
            product.setRatingTotal(product.getRatingCount() * (38 + iter * 7 % 12) / 10);
            product.setCreatedAt(Instant.ofEpochMilli(now - (rows.length - iter) * 86400000L));
            product.setUpdatedAt(product.getCreatedAt());
            made.add(product);

            String kind = row[5];
            boolean shoes = "Footwear".equals(row[1]) || row[0].indexOf("Socks") >= 0;
            String[] first = new String[] {""};
            String[] second = new String[] {""};
            String firstName = null;
            String secondName = null;
            if (kind.indexOf('S') >= 0) {
                first = shoes ? SHOE_SIZES : SIZES;
                firstName = "Size";
            }
            if (kind.indexOf('C') >= 0) {
                String[] colors = apparel ? APPAREL_COLORS : CLAY_COLORS;
                if (firstName == null) {
                    first = colors;
                    firstName = "Color";
                } else {
                    second = new String[] {colors[0], colors[1]};
                    secondName = "Color";
                }
            }
            if (firstName != null) {
                optionRows.add(option(product, firstName, first, 0));
            }
            if (secondName != null) {
                optionRows.add(option(product, secondName, second, 1));
            }
            long price = Long.parseLong(row[4]);
            int position = 0;
            for (int a = 0; a < first.length; a++) {
                for (int b = 0; b < second.length; b++) {
                    serial++;
                    Variant v = new Variant();
                    v.setId(Ids.next());
                    v.setProductId(product.getId());
                    v.setStoreId(store.getId());
                    v.setOption1(first[a]);
                    v.setOption2(second[b]);
                    v.setTitle(first[a].length() == 0 ? "Default"
                            : second[b].length() == 0 ? first[a] : first[a] + " / " + second[b]);
                    v.setSku(skuPrefix + "-" + pad(iter + 1, 3)
                            + (first[a].length() == 0 ? "" : "-" + code(first[a]))
                            + (second[b].length() == 0 ? "" : "-" + code(second[b])));
                    v.setBarcode(ean13((apparel ? "20012" : "20034") + pad(serial, 7)));
                    // XL costs a little more, as it does in most shops.
                    v.setPriceCents("XL".equals(first[a]) ? price + 500 : price);
                    v.setCompareAtCents(iter % 7 == 2 ? price + price / 4 : 0);
                    v.setCostCents(price * 42 / 100);
                    v.setWeightGrams(apparel ? 250 + iter * 37 % 900 : 300 + iter * 53 % 1200);
                    v.setTaxable(!"Classes".equals(row[1]));
                    // Classes and the gift envelope are not stock.
                    v.setTrackInventory(!"Classes".equals(row[1])
                            && row[0].indexOf("Gift Card") < 0);
                    v.setAllowBackorder(row[0].indexOf("Tent") >= 0);
                    v.setPosition(position++);
                    variantRows.add(v);
                    for (int p = 0; p < places.size(); p++) {
                        // The tent keeps selling with nothing on the shelf, so the
                        // shop shows a backorder.
                        int onHand = v.isTrackInventory() && !v.isAllowBackorder()
                                ? stock(serial, p, iter) : 0;
                        InventoryLevel level = Inventory.newLevel(store.getId(), v.getId(),
                                places.get(p).getId(), onHand);
                        // The pop-up and the backordered tent never alert.
                        level.setReorderPoint(p == 2 || v.isAllowBackorder() ? 0 : 4);
                        levelRows.add(level);
                        if (onHand != 0) {
                            moveRows.add(Inventory.newMove(store.getId(), v.getId(),
                                    places.get(p).getId(), onHand, 0, Inventory.RESTOCK,
                                    "opening stock", store.getOwnerUsername()));
                        }
                    }
                }
            }
        }
        pictures(store, made);
        List<Review> reviewRows = reviews(made, now);
        products.saveAll(made);
        reviews.saveAll(reviewRows);
        options.saveAll(optionRows);
        variants.saveAll(variantRows);
        levels.saveAll(levelRows);
        moves.saveAll(moveRows);
        return made;
    }

    /// The published reviews behind each product's seeded rating: as many as
    /// its rating count, their stars spread so they add up to its rating
    /// total (which is then set to exactly that sum), so the average on a
    /// card and the reviews behind it agree.
    private static List<Review> reviews(List<Product> made, long now) throws IOException {
        List<Review> out = new ArrayList<Review>();
        for (int p = 0; p < made.size(); p++) {
            Product product = made.get(p);
            int count = (int) product.getRatingCount();
            if (count <= 0) {
                continue;
            }
            long total = Math.max(count, Math.min(5L * count, product.getRatingTotal()));
            int base = (int) (total / count);
            int extra = (int) (total % count);
            long sum = 0;
            for (int r = 0; r < count; r++) {
                int stars = Math.min(5, base + (r < extra ? 1 : 0));
                sum += stars;
                String[] text = REVIEW_TEXT[(p + r) % REVIEW_TEXT.length];
                Review review = new Review();
                review.setId(Ids.next());
                review.setStoreId(product.getStoreId());
                review.setProductId(product.getId());
                review.setUsername("demo-reviewer-" + r + "@bazaar.example");
                review.setAuthorName(REVIEWERS[(p * 3 + r) % REVIEWERS.length]);
                review.setRating(stars);
                review.setTitle(text[0]);
                review.setBody(text[1]);
                review.setStatus("PUBLISHED");
                review.setVerified(r % 3 != 2);
                review.setCreatedAt(Instant.ofEpochMilli(now - (r * 5L + p % 7 + 1) * 86400000L));
                out.add(review);
            }
            product.setRatingTotal(sum);
        }
        return out;
    }

    /// Stores each product's seed photos and makes the first its main one.
    /// A product with no file keeps the drawn placeholder.
    private void pictures(Store store, List<Product> made) throws IOException {
        for (int iter = 0; iter < made.size(); iter++) {
            Product product = made.get(iter);
            for (int n = 1; n <= 3; n++) {
                String id = seedPhoto(store,
                        product.getHandle() + (n == 1 ? "" : "-" + n) + ".jpg");
                if (id == null) {
                    break;
                }
                ProductPhoto row = new ProductPhoto();
                row.setId(Ids.next());
                row.setProductId(product.getId());
                row.setPhotoId(id);
                row.setAlt(product.getTitle());
                row.setPosition(n - 1);
                gallery.insert(row);
                if (n == 1) {
                    product.setPhotoId(id);
                }
            }
        }
    }

    /// The store's hero picture, from a seed file.
    private void hero(Store store, String file) throws IOException {
        String id = seedPhoto(store, file);
        if (id != null) {
            store.setHeroPhotoId(id);
            stores.save(store);
        }
    }

    /// Stores seed file `name` as a photo of `store` and answers its id, or
    /// null when there is no such file.
    private String seedPhoto(Store store, String name) throws IOException {
        InputStream in = DemoStores.class.getResourceAsStream("/seed-media/" + name);
        if (in == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            byte[] chunk = new byte[16384];
            int read = in.read(chunk);
            while (read >= 0) {
                out.write(chunk, 0, read);
                read = in.read(chunk);
            }
        } finally {
            in.close();
        }
        Photo row = new Photo();
        row.setId(Ids.next());
        row.setOwnerId(store.getId());
        row.setContentType("image/jpeg");
        row.setData(out.toByteArray());
        row.setCreatedAt(Instant.now());
        photos.insert(row);
        return row.getId();
    }

    /// Opening stock: plenty at most places, a few low, a few gone.
    private static int stock(int serial, int place, int product) {
        int roll = (serial * 31 + place * 17 + product * 7) % 89;
        if (roll == 0) {
            return 0;
        }
        if (roll < 3 && place < 2) {
            return roll + 1;
        }
        if (place == 2) {
            return 2 + roll % 6;
        }
        return place == 0 ? 18 + roll % 40 : 8 + roll % 16;
    }

    private static ProductOption option(Product product, String name, String[] values,
            int position) throws IOException {
        ProductOption option = new ProductOption();
        option.setId(Ids.next());
        option.setProductId(product.getId());
        option.setName(name);
        StringBuilder joined = new StringBuilder();
        for (int iter = 0; iter < values.length; iter++) {
            if (iter > 0) {
                joined.append(", ");
            }
            joined.append(values[iter]);
        }
        option.setOptionValues(joined.toString());
        option.setPosition(position);
        return option;
    }

    private void collection(Store store, String title, String handle, String description,
            String rules, int position, List<Product> manual) throws IOException {
        Collection collection = new Collection();
        collection.setId(Ids.next());
        collection.setStoreId(store.getId());
        collection.setHandle(handle);
        collection.setTitle(title);
        collection.setDescription(description);
        collection.setRuleText(rules);
        collection.setPosition(position);
        collection.setPublished(true);
        collections.insert(collection);
        if (manual != null) {
            List<CollectionProduct> rows = new ArrayList<CollectionProduct>();
            for (int iter = 0; iter < manual.size(); iter++) {
                CollectionProduct row = new CollectionProduct();
                row.setId(Ids.next());
                row.setCollectionId(collection.getId());
                row.setProductId(manual.get(iter).getId());
                row.setPosition(iter);
                rows.add(row);
            }
            members.saveAll(rows);
        }
    }

    private static List<Product> pick(List<Product> made, int[] indexes) {
        List<Product> out = new ArrayList<Product>();
        for (int iter = 0; iter < indexes.length; iter++) {
            out.add(made.get(indexes[iter]));
        }
        return out;
    }

    /// The first letters of an option value, upper-cased ASCII, for a SKU.
    private static String code(String value) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < value.length() && out.length() < 3; iter++) {
            char c = value.charAt(iter);
            if (c >= 'a' && c <= 'z') {
                c = (char) (c - ('a' - 'A'));
            }
            if (c >= 'A' && c <= 'Z' || c >= '0' && c <= '9') {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String pad(int n, int width) {
        StringBuilder out = new StringBuilder(Integer.toString(n));
        while (out.length() < width) {
            out.insert(0, '0');
        }
        return out.toString();
    }

    /// Twelve digits and their EAN-13 check digit. The 200-299 prefixes are
    /// reserved for a store's own numbering, so these collide with no real
    /// product.
    public static String ean13(String twelve) {
        int sum = 0;
        for (int iter = 0; iter < 12; iter++) {
            int digit = twelve.charAt(iter) - '0';
            sum += iter % 2 == 0 ? digit : digit * 3;
        }
        return twelve + (char) ('0' + (10 - sum % 10) % 10);
    }
}
