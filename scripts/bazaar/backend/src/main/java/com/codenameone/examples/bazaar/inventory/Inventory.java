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
package com.codenameone.examples.bazaar.inventory;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bazaar.Ids;
import com.codenameone.examples.bazaar.domain.InventoryLevel;
import com.codenameone.examples.bazaar.domain.InventoryMove;
import com.codenameone.examples.bazaar.domain.Location;
import com.codenameone.examples.bazaar.domain.Variant;
import com.codenameone.examples.bazaar.repository.InventoryLevelRepository;
import com.codenameone.examples.bazaar.repository.InventoryMoveRepository;
import com.codenameone.examples.bazaar.repository.LocationRepository;
import com.codenameone.examples.bazaar.repository.VariantRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/// The stock ledger: the one place a stock level changes.
///
/// A level ([InventoryLevel], one per variant and location) holds `onHand`,
/// `committed` (promised to orders not yet shipped) and `incoming` (on its way
/// from a supplier or another location). `available = onHand - committed`.
/// Every change to `onHand` or `committed` writes an [InventoryMove] in the
/// same transaction, so a level always equals the sum of its moves (`incoming`
/// is a forecast and has no moves).
///
/// Levels are never loaded, changed and saved: two checkouts doing that at
/// once would both see the last unit and both take it. Every change is one
/// relative `update` statement, and a reservation is conditional on the row's
/// `version` and on the units being there, retried when another transaction
/// moved the row first. Those statements flush and clear the session, so a
/// caller that loaded entities before calling in here must read them again
/// before changing them.
///
/// A variant that does not track inventory has no stock to keep: the calls
/// below answer as if there were always enough and write nothing.
///
/// Each method is a transaction of its own, or joins the caller's.
@Component
public class Inventory {
    public static final String SALE = "SALE";
    public static final String RESERVE = "RESERVE";
    public static final String RELEASE = "RELEASE";
    public static final String RETURN = "RETURN";
    public static final String RESTOCK = "RESTOCK";
    public static final String COUNT = "COUNT";
    public static final String DAMAGE = "DAMAGE";
    public static final String TRANSFER_OUT = "TRANSFER_OUT";
    public static final String TRANSFER_IN = "TRANSFER_IN";
    public static final String RECEIVE = "RECEIVE";
    public static final String CORRECTION = "CORRECTION";
    public static final String POS_SYNC = "POS_SYNC";

    /// How many times a reservation is tried against a row other checkouts
    /// keep moving. Each failed try means another one succeeded.
    private static final int ATTEMPTS = 200;

    private final InventoryLevelRepository levels;
    private final InventoryMoveRepository moves;
    private final VariantRepository variants;
    private final LocationRepository locations;

    public Inventory(InventoryLevelRepository levels, InventoryMoveRepository moves,
            VariantRepository variants, LocationRepository locations) {
        this.levels = levels;
        this.moves = moves;
        this.variants = variants;
        this.locations = locations;
    }

