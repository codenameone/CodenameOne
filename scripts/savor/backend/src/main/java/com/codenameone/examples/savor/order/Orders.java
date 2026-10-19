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
package com.codenameone.examples.savor.order;

import com.codename1.backend.ConcurrencyFailureException;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codename1.backend.security.core.userdetails.JdbcUserDetailsManager;
import com.codename1.backend.security.core.userdetails.UsernameNotFoundException;
import com.codename1.orm.repository.PageRequest;
import com.codename1.orm.session.Identifier;
import com.codename1.orm.session.PersistenceException;
import com.codenameone.examples.savor.Ids;
import com.codenameone.examples.savor.Text;
import com.codenameone.examples.savor.account.Moderation;
import com.codenameone.examples.savor.api.CartDto;
import com.codenameone.examples.savor.api.DeliveryDto;
import com.codenameone.examples.savor.api.OrderDto;
import com.codenameone.examples.savor.api.OrderLineDto;
import com.codenameone.examples.savor.api.OrderState;
import com.codenameone.examples.savor.api.QuoteDto;
import com.codenameone.examples.savor.delivery.Deliveries;
import com.codenameone.examples.savor.domain.CustomerOrder;
import com.codenameone.examples.savor.domain.OrderLine;
import com.codenameone.examples.savor.domain.Profile;
import com.codenameone.examples.savor.domain.PromoUse;
import com.codenameone.examples.savor.domain.Restaurant;
import com.codenameone.examples.savor.domain.RestaurantStaff;
import com.codenameone.examples.savor.pay.Payments;
import com.codenameone.examples.savor.repository.CustomerOrderRepository;
import com.codenameone.examples.savor.repository.OrderLineRepository;
import com.codenameone.examples.savor.repository.ProfileRepository;
import com.codenameone.examples.savor.repository.PromoUseRepository;
import com.codenameone.examples.savor.repository.RestaurantRepository;
import com.codenameone.examples.savor.repository.RestaurantStaffRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Orders: placing one, the kitchen's moves, and how each party sees it.
///
/// As in [Deliveries], every change is a conditional update
/// ([OrderStateMachine]) inside a private transaction, and the public method
/// tells whoever is concerned once it has committed. The card is held before
/// the order is written and let go if the order ends without arriving; both
/// calls to the processor happen with no transaction open.
@Component
public class Orders {
    /// How many orders one customer may have on the way at once.
    static final int MAX_ACTIVE = 3;
    private static final OrderState[] ACTIVE = {OrderState.PLACED, OrderState.ACCEPTED,
        OrderState.READY, OrderState.PICKED_UP};
    private static final OrderState[] KITCHEN = {OrderState.PLACED, OrderState.ACCEPTED,
        OrderState.READY};

    private final CustomerOrderRepository orders;
    private final OrderLineRepository lines;
    private final RestaurantRepository restaurants;
    private final RestaurantStaffRepository staff;
    private final PromoUseRepository promoUses;
    private final ProfileRepository profiles;
    private final Quotes quotes;
    private final OrderEvents events;
    private final Deliveries deliveries;
    private final Payments payments;
    private final Moderation moderation;
    private final OrderNews news;
    private final JdbcUserDetailsManager users;
    private final long answerMillis;

    /// @param answerSeconds how long a kitchen has to accept an order before
    ///     it is turned down for them
    public Orders(CustomerOrderRepository orders, OrderLineRepository lines,
            RestaurantRepository restaurants, RestaurantStaffRepository staff,
            PromoUseRepository promoUses, ProfileRepository profiles, Quotes quotes,
            OrderEvents events, Deliveries deliveries, Payments payments, OrderNews news,
            Moderation moderation, JdbcUserDetailsManager users,
            @Value("${savor.kitchen.answerSeconds:300}") int answerSeconds) {
        this.orders = orders;
        this.lines = lines;
        this.restaurants = restaurants;
        this.staff = staff;
        this.promoUses = promoUses;
        this.profiles = profiles;
        this.quotes = quotes;
        this.events = events;
        this.deliveries = deliveries;
        this.payments = payments;
        this.moderation = moderation;
        this.news = news;
        this.users = users;
        answerMillis = answerSeconds * 1000L;
    }

    private boolean enabled(String username) {
        try {
            return users.loadUserByUsername(username).isEnabled();
        } catch (UsernameNotFoundException gone) {
            return false;
        }
    }

