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
package com.codenameone.examples.skirmish.shop;

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.skirmish.Clock;
import com.codenameone.examples.skirmish.Ids;
import com.codenameone.examples.skirmish.Text;
import com.codenameone.examples.skirmish.account.Lists;
import com.codenameone.examples.skirmish.api.ReceiptDto;
import com.codenameone.examples.skirmish.api.ShopItemDto;
import com.codenameone.examples.skirmish.api.WalletDto;
import com.codenameone.examples.skirmish.domain.Profile;
import com.codenameone.examples.skirmish.domain.StorePurchase;
import com.codenameone.examples.skirmish.repository.ProfileRepository;
import com.codenameone.examples.skirmish.repository.StorePurchaseRepository;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/// The wallet: coins (earned by winning and by rewarded ads), gems (bought) and
/// the cosmetic items they unlock.
// tag::skirmish-shop[]
@Component
public class Shop {
    public static final int AD_REWARDS_PER_DAY = 3;
    public static final int AD_REWARD_COINS = 25;

    private final ProfileRepository profiles;
    private final StorePurchaseRepository purchases;
    private final ReceiptValidator validator;

    public Shop(ProfileRepository profiles, StorePurchaseRepository purchases, ReceiptValidator validator) {
        this.profiles = profiles;
        this.purchases = purchases;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public WalletDto wallet(String username) {
        return toDto(profile(username));
    }

    /// Spends coins or gems on a cosmetic.
    @Transactional(rollbackFor = IOException.class)
    public WalletDto buy(String username, String itemId) {
        ShopItemDto item = Catalog.item(itemId);
        if (item == null || item.sku().length() > 0) {
            throw new ResponseStatusException(404, "That is not for sale for coins or gems");
        }
        hold(username);
        Profile p = profile(username);
        if (Lists.contains(p.getOwned(), item.id())) {
            throw new ResponseStatusException(409, "You already own it");
        }
        if (p.getCoins() < item.priceCoins() || p.getGems() < item.priceGems()) {
            throw new ResponseStatusException(402, "Not enough coins or gems");
        }
        p.setCoins(p.getCoins() - item.priceCoins());
        p.setGems(p.getGems() - item.priceGems());
        grant(p, item.id());
        profiles.save(p);
        return toDto(p);
    }

    @Transactional(rollbackFor = IOException.class)
    public WalletDto equip(String username, String arena) {
        Profile p = profile(username);
        boolean owned = false;
        for (String id : Lists.split(p.getOwned())) {
            String unlocked = Catalog.arenaOf(id);
            owned |= unlocked != null && unlocked.equals(arena);
        }
        if (!owned) {
            throw new ResponseStatusException(403, "You do not own that arena");
        }
        p.setArena(arena);
        profiles.save(p);
        return toDto(p);
    }

    /// Grants a store purchase once its receipt checks out. The store's
    /// transaction id is the row's key, so replaying a receipt grants nothing.
    public WalletDto receipt(String username, ReceiptDto receipt) throws IOException {
        if (receipt == null) {
            throw new ResponseStatusException(400, "No receipt");
        }
        String store = Text.oneOf(receipt.store(), new String[] {"itunes", "play", "simulator"}, "",
                "the store");
        ShopItemDto item = Catalog.bySku(receipt.sku());
        if (item == null) {
            throw new ResponseStatusException(404, "Unknown product");
        }
        String transaction = validator.verify(store, receipt.sku(),
                Text.optional(receipt.transactionId(), 190, "the transaction"), receipt.receipt());
        if (transaction == null) {
            throw new ResponseStatusException(402, "The store did not confirm this purchase");
        }
        return redeem(username, store, item, transaction);
    }

    @Transactional(rollbackFor = IOException.class)
    WalletDto redeem(String username, String store, ShopItemDto item, String transaction) throws IOException {
        hold(username);
        Profile p = profile(username);
        String key = Ids.sha256(transaction);
        StorePurchase seen = purchases.findById(key);
        if (seen != null) {
            if (!seen.getUsername().equals(username)) {
                throw new ResponseStatusException(409, "That purchase belongs to another account");
            }
            return toDto(p);
        }
        StorePurchase row = new StorePurchase();
        row.setId(key);
        row.setUsername(username);
        row.setStore(store);
        row.setSku(item.sku());
        int gems = Catalog.gems(item);
        row.setGems(gems);
        row.setCreatedAt(Instant.now());
        purchases.insert(row);
        if (gems > 0) {
            p.setGems(p.getGems() + gems);
        } else {
            grant(p, item.id());
        }
        profiles.save(p);
        return toDto(p);
    }

    /// The reward for a watched rewarded ad: a few coins, a few times a day.
    ///
    /// The cap is the guard here. In production, have the ad network call the
    /// server directly (AdMob's server-side verification) and grant from that
    /// callback, so not even a modified app can claim a reward it did not earn.
    @Transactional(rollbackFor = IOException.class)
    public WalletDto adReward(String username) {
        hold(username);
        Profile p = profile(username);
        long today = Clock.dayOf(Clock.now());
        if (p.getAdDay() != today) {
            p.setAdDay(today);
            p.setAdCount(0);
        }
        if (p.getAdCount() >= AD_REWARDS_PER_DAY) {
            throw new ResponseStatusException(429, "That is all the rewards for today");
        }
        p.setAdCount(p.getAdCount() + 1);
        p.setCoins(p.getCoins() + AD_REWARD_COINS);
        profiles.save(p);
        return toDto(p);
    }

    @Transactional(readOnly = true)
    public List<String> purchasesOf(String username) {
        List<String> out = new ArrayList<String>();
        for (StorePurchase s : purchases.of(username)) {
            out.add(s.getStore() + " " + s.getSku() + " " + s.getCreatedAt());
        }
        return out;
    }

    private static void grant(Profile p, String itemId) {
        if (!Lists.contains(p.getOwned(), itemId)) {
            p.setOwned(p.getOwned().length() == 0 ? itemId : p.getOwned() + "," + itemId);
        }
    }

    private void hold(String username) {
        if (profiles.hold(username) == 0) {
            throw new ResponseStatusException(404, "No such account");
        }
    }

    private Profile profile(String username) {
        Profile p = profiles.findById(username);
        if (p == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        return p;
    }

    static WalletDto toDto(Profile p) {
        int left = p.getAdDay() == Clock.dayOf(Clock.now()) ? AD_REWARDS_PER_DAY - p.getAdCount() : AD_REWARDS_PER_DAY;
        return new WalletDto(p.getCoins(), p.getGems(), Lists.split(p.getOwned()), p.getArena(), left);
    }
}
// end::skirmish-shop[]
