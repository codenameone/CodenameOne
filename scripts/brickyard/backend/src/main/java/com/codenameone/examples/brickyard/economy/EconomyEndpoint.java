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
package com.codenameone.examples.brickyard.economy;

import com.codename1.appkit.server.Caller;
import com.codename1.backend.annotations.Component;
import com.codenameone.examples.brickyard.api.CatalogItemDto;
import com.codenameone.examples.brickyard.api.EconomyApiServer;
import com.codenameone.examples.brickyard.api.OwnershipDto;
import com.codenameone.examples.brickyard.api.PayoutDto;
import com.codenameone.examples.brickyard.api.PlayReportDto;
import com.codenameone.examples.brickyard.api.PlusDto;
import com.codenameone.examples.brickyard.api.ProductDto;
import com.codenameone.examples.brickyard.api.ReceiptDto;
import com.codenameone.examples.brickyard.api.WalletDto;
import java.util.List;

/// The economy routes; the work is in [Economy] and [Wallet].
@Component
public class EconomyEndpoint implements EconomyApiServer {
    private final Economy economy;
    private final Wallet wallet;

    public EconomyEndpoint(Economy economy, Wallet wallet) {
        this.economy = economy;
        this.wallet = wallet;
    }

    @Override
    public WalletDto wallet() throws Exception {
        return wallet.describe(Caller.name());
    }

    @Override
    public WalletDto redeemReceipt(ReceiptDto receipt) throws Exception {
        return economy.redeem(Caller.name(), receipt);
    }

    @Override
    public ProductDto createProduct(String experienceId, ProductDto product) throws Exception {
        return economy.createProduct(experienceId, product);
    }

    @Override
    public List<ProductDto> products(String experienceId) throws Exception {
        return economy.products(experienceId);
    }

    @Override
    public OwnershipDto buyProduct(String productId) throws Exception {
        return economy.buyProduct(Caller.name(), productId);
    }

    @Override
    public OwnershipDto owns(String username, String productId) throws Exception {
        return economy.owns(username, productId);
    }

    @Override
    public List<CatalogItemDto> catalog() throws Exception {
        return economy.catalog();
    }

    @Override
    public OwnershipDto buyItem(String itemId) throws Exception {
        return economy.buyItem(Caller.name(), itemId);
    }

    @Override
    public List<OwnershipDto> inventory() throws Exception {
        return economy.inventory(Caller.name());
    }

    @Override
    public PlusDto plus() throws Exception {
        return economy.plusOf(Caller.name());
    }

    @Override
    public PlayReportDto reportPlay(PlayReportDto report) throws Exception {
        return economy.reportPlay(report);
    }

    @Override
    public PayoutDto requestPayout(PayoutDto payout) throws Exception {
        return economy.requestPayout(Caller.name(), payout);
    }
}
