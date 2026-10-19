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
import com.codenameone.examples.savor.Ids;
import com.codenameone.examples.savor.Text;
import com.codenameone.examples.savor.account.Accounts;
import com.codenameone.examples.savor.account.Moderation;
import com.codenameone.examples.savor.api.DishDto;
import com.codenameone.examples.savor.api.DishOptionDto;
import com.codenameone.examples.savor.api.MenuSectionDto;
import com.codenameone.examples.savor.api.MerchantSummaryDto;
import com.codenameone.examples.savor.api.OrderState;
import com.codenameone.examples.savor.api.PhotoDto;
import com.codenameone.examples.savor.api.RestaurantDto;
import com.codenameone.examples.savor.api.RestaurantEditDto;
import com.codenameone.examples.savor.domain.CustomerOrder;
import com.codenameone.examples.savor.domain.Dish;
import com.codenameone.examples.savor.domain.DishOption;
import com.codenameone.examples.savor.domain.MenuSection;
import com.codenameone.examples.savor.domain.Photo;
import com.codenameone.examples.savor.domain.Restaurant;
import com.codenameone.examples.savor.domain.RestaurantStaff;
import com.codenameone.examples.savor.geo.Geo;
import com.codenameone.examples.savor.onboarding.Applications;
import com.codenameone.examples.savor.order.Quotes;
import com.codenameone.examples.savor.pay.Payments;
import com.codenameone.examples.savor.repository.CustomerOrderRepository;
import com.codenameone.examples.savor.repository.DishOptionRepository;
import com.codenameone.examples.savor.repository.DishRepository;
import com.codenameone.examples.savor.repository.MenuSectionRepository;
import com.codenameone.examples.savor.repository.PhotoRepository;
import com.codenameone.examples.savor.repository.ProfileRepository;
import com.codenameone.examples.savor.repository.RestaurantRepository;
import com.codenameone.examples.savor.repository.RestaurantStaffRepository;
import com.codenameone.examples.savor.Days;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Restaurants as their owners and the admins see them: applying to join,
/// approval, the kitchen's own details and menu, and photo uploads.
///
/// A merchant acts for the restaurant whose staff they are on, found from
/// the signed-in account on every request; the client never names it.
@Component
public class Restaurants {
    /// A photo is kept in the database as base64, so it is kept small: the
    /// client scales it down before it uploads.
    static final int MAX_PHOTO_CHARS = 700000;

    /// The most photo rows one restaurant may hold. Every upload is a new
    /// row, so without a cap one account could grow the database without
    /// bound; rows nothing uses are pruned first (see [#prune]).
    static final int MAX_PHOTOS = 300;

    /// How long an uploaded photo nothing uses yet is kept: the app uploads
    /// a picture and then saves the form naming it.
    static final long UNUSED_PHOTO_GRACE = 3600000L;
    static final int MAX_DISHES = 300;
    static final int MAX_SECTIONS = 40;
    static final int MAX_OPTIONS = 20;

    private final RestaurantRepository restaurants;
    private final RestaurantStaffRepository staff;
    private final MenuSectionRepository sections;
    private final DishRepository dishes;
    private final DishOptionRepository options;
    private final PhotoRepository photos;
    private final CustomerOrderRepository orders;
    private final Catalog catalog;
    private final Accounts accounts;
    private final Moderation moderation;
    private final Quotes quotes;
    private final ProfileRepository profiles;

    public Restaurants(RestaurantRepository restaurants, RestaurantStaffRepository staff,
            MenuSectionRepository sections, DishRepository dishes, DishOptionRepository options,
            PhotoRepository photos, CustomerOrderRepository orders, Catalog catalog,
            Accounts accounts, Moderation moderation, Quotes quotes,
            ProfileRepository profiles) {
        this.restaurants = restaurants;
        this.staff = staff;
        this.sections = sections;
        this.dishes = dishes;
        this.options = options;
        this.photos = photos;
        this.orders = orders;
        this.catalog = catalog;
        this.accounts = accounts;
        this.moderation = moderation;
        this.quotes = quotes;
        this.profiles = profiles;
    }

