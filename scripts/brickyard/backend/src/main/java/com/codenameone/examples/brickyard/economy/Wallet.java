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

import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.brickyard.Clock;
import com.codenameone.examples.brickyard.Ids;
import com.codenameone.examples.brickyard.api.LedgerDto;
import com.codenameone.examples.brickyard.api.WalletDto;
import com.codenameone.examples.brickyard.domain.Account;
import com.codenameone.examples.brickyard.domain.LedgerEntry;
import com.codenameone.examples.brickyard.repository.AccountRepository;
import com.codenameone.examples.brickyard.repository.LedgerEntryRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Bricks. The ledger is append-only and the balance is its sum; the
/// `bricks` column of the account is only a copy of that sum for the
/// account screen, rewritten on every post. Nothing edits or deletes an
/// entry: a refund or a revocation is a new, negative one.
@Component
public class Wallet {
    private final LedgerEntryRepository ledger;
    private final AccountRepository accounts;

    public Wallet(LedgerEntryRepository ledger, AccountRepository accounts) {
        this.ledger = ledger;
        this.accounts = accounts;
    }

    public long balance(String username) {
        long sum = 0;
        for (LedgerEntry e : ledger.of(username)) {
            sum += e.getAmount();
        }
        return sum;
    }

    /// Posts an entry. A debit that would take the balance below zero is
    /// refused unless `mayGoNegative` (a refund of Bricks already spent).
    @Transactional(rollbackFor = IOException.class)
    public long post(String username, long amount, String kind, String reference, boolean mayGoNegative)
            throws IOException {
        Account a = username == null ? null : accounts.findById(username);
        if (a == null) {
            throw new ResponseStatusException(404, "No such account");
        }
        long balance = balance(username);
        if (amount < 0 && balance + amount < 0 && !mayGoNegative) {
            throw new ResponseStatusException(402, "Not enough Bricks");
        }
        LedgerEntry e = new LedgerEntry();
        e.setId(Ids.next());
        e.setUsername(username);
        e.setAmount(amount);
        e.setKind(kind);
        e.setReference(reference == null ? "" : reference);
        e.setCreatedAt(Clock.now());
        ledger.insert(e);
        a.setBricks(balance + amount);
        accounts.save(a);
        return balance + amount;
    }

    public long post(String username, long amount, String kind, String reference) throws IOException {
        return post(username, amount, kind, reference, false);
    }

    @Transactional(readOnly = true)
    public WalletDto describe(String username) {
        List<LedgerDto> out = new ArrayList<LedgerDto>();
        long sum = 0;
        for (LedgerEntry e : ledger.of(username)) {
            sum += e.getAmount();
            out.add(new LedgerDto(e.getId(), e.getAmount(), e.getKind(), e.getReference(), e.getCreatedAt()));
        }
        return new WalletDto(sum, out);
    }
}
