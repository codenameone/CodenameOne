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
package com.codenameone.examples.savor.catalog;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.savor.Ids;
import com.codenameone.examples.savor.account.Accounts;
import com.codenameone.examples.savor.domain.Dish;
import com.codenameone.examples.savor.domain.DishOption;
import com.codenameone.examples.savor.domain.MenuSection;
import com.codenameone.examples.savor.domain.Photo;
import com.codenameone.examples.savor.domain.Promo;
import com.codenameone.examples.savor.domain.Restaurant;
import com.codenameone.examples.savor.domain.RestaurantStaff;
import com.codenameone.examples.savor.repository.DishOptionRepository;
import com.codenameone.examples.savor.repository.DishRepository;
import com.codenameone.examples.savor.repository.MenuSectionRepository;
import com.codenameone.examples.savor.repository.PhotoRepository;
import com.codenameone.examples.savor.repository.PromoRepository;
import com.codenameone.examples.savor.repository.RestaurantRepository;
import com.codenameone.examples.savor.repository.RestaurantStaffRepository;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

/// The restaurants a development server starts with: a dozen around San
/// Francisco's waterfront, one per cuisine on the home screen's row, each with
/// a menu, plus one application waiting for an admin and two promotion codes.
///
/// The first restaurant's kitchen is run by the demo merchant; the rest have
/// nobody, and `DemoKitchen` accepts and cooks their orders so a customer can
/// order anywhere and watch it arrive.
@Component
public class DemoRestaurants {
    /// The cuisines of the home screen's row, in its order. The client keeps
    /// the same list for its icons.
    public static final String[] CUISINES = {"Pizza", "Burgers", "Sushi", "Chicken", "Kebab",
        "Rice", "Steak", "Tacos", "Salads", "Donuts", "Dessert", "Coffee"};

    /// Each restaurant: id, name, cuisine, description, address, lat, lng,
    /// prep minutes, price level, rating total, rating count. Its menu is
    /// [#MENUS] at the same index.
    static final Object[][] RESTAURANTS = {
        {"r-ember", "Ember & Crust", "Pizza",
            "Wood-fired Neapolitan pies, fresh pasta and natural wines.",
            "Pier 39", 37.8085, -122.4100, 18, 2, 470L, 100L},
        {"r-harbor", "Harbor Burger Co.", "Burgers", "Dry-aged smash burgers and hand-cut fries.",
            "1 Ferry Building", 37.7952, -122.3934, 12, 2, 452L, 100L},
        {"r-kumo", "Kumo Omakase", "Sushi", "Seasonal nigiri, hand rolls and donburi.",
            "550 Washington St", 37.7959, -122.4011, 20, 4, 489L, 100L},
        {"r-gilded", "The Gilded Hen", "Chicken", "Buttermilk fried chicken and biscuits.",
            "900 North Point St", 37.8059, -122.4229, 15, 2, 461L, 100L},
        {"r-saffron", "Saffron Grill", "Kebab", "Charcoal kebabs, mezze and warm pita.",
            "2 Embarcadero Center", 37.7949, -122.3991, 14, 2, 455L, 100L},
        {"r-jade", "Jade Bowl", "Rice", "Claypot rice, congee and wok-fired greens.",
            "750 Kearny St", 37.7952, -122.4048, 16, 1, 444L, 100L},
        {"r-prime", "Prime & Pepper", "Steak", "Steakhouse cuts, sides and sauces to share.",
            "Pier 15", 37.8017, -122.3973, 25, 4, 482L, 100L},
        {"r-luz", "Luz Taqueria", "Tacos", "Handmade tortillas, al pastor and fresh salsas.",
            "Fort Mason Center", 37.8066, -122.4310, 10, 1, 466L, 100L},
        {"r-verde", "Verde Table", "Salads", "Grain bowls and big salads, all made to order.",
            "Union Square", 37.7880, -122.4075, 8, 2, 438L, 100L},
        {"r-gloss", "Glaze Donut Atelier", "Donuts", "Brioche donuts, glazed every hour.",
            "Lombard St", 37.8021, -122.4187, 6, 1, 471L, 100L},
        {"r-velvet", "Velvet Spoon", "Dessert", "Gelato, tarts and chocolate made in-house.",
            "Ghirardelli Square", 37.8057, -122.4225, 6, 2, 478L, 100L},
        {"r-ritual", "Morning Ritual", "Coffee", "Single-origin coffee, pancakes and fresh juice.",
            "Oracle Park", 37.7786, -122.3893, 5, 1, 459L, 100L}};

