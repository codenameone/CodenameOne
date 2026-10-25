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

import com.codenameone.examples.bastion.repository.ProfileRepository;
import com.codenameone.examples.bastion.Refused;
import com.codename1.backend.Config;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Transactional;
import com.codenameone.examples.bastion.api.AccountDto;
import com.codenameone.examples.bastion.api.AssetDto;
import com.codenameone.examples.bastion.api.BankingApiServer;
import com.codenameone.examples.bastion.api.CardDto;
import com.codenameone.examples.bastion.api.CardLimitDto;
import com.codenameone.examples.bastion.api.CardSpendDto;
import com.codenameone.examples.bastion.api.CashDto;
import com.codenameone.examples.bastion.api.DeviceDto;
import com.codenameone.examples.bastion.api.DeviceRegistrationDto;
import com.codenameone.examples.bastion.api.DeviceRevokeDto;
import com.codenameone.examples.bastion.api.NewPayeeDto;
import com.codenameone.examples.bastion.api.OpenAccountDto;
import com.codenameone.examples.bastion.api.PayeeDto;
import com.codenameone.examples.bastion.api.PostingDto;
import com.codenameone.examples.bastion.api.ReceiptDto;
import com.codenameone.examples.bastion.api.TransferDto;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.Asset;
import com.codenameone.examples.bastion.domain.Card;
import com.codenameone.examples.bastion.domain.Device;
import com.codenameone.examples.bastion.domain.JournalEntry;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.domain.Payee;
import com.codenameone.examples.bastion.domain.Posting;
import com.codenameone.examples.bastion.ledger.AccountKinds;
import com.codenameone.examples.bastion.ledger.Ledger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// [com.codenameone.examples.bastion.api.BankingApi], served.
///
/// Every call acts on the caller's own money, named by their token: an
/// account, payee or card id in a request is looked up *through* the caller,
/// and anyone else's answers 404 exactly as one that does not exist does.
/// The signature is checked here, before any transaction opens, so that its
/// nonce is spent and its refusal audited whatever the movement then does.
@Component
public class BankingEndpoint implements BankingApiServer {
    /// More accounts than a person keeps; a ceiling on rows a token can make.
    private static final int MAX_ACCOUNTS = 20;

    private final Ledger ledger;
    private final Money money;
    private final Payees payees;
    private final Cards cards;
    private final Devices devices;
    private final RequestSigning signing;
    private final Audit audit;
    private final Config config;
    private final ProfileRepository profiles;

    public BankingEndpoint(Ledger ledger, Money money, Payees payees, Cards cards,
            Devices devices, RequestSigning signing, Audit audit, Config config,
            ProfileRepository profiles) {
        this.profiles = profiles;
        this.ledger = ledger;
        this.money = money;
        this.payees = payees;
        this.cards = cards;
        this.devices = devices;
        this.signing = signing;
        this.audit = audit;
        this.config = config;
    }

    @Override
    public List<AssetDto> assets() throws Exception {
        List<AssetDto> out = new ArrayList<AssetDto>();
        for (Asset a : ledger.assets()) {
            out.add(new AssetDto(a.getCode(), a.getName(), a.getKind(), a.getDecimals()));
        }
        return out;
    }

    @Override
    public List<AccountDto> accounts() throws Exception {
        String owner = Devices.customer();
        List<AccountDto> out = new ArrayList<AccountDto>();
        for (LedgerAccount a : ledger.accountsOf(owner)) {
            if (!AccountKinds.HOLD.equals(a.getKind())) {
                out.add(dto(a));
            }
        }
        return out;
    }

    @Override
    public AccountDto open(OpenAccountDto request) throws Exception {
        String owner = Devices.customer();
        if (request == null) {
            throw Refused.with(400, "say-which-account-open", "Say which account to open");
        }
        String kind = request.kind();
        if (!AccountKinds.CHECKING.equals(kind) && !AccountKinds.SAVINGS.equals(kind)
                && !AccountKinds.WALLET.equals(kind)) {
            throw Refused.with(400, "open-checking-savings-wallet-account", "Open a CHECKING, SAVINGS or WALLET account");
        }
        return dto(openAudited(owner, request.asset(), kind, request.name()));
    }

