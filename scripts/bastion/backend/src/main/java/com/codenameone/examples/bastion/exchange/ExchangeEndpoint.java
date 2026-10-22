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
package com.codenameone.examples.bastion.exchange;

import com.codenameone.examples.bastion.Refused;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bastion.api.BookDto;
import com.codenameone.examples.bastion.api.CryptoAddressDto;
import com.codenameone.examples.bastion.api.CryptoWithdrawalDto;
import com.codenameone.examples.bastion.api.ExchangeApiServer;
import com.codenameone.examples.bastion.api.LevelDto;
import com.codenameone.examples.bastion.api.MarketDto;
import com.codenameone.examples.bastion.api.NewCryptoAddressDto;
import com.codenameone.examples.bastion.api.NewOrderDto;
import com.codenameone.examples.bastion.api.OrderDto;
import com.codenameone.examples.bastion.api.ReceiptDto;
import com.codenameone.examples.bastion.api.TickerDto;
import com.codenameone.examples.bastion.api.TradeDto;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.CryptoAddress;
import com.codenameone.examples.bastion.domain.Device;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.domain.Market;
import com.codenameone.examples.bastion.domain.Trade;
import com.codenameone.examples.bastion.domain.TradeOrder;
import com.codenameone.examples.bastion.ledger.AccountKinds;
import com.codenameone.examples.bastion.ledger.Ledger;
import com.codenameone.examples.bastion.money.Devices;
import com.codenameone.examples.bastion.money.Money;
import com.codenameone.examples.bastion.money.RequestSigning;
import com.codenameone.examples.bastion.repository.TradeOrderRepository;
import com.codenameone.examples.bastion.repository.TradeRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/// [com.codenameone.examples.bastion.api.ExchangeApi], served.
///
/// The markets, books and tickers are public to any signed-in caller and
/// name nobody: a book is quantities at prices, never an order id or an
/// owner. Orders, trades, addresses and withdrawals are the caller's own,
/// looked up through them, so another customer's id answers 404.
@Component
public class ExchangeEndpoint implements ExchangeApiServer {
    /// Price levels shown on each side of a book.
    static final int DEPTH = 20;

    private final Markets markets;
    private final Exchange exchange;
    private final CryptoWallets wallets;
    private final Ledger ledger;
    private final Money money;
    private final RequestSigning signing;
    private final Audit audit;
    private final TradeOrderRepository orders;
    private final TradeRepository trades;

    public ExchangeEndpoint(Markets markets, Exchange exchange, CryptoWallets wallets,
            Ledger ledger, Money money, RequestSigning signing, Audit audit,
            TradeOrderRepository orders, TradeRepository trades) {
        this.markets = markets;
        this.exchange = exchange;
        this.wallets = wallets;
        this.ledger = ledger;
        this.money = money;
        this.signing = signing;
        this.audit = audit;
        this.orders = orders;
        this.trades = trades;
    }

    @Override
    public List<MarketDto> markets() throws Exception {
        List<MarketDto> out = new ArrayList<MarketDto>();
        for (Market m : markets.all()) {
            out.add(new MarketDto(m.getId(), m.getBase(), m.getQuote(), m.getLot(),
                    m.getIndexPrice(), m.getLastPrice()));
        }
        return out;
    }

    @Override
    public BookDto book(String market) throws Exception {
        Market m = markets.get(market);
        List<TradeOrder> open = openOrders(m.getId());
        return new BookDto(m.getId(), levels(open, Exchange.BUY), levels(open, Exchange.SELL));
    }

    @Override
    public TickerDto ticker(String market) throws Exception {
        Market m = markets.get(market);
        List<TradeOrder> open = openOrders(m.getId());
        List<LevelDto> bids = levels(open, Exchange.BUY);
        List<LevelDto> asks = levels(open, Exchange.SELL);
        return new TickerDto(m.getId(), m.getIndexPrice(), m.getLastPrice(),
                bids.isEmpty() ? 0L : bids.get(0).price(), asks.isEmpty() ? 0L : asks.get(0).price(),
                System.currentTimeMillis());
    }

    @Transactional(readOnly = true)
    private List<TradeOrder> openOrders(String market) throws IOException {
        return orders.findByMarketAndStatus(market, Exchange.OPEN);
    }

