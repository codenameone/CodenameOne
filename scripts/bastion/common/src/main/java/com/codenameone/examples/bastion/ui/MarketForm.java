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
package com.codenameone.examples.bastion.ui;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.bastion.api.BookDto;
import com.codenameone.examples.bastion.api.LevelDto;
import com.codenameone.examples.bastion.api.MarketDto;
import com.codenameone.examples.bastion.api.NewOrderDto;
import com.codenameone.examples.bastion.api.OrderDto;
import com.codenameone.examples.bastion.api.TickerDto;
import com.codenameone.examples.bastion.api.TradeDto;
import com.codenameone.examples.bastion.money.Amounts;
import com.codenameone.examples.bastion.money.Assets;
import com.codenameone.examples.bastion.net.Api;
import com.codenameone.examples.bastion.net.Net;
import com.codenameone.examples.bastion.security.RequestSigner;
import java.util.List;

/// One market: the ticker, the order book, an order ticket, and the user's
/// open orders in it.
///
/// Prices are quote units per **one whole** base unit (cents per bitcoin);
/// quantities are base units (satoshi) and must be a whole number of the
/// market's lot. The ticket reads both as the user types them, in the
/// asset's own decimals, and refuses a quantity that is not whole lots before
/// the server has to.
///
/// The four shapes of order are the server's:
///
/// - a **limit** buy or sell names a price and a quantity, and rests on the
///   book for whatever does not fill at once;
/// - a **market buy** names only a budget in the quote asset, and buys whole
///   lots from the best asks while the budget lasts;
/// - a **market sell** names only a quantity, sold into the best bids.
///
/// An order is placed with an idempotency key, so a retry after a lost
/// answer cannot place it twice. It needs no device signature: an order
/// moves money only between the user's own accounts, at a price on the book.
///
/// The book and the orders refresh every few seconds while the screen is
/// up; the timer belongs to the form and stops with it.
public final class MarketForm {
    private static final int DEPTH = 6;
    private static final int REFRESH_MILLIS = 4000;

    private MarketForm() {
    }

    public static void show(final MarketDto market, final Runnable back) {
        Form form = Nav.form(market.base() + " / " + market.quote(), back);
        final Label ticker = new Label(" ", "BnHeroAmount");
        final Label spread = new Label(" ", "BnHeroNote");
        Container hero = new Container(BoxLayout.y(), "BnHero");
        hero.addAll(new Label(Lang.tr("Last price"), "BnHeroLabel"), ticker, spread);
        form.add(hero);
        final Container book = new Container(BoxLayout.y());
        final Container orders = new Container(BoxLayout.y());
        form.add(Ui.section("Order book"));
        form.add(book);
        form.add(Ui.section("Place an order"));
        form.add(ticket(market, () -> refresh(market, ticker, spread, book, orders)));
        form.add(Ui.section("Your open orders"));
        form.add(orders);
        Ui.show(form, true);
        Runnable tick = () -> refresh(market, ticker, spread, book, orders);
        tick.run();
        UITimer.timer(REFRESH_MILLIS, true, form, tick);
    }

    private static void refresh(final MarketDto market, final Label ticker, final Label spread,
            final Container book, final Container orders) {
        Net.Failed quiet = (status, message) -> { };
        Api.exchange().ticker(market.id(), Net.to((TickerDto t) -> {
            long last = t.lastPrice() > 0 ? t.lastPrice() : t.indexPrice();
            ticker.setText(Amounts.format(last, market.quote()));
            spread.setText(Lang.tr("Bid {0} - Ask {1}", price(t.bestBid(), market),
                    price(t.bestAsk(), market)));
            ticker.getParent().revalidate();
        }, quiet));
        Api.exchange().book(market.id(), Net.to((BookDto b) -> fillBook(book, b, market), quiet));
        Api.exchange().openOrders(Net.to((List<OrderDto> open) -> fillOrders(orders, open, market,
                () -> refresh(market, ticker, spread, book, orders)), quiet));
    }

    private static String price(long units, MarketDto market) {
        return units > 0 ? Amounts.format(units, market.quote()) : "-";
    }