    @Transactional(rollbackFor = Exception.class)
    private LedgerAccount openAudited(String owner, String asset, String kind, String name)
            throws IOException {
        // Behind the customer's profile row: two of these at once would each
        // count the rows before the other inserted and together pass the cap.
        profiles.hold(owner);
        if (ledger.accountsOf(owner).size() >= MAX_ACCOUNTS) {
            throw Refused.with(409, "close-account-before-opening-another", "Close an account before opening another");
        }
        String clipped = name == null ? kind : (name.length() > 80 ? name.substring(0, 80) : name);
        LedgerAccount account = ledger.open(owner, asset, kind, clipped);
        audit.append(owner, "ACCOUNT_OPENED", account.getId(), kind + " " + asset);
        return account;
    }

    @Override
    public List<PostingDto> history(String id) throws Exception {
        String owner = Devices.customer();
        LedgerAccount account = ledger.owned(owner, id);
        List<PostingDto> out = new ArrayList<PostingDto>();
        // At most a hundred postings, and most share no entry; each entry is
        // read once for its kind and description.
        java.util.Map<String, JournalEntry> entries = new java.util.HashMap<String, JournalEntry>();
        for (Posting p : ledger.history(account.getId())) {
            JournalEntry entry = entries.get(p.getEntryId());
            if (entry == null && !entries.containsKey(p.getEntryId())) {
                entry = ledger.entry(p.getEntryId());
                entries.put(p.getEntryId(), entry);
            }
            out.add(new PostingDto(p.getId(), p.getEntryId(), p.getAsset(), p.getAmount(),
                    p.getCreatedAt().toEpochMilli(), entry == null ? "" : entry.getKind(),
                    entry == null ? "" : entry.getMemo()));
        }
        return out;
    }

    @Override
    public ReceiptDto transfer(String idempotencyKey, String signature, TransferDto transfer)
            throws Exception {
        String owner = Devices.customer();
        if (transfer == null || transfer.fromAccountId() == null
                || (transfer.toAccountId() == null) == (transfer.payeeId() == null)) {
            throw Refused.with(400, "name-account-pay-either-account", "Name the account to pay from, and either an account or a payee to pay");
        }
        LedgerAccount from = ledger.movable(owner, transfer.fromAccountId());
        boolean leaves = transfer.payeeId() != null;
        Device device = null;
        // A signature that is sent is always checked, even where none is
        // needed: one that is wrong is never ignored.
        if (money.needsSignature(from, transfer.amount(), leaves) || RequestSigning.present()) {
            device = signing.require(owner);
        }
        if (!leaves) {
            LedgerAccount to = ledger.movable(owner, transfer.toAccountId());
            return money.internal(owner, idempotencyKey, from, to, transfer.amount(),
                    transfer.memo());
        }
        money.requireTrusted(device);
        Payee payee = payees.owned(owner, transfer.payeeId());
        List<LedgerAccount> theirs = ledger.accountsOf(payee.getUsername());
        LedgerAccount to = null;
        for (LedgerAccount a : theirs) {
            // A frozen account is passed over, so paying a frozen customer
            // gets the same "cannot receive" as one with no such account:
            // the payer is not told that someone else's account is frozen.
            if (to == null && AccountKinds.CHECKING.equals(a.getKind())
                    && a.getAsset().equals(from.getAsset()) && !a.isFrozen()) {
                to = a;
            }
        }
        if (to == null) {
            throw Refused.with(409, "payee-cannot-receive", "That payee cannot receive {0}", from.getAsset());
        }
        return money.pay(owner, idempotencyKey, from, payee, to, transfer.amount(),
                transfer.memo());
    }

    @Override
    public ReceiptDto deposit(String id, String idempotencyKey, CashDto cash) throws Exception {
        String owner = Devices.customer();
        if (!config.isDevelopmentProfile()) {
            // Money appears from nowhere: a simulation for the demo, which no
            // production server has.
            throw Refused.with(404, "not-found", "Not found");
        }
        LedgerAccount to = ledger.movable(owner, id);
        return money.deposit(owner, idempotencyKey, to, cash == null ? 0 : cash.amount(),
                cash == null ? null : cash.memo());
    }

    @Override
    public ReceiptDto withdraw(String id, String idempotencyKey, String signature, CashDto cash)
            throws Exception {
        String owner = Devices.customer();
        LedgerAccount from = ledger.movable(owner, id);
        // Always signed, whatever the policy: money leaving the bank does not
        // come back.
        Device device = signing.require(owner);
        money.requireTrusted(device);
        return money.withdraw(owner, idempotencyKey, from, cash == null ? 0 : cash.amount(),
                cash == null ? null : cash.memo());
    }

    @Override
    public List<PayeeDto> payees() throws Exception {
        return payees.list(Devices.customer());
    }