    // -------------------------------------------------------------- customer

    public QuoteDto quote(String customer, CartDto cart) throws IOException {
        return quotes.quote(customer, cart).quote;
    }

    // tag::savor-place[]
    /// Places an order: prices it from the menu, holds the total on the card,
    /// and writes it. A hold whose order could not be written is let go.
    public OrderDto place(String customer, CartDto cart) throws IOException {
        Quotes.Priced priced = quotes.quote(customer, cart);
        Restaurant restaurant = priced.restaurant;
        if (!Quotes.open(restaurant, quotes.minuteOfDay(System.currentTimeMillis()))) {
            throw new ResponseStatusException(409, restaurant.getName()
                    + " is not taking orders right now");
        }
        if (priced.quote.subtotalCents() < restaurant.getMinimumCents()) {
            throw new ResponseStatusException(400, restaurant.getName()
                    + " needs a bigger order");
        }
        if (cart.promoCode() != null && cart.promoCode().trim().length() > 0
                && priced.promo == null) {
            throw new ResponseStatusException(400, priced.quote.promoMessage());
        }
        // The same refusals store() makes under its locks, asked first without
        // them so an order that could never be accepted puts no hold on the
        // card and costs no call to the payment processor. store() still asks
        // again: two orders placed at once both pass here.
        eligible(customer);
        String[] method = payments.choose(customer, cart.paymentMethodId());
        String id = Ids.next();
        String hold = payments.authorize(id, customer, method[0], priced.quote.totalCents(),
                priced.quote.currency());
        try {
            attempt(() -> store(id, customer, cart, priced, method, hold));
        } catch (IOException | RuntimeException failed) {
            // Nothing was written, so the hold is let go by its reference.
            payments.releaseHold(id, hold);
            throw failed;
        }
        CustomerOrder order = find(id);
        tell(order);
        return view(customer, false, id);
    }

    /// The refusals [#store] makes, without its locks. See [#place].
    @Transactional(readOnly = true)
    private Void eligible(String customer) {
        if (!enabled(customer)) {
            throw new ResponseStatusException(403, "This account is blocked");
        }
        if (orders.countByCustomerAndStateIn(customer, ACTIVE) >= MAX_ACTIVE) {
            throw new ResponseStatusException(409, "You already have " + MAX_ACTIVE
                    + " orders on the way");
        }
        Profile profile = profiles.findById(customer);
        if (profile == null || !profile.isPhoneVerified()) {
            throw new ResponseStatusException(403, "Verify your phone number before you order");
        }
        return null;
    }