    /// Asks on top, highest first down to the best; bids under, best first --
    /// the spread sits in the middle where the eye goes.
    private static void fillBook(Container book, BookDto b, MarketDto market) {
        book.removeAll();
        List<LevelDto> asks = b.asks();
        List<LevelDto> bids = b.bids();
        if (asks.isEmpty() && bids.isEmpty()) {
            book.add(Ui.empty("The book is empty."));
        }
        int askCount = Math.min(DEPTH, asks.size());
        for (int iter = askCount - 1; iter >= 0; iter--) {
            book.add(level(asks.get(iter), market, "BnAsk"));
        }
        for (int iter = 0; iter < Math.min(DEPTH, bids.size()); iter++) {
            book.add(level(bids.get(iter), market, "BnBid"));
        }
        book.revalidate();
    }

    private static Container level(LevelDto level, MarketDto market, String uiid) {
        Container row = new Container(new GridLayout(1, 2), "BnBookRow");
        row.add(new Label(Amounts.format(level.price(), market.quote()), uiid));
        Label qty = new Label(Amounts.format(level.quantity(), market.base()), "BnBookQty");
        row.add(qty);
        return row;
    }

    private static void fillOrders(final Container orders, List<OrderDto> open,
            final MarketDto market, final Runnable changed) {
        orders.removeAll();
        boolean any = false;
        for (final OrderDto order : open) {
            if (!order.market().equals(market.id())) {
                continue;
            }
            any = true;
            String what = Lang.tr("BUY".equals(order.side()) ? "Buy" : "Sell") + " "
                    + Amounts.format(order.quantity(), market.base());
            String detail = Lang.tr("at {0}, {1} filled", price(order.price(), market),
                    Amounts.format(order.filled(), market.base()));
            Container row = new Container(new BorderLayout(), "BnRow");
            Container words = new Container(BoxLayout.y());
            words.addAll(new Label(what, "BnRowTitle"), new Label(detail, "BnRowDetail"));
            row.add(BorderLayout.CENTER, words);
            final Button cancel = new Button(Lang.tr("Cancel"), "BnLink");
            cancel.addActionListener(evt -> {
                cancel.setEnabled(false);
                Api.exchange().cancel(order.id(), Net.to((OrderDto done) -> changed.run(),
                        (status, message) -> {
                            cancel.setEnabled(true);
                            Ui.toast(message);
                        }));
            });
            row.add(BorderLayout.EAST, cancel);
            orders.add(row);
        }
        if (!any) {
            orders.add(Ui.empty("No open orders in this market."));
        }
        orders.revalidate();
    }

    private static Container ticket(final MarketDto market, final Runnable placed) {
        final Container ticket = new Container(BoxLayout.y(), "BnPanel");
        final boolean[] buy = {true};
        final boolean[] limit = {true};
        final TextField price = Ui.field(Lang.tr("Price in {0}", market.quote()), TextField.DECIMAL);
        final TextField quantity = Ui.field(Lang.tr("Amount of {0}", market.base()), TextField.DECIMAL);
        final TextField budget = Ui.field(Lang.tr("Spend, in {0}", market.quote()), TextField.DECIMAL);
        final Label hint = new Label(Lang.tr("In steps of {0}",
                Amounts.format(market.lot(), market.base())), "BnMuted");
        final Container fields = new Container(BoxLayout.y());
        final Runnable layout = () -> {
            fields.removeAll();
            if (limit[0]) {
                fields.addAll(price, quantity, hint);
            } else if (buy[0]) {
                fields.add(budget);
            } else {
                fields.addAll(quantity, hint);
            }
            fields.revalidate();
        };
        ticket.add(Ui.segments(new String[] {"Buy", "Sell"}, 0, index -> {
            buy[0] = index == 0;
            layout.run();
        }));
        ticket.add(Ui.segments(new String[] {"Limit", "Market"}, 0, index -> {
            limit[0] = index == 0;
            layout.run();
        }));
        final Label error = Ui.error();
        final Button place = Ui.primary("Place order");
        ticket.addAll(fields, error, place);
        final NewOrderDto[] tried = new NewOrderDto[1];
        final String[] key = new String[1];
        place.addActionListener(evt -> {
            Ui.showError(error, null);
            NewOrderDto order = order(market, buy[0], limit[0], price, quantity, budget, error);
            if (order == null) {
                return;
            }
            if (!Ui.confirm("Confirm order", describe(order, market), "Place")) {
                return;
            }
            if (tried[0] == null || !tried[0].equals(order)) {
                tried[0] = order;
                key[0] = RequestSigner.idempotencyKey();
            }
            place.setEnabled(false);
            // Signed by this device, as a payment is: an order moves value.
            final String k = key[0];
            DeviceSetup.withDevice(retry -> Api.exchange().place(k,
                    RequestSigner.sign("POST", "/api/exchange/orders", k, order), order,
                    Net.to((OrderDto done) -> {
                        place.setEnabled(true);
                        tried[0] = null;
                        quantity.setText("");
                        budget.setText("");
                        Ui.toast(Lang.tr("Order {0}", status(done.status())));
                        placed.run();
                    }, retry)), (status, message) -> {
                        place.setEnabled(true);
                        Ui.showError(error, Lang.tr(message));
                    });
        });
        return ticket;
    }

