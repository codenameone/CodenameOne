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

import com.codename1.backend.Base64;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.orm.session.Identifier;
import com.codenameone.examples.savor.api.DishDto;
import com.codenameone.examples.savor.api.DishOptionDto;
import com.codenameone.examples.savor.api.MenuSectionDto;
import com.codenameone.examples.savor.api.PhotoDto;
import com.codenameone.examples.savor.api.PricingDto;
import com.codenameone.examples.savor.api.RestaurantDto;
import com.codenameone.examples.savor.api.RestaurantSummaryDto;
import com.codenameone.examples.savor.domain.Dish;
import com.codenameone.examples.savor.domain.DishOption;
import com.codenameone.examples.savor.domain.Favorite;
import com.codenameone.examples.savor.domain.MenuSection;
import com.codenameone.examples.savor.domain.Photo;
import com.codenameone.examples.savor.domain.Restaurant;
import com.codenameone.examples.savor.geo.Geo;
import com.codenameone.examples.savor.order.Quotes;
import com.codenameone.examples.savor.repository.DishOptionRepository;
import com.codenameone.examples.savor.repository.DishRepository;
import com.codenameone.examples.savor.repository.FavoriteRepository;
import com.codenameone.examples.savor.repository.MenuSectionRepository;
import com.codenameone.examples.savor.repository.PhotoRepository;
import com.codenameone.examples.savor.repository.RestaurantRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// What a customer browses: the restaurants near a point, one restaurant and
/// its menu, the photos, and the restaurants they keep as favourites.
///
/// Nearby is a bounding box the database answers from its index, measured and
/// sorted here, the way couriers are found in `Matcher`.
@Component
public class Catalog {
    public static final String APPROVED = "APPROVED";
    public static final String PENDING = "PENDING";
    public static final String REJECTED = "REJECTED";
    public static final String SUSPENDED = "SUSPENDED";
    static final String[] SORTS = {"distance", "rating", "fee", "time"};

    private final RestaurantRepository restaurants;
    private final MenuSectionRepository sections;
    private final DishRepository dishes;
    private final DishOptionRepository options;
    private final PhotoRepository photos;
    private final FavoriteRepository favorites;
    private final Quotes quotes;

    public Catalog(RestaurantRepository restaurants, MenuSectionRepository sections,
            DishRepository dishes, DishOptionRepository options, PhotoRepository photos,
            FavoriteRepository favorites, Quotes quotes) {
        this.restaurants = restaurants;
        this.sections = sections;
        this.dishes = dishes;
        this.options = options;
        this.photos = photos;
        this.favorites = favorites;
        this.quotes = quotes;
    }

    // tag::savor-nearby[]
    /// The approved restaurants that deliver to a point, filtered by cuisine and
    /// by a word in the name, the cuisine or a dish, and sorted.
    @Transactional(readOnly = true)
    public List<RestaurantSummaryDto> nearby(String viewer, double lat, double lng,
            String cuisine, String text, String sort) throws IOException {
        if (!Geo.isPoint(lat, lng)) {
            throw new ResponseStatusException(400, "Choose where to deliver to");
        }
        double straight = Quotes.MAX_METERS / Quotes.ROAD_FACTOR;
        double dLat = straight / Geo.METERS_PER_DEGREE;
        double dLng = dLat / Math.max(0.01d, Math.cos(Math.toRadians(lat)));
        List<Restaurant> box = restaurants.inBox(APPROVED, lat - dLat, lat + dLat, lng - dLng,
                lng + dLng);
        String word = text == null ? "" : text.trim();
        Set<String> dishMatches = word.length() == 0 ? Collections.<String>emptySet()
                : dishMatches(box, word);
        Set<String> kept = favoriteIds(viewer);
        PricingDto pricing = quotes.pricing();
        int minute = Quotes.minuteOfDay(System.currentTimeMillis());
        List<RestaurantSummaryDto> out = new ArrayList<RestaurantSummaryDto>();
        for (int iter = 0; iter < box.size(); iter++) {
            Restaurant restaurant = box.get(iter);
            if (cuisine != null && cuisine.length() > 0
                    && !cuisine.equalsIgnoreCase(restaurant.getCuisine())) {
                continue;
            }
            if (word.length() > 0 && !contains(restaurant.getName(), word)
                    && !contains(restaurant.getCuisine(), word)
                    && !dishMatches.contains(restaurant.getId())) {
                continue;
            }
            double meters = Quotes.roadMeters(restaurant.getLat(), restaurant.getLng(), lat, lng);
            if (meters > Quotes.MAX_METERS) {
                continue;
            }
            out.add(summary(restaurant, meters, pricing, minute,
                    kept.contains(restaurant.getId())));
        }
        sort(out, sort);
        return out;
    }
    // end::savor-nearby[]

