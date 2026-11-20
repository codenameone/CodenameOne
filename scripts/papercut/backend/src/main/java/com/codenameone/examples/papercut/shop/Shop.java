/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * Use of this source code is governed by the license in the LICENSE file beside
 * this project's pom.xml.
 */
package com.codenameone.examples.papercut.shop;

import com.codename1.appkit.server.Text;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.papercut.Ids;
import com.codenameone.examples.papercut.account.Lists;
import com.codenameone.examples.papercut.api.AdRewardDto;
import com.codenameone.examples.papercut.api.ReceiptDto;
import com.codenameone.examples.papercut.api.ShopItemDto;
import com.codenameone.examples.papercut.api.WalletDto;
import com.codenameone.examples.papercut.domain.AdNonce;
import com.codenameone.examples.papercut.domain.Profile;
import com.codenameone.examples.papercut.domain.StorePurchase;
import com.codenameone.examples.papercut.repository.AdNonceRepository;
import com.codenameone.examples.papercut.repository.ProfileRepository;
import com.codenameone.examples.papercut.repository.StorePurchaseRepository;
import java.io.IOException;
import java.time.Instant;

/// The wallet. Every change takes the profile row's lock first, so two taps
/// on "buy" cannot both spend the same coins.
@Component
public class Shop {
    /// What one watched rewarded ad pays.
    public static final int AD_REWARD = 15;

    private final ProfileRepository profiles;
    private final StorePurchaseRepository purchases;
    private final AdNonceRepository nonces;
    private final ReceiptValidator validator;

    public Shop(ProfileRepository profiles, StorePurchaseRepository purchases, AdNonceRepository nonces,
            ReceiptValidator validator) {
        this.profiles = profiles;
        this.purchases = purchases;
        this.nonces = nonces;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public WalletDto wallet(String username) {
        return toDto(profile(username));
    }

    /// Buys a paper (once) or a pack of spare erasers (any number of times).
    @Transactional(rollbackFor = IOException.class)
    public WalletDto buy(String username, String itemId) throws IOException {
        ShopItemDto item = Catalog.byId(itemId == null ? "" : itemId);
        if (item == null || Catalog.COINS.equals(item.kind())) {
            // Coin packs cost real money and only come through a receipt.
            throw new ResponseStatusException(404, "No such item");
        }
        hold(username);
        Profile p = profile(username);
        if (Catalog.QUILL.equals(item.kind()) && Lists.contains(p.getOwned(), item.id())) {
            throw new ResponseStatusException(409, "You already own that paper");
        }
        if (p.getInkCoins() < item.priceCoins()) {
            throw new ResponseStatusException(402, "Not enough ink coins");
        }
        p.setInkCoins(p.getInkCoins() - item.priceCoins());
        if (Catalog.QUILL.equals(item.kind())) {
            p.setOwned(p.getOwned().length() == 0 ? item.id() : p.getOwned() + "," + item.id());
        } else {
            p.setErasers(p.getErasers() + Catalog.ERASERS_PER_PACK);
        }
        profiles.save(p);
        return toDto(p);
    }

    @Transactional(rollbackFor = IOException.class)
    public WalletDto equip(String username, String quill) throws IOException {
        hold(username);
        Profile p = profile(username);
        if (quill == null || !Lists.contains(p.getOwned(), quill)) {
            throw new ResponseStatusException(403, "You do not own that paper");
        }
        p.setQuill(quill);
        profiles.save(p);
        return toDto(p);
    }

    /// Spends every spare eraser, at the start of a run that takes them.
    @Transactional(rollbackFor = IOException.class)
    public WalletDto useErasers(String username) throws IOException {
        hold(username);
        Profile p = profile(username);
        p.setErasers(0);
        profiles.save(p);
        return toDto(p);
    }

    /// Grants a store purchase once its receipt checks out. The key is the
    /// hash of the store's transaction id, so replaying a receipt -- the app
    /// retrying after a lost answer -- grants nothing a second time.
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
        int coins = Catalog.coins(item);
        row.setCoins(coins);
        row.setCreatedAt(Instant.now());
        purchases.insert(row);
        p.setInkCoins(p.getInkCoins() + coins);
        profiles.save(p);
        return toDto(p);
    }

    /// Pays for a watched rewarded ad. The nonce comes from the ad network's
    /// callback; each pays once, which is all a sample can promise without a
    /// server-to-server verification with a real network.
    @Transactional(rollbackFor = IOException.class)
    public WalletDto adReward(String username, AdRewardDto reward) throws IOException {
        String nonce = Text.required(reward == null ? null : reward.nonce(), 190, "the ad nonce");
        hold(username);
        Profile p = profile(username);
        String key = Ids.sha256(nonce);
        if (nonces.findById(key) != null) {
            throw new ResponseStatusException(409, "That ad was already rewarded");
        }
        AdNonce row = new AdNonce();
        row.setId(key);
        row.setUsername(username);
        row.setCreatedAt(Instant.now());
        nonces.insert(row);
        p.setInkCoins(p.getInkCoins() + AD_REWARD);
        profiles.save(p);
        return toDto(p);
    }

    /// The wallet as the app sees it.
    public static WalletDto toDto(Profile p) {
        return new WalletDto(p.getInkCoins(), p.getErasers(), Lists.split(p.getOwned()), p.getQuill());
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
}