    /// The order the ticket describes, or null with the reason shown.
    static NewOrderDto order(MarketDto market, boolean buy, boolean limit, TextField price,
            TextField quantity, TextField budget, Label error) {
        String side = buy ? "BUY" : "SELL";
        if (!limit && buy) {
            long spend = Amounts.parse(budget.getText(), market.quote());
            if (spend <= 0) {
                Ui.showError(error, Lang.tr("Enter how much to spend."));
                return null;
            }
            return new NewOrderDto(market.id(), side, "MARKET", 0, 0, spend);
        }
        long qty = Amounts.parse(quantity.getText(), market.base());
        if (qty <= 0 || qty % market.lot() != 0) {
            Ui.showError(error, Lang.tr("Enter an amount in steps of {0}.",
                    Amounts.format(market.lot(), market.base())));
            return null;
        }
        if (!limit) {
            return new NewOrderDto(market.id(), side, "MARKET", 0, qty, 0);
        }
        long at = Amounts.parse(price.getText(), market.quote());
        if (at <= 0) {
            Ui.showError(error, Lang.tr("Enter a price."));
            return null;
        }
        return new NewOrderDto(market.id(), side, "LIMIT", at, qty, 0);
    }

    private static String describe(NewOrderDto order, MarketDto market) {
        if (order.budget() > 0) {
            return Lang.tr("Buy {0} at the best prices, spending up to {1}?", market.base(),
                    Amounts.format(order.budget(), market.quote()));
        }
        String amount = Amounts.format(order.quantity(), market.base());
        if (order.price() == 0) {
            return Lang.tr("Sell {0} at the best prices?", amount);
        }
        // What the order is worth at its own price, rounded down: an estimate,
        // since the fee and the fills decide the rest.
        String worth = Amounts.format(Amounts.value(order.quantity(),
                Assets.decimals(market.base()), order.price()), market.quote());
        return Lang.tr("BUY".equals(order.side()) ? "Buy {0} at {1} (about {2})?"
                : "Sell {0} at {1} (about {2})?", amount,
                Amounts.format(order.price(), market.quote()), worth);
    }

    private static String status(String status) {
        if ("FILLED".equals(status)) {
            return Lang.tr("filled");
        }
        if ("OPEN".equals(status)) {
            return Lang.tr("on the book");
        }
        if ("CANCELLED".equals(status)) {
            // A market order whose remainder found no one to trade with.
            return Lang.tr("ended, the rest unfilled");
        }
        return status;
    }

    /// A trade of the user's, for the exchange's front page.
    static Container tradeRow(TradeDto trade, List<MarketDto> markets) {
        String base = trade.market();
        String quote = "USD";
        for (MarketDto market : markets) {
            if (market.id().equals(trade.market())) {
                base = market.base();
                quote = market.quote();
            }
        }
        boolean bought = "BUY".equals(trade.side());
        String title = Lang.tr(bought ? "Bought {0}" : "Sold {0}",
                Amounts.format(trade.quantity(), base));
        String detail = Lang.tr("at {0}, fee {1}", Amounts.format(trade.price(), quote),
                Amounts.format(trade.fee(), quote)) + " - " + Ui.when(trade.createdAt());
        return Ui.row(title, detail, (bought ? "-" : "+") + Amounts.format(trade.quote(), quote),
                bought ? "BnAmountOut" : "BnAmountIn", null);
    }
}
