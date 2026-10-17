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
import com.codenameone.examples.savor.Ids;
import com.codenameone.examples.savor.account.Accounts;
import com.codenameone.examples.savor.domain.Dish;
import com.codenameone.examples.savor.domain.DishOption;
import com.codenameone.examples.savor.domain.MenuSection;
import com.codenameone.examples.savor.domain.Promo;
import com.codenameone.examples.savor.domain.Restaurant;
import com.codenameone.examples.savor.domain.RestaurantStaff;
import com.codenameone.examples.savor.repository.DishOptionRepository;
import com.codenameone.examples.savor.repository.DishRepository;
import com.codenameone.examples.savor.repository.MenuSectionRepository;
import com.codenameone.examples.savor.repository.PromoRepository;
import com.codenameone.examples.savor.repository.RestaurantRepository;
import com.codenameone.examples.savor.repository.RestaurantStaffRepository;
import java.io.IOException;

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
        {"r-ember", "Ember & Crust", "Pizza", "Wood-fired Neapolitan pies and natural wines.",
            "1 Ferry Building", 37.7952, -122.3934, 18, 2, 470L, 100L},
        {"r-harbor", "Harbor Burger Co.", "Burgers", "Dry-aged smash burgers and hand-cut fries.",
            "Pier 39", 37.8085, -122.4100, 12, 2, 452L, 100L},
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
        {"r-ritual", "Morning Ritual", "Coffee", "Single-origin coffee and fresh pastries.",
            "Oracle Park", 37.7786, -122.3893, 5, 1, 459L, 100L}};

    /// Each menu: sections of {name, dishes...}, each dish {name, description,
    /// cents, popular, options as "name:cents|name:cents" or ""}.
    static final Object[][][][] MENUS = {
        {{{"Pizza"},
            {"Margherita", "San Marzano tomato, fior di latte, basil.", 1600, true,
                "Extra mozzarella:250|Fresh chili:100|Gluten-free crust:300"},
            {"Diavola", "Spicy salami, chili honey, oregano.", 1900, true,
                "Extra salami:350|Burrata:450"},
            {"Funghi Bianca", "Wild mushrooms, ricotta, thyme, no tomato.", 1850, false, ""}},
         {{"Starters"},
            {"Burrata", "Heirloom tomatoes, basil oil, sourdough.", 1400, false, ""},
            {"Garlic Knots", "With whipped ricotta.", 800, false, ""}}},
        {{{"Burgers"},
            {"Double Smash", "Two dry-aged patties, American cheese, house sauce.", 1500, true,
                "Bacon:250|Extra patty:400|Lettuce wrap:0"},
            {"Mushroom Melt", "Swiss, caramelised onion, truffle mayo.", 1600, false, ""},
            {"Crispy Chicken", "Pickles, slaw, hot honey.", 1450, false, ""}},
         {{"Sides"},
            {"Hand-cut Fries", "Sea salt and rosemary.", 600, true, "Truffle:200|Cheese:150"},
            {"Onion Rings", "Beer battered.", 700, false, ""},
            {"Shake", "Vanilla, chocolate or strawberry.", 750, false, ""}}},
        {{{"Nigiri sets"},
            {"Omakase Twelve", "The chef's twelve pieces of the day.", 5800, true, ""},
            {"Salmon Lover", "Eight salmon nigiri, three ways.", 3200, false, ""}},
         {{"Rolls and bowls"},
            {"Toro Hand Roll", "Fatty tuna, scallion, pickled radish.", 1400, true, ""},
            {"Chirashi Bowl", "Chef's selection over seasoned rice.", 3600, false,
                "Extra uni:1200|Ikura:600"},
            {"Miso Soup", "White miso, tofu, wakame.", 450, false, ""}}},
        {{{"Fried chicken"},
            {"Three-piece Meal", "Thigh, drumstick and wing, with a biscuit.", 1550, true,
                "Make it spicy:0|Extra biscuit:300"},
            {"Chicken Sandwich", "Buttermilk thigh, slaw, pickles.", 1350, true, ""},
            {"Tenders", "Five tenders and two sauces.", 1250, false, ""}},
         {{"Sides"},
            {"Mac and Cheese", "Three cheeses, breadcrumb crust.", 650, false, ""},
            {"Collard Greens", "Slow cooked.", 550, false, ""}}},
        {{{"Kebabs"},
            {"Lamb Adana", "Hand-minced lamb, sumac onions, grilled pepper.", 1900, true,
                "Extra pita:150|Garlic sauce:100"},
            {"Chicken Shish", "Yoghurt-marinated thigh, saffron rice.", 1700, true, ""},
            {"Falafel Plate", "Tahini, pickles, salad.", 1450, false, ""}},
         {{"Mezze"},
            {"Hummus", "With warm pita.", 750, false, ""},
            {"Baba Ghanoush", "Smoked aubergine, pomegranate.", 800, false, ""}}},
        {{{"Rice"},
            {"Claypot Chicken Rice", "Lap cheong, mushroom, soy.", 1600, true, "Fried egg:200"},
            {"Pork Belly Bowl", "Five-spice belly, pickled greens.", 1700, false, ""},
            {"Seafood Congee", "Prawn, scallop, ginger.", 1500, false, ""}},
         {{"Wok"},
            {"Garlic Greens", "Gai lan, garlic, oyster sauce.", 900, false, ""},
            {"Salt and Pepper Tofu", "Chili, scallion.", 1100, true, ""}}},
        {{{"Steaks"},
            {"Ribeye 14oz", "Dry-aged, with peppercorn sauce.", 5400, true,
                "Bearnaise:300|Truffle butter:500"},
            {"Filet 8oz", "With red wine jus.", 4800, false, ""},
            {"Steak Frites", "Hanger steak, fries, chimichurri.", 3200, true, ""}},
         {{"Sides"},
            {"Creamed Spinach", "Nutmeg and parmesan.", 900, false, ""},
            {"Potato Gratin", "Gruyere and thyme.", 1000, false, ""}}},
        {{{"Tacos"},
            {"Al Pastor", "Spit-roasted pork, pineapple, salsa verde. Three tacos.", 1200, true,
                "Extra pineapple:100|Guacamole:250"},
            {"Baja Fish", "Beer-battered cod, cabbage, chipotle crema.", 1350, true, ""},
            {"Mushroom Asada", "Grilled oyster mushrooms, salsa macha.", 1150, false, ""}},
         {{"Extras"},
            {"Chips and Guacamole", "Made to order.", 800, false, ""},
            {"Elote", "Grilled corn, cotija, lime.", 600, false, ""}}},
        {{{"Bowls"},
            {"Harvest Bowl", "Wild rice, roast squash, kale, goat cheese.", 1500, true,
                "Chicken:400|Salmon:600|Avocado:250"},
            {"Green Goddess", "Greens, cucumber, herbs, chickpeas.", 1350, false, ""},
            {"Spicy Tuna Poke", "Seasoned rice, edamame, nori.", 1700, true, ""}},
         {{"Drinks"},
            {"Cold-pressed Juice", "Green or citrus.", 750, false, ""}}},
        {{{"Donuts"},
            {"Classic Glazed", "Brioche, vanilla glaze.", 400, true, "Make it a dozen:3600"},
            {"Creme Brulee", "Custard filled, torched sugar.", 550, true, ""},
            {"Maple Bacon", "Maple glaze, candied bacon.", 550, false, ""}},
         {{"Coffee"},
            {"Drip Coffee", "House blend.", 350, false, ""}}},
        {{{"Gelato"},
            {"Pint of Gelato", "Pistachio, chocolate, or seasonal.", 1200, true, ""},
            {"Affogato", "Vanilla gelato, espresso.", 700, false, ""}},
         {{"Pastry"},
            {"Lemon Tart", "Italian meringue.", 800, true, ""},
            {"Chocolate Torte", "Flourless, 70% cacao.", 850, false, ""}}},
        {{{"Coffee"},
            {"Flat White", "Double ristretto.", 500, true, "Oat milk:75|Extra shot:100"},
            {"Cold Brew", "Steeped eighteen hours.", 550, false, ""}},
         {{"Bakery"},
            {"Almond Croissant", "Baked this morning.", 550, true, ""},
            {"Breakfast Sandwich", "Egg, cheddar, chive, brioche.", 950, false, "Bacon:200"}}}};

    private final RestaurantRepository restaurants;
    private final RestaurantStaffRepository staff;
    private final MenuSectionRepository sections;
    private final DishRepository dishes;
    private final DishOptionRepository options;
    private final PromoRepository promos;
    private final Accounts accounts;

    public DemoRestaurants(RestaurantRepository restaurants, RestaurantStaffRepository staff,
            MenuSectionRepository sections, DishRepository dishes, DishOptionRepository options,
            PromoRepository promos, Accounts accounts) {
        this.restaurants = restaurants;
        this.staff = staff;
        this.sections = sections;
        this.dishes = dishes;
        this.options = options;
        this.promos = promos;
        this.accounts = accounts;
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
            restaurant.setPhotoId("");
            restaurant.setApplicant(iter == 0 ? merchant : "");
            restaurant.setReason("");
            restaurant.setCreatedAt(now - (60 - iter) * 86400000L);
            restaurant.setUpdatedAt(now);
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
        waiting.setCreatedAt(now - 2L * 86400000L);
        waiting.setUpdatedAt(now - 2L * 86400000L);
        restaurants.insert(waiting);

        promo("WELCOME10", 10, 0L, 1500L, now);
        promo("SAVOR5", 0, 500L, 2500L, now);
    }

    private void menu(String restaurantId, Object[][][] menu) throws IOException {
        int dishAt = 0;
        for (int section = 0; section < menu.length; section++) {
            MenuSection row = new MenuSection();
            row.setId(Ids.next());
            row.setRestaurantId(restaurantId);
            row.setName(String.valueOf(menu[section][0][0]));
            row.setPosition(section);
            sections.insert(row);
            for (int iter = 1; iter < menu[section].length; iter++) {
                Object[] food = menu[section][iter];
                Dish dish = new Dish();
                dish.setId(Ids.next());
                dish.setRestaurantId(restaurantId);
                dish.setSectionId(row.getId());
                dish.setName(String.valueOf(food[0]));
                dish.setDescription(String.valueOf(food[1]));
                dish.setPriceCents(whole(food[2]));
                dish.setPopular(Boolean.TRUE.equals(food[3]));
                dish.setAvailable(true);
                dish.setPhotoId("");
                dish.setPosition(dishAt++);
                dishes.insert(dish);
                String extras = String.valueOf(food[4]);
                if (extras.length() == 0) {
                    continue;
                }
                String[] parts = extras.split("\\|");
                for (int option = 0; option < parts.length; option++) {
                    int colon = parts[option].indexOf(':');
                    DishOption extra = new DishOption();
                    extra.setId(Ids.next());
                    extra.setDishId(dish.getId());
                    extra.setName(parts[option].substring(0, colon));
                    extra.setPriceCents(Long.parseLong(parts[option].substring(colon + 1)));
                    extra.setPosition(option);
                    options.insert(extra);
                }
            }
        }
    }

    private void promo(String code, int percent, long amount, long minimum, long now) {
        Promo promo = new Promo();
        promo.setCode(code);
        promo.setPercentOff(percent);
        promo.setAmountOffCents(amount);
        promo.setMinimumCents(minimum);
        promo.setActive(true);
        promo.setCreatedAt(now);
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