    @Transactional
    private Void store(String id, String customer, CartDto cart, Quotes.Priced priced,
            String[] method, String hold) throws IOException {
        // Held first, so two orders placed at once count each other and not
        // just what had committed before either began.
        profiles.hold(customer);
        // The card was held with no transaction open, and that can take a
        // while: the account may have been blocked, or the restaurant paused
        // or suspended, since the checks before it. Asked again under the
        // locks those changes take, a refusal here lets the hold go.
        if (!enabled(customer)) {
            throw new ResponseStatusException(403, "This account is blocked");
        }
        Restaurant restaurant = restaurants.hold(priced.restaurant.getId()) == 0 ? null
                : restaurants.findById(priced.restaurant.getId());
        if (restaurant == null
                || !Quotes.open(restaurant, quotes.minuteOfDay(System.currentTimeMillis()))) {
            throw new ResponseStatusException(409, priced.restaurant.getName()
                    + " is not taking orders right now");
        }
        if (orders.countByCustomerAndStateIn(customer, ACTIVE) >= MAX_ACTIVE) {
            throw new ResponseStatusException(409, "You already have " + MAX_ACTIVE
                    + " orders on the way");
        }
        Profile profile = profiles.findById(customer);
        if (profile == null || !profile.isPhoneVerified()) {
            // The courier calls this number from the door.
            throw new ResponseStatusException(403, "Verify your phone number before you order");
        }
        QuoteDto quote = priced.quote;
        long now = System.currentTimeMillis();
        CustomerOrder order = new CustomerOrder();
        order.setId(id);
        order.setCustomer(customer);
        order.setRestaurantId(priced.restaurant.getId());
        order.setRestaurantName(priced.restaurant.getName());
        order.setState(OrderState.PLACED);
        order.setSubtotalCents(quote.subtotalCents());
        order.setDeliveryFeeCents(quote.deliveryFeeCents());
        order.setServiceFeeCents(quote.serviceFeeCents());
        order.setSmallOrderCents(quote.smallOrderCents());
        order.setTaxCents(quote.taxCents());
        order.setDiscountCents(quote.discountCents());
        order.setTipCents(quote.tipCents());
        order.setTotalCents(quote.totalCents());
        order.setCommissionCents(priced.commissionCents);
        order.setCurrency(quote.currency());
        order.setPromoCode(priced.promo == null ? "" : priced.promo.getCode());
        order.setDropoffLat(cart.dropoffLat());
        order.setDropoffLng(cart.dropoffLng());
        order.setDropoffAddress(Text.required(cart.dropoffAddress(), 255, "the address"));
        order.setNote(Text.optional(cart.note(), 255, "the note"));
        order.setMethodId(method[0]);
        order.setMethodLabel(method[1]);
        order.setAuthorizationRef(hold);
        order.setPaymentStatus(Payments.CASH.equals(hold) ? Payments.PENDING : Payments.AUTHORIZED);
        order.setPrepMinutes(priced.restaurant.getPrepMinutes());
        order.setPlacedAt(now);
        order.setUpdatedAt(now);
        order.setReason("");
        orders.insert(order);
        for (int iter = 0; iter < priced.lines.size(); iter++) {
            OrderLineDto dto = priced.lines.get(iter);
            OrderLine line = new OrderLine();
            line.setId(Ids.next());
            line.setOrderId(id);
            line.setDishId(dto.dishId());
            line.setName(dto.name());
            line.setOptions(dto.options());
            line.setQuantity(dto.quantity());
            line.setUnitCents(dto.unitCents());
            line.setLineCents(dto.lineCents());
            line.setPosition(iter);
            lines.insert(line);
        }
        if (priced.promo != null) {
            // Keyed by code and account, so a second order racing this one
            // with the same code fails its insert and is rolled back.
            if (promoUses.existsById(Identifier.of(priced.promo.getCode(), customer))) {
                throw new ResponseStatusException(409, "You have already used that code");
            }
            PromoUse use = new PromoUse();
            use.setCode(priced.promo.getCode());
            use.setUsername(customer);
            use.setOrderId(id);
            promoUses.insert(use);
        }
        events.record(id, OrderState.PLACED, customer);
        return null;
    }
    // end::savor-place[]