    /// The restaurant `username` works for; 403 when none.
    @Transactional(readOnly = true)
    public Restaurant mine(String username) {
        List<RestaurantStaff> rows = staff.findByUsername(username);
        Restaurant restaurant = rows.isEmpty() ? null
                : restaurants.findById(rows.get(0).getRestaurantId());
        if (restaurant == null) {
            throw new ResponseStatusException(403, "This account works for no restaurant");
        }
        return restaurant;
    }

    // ----------------------------------------------------------- application

    // tag::savor-apply[]
    /// Asks for a restaurant to be listed. It waits, unseen by customers, for
    /// an admin to approve it; the applicant then joins its staff and gets the
    /// merchant role. One application at a time per account.
    @Transactional
    public RestaurantDto apply(String username, RestaurantEditDto form) throws IOException {
        // Two submissions at once would both find no application below and
        // both insert one; holding the profile first makes the second wait
        // and then see the first.
        profiles.hold(username);
        Restaurant last = restaurants.findFirstByApplicantOrderByCreatedAtDesc(username);
        if (last != null && !Catalog.REJECTED.equals(last.getStatus())) {
            throw new ResponseStatusException(409, Catalog.PENDING.equals(last.getStatus())
                    ? "Your application is waiting for review" : "You already have a restaurant");
        }
        Restaurant restaurant = new Restaurant();
        restaurant.setId(Ids.next());
        restaurant.setStatus(Catalog.PENDING);
        restaurant.setApplicant(username);
        restaurant.setReason("");
        restaurant.setPhotoId("");
        long now = System.currentTimeMillis();
        restaurant.setCreatedAt(now);
        restaurant.setUpdatedAt(now);
        fill(restaurant, form);
        restaurants.insert(restaurant);
        return catalog.detail(restaurant, username, 0d, 0d, true);
    }
    // end::savor-apply[]

    /// The account's latest application, or 404.
    @Transactional(readOnly = true)
    public RestaurantDto application(String username) throws IOException {
        Restaurant last = restaurants.findFirstByApplicantOrderByCreatedAtDesc(username);
        if (last == null) {
            throw new ResponseStatusException(404, "No application");
        }
        return catalog.detail(last, username, 0d, 0d, true);
    }

    /// A photo for the application waiting for review, which becomes its cover.
    @Transactional
    public PhotoDto applicationPhoto(String username, PhotoDto photo) throws IOException {
        Restaurant found = restaurants.findFirstByApplicantOrderByCreatedAtDesc(username);
        // Held and read again: an approval or a rejection that lands meanwhile
        // is seen here, and one that comes after waits for this photo, so a
        // listed restaurant never shows a cover nobody reviewed.
        Restaurant last = found == null || restaurants.hold(found.getId()) == 0 ? null
                : restaurants.findById(found.getId());
        if (last == null || !Catalog.PENDING.equals(last.getStatus())) {
            throw new ResponseStatusException(409, "Apply first");
        }
        PhotoDto saved = store(last.getId(), photo);
        last.setPhotoId(saved.id());
        last.setUpdatedAt(System.currentTimeMillis());
        return saved;
    }

    private static void fill(Restaurant restaurant, RestaurantEditDto form) {
        if (form == null) {
            throw new ResponseStatusException(400, "Fill in the restaurant's details");
        }
        restaurant.setName(Text.required(form.name(), 80, "the restaurant's name"));
        restaurant.setCuisine(Text.required(form.cuisine(), 32, "the cuisine"));
        restaurant.setDescription(Text.optional(form.description(), 500, "the description"));
        restaurant.setAddress(Text.required(form.address(), 255, "the address"));
        if (!Geo.isPoint(form.lat(), form.lng())) {
            throw new ResponseStatusException(400, "Choose where the restaurant is");
        }
        restaurant.setLat(form.lat());
        restaurant.setLng(form.lng());
        if (form.opensAt() < 0 || form.opensAt() >= 1440 || form.closesAt() < 0
                || form.closesAt() >= 1440) {
            throw new ResponseStatusException(400, "Check the opening hours");
        }
        restaurant.setOpensAt(form.opensAt());
        restaurant.setClosesAt(form.closesAt());
        if (form.prepMinutes() < 1 || form.prepMinutes() > 180) {
            throw new ResponseStatusException(400, "Check how long an order takes");
        }
        restaurant.setPrepMinutes(form.prepMinutes());
        if (form.minimumCents() < 0 || form.minimumCents() > 100000) {
            throw new ResponseStatusException(400, "Check the minimum order");
        }
        restaurant.setMinimumCents(form.minimumCents());
        restaurant.setPriceLevel(Math.max(1, Math.min(4, form.priceLevel())));
    }