    @Override
    public PayeeDto addPayee(String signature, NewPayeeDto payee) throws Exception {
        String owner = Devices.customer();
        Device device = signing.require(owner);
        money.requireTrusted(device);
        return payees.add(owner, payee);
    }

    @Override
    public PayeeDto renamePayee(String id, String signature, NewPayeeDto payee)
            throws Exception {
        // Signed: the name is what the customer picks a payee by, so a token
        // alone could otherwise relabel an attacker's payee as the landlord.
        String owner = Devices.customer();
        signing.require(owner);
        return payees.rename(owner, id, payee);
    }

    @Override
    public PayeeDto removePayee(String id, String signature) throws Exception {
        String owner = Devices.customer();
        signing.require(owner);
        return payees.remove(owner, id);
    }

    @Override
    public List<CardDto> cards() throws Exception {
        return cards.list(Devices.customer());
    }

    @Override
    public CardDto issueCard(String accountId, String signature) throws Exception {
        // Signed by a trusted device: a card is one more way for money to
        // leave the account, so a token alone may not mint one.
        String owner = Devices.customer();
        Device device = signing.require(owner);
        money.requireTrusted(device);
        return cards.issue(owner, ledger.movable(owner, accountId));
    }

    @Override
    public CardDto freezeCard(String id) throws Exception {
        return cards.freeze(Devices.customer(), id, true);
    }

    @Override
    public CardDto unfreezeCard(String id, String signature) throws Exception {
        // Freezing stays a token-only call -- anyone holding the session may
        // stop a card, and should be able to the moment it is lost -- but
        // unfreezing puts spending back, so it is held to what moving money is.
        String owner = Devices.customer();
        Device device = signing.require(owner);
        money.requireTrusted(device);
        return cards.freeze(owner, id, false);
    }

    @Override
    public CardDto cardLimit(String id, String signature, CardLimitDto limit) throws Exception {
        if (limit == null) {
            throw Refused.with(400, "send-limit", "Send the limit");
        }
        String owner = Devices.customer();
        // Lowering a limit is as harmless as freezing; raising one is more
        // spending, so it is signed by a trusted device. Whether this is a
        // raise is decided by Cards.limit behind the card's row: compared
        // here, a concurrent lower committed in between would let an unsigned
        // request "lower" the limit back up. A signature, when sent, is
        // checked first (it can be used only once).
        boolean signed = false;
        if (RequestSigning.present()) {
            Device device = signing.require(owner);
            money.requireTrusted(device);
            signed = true;
        }
        return cards.limit(owner, id, limit.dailyLimit(), signed);
    }

    @Override
    public ReceiptDto spend(String id, String idempotencyKey, CardSpendDto spend)
            throws Exception {
        String owner = Devices.customer();
        if (!config.isDevelopmentProfile()) {
            // A real authorization arrives from the card network over its own
            // authenticated channel, never from the cardholder's token.
            throw Refused.with(404, "not-found", "Not found");
        }
        Card card = cards.owned(owner, id);
        if (card.isFrozen()) {
            audit.append(owner, "CARD_DECLINED", id, "frozen");
            throw Refused.with(409, "card-frozen", "That card is frozen");
        }
        LedgerAccount from = ledger.movable(owner, card.getAccountId());
        return money.spend(owner, idempotencyKey, card, from, spend == null ? 0 : spend.amount(),
                spend == null ? null : spend.merchant());
    }

    @Override
    public List<DeviceDto> devices() throws Exception {
        return devices.list(Devices.customer());
    }

    @Override
    public DeviceDto registerDevice(DeviceRegistrationDto device) throws Exception {
        return devices.register(Devices.customer(), device);
    }

    @Override
    public DeviceDto revokeDevice(String id, String signature, DeviceRevokeDto request)
            throws Exception {
        String owner = Devices.customer();
        // A stolen token must not revoke the owner's phone, which would leave
        // the thief's session as the only one that can approve anything. One
        // of the account's devices signs it (another, or the one going), or,
        // with none at hand, the password is entered again.
        if (RequestSigning.present()) {
            signing.require(owner);
        } else {
            devices.recheck(owner, request == null ? null : request.password());
        }
        return devices.revoke(owner, id);
    }

    private static AccountDto dto(LedgerAccount a) {
        return new AccountDto(a.getId(), a.getAsset(), a.getKind(), a.getName(), a.getBalance(),
                a.isFrozen(), a.getCreatedAt().toEpochMilli());
    }
}
