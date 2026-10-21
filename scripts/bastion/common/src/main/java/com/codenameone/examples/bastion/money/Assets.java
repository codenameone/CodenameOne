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
package com.codenameone.examples.bastion.money;

/// The assets the bank holds, and how many decimal places each is counted in.
///
/// The ledger keeps every amount as a whole number of the asset's smallest
/// unit -- cents for dollars, satoshi for bitcoin -- and `decimals` says how
/// many of those make one: an `AccountDto.balance` of `150` in USD is $1.50.
/// The server's `asset` table is the source; this mirrors its rows
/// (`V2__ledger.sql`) because the API has no call that lists them, and an
/// asset missing here is shown in raw units rather than guessed at.
public final class Assets {
    public static final String FIAT = "FIAT";
    public static final String CRYPTO = "CRYPTO";

    /// The currency balances are totalled in on the dashboard.
    public static final String REPORTING = "USD";

    private static final Asset[] ALL = {
        new Asset("USD", "US Dollar", FIAT, 2),
        new Asset("EUR", "Euro", FIAT, 2),
        new Asset("BTC", "Bitcoin", CRYPTO, 8),
        // 9, not the chain's 18: see the comment beside the row on the server.
        new Asset("ETH", "Ether", CRYPTO, 9),
    };

    /// One asset.
    public static final class Asset {
        public final String code;
        public final String name;
        public final String kind;
        public final int decimals;

        Asset(String code, String name, String kind, int decimals) {
            this.code = code;
            this.name = name;
            this.kind = kind;
            this.decimals = decimals;
        }

        public boolean isCrypto() {
            return CRYPTO.equals(kind);
        }
    }

    private Assets() {
    }

    /// The asset named `code`, or null.
    public static Asset get(String code) {
        for (Asset asset : ALL) {
            if (asset.code.equals(code)) {
                return asset;
            }
        }
        return null;
    }

    /// The decimals of `code`; 0 for an asset this app does not know, so its
    /// amounts are shown as the raw units they are.
    public static int decimals(String code) {
        Asset asset = get(code);
        return asset == null ? 0 : asset.decimals;
    }

    public static boolean isCrypto(String code) {
        Asset asset = get(code);
        return asset != null && asset.isCrypto();
    }

    /// Every asset, fiat first.
    public static Asset[] all() {
        Asset[] copy = new Asset[ALL.length];
        System.arraycopy(ALL, 0, copy, 0, ALL.length);
        return copy;
    }
}