    /// Each menu: rows in order, where a row of one element names a section and
    /// the dishes after it, each {name, description, cents, popular, then its
    /// options as name, cents, name, cents...}, are in that section. Three dimensions, not
    /// four: the compiled server has no array classes deeper than that.
    static final Object[][][] MENUS = {
        {{"Pizza"},
            {"Margherita", "San Marzano tomato, fior di latte, basil.", 1600, true,
                "Extra mozzarella", 250, "Fresh chili", 100, "Gluten-free crust", 300},
            {"Diavola", "Spicy salami, chili honey, oregano.", 1900, true,
                "Extra salami", 350, "Burrata", 450},
            {"Funghi Bianca", "Wild mushrooms, ricotta, thyme, no tomato.", 1850, false},
         {"Starters"},
            {"Burrata", "Heirloom tomatoes, basil oil, sourdough.", 1400, false},
            {"Garlic Knots", "With whipped ricotta.", 800, false},
         {"Pasta"},
            {"Spaghetti Carbonara", "Guanciale, pecorino, egg yolk, black pepper.", 1750, true}},
        {{"Burgers"},
            {"Double Smash", "Two dry-aged patties, American cheese, house sauce.", 1500, true,
                "Bacon", 250, "Extra patty", 400, "Lettuce wrap", 0},
            {"Mushroom Melt", "Swiss, caramelised onion, truffle mayo.", 1600, false},
            {"Crispy Chicken", "Pickles, slaw, hot honey.", 1450, false},
         {"Sides"},
            {"Hand-cut Fries", "Sea salt and rosemary.", 600, true, "Truffle", 200, "Cheese", 150},
            {"Onion Rings", "Beer battered.", 700, false},
            {"Shake", "Vanilla, chocolate or strawberry.", 750, false}},
        {{"Nigiri sets"},
            {"Omakase Twelve", "The chef's twelve pieces of the day.", 5800, true},
            {"Salmon Lover", "Eight salmon nigiri, three ways.", 3200, false},
         {"Rolls and bowls"},
            {"Toro Hand Roll", "Fatty tuna, scallion, pickled radish.", 1400, true},
            {"Chirashi Bowl", "Chef's selection over seasoned rice.", 3600, false,
                "Extra uni", 1200, "Ikura", 600},
            {"Miso Soup", "White miso, tofu, wakame.", 450, false}},
        {{"Fried chicken"},
            {"Three-piece Meal", "Thigh, drumstick and wing, with a biscuit.", 1550, true,
                "Make it spicy", 0, "Extra biscuit", 300},
            {"Chicken Sandwich", "Buttermilk thigh, slaw, pickles.", 1350, true},
            {"Tenders", "Five tenders and two sauces.", 1250, false},
         {"Sides"},
            {"Mac and Cheese", "Three cheeses, breadcrumb crust.", 650, false},
            {"Collard Greens", "Slow cooked.", 550, false}},
        {{"Kebabs"},
            {"Lamb Adana", "Hand-minced lamb, sumac onions, grilled pepper.", 1900, true,
                "Extra pita", 150, "Garlic sauce", 100},
            {"Chicken Shish", "Yoghurt-marinated thigh, saffron rice.", 1700, true},
            {"Falafel Plate", "Tahini, pickles, salad.", 1450, false},
         {"Mezze"},
            {"Hummus", "With warm pita.", 750, false},
            {"Baba Ghanoush", "Smoked aubergine, pomegranate.", 800, false},
            {"Mezze Platter", "Hummus, baba ghanoush, falafel and warm pita to share.", 1800,
                true}},
        {{"Rice"},
            {"Claypot Chicken Rice", "Lap cheong, mushroom, soy.", 1600, true, "Fried egg", 200},
            {"Pork Belly Bowl", "Five-spice belly, pickled greens.", 1700, false},
            {"Seafood Congee", "Prawn, scallop, ginger.", 1500, false},
         {"Wok"},
            {"Garlic Greens", "Gai lan, garlic, oyster sauce.", 900, false},
            {"Salt and Pepper Tofu", "Chili, scallion.", 1100, true}},
        {{"Steaks"},
            {"Ribeye 14oz", "Dry-aged, with peppercorn sauce.", 5400, true,
                "Bearnaise", 300, "Truffle butter", 500},
            {"Filet 8oz", "With red wine jus.", 4800, false},
            {"Steak Frites", "Hanger steak, fries, chimichurri.", 3200, true},
         {"Sides"},
            {"Creamed Spinach", "Nutmeg and parmesan.", 900, false},
            {"Potato Gratin", "Gruyere and thyme.", 1000, false}},
        {{"Tacos"},
            {"Al Pastor", "Spit-roasted pork, pineapple, salsa verde. Three tacos.", 1200, true,
                "Extra pineapple", 100, "Guacamole", 250},
            {"Baja Fish", "Beer-battered cod, cabbage, chipotle crema.", 1350, true},
            {"Mushroom Asada", "Grilled oyster mushrooms, salsa macha.", 1150, false},
         {"Extras"},
            {"Chips and Guacamole", "Made to order.", 800, false},
            {"Elote", "Grilled corn, cotija, lime.", 600, false}},
        {{"Bowls"},
            {"Harvest Bowl", "Wild rice, roast squash, kale, goat cheese.", 1500, true,
                "Chicken", 400, "Salmon", 600, "Avocado", 250},
            {"Green Goddess", "Greens, cucumber, herbs, chickpeas.", 1350, false},
            {"Spicy Tuna Poke", "Seasoned rice, edamame, nori.", 1700, true},
         {"Drinks"},
            {"Cold-pressed Juice", "Green or citrus.", 750, false}},
        {{"Donuts"},
            {"Classic Glazed", "Brioche, vanilla glaze.", 400, true, "Make it a dozen", 3600},
            {"Creme Brulee", "Custard filled, torched sugar.", 550, true},
            {"Maple Bacon", "Maple glaze, candied bacon.", 550, false},
         {"Coffee"},
            {"Drip Coffee", "House blend.", 350, false}},
        {{"Gelato"},
            {"Pint of Gelato", "Pistachio, chocolate, or seasonal.", 1200, true},
            {"Affogato", "Vanilla gelato, espresso.", 700, false},
         {"Pastry"},
            {"Lemon Tart", "Italian meringue.", 800, true},
            {"Chocolate Torte", "Flourless, 70% cacao.", 850, false}},
        {{"Coffee"},
            {"Flat White", "Double ristretto.", 500, true, "Oat milk", 75, "Extra shot", 100},
            {"Cold Brew", "Steeped eighteen hours.", 550, false},
            {"Fresh Orange Juice", "Squeezed to order.", 600, false},
         {"Bakery"},
            {"Almond Croissant", "Baked this morning.", 550, true},
            {"Buttermilk Pancakes", "Raspberries and maple syrup.", 1150, true},
            {"Breakfast Sandwich", "Egg, cheddar, chive, brioche.", 950, false, "Bacon", 200}}};

