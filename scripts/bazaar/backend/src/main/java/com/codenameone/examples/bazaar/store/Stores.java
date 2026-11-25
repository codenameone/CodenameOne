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

import com.codename1.appkit.server.Text;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.crypto.PasswordEncoder;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.api.StaffDto;
import com.codenameone.examples.bazaar.api.StoreDto;
import com.codenameone.examples.bazaar.api.StoreRoleDto;
import com.codenameone.examples.bazaar.domain.InventoryLevel;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.Store;
import com.codenameone.examples.bazaar.domain.StoreStaff;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.repository.InventoryLevelRepository;
import com.codenameone.examples.bazaar.repository.InventoryMoveRepository;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.repository.StoreRepository;
import com.codenameone.examples.bazaar.repository.StoreStaffRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// Stores: opening one, what shoppers see of it, and what its staff
/// configure -- settings, locations and who works there.
@Component
public class Stores {
    /// Who single sign-on opened; field-injected, as it was added after
    /// the constructor's callers.
    @com.codename1.backend.annotations.Autowired
    private com.codenameone.examples.bazaar.staff.SsoProviders sso;
    public static final String ACTIVE = "ACTIVE";
    public static final String PENDING = "PENDING";
    public static final String SUSPENDED = "SUSPENDED";
    private static final String[] LOCATION_KINDS = {"WAREHOUSE", "RETAIL", "POPUP"};
    private static final String[] CURRENCIES = {"USD", "EUR", "GBP", "CAD", "AUD", "ILS", "JPY",
        "BHD"};

    private final StoreRepository stores;
    private final StoreStaffRepository staff;
    private final LocationRepository locations;
    private final VariantRepository variants;
    private final InventoryLevelRepository levels;
    private final InventoryMoveRepository moves;
    private final StoreAccess access;
    private final Accounts accounts;
    private final PasswordEncoder encoder;

    public Stores(StoreRepository stores, StoreStaffRepository staff,
            LocationRepository locations, VariantRepository variants,
            InventoryLevelRepository levels, InventoryMoveRepository moves, StoreAccess access,
            Accounts accounts, PasswordEncoder encoder) {
        this.stores = stores;
        this.staff = staff;
        this.locations = locations;
        this.variants = variants;
        this.levels = levels;
        this.moves = moves;
        this.access = access;
        this.accounts = accounts;
        this.encoder = encoder;
    }

    /// Opens an ACTIVE store with `owner` as its OWNER, and gives the owner
    /// the merchant role. Answers 409 when the handle is taken.
    public Store create(String owner, String name, String handle, String currency)
            throws IOException {
        Store store = insertStore(owner, name, handle, currency);
        accounts.setMerchant(owner, true);
        return store;
    }

    @Transactional(rollbackFor = IOException.class)
    private Store insertStore(String owner, String name, String handle, String currency)
            throws IOException {
        if (!accounts.exists(owner)) {
            throw new ResponseStatusException(404, "No such account");
        }
        String cleanHandle = handle(handle);
        if (stores.findByHandle(cleanHandle) != null) {
            throw new ResponseStatusException(409, "That handle is taken");
        }
        Store store = new Store();
        store.setId(Ids.next());
        store.setHandle(cleanHandle);
        store.setName(Text.required(name, 120, "the store's name"));
        store.setCurrency(Text.oneOf(currency, CURRENCIES, "USD", "the currency"));
        store.setAccentColor(0x2f6f4f);
        store.setOwnerUsername(owner);
        store.setStatus(ACTIVE);
        store.setEmail(owner);
        store.setCreatedAt(Instant.now());
        store.setUpdatedAt(Instant.now());
        stores.insert(store);
        StoreStaff member = new StoreStaff();
        member.setId(Ids.next());
        member.setStoreId(store.getId());
        member.setUsername(owner);
        member.setRole(StoreAccess.OWNER);
        member.setDisplayName(accounts.describe(owner).displayName());
        member.setCreatedAt(Instant.now());
        staff.insert(member);
        return store;
    }