    /// Promises `qty` units at a location to an order. Answers false, changing
    /// nothing, when fewer are available -- unless the variant allows
    /// backorders, when the units are promised anyway and available goes
    /// negative. Concurrent reservations never promise the same unit twice.
    // tag::bazaar-reserve[]
    @Transactional(rollbackFor = IOException.class)
    public boolean reserve(String storeId, String variantId, String locationId, int qty,
            String reference, String username) throws IOException {
        Variant variant = variant(storeId, variantId);
        location(storeId, locationId);
        positive(qty);
        if (!variant.isTrackInventory()) {
            return true;
        }
        if (variant.isAllowBackorder()) {
            change(storeId, variantId, locationId, 0, qty, 0, RESERVE, reference, username);
            return true;
        }
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            InventoryLevel level = level(storeId, variantId, locationId);
            if (level.getOnHand() - level.getCommitted() < qty) {
                return false;
            }
            if (levels.reserveIf(level.getId(), level.getVersion(), qty, Instant.now()) == 1) {
                record(storeId, variantId, locationId, 0, qty, RESERVE, reference, username);
                return true;
            }
            // Another transaction moved the row first. The update cleared the
            // session, so the next read is of the row as it is now.
        }
        return false;
    }
    // end::bazaar-reserve[]

    /// Takes back a promise [#reserve] made: an order cancelled or edited.
    @Transactional(rollbackFor = IOException.class)
    public void release(String storeId, String variantId, String locationId, int qty,
            String reference, String username) throws IOException {
        if (tracked(storeId, variantId, locationId, qty)) {
            change(storeId, variantId, locationId, 0, -qty, 0, RELEASE, reference, username);
        }
    }

    /// Units promised to an order leave the building: on hand and committed
    /// both drop by `qty`.
    @Transactional(rollbackFor = IOException.class)
    public void ship(String storeId, String variantId, String locationId, int qty,
            String reference, String username) throws IOException {
        if (tracked(storeId, variantId, locationId, qty)) {
            change(storeId, variantId, locationId, -qty, -qty, 0, SALE, reference, username);
        }
    }

    /// Units come back on hand: a return ([#RETURN]) or stock found or put
    /// back ([#RESTOCK]).
    ///
    /// @param reason RETURN or RESTOCK
    @Transactional(rollbackFor = IOException.class)
    public void restock(String storeId, String variantId, String locationId, int qty,
            String reference, String username, String reason) throws IOException {
        if (!RETURN.equals(reason) && !RESTOCK.equals(reason)) {
            throw new ResponseStatusException(400, "A restock is a RETURN or a RESTOCK");
        }
        if (tracked(storeId, variantId, locationId, qty)) {
            change(storeId, variantId, locationId, qty, 0, 0, reason, reference, username);
        }
    }

    /// A sale a register made, possibly offline and long ago. Never refused
    /// for want of stock: the goods are already gone, so on hand drops and
    /// may go below zero, which the low-stock list then shows.
    @Transactional(rollbackFor = IOException.class)
    public void recordPosSale(String storeId, String variantId, String locationId, int qty,
            String reference, String username) throws IOException {
        if (tracked(storeId, variantId, locationId, qty)) {
            change(storeId, variantId, locationId, -qty, 0, 0, POS_SYNC, reference, username);
        }
    }

    /// Where an online order for `qty` units should ship from: the first
    /// active location, in the store's order, that fulfils online orders and
    /// has them available; failing that the first that fulfils online at all.
    /// Null when the store has no such location.
    @Transactional(readOnly = true)
    public String pickLocation(String storeId, String variantId, int qty) throws IOException {
        Variant variant = variant(storeId, variantId);
        String fallback = null;
        List<Location> all = locations.findByStoreIdOrderByPositionAsc(storeId);
        for (int iter = 0; iter < all.size(); iter++) {
            Location location = all.get(iter);
            if (!location.isActive() || !location.isFulfilsOnline()) {
                continue;
            }
            if (fallback == null) {
                fallback = location.getId();
            }
            if (!variant.isTrackInventory() || available(variantId, location.getId()) >= qty) {
                return location.getId();
            }
        }
        return fallback;
    }

    /// Units of a variant available at a location; 0 where it has no level.
    @Transactional(readOnly = true)
    public int available(String variantId, String locationId) throws IOException {
        InventoryLevel level = levels.findByVariantIdAndLocationId(variantId, locationId);
        return level == null ? 0 : level.getOnHand() - level.getCommitted();
    }

    /// Sets on hand to a counted figure and records the difference as a
    /// COUNT move. Answers the difference.
    @Transactional(rollbackFor = IOException.class)
    public int count(String storeId, String variantId, String locationId, int counted,
            String reference, String username) throws IOException {
        variant(storeId, variantId);
        location(storeId, locationId);
        if (counted < 0) {
            throw new ResponseStatusException(400, "A count cannot be below zero");
        }
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            InventoryLevel level = level(storeId, variantId, locationId);
            int delta = counted - level.getOnHand();
            if (delta == 0) {
                return 0;
            }
            if (levels.setOnHandIf(level.getId(), level.getVersion(), counted,
                    Instant.now()) == 1) {
                record(storeId, variantId, locationId, delta, 0, COUNT, reference, username);
                return delta;
            }
        }
        throw new ResponseStatusException(409, "The stock kept changing; count again");
    }

    /// Any change at all, for the stock screens, transfers and purchase
    /// orders: the deltas are applied as they are. A move is written when on
    /// hand or committed changes; `incoming` alone writes none.
    @Transactional(rollbackFor = IOException.class)
    public void change(String storeId, String variantId, String locationId, int onHand,
            int committed, int incoming, String reason, String reference, String username)
            throws IOException {
        InventoryLevel level = level(storeId, variantId, locationId);
        levels.apply(level.getId(), onHand, committed, incoming, Instant.now());
        if (onHand != 0 || committed != 0) {
            record(storeId, variantId, locationId, onHand, committed, reason, reference, username);
        }
    }

    /// The level of a variant at a location, made empty when there is none
    /// yet. Levels are made when variants and locations are, so this rarely
    /// writes.
    @Transactional(rollbackFor = IOException.class)
    public InventoryLevel level(String storeId, String variantId, String locationId)
            throws IOException {
        InventoryLevel level = levels.findByVariantIdAndLocationId(variantId, locationId);
        if (level == null) {
            level = newLevel(storeId, variantId, locationId, 0);
            levels.insert(level);
        }
        return level;
    }

    /// An unsaved level holding `onHand` units, for code making many at once.
    public static InventoryLevel newLevel(String storeId, String variantId, String locationId,
            int onHand) throws IOException {
        InventoryLevel level = new InventoryLevel();
        level.setId(Ids.next());
        level.setStoreId(storeId);
        level.setVariantId(variantId);
        level.setLocationId(locationId);
        level.setOnHand(onHand);
        level.setUpdatedAt(Instant.now());
        return level;
    }

    /// An unsaved ledger line, for code making many at once.
    public static InventoryMove newMove(String storeId, String variantId, String locationId,
            int onHand, int committed, String reason, String reference, String username)
            throws IOException {
        InventoryMove move = new InventoryMove();
        move.setId(Ids.next());
        move.setStoreId(storeId);
        move.setVariantId(variantId);
        move.setLocationId(locationId);
        move.setOnHandDelta(onHand);
        move.setCommittedDelta(committed);
        move.setReason(reason);
        move.setReference(clip(reference, 64));
        move.setUsername(clip(username, 190));
        move.setCreatedAt(Instant.now());
        return move;
    }

    private void record(String storeId, String variantId, String locationId, int onHand,
            int committed, String reason, String reference, String username) throws IOException {
        moves.insert(newMove(storeId, variantId, locationId, onHand, committed, reason,
                reference, username));
    }

    private boolean tracked(String storeId, String variantId, String locationId, int qty)
            throws IOException {
        Variant variant = variant(storeId, variantId);
        location(storeId, locationId);
        positive(qty);
        return variant.isTrackInventory();
    }

    private Variant variant(String storeId, String variantId) {
        Variant variant = variantId == null ? null : variants.findById(variantId);
        if (variant == null || !variant.getStoreId().equals(storeId)) {
            throw new ResponseStatusException(404, "No such variant");
        }
        return variant;
    }

    private Location location(String storeId, String locationId) {
        Location location = locationId == null ? null : locations.findById(locationId);
        if (location == null || !location.getStoreId().equals(storeId)) {
            throw new ResponseStatusException(404, "No such location");
        }
        return location;
    }

    private static void positive(int qty) {
        if (qty <= 0) {
            throw new ResponseStatusException(400, "A quantity is at least 1");
        }
    }

    private static String clip(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max) : text;
    }
}
