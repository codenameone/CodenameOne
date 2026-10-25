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
package com.codenameone.examples.bastion.chain;

import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.PostConstruct;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.annotations.Value;
import com.codenameone.examples.bastion.Ids;
import com.codenameone.examples.bastion.api.CustodyDto;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.ChainCursor;
import com.codenameone.examples.bastion.domain.ChainTransfer;
import com.codenameone.examples.bastion.domain.CryptoAddress;
import com.codenameone.examples.bastion.domain.DepositAddress;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.ledger.AccountKinds;
import com.codenameone.examples.bastion.ledger.Ledger;
import com.codenameone.examples.bastion.ledger.Leg;
import com.codenameone.examples.bastion.repository.ChainCursorRepository;
import com.codenameone.examples.bastion.repository.ChainTransferRepository;
import com.codenameone.examples.bastion.repository.DepositAddressRepository;
import com.codenameone.examples.bastion.repository.LedgerAccountRepository;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/// Real Ether, on a real Ethereum network: deposits credited from the chain,
/// withdrawals signed here and broadcast, and every coin that crosses posted
/// to the ledger exactly once.
///
/// Off unless `bastion.chain.rpc-url` names a node; the sample then keeps its
/// simulated chain (SYS_CHAIN as the far side of every withdrawal). On, ETH
/// moves for real -- on a test network or a local development chain for the
/// sample, on mainnet for a product that has done what the implementor's
/// guide lists first.
///
/// **Custody.** One seed (`bastion.chain.seed`, 32 bytes of hex) backs every
/// key, through BIP-32: customer deposit addresses at m/44'/60'/0'/0/i and the
/// hot wallet withdrawals are paid from at m/44'/60'/1'/0/0. A product keeps
/// the seed in an HSM or a cloud key service and replaces [ChainSigner]'s
/// in-process implementation with one that asks it to sign; nothing else here
/// would change. The hot wallet should hold only what a day of withdrawals
/// needs, the rest moved to cold storage by hand.
///
/// **Withdrawals** are a small state machine (see [ChainTransfer]). The
/// customer's coins go to SYS_CHAIN_TRANSIT in the same transaction that
/// queues the row. A transaction is signed with a fixed nonce and the signed
/// bytes are saved *before* the first broadcast, so a crash at any point
/// re-broadcasts the same transaction instead of signing a second spend. It
/// is final after `bastion.chain.confirmations` blocks, and only if the block
/// that holds it is still the canonical one at that height.
///
/// **Deposits** are read from blocks already `confirmations` deep, so they are
/// never credited from a block that is later reorganized away (deeper reorgs
/// stop the scanner for a person to look at: see [#scan]). A deposit is a
/// transaction whose `to` is a customer's deposit address and whose receipt
/// says it succeeded. Ether sent by a contract (internal transactions) is not
/// seen by this scan; it needs a tracing node, and until then the scheduled
/// sweep still moves it, so it shows up as unexplained balance to reconcile.
///
/// **Postings.** Customer -> SYS_CHAIN_TRANSIT when queued; TRANSIT ->
/// SYS_CHAIN when final, or back to the customer if the transaction failed;
/// SYS_CHAIN -> customer for a deposit; SYS_NETWORK_FEES -> SYS_CHAIN for
/// every fee paid. Each posting's idempotency key names the row and the step,
/// so running a step twice moves nothing the second time.
@Component
public class Chain {
    public static final String ASSET = "ETH";
    static final String OUT = "OUT";
    static final String IN = "IN";
    static final String SWEEP = "SWP";
    static final String QUEUED = "QUEUED";
    static final String SIGNED = "SIGNED";
    static final String BROADCAST = "BROADCAST";
    static final String CONFIRMED = "CONFIRMED";
    static final String FAILED = "FAILED";
    /// A plain Ether transfer to an account with no code.
    static final long TRANSFER_GAS = 21000L;
    private static final String CURSOR = "ETH";
    private static final String HD_COUNTER = "ETH-HD";
    /// Blocks read per tick, so one tick never holds the scheduler for long.
    private static final int BLOCKS_PER_TICK = 64;