    private static void sort(List<RestaurantSummaryDto> list, String sort) {
        String by = sort == null ? SORTS[0] : sort;
        Comparator<RestaurantSummaryDto> order;
        if ("rating".equals(by)) {
            order = (a, b) -> Double.compare(b.rating(), a.rating());
        } else if ("fee".equals(by)) {
            order = (a, b) -> Long.compare(a.deliveryFeeCents(), b.deliveryFeeCents());
        } else if ("time".equals(by)) {
            order = (a, b) -> Integer.compare(a.etaMinutes(), b.etaMinutes());
        } else {
            order = (a, b) -> Double.compare(a.distanceMeters(), b.distanceMeters());
        }
        // Open restaurants first, whatever the order asked for.
        Collections.sort(list, (a, b) -> a.open() == b.open() ? order.compare(a, b)
                : (a.open() ? -1 : 1));
    }

    /// Whether `text` holds `word`, ignoring ASCII case, without folding
    /// either: `toLowerCase()` follows the server's locale.
    static boolean contains(String text, String word) {
        if (text == null) {
            return false;
        }
        int last = text.length() - word.length();
        for (int iter = 0; iter <= last; iter++) {
            if (text.regionMatches(true, iter, word, 0, word.length())) {
                return true;
            }
        }
        return false;
    }