    /// The restaurants that have a photograph, as {restaurant id, photo key}. A
    /// key is a file in the photos directory, `savor.photos.dir` (`photos`
    /// beside the server), as `<key>.jpg` or `<key>.png`. Replace the files to
    /// change the pictures; a missing file leaves the cover the app draws.
    private static final String[][] RESTAURANT_PHOTOS = {
        {"r-ember", "ember"}, {"r-harbor", "harbor"}, {"r-kumo", "kumo"},
        {"r-gilded", "gilded"}, {"r-saffron", "saffron"}, {"r-jade", "jade"},
        {"r-prime", "prime"}, {"r-luz", "luz"}, {"r-verde", "verde"}, {"r-gloss", "glaze"},
        {"r-velvet", "velvet"}, {"r-ritual", "ritual"}};

    /// The dishes that have one, as {dish name, photo key}.
    private static final String[][] DISH_PHOTOS = {
        {"Margherita", "ember"}, {"Diavola", "diavola"}, {"Funghi Bianca", "mushrooms"},
        {"Burrata", "burrata"}, {"Spaghetti Carbonara", "carbonara"},
        {"Double Smash", "harbor"}, {"Mushroom Melt", "mushrooms"}, {"Onion Rings", "onion"},
        {"Hand-cut Fries", "fries"}, {"Omakase Twelve", "kumo"}, {"Toro Hand Roll", "toro-roll"},
        {"Chirashi Bowl", "chirashi"}, {"Three-piece Meal", "gilded"},
        {"Chicken Sandwich", "chicken-sandwich"}, {"Lamb Adana", "saffron"},
        {"Mezze Platter", "mezze"}, {"Ribeye 14oz", "prime"}, {"Steak Frites", "steak-frites"},
        {"Al Pastor", "luz"}, {"Elote", "elote"}, {"Buttermilk Pancakes", "pancakes"},
        {"Flat White", "ritual"}, {"Cold Brew", "cold-brew"}, {"Fresh Orange Juice", "juice"}};