    @Transactional(readOnly = true)
    public List<OrderDto> active(String customer) throws IOException {
        return list(orders.findByCustomerAndStateInOrderByPlacedAtDesc(customer, ACTIVE),
                customer, false);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> history(String customer) throws IOException {
        List<CustomerOrder> rows = orders.findTop50ByCustomerOrderByPlacedAtDesc(customer);
        List<CustomerOrder> ended = new ArrayList<CustomerOrder>();
        for (int iter = 0; iter < rows.size(); iter++) {
            if (!rows.get(iter).getState().isActive()) {
                ended.add(rows.get(iter));
            }
        }
        return list(ended, customer, false);
    }

    /// An order, for its customer, its restaurant's staff, its courier, or an
    /// admin; 404 for anyone else.
    @Transactional(readOnly = true)
    public OrderDto get(String viewer, boolean admin, String id) throws IOException {
        CustomerOrder order = find(id);
        if (order == null || !(admin || mayView(viewer, order))) {
            throw new ResponseStatusException(404, "No such order");
        }
        return toDto(order, viewer, !admin && viewer.equals(order.getCustomer()), admin,
                new HashMap<String, Restaurant>());
    }

    private boolean mayView(String viewer, CustomerOrder order) throws IOException {
        if (viewer.equals(order.getCustomer())) {
            return true;
        }
        if (staff.existsById(Identifier.of(order.getRestaurantId(), viewer))) {
            return true;
        }
        DeliveryDto delivery = deliveries.forOrder(order.getId(), viewer, false);
        return delivery != null && viewer.equals(delivery.courierUsername());
    }

    /// Cancels an order the kitchen has not accepted yet, and lets the hold go.
    public OrderDto cancel(String customer, String id) throws IOException {
        CustomerOrder order = owned(customer, id);
        attempt(() -> end(id, OrderState.CANCELLED_BY_CUSTOMER, customer,
                "Cancelled by the customer"));
        payments.release(id);
        tell(find(id));
        return view(customer, false, id);
    }

    /// The order, its delivery and the customer's record, as one step: an
    /// order that ends with its courier still riding to it would be a delivery
    /// nobody pays for.
    @Transactional
    private Void adminEnd(String by, String id, String why, Deliveries.Change[] change)
            throws IOException {
        CustomerOrder order = find(id);
        if (order == null) {
            throw new ResponseStatusException(404, "No such order");
        }
        end(id, OrderState.CANCELLED_BY_ADMIN, by, why);
        change[0] = deliveries.endForOrder(id, by);
        // On the customer's record, where the admin looking them up next sees it.
        moderation.record(order.getCustomer(), by, Moderation.CANCEL_ORDER, why);
        return null;
    }

    @Transactional
    private Void end(String id, OrderState to, String actor, String reason) throws IOException {
        if (orders.end(id, OrderStateMachine.sources(to), to, reason,
                System.currentTimeMillis()) != 1) {
            CustomerOrder now = find(id);
            throw new ResponseStatusException(409, "That order is "
                    + (now == null ? "gone" : describe(now.getState())));
        }
        events.record(id, to, actor);
        return null;
    }

    /// The customer's stars for a delivered order, once; they count toward
    /// the restaurant's rating and its courier's.
    public OrderDto rate(String customer, String id, int stars) throws IOException {
        if (stars < 1 || stars > 5) {
            throw new ResponseStatusException(400, "Give between one and five stars");
        }
        attempt(() -> rated(customer, id, stars));
        return view(customer, false, id);
    }

    @Transactional
    private Void rated(String customer, String id, int stars) throws IOException {
        CustomerOrder order = owned(customer, id);
        if (OrderState.DELIVERED != order.getState()) {
            throw new ResponseStatusException(409, "Only a delivered order can be rated");
        }
        if (orders.rate(id, customer, stars, System.currentTimeMillis()) != 1) {
            throw new ResponseStatusException(409, "That order is already rated");
        }
        restaurants.addRating(order.getRestaurantId(), stars);
        deliveries.rate(id, customer, stars);
        return null;
    }

    // --------------------------------------------------------------- kitchen

    /// The orders a restaurant has to deal with, oldest first.
    @Transactional(readOnly = true)
    public List<OrderDto> queue(String restaurantId, String viewer) throws IOException {
        return list(orders.findByRestaurantIdAndStateInOrderByPlacedAtAsc(restaurantId, KITCHEN),
                viewer, false);
    }

    @Transactional(readOnly = true)
    public List<OrderDto> kitchenHistory(String restaurantId, String viewer) throws IOException {
        List<CustomerOrder> rows = orders.findTop50ByRestaurantIdOrderByPlacedAtDesc(restaurantId);
        List<CustomerOrder> out = new ArrayList<CustomerOrder>();
        for (int iter = 0; iter < rows.size(); iter++) {
            if (!inKitchen(rows.get(iter).getState())) {
                out.add(rows.get(iter));
            }
        }
        return list(out, viewer, false);
    }

    private static boolean inKitchen(OrderState state) {
        return OrderState.PLACED == state || OrderState.ACCEPTED == state
                || OrderState.READY == state;
    }

    // tag::savor-accept[]
    /// The kitchen takes an order: it says how long it needs, and the order's
    /// delivery starts looking for a courier in the same transaction.
    public OrderDto accept(String actor, String restaurantId, String id, int prepMinutes)
            throws IOException {
        if (prepMinutes < 1 || prepMinutes > 180) {
            throw new ResponseStatusException(400, "Say how many minutes it needs");
        }
        attempt(() -> accepted(actor, restaurantId, id, prepMinutes));
        tell(find(id));
        // Once committed, as an offer is always made: a courier asked from
        // inside the transaction could be shown an order that then rolled back.
        deliveries.search(id);
        return view(actor, false, id);
    }

    @Transactional
    private Void accepted(String actor, String restaurantId, String id, int prepMinutes)
            throws IOException {
        CustomerOrder order = kitchens(restaurantId, id);
        if (orders.accept(id, OrderStateMachine.sources(OrderState.ACCEPTED),
                OrderState.ACCEPTED, prepMinutes, System.currentTimeMillis()) != 1) {
            throw new ResponseStatusException(409, "That order is "
                    + describe(order.getState()));
        }
        events.record(id, OrderState.ACCEPTED, actor);
        deliveries.dispatch(find(id), restaurants.findById(restaurantId));
        return null;
    }
    // end::savor-accept[]

    public OrderDto reject(String actor, String restaurantId, String id, String reason)
            throws IOException {
        String why = Text.required(reason, 255, "why the order is turned down");
        kitchensReadOnly(restaurantId, id);
        attempt(() -> end(id, OrderState.REJECTED, actor, why));
        payments.release(id);
        tell(find(id));
        return view(actor, false, id);
    }

    public OrderDto ready(String actor, String restaurantId, String id) throws IOException {
        attempt(() -> readied(actor, restaurantId, id));
        tell(find(id));
        return view(actor, false, id);
    }

    @Transactional
    private Void readied(String actor, String restaurantId, String id) throws IOException {
        CustomerOrder order = kitchens(restaurantId, id);
        if (orders.ready(id, OrderStateMachine.sources(OrderState.READY), OrderState.READY,
                System.currentTimeMillis()) != 1) {
            throw new ResponseStatusException(409, "That order is "
                    + describe(order.getState()));
        }
        events.record(id, OrderState.READY, actor);
        return null;
    }

    /// Turns down, for the kitchen, the orders nobody accepted in time, and
    /// lets their holds go. A customer is not left waiting on a closed
    /// kitchen.
    @Scheduled(fixedDelay = 15000, initialDelay = 15000, lock = "savor-kitchen")
    public void sweep() throws IOException {
        List<CustomerOrder> late = late(System.currentTimeMillis() - answerMillis);
        for (int iter = 0; iter < late.size(); iter++) {
            String id = late.get(iter).getId();
            try {
                attempt(() -> end(id, OrderState.REJECTED, "",
                        "The restaurant did not answer in time"));
            } catch (ResponseStatusException moved) {
                // Accepted in the meantime.
                continue;
            }
            payments.release(id);
            tell(find(id));
        }
    }

    @Transactional(readOnly = true)
    private List<CustomerOrder> late(long before) {
        return orders.findByStateAndPlacedAtLessThan(OrderState.PLACED, before);
    }

    // ----------------------------------------------------------------- admin

    /// Ends an order that has not arrived, whatever it was doing, lets the
    /// hold go and takes its delivery off the road.
    public OrderDto cancelByAdmin(String by, String id, String reason) throws IOException {
        String why = Text.required(reason, 255, "why the order is cancelled");
        Deliveries.Change[] change = new Deliveries.Change[1];
        attempt(() -> adminEnd(by, id, why, change));
        payments.release(id);
        deliveries.told(change[0]);
        tell(find(id));
        return get(by, true, id);
    }

    /// The most recent orders, for the admin.
    @Transactional(readOnly = true)
    public List<OrderDto> recent() throws IOException {
        return list(orders.newest(new PageRequest(0, 100)), "", true);
    }

    // ------------------------------------------------------------- internals

    /// Tells the order's people it changed, once the change has committed.
    private void tell(CustomerOrder order) {
        if (order != null) {
            news.told(order.getId());
        }
    }

    private interface Work {
        Void run() throws IOException;
    }

    /// Runs a transaction again when the database ended it because another
    /// changed the same order first; see `Deliveries.attempt`.
    private static void attempt(Work work) throws IOException {
        for (int tries = 1; ; tries++) {
            try {
                work.run();
                return;
            } catch (PersistenceException err) {
                if (tries == 3 || !ConcurrencyFailureException.isCauseOf(err)) {
                    throw err;
                }
            } catch (ConcurrencyFailureException err) {
                if (tries == 3) {
                    throw err;
                }
            }
        }
    }

    @Transactional(readOnly = true)
    private CustomerOrder find(String id) {
        if (id == null || id.length() == 0 || id.length() > 32) {
            return null;
        }
        return orders.findById(id);
    }

    @Transactional(readOnly = true)
    private CustomerOrder owned(String customer, String id) {
        CustomerOrder order = find(id);
        if (order == null || !customer.equals(order.getCustomer())) {
            throw new ResponseStatusException(404, "No such order");
        }
        return order;
    }

    private CustomerOrder kitchens(String restaurantId, String id) {
        CustomerOrder order = find(id);
        if (order == null || !order.getRestaurantId().equals(restaurantId)) {
            throw new ResponseStatusException(404, "No such order");
        }
        return order;
    }

    @Transactional(readOnly = true)
    private CustomerOrder kitchensReadOnly(String restaurantId, String id) {
        return kitchens(restaurantId, id);
    }

    @Transactional(readOnly = true)
    private OrderDto view(String viewer, boolean admin, String id) throws IOException {
        CustomerOrder order = find(id);
        return toDto(order, viewer, viewer.equals(order.getCustomer()), admin,
                new HashMap<String, Restaurant>());
    }

    private List<OrderDto> list(List<CustomerOrder> rows, String viewer, boolean admin)
            throws IOException {
        Map<String, Restaurant> cache = new HashMap<String, Restaurant>();
        List<OrderDto> out = new ArrayList<OrderDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            CustomerOrder order = rows.get(iter);
            out.add(toDto(order, viewer, viewer.equals(order.getCustomer()), admin, cache));
        }
        return out;
    }