    private final Ledger ledger;
    private final Audit audit;
    private final ChainTransferRepository transfers;
    private final DepositAddressRepository deposits;
    private final ChainCursorRepository cursors;
    private final LedgerAccountRepository accounts;
    private final String rpcUrl;
    private final long expectedChainId;
    private final int confirmations;
    private final String seedHex;
    private final long rebroadcastMillis;
    private final long sweepMinimumGwei;
    private static final String LEASE = "ETH-LEASE";
    /// A worker that has not renewed its lease for this long is presumed
    /// dead and another server takes over.
    private static final long LEASE_MILLIS = 5L * 60L * 1000L;
    /// Keeps this server's own ticks from overlapping.
    private final AtomicBoolean running = new AtomicBoolean();
    private final String instance;
    private EthRpc rpc;
    private ChainSigner signer;

    public Chain(Ledger ledger, Audit audit, ChainTransferRepository transfers,
            DepositAddressRepository deposits, ChainCursorRepository cursors,
            LedgerAccountRepository accounts,
            @Value("${bastion.chain.rpc-url:}") String rpcUrl,
            @Value("${bastion.chain.id:11155111}") String chainId,
            @Value("${bastion.chain.confirmations:12}") String confirmations,
            @Value("${bastion.chain.seed:}") String seedHex,
            @Value("${bastion.chain.rebroadcast-seconds:300}") String rebroadcastSeconds,
            @Value("${bastion.chain.sweep-minimum-gwei:1000000}") String sweepMinimumGwei) {
        this.ledger = ledger;
        this.audit = audit;
        this.transfers = transfers;
        this.deposits = deposits;
        this.cursors = cursors;
        this.accounts = accounts;
        this.rpcUrl = rpcUrl.trim();
        this.expectedChainId = Long.parseLong(chainId.trim());
        this.confirmations = Math.max(1, Integer.parseInt(confirmations.trim()));
        this.seedHex = seedHex.trim();
        this.rebroadcastMillis = Long.parseLong(rebroadcastSeconds.trim()) * 1000L;
        this.sweepMinimumGwei = Long.parseLong(sweepMinimumGwei.trim());
        try {
            this.instance = Ids.next();
        } catch (IOException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    /// Connects, and refuses to start against the wrong network: a seed
    /// configured for a test network must never sign on mainnet by a typo in
    /// the URL, nor the reverse.
    @PostConstruct
    public void start() throws IOException {
        if (rpcUrl.length() == 0) {
            return;
        }
        if (seedHex.length() != 64) {
            throw new IllegalStateException("bastion.chain.seed must be 32 bytes of hex");
        }
        rpc = new EthRpc(rpcUrl);
        long actual = rpc.chainId();
        if (actual != expectedChainId) {
            throw new IllegalStateException("the node is on chain " + actual
                    + ", bastion.chain.id says " + expectedChainId);
        }
        signer = new SeedSigner(Hex32.decode(seedHex));
        // The cursor exists before the first address is handed out. Made
        // lazily by the first scan instead, it would start at the head of
        // that moment, and a deposit sent between the address being shown
        // and that scan would sit in a block nothing ever reads.
        cursor(Math.max(0, rpc.blockNumber() - confirmations + 1));
    }

    /// True when ETH moves on a real chain. Read from the configuration, not
    /// from [#start], so a component that asks during its own start-up gets
    /// the right answer whichever starts first.
    public boolean enabled() {
        return rpcUrl.length() > 0;
    }

    public boolean handles(String asset) {
        return enabled() && ASSET.equals(asset);
    }

    /// The address customers' withdrawals are paid from, so an operator can
    /// fund it and watch it.
    public String hotWallet() {
        return signer.hotAddress();
    }

    public long chainId() {
        return expectedChainId;
    }

    public int confirmations() {
        return confirmations;
    }

    /// The network's name, for people: a customer must be able to tell a
    /// test network from the real one before sending anything.
    public String network() {
        if (expectedChainId == 1) {
            return "Ethereum";
        }
        if (expectedChainId == 11155111) {
            return "Sepolia test network";
        }
        if (expectedChainId == 17000) {
            return "Holesky test network";
        }
        return "Development chain " + expectedChainId;
    }

    /// [CustodyDto] for the back office.
    @Transactional(readOnly = true)
    public CustodyDto custodyReport() throws IOException {
        if (!enabled()) {
            return new CustodyDto(false, ASSET, 0, "", 0, 0, 0, 0);
        }
        long owed = 0;
        long transit = 0;
        for (LedgerAccount a : accounts.findByAsset(ASSET)) {
            if (AccountKinds.SYS_CHAIN_TRANSIT.equals(a.getKind())) {
                transit += a.getBalance();
            }
            if (!AccountKinds.BANK.equals(a.getOwner())
                    || AccountKinds.SYS_CHAIN_TRANSIT.equals(a.getKind())) {
                owed += a.getBalance();
            }
        }
        return new CustodyDto(true, ASSET, expectedChainId, EthAddress.checksum(signer.hotAddress()),
                Wei.toGwei(rpc.balance(signer.hotAddress())), custody(), owed, transit);
    }

    // ------------------------------------------------------------- deposits

    /// `owner`'s deposit address, derived and stored the first time.
    @Transactional(rollbackFor = Exception.class)
    public DepositAddress depositAddress(String owner) throws IOException {
        List<DepositAddress> mine = deposits.findByOwnerAndAsset(owner, ASSET);
        if (!mine.isEmpty()) {
            return mine.get(0);
        }
        // The next index, behind the counter row: two customers asking at
        // once must not be given one address.
        ChainCursor counter = cursors.findById(HD_COUNTER);
        if (counter == null) {
            counter = new ChainCursor();
            counter.setChain(HD_COUNTER);
            counter.setLastHash("");
            cursors.insert(counter);
        }
        long index = counter.getNextBlock();
        counter.setNextBlock(index + 1);
        counter.setUpdatedAt(System.currentTimeMillis());
        cursors.save(counter);
        DepositAddress d = new DepositAddress();
        d.setAddress(signer.depositAddress(index));
        d.setOwner(owner);
        d.setAsset(ASSET);
        d.setHdIndex(index);
        d.setCreatedAt(System.currentTimeMillis());
        deposits.insert(d);
        audit.append(owner, "DEPOSIT_ADDRESS", d.getAddress(), "index " + index);
        return d;
    }

    /// Reads the blocks that became `confirmations` deep since the last tick
    /// and credits every deposit in them.
    ///
    /// The cursor remembers the hash of the last block read. A block whose
    /// parent is not that hash means the chain was reorganized deeper than
    /// the confirmation depth -- something that should not happen at a sane
    /// depth and that may already have credited a deposit that no longer
    /// exists. The scanner stops there, audits it, and waits for a person.
    @SuppressWarnings("unchecked")
    public void scan() throws IOException {
        long safeHead = rpc.blockNumber() - confirmations + 1;
        ChainCursor cursor = cursor(safeHead);
        long end = Math.min(safeHead, cursor.getNextBlock() + BLOCKS_PER_TICK - 1);
        for (long n = cursor.getNextBlock(); n <= end; n++) {
            Map<String, Object> block = rpc.block(n);
            if (block == null) {
                return;
            }
            String parent = (String) block.get("parentHash");
            if (cursor.getLastHash().length() > 0 && !cursor.getLastHash().equalsIgnoreCase(parent)) {
                audit.append("@chain", "CHAIN_REORG", String.valueOf(n),
                        "parent " + parent + " is not " + cursor.getLastHash());
                throw new IOException("chain reorganized below the confirmation depth at block " + n);
            }
            for (Object o : (List<Object>) block.get("transactions")) {
                Map<String, Object> tx = (Map<String, Object>) o;
                String to = (String) tx.get("to");
                if (to == null) {
                    continue;
                }
                DepositAddress d = depositAt(to.toLowerCase());
                if (d == null) {
                    continue;
                }
                byte[] value = Wei.parseQuantity((String) tx.get("value"));
                long gwei = Wei.toGwei(value);
                if (gwei <= 0) {
                    continue;
                }
                Map<String, Object> receipt = rpc.receipt((String) tx.get("hash"));
                if (receipt == null || !"0x1".equals(receipt.get("status"))) {
                    // Included but reverted: no value moved.
                    continue;
                }
                credit(d, (String) tx.get("hash"), gwei, n, (String) block.get("hash"));
            }
            advance(cursor, n + 1, (String) block.get("hash"));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    private ChainCursor cursor(long safeHead) throws IOException {
        ChainCursor c = cursors.findById(CURSOR);
        if (c == null) {
            // A new bank starts reading at the current safe head: there are
            // no deposit addresses before it, so nothing earlier is ours.
            c = new ChainCursor();
            c.setChain(CURSOR);
            c.setNextBlock(Math.max(0, safeHead));
            c.setLastHash("");
            c.setUpdatedAt(System.currentTimeMillis());
            cursors.insert(c);
        }
        return c;
    }

    @Transactional(rollbackFor = Exception.class)
    private void advance(ChainCursor c, long next, String hash) throws IOException {
        c.setNextBlock(next);
        c.setLastHash(hash);
        c.setUpdatedAt(System.currentTimeMillis());
        cursors.save(c);
    }

    /// Credits one deposit, once: the row's unique transaction hash and the
    /// posting's idempotency key both refuse a second.
    @Transactional(rollbackFor = Exception.class)
    private void credit(DepositAddress d, String txHash, long gwei, long blockNumber,
            String blockHash) throws IOException {
        if (!transfers.findByDirectionAndTxHash(IN, txHash).isEmpty()) {
            return;
        }
        LedgerAccount wallet = wallet(d.getOwner());
        LedgerAccount chain = ledger.system(ASSET, AccountKinds.SYS_CHAIN);
        String id = Ids.next();
        ledger.post("chain:in:" + txHash, "CRYPTO_IN", d.getOwner(), "from the chain " + txHash,
                Arrays.asList(new Leg(chain.getId(), -gwei), new Leg(wallet.getId(), gwei)));
        ChainTransfer t = row(id, IN, d.getOwner(), d.getAddress(), gwei, CONFIRMED);
        t.setTxHash(txHash);
        t.setBlockNumber(blockNumber);
        t.setBlockHash(blockHash);
        t.setReference(d.getAddress().substring(2, 34));
        transfers.insert(t);
        audit.append(d.getOwner(), "CRYPTO_IN", txHash, gwei + " " + ASSET + " at block " + blockNumber);
    }

    // ---------------------------------------------------------- withdrawals

    /// Queues the withdrawal the ledger has just moved into transit, in the
    /// caller's transaction. `entryId` is that movement's journal entry,
    /// which makes the row's id: a retried request finds the row it made.
    public void queue(String entryId, String owner, CryptoAddress to, long gwei) throws IOException {
        if (transfers.findById(entryId) != null) {
            return;
        }
        if (!EthAddress.valid(to.getAddress())) {
            // CryptoWallets checks this when the address is added; an address
            // added while the chain was simulated may predate the check.
            throw new IllegalArgumentException("not an Ethereum address");
        }
        ChainTransfer t = row(entryId, OUT, owner, EthAddress.normalize(to.getAddress()), gwei, QUEUED);
        t.setReference(to.getId());
        transfers.insert(t);
    }

    /// The scheduler's whole job, in an order that keeps money safe if any
    /// step stops halfway: sign what is queued, broadcast what is signed,
    /// settle what is mined, then read new deposits and sweep them.
    ///
    /// One worker at a time across every server: the lease row is taken with
    /// a single conditional update, and every state change below is itself a
    /// compare-and-set, so even two workers that both believe they hold the
    /// lease cannot sign one withdrawal twice.
    @Scheduled(fixedRateString = "${bastion.chain.period-ms:15000}")
    public void tick() {
        if (!enabled() || !running.compareAndSet(false, true)) {
            return;
        }
        try {
            if (!lease()) {
                return;
            }
            for (ChainTransfer t : inState(QUEUED)) {
                sign(t);
            }
            for (ChainTransfer t : inState(SIGNED)) {
                broadcast(t);
            }
            for (ChainTransfer t : inState(BROADCAST)) {
                settle(t);
            }
            scan();
            sweep();
        } catch (IOException | RuntimeException failed) {
            // The next tick starts over from the rows; nothing is half done.
            try {
                audit.append("@chain", "CHAIN_TICK_FAILED", "", String.valueOf(failed.getMessage()));
            } catch (IOException notRecorded) {
                // The database is what failed; the next tick tries again.
            }
        } finally {
            running.set(false);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    private boolean lease() throws IOException {
        if (cursors.findById(LEASE) == null) {
            ChainCursor row = new ChainCursor();
            row.setChain(LEASE);
            row.setLastHash("");
            cursors.insert(row);
        }
        long now = System.currentTimeMillis();
        return cursors.lease(LEASE, instance, now, now - LEASE_MILLIS) == 1;
    }

    /// Fixes a nonce and fees and signs, saving the signed bytes before any
    /// broadcast.
    @Transactional(rollbackFor = Exception.class)
    private void sign(ChainTransfer t) throws IOException {
        if (OUT.equals(t.getDirection())) {
            String shortfall = reserves(t);
            if (shortfall != null) {
                // Stays QUEUED: the customer's coins wait in transit, nothing
                // is lost, and a person decides. Paying it would spend coins
                // that belong to other customers.
                if (!shortfall.equals(t.getDetail())) {
                    t.setDetail(shortfall);
                    save(t);
                    audit.append("@chain", "CHAIN_RESERVES_SHORT", t.getId(), shortfall);
                }
                return;
            }
        }
        String from = OUT.equals(t.getDirection()) ? signer.hotAddress() : t.getAddress();
        long nonce = nextNonce(from);
        byte[] priority = rpc.priorityFee();
        // Twice the next base fee plus the tip: enough to survive several
        // full blocks of rising fees; unused headroom is not charged.
        byte[] maxFee = add(Wei.mulSmall(rpc.nextBaseFee(), 2), priority);
        byte[] value = Wei.fromGwei(t.getAmount());
        String to = OUT.equals(t.getDirection()) ? t.getAddress() : signer.hotAddress();
        Eip1559Tx tx;
        byte[] signature;
        if (SWEEP.equals(t.getDirection())) {
            // A sweep sends everything but its own fee.
            Eip1559Tx probe = new Eip1559Tx(expectedChainId, nonce, priority, maxFee, TRANSFER_GAS,
                    to, value, null);
            long feeGwei = ceilGwei(probe.maxCost());
            long send = t.getAmount() - feeGwei;
            if (send <= 0) {
                finish(t, FAILED, "too little to pay its own fee");
                return;
            }
            tx = new Eip1559Tx(expectedChainId, nonce, priority, maxFee, TRANSFER_GAS, to,
                    Wei.fromGwei(send), null);
            signature = signer.signWithDeposit(depositIndex(t.getAddress()), tx.signingHash());
            t.setAmount(send);
        } else {
            tx = new Eip1559Tx(expectedChainId, nonce, priority, maxFee, TRANSFER_GAS, to, value, null);
            signature = signer.signWithHot(tx.signingHash());
        }
        byte[] raw = tx.raw(signature);
        // Saved before any broadcast, and only over the QUEUED row it was
        // made from: a second worker's signature is dropped here, unsent.
        transfers.signed(t.getId(), QUEUED, SIGNED, nonce, Hex32.encode(raw), Eip1559Tx.hash(raw),
                t.getAmount(), System.currentTimeMillis());
    }

    /// The node's pending nonce, or one past the highest this bank has
    /// signed from `from` if that is higher -- a transaction signed but not
    /// yet seen by the node must not have its nonce given to another.
    @Transactional(readOnly = true)
    private long nextNonce(String from) throws IOException {
        long next = rpc.pendingNonce(from);
        for (String status : new String[] {SIGNED, BROADCAST}) {
            for (ChainTransfer t : transfers.findByStatusOrderByCreatedAtAsc(status)) {
                String sender = OUT.equals(t.getDirection()) ? signer.hotAddress() : t.getAddress();
                if (sender.equals(from) && t.getNonce() >= next) {
                    next = t.getNonce() + 1;
                }
            }
        }
        return next;
    }

    @Transactional(rollbackFor = Exception.class)
    private void broadcast(ChainTransfer t) throws IOException {
        try {
            rpc.sendRaw(Hex32.decode(t.getRaw()));
        } catch (EthRpc.NodeException refused) {
            String m = String.valueOf(refused.getMessage());
            // The node already has it: from an earlier broadcast whose answer
            // was lost. Anything else stays SIGNED and is retried; the same
            // bytes can be sent any number of times.
            if (!m.contains("already known") && !m.contains("nonce too low")) {
                t.setDetail(m.length() > 200 ? m.substring(0, 200) : m);
                save(t);
                return;
            }
        }
        if (transfers.advance(t.getId(), SIGNED, BROADCAST, System.currentTimeMillis()) == 1) {
            t.setStatus(BROADCAST);
            t.setBroadcastAt(System.currentTimeMillis());
            t.setUpdatedAt(t.getBroadcastAt());
            save(t);
        }
    }

    /// Final once its block is `confirmations` deep and still canonical.
    @SuppressWarnings("unchecked")
    private void settle(ChainTransfer t) throws IOException {
        Map<String, Object> receipt = rpc.receipt(t.getTxHash());
        if (receipt == null) {
            if (System.currentTimeMillis() - t.getBroadcastAt() > rebroadcastMillis) {
                // Dropped from a node's pool: send the same bytes again.
                try {
                    rpc.sendRaw(Hex32.decode(t.getRaw()));
                } catch (EthRpc.NodeException ignored) {
                    // "already known" and "nonce too low" both mean wait.
                }
                t.setBroadcastAt(System.currentTimeMillis());
                save(t);
            }
            return;
        }
        long mined = Wei.parseLong((String) receipt.get("blockNumber"));
        if (rpc.blockNumber() - mined + 1 < confirmations) {
            return;
        }
        Map<String, Object> canonical = rpc.block(mined);
        if (canonical == null || !String.valueOf(canonical.get("hash"))
                .equalsIgnoreCase((String) receipt.get("blockHash"))) {
            // Its block was replaced; the transaction will be mined again
            // (same nonce, same bytes) or its receipt will disappear.
            return;
        }
        long gasUsed = Wei.parseLong((String) receipt.get("gasUsed"));
        byte[] price = Wei.parseQuantity((String) receipt.get("effectiveGasPrice"));
        long fee = ceilGwei(Wei.mulSmall(price, gasUsed));
        t.setBlockNumber(mined);
        t.setBlockHash((String) receipt.get("blockHash"));
        t.setFee(fee);
        finish(t, "0x1".equals(receipt.get("status")) ? CONFIRMED : FAILED, "");
    }

    /// Posts a final state once. A withdrawal that failed on the chain goes
    /// back to the customer; the fee is the bank's either way.
    @Transactional(rollbackFor = Exception.class)
    private void finish(ChainTransfer t, String status, String detail) throws IOException {
        if (transfers.advance(t.getId(), t.getStatus(), status, System.currentTimeMillis()) != 1) {
            // Another worker settled it; its postings stand.
            return;
        }
        LedgerAccount chain = ledger.system(ASSET, AccountKinds.SYS_CHAIN);
        if (OUT.equals(t.getDirection())) {
            LedgerAccount transit = ledger.system(ASSET, AccountKinds.SYS_CHAIN_TRANSIT);
            LedgerAccount back = CONFIRMED.equals(status) ? chain : wallet(t.getOwner());
            ledger.post("chain:out:" + t.getId() + ":" + status, CONFIRMED.equals(status)
                    ? "CRYPTO_OUT_FINAL" : "CRYPTO_OUT_FAILED", t.getOwner(),
                    (CONFIRMED.equals(status) ? "final " : "failed, returned ") + t.getTxHash(),
                    Arrays.asList(new Leg(transit.getId(), -t.getAmount()),
                            new Leg(back.getId(), t.getAmount())));
        }
        if (t.getFee() > 0) {
            LedgerAccount fees = ledger.system(ASSET, AccountKinds.SYS_NETWORK_FEES);
            ledger.post("chain:fee:" + t.getId(), "NETWORK_FEE", "@chain", "gas for " + t.getTxHash(),
                    Arrays.asList(new Leg(fees.getId(), -t.getFee()), new Leg(chain.getId(), t.getFee())));
        }
        t.setStatus(status);
        if (detail.length() > 0) {
            t.setDetail(detail);
        }
        t.setUpdatedAt(System.currentTimeMillis());
        transfers.save(t);
        audit.append(t.getOwner(), "CHAIN_" + t.getDirection() + "_" + status, t.getTxHash(),
                t.getAmount() + " " + ASSET + " fee " + t.getFee());
    }

    /// Null when the chain holds every coin the ledger owes; otherwise why
    /// not. The check every withdrawal passes before it is signed.
    ///
    /// The ledger owes customers the balances of every non-bank ETH account
    /// plus what is in transit. The chain holds the hot wallet and every
    /// deposit address. If the first is ever larger -- coins minted in the
    /// ledger by a bug, an operator's mistake or an attacker, or a hot wallet
    /// emptied by someone holding its key -- no withdrawal is signed until a
    /// person has looked, so one customer can never be paid with another's
    /// coins. The hot wallet itself must also cover this payment and its
    /// worst-case fee. A product keeps the total as a maintained aggregate
    /// rather than adding up every account, and alerts on it.
    @Transactional(readOnly = true)
    String reserves(ChainTransfer t) throws IOException {
        long owed = 0;
        for (LedgerAccount a : accounts.findByAsset(ASSET)) {
            if (!AccountKinds.BANK.equals(a.getOwner())
                    || AccountKinds.SYS_CHAIN_TRANSIT.equals(a.getKind())) {
                owed += a.getBalance();
            }
        }
        long held = custody();
        if (owed > held) {
            return "the ledger owes " + owed + " gwei and the chain holds " + held;
        }
        long hot = Wei.toGwei(rpc.balance(signer.hotAddress()));
        long worstFee = ceilGwei(Wei.mulSmall(Wei.mulSmall(rpc.nextBaseFee(), 2), TRANSFER_GAS))
                + ceilGwei(Wei.mulSmall(rpc.priorityFee(), TRANSFER_GAS));
        if (hot < t.getAmount() + worstFee) {
            return "the hot wallet holds " + hot + " gwei; this needs " + (t.getAmount() + worstFee);
        }
        return null;
    }

    // ---------------------------------------------------------------- sweep

    /// Moves deposits into the hot wallet once an address holds more than
    /// `bastion.chain.sweep-minimum-gwei`, so withdrawals are paid from one
    /// place and customers' addresses hold only what just arrived. A sweep
    /// moves no customer's money -- it is the bank's coins changing address --
    /// so the ledger sees only its fee.
    private void sweep() throws IOException {
        // One in flight per address: its balance is what the next one reads.
        for (DepositAddress d : allDeposits()) {
            if (sweeping(d.getAddress())) {
                continue;
            }
            long gwei = Wei.toGwei(rpc.balance(d.getAddress()));
            if (gwei < sweepMinimumGwei) {
                continue;
            }
            ChainTransfer t = row(Ids.next(), SWEEP, "@bank", d.getAddress(), gwei, QUEUED);
            t.setReference(String.valueOf(d.getHdIndex()));
            save(t);
        }
    }

    @Transactional(readOnly = true)
    private boolean sweeping(String address) throws IOException {
        for (String status : new String[] {QUEUED, SIGNED, BROADCAST}) {
            for (ChainTransfer t : transfers.findByStatusOrderByCreatedAtAsc(status)) {
                if (SWEEP.equals(t.getDirection()) && t.getAddress().equals(address)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Transactional(readOnly = true)
    private long depositIndex(String address) throws IOException {
        DepositAddress d = deposits.findById(address);
        if (d == null) {
            throw new IOException("no deposit address " + address);
        }
        return d.getHdIndex();
    }

    // -------------------------------------------------------------- helpers

    /// What the bank's books and the chain each say it holds in custody: the
    /// proof an auditor can check against any block explorer.
    @Transactional(readOnly = true)
    public long custody() throws IOException {
        long onChain = Wei.toGwei(rpc.balance(signer.hotAddress()));
        for (DepositAddress d : deposits.findAll()) {
            onChain += Wei.toGwei(rpc.balance(d.getAddress()));
        }
        return onChain;
    }

    /// One row, for the back office and the tests.
    @Transactional(readOnly = true)
    public ChainTransfer transfer(String id) throws IOException {
        return transfers.findById(id);
    }

    /// Every row, for the back office and the tests.
    @Transactional(readOnly = true)
    public List<ChainTransfer> transfers() throws IOException {
        return transfers.findAll();
    }

    @Transactional(readOnly = true)
    public List<ChainTransfer> history(String owner) throws IOException {
        return transfers.findByOwnerOrderByCreatedAtDesc(owner);
    }

    @Transactional(rollbackFor = Exception.class)
    private void save(ChainTransfer t) throws IOException {
        transfers.save(t);
    }

    @Transactional(readOnly = true)
    private LedgerAccount wallet(String owner) throws IOException {
        for (LedgerAccount a : ledger.accountsOf(owner)) {
            if (ASSET.equals(a.getAsset()) && AccountKinds.WALLET.equals(a.getKind())) {
                return a;
            }
        }
        return ledger.open(owner, ASSET, AccountKinds.WALLET, AccountKinds.WALLET);
    }

    private static ChainTransfer row(String id, String direction, String owner, String address,
            long gwei, String status) {
        ChainTransfer t = new ChainTransfer();
        long now = System.currentTimeMillis();
        t.setId(id);
        t.setDirection(direction);
        t.setOwner(owner);
        t.setAsset(ASSET);
        t.setAddress(address);
        t.setAmount(gwei);
        t.setStatus(status);
        t.setTxHash("unsigned:" + id);
        t.setNonce(-1);
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        return t;
    }

    /// Fees round up: the books may say the bank paid a fraction of a gwei
    /// more than it did, never less, so they never claim coins it spent.
    // Reads the tick makes between node calls, each in a short transaction
    // of its own rather than one held open across the network.
    @Transactional(readOnly = true)
    private List<ChainTransfer> inState(String status) throws IOException {
        return transfers.findByStatusOrderByCreatedAtAsc(status);
    }

    @Transactional(readOnly = true)
    private DepositAddress depositAt(String address) throws IOException {
        return deposits.findById(address);
    }

    @Transactional(readOnly = true)
    private List<DepositAddress> allDeposits() throws IOException {
        return deposits.findAll();
    }

    static long ceilGwei(byte[] wei) {
        long g = Wei.toGwei(wei);
        return Wei.hasDust(wei) ? g + 1 : g;
    }

    static byte[] add(byte[] a, byte[] b) {
        int n = Math.max(a.length, b.length) + 1;
        byte[] out = new byte[n];
        int carry = 0;
        for (int iter = 0; iter < n; iter++) {
            int x = iter < a.length ? a[a.length - 1 - iter] & 0xff : 0;
            int y = iter < b.length ? b[b.length - 1 - iter] & 0xff : 0;
            int sum = x + y + carry;
            out[n - 1 - iter] = (byte) sum;
            carry = sum >> 8;
        }
        return Wei.trim(out);
    }
}