    /// One side of a book: open quantity summed per price, best first.
    static List<LevelDto> levels(List<TradeOrder> open, final String side) {
        List<TradeOrder> mine = new ArrayList<TradeOrder>();
        for (TradeOrder o : open) {
            if (o.getSide().equals(side)) {
                mine.add(o);
            }
        }
        final boolean bids = Exchange.BUY.equals(side);
        Collections.sort(mine, new Comparator<TradeOrder>() {
            @Override
            public int compare(TradeOrder a, TradeOrder b) {
                int byPrice = Long.compare(a.getPrice(), b.getPrice());
                return bids ? -byPrice : byPrice;
            }
        });
        List<LevelDto> out = new ArrayList<LevelDto>();
        long price = -1;
        long quantity = 0;
        int count = 0;
        for (TradeOrder o : mine) {
            if (o.getPrice() != price) {
                if (count > 0) {
                    out.add(new LevelDto(price, quantity, count));
                    if (out.size() == DEPTH) {
                        return out;
                    }
                }
                price = o.getPrice();
                quantity = 0;
                count = 0;
            }
            quantity += o.getQuantity() - o.getFilled();
            count++;
        }
        if (count > 0) {
            out.add(new LevelDto(price, quantity, count));
        }
        return out;
    }

    @Override
    public OrderDto place(String idempotencyKey, NewOrderDto order) throws Exception {
        return exchange.place(Devices.customer(), idempotencyKey, order);
    }

    @Override
    public OrderDto cancel(String id) throws Exception {
        return exchange.cancel(Devices.customer(), id);
    }

    @Override
    public List<OrderDto> openOrders() throws Exception {
        String owner = Devices.customer();
        List<OrderDto> out = new ArrayList<OrderDto>();
        for (TradeOrder o : mine(owner)) {
            out.add(Exchange.dto(o));
        }
        return out;
    }

    @Transactional(readOnly = true)
    private List<TradeOrder> mine(String owner) throws IOException {
        return orders.findByOwnerAndStatusOrderByCreatedAtDesc(owner, Exchange.OPEN);
    }

    @Override
    public List<TradeDto> trades() throws Exception {
        String owner = Devices.customer();
        List<TradeDto> out = new ArrayList<TradeDto>();
        for (Trade t : tradesOf(owner)) {
            boolean bought = t.getBuyer().equals(owner);
            String side = bought ? Exchange.BUY : Exchange.SELL;
            boolean taker = side.equals(t.getTakerSide());
            out.add(new TradeDto(t.getId(), t.getMarket(), side, taker, t.getPrice(),
                    t.getQuantity(), bought ? t.getBuyerPaid() : t.getSellerGot(),
                    taker ? t.getFee() : 0L, t.getCreatedAt()));
        }
        return out;
    }

    @Transactional(readOnly = true)
    private List<Trade> tradesOf(String owner) throws IOException {
        return trades.of(owner);
    }

    @Override
    public List<CryptoAddressDto> addresses() throws Exception {
        return wallets.list(Devices.customer());
    }

    @Override
    public CryptoAddressDto addAddress(String signature, NewCryptoAddressDto address)
            throws Exception {
        String owner = Devices.customer();
        Device device = signing.require(owner);
        money.requireTrusted(device);
        return wallets.add(owner, address);
    }

    @Override
    public CryptoAddressDto removeAddress(String id) throws Exception {
        return wallets.remove(Devices.customer(), id);
    }

    /// Coins out of the bank: always signed by a trusted device, only to an
    /// allow-listed address of the same coin past its cooling-off, and held
    /// to the crypto limits ([Money]). A refusal is audited here, outside the
    /// movement's transaction, which would roll the record back with it.
    @Override
    public ReceiptDto withdrawCrypto(String id, String idempotencyKey, String signature,
            CryptoWithdrawalDto withdrawal) throws Exception {
        String owner = Devices.customer();
        LedgerAccount from = ledger.owned(owner, id);
        if (!AccountKinds.WALLET.equals(from.getKind())) {
            throw Refused.with(400, "coins-withdrawn-wallet", "Coins are withdrawn from a wallet");
        }
        Device device = signing.require(owner);
        money.requireTrusted(device);
        if (withdrawal == null) {
            throw Refused.with(400, "send-address-amount", "Send the address and the amount");
        }
        CryptoAddress address;
        try {
            address = wallets.owned(owner, withdrawal.addressId());
        } catch (ResponseStatusException unknown) {
            audit.append(owner, "CRYPTO_OUT_REFUSED", from.getId(), "address not allow-listed");
            throw Refused.with(403, "withdraw-address-list", "Withdraw only to an address on your list");
        }
        if (!address.getAsset().equals(from.getAsset())) {
            throw Refused.with(400, "address-wrong-asset", "That address is for {0}", address.getAsset());
        }
        if (System.currentTimeMillis() < address.getUsableAt()) {
            audit.append(owner, "CRYPTO_OUT_REFUSED", address.getId(), "cooling off until "
                    + address.getUsableAt());
            throw Refused.with(409, "address-new-used", "That address is new: it can be used from {0}", address.getUsableAt());
        }
        try {
            return money.withdrawCrypto(owner, idempotencyKey, from, address,
                    withdrawal.amount());
        } catch (ResponseStatusException refused) {
            if (refused.getStatus() == 409) {
                audit.append(owner, "CRYPTO_OUT_REFUSED", address.getId(), refused.getReason());
            }
            throw refused;
        }
    }
}