    /// `admin` is whether the order is read for an admin, the only reader its
    /// delivery shows the customer's sign-in name to; its restaurant's staff
    /// and its courier are parties, and see what a party sees.
    private OrderDto toDto(CustomerOrder order, String viewer, boolean customer, boolean admin,
            Map<String, Restaurant> cache) throws IOException {
        Restaurant restaurant = cache.get(order.getRestaurantId());
        if (restaurant == null) {
            restaurant = restaurants.findById(order.getRestaurantId());
            if (restaurant != null) {
                cache.put(restaurant.getId(), restaurant);
            }
        }
        List<OrderLine> rows = lines.findByOrderIdOrderByPositionAsc(order.getId());
        List<OrderLineDto> items = new ArrayList<OrderLineDto>();
        for (int iter = 0; iter < rows.size(); iter++) {
            OrderLine line = rows.get(iter);
            items.add(new OrderLineDto(line.getDishId(), line.getName(), line.getOptions(),
                    line.getQuantity(), line.getUnitCents(), line.getLineCents()));
        }
        Profile who = profiles.findById(order.getCustomer());
        DeliveryDto delivery = deliveries.forOrder(order.getId(), viewer, !admin);
        int eta = 0;
        OrderState state = order.getState();
        if (state.isActive() && restaurant != null) {
            int ride = Quotes.travelMinutes(Quotes.roadMeters(restaurant.getLat(),
                    restaurant.getLng(), order.getDropoffLat(), order.getDropoffLng()));
            if (OrderState.PICKED_UP == state && delivery != null && delivery.etaMinutes() > 0) {
                eta = delivery.etaMinutes();
            } else {
                long cooked = (order.getAcceptedAt() == 0L ? order.getPlacedAt()
                        : order.getAcceptedAt()) + order.getPrepMinutes() * 60000L;
                int left = (int) Math.max(0L, (cooked - System.currentTimeMillis()) / 60000L);
                eta = left + ride;
            }
        }
        return new OrderDto(order.getId(), state, order.getRestaurantId(),
                order.getRestaurantName(),
                restaurant == null ? "" : restaurant.getAddress(),
                restaurant == null ? 0d : restaurant.getLat(),
                restaurant == null ? 0d : restaurant.getLng(),
                customer ? order.getCustomer() : null,
                who == null ? "" : who.getDisplayName(),
                items, order.getSubtotalCents(), order.getDeliveryFeeCents(),
                order.getServiceFeeCents(), order.getSmallOrderCents(), order.getTaxCents(),
                order.getDiscountCents(), order.getTipCents(), order.getTotalCents(),
                order.getCurrency(), order.getDropoffLat(), order.getDropoffLng(),
                order.getDropoffAddress(), order.getNote(), order.getMethodLabel(),
                order.getPaymentStatus(), order.getPrepMinutes(), order.getPlacedAt(),
                order.getAcceptedAt(), order.getReadyAt(), order.getDeliveredAt(), eta,
                order.getReason(), order.getStars(), delivery);
    }

    private static String describe(OrderState state) {
        if (OrderState.DELIVERED == state) {
            return "already delivered";
        }
        if (OrderState.PICKED_UP == state) {
            return "already on its way";
        }
        if (OrderState.ACCEPTED == state || OrderState.READY == state) {
            return "already being prepared";
        }
        if (state.isCancelled() || OrderState.REJECTED == state
                || OrderState.NO_COURIERS == state) {
            return "no longer going ahead";
        }
        return "not ready for that";
    }
}