    /// Every active store, by name.
    @Transactional(readOnly = true)
    public List<StoreDto> publicStores() throws IOException {
        List<Store> all = stores.findByStatusOrderByNameAsc(ACTIVE);
        List<StoreDto> out = new ArrayList<StoreDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            out.add(toDto(all.get(iter)));
        }
        return out;
    }

    /// An active store by handle; 404 for any other.
    @Transactional(readOnly = true)
    public Store activeStore(String handle) throws IOException {
        Store store = handle == null ? null : stores.findByHandle(handle);
        if (store == null || !ACTIVE.equals(store.getStatus())) {
            throw new ResponseStatusException(404, "No such store");
        }
        return store;
    }

    @Transactional(readOnly = true)
    public StoreDto publicStore(String handle) throws IOException {
        return toDto(activeStore(handle));
    }

    /// The active locations of an active store, in order.
    @Transactional(readOnly = true)
    public List<LocationDto> pickupLocations(String handle) throws IOException {
        Store store = activeStore(handle);
        List<Location> all = locations.findByStoreIdOrderByPositionAsc(store.getId());
        List<LocationDto> out = new ArrayList<LocationDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            if (all.get(iter).isActive()) {
                out.add(toDto(all.get(iter)));
            }
        }
        return out;
    }

    /// The stores `username` works for, with the role at each.
    @Transactional(readOnly = true)
    public List<StoreRoleDto> myStores(String username) throws IOException {
        List<StoreStaff> places = staff.findByUsername(username);
        List<StoreRoleDto> out = new ArrayList<StoreRoleDto>();
        for (int iter = 0; iter < places.size(); iter++) {
            Store store = stores.findById(places.get(iter).getStoreId());
            if (store != null) {
                out.add(new StoreRoleDto(toDto(store), places.get(iter).getRole()));
            }
        }
        return out;
    }

    @Transactional(readOnly = true)
    public StoreDto settings(String username, String storeId) throws IOException {
        return toDto(access.requireStore(username, storeId, StoreAccess.ANYONE));
    }

    /// Saves what the staff may change of a store; its handle, owner and
    /// status are the platform's.
    @Transactional(rollbackFor = IOException.class)
    public StoreDto saveSettings(String username, String storeId, StoreDto wanted)
            throws IOException {
        Store store = access.requireStore(username, storeId, StoreAccess.MANAGERS);
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        store.setName(Text.required(wanted.name(), 120, "the store's name"));
        store.setTagline(Text.optional(wanted.tagline(), 200, "the tagline"));
        store.setDescription(Text.optional(wanted.description(), 2000, "the description"));
        store.setCurrency(Text.oneOf(wanted.currency(), CURRENCIES, store.getCurrency(),
                "the currency"));
        store.setAccentColor(wanted.accentColor() & 0xffffff);
        store.setLogoPhotoId(Text.optional(wanted.logoPhotoId(), 32, "the logo"));
        store.setHeroPhotoId(Text.optional(wanted.heroPhotoId(), 32, "the banner"));
        store.setTaxInclusive(wanted.taxInclusive());
        store.setEmail(Text.optional(wanted.email(), 190, "the e-mail"));
        store.setPhone(Text.optional(wanted.phone(), 32, "the phone"));
        store.setAddress(Text.optional(wanted.address(), 255, "the address"));
        store.setPolicies(Text.optional(wanted.policies(), 20000, "the policies"));
        store.setUpdatedAt(Instant.now());
        stores.save(store);
        return toDto(store);
    }

    @Transactional(readOnly = true)
    public List<LocationDto> locations(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.ANYONE);
        List<Location> all = locations.findByStoreIdOrderByPositionAsc(storeId);
        List<LocationDto> out = new ArrayList<LocationDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            out.add(toDto(all.get(iter)));
        }
        return out;
    }

    /// Adds a location, with an empty level for every variant of the store.
    @Transactional(rollbackFor = IOException.class)
    public LocationDto addLocation(String username, String storeId, LocationDto wanted)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Location location = new Location();
        location.setId(Ids.next());
        location.setStoreId(storeId);
        location.setPosition((int) locations.countByStoreId(storeId));
        fill(location, wanted);
        locations.insert(location);
        List<Variant> all = variants.findByStoreId(storeId);
        List<InventoryLevel> made = new ArrayList<InventoryLevel>();
        for (int iter = 0; iter < all.size(); iter++) {
            made.add(Inventory.newLevel(storeId, all.get(iter).getId(), location.getId(), 0));
        }
        levels.saveAll(made);
        return toDto(location);
    }

    @Transactional(rollbackFor = IOException.class)
    public LocationDto saveLocation(String username, String storeId, String locationId,
            LocationDto wanted) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Location location = location(storeId, locationId);
        fill(location, wanted);
        if (wanted != null && wanted.position() >= 0) {
            location.setPosition(wanted.position());
        }
        locations.save(location);
        return toDto(location);
    }

    /// Deletes a location nothing was ever stocked at; one with a history is
    /// deactivated instead, so its ledger keeps making sense.
    @Transactional(rollbackFor = IOException.class)
    public LocationDto removeLocation(String username, String storeId, String locationId)
            throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        Location location = location(storeId, locationId);
        List<InventoryLevel> held = levels.findByLocationId(locationId);
        boolean history = false;
        for (int iter = 0; iter < held.size() && !history; iter++) {
            InventoryLevel level = held.get(iter);
            history = level.getOnHand() != 0 || level.getCommitted() != 0
                    || level.getIncoming() != 0;
        }
        history = history || moves.existsByLocationId(locationId);
        LocationDto answer;
        if (history) {
            location.setActive(false);
            location.setFulfilsOnline(false);
            locations.save(location);
            answer = toDto(location);
        } else {
            answer = toDto(location);
            levels.deleteAll(held);
            locations.delete(location);
        }
        return answer;
    }

    @Transactional(readOnly = true)
    public List<StaffDto> staff(String username, String storeId) throws IOException {
        access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        List<StoreStaff> all = staff.findByStoreIdOrderByDisplayNameAsc(storeId);
        List<StaffDto> out = new ArrayList<StaffDto>();
        for (int iter = 0; iter < all.size(); iter++) {
            out.add(toDto(all.get(iter)));
        }
        return out;
    }

    /// Adds an account to the staff or changes its place there. Only the owner
    /// makes managers; nobody changes the owner's own role.
    public StaffDto saveStaff(String username, String storeId, StaffDto wanted)
            throws IOException {
        StaffDto saved = putStaff(username, storeId, wanted);
        accounts.setMerchant(saved.username(), true);
        return saved;
    }

    @Transactional(rollbackFor = IOException.class)
    private StaffDto putStaff(String username, String storeId, StaffDto wanted)
            throws IOException {
        StoreStaff me = access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        if (wanted == null || wanted.username() == null) {
            throw new ResponseStatusException(400, "Name the account to add");
        }
        String who = Accounts.email(wanted.username());
        if (!accounts.exists(who)) {
            throw new ResponseStatusException(404, "No such account");
        }
        // An account a store's own sign-on provider opened is that store's:
        // the provider vouched for the address, nobody else did.
        if (sso.ownedElsewhere(who, storeId)) {
            throw new ResponseStatusException(409, "That account signs in through another "
                    + "store's single sign-on");
        }
        String role = Text.oneOf(wanted.role(), new String[] {StoreAccess.MANAGER,
            StoreAccess.CASHIER, StoreAccess.FULFILMENT}, "", "the role");
        if (role.length() == 0) {
            throw new ResponseStatusException(400, "The role is MANAGER, CASHIER or FULFILMENT");
        }
        StoreStaff member = staff.findByStoreIdAndUsername(storeId, who);
        if (member != null && StoreAccess.OWNER.equals(member.getRole())) {
            throw new ResponseStatusException(400, "The owner's role cannot be changed");
        }
        boolean managerTouched = StoreAccess.MANAGER.equals(role)
                || member != null && StoreAccess.MANAGER.equals(member.getRole());
        if (managerTouched && !StoreAccess.OWNER.equals(me.getRole())) {
            throw new ResponseStatusException(403, "Only the owner appoints managers");
        }
        if (member == null) {
            member = new StoreStaff();
            member.setId(Ids.next());
            member.setStoreId(storeId);
            member.setUsername(who);
            member.setCreatedAt(Instant.now());
            member.setRole(role);
            fillStaff(member, wanted, who);
            staff.insert(member);
        } else {
            member.setRole(role);
            fillStaff(member, wanted, who);
            staff.save(member);
        }
        return toDto(member);
    }

    private void fillStaff(StoreStaff member, StaffDto wanted, String who) throws IOException {
        String name = wanted.displayName();
        member.setDisplayName(name == null || name.trim().length() == 0
                ? accounts.describe(who).displayName()
                : Text.required(name, 120, "the name"));
        String locationId = wanted.locationId() == null ? "" : wanted.locationId();
        if (locationId.length() > 0) {
            location(member.getStoreId(), locationId);
        }
        member.setLocationId(locationId);
        if (wanted.pin() != null && wanted.pin().length() > 0) {
            member.setPinHash(encoder.encode(pin(wanted.pin())));
        }
    }

    /// Takes someone off the staff; the merchant role goes too once the
    /// account works for no store.
    public StaffDto removeStaff(String username, String storeId, String staffId)
            throws IOException {
        StaffDto gone = deleteStaff(username, storeId, staffId);
        if (myStores(gone.username()).isEmpty()) {
            accounts.setMerchant(gone.username(), false);
        }
        return gone;
    }

    @Transactional(rollbackFor = IOException.class)
    private StaffDto deleteStaff(String username, String storeId, String staffId)
            throws IOException {
        StoreStaff me = access.requireStaff(username, storeId, StoreAccess.MANAGERS);
        StoreStaff member = staffId == null ? null : staff.findById(staffId);
        StoreAccess.requireIn(member == null ? null : member.getStoreId(), storeId, "staff member");
        if (StoreAccess.OWNER.equals(member.getRole())) {
            throw new ResponseStatusException(400, "The owner cannot be removed");
        }
        if (StoreAccess.MANAGER.equals(member.getRole())
                && !StoreAccess.OWNER.equals(me.getRole())) {
            throw new ResponseStatusException(403, "Only the owner removes managers");
        }
        StaffDto answer = toDto(member);
        staff.delete(member);
        return answer;
    }

    /// Whether `pin` is the register PIN of `username` at the store.
    @Transactional(readOnly = true)
    public boolean checkPin(String storeId, String username, String pin) throws IOException {
        StoreStaff member = staff.findByStoreIdAndUsername(storeId, username);
        return member != null && member.getPinHash().length() > 0 && pin != null
                && encoder.matches(pin, member.getPinHash());
    }

    /// A PIN: 4 to 6 digits.
    public static String pin(String pin) {
        String clean = pin == null ? "" : pin.trim();
        if (clean.length() < 4 || clean.length() > 6) {
            throw new ResponseStatusException(400, "A PIN is 4 to 6 digits");
        }
        for (int iter = 0; iter < clean.length(); iter++) {
            char c = clean.charAt(iter);
            if (c < '0' || c > '9') {
                throw new ResponseStatusException(400, "A PIN is 4 to 6 digits");
            }
        }
        return clean;
    }

    /// A handle: 2 to 64 lower-case letters, digits and dashes. Upper-case
    /// ASCII letters are folded; anything else is refused.
    public static String handle(String handle) {
        String clean = handle == null ? "" : handle.trim();
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < clean.length(); iter++) {
            char c = clean.charAt(iter);
            if (c >= 'A' && c <= 'Z') {
                c = (char) (c + ('a' - 'A'));
            }
            boolean ok = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '-';
            if (!ok) {
                throw new ResponseStatusException(400,
                        "A handle is letters, digits and dashes");
            }
            out.append(c);
        }
        if (out.length() < 2 || out.length() > 64) {
            throw new ResponseStatusException(400, "A handle is 2 to 64 characters");
        }
        return out.toString();
    }

    private Location location(String storeId, String locationId) {
        Location location = locationId == null ? null : locations.findById(locationId);
        StoreAccess.requireIn(location == null ? null : location.getStoreId(), storeId,
                "location");
        return location;
    }

    private static void fill(Location location, LocationDto wanted) {
        if (wanted == null) {
            throw new ResponseStatusException(400, "Nothing to save");
        }
        location.setName(Text.required(wanted.name(), 120, "the location's name"));
        location.setKind(Text.oneOf(wanted.kind(), LOCATION_KINDS, "RETAIL", "the kind"));
        location.setAddress(Text.optional(wanted.address(), 255, "the address"));
        location.setFulfilsOnline(wanted.fulfilsOnline());
        location.setActive(wanted.active());
    }

    public static StoreDto toDto(Store store) {
        return new StoreDto(store.getId(), store.getHandle(), store.getName(), store.getTagline(),
                store.getDescription(), store.getCurrency(), store.getAccentColor(),
                store.getLogoPhotoId(), store.getHeroPhotoId(), store.getOwnerUsername(),
                store.getStatus(), store.isTaxInclusive(), store.getEmail(), store.getPhone(),
                store.getAddress(), store.getPolicies());
    }

    public static LocationDto toDto(Location location) {
        return new LocationDto(location.getId(), location.getName(), location.getKind(),
                location.getAddress(), location.isFulfilsOnline(), location.isActive(),
                location.getPosition());
    }

    public static StaffDto toDto(StoreStaff member) {
        return new StaffDto(member.getId(), member.getUsername(), member.getDisplayName(),
                member.getRole(), member.getLocationId(), "", member.getPinHash().length() > 0);
    }
}