    private final RestaurantRepository restaurants;
    private final RestaurantStaffRepository staff;
    private final MenuSectionRepository sections;
    private final DishRepository dishes;
    private final DishOptionRepository options;
    private final PromoRepository promos;
    private final Accounts accounts;
    private final PhotoRepository photos;
    private final String photosDir;

    public DemoRestaurants(RestaurantRepository restaurants, RestaurantStaffRepository staff,
            MenuSectionRepository sections, DishRepository dishes, DishOptionRepository options,
            PromoRepository promos, Accounts accounts, PhotoRepository photos,
            @Value("${savor.photos.dir:photos}") String photosDir) {
        this.restaurants = restaurants;
        this.staff = staff;
        this.sections = sections;
        this.dishes = dishes;
        this.options = options;
        this.promos = promos;
        this.accounts = accounts;
        this.photos = photos;
        this.photosDir = photosDir;
    }

    /// Writes them all, once: a database that already has the first one is
    /// left as it is.
    @Transactional(rollbackFor = IOException.class)
    public void seed(String merchant) throws IOException {
        if (restaurants.findById(String.valueOf(RESTAURANTS[0][0])) != null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (int iter = 0; iter < RESTAURANTS.length; iter++) {
            Object[] row = RESTAURANTS[iter];
            Restaurant restaurant = new Restaurant();
            restaurant.setId(String.valueOf(row[0]));
            restaurant.setName(String.valueOf(row[1]));
            restaurant.setCuisine(String.valueOf(row[2]));
            restaurant.setDescription(String.valueOf(row[3]));
            restaurant.setAddress(String.valueOf(row[4]) + ", San Francisco");
            restaurant.setLat(number(row[5]));
            restaurant.setLng(number(row[6]));
            restaurant.setStatus(Catalog.APPROVED);
            // Open around the clock, so the demo works whenever it is run.
            restaurant.setOpensAt(0);
            restaurant.setClosesAt(0);
            restaurant.setPrepMinutes(whole(row[7]));
            restaurant.setPriceLevel(whole(row[8]));
            restaurant.setMinimumCents(0L);
            restaurant.setRatingTotal(count(row[9]));
            restaurant.setRatingCount(count(row[10]));
            restaurant.setPhotoId(photoFor(RESTAURANT_PHOTOS, restaurant.getId(), restaurant.getId()));
            restaurant.setApplicant(iter == 0 ? merchant : "");
            restaurant.setReason("");
            restaurant.setCreatedAt(Instant.ofEpochMilli(now - (60 - iter) * 86400000L));
            restaurant.setUpdatedAt(Instant.ofEpochMilli(now));
            restaurants.insert(restaurant);
            menu(restaurant.getId(), MENUS[iter]);
        }
        RestaurantStaff kitchen = new RestaurantStaff();
        kitchen.setRestaurantId(String.valueOf(RESTAURANTS[0][0]));
        kitchen.setUsername(merchant);
        staff.insert(kitchen);
        accounts.setMerchant(merchant, true);

        Restaurant waiting = new Restaurant();
        waiting.setId("r-noodle");
        waiting.setName("Noodle Lab");
        waiting.setCuisine("Rice");
        waiting.setDescription("Hand-pulled noodles and dumplings.");
        waiting.setAddress("Transamerica Pyramid, San Francisco");
        waiting.setLat(37.7952);
        waiting.setLng(-122.4028);
        waiting.setStatus(Catalog.PENDING);
        waiting.setPrepMinutes(15);
        waiting.setPriceLevel(1);
        waiting.setPhotoId("");
        waiting.setApplicant("");
        waiting.setReason("");
        waiting.setCreatedAt(Instant.ofEpochMilli(now - 2L * 86400000L));
        waiting.setUpdatedAt(Instant.ofEpochMilli(now - 2L * 86400000L));
        restaurants.insert(waiting);

        promo("WELCOME10", 10, 0L, 1500L, now);
        promo("SAVOR5", 0, 500L, 2500L, now);
    }

    private void menu(String restaurantId, Object[][] menu) throws IOException {
        int dishAt = 0;
        int section = -1;
        String sectionId = null;
        for (int iter = 0; iter < menu.length; iter++) {
            if (menu[iter].length == 1) {
                MenuSection row = new MenuSection();
                row.setId(Ids.next());
                row.setRestaurantId(restaurantId);
                row.setName(String.valueOf(menu[iter][0]));
                row.setPosition(++section);
                sections.insert(row);
                sectionId = row.getId();
                continue;
            }
            Object[] food = menu[iter];
            Dish dish = new Dish();
            dish.setId(Ids.next());
            dish.setRestaurantId(restaurantId);
            dish.setSectionId(sectionId);
            dish.setName(String.valueOf(food[0]));
            dish.setDescription(String.valueOf(food[1]));
            dish.setPriceCents(whole(food[2]));
            dish.setPopular(Boolean.TRUE.equals(food[3]));
            dish.setAvailable(true);
            dish.setPhotoId(photoFor(DISH_PHOTOS, dish.getName(), restaurantId));
            dish.setPosition(dishAt++);
            dishes.insert(dish);
            for (int at = 4; at + 1 < food.length; at += 2) {
                DishOption extra = new DishOption();
                extra.setId(Ids.next());
                extra.setDishId(dish.getId());
                extra.setName(String.valueOf(food[at]));
                extra.setPriceCents(count(food[at + 1]));
                extra.setPosition((at - 4) / 2);
                options.insert(extra);
            }
        }
    }

    /// The photo `pairs` gives `name`, stored for `restaurantId` -- a photo is
    /// the restaurant's, so its merchant may keep it on a dish -- or "" when
    /// there is none or its file is missing.
    private String photoFor(String[][] pairs, String name, String restaurantId)
            throws IOException {
        for (int iter = 0; iter < pairs.length; iter++) {
            if (pairs[iter][0].equals(name)) {
                return photo(pairs[iter][1], restaurantId);
            }
        }
        return "";
    }

    private String photo(String key, String restaurantId) throws IOException {
        String id = restaurantId + "-" + key;
        if (photos.findById(id) != null) {
            return id;
        }
        String type = "image/jpeg";
        File file = new File(photosDir, key + ".jpg");
        if (!file.exists()) {
            type = "image/png";
            file = new File(photosDir, key + ".png");
        }
        if (!file.exists()) {
            return "";
        }
        Photo row = new Photo();
        row.setId(id);
        row.setRestaurantId(restaurantId);
        row.setContentType(type);
        row.setData(read(file));
        row.setCreatedAt(Instant.now());
        photos.insert(row);
        return id;
    }

    private static byte[] read(File file) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[16384];
            int count;
            while ((count = in.read(buffer)) > 0) {
                out.write(buffer, 0, count);
            }
        }
        return out.toByteArray();
    }

    private void promo(String code, int percent, long amount, long minimum, long now) {
        Promo promo = new Promo();
        promo.setCode(code);
        promo.setPercentOff(percent);
        promo.setAmountOffCents(amount);
        promo.setMinimumCents(minimum);
        promo.setActive(true);
        promo.setCreatedAt(Instant.ofEpochMilli(now));
        promos.insert(promo);
    }

    private static double number(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : 0d;
    }

    private static int whole(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    private static long count(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }
}