    // ----------------------------------------------------------------- admin

    @Transactional(readOnly = true)
    public List<RestaurantDto> byStatus(String status) throws IOException {
        List<Restaurant> rows = restaurants.findByStatusOrderByCreatedAtDesc(status);
        List<RestaurantDto> out = new ArrayList<RestaurantDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            out.add(catalog.detail(rows.get(iter), null, 0d, 0d, true));
        }
        return out;
    }

    /// Lists an application: the restaurant is shown to customers, and its
    /// applicant joins its staff with the merchant role.
    @Transactional(rollbackFor = IOException.class)
    public RestaurantDto approve(String by, String id) throws IOException {
        // Held before it is read, as an application photo holds it: the two
        // take turns, so what is approved is what the admin last saw.
        moderated(id);
        Restaurant restaurant = existing(id);
        // Only an application: a suspended restaurant comes back through
        // suspend(), which checks it still has staff to take its orders.
        if (!Catalog.PENDING.equals(restaurant.getStatus())) {
            throw new ResponseStatusException(409, Catalog.APPROVED.equals(restaurant.getStatus())
                    ? "That restaurant is already listed" : "Only an application can be approved");
        }
        // Listed without its applicant, it would take orders nobody can see.
        // The profile is held so a closure cannot land between this and the
        // staff row below.
        String applicant = restaurant.getApplicant();
        if (applicant == null || applicant.length() == 0 || profiles.hold(applicant) == 0
                || !accounts.exists(applicant)) {
            throw new ResponseStatusException(409,
                    "The applicant closed their account; turn the application down instead");
        }
        if (!accounts.enabled(applicant)) {
            // Blocked: listed, it would take orders its merchant cannot see.
            throw new ResponseStatusException(409,
                    "The applicant is blocked; unblock them or turn the application down");
        }
        restaurant.setStatus(Catalog.APPROVED);
        restaurant.setReason("");
        restaurant.setUpdatedAt(System.currentTimeMillis());
        if (!staff.existsById(Identifier.of(restaurant.getId(), applicant))) {
            RestaurantStaff row = new RestaurantStaff();
            row.setRestaurantId(restaurant.getId());
            row.setUsername(applicant);
            staff.insert(row);
        }
        accounts.setMerchant(applicant, true);
        moderation.record(applicant, by, Moderation.LISTED, restaurant.getName());
        return catalog.detail(restaurant, null, 0d, 0d, true);
    }

    @Transactional
    public RestaurantDto reject(String by, String id, String reason) throws IOException {
        // Held before it is read, as an application photo holds it: the two
        // take turns, so what is approved is what the admin last saw.
        moderated(id);
        Restaurant restaurant = existing(id);
        if (!Catalog.PENDING.equals(restaurant.getStatus())) {
            throw new ResponseStatusException(409, "Only an application can be turned down");
        }
        restaurant.setStatus(Catalog.REJECTED);
        restaurant.setReason(Text.required(reason, 255, "why it is turned down"));
        restaurant.setUpdatedAt(System.currentTimeMillis());
        return catalog.detail(restaurant, null, 0d, 0d, true);
    }

    /// Takes a listed restaurant off the app, or puts it back. Its staff keep
    /// their places and can still see their orders.
    @Transactional
    public RestaurantDto suspend(String by, String id, boolean suspended, String reason)
            throws IOException {
        Restaurant restaurant = existing(id);
        String from = suspended ? Catalog.APPROVED : Catalog.SUSPENDED;
        if (!from.equals(restaurant.getStatus())) {
            throw new ResponseStatusException(409, suspended
                    ? "Only a listed restaurant can be suspended" : "That restaurant is not suspended");
        }
        if (!suspended && staff.findByRestaurantId(restaurant.getId()).isEmpty()) {
            // Its only merchant may have closed their account while it was
            // suspended. Listed again, it would take orders nobody can see.
            throw new ResponseStatusException(409,
                    "That restaurant has no staff left to take its orders");
        }
        restaurant.setStatus(suspended ? Catalog.SUSPENDED : Catalog.APPROVED);
        restaurant.setReason(suspended ? Text.required(reason, 255, "why it is suspended") : "");
        restaurant.setUpdatedAt(System.currentTimeMillis());
        return catalog.detail(restaurant, null, 0d, 0d, true);
    }

    private void moderated(String id) {
        if (id != null && id.length() <= 32) {
            restaurants.hold(id);
        }
    }

    private Restaurant existing(String id) {
        Restaurant restaurant = id == null || id.length() > 32 ? null : restaurants.findById(id);
        if (restaurant == null) {
            throw new ResponseStatusException(404, "No such restaurant");
        }
        return restaurant;
    }

    // -------------------------------------------------------------- merchant

    @Transactional(readOnly = true)
    public RestaurantDto view(String username) throws IOException {
        return catalog.detail(mine(username), username, 0d, 0d, true);
    }

    @Transactional
    public RestaurantDto save(String username, RestaurantEditDto form) throws IOException {
        Restaurant restaurant = mine(username);
        fill(restaurant, form);
        if (form.photoId() != null && form.photoId().length() > 0) {
            ownPhoto(restaurant, form.photoId());
            restaurant.setPhotoId(form.photoId());
        }
        restaurant.setUpdatedAt(System.currentTimeMillis());
        return catalog.detail(restaurant, username, 0d, 0d, true);
    }

    /// Stops new orders for a while, or takes them again.
    @Transactional
    public RestaurantDto pause(String username, boolean paused) throws IOException {
        Restaurant restaurant = mine(username);
        restaurant.setPaused(paused);
        restaurant.setUpdatedAt(System.currentTimeMillis());
        return catalog.detail(restaurant, username, 0d, 0d, true);
    }

    /// Today's and this week's takings: delivered orders' subtotals after
    /// discount, less the platform's commission.
    @Transactional(readOnly = true)
    public MerchantSummaryDto summary(String username) throws IOException {
        Restaurant restaurant = mine(username);
        long now = System.currentTimeMillis();
        long today = Days.start(now);
        long week = today - 6L * Days.MILLIS;
        List<CustomerOrder> delivered = orders.findByRestaurantIdAndStateAndDeliveredAtGreaterThanEqual(
                restaurant.getId(), OrderState.DELIVERED, week);
        long count = 0L;
        long todayCents = 0L;
        long weekCents = 0L;
        for (int iter = 0; iter < delivered.size(); iter++) {
            CustomerOrder order = delivered.get(iter);
            // A delivered order whose capture failed, or that was refunded,
            // took no money: counted only once paid, as the admin's revenue is.
            if (!Payments.PAID.equals(order.getPaymentStatus())) {
                continue;
            }
            long earned = order.getSubtotalCents() - order.getDiscountCents()
                    - order.getCommissionCents();
            weekCents += earned;
            if (order.getDeliveredAt() >= today) {
                todayCents += earned;
                count++;
            }
        }
        int waiting = orders.findByRestaurantIdAndStateInOrderByPlacedAtAsc(restaurant.getId(),
                new OrderState[] {OrderState.PLACED}).size();
        return new MerchantSummaryDto(count, todayCents, weekCents, waiting, quotes.currency());
    }

    // ------------------------------------------------------------------ menu

    @Transactional
    public MenuSectionDto addSection(String username, MenuSectionDto form) {
        Restaurant restaurant = mine(username);
        if (sections.countByRestaurantId(restaurant.getId()) >= MAX_SECTIONS) {
            throw new ResponseStatusException(409, "That is enough sections");
        }
        MenuSection section = new MenuSection();
        section.setId(sectionId());
        section.setRestaurantId(restaurant.getId());
        section.setName(Text.required(form == null ? null : form.name(), 60, "the section's name"));
        section.setPosition((int) sections.countByRestaurantId(restaurant.getId()));
        sections.insert(section);
        return new MenuSectionDto(section.getId(), section.getName(), new ArrayList<DishDto>());
    }

    private static String sectionId() {
        try {
            return Ids.next();
        } catch (IOException err) {
            throw new IllegalStateException(err);
        }
    }

    @Transactional
    public MenuSectionDto saveSection(String username, String id, MenuSectionDto form) {
        MenuSection section = ownSection(mine(username), id);
        section.setName(Text.required(form == null ? null : form.name(), 60, "the section's name"));
        return new MenuSectionDto(section.getId(), section.getName(), new ArrayList<DishDto>());
    }

    @Transactional
    public MenuSectionDto removeSection(String username, String id) {
        MenuSection section = ownSection(mine(username), id);
        if (dishes.countBySectionId(id) > 0) {
            throw new ResponseStatusException(409, "Move or remove its dishes first");
        }
        sections.deleteById(id);
        return new MenuSectionDto(section.getId(), section.getName(), new ArrayList<DishDto>());
    }

    /// The merchant's restaurant, its row locked: a change to what an order can
    /// contain or cost takes the lock Orders.store takes before it prices the cart
    /// again, so an order is stored against the menu before the change or after
    /// it, never against one the customer was not charged for.
    private Restaurant editing(String username) {
        Restaurant restaurant = mine(username);
        restaurants.hold(restaurant.getId());
        return restaurant;
    }

    private MenuSection ownSection(Restaurant restaurant, String id) {
        MenuSection section = id == null || id.length() > 32 ? null : sections.findById(id);
        if (section == null || !section.getRestaurantId().equals(restaurant.getId())) {
            throw new ResponseStatusException(404, "No such section");
        }
        return section;
    }

    @Transactional
    public DishDto addDish(String username, DishDto form) throws IOException {
        Restaurant restaurant = mine(username);
        if (dishes.countByRestaurantId(restaurant.getId()) >= MAX_DISHES) {
            throw new ResponseStatusException(409, "That is enough dishes");
        }
        Dish dish = new Dish();
        dish.setId(Ids.next());
        dish.setRestaurantId(restaurant.getId());
        dish.setAvailable(true);
        dish.setPosition((int) dishes.countByRestaurantId(restaurant.getId()));
        fill(restaurant, dish, form);
        dishes.insert(dish);
        return writeOptions(dish, form);
    }

    @Transactional
    public DishDto saveDish(String username, String id, DishDto form) throws IOException {
        Restaurant restaurant = editing(username);
        Dish dish = ownDish(restaurant, id);
        fill(restaurant, dish, form);
        options.deleteByDishId(id);
        return writeOptions(dish, form);
    }

    @Transactional
    public DishDto removeDish(String username, String id) {
        Dish dish = ownDish(editing(username), id);
        options.deleteByDishId(id);
        dishes.deleteById(id);
        return Catalog.dish(dish, null);
    }

    /// Marks a dish sold out, or back on.
    @Transactional
    public DishDto available(String username, String id, boolean available) {
        Dish dish = ownDish(editing(username), id);
        dish.setAvailable(available);
        return Catalog.dish(dish, extras(id));
    }

    private void fill(Restaurant restaurant, Dish dish, DishDto form) {
        if (form == null) {
            throw new ResponseStatusException(400, "Fill in the dish");
        }
        dish.setName(Text.required(form.name(), 80, "the dish's name"));
        dish.setDescription(Text.optional(form.description(), 500, "the description"));
        if (form.priceCents() < 0 || form.priceCents() > 100000) {
            throw new ResponseStatusException(400, "Check the price");
        }
        dish.setPriceCents(form.priceCents());
        dish.setSectionId(ownSection(restaurant, form.sectionId()).getId());
        dish.setPopular(form.popular());
        String photo = form.photoId() == null ? "" : form.photoId();
        if (photo.length() > 0) {
            ownPhoto(restaurant, photo);
        }
        dish.setPhotoId(photo);
    }

    private DishDto writeOptions(Dish dish, DishDto form) throws IOException {
        List<DishOptionDto> wanted = form.options() == null ? new ArrayList<DishOptionDto>()
                : form.options();
        if (wanted.size() > MAX_OPTIONS) {
            throw new ResponseStatusException(400, "That is too many extras");
        }
        for (int iter = 0; iter < wanted.size(); iter++) {
            DishOptionDto dto = wanted.get(iter);
            if (dto.priceCents() < 0 || dto.priceCents() > 100000) {
                throw new ResponseStatusException(400, "Check the extra's price");
            }
            DishOption option = new DishOption();
            option.setId(Ids.next());
            option.setDishId(dish.getId());
            option.setName(Text.required(dto.name(), 60, "the extra's name"));
            option.setPriceCents(dto.priceCents());
            option.setPosition(iter);
            options.insert(option);
        }
        return Catalog.dish(dish, extras(dish.getId()));
    }

    private List<DishOptionDto> extras(String dishId) {
        List<DishOption> rows = options.findByDishIdOrderByPositionAsc(dishId);
        List<DishOptionDto> out = new ArrayList<DishOptionDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            DishOption row = rows.get(iter);
            out.add(new DishOptionDto(row.getId(), row.getName(), row.getPriceCents()));
        }
        return out;
    }

    private Dish ownDish(Restaurant restaurant, String id) {
        Dish dish = id == null || id.length() > 32 ? null : dishes.findById(id);
        if (dish == null || !dish.getRestaurantId().equals(restaurant.getId())) {
            throw new ResponseStatusException(404, "No such dish");
        }
        return dish;
    }

    private void ownPhoto(Restaurant restaurant, String id) {
        Photo photo = id.length() > 32 ? null : photos.findById(id);
        if (photo == null || !photo.getRestaurantId().equals(restaurant.getId())) {
            throw new ResponseStatusException(400, "Upload the photo first");
        }
    }

    // ---------------------------------------------------------------- photos

    @Transactional
    public PhotoDto upload(String username, PhotoDto photo) throws IOException {
        return store(mine(username).getId(), photo);
    }

    private PhotoDto store(String restaurantId, PhotoDto photo) throws IOException {
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
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(400, "The photo is not base64");
        }
        Applications.checkSize(bytes, type, "The photo is not a JPEG or a PNG");
        if (prune(restaurantId) >= MAX_PHOTOS) {
            throw new ResponseStatusException(429, "Too many photos; try again later");
        }
        Photo row = new Photo();
        row.setId(Ids.next());
        row.setRestaurantId(restaurantId);
        row.setContentType(type);
        row.setData(bytes);
        row.setCreatedAt(System.currentTimeMillis());
        photos.insert(row);
        return new PhotoDto(row.getId(), type, "");
    }

    /// Deletes the restaurant's photos that neither it nor any of its dishes
    /// names and that are past [#UNUSED_PHOTO_GRACE], answering how many
    /// rows remain.
    private int prune(String restaurantId) {
        List<String> used = new ArrayList<String>();
        Restaurant restaurant = restaurants.findById(restaurantId);
        if (restaurant != null) {
            used.add(restaurant.getPhotoId());
        }
        List<Dish> menu = dishes.findByRestaurantIdOrderByPositionAsc(restaurantId);
        for (int iter = 0; iter < menu.size(); iter++) {
            used.add(menu.get(iter).getPhotoId());
        }
        long before = System.currentTimeMillis() - UNUSED_PHOTO_GRACE;
        List<Object[]> stored = photos.stored(restaurantId);
        int remaining = 0;
        for (int iter = 0; iter < stored.size(); iter++) {
            Object id = stored.get(iter)[0];
            Object at = stored.get(iter)[1];
            long createdAt = at instanceof Number ? ((Number) at).longValue() : Long.MAX_VALUE;
            if (id instanceof String && !used.contains(id) && createdAt < before) {
                photos.deleteById((String) id);
            } else {
                remaining++;
            }
        }
        return remaining;
    }
}