    private Set<String> dishMatches(List<Restaurant> box, String word) {
        Set<String> out = new HashSet<String>();
        if (box.isEmpty()) {
            return out;
        }
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < box.size(); iter++) {
            ids.add(box.get(iter).getId());
        }
        List<Dish> rows = dishes.findByRestaurantIdIn(ids);
        for (int iter = 0; iter < rows.size(); iter++) {
            Dish dish = rows.get(iter);
            if (dish.isAvailable() && (contains(dish.getName(), word)
                    || contains(dish.getDescription(), word))) {
                out.add(dish.getRestaurantId());
            }
        }
        return out;
    }

    private Set<String> favoriteIds(String viewer) {
        Set<String> out = new HashSet<String>();
        if (viewer == null) {
            return out;
        }
        List<Favorite> rows = favorites.findByUsernameOrderByCreatedAtDesc(viewer);
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(rows.get(iter).getRestaurantId());
        }
        return out;
    }

    static double rating(Restaurant restaurant) {
        return restaurant.getRatingCount() == 0L ? 0d
                : Math.round(restaurant.getRatingTotal() * 10d / restaurant.getRatingCount())
                        / 10d;
    }

    private static RestaurantSummaryDto summary(Restaurant restaurant, double meters,
            PricingDto pricing, int minute, boolean favorite) {
        return new RestaurantSummaryDto(restaurant.getId(), restaurant.getName(),
                restaurant.getCuisine(), restaurant.getPhotoId(), rating(restaurant),
                restaurant.getRatingCount(), restaurant.getPriceLevel(),
                restaurant.getPrepMinutes() + Quotes.travelMinutes(meters),
                Quotes.deliveryFee(pricing, meters), meters, Quotes.open(restaurant, minute),
                favorite);
    }

    /// One restaurant and its menu, as a customer sees it. A restaurant that is
    /// not approved is not found, except by its own staff and the admins, who
    /// see it through [#detail].
    @Transactional(readOnly = true)
    public RestaurantDto restaurant(String viewer, String id, double lat, double lng)
            throws IOException {
        Restaurant restaurant = id == null ? null : restaurants.findById(id);
        if (restaurant == null || !APPROVED.equals(restaurant.getStatus())) {
            throw new ResponseStatusException(404, "No such restaurant");
        }
        return detail(restaurant, viewer, lat, lng, false);
    }

    /// A restaurant and its whole menu.
    ///
    /// @param everything whether dishes that are sold out are listed: the
    ///     kitchen sees them to put them back, a customer sees them greyed
    public RestaurantDto detail(Restaurant restaurant, String viewer, double lat, double lng,
            boolean everything) throws IOException {
        PricingDto pricing = quotes.pricing();
        boolean located = Geo.isPoint(lat, lng);
        double meters = located ? Quotes.roadMeters(restaurant.getLat(), restaurant.getLng(),
                lat, lng) : 0d;
        List<MenuSection> rows = sections.findByRestaurantIdOrderByPositionAsc(restaurant.getId());
        List<Dish> food = dishes.findByRestaurantIdOrderByPositionAsc(restaurant.getId());
        List<String> ids = new ArrayList<String>();
        for (int iter = 0; iter < food.size(); iter++) {
            ids.add(food.get(iter).getId());
        }
        Map<String, List<DishOptionDto>> extras = new HashMap<String, List<DishOptionDto>>();
        if (!ids.isEmpty()) {
            List<DishOption> all = options.findByDishIdIn(ids);
            Collections.sort(all, (a, b) -> Integer.compare(a.getPosition(), b.getPosition()));
            for (int iter = 0; iter < all.size(); iter++) {
                DishOption option = all.get(iter);
                List<DishOptionDto> list = extras.get(option.getDishId());
                if (list == null) {
                    list = new ArrayList<DishOptionDto>();
                    extras.put(option.getDishId(), list);
                }
                list.add(new DishOptionDto(option.getId(), option.getName(),
                        option.getPriceCents()));
            }
        }
        List<MenuSectionDto> menu = new ArrayList<MenuSectionDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            MenuSection section = rows.get(iter);
            List<DishDto> list = new ArrayList<DishDto>();
            for (int dish = 0; dish < food.size(); dish++) {
                Dish row = food.get(dish);
                if (row.getSectionId().equals(section.getId())) {
                    list.add(dish(row, extras.get(row.getId())));
                }
            }
            if (everything || !list.isEmpty()) {
                menu.add(new MenuSectionDto(section.getId(), section.getName(), list));
            }
        }
        boolean favorite = viewer != null && favorites.existsById(Identifier.of(viewer,
                restaurant.getId()));
        return new RestaurantDto(restaurant.getId(), restaurant.getName(),
                restaurant.getCuisine(), restaurant.getDescription(), restaurant.getAddress(),
                restaurant.getLat(), restaurant.getLng(), restaurant.getPhotoId(),
                rating(restaurant), restaurant.getRatingCount(), restaurant.getPriceLevel(),
                restaurant.getPrepMinutes() + (located ? Quotes.travelMinutes(meters) : 0),
                located ? Quotes.deliveryFee(pricing, meters) : pricing.baseCents(),
                restaurant.getMinimumCents(),
                Quotes.open(restaurant, Quotes.minuteOfDay(System.currentTimeMillis())),
                restaurant.isPaused(), restaurant.getOpensAt(), restaurant.getClosesAt(),
                restaurant.getPrepMinutes(), favorite, restaurant.getStatus(),
                restaurant.getReason(), menu);
    }

    static DishDto dish(Dish row, List<DishOptionDto> extras) {
        return new DishDto(row.getId(), row.getSectionId(), row.getName(), row.getDescription(),
                row.getPriceCents(), row.getPhotoId(), row.isAvailable(), row.isPopular(),
                extras == null ? new ArrayList<DishOptionDto>() : extras);
    }

    /// A photo, to anyone: menus are public.
    @Transactional(readOnly = true)
    public PhotoDto photo(String id) {
        Photo photo = id == null || id.length() > 32 ? null : photos.findById(id);
        if (photo == null) {
            throw new ResponseStatusException(404, "No such photo");
        }
        return new PhotoDto(photo.getId(), photo.getContentType(), Base64.encode(photo.getData()));
    }

    // ------------------------------------------------------------- favorites

    @Transactional(readOnly = true)
    public List<RestaurantSummaryDto> favorites(String viewer, double lat, double lng)
            throws IOException {
        List<Favorite> rows = favorites.findByUsernameOrderByCreatedAtDesc(viewer);
        PricingDto pricing = quotes.pricing();
        boolean located = Geo.isPoint(lat, lng);
        int minute = Quotes.minuteOfDay(System.currentTimeMillis());
        List<RestaurantSummaryDto> out = new ArrayList<RestaurantSummaryDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            Restaurant restaurant = restaurants.findById(rows.get(iter).getRestaurantId());
            if (restaurant != null && APPROVED.equals(restaurant.getStatus())) {
                double meters = located ? Quotes.roadMeters(restaurant.getLat(),
                        restaurant.getLng(), lat, lng) : 0d;
                out.add(summary(restaurant, meters, pricing, minute, true));
            }
        }
        return out;
    }

    @Transactional
    public RestaurantSummaryDto favorite(String viewer, String id, boolean keep)
            throws IOException {
        Restaurant restaurant = id == null ? null : restaurants.findById(id);
        if (restaurant == null || !APPROVED.equals(restaurant.getStatus())) {
            throw new ResponseStatusException(404, "No such restaurant");
        }
        Identifier key = Identifier.of(viewer, id);
        boolean kept = favorites.existsById(key);
        if (keep && !kept) {
            if (favorites.countByUsername(viewer) >= 200) {
                throw new ResponseStatusException(409, "That is enough favourites");
            }
            Favorite row = new Favorite();
            row.setUsername(viewer);
            row.setRestaurantId(id);
            row.setCreatedAt(System.currentTimeMillis());
            favorites.insert(row);
        } else if (!keep && kept) {
            favorites.deleteById(key);
        }
        return summary(restaurant, 0d, quotes.pricing(),
                Quotes.minuteOfDay(System.currentTimeMillis()), keep);
    }
}
